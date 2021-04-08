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

package androidx.camera.core.internal.compat.quirk;

import android.os.Build;

import androidx.camera.core.impl.Quirk;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Quirk that prevents from getting washed out image while taking picture with flash ON/AUTO mode.
 *
 * <p>See b/176399765 and b/181966663.
 */
public class ImageCaptureWashedOutImageQuirk implements Quirk {

    // List of devices with the issue. See b/181966663.
    private static final List<String> DEVICE_MODELS = Arrays.asList(
            // Galaxy S7
            "SM-G9300",
            "SM-G930R",
            "SM-G930A",
            "SM-G930V",
            "SM-G930T",
            "SM-G930U",
            "SM-G930P",

            // Galaxy S7+
            "SM-SC02H",
            "SM-SCV33",
            "SM-G9350",
            "SM-G935R",
            "SM-G935A",
            "SM-G935V",
            "SM-G935T",
            "SM-G935U",
            "SM-G935P"
    );

    static boolean load() {
        return "SAMSUNG".equals(Build.BRAND.toUpperCase(Locale.US))
                && DEVICE_MODELS.contains(Build.MODEL.toUpperCase(Locale.US));
    }
}
