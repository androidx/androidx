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
import androidx.compose.remote.creation.dsl.RcWeight
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.floor
import androidx.compose.remote.creation.dsl.rcColor
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * DSL conversion of `examples/DataVizDemos.kt` `demoCalendarHeatmapGrid()`.
 *
 * 7×5 calendar grid heatmap of "activity intensity" over 35 days. Cells are filled by a player-side
 * `loop(...)` that derives row/col from the index, varies HSV saturation and value by intensity,
 * and registers a fresh color expression per cell.
 *
 * The current day cell is outlined in white. A legend below shows the low→high gradient.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslDemoCalendarHeatmap(): ByteArray {
    val patterns =
        floatArrayOf(
            0.2f,
            0.8f,
            0.6f,
            0.0f,
            0.4f,
            0.9f,
            0.1f,
            0.5f,
            0.7f,
            0.3f,
            0.0f,
            0.6f,
            1.0f,
            0.2f,
            0.4f,
            0.9f,
            0.8f,
            0.1f,
            0.3f,
            0.7f,
            0.0f,
            0.6f,
            0.5f,
            0.7f,
            0.0f,
            0.8f,
            0.4f,
            0.3f,
            0.3f,
            0.6f,
            0.9f,
            0.2f,
            0.5f,
            0.7f,
            0.8f,
        )
    val dayLabels = arrayOf("M", "T", "W", "T", "F", "S", "S")
    val currentDayIndex = 33

    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Calendar Heatmap Grid"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFF161B22.toInt())) {
            val w = componentWidth()
            val h = componentHeight()
            val density = density()

            val values = remoteFloatArray(patterns)

            val gridLeft = w * 0.15f
            val gridTop = h * 0.22f
            val gridW = w * 0.75f
            val gridH = h * 0.6f
            val cols = 7f
            val rows = 5f
            val cellW = gridW / cols
            val cellH = gridH / rows
            val gap = density * 3f

            // Title.
            paint {
                color(0xFFFFFFFF.toInt())
                style(RcPaintStyle.Fill)
                textSize((density * 16f).toFloat())
                typeface(RcFontType.Default, RcWeight.Bold, italic = false)
            }
            drawTextAnchored(
                remoteText("Activity · Past 35 Days"),
                w / 2f,
                density * 20f,
                0f.rf,
                0f.rf,
                0,
            )

            // Day-of-week headers.
            paint {
                color(0x99FFFFFF.toInt())
                textSize((density * 11f).toFloat())
                typeface(RcFontType.Default, RcWeight.Normal, italic = false)
            }
            for (d in 0..6) {
                val x = gridLeft + d.rf * cellW + cellW / 2f
                drawTextAnchored(
                    remoteText(dayLabels[d]),
                    x,
                    gridTop - density * 8f,
                    0f.rf,
                    0f.rf,
                    0,
                )
            }

            // Heatmap cells via player-side loop.
            loop(0f.rf, 1f.rf, 35f.rf) { idx ->
                val col = idx % 7f
                val row = floor(idx / 7f)
                val cellX = gridLeft + col * cellW + gap / 2f
                val cellY = gridTop + row * cellH + gap / 2f
                val intensity = values[idx]

                // Color: dark green (low) to bright green (high). Hue is constant so we
                // can pass it as Float; sat/value are dynamic via .toFloat() NaN-encoding.
                val colorId =
                    remoteColorExpression(
                        alpha = 0xFF,
                        hue = 0.33f,
                        sat = (intensity * 0.8f + 0.1f).toFloat(),
                        value = (intensity * 0.7f + 0.15f).toFloat(),
                    )
                paint {
                    color(colorId)
                    style(RcPaintStyle.Fill)
                }
                drawRoundRect(cellX, cellY, cellW - gap, cellH - gap, density * 3f, density * 3f)
            }

            // Highlight current day with a border.
            val curCol = currentDayIndex % 7
            val curRow = currentDayIndex / 7
            val hlX = gridLeft + curCol.rf * cellW + gap / 2f
            val hlY = gridTop + curRow.rf * cellH + gap / 2f
            paint {
                color(0xFFFFFFFF.rcColor())
                style(RcPaintStyle.Stroke)
                strokeWidth((density * 2f).toFloat())
            }
            drawRoundRect(hlX, hlY, cellW - gap, cellH - gap, density * 3f, density * 3f)

            // Legend.
            paint { style(RcPaintStyle.Fill) }
            val legendY = gridTop + gridH + density * 30f
            paint {
                color(0x99FFFFFF.rcColor())
                textSize((density * 10f).toFloat())
            }
            drawTextAnchored(remoteText("Less"), w * 0.3f, legendY, 1.5f.rf, 0f.rf, 0)
            drawTextAnchored(remoteText("More"), w * 0.7f, legendY, (-1.5f).rf, 0f.rf, 0)
            for (level in 0..4) {
                val lx = w * 0.35f + level.rf * density * 16f
                val intensity = level.toFloat() / 4f
                val colorId =
                    remoteColorExpression(
                        alpha = 0xFF,
                        hue = 0.33f,
                        sat = intensity * 0.8f + 0.1f,
                        value = intensity * 0.7f + 0.15f,
                    )
                paint { color(colorId) }
                drawRoundRect(
                    lx,
                    legendY - density * 5f,
                    density * 10f,
                    density * 10f,
                    density * 2f,
                    density * 2f,
                )
            }
        }
    }
}
