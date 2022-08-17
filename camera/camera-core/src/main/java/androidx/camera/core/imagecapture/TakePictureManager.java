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
import static androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor;
import static androidx.core.util.Preconditions.checkState;

import static java.util.Objects.requireNonNull;

import android.os.Build;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Pair;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Manages {@link ImageCapture#takePicture} calls.
 *
 * <p>In coming requests are added to a queue and later sent to camera one at a time. Only one
 * in-flight request is allowed at a time. The next request cannot be sent until the current one
 * is completed by camera. However, it allows multiple concurrent requests for post-processing,
 * as {@link ImagePipeline} supports parallel processing.
 *
 * <p>This class selectively propagates callbacks from camera and {@link ImagePipeline} to the
 * app. e.g. it may choose to retry the request before sending the {@link ImageCaptureException}
 * to the app.
 *
 * <p>The thread safety is guaranteed by using the main thread.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class TakePictureManager {

    private static final String TAG = "TakePictureManager";

    // Queue of new requests that have not been sent to the pipeline/camera.
    @VisibleForTesting
    final Deque<TakePictureRequest> mNewRequests = new ArrayDeque<>();
    final ImagePipeline mImagePipeline;
    final ImageCaptureControl mImageCaptureControl;

    // The current request being processed by camera. Null if the camera is idle.
    @VisibleForTesting
    @Nullable
    RequestWithCallback mInFlightRequest;

    // Once paused, the class waits until the class is resumed to handle new requests.
    boolean mPaused = false;

    /**
     * @param imageCaptureControl for controlling {@link ImageCapture}
     * @param imagePipeline       for building capture requests and post-processing camera output.
     */
    @MainThread
    public TakePictureManager(
            @NonNull ImageCaptureControl imageCaptureControl,
            @NonNull ImagePipeline imagePipeline) {
        checkMainThread();
        mImageCaptureControl = imageCaptureControl;
        mImagePipeline = imagePipeline;
    }

    /**
     * Adds requests to the queue.
     *
     * <p>The requests in the queue will be executed based on the order being added.
     */
    @MainThread
    public void offerRequest(@NonNull TakePictureRequest takePictureRequest) {
        checkMainThread();
        mNewRequests.offer(takePictureRequest);
        issueNextRequest();
    }

    /**
     * Pauses sending request to camera.
     */
    @MainThread
    public void pause() {
        checkMainThread();
        mPaused = true;
        // TODO(b/242683221): increment the retry counter on the in-flight request. The
        //  mInFlightRequest may fail due to the pausing and need one more retry.
    }

    /**
     * Resumes sending request to camera.
     */
    @MainThread
    public void resume() {
        checkMainThread();
        mPaused = false;
        issueNextRequest();
    }

    /**
     * Clears the requests queue.
     */
    @MainThread
    public void cancelUnsentRequests() {
        checkMainThread();
        mNewRequests.clear();
    }

    /**
     * Issues the next request if conditions allow.
     */
    @MainThread
    void issueNextRequest() {
        checkMainThread();
        Log.d(TAG, "Issue the next TakePictureRequest.");
        if (hasInFlightRequest()) {
            Log.d(TAG, "There is already a request in-flight.");
            return;
        }
        if (mPaused) {
            Log.d(TAG, "The class is paused.");
            return;
        }
        TakePictureRequest request = mNewRequests.poll();
        if (request == null) {
            Log.d(TAG, "No new request.");
            return;
        }

        RequestWithCallback requestWithCallback = new RequestWithCallback(request);
        trackCurrentRequest(requestWithCallback);

        // Send requests.
        Pair<CameraRequest, ProcessingRequest> requests =
                mImagePipeline.createRequests(request, requestWithCallback);
        CameraRequest cameraRequest = requireNonNull(requests.first);
        ProcessingRequest processingRequest = requireNonNull(requests.second);
        submitCameraRequest(cameraRequest, () -> mImagePipeline.postProcess(processingRequest));
    }

    /**
     * Waits for the request to finish before issuing the next.
     */
    private void trackCurrentRequest(@NonNull RequestWithCallback requestWithCallback) {
        checkState(!hasInFlightRequest());
        mInFlightRequest = requestWithCallback;
        mInFlightRequest.getCaptureFuture().addListener(() -> {
            mInFlightRequest = null;
            issueNextRequest();
        }, directExecutor());
    }

    /**
     * Submit a request to camera and post-processing pipeline.
     *
     * <p>Flash is locked/unlocked during the flight of a {@link CameraRequest}.
     */
    @MainThread
    private void submitCameraRequest(
            @NonNull CameraRequest cameraRequest,
            @NonNull Runnable successRunnable) {
        checkMainThread();
        mImageCaptureControl.lockFlashMode();
        ListenableFuture<Void> submitRequestFuture =
                mImageCaptureControl.submitStillCaptureRequests(cameraRequest.getCaptureConfigs());
        Futures.addCallback(submitRequestFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                successRunnable.run();
                mImageCaptureControl.unlockFlashMode();
            }

            @Override
            public void onFailure(@NonNull Throwable throwable) {
                cameraRequest.onCaptureFailure(new ImageCaptureException(
                        ERROR_CAPTURE_FAILED,
                        "Failed to submit capture request",
                        throwable));
                mImageCaptureControl.unlockFlashMode();
            }
        }, directExecutor());
    }

    @VisibleForTesting
    boolean hasInFlightRequest() {
        return mInFlightRequest != null;
    }

    /**
     * A wrapper of a {@link TakePictureRequest} and its {@link TakePictureCallback}.
     *
     * <p>This is the connection between the internal callback and the app callback. This
     * connection allows us to manipulate the propagation of the callback. For example, failures
     * might be retried before sent to the app.
     */
    private static class RequestWithCallback implements TakePictureCallback {

        private final TakePictureRequest mTakePictureRequest;
        private final ListenableFuture<Void> mCaptureFuture;
        private CallbackToFutureAdapter.Completer<Void> mCaptureCompleter;
        // Tombstone flag that indicates that this callback should not be invoked anymore.
        private boolean mIsComplete = false;

        RequestWithCallback(@NonNull TakePictureRequest takePictureRequest) {
            mTakePictureRequest = takePictureRequest;
            mCaptureFuture = CallbackToFutureAdapter.getFuture(
                    completer -> {
                        mCaptureCompleter = completer;
                        return "CaptureCompleteFuture";
                    });
        }

        @MainThread
        @Override
        public void onImageCaptured() {
            checkMainThread();
            mCaptureCompleter.set(null);
            // TODO: send early callback to app.
        }

        @MainThread
        @Override
        public void onFinalResult(@Nullable ImageCapture.OutputFileResults outputFileResults) {
            checkMainThread();
            checkOnImageCaptured();
            markComplete();
            mTakePictureRequest.onResult(outputFileResults);
        }

        @MainThread
        @Override
        public void onFinalResult(@Nullable ImageProxy imageProxy) {
            checkMainThread();
            checkOnImageCaptured();
            markComplete();
            mTakePictureRequest.onResult(imageProxy);
        }


        @MainThread
        @Override
        public void onProcessFailure(@NonNull ImageCaptureException imageCaptureException) {
            checkMainThread();
            checkOnImageCaptured();
            markComplete();
            onFailure(imageCaptureException);
        }

        @MainThread
        @Override
        public void onCaptureFailure(@NonNull ImageCaptureException imageCaptureException) {
            checkMainThread();
            markComplete();
            mCaptureCompleter.set(null);

            // TODO(b/242683221): Add retry logic.
            onFailure(imageCaptureException);
        }

        /**
         * Gets a {@link ListenableFuture} that finishes when the capture is completed by camera.
         *
         * <p>Send the next request after this one completes.
         */
        @MainThread
        @NonNull
        ListenableFuture<Void> getCaptureFuture() {
            checkMainThread();
            return mCaptureFuture;
        }

        private void checkOnImageCaptured() {
            checkState(mCaptureFuture.isDone(),
                    "onImageCaptured() must be called before onFinalResult()");
        }

        private void markComplete() {
            checkState(!mIsComplete, "The callback can only complete once.");
            mIsComplete = true;
        }

        @MainThread
        private void onFailure(@NonNull ImageCaptureException imageCaptureException) {
            checkMainThread();
            mTakePictureRequest.onError(imageCaptureException);
        }
    }
}
