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

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.tokens.ColorSchemeKeyTokens
import androidx.wear.compose.materialcore.toRadians
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Material Design circular progress indicator.
 *
 * Example of a full screen [CircularProgressIndicator]. Note that the padding
 * [ProgressIndicatorDefaults.FullScreenPadding] should be applied:
 *
 * @sample androidx.wear.compose.material3.samples.FullScreenProgressIndicatorSample
 *
 * Example of progress showing overflow value (more than 1) by [CircularProgressIndicator]:
 *
 * @sample androidx.wear.compose.material3.samples.OverflowProgressIndicatorSample
 *
 * Example of progress indicator wrapping media control by [CircularProgressIndicator]:
 *
 * @sample androidx.wear.compose.material3.samples.MediaButtonProgressIndicatorSample
 *
 * Example of a [CircularProgressIndicator] with small progress values:
 *
 * @sample androidx.wear.compose.material3.samples.SmallValuesProgressIndicatorSample
 *
 * Progress indicators express the proportion of completion of an ongoing task.
 *
 * @param progress The progress of this progress indicator where 0.0 represents no progress and 1.0
 *   represents completion. Values outside of this range are coerced into the range 0..1.
 * @param modifier Modifier to be applied to the CircularProgressIndicator.
 * @param startAngle The starting position of the progress arc, measured clockwise in degrees (0
 *   to 360) from the 3 o'clock position. For example, 0 and 360 represent 3 o'clock, 90 and 180
 *   represent 6 o'clock and 9 o'clock respectively. Default is 270 degrees
 *   [ProgressIndicatorDefaults.StartAngle] (top of the screen).
 * @param endAngle The ending position of the progress arc, measured clockwise in degrees (0 to 360)
 *   from the 3 o'clock position. For example, 0 and 360 represent 3 o'clock, 90 and 180 represent 6
 *   o'clock and 9 o'clock respectively. By default equal to [startAngle].
 * @param colors [ProgressIndicatorColors] that will be used to resolve the indicator and track
 *   color for this progress indicator in different states.
 * @param strokeWidth The stroke width for the progress indicator.
 * @param gapSize The space left between the ends of the progress indicator and the track (in Dp).
 */
@Composable
fun CircularProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    startAngle: Float = ProgressIndicatorDefaults.StartAngle,
    endAngle: Float = startAngle,
    colors: ProgressIndicatorColors = ProgressIndicatorDefaults.colors(),
    strokeWidth: Dp = ProgressIndicatorDefaults.StrokeWidth,
    gapSize: Dp = ProgressIndicatorDefaults.gapSize(strokeWidth),
) {
    val coercedProgress = { progress().coerceIn(0f, 1f) }
    // Canvas internally uses Spacer.drawBehind.
    // Using Spacer.drawWithCache to optimize the stroke allocations.
    Spacer(
        modifier
            .clearAndSetSemantics {}
            .fillMaxSize()
            .focusable()
            .drawWithCache {
                val fullSweep = 360f - ((startAngle - endAngle) % 360 + 360) % 360
                var progressSweep = fullSweep * coercedProgress()
                val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                val minSize = min(size.height, size.width)
                // Sweep angle between two progress indicator segments.
                val gapSweep =
                    asin((stroke.width + gapSize.toPx()) / (minSize - stroke.width)).toDegrees() *
                        2f

                if (progressSweep > 0) {
                    progressSweep = max(progressSweep, gapSweep)
                }

                onDrawWithContent {
                    // Draw an indicator.
                    drawIndicatorSegment(
                        startAngle = startAngle,
                        sweep = progressSweep,
                        gapSweep = gapSweep,
                        brush = colors.indicatorBrush,
                        stroke = stroke
                    )

                    // Draw a background.
                    drawIndicatorSegment(
                        startAngle = startAngle + progressSweep,
                        sweep = fullSweep - progressSweep,
                        gapSweep = gapSweep,
                        brush = colors.trackBrush,
                        stroke = stroke
                    )
                }
            }
    )
}

/** Contains defaults for Progress Indicators. */
object ProgressIndicatorDefaults {
    /**
     * The default stroke width for a circular progress indicator. For example, you can apply this
     * value when drawn around an [IconButton] with size [IconButtonDefaults.DefaultButtonSize].
     *
     * This can be customized with `strokeWidth` parameter on [CircularProgressIndicator].
     */
    val ButtonCircularIndicatorStrokeWidth = 6.dp

    /**
     * The recommended stroke width when used for default and large size circular progress
     * indicators.
     *
     * This can be customized with `strokeWidth` parameter on [CircularProgressIndicator].
     */
    val StrokeWidth = 18.dp

    /**
     * The default angle used for the start of the progress indicator arc.
     *
     * This can be customized with `startAngle` parameter on [CircularProgressIndicator].
     */
    val StartAngle = 270f

    /**
     * Returns recommended size of the gap based on `strokeWidth`.
     *
     * The absolute value can be customized with `gapSize` parameter on [CircularProgressIndicator].
     */
    fun gapSize(strokeWidth: Dp): Dp = strokeWidth / 3f

