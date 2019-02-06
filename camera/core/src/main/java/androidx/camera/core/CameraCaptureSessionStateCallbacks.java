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

import android.hardware.camera2.CameraCaptureSession;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.view.Surface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Different implementations of {@link CameraCaptureSession.StateCallback}.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class CameraCaptureSessionStateCallbacks {
  /** Returns a session state callback which does nothing. */
  @RestrictTo(Scope.LIBRARY_GROUP)
  public static CameraCaptureSession.StateCallback createNoOpCallback() {
    return new NoOpSessionStateCallback();
  }

  /** Returns a session state callback which calls a list of other callbacks. */
  @RestrictTo(Scope.LIBRARY_GROUP)
  public static CameraCaptureSession.StateCallback createComboCallback(
      List<CameraCaptureSession.StateCallback> callbacks) {
    return new ComboSessionStateCallback(callbacks);
  }

  /** Returns a session state callback which calls a list of other callbacks. */
  @RestrictTo(Scope.LIBRARY_GROUP)
  public static CameraCaptureSession.StateCallback createComboCallback(
      CameraCaptureSession.StateCallback... callbacks) {
    return createComboCallback(Arrays.asList(callbacks));
  }

  private static final class NoOpSessionStateCallback extends CameraCaptureSession.StateCallback {
    @Override
    public void onConfigured(CameraCaptureSession session) {}

    @Override
    public void onActive(CameraCaptureSession session) {}

    @Override
    public void onClosed(CameraCaptureSession session) {}

    @Override
    public void onReady(CameraCaptureSession session) {}

    @Override
    public void onCaptureQueueEmpty(CameraCaptureSession session) {}

    @Override
    public void onSurfacePrepared(CameraCaptureSession session, Surface surface) {}

    @Override
    public void onConfigureFailed(CameraCaptureSession session) {}
  }

  private static final class ComboSessionStateCallback extends CameraCaptureSession.StateCallback {
    private final List<CameraCaptureSession.StateCallback> callbacks = new ArrayList<>();

    private ComboSessionStateCallback(List<CameraCaptureSession.StateCallback> callbacks) {
      for (CameraCaptureSession.StateCallback callback : callbacks) {
        // A no-op callback doesn't do anything, so avoid adding it to the final list.
        if (!(callback instanceof NoOpSessionStateCallback)) {
          this.callbacks.add(callback);
        }
      }
    }

    @Override
    public void onConfigured(CameraCaptureSession session) {
      for (CameraCaptureSession.StateCallback callback : callbacks) {
        callback.onConfigured(session);
      }
    }

    @Override
    public void onActive(CameraCaptureSession session) {
      for (CameraCaptureSession.StateCallback callback : callbacks) {
        callback.onActive(session);
      }
    }

    @Override
    public void onClosed(CameraCaptureSession session) {
      for (CameraCaptureSession.StateCallback callback : callbacks) {
        callback.onClosed(session);
      }
    }

    @Override
    public void onReady(CameraCaptureSession session) {
      for (CameraCaptureSession.StateCallback callback : callbacks) {
        callback.onReady(session);
      }
    }

    @Override
    public void onCaptureQueueEmpty(CameraCaptureSession session) {
      for (CameraCaptureSession.StateCallback callback : callbacks) {
        callback.onCaptureQueueEmpty(session);
      }
    }

    @Override
    public void onSurfacePrepared(CameraCaptureSession session, Surface surface) {
      for (CameraCaptureSession.StateCallback callback : callbacks) {
        callback.onSurfacePrepared(session, surface);
      }
    }

    @Override
    public void onConfigureFailed(CameraCaptureSession session) {
      for (CameraCaptureSession.StateCallback callback : callbacks) {
        callback.onConfigureFailed(session);
      }
    }
  }

  private CameraCaptureSessionStateCallbacks() {}
}
