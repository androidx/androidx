/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat.workaround;


import static android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
import static android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED;

import static androidx.camera.camera2.internal.compat.workaround.TargetAspectRatio.RATIO_MAX_JPEG;
import static androidx.camera.camera2.internal.compat.workaround.TargetAspectRatio.RATIO_ORIGINAL;

import static com.google.common.truth.Truth.assertThat;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.util.Range;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Unit test for {@link TargetAspectRatio}.
 */
@RunWith(ParameterizedRobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class TargetAspectRatioTest {
    private static final String BACK_CAMERA_ID = "0";
    private static final Range<Integer> ALL_API_LEVELS = new Range<>(0, Integer.MAX_VALUE);

    @ParameterizedRobolectricTestRunner.Parameters
    public static Collection<Object[]> data() {
        final List<Object[]> data = new ArrayList<>();
        data.add(new Object[]{new Config("Google", "Nexus 4",
                INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, RATIO_MAX_JPEG,
                new Range<>(21, 22))});
        data.add(new Object[]{new Config("Google", "Nexus 4",
                INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, RATIO_MAX_JPEG,
                new Range<>(21, 22))});
        data.add(new Object[]{new Config("Google", "Nexus 4",
                INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, RATIO_MAX_JPEG,
                new Range<>(21, 22))});
        data.add(new Object[]{new Config("Google", "Nexus 4",
                INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, RATIO_MAX_JPEG,
                new Range<>(21, 22))});

        data.add(new Object[]{new Config(null, null,
                INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED, RATIO_ORIGINAL, ALL_API_LEVELS)});
        data.add(new Object[]{new Config(null, null,
                INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED, RATIO_ORIGINAL,
                ALL_API_LEVELS)});
        data.add(new Object[]{new Config(null, null,
                INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED, RATIO_ORIGINAL, ALL_API_LEVELS)});
        data.add(new Object[]{new Config(null, null,
                INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED, RATIO_ORIGINAL,
                ALL_API_LEVELS)});

        // Test the legacy camera/Android 5.0 quirk.
        data.add(new Object[]{new Config(null, null, INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                 RATIO_MAX_JPEG, new Range<>(21, 21))});
        data.add(new Object[]{new Config(null, null, INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                RATIO_MAX_JPEG, new Range<>(21, 21))});
        data.add(new Object[]{new Config(null, null, INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                RATIO_MAX_JPEG, new Range<>(21, 21))});
        data.add(new Object[]{new Config(null, null, INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                RATIO_MAX_JPEG, new Range<>(21, 21))});
        return data;
    }

    @NonNull
    private final Config mConfig;

    public TargetAspectRatioTest(@NonNull final Config config) {
        mConfig = config;
    }

    @SuppressWarnings("deprecation") // legacy resolution API
    @Test
    public void getCorrectedRatio() {
        // Set up device properties
        if (mConfig.mBrand != null) {
            ReflectionHelpers.setStaticField(Build.class, "BRAND", mConfig.mBrand);
            ReflectionHelpers.setStaticField(Build.class, "MODEL", mConfig.mModel);
        }

        final int aspectRatio = new TargetAspectRatio().get(
                BACK_CAMERA_ID, getCharacteristicsCompat(mConfig.mHardwareLevel));
        assertThat(aspectRatio).isEqualTo(getExpectedAspectRatio());
    }

    @NonNull
    private CameraCharacteristicsCompat getCharacteristicsCompat(int supportedHardwareLevel) {
        CameraCharacteristics characteristics =
                ShadowCameraCharacteristics.newCameraCharacteristics();

        ShadowCameraCharacteristics shadowCharacteristics = Shadow.extract(characteristics);
        shadowCharacteristics.set(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                supportedHardwareLevel);
        shadowCharacteristics.set(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP,
                Mockito.mock(StreamConfigurationMap.class));
        return CameraCharacteristicsCompat.toCameraCharacteristicsCompat(characteristics,
                BACK_CAMERA_ID);
    }

    @TargetAspectRatio.Ratio
    private int getExpectedAspectRatio() {
        return mConfig.mAffectedApiLevels.contains(Build.VERSION.SDK_INT)
                ? mConfig.mExpectedAspectRatio : RATIO_ORIGINAL;
    }

    static class Config {
        @Nullable
        final String mBrand;
        @Nullable
        final String mModel;
        @TargetAspectRatio.Ratio
        final int mExpectedAspectRatio;
        final int mHardwareLevel;
        final Range<Integer> mAffectedApiLevels;

        Config(@Nullable String brand, @Nullable String model, int hardwareLevel,
                @TargetAspectRatio.Ratio int expectedAspectRatio,
                @NonNull Range<Integer> affectedApiLevels) {
            mBrand = brand;
            mModel = model;
            mHardwareLevel = hardwareLevel;
            mExpectedAspectRatio = expectedAspectRatio;
            mAffectedApiLevels = affectedApiLevels;
        }
    }
}
