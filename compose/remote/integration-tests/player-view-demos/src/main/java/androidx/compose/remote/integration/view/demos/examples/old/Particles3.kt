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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.compose.remote.creation.*
import androidx.compose.remote.creation.RemoteComposeWriter.IMAGE_SCALE_FIT
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.pow
import kotlin.random.Random

// =========================== confetti ====================================
@SuppressLint("RestrictedApiAndroidX")
fun createBall(): Bitmap {
    val ball = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
    val w = ball.width
    val h = ball.height
    val cx = (w / 2).toFloat()
    val cy = (h / 2).toFloat()
    val radius = cx * 0.9f
    val radius2 = radius * radius
    val data = IntArray(w * h)
    for (i in data.indices) {
        val x = i % w
        val y = i / w
        val dx = x - cx
        val dy = y - cy
        val dist2 = dx * dx + dy * dy
        if (dist2 > radius2) {
            continue
        }
        val norm2 = radius * radius - dist2
        val bright = (norm2 * 255 / radius2).toInt()
        data[i] = -0x78000000 + 0x10101 * bright
    }
    ball.setPixels(data, 0, w, 0, 0, w, h)
    return ball
}

@SuppressLint("RestrictedApiAndroidX")
fun createTeardrop(): Bitmap {
    val ball = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
    val w = ball.width
    val h = ball.height
    val cx = (w / 2).toFloat()
    val cy = (h / 2).toFloat()
    val radius = cx * 0.9
    val radius2 = radius * radius
    val data = IntArray(w * h)
    for (i in data.indices) {
        val x = i % w
        val y = i / w
        val py: Double = (cx - x) / radius
        val px: Double = (cy - y) / radius
        val px2 = px * px
        val py2 = py * py
        var d = (1 - px2) * ((1 - px) / 2).pow(1.5) - py2
        if (d > 0) {
            d = Math.pow(d, 0.25)
            val bright = (255 * d).toInt()
            data[i] = -0x78000000 or (0x10101 * bright)
            // data[i] = -0x56000000 + 0x10101 * bright
            // data[i] = -0x1000000 + 0x10101 * bright

        }
    }
    ball.setPixels(data, 0, w, 0, 0, w, h)
    return ball
}

/** Creates a spinning test */
@SuppressLint("RestrictedApiAndroidX")
fun rain1(): RemoteComposeContext {

    return RemoteComposeContextAndroid(
        800,
        800,
        "Clock",
        6,
        0,
        platform = AndroidxRcPlatformServices(),
    ) {
        root {
            box(Modifier.fillMaxSize()) {
                canvas(Modifier.fillMaxSize()) {
                    val width = ComponentWidth()
                    val height = ComponentHeight()
                    val centerX = width / 2f
                    val centerY = height / 2f
                    painter.setColor(Color(0xFF675236).toArgb()).commit()
                    drawRoundRect(
                        10f,
                        10f,
                        (width - 20f).toFloat(),
                        (height - 20f).toFloat(),
                        120f,
                        120f,
                    )
                    painter
                        .setLinearGradient(
                            0f,
                            0f,
                            0f,
                            height.toFloat(),
                            intArrayOf(Color(0xFF6688FF).toArgb(), Color(0xFF222255).toArgb()),
                            null,
                            Shader.TileMode.CLAMP,
                        )
                        .setColor(Color.Blue.toArgb())
                        .commit()
                    val rad = width * 0.4f
                    drawCircle(centerX.toFloat(), centerY.toFloat(), rad.toFloat())
                    val circle =
                        addPathData(
                            createCirclePath(centerX.toFloat(), centerY.toFloat(), rad.toFloat())
                        )
                    addClipPath(circle)
                    droppingEngine(
                        writer as RemoteComposeWriterAndroid,
                        ContinuousSec().toFloat(),
                        width,
                        height,
                    )

                    var id = textCreateId("Simple test")
                    drawTextAnchored(id, centerX.toFloat(), centerY.toFloat(), 0f, 0f, 2)
                }
            }
        }
    }
}

