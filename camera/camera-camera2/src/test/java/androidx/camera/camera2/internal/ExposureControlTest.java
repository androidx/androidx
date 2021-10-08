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

package androidx.camera.camera2.internal;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.Range;
import android.util.Rational;

import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.CameraControl;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;
import org.robolectric.shadows.ShadowCameraManager;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(RobolectricTestRunner.class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@DoNotInstrument
public class ExposureControlTest {

    private static final String CAMERA0_ID = "0";
    private static final String CAMERA1_ID = "1";

    private ExposureControl mExposureControl;
    private Camera2CameraControlImpl mCamera2CameraControl;

    @Before
    public void setUp() throws CameraAccessException {
        initCameras();

        CameraControlInternal.ControlUpdateCallback updateCallback = mock(
                CameraControlInternal.ControlUpdateCallback.class);

        CameraManager cameraManager =
                (CameraManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE);
        CameraCharacteristics cameraCharacteristics =
                cameraManager.getCameraCharacteristics(CAMERA0_ID);
        CameraCharacteristicsCompat cameraCharacteristicsCompat =
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(cameraCharacteristics);

        mCamera2CameraControl = spy(new Camera2CameraControlImpl(
                cameraCharacteristicsCompat,
                CameraXExecutors.mainThreadExecutor(),
                CameraXExecutors.directExecutor(),
                updateCallback));

        mExposureControl = new ExposureControl(mCamera2CameraControl, cameraCharacteristicsCompat,
                CameraXExecutors.directExecutor());
        mExposureControl.setActive(true);
    }

    private void initCameras() {
        CameraCharacteristics characteristics0 =
                ShadowCameraCharacteristics.newCameraCharacteristics();
        ShadowCameraCharacteristics shadowCharacteristics0 = Shadow.extract(characteristics0);
        shadowCharacteristics0.set(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE,
                Range.create(-4, 4));
        shadowCharacteristics0.set(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP,
                Rational.parseRational("1/2"));

        CameraCharacteristics characteristics1 =
                ShadowCameraCharacteristics.newCameraCharacteristics();
        ShadowCameraCharacteristics shadowCharacteristics1 = Shadow.extract(characteristics1);
        shadowCharacteristics1.set(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE,
                Range.create(-0, 0));

        // Add the camera to the camera service
        ShadowCameraManager shadowCameraManager = Shadow.extract(
                ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE));

        shadowCameraManager.addCamera(CAMERA0_ID, characteristics0);
        shadowCameraManager.addCamera(CAMERA1_ID, characteristics1);
    }

    @Test
    public void setExposureTwice_theFirstCallShouldBeCancelled() throws InterruptedException {
        ListenableFuture<Integer> future1 = mExposureControl.setExposureCompensationIndex(1);
        ListenableFuture<Integer> future2 = mExposureControl.setExposureCompensationIndex(2);

        // The second call should keep working.
        assertFalse(future2.isDone());

        // The first call should be cancelled with a CameraControl.OperationCanceledException.
        try {
            future1.get();
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(
                    CameraControlInternal.OperationCanceledException.class);
        }
    }

    @Test
    public void setExposureTimeout_theCompensationValueShouldKeepInControl() {
        ListenableFuture<Integer> future1 = mExposureControl.setExposureCompensationIndex(1);

        try {
            // The set future should timeout in this test.
            future1.get(0, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(TimeoutException.class);
        }

        // The new value should be set to the exposure control even when the ListenableFuture
        // task fails.
        assertThat(mExposureControl.getExposureState().getExposureCompensationIndex()).isEqualTo(1);
    }

    @Test
    public void exposureControlInactive_setExposureTaskShouldCancel()
            throws InterruptedException, TimeoutException {
        ListenableFuture<Integer> future = mExposureControl.setExposureCompensationIndex(1);
        mExposureControl.setActive(false);

        try {
            // The exposure control has been set to inactive. It should throw the exception.
            future.get(3000, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(CameraControl.OperationCanceledException.class);
        }
    }

    @Test
    public void setExposureNotInRange_shouldCompleteTheTaskWithException()
            throws InterruptedException, TimeoutException {
        try {
            // The Exposure index to 5 not in the valid range. It should throw the exception.
            mExposureControl.setExposureCompensationIndex(5).get(3000, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    public void setExposureOnNotSupportedCamera_shouldCompleteTheTaskWithException()
            throws CameraAccessException, InterruptedException, TimeoutException {
        CameraManager cameraManager =
                (CameraManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE);
        CameraCharacteristics cameraCharacteristics =
                cameraManager.getCameraCharacteristics(CAMERA1_ID);
        CameraCharacteristicsCompat cameraCharacteristicsCompat =
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(cameraCharacteristics);
        mCamera2CameraControl = spy(new Camera2CameraControlImpl(
                cameraCharacteristicsCompat,
                CameraXExecutors.mainThreadExecutor(),
                CameraXExecutors.directExecutor(),
                mock(CameraControlInternal.ControlUpdateCallback.class)));

        mExposureControl = new ExposureControl(mCamera2CameraControl, cameraCharacteristicsCompat,
                CameraXExecutors.directExecutor());
        mExposureControl.setActive(true);

        ListenableFuture<Integer> future = mExposureControl.setExposureCompensationIndex(1);
        try {
            // This camera does not support the exposure compensation, the task should fail.
            future.get(3000, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
