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

import android.os.Build
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.OutputStatus
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.media.OutputImage
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeImage
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.FakeSurfaces
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class FrameStateTest {

    private val stream1Id = StreamId(1)
    private val stream2Id = StreamId(2)

    private val output1Id = OutputId(1)

    private val frameNumber = FrameNumber(420)
    private val frameTimestampNs = 1234L
    private val frameTimestamp = CameraTimestamp(frameTimestampNs)

    private val imageStreams = setOf(stream1Id, stream2Id)
    private val fakeImage = FakeImage(640, 480, StreamFormat.YUV_420_888.value, frameTimestampNs)
    private val outputImage = OutputImage.from(
        stream1Id,
        output1Id,
        fakeImage
    )
    private val fakeSurfaces = FakeSurfaces()
    private val stream1Surface = fakeSurfaces.createFakeSurface()
    private val stream2Surface = fakeSurfaces.createFakeSurface()

    private val fakeRequestMetadata = FakeRequestMetadata(
        streams = mapOf(stream1Id to stream1Surface, stream2Id to stream2Surface)
    )
    private val fakeFrameMetadata = FakeFrameMetadata(
        frameNumber = frameNumber
    )
    private val fakeFrameInfo = FakeFrameInfo(
        metadata = fakeFrameMetadata,
        requestMetadata = fakeRequestMetadata
    )

    private val frameState = FrameState(
        requestMetadata = fakeRequestMetadata,
        frameNumber = frameNumber,
        frameTimestamp = frameTimestamp,
        imageStreams
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
            OutputResult.from(outputImage)
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
            OutputResult.from(outputImage)
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
            OutputResult.from(outputImage)
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
            OutputResult.from(fakeFrameInfo)
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
            OutputResult.from(fakeFrameInfo)
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
            OutputResult.from(fakeFrameInfo)
        )

        assertThat(frameState.frameInfoOutput.status).isEqualTo(OutputStatus.UNAVAILABLE)
        assertThat(frameState.frameInfoOutput.outputOrNull()).isNull()
    }
}
