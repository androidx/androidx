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
import static androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor;
import static androidx.core.util.Preconditions.checkState;

import static java.util.Objects.requireNonNull;

import android.os.Build;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.ForwardingImageProxy.OnImageCloseListener;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
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
public class TakePictureManager implements OnImageCloseListener {

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
        mImagePipeline.setOnImageCloseListener(this);
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
        if (mImagePipeline.getCapacity() == 0) {
            Log.d(TAG, "Too many acquire images. Close image to be able to process next.");
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
                if (throwable instanceof ImageCaptureException) {
                    cameraRequest.onCaptureFailure((ImageCaptureException) throwable);
                } else {
                    cameraRequest.onCaptureFailure(new ImageCaptureException(
                            ERROR_CAPTURE_FAILED,
                            "Failed to submit capture request",
                            throwable));
                }
                mImageCaptureControl.unlockFlashMode();
            }
        }, mainThreadExecutor());
    }

    @VisibleForTesting
    boolean hasInFlightRequest() {
        return mInFlightRequest != null;
    }

    @Override
    public void onImageClose(@NonNull ImageProxy image) {
        mainThreadExecutor().execute(this::issueNextRequest);
    }
}
