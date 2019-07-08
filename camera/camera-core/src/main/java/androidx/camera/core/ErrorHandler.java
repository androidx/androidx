/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.CameraX.ErrorCode;
import androidx.camera.core.CameraX.ErrorListener;

/**
 * Handler for sending and receiving error messages.
 *
 * @hide Only internal classes should post error messages
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class ErrorHandler {
    private static final String TAG = "ErrorHandler";

    private final Object mErrorLock = new Object();

    @GuardedBy("mErrorLock")
    private ErrorListener mListener = new PrintingErrorListener();

    @GuardedBy("mErrorLock")
    private Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * Posts an error message.
     *
     * @param error   the type of error that occurred
     * @param message detailed message of the error condition
     */
    void postError(final ErrorCode error, final String message) {
        synchronized (mErrorLock) {
            final ErrorListener listenerReference = mListener;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    listenerReference.onError(error, message);
                }
            });
        }
    }

    /**
     * Sets the listener for the error.
     *
     * @param listener the listener which should handle the error condition
     * @param handler  the handler on which to run the listener
     */
    void setErrorListener(ErrorListener listener, Handler handler) {
        synchronized (mErrorLock) {
            if (handler == null) {
                mHandler = new Handler(Looper.getMainLooper());
            } else {
                mHandler = handler;
            }
            if (listener == null) {
                mListener = new PrintingErrorListener();
            } else {
                mListener = listener;
            }
        }
    }

    /** An error listener which logs the error message and returns. */
    static final class PrintingErrorListener implements ErrorListener {
        @Override
        public void onError(@NonNull ErrorCode error, @NonNull String message) {
            Log.e(TAG, "ErrorHandler occurred: " + error + " with message: " + message);
        }
    }
}