    /** Padding used for displaying [CircularProgressIndicator] full screen. */
    val FullScreenPadding = 2.dp

    /**
     * Creates a [ProgressIndicatorColors] that represents the default arc colors used in a
     * [CircularProgressIndicator].
     */
    @Composable fun colors() = MaterialTheme.colorScheme.defaultProgressIndicatorColors

    /**
     * Creates a [ProgressIndicatorColors] with modified colors used in a
     * [CircularProgressIndicator].
     *
     * @param indicatorColor The indicator arc color.
     * @param trackColor The track arc color.
     */
    @Composable
    fun colors(indicatorColor: Color = Color.Unspecified, trackColor: Color = Color.Unspecified) =
        MaterialTheme.colorScheme.defaultProgressIndicatorColors.copy(
            indicatorColor = indicatorColor,
            trackColor = trackColor
        )

    /**
     * Creates a [ProgressIndicatorColors] with modified brushes used to draw arcs in a
     * [CircularProgressIndicator].
     *
     * @param indicatorBrush The brush used to draw indicator arc.
     * @param trackBrush The brush used to draw track arc.
     */
    @Composable
    fun colors(indicatorBrush: Brush? = null, trackBrush: Brush? = null) =
        MaterialTheme.colorScheme.defaultProgressIndicatorColors.copy(
            indicatorBrush = indicatorBrush,
            trackBrush = trackBrush
        )

    private val ColorScheme.defaultProgressIndicatorColors: ProgressIndicatorColors
        get() {
            return defaultProgressIndicatorColorsCached
                ?: ProgressIndicatorColors(
                        indicatorBrush = SolidColor(fromToken(ColorSchemeKeyTokens.Primary)),
                        trackBrush = SolidColor(fromToken(ColorSchemeKeyTokens.SurfaceContainer)),
                    )
                    .also { defaultProgressIndicatorColorsCached = it }
        }
}

/**
 * Represents the indicator and track colors used in progress indicator.
 *
 * @param indicatorBrush [Brush] used to draw the indicator arc of progress indicator.
 * @param trackBrush [Brush] used to draw the track arc of progress indicator.
 */
class ProgressIndicatorColors(val indicatorBrush: Brush, val trackBrush: Brush) {
    internal fun copy(
        indicatorColor: Color = Color.Unspecified,
        trackColor: Color = Color.Unspecified,
    ) =
        ProgressIndicatorColors(
            indicatorBrush =
                if (indicatorColor.isSpecified) SolidColor(indicatorColor) else indicatorBrush,
            trackBrush = if (trackColor.isSpecified) SolidColor(trackColor) else trackBrush
        )

    internal fun copy(
        indicatorBrush: Brush? = null,
        trackBrush: Brush? = null,
    ) =
        ProgressIndicatorColors(
            indicatorBrush = indicatorBrush ?: this.indicatorBrush,
            trackBrush = trackBrush ?: this.trackBrush
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ProgressIndicatorColors) return false

        if (indicatorBrush != other.indicatorBrush) return false
        if (trackBrush != other.trackBrush) return false

        return true
    }

    override fun hashCode(): Int {
        var result = indicatorBrush.hashCode()
        result = 31 * result + trackBrush.hashCode()
        return result
    }
}

/**
 * Draws an arc for indicator segment leaving half of the `gapSweep` before each visual end.
 *
 * If indicator gets too small, the circle that proportionally scales down is drawn instead.
 */
internal fun DrawScope.drawIndicatorSegment(
    startAngle: Float,
    sweep: Float,
    gapSweep: Float,
    brush: Brush,
    stroke: Stroke
) {
    if (sweep <= gapSweep) {
        // Draw a small indicator.
        val angle = (startAngle + sweep / 2f).toRadians()
        val radius = size.minDimension / 2 - stroke.width / 2
        val circleRadius = (stroke.width / 2) * sweep / gapSweep
        val alpha = (circleRadius / stroke.width * 2f).coerceAtMost(1f)
        val brushWithAlpha =
            if (brush is SolidColor && alpha < 1f) {
                SolidColor(brush.value.copy(alpha = alpha))
            } else {
                brush
            }
        drawCircle(
            brushWithAlpha,
            circleRadius,
            center =
                Offset(
                    radius * cos(angle) + size.minDimension / 2,
                    radius * sin(angle) + size.minDimension / 2
                )
        )
    } else {
        // To draw this circle we need a rect with edges that line up with the midpoint of the
        // stroke.
        // To do this we need to remove half the stroke width from the total diameter for both
        // sides.
        val diameter = min(size.width, size.height)
        val diameterOffset = stroke.width / 2
        val arcDimen = diameter - 2 * diameterOffset
        drawArc(
            brush = brush,
            startAngle = startAngle + gapSweep / 2,
            sweepAngle = sweep - gapSweep,
            useCenter = false,
            topLeft =
                Offset(
                    diameterOffset + (size.width - diameter) / 2,
                    diameterOffset + (size.height - diameter) / 2
                ),
            size = Size(arcDimen, arcDimen),
            style = stroke
        )
    }
}
