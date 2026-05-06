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

package androidx.compose.animation.benchmark

import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.MicrobenchmarkConfig
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.CapturedAnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@LargeTest
@RunWith(Parameterized::class)
class CapturedAnimatedVisibilityBenchmark(
    private val useCaptured: Boolean,
    private val isComplexContent: Boolean,
) {
    companion object {
        @JvmStatic
        @Parameters(name = "captured={0}_complex={1}")
        fun data() =
            listOf(
                arrayOf(false, false), // AV Simple
                arrayOf(true, false), // CAV Simple
                arrayOf(false, true), // AV Elaborate
                arrayOf(true, true), // CAV Elaborate
            )
    }

    // Run the whole animation sequence ~18 frames per animation 10 times.
    @OptIn(ExperimentalBenchmarkConfigApi::class)
    @get:Rule
    val rule = ComposeBenchmarkRule(MicrobenchmarkConfig(warmupCount = 3, measurementCount = 10))

    /** Measures total combined CPU duration across the full exit animation sequence. */
    @Test
    fun exitFullSequence() {
        rule.runBenchmarkFor({ VisibilityBenchmarkTestCase(useCaptured, isComplexContent) }) {
            rule.runOnUiThread { doFramesUntilNoChangesPending(60) }
            rule.measureRepeatedOnUiThread {
                runWithMeasurementDisabled {
                    getTestCase().setToVisible()
                    doFramesUntilNoChangesPending(10)
                    // Once all the changes are settled, change the visibility to start exit
                    // animation, and subsequent measurements.
                    getTestCase().setToInvisible()
                }
                doFramesUntilNoChangesPending(maxAmountOfFrames = 60)
            }
        }
    }

    /** Measures isolated Recomposition pass duration across the full exit animation sequence. */
    @Test
    fun exitFullSequenceRecompose() {
        rule.runBenchmarkFor({ VisibilityBenchmarkTestCase(useCaptured, isComplexContent) }) {
            rule.runOnUiThread { doFramesUntilNoChangesPending(60) }
            rule.measureRepeatedOnUiThread {
                runWithMeasurementDisabled {
                    getTestCase().setToVisible()
                    doFramesUntilNoChangesPending(10)
                    getTestCase().setToInvisible()
                }
                while (hasPendingChanges()) {
                    recompose()
                    runWithMeasurementDisabled {
                        measure()
                        layout()
                        drawToBitmap()
                    }
                }
            }
        }
    }

    /** Measures isolated Measure & Layout pass duration across the full exit animation sequence. */
    @Test
    fun exitFullSequenceMeasureLayout() {
        rule.runBenchmarkFor({ VisibilityBenchmarkTestCase(useCaptured, isComplexContent) }) {
            rule.runOnUiThread { doFramesUntilNoChangesPending(60) }
            rule.measureRepeatedOnUiThread {
                runWithMeasurementDisabled {
                    getTestCase().setToVisible()
                    doFramesUntilNoChangesPending(10)
                    getTestCase().setToInvisible()
                }
                while (hasPendingChanges()) {
                    runWithMeasurementDisabled { recompose() }
                    measure()
                    layout()
                    runWithMeasurementDisabled { drawToBitmap() }
                }
            }
        }
    }

    /** Measures isolated Draw pass duration across the full exit animation sequence. */
    @Test
    fun exitFullSequenceDraw() {
        rule.runBenchmarkFor({ VisibilityBenchmarkTestCase(useCaptured, isComplexContent) }) {
            rule.runOnUiThread { doFramesUntilNoChangesPending(60) }
            rule.measureRepeatedOnUiThread {
                runWithMeasurementDisabled {
                    getTestCase().setToVisible()
                    doFramesUntilNoChangesPending(10)
                    getTestCase().setToInvisible()
                }
                while (hasPendingChanges()) {
                    runWithMeasurementDisabled {
                        recompose()
                        measure()
                        layout()
                        drawPrepare()
                    }
                    draw()
                    runWithMeasurementDisabled { drawFinish() }
                }
            }
        }
    }
}

private class VisibilityBenchmarkTestCase(
    private val useCaptured: Boolean,
    private val isComplexContent: Boolean,
) : LayeredComposeTestCase(), ToggleableTestCase {
    val visibleState = MutableTransitionState(true)
    var visible: Boolean = true

    fun setToVisible() {
        visibleState.targetState = true
    }

    fun setToInvisible() {
        visibleState.targetState = false
    }

    @Composable
    override fun MeasuredContent() {
        visible = visibleState.currentState
        if (useCaptured) {
            CapturedAnimatedVisibility(
                visibleState = visibleState,
                enter = EnterTransition.None,
                exit = fadeOut(tween(300)),
            ) {
                if (isComplexContent) ElaborateContent() else SimpleContent()
            }
        } else {
            AnimatedVisibility(
                visibleState = visibleState,
                enter = EnterTransition.None,
                exit = fadeOut(tween(300)),
            ) {
                if (isComplexContent) ElaborateContent() else SimpleContent()
            }
        }
    }

    override fun toggleState() {
        visibleState.targetState = !visibleState.targetState
    }
}

@Composable
private fun SimpleContent() {
    Box(
        modifier = Modifier.size(100.dp).background(Color.Red),
        contentAlignment = Alignment.Center,
    ) {
        BasicText("Simple Content", style = TextStyle(color = Color.White))
    }
}

@Composable
private fun ElaborateContent() {
    Column(Modifier.fillMaxWidth().padding(8.dp)) {
        repeat(15) { rowIndex ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                repeat(4) { colIndex ->
                    Box(
                        modifier =
                            Modifier.weight(1f)
                                .height(40.dp)
                                .padding(2.dp)
                                .background(Color(0xFFE0F7FA)),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "Item $rowIndex-$colIndex",
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                }
            }
        }
    }
}
