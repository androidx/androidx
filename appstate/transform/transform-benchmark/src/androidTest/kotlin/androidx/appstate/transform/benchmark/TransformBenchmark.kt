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

import androidx.appstate.transform.transform
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
                    transform(0, backgroundScope, testDispatcher) { inputs.sumOf { it.intValue } }
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
            transform(0, backgroundScope, testDispatcher) { inputs.sumOf { it.intValue } }

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
            transform(0, backgroundScope, testDispatcher) {
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
                testDispatcher,
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
            transform(InboxUi(emptyList(), 0, 0L), backgroundScope, testDispatcher) {
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
        val output = transform(0, backgroundScope, testDispatcher) { state.intValue * 2 }

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
}
