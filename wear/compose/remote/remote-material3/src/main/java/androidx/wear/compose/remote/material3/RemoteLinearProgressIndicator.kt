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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.wear.compose.remote.material3

import androidx.annotation.RestrictTo
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
import androidx.compose.remote.creation.compose.state.RemotePaintingStyle
import androidx.compose.remote.creation.compose.state.RemoteStrokeCap
import androidx.compose.remote.creation.compose.state.clamp
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * @sample androidx.wear.compose.remote.material3.samples.RemoteLinearProgressIndicatorSample
 * @sample androidx.wear.compose.remote.material3.samples.RemoteLinearProgressIndicatorAnimatedSample
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
    val coercedStrokeWidth = max(0.rdp, strokeWidth)
    // Coerce before animating, as Wear Material3 does, so an out of range value animates from the
    // end of the track instead of pausing there.
    val animatedProgress =
        animateProgress(
            clamp(progress, 0f.rf, 1f.rf),
            RemoteMaterialTheme.motionScheme.defaultEffectsSpec(),
        )

    RemoteCanvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(coercedStrokeWidth)
                .scale(scaleX = if (isRtl) (-1f).rf else 1f.rf, scaleY = 1f.rf)
    ) {
        drawLinearIndicator(animatedProgress, coercedStrokeWidth, colors, enabled)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteLinearProgressIndicatorDefaults {

    public val StrokeWidthLarge: RemoteDp = 12.rdp

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
    val isZeroStroke = strokeWidth.isLessThanOrEqualTo(0.rdp)
    val strokePx = strokeWidth.toPx()
    val strokeCapOffset = strokePx / 2f.rf
    val yOffset = height / 2f.rf
    val clampedProgress = clamp(progress, 0f.rf, 1f.rf)
    val availableWidth = max(0f.rf, width - strokePx - horizontalMargin * 2f.rf)
    val progressPx = clampedProgress * availableWidth

    val trackStart = strokeCapOffset + horizontalMargin
    val trackEnd = width - strokeCapOffset - horizontalMargin

    // Track Background
    val trackPaint = RemotePaint {
        style = RemotePaintingStyle.Stroke
        this.strokeWidth = strokePx
        strokeCap = RemoteStrokeCap.Round
        with(colors.trackBrush(enabled)) { applyTo(this@RemotePaint, size) }
        this.color = isZeroStroke.select(Color.Transparent.rc, this.color)
    }
    drawLine(
        paint = trackPaint,
        start = RemoteOffset(trackStart, yOffset),
        end = RemoteOffset(trackEnd, yOffset),
    )

    // Indicator Foreground
    val isZero = isZeroStroke.select(true.rb, clampedProgress.isLessThan(0.001f.rf))
    val indicatorPaint = RemotePaint {
        style = RemotePaintingStyle.Stroke
        this.strokeWidth = strokePx
        strokeCap = RemoteStrokeCap.Round
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

    val scaleFraction = clamp(distanceFromProgressToDot / dotMargin, 0f.rf, 1f.rf)
    val scaledDotRadius = dotRadius * scaleFraction
    val isDotHidden = isZeroStroke.select(true.rb, scaleFraction.isLessThan(0.01f.rf))

    val dotPaint = RemotePaint {
        style = RemotePaintingStyle.Fill
        with(colors.indicatorBrush(enabled)) { applyTo(this@RemotePaint, size) }
        this.color = isDotHidden.select(Color.Transparent.rc, this.color)
    }
    drawCircle(
        paint = dotPaint,
        radius = scaledDotRadius,
        center = RemoteOffset(dotCenterX, dotCenterY),
    )
}
