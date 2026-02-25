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

import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

val SEC_TEXT_SCALE: Float = 0.12f
val MIN_TEXT_SCALE: Float = 0.09f
val HR_TEXT_SIZE: Float = .25f

// measurd from the edge in rad units
const val SEC_OFF = 0.0f
const val SEC_LINE = 0.1f
const val SEC_LINE2 = 0.15f

const val MIN_OFF = 0.36f
const val MIN_LINE = MIN_OFF + 0.05f
const val MIN_LINE2 = MIN_LINE + 0.03f
val clockNums = arrayOf("00", "05", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55")
var clockNumsId: IntArray? = null

/**
 * Draws a dial with ticks and labels.
 *
 * @param radius The radius of the dial.
 * @param rotation The rotation of the dial in degrees.
 * @param centerX The X coordinate of the center point.
 * @param centerY The Y coordinate of the center point.
 */
@Suppress("RestrictedApiAndroidX")
fun RemoteComposeContext.drawDigits(
    radius: Float,
    rotation: Float,
    centerX: Float,
    centerY: Float,
    isSecond: Boolean = false,
) {
    val fontOffset = 0.7f * if (isSecond) SEC_TEXT_SCALE else MIN_TEXT_SCALE
    val offset = 1f - fontOffset - if (isSecond) SEC_LINE2 else MIN_LINE2
    // Draw numbers.

    if (clockNumsId == null) {
        val numbers = IntArray(clockNums.size)
        clockNumsId = numbers
        for (i in clockNums.indices) {
            numbers[i] = textCreateId(clockNums[i])
        }
    }
    val labelOffset = floatExpression(radius, offset, Rc.FloatExpression.MUL)
    val numbers: IntArray = clockNumsId as IntArray
    for (step in 0..59 step 5) {
        val stepsAngle = step * 6f

        val (textX, textY) = polarExpression(centerX, centerY, labelOffset, stepsAngle, rotation)
        drawTextAnchored(numbers[step / 5], textX, textY, 0f, 0f, 0)
    }
}

/**
 * Draws a dial with ticks and labels.
 *
 * @param radius The radius of the dial.
 * @param rotation The rotation of the dial in degrees.
 * @param centerX The X coordinate of the center point.
 * @param centerY The Y coordinate of the center point.
 */
@Suppress("RestrictedApiAndroidX")
fun RemoteComposeContext.drawTicks(
    radius: Float,
    rotation: Float,
    centerX: Float,
    centerY: Float,
    isSecond: Boolean = false,
) {
    val top = floatExpression(centerY, radius, Rc.FloatExpression.SUB)
    val start =
        floatExpression(
            top,
            radius,
            if (isSecond) SEC_OFF else MIN_OFF,
            Rc.FloatExpression.MUL,
            Rc.FloatExpression.ADD,
        )
    val smallTick =
        floatExpression(
            top,
            radius,
            if (isSecond) SEC_LINE else MIN_LINE,
            Rc.FloatExpression.MUL,
            Rc.FloatExpression.ADD,
        )
    val bigTick =
        floatExpression(
            top,
            radius,
            if (isSecond) SEC_LINE2 else MIN_LINE2,
            Rc.FloatExpression.MUL,
            Rc.FloatExpression.ADD,
        )
    // The tick mark line height at number.

    save()
    rotate(rotation, centerX, centerY)

    // This is not ideal: Each tick mark coordinate is calculated using an expression.

    repeat(60) { step ->
        val tick =
            if (step % 5 == 0) {
                bigTick
            } else {
                smallTick
            }
        save()
        rotate(6f * step, centerX, centerY)
        drawLine(centerX, start, centerX, tick)
        restore()
    }
    restore()
}

/** Creates a concentric clock face. */
@Suppress("RestrictedApiAndroidX")
fun concentricDemo(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        800,
        800,
        "Clock",
        6,
        0,
        platform = AndroidxRcPlatformServices(),
    ) {
        root {
            box(modifier = Modifier.fillMaxSize()) {
                canvas(modifier = Modifier.fillMaxSize()) {
                    painter.setColor(Color.Black.toArgb()).setStyle(Paint.Style.FILL).commit()
                    val w = floatExpression(addComponentWidthValue())
                    val h = floatExpression(addComponentHeightValue())
                    val centerX = floatExpression(w, 2f, Rc.FloatExpression.DIV)
                    val centerY = floatExpression(h, 2f, Rc.FloatExpression.DIV)
                    val rad = floatExpression(centerX, centerY, Rc.FloatExpression.MIN)
                    val gradRad = floatExpression(rad, 1.1f, Rc.FloatExpression.MUL)
                    // draw Black dial
                    painter
                        .setRadialGradient(
                            centerX,
                            centerY,
                            gradRad,
                            intArrayOf(
                                Color.Black.toArgb(),
                                Color.Black.toArgb(),
                                Color.Transparent.toArgb(),
                            ),
                            floatArrayOf(0f, 0.8f, 1f),
                            Shader.TileMode.CLAMP,
                        )
                        .commit()
                    drawCircle(centerX, centerY, rad)
                    painter
                        .setShader(0)
                        .setColor(Color.Black.toArgb())
                        .setStyle(Paint.Style.FILL)
                        .commit()

                    val secondsAngle =
                        floatExpression(
                            Rc.Time.CONTINUOUS_SEC,
                            60f,
                            Rc.FloatExpression.MOD,
                            6f,
                            Rc.FloatExpression.MUL,
                        )
                    val minuteAngle =
                        floatExpression(
                            exp(RemoteContext.FLOAT_TIME_IN_MIN, 6f, Rc.FloatExpression.MUL),
                            anim(
                                1f,
                                RemoteComposeBuffer.EASING_CUBIC_STANDARD,
                                null,
                                Float.NaN,
                                360f,
                            ),
                        )
                    val secTextSize = floatExpression(rad, SEC_TEXT_SCALE, Rc.FloatExpression.MUL)

                    // ================ Seconds Dial.
                    painter
                        .setTypeface(0, 700, false)
                        .setTextSize(secTextSize)
                        .setColor(Color.White.toArgb())
                        .commit()
                    drawDigits(radius = rad, secondsAngle, centerX, centerY, true)

                    drawTicks(radius = rad, secondsAngle, centerX, centerY, true)

                    //  ================ Minute Dial.
                    val minTextSize = floatExpression(rad, MIN_TEXT_SCALE, Rc.FloatExpression.MUL)

                    painter
                        .setTypeface(0, 700, false)
                        .setTextSize(minTextSize)
                        .setColor(Color.Gray.toArgb())
                        .commit()
                    val minRad = floatExpression(centerX, 0.5f, Rc.FloatExpression.MUL)
                    drawDigits(radius = rad, minuteAngle, centerX, centerY)

                    drawTicks(radius = rad, minuteAngle, centerX, centerY)
                    val hrTextSize = floatExpression(rad, HR_TEXT_SIZE, Rc.FloatExpression.MUL)

                    // ======================= Current Hour text
                    painter
                        .setTypeface(0, 1900, false)
                        .setTextSize(hrTextSize)
                        .setColor(Color.White.toArgb())
                        .commit()
                    val hourTextId =
                        createTextFromFloat(
                            RemoteContext.FLOAT_TIME_IN_HR,
                            2,
                            0,
                            Rc.TextFromFloat.PAD_PRE_ZERO,
                        )
                    drawTextAnchored(hourTextId, centerX, centerY, 0f, 0f, 0)

                    // Draw a black circle as a background for the current minute text.
                    // Minute outline.
                    painter.setColor(Color.Black.toArgb()).setStyle(Paint.Style.FILL).commit()
                    //   painter.setColor(Color.Green.toArgb()).setStyle(Paint.Style.FILL).commit()
                    val fRad = floatExpression(rad, 0.12f, Rc.FloatExpression.MUL)
                    val roundFeatureX =
                        floatExpression(
                            centerX,
                            rad,
                            .3f,
                            Rc.FloatExpression.MUL,
                            Rc.FloatExpression.ADD,
                        )

                    drawRoundRect(
                        roundFeatureX,
                        floatExpression(centerY, fRad, Rc.FloatExpression.SUB),
                        floatExpression(
                            centerX,
                            rad,
                            1f,
                            MIN_LINE2,
                            Rc.FloatExpression.SUB,
                            Rc.FloatExpression.MUL,
                            Rc.FloatExpression.ADD,
                        ),
                        floatExpression(centerY, fRad, Rc.FloatExpression.ADD),
                        0f,
                        0f,
                    )
                    // ========================== Current minute Text
                    painter
                        .setTypeface(0, 700, false)
                        .setTextSize(floatExpression(minTextSize, 1.5f, Rc.FloatExpression.MUL))
                        .setColor(Color.White.toArgb())
                        .commit()
                    val min =
                        floatExpression(
                            RemoteContext.FLOAT_TIME_IN_MIN,
                            60f,
                            Rc.FloatExpression.MOD,
                        )
                    val minuteTextId = createTextFromFloat(min, 2, 0, Rc.TextFromFloat.PAD_PRE_ZERO)
                    drawTextAnchored(
                        minuteTextId,
                        floatExpression(roundFeatureX, fRad, Rc.FloatExpression.ADD),
                        centerY,
                        0f,
                        0f,
                        0,
                    )

                    // ================  Yellow inset around the minute text.
                    //                    painter
                    //                        .setColor(Color.Yellow.toArgb())
                    //
                    //                        .setStrokeCap(Paint.Cap.BUTT)
                    //                        .setStyle(Paint.Style.STROKE)
                    //                        .commit()
                    painter
                        .setRadialGradient(
                            centerX,
                            centerY,
                            gradRad,
                            intArrayOf(
                                Color.Yellow.toArgb(),
                                Color.Yellow.toArgb(),
                                Color.Transparent.toArgb(),
                            ),
                            floatArrayOf(0f, 0.8f, 1f),
                            Shader.TileMode.CLAMP,
                        )
                        .setStrokeWidth(4f)
                        .setStyle(Paint.Style.STROKE)
                        .commit()
                    drawRoundRect(
                        roundFeatureX,
                        floatExpression(centerY, fRad, Rc.FloatExpression.SUB),
                        floatExpression(w, fRad, Rc.FloatExpression.ADD),
                        floatExpression(centerY, fRad, Rc.FloatExpression.ADD),
                        fRad,
                        fRad,
                    )
                }
            }
        }
    }
}

/** Converts polar coordinates to cartesian coordinates using expressions. */
@Suppress("RestrictedApiAndroidX")
fun RemoteComposeContext.polarExpression(
    centerX: Float,
    centerY: Float,
    radius: Float,
    stepsAngle: Float,
    rotation: Float,
): Pair<Float, Float> {
    val angleInRadians =
        floatExpression(stepsAngle, rotation, Rc.FloatExpression.SUB, Rc.FloatExpression.RAD)

    val x =
        floatExpression(
            centerX,
            radius,
            angleInRadians,
            Rc.FloatExpression.COS,
            Rc.FloatExpression.MUL,
            Rc.FloatExpression.ADD,
        )
    val y =
        floatExpression(
            centerY,
            radius,
            angleInRadians,
            Rc.FloatExpression.SIN,
            Rc.FloatExpression.MUL,
            Rc.FloatExpression.SUB,
        )

    return Pair(x, y)
}
