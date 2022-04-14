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

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.util.Pair;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

import java.nio.BufferUnderflowException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * A quirk for devices that throw a
 * {@link BufferUnderflowException} when querying the flash availability.
 *
 * <p>QuirkSummary
 *     Bug Id: 216667482
 *     Description: When attempting to retrieve the
 *                  {@link CameraCharacteristics#FLASH_INFO_AVAILABLE} characteristic, a
 *                  {@link BufferUnderflowException} is thrown. This is an undocumented exception
 *                  on the {@link CameraCharacteristics#get(CameraCharacteristics.Key)} method,
 *                  so this violates the API contract.
 *     Device(s): LEMFO LEMP (a.k.a. Spreadtrum LEMP)
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class FlashAvailabilityBufferUnderflowQuirk implements Quirk {
    private static final Set<Pair<String, String>> KNOWN_AFFECTED_MODELS = new HashSet<>(
            Arrays.asList(
                    // Devices enumerated as Pair(Build.MANUFACTURER, Build.MODEL)
                    new Pair<>("sprd", "lemp")
            ));

    static boolean load() {
        return KNOWN_AFFECTED_MODELS.contains(new Pair<>(Build.MANUFACTURER.toLowerCase(Locale.US),
                Build.MODEL.toLowerCase(Locale.US)));
    }
}
