/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core

import androidx.compose.Composable
import androidx.compose.State
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.test.filters.LargeTest
import androidx.ui.benchmark.ComposeBenchmarkRule
import androidx.ui.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.ui.benchmark.toggleStateBenchmarkMeasureLayout
import androidx.ui.core.Placeable.PlacementScope.place
import androidx.ui.layout.Container
import androidx.ui.layout.Spacer
import androidx.ui.test.ComposeTestCase
import androidx.ui.test.ToggleableTestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class WithConstraintsBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun no_withconstraints_inner_recompose() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout { NoWithConstraintsTestCase() }
    }

    @Test
    fun withconstraints_inner_recompose() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout { WithConstraintsTestCase() }
    }

    @Test
    fun withconstraints_changing_constraints() {
        benchmarkRule.toggleStateBenchmarkMeasureLayout { ChangingConstraintsTestCase() }
    }
}

private class NoWithConstraintsTestCase : ComposeTestCase, ToggleableTestCase {

    private lateinit var state: State<Dp>

    @Composable
    override fun emitContent() {
        val size = +state { 200.dp }
        this.state = size
        Container(width = 300.dp, height = 300.dp) {
            Spacer(androidx.ui.layout.Size(width = size.value, height = size.value))
        }
    }

    override fun toggleState() {
        state.value = if (state.value == 200.dp) 150.dp else 200.dp
    }
}

private class WithConstraintsTestCase : ComposeTestCase, ToggleableTestCase {

    private lateinit var state: State<Dp>

    @Composable
    override fun emitContent() {
        val size = +state { 200.dp }
        this.state = size
        WithConstraints {
            Container(width = 300.dp, height = 300.dp) {
                Spacer(androidx.ui.layout.Size(width = size.value, height = size.value))
            }
        }
    }

    override fun toggleState() {
        state.value = if (state.value == 200.dp) 150.dp else 200.dp
    }
}

private class ChangingConstraintsTestCase : ComposeTestCase, ToggleableTestCase {

    private lateinit var state: State<IntPx>

    @Composable
    override fun emitContent() {
        val size = +state { 100.ipx }
        this.state = size
        ChangingConstraintsLayout(state) {
            WithConstraints {
                Container(expanded = true) {}
            }
        }
    }

    override fun toggleState() {
        state.value = if (state.value == 100.ipx) 50.ipx else 100.ipx
    }
}

@Composable
private fun ChangingConstraintsLayout(size: State<IntPx>, children: @Composable() () -> Unit) {
    Layout(children) { measurables, _ ->
        val constraints = Constraints.tightConstraints(size.value, size.value)
        measurables.first().measure(constraints).place(0.ipx, 0.ipx)
        layout(100.ipx, 100.ipx) {}
    }
}
