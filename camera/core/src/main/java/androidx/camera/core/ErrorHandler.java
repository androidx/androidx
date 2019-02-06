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
import android.support.annotation.GuardedBy;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.util.Log;
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

  private final Object errorLock = new Object();

  @GuardedBy("errorLock")
  private ErrorListener listener = new PrintingErrorListener();

  @GuardedBy("errorLock")
  private Handler handler = new Handler(Looper.getMainLooper());

  /**
   * Posts an error message.
   *
   * @param error the type of error that occurred
   * @param message detailed message of the error condition
   */
  void postError(ErrorCode error, String message) {
    synchronized (errorLock) {
      ErrorListener listenerReference = listener;
      handler.post(() -> listenerReference.onError(error, message));
    }
  }

  /**
   * Sets the listener for the error.
   *
   * @param listener the listener which should handle the error condition
   * @param handler the handler on which to run the listener
   */
  void setErrorListener(ErrorListener listener, Handler handler) {
    synchronized (errorLock) {
      if (handler == null) {
        this.handler = new Handler(Looper.getMainLooper());
      } else {
        this.handler = handler;
      }
      if (listener == null) {
        this.listener = new PrintingErrorListener();
      } else {
        this.listener = listener;
      }
    }
  }

  /** An error listener which logs the error message and returns. */
  static final class PrintingErrorListener implements ErrorListener {
    @Override
    public void onError(ErrorCode error, String message) {
      Log.e(TAG, "ErrorHandler occurred: " + error + " with message: " + message);
    }
  }

}
