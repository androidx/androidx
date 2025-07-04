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


import static android.graphics.ImageFormat.JPEG;
import static android.graphics.ImageFormat.JPEG_R;

import static androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_1080P;
import static androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_480P;
import static androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_720P;
import static androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.Rect;
import android.os.Build;
import android.util.Range;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public void canRetrieveSupportedOutputFormats() {
        mFakeCameraInfo.setSupportedResolutions(JPEG, new ArrayList<>());
        mFakeCameraInfo.setSupportedResolutions(JPEG_R, new ArrayList<>());

        Set<Integer> formats = new HashSet<>();
        formats.add(JPEG);
        formats.add(JPEG_R);
        assertThat(mFakeCameraInfo.getSupportedOutputFormats()).containsExactlyElementsIn(formats);
    }

    @Test
    public void canRetrieveSupportedFpsRanges() {
        assertThat(mFakeCameraInfo.getSupportedFrameRateRanges()).isNotEmpty();

    }

    @Test
    public void canRetrieveSupportedDynamicRanges() {
        assertThat(mFakeCameraInfo.getSupportedDynamicRanges()).isNotEmpty();
    }

    @Test
    public void canRetrieveIsHighSpeedSupported() {
        assertThat(mFakeCameraInfo.isHighSpeedSupported()).isFalse();

        mFakeCameraInfo.setHighSpeedSupported(true);

        assertThat(mFakeCameraInfo.isHighSpeedSupported()).isTrue();
    }

    @Test
    public void canRetrieveSupportedHighSpeedSizesAndRanges() {
        List<Size> sizes120p = Arrays.asList(RESOLUTION_1080P, RESOLUTION_480P);
        List<Size> sizes240fps = Arrays.asList(RESOLUTION_720P, RESOLUTION_480P);
        Range<Integer> fps120 = Range.create(120, 120);
        Range<Integer> fps240 = Range.create(240, 240);
        mFakeCameraInfo.setSupportedHighSpeedResolutions(fps120, sizes120p);
        mFakeCameraInfo.setSupportedHighSpeedResolutions(fps240, sizes240fps);

        // getSupportedHighSpeedResolutions
        assertThat(mFakeCameraInfo.getSupportedHighSpeedResolutions())
                .containsExactly(RESOLUTION_1080P, RESOLUTION_720P, RESOLUTION_480P);

        // getSupportedHighSpeedResolutionsFor
        assertThat(mFakeCameraInfo.getSupportedHighSpeedResolutionsFor(fps120))
                .containsExactlyElementsIn(sizes120p);
        assertThat(mFakeCameraInfo.getSupportedHighSpeedResolutionsFor(fps240))
                .containsExactlyElementsIn(sizes240fps);
        assertThat(mFakeCameraInfo.getSupportedHighSpeedResolutionsFor(Range.create(1, 1)))
                .isEmpty();

        // getSupportedHighSpeedFrameRateRanges
        assertThat(mFakeCameraInfo.getSupportedHighSpeedFrameRateRanges())
                .containsExactly(fps120, fps240);

        // getSupportedHighSpeedFrameRateRangesFor
        assertThat(mFakeCameraInfo.getSupportedHighSpeedFrameRateRangesFor(new Size(1, 1)))
                .isEmpty();
        assertThat(mFakeCameraInfo.getSupportedHighSpeedFrameRateRangesFor(RESOLUTION_480P))
                .containsExactly(fps120, fps240);
        assertThat(mFakeCameraInfo.getSupportedHighSpeedFrameRateRangesFor(RESOLUTION_720P))
                .containsExactly(fps240);
        assertThat(mFakeCameraInfo.getSupportedHighSpeedFrameRateRangesFor(RESOLUTION_1080P))
                .containsExactly(fps120);
    }

    @Test
    public void providesSensorRectAccordingToMaxOutputSize() {
        assertThat(mFakeCameraInfo.getSensorRect()).isEqualTo(
                new Rect(0, 0, MAX_OUTPUT_SIZE.getWidth(), MAX_OUTPUT_SIZE.getHeight()));
    }
}
