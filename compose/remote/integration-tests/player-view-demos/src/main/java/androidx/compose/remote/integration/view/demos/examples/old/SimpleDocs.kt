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
import android.graphics.Path
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.ADD
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.DIV
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MIN
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MOD
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.SUB
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

/**
 * Collection of simple examples of creating RemoteCompose Documents to visually exercise the
 * functionality
 */

/** Simple ovals */
@Suppress("RestrictedApiAndroidX")
fun createSimpleOvalDoc(): RemoteComposeContext {

    val doc =
        RemoteComposeContextAndroid(600, 600, "Demo", platform = AndroidxRcPlatformServices()) {
            painter.setShader(0).setColor(Color.Yellow.toArgb()).commit()
            drawOval(0f, 0f, RemoteContext.FLOAT_WINDOW_WIDTH, RemoteContext.FLOAT_WINDOW_HEIGHT)
            var w2 = floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 2f, DIV)
            var h2 = floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, 2f, DIV)
            painter.setColor(Color.Red.toArgb()).commit()
            drawOval(0f, 0f, w2, h2)
            painter.setColor(Color.Green.toArgb()).commit()
            drawOval(w2, 0f, RemoteContext.FLOAT_WINDOW_WIDTH, h2)
            painter.setColor(Color.Blue.toArgb()).commit()
            drawOval(0f, h2, w2, RemoteContext.FLOAT_WINDOW_HEIGHT)
            painter.setColor(Color.Gray.toArgb()).commit()
            drawOval(w2, h2, RemoteContext.FLOAT_WINDOW_WIDTH, RemoteContext.FLOAT_WINDOW_HEIGHT)
        }

    return doc
}

/** Simple ovals */
@Suppress("RestrictedApiAndroidX")
fun createSimpleOvalDoc2(): RemoteComposeContext {

    val doc =
        RemoteComposeContextAndroid(600, 600, "Demo", platform = AndroidxRcPlatformServices()) {
            painter.setShader(0).setColor(Color.Yellow.toArgb()).commit()
            drawOval(0f, 0f, 400f, 400f)
            var w2 = 200f
            var h2 = 200f
            painter.setColor(Color.Red.toArgb()).commit()
            drawOval(0f, 0f, w2, h2)
            painter.setColor(Color.Green.toArgb()).commit()
            drawOval(w2, 0f, 400f, h2)
            painter.setColor(Color.Blue.toArgb()).commit()
            drawOval(0f, h2, w2, 400f)
            painter.setColor(Color.Gray.toArgb()).commit()
            drawOval(w2, h2, 400f, 400f)
        }

    return doc
}

@Suppress("RestrictedApiAndroidX")
fun createTextInRect02(): RemoteComposeContext {
    var str = "Hello"
    val colors =
        intArrayOf(
            -0x10000,
            -0x7800,
            -0x777778,
            -0xff7701,
            -0x10000,
            -0x7800,
            -0x777778,
            -0xff7701,
            -0x10000,
        )
    val remoteWriter =
        RemoteComposeContextAndroid(600, 600, "Demo", platform = AndroidxRcPlatformServices()) {
            painter.setTextSize(50f).commit()
            var l = 0f
            var t = 0f
            var r = 500f
            var b = 80f
            painter.setColor(Color.LightGray.toArgb()).setStyle(Paint.Style.FILL).commit()
            drawRect(0f, 0f, 600f, 600f)
            for ((k, c) in colors.withIndex()) {
                painter.setColor(c).setStyle(Paint.Style.FILL).commit()
                drawRect((l + r) / 2 - 10, (t + b) / 2 - 10, (l + r) / 2 + 10, (t + b) / 2 + 10)
                painter.setColor(android.graphics.Color.BLUE).setStyle(Paint.Style.FILL).commit()
                val dx = ((k % 3) - 1).toFloat()
                val dy = (((k / 3) % 3) - 1).toFloat()
                drawTextAnchored(str, (l + r) / 2, (t + b) / 2, dx, dy, 0)
                l += 30
                t += 100f
                r += 30
                b += 100
            }
        }
    return remoteWriter
}

