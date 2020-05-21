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

package androidx.camera.camera2.internal;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.view.Surface;

import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.TorchState;
import androidx.camera.core.ZoomState;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.internal.ImmutableZoomState;
import androidx.lifecycle.MutableLiveData;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;
import org.robolectric.shadows.ShadowCameraManager;

import java.util.concurrent.Executor;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class Camera2CameraInfoImplTest {

    private static final String CAMERA0_ID = "0";
    private static final int CAMERA0_SUPPORTED_HARDWARE_LEVEL =
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
    private static final int CAMERA0_SENSOR_ORIENTATION = 90;
    @CameraSelector.LensFacing
    private static final int CAMERA0_LENS_FACING_ENUM = CameraSelector.LENS_FACING_BACK;
    private static final int CAMERA0_LENS_FACING_INT = CameraCharacteristics.LENS_FACING_BACK;
    private static final boolean CAMERA0_FLASH_INFO_BOOLEAN = true;

    private static final String CAMERA1_ID = "1";
    private static final int CAMERA1_SUPPORTED_HARDWARE_LEVEL =
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3;
    private static final int CAMERA1_SENSOR_ORIENTATION = 0;
    private static final int CAMERA1_LENS_FACING_INT = CameraCharacteristics.LENS_FACING_FRONT;
    private static final boolean CAMERA1_FLASH_INFO_BOOLEAN = false;

    private CameraManager mCameraManager;
    private CameraCharacteristics mCameraCharacteristics0;
    private CameraCharacteristics mCameraCharacteristics1;
    private ZoomControl mMockZoomControl;
    private TorchControl mMockTorchControl;
    private Camera2CameraControl mMockCameraControl;

    @Before
    public void setUp() throws CameraAccessException {
        initCameras();

        mCameraManager =
                (CameraManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE);

        mCameraCharacteristics0 = mCameraManager.getCameraCharacteristics(CAMERA0_ID);
        mCameraCharacteristics1 = mCameraManager.getCameraCharacteristics(CAMERA1_ID);

        mMockZoomControl = mock(ZoomControl.class);
        mMockTorchControl = mock(TorchControl.class);
        mMockCameraControl = mock(Camera2CameraControl.class);

        when(mMockCameraControl.getZoomControl()).thenReturn(mMockZoomControl);
        when(mMockCameraControl.getTorchControl()).thenReturn(mMockTorchControl);
    }

    @Test
    public void canCreateCameraInfo() {
        CameraInfoInternal cameraInfoInternal =
                new Camera2CameraInfoImpl(CAMERA0_ID, mCameraCharacteristics0, mMockCameraControl);
        assertThat(cameraInfoInternal).isNotNull();
    }

    @Test
    public void cameraInfo_canReturnSensorOrientation() {
        CameraInfoInternal cameraInfoInternal =
                new Camera2CameraInfoImpl(CAMERA0_ID, mCameraCharacteristics0, mMockCameraControl);
        assertThat(cameraInfoInternal.getSensorRotationDegrees()).isEqualTo(
                CAMERA0_SENSOR_ORIENTATION);
    }

    @Test
    public void cameraInfo_canCalculateCorrectRelativeRotation_forBackCamera() {
        CameraInfoInternal cameraInfoInternal =
                new Camera2CameraInfoImpl(CAMERA0_ID, mCameraCharacteristics0, mMockCameraControl);

        // Note: these numbers depend on the camera being a back-facing camera.
        assertThat(cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_0))
                .isEqualTo(CAMERA0_SENSOR_ORIENTATION);
        assertThat(cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_90))
                .isEqualTo((CAMERA0_SENSOR_ORIENTATION - 90 + 360) % 360);
        assertThat(cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_180))
                .isEqualTo((CAMERA0_SENSOR_ORIENTATION - 180 + 360) % 360);
        assertThat(cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_270))
                .isEqualTo((CAMERA0_SENSOR_ORIENTATION - 270 + 360) % 360);
    }

    @Test
    public void cameraInfo_canCalculateCorrectRelativeRotation_forFrontCamera() {
        CameraInfoInternal cameraInfoInternal =
                new Camera2CameraInfoImpl(CAMERA1_ID, mCameraCharacteristics1, mMockCameraControl);

        // Note: these numbers depend on the camera being a front-facing camera.
        assertThat(cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_0))
                .isEqualTo(CAMERA1_SENSOR_ORIENTATION);
        assertThat(cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_90))
                .isEqualTo((CAMERA1_SENSOR_ORIENTATION + 90) % 360);
        assertThat(cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_180))
                .isEqualTo((CAMERA1_SENSOR_ORIENTATION + 180) % 360);
        assertThat(cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_270))
                .isEqualTo((CAMERA1_SENSOR_ORIENTATION + 270) % 360);
    }

    @Test
    public void cameraInfo_canReturnLensFacing() {
        CameraInfoInternal cameraInfoInternal =
                new Camera2CameraInfoImpl(CAMERA0_ID, mCameraCharacteristics0, mMockCameraControl);
        assertThat(cameraInfoInternal.getLensFacing()).isEqualTo(CAMERA0_LENS_FACING_ENUM);
    }

    @Test
    public void cameraInfo_canReturnHasFlashUnit_forBackCamera() {
        CameraInfoInternal cameraInfoInternal =
                new Camera2CameraInfoImpl(CAMERA0_ID, mCameraCharacteristics0, mMockCameraControl);
        assertThat(cameraInfoInternal.hasFlashUnit()).isEqualTo(CAMERA0_FLASH_INFO_BOOLEAN);
    }

    @Test
    public void cameraInfo_canReturnHasFlashUnit_forFrontCamera() {
        CameraInfoInternal cameraInfoInternal =
                new Camera2CameraInfoImpl(CAMERA1_ID, mCameraCharacteristics1, mMockCameraControl);
        assertThat(cameraInfoInternal.hasFlashUnit()).isEqualTo(CAMERA1_FLASH_INFO_BOOLEAN);
    }

    @Test
    public void cameraInfo_canReturnTorchState() {
        CameraInfoInternal cameraInfoInternal =
                new Camera2CameraInfoImpl(CAMERA0_ID, mCameraCharacteristics0, mMockCameraControl);
        when(mMockTorchControl.getTorchState()).thenReturn(new MutableLiveData<>(TorchState.OFF));
        assertThat(cameraInfoInternal.getTorchState().getValue()).isEqualTo(TorchState.OFF);
    }

    // zoom related tests just ensure it uses ZoomControl to get the value
    // Full tests are performed at ZoomControlDeviceTest / ZoomControlTest.
    @Test
    public void cameraInfo_getZoom_valueIsCorrect() {
        CameraInfoInternal cameraInfo =
                new Camera2CameraInfoImpl(CAMERA0_ID, mCameraCharacteristics0, mMockCameraControl);
        ZoomState zoomState = ImmutableZoomState.create(3.0f, 8.0f, 1.0f, 0.2f);
        when(mMockZoomControl.getZoomState()).thenReturn(new MutableLiveData<>(zoomState));
        assertThat(mMockZoomControl.getZoomState().getValue()).isEqualTo(zoomState);
    }

    @Test
    public void cameraInfo_getImplementationType_legacy() {
        final CameraInfoInternal cameraInfo = new Camera2CameraInfoImpl(CAMERA0_ID,
                mCameraCharacteristics0, mMockCameraControl);
        assertThat(cameraInfo.getImplementationType()).isEqualTo(
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2_LEGACY);
    }

    @Test
    public void cameraInfo_getImplementationType_noneLegacy() {
        final CameraInfoInternal cameraInfo = new Camera2CameraInfoImpl(CAMERA1_ID,
                mCameraCharacteristics1, mMockCameraControl);
        assertThat(cameraInfo.getImplementationType()).isEqualTo(
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2);
    }

    @Test
    public void addSessionCameraCaptureCallback_isCalledToCameraControl() {
        final CameraInfoInternal cameraInfo = new Camera2CameraInfoImpl(CAMERA1_ID,
                mCameraCharacteristics1, mMockCameraControl);

        Executor executor = mock(Executor.class);
        CameraCaptureCallback callback = mock(CameraCaptureCallback.class);
        cameraInfo.addSessionCaptureCallback(executor, callback);

        ArgumentCaptor<Executor> executorCaptor = ArgumentCaptor.forClass(Executor.class);
        ArgumentCaptor<CameraCaptureCallback> callbackCaptor =
                ArgumentCaptor.forClass(CameraCaptureCallback.class);

        verify(mMockCameraControl).addSessionCameraCaptureCallback(executorCaptor.capture(),
                callbackCaptor.capture());
        assertThat(executorCaptor.getValue()).isSameInstanceAs(executor);
        assertThat(callbackCaptor.getValue()).isSameInstanceAs(callback);
    }

    @Test
    public void removeSessionCameraCaptureCallback_isCalledToCameraControl() {
        final CameraInfoInternal cameraInfo = new Camera2CameraInfoImpl(CAMERA1_ID,
                mCameraCharacteristics1, mMockCameraControl);

        CameraCaptureCallback callback = mock(CameraCaptureCallback.class);
        cameraInfo.removeSessionCaptureCallback(callback);

        ArgumentCaptor<CameraCaptureCallback> callbackCaptor =
                ArgumentCaptor.forClass(CameraCaptureCallback.class);

        verify(mMockCameraControl).removeSessionCameraCaptureCallback(callbackCaptor.capture());
        assertThat(callbackCaptor.getValue()).isSameInstanceAs(callback);
    }

    private void initCameras() {
        // **** Camera 0 characteristics ****//
        CameraCharacteristics characteristics0 =
                ShadowCameraCharacteristics.newCameraCharacteristics();

        ShadowCameraCharacteristics shadowCharacteristics0 = Shadow.extract(characteristics0);

        shadowCharacteristics0.set(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                CAMERA0_SUPPORTED_HARDWARE_LEVEL);

        // Add a lens facing to the camera
        shadowCharacteristics0.set(CameraCharacteristics.LENS_FACING, CAMERA0_LENS_FACING_INT);

        // Mock the sensor orientation
        shadowCharacteristics0.set(
                CameraCharacteristics.SENSOR_ORIENTATION, CAMERA0_SENSOR_ORIENTATION);

        // Mock the flash unit availability
        shadowCharacteristics0.set(
                CameraCharacteristics.FLASH_INFO_AVAILABLE, CAMERA0_FLASH_INFO_BOOLEAN);

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
                CAMERA1_SUPPORTED_HARDWARE_LEVEL);

        // Add a lens facing to the camera
        shadowCharacteristics1.set(CameraCharacteristics.LENS_FACING, CAMERA1_LENS_FACING_INT);

        // Mock the sensor orientation
        shadowCharacteristics1.set(
                CameraCharacteristics.SENSOR_ORIENTATION, CAMERA1_SENSOR_ORIENTATION);

        // Mock the flash unit availability
        shadowCharacteristics1.set(
                CameraCharacteristics.FLASH_INFO_AVAILABLE, CAMERA1_FLASH_INFO_BOOLEAN);

        // Add the camera to the camera service
        ((ShadowCameraManager)
                Shadow.extract(
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CAMERA_SERVICE)))
                .addCamera(CAMERA1_ID, characteristics1);
    }
}
