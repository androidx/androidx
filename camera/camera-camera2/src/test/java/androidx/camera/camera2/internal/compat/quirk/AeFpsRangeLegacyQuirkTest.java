/*
 * Copyright 2024 The Android Open Source Project
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
import android.util.Range;

import androidx.annotation.Nullable;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.internal.compat.quirk.AeFpsRangeQuirk;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;
import org.robolectric.shadows.StreamConfigurationMapBuilder;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class AeFpsRangeLegacyQuirkTest {

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

        AeFpsRangeQuirk aeFpsRangeQuirk =
                createAeFpsRangeQuirk(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                        availableFpsRanges);
        assertThat(aeFpsRangeQuirk.getTargetAeFpsRange()).isEqualTo(Range.create(15, 30));
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

        AeFpsRangeQuirk aeFpsRangeQuirk =
                createAeFpsRangeQuirk(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                        availableFpsRanges);
        assertThat(aeFpsRangeQuirk.getTargetAeFpsRange()).isEqualTo(
                StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED);
    }

    @Test
    public void availableArrayIsNull_doesNotSetFpsRange() {
        AeFpsRangeQuirk aeFpsRangeQuirk =
                createAeFpsRangeQuirk(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                        null);
        assertThat(aeFpsRangeQuirk.getTargetAeFpsRange()).isEqualTo(
                StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void limitedDevices_doesNotSetFpsRange() {
        Range<Integer>[] availableFpsRanges = new Range[]{
                new Range<>(15, 30),
        };

        AeFpsRangeQuirk aeFpsRangeQuirk =
                createAeFpsRangeQuirk(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                        availableFpsRanges);
        assertThat(aeFpsRangeQuirk).isNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void fullDevices_doesNotSetFpsRange() {
        Range<Integer>[] availableFpsRanges = new Range[]{
                new Range<>(15, 30),
        };

        AeFpsRangeQuirk aeFpsRangeQuirk =
                createAeFpsRangeQuirk(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                        availableFpsRanges);
        assertThat(aeFpsRangeQuirk).isNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void level3Devices_doesNotSetFpsRange() {
        Range<Integer>[] availableFpsRanges = new Range[]{
                new Range<>(15, 30),
        };

        AeFpsRangeQuirk aeFpsRangeQuirk =
                createAeFpsRangeQuirk(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3,
                        availableFpsRanges);
        assertThat(aeFpsRangeQuirk).isNull();
    }

    @Nullable
    private AeFpsRangeQuirk createAeFpsRangeQuirk(int hardwareLevel,
            Range<Integer>[] availableFpsRanges) {
        CameraCharacteristics characteristics =
                ShadowCameraCharacteristics.newCameraCharacteristics();

        ShadowCameraCharacteristics shadowCharacteristics = Shadow.extract(characteristics);
        shadowCharacteristics.set(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                hardwareLevel);
        shadowCharacteristics.set(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES,
                availableFpsRanges);
        shadowCharacteristics.set(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP,
                StreamConfigurationMapBuilder.newBuilder().build());

        CameraCharacteristicsCompat characteristicsCompat =
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(characteristics,
                        ANY_CAMERA_ID);
        List<AeFpsRangeQuirk> aeFpsRangeQuirkList = CameraQuirks.get(ANY_CAMERA_ID,
                characteristicsCompat).getAll(AeFpsRangeQuirk.class);
        return aeFpsRangeQuirkList.isEmpty() ? null : aeFpsRangeQuirkList.get(0);
    }
}
