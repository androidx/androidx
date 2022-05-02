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

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
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
 *     Device(s): Itel w6004
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ImageCaptureFlashNotFireQuirk implements UseTorchAsFlashQuirk {

    // List of devices with the issue. See b/228800360.
    private static final List<String> BUILD_MODELS = Arrays.asList(
            "itel w6004"  // Itel W6004
    );

    static boolean load(@NonNull CameraCharacteristicsCompat cameraCharacteristics) {
        return BUILD_MODELS.contains(Build.MODEL.toLowerCase(Locale.US));
    }
}
