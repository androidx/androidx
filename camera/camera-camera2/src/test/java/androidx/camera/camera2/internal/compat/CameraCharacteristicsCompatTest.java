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

package androidx.camera.camera2.internal.compat;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import androidx.annotation.RequiresApi;

import org.junit.Before;
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
public class CameraCharacteristicsCompatTest {
    private CameraCharacteristics mCharacteristics;
    private static final int SENSOR_ORIENTATION_VAL = 270;

    @Before
    public void setUp() {
        mCharacteristics = ShadowCameraCharacteristics.newCameraCharacteristics();
        ShadowCameraCharacteristics shadowCharacteristics0 = Shadow.extract(mCharacteristics);
        shadowCharacteristics0.set(CameraCharacteristics.CONTROL_MAX_REGIONS_AE, 1);
        shadowCharacteristics0.set(CameraCharacteristics.CONTROL_MAX_REGIONS_AF, 2);
        shadowCharacteristics0.set(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB, 3);

        shadowCharacteristics0.set(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES, null);
        shadowCharacteristics0.set(
                CameraCharacteristics.SENSOR_ORIENTATION, SENSOR_ORIENTATION_VAL);
    }

    @Test
    public void canGetCorrectValues() {
        CameraCharacteristicsCompat characteristicsCompat =
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(mCharacteristics);

        assertThat(characteristicsCompat.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE))
                .isEqualTo(mCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE));

        assertThat(characteristicsCompat.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF))
                .isEqualTo(mCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF));
    }

    @Test
    public void canGetCachedValues() {
        CameraCharacteristicsCompat characteristicsCompat =
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(mCharacteristics);


        assertThat(characteristicsCompat.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE))
                .isEqualTo(mCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE));

        assertThat(characteristicsCompat.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE))
                .isEqualTo(mCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE));
    }

    @Test
    public void canGetNullValue() {
        CameraCharacteristicsCompat characteristicsCompat =
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(mCharacteristics);

        // CONTROL_AE_AVAILABLE_MODES is set to null in setUp
        assertThat(characteristicsCompat.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES))
                .isNull();
        // INFO_SUPPORTED_HARDWARE_LEVEL is not set.
        assertThat(characteristicsCompat.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL))
                .isNull();
    }

    @Config(minSdk = 28)
    @RequiresApi(28)
    @Test
    public void getPhysicalCameraIds_invokeCameraCharacteristics_api28() {
        CameraCharacteristics cameraCharacteristics = mock(CameraCharacteristics.class);
        CameraCharacteristicsCompat characteristicsCompat =
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(cameraCharacteristics);

        characteristicsCompat.getPhysicalCameraIds();
        verify(cameraCharacteristics).getPhysicalCameraIds();
    }

    @Config(maxSdk = 27)
    @Test
    public void getPhysicalCameraIds_returnEmptyList_below28() {
        CameraCharacteristicsCompat characteristicsCompat =
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(mCharacteristics);
        assertThat(characteristicsCompat.getPhysicalCameraIds()).isEmpty();
    }

    @Test
    public void getSensorOrientation_shouldNotCache() {
        CameraCharacteristics cameraCharacteristics = spy(mCharacteristics);
        CameraCharacteristicsCompat characteristicsCompat =
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(cameraCharacteristics);
        assertThat(characteristicsCompat.get(CameraCharacteristics.SENSOR_ORIENTATION))
                .isEqualTo(SENSOR_ORIENTATION_VAL);

        // Call get() twice, cameraCharacteristics.get() should be called twice as well.
        assertThat(characteristicsCompat.get(CameraCharacteristics.SENSOR_ORIENTATION))
                .isEqualTo(SENSOR_ORIENTATION_VAL);
        verify(cameraCharacteristics, times(2)).get(CameraCharacteristics.SENSOR_ORIENTATION);
    }
}
