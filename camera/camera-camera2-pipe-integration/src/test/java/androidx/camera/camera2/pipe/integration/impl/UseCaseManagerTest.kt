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

package androidx.camera.camera2.pipe.integration.impl

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.util.Size
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.integration.adapter.CameraStateAdapter
import androidx.camera.camera2.pipe.integration.adapter.CameraUseCaseAdapter
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.impl.UseCaseCamera.RunningUseCasesChangeListener
import androidx.camera.camera2.pipe.integration.interop.Camera2CameraControl
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.pipe.integration.testing.FakeCamera2CameraControlCompat
import androidx.camera.camera2.pipe.integration.testing.FakeCameraProperties
import androidx.camera.camera2.pipe.integration.testing.FakeUseCaseCameraComponentBuilder
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager
import org.robolectric.shadows.StreamConfigurationMapBuilder

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class UseCaseManagerTest {
    private val supportedSizes = arrayOf(Size(640, 480))
    private val streamConfigurationMap = StreamConfigurationMapBuilder.newBuilder().apply {
        supportedSizes.forEach(::addOutputSize)
    }.build()
    private val useCaseManagerList = mutableListOf<UseCaseManager>()
    private val useCaseList = mutableListOf<UseCase>()
    private val useCaseThreads by lazy {
        val dispatcher = Dispatchers.Default
        val cameraScope = CoroutineScope(
            Job() +
                dispatcher
        )

        UseCaseThreads(
            cameraScope,
            dispatcher.asExecutor(),
            dispatcher
        )
    }

    @After
    fun tearDown() = runBlocking {
        useCaseManagerList.forEach { it.close() }
        useCaseList.forEach { it.onUnbind() }
    }

    @Test
    fun enabledUseCasesEmpty_whenUseCaseAttachedOnly() {
        // Arrange
        val useCaseManager = createUseCaseManager()
        val useCase = createPreview()

        // Act
        useCaseManager.attach(listOf(useCase))

        // Assert
        val enabledUseCases = useCaseManager.camera?.runningUseCases
        assertThat(enabledUseCases).isEmpty()
    }

    @Test
    fun enabledUseCasesNotEmpty_whenUseCaseEnabled() {
        // Arrange
        val useCaseManager = createUseCaseManager()
        val useCase = createPreview()
        useCaseManager.attach(listOf(useCase))

        // Act
        useCaseManager.activate(useCase)

        // Assert
        val enabledUseCases = useCaseManager.camera?.runningUseCases
        assertThat(enabledUseCases).containsExactly(useCase)
    }

    @Test
    fun meteringRepeatingNotEnabled_whenPreviewEnabled() {
        // Arrange
        val useCaseManager = createUseCaseManager()
        val preview = createPreview()
        val imageCapture = createImageCapture()
        useCaseManager.attach(listOf(preview, imageCapture))

        // Act
        useCaseManager.activate(preview)
        useCaseManager.activate(imageCapture)

        // Assert
        val enabledUseCases = useCaseManager.camera?.runningUseCases
        assertThat(enabledUseCases).containsExactly(preview, imageCapture)
    }

    @Test
    fun meteringRepeatingEnabled_whenOnlyImageCaptureEnabled() {
        // Arrange
        val useCaseManager = createUseCaseManager()
        val imageCapture = createImageCapture()
        useCaseManager.attach(listOf(imageCapture))

        // Act
        useCaseManager.activate(imageCapture)

        // Assert
        val enabledUseCaseClasses = useCaseManager.camera?.runningUseCases?.map {
            it::class.java
        }
        assertThat(enabledUseCaseClasses).containsExactly(
            ImageCapture::class.java,
            MeteringRepeating::class.java
        )
    }

    @Test
    fun meteringRepeatingDisabled_whenPreviewBecomesEnabled() {
        // Arrange
        val useCaseManager = createUseCaseManager()
        val imageCapture = createImageCapture()
        useCaseManager.attach(listOf(imageCapture))
        useCaseManager.activate(imageCapture)

        // Act
        val preview = createPreview()
        useCaseManager.attach(listOf(preview))
        useCaseManager.activate(preview)

        // Assert
        val activeUseCases = useCaseManager.camera?.runningUseCases
        assertThat(activeUseCases).containsExactly(preview, imageCapture)
    }

    @Test
    fun meteringRepeatingEnabled_afterAllUseCasesButImageCaptureDisabled() {
        // Arrange
        val useCaseManager = createUseCaseManager()
        val preview = createPreview()
        val imageCapture = createImageCapture()
        useCaseManager.attach(listOf(preview, imageCapture))
        useCaseManager.activate(preview)
        useCaseManager.activate(imageCapture)

        // Act
        useCaseManager.detach(listOf(preview))

        // Assert
        val enabledUseCaseClasses = useCaseManager.camera?.runningUseCases?.map {
            it::class.java
        }
        assertThat(enabledUseCaseClasses).containsExactly(
            ImageCapture::class.java,
            MeteringRepeating::class.java
        )
    }

    @Test
    fun onlyOneUseCaseCameraBuilt_whenAllUseCasesButImageCaptureDisabled() {
        // Arrange
        val useCaseCameraBuilder = FakeUseCaseCameraComponentBuilder()
        val useCaseManager = createUseCaseManager(
            useCaseCameraComponentBuilder = useCaseCameraBuilder
        )

        val preview = createPreview()
        val imageCapture = createImageCapture()
        useCaseManager.attach(listOf(preview, imageCapture))
        useCaseManager.activate(preview)
        useCaseManager.activate(imageCapture)
        useCaseCameraBuilder.buildInvocationCount = 0

        // Act
        useCaseManager.detach(listOf(preview))

        // Assert
        assertThat(useCaseCameraBuilder.buildInvocationCount).isEqualTo(1)
    }

    @Test
    fun meteringRepeatingDisabled_whenAllUseCasesDisabled() {
        // Arrange
        val useCaseManager = createUseCaseManager()
        val imageCapture = createImageCapture()
        useCaseManager.attach(listOf(imageCapture))
        useCaseManager.activate(imageCapture)

        // Act
        useCaseManager.deactivate(imageCapture)

        // Assert
        val enabledUseCases = useCaseManager.camera?.runningUseCases
        assertThat(enabledUseCases).isEmpty()
    }

    @Test
    fun onlyOneUseCaseCameraBuilt_whenAllUseCasesDisabled() {
        // Arrange
        val useCaseCameraBuilder = FakeUseCaseCameraComponentBuilder()
        val useCaseManager = createUseCaseManager(
            useCaseCameraComponentBuilder = useCaseCameraBuilder
        )

        val imageCapture = createImageCapture()
        useCaseManager.attach(listOf(imageCapture))
        useCaseManager.activate(imageCapture)
        useCaseCameraBuilder.buildInvocationCount = 0

        // Act
        useCaseManager.deactivate(imageCapture)

        // Assert
        assertThat(useCaseCameraBuilder.buildInvocationCount).isEqualTo(1)
    }

    @Test
    fun onStateAttachedInvokedExactlyOnce_whenUseCaseAttachedAndMeteringRepeatingAdded() {
        // Arrange
        val useCaseManager = createUseCaseManager()
        val imageCapture = createImageCapture()
        val useCase = FakeUseCase().also {
            it.simulateActivation()
        }

        // Act
        useCaseManager.activate(imageCapture)
        useCaseManager.activate(useCase)
        useCaseManager.attach(listOf(imageCapture, useCase))

        // Assert
        assertThat(useCase.stateAttachedCount).isEqualTo(1)
    }

    @Test
    fun onStateAttachedInvokedExactlyOnce_whenUseCaseAttachedAndMeteringRepeatingNotAdded() {
        // Arrange
        val useCaseManager = createUseCaseManager()
        val preview = createPreview()
        val useCase = FakeUseCase()

        // Act
        useCaseManager.activate(preview)
        useCaseManager.activate(useCase)
        useCaseManager.attach(listOf(preview, useCase))

        // Assert
        assertThat(useCase.stateAttachedCount).isEqualTo(1)
    }

    @Test
    fun controlsNotified_whenRunningUseCasesChanged() {
        // Arrange
        val fakeControl = object : UseCaseCameraControl, RunningUseCasesChangeListener {
            var runningUseCases: Set<UseCase> = emptySet()

            @Suppress("UNUSED_PARAMETER")
            override var useCaseCamera: UseCaseCamera?
                get() = TODO("Not yet implemented")
                set(value) {
                    runningUseCases = value?.runningUseCases ?: emptySet()
                }

            override fun reset() {}

            override fun onRunningUseCasesChanged() {}
        }

        val useCaseManager = createUseCaseManager(setOf(fakeControl))
        val preview = createPreview()
        val useCase = FakeUseCase()

        // Act
        useCaseManager.activate(preview)
        useCaseManager.activate(useCase)
        useCaseManager.attach(listOf(preview, useCase))

        // Assert
        assertThat(fakeControl.runningUseCases).isEqualTo(setOf(preview, useCase))
    }

    @OptIn(ExperimentalCamera2Interop::class)
    @Suppress("UNCHECKED_CAST", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun createUseCaseManager(
        controls: Set<UseCaseCameraControl> = emptySet(),
        useCaseCameraComponentBuilder: FakeUseCaseCameraComponentBuilder =
            FakeUseCaseCameraComponentBuilder(),
    ): UseCaseManager {
        val cameraId = CameraId("0")
        val characteristicsMap: Map<CameraCharacteristics.Key<*>, Any?> = mapOf(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP to streamConfigurationMap,
        )

        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        (Shadow.extract<Any>(
            ApplicationProvider.getApplicationContext<Context>()
                .getSystemService(Context.CAMERA_SERVICE)
        ) as ShadowCameraManager).addCamera("0", characteristics)

        val fakeCameraMetadata = FakeCameraMetadata(
            cameraId = cameraId,
            characteristics = characteristicsMap
        )
        val fakeCamera = FakeCamera()
        val fakeUseCaseThreads = run {
            val executor = MoreExecutors.directExecutor()
            val dispatcher = executor.asCoroutineDispatcher()
            val cameraScope = CoroutineScope(Job() + dispatcher)

            UseCaseThreads(
                cameraScope,
                executor,
                dispatcher,
            )
        }
        return UseCaseManager(
            cameraPipe = CameraPipe(CameraPipe.Config(ApplicationProvider.getApplicationContext())),
            cameraConfig = CameraConfig(cameraId),
            callbackMap = CameraCallbackMap(),
            requestListener = ComboRequestListener(),
            builder = useCaseCameraComponentBuilder,
            controls = controls as java.util.Set<UseCaseCameraControl>,
            cameraProperties = FakeCameraProperties(
                metadata = fakeCameraMetadata,
                cameraId = cameraId,
            ),
            camera2CameraControl = Camera2CameraControl.create(
                FakeCamera2CameraControlCompat(),
                useCaseThreads,
                ComboRequestListener()
            ),
            cameraStateAdapter = CameraStateAdapter(),
            cameraGraphFlags = CameraGraph.Flags(),
            cameraInternal = { fakeCamera },
            cameraQuirks = CameraQuirks(
                fakeCameraMetadata,
                StreamConfigurationMapCompat(null, OutputSizesCorrector(fakeCameraMetadata, null))
            ),
            displayInfoManager = DisplayInfoManager(ApplicationProvider.getApplicationContext()),
            context = ApplicationProvider.getApplicationContext(),
            cameraInfoInternal = { fakeCamera.cameraInfoInternal },
            useCaseThreads = { fakeUseCaseThreads },
        ).also {
            useCaseManagerList.add(it)
        }
    }

    private fun createImageCapture(): ImageCapture =
        ImageCapture.Builder()
            .setCaptureOptionUnpacker { _, _ -> }
            .setSessionOptionUnpacker { _, _, _ -> }
            .build().also {
                it.simulateActivation()
                useCaseList.add(it)
            }

    private fun createPreview(): Preview =
        Preview.Builder()
            .setCaptureOptionUnpacker { _, _ -> }
            .setSessionOptionUnpacker { _, _, _ -> }
            .build().apply {
                setSurfaceProvider(
                    CameraXExecutors.mainThreadExecutor(),
                    SurfaceTextureProvider.createSurfaceTextureProvider()
                )
            }.also {
                it.simulateActivation()
                useCaseList.add(it)
            }

    private fun UseCase.simulateActivation() {
        bindToCamera(
            FakeCamera("0"),
            null,
            getDefaultConfig(
                true,
                CameraUseCaseAdapter(ApplicationProvider.getApplicationContext())
            )
        )
        updateSuggestedStreamSpec(StreamSpec.builder(supportedSizes[0]).build())
    }
}
