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

package androidx.camera.camera2.impl.compat;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.util.Preconditions;

import java.util.List;
import java.util.concurrent.Executor;

@RequiresApi(28)
class CameraCaptureSessionCompatApi28Impl extends CameraCaptureSessionCompatBaseImpl {
    @Override
    public int captureBurstRequests(@NonNull CameraCaptureSession captureSession,
            @NonNull List<CaptureRequest> requests, @NonNull Executor executor,
            @NonNull CameraCaptureSession.CaptureCallback listener) throws CameraAccessException {
        Preconditions.checkNotNull(captureSession);

        // Call through directly to executor API
        return captureSession.captureBurstRequests(requests, executor, listener);
    }

    @Override
    public int captureSingleRequest(@NonNull CameraCaptureSession captureSession,
            @NonNull CaptureRequest request, @NonNull Executor executor,
            @NonNull CameraCaptureSession.CaptureCallback listener) throws CameraAccessException {
        Preconditions.checkNotNull(captureSession);

        // Call through directly to executor API
        return captureSession.captureSingleRequest(request, executor, listener);
    }

    @Override
    public int setRepeatingBurstRequests(@NonNull CameraCaptureSession captureSession,
            @NonNull List<CaptureRequest> requests, @NonNull Executor executor,
            @NonNull CameraCaptureSession.CaptureCallback listener) throws CameraAccessException {
        Preconditions.checkNotNull(captureSession);

        // Call through directly to executor API
        return captureSession.setRepeatingBurstRequests(requests, executor, listener);
    }

    @Override
    public int setSingleRepeatingRequest(@NonNull CameraCaptureSession captureSession,
            @NonNull CaptureRequest request, @NonNull Executor executor,
            @NonNull CameraCaptureSession.CaptureCallback listener) throws CameraAccessException {
        Preconditions.checkNotNull(captureSession);

        // Call through directly to executor API
        return captureSession.setSingleRepeatingRequest(request, executor, listener);
    }
}
