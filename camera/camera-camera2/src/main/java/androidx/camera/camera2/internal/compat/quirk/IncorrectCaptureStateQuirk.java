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

package androidx.camera.camera2.internal.compat.quirk;

import android.hardware.camera2.CameraCharacteristics;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.impl.Quirk;

/**
 * CameraCaptureSession report error when a capture request was submitted in an incorrect state.
 *
 * <p>QuirkSummary
 * Bug Id: 297501750
 * Description: When quickly configuring two CameraCaptureSession objects in sequence on a legacy
 *              camera hardware device, the second CameraCaptureSession object may fail with an
 *              "Illegal state encountered in camera service" error if the first
 *              CameraCaptureSession object submits a capture request in an incorrect state.
 * Device(s): Devices in LEGACY camera hardware level.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class IncorrectCaptureStateQuirk implements Quirk {

    static boolean load(@NonNull CameraCharacteristicsCompat characteristics) {
        final Integer level = characteristics.get(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        return level != null && level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
    }
}
