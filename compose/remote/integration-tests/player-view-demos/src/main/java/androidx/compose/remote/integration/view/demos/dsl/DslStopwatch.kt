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

import android.graphics.Paint
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.dsl.*
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.profile.RcPlatformProfiles

@Suppress("RestrictedApiAndroidX")
public fun dslStopwatchDemo(): ByteArray {
    return createRawRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.FEATURE_MEASURE_VERSION, 2),
        experimental = true,
    ) {
        val len = 5f
        val lapTimes = remoteDynamicFloatArray(len)

        setArrayValue(lapTimes, 0.rf, 1.rf)
        setArrayValue(lapTimes, 1.rf, 2.rf)
        setArrayValue(lapTimes, 2.rf, 3.rf)
        setArrayValue(lapTimes, 3.rf, 4.rf)
        setArrayValue(lapTimes, 4.rf, 5.rf)

        RcRoot {
            Column(
                modifier = Modifier.fillMaxSize().background(0xFF0F172A.toInt()).padding(24f),
                horizontal = RcHorizontalPositioning.Center,
            ) {
                val start = remoteNamedFloat("start", 0f)
                val startTime = remoteNamedFloat("startTime", 0f)
                val lapCount = remoteNamedFloat("lapCount", 0f)
                val clock = animationTime().flush()
                val timeExpr = (start * (clock - startTime)).flush()

                val minutes = floor(timeExpr / 60f)
                val seconds = floor(timeExpr % 60f)
                val centiseconds = floor((timeExpr % 1f) * 100f).anim(0.1f)

                val padSpec =
                    RcTextFromFloatSpec.of(
                        padPre = RcTextFromFloatSpec.PadPre.Zero,
                        padAfter = RcTextFromFloatSpec.PadAfter.None,
                    )
                val minutesStr = createTextFromFloat(minutes, 2, 0, padSpec)
                val secondsStr = createTextFromFloat(seconds, 2, 0, padSpec)
                val centisecondsStr = createTextFromFloat(centiseconds, 2, 0, padSpec)

                val display = minutesStr + ":" + secondsStr + "." + centisecondsStr
                // Main stopwatch dial canvas
                Box(modifier = Modifier.size(400f)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = componentWidth()
                        val h = componentHeight()
                        val cx = w / 2f
                        val cy = h / 2f
                        val radius = min(cx, cy) - 12f

                        // Glowing teal active border
                        paint {
                            color(0xFF0D9488.toInt())
                            raw.setStyle(Paint.Style.STROKE)
                            strokeWidth(8f)
                        }
                        drawCircle(cx.toFloat(), cy.toFloat(), radius.toFloat())

                        // Large digital time display centered inside the canvas
                        paint {
                            color(0xFFFFFFFF.toInt())
                            textSize(64f)
                            raw.setStyle(Paint.Style.FILL)

                            strokeWidth(0f)
                        }

                        drawTextAnchored(display, cx, cy, 0f.rf, 0f.rf, RcTextAnchorFlags.None)
                    }
                }

                Box(modifier = Modifier.size(24f)) {}

                // 3 Buttons (Text) in a row: Start, Stop, Lap
                Row(
                    modifier = Modifier.fillParentMaxWidth(0.95f),
                    vertical = RcVerticalPositioning.Center,
                    horizontal = RcRowHorizontalPositioning.SpaceEvenly,
                ) {
                    // START button
                    Box(
                        modifier =
                            Modifier.background(0xFF059669.toInt()) // Emerald Green
                                .clip(RoundedRectShape(16f, 16f, 16f, 16f))
                                .padding(20f, 12f, 20f, 12f)
                                .onClick {
                                    setValue(start, 1f)
                                    setValue(startTime, clock)
                                }
                    ) {
                        Text("START", color = 0xFFFFFFFF.rcColor(), fontSize = 64.rsp)
                    }

                    // STOP button
                    Box(
                        modifier =
                            Modifier.background(0xFFDC2626.toInt()) // Crimson Red
                                .clip(RoundedRectShape(16f, 16f, 16f, 16f))
                                .padding(20f, 12f, 20f, 12f)
                                .onClick { setValue(start, 0f) }
                    ) {
                        Text("STOP", color = 0xFFFFFFFF.rcColor(), fontSize = 64.rsp)
                    }

                    // LAP button

                    Box(
                        modifier =
                            Modifier.background(0xFF0891B2.toInt()) // Cyan
                                .clip(RoundedRectShape(16f, 16f, 16f, 16f))
                                .padding(20f, 12f, 20f, 12f)
                                .onClick { setValue(lapCount, (lapCount + 1f) % len) }
                    ) {
                        Text("LAP", color = 0xFFFFFFFF.rcColor(), fontSize = 64.rsp)
                    }
                }

                Box(modifier = Modifier.size(24f)) {}

                // Canvas where a scrolling set of Laps is shown inside vertical scroll
                Column(
                    modifier =
                        Modifier.fillParentMaxWidth(0.9f)
                            .fillMaxHeight()
                            .background(0x15FFFFFF)
                            .clip(RoundedRectShape(16f, 16f, 16f, 16f))
                            .border(1f, 16f, 0x33FFFFFF.rcColor(), RcBorderShape.RoundedRectangle)
                            .verticalScroll()
                ) {
                    Canvas(modifier = Modifier.fillParentMaxWidth().height(250f)) {
                        val fontHeight = 64.rf
                        val cx = componentWidth() / 2f

                        lapTimes[lapCount] = timeExpr

                        save {
                            loop(0.rf, 1.rf, lapCount) { i ->
                                val centiseconds = floor((lapTimes[i] % 1f) * 100f)

                                val secondsStr = createTextFromFloat(lapTimes[i], 2, 0, padSpec)
                                val minutesStr =
                                    createTextFromFloat(lapTimes[i] / 60f, 2, 0, padSpec)
                                val centisecondsStr =
                                    createTextFromFloat(centiseconds, 2, 0, padSpec)

                                val display = minutesStr + ":" + secondsStr + "." + centisecondsStr
                                paint {
                                    color(0xFFE2E8F0.toInt())
                                    textSize(fontHeight)
                                }
                                drawTextAnchored(
                                    display,
                                    cx,
                                    fontHeight,
                                    0f.rf,
                                    i * 3f,
                                    RcTextAnchorFlags.None,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
