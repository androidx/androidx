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

import android.hardware.camera2.CameraCharacteristics;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.impl.Quirk;

/**
 * A quirk to denote the {@link android.hardware.camera2.CameraCaptureSession} cannot
 * successfully be configured if the previous CameraCaptureSession doesn't finish its in-flight
 * capture sequence.
 *
 * <p>QuirkSummary
 *     Bug Id: 146773463
 *     Description: Opening and releasing the capture session quickly and constantly is a problem
 *                  for LEGACY devices. It needs to check that all the existing capture sessions
 *                  have finished the processing of their capture sequences before opening the
 *                  next capture session.
 *     Device(s): Devices in LEGACY camera hardware level.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class CaptureSessionStuckQuirk implements Quirk {

    static boolean load(@NonNull CameraCharacteristicsCompat characteristics) {
        final Integer level = characteristics.get(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        return level != null && level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
    }
}
