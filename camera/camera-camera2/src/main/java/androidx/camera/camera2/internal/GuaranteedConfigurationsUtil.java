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

package androidx.camera.camera2.internal;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.SurfaceCombination;
import androidx.camera.core.impl.SurfaceConfig;
import androidx.camera.core.impl.SurfaceConfig.ConfigSize;
import androidx.camera.core.impl.SurfaceConfig.ConfigType;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for providing hardcoded sets of supported stream combinations based on the
 * hardware level and capabilities of the device.
 *
 * <pre>
 * @see <a href="https://developer.android.com/reference/android/hardware/camera2/CameraDevice">
 *     CameraDevice</a>
 * </pre>
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class GuaranteedConfigurationsUtil {

    private GuaranteedConfigurationsUtil() {
    }

    /**
     * Returns the at least supported stream combinations for legacy devices.
     */
    @NonNull
    public static List<SurfaceCombination> getLegacySupportedCombinationList() {
        List<SurfaceCombination> combinationList = new ArrayList<>();

        // (PRIV, MAXIMUM)
        SurfaceCombination surfaceCombination1 = new SurfaceCombination();
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination1);

        // (JPEG, MAXIMUM)
        SurfaceCombination surfaceCombination2 = new SurfaceCombination();
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination2);

        // (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination3 = new SurfaceCombination();
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination3);

        // Below two combinations are all supported in the combination
        // (PRIV, PREVIEW) + (JPEG, MAXIMUM)
        SurfaceCombination surfaceCombination4 = new SurfaceCombination();
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination4);

        // (YUV, PREVIEW) + (JPEG, MAXIMUM)
        SurfaceCombination surfaceCombination5 = new SurfaceCombination();
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination5);

        // (PRIV, PREVIEW) + (PRIV, PREVIEW)
        SurfaceCombination surfaceCombination6 = new SurfaceCombination();
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        combinationList.add(surfaceCombination6);

        // (PRIV, PREVIEW) + (YUV, PREVIEW)
        SurfaceCombination surfaceCombination7 = new SurfaceCombination();
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        combinationList.add(surfaceCombination7);

        // (PRIV, PREVIEW) + (PRIV, PREVIEW) + (JPEG, MAXIMUM)
        SurfaceCombination surfaceCombination8 = new SurfaceCombination();
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination8);

        return combinationList;
    }

    /**
     * Returns the at least supported stream combinations for limited-level devices
     * in addition to those for legacy devices.
     */
    @NonNull
    public static List<SurfaceCombination> getLimitedSupportedCombinationList() {
        List<SurfaceCombination> combinationList = new ArrayList<>();

        // (PRIV, PREVIEW) + (PRIV, RECORD)
        SurfaceCombination surfaceCombination1 = new SurfaceCombination();
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD));
        combinationList.add(surfaceCombination1);

        // (PRIV, PREVIEW) + (YUV, RECORD)
        SurfaceCombination surfaceCombination2 = new SurfaceCombination();
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.RECORD));
        combinationList.add(surfaceCombination2);

        // (YUV, PREVIEW) + (YUV, RECORD)
        SurfaceCombination surfaceCombination3 = new SurfaceCombination();
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.RECORD));
        combinationList.add(surfaceCombination3);

        // (PRIV, PREVIEW) + (PRIV, RECORD) + (JPEG, RECORD)
        SurfaceCombination surfaceCombination4 = new SurfaceCombination();
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD));
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.RECORD));
        combinationList.add(surfaceCombination4);

        // (PRIV, PREVIEW) + (YUV, RECORD) + (JPEG, RECORD)
        SurfaceCombination surfaceCombination5 = new SurfaceCombination();
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.RECORD));
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.RECORD));
        combinationList.add(surfaceCombination5);

        // (YUV, PREVIEW) + (YUV, PREVIEW) + (JPEG, MAXIMUM)
        SurfaceCombination surfaceCombination6 = new SurfaceCombination();
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination6);

        return combinationList;
    }

    /**
     * Returns the at least supported stream combinations for full-level devices
     * in addition to those for limited-level and legacy devices.
     */
    @NonNull
    public static List<SurfaceCombination> getFullSupportedCombinationList() {
        List<SurfaceCombination> combinationList = new ArrayList<>();

        // (PRIV, PREVIEW) + (PRIV, MAXIMUM)
        SurfaceCombination surfaceCombination1 = new SurfaceCombination();
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination1);

        // (PRIV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination2 = new SurfaceCombination();
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination2);

        // (YUV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination3 = new SurfaceCombination();
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination3);

        // (PRIV, PREVIEW) + (PRIV, PREVIEW) + (JPEG, MAXIMUM)
        SurfaceCombination surfaceCombination4 = new SurfaceCombination();
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination4);

        // (YUV, ANALYSIS) + (PRIV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination5 = new SurfaceCombination();
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.VGA));
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination5);

        // (YUV, ANALYSIS) + (YUV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination6 = new SurfaceCombination();
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.VGA));
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination6);

        return combinationList;
    }

    /**
     * Returns the at least supported stream combinations for RAW-capability devices
     * on both full and limited devices.
     */
    @NonNull
    public static List<SurfaceCombination> getRAWSupportedCombinationList() {
        List<SurfaceCombination> combinationList = new ArrayList<>();

        // (RAW, MAXIMUM)
        SurfaceCombination surfaceCombination1 = new SurfaceCombination();
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination1);

        // (PRIV, PREVIEW) + (RAW, MAXIMUM)
        SurfaceCombination surfaceCombination2 = new SurfaceCombination();
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination2);

        // (YUV, PREVIEW) + (RAW, MAXIMUM)
        SurfaceCombination surfaceCombination3 = new SurfaceCombination();
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination3);

        // (PRIV, PREVIEW) + (PRIV, PREVIEW) + (RAW, MAXIMUM)
        SurfaceCombination surfaceCombination4 = new SurfaceCombination();
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination4);

        // (PRIV, PREVIEW) + (YUV, PREVIEW) + (RAW, MAXIMUM)
        SurfaceCombination surfaceCombination5 = new SurfaceCombination();
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination5);

        // (YUV, PREVIEW) + (YUV, PREVIEW) + (RAW, MAXIMUM)
        SurfaceCombination surfaceCombination6 = new SurfaceCombination();
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination6);

        // (PRIV, PREVIEW) + (JPEG, MAXIMUM) + (RAW, MAXIMUM)
        SurfaceCombination surfaceCombination7 = new SurfaceCombination();
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination7);

        // (YUV, PREVIEW) + (JPEG, MAXIMUM) + (RAW, MAXIMUM)
        SurfaceCombination surfaceCombination8 = new SurfaceCombination();
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination8);

        return combinationList;
    }

    /**
     * Returns the at least supported stream combinations for BURST-capability devices
     * in addition to those for limited device. Note that all FULL-level devices support the
     * BURST capability, and the below list is a strict subset of the list for FULL-level
     * devices, so this table is only relevant for LIMITED-level devices that support the
     * BURST_CAPTURE capability.
     */
    @NonNull
    public static List<SurfaceCombination> getBurstSupportedCombinationList() {
        List<SurfaceCombination> combinationList = new ArrayList<>();

        // (PRIV, PREVIEW) + (PRIV, MAXIMUM)
        SurfaceCombination surfaceCombination1 = new SurfaceCombination();
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination1);

        // (PRIV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination2 = new SurfaceCombination();
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination2);

        // (YUV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination3 = new SurfaceCombination();
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination3);

        return combinationList;
    }

    /**
     * Returns the at least supported stream combinations for level-3 devices
     * in addition to tje combinations for full and for RAW capability.
     */
    @NonNull
    public static List<SurfaceCombination> getLevel3SupportedCombinationList() {
        List<SurfaceCombination> combinationList = new ArrayList<>();

        // (PRIV, PREVIEW) + (PRIV, ANALYSIS) + (YUV, MAXIMUM) + (RAW, MAXIMUM)
        SurfaceCombination surfaceCombination1 = new SurfaceCombination();
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.VGA));
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination1);

        // (PRIV, PREVIEW) + (PRIV, ANALYSIS) + (JPEG, MAXIMUM) + (RAW, MAXIMUM)
        SurfaceCombination surfaceCombination2 = new SurfaceCombination();
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.VGA));
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination2);

        return combinationList;
    }

    /**
     * Returns the at least supported stream combinations for the ultra high resolution pixel
     * sensor mode.
     */
    @NonNull
    public static List<SurfaceCombination> getUltraHighResolutionSupportedCombinationList() {
        List<SurfaceCombination> combinationList = new ArrayList<>();

        // (YUV, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (PRIV, RECORD)
        // Covers (YUV, ULTRA_MAXIMUM) + (PRIV, PREVIEW) in the guaranteed table.
        SurfaceCombination surfaceCombination1 = new SurfaceCombination();
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.ULTRA_MAXIMUM));
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD));
        combinationList.add(surfaceCombination1);

        // (JPEG, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (PRIV, RECORD)
        // Covers (JPEG, ULTRA_MAXIMUM) + (PRIV, PREVIEW) in the guaranteed table.
        SurfaceCombination surfaceCombination2 = new SurfaceCombination();
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.ULTRA_MAXIMUM));
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD));
        combinationList.add(surfaceCombination2);

        // (RAW, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (PRIV, RECORD)
        // Covers (RAW, ULTRA_MAXIMUM) + (PRIV, PREVIEW) in the guaranteed table.
        SurfaceCombination surfaceCombination3 = new SurfaceCombination();
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.ULTRA_MAXIMUM));
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD));
        combinationList.add(surfaceCombination3);

        // (YUV, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (JPEG, MAXIMUM)
        SurfaceCombination surfaceCombination4 = new SurfaceCombination();
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.ULTRA_MAXIMUM));
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination4);

        // (JPEG, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (JPEG, MAXIMUM)
        SurfaceCombination surfaceCombination5 = new SurfaceCombination();
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.ULTRA_MAXIMUM));
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination5);

        // (RAW, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (JPEG, MAXIMUM)
        SurfaceCombination surfaceCombination6 = new SurfaceCombination();
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.ULTRA_MAXIMUM));
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination6);

        // (YUV, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (YUV, MAXIMUM)
        // Covers (YUV, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (YUV, RECORD) in the guaranteed table.
        SurfaceCombination surfaceCombination7 = new SurfaceCombination();
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.ULTRA_MAXIMUM));
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination7);

        // (JPEG, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (YUV, MAXIMUM)
        // Covers (JPEG, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (YUV, RECORD) in the guaranteed table.
        SurfaceCombination surfaceCombination8 = new SurfaceCombination();
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.ULTRA_MAXIMUM));
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination8);

        // (RAW, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (YUV, MAXIMUM)
        // Covers (RAW, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (YUV, RECORD) in the guaranteed table.
        SurfaceCombination surfaceCombination9 = new SurfaceCombination();
        surfaceCombination9.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.ULTRA_MAXIMUM));
        surfaceCombination9.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination9.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination9);

        // (YUV, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (RAW, MAXIMUM)
        SurfaceCombination surfaceCombination10 = new SurfaceCombination();
        surfaceCombination10.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.ULTRA_MAXIMUM));
        surfaceCombination10.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination10.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination10);

        // (JPEG, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (RAW, MAXIMUM)
        SurfaceCombination surfaceCombination11 = new SurfaceCombination();
        surfaceCombination11.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.ULTRA_MAXIMUM));
        surfaceCombination11.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination11.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination11);

        // (RAW, ULTRA_MAXIMUM) + (PRIV, PREVIEW) + (RAW, MAXIMUM)
        SurfaceCombination surfaceCombination12 = new SurfaceCombination();
        surfaceCombination12.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.ULTRA_MAXIMUM));
        surfaceCombination12.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination12.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.RAW, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination12);

        return combinationList;
    }

    /**
     * Returns the minimally guaranteed stream combinations when one or more
     * streams are configured as a 10-bit input.
     */
    @NonNull
    public static List<SurfaceCombination> get10BitSupportedCombinationList() {
        List<SurfaceCombination> combinationList = new ArrayList<>();

        // (PRIV, MAXIMUM)
        SurfaceCombination surfaceCombination1 = new SurfaceCombination();
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination1);

        // (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination2 = new SurfaceCombination();
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination2);

        // (PRIV, PREVIEW) + (JPEG, MAXIMUM)
        SurfaceCombination surfaceCombination3 = new SurfaceCombination();
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination3);

        // (PRIV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination4 = new SurfaceCombination();
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination4);

        // (YUV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination5 = new SurfaceCombination();
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination5);

        // (PRIV, PREVIEW) + (PRIV, RECORD)
        SurfaceCombination surfaceCombination6 = new SurfaceCombination();
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD));
        combinationList.add(surfaceCombination6);

        // (PRIV, PREVIEW) + (PRIV, RECORD) + (YUV, RECORD)
        SurfaceCombination surfaceCombination7 = new SurfaceCombination();
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD));
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.RECORD));
        combinationList.add(surfaceCombination7);

        // (PRIV, PREVIEW) + (PRIV, RECORD) + (JPEG, RECORD)
        SurfaceCombination surfaceCombination8 = new SurfaceCombination();
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD));
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.RECORD));
        combinationList.add(surfaceCombination8);

        return combinationList;
    }

    /**
     * Returns the at least supported stream combinations for concurrent cameras.
     */
    @NonNull
    public static List<SurfaceCombination> getConcurrentSupportedCombinationList() {
        List<SurfaceCombination> combinationList = new ArrayList<>();

        // (YUV, s1440p)
        SurfaceCombination surfaceCombination1 = new SurfaceCombination();
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.s1440p));
        combinationList.add(surfaceCombination1);

        // (PRIV, s1440p)
        SurfaceCombination surfaceCombination2 = new SurfaceCombination();
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.s1440p));
        combinationList.add(surfaceCombination2);

        // (JPEG, s1440p)
        SurfaceCombination surfaceCombination3 = new SurfaceCombination();
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.s1440p));
        combinationList.add(surfaceCombination3);

        // (YUV, s720p) + (JPEG, s1440p)
        SurfaceCombination surfaceCombination4 = new SurfaceCombination();
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.s720p));
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.s1440p));
        combinationList.add(surfaceCombination4);

        // (PRIV, s720p) + (JPEG, s1440p)
        SurfaceCombination surfaceCombination5 = new SurfaceCombination();
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.s720p));
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.s1440p));
        combinationList.add(surfaceCombination5);

        // (YUV, s720p) + (YUV, s1440p)
        SurfaceCombination surfaceCombination6 = new SurfaceCombination();
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.s720p));
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.s1440p));
        combinationList.add(surfaceCombination6);

        // (YUV, s720p) + (PRIV, s1440p)
        SurfaceCombination surfaceCombination7 = new SurfaceCombination();
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.s720p));
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.s1440p));
        combinationList.add(surfaceCombination7);

        // (PRIV, s720p) + (YUV, s1440p)
        SurfaceCombination surfaceCombination8 = new SurfaceCombination();
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.s720p));
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.s1440p));
        combinationList.add(surfaceCombination8);

        // (PRIV, s720p) + (PRIV, s1440p)
        SurfaceCombination surfaceCombination9 = new SurfaceCombination();
        surfaceCombination9.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.s720p));
        surfaceCombination9.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.s1440p));
        combinationList.add(surfaceCombination9);

        return combinationList;
    }

    /**
     * Returns the entire supported stream combinations for devices with Stream Use Case capability
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @NonNull
    public static List<SurfaceCombination> getStreamUseCaseSupportedCombinationList() {
        List<SurfaceCombination> combinationList = new ArrayList<>();

        // (PRIV, s1440p, PREVIEW_VIDEO_STILL)
        SurfaceCombination surfaceCombination1 = new SurfaceCombination();
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.s1440p,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL));
        combinationList.add(surfaceCombination1);

        // (YUV, s1440p, PREVIEW_VIDEO_STILL)
        SurfaceCombination surfaceCombination2 = new SurfaceCombination();
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.s1440p,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL));
        combinationList.add(surfaceCombination2);

        // (PRIV, RECORD, VIDEO_RECORD)
        SurfaceCombination surfaceCombination3 = new SurfaceCombination();
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD));
        combinationList.add(surfaceCombination3);

        // (YUV, RECORD, VIDEO_RECORD)
        SurfaceCombination surfaceCombination4 = new SurfaceCombination();
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.RECORD,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD));
        combinationList.add(surfaceCombination4);

        // (JPEG, MAXIMUM, STILL_CAPTURE)
        SurfaceCombination surfaceCombination5 = new SurfaceCombination();
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE));
        combinationList.add(surfaceCombination5);

        // (YUV, MAXIMUM, STILL_CAPTURE)
        SurfaceCombination surfaceCombination6 = new SurfaceCombination();
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE));
        combinationList.add(surfaceCombination6);

        // (PRIV, PREVIEW, PREVIEW) + (JPEG, MAXIMUM, STILL_CAPTURE)
        SurfaceCombination surfaceCombination7 = new SurfaceCombination();
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE));
        combinationList.add(surfaceCombination7);

        // (PRIV, PREVIEW, PREVIEW) + (YUV, MAXIMUM, STILL_CAPTURE)
        SurfaceCombination surfaceCombination8 = new SurfaceCombination();
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE));
        combinationList.add(surfaceCombination8);

        // (PRIV, PREVIEW, PREVIEW) + (PRIV, RECORD, VIDEO_RECORD)
        SurfaceCombination surfaceCombination9 = new SurfaceCombination();
        surfaceCombination9.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));
        surfaceCombination9.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD));
        combinationList.add(surfaceCombination9);

        // (PRIV, PREVIEW, PREVIEW) + (YUV, RECORD, VIDEO_RECORD)
        SurfaceCombination surfaceCombination10 = new SurfaceCombination();
        surfaceCombination10.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));
        surfaceCombination10.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.RECORD,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD));
        combinationList.add(surfaceCombination10);

        // (PRIV, PREVIEW, PREVIEW) + (YUV, PREVIEW, PREVIEW)
        SurfaceCombination surfaceCombination11 = new SurfaceCombination();
        surfaceCombination11.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));
        surfaceCombination11.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));
        combinationList.add(surfaceCombination11);

        // (PRIV, PREVIEW, PREVIEW) + (PRIV, RECORD, VIDEO_RECORD) + (JPEG, RECORD, STILL_CAPTURE)
        SurfaceCombination surfaceCombination12 = new SurfaceCombination();
        surfaceCombination12.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));
        surfaceCombination12.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD,
                        CameraMetadata.CONTROL_CAPTURE_INTENT_VIDEO_RECORD));
        surfaceCombination12.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.RECORD,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE));
        combinationList.add(surfaceCombination12);

        // (PRIV, PREVIEW, PREVIEW) + (YUV, RECORD, VIDEO_RECORD) + (JPEG, RECORD, STILL_CAPTURE)
        SurfaceCombination surfaceCombination13 = new SurfaceCombination();
        surfaceCombination13.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));
        surfaceCombination13.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.RECORD,
                        CameraMetadata.CONTROL_CAPTURE_INTENT_VIDEO_RECORD));
        surfaceCombination13.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.RECORD,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE));
        combinationList.add(surfaceCombination13);

        // (PRIV, PREVIEW, PREVIEW) + (YUV, PREVIEW, PREVIEW) + (JPEG, MAXIMUM, STILL_CAPTURE)
        SurfaceCombination surfaceCombination14 = new SurfaceCombination();
        surfaceCombination14.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));
        surfaceCombination14.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));
        surfaceCombination14.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM,
                        CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE));
        combinationList.add(surfaceCombination14);

        return combinationList;
    }

    /**
     * Returns the supported stream combinations for preview stabilization.
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @NonNull
    public static List<SurfaceCombination> getPreviewStabilizationSupportedCombinationList() {
        List<SurfaceCombination> combinationList = new ArrayList<>();

        // (PRIV, s1440p)
        SurfaceCombination surfaceCombination1 = new SurfaceCombination();
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.s1440p));
        combinationList.add(surfaceCombination1);

        // (YUV, s1440p)
        SurfaceCombination surfaceCombination2 = new SurfaceCombination();
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.s1440p));
        combinationList.add(surfaceCombination2);

        // (PRIV, s1440p) + (JPEG, MAXIMUM)
        SurfaceCombination surfaceCombination3 = new SurfaceCombination();
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.s1440p));
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination3);

        // (YUV, s1440p) + (JPEG, MAXIMUM)
        SurfaceCombination surfaceCombination4 = new SurfaceCombination();
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.s1440p));
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination4);

        // (PRIV, s1440p) + (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination5 = new SurfaceCombination();
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.s1440p));
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination5);

        // (YUV, s1440p) + (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination6 = new SurfaceCombination();
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.s1440p));
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination6);

        // (PRIV, PREVIEW) + (PRIV, s1440)
        SurfaceCombination surfaceCombination7 = new SurfaceCombination();
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.s1440p));
        combinationList.add(surfaceCombination7);

        // (YUV, PREVIEW) + (PRIV, s1440)
        SurfaceCombination surfaceCombination8 = new SurfaceCombination();
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.s1440p));
        combinationList.add(surfaceCombination8);

        // (PRIV, PREVIEW) + (YUV, s1440)
        SurfaceCombination surfaceCombination9 = new SurfaceCombination();
        surfaceCombination9.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination9.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.s1440p));
        combinationList.add(surfaceCombination9);

        // (YUV, PREVIEW) + (YUV, s1440)
        SurfaceCombination surfaceCombination10 = new SurfaceCombination();
        surfaceCombination10.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination10.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.s1440p));
        combinationList.add(surfaceCombination10);

        return combinationList;
    }

    /**
     * Returns the supported stream combinations based on the hardware level and capabilities of
     * the device.
     */
    @NonNull
    public static List<SurfaceCombination> generateSupportedCombinationList(int hardwareLevel,
            boolean isRawSupported, boolean isBurstCaptureSupported) {
        List<SurfaceCombination> surfaceCombinations = new ArrayList<>();
        surfaceCombinations.addAll(getLegacySupportedCombinationList());

        if (hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
                || hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
                || hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3) {
            surfaceCombinations.addAll(getLimitedSupportedCombinationList());
        }

        if (hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
                || hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3) {
            surfaceCombinations.addAll(getFullSupportedCombinationList());
        }

        if (isRawSupported) {
            surfaceCombinations.addAll(getRAWSupportedCombinationList());
        }

        if (isBurstCaptureSupported
                && hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED) {
            surfaceCombinations.addAll(getBurstSupportedCombinationList());
        }

        if (hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3) {
            surfaceCombinations.addAll(getLevel3SupportedCombinationList());
        }
        return surfaceCombinations;
    }
}