@Suppress("RestrictedApiAndroidX")
fun clock(): RemoteComposeContext {
    return RemoteComposeContextAndroid(600, 600, "Clock", platform = AndroidxRcPlatformServices()) {
        val centerX = floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 2f, DIV)
        val centerY = floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, 2f, DIV)
        val rad = floatExpression(centerX, centerY, MIN)
        val hourHandLength = floatExpression(centerX, centerY, MIN, 0.5f, MUL)
        val minHandLength = floatExpression(centerX, centerY, MIN, 0.2f, MUL)
        val secondAngle = floatExpression(RemoteContext.FLOAT_CONTINUOUS_SEC, 6f, MUL)
        val minAngle = floatExpression(RemoteContext.FLOAT_TIME_IN_MIN, 6f, MUL)
        val hrAngle = floatExpression(RemoteContext.FLOAT_TIME_IN_HR, 30f, MUL)
        val handWidth = 20f
        painter.setColor(Color.Black.toArgb()).commit()
        drawCircle(centerX, centerY, rad)

        painter.setColor(Color.Green.toArgb()).setStrokeWidth(8f).commit()
        save()
        for (i in 0 until 12) {
            rotate(30f, centerX, centerY)
            drawLine(centerX, 0f, centerX, 40f)
        }
        restore()
        painter
            .setColor(Color.White.toArgb())
            .setStrokeWidth(handWidth)
            .setStrokeCap(Paint.Cap.ROUND)
            .commit()
        save()
        rotate(hrAngle, centerX, centerY)
        drawLine(centerX, centerY, centerX, hourHandLength)
        restore()
        save()
        rotate(minAngle, centerX, centerY)
        drawLine(centerX, centerY, centerX, minHandLength)
        restore()
        save()
        rotate(secondAngle, centerX, centerY)
        painter
            .setColor(Color.Red.toArgb())
            .setStrokeWidth(4f)
            .setStrokeCap(Paint.Cap.ROUND)
            .commit()
        drawLine(centerX, centerY, centerX, 30f)
        restore()
        drawCircle(centerX, centerY, handWidth)
        painter.setColor(Color.Black.toArgb()).commit()
        drawCircle(centerX, centerY, handWidth / 2)
    }
}

@Suppress("RestrictedApiAndroidX")
fun clock2(): RemoteComposeContext {
    return RemoteComposeContextAndroid(600, 600, "Clock", platform = AndroidxRcPlatformServices()) {
        val centerX = floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 2f, DIV)
        val centerY = floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, 2f, DIV)
        val rad = floatExpression(centerX, centerY, MIN)
        val hourHandLength = floatExpression(centerX, centerY, MIN, 0.5f, MUL)
        val minHandLength = floatExpression(centerX, centerY, MIN, 0.2f, MUL)
        val secondAngle = floatExpression(RemoteContext.FLOAT_TIME_IN_SEC, 6f, MUL)
        val minAngle = floatExpression(RemoteContext.FLOAT_TIME_IN_MIN, 6f, MUL)
        val hrAngle = floatExpression(RemoteContext.FLOAT_TIME_IN_HR, 30f, MUL)
        val handWidth = 20f
        painter.setColor(Color.Black.toArgb()).commit()
        drawCircle(centerX, centerY, rad)

        painter.setColor(Color.Green.toArgb()).setStrokeWidth(8f).commit()
        save()
        for (i in 0 until 12) {
            rotate(30f, centerX, centerY)
            drawLine(centerX, 0f, centerX, 40f)
        }
        restore()
        painter
            .setColor(Color.White.toArgb())
            .setStrokeWidth(handWidth)
            .setStrokeCap(Paint.Cap.ROUND)
            .commit()
        save()
        rotate(hrAngle, centerX, centerY)
        drawLine(centerX, centerY, centerX, hourHandLength)
        restore()
        save()
        rotate(minAngle, centerX, centerY)
        drawLine(centerX, centerY, centerX, minHandLength)
        restore()
        save()
        rotate(secondAngle, centerX, centerY)
        painter
            .setColor(Color.Red.toArgb())
            .setStrokeWidth(4f)
            .setStrokeCap(Paint.Cap.ROUND)
            .commit()
        drawLine(centerX, centerY, centerX, 30f)
        restore()
        drawCircle(centerX, centerY, handWidth)
        painter.setColor(Color.Black.toArgb()).commit()
        drawCircle(centerX, centerY, handWidth / 2)
    }
}