@SuppressLint("RestrictedApiAndroidX")
fun droppingEngine(rcDoc: RemoteComposeWriterAndroid, event: Float, width: RFloat, height: RFloat) {
    with(rcDoc) {
        painter.setTextSize(64f).setColor(android.graphics.Color.YELLOW).commit()
        painter.setColor(0xFFFFFF).setAlpha(1f).setStyle(Paint.Style.FILL).commit()

        val image = addBitmap(createTeardrop())

        impulse(20f, event) {
            val variables: Array<RFloat> = Array<RFloat>(6, { RFloat(this, 0f) })
            val ps =
                createParticles(
                    variables,
                    arrayOf((width * rand()), (height * rand() * -2f), 0f, 0f, (index() + 10f), 1f),
                    40,
                )
            val (x, y, dx, dy, dist, alpha) = variables

            val dt = deltaTime()
            impulseProcess() { // current_time - envnt_time  < 0.5
                particlesLoops(
                    ps,
                    (y - height),
                    arrayOf(
                        x + dx * dt,
                        y + dist * dt * 24f,
                        dx,
                        dy + 98f + dist * dt * 2f,
                        dist,
                        alpha,
                    ),
                ) {
                    save()
                    translate(x.toFloat(), y.toFloat())
                    scale(dist.toFloat(), dist.toFloat())
                    scale(0.04f, 0.04f)
                    drawBitmap(
                        image,
                        0f,
                        0f,
                        ImpulseDemo.sBall.width.toFloat(),
                        ImpulseDemo.sBall.height.toFloat(),
                        "",
                    )
                    restore()
                }
            }
        }
    }
}

// ================================================================================ */
/** Warp hyper space effect */
@SuppressLint("RestrictedApiAndroidX")
fun warp(): RemoteComposeContext {

    return RemoteComposeContextAndroid(
        800,
        800,
        "Clock",
        6,
        0,
        platform = AndroidxRcPlatformServices(),
    ) {
        root {
            box(Modifier.fillMaxSize()) {
                canvas(Modifier.fillMaxSize()) {
                    val width = ComponentWidth()
                    val height = ComponentHeight()
                    val centerX = width / 2f
                    val centerY = height / 2f
                    painter.setColor(Color(0xFF675236).toArgb()).commit()
                    drawRoundRect(
                        10f,
                        10f,
                        (width - 20f).toFloat(),
                        (height - 20f).toFloat(),
                        120f,
                        120f,
                    )
                    painter.setColor(Color(0xFF000033).toArgb()).commit()
                    val rad = width * 0.4f
                    drawCircle(centerX.toFloat(), centerY.toFloat(), rad.toFloat())
                    //  val circle = addPathData(createCirclePath(centerX, centerY, rad))
                    // addClipPath(circle)
                    warpEngine(
                        writer as RemoteComposeWriterAndroid,
                        ContinuousSec().toFloat(),
                        width,
                        height,
                        centerX,
                        centerY,
                    )
                    val id = textCreateId("Simple test")
                    drawTextAnchored(id, centerX.toFloat(), centerY.toFloat(), 0f, 0f, 2)
                }
            }
        }
    }
}

@SuppressLint("RestrictedApiAndroidX")
fun warpEngine(
    rcDoc: RemoteComposeWriterAndroid,
    event: Float,
    width: RFloat,
    height: RFloat,
    centerX: RFloat,
    centerY: RFloat,
) {

    with(rcDoc) {
        painter.setTextSize(64f).setColor(android.graphics.Color.YELLOW).commit()
        painter
            .setShader(0)
            .setColor(0xFFFFFF)
            .setAlpha(1f)
            .setStyle(Paint.Style.STROKE)
            .setStrokeWidth(3f)
            .commit()

        impulse(20f, event) {
            val variables: Array<RFloat> = Array<RFloat>(4, { RFloat(this, 0f) })
            val ps =
                createParticles(
                    variables,
                    arrayOf(rand() * (2 * Math.PI.toFloat()), rand() * centerX, 0f, 0f),
                    200,
                )
            val (angle, rad, start, end) = variables

            val dt = deltaTime()
            impulseProcess() { // current_time - envnt_time  < 0.5
                particlesLoops(
                    ps,
                    (start + rad - height),
                    arrayOf(angle, rad, start + dt * 400f, end + dt * 600f),
                ) {
                    val sin = sin(angle)
                    val cos = cos(angle)

                    val x1 = sin * (rad + start) + centerX
                    val y1 = cos * (rad + start) + centerY
                    val x2 = sin * (rad + end) + centerX
                    val y2 = cos * (rad + end) + centerY
                    drawLine(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat())
                }
            }
        }
    }
}

