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

import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcHaptic
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.RcTouchStopMode
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.ifElse
import androidx.compose.remote.creation.dsl.padding
import androidx.compose.remote.creation.dsl.rdp
import androidx.compose.remote.creation.dsl.rsp
import androidx.compose.remote.creation.dsl.times
import androidx.compose.remote.creation.dsl.touchPosX
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * A classic paddle-and-ball game experiment. Control the paddle at the bottom by dragging
 * left/right. The ball bounces continuously. If the ball reaches the bottom and the paddle is not
 * underneath, a miss/game-over state is triggered with audio/haptic feedback.
 */
@Suppress("RestrictedApiAndroidX")
fun dslGamePaddleBattle(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        //        val bounceSound =
        //            soundExpression(
        //                type = RcSoundType.Tone,
        //                frequency = 660f,
        //                durationSeconds = 0.05f,
        //                waveform = RcWaveform.Sine,
        //            )
        //
        //        val missSound =
        //            soundExpression(
        //                type = RcSoundType.Tone,
        //                frequency = 150f,
        //                durationSeconds = 0.25f,
        //                waveform = RcWaveform.Square,
        //            )

        val t = continuousSeconds()

        Box(modifier = Modifier.fillMaxSize()) {
            // Game rendering
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = componentWidth()
                val h = componentHeight()

                // 1. Paddle X controlled by touch
                val paddleWidth = 200.rf
                val paddleHeight = 20f.rf
                val paddleY = h - 120f.rf
                // 2. Spaceship X controlled by drag/touch
                val paddleX =
                    addTouch(
                        defValue = w / 2f,
                        min = paddleWidth,
                        max = w - paddleWidth,
                        stopMode = RcTouchStopMode.Gently,
                        velocity = 0.rf,
                        notchHaptic = RcHaptic.NoHaptics,
                        touchSpec = null,
                        easingSpec = null,
                        exp = touchPosX(),
                    )

                // 2. Ball position using ping-pong math

                ball(w, h, paddleX, paddleY, paddleWidth, paddleHeight)

                val halfPaddle = paddleWidth / 2f

                // Draw Background Grid for retro arcade feel
                paint {
                    color(0x15FFFFFL.toInt())
                    style(RcPaintStyle.Stroke)
                    strokeWidth(2f)
                }
                loop(0f.rf, 80f.rf, w) { gridX -> drawLine(gridX, 0f.rf, gridX, h) }
                loop(0f.rf, 80f.rf, h) { gridY -> drawLine(0f.rf, gridY, w, gridY) }

                // Draw Paddle
                paint {
                    color(0xFF00FF88.toInt()) // Neon green
                    style(RcPaintStyle.Fill)
                }
                drawRect(
                    paddleX - halfPaddle,
                    paddleY,
                    paddleX + halfPaddle,
                    paddleY + paddleHeight,
                )

                // Draw Ball
                paint {
                    color(0xFFFFCC00.toInt()) // Yellow
                    style(RcPaintStyle.Fill)
                }
            }

            // Score / Time display overlay
            Row(modifier = Modifier.padding(24.rdp)) {
                Text(text = remoteText("TIME: "), color = 0xFFFFFFFF.toInt(), fontSize = 18.rsp)
                Text(text = t.format(3, 0, 0), color = 0xFFFFCC00.toInt(), fontSize = 18.rsp)
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun RcScope.ball(
    w: RcFloat,
    h: RcFloat,
    paddleX: RcFloat = 0.rf,
    paddleY: RcFloat = 0.rf,
    paddleWidth: RcFloat = 200.rf,
    paddleHeight: RcFloat = 20f.rf,
): Array<RcFloat> {
    val variables = FloatArray(4)

    val rad = 28.rf
    val dt = deltaTime()
    var returnX: RcFloat = 0.rf
    var returnY: RcFloat = 0.rf
    val paddleHalfWidth = paddleWidth / 2f
    val py1 = (paddleY - paddleHeight).flush()
    val py2 = (paddleY + paddleHeight).flush()
    val px1 = (paddleX - paddleHalfWidth).flush()
    val px2 = (paddleX + paddleHalfWidth).flush()
    val accX: RcFloat = accelerometerX() * -100f
    val accY: RcFloat = accelerometerY() * 100f
    impulse(200000.rf, 0.rf) {
        val ps = createParticles(variables, arrayOf(w / 2f, h / 2f, 500f.rf, 500f.rf), 1)
        val (x, y, dx, dy) = RcFloats(variables)
        returnX = x
        returnY = y
        beginGlobal()
        val count = 0.rf.flush()
        endGlobal()
        impulseProcess() {
            // hit the walls
            particlesComparison(
                id = ps,
                flags = 0,
                min = 0.rf,
                max = 1.rf,
                condition = (y - rad) * (h - y) * -1f,
                then = arrayOf(x, y - dy * dt, dx, dy * -1f),
            ) {}
            particlesComparison(
                id = ps,
                flags = 0,
                min = 0.rf,
                max = 1.rf,
                condition = (x - rad) * (w - x) * -1f,
                then = arrayOf(x - dx * dt, y, dx * -1f, dy),
            ) {}

            // hit the paddle
            particlesComparison(
                id = ps,
                flags = 0,
                min = 0.rf,
                max = 1.rf,
                condition = -1f * ifElse((x - px1) * (px2 - x), (y - py1) * (py2 - y) * -1f, 1.rf),
                then = arrayOf(x, y - dy * dt * 2f, abs(dx) * sign(x - paddleX), dy * -1f),
            ) {
                paint {
                    color(0xFFFFFF00.toInt())
                    style(RcPaintStyle.Fill)
                }
                runAction { setValue(count, count + 1f) }
                debug("count ", count)

                drawCircle(x, y, rad)
                drawRect(px1, py1, px2, py2 + 50f.rf)
                //                playSound(missSound)
                //                performHaptic(RcHaptic.Reject)

            }

            particlesLoop(
                ps,
                null,
                arrayOf(x + dx * dt, y + dy * dt, dx * 0.999f + accX * dt, dy * 0.999f + accY * dt),
            ) {
                paint {
                    color(0xFF007AFF.toInt())
                    style(RcPaintStyle.Fill)
                }
                drawCircle(x, y, rad)
                paint {
                    color(0xFFFFFFFF.toInt())
                    style(RcPaintStyle.Fill)
                    textSize(80f)
                }
                drawCircle(x - 8f, y - 8f, 8f.rf)

                drawTextAnchored(createTextFromFloat(count, 3, 0, 0), 100.rf, 200.rf, 0.rf, 0.rf, 0)
            }
        }
    }
    return arrayOf(returnX, returnY)
}
