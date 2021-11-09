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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.params.InputConfigurationCompat;
import androidx.camera.camera2.internal.compat.params.OutputConfigurationCompat;
import androidx.camera.camera2.internal.compat.params.SessionConfigurationCompat;
import androidx.core.util.Preconditions;

import java.util.List;

@RequiresApi(24)
@SuppressWarnings("deprecation")
class CameraDeviceCompatApi24Impl extends CameraDeviceCompatApi23Impl {

    CameraDeviceCompatApi24Impl(@NonNull CameraDevice cameraDevice, @Nullable Object implParams) {
        super(cameraDevice, implParams);
    }

    static CameraDeviceCompatApi24Impl create(@NonNull CameraDevice cameraDevice,
            @NonNull Handler compatHandler) {
        return new CameraDeviceCompatApi24Impl(cameraDevice,
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
        List<OutputConfigurationCompat> outputs = config.getOutputConfigurations();

        CameraDeviceCompatParamsApi21 params = (CameraDeviceCompatParamsApi21) mImplParams;
        Handler handler = Preconditions.checkNotNull(params).mCompatHandler;

        InputConfigurationCompat inputConfigCompat = config.getInputConfiguration();
        try {
            if (inputConfigCompat != null) {
                // Client is requesting a reprocessable capture session
                InputConfiguration inputConfig = (InputConfiguration) inputConfigCompat.unwrap();

                Preconditions.checkNotNull(inputConfig);
                // Use OutputConfigurations on this API level
                mCameraDevice.createReprocessableCaptureSessionByConfigurations(inputConfig,
                        SessionConfigurationCompat.transformFromCompat(outputs), cb, handler);
            } else if (config.getSessionType() == SessionConfigurationCompat.SESSION_HIGH_SPEED) {
                // Client is requesting a high speed capture session
                mCameraDevice.createConstrainedHighSpeedCaptureSession(unpackSurfaces(outputs), cb,
                        handler);
            } else {
                // Fall back to a normal capture session (created from OutputConfigurations)
                mCameraDevice.createCaptureSessionByOutputConfigurations(
                        SessionConfigurationCompat.transformFromCompat(outputs), cb, handler);
            }
        } catch (CameraAccessException e) {
            throw CameraAccessExceptionCompat.toCameraAccessExceptionCompat(e);
        }
    }
}