@Suppress("RestrictedApiAndroidX")
fun clock3(): RemoteComposeContext {
    return RemoteComposeContextAndroid(600, 600, "Clock", platform = AndroidxRcPlatformServices()) {
        val centerX = floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 2f, DIV)
        val centerY = floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, 2f, DIV)
        val rad = floatExpression(centerX, centerY, MIN)
        val hourHandLength = floatExpression(centerX, centerY, MIN, 0.5f, MUL)
        val minHandLength = floatExpression(centerX, centerY, MIN, 0.2f, MUL)
        val secondAngle =
            floatExpression(exp(RemoteContext.FLOAT_TIME_IN_SEC, 60f, MOD, 90f, MUL), anim(0.999f))
        val minAngle = floatExpression(RemoteContext.FLOAT_TIME_IN_MIN, 6f, MUL)
        val hrAngle = floatExpression(RemoteContext.FLOAT_TIME_IN_HR, 30f, MUL)
        val handWidth = 20f
        painter.setColor(Color.Black.toArgb()).commit()
        drawCircle(centerX, centerY, rad)

        painter.setColor(Color.Green.toArgb()).setStrokeWidth(8f).commit()
        save()
        for (i in 0 until 12) {
            rotate(30f, centerX, centerY)
            drawLine(centerX, 0f, centerX, 40f)
        }
        restore()
        painter
            .setColor(Color.White.toArgb())
            .setStrokeWidth(handWidth)
            .setStrokeCap(Paint.Cap.ROUND)
            .commit()
        save()
        rotate(hrAngle, centerX, centerY)
        drawLine(centerX, centerY, centerX, hourHandLength)
        restore()
        save()
        rotate(minAngle, centerX, centerY)
        drawLine(centerX, centerY, centerX, minHandLength)
        restore()
        save()
        rotate(secondAngle, centerX, centerY)
        painter
            .setColor(Color.Red.toArgb())
            .setStrokeWidth(4f)
            .setStrokeCap(Paint.Cap.ROUND)
            .commit()
        drawLine(centerX, centerY, centerX, 30f)
        restore()
        drawCircle(centerX, centerY, handWidth)
        painter.setColor(Color.Black.toArgb()).commit()
        drawCircle(centerX, centerY, handWidth / 2)
    }
}

@Suppress("RestrictedApiAndroidX")
fun clockLinear(): RemoteComposeContext {
    return baseClock(type = RemoteComposeBuffer.EASING_CUBIC_LINEAR)
}

@Suppress("RestrictedApiAndroidX")
fun clockOvershoot(): RemoteComposeContext {
    return baseClock(type = RemoteComposeBuffer.EASING_CUBIC_OVERSHOOT)
}

@Suppress("RestrictedApiAndroidX")
fun clockAccelerate(): RemoteComposeContext {
    return baseClock(type = RemoteComposeBuffer.EASING_CUBIC_ACCELERATE)
}

@Suppress("RestrictedApiAndroidX")
fun clockAnticipate(): RemoteComposeContext {
    return baseClock(type = RemoteComposeBuffer.EASING_CUBIC_ANTICIPATE)
}

@Suppress("RestrictedApiAndroidX")
fun clockDecelerate(): RemoteComposeContext {
    return baseClock(type = RemoteComposeBuffer.EASING_CUBIC_DECELERATE)
}

@Suppress("RestrictedApiAndroidX")
fun clockHighBeat(): RemoteComposeContext {
    return baseClock(
        type = RemoteComposeBuffer.EASING_SPLINE_CUSTOM,
        spec = floatArrayOf(0f, 0f, 0.25f, 0.25f, 0.5f, 0.5f, 0.75f, 0.75f, 1f),
    )
}

