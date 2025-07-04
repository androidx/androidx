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

package androidx.camera.camera2.pipe.integration.adapter

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.media.MediaCodec
import android.os.Build
import android.util.Range
import android.view.Surface
import androidx.camera.camera2.pipe.OutputStream.StreamUseHint
import androidx.camera.camera2.pipe.integration.impl.STREAM_USE_HINT_OPTION
import androidx.camera.core.Preview
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.SessionConfig
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
import kotlinx.coroutines.asCoroutineDispatcher
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@DoNotInstrument
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

    @Test
    fun getExpectedFrameRateRange() {
        // Arrange
        val testDeferrableSurface = createTestDeferrableSurface()

        // Create an invalid SessionConfig which doesn't set the template
        val fakeTestUseCase = createFakeTestUseCase {
            it.setupSessionConfig(
                SessionConfig.Builder().also { sessionConfigBuilder ->
                    sessionConfigBuilder.addSurface(testDeferrableSurface)
                    sessionConfigBuilder.setTemplateType(TEMPLATE_PREVIEW)
                    sessionConfigBuilder.setExpectedFrameRateRange(Range(15, 24))
                }
            )
        }

        // Act
        val sessionConfigAdapter = SessionConfigAdapter(useCases = listOf(fakeTestUseCase))

        // Assert
        assertThat(sessionConfigAdapter.isSessionConfigValid()).isTrue()
        assertThat(sessionConfigAdapter.getValidSessionConfigOrNull()).isNotNull()
        assertThat(sessionConfigAdapter.getExpectedFrameRateRange()).isEqualTo(Range(15, 24))
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
                        insertOption(STREAM_USE_HINT_OPTION, StreamUseHint.DEFAULT.value)
                    },
            )
        val fakeSurface2 = createTestDeferrableSurface()
        val fakeSessionConfig2 =
            createFakeSessionConfig(
                surface = fakeSurface2,
                options =
                    MutableOptionsBundle.create().apply {
                        insertOption(STREAM_USE_HINT_OPTION, StreamUseHint.VIDEO_RECORD.value)
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
                        insertOption(STREAM_USE_HINT_OPTION, StreamUseHint.VIDEO_RECORD.value)
                    },
            )
        val fakeSurface2 = createTestDeferrableSurface(containerClass = MediaCodec::class.java)
        val fakeSessionConfig2 =
            createFakeSessionConfig(
                surface = fakeSurface2,
                options =
                    MutableOptionsBundle.create().apply {
                        insertOption(STREAM_USE_HINT_OPTION, StreamUseHint.DEFAULT.value)
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
