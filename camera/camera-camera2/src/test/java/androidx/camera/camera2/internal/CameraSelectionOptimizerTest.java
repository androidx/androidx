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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.core.CameraFilter;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.impl.CameraThreadConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;
import org.robolectric.shadows.ShadowCameraManager;

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class CameraSelectionOptimizerTest {
    private Camera2CameraFactory mCamera2CameraFactory;

    @Before
    public void setUp() throws Exception {
        mCamera2CameraFactory =
                spy(new Camera2CameraFactory(ApplicationProvider.getApplicationContext(),
                        CameraThreadConfig.create(CameraXExecutors.mainThreadExecutor(),
                                new Handler(Looper.getMainLooper())),
                        null));
    }

    void setupNormalCameras() throws Exception {
        initCharacterisic("0", CameraCharacteristics.LENS_FACING_BACK, 3.52f);
        initCharacterisic("1", CameraCharacteristics.LENS_FACING_FRONT, 3.52f);
        initCharacterisic("2", CameraCharacteristics.LENS_FACING_BACK, 2.7f);
        initCharacterisic("3", CameraCharacteristics.LENS_FACING_BACK, 10.0f);
    }

    void setupAbnormalCameras() throws Exception {
        // "0" is front
        initCharacterisic("0", CameraCharacteristics.LENS_FACING_FRONT, 3.52f);
        // "1" is back
        initCharacterisic("1", CameraCharacteristics.LENS_FACING_BACK, 3.52f);
        initCharacterisic("2", CameraCharacteristics.LENS_FACING_BACK, 2.7f);
        initCharacterisic("3", CameraCharacteristics.LENS_FACING_BACK, 10.0f);
    }

    @Test
    public void availableCamerasSelectorNull_returnAllCameras() throws Exception {
        setupNormalCameras();
        List<String> cameraIds =
                CameraSelectionOptimizer.getSelectedAvailableCameraIds(mCamera2CameraFactory,
                        null);

        assertThat(cameraIds).containsExactly("0", "1", "2", "3");
    }

    @Test
    public void requireLensFacingBack() throws Exception {
        setupNormalCameras();

        CameraSelector cameraSelector =
                new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();
        List<String> cameraIds =
                CameraSelectionOptimizer.getSelectedAvailableCameraIds(mCamera2CameraFactory,
                        cameraSelector);

        assertThat(cameraIds).containsExactly("0", "2", "3");
        verify(mCamera2CameraFactory, never()).getCameraInfo("1");
    }

    @Test
    public void requireLensFacingFront() throws Exception {
        setupNormalCameras();

        CameraSelector cameraSelector =
                new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();
        List<String> cameraIds =
                CameraSelectionOptimizer.getSelectedAvailableCameraIds(mCamera2CameraFactory,
                        cameraSelector);

        assertThat(cameraIds).containsExactly("1");
        // only camera "0" 's getCameraCharacteristics can be avoided.
        verify(mCamera2CameraFactory, never()).getCameraInfo("0");
    }

    @Test
    public void requireLensFacingBack_andSelectWidestAngle() throws Exception {
        setupNormalCameras();

        CameraFilter widestAngleFilter = cameraInfoList -> {
            float minFocalLength = 10000;
            CameraInfo minFocalCameraInfo = null;
            for (CameraInfo cameraInfo : cameraInfoList) {
                float focalLength =
                        Camera2CameraInfo.from(cameraInfo).getCameraCharacteristic(
                                CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)[0];
                if (focalLength < minFocalLength) {
                    minFocalLength = focalLength;
                    minFocalCameraInfo = cameraInfo;
                }
            }
            return Arrays.asList(minFocalCameraInfo);
        };

        CameraSelector cameraSelector =
                new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .addCameraFilter(widestAngleFilter)
                        .build();

        List<String> cameraIds =
                CameraSelectionOptimizer.getSelectedAvailableCameraIds(mCamera2CameraFactory,
                        cameraSelector);

        assertThat(cameraIds).containsExactly("2");
        // only camera "1" 's getCameraCharacteristics can be avoided.
        verify(mCamera2CameraFactory, never()).getCameraInfo("1");
    }

    @Test
    public void abnormalCameraSetup_requireLensFacingBack() throws Exception {
        setupAbnormalCameras();

        CameraSelector cameraSelector =
                new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();
        List<String> cameraIds =
                CameraSelectionOptimizer.getSelectedAvailableCameraIds(mCamera2CameraFactory,
                        cameraSelector);

        // even though heuristic failed, it still works as expected.
        assertThat(cameraIds).containsExactly("1", "2", "3");
    }

    @Test
    public void abnormalCameraSetup_requireLensFacingFront() throws Exception {
        setupAbnormalCameras();

        CameraSelector cameraSelector =
                new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();
        List<String> cameraIds =
                CameraSelectionOptimizer.getSelectedAvailableCameraIds(mCamera2CameraFactory,
                        cameraSelector);

        // even though heuristic failed, it still works as expected.
        assertThat(cameraIds).containsExactly("0");
    }

    @Test
    public void emptyCameraIdList_returnEmptyAvailableIds() throws Exception {
        // Do not set up any cameras.
        List<String> cameraIds =
                CameraSelectionOptimizer.getSelectedAvailableCameraIds(mCamera2CameraFactory,
                        CameraSelector.DEFAULT_BACK_CAMERA);

        assertThat(cameraIds).isEmpty();
    }

    @Test
    public void onlyCamera0_requireFront_returnEmptyAvailableIds() throws Exception {
        initCharacterisic("0", CameraCharacteristics.LENS_FACING_BACK, 3.52f);

        List<String> cameraIds =
                CameraSelectionOptimizer.getSelectedAvailableCameraIds(mCamera2CameraFactory,
                        CameraSelector.DEFAULT_FRONT_CAMERA);

        assertThat(cameraIds).isEmpty();
    }

    @Test
    public void onlyCamera1_requireBack_returnEmptyAvailableIds() throws Exception {
        initCharacterisic("1", CameraCharacteristics.LENS_FACING_FRONT, 3.52f);

        List<String> cameraIds =
                CameraSelectionOptimizer.getSelectedAvailableCameraIds(mCamera2CameraFactory,
                        CameraSelector.DEFAULT_BACK_CAMERA);

        assertThat(cameraIds).isEmpty();
    }

    private void initCharacterisic(String cameraId, int lensFacing, float focalLength) {
        CameraCharacteristics characteristics =
                ShadowCameraCharacteristics.newCameraCharacteristics();

        ShadowCameraCharacteristics shadowCharacteristics = Shadow.extract(characteristics);

        shadowCharacteristics.set(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);

        // Add a lens facing to the camera
        shadowCharacteristics.set(CameraCharacteristics.LENS_FACING, lensFacing);

        shadowCharacteristics.set(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS,
                new float[]{focalLength});

        // Add the camera to the camera service
        ((ShadowCameraManager)
                Shadow.extract(
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CAMERA_SERVICE)))
                .addCamera(cameraId, characteristics);
    }
}
