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

package androidx.camera.video.internal.compat.quirk;

import static androidx.camera.core.CameraSelector.LENS_FACING_FRONT;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.video.Quality;

/**
 * <p>QuirkSummary
 *     Bug Id: 218841498
 *     Description: The Quirk denotes the devices may crash while using the specific
 *                  {@link Quality} option for video recording. The mp4v-es video encoder crashes
 *                  on Positivo Twist 2 Pro. It only crashes while processing the SD resolution,
 *                  others work fine.
 *     Device(s): Positivo Twist 2 Pro (twist 2 pro)
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class VideoEncoderCrashQuirk implements VideoQualityQuirk {
    static boolean load() {
        return isPositivoTwist2Pro();
    }

    private static boolean isPositivoTwist2Pro() {
        return "positivo".equalsIgnoreCase(Build.BRAND) && "twist 2 pro".equalsIgnoreCase(
                Build.MODEL);
    }

    /** Checks if the given Quality type is a problematic quality. */
    @Override
    public boolean isProblematicVideoQuality(@NonNull CameraInfoInternal cameraInfo,
            @NonNull Quality quality) {
        if (isPositivoTwist2Pro()) {
            return cameraInfo.getLensFacing() == LENS_FACING_FRONT && quality == Quality.SD;
        }
        return false;
    }

    @Override
    public boolean workaroundBySurfaceProcessing() {
        // Failed to record video for front camera + SD quality. See b/218841498 comment#5.
        return false;
    }
}
