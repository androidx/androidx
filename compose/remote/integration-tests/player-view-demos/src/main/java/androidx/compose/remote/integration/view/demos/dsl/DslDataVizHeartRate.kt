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
import androidx.compose.remote.creation.dsl.arraySpline
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.sin
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * DSL conversion of `examples/DataVizDemos.kt` `demoHeartRateTimeline()`.
 *
 * 24-hour heart rate spline curve with color-zone backgrounds (resting blue / normal green /
 * elevated yellow / peak red), hour-tick labels, and a pulsing current-BPM readout (size animated
 * via `sin(continuousSec)`).
 */
@Suppress("RestrictedApiAndroidX")
public fun dslDemoHeartRateTimeline(): ByteArray {
    val hrData =
        floatArrayOf(
            62f,
            58f,
            55f,
            54f,
            56f,
            60f,
            72f,
            85f,
            95f,
            88f,
            78f,
            82f,
            90f,
            76f,
            70f,
            68f,
            105f,
            140f,
            130f,
            95f,
            80f,
            72f,
            65f,
            60f,
        )

    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 500),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Heart Rate Timeline"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFF1A1A2E.toInt())) {
            val w = componentWidth()
            val h = componentHeight()
            val density = density()
            val values = remoteFloatArray(hrData)

            val gLeft = density * 20f
            val gRight = w - density * 8f
            val gTop = h * 0.28f
            val gBottom = h * 0.82f
            val gW = gRight - gLeft
            val gH = gBottom - gTop
            val minHR = 40f
            val maxHR = 160f
            val hrRange = maxHR - minHR

            // Color-zone backgrounds.
            val zones =
                arrayOf(
                    Triple(40f, 60f, 0x30448AFF), // Resting blue
                    Triple(60f, 100f, 0x304CD964), // Normal green
                    Triple(100f, 130f, 0x30FFCC00), // Elevated yellow
                    Triple(130f, 160f, 0x30FF3B30), // Peak red
                )
            paint {
                style(RcPaintStyle.Fill)
                strokeWidth(0f)
            }
            for ((lo, hi, c) in zones) {
                paint { color(c) }
                val y1 = gBottom - gH * ((hi - minHR) / hrRange)
                val y2 = gBottom - gH * ((lo - minHR) / hrRange)
                // Note: original passes (gW, y2-y1) as right/bottom — preserved verbatim.
                drawRect(gLeft, y1, gW, y2 - y1)
            }

            // HR curve via arraySpline.
            val startY = gBottom - (arraySpline(values, 0f.rf) - minHR) / hrRange * gH
            val linePath = remotePath(gLeft.toFloat(), startY.toFloat())
            val steps = 80f
            loop(1f.rf, 1f.rf, steps.rf) { step ->
                val t = step / steps
                val x = gLeft + gW * t
                val hrVal = arraySpline(values, t)
                val y = gBottom - (hrVal - minHR) / hrRange * gH
                linePath.lineTo(x.toFloat(), y.toFloat())
            }
            paint {
                color(0xFFFF3B30.toInt())
                style(RcPaintStyle.Stroke)
                strokeWidth((density * 2.5f).toFloat())
                strokeCap(RcStrokeCap.Round)
            }
            drawPath(linePath.getPath())

            // Hour labels (host-side loop over [0, 6, 12, 18]).
            paint {
                color(0x99FFFFFF.toInt())
                style(RcPaintStyle.Fill)
                textSize((density * 10f).toFloat())
            }
            for (hr in intArrayOf(0, 6, 12, 18)) {
                val x = gLeft + gW * hr.rf / 23f
                drawTextAnchored(remoteText("${hr}h"), x, gBottom + density * 12f, 0f.rf, 0f.rf, 0)
            }

            // Current BPM with pulsing effect.
            val pulse = sin(continuousSeconds() * 8f) * 0.15f + 1.0f
            val bpmSize = density * 38f * pulse
            paint {
                color(0xFFFF3B30.toInt())
                textSize(bpmSize.toFloat())
                typeface(RcFontType.Default, RcWeight.Bold, italic = false)
            }
            val bpmText =
                createTextFromFloat(
                    values[23.rf],
                    3,
                    0,
                    RcTextFromFloatSpec.of(padPre = RcTextFromFloatSpec.PadPre.None),
                )
            drawTextAnchored(bpmText, w * 0.75f, h * 0.12f, 0f.rf, 0f.rf, 0)
            paint {
                textSize((density * 14f).toFloat())
                typeface(RcFontType.Default, RcWeight.Normal, italic = false)
                color(0xAAFFFFFF.toInt())
            }
            drawTextAnchored(remoteText("BPM"), w * 0.75f, h * 0.12f, 0f.rf, 3f.rf, 0)

            // Heart icon.
            paint {
                color(0xFFFF3B30.toInt())
                textSize((density * 20f * pulse).toFloat())
            }
            drawTextAnchored(remoteText("♥"), w * 0.75f, h * 0.12f, 4f.rf, 0f.rf, 0)
        }
    }
}
