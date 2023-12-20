/*
 * Copyright 2022 The Android Open Source Project
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

import android.hardware.camera2.CameraDevice
import android.os.Build
import android.view.Surface
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.integration.adapter.FakeTestUseCase
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.adapter.SessionConfigAdapter
import androidx.camera.camera2.pipe.integration.adapter.TestDeferrableSurface
import androidx.camera.camera2.pipe.integration.adapter.asListenableFuture
import androidx.camera.camera2.pipe.integration.compat.workaround.NoOpInactiveSurfaceCloser
import androidx.camera.camera2.pipe.integration.testing.FakeCameraGraph
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.SessionConfig
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.test.core.app.ApplicationProvider
import androidx.testutils.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@DoNotInstrument
class UseCaseSurfaceManagerTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule(useCaseThreads.backgroundDispatcher)

    companion object {
        private val executor = MoreExecutors.directExecutor()
        private val useCaseThreads by lazy {
            val dispatcher = executor.asCoroutineDispatcher()
            val cameraScope = CoroutineScope(
                SupervisorJob() +
                    dispatcher +
                    CoroutineName("UseCaseSurfaceManagerTest")
            )

            UseCaseThreads(
                cameraScope,
                executor,
                dispatcher
            )
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
        UseCaseSurfaceManager(
            useCaseThreads,
            CameraPipe(CameraPipe.Config(ApplicationProvider.getApplicationContext())),
            NoOpInactiveSurfaceCloser,
        ).setupAsync(
            graph = fakeGraph,
            sessionConfigAdapter = SessionConfigAdapter(
                useCases = listOf(fakeTestUseCase1, fakeTestUseCase2)
            ),
            surfaceToStreamMap = mapOf(
                testDeferrableSurface1 to StreamId(0),
                testDeferrableSurface2 to StreamId(1)
            ),
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
        UseCaseSurfaceManager(
            useCaseThreads,
            CameraPipe(CameraPipe.Config(ApplicationProvider.getApplicationContext())),
            NoOpInactiveSurfaceCloser,
        ).setupAsync(
            graph = fakeGraph,
            sessionConfigAdapter = SessionConfigAdapter(
                useCases = listOf(fakeTestUseCase1, fakeTestUseCase2)
            ),
            surfaceToStreamMap = deferrableSurfaceToStreamId,
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
    fun setupNeverCompleteDeferrableSurface_shouldTimeout() = runBlocking {
        // Arrange
        val neverCompleteDeferrableSurface = object : DeferrableSurface() {
            val provideSurfaceDeferred = CompletableDeferred<Surface>()
            override fun provideSurface(): ListenableFuture<Surface> {
                return provideSurfaceDeferred.asListenableFuture()
            }

            fun cleanUp() {
                provideSurfaceDeferred.cancel()
                close()
            }
        }
        val errorListener = object : SessionConfig.ErrorListener {
            val results = mutableListOf<Pair<SessionConfig, SessionConfig.SessionError>>()
            override fun onError(sessionConfig: SessionConfig, error: SessionConfig.SessionError) {
                results.add(Pair(sessionConfig, error))
            }
        }
        val fakeTestUseCase = createFakeTestUseCase {
            it.setupSessionConfig(SessionConfig.Builder().also { sessionConfigBuilder ->
                sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                sessionConfigBuilder.addSurface(neverCompleteDeferrableSurface)
                sessionConfigBuilder.addErrorListener(errorListener)
            })
        }

        val fakeGraph = FakeCameraGraph()
        val deferrableSurfaceToStreamId: Map<DeferrableSurface, StreamId> = mapOf(
            neverCompleteDeferrableSurface to StreamId(0),
        )

        // Act
        UseCaseSurfaceManager(
            useCaseThreads,
            CameraPipe(CameraPipe.Config(ApplicationProvider.getApplicationContext())),
            NoOpInactiveSurfaceCloser,
        ).setupAsync(
            graph = fakeGraph,
            sessionConfigAdapter = SessionConfigAdapter(
                useCases = listOf(fakeTestUseCase)
            ),
            surfaceToStreamMap = deferrableSurfaceToStreamId,
            timeoutMillis = TimeUnit.SECONDS.toMillis(1)
        ).await()

        // Assert, verify it is no-op for the getSurface timeout case.
        assertThat(fakeGraph.setSurfaceResults.size).isEqualTo(0)
        assertThat(errorListener.results.size).isEqualTo(0)

        // Clean up
        neverCompleteDeferrableSurface.cleanUp()
    }

    @Test
    fun stopNeverCompleteTask_shouldCancelSurfaceSetup() = runBlocking {
        // Arrange
        val neverCompleteDeferrableSurface = object : DeferrableSurface() {
            val provideSurfaceDeferred = CompletableDeferred<Surface>()
            val provideSurfaceIsCalledDeferred = CompletableDeferred<Unit>()

            override fun provideSurface(): ListenableFuture<Surface> {
                try {
                    return provideSurfaceDeferred.asListenableFuture()
                } finally {
                    provideSurfaceIsCalledDeferred.complete(Unit)
                }
            }

            fun cleanUp() {
                close()
                provideSurfaceDeferred.cancel()
            }
        }
        val errorListener = object : SessionConfig.ErrorListener {
            val results = mutableListOf<Pair<SessionConfig, SessionConfig.SessionError>>()
            override fun onError(sessionConfig: SessionConfig, error: SessionConfig.SessionError) {
                results.add(Pair(sessionConfig, error))
            }
        }
        val fakeTestUseCase = createFakeTestUseCase {
            it.setupSessionConfig(
                SessionConfig.Builder().also { sessionConfigBuilder ->
                    sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                    sessionConfigBuilder.addSurface(neverCompleteDeferrableSurface)
                    sessionConfigBuilder.addErrorListener(errorListener)
                }
            )
        }

        val fakeGraph = FakeCameraGraph()
        val deferrableSurfaceToStreamId: Map<DeferrableSurface, StreamId> = mapOf(
            neverCompleteDeferrableSurface to StreamId(0),
        )
        val useCaseSurfaceManager = UseCaseSurfaceManager(
            useCaseThreads,
            CameraPipe(CameraPipe.Config(ApplicationProvider.getApplicationContext())),
            NoOpInactiveSurfaceCloser,
        )
        val deferred = useCaseSurfaceManager.setupAsync(
            graph = fakeGraph,
            sessionConfigAdapter = SessionConfigAdapter(
                useCases = listOf(fakeTestUseCase)
            ),
            surfaceToStreamMap = deferrableSurfaceToStreamId,
            timeoutMillis = TimeUnit.SECONDS.toMillis(60)
        )
        neverCompleteDeferrableSurface.provideSurfaceIsCalledDeferred.await()

        // Act.
        useCaseSurfaceManager.stopAsync()

        // Assert, verify no further error/setSurface for the stopped case.
        assertThat(deferred.isCancelled).isTrue()
        assertThat(fakeGraph.setSurfaceResults.size).isEqualTo(0)
        assertThat(errorListener.results.size).isEqualTo(0)
        // The return ListenableFuture of DeferrableSurface#getSurface() should never be cancelled.
        assertThat(neverCompleteDeferrableSurface.surface.isCancelled).isFalse()

        // Clean up
        neverCompleteDeferrableSurface.cleanUp()
    }

    private fun createFakeTestUseCase(block: (FakeTestUseCase) -> Unit) =
        FakeTestUseCase(
            FakeUseCaseConfig.Builder().setTargetName("UseCase").useCaseConfig
        ).also {
            block(it)
        }

    private fun createTestDeferrableSurface() = TestDeferrableSurface().also {
        it.terminationFuture.addListener({ it.cleanUp() }, useCaseThreads.backgroundExecutor)
    }
}
