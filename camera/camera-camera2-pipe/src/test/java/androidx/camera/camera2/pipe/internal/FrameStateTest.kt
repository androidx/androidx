/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.internal

import android.util.Size
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.Frame
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.OutputStatus
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.graph.StreamGraphImpl
import androidx.camera.camera2.pipe.media.OutputImage
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeImage
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.FakeSurfaces
import com.google.common.truth.Truth.assertThat
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class FrameStateTest {

    private val stream1Id = StreamId(1)
    private val stream2Id = StreamId(2)
    private val stream3Id = StreamId(3)

    private val output1Id = OutputId(1)
    private val output2Id = OutputId(2)
    private val output3Id = OutputId(3)
    private val output4Id = OutputId(4)
    private val output5Id = OutputId(5)

    private val cameraId = CameraId("0")
    private val outputStream1 =
        StreamGraphImpl.OutputStreamImpl(
            output1Id,
            Size(640, 480),
            StreamFormat.YUV_420_888,
            cameraId,
        )
    private val outputStream2 =
        StreamGraphImpl.OutputStreamImpl(
            output2Id,
            Size(640, 480),
            StreamFormat.YUV_420_888,
            cameraId,
        )
    private val outputStream3 =
        StreamGraphImpl.OutputStreamImpl(output3Id, Size(640, 480), StreamFormat.RAW10, cameraId)
    private val outputStream4 =
        StreamGraphImpl.OutputStreamImpl(output4Id, Size(720, 480), StreamFormat.RAW10, cameraId)
    private val outputStream5 =
        StreamGraphImpl.OutputStreamImpl(output5Id, Size(1600, 900), StreamFormat.RAW10, cameraId)
    private val stream1 =
        CameraStream(stream1Id, listOf(outputStream1)).apply { outputStream1.stream = this }
    private val stream2 =
        CameraStream(stream2Id, listOf(outputStream2)).apply { outputStream2.stream = this }
    private val stream3 =
        CameraStream(stream3Id, listOf(outputStream3, outputStream4, outputStream5)).apply {
            outputStream3.stream = this
            outputStream4.stream = this
            outputStream5.stream = this
        }

    private val frameNumber = FrameNumber(420)
    private val frameTimestampNs = 1234L
    private val frameTimestamp = CameraTimestamp(frameTimestampNs)

    private val imageStreams = setOf(stream1, stream2, stream3)
    private val fakeImage = FakeImage(640, 480, StreamFormat.YUV_420_888.value, frameTimestampNs)
    private val outputImage = OutputImage.from(stream1Id, output1Id, fakeImage)
    private val fakeSurfaces = FakeSurfaces()
    private val stream1Surface = fakeSurfaces.createFakeSurface()
    private val stream2Surface = fakeSurfaces.createFakeSurface()
    private val stream3Surface = fakeSurfaces.createFakeSurface()

    private val fakeRequestMetadata =
        FakeRequestMetadata(
            streams =
                mapOf(
                    stream1Id to stream1Surface,
                    stream2Id to stream2Surface,
                    stream3Id to stream3Surface,
                )
        )
    private val fakeFrameMetadata = FakeFrameMetadata(frameNumber = frameNumber)
    private val fakeFrameInfo =
        FakeFrameInfo(metadata = fakeFrameMetadata, requestMetadata = fakeRequestMetadata)

    private val fakeListener =
        object : Frame.Listener {
            val frameStartedCalled = atomic(0)
            val frameInfoAvailableCalled = atomic(0)
            val imagesAvailableCalled = atomic(0)
            val frameCompletedCalled = atomic(0)

            override fun onFrameStarted(frameNumber: FrameNumber, frameTimestamp: CameraTimestamp) {
                frameStartedCalled.incrementAndGet()
            }

            override fun onFrameInfoAvailable() {
                frameInfoAvailableCalled.incrementAndGet()
            }

            override fun onImageAvailable(streamId: StreamId) {
                // Do nothing. ListenerState doesn't care about onImageAvailable on stream level
                // currently.
            }

            override fun onImagesAvailable() {
                imagesAvailableCalled.incrementAndGet()
            }

            override fun onFrameComplete() {
                frameCompletedCalled.incrementAndGet()
            }
        }

    private val frameState =
        FrameState(
            requestMetadata = fakeRequestMetadata,
            frameNumber = frameNumber,
            frameTimestamp = frameTimestamp,
            imageStreams = imageStreams,
            concurrentImageStreams = setOf(),
        )

    private val imageResult1 = frameState.imageOutputs.first { it.streamId == stream1Id }
    private val imageResult2 = frameState.imageOutputs.first { it.streamId == stream2Id }

    @After
    fun cleanup() {
        fakeSurfaces.close()
    }

    @Test
    fun streamResultsAreNotStartedByDefault() {
        assertThat(imageResult1.status).isEqualTo(OutputStatus.PENDING)
    }

    @Test
    fun streamResultKeepsCountBeforeCancelling() {
        imageResult1.increment() // 2
        imageResult1.increment() // 3
        imageResult1.decrement() // 2
        imageResult1.increment() // 3
        imageResult1.decrement() // 2
        imageResult1.decrement() // 1

        assertThat(imageResult1.status).isEqualTo(OutputStatus.PENDING)

        imageResult1.decrement() // 0 -> Close/Cancel

        assertThat(imageResult1.status).isEqualTo(OutputStatus.UNAVAILABLE)
    }

    @Test
    fun streamResultClosesOutputAfterCountReachesZero() {
        imageResult1.onOutputComplete(
            frameNumber,
            frameTimestamp,
            64L,
            frameTimestampNs,
            OutputResult.from(outputImage),
        )

        assertThat(fakeImage.isClosed).isFalse()
        assertThat(imageResult1.status).isEqualTo(OutputStatus.AVAILABLE)

        imageResult1.decrement() // 0 -> Close/Cancel

        assertThat(fakeImage.isClosed).isTrue()
        assertThat(imageResult1.status).isEqualTo(OutputStatus.UNAVAILABLE)

        val result = imageResult1.outputOrNull()
        assertThat(result).isNull()
    }

    @Test
    fun streamResultAfterCountReachesZeroIsClosed() {
        imageResult1.decrement() // 0 -> Close/Cancel

        imageResult1.onOutputComplete(
            frameNumber,
            frameTimestamp,
            64L,
            frameTimestampNs,
            OutputResult.from(outputImage),
        )

        assertThat(fakeImage.isClosed).isTrue()
        assertThat(imageResult1.outputOrNull()).isNull()
    }

    @Test
    fun acquiringAnImageAndThenClosingResultDoesNotCloseImage() {
        imageResult1.onOutputComplete(
            frameNumber,
            frameTimestamp,
            64L,
            frameTimestampNs,
            OutputResult.from(outputImage),
        )
        val imageCopy1 = imageResult1.outputOrNull()
        val imageCopy2 = imageResult1.outputOrNull()

        assertThat(imageCopy1).isNotNull()
        assertThat(imageCopy2).isNotNull()
        assertThat(fakeImage.isClosed).isFalse()

        imageResult1.decrement() // 0 -> Close/Cancel

        assertThat(fakeImage.isClosed).isFalse()

        imageCopy2!!.close()
        assertThat(fakeImage.isClosed).isFalse()

        imageCopy1!!.close() // All references are released, closing the underlying image.
        assertThat(fakeImage.isClosed).isTrue()
    }

    @Test
    fun frameInfoResultCanBeCompleted() {
        frameState.frameInfoOutput.onOutputComplete(
            frameNumber,
            frameTimestamp,
            10,
            frameNumber.value,
            OutputResult.from(fakeFrameInfo),
        )

        assertThat(frameState.frameInfoOutput.status).isEqualTo(OutputStatus.AVAILABLE)
        assertThat(frameState.frameInfoOutput.outputOrNull()).isSameInstanceAs(fakeFrameInfo)
    }

    @Test
    fun frameInfoResultCanBeCompletedWithAResultWithADifferentFrameNumber() {
        frameState.frameInfoOutput.onOutputComplete(
            FrameNumber(1),
            frameTimestamp,
            10,
            1,
            OutputResult.from(fakeFrameInfo),
        )

        assertThat(frameState.frameInfoOutput.status).isEqualTo(OutputStatus.AVAILABLE)
        assertThat(frameState.frameInfoOutput.outputOrNull()).isSameInstanceAs(fakeFrameInfo)
    }

    @Test
    fun frameInfoResultAfterCanceledIsNull() {
        frameState.frameInfoOutput.decrement()
        frameState.frameInfoOutput.onOutputComplete(
            frameNumber,
            frameTimestamp,
            10,
            frameNumber.value,
            OutputResult.from(fakeFrameInfo),
        )

        assertThat(frameState.frameInfoOutput.status).isEqualTo(OutputStatus.UNAVAILABLE)
        assertThat(frameState.frameInfoOutput.outputOrNull()).isNull()
    }

    @Test
    fun addListener_invokesOnStarted_whenStateIsStarted() {
        // FrameState's initial state is STARTED
        frameState.addListener(fakeListener)

        assertThat(fakeListener.frameStartedCalled.value).isEqualTo(1)
        assertThat(fakeListener.frameInfoAvailableCalled.value).isEqualTo(0)
        assertThat(fakeListener.imagesAvailableCalled.value).isEqualTo(0)
        assertThat(fakeListener.frameCompletedCalled.value).isEqualTo(0)
    }

    @Test
    fun addListener_stateIsFrameInfoAvailable_invokesStartAndFrameInfoComplete() {
        frameState.onFrameInfoComplete()

        frameState.addListener(fakeListener)

        assertThat(fakeListener.frameStartedCalled.value).isEqualTo(1)
        assertThat(fakeListener.frameInfoAvailableCalled.value).isEqualTo(1)
        assertThat(fakeListener.imagesAvailableCalled.value).isEqualTo(0)
        assertThat(fakeListener.frameCompletedCalled.value).isEqualTo(0)
    }

    @Test
    fun addListener_stateIsImagesAvailable_invokesStartAndImagesAvailable() {
        // All stream result completed
        frameState.onStreamResultComplete(stream1Id)
        frameState.onStreamResultComplete(stream2Id)
        frameState.onStreamResultComplete(stream3Id)

        frameState.addListener(fakeListener)

        assertThat(fakeListener.frameStartedCalled.value).isEqualTo(1)
        assertThat(fakeListener.frameInfoAvailableCalled.value).isEqualTo(0)
        assertThat(fakeListener.imagesAvailableCalled.value).isEqualTo(1)
        assertThat(fakeListener.frameCompletedCalled.value).isEqualTo(0)
    }

    @Test
    fun addListener_stateIsFrameComplete_invokesAllCallbacks() {
        frameState.onStreamResultComplete(stream1Id)
        frameState.onStreamResultComplete(stream2Id)
        frameState.onStreamResultComplete(stream3Id)
        frameState.onFrameInfoComplete()

        frameState.addListener(fakeListener)

        assertThat(fakeListener.frameStartedCalled.value).isEqualTo(1)
        assertThat(fakeListener.frameInfoAvailableCalled.value).isEqualTo(1)
        assertThat(fakeListener.imagesAvailableCalled.value).isEqualTo(1)
        assertThat(fakeListener.frameCompletedCalled.value).isEqualTo(1)
    }

    @Test
    fun onFrameInfoComplete_invokesOnFrameInfoAvailable() {
        frameState.addListener(fakeListener)

        frameState.onFrameInfoComplete()

        assertThat(fakeListener.frameStartedCalled.value).isEqualTo(1)
        assertThat(fakeListener.frameInfoAvailableCalled.value).isEqualTo(1)
        assertThat(fakeListener.imagesAvailableCalled.value).isEqualTo(0)
        assertThat(fakeListener.frameCompletedCalled.value).isEqualTo(0)
    }

    @Test
    fun onStreamResultComplete_doesNotHaveStreamResultForAllStreams_doesNotInvokesOnImagesAvailable() {
        frameState.addListener(fakeListener)

        frameState.onStreamResultComplete(stream1Id)

        assertThat(fakeListener.frameStartedCalled.value).isEqualTo(1)
        assertThat(fakeListener.frameInfoAvailableCalled.value).isEqualTo(0)
        assertThat(fakeListener.imagesAvailableCalled.value).isEqualTo(0)
        assertThat(fakeListener.frameCompletedCalled.value).isEqualTo(0)
    }

    @Test
    fun onStreamResultComplete_invokesOnImagesAvailable_afterAllStreamsComplete() {
        frameState.addListener(fakeListener)

        frameState.onStreamResultComplete(stream1Id)
        frameState.onStreamResultComplete(stream2Id)
        frameState.onStreamResultComplete(stream3Id)

        assertThat(fakeListener.frameStartedCalled.value).isEqualTo(1)
        assertThat(fakeListener.frameInfoAvailableCalled.value).isEqualTo(0)
        assertThat(fakeListener.imagesAvailableCalled.value).isEqualTo(1)
        assertThat(fakeListener.frameCompletedCalled.value).isEqualTo(0)
    }

    @Test
    fun frameState_transitionsToComplete_allCallbacksAreTriggered() {
        frameState.addListener(fakeListener)

        frameState.onFrameInfoComplete()
        frameState.onStreamResultComplete(stream1Id)
        frameState.onStreamResultComplete(stream2Id)
        frameState.onStreamResultComplete(stream3Id)

        assertThat(fakeListener.frameStartedCalled.value).isEqualTo(1)
        assertThat(fakeListener.frameInfoAvailableCalled.value).isEqualTo(1)
        assertThat(fakeListener.imagesAvailableCalled.value).isEqualTo(1)
        assertThat(fakeListener.frameCompletedCalled.value).isEqualTo(1)
    }

    @Test
    fun concurrentFrameStateChangeAndNewListenerAdded_ensureCallbacksCalledOnce() = runBlocking {
        val numCoroutines = 4

        val jobs =
            listOf(
                launch(Dispatchers.Default) { frameState.onFrameInfoComplete() },
                launch(Dispatchers.Default) { frameState.onStreamResultComplete(stream1Id) },
                launch(Dispatchers.Default) { frameState.addListener(fakeListener) },
                launch(Dispatchers.Default) { frameState.onStreamResultComplete(stream2Id) },
                launch(Dispatchers.Default) { frameState.onStreamResultComplete(stream3Id) },
            )
        jobs.joinAll()

        assertThat(fakeListener.frameStartedCalled.value).isEqualTo(1)
        assertThat(fakeListener.frameInfoAvailableCalled.value).isEqualTo(1)
        assertThat(fakeListener.imagesAvailableCalled.value).isEqualTo(1)
        assertThat(fakeListener.frameCompletedCalled.value).isEqualTo(1)
    }
}
