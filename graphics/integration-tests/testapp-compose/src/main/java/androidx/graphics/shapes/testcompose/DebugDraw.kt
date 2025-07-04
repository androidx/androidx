/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.graphics.shapes.testcompose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.Feature
import kotlin.math.roundToInt

internal fun DrawScope.debugDrawCubic(bezier: Cubic, scheme: ColorScheme) {
    // Draw red circles for start and end.
    drawCircle(scheme.inverseSurface, radius = 6f, center = bezier.anchor0(), style = Stroke(2f))
    drawCircle(scheme.inverseSurface, radius = 8f, center = bezier.anchor1(), style = Stroke(2f))

    // Draw a circle for the first control point, and a line from start to it.
    // The curve will start in this direction
    drawLine(scheme.scrim, bezier.anchor0(), bezier.control0(), strokeWidth = 0f)
    drawCircle(scheme.scrim, radius = 4f, center = bezier.control0(), style = Stroke(2f))

    // Draw a circle for the second control point, and a line from it to the end.
    // The curve will end in this direction
    drawLine(scheme.scrim, bezier.control1(), bezier.anchor1(), strokeWidth = 0f)
    drawCircle(scheme.scrim, radius = 4f, center = bezier.control1(), style = Stroke(2f))

    // Draw dots along each curve
    var t = .1f
    while (t < 1f) {
        drawCircle(scheme.primary, radius = 2f, center = bezier.pointOnCurve(t), style = Stroke(2f))
        t += .1f
    }
}

internal fun DrawScope.debugDrawFeature(
    feature: Feature,
    colorScheme: FeatureColorScheme,
    backgroundColor: Color,
    radius: Float,
) {
    val color = featureToColor(feature, colorScheme)
    val representativePoint = featureRepresentativePoint(feature)

    // Draw a clickable circle for the representative Point
    drawCircle(color, radius = radius, center = representativePoint, style = Fill)

    // With a bit of a background to suggest tapping is possible
    drawCircle(
        color.copy(0.2f),
        radius = radius + (radius * 0.6f),
        center = representativePoint,
        style = Fill,
    )

    // Finally add a border around the representative point
    drawCircle(
        backgroundColor,
        radius = radius,
        center = representativePoint,
        style = Stroke(radius / 4),
    )
}

@Composable
internal fun FeatureRepresentativePoint(
    modifier: Modifier = Modifier,
    feature: Feature,
    colorScheme: FeatureColorScheme,
    backgroundColor: Color,
    model: PanZoomRotateBoxState = PanZoomRotateBoxState(),
    pointSize: Dp = 15.dp,
    onClick: () -> Unit,
) {
    val radius = with(LocalDensity.current) { (pointSize / 2).roundToPx() }
    val position = model.mapOut(featureRepresentativePoint(feature))

    Box(
        modifier
            .offset { IntOffset((position.x).roundToInt(), (position.y).roundToInt()) }
            .drawWithContent {
                drawContent()
                debugDrawFeature(feature, colorScheme, backgroundColor, radius.toFloat())
            }
    )

    Box(
        modifier
            .offset {
                IntOffset(
                    (position.x - CLICKABLE_SCALE * radius).roundToInt(),
                    (position.y - CLICKABLE_SCALE * radius).roundToInt(),
                )
            }
            .size(pointSize * CLICKABLE_SCALE)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    )
}

internal fun featureToColor(feature: Feature, scheme: FeatureColorScheme): Color =
    if (feature.isEdge) {
        scheme.edgeColor
    } else if (feature.isConvexCorner) {
        scheme.convexColor
    } else {
        scheme.concaveColor
    }

// TODO: b/378441547 - Remove if explicit / exposed by default
internal fun featureRepresentativePoint(feature: Feature): Offset =
    (feature.cubics.first().anchor0() + feature.cubics.last().anchor1()) / 2f

internal fun Cubic.anchor0() = Offset(anchor0X, anchor0Y)

internal fun Cubic.control0() = Offset(control0X, control0Y)

internal fun Cubic.control1() = Offset(control1X, control1Y)

internal fun Cubic.anchor1() = Offset(anchor1X, anchor1Y)

internal const val CLICKABLE_SCALE = 1.8f
