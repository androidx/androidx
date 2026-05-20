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
import androidx.compose.remote.creation.dsl.sin
import androidx.compose.remote.creation.dsl.toRad
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * DSL conversion of `examples/DataVizDemos.kt` `demoStepProgressArc()`.
 *
 * Semicircular progress arc for daily steps with milestone tick marks at 25/50/75/100%, a centre
 * step-count, and goal label below.
 *
 * Exercises:
 * - Host-side iteration over a constant `intArrayOf(25, 50, 75, 100)` to render labels.
 * - `toRad(...)`, `cos(...)`, `sin(...)` typed math.
 * - Mixed-float drawArc still requires `.rf` coercion (G9 workaround).
 */
@Suppress("RestrictedApiAndroidX")
public fun dslDemoStepProgressArc(): ByteArray {
    val data = floatArrayOf(7250f, 10000f) // currentSteps, goal

    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 350),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Step Progress Arc"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFF16213E.toInt())) {
            val w = componentWidth()
            val h = componentHeight()
            val cx = w / 2f
            val cy = h * 0.55f
            val density = density()
            val size = min(w, h)
            val arcRadius = size * 0.4f
            val strokeW = size * 0.07f

            val values = remoteFloatArray(data)
            val current = values[0.rf]
            val goal = values[1.rf]
            val progress = min(current / goal, 1f.rf)

            // Background track (semicircle, 180° from left to right).
            paint {
                color(0x33FFFFFF)
                style(RcPaintStyle.Stroke)
                strokeWidth(strokeW.toFloat())
                strokeCap(RcStrokeCap.Round)
            }
            drawArc(
                cx - arcRadius,
                cy - arcRadius,
                cx + arcRadius,
                cy + arcRadius,
                180f.rf,
                180f.rf,
            )

            // Progress arc.
            paint { color(0xFF00D2FF.toInt()) }
            drawArc(
                cx - arcRadius,
                cy - arcRadius,
                cx + arcRadius,
                cy + arcRadius,
                180f.rf,
                progress * 180f,
            )

            // Milestone markers at 25%, 50%, 75%, 100%.
            paint {
                color(0x88FFFFFF.toInt())
                strokeWidth((density * 2f).toFloat())
            }
            val tickLen = strokeW * 0.6f
            for (pct in intArrayOf(25, 50, 75, 100)) {
                val angle = 180f + pct.toFloat() / 100f * 180f
                val rad = toRad(angle.rf)
                val outerR = arcRadius + strokeW / 2f + density * 2f
                val innerR = arcRadius + strokeW / 2f + tickLen
                val tx1 = cx + outerR * cos(rad)
                val ty1 = cy + outerR * sin(rad)
                val tx2 = cx + innerR * cos(rad)
                val ty2 = cy + innerR * sin(rad)
                drawLine(tx1, ty1, tx2, ty2)

                // Label.
                paint {
                    style(RcPaintStyle.Fill)
                    textSize((density * 10f).toFloat())
                }
                val lR = arcRadius + strokeW / 2f + tickLen + density * 10f
                drawTextAnchored(
                    remoteText("$pct%"),
                    cx + lR * cos(rad),
                    cy + lR * sin(rad),
                    0f.rf,
                    0f.rf,
                    0,
                )
                paint { style(RcPaintStyle.Stroke) }
            }

            // Step count text.
            paint {
                color(0xFFFFFFFF.toInt())
                style(RcPaintStyle.Fill)
                textSize((density * 40f).toFloat())
                typeface(RcFontType.Default, RcWeight.Bold, italic = false)
            }
            val stepsText =
                createTextFromFloat(
                    current,
                    5,
                    0,
                    RcTextFromFloatSpec.of(padPre = RcTextFromFloatSpec.PadPre.None),
                )
            drawTextAnchored(stepsText, cx, cy - density * 15f, 0f.rf, 0f.rf, 0)

            // Goal text.
            paint {
                color(0xAAFFFFFF.toInt())
                textSize((density * 16f).toFloat())
                typeface(RcFontType.Default, RcWeight.Normal, italic = false)
            }
            val goalNumber =
                createTextFromFloat(
                    goal,
                    5,
                    0,
                    RcTextFromFloatSpec.of(padPre = RcTextFromFloatSpec.PadPre.None),
                )
            val goalLabel = textMerge(textMerge(remoteText("/ "), goalNumber), remoteText(" steps"))
            drawTextAnchored(goalLabel, cx, cy + density * 8f, 0f.rf, 0f.rf, 0)
        }
    }
}
