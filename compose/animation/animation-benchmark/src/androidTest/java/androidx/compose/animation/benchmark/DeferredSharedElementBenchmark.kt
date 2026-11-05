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

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.DeferredAnimatedContent
import androidx.compose.animation.MutableContentTransform
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.DeferredTransitionBehavior
import androidx.compose.animation.SharedTransitionScope.SharedContentConfig
import androidx.compose.animation.core.DeferredTransitionState
import androidx.compose.animation.core.rememberDeferredTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
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
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class DeferredSharedElementBenchmark {

    @get:Rule val rule = ComposeBenchmarkRule()

    /**
     * Measures the initial composition cost of a screen with Shared Elements
     * ([DeferredTransitionBehavior.FollowContainer]) inside [DeferredAnimatedContent].
     */
    @Test
    fun instantiateDeferredSharedElement() {
        rule.benchmarkFirstCompose {
            DeferredSharedElementTestCase(
                SharedElementCount,
                DeferredTransitionBehavior.FollowContainer,
            )
        }
    }

    /**
     * Measures the initiation frames until idle when [DeferredTransitionState.defer] is called on a
     * screen with [DeferredTransitionBehavior.FollowContainer] shared elements.
     */
    @Test
    fun deferInitiation() {
        rule.runBenchmarkFor({
            DeferredSharedElementTestCase(
                SharedElementCount,
                DeferredTransitionBehavior.FollowContainer,
            )
        }) {
            rule.runOnUiThread { doFramesUntilNoChangesPending(60) }
            rule.measureRepeatedOnUiThread {
                getTestCase().deferToTarget()
                var frames = 0
                do {
                    check(frames++ < MaxInitiationFrames) { "Defer initiation did not stabilize" }
                    recomposeAssertHadChanges()
                    Snapshot.sendApplyNotifications()
                    runWithMeasurementDisabled { requestLayout() }
                    measure()
                    layout()
                    Snapshot.sendApplyNotifications()
                    runWithMeasurementDisabled { drawPrepare() }
                    draw()
                    runWithMeasurementDisabled { drawFinish() }
                } while (runWithMeasurementDisabled { hasPendingChanges() })
                runWithMeasurementDisabled {
                    getTestCase().resetToInitial()
                    doFramesUntilNoChangesPending(60)
                }
            }
        }
    }

    /**
     * Measures per-frame measure and layout duration during steady-state gesture scrubbing with
     * [DeferredTransitionBehavior.FollowContainer] (container pivot and transform projection).
     */
    @Test
    fun gestureUpdateLayoutFollowContainer() {
        benchmarkGestureUpdateLayout(DeferredTransitionBehavior.FollowContainer)
    }

    /**
     * Measures per-frame measure and layout duration during steady-state gesture scrubbing with
     * [DeferredTransitionBehavior.Static] (detached from container transforms, so only the
     * container itself is transformed by the gesture).
     */
    @Test
    fun gestureUpdateLayoutStatic() {
        benchmarkGestureUpdateLayout(DeferredTransitionBehavior.Static)
    }

    /**
     * Measures per-frame measure and layout duration during steady-state gesture scrubbing with
     * [DeferredTransitionBehavior.SeekToTarget] (bounds interpolation and fixed-constraint
     * remeasurement).
     */
    @Test
    fun gestureUpdateLayoutSeekToTarget() {
        benchmarkGestureUpdateLayout(DeferredTransitionBehavior.SeekToTarget)
    }

    /**
     * Measures per-frame draw duration of the whole view tree (including the shared element
     * overlay) during steady-state gesture scrubbing with
     * [DeferredTransitionBehavior.FollowContainer] (canvas translation and scale transform around
     * container pivot).
     */
    @Test
    fun gestureUpdateDrawFollowContainer() {
        benchmarkGestureUpdateDraw(DeferredTransitionBehavior.FollowContainer)
    }

    /**
     * Measures per-frame draw duration of the whole view tree (including the shared element
     * overlay) during steady-state gesture scrubbing with [DeferredTransitionBehavior.Static].
     */
    @Test
    fun gestureUpdateDrawStatic() {
        benchmarkGestureUpdateDraw(DeferredTransitionBehavior.Static)
    }

    /**
     * Measures per-frame draw duration of the whole view tree (including the shared element
     * overlay) during steady-state gesture scrubbing with
     * [DeferredTransitionBehavior.SeekToTarget].
     */
    @Test
    fun gestureUpdateDrawSeekToTarget() {
        benchmarkGestureUpdateDraw(DeferredTransitionBehavior.SeekToTarget)
    }

    /**
     * Measures the handoff measure and layout pass when committing a deferred gesture with
     * [DeferredTransitionBehavior.FollowContainer] via [DeferredTransitionState.animateTo]
     * (container pivot transform and velocity projection).
     */
    @Test
    fun handoffLayoutFollowContainer() {
        benchmarkHandoffLayout(DeferredTransitionBehavior.FollowContainer)
    }

    /**
     * Measures the handoff measure and layout pass when committing a deferred gesture with
     * [DeferredTransitionBehavior.SeekToTarget] via [DeferredTransitionState.animateTo].
     */
    @Test
    fun handoffLayoutSeekToTarget() {
        benchmarkHandoffLayout(DeferredTransitionBehavior.SeekToTarget)
    }

    private fun benchmarkHandoffLayout(behavior: DeferredTransitionBehavior) {
        rule.runBenchmarkFor({ DeferredSharedElementTestCase(SharedElementCount, behavior) }) {
            rule.runOnUiThread { doFramesUntilNoChangesPending(60) }
            rule.measureRepeatedOnUiThread {
                runWithMeasurementDisabled {
                    getTestCase().resetToInitial()
                    doFramesUntilNoChangesPending(60)
                    getTestCase().deferToTarget()
                    doFramesUntilNoChangesPending(10)
                    // Commit without stepping the gesture right before: handoff velocities are
                    // sampled from wall-clock time, so constant gesture values keep handoff
                    // velocity calculation and initial conditions deterministic across iterations.
                    getTestCase().commitToTarget()
                    recomposeAssertHadChanges()
                    Snapshot.sendApplyNotifications()
                    check(hasPendingMeasureOrLayout())
                    requestLayout()
                }
                measure()
                layout()
                runWithMeasurementDisabled { drawToBitmap() }
            }
        }
    }

    private fun benchmarkGestureUpdateLayout(behavior: DeferredTransitionBehavior) {
        rule.runBenchmarkFor({ DeferredSharedElementTestCase(SharedElementCount, behavior) }) {
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
                runWithMeasurementDisabled { Snapshot.sendApplyNotifications() }
            }
        }
    }

    private fun benchmarkGestureUpdateDraw(behavior: DeferredTransitionBehavior) {
        rule.runBenchmarkFor({ DeferredSharedElementTestCase(SharedElementCount, behavior) }) {
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
                    drawPrepare()
                }
                draw()
                runWithMeasurementDisabled { drawFinish() }
            }
        }
    }
}

