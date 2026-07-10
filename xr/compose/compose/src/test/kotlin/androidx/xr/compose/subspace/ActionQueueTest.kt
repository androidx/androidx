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
    fun constructor_withInvalidInitialValue_valueIsNull() {
        val queue = ActionQueue(initialValue = "invalid", isValid = { it != "invalid" })
        assertNull(queue.value)
    }

    @Test
    fun clear_withQueuedActions_removesAllActions() {
        val queue = ActionQueue<StringBuilder>()
        queue.executeWhenAvailable { it.append("test") }

        queue.clear()
        queue.value = StringBuilder()

        assertThat(queue.value.toString()).isEmpty()
    }

    @Test
    fun executeWhenAvailable_valueIsNull_actionIsQueuedAndReturnsNull() {
        val queue = ActionQueue<StringBuilder>()
        var executed = false
        val result =
            queue.executeWhenAvailable { builder ->
                executed = true
                builder.append("test")
            }

        assertThat(executed).isFalse()
        assertNull(result)
    }

    @Test
    fun executeWhenAvailable_valueIsInvalidatedByAction_queueIsStopped() {
        val queue =
            ActionQueue(
                initialValue = StringBuilder("in"),
                isValid = { it.toString() != "invalid" },
            )
        val executions = mutableListOf<Int>()

        queue.executeWhenAvailable { executions += 1 }
        queue.executeWhenAvailable { builder ->
            executions += 2
            builder.append("valid")
        }
        queue.executeWhenAvailable { executions += 3 }

        assertNull(queue.value)
        assertThat(executions).containsExactly(1, 2).inOrder()
    }

    @Test
    fun executeWhenAvailable_valueIsInvalidatedBetweenActions_queueIsStopped() {
        val queue =
            ActionQueue(
                initialValue = StringBuilder("in"),
                isValid = { it.toString() != "invalid" },
            )
        val executions = mutableListOf<Int>()

        queue.executeWhenAvailable { executions += 1 }
        queue.executeWhenAvailable { executions += 2 }

        queue.value?.append("valid")

        queue.executeWhenAvailable { executions += 3 }

        assertNull(queue.value)
        assertThat(executions).containsExactly(1, 2).inOrder()
    }

    @Test
    fun executeWhenAvailable_valueIsAvailable_actionIsExecutedImmediately() {
        val queue = ActionQueue(initialValue = StringBuilder("initial"))
        var executed = false
        val result =
            queue.executeWhenAvailable { builder ->
                executed = true
                builder.append("-test")
                builder.toString()
            }

        assertThat(executed).isTrue()
        assertThat(result).isEqualTo("initial-test")
        assertThat(queue.value.toString()).isEqualTo("initial-test")
    }

    @Test
    fun executeWhenAvailable_valueIsNull_actionIsExecutedImmediately() {
        val queue = ActionQueue(initialValue = StringBuilder("initial"))
        var executed = false
        val result =
            queue.executeWhenAvailable { builder ->
                executed = true
                builder.append("-test")
                builder.toString()
            }

        assertThat(executed).isTrue()
        assertThat(result).isEqualTo("initial-test")
        assertThat(queue.value.toString()).isEqualTo("initial-test")
    }

    @Test
    fun executeWhenAvailable_valueIsAvailable_actionIsNotQueued() {
        val queue = ActionQueue(initialValue = StringBuilder("initial"))
        var executions = 0

        // This action should execute immediately and not be queued.
        queue.executeWhenAvailable {
            executions++
            it.append("-executed")
            // return null to ensure that the return value is not used to determine eligibility for
            // queueing this action
            null
        }

        // Verify immediate execution.
        assertThat(executions).isEqualTo(1)
        assertThat(queue.value.toString()).isEqualTo("initial-executed")

        // Set the value to null and then provide a new value. If the original action was
        // queued, it would execute again here.
        queue.value = null
        queue.value = StringBuilder("new")

        // Verify the action was not executed again.
        assertThat(executions).isEqualTo(1)
        assertThat(queue.value.toString()).isEqualTo("new")
    }

    @Test
    fun setValue_fromNullToNonNull_executesQueuedActions() {
        val queue = ActionQueue<StringBuilder>()
        val executionOrder = mutableListOf<Int>()

        queue.executeWhenAvailable { builder ->
            executionOrder.add(1)
            builder.append("first-")
        }
        queue.executeWhenAvailable { builder ->
            executionOrder.add(2)
            builder.append("second")
        }

        assertThat(executionOrder).isEmpty()

        queue.value = StringBuilder()

        assertThat(queue.value.toString()).isEqualTo("first-second")
        assertThat(executionOrder).containsExactly(1, 2).inOrder()
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

    @Test
    fun setValue_withInvalidValue_doesNotExecuteActions() {
        val queue = ActionQueue<StringBuilder>(isValid = { it.toString() != "invalid" })
        var executed = 0

        queue.executeWhenAvailable { executed++ }

        queue.value = StringBuilder("invalid")

        assertNull(queue.value)
        assertThat(executed).isEqualTo(0)
    }

    @Test
    fun setValue_withInvalidatedValue_doesNotProceedExecutions() {
        val queue = ActionQueue<StringBuilder>(isValid = { it.toString() != "invalid" })
        val executions = mutableListOf<Int>()

        queue.executeWhenAvailable { executions += 1 }
        queue.executeWhenAvailable { builder ->
            executions += 2
            builder.append("valid")
        }
        queue.executeWhenAvailable { executions += 3 }

        queue.value = StringBuilder("in")
        assertNull(queue.value)
        assertThat(executions).containsExactly(1, 2).inOrder()
    }

    @Test
    fun setValue_inQueuedAction_doesNotRecurse() {
        val queue = ActionQueue<StringBuilder>()
        val executions = mutableListOf<Int>()

        queue.executeWhenAvailable {
            executions.add(1)
            // This setter should not trigger re-execution of the action queue.
            queue.value = StringBuilder("new value")
            executions.add(2)
        }

        // This triggers the queued action.
        queue.value = StringBuilder("initial")

        assertThat(queue.value.toString()).isEqualTo("new value")
        assertThat(executions).containsExactly(1, 2).inOrder()
    }

    @Test
    fun setValue_inImmediatelyExecutedAction_doesNotRecurse() {
        val queue = ActionQueue(initialValue = StringBuilder("initial"))
        val executions = mutableListOf<Int>()

        // This action will be executed immediately because the queue already has a value.
        queue.executeWhenAvailable {
            executions.add(1)
            // This setter should not trigger re-execution of the action queue.
            queue.value = StringBuilder("new value")
            executions.add(2)
        }

        assertThat(queue.value.toString()).isEqualTo("new value")
        assertThat(executions).containsExactly(1, 2).inOrder()
    }
}
