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

package androidx.compose.remote.integration.view.demos.examples

// import androidx.compose.remote.creation.onDark
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.TouchExpression
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.abs
import androidx.compose.remote.creation.cos
import androidx.compose.remote.creation.div
import androidx.compose.remote.creation.minus
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.plus
import androidx.compose.remote.creation.rf
import androidx.compose.remote.creation.times

@Suppress("RestrictedApiAndroidX")
fun demoMoonPhases(): RemoteComposeContextAndroid {
    val rc =
        RemoteComposeContextAndroid(
            platform = AndroidxRcPlatformServices(),
            apiLevel = 7,
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 500),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 500),
            RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Moon Phases Demo"),
            RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
        ) {
            val density = rf(Rc.System.DENSITY)
            root {
                box(
                    RecordingModifier().fillMaxSize().background(0xFF111111.toInt()),
                    BoxLayout.CENTER,
                    BoxLayout.CENTER,
                ) {
                    canvas(RecordingModifier().fillMaxSize()) {
                        val w = ComponentWidth()
                        val h = ComponentHeight()
                        val cx = w / 2f
                        val cy = h / 2f
                        val moonRadius = 150f * density

                        // Drag up/down to change date (1 pixel = 0.1 days)
                        val daysOffset =
                            rf(
                                touchExpression(
                                    RemoteContext.FLOAT_TOUCH_POS_Y,
                                    -0.1f,
                                    Rc.FloatExpression.MUL,
                                    defValue = 0f,
                                    min = -32f,
                                    max = 32f,
                                    touchMode = TouchExpression.STOP_GENTLY,
                                )
                            )

                        // 1738144800 is a New Moon (Jan 29, 2025)
                        val refTime = 1738144800L
                        val secondsPerDay = 86400f
                        val lunarCycle = 29.53059f

                        val currentTimeSeconds =
                            rf(Utils.asNan(RemoteContext.INT_EPOCH_SECOND.toInt()))
                        val targetTimeSeconds = currentTimeSeconds + daysOffset * secondsPerDay

                        val daysSinceRef = (targetTimeSeconds - refTime.toFloat()) / secondsPerDay
                        val phase = (daysSinceRef % lunarCycle + lunarCycle) % lunarCycle
                        val normalizedPhase = phase / lunarCycle // 0 to 1

                        // Draw Moon Background (Dark side)
                        painter.setColor(0xFF333333.toInt()).setStyle(Paint.Style.FILL).commit()
                        drawCircle(cx, cy, moonRadius)

                        // Draw Illuminated Part
                        // phase 0: New, 0.25: First Quarter, 0.5: Full, 0.75: Last Quarter
                        painter.setColor(0xFFCCCCCC.toInt()).setStyle(Paint.Style.FILL).commit()

                        // The terminator is an ellipse with width varying by cos
                        val terminatorW = abs(cos(normalizedPhase * 2f * 3.14159f)) * moonRadius

                        // Simplified logic for demo:
                        // If phase < 0.5, illumination is on the right.
                        // If phase > 0.5, illumination is on the left.
                        // This is a rough approximation for a procedural demo.

                        ifElse(
                            normalizedPhase - 0.5f,
                            {
                                // Waning (Left side illuminated)
                                drawSector(
                                    cx - moonRadius,
                                    cy - moonRadius,
                                    cx + moonRadius,
                                    cy + moonRadius,
                                    90f,
                                    180f,
                                )
                                ifElse(
                                    normalizedPhase - 0.75f,
                                    {
                                        // 0.75 to 1.0 (Crescent)
                                        painter.setColor(0xFF333333.toInt()).commit()
                                        drawOval(
                                            cx - terminatorW,
                                            cy - moonRadius,
                                            cx + terminatorW,
                                            cy + moonRadius,
                                        )
                                    },
                                    {
                                        // 0.5 to 0.75 (Gibbous)
                                        drawOval(
                                            cx - terminatorW,
                                            cy - moonRadius,
                                            cx + terminatorW,
                                            cy + moonRadius,
                                        )
                                    },
                                )
                            },
                            {
                                // Waxing (Right side illuminated)
                                drawSector(
                                    cx - moonRadius,
                                    cy - moonRadius,
                                    cx + moonRadius,
                                    cy + moonRadius,
                                    270f,
                                    180f,
                                )
                                ifElse(
                                    normalizedPhase - 0.25f,
                                    {
                                        // 0.25 to 0.5 (Gibbous)
                                        drawOval(
                                            cx - terminatorW,
                                            cy - moonRadius,
                                            cx + terminatorW,
                                            cy + moonRadius,
                                        )
                                    },
                                    {
                                        // 0 to 0.25 (Crescent)
                                        painter.setColor(0xFF333333.toInt()).commit()
                                        drawOval(
                                            cx - terminatorW,
                                            cy - moonRadius,
                                            cx + terminatorW,
                                            cy + moonRadius,
                                        )
                                    },
                                )
                            },
                        )

                        // Date display
                        // timeAttribute takes an Int ID for the long value
                        // Since targetTimeSeconds is a Float expression, we need to convert it?
                        // Actually, the system might have a way to treat a float expression as a
                        // time.
                        // Let's use the provided targetTimeSeconds directly if possible.

                        // RemoteComposeWriter.addTimeAttribute(int id, int longId, short type,
                        // int[] args)
                        // We don't have a direct RInt from an RFloat expression easily without more
                        // ops.
                        // For this demo, we'll show the days offset as a proxy or try to use the
                        // system time.

                        painter
                            .setColor(Color.WHITE)
                            .setTextSize((24f * density).toFloat())
                            .commit()
                        val offsetText =
                            textMerge(
                                textCreateId("Days Offset: "),
                                createTextFromFloat(daysOffset, 4, 1, 0),
                            )
                        drawTextAnchored(
                            offsetText,
                            cx,
                            cy + moonRadius + 40f * density,
                            0.5f,
                            0f,
                            0,
                        )

                        //                    onDark {
                        //                        painter.setColor(Color.LTGRAY).setTextSize(18f *
                        // density).commit()
                        //                        val msg = textCreateId("Drag Up/Down to change
                        // date")
                        //                        drawTextAnchored(msg, cx, cy + moonRadius + 80f *
                        // density, 0.5f, 0f, 0)
                        //                    }
                    }
                }
            }
        }
    return rc
}

@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.drawCircle(x: Number, y: Number, rad: Number) {
    drawCircle(x.toFloat(), y.toFloat(), rad.toFloat())
}

@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.drawOval(x: Number, y: Number, w: Number, h: Number) {
    drawOval(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat())
}
