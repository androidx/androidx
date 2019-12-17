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

import android.annotation.SuppressLint;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCapture.CaptureMode;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.DeviceProperties;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.UseCaseConfig;

/**
 * A {@link Camera2CaptureOptionUnpacker} extender for unpacking ImageCapture options into
 * {@link CaptureConfig.Builder}.
 */
final class ImageCaptureOptionUnpacker extends Camera2CaptureOptionUnpacker {

    static final ImageCaptureOptionUnpacker INSTANCE = new ImageCaptureOptionUnpacker();

    private DeviceProperties mDeviceProperties = DeviceProperties.create();

    @Override
    public void unpack(@NonNull UseCaseConfig<?> config,
            @NonNull final CaptureConfig.Builder builder) {
        super.unpack(config, builder);

        if (!(config instanceof ImageCaptureConfig)) {
            throw new IllegalArgumentException("config is not ImageCaptureConfig");
        }
        ImageCaptureConfig imageCaptureConfig = (ImageCaptureConfig) config;

        Camera2ImplConfig.Builder camera2ConfigBuilder = new Camera2ImplConfig.Builder();

        if (imageCaptureConfig.hasCaptureMode()) {
            applyPixelHdrPlusChangeForCaptureMode(imageCaptureConfig.getCaptureMode(),
                    camera2ConfigBuilder);
        }

        builder.addImplementationOptions(camera2ConfigBuilder.build());
    }

    void setDeviceProperty(DeviceProperties deviceProperties) {
        mDeviceProperties = deviceProperties;
    }

    // TODO(b/123897971):  move the device specific code once we complete the device workaround
    // module.
    @SuppressLint("NewApi")
    private void applyPixelHdrPlusChangeForCaptureMode(@CaptureMode int captureMode,
            Camera2ImplConfig.Builder builder) {
        if ("Google".equals(mDeviceProperties.manufacturer())
                && ("Pixel 2".equals(mDeviceProperties.model())
                || "Pixel 3".equals(mDeviceProperties.model()))) {
            if (mDeviceProperties.sdkVersion() >= Build.VERSION_CODES.O) {
                switch (captureMode) {
                    case ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY:
                        // enable ZSL to make sure HDR+ is enabled
                        builder.setCaptureRequestOption(
                                CaptureRequest.CONTROL_ENABLE_ZSL, true);
                        break;
                    case ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY:
                        // disable ZSL to turn off HDR+
                        builder.setCaptureRequestOption(
                                CaptureRequest.CONTROL_ENABLE_ZSL, false);
                        break;
                }
            }
        }
    }
}
