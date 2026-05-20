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
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.min
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * DSL conversion of `examples/DataVizDemos.kt` `demoActivityRings()`.
 *
 * Three concentric circular progress rings (Move / Exercise / Stand) with track, progress arc, and
 * labeled percentages below.
 *
 * Demonstrates:
 * - `remoteFloatArray(...)` to register a float-array resource and indexing via `arr.get(i.rf)`.
 * - Typed `typeface(RcFontType.Default, RcWeight.Bold)` (B6).
 * - `RcTextFromFloatSpec.of(padPre = PadPre.None)` for the percentage formatting.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslDemoActivityRings(): ByteArray {
    val data = floatArrayOf(78f, 62f, 91f) // Move%, Exercise%, Stand%
    val ringColors = intArrayOf(0xFFFF2D55.toInt(), 0xFF4CD964.toInt(), 0xFF5AC8FA.toInt())
    val trackColors = intArrayOf(0x44FF2D55, 0x444CD964, 0x445AC8FA)
    val labels = arrayOf("Move", "Exercise", "Stand")

    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Activity Rings"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFF1C1C1E.toInt())) {
            val w = componentWidth()
            val h = componentHeight()
            val cx = w / 2f
            val cy = h * 0.43f
            val density = density()
            val size = min(w, h)
            val strokeW = size * 0.06f
            val ringGap = strokeW * 1.6f

            val values = remoteFloatArray(data)

            for (i in 0..2) {
                val radius = size * 0.38f - i.rf * ringGap
                val progress = values[i.rf]
                val sweepAngle = progress / 100f * 360f

                // Background track.
                paint {
                    color(trackColors[i])
                    style(RcPaintStyle.Stroke)
                    strokeWidth(strokeW.toFloat())
                    strokeCap(RcStrokeCap.Round)
                }
                drawArc(cx - radius, cy - radius, cx + radius, cy + radius, 0f.rf, 360f.rf)

                // Progress arc from 12 o'clock.
                paint { color(ringColors[i]) }
                drawArc(cx - radius, cy - radius, cx + radius, cy + radius, (-90f).rf, sweepAngle)
            }

            // Labels row below rings.
            paint {
                style(RcPaintStyle.Fill)
                strokeWidth(0f)
            }
            for (i in 0..2) {
                val progress = values[i.rf]
                val labelX = w * (0.2f + i.toFloat() * 0.3f)
                val labelY = h * 0.85f
                paint {
                    color(ringColors[i])
                    textSize((density * 22f).toFloat())
                    typeface(RcFontType.Default, RcWeight.Bold, italic = false)
                }
                val pctText =
                    createTextFromFloat(
                        progress,
                        3,
                        0,
                        RcTextFromFloatSpec.of(padPre = RcTextFromFloatSpec.PadPre.None),
                    )
                val pctLabel = pctText + "%"
                drawTextAnchored(pctLabel, labelX, labelY, 0f.rf, 0f.rf, 0)

                paint {
                    textSize((density * 11f).toFloat())
                    typeface(RcFontType.Default, RcWeight.Normal, italic = false)
                }
                val labelText = remoteText(labels[i])
                drawTextAnchored(labelText, labelX, labelY, 0f.rf, 2.5f.rf, 0)
            }
        }
    }
}
