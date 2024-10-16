/*
 * Copyright 2023 The Android Open Source Project
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

import android.hardware.camera2.CameraDevice;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks;
import androidx.camera.camera2.internal.compat.quirk.PreviewUnderExposureQuirk;
import androidx.camera.core.ExperimentalZeroShutterLag;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.impl.UseCaseConfigFactory;

/**
 * A class that contains utility methods for template type.
 */
public class TemplateTypeUtil {

    private TemplateTypeUtil() {

    }

    /**
     * Returns the appropriate template type for a session configuration.
     */
    @OptIn(markerClass = ExperimentalZeroShutterLag.class)
    public static int getSessionConfigTemplateType(
            @NonNull UseCaseConfigFactory.CaptureType captureType,
            @ImageCapture.CaptureMode int captureMode
    ) {
        switch (captureType) {
            case IMAGE_CAPTURE:
                return captureMode == ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG
                        ? CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG :
                        CameraDevice.TEMPLATE_PREVIEW;
            case VIDEO_CAPTURE:
                if (DeviceQuirks.get(PreviewUnderExposureQuirk.class) != null) {
                    return CameraDevice.TEMPLATE_PREVIEW;
                }
                return CameraDevice.TEMPLATE_RECORD;
            case STREAM_SHARING:
            case PREVIEW:
            case IMAGE_ANALYSIS:
            default:
                return CameraDevice.TEMPLATE_PREVIEW;
        }
    }

    /**
     * Returns the appropriate template type for a capture configuration.
     */
    @OptIn(markerClass = ExperimentalZeroShutterLag.class)
    public static int getCaptureConfigTemplateType(
            @NonNull UseCaseConfigFactory.CaptureType captureType,
            @ImageCapture.CaptureMode int captureMode
    ) {
        switch (captureType) {
            case IMAGE_CAPTURE:
                return captureMode == ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG
                        ? CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG :
                        CameraDevice.TEMPLATE_STILL_CAPTURE;
            case VIDEO_CAPTURE:
                if (DeviceQuirks.get(PreviewUnderExposureQuirk.class) != null) {
                    return CameraDevice.TEMPLATE_PREVIEW;
                }
                return CameraDevice.TEMPLATE_RECORD;
            case STREAM_SHARING:
            case PREVIEW:
            case IMAGE_ANALYSIS:
            default:
                return CameraDevice.TEMPLATE_PREVIEW;
        }
    }
}
