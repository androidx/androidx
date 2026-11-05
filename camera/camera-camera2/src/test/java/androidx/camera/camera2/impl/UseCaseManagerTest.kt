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
@file:Suppress("DEPRECATION")

package androidx.camera.camera2.impl

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES
import android.hardware.camera2.CameraDevice.TEMPLATE_MANUAL
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.hardware.camera2.CameraDevice.TEMPLATE_RECORD
import android.hardware.camera2.CameraDevice.TEMPLATE_STILL_CAPTURE
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY
import android.hardware.camera2.CameraMetadata.CONTROL_CAPTURE_INTENT_PREVIEW
import android.hardware.camera2.CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE
import android.hardware.camera2.CaptureRequest.CONTROL_CAPTURE_INTENT
import android.hardware.camera2.params.DynamicRangeProfiles
import android.hardware.camera2.params.SessionConfiguration.SESSION_HIGH_SPEED
import android.media.MediaCodec
import android.os.Build
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.adapter.CameraCoordinatorAdapter
import androidx.camera.camera2.adapter.CameraInternalAdapter
import androidx.camera.camera2.adapter.CameraSessionLifecycleAdapter
import androidx.camera.camera2.adapter.CameraStateAdapter
import androidx.camera.camera2.adapter.CameraUseCaseAdapter
import androidx.camera.camera2.adapter.CaptureConfigAdapter
import androidx.camera.camera2.adapter.GraphStateToCameraStateAdapter
import androidx.camera.camera2.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.adapter.SessionConfigAdapter
import androidx.camera.camera2.adapter.SupportedSurfaceCombination
import androidx.camera.camera2.adapter.TestDeferrableSurface
import androidx.camera.camera2.adapter.ZslControl
import androidx.camera.camera2.adapter.ZslControlNoOpImpl
import androidx.camera.camera2.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.compat.quirk.CameraQuirks
import androidx.camera.camera2.compat.quirk.CaptureIntentPreviewQuirk
import androidx.camera.camera2.compat.workaround.ExtraSupportedSurfaceCombinationsContainer
import androidx.camera.camera2.compat.workaround.NoOpAutoFlashAEModeDisabler
import androidx.camera.camera2.compat.workaround.NoOpTemplateParamsOverride
import androidx.camera.camera2.compat.workaround.TemplateParamsOverride
import androidx.camera.camera2.compat.workaround.TemplateParamsQuirkOverride
import androidx.camera.camera2.config.CameraConfig
import androidx.camera.camera2.config.UseCaseCameraComponent
import androidx.camera.camera2.config.UseCaseCameraConfig
import androidx.camera.camera2.config.UseCaseCameraContext
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.interop.camera2Interop
import androidx.camera.camera2.interop.setCamera2CaptureRequestConfigurator
import androidx.camera.camera2.pipe.CameraGraph.OperatingMode.Companion.HIGH_SPEED
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.OutputStream.DynamicRangeProfile
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.HighEndDeviceTemplate
import androidx.camera.camera2.testing.FakeCamera2CameraControlCompat
import androidx.camera.camera2.testing.FakeCameraGraph
import androidx.camera.camera2.testing.FakeCameraProperties
import androidx.camera.camera2.testing.FakeCapturePipeline
import androidx.camera.camera2.testing.FakeUseCaseCameraComponentBuilder
import androidx.camera.camera2.testing.FakeUseCaseSurfaceManager
import androidx.camera.common.CameraFrameNumber
import androidx.camera.common.CameraId
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraXConfig
import androidx.camera.core.CompositionSettings
import androidx.camera.core.DynamicRange
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.featuregroup.impl.FeatureCombinationQuery
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.CameraConfigs
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.ImmediateSurface
import androidx.camera.core.impl.Quirks
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.impl.stabilization.StabilizationMode
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.StreamSpecsCalculatorImpl
import androidx.camera.core.processing.DefaultSurfaceProcessor
import androidx.camera.core.streamsharing.StreamSharing
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraControl
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.GarbageCollectionUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator
import androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.impl.fakes.FakeEncoderProfilesProvider
import androidx.camera.testing.impl.fakes.FakeSurfaceProcessorInternal
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.test.core.app.ApplicationProvider
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.lang.ref.WeakReference
import javax.inject.Provider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
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
    private val adaptersToDetach =
        mutableListOf<androidx.camera.core.internal.CameraUseCaseAdapter>()
    private val surfaceProcessorsToCleanUp = mutableListOf<FakeSurfaceProcessorInternal>()
    private val testSurfacesToClose = mutableListOf<DeferrableSurface>()
    private val useCaseManagerList = mutableListOf<UseCaseManager>()
    private val useCaseList = mutableListOf<UseCase>()
    private lateinit var useCaseThreads: UseCaseThreads
    private lateinit var lowLightBoostControl: LowLightBoostControl

    @After
    fun tearDown() = runBlocking {
        adaptersToDetach.forEach { it.removeAllUseCases() }
        surfaceProcessorsToCleanUp.forEach { it.cleanUp() }
        testSurfacesToClose.forEach { it.close() }
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
            createUseCaseManager(
                useCaseCameraComponentBuilderProvider =
                    Provider<UseCaseCameraComponent.Builder> { useCaseCameraBuilder }
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
            createUseCaseManager(
                useCaseCameraComponentBuilderProvider =
                    Provider<UseCaseCameraComponent.Builder> { useCaseCameraBuilder }
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

    @Test
    fun createCameraGraphConfig_propagatesSessionConfigInteropToGraphConfig() = runTest {
        // Arrange
        initializeUseCaseThreads(this)
        val useCaseManager = createUseCaseManager()
        val fakeUseCase = FakeUseCase()

        val testSurface = TestDeferrableSurface()
        val sessionConfigBuilder = SessionConfig.Builder()
        sessionConfigBuilder.addSurface(testSurface)
        sessionConfigBuilder.setTemplateType(TEMPLATE_PREVIEW)
        sessionConfigBuilder.setSessionType(SESSION_HIGH_SPEED)

        val mutableConfig = androidx.camera.core.impl.MutableOptionsBundle.create()
        val opt =
            androidx.camera.core.impl.Config.Option.create<Any>(
                "camera2.sessionParameter.option." +
                    android.hardware.camera2.CaptureRequest.CONTROL_EFFECT_MODE.name,
                Any::class.java,
                android.hardware.camera2.CaptureRequest.CONTROL_EFFECT_MODE,
            )
        mutableConfig.insertOption(
            opt,
            androidx.camera.core.impl.Config.OptionPriority.ALWAYS_OVERRIDE,
            android.hardware.camera2.CaptureRequest.CONTROL_EFFECT_MODE_MONO,
        )
        mutableConfig.insertOption(Camera2ImplConfig.SESSION_COLOR_SPACE_OPTION, 0)
        sessionConfigBuilder.addImplementationOptions(mutableConfig)

        val sessionConfig = sessionConfigBuilder.build()
        fakeUseCase.updateSessionConfigForTesting(sessionConfig)
        val sessionConfigAdapter = SessionConfigAdapter(setOf(fakeUseCase))

        // Act
        val graphConfig =
            useCaseManager.createUseCaseCameraConfig(sessionConfigAdapter, null).cameraGraphConfig

        // Assert
        assertThat(graphConfig.sessionMode)
            .isEqualTo(androidx.camera.camera2.pipe.CameraGraph.OperatingMode.HIGH_SPEED)
        assertThat(
                graphConfig.sessionParameters[
                        android.hardware.camera2.CaptureRequest.CONTROL_EFFECT_MODE]
            )
            .isEqualTo(android.hardware.camera2.CaptureRequest.CONTROL_EFFECT_MODE_MONO)
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            assertThat(graphConfig.sessionColorSpace)
                .isEqualTo(androidx.camera.camera2.pipe.CameraColorSpace.SRGB)
        }
        testSurface.close()
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

    @Config(minSdk = 33)
    @Test
    fun meteringRepeatingEnabled_withHlg10VideoOnly_resolvesMeteringRepeatingToHlg10() = runTest {
        // Arrange: Configure HLG10 constrained to HLG10 only (matching Pixel 7+ behavior)
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
        val videoSurface = createTestDeferrableSurface(MediaCodec::class.java)
        val fakeVideoUseCase =
            FakeUseCase().apply {
                bindToCamera(
                    FakeCamera("0"),
                    null,
                    null,
                    getDefaultConfig(
                        true,
                        CameraUseCaseAdapter(ApplicationProvider.getApplicationContext()),
                    ),
                )
                updateSuggestedStreamSpec(
                    StreamSpec.builder(supportedSizes[0])
                        .setDynamicRange(DynamicRange.HLG_10_BIT)
                        .build(),
                    null,
                )
                updateSessionConfigForTesting(
                    SessionConfig.Builder()
                        .setTemplateType(TEMPLATE_RECORD)
                        .addSurface(videoSurface, DynamicRange.HLG_10_BIT)
                        .build()
                )
                useCaseList.add(this)
            }

        // Act
        useCaseManager.attach(listOf(fakeVideoUseCase))
        useCaseManager.activate(fakeVideoUseCase)

        // Assert
        val runningUseCases = useCaseManager.getRunningUseCasesForTest()
        val meteringRepeating = runningUseCases.filterIsInstance<MeteringRepeating>().single()
        assertThat(meteringRepeating.attachedStreamSpec?.dynamicRange)
            .isEqualTo(DynamicRange.HLG_10_BIT)
        assertThat(meteringRepeating.sessionConfig.outputConfigs.single().dynamicRange)
            .isEqualTo(DynamicRange.HLG_10_BIT)

        val graphConfig =
            useCaseManager
                .createUseCaseCameraConfig(SessionConfigAdapter(runningUseCases), null)
                .cameraGraphConfig
        assertThat(graphConfig.streams).hasSize(2)
        for (stream in graphConfig.streams) {
            assertThat(stream.outputs.single().dynamicRangeProfile)
                .isEqualTo(DynamicRangeProfile.HLG10)
        }
        videoSurface.close()
    }

    @Config(minSdk = 33)
    @Test
    fun meteringRepeatingDynamicRangeUpdates_whenHlg10AttachedWhileMeteringAttached() = runTest {
        // Arrange: Configure HLG10 unconstrained (supports STANDARD | HLG10)
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
                                    DynamicRangeProfiles.STANDARD or DynamicRangeProfiles.HLG10,
                                    0L,
                                )
                            ),
                    )
            )
        val imageCapture = createImageCapture()
        useCaseManager.attach(listOf(imageCapture))
        useCaseManager.activate(imageCapture)

        val initialMetering =
            useCaseManager
                .getRunningUseCasesForTest()
                .filterIsInstance<MeteringRepeating>()
                .single()
        assertThat(initialMetering.attachedStreamSpec?.dynamicRange).isEqualTo(DynamicRange.SDR)

        val videoSurface = createTestDeferrableSurface(MediaCodec::class.java)
        val fakeVideoUseCase =
            FakeUseCase().apply {
                bindToCamera(
                    FakeCamera("0"),
                    null,
                    null,
                    getDefaultConfig(
                        true,
                        CameraUseCaseAdapter(ApplicationProvider.getApplicationContext()),
                    ),
                )
                updateSuggestedStreamSpec(
                    StreamSpec.builder(supportedSizes[0])
                        .setDynamicRange(DynamicRange.HLG_10_BIT)
                        .build(),
                    null,
                )
                updateSessionConfigForTesting(
                    SessionConfig.Builder()
                        .setTemplateType(TEMPLATE_RECORD)
                        .addNonRepeatingSurface(videoSurface, DynamicRange.HLG_10_BIT)
                        .build()
                )
                useCaseList.add(this)
            }

        // Act: Attach HLG10 non-repeating use case while MeteringRepeating is already attached
        useCaseManager.attach(listOf(fakeVideoUseCase))
        useCaseManager.activate(fakeVideoUseCase)

        // Assert: MeteringRepeating updates its dynamic range to HLG_10_BIT
        val updatedMetering =
            useCaseManager
                .getRunningUseCasesForTest()
                .filterIsInstance<MeteringRepeating>()
                .single()
        assertThat(updatedMetering.attachedStreamSpec?.dynamicRange)
            .isEqualTo(DynamicRange.HLG_10_BIT)
        assertThat(updatedMetering.sessionConfig.outputConfigs.single().dynamicRange)
            .isEqualTo(DynamicRange.HLG_10_BIT)
        videoSurface.close()
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
        useCaseCameraComponentBuilderProvider: Provider<UseCaseCameraComponent.Builder>? = null,
        templateParamsOverride: TemplateParamsOverride = NoOpTemplateParamsOverride,
        characteristicsMap: Map<CameraCharacteristics.Key<*>, Any?> =
            mapOf(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP to streamConfigurationMap),
        cameraXConfig: CameraXConfig? = null,
        zslControl: ZslControl = ZslControlNoOpImpl(),
    ): UseCaseManager {
        val cameraId = CameraId("0")

        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        (Shadow.extract<Any>(
                ApplicationProvider.getApplicationContext<Context>()
                    .getSystemService(Context.CAMERA_SERVICE)
            ) as ShadowCameraManager)
            .addCamera("0", characteristics)

        val fakeCameraMetadata =
            FakeCameraMetadata.fromTemplate(
                template = HighEndDeviceTemplate,
                cameraId = cameraId,
                characteristicsOverrides = characteristicsMap,
            )
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
            CameraQuirks(fakeCameraMetadata, StreamConfigurationMapCompat(null, fakeCameraMetadata))
        val configProvider =
            CameraGraphConfigProvider(
                callbackMap = CameraCallbackMap(),
                requestListener = ComboRequestListener(),
                cameraConfig = CameraConfig(cameraId),
                cameraQuirks = cameraQuirks,
                zslControl = zslControl,
                templateParamsOverride = templateParamsOverride,
                cameraMetadata = fakeCameraMetadata,
                cameraXConfig = cameraXConfig,
            )
        val builderProvider: Provider<UseCaseCameraComponent.Builder> =
            useCaseCameraComponentBuilderProvider
                ?: Provider<UseCaseCameraComponent.Builder> { FakeUseCaseCameraComponentBuilder() }
        return UseCaseManager(
                cameraPipe = cameraPipe,
                cameraCoordinator = CameraCoordinatorAdapter(cameraPipe, cameraPipe.cameras()),
                builder = builderProvider,
                zslControl = zslControl,
                lowLightBoostControl = lowLightBoostControl,
                controls = controls as java.util.Set<UseCaseCameraControl>,
                camera2CameraControl =
                    Camera2CameraControl.create(
                        FakeCamera2CameraControlCompat(),
                        checkNotNull(useCaseThreads),
                        ComboRequestListener(),
                    ),
                cameraStateAdapter = CameraStateAdapter(CameraSessionLifecycleAdapter()),
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
                supportedSurfaceCombination =
                    SupportedSurfaceCombination(
                        ApplicationProvider.getApplicationContext(),
                        cameraProperties.metadata,
                        FakeEncoderProfilesProvider.Builder().build(),
                        FeatureCombinationQuery.NO_OP_FEATURE_COMBINATION_QUERY,
                        ExtraSupportedSurfaceCombinationsContainer(),
                    ),
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

    @Test
    fun useCase_isGarbageCollectedAfterDetach_normalMode() = runTest {
        initializeUseCaseThreads(this)
        val useCaseManager = createUseCaseManager()
        var useCase: Preview? = createPreview()
        val weakRef = WeakReference(useCase)

        useCaseManager.attach(listOf(useCase!!))
        useCaseManager.activate(useCase!!)

        useCaseManager.deactivate(useCase!!)
        useCaseManager.detach(listOf(useCase!!))

        useCaseList.remove(useCase!!)
        @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
        useCase = null
        testScheduler.advanceUntilIdle()
        ShadowLooper.idleMainLooper()
        GarbageCollectionUtil.runFinalization()

        assertThat(weakRef.get()).isNull()
    }

    @Test
    fun useCase_isGarbageCollectedAfterDetach_deferredMode() = runTest {
        initializeUseCaseThreads(this)
        val useCaseManager = createUseCaseManager()
        useCaseManager.setCameraGraphCreationMode(false)
        var useCase: Preview? = createPreview()
        val weakRef = WeakReference(useCase)

        useCaseManager.attach(listOf(useCase!!))
        useCaseManager.activate(useCase!!)

        useCaseManager.deactivate(useCase!!)
        useCaseManager.detach(listOf(useCase!!))

        useCaseList.remove(useCase!!)
        @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
        useCase = null
        testScheduler.advanceUntilIdle()
        ShadowLooper.idleMainLooper()
        GarbageCollectionUtil.runFinalization()

        assertThat(weakRef.get()).isNull()
    }

    @Test
    fun useCase_isLeaked_whenBuilderIsReused() = runTest {
        initializeUseCaseThreads(this)
        val sharedBuilder = FakeUseCaseCameraComponentBuilder()
        val useCaseManager =
            createUseCaseManager(
                useCaseCameraComponentBuilderProvider =
                    Provider<UseCaseCameraComponent.Builder> { sharedBuilder }
            )
        var useCase: Preview? = createPreview()
        val weakRef = WeakReference(useCase)

        useCaseManager.attach(listOf(useCase!!))
        useCaseManager.activate(useCase!!)

        useCaseManager.deactivate(useCase!!)
        useCaseManager.detach(listOf(useCase!!))

        useCaseList.remove(useCase!!)
        @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
        useCase = null
        testScheduler.advanceUntilIdle()
        ShadowLooper.idleMainLooper()
        GarbageCollectionUtil.runFinalization()

        assertThat(weakRef.get()).isNotNull()
    }

    @Test
    fun detachAllUseCases_clearsZslRingBuffer() = runTest {
        initializeUseCaseThreads(this)
        var clearRingBufferCallCount = 0
        var isDisabledByDetach = false
        val fakeZslControl =
            object : ZslControl by ZslControlNoOpImpl() {
                override fun clearRingBuffer() {
                    clearRingBufferCallCount++
                }

                override fun setZslDisabledByDetach(disabled: Boolean) {
                    isDisabledByDetach = disabled
                }
            }
        val useCaseManager = createUseCaseManager(zslControl = fakeZslControl)
        val preview = createPreview()

        useCaseManager.attach(listOf(preview))
        useCaseManager.activate(preview)
        assertThat(isDisabledByDetach).isFalse()
        clearRingBufferCallCount = 0

        useCaseManager.deactivate(preview)
        useCaseManager.detach(listOf(preview))

        assertThat(isDisabledByDetach).isTrue()
        assertThat(clearRingBufferCallCount).isAtLeast(1)
    }

    @Test
    fun detachThenReattachBeforeCloseJobCompletes_keepsZslEnabled() = runTest {
        initializeUseCaseThreads(this)
        var clearRingBufferCallCount = 0
        var isDisabledByDetach = false
        val fakeZslControl =
            object : ZslControl by ZslControlNoOpImpl() {
                override fun clearRingBuffer() {
                    clearRingBufferCallCount++
                }

                override fun setZslDisabledByDetach(disabled: Boolean) {
                    isDisabledByDetach = disabled
                }
            }
        val firstCameraCloseJob = kotlinx.coroutines.CompletableDeferred<Unit>()
        var buildCount = 0
        val customBuilderProvider =
            Provider<UseCaseCameraComponent.Builder> {
                val delegateBuilder = FakeUseCaseCameraComponentBuilder()
                object : UseCaseCameraComponent.Builder by delegateBuilder {
                    override fun build(): UseCaseCameraComponent {
                        buildCount++
                        val isFirstCamera = buildCount == 1
                        val delegateComponent = delegateBuilder.build()
                        val delegateCamera = delegateComponent.getUseCaseCamera()
                        val customCamera =
                            object : UseCaseCamera by delegateCamera {
                                override fun close(): kotlinx.coroutines.Job =
                                    if (isFirstCamera) {
                                        firstCameraCloseJob
                                    } else {
                                        delegateCamera.close()
                                    }
                            }
                        return object : UseCaseCameraComponent by delegateComponent {
                            override fun getUseCaseCamera(): UseCaseCamera = customCamera
                        }
                    }
                }
            }
        val useCaseManager =
            createUseCaseManager(
                useCaseCameraComponentBuilderProvider = customBuilderProvider,
                zslControl = fakeZslControl,
            )
        val preview = createPreview()

        useCaseManager.attach(listOf(preview))
        useCaseManager.activate(preview)
        assertThat(isDisabledByDetach).isFalse()

        // Detach all use cases (firstCameraCloseJob is still in-flight)
        useCaseManager.deactivate(preview)
        useCaseManager.detach(listOf(preview))
        assertThat(isDisabledByDetach).isTrue()

        // Re-attach before firstCameraCloseJob completes
        useCaseManager.attach(listOf(preview))
        useCaseManager.activate(preview)
        assertThat(isDisabledByDetach).isFalse()
        clearRingBufferCallCount = 0

        // Complete the prior UseCaseCamera's close job; ZSL must remain enabled for the active
        // session and its ring buffer must not be cleared.
        firstCameraCloseJob.complete(Unit)

        assertThat(isDisabledByDetach).isFalse()
        assertThat(clearRingBufferCallCount).isEqualTo(0)
    }

    @Test
    @Config(minSdk = 33, qualifiers = "w1080dp-h1920dp-mdpi")
    fun meteringRepeatingEnabled_whenS1440pVideoCaptureWithPreviewStabilization() = runTest {
        // Arrange: Configure a StreamConfigurationMap where 1920x1440 (S1440P_4_3) is larger than
        // PREVIEW (1920x1080) and maps to RECORD under CAPTURE_SESSION_TABLES, but maps to
        // S1440P_4_3 under PREVIEW_STABILIZATION_TABLE.
        initializeUseCaseThreads(this)
        val s1440pSize = Size(1920, 1440)
        val privFormat = ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
        val customStreamConfigMap =
            StreamConfigurationMapBuilder.newBuilder()
                .apply {
                    addOutputSize(privFormat, Size(640, 480))
                    addOutputSize(privFormat, Size(1920, 1080))
                    addOutputSize(privFormat, s1440pSize)
                    addOutputSize(privFormat, Size(3840, 2160))
                }
                .build()
        val useCaseManager =
            createUseCaseManager(
                characteristicsMap =
                    mapOf(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP to
                            customStreamConfigMap,
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to
                            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3,
                    )
            )

        val videoCaptureConfig =
            FakeUseCaseConfig.Builder(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE)
                .setBufferFormat(privFormat)
                .apply {
                    mutableConfig.insertOption(
                        UseCaseConfig.OPTION_PREVIEW_STABILIZATION_MODE,
                        StabilizationMode.ON,
                    )
                }
                .useCaseConfig
        val surfaceTexture = SurfaceTexture(0)
        val surface = Surface(surfaceTexture)
        val nonRepeatingSurface =
            ImmediateSurface(surface, s1440pSize, privFormat).apply {
                setContainerClass(MediaCodec::class.java)
                terminationFuture.addListener(
                    {
                        surface.release()
                        surfaceTexture.release()
                    },
                    useCaseThreads.backgroundExecutor,
                )
            }
        try {
            val fakeVideoCapture =
                FakeUseCase(videoCaptureConfig, UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE)
                    .apply {
                        bindToCamera(FakeCamera("0"), null, null, null)
                        updateSuggestedStreamSpec(StreamSpec.builder(s1440pSize).build(), null)
                        updateSessionConfigForTesting(
                            SessionConfig.Builder()
                                .setTemplateType(TEMPLATE_RECORD)
                                .addNonRepeatingSurface(nonRepeatingSurface)
                                .build()
                        )
                        useCaseList.add(this)
                    }

            // Act
            useCaseManager.attach(listOf(fakeVideoCapture))
            useCaseManager.activate(fakeVideoCapture)

            // Assert: MeteringRepeating must be enabled because S1440P_4_3 + PREVIEW is supported
            // in the preview stabilization table.
            val enabledUseCaseClasses =
                useCaseManager.getRunningUseCasesForTest().map { it::class.java }
            assertThat(enabledUseCaseClasses)
                .containsExactly(FakeUseCase::class.java, MeteringRepeating::class.java)
        } finally {
            nonRepeatingSurface.close()
        }
    }

    @Test
    fun sessionConfigCamera2Interop_previewAndVideoCapture_overridesRecordTemplate() = runTest {
        initializeUseCaseThreads(this)
        val harness = createSessionInteropEndToEndHarness()

        // Baseline: Preview + VideoCapture without interop defaults to TEMPLATE_RECORD.
        val baselinePreview = createBoundPreview()
        val baselineVideo = createTestVideoUseCase()
        harness.bindSessionConfig(
            androidx.camera.core.SessionConfig.Builder(baselinePreview, baselineVideo).build()
        )
        assertThat(harness.builtConfig.cameraGraphConfig.sessionTemplate)
            .isEqualTo(RequestTemplate(TEMPLATE_RECORD))
        val baselineRepeating = harness.submitRepeatingRequest(this)
        assertThat(baselineRepeating.template).isEqualTo(RequestTemplate(TEMPLATE_RECORD))
        assertThat(harness.state3AControl.template).isEqualTo(TEMPLATE_RECORD)
        assertThat(baselineRepeating.parameters[CaptureRequest.CONTROL_AF_MODE])
            .isEqualTo(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
        harness.unbindAll()

        // Act 1: SessionConfigCamera2Interop sets TEMPLATE_PREVIEW (lower priority than
        // TEMPLATE_RECORD in UseCase template merging, so session-level interop must override).
        val preview = createBoundPreview()
        val videoCapture = createTestVideoUseCase()
        val sessionConfig =
            androidx.camera.core.SessionConfig.Builder(preview, videoCapture)
                .camera2Interop {
                    repeatingCaptureRequestTemplate = TEMPLATE_PREVIEW
                    captureRequest[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION] = 4
                }
                .build()
        harness.bindSessionConfig(sessionConfig)

        // Assert 1: Both CameraGraph.Config, UseCaseCameraRequestControlImpl, and State3AControl
        // use TEMPLATE_PREVIEW and continuous picture AF mode, without polluting UseCaseConfig.
        assertThat(harness.builtConfig.cameraGraphConfig.sessionTemplate)
            .isEqualTo(RequestTemplate(TEMPLATE_PREVIEW))
        assertThat(
                harness.builtConfig.cameraGraphConfig.sessionParameters[
                        CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION]
            )
            .isEqualTo(4)
        val repeatingRequest = harness.submitRepeatingRequest(this)
        assertThat(repeatingRequest.template).isEqualTo(RequestTemplate(TEMPLATE_PREVIEW))
        assertThat(repeatingRequest.parameters[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION])
            .isEqualTo(4)
        assertThat(harness.state3AControl.template).isEqualTo(TEMPLATE_PREVIEW)
        assertThat(repeatingRequest.parameters[CaptureRequest.CONTROL_AF_MODE])
            .isEqualTo(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        assertThat(preview.currentConfig.containsOption(Camera2ImplConfig.TEMPLATE_TYPE_OPTION))
            .isFalse()
        assertThat(
                videoCapture.currentConfig.containsOption(Camera2ImplConfig.TEMPLATE_TYPE_OPTION)
            )
            .isFalse()
        harness.unbindAll()

        // Act 2: SessionConfigCamera2Interop sets TEMPLATE_MANUAL.
        val manualPreview = createBoundPreview()
        val manualVideo = createTestVideoUseCase()
        harness.bindSessionConfig(
            androidx.camera.core.SessionConfig.Builder(manualPreview, manualVideo)
                .camera2Interop { repeatingCaptureRequestTemplate = TEMPLATE_MANUAL }
                .build()
        )
        assertThat(harness.builtConfig.cameraGraphConfig.sessionTemplate)
            .isEqualTo(RequestTemplate(TEMPLATE_MANUAL))
        val manualRepeating = harness.submitRepeatingRequest(this)
        assertThat(manualRepeating.template).isEqualTo(RequestTemplate(TEMPLATE_MANUAL))
        assertThat(harness.state3AControl.template).isEqualTo(TEMPLATE_MANUAL)
    }

    @Test
    fun sessionConfigCamera2Interop_previewAndImageCapture_recordTemplate_configuresContinuousVideoAfMode() =
        runTest {
            initializeUseCaseThreads(this)
            val harness = createSessionInteropEndToEndHarness()
            val preview = createBoundPreview()
            val imageCapture = ImageCapture.Builder().build()
            val sessionConfig =
                androidx.camera.core.SessionConfig.Builder(preview, imageCapture)
                    .camera2Interop { repeatingCaptureRequestTemplate = TEMPLATE_RECORD }
                    .build()
            harness.bindSessionConfig(sessionConfig)

            assertThat(harness.builtConfig.cameraGraphConfig.sessionTemplate)
                .isEqualTo(RequestTemplate(TEMPLATE_RECORD))
            val repeatingRequest = harness.submitRepeatingRequest(this)
            assertThat(repeatingRequest.template).isEqualTo(RequestTemplate(TEMPLATE_RECORD))
            assertThat(harness.state3AControl.template).isEqualTo(TEMPLATE_RECORD)
            assertThat(repeatingRequest.parameters[CaptureRequest.CONTROL_AF_MODE])
                .isEqualTo(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
        }

    @Test
    fun sessionConfigCamera2Interop_withStreamSharing_propagatesTemplateCallbacksAndOptions() =
        runTest {
            initializeUseCaseThreads(this)
            val harness = createSessionInteropEndToEndHarness()
            val preview = createBoundPreview()
            val videoCapture = createTestVideoUseCase()
            val imageCapture = ImageCapture.Builder().build()

            var repeatingCallbackCount = 0
            val repeatingCallback =
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureSequenceCompleted(
                        session: CameraCaptureSession,
                        sequenceId: Int,
                        frameNumber: Long,
                    ) {
                        repeatingCallbackCount++
                    }
                }

            val sessionConfig =
                androidx.camera.core.SessionConfig.Builder(preview, videoCapture, imageCapture)
                    .camera2Interop {
                        repeatingCaptureRequestTemplate = TEMPLATE_PREVIEW
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            sessionParameter[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION] = 3
                        }
                        captureRequest[CaptureRequest.CONTROL_AWB_LOCK] = true
                        setRepeatingCaptureCallback(
                            CameraXExecutors.directExecutor(),
                            repeatingCallback,
                        )
                    }
                    .build()

            // Act: FakeCameraDeviceSurfaceManager wraps Preview + VideoCapture in StreamSharing.
            harness.bindSessionConfig(sessionConfig)

            // Assert: StreamSharing is active and SessionConfigCamera2Interop overrides
            // StreamSharing's TEMPLATE_RECORD without merging into any UseCase's currentConfig.
            val streamSharing =
                harness.cameraUseCaseAdapter.cameraUseCases
                    .filterIsInstance<StreamSharing>()
                    .single()
            val graphConfig = harness.builtConfig.cameraGraphConfig
            assertThat(graphConfig.sessionTemplate).isEqualTo(RequestTemplate(TEMPLATE_PREVIEW))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                assertThat(
                        graphConfig.sessionParameters[
                                CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION]
                    )
                    .isEqualTo(3)
            }
            assertThat(graphConfig.sessionParameters[CaptureRequest.CONTROL_AWB_LOCK])
                .isEqualTo(true)

            for (useCase in listOf(preview, videoCapture, imageCapture, streamSharing)) {
                assertThat(
                        useCase.currentConfig.containsOption(Camera2ImplConfig.TEMPLATE_TYPE_OPTION)
                    )
                    .isFalse()
                assertThat(
                        useCase.currentConfig.containsOption(
                            Camera2ImplConfig.SESSION_CAPTURE_CALLBACK_OPTION
                        )
                    )
                    .isFalse()
            }

            val repeatingRequest = harness.submitRepeatingRequest(this)
            assertThat(repeatingRequest.template).isEqualTo(RequestTemplate(TEMPLATE_PREVIEW))
            assertThat(repeatingRequest.parameters[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION])
                .isNull()
            assertThat(repeatingRequest.parameters[CaptureRequest.CONTROL_AWB_LOCK]).isEqualTo(true)

            dispatchSequenceCompleted(repeatingRequest, this)
            assertThat(repeatingCallbackCount).isEqualTo(1)
        }

    @Test
    fun sessionConfigCamera2Interop_sessionParameterIsolatedFromCaptureRequest() = runTest {
        initializeUseCaseThreads(this)
        val harness = createSessionInteropEndToEndHarness()
        val preview = createBoundPreview()
        val sessionConfig =
            androidx.camera.core.SessionConfig.Builder(preview)
                .camera2Interop {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        sessionParameter[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION] = 1
                    }
                    captureRequest[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION] = 2
                }
                .build()

        harness.bindSessionConfig(sessionConfig)

        val graphConfig = harness.builtConfig.cameraGraphConfig
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            assertThat(
                    graphConfig.sessionParameters[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION]
                )
                .isEqualTo(1)
        }

        val repeatingRequest = harness.submitRepeatingRequest(this)
        assertThat(repeatingRequest.parameters[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION])
            .isEqualTo(2)
    }

    @Test
    fun sessionConfigCamera2Interop_withImageCapture_preservesStillCaptureTemplateAndIsolatesCallback() =
        runTest {
            initializeUseCaseThreads(this)
            val harness = createSessionInteropEndToEndHarness()

            // Subcase A: ImageCapture is index 0 in SessionConfig(imageCapture, preview).
            val imageCaptureFirst =
                ImageCapture.Builder()
                    .camera2Interop {
                        stillCaptureRequest[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION] = -2
                    }
                    .build()
            val preview = createBoundPreview()
            var indexZeroRepeatingCallbackCount = 0
            val indexZeroRepeatingCallback =
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureSequenceCompleted(
                        session: CameraCaptureSession,
                        sequenceId: Int,
                        frameNumber: Long,
                    ) {
                        indexZeroRepeatingCallbackCount++
                    }
                }

            harness.bindSessionConfig(
                androidx.camera.core.SessionConfig.Builder(imageCaptureFirst, preview)
                    .camera2Interop {
                        repeatingCaptureRequestTemplate = TEMPLATE_MANUAL
                        captureRequest[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION] = 2
                        captureRequest[CaptureRequest.CONTROL_AWB_LOCK] = true
                        setRepeatingCaptureCallback(
                            CameraXExecutors.directExecutor(),
                            indexZeroRepeatingCallback,
                        )
                    }
                    .build()
            )

            // Repeating request uses TEMPLATE_MANUAL and invokes repeatingCallback even though
            // ImageCapture (at index 0) is inactive during normal preview.
            assertThat(harness.builtConfig.cameraGraphConfig.sessionTemplate)
                .isEqualTo(RequestTemplate(TEMPLATE_MANUAL))
            val repeatingRequestA = harness.submitRepeatingRequest(this)
            assertThat(repeatingRequestA.template).isEqualTo(RequestTemplate(TEMPLATE_MANUAL))
            assertThat(
                    repeatingRequestA.parameters[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION]
                )
                .isEqualTo(2)
            assertThat(repeatingRequestA.parameters[CaptureRequest.CONTROL_AWB_LOCK])
                .isEqualTo(true)
            dispatchSequenceCompleted(repeatingRequestA, this)
            assertThat(indexZeroRepeatingCallbackCount).isEqualTo(1)

            // One-shot still capture preserves TEMPLATE_STILL_CAPTURE, inherits session repeating
            // captureRequest keys with still-capture override precedence, and does not invoke
            // repeatingCallback.
            val (stillConfigA, stillRequestA) = harness.submitStillCapture(imageCaptureFirst, this)
            assertThat(stillConfigA.templateType).isEqualTo(TEMPLATE_STILL_CAPTURE)
            assertThat(stillRequestA.template).isEqualTo(RequestTemplate(TEMPLATE_STILL_CAPTURE))
            assertThat(stillRequestA.parameters[CaptureRequest.CONTROL_AWB_LOCK]).isEqualTo(true)
            assertThat(stillRequestA.parameters[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION])
                .isEqualTo(-2)
            dispatchSequenceCompleted(stillRequestA, this)
            assertThat(indexZeroRepeatingCallbackCount).isEqualTo(1)
            harness.unbindAll()

            // Subcase B: ImageCapture alongside StreamSharing(Preview, VideoCapture).
            val sharedPreview = createBoundPreview()
            val sharedVideo = createTestVideoUseCase()
            val sharedImageCapture = ImageCapture.Builder().build()
            var sharedRepeatingCallbackCount = 0
            val sharedRepeatingCallback =
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureSequenceCompleted(
                        session: CameraCaptureSession,
                        sequenceId: Int,
                        frameNumber: Long,
                    ) {
                        sharedRepeatingCallbackCount++
                    }
                }

            harness.bindSessionConfig(
                androidx.camera.core.SessionConfig.Builder(
                        sharedPreview,
                        sharedVideo,
                        sharedImageCapture,
                    )
                    .camera2Interop {
                        repeatingCaptureRequestTemplate = TEMPLATE_MANUAL
                        captureRequest[CaptureRequest.CONTROL_AWB_LOCK] = true
                        setRepeatingCaptureCallback(
                            CameraXExecutors.directExecutor(),
                            sharedRepeatingCallback,
                        )
                    }
                    .build()
            )
            assertThat(
                    harness.cameraUseCaseAdapter.cameraUseCases.filterIsInstance<StreamSharing>()
                )
                .hasSize(1)

            val repeatingRequestB = harness.submitRepeatingRequest(this)
            assertThat(repeatingRequestB.template).isEqualTo(RequestTemplate(TEMPLATE_MANUAL))
            dispatchSequenceCompleted(repeatingRequestB, this)
            assertThat(sharedRepeatingCallbackCount).isEqualTo(1)

            val (stillConfigB, stillRequestB) = harness.submitStillCapture(sharedImageCapture, this)
            assertThat(stillConfigB.templateType).isEqualTo(TEMPLATE_STILL_CAPTURE)
            assertThat(stillRequestB.template).isEqualTo(RequestTemplate(TEMPLATE_STILL_CAPTURE))
            assertThat(stillRequestB.parameters[CaptureRequest.CONTROL_AWB_LOCK]).isEqualTo(true)
            dispatchSequenceCompleted(stillRequestB, this)
            assertThat(sharedRepeatingCallbackCount).isEqualTo(1)
        }

    private inner class SessionInteropEndToEndHarness(
        val useCaseManager: UseCaseManager,
        val cameraUseCaseAdapter: androidx.camera.core.internal.CameraUseCaseAdapter,
        private val createdBuilders: List<FakeUseCaseCameraComponentBuilder>,
    ) {
        val builtConfig: UseCaseCameraConfig
            get() = checkNotNull(createdBuilders.last().lastBuiltConfig)

        private var fakeCameraGraph: FakeCameraGraph? = null
        private var requestControl: UseCaseCameraRequestControlImpl? = null
        private val submittedStillRequests = mutableListOf<Request>()
        val state3AControl =
            State3AControl(FakeCameraProperties(), NoOpAutoFlashAEModeDisabler, useCaseThreads)

        fun bindSessionConfig(sessionConfig: androidx.camera.core.SessionConfig) {
            cameraUseCaseAdapter.setSessionInteropConfig(sessionConfig.interopConfig)
            cameraUseCaseAdapter.addUseCases(sessionConfig.useCases)
            ShadowLooper.idleMainLooper()
        }

        fun unbindAll() {
            state3AControl.reset()
            requestControl?.close()
            requestControl = null
            fakeCameraGraph = null
            cameraUseCaseAdapter.removeAllUseCases()
            ShadowLooper.idleMainLooper()
        }

        private fun ensureRequestControl(): Pair<FakeCameraGraph, UseCaseCameraRequestControlImpl> {
            val existingGraph = fakeCameraGraph
            val existingControl = requestControl
            if (existingGraph != null && existingControl != null) {
                return existingGraph to existingControl
            }
            val graph = FakeCameraGraph()
            val stateAdapter = CameraStateAdapter(CameraSessionLifecycleAdapter())
            val sessionConfigAdapter = builtConfig.provideSessionConfigAdapter()
            val surfaceToStreamMap =
                buildMap<DeferrableSurface, StreamId> {
                    var index = 0
                    for (useCase in cameraUseCaseAdapter.cameraUseCases) {
                        for (surface in useCase.sessionConfig.surfaces) {
                            put(surface, StreamId(index++))
                        }
                    }
                }
            val context =
                UseCaseCameraContext(
                    cameraGraphProvider = { graph },
                    cameraStateAdapter = stateAdapter,
                    graphStateToCameraStateAdapter = GraphStateToCameraStateAdapter(stateAdapter),
                    streamConfigMapProvider = { emptyMap() },
                    defaultSurfaceToStreamMap = surfaceToStreamMap,
                )
            val captureConfigAdapter =
                CaptureConfigAdapter(
                    cameraProperties = FakeCameraProperties(),
                    useCaseCameraContext = context,
                    zslControl = ZslControlNoOpImpl(),
                    threads = useCaseThreads,
                    templateParamsOverride = NoOpTemplateParamsOverride,
                )
            val capturePipeline =
                object : CapturePipeline by FakeCapturePipeline() {
                    override suspend fun submitStillCaptures(
                        configs: List<CaptureConfig>,
                        requestTemplate: RequestTemplate,
                        sessionConfigOptions: androidx.camera.core.impl.Config,
                        captureMode: Int,
                        flashType: Int,
                        flashMode: Int,
                    ): List<Deferred<Void?>> {
                        configs.forEach { config ->
                            submittedStillRequests.add(
                                captureConfigAdapter.mapToRequest(
                                    captureConfig = config,
                                    requestTemplate = requestTemplate,
                                    sessionConfigOptions = sessionConfigOptions,
                                )
                            )
                        }
                        return configs.map {
                            CompletableDeferred<Void?>().apply { complete(null) }
                        }
                    }
                }
            val control =
                UseCaseCameraRequestControlImpl(
                    capturePipelineProvider = { capturePipeline },
                    state3AControlProvider = { state3AControl },
                    useCaseCameraStateProvider = {
                        UseCaseCameraState(
                            useCaseCameraContext = context,
                            templateParamsOverride = NoOpTemplateParamsOverride,
                        )
                    },
                    useCaseCameraContext = context,
                    useCaseSurfaceManagerProvider = {
                        FakeUseCaseSurfaceManager(threads = useCaseThreads)
                    },
                    threads = useCaseThreads,
                    sessionConfigAdapterProvider = { sessionConfigAdapter },
                )
            state3AControl.requestControl = control
            fakeCameraGraph = graph
            requestControl = control
            return graph to control
        }

        suspend fun submitRepeatingRequest(testScope: TestScope): Request {
            val (graph, control) = ensureRequestControl()
            val deferred =
                control.updateRepeatingRequestAsync(
                    isPrimary = true,
                    runningUseCases = useCaseManager.getRunningUseCasesForTest(),
                )
            testScope.testScheduler.advanceUntilIdle()
            deferred.await()
            return graph.fakeCameraGraphSession.repeatingRequests.last()
        }

        suspend fun submitStillCapture(
            imageCapture: ImageCapture,
            testScope: TestScope,
        ): Pair<CaptureConfig, Request> {
            val (_, control) = ensureRequestControl()
            val stillConfig =
                CaptureConfig.Builder.createFrom(imageCapture.currentConfig)
                    .apply { addSurface(imageCapture.sessionConfig.surfaces.single()) }
                    .build()
            val deferreds =
                control.issueSingleCaptureAsync(
                    captureSequence = listOf(stillConfig),
                    captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                    flashType = ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
                    flashMode = ImageCapture.FLASH_MODE_OFF,
                )
            testScope.testScheduler.advanceUntilIdle()
            deferreds.forEach { it.await() }
            return stillConfig to submittedStillRequests.last()
        }
    }

    private fun createSessionInteropEndToEndHarness(): SessionInteropEndToEndHarness {
        val surfaceProcessor = FakeSurfaceProcessorInternal(CameraXExecutors.mainThreadExecutor())
        surfaceProcessorsToCleanUp.add(surfaceProcessor)
        DefaultSurfaceProcessor.Factory.setSupplier { surfaceProcessor }

        val createdBuilders = mutableListOf<FakeUseCaseCameraComponentBuilder>()
        val useCaseManager =
            createUseCaseManager(
                useCaseCameraComponentBuilderProvider =
                    Provider {
                        FakeUseCaseCameraComponentBuilder().also { createdBuilders.add(it) }
                    }
            )
        val useCaseConfigFactory = CameraUseCaseAdapter(ApplicationProvider.getApplicationContext())
        val streamSpecsCalculator =
            StreamSpecsCalculatorImpl(useCaseConfigFactory, FakeCameraDeviceSurfaceManager())
        val fakeCameraInfo = FakeCameraInfoInternal("0", streamSpecsCalculator)
        val cameraInternalAdapter =
            CameraInternalAdapter(
                config = CameraConfig(CameraId("0")),
                useCaseManager = useCaseManager,
                cameraInfo = fakeCameraInfo,
                cameraController = FakeCameraControl(),
                threads = useCaseThreads,
                cameraStateAdapter = CameraStateAdapter(CameraSessionLifecycleAdapter()),
            )
        val cameraUseCaseAdapter =
            androidx.camera.core.internal.CameraUseCaseAdapter(
                cameraInternalAdapter,
                null,
                AdapterCameraInfo(
                    cameraInternalAdapter.cameraInfoInternal,
                    CameraConfigs.defaultConfig(),
                ),
                null,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                FakeCameraCoordinator(),
                streamSpecsCalculator,
                useCaseConfigFactory,
            )
        adaptersToDetach.add(cameraUseCaseAdapter)
        return SessionInteropEndToEndHarness(
            useCaseManager = useCaseManager,
            cameraUseCaseAdapter = cameraUseCaseAdapter,
            createdBuilders = createdBuilders,
        )
    }

    private fun createBoundPreview(): Preview =
        Preview.Builder().build().apply {
            setSurfaceProvider(
                CameraXExecutors.directExecutor(),
                SurfaceTextureProvider.createSurfaceTextureProvider(),
            )
        }

    private fun createTestVideoUseCase(): FakeUseCase {
        val config =
            FakeUseCaseConfig.Builder()
                .setCaptureType(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE)
                .setSurfaceOccupancyPriority(0)
                .useCaseConfig
        return object : FakeUseCase(config, UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE) {
                override fun getUseCaseConfigBuilder(
                    config: androidx.camera.core.impl.Config
                ): UseCaseConfig.Builder<*, *, *> =
                    FakeUseCaseConfig.Builder(config)
                        .setCaptureType(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE)
                        .setSessionOptionUnpacker(
                            CameraUseCaseAdapter.DefaultSessionOptionsUnpacker
                        )
                        .setCaptureOptionUnpacker(
                            CameraUseCaseAdapter.DefaultCaptureOptionsUnpacker.INSTANCE
                        )

                override fun onSuggestedStreamSpecUpdated(
                    primaryStreamSpec: StreamSpec,
                    secondaryStreamSpec: StreamSpec?,
                ): StreamSpec {
                    val surfaceTexture = SurfaceTexture(0)
                    val rawSurface = Surface(surfaceTexture)
                    val surface =
                        ImmediateSurface(
                                rawSurface,
                                primaryStreamSpec.resolution,
                                ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                            )
                            .apply {
                                setContainerClass(MediaCodec::class.java)
                                terminationFuture.addListener(
                                    {
                                        rawSurface.release()
                                        surfaceTexture.release()
                                    },
                                    CameraXExecutors.directExecutor(),
                                )
                            }
                            .also { testSurfacesToClose.add(it) }
                    val sessionConfig =
                        SessionConfig.Builder.createFrom(
                                currentConfig,
                                primaryStreamSpec.resolution,
                            )
                            .addSurface(surface, primaryStreamSpec.dynamicRange)
                            .build()
                    updateSessionConfigForTesting(sessionConfig)
                    notifyActiveForTesting()
                    return primaryStreamSpec
                }
            }
            .apply { supportedEffectTargets = setOf(CameraEffect.VIDEO_CAPTURE) }
    }

    private fun dispatchSequenceCompleted(request: Request, testScope: TestScope) {
        val mockSession = mock(CameraCaptureSession::class.java)
        val mockCaptureRequest = mock(CaptureRequest::class.java)
        val delegate = FakeRequestMetadata(request = request)
        val metadata =
            object : RequestMetadata by delegate {
                @Suppress("UNCHECKED_CAST")
                override fun <T : Any> unwrapAs(type: Class<T>): T? =
                    when (type) {
                        CameraCaptureSession::class.java -> mockSession as T
                        CaptureRequest::class.java -> mockCaptureRequest as T
                        else -> delegate.unwrapAs(type)
                    }
            }
        request.listeners.forEach { listener ->
            listener.onRequestSequenceCompleted(metadata, CameraFrameNumber(1L))
        }
        testScope.testScheduler.advanceUntilIdle()
    }
}
