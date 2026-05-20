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
import androidx.compose.remote.creation.dsl.RcTileMode
import androidx.compose.remote.creation.dsl.RcWeight
import androidx.compose.remote.creation.dsl.arrayMax
import androidx.compose.remote.creation.dsl.arrayMin
import androidx.compose.remote.creation.dsl.arraySpline
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.max
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * DSL conversion of `examples/DataVizDemos.kt` `demoStockSparkline()`.
 *
 * 24-hour stock sparkline with header price, change %, spline-smoothed line, gradient fill under
 * the curve, and min/max labels.
 *
 * Demonstrates:
 * - `remoteFloatArray(...)` + `arrayMin`/`arrayMax`/`arraySpline` for the dataset.
 * - `remotePath(x, y)` + `RcDynamicPath.lineTo` / `.close()` building a dynamic path inside a `loop
 *   { ... }`.
 * - `paint { linearGradient(...) }` for the fill-under-curve.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslDemoStockSparkline(): ByteArray {
    val priceData =
        floatArrayOf(
            42150f,
            42380f,
            42200f,
            41900f,
            41750f,
            42000f,
            42400f,
            42800f,
            43100f,
            43050f,
            42900f,
            43200f,
            43500f,
            43400f,
            43800f,
            44100f,
            44000f,
            43700f,
            43900f,
            44200f,
            44500f,
            44300f,
            44600f,
            44850f,
        )

    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 500),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 350),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Stock Sparkline"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFF1C1C1E.toInt())) {
            val w = componentWidth()
            val h = componentHeight()
            val density = density()

            val prices = remoteFloatArray(priceData)
            val priceMin = arrayMin(prices)
            val priceMax = arrayMax(prices)
            val priceRange = max(priceMax - priceMin, 1f.rf)
            val firstPrice = prices[0.rf]
            val lastPrice = prices[23.rf]
            val changeAmt = lastPrice - firstPrice
            val changePct = changeAmt / firstPrice * 100f

            val gLeft = density * 15f
            val gRight = w - density * 15f
            val gTop = h * 0.38f
            val gBottom = h * 0.82f
            val gW = gRight - gLeft
            val gH = gBottom - gTop

            // Current price.
            paint {
                color(0xFFFFFFFF.toInt())
                style(RcPaintStyle.Fill)
                textSize((density * 32f).toFloat())
                typeface(RcFontType.Default, RcWeight.Bold, italic = false)
            }
            val priceText =
                createTextFromFloat(
                    lastPrice,
                    5,
                    0,
                    RcTextFromFloatSpec.of(padPre = RcTextFromFloatSpec.PadPre.None),
                )
            drawTextAnchored(priceText, density * 20f, h * 0.12f, (-1f).rf, 0f.rf, 0)

            // Change percentage.
            paint {
                color(0xFF4CD964.toInt())
                textSize((density * 16f).toFloat())
            }
            val changeText =
                createTextFromFloat(
                    changePct,
                    2,
                    2,
                    RcTextFromFloatSpec.of(
                        padAfter = RcTextFromFloatSpec.PadAfter.Zero,
                        padPre = RcTextFromFloatSpec.PadPre.None,
                    ),
                )
            val changeLabel = textMerge(textMerge(remoteText("▲ "), changeText), remoteText("%"))
            drawTextAnchored(changeLabel, density * 20f, h * 0.22f, (-1f).rf, 0f.rf, 0)

            paint {
                color(0x66FFFFFF)
                textSize((density * 11f).toFloat())
            }
            drawTextAnchored(remoteText("24h"), density * 20f, h * 0.30f, (-1f).rf, 0f.rf, 0)

            // Build the spline-smoothed sparkline path.
            val startY = gBottom - (arraySpline(prices, 0f.rf) - priceMin) / priceRange * gH
            val linePath = remotePath(gLeft.toFloat(), startY.toFloat())

            val steps = 80f
            loop(1f.rf, 1f.rf, steps.rf) { step ->
                val t = step / steps
                val x = gLeft + gW * t
                val price = arraySpline(prices, t)
                val y = gBottom - (price - priceMin) / priceRange * gH
                linePath.lineTo(x.toFloat(), y.toFloat())
            }

            // Stroke the line.
            paint {
                color(0xFF4CD964.toInt())
                style(RcPaintStyle.Stroke)
                strokeWidth((density * 2f).toFloat())
                strokeCap(RcStrokeCap.Round)
            }
            drawPath(linePath.getPath())

            // Close the path to form a polygon for gradient fill.
            linePath.lineTo(gRight.toFloat(), gBottom.toFloat())
            linePath.lineTo(gLeft.toFloat(), gBottom.toFloat())
            linePath.close()

            paint {
                style(RcPaintStyle.Fill)
                linearGradient(
                    startX = gLeft.toFloat(),
                    startY = gTop.toFloat(),
                    endX = gLeft.toFloat(),
                    endY = gBottom.toFloat(),
                    colors = intArrayOf(0x664CD964, 0x00000000),
                    tileMode = RcTileMode.Clamp,
                )
            }
            drawPath(linePath.getPath())

            // Reset shader so the labels below render as solid colour.
            paint { raw.setShader(0) }

            // Min/max indicators.
            paint {
                color(0x66FFFFFF)
                textSize((density * 9f).toFloat())
                style(RcPaintStyle.Fill)
            }
            val minText =
                createTextFromFloat(
                    priceMin,
                    5,
                    0,
                    RcTextFromFloatSpec.of(padPre = RcTextFromFloatSpec.PadPre.None),
                )
            val maxText =
                createTextFromFloat(
                    priceMax,
                    5,
                    0,
                    RcTextFromFloatSpec.of(padPre = RcTextFromFloatSpec.PadPre.None),
                )
            drawTextAnchored(minText, gRight, gBottom + density * 10f, 1f.rf, 0f.rf, 0)
            drawTextAnchored(maxText, gRight, gTop - density * 4f, 1f.rf, 0f.rf, 0)
        }
    }
}
