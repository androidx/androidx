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
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.os.Build
import android.util.Range
import android.view.Surface
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.testutils.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import junit.framework.TestCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.asCoroutineDispatcher
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

        // Clean up
        testDeferrableSurface.close()
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
                    error: SessionConfig.SessionError
                ) {
                    results.add(Pair(sessionConfig, error))
                }
            }

        val fakeTestUseCase1 = createFakeTestUseCase {
            it.setupSessionConfig(
                SessionConfig.Builder().also { sessionConfigBuilder ->
                    sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                    sessionConfigBuilder.addSurface(testDeferrableSurface)
                    sessionConfigBuilder.setErrorListener(errorListener)
                }
            )
        }
        val fakeTestUseCase2 = createFakeTestUseCase {
            it.setupSessionConfig(
                SessionConfig.Builder().also { sessionConfigBuilder ->
                    sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
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

        // Clean up
        testDeferrableSurface.close()
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

    private fun createFakeTestUseCase(block: (FakeTestUseCase) -> Unit): FakeTestUseCase = run {
        val configBuilder = FakeUseCaseConfig.Builder().setTargetName("UseCase")
        FakeTestUseCase(configBuilder.useCaseConfig).also { block(it) }
    }

    private fun createTestDeferrableSurface(): TestDeferrableSurface = run {
        TestDeferrableSurface().also {
            it.terminationFuture.addListener({ it.cleanUp() }, MoreExecutors.directExecutor())
        }
    }
}

class FakeTestUseCase(
    config: FakeUseCaseConfig,
) : FakeUseCase(config) {
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