@Suppress("RestrictedApiAndroidX")
fun baseClock(
    duration: Float = 1.0f,
    type: Int = RemoteComposeBuffer.EASING_CUBIC_STANDARD,
    spec: FloatArray? = null,
    initialValue: Float = Float.NaN,
): RemoteComposeContext {
    return RemoteComposeContextAndroid(600, 600, "Clock", platform = AndroidxRcPlatformServices()) {
        val centerX = floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 2f, DIV)
        val centerY = floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, 2f, DIV)
        val rad = floatExpression(centerX, centerY, MIN)
        val hourHandLength = floatExpression(centerX, centerY, MIN, 0.5f, MUL)
        val minHandLength = floatExpression(centerX, centerY, MIN, 0.2f, MUL)
        val secondAngle =
            floatExpression(
                exp(RemoteContext.FLOAT_TIME_IN_SEC, 60f, MOD, 6f, MUL),
                anim(duration, type, spec, initialValue, 360f),
            )
        val minAngle = floatExpression(RemoteContext.FLOAT_TIME_IN_MIN, 6f, MUL)
        val hrAngle = floatExpression(RemoteContext.FLOAT_TIME_IN_HR, 30f, MUL)
        val handWidth = 20f

        addClickArea(567, "clock face", 0f, 0f, 600f, 600f, "1234")

        painter.setColor(Color.Black.toArgb()).commit()
        drawCircle(centerX, centerY, rad)

        painter.setColor(Color.Green.toArgb()).setStrokeWidth(8f).commit()
        save()
        for (i in 0 until 12) {
            rotate(30f, centerX, centerY)
            drawLine(centerX, 0f, centerX, 40f)
        }
        restore()
        painter
            .setColor(Color.White.toArgb())
            .setStrokeWidth(handWidth)
            .setStrokeCap(Paint.Cap.ROUND)
            .commit()
        save()
        rotate(hrAngle, centerX, centerY)
        drawLine(centerX, centerY, centerX, hourHandLength)
        restore()
        save()
        rotate(minAngle, centerX, centerY)
        drawLine(centerX, centerY, centerX, minHandLength)
        restore()
        save()
        rotate(secondAngle, centerX, centerY)
        painter
            .setColor(Color.Red.toArgb())
            .setStrokeWidth(4f)
            .setStrokeCap(Paint.Cap.ROUND)
            .commit()
        drawLine(centerX, centerY, centerX, 30f)
        restore()
        drawCircle(centerX, centerY, handWidth)
        painter.setColor(Color.Black.toArgb()).commit()
        drawCircle(centerX, centerY, handWidth / 2)
    }
}

@Suppress("RestrictedApiAndroidX")
fun MClock2(): RemoteComposeContext {
    val duration = 1.0f
    val type = RemoteComposeBuffer.EASING_CUBIC_STANDARD
    val spec: FloatArray? = null
    val initialValue: Float = Float.NaN
    val radius = 1f

    val rPoly =
        RoundedPolygon.star(
                numVerticesPerRadius = 4,
                rounding = CornerRounding(.352f, .8f),
                innerRounding = CornerRounding(.32f, .8f),
                innerRadius = .352f,
            )
            .toPath()

    return RemoteComposeContextAndroid(600, 600, "Clock", platform = AndroidxRcPlatformServices()) {
        val centerX = floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 2f, DIV)
        val centerY = floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, 2f, DIV)
        val scale =
            floatExpression(
                RemoteContext.FLOAT_WINDOW_WIDTH,
                RemoteContext.FLOAT_WINDOW_HEIGHT,
                MIN,
            )
        val hourHandLength = floatExpression(centerX, centerY, MIN, 0.4f, MUL)
        val minHandLength = floatExpression(centerX, centerY, MIN, 0.3f, MUL)
        val secondAngle = floatExpression(RemoteContext.FLOAT_CONTINUOUS_SEC, 60f, MOD, 6f, MUL)
        val minAngle = floatExpression(RemoteContext.FLOAT_TIME_IN_MIN, 6f, MUL)
        val hrAngle = floatExpression(RemoteContext.FLOAT_TIME_IN_HR, 30f, MUL)
        val hourWidth = 40f
        val handWidth = 20f
        val baseColor = addNamedColor("android.colorControlNormal", Color(0xFF1A1A5E).toArgb())
        painter.setColorId(baseColor).setStyle(Paint.Style.FILL).commit()

        // background
        save()
        translate(centerX, centerY)
        scale(scale, scale)
        rotate(45f)
        drawPath(rPoly)
        restore()
        painter.setColor(Color.Green.toArgb()).setStrokeWidth(8f).commit()

        // hour
        save()
        painter
            .setColor(Color.Gray.toArgb())
            .setStrokeWidth(hourWidth)
            .setStrokeCap(Paint.Cap.ROUND)
            .commit()
        rotate(hrAngle, centerX, centerY)
        drawLine(centerX, centerY, centerX, hourHandLength)
        restore()

        // min
        save()
        painter.setColor(Color.White.toArgb()).setStrokeWidth(handWidth).commit()
        rotate(minAngle, centerX, centerY)
        drawLine(centerX, centerY, centerX, minHandLength)
        restore()

        // sec
        save()
        rotate(secondAngle, centerX, centerY)
        painter.setColor(Color.Red.toArgb()).setStrokeWidth(4f).commit()
        drawLine(centerX, centerY, centerX, minHandLength)
        restore()
        // cap
        drawCircle(centerX, centerY, handWidth / 2)
    }
}

