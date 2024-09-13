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

package androidx.wear.compose.material3

import androidx.annotation.IntRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import kotlin.math.asin
import kotlin.math.floor
import kotlin.math.min
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Material Design segmented circular progress indicator.
 *
 * A segmented variant of [CircularProgressIndicator] that is divided into equally sized segments.
 *
 * Example of [SegmentedCircularProgressIndicator] with progress value:
 *
 * @sample androidx.wear.compose.material3.samples.SegmentedProgressIndicatorSample
 * @param segmentCount Number of equal segments that the progress indicator should be divided into.
 *   Has to be a number equal or greater to 1.
 * @param progress The progress of this progress indicator where 0.0 represents no progress and 1.0
 *   represents completion. Values smaller than 0.0 will be coerced to 0, while values larger than
 *   1.0 will be wrapped around and shown as overflow with a different track color. The progress is
 *   applied to the entire [SegmentedCircularProgressIndicator] across all segments. Progress
 *   changes will be animated.
 * @param modifier Modifier to be applied to the SegmentedCircularProgressIndicator.
 * @param allowProgressOverflow When progress overflow is allowed, values smaller than 0.0 will be
 *   coerced to 0, while values larger than 1.0 will be wrapped around and shown as overflow with a
 *   different track color [ProgressIndicatorColors.overflowTrackBrush]. For example values 1.2, 2.2
 *   etc will be shown as 20% progress with the overflow color. When progress overflow is not
 *   allowed, progress values will be coerced into the range 0..1.
 * @param startAngle The starting position of the progress arc, measured clockwise in degrees (0
 *   to 360) from the 3 o'clock position. For example, 0 and 360 represent 3 o'clock, 90 and 180
 *   represent 6 o'clock and 9 o'clock respectively. Default is 270 degrees
 *   [CircularProgressIndicatorDefaults.StartAngle] (top of the screen).
 * @param endAngle The ending position of the progress arc, measured clockwise in degrees (0 to 360)
 *   from the 3 o'clock position. For example, 0 and 360 represent 3 o'clock, 90 and 180 represent 6
 *   o'clock and 9 o'clock respectively. By default equal to [startAngle].
 * @param colors [ProgressIndicatorColors] that will be used to resolve the indicator and track
 *   color for this progress indicator in different states.
 * @param strokeWidth The stroke width for the progress indicator.
 * @param gapSize The size of the gap between segments (in Dp).
 * @param enabled controls the enabled state. Although this component is not clickable, it can be
 *   contained within a clickable component. When enabled is `false`, this component will appear
 *   visually disabled.
 */
