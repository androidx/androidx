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

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.video.Quality;

/**
 * Bug Id: 202792648
 * Description: The captured video is stretched while selecting the quality is greater or
 * equality to FHD resolution.
 * Device(s): Samsung J4 (sm-j400g)
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ExcludeStretchedVideoQualityQuirk implements VideoQualityQuirk {
    static boolean load() {
        return isSamsungJ4();
    }

    private static boolean isSamsungJ4() {
        return "Samsung".equalsIgnoreCase(Build.BRAND) && "SM-J400G".equalsIgnoreCase(Build.MODEL);
    }

    /** Checks if the given Quality type is a problematic quality. */
    @Override
    public boolean isProblematicVideoQuality(@NonNull Quality quality) {
        if (isSamsungJ4()) {
            return quality == Quality.FHD || quality == Quality.UHD;
        }
        return false;
    }
}
