/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.camera2.internal;

import android.hardware.camera2.CameraCaptureSession;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.ApiCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Different implementations of {@link CameraCaptureSession.StateCallback}.
 */
public final class CameraCaptureSessionStateCallbacks {
    private CameraCaptureSessionStateCallbacks() {
    }

    /**
     * Returns a session state callback which does nothing.
     **/
    @NonNull
    public static CameraCaptureSession.StateCallback createNoOpCallback() {
        return new NoOpSessionStateCallback();
    }

    /**
     * Returns a session state callback which calls a list of other callbacks.
     */
    @NonNull
    public static CameraCaptureSession.StateCallback createComboCallback(
            @NonNull List<CameraCaptureSession.StateCallback> callbacks) {
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
    @NonNull
    public static CameraCaptureSession.StateCallback createComboCallback(
            @NonNull CameraCaptureSession.StateCallback... callbacks) {
        return createComboCallback(Arrays.asList(callbacks));
    }

    static final class NoOpSessionStateCallback extends CameraCaptureSession.StateCallback {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
        }

        @Override
        public void onActive(@NonNull CameraCaptureSession session) {
        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
        }

        @Override
        public void onReady(@NonNull CameraCaptureSession session) {
        }

        @Override
        public void onCaptureQueueEmpty(@NonNull CameraCaptureSession session) {
        }

        @Override
        public void onSurfacePrepared(@NonNull CameraCaptureSession session,
                @NonNull Surface surface) {
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
        }
    }

    static final class ComboSessionStateCallback
            extends CameraCaptureSession.StateCallback {
        private final List<CameraCaptureSession.StateCallback> mCallbacks = new ArrayList<>();

        ComboSessionStateCallback(@NonNull List<CameraCaptureSession.StateCallback> callbacks) {
            for (CameraCaptureSession.StateCallback callback : callbacks) {
                // A no-op callback doesn't do anything, so avoid adding it to the final list.
                if (!(callback instanceof NoOpSessionStateCallback)) {
                    mCallbacks.add(callback);
                }
            }
        }

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            for (CameraCaptureSession.StateCallback callback : mCallbacks) {
                callback.onConfigured(session);
            }
        }

        @Override
        public void onActive(@NonNull CameraCaptureSession session) {
            for (CameraCaptureSession.StateCallback callback : mCallbacks) {
                callback.onActive(session);
            }
        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
            for (CameraCaptureSession.StateCallback callback : mCallbacks) {
                callback.onClosed(session);
            }
        }

        @Override
        public void onReady(@NonNull CameraCaptureSession session) {
            for (CameraCaptureSession.StateCallback callback : mCallbacks) {
                callback.onReady(session);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onCaptureQueueEmpty(@NonNull CameraCaptureSession session) {
            for (CameraCaptureSession.StateCallback callback : mCallbacks) {
                ApiCompat.Api26Impl.onCaptureQueueEmpty(callback, session);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onSurfacePrepared(@NonNull CameraCaptureSession session,
                @NonNull Surface surface) {
            for (CameraCaptureSession.StateCallback callback : mCallbacks) {
                ApiCompat.Api23Impl.onSurfacePrepared(callback, session, surface);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            for (CameraCaptureSession.StateCallback callback : mCallbacks) {
                callback.onConfigureFailed(session);
            }
        }
    }
}
