/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.appstate.transform.benchmark

import androidx.appstate.transform.RunnableTransformScope
import androidx.appstate.transform.transform
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Microbenchmark suite for state transformation measuring key performance characteristics:
 * 1. Instantiation / Setup overhead.
 * 2. Flat N-way state fan-in combination and recomposition propagation.
 * 3. Dynamic branch switching (evaluating changed read-sets without tearing down pipelines).
 * 4. Transactional atomicity and diamond DAG propagation ($A \to B, C \to D$).
 * 5. Memoization during high-frequency mutations.
 * 6. Burst mutation conflation throughput.
 * 7. Shared [RunnableTransformScope] setup overhead, chained propagation, and selective fan-out.
 * 8. Nested `@Composable` [transform] setup overhead and chained propagation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class TransformBenchmark {

    @get:Rule val benchmarkRule = BenchmarkRule()

    /**
     * ─── 1. INSTANTIATION / SETUP OVERHEAD
     * ────────────────────────────────────────────────────────
     *
     * WHY THIS IS IMPORTANT: Evaluates the baseline cold-start cost of instantiating a new reactive
     * transformation pipeline (e.g. creating state derivation during ViewModel initialization or
     * navigation destination entry).
     *
     * GENERAL SUBSYSTEMS & AREAS INVOLVED:
     * - Headless Composition Lifecycle: Setting up the composition context, frame clock,
     *   recomposer, and launching the background recomposition loop.
     * - Global Write Monitoring: Initializing observation channels and apply dispatch listeners.
     * - Initial Graph Evaluation: Allocating slot storage and evaluating the initial state values.
     *
     * WHAT CHANGES WOULD IMPACT RESULTS:
     * - Sharing or scoping recomposition infrastructure across multiple transformation instances
     *   instead of creating dedicated execution contexts per call.
     * - Reducing coroutine launch and context composition overhead during setup.
     * - Optimizing frame clock setup and initialization paths.
     */
    @Test
    fun benchmarkTransformSetupOverhead() {
        val numInputs = 200
        benchmarkRule.measureRepeated {
            runTest {
                val inputs = runWithMeasurementDisabled { List(numInputs) { mutableIntStateOf(0) } }
                val state =
                    transform(0, backgroundScope, dispatcher = testDispatcher) {
                        inputs.sumOf { it.intValue }
                    }
                BlackHole.consume(state.value)
            }
        }
    }

    /**
     * ─── 1b. BASELINE UNIT INSTANTIATION / SETUP OVERHEAD ────────────────────────────────────────
     *
     * WHY THIS IS IMPORTANT: Evaluates the pure framework floor of instantiating a single reactive
     * transformation pipeline that performs no calculations or state reads (`{ Unit }`), isolating
     * headless composition, recomposer, and frame clock setup from state subscription work.
     */
    @Test
    fun benchmarkTransformUnitSetupOverhead() {
        benchmarkRule.measureRepeated {
            runTest {
                val state = transform(Unit, backgroundScope, dispatcher = testDispatcher) { Unit }
                BlackHole.consume(state.value)
            }
        }
    }

    /**
     * ─── 2. FLAT N-WAY FAN-IN STATE COMBINATION ──────────────────────────────────────────────────
     *
     * WHY THIS IS IMPORTANT: Simulates combining multiple independent upstream state sources (e.g.
     * 10 repository flags, filter selections, or user settings) into a single composite state.
     * Measures steady-state dependency tracking and change propagation latency.
     *
     * GENERAL SUBSYSTEMS & AREAS INVOLVED:
     * - Snapshot Tracking: Recording writes across multiple states into a change set.
     * - Apply Notification Dispatch: Propagating change notifications to active observers.
     * - Frame Clock Scheduling: Coordinating frame delivery to wake the evaluation loop.
     * - Scope Invalidation: Detecting invalidated dependency scopes and executing the transform
     *   block.
     *
     * WHAT CHANGES WOULD IMPACT RESULTS:
     * - Reducing scheduling latency between state write commits and evaluation execution.
     * - Optimizing frame dispatch buffering and inter-thread event delivery.
     * - Internal improvements in snapshot dependency tracking and scope invalidation.
     */
    @Test
    fun benchmarkFlatCombination() = runTest {
        val numInputs = 10
        val inputs = List(numInputs) { mutableIntStateOf(0) }
        val outputState =
            transform(0, backgroundScope, dispatcher = testDispatcher) {
                inputs.sumOf { it.intValue }
            }

        // Correctness verification
        inputs[0].intValue = 10
        runRecomposition()
        assertEquals(10, outputState.value)
        inputs[0].intValue = 0
        runRecomposition()
        assertEquals(0, outputState.value)

        var counter = 0
        benchmarkRule.measureRepeated {
            for (i in 0 until numInputs) {
                inputs[i].intValue = counter + i
            }
            runRecomposition()
            BlackHole.consume(outputState.value)
            counter++
        }
    }

    /**
     * ─── 3. DYNAMIC BRANCH SWITCHING ─────────────────────────────────────────────────────────────
     *
     * WHY THIS IS IMPORTANT: Tests how efficiently transformation handles conditional branch
     * switches (e.g. switching between authenticated and unauthenticated UI states, active tabs, or
     * feature flags). Unlike stream-based pipelines that require cancelling and rebuilding
     * asynchronous subscriptions, reactive composition updates its active read dependencies
     * dynamically in a single pass.
     *
     * GENERAL SUBSYSTEMS & AREAS INVOLVED:
     * - Dynamic Read Tracking: Unsubscribing from previously read state instances and subscribing
     *   to newly active branch dependencies without resetting the execution environment.
     * - Conditional Scope Invalidation: Scheduling single-pass evaluation when branch criteria
     *   toggle.
     *
     * WHAT CHANGES WOULD IMPACT RESULTS:
     * - Optimizations to conditional dependency tracking and dynamic read-set updates.
     * - Efficiency of conditional branch execution in the underlying runtime.
     */
    @Test
    fun benchmarkDynamicBranching() = runTest {
        val numInputs = 10
        val inputs = List(numInputs * 2) { mutableIntStateOf(0) }
        val inputsA = inputs.take(numInputs)
        val inputsB = inputs.drop(numInputs)
        val condition = mutableStateOf(true)

        val outputState =
            transform(0, backgroundScope, dispatcher = testDispatcher) {
                if (condition.value) {
                    inputsA.sumOf { it.intValue }
                } else {
                    inputsB.sumOf { it.intValue }
                }
            }

        inputsA[0].intValue = 5
        inputsB[0].intValue = 10
        condition.value = true
        runRecomposition()
        assertEquals(5, outputState.value)

        var conditionState = true
        benchmarkRule.measureRepeated {
            conditionState = !conditionState
            condition.value = conditionState
            runRecomposition()
            BlackHole.consume(outputState.value)
        }
    }

    /**
     * ─── 4. DIAMOND DEPENDENCY PROPAGATION & GLITCH FREEDOM
     * ───────────────────────────────────────
     *
     * WHY THIS IS IMPORTANT: Evaluates the classic reactive "Diamond Dependency" problem:
     *
     *         Root State (A)
     *          /          \
     *         v            v
     *   Derived Left (B)  Derived Right (C)
     *          \          /
     *           v        v
     *         Target State (D)
     *
     * In asynchronous stream architectures, independent intermediate branches can emit separately,
     * causing temporary intermediate "glitches" (where D sees new B with old C). In transactional
     * state models, atomic commits ensure node D evaluates both branches coherently in a single
     * step with 0 glitches.
     *
     * GENERAL SUBSYSTEMS & AREAS INVOLVED:
     * - Transactional Isolation: Grouping upstream mutations into atomic commits.
     * - Intermediate State Derivation: Evaluating intermediate sub-computations within the same
     *   pass.
     * - Consistency Guarantees: Verifying that all downstream reads observe consistent sequence
     *   versions.
     *
     * WHAT CHANGES WOULD IMPACT RESULTS:
     * - Modifications to transaction commit boundaries or notification scheduling.
     * - Any regressions in atomic evaluation ordering that could introduce transient intermediate
     *   states.
     */
    @Test
    fun benchmarkDiamondGraphPropagation() = runTest {
        data class UserSession(val role: String, val isPremium: Boolean, val seq: Long)
        data class Permissions(val canAdmin: Boolean, val seq: Long)
        data class Badge(val title: String, val seq: Long)
        data class DashboardState(val perms: Permissions, val badge: Badge, val isGlitch: Boolean)

        val sessionState = mutableStateOf(UserSession(role = "USER", isPremium = false, seq = 0L))

        var glitchCount = 0
        val dashboardState =
            transform(
                DashboardState(Permissions(false, 0L), Badge("Basic", 0L), false),
                backgroundScope,
                dispatcher = testDispatcher,
            ) {
                val session = sessionState.value
                val perms = remember(session) { Permissions(session.role == "ADMIN", session.seq) }
                val badge =
                    remember(session) {
                        Badge(if (session.isPremium) "VIP" else "Basic", session.seq)
                    }
                val isGlitch = perms.seq != badge.seq
                if (isGlitch) {
                    glitchCount++
                }
                DashboardState(perms, badge, isGlitch)
            }

        var counter = 1000L
        benchmarkRule.measureRepeated {
            val seq = counter
            val isAdmin = seq % 2 == 0L
            sessionState.value =
                UserSession(role = if (isAdmin) "ADMIN" else "USER", isPremium = isAdmin, seq = seq)
            runRecomposition()
            BlackHole.consume(dashboardState.value)
            counter++
        }

        assertEquals(0, glitchCount)
    }

    /**
     * ─── 5. MEMOIZED SUBGRAPHS ───────────────────────────────────────────────────────────────────
     *
     * WHY THIS IS IMPORTANT: Simulates state models where high-frequency events (like clock ticks
     * or progress updates) trigger re-evaluation, while expensive sub-computations (such as sorting
     * or filtering lists) are memoized against specific keys.
     *
     * GENERAL SUBSYSTEMS & AREAS INVOLVED:
     * - Positional Memoization: Retaining previous calculation results based on key equality.
     * - Selective Re-evaluation: Executing lightweight outer transformations while skipping
     *   unchanged, expensive sub-calculations.
     *
     * WHAT CHANGES WOULD IMPACT RESULTS:
     * - Efficiency of key equality checking and memoized value lookup in state storage.
     * - Overhead of skipped blocks during re-evaluation passes.
     */
    @Test
    fun benchmarkMemoizedSubgraph() = runTest {
        data class Item(val id: Int, val isSpam: Boolean, val timestamp: Long)
        data class InboxUi(val items: List<Item>, val unreadCount: Int, val clockTick: Long)

        val itemsState = mutableStateOf((1..100).map { Item(it, it % 5 == 0, it.toLong()) })
        val isPremiumState = mutableStateOf(false)
        val clockTickState = mutableStateOf(0L)

        var heavyComputationCount = 0
        val inboxState: State<InboxUi> =
            transform(InboxUi(emptyList(), 0, 0L), backgroundScope, dispatcher = testDispatcher) {
                val items = itemsState.value
                val isPremium = isPremiumState.value
                val tick = clockTickState.value

                // Expensive filter/sort is remembered and skipped on tick updates
                val sorted =
                    remember(items, isPremium) {
                        heavyComputationCount++
                        val filtered = if (isPremium) items else items.filter { !it.isSpam }
                        filtered.sortedByDescending { it.timestamp }
                    }

                InboxUi(sorted, sorted.size, tick)
            }

        var tick = 0L
        benchmarkRule.measureRepeated {
            tick++
            clockTickState.value = tick
            runRecomposition()
            BlackHole.consume(inboxState.value)
        }

        // Heavy computation must not run on tick updates
        assertEquals(1, heavyComputationCount)
    }

    /**
     * ─── 6. BURST WRITE CONFLATION THROUGHPUT ────────────────────────────────────────────────────
     *
     * WHY THIS IS IMPORTANT: Simulates high-frequency bursts of state mutations (e.g. sensor
     * readings, touch gestures, or rapid socket events). Tests whether the reactive system
     * naturally conflates intermediate mutations into a single coherent evaluation pass without
     * queuing individual passes per mutation.
     *
     * GENERAL SUBSYSTEMS & AREAS INVOLVED:
     * - Conflation & Buffering: Merging multiple rapid state changes before the next evaluation
     *   cycle.
     * - Batch Processing: Applying and processing multiple writes in a single update pass.
     *
     * WHAT CHANGES WOULD IMPACT RESULTS:
     * - Changes to event buffering, batching windows, or conflation mechanisms.
     * - Scheduling policies when multiple writes arrive between evaluation frames.
     */
    @Test
    fun benchmarkBurstWriteThroughput() = runTest {
        val state = mutableIntStateOf(0)
        val output =
            transform(0, backgroundScope, dispatcher = testDispatcher) { state.intValue * 2 }

        var counter = 0
        benchmarkRule.measureRepeated {
            counter += 50
            for (i in 1..50) {
                state.intValue = (counter - 50) + i
            }
            runRecomposition()
            BlackHole.consume(output.value)
        }
    }

    /**
     * ─── 7. SHARED TRANSFORM SCOPE: SETUP OVERHEAD ───────────────────────────────────────────────
     *
     * WHY THIS IS IMPORTANT: Evaluates the cold-start cost of instantiating multiple
     * transformations that share a single [RunnableTransformScope] via
     * [RunnableTransformScope.launchTransform] and starting [RunnableTransformScope.run].
     *
     * GENERAL SUBSYSTEMS & AREAS INVOLVED:
     * - Initial State Evaluation: Computing initial values for each launched transformation.
     * - Shared Composition Lifecycle: Combining multiple transformations into a single headless
     *   composition and recomposer loop.
     */
    @Test
    fun benchmarkSharedTransformScopeSetupOverhead() {
        val numTransforms = 10
        val numInputsPerTransform = 20
        benchmarkRule.measureRepeated {
            runTest {
                val inputs = runWithMeasurementDisabled {
                    List(numTransforms) { List(numInputsPerTransform) { mutableIntStateOf(0) } }
                }
                val transformScope = RunnableTransformScope()
                val states = inputs.map { group ->
                    transformScope.launchTransform(0) { group.sumOf { it.intValue } }
                }
                backgroundScope.launch(testDispatcher, start = CoroutineStart.UNDISPATCHED) {
                    transformScope.run()
                }
                BlackHole.consume(states.sumOf { it.value })
            }
        }
    }

    /**
     * ─── 7b. SHARED TRANSFORM SCOPE: BASELINE UNIT SETUP OVERHEAD ────────────────────────────────
     *
     * WHY THIS IS IMPORTANT: Evaluates the pure framework floor of instantiating 10 transformations
     * that return [Unit] without reading any state and share a single [RunnableTransformScope] via
     * [RunnableTransformScope.launchTransform], isolating per-transformation composition overhead
     * from upstream state read subscriptions.
     */
    @Test
    fun benchmarkSharedTransformScopeUnitSetupOverhead() {
        val numTransforms = 10
        benchmarkRule.measureRepeated {
            runTest {
                val transformScope = RunnableTransformScope()
                val states = List(numTransforms) { transformScope.launchTransform(Unit) { Unit } }
                backgroundScope.launch(testDispatcher, start = CoroutineStart.UNDISPATCHED) {
                    transformScope.run()
                }
                states.forEach { BlackHole.consume(it.value) }
            }
        }
    }

    /**
     * ─── 8. SHARED TRANSFORM SCOPE: CHAINED TRANSFORMS ───────────────────────────────────────────
     *
     * WHY THIS IS IMPORTANT: Evaluates a 10-stage sequential reactive pipeline where all 10
     * transformations are launched into a SINGLE shared [RunnableTransformScope] via
     * [RunnableTransformScope.launchTransform], sharing one headless composition, recomposer, and
     * frame clock.
     *
     * GENERAL SUBSYSTEMS & AREAS INVOLVED:
     * - Shared Composition Recomposition: Propagating state updates across keyed transformation
     *   groups within a single composition.
     * - Single Recomposer & Frame Clock: Avoiding multi-recomposer coroutine and clock overhead.
     */
    @Test
    fun benchmarkSharedTransformScopeChained() = runTest {
        val transformScope = RunnableTransformScope()
        val rootState = mutableIntStateOf(0)
        var current: State<Int> = rootState
        val chain = mutableListOf<State<Int>>()
        repeat(10) {
            val prev = current
            current = transformScope.launchTransform(0) { prev.value + 1 }
            chain.add(current)
        }
        val outputState = current
        backgroundScope.launch(testDispatcher, start = CoroutineStart.UNDISPATCHED) {
            transformScope.run()
        }

        // Correctness verification
        rootState.intValue = 10
        runRecomposition()
        assertEquals(20, outputState.value)
        rootState.intValue = 0
        runRecomposition()
        assertEquals(10, outputState.value)

        var counter = 0
        benchmarkRule.measureRepeated {
            rootState.intValue = ++counter
            runRecomposition()
            BlackHole.consume(outputState.value)
        }
    }

    /**
     * ─── 9. SHARED TRANSFORM SCOPE: SELECTIVE RECOMPOSITION (FAN-OUT) ────────────────────────────
     *
     * WHY THIS IS IMPORTANT: Evaluates 10 independent transformations sharing a single
     * [RunnableTransformScope] when only one transformation's upstream input state changes per
     * frame, verifying that the shared composition selectively recomposes only the invalidated
     * transformation while skipping the other 9.
     *
     * GENERAL SUBSYSTEMS & AREAS INVOLVED:
     * - Per-Transformation Recompose Scopes: Isolating invalidations to the affected transformation
     *   within the shared composition.
     * - Slot Table Skipping: Skipping unchanged sibling transformations in the shared composition.
     */
    @Test
    fun benchmarkSharedTransformScopeSelectiveRecomposition() = runTest {
        val numTransforms = 10
        val inputs = List(numTransforms) { mutableIntStateOf(0) }
        val transformScope = RunnableTransformScope()
        val outputs = inputs.map { input ->
            transformScope.launchTransform(0) { input.intValue + 1 }
        }
        backgroundScope.launch(testDispatcher, start = CoroutineStart.UNDISPATCHED) {
            transformScope.run()
        }

        inputs[0].intValue = 10
        runRecomposition()
        assertEquals(11, outputs[0].value)
        inputs[0].intValue = 0
        runRecomposition()
        assertEquals(1, outputs[0].value)

        var counter = 0
        benchmarkRule.measureRepeated {
            val index = counter % numTransforms
            inputs[index].intValue = ++counter
            runRecomposition()
            BlackHole.consume(outputs[index].value)
        }
    }

    /**
     * ─── 10. NESTED TRANSFORMS: SETUP OVERHEAD ───────────────────────────────────────────────────
     *
     * WHY THIS IS IMPORTANT: Evaluates the cold-start cost of an outer [transform] that
     * instantiates 10 nested `@Composable` [transform] calls inside its composition block.
     *
     * GENERAL SUBSYSTEMS & AREAS INVOLVED:
     * - Nested Headless Compositions: Creating and remembering child transformations tied to the
     *   parent composition's coroutine scope and frame clock.
     */
    @Test
    fun benchmarkNestedTransformsSetupOverhead() {
        val numNested = 10
        benchmarkRule.measureRepeated {
            runTest {
                val rootState = runWithMeasurementDisabled { mutableIntStateOf(1) }
                val dispatcher = testDispatcher
                val outputState =
                    transform(0, backgroundScope, dispatcher = dispatcher) {
                        var current: State<Int> = rootState
                        repeat(numNested) {
                            val prev = current
                            current = transform(0, dispatcher = dispatcher) { prev.value + 1 }
                        }
                        current.value
                    }
                runRecomposition()
                BlackHole.consume(outputState.value)
            }
        }
    }

    /**
     * ─── 11. NESTED TRANSFORMS: CHAINED PROPAGATION ──────────────────────────────────────────────
     *
     * WHY THIS IS IMPORTANT: Evaluates steady-state recomposition propagation through a 10-stage
     * chain of nested `@Composable` [transform] calls inside an outer [transform].
     *
     * GENERAL SUBSYSTEMS & AREAS INVOLVED:
     * - Hierarchical Frame Clock & Recomposer Coordination: Propagating frames and snapshot
     *   invalidations from the outer composition's broadcast frame clock through nested child
     *   transformations.
     */
    @Test
    fun benchmarkNestedChainedTransforms() = runTest {
        val rootState = mutableIntStateOf(0)
        val dispatcher = testDispatcher
        val outputState =
            transform(0, backgroundScope, dispatcher = dispatcher) {
                var current: State<Int> = rootState
                repeat(10) {
                    val prev = current
                    current = transform(0, dispatcher = dispatcher) { prev.value + 1 }
                }
                current.value
            }
        runRecomposition()

        // Correctness verification
        rootState.intValue = 10
        runRecomposition()
        assertEquals(20, outputState.value)
        rootState.intValue = 0
        runRecomposition()
        assertEquals(10, outputState.value)

        var counter = 0
        benchmarkRule.measureRepeated {
            rootState.intValue = ++counter
            runRecomposition()
            BlackHole.consume(outputState.value)
        }
    }
}
