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
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
import android.os.Build
import android.util.Size
import androidx.camera.camera2.pipe.CameraBackendFactory
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraMetadata.Companion.isHardwareLevelExternal
import androidx.camera.camera2.pipe.CameraMetadata.Companion.isHardwareLevelLegacy
import androidx.camera.camera2.pipe.CameraMetadata.Companion.isHardwareLevelLimited
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.internal.CameraBackendsImpl
import androidx.camera.camera2.pipe.testing.CameraControllerSimulator
import androidx.camera.camera2.pipe.testing.FakeCameraBackend
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeGraphConfigs
import androidx.camera.camera2.pipe.testing.FakeGraphProcessor
import androidx.camera.camera2.pipe.testing.FakeThreads
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.TestScope
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class StreamGraphImplTest {
    private val testScope = TestScope()

    private val context = ApplicationProvider.getApplicationContext() as Context
    private val metadata =
        FakeCameraMetadata(
            mapOf(INFO_SUPPORTED_HARDWARE_LEVEL to INFO_SUPPORTED_HARDWARE_LEVEL_FULL),
        )
    private val config = FakeGraphConfigs
    private val fakeGraphProcessor = FakeGraphProcessor()

    private val stream1Config =
        CameraStream.Config.create(Size(1280, 720), StreamFormat.YUV_420_888)
    private val stream2Config =
        CameraStream.Config.create(Size(1920, 1080), StreamFormat.YUV_420_888)

    private val graphId = CameraGraphId.nextId()
    private val graphConfig =
        CameraGraph.Config(
            camera = metadata.camera,
            streams = listOf(stream1Config, stream2Config),
        )
    private val threads = FakeThreads.fromTestScope(testScope)
    private val backend = FakeCameraBackend(fakeCameras = mapOf(metadata.camera to metadata))
    private val backends =
        CameraBackendsImpl(
            defaultBackendId = backend.id,
            cameraBackends = mapOf(backend.id to CameraBackendFactory { backend }),
            context,
            threads
        )
    private val cameraContext = CameraBackendsImpl.CameraBackendContext(context, threads, backends)
    private val cameraController =
        CameraControllerSimulator(cameraContext, graphId, graphConfig, fakeGraphProcessor)
    private val cameraControllerProvider: () -> CameraControllerSimulator = { cameraController }

    @Test
    fun testPrecomputedTestData() {
        val streamGraph =
            StreamGraphImpl(config.fakeMetadata, config.graphConfig, cameraControllerProvider)
        cameraController.streamGraph = streamGraph

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
        val streamGraph =
            StreamGraphImpl(config.fakeMetadata, config.graphConfig, cameraControllerProvider)
        cameraController.streamGraph = streamGraph
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
        val streamGraph =
            StreamGraphImpl(config.fakeMetadata, graphConfig, cameraControllerProvider)
        cameraController.streamGraph = streamGraph

        assertThat(streamGraph.streams).hasSize(1)
        assertThat(streamGraph.streams).hasSize(1)
        assertThat(streamGraph.outputConfigs).hasSize(3)
    }

    @Test
    fun testOutputSortingWithStreamUseCase() {
        val streamConfigA =
            CameraStream.Config.create(
                Size(800, 600),
                StreamFormat.UNKNOWN,
                streamUseCase = OutputStream.StreamUseCase.VIDEO_RECORD
            )
        val streamConfigB =
            CameraStream.Config.create(
                Size(1600, 1200),
                StreamFormat.UNKNOWN,
                streamUseCase = OutputStream.StreamUseCase.PREVIEW
            )
        val streamConfigC =
            CameraStream.Config.create(
                Size(800, 600),
                StreamFormat.UNKNOWN,
                streamUseCase = OutputStream.StreamUseCase.DEFAULT
            )
        val graphConfig =
            CameraGraph.Config(
                camera = CameraId("0"),
                streams = listOf(streamConfigA, streamConfigB, streamConfigC),
            )

        val streamGraph =
            StreamGraphImpl(config.fakeMetadata, graphConfig, cameraControllerProvider)
        cameraController.streamGraph = streamGraph

        // Get the stream for each streamConfig
        val streamA = streamGraph[streamConfigA]
        val streamB = streamGraph[streamConfigB]
        val streamC = streamGraph[streamConfigC]

        // Read the list of outputConfigs (in order)
        val outputConfigAt0 = streamGraph.outputConfigs[0]
        val outputConfigAt1 = streamGraph.outputConfigs[1]
        val outputConfigAt2 = streamGraph.outputConfigs[2]

        // Assert that the outputConfig order is B, C, A because:
        // B is moved to the front of the list, because it is a PREVIEW stream.
        // A is moved to the end of the list, because it is a VIDEO_RECORD output stream
        assertThat(outputConfigAt0.streams).containsExactly(streamB)
        assertThat(outputConfigAt1.streams).containsExactly(streamC)
        assertThat(outputConfigAt2.streams).containsExactly(streamA)
    }

    @Test
    fun testOutputSortingWithOutputType() {
        val streamConfigA =
            CameraStream.Config.create(
                Size(800, 600),
                StreamFormat.UNKNOWN,
                outputType = OutputStream.OutputType.SURFACE
            )
        val streamConfigB =
            CameraStream.Config.create(
                Size(1600, 1200),
                StreamFormat.UNKNOWN,
                outputType = OutputStream.OutputType.SURFACE_TEXTURE
            )
        val streamConfigC =
            CameraStream.Config.create(
                Size(800, 600),
                StreamFormat.UNKNOWN,
                outputType = OutputStream.OutputType.SURFACE_VIEW
            )
        val graphConfig =
            CameraGraph.Config(
                camera = CameraId("0"),
                streams = listOf(streamConfigA, streamConfigB, streamConfigC),
            )

        val streamGraph =
            StreamGraphImpl(config.fakeMetadata, graphConfig, cameraControllerProvider)
        cameraController.streamGraph = streamGraph

        // Get the stream for each streamConfig
        val streamA = streamGraph[streamConfigA]
        val streamB = streamGraph[streamConfigB]
        val streamC = streamGraph[streamConfigC]

        // Read the list of outputConfigs (in order)
        val outputConfigAt0 = streamGraph.outputConfigs[0]
        val outputConfigAt1 = streamGraph.outputConfigs[1]
        val outputConfigAt2 = streamGraph.outputConfigs[2]

        if (deferredStreamsAreSupported(config.fakeMetadata, graphConfig)) {
            // Assert that the outputConfig order is C, B, A because:
            // B and C are moved to the front of the list because they are
            // SURFACE_VIEW/SURFACE_TEXTURE
            // C is sorted before B because SURFACE_VIEW has higher precedence than SURFACE_TEXTURE
            assertThat(outputConfigAt0.streams).containsExactly(streamC)
            assertThat(outputConfigAt1.streams).containsExactly(streamB)
            assertThat(outputConfigAt2.streams).containsExactly(streamA)
        } else {
            // If deferred streams are not supported, the difference between
            // SURFACE_VIEW/SURFACE_TEXTURE do not matter, and the order is preserved.
            assertThat(outputConfigAt0.streams).containsExactly(streamA)
            assertThat(outputConfigAt1.streams).containsExactly(streamB)
            assertThat(outputConfigAt2.streams).containsExactly(streamC)
        }
    }

    @Test
    fun testOutputSortingWithStreamFormat() {
        val streamConfigA =
            CameraStream.Config.create(Size(800, 600), StreamFormat.DEPTH_POINT_CLOUD)
        val streamConfigB = CameraStream.Config.create(Size(1600, 1200), StreamFormat.PRIVATE)
        val streamConfigC = CameraStream.Config.create(Size(800, 600), StreamFormat.UNKNOWN)
        val graphConfig =
            CameraGraph.Config(
                camera = CameraId("0"),
                streams = listOf(streamConfigA, streamConfigB, streamConfigC),
            )

        val streamGraph =
            StreamGraphImpl(config.fakeMetadata, graphConfig, cameraControllerProvider)
        cameraController.streamGraph = streamGraph

        // Get the stream for each streamConfig
        val streamA = streamGraph[streamConfigA]
        val streamB = streamGraph[streamConfigB]
        val streamC = streamGraph[streamConfigC]

        // Read the list of outputConfigs (in order)
        val outputConfigAt0 = streamGraph.outputConfigs[0]
        val outputConfigAt1 = streamGraph.outputConfigs[1]
        val outputConfigAt2 = streamGraph.outputConfigs[2]

        // Assert that the outputConfig order is C, B, A because:
        // B and C are moved to the front of the list because they are UNKNOWN/PRIVATE
        // C is sorted before B because UNKNOWN has higher precedence than PRIVATE
        assertThat(outputConfigAt0.streams).containsExactly(streamC)
        assertThat(outputConfigAt1.streams).containsExactly(streamB)
        assertThat(outputConfigAt2.streams).containsExactly(streamA)
    }

    @Test
    fun testOutputSortingWithNoConditionsMet() {
        val streamConfigA = CameraStream.Config.create(Size(800, 600), StreamFormat.YUV_420_888)
        val streamConfigB = CameraStream.Config.create(Size(1600, 1200), StreamFormat.YUV_420_888)
        val streamConfigC = CameraStream.Config.create(Size(800, 600), StreamFormat.YUV_420_888)
        val graphConfig =
            CameraGraph.Config(
                camera = CameraId("0"),
                streams = listOf(streamConfigA, streamConfigB, streamConfigC),
            )

        val streamGraph =
            StreamGraphImpl(config.fakeMetadata, graphConfig, cameraControllerProvider)
        cameraController.streamGraph = streamGraph

        // Get the stream for each streamConfig
        val streamA = streamGraph[streamConfigA]
        val streamB = streamGraph[streamConfigB]
        val streamC = streamGraph[streamConfigC]

        // Read the list of outputConfigs (in order)
        val outputConfigAt0 = streamGraph.outputConfigs[0]
        val outputConfigAt1 = streamGraph.outputConfigs[1]
        val outputConfigAt2 = streamGraph.outputConfigs[2]

        // Assert that the outputConfig order is A, B, C.
        assertThat(outputConfigAt0.streams).containsExactly(streamA)
        assertThat(outputConfigAt1.streams).containsExactly(streamB)
        assertThat(outputConfigAt2.streams).containsExactly(streamC)
    }

    @Test
    fun testOutputSortingWithSameStreamUseCase() {
        val streamConfigA =
            CameraStream.Config.create(
                Size(800, 600),
                StreamFormat.RAW12,
                streamUseCase = OutputStream.StreamUseCase.PREVIEW,
                outputType = OutputStream.OutputType.SURFACE
            )
        val streamConfigB =
            CameraStream.Config.create(
                Size(1600, 1200),
                StreamFormat.UNKNOWN,
                streamUseCase = OutputStream.StreamUseCase.PREVIEW,
                outputType = OutputStream.OutputType.SURFACE_VIEW
            )
        val streamConfigC =
            CameraStream.Config.create(
                Size(800, 600),
                StreamFormat.PRIVATE,
                streamUseCase = OutputStream.StreamUseCase.PREVIEW,
                outputType = OutputStream.OutputType.SURFACE_TEXTURE
            )
        val graphConfig =
            CameraGraph.Config(
                camera = CameraId("0"),
                streams = listOf(streamConfigA, streamConfigB, streamConfigC),
            )

        val streamGraph =
            StreamGraphImpl(config.fakeMetadata, graphConfig, cameraControllerProvider)
        cameraController.streamGraph = streamGraph

        // Get the stream for each streamConfig
        val streamA = streamGraph[streamConfigA]
        val streamB = streamGraph[streamConfigB]
        val streamC = streamGraph[streamConfigC]

        // Read the list of outputConfigs (in order)
        val outputConfigAt0 = streamGraph.outputConfigs[0]
        val outputConfigAt1 = streamGraph.outputConfigs[1]
        val outputConfigAt2 = streamGraph.outputConfigs[2]

        // Assert that the outputConfig order is A, B, C:
        // All outputs specify StreamUseCase.PREVIEW, and any additional ordering is preserved.
        assertThat(outputConfigAt0.streams).containsExactly(streamA)
        assertThat(outputConfigAt1.streams).containsExactly(streamB)
        assertThat(outputConfigAt2.streams).containsExactly(streamC)
    }

    @Test
    fun testOutputSortingWithSameOutputType() {
        val streamConfigA =
            CameraStream.Config.create(
                Size(800, 600),
                StreamFormat.UNKNOWN,
                streamUseCase = OutputStream.StreamUseCase.DEFAULT,
                outputType = OutputStream.OutputType.SURFACE_TEXTURE
            )
        val streamConfigB =
            CameraStream.Config.create(
                Size(1600, 1200),
                StreamFormat.UNKNOWN,
                streamUseCase = OutputStream.StreamUseCase.VIDEO_CALL,
                outputType = OutputStream.OutputType.SURFACE_TEXTURE
            )
        val streamConfigC =
            CameraStream.Config.create(
                Size(800, 600),
                StreamFormat.UNKNOWN,
                streamUseCase = OutputStream.StreamUseCase.STILL_CAPTURE,
                outputType = OutputStream.OutputType.SURFACE_TEXTURE
            )
        val graphConfig =
            CameraGraph.Config(
                camera = CameraId("0"),
                streams = listOf(streamConfigA, streamConfigB, streamConfigC),
            )

        val streamGraph =
            StreamGraphImpl(config.fakeMetadata, graphConfig, cameraControllerProvider)
        cameraController.streamGraph = streamGraph

        // Get the stream for each streamConfig
        val streamA = streamGraph[streamConfigA]
        val streamB = streamGraph[streamConfigB]
        val streamC = streamGraph[streamConfigC]

        // Read the list of outputConfigs (in order)
        val outputConfigAt0 = streamGraph.outputConfigs[0]
        val outputConfigAt1 = streamGraph.outputConfigs[1]
        val outputConfigAt2 = streamGraph.outputConfigs[2]

        // Assert that the outputConfig order is A, B, C because all outputType are SURFACE_TEXTURE,
        // and order should be preserved since there are no other conditions.
        assertThat(outputConfigAt0.streams).containsExactly(streamA)
        assertThat(outputConfigAt1.streams).containsExactly(streamB)
        assertThat(outputConfigAt2.streams).containsExactly(streamC)
    }

    @Test
    fun testOutputSortingWithStreamUseHint() {
        val streamConfigA =
            CameraStream.Config.create(
                Size(800, 600),
                StreamFormat.UNKNOWN,
                streamUseCase = OutputStream.StreamUseCase.DEFAULT,
                streamUseHint = OutputStream.StreamUseHint.VIDEO_RECORD
            )
        val streamConfigB =
            CameraStream.Config.create(
                Size(1600, 1200),
                StreamFormat.UNKNOWN,
                streamUseCase = OutputStream.StreamUseCase.VIDEO_CALL
            )
        val streamConfigC =
            CameraStream.Config.create(
                Size(800, 600),
                StreamFormat.UNKNOWN,
                streamUseCase = OutputStream.StreamUseCase.STILL_CAPTURE
            )
        val graphConfig =
            CameraGraph.Config(
                camera = CameraId("0"),
                streams = listOf(streamConfigA, streamConfigB, streamConfigC),
            )

        val streamGraph =
            StreamGraphImpl(config.fakeMetadata, graphConfig, cameraControllerProvider)
        cameraController.streamGraph = streamGraph

        // Get the stream for each streamConfig
        val streamA = streamGraph[streamConfigA]
        val streamB = streamGraph[streamConfigB]
        val streamC = streamGraph[streamConfigC]

        // Read the list of outputConfigs (in order)
        val outputConfigAt0 = streamGraph.outputConfigs[0]
        val outputConfigAt1 = streamGraph.outputConfigs[1]
        val outputConfigAt2 = streamGraph.outputConfigs[2]

        // Assert that the outputConfig order is B, C, A because A is moved to the bottom of the
        // list due to StreamUseHint.VIDEO_RECORD
        assertThat(outputConfigAt0.streams).containsExactly(streamB)
        assertThat(outputConfigAt1.streams).containsExactly(streamC)
        assertThat(outputConfigAt2.streams).containsExactly(streamA)
    }

    @Test
    fun testStreamMapConvertsConfigObjectsToStreamIds() {
        val streamGraph =
            StreamGraphImpl(config.fakeMetadata, config.graphConfig, cameraControllerProvider)
        cameraController.streamGraph = streamGraph

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
        val streamGraphA =
            StreamGraphImpl(config.fakeMetadata, config.graphConfig, cameraControllerProvider)
        val streamGraphB =
            StreamGraphImpl(config.fakeMetadata, config.graphConfig, cameraControllerProvider)

        val stream1A = streamGraphA[config.streamConfig1]!!
        val stream1B = streamGraphB[config.streamConfig1]!!

        assertThat(stream1A).isNotEqualTo(stream1B)
        assertThat(stream1A.id).isNotEqualTo(stream1B.id)
    }

    @Test
    fun testSharedStreamsHaveOneOutputConfig() {
        val streamGraph =
            StreamGraphImpl(config.fakeMetadata, config.graphConfig, cameraControllerProvider)
        cameraController.streamGraph = streamGraph
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
        val streamGraph =
            StreamGraphImpl(config.fakeMetadata, config.graphConfig, cameraControllerProvider)
        cameraController.streamGraph = streamGraph
        val stream1 = streamGraph[config.sharedStreamConfig1]!!
        val stream2 = streamGraph[config.sharedStreamConfig2]!!

        assertThat(stream1.outputs.first()).isNotEqualTo(stream2.outputs.first())
    }

    @Test
    fun testGroupedStreamsHaveSameGroupNumber() {
        val streamGraph =
            StreamGraphImpl(config.fakeMetadata, config.graphConfig, cameraControllerProvider)
        cameraController.streamGraph = streamGraph
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
        val streamGraph =
            StreamGraphImpl(config.fakeMetadata, config.graphConfig, cameraControllerProvider)
        cameraController.streamGraph = streamGraph
        val stream1 = streamGraph[config.streamConfig1]!!
        assertThat(stream1.outputs.single().mirrorMode).isNull()

        val stream2 = streamGraph[config.streamConfig4]!!
        assertThat(stream2.outputs.single().mirrorMode)
            .isEqualTo(OutputStream.MirrorMode.MIRROR_MODE_H)
    }

    @Test
    fun testDefaultAndPropagatedTimestampBases() {
        val streamGraph =
            StreamGraphImpl(config.fakeMetadata, config.graphConfig, cameraControllerProvider)
        cameraController.streamGraph = streamGraph
        val stream1 = streamGraph[config.streamConfig1]!!
        assertThat(stream1.outputs.single().timestampBase).isNull()

        val stream2 = streamGraph[config.streamConfig5]!!
        assertThat(stream2.outputs.single().timestampBase)
            .isEqualTo(OutputStream.TimestampBase.TIMESTAMP_BASE_MONOTONIC)
    }

    @Test
    fun testDefaultAndPropagatedDynamicRangeProfiles() {
        val streamGraph =
            StreamGraphImpl(config.fakeMetadata, config.graphConfig, cameraControllerProvider)
        cameraController.streamGraph = streamGraph
        val stream1 = streamGraph[config.streamConfig1]!!
        assertThat(stream1.outputs.single().dynamicRangeProfile).isNull()

        val stream2 = streamGraph[config.streamConfig6]!!
        assertThat(stream2.outputs.single().dynamicRangeProfile)
            .isEqualTo(OutputStream.DynamicRangeProfile.PUBLIC_MAX)
    }

    @Test
    fun testDefaultAndPropagatedStreamUseCases() {
        val streamGraph =
            StreamGraphImpl(config.fakeMetadata, config.graphConfig, cameraControllerProvider)
        cameraController.streamGraph = streamGraph
        val stream1 = streamGraph[config.streamConfig1]!!
        assertThat(stream1.outputs.single().streamUseCase).isNull()

        val stream2 = streamGraph[config.streamConfig7]!!
        assertThat(stream2.outputs.single().streamUseCase)
            .isEqualTo(OutputStream.StreamUseCase.VIDEO_RECORD)
    }

    @Test
    fun testDefaultAndPropagatedStreamUseHints() {
        val streamGraph =
            StreamGraphImpl(config.fakeMetadata, config.graphConfig, cameraControllerProvider)
        cameraController.streamGraph = streamGraph
        val stream1 = streamGraph[config.streamConfig1]!!
        assertThat(stream1.outputs.single().streamUseCase).isNull()

        val stream2 = streamGraph[config.streamConfig8]!!
        assertThat(stream2.outputs.single().streamUseHint)
            .isEqualTo(OutputStream.StreamUseHint.VIDEO_RECORD)
    }

    @Test
    fun testGetOutputLatency() {
        val streamGraph =
            StreamGraphImpl(config.fakeMetadata, config.graphConfig, cameraControllerProvider)
        cameraController.streamGraph = streamGraph
        val stream1 = streamGraph[config.streamConfig1]!!
        assertThat(streamGraph.getOutputLatency(stream1.id)).isNull()
        cameraController.simulateOutputLatency()
        assertThat(
            streamGraph
                .getOutputLatency(stream1.id)
                ?.equals(cameraController.outputLatencySet?.estimatedLatencyNs)
        )
    }

    private fun deferredStreamsAreSupported(
        cameraMetadata: CameraMetadata,
        graphConfig: CameraGraph.Config
    ): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            graphConfig.sessionMode == CameraGraph.OperatingMode.NORMAL &&
            !cameraMetadata.isHardwareLevelLegacy &&
            !cameraMetadata.isHardwareLevelLimited &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.P ||
                !cameraMetadata.isHardwareLevelExternal)
    }
}
