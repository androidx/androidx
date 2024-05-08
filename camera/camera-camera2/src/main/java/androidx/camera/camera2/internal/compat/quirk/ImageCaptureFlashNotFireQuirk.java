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

import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * A quirk to denote the camera never fire the flash while taking picture with flash ON/AUTO mode.
 *
 * <p>QuirkSummary
 *     Bug Id: 228800360
 *     Description: The flash doesn't fire while taking picture with flash ON/AUTO mode.
 *     Device(s): Itel w6004, Samsung Galaxy J7 (sm-j700f, sm-j710f) front camera
 */
public class ImageCaptureFlashNotFireQuirk implements UseTorchAsFlashQuirk {

    // List of devices with the issue. See b/228800360.
    private static final List<String> BUILD_MODELS = Arrays.asList(
            "itel w6004"  // Itel W6004
    );

    private static final List<String> BUILD_MODELS_FRONT_CAMERA = Arrays.asList(
            "sm-j700f",    // Samsung Galaxy J7
            "sm-j710f"     // Samsung Galaxy J7
    );

    static boolean load(@NonNull CameraCharacteristicsCompat cameraCharacteristics) {
        boolean isFrontCameraAffected =
                BUILD_MODELS_FRONT_CAMERA.contains(Build.MODEL.toLowerCase(Locale.US))
                && cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                == LENS_FACING_FRONT;
        boolean isAffected = BUILD_MODELS.contains(Build.MODEL.toLowerCase(Locale.US));

        return isFrontCameraAffected || isAffected;
    }
}
