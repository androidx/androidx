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

package androidx.camera.camera2.pipe.graph

import android.content.Context
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
import android.os.Build
import androidx.camera.camera2.pipe.CameraBackendFactory
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.internal.CameraBackendsImpl
import androidx.camera.camera2.pipe.testing.FakeCameraBackend
import androidx.camera.camera2.pipe.testing.CameraControllerSimulator
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeGraphProcessor
import androidx.camera.camera2.pipe.testing.FakeThreads
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class CameraGraphImplTest {
    private val context = ApplicationProvider.getApplicationContext() as Context
    private val metadata = FakeCameraMetadata(
        mapOf(INFO_SUPPORTED_HARDWARE_LEVEL to INFO_SUPPORTED_HARDWARE_LEVEL_FULL),
    )
    private val fakeGraphProcessor = FakeGraphProcessor()
    private lateinit var cameraController: CameraControllerSimulator

    private fun initializeCameraGraphImpl(scope: TestScope): CameraGraphImpl {
        val graphConfig = CameraGraph.Config(
            camera = metadata.camera,
            streams = listOf(),
        )
        val threads = FakeThreads.fromTestScope(scope)
        val backend = FakeCameraBackend(
            fakeCameras = mapOf(metadata.camera to metadata)
        )
        val backends = CameraBackendsImpl(
            defaultBackendId = backend.id,
            cameraBackends = mapOf(backend.id to CameraBackendFactory { backend }),
            context,
            threads
        )
        val cameraContext = CameraBackendsImpl.CameraBackendContext(
            context,
            threads,
            backends
        )
        val streamGraph = StreamGraphImpl(
            metadata,
            graphConfig
        )
        cameraController = CameraControllerSimulator(
            cameraContext,
            graphConfig,
            fakeGraphProcessor,
            streamGraph
        )
        val surfaceGraph = SurfaceGraph(streamGraph, cameraController)
        return CameraGraphImpl(
            graphConfig,
            metadata,
            fakeGraphProcessor,
            streamGraph,
            surfaceGraph,
            cameraController,
            GraphState3A(),
            Listener3A()
        )
    }

    @Test
    fun createCameraGraphImpl() = runTest {
        val cameraGraphImpl = initializeCameraGraphImpl(this)
        assertThat(cameraGraphImpl).isNotNull()
    }

    @Test
    fun testAcquireSession() = runTest {
        val cameraGraphImpl = initializeCameraGraphImpl(this)
        val session = cameraGraphImpl.acquireSession()
        assertThat(session).isNotNull()
    }

    @Test
    fun testAcquireSessionOrNull() = runTest {
        val cameraGraphImpl = initializeCameraGraphImpl(this)
        val session = cameraGraphImpl.acquireSessionOrNull()
        assertThat(session).isNotNull()
    }

    @Test
    fun testAcquireSessionOrNullAfterAcquireSession() = runTest {
        val cameraGraphImpl = initializeCameraGraphImpl(this)
        val session = cameraGraphImpl.acquireSession()
        assertThat(session).isNotNull()

        // Since a session is already active, an attempt to acquire another session will fail.
        val session1 = cameraGraphImpl.acquireSessionOrNull()
        assertThat(session1).isNull()

        // Closing an active session should allow a new session instance to be created.
        session.close()

        val session2 = cameraGraphImpl.acquireSessionOrNull()
        assertThat(session2).isNotNull()
    }

    @Test
    fun sessionSubmitsRequestsToGraphProcessor() = runTest {
        val cameraGraphImpl = initializeCameraGraphImpl(this)
        val session = checkNotNull(cameraGraphImpl.acquireSessionOrNull())
        val request = Request(listOf())
        session.submit(request)
        advanceUntilIdle()

        assertThat(fakeGraphProcessor.requestQueue).contains(listOf(request))
    }

    @Test
    fun sessionSetsRepeatingRequestOnGraphProcessor() = runTest {
        val cameraGraphImpl = initializeCameraGraphImpl(this)
        val session = checkNotNull(cameraGraphImpl.acquireSessionOrNull())
        val request = Request(listOf())
        session.startRepeating(request)
        advanceUntilIdle()

        assertThat(fakeGraphProcessor.repeatingRequest).isSameInstanceAs(request)
    }

    @Test
    fun sessionAbortsRequestOnGraphProcessor() = runTest {
        val cameraGraphImpl = initializeCameraGraphImpl(this)
        val session = checkNotNull(cameraGraphImpl.acquireSessionOrNull())
        val request = Request(listOf())
        session.submit(request)
        session.abort()
        advanceUntilIdle()

        assertThat(fakeGraphProcessor.requestQueue).isEmpty()
    }

    @Test
    fun closingSessionDoesNotCloseGraphProcessor() = runTest {
        val cameraGraphImpl = initializeCameraGraphImpl(this)
        val session = cameraGraphImpl.acquireSessionOrNull()
        checkNotNull(session).close()
        advanceUntilIdle()

        assertThat(fakeGraphProcessor.closed).isFalse()
    }

    @Test
    fun closingCameraGraphClosesGraphProcessor() = runTest {
        val cameraGraphImpl = initializeCameraGraphImpl(this)
        cameraGraphImpl.close()
        assertThat(fakeGraphProcessor.closed).isTrue()
    }

    @Test
    fun stoppingCameraGraphStopsGraphProcessor() = runTest {
        val cameraGraph = initializeCameraGraphImpl(this)

        assertThat(cameraController.started).isFalse()
        assertThat(fakeGraphProcessor.closed).isFalse()
        cameraGraph.start()
        assertThat(cameraController.started).isTrue()
        cameraGraph.stop()
        assertThat(cameraController.started).isFalse()
        assertThat(fakeGraphProcessor.closed).isFalse()
        cameraGraph.start()
        assertThat(cameraController.started).isTrue()
        cameraGraph.close()
        assertThat(cameraController.started).isFalse()
        assertThat(fakeGraphProcessor.closed).isTrue()
    }
}