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
import androidx.compose.remote.creation.dsl.RcConditionOp
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcHaptic
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcSoundType
import androidx.compose.remote.creation.dsl.RcStrokeCap
import androidx.compose.remote.creation.dsl.RcWaveform
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.sqrt
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * A physics-based cannon shooting experiment. Tap anywhere on the screen to aim and fire. The
 * projectile's speed is proportional to the distance of the tap. Hit the target on the right side
 * of the screen.
 */
@Suppress("RestrictedApiAndroidX")
fun dslGameCannonShoot(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        val fireSound =
            soundExpression(
                type = RcSoundType.Tone,
                frequency = 180f,
                durationSeconds = 0.1f,
                waveform = RcWaveform.Sawtooth,
            )

        val hitSound =
            soundExpression(
                type = RcSoundType.Tone,
                frequency = 880f,
                durationSeconds = 0.15f,
                waveform = RcWaveform.Sine,
            )

        Canvas(modifier = Modifier.fillMaxSize().background(0xFF1E102F.toInt())) {
            val w = componentWidth()
            val h = componentHeight()
            val t = continuousSeconds()

            // Cannon base position
            val cannonX = 80f.rf
            val cannonY = h - 80f.rf

            // 1. Get touch coordinates to aim
            val tx = RcFloat(RemoteContext.FLOAT_TOUCH_POS_X)
            val ty = RcFloat(RemoteContext.FLOAT_TOUCH_POS_Y)

            // Calculate direction vector and distance
            val dx = (tx - cannonX).flush()
            val dy = (ty - cannonY).flush()
            val dist = max(1f.rf, sqrt(dx * dx + dy * dy)).flush()
            val nx = dx / dist
            val ny = dy / dist

            // 2. Projectile motion
            // Time elapsed since the last shot (last touch event)
            val s = (max(0f.rf, animationTime() - touchTime())).flush()

            // Initial velocity proportional to touch distance
            val velocity = (dist * 1.5f).flush()

            // Ball position: X = x0 + vx*t, Y = y0 + vy*t + 0.5*g*t^2
            val ballX = (cannonX + nx * velocity * s).flush()
            val ballY = (cannonY + ny * velocity * s + 0.5f.rf * 450f.rf * s * s).flush()

            // Target position (floating wheel on the right)
            val targetX = w - 120f.rf
            val targetY = (h / 2f + sin(t * 2f) * 150f).flush()
            val targetRadius = 40f.rf

            // Draw Target
            paint {
                color(0xFFFF3B30.toInt()) // Outer red ring
                style(RcPaintStyle.Fill)
            }
            drawCircle(targetX, targetY, targetRadius)
            paint {
                color(0xFFFFFFFF.toInt()) // Inner white ring
                style(RcPaintStyle.Fill)
            }
            drawCircle(targetX, targetY, 25f.rf)
            paint {
                color(0xFFFF3B30.toInt()) // Center bullseye
                style(RcPaintStyle.Fill)
            }
            drawCircle(targetX, targetY, 10f.rf)

            // Draw Cannon Barrel pointing towards touch
            paint {
                color(0xFF00FFCC.toInt()) // Neon teal
                style(RcPaintStyle.Stroke)
                strokeWidth(24f)
                strokeCap(RcStrokeCap.Round)
            }
            drawLine(cannonX, cannonY, cannonX + nx * 90f, cannonY + ny * 90f)

            // Draw Cannon Base wheel
            paint {
                color(0xFF3A3A45.toInt())
                style(RcPaintStyle.Fill)
            }
            drawCircle(cannonX, cannonY, 35f.rf)

            // 3. Draw Projectile & Collision Check (only draw if shot is active, e.g. s < 2.5s)
            conditionalOperations(RcConditionOp.Lt, s, 2.5f.rf) {
                // Draw Cannon Ball
                paint {
                    color(0xFFFFCC00.toInt()) // Glowing yellow ball
                    style(RcPaintStyle.Fill)
                }
                drawCircle(ballX, ballY, 14f.rf)

                // Check collision with target
                val distToTargetX = ballX - targetX
                val distToTargetY = ballY - targetY
                val distToTarget =
                    sqrt(distToTargetX * distToTargetX + distToTargetY * distToTargetY)

                conditionalOperations(RcConditionOp.Lt, distToTarget, targetRadius + 14f.rf) {
                    // Target Hit! Draw explosion, play sound, haptic
                    paint {
                        color(0xCCFFCC00.toInt())
                        style(RcPaintStyle.Fill)
                    }
                    drawCircle(targetX, targetY, 70f.rf)

                    playSound(hitSound)
                    performHaptic(RcHaptic.Confirm)
                }
            }

            // Play firing sound on initial touch-down (when s is close to 0)
            conditionalOperations(RcConditionOp.Lt, s, 0.05f.rf) { playSound(fireSound) }
        }
    }
}
