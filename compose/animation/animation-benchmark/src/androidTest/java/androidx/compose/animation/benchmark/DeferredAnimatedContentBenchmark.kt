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

import androidx.benchmark.junit4.BenchmarkRule
import androidx.compose.animation.DeferredAnimatedContent
import androidx.compose.animation.MutableContentTransform
import androidx.compose.animation.core.DeferredTransitionState
import androidx.compose.animation.core.rememberDeferredTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.unveilIn
import androidx.compose.animation.veilOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.testutils.ComposeBenchmarkScope
import androidx.compose.testutils.ComposeExecutionControl
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.assertNoPendingChanges
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.testutils.recomposeAssertHadChanges
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class DeferredAnimatedContentBenchmark {

    @get:Rule val rule = ComposeBenchmarkRule()

    /** Measures the initial composition cost of [DeferredAnimatedContent]. */
    @Test fun compose() = rule.benchmarkFirstCompose(::DeferredAnimatedContentTestCase)

    /**
     * Measures the initial frame cost when [DeferredTransitionState.defer] is invoked from a
     * settled state, composing and placing the incoming target content in its deferred state.
     */
    @Test
    fun deferInitiation() {
        rule.runBenchmarkFor(::DeferredAnimatedContentTestCase) {
            rule.runOnUiThread { doFramesUntilNoChangesPending(60) }
            rule.measureRepeatedOnUiThread {
                getTestCase().deferToTarget()
                recomposeAssertHadChanges()
                runWithMeasurementDisabled {
                    check(hasPendingMeasureOrLayout())
                    requestLayout()
                }
                measure()
                layout()
                Snapshot.sendApplyNotifications()
                runWithMeasurementDisabled { drawPrepare() }
                draw()
                runWithMeasurementDisabled {
                    drawFinish()
                    getTestCase().resetToInitial()
                    doFramesUntilNoChangesPending(60)
                }
            }
        }
    }

    /**
     * Measures per-frame measure and layout duration during steady-state gesture scrubbing with
     * [MutableContentTransform], asserting zero recompositions per gesture frame.
     */
    @Test
    fun gestureUpdateLayout() {
        rule.runBenchmarkFor(::DeferredAnimatedContentTestCase) {
            rule.runOnUiThread {
                doFramesUntilNoChangesPending(60)
                getTestCase().deferToTarget()
                doFramesUntilNoChangesPending(10)
            }
            rule.measureRepeatedOnUiThread {
                runWithMeasurementDisabled {
                    getTestCase().stepGestureProgress()
                    Snapshot.sendApplyNotifications()
                    assertNoPendingChanges()
                    check(hasPendingMeasureOrLayout())
                    requestLayout()
                }
                measure()
                layout()
                runWithMeasurementDisabled {
                    Snapshot.sendApplyNotifications()
                    check(!hasPendingMeasureOrLayout())
                }
            }
        }
    }

    /**
     * Measures per-frame draw duration of the whole view tree during steady-state gesture scrubbing
     * with [MutableContentTransform] (including veil rendering), asserting zero recompositions.
     */
    @Test
    fun gestureUpdateDraw() {
        rule.runBenchmarkFor(::DeferredAnimatedContentTestCase) {
            rule.runOnUiThread {
                doFramesUntilNoChangesPending(60)
                getTestCase().deferToTarget()
                doFramesUntilNoChangesPending(10)
            }
            rule.measureRepeatedOnUiThread {
                runWithMeasurementDisabled {
                    getTestCase().stepGestureProgress()
                    Snapshot.sendApplyNotifications()
                    assertNoPendingChanges()
                    check(hasPendingMeasureOrLayout())
                    requestLayout()
                    measure()
                    layout()
                    Snapshot.sendApplyNotifications()
                    check(!hasPendingMeasureOrLayout())
                    drawPrepare()
                }
                draw()
                runWithMeasurementDisabled { drawFinish() }
            }
        }
    }

    /**
     * Measures recomposition (including animation frame ticks) across all frames of the
     * handoff-and-settle sequence when committing a deferred gesture via
     * [DeferredTransitionState.animateTo].
     */
    @Test
    fun handoffAndSettleRecompose() {
        rule.runBenchmarkFor(::DeferredAnimatedContentTestCase) {
            rule.runOnUiThread { doFramesUntilNoChangesPending(60) }
            rule.measureRepeatedOnUiThread {
                runWithMeasurementDisabled { prepareHandoff() }
                forEachSettleFrame(this@runBenchmarkFor) {
                    recompose()
                    runWithMeasurementDisabled {
                        requestLayout()
                        measure()
                        layout()
                        drawToBitmap()
                    }
                }
            }
        }
    }

    /**
     * Measures measure and layout across all frames of the handoff-and-settle sequence when
     * committing a deferred gesture via [DeferredTransitionState.animateTo].
     */
    @Test
    fun handoffAndSettleMeasureLayout() {
        rule.runBenchmarkFor(::DeferredAnimatedContentTestCase) {
            rule.runOnUiThread { doFramesUntilNoChangesPending(60) }
            rule.measureRepeatedOnUiThread {
                runWithMeasurementDisabled { prepareHandoff() }
                forEachSettleFrame(this@runBenchmarkFor) {
                    runWithMeasurementDisabled {
                        recompose()
                        requestLayout()
                    }
                    measure()
                    layout()
                    runWithMeasurementDisabled { drawToBitmap() }
                }
            }
        }
    }

    /**
     * Measures draw of the whole view tree across all frames of the handoff-and-settle sequence
     * when committing a deferred gesture via [DeferredTransitionState.animateTo].
     */
    @Test
    fun handoffAndSettleDraw() {
        rule.runBenchmarkFor(::DeferredAnimatedContentTestCase) {
            rule.runOnUiThread { doFramesUntilNoChangesPending(60) }
            rule.measureRepeatedOnUiThread {
                runWithMeasurementDisabled { prepareHandoff() }
                forEachSettleFrame(this@runBenchmarkFor) {
                    runWithMeasurementDisabled {
                        recompose()
                        requestLayout()
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

    /**
     * Measures all frames needed to settle after a new deferred gesture
     * ([DeferredTransitionState.defer]) interrupts an in-flight transition. This includes the
     * catch-up spring as well as the remainder of the interrupted transition.
     */
    @Test
    fun interruptWithCatchUpFullSequence() {
        rule.runBenchmarkFor(::DeferredAnimatedContentTestCase) {
            rule.runOnUiThread { doFramesUntilNoChangesPending(60) }
            rule.measureRepeatedOnUiThread {
                runWithMeasurementDisabled {
                    getTestCase().resetToInitial()
                    doFramesUntilNoChangesPending(60)
                    getTestCase().commitToTarget()
                    repeat(3) { doFrame() }
                    getTestCase().deferToInitial()
                }
                doFramesUntilNoChangesPending(maxAmountOfFrames = 60)
            }
        }
    }

    /**
     * Resets to the initial state and enters the deferred phase towards the target, ready for
     * [DeferredAnimatedContentTestCase.commitToTarget].
     *
     * The gesture progress is not stepped right before the commit: handoff velocities are derived
     * from wall-clock sampled values, so keeping the gesture values constant pins them to zero and
     * keeps handoff velocity calculation and initial conditions deterministic across iterations.
     */
    private fun ComposeBenchmarkScope<DeferredAnimatedContentTestCase>.prepareHandoff() {
        getTestCase().resetToInitial()
        doFramesUntilNoChangesPending(60)
        getTestCase().deferToTarget()
        doFramesUntilNoChangesPending(10)
        getTestCase().commitToTarget()
        Snapshot.sendApplyNotifications()
    }

    /**
     * Runs [frame] until [control] has no pending changes, checking for pending changes outside of
     * the measured region.
     */
    private inline fun BenchmarkRule.Scope.forEachSettleFrame(
        control: ComposeExecutionControl,
        frame: () -> Unit,
    ) {
        var frames = 0
        while (runWithMeasurementDisabled { control.hasPendingChanges() }) {
            check(frames++ < MaxSettleFrames) { "Transition did not settle" }
            frame()
        }
    }
}

private const val MaxSettleFrames = 60

private class DeferredAnimatedContentTestCase : LayeredComposeTestCase() {
    private val deferredState = DeferredTransitionState(true)
    private var gestureProgress by mutableFloatStateOf(0.2f)

    fun deferToTarget() {
        gestureProgress = 0.2f
        deferredState.defer(false)
    }

    fun deferToInitial() {
        gestureProgress = 0.4f
        deferredState.defer(true)
    }

    fun stepGestureProgress() {
        gestureProgress = if (gestureProgress < 0.8f) gestureProgress + 0.1f else 0.2f
    }

    fun commitToTarget() {
        deferredState.animateTo(false)
    }

    fun resetToInitial() {
        deferredState.animateTo(true)
    }

    @Composable
    override fun MeasuredContent() {
        val transition = rememberDeferredTransition(deferredState, label = "DeferredACBenchmark")
        val mutableTransform = remember {
            MutableContentTransform {
                initialContentTransform { fullSize ->
                    val p = gestureProgress
                    offset = IntOffset((fullSize.width * p * 0.2f).roundToInt(), 0)
                    scale = 1f - p * 0.1f
                    alpha = 1f - p * 0.2f
                    transformOrigin = TransformOrigin(1f, 0.5f)
                    veil = Color.Black.copy(alpha = p * 0.3f)
                }
                targetContentTransform { fullSize ->
                    val p = gestureProgress
                    offset = IntOffset((-fullSize.width * (1f - p) * 0.2f).roundToInt(), 0)
                    scale = 0.9f + p * 0.1f
                    alpha = 0.5f + p * 0.5f
                    transformOrigin = TransformOrigin(0f, 0.5f)
                    veil = Color.Black.copy(alpha = (1f - p) * 0.3f)
                }
            }
        }

        transition.DeferredAnimatedContent(
            transitionSpec = {
                (fadeIn(tween(160)) +
                    scaleIn(tween(160), initialScale = 0.9f) +
                    slideInHorizontally(tween(160)) { -it / 5 } +
                    unveilIn(
                        tween(160),
                        initialColor = Color.Black.copy(alpha = 0.3f),
                    )) togetherWith
                    (fadeOut(tween(160)) +
                        scaleOut(tween(160), targetScale = 0.9f) +
                        slideOutHorizontally(tween(160)) { it / 5 } +
                        veilOut(tween(160), targetColor = Color.Black.copy(alpha = 0.3f)))
            },
            mutableTransformSpec = { mutableTransform },
        ) { targetState ->
            Box(Modifier.fillMaxSize().background(if (targetState) Color.Red else Color.Green))
        }
    }
}
