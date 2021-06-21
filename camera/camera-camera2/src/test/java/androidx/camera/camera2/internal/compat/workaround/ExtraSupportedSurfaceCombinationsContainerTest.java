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

package androidx.camera.camera2.internal.compat.workaround;


import static com.google.common.truth.Truth.assertThat;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.impl.SurfaceCombination;
import androidx.camera.core.impl.SurfaceConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Unit test for {@link ExtraSupportedSurfaceCombinationsContainer}
 */
@RunWith(ParameterizedRobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class ExtraSupportedSurfaceCombinationsContainerTest {

    @ParameterizedRobolectricTestRunner.Parameters
    public static Collection<Object[]> data() {
        final List<Object[]> data = new ArrayList<>();
        data.add(new Object[]{new Config("Samsung", "heroqltevzw", "0")});
        data.add(new Object[]{new Config("Samsung", "heroqltevzw", "1",
                getExpectedSupportedCombinations())});
        data.add(new Object[]{new Config("Samsung", "heroqltetmo", "0")});
        data.add(new Object[]{new Config("Samsung", "heroqltetmo", "1",
                getExpectedSupportedCombinations())});
        data.add(new Object[]{new Config(null, null, "0")});
        return data;
    }

    @NonNull
    private final Config mConfig;

    public ExtraSupportedSurfaceCombinationsContainerTest(@NonNull final Config config) {
        mConfig = config;

    }

    @Test
    public void checkExtraSupportedSurfaceCombinations() {
        // Set up device properties
        if (mConfig.mBrand != null) {
            ReflectionHelpers.setStaticField(Build.class, "BRAND", mConfig.mBrand);
            ReflectionHelpers.setStaticField(Build.class, "DEVICE", mConfig.mDevice);
        }

        // Initializes ExtraSupportedSurfaceCombinationsContainer instance with camera id
        final ExtraSupportedSurfaceCombinationsContainer
                extraSupportedSurfaceCombinationsContainer =
                new ExtraSupportedSurfaceCombinationsContainer(mConfig.mCameraId);

        // Gets the extra supported surface combinations on the device
        List<SurfaceCombination> extraSurfaceCombinations =
                extraSupportedSurfaceCombinationsContainer.get();

        for (SurfaceCombination expectedSupportedSurfaceCombination :
                mConfig.mExpectedSupportedSurfaceCombinations) {
            boolean isSupported = false;

            // Checks the combination is supported by the list retrieved from the
            // ExtraSupportedSurfaceCombinationsContainer.
            for (SurfaceCombination extraSurfaceCombination : extraSurfaceCombinations) {
                if (extraSurfaceCombination.isSupported(
                        expectedSupportedSurfaceCombination.getSurfaceConfigList())) {
                    isSupported = true;
                    break;
                }
            }

            assertThat(isSupported).isTrue();
        }
    }

    private static SurfaceCombination[] getExpectedSupportedCombinations() {
        // (YUV, ANALYSIS) + (PRIV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination = new SurfaceCombination();
        surfaceCombination.addSurfaceConfig(SurfaceConfig.create(SurfaceConfig.ConfigType.YUV,
                SurfaceConfig.ConfigSize.ANALYSIS));
        surfaceCombination.addSurfaceConfig(SurfaceConfig.create(SurfaceConfig.ConfigType.PRIV,
                SurfaceConfig.ConfigSize.PREVIEW));
        surfaceCombination.addSurfaceConfig(SurfaceConfig.create(SurfaceConfig.ConfigType.YUV,
                SurfaceConfig.ConfigSize.MAXIMUM));
        return new SurfaceCombination[]{surfaceCombination};
    }

    static class Config {
        @Nullable
        final String mBrand;
        @Nullable
        final String mDevice;
        @NonNull
        final String mCameraId;
        @NonNull
        final SurfaceCombination[] mExpectedSupportedSurfaceCombinations;

        Config(@Nullable String brand, @Nullable String device, @NonNull String cameraId,
                @NonNull SurfaceCombination... expectedSupportedSurfaceCombinations) {
            mBrand = brand;
            mDevice = device;
            mCameraId = cameraId;
            mExpectedSupportedSurfaceCombinations = expectedSupportedSurfaceCombinations;
        }
    }
}
