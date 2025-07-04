/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.core

import android.os.Build
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.CameraConfigs
import androidx.camera.core.impl.CameraRepository
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.FakeStreamSpecsCalculator
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator
import androidx.camera.testing.impl.fakes.FakeCameraFactory
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CameraUseCaseAdapterProviderTest {

    private val cameraCoordinator = FakeCameraCoordinator()
    private val useCaseConfigFactory = FakeUseCaseConfigFactory()
    private val streamSpecsCalculator = FakeStreamSpecsCalculator()

    @Test
    fun provide_withInvalidCameraId_throwsIllegalArgumentException() {
        // Arrange.
        val cameraId = "camera_0"
        val camera = FakeCamera(cameraId)
        val cameraFactory =
            FakeCameraFactory().apply { insertDefaultBackCamera(cameraId) { camera } }
        val cameraRepository = CameraRepository().apply { init(cameraFactory) }
        val cameraUseCaseAdapterProvider =
            CameraUseCaseAdapterProviderImpl(
                cameraRepository,
                cameraCoordinator,
                useCaseConfigFactory,
                streamSpecsCalculator,
            )

        // Act & Assert.
        assertThrows(IllegalArgumentException::class.java) {
            cameraUseCaseAdapterProvider.provide("non_existent_camera")
        }
    }

    @Test
    fun provide_withCameraId_returnsCameraUseCaseAdapter() {
        // Arrange.
        val cameraId = "camera_0"
        val camera = FakeCamera(cameraId)
        val cameraFactory =
            FakeCameraFactory().apply { insertDefaultBackCamera(cameraId) { camera } }
        val cameraRepository = CameraRepository().apply { init(cameraFactory) }
        val cameraUseCaseAdapterProvider =
            CameraUseCaseAdapterProviderImpl(
                cameraRepository,
                cameraCoordinator,
                useCaseConfigFactory,
                streamSpecsCalculator,
            )

        // Act: no exception is thrown.
        val adapter = cameraUseCaseAdapterProvider.provide(cameraId)

        // Assert.
        assertThat(adapter.adapterIdentifier.cameraIds).containsExactly(cameraId)
    }

    @Test
    fun provide_withMultipleCamerasAndSettings_returnsCameraUseCaseAdapter() {
        // Arrange.
        val cameraId0 = "camera_0"
        val cameraInfo0 = FakeCameraInfoInternal(cameraId0)
        val camera0 = FakeCamera(cameraInfo0)
        val cameraId1 = "camera_1"
        val cameraInfo1 = FakeCameraInfoInternal(cameraId1)
        val camera1 = FakeCamera(cameraInfo1)
        val cameraFactory =
            FakeCameraFactory().apply {
                insertDefaultBackCamera(cameraId0) { camera0 }
                insertDefaultFrontCamera(cameraId1) { camera1 }
            }
        val cameraRepository = CameraRepository().apply { init(cameraFactory) }
        val cameraUseCaseAdapterProvider =
            CameraUseCaseAdapterProviderImpl(
                cameraRepository,
                cameraCoordinator,
                useCaseConfigFactory,
                streamSpecsCalculator,
            )
        val adapterCameraInfo0 =
            AdapterCameraInfo(camera0.cameraInfoInternal, CameraConfigs.defaultConfig())
        val adapterCameraInfo1 =
            AdapterCameraInfo(camera1.cameraInfoInternal, CameraConfigs.defaultConfig())

        // Act: no exception is thrown.
        val adapter =
            cameraUseCaseAdapterProvider.provide(
                camera0,
                camera1,
                adapterCameraInfo0,
                adapterCameraInfo1,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
            )

        // Assert.
        val expectedCameraId =
            CameraIdentifier.fromAdapterInfos(adapterCameraInfo0, adapterCameraInfo1)
        assertThat(adapter.adapterIdentifier).isEqualTo(expectedCameraId)
    }
}
