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

import android.os.Build
import android.util.Size
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.testing.FakeGraphConfigs
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class StreamGraphImplTest {
    private val config = FakeGraphConfigs

    @Test
    fun testPrecomputedTestData() {
        val streamGraph = StreamGraphImpl(config.fakeMetadata, config.graphConfig)

        assertThat(streamGraph.streams).hasSize(9)
        assertThat(streamGraph.streams).hasSize(9)
        assertThat(streamGraph.outputConfigs).hasSize(8)

        val stream1 = streamGraph[config.streamConfig1]!!
        val outputStream1 = stream1.outputs.single()
        assertThat(outputStream1.format).isEqualTo(StreamFormat.YUV_420_888)
        assertThat(outputStream1.size.width).isEqualTo(100)
        assertThat(outputStream1.size.height).isEqualTo(100)
        assertThat(outputStream1.mirrorMode).isNull()
        assertThat(outputStream1.timestampBase).isNull()
        assertThat(outputStream1.dynamicRangeProfile).isNull()
        assertThat(outputStream1.streamUseCase).isNull()

        val stream2 = streamGraph[config.streamConfig2]!!
        val outputStream2 = stream2.outputs.single()
        assertThat(outputStream2.camera).isEqualTo(config.graphConfig.camera)
        assertThat(outputStream2.format).isEqualTo(StreamFormat.YUV_420_888)
        assertThat(outputStream2.size.width).isEqualTo(123)
        assertThat(outputStream2.size.height).isEqualTo(321)
        assertThat(outputStream2.mirrorMode).isNull()
        assertThat(outputStream2.timestampBase).isNull()
        assertThat(outputStream2.dynamicRangeProfile).isNull()
        assertThat(outputStream2.streamUseCase).isNull()
    }

    @Test
    fun testStreamGraphPopulatesCameraId() {
        val streamGraph = StreamGraphImpl(config.fakeMetadata, config.graphConfig)
        val stream = streamGraph[config.streamConfig1]!!
        assertThat(config.streamConfig1.outputs.single().camera).isNull()
        assertThat(stream.outputs.single().camera).isEqualTo(config.graphConfig.camera)
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
        val graphConfig = CameraGraph.Config(
            camera = CameraId("TestCamera"),
            streams = listOf(streamConfig),
        )
        val streamGraph = StreamGraphImpl(config.fakeMetadata, graphConfig)

        assertThat(streamGraph.streams).hasSize(1)
        assertThat(streamGraph.streams).hasSize(1)
        assertThat(streamGraph.outputConfigs).hasSize(3)
    }

    @Test
    fun testStreamMapConvertsConfigObjectsToStreamIds() {
        val streamGraph = StreamGraphImpl(config.fakeMetadata, config.graphConfig)

        assertThat(streamGraph[config.streamConfig1]).isNotNull()
        assertThat(streamGraph[config.streamConfig2]).isNotNull()
        assertThat(streamGraph[config.streamConfig3]).isNotNull()

        val stream1 = streamGraph[config.streamConfig1]!!
        val stream2 = streamGraph[config.streamConfig2]!!
        val stream3 = streamGraph[config.streamConfig3]!!

        assertThat(stream1).isEqualTo(streamGraph[config.streamConfig1])
        assertThat(stream2).isEqualTo(streamGraph[config.streamConfig2])
        assertThat(stream3).isEqualTo(streamGraph[config.streamConfig3])

        assertThat(config.streamConfig1).isNotEqualTo(config.streamConfig2)
        assertThat(config.streamConfig1).isNotEqualTo(config.streamConfig3)
        assertThat(config.streamConfig2).isNotEqualTo(config.streamConfig3)
    }

    @Test
    fun testStreamMapIdsAreNotEqualAcrossMultipleStreamMapInstances() {
        val streamGraphA = StreamGraphImpl(config.fakeMetadata, config.graphConfig)
        val streamGraphB = StreamGraphImpl(config.fakeMetadata, config.graphConfig)

        val stream1A = streamGraphA[config.streamConfig1]!!
        val stream1B = streamGraphB[config.streamConfig1]!!

        assertThat(stream1A).isNotEqualTo(stream1B)
        assertThat(stream1A.id).isNotEqualTo(stream1B.id)
    }

    @Test
    fun testSharedStreamsHaveOneOutputConfig() {
        val streamGraph = StreamGraphImpl(config.fakeMetadata, config.graphConfig)
        val stream1 = streamGraph[config.sharedStreamConfig1]!!
        val stream2 = streamGraph[config.sharedStreamConfig2]!!

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
        val streamGraph = StreamGraphImpl(config.fakeMetadata, config.graphConfig)
        val stream1 = streamGraph[config.sharedStreamConfig1]!!
        val stream2 = streamGraph[config.sharedStreamConfig2]!!

        assertThat(stream1.outputs.first()).isNotEqualTo(stream2.outputs.first())
    }

    @Test
    fun testGroupedStreamsHaveSameGroupNumber() {
        val streamGraph = StreamGraphImpl(config.fakeMetadata, config.graphConfig)
        val stream1 = streamGraph[config.streamConfig1]!!
        val stream2 = streamGraph[config.streamConfig2]!!

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
    fun testDefaultAndPropagatedMirrorModes() {
        val streamGraph = StreamGraphImpl(config.fakeMetadata, config.graphConfig)
        val stream1 = streamGraph[config.streamConfig1]!!
        assertThat(stream1.outputs.single().mirrorMode).isNull()

        val stream2 = streamGraph[config.streamConfig4]!!
        assertThat(stream2.outputs.single().mirrorMode)
            .isEqualTo(OutputStream.MirrorMode.MIRROR_MODE_H)
    }

    @Test
    fun testDefaultAndPropagatedTimestampBases() {
        val streamGraph = StreamGraphImpl(config.fakeMetadata, config.graphConfig)
        val stream1 = streamGraph[config.streamConfig1]!!
        assertThat(stream1.outputs.single().timestampBase).isNull()

        val stream2 = streamGraph[config.streamConfig5]!!
        assertThat(stream2.outputs.single().timestampBase)
            .isEqualTo(OutputStream.TimestampBase.TIMESTAMP_BASE_MONOTONIC)
    }

    @Test
    fun testDefaultAndPropagatedDynamicRangeProfiles() {
        val streamGraph = StreamGraphImpl(config.fakeMetadata, config.graphConfig)
        val stream1 = streamGraph[config.streamConfig1]!!
        assertThat(stream1.outputs.single().dynamicRangeProfile).isNull()

        val stream2 = streamGraph[config.streamConfig6]!!
        assertThat(stream2.outputs.single().dynamicRangeProfile)
            .isEqualTo(OutputStream.DynamicRangeProfile.PUBLIC_MAX)
    }

    @Test
    fun testDefaultAndPropagatedStreamUseCases() {
        val streamGraph = StreamGraphImpl(config.fakeMetadata, config.graphConfig)
        val stream1 = streamGraph[config.streamConfig1]!!
        assertThat(stream1.outputs.single().streamUseCase).isNull()

        val stream2 = streamGraph[config.streamConfig7]!!
        assertThat(stream2.outputs.single().streamUseCase)
            .isEqualTo(OutputStream.StreamUseCase.VIDEO_RECORD)
    }
}