// ================================================================================ */

/** Creates a spinning test */
@SuppressLint("RestrictedApiAndroidX")
fun fireworks(): RemoteComposeContext {

    return RemoteComposeContextAndroid(
        800,
        800,
        "Clock",
        6,
        0,
        platform = AndroidxRcPlatformServices(),
    ) {
        root {
            box(Modifier.fillMaxSize()) {
                canvas(Modifier.fillMaxSize()) {
                    val width = ComponentWidth()
                    val height = ComponentHeight()
                    val centerX = width / 2f
                    val centerY = height / 2f
                    painter.setColor(Color(0xFF675236).toArgb()).commit()
                    drawRoundRect(
                        10f,
                        10f,
                        (width - 20f).toFloat(),
                        (height - 20f).toFloat(),
                        120f,
                        120f,
                    )
                    painter
                        .setLinearGradient(
                            0f,
                            0f,
                            0f,
                            height.toFloat(),
                            intArrayOf(Color(0xFF6688FF).toArgb(), Color(0xFF222255).toArgb()),
                            null,
                            Shader.TileMode.CLAMP,
                        )
                        .setColor(Color.Blue.toArgb())
                        .commit()
                    val rad = width * 0.4f
                    drawCircle(centerX.toFloat(), centerY.toFloat(), rad.toFloat())
                    //          val circle = addPathData(createCirclePath(centerX, centerY, rad))
                    //          addClipPath(circle)
                    fireworksEngine(
                        writer as RemoteComposeWriterAndroid,
                        ContinuousSec().toFloat(),
                        width,
                        height,
                    )

                    var id = textCreateId("Simple test")
                    drawTextAnchored(id, centerX.toFloat(), centerY.toFloat(), 0f, 0f, 2)
                }
            }
        }
    }
}

@SuppressLint("RestrictedApiAndroidX")
fun fireworksEngine(
    rcDoc: RemoteComposeWriterAndroid,
    event: Float,
    width: RFloat,
    height: RFloat,
) {

    val pi2 = 2 * Math.PI.toFloat()
    with(rcDoc) {
        painter.setTextSize(64f).setColor(android.graphics.Color.YELLOW).commit()
        painter
            .setShader(0)
            .setColor(0xFFFFFF)
            .setAlpha(1f)
            .setStyle(Paint.Style.STROKE)
            .setStrokeWidth(3f)
            .commit()
        val count = 200
        val groups = 5f
        val step = count / groups
        impulse(20f, event) {
            val variables: Array<RFloat> = Array<RFloat>(6, { RFloat(this, 0f) })
            val set = round(index() / step) / groups
            val ps =
                createParticles(
                    variables,
                    arrayOf(
                        width * set / 2f + width / 4f,
                        height,
                        (set * -1f + 0.5f) * 100f,
                        (round(index() / step) * 231f) % 12f * 4f - 130f,
                        round(index() / step) * -2f,
                        index(),
                    ),
                    count,
                )
            val (x, y, dx, dy, phase, index) = variables
            val thresh = 30f
            val dt = deltaTime()
            impulseProcess() { // current_time - envnt_time  < 0.5
                particlesLoops(
                    ps,
                    (ifElse(phase + 100f, -1f, phase + 110f)),
                    arrayOf(
                        x + dx * dt * 3f,
                        y + dy * 3f * dt,
                        ifElse(phase - thresh, sin(index * pi2 / 10f) * 70f, dx * 0.99f),
                        ifElse(
                            phase - thresh,
                            cos(index * pi2 / 10f) * 70f,
                            dy * 0.99f + dt * 9.8f * 2f,
                        ),
                        ifElse(phase - thresh, -120, phase + dt * 10f),
                        index,
                    ),
                ) {
                    // drawRect(x.toFloat(), y.toFloat(),(x+1f).toFloat(), (y+1f).toFloat())
                    drawCircle(x.toFloat(), y.toFloat(), 2f)
                }
            }
        }
    }
}

