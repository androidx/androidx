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

package androidx.compose.ui.benchmark.focus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.assertNoPendingChanges
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.toggleStateBenchmarkRecompose
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.testutils.recomposeAssertHadChanges
import androidx.compose.ui.Modifier
import androidx.compose.ui.benchmark.repeatModifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusEvent
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class ParameterizedFocusBenchmark(val count: Int) {

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun parameters() = listOf(1, 10, 100)
    }

    @get:Rule val composeBenchmarkRule = ComposeBenchmarkRule()

    @Test
    fun modifyActiveHierarchy_addRemoveSubtree() {
        composeBenchmarkRule.toggleAlternatingStateBenchmarkRecompose({
            object : LayeredComposeTestCase(), ToggleableAlternatingTestCase {

                private val focusRequester = FocusRequester()
                private var shouldAddNodes by mutableStateOf(false)
                private var rootFocusState: FocusState? = null

                @Composable
                override fun MeasuredContent() {
                    if (shouldAddNodes) {
                        Box(focusTargetModifiers()) {
                            repeat(count) {
                                Box(Modifier.focusTarget()) {
                                    repeat(count) { Box(Modifier.focusTarget()) }
                                }
                            }
                        }
                    }
                }

                @Composable
                override fun ContentWrappers(content: @Composable () -> Unit) {
                    Box(
                        Modifier.fillMaxSize()
                            .onFocusEvent { rootFocusState = it }
                            .then(focusTargetModifiers())
                    ) {
                        Column {
                            Box(Modifier.focusRequester(focusRequester).focusTarget())
                            LaunchedEffect(Unit) { focusRequester.requestFocus() }
                            content()
                        }
                    }
                }

                override fun toggleState(isStateChangeMeasured: Boolean) {
                    assertThat(rootFocusState?.hasFocus).isTrue()
                    shouldAddNodes = isStateChangeMeasured
                }
            }
        })
    }

    @Test
    fun modifyActiveHierarchy_addRemoveModifiersWithExistingSubtree() {
        composeBenchmarkRule.toggleAlternatingStateBenchmarkRecompose({
            object : LayeredComposeTestCase(), ToggleableAlternatingTestCase {

                private val focusRequester = FocusRequester()
                private var shouldAddNodes by mutableStateOf(false)
                private var rootFocusState: FocusState? = null

                @Composable
                override fun MeasuredContent() {
                    Box(Modifier.thenIf(shouldAddNodes) { focusTargetModifiers() }) {
                        repeat(count) {
                            Box(Modifier.focusTarget()) {
                                repeat(count) { Box(Modifier.focusTarget()) }
                            }
                        }
                    }
                }

                @Composable
                override fun ContentWrappers(content: @Composable () -> Unit) {
                    Box(
                        Modifier.fillMaxSize()
                            .onFocusEvent { rootFocusState = it }
                            .then(focusTargetModifiers())
                    ) {
                        Column {
                            Box(Modifier.focusRequester(focusRequester).focusTarget())
                            LaunchedEffect(Unit) { focusRequester.requestFocus() }
                            content()
                        }
                    }
                }

                override fun toggleState(isStateChangeMeasured: Boolean) {
                    assertThat(rootFocusState?.hasFocus).isTrue()
                    shouldAddNodes = isStateChangeMeasured
                }
            }
        })
    }

    @Test
    fun modifyActiveHierarchy_addRemoveModifiersWithExistingActiveSubtree() {
        composeBenchmarkRule.toggleAlternatingStateBenchmarkRecompose({
            object : LayeredComposeTestCase(), ToggleableAlternatingTestCase {

                private val focusRequester = FocusRequester()
                private var shouldAddNodes by mutableStateOf(false)

                @Composable
                override fun MeasuredContent() {
                    Box(Modifier.thenIf(shouldAddNodes) { focusTargetModifiers() }) {
                        for (i in 0 until count) {
                            Box(Modifier.focusTarget()) {
                                for (j in 0 until count) {
                                    if (i == count - 1 && j == count - 1) {
                                        // Focus on the last child in a depth first traversal
                                        Box(Modifier.focusRequester(focusRequester).focusTarget())
                                    } else {
                                        Box(Modifier.focusTarget())
                                    }
                                }
                            }
                        }
                    }
                }

                @Composable
                override fun ContentWrappers(content: @Composable () -> Unit) {
                    Box(Modifier.fillMaxSize().then(focusTargetModifiers())) {
                        content()
                        LaunchedEffect(Unit) { focusRequester.requestFocus() }
                    }
                }

                override fun toggleState(isStateChangeMeasured: Boolean) {
                    shouldAddNodes = isStateChangeMeasured
                }
            }
        })
    }

    @Test
    fun reuseInactiveFocusTarget() {
        composeBenchmarkRule.toggleStateBenchmarkRecompose({
            object : LayeredComposeTestCase(), ToggleableTestCase {

                private var reuseKey by mutableStateOf(0)

                @Composable
                override fun MeasuredContent() {
                    ReusableContent(reuseKey) { Box(focusTargetModifiers()) }
                }

                @Composable
                override fun ContentWrappers(content: @Composable () -> Unit) {
                    Box(Modifier.fillMaxSize()) { content() }
                }

                override fun toggleState() {
                    reuseKey++
                }
            }
        })
    }

    @Test
    fun reuseInactiveFocusTarget_insideActiveParent() {
        composeBenchmarkRule.toggleStateBenchmarkRecompose({
            object : LayeredComposeTestCase(), ToggleableTestCase {

                private val focusRequester = FocusRequester()
                private var reuseKey by mutableStateOf(0)
                private var rootFocusState: FocusState? = null

                @Composable
                override fun MeasuredContent() {
                    ReusableContent(reuseKey) { Box(focusTargetModifiers()) }
                }

                @Composable
                override fun ContentWrappers(content: @Composable () -> Unit) {
                    Column(
                        Modifier.fillMaxSize().onFocusEvent { rootFocusState = it }.focusTarget()
                    ) {
                        Box(Modifier.focusRequester(focusRequester).focusTarget())
                        LaunchedEffect(Unit) { focusRequester.requestFocus() }
                        content()
                    }
                }

                override fun toggleState() {
                    assertThat(rootFocusState?.hasFocus).isTrue()
                    reuseKey++
                }
            }
        })
    }

    @Test
    fun moveInactiveFocusTarget() {
        composeBenchmarkRule.toggleStateBenchmarkRecompose({
            object : LayeredComposeTestCase(), ToggleableTestCase {

                private var moveContent by mutableStateOf(false)
                private val content = movableContentOf { Box(focusTargetModifiers()) }

                @Composable
                override fun MeasuredContent() {
                    if (moveContent) {
                        Box { content() }
                    } else {
                        Box { content() }
                    }
                }

                @Composable
                override fun ContentWrappers(content: @Composable () -> Unit) {
                    Box(Modifier.fillMaxSize()) { content() }
                }

                override fun toggleState() {
                    moveContent = !moveContent
                }
            }
        })
    }

    @Test
    fun moveInactiveFocusTarget_insideActiveParent() {
        composeBenchmarkRule.toggleStateBenchmarkRecompose({
            object : LayeredComposeTestCase(), ToggleableTestCase {

                private val focusRequester = FocusRequester()
                private var moveContent by mutableStateOf(false)
                private val movableContent = movableContentOf { Box(focusTargetModifiers()) }
                private var rootFocusState: FocusState? = null

                @Composable
                override fun MeasuredContent() {
                    if (moveContent) {
                        Box { movableContent() }
                    } else {
                        Box { movableContent() }
                    }
                }

                @Composable
                override fun ContentWrappers(content: @Composable () -> Unit) {
                    Column(
                        Modifier.fillMaxSize().onFocusEvent { rootFocusState = it }.focusTarget()
                    ) {
                        Box(Modifier.focusRequester(focusRequester).focusTarget())
                        LaunchedEffect(Unit) { focusRequester.requestFocus() }
                        content()
                    }
                }

                override fun toggleState() {
                    assertThat(rootFocusState?.hasFocus).isTrue()
                    moveContent = !moveContent
                }
            }
        })
    }

    private fun focusTargetModifiers() = repeatModifier(count, Modifier::focusTarget)

    private inline fun Modifier.thenIf(condition: Boolean, block: () -> Modifier): Modifier {
        return if (condition) then(block()) else this
    }

    /**
     * Measures the recomposition time of the hierarchy after changing a state and then changes the
     * state again to return to the initial composition excluding that from measurement.
     *
     * This is useful for benchmarks that toggle state between 2 values, i.e., an initial and target
     * values, and only the change to the target value is intended to be measured. For example, with
     * a boolean state, nodes are added when the state changes from false to true and removed when
     * the state changes back to false. In this example scenario, only the node addition is
     * measured.
     *
     * @param assertOneRecomposition whether the benchmark will fail if there are pending
     *   recompositions after the first recomposition. By default this is true to enforce
     *   correctness in the benchmark, but for components that have animations after being
     *   recomposed this can be turned off to benchmark just the first recomposition without any
     *   pending animations.
     * @param requireRecomposition whether the benchmark will fail if no changes were produce from a
     *   recomposition.there are pending recompositions. By default this is true to enforce
     *   correctness.
     */
    private fun <T> ComposeBenchmarkRule.toggleAlternatingStateBenchmarkRecompose(
        caseFactory: () -> T,
        assertOneRecomposition: Boolean = true,
        requireRecomposition: Boolean = true,
    ) where T : ComposeTestCase, T : ToggleableAlternatingTestCase {

        runBenchmarkFor(caseFactory) {
            fun recomposeWithAssertions() {
                if (requireRecomposition) {
                    recomposeAssertHadChanges()
                } else {
                    recompose()
                }
                if (assertOneRecomposition) {
                    assertNoPendingChanges()
                }
            }

            runOnUiThread { doFramesUntilNoChangesPending() }
            measureRepeatedOnUiThread {
                runWithTimingDisabled { getTestCase().toggleState(true) }
                recomposeWithAssertions()
                runWithTimingDisabled {
                    getTestCase().toggleState(false)
                    recomposeWithAssertions()
                }
            }
        }
    }

    /**
     * Test case that triggers a state change with alternating enabling/disabling measurement of the
     * effect of the state change.
     *
     * This is similar to [ToggleableTestCase] with the difference that this allows measuring only
     * one direction of state change. For example, this can be used to only measure checking the
     * checkbox and skipping measurement of unchecking it. This is run multiple times during a
     * benchmark run.
     */
    private interface ToggleableAlternatingTestCase {
        fun toggleState(isStateChangeMeasured: Boolean)
    }
}
