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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import kotlin.math.asin
import kotlin.math.floor
import kotlin.math.min

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
 *   applied to the entire [SegmentedCircularProgressIndicator] across all segments.
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
) =
    SegmentedCircularProgressIndicatorImpl(
        segmentParams = SegmentParams.Progress(progress, allowProgressOverflow),
        modifier = modifier,
        segmentCount = segmentCount,
        startAngle = startAngle,
        endAngle = endAngle,
        colors = colors,
        strokeWidth = strokeWidth,
        gapSize = gapSize,
        enabled = enabled,
    )

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
) =
    SegmentedCircularProgressIndicatorImpl(
        segmentParams = SegmentParams.Binary(segmentValue),
        modifier = modifier,
        segmentCount = segmentCount,
        startAngle = startAngle,
        endAngle = endAngle,
        colors = colors,
        strokeWidth = strokeWidth,
        gapSize = gapSize,
        enabled = enabled,
    )

@Composable
private fun SegmentedCircularProgressIndicatorImpl(
    segmentParams: SegmentParams,
    @IntRange(from = 1) segmentCount: Int,
    modifier: Modifier,
    startAngle: Float,
    endAngle: Float,
    colors: ProgressIndicatorColors,
    strokeWidth: Dp,
    gapSize: Dp,
    enabled: Boolean,
) {
    Spacer(
        modifier
            .clearAndSetSemantics {}
            .fillMaxSize()
            .drawWithCache {
                onDrawWithContent {
                    val fullSweep = 360f - ((startAngle - endAngle) % 360 + 360) % 360
                    val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                    val minSize = min(size.height, size.width)
                    // Sweep angle between two progress indicator segments.
                    val gapSweep =
                        asin((stroke.width + gapSize.toPx()) / (minSize - stroke.width))
                            .toDegrees() * 2f
                    val segmentSweepAngle =
                        if (segmentCount > 1) fullSweep / segmentCount - gapSweep else fullSweep

                    for (segment in 0 until segmentCount) {
                        val segmentStartAngle =
                            startAngle +
                                fullSweep * segment / segmentCount +
                                (if (segmentCount > 1) gapSweep / 2 else 0f)

                        when (segmentParams) {
                            is SegmentParams.Binary -> {
                                val color =
                                    if (segmentParams.segmentValue(segment))
                                        colors.indicatorBrush(enabled)
                                    else colors.trackBrush(enabled)

                                drawIndicatorSegment(
                                    startAngle = segmentStartAngle,
                                    sweep = segmentSweepAngle,
                                    gapSweep = 0f,
                                    brush = color,
                                    stroke = stroke
                                )
                            }
                            is SegmentParams.Progress -> {
                                val currentProgress = segmentParams.progress()
                                val coercedProgress =
                                    coerceProgress(currentProgress, segmentParams.allowOverflow)
                                val progressInSegments = segmentCount * coercedProgress
                                val hasOverflow =
                                    segmentParams.allowOverflow && currentProgress > 1.0f

                                if (segment >= floor(progressInSegments)) {
                                    drawIndicatorSegment(
                                        startAngle = segmentStartAngle,
                                        sweep = segmentSweepAngle,
                                        gapSweep = 0f, // Overlay, no gap
                                        brush =
                                            colors.trackBrush(
                                                enabled = enabled,
                                                hasOverflow = hasOverflow
                                            ),
                                        stroke = stroke
                                    )
                                }
                                if (segment < progressInSegments) {
                                    val progressSweepAngle =
                                        segmentSweepAngle *
                                            (progressInSegments - segment).coerceAtMost(1f)

                                    drawIndicatorSegment(
                                        startAngle = segmentStartAngle,
                                        sweep = progressSweepAngle,
                                        gapSweep = 0f, // Overlay, no gap
                                        brush = colors.indicatorBrush(enabled),
                                        stroke = stroke
                                    )
                                }
                            }
                        }
                    }
                }
            }
    )
}

private sealed interface SegmentParams {
    data class Binary(val segmentValue: (segmentIndex: Int) -> Boolean) : SegmentParams

    data class Progress(val progress: () -> Float, val allowOverflow: Boolean) : SegmentParams
}
