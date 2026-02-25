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
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
import android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
import android.util.Size
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.Frame
import androidx.camera.camera2.pipe.FrameBuffers.tryPeekAll
import androidx.camera.camera2.pipe.FrameBuffers.tryPeekFirst
import androidx.camera.camera2.pipe.FrameBuffers.tryPeekLast
import androidx.camera.camera2.pipe.FrameBuffers.tryRemoveAll
import androidx.camera.camera2.pipe.FrameBuffers.tryRemoveFirst
import androidx.camera.camera2.pipe.FrameBuffers.tryRemoveLast
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.FrameReference
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.OutputStatus
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.graph.StreamGraphImpl
import androidx.camera.camera2.pipe.internal.FrameImpl
import androidx.camera.camera2.pipe.internal.FrameState
import androidx.camera.camera2.pipe.internal.OutputResult
import androidx.camera.camera2.pipe.media.OutputImage
import androidx.camera.camera2.pipe.testing.CameraGraphSimulator
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeImage
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.FakeSurfaces
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class FrameBufferImplTest {
    private val testScope = TestScope()
    private val context = ApplicationProvider.getApplicationContext() as Context
    private val fakeSurfaces = FakeSurfaces()
    private val metadata =
        FakeCameraMetadata(
            mapOf(INFO_SUPPORTED_HARDWARE_LEVEL to INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
        )
    private val stream1Config = CameraStream.Config.create(Size(200, 100), StreamFormat.YUV_420_888)
    private val stream2Config = CameraStream.Config.create(Size(200, 100), StreamFormat.YUV_420_888)
    private val graphConfig =
        CameraGraph.Config(camera = metadata.camera, streams = listOf(stream1Config, stream2Config))
    private val simulator = CameraGraphSimulator.create(testScope, context, metadata, graphConfig)
    private val frameGraphBuffers = FrameGraphBuffers(simulator, testScope)
    private val stream1Id: StreamId = StreamId(1)
    private val stream2Id: StreamId = StreamId(2)
    private val defaultStreams = setOf(stream1Id, stream2Id)
    private val defaultParameters = mapOf<Any, Any?>("paramKey" to "paramValue")
    private val defaultCapacity = 3
    private val frameInfoDoneFilter: (FrameReference) -> Boolean = {
        it.frameInfoStatus == OutputStatus.AVAILABLE
    }
    private lateinit var frameBuffer: FrameBufferImpl

    private fun createFrameBuffer(
        streams: Set<StreamId> = defaultStreams,
        parameters: Map<Any, Any?> = defaultParameters,
        capacity: Int = defaultCapacity,
    ): FrameBufferImpl {
        return FrameBufferImpl(frameGraphBuffers, streams, parameters, capacity)
    }

    @Before
    fun setup() {
        frameBuffer = createFrameBuffer()
    }

    private fun createTestFrame(frameNumberValue: Long): Frame {
        val frameNumber = FrameNumber(frameNumberValue)
        val frameTimestamp = CameraTimestamp(101L)
        val cameraId = CameraId("0")
        val output1 =
            StreamGraphImpl.OutputStreamImpl(
                OutputId(1),
                Size(200, 100),
                StreamFormat.YUV_420_888,
                cameraId,
            )
        val output2 =
            StreamGraphImpl.OutputStreamImpl(
                OutputId(1),
                Size(200, 100),
                StreamFormat.YUV_420_888,
                cameraId,
            )
        val stream1 = CameraStream(stream1Id, listOf(output1)).apply { output1.stream = this }
        val stream2 = CameraStream(stream2Id, listOf(output2)).apply { output2.stream = this }
        val frameState =
            FrameState(
                requestMetadata =
                    FakeRequestMetadata.from(
                        request = Request(streams = listOf(stream1Id, stream2Id)),
                        streamToSurfaces =
                            mapOf(
                                stream1Id to fakeSurfaces.createFakeSurface(Size(200, 100)),
                                stream2Id to fakeSurfaces.createFakeSurface(Size(200, 100)),
                            ),
                    ),
                frameNumber = frameNumber,
                frameTimestamp = frameTimestamp,
                imageStreams = setOf(stream1, stream2),
                concurrentImageStreams = emptySet(),
            )

        val frame = FrameImpl(frameState)

        frameState.imageOutputs
            .first { it.streamId == stream1Id }
            .onOutputComplete(
                frameNumber,
                frameTimestamp,
                42,
                frameTimestamp.value,
                OutputResult.from(
                    OutputImage.from(
                        stream1Id,
                        OutputId(10),
                        FakeImage(200, 100, StreamFormat.YUV_420_888.value, frameTimestamp.value),
                    )
                ),
            )
        frameState.imageOutputs
            .first { it.streamId == stream2Id }
            .onOutputComplete(
                frameNumber,
                frameTimestamp,
                42,
                frameTimestamp.value,
                OutputResult.from(
                    OutputImage.from(
                        stream2Id,
                        OutputId(12),
                        FakeImage(200, 100, StreamFormat.YUV_420_888.value, frameTimestamp.value),
                    )
                ),
            )
        frameState.frameInfoOutput.onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameNumber.value,
            OutputResult.from(
                FakeFrameInfo(metadata = FakeFrameMetadata(frameNumber = frameNumber))
            ),
        )

        return frame
    }

    @Test
    fun initialization_propertiesCorrectlySet() {
        assertThat(frameBuffer.streams).isEqualTo(defaultStreams)
        assertThat(frameBuffer.parameters).isEqualTo(defaultParameters)
        assertThat(frameBuffer.capacity).isEqualTo(defaultCapacity)
        assertThat(frameBuffer.size.value).isEqualTo(0)
    }

    @Test
    fun initialization_zeroCapacity_initializeSuccessfully() {
        val frameBuffer = createFrameBuffer(capacity = 0)

        assertThat(frameBuffer.capacity).isEqualTo(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun initialization_negativeCapacity_throwsException() {
        createFrameBuffer(capacity = -1)
    }

    @Test
    fun onFrameStarted_addsFrame_updatesSize() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            frameBuffer.onFrameStarted(frameRef1)
            advanceUntilIdle()

            assertThat(frameBuffer.size.value).isEqualTo(1)
            assertThat(frameBuffer.peekFirstReference()!!.frameNumber)
                .isEqualTo(frameRef1.frameNumber)
        }

    @Test
    fun onFrameStarted_whenBufferIsClosed_doesNothing() =
        testScope.runTest {
            frameBuffer.close()
            advanceUntilIdle()

            val frameRef1 = createTestFrame(1)
            frameBuffer.onFrameStarted(frameRef1)
            advanceUntilIdle()

            assertThat(frameBuffer.size.value).isEqualTo(0)
            assertThat(frameBuffer.peekFirstReference()).isNull()
        }

    @Test
    fun onFrameStarted_whenFrameIsNotAcquired_addsAValidEntry() {
        testScope.runTest {
            val frameReference = createTestFrame(1)
            frameReference.close()

            frameBuffer.onFrameStarted(frameReference)

            val peeked = frameBuffer.peekFirstReference()
            val peekedFrame = frameBuffer.tryPeekFirst()
            assertThat(peeked!!.frameNumber.value).isEqualTo(1)
            assertThat(peekedFrame).isNull()
        }
    }

    @Test
    fun onFrameStarted_zeroCapacity_doesNotBufferFrame() =
        testScope.runTest {
            val frameBuffer = createFrameBuffer(capacity = 0)
            val frameReference = createTestFrame(1)

            // Simulate onFrameStarted being called.
            frameBuffer.onFrameStarted(frameReference)
            advanceUntilIdle()

            // Assert that the buffer size remains 0.
            assertThat(frameBuffer.size.value).isEqualTo(0)
            assertThat(frameBuffer.peekFirstReference()).isNull()
            assertThat(frameBuffer.peekAllReferences()).isEmpty()
        }

    @Test
    fun removeFirstReference_emptyBuffer_returnsNull() =
        testScope.runTest {
            assertThat(frameBuffer.removeFirstReference()).isNull()
            assertThat(frameBuffer.size.value).isEqualTo(0)
        }

    @Test
    fun removeFirstReference_zeroCapacityBuffer_returnsNull() =
        testScope.runTest {
            val frameBuffer = createFrameBuffer(capacity = 0)

            assertThat(frameBuffer.removeFirstReference()).isNull()
        }

    @Test
    fun removeFirstReference_removesCorrectFrame_updatesSize() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            val frameRef2 = createTestFrame(2)
            frameBuffer.onFrameStarted(frameRef1)
            frameBuffer.onFrameStarted(frameRef2)
            advanceUntilIdle()

            val removed = frameBuffer.removeFirstReference()
            assertThat(removed!!.frameNumber).isEqualTo(frameRef1.frameNumber)
            assertThat(frameBuffer.size.value).isEqualTo(1)

            val removedNext = frameBuffer.removeFirstReference()
            assertThat(removedNext!!.frameNumber).isEqualTo(frameRef2.frameNumber)
            assertThat(frameBuffer.size.value).isEqualTo(0)
        }

    @Test
    fun removeFirstReference_whenBufferIsClosed_returnsNull() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            frameBuffer.onFrameStarted(frameRef1)
            advanceUntilIdle()
            frameBuffer.close()
            advanceUntilIdle()

            assertThat(frameBuffer.removeFirstReference()).isNull()
        }

    @Test
    fun removeLastReference_emptyBuffer_returnsNull() =
        testScope.runTest {
            assertThat(frameBuffer.removeLastReference()).isNull()
            assertThat(frameBuffer.size.value).isEqualTo(0)
        }

    @Test
    fun removeLastReference_zeroCapacityBuffer_returnsNull() =
        testScope.runTest {
            val frameBuffer = createFrameBuffer(capacity = 0)

            assertThat(frameBuffer.removeLastReference()).isNull()
        }

    @Test
    fun removeLastReference_removesCorrectFrame_updatesSize() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            val frameRef2 = createTestFrame(2)
            frameBuffer.onFrameStarted(frameRef1)
            frameBuffer.onFrameStarted(frameRef2)
            advanceUntilIdle()

            val removed = frameBuffer.removeLastReference()
            assertThat(removed!!.frameNumber).isEqualTo(frameRef2.frameNumber)
            assertThat(frameBuffer.size.value).isEqualTo(1)

            val removedNext = frameBuffer.removeLastReference()
            assertThat(removedNext!!.frameNumber).isEqualTo(frameRef1.frameNumber)
            assertThat(frameBuffer.size.value).isEqualTo(0)
        }

    @Test
    fun removeLastReference_whenClosed_returnsNull() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            frameBuffer.onFrameStarted(frameRef1)
            advanceUntilIdle()
            frameBuffer.close()
            advanceUntilIdle()

            assertThat(frameBuffer.removeLastReference()).isNull()
        }

    @Test
    fun removeAllReferences_emptyBuffer_returnsEmptyList() =
        testScope.runTest {
            assertThat(frameBuffer.removeAllReferences()).isEmpty()
            assertThat(frameBuffer.size.value).isEqualTo(0)
        }

    @Test
    fun removeAllReference_zeroCapacityBuffer_doesNotThowErrors() =
        testScope.runTest {
            val frameBuffer = createFrameBuffer(capacity = 0)

            frameBuffer.removeAllReferences()
        }

    @Test
    fun removeAllReferences_returnsAllFramesInOrder_updatesSize() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            val frameRef2 = createTestFrame(2)
            frameBuffer.onFrameStarted(frameRef1)
            frameBuffer.onFrameStarted(frameRef2)
            advanceUntilIdle()

            val removed = frameBuffer.removeAllReferences()
            assertThat(removed.map { it.frameNumber })
                .containsExactly(frameRef1.frameNumber, frameRef2.frameNumber)
                .inOrder()
            assertThat(frameBuffer.size.value).isEqualTo(0)
        }

    @Test
    fun removeAllReferences_whenClosed_returnsEmptyList() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            frameBuffer.onFrameStarted(frameRef1)
            advanceUntilIdle()
            frameBuffer.close()
            advanceUntilIdle()

            assertThat(frameBuffer.removeAllReferences()).isEmpty()
        }

    @Test
    fun removeFirstFrameReferenceAndAcquire_emptyBuffer_returnsNull() =
        testScope.runTest {
            assertThat(frameBuffer.removeFirstFrameReferenceAndAcquire(frameInfoDoneFilter))
                .isNull()
            assertThat(frameBuffer.size.value).isEqualTo(0)
        }

    @Test
    fun removeFirstFrameReferenceAndAcquire_zeroCapacityBuffer_returnsNull() =
        testScope.runTest {
            val frameBuffer = createFrameBuffer(capacity = 0)

            assertThat(frameBuffer.removeFirstFrameReferenceAndAcquire(frameInfoDoneFilter))
                .isNull()
        }

    @Test
    fun removeFirstFrameReferenceAndAcquire_removesFirstMatchingFrame_updatesSize() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            val frameRef2 = createTestFrame(2)
            val frameIdFilter: (FrameReference) -> Boolean = {
                it.frameId.value == frameRef2.frameId.value
            }
            frameBuffer.onFrameStarted(frameRef1)
            frameBuffer.onFrameStarted(frameRef2)
            advanceUntilIdle()

            val removed = frameBuffer.removeFirstFrameReferenceAndAcquire(frameIdFilter)

            assertThat(removed!!.frameNumber).isEqualTo(frameRef2.frameNumber)
            assertThat(frameBuffer.size.value).isEqualTo(1)

            removed.close()
        }

    @Test
    fun removeFirstFrameReferenceAndAcquire_noMatches_returnsNull() =
        testScope.runTest {
            val undefinedFrameIdFilter: (FrameReference) -> Boolean = { it.frameId.value == -1L }
            val frameRef1 = createTestFrame(1)
            frameBuffer.onFrameStarted(frameRef1)

            assertThat(frameBuffer.removeFirstFrameReferenceAndAcquire(undefinedFrameIdFilter))
                .isNull()
            assertThat(frameBuffer.size.value).isEqualTo(1)
        }

    @Test
    fun removeFirstFrameReferenceAndAcquire_whenBufferIsClosed_returnsNull() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            frameBuffer.onFrameStarted(frameRef1)
            advanceUntilIdle()
            frameBuffer.close()
            advanceUntilIdle()

            assertThat(frameBuffer.removeFirstFrameReferenceAndAcquire(frameInfoDoneFilter))
                .isNull()
        }

    @Test
    fun removeLastFrameReferenceAndAcquire_emptyBuffer_returnsNull() =
        testScope.runTest {
            assertThat(frameBuffer.removeLastFrameReferenceAndAcquire(frameInfoDoneFilter)).isNull()
            assertThat(frameBuffer.size.value).isEqualTo(0)
        }

    @Test
    fun removeLastFrameReferenceAndAcquire_zeroCapacityBuffer_returnsNull() =
        testScope.runTest {
            val frameBuffer = createFrameBuffer(capacity = 0)

            assertThat(frameBuffer.removeLastFrameReferenceAndAcquire(frameInfoDoneFilter)).isNull()
        }

    @Test
    fun removeLastFrameReferenceAndAcquire_removesLastMatchingFrame_updatesSize() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            val frameRef2 = createTestFrame(2)
            val frameIdFilter: (FrameReference) -> Boolean = {
                it.frameId.value == frameRef1.frameId.value
            }
            frameBuffer.onFrameStarted(frameRef1)
            frameBuffer.onFrameStarted(frameRef2)
            advanceUntilIdle()

            val removed = frameBuffer.removeLastFrameReferenceAndAcquire(frameIdFilter)

            assertThat(removed!!.frameNumber).isEqualTo(frameRef1.frameNumber)
            assertThat(frameBuffer.size.value).isEqualTo(1)

            removed.close()
        }

    @Test
    fun removeLastFrameReferenceAndAcquire_whenBufferIsClosed_returnsNull() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            frameBuffer.onFrameStarted(frameRef1)
            advanceUntilIdle()
            frameBuffer.close()
            advanceUntilIdle()

            assertThat(frameBuffer.removeLastFrameReferenceAndAcquire(frameInfoDoneFilter)).isNull()
        }

    @Test
    fun removeLastFrameReferenceAndAcquire_noMatches_returnsNull() =
        testScope.runTest {
            val undefinedFrameIdFilter: (FrameReference) -> Boolean = { it.frameId.value == -1L }
            val frameRef1 = createTestFrame(1)
            frameBuffer.onFrameStarted(frameRef1)

            assertThat(frameBuffer.removeLastFrameReferenceAndAcquire(undefinedFrameIdFilter))
                .isNull()
            assertThat(frameBuffer.size.value).isEqualTo(1)
        }

    @Test
    fun removeAllFrameReferencesAndAcquire_emptyBuffer_returnsEmptyList() =
        testScope.runTest {
            assertThat(frameBuffer.removeAllFrameReferencesAndAcquire(frameInfoDoneFilter))
                .isEmpty()
            assertThat(frameBuffer.size.value).isEqualTo(0)
        }

    @Test
    fun removeAllFrameReferencesAndAcquire_zeroCapacityBuffer_returnsEmptyList() =
        testScope.runTest {
            val frameBuffer = createFrameBuffer(capacity = 0)

            assertThat(frameBuffer.removeAllFrameReferencesAndAcquire(frameInfoDoneFilter))
                .isEmpty()
        }

    @Test
    fun removeAllFrameReferencesAndAcquire_removesAllMatchingFrames_updatesSize() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            val frameRef2 = createTestFrame(2)
            frameBuffer.onFrameStarted(frameRef1)
            frameBuffer.onFrameStarted(frameRef2)
            advanceUntilIdle()

            val removed = frameBuffer.removeAllFrameReferencesAndAcquire(frameInfoDoneFilter)

            assertThat(frameBuffer.size.value).isEqualTo(0)
            assertThat(removed.size).isEqualTo(2)

            removed.forEach { it.close() }
        }

    @Test
    fun removeAllFrameReferencesAndAcquire_whenBufferIsClosed_returnsEmptyList() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            frameBuffer.onFrameStarted(frameRef1)
            advanceUntilIdle()
            frameBuffer.close()
            advanceUntilIdle()

            assertThat(frameBuffer.removeAllFrameReferencesAndAcquire(frameInfoDoneFilter))
                .isEmpty()
        }

    @Test
    fun removeAllFrameReferencesAndAcquire_noMatches_returnsEmptyList() =
        testScope.runTest {
            val undefinedFrameIdFilter: (FrameReference) -> Boolean = { it.frameId.value == -1L }
            val frameRef1 = createTestFrame(1)
            frameBuffer.onFrameStarted(frameRef1)

            assertThat(frameBuffer.removeAllFrameReferencesAndAcquire(undefinedFrameIdFilter))
                .isEmpty()
            assertThat(frameBuffer.size.value).isEqualTo(1)
        }

    @Test
    fun peekFirstReference_emptyBuffer_returnsNull() =
        testScope.runTest { assertThat(frameBuffer.peekFirstReference()).isNull() }

    @Test
    fun peekFirstReference_zeroCapacityBuffer_returnsNull() =
        testScope.runTest {
            val frameBuffer = createFrameBuffer(capacity = 0)

            assertThat(frameBuffer.peekFirstReference()).isNull()
        }

    @Test
    fun peekFirstReference_returnsFrame_doesNotChangeSize() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            frameBuffer.onFrameStarted(frameRef1)
            advanceUntilIdle()

            val peeked = frameBuffer.peekFirstReference()
            assertThat(peeked!!.frameNumber).isEqualTo(frameRef1.frameNumber)
            assertThat(frameBuffer.size.value).isEqualTo(1)
        }

    @Test
    fun peekFirstReference_whenClosed_returnsNull() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            frameBuffer.onFrameStarted(frameRef1)
            advanceUntilIdle()
            frameBuffer.close()
            advanceUntilIdle()

            assertThat(frameBuffer.peekFirstReference()).isNull()
        }

    @Test
    fun peekLastReference_emptyBuffer_returnsNull() =
        testScope.runTest { assertThat(frameBuffer.peekLastReference()).isNull() }

    @Test
    fun peekLastReference_zeroCapacityBuffer_returnsNull() =
        testScope.runTest {
            val frameBuffer = createFrameBuffer(capacity = 0)

            assertThat(frameBuffer.peekLastReference()).isNull()
        }

    @Test
    fun peekLastReference_returnsFrame_doesNotChangeSize() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            val frameRef2 = createTestFrame(2)
            frameBuffer.onFrameStarted(frameRef1)
            frameBuffer.onFrameStarted(frameRef2)
            advanceUntilIdle()

            val peeked = frameBuffer.peekLastReference()
            assertThat(peeked!!.frameNumber).isEqualTo(frameRef2.frameNumber)
            assertThat(frameBuffer.size.value).isEqualTo(2)
        }

    @Test
    fun peekLastReference_whenClosed_returnsNull() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            frameBuffer.onFrameStarted(frameRef1)
            advanceUntilIdle()
            frameBuffer.close()
            advanceUntilIdle()

            assertThat(frameBuffer.peekLastReference()).isNull()
        }

    @Test
    fun peekAllReferences_emptyBuffer_returnsEmptyList() =
        testScope.runTest { assertThat(frameBuffer.peekAllReferences()).isEmpty() }

    @Test
    fun peekAllReference_zeroCapacityBuffer_returnsEmptyList() =
        testScope.runTest {
            val frameBuffer = createFrameBuffer(capacity = 0)

            assertThat(frameBuffer.peekAllReferences()).isEmpty()
        }

    @Test
    fun peekAllReferences_returnsAllFramesInOrder_doesNotChangeSize() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            val frameRef2 = createTestFrame(2)
            frameBuffer.onFrameStarted(frameRef1)
            frameBuffer.onFrameStarted(frameRef2)
            advanceUntilIdle()

            val peeked = frameBuffer.peekAllReferences()
            assertThat(peeked.map { it.frameNumber })
                .containsExactly(frameRef1.frameNumber, frameRef2.frameNumber)
                .inOrder()
            assertThat(frameBuffer.size.value).isEqualTo(2)
        }

    @Test
    fun peekAllReferences_whenClosed_returnsEmptyList() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            frameBuffer.onFrameStarted(frameRef1)
            advanceUntilIdle()
            frameBuffer.close()
            advanceUntilIdle()

            assertThat(frameBuffer.peekAllReferences()).isEmpty()
        }

    @Test
    fun onFrameAvailable_zeroCapacity_flowEmitted() =
        testScope.runTest {
            val frameBuffer = createFrameBuffer(capacity = 0)
            val frameRef1 = createTestFrame(1)
            val ready = CompletableDeferred<Unit>()
            val resultsChannel = Channel<FrameReference>(Channel.UNLIMITED)

            val job =
                backgroundScope.launch {
                    frameBuffer.frameFlow
                        .onStart { ready.complete(Unit) }
                        .collect { frame -> resultsChannel.send(frame) }
                }

            ready.await()
            frameBuffer.onFrameStarted(frameRef1)
            advanceUntilIdle()

            val receivedFrame = resultsChannel.receive()
            assertThat(receivedFrame.frameNumber).isEqualTo(frameRef1.frameNumber)
            assertThat(frameBuffer.size.value).isEqualTo(0)
            assertThat(frameBuffer.peekFirstReference()).isNull()
            job.cancel()
        }

    @Test
    fun onFrameAvailable_flowEmitted() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            val ready = CompletableDeferred<Unit>()
            val resultsChannel = Channel<FrameReference>(Channel.UNLIMITED)

            val job =
                backgroundScope.launch {
                    frameBuffer.frameFlow
                        .onStart { ready.complete(Unit) }
                        .collect { frame -> resultsChannel.send(frame) }
                }

            ready.await()
            frameBuffer.onFrameStarted(frameRef1)

            val receivedFrame = resultsChannel.receive()
            assertThat(receivedFrame.frameNumber).isEqualTo(frameRef1.frameNumber)
            job.cancel()
        }

    @Test
    fun onFrameAvailableCalls_multipleCalls_multipleEmitted() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            val frameRef2 = createTestFrame(2)
            val ready = CompletableDeferred<Unit>()
            val resultsChannel = Channel<FrameReference>(Channel.UNLIMITED)
            val job =
                backgroundScope.launch {
                    frameBuffer.frameFlow
                        .onStart { ready.complete(Unit) }
                        .collect { frame -> resultsChannel.send(frame) }
                }

            ready.await()
            frameBuffer.onFrameStarted(frameRef1)
            frameBuffer.onFrameStarted(frameRef2)

            assertThat(resultsChannel.receive().frameNumber).isEqualTo(frameRef1.frameNumber)
            assertThat(resultsChannel.receive().frameNumber).isEqualTo(frameRef2.frameNumber)
            job.cancel()
        }

    @Test
    fun onFrameAvailable_exceedsExtraCapacity_oldestDropped() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            val frameRef2 = createTestFrame(2)
            val frameRef3 = createTestFrame(3)
            val frameRef4 = createTestFrame(4)
            val frameRef5 = createTestFrame(5)
            val ready = CompletableDeferred<Unit>()
            val resultsChannel = Channel<FrameReference>(Channel.UNLIMITED)
            val job =
                backgroundScope.launch {
                    frameBuffer.frameFlow
                        .onStart { ready.complete(Unit) }
                        .collect { frame -> resultsChannel.send(frame) }
                }

            ready.await()
            frameBuffer.onFrameStarted(frameRef1)
            frameBuffer.onFrameStarted(frameRef2)
            frameBuffer.onFrameStarted(frameRef3)
            frameBuffer.onFrameStarted(frameRef4)
            frameBuffer.onFrameStarted(frameRef5)

            // frameRef1 will drop because the extraBufferCapacity of the flow is 4
            assertThat(resultsChannel.receive().frameNumber).isEqualTo(frameRef2.frameNumber)
            assertThat(resultsChannel.receive().frameNumber).isEqualTo(frameRef3.frameNumber)
            assertThat(resultsChannel.receive().frameNumber).isEqualTo(frameRef4.frameNumber)
            assertThat(resultsChannel.receive().frameNumber).isEqualTo(frameRef5.frameNumber)
            job.cancel()
        }

    @Test
    fun onFrameAvailable_multipleConsumers_allReceiveFrames() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            val frameRef2 = createTestFrame(2)
            val ready1 = CompletableDeferred<Unit>()
            val ready2 = CompletableDeferred<Unit>()
            val resultsChannel1 = Channel<FrameReference>(Channel.UNLIMITED)
            val resultsChannel2 = Channel<FrameReference>(Channel.UNLIMITED)
            val job1 =
                backgroundScope.launch {
                    frameBuffer.frameFlow
                        .onStart { ready1.complete(Unit) }
                        .collect { frame -> resultsChannel1.send(frame) }
                }
            val job2 =
                backgroundScope.launch {
                    frameBuffer.frameFlow
                        .onStart { ready2.complete(Unit) }
                        .collect { frame -> resultsChannel2.send(frame) }
                }

            ready1.await()
            ready2.await()
            frameBuffer.onFrameStarted(frameRef1)
            frameBuffer.onFrameStarted(frameRef2)

            assertThat(resultsChannel1.receive().frameNumber).isEqualTo(frameRef1.frameNumber)
            assertThat(resultsChannel1.receive().frameNumber).isEqualTo(frameRef2.frameNumber)
            assertThat(resultsChannel2.receive().frameNumber).isEqualTo(frameRef1.frameNumber)
            assertThat(resultsChannel2.receive().frameNumber).isEqualTo(frameRef2.frameNumber)
            job1.cancel()
            job2.cancel()
        }

    @Test
    fun onFrameAvailable_slowAndFastConsumers_fastConsumerDoesNotDropFrames() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            val frameRef2 = createTestFrame(2)
            val frameRef3 = createTestFrame(3)
            val ready1 = CompletableDeferred<Unit>()
            val ready2 = CompletableDeferred<Unit>()
            val resultsChannel1 = Channel<FrameReference>(capacity = 1)
            val resultsChannel2 = Channel<FrameReference>(Channel.UNLIMITED)
            val job1 =
                backgroundScope.launch {
                    frameBuffer.frameFlow
                        .onStart { ready1.complete(Unit) }
                        .collect { frame -> resultsChannel1.send(frame) }
                }
            val job2 =
                backgroundScope.launch {
                    frameBuffer.frameFlow
                        .onStart { ready2.complete(Unit) }
                        .collect { frame -> resultsChannel2.send(frame) }
                }

            ready1.await()
            ready2.await()
            frameBuffer.onFrameStarted(frameRef1)
            frameBuffer.onFrameStarted(frameRef2)
            frameBuffer.onFrameStarted(frameRef3)

            // Channel 1 is full, so the next frame will be dropped for this consumer.
            assertThat(resultsChannel1.receive().frameNumber).isEqualTo(frameRef1.frameNumber)
            assertThat(resultsChannel2.receive().frameNumber).isEqualTo(frameRef1.frameNumber)
            assertThat(resultsChannel2.receive().frameNumber).isEqualTo(frameRef2.frameNumber)
            assertThat(resultsChannel2.receive().frameNumber).isEqualTo(frameRef3.frameNumber)
            job1.cancel()
            job2.cancel()
        }

    @Test
    fun close_clearsQueue_updatesSize() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            frameBuffer.onFrameStarted(frameRef1)
            advanceUntilIdle()
            frameBuffer.close()
            advanceUntilIdle()

            assertThat(frameBuffer.size.value).isEqualTo(0)
            assertThat(frameBuffer.peekFirstReference()).isNull()
        }

    @Test
    fun peekFirst_peeksFirstReferenceAndAcquires() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)

            frameBuffer.onFrameStarted(frameRef1)
            frameRef1.close()
            advanceUntilIdle()

            val frame = frameBuffer.tryPeekFirst()
            frameBuffer.close()

            assertThat(frame!!.isClosed()).isFalse()
            assertThat(frame.frameNumber).isEqualTo(frameRef1.frameNumber)
        }

    @Test
    fun peekFirst_emptyBuffer_returnsNull() =
        testScope.runTest { assertThat(frameBuffer.tryPeekFirst()).isNull() }

    @Test
    fun peekLast_peeksLastReferenceAndAcquires() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)

            frameBuffer.onFrameStarted(frameRef1)
            frameRef1.close()
            advanceUntilIdle()

            val frame = frameBuffer.tryPeekLast()
            frameBuffer.close()

            assertThat(frame!!.isClosed()).isFalse()
            assertThat(frame.frameNumber).isEqualTo(frameRef1.frameNumber)
        }

    @Test
    fun peekAll_peeksAllReferencesAndAcquires() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            val frameRef2 = createTestFrame(2)

            frameBuffer.onFrameStarted(frameRef1)
            frameRef1.close()
            frameBuffer.onFrameStarted(frameRef2)
            frameRef2.close()
            advanceUntilIdle()

            val frames = frameBuffer.tryPeekAll()
            frameBuffer.close()

            assertThat(frames.map { it.frameNumber })
                .containsExactly(frameRef1.frameNumber, frameRef2.frameNumber)
                .inOrder()
            assertThat(frames[0].isClosed()).isFalse()
            assertThat(frames[1].isClosed()).isFalse()
        }

    @Test
    fun removeFirst_removesFirstReferenceAndAcquires() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)

            frameBuffer.onFrameStarted(frameRef1)
            frameRef1.close()
            advanceUntilIdle()

            val frame = frameBuffer.tryRemoveFirst()
            assertThat(frameBuffer.size.value).isEqualTo(0)

            assertThat(frame!!.isClosed()).isFalse()
            assertThat(frame.frameNumber).isEqualTo(frameRef1.frameNumber)
        }

    @Test
    fun removeLast_removesLastReferenceAndAcquires() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)

            frameBuffer.onFrameStarted(frameRef1)
            frameRef1.close()
            advanceUntilIdle()

            val frame = frameBuffer.tryRemoveLast()
            frameBuffer.close()

            assertThat(frame!!.isClosed()).isFalse()
            assertThat(frame.frameNumber).isEqualTo(frameRef1.frameNumber)
        }

    @Test
    fun removeAll_removesAllReferencesAndAcquires() =
        testScope.runTest {
            val frameRef1 = createTestFrame(1)
            val frameRef2 = createTestFrame(2)

            frameBuffer.onFrameStarted(frameRef1)
            frameRef1.close()
            frameBuffer.onFrameStarted(frameRef2)
            frameRef2.close()
            advanceUntilIdle()

            val frames = frameBuffer.tryRemoveAll()

            assertThat(frames.map { it.frameNumber })
                .containsExactly(frameRef1.frameNumber, frameRef2.frameNumber)
                .inOrder()
            assertThat(frames[0].isClosed()).isFalse()
            assertThat(frames[1].isClosed()).isFalse()
        }

    @Test
    fun onFrameStarted_acquiresFrameAndAddsItToQueue() =
        testScope.runTest {
            val frame1 = createTestFrame(1L)

            frameBuffer.onFrameStarted(frame1)
            frame1.close()
            advanceUntilIdle()

            val frameInQueue = frameBuffer.peekFirstReference()!!
            val frame = frameInQueue.tryAcquire()!!
            assertThat(frameBuffer.size.value).isEqualTo(1)
            assertThat(frameInQueue).isNotSameInstanceAs(frame1)
            assertThat(frameInQueue.frameNumber).isEqualTo(frame1.frameNumber)
            assertThat(frame.isClosed()).isFalse()
        }

    @Test
    fun onFrameStarted_exceedsCapacity_closesEvictedFrame() =
        testScope.runTest {
            val buffer = createFrameBuffer(capacity = 2)
            val frame1 = createTestFrame(1L)
            val frame2 = createTestFrame(2L)
            val frame3 = createTestFrame(3L)

            buffer.onFrameStarted(frame1)
            frame1.close()
            buffer.onFrameStarted(frame2)
            frame2.close()
            advanceUntilIdle()

            val peekedFrame1 = buffer.peekFirstReference()

            buffer.onFrameStarted(frame3)
            frame2.close()
            advanceUntilIdle()

            assertThat(buffer.size.value).isEqualTo(2)
            assertThat(peekedFrame1!!.tryAcquire()).isNull()
            val remainingFrames = buffer.peekAllReferences()
            assertThat(remainingFrames.map { it.frameNumber })
                .containsExactly(frame2.frameNumber, frame3.frameNumber)
                .inOrder()
        }

    @Test
    fun close_closesAllHeldFrames() =
        testScope.runTest {
            val frame1 = createTestFrame(1L)
            val frame2 = createTestFrame(2L)
            frameBuffer.onFrameStarted(frame1)
            frame1.close()
            frameBuffer.onFrameStarted(frame2)
            frame2.close()
            advanceUntilIdle()

            val firstPeekedFrame = frameBuffer.peekFirstReference()!!
            val lastPeekedFrame = frameBuffer.peekLastReference()!!

            frameBuffer.close()
            advanceUntilIdle()

            assertThat(frameBuffer.size.value).isEqualTo(0)
            assertThat(firstPeekedFrame.tryAcquire()).isNull()
            assertThat(lastPeekedFrame.tryAcquire()).isNull()
        }

    @Test
    fun close_keepsAcquiredFrameOpen() =
        testScope.runTest {
            val frame1 = createTestFrame(1L)
            val frame2 = createTestFrame(2L)
            frameBuffer.onFrameStarted(frame1)
            frame1.close()
            frameBuffer.onFrameStarted(frame2)
            frame2.close()
            advanceUntilIdle()

            val peekedFrame = frameBuffer.peekFirstReference()!!
            val acquiredFrame = peekedFrame.tryAcquire()!!

            frameBuffer.close()
            advanceUntilIdle()

            assertThat(frameBuffer.size.value).isEqualTo(0)
            assertThat(acquiredFrame.isClosed()).isFalse()
        }

    @Test
    fun setCapacity_increaseCapacity_doesNotEvictFrames() =
        testScope.runTest {
            val frame1 = createTestFrame(1)
            val frame2 = createTestFrame(2)
            val frame3 = createTestFrame(3)
            frameBuffer.onFrameStarted(frame1)
            frameBuffer.onFrameStarted(frame2)
            frameBuffer.onFrameStarted(frame3)
            val newCapacity = 4

            frameBuffer.capacity = newCapacity

            assertThat(frameBuffer.size.value).isEqualTo(defaultCapacity)
            assertThat(frameBuffer.capacity).isEqualTo(newCapacity)
            assertThat(frameBuffer.peekFirstReference()?.frameNumber?.value).isEqualTo(1L)

            frame1.close()
            frame2.close()
            frame3.close()
        }

    @Test
    fun setCapacity_sameCapacity_doesNothing() =
        testScope.runTest {
            val frame1 = createTestFrame(1)
            val frame2 = createTestFrame(2)
            val frame3 = createTestFrame(3)
            frameBuffer.onFrameStarted(frame1)
            frameBuffer.onFrameStarted(frame2)
            frameBuffer.onFrameStarted(frame3)

            frameBuffer.capacity = defaultCapacity

            assertThat(frameBuffer.size.value).isEqualTo(defaultCapacity)
            assertThat(frameBuffer.capacity).isEqualTo(defaultCapacity)

            frame1.close()
            frame2.close()
            frame3.close()
        }

    @Test
    fun setCapacity_decreaseCapacity_evictsOldestFrames() =
        testScope.runTest {
            val frame1 = createTestFrame(1)
            val frame2 = createTestFrame(2)
            val frame3 = createTestFrame(3)
            frameBuffer.onFrameStarted(frame1)
            frameBuffer.onFrameStarted(frame2)
            frameBuffer.onFrameStarted(frame3)
            advanceUntilIdle()
            val newCapacity = 2

            frameBuffer.capacity = newCapacity
            advanceUntilIdle()

            assertThat(frameBuffer.size.value).isEqualTo(newCapacity)
            assertThat(frameBuffer.capacity).isEqualTo(newCapacity)
            assertThat(frameBuffer.peekFirstReference()?.frameNumber?.value).isEqualTo(2L)

            frame1.close()
            frame2.close()
            frame3.close()
        }

    @Test
    fun setCapacity_afterClose_doesNothing() =
        testScope.runTest {
            val frame1 = createTestFrame(1)
            frameBuffer.onFrameStarted(frame1)
            frameBuffer.close()
            advanceUntilIdle()
            val newCapacity = 5

            frameBuffer.capacity = newCapacity

            assertThat(frameBuffer.capacity).isEqualTo(defaultCapacity)

            frame1.close()
        }

    @After
    fun cleanup() {
        fakeSurfaces.close()
    }

    private fun Frame.isClosed(): Boolean {
        return !(this.frameInfoStatus == OutputStatus.AVAILABLE &&
            this.imageStatus(stream1Id) == OutputStatus.AVAILABLE &&
            this.imageStatus(stream2Id) == OutputStatus.AVAILABLE &&
            this.getImage(stream1Id) != null &&
            this.getImage(stream2Id) != null)
    }
}
