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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@UseExperimental(markerClass = ExperimentalCamera2Interop.class)
public final class Camera2CameraFilterTest {
    private static final String BACK_ID = "0";
    private static final String FRONT_ID = "1";

    private LinkedHashSet<CameraInternal> mCameras = new LinkedHashSet<>();
    private CameraInternal mBackCamera;
    private CameraInternal mFrontCamera;
    private List<Camera2CameraInfo> mCameraInfos = new ArrayList<>();
    private Camera2CameraInfo mCameraInfoBack;
    private Camera2CameraInfo mCameraInfoFront;

    @Before
    public void setUp() {
        CameraCharacteristicsCompat mockCharacteristicsCompatBack =
                mock(CameraCharacteristicsCompat.class);
        when(mockCharacteristicsCompatBack.get(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)).thenReturn(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        Camera2CameraInfoImpl mockCameraInfoImplBack = mock(Camera2CameraInfoImpl.class);
        when(mockCameraInfoImplBack.getCameraId()).thenReturn(BACK_ID);
        when(mockCameraInfoImplBack.getCameraCharacteristicsCompat())
                .thenReturn(mockCharacteristicsCompatBack);
        mCameraInfoBack = new Camera2CameraInfo(mockCameraInfoImplBack);
        when(mockCameraInfoImplBack.getCamera2CameraInfo()).thenReturn(mCameraInfoBack);
        mCameraInfos.add(mCameraInfoBack);
        mBackCamera = new FakeCamera(null, mockCameraInfoImplBack);
        mCameras.add(mBackCamera);

        CameraCharacteristicsCompat mCharacteristicsCompatFront =
                mock(CameraCharacteristicsCompat.class);
        when(mCharacteristicsCompatFront.get(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)).thenReturn(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        Camera2CameraInfoImpl mockCameraInfoImplFront = mock(Camera2CameraInfoImpl.class);
        when(mockCameraInfoImplFront.getCameraId()).thenReturn(FRONT_ID);
        when(mockCameraInfoImplFront.getCameraCharacteristicsCompat())
                .thenReturn(mCharacteristicsCompatFront);
        mCameraInfoFront = new Camera2CameraInfo(mockCameraInfoImplFront);
        when(mockCameraInfoImplFront.getCamera2CameraInfo()).thenReturn(mCameraInfoFront);
        mCameraInfos.add(mCameraInfoFront);
        mFrontCamera = new FakeCamera(null, mockCameraInfoImplFront);
        mCameras.add(mFrontCamera);
    }

    @Test
    public void canFilterWithCamera2Filter() {
        Camera2CameraFilter.Camera2Filter camera2Filter = cameraInfos -> {
            List<Camera2CameraInfo> output = new ArrayList<>();
            for (Camera2CameraInfo cameraInfo : cameraInfos) {
                if (cameraInfo.getCameraCharacteristic(
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL).equals(
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)) {
                    output.add(cameraInfo);
                }
            }
            return output;
        };

        assertThat(camera2Filter.filter(mCameraInfos)).containsExactly(mCameraInfoFront);
    }

    @Test
    public void canSelectWithCameraSelector() {
        CameraFilter filter = Camera2CameraFilter.createCameraFilter(cameraInfos -> {
            List<Camera2CameraInfo> output = new ArrayList<>();
            for (Camera2CameraInfo cameraInfo : cameraInfos) {
                if (cameraInfo.getCameraId().equals(FRONT_ID)) {
                    output.add(cameraInfo);
                }
            }
            return output;
        });

        CameraSelector cameraSelector = new CameraSelector.Builder().addCameraFilter(
                filter).build();
        assertThat(cameraSelector.select(mCameras)).isEqualTo(mFrontCamera);
    }

    @Test(expected = IllegalArgumentException.class)
    public void exception_extraOutputCameraInfo() {
        CameraFilter filter = Camera2CameraFilter.createCameraFilter(cameraInfos -> {
            List<Camera2CameraInfo> output = new ArrayList<>();
            // Add an extra camera id to output.
            output.add(mock(Camera2CameraInfo.class));
            return output;
        });

        CameraSelector cameraSelector = new CameraSelector.Builder().addCameraFilter(
                filter).build();
        cameraSelector.select(mCameras);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void exception_extraInputCameraInfo() {
        CameraFilter filter = Camera2CameraFilter.createCameraFilter(cameraInfos -> {
            // Add an extra camera id to input.
            cameraInfos.add(mock(Camera2CameraInfo.class));
            return cameraInfos;
        });

        CameraSelector cameraSelector = new CameraSelector.Builder().addCameraFilter(
                filter).build();
        // Should throw an exception if the input is modified.
        cameraSelector.select(mCameras);
    }
}
