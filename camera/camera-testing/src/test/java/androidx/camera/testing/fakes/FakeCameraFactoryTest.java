/*
 * Copyright 2021 The Android Open Source Project
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

import static androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA;
import static androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA;
import static androidx.camera.core.CameraSelector.LENS_FACING_BACK;
import static androidx.camera.core.CameraSelector.LENS_FACING_FRONT;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;

import androidx.camera.testing.impl.fakes.FakeCameraFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class FakeCameraFactoryTest {

    private static final String CAMERA_ID_0 = "0";
    private static final String CAMERA_ID_1 = "1";

    @Test
    public void availableCameraSelectorNotProvided_getAllCameraIds() {
        // Arrange
        final FakeCameraFactory factory = new FakeCameraFactory();
        insertCamera(factory, LENS_FACING_BACK, CAMERA_ID_0);
        insertCamera(factory, LENS_FACING_FRONT, CAMERA_ID_1);

        // Act
        final Set<String> cameraIds = factory.getAvailableCameraIds();

        // Assert
        assertThat(cameraIds).containsExactly(CAMERA_ID_0, CAMERA_ID_1);
    }

    @Test
    public void availableCameraSelectorMatchesAllCameras_getAllCameraIds() {
        // Arrange
        final FakeCameraFactory factory = new FakeCameraFactory(DEFAULT_BACK_CAMERA);
        insertCamera(factory, LENS_FACING_BACK, CAMERA_ID_0);
        insertCamera(factory, LENS_FACING_BACK, CAMERA_ID_1);

        // Act
        final Set<String> cameraIds = factory.getAvailableCameraIds();

        // Assert
        assertThat(cameraIds).containsExactly(CAMERA_ID_0, CAMERA_ID_1);
    }

    @Test
    public void availableCameraSelectorMatchesSubsetOfCameras_getFilteredCameraIds() {
        // Arrange
        final FakeCameraFactory factory = new FakeCameraFactory(DEFAULT_BACK_CAMERA);
        insertCamera(factory, LENS_FACING_BACK, CAMERA_ID_0);
        insertCamera(factory, LENS_FACING_FRONT, CAMERA_ID_1);

        // Act
        final Set<String> cameraIds = factory.getAvailableCameraIds();

        // Assert
        assertThat(cameraIds).containsExactly(CAMERA_ID_0);
    }

    @Test
    public void availableCameraSelectorMatchesNoCameras_getNoCameraIds() {
        // Arrange
        final FakeCameraFactory factory = new FakeCameraFactory(DEFAULT_FRONT_CAMERA);
        insertCamera(factory, LENS_FACING_BACK, CAMERA_ID_0);
        insertCamera(factory, LENS_FACING_BACK, CAMERA_ID_1);

        // Act
        final Set<String> cameraIds = factory.getAvailableCameraIds();

        // Assert
        assertThat(cameraIds).isEmpty();
    }

    private void insertCamera(FakeCameraFactory factory, int lensFacing, String cameraId) {
        factory.insertCamera(lensFacing, cameraId, () -> new FakeCamera(cameraId, null,
                new FakeCameraInfoInternal(cameraId, 0, lensFacing)));
    }
}
