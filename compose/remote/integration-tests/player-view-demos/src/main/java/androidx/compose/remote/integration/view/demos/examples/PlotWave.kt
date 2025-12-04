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
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.RFloat
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.Rc.FloatExpression.VAR1
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.cos
import androidx.compose.remote.creation.floor
import androidx.compose.remote.creation.ifElse
import androidx.compose.remote.creation.log
import androidx.compose.remote.creation.minus
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.pow
import androidx.compose.remote.creation.sin
import androidx.compose.remote.creation.times
import androidx.compose.remote.tooling.preview.RemoteDocPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

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
                        loop(minX, majorStepX, maxX) { x ->
                            val posX = x * scaleX + offsetX
                            drawLine(posX, insertY, posX, h - insertY)
                        }
                        painter.setColor(Color.GRAY).commit()
                        val bottom = h - insertY
                        loop(minX, minorStepX, maxX) { x ->
                            val posX = x * scaleX + offsetX
                            val id = createTextFromFloat(x, 1, 1, Rc.TextFromFloat.PAD_AFTER_ZERO)
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

                        loop(minY, majorStepY, maxY) { y ->
                            val yPos = y * scaleY + offsetY
                            drawLine(insertX, yPos, w - insertX, yPos)
                        }
                        painter.setColor(Color.GRAY).commit()
                        loop(minY, minorStepY, maxY) { y ->
                            val yPos = y * scaleY + offsetY
                            val id = createTextFromFloat(y, 1, 1, Rc.TextFromFloat.PAD_AFTER_ZERO)
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
                                    rFun { x -> sin(x) },
                                    rFun { x -> cos(x) },
                                    rFun { x -> sin(x) * sin(x) },
                                    rFun { x -> ifElse(x, rf(0.1f), rf(0.9f)) },
                                )
                            val title =
                                arrayOf(
                                    "sin(x)",
                                    "cos(x)",
                                    "sin(x) * sin(x)",
                                    "ifElse( x, rf(0.1f), rf(0.9f))",
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

// ========================================================================================
// ========================================================================================
// ========================================================================================
@Suppress("RestrictedApiAndroidX")
fun basicPlot4(): RemoteComposeWriter {
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
                    canvas(RecordingModifier().fillMaxSize().background(0xFF127799.toInt())) {
                        painter.setColor(0xFFFF9966.toInt()).setTextSize(64f).commit()
                        val minX = -10f
                        val maxX = 10f
                        val scale = (((Seconds() / 2f) % 2f) + 1f).anim(0.5f)
                        val scaleY = scale * (ComponentHeight() - 100f) / -10f
                        val offsetY = ComponentHeight() / 2f
                        val scaleX = (ComponentWidth() - 100f) / (maxX - minX)
                        val offsetX = 50f - minX * scaleX
                        val id = scale.genTextId(1, 1)
                        drawTextAnchored(id, ComponentWidth() / 2f, 100f, 0, 0, 0)
                        painter.setStrokeWidth(10f).setStyle(Paint.Style.STROKE).commit()
                        val equ = rFun { x -> sin(x + ContinuousSec() * 3f) }
                        val pathId =
                            addPathExpression(
                                rFun { x -> x * scaleX + offsetX },
                                equ * scaleY + offsetY,
                                minX,
                                maxX,
                                64,
                                Rc.PathExpression.SPLINE_PATH,
                            )
                        drawPath(pathId)
                    }
                }
            }
        }
    return rc.writer
}

@Suppress("RestrictedApiAndroidX")
fun basicPlot1(): RemoteComposeWriter {
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
                    canvas(RecordingModifier().fillMaxSize().background(0xFF122334.toInt())) {
                        painter.setColor(0xFFFFAABB.toInt()).setTextSize(64f).commit()
                        val minX = -10f
                        val maxX = 10f
                        val scale = (((Seconds() / 2f) % 2f) + 1f).anim(0.5f).flush()
                        val scaleY = scale * (ComponentHeight() - 100f) / -10f
                        val offsetY = ComponentHeight() / 2f
                        val scaleX = (ComponentWidth() - 100f) / (maxX - minX)
                        val offsetX = 50f - minX * scaleX
                        val id = createTextFromFloat(scale, 1, 1, Rc.TextFromFloat.PAD_AFTER_ZERO)
                        drawTextAnchored(id, ComponentWidth() / 2f, 100f, 0, 0, 0)
                        painter.setStrokeWidth(10f).setStyle(Paint.Style.STROKE).commit()
                        val equ = rFun { x -> sin(x + ContinuousSec() * 3f) }
                        val pathId =
                            addPathExpression(
                                rFun { x -> x * scaleX + offsetX },
                                equ * scaleY + offsetY,
                                minX,
                                maxX,
                                64,
                                Rc.PathExpression.SPLINE_PATH,
                            )
                        drawPath(pathId)
                    }
                }
            }
        }
    return rc.writer
}

