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

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.wear.compose.material3.tokens.ColorSchemeKeyTokens
import androidx.wear.compose.materialcore.toRadians
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin

/** Contains defaults for Progress Indicators. */
object ProgressIndicatorDefaults {
    /** Creates a [ProgressIndicatorColors] with the default colors. */
    @Composable fun colors() = MaterialTheme.colorScheme.defaultProgressIndicatorColors

    /**
     * Creates a [ProgressIndicatorColors] with modified colors.
     *
     * @param indicatorColor The indicator color.
     * @param trackColor The track color.
     * @param overflowTrackColor The overflow track color.
     * @param disabledIndicatorColor The disabled indicator color.
     * @param disabledTrackColor The disabled track color.
     * @param disabledOverflowTrackColor The disabled overflow track color.
     */
    @Composable
    fun colors(
        indicatorColor: Color = Color.Unspecified,
        trackColor: Color = Color.Unspecified,
        overflowTrackColor: Color = Color.Unspecified,
        disabledIndicatorColor: Color = Color.Unspecified,
        disabledTrackColor: Color = Color.Unspecified,
        disabledOverflowTrackColor: Color = Color.Unspecified,
    ) =
        MaterialTheme.colorScheme.defaultProgressIndicatorColors.copy(
            indicatorColor = indicatorColor,
            trackColor = trackColor,
            overflowTrackColor = overflowTrackColor,
            disabledIndicatorColor = disabledIndicatorColor,
            disabledTrackColor = disabledTrackColor,
            disabledOverflowTrackColor = disabledOverflowTrackColor,
        )

    /**
     * Creates a [ProgressIndicatorColors] with modified brushes.
     *
     * @param indicatorBrush [Brush] used to draw indicator.
     * @param trackBrush [Brush] used to draw track.
     * @param overflowTrackBrush [Brush] used to draw track for progress overflow.
     * @param disabledIndicatorBrush [Brush] used to draw the indicator if the progress is disabled.
     * @param disabledTrackBrush [Brush] used to draw the track if the progress is disabled.
     * @param disabledOverflowTrackBrush [Brush] used to draw the overflow track if the progress is
     *   disabled.
     */
    @Composable
    fun colors(
        indicatorBrush: Brush? = null,
        trackBrush: Brush? = null,
        overflowTrackBrush: Brush? = null,
        disabledIndicatorBrush: Brush? = null,
        disabledTrackBrush: Brush? = null,
        disabledOverflowTrackBrush: Brush? = null,
    ) =
        MaterialTheme.colorScheme.defaultProgressIndicatorColors.copy(
            indicatorBrush = indicatorBrush,
            trackBrush = trackBrush,
            overflowTrackBrush = overflowTrackBrush,
            disabledIndicatorBrush = disabledIndicatorBrush,
            disabledTrackBrush = disabledTrackBrush,
            disabledOverflowTrackBrush = disabledOverflowTrackBrush
        )

    // TODO(b/364538891): add color and alpha tokens for ProgressIndicator
    private const val OverflowTrackColorAlpha = 0.6f

    private val ColorScheme.defaultProgressIndicatorColors: ProgressIndicatorColors
        get() {
            return defaultProgressIndicatorColorsCached
                ?: ProgressIndicatorColors(
                        indicatorBrush = SolidColor(fromToken(ColorSchemeKeyTokens.Primary)),
                        trackBrush = SolidColor(fromToken(ColorSchemeKeyTokens.SurfaceContainer)),
                        overflowTrackBrush =
                            SolidColor(
                                fromToken(ColorSchemeKeyTokens.Primary)
                                    .copy(alpha = OverflowTrackColorAlpha)
                            ),
                        disabledIndicatorBrush =
                            SolidColor(
                                fromToken(ColorSchemeKeyTokens.OnSurface)
                                    .toDisabledColor(disabledAlpha = DisabledContentAlpha)
                            ),
                        disabledTrackBrush =
                            SolidColor(
                                fromToken(ColorSchemeKeyTokens.OnSurface)
                                    .toDisabledColor(disabledAlpha = DisabledContainerAlpha)
                            ),
                        disabledOverflowTrackBrush =
                            SolidColor(
                                fromToken(ColorSchemeKeyTokens.Primary)
                                    .copy(alpha = OverflowTrackColorAlpha)
                                    .toDisabledColor(disabledAlpha = DisabledContainerAlpha)
                            )
                    )
                    .also { defaultProgressIndicatorColorsCached = it }
        }
}

/**
 * Represents the indicator and track colors used in progress indicator.
 *
 * @param indicatorBrush [Brush] used to draw the indicator of progress indicator.
 * @param trackBrush [Brush] used to draw the track of progress indicator.
 * @param overflowTrackBrush [Brush] used to draw the track for progress overflow (>100%).
 * @param disabledIndicatorBrush [Brush] used to draw the indicator if the component is disabled.
 * @param disabledTrackBrush [Brush] used to draw the track if the component is disabled.
 * @param disabledOverflowTrackBrush [Brush] used to draw the track if the component is disabled.
 */
