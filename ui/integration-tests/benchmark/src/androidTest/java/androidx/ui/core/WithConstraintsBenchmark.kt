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
import androidx.compose.MutableState
import androidx.compose.State
import androidx.compose.state
import androidx.test.filters.LargeTest
import androidx.ui.benchmark.ComposeBenchmarkRule
import androidx.ui.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.ui.benchmark.toggleStateBenchmarkMeasureLayout
import androidx.ui.foundation.Box
import androidx.ui.foundation.ContentGravity
import androidx.ui.integration.test.ToggleableTestCase
import androidx.ui.layout.Spacer
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.preferredSize
import androidx.ui.test.ComposeTestCase
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
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

    private lateinit var state: MutableState<Dp>

    @Composable
    override fun emitContent() {
        val size = state { 200.dp }
        this.state = size
        Box(Modifier.preferredSize(300.dp), gravity = ContentGravity.Center) {
            Spacer(Modifier.preferredSize(width = size.value, height = size.value))
        }
    }

    override fun toggleState() {
        state.value = if (state.value == 200.dp) 150.dp else 200.dp
    }
}

private class WithConstraintsTestCase : ComposeTestCase, ToggleableTestCase {

    private lateinit var state: MutableState<Dp>

    @Composable
    override fun emitContent() {
        val size = state { 200.dp }
        this.state = size
        WithConstraints {
            Box(Modifier.preferredSize(300.dp), gravity = ContentGravity.Center) {
                Spacer(Modifier.preferredSize(width = size.value, height = size.value))
            }
        }
    }

    override fun toggleState() {
        state.value = if (state.value == 200.dp) 150.dp else 200.dp
    }
}

private class ChangingConstraintsTestCase : ComposeTestCase, ToggleableTestCase {

    private lateinit var state: MutableState<IntPx>

    @Composable
    override fun emitContent() {
        val size = state { 100.ipx }
        this.state = size
        ChangingConstraintsLayout(state) {
            WithConstraints {
                Box(Modifier.fillMaxSize())
            }
        }
    }

    override fun toggleState() {
        state.value = if (state.value == 100.ipx) 50.ipx else 100.ipx
    }
}

@Composable
private fun ChangingConstraintsLayout(size: State<IntPx>, children: @Composable () -> Unit) {
    Layout(children) { measurables, _, _ ->
        val constraints = Constraints.fixed(size.value, size.value)
        with(PlacementScope) { measurables.first().measure(constraints).place(0.ipx, 0.ipx) }
        layout(100.ipx, 100.ipx) {}
    }
}

private object PlacementScope : Placeable.PlacementScope() {
    override val parentWidth = 0.ipx
    override val parentLayoutDirection = LayoutDirection.Ltr
}