@Composable
fun SegmentedCircularProgressIndicator(
    @IntRange(from = 1) segmentCount: Int,
    progress: () -> Float,
    modifier: Modifier = Modifier,
    allowProgressOverflow: Boolean = false,
    startAngle: Float = CircularProgressIndicatorDefaults.StartAngle,
    endAngle: Float = startAngle,
    colors: ProgressIndicatorColors = ProgressIndicatorDefaults.colors(),
    strokeWidth: Dp = CircularProgressIndicatorDefaults.largeStrokeWidth,
    gapSize: Dp = CircularProgressIndicatorDefaults.calculateRecommendedGapSize(strokeWidth),
    enabled: Boolean = true,
) {
    var currentProgress by remember { mutableFloatStateOf(progress()) }
    val progressAnimationSpec = determinateCircularProgressAnimationSpec
    val colorAnimationSpec = progressOverflowColorAnimationSpec
    val fullSweep = 360f - ((startAngle - endAngle) % 360 + 360) % 360

    val animatedProgress = remember {
        Animatable(if (allowProgressOverflow) progress() else progress().coerceIn(0f, 1f))
    }
    val animatedOverflowColor = remember { Animatable(if (progress() > 1f) 1f else 0f) }

    LaunchedEffect(progress, allowProgressOverflow) {
        snapshotFlow(progress).collectLatest {
            val newProgress = if (allowProgressOverflow) it else it.coerceIn(0f, 1f)

            if (allowProgressOverflow && newProgress <= 1f && currentProgress > 1f) {
                // Reverse overflow color transition - animate the progress and color at the
                // same time.
                launch {
                    awaitAll(
                        async { animatedProgress.animateTo(newProgress, progressAnimationSpec) },
                        async { animatedOverflowColor.animateTo(0f, colorAnimationSpec) }
                    )
                }
            } else if (allowProgressOverflow && newProgress > 1f && currentProgress <= 1f) {
                // Animate the progress arc.
                animatedProgress.animateTo(newProgress, progressAnimationSpec) {
                    // Start overflow color transition when progress crosses 1 (full circle).
                    if (animatedProgress.value.equalsWithTolerance(1f)) {
                        launch { animatedOverflowColor.animateTo(1f, colorAnimationSpec) }
                    }
                }
                animatedOverflowColor.snapTo(1f)
            } else {
                animatedProgress.animateTo(newProgress, progressAnimationSpec)
            }

            currentProgress = newProgress
        }
    }

    Spacer(
        modifier
            .clearAndSetSemantics {}
            .fillMaxSize()
            .drawWithCache {
                onDrawWithContent {
                    val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                    val minSize = min(size.height, size.width)
                    // Sweep angle between two progress indicator segments.
                    val gapSweep =
                        asin((stroke.width + gapSize.toPx()) / (minSize - stroke.width))
                            .toDegrees() * 2f
                    val segmentSweepAngle = fullSweep / segmentCount
                    val wrappedProgress =
                        wrapProgress(animatedProgress.value, allowProgressOverflow)
                    val progressInSegments = segmentCount * wrappedProgress
                    val targetProgress =
                        segmentCount *
                            wrapProgress(animatedProgress.targetValue, allowProgressOverflow)
                    val hasOverflow = allowProgressOverflow && animatedProgress.value > 1.0f

                    for (segment in 0 until segmentCount) {
                        val segmentStartAngle = startAngle + fullSweep * segment / segmentCount
                        if (segment >= floor(progressInSegments)) {
                            if (hasOverflow) {
                                drawIndicatorSegment(
                                    startAngle = segmentStartAngle,
                                    sweep = segmentSweepAngle,
                                    gapSweep = gapSweep,
                                    brush =
                                        colors.animatedOverflowTrackBrush(
                                            enabled,
                                            animatedOverflowColor.value
                                        ),
                                    stroke = stroke,
                                )
                            } else {
                                drawIndicatorSegment(
                                    startAngle = segmentStartAngle,
                                    sweep = segmentSweepAngle,
                                    gapSweep = gapSweep,
                                    brush = colors.trackBrush(enabled),
                                    stroke = stroke,
                                )
                            }
                        }

                        if (segment < progressInSegments) {
                            var progressSweep =
                                segmentSweepAngle * (progressInSegments - segment).coerceAtMost(1f)

                            // If progress sweep in segment is smaller than gap sweep, it
                            // will be shown as a small dot. This dot should never be shown as
                            // static value, only in progress animation transitions.
                            val isValidTarget =
                                targetProgress < segment ||
                                    targetProgress > segment + 1 ||
                                    targetProgress.isFullInt() ||
                                    floor(animatedProgress.value) !=
                                        floor(animatedProgress.targetValue) ||
                                    segmentSweepAngle * (targetProgress - segment) > gapSweep

                            if (progressSweep != 0f && !isValidTarget) {
                                progressSweep = progressSweep.coerceIn(gapSweep, segmentSweepAngle)
                            }

                            drawIndicatorSegment(
                                startAngle = segmentStartAngle,
                                sweep = progressSweep,
                                gapSweep = gapSweep,
                                brush = colors.indicatorBrush(enabled),
                                stroke = stroke,
                            )
                        }
                    }
                }
            }
    )
}

