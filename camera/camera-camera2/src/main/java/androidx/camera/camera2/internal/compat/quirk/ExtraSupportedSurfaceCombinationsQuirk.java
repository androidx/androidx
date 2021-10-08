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

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
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
 * Quirk required to include extra supported surface combinations which are additional to the
 * guaranteed supported configurations.
 *
 * <p>An example is the Samsung S7's LIMITED-level camera device can support additional
 * YUV/640x480 + PRIV/PREVIEW + YUV/MAXIMUM combination. Some other Samsung devices can support
 * additional YUV/640x480 + PRIV/PREVIEW + YUV/MAXIMUM and YUV/640x480 + YUV/PREVIEW +
 * YUV/MAXIMUM configutations (See b/194149215).
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ExtraSupportedSurfaceCombinationsQuirk implements Quirk {
    private static final String TAG = "ExtraSupportedSurfaceCombinationsQuirk";

    private static final SurfaceCombination FULL_LEVEL_YUV_PRIV_YUV_CONFIGURATION =
            createFullYuvPrivYuvConfiguration();

    private static final SurfaceCombination FULL_LEVEL_YUV_YUV_YUV_CONFIGURATION =
            createFullYuvYuvYuvConfiguration();

    private static final Set<String> SUPPORT_EXTRA_FULL_CONFIGURATIONS_SAMSUNG_MODELS =
            new HashSet<>(Arrays.asList(
                    "SM-A515F", // Galaxy A51
                    "SM-A515U", // Galaxy A51
                    "SM-A515U1", // Galaxy A51
                    "SM-A515W", // Galaxy A51
                    "SM-S515DL", // Galaxy A51
                    "SC-54A", // Galaxy A51 5G
                    "SCG07", // Galaxy A51 5G
                    "SM-A5160", // Galaxy A51 5G
                    "SM-A516B", // Galaxy A51 5G
                    "SM-A516N", // Galaxy A51 5G
                    "SM-A516U", // Galaxy A51 5G
                    "SM-A516U1", // Galaxy A51 5G
                    "SM-A516V", // Galaxy A51 5G
                    "SM-A715F", // Galaxy A71
                    "SM-A715W", // Galaxy A71
                    "SM-A7160", // Galaxy A71 5G
                    "SM-A716B", // Galaxy A71 5G
                    "SM-A716U", // Galaxy A71 5G
                    "SM-A716U1", // Galaxy A71 5G
                    "SM-A716V", // Galaxy A71 5G
                    "SM-A8050", // Galaxy A80
                    "SM-A805F", // Galaxy A80
                    "SM-A805N", // Galaxy A80
                    "SCV44", // Galaxy Fold
                    "SM-F9000", // Galaxy Fold
                    "SM-F900F", // Galaxy Fold
                    "SM-F900U", // Galaxy Fold
                    "SM-F900U1", // Galaxy Fold
                    "SM-F900W", // Galaxy Fold
                    "SM-F907B", // Galaxy Fold 5G
                    "SM-F907N", // Galaxy Fold 5G
                    "SM-N970F", // Galaxy Note10
                    "SM-N9700", // Galaxy Note10
                    "SM-N970U", // Galaxy Note10
                    "SM-N970U1", // Galaxy Note10
                    "SM-N970W", // Galaxy Note10
                    "SM-N971N", // Galaxy Note10 5G
                    "SM-N770F", // Galaxy Note10 Lite
                    "SC-01M", // Galaxy Note10+
                    "SCV45", // Galaxy Note10+
                    "SM-N9750", // Galaxy Note10+
                    "SM-N975C", // Galaxy Note10+
                    "SM-N975U", // Galaxy Note10+
                    "SM-N975U1", // Galaxy Note10+
                    "SM-N975W", // Galaxy Note10+
                    "SM-N975F", // Galaxy Note10+
                    "SM-N976B", // Galaxy Note10+ 5G
                    "SM-N976N", // Galaxy Note10+ 5G
                    "SM-N9760", // Galaxy Note10+ 5G
                    "SM-N976Q", // Galaxy Note10+ 5G
                    "SM-N976V", // Galaxy Note10+ 5G
                    "SM-N976U", // Galaxy Note10+ 5G
                    "SM-N9810", // Galaxy Note20 5G
                    "SM-N981N", // Galaxy Note20 5G
                    "SM-N981U", // Galaxy Note20 5G
                    "SM-N981U1", // Galaxy Note20 5G
                    "SM-N981W", // Galaxy Note20 5G
                    "SM-N981B", // Galaxy Note20 5G
                    "SC-53A", // Galaxy Note20 Ultra 5G
                    "SCG06", // Galaxy Note20 Ultra 5G
                    "SM-N9860", // Galaxy Note20 Ultra 5G
                    "SM-N986N", // Galaxy Note20 Ultra 5G
                    "SM-N986U", // Galaxy Note20 Ultra 5G
                    "SM-N986U1", // Galaxy Note20 Ultra 5G
                    "SM-N986W", // Galaxy Note20 Ultra 5G
                    "SM-N986B", // Galaxy Note20 Ultra 5G
                    "SC-03L", // Galaxy S10
                    "SCV41", // Galaxy S10
                    "SM-G973F", // Galaxy S10
                    "SM-G973N", // Galaxy S10
                    "SM-G9730", // Galaxy S10
                    "SM-G9738", // Galaxy S10
                    "SM-G973C", // Galaxy S10
                    "SM-G973U", // Galaxy S10
                    "SM-G973U1", // Galaxy S10
                    "SM-G973W", // Galaxy S10
                    "SM-G977B", // Galaxy S10 5G
                    "SM-G977N", // Galaxy S10 5G
                    "SM-G977P", // Galaxy S10 5G
                    "SM-G977T", // Galaxy S10 5G
                    "SM-G977U", // Galaxy S10 5G
                    "SM-G770F", // Galaxy S10 Lite
                    "SM-G770U1", // Galaxy S10 Lite
                    "SC-04L", // Galaxy S10+
                    "SCV42", // Galaxy S10+
                    "SM-G975F", // Galaxy S10+
                    "SM-G975N", // Galaxy S10+
                    "SM-G9750", // Galaxy S10+
                    "SM-G9758", // Galaxy S10+
                    "SM-G975U", // Galaxy S10+
                    "SM-G975U1", // Galaxy S10+
                    "SM-G975W", // Galaxy S10+
                    "SC-05L", // Galaxy S10+ Olympic Games Edition
                    "SM-G970F", // Galaxy S10e
                    "SM-G970N", // Galaxy S10e
                    "SM-G9700", // Galaxy S10e
                    "SM-G9708", // Galaxy S10e
                    "SM-G970U", // Galaxy S10e
                    "SM-G970U1", // Galaxy S10e
                    "SM-G970W", // Galaxy S10e
                    "SC-51A", // Galaxy S20 5G
                    "SC51Aa", // Galaxy S20 5G
                    "SCG01", // Galaxy S20 5G
                    "SM-G9810", // Galaxy S20 5G
                    "SM-G981N", // Galaxy S20 5G
                    "SM-G981U", // Galaxy S20 5G
                    "SM-G981U1", // Galaxy S20 5G
                    "SM-G981V", // Galaxy S20 5G
                    "SM-G981W", // Galaxy S20 5G
                    "SM-G981B", // Galaxy S20 5G
                    "SCG03", // Galaxy S20 Ultra 5G
                    "SM-G9880", // Galaxy S20 Ultra 5G
                    "SM-G988N", // Galaxy S20 Ultra 5G
                    "SM-G988Q", // Galaxy S20 Ultra 5G
                    "SM-G988U", // Galaxy S20 Ultra 5G
                    "SM-G988U1", // Galaxy S20 Ultra 5G
                    "SM-G988W", // Galaxy S20 Ultra 5G
                    "SM-G988B", // Galaxy S20 Ultra 5G
                    "SC-52A", // Galaxy S20+ 5G
                    "SCG02", // Galaxy S20+ 5G
                    "SM-G9860", // Galaxy S20+ 5G
                    "SM-G986N", // Galaxy S20+ 5G
                    "SM-G986U", // Galaxy S20+ 5G
                    "SM-G986U1", // Galaxy S20+ 5G
                    "SM-G986W", // Galaxy S20+ 5G
                    "SM-G986B", // Galaxy S20+ 5G
                    "SCV47", // Galaxy Z Flip
                    "SM-F7000", // Galaxy Z Flip
                    "SM-F700F", // Galaxy Z Flip
                    "SM-F700N", // Galaxy Z Flip
                    "SM-F700U", // Galaxy Z Flip
                    "SM-F700U1", // Galaxy Z Flip
                    "SM-F700W", // Galaxy Z Flip
                    "SCG04", // Galaxy Z Flip 5G
                    "SM-F7070", // Galaxy Z Flip 5G
                    "SM-F707B", // Galaxy Z Flip 5G
                    "SM-F707N", // Galaxy Z Flip 5G
                    "SM-F707U", // Galaxy Z Flip 5G
                    "SM-F707U1", // Galaxy Z Flip 5G
                    "SM-F707W", // Galaxy Z Flip 5G
                    "SM-F9160", // Galaxy Z Fold2 5G
                    "SM-F916B", // Galaxy Z Fold2 5G
                    "SM-F916N", // Galaxy Z Fold2 5G
                    "SM-F916Q", // Galaxy Z Fold2 5G
                    "SM-F916U", // Galaxy Z Fold2 5G
                    "SM-F916U1", // Galaxy Z Fold2 5G
                    "SM-F916W"// Galaxy Z Fold2 5G
            ));

    static boolean load() {
        return isSamsungS7() || supportExtraFullConfigurationsSamsungDevice();
    }

    private static boolean isSamsungS7() {
        return "heroqltevzw".equalsIgnoreCase(Build.DEVICE) || "heroqltetmo".equalsIgnoreCase(
                Build.DEVICE);
    }

    private static boolean supportExtraFullConfigurationsSamsungDevice() {
        if (!"samsung".equalsIgnoreCase(Build.BRAND)) {
            return false;
        }

        String capitalModelName = Build.MODEL.toUpperCase(Locale.US);

        return SUPPORT_EXTRA_FULL_CONFIGURATIONS_SAMSUNG_MODELS.contains(capitalModelName);
    }

    /**
     * Returns the extra supported surface combinations for specific camera on the device.
     */
    @NonNull
    public List<SurfaceCombination> getExtraSupportedSurfaceCombinations(@NonNull String cameraId,
            int hardwareLevel) {
        if (isSamsungS7()) {
            return getSamsungS7ExtraCombinations(cameraId);
        }

        if (supportExtraFullConfigurationsSamsungDevice()) {
            return getLimitedDeviceExtraSupportedFullConfigurations(hardwareLevel);
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
    private List<SurfaceCombination> getLimitedDeviceExtraSupportedFullConfigurations(
            int hardwareLevel) {
        List<SurfaceCombination> extraCombinations = new ArrayList<>();

        if (hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED) {
            // (YUV, ANALYSIS) + (PRIV, PREVIEW) + (YUV, MAXIMUM)
            extraCombinations.add(FULL_LEVEL_YUV_PRIV_YUV_CONFIGURATION);
            // (YUV, ANALYSIS) + (YUV, PREVIEW) + (YUV, MAXIMUM)
            extraCombinations.add(FULL_LEVEL_YUV_YUV_YUV_CONFIGURATION);
        }

        return extraCombinations;
    }

    @NonNull
    private static SurfaceCombination createFullYuvPrivYuvConfiguration() {
        // (YUV, ANALYSIS) + (PRIV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination = new SurfaceCombination();
        surfaceCombination.addSurfaceConfig(SurfaceConfig.create(SurfaceConfig.ConfigType.YUV,
                SurfaceConfig.ConfigSize.ANALYSIS));
        surfaceCombination.addSurfaceConfig(SurfaceConfig.create(SurfaceConfig.ConfigType.PRIV,
                SurfaceConfig.ConfigSize.PREVIEW));
        surfaceCombination.addSurfaceConfig(SurfaceConfig.create(SurfaceConfig.ConfigType.YUV,
                SurfaceConfig.ConfigSize.MAXIMUM));

        return surfaceCombination;
    }

    @NonNull
    private static SurfaceCombination createFullYuvYuvYuvConfiguration() {
        // (YUV, ANALYSIS) + (YUV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination = new SurfaceCombination();
        surfaceCombination.addSurfaceConfig(SurfaceConfig.create(SurfaceConfig.ConfigType.YUV,
                SurfaceConfig.ConfigSize.ANALYSIS));
        surfaceCombination.addSurfaceConfig(SurfaceConfig.create(SurfaceConfig.ConfigType.YUV,
                SurfaceConfig.ConfigSize.PREVIEW));
        surfaceCombination.addSurfaceConfig(SurfaceConfig.create(SurfaceConfig.ConfigType.YUV,
                SurfaceConfig.ConfigSize.MAXIMUM));

        return surfaceCombination;
    }
}
