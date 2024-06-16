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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.core.processing.Edge;
import androidx.camera.core.processing.Node;
import androidx.core.util.Consumer;

import com.google.auto.value.AutoValue;

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

    ProcessingRequest mCurrentRequest = null;

    @Nullable
    SafeCloseImageReaderProxy mSafeCloseImageReaderProxy;

    @Nullable
    SafeCloseImageReaderProxy mSafeCloseImageReaderForPostview;

    @Nullable
    private ProcessingNode.In  mOutputEdge;
    @Nullable
    private In mInputEdge;
    @Nullable
    private NoMetadataImageReader mNoMetadataImageReader = null;
    @NonNull
    @Override
    public ProcessingNode.In transform(@NonNull In inputEdge) {
        checkState(mInputEdge == null && mSafeCloseImageReaderProxy == null,
                "CaptureNode does not support recreation yet.");
        mInputEdge = inputEdge;
        Size size = inputEdge.getSize();
        int format = inputEdge.getInputFormat();

        // Create and configure ImageReader.
        Consumer<ProcessingRequest> requestConsumer;
        ImageReaderProxy wrappedImageReader;
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
        if (hasMetadata && inputEdge.getImageReaderProxyProvider() == null) {
            // Use MetadataImageReader if the input edge expects metadata.
            MetadataImageReader metadataImageReader = new MetadataImageReader(size.getWidth(),
                    size.getHeight(), format, MAX_IMAGES);
            cameraCaptureCallbacks =
                    CameraCaptureCallbacks.createComboCallback(
                            progressCallback, metadataImageReader.getCameraCaptureCallback());
            wrappedImageReader = metadataImageReader;
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
        inputEdge.setSurface(requireNonNull(wrappedImageReader.getSurface()));
        mSafeCloseImageReaderProxy = new SafeCloseImageReaderProxy(wrappedImageReader);

        // Listen to the input edges.
        wrappedImageReader.setOnImageAvailableListener(imageReader -> {
            try {
                ImageProxy image = imageReader.acquireLatestImage();
                if (image != null) {
                    onImageProxyAvailable(image);
                } else {
                    if (mCurrentRequest != null) {
                        sendCaptureError(
                                TakePictureManager.CaptureError.of(mCurrentRequest.getRequestId(),
                                        new ImageCaptureException(ERROR_CAPTURE_FAILED,
                                                "Failed to acquire latest image", null)));
                    }
                }
            } catch (IllegalStateException e) {
                if (mCurrentRequest != null) {
                    sendCaptureError(
                            TakePictureManager.CaptureError.of(mCurrentRequest.getRequestId(),
                                    new ImageCaptureException(
                                            ERROR_CAPTURE_FAILED, "Failed to acquire latest image",
                                            e)));
                }
            }
        }, mainThreadExecutor());

        // Postview
        if (inputEdge.getPostviewSize() != null) {
            ImageReaderProxy postviewImageReader =
                    createImageReaderProxy(inputEdge.getImageReaderProxyProvider(),
                            inputEdge.getPostviewSize().getWidth(),
                            inputEdge.getPostviewSize().getHeight(),
                            inputEdge.getPostviewImageFormat());
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
            inputEdge.setPostviewSurface(
                    postviewImageReader.getSurface(),
                    inputEdge.getPostviewSize(), inputEdge.getPostviewImageFormat());
        }

        inputEdge.getRequestEdge().setListener(requestConsumer);
        inputEdge.getErrorEdge().setListener(this::sendCaptureError);

        mOutputEdge = ProcessingNode.In.of(inputEdge.getInputFormat(), inputEdge.getOutputFormat());

        return mOutputEdge;
    }

    @NonNull
    private static ImageReaderProxy createImageReaderProxy(
            @Nullable ImageReaderProxyProvider imageReaderProxyProvider, int width, int height,
            int format) {
        if (imageReaderProxyProvider != null) {
            return imageReaderProxyProvider.newInstance(width, height, format, MAX_IMAGES, 0);
        } else {
            return ImageReaderProxys.createIsolatedReader(width, height, format, MAX_IMAGES);
        }
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
            Integer stageId = (Integer) imageProxy.getImageInfo().getTagBundle()
                    .getTag(mCurrentRequest.getTagBundleKey());
            if (stageId == null) {
                Logger.w(TAG, "Discarding ImageProxy which was acquired for aborted request");
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
        mCurrentRequest = null;
        request.onImageCaptured();
    }

    private void propagatePostviewImage(@NonNull ImageProxy imageProxy) {
        if (mCurrentRequest == null) {
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
    void sendCaptureError(@NonNull TakePictureManager.CaptureError error) {
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
                mSafeCloseImageReaderForPostview);

    }

    private void releaseInputResources(@NonNull CaptureNode.In inputEdge,
            @NonNull SafeCloseImageReaderProxy imageReader,
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
    }

    @VisibleForTesting
    @NonNull
    In getInputEdge() {
        return requireNonNull(mInputEdge);
    }

    @VisibleForTesting
    @NonNull
    public SafeCloseImageReaderProxy getSafeCloseImageReaderProxy() {
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

        @NonNull
        private CameraCaptureCallback mCameraCaptureCallback = new CameraCaptureCallback() {
        };

        @Nullable
        private DeferrableSurface mSurface;

        @Nullable
        private DeferrableSurface mPostviewSurface = null;

        /**
         * Size of the {@link ImageReader} buffer.
         */
        abstract Size getSize();

        /**
         * The input format of the pipeline. The format of the {@link ImageReader}.
         */
        abstract int getInputFormat();

        /**
         * The output format of the pipeline.
         *
         * <p> For public users, only {@link ImageFormat#JPEG} and {@link ImageFormat#JPEG_R} are
         * supported. Other formats are only used by in-memory capture in tests.
         */
        abstract int getOutputFormat();

        /**
         * Whether the pipeline is connected to a virtual camera.
         */
        abstract boolean isVirtualCamera();

        /**
         * The {@link ImageReaderProxyProvider} associated with the node. When the value exists,
         * the node will use it to create the {@link ImageReaderProxy} that connects to
         * the camera.
         */
        @Nullable
        abstract ImageReaderProxyProvider getImageReaderProxyProvider();

        /**
         * The size of the postview. Postview is configured if not null.
         */
        @Nullable
        abstract Size getPostviewSize();

        /**
         * The image format of the postview.
         */
        abstract int getPostviewImageFormat();

        /**
         * Edge that accepts {@link ProcessingRequest}.
         */
        @NonNull
        abstract Edge<ProcessingRequest> getRequestEdge();

        /**
         * Edge that accepts {@link ImageCaptureException}.
         */
        @NonNull
        abstract Edge<TakePictureManager.CaptureError> getErrorEdge();

        /**
         * Edge that accepts the image frames.
         *
         * <p>The value will be used in a capture request sent to the camera.
         */
        @NonNull
        DeferrableSurface getSurface() {
            return requireNonNull(mSurface);
        }

        /**
         * Edge that accepts the postview image frame.
         */
        @Nullable
        DeferrableSurface getPostviewSurface() {
            return mPostviewSurface;
        }


        void setSurface(@NonNull Surface surface) {
            checkState(mSurface == null, "The surface is already set.");
            mSurface = new ImmediateSurface(surface, getSize(), getInputFormat());
        }

        void setPostviewSurface(@NonNull Surface surface, @NonNull Size size, int imageFormat) {
            mPostviewSurface = new ImmediateSurface(surface, size, imageFormat);
        }

        /**
         * Edge that accepts image metadata.
         *
         * <p>The value will be used in a capture request sent to the camera.
         */
        @NonNull
        CameraCaptureCallback getCameraCaptureCallback() {
            return mCameraCaptureCallback;
        }

        void setCameraCaptureCallback(@NonNull CameraCaptureCallback cameraCaptureCallback) {
            mCameraCaptureCallback = cameraCaptureCallback;
        }

        @NonNull
        static In of(Size size, int inputFormat, int outputFormat, boolean isVirtualCamera,
                @Nullable ImageReaderProxyProvider imageReaderProxyProvider) {
            return new AutoValue_CaptureNode_In(size, inputFormat, outputFormat, isVirtualCamera,
                    imageReaderProxyProvider, null, ImageFormat.YUV_420_888,
                    new Edge<>(), new Edge<>());
        }

        @NonNull
        static In of(Size size, int inputFormat, int outputFormat, boolean isVirtualCamera,
                @Nullable ImageReaderProxyProvider imageReaderProxyProvider,
                @Nullable Size postviewSize, int postviewImageFormat) {
            return new AutoValue_CaptureNode_In(size, inputFormat, outputFormat, isVirtualCamera,
                    imageReaderProxyProvider, postviewSize, postviewImageFormat,
                    new Edge<>(), new Edge<>());
        }
    }
}
