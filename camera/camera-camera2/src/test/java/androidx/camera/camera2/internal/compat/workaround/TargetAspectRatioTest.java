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


import static androidx.camera.core.AspectRatio.RATIO_16_9;
import static androidx.camera.core.AspectRatio.RATIO_4_3;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.ImageOutputConfig;

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
 * Unit test for {@link TargetAspectRatio}.
 */
@RunWith(ParameterizedRobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class TargetAspectRatioTest {

    @ParameterizedRobolectricTestRunner.Parameters
    public static Collection<Object[]> data() {
        final List<Object[]> data = new ArrayList<>();
        data.add(new Object[]{new Config("Samsung", "SM-J710MN", true, RATIO_4_3, RATIO_16_9)});
        data.add(new Object[]{new Config("Samsung", "SM-J710MN", true, RATIO_16_9, RATIO_16_9)});
        data.add(new Object[]{new Config("Samsung", "SM-J710MN", false, RATIO_4_3, RATIO_4_3)});
        data.add(new Object[]{new Config("Samsung", "SM-J710MN", false, RATIO_16_9, RATIO_16_9)});
        data.add(new Object[]{new Config("Samsung", "SM-T580", true, RATIO_4_3, RATIO_16_9)});
        data.add(new Object[]{new Config("Samsung", "SM-T580", true, RATIO_16_9, RATIO_16_9)});
        data.add(new Object[]{new Config("Samsung", "SM-T580", false, RATIO_4_3, RATIO_4_3)});
        data.add(new Object[]{new Config("Samsung", "SM-T580", false, RATIO_16_9, RATIO_16_9)});
        data.add(new Object[]{new Config(null, null, true, RATIO_4_3, RATIO_4_3)});
        data.add(new Object[]{new Config(null, null, true, RATIO_16_9, RATIO_16_9)});
        data.add(new Object[]{new Config(null, null, false, RATIO_4_3, RATIO_4_3)});
        data.add(new Object[]{new Config(null, null, false, RATIO_16_9, RATIO_16_9)});
        return data;
    }

    @NonNull
    private final Config mConfig;

    public TargetAspectRatioTest(@NonNull final Config config) {
        mConfig = config;
    }

    @Test
    public void getCorrectedRatio() {
        // Set up device properties
        if (mConfig.mManufacturer != null) {
            ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", mConfig.mManufacturer);
            ReflectionHelpers.setStaticField(Build.class, "MODEL", mConfig.mModel);
        }

        // Set up use case
        final UseCase usecase;
        if (mConfig.mIsPreview) {
            usecase = new Preview.Builder()
                    .setTargetAspectRatio(mConfig.mInputAspectRatio)
                    .build();
        } else {
            usecase = new ImageAnalysis.Builder()
                    .setTargetAspectRatio(mConfig.mInputAspectRatio)
                    .build();
        }
        final ImageOutputConfig imageOutputConfig = (ImageOutputConfig) usecase.getCurrentConfig();

        final int aspectRatio = new TargetAspectRatio().get(imageOutputConfig);
        assertThat(aspectRatio).isEqualTo(mConfig.mExpectedAspectRatio);
    }

    static class Config {
        @Nullable
        final String mManufacturer;
        @Nullable
        final String mModel;
        final boolean mIsPreview;
        @AspectRatio.Ratio
        final int mInputAspectRatio;
        @AspectRatio.Ratio
        final int mExpectedAspectRatio;

        Config(@Nullable String manufacturer, @Nullable String model, boolean isPreview,
                @AspectRatio.Ratio int inputAspectRatio,
                @AspectRatio.Ratio int expectedAspectRatio) {
            mManufacturer = manufacturer;
            mModel = model;
            mIsPreview = isPreview;
            mInputAspectRatio = inputAspectRatio;
            mExpectedAspectRatio = expectedAspectRatio;
        }
    }
}
