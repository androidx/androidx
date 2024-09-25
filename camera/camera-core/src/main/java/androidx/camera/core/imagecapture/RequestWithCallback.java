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

import static androidx.camera.core.impl.utils.Threads.checkMainThread;
import static androidx.core.util.Preconditions.checkState;

import static java.util.Objects.requireNonNull;

import android.graphics.Bitmap;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * A wrapper of a {@link TakePictureRequest} and its {@link TakePictureCallback}.
 *
 * <p>This is the connection between the internal callback and the app callback. This
 * connection allows us to manipulate the propagation of the callback. For example, failures
 * might be retried before sent to the app.
 */
public class RequestWithCallback implements TakePictureCallback {

    private final TakePictureRequest mTakePictureRequest;
    private final TakePictureRequest.RetryControl mRetryControl;
    private final ListenableFuture<Void> mCaptureFuture;
    private final ListenableFuture<Void> mCompleteFuture;
    private CallbackToFutureAdapter.Completer<Void> mCaptureCompleter;
    private CallbackToFutureAdapter.Completer<Void> mCompleteCompleter;

    // Flag tracks if the request has been aborted by the UseCase. Once aborted, this class stops
    // propagating callbacks to the app.
    private boolean mIsAborted = false;
    private boolean mIsStarted = false;
    @Nullable
    private ListenableFuture<Void> mCaptureRequestFuture;

    RequestWithCallback(@NonNull TakePictureRequest takePictureRequest,
            @NonNull TakePictureRequest.RetryControl retryControl) {
        mTakePictureRequest = takePictureRequest;
        mRetryControl = retryControl;
        mCaptureFuture = CallbackToFutureAdapter.getFuture(
                completer -> {
                    mCaptureCompleter = completer;
                    return "CaptureCompleteFuture";
                });
        mCompleteFuture = CallbackToFutureAdapter.getFuture(
                completer -> {
                    mCompleteCompleter = completer;
                    return "RequestCompleteFuture";
                });
    }

    /**
     * Sets the {@link ListenableFuture} associated with camera2 capture request.
     *
     * <p>Canceling this future should cancel the request sent to camera2.
     */
    @MainThread
    public void setCaptureRequestFuture(@NonNull ListenableFuture<Void> captureRequestFuture) {
        checkMainThread();
        checkState(mCaptureRequestFuture == null, "CaptureRequestFuture can only be set once.");
        mCaptureRequestFuture = captureRequestFuture;
    }

    @MainThread
    @Override
    public void onCaptureStarted() {
        checkMainThread();
        if (mIsAborted || mIsStarted) {
            // Ignore the event if the request has been aborted or started.
            return;
        }
        mIsStarted = true;

        ImageCapture.OnImageCapturedCallback inMemoryCallback =
                mTakePictureRequest.getInMemoryCallback();
        if (inMemoryCallback != null) {
            inMemoryCallback.onCaptureStarted();
        }

        ImageCapture.OnImageSavedCallback onDiskCallback = mTakePictureRequest.getOnDiskCallback();
        if (onDiskCallback != null) {
            onDiskCallback.onCaptureStarted();
        }
    }

    @MainThread
    @Override
    public void onImageCaptured() {
        checkMainThread();
        if (mIsAborted) {
            // Ignore. mCaptureFuture should have been completed by the #abort() call.
            return;
        }
        if (!mIsStarted) {
            // Send the started event if the capture is completed but hasn't been started yet.
            onCaptureStarted();
        }

        mCaptureCompleter.set(null);
        // TODO: send early callback to app.
    }

    @MainThread
    @Override
    public void onFinalResult(@NonNull ImageCapture.OutputFileResults outputFileResults) {
        checkMainThread();
        if (mIsAborted) {
            // Do not deliver result if the request has been aborted.
            // TODO: delete the saved file when the request is aborted.
            return;
        }
        checkOnImageCaptured();
        markComplete();
        mTakePictureRequest.onResult(outputFileResults);
    }

    @MainThread
    @Override
    public void onFinalResult(@NonNull ImageProxy imageProxy) {
        checkMainThread();
        if (mIsAborted) {
            imageProxy.close();
            // Do not deliver result if the request has been aborted.
            return;
        }
        checkOnImageCaptured();
        markComplete();
        mTakePictureRequest.onResult(imageProxy);
    }

