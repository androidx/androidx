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

import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.hardware.camera2.CaptureRequest;
import android.os.Build;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunWith(ParameterizedRobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class StillCaptureFlowTest {
    @ParameterizedRobolectricTestRunner.Parameters
    public static Collection<Object[]> data() {
        final List<Object[]> data = new ArrayList<>();
        data.add(new Object[]{"Samsung", "SM-A716B", CONTROL_AE_MODE_ON, true, false});
        data.add(new Object[]{"Samsung", "SM-A716B", CONTROL_AE_MODE_ON_AUTO_FLASH, true, true});
        data.add(new Object[]{"Samsung", "SM-A716B", CONTROL_AE_MODE_ON_ALWAYS_FLASH, true, true});
        data.add(new Object[]{"Samsung", "SM-A716B", CONTROL_AE_MODE_ON_AUTO_FLASH, false, false});

        data.add(new Object[]{"Samsung", "SM-A716U", CONTROL_AE_MODE_ON, true, false});
        data.add(new Object[]{"Samsung", "SM-A716U", CONTROL_AE_MODE_ON_AUTO_FLASH, true, true});
        data.add(new Object[]{"Samsung", "SM-A716U", CONTROL_AE_MODE_ON_ALWAYS_FLASH, true, true});
        data.add(new Object[]{"Samsung", "SM-A716U", CONTROL_AE_MODE_ON_AUTO_FLASH, false, false});

        data.add(new Object[]{"Google", "Pixel 2", CONTROL_AE_MODE_ON_AUTO_FLASH, true, false});
        data.add(new Object[]{"Moto", "G3", CONTROL_AE_MODE_ON_AUTO_FLASH, true, false});
        data.add(new Object[]{"Samsung", "SM-A722", CONTROL_AE_MODE_ON_AUTO_FLASH, true, false});

        return data;
    }

    private final String mBrand;
    private final String mModel;
    private final int mAeMode;
    private final boolean mIsStillCapture;
    private final boolean mExpectedShouldStopRepeating;
    public StillCaptureFlowTest(
            String brand,
            String model,
            int aeMode,
            boolean isStillCapture,
            boolean expectedShouldStopRepeating) {
        mBrand = brand;
        mModel = model;
        mAeMode = aeMode;
        mIsStillCapture = isStillCapture;
        mExpectedShouldStopRepeating = expectedShouldStopRepeating;
    }

    @Test
    public void shouldStopRepeating() {
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", mBrand);
        ReflectionHelpers.setStaticField(Build.class, "MODEL", mModel);

        StillCaptureFlow stillCaptureFlow = new StillCaptureFlow();
        CaptureRequest captureRequest = mock(CaptureRequest.class);
        when(captureRequest.get(CaptureRequest.CONTROL_AE_MODE)).thenReturn(mAeMode);

        assertThat(stillCaptureFlow.shouldStopRepeatingBeforeCapture(
                Arrays.asList(captureRequest), mIsStillCapture))
                .isEqualTo(mExpectedShouldStopRepeating);
    }
}
