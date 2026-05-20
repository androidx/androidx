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

import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcTextFromFloatSpec
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.min
import androidx.compose.remote.creation.dsl.sin
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * DSL conversion of `examples/ExampleNumbers.kt` `spreadSheet()`.
 *
 * Two grids of `createTextFromFloat` outputs comparing the available formatting flags:
 * - Top grid: default vs `PadAfterZero` vs `PadAfterZero | PadPreZero` vs grouping-by-3.
 * - Bottom grid: variations on grouping (By4, By32, By3) crossed with separator style
 *   (period/comma, comma/period, space/comma, underscore/period).
 *
 * Demonstrates the full surface of typed `RcTextFromFloatSpec.of(...)` — including its `padAfter`,
 * `padPre`, `grouping`, and `separator` sub-enums.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslSpreadSheet(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = componentWidth()
            val h = componentHeight()
            val cx = w / 2f
            val cy = h / 2f
            val rad = min(cx, cy)
            val round = rad / 8f

            paint { color(0xFF0000FF.toInt()) }
            drawRoundRect(0f.rf, 0f.rf, w, h, round, round)

            paint { color(0xFFFFFFFF.toInt()) }
            drawRect(10f.rf, 10f.rf, w - 10f, h - round)

            paint {
                color(0xFFCCCCCC.toInt())
                strokeWidth(5f)
                textSize(30f)
            }
            val header0 = remoteArrayOf("", "", "", "GROUPING_BY3")
            val header1 =
                remoteArrayOf("default", "PAD_AFTER_ZERO", "PAD_AFTER_ZERO", "PAD_AFTER_ZERO")
            val header2 = remoteArrayOf("", "PAD_PRE_NONE", "PAD_PRE_ZERO", "PAD_PRE_ZERO")
            val cellWidth = w / 4f
            val cellHeight = 80f.rf

            loop(round + cellHeight, cellHeight, cy) { k -> drawLine(0f.rf, k, w, k) }
            loop(cy + cellHeight * 2f, cellHeight, h) { k -> drawLine(0f.rf, k, w, k) }

            loop(0f.rf, 1f.rf, 4f.rf) { k ->
                val colPos = k * cellWidth
                paint { color(0xFFCCCCCC.toInt()) }

                drawLine(colPos, round, colPos, h - round)
                val id0 = textLookup(header0, k)
                val id1 = textLookup(header1, k)
                val id2 = textLookup(header2, k)
                paint { color(0xFFFF00FF.toInt()) }
                val centerText = colPos + cellWidth / 2f
                drawTextAnchored(id0, centerText, round + cellHeight / 2f, 0f.rf, (-3.5f).rf, 0)
                drawTextAnchored(id1, centerText, round + cellHeight / 2f, 0f.rf, (-1f).rf, 0)
                drawTextAnchored(id2, centerText, round + cellHeight / 2f, 0f.rf, 1.5f.rf, 0)
            }

            paint {
                color(0xFF000000.toInt())
                textSize(34f)
            }
            loop(1f.rf, 1f.rf, 5f.rf) { y ->
                val yPos = y * cellHeight + cellHeight / 2f + round
                val num = sin(y) * 1000f - 500f
                var centerText = cellWidth * 0.5f
                var spec = RcTextFromFloatSpec.Default
                var tid = createTextFromFloat(num, 6, 2, spec)
                drawTextAnchored(tid, centerText, yPos, 0f.rf, 0f.rf, 0)

                spec =
                    RcTextFromFloatSpec.of(
                        padAfter = RcTextFromFloatSpec.PadAfter.Zero,
                        padPre = RcTextFromFloatSpec.PadPre.None,
                    )
                centerText = cellWidth * 1.5f
                tid = createTextFromFloat(num, 6, 2, spec)
                drawTextAnchored(tid, centerText, yPos, 0f.rf, 0f.rf, 0)

                spec =
                    RcTextFromFloatSpec.of(
                        padAfter = RcTextFromFloatSpec.PadAfter.Zero,
                        padPre = RcTextFromFloatSpec.PadPre.Zero,
                    )
                centerText = cellWidth * 2.5f
                tid = createTextFromFloat(num, 6, 2, spec)
                drawTextAnchored(tid, centerText, yPos, 0f.rf, 0f.rf, 0)

                spec =
                    RcTextFromFloatSpec.of(
                        padAfter = RcTextFromFloatSpec.PadAfter.Zero,
                        padPre = RcTextFromFloatSpec.PadPre.None,
                        grouping = RcTextFromFloatSpec.Grouping.By3,
                    )
                centerText = cellWidth * 3.5f
                tid = createTextFromFloat(num, 6, 2, spec)
                drawTextAnchored(tid, centerText, yPos, 0f.rf, 0f.rf, 0)
            }

            paint {
                color(0xFFCCCCCC.toInt())
                strokeWidth(5f)
                textSize(30f)
            }
            val header20 =
                remoteArrayOf(
                    "PAD_AFTER_ZERO",
                    "PAD_AFTER_ZERO",
                    "PAD_AFTER_ZERO",
                    "PAD_AFTER_ZERO",
                )
            val header21 =
                remoteArrayOf("PAD_PRE_NONE", "PAD_PRE_NONE", "PAD_PRE_ZERO", "PAD_PRE_ZERO")
            val header22 =
                remoteArrayOf("GROUPING_BY4", "GROUPING_BY32", "GROUPING_BY3", "GROUPING_BY3")
            val header23 =
                remoteArrayOf("PERIOD_COMMA", "COMMA_PERIOD", "SPACE_COMMA", "UNDER_PERIOD")

            loop(0f.rf, 1f.rf, 4f.rf) { k ->
                val colPos = k * cellWidth

                val id0 = textLookup(header20, k)
                val id1 = textLookup(header21, k)
                val id2 = textLookup(header22, k)
                val id3 = textLookup(header23, k)
                paint { color(0xFFFF00FF.toInt()) }
                val centerText = colPos + cellWidth / 2f
                drawTextAnchored(id0, centerText, cy + 50f, 0f.rf, (-3.5f).rf, 0)
                drawTextAnchored(id1, centerText, cy + 50f, 0f.rf, (-1f).rf, 0)
                drawTextAnchored(id2, centerText, cy + 50f, 0f.rf, 1.5f.rf, 0)
                drawTextAnchored(id3, centerText, cy + 50f, 0f.rf, 4f.rf, 0)
            }
            paint {
                color(0xFF000000.toInt())
                textSize(34f)
            }
            loop(1f.rf, 1f.rf, 5f.rf) { y ->
                val yPos = y * cellHeight + cellHeight / 2f + round + cy
                val num = ((y % 5f) - 2f) * 1234567.8f
                var centerText = cellWidth * 0.5f
                var spec =
                    RcTextFromFloatSpec.of(
                        padAfter = RcTextFromFloatSpec.PadAfter.Zero,
                        padPre = RcTextFromFloatSpec.PadPre.None,
                        grouping = RcTextFromFloatSpec.Grouping.By4,
                        separator = RcTextFromFloatSpec.Separator.PeriodComma,
                    )

                var tid = createTextFromFloat(num, 10, 2, spec)
                drawTextAnchored(tid, centerText, yPos, 0f.rf, 0f.rf, 0)

                spec =
                    RcTextFromFloatSpec.of(
                        padAfter = RcTextFromFloatSpec.PadAfter.Zero,
                        padPre = RcTextFromFloatSpec.PadPre.None,
                        grouping = RcTextFromFloatSpec.Grouping.By32,
                        separator = RcTextFromFloatSpec.Separator.CommaPeriod,
                    )
                centerText = cellWidth * 1.5f
                tid = createTextFromFloat(num, 10, 2, spec)
                drawTextAnchored(tid, centerText, yPos, 0f.rf, 0f.rf, 0)

                spec =
                    RcTextFromFloatSpec.of(
                        padAfter = RcTextFromFloatSpec.PadAfter.Zero,
                        padPre = RcTextFromFloatSpec.PadPre.None,
                        grouping = RcTextFromFloatSpec.Grouping.By3,
                        separator = RcTextFromFloatSpec.Separator.SpaceComma,
                    )
                centerText = cellWidth * 2.5f
                tid = createTextFromFloat(num, 10, 2, spec)
                drawTextAnchored(tid, centerText, yPos, 0f.rf, 0f.rf, 0)

                spec =
                    RcTextFromFloatSpec.of(
                        padAfter = RcTextFromFloatSpec.PadAfter.Zero,
                        padPre = RcTextFromFloatSpec.PadPre.None,
                        grouping = RcTextFromFloatSpec.Grouping.By3,
                        separator = RcTextFromFloatSpec.Separator.UnderPeriod,
                    )
                centerText = cellWidth * 3.5f
                tid = createTextFromFloat(num, 10, 2, spec)
                drawTextAnchored(tid, centerText, yPos, 0f.rf, 0f.rf, 0)
            }
        }
    }
}
