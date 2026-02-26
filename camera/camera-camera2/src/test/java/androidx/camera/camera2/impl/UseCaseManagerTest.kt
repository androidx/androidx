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

package androidx.camera.camera2.impl

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.hardware.camera2.CameraDevice.TEMPLATE_RECORD
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY
import android.hardware.camera2.CameraMetadata.CONTROL_CAPTURE_INTENT_PREVIEW
import android.hardware.camera2.CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT
import android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE
import android.hardware.camera2.CaptureRequest.CONTROL_CAPTURE_INTENT
import android.hardware.camera2.params.DynamicRangeProfiles
import android.hardware.camera2.params.SessionConfiguration.SESSION_HIGH_SPEED
import android.util.Range
import android.util.Size
import androidx.camera.camera2.adapter.CameraCoordinatorAdapter
import androidx.camera.camera2.adapter.CameraStateAdapter
import androidx.camera.camera2.adapter.CameraUseCaseAdapter
import androidx.camera.camera2.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.adapter.SessionConfigAdapter
import androidx.camera.camera2.adapter.TestDeferrableSurface
import androidx.camera.camera2.adapter.ZslControlNoOpImpl
import androidx.camera.camera2.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.compat.quirk.CameraQuirks
import androidx.camera.camera2.compat.quirk.CaptureIntentPreviewQuirk
import androidx.camera.camera2.compat.workaround.NoOpAutoFlashAEModeDisabler
import androidx.camera.camera2.compat.workaround.NoOpTemplateParamsOverride
import androidx.camera.camera2.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.compat.workaround.TemplateParamsOverride
import androidx.camera.camera2.compat.workaround.TemplateParamsQuirkOverride
import androidx.camera.camera2.config.CameraConfig
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.interop.setCamera2CaptureRequestConfigurator
import androidx.camera.camera2.pipe.CameraGraph.OperatingMode.Companion.HIGH_SPEED
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.OutputStream.DynamicRangeProfile
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.testing.FakeCamera2CameraControlCompat
import androidx.camera.camera2.testing.FakeUseCaseCameraComponentBuilder
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.impl.Quirks
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeEncoderProfilesProvider
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.test.core.app.ApplicationProvider
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.StreamConfigurationMapBuilder

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class UseCaseManagerTest {
    private val supportedSizes = arrayOf(Size(640, 480))
    private val streamConfigurationMap =
        StreamConfigurationMapBuilder.newBuilder()
            .apply { supportedSizes.forEach(::addOutputSize) }
            .build()
    private val useCaseManagerList = mutableListOf<UseCaseManager>()
    private val useCaseList = mutableListOf<UseCase>()
    private lateinit var useCaseThreads: UseCaseThreads
    private lateinit var lowLightBoostControl: LowLightBoostControl

    @After
    fun tearDown() = runBlocking {
        useCaseManagerList.forEach { it.close() }
        useCaseList.forEach { it.onUnbind() }
        DisplayInfoManager.releaseInstance()

        // Drains the main looper's queue to ensure all CameraStateAdapter updates are processed.
        ShadowLooper.idleMainLooper()
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
                MeteringRepeating::class.java,
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
    fun onSessionStartInvokedExactlyOnce_whenUseCaseAttachedAndMeteringRepeatingAdded() = runTest {
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
    fun onSessionStopInvokedExactlyOnce_whenUseCaseAttachedAndMeteringRepeatingNotAdded() =
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
                                    CONTROL_CAPTURE_INTENT_PREVIEW,
                                )
                                .build()
                        )
                        .build()
                )
            }
        val sessionConfigAdapter = SessionConfigAdapter(setOf(fakeUseCase))

        // Act
        val graphConfig =
            useCaseManager.createUseCaseCameraConfig(sessionConfigAdapter, null).cameraGraphConfig

        // Assert
        assertThat(graphConfig.sessionMode).isEqualTo(HIGH_SPEED)
        assertThat(graphConfig.sessionTemplate).isEqualTo(RequestTemplate(TEMPLATE_RECORD))
        assertThat(graphConfig.sessionParameters)
            .isEqualTo(mapOf(CONTROL_CAPTURE_INTENT to CONTROL_CAPTURE_INTENT_PREVIEW))
    }

    @Config(maxSdk = 32)
    @Test
    fun createCameraGraphConfig_underTiramisu_notSetDynamicRangeToGraphConfig() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseManager = createUseCaseManager()
        val previewDeferrableSurface = createTestDeferrableSurface(Preview::class.java)
        val outputConfig =
            SessionConfig.OutputConfig.builder(previewDeferrableSurface)
                .setDynamicRange(DynamicRange.SDR)
                .build()
        val fakeUseCase =
            FakeUseCase().apply {
                updateSessionConfigForTesting(
                    SessionConfig.Builder()
                        .setTemplateType(TEMPLATE_PREVIEW)
                        .addOutputConfig(outputConfig)
                        .build()
                )
            }
        val sessionConfigAdapter = SessionConfigAdapter(setOf(fakeUseCase))

        // Act
        val graphConfig =
            useCaseManager.createUseCaseCameraConfig(sessionConfigAdapter, null).cameraGraphConfig

        // Assert
        assertThat(graphConfig.streams.size).isEqualTo(1)
        val streamConfig = graphConfig.streams[0]
        assertThat(streamConfig.outputs.size).isEqualTo(1)
        val dynamicRangeProfile = streamConfig.outputs[0].dynamicRangeProfile
        assertThat(dynamicRangeProfile).isEqualTo(null)
    }

    @Config(minSdk = 33)
    @Test
    fun createCameraGraphConfig_propagateDynamicRangeSdrToGraphConfig() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseManager = createUseCaseManager()
        val previewDeferrableSurface = createTestDeferrableSurface(Preview::class.java)
        val outputConfig =
            SessionConfig.OutputConfig.builder(previewDeferrableSurface)
                .setDynamicRange(DynamicRange.SDR)
                .build()
        val fakeUseCase =
            FakeUseCase().apply {
                updateSessionConfigForTesting(
                    SessionConfig.Builder()
                        .setTemplateType(TEMPLATE_PREVIEW)
                        .addOutputConfig(outputConfig)
                        .build()
                )
            }
        val sessionConfigAdapter = SessionConfigAdapter(setOf(fakeUseCase))

        // Act
        val graphConfig =
            useCaseManager.createUseCaseCameraConfig(sessionConfigAdapter, null).cameraGraphConfig

        // Assert
        assertThat(graphConfig.streams.size).isEqualTo(1)
        val streamConfig = graphConfig.streams[0]
        assertThat(streamConfig.outputs.size).isEqualTo(1)
        val dynamicRangeProfile = streamConfig.outputs[0].dynamicRangeProfile
        assertThat(dynamicRangeProfile).isEqualTo(DynamicRangeProfile.STANDARD)
    }

    @Config(minSdk = 33)
    @Test
    fun createCameraGraphConfig_propagateDynamicRangeHlg10ToGraphConfig() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseManager =
            createUseCaseManager(
                characteristicsMap =
                    mapOf(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP to
                            streamConfigurationMap,
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES to
                            intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
                        REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES to
                            DynamicRangeProfiles(
                                longArrayOf(
                                    DynamicRangeProfiles.HLG10,
                                    DynamicRangeProfiles.HLG10,
                                    0L,
                                )
                            ),
                    )
            )
        val previewDeferrableSurface = createTestDeferrableSurface(Preview::class.java)
        val outputConfig =
            SessionConfig.OutputConfig.builder(previewDeferrableSurface)
                .setDynamicRange(DynamicRange.HLG_10_BIT)
                .build()
        val fakeUseCase =
            FakeUseCase().apply {
                updateSessionConfigForTesting(
                    SessionConfig.Builder()
                        .setTemplateType(TEMPLATE_PREVIEW)
                        .addOutputConfig(outputConfig)
                        .build()
                )
            }
        val sessionConfigAdapter = SessionConfigAdapter(setOf(fakeUseCase))

        // Act
        val graphConfig =
            useCaseManager.createUseCaseCameraConfig(sessionConfigAdapter, null).cameraGraphConfig

        // Assert
        assertThat(graphConfig.streams.size).isEqualTo(1)
        val streamConfig = graphConfig.streams[0]
        assertThat(streamConfig.outputs.size).isEqualTo(1)
        val dynamicRangeProfile = streamConfig.outputs[0].dynamicRangeProfile
        assertThat(dynamicRangeProfile).isEqualTo(DynamicRangeProfile.HLG10)
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

        // Act
        val graphConfig =
            useCaseManager.createUseCaseCameraConfig(sessionConfigAdapter, null).cameraGraphConfig

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

        // Act.
        val cameraGraphConfig =
            useCaseManager.createUseCaseCameraConfig(sessionConfigAdapter, null).cameraGraphConfig

        // Assert
        assertThat(cameraGraphConfig.sessionParameters[CONTROL_CAPTURE_INTENT])
            .isEqualTo(CONTROL_CAPTURE_INTENT_PREVIEW)
    }

    @Test
    @Config(minSdk = 35)
    fun enableLowLightBoost_whenFpsRangeExceed30() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseManager =
            createUseCaseManager(
                characteristicsMap =
                    mapOf(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP to
                            streamConfigurationMap,
                        CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES to
                            intArrayOf(CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY),
                    )
            )
        val fakeUseCase =
            FakeUseCase().apply {
                updateSessionConfigForTesting(
                    SessionConfig.Builder()
                        .setTemplateType(TEMPLATE_PREVIEW)
                        .setExpectedFrameRateRange(Range(30, 60))
                        .build()
                )
            }
        useCaseManager.attach(listOf(fakeUseCase))

        assertThrows<IllegalStateException> {
            lowLightBoostControl.setLowLightBoostAsync(true).await()
        }
    }

    @Test
    fun cameraXConfig_camera2CaptureRequestConfiguratorCalled() = runTest {
        // Arrange.
        initializeUseCaseThreads(this)
        val fpsRange = Range(15, 15)
        lateinit var resultFpsRange: Range<Int>
        val useCaseManager =
            createUseCaseManager(
                cameraXConfig =
                    CameraXConfig.Builder()
                        .setCamera2CaptureRequestConfigurator { parameters ->
                            parameters.forEach { (key, value) ->
                                if (key == CONTROL_AE_TARGET_FPS_RANGE) {
                                    @Suppress("UNCHECKED_CAST")
                                    resultFpsRange = value as Range<Int>
                                }
                            }
                        }
                        .build()
            )
        val fakeUseCase =
            FakeUseCase().apply {
                updateSessionConfigForTesting(
                    SessionConfig.Builder()
                        .setTemplateType(TEMPLATE_PREVIEW)
                        .setExpectedFrameRateRange(fpsRange)
                        .build()
                )
            }
        val sessionConfigAdapter = SessionConfigAdapter(setOf(fakeUseCase))

        // Act.
        useCaseManager.createUseCaseCameraConfig(sessionConfigAdapter, null).cameraGraphConfig

        // Assert.
        assertThat(resultFpsRange).isEqualTo(fpsRange)
    }

    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
    @Suppress("UNCHECKED_CAST", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun createUseCaseManager(
        controls: Set<UseCaseCameraControl> = emptySet(),
        useCaseCameraComponentBuilder: FakeUseCaseCameraComponentBuilder =
            FakeUseCaseCameraComponentBuilder(),
        templateParamsOverride: TemplateParamsOverride = NoOpTemplateParamsOverride,
        characteristicsMap: Map<CameraCharacteristics.Key<*>, Any?> =
            mapOf(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP to streamConfigurationMap),
        cameraXConfig: CameraXConfig? = null,
    ): UseCaseManager {
        val cameraId = CameraId("0")

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
        val cameraProperties =
            CameraPipeCameraProperties(CameraConfig(cameraId), fakeCameraMetadata)
        lowLightBoostControl =
            LowLightBoostControl(
                fakeCameraMetadata,
                State3AControl(cameraProperties, NoOpAutoFlashAEModeDisabler, useCaseThreads),
                useCaseThreads,
                ComboRequestListener(),
            )
        val cameraQuirks =
            CameraQuirks(
                fakeCameraMetadata,
                StreamConfigurationMapCompat(null, OutputSizesCorrector(fakeCameraMetadata, null)),
            )
        val configProvider =
            CameraGraphConfigProvider(
                callbackMap = CameraCallbackMap(),
                requestListener = ComboRequestListener(),
                cameraConfig = CameraConfig(cameraId),
                cameraQuirks = cameraQuirks,
                zslControl = ZslControlNoOpImpl(),
                templateParamsOverride = templateParamsOverride,
                cameraMetadata = fakeCameraMetadata,
                cameraXConfig = cameraXConfig,
            )
        return UseCaseManager(
                cameraPipe = cameraPipe,
                cameraCoordinator = CameraCoordinatorAdapter(cameraPipe, cameraPipe.cameras()),
                builder = useCaseCameraComponentBuilder,
                zslControl = ZslControlNoOpImpl(),
                lowLightBoostControl = lowLightBoostControl,
                controls = controls as java.util.Set<UseCaseCameraControl>,
                camera2CameraControl =
                    Camera2CameraControl.create(
                        FakeCamera2CameraControlCompat(),
                        checkNotNull(useCaseThreads),
                        ComboRequestListener(),
                    ),
                cameraStateAdapter = CameraStateAdapter(),
                cameraInternal = { fakeCamera },
                useCaseThreads = { useCaseThreads },
                cameraInfoInternal = { fakeCamera.cameraInfoInternal },
                encoderProfilesProvider = FakeEncoderProfilesProvider.Builder().build(),
                context = ApplicationProvider.getApplicationContext(),
                cameraProperties = cameraProperties,
                displayInfoManager =
                    DisplayInfoManager.getInstance(ApplicationProvider.getApplicationContext()),
                cameraXConfig = cameraXConfig ?: CameraXConfig.Builder().build(),
                cameraGraphConfigProvider = configProvider,
            )
            .also { useCaseManagerList.add(it) }
    }

    private fun initializeUseCaseThreads(testScope: TestScope) {
        val dispatcher = StandardTestDispatcher(testScope.testScheduler)
        useCaseThreads = UseCaseThreads(testScope, dispatcher.asExecutor(), dispatcher)
    }

    private fun <T> createTestDeferrableSurface(containerClass: Class<T>): TestDeferrableSurface {
        return TestDeferrableSurface().apply {
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
                        SurfaceTextureProvider.createSurfaceTextureProvider(),
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
                CameraUseCaseAdapter(ApplicationProvider.getApplicationContext()),
            ),
        )
        updateSuggestedStreamSpec(StreamSpec.builder(supportedSizes[0]).build(), null)
    }
}
