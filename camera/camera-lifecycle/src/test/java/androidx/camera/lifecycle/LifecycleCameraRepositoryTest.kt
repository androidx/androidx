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
package androidx.camera.lifecycle

import android.os.Build
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraIdentifier
import androidx.camera.core.CompositionSettings
import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.LegacySessionConfig
import androidx.camera.core.UseCase
import androidx.camera.core.ViewPort
import androidx.camera.core.concurrent.CameraCoordinator
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.CameraConfig
import androidx.camera.core.impl.CameraConfigs
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.core.internal.StreamSpecsCalculatorImpl
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.impl.fakes.FakeCameraConfig
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator
import androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@OptIn(ExperimentalSessionConfig::class)
class LifecycleCameraRepositoryTest {
    private lateinit var lifecycleOwner: FakeLifecycleOwner
    private lateinit var repository: LifecycleCameraRepository
    private lateinit var defaultCameraCoordinator: FakeCameraCoordinator
    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter
    private var cameraId = 0
    private val camera: CameraInternal = FakeCamera(cameraId.toString())

    @Before
    fun setUp() {
        defaultCameraCoordinator = FakeCameraCoordinator()
        lifecycleOwner = FakeLifecycleOwner()
        repository = LifecycleCameraRepository()
        val useCaseConfigFactory = FakeUseCaseConfigFactory()
        cameraUseCaseAdapter =
            CameraUseCaseAdapter(
                camera,
                defaultCameraCoordinator,
                StreamSpecsCalculatorImpl(useCaseConfigFactory, FakeCameraDeviceSurfaceManager()),
                FakeUseCaseConfigFactory(),
            )
    }

    @Test
    fun throwException_ifTryingToCreateWithExistingIdentifier() {
        repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)

