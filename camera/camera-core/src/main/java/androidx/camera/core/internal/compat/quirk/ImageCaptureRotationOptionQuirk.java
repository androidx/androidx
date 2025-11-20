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

package androidx.camera.core.internal.compat.quirk;

import android.os.Build;

import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.Quirk;

import org.jspecify.annotations.NonNull;

/**
 * <p>QuirkSummary
 *     Bug Id: b/171492111
 *     Description: Quirk required to check whether ImageCapture supports a specific capture
 *                  config option. For example, JPEG related options will be applied when
 *                  capturing images. CaptureConfig.OPTION_ROTATION is used to provide the target
 *                  rotation information to the HAL. So that the HAL can rotate the image buffer
 *                  directly and provide the correct orientation information in the embedded exif
 *                  data. But not all devices can support CaptureConfig.OPTION_ROTATION. Huawei
 *                  Mate 20 Lite and Honor 9X can't handle the capture rotation option correctly
 *                  and the embedded exif's orientation value is wrong. For these devices, the
 *                  rotation option can't be used and we should calculate the rotation value
 *                  according to the target rotation setting for the final output image.
 *     Device(s): Huawei Mate 20 Lite, Honor 9X
 *     @see androidx.camera.core.internal.compat.workaround.ExifRotationAvailability
 */
public final class ImageCaptureRotationOptionQuirk implements Quirk {

    static boolean load() {
        return isHuaweiMate20Lite() || isHonor9X();
    }

    private static boolean isHuaweiMate20Lite() {
        return "HUAWEI".equalsIgnoreCase(Build.BRAND) && "SNE-LX1".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isHonor9X() {
        return "HONOR".equalsIgnoreCase(Build.BRAND) && "STK-LX1".equalsIgnoreCase(
                Build.MODEL);
    }

    /**
     * Returns true if the capture config option can be supported.
     */
    public boolean isSupported(Config.@NonNull Option<?> option) {
        return option != CaptureConfig.OPTION_ROTATION;
    }
}
