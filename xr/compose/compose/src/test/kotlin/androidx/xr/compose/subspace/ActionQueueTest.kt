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

package androidx.xr.compose.subspace

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActionQueueTest {
    @Test
    fun constructor_withNullInitialValue_valueIsNull() {
        val queue = ActionQueue<String>()
        assertNull(queue.value)
    }

    @Test
    fun constructor_withNonNullInitialValue_valueIsSet() {
        val queue = ActionQueue(initialValue = "initial")
        assertThat(queue.value).isEqualTo("initial")
    }

    @Test
    fun executeWhenAvailable_valueIsNull_actionIsQueuedAndReturnsNull() {
        val queue = ActionQueue<StringBuilder>()
        var executed = false
        val result =
            queue.executeWhenAvailable {
                executed = true
                append("test")
            }

        assertThat(executed).isFalse()
        assertNull(result)
    }

    @Test
    fun executeWhenAvailable_valueIsAvailable_actionIsExecutedImmediately() {
        val queue = ActionQueue(initialValue = StringBuilder("initial"))
        var executed = false
        val result =
            queue.executeWhenAvailable {
                executed = true
                append("-test")
                toString()
            }

        assertThat(executed).isTrue()
        assertThat(result).isEqualTo("initial-test")
        assertThat(queue.value.toString()).isEqualTo("initial-test")
    }

    @Test
    fun setValue_fromNullToNonNull_executesQueuedActions() {
        val queue = ActionQueue<StringBuilder>()
        val executionOrder = mutableListOf<Int>()

        queue.executeWhenAvailable {
            executionOrder.add(1)
            append("first-")
        }
        queue.executeWhenAvailable {
            executionOrder.add(2)
            append("second")
        }

        assertThat(executionOrder).isEmpty()

        queue.value = StringBuilder()

        assertThat(queue.value.toString()).isEqualTo("first-second")
        assertThat(executionOrder).containsExactly(1, 2).inOrder()
    }

    @Test
    fun clear_withQueuedActions_removesAllActions() {
        val queue = ActionQueue<StringBuilder>()
        queue.executeWhenAvailable { append("test") }

        queue.clear()
        queue.value = StringBuilder()

        assertThat(queue.value.toString()).isEmpty()
    }

    @Test
    fun setValue_fromNonNullToNull_doesNotExecuteActions() {
        val queue = ActionQueue(initialValue = "initial")
        queue.value = null
        var executed = false
        queue.executeWhenAvailable { executed = true }

        assertThat(executed).isFalse()
    }

    @Test
    fun setValue_withNull_doesNotExecuteActions() {
        val queue = ActionQueue<String>()
        var executed = false
        queue.executeWhenAvailable { executed = true }
        queue.value = null

        assertThat(executed).isFalse()
    }
}
