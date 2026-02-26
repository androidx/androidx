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

package androidx.compose.remote.integration.view.demos.examples

import android.graphics.Paint
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.cos
import androidx.compose.remote.creation.min
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.sin
import androidx.compose.remote.creation.times

/** Draw a circular pressure gauge */
@Suppress("RestrictedApiAndroidX")
fun demoPressureGauge(): RemoteComposeWriter {
    addHeaderParam(Header.DOC_WIDTH, 500)
    addHeaderParam(Header.DOC_HEIGHT, 500)
    addHeaderParam(Header.DOC_CONTENT_DESCRIPTION, "Pressure Gauge")
    addHeaderParam(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX)

    val rc = demo7 {
        root {
            canvas(RecordingModifier().fillMaxSize().background(0xFF4A6DA7.toInt())) {
                val w = ComponentWidth()
                val h = ComponentHeight()
                val cx = w / 2f
                val cy = h / 2f
                val density = rf(Rc.System.DENSITY)
                val radius = min(w, h) / 2f
                val pressure = sin(ContinuousSec()) * 50f + 750f
                val deltaPressure = cos(ContinuousSec())
                val maxPressure = 800f
                val minPressure = 700f

                painter.setColor(0xFF6A8DC7.toInt()).setTextSize((density * 24f).toFloat()).commit()
                drawTextAnchored("PRESSURE", 50f, 40f, -1, 1, 0)

                painter
                    .setStyle(Paint.Style.STROKE)
                    .setStrokeWidth((density * 6f).toFloat())
                    .commit()
                val dialY = cy + radius * 0.1f
                val dialRadius = radius * 0.8f
                val tickLength = radius * 0.2f

                // Draw a sweep of ticks
                save {
                    rotate(-135, cx, dialY)
                    loop(0, 5, 265) { angle ->
                        rotate(5, cx, dialY)
                        drawLine(cx, dialY - dialRadius + tickLength, cx, dialY - dialRadius)
                    }
                }

                // convert pressure to angle
                val pressureAngle =
                    (pressure - minPressure) / (maxPressure - minPressure) * 270f - 135f

                // draw a motion blur of a gradient where the pressure is
                // Rather than rotate the gradient rotate the paint
                painter
                    .setSweepGradient(
                        cx.toFloat(),
                        dialY.toFloat(),
                        intArrayOf(0x00FFFFFFL.toInt(), 0x99FFFFFFL.toInt(), 0x00FFFFFFL.toInt()),
                        floatArrayOf(0.01f, 0.05f, 0.09f),
                    )
                    .setStyle(Paint.Style.STROKE)
                    .setStrokeWidth((tickLength).toFloat())
                    .setStrokeCap(Paint.Cap.BUTT)
                    .commit()
                val gap = 18f
                val drawRadius = dialRadius - tickLength / 2f
                save() {
                    rotate(pressureAngle - 90f - gap, cx, dialY)
                    drawArc(
                        cx - drawRadius,
                        dialY - drawRadius,
                        cx + drawRadius,
                        dialY + drawRadius,
                        gap,
                        deltaPressure * (-30f + gap),
                    )
                }
                // Draw the line
                painter
                    .setShader(0)
                    .setColor(WHITE)
                    .setStyle(Paint.Style.STROKE)
                    .setStrokeWidth((density * 8f).toFloat())
                    .setStrokeCap(Paint.Cap.ROUND)
                    .commit()
                save {
                    rotate(pressureAngle, cx, dialY)
                    drawLine(cx, dialY - dialRadius + tickLength, cx, dialY - dialRadius)
                }

                painter
                    .setShader(0)
                    .setTextSize((density * 64f).toFloat())
                    .setColor(0xFFFFFFFF.toInt())
                    .setStyle(Paint.Style.FILL)
                    .setStrokeWidth(0f)
                    .commit()

                // draw the correct arrow based on the pressure direction
                conditionalOperations(Rc.Condition.LT, 0f, deltaPressure.toFloat()) {
                    drawTextAnchored("↑", cx, dialY, 0, -3, 0)
                }

                conditionalOperations(Rc.Condition.GTE, 0f, deltaPressure.toFloat()) {
                    drawTextAnchored("↓", cx, dialY, 0, -3, 0)
                }

                // Draw the text pressure
                val textID = createTextFromFloat(pressure, 3, 0, Rc.TextFromFloat.PAD_AFTER_ZERO)

                drawTextAnchored(textID, cx, dialY, 0, 0, 0)
                // draw labels
                painter.setTextSize((density * 32f).toFloat()).commit()
                drawTextAnchored("mmHg", cx, dialY, 0, 4, 0)

                drawTextAnchored("Low", cx - dialRadius * 0.6f, dialY + dialRadius * 0.9f, 0, 0, 0)
                drawTextAnchored("High", cx + dialRadius * 0.6f, dialY + dialRadius * 0.9f, 0, 0, 0)
            }
        }
    }
    return rc.writer
}
