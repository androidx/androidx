/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.health.connect.client.testing.stubs

import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test

/** Unit tests for [MutableStub]. */
class MutableStubTest {

    private val exception = IllegalArgumentException()
    private val value = "1"
    private val defaultValue = "testDefault"

    @Test
    fun defaultElement() {
        val stub = MutableStub(value)
        // The first call to next returns the value
        assertThat(stub.next(0)).isEqualTo(value)
        // and so do subsequent calls
        assertThat(stub.next(0)).isEqualTo(value)
    }

    @Test
    fun enqueuedElementWithDefault() {
        val stub = MutableStub(defaultValue).apply { enqueue(value) }
        // The first call to next returns the value
        assertThat(stub.next(0)).isEqualTo(value)
        // but the second returns the default
        assertThat(stub.next(0)).isEqualTo(defaultValue)
        // third one returns the default
        assertThat(stub.next(0)).isEqualTo(defaultValue)
    }

    @Test
    fun defaultException() {
        val stub = MutableStub<String>(exception)

        // The first call to next throws an exception
        Assert.assertThrows(exception::class.java) { stub.next(0) }
    }

    @Test
    fun queueAndDefaultException() {
        val stub = MutableStub<String>(exception).apply { enqueue(value) }

        // The first one reads the queue
        assertThat(stub.next(0)).isEqualTo(value)
        // The second call to next throws an exception
        Assert.assertThrows(exception::class.java) { stub.next(0) }
    }

    @Test
    fun plusAssignOperator() {
        val stub = MutableStub(defaultValue)
        stub += value

        // The first one reads the queue
        assertThat(stub.next(0)).isEqualTo(value)
        assertThat(stub.next(0)).isEqualTo(defaultValue)
    }

    @Test
    fun builder_queueAndDefault() {
        val stub =
            buildStub<Int, String> {
                enqueue(value)
                defaultHandler = { defaultValue }
            }

        // The first one reads the queue
        assertThat(stub.next(0)).isEqualTo(value)
        // but the second returns the default
        assertThat(stub.next(0)).isEqualTo(defaultValue)
        // third one returns the default
        assertThat(stub.next(0)).isEqualTo(defaultValue)
    }

    @Test
    fun builder_enqueueList() {
        val stub =
            buildStub<Int, String> {
                defaultHandler = { defaultValue }
                enqueue(listOf("1", "2"))
            }

        // The first one reads the queue
        assertThat(stub.next(0)).isEqualTo("1")
        // and the second one
        assertThat(stub.next(0)).isEqualTo("2")
        // third one returns the default
        assertThat(stub.next(0)).isEqualTo(defaultValue)
        // fourth one returns the default
        assertThat(stub.next(0)).isEqualTo(defaultValue)
    }

    @Test
    fun defaultHandlerSet_clearsExceptionDefault() {
        val stub = MutableStub<Int, String> { throw exception }

        stub.defaultHandler = { defaultValue }

        // First one returns the default
        assertThat(stub.next(0)).isEqualTo(defaultValue)
    }

    @Test
    fun exceptionConstructorWithType_enqueue() {
        val stub = MutableStub<String>(exception)

        stub.enqueue("1")

        // First one returns item from queue
        assertThat(stub.next(0)).isEqualTo("1")
        // The second call to next throws an exception
        Assert.assertThrows(exception::class.java) { stub.next(0) }
    }
}
