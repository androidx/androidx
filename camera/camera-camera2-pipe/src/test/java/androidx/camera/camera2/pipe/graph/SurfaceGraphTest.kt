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
import androidx.camera.camera2.pipe.testing.FakeCameraController
import androidx.camera.camera2.pipe.testing.FakeCameraGraphConfig
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SurfaceGraphTest {
    private val config = FakeCameraGraphConfig()

    private val streamMap = StreamGraphImpl(config.fakeMetadata, config.graphConfig)
    private val controller = FakeCameraController()
    private val surfaceGraph = SurfaceGraph(streamMap, controller)

    private val stream1 = streamMap[config.streamConfig1]!!
    private val stream2 = streamMap[config.streamConfig2]!!
    private val stream3 = streamMap[config.streamConfig3]!!
    private val stream4 = streamMap[config.sharedStreamConfig1]!!
    private val stream5 = streamMap[config.sharedStreamConfig2]!!

    private val fakeSurface1 = Surface(SurfaceTexture(1))
    private val fakeSurface2 = Surface(SurfaceTexture(2))
    private val fakeSurface3 = Surface(SurfaceTexture(3))
    private val fakeSurface4 = Surface(SurfaceTexture(4))
    private val fakeSurface5 = Surface(SurfaceTexture(5))

    @Test
    fun outputSurfacesArePassedToControllerImmediately() {
        surfaceGraph[stream1.id] = fakeSurface1
        surfaceGraph[stream2.id] = fakeSurface2
        surfaceGraph[stream3.id] = fakeSurface3
        surfaceGraph[stream4.id] = fakeSurface4
        surfaceGraph[stream5.id] = fakeSurface5

        assertThat(controller.surfaceMap).isNotNull()
        assertThat(controller.surfaceMap?.get(stream1.id)).isEqualTo(fakeSurface1)
        assertThat(controller.surfaceMap?.get(stream2.id)).isEqualTo(fakeSurface2)
        assertThat(controller.surfaceMap?.get(stream3.id)).isEqualTo(fakeSurface3)
    }

    @Test
    fun outputSurfacesArePassedToListenerWhenAvailable() {
        assertThat(controller.surfaceMap).isNull()

        surfaceGraph[stream1.id] = fakeSurface1
        surfaceGraph[stream2.id] = fakeSurface2
        surfaceGraph[stream3.id] = fakeSurface3
        assertThat(controller.surfaceMap).isNull()

        surfaceGraph[stream4.id] = fakeSurface4
        surfaceGraph[stream5.id] = fakeSurface5

        assertThat(controller.surfaceMap).isNotNull()
        assertThat(controller.surfaceMap?.get(stream1.id)).isEqualTo(fakeSurface1)
        assertThat(controller.surfaceMap?.get(stream2.id)).isEqualTo(fakeSurface2)
        assertThat(controller.surfaceMap?.get(stream3.id)).isEqualTo(fakeSurface3)
        assertThat(controller.surfaceMap?.get(stream4.id)).isEqualTo(fakeSurface4)
        assertThat(controller.surfaceMap?.get(stream5.id)).isEqualTo(fakeSurface5)
    }

    @Test
    fun onlyMostRecentSurfacesArePassedToSession() {
        val fakeSurface1A = Surface(SurfaceTexture(7))
        val fakeSurface1B = Surface(SurfaceTexture(8))

        surfaceGraph[stream1.id] = fakeSurface1A
        surfaceGraph[stream1.id] = fakeSurface1B
        assertThat(controller.surfaceMap).isNull()

        surfaceGraph[stream2.id] = fakeSurface2
        surfaceGraph[stream3.id] = fakeSurface3
        surfaceGraph[stream4.id] = fakeSurface4
        surfaceGraph[stream5.id] = fakeSurface5

        assertThat(controller.surfaceMap).isNotNull()
        assertThat(controller.surfaceMap?.get(stream1.id)).isEqualTo(fakeSurface1B)
        assertThat(controller.surfaceMap?.get(stream2.id)).isEqualTo(fakeSurface2)
        assertThat(controller.surfaceMap?.get(stream3.id)).isEqualTo(fakeSurface3)
        assertThat(controller.surfaceMap?.get(stream4.id)).isEqualTo(fakeSurface4)
        assertThat(controller.surfaceMap?.get(stream5.id)).isEqualTo(fakeSurface5)
    }
}