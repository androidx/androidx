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

/** Unit tests for [Stub]. */
class StubTest {

    @Test
    fun defaultElement_shouldAlwaysReturnDefault() {
        val value = "test"
        val stub = stub(default = value)
        // The first call to next returns the value
        assertThat(stub.next(0)).isEqualTo(value)
        // and so do subsequent calls
        assertThat(stub.next(0)).isEqualTo(value)
    }

    @Test
    fun enqueuedElementWithDefault_shoulReturnElementAndThenDefault() {
        val value = "test"
        val defaultValue = "testDefault"
        val stub = stub<String>(queue = listOf(value)) { defaultValue }
        // The first call to next returns the value
        assertThat(stub.next(0)).isEqualTo(value)
        // but the second returns the default
        assertThat(stub.next(0)).isEqualTo(defaultValue)
        // third one returns the default
        assertThat(stub.next(0)).isEqualTo(defaultValue)
    }

    @Test
    fun stubFunction_defaultException_shouldThrow() {
        val exception = IllegalArgumentException()
        val stub = stub<String> { throw exception }

        // The first call to next throws an exception
        Assert.assertThrows(exception::class.java) { stub.next(0) }
    }

    @Test
    fun stubFunction_queueAndException_shouldReturnAndThenThrow() {
        val exception = IllegalArgumentException()
        val value = "1"
        val stub = stub(defaultHandler = { throw exception }, queue = listOf(value))

        // The first one reads the queue
        assertThat(stub.next(0)).isEqualTo(value)
        // The second call to next throws an exception
        Assert.assertThrows(exception::class.java) { stub.next(0) }
    }
}