@Suppress("RestrictedApiAndroidX")
fun MClock(): RemoteComposeContext {
    val duration = 1.0f
    val type = RemoteComposeBuffer.EASING_CUBIC_STANDARD
    val spec: FloatArray? = null
    val initialValue: Float = Float.NaN
    val radius = 1f

    val rPoly =
        RoundedPolygon.star(
                numVerticesPerRadius = 4,
                rounding = CornerRounding(.352f, .8f),
                innerRounding = CornerRounding(.32f, .8f),
                innerRadius = .352f,
            )
            .toPath()

    return RemoteComposeContextAndroid(600, 600, "Clock", platform = AndroidxRcPlatformServices()) {
        val centerX = floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 2f, DIV)
        val centerY = floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, 2f, DIV)
        val scale =
            floatExpression(
                RemoteContext.FLOAT_WINDOW_WIDTH,
                RemoteContext.FLOAT_WINDOW_HEIGHT,
                MIN,
            )
        val hourHandLength = floatExpression(centerX, centerY, MIN, 0.4f, MUL)
        val minHandLength = floatExpression(centerX, centerY, MIN, 0.3f, MUL)
        val secondAngle = floatExpression(RemoteContext.FLOAT_TIME_IN_SEC, 60f, MOD, 6f, MUL)
        val minAngle = floatExpression(RemoteContext.FLOAT_TIME_IN_MIN, 6f, MUL)
        val hrAngle = floatExpression(RemoteContext.FLOAT_TIME_IN_HR, 30f, MUL)
        val hourWidth = 40f
        val handWidth = 20f
        val baseColor = addNamedColor("android.colorAccent", Color(0xFF1A1A5E).toArgb())
        painter.setColorId(baseColor).setStyle(Paint.Style.FILL).commit()

        // background
        save()
        translate(centerX, centerY)
        scale(scale, scale)
        rotate(45f)
        drawPath(rPoly)
        restore()
        painter.setColor(Color.Green.toArgb()).setStrokeWidth(8f).commit()

        // hour
        save()
        painter
            .setColor(Color.Gray.toArgb())
            .setStrokeWidth(hourWidth)
            .setStrokeCap(Paint.Cap.ROUND)
            .commit()
        rotate(hrAngle, centerX, centerY)
        drawLine(centerX, centerY, centerX, hourHandLength)
        restore()

        // min
        save()
        painter.setColor(Color.White.toArgb()).setStrokeWidth(handWidth).commit()
        rotate(minAngle, centerX, centerY)
        drawLine(centerX, centerY, centerX, minHandLength)
        restore()

        // sec
        save()
        rotate(secondAngle, centerX, centerY)
        painter.setColor(Color.Red.toArgb()).setStrokeWidth(4f).commit()
        drawLine(centerX, centerY, centerX, minHandLength)
        restore()
        // cap
        drawCircle(centerX, centerY, handWidth / 2)
    }
}

