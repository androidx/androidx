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
import androidx.compose.remote.creation.dsl.RcTileMode
import androidx.compose.remote.creation.dsl.RcWeight
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.min
import androidx.compose.remote.creation.dsl.sin
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * DSL conversion of `examples/DataVizDemos.kt` `demoHydrationWave()`.
 *
 * Cup-count progress as a sinusoidal water surface inside a rounded-rect glass, clipped to the
 * glass interior. Percentage, cup count and goal labels render outside the clip.
 *
 * Exercises:
 * - `remotePath(...)` building a wave path via `lineTo` inside `loop { … }`.
 * - `save { clipRect(...); drawPath(...) }` with the block-form save.
 * - `paint { linearGradient(...) }` for the water gradient.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslDemoHydrationWave(): ByteArray {
    val data = floatArrayOf(5f, 8f) // cupsConsumed, goalCups

    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 450),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Hydration Progress Wave"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFF0F172A.toInt())) {
            val w = componentWidth()
            val h = componentHeight()
            val cx = w / 2f
            val density = density()

            val values = remoteFloatArray(data)
            val cups = values[0.rf]
            val goal = values[1.rf]
            val progress = min(cups / goal, 1f.rf)

            // Container dimensions (glass shape).
            val glassW = w * 0.45f
            val glassH = h * 0.55f
            val glassLeft = cx - glassW / 2f
            val glassRight = cx + glassW / 2f
            val glassTop = h * 0.18f
            val glassBottom = glassTop + glassH
            val cornerR = density * 12f

            // Glass outline.
            paint {
                color(0x44FFFFFF)
                style(RcPaintStyle.Stroke)
                strokeWidth((density * 2f).toFloat())
            }
            drawRoundRect(
                glassLeft,
                glassTop,
                glassLeft + glassW,
                glassTop + glassH,
                cornerR,
                cornerR,
            )

            // Wave fill - animated sinusoidal surface.
            val fillH = progress * glassH
            val waveY = glassBottom - fillH
            val waveAmp = density * 6f
            val waveFreq = 3.14159f * 4f // 2 full waves across glass

            // Build wave path: bottom-left → wave surface → bottom-right → close.
            val wavePath = remotePath(glassLeft.toFloat(), glassBottom.toFloat())

            val waveSteps = 40f
            loop(0f.rf, 1f.rf, (waveSteps + 1f).rf) { step ->
                val t = (step / waveSteps).flush()
                val x = (glassLeft + t * glassW).flush()
                val fl = (t * waveFreq + continuousSeconds() * 3f).flush()
                val y = waveY + sin(fl) * waveAmp
                wavePath.lineTo(x.toFloat(), y.toFloat())
            }

            // Close: right side down, across bottom, back up.
            wavePath.lineTo(glassRight.toFloat(), glassBottom.toFloat())
            wavePath.close()

            // Fill with water gradient.
            paint {
                style(RcPaintStyle.Fill)
                linearGradient(
                    startX = glassLeft.toFloat(),
                    startY = waveY.toFloat(),
                    endX = glassLeft.toFloat(),
                    endY = glassBottom.toFloat(),
                    colors = intArrayOf(0xFF38BDF8.toInt(), 0xFF0284C7.toInt()),
                    tileMode = RcTileMode.Clamp,
                )
            }

            // Clip to glass interior and draw the wave.
            save {
                clipRect(
                    (glassLeft + density * 2f).toFloat(),
                    (glassTop + density * 2f).toFloat(),
                    (glassRight - density * 2f).toFloat(),
                    (glassBottom - density * 2f).toFloat(),
                )
                drawPath(wavePath.getPath())
            }
            paint { raw.setShader(0) }

            // Cup count display.
            paint {
                color(0xFFFFFFFF.toInt())
                style(RcPaintStyle.Fill)
                textSize((density * 42f).toFloat())
                typeface(RcFontType.Default, RcWeight.Bold, italic = false)
            }
            val cupsText =
                createTextFromFloat(
                    cups,
                    1,
                    0,
                    RcTextFromFloatSpec.of(padPre = RcTextFromFloatSpec.PadPre.None),
                )
            drawTextAnchored(cupsText, cx, h * 0.83f, 0f.rf, 0f.rf, 0)

            paint {
                color(0xAAFFFFFF.toInt())
                textSize((density * 16f).toFloat())
                typeface(RcFontType.Default, RcWeight.Normal, italic = false)
            }
            val goalText =
                createTextFromFloat(
                    goal,
                    1,
                    0,
                    RcTextFromFloatSpec.of(padPre = RcTextFromFloatSpec.PadPre.None),
                )
            val label = textMerge(textMerge(remoteText("of "), goalText), remoteText(" cups"))
            drawTextAnchored(label, cx, h * 0.90f, 0f.rf, 0f.rf, 0)

            // Progress percentage inside glass.
            paint {
                color(0xCCFFFFFF.toInt())
                textSize((density * 20f).toFloat())
                typeface(RcFontType.Default, RcWeight.Bold, italic = false)
            }
            val pctVal = progress * 100f
            val pctText =
                createTextFromFloat(
                    pctVal,
                    3,
                    0,
                    RcTextFromFloatSpec.of(padPre = RcTextFromFloatSpec.PadPre.None),
                )
            val pctLabel = textMerge(pctText, remoteText("%"))
            drawTextAnchored(pctLabel, cx, glassTop + glassH * 0.4f, 0f.rf, 0f.rf, 0)

            // Water drop icon + label.
            paint {
                color(0xFF38BDF8.toInt())
                textSize((density * 24f).toFloat())
            }
            drawTextAnchored(remoteText("⬩"), cx, h * 0.10f, 0f.rf, 0f.rf, 0)
            paint {
                color(0xFFFFFFFF.toInt())
                textSize((density * 14f).toFloat())
            }
            drawTextAnchored(remoteText("Hydration"), cx, h * 0.10f, 0f.rf, 3f.rf, 0)
        }
    }
}
