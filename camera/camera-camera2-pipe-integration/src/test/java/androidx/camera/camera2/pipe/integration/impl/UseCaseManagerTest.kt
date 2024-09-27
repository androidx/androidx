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
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.hardware.camera2.CameraDevice.TEMPLATE_RECORD
import android.hardware.camera2.CameraMetadata.CONTROL_CAPTURE_INTENT_PREVIEW
import android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE
import android.hardware.camera2.CaptureRequest.CONTROL_CAPTURE_INTENT
import android.hardware.camera2.params.SessionConfiguration.SESSION_HIGH_SPEED
import android.os.Build
import android.util.Range
import android.util.Size
import androidx.camera.camera2.pipe.CameraGraph.OperatingMode.Companion.HIGH_SPEED
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.integration.adapter.BlockingTestDeferrableSurface
import androidx.camera.camera2.pipe.integration.adapter.CameraCoordinatorAdapter
import androidx.camera.camera2.pipe.integration.adapter.CameraStateAdapter
import androidx.camera.camera2.pipe.integration.adapter.CameraUseCaseAdapter
import androidx.camera.camera2.pipe.integration.adapter.FakeTestUseCase
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.adapter.SessionConfigAdapter
import androidx.camera.camera2.pipe.integration.adapter.TestDeferrableSurface
import androidx.camera.camera2.pipe.integration.adapter.ZslControlNoOpImpl
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.CaptureIntentPreviewQuirk
import androidx.camera.camera2.pipe.integration.compat.workaround.NoOpTemplateParamsOverride
import androidx.camera.camera2.pipe.integration.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.pipe.integration.compat.workaround.TemplateParamsOverride
import androidx.camera.camera2.pipe.integration.compat.workaround.TemplateParamsQuirkOverride
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.interop.Camera2CameraControl
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.pipe.integration.testing.FakeCamera2CameraControlCompat
import androidx.camera.camera2.pipe.integration.testing.FakeCameraProperties
import androidx.camera.camera2.pipe.integration.testing.FakeSessionProcessor
import androidx.camera.camera2.pipe.integration.testing.FakeUseCaseCameraComponentBuilder
import androidx.camera.camera2.pipe.testing.FakeCameraBackend
import androidx.camera.camera2.pipe.testing.FakeCameraDevices
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.Quirks
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlin.test.assertFalse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager
import org.robolectric.shadows.StreamConfigurationMapBuilder

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class UseCaseManagerTest {
    private val supportedSizes = arrayOf(Size(640, 480))
    private val streamConfigurationMap =
        StreamConfigurationMapBuilder.newBuilder()
            .apply { supportedSizes.forEach(::addOutputSize) }
            .build()
    private val useCaseManagerList = mutableListOf<UseCaseManager>()
    private val useCaseList = mutableListOf<UseCase>()
    private lateinit var useCaseThreads: UseCaseThreads

    @After
    fun tearDown() = runBlocking {
        useCaseManagerList.forEach { it.close() }
        useCaseList.forEach { it.onUnbind() }
    }

    @Test
    fun enabledUseCasesEmpty_whenUseCaseAttachedOnly() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseManager = createUseCaseManager()
        val useCase = createPreview()

        // Act
        useCaseManager.attach(listOf(useCase))

        // Assert
        val enabledUseCases = useCaseManager.getRunningUseCasesForTest()
        assertThat(enabledUseCases).isEmpty()
    }

    @Test
    fun enabledUseCasesNotEmpty_whenUseCaseEnabled() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseManager = createUseCaseManager()
        val useCase = createPreview()
        useCaseManager.attach(listOf(useCase))

        // Act
        useCaseManager.activate(useCase)

        // Assert
        val enabledUseCases = useCaseManager.getRunningUseCasesForTest()
        assertThat(enabledUseCases).containsExactly(useCase)
    }

    @Test
    fun attachingUseCasesWithSessionProcessor_ShouldSucceed() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseManager = createUseCaseManager()
        val previewUseCase = createFakePreview()
        val imageCaptureUseCase = createFakeImageCapture()

        val fakeSessionProcessor: SessionProcessor = FakeSessionProcessor()

        // Act
        useCaseManager.sessionProcessor = fakeSessionProcessor
        useCaseManager.activate(previewUseCase)
        useCaseManager.activate(imageCaptureUseCase)
        useCaseManager.attach(listOf(previewUseCase, imageCaptureUseCase))
        advanceUntilIdle()

        // Assert
        assertNotNull(useCaseManager.camera)
        assertThat(useCaseManager.getRunningUseCasesForTest())
            .containsExactly(previewUseCase, imageCaptureUseCase)
    }

    @Test
    fun attachingUseCases_ShouldSupersedeUseCasesPendingInitialization() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseManager = createUseCaseManager()
        val previewDeferrableSurface = createBlockingTestDeferrableSurface(Preview::class.java)
        val imageCaptureDeferrableSurface =
            createBlockingTestDeferrableSurface(ImageCapture::class.java)
        val imageAnalysisDeferrableSurface =
            createBlockingTestDeferrableSurface(ImageAnalysis::class.java)
        val previewUseCase = createFakePreview(previewDeferrableSurface)
        val imageCaptureUseCase = createFakeImageCapture(imageCaptureDeferrableSurface)
        val imageAnalysisUseCase = createFakeImageAnalysis(imageAnalysisDeferrableSurface)
        val fakeSessionProcessor = FakeSessionProcessor()

        // Act
        useCaseManager.sessionProcessor = fakeSessionProcessor
        useCaseManager.activate(previewUseCase)
        useCaseManager.activate(imageCaptureUseCase)
        useCaseManager.attach(listOf(previewUseCase, imageCaptureUseCase))
        advanceUntilIdle()
        // Here SessionProcessorProcessor.initialize would stall due to not getting its Surfaces.
        // When initialization is still pending, the current UseCaseCamera should be null (i.e.,
        // no attached or running use cases).
        assertNull(useCaseManager.camera)

        // Attaching an ImageAnalysis use case, which should refresh the attached use cases, and
        // supersede the current set of use cases.
        useCaseManager.activate(imageAnalysisUseCase)
        useCaseManager.attach(listOf(imageAnalysisUseCase))
        // Resume the DeferrableSurfaces to allow them to be retrieved.
        previewDeferrableSurface.resume()
        imageCaptureDeferrableSurface.resume()
        imageAnalysisDeferrableSurface.resume()
        advanceUntilIdle()

        // Assert
        assertNotNull(useCaseManager.camera)
        // Check that the new set of running use cases is Preview, ImageCapture and ImageAnalysis.
        assertThat(useCaseManager.getRunningUseCasesForTest())
            .containsExactly(previewUseCase, imageCaptureUseCase, imageAnalysisUseCase)
    }

    @Test
    fun meteringRepeatingNotEnabled_whenPreviewEnabled() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseManager = createUseCaseManager()
        val preview = createPreview()
        val imageCapture = createImageCapture()
        useCaseManager.attach(listOf(preview, imageCapture))

        // Act
        useCaseManager.activate(preview)
        useCaseManager.activate(imageCapture)

        // Assert
        val enabledUseCases = useCaseManager.getRunningUseCasesForTest()
        assertThat(enabledUseCases).containsExactly(preview, imageCapture)
    }

    @Test
    fun meteringRepeatingEnabled_whenPreviewEnabledWithNoSurfaceProvider() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseManager = createUseCaseManager()
        val preview = createPreview(/* withSurfaceProvider= */ false)
        val imageCapture = createImageCapture()
        useCaseManager.attach(listOf(preview, imageCapture))

        // Act
        useCaseManager.activate(preview)
        useCaseManager.activate(imageCapture)

        // Assert
        val enabledUseCaseClasses =
            useCaseManager.getRunningUseCasesForTest().map { it::class.java }
        assertThat(enabledUseCaseClasses)
            .containsExactly(
                Preview::class.java,
                ImageCapture::class.java,
                MeteringRepeating::class.java
            )
    }

    @Test
    fun meteringRepeatingNotEnabled_whenImageAnalysisAndPreviewWithNoSurfaceProvider() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseManager = createUseCaseManager()
        val preview = createPreview(/* withSurfaceProvider= */ false)
        val imageAnalysis =
            ImageAnalysis.Builder().build().apply {
                setAnalyzer(useCaseThreads.backgroundExecutor) { image -> image.close() }
            }
        useCaseManager.attach(listOf(preview, imageAnalysis))

        // Act
        useCaseManager.activate(preview)
        useCaseManager.activate(imageAnalysis)

        // Assert
        val enabledUseCases = useCaseManager.getRunningUseCasesForTest()
        assertThat(enabledUseCases).containsExactly(preview, imageAnalysis)
    }

    @Test
    fun meteringRepeatingNotEnabled_whenOnlyPreviewWithNoSurfaceProvider() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseManager = createUseCaseManager()
        val preview = createPreview(/* withSurfaceProvider= */ false)
        useCaseManager.attach(listOf(preview))

        // Act
        useCaseManager.activate(preview)

        // Assert
        val enabledUseCases = useCaseManager.getRunningUseCasesForTest()
        assertThat(enabledUseCases).containsExactly(preview)
    }

    @Test
    fun meteringRepeatingEnabled_whenOnlyImageCaptureEnabled() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseManager = createUseCaseManager()
        val imageCapture = createImageCapture()
        useCaseManager.attach(listOf(imageCapture))

        // Act
        useCaseManager.activate(imageCapture)

        // Assert
        val enabledUseCaseClasses =
            useCaseManager.getRunningUseCasesForTest().map { it::class.java }
        assertThat(enabledUseCaseClasses)
            .containsExactly(ImageCapture::class.java, MeteringRepeating::class.java)
    }

    @Test
    fun meteringRepeatingDisabled_whenPreviewBecomesEnabled() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseManager = createUseCaseManager()
        val imageCapture = createImageCapture()
        useCaseManager.attach(listOf(imageCapture))
        useCaseManager.activate(imageCapture)

        // Act
        val preview = createPreview()
        useCaseManager.attach(listOf(preview))
        useCaseManager.activate(preview)

        // Assert
        val activeUseCases = useCaseManager.getRunningUseCasesForTest()
        assertThat(activeUseCases).containsExactly(preview, imageCapture)
    }

    @Test
    fun meteringRepeatingEnabled_afterAllUseCasesButImageCaptureDisabled() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseManager = createUseCaseManager()
        val preview = createPreview()
        val imageCapture = createImageCapture()
        useCaseManager.attach(listOf(preview, imageCapture))
        useCaseManager.activate(preview)
        useCaseManager.activate(imageCapture)

        // Act
        useCaseManager.detach(listOf(preview))

        // Assert
        val enabledUseCaseClasses =
            useCaseManager.getRunningUseCasesForTest().map { it::class.java }
        assertThat(enabledUseCaseClasses)
            .containsExactly(ImageCapture::class.java, MeteringRepeating::class.java)
    }

    @Test
    fun onlyOneUseCaseCameraBuilt_whenAllUseCasesButImageCaptureDisabled() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseCameraBuilder = FakeUseCaseCameraComponentBuilder()
        val useCaseManager =
            createUseCaseManager(useCaseCameraComponentBuilder = useCaseCameraBuilder)

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
    fun meteringRepeatingDisabled_whenAllUseCasesDisabled() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseManager = createUseCaseManager()
        val imageCapture = createImageCapture()
        useCaseManager.attach(listOf(imageCapture))
        useCaseManager.activate(imageCapture)

        // Act
        useCaseManager.deactivate(imageCapture)

        // Assert
        val enabledUseCases = useCaseManager.getRunningUseCasesForTest()
        assertThat(enabledUseCases).isEmpty()
    }

    @Test
    fun onlyOneUseCaseCameraBuilt_whenAllUseCasesDisabled() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseCameraBuilder = FakeUseCaseCameraComponentBuilder()
        val useCaseManager =
            createUseCaseManager(useCaseCameraComponentBuilder = useCaseCameraBuilder)

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
    fun onStateAttachedInvokedExactlyOnce_whenUseCaseAttachedAndMeteringRepeatingAdded() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseManager = createUseCaseManager()
        val imageCapture = createImageCapture()
        val useCase = FakeUseCase().also { it.simulateActivation() }

        // Act
        useCaseManager.activate(imageCapture)
        useCaseManager.activate(useCase)
        useCaseManager.attach(listOf(imageCapture, useCase))

        // Assert
        assertThat(useCase.stateAttachedCount).isEqualTo(1)
    }

    @Test
    fun onStateAttachedInvokedExactlyOnce_whenUseCaseAttachedAndMeteringRepeatingNotAdded() =
        runTest {
            // Arrange
            initializeUseCaseThreads(this)
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
    fun controlsNotified_whenRunningUseCasesChanged() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val fakeControl =
            object : UseCaseCameraControl, UseCaseManager.RunningUseCasesChangeListener {
                var runningUseCaseSet: Set<UseCase> = emptySet()

                override var requestControl: UseCaseCameraRequestControl?
                    get() = TODO("Not yet implemented")
                    set(_) {}

                override fun reset() {}

                override fun onRunningUseCasesChanged(runningUseCases: Set<UseCase>) {
                    runningUseCaseSet = runningUseCases
                }
            }

        val useCaseManager = createUseCaseManager(controls = setOf(fakeControl))
        val preview = createPreview()
        val useCase = FakeUseCase()

        // Act
        useCaseManager.activate(preview)
        useCaseManager.activate(useCase)
        useCaseManager.attach(listOf(preview, useCase))

        // Assert
        assertThat(fakeControl.runningUseCaseSet).isEqualTo(setOf(preview, useCase))
    }

    @Test
    fun useCasesNotifiedOnCameraControlReady_whenAttachingWithSessionProcessor() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseManager = createUseCaseManager()
        val previewUseCase = createFakePreview()
        val imageCaptureUseCase = createFakeImageCapture()

        val fakeSessionProcessor: SessionProcessor = FakeSessionProcessor()

        useCaseManager.sessionProcessor = fakeSessionProcessor
        useCaseManager.activate(previewUseCase)
        useCaseManager.activate(imageCaptureUseCase)
        useCaseManager.attach(listOf(previewUseCase, imageCaptureUseCase))
        advanceUntilIdle()

        assertNotNull(useCaseManager.camera)
        assertThat(useCaseManager.getRunningUseCasesForTest())
            .containsExactly(previewUseCase, imageCaptureUseCase)
        assertTrue(previewUseCase.cameraControlReady)
        assertTrue(imageCaptureUseCase.cameraControlReady)
    }

    @Test
    fun allUseCasesNotifiedOnCameraControlReady_whenSessionProcessorPending() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseManager = createUseCaseManager()
        val previewDeferrableSurface = createBlockingTestDeferrableSurface(Preview::class.java)
        val imageCaptureDeferrableSurface =
            createBlockingTestDeferrableSurface(ImageCapture::class.java)
        val imageAnalysisDeferrableSurface =
            createBlockingTestDeferrableSurface(ImageAnalysis::class.java)
        val previewUseCase = createFakePreview(previewDeferrableSurface)
        val imageCaptureUseCase = createFakeImageCapture(imageCaptureDeferrableSurface)
        val imageAnalysisUseCase = createFakeImageAnalysis(imageAnalysisDeferrableSurface)
        val fakeSessionProcessor = FakeSessionProcessor()

        // Act
        useCaseManager.sessionProcessor = fakeSessionProcessor
        useCaseManager.activate(previewUseCase)
        useCaseManager.activate(imageCaptureUseCase)
        useCaseManager.attach(listOf(previewUseCase, imageCaptureUseCase))
        advanceUntilIdle()
        // Here SessionProcessorProcessor.initialize due to not getting its Surfaces. While we're
        // still initializing, the current UseCaseCamera should be null (i.e., no attached or
        // running use cases).
        // Assert
        assertNull(useCaseManager.camera)
        // We haven't finished initialization, and therefore the controls aren't ready.
        assertFalse(previewUseCase.cameraControlReady)
        assertFalse(imageCaptureUseCase.cameraControlReady)

        // Attaching an ImageAnalysis use case, which should refresh the attached use cases, and
        // supersede the current set of use cases.
        useCaseManager.activate(imageAnalysisUseCase)
        useCaseManager.attach(listOf(imageAnalysisUseCase))
        // Resume the DeferrableSurfaces to allow them to be retrieved.
        previewDeferrableSurface.resume()
        imageCaptureDeferrableSurface.resume()
        imageAnalysisDeferrableSurface.resume()
        advanceUntilIdle()

        // Assert
        assertNotNull(useCaseManager.camera)
        // Check that the new set of running use cases is Preview, ImageCapture and ImageAnalysis.
        assertThat(useCaseManager.getRunningUseCasesForTest())
            .containsExactly(previewUseCase, imageCaptureUseCase, imageAnalysisUseCase)
        // Despite only attaching the ImageAnalysis use case in the prior step. All not-yet-notified
        // use cases should be notified that their camera controls are ready.
        assertTrue(previewUseCase.cameraControlReady)
        assertTrue(imageCaptureUseCase.cameraControlReady)
        assertTrue(imageAnalysisUseCase.cameraControlReady)
    }

    @Test
    fun createCameraGraphConfig_propagateUseCaseConfigToGraphConfig() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseManager = createUseCaseManager()
        val fakeUseCase =
            FakeUseCase().apply {
                updateSessionConfigForTesting(
                    SessionConfig.Builder()
                        .setSessionType(SESSION_HIGH_SPEED)
                        .setTemplateType(TEMPLATE_RECORD)
                        .setImplementationOptions(
                            Camera2ImplConfig.Builder()
                                .setCaptureRequestOption(
                                    CONTROL_CAPTURE_INTENT,
                                    CONTROL_CAPTURE_INTENT_PREVIEW
                                )
                                .build()
                        )
                        .build()
                )
            }
        val sessionConfigAdapter = SessionConfigAdapter(setOf(fakeUseCase))
        val streamConfigMap = mutableMapOf<CameraStream.Config, DeferrableSurface>()

        // Act
        val graphConfig =
            useCaseManager.createCameraGraphConfig(
                sessionConfigAdapter,
                streamConfigMap,
            )

        // Assert
        assertThat(graphConfig.sessionMode).isEqualTo(HIGH_SPEED)
        assertThat(graphConfig.sessionTemplate).isEqualTo(RequestTemplate(TEMPLATE_RECORD))
        assertThat(graphConfig.sessionParameters)
            .isEqualTo(mapOf(CONTROL_CAPTURE_INTENT to CONTROL_CAPTURE_INTENT_PREVIEW))
    }

    @Test
    fun createCameraGraphConfig_setTargetFpsRange() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseManager = createUseCaseManager()
        val fakeUseCase =
            FakeUseCase().apply {
                updateSessionConfigForTesting(
                    SessionConfig.Builder()
                        .setTemplateType(TEMPLATE_PREVIEW)
                        .setExpectedFrameRateRange(Range(15, 24))
                        .build()
                )
            }
        val sessionConfigAdapter = SessionConfigAdapter(setOf(fakeUseCase))
        val streamConfigMap = mutableMapOf<CameraStream.Config, DeferrableSurface>()

        // Act
        val graphConfig =
            useCaseManager.createCameraGraphConfig(
                sessionConfigAdapter,
                streamConfigMap,
            )

        // Assert
        assertThat(graphConfig.sessionTemplate).isEqualTo(RequestTemplate(TEMPLATE_PREVIEW))
        assertThat(graphConfig.sessionParameters).containsKey(CONTROL_AE_TARGET_FPS_RANGE)
        assertThat(graphConfig.sessionParameters[CONTROL_AE_TARGET_FPS_RANGE])
            .isEqualTo(Range(15, 24))
        assertThat(graphConfig.defaultParameters).containsKey(CONTROL_AE_TARGET_FPS_RANGE)
        assertThat(graphConfig.defaultParameters[CONTROL_AE_TARGET_FPS_RANGE])
            .isEqualTo(Range(15, 24))
    }

    @Test
    fun overrideTemplateParams() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseManager =
            createUseCaseManager(
                templateParamsOverride =
                    TemplateParamsQuirkOverride(
                        Quirks(listOf(object : CaptureIntentPreviewQuirk {}))
                    )
            )
        val fakeUseCase =
            FakeUseCase().apply {
                updateSessionConfigForTesting(
                    SessionConfig.Builder().setTemplateType(TEMPLATE_RECORD).build()
                )
            }
        val sessionConfigAdapter = SessionConfigAdapter(setOf(fakeUseCase))
        val streamConfigMap = mutableMapOf<CameraStream.Config, DeferrableSurface>()

        // Act.
        val cameraGraphConfig =
            useCaseManager.createCameraGraphConfig(
                sessionConfigAdapter,
                streamConfigMap,
            )

        // Assert
        assertThat(cameraGraphConfig.sessionParameters[CONTROL_CAPTURE_INTENT])
            .isEqualTo(CONTROL_CAPTURE_INTENT_PREVIEW)
    }

    @OptIn(ExperimentalCamera2Interop::class)
    @Suppress("UNCHECKED_CAST", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun createUseCaseManager(
        controls: Set<UseCaseCameraControl> = emptySet(),
        useCaseCameraComponentBuilder: FakeUseCaseCameraComponentBuilder =
            FakeUseCaseCameraComponentBuilder(),
        templateParamsOverride: TemplateParamsOverride = NoOpTemplateParamsOverride,
    ): UseCaseManager {
        val cameraId = CameraId("0")
        val characteristicsMap: Map<CameraCharacteristics.Key<*>, Any?> =
            mapOf(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP to streamConfigurationMap,
            )

        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        (Shadow.extract<Any>(
                ApplicationProvider.getApplicationContext<Context>()
                    .getSystemService(Context.CAMERA_SERVICE)
            ) as ShadowCameraManager)
            .addCamera("0", characteristics)

        val fakeCameraMetadata =
            FakeCameraMetadata(cameraId = cameraId, characteristics = characteristicsMap)
        val fakeCamera = FakeCamera()
        val cameraPipe = CameraPipe(CameraPipe.Config(ApplicationProvider.getApplicationContext()))
        val fakeCameraBackend = FakeCameraBackend(mapOf(cameraId to fakeCameraMetadata))
        return UseCaseManager(
                cameraPipe = cameraPipe,
                cameraDevices =
                    FakeCameraDevices(
                        fakeCameraBackend.id,
                        emptySet(),
                        mapOf(fakeCameraBackend.id to listOf(fakeCameraMetadata))
                    ),
                cameraCoordinator = CameraCoordinatorAdapter(cameraPipe, cameraPipe.cameras()),
                callbackMap = CameraCallbackMap(),
                requestListener = ComboRequestListener(),
                cameraConfig = CameraConfig(cameraId),
                builder = useCaseCameraComponentBuilder,
                cameraControl = fakeCamera.cameraControlInternal,
                zslControl = ZslControlNoOpImpl(),
                controls = controls as java.util.Set<UseCaseCameraControl>,
                camera2CameraControl =
                    Camera2CameraControl.create(
                        FakeCamera2CameraControlCompat(),
                        checkNotNull(useCaseThreads),
                        ComboRequestListener()
                    ),
                cameraStateAdapter = CameraStateAdapter(),
                cameraQuirks =
                    CameraQuirks(
                        fakeCameraMetadata,
                        StreamConfigurationMapCompat(
                            null,
                            OutputSizesCorrector(fakeCameraMetadata, null)
                        )
                    ),
                cameraInternal = { fakeCamera },
                useCaseThreads = { useCaseThreads },
                cameraInfoInternal = { fakeCamera.cameraInfoInternal },
                templateParamsOverride = templateParamsOverride,
                context = ApplicationProvider.getApplicationContext(),
                cameraProperties =
                    FakeCameraProperties(
                        metadata = fakeCameraMetadata,
                        cameraId = cameraId,
                    ),
                displayInfoManager =
                    DisplayInfoManager(ApplicationProvider.getApplicationContext()),
            )
            .also { useCaseManagerList.add(it) }
    }

    private fun initializeUseCaseThreads(testScope: TestScope) {
        val dispatcher = StandardTestDispatcher(testScope.testScheduler)
        useCaseThreads =
            UseCaseThreads(
                testScope,
                dispatcher.asExecutor(),
                dispatcher,
            )
    }

    private fun createFakePreview(customDeferrableSurface: DeferrableSurface? = null) =
        createFakeTestUseCase(
            "Preview",
            TEMPLATE_PREVIEW,
            Preview::class.java,
            customDeferrableSurface,
        )

    private fun createFakeImageCapture(customDeferrableSurface: DeferrableSurface? = null) =
        createFakeTestUseCase(
            "ImageCapture",
            CameraDevice.TEMPLATE_STILL_CAPTURE,
            ImageCapture::class.java,
            customDeferrableSurface,
        )

    private fun createFakeImageAnalysis(customDeferrableSurface: DeferrableSurface? = null) =
        createFakeTestUseCase(
            "ImageAnalysis",
            TEMPLATE_PREVIEW,
            ImageAnalysis::class.java,
            customDeferrableSurface,
        )

    private fun <T> createFakeTestUseCase(
        name: String,
        template: Int,
        containerClass: Class<T>,
        customDeferrableSurface: DeferrableSurface? = null,
    ): FakeTestUseCase {
        val deferrableSurface =
            customDeferrableSurface ?: createTestDeferrableSurface(containerClass)
        return FakeTestUseCase(FakeUseCaseConfig.Builder().setTargetName(name).useCaseConfig)
            .apply {
                setupSessionConfig(
                    SessionConfig.Builder().also { sessionConfigBuilder ->
                        sessionConfigBuilder.setTemplateType(template)
                        sessionConfigBuilder.addSurface(deferrableSurface)
                    }
                )
            }
    }

    private fun <T> createTestDeferrableSurface(containerClass: Class<T>): TestDeferrableSurface {
        return TestDeferrableSurface().apply {
            setContainerClass(containerClass)
            terminationFuture.addListener({ cleanUp() }, useCaseThreads.backgroundExecutor)
        }
    }

    private fun <T> createBlockingTestDeferrableSurface(
        containerClass: Class<T>
    ): BlockingTestDeferrableSurface {
        return BlockingTestDeferrableSurface().apply {
            setContainerClass(containerClass)
            terminationFuture.addListener({ cleanUp() }, useCaseThreads.backgroundExecutor)
        }
    }

    private fun createImageCapture(): ImageCapture =
        ImageCapture.Builder()
            .setCaptureOptionUnpacker(CameraUseCaseAdapter.DefaultCaptureOptionsUnpacker.INSTANCE)
            .setSessionOptionUnpacker(CameraUseCaseAdapter.DefaultSessionOptionsUnpacker)
            .build()
            .also {
                it.simulateActivation()
                useCaseList.add(it)
            }

    private fun createPreview(withSurfaceProvider: Boolean = true): Preview =
        Preview.Builder()
            .setCaptureOptionUnpacker(CameraUseCaseAdapter.DefaultCaptureOptionsUnpacker.INSTANCE)
            .setSessionOptionUnpacker(CameraUseCaseAdapter.DefaultSessionOptionsUnpacker)
            .build()
            .apply {
                if (withSurfaceProvider) {
                    setSurfaceProvider(
                        CameraXExecutors.mainThreadExecutor(),
                        SurfaceTextureProvider.createSurfaceTextureProvider()
                    )
                }
            }
            .also {
                it.simulateActivation()
                useCaseList.add(it)
            }

    private fun UseCase.simulateActivation() {
        bindToCamera(
            FakeCamera("0"),
            null,
            null,
            getDefaultConfig(
                true,
                CameraUseCaseAdapter(ApplicationProvider.getApplicationContext())
            )
        )
        updateSuggestedStreamSpec(StreamSpec.builder(supportedSizes[0]).build(), null)
    }
}
