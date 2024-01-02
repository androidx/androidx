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

package androidx.camera.video.internal.compat.quirk;

import static android.media.CamcorderProfile.QUALITY_1080P;
import static android.media.CamcorderProfile.QUALITY_480P;
import static android.media.CamcorderProfile.QUALITY_720P;

import android.os.Build;
import android.util.Size;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

/**
 * <p>QuirkSummary
 *     Bug Id: b/299075294
 *     Description: The captured video is stretched while using the resolution obtained from
 *                  EncoderProfiles. The quirk provides an alternative resolution for supported
 *                  qualities.
 *     Device(s): Motorola Moto E5 Play.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class StretchedVideoResolutionQuirk implements Quirk {

    static boolean load() {
        return isMotoE5Play();
    }

    private static boolean isMotoE5Play() {
        return "motorola".equalsIgnoreCase(Build.BRAND) && "moto e5 play".equalsIgnoreCase(
                Build.MODEL);
    }

    /**
     * Returns an alternative resolution available on the device.
     */
    @Nullable
    public Size getAlternativeResolution(int quality) {
        switch (quality) {
            case QUALITY_480P:
                return new Size(640, 480);
            case QUALITY_720P:
                return new Size(960, 720);
            case QUALITY_1080P:
                return new Size(1440, 1080);
            default:
                return null;
        }
    }

}