@Suppress("RestrictedApiAndroidX")
fun basicPlot(): RemoteComposeWriter {
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
                        graph(rFun { x -> sin(x) }, -10, 10, -2, 2)
                    }
                }
            }
        }
    return rc.writer
}

@Suppress("RestrictedApiAndroidX")
fun RemoteComposeContextAndroid.graph(
    equation: RFloat,
    minX: Number,
    maxX: Number,
    minY: Number,
    maxY: Number,
) {
    val w = ComponentWidth()
    val h = ComponentHeight()
    painter.setColor(0xFF000077L.toInt()).setStyle(Paint.Style.STROKE).commit()
    drawRoundRect(0, 0, w, h, 2f, 2f)

    val scale = axis(minX, maxX, minY, maxY)
    painter.setColor(Color.YELLOW).commit()

    graphPlot(this, equation, minX, maxX, scale)
}

@Suppress("RestrictedApiAndroidX")
fun graphPlot(
    rc: RemoteComposeContext,
    equation: RFloat,
    minX: Number,
    maxX: Number,
    scale: Array<RFloat>,
) {
    val scaleX = scale[0]
    val offsetX = scale[1]
    val scaleY = scale[2]
    val offsetY = scale[3]
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

@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.axis(
    minNX: Number,
    maxNX: Number,
    minNY: Number,
    maxNY: Number,
): Array<RFloat> {
    val w = ComponentWidth()
    val h = ComponentHeight()
    painter.setColor(0xFFFFFF77L.toInt()).commit()
    val minX = minNX as? RFloat ?: rf(minNX.toFloat())
    val maxX = maxNX as? RFloat ?: rf(maxNX.toFloat())
    val minY = minNY as? RFloat ?: rf(minNY.toFloat())
    val maxY = maxNY as? RFloat ?: rf(maxNY.toFloat())
    addDebugMessage("============================ ")

    val xRange = maxX - minX
    val yRange = maxY - minY
    yRange.flush()
    xRange.toFloat()

    val majorStepX = niceIncrement(xRange, 3)
    val insertX = 100f
    val insertY = 100f

    val scaleX = (h - insertX * 2f) / (maxX - minX)
    val offsetX = insertX - (minX * scaleX)
    val scaleY = (h - insertY * 2f) / (minY - maxY)
    val offsetY = (h - insertY) - (scaleY * minY)
    val ret = arrayOf(scaleX, offsetX, scaleY, offsetY)
    painter.setColor(Color.DKGRAY).setStrokeWidth(4f).setTextSize(48f).commit()
    // ================ minor grid  ================
    val minorStepX = niceIncrement(xRange, 20)
    loop(minX, minorStepX, maxX) { x ->
        val posX = x * scaleX + offsetX
        drawLine(posX, insertY, posX, h - insertY)
    }

    painter.setColor(Color.DKGRAY).setStrokeWidth(4f).commit()
    val minorStepY = niceIncrement(yRange, 30)
    loop(minY, minorStepY, maxY + 0.01f) { y ->
        val yPos = y * scaleY + offsetY
        drawLine(insertX, yPos, w - insertX, yPos)
    }
    // ================ major grid  ================
    painter.setColor(Color.GRAY).commit()
    val bottom = h - insertY
    loop(minX, majorStepX, maxX + 0.01f) { x ->
        val posX = x * scaleX + offsetX
        val id = createTextFromFloat(x.toFloat(), 3, 1, Rc.TextFromFloat.PAD_AFTER_ZERO)
        drawTextAnchored(id, posX, bottom, 0f, 1.5f, 0)
        drawLine(posX, insertY, posX, bottom)
    }

    val majorStepY = niceIncrement(yRange, 5)
    loop(minY, majorStepY, maxY + 0.01f) { y ->
        val yPos = y * scaleY + offsetY
        val id = createTextFromFloat(y.toFloat(), 1, 1, Rc.TextFromFloat.PAD_AFTER_ZERO)
        drawTextAnchored(id, insertX, yPos, 1.5f, 0f, 0)
        drawLine(insertX, yPos, w - insertX, yPos)
    }

    painter.setColor(Color.LTGRAY).commit()
    drawLine(offsetX, insertY, offsetX, bottom)
    painter.setColor(Color.WHITE).commit()
    drawLine(insertX, offsetY, w - insertX, offsetY)
    // ========================================================
    return ret
}

@Suppress("RestrictedApiAndroidX")
fun niceIncrement(range: RFloat, minSteps: Int): RFloat {
    val maxIncrement = range / minSteps.toFloat()
    val n = floor(log(maxIncrement))
    val powerOf10 = pow(10f, n)
    powerOf10.flush()

    val normalizedIncrement = maxIncrement / powerOf10

    val ret =
        ifElse(
            normalizedIncrement - 5f,
            powerOf10 * 5.0f,
            ifElse(normalizedIncrement - 2f, powerOf10 * 2.0f, powerOf10),
        )
    return ret
}

@Preview @Composable fun PlotWavePreview() = RemoteDocPreview(plotWave())

@Preview @Composable fun BasicPlotPreview() = RemoteDocPreview(basicPlot())
