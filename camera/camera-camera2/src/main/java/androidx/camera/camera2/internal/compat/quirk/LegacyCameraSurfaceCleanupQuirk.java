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
 * Quirk addressing LegacyCameraDevice surface disconnection issues.
 *
 * <p>QuirkSummary
 *    Bug Id: 128600230
 *    Description: LegacyCameraDevice may not disconnect all configured surfaces upon closing,
 *                 leading to "connect: already connected" errors when reopening the camera.
 *                 Workaround: Force disconnection via an additional CaptureSession configuration.
 *    Device(s): Affects devices with LEGACY camera hardware level (Android API levels greater
 *               than M, less than Q).
 */
public class LegacyCameraSurfaceCleanupQuirk implements Quirk {

    static boolean load(@NonNull CameraCharacteristicsCompat cameraCharacteristicsCompat) {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.M
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                && isLegacyDevice(cameraCharacteristicsCompat);
    }

    static boolean isLegacyDevice(
            @NonNull CameraCharacteristicsCompat cameraCharacteristicsCompat) {
        final Integer level = cameraCharacteristicsCompat.get(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        return level != null && level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
    }
}