/** THIS IS THE GMT WATCH */
@Suppress("RestrictedApiAndroidX")
fun gmt(iconPath: Path? = null): RemoteComposeContext {
    var duration: Float = 1.0f
    var topBezel = "android.colorAccent"
    var bottomBezel = "android.colorControlNormal"
    var textColor = "android.textColor"
    var backgroundColor = "android.colorPrimary"
    var gmtHandColor = "android.colorError"
    var tickColor = "android.colorButtonNormal"

    // 4 Hz tick movement
    var type: Int = RemoteComposeBuffer.EASING_SPLINE_CUSTOM
    var days = arrayOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    var spec: FloatArray? =
        floatArrayOf(
            0f,
            0f,
            0f,
            0.25f,
            0.25f,
            0.25f,
            0.25f,
            0.25f,
            0.5f,
            0.5f,
            0.5f,
            0.5f,
            0.5f,
            0.75f,
            0.75f,
            0.75f,
            0.75f,
            0.75f,
            1f,
            1f,
            1f,
        )
    var initialValue: Float = Float.NaN
    val pathDescription =
        ("M17.6,9.48" +
            "L19.44,6.3" +
            "C19.6,5.99,19.48,5.61,19.18,5.45" +
            "C18.89,5.30,18.53,5.395,18.35,5.67" +
            "L16.47,8.91" +
            "C13.61,7.7,10.39,7.7,7.53,8.91" +
            "L5.65,5.67" +
            "C5.46,5.38,5.07,5.29,4.78,5.47" +
            "C4.5,5.65,4.41,6.01,4.56,6.3" +
            "L6.4,9.48" +
            "C3.3,11.25,1.28,14.44,1,18" +
            "H23" +
            "C22.72,14.44,20.7,11.25,17.6,9.48Z" +
            "M7,15.25" +
            "C6.31,15.25,5.75,14.69,5.75,14" +
            "C5.75,13.31,6.31,12.75,7,12.75" +
            "S8.25,13.31,8.25,14" +
            "C8.25,14.69,7.69,15.25,7,15.25Z" +
            "M17,15.25" +
            "C16.31,15.25,15.75,14.69,15.75,14" +
            "C15.75,13.31,16.31,12.75,17,12.75" +
            "S18.25,13.31,18.25,14" +
            "C18.25,14.69,17.69,15.25,17,15.25Z")

    return RemoteComposeContextAndroid(800, 800, "Clock", platform = AndroidxRcPlatformServices()) {
        val centerX = floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 2f, DIV)
        val centerY = floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, 2f, DIV)
        val rad = floatExpression(centerX, centerY, MIN)
        val hourHandLength = floatExpression(centerX, centerY, MIN, 0.7f, MUL)
        val minHandLength = floatExpression(centerX, centerY, MIN, 0.4f, MUL)
        val h = floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT)
        val w = floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH)
        //  var topBezel = "android.colorAccent"
        //  var bottomBezel = "android.colorControlNormal"

        val bezel2Id = addNamedColor(bottomBezel, Color(0xFF5E1A1A).toArgb())
        val bezel1Id = addNamedColor(topBezel, Color(0xFF1A1A5E).toArgb())
        val textColorId = addNamedColor(textColor, Color(0xFFAAAAAA).toArgb())
        val backgroundColorId = addNamedColor(backgroundColor, Color(0xFF323288).toArgb())
        val gmtColorId = addNamedColor(gmtHandColor, Color(0xFFFF0000).toArgb())
        val tickColorId = addNamedColor(tickColor, Color(0xFF7A7B7B).toArgb())
        val tickMarkColorId = tickColorId
        val bezelMarkColorId = tickColorId
        val dayDateTextColorId = backgroundColorId
        val dayDateBackgroundId = tickColorId
        val hourHandColorId = textColorId
        val minHandColorId = textColorId
        val ticksColorId = textColorId

        val secondAngle =
            floatExpression(
                exp(RemoteContext.FLOAT_TIME_IN_SEC, 60f, MOD, 6f, MUL),
                anim(duration, type, spec, initialValue, 360f),
            )

        val minAngle = floatExpression(RemoteContext.FLOAT_TIME_IN_MIN, 6f, MUL)
        val hrAngle = floatExpression(RemoteContext.FLOAT_TIME_IN_HR, 30f, MUL)
        val gmtAngle =
            floatExpression(
                RemoteContext.FLOAT_TIME_IN_HR,
                RemoteContext.FLOAT_OFFSET_TO_UTC,
                3600f,
                DIV,
                SUB,
                RemoteContext.FLOAT_TIME_IN_MIN,
                60f,
                MOD,
                60f,
                DIV,
                ADD,
                15f,
                MUL,
            )

        val handWidth = 20f

        painter.setColorId(bezel1Id).commit()
        drawCircle(centerX, centerY, rad)

        save()
        painter.setColorId(bezel2Id).commit()
        clipRect(0f, rad, w, h)
        drawCircle(centerX, centerY, rad)
        restore()
        painter
            .setColor(Color.LightGray.toArgb())
            .setStyle(Paint.Style.STROKE)
            .setStrokeWidth(8f)
            .commit()
        drawCircle(centerX, centerY, rad)

        val blackPart = floatExpression(rad, 100f, SUB)
        drawCircle(centerX, centerY, blackPart)
        painter.setColorId(backgroundColorId).setStyle(Paint.Style.FILL).commit()
        drawCircle(centerX, centerY, blackPart)

        // TICK MARKS
        painter.setColorId(tickMarkColorId).setStrokeWidth(2f).commit()

        save()
        for (i in 0 until 60) {
            rotate(6f, centerX, centerY)
            drawLine(centerX, 110f, centerX, 130f)
        }
        restore()

        // ========= Bezel markings ========
        val rect1 = floatExpression(centerX, 10f, ADD)
        val rect2 = floatExpression(centerX, 10f, SUB)
        val tri1 = floatExpression(centerX, 30f, ADD)
        val tri2 = floatExpression(centerX, 30f, SUB)
        val bezelTri1 = floatExpression(centerX, 40f, ADD)
        val bezelTri2 = floatExpression(centerX, 40f, SUB)
        save()
        painter.setTypeface(1, 700, false).setTextSize(80f).setColorId(bezelMarkColorId).commit()
        val path = Path()
        path.moveTo(bezelTri1, 20f)
        path.lineTo(bezelTri2, 20f)
        path.lineTo(centerX, 80f)
        path.close()
        drawPath(addPathData(path))
        rotate(15f, centerX, centerY)
        for (i in 0 until 12) {
            rotate(30f, centerX, centerY)
            drawCircle(centerX, 70f, 8f)
        }
        restore()

        // ============= Bezel 2,4,6... =========

        save()
        for (i in 0 until 12) {
            rotate(30f, centerX, centerY)
            if (i != 11) drawTextAnchored("" + (i * 2 + 2), centerX, 50f, 0f, 0f, 0)
            if ((i + 1) % 3 != 0) {
                drawCircle(centerX, 150f, 20f)
            } else {
                if (i == 5 || i == 8) {
                    drawRect(rect1, 130f, rect2, 180f)
                } else if (i == 11) {
                    val path = Path()
                    path.moveTo(tri1, 130f)
                    path.lineTo(tri2, 130f)
                    path.lineTo(centerX, 190f)
                    path.close()
                    drawPath(addPathData(path))
                }
            }
        }
        restore()
        // =========== DATE Complication ===========
        val dateLeft = floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 220f, SUB)
        val dateTop = floatExpression(centerY, 30f, SUB)
        val dateRight = floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 140f, SUB)
        val dateBottom = floatExpression(centerY, 30f, ADD)
        val cx = floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, (140f + 220f) / 2, SUB)
        drawRect(dateLeft, dateTop, dateRight, dateBottom)
        painter.setTypeface(3, 700, false).setTextSize(60f).setColorId(dayDateTextColorId).commit()
        val id = createTextFromFloat(RemoteContext.FLOAT_DAY_OF_MONTH, 2, 0, 0)
        drawTextAnchored(id, cx, centerY, 0f, 0f, 0)

        // =============== DAY Complication ===============

        val dayCenterX = floatExpression(centerX, rad, 280f, SUB, ADD)
        val dayLeft = floatExpression(dayCenterX, 46f, SUB)
        val dayRight = floatExpression(dayCenterX, 46f, ADD)
        save()
        painter.setColorId(dayDateBackgroundId).commit()

        clipRect(dayLeft, dateTop, dayRight, dateBottom)
        drawCircle(centerX, centerY, 200f)

        val angle = 360f / 7f

        painter.setColorId(dayDateTextColorId).setTextSize(40f).commit()
        rotate(
            floatExpression(
                exp(1f, RemoteContext.FLOAT_WEEK_DAY, SUB, angle, MUL),
                anim(0.5f, RemoteComposeBuffer.EASING_CUBIC_OVERSHOOT),
            ),
            centerX,
            centerY,
        )
        for (i in 0 until 7) {
            drawTextAnchored(days[i], dayCenterX, centerY, 0f, 0f, 0)
            rotate(angle, centerX, centerY)
        }
        restore()
        var iPath = iconPath
        if (iPath == null) {
            iPath = DemoUtils.parsePath(pathDescription) as Path
        }
        val conditionAlwaysTrue = true
        /** Icon */
        if (conditionAlwaysTrue) { // iPath != null) {
            save()
            translate(centerX, centerY)

            scale(5f, 5f)
            translate(-12f, -40f)

            painter
                .setColor(Color(0xFFA4C639).toArgb())
                .setStrokeCap(Paint.Cap.ROUND)
                .setStyle(Paint.Style.FILL)
                .commit()
            drawPath(iPath)
            restore()
        }
        // GMT HAND
        save()
        painter
            .setColorId(gmtColorId)
            .setStrokeWidth(5f)
            .setStrokeCap(Paint.Cap.ROUND)
            .setStyle(Paint.Style.FILL_AND_STROKE)
            .commit()
        rotate(gmtAngle, centerX, centerY)
        // drawLine(centerX, centerY, centerX, minHandLength)
        val gmtPath = Path()
        val top = floatExpression(minHandLength, 20f, SUB)
        val leftEdge = floatExpression(centerX, 20f, SUB)
        val rightEdget = floatExpression(centerX, 20f, ADD)

        gmtPath.moveTo(centerX, centerY)
        gmtPath.lineTo(centerX, minHandLength)
        gmtPath.lineTo(leftEdge, minHandLength)
        gmtPath.lineTo(centerX, top)
        gmtPath.lineTo(rightEdget, minHandLength)
        gmtPath.lineTo(centerX, minHandLength)
        gmtPath.lineTo(centerX, centerY)

        gmtPath.close()
        drawPath(addPathData(gmtPath))
        restore()

        // hour hand
        painter
            .setColorId(hourHandColorId)
            .setStrokeWidth(handWidth)
            .setStrokeCap(Paint.Cap.ROUND)
            .commit()
        save()
        rotate(hrAngle, centerX, centerY)
        drawLine(centerX, centerY, centerX, hourHandLength)

        restore()

        save()
        // min hand
        rotate(minAngle, centerX, centerY)
        drawLine(centerX, centerY, centerX, minHandLength)
        restore()
        // second hand
        save()
        rotate(secondAngle, centerX, centerY)
        painter.setColorId(minHandColorId).setStrokeWidth(4f).setStrokeCap(Paint.Cap.ROUND).commit()
        drawLine(centerX, centerY, centerX, 128f)
        drawCircle(centerX, 200f, 16f)
        restore()
        drawCircle(centerX, centerY, handWidth)
        painter.setColor(Color.Black.toArgb()).commit()
        // circle on top
        drawCircle(centerX, centerY, handWidth / 2)
    }
}

@Suppress("RestrictedApiAndroidX")
fun graph(iconPath: Path? = null): RemoteComposeContext {
    return RemoteComposeContextAndroid(600, 600, "Clock", platform = AndroidxRcPlatformServices()) {
        val centerX = floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 2f, DIV)
        val centerY = floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, 2f, DIV)
        val rad = floatExpression(centerX, centerY, MIN)
        val hourHandLength = floatExpression(centerX, centerY, MIN, 0.5f, MUL)
        val minHandLength = floatExpression(centerX, centerY, MIN, 0.2f, MUL)
        val secondAngle =
            floatExpression(exp(RemoteContext.FLOAT_TIME_IN_SEC, 60f, MOD, 90f, MUL), anim(0.999f))
        val minAngle = floatExpression(RemoteContext.FLOAT_TIME_IN_MIN, 6f, MUL)
        val hrAngle = floatExpression(RemoteContext.FLOAT_TIME_IN_HR, 30f, MUL)
        val handWidth = 20f
        painter.setColor(Color.Black.toArgb()).commit()
        drawCircle(centerX, centerY, rad)

        painter.setColor(Color.Green.toArgb()).setStrokeWidth(8f).commit()
    }
}
