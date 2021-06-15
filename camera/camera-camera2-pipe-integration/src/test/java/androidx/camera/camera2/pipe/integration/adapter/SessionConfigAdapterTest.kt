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
import android.os.Build
import android.view.Surface
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.StreamGraph
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.testing.fakes.FakeUseCase
import androidx.camera.testing.fakes.FakeUseCaseConfig
import androidx.testutils.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import java.util.concurrent.Executors

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@DoNotInstrument
class SessionConfigAdapterTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule(useCaseThreads.backgroundDispatcher)

    companion object {
        private val executor = Executors.newSingleThreadExecutor()
        private val useCaseThreads by lazy {
            val dispatcher = executor.asCoroutineDispatcher()
            val cameraScope = CoroutineScope(
                Job() +
                    dispatcher +
                    CoroutineName("SessionConfigAdapterTest")
            )

            UseCaseThreads(
                cameraScope,
                executor,
                dispatcher
            )
        }

        @JvmStatic
        @AfterClass
        fun close() {
            executor.shutdown()
        }
    }

    @Test
    fun setupSurface_deferrableSurfaceClosed_notifyError() = runBlocking {
        // Arrange, create DeferrableSurface and invoke DeferrableSurface#close() immediately to
        // close the Surface and we expect the DeferrableSurface.getSurface() will return a
        // {@link SurfaceClosedException}.
        val testDeferrableSurface1 = createTestDeferrableSurface().also { it.close() }
        val testDeferrableSurface2 = createTestDeferrableSurface().also { it.close() }

        val errorListener = object : SessionConfig.ErrorListener {
            val results = mutableListOf<Pair<SessionConfig, SessionConfig.SessionError>>()
            override fun onError(sessionConfig: SessionConfig, error: SessionConfig.SessionError) {
                results.add(Pair(sessionConfig, error))
            }
        }

        val fakeTestUseCase1 = createFakeTestUseCase {
            it.setupSessionConfig(
                SessionConfig.Builder().also { sessionConfigBuilder ->
                    sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                    sessionConfigBuilder.addSurface(testDeferrableSurface1)
                    sessionConfigBuilder.addErrorListener(errorListener)
                }
            )
        }
        val fakeTestUseCase2 = createFakeTestUseCase {
            it.setupSessionConfig(
                SessionConfig.Builder().also { sessionConfigBuilder ->
                    sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                    sessionConfigBuilder.addSurface(testDeferrableSurface2)
                    sessionConfigBuilder.addErrorListener(errorListener)
                }
            )
        }

        val fakeGraph = FakeCameraGraph()

        // Act
        SessionConfigAdapter(
            useCases = listOf(fakeTestUseCase1, fakeTestUseCase2), threads = useCaseThreads
        ).setupSurfaceAsync(
            fakeGraph,
            mapOf(
                testDeferrableSurface1 to StreamId(0),
                testDeferrableSurface2 to StreamId(1)
            )
        ).await()

        // Assert, verify it only reports the SURFACE_NEEDS_RESET error on one SessionConfig
        // at a time.
        assertThat(fakeGraph.setSurfaceResults.size).isEqualTo(0)
        assertThat(errorListener.results.size).isEqualTo(1)
        assertThat(errorListener.results[0].second).isEqualTo(
            SessionConfig.SessionError.SESSION_ERROR_SURFACE_NEEDS_RESET
        )
    }

    @Test
    fun setupSurface_surfacesShouldSetToGraph() = runBlocking {
        // Arrange
        val testDeferrableSurface1 = createTestDeferrableSurface()
        val testDeferrableSurface2 = createTestDeferrableSurface()
        val fakeTestUseCase1 = createFakeTestUseCase {
            it.setupSessionConfig(
                SessionConfig.Builder().also { sessionConfigBuilder ->
                    sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                    sessionConfigBuilder.addSurface(testDeferrableSurface1)
                }
            )
        }
        val fakeTestUseCase2 = createFakeTestUseCase {
            it.setupSessionConfig(
                SessionConfig.Builder().also { sessionConfigBuilder ->
                    sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                    sessionConfigBuilder.addSurface(testDeferrableSurface2)
                }
            )
        }

        val fakeGraph = FakeCameraGraph()
        val deferrableSurfaceToStreamId: Map<DeferrableSurface, StreamId> = mapOf(
            testDeferrableSurface1 to StreamId(0),
            testDeferrableSurface2 to StreamId(1)
        )

        // Act
        SessionConfigAdapter(
            useCases = listOf(fakeTestUseCase1, fakeTestUseCase2), threads = useCaseThreads
        ).setupSurfaceAsync(
            fakeGraph, deferrableSurfaceToStreamId
        ).await()

        // Assert, 2 surfaces from the fakeTestUseCase1 and fakeTestUseCase2 should be set to the
        // Graph
        assertThat(fakeGraph.setSurfaceResults).isEqualTo(
            deferrableSurfaceToStreamId.map {
                it.value to (it.key as TestDeferrableSurface).testSurface
            }.toMap()
        )

        // Clean up
        testDeferrableSurface1.close()
        testDeferrableSurface2.close()
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
        val sessionConfigAdapter = SessionConfigAdapter(
            useCases = listOf(fakeTestUseCase), threads = useCaseThreads
        )

        // Assert
        assertThat(sessionConfigAdapter.isSessionConfigValid()).isFalse()
        assertThat(sessionConfigAdapter.getValidSessionConfigOrNull()).isNull()

        // Clean up
        testDeferrableSurface.close()
    }

    private fun createFakeTestUseCase(block: (FakeTestUseCase) -> Unit): FakeTestUseCase = run {
        val configBuilder = FakeUseCaseConfig.Builder().setTargetName("UseCase")
        FakeTestUseCase(configBuilder.useCaseConfig).also {
            block(it)
        }
    }

    private fun createTestDeferrableSurface(): TestDeferrableSurface = run {
        TestDeferrableSurface().also {
            it.terminationFuture.addListener({ it.cleanUp() }, useCaseThreads.backgroundExecutor)
        }
    }
}

private class FakeTestUseCase(
    config: FakeUseCaseConfig,
) : FakeUseCase(config) {

    fun setupSessionConfig(sessionConfigBuilder: SessionConfig.Builder) {
        updateSessionConfig(sessionConfigBuilder.build())
        notifyActive()
    }
}

private class TestDeferrableSurface : DeferrableSurface() {
    private val surfaceTexture = SurfaceTexture(0).also {
        it.setDefaultBufferSize(0, 0)
    }
    val testSurface = Surface(surfaceTexture)

    override fun provideSurface(): ListenableFuture<Surface> {
        return Futures.immediateFuture(testSurface)
    }

    fun cleanUp() {
        testSurface.release()
        surfaceTexture.release()
    }
}

private class FakeCameraGraph : CameraGraph {
    val setSurfaceResults = mutableMapOf<StreamId, Surface?>()

    override val streams: StreamGraph
        get() = throw NotImplementedError("Not used in testing")

    override suspend fun acquireSession(): CameraGraph.Session {
        throw NotImplementedError("Not used in testing")
    }

    override fun acquireSessionOrNull(): CameraGraph.Session? {
        throw NotImplementedError("Not used in testing")
    }

    override fun close() {
        throw NotImplementedError("Not used in testing")
    }

    override fun setSurface(stream: StreamId, surface: Surface?) {
        setSurfaceResults[stream] = surface
    }

    override fun start() {
        throw NotImplementedError("Not used in testing")
    }

    override fun stop() {
        throw NotImplementedError("Not used in testing")
    }
}
