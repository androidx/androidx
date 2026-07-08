/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.runtime

import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.expectChanges
import androidx.compose.runtime.mock.revalidate
import androidx.compose.runtime.mock.validate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

class ProduceStateTests {

    @Test
    fun produceState_initialValue() = compositionTest {
        compose {
            @Suppress("ProduceStateDoesNotAssignValue") val state by produceState("initial") {}
            Text(state)
        }

        validate { Text("initial") }
    }

    @Test
    fun produceState_updates() = compositionTest {
        compose {
            val state by
                produceState("initial") {
                    delay(99.milliseconds)
                    value = "updated"
                }
            Text(state)
        }

        validate { Text("initial") }

        advanceTimeBy(100)
        expectChanges()

        validate { Text("updated") }
    }

    @Test
    fun produceState_restartsOnKeyChange() = compositionTest {
        var key by mutableIntStateOf(0)
        var producerStarts = 0
        compose {
            val state by
                produceState("initial", key) {
                    producerStarts++
                    value = "key $key"
                }
            Text(state)
        }

        advanceTimeBy(100)
        expectChanges()
        validate { Text("key 0") }
        assertEquals(1, producerStarts)

        key++
        expectChanges()

        advanceTimeBy(100)
        expectChanges()
        validate { Text("key 1") }
        assertEquals(2, producerStarts)
    }

    @Test
    fun produceState_cancelledOnDisposal() = compositionTest {
        var show by mutableStateOf(true)
        var cancelled = true
        compose {
            if (show) {
                @Suppress("ProduceStateDoesNotAssignValue")
                produceState("initial") {
                    try {
                        awaitDispose {}
                    } finally {
                        cancelled = true
                    }
                }
            }
        }

        show = false
        expectChanges()

        assertTrue(cancelled)
    }

    @Test
    fun produceState_awaitDispose() = compositionTest {
        var show by mutableStateOf(true)
        var disposed = false
        compose {
            if (show) {
                @Suppress("ProduceStateDoesNotAssignValue")
                produceState("initial") { awaitDispose { disposed = true } }
            }
        }

        show = false
        expectChanges()

        assertTrue(disposed)
    }

    @Test
    fun produceState_mutationPolicy_referential() = compositionTest {
        val people = listOf("Alice", "Bob")
        val people2 = listOf("Alice", "Bob")
        compose {
            val state by
                produceState(people, referentialEqualityPolicy()) {
                    delay(100.milliseconds)
                    value = people2
                }
            Text("$state@${state.hashCode().toString(16)}")
        }

        var expected = people
        validate { Text("$expected@${expected.hashCode().toString(16)}") }

        advanceTimeBy(100)
        expected = people2
        revalidate()
    }
}
