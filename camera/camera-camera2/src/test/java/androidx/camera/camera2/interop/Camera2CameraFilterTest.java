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

package androidx.camera.camera2.interop;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import androidx.annotation.experimental.UseExperimental;
import androidx.camera.camera2.internal.Camera2CameraInfoImpl;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.CameraFilter;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.testing.fakes.FakeCamera;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@UseExperimental(markerClass = ExperimentalCamera2Interop.class)
public final class Camera2CameraFilterTest {
    private static final String BACK_ID = "0";
    private static final String FRONT_ID = "1";
    private static final String EXTRA_ID = "2";

    private LinkedHashSet<CameraInternal> mCameras = new LinkedHashSet<>();
    private CameraInternal mBackCamera;
    private CameraInternal mFrontCamera;
    private LinkedHashMap<String, CameraCharacteristics> mIdCharMap = new LinkedHashMap<>();

    @Before
    public void setUp() {
        Camera2CameraInfoImpl mockCameraInfoBack = mock(Camera2CameraInfoImpl.class);
        when(mockCameraInfoBack.getCameraId()).thenReturn(BACK_ID);
        mBackCamera = new FakeCamera(null, mockCameraInfoBack);
        mCameras.add(mBackCamera);
        CameraCharacteristics mockCharacteristicsBack = mock(CameraCharacteristics.class);
        when(mockCharacteristicsBack.get(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)).thenReturn(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        mIdCharMap.put(BACK_ID, mockCharacteristicsBack);
        CameraCharacteristicsCompat characteristicsCompatBack =
                mock(CameraCharacteristicsCompat.class);
        when(characteristicsCompatBack.toCameraCharacteristics())
                .thenReturn(mockCharacteristicsBack);
        when(mockCameraInfoBack.getCameraCharacteristicsCompat())
                .thenReturn(characteristicsCompatBack);

        Camera2CameraInfoImpl mockCameraInfoFront = mock(Camera2CameraInfoImpl.class);
        when(mockCameraInfoFront.getCameraId()).thenReturn(FRONT_ID);
        mFrontCamera = new FakeCamera(null, mockCameraInfoFront);
        mCameras.add(mFrontCamera);
        CameraCharacteristics mockCharacteristicsFront = mock(CameraCharacteristics.class);
        when(mockCharacteristicsFront.get(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)).thenReturn(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        mIdCharMap.put(FRONT_ID, mockCharacteristicsFront);
        CameraCharacteristicsCompat characteristicsCompatFront =
                mock(CameraCharacteristicsCompat.class);
        when(characteristicsCompatFront.toCameraCharacteristics())
                .thenReturn(mockCharacteristicsFront);
        when(mockCameraInfoFront.getCameraCharacteristicsCompat())
                .thenReturn(characteristicsCompatFront);

    }

    @Test
    public void canFilterWithCamera2Filter() {
        Camera2CameraFilter.Camera2Filter camera2Filter = (idCharMap) -> {
            LinkedHashMap<String, CameraCharacteristics> resultMap = new LinkedHashMap<>();
            for (Map.Entry<String, CameraCharacteristics> entry : idCharMap.entrySet()) {
                if (entry.getValue().get(
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL).equals(
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)) {
                    resultMap.put(entry.getKey(), entry.getValue());
                }
            }
            return resultMap;
        };

        assertThat(camera2Filter.filter(mIdCharMap).keySet()).containsExactly(FRONT_ID);
    }

    @Test
    public void canSelectWithCameraSelector() {
        CameraFilter filter = Camera2CameraFilter.createCameraFilter((idCharMap) -> {
            LinkedHashMap<String, CameraCharacteristics> resultMap = new LinkedHashMap<>();
            if (idCharMap.containsKey(FRONT_ID)) {
                CameraCharacteristics characteristics = idCharMap.get(FRONT_ID);
                resultMap.put(FRONT_ID, characteristics);
            }
            return resultMap;
        });

        CameraSelector cameraSelector = new CameraSelector.Builder().addCameraFilter(
                filter).build();
        assertThat(cameraSelector.select(mCameras)).isEqualTo(mFrontCamera);
    }

    @Test(expected = IllegalArgumentException.class)
    public void exception_extraOutputCameraId() {
        CameraFilter filter = Camera2CameraFilter.createCameraFilter((idCharMap) -> {
            LinkedHashMap<String, CameraCharacteristics> resultMap = new LinkedHashMap<>();
            // Add an extra camera id to output.
            resultMap.put(EXTRA_ID, null);
            return resultMap;
        });
        CameraSelector cameraSelector = new CameraSelector.Builder().addCameraFilter(
                filter).build();
        cameraSelector.select(mCameras);
    }

    @Test(expected = IllegalArgumentException.class)
    public void exception_extraInputAndOutputCameraId() {
        CameraFilter filter = Camera2CameraFilter.createCameraFilter((idCharMap) -> {
            // Add an extra camera id to input.
            idCharMap.put(EXTRA_ID, null);
            LinkedHashMap<String, CameraCharacteristics> resultMap = new LinkedHashMap<>();
            // Add an extra camera id to output.
            resultMap.put(EXTRA_ID, null);
            return resultMap;
        });
        CameraSelector cameraSelector = new CameraSelector.Builder().addCameraFilter(
                filter).build();
        // Should throw an exception even the extra camera id is also added to the input.
        cameraSelector.select(mCameras);
    }
}
