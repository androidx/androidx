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

package androidx.camera.camera2.internal.compat.workaround;

import android.annotation.SuppressLint;
import android.hardware.camera2.CaptureRequest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks;
import androidx.camera.camera2.internal.compat.quirk.ImageCapturePixelHDRPlusQuirk;
import androidx.camera.core.ImageCapture;

/**
 * Workaround that handles turning on/off HDR+ on Pixel devices.
 *
 * @see ImageCapturePixelHDRPlusQuirk
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ImageCapturePixelHDRPlus {

    /**
     * Turns on or turns off HDR+ on Pixel devices depending on the image capture use case's
     * capture mode. When the mode is {@link ImageCapture#CAPTURE_MODE_MINIMIZE_LATENCY}, HDR+ is
     * turned off by disabling ZSL. When the mode is
     * {@link ImageCapture#CAPTURE_MODE_MAXIMIZE_QUALITY}, HDR+ is turned on by enabling ZSL.
     */
    @SuppressLint("NewApi")
    public void toggleHDRPlus(@ImageCapture.CaptureMode int captureMode,
            @NonNull Camera2ImplConfig.Builder builder) {
        final ImageCapturePixelHDRPlusQuirk quirk = DeviceQuirks.get(
                ImageCapturePixelHDRPlusQuirk.class);
        if (quirk == null) {
            return;
        }

        switch (captureMode) {
            case ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY:
                // enable ZSL to make sure HDR+ is enabled
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_ENABLE_ZSL, true);
                break;
            case ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY:
                // disable ZSL to turn off HDR+
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_ENABLE_ZSL, false);
                break;
        }
    }
}
