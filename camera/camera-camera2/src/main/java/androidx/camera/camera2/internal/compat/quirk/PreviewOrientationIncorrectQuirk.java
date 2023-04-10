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

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.impl.Quirk;

/**
 * A quirk where the orientation of the preview is incorrect while a surface to be used to
 * configure on 2 different {@link CameraCaptureSession}.
 *
 * <p>QuirkSummary
 *     Bug Id: 128577112
 *     Description: Reusing a surface to create a different {@link CameraCaptureSession}
 *                  causes incorrect preview orientation for LEGACY devices. Some GL related setting
 *                  is left over when the legacy shim is recreated. It causes the 2nd
 *                  PreviewCaptureSession to be rotated and stretched compared to the 1st one.
 *     Device(s): Devices in LEGACY camera hardware level.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class PreviewOrientationIncorrectQuirk implements Quirk {

    static boolean load(@NonNull CameraCharacteristicsCompat characteristics) {
        final Integer level = characteristics.get(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        return level != null && level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
    }
}
