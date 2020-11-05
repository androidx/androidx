/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe.impl

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamConfig
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.StreamType
import androidx.camera.camera2.pipe.testing.CameraPipeRobolectricTestRunner
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(CameraPipeRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class StreamMapTest {
    private val fakeMetadata = FakeCameraMetadata(
        mapOf(INFO_SUPPORTED_HARDWARE_LEVEL to INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
    )

    private val camera1 = CameraId("TestCamera-1")
    private val camera2 = CameraId("TestCamera-2")

    private val streamConfig1 = StreamConfig(
        size = Size(100, 100),
        format = StreamFormat.YUV_420_888,
        camera = camera1,
        type = StreamType.SURFACE,
        deferrable = false
    )

    private val streamConfig2 = StreamConfig(
        size = Size(100, 100),
        format = StreamFormat.YUV_420_888,
        camera = camera1,
        type = StreamType.SURFACE,
        deferrable = false
    )

    private val streamConfig3 =
        StreamConfig(
            size = Size(200, 200),
            format = StreamFormat.YUV_420_888,
            camera = camera2,
            type = StreamType.SURFACE_TEXTURE,
            deferrable = true
        )

    private val graphConfig = CameraGraph.Config(
        camera = CameraId("0"),
        streams = listOf(
            streamConfig1,
            streamConfig2,
            streamConfig3
        ),
        template = RequestTemplate(0)
    )

    @Test
    fun testStreamMapConvertsConfigObjectsToStreamIds() {
        val streamMap = StreamMap(fakeMetadata, graphConfig)

        assertThat(streamMap.streamConfigMap[streamConfig1]).isNotNull()
        assertThat(streamMap.streamConfigMap[streamConfig2]).isNotNull()
        assertThat(streamMap.streamConfigMap[streamConfig3]).isNotNull()

        val stream1 = streamMap.streamConfigMap[streamConfig1]!!
        val stream2 = streamMap.streamConfigMap[streamConfig2]!!
        val stream3 = streamMap.streamConfigMap[streamConfig3]!!

        assertThat(stream1).isEqualTo(streamMap.streamConfigMap[streamConfig1])
        assertThat(stream2).isEqualTo(streamMap.streamConfigMap[streamConfig2])
        assertThat(stream3).isEqualTo(streamMap.streamConfigMap[streamConfig3])

        assertThat(stream1).isNotEqualTo(stream2)
        assertThat(stream1).isNotEqualTo(stream3)
        assertThat(stream2).isNotEqualTo(stream3)
    }

    @Test
    fun testStreamMapIdsAreNotEqualAcrossMultipleStreamMapInstances() {
        val streamMap1 = StreamMap(fakeMetadata, graphConfig)
        val streamMap2 = StreamMap(fakeMetadata, graphConfig)

        val stream1FromConfig1 = streamMap1.streamConfigMap[streamConfig1]
        val stream1FromConfig2 = streamMap2.streamConfigMap[streamConfig1]

        assertThat(stream1FromConfig1).isNotEqualTo(stream1FromConfig2)
    }

    @Test
    fun streamsFromSameConfigAreDifferent() {
        val stream1 = StreamMap.StreamImpl(
            StreamId(1),
            streamConfig1.size,
            streamConfig1.format,
            streamConfig1.camera,
            streamConfig1.type
        )
        val stream2 = StreamMap.StreamImpl(
            StreamId(2),
            streamConfig1.size,
            streamConfig1.format,
            streamConfig1.camera,
            streamConfig1.type
        )

        assertThat(stream1).isNotEqualTo(stream2)
    }

    @Test
    fun surfacesAreSetOnVirtualCaptureSession() {
        val streamMap = StreamMap(fakeMetadata, graphConfig)
        val stream1 = streamMap.streamConfigMap[streamConfig1]!!
        val stream2 = streamMap.streamConfigMap[streamConfig2]!!
        val stream3 = streamMap.streamConfigMap[streamConfig3]!!

        val fakeSurface1 = Surface(SurfaceTexture(1))
        val fakeSurface2 = Surface(SurfaceTexture(2))
        val fakeSurface3 = Surface(SurfaceTexture(3))

        streamMap[stream1.id] = fakeSurface1
        streamMap[stream2.id] = fakeSurface2
        streamMap[stream3.id] = fakeSurface3

        val session = FakeSurfaceListener()

        streamMap.listener = session

        assertThat(session.surfaces).isNotNull()
        assertThat(session.surfaces?.get(stream1.id)).isEqualTo(fakeSurface1)
        assertThat(session.surfaces?.get(stream2.id)).isEqualTo(fakeSurface2)
        assertThat(session.surfaces?.get(stream3.id)).isEqualTo(fakeSurface3)
    }

    @Test
    fun surfacesAreSetOnceAllSurfacesAreAvailable() {
        val streamMap = StreamMap(fakeMetadata, graphConfig)
        val stream1 = streamMap.streamConfigMap[streamConfig1]!!
        val stream2 = streamMap.streamConfigMap[streamConfig2]!!
        val stream3 = streamMap.streamConfigMap[streamConfig3]!!

        val fakeSurface1 = Surface(SurfaceTexture(1))
        val fakeSurface2 = Surface(SurfaceTexture(2))
        val fakeSurface3 = Surface(SurfaceTexture(3))

        val session = FakeSurfaceListener()
        streamMap.listener = session
        assertThat(session.surfaces).isNull()

        streamMap[stream1.id] = fakeSurface1
        assertThat(session.surfaces).isNull()

        streamMap[stream2.id] = fakeSurface2
        streamMap[stream3.id] = fakeSurface3

        assertThat(session.surfaces).isNotNull()
        assertThat(session.surfaces?.get(stream1.id)).isEqualTo(fakeSurface1)
        assertThat(session.surfaces?.get(stream2.id)).isEqualTo(fakeSurface2)
        assertThat(session.surfaces?.get(stream3.id)).isEqualTo(fakeSurface3)
    }

    @Test
    fun onlyFinalSurfacesAreSentToSession() {
        val streamMap = StreamMap(fakeMetadata, graphConfig)
        val stream1 = streamMap.streamConfigMap[streamConfig1]!!
        val stream2 = streamMap.streamConfigMap[streamConfig2]!!
        val stream3 = streamMap.streamConfigMap[streamConfig3]!!

        val fakeSurface1 = Surface(SurfaceTexture(1))
        val fakeSurface2 = Surface(SurfaceTexture(2))
        val fakeSurface3 = Surface(SurfaceTexture(3))
        val fakeSurface4 = Surface(SurfaceTexture(4))

        val session = FakeSurfaceListener()
        streamMap.listener = session
        streamMap[stream1.id] = fakeSurface1
        streamMap[stream1.id] = fakeSurface2
        assertThat(session.surfaces).isNull()

        streamMap[stream2.id] = fakeSurface3
        streamMap[stream3.id] = fakeSurface4

        assertThat(session.surfaces).isNotNull()
        assertThat(session.surfaces?.get(stream1.id)).isEqualTo(fakeSurface2)
        assertThat(session.surfaces?.get(stream2.id)).isEqualTo(fakeSurface3)
        assertThat(session.surfaces?.get(stream3.id)).isEqualTo(fakeSurface4)
    }

    @Test
    fun settingSessionToNullDoesNotSetSurfaces() {
        val streamMap = StreamMap(fakeMetadata, graphConfig)
        val stream1 = streamMap.streamConfigMap[streamConfig1]!!
        val stream2 = streamMap.streamConfigMap[streamConfig2]!!
        val stream3 = streamMap.streamConfigMap[streamConfig3]!!

        val fakeSurface1 = Surface(SurfaceTexture(1))
        val fakeSurface2 = Surface(SurfaceTexture(2))
        val fakeSurface3 = Surface(SurfaceTexture(3))

        val session = FakeSurfaceListener()
        streamMap.listener = session
        streamMap[stream1.id] = fakeSurface1
        streamMap.listener = null

        streamMap[stream2.id] = fakeSurface2
        streamMap[stream3.id] = fakeSurface3

        assertThat(session.surfaces).isNull()
    }

    @Test
    fun replacingSessionPassesSurfacesToNewSession() {
        val streamMap = StreamMap(fakeMetadata, graphConfig)
        val stream1 = streamMap.streamConfigMap[streamConfig1]!!
        val stream2 = streamMap.streamConfigMap[streamConfig2]!!
        val stream3 = streamMap.streamConfigMap[streamConfig3]!!

        val fakeSurface1 = Surface(SurfaceTexture(1))
        val fakeSurface2 = Surface(SurfaceTexture(2))
        val fakeSurface3 = Surface(SurfaceTexture(3))

        streamMap[stream1.id] = fakeSurface1
        streamMap[stream2.id] = fakeSurface2
        streamMap[stream3.id] = fakeSurface3

        val session1 = FakeSurfaceListener()
        streamMap.listener = session1

        val session2 = FakeSurfaceListener()
        streamMap.listener = session2

        assertThat(session2.surfaces).isNotNull()
        assertThat(session2.surfaces?.get(stream1.id)).isEqualTo(fakeSurface1)
        assertThat(session2.surfaces?.get(stream2.id)).isEqualTo(fakeSurface2)
        assertThat(session2.surfaces?.get(stream3.id)).isEqualTo(fakeSurface3)
    }

    class FakeSurfaceListener : SurfaceListener {
        var surfaces: Map<StreamId, Surface>? = null

        override fun setSurfaceMap(surfaces: Map<StreamId, Surface>) {
            this.surfaces = surfaces
        }
    }
}