/**
 * Material Design segmented circular progress indicator.
 *
 * A segmented variant of [CircularProgressIndicator] that is divided into equally sized segments.
 * This overload of [SegmentedCircularProgressIndicator] allows for each segment to be individually
 * indicated as completed, such as for showing activity for intervals within a longer period
 *
 * Example of [SegmentedCircularProgressIndicator] where the segments are turned on/off:
 *
 * @sample androidx.wear.compose.material3.samples.SegmentedProgressIndicatorBinarySample
 *
 * Example of smaller size [SegmentedCircularProgressIndicator]:
 *
 * @sample androidx.wear.compose.material3.samples.SmallSegmentedProgressIndicatorSample
 * @param segmentCount Number of equal segments that the progress indicator should be divided into.
 *   Has to be a number equal or greater to 1.
 * @param segmentValue A function that for each segment between 1..[segmentCount] returns true if
 *   this segment should be displayed with the indicator color to show progress, and false if the
 *   segment should be displayed with the track color.
 * @param modifier Modifier to be applied to the SegmentedCircularProgressIndicator.
 * @param startAngle The starting position of the progress arc, measured clockwise in degrees (0
 *   to 360) from the 3 o'clock position. For example, 0 and 360 represent 3 o'clock, 90 and 180
 *   represent 6 o'clock and 9 o'clock respectively. Default is 270 degrees
 *   [CircularProgressIndicatorDefaults.StartAngle] (top of the screen).
 * @param endAngle The ending position of the progress arc, measured clockwise in degrees (0 to 360)
 *   from the 3 o'clock position. For example, 0 and 360 represent 3 o'clock, 90 and 180 represent 6
 *   o'clock and 9 o'clock respectively. By default equal to [startAngle].
 * @param colors [ProgressIndicatorColors] that will be used to resolve the indicator and track
 *   color for this progress indicator in different states.
 * @param strokeWidth The stroke width for the progress indicator.
 * @param gapSize The size of the gap between segments (in Dp).
 * @param enabled controls the enabled state. Although this component is not clickable, it can be
 *   contained within a clickable component. When enabled is `false`, this component will appear
 *   visually disabled.
 */
@Composable
fun SegmentedCircularProgressIndicator(
    @IntRange(from = 1) segmentCount: Int,
    segmentValue: (segmentIndex: Int) -> Boolean,
    modifier: Modifier = Modifier,
    startAngle: Float = CircularProgressIndicatorDefaults.StartAngle,
    endAngle: Float = startAngle,
    colors: ProgressIndicatorColors = ProgressIndicatorDefaults.colors(),
    strokeWidth: Dp = CircularProgressIndicatorDefaults.largeStrokeWidth,
    gapSize: Dp = CircularProgressIndicatorDefaults.calculateRecommendedGapSize(strokeWidth),
    enabled: Boolean = true,
) {
    var shouldAnimateProgress by remember { mutableStateOf(false) }
    val progressAnimationSpec = binarySegmentedProgressAnimationSpec
    val fullSweep = 360f - ((startAngle - endAngle) % 360 + 360) % 360
    val animatedProgress = remember { Animatable(1f) }

    LaunchedEffect(segmentValue) {
        if (shouldAnimateProgress) {
            // For the  binary version of segmented progress, we don't have a progress, but we
            // still have to emulate the progress animation, so we just animate the progress from
            // 0 to 1 each time the segmentValue parameter changes.
            animatedProgress.snapTo(0f)
            animatedProgress.animateTo(1f, progressAnimationSpec)
        } else {
            // Do not animate progress on the first composition, only on parameter update.
            shouldAnimateProgress = true
        }
    }

    Spacer(
        modifier
            .clearAndSetSemantics {}
            .fillMaxSize()
            .drawWithCache {
                onDrawWithContent {
                    val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                    val minSize = min(size.height, size.width)
                    // Sweep angle between two progress indicator segments.
                    val gapSweep =
                        asin((stroke.width + gapSize.toPx()) / (minSize - stroke.width))
                            .toDegrees() * 2f
                    val segmentSweepAngle = fullSweep / segmentCount
                    val currentProgress = animatedProgress.value
                    val progressInSegments = segmentCount * currentProgress

                    for (segment in 0 until segmentCount) {
                        val segmentStartAngle = startAngle + fullSweep * segment / segmentCount

                        drawIndicatorSegment(
                            startAngle = segmentStartAngle,
                            sweep = segmentSweepAngle,
                            gapSweep = gapSweep,
                            brush = colors.trackBrush(enabled),
                            stroke = stroke,
                        )
                        if (segment < progressInSegments && segmentValue(segment)) {
                            var progressSweep =
                                segmentSweepAngle * (progressInSegments - segment).coerceAtMost(1f)

                            // Coerce progress sweep to the minimum of gap sweep.
                            if (progressSweep != 0f) {
                                progressSweep = progressSweep.coerceIn(gapSweep, segmentSweepAngle)
                            }

                            drawIndicatorSegment(
                                startAngle = segmentStartAngle,
                                sweep = progressSweep,
                                gapSweep = gapSweep,
                                brush = colors.indicatorBrush(enabled),
                                stroke = stroke,
                            )
                        }
                    }
                }
            }
    )
}

/** Progress animation spec for binary [SegmentedCircularProgressIndicator] */
internal val binarySegmentedProgressAnimationSpec: AnimationSpec<Float>
    @Composable get() = MaterialTheme.motionScheme.fastEffectsSpec()
