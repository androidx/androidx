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
import android.os.Build;
import android.view.Surface;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

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
    private CameraCaptureSessionStateCallbacks() {
    }

    /**
     * Returns a session state callback which does nothing.
     **/
    public static CameraCaptureSession.StateCallback createNoOpCallback() {
        return new NoOpSessionStateCallback();
    }

    /**
     * Returns a session state callback which calls a list of other callbacks.
     */
    public static CameraCaptureSession.StateCallback createComboCallback(
            List<CameraCaptureSession.StateCallback> callbacks) {
        if (callbacks.isEmpty()) {
            return createNoOpCallback();
        } else if (callbacks.size() == 1) {
            return callbacks.get(0);
        }
        return new ComboSessionStateCallback(callbacks);
    }

    /**
     * Returns a session state callback which calls a list of other callbacks.
     */
    public static CameraCaptureSession.StateCallback createComboCallback(
            CameraCaptureSession.StateCallback... callbacks) {
        return createComboCallback(Arrays.asList(callbacks));
    }

    static final class NoOpSessionStateCallback extends CameraCaptureSession.StateCallback {
        @Override
        public void onConfigured(CameraCaptureSession session) {
        }

        @Override
        public void onActive(CameraCaptureSession session) {
        }

        @Override
        public void onClosed(CameraCaptureSession session) {
        }

        @Override
        public void onReady(CameraCaptureSession session) {
        }

        @Override
        public void onCaptureQueueEmpty(CameraCaptureSession session) {
        }

        @Override
        public void onSurfacePrepared(CameraCaptureSession session, Surface surface) {
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
        }
    }

    static final class ComboSessionStateCallback
            extends CameraCaptureSession.StateCallback {
        private final List<CameraCaptureSession.StateCallback> mCallbacks = new ArrayList<>();

        ComboSessionStateCallback(List<CameraCaptureSession.StateCallback> callbacks) {
            for (CameraCaptureSession.StateCallback callback : callbacks) {
                // A no-op callback doesn't do anything, so avoid adding it to the final list.
                if (!(callback instanceof NoOpSessionStateCallback)) {
                    mCallbacks.add(callback);
                }
            }
        }

        @Override
        public void onConfigured(CameraCaptureSession session) {
            for (CameraCaptureSession.StateCallback callback : mCallbacks) {
                callback.onConfigured(session);
            }
        }

        @Override
        public void onActive(CameraCaptureSession session) {
            for (CameraCaptureSession.StateCallback callback : mCallbacks) {
                callback.onActive(session);
            }
        }

        @Override
        public void onClosed(CameraCaptureSession session) {
            for (CameraCaptureSession.StateCallback callback : mCallbacks) {
                callback.onClosed(session);
            }
        }

        @Override
        public void onReady(CameraCaptureSession session) {
            for (CameraCaptureSession.StateCallback callback : mCallbacks) {
                callback.onReady(session);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onCaptureQueueEmpty(CameraCaptureSession session) {
            for (CameraCaptureSession.StateCallback callback : mCallbacks) {
                callback.onCaptureQueueEmpty(session);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onSurfacePrepared(CameraCaptureSession session, Surface surface) {
            for (CameraCaptureSession.StateCallback callback : mCallbacks) {
                callback.onSurfacePrepared(session, surface);
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            for (CameraCaptureSession.StateCallback callback : mCallbacks) {
                callback.onConfigureFailed(session);
            }
        }
    }
}
