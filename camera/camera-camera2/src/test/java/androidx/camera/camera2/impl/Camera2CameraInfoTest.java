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

package androidx.camera.camera2.impl;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.view.Surface;

import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX.LensFacing;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;
import org.robolectric.shadows.ShadowCameraManager;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class Camera2CameraInfoTest {

    private static final String CAMERA0_ID = "0";
    private static final int CAMERA0_SENSOR_ORIENTATION = 90;
    private static final LensFacing CAMERA0_LENS_FACING_ENUM = LensFacing.BACK;
    private static final int CAMERA0_LENS_FACING_INT = CameraCharacteristics.LENS_FACING_BACK;

    private static final String CAMERA1_ID = "1";
    private static final int CAMERA1_SENSOR_ORIENTATION = 0;
    private static final int CAMERA1_LENS_FACING_INT = CameraCharacteristics.LENS_FACING_FRONT;

    private static final int FAKE_SUPPORTED_HARDWARE_LEVEL =
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3;

    private CameraManager mCameraManager;

    @Before
    public void setUp() {
        initCameras();
        mCameraManager =
                (CameraManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE);
    }

    @Test
    public void canCreateCameraInfo() throws CameraInfoUnavailableException {
        CameraInfo cameraInfo = new Camera2CameraInfo(mCameraManager, CAMERA0_ID);
        assertThat(cameraInfo).isNotNull();
    }

    @Test
    public void cameraInfo_canReturnSensorOrientation() throws CameraInfoUnavailableException {
        CameraInfo cameraInfo = new Camera2CameraInfo(mCameraManager, CAMERA0_ID);
        assertThat(cameraInfo.getSensorRotationDegrees()).isEqualTo(CAMERA0_SENSOR_ORIENTATION);
    }

    @Test
    public void cameraInfo_canCalculateCorrectRelativeRotation_forBackCamera()
            throws CameraInfoUnavailableException {
        CameraInfo cameraInfo = new Camera2CameraInfo(mCameraManager, CAMERA0_ID);

        // Note: these numbers depend on the camera being a back-facing camera.
        assertThat(cameraInfo.getSensorRotationDegrees(Surface.ROTATION_0))
                .isEqualTo(CAMERA0_SENSOR_ORIENTATION);
        assertThat(cameraInfo.getSensorRotationDegrees(Surface.ROTATION_90))
                .isEqualTo((CAMERA0_SENSOR_ORIENTATION - 90 + 360) % 360);
        assertThat(cameraInfo.getSensorRotationDegrees(Surface.ROTATION_180))
                .isEqualTo((CAMERA0_SENSOR_ORIENTATION - 180 + 360) % 360);
        assertThat(cameraInfo.getSensorRotationDegrees(Surface.ROTATION_270))
                .isEqualTo((CAMERA0_SENSOR_ORIENTATION - 270 + 360) % 360);
    }

    @Test
    public void cameraInfo_canCalculateCorrectRelativeRotation_forFrontCamera()
            throws CameraInfoUnavailableException {
        CameraInfo cameraInfo = new Camera2CameraInfo(mCameraManager, CAMERA1_ID);

        // Note: these numbers depend on the camera being a front-facing camera.
        assertThat(cameraInfo.getSensorRotationDegrees(Surface.ROTATION_0))
                .isEqualTo(CAMERA1_SENSOR_ORIENTATION);
        assertThat(cameraInfo.getSensorRotationDegrees(Surface.ROTATION_90))
                .isEqualTo((CAMERA1_SENSOR_ORIENTATION + 90) % 360);
        assertThat(cameraInfo.getSensorRotationDegrees(Surface.ROTATION_180))
                .isEqualTo((CAMERA1_SENSOR_ORIENTATION + 180) % 360);
        assertThat(cameraInfo.getSensorRotationDegrees(Surface.ROTATION_270))
                .isEqualTo((CAMERA1_SENSOR_ORIENTATION + 270) % 360);
    }

    @Test
    public void cameraInfo_canReturnLensFacing() throws CameraInfoUnavailableException {
        CameraInfo cameraInfo = new Camera2CameraInfo(mCameraManager, CAMERA0_ID);
        assertThat(cameraInfo.getLensFacing()).isEqualTo(CAMERA0_LENS_FACING_ENUM);
    }

    private void initCameras() {
        // **** Camera 0 characteristics ****//
        CameraCharacteristics characteristics0 =
                ShadowCameraCharacteristics.newCameraCharacteristics();

        ShadowCameraCharacteristics shadowCharacteristics0 = Shadow.extract(characteristics0);

        shadowCharacteristics0.set(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                FAKE_SUPPORTED_HARDWARE_LEVEL);

        // Add a lens facing to the camera
        shadowCharacteristics0.set(CameraCharacteristics.LENS_FACING, CAMERA0_LENS_FACING_INT);

        // Mock the sensor orientation
        shadowCharacteristics0.set(
                CameraCharacteristics.SENSOR_ORIENTATION, CAMERA0_SENSOR_ORIENTATION);

        // Add the camera to the camera service
        ((ShadowCameraManager)
                Shadow.extract(
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CAMERA_SERVICE)))
                .addCamera(CAMERA0_ID, characteristics0);

        // **** Camera 1 characteristics ****//
        CameraCharacteristics characteristics1 =
                ShadowCameraCharacteristics.newCameraCharacteristics();

        ShadowCameraCharacteristics shadowCharacteristics1 = Shadow.extract(characteristics1);

        shadowCharacteristics1.set(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                FAKE_SUPPORTED_HARDWARE_LEVEL);

        // Add a lens facing to the camera
        shadowCharacteristics1.set(CameraCharacteristics.LENS_FACING, CAMERA1_LENS_FACING_INT);

        // Mock the sensor orientation
        shadowCharacteristics1.set(
                CameraCharacteristics.SENSOR_ORIENTATION, CAMERA1_SENSOR_ORIENTATION);

        // Add the camera to the camera service
        ((ShadowCameraManager)
                Shadow.extract(
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CAMERA_SERVICE)))
                .addCamera(CAMERA1_ID, characteristics1);
    }
}
