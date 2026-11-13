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

package androidx.glance.adaptive.appwidget.components.ring

import androidx.annotation.FloatRange
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.widthIn
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemotePaintingStyle
import androidx.compose.remote.creation.compose.state.RemoteStrokeCap
import androidx.compose.remote.creation.compose.state.asRdp
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.adaptive.appwidget.components.RemoteComposeFonts
import androidx.glance.adaptive.appwidget.components.RemoteComposePalette
import androidx.glance.adaptive.appwidget.components.RemoteVectorGlyph
import androidx.glance.adaptive.appwidget.components.SlotText
import androidx.glance.adaptive.appwidget.components.fitMetric
import androidx.glance.adaptive.core.slots.ContainedRing
import androidx.glance.adaptive.core.slots.Ring

/** Vertical gap between the glyph and the value inside a contained ring. */
private const val CONTAINED_RING_VALUE_TOP_SPACE_DP = 2f

/** 12 o'clock, the angle progress starts from. Canvas angles are measured from 3 o'clock. */
private const val RING_START_ANGLE_DEGREES = -90f

/** Sweep of a full turn, in degrees. */
private const val FULL_SWEEP_DEGREES = 360f

/**
 * Draws the ring for the leading slot, [size] across.
 *
 * @param size Edge length the matrix gives the ring.
 */
@RemoteComposable
@Composable
internal fun Ring.Content(size: Dp) {
    RemoteBox(
        modifier = RemoteModifier.size(size.asRdp()),
        contentAlignment = RemoteAlignment.Center,
    ) {
        ProgressRingCanvas(
            progress = progress,
            size = size,
            containerColor = RemoteComposePalette.secondaryContainer,
        )
        icon?.let { iconRes ->
            RemoteVectorGlyph(
                icon = iconRes,
                size = (size.value * RingDefaults.IconRatio).dp,
                tint = RemoteComposePalette.primary,
            )
        }
    }
}

/**
 * Draws the ring for the bulb slot, in a circle [containerSize] across.
 *
 * The circle is drawn by the component rather than by rounding the widget's own bounds: an oblong
 * box rounded to half its shorter axis keeps a flat run along its longer one, which reads as a
 * circle with its top and bottom sliced off.
 *
 * @param containerSize Diameter of the circle the bulb gives the ring.
 */
@RemoteComposable
@Composable
internal fun ContainedRing.Content(containerSize: Dp) {
    val geometry = RingDefaults.containedRingGeometry(containerSize)
    RemoteBox(
        modifier =
            RemoteModifier.size(containerSize.asRdp())
                .clip(RemoteCircleShape)
                .background(RemoteComposePalette.widgetBackground),
        contentAlignment = RemoteAlignment.Center,
    ) {
        ProgressRingCanvas(
            progress = progress,
            size = geometry.ringSize,
            containerColor = RemoteComposePalette.widgetBackground,
        )
        RemoteColumn(horizontalAlignment = RemoteAlignment.CenterHorizontally) {
            icon?.let { iconRes ->
                RemoteVectorGlyph(
                    icon = iconRes,
                    size = (geometry.ringSize.value * RingDefaults.ContainedIconRatio).dp,
                    tint = RemoteComposePalette.primary,
                )
                RemoteBox(modifier = RemoteModifier.height(CONTAINED_RING_VALUE_TOP_SPACE_DP.rdp))
            }
            SlotText(
                text = fitMetric(value, geometry.valueMaxWidth.value, geometry.fontSize),
                fontSize = geometry.fontSize,
                fontWeight = RemoteComposeFonts.Bold,
                color = RemoteComposePalette.primary,
                modifier = RemoteModifier.widthIn(max = geometry.ringSize.asRdp()),
            )
        }
    }
}

/**
 * Draws the circular fill, track ring and round-capped progress arc natively on a [RemoteCanvas].
 */
@RemoteComposable
@Composable
private fun ProgressRingCanvas(
    @FloatRange(from = 0.0, to = 1.0) progress: Float?,
    size: Dp,
    containerColor: RemoteColor,
) {
    val clamped = progress?.coerceIn(0f, 1f) ?: 0f
    RemoteCanvas(modifier = RemoteModifier.size(size.asRdp())) {
        val strokePx = max(width * RingDefaults.StrokeRatio.rf, 1f.rf)
        val fillRadius = max(width / 2f.rf - strokePx, 0f.rf)
        drawCircle(
            paint = RemotePaint { color = containerColor },
            radius = fillRadius,
            center = center,
        )

        val halfStroke = strokePx / 2f.rf
        val arcTopLeft = RemoteOffset(halfStroke, halfStroke)
        val arcDiameter = max(width - strokePx, 0f.rf)
        val arcSize = RemoteSize(arcDiameter, arcDiameter)
        drawArc(
            paint =
                RemotePaint {
                    color = RemoteComposePalette.ringTrack
                    style = RemotePaintingStyle.Stroke
                    strokeCap = RemoteStrokeCap.Round
                    strokeWidth = strokePx
                },
            startAngle = RING_START_ANGLE_DEGREES.rf,
            sweepAngle = FULL_SWEEP_DEGREES.rf,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
        )
        if (clamped > 0f) {
            drawArc(
                paint =
                    RemotePaint {
                        color = RemoteComposePalette.primary
                        style = RemotePaintingStyle.Stroke
                        strokeCap = RemoteStrokeCap.Round
                        strokeWidth = strokePx
                    },
                startAngle = RING_START_ANGLE_DEGREES.rf,
                sweepAngle = (clamped * FULL_SWEEP_DEGREES).rf,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
            )
        }
    }
}
