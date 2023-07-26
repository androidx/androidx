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

package androidx.camera.core.internal.compat.workaround;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.ImageFormat;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;
import androidx.camera.testing.impl.fakes.FakeImageInfo;
import androidx.camera.testing.impl.fakes.FakeImageProxy;

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
 * Unit test for {@link ExifRotationAvailability}.
 */
@RunWith(ParameterizedRobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class ExifRotationAvailabilityTest {
    @ParameterizedRobolectricTestRunner.Parameters
    public static Collection<Object[]> data() {
        final List<Object[]> data = new ArrayList<>();
        data.add(new Object[]{new Config("HUAWEI", "SNE-LX1",
                createFakeImage(ImageFormat.YUV_420_888), false, false)});
        data.add(new Object[]{new Config("HUAWEI", "SNE-LX1",
                createFakeImage(ImageFormat.JPEG), false, false)});
        data.add(new Object[]{new Config("HONOR", "STK-LX1",
                createFakeImage(ImageFormat.YUV_420_888), false, false)});
        data.add(new Object[]{new Config("HONOR", "STK-LX1",
                createFakeImage(ImageFormat.JPEG), false, false)});
        data.add(new Object[]{new Config(null, null,
                createFakeImage(ImageFormat.YUV_420_888), false, true)});
        data.add(new Object[]{new Config(null, null,
                createFakeImage(ImageFormat.JPEG), true, true)});

        return data;
    }

    @NonNull
    private final Config mConfig;

    public ExifRotationAvailabilityTest(@NonNull final Config config) {
        mConfig = config;
    }

    @SuppressWarnings("ConfusingArgumentToVarargsMethod")
    @Test
    public void shouldUseExifOrientation() {
        // Set up device properties
        if (mConfig.mBrand != null) {
            ReflectionHelpers.setStaticField(Build.class, "BRAND", mConfig.mBrand);
            ReflectionHelpers.setStaticField(Build.class, "MODEL", mConfig.mModel);
        }

        // Create the ExifRotationAvailability workaround object.
        final ExifRotationAvailability exifRotationAvailability = new ExifRotationAvailability();

        // Checks whether the exif's orientation value should be used and meet the expectation.
        assertThat(exifRotationAvailability.shouldUseExifOrientation(mConfig.mImage)).isEqualTo(
                mConfig.mShouldUseExifOrientation);
    }

    @SuppressWarnings("ConfusingArgumentToVarargsMethod")
    @Test
    public void isRotationOptionSupported() {
        // Set up device properties
        if (mConfig.mBrand != null) {
            ReflectionHelpers.setStaticField(Build.class, "BRAND", mConfig.mBrand);
            ReflectionHelpers.setStaticField(Build.class, "MODEL", mConfig.mModel);
        }

        // Create the ExifRotationAvailability workaround object.
        final ExifRotationAvailability exifRotationAvailability = new ExifRotationAvailability();

        // Checks whether the rotation option is supported and meet the expectation.
        assertThat(exifRotationAvailability.isRotationOptionSupported()).isEqualTo(
                mConfig.mIsRotationOptionSupported);
    }

    private static ImageProxy createFakeImage(int imageFormat) {
        FakeImageProxy fakeImageProxy = new FakeImageProxy(new FakeImageInfo());
        fakeImageProxy.setFormat(imageFormat);

        return fakeImageProxy;
    }

    static class Config {
        @Nullable
        final String mBrand;
        @Nullable
        final String mModel;
        @NonNull
        final ImageProxy mImage;
        final boolean mShouldUseExifOrientation;
        final boolean mIsRotationOptionSupported;

        Config(@Nullable String brand, @Nullable String model, @NonNull ImageProxy image,
                boolean shouldUseExifOrientation, boolean isRotationOptionSupported) {
            mBrand = brand;
            mModel = model;
            mImage = image;
            mShouldUseExifOrientation = shouldUseExifOrientation;
            mIsRotationOptionSupported = isRotationOptionSupported;
        }
    }
}
