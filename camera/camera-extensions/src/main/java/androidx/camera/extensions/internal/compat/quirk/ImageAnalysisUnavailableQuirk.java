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

package androidx.camera.extensions.internal.compat.quirk;

import android.os.Build;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.Quirk;
import androidx.camera.extensions.ExtensionMode;
import androidx.camera.extensions.internal.compat.workaround.ImageAnalysisAvailability;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
/**
 * <p>QuirkSummary
 *     Bug Id: b/290007642,
 *     Description: When enabling Extensions on devices that implement the Basic Extender,
 *                  ImageAnalysis is assumed to be supported always. But this might be false on
 *                  some devices like Samsung Galaxy S23 Ultra 5G, even if the device hardware
 *                  level is FULL or above that should be able to support the additional
 *                  ImageAnalysis no matter the Preview and ImageCapture have capture processor
 *                  or not. This might cause preview black screen or unable to capture image issues.
 *     Device(s): Samsung Galaxy S23 Ultra 5G, Z Fold3 5G, A52s 5G or S22 Ultra devices
 *     @see ImageAnalysisAvailability
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ImageAnalysisUnavailableQuirk implements Quirk {
    private static final Set<Pair<String, String>> KNOWN_DEVICES = new HashSet<>(
            Arrays.asList(
                    Pair.create("samsung", "dm3q"), // Samsung Galaxy S23 Ultra 5G
                    Pair.create("samsung", "q2q"), // Samsung Galaxy Z Fold3 5G
                    Pair.create("samsung", "a52sxq"), // Samsung Galaxy A52s 5G
                    Pair.create("samsung", "b0q") // Samsung Galaxy S22 Ultra
            ));
    private final Set<Pair<String, Integer>> mUnavailableCombinations = new HashSet<>();
    ImageAnalysisUnavailableQuirk() {
        if (Build.BRAND.equalsIgnoreCase("SAMSUNG") && Build.DEVICE.equalsIgnoreCase(
                "dm3q")) { // Samsung Galaxy S23 Ultra 5G
            mUnavailableCombinations.addAll(Arrays.asList(
                    Pair.create("1", ExtensionMode.BOKEH), // LEVEL_FULL
                    Pair.create("1", ExtensionMode.FACE_RETOUCH),
                    Pair.create("3", ExtensionMode.BOKEH), // LEVEL_FULL
                    Pair.create("3", ExtensionMode.FACE_RETOUCH)
            ));
        } else if (Build.BRAND.equalsIgnoreCase("SAMSUNG") && Build.DEVICE.equalsIgnoreCase(
                "q2q")) { // Samsung Galaxy Z Fold3 5G
            mUnavailableCombinations.addAll(Arrays.asList(
                    Pair.create("0", ExtensionMode.BOKEH), // LEVEL_3
                    Pair.create("0", ExtensionMode.FACE_RETOUCH)
            ));
        } else if (Build.BRAND.equalsIgnoreCase("SAMSUNG") && Build.DEVICE.equalsIgnoreCase(
                "a52sxq")) { // Samsung Galaxy A52s 5G
            mUnavailableCombinations.addAll(Arrays.asList(
                    Pair.create("0", ExtensionMode.BOKEH), // LEVEL_3
                    Pair.create("0", ExtensionMode.FACE_RETOUCH)
            ));
        } else if (Build.BRAND.equalsIgnoreCase("SAMSUNG") && Build.DEVICE.equalsIgnoreCase(
                "b0q")) { // Samsung Galaxy A52s 5G
            mUnavailableCombinations.addAll(Arrays.asList(
                    Pair.create("3", ExtensionMode.BOKEH), // FULL
                    Pair.create("3", ExtensionMode.FACE_RETOUCH)
            ));
        }
    }
    static boolean load() {
        return KNOWN_DEVICES.contains(Pair.create(Build.BRAND.toLowerCase(Locale.US),
                Build.DEVICE.toLowerCase(Locale.US)));
    }
    /**
     * Returns whether {@link ImageAnalysis} is unavailable to be bound together with
     * {@link Preview} and {@link ImageCapture} for the specified camera id and extensions mode.
     */
    public boolean isUnavailable(@NonNull String cameraId, @ExtensionMode.Mode int mode) {
        return mUnavailableCombinations.contains(Pair.create(cameraId, mode));
    }
}
