/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.integration.view.demos.examples.old

import android.annotation.SuppressLint
import android.graphics.Paint
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.ADD
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.DIV
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.IFELSE
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MOD
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.SUB
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Draws a dial with ticks and labels.
 *
 * @param radius The radius of the dial.
 * @param rotation The rotation of the dial in degrees.
 * @param centerX The X coordinate of the center point.
 * @param centerY The Y coordinate of the center point.
 */
@SuppressLint("RestrictedApiAndroidX")
fun RemoteComposeContext.optimizedDialLoop(
    radius: Float,
    rotation: Float,
    centerX: Float,
    centerY: Float,
) {
    // The tick mark line height at number.
    val lineHeightOn = floatExpression(radius, 50f, SUB)
    // The tick mark line height off number.
    val lineHeightOff = floatExpression(radius, 30f, SUB)
    val rotationAngle = floatExpression(rotation, -1f, MUL)

    save()
    rotate(rotationAngle, centerX, centerY)

    val h = centerY * 2f
    val index = addFloatConstant(0f)
    loop(Utils.idFromNan(index), 0, 1, 60) {
        val onHourMarker = floatExpression(index, 5f, MOD)
        val lineHeight = floatExpression(lineHeightOn, lineHeightOff, onHourMarker, IFELSE)
        val lH = floatExpression(50f, 30f, onHourMarker, IFELSE)
        save()
        translate(centerX, centerY)
        val rotationValue = floatExpression(index, 6f, MUL)
        rotate(rotationValue)
        translate(0f, h - lineHeight)
        drawLine(0f, 0f, 0f, lH)
        restore()
    }
    restore()

    // Draw numbers.
    val labelOffset = floatExpression(radius, 80f, SUB)
    loop(Utils.idFromNan(index), 0, 5, 60) {
        val stepsAngle = floatExpression(index, 6f, MUL)
        val (textX, textY) =
            polarToCartesianExpression(centerX, centerY, labelOffset, stepsAngle, rotation)
        val stringId = createTextFromFloat(index, 2, 0, 0)
        drawTextAnchored(stringId, textX, textY, 0f, 0f, 0)
    }
}

/** Creates a concentric clock face. */
@SuppressLint("RestrictedApiAndroidX")
fun concentricOptimizedLoop(): RemoteComposeContext {
    return RemoteComposeContextAndroid(800, 800, "Clock", platform = AndroidxRcPlatformServices()) {
        root {
            box(Modifier.fillMaxSize().background(android.graphics.Color.BLACK)) {
                canvas(Modifier.fillMaxSize()) {
                    painter.setColor(Color.Black.toArgb()).setStyle(Paint.Style.FILL).commit()

                    val centerX = floatExpression(addComponentWidthValue(), 2f, DIV)
                    val centerY = floatExpression(addComponentHeightValue(), 2f, DIV)
                    val secondsAngle =
                        floatExpression(
                            exp(RemoteContext.FLOAT_TIME_IN_SEC, 60f, MOD, -6f, MUL),
                            anim(1f, RemoteComposeBuffer.EASING_CUBIC_LINEAR, null, Float.NaN, 360f),
                        )
                    val minuteAngle =
                        floatExpression(
                            exp(RemoteContext.FLOAT_TIME_IN_MIN, 6f, MUL),
                            anim(
                                60f,
                                RemoteComposeBuffer.EASING_CUBIC_LINEAR,
                                null,
                                Float.NaN,
                                360f,
                            ),
                        )

                    // Seconds Dial.
                    painter
                        .setTypeface(0, 700, false)
                        .setTextSize(60f)
                        .setColor(Color.White.toArgb())
                        .commit()
                    optimizedDialLoop(
                        radius = floatExpression(centerX, 30f, SUB),
                        secondsAngle,
                        centerX,
                        centerY,
                    )

                    // Minute Dial.
                    painter
                        .setTypeface(0, 700, false)
                        .setTextSize(40f)
                        .setColor(Color.Gray.toArgb())
                        .commit()
                    optimizedDialLoop(
                        radius = floatExpression(centerX, 160f, SUB),
                        minuteAngle,
                        centerX,
                        centerY,
                    )

                    // Hour Text.
                    painter
                        .setTypeface(0, 1900, false)
                        .setTextSize(160f)
                        .setColor(Color.White.toArgb())
                        .commit()
                    val hourTextId = createTextFromFloat(RemoteContext.FLOAT_TIME_IN_HR, 2, 0, 0)
                    drawTextAnchored(hourTextId, centerX, centerY, 0f, 0f, 0)

                    // Draw a black circle as a background for the current minute text.
                    // Minute outline.
                    painter.setColor(Color.Black.toArgb()).setStyle(Paint.Style.FILL).commit()

                    drawCircle(floatExpression(centerX, 130f + 50f, ADD), centerY, 50f)

                    // Current minute Text.
                    painter
                        .setTypeface(0, 700, false)
                        .setTextSize(50f)
                        .setColor(Color.White.toArgb())
                        .commit()
                    val minuteTextId = createTextFromFloat(RemoteContext.FLOAT_TIME_IN_MIN, 2, 0, 0)
                    drawTextAnchored(
                        minuteTextId,
                        floatExpression(centerX, 130f + 50f, ADD),
                        centerY,
                        0f,
                        0f,
                        0,
                    )

                    // Yellow inset around the minute text.
                    painter
                        .setColor(Color.Yellow.toArgb())
                        .setStrokeWidth(4f)
                        .setStrokeCap(Paint.Cap.BUTT)
                        .setStyle(Paint.Style.STROKE)
                        .commit()

                    drawRoundRect(
                        floatExpression(centerX, 130f, ADD),
                        floatExpression(centerY, 50f, SUB),
                        floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH),
                        floatExpression(centerY, 50f, ADD),
                        50f,
                        50f,
                    )
                }
            }
        }
    }
}
