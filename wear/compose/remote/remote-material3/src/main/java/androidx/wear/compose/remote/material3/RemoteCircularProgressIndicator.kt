/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.wear.compose.remote.material3

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.solidColor
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.asRemoteDp
import androidx.compose.remote.creation.compose.state.asin
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.toDeg
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap

/**
 * Material Design circular progress indicator.
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteCircularProgressIndicatorSample
 *
 * For an animated progress, see:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteCircularProgressIndicatorAnimatedSample
 * @param progress The progress of this progress indicator where 0.0 represents no progress and 1.0
 *   represents completion.
 * @param modifier Modifier to be applied to the CircularProgressIndicator.
 * @param enabled controls the enabled state. When enabled is `false`, this component will appear
 *   visually disabled. Note that only solid colors are when [enabled] is an expression, otherwise
 *   it must be a constant.
 * @param startAngle The starting position of the progress arc, measured clockwise in degrees. For
 *   example, 0 is 3 o'clock.
 * @param endAngle The ending position of the progress arc.
 * @param colors [RemoteProgressIndicatorColors] that will be used to resolve the indicator and
 *   track color.
 * @param strokeWidth The stroke width for the progress indicator.
 * @param gapSize The size (in RemoteDp) of the gap between the ends of the progress indicator and
 *   the track. The stroke endcaps are not included in this distance.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RemoteComposable
@Composable
@Suppress("RestrictedApiAndroidX")
public fun RemoteCircularProgressIndicator(
    progress: RemoteFloat,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    startAngle: RemoteFloat = 270f.rf,
    endAngle: RemoteFloat = startAngle,
    colors: RemoteProgressIndicatorColors = RemoteProgressIndicatorDefaults.colors(),
    strokeWidth: RemoteDp = 8.rdp,
    gapSize: RemoteDp = RemoteProgressIndicatorDefaults.calculateRecommendedGapSize(strokeWidth),
) {
    RemoteCanvas(modifier = modifier.fillMaxSize()) {
        val fullSweep = 360f.rf - ((startAngle - endAngle) % 360f.rf + 360f.rf) % 360f.rf
        val sweepAngle = progress * fullSweep
        val strokePx = strokeWidth.toPx()
        val diameter = min(width, height)
        val diameterOffset = strokePx / 2f.rf
        val arcDimen = diameter - (diameterOffset * 2f.rf)

        val left = diameterOffset + (width - diameter) / 2f.rf
        val top = diameterOffset + (height - diameter) / 2f.rf
        val right = left + arcDimen
        val bottom = top + arcDimen

        // Track Background
        val trackPaint = RemotePaint {
            style = PaintingStyle.Stroke
            this.strokeWidth = strokePx
            strokeCap = StrokeCap.Round
            applyRemoteBrush(colors.trackBrush(enabled), size)
        }

        val gapSizePx = gapSize.toPx()

        // Sweep angle between two segments.
        val gapSweep = toDeg(asin((strokePx + gapSizePx) / (diameter - strokePx))) * 2f.rf

        drawArc(
            paint = trackPaint,
            startAngle = startAngle + sweepAngle + gapSweep / 2f.rf,
            sweepAngle = max(0f.rf, fullSweep - sweepAngle - gapSweep),
            useCenter = false,
            topLeft = RemoteOffset(left, top),
            size = RemoteSize(right - left, bottom - top),
        )

        // Progress Indicator
        val indicatorPaint = RemotePaint {
            style = PaintingStyle.Stroke
            this.strokeWidth = trackPaint.strokeWidth
            strokeCap = StrokeCap.Round
            applyRemoteBrush(colors.indicatorBrush(enabled), size)
        }

        drawArc(
            paint = indicatorPaint,
            startAngle = startAngle + gapSweep / 2f.rf,
            sweepAngle = max(0f.rf, sweepAngle - gapSweep),
            useCenter = false,
            topLeft = RemoteOffset(left, top),
            size = RemoteSize(right - left, bottom - top),
        )
    }
}

/** Contains defaults for Remote Progress Indicators. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteProgressIndicatorDefaults {
    /** Creates a [RemoteProgressIndicatorColors] with the default colors. */
    @Composable
    @RemoteComposable
    public fun colors(): RemoteProgressIndicatorColors = defaultProgressIndicatorColors()

    /** Returns recommended size of the gap based on `strokeWidth`. */
    public fun calculateRecommendedGapSize(strokeWidth: RemoteDp): RemoteDp =
        (strokeWidth.value * (1f.rf / 3f.rf)).asRemoteDp()

    /** Creates a [RemoteProgressIndicatorColors] with modified colors. */
    @Composable
    @RemoteComposable
    public fun colors(
        indicatorColor: RemoteColor? = null,
        trackColor: RemoteColor? = null,
        overflowTrackColor: RemoteColor? = null,
        disabledIndicatorColor: RemoteColor? = null,
        disabledTrackColor: RemoteColor? = null,
        disabledOverflowTrackColor: RemoteColor? = null,
    ): RemoteProgressIndicatorColors {
        val defaults = defaultProgressIndicatorColors()
        return RemoteProgressIndicatorColors(
            indicatorBrush =
                indicatorColor?.let { RemoteBrush.solidColor(it) } ?: defaults.indicatorBrush,
            trackBrush = trackColor?.let { RemoteBrush.solidColor(it) } ?: defaults.trackBrush,
            overflowTrackBrush =
                overflowTrackColor?.let { RemoteBrush.solidColor(it) }
                    ?: defaults.overflowTrackBrush,
            disabledIndicatorBrush =
                disabledIndicatorColor?.let { RemoteBrush.solidColor(it) }
                    ?: defaults.disabledIndicatorBrush,
            disabledTrackBrush =
                disabledTrackColor?.let { RemoteBrush.solidColor(it) }
                    ?: defaults.disabledTrackBrush,
            disabledOverflowTrackBrush =
                disabledOverflowTrackColor?.let { RemoteBrush.solidColor(it) }
                    ?: defaults.disabledOverflowTrackBrush,
        )
    }

    /** Creates a [RemoteProgressIndicatorColors] with modified brushes. */
    @Composable
    @RemoteComposable
    public fun colors(
        indicatorBrush: RemoteBrush? = null,
        trackBrush: RemoteBrush? = null,
        overflowTrackBrush: RemoteBrush? = null,
        disabledIndicatorBrush: RemoteBrush? = null,
        disabledTrackBrush: RemoteBrush? = null,
        disabledOverflowTrackBrush: RemoteBrush? = null,
    ): RemoteProgressIndicatorColors {
        val defaults = defaultProgressIndicatorColors()
        return RemoteProgressIndicatorColors(
            indicatorBrush = indicatorBrush ?: defaults.indicatorBrush,
            trackBrush = trackBrush ?: defaults.trackBrush,
            overflowTrackBrush = overflowTrackBrush ?: defaults.overflowTrackBrush,
            disabledIndicatorBrush = disabledIndicatorBrush ?: defaults.disabledIndicatorBrush,
            disabledTrackBrush = disabledTrackBrush ?: defaults.disabledTrackBrush,
            disabledOverflowTrackBrush =
                disabledOverflowTrackBrush ?: defaults.disabledOverflowTrackBrush,
        )
    }

    @Composable
    @RemoteComposable
    private fun defaultProgressIndicatorColors(): RemoteProgressIndicatorColors {
        val colorScheme = RemoteMaterialTheme.colorScheme
        return RemoteProgressIndicatorColors(
            indicatorBrush = RemoteBrush.solidColor(colorScheme.primary),
            trackBrush = RemoteBrush.solidColor(colorScheme.surfaceContainer),
            overflowTrackBrush = RemoteBrush.solidColor(colorScheme.primary.copy(alpha = 0.6f.rf)),
            disabledIndicatorBrush =
                RemoteBrush.solidColor(colorScheme.onSurface.toDisabledColor(0.38f.rf)),
            disabledTrackBrush =
                RemoteBrush.solidColor(colorScheme.onSurface.toDisabledColor(0.12f.rf)),
            disabledOverflowTrackBrush =
                RemoteBrush.solidColor(
                    colorScheme.primary.copy(alpha = 0.6f.rf).toDisabledColor(0.12f.rf)
                ),
        )
    }
}

