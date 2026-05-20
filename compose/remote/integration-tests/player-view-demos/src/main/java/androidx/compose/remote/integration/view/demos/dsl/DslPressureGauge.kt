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

package androidx.compose.remote.integration.view.demos.dsl

import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcConditionOp
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcStrokeCap
import androidx.compose.remote.creation.dsl.RcTextFromFloatSpec
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.cos
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.min
import androidx.compose.remote.creation.dsl.sin
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * DSL conversion of `examples/PressureGauge.kt` `demoPressureGauge()`.
 *
 * A circular pressure dial with a sweep of ticks, a motion-blur sweep gradient, the current
 * pressure reading at the centre, and up/down arrows indicating direction of change.
 *
 * Demonstrates:
 * - `paint { sweepGradient(...) }` typed gradient on the canvas-paint scope.
 * - `paint { raw.setShader(0) }` escape hatch for clearing the shader (G2).
 * - Typed `conditionalOperations(RcConditionOp.Lt, …)` with the new enum (B5).
 * - `loop(0f.rf, 5f, 265f.rf)` for the sweep of tick marks.
 * - `createTextFromFloat(pressure, …, RcTextFromFloatSpec.PadAfterZero)` for the formatted reading.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslDemoPressureGauge(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 500),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 500),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Pressure Gauge"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFF4A6DA7.toInt())) {
            val w = componentWidth()
            val h = componentHeight()
            val cx = w / 2f
            val cy = h / 2f
            val density = density()
            val radius = min(w, h) / 2f
            val pressure = sin(continuousSeconds()) * 50f + 750f
            val deltaPressure = cos(continuousSeconds())
            val maxPressure = 800f
            val minPressure = 700f

            paint {
                color(0xFF6A8DC7.toInt())
                textSize((density * 24f).toFloat())
            }
            val labelId = remoteText("PRESSURE")
            drawTextAnchored(labelId, 50f, 40f, -1f, 1f, 0)

            paint {
                style(RcPaintStyle.Stroke)
                strokeWidth((density * 6f).toFloat())
            }
            val dialY = cy + radius * 0.1f
            val dialRadius = radius * 0.8f
            val tickLength = radius * 0.2f

            // Draw a sweep of ticks. Loop variable `angle` is unused — the inner
            // `rotate(5, cx, dialY)` walks the canvas by 5° per iteration.
            save {
                rotate((-135f).rf, cx, dialY)
                loop(0f.rf, 5f.rf, 265f.rf) { _ ->
                    rotate(5f.rf, cx, dialY)
                    drawLine(cx, dialY - dialRadius + tickLength, cx, dialY - dialRadius)
                }
            }

            // Convert pressure to angle.
            val pressureAngle = (pressure - minPressure) / (maxPressure - minPressure) * 270f - 135f

            // Draw a motion-blur sweep gradient where the pressure is. Rather than rotate
            // the gradient we rotate the paint by `pressureAngle`.
            paint {
                sweepGradient(
                    centerX = cx.toFloat(),
                    centerY = dialY.toFloat(),
                    colors = intArrayOf(0x00FFFFFF, 0x99FFFFFF.toInt(), 0x00FFFFFF),
                    positions = floatArrayOf(0.01f, 0.05f, 0.09f),
                )
                style(RcPaintStyle.Stroke)
                strokeWidth(tickLength.toFloat())
                strokeCap(RcStrokeCap.Butt)
            }
            val gap = 18f
            val drawRadius = dialRadius - tickLength / 2f
            save {
                rotate(pressureAngle - 90f - gap, cx, dialY)
                drawArc(
                    cx - drawRadius,
                    dialY - drawRadius,
                    cx + drawRadius,
                    dialY + drawRadius,
                    gap.rf,
                    deltaPressure * (-30f + gap),
                )
            }

            // Draw the indicator needle.
            paint {
                raw.setShader(0)
                color(0xFFFFFFFF.toInt())
                style(RcPaintStyle.Stroke)
                strokeWidth((density * 8f).toFloat())
                strokeCap(RcStrokeCap.Round)
            }
            save {
                rotate(pressureAngle, cx, dialY)
                drawLine(cx, dialY - dialRadius + tickLength, cx, dialY - dialRadius)
            }

            // Pressure-text style.
            paint {
                raw.setShader(0)
                textSize((density * 64f).toFloat())
                color(0xFFFFFFFF.toInt())
                style(RcPaintStyle.Fill)
                strokeWidth(0f)
            }

            // Draw the correct arrow based on the pressure direction.
            conditionalOperations(RcConditionOp.Lt, 0f.rf, deltaPressure) {
                val up = remoteText("↑")
                drawTextAnchored(up, cx, dialY, 0f.rf, (-3f).rf, 0)
            }
            conditionalOperations(RcConditionOp.Gte, 0f.rf, deltaPressure) {
                val down = remoteText("↓")
                drawTextAnchored(down, cx, dialY, 0f.rf, (-3f).rf, 0)
            }

            // Draw the formatted pressure text.
            val textId =
                createTextFromFloat(
                    pressure,
                    3,
                    0,
                    RcTextFromFloatSpec.of(padAfter = RcTextFromFloatSpec.PadAfter.Zero),
                )
            drawTextAnchored(textId, cx, dialY, 0f.rf, 0f.rf, 0)

            // Labels.
            paint { textSize((density * 32f).toFloat()) }
            val mmhg = remoteText("mmHg")
            drawTextAnchored(mmhg, cx, dialY, 0f.rf, 4f.rf, 0)

            val low = remoteText("Low")
            drawTextAnchored(
                low,
                cx - dialRadius * 0.6f,
                dialY + dialRadius * 0.9f,
                0f.rf,
                0f.rf,
                0,
            )
            val high = remoteText("High")
            drawTextAnchored(
                high,
                cx + dialRadius * 0.6f,
                dialY + dialRadius * 0.9f,
                0f.rf,
                0f.rf,
                0,
            )
        }
    }
}