        assertThrows(IllegalArgumentException::class.java) {
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        }
    }

    @Test
    fun differentLifecycleCamerasAreCreated_forDifferentLifecycles() {
        val firstLifecycleCamera =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        val secondLifecycle = FakeLifecycleOwner()
        val secondLifecycleCamera =
            repository.createLifecycleCamera(secondLifecycle, cameraUseCaseAdapter)

        assertThat(firstLifecycleCamera).isNotEqualTo(secondLifecycleCamera)
    }

    @Test
    fun differentLifecycleCamerasAreCreated_forDifferentCameraSets() {
        val firstLifecycleCamera =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)

        // Creates LifecycleCamera with different camera set
        val secondLifecycleCamera =
            repository.createLifecycleCamera(lifecycleOwner, createNewCameraUseCaseAdapter())

        assertThat(firstLifecycleCamera).isNotEqualTo(secondLifecycleCamera)
    }

    @Test
    fun differentLifecycleCamerasAreCreated_forDifferentCameraConfig() {
        val firstLifecycleCamera =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)

        // Creates LifecycleCamera with different camera set
        val secondLifecycleCamera =
            repository.createLifecycleCamera(
                lifecycleOwner,
                createCameraUseCaseAdapterWithNewCameraConfig(),
            )

        assertThat(firstLifecycleCamera).isNotEqualTo(secondLifecycleCamera)
    }

    @Test
    fun lifecycleCameraIsNotActive_createWithNoUseCasesAfterLifecycleStarted() {
        lifecycleOwner.start()
        val lifecycleCamera = repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        assertThat(lifecycleCamera.isActive).isFalse()
    }

    @Test
    fun lifecycleCameraIsNotActive_createWithNoUseCasesBeforeLifecycleStarted() {
        val lifecycleCamera = repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        lifecycleOwner.start()
        assertThat(lifecycleCamera.isActive).isFalse()
    }

    @Test
    fun lifecycleCameraIsNotActive_bindUseCase_whenLifecycleIsNotStarted() {
        val lifecycleCamera = repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        repository.bindToLifecycleCameraExt(lifecycleCamera, listOf(FakeUseCase()))
        // LifecycleCamera is inactive before the lifecycle state becomes ON_START.
        assertThat(lifecycleCamera.isActive).isFalse()
    }

    @Test
    fun lifecycleCameraIsActive_lifecycleStartedAfterBindUseCase() {
        val lifecycleCamera = repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        repository.bindToLifecycleCameraExt(lifecycleCamera, listOf(FakeUseCase()))
        lifecycleOwner.start()
        // LifecycleCamera is active after the lifecycle state becomes ON_START.
        assertThat(lifecycleCamera.isActive).isTrue()
    }

    @Test
    fun lifecycleCameraIsActive_bindToLifecycleCameraAfterLifecycleStarted() {
        val lifecycleCamera = repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        lifecycleOwner.start()
        repository.bindToLifecycleCameraExt(lifecycleCamera, listOf(FakeUseCase()))

        // LifecycleCamera is active after binding a use case when lifecycle state is ON_START.
        assertThat(lifecycleCamera.isActive).isTrue()
    }

    @Test
    fun throwException_withUseCase_twoLifecycleCamerasControlledByOneLifecycle() {
        // Creates first LifecycleCamera with use case bound.
        val lifecycleCamera0 =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        repository.bindToLifecycleCameraExt(lifecycleCamera0, listOf(FakeUseCase()))

        // Creates second LifecycleCamera with use case bound to the same Lifecycle.
        val lifecycleCamera1 =
            repository.createLifecycleCamera(lifecycleOwner, createNewCameraUseCaseAdapter())
        assertThrows(IllegalArgumentException::class.java) {
            repository.bindToLifecycleCameraExt(lifecycleCamera1, listOf(FakeUseCase()))
        }
    }

    @Test
    fun lifecycleCameraIsNotActive_withNoUseCase_unbindAfterLifecycleStarted() {
        // Creates LifecycleCamera with use case bound.
        val lifecycleCamera = repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        lifecycleOwner.start()
        val useCase = FakeUseCase()
        repository.bindToLifecycleCameraExt(lifecycleCamera, listOf(useCase))

        // Unbinds the use case that was bound previously.
        repository.unbind(LegacySessionConfig(listOf(useCase)))

        // LifecycleCamera is not active if LifecycleCamera has no use case bound after unbinding
        // the use case.
        assertThat(lifecycleCamera.isActive).isFalse()
    }

    @Test
    fun lifecycleCameraIsActive_withUseCase_unbindAfterLifecycleStarted() {
        // Creates LifecycleCamera with two use cases bound.
        val lifecycleCamera = repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        lifecycleOwner.start()
        val useCase0 = FakeUseCase()
        val useCase1 = FakeUseCase()
        repository.bindToLifecycleCameraExt(lifecycleCamera, listOf(useCase0, useCase1))

        // Only unbinds one use case but another one is kept in the LifecycleCamera.
        repository.unbind(LegacySessionConfig(listOf(useCase0)))

        // LifecycleCamera is active if LifecycleCamera still has use case bound after unbinding
        // the use case.
        assertThat(lifecycleCamera.isActive).isTrue()
    }

    @Test
    fun lifecycleCameraIsNotActive_unbindAllAfterLifecycleStarted() {
        // Creates LifecycleCamera with use case bound.
        val lifecycleCamera = repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        lifecycleOwner.start()
        repository.bindToLifecycleCameraExt(lifecycleCamera, listOf(FakeUseCase()))

        // Unbinds all use cases from all LifecycleCamera by the unbindAll() API.
        repository.unbindAll()

        // LifecycleCamera is not active after unbinding all use cases.
        assertThat(lifecycleCamera.isActive).isFalse()
    }

    @Test
    fun lifecycleCameraOf1stActiveLifecycleIsInactive_bindToNewActiveLifecycleCamera() {
        // Starts first lifecycle with use case bound.
        val lifecycleCamera0 =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        lifecycleOwner.start()
        repository.bindToLifecycleCameraExt(lifecycleCamera0, listOf(FakeUseCase()))

        // Starts second lifecycle with use case bound.
        val lifecycle1 = FakeLifecycleOwner()
        val lifecycleCamera1 =
            repository.createLifecycleCamera(lifecycle1, createNewCameraUseCaseAdapter())
        lifecycle1.start()
        repository.bindToLifecycleCameraExt(lifecycleCamera1, listOf(FakeUseCase()))

        // The previous LifecycleCamera becomes inactive after new LifecycleCamera becomes active.
        assertThat(lifecycleCamera0.isActive).isFalse()
        // New LifecycleCamera becomes active after binding use case to it.
        assertThat(lifecycleCamera1.isActive).isTrue()
    }

    @Test
    fun lifecycleCameraOf1stActiveLifecycleIsActive_bindNewUseCase() {
        // Starts first lifecycle with use case bound.
        val lifecycleCamera0 =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        lifecycleOwner.start()
        repository.bindToLifecycleCameraExt(lifecycleCamera0, listOf(FakeUseCase()))

        // Starts second lifecycle with use case bound.
        val lifecycle1 = FakeLifecycleOwner()
        val lifecycleCamera1 =
            repository.createLifecycleCamera(lifecycle1, createNewCameraUseCaseAdapter())
        lifecycle1.start()
        repository.bindToLifecycleCameraExt(lifecycleCamera1, listOf(FakeUseCase()))

        // Binds new use case to the next most recent active LifecycleCamera.
        repository.bindToLifecycleCameraExt(lifecycleCamera0, listOf(FakeUseCase()))

        // The next most recent active LifecycleCamera becomes active after binding new use case.
        assertThat(lifecycleCamera0.isActive).isTrue()
        // The original active LifecycleCamera becomes inactive after the next most recent active
        // LifecycleCamera becomes active.
        assertThat(lifecycleCamera1.isActive).isFalse()
    }

    @Test
    fun lifecycleCameraOf2ndActiveLifecycleIsActive_unbindFromActiveLifecycleCamera() {
        // Starts first lifecycle with use case bound.
        val lifecycleCamera0 =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        lifecycleOwner.start()
        repository.bindToLifecycleCameraExt(lifecycleCamera0, listOf(FakeUseCase()))

        // Starts second lifecycle with use case bound.
        val lifecycle1 = FakeLifecycleOwner()
        val lifecycleCamera1 =
            repository.createLifecycleCamera(lifecycle1, createNewCameraUseCaseAdapter())
        lifecycle1.start()
        val useCase = FakeUseCase()
        repository.bindToLifecycleCameraExt(lifecycleCamera1, listOf(useCase))

        // Unbinds use case from the most recent active LifecycleCamera.
        repository.unbind(LegacySessionConfig(listOf(useCase)))

        // The most recent active LifecycleCamera becomes inactive after all use case unbound
        // from it.
        assertThat(lifecycleCamera1.isActive).isFalse()
        // The next most recent active LifecycleCamera becomes active after previous active
        // LifecycleCamera becomes inactive.
        assertThat(lifecycleCamera0.isActive).isTrue()
    }

    @Test
    fun useCaseIsCleared_whenLifecycleIsDestroyed() {
        val lifecycleCamera = repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        val useCase = FakeUseCase()
        repository.bindToLifecycleCameraExt(lifecycleCamera, listOf(useCase))

        assertThat(useCase.isDetached).isFalse()

        lifecycleOwner.destroy()

        assertThat(useCase.isDetached).isTrue()
    }

    @Test
    fun lifecycleCameraIsStopped_whenNewLifecycleIsStarted() {
        // Starts first lifecycle and check LifecycleCamera active state is true.
        val firstLifecycleCamera =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        repository.bindToLifecycleCameraExt(firstLifecycleCamera, listOf(FakeUseCase()))
        lifecycleOwner.start()
        assertThat(firstLifecycleCamera.isActive).isTrue()

        // Starts second lifecycle and check previous LifecycleCamera is stopped.
        val secondLifecycle = FakeLifecycleOwner()
        val secondLifecycleCamera =
            repository.createLifecycleCamera(secondLifecycle, createNewCameraUseCaseAdapter())
        repository.bindToLifecycleCameraExt(secondLifecycleCamera, listOf(FakeUseCase()))
        secondLifecycle.start()
        assertThat(secondLifecycleCamera.isActive).isTrue()
        assertThat(firstLifecycleCamera.isActive).isFalse()
    }

    @Test
    fun lifecycleCameraOf2ndActiveLifecycleIsStarted_when1stActiveLifecycleIsStopped() {
        // Starts first lifecycle and check LifecycleCamera active state is true.
        val firstLifecycleCamera =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        repository.bindToLifecycleCameraExt(firstLifecycleCamera, listOf(FakeUseCase()))
        lifecycleOwner.start()
        assertThat(firstLifecycleCamera.isActive).isTrue()

        // Starts second lifecycle and check previous LifecycleCamera is stopped.
        val secondLifecycle = FakeLifecycleOwner()
        val secondLifecycleCamera =
            repository.createLifecycleCamera(secondLifecycle, createNewCameraUseCaseAdapter())
        repository.bindToLifecycleCameraExt(secondLifecycleCamera, listOf(FakeUseCase()))
        secondLifecycle.start()
        assertThat(secondLifecycleCamera.isActive).isTrue()
        assertThat(firstLifecycleCamera.isActive).isFalse()

        // Stops second lifecycle and check previous LifecycleCamera is started again.
        secondLifecycle.stop()
        assertThat(secondLifecycleCamera.isActive).isFalse()
        assertThat(firstLifecycleCamera.isActive).isTrue()
    }

    @Test
    fun lifecycleCameraWithUseCaseIsActive_whenNewLifecycleCameraWithoutUseCaseIsStarted() {
        // Starts first LifecycleCamera with use case bound.
        val firstLifecycleCamera =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        repository.bindToLifecycleCameraExt(firstLifecycleCamera, listOf(FakeUseCase()))
        lifecycleOwner.start()
        assertThat(firstLifecycleCamera.isActive).isTrue()

        // Starts second LifecycleCamera without use case bound.
        val secondLifecycle = FakeLifecycleOwner()
        val secondLifecycleCamera =
            repository.createLifecycleCamera(secondLifecycle, createNewCameraUseCaseAdapter())
        secondLifecycle.start()

        // The first LifecycleCamera is still active because the second LifecycleCamera won't
        // become active when there is no use case bound.
        assertThat(firstLifecycleCamera.isActive).isTrue()
        assertThat(secondLifecycleCamera.isActive).isFalse()
    }

    @Test
    fun onlyLifecycleCameraWithUseCaseIsActive_afterLifecycleIsStarted() {
        // Starts first LifecycleCamera with no use case bound.
        val lifecycleCamera0 =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        lifecycleOwner.start()

        // Starts second LifecycleCamera with use case bound to the same Lifecycle.
        val lifecycleCamera1 =
            repository.createLifecycleCamera(lifecycleOwner, createNewCameraUseCaseAdapter())
        repository.bindToLifecycleCameraExt(lifecycleCamera1, listOf(FakeUseCase()))

        // Starts third LifecycleCamera with no use case bound to the same Lifecycle.
        val lifecycleCamera2 =
            repository.createLifecycleCamera(lifecycleOwner, createNewCameraUseCaseAdapter())

        // Checks only the LifecycleCamera with use case bound can become active.
        assertThat(lifecycleCamera0.isActive).isFalse()
        assertThat(lifecycleCamera1.isActive).isTrue()
        assertThat(lifecycleCamera2.isActive).isFalse()

        // Stops and resumes the lifecycle
        lifecycleOwner.stop()
        lifecycleOwner.start()

        // Checks still only the LifecycleCamera with use case bound is active.
        assertThat(lifecycleCamera0.isActive).isFalse()
        assertThat(lifecycleCamera1.isActive).isTrue()
        assertThat(lifecycleCamera2.isActive).isFalse()
    }

    @Test
    fun retrievesExistingCamera() {
        val lifecycleCamera = repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        val retrieved =
            repository.getLifecycleCamera(
                lifecycleOwner,
                CameraIdentifier.create(
                    camera.getCameraInfoInternal().getCameraId(),
                    null,
                    CameraConfigs.defaultConfig().getCompatibilityId(),
                ),
            )

        assertThat(lifecycleCamera).isSameInstanceAs(retrieved)
    }

    @Test
    fun removeLifecycleCameras_removedFromRepository() {
        repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        val key =
            LifecycleCameraRepository.Key.create(
                lifecycleOwner,
                cameraUseCaseAdapter.adapterIdentifier,
            )
        repository.removeLifecycleCameras(setOf(key))

        assertThat(
                repository.getLifecycleCamera(
                    lifecycleOwner,
                    cameraUseCaseAdapter.adapterIdentifier,
                )
            )
            .isNull()
    }

    @Test
    fun lifecycleCameraWithDifferentCameraConfig_returnDifferentInstance() {
        val lifecycleCamera1 =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)

        val newCameraUseCaseAdapter = createCameraUseCaseAdapterWithNewCameraConfig()
        val lifecycleCamera2 =
            repository.createLifecycleCamera(lifecycleOwner, newCameraUseCaseAdapter)

        val retrieved1 =
            repository.getLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter.adapterIdentifier)

        val retrieved2 =
            repository.getLifecycleCamera(lifecycleOwner, newCameraUseCaseAdapter.adapterIdentifier)

        assertThat(lifecycleCamera1).isSameInstanceAs(retrieved1)
        assertThat(lifecycleCamera2).isSameInstanceAs(retrieved2)
        assertThat(retrieved1).isNotSameInstanceAs(retrieved2)
    }

    @Test
    fun keys() {
        val key0 =
            LifecycleCameraRepository.Key.create(
                lifecycleOwner,
                cameraUseCaseAdapter.adapterIdentifier,
            )
        val key1 =
            LifecycleCameraRepository.Key.create(
                lifecycleOwner,
                CameraIdentifier.create(
                    camera.getCameraInfoInternal().getCameraId(),
                    null,
                    CameraConfigs.defaultConfig().getCompatibilityId(),
                ),
            )

        assertThat(key0).isEqualTo(key1)
    }

    @Test
    fun noException_setInactiveAfterUnregisterLifecycle() {
        // This test simulate an ON_STOP event comes after an ON_DESTROY event. It should be an
        // abnormal case and the FakeLifecycleOwner will throw IllegalStateException. See
        // b/222105787 for why this test is added.

        // Starts LifecycleCamera with use case bound.

        val lifecycleCamera = repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        repository.bindToLifecycleCameraExt(lifecycleCamera, listOf(FakeUseCase()))
        lifecycleOwner.start()
        assertThat(lifecycleCamera.isActive).isTrue()

        // This will be called when an ON_DESTROY event is received.
        repository.unregisterLifecycle(lifecycleOwner)

        // This will be called when an ON_STOP event is received.
        repository.setInactive(lifecycleOwner)
    }

    @Test
    fun concurrentModeOn_twoLifecycleCamerasControlledByOneLifecycle_start() {
        defaultCameraCoordinator.setCameraOperatingMode(
            CameraCoordinator.CAMERA_OPERATING_MODE_CONCURRENT
        )

        // Starts first lifecycle camera
        val lifecycleCamera0 =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        repository.bindToLifecycleCameraExt(lifecycleCamera0, listOf(FakeUseCase()))

        // Starts second lifecycle camera
        val lifecycleCamera1 =
            repository.createLifecycleCamera(lifecycleOwner, createNewCameraUseCaseAdapter())
        repository.bindToLifecycleCameraExt(lifecycleCamera1, listOf(FakeUseCase()))

        // Starts lifecycle
        lifecycleOwner.start()

        // Both cameras are active in concurrent mode
        assertThat(lifecycleCamera0.isActive).isTrue()
        assertThat(lifecycleCamera1.isActive).isTrue()
    }

    @Test
    fun concurrentModeOn_twoLifecycleCamerasControlledByTwoLifecycles_start() {
        defaultCameraCoordinator.setCameraOperatingMode(
            CameraCoordinator.CAMERA_OPERATING_MODE_CONCURRENT
        )

        // Starts first lifecycle camera
        val lifecycleCamera0 =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        repository.bindToLifecycleCameraExt(lifecycleCamera0, listOf(FakeUseCase()))

        // Starts lifecycle
        lifecycleOwner.start()

        // Starts second lifecycle camera
        val lifecycle1 = FakeLifecycleOwner()
        val lifecycleCamera1 =
            repository.createLifecycleCamera(lifecycle1, createNewCameraUseCaseAdapter())
        repository.bindToLifecycleCameraExt(lifecycleCamera1, listOf(FakeUseCase()))

        // Starts lifecycle1
        lifecycle1.start()

        // Both cameras are active in concurrent mode
        assertThat(lifecycleCamera0.isActive).isTrue()
        assertThat(lifecycleCamera1.isActive).isTrue()
    }

    @Test
    fun concurrentModeOn_twoLifecycleCamerasControlledByOneLifecycle_stop() {
        defaultCameraCoordinator.setCameraOperatingMode(
            CameraCoordinator.CAMERA_OPERATING_MODE_CONCURRENT
        )

        // Starts first lifecycle camera
        val firstLifecycleCamera =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        repository.bindToLifecycleCameraExt(firstLifecycleCamera, listOf(FakeUseCase()))

        // Starts second lifecycle camera
        val secondLifecycleCamera =
            repository.createLifecycleCamera(lifecycleOwner, createNewCameraUseCaseAdapter())
        repository.bindToLifecycleCameraExt(secondLifecycleCamera, listOf(FakeUseCase()))

        // Starts lifecycle
        lifecycleOwner.start()
        assertThat(secondLifecycleCamera.isActive).isTrue()
        assertThat(firstLifecycleCamera.isActive).isTrue()

        // Stops lifecycle
        lifecycleOwner.stop()
        assertThat(secondLifecycleCamera.isActive).isFalse()
        assertThat(firstLifecycleCamera.isActive).isFalse()
    }

    @Test
    fun concurrentModeOn_twoLifecycleCamerasControlledByTwoLifecycles_stop() {
        defaultCameraCoordinator.setCameraOperatingMode(
            CameraCoordinator.CAMERA_OPERATING_MODE_CONCURRENT
        )

        // Starts first lifecycle camera
        val firstLifecycleCamera =
            repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        repository.bindToLifecycleCameraExt(firstLifecycleCamera, listOf(FakeUseCase()))
        lifecycleOwner.start()
        assertThat(firstLifecycleCamera.isActive).isTrue()

        // Starts second lifecycle camera
        val secondLifecycle = FakeLifecycleOwner()
        val secondLifecycleCamera =
            repository.createLifecycleCamera(secondLifecycle, createNewCameraUseCaseAdapter())
        repository.bindToLifecycleCameraExt(secondLifecycleCamera, listOf(FakeUseCase()))
        secondLifecycle.start()
        assertThat(secondLifecycleCamera.isActive).isTrue()
        assertThat(firstLifecycleCamera.isActive).isTrue()

        // Stops lifecycle
        secondLifecycle.stop()
        assertThat(secondLifecycleCamera.isActive).isFalse()
        assertThat(firstLifecycleCamera.isActive).isTrue()
    }

    @Test
    fun lifecycleCameraIsInactive_createAndBindToLifecycleCamera_AfterLifecycleDestroyed() {
        lifecycleOwner.destroy()
        val lifecycleCamera = repository.createLifecycleCamera(lifecycleOwner, cameraUseCaseAdapter)
        repository.bindToLifecycleCameraExt(lifecycleCamera, listOf(FakeUseCase()))

        assertThat(lifecycleCamera.isActive).isFalse()
    }

    private fun createNewCameraUseCaseAdapter(): CameraUseCaseAdapter {
        val cameraId = (++cameraId).toString()
        val fakeCamera: CameraInternal = FakeCamera(cameraId)
        val useCaseConfigFactory = FakeUseCaseConfigFactory()
        return CameraUseCaseAdapter(
            fakeCamera,
            defaultCameraCoordinator,
            StreamSpecsCalculatorImpl(useCaseConfigFactory, FakeCameraDeviceSurfaceManager()),
            useCaseConfigFactory,
        )
    }

    private fun createCameraUseCaseAdapterWithNewCameraConfig(): CameraUseCaseAdapter {
        val cameraConfig: CameraConfig = FakeCameraConfig()
        val useCaseConfigFactory = FakeUseCaseConfigFactory()
        return CameraUseCaseAdapter(
            camera,
            null,
            AdapterCameraInfo(camera.cameraInfo as CameraInfoInternal, cameraConfig),
            null,
            CompositionSettings.DEFAULT,
            CompositionSettings.DEFAULT,
            defaultCameraCoordinator,
            StreamSpecsCalculatorImpl(useCaseConfigFactory, FakeCameraDeviceSurfaceManager()),
            useCaseConfigFactory,
        )
    }

    private fun LifecycleCameraRepository.bindToLifecycleCameraExt(
        lifecycleCamera: LifecycleCamera,
        useCases: List<UseCase>,
        viewPort: ViewPort? = null,
        effects: List<CameraEffect> = emptyList(),
        cameraCoordinator: CameraCoordinator? = defaultCameraCoordinator,
    ) {
        bindToLifecycleCamera(
            lifecycleCamera,
            LegacySessionConfig(useCases, viewPort, effects),
            cameraCoordinator,
        )
    }
}