/** Represents the indicator and track colors used in progress indicator in a remote context. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteProgressIndicatorColors(
    public val indicatorBrush: RemoteBrush,
    public val trackBrush: RemoteBrush,
    public val overflowTrackBrush: RemoteBrush,
    public val disabledIndicatorBrush: RemoteBrush,
    public val disabledTrackBrush: RemoteBrush,
    public val disabledOverflowTrackBrush: RemoteBrush,
) {
    /**
     * Represents the indicator color, depending on [enabled].
     *
     * @param enabled whether the component is enabled.
     */
    internal fun indicatorBrush(enabled: RemoteBoolean): RemoteBrush =
        resolveRemoteBrush(enabled, indicatorBrush, disabledIndicatorBrush)

    /**
     * Represents the track color, depending on [enabled].
     *
     * @param enabled whether the component is enabled.
     */
    internal fun trackBrush(enabled: RemoteBoolean): RemoteBrush =
        resolveRemoteBrush(enabled, trackBrush, disabledTrackBrush)

    /**
     * Represents the animated overflow track color.
     *
     * @param enabled whether the component is enabled.
     */
    internal fun overflowTrackBrush(enabled: RemoteBoolean): RemoteBrush =
        resolveRemoteBrush(enabled, overflowTrackBrush, disabledOverflowTrackBrush)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is RemoteProgressIndicatorColors) return false

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

    /** Returns the resolved brush for the given [enabled] state. */
    private fun resolveRemoteBrush(
        enabled: RemoteBoolean,
        enabledBrush: RemoteBrush,
        disableBrush: RemoteBrush,
    ): RemoteBrush {
        // Always returns the enabled brush if enabled is not constant
        return enabled.constantValueOrNull?.let { isEnabled ->
            if (isEnabled) enabledBrush else disableBrush
        } ?: enabledBrush
    }
}
