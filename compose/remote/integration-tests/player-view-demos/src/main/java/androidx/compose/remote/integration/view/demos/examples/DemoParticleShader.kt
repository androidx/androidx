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

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.Utils.idFromNan
import androidx.compose.remote.creation.RFloat
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.component6
import androidx.compose.remote.creation.cos
import androidx.compose.remote.creation.createParticles
import androidx.compose.remote.creation.deltaTime
import androidx.compose.remote.creation.ifElse
import androidx.compose.remote.creation.index
import androidx.compose.remote.creation.particlesLoops
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.round
import androidx.compose.remote.creation.sin
import androidx.compose.remote.tooling.preview.RemoteDocPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview

@SuppressLint("RestrictedApiAndroidX")
fun RemoteComposeWriterAndroid.drawWithShader(
    bitmapId: Int,
    tDim: Int = 512,
    count: Int,
    positionX: Float,
    positionY: Float,
    bitmap: Bitmap? = null,
) {

    val FIREWORKS =
        """
        uniform shader originalImage;
        uniform float2 iResolution;
        uniform float iTime;
        uniform float radius;
        uniform float threshold;
        uniform float smoothing;
        uniform float positionsX[$count];
        uniform float positionsY[$count];
        uniform float background[4];
        uniform float color[4];
        vec4 main(vec2 pos) { 
            float totalPotential = 0.0;
            float radiusSq = radius * radius;
            for (int i = 0; i < $count; i ++) {
                float2 center = float2(positionsX[i], positionsY[i]);
                float distSq = distance(pos, center);
                distSq = distSq * distSq;
                if (distSq < 0.0001) { 
                    distSq = 0.0001; 
                }
                totalPotential += radiusSq / distSq; 
            }
            float coverage = smoothstep(threshold + smoothing, threshold - smoothing, totalPotential);
            vec4 backgroundColor = vec4(background[0], background[1], background[2], background[3]);
            vec4 particleColor = vec4(color[0], color[1], color[2], color[3]);
//            return vec4(mix(backgroundColor, particleColor, 1.0 - coverage).rgb, 0.5);
            return mix(backgroundColor, particleColor, 1.0 - coverage);
        }
        """

    val shader = FIREWORKS
    //    var bid = bitmapId
    //    if (bitmap != null) {
    //        bid = addBitmap(bitmap)
    //    }
    val tw: Float = tDim.toFloat()
    val th: Float = tDim.toFloat()
    val shaderId =
        createShader(shader)
            .setFloatUniform("iTime", RemoteContext.FLOAT_CONTINUOUS_SEC)
            .setFloatUniform("iResolution", tw, th)
            //            .setBitmapUniform("originalImage", bid)
            .setFloatUniform("radius", 12f)
            .setFloatUniform("threshold", 8f)
            .setFloatUniform("smoothing", 20f)
            .setFloatUniform("positionsX", positionX)
            .setFloatUniform("positionsY", positionY)
            .setFloatUniform("background", 0f, 0f, 0.2f, 1f)
            .setFloatUniform("color", 1f, 1f, 1f, 1f)
            .commit()
    painter.setShader(shaderId).commit()

    drawRect(0f, 0f, tw, th)
}

@SuppressLint("RestrictedApiAndroidX")
fun shaderFireworks(): RemoteComposeContext {

    return RemoteComposeContextAndroid(
        800,
        800,
        "Clock",
        7,
        RcProfiles.PROFILE_ANDROIDX,
        AndroidxRcPlatformServices(),
    ) {
        root {
            box(Modifier.fillMaxSize()) {
                canvas(Modifier.fillMaxSize()) {
                    val width = ComponentWidth()
                    val height = ComponentHeight()
                    painter.setColor(Color(0xFF675236).toArgb()).commit()
                    fireworksEngine(
                        writer as RemoteComposeWriterAndroid,
                        ContinuousSec().toFloat(),
                        width,
                        height,
                    )
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
            .setStyle(Paint.Style.FILL)
            // .setStrokeWidth(3f)
            .commit()
        val count = 200
        val groups = 10f
        val step = count / groups

        val texture = 1024
        //        val bitmapId = rcDoc.createBitmap(texture, texture)
        //        rcDoc.save()
        //        drawOnBitmap(bitmapId, 0, Color(0xff4400FF).toArgb())

        val positionArrayX = this.addDynamicFloatArray(count.toFloat())
        val positionArrayY = this.addDynamicFloatArray(count.toFloat())
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
            val thresh = 25f
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
                    drawCircle(x.toFloat(), y.toFloat(), 6f)
                    this.setArrayValue(idFromNan(positionArrayX), index.toFloat(), x.toFloat())
                    this.setArrayValue(idFromNan(positionArrayY), index.toFloat(), y.toFloat())
                }
            }
        }
        //        drawOnBitmap(0, 0, Color(0xff4400FF).toArgb())
        //        drawScaledBitmap(bitmapId, 0f, 0f, texture.toFloat(), texture.toFloat(), 0f, 0f,
        // width.toFloat(), height.toFloat(),0,1f,"")
        drawWithShader(0, texture, 200, positionX = positionArrayX, positionY = positionArrayY)
        painter.setShader(0).commit()

        //        rcDoc.restore()
    }
}

@Preview @Composable fun ShaderFireworksPreview() = RemoteDocPreview(shaderFireworks())
