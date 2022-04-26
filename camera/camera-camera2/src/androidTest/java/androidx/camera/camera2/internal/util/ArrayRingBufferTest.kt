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
package androidx.camera.camera2.internal.util

import androidx.camera.camera2.internal.util.RingBuffer.OnRemoveCallback
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(JUnit4::class)
class ArrayRingBufferTest {

    @Test
    fun testEnqueue() {
        val testBuffer: RingBuffer<Int> =
            androidx.camera.camera2.internal.util.ArrayRingBuffer(3)
        testBuffer.enqueue(1)
        testBuffer.enqueue(2)
        testBuffer.enqueue(3)
        testBuffer.enqueue(4)
        assertThat(testBuffer.dequeue()).isEqualTo(2)
    }

    @Test
    fun testDequeue_correctValueIsDequeued() {
        @Suppress("UNCHECKED_CAST")
        val mockCallback: OnRemoveCallback<Int> = mock(
            OnRemoveCallback::class.java) as OnRemoveCallback<Int>

        val testBuffer: RingBuffer<Int> =
            androidx.camera.camera2.internal.util.ArrayRingBuffer(
                3,
                mockCallback
            )
        testBuffer.enqueue(1)
        testBuffer.enqueue(2)
        testBuffer.enqueue(3)
        assertThat(testBuffer.dequeue()).isEqualTo(1)
        verify(mockCallback, times(0)).onRemove(any())
    }

    @Test
    fun testDequeue_OnRemoveCallbackCalledOnlyWhenDiscardingItemsDueToCapacity() {
        @Suppress("UNCHECKED_CAST")
        val mockCallback: OnRemoveCallback<Int> = mock(
            OnRemoveCallback::class.java) as OnRemoveCallback<Int>

        val testBuffer: RingBuffer<Int> =
            androidx.camera.camera2.internal.util.ArrayRingBuffer(
                3,
                mockCallback
            )
        testBuffer.enqueue(1)
        testBuffer.enqueue(2)
        testBuffer.enqueue(3)
        testBuffer.enqueue(4)
        verify(mockCallback).onRemove(1)
        assertThat(testBuffer.dequeue()).isEqualTo(2)
        verify(mockCallback, times(1)).onRemove(any())
    }

    @Test()
    fun testDequeue_exceptionThrownWhenBufferEmpty() {
        val testBuffer: RingBuffer<Int> =
            androidx.camera.camera2.internal.util.ArrayRingBuffer(5)
        assertThrows(NoSuchElementException::class.java, testBuffer::dequeue)
    }
}