private const val SharedElementCount = 10
private const val MaxInitiationFrames = 10

@Composable
private fun Modifier.deferredSharedElements(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    behavior: DeferredTransitionBehavior,
    count: Int,
): Modifier =
    with(sharedTransitionScope) {
        var result = this@deferredSharedElements
        repeat(count) {
            result =
                result.sharedElement(
                    rememberSharedContentState(
                        key = "key_$it",
                        config = SharedContentConfig(deferredTransitionBehavior = behavior),
                    ),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
        }
        result
    }

private class DeferredSharedElementTestCase(
    private val modifierCount: Int,
    private val behavior: DeferredTransitionBehavior,
) : LayeredComposeTestCase() {
    private val deferredState = DeferredTransitionState(false)
    private var gestureProgress by mutableFloatStateOf(0.2f)

    fun deferToTarget() {
        gestureProgress = 0.2f
        deferredState.defer(true)
    }

    fun stepGestureProgress() {
        gestureProgress = if (gestureProgress < 0.8f) gestureProgress + 0.1f else 0.2f
    }

    fun commitToTarget() {
        deferredState.animateTo(true)
    }

    fun resetToInitial() {
        deferredState.animateTo(false)
    }

    @Composable
    override fun MeasuredContent() {
        SharedTransitionLayout {
            val transition = rememberDeferredTransition(deferredState, label = "SharedElementAnim")
            val mutableTransform = remember {
                MutableContentTransform {
                    sharedElementProgress { gestureProgress }
                    initialContentTransform { fullSize ->
                        val p = gestureProgress
                        offset = IntOffset((fullSize.width * p * 0.2f).roundToInt(), 0)
                        scale = 1f - p * 0.15f
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    }
                    targetContentTransform { fullSize ->
                        val p = gestureProgress
                        offset = IntOffset((-fullSize.width * (1f - p) * 0.2f).roundToInt(), 0)
                        scale = 0.85f + p * 0.15f
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    }
                }
            }

            transition.DeferredAnimatedContent(
                transitionSpec = {
                    (fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.85f)) togetherWith
                        (fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.85f))
                },
                mutableTransformSpec = { mutableTransform },
            ) { targetExpanded ->
                Box(Modifier.fillMaxSize()) {
                    if (targetExpanded) {
                        Box(
                            Modifier.offset(100.dp, 100.dp)
                                .size(200.dp)
                                .deferredSharedElements(
                                    this@SharedTransitionLayout,
                                    this@DeferredAnimatedContent,
                                    behavior,
                                    modifierCount,
                                )
                                .background(Color.Red)
                        )
                    } else {
                        Box(
                            Modifier.offset(0.dp, 0.dp)
                                .size(50.dp)
                                .deferredSharedElements(
                                    this@SharedTransitionLayout,
                                    this@DeferredAnimatedContent,
                                    behavior,
                                    modifierCount,
                                )
                                .background(Color.Red)
                        )
                    }
                }
            }
        }
    }
}
