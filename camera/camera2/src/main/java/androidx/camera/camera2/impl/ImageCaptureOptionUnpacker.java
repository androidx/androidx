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

package androidx.camera.camera2.impl;

import android.annotation.SuppressLint;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.DeviceProperties;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.UseCaseConfig;

/**
 *  A {@link Camera2CaptureOptionUnpacker} extender for unpacking ImageCapture options into
 *  {@link CaptureConfig.Builder}.
 */
final class ImageCaptureOptionUnpacker extends Camera2CaptureOptionUnpacker {

    static final ImageCaptureOptionUnpacker INSTANCE = new ImageCaptureOptionUnpacker();

    private DeviceProperties mDeviceProperties = DeviceProperties.create();

    @Override
    public void unpack(UseCaseConfig<?> config, final CaptureConfig.Builder builder) {
        super.unpack(config, builder);

        if (!(config instanceof ImageCaptureConfig)) {
            throw new IllegalArgumentException("config is not ImageCaptureConfig");
        }
        ImageCaptureConfig imageCaptureConfig = (ImageCaptureConfig) config;

        Camera2Config.Builder camera2ConfigBuilder = new Camera2Config.Builder();
        applyPixelHdrPlusChangeForCaptureMode(
                imageCaptureConfig.getCaptureMode(null), camera2ConfigBuilder);

        builder.addImplementationOptions(camera2ConfigBuilder.build());
    }

    void setDeviceProperty(DeviceProperties deviceProperties) {
        mDeviceProperties = deviceProperties;
    }

    // TODO(b/123897971):  move the device specific code once we complete the device workaround
    // module.
    @SuppressLint("NewApi")
    private void applyPixelHdrPlusChangeForCaptureMode(
            ImageCapture.CaptureMode captureMode, Camera2Config.Builder builder) {
        if ("Google".equals(mDeviceProperties.manufacturer())
                && ("Pixel 2".equals(mDeviceProperties.model())
                || "Pixel 3".equals(mDeviceProperties.model()))) {
            if (mDeviceProperties.sdkVersion() >= Build.VERSION_CODES.O) {
                if (captureMode != null) {
                    switch (captureMode) {
                        case MAX_QUALITY:
                            // enable ZSL to make sure HDR+ is enabled
                            builder.setCaptureRequestOption(
                                    CaptureRequest.CONTROL_ENABLE_ZSL, true);
                            break;
                        case MIN_LATENCY:
                            // disable ZSL to turn off HDR+
                            builder.setCaptureRequestOption(
                                    CaptureRequest.CONTROL_ENABLE_ZSL, false);
                            break;
                    }
                }
            }
        }
    }
}
