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
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.params.InputConfiguration;
import android.os.Handler;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.impl.compat.params.InputConfigurationCompat;
import androidx.camera.camera2.impl.compat.params.SessionConfigurationCompat;
import androidx.camera.core.impl.utils.MainThreadAsyncHandler;
import androidx.core.util.Preconditions;

import java.util.List;

@RequiresApi(23)
class CameraDeviceCompatApi23Impl extends CameraDeviceCompatBaseImpl {

    @Override
    public void createCaptureSession(@NonNull CameraDevice device,
            @NonNull SessionConfigurationCompat config) throws CameraAccessException {
        checkPreconditions(device, config);

        // Wrap the executor in the callback
        CameraCaptureSession.StateCallback cb =
                new CameraCaptureSessionCompat.StateCallbackExecutorWrapper(
                        config.getExecutor(), config.getStateCallback());

        // Convert the OutputConfigurations to surfaces
        List<Surface> surfaces = unpackSurfaces(config.getOutputConfigurations());

        Handler handler = MainThreadAsyncHandler.getInstance();

        InputConfigurationCompat inputConfigCompat = config.getInputConfiguration();
        if (inputConfigCompat != null) {
            // Client is requesting a reprocessable capture session
            InputConfiguration inputConfig = (InputConfiguration) inputConfigCompat.unwrap();

            Preconditions.checkNotNull(inputConfig);
            device.createReprocessableCaptureSession(inputConfig, surfaces, cb, handler);
        } else if (config.getSessionType() == SessionConfigurationCompat.SESSION_HIGH_SPEED) {
            // Client is requesting a high speed capture session
            device.createConstrainedHighSpeedCaptureSession(surfaces, cb, handler);
        } else {
            // Fall back to a normal capture session
            createBaseCaptureSession(device, surfaces, cb, handler);
        }
    }
}
