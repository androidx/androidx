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
import androidx.camera.core.impl.CamcorderProfileProvider;
import androidx.camera.core.impl.Quirk;
import androidx.camera.video.Quality;
import androidx.camera.video.VideoCapabilities;

/**
 * Quirk denotes that the quality {@link VideoCapabilities} queried by
 * {@link CamcorderProfileProvider} cannot find a proper encoder codec for video recording on
 * device.
 *
 * <p>The video AVC encoder on Redmi note 4 and LG K10 LTE K430 aligned down the video height.
 * The maximum supported encoder resolution is 1920x1072 and the FHD quality option cannot find a
 * proper encoder. See b/216583006 for more information.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class CamcorderProfileResolutionNotSupportedByEncoderQuirk implements Quirk {

    // TODO(b/191678894): Update this Quirk after we can crop the output to the supported codec
    //  resolution.

    static boolean load() {
        return isRedmiNote4() || isLGK430();
    }

    private static boolean isRedmiNote4() {
        return "Xiaomi".equalsIgnoreCase(Build.BRAND) && "redmi note 4".equalsIgnoreCase(
                Build.MODEL);
    }

    private static boolean isLGK430() {
        return "lge".equalsIgnoreCase(Build.BRAND) && "lg-k430".equalsIgnoreCase(Build.MODEL);
    }

    /** Checks if the given Quality type is a problematic quality. */
    public boolean isProblematicVideoQuality(@NonNull Quality quality) {
        if (isRedmiNote4() || isLGK430()) {
            return quality == Quality.FHD;
        }
        return false;
    }
}
