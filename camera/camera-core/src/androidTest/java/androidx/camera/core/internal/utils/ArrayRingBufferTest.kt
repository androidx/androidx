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

package androidx.camera.core.internal.utils

import androidx.testutils.assertThrows
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito

@RunWith(JUnit4::class)
class ArrayRingBufferTest {

    @Test
    fun testEnqueue() {
        val testBuffer: RingBuffer<Int> =
            ArrayRingBuffer(3)
        testBuffer.enqueue(1)
        testBuffer.enqueue(2)
        testBuffer.enqueue(3)
        testBuffer.enqueue(4)
        Truth.assertThat(testBuffer.dequeue()).isEqualTo(2)
    }

    @Test
    fun testDequeue_correctValueIsDequeued() {
        @Suppress("UNCHECKED_CAST")
        val mockCallback: RingBuffer.OnRemoveCallback<Int> = Mockito.mock(
            RingBuffer.OnRemoveCallback::class.java
        ) as RingBuffer.OnRemoveCallback<Int>

        val testBuffer: RingBuffer<Int> =
            ArrayRingBuffer(
                3,
                mockCallback
            )
        testBuffer.enqueue(1)
        testBuffer.enqueue(2)
        testBuffer.enqueue(3)
        Truth.assertThat(testBuffer.dequeue()).isEqualTo(1)
        Mockito.verify(mockCallback, Mockito.times(0)).onRemove(any())
    }

    @Test
    fun testDequeue_OnRemoveCallbackCalledOnlyWhenDiscardingItemsDueToCapacity() {
        @Suppress("UNCHECKED_CAST")
        val mockCallback: RingBuffer.OnRemoveCallback<Int> = Mockito.mock(
            RingBuffer.OnRemoveCallback::class.java
        ) as RingBuffer.OnRemoveCallback<Int>

        val testBuffer: RingBuffer<Int> =
            ArrayRingBuffer(
                3,
                mockCallback
            )
        testBuffer.enqueue(1)
        testBuffer.enqueue(2)
        testBuffer.enqueue(3)
        testBuffer.enqueue(4)
        Mockito.verify(mockCallback).onRemove(1)
        Truth.assertThat(testBuffer.dequeue()).isEqualTo(2)
        Mockito.verify(mockCallback, Mockito.times(1)).onRemove(any())
    }

    @Test()
    fun testDequeue_exceptionThrownWhenBufferEmpty() {
        val testBuffer: RingBuffer<Int> =
            ArrayRingBuffer(5)
        assertThrows(NoSuchElementException::class.java, testBuffer::dequeue)
    }
}
