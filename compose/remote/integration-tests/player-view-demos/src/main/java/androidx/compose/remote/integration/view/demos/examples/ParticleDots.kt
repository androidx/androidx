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

import android.graphics.Shader
import androidx.compose.remote.creation.*
import androidx.compose.remote.creation.compose.state.CUBIC_LINEAR
import androidx.compose.remote.creation.exp
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.atan2
import kotlin.math.hypot

/** Creates a spinning test */
@Suppress("RestrictedApiAndroidX")
fun particleDots2(): RemoteComposeContext {
    return RemoteComposeContextAndroid(800, 800, "spinning", 6, 0, AndroidxRcPlatformServices()) {
        val width = rf(Rc.System.WINDOW_WIDTH)
        val height = rf(Rc.System.WINDOW_HEIGHT)
        val centerX = width / 2f
        val centerY = height / 2f
        drawCircle(centerX.toFloat(), centerY.toFloat(), min(centerY, centerY).toFloat())
        painter
            .setLinearGradient(
                0f,
                0f,
                0f,
                height.toFloat(),
                intArrayOf(Color(0xFF334477).toArgb(), Color(0xFF111122).toArgb()),
                null,
                Shader.TileMode.CLAMP,
            )
            .setColor(Color(0xFF1F2331).toArgb())
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
        droppingEngine3(
            writer as RemoteComposeWriterAndroid,
            0f,
            width,
            height,
            centerX,
            centerY,
            rad,
        )
        val rpn =
            floatExpression(
                exp(Rc.Time.ANIMATION_DELTA_TIME, 1000f, Rc.FloatExpression.MUL),
                anim(2f, CUBIC_LINEAR, null, 16f),
            )
        var foo =
            floatExpression(
                Rc.Time.ANIMATION_DELTA_TIME,
                1000f,
                Rc.FloatExpression.MUL,
                16f,
                Rc.FloatExpression.DIV,
            )
        var id = createTextFromFloat(foo, 2, 1, 0)
        // var id = textCreateId("9 30 45")
        painter.setColor(Color(0xFFABAA9C).toArgb()).setTextSize(123f).commit()
        drawTextAnchored(id, centerX.toFloat(), centerY.toFloat(), 0f, 0f, 2)
    }
}

