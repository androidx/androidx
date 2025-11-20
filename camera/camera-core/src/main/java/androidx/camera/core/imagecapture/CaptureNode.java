/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.core.imagecapture;

import static android.graphics.ImageFormat.JPEG;
import static android.graphics.ImageFormat.RAW_SENSOR;

import static androidx.camera.core.ImageCapture.ERROR_CAPTURE_FAILED;
import static androidx.camera.core.impl.utils.Threads.checkMainThread;
import static androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor;
import static androidx.core.util.Preconditions.checkState;

import static java.util.Objects.requireNonNull;

import android.graphics.ImageFormat;
import android.media.ImageReader;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.MainThread;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.ForwardingImageProxy;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.ImageReaderProxyProvider;
import androidx.camera.core.ImageReaderProxys;
import androidx.camera.core.Logger;
import androidx.camera.core.MetadataImageReader;
import androidx.camera.core.SafeCloseImageReaderProxy;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureCallbacks;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.ImmediateSurface;
import androidx.camera.core.impl.TagBundle;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.core.processing.Edge;
import androidx.camera.core.processing.Node;
import androidx.core.util.Consumer;

import com.google.auto.value.AutoValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A {@link Node} that calls back when all the images for one capture are received.
 *
 * <p>This {@link Node} waits for all the images in a {@link ProcessingRequest} are received
 * before invoking the {@link ProcessingRequest#onImageCaptured()} callback. Then it makes
 * sure that the {@link ImageProxy} are omitted after their corresponding {@link ProcessingRequest}.
 *
 * <p>It's also responsible for managing the {@link ImageReaderProxy}. It makes sure that the
 * queue is not overflowed.
 */
class CaptureNode implements Node<CaptureNode.In, ProcessingNode.In> {

    private static final String TAG = "CaptureNode";

    // TODO: we might need to calculate this number dynamically based on on many frames are
    //  needed by the post-processing. e.g. night mode might need to merge 10+ frames. 4 images
    //  should be enough for now.
    @VisibleForTesting
    static final int MAX_IMAGES = 4;

    @Nullable ProcessingRequest mCurrentRequest = null;

    @Nullable SafeCloseImageReaderProxy mSafeCloseImageReaderProxy;

    /* Additional image reader for simultaneous RAW + JPEG capture */
    @Nullable SafeCloseImageReaderProxy mSecondarySafeCloseImageReaderProxy;

    @Nullable SafeCloseImageReaderProxy mSafeCloseImageReaderForPostview;

    private ProcessingNode.@Nullable In  mOutputEdge;
    private @Nullable In mInputEdge;
    private @Nullable NoMetadataImageReader mNoMetadataImageReader = null;

    @Override
    public ProcessingNode.@NonNull In transform(@NonNull In inputEdge) {
        checkState(mInputEdge == null && mSafeCloseImageReaderProxy == null,
                "CaptureNode does not support recreation yet.");
        mInputEdge = inputEdge;
        Size size = inputEdge.getSize();
        int format = inputEdge.getInputFormat();

        // Create and configure ImageReader.
        Consumer<ProcessingRequest> requestConsumer;
        ImageReaderProxy wrappedImageReader;
        ImageReaderProxy secondaryWrappedImageReader = null;
        boolean hasMetadata = !inputEdge.isVirtualCamera();
        CameraCaptureCallback progressCallback = new CameraCaptureCallback() {
            @Override
            public void onCaptureStarted(int captureConfigId) {
                mainThreadExecutor().execute(() -> {
                    if (mCurrentRequest != null) {
                        mCurrentRequest.onCaptureStarted();
                    }
                });
            }

            @Override
            public void onCaptureProcessProgressed(int captureConfigId, int progress) {
                mainThreadExecutor().execute(() -> {
                    if (mCurrentRequest != null) {
                        mCurrentRequest.onCaptureProcessProgressed(progress);
                    }
                });
            }
        };

        CameraCaptureCallback cameraCaptureCallbacks;
        CameraCaptureCallback secondaryCameraCaptureCallback = null;
        boolean isSimultaneousCaptureEnabled = inputEdge.getOutputFormats().size() > 1;
        if (hasMetadata && inputEdge.getImageReaderProxyProvider() == null) {
            if (isSimultaneousCaptureEnabled) {
                MetadataImageReader metadataImageReader = new MetadataImageReader(size.getWidth(),
                        size.getHeight(), JPEG, MAX_IMAGES);
                cameraCaptureCallbacks =
                        CameraCaptureCallbacks.createComboCallback(
                                progressCallback, metadataImageReader.getCameraCaptureCallback());
                wrappedImageReader = metadataImageReader;

                MetadataImageReader secondaryMetadataImageReader = new MetadataImageReader(
                        size.getWidth(), size.getHeight(), RAW_SENSOR, MAX_IMAGES);
                secondaryCameraCaptureCallback =
                        CameraCaptureCallbacks.createComboCallback(
                                progressCallback,
                                secondaryMetadataImageReader.getCameraCaptureCallback());
                secondaryWrappedImageReader = secondaryMetadataImageReader;
            } else {
                // Use MetadataImageReader if the input edge expects metadata.
                MetadataImageReader metadataImageReader = new MetadataImageReader(size.getWidth(),
                        size.getHeight(), format, MAX_IMAGES);
                cameraCaptureCallbacks =
                        CameraCaptureCallbacks.createComboCallback(
                                progressCallback, metadataImageReader.getCameraCaptureCallback());
                wrappedImageReader = metadataImageReader;
            }

            requestConsumer = this::onRequestAvailable;
        } else {
            cameraCaptureCallbacks = progressCallback;
            // Use NoMetadataImageReader if the input edge does not expect metadata.
            mNoMetadataImageReader = new NoMetadataImageReader(
                    createImageReaderProxy(inputEdge.getImageReaderProxyProvider(),
                            size.getWidth(), size.getHeight(), format));
            wrappedImageReader = mNoMetadataImageReader;
            // Forward the request to the NoMetadataImageReader to create fake metadata.
            requestConsumer = request -> {
                onRequestAvailable(request);
                mNoMetadataImageReader.acceptProcessingRequest(request);
            };
        }
        inputEdge.setCameraCaptureCallback(cameraCaptureCallbacks);
        if (isSimultaneousCaptureEnabled && secondaryCameraCaptureCallback != null) {
            inputEdge.setSecondaryCameraCaptureCallback(secondaryCameraCaptureCallback);
        }
        inputEdge.setSurface(requireNonNull(wrappedImageReader.getSurface()));
        mSafeCloseImageReaderProxy = new SafeCloseImageReaderProxy(wrappedImageReader);

        // Listen to the input edges.
        setOnImageAvailableListener(wrappedImageReader);

        // Postview
        PostviewSettings postviewSettings = inputEdge.getPostviewSettings();
        if (postviewSettings != null) {
            ImageReaderProxy postviewImageReader =
                    createImageReaderProxy(inputEdge.getImageReaderProxyProvider(),
                            postviewSettings.getResolution().getWidth(),
                            postviewSettings.getResolution().getHeight(),
                            postviewSettings.getInputFormat());
            postviewImageReader.setOnImageAvailableListener(imageReader -> {
                try {
                    ImageProxy image = imageReader.acquireLatestImage();
                    if (image != null) {
                        propagatePostviewImage(image);
                    }
                } catch (IllegalStateException e) {
                    Logger.e(TAG, "Failed to acquire latest image of postview", e);
                }
            }, mainThreadExecutor());

            mSafeCloseImageReaderForPostview = new SafeCloseImageReaderProxy(postviewImageReader);
            inputEdge.setPostviewSurface(postviewImageReader.getSurface(),
                    postviewSettings.getResolution(), postviewSettings.getInputFormat());
        }

        // Simultaneous capture RAW + JPEG
        if (isSimultaneousCaptureEnabled && secondaryWrappedImageReader != null) {
            inputEdge.setSecondarySurface(secondaryWrappedImageReader.getSurface());
            mSecondarySafeCloseImageReaderProxy = new SafeCloseImageReaderProxy(
                    secondaryWrappedImageReader);
            setOnImageAvailableListener(secondaryWrappedImageReader);
        }

        inputEdge.getRequestEdge().setListener(requestConsumer);
        inputEdge.getErrorEdge().setListener(this::sendCaptureError);

        mOutputEdge = ProcessingNode.In.of(
                inputEdge.getInputFormat(),
                inputEdge.getOutputFormats());

        return mOutputEdge;
    }

    private static @NonNull ImageReaderProxy createImageReaderProxy(
            @Nullable ImageReaderProxyProvider imageReaderProxyProvider, int width, int height,
            int format) {
        if (imageReaderProxyProvider != null) {
            return imageReaderProxyProvider.newInstance(width, height, format, MAX_IMAGES, 0);
        } else {
            return ImageReaderProxys.createIsolatedReader(width, height, format, MAX_IMAGES);
        }
    }

    private void setOnImageAvailableListener(@NonNull ImageReaderProxy imageReaderProxy) {
        imageReaderProxy.setOnImageAvailableListener(imageReader -> {
            try {
                ImageProxy image = imageReader.acquireLatestImage();

                Logger.d(TAG, "OnImageAvailableListener: mCurrentRequest ID = "
                        + (mCurrentRequest == null ? null : mCurrentRequest.getRequestId())
                        + ", image.isNull = " + (image == null));

                if (image != null) {
                    onImageProxyAvailable(image);
                } else {
                    if (mCurrentRequest != null) {
                        sendCaptureError(
                                TakePictureManager.CaptureError.of(
                                        mCurrentRequest.getRequestId(),
                                        new ImageCaptureException(ERROR_CAPTURE_FAILED,
                                                "Failed to acquire latest image", null)));
                    }
                }
            } catch (IllegalStateException e) {
                if (mCurrentRequest != null) {
                    sendCaptureError(
                            TakePictureManager.CaptureError.of(mCurrentRequest.getRequestId(),
                                    new ImageCaptureException(
                                            ERROR_CAPTURE_FAILED,
                                            "Failed to acquire latest image", e)));
                }
            }
        }, mainThreadExecutor());
    }

    @VisibleForTesting
    @MainThread
    void onImageProxyAvailable(@NonNull ImageProxy imageProxy) {
        checkMainThread();
        if (mCurrentRequest == null) {
            // When aborted request still generates image, close the image and do nothing.
            Logger.w(TAG, "Discarding ImageProxy which was inadvertently acquired: " + imageProxy);
            imageProxy.close();
        } else {
            // If new request arrives but the previous aborted request still generates Image,
            // close the image and do nothing.
            TagBundle tagBundle = imageProxy.getImageInfo().getTagBundle();
            Integer stageId = (Integer) tagBundle.getTag(mCurrentRequest.getTagBundleKey());
            if (stageId == null) {
                Logger.w(TAG, "Discarding ImageProxy which was acquired for another request"
                        + ", mCurrentRequest id = " + mCurrentRequest.getRequestId()
                        + ", ImageProxy tagBundle keys = " + tagBundle.listKeys());
                imageProxy.close();
                return;
            }
            // Match image and send it downstream.
            matchAndPropagateImage(imageProxy);
        }
    }

    @MainThread
    private void matchAndPropagateImage(@NonNull ImageProxy imageProxy) {
        checkMainThread();
        // Send the image downstream.
        requireNonNull(mOutputEdge).getEdge().accept(
                ProcessingNode.InputPacket.of(mCurrentRequest, imageProxy));

        // The capture is complete. Let the pipeline know it can take another picture.
        ProcessingRequest request = mCurrentRequest;

        // If simultaneous capture RAW + JPEG, only reset when both images are processed.
        boolean isSimultaneousCaptureEnabled = mInputEdge != null
                && mInputEdge.getOutputFormats().size() > 1;
        if (isSimultaneousCaptureEnabled && mCurrentRequest != null) {
            mCurrentRequest.getTakePictureRequest()
                    .markFormatProcessStatusInSimultaneousCapture(
                            imageProxy.getFormat(), true);
        }
        boolean isProcessed =
                !isSimultaneousCaptureEnabled || (mCurrentRequest != null
                        && mCurrentRequest.getTakePictureRequest()
                        .isFormatProcessedInSimultaneousCapture());
        if (isProcessed) {
            mCurrentRequest = null;
        }
        request.onImageCaptured();
    }

    private void propagatePostviewImage(@NonNull ImageProxy imageProxy) {
        if (mCurrentRequest == null) {
            Logger.w(TAG, "Postview image is closed due to request completed or aborted");
            imageProxy.close();
            return;
        }
        requireNonNull(mOutputEdge).getPostviewEdge().accept(
                ProcessingNode.InputPacket.of(mCurrentRequest, imageProxy));
    }

    @VisibleForTesting
    @MainThread
    void onRequestAvailable(@NonNull ProcessingRequest request) {
        checkMainThread();
        checkState(request.getStageIds().size() == 1,
                "only one capture stage is supported.");
        // Unable to issue request if the queue has no capacity.
        checkState(getCapacity() > 0,
                "Too many acquire images. Close image to be able to process next.");

        // Track the request and its stage IDs.
        mCurrentRequest = request;

        Futures.addCallback(request.getCaptureFuture(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                // Do nothing
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                checkMainThread();
                if (request == mCurrentRequest) {
                    Logger.w(TAG, "request aborted, id=" + mCurrentRequest.getRequestId());
                    if (mNoMetadataImageReader != null) {
                        mNoMetadataImageReader.clearProcessingRequest();
                    }
                    mCurrentRequest = null;
                }
            }
        }, CameraXExecutors.directExecutor());
    }

    @MainThread
    void sendCaptureError(TakePictureManager.@NonNull CaptureError error) {
        checkMainThread();
        if (mCurrentRequest != null
                && mCurrentRequest.getRequestId() == error.getRequestId()) {
            mCurrentRequest.onCaptureFailure(error.getImageCaptureException());
        }
    }

    @MainThread
    @Override
    public void release() {
        checkMainThread();
        releaseInputResources(requireNonNull(mInputEdge),
                requireNonNull(mSafeCloseImageReaderProxy),
                mSecondarySafeCloseImageReaderProxy,
                mSafeCloseImageReaderForPostview);

    }

    private void releaseInputResources(CaptureNode.@NonNull In inputEdge,
            @NonNull SafeCloseImageReaderProxy imageReader,
            @Nullable SafeCloseImageReaderProxy secondaryImageReader,
            @Nullable SafeCloseImageReaderProxy imageReaderForPostview) {
        inputEdge.getSurface().close();
        // Wait for the termination to close the ImageReader or the Surface may be released
        // prematurely before it can be used by camera2.
        inputEdge.getSurface().getTerminationFuture().addListener(() -> {
            imageReader.safeClose();
        }, mainThreadExecutor());

        if (inputEdge.getPostviewSurface() != null) {
            inputEdge.getPostviewSurface().close();
            inputEdge.getPostviewSurface().getTerminationFuture().addListener(() -> {
                if (imageReaderForPostview != null) {
                    imageReaderForPostview.safeClose();
                }
            }, mainThreadExecutor());
        }

        if (inputEdge.getOutputFormats().size() > 1 && inputEdge.getSecondarySurface() != null) {
            inputEdge.getSecondarySurface().close();
            inputEdge.getSecondarySurface().getTerminationFuture().addListener(() -> {
                if (secondaryImageReader != null) {
                    secondaryImageReader.safeClose();
                }
            }, mainThreadExecutor());
        }
    }

    @VisibleForTesting
    @NonNull In getInputEdge() {
        return requireNonNull(mInputEdge);
    }

    @VisibleForTesting
    public @NonNull SafeCloseImageReaderProxy getSafeCloseImageReaderProxy() {
        return requireNonNull(mSafeCloseImageReaderProxy);
    }

    @MainThread
    public int getCapacity() {
        checkMainThread();
        checkState(mSafeCloseImageReaderProxy != null,
                "The ImageReader is not initialized.");
        return mSafeCloseImageReaderProxy.getCapacity();
    }

    @MainThread
    public void setOnImageCloseListener(ForwardingImageProxy.OnImageCloseListener listener) {
        checkMainThread();
        checkState(mSafeCloseImageReaderProxy != null,
                "The ImageReader is not initialized.");
        mSafeCloseImageReaderProxy.setOnImageCloseListener(listener);
    }

    /**
     * Input edges of a {@link CaptureNode}.
     */
    @AutoValue
    abstract static class In {

        private @NonNull CameraCaptureCallback mCameraCaptureCallback =
                new CameraCaptureCallback() {};

        /* Additional camera capture callback for simultaneous RAW + JPEG capture */
        private @Nullable CameraCaptureCallback mSecondaryCameraCaptureCallback;

        private @Nullable DeferrableSurface mSurface;

        /* Additional surface for simultaneous RAW + JPEG capture */
        private @Nullable DeferrableSurface mSecondarySurface;

        private @Nullable DeferrableSurface mPostviewSurface = null;

        /**
         * Size of the {@link ImageReader} buffer.
         */
        abstract @NonNull Size getSize();

        /**
         * The input format of the pipeline. The format of the {@link ImageReader}.
         */
        abstract int getInputFormat();

        /**
         * The output format of the pipeline.
         *
         * <p> For public users, {@link ImageFormat#JPEG}, {@link ImageFormat#JPEG_R} and
         * {@link ImageFormat#RAW_SENSOR}} are supported. Other formats are only used by in-memory
         * capture in tests.
         */
        @SuppressWarnings("AutoValueImmutableFields")
        abstract @NonNull List<Integer> getOutputFormats();

        /**
         * Whether the pipeline is connected to a virtual camera.
         */
        abstract boolean isVirtualCamera();

        /**
         * The {@link ImageReaderProxyProvider} associated with the node. When the value exists,
         * the node will use it to create the {@link ImageReaderProxy} that connects to
         * the camera.
         */
        abstract @Nullable ImageReaderProxyProvider getImageReaderProxyProvider();

        /**
         * The settings of the postview. Postview is configured if not null.
         */
        abstract @Nullable PostviewSettings getPostviewSettings();

        /**
         * Edge that accepts {@link ProcessingRequest}.
         */
        abstract @NonNull Edge<ProcessingRequest> getRequestEdge();

        /**
         * Edge that accepts {@link ImageCaptureException}.
         */
        abstract @NonNull Edge<TakePictureManager.CaptureError> getErrorEdge();

        /**
         * Edge that accepts the image frames.
         *
         * <p>The value will be used in a capture request sent to the camera.
         */
        @NonNull DeferrableSurface getSurface() {
            return requireNonNull(mSurface);
        }

        /**
         * Edge that accepts the postview image frame.
         */
        @Nullable DeferrableSurface getPostviewSurface() {
            return mPostviewSurface;
        }

        /**
         * Edge that accepts the image frames for simultaneous RAW + JPEG capture.
         */
        @Nullable DeferrableSurface getSecondarySurface() {
            return mSecondarySurface;
        }

        void setSurface(@NonNull Surface surface) {
            checkState(mSurface == null, "The surface is already set.");
            mSurface = new ImmediateSurface(surface, getSize(), getInputFormat());
        }

        void setPostviewSurface(@NonNull Surface surface, @NonNull Size size, int imageFormat) {
            mPostviewSurface = new ImmediateSurface(surface, size, imageFormat);
        }

        void setSecondarySurface(@NonNull Surface surface) {
            checkState(mSecondarySurface == null, "The secondary surface is "
                    + "already set.");
            mSecondarySurface = new ImmediateSurface(surface, getSize(), getInputFormat());
        }

        /**
         * Edge that accepts image metadata.
         *
         * <p>The value will be used in a capture request sent to the camera.
         */
        @NonNull CameraCaptureCallback getCameraCaptureCallback() {
            return mCameraCaptureCallback;
        }

        void setCameraCaptureCallback(@NonNull CameraCaptureCallback cameraCaptureCallback) {
            mCameraCaptureCallback = cameraCaptureCallback;
        }

        @Nullable CameraCaptureCallback getSecondaryCameraCaptureCallback() {
            return mSecondaryCameraCaptureCallback;
        }

        void setSecondaryCameraCaptureCallback(
                @NonNull CameraCaptureCallback cameraCaptureCallback) {
            mSecondaryCameraCaptureCallback = cameraCaptureCallback;
        }

        static @NonNull In of(
                @NonNull Size size,
                int inputFormat,
                @NonNull List<Integer> outputFormats,
                boolean isVirtualCamera,
                @Nullable ImageReaderProxyProvider imageReaderProxyProvider) {
            return new AutoValue_CaptureNode_In(size, inputFormat, outputFormats, isVirtualCamera,
                    imageReaderProxyProvider, null, new Edge<>(), new Edge<>());
        }

        static @NonNull In of(
                @NonNull Size size,
                int inputFormat,
                @NonNull List<Integer> outputFormats,
                boolean isVirtualCamera,
                @Nullable ImageReaderProxyProvider imageReaderProxyProvider,
                @Nullable PostviewSettings postviewSettings) {
            return new AutoValue_CaptureNode_In(size, inputFormat, outputFormats, isVirtualCamera,
                    imageReaderProxyProvider, postviewSettings, new Edge<>(), new Edge<>());
        }
    }
}