/** Creates a spinning test */
@SuppressLint("RestrictedApiAndroidX")
fun fireworks2(): RemoteComposeContext {

    return RemoteComposeContextAndroid(
        800,
        800,
        "Clock",
        6,
        0,
        platform = AndroidxRcPlatformServices(),
    ) {
        root {
            box(Modifier.fillMaxSize()) {
                canvas(Modifier.fillMaxSize()) {
                    val width = ComponentWidth()
                    val height = ComponentHeight()
                    val centerX = width / 2f
                    val centerY = height / 2f
                    painter.setColor(Color(0xFF675236).toArgb()).commit()
                    drawRoundRect(
                        10f,
                        10f,
                        (width - 20f).toFloat(),
                        (height - 20f).toFloat(),
                        120f,
                        120f,
                    )
                    painter
                        .setLinearGradient(
                            0f,
                            0f,
                            0f,
                            height.toFloat(),
                            intArrayOf(Color(0xFF6688FF).toArgb(), Color(0xFF222255).toArgb()),
                            null,
                            Shader.TileMode.CLAMP,
                        )
                        .setColor(Color.Blue.toArgb())
                        .commit()
                    val rad = width * 0.4f
                    drawCircle(centerX.toFloat(), centerY.toFloat(), rad.toFloat())
                    //          val circle = addPathData(createCirclePath(centerX, centerY, rad))
                    //          addClipPath(circle)
                    fireworksEngine2(
                        writer as RemoteComposeWriterAndroid,
                        ContinuousSec().toFloat(),
                        width,
                        height,
                    )
                    fireworksEngine2(
                        writer as RemoteComposeWriterAndroid,
                        ContinuousSec().toFloat(),
                        width,
                        height,
                    )
                    fireworksEngine2(
                        writer as RemoteComposeWriterAndroid,
                        ContinuousSec().toFloat(),
                        width,
                        height,
                    )

                    var id = textCreateId("Simple test")
                    drawTextAnchored(id, centerX.toFloat(), centerY.toFloat(), 0f, 0f, 2)
                }
            }
        }
    }
}

@SuppressLint("RestrictedApiAndroidX")
fun fireworksEngine2(
    rcDoc: RemoteComposeWriterAndroid,
    event: Float,
    width: RFloat,
    height: RFloat,
) {

    val pi2 = 2 * Math.PI.toFloat()
    with(rcDoc) {
        painter.setTextSize(64f).setColor(android.graphics.Color.YELLOW).commit()

        painter
            .setShader(0)
            .setColor(0xFFFFFF)
            .setAlpha(1f)
            .setStyle(Paint.Style.STROKE)
            .setStrokeWidth(3f)
            .commit()
        val count = 300
        val groups = 5f
        val noize = (Math.random() * 121).toFloat()
        val step = count / groups
        val angle = pi2 * 2f / step
        impulse(20f, event) {
            val variables: Array<RFloat> = Array<RFloat>(6, { RFloat(this, 0f) })
            val set = round(index() / step) / groups
            val ps =
                createParticles(
                    variables,
                    arrayOf(
                        width * set / 2f + width / 4f,
                        height,
                        (set * -1f + 0.5f) * (100f * Math.random().toFloat()),
                        (round(index() / step) * 231f) % 12f * 4f - 130f,
                        round(index() / step) * -2f,
                        index(),
                    ),
                    count,
                )
            val (x, y, dx, dy, phase, index) = variables
            val thresh = 30f
            val dt = deltaTime()
            // val pathId = pathCreate(0f,0f);
            impulseProcess() { // current_time - envnt_time  < 0.5
                particlesLoops(
                    ps,
                    (ifElse(phase + 100f, -1f, phase + 110f)),
                    arrayOf(
                        x + dx * dt * 3f,
                        y + dy * 3f * dt,
                        ifElse(
                            phase - thresh,
                            sin(index * angle) * 70f * sin(index * noize),
                            dx * 0.99f,
                        ),
                        ifElse(
                            phase - thresh,
                            cos(index * angle) * 70f * sin(index * noize),
                            dy * 0.99f + dt * 9.8f * 2f,
                        ),
                        ifElse(phase - thresh, -120, phase + dt * 10f),
                        index,
                    ),
                ) {
                    //                    pathAppendMoveTo(pathId,  x.toFloat(), y.toFloat())
                    //                    pathAppendLineTo(pathId, (x -
                    // dx/2f).toFloat(),(y-dy/2f).toFloat());
                    val id =
                        addColorExpression(
                            ((round(index / step) * 65.23f) % 1f).toFloat(),
                            0.9f,
                            ifElse(phase, 0.2, 1f).toFloat(),
                        )
                    painter.setColorId(id.toInt()).commit()
                    drawLine(
                        x.toFloat(),
                        y.toFloat(),
                        (x - dx / 3f).toFloat(),
                        (y - dy / 3f).toFloat(),
                    )
                    // drawRect(x.toFloat(), y.toFloat(),(x+1f).toFloat(), (y+1f).toFloat())
                    // drawCircle(x.toFloat(), y.toFloat(), 2f)
                }

                //                drawPath(pathId)
                //                pathAppendReset(pathId)
            }
        }
    }
}

