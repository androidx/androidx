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
import androidx.camera.core.Logger;
import androidx.camera.core.impl.Quirk;
import androidx.camera.core.impl.SurfaceCombination;
import androidx.camera.core.impl.SurfaceConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Quirk required to include extra supported surface combinations which are additional to the
 * guaranteed supported configurations.
 *
 * <p>An example is the Samsung S7 device can support additional YUV/640x480 + PRIV/PREVIEW +
 * YUV/MAXIMUM combination.
 */
public class ExtraSupportedSurfaceCombinationsQuirk implements Quirk {
    private static final String TAG = "ExtraSupportedSurfaceCombinationsQuirk";

    static boolean load() {
        return isSamsungS7();
    }

    private static boolean isSamsungS7() {
        return "Samsung".equalsIgnoreCase(Build.BRAND) && ("heroqltevzw".equalsIgnoreCase(
                Build.DEVICE) || "heroqltetmo".equalsIgnoreCase(Build.DEVICE));
    }

    /**
     * Returns the extra supported surface combinations for specific camera on the device.
     */
    @NonNull
    public List<SurfaceCombination> getExtraSupportedSurfaceCombinations(@NonNull String cameraId) {
        if (isSamsungS7()) {
            return getSamsungS7ExtraCombinations(cameraId);
        }

        Logger.w(TAG,
                "Cannot retrieve list of extra supported surface combinations on this device.");
        return Collections.emptyList();
    }

    @NonNull
    private List<SurfaceCombination> getSamsungS7ExtraCombinations(@NonNull String cameraId) {
        List<SurfaceCombination> extraCombinations = new ArrayList<>();

        if (cameraId.equals("1")) {
            // (YUV, ANALYSIS) + (PRIV, PREVIEW) + (YUV, MAXIMUM)
            SurfaceCombination surfaceCombination = new SurfaceCombination();
            surfaceCombination.addSurfaceConfig(SurfaceConfig.create(SurfaceConfig.ConfigType.YUV,
                    SurfaceConfig.ConfigSize.ANALYSIS));
            surfaceCombination.addSurfaceConfig(SurfaceConfig.create(SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.PREVIEW));
            surfaceCombination.addSurfaceConfig(SurfaceConfig.create(SurfaceConfig.ConfigType.YUV,
                    SurfaceConfig.ConfigSize.MAXIMUM));
            extraCombinations.add(surfaceCombination);
        }

        return extraCombinations;
    }
}
