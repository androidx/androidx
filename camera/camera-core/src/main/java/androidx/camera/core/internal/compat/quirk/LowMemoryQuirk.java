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

package androidx.camera.core.internal.compat.quirk;

import android.os.Build;

import androidx.camera.core.impl.Quirk;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * <p>QuirkSummary
 *     Bug Id: 235321365
 *     Description: For the devices in low spec which may not have enough memory to process the
 *     image cropping and apply effects in parallel.
 *     Device(s): Samsung Galaxy A5, Motorola Moto G (3rd gen)
 */
public class LowMemoryQuirk implements Quirk {

    // TODO(b/258618028): Making a public API and giving developers the option to set the devices
    //  having the Quirk.
    private static final Set<String> DEVICE_MODELS = new HashSet<>(Arrays.asList(
            "SM-A520W", // Samsung Galaxy A5
            "MOTOG3" // Motorola Moto G (3rd gen)
    ));

    static boolean load() {
        return DEVICE_MODELS.contains(Build.MODEL.toUpperCase(Locale.US));
    }
}
