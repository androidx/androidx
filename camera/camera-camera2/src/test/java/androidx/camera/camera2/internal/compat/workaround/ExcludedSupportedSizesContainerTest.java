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

import static android.graphics.ImageFormat.JPEG;
import static android.graphics.ImageFormat.PRIVATE;
import static android.graphics.ImageFormat.YUV_420_888;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
 * Unit test for {@link ExcludedSupportedSizesContainer}
 */
@RunWith(ParameterizedRobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class ExcludedSupportedSizesContainerTest {

    private static final Size SIZE_4000_3000 = new Size(4000, 3000);
    private static final Size SIZE_4160_3120 = new Size(4160, 3120);
    private static final Size SIZE_720_720 = new Size(720, 720);
    private static final Size SIZE_400_400 = new Size(400, 400);

    @ParameterizedRobolectricTestRunner.Parameters
    public static Collection<Object[]> data() {
        final List<Object[]> data = new ArrayList<>();
        data.add(new Object[]{
                new Config("OnePlus", "OnePlus6", "0", JPEG, SIZE_4000_3000, SIZE_4160_3120)});
        data.add(new Object[]{new Config("OnePlus", "OnePlus6", "1", JPEG)});
        data.add(new Object[]{new Config("OnePlus", "OnePlus6", "0", PRIVATE)});
        data.add(new Object[]{
                new Config("OnePlus", "OnePlus6T", "0", JPEG, SIZE_4000_3000, SIZE_4160_3120)});
        data.add(new Object[]{new Config("OnePlus", "OnePlus6T", "1", JPEG)});
        data.add(new Object[]{new Config("OnePlus", "OnePlus6T", "0", PRIVATE)});
        data.add(new Object[]{new Config("OnePlus", "OnePlus3", "0", JPEG)});
        data.add(new Object[]{new Config(null, null, "0", JPEG)});
        // Huawei P20 Lite
        data.add(new Object[]{
                new Config("HUAWEI", "HWANE", "0", PRIVATE, SIZE_720_720, SIZE_400_400)});
        data.add(new Object[]{new Config("HUAWEI", "HWANE", "1", PRIVATE)});
        data.add(new Object[]{
                new Config("HUAWEI", "HWANE", "0", YUV_420_888, SIZE_720_720, SIZE_400_400)});
        data.add(new Object[]{new Config("HUAWEI", "HWANE", "1", YUV_420_888)});
        data.add(new Object[]{new Config("HUAWEI", "HWANE", "0", JPEG)});
        return data;
    }

    @NonNull
    private final Config mConfig;

    public ExcludedSupportedSizesContainerTest(@NonNull final Config config) {
        mConfig = config;
    }

    @Test
    public void exclude() {
        // Set up device properties
        if (mConfig.mBrand != null) {
            ReflectionHelpers.setStaticField(Build.class, "BRAND", mConfig.mBrand);
            ReflectionHelpers.setStaticField(Build.class, "DEVICE", mConfig.mDevice);
        }

        // Initialize ExcludedSupportedSizesContainer instance with camera id
        final ExcludedSupportedSizesContainer excludedSupportedSizesContainer =
                new ExcludedSupportedSizesContainer(mConfig.mCameraId);

        // Get sizes to exclude
        final List<Size> excludedSizes = excludedSupportedSizesContainer.get(mConfig.mImageFormat);

        assertThat(excludedSizes).containsExactly((Object[]) mConfig.mExcludedSizes);
    }

    static class Config {
        @Nullable
        final String mBrand;
        @Nullable
        final String mDevice;
        @NonNull
        final String mCameraId;
        final int mImageFormat;
        @NonNull
        final Size[] mExcludedSizes;

        Config(@Nullable String brand, @Nullable String device,
                @NonNull String cameraId, int imageFormat, @NonNull Size... excludedSizes) {
            mBrand = brand;
            mDevice = device;
            mCameraId = cameraId;
            mImageFormat = imageFormat;
            mExcludedSizes = excludedSizes;
        }
    }
}
