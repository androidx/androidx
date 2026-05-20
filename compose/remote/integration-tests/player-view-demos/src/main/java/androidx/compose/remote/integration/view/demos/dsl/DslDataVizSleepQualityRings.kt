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
import androidx.compose.remote.creation.dsl.RcFontType
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcStrokeCap
import androidx.compose.remote.creation.dsl.RcTextFromFloatSpec
import androidx.compose.remote.creation.dsl.RcWeight
import androidx.compose.remote.creation.dsl.arraySum
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.floor
import androidx.compose.remote.creation.dsl.min
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * DSL conversion of `examples/DataVizDemos.kt` `demoSleepQualityRings()`.
 *
 * Donut chart of four sleep stages (Deep / Light / REM / Awake) with thin background-color
 * separators between segments, total sleep time at center, and a legend below.
 *
 * Host-side `for (i in stageData.indices)` loop walks the stage data and accumulates the running
 * `currentAngle` so each segment starts where the previous ended.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslDemoSleepQualityRings(): ByteArray {
    // Minutes in each stage: deep, light, REM, awake
    val stageData = floatArrayOf(110f, 180f, 90f, 20f)
    val stageColors =
        intArrayOf(
            0xFF1A237E.toInt(), // Deep - dark blue
            0xFF42A5F5.toInt(), // Light - light blue
            0xFF7E57C2.toInt(), // REM - purple
            0xFFFF7043.toInt(), // Awake - orange
        )
    val stageLabels = arrayOf("Deep", "Light", "REM", "Awake")

    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Sleep Quality Rings"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFF0D1B2A.toInt())) {
            val w = componentWidth()
            val h = componentHeight()
            val cx = w / 2f
            val cy = h * 0.45f
            val density = density()
            val size = min(w, h)
            val ringRadius = size * 0.35f
            val strokeW = size * 0.09f

            val values = remoteFloatArray(stageData)
            val total = arraySum(values)

            // Draw ring segments.
            var currentAngle = (-90f).rf // Start from 12 o'clock.
            for (i in stageData.indices) {
                val stageMin = values[i.rf]
                val sweepAngle = stageMin / total * 360f

                paint {
                    color(stageColors[i])
                    style(RcPaintStyle.Stroke)
                    strokeWidth(strokeW.toFloat())
                    strokeCap(RcStrokeCap.Butt)
                }
                drawArc(
                    cx - ringRadius,
                    cy - ringRadius,
                    cx + ringRadius,
                    cy + ringRadius,
                    currentAngle,
                    sweepAngle,
                )

                // Thin separator using the page background colour.
                paint {
                    color(0xFF0D1B2A.toInt())
                    strokeWidth((density * 2f).toFloat())
                }
                save {
                    rotate(currentAngle + sweepAngle, cx, cy)
                    drawLine(cx, cy - ringRadius - strokeW / 2f, cx, cy - ringRadius + strokeW / 2f)
                }

                currentAngle += sweepAngle
            }

            // Center: total sleep time.
            val totalMinutes = total
            val hrs = floor(totalMinutes / 60f)
            val mins = totalMinutes - hrs * 60f

            paint {
                color(0xFFFFFFFF.toInt())
                style(RcPaintStyle.Fill)
                textSize((density * 32f).toFloat())
                typeface(RcFontType.Default, RcWeight.Bold, italic = false)
            }
            val hrsText =
                createTextFromFloat(
                    hrs,
                    1,
                    0,
                    RcTextFromFloatSpec.of(padPre = RcTextFromFloatSpec.PadPre.None),
                )
            val minsText =
                createTextFromFloat(
                    mins,
                    2,
                    0,
                    RcTextFromFloatSpec.of(padPre = RcTextFromFloatSpec.PadPre.None),
                )
            val timeLabel =
                textMerge(
                    textMerge(textMerge(hrsText, remoteText("h ")), minsText),
                    remoteText("m"),
                )
            drawTextAnchored(timeLabel, cx, cy, 0f.rf, 0f.rf, 0)

            paint {
                color(0xAAFFFFFF.toInt())
                textSize((density * 13f).toFloat())
                typeface(RcFontType.Default, RcWeight.Normal, italic = false)
            }
            drawTextAnchored(remoteText("Total Sleep"), cx, cy, 0f.rf, 3.5f.rf, 0)

            // Legend.
            for (i in stageData.indices) {
                val lx = w * 0.15f + i.rf * w * 0.2f
                val ly = h * 0.88f
                paint {
                    color(stageColors[i])
                    style(RcPaintStyle.Fill)
                }
                drawCircle(lx, ly, (density * 4f))

                paint {
                    color(0xCCFFFFFF.toInt())
                    textSize((density * 10f).toFloat())
                }
                drawTextAnchored(
                    remoteText(stageLabels[i]),
                    lx,
                    ly + density * 12f,
                    0f.rf,
                    0f.rf,
                    0,
                )
            }
        }
    }
}
