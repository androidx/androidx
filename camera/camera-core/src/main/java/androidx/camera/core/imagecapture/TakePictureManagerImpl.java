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

import static androidx.camera.core.ImageCapture.ERROR_CAMERA_CLOSED;
import static androidx.camera.core.ImageCapture.ERROR_CAPTURE_FAILED;
import static androidx.camera.core.impl.utils.Threads.checkMainThread;
import static androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor;
import static androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor;
import static androidx.core.util.Preconditions.checkState;

import static java.util.Objects.requireNonNull;

import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.ForwardingImageProxy.OnImageCloseListener;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.core.util.Pair;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

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
public class TakePictureManagerImpl implements TakePictureManager, OnImageCloseListener,
        TakePictureRequest.RetryControl {

    private static final String TAG = "TakePictureManagerImpl";

    // Queue of new requests that have not been sent to the pipeline/camera.
    @VisibleForTesting
    final Deque<TakePictureRequest> mNewRequests = new ArrayDeque<>();
    final ImageCaptureControl mImageCaptureControl;
    ImagePipeline mImagePipeline;

    // The current request being processed by the camera. Only one request can be processed by
    // the camera at the same time. Null if the camera is idle.
    @Nullable
    private RequestWithCallback mCapturingRequest;
    // The current requests that have not received a result or an error.
    private final List<RequestWithCallback> mIncompleteRequests;

    // Once paused, the class waits until the class is resumed to handle new requests.
    boolean mPaused = false;

    /**
     * @param imageCaptureControl for controlling {@link ImageCapture}
     */
    @MainThread
    public TakePictureManagerImpl(@NonNull ImageCaptureControl imageCaptureControl) {
        checkMainThread();
        mImageCaptureControl = imageCaptureControl;
        mIncompleteRequests = new ArrayList<>();
    }

    /**
     * Sets the {@link ImagePipeline} for building capture requests and post-processing camera
     * output.
     */
    @MainThread
    @Override
    public void setImagePipeline(@NonNull ImagePipeline imagePipeline) {
        checkMainThread();
        mImagePipeline = imagePipeline;
        mImagePipeline.setOnImageCloseListener(this);
    }

    /**
     * Adds requests to the queue.
     *
     * <p>The requests in the queue will be executed based on the order being added.
     */
    @MainThread
    @Override
    public void offerRequest(@NonNull TakePictureRequest takePictureRequest) {
        checkMainThread();
        mNewRequests.offer(takePictureRequest);
        issueNextRequest();
    }

    @MainThread
    @Override
    public void retryRequest(@NonNull TakePictureRequest request) {
        checkMainThread();
        Logger.d(TAG, "Add a new request for retrying.");
        // Insert the request to the front of the queue.
        mNewRequests.addFirst(request);
        // Try to issue the newly added request in case condition allows.
        issueNextRequest();
    }

    /**
     * Pauses sending request to camera.
     */
    @MainThread
    @Override
    public void pause() {
        checkMainThread();
        mPaused = true;

        // Always retry because the camera may not send an error callback during the reset.
        if (mCapturingRequest != null) {
            mCapturingRequest.abortSilentlyAndRetry();
        }
    }

    /**
     * Resumes sending request to camera.
     */
    @MainThread
    @Override
    public void resume() {
        checkMainThread();
        mPaused = false;
        issueNextRequest();
    }

    /**
     * Clears the requests queue.
     */
    @MainThread
    @Override
    public void abortRequests() {
        checkMainThread();
        ImageCaptureException exception =
                new ImageCaptureException(ERROR_CAMERA_CLOSED, "Camera is closed.", null);

        // Clear pending request first so aborting in-flight request won't trigger another capture.
        for (TakePictureRequest request : mNewRequests) {
            request.onError(exception);
        }
        mNewRequests.clear();

        // Abort the in-flight request after clearing the pending requests.
        // Snapshot to avoid concurrent modification with the removal in getCompleteFuture().
        List<RequestWithCallback> requestsSnapshot = new ArrayList<>(mIncompleteRequests);
        for (RequestWithCallback request : requestsSnapshot) {
            // TODO: optimize the performance by not processing aborted requests.
            request.abortAndSendErrorToApp(exception);
        }
    }

    /**
     * Issues the next request if conditions allow.
     */
    @MainThread
    void issueNextRequest() {
        checkMainThread();
        Log.d(TAG, "Issue the next TakePictureRequest.");
        if (hasCapturingRequest()) {
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

        RequestWithCallback requestWithCallback = new RequestWithCallback(request, this);
        trackCurrentRequests(requestWithCallback);

        // Send requests.
        Pair<CameraRequest, ProcessingRequest> requests =
                mImagePipeline.createRequests(request, requestWithCallback,
                        requestWithCallback.getCaptureFuture());
        CameraRequest cameraRequest = requireNonNull(requests.first);
        ProcessingRequest processingRequest = requireNonNull(requests.second);
        mImagePipeline.submitProcessingRequest(processingRequest);
        ListenableFuture<Void> captureRequestFuture = submitCameraRequest(cameraRequest);
        requestWithCallback.setCaptureRequestFuture(captureRequestFuture);
    }

    /**
     * Waits for the request to finish before issuing the next.
     */
    private void trackCurrentRequests(@NonNull RequestWithCallback requestWithCallback) {
        checkState(!hasCapturingRequest());
        mCapturingRequest = requestWithCallback;

        // Waits for the capture to finish before issuing the next.
        mCapturingRequest.getCaptureFuture().addListener(() -> {
            mCapturingRequest = null;
            issueNextRequest();
        }, directExecutor());

        // Track all incomplete requests so we can abort them when UseCase is detached.
        mIncompleteRequests.add(requestWithCallback);
        requestWithCallback.getCompleteFuture().addListener(() -> {
            mIncompleteRequests.remove(requestWithCallback);
        }, directExecutor());
    }

    /**
     * Submit a request to camera and post-processing pipeline.
     *
     * <p>Flash is locked/unlocked during the flight of a {@link CameraRequest}.
     */
    @MainThread
    private ListenableFuture<Void> submitCameraRequest(
            @NonNull CameraRequest cameraRequest) {
        checkMainThread();
        mImageCaptureControl.lockFlashMode();
        ListenableFuture<Void> captureRequestFuture =
                mImageCaptureControl.submitStillCaptureRequests(cameraRequest.getCaptureConfigs());
        Futures.addCallback(captureRequestFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                mImageCaptureControl.unlockFlashMode();
            }

            @Override
            public void onFailure(@NonNull Throwable throwable) {
                if (cameraRequest.isAborted()) {
                    // When the pipeline is recreated, the in-flight request is aborted and
                    // retried. On legacy devices, the camera may return CancellationException
                    // for the aborted request which causes the retried request to fail. Return
                    // early if the request has been aborted.
                    return;
                } else {
                    int requestId = cameraRequest.getCaptureConfigs().get(0).getId();
                    if (throwable instanceof ImageCaptureException) {
                        mImagePipeline.notifyCaptureError(
                                CaptureError.of(requestId, (ImageCaptureException) throwable));
                    } else {
                        mImagePipeline.notifyCaptureError(
                                CaptureError.of(requestId, new ImageCaptureException(
                                        ERROR_CAPTURE_FAILED,
                                        "Failed to submit capture request",
                                        throwable)));
                    }
                }
                mImageCaptureControl.unlockFlashMode();
            }
        }, mainThreadExecutor());
        return captureRequestFuture;
    }

    @VisibleForTesting
    @Override
    public boolean hasCapturingRequest() {
        return mCapturingRequest != null;
    }

    @VisibleForTesting
    @Nullable
    @Override
    public RequestWithCallback getCapturingRequest() {
        return mCapturingRequest;
    }

    @NonNull
    @VisibleForTesting
    @Override
    public List<RequestWithCallback> getIncompleteRequests() {
        return mIncompleteRequests;
    }

    @VisibleForTesting
    @NonNull
    @Override
    public ImagePipeline getImagePipeline() {
        return mImagePipeline;
    }

    @Override
    public void onImageClose(@NonNull ImageProxy image) {
        mainThreadExecutor().execute(this::issueNextRequest);
    }
}
