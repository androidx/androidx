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

package androidx.camera.video.internal.utils

import android.os.Build
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ArrayDequeRingBufferTest {

    private val capacity = 10

    @Test
    fun getCapacity_returnCorrectResult() {
        val ringBuffer = ArrayDequeRingBuffer<Any>(capacity)
        Truth.assertThat(ringBuffer.capacity()).isEqualTo(capacity)
    }

    @Test
    fun getSize_returnCorrectResult() {
        val offerTimes = 5

        val ringBuffer = ArrayDequeRingBuffer<Any>(capacity)
        for (i in 0 until offerTimes) {
            ringBuffer.offer(i)
        }

        Truth.assertThat(ringBuffer.size()).isEqualTo(offerTimes)
    }

    @Test
    fun getSize_returnCorrectResultWhenReachCapacity() {
        val offerTimes = 15

        val ringBuffer = ArrayDequeRingBuffer<Any>(capacity)
        for (i in 0 until offerTimes) {
            ringBuffer.offer(i)
        }

        Truth.assertThat(ringBuffer.size()).isEqualTo(capacity)
    }

    @Test
    fun getSize_returnCorrectResultWhenDataAddedThenRemoved() {
        val offerTimes = 7
        val pollTimes = 3

        val ringBuffer = ArrayDequeRingBuffer<Any>(capacity)
        for (i in 0 until offerTimes) {
            ringBuffer.offer(i)
        }
        for (i in 0 until pollTimes) {
            ringBuffer.poll()
        }

        Truth.assertThat(ringBuffer.size()).isEqualTo(offerTimes - pollTimes)
    }

    @Test
    fun remove_returnCorrectResult() {
        val offerTimes = 5

        val ringBuffer = ArrayDequeRingBuffer<Any>(capacity)
        for (i in 0 until offerTimes) {
            ringBuffer.offer(i)
        }
        for (i in 0 until offerTimes) {
            Truth.assertThat(ringBuffer.poll()).isEqualTo(i)
        }
    }

    @Test
    fun peek_returnCorrectResult() {
        val offeredValues = listOf(0, 1, 2, 3, 4)
        val pollTimes = 3
        val expectedPeekResult = 3

        val ringBuffer = ArrayDequeRingBuffer<Any>(capacity)
        for (value in offeredValues) {
            ringBuffer.offer(value)
        }
        for (i in 0 until pollTimes) {
            ringBuffer.poll()
        }

        Truth.assertThat(ringBuffer.peek()).isEqualTo(expectedPeekResult)
    }

    @Test
    fun isEmpty_returnCorrectResultAfterDataAdded() {
        val ringBuffer = ArrayDequeRingBuffer<Any>(capacity)
        ringBuffer.offer(Any())

        Truth.assertThat(ringBuffer.isEmpty).isFalse()
    }

    @Test
    fun isEmpty_returnCorrectResultAfterClear() {
        val offerTimes = 5

        val ringBuffer = ArrayDequeRingBuffer<Any>(capacity)
        for (i in 0 until offerTimes) {
            ringBuffer.offer(i)
        }
        ringBuffer.clear()

        Truth.assertThat(ringBuffer.isEmpty).isTrue()
    }
}