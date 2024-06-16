/*
 * Copyright 2024 The Android Open Source Project
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

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.impl.Quirk;

/**
 * Quirk addressing null pointer issue during CameraCaptureSession configuration.
 *
 * <p>QuirkSummary
 *    Bug Id: 237341513
 *    Description: An incorrect state in the legacy Camera2 framework can lead to a
 *                 null pointer exception when configuring a second CameraCaptureSession
 *                 with `getSurface()`. This workaround prevents the issue by proactively
 *                 closing and reopening the camera device.
 *    Device(s): Affects devices with LEGACY camera hardware level (API levels greater than M).
 */
public class LegacyCameraOutputConfigNullPointerQuirk implements Quirk {

    static boolean load(@NonNull CameraCharacteristicsCompat characteristics) {
        Integer level = characteristics.get(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.M && level != null
                && level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
    }
}
