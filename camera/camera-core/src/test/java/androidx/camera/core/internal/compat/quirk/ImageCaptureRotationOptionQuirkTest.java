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

package androidx.camera.core.internal.compat.quirk;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config.Option;

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
 * Unit test for {@link ImageCaptureRotationOptionQuirk}.
 */
@RunWith(ParameterizedRobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class ImageCaptureRotationOptionQuirkTest {
    @ParameterizedRobolectricTestRunner.Parameters
    public static Collection<Object[]> data() {
        final List<Object[]> data = new ArrayList<>();
        data.add(new Object[]{new Config("HUAWEI", "SNE-LX1", CaptureConfig.OPTION_ROTATION,
                false)});
        data.add(new Object[]{new Config("HUAWEI", "SNE-LX1", CaptureConfig.OPTION_JPEG_QUALITY,
                true)});
        data.add(new Object[]{new Config("HONOR", "STK-LX1", CaptureConfig.OPTION_ROTATION,
                false)});
        data.add(new Object[]{new Config("HONOR", "STK-LX1", CaptureConfig.OPTION_JPEG_QUALITY,
                true)});
        data.add(new Object[]{new Config(null, null, CaptureConfig.OPTION_ROTATION, true)});
        data.add(new Object[]{new Config(null, null, CaptureConfig.OPTION_JPEG_QUALITY, true)});

        return data;
    }

    @NonNull
    private final Config mConfig;

    public ImageCaptureRotationOptionQuirkTest(@NonNull final Config config) {
        mConfig = config;
    }

    @SuppressWarnings("ConfusingArgumentToVarargsMethod")
    @Test
    public void isCaptureConfigOptionSupported() {
        // Set up device properties
        if (mConfig.mBrand != null) {
            ReflectionHelpers.setStaticField(Build.class, "BRAND", mConfig.mBrand);
            ReflectionHelpers.setStaticField(Build.class, "MODEL", mConfig.mModel);
        }

        // Retrieve the ImageCaptureRotationOptionQuirk workaround object.
        final ImageCaptureRotationOptionQuirk quirk =
                DeviceQuirks.get(ImageCaptureRotationOptionQuirk.class);

        // Checks whether the option can be supported and meet the expectation.
        boolean isSupported = quirk != null ? quirk.isSupported(mConfig.mOption) : true;
        assertThat(isSupported).isEqualTo(mConfig.mIsSupported);
    }

    static class Config {
        @Nullable
        final String mBrand;
        @Nullable
        final String mModel;
        @NonNull
        final Option<?> mOption;
        final boolean mIsSupported;

        Config(@Nullable String brand, @Nullable String model, @NonNull Option<?> option,
                boolean isSupported) {
            mBrand = brand;
            mModel = model;
            mOption = option;
            mIsSupported = isSupported;
        }
    }
}
