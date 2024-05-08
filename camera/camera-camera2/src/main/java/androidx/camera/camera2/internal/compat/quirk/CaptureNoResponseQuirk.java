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

import static android.hardware.camera2.CameraMetadata.LENS_FACING_BACK;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.impl.Quirk;

/**
 * Capture not response if submitting a single capture request simultaneously with repeating
 * requests
 *
 * <p>QuirkSummary
 *     Bug Id: 305835396
 *     Description: This class defines a quirk related to the camera capture functionality on
 *                  specific devices. It describes a scenario where single capture requests may
 *                  not receive a response if they are submitted simultaneously with repeating
 *                  capture requests. Single capture requests fail to receive a response
 *                  approximately 10% of the time when submitted within milliseconds of a
 *                  repeating capture request.
 *     Device(s): Samsung device with samsungexynos7420 hardware
 */
public class CaptureNoResponseQuirk implements Quirk {

    static boolean load(@NonNull CameraCharacteristicsCompat characteristics) {
        return ("samsungexynos7420".equalsIgnoreCase(Build.HARDWARE)
                || "universal7420".equalsIgnoreCase(Build.HARDWARE))
                && characteristics.get(CameraCharacteristics.LENS_FACING) == LENS_FACING_BACK;
    }
}
