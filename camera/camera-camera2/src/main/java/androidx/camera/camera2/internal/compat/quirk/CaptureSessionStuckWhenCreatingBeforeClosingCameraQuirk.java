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

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.impl.Quirk;

/**
 * Quirk addressing the capture session stuck issue when creating new one before closing the camera
 * device first.
 *
 * <p>QuirkSummary
 *    Bug Id: 359062845
 *    Description: Camera can't stream the image normally if the new capture session is created
 *    before closing the camera device first when a capture session has been opened. This can
 *    happen if the apps bind the UseCases sequentially. The workaround is to close the camera
 *    device before creating the new capture session.
 *    Device(s): Moto e20's front camera.
 */
public class CaptureSessionStuckWhenCreatingBeforeClosingCameraQuirk implements Quirk {

    static boolean load(@NonNull CameraCharacteristicsCompat characteristicsCompat) {
        return shouldLoadForMotoE20(characteristicsCompat);
    }

    private static boolean shouldLoadForMotoE20(
            @NonNull CameraCharacteristicsCompat characteristicsCompat) {
        return "motorola".equalsIgnoreCase(Build.BRAND) && "moto e20".equalsIgnoreCase(Build.MODEL)
                && characteristicsCompat.getCameraId().equals("1");
    }
}
