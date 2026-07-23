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

package androidx.compose.remote.integration.view.demos.dsl.games

import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcColumnVerticalPositioning
import androidx.compose.remote.creation.dsl.RcConditionOp
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.clamp
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.padding
import androidx.compose.remote.creation.dsl.rsp
import androidx.compose.remote.creation.dsl.sqrt
import androidx.compose.remote.creation.dsl.times
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * A tilt-controlled marble game experiment. Uses the device accelerometer (FLOAT_ACCELERATION_X/Y)
 * to position the marble. Tilt the device to keep the blue marble inside the moving green/red
 * target ring.
 */
@Suppress("RestrictedApiAndroidX")
fun dslGameMarbleTilt(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        //        val warningSound =
        //            soundExpression(
        //                type = RcSoundType.Tone,
        //                frequency = 220f,
        //                durationSeconds = 0.08f,
        //                waveform = RcWaveform.Square,
        //            )

        Box(modifier = Modifier.fillMaxSize()) {
            // Game rendering
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = componentWidth()
                val h = componentHeight()
                val t = continuousSeconds()
                val (mx, my) = marble(w, h)
                // 1. Get Accelerometer inputs (tilt) using the public constructor with NaN ID
                //                val ax = RcFloat(RemoteContext.FLOAT_ACCELERATION_X)
                //                val ay = RcFloat(RemoteContext.FLOAT_ACCELERATION_Y)

                // Map tilt to ball position (with clamping to keep it on screen)
                val cx = w / 2f
                val cy = h / 2f

                // 2. Moving target ring path (lissajous curve)
                val targetX = (cx + sin(t * 1.2f) * (w * 0.3f)).flush()
                val targetY = (cy + cos(t * 0.8f) * (h * 0.25f)).flush()
                val targetRadius = 90f.rf

                // 3. Distance check
                val dx = (mx - targetX).flush()
                val dy = (my - targetY).flush()
                val dist = sqrt(dx * dx + dy * dy).flush()

                paint {
                    color(0xFF4CAF50.toInt()) // Green
                    style(RcPaintStyle.Stroke)
                    strokeWidth(8f)
                }
                conditionalOperations(RcConditionOp.Gte, dist, targetRadius) {
                    paint {
                        color(0xFFFF0000.toInt()) // Green
                    }
                }

                drawCircle(targetX, targetY, targetRadius)
            }

            // Static Instructions Overlay at the bottom
            Column(
                modifier = Modifier.fillMaxSize().padding(0f, 0f, 0f, 48f),
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcColumnVerticalPositioning.Bottom,
            ) {
                Text(
                    text = remoteText("Tilt your device to roll the marble."),
                    color = 0xAAFFFFFF.toInt(),
                    fontSize = 48.rsp,
                )
                Text(
                    text = remoteText("Keep it inside the moving target ring!"),
                    color = 0xAAFFFFFF.toInt(),
                    fontSize = 48.rsp,
                )
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun RcScope.marble(w: RcFloat, h: RcFloat): Array<RcFloat> {
    val variables = FloatArray(4)
    val ax = -400f * RcFloat(RemoteContext.FLOAT_ACCELERATION_X)
    val ay = 400f * RcFloat(RemoteContext.FLOAT_ACCELERATION_Y)
    val rad = 28.rf
    val dt = deltaTime()
    var returnX: RcFloat = 0.rf
    var returnY: RcFloat = 0.rf

    impulse(20000.rf, 0.rf) {
        val ps = createParticles(variables, arrayOf(w / 2f, h / 2f, 0f.rf, 0f.rf), 1)
        val (x, y, dx, dy) = RcFloats(variables)
        returnX = x
        returnY = y
        impulseProcess() {
            particlesLoop(
                ps,
                null,
                arrayOf(
                    clamp(rad, w - rad, x + dx * dt),
                    clamp(rad, h - rad, y + dy * dt),
                    clamp(-300.rf, 300.rf, dx + ax * dt),
                    clamp(-300.rf, 300.rf, dy + ay * dt),
                ),
            ) {
                paint {
                    color(0xFF007AFF.toInt())
                    style(RcPaintStyle.Fill)
                }
                drawCircle(x, y, rad)
                paint {
                    color(0xFFFFFFFF.toInt())
                    style(RcPaintStyle.Fill)
                }
                drawCircle(x - 8f, y - 8f, 8f.rf)
            }
        }
    }
    return arrayOf(returnX, returnY)
}
