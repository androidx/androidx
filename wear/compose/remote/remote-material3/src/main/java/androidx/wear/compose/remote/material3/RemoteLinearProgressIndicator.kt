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

package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.scale
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Material Design linear progress indicator.
 *
 * The [RemoteLinearProgressIndicator] displays progress as a horizontal bar, consisting of two
 * visual components:
 * - Track: The background line representing the total range of progress.
 * - Indicator: A colored line that fills the track, indicating the current progress value.
 *
 * The indicator also includes a small dot at the end of the progress line. This dot serves as an
 * accessibility feature to show the range of the indicator.
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteLinearProgressIndicatorSample
 *
 * For an animated progress, see:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteLinearProgressIndicatorAnimatedSample
 * @param progress The progress of this progress indicator where 0.0 represents no progress and 1.0
 *   represents completion. Values outside of this range are coerced into the range 0..1.
 * @param modifier Modifier to be applied to the [RemoteLinearProgressIndicator].
 * @param colors [RemoteProgressIndicatorColors] that will be used to resolve the indicator and
 *   track colors for this progress indicator in different states.
 * @param strokeWidth The stroke width for the progress indicator. Defaults to
 *   [RemoteLinearProgressIndicatorDefaults.StrokeWidthLarge].
 * @param enabled Controls the enabled state. Although this component is not clickable, it can be
 *   contained within a clickable component. When enabled is `false`, this component will appear
 *   visually disabled. Note that only constant values are currently supported for [enabled];
 *   expressions will evaluate to true.
 */
@RemoteComposable
@Composable
public fun RemoteLinearProgressIndicator(
    progress: RemoteFloat,
    modifier: RemoteModifier = RemoteModifier,
    colors: RemoteProgressIndicatorColors = RemoteProgressIndicatorDefaults.colors(),
    strokeWidth: RemoteDp = RemoteLinearProgressIndicatorDefaults.StrokeWidthLarge,
    enabled: RemoteBoolean = true.rb,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    RemoteCanvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(strokeWidth)
                .scale(scaleX = if (isRtl) (-1f).rf else 1f.rf, scaleY = 1f.rf)
    ) {
        drawLinearIndicator(progress, strokeWidth, colors, enabled)
    }
}

/** Contains defaults for Linear Progress Indicator. */
public object RemoteLinearProgressIndicatorDefaults {

    /**
     * Large stroke width for [RemoteLinearProgressIndicator].
     *
     * This is also the default stroke width for [RemoteLinearProgressIndicator].
     */
    public val StrokeWidthLarge: RemoteDp = 12.rdp

    /**
     * Small stroke width for [RemoteLinearProgressIndicator].
     *
     * This is the minimum recommended stroke value for [RemoteLinearProgressIndicator] to ensure
     * that the dot shown at the end of the range can be distinguished.
     */
    public val StrokeWidthSmall: RemoteDp = 8.rdp

    /** Radius for the dot shown at the end of the [RemoteLinearProgressIndicator]. */
    internal val DotRadius: RemoteDp = 2.rdp

    /** Margin for the dot shown at the end of the [RemoteLinearProgressIndicator]. */
    internal val DotMargin: RemoteDp = 4.rdp

    /** Horizontal padding for the [RemoteLinearProgressIndicator]. */
    internal val OuterHorizontalMargin: RemoteDp = 2.rdp
}

private fun RemoteDrawScope.drawLinearIndicator(
    progress: RemoteFloat,
    strokeWidth: RemoteDp,
    colors: RemoteProgressIndicatorColors,
    enabled: RemoteBoolean,
) {
    // Note: When padding is applied to RemoteCanvas, RemoteDrawScope.width reflects the unpadded
    // canvas width while drawing coordinates are offset by the padding, causing the right rounded
    // stroke cap to exceed the canvas boundary and get clipped.
    // Instead, we handle the horizontal margin directly within the drawing coordinates.
    // TODO: b/553472139 - RemoteCanvas with padding modifier causes RemoteDrawScope drawing to be
    // clipped.
    val horizontalMargin = RemoteLinearProgressIndicatorDefaults.OuterHorizontalMargin.toPx()
    val strokePx = strokeWidth.toPx()
    val strokeCapOffset = strokePx / 2f.rf
    val yOffset = height / 2f.rf
    val clampedProgress = min(1f.rf, max(0f.rf, progress))
    val availableWidth = max(0f.rf, width - strokePx - horizontalMargin * 2f.rf)
    val progressPx = clampedProgress * availableWidth

    val trackStart = strokeCapOffset + horizontalMargin
    val trackEnd = width - strokeCapOffset - horizontalMargin

    // Track Background
    val trackPaint = RemotePaint {
        style = PaintingStyle.Stroke
        this.strokeWidth = strokePx
        strokeCap = StrokeCap.Round
        with(colors.trackBrush(enabled)) { applyTo(this@RemotePaint, size) }
    }
    drawLine(
        paint = trackPaint,
        start = RemoteOffset(trackStart, yOffset),
        end = RemoteOffset(trackEnd, yOffset),
    )

    // Indicator Foreground
    val isZero = clampedProgress.isLessThan(0.001f.rf)
    val indicatorPaint = RemotePaint {
        style = PaintingStyle.Stroke
        this.strokeWidth = strokePx
        strokeCap = StrokeCap.Round
        with(colors.indicatorBrush(enabled)) { applyTo(this@RemotePaint, size) }
        this.color = isZero.select(Color.Transparent.rc, this.color)
    }
    drawLine(
        paint = indicatorPaint,
        start = RemoteOffset(trackStart, yOffset),
        end = RemoteOffset(trackStart + progressPx, yOffset),
    )

    // Dot at the end of the range
    val dotRadius = RemoteLinearProgressIndicatorDefaults.DotRadius.toPx()
    val dotMargin = RemoteLinearProgressIndicatorDefaults.DotMargin.toPx()
    val dotCenterX = width - horizontalMargin - dotRadius - dotMargin
    val dotCenterY = yOffset
    val distanceFromProgressToDot =
        dotCenterX - dotRadius - progressPx - strokeCapOffset * 2f.rf - horizontalMargin

    val scaleFraction = min(1f.rf, max(0f.rf, distanceFromProgressToDot / dotMargin))
    val scaledDotRadius = dotRadius * scaleFraction
    val isDotHidden = scaleFraction.isLessThan(0.01f.rf)

    val dotPaint = RemotePaint {
        style = PaintingStyle.Fill
        with(colors.indicatorBrush(enabled)) { applyTo(this@RemotePaint, size) }
        this.color = isDotHidden.select(Color.Transparent.rc, this.color)
    }
    drawCircle(
        paint = dotPaint,
        radius = scaledDotRadius,
        center = RemoteOffset(dotCenterX, dotCenterY),
    )
}
