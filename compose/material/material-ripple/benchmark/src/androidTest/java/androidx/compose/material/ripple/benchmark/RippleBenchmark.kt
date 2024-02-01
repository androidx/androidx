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

package androidx.compose.material.ripple.benchmark

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.createRippleModifierNode
import androidx.compose.runtime.Composable
import androidx.compose.testutils.ComposeBenchmarkScope
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark for Android ripple performance
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class RippleBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun firstComposition() {
        val interactionSource = MutableInteractionSource()
        benchmarkRule.benchmarkFirstCompose {
            object : LayeredComposeTestCase() {
                @Composable
                override fun MeasuredContent() {
                    Box(Modifier.indication(
                        interactionSource = interactionSource,
                        indication = TestRipple
                    ))
                }
            }
        }
    }

    /**
     * Cost of emitting a [PressInteraction] for the first time (so including any work done as part
     * of collecting the interaction and creating a ripple) and then rendering a frame after that.
     *
     * [PressInteraction] tests the RippleDrawable codepath - other [Interaction]s use a simplified
     * common-code StateLayer.
     */
    @Test
    fun initialEmitPressInteraction() {
        val press = PressInteraction.Press(Offset.Zero)

        with(benchmarkRule) {
            runBenchmarkFor({ RippleInteractionTestCase() }) {
                measureRepeatedOnUiThread {
                    runWithTimingDisabled {
                        doFramesUntilNoChangesMeasureLayoutOrDrawPending()
                    }

                    runBlocking { getTestCase().emitInteraction(press) }
                    doFrame()

                    // We explicitly tear down after each iteration so we incur costs for anything
                    // cached at the view hierarchy level, in this case the RippleContainer.
                    runWithTimingDisabled {
                        disposeContent()
                    }
                }
            }
        }
    }

    /**
     * Cost of emitting another [PressInteraction] and rendering a frame after we have already
     * emitted one.
     */
    @Test
    fun additionalEmitPressInteraction() {
        val press1 = PressInteraction.Press(Offset.Zero)
        val release1 = PressInteraction.Release(press1)
        val press2 = PressInteraction.Press(Offset.Zero)

        with(benchmarkRule) {
            runBenchmarkFor({ RippleInteractionTestCase() }) {
                measureRepeatedOnUiThread {
                    runWithTimingDisabled {
                        doFramesUntilNoChangesMeasureLayoutOrDrawPending()
                        runBlocking {
                            getTestCase().emitInteraction(press1)
                            // Account for RippleHostView#setRippleState logic that will delay
                            // an exit that happens on the same frame as an enter which will cause
                            // a callback to be posted, breaking synchronization in this case /
                            // causing a more costly and rare codepath.
                            delay(100)
                            getTestCase().emitInteraction(release1)
                        }
                        doFramesUntilNoChangesMeasureLayoutOrDrawPending()
                    }

                    runBlocking { getTestCase().emitInteraction(press2) }
                    doFrame()

                    runWithTimingDisabled {
                        disposeContent()
                    }
                }
            }
        }
    }

    /**
     * Cost of emitting a [HoverInteraction] for the first time and then rendering a frame after
     * that.
     *
     * [HoverInteraction] ends up being drawn by a common-code StateLayer, as with focus and drag -
     * so there is no need to test those cases separately.
     */
    @Test
    fun initialEmitHoverInteraction() {
        val hover = HoverInteraction.Enter()

        with(benchmarkRule) {
            runBenchmarkFor({ RippleInteractionTestCase() }) {
                measureRepeatedOnUiThread {
                    runWithTimingDisabled {
                        doFramesUntilNoChangesMeasureLayoutOrDrawPending()
                    }

                    runBlocking { getTestCase().emitInteraction(hover) }
                    doFrame()

                    // We explicitly tear down after each iteration so we incur costs for anything
                    // cached at the view hierarchy level. There shouldn't be anything cached in
                    // this way for the hover case, but we do it to be consistent with the press
                    // case.
                    runWithTimingDisabled {
                        disposeContent()
                    }
                }
            }
        }
    }

    /**
     * Cost of emitting another [HoverInteraction] and rendering a frame after we have already
     * emitted one.
     */
    @Test
    fun additionalEmitHoverInteraction() {
        val hover1 = HoverInteraction.Enter()
        val unhover1 = HoverInteraction.Exit(hover1)
        val hover2 = HoverInteraction.Enter()

        with(benchmarkRule) {
            runBenchmarkFor({ RippleInteractionTestCase() }) {
                measureRepeatedOnUiThread {
                    runWithTimingDisabled {
                        doFramesUntilNoChangesMeasureLayoutOrDrawPending()
                        runBlocking {
                            getTestCase().emitInteraction(hover1)
                            getTestCase().emitInteraction(unhover1)
                        }
                        doFramesUntilNoChangesMeasureLayoutOrDrawPending()
                    }

                    runBlocking { getTestCase().emitInteraction(hover2) }
                    doFrame()

                    runWithTimingDisabled {
                        disposeContent()
                    }
                }
            }
        }
    }
}

/**
 * Test case a ripple that allows emitting [Interaction]s with [emitInteraction].
 */
@Suppress("DEPRECATION_ERROR")
private class RippleInteractionTestCase : LayeredComposeTestCase() {
    private val interactionSource = MutableInteractionSource()

    @Composable
    override fun MeasuredContent() {
        Box(
            Modifier
                .size(100.dp)
                .indication(interactionSource, TestRipple)
        )
    }

    suspend fun emitInteraction(interaction: Interaction) {
        interactionSource.emit(interaction)
    }
}

/**
 * [doFramesUntilNoChangesPending] but also accounts for pending measure or layout passes, and
 * whether the view itself is dirty
 */
private fun ComposeBenchmarkScope<*>.doFramesUntilNoChangesMeasureLayoutOrDrawPending() {
    val maxAmountOfFrames = 10
    var framesDone = 0
    while (framesDone < maxAmountOfFrames) {
        doFrame()
        framesDone++

        fun hasPending(): Boolean {
            var hasPending = hasPendingChanges()
            val hostView = getHostView()
            hasPending = hasPending || (hostView as ViewRootForTest).hasPendingMeasureOrLayout
            hasPending = hasPending || hostView.isDirty
            return hasPending
        }

        if (!(hasPending())) {
            // We are stable!
            return
        }
    }

    // Still not stable
    throw AssertionError("Changes are still pending after '$maxAmountOfFrames' frames.")
}

private val TestRipple = TestIndicationNodeFactory({ TestRippleColor }, { TestRippleAlpha })

private val TestRippleColor = Color.Red

private val TestRippleAlpha = RippleAlpha(
    draggedAlpha = 0.1f,
    focusedAlpha = 0.2f,
    hoveredAlpha = 0.3f,
    pressedAlpha = 0.4f
)

private class TestIndicationNodeFactory(
    private val color: ColorProducer,
    private val rippleAlpha: () -> RippleAlpha
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return createRippleModifierNode(
            interactionSource = interactionSource,
            bounded = true,
            radius = Dp.Unspecified,
            color = color,
            rippleAlpha = rippleAlpha
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TestIndicationNodeFactory

        if (color != other.color) return false
        if (rippleAlpha != other.rippleAlpha) return false

        return true
    }

    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + rippleAlpha.hashCode()
        return result
    }
}
