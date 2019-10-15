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

package androidx.ui.benchmark.test

import android.app.Activity
import androidx.benchmark.junit4.BenchmarkRule
import androidx.compose.Composable
import androidx.compose.FrameManager
import androidx.compose.State
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.benchmark.measureDrawPerf
import androidx.ui.benchmark.measureFirstCompose
import androidx.ui.benchmark.measureFirstDraw
import androidx.ui.benchmark.measureFirstLayout
import androidx.ui.benchmark.measureFirstMeasure
import androidx.ui.benchmark.measureLayoutPerf
import androidx.ui.benchmark.toggleStateMeasureDraw
import androidx.ui.benchmark.toggleStateMeasureLayout
import androidx.ui.benchmark.toggleStateMeasureMeasure
import androidx.ui.benchmark.toggleStateMeasureRecompose
import androidx.ui.core.Dp
import androidx.ui.core.dp
import androidx.ui.core.setContent
import androidx.ui.layout.Container
import androidx.ui.layout.Padding
import androidx.ui.layout.Spacing
import androidx.ui.test.ComposeTestCase
import androidx.ui.test.DisableTransitions
import androidx.ui.test.ToggleableTestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class PaddingBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val activityRule = ActivityTestRule(Activity::class.java)

    @get:Rule
    val disableAnimationRule = DisableTransitions()

    private val activity: Activity get() = activityRule.activity

    @Test
    fun noModifier_first_compose() {
        benchmarkRule.measureFirstCompose(activity, NoModifierTestCase(activity))
    }

    @Test
    fun noModifier_first_measure() {
        benchmarkRule.measureFirstMeasure(activity, NoModifierTestCase(activity))
    }

    @Test
    fun noModifier_first_layout() {
        benchmarkRule.measureFirstLayout(activity, NoModifierTestCase(activity))
    }

    @Test
    fun noModifier_first_draw() {
        benchmarkRule.measureFirstDraw(activity, NoModifierTestCase(activity))
    }

    @Test
    fun noModifier_togglePadding_recompose() {
        benchmarkRule.toggleStateMeasureRecompose(activity, NoModifierTestCase(activity))
    }

    @Test
    fun noModifier_togglePadding_measure() {
        benchmarkRule.toggleStateMeasureMeasure(activity, NoModifierTestCase(activity))
    }

    @Test
    fun noModifier_togglePadding_layout() {
        benchmarkRule.toggleStateMeasureLayout(activity, NoModifierTestCase(activity))
    }

    @Test
    fun noModifier_togglePadding_draw() {
        benchmarkRule.toggleStateMeasureDraw(activity, NoModifierTestCase(activity))
    }

    @Test
    fun noModifier_layout() {
        benchmarkRule.measureLayoutPerf(activity, NoModifierTestCase(activity))
    }

    @Test
    fun noModifier_draw() {
        benchmarkRule.measureDrawPerf(activity, NoModifierTestCase(activity))
    }

    @Test
    fun modifier_first_compose() {
        benchmarkRule.measureFirstCompose(activity, ModifierTestCase(activity))
    }

    @Test
    fun modifier_first_measure() {
        benchmarkRule.measureFirstMeasure(activity, ModifierTestCase(activity))
    }

    @Test
    fun modifier_first_layout() {
        benchmarkRule.measureFirstLayout(activity, ModifierTestCase(activity))
    }

    @Test
    fun modifier_first_draw() {
        benchmarkRule.measureFirstDraw(activity, ModifierTestCase(activity))
    }

    @Test
    fun modifier_togglePadding_recompose() {
        benchmarkRule.toggleStateMeasureRecompose(activity, ModifierTestCase(activity))
    }

    @Test
    fun modifier_togglePadding_measure() {
        benchmarkRule.toggleStateMeasureMeasure(activity, ModifierTestCase(activity))
    }

    @Test
    fun modifier_togglePadding_layout() {
        benchmarkRule.toggleStateMeasureLayout(activity, ModifierTestCase(activity))
    }

    @Test
    fun modifier_togglePadding_draw() {
        benchmarkRule.toggleStateMeasureDraw(activity, ModifierTestCase(activity))
    }

    @Test
    fun modifier_layout() {
        benchmarkRule.measureLayoutPerf(activity, ModifierTestCase(activity))
    }

    @Test
    fun modifier_draw() {
        benchmarkRule.measureDrawPerf(activity, ModifierTestCase(activity))
    }
}

private sealed class PaddingTestCase(activity: Activity) : ComposeTestCase(activity),
    ToggleableTestCase {

    var paddingState: State<Dp>? = null

    override fun toggleState() {
        with(paddingState!!) {
            value = if (value == 5.dp) 10.dp else 5.dp
        }
        FrameManager.nextFrame()
    }

    override fun setComposeContent(activity: Activity) = activity.setContent {
        val padding = +state { 5.dp }
        paddingState = padding

        Container(expanded = true) {
            emitPaddedContainer(padding.value) {
                emitPaddedContainer(padding.value) {
                    emitPaddedContainer(padding.value) {
                        emitPaddedContainer(padding.value) {
                            emitPaddedContainer(padding.value) {}
                        }
                    }
                }
            }
        }
    }!!

    @Composable
    abstract fun emitPaddedContainer(padding: Dp, child: @Composable() () -> Unit)
}

private class ModifierTestCase(activity: Activity) : PaddingTestCase(activity) {

    @Composable
    override fun emitPaddedContainer(padding: Dp, child: @Composable() () -> Unit) {
        Container(expanded = true, modifier = Spacing(padding), children = child)
    }
}

private class NoModifierTestCase(activity: Activity) : PaddingTestCase(activity) {

    @Composable
    override fun emitPaddedContainer(padding: Dp, child: @Composable() () -> Unit) {
        Container(expanded = true) {
            Padding(padding = padding, children = child)
        }
    }
}
