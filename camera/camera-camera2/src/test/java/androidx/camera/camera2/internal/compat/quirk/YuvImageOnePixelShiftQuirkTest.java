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

import static com.google.common.truth.Truth.assertThat;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.impl.Quirks;
import androidx.camera.core.internal.compat.quirk.OnePixelShiftQuirk;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowCameraCharacteristics;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Unit test for {@link YuvImageOnePixelShiftQuirk}.
 */
@RunWith(ParameterizedRobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class YuvImageOnePixelShiftQuirkTest {

    private static final String CAMERA_ID = "0";

    @ParameterizedRobolectricTestRunner.Parameters
    public static Collection<Object[]> data() {
        final List<Object[]> data = new ArrayList<>();
        data.add(new Object[]{new Config(
                "motorola", "MotoG3", true)});
        data.add(new Object[]{new Config(
                "samsung", "SM-G532F", true)});
        data.add(new Object[]{new Config(
                "samsung", "SM-J700F", true)});
        data.add(new Object[]{new Config(
                "motorola", "MotoG100", false)});
        return data;
    }

    @NonNull private final Config mConfig;

    public YuvImageOnePixelShiftQuirkTest(@NonNull Config config) {
        mConfig = config;
    }

    @Test
    public void shouldApplyOnePixelShift() {
        // Arrange.
        if (mConfig.mBrand != null) {
            ReflectionHelpers.setStaticField(Build.class, "BRAND", mConfig.mBrand);
            ReflectionHelpers.setStaticField(Build.class, "MODEL", mConfig.mModel);
        }

        // Act.
        CameraCharacteristics characteristics =
                ShadowCameraCharacteristics.newCameraCharacteristics();
        CameraCharacteristicsCompat characteristicsCompat =
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(characteristics);
        final Quirks quirks = CameraQuirks.get(CAMERA_ID, characteristicsCompat);

        // Assert.
        boolean isSupported = quirks != null && quirks.contains(OnePixelShiftQuirk.class);
        assertThat(isSupported).isEqualTo(mConfig.mIsSupported);
    }

    static class Config {
        @NonNull final String mBrand;
        @NonNull final String mModel;
        final boolean mIsSupported;

        Config(@NonNull String brand, @NonNull String model, boolean isSupported) {
            mBrand = brand;
            mModel = model;
            mIsSupported = isSupported;
        }
    }
}