/** Creates a spinning test */
@SuppressLint("RestrictedApiAndroidX")
fun confetti(): RemoteComposeContext {

    return RemoteComposeContextAndroid(
        800,
        800,
        "Clock",
        6,
        0,
        platform = AndroidxRcPlatformServices(),
    ) {
        root {
            box(Modifier.fillMaxSize()) {
                canvas(Modifier.fillMaxSize()) {
                    val width = ComponentWidth()
                    val height = ComponentHeight()
                    val centerX = width / 2f
                    val centerY = height / 2f
                    painter.setColor(Color(0xFF675236).toArgb()).commit()
                    drawRoundRect(
                        10f,
                        10f,
                        (width - 20f).toFloat(),
                        (height - 20f).toFloat(),
                        120f,
                        120f,
                    )
                    painter
                        .setLinearGradient(
                            0f,
                            0f,
                            0f,
                            height.toFloat(),
                            intArrayOf(Color(0xFF6688FF).toArgb(), Color(0xFF222255).toArgb()),
                            null,
                            Shader.TileMode.CLAMP,
                        )
                        .setColor(Color.Blue.toArgb())
                        .commit()
                    val rad = width * 0.4f
                    drawCircle(centerX.toFloat(), centerY.toFloat(), rad.toFloat())
                    //          val circle = addPathData(createCirclePath(centerX, centerY, rad))
                    //          addClipPath(circle)
                    val imageId = addBitmap(genConfettiImage(40, 50, 50))
                    for (n in 0..9) {
                        confettiEngine(
                            writer as RemoteComposeWriterAndroid,
                            ContinuousSec().toFloat(),
                            width,
                            height,
                            imageId,
                            n * 10,
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("RestrictedApiAndroidX")
fun confettiEngine(
    rcDoc: RemoteComposeWriterAndroid,
    event: Float,
    width: RFloat,
    height: RFloat,
    imageId: Int,
    nEng: Int,
) {
    with(rcDoc) {
        painter.setTextSize(64f).setColor(android.graphics.Color.YELLOW).commit()
        painter.setColor(0xFFFFFF).setAlpha(1f).setStyle(Paint.Style.FILL).commit()

        val pcount = 40
        //    val paths = genConfetti(pcount)
        //    val pathIds = Array<Int>(paths.size, { addPathData(paths[it]) })
        //    val list = addList(*pathIds.toIntArray())

        impulse(20f, event) {
            val variables: Array<RFloat> = Array<RFloat>(4, { RFloat(this, 0f) })
            val ps =
                createParticles(
                    variables,
                    arrayOf(
                        (width * rand()),
                        (height * rand() * -2f),
                        (index() / 20f + 5f),
                        index(),
                    ),
                    pcount,
                )
            val (x, y, dist, index) = variables
            painter.setColor(Color.Red.toArgb()).setStyle(Paint.Style.FILL).commit()
            val dt = deltaTime()
            impulseProcess() { // current_time - envnt_time  < 0.5
                particlesLoops(ps, (y - height), arrayOf(x, y + dist * dt * 100f, dist, index)) {
                    save()

                    translate((x + sin(index * 341f + y / 200f) * 30f).toFloat(), y.toFloat())
                    //  translate((x).toFloat(), y.toFloat())
                    scale(dist.toFloat(), dist.toFloat(), 25f, 25f)
                    scale(
                        (sin(y / 7000f) * 0.2f + 0.4f).toFloat(),
                        (sin((index * 321f + animationTime() * 3f) * (nEng / 100f + 1)) * 0.5f)
                            .toFloat(),
                        25f,
                        25f,
                    )

                    var sTop = 0f
                    var sLeft = (index * 50f).toFloat()
                    var sRight = ((index + 1f) * 50f).toFloat()
                    var sBottom = 50f
                    drawScaledBitmap(
                        imageId,
                        sLeft,
                        sTop,
                        sRight,
                        sBottom,
                        0f,
                        0f,
                        50f,
                        50f,
                        IMAGE_SCALE_FIT,
                        1f,
                        "",
                    )

                    restore()
                }
            }
        }
    }
}

// ================================= utilities =====================
@SuppressLint("RestrictedApiAndroidX", "PrimitiveInCollection")
fun genConfetti(count: Int): ArrayList<RemotePath> {
    val paths = ArrayList<RemotePath>()
    // Generate random confetti
    val r: Random = Random
    val scale = 50f
    val shift = scale / 2
    for (i in 0..count) {

        val path = RemotePath()
        paths.add(path)
        val x = r.nextFloat()
        val y = r.nextFloat()

        // Create a random path
        path.moveTo(x, y)
        var x1: Float
        var x2: Float
        var x3: Float
        var x4: Float
        var dx: Float
        var y1: Float
        var y2: Float
        var y3: Float
        var y4: Float
        var dy: Float
        var curve: Float
        // Randomly choose a path command
        val command: Int = r.nextInt(4)
        //            for (int j = 0; j < 5; j++) {
        when (command) {
            0,
            1 -> {
                x1 = x + (Math.random() * scale).toInt() - shift
                y1 = y + (Math.random() * scale).toInt() - shift
                curve = r.nextFloat()
                dx = (x1 - x) * curve
                dy = (y1 - y) * curve
                x2 = x - dy
                y2 = y + dx

                x3 = x2 - dx
                y3 = y2 - dy
                path.lineTo(x1, y1)
                path.lineTo(x2, y2)
                path.lineTo(x3, y3)
                path.close()
            }
            2 -> {
                x2 = x + (Math.random() * scale).toInt() - shift
                y2 = y + (Math.random() * scale).toInt() - shift
                dx = x2 - x
                dy = y2 - y
                curve = r.nextFloat()
                x1 = (x2 + x) / 2 + dy * curve
                y1 = (y2 + y) / 2 - dx * curve

                path.quadTo(x1, y1, x2, y2)
                x3 = x2 - dy / 2
                y3 = y2 + dx / 2

                x4 = x - dy / 2
                y4 = y + dx / 2

                path.quadTo(x1, y1, x2, y2)
                path.lineTo(x3, y3)

                x1 = (x4 + x3) / 2 + dy * curve
                y1 = (y4 + y3) / 2 - dx * curve
                path.quadTo(x1, y1, x4, y4)
                path.close()
            }
            3 -> {
                x3 = x + (Math.random() * scale).toInt() - shift
                y3 = y + (Math.random() * scale).toInt() - shift
                dx = x3 - x
                dy = y3 - y
                curve = r.nextFloat() - 0.5f

                x1 = x + dx * 0.3f + curve * dy
                y1 = y + dy * 0.3f - curve * dx
                x2 = x + dx * 0.6f + curve * dy
                y2 = y + dy * 0.6f - curve * dx

                path.cubicTo(x1, y1, x2, y2, x3, y3)
                x1 = x3 + dy / 3
                y1 = y3 - dx / 3
                path.lineTo(x1, y1)
                x4 = x1 - dx
                y4 = y1 - dy
                dx = x4 - x1
                dy = y4 - y1
                x1 = x1 + dx * 0.3f - curve * dy
                y1 = y1 + dy * 0.3f + curve * dx
                x2 = x1 + dx * 0.6f - curve * dy
                y2 = y1 + dy * 0.6f + curve * dx
                path.cubicTo(x1, y1, x2, y2, x4, y4)
                path.close()
            }
        }
    }
    return paths
}

// ================================= utilities =====================
@SuppressLint("RestrictedApiAndroidX")
fun genConfettiImage(n: Int, width: Int, height: Int): Bitmap {

    val image = Bitmap.createBitmap(n * width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(image)
    val paint = Paint()
    // Generate random confetti
    val r: Random = Random
    val scale = width
    val shift = scale / 2
    val path = Path()
    for (i in 0..n) {
        path.reset()
        val x = r.nextFloat() + width * i
        val y = r.nextFloat()

        // Create a random path
        path.moveTo(x, y)
        var x1: Float
        var x2: Float
        var x3: Float
        var x4: Float
        var dx: Float
        var y1: Float
        var y2: Float
        var y3: Float
        var y4: Float
        var dy: Float
        var curve: Float
        // Randomly choose a path command
        val command: Int = r.nextInt(4)
        //            for (int j = 0; j < 5; j++) {
        when (command) {
            0,
            1 -> {
                x1 = x + (Math.random() * scale).toInt() - shift
                y1 = y + (Math.random() * scale).toInt() - shift
                curve = r.nextFloat()
                dx = (x1 - x) * curve
                dy = (y1 - y) * curve
                x2 = x - dy
                y2 = y + dx

                x3 = x2 - dx
                y3 = y2 - dy
                path.lineTo(x1, y1)
                path.lineTo(x2, y2)
                path.lineTo(x3, y3)
                path.close()
            }
            2 -> {
                x2 = x + (Math.random() * scale).toInt() - shift
                y2 = y + (Math.random() * scale).toInt() - shift
                dx = x2 - x
                dy = y2 - y
                curve = r.nextFloat()
                x1 = (x2 + x) / 2 + dy * curve
                y1 = (y2 + y) / 2 - dx * curve

                path.quadTo(x1, y1, x2, y2)
                x3 = x2 - dy / 2
                y3 = y2 + dx / 2

                x4 = x - dy / 2
                y4 = y + dx / 2

                path.quadTo(x1, y1, x2, y2)
                path.lineTo(x3, y3)

                x1 = (x4 + x3) / 2 + dy * curve
                y1 = (y4 + y3) / 2 - dx * curve
                path.quadTo(x1, y1, x4, y4)
                path.close()
            }
            3 -> {
                x3 = x + (Math.random() * scale).toInt() - shift
                y3 = y + (Math.random() * scale).toInt() - shift
                dx = x3 - x
                dy = y3 - y
                curve = r.nextFloat() - 0.5f

                x1 = x + dx * 0.3f + curve * dy
                y1 = y + dy * 0.3f - curve * dx
                x2 = x + dx * 0.6f + curve * dy
                y2 = y + dy * 0.6f - curve * dx

                path.cubicTo(x1, y1, x2, y2, x3, y3)
                x1 = x3 + dy / 3
                y1 = y3 - dx / 3
                path.lineTo(x1, y1)
                x4 = x1 - dx
                y4 = y1 - dy
                dx = x4 - x1
                dy = y4 - y1
                x1 = x1 + dx * 0.3f - curve * dy
                y1 = y1 + dy * 0.3f + curve * dx
                x2 = x1 + dx * 0.6f - curve * dy
                y2 = y1 + dy * 0.6f + curve * dx
                path.cubicTo(x1, y1, x2, y2, x4, y4)
                path.close()
            }
        }
        paint.setColor(Color.hsv(360f * r.nextFloat(), .8f, .9f, 1f).toArgb())
        canvas.drawPath(path, paint)
    }
    return image
}
