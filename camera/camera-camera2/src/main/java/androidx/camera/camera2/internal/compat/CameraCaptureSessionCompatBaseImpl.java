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

package androidx.camera.camera2.internal.compat;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.Preconditions;

import java.util.List;
import java.util.concurrent.Executor;

@RequiresApi(21)
class CameraCaptureSessionCompatBaseImpl implements
        CameraCaptureSessionCompat.CameraCaptureSessionCompatImpl {

    final CameraCaptureSession mCameraCaptureSession;
    final Object mObject;

    CameraCaptureSessionCompatBaseImpl(@NonNull CameraCaptureSession captureSession,
            @Nullable Object implParams) {
        mCameraCaptureSession = Preconditions.checkNotNull(captureSession);
        mObject = implParams;
    }

    static CameraCaptureSessionCompat.CameraCaptureSessionCompatImpl create(
            @NonNull CameraCaptureSession captureSession,
            @NonNull Handler compatHandler) {
        return new CameraCaptureSessionCompatBaseImpl(captureSession,
                new CameraCaptureSessionCompatBaseImpl.CameraCaptureSessionCompatParamsApi21(
                        compatHandler));
    }

    @Override
    public int captureBurstRequests(@NonNull List<CaptureRequest> requests,
            @NonNull Executor executor,
            @NonNull CameraCaptureSession.CaptureCallback listener) throws CameraAccessException {
        // Wrap the executor in the callback
        CameraCaptureSession.CaptureCallback cb =
                new CameraCaptureSessionCompat.CaptureCallbackExecutorWrapper(executor, listener);

        CameraCaptureSessionCompatParamsApi21 params =
                (CameraCaptureSessionCompatParamsApi21) mObject;
        return mCameraCaptureSession.captureBurst(requests, cb, params.mCompatHandler);
    }

    @Override
    public int captureSingleRequest(@NonNull CaptureRequest request, @NonNull Executor executor,
            @NonNull CameraCaptureSession.CaptureCallback listener) throws CameraAccessException {
        // Wrap the executor in the callback
        CameraCaptureSession.CaptureCallback cb =
                new CameraCaptureSessionCompat.CaptureCallbackExecutorWrapper(executor, listener);

        CameraCaptureSessionCompatParamsApi21 params =
                (CameraCaptureSessionCompatParamsApi21) mObject;
        return mCameraCaptureSession.capture(request, cb, params.mCompatHandler);
    }

    @Override
    public int setRepeatingBurstRequests(@NonNull List<CaptureRequest> requests,
            @NonNull Executor executor,
            @NonNull CameraCaptureSession.CaptureCallback listener) throws CameraAccessException {
        // Wrap the executor in the callback
        CameraCaptureSession.CaptureCallback cb =
                new CameraCaptureSessionCompat.CaptureCallbackExecutorWrapper(executor, listener);

        CameraCaptureSessionCompatParamsApi21 params =
                (CameraCaptureSessionCompatParamsApi21) mObject;
        return mCameraCaptureSession.setRepeatingBurst(requests, cb, params.mCompatHandler);
    }

    @Override
    public int setSingleRepeatingRequest(@NonNull CaptureRequest request,
            @NonNull Executor executor,
            @NonNull CameraCaptureSession.CaptureCallback listener) throws CameraAccessException {
        // Wrap the executor in the callback
        CameraCaptureSession.CaptureCallback cb =
                new CameraCaptureSessionCompat.CaptureCallbackExecutorWrapper(executor, listener);

        CameraCaptureSessionCompatParamsApi21 params =
                (CameraCaptureSessionCompatParamsApi21) mObject;
        return mCameraCaptureSession.setRepeatingRequest(
                request, cb, params.mCompatHandler);
    }

    @NonNull
    @Override
    public CameraCaptureSession unwrap() {
        return mCameraCaptureSession;
    }

    private static class CameraCaptureSessionCompatParamsApi21 {
        final Handler mCompatHandler;

        CameraCaptureSessionCompatParamsApi21(@NonNull Handler compatHandler) {
            mCompatHandler = compatHandler;
        }
    }
}
