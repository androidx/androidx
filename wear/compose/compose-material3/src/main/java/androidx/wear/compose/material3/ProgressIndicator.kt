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
import kotlin.math.sin

/** Contains defaults for Progress Indicators. */
object ProgressIndicatorDefaults {
    /** Creates a [ProgressIndicatorColors] with the default colors. */
    @Composable fun colors() = MaterialTheme.colorScheme.defaultProgressIndicatorColors

    /**
     * Creates a [ProgressIndicatorColors] with modified colors used in [CircularProgressIndicator]
     * and [LinearProgressIndicator].
     *
     * @param indicatorColor The indicator color.
     * @param trackColor The track color.
     * @param disabledIndicatorColor The disabled indicator color.
     * @param disabledTrackColor The disabled track color.
     */
    @Composable
    fun colors(
        indicatorColor: Color = Color.Unspecified,
        trackColor: Color = Color.Unspecified,
        disabledIndicatorColor: Color = Color.Unspecified,
        disabledTrackColor: Color = Color.Unspecified,
    ) =
        MaterialTheme.colorScheme.defaultProgressIndicatorColors.copy(
            indicatorColor = indicatorColor,
            trackColor = trackColor,
            disabledIndicatorColor = disabledIndicatorColor,
            disabledTrackColor = disabledTrackColor,
        )

    /**
     * Creates a [ProgressIndicatorColors] with modified brushes used in [CircularProgressIndicator]
     * and [LinearProgressIndicator].
     *
     * @param indicatorBrush [Brush] used to draw indicator.
     * @param trackBrush [Brush] used to draw track.
     * @param disabledIndicatorBrush [Brush] used to draw the indicator if the progress is disabled.
     * @param disabledTrackBrush [Brush] used to draw the track if the progress is disabled.
     */
    @Composable
    fun colors(
        indicatorBrush: Brush? = null,
        trackBrush: Brush? = null,
        disabledIndicatorBrush: Brush? = null,
        disabledTrackBrush: Brush? = null,
    ) =
        MaterialTheme.colorScheme.defaultProgressIndicatorColors.copy(
            indicatorBrush = indicatorBrush,
            trackBrush = trackBrush,
            disabledIndicatorBrush = disabledIndicatorBrush,
            disabledTrackBrush = disabledTrackBrush,
        )

    private val ColorScheme.defaultProgressIndicatorColors: ProgressIndicatorColors
        get() {
            return defaultProgressIndicatorColorsCached
                ?: ProgressIndicatorColors(
                        indicatorBrush = SolidColor(fromToken(ColorSchemeKeyTokens.Primary)),
                        trackBrush = SolidColor(fromToken(ColorSchemeKeyTokens.SurfaceContainer)),
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
                    )
                    .also { defaultProgressIndicatorColorsCached = it }
        }
}

/**
 * Represents the indicator and track colors used in progress indicator.
 *
 * @param indicatorBrush [Brush] used to draw the indicator of progress indicator.
 * @param trackBrush [Brush] used to draw the track of progress indicator.
 * @param disabledIndicatorBrush [Brush] used to draw the indicator if the component is disabled.
 * @param disabledTrackBrush [Brush] used to draw the track if the component is disabled.
 */
class ProgressIndicatorColors(
    val indicatorBrush: Brush,
    val trackBrush: Brush,
    val disabledIndicatorBrush: Brush = indicatorBrush,
    val disabledTrackBrush: Brush = disabledIndicatorBrush,
) {
    internal fun copy(
        indicatorColor: Color = Color.Unspecified,
        trackColor: Color = Color.Unspecified,
        disabledIndicatorColor: Color = Color.Unspecified,
        disabledTrackColor: Color = Color.Unspecified,
    ) =
        ProgressIndicatorColors(
            indicatorBrush =
                if (indicatorColor.isSpecified) SolidColor(indicatorColor) else indicatorBrush,
            trackBrush = if (trackColor.isSpecified) SolidColor(trackColor) else trackBrush,
            disabledIndicatorBrush =
                if (disabledIndicatorColor.isSpecified) SolidColor(disabledIndicatorColor)
                else disabledIndicatorBrush,
            disabledTrackBrush =
                if (disabledTrackColor.isSpecified) SolidColor(disabledTrackColor)
                else disabledTrackBrush,
        )

    internal fun copy(
        indicatorBrush: Brush? = null,
        trackBrush: Brush? = null,
        disabledIndicatorBrush: Brush? = null,
        disabledTrackBrush: Brush? = null,
    ) =
        ProgressIndicatorColors(
            indicatorBrush = indicatorBrush ?: this.indicatorBrush,
            trackBrush = trackBrush ?: this.trackBrush,
            disabledIndicatorBrush = disabledIndicatorBrush ?: this.disabledIndicatorBrush,
            disabledTrackBrush = disabledTrackBrush ?: this.disabledTrackBrush,
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
     * Represents the track color, depending on [enabled].
     *
     * @param enabled whether the component is enabled.
     */
    internal fun trackBrush(enabled: Boolean): Brush {
        return if (enabled) trackBrush else disabledTrackBrush
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ProgressIndicatorColors) return false

        if (indicatorBrush != other.indicatorBrush) return false
        if (trackBrush != other.trackBrush) return false
        if (disabledIndicatorBrush != other.disabledIndicatorBrush) return false
        if (disabledTrackBrush != other.disabledTrackBrush) return false

        return true
    }

    override fun hashCode(): Int {
        var result = indicatorBrush.hashCode()
        result = 31 * result + trackBrush.hashCode()
        result = 31 * result + disabledIndicatorBrush.hashCode()
        result = 31 * result + disabledTrackBrush.hashCode()
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
