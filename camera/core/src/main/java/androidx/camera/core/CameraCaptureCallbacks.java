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

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Different implementations of {@link CameraCaptureCallback}.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class CameraCaptureCallbacks {
  /** Returns a camera capture callback which does nothing. */
  public static CameraCaptureCallback createNoOpCallback() {
    return new NoOpCameraCaptureCallback();
  }

  /** Returns a camera capture callback which calls a list of other callbacks. */
  static CameraCaptureCallback createComboCallback(List<CameraCaptureCallback> callbacks) {
    return new ComboCameraCaptureCallback(callbacks);
  }

  /** Returns a camera capture callback which calls a list of other callbacks. */
  public static CameraCaptureCallback createComboCallback(CameraCaptureCallback... callbacks) {
    return createComboCallback(Arrays.asList(callbacks));
  }

  private static final class NoOpCameraCaptureCallback extends CameraCaptureCallback {
    @Override
    public void onCaptureCompleted(CameraCaptureResult cameraCaptureResult) {}

    @Override
    public void onCaptureFailed(CameraCaptureFailure failure) {}
  }

  /**
   * A CameraCaptureCallback which contains a list of CameraCaptureCallback and will propagate
   * received callback to the list.
   */
  public static final class ComboCameraCaptureCallback extends CameraCaptureCallback {
    private final List<CameraCaptureCallback> callbacks = new ArrayList<>();

    private ComboCameraCaptureCallback(List<CameraCaptureCallback> callbacks) {
      for (CameraCaptureCallback callback : callbacks) {
        // A no-op callback doesn't do anything, so avoid adding it to the final list.
        if (!(callback instanceof NoOpCameraCaptureCallback)) {
          this.callbacks.add(callback);
        }
      }
    }

    @Override
    public void onCaptureCompleted(CameraCaptureResult result) {
      for (CameraCaptureCallback callback : callbacks) {
        callback.onCaptureCompleted(result);
      }
    }

    @Override
    public void onCaptureFailed(CameraCaptureFailure failure) {
      for (CameraCaptureCallback callback : callbacks) {
        callback.onCaptureFailed(failure);
      }
    }

    @NonNull
    public List<CameraCaptureCallback> getCallbacks() {
      return callbacks;
    }
  }

  private CameraCaptureCallbacks() {}
}
