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

import android.graphics.SurfaceTexture
import android.os.Build
import android.view.Surface
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CameraSurfaceManager
import androidx.camera.camera2.pipe.testing.FakeCameraController
import androidx.camera.camera2.pipe.testing.FakeGraphConfigs
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
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
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SurfaceGraphTest {
    private val config = FakeGraphConfigs
    private val graphId = CameraGraphId.nextId()
    private val fakeCameraController = FakeCameraController(graphId)

    private val streamMap = StreamGraphImpl(config.fakeMetadata, config.graphConfig, mock())

    private val fakeSurfaceListener: CameraSurfaceManager.SurfaceListener = mock()
    private val cameraSurfaceManager =
        CameraSurfaceManager().also { it.addListener(fakeSurfaceListener) }
    private val cameraGraphFlags = CameraGraph.Flags()
    private val surfaceGraph =
        SurfaceGraph(
            streamMap,
            fakeCameraController,
            cameraSurfaceManager,
            emptyMap(),
            cameraGraphFlags,
        )

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

    private val fakeSurface1 = Surface(SurfaceTexture(1))
    private val fakeSurface2 = Surface(SurfaceTexture(2))
    private val fakeSurface3 = Surface(SurfaceTexture(3))
    private val fakeSurface4 = Surface(SurfaceTexture(4))
    private val fakeSurface5 = Surface(SurfaceTexture(5))
    private val fakeSurface6 = Surface(SurfaceTexture(6))
    private val fakeSurface7 = Surface(SurfaceTexture(7))
    private val fakeSurface8 = Surface(SurfaceTexture(8))
    private val fakeSurface9 = Surface(SurfaceTexture(9))
    private val fakeSurface10 = Surface(SurfaceTexture(10))

    @After
    fun teardown() {
        fakeSurface1.release()
        fakeSurface2.release()
        fakeSurface3.release()
        fakeSurface4.release()
        fakeSurface5.release()
        fakeSurface6.release()
        fakeSurface7.release()
        fakeSurface8.release()
        fakeSurface9.release()
        fakeSurface10.release()
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

        assertThat(fakeCameraController.surfaceMap).isNotNull()
        assertThat(fakeCameraController.surfaceMap?.get(stream1.id)).isEqualTo(fakeSurface1)
        assertThat(fakeCameraController.surfaceMap?.get(stream2.id)).isEqualTo(fakeSurface2)
        assertThat(fakeCameraController.surfaceMap?.get(stream3.id)).isEqualTo(fakeSurface3)
        assertThat(fakeCameraController.surfaceMap?.get(stream4.id)).isEqualTo(fakeSurface4)
        assertThat(fakeCameraController.surfaceMap?.get(stream5.id)).isEqualTo(fakeSurface5)
        assertThat(fakeCameraController.surfaceMap?.get(stream6.id)).isEqualTo(fakeSurface6)
        assertThat(fakeCameraController.surfaceMap?.get(stream7.id)).isEqualTo(fakeSurface7)
        assertThat(fakeCameraController.surfaceMap?.get(stream8.id)).isEqualTo(fakeSurface8)
    }

    @Test
    fun outputSurfacesArePassedToListenerWhenAvailable() {
        assertThat(fakeCameraController.surfaceMap).isNull()

        surfaceGraph[stream1.id] = fakeSurface1
        surfaceGraph[stream2.id] = fakeSurface2
        surfaceGraph[stream3.id] = fakeSurface3
        surfaceGraph[stream4.id] = fakeSurface4
        surfaceGraph[stream5.id] = fakeSurface5
        surfaceGraph[stream6.id] = fakeSurface6
        surfaceGraph[stream7.id] = fakeSurface7
        surfaceGraph[stream8.id] = fakeSurface8
        assertThat(fakeCameraController.surfaceMap).isNull()

        surfaceGraph[stream9.id] = fakeSurface9
        surfaceGraph[stream10.id] = fakeSurface10

        assertThat(fakeCameraController.surfaceMap).isNotNull()
        assertThat(fakeCameraController.surfaceMap?.get(stream1.id)).isEqualTo(fakeSurface1)
        assertThat(fakeCameraController.surfaceMap?.get(stream2.id)).isEqualTo(fakeSurface2)
        assertThat(fakeCameraController.surfaceMap?.get(stream3.id)).isEqualTo(fakeSurface3)
        assertThat(fakeCameraController.surfaceMap?.get(stream4.id)).isEqualTo(fakeSurface4)
        assertThat(fakeCameraController.surfaceMap?.get(stream5.id)).isEqualTo(fakeSurface5)
        assertThat(fakeCameraController.surfaceMap?.get(stream6.id)).isEqualTo(fakeSurface6)
        assertThat(fakeCameraController.surfaceMap?.get(stream7.id)).isEqualTo(fakeSurface7)
        assertThat(fakeCameraController.surfaceMap?.get(stream8.id)).isEqualTo(fakeSurface8)
        assertThat(fakeCameraController.surfaceMap?.get(stream9.id)).isEqualTo(fakeSurface9)
        assertThat(fakeCameraController.surfaceMap?.get(stream10.id)).isEqualTo(fakeSurface10)
    }

    @Test
    fun outputSurfacesArePassedToListenerWhenAvailableWithGraphTrackingOff() {
        val surfaceGraph2 =
            SurfaceGraph(
                streamMap,
                fakeCameraController,
                cameraSurfaceManager,
                emptyMap(),
                cameraGraphFlags.copy(disableGraphLevelSurfaceTracking = true),
            )

        assertThat(fakeCameraController.surfaceMap).isNull()

        surfaceGraph2[stream1.id] = fakeSurface1
        surfaceGraph2[stream2.id] = fakeSurface2
        surfaceGraph2[stream3.id] = fakeSurface3
        surfaceGraph2[stream4.id] = fakeSurface4
        surfaceGraph2[stream5.id] = fakeSurface5
        surfaceGraph2[stream6.id] = fakeSurface6
        surfaceGraph2[stream7.id] = fakeSurface7
        surfaceGraph2[stream8.id] = fakeSurface8
        assertThat(fakeCameraController.surfaceMap).isNull()

        surfaceGraph2[stream9.id] = fakeSurface9
        surfaceGraph2[stream10.id] = fakeSurface10

        assertThat(fakeCameraController.surfaceMap).isNotNull()
        assertThat(fakeCameraController.surfaceMap?.get(stream1.id)).isEqualTo(fakeSurface1)
        assertThat(fakeCameraController.surfaceMap?.get(stream2.id)).isEqualTo(fakeSurface2)
        assertThat(fakeCameraController.surfaceMap?.get(stream3.id)).isEqualTo(fakeSurface3)
        assertThat(fakeCameraController.surfaceMap?.get(stream4.id)).isEqualTo(fakeSurface4)
        assertThat(fakeCameraController.surfaceMap?.get(stream5.id)).isEqualTo(fakeSurface5)
        assertThat(fakeCameraController.surfaceMap?.get(stream6.id)).isEqualTo(fakeSurface6)
        assertThat(fakeCameraController.surfaceMap?.get(stream7.id)).isEqualTo(fakeSurface7)
        assertThat(fakeCameraController.surfaceMap?.get(stream8.id)).isEqualTo(fakeSurface8)
        assertThat(fakeCameraController.surfaceMap?.get(stream9.id)).isEqualTo(fakeSurface9)
        assertThat(fakeCameraController.surfaceMap?.get(stream10.id)).isEqualTo(fakeSurface10)
    }

    @Test
    fun onlyMostRecentSurfacesArePassedToSession() {
        val fakeSurface1A = Surface(SurfaceTexture(7))
        val fakeSurface1B = Surface(SurfaceTexture(8))

        surfaceGraph[stream1.id] = fakeSurface1A
        surfaceGraph[stream1.id] = fakeSurface1B
        assertThat(fakeCameraController.surfaceMap).isNull()

        surfaceGraph[stream2.id] = fakeSurface2
        surfaceGraph[stream3.id] = fakeSurface3
        surfaceGraph[stream4.id] = fakeSurface4
        surfaceGraph[stream5.id] = fakeSurface5
        surfaceGraph[stream6.id] = fakeSurface6
        surfaceGraph[stream7.id] = fakeSurface7
        surfaceGraph[stream8.id] = fakeSurface8
        surfaceGraph[stream9.id] = fakeSurface9
        surfaceGraph[stream10.id] = fakeSurface10

        assertThat(fakeCameraController.surfaceMap).isNotNull()
        assertThat(fakeCameraController.surfaceMap?.get(stream1.id)).isEqualTo(fakeSurface1B)
        assertThat(fakeCameraController.surfaceMap?.get(stream2.id)).isEqualTo(fakeSurface2)
        assertThat(fakeCameraController.surfaceMap?.get(stream3.id)).isEqualTo(fakeSurface3)
        assertThat(fakeCameraController.surfaceMap?.get(stream4.id)).isEqualTo(fakeSurface4)
        assertThat(fakeCameraController.surfaceMap?.get(stream5.id)).isEqualTo(fakeSurface5)
        assertThat(fakeCameraController.surfaceMap?.get(stream6.id)).isEqualTo(fakeSurface6)
        assertThat(fakeCameraController.surfaceMap?.get(stream7.id)).isEqualTo(fakeSurface7)
        assertThat(fakeCameraController.surfaceMap?.get(stream8.id)).isEqualTo(fakeSurface8)
        assertThat(fakeCameraController.surfaceMap?.get(stream9.id)).isEqualTo(fakeSurface9)
        assertThat(fakeCameraController.surfaceMap?.get(stream10.id)).isEqualTo(fakeSurface10)

        fakeSurface1A.release()
        fakeSurface1B.release()
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
    fun newSurfacesDoesNotAcquireTokensWithGraphTrackingOff() {
        val surfaceGraph2 =
            SurfaceGraph(
                streamMap,
                fakeCameraController,
                cameraSurfaceManager,
                emptyMap(),
                cameraGraphFlags.copy(disableGraphLevelSurfaceTracking = true),
            )
        surfaceGraph2[stream1.id] = fakeSurface1

        verify(fakeSurfaceListener, never()).onSurfaceActive(eq(fakeSurface1))
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
    fun settingSurfaceToNullThenPreviousSurfaceWillReaquireSurfaceToken() {
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
}
