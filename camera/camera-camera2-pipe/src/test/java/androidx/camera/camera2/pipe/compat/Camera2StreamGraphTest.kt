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

package androidx.camera.camera2.pipe.compat

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class Camera2StreamGraphTest {
    private val fakeMetadata = FakeCameraMetadata(
        mapOf(INFO_SUPPORTED_HARDWARE_LEVEL to INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
    )

    private val camera1 = CameraId("TestCamera-1")
    private val camera2 = CameraId("TestCamera-2")

    private val streamConfig1 = CameraStream.Config.create(
        size = Size(100, 100),
        format = StreamFormat.YUV_420_888
    )
    private val streamConfig2 = CameraStream.Config.create(
        size = Size(123, 321),
        format = StreamFormat.YUV_420_888,
        camera = camera1
    )
    private val streamConfig3 = CameraStream.Config.create(
        size = Size(200, 200),
        format = StreamFormat.YUV_420_888,
        camera = camera2,
        outputType = OutputStream.OutputType.SURFACE_TEXTURE
    )
    private val sharedOutputConfig = OutputStream.Config.create(
        size = Size(200, 200),
        format = StreamFormat.YUV_420_888,
        camera = camera1
    )
    private val sharedStreamConfig1 = CameraStream.Config.create(sharedOutputConfig)
    private val sharedStreamConfig2 = CameraStream.Config.create(sharedOutputConfig)

    private val graphConfig = CameraGraph.Config(
        camera = camera1,
        streams = listOf(
            streamConfig1,
            streamConfig2,
            streamConfig3,
            sharedStreamConfig1,
            sharedStreamConfig2
        ),
        streamSharingGroups = listOf(listOf(streamConfig1, streamConfig2))
    )

    @Test
    fun testPrecomputedTestData() {
        val streamGraph = Camera2StreamGraph(fakeMetadata, graphConfig)

        assertThat(streamGraph.streams).hasSize(5)
        assertThat(streamGraph.streams).hasSize(5)
        assertThat(streamGraph.outputConfigs).hasSize(4)

        val stream1 = streamGraph[streamConfig1]!!
        val outputStream1 = stream1.outputs.single()
        assertThat(outputStream1.format).isEqualTo(StreamFormat.YUV_420_888)
        assertThat(outputStream1.size.width).isEqualTo(100)
        assertThat(outputStream1.size.height).isEqualTo(100)

        val stream2 = streamGraph[streamConfig2]!!
        val outputStream2 = stream2.outputs.single()
        assertThat(outputStream2.camera).isEqualTo(graphConfig.camera)
        assertThat(outputStream2.format).isEqualTo(StreamFormat.YUV_420_888)
        assertThat(outputStream2.size.width).isEqualTo(123)
        assertThat(outputStream2.size.height).isEqualTo(321)
    }

    @Test
    fun testStreamGraphPopulatesCameraId() {
        val streamGraph = Camera2StreamGraph(fakeMetadata, graphConfig)
        val stream = streamGraph[streamConfig1]!!
        assertThat(streamConfig1.outputs.single().camera).isNull()
        assertThat(stream.outputs.single().camera).isEqualTo(graphConfig.camera)
    }

    @Test
    fun testStreamWithMultipleOutputs() {

        val streamConfig = CameraStream.Config.create(
            listOf(
                OutputStream.Config.create(
                    Size(800, 600),
                    StreamFormat.YUV_420_888
                ),
                OutputStream.Config.create(
                    Size(1600, 1200),
                    StreamFormat.YUV_420_888
                ),
                OutputStream.Config.create(
                    Size(800, 600),
                    StreamFormat.YUV_420_888
                ),
            )
        )
        val config = CameraGraph.Config(
            camera = CameraId("TestCamera"),
            streams = listOf(streamConfig),
        )
        val streamGraph = Camera2StreamGraph(fakeMetadata, config)

        assertThat(streamGraph.streams).hasSize(1)
        assertThat(streamGraph.streams).hasSize(1)
        assertThat(streamGraph.outputConfigs).hasSize(3)
    }

    @Test
    fun testStreamMapConvertsConfigObjectsToStreamIds() {
        val streamGraph = Camera2StreamGraph(fakeMetadata, graphConfig)

        assertThat(streamGraph[streamConfig1]).isNotNull()
        assertThat(streamGraph[streamConfig2]).isNotNull()
        assertThat(streamGraph[streamConfig3]).isNotNull()

        val stream1 = streamGraph[streamConfig1]!!
        val stream2 = streamGraph[streamConfig2]!!
        val stream3 = streamGraph[streamConfig3]!!

        assertThat(stream1).isEqualTo(streamGraph[streamConfig1])
        assertThat(stream2).isEqualTo(streamGraph[streamConfig2])
        assertThat(stream3).isEqualTo(streamGraph[streamConfig3])

        assertThat(streamConfig1).isNotEqualTo(streamConfig2)
        assertThat(streamConfig1).isNotEqualTo(streamConfig3)
        assertThat(streamConfig2).isNotEqualTo(streamConfig3)
    }

    @Test
    fun testStreamMapIdsAreNotEqualAcrossMultipleStreamMapInstances() {
        val streamGraphA = Camera2StreamGraph(fakeMetadata, graphConfig)
        val streamGraphB = Camera2StreamGraph(fakeMetadata, graphConfig)

        val stream1A = streamGraphA[streamConfig1]!!
        val stream1B = streamGraphB[streamConfig1]!!

        assertThat(stream1A).isNotEqualTo(stream1B)
        assertThat(stream1A.id).isNotEqualTo(stream1B.id)
    }

    @Test
    fun testSharedStreamsHaveOneOutputConfig() {
        val streamGraph = Camera2StreamGraph(fakeMetadata, graphConfig)
        val stream1 = streamGraph[sharedStreamConfig1]!!
        val stream2 = streamGraph[sharedStreamConfig2]!!

        val outputConfigForStream1 =
            streamGraph.outputConfigs.filter { it.streams.contains(stream1) }
        val outputConfigForStream2 =
            streamGraph.outputConfigs.filter { it.streams.contains(stream2) }

        assertThat(outputConfigForStream1).hasSize(1)
        assertThat(outputConfigForStream2).hasSize(1)
        assertThat(outputConfigForStream1.first()).isSameInstanceAs(outputConfigForStream2.first())
    }

    @Test
    fun testSharedStreamsHaveDifferentOutputStreams() {
        val streamGraph = Camera2StreamGraph(fakeMetadata, graphConfig)
        val stream1 = streamGraph[sharedStreamConfig1]!!
        val stream2 = streamGraph[sharedStreamConfig2]!!

        assertThat(stream1.outputs.first()).isNotEqualTo(stream2.outputs.first())
    }

    @Test
    fun testGroupedStreamsHaveSameGroupNumber() {
        val streamGraph = Camera2StreamGraph(fakeMetadata, graphConfig)
        val stream1 = streamGraph[streamConfig1]!!
        val stream2 = streamGraph[streamConfig2]!!

        val outputConfigForStream1 =
            streamGraph.outputConfigs.filter { it.streams.contains(stream1) }
        val outputConfigForStream2 =
            streamGraph.outputConfigs.filter { it.streams.contains(stream2) }
        assertThat(outputConfigForStream1).hasSize(1)
        assertThat(outputConfigForStream2).hasSize(1)

        val config1 = outputConfigForStream1.first()
        val config2 = outputConfigForStream2.first()
        assertThat(config1).isNotEqualTo(config2)

        assertThat(config1.groupNumber).isGreaterThan(-1)
        assertThat(config2.groupNumber).isGreaterThan(-1)
        assertThat(config1.groupNumber).isEqualTo(config2.groupNumber)
    }

    @Test
    fun outputSurfacesArePassedToListenerImmediately() {
        val streamMap = Camera2StreamGraph(fakeMetadata, graphConfig)
        val stream1 = streamMap[streamConfig1]!!
        val stream2 = streamMap[streamConfig2]!!
        val stream3 = streamMap[streamConfig3]!!
        val stream4 = streamMap[sharedStreamConfig1]!!
        val stream5 = streamMap[sharedStreamConfig2]!!

        val fakeSurface1 = Surface(SurfaceTexture(1))
        val fakeSurface2 = Surface(SurfaceTexture(2))
        val fakeSurface3 = Surface(SurfaceTexture(3))
        val fakeSurface4 = Surface(SurfaceTexture(4))
        val fakeSurface5 = Surface(SurfaceTexture(5))

        streamMap[stream1.id] = fakeSurface1
        streamMap[stream2.id] = fakeSurface2
        streamMap[stream3.id] = fakeSurface3
        streamMap[stream4.id] = fakeSurface4
        streamMap[stream5.id] = fakeSurface5

        val session = FakeSurfaceListener()

        streamMap.listener = session

        assertThat(session.surfaces).isNotNull()
        assertThat(session.surfaces?.get(stream1.id)).isEqualTo(fakeSurface1)
        assertThat(session.surfaces?.get(stream2.id)).isEqualTo(fakeSurface2)
        assertThat(session.surfaces?.get(stream3.id)).isEqualTo(fakeSurface3)
    }

    @Test
    fun outputSurfacesArePassedToListenerWhenAvailable() {
        val streamMap = Camera2StreamGraph(fakeMetadata, graphConfig)
        val stream1 = streamMap[streamConfig1]!!
        val stream2 = streamMap[streamConfig2]!!
        val stream3 = streamMap[streamConfig3]!!
        val stream4 = streamMap[sharedStreamConfig1]!!
        val stream5 = streamMap[sharedStreamConfig2]!!

        val fakeSurface1 = Surface(SurfaceTexture(1))
        val fakeSurface2 = Surface(SurfaceTexture(2))
        val fakeSurface3 = Surface(SurfaceTexture(3))
        val fakeSurface4 = Surface(SurfaceTexture(4))
        val fakeSurface5 = Surface(SurfaceTexture(5))

        val session = FakeSurfaceListener()
        streamMap.listener = session
        assertThat(session.surfaces).isNull()

        streamMap[stream1.id] = fakeSurface1
        streamMap[stream2.id] = fakeSurface2
        streamMap[stream3.id] = fakeSurface3
        assertThat(session.surfaces).isNull()

        streamMap[stream4.id] = fakeSurface4
        streamMap[stream5.id] = fakeSurface5

        assertThat(session.surfaces).isNotNull()
        assertThat(session.surfaces?.get(stream1.id)).isEqualTo(fakeSurface1)
        assertThat(session.surfaces?.get(stream2.id)).isEqualTo(fakeSurface2)
        assertThat(session.surfaces?.get(stream3.id)).isEqualTo(fakeSurface3)
        assertThat(session.surfaces?.get(stream4.id)).isEqualTo(fakeSurface4)
        assertThat(session.surfaces?.get(stream5.id)).isEqualTo(fakeSurface5)
    }

    @Test
    fun onlyFinalSurfacesAreSentToSession() {
        val streamMap = Camera2StreamGraph(fakeMetadata, graphConfig)
        val stream1 = streamMap[streamConfig1]!!
        val stream2 = streamMap[streamConfig2]!!
        val stream3 = streamMap[streamConfig3]!!
        val stream4 = streamMap[sharedStreamConfig1]!!
        val stream5 = streamMap[sharedStreamConfig2]!!

        val fakeSurface1A = Surface(SurfaceTexture(1))
        val fakeSurface1B = Surface(SurfaceTexture(2))
        val fakeSurface2 = Surface(SurfaceTexture(3))
        val fakeSurface3 = Surface(SurfaceTexture(4))
        val fakeSurface4 = Surface(SurfaceTexture(5))
        val fakeSurface5 = Surface(SurfaceTexture(6))

        val session = FakeSurfaceListener()
        streamMap.listener = session
        streamMap[stream1.id] = fakeSurface1A
        streamMap[stream1.id] = fakeSurface1B
        assertThat(session.surfaces).isNull()

        streamMap[stream2.id] = fakeSurface2
        streamMap[stream3.id] = fakeSurface3
        streamMap[stream4.id] = fakeSurface4
        streamMap[stream5.id] = fakeSurface5

        assertThat(session.surfaces).isNotNull()
        assertThat(session.surfaces?.get(stream1.id)).isEqualTo(fakeSurface1B)
        assertThat(session.surfaces?.get(stream2.id)).isEqualTo(fakeSurface2)
        assertThat(session.surfaces?.get(stream3.id)).isEqualTo(fakeSurface3)
        assertThat(session.surfaces?.get(stream4.id)).isEqualTo(fakeSurface4)
        assertThat(session.surfaces?.get(stream5.id)).isEqualTo(fakeSurface5)
    }

    @Test
    fun settingListenerToNullDoesNotClearSurfaces() {
        val streamMap = Camera2StreamGraph(fakeMetadata, graphConfig)
        val stream1 = streamMap[streamConfig1]!!
        val stream2 = streamMap[streamConfig2]!!
        val stream3 = streamMap[streamConfig3]!!

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
        val streamMap = Camera2StreamGraph(fakeMetadata, graphConfig)
        val stream1 = streamMap[streamConfig1]!!
        val stream2 = streamMap[streamConfig2]!!
        val stream3 = streamMap[streamConfig3]!!
        val stream4 = streamMap[sharedStreamConfig1]!!
        val stream5 = streamMap[sharedStreamConfig2]!!

        val fakeSurface1 = Surface(SurfaceTexture(1))
        val fakeSurface2 = Surface(SurfaceTexture(2))
        val fakeSurface3 = Surface(SurfaceTexture(3))
        val fakeSurface4 = Surface(SurfaceTexture(4))
        val fakeSurface5 = Surface(SurfaceTexture(5))

        streamMap[stream1.id] = fakeSurface1
        streamMap[stream2.id] = fakeSurface2
        streamMap[stream3.id] = fakeSurface3
        streamMap[stream4.id] = fakeSurface4
        streamMap[stream5.id] = fakeSurface5

        val listener1 = FakeSurfaceListener()
        streamMap.listener = listener1

        val listener2 = FakeSurfaceListener()
        streamMap.listener = listener2

        assertThat(listener2.surfaces).isNotNull()
        assertThat(listener2.surfaces?.get(stream1.id)).isEqualTo(fakeSurface1)
        assertThat(listener2.surfaces?.get(stream2.id)).isEqualTo(fakeSurface2)
        assertThat(listener2.surfaces?.get(stream3.id)).isEqualTo(fakeSurface3)
        assertThat(listener2.surfaces?.get(stream4.id)).isEqualTo(fakeSurface4)
        assertThat(listener2.surfaces?.get(stream5.id)).isEqualTo(fakeSurface5)
    }

    class FakeSurfaceListener : Camera2StreamGraph.SurfaceListener {
        var surfaces: Map<StreamId, Surface>? = null

        override fun onSurfaceMapUpdated(surfaces: Map<StreamId, Surface>) {
            this.surfaces = surfaces
        }
    }
}