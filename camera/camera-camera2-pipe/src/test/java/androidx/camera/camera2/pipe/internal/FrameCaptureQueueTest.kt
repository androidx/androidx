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
import androidx.camera.camera2.pipe.OutputStatus
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamId
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [FrameCaptureQueue] */
@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class FrameCaptureQueueTest {
    private val imageStreams = listOf(StreamId(1), StreamId(2), StreamId(3))
    private val captureQueue = FrameCaptureQueue()

    private val request1 = Request(streams = imageStreams)
    private val request2 = Request(streams = imageStreams)
    private val request3 = Request(streams = imageStreams)

    @Test
    fun outputFrameQueueHoldsDeferredFrames() {
        val frameCapture = captureQueue.enqueue(request1)
        assertThat(frameCapture.status).isEqualTo(OutputStatus.PENDING)
    }

    @Test
    fun outputFrameQueueCanRemoveDeferredFrames() {
        val deferred = captureQueue.enqueue(request1)

        val deferred1 = captureQueue.remove(request1)
        val deferred2 = captureQueue.remove(request1)
        assertThat(deferred1).isSameInstanceAs(deferred)
        assertThat(deferred2).isNull()
    }

    @Test
    fun outputFrameQueueHoldsMultipleDeferredFramesInOrder() {
        val deferred1 = captureQueue.enqueue(request1)
        val deferred2 = captureQueue.enqueue(request1)
        val deferred3 = captureQueue.enqueue(request1)

        assertThat(deferred1).isNotSameInstanceAs(deferred2)
        assertThat(deferred2).isNotSameInstanceAs(deferred3)

        val removedFrame1 = captureQueue.remove(request1)
        val removedFrame2 = captureQueue.remove(request1)
        val removedFrame3 = captureQueue.remove(request1)
        val removedFrame4 = captureQueue.remove(request1)

        assertThat(removedFrame1).isSameInstanceAs(deferred1)
        assertThat(removedFrame2).isSameInstanceAs(deferred2)
        assertThat(removedFrame3).isSameInstanceAs(deferred3)
        assertThat(removedFrame4).isNull()
    }

    @Test
    fun outputFrameQueueCanRemoveIntermixedRequests() {
        // Intermixed requests (2 request1's, 1 request2, 1 request3)
        val frame1 = captureQueue.enqueue(request1)
        val frame2 = captureQueue.enqueue(request2)
        val frame3 = captureQueue.enqueue(request1)
        val frame4 = captureQueue.enqueue(request3)

        // Remove request1's first
        val removedFrame1 = captureQueue.remove(request1)
        val removedFrame2 = captureQueue.remove(request1)
        val removedFrame3 = captureQueue.remove(request3)
        val removedFrame4 = captureQueue.remove(request2)

        assertThat(removedFrame1).isSameInstanceAs(frame1)
        assertThat(removedFrame2).isSameInstanceAs(frame3)
        assertThat(removedFrame3).isSameInstanceAs(frame4)
        assertThat(removedFrame4).isSameInstanceAs(frame2)
    }

    @Test
    fun closingOutputFrameRemovesItFromOutputFrameQueue() {
        val frame1 = captureQueue.enqueue(request1)
        val frame2 = captureQueue.enqueue(request1)
        val frame3 = captureQueue.enqueue(request1)

        frame2.close()

        val removedFrame1 = captureQueue.remove(request1)
        val removedFrame2 = captureQueue.remove(request1)
        val removedFrame3 = captureQueue.remove(request1)

        assertThat(removedFrame1).isSameInstanceAs(frame1)
        assertThat(removedFrame2).isSameInstanceAs(frame3)
        assertThat(removedFrame3).isNull()
    }

    @Test
    fun closingDeferredFrameCancelsResults() {
        val frame = captureQueue.enqueue(request1)
        frame.close()

        val frameFromQueue = captureQueue.remove(request1)
        assertThat(frameFromQueue).isNull()
    }

    @Test
    fun canEnqueueMultipleResults() {
        val frames = captureQueue.enqueue(listOf(request1, request1, request1))
        assertThat(frames.size).isEqualTo(3)

        val removedFrame1 = captureQueue.remove(request1)
        val removedFrame2 = captureQueue.remove(request1)
        val removedFrame3 = captureQueue.remove(request1)

        assertThat(removedFrame1).isSameInstanceAs(frames[0])
        assertThat(removedFrame2).isSameInstanceAs(frames[1])
        assertThat(removedFrame3).isSameInstanceAs(frames[2])
    }
}
