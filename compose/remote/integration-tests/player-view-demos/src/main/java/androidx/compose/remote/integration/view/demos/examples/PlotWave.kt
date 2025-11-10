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
import android.graphics.Paint
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.RFloat
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.Rc.FloatExpression.VAR1
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.cos
import androidx.compose.remote.creation.lerp
import androidx.compose.remote.creation.min
import androidx.compose.remote.creation.minus
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.pingPong
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.sin
import androidx.compose.remote.creation.smoothStep
import androidx.compose.remote.creation.times

@Suppress("RestrictedApiAndroidX")
fun plotWave(): RemoteComposeWriter {
    val rc =
        RemoteComposeContextAndroid(
            width = 500,
            height = 500,
            contentDescription = "Simple Timer",
            apiLevel = 7,
            profiles = RcProfiles.PROFILE_ANDROIDX,
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

                        painter.setColor(0xFF000077L.toInt()).commit()

                        drawRoundRect(0, 0, w, h, 2f, 2f)
                        // ================ vertical lines ================

                        val minX = -2f
                        val maxX = 2f
                        val majorStepX = 0.1f
                        val minorStepX = 0.5f
                        val insertX = 100f
                        val insertY = 100f

                        val scaleX = (h - insertX * 2f) / (maxX - minX)
                        val offsetX = insertX - (minX * scaleX)

                        painter.setColor(Color.DKGRAY).setStrokeWidth(4f).setTextSize(48f).commit()
                        val x = rf(createFloatId())
                        loop(Utils.idFromNan(x.toFloat()), minX, majorStepX, maxX) {
                            val posX = x * scaleX + offsetX
                            drawLine(posX, insertY, posX, h - insertY)
                        }
                        painter.setColor(Color.GRAY).commit()
                        val bottom = h - insertY
                        loop(Utils.idFromNan(x.toFloat()), minX, minorStepX, maxX) {
                            val posX = x * scaleX + offsetX
                            val id =
                                createTextFromFloat(
                                    x.toFloat(),
                                    1,
                                    1,
                                    Rc.TextFromFloat.PAD_AFTER_ZERO,
                                )
                            drawTextAnchored(id, posX, bottom, 0f, 1.5f, 0)
                            drawLine(posX, insertY, posX, bottom)
                        }
                        painter.setColor(Color.LTGRAY).commit()
                        drawLine(offsetX, insertY, offsetX, bottom)
                        // =================== Horizontal lines ============

                        val minY = -2f
                        val maxY = 2f
                        val majorStepY = 0.1f
                        val minorStepY = 0.5f
                        val scaleY = (h - insertY * 2f) / (minY - maxY)
                        val offsetY = (h - insertY) - (scaleY * minY)

                        painter.setColor(Color.DKGRAY).setStrokeWidth(4f).commit()
                        val y = rf(createFloatId())
                        loop(Utils.idFromNan(y.toFloat()), minY, majorStepY, maxY) {
                            val yPos = y * scaleY + offsetY
                            drawLine(insertX, yPos, w - insertX, yPos)
                        }
                        painter.setColor(Color.GRAY).commit()
                        loop(Utils.idFromNan(y.toFloat()), minY, minorStepY, maxY) {
                            val yPos = y * scaleY + offsetY
                            val id =
                                createTextFromFloat(
                                    y.toFloat(),
                                    1,
                                    1,
                                    Rc.TextFromFloat.PAD_AFTER_ZERO,
                                )
                            drawTextAnchored(id, insertX, yPos, 1.5f, 0f, 0)
                            drawLine(insertX, yPos, w - insertX, yPos)
                        }
                        painter.setColor(Color.WHITE).commit()
                        drawLine(insertX, offsetY, w - insertX, offsetY)
                        // ========================================================
                        painter
                            .setColor(Color.YELLOW)
                            .setStyle(Paint.Style.STROKE)
                            .setStrokeWidth(12f)
                            .commit()
                        if (true) {
                            val x = rf(VAR1)
                            val equations =
                                arrayOf(
                                    sin(x),
                                    cos(x),
                                    lerp(-0.3f, 0.3f, x),
                                    smoothStep(x, -0.3f, 0.3f),
                                    pingPong(0.5f, x),
                                    x % 1.4f,
                                )
                            val title =
                                arrayOf(
                                    "sin(x)",
                                    "cos(x)",
                                    "lerp(-0.3, 0.3, x)",
                                    "smoothStep(x,-0.3, 0.3)",
                                    "pingPong(0.5, x)",
                                    "(x % 1.4)",
                                )

                            for ((i, equation) in equations.withIndex()) {
                                val color =
                                    addColorExpression(i / (1.5f * equations.size), 0.9f, 0.9f)
                                painter
                                    .setColorId(color.toInt())
                                    .setStyle(Paint.Style.FILL)
                                    .commit()
                                drawTextAnchored(
                                    title[i],
                                    (w - insertX + 10f).toFloat(),
                                    (h * 0.75f).toFloat(),
                                    1f,
                                    4 - i * 2f,
                                    0,
                                )
                                painter.setStyle(Paint.Style.STROKE).commit()
                                plot(this, equation, scaleX, offsetX, scaleY, offsetY, minX, maxX)
                            }
                        }
                    }
                }
            }
        }
    return rc.writer
}

@Suppress("RestrictedApiAndroidX")
fun plot(
    rc: RemoteComposeContext,
    equation: RFloat,
    scaleX: RFloat,
    offsetX: RFloat,
    scaleY: RFloat,
    offsetY: RFloat,
    minX: Number,
    maxX: Number,
) {
    with(rc) {
        val pathId =
            addPathExpression(
                rf(VAR1) * scaleX + offsetX,
                equation * scaleY + offsetY,
                minX.toFloat(),
                maxX.toFloat(),
                300,
                Rc.PathExpression.LINEAR_PATH,
            )
        drawPath(pathId)
    }
}
