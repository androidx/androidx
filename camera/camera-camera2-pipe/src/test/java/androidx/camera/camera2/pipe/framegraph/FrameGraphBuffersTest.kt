/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.camera2.pipe.framegraph

import android.content.Context
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.MeteringRectangle
import android.util.Size
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.Frame
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.TagBundle
import androidx.camera.camera2.pipe.internal.FrameImpl
import androidx.camera.camera2.pipe.internal.FrameState
import androidx.camera.camera2.pipe.internal.NoOpFrameGraphResourceTrimmer
import androidx.camera.camera2.pipe.tagsOf
import androidx.camera.camera2.pipe.testing.CameraGraphSimulator
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeMetadata.Companion.TEST_KEY
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.FakeSurfaces
import androidx.camera.camera2.pipe.testing.HighEndDeviceTemplate
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.camera.common.CameraFrameNumber
import androidx.camera.common.Metadata
import androidx.test.core.app.ApplicationProvider
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class FrameGraphBuffersTest {
    private val testScope = TestScope()
    private val context = ApplicationProvider.getApplicationContext() as Context
    private val fakeSurfaces = FakeSurfaces()
    private val metadata = FakeCameraMetadata.fromTemplate(HighEndDeviceTemplate)
    private val stream1Config =
        CameraStream.Config.create(Size(1280, 720), StreamFormat.YUV_420_888)
    private val stream2Config =
        CameraStream.Config.create(Size(1920, 1080), StreamFormat.YUV_420_888)
    private val graphConfig =
        CameraGraph.Config(camera = metadata.camera, streams = listOf(stream1Config, stream2Config))
    private val simulator = CameraGraphSimulator.create(testScope, context, metadata, graphConfig)
    private val frameGraphBuffers =
        FrameGraphBuffers(simulator, testScope, NoOpFrameGraphResourceTrimmer)
    private val streamIdList = simulator.streams.streamIds.toList()
    private val streamId1 = streamIdList[0]
    private val streamId2 = streamIdList[1]

    @Before
    fun setup() {
        simulator.start()
        simulator.initializeSurfaces()
        simulator.simulateCameraStarted()
        testScope.advanceUntilIdle()
    }

    @Test
    fun attachActualChange_repeatingRequestUpdated() = testScope.runTest {
        frameGraphBuffers.attach(
            setOf(streamId1, streamId2),
            mapOf(CAPTURE_REQUEST_KEY to 2, TEST_KEY to 5),
            1,
        )

        val frame = simulator.simulateNextFrame()
        val parameters: Map<CaptureRequest.Key<*>, Any?> = mapOf(CAPTURE_REQUEST_KEY to 2)
        val extras: Map<Metadata.Key<*>, Any?> = mapOf(TEST_KEY to 5)
        assertThat(frame.request.streams).isEqualTo(listOf(streamId1, streamId2))
        assertThat(frame.request.parameters).isEqualTo(parameters)
        assertThat(frame.request.extras).isEqualTo(extras)
    }

    @Test
    fun detachActualChange_repeatingRequestUpdated() = testScope.runTest {
        val frameBuffer1 =
            frameGraphBuffers.attach(
                setOf(streamId1),
                mapOf(CAPTURE_REQUEST_KEY to 2, TEST_KEY to 5),
                1,
            )
        val frameBuffer2 =
            frameGraphBuffers.attach(setOf(streamId2), mapOf(TEST_NULLABLE_KEY to 42), 1)
        var parameters: Map<CaptureRequest.Key<*>, Any?> =
            mapOf(CAPTURE_REQUEST_KEY to 2, TEST_NULLABLE_KEY to 42)
        val extras: Map<Metadata.Key<*>, Any?> = mapOf(TEST_KEY to 5)

        assertThat(simulator.simulateNextFrame().request.streams)
            .isEqualTo(listOf(streamId1, streamId2))
        assertThat(simulator.simulateNextFrame().request.parameters).isEqualTo(parameters)
        assertThat(simulator.simulateNextFrame().request.extras).isEqualTo(extras)

        frameBuffer1.close()

        parameters = mapOf(TEST_NULLABLE_KEY to 42)
        assertThat(simulator.simulateNextFrame().request.streams).isEqualTo(listOf(streamId2))
        assertThat(simulator.simulateNextFrame().request.parameters).isEqualTo(parameters)
        assertThat(simulator.simulateNextFrame().request.extras)
            .isEqualTo(emptyMap<Metadata.Key<*>, Any?>())

        frameBuffer2.close()
    }

    @Test
    fun trimAll_drainsAssociatedBuffers() = testScope.runTest {
        val buffer1 = frameGraphBuffers.attach(setOf(streamId1), emptyMap(), 10)
        val buffer2 = frameGraphBuffers.attach(setOf(streamId2), emptyMap(), 10)
        advanceUntilIdle()
        // Produce 5 frames. Each frame will be added to both buffers.
        repeat(5) {
            val frame = createTestFrame(it.toLong())
            frameGraphBuffers.onFrameStarted(frame)
        }
        advanceUntilIdle()
        assertThat(buffer1.size.value).isEqualTo(5)
        assertThat(buffer2.size.value).isEqualTo(5)

        frameGraphBuffers.trimAll(streamId1)
        advanceUntilIdle()

        assertThat(buffer1.size.value).isEqualTo(0)
        assertThat(buffer2.size.value).isEqualTo(5)

        frameGraphBuffers.trimAll(streamId2)
        advanceUntilIdle()

        assertThat(buffer1.size.value).isEqualTo(0)
        assertThat(buffer2.size.value).isEqualTo(0)
    }

    @Test
    fun trimAll_noMatchingBuffer_doesNotTrimAll() = testScope.runTest {
        val buffer1 = frameGraphBuffers.attach(setOf(streamId1), emptyMap(), 10)
        advanceUntilIdle()
        // Produce 3 frames. These will be added to buffer1.
        repeat(3) {
            val frame = createTestFrame(it.toLong())
            frameGraphBuffers.onFrameStarted(frame)
        }
        advanceUntilIdle()
        assertThat(buffer1.size.value).isEqualTo(3)

        frameGraphBuffers.trimAll(streamId2)
        advanceUntilIdle()

        // buffer1 should be unaffected.
        assertThat(buffer1.size.value).isEqualTo(3)
    }

    @Test
    fun trimAll_matchingBufferWithZeroFrames_doesNothing() = testScope.runTest {
        val buffer1 = frameGraphBuffers.attach(setOf(streamId1), emptyMap(), 10)
        advanceUntilIdle()
        assertThat(buffer1.size.value).isEqualTo(0)

        frameGraphBuffers.trimAll(streamId1)
        advanceUntilIdle()

        assertThat(buffer1.size.value).isEqualTo(0)
    }

    @Test
    fun attach_differentTagSets_mergesTagSetsIntoRepeatingRequest() = testScope.runTest {
        frameGraphBuffers.attach(setOf(streamId1), mapOf(TAG_KEY to tagsOf("a")), 1)
        frameGraphBuffers.attach(setOf(streamId1), mapOf(TAG_KEY to tagsOf("b")), 1)
        advanceUntilIdle()

        val repeatingRequest = currentRepeatingRequest()

        assertThat(repeatingRequest!!.streams).isEqualTo(listOf(streamId1))
        assertThat(repeatingRequest[TAG_KEY]).containsExactly("a", "b")
    }

    @Test
    fun detach_removesTagSetFromRepeatingRequest() = testScope.runTest {
        val buffer1 = frameGraphBuffers.attach(setOf(streamId1), mapOf(TAG_KEY to tagsOf("a")), 1)
        val buffer2 = frameGraphBuffers.attach(setOf(streamId1), mapOf(TAG_KEY to tagsOf("b")), 1)
        advanceUntilIdle()
        assertThat(currentRepeatingRequest()!![TAG_KEY]).containsExactly("a", "b")

        buffer1.close()
        advanceUntilIdle()

        assertThat(currentRepeatingRequest()!![TAG_KEY]).containsExactly("b")
        buffer2.close()
    }

    @Test
    fun attach_tagSetsWithGraphParameters_mergesTagSetsAndKeepsGraphParameters() =
        testScope.runTest {
            val cameraGraphConfigWithParameters =
                graphConfig.copy(
                    defaultParameters =
                        mapOf(TAG_KEY to tagsOf("default"), DEFAULT_CAPTURE_REQUEST_KEY to 1),
                    requiredParameters = mapOf(REQUIRED_CAPTURE_REQUEST_KEY to 2),
                )
            val simulatorWithParameters =
                CameraGraphSimulator.create(
                    testScope,
                    context,
                    metadata,
                    cameraGraphConfigWithParameters,
                )
            val buffers =
                FrameGraphBuffers(simulatorWithParameters, testScope, NoOpFrameGraphResourceTrimmer)
            simulatorWithParameters.start()
            simulatorWithParameters.initializeSurfaces()
            simulatorWithParameters.simulateCameraStarted()
            advanceUntilIdle()
            val streamId = simulatorWithParameters.streams.streamIds.first()

            val buffer1 = buffers.attach(setOf(streamId), mapOf(TAG_KEY to tagsOf("a")), 1)
            val buffer2 = buffers.attach(setOf(streamId), mapOf(TAG_KEY to tagsOf("b")), 1)
            advanceUntilIdle()
            val frame = simulatorWithParameters.simulateNextFrame()
            val requestMetadata = frame.requestSequence.requestMetadata[frame.request]!!

            // The repeating request carries the merged tag bundles of the buffers.
            assertThat(frame.request[TAG_KEY]).containsExactly("a", "b")
            // The default and required parameters of the CameraGraph are still sent with it.
            assertThat(frame.requestSequence.defaultParameters)
                .containsEntry(DEFAULT_CAPTURE_REQUEST_KEY, 1)
            assertThat(frame.requestSequence.requiredParameters)
                .containsEntry(REQUIRED_CAPTURE_REQUEST_KEY, 2)
            assertThat(requestMetadata[DEFAULT_CAPTURE_REQUEST_KEY]).isEqualTo(1)
            assertThat(requestMetadata[REQUIRED_CAPTURE_REQUEST_KEY]).isEqualTo(2)
            // The merged tag bundles in the request replace the default value for the same key.
            assertThat(requestMetadata[TAG_KEY]).containsExactly("a", "b")

            buffer1.close()
            buffer2.close()
            simulatorWithParameters.close()
        }

    @Test
    fun onFrameStarted_taggedRequest_onlyAddsFrameToMatchingBuffers() = testScope.runTest {
        val bufferA = frameGraphBuffers.attach(setOf(streamId1), mapOf(TAG_KEY to tagsOf("a")), 10)
        val bufferB = frameGraphBuffers.attach(setOf(streamId1), mapOf(TAG_KEY to tagsOf("b")), 10)
        val untaggedBuffer = frameGraphBuffers.attach(setOf(streamId1), emptyMap(), 10)
        advanceUntilIdle()

        val taggedFrame =
            createTestFrame(
                1,
                Request(
                    streams = listOf(streamId1, streamId2),
                    extras = mapOf(TAG_KEY to tagsOf("a")),
                ),
            )
        frameGraphBuffers.onFrameStarted(taggedFrame)
        val untaggedFrame = createTestFrame(2)
        frameGraphBuffers.onFrameStarted(untaggedFrame)
        advanceUntilIdle()

        // The tagged frame is added to bufferA and to the buffer without tags. The untagged frame
        // is only added to the buffer without tags.
        assertThat(bufferA.size.value).isEqualTo(1)
        assertThat(bufferB.size.value).isEqualTo(0)
        assertThat(untaggedBuffer.size.value).isEqualTo(2)
        bufferA.close()
        bufferB.close()
        untaggedBuffer.close()
        taggedFrame.close()
        untaggedFrame.close()
    }

    @Test
    fun attach_emptyTagBundle_repeatingFrameReachesEveryBuffer() = testScope.runTest {
        val bufferA = frameGraphBuffers.attach(setOf(streamId1), mapOf(TAG_KEY to tagsOf("a")), 1)
        val bufferB =
            frameGraphBuffers.attach(setOf(streamId2), mapOf(TAG_KEY to tagsOf<String>()), 1)
        advanceUntilIdle()

        // The repeating request has both streams and the tags of the attached buffers.
        val repeatingRequest = currentRepeatingRequest()!!
        assertThat(repeatingRequest.streams).containsExactly(streamId1, streamId2)
        assertThat(repeatingRequest[TAG_KEY]).containsExactly("a")

        // The repeating request carries the tag key, and an empty tag bundle has no tags that it
        // must carry, so the repeating frame reaches both buffers.
        val frame = createTestFrame(1, repeatingRequest)
        frameGraphBuffers.onFrameStarted(frame)
        advanceUntilIdle()
        assertThat(bufferA.size.value).isEqualTo(1)
        assertThat(bufferB.size.value).isEqualTo(1)
        bufferA.close()
        bufferB.close()
        frame.close()
    }

    @Test
    fun repeatingRequest_buffersWithTagSetsAndParameters_addsFrameToEveryBuffer() =
        testScope.runTest {
            val bufferA =
                frameGraphBuffers.attach(
                    setOf(streamId1),
                    mapOf(TAG_KEY to tagsOf("a"), CAPTURE_REQUEST_KEY to 1),
                    1,
                )
            val bufferB =
                frameGraphBuffers.attach(
                    setOf(streamId1),
                    mapOf(TAG_KEY to tagsOf("b"), CAPTURE_REQUEST_KEY to 1),
                    1,
                )
            advanceUntilIdle()

            // The repeating request carries the parameters of every buffer and the union of their
            // tag bundles, so its frames match every buffer.
            val repeatingRequest = currentRepeatingRequest()!!
            val frame = createTestFrame(1, repeatingRequest)
            frameGraphBuffers.onFrameStarted(frame)
            advanceUntilIdle()

            assertThat(repeatingRequest[CAPTURE_REQUEST_KEY]).isEqualTo(1)
            assertThat(repeatingRequest[TAG_KEY]).containsExactly("a", "b")
            assertThat(bufferA.size.value).isEqualTo(1)
            assertThat(bufferB.size.value).isEqualTo(1)
            bufferA.close()
            bufferB.close()
            frame.close()
        }

    @After
    fun cleanup() {
        fakeSurfaces.close()
    }

    private suspend fun currentRepeatingRequest(): Request? = simulator.useSession {
        it.repeatingRequest
    }

    private fun createTestFrame(
        frameNumberValue: Long,
        request: Request = Request(streams = listOf(streamId1, streamId2)),
    ): Frame {
        val frameNumber = CameraFrameNumber(frameNumberValue)
        val frameTimestamp = CameraTimestamp(100L + frameNumberValue)
        val frameState =
            FrameState(
                requestMetadata =
                    FakeRequestMetadata.from(
                        request = request,
                        streamToSurfaces =
                            mapOf(
                                streamId1 to fakeSurfaces.createFakeSurface(),
                                streamId2 to fakeSurfaces.createFakeSurface(),
                            ),
                    ),
                frameNumber = frameNumber,
                frameTimestamp = frameTimestamp,
                imageStreams =
                    setOf(simulator.streams[streamId1]!!, simulator.streams[streamId2]!!),
                concurrentImageStreams = emptySet(),
            )

        return FrameImpl(frameState)
    }

    @Test
    fun attach_tagKeyWithPlainSet_throwsAndDoesNotAttachBuffer() = testScope.runTest {
        val buffer = frameGraphBuffers.attach(setOf(streamId1), mapOf(TAG_KEY to tagsOf("a")), 1)
        advanceUntilIdle()

        // A tag key needs a TagBundle value. A plain Set is rejected before the buffer is attached,
        // so it cannot replace the tag bundle in the repeating request.
        assertThrows<IllegalArgumentException> {
            frameGraphBuffers.attach(setOf(streamId2), mapOf(TAG_KEY to setOf("a")), 1)
        }
        advanceUntilIdle()

        val repeatingRequest = currentRepeatingRequest()!!
        assertThat(repeatingRequest.streams).containsExactly(streamId1)
        assertThat(repeatingRequest[TAG_KEY]).isInstanceOf(TagBundle::class.java)
        assertThat(repeatingRequest[TAG_KEY]).containsExactly("a")
        buffer.close()
    }

    @Test
    fun attach_plainSetParameter_isNotTreatedAsTagBundle() = testScope.runTest {
        frameGraphBuffers.attach(setOf(streamId1), mapOf(SET_KEY to setOf("a")), 1)
        frameGraphBuffers.attach(setOf(streamId2), mapOf(SET_KEY to setOf("a")), 1)
        advanceUntilIdle()

        // A Metadata.Key whose type is a plain Set is a regular parameter: equal values are kept
        // as they are and are not merged.
        val repeatingRequest = currentRepeatingRequest()!!
        assertThat(repeatingRequest[SET_KEY]).isEqualTo(setOf("a"))
        assertThat(repeatingRequest[SET_KEY]).isNotInstanceOf(TagBundle::class.java)
    }

    @Test
    fun attachActualChange_arrayValuesWithSameContent_doesNotThrow() = testScope.runTest {
        val array1 = arrayOf(MeteringRectangle(0, 0, 100, 100, 100))
        val array2 = arrayOf(MeteringRectangle(0, 0, 100, 100, 100))

        frameGraphBuffers.attach(
            setOf(streamId1),
            mapOf(CaptureRequest.CONTROL_AE_REGIONS to array1),
            1,
        )

        // This should not throw an exception because of deep equality.
        frameGraphBuffers.attach(
            setOf(streamId2),
            mapOf(CaptureRequest.CONTROL_AE_REGIONS to array2),
            1,
        )
    }

    companion object {
        private val CAPTURE_REQUEST_KEY = CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION
        private val TEST_NULLABLE_KEY = CaptureRequest.BLACK_LEVEL_LOCK
        private val DEFAULT_CAPTURE_REQUEST_KEY = CaptureRequest.NOISE_REDUCTION_MODE
        private val REQUIRED_CAPTURE_REQUEST_KEY = CaptureRequest.EDGE_MODE
        private val TAG_KEY: Metadata.Key<TagBundle<String>> =
            Metadata.Key("test.frameGraphBuffers.tags")
        private val SET_KEY: Metadata.Key<Set<String>> = Metadata.Key("test.frameGraphBuffers.set")
    }
}
