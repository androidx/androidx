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

import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.Shader
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.*
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** Creates a spinning test */
@Suppress("RestrictedApiAndroidX")
fun particleSphere(): RemoteComposeContext {

    return RemoteComposeContextAndroid(800, 800, "spinning", 6, 0, AndroidxRcPlatformServices()) {
        root {
            box(Modifier.fillMaxSize()) {
                canvas(Modifier.fillMaxSize()) {
                    val width = ComponentWidth()
                    val height = ComponentHeight()
                    val centerX = width / 2f
                    val centerY = height / 2f
                    painter.setColor(Color(0xFF110F0C).toArgb()).commit()
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
                            intArrayOf(Color(0xFF9FAAC9).toArgb(), Color(0xFF111122).toArgb()),
                            null,
                            Shader.TileMode.CLAMP,
                        )
                        .setColor(Color.Blue.toArgb())
                        .commit()
                    val rad = min(width, height) * 0.3f
                    drawCircle(centerX.toFloat(), centerY.toFloat(), rad.toFloat())
                    /*
                    val circle = addPathData(createCirclePath(
                        centerX.toFloat(),
                        centerY.toFloat(),
                        rad.toFloat()))
                     addClipPath(circle)
                     */
                    droppingEngine2(
                        writer as RemoteComposeWriterAndroid,
                        0f,
                        width,
                        height,
                        centerX,
                        centerY,
                        rad,
                    )

                    var id = createTextFromFloat(Seconds().toFloat(), 2, 0, 0)
                    painter.setColor(Color(0xFFA0FF99).toArgb()).setTextSize(123f).commit()

                    drawTextAnchored(id, centerX.toFloat(), centerY.toFloat(), 0f, 0f, 2)
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun droppingEngine2(
    rcDoc: RemoteComposeWriterAndroid,
    event: Float,
    width: RFloat,
    height: RFloat,
    centerX: RFloat,
    centerY: RFloat,
    rad: RFloat,
) {
    with(rcDoc) {
        val size = 2
        val diameter = size.toFloat() * 2
        val image = addBitmap(createMark(size))
        val pCount = 800
        painter.setColor(Color(0xFFFFC45E).toArgb()).setAlpha(1f).setShader(0).commit()
        val angle = ContinuousSec() * 0.2f
        impulse(200f, event) {
            val variables: Array<RFloat> = Array<RFloat>(3, { RFloat(this, 0f) })
            val pi2 = (2 * Math.PI).toFloat()
            val ps =
                createParticles(
                    variables,
                    arrayOf(
                        ((rand()) * pi2),
                        ((((rand() * 797f) % pCount.toFloat()) / pCount.toFloat()) * pi2),
                        sign(max(0f, rand() - 0.8f)) * 0.2f,
                    ),
                    pCount,
                )
            val (lat, lon, pos) = variables

            val dt = deltaTime()

            impulseProcess() { // current_time - envnt_time  < 0.5
                particlesLoops(
                    ps,
                    null,
                    arrayOf(
                        lat,
                        lon, // (lon + dt) % pi2,
                        pos,
                    ),
                ) {
                    save()
                    val y = cos(lat) * rad
                    val x = cos(lon + angle) * sqrt(rad * rad - y * y)

                    x.toFloat()
                    y.toFloat()
                    val scale = (rad * rad - sqSum(x, y)) / (rad * rad)
                    // painter.setAlpha(1f).commit()
                    translate(
                        (x * (pos + 1f) + centerX - (size / 2f)).toFloat(),
                        (y * (pos + 1f) + centerY - (size / 2f)).toFloat(),
                    )
                    // scale(scale.toFloat(), scale.toFloat())
                    // scale(0.04f, 0.04f)

                    //                    conditionalOperations(Rc.Condition.LT,
                    // (lon+angle).toFloat(), Math.PI.toFloat())
                    //                    scale(0.2f, 0.2f)
                    // painter.setAlpha(scale.toFloat()).commit()
                    //                    endConditionalOperations()

                    drawRoundRect(0f, 0f, diameter, diameter, diameter, diameter)

                    restore()
                }
            }
        }
    }
}

// =========================== confetti ====================================

@Suppress("RestrictedApiAndroidX")
fun createMark(size: Int): Bitmap {
    val ball = createBitmap(size, size, Bitmap.Config.ARGB_8888)
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

/** NOISE_FROM operator calculate a random 0..1 number based on a seed */
@Suppress("RestrictedApiAndroidX")
public fun noiseFrom(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.NOISE_FROM))
}

/** parameters can be float or RFloat. Coded this way to not require 8 versions returns a*a+b*b */
@Suppress("RestrictedApiAndroidX")
public fun sqSum(a: RFloat, b: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, *b.array, AnimatedFloatExpression.SQUARE_SUM))
}

/** hypot = sqrt(a*a+b*b) */
@Suppress("RestrictedApiAndroidX")
public fun hypot(a: RFloat, b: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, *b.array, AnimatedFloatExpression.HYPOT))
}

/** INV operator 1/x */
@Suppress("RestrictedApiAndroidX")
public fun invert(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.INV))
}

/** INV operator 1/x */
@Suppress("RestrictedApiAndroidX")
public fun sq(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, AnimatedFloatExpression.SQUARE))
}