    @Override
    public void onCaptureProcessProgressed(int progress) {
        checkMainThread();
        if (mIsAborted) {
            return;
        }

        mTakePictureRequest.onCaptureProcessProgressed(progress);
    }

    @Override
    public void onPostviewBitmapAvailable(@NonNull Bitmap bitmap) {
        checkMainThread();
        if (mIsAborted) {
            // Do not deliver result if the request has been aborted.
            return;
        }

        mTakePictureRequest.onPostviewBitmapAvailable(bitmap);
    }

    @MainThread
    @Override
    public void onProcessFailure(@NonNull ImageCaptureException imageCaptureException) {
        checkMainThread();
        if (mIsAborted) {
            // Fail silently if the request has been aborted.
            return;
        }
        checkOnImageCaptured();
        markComplete();
        onFailure(imageCaptureException);
    }

    @Override
    public boolean isAborted() {
        return mIsAborted;
    }

    @MainThread
    @Override
    public void onCaptureFailure(@NonNull ImageCaptureException imageCaptureException) {
        checkMainThread();
        if (mIsAborted) {
            // Fail silently if the request has been aborted.
            return;
        }

        boolean isRetryAllowed = mTakePictureRequest.decrementRetryCounter();
        if (!isRetryAllowed) {
            onFailure(imageCaptureException);
        }
        markComplete();
        mCaptureCompleter.setException(imageCaptureException);

        if (isRetryAllowed) {
            // retry after all the cleaning up works are done via mCaptureCompleter.setException,
            // e.g. removing previous request from CaptureNode, SingleBundlingNode etc.
            mRetryControl.retryRequest(mTakePictureRequest);
        }
    }

    @MainThread
    void abortAndSendErrorToApp(@NonNull ImageCaptureException imageCaptureException) {
        checkMainThread();
        if (mCompleteFuture.isDone()) {
            // The app has already received a callback. No need to abort.
            return;
        }
        abort(imageCaptureException);
        onFailure(imageCaptureException);
    }

    @MainThread
    void abortSilentlyAndRetry() {
        checkMainThread();
        if (mCompleteFuture.isDone()) {
            // The app has already received a callback. No need to abort.
            return;
        }
        abort(new ImageCaptureException(ImageCapture.ERROR_CAMERA_CLOSED,
                "The request is aborted silently and retried.", null));
        mRetryControl.retryRequest(mTakePictureRequest);
    }

    @MainThread
    private void abort(@NonNull ImageCaptureException imageCaptureException) {
        checkMainThread();
        mIsAborted = true;
        // Cancel the capture request sent to camera2.
        requireNonNull(mCaptureRequestFuture).cancel(true);
        mCaptureCompleter.setException(imageCaptureException);
        mCompleteCompleter.set(null);
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

    /**
     * Gets a {@link ListenableFuture} that finishes when the capture is completed.
     *
     * <p>A request is completed when it gets either a result or an unrecoverable error.
     */
    @MainThread
    @NonNull
    ListenableFuture<Void> getCompleteFuture() {
        checkMainThread();
        return mCompleteFuture;
    }

    @VisibleForTesting
    @NonNull
    public TakePictureRequest getTakePictureRequest() {
        return mTakePictureRequest;
    }

    private void checkOnImageCaptured() {
        checkState(mCaptureFuture.isDone(),
                "onImageCaptured() must be called before onFinalResult()");
    }

    private void markComplete() {
        if (mTakePictureRequest.isSimultaneousCapture()
                && !mTakePictureRequest.isFormatProcessedInSimultaneousCapture()) {
            return;
        }
        if (!mTakePictureRequest.isSimultaneousCapture()) {
            checkState(!mCompleteFuture.isDone(), "The callback can only complete once.");
        }
        mCompleteCompleter.set(null);
    }

    @MainThread
    private void onFailure(@NonNull ImageCaptureException imageCaptureException) {
        checkMainThread();
        mTakePictureRequest.onError(imageCaptureException);
    }
}
