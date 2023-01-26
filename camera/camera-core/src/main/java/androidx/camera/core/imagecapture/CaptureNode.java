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

import android.media.ImageReader;
import android.os.Build;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.ForwardingImageProxy;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Logger;
import androidx.camera.core.MetadataImageReader;
import androidx.camera.core.SafeCloseImageReaderProxy;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.ImmediateSurface;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.core.processing.Edge;
import androidx.camera.core.processing.Node;

import com.google.auto.value.AutoValue;

import java.util.HashSet;
import java.util.Set;

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
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class CaptureNode implements Node<CaptureNode.In, CaptureNode.Out> {

    private static final String TAG = "CaptureNode";

    // TODO: we might need to calculate this number dynamically based on on many frames are
    //  needed by the post-processing. e.g. night mode might need to merge 10+ frames. 4 images
    //  should be enough for now.
    @VisibleForTesting
    static final int MAX_IMAGES = 4;

    @NonNull
    private final Set<Integer> mPendingStageIds = new HashSet<>();
    ProcessingRequest mCurrentRequest = null;

    @Nullable
    SafeCloseImageReaderProxy mSafeCloseImageReaderProxy;
    @Nullable
    private Out mOutputEdge;
    @Nullable
    private In mInputEdge;

    @NonNull
    @Override
    public Out transform(@NonNull In inputEdge) {
        checkState(mInputEdge == null && mSafeCloseImageReaderProxy == null,
                "CaptureNode does not support recreation yet.");
        mInputEdge = inputEdge;
        Size size = inputEdge.getSize();
        int format = inputEdge.getFormat();

        // Creates ImageReaders.
        MetadataImageReader metadataImageReader = new MetadataImageReader(size.getWidth(),
                size.getHeight(), format, MAX_IMAGES);
        mSafeCloseImageReaderProxy = new SafeCloseImageReaderProxy(metadataImageReader);
        inputEdge.setCameraCaptureCallback(metadataImageReader.getCameraCaptureCallback());
        inputEdge.setSurface(requireNonNull(metadataImageReader.getSurface()));

        // Listen to the input edges.
        metadataImageReader.setOnImageAvailableListener(imageReader -> {
            try {
                ImageProxy image = imageReader.acquireLatestImage();
                if (image != null) {
                    onImageProxyAvailable(image);
                } else {
                    sendCaptureError(new ImageCaptureException(ERROR_CAPTURE_FAILED, "Failed to "
                            + "acquire latest image", null));
                }
            } catch (IllegalStateException e) {
                sendCaptureError(new ImageCaptureException(ERROR_CAPTURE_FAILED, "Failed to "
                        + "acquire latest image", e));
            }
        }, mainThreadExecutor());
        inputEdge.getRequestEdge().setListener(this::onRequestAvailable);
        inputEdge.getErrorEdge().setListener(this::sendCaptureError);

        mOutputEdge = Out.of(inputEdge.getFormat());
        return mOutputEdge;
    }

    @VisibleForTesting
    @MainThread
    void onImageProxyAvailable(@NonNull ImageProxy imageProxy) {
        checkMainThread();
        if (mCurrentRequest == null) {
            Logger.d(TAG, "Discarding ImageProxy which was inadvertently acquired: " + imageProxy);
            imageProxy.close();
        } else {
            // Match image and send it downstream.
            matchAndPropagateImage(imageProxy);
        }
    }

    private void matchAndPropagateImage(@NonNull ImageProxy imageProxy) {
        // Check if the capture is complete.
        int stageId = (Integer) requireNonNull(
                imageProxy.getImageInfo().getTagBundle().getTag(mCurrentRequest.getTagBundleKey()));
        checkState(mPendingStageIds.contains(stageId),
                "Received an unexpected stage id" + stageId);
        mPendingStageIds.remove(stageId);

        // Send the image downstream.
        requireNonNull(mOutputEdge).getImageEdge().accept(imageProxy);

        if (mPendingStageIds.isEmpty()) {
            // The capture is complete. Let the pipeline know it can take another picture.
            ProcessingRequest request = mCurrentRequest;
            mCurrentRequest = null;
            request.onImageCaptured();
        }
    }

    @VisibleForTesting
    @MainThread
    void onRequestAvailable(@NonNull ProcessingRequest request) {
        checkMainThread();
        // Unable to issue request if the queue has no capacity.
        checkState(getCapacity() > 0,
                "Too many acquire images. Close image to be able to process next.");
        // Check if there is already a current request. Only one concurrent request is allowed.
        checkState(mCurrentRequest == null || mPendingStageIds.isEmpty(),
                "The previous request is not complete");

        // Track the request and its stage IDs.
        mCurrentRequest = request;
        mPendingStageIds.addAll(request.getStageIds());

        // Send the request downstream.
        requireNonNull(mOutputEdge).getRequestEdge().accept(request);
        Futures.addCallback(request.getCaptureFuture(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                // Do nothing
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                checkMainThread();
                if (request == mCurrentRequest) {
                    mCurrentRequest = null;
                }
            }
        }, CameraXExecutors.directExecutor());
    }

    @MainThread
    void sendCaptureError(@NonNull ImageCaptureException e) {
        checkMainThread();
        if (mCurrentRequest != null) {
            mCurrentRequest.onCaptureFailure(e);
        }
    }

    @MainThread
    @Override
    public void release() {
        checkMainThread();
        releaseInputResources(requireNonNull(mInputEdge),
                requireNonNull(mSafeCloseImageReaderProxy));
    }

    private void releaseInputResources(@NonNull CaptureNode.In inputEdge,
            @NonNull SafeCloseImageReaderProxy imageReader) {
        inputEdge.getSurface().close();
        // Wait for the termination to close the ImageReader or the Surface may be released
        // prematurely before it can be used by camera2.
        inputEdge.getSurface().getTerminationFuture().addListener(
                imageReader::safeClose, mainThreadExecutor());
    }

    @VisibleForTesting
    @NonNull
    In getInputEdge() {
        return requireNonNull(mInputEdge);
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

        private CameraCaptureCallback mCameraCaptureCallback;

        private DeferrableSurface mSurface;

        /**
         * Size of the {@link ImageReader} buffer.
         */
        abstract Size getSize();

        /**
         * Size of the {@link ImageReader} format.
         */
        abstract int getFormat();

        /**
         * Edge that accepts {@link ProcessingRequest}.
         */
        @NonNull
        abstract Edge<ProcessingRequest> getRequestEdge();

        /**
         * Edge that accepts {@link ImageCaptureException}.
         */
        @NonNull
        abstract Edge<ImageCaptureException> getErrorEdge();

        /**
         * Edge that accepts the image frames.
         *
         * <p>The value will be used in a capture request sent to the camera.
         */
        @NonNull
        DeferrableSurface getSurface() {
            return mSurface;
        }

        void setSurface(@NonNull Surface surface) {
            checkState(mSurface == null, "The surface is already set.");
            mSurface = new ImmediateSurface(surface);
        }

        /**
         * Edge that accepts image metadata.
         *
         * <p>The value will be used in a capture request sent to the camera.
         */
        CameraCaptureCallback getCameraCaptureCallback() {
            return mCameraCaptureCallback;
        }

        void setCameraCaptureCallback(@NonNull CameraCaptureCallback cameraCaptureCallback) {
            mCameraCaptureCallback = cameraCaptureCallback;
        }

        @NonNull
        static In of(Size size, int format) {
            return new AutoValue_CaptureNode_In(size, format, new Edge<>(), new Edge<>());
        }
    }

    /**
     * Output edges of a {@link CaptureNode}.
     */
    @AutoValue
    abstract static class Out {

        /**
         * Edge that omits {@link ImageProxy}s.
         *
         * <p>The frames will be closed by downstream nodes.
         */
        abstract Edge<ImageProxy> getImageEdge();

        /**
         * Edge that omits {@link ProcessingRequest}.
         */
        abstract Edge<ProcessingRequest> getRequestEdge();

        /**
         * Format of the {@link ImageProxy} in {@link #getImageEdge()}.
         */
        abstract int getFormat();

        static Out of(int format) {
            return new AutoValue_CaptureNode_Out(new Edge<>(), new Edge<>(), format);
        }
    }
}