class ProgressIndicatorColors(
    val indicatorBrush: Brush,
    val trackBrush: Brush,
    val overflowTrackBrush: Brush,
    val disabledIndicatorBrush: Brush,
    val disabledTrackBrush: Brush,
    val disabledOverflowTrackBrush: Brush,
) {
    internal fun copy(
        indicatorColor: Color = Color.Unspecified,
        trackColor: Color = Color.Unspecified,
        overflowTrackColor: Color = Color.Unspecified,
        disabledIndicatorColor: Color = Color.Unspecified,
        disabledTrackColor: Color = Color.Unspecified,
        disabledOverflowTrackColor: Color = Color.Unspecified,
    ) =
        ProgressIndicatorColors(
            indicatorBrush =
                if (indicatorColor.isSpecified) SolidColor(indicatorColor) else indicatorBrush,
            trackBrush = if (trackColor.isSpecified) SolidColor(trackColor) else trackBrush,
            overflowTrackBrush =
                if (overflowTrackColor.isSpecified) SolidColor(overflowTrackColor)
                else overflowTrackBrush,
            disabledIndicatorBrush =
                if (disabledIndicatorColor.isSpecified) SolidColor(disabledIndicatorColor)
                else disabledIndicatorBrush,
            disabledTrackBrush =
                if (disabledTrackColor.isSpecified) SolidColor(disabledTrackColor)
                else disabledTrackBrush,
            disabledOverflowTrackBrush =
                if (disabledOverflowTrackColor.isSpecified) SolidColor(disabledOverflowTrackColor)
                else disabledOverflowTrackBrush,
        )

    internal fun copy(
        indicatorBrush: Brush? = null,
        trackBrush: Brush? = null,
        overflowTrackBrush: Brush? = null,
        disabledIndicatorBrush: Brush? = null,
        disabledTrackBrush: Brush? = null,
        disabledOverflowTrackBrush: Brush? = null,
    ) =
        ProgressIndicatorColors(
            indicatorBrush = indicatorBrush ?: this.indicatorBrush,
            trackBrush = trackBrush ?: this.trackBrush,
            overflowTrackBrush = overflowTrackBrush ?: this.overflowTrackBrush,
            disabledIndicatorBrush = disabledIndicatorBrush ?: this.disabledIndicatorBrush,
            disabledTrackBrush = disabledTrackBrush ?: this.disabledTrackBrush,
            disabledOverflowTrackBrush =
                disabledOverflowTrackBrush ?: this.disabledOverflowTrackBrush,
        )

    /**
     * Represents the indicator color, depending on [enabled].
     *
     * @param enabled whether the component is enabled.
     */
    internal fun indicatorBrush(enabled: Boolean): Brush {
        return if (enabled) indicatorBrush else disabledIndicatorBrush
    }

    /**
     * Represents the track color, depending on [enabled] and [hasOverflow] parameters.
     *
     * @param enabled whether the component is enabled.
     * @param enabled whether the progress has overflow.
     */
    internal fun trackBrush(enabled: Boolean, hasOverflow: Boolean = false): Brush {
        return if (enabled) {
            if (hasOverflow) overflowTrackBrush else trackBrush
        } else {
            if (hasOverflow) disabledOverflowTrackBrush else disabledTrackBrush
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ProgressIndicatorColors) return false

        if (indicatorBrush != other.indicatorBrush) return false
        if (trackBrush != other.trackBrush) return false
        if (overflowTrackBrush != other.overflowTrackBrush) return false
        if (disabledIndicatorBrush != other.disabledIndicatorBrush) return false
        if (disabledTrackBrush != other.disabledTrackBrush) return false
        if (disabledOverflowTrackBrush != other.disabledOverflowTrackBrush) return false

        return true
    }

    override fun hashCode(): Int {
        var result = indicatorBrush.hashCode()
        result = 31 * result + trackBrush.hashCode()
        result = 31 * result + overflowTrackBrush.hashCode()
        result = 31 * result + disabledIndicatorBrush.hashCode()
        result = 31 * result + disabledTrackBrush.hashCode()
        result = 31 * result + disabledOverflowTrackBrush.hashCode()
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

/**
 * Coerce a [Float] progress value to [0.0..1.0] range.
 *
 * If overflow is enabled, truncate overflow values larger than 1.0 to only the fractional part.
 * Integer values larger than 0.0 always return 1.0 (full progress) and negative values are coerced
 * to 0.0. For example: 1.2 will be return 0.2, and 2.0 will return 1.0. If overflow is disabled,
 * simply coerce all values to [0.0..1.0] range. For example, 1.2 and 2.0 will both return 1.0.
 *
 * @param progress The progress value to be coerced to [0.0..1.0] range.
 * @param allowProgressOverflow If overflow is allowed.
 */
internal fun coerceProgress(progress: Float, allowProgressOverflow: Boolean): Float {
    if (!allowProgressOverflow) return progress.coerceIn(0f, 1f)
    if (progress <= 0.0f) return 0.0f
    if (progress <= 1.0f) return progress

    val fraction = progress % 1.0f
    // Round to 5 decimals to avoid floating point errors.
    val roundedFraction = round(fraction * 100000f) / 100000f
    return if (roundedFraction == 0.0f) 1.0f else roundedFraction
}
