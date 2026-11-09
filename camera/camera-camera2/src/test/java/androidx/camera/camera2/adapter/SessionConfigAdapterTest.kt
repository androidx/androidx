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

package androidx.camera.camera2.adapter

import android.graphics.ColorSpace
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.hardware.camera2.CameraDevice.TEMPLATE_RECORD
import android.hardware.camera2.CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG
import android.hardware.camera2.CaptureRequest
import android.media.MediaCodec
import android.os.Build
import android.util.Range
import android.view.Surface
import androidx.camera.camera2.impl.Camera2ImplConfig
import androidx.camera.camera2.impl.createCaptureRequestOption
import androidx.camera.camera2.impl.createSessionParameterOption
import androidx.camera.camera2.pipe.OutputStream.StreamUseHint
import androidx.camera.core.Preview
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.core.streamsharing.StreamSharing
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.testutils.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import junit.framework.TestCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class SessionConfigAdapterTest {

    private val sessionConfigAdapter = SessionConfigAdapter(listOf())

    @get:Rule
    val dispatcherRule = MainDispatcherRule(MoreExecutors.directExecutor().asCoroutineDispatcher())

    private val deferrableSurfacesToClose = mutableListOf<DeferrableSurface>()

    @After
    fun tearDown() {
        for (surface in deferrableSurfacesToClose) {
            surface.close()
        }
    }

    @Test
    fun invalidSessionConfig() {
        // Arrange
        val testDeferrableSurface = createTestDeferrableSurface()

        // Create an invalid SessionConfig which doesn't set the template
        val fakeTestUseCase = createFakeTestUseCase {
            it.setupSessionConfig(
                SessionConfig.Builder().also { sessionConfigBuilder ->
                    sessionConfigBuilder.addSurface(testDeferrableSurface)
                }
            )
        }

        // Act
        val sessionConfigAdapter = SessionConfigAdapter(useCases = listOf(fakeTestUseCase))

        // Assert
        assertThat(sessionConfigAdapter.isSessionConfigValid()).isFalse()
        assertThat(sessionConfigAdapter.getValidSessionConfigOrNull()).isNull()
    }

    @Test
    fun reportInvalidSurfaceTest() {
        // Arrange
        val testDeferrableSurface = createTestDeferrableSurface().apply { close() }

        val errorListener =
            object : SessionConfig.ErrorListener {
                val results = mutableListOf<Pair<SessionConfig, SessionConfig.SessionError>>()

                override fun onError(
                    sessionConfig: SessionConfig,
                    error: SessionConfig.SessionError,
                ) {
                    results.add(Pair(sessionConfig, error))
                }
            }

        val fakeTestUseCase1 = createFakeTestUseCase {
            it.setupSessionConfig(
                SessionConfig.Builder().also { sessionConfigBuilder ->
                    sessionConfigBuilder.setTemplateType(TEMPLATE_PREVIEW)
                    sessionConfigBuilder.addSurface(testDeferrableSurface)
                    sessionConfigBuilder.setErrorListener(errorListener)
                }
            )
        }
        val fakeTestUseCase2 = createFakeTestUseCase {
            it.setupSessionConfig(
                SessionConfig.Builder().also { sessionConfigBuilder ->
                    sessionConfigBuilder.setTemplateType(TEMPLATE_PREVIEW)
                    sessionConfigBuilder.addSurface(createTestDeferrableSurface().apply { close() })
                    sessionConfigBuilder.setErrorListener(errorListener)
                }
            )
        }

        // Act
        SessionConfigAdapter(useCases = listOf(fakeTestUseCase1, fakeTestUseCase2))
            .reportSurfaceInvalid(testDeferrableSurface)

        // Assert, verify it only reports the SURFACE_NEEDS_RESET error on one SessionConfig
        // at a time.
        assertThat(errorListener.results.size).isEqualTo(1)
        assertThat(errorListener.results[0].second)
            .isEqualTo(SessionConfig.SessionError.SESSION_ERROR_SURFACE_NEEDS_RESET)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun reportInvalidSurface_sessionConfigReplacedBeforeMainDispatch_doesNotNotifyStaleOrNew() {
        val testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        try {
            // Arrange: UseCase initially has sessionConfig1 with surface1 and errorListener1
            val surface1 = createTestDeferrableSurface().apply { close() }
            val surface2 = createTestDeferrableSurface()
            val errorListener1 =
                object : SessionConfig.ErrorListener {
                    val results = mutableListOf<Pair<SessionConfig, SessionConfig.SessionError>>()

                    override fun onError(
                        sessionConfig: SessionConfig,
                        error: SessionConfig.SessionError,
                    ) {
                        results.add(Pair(sessionConfig, error))
                    }
                }
            val errorListener2 =
                object : SessionConfig.ErrorListener {
                    val results = mutableListOf<Pair<SessionConfig, SessionConfig.SessionError>>()

                    override fun onError(
                        sessionConfig: SessionConfig,
                        error: SessionConfig.SessionError,
                    ) {
                        results.add(Pair(sessionConfig, error))
                    }
                }

            val fakeTestUseCase = createFakeTestUseCase {
                it.setupSessionConfig(
                    SessionConfig.Builder().apply {
                        setTemplateType(TEMPLATE_PREVIEW)
                        addSurface(surface1)
                        setErrorListener(errorListener1)
                    }
                )
            }
            val adapter = SessionConfigAdapter(useCases = listOf(fakeTestUseCase))

            // Act: background thread reports surface1 invalid before main-thread dispatch runs,
            // and the main thread concurrently replaces the UseCase's SessionConfig with surface2.
            adapter.reportSurfaceInvalid(surface1)
            fakeTestUseCase.setupSessionConfig(
                SessionConfig.Builder().apply {
                    setTemplateType(TEMPLATE_PREVIEW)
                    addSurface(surface2)
                    setErrorListener(errorListener2)
                }
            )
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert: neither the stale SessionConfig nor the newly attached SessionConfig
            // should receive SESSION_ERROR_SURFACE_NEEDS_RESET for surface1.
            assertThat(errorListener1.results).isEmpty()
            assertThat(errorListener2.results).isEmpty()
        } finally {
            Dispatchers.setMain(MoreExecutors.directExecutor().asCoroutineDispatcher())
        }
    }

    @Test
    fun reportInvalidSurface_secondarySessionConfig_notifiesSecondaryErrorListener() {
        // Arrange
        val primarySurface = createTestDeferrableSurface()
        val secondarySurface = createTestDeferrableSurface().apply { close() }
        val primaryErrorResults = mutableListOf<Pair<SessionConfig, SessionConfig.SessionError>>()
        val secondaryErrorResults = mutableListOf<Pair<SessionConfig, SessionConfig.SessionError>>()

        val fakeTestUseCase = createFakeTestUseCase {
            it.setupSessionConfigs(
                primarySessionConfigBuilder =
                    SessionConfig.Builder().apply {
                        setTemplateType(TEMPLATE_PREVIEW)
                        addSurface(primarySurface)
                        setErrorListener { sessionConfig, error ->
                            primaryErrorResults.add(Pair(sessionConfig, error))
                        }
                    },
                secondarySessionConfigBuilder =
                    SessionConfig.Builder().apply {
                        setTemplateType(TEMPLATE_PREVIEW)
                        addSurface(secondarySurface)
                        setErrorListener { sessionConfig, error ->
                            secondaryErrorResults.add(Pair(sessionConfig, error))
                        }
                    },
            )
        }

        // Act
        SessionConfigAdapter(useCases = listOf(fakeTestUseCase), isPrimary = false)
            .reportSurfaceInvalid(secondarySurface)

        // Assert
        assertThat(primaryErrorResults).isEmpty()
        assertThat(secondaryErrorResults).hasSize(1)
        assertThat(secondaryErrorResults[0].first).isEqualTo(fakeTestUseCase.secondarySessionConfig)
        assertThat(secondaryErrorResults[0].second)
            .isEqualTo(SessionConfig.SessionError.SESSION_ERROR_SURFACE_NEEDS_RESET)
    }

    @Test
    fun reportInvalidSurface_postviewSurface_notifiesErrorListener() {
        // Arrange
        val primarySurface = createTestDeferrableSurface()
        val postviewSurface = createTestDeferrableSurface().apply { close() }
        val errorResults = mutableListOf<Pair<SessionConfig, SessionConfig.SessionError>>()

        val fakeTestUseCase = createFakeTestUseCase {
            it.setupSessionConfig(
                SessionConfig.Builder().apply {
                    setTemplateType(TEMPLATE_PREVIEW)
                    addSurface(primarySurface)
                    setPostviewSurface(postviewSurface)
                    setErrorListener { sessionConfig, error ->
                        errorResults.add(Pair(sessionConfig, error))
                    }
                }
            )
        }

        // Act
        SessionConfigAdapter(useCases = listOf(fakeTestUseCase))
            .reportSurfaceInvalid(postviewSurface)

        // Assert
        assertThat(errorResults).hasSize(1)
        assertThat(errorResults[0].first).isEqualTo(fakeTestUseCase.sessionConfig)
        assertThat(errorResults[0].second)
            .isEqualTo(SessionConfig.SessionError.SESSION_ERROR_SURFACE_NEEDS_RESET)
    }

    @Test
    fun populateSurfaceToStreamUseCaseMappingEmptyUseCase() {
        val mapping = sessionConfigAdapter.getSurfaceToStreamUseCaseMapping(listOf(), listOf())
        TestCase.assertTrue(mapping.isEmpty())
    }

    @Test
    fun populateSurfaceToStreamUseHintMappingEmptyUseCase() {
        val mapping = sessionConfigAdapter.getSurfaceToStreamUseHintMapping(listOf())
        TestCase.assertTrue(mapping.isEmpty())
    }

    @Test
    fun populateSurfaceToStreamUseHintMapping_setStreamUseHintOption_streamUseHintIsSet() {
        // Arrange.
        val fakeSurface1 = createTestDeferrableSurface()
        val fakeSessionConfig1 =
            createFakeSessionConfig(
                surface = fakeSurface1,
                options =
                    MutableOptionsBundle.create().apply {
                        insertOption(
                            Camera2ImplConfig.STREAM_USE_HINT_OPTION,
                            StreamUseHint.DEFAULT.value,
                        )
                    },
            )
        val fakeSurface2 = createTestDeferrableSurface()
        val fakeSessionConfig2 =
            createFakeSessionConfig(
                surface = fakeSurface2,
                options =
                    MutableOptionsBundle.create().apply {
                        insertOption(
                            Camera2ImplConfig.STREAM_USE_HINT_OPTION,
                            StreamUseHint.VIDEO_RECORD.value,
                        )
                    },
            )

        // Act.
        val mapping: Map<DeferrableSurface, Long> =
            sessionConfigAdapter.getSurfaceToStreamUseHintMapping(
                listOf(fakeSessionConfig1, fakeSessionConfig2)
            )

        // Assert.
        assertThat(mapping[fakeSurface1]).isEqualTo(StreamUseHint.DEFAULT.value)
        assertThat(mapping[fakeSurface2]).isEqualTo(StreamUseHint.VIDEO_RECORD.value)
    }

    @Test
    fun populateSurfaceToStreamUseHintMapping_useContainerClass_streamUseHintIsSet() {
        // Arrange.
        val fakePreviewSurface = createTestDeferrableSurface(containerClass = Preview::class.java)
        val fakePreviewSessionConfig = createFakeSessionConfig(fakePreviewSurface)
        val fakeVideoSurface = createTestDeferrableSurface(containerClass = MediaCodec::class.java)
        val fakeVideoSessionConfig = createFakeSessionConfig(fakeVideoSurface)
        val fakeStreamSharingSurface =
            createTestDeferrableSurface(containerClass = StreamSharing::class.java)
        val fakeStreamSharingSessionConfig = createFakeSessionConfig(fakeStreamSharingSurface)

        // Act.
        val mapping: Map<DeferrableSurface, Long> =
            sessionConfigAdapter.getSurfaceToStreamUseHintMapping(
                listOf(
                    fakePreviewSessionConfig,
                    fakeVideoSessionConfig,
                    fakeStreamSharingSessionConfig,
                )
            )

        // Assert.
        assertThat(mapping[fakePreviewSurface]).isEqualTo(StreamUseHint.DEFAULT.value)
        assertThat(mapping[fakeVideoSurface]).isEqualTo(StreamUseHint.VIDEO_RECORD.value)
        assertThat(mapping[fakeStreamSharingSurface]).isEqualTo(StreamUseHint.DEFAULT.value)
    }

    @Test
    fun populateSurfaceToStreamUseHintMapping_setHintOptionAndContainerClass_optionHintWins() {
        // Arrange.
        val fakeSurface1 = createTestDeferrableSurface(containerClass = Preview::class.java)
        val fakeSessionConfig1 =
            createFakeSessionConfig(
                surface = fakeSurface1,
                options =
                    MutableOptionsBundle.create().apply {
                        insertOption(
                            Camera2ImplConfig.STREAM_USE_HINT_OPTION,
                            StreamUseHint.VIDEO_RECORD.value,
                        )
                    },
            )
        val fakeSurface2 = createTestDeferrableSurface(containerClass = MediaCodec::class.java)
        val fakeSessionConfig2 =
            createFakeSessionConfig(
                surface = fakeSurface2,
                options =
                    MutableOptionsBundle.create().apply {
                        insertOption(
                            Camera2ImplConfig.STREAM_USE_HINT_OPTION,
                            StreamUseHint.DEFAULT.value,
                        )
                    },
            )

        // Act.
        val mapping: Map<DeferrableSurface, Long> =
            sessionConfigAdapter.getSurfaceToStreamUseHintMapping(
                listOf(fakeSessionConfig1, fakeSessionConfig2)
            )

        // Assert.
        assertThat(mapping[fakeSurface1]).isEqualTo(StreamUseHint.VIDEO_RECORD.value)
        assertThat(mapping[fakeSurface2]).isEqualTo(StreamUseHint.DEFAULT.value)
    }

    @Test
    fun sessionInteropConfig_overridesTemplateTypeAndPropagatesCallbacksAndOptions() {
        val surface = createTestDeferrableSurface()
        val videoUseCase = createFakeTestUseCase {
            it.setupSessionConfig(
                SessionConfig.Builder().apply {
                    setTemplateType(TEMPLATE_RECORD)
                    addSurface(surface)
                }
            )
        }
        val deviceCallback =
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {}

                override fun onDisconnected(camera: CameraDevice) {}

                override fun onError(camera: CameraDevice, error: Int) {}
            }
        val sessionCallback =
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {}

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            }
        val repeatingCallback = object : CameraCaptureSession.CaptureCallback() {}
        val sessionParamOpt = CaptureRequest.CONTROL_AWB_MODE.createSessionParameterOption()
        val interopConfig =
            MutableOptionsBundle.create().apply {
                insertOption(Camera2ImplConfig.TEMPLATE_TYPE_OPTION, TEMPLATE_PREVIEW)
                insertOption(Camera2ImplConfig.DEVICE_STATE_CALLBACK_OPTION, deviceCallback)
                insertOption(Camera2ImplConfig.SESSION_STATE_CALLBACK_OPTION, sessionCallback)
                insertOption(
                    Camera2ImplConfig.SESSION_REPEATING_CAPTURE_CALLBACK_OPTION,
                    repeatingCallback,
                )
                insertOption(Camera2ImplConfig.SESSION_PHYSICAL_CAMERA_ID_OPTION, "2")
                insertOption(Camera2ImplConfig.SESSION_TYPE_OPTION, 1)
                if (Build.VERSION.SDK_INT >= 34) {
                    insertOption(
                        Camera2ImplConfig.SESSION_COLOR_SPACE_OPTION,
                        ColorSpace.Named.DISPLAY_P3.ordinal,
                    )
                }
                insertOption(
                    sessionParamOpt,
                    androidx.camera.core.impl.Config.OptionPriority.ALWAYS_OVERRIDE,
                    CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT,
                )
                insertOption(
                    CaptureRequest.CONTROL_AE_MODE.createCaptureRequestOption(),
                    androidx.camera.core.impl.Config.OptionPriority.ALWAYS_OVERRIDE,
                    CaptureRequest.CONTROL_AE_MODE_OFF,
                )
            }

        val resolved =
            SessionConfigAdapter(
                    useCases = listOf(videoUseCase),
                    sessionInteropConfig = interopConfig,
                )
                .getValidSessionConfigOrNull()

        assertThat(resolved).isNotNull()
        assertThat(resolved!!.templateType).isEqualTo(TEMPLATE_PREVIEW)
        assertThat(resolved.deviceStateCallbacks).contains(deviceCallback)
        assertThat(resolved.sessionStateCallbacks).contains(sessionCallback)
        assertThat(resolved.repeatingCameraCaptureCallbacks)
            .contains(CameraUseCaseAdapter.CaptureCallbackContainer.create(repeatingCallback))
        val resolvedCamera2Config = Camera2ImplConfig(resolved.implementationOptions)
        assertThat(resolvedCamera2Config.getPhysicalCameraId(null)).isEqualTo("2")
        assertThat(resolvedCamera2Config.getSessionType(-1)).isEqualTo(1)
        if (Build.VERSION.SDK_INT >= 34) {
            assertThat(resolvedCamera2Config.getColorSpace(null))
                .isEqualTo(ColorSpace.Named.DISPLAY_P3.ordinal)
        }
        assertThat(resolved.implementationOptions.retrieveOption(sessionParamOpt))
            .isEqualTo(CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT)
        assertThat(
                resolvedCamera2Config.getCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, -1)
            )
            .isEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF)
    }

    @Test
    @Config(minSdk = 33)
    fun sessionInteropConfig_withZslTemplate_suppressesStreamUseCaseMapping() {
        val surface = createTestDeferrableSurface()
        val useCaseConfig =
            FakeUseCaseConfig.Builder(UseCaseConfigFactory.CaptureType.PREVIEW)
                .apply {
                    mutableConfig.insertOption(
                        UseCaseConfig.OPTION_INPUT_FORMAT,
                        ImageFormat.PRIVATE,
                    )
                }
                .useCaseConfig
        val previewUseCase =
            FakeTestUseCase(useCaseConfig).apply {
                setupSessionConfig(
                    SessionConfig.Builder().apply {
                        setTemplateType(TEMPLATE_PREVIEW)
                        addSurface(surface)
                        addImplementationOptions(
                            MutableOptionsBundle.create().apply {
                                insertOption(
                                    androidx.camera.camera2.internal.StreamUseCaseUtil
                                        .STREAM_USE_CASE_STREAM_SPEC_OPTION,
                                    android.hardware.camera2.CameraMetadata
                                        .SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW
                                        .toLong(),
                                )
                            }
                        )
                    }
                )
            }
        val zslInteropConfig =
            MutableOptionsBundle.create().apply {
                insertOption(Camera2ImplConfig.TEMPLATE_TYPE_OPTION, TEMPLATE_ZERO_SHUTTER_LAG)
            }

        val nonZslAdapter = SessionConfigAdapter(useCases = listOf(previewUseCase))
        val zslAdapter =
            SessionConfigAdapter(
                useCases = listOf(previewUseCase),
                sessionInteropConfig = zslInteropConfig,
            )

        assertThat(nonZslAdapter.surfaceConfigOptionsMap[surface]?.streamUseCase).isNotNull()
        assertThat(zslAdapter.surfaceConfigOptionsMap[surface]?.streamUseCase).isNull()
    }

    @Test
    fun sessionInteropConfig_withEmptyUseCases_keepsAdapterInvalid() {
        val interopConfig =
            MutableOptionsBundle.create().apply {
                insertOption(Camera2ImplConfig.TEMPLATE_TYPE_OPTION, TEMPLATE_RECORD)
            }

        val adapter =
            SessionConfigAdapter(useCases = emptyList(), sessionInteropConfig = interopConfig)

        assertThat(adapter.isSessionConfigValid()).isFalse()
        assertThat(adapter.getValidSessionConfigOrNull()).isNull()
    }

    private fun createFakeTestUseCase(block: (FakeTestUseCase) -> Unit): FakeTestUseCase = run {
        val configBuilder = FakeUseCaseConfig.Builder().setTargetName("UseCase")
        FakeTestUseCase(configBuilder.useCaseConfig).also { block(it) }
    }

    private fun createTestDeferrableSurface(containerClass: Class<*>? = null) =
        TestDeferrableSurface().apply {
            deferrableSurfacesToClose.add(this)
            terminationFuture.addListener({ cleanUp() }, MoreExecutors.directExecutor())
            containerClass?.let { setContainerClass(it) }
        }

    private fun createFakeSessionConfig(
        surface: DeferrableSurface,
        template: Int = TEMPLATE_PREVIEW,
        expectedFrameRateRange: Range<Int> = Range(15, 24),
        options: androidx.camera.core.impl.Config? = null,
    ) =
        SessionConfig.Builder()
            .apply {
                addSurface(surface)
                setTemplateType(template)
                setExpectedFrameRateRange(expectedFrameRateRange)
                options?.let { setImplementationOptions(it) }
            }
            .build()
}

