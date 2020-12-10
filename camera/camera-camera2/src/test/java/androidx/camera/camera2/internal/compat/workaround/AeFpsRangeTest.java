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


import static com.google.common.truth.Truth.assertThat;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.util.Range;

import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.quirk.CameraQuirks;
import androidx.camera.core.impl.Quirks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class AeFpsRangeTest {

    private static final String ANY_CAMERA_ID = "0";

    @SuppressWarnings("unchecked")
    @Test
    public void validEntryExists_correctRangeIsSelected() {
        Range<Integer>[] availableFpsRanges = new Range[]{
                new Range<>(25, 30),
                new Range<>(7, 33),
                new Range<>(15, 30),
                new Range<>(11, 22),
                new Range<>(30, 30),
        };

        AeFpsRange aeFpsRange =
                createAeFpsRange(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                        availableFpsRanges);

        Range<Integer> pick = getAeFpsRange(aeFpsRange);
        assertThat(pick).isEqualTo(new Range<>(15, 30));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void noValidEntry_doesNotSetFpsRange() {
        Range<Integer>[] availableFpsRanges = new Range[]{
                new Range<>(25, 25),
                new Range<>(7, 33),
                new Range<>(15, 24),
                new Range<>(11, 22),
        };

        AeFpsRange aeFpsRange =
                createAeFpsRange(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                        availableFpsRanges);
        Range<Integer> pick = getAeFpsRange(aeFpsRange);
        assertThat(pick).isNull();
    }

    @Test
    public void availableArrayIsNull_doesNotSetFpsRange() {
        AeFpsRange aeFpsRange =
                createAeFpsRange(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                        null);
        Range<Integer> pick = getAeFpsRange(aeFpsRange);
        assertThat(pick).isNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void limitedDevices_doesNotSetFpsRange() {
        Range<Integer>[] availableFpsRanges = new Range[]{
                new Range<>(15, 30),
        };

        AeFpsRange aeFpsRange =
                createAeFpsRange(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                        availableFpsRanges);

        Range<Integer> pick = getAeFpsRange(aeFpsRange);
        assertThat(pick).isNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void fullDevices_doesNotSetFpsRange() {
        Range<Integer>[] availableFpsRanges = new Range[]{
                new Range<>(15, 30),
        };

        AeFpsRange aeFpsRange =
                createAeFpsRange(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                        availableFpsRanges);

        Range<Integer> pick = getAeFpsRange(aeFpsRange);
        assertThat(pick).isNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void level3Devices_doesNotSetFpsRange() {
        Range<Integer>[] availableFpsRanges = new Range[]{
                new Range<>(15, 30),
        };

        AeFpsRange aeFpsRange =
                createAeFpsRange(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3,
                        availableFpsRanges);

        Range<Integer> pick = getAeFpsRange(aeFpsRange);
        assertThat(pick).isNull();
    }

    private AeFpsRange createAeFpsRange(int hardwareLevel,
            Range<Integer>[] availableFpsRanges) {
        CameraCharacteristics characteristics =
                ShadowCameraCharacteristics.newCameraCharacteristics();

        ShadowCameraCharacteristics shadowCharacteristics = Shadow.extract(characteristics);
        shadowCharacteristics.set(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                hardwareLevel);
        shadowCharacteristics.set(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES,
                availableFpsRanges);

        CameraCharacteristicsCompat characteristicsCompat =
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(characteristics);
        final Quirks quirks = CameraQuirks.get(ANY_CAMERA_ID, characteristicsCompat);
        return new AeFpsRange(quirks);
    }

    private Range<Integer> getAeFpsRange(AeFpsRange aeFpsRange) {
        Camera2ImplConfig.Builder builder = new Camera2ImplConfig.Builder();
        aeFpsRange.addAeFpsRangeOptions(builder);
        return builder.build().getCaptureRequestOption(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, null);
    }
}
