/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.runtime.benchmark.state

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.test.filters.LargeTest
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class ComposeStateReadBenchmark(private val readContext: ReadContext) {
    enum class ReadContext {
        Composition,
        Measure;
    }

    companion object {
        private const val MEASURE_OBSERVATION_DEPTH = 5
        private val OnCommitInvalidatingMeasure: (Any) -> Unit = {}

        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = arrayOf(ReadContext.Composition, ReadContext.Measure)
    }

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun readState() {
        val state = mutableIntStateOf(0)

        benchmarkRead {
            state.value
        }
    }

    @Test
    fun readDerivedState() {
        val stateA = mutableIntStateOf(0)
        val stateB = mutableIntStateOf(0)
        val derivedState = derivedStateOf { stateA.value + stateB.value }

        derivedState.value // precompute result

        benchmarkRead {
            derivedState.value
        }
    }

    @Test
    fun readDerivedState_secondRead() {
        val stateA = mutableIntStateOf(0)
        val stateB = mutableIntStateOf(0)
        val derivedState = derivedStateOf { stateA.value + stateB.value }

        derivedState.value // precompute result

        benchmarkRead(before = { derivedState.value }) {
            derivedState.value
        }
    }

    @Test
    fun readDerivedState_afterWrite() {
        val stateA = mutableIntStateOf(0)
        val stateB = mutableIntStateOf(0)
        val derivedState = derivedStateOf { stateA.value + stateB.value }

        derivedState.value // precompute result

        benchmarkRead(before = { stateA.value += 1 }) {
            derivedState.value
        }
    }

    @Test
    fun readState_afterWrite() {
        val stateA = mutableIntStateOf(0)

        benchmarkRead(before = { stateA.value += 1 }) {
            stateA.value
        }
    }

    @Test
    fun readState_preinitialized() {
        val stateA = mutableIntStateOf(0)
        val stateB = mutableIntStateOf(0)

        benchmarkRead(before = { stateA.value }) {
            stateB.value
        }
    }

    @Test
    fun readDerivedState_preinitialized() {
        val stateA = mutableIntStateOf(0)
        val stateB = mutableIntStateOf(0)

        val derivedStateA = derivedStateOf { stateA.value + stateB.value }
        val derivedStateB = derivedStateOf { stateB.value + stateA.value }

        benchmarkRead(before = { derivedStateA.value }) {
            derivedStateB.value
        }
    }

    private fun benchmarkRead(
        before: () -> Unit = {},
        after: () -> Unit = {},
        measure: () -> Unit
    ) {
        val benchmarkState = benchmarkRule.getState()
        benchmarkRule.measureRepeated {
            benchmarkState.pauseTiming()
            runInReadObservationScope {
                before()
                benchmarkState.resumeTiming()

                measure()

                benchmarkState.pauseTiming()
                after()
            }
            benchmarkRule.getState().resumeTiming()
        }
    }

    private fun runInReadObservationScope(scopeBlock: () -> Unit) {
        when (readContext) {
            ReadContext.Composition -> createComposition().setContent { scopeBlock() }
            ReadContext.Measure -> {
                SnapshotStateObserver { it() }.apply {
                    val nodes = List(MEASURE_OBSERVATION_DEPTH) { Any() }
                    start()
                    recursiveObserve(nodes, nodes.size, scopeBlock)
                    stop()
                }
            }
        }
    }

    private fun SnapshotStateObserver.recursiveObserve(
        nodes: List<Any>,
        depth: Int,
        block: () -> Unit
    ) {
        if (depth == 0) {
            block()
            return
        }
        observeReads(nodes[depth - 1], OnCommitInvalidatingMeasure) {
            recursiveObserve(nodes, depth - 1, block)
        }
    }

    private fun createComposition(
        coroutineContext: CoroutineContext = EmptyCoroutineContext
    ): Composition {
        val applier = UnitApplier()
        val recomposer = Recomposer(coroutineContext)
        return Composition(applier, recomposer)
    }

    private class UnitApplier : Applier<Unit> {
        override val current: Unit = Unit
        override fun clear() {}
        override fun move(from: Int, to: Int, count: Int) {}
        override fun remove(index: Int, count: Int) {}
        override fun up() {}
        override fun insertTopDown(index: Int, instance: Unit) {}
        override fun insertBottomUp(index: Int, instance: Unit) {}
        override fun down(node: Unit) {}
    }
}