class FakeTestUseCase(config: FakeUseCaseConfig) : FakeUseCase(config) {
    var cameraControlReady = false

    fun setupSessionConfig(sessionConfigBuilder: SessionConfig.Builder) {
        updateSessionConfig(listOf(sessionConfigBuilder.build()))
        notifyActive()
    }

    fun setupSessionConfigs(
        primarySessionConfigBuilder: SessionConfig.Builder,
        secondarySessionConfigBuilder: SessionConfig.Builder,
    ) {
        updateSessionConfig(
            listOf(primarySessionConfigBuilder.build(), secondarySessionConfigBuilder.build())
        )
        notifyActive()
    }

    override fun onCameraControlReady() {
        cameraControlReady = true
    }
}

open class TestDeferrableSurface : DeferrableSurface() {
    private val surfaceTexture = SurfaceTexture(0).also { it.setDefaultBufferSize(0, 0) }
    val testSurface = Surface(surfaceTexture)

    override fun provideSurface(): ListenableFuture<Surface> {
        return Futures.immediateFuture(testSurface)
    }

    fun cleanUp() {
        testSurface.release()
        surfaceTexture.release()
    }
}

class BlockingTestDeferrableSurface : TestDeferrableSurface() {
    private val deferred = CompletableDeferred<Surface>()

    override fun provideSurface(): ListenableFuture<Surface> {
        return deferred.asListenableFuture()
    }

    fun resume() = deferred.complete(testSurface)
}
