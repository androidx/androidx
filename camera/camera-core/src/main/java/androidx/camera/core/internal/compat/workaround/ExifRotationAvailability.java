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

package androidx.camera.core.internal.compat.workaround;

import android.graphics.ImageFormat;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.internal.compat.quirk.DeviceQuirks;
import androidx.camera.core.internal.compat.quirk.ImageCaptureRotationOptionQuirk;

/**
 * Workaround to check whether the exif rotation value embedded in the capture JPEG image is
 * available.
 *
 * @see ImageCaptureRotationOptionQuirk
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ExifRotationAvailability {

    /**
     * Returns whether rotation option is supported on the device.
     */
    public boolean isRotationOptionSupported() {
        ImageCaptureRotationOptionQuirk quirk =
                DeviceQuirks.get(ImageCaptureRotationOptionQuirk.class);

        return quirk != null ? quirk.isSupported(CaptureConfig.OPTION_ROTATION) : true;
    }

    /**
     * Checks whether the exif orientation value should be used for the final output image.
     *
     * <p>On some devices, the orientation value in the embedded exif of the captured images may
     * be 0 but the image buffer data actually is not rotated to upright orientation by HAL. For
     * these devices, the exif orientation value should not be used for the final output image.
     *
     * @param image The captured image object.
     */
    public boolean shouldUseExifOrientation(@NonNull ImageProxy image) {
        ImageCaptureRotationOptionQuirk quirk =
                DeviceQuirks.get(ImageCaptureRotationOptionQuirk.class);

        return (quirk != null ? quirk.isSupported(CaptureConfig.OPTION_ROTATION) : true)
                && image.getFormat() == ImageFormat.JPEG;
    }
}
