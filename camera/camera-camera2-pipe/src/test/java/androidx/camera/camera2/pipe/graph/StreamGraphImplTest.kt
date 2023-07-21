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

import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.util.Size
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
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

        assertThat(streamGraph.streams).hasSize(10)
        assertThat(streamGraph.streams).hasSize(10)
        assertThat(streamGraph.outputConfigs).hasSize(9)

        val stream1 = streamGraph[config.streamConfig1]!!
        val outputStream1 = stream1.outputs.single()
        assertThat(outputStream1.format).isEqualTo(StreamFormat.YUV_420_888)
        assertThat(outputStream1.size.width).isEqualTo(100)
        assertThat(outputStream1.size.height).isEqualTo(100)
        assertThat(outputStream1.mirrorMode).isNull()
        assertThat(outputStream1.timestampBase).isNull()
        assertThat(outputStream1.dynamicRangeProfile).isNull()
        assertThat(outputStream1.streamUseCase).isNull()
        assertThat(outputStream1.streamUseHint).isNull()

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
        assertThat(outputStream2.streamUseHint).isNull()
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

        val streamConfig =
            CameraStream.Config.create(
                listOf(
                    OutputStream.Config.create(Size(800, 600), StreamFormat.YUV_420_888),
                    OutputStream.Config.create(Size(1600, 1200), StreamFormat.YUV_420_888),
                    OutputStream.Config.create(Size(800, 600), StreamFormat.YUV_420_888),
                )
            )
        val graphConfig =
            CameraGraph.Config(
                camera = CameraId("0"),
                streams = listOf(streamConfig),
            )
        val streamGraph = StreamGraphImpl(config.fakeMetadata, graphConfig)

        assertThat(streamGraph.streams).hasSize(1)
        assertThat(streamGraph.streams).hasSize(1)
        assertThat(streamGraph.outputConfigs).hasSize(3)
    }

    @Test
    fun testOutputSortingWithStreamUseCase() {

        val streamConfig =
            CameraStream.Config.create(
                listOf(
                    OutputStream.Config.create(
                        Size(800, 600), StreamFormat.UNKNOWN,
                        streamUseCase = OutputStream.StreamUseCase.VIDEO_RECORD
                    ),
                    OutputStream.Config.create(
                        Size(1600, 1200), StreamFormat.UNKNOWN,
                        streamUseCase = OutputStream.StreamUseCase.PREVIEW
                    ),
                    OutputStream.Config.create(
                        Size(800, 600), StreamFormat.UNKNOWN,
                        streamUseCase = OutputStream.StreamUseCase.DEFAULT
                    ),
                )
            )
        val graphConfig =
            CameraGraph.Config(
                camera = CameraId("0"),
                streams = listOf(streamConfig),
            )
        val streamGraph = StreamGraphImpl(config.fakeMetadata, graphConfig)

        val stream1 = streamGraph.outputs[0]
        assertThat(stream1.streamUseCase).isEqualTo(OutputStream.StreamUseCase.PREVIEW)

        val stream2 = streamGraph.outputs[1]
        assertThat(stream2.streamUseCase).isEqualTo(OutputStream.StreamUseCase.DEFAULT)

        val stream3 = streamGraph.outputs[2]
        assertThat(stream3.streamUseCase).isEqualTo(OutputStream.StreamUseCase.VIDEO_RECORD)
    }

    @Test
    fun testOutputSortingWithOutputType() {

        val streamConfig =
            CameraStream.Config.create(
                listOf(
                    OutputStream.Config.create(
                        Size(800, 600), StreamFormat.UNKNOWN,
                        outputType = OutputStream.OutputType.SURFACE
                    ),
                    OutputStream.Config.create(
                        Size(1600, 1200), StreamFormat.UNKNOWN,
                        outputType = OutputStream.OutputType.SURFACE_TEXTURE
                    ),
                    OutputStream.Config.create(
                        Size(800, 600), StreamFormat.UNKNOWN,
                        outputType = OutputStream.OutputType.SURFACE_VIEW
                    ),
                )
            )
        val graphConfig =
            CameraGraph.Config(
                camera = CameraId("0"),
                streams = listOf(streamConfig),
            )
        val streamGraph = StreamGraphImpl(config.fakeMetadata, graphConfig)

        val stream1 = streamGraph.outputs[0]
        val stream2 = streamGraph.outputs[1]
        val stream3 = streamGraph.outputs[2]

        if (deferredStreamsAreSupported(config.fakeMetadata, graphConfig)) {
            assertThat(stream1.outputType).isEqualTo(OutputStream.OutputType.SURFACE_VIEW)
            assertThat(stream2.outputType).isEqualTo(OutputStream.OutputType.SURFACE_TEXTURE)
            assertThat(stream3.outputType).isNull()
        } else {
            assertThat(stream1.outputType).isNull()
            assertThat(stream2.outputType).isNull()
            assertThat(stream3.outputType).isNull()
        }
    }

    @Test
    fun testOutputSortingWithStreamFormat() {

        val streamConfig =
            CameraStream.Config.create(
                listOf(
                    OutputStream.Config.create(
                        Size(800, 600), StreamFormat.PRIVATE
                    ),
                    OutputStream.Config.create(
                        Size(1600, 1200), StreamFormat.UNKNOWN
                    ),
                    OutputStream.Config.create(
                        Size(800, 600), StreamFormat.DEPTH_POINT_CLOUD
                    ),
                )
            )
        val graphConfig =
            CameraGraph.Config(
                camera = CameraId("0"),
                streams = listOf(streamConfig),
            )
        val streamGraph = StreamGraphImpl(config.fakeMetadata, graphConfig)

        val stream1 = streamGraph.outputs[0]
        assertThat(stream1.format).isEqualTo(StreamFormat.UNKNOWN)

        val stream2 = streamGraph.outputs[1]
        assertThat(stream2.format).isEqualTo(StreamFormat.PRIVATE)

        val stream3 = streamGraph.outputs[2]
        assertThat(stream3.format).isEqualTo(StreamFormat.DEPTH_POINT_CLOUD)
    }

    @Test
    fun testOutputSortingWithNoConditionsMet() {

        val streamConfig =
            CameraStream.Config.create(
                listOf(
                    OutputStream.Config.create(
                        Size(800, 600), StreamFormat.YUV_420_888,
                        streamUseCase = OutputStream.StreamUseCase.DEFAULT,
                        outputType = OutputStream.OutputType.SURFACE
                    ),
                    OutputStream.Config.create(
                        Size(1600, 1200), StreamFormat.DEPTH16,
                        streamUseCase = OutputStream.StreamUseCase.DEFAULT,
                        outputType = OutputStream.OutputType.SURFACE
                    ),
                    OutputStream.Config.create(
                        Size(800, 600), StreamFormat.JPEG,
                        streamUseCase = OutputStream.StreamUseCase.VIDEO_CALL,
                        outputType = OutputStream.OutputType.SURFACE
                    ),
                )
            )
        val graphConfig =
            CameraGraph.Config(
                camera = CameraId("0"),
                streams = listOf(streamConfig),
            )
        val streamGraph = StreamGraphImpl(config.fakeMetadata, graphConfig)

        val stream1 = streamGraph.outputs[0]
        assertThat(stream1.format).isEqualTo(StreamFormat.YUV_420_888)

        val stream2 = streamGraph.outputs[1]
        assertThat(stream2.format).isEqualTo(StreamFormat.DEPTH16)

        val stream3 = streamGraph.outputs[2]
        assertThat(stream3.format).isEqualTo(StreamFormat.JPEG)
    }

    @Test
    fun testOutputSortingWithSameStreamUseCase() {

        val streamConfig =
            CameraStream.Config.create(
                listOf(
                    OutputStream.Config.create(
                        Size(800, 600), StreamFormat.PRIVATE,
                        streamUseCase = OutputStream.StreamUseCase.PREVIEW,
                        outputType = OutputStream.OutputType.SURFACE_TEXTURE
                    ),
                    OutputStream.Config.create(
                        Size(1600, 1200), StreamFormat.UNKNOWN,
                        streamUseCase = OutputStream.StreamUseCase.PREVIEW,
                        outputType = OutputStream.OutputType.SURFACE_VIEW
                    ),
                    OutputStream.Config.create(
                        Size(800, 600), StreamFormat.RAW12,
                        streamUseCase = OutputStream.StreamUseCase.PREVIEW,
                        outputType = OutputStream.OutputType.SURFACE
                    ),
                )
            )
        val graphConfig =
            CameraGraph.Config(
                camera = CameraId("0"),
                streams = listOf(streamConfig),
            )
        val streamGraph = StreamGraphImpl(config.fakeMetadata, graphConfig)

        val stream1 = streamGraph.outputs[0]
        assertThat(stream1.format).isEqualTo(StreamFormat.PRIVATE)

        val stream2 = streamGraph.outputs[1]
        assertThat(stream2.format).isEqualTo(StreamFormat.UNKNOWN)

        val stream3 = streamGraph.outputs[2]
        assertThat(stream3.format).isEqualTo(StreamFormat.RAW12)
    }

    @Test
    fun testOutputSortingWithSameOutputType() {

        val streamConfig =
            CameraStream.Config.create(
                listOf(
                    OutputStream.Config.create(
                        Size(800, 600), StreamFormat.UNKNOWN,
                        streamUseCase = OutputStream.StreamUseCase.DEFAULT,
                        outputType = OutputStream.OutputType.SURFACE_TEXTURE
                    ),
                    OutputStream.Config.create(
                        Size(1600, 1200), StreamFormat.UNKNOWN,
                        streamUseCase = OutputStream.StreamUseCase.VIDEO_CALL,
                        outputType = OutputStream.OutputType.SURFACE_TEXTURE
                    ),
                    OutputStream.Config.create(
                        Size(800, 600), StreamFormat.UNKNOWN,
                        streamUseCase = OutputStream.StreamUseCase.STILL_CAPTURE,
                        outputType = OutputStream.OutputType.SURFACE_TEXTURE
                    ),
                )
            )
        val graphConfig =
            CameraGraph.Config(
                camera = CameraId("0"),
                streams = listOf(streamConfig),
            )
        val streamGraph = StreamGraphImpl(config.fakeMetadata, graphConfig)

        val stream1 = streamGraph.outputs[0]
        assertThat(stream1.streamUseCase).isEqualTo(OutputStream.StreamUseCase.DEFAULT)

        val stream2 = streamGraph.outputs[1]
        assertThat(stream2.streamUseCase).isEqualTo(OutputStream.StreamUseCase.VIDEO_CALL)

        val stream3 = streamGraph.outputs[2]
        assertThat(stream3.streamUseCase).isEqualTo(OutputStream.StreamUseCase.STILL_CAPTURE)
    }

    @Test
    fun testOutputSortingWithSameImageFormat() {

        val streamConfig =
            CameraStream.Config.create(
                listOf(
                    OutputStream.Config.create(
                        Size(800, 600), StreamFormat.UNKNOWN,
                        streamUseCase = OutputStream.StreamUseCase.VIDEO_RECORD,
                        outputType = OutputStream.OutputType.SURFACE
                    ),
                    OutputStream.Config.create(
                        Size(1600, 1200), StreamFormat.UNKNOWN,
                        streamUseCase = OutputStream.StreamUseCase.VIDEO_CALL,
                        outputType = OutputStream.OutputType.SURFACE
                    ),
                    OutputStream.Config.create(
                        Size(800, 600), StreamFormat.UNKNOWN,
                        streamUseCase = OutputStream.StreamUseCase.STILL_CAPTURE,
                        outputType = OutputStream.OutputType.SURFACE
                    ),
                )
            )
        val graphConfig =
            CameraGraph.Config(
                camera = CameraId("0"),
                streams = listOf(streamConfig),
            )
        val streamGraph = StreamGraphImpl(config.fakeMetadata, graphConfig)

        val stream1 = streamGraph.outputs[0]
        assertThat(stream1.streamUseCase).isEqualTo(OutputStream.StreamUseCase.VIDEO_CALL)

        val stream2 = streamGraph.outputs[1]
        assertThat(stream2.streamUseCase).isEqualTo(OutputStream.StreamUseCase.STILL_CAPTURE)

        val stream3 = streamGraph.outputs[2]
        assertThat(stream3.streamUseCase).isEqualTo(OutputStream.StreamUseCase.VIDEO_RECORD)
    }

    @Test
    fun testOutputSortingWithStreamUseHint() {

        val streamConfig =
            CameraStream.Config.create(
                listOf(
                    OutputStream.Config.create(
                        Size(800, 600), StreamFormat.UNKNOWN,
                        streamUseCase = OutputStream.StreamUseCase.DEFAULT,
                        streamUseHint = OutputStream.StreamUseHint.VIDEO_RECORD
                    ),
                    OutputStream.Config.create(
                        Size(1600, 1200), StreamFormat.UNKNOWN,
                        streamUseCase = OutputStream.StreamUseCase.PREVIEW
                    ),
                    OutputStream.Config.create(
                        Size(800, 600), StreamFormat.UNKNOWN,
                        streamUseCase = OutputStream.StreamUseCase.STILL_CAPTURE
                    ),
                )
            )
        val graphConfig =
            CameraGraph.Config(
                camera = CameraId("0"),
                streams = listOf(streamConfig),
            )
        val streamGraph = StreamGraphImpl(config.fakeMetadata, graphConfig)

        val stream1 = streamGraph.outputs[0]
        assertThat(stream1.streamUseCase).isEqualTo(OutputStream.StreamUseCase.PREVIEW)

        val stream2 = streamGraph.outputs[1]
        assertThat(stream2.streamUseCase).isEqualTo(OutputStream.StreamUseCase.STILL_CAPTURE)

        val stream3 = streamGraph.outputs[2]
        assertThat(stream3.streamUseCase).isEqualTo(OutputStream.StreamUseCase.DEFAULT)
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

    @Test
    fun testDefaultAndPropagatedStreamUseHints() {
        val streamGraph = StreamGraphImpl(config.fakeMetadata, config.graphConfig)
        val stream1 = streamGraph[config.streamConfig1]!!
        assertThat(stream1.outputs.single().streamUseCase).isNull()

        val stream2 = streamGraph[config.streamConfig8]!!
        assertThat(stream2.outputs.single().streamUseHint)
            .isEqualTo(OutputStream.StreamUseHint.VIDEO_RECORD)
    }

    private fun deferredStreamsAreSupported(
        cameraMetadata: CameraMetadata,
        graphConfig: CameraGraph.Config
    ): Boolean {
        val hardwareLevel = cameraMetadata[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL]
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            graphConfig.sessionMode == CameraGraph.OperatingMode.NORMAL &&
            hardwareLevel != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY &&
            hardwareLevel != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.P ||
                hardwareLevel != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL)
    }
}
