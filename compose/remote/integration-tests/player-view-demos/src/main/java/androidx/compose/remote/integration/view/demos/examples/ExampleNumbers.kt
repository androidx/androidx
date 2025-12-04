/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.integration.view.demos.examples

import android.graphics.Color
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.Rc.FloatExpression.*
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.min
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.sin
import androidx.compose.remote.tooling.preview.RemoteDocPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Suppress("RestrictedApiAndroidX")
fun spreadSheet(): RemoteComposeWriter {
    val rc =
        RemoteComposeContextAndroid(
            width = 500,
            height = 500,
            contentDescription = "Simple Timer",
            apiLevel = 6,
            profiles = 0,
            platform = AndroidxRcPlatformServices(),
        ) {
            root {
                box(RecordingModifier().fillMaxSize(), BoxLayout.START, BoxLayout.START) {
                    canvas(RecordingModifier().fillMaxSize()) {
                        val w = ComponentWidth() // component.width()
                        val h = ComponentHeight()
                        val cx = w / 2f
                        val cy = h / 2f
                        val rad = min(cx, cy)
                        val round = rad / 8f
                        painter.setColor(Color.BLUE).commit()

                        drawRoundRect(0, 0, w, h, round, round)
                        painter.setColor(Color.WHITE).commit()
                        drawRect(10f, 10, w - 10f, h - round)
                        painter.setColor(Color.LTGRAY).setStrokeWidth(5f).setTextSize(30f).commit()
                        val header0 = addStringList("", "", "", "GROUPING_BY3")
                        val header1 =
                            addStringList(
                                "default",
                                "PAD_AFTER_ZERO",
                                "PAD_AFTER_ZERO",
                                "PAD_AFTER_ZERO",
                            )
                        val header2 =
                            addStringList("", "PAD_PRE_NONE", "PAD_PRE_ZERO", "PAD_PRE_ZERO")
                        val cellWidth = w / 4f
                        val cellHeight = 80f

                        loop(round + cellHeight, cellHeight, cy) { k -> drawLine(0, k, w, k) }
                        loop(cy + cellHeight * 2f, cellHeight, h) { k -> drawLine(0, k, w, k) }

                        loop(0, 1, 4) { k ->
                            val colPos = k * cellWidth
                            painter.setColor(Color.LTGRAY).commit()

                            drawLine(colPos, round, colPos, h - round)
                            val id0 = textLookup(header0, k.toFloat())
                            val id1 = textLookup(header1, k.toFloat())
                            val id2 = textLookup(header2, k.toFloat())
                            painter.setColor(Color.MAGENTA).commit()
                            val centerText = colPos + cellWidth / 2f
                            drawTextAnchored(id0, centerText, round + cellHeight / 2f, 0, -3.5, 0)
                            drawTextAnchored(id1, centerText, round + cellHeight / 2f, 0, -1, 0)
                            drawTextAnchored(id2, centerText, round + cellHeight / 2f, 0, 1.5f, 0)
                        }
                        painter.setColor(Color.BLACK).setTextSize(34f).commit()
                        loop(1, 1, 5) { y ->
                            val yPos = y * cellHeight + cellHeight / 2f + round
                            val num = sin(y) * 1000f - 500f
                            var centerText = cellWidth * 0.5f
                            var mode = 0
                            var tid = createTextFromFloat(num, 6, 2, mode)
                            drawTextAnchored(tid, centerText, yPos, 0, 0, 0)

                            mode = Rc.TextFromFloat.PAD_AFTER_ZERO or Rc.TextFromFloat.PAD_PRE_NONE
                            centerText = cellWidth * 1.5f
                            tid = createTextFromFloat(num, 6, 2, mode)
                            drawTextAnchored(tid, centerText, yPos, 0, 0, 0)

                            mode = Rc.TextFromFloat.PAD_AFTER_ZERO or Rc.TextFromFloat.PAD_PRE_ZERO
                            centerText = cellWidth * 2.5f
                            tid = createTextFromFloat(num, 6, 2, mode)
                            drawTextAnchored(tid, centerText, yPos, 0, 0, 0)
                            mode =
                                Rc.TextFromFloat.PAD_AFTER_ZERO or
                                    Rc.TextFromFloat.PAD_PRE_NONE or
                                    Rc.TextFromFloat.GROUPING_BY3
                            centerText = cellWidth * 3.5f
                            tid = createTextFromFloat(num, 6, 2, mode)
                            drawTextAnchored(tid, centerText, yPos, 0, 0, 0)
                        }

                        painter.setColor(Color.LTGRAY).setStrokeWidth(5f).setTextSize(30f).commit()
                        val header20 =
                            addStringList(
                                "PAD_AFTER_ZERO",
                                "PAD_AFTER_ZERO",
                                "PAD_AFTER_ZERO",
                                "PAD_AFTER_ZERO",
                            )
                        val header21 =
                            addStringList(
                                "PAD_PRE_NONE",
                                "PAD_PRE_NONE",
                                "PAD_PRE_ZERO",
                                "PAD_PRE_ZERO",
                            )
                        val header22 =
                            addStringList(
                                "GROUPING_BY4",
                                "GROUPING_BY32",
                                "GROUPING_BY3",
                                "GROUPING_BY3",
                            )
                        val header23 =
                            addStringList(
                                "PERIOD_COMMA",
                                "COMMA_PERIOD",
                                "SPACE_COMMA",
                                "UNDER_PERIOD",
                            )

                        loop(0, 1, 4) { k ->
                            val colPos = k * cellWidth

                            val id0 = textLookup(header20, k.toFloat())
                            val id1 = textLookup(header21, k.toFloat())
                            val id2 = textLookup(header22, k.toFloat())
                            val id3 = textLookup(header23, k.toFloat())
                            painter.setColor(Color.MAGENTA).commit()
                            val centerText = colPos + cellWidth / 2f
                            drawTextAnchored(id0, centerText, cy + 50f, 0, -3.5, 0)
                            drawTextAnchored(id1, centerText, cy + 50f, 0, -1, 0)
                            drawTextAnchored(id2, centerText, cy + 50f, 0, 1.5f, 0)
                            drawTextAnchored(id3, centerText, cy + 50f, 0, 4f, 0)
                        }
                        painter.setColor(Color.BLACK).setTextSize(34f).commit()
                        loop(1, 1, 5) { y ->
                            val yPos = y * cellHeight + cellHeight / 2f + round + cy
                            val num = ((y % 5f) - 2) * 1234567.8f
                            var centerText = cellWidth * 0.5f
                            var mode =
                                Rc.TextFromFloat.PAD_AFTER_ZERO or
                                    Rc.TextFromFloat.PAD_PRE_NONE or
                                    Rc.TextFromFloat.GROUPING_BY4 or
                                    Rc.TextFromFloat.SEPARATOR_PERIOD_COMMA

                            var tid = createTextFromFloat(num, 10, 2, mode)
                            drawTextAnchored(tid, centerText, yPos, 0, 0, 0)

                            mode =
                                Rc.TextFromFloat.PAD_AFTER_ZERO or
                                    Rc.TextFromFloat.PAD_PRE_NONE or
                                    Rc.TextFromFloat.GROUPING_BY32 or
                                    Rc.TextFromFloat.SEPARATOR_COMMA_PERIOD
                            centerText = cellWidth * 1.5f
                            tid = createTextFromFloat(num, 10, 2, mode)
                            drawTextAnchored(tid, centerText, yPos, 0, 0, 0)

                            mode =
                                Rc.TextFromFloat.PAD_AFTER_ZERO or
                                    Rc.TextFromFloat.PAD_PRE_NONE or
                                    Rc.TextFromFloat.GROUPING_BY3 or
                                    Rc.TextFromFloat.SEPARATOR_SPACE_COMMA
                            centerText = cellWidth * 2.5f
                            tid = createTextFromFloat(num, 10, 2, mode)
                            drawTextAnchored(tid, centerText, yPos, 0, 0, 0)
                            mode =
                                Rc.TextFromFloat.PAD_AFTER_ZERO or
                                    Rc.TextFromFloat.PAD_PRE_NONE or
                                    Rc.TextFromFloat.GROUPING_BY3 or
                                    Rc.TextFromFloat.SEPARATOR_UNDER_PERIOD
                            centerText = cellWidth * 3.5f
                            tid = createTextFromFloat(num, 10, 2, mode)
                            drawTextAnchored(tid, centerText, yPos, 0, 0, 0)
                        }
                    }
                }
            }
        }
    return rc.writer
}

@Preview @Composable fun SpreadSheetPreview() = RemoteDocPreview(spreadSheet())
