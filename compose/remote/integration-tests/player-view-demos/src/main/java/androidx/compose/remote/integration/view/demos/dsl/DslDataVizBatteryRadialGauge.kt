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
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.cos
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.min
import androidx.compose.remote.creation.dsl.rcColor
import androidx.compose.remote.creation.dsl.sin
import androidx.compose.remote.creation.dsl.toRad
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * DSL conversion of `examples/DataVizDemos.kt` `demoBatteryRadialGauge()`.
 *
 * 270° battery gauge with HSV-driven dynamic color (green at 100% → red at 0%), milestone tick
 * marks, % readout and "Xh remaining" sub-label.
 *
 * Exercises typed `paint { color(rcColor) }` taking a dynamic `RcColor` from
 * `remoteColorExpression(alpha, hue, sat, value)`.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslDemoBatteryRadialGauge(): ByteArray {
    val data = floatArrayOf(67f, 8.5f) // percentage, estimatedHoursRemaining

    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Battery Radial Gauge"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFF1A1A2E.toInt())) {
            val w = componentWidth()
            val h = componentHeight()
            val cx = w / 2f
            val cy = h * 0.45f
            val density = density()
            val size = min(w, h)
            val gaugeRadius = size * 0.38f
            val strokeW = size * 0.08f

            val values = remoteFloatArray(data)
            val level = values[0.rf]
            val hoursLeft = values[1.rf]

            // Background track (270°, gap at bottom).
            paint {
                color(0x33FFFFFF)
                style(RcPaintStyle.Stroke)
                strokeWidth(strokeW.toFloat())
                strokeCap(RcStrokeCap.Round)
            }
            drawArc(
                cx - gaugeRadius,
                cy - gaugeRadius,
                cx + gaugeRadius,
                cy + gaugeRadius,
                135f.rf,
                270f.rf,
            )

            // Active gauge arc — color from green(100%) to red(0%) via HSV.
            val normalized = level / 100f
            val hue = normalized * 0.33f // 0 = red, 0.33 = green
            val gaugeColor = remoteColorExpression(0xFF, hue, 0.9f, 0.9f)
            paint { color(gaugeColor) }
            drawArc(
                cx - gaugeRadius,
                cy - gaugeRadius,
                cx + gaugeRadius,
                cy + gaugeRadius,
                135f.rf,
                normalized * 270f,
            )

            // Scale ticks.
            paint {
                color(0x55FFFFFF.rcColor())
                strokeWidth((density * 1.5f).toFloat())
            }
            val tickR1 = gaugeRadius + strokeW / 2f + density * 3f
            val tickR2 = tickR1 + density * 6f
            for (pct in intArrayOf(0, 25, 50, 75, 100)) {
                val angle = 135f + pct.toFloat() / 100f * 270f
                val rad = toRad(angle.rf)
                drawLine(
                    cx + tickR1 * cos(rad),
                    cy + tickR1 * sin(rad),
                    cx + tickR2 * cos(rad),
                    cy + tickR2 * sin(rad),
                )
            }

            // Percentage text.
            paint {
                color(0xFFFFFFFF.toInt())
                style(RcPaintStyle.Fill)
                textSize((density * 48f).toFloat())
                typeface(RcFontType.Default, RcWeight.Bold, italic = false)
            }
            val pctText =
                createTextFromFloat(
                    level,
                    3,
                    0,
                    RcTextFromFloatSpec.of(padPre = RcTextFromFloatSpec.PadPre.None),
                )
            val pctLabel = textMerge(pctText, remoteText("%"))
            drawTextAnchored(pctLabel, cx, cy, 0f.rf, 0f.rf, 0)

            // Hours remaining.
            paint {
                color(0xAAFFFFFF.toInt())
                textSize((density * 14f).toFloat())
                typeface(RcFontType.Default, RcWeight.Normal, italic = false)
            }
            val hrsText =
                createTextFromFloat(
                    hoursLeft,
                    2,
                    1,
                    RcTextFromFloatSpec.of(padPre = RcTextFromFloatSpec.PadPre.None),
                )
            val hrsLabel = textMerge(hrsText, remoteText("h remaining"))
            drawTextAnchored(hrsLabel, cx, cy, 0f.rf, 3.5f.rf, 0)

            // Battery icon indicator (using same gauge color).
            paint {
                color(gaugeColor)
                textSize((density * 20f).toFloat())
            }
            drawTextAnchored(remoteText("⚡"), cx, cy, 0f.rf, (-3.5f).rf, 0)
        }
    }
}
