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

import android.os.Build;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
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
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class RequestWithCallback implements TakePictureCallback {

    private final TakePictureRequest mTakePictureRequest;
    private final ListenableFuture<Void> mCaptureFuture;
    private CallbackToFutureAdapter.Completer<Void> mCaptureCompleter;
    // Tombstone flag that indicates that this callback should not be invoked anymore.
    private boolean mIsComplete = false;
    // Flag tracks if the request has been aborted by the UseCase. Once aborted, this class stops
    // propagating callbacks to the app.
    private boolean mIsAborted = false;

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
        if (mIsAborted) {
            // Ignore. mCaptureFuture should have been completed by the #abort() call.
            return;
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
            // Do not deliver result if the request has been aborted.
            return;
        }
        checkOnImageCaptured();
        markComplete();
        mTakePictureRequest.onResult(imageProxy);
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
        markComplete();
        mCaptureCompleter.set(null);

        // TODO(b/242683221): Add retry logic.
        onFailure(imageCaptureException);
    }

    @MainThread
    void abort(@NonNull ImageCaptureException imageCaptureException) {
        checkMainThread();
        mIsAborted = true;
        mCaptureCompleter.set(null);
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
