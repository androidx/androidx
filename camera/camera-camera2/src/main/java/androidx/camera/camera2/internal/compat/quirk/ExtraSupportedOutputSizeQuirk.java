/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat.quirk;

import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.core.impl.Quirk;

/**
 * <p>QuirkSummary
 *     Bug Id: b/241876294, b/299075294
 *     Description: CamcorderProfile resolutions can not find a match in the output size list of
 *                  CameraCharacteristics#SCALER_STREAM_CONFIGURATION_MAP. Some resolutions are
 *                  added back as they are supported by the camera and do not have stretching
 *                  issues.
 *     Device(s): Motorola Moto E5 Play.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ExtraSupportedOutputSizeQuirk implements Quirk {

    static boolean load() {
        return isMotoE5Play();
    }

    private static boolean isMotoE5Play() {
        return "motorola".equalsIgnoreCase(Build.BRAND) && "moto e5 play".equalsIgnoreCase(
                Build.MODEL);
    }

    /**
     * Returns the extra supported resolutions on the device.
     */
    @NonNull
    public Size[] getExtraSupportedResolutions(int format) {
        if (format == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
                && isMotoE5Play()) {
            return getMotoE5PlayExtraSupportedResolutions();
        } else {
            return new Size[]{};
        }
    }

    /**
     * Returns the extra supported resolutions on the device.
     */
    @NonNull
    public <T> Size[] getExtraSupportedResolutions(@NonNull Class<T> klass) {
        if (StreamConfigurationMap.isOutputSupportedFor(klass) && isMotoE5Play()) {
            return getMotoE5PlayExtraSupportedResolutions();
        } else {
            return new Size[]{};
        }
    }

    @NonNull
    private Size[] getMotoE5PlayExtraSupportedResolutions() {
        // Both the front and the main cameras support the following resolutions.
        return new Size[]{
                // FHD
                new Size(1440, 1080),
                // HD
                new Size(960, 720),
                // SD (640:480 is already included in the original list)
        };
    }
}
