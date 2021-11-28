/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.impl.Quirk;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * This denotes a quirk that when flash is auto, the device fails to get bright still photos with
 * good exposure.
 *
 * see b/205373142
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class AutoFlashUnderExposedQuirk implements Quirk {
    private static final List<String> AFFECTED_MODELS = Arrays.asList(
            "PIXEL 3A",
            "PIXEL 3A XL"
    );

    static boolean load(@NonNull CameraCharacteristicsCompat cameraCharacteristics) {
        return AFFECTED_MODELS.contains(Build.MODEL.toUpperCase(Locale.US))
                && cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == LENS_FACING_BACK;
    }
}
