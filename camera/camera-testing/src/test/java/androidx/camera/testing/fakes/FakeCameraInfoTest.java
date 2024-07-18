/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.testing.fakes;


import static com.google.common.truth.Truth.assertThat;

import android.os.Build;
import android.util.Size;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.impl.ImageFormatConstants;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public final class FakeCameraInfoTest {

    private static final int SENSOR_ROTATION_DEGREES = 90;
    @CameraSelector.LensFacing
    private static final int LENS_FACING = CameraSelector.LENS_FACING_FRONT;

    private FakeCameraInfoInternal mFakeCameraInfo;

    @Before
    public void setUp() {
        mFakeCameraInfo = new FakeCameraInfoInternal(SENSOR_ROTATION_DEGREES, LENS_FACING);
    }

    @Test
    public void canRetrieveLensFacingDirection() {
        assertThat(mFakeCameraInfo.getLensFacing()).isSameInstanceAs(LENS_FACING);
    }

    @Test
    public void canRetrieveSensorRotation() {
        assertThat(mFakeCameraInfo.getSensorRotationDegrees()).isEqualTo(SENSOR_ROTATION_DEGREES);
    }

    @Test
    public void canRetrieveSupportedResolutions() {
        List<Size> resolutions = new ArrayList<>();
        resolutions.add(new Size(1280, 720));
        resolutions.add(new Size(640, 480));
        mFakeCameraInfo.setSupportedResolutions(
                ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE, resolutions);

        assertThat(mFakeCameraInfo.getSupportedResolutions(
                ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE))
                .containsExactlyElementsIn(resolutions);
    }

    @Test
    public void canRetrieveSupportedFpsRanges() {
        assertThat(mFakeCameraInfo.getSupportedFrameRateRanges()).isNotEmpty();

    }

    @Test
    public void canRetrieveSupportedDynamicRanges() {
        assertThat(mFakeCameraInfo.getSupportedDynamicRanges()).isNotEmpty();
    }
}
