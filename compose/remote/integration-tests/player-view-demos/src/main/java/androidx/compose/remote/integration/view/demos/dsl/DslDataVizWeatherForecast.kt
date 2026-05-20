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
import androidx.compose.remote.creation.dsl.RcTextFromFloatSpec
import androidx.compose.remote.creation.dsl.RcWeight
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.max
import androidx.compose.remote.creation.dsl.rcColor
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * DSL conversion of `examples/DataVizDemos.kt` `demoWeatherForecastBars()`.
 *
 * 12 hourly temperature bars with HSV-driven color (cold-blue → hot-red), per-bar temperature
 * label, and a precipitation-probability dot below each bar. Y-axis temperature gridlines and hour
 * labels frame the chart.
 *
 * The temperature bars are drawn via a player-side `loop`; the hour labels and Y-axis gridlines use
 * host-side Kotlin loops since the strings are static.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslDemoWeatherForecast(): ByteArray {
    val tempData = floatArrayOf(18f, 17f, 16f, 17f, 19f, 22f, 25f, 28f, 30f, 29f, 27f, 24f)
    val precipData = floatArrayOf(0f, 0f, 10f, 20f, 5f, 0f, 0f, 0f, 15f, 40f, 60f, 30f)
    val hours = arrayOf("6a", "7a", "8a", "9a", "10a", "11a", "12p", "1p", "2p", "3p", "4p", "5p")

    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 500),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Weather Forecast Bars"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFF0A1628.toInt())) {
            val w = componentWidth()
            val h = componentHeight()
            val density = density()

            val temps = remoteFloatArray(tempData)
            val precips = remoteFloatArray(precipData)

            val marginL = density * 35f
            val marginR = density * 10f
            val marginTop = density * 50f
            val marginBot = density * 40f
            val graphW = w - (marginL - marginR).flush()
            val graphH = h - marginTop - marginBot
            val graphBottom = h - marginBot
            val barCount = 12f
            val barSpacing = graphW / barCount
            val barWidth = barSpacing * 0.6f
            val minTemp = 10f
            val maxTemp = 35f
            val tempRange = maxTemp - minTemp

            // Title.
            paint {
                color(0xFFFFFFFF.toInt())
                style(RcPaintStyle.Fill)
                textSize((density * 16f).toFloat())
                typeface(RcFontType.Default, RcWeight.Bold, italic = false)
            }
            drawTextAnchored(remoteText("Hourly Forecast"), w / 2f, density * 18f, 0f.rf, 0f.rf, 0)

            // Temperature bars via player-side loop.
            loop(0f.rf, 1f.rf, barCount.rf) { i ->
                val temp = temps[i]
                val normalizedTemp = ((temp - minTemp) / tempRange).flush()
                val barH = max(density * 4f, normalizedTemp * graphH).flush()
                val barX = (marginL + i * barSpacing + (barSpacing - barWidth) / 2f).flush()
                val barY = graphBottom - barH

                // Color by temperature: blue(cold) → red(hot) via HSV.
                // Rearranged so RcFloat is on the left of `-` (G15).
                val hue = (-normalizedTemp + 1f) * 0.66f
                val colorId = remoteColorExpression(0xFF, hue, 0.8f, 0.9f)
                paint {
                    color(colorId)
                    style(RcPaintStyle.Fill)
                }
                drawRoundRect(barX, barY, barWidth, barH, density * 3f, density * 3f)

                // Temperature label on top of bar.
                paint {
                    color(0xFFFFFFFF.toInt())
                    textSize((density * 10f).toFloat())
                    typeface(RcFontType.Default, RcWeight.SemiBold, italic = false)
                }
                val tempLabel =
                    createTextFromFloat(
                        temp,
                        2,
                        0,
                        RcTextFromFloatSpec.of(padPre = RcTextFromFloatSpec.PadPre.None),
                    )
                val tempStr = textMerge(tempLabel, remoteText("°"))
                drawTextAnchored(
                    tempStr,
                    barX + barWidth / 2f,
                    barY - density * 6f,
                    0f.rf,
                    0f.rf,
                    0,
                )

                // Precipitation indicator (blue dot, size = probability).
                val precip = precips[i]
                val dotRadius = precip / 100f * density * 6f
                paint { color(0xAA4488FF.toInt()) }
                drawCircle(barX + barWidth / 2f, graphBottom + density * 10f, dotRadius)
            }

            // Hour labels (host-side loop for static strings).
            paint {
                color(0x99FFFFFF.toInt())
                textSize((density * 9f).toFloat())
                typeface(RcFontType.Default, RcWeight.Normal, italic = false)
            }
            for (i in hours.indices) {
                val x = marginL + i.rf * barSpacing + barSpacing / 2f
                drawTextAnchored(
                    remoteText(hours[i]),
                    x,
                    graphBottom + density * 22f,
                    0f.rf,
                    0f.rf,
                    0,
                )
            }

            // Y-axis temperature labels + gridlines.
            paint {
                color(0x66FFFFFF.rcColor())
                textSize((density * 9f).toFloat())
            }
            for (t in intArrayOf(15, 20, 25, 30)) {
                // Rearranged so `graphH` (RcFloat) is on the left of the `*` (G15).
                val y = graphBottom - graphH * ((t.toFloat() - minTemp) / tempRange)
                drawTextAnchored(remoteText("$t°"), marginL - density * 6f, y, 1f.rf, 0f.rf, 0)
                paint {
                    color(0x22FFFFFF.rcColor())
                    strokeWidth(1f)
                    style(RcPaintStyle.Stroke)
                }
                drawLine(marginL, y, w - marginR, y)
                paint {
                    color(0x66FFFFFF.rcColor())
                    style(RcPaintStyle.Fill)
                }
            }
        }
    }
}
