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
import android.hardware.camera2.CameraDevice;
import android.os.Build;
import android.util.Size;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.core.impl.StreamUseCase;
import androidx.camera.core.impl.SurfaceCombination;
import androidx.camera.core.impl.SurfaceConfig;
import androidx.camera.core.impl.SurfaceConfig.ConfigSize;
import androidx.camera.core.impl.SurfaceConfig.ConfigType;
import androidx.camera.core.impl.SurfaceSizeDefinition;

import org.jspecify.annotations.NonNull;

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
public final class GuaranteedConfigurationsUtil {

    private GuaranteedConfigurationsUtil() {
    }

    /**
     * Returns the at least supported stream combinations for legacy devices.
     */
    public static @NonNull List<SurfaceCombination> getLegacySupportedCombinationList() {
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
    public static @NonNull List<SurfaceCombination> getLimitedSupportedCombinationList() {
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
    public static @NonNull List<SurfaceCombination> getFullSupportedCombinationList() {
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
    public static @NonNull List<SurfaceCombination> getRAWSupportedCombinationList() {
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
    public static @NonNull List<SurfaceCombination> getBurstSupportedCombinationList() {
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
    public static @NonNull List<SurfaceCombination> getLevel3SupportedCombinationList() {
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
    public static @NonNull List<SurfaceCombination>
            getUltraHighResolutionSupportedCombinationList() {
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
    public static @NonNull List<SurfaceCombination> get10BitSupportedCombinationList() {
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
     * Returns the minimally guaranteed stream combinations for Ultra HDR.
     */
    public static @NonNull List<SurfaceCombination> getUltraHdrSupportedCombinationList() {
        // Due to the unique characteristics of JPEG/R, some devices might configure an extra 8-bit
        // JPEG stream internally in addition to the 10-bit YUV stream. The 10-bit mandatory
        // stream combination table is actually not suitable for use. Adds only (PRIV, PREVIEW) +
        // (JPEG_R, MAXIMUM), which is guaranteed by CTS test, as the supported combination.

        List<SurfaceCombination> combinationList = new ArrayList<>();

        // (JPEG_R, MAXIMUM)
        SurfaceCombination surfaceCombination1 = new SurfaceCombination();
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG_R, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination1);

        // (PRIV, PREVIEW) + (JPEG_R, MAXIMUM)
        SurfaceCombination surfaceCombination2 = new SurfaceCombination();
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG_R, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination2);

        return combinationList;
    }

    /**
     * Returns the at least supported stream combinations for concurrent cameras.
     */
    public static @NonNull List<SurfaceCombination> getConcurrentSupportedCombinationList() {
        List<SurfaceCombination> combinationList = new ArrayList<>();

        // (YUV, s1440p)
        SurfaceCombination surfaceCombination1 = new SurfaceCombination();
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.S1440P_4_3));
        combinationList.add(surfaceCombination1);

        // (PRIV, s1440p)
        SurfaceCombination surfaceCombination2 = new SurfaceCombination();
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.S1440P_4_3));
        combinationList.add(surfaceCombination2);

        // (JPEG, s1440p)
        SurfaceCombination surfaceCombination3 = new SurfaceCombination();
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.S1440P_4_3));
        combinationList.add(surfaceCombination3);

        // (YUV, s720p) + (JPEG, s1440p)
        SurfaceCombination surfaceCombination4 = new SurfaceCombination();
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.S720P_16_9));
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.S1440P_4_3));
        combinationList.add(surfaceCombination4);

        // (PRIV, s720p) + (JPEG, s1440p)
        SurfaceCombination surfaceCombination5 = new SurfaceCombination();
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.S720P_16_9));
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.S1440P_4_3));
        combinationList.add(surfaceCombination5);

        // (YUV, s720p) + (YUV, s1440p)
        SurfaceCombination surfaceCombination6 = new SurfaceCombination();
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.S720P_16_9));
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.S1440P_4_3));
        combinationList.add(surfaceCombination6);

        // (YUV, s720p) + (PRIV, s1440p)
        SurfaceCombination surfaceCombination7 = new SurfaceCombination();
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.S720P_16_9));
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.S1440P_4_3));
        combinationList.add(surfaceCombination7);

        // (PRIV, s720p) + (YUV, s1440p)
        SurfaceCombination surfaceCombination8 = new SurfaceCombination();
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.S720P_16_9));
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.S1440P_4_3));
        combinationList.add(surfaceCombination8);

        // (PRIV, s720p) + (PRIV, s1440p)
        SurfaceCombination surfaceCombination9 = new SurfaceCombination();
        surfaceCombination9.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.S720P_16_9));
        surfaceCombination9.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.S1440P_4_3));
        combinationList.add(surfaceCombination9);

        return combinationList;
    }

    /**
     * Returns the entire supported stream combinations for devices with Stream Use Case capability
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public static @NonNull List<SurfaceCombination> getStreamUseCaseSupportedCombinationList() {
        List<SurfaceCombination> combinationList = new ArrayList<>();

        // (PRIV, s1440p, PREVIEW_VIDEO_STILL)
        SurfaceCombination surfaceCombination1 = new SurfaceCombination();
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.S1440P_4_3,
                        StreamUseCase.PREVIEW_VIDEO_STILL));
        combinationList.add(surfaceCombination1);

        // (YUV, s1440p, PREVIEW_VIDEO_STILL)
        SurfaceCombination surfaceCombination2 = new SurfaceCombination();
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.S1440P_4_3,
                        StreamUseCase.PREVIEW_VIDEO_STILL));
        combinationList.add(surfaceCombination2);

        // (PRIV, RECORD, VIDEO_RECORD)
        SurfaceCombination surfaceCombination3 = new SurfaceCombination();
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD,
                        StreamUseCase.VIDEO_RECORD));
        combinationList.add(surfaceCombination3);

        // (YUV, RECORD, VIDEO_RECORD)
        SurfaceCombination surfaceCombination4 = new SurfaceCombination();
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.RECORD,
                        StreamUseCase.VIDEO_RECORD));
        combinationList.add(surfaceCombination4);

        // (JPEG, MAXIMUM, STILL_CAPTURE)
        SurfaceCombination surfaceCombination5 = new SurfaceCombination();
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM,
                        StreamUseCase.STILL_CAPTURE));
        combinationList.add(surfaceCombination5);

        // (YUV, MAXIMUM, STILL_CAPTURE)
        SurfaceCombination surfaceCombination6 = new SurfaceCombination();
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM,
                        StreamUseCase.STILL_CAPTURE));
        combinationList.add(surfaceCombination6);

        // (PRIV, PREVIEW, PREVIEW) + (JPEG, MAXIMUM, STILL_CAPTURE)
        SurfaceCombination surfaceCombination7 = new SurfaceCombination();
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW,
                        StreamUseCase.PREVIEW));
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM,
                        StreamUseCase.STILL_CAPTURE));
        combinationList.add(surfaceCombination7);

        // (PRIV, PREVIEW, PREVIEW) + (YUV, MAXIMUM, STILL_CAPTURE)
        SurfaceCombination surfaceCombination8 = new SurfaceCombination();
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW,
                        StreamUseCase.PREVIEW));
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM,
                        StreamUseCase.STILL_CAPTURE));
        combinationList.add(surfaceCombination8);

        // (PRIV, PREVIEW, PREVIEW) + (PRIV, RECORD, VIDEO_RECORD)
        SurfaceCombination surfaceCombination9 = new SurfaceCombination();
        surfaceCombination9.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW,
                        StreamUseCase.PREVIEW));
        surfaceCombination9.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD,
                        StreamUseCase.VIDEO_RECORD));
        combinationList.add(surfaceCombination9);

        // (PRIV, PREVIEW, PREVIEW) + (YUV, RECORD, VIDEO_RECORD)
        SurfaceCombination surfaceCombination10 = new SurfaceCombination();
        surfaceCombination10.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW,
                        StreamUseCase.PREVIEW));
        surfaceCombination10.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.RECORD,
                        StreamUseCase.VIDEO_RECORD));
        combinationList.add(surfaceCombination10);

        // (PRIV, PREVIEW, PREVIEW) + (YUV, PREVIEW, PREVIEW)
        SurfaceCombination surfaceCombination11 = new SurfaceCombination();
        surfaceCombination11.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW,
                        StreamUseCase.PREVIEW));
        surfaceCombination11.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW,
                        StreamUseCase.PREVIEW));
        combinationList.add(surfaceCombination11);

        // (PRIV, PREVIEW, PREVIEW) + (PRIV, RECORD, VIDEO_RECORD) + (JPEG, RECORD, STILL_CAPTURE)
        SurfaceCombination surfaceCombination12 = new SurfaceCombination();
        surfaceCombination12.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW,
                        StreamUseCase.PREVIEW));
        surfaceCombination12.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD,
                        StreamUseCase.VIDEO_RECORD));
        surfaceCombination12.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.RECORD,
                        StreamUseCase.STILL_CAPTURE));
        combinationList.add(surfaceCombination12);

        // (PRIV, PREVIEW, PREVIEW) + (YUV, RECORD, VIDEO_RECORD) + (JPEG, RECORD, STILL_CAPTURE)
        SurfaceCombination surfaceCombination13 = new SurfaceCombination();
        surfaceCombination13.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW,
                        StreamUseCase.PREVIEW));
        surfaceCombination13.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.RECORD,
                        StreamUseCase.VIDEO_RECORD));
        surfaceCombination13.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.RECORD,
                        StreamUseCase.STILL_CAPTURE));
        combinationList.add(surfaceCombination13);

        // (PRIV, PREVIEW, PREVIEW) + (YUV, PREVIEW, PREVIEW) + (JPEG, MAXIMUM, STILL_CAPTURE)
        SurfaceCombination surfaceCombination14 = new SurfaceCombination();
        surfaceCombination14.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW,
                        StreamUseCase.PREVIEW));
        surfaceCombination14.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW,
                        StreamUseCase.PREVIEW));
        surfaceCombination14.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM,
                        StreamUseCase.STILL_CAPTURE));
        combinationList.add(surfaceCombination14);

        return combinationList;
    }

    /**
     * Returns the supported stream combinations for preview stabilization.
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public static @NonNull List<SurfaceCombination>
            getPreviewStabilizationSupportedCombinationList() {
        List<SurfaceCombination> combinationList = new ArrayList<>();

        // (PRIV, s1440p)
        SurfaceCombination surfaceCombination1 = new SurfaceCombination();
        surfaceCombination1.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.S1440P_4_3));
        combinationList.add(surfaceCombination1);

        // (YUV, s1440p)
        SurfaceCombination surfaceCombination2 = new SurfaceCombination();
        surfaceCombination2.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.S1440P_4_3));
        combinationList.add(surfaceCombination2);

        // (PRIV, s1440p) + (JPEG, MAXIMUM)
        SurfaceCombination surfaceCombination3 = new SurfaceCombination();
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.S1440P_4_3));
        surfaceCombination3.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination3);

        // (YUV, s1440p) + (JPEG, MAXIMUM)
        SurfaceCombination surfaceCombination4 = new SurfaceCombination();
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.S1440P_4_3));
        surfaceCombination4.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination4);

        // (PRIV, s1440p) + (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination5 = new SurfaceCombination();
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.S1440P_4_3));
        surfaceCombination5.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination5);

        // (YUV, s1440p) + (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination6 = new SurfaceCombination();
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.S1440P_4_3));
        surfaceCombination6.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM));
        combinationList.add(surfaceCombination6);

        // (PRIV, PREVIEW) + (PRIV, s1440)
        SurfaceCombination surfaceCombination7 = new SurfaceCombination();
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination7.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.S1440P_4_3));
        combinationList.add(surfaceCombination7);

        // (YUV, PREVIEW) + (PRIV, s1440)
        SurfaceCombination surfaceCombination8 = new SurfaceCombination();
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination8.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.S1440P_4_3));
        combinationList.add(surfaceCombination8);

        // (PRIV, PREVIEW) + (YUV, s1440)
        SurfaceCombination surfaceCombination9 = new SurfaceCombination();
        surfaceCombination9.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW));
        surfaceCombination9.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.S1440P_4_3));
        combinationList.add(surfaceCombination9);

        // (YUV, PREVIEW) + (YUV, s1440)
        SurfaceCombination surfaceCombination10 = new SurfaceCombination();
        surfaceCombination10.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW));
        surfaceCombination10.addSurfaceConfig(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.S1440P_4_3));
        combinationList.add(surfaceCombination10);

        return combinationList;
    }

    /**
     * Returns the supported stream combinations based on the hardware level and capabilities of
     * the device.
     */
    public static @NonNull List<SurfaceCombination> generateSupportedCombinationList(
            int hardwareLevel, boolean isRawSupported, boolean isBurstCaptureSupported) {
        List<SurfaceCombination> surfaceCombinations = new ArrayList<>();
        surfaceCombinations.addAll(getLegacySupportedCombinationList());

        if (hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
                || hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL
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

    /**
     * Returns the supported stream combinations for high-speed sessions.
     */
    public static @NonNull List<SurfaceCombination> generateHighSpeedSupportedCombinationList(
            @NonNull Size maxSupportedSize,
            @NonNull SurfaceSizeDefinition surfaceSizeDefinition) {
        List<SurfaceCombination> surfaceCombinations = new ArrayList<>();

        // Find the closest SurfaceConfig that can contain the max supported size. Ultimately,
        // the target resolution still needs to be verified by the StreamConfigurationMap API for
        // high-speed.
        SurfaceConfig surfaceConfig = SurfaceConfig.transformSurfaceConfig(
                ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                maxSupportedSize,
                surfaceSizeDefinition);

        // Create high-speed supported combinations based on the constraints:
        // - Only support preview and/or video surface.
        // - Maximum 2 surfaces.
        // - All surfaces must have the same size.

        // PRIV
        SurfaceCombination surfaceCombination = new SurfaceCombination();
        surfaceCombination.addSurfaceConfig(surfaceConfig);
        surfaceCombinations.add(surfaceCombination);

        // PRIV + PRIV
        surfaceCombination = new SurfaceCombination();
        surfaceCombination.addSurfaceConfig(surfaceConfig);
        surfaceCombination.addSurfaceConfig(surfaceConfig);
        surfaceCombinations.add(surfaceCombination);

        return surfaceCombinations;
    }

    /**
     * Generates the list of {@link SurfaceCombination} that are guaranteed to be queryable with
     * feature combination query APIs.
     *
     * <p> Note that these stream combinations are not guaranteed to be always supported, but rather
     * guaranteed to provide a valid result via feature combination query (i.e.
     * {@link CameraDevice.CameraDeviceSetup#isSessionConfigurationSupported} API).
     *
     * <p> These combinations are generated based on the documentation of
     * {@link CameraCharacteristics#INFO_SESSION_CONFIGURATION_QUERY_VERSION}.
     */
    public static @NonNull List<@NonNull SurfaceCombination> generateQueryableFcqCombinations() {
        List<SurfaceCombination> combinations = new ArrayList<>();

        // (PRIV, S1080P)
        combinations.add(new SurfaceCombination(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.S1080P_16_9)));

        // (PRIV, S720P)
        combinations.add(new SurfaceCombination(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.S720P_16_9)));

        // (PRIV, S1080P) + (JPEG/JPEG_R, MAX_16_9)
        combinations.addAll(
                createPrivJpegXCombinations(
                        /* privSize = */ ConfigSize.S1080P_16_9,
                        /* jpegXSize = */ ConfigSize.MAXIMUM_16_9
                )
        );

        // (PRIV, S1080P) + (JPEG/JPEG_R, UHD)
        combinations.addAll(
                createPrivJpegXCombinations(
                        /* privSize = */ ConfigSize.S1080P_16_9,
                        /* jpegXSize = */ ConfigSize.UHD
                )
        );

        // (PRIV, S1080P) + (JPEG/JPEG_R, S1440P)
        combinations.addAll(
                createPrivJpegXCombinations(
                        /* privSize = */ ConfigSize.S1080P_16_9,
                        /* jpegXSize = */ ConfigSize.S1440P_16_9
                )
        );

        // (PRIV, S1080P) + (JPEG/JPEG_R, S1080P)
        combinations.addAll(
                createPrivJpegXCombinations(
                        /* privSize = */ ConfigSize.S1080P_16_9,
                        /* jpegXSize = */ ConfigSize.S1080P_16_9
                )
        );

        // (PRIV, S720P) + (JPEG/JPEG_R, MAX_16_9)
        combinations.addAll(
                createPrivJpegXCombinations(
                        /* privSize = */ ConfigSize.S720P_16_9,
                        /* jpegXSize = */ ConfigSize.MAXIMUM_16_9
                )
        );

        // (PRIV, S720P) + (JPEG/JPEG_R, UHD)
        combinations.addAll(
                createPrivJpegXCombinations(
                        /* privSize = */ ConfigSize.S720P_16_9,
                        /* jpegXSize = */ ConfigSize.UHD
                )
        );

        // (PRIV, S720P) + (JPEG/JPEG_R, S1080P)
        combinations.addAll(
                createPrivJpegXCombinations(
                        /* privSize = */ ConfigSize.S720P_16_9,
                        /* jpegXSize = */ ConfigSize.S1080P_16_9
                )
        );

        // (PRIV, XVGA) + (JPEG/JPEG_R, MAX_4_3)
        combinations.addAll(
                createPrivJpegXCombinations(
                        /* privSize = */ ConfigSize.X_VGA,
                        /* jpegXSize = */ ConfigSize.MAXIMUM_4_3
                )
        );

        // (PRIV, S1080P_4_3) + (JPEG/JPEG_R, MAX_4_3)
        combinations.addAll(
                createPrivJpegXCombinations(
                        /* privSize = */ ConfigSize.S1080P_4_3,
                        /* jpegXSize = */ ConfigSize.MAXIMUM_4_3
                )
        );

        // TODO: Add the combinations for Android 16

        return combinations;
    }

    /**
     * Creates a list of [SurfaceCombination] based on the input PRIV size and JPEG_X (i.e. JPEG
     * and JPEG_R) size.
     */
    private static @NonNull List<@NonNull SurfaceCombination> createPrivJpegXCombinations(
            ConfigSize privSize,
            ConfigSize jpegXSize
    ) {
        List<SurfaceCombination> combinationList = new ArrayList<>();

        combinationList.add(new SurfaceCombination(
                SurfaceConfig.create(ConfigType.PRIV, privSize),
                SurfaceConfig.create(ConfigType.JPEG, jpegXSize)));

        combinationList.add(new SurfaceCombination(
                SurfaceConfig.create(ConfigType.PRIV, privSize),
                SurfaceConfig.create(ConfigType.JPEG_R, jpegXSize)));

        return combinationList;
    }
}
