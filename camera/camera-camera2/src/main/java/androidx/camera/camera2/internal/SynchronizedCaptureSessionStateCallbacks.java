/*
 * Copyright 2020 The Android Open Source Project
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

final class SynchronizedCaptureSessionStateCallbacks extends
        SynchronizedCaptureSession.StateCallback {

    private final List<SynchronizedCaptureSession.StateCallback> mCallbacks = new ArrayList<>();

    /**
     * Returns a session state callback which calls a list of other callbacks.
     */
    @NonNull
    static SynchronizedCaptureSession.StateCallback createComboCallback(
            @NonNull SynchronizedCaptureSession.StateCallback... callbacks) {
        return new SynchronizedCaptureSessionStateCallbacks(Arrays.asList(callbacks));
    }

    SynchronizedCaptureSessionStateCallbacks(
            @NonNull List<SynchronizedCaptureSession.StateCallback> callbacks) {
        mCallbacks.addAll(callbacks);
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onSurfacePrepared(@NonNull SynchronizedCaptureSession session,
            @NonNull Surface surface) {
        for (SynchronizedCaptureSession.StateCallback callback : mCallbacks) {
            callback.onSurfacePrepared(session, surface);
        }
    }

    @Override
    public void onReady(@NonNull SynchronizedCaptureSession session) {
        for (SynchronizedCaptureSession.StateCallback callback : mCallbacks) {
            callback.onReady(session);
        }
    }

    @Override
    public void onActive(@NonNull SynchronizedCaptureSession session) {
        for (SynchronizedCaptureSession.StateCallback callback : mCallbacks) {
            callback.onActive(session);
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onCaptureQueueEmpty(@NonNull SynchronizedCaptureSession session) {
        for (SynchronizedCaptureSession.StateCallback callback : mCallbacks) {
            callback.onCaptureQueueEmpty(session);
        }
    }

    @Override
    public void onConfigured(@NonNull SynchronizedCaptureSession session) {
        for (SynchronizedCaptureSession.StateCallback callback : mCallbacks) {
            callback.onConfigured(session);
        }
    }

    @Override
    public void onConfigureFailed(@NonNull SynchronizedCaptureSession session) {
        for (SynchronizedCaptureSession.StateCallback callback : mCallbacks) {
            callback.onConfigureFailed(session);
        }
    }

    @Override
    public void onClosed(@NonNull SynchronizedCaptureSession session) {
        for (SynchronizedCaptureSession.StateCallback callback : mCallbacks) {
            callback.onClosed(session);
        }
    }

    @Override
    void onSessionFinished(@NonNull SynchronizedCaptureSession session) {
        for (SynchronizedCaptureSession.StateCallback callback : mCallbacks) {
            callback.onSessionFinished(session);
        }
    }

    static class Adapter extends SynchronizedCaptureSession.StateCallback {
        @NonNull
        private final CameraCaptureSession.StateCallback mCameraCaptureSessionStateCallback;

        Adapter(@NonNull CameraCaptureSession.StateCallback cameraCaptureSessionStateCallback) {
            mCameraCaptureSessionStateCallback = cameraCaptureSessionStateCallback;
        }

        Adapter(@NonNull List<CameraCaptureSession.StateCallback> callbackList) {
            this(CameraCaptureSessionStateCallbacks.createComboCallback(callbackList));
        }

        @Override
        @RequiresApi(api = Build.VERSION_CODES.M)
        public void onSurfacePrepared(@NonNull SynchronizedCaptureSession session,
                @NonNull Surface surface) {
            ApiCompat.Api23Impl.onSurfacePrepared(mCameraCaptureSessionStateCallback,
                    session.toCameraCaptureSessionCompat().toCameraCaptureSession(), surface);
        }

        @Override
        public void onReady(@NonNull SynchronizedCaptureSession session) {
            mCameraCaptureSessionStateCallback.onReady(
                    session.toCameraCaptureSessionCompat().toCameraCaptureSession());
        }

        @Override
        public void onActive(@NonNull SynchronizedCaptureSession session) {
            mCameraCaptureSessionStateCallback.onActive(
                    session.toCameraCaptureSessionCompat().toCameraCaptureSession());
        }

        @Override
        @RequiresApi(api = Build.VERSION_CODES.O)
        public void onCaptureQueueEmpty(@NonNull SynchronizedCaptureSession session) {
            ApiCompat.Api26Impl.onCaptureQueueEmpty(mCameraCaptureSessionStateCallback,
                    session.toCameraCaptureSessionCompat().toCameraCaptureSession());
        }

        @Override
        public void onConfigured(@NonNull SynchronizedCaptureSession session) {
            mCameraCaptureSessionStateCallback.onConfigured(
                    session.toCameraCaptureSessionCompat().toCameraCaptureSession());
        }

        @Override
        public void onConfigureFailed(@NonNull SynchronizedCaptureSession session) {
            mCameraCaptureSessionStateCallback.onConfigureFailed(
                    session.toCameraCaptureSessionCompat().toCameraCaptureSession());
        }

        @Override
        public void onClosed(@NonNull SynchronizedCaptureSession session) {
            mCameraCaptureSessionStateCallback.onClosed(
                    session.toCameraCaptureSessionCompat().toCameraCaptureSession());
        }

        @Override
        void onSessionFinished(@NonNull SynchronizedCaptureSession session) {
            // This callback is internally used, don't forward.
        }
    }
}
