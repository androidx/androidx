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

package androidx.camera.camera2.internal.compat.quirk;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.compat.workaround.ExtraSupportedSurfaceCombinationsContainer;
import androidx.camera.core.impl.Quirk;
import androidx.camera.core.impl.SurfaceCombination;
import androidx.camera.core.impl.SurfaceConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * <p>QuirkSummary
 *     Bug Id: b/194149215
 *     Description: Quirk required to include extra supported surface combinations which are
 *                  additional to the guaranteed supported configurations. An example is the
 *                  Samsung S7's LIMITED-level camera device can support additional YUV/640x480 +
 *                  PRIV/PREVIEW + YUV/MAXIMUM combination.
 *     Device(s): Samsung S7 devices
 *     @see ExtraSupportedSurfaceCombinationsContainer
 */
public class ExtraSupportedSurfaceCombinationsQuirk implements Quirk {
    private static final String TAG = "ExtraSupportedSurfaceCombinationsQuirk";

    private static final SurfaceCombination FULL_LEVEL_YUV_PRIV_YUV_CONFIGURATION =
            createFullYuvPrivYuvConfiguration();

    private static final SurfaceCombination LEVEL_3_LEVEL_PRIV_PRIV_YUV_SUBSET_CONFIGURATION =
            createLevel3PrivPrivYuvSubsetConfiguration();

    private static final Set<String> SUPPORT_EXTRA_LEVEL_3_CONFIGURATIONS_GOOGLE_MODELS =
            new HashSet<>(Arrays.asList(
                    "PIXEL 6",
                    "PIXEL 6 PRO",
                    "PIXEL 7",
                    "PIXEL 7 PRO",
                    "PIXEL 8",
                    "PIXEL 8 PRO"));

    private static final Set<String> SUPPORT_EXTRA_LEVEL_3_CONFIGURATIONS_SAMSUNG_MODELS =
            new HashSet<>(Arrays.asList(
                    "SM-S921", // Galaxy S24
                    "SC-51E",  // Galaxy S24
                    "SCG25",   // Galaxy S24
                    "SM-S926", // Galaxy S24+
                    "SM-S928", // Galaxy S24 Ultra
                    "SC-52E",  // Galaxy S24 Ultra
                    "SCG26"   // Galaxy S24 Ultra
              ));

    static boolean load() {
        return isSamsungS7() || supportExtraLevel3ConfigurationsGoogleDevice()
                || supportExtraLevel3ConfigurationsSamsungDevice();
    }

    private static boolean isSamsungS7() {
        return "heroqltevzw".equalsIgnoreCase(Build.DEVICE) || "heroqltetmo".equalsIgnoreCase(
                Build.DEVICE);
    }

    private static boolean supportExtraLevel3ConfigurationsGoogleDevice() {
        if (!"google".equalsIgnoreCase(Build.BRAND)) {
            return false;
        }

        String capitalModelName = Build.MODEL.toUpperCase(Locale.US);

        return SUPPORT_EXTRA_LEVEL_3_CONFIGURATIONS_GOOGLE_MODELS.contains(capitalModelName);
    }

    private static boolean supportExtraLevel3ConfigurationsSamsungDevice() {
        if (!"samsung".equalsIgnoreCase(Build.BRAND)) {
            return false;
        }

        String capitalModelName = Build.MODEL.toUpperCase(Locale.US);

        // Check if the device model starts with the one of the predefined models
        for (String supportedModel : SUPPORT_EXTRA_LEVEL_3_CONFIGURATIONS_SAMSUNG_MODELS) {
            if (capitalModelName.startsWith(supportedModel)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the extra supported surface combinations for specific camera on the device.
     */
    @NonNull
    public List<SurfaceCombination> getExtraSupportedSurfaceCombinations(@NonNull String cameraId) {
        if (isSamsungS7()) {
            return getSamsungS7ExtraCombinations(cameraId);
        }

        if (supportExtraLevel3ConfigurationsGoogleDevice()
                || supportExtraLevel3ConfigurationsSamsungDevice()) {
            return Collections.singletonList(LEVEL_3_LEVEL_PRIV_PRIV_YUV_SUBSET_CONFIGURATION);
        }

        return Collections.emptyList();
    }

    @NonNull
    private List<SurfaceCombination> getSamsungS7ExtraCombinations(@NonNull String cameraId) {
        List<SurfaceCombination> extraCombinations = new ArrayList<>();

        if (cameraId.equals("1")) {
            // (YUV, ANALYSIS) + (PRIV, PREVIEW) + (YUV, MAXIMUM)
            extraCombinations.add(FULL_LEVEL_YUV_PRIV_YUV_CONFIGURATION);
        }

        return extraCombinations;
    }

    @NonNull
    private static SurfaceCombination createFullYuvPrivYuvConfiguration() {
        // (YUV, ANALYSIS) + (PRIV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination = new SurfaceCombination();
        surfaceCombination.addSurfaceConfig(SurfaceConfig.create(SurfaceConfig.ConfigType.YUV,
                SurfaceConfig.ConfigSize.VGA));
        surfaceCombination.addSurfaceConfig(SurfaceConfig.create(SurfaceConfig.ConfigType.PRIV,
                SurfaceConfig.ConfigSize.PREVIEW));
        surfaceCombination.addSurfaceConfig(SurfaceConfig.create(SurfaceConfig.ConfigType.YUV,
                SurfaceConfig.ConfigSize.MAXIMUM));

        return surfaceCombination;
    }

    /**
     * Creates (PRIV, PREVIEW) + (PRIV, ANALYSIS) + (YUV, MAXIMUM) surface combination.
     *
     * <p>This is a subset of LEVEL_3 camera devices'
     * (PRIV, PREVIEW) + (PRIV, ANALYSIS) + (YUV, MAXIMUM) + (RAW, MAXIMUM)
     * guaranteed supported configuration. This configuration has been verified to make sure that
     * the surface combination can work well on the target devices.
     */
    private static SurfaceCombination createLevel3PrivPrivYuvSubsetConfiguration() {
        // (PRIV, PREVIEW) + (PRIV, ANALYSIS) + (YUV, MAXIMUM) + (RAW, MAXIMUM)
        SurfaceCombination surfaceCombination = new SurfaceCombination();
        surfaceCombination.addSurfaceConfig(SurfaceConfig.create(SurfaceConfig.ConfigType.PRIV,
                SurfaceConfig.ConfigSize.PREVIEW));
        surfaceCombination.addSurfaceConfig(SurfaceConfig.create(SurfaceConfig.ConfigType.PRIV,
                SurfaceConfig.ConfigSize.VGA));
        surfaceCombination.addSurfaceConfig(SurfaceConfig.create(SurfaceConfig.ConfigType.YUV,
                SurfaceConfig.ConfigSize.MAXIMUM));

        return surfaceCombination;
    }
}
