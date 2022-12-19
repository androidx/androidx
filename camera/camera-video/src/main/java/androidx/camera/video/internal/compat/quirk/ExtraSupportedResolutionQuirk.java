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

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

/**
 * <p>QuirkSummary
 *     Bug Id: b/241876294
 *     Description: CamcorderProfile resolutions can not find a match in the output size list of
 *                  CameraCharacteristics#SCALER_STREAM_CONFIGURATION_MAP. Since the resolutions
 *                  can be used when the surface is not an encoder surface, enable surface
 *                  processing on the {@link androidx.camera.video.VideoCapture} use case to
 *                  support these resolutions.
 *     Device(s): Motorola Moto E5 Play.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ExtraSupportedResolutionQuirk implements Quirk {

    static boolean load() {
        return isMotoE5Play();
    }

    private static boolean isMotoE5Play() {
        return "motorola".equalsIgnoreCase(Build.BRAND) && "moto e5 play".equalsIgnoreCase(
                Build.MODEL);
    }
}
