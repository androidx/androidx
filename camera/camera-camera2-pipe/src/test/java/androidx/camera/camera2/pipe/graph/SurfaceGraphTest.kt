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

package androidx.camera.camera2.pipe.graph

import android.content.Context
import androidx.camera.camera2.pipe.CameraBackendFactory
import androidx.camera.camera2.pipe.CameraController
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CameraSurfaceManager
import androidx.camera.camera2.pipe.MemoryEstimator
import androidx.camera.camera2.pipe.internal.CameraBackendsImpl
import androidx.camera.camera2.pipe.internal.CameraPipeLifetime
import androidx.camera.camera2.pipe.testing.CameraControllerSimulator
import androidx.camera.camera2.pipe.testing.FakeGraphConfigs
import androidx.camera.camera2.pipe.testing.FakeSurfaces
import androidx.camera.camera2.pipe.testing.FakeThreads
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.test.core.app.ApplicationProvider
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class SurfaceGraphTest {
    private val context = ApplicationProvider.getApplicationContext() as Context
    private val testScope = TestScope()
    private val cameraPipeJob = Job()
    private val cameraPipeLifetime = CameraPipeLifetime(cameraPipeJob)
    private val threads = FakeThreads.fromTestScope(testScope)
    private val graphId = CameraGraphId.nextId()
    private val config = FakeGraphConfigs
    private val backend = config.fakeCameraBackend
    private val backends =
        CameraBackendsImpl(
            defaultBackendId = backend.id,
            cameraBackends = mapOf(backend.id to CameraBackendFactory { backend }),
            context,
            threads,
            cameraPipeLifetime,
        )
    private val cameraContext = CameraBackendsImpl.CameraBackendContext(context, threads, backends)
    private val fakeCameraController =
        CameraControllerSimulator(cameraContext, graphId, config.graphConfig, mock())
    private val fakeCameraControllerProvider: () -> CameraController = { fakeCameraController }

    private val streamMap =
        StreamGraphImpl(
                config.fakeMetadata,
                config.graphConfig,
                mock(),
                fakeCameraControllerProvider,
                MemoryEstimator.create(),
            )
            .also { fakeCameraController.streamGraph = it }

    private val fakeSurfaceListener: CameraSurfaceManager.SurfaceListener = mock()
    private val cameraSurfaceManager =
        CameraSurfaceManager().also { it.addListener(fakeSurfaceListener) }
    private val surfaceGraph =
        SurfaceGraph(streamMap, fakeCameraControllerProvider, cameraSurfaceManager, emptyMap())

    private val stream1 = streamMap[config.streamConfig1]!!
    private val stream2 = streamMap[config.streamConfig2]!!
    private val stream3 = streamMap[config.streamConfig3]!!
    private val stream4 = streamMap[config.streamConfig4]!!
    private val stream5 = streamMap[config.streamConfig5]!!
    private val stream6 = streamMap[config.streamConfig6]!!
    private val stream7 = streamMap[config.streamConfig7]!!
    private val stream8 = streamMap[config.streamConfig8]!!
    private val stream9 = streamMap[config.sharedStreamConfig1]!!
    private val stream10 = streamMap[config.sharedStreamConfig2]!!

    private val fakeSurfaces = FakeSurfaces()
    private val fakeSurface1 = fakeSurfaces.createFakeSurface()
    private val fakeSurface2 = fakeSurfaces.createFakeSurface()
    private val fakeSurface3 = fakeSurfaces.createFakeSurface()
    private val fakeSurface4 = fakeSurfaces.createFakeSurface()
    private val fakeSurface5 = fakeSurfaces.createFakeSurface()
    private val fakeSurface6 = fakeSurfaces.createFakeSurface()
    private val fakeSurface7 = fakeSurfaces.createFakeSurface()
    private val fakeSurface8 = fakeSurfaces.createFakeSurface()
    private val fakeSurface9 = fakeSurfaces.createFakeSurface()
    private val fakeSurface10 = fakeSurfaces.createFakeSurface()

    @After
    fun teardown() {
        fakeSurfaces.close()
    }

    @Test
    fun outputSurfacesArePassedToControllerImmediately() {
        surfaceGraph[stream1.id] = fakeSurface1
        surfaceGraph[stream2.id] = fakeSurface2
        surfaceGraph[stream3.id] = fakeSurface3
        surfaceGraph[stream4.id] = fakeSurface4
        surfaceGraph[stream5.id] = fakeSurface5
        surfaceGraph[stream6.id] = fakeSurface6
        surfaceGraph[stream7.id] = fakeSurface7
        surfaceGraph[stream8.id] = fakeSurface8
        surfaceGraph[stream9.id] = fakeSurface9
        surfaceGraph[stream10.id] = fakeSurface10

        val surfaceMap = fakeCameraController.currentSurfaceMap
        assertThat(surfaceMap).isNotNull()
        checkNotNull(surfaceMap)
        assertThat(surfaceMap[stream1.id]).isEqualTo(fakeSurface1)
        assertThat(surfaceMap[stream2.id]).isEqualTo(fakeSurface2)
        assertThat(surfaceMap[stream3.id]).isEqualTo(fakeSurface3)
        assertThat(surfaceMap[stream4.id]).isEqualTo(fakeSurface4)
        assertThat(surfaceMap[stream5.id]).isEqualTo(fakeSurface5)
        assertThat(surfaceMap[stream6.id]).isEqualTo(fakeSurface6)
        assertThat(surfaceMap[stream7.id]).isEqualTo(fakeSurface7)
        assertThat(surfaceMap[stream8.id]).isEqualTo(fakeSurface8)
    }

    @Test
    fun outputSurfacesArePassedToListenerWhenAvailable() {
        assertThat(fakeCameraController.currentSurfaceMap).isNull()

        surfaceGraph[stream1.id] = fakeSurface1
        surfaceGraph[stream2.id] = fakeSurface2
        surfaceGraph[stream3.id] = fakeSurface3
        surfaceGraph[stream4.id] = fakeSurface4
        surfaceGraph[stream5.id] = fakeSurface5
        surfaceGraph[stream6.id] = fakeSurface6
        surfaceGraph[stream7.id] = fakeSurface7
        surfaceGraph[stream8.id] = fakeSurface8
        assertThat(fakeCameraController.currentSurfaceMap).isNull()

        surfaceGraph[stream9.id] = fakeSurface9
        surfaceGraph[stream10.id] = fakeSurface10

        val surfaceMap = fakeCameraController.currentSurfaceMap
        assertThat(surfaceMap).isNotNull()
        checkNotNull(surfaceMap)
        assertThat(surfaceMap[stream1.id]).isEqualTo(fakeSurface1)
        assertThat(surfaceMap[stream2.id]).isEqualTo(fakeSurface2)
        assertThat(surfaceMap[stream3.id]).isEqualTo(fakeSurface3)
        assertThat(surfaceMap[stream4.id]).isEqualTo(fakeSurface4)
        assertThat(surfaceMap[stream5.id]).isEqualTo(fakeSurface5)
        assertThat(surfaceMap[stream6.id]).isEqualTo(fakeSurface6)
        assertThat(surfaceMap[stream7.id]).isEqualTo(fakeSurface7)
        assertThat(surfaceMap[stream8.id]).isEqualTo(fakeSurface8)
        assertThat(surfaceMap[stream9.id]).isEqualTo(fakeSurface9)
        assertThat(surfaceMap[stream10.id]).isEqualTo(fakeSurface10)
    }

    @Test
    fun onlyMostRecentSurfacesArePassedToSession() {
        val fakeSurface1A = fakeSurfaces.createFakeSurface()
        val fakeSurface1B = fakeSurfaces.createFakeSurface()

        surfaceGraph[stream1.id] = fakeSurface1A
        surfaceGraph[stream1.id] = fakeSurface1B
        assertThat(fakeCameraController.currentSurfaceMap).isNull()

        surfaceGraph[stream2.id] = fakeSurface2
        surfaceGraph[stream3.id] = fakeSurface3
        surfaceGraph[stream4.id] = fakeSurface4
        surfaceGraph[stream5.id] = fakeSurface5
        surfaceGraph[stream6.id] = fakeSurface6
        surfaceGraph[stream7.id] = fakeSurface7
        surfaceGraph[stream8.id] = fakeSurface8
        surfaceGraph[stream9.id] = fakeSurface9
        surfaceGraph[stream10.id] = fakeSurface10

        val surfaceMap = fakeCameraController.currentSurfaceMap
        assertThat(surfaceMap).isNotNull()
        checkNotNull(surfaceMap)
        assertThat(surfaceMap[stream1.id]).isEqualTo(fakeSurface1B)
        assertThat(surfaceMap[stream2.id]).isEqualTo(fakeSurface2)
        assertThat(surfaceMap[stream3.id]).isEqualTo(fakeSurface3)
        assertThat(surfaceMap[stream4.id]).isEqualTo(fakeSurface4)
        assertThat(surfaceMap[stream5.id]).isEqualTo(fakeSurface5)
        assertThat(surfaceMap[stream6.id]).isEqualTo(fakeSurface6)
        assertThat(surfaceMap[stream7.id]).isEqualTo(fakeSurface7)
        assertThat(surfaceMap[stream8.id]).isEqualTo(fakeSurface8)
        assertThat(surfaceMap[stream9.id]).isEqualTo(fakeSurface9)
        assertThat(surfaceMap[stream10.id]).isEqualTo(fakeSurface10)
    }

    @Test
    fun newSurfacesAcquireTokens() {
        surfaceGraph[stream1.id] = fakeSurface1

        verify(fakeSurfaceListener, times(1)).onSurfaceActive(eq(fakeSurface1))
        verify(fakeSurfaceListener, never()).onSurfaceActive(eq(fakeSurface2))
        verify(fakeSurfaceListener, never()).onSurfaceActive(eq(fakeSurface3))
        verify(fakeSurfaceListener, never()).onSurfaceInactive(eq(fakeSurface1))
        verify(fakeSurfaceListener, never()).onSurfaceInactive(eq(fakeSurface2))
        verify(fakeSurfaceListener, never()).onSurfaceInactive(eq(fakeSurface3))
    }

    @Test
    fun replacingSurfacesReleasesPreviousToken() {
        surfaceGraph[stream1.id] = fakeSurface1
        surfaceGraph[stream1.id] = fakeSurface2

        verify(fakeSurfaceListener, times(1)).onSurfaceActive(eq(fakeSurface1))
        verify(fakeSurfaceListener, times(1)).onSurfaceActive(eq(fakeSurface2))
        verify(fakeSurfaceListener, never()).onSurfaceActive(eq(fakeSurface3))
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(eq(fakeSurface1))
        verify(fakeSurfaceListener, never()).onSurfaceInactive(eq(fakeSurface2))
        verify(fakeSurfaceListener, never()).onSurfaceInactive(eq(fakeSurface3))
    }

    @Test
    fun settingSurfaceToNullReleasesToken() {
        surfaceGraph[stream1.id] = fakeSurface1
        surfaceGraph[stream1.id] = null

        verify(fakeSurfaceListener, times(1)).onSurfaceActive(eq(fakeSurface1))
        verify(fakeSurfaceListener, never()).onSurfaceActive(eq(fakeSurface2))
        verify(fakeSurfaceListener, never()).onSurfaceActive(eq(fakeSurface3))
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(eq(fakeSurface1))
        verify(fakeSurfaceListener, never()).onSurfaceInactive(eq(fakeSurface2))
        verify(fakeSurfaceListener, never()).onSurfaceInactive(eq(fakeSurface3))
    }

    @Test
    fun settingSurfaceToPreviouslySetSurfaceIsANoOp() {
        surfaceGraph[stream1.id] = fakeSurface1
        surfaceGraph[stream1.id] = fakeSurface1

        verify(fakeSurfaceListener, times(1)).onSurfaceActive(eq(fakeSurface1))
        verify(fakeSurfaceListener, never()).onSurfaceActive(eq(fakeSurface2))
        verify(fakeSurfaceListener, never()).onSurfaceActive(eq(fakeSurface3))
        verify(fakeSurfaceListener, never()).onSurfaceInactive(eq(fakeSurface1))
        verify(fakeSurfaceListener, never()).onSurfaceInactive(eq(fakeSurface2))
        verify(fakeSurfaceListener, never()).onSurfaceInactive(eq(fakeSurface3))
    }

    @Test
    fun settingSurfaceToNullThenPreviousSurfaceWillReacquireSurfaceToken() {
        surfaceGraph[stream1.id] = fakeSurface1
        surfaceGraph[stream1.id] = null
        surfaceGraph[stream1.id] = fakeSurface1

        verify(fakeSurfaceListener, times(2)).onSurfaceActive(eq(fakeSurface1))
        verify(fakeSurfaceListener, never()).onSurfaceActive(eq(fakeSurface2))
        verify(fakeSurfaceListener, never()).onSurfaceActive(eq(fakeSurface3))
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(eq(fakeSurface1))
        verify(fakeSurfaceListener, never()).onSurfaceInactive(eq(fakeSurface2))
        verify(fakeSurfaceListener, never()).onSurfaceInactive(eq(fakeSurface3))
    }

    @Test
    fun surfaceGraphDoesNotAllowDuplicateSurfaces() {
        surfaceGraph[stream1.id] = fakeSurface1
        assertThrows<Exception> { surfaceGraph[stream2.id] = fakeSurface1 }
    }

    @Test
    fun disconnectSurfaceGraphReleasesSurfaceTokens() {
        surfaceGraph[stream1.id] = fakeSurface1
        surfaceGraph[stream2.id] = fakeSurface2
        verify(fakeSurfaceListener, times(1)).onSurfaceActive(fakeSurface1)
        verify(fakeSurfaceListener, times(1)).onSurfaceActive(fakeSurface2)

        surfaceGraph.unregisterAllSurfaces()
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(fakeSurface1)
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(fakeSurface2)
    }

    @Test
    fun reconnectSurfaceGraphThenSurfaceWillReacquireSurfaceTokens() {
        surfaceGraph[stream1.id] = fakeSurface1
        surfaceGraph[stream2.id] = fakeSurface2
        verify(fakeSurfaceListener, times(1)).onSurfaceActive(fakeSurface1)
        verify(fakeSurfaceListener, times(1)).onSurfaceActive(fakeSurface2)

        surfaceGraph.unregisterAllSurfaces()
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(fakeSurface1)
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(fakeSurface2)

        surfaceGraph.registerAllSurfaces()
        verify(fakeSurfaceListener, times(2)).onSurfaceActive(fakeSurface1)
        verify(fakeSurfaceListener, times(2)).onSurfaceActive(fakeSurface2)
    }

    @Test
    fun reconnectSurfaceGraphWhenSurfaceChangedThenOnlyNewSurfaceWillReacquireSurfaceTokens() {
        surfaceGraph[stream1.id] = fakeSurface1
        surfaceGraph[stream2.id] = fakeSurface2
        verify(fakeSurfaceListener, times(1)).onSurfaceActive(fakeSurface1)
        verify(fakeSurfaceListener, times(1)).onSurfaceActive(fakeSurface2)

        surfaceGraph.unregisterAllSurfaces()
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(fakeSurface1)
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(fakeSurface2)

        surfaceGraph[stream2.id] = fakeSurface3
        verify(fakeSurfaceListener, times(1)).onSurfaceInactive(fakeSurface2)
        verify(fakeSurfaceListener, never()).onSurfaceActive(fakeSurface3)

        surfaceGraph.registerAllSurfaces()
        verify(fakeSurfaceListener, times(2)).onSurfaceActive(fakeSurface1)
        verify(fakeSurfaceListener, times(1)).onSurfaceActive(fakeSurface3)
    }
}
