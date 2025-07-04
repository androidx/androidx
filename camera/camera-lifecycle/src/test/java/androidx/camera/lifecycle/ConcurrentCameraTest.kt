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

package androidx.camera.lifecycle

import android.content.Context
import android.content.pm.PackageManager.FEATURE_CAMERA_CONCURRENT
import android.os.Build
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ConcurrentCamera.SingleCameraConfig
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.concurrent.CameraCoordinator.CAMERA_OPERATING_MODE_UNSPECIFIED
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator
import androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.impl.fakes.FakeCameraFactory
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ConcurrentCameraTest {
    private val context = ApplicationProvider.getApplicationContext() as Context
    private lateinit var provider: ProcessCameraProvider

    private val lifecycleOwner0 = FakeLifecycleOwner()
    private val lifecycleOwner1 = FakeLifecycleOwner()
    private val cameraCoordinator = FakeCameraCoordinator()

    @After
    fun tearDown(): Unit = runBlocking {
        try {
            val provider = ProcessCameraProvider.getInstance(context).await()
            provider.shutdownAsync().await()
        } catch (_: IllegalStateException) {
            // ProcessCameraProvider may not be configured. Ignore.
        }
    }

    @Test
    fun getAvailableConcurrentCameraInfos() = runBlocking {
        ProcessCameraProvider.configureInstance(createConcurrentCameraAppConfig())

        provider = ProcessCameraProvider.getInstance(context).await()
        assertThat(provider.availableConcurrentCameraInfos.size).isEqualTo(2)
        assertThat(provider.availableConcurrentCameraInfos[0].size).isEqualTo(2)
        assertThat(provider.availableConcurrentCameraInfos[1].size).isEqualTo(2)
    }

    @Test
    fun shutdown_clearsPreviousConfiguration() = runBlocking {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())

        provider = ProcessCameraProvider.getInstance(context).await()
        // Clear the configuration so we can reinit
        provider.shutdownAsync().await()

        // Should not throw exception
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())
        assertThat(cameraCoordinator.cameraOperatingMode)
            .isEqualTo(CAMERA_OPERATING_MODE_UNSPECIFIED)
        assertThat(cameraCoordinator.concurrentCameraSelectors).isEmpty()
        assertThat(cameraCoordinator.activeConcurrentCameraInfos).isEmpty()
    }

    @Test
    fun bindConcurrentCamera_isBound(): Unit = runBlocking {
        ProcessCameraProvider.configureInstance(createConcurrentCameraAppConfig())

        provider = ProcessCameraProvider.getInstance(context).await()
        val useCase0 = Preview.Builder().build()
        val useCase1 = Preview.Builder().build()

        val singleCameraConfig0 =
            SingleCameraConfig(
                CameraSelector.DEFAULT_BACK_CAMERA,
                UseCaseGroup.Builder().addUseCase(useCase0).build(),
                lifecycleOwner0,
            )
        val singleCameraConfig1 =
            SingleCameraConfig(
                CameraSelector.DEFAULT_FRONT_CAMERA,
                UseCaseGroup.Builder().addUseCase(useCase1).build(),
                lifecycleOwner1,
            )

        if (context.packageManager.hasSystemFeature(FEATURE_CAMERA_CONCURRENT)) {
            val concurrentCamera =
                provider.bindToLifecycle(listOf(singleCameraConfig0, singleCameraConfig1))

            assertThat(concurrentCamera).isNotNull()
            assertThat(concurrentCamera.cameras.size).isEqualTo(2)
            assertThat(provider.isBound(useCase0)).isTrue()
            assertThat(provider.isBound(useCase1)).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isTrue()
        } else {
            assertThrows<UnsupportedOperationException> {
                provider.bindToLifecycle(listOf(singleCameraConfig0, singleCameraConfig1))
            }
        }
    }

    @Test
    fun bindConcurrentPhysicalCamera_isBound() = runBlocking {
        ProcessCameraProvider.configureInstance(createConcurrentCameraAppConfig())

        provider = ProcessCameraProvider.getInstance(context).await()
        val useCase0 = Preview.Builder().build()
        val useCase1 = Preview.Builder().build()

        val singleCameraConfig0 =
            SingleCameraConfig(
                CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build(),
                UseCaseGroup.Builder().addUseCase(useCase0).build(),
                lifecycleOwner0,
            )
        val singleCameraConfig1 =
            SingleCameraConfig(
                CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build(),
                UseCaseGroup.Builder().addUseCase(useCase1).build(),
                lifecycleOwner0,
            )

        val concurrentCamera =
            provider.bindToLifecycle(listOf(singleCameraConfig0, singleCameraConfig1))

        assertThat(concurrentCamera).isNotNull()
        assertThat(concurrentCamera.cameras.size).isEqualTo(1)
        assertThat(provider.isBound(useCase0)).isTrue()
        assertThat(provider.isBound(useCase1)).isTrue()
        assertThat(provider.isConcurrentCameraModeOn).isFalse()
    }

    @Test
    fun bindConcurrentCameraTwice_isBound(): Unit = runBlocking {
        ProcessCameraProvider.configureInstance(createConcurrentCameraAppConfig())

        provider = ProcessCameraProvider.getInstance(context).await()
        val useCase0 = Preview.Builder().build()
        val useCase1 = Preview.Builder().build()
        val useCase2 = Preview.Builder().build()

        val singleCameraConfig0 =
            SingleCameraConfig(
                CameraSelector.DEFAULT_BACK_CAMERA,
                UseCaseGroup.Builder().addUseCase(useCase0).build(),
                lifecycleOwner0,
            )
        val singleCameraConfig1 =
            SingleCameraConfig(
                CameraSelector.DEFAULT_FRONT_CAMERA,
                UseCaseGroup.Builder().addUseCase(useCase1).build(),
                lifecycleOwner1,
            )
        val singleCameraConfig2 =
            SingleCameraConfig(
                CameraSelector.DEFAULT_FRONT_CAMERA,
                UseCaseGroup.Builder().addUseCase(useCase2).build(),
                lifecycleOwner1,
            )

        if (context.packageManager.hasSystemFeature(FEATURE_CAMERA_CONCURRENT)) {
            val concurrentCamera0 =
                provider.bindToLifecycle(listOf(singleCameraConfig0, singleCameraConfig1))

            assertThat(concurrentCamera0).isNotNull()
            assertThat(concurrentCamera0.cameras.size).isEqualTo(2)
            assertThat(provider.isBound(useCase0)).isTrue()
            assertThat(provider.isBound(useCase1)).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isTrue()
        } else {
            assertThrows<UnsupportedOperationException> {
                provider.bindToLifecycle(listOf(singleCameraConfig0, singleCameraConfig1))
            }
        }

        if (context.packageManager.hasSystemFeature(FEATURE_CAMERA_CONCURRENT)) {
            val concurrentCamera1 =
                provider.bindToLifecycle(listOf(singleCameraConfig0, singleCameraConfig2))

            assertThat(concurrentCamera1).isNotNull()
            assertThat(concurrentCamera1.cameras.size).isEqualTo(2)
            assertThat(provider.isBound(useCase0)).isTrue()
            assertThat(provider.isBound(useCase2)).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isTrue()
        } else {
            assertThrows<UnsupportedOperationException> {
                provider.bindToLifecycle(listOf(singleCameraConfig0, singleCameraConfig2))
            }
        }
    }

    @Test
    fun bindConcurrentCamera_lessThanTwoSingleCameraConfigs(): Unit = runBlocking {
        ProcessCameraProvider.configureInstance(createConcurrentCameraAppConfig())

        provider = ProcessCameraProvider.getInstance(context).await()
        val useCase0 = Preview.Builder().build()

        val singleCameraConfig0 =
            SingleCameraConfig(
                CameraSelector.DEFAULT_BACK_CAMERA,
                UseCaseGroup.Builder().addUseCase(useCase0).build(),
                lifecycleOwner0,
            )

        assertThrows<IllegalArgumentException> {
            provider.bindToLifecycle(listOf(singleCameraConfig0))
        }
    }

    @Test
    fun bindConcurrentCamera_moreThanTwoSingleCameraConfigs(): Unit = runBlocking {
        ProcessCameraProvider.configureInstance(createConcurrentCameraAppConfig())

        provider = ProcessCameraProvider.getInstance(context).await()
        val useCase0 = Preview.Builder().build()
        val useCase1 = Preview.Builder().build()

        val singleCameraConfig0 =
            SingleCameraConfig(
                CameraSelector.DEFAULT_BACK_CAMERA,
                UseCaseGroup.Builder().addUseCase(useCase0).build(),
                lifecycleOwner0,
            )
        val singleCameraConfig1 =
            SingleCameraConfig(
                CameraSelector.DEFAULT_FRONT_CAMERA,
                UseCaseGroup.Builder().addUseCase(useCase1).build(),
                lifecycleOwner1,
            )
        val singleCameraConfig2 =
            SingleCameraConfig(
                CameraSelector.DEFAULT_FRONT_CAMERA,
                UseCaseGroup.Builder().addUseCase(useCase0).build(),
                lifecycleOwner1,
            )

        assertThrows<java.lang.IllegalArgumentException> {
            provider.bindToLifecycle(
                listOf(singleCameraConfig0, singleCameraConfig1, singleCameraConfig2)
            )
        }
    }

    @Test
    fun bindConcurrentCamera_isDualRecording(): Unit = runBlocking {
        ProcessCameraProvider.configureInstance(createConcurrentCameraAppConfig())

        provider = ProcessCameraProvider.getInstance(context).await()
        val useCase0 = Preview.Builder().build()
        val useCase1 =
            FakeUseCase(
                FakeUseCaseConfig.Builder(CaptureType.VIDEO_CAPTURE).useCaseConfig,
                CaptureType.VIDEO_CAPTURE,
            )

        val singleCameraConfig0 =
            SingleCameraConfig(
                CameraSelector.DEFAULT_BACK_CAMERA,
                UseCaseGroup.Builder().addUseCase(useCase0).addUseCase(useCase1).build(),
                lifecycleOwner0,
            )
        val singleCameraConfig1 =
            SingleCameraConfig(
                CameraSelector.DEFAULT_FRONT_CAMERA,
                UseCaseGroup.Builder().addUseCase(useCase0).addUseCase(useCase1).build(),
                lifecycleOwner1,
            )

        if (context.packageManager.hasSystemFeature(FEATURE_CAMERA_CONCURRENT)) {
            val concurrentCamera =
                provider.bindToLifecycle(listOf(singleCameraConfig0, singleCameraConfig1))

            assertThat(concurrentCamera).isNotNull()
            assertThat(concurrentCamera.cameras.size).isEqualTo(1)
            assertThat(provider.isBound(useCase0)).isTrue()
            assertThat(provider.isBound(useCase1)).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isTrue()
        } else {
            assertThrows<UnsupportedOperationException> {
                provider.bindToLifecycle(listOf(singleCameraConfig0, singleCameraConfig1))
            }
        }
    }

    private fun createConcurrentCameraAppConfig(): CameraXConfig {
        val combination0 =
            mapOf(
                "0" to CameraSelector.Builder().requireLensFacing(LENS_FACING_BACK).build(),
                "1" to CameraSelector.Builder().requireLensFacing(LENS_FACING_FRONT).build(),
            )
        val combination1 =
            mapOf(
                "0" to CameraSelector.Builder().requireLensFacing(LENS_FACING_BACK).build(),
                "2" to CameraSelector.Builder().requireLensFacing(LENS_FACING_FRONT).build(),
            )

        cameraCoordinator.addConcurrentCameraIdsAndCameraSelectors(combination0)
        cameraCoordinator.addConcurrentCameraIdsAndCameraSelectors(combination1)
        val cameraFactoryProvider =
            CameraFactory.Provider { _, _, _, _, _ ->
                val cameraFactory = FakeCameraFactory()
                cameraFactory.insertCamera(LENS_FACING_BACK, "0") {
                    FakeCamera("0", null, FakeCameraInfoInternal("0", 0, LENS_FACING_BACK))
                }
                cameraFactory.insertCamera(LENS_FACING_FRONT, "1") {
                    FakeCamera("1", null, FakeCameraInfoInternal("1", 0, LENS_FACING_FRONT))
                }
                cameraFactory.insertCamera(LENS_FACING_FRONT, "2") {
                    FakeCamera("2", null, FakeCameraInfoInternal("2", 0, LENS_FACING_FRONT))
                }
                cameraFactory.cameraCoordinator = cameraCoordinator
                cameraFactory
            }
        val appConfigBuilder =
            CameraXConfig.Builder()
                .setCameraFactoryProvider(cameraFactoryProvider)
                .setDeviceSurfaceManagerProvider { _, _, _ -> FakeCameraDeviceSurfaceManager() }
                .setUseCaseConfigFactoryProvider { FakeUseCaseConfigFactory() }

        return appConfigBuilder.build()
    }
}
