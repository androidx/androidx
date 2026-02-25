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

import android.graphics.Paint
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteContext.FLOAT_API_LEVEL
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.cos
import androidx.compose.remote.creation.min
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.plus
import androidx.compose.remote.creation.times
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Suppress("RestrictedApiAndroidX")
fun MClock(): RemoteComposeWriter {
    val rc =
        RemoteComposeContextAndroid(
            width = 500,
            height = 500,
            contentDescription = "Simple Timer",
            apiLevel = 7,
            profiles = RcProfiles.PROFILE_ANDROIDX,
            platform = AndroidxRcPlatformServices(),
        ) {
            val color = MClockColorPack(this)
            root {
                val dayNamesId = addStringList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val day: Int = textLookup(dayNamesId, (DayOfWeek() - 1).toFloat())
                val dom = createTextFromFloat(DayOfMonth(), 2, 0, 0)
                val date = textMerge(day, dom)

                box(RecordingModifier().fillMaxSize(), BoxLayout.START, BoxLayout.START) {
                    canvas(RecordingModifier().fillMaxSize()) {
                        val minX = 0
                        val maxX = Math.PI.toFloat() * 2f
                        val w = ComponentWidth() // component.width()
                        val h = ComponentHeight()
                        val cx = (w / 2f).flush()
                        val cy = (h / 2f).flush()
                        val rad = min(cx, cy).flush()
                        val strokeWidth = (rad / 6f).toFloat()
                        val equ = rFun { x -> rad * (0.97f + 0.03f * cos(x * 12f)) }

                        val textSize = rad / 5f
                        painter
                            .setColorId(color.backgroundId.toInt())
                            .setTextSize(textSize.toFloat())
                            .commit()

                        val pathId =
                            addPolarPathExpression(
                                equ,
                                minX,
                                maxX,
                                64,
                                centerX = cx,
                                centerY = cy,
                                Rc.PathExpression.SPLINE_PATH,
                            )
                        drawPath(pathId)

                        painter
                            .setColorId(color.hrId.toInt())
                            .setStrokeWidth(strokeWidth)
                            .setStrokeCap(Paint.Cap.ROUND)
                            .commit()
                        val hrHand = (Hour() + (Minutes() % 60f) / 60f) * 30f
                        save() {
                            rotate(hrHand, cx, cy)
                            drawLine(cx, cy, cx, cy - rad / 3f)
                        }

                        painter
                            .setColorId(color.minutesColorId.toInt())
                            .setStrokeWidth(strokeWidth)
                            .setStrokeCap(Paint.Cap.ROUND)
                            .commit()
                        save() {
                            rotate(Minutes() * 6f, cx, cy)
                            drawLine(cx, cy, cx, cy - rad * .6f)
                        }

                        val textPath =
                            addPolarPathExpression(
                                rFun { y -> rad * 0.7f },
                                minX,
                                maxX,
                                64,
                                centerX = cx,
                                centerY = cy,
                                Rc.PathExpression.SPLINE_PATH,
                            )

                        save() {
                            rotate(Seconds() * 6f, cx, cy)
                            val radius = rad * 0.1f
                            painter
                                .setStyle(Paint.Style.FILL)
                                .setColorId(color.dotColorId.toInt())
                                .commit()
                            drawCircle(
                                cx.toFloat(),
                                (cy - rad + (2f * radius)).toFloat(),
                                radius.toFloat(),
                            )
                            rotate(70f, cx, cy)
                            painter.setColorId(color.textColorId.toInt()).commit()
                            drawTextOnPath(date, textPath, 0f, 0f)
                        }
                        val versionId = writer.createTextFromFloat(FLOAT_API_LEVEL, 2, 2, 0)
                        drawTextAnchored(versionId, cx, (cy + h) / 2f, 0, 0, 0)
                    }
                }
            }
        }
    return rc.writer
}

@Suppress("RestrictedApiAndroidX")
private class MClockColorPack(val rc: RemoteComposeContextAndroid) {
    val backgroundId: Short
        get() {
            rc.beginGlobal()
            val color =
                rc.writer.addThemedColor(
                    "color.system_accent2_50",
                    0xFF113311.toInt(),
                    "color.system_accent2_800",
                    0xFFFF9966.toInt(),
                )
            rc.endGlobal()
            return color
        }

    val minutesColorId: Short
        get() {
            rc.beginGlobal()
            val color =
                rc.writer.addThemedColor(
                    "color.system_accent1_500",
                    0xFF113311.toInt(),
                    "color.system_accent1_100",
                    0xFFFF9966.toInt(),
                )
            rc.endGlobal()
            return color
        }

    val textColorId: Short
        get() {
            rc.beginGlobal()
            val color =
                rc.writer.addThemedColor(
                    "color.system_on_surface_light",
                    0xFF113311.toInt(),
                    "color.system_on_surface_dark",
                    0xFFFF9966.toInt(),
                )
            rc.endGlobal()
            return color
        }

    val dotColorId: Short
        get() {
            rc.beginGlobal()
            val color =
                rc.writer.addThemedColor(
                    "color.system_accent3_500",
                    0xFF113311.toInt(),
                    "color.system_accent3_100",
                    0xFFFF9966.toInt(),
                )
            rc.endGlobal()
            return color
        }

    val hrId: Short
        get() {
            rc.beginGlobal()
            val color =
                rc.writer.addThemedColor(
                    "color.system_accent2_700",
                    0xFF113311.toInt(),
                    "color.system_accent2_400",
                    0xFFFF9966.toInt(),
                )
            rc.endGlobal()
            return color
        }
}

@Preview @Composable private fun MClockPreview() = RemoteDocPreview(MClock())
