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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

class CollectAsStateTests {

    @Test
    fun stateFlow_collectAsState() = compositionTest {
        val stateFlow = MutableStateFlow("initial")
        compose {
            val state by stateFlow.collectAsState()
            Text(state)
        }

        validate { Text("initial") }

        stateFlow.value = "updated"
        advanceTimeBy(100)
        expectChanges()

        validate { Text("updated") }
    }

    @Test
    fun flow_collectAsState() = compositionTest {
        val flow = flow { emit("updated") }
        compose {
            val state by flow.collectAsState("initial")
            Text(state)
        }

        validate { Text("initial") }

        advanceTimeBy(100)
        expectChanges()

        validate { Text("updated") }
    }

    @Test
    fun flow_collectAsState_restartsOnFlowChange() = compositionTest {
        var flow1Emitted = false
        val flow1 = flow {
            flow1Emitted = true
            emit("flow 1")
        }
        var flow2Emitted = false
        val flow2 = flow {
            flow2Emitted = true
            emit("flow 2")
        }

        var currentFlow by mutableStateOf(flow1)

        compose {
            val state by currentFlow.collectAsState("initial")
            Text(state)
        }

        advanceTimeBy(100)
        expectChanges()
        validate { Text("flow 1") }
        assertEquals(true, flow1Emitted)

        currentFlow = flow2
        expectChanges()

        advanceTimeBy(100)
        expectChanges()
        validate { Text("flow 2") }
        assertEquals(true, flow2Emitted)
    }

    @Test
    fun collectAsState_mutationPolicy_referential() = compositionTest {
        val people = listOf("Alice", "Bob")
        val people2 = listOf("Alice", "Bob")
        val stateFlow = MutableStateFlow(people)
        var recompositions = 0
        compose {
            recompositions++
            val state by stateFlow.collectAsState(mutationPolicy = referentialEqualityPolicy())
            Text("$state@${state.hashCode().toString(16)}")
        }

        validate { Text("${stateFlow.value}@${stateFlow.value.hashCode().toString(16)}") }

        stateFlow.value = people2
        advance()
        revalidate()
    }
}
