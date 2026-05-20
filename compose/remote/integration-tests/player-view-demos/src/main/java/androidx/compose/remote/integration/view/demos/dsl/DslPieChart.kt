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
import androidx.compose.remote.creation.dsl.min
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * DSL conversion of `examples/PieChart.kt` `demoPieChart()` (the host-side-loop variant).
 *
 * The slice colors, names, and percentages are baked into the document at creation time (host-side
 * `for` over `data.indices`). The player-side `loop(...)` variant of PieChart (`demoPieChart_good`)
 * uses helpers (`getDistributedColor`, dynamic `textLookup`) that aren't in the DSL — skipped for
 * now.
 *
 * Demonstrates:
 * - Mixed Float / RcFloat in drawSector — converted via `currentAngle.rf` etc. so the all-RcFloat
 *   overload matches.
 * - Typed `typeface(RcFontType.Default, RcWeight.Bold)` for bold legend labels.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslPieChart(): ByteArray {
    val data = floatArrayOf(30f, 20f, 15f, 25f, 10f)
    val names = arrayOf("Android", "iOS", "Web", "Desktop", "Other")
    val colors =
        intArrayOf(
            0xFF4CAF50.toInt(), // Green
            0xFF2196F3.toInt(), // Blue
            0xFFFFC107.toInt(), // Amber
            0xFFE91E63.toInt(), // Pink
            0xFF9C27B0.toInt(), // Purple
        )

    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 500),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 500),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Pie Chart Demo"),
        experimental = true,
    ) {
        Box(modifier = Modifier.fillMaxSize().background(0xFFF0F0F0.toInt())) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = componentWidth()
                val h = componentHeight()
                val cx = w / 2f
                val cy = h / 2f
                val radius = min(w, h) * 0.4f

                val total = data.sum()
                var currentAngle = -90f // Start from top

                for (i in data.indices) {
                    val sweepAngle = (data[i] / total) * 360f

                    // Slice fill.
                    paint {
                        color(colors[i % colors.size])
                        style(RcPaintStyle.Fill)
                    }
                    drawSector(
                        cx - radius,
                        cy - radius,
                        cx + radius,
                        cy + radius,
                        currentAngle.rf,
                        sweepAngle.rf,
                    )

                    // Slice border.
                    paint {
                        color(0xFFFFFFFF.toInt())
                        style(RcPaintStyle.Stroke)
                        strokeWidth(2f)
                    }
                    drawSector(
                        cx - radius,
                        cy - radius,
                        cx + radius,
                        cy + radius,
                        currentAngle.rf,
                        sweepAngle.rf,
                    )

                    // Label at the slice midpoint (host-side trig).
                    val labelAngle = (currentAngle + sweepAngle / 2f) * PI.toFloat() / 180f
                    val labelRadius = radius * 0.7f
                    val lx = cx + labelRadius * cos(labelAngle)
                    val ly = cy + labelRadius * sin(labelAngle)

                    paint {
                        color(0xFFFFFFFF.toInt())
                        textSize(24f)
                        style(RcPaintStyle.Fill)
                        typeface(RcFontType.Default, RcWeight.Bold, italic = false)
                    }
                    val textId = remoteText(names[i])
                    drawTextAnchored(textId, lx, ly, 0.5f.rf, 0.5f.rf, 0)

                    currentAngle += sweepAngle
                }

                // Legend.
                val legendX = 20f
                var legendY = 20f
                for (i in data.indices) {
                    paint {
                        color(colors[i % colors.size])
                        style(RcPaintStyle.Fill)
                    }
                    drawRect(legendX, legendY, legendX + 20f, legendY + 20f)

                    paint {
                        color(0xFF000000.toInt())
                        textSize(20f)
                    }
                    val textId = remoteText(names[i] + " (" + data[i].toInt() + "%)")
                    drawTextAnchored(textId, legendX + 30f, legendY + 15f, -1f, 0f, 0)

                    legendY += 30f
                }
            }
        }
    }
}
