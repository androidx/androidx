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
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.params.InputConfiguration;
import android.os.Handler;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.params.InputConfigurationCompat;
import androidx.camera.camera2.internal.compat.params.SessionConfigurationCompat;
import androidx.core.util.Preconditions;

import java.util.List;

@RequiresApi(23)
@SuppressWarnings("deprecation")
class CameraDeviceCompatApi23Impl extends CameraDeviceCompatBaseImpl {

    CameraDeviceCompatApi23Impl(@NonNull CameraDevice cameraDevice, @Nullable Object implParams) {
        super(cameraDevice, implParams);
    }

    static CameraDeviceCompatApi23Impl create(@NonNull CameraDevice cameraDevice,
            @NonNull Handler compatHandler) {
        return new CameraDeviceCompatApi23Impl(cameraDevice,
                new CameraDeviceCompatBaseImpl.CameraDeviceCompatParamsApi21(compatHandler));
    }

    @Override
    public void createCaptureSession(@NonNull SessionConfigurationCompat config)
            throws CameraAccessExceptionCompat {
        checkPreconditions(mCameraDevice, config);

        // Wrap the executor in the callback
        CameraCaptureSession.StateCallback cb =
                new CameraCaptureSessionCompat.StateCallbackExecutorWrapper(
                        config.getExecutor(), config.getStateCallback());

        // Convert the OutputConfigurations to surfaces
        List<Surface> surfaces = unpackSurfaces(config.getOutputConfigurations());

        CameraDeviceCompatParamsApi21 params = (CameraDeviceCompatParamsApi21) mImplParams;
        Handler handler = Preconditions.checkNotNull(params).mCompatHandler;

        InputConfigurationCompat inputConfigCompat = config.getInputConfiguration();
        try {
            if (inputConfigCompat != null) {
                // Client is requesting a reprocessable capture session
                InputConfiguration inputConfig = (InputConfiguration) inputConfigCompat.unwrap();

                Preconditions.checkNotNull(inputConfig);
                mCameraDevice.createReprocessableCaptureSession(inputConfig, surfaces, cb, handler);
            } else if (config.getSessionType() == SessionConfigurationCompat.SESSION_HIGH_SPEED) {
                // Client is requesting a high speed capture session
                mCameraDevice.createConstrainedHighSpeedCaptureSession(surfaces, cb, handler);
            } else {
                // Fall back to a normal capture session
                createBaseCaptureSession(mCameraDevice, surfaces, cb, handler);
            }
        } catch (CameraAccessException e) {
            throw CameraAccessExceptionCompat.toCameraAccessExceptionCompat(e);
        }
    }
}