/** Creates a spinning test */
@Suppress("RestrictedApiAndroidX")
fun particleDots(): RemoteComposeContext {
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
                            intArrayOf(Color(0xFF334477).toArgb(), Color(0xFF111122).toArgb()),
                            null,
                            Shader.TileMode.CLAMP,
                        )
                        .setColor(Color(0xFF1F2331).toArgb())
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
                    droppingEngine3(
                        writer as RemoteComposeWriterAndroid,
                        0f,
                        width,
                        height,
                        centerX,
                        centerY,
                        rad,
                    )
                    //                    var id = textCreateId("9 30 45")
                    //
                    // painter.setColor(Color(0xFFABAA9C).toArgb()).setTextSize(123f).commit()
                    //                    drawTextAnchored(id, centerX.toFloat(), centerY.toFloat(),
                    // 0f, 0f, 2)
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun ants(
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
        val pCount = 400
        val skip = 10f
        painter.setColor(Color(0xFF00FFFF).toArgb()).setAlpha(1f).setShader(0).commit()
        val angle = sin(ContinuousSec()) / 10f
        impulse(200f, event) {
            val variables: Array<RFloat> = Array<RFloat>(5, { RFloat(this, 0f) })
            val pi2 = (2 * Math.PI).toFloat()
            val ps =
                createParticles(
                    variables,
                    arrayOf(
                        ((index() * (pi2 / pCount.toFloat()))),
                        (((index() * 0.07f) * pi2)),
                        rand(),
                        rand(),
                        0,
                    ),
                    pCount,
                )
            val (lat, lon, dLat, dLon, pos) = variables
            val dt = deltaTime()
            impulseProcess() { // current_time - envnt_time  < 0.5
                particlesLoops(
                    ps,
                    null,
                    arrayOf((lat + dt * dLat) % pi2, (lon + dt * dLon) % pi2, dLat, dLon, pos),
                ) {
                    save()
                    val y = cos(lat) * rad
                    val scaleX = sqrt(rad * rad - y * y)
                    scaleX.toFloat()
                    val dScaleX = y / scaleX
                    val x = cos(lon + angle) * scaleX
                    x.toFloat()
                    y.toFloat()
                    val scale = (rad * rad - sqSum(x, y)) / (rad * rad)
                    // painter.setAlpha(1f).commit()
                    // rotate((atan2(sin(dLon)*scaleX - dScaleX*cos(dLon),
                    // sin(dLat)*rad)).toFloat(),0f,0f)
                    translate(
                        (x * (pos + 1f) + centerX - (size / 2f)).toFloat(),
                        (y * (pos + 1f) + centerY - (size / 2f)).toFloat(),
                    )
                    scale(sin(lon + angle).toFloat(), sin(lat).toFloat())
                    rotate((atan2(dLat, dLon) * (360 / pi2)).toFloat(), 0f, 0f)
                    conditionalOperations(
                        Rc.Condition.GT,
                        (lon + angle).toFloat(),
                        Math.PI.toFloat(),
                    )
                    drawRoundRect(0f, 0f, 30f, 10f, 4f, 4f)
                    endConditionalOperations()
                    restore()
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun droppingEngine3(
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
        val ring = 200f
        val rx = sin(ContinuousSec()) * 100f
        val ry = cos(ContinuousSec() * 2f) * 100f
        val pCount = 423
        val skip = 23f
        painter.setColor(Color(0xFF00FF37).toArgb()).setAlpha(1f).setShader(0).commit()
        val angle = sin(ContinuousSec()) / 10f
        //  impulse(2000f, event) {
        val variables: Array<RFloat> = Array<RFloat>(5, { RFloat(this, 0f) })
        val pi2 = (2 * Math.PI).toFloat()
        val pi = (Math.PI).toFloat()
        impulse(200f, event) {
            val ps =
                createParticles(
                    variables,
                    arrayOf(
                        ((floor(index() / skip) * (skip / pCount)) * (pi)),
                        ((index() % skip) / skip) * pi2,
                        sin(index()),
                        cos(index()),
                        0,
                    ),
                    pCount,
                )

            //        val ps =
            //            createParticles(
            //                variables,
            //                arrayOf(
            //                    (( floor(index() / skip) /skip) * (pi)),
            //                    ((((index() % skip) / skip+ (floor(index() / skip) % 2f) * 0.5f) *
            // pi + pi) ),
            //                    rand(),
            //                    rand(),
            //                    0),
            //                pCount
            //            )
            val (lat, lon, dLat, dLon, pos) = variables
            val dt = deltaTime()
            impulseProcess() { // current_time - envnt_time  < 0.5
                particlesLoops(ps, null, arrayOf(lat, (lon + deltaTime()) % pi2, dLat, dLon, pos)) {
                    save()
                    val y = cos(lat) * rad
                    val scaleX = sqrt(rad * rad - y * y)
                    scaleX.toFloat()
                    val x = cos(lon) * scaleX
                    x.toFloat()
                    y.toFloat()
                    val scaleEdge = (invert(abs(hypot(x - rx, y - ry) - ring) / 100f + 1f) + 1f)
                    scaleEdge.toFloat()
                    val scale = (rad * rad - sqSum(x, y)) / (rad * rad)
                    painter.setAlpha((scaleEdge - 1f).toFloat()).commit()
                    // rotate((atan2(sin(dLon)*scaleX - dScaleX*cos(dLon),
                    // sin(dLat)*rad)).toFloat(),0f,0f)
                    translate(
                        (x * (pos + 1f) + centerX - (size / 2f)).toFloat(),
                        (y * (pos + 1f) + centerY - (size / 2f)).toFloat(),
                    )
                    scale(
                        (sq(sin(lon + angle)) * scaleEdge).toFloat(),
                        (sq(sin(lat)) * scaleEdge).toFloat(),
                    )
                    rotate((atan2(dLat, dLon) * (360 / pi2)).toFloat(), 0f, 0f)
                    conditionalOperations(
                        Rc.Condition.GT,
                        (lon + angle).toFloat(),
                        Math.PI.toFloat(),
                    )
                    drawRoundRect(0f, 0f, 20f, 20f, 20f, 20f)
                    endConditionalOperations()
                    restore()
                }
            }
        }
    }
}
