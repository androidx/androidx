/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.animation.benchmark

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.toggleStateBenchmarkRecompose
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class TransitionBenchmark {

    @get:Rule val rule = ComposeBenchmarkRule()

    /** Measures the cost of calling [updateTransition]. */
    @Test
    fun createTransitionThroughUpdateTransition() {
        rule.benchmarkFirstCompose(::UpdateTransitionTestCase)
    }

    /** Measures the cost of calling [rememberTransition]. */
    @Test
    fun createTransitionThroughRememberTransition() {
        rule.benchmarkFirstCompose { RememberTransitionTestCase(sameState = true) }
    }

    /** Measures the cost of calling [rememberTransition] with different initial state. */
    @Test
    fun createTransitionThroughRememberTransition_differentState() {
        rule.benchmarkFirstCompose { RememberTransitionTestCase(sameState = false) }
    }

    /** Measures the cost of running a [androidx.compose.animation.core.Transition]. */
    @Test
    fun recomposeTransition() {
        rule.toggleStateBenchmarkRecompose(::TransitionTestCase, assertOneRecomposition = false)
    }
}

private class UpdateTransitionTestCase : LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        updateTransition(targetState = Unit, label = null)
    }
}

private class RememberTransitionTestCase(private val sameState: Boolean) :
    LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        val state = remember { MutableTransitionState(true) }
        if (!sameState) {
            state.targetState = false
        }
        rememberTransition(state, label = null)
    }
}

private class TransitionTestCase : LayeredComposeTestCase(), ToggleableTestCase {
    private var state by mutableStateOf(false)

    @Composable
    override fun MeasuredContent() {
        val transition = updateTransition(state, label = "Benchmark")

        val alpha by transition.animateFloat(label = "Alpha") { if (it) 1f else 0f }
        val size by transition.animateDp(label = "Size") { if (it) 100.dp else 50.dp }

        Box(
            Modifier.layout { measurable, constraints ->
                val currentSize = size.roundToPx()
                val currentAlpha = alpha // Just reading to simulate usage

                val placeable = measurable.measure(constraints)
                layout(currentSize, currentSize) { placeable.place(0, 0) }
            }
        )
    }

    override fun toggleState() {
        state = !state
    }
}
