/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.runtime.benchmark

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.filters.LargeTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Assert.assertEquals
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.junit.runners.Parameterized

@LargeTest
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class CollectAsStateBenchmark(private val count: Int) : ComposeBenchmarkBase() {

    @Test
    fun collectAsState_add() = runBlockingTestWithFrameClock {
        val flow = flow<Int> { waitForever() }
        var flowCount by mutableStateOf(0)
        measureRecompose {
            var seen = 0
            compose { repeat(flowCount) { seen += flow.collectAsState(1).value } }
            update { flowCount = count }
            reset {
                flowCount = 0
                assertEquals("Didn't see the right number of flows", seen, count)
                seen = 0
            }
        }
    }

    @Test
    fun collectAsState_remove() = runBlockingTestWithFrameClock {
        val flow = flow<Int> { waitForever() }
        var flowCount by mutableStateOf(count)
        measureRecompose {
            var seen = 0
            compose { repeat(flowCount) { seen += flow.collectAsState(1).value } }
            update { flowCount = 0 }
            reset {
                flowCount = count
                assertEquals("Didn't see the right number of flows", seen, count)
                seen = 0
            }
        }
    }

    @Test
    fun collectAsState_stateFlow_add() = runBlockingTestWithFrameClock {
        val flow = MutableStateFlow(1)
        var flowCount by mutableStateOf(0)
        measureRecompose {
            var seen = 0
            compose { repeat(flowCount) { seen += flow.collectAsState().value } }
            update { flowCount = count }
            reset {
                flowCount = 0
                assertEquals("Didn't see the right number of flows", seen, count)
                seen = 0
            }
        }
    }

    @Test
    fun collectAsState_stateFlow_remove() = runBlockingTestWithFrameClock {
        val flow = MutableStateFlow(1)
        var flowCount by mutableStateOf(count)
        measureRecompose {
            var seen = 0
            compose { repeat(flowCount) { seen += flow.collectAsState().value } }
            update { flowCount = 0 }
            reset {
                flowCount = count
                assertEquals("Didn't see the right number of flows", seen, count)
                seen = 0
            }
        }
    }

    @Test
    fun collectAsStateWithLifecycle_add() = runBlockingTestWithFrameClock {
        val flow = flow<Int> { waitForever() }
        var flowCount by mutableStateOf(0)
        measureRecompose {
            var seen = 0
            compose { repeat(flowCount) { seen += flow.collectAsStateWithLifecycle(1).value } }
            update { flowCount = count }
            reset {
                flowCount = 0
                assertEquals("Didn't see the right number of flows", seen, count)
                seen = 0
            }
        }
    }

    @Test
    fun collectAsStateWithLifecycle_remove() = runBlockingTestWithFrameClock {
        val flow = flow<Int> { waitForever() }
        var flowCount by mutableStateOf(count)
        measureRecompose {
            var seen = 0
            compose { repeat(flowCount) { seen += flow.collectAsStateWithLifecycle(1).value } }
            update { flowCount = 0 }
            reset {
                flowCount = count
                assertEquals("Didn't see the right number of flows", seen, count)
                seen = 0
            }
        }
    }

    @Test
    fun collectAsStateWithLifecycle_stateFlow_add() = runBlockingTestWithFrameClock {
        val flow = MutableStateFlow(1)
        var flowCount by mutableStateOf(0)
        measureRecompose {
            var seen = 0
            compose { repeat(flowCount) { seen += flow.collectAsStateWithLifecycle().value } }
            update { flowCount = count }
            reset {
                flowCount = 0
                assertEquals("Didn't see the right number of flows", seen, count)
                seen = 0
            }
        }
    }

    @Test
    fun collectAsStateWithLifecycle_stateFlow_remove() = runBlockingTestWithFrameClock {
        val flow = MutableStateFlow(1)
        var flowCount by mutableStateOf(count)
        measureRecompose {
            var seen = 0
            compose { repeat(flowCount) { seen += flow.collectAsStateWithLifecycle().value } }
            update { flowCount = 0 }
            reset {
                flowCount = count
                assertEquals("Didn't see the right number of flows", seen, count)
                seen = 0
            }
        }
    }

    companion object {
        @Parameterized.Parameters(name = "count={0}")
        @JvmStatic
        fun parameters() = listOf<Array<Any?>>(arrayOf(1), arrayOf(10), arrayOf(100))
    }
}

private suspend fun waitForever() = suspendCancellableCoroutine<Unit> {}
