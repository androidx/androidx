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
import androidx.compose.remote.creation.dsl.min
import androidx.compose.remote.creation.dsl.rcColor
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * DSL conversion of `examples/DataVizDemos.kt` `demoMoonPhaseDial()`.
 *
 * Circular moon dial with illuminated/shadow halves split by a vertical terminator (rectangular
 * clip), phase name and illumination percentage below.
 *
 * Note: rect-clipped circle gives a vertical-line terminator (not a curved one). The original notes
 * this is a known simplification (a curved terminator needs an `addClipOval`/`addClipCircle` op
 * which doesn't exist yet).
 */
@Suppress("RestrictedApiAndroidX")
public fun dslDemoMoonPhaseDial(): ByteArray {
    val data = floatArrayOf(0.72f, 3f) // illumination, phaseIndex (Waxing Gibbous)
    val phaseNames =
        arrayOf(
            "New Moon",
            "Waxing Crescent",
            "First Quarter",
            "Waxing Gibbous",
            "Full Moon",
            "Waning Gibbous",
            "Last Quarter",
            "Waning Crescent",
        )

    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Moon Phase Dial"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFF0B0D17.toInt())) {
            val w = componentWidth()
            val h = componentHeight()
            val cx = w / 2f
            val cy = h * 0.42f
            val density = density()
            val size = min(w, h)
            val moonR = size * 0.3f

            val values = remoteFloatArray(data)
            val illumination = values[0.rf]
            val phaseIdx = values[1.rf]
            val nameList = remoteArrayOf(*phaseNames)

            // Outer glow ring.
            paint {
                color(0x22FFFFFF)
                style(RcPaintStyle.Stroke)
                strokeWidth((density * 8f).toFloat())
            }
            drawCircle(cx, cy, moonR + density * 6f)

            // Full bright moon surface.
            paint {
                color(0xFFE8E8D0.toInt())
                style(RcPaintStyle.Fill)
            }
            drawCircle(cx, cy, moonR)

            // Subtle surface texture (dark spots).
            paint { color(0x15000000) }
            drawCircle(cx - moonR * 0.2f, cy - moonR * 0.15f, moonR * 0.15f)
            drawCircle(cx + moonR * 0.25f, cy + moonR * 0.1f, moonR * 0.1f)
            drawCircle(cx - moonR * 0.1f, cy + moonR * 0.3f, moonR * 0.12f)

            // Shadow overlay clipped to a vertical band (the unilluminated portion).
            save {
                val shadowLeft = cx - moonR
                val shadowRight = cx + moonR - illumination * moonR * 2f
                clipRect(shadowLeft, cy - moonR, shadowRight, cy + moonR)
                paint { color(0xDD0B0D17.rcColor()) }
                drawCircle(cx, cy, moonR)
            }

            // Moon outline.
            paint {
                color(0x33FFFFFF.rcColor())
                style(RcPaintStyle.Stroke)
                strokeWidth((density * 1f).toFloat())
            }
            drawCircle(cx, cy, moonR)

            // Phase name.
            val phaseName = textLookup(nameList, phaseIdx)
            paint {
                color(0xFFFFFFFF.toInt())
                style(RcPaintStyle.Fill)
                textSize((density * 18f).toFloat())
                typeface(RcFontType.Default, RcWeight.SemiBold, italic = false)
            }
            drawTextAnchored(phaseName, cx, h * 0.74f, 0f.rf, 0f.rf, 0)

            // Illumination percentage.
            paint {
                color(0xAAFFFFFF.toInt())
                textSize((density * 14f).toFloat())
                typeface(RcFontType.Default, RcWeight.Normal, italic = false)
            }
            val illumText =
                createTextFromFloat(
                    illumination * 100f,
                    3,
                    0,
                    RcTextFromFloatSpec.of(padPre = RcTextFromFloatSpec.PadPre.None),
                )
            val illumLabel = textMerge(illumText, remoteText("% illuminated"))
            drawTextAnchored(illumLabel, cx, h * 0.81f, 0f.rf, 0f.rf, 0)

            // Decorative stars.
            paint {
                color(0x66FFFFFF.rcColor())
                textSize((density * 8f).toFloat())
            }
            val starPositions =
                floatArrayOf(
                    0.1f,
                    0.15f,
                    0.85f,
                    0.2f,
                    0.15f,
                    0.7f,
                    0.9f,
                    0.65f,
                    0.75f,
                    0.1f,
                    0.3f,
                    0.9f,
                )
            for (s in 0 until starPositions.size / 2) {
                drawTextAnchored(
                    remoteText("·"),
                    w * starPositions[s * 2],
                    h * starPositions[s * 2 + 1],
                    0f.rf,
                    0f.rf,
                    0,
                )
            }
        }
    }
}
