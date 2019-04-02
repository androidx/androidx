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

package androidx.camera.camera2;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;

import androidx.camera.core.CameraCaptureCallback;
import androidx.camera.core.CameraCaptureCallbacks;
import androidx.camera.core.CameraCaptureSessionStateCallbacks;
import androidx.camera.core.CameraDeviceStateCallbacks;
import androidx.camera.core.Config;
import androidx.camera.core.Config.Option;
import androidx.camera.core.OptionsBundle;
import androidx.camera.core.SessionConfig;
import androidx.camera.core.UseCaseConfig;

/**
 * A {@link SessionConfig.OptionUnpacker} implementation for unpacking Camera2 options into a
 * {@link SessionConfig.Builder}.
 */
final class Camera2OptionUnpacker implements SessionConfig.OptionUnpacker {

    static final Camera2OptionUnpacker INSTANCE = new Camera2OptionUnpacker();

    @Override
    public void unpack(UseCaseConfig<?> config, final SessionConfig.Builder builder) {
        SessionConfig defaultSessionConfig =
                config.getDefaultSessionConfig(/*valueIfMissing=*/ null);

        CameraDevice.StateCallback deviceStateCallback =
                CameraDeviceStateCallbacks.createNoOpCallback();
        CameraCaptureSession.StateCallback sessionStateCallback =
                CameraCaptureSessionStateCallbacks.createNoOpCallback();
        CameraCaptureCallback cameraCaptureCallback = CameraCaptureCallbacks.createNoOpCallback();
        Config implOptions = OptionsBundle.emptyBundle();
        int templateType = SessionConfig.defaultEmptySessionConfig().getTemplateType();

        // Apply/extract defaults from session config
        if (defaultSessionConfig != null) {
            templateType = defaultSessionConfig.getTemplateType();
            deviceStateCallback = defaultSessionConfig.getDeviceStateCallback();
            sessionStateCallback = defaultSessionConfig.getSessionStateCallback();
            cameraCaptureCallback = defaultSessionConfig.getCameraCaptureCallback();
            implOptions = defaultSessionConfig.getImplementationOptions();

            // Add all default camera characteristics
            builder.addCharacteristics(defaultSessionConfig.getCameraCharacteristics());
        }

        // Set the any additional implementation options
        builder.setImplementationOptions(implOptions);

        // Get Camera2 extended options
        final Camera2Config camera2Config = new Camera2Config(config);

        // Apply template type
        builder.setTemplateType(camera2Config.getCaptureRequestTemplate(templateType));

        // Combine default config callbacks with extension callbacks
        deviceStateCallback =
                CameraDeviceStateCallbacks.createComboCallback(
                        deviceStateCallback,
                        camera2Config.getDeviceStateCallback(
                                CameraDeviceStateCallbacks.createNoOpCallback()));
        sessionStateCallback =
                CameraCaptureSessionStateCallbacks.createComboCallback(
                        sessionStateCallback,
                        camera2Config.getSessionStateCallback(
                                CameraCaptureSessionStateCallbacks.createNoOpCallback()));
        cameraCaptureCallback =
                CameraCaptureCallbacks.createComboCallback(
                        cameraCaptureCallback,
                        CaptureCallbackContainer.create(
                                camera2Config.getSessionCaptureCallback(
                                        Camera2CaptureCallbacks.createNoOpCallback())));

        // Apply state callbacks
        builder.setDeviceStateCallback(deviceStateCallback);
        builder.setSessionStateCallback(sessionStateCallback);
        builder.setCameraCaptureCallback(cameraCaptureCallback);

        // Copy extension keys
        camera2Config.findOptions(
                Camera2Config.CAPTURE_REQUEST_ID_STEM,
                new Config.OptionMatcher() {
                    @Override
                    public boolean onOptionMatched(Option<?> option) {
                        @SuppressWarnings(
                                "unchecked")
                        // No way to get actual type info here, so treat as Object
                                Option<Object> typeErasedOption = (Option<Object>) option;
                        @SuppressWarnings("unchecked")
                        CaptureRequest.Key<Object> key =
                                (CaptureRequest.Key<Object>) option.getToken();

                        builder.addCharacteristic(key,
                                camera2Config.retrieveOption(typeErasedOption));
                        return true;
                    }
                });
    }
}
