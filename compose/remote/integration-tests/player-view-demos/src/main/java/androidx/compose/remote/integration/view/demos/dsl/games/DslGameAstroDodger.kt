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
import androidx.compose.remote.creation.dsl.RcConditionOp
import androidx.compose.remote.creation.dsl.RcHaptic
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcSoundType
import androidx.compose.remote.creation.dsl.RcTouchStopMode
import androidx.compose.remote.creation.dsl.RcWaveform
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.noiseFrom
import androidx.compose.remote.creation.dsl.touchPosX
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * A space dodging game experiment using RemoteCompose DSL. Drag anywhere on the screen (X-axis) to
 * move the ship at the bottom. Dodge the falling, swaying asteroids. Collision triggers haptic
 * feedback and an explosion sound.
 */
@Suppress("RestrictedApiAndroidX")
fun dslGameAstroDodger(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        val explosionSound =
            soundExpression(
                type = RcSoundType.Tone,
                frequency = 120f,
                durationSeconds = 0.3f,
                waveform = RcWaveform.Sawtooth,
            )

        Canvas(modifier = Modifier.fillMaxSize().background(0xFF0B0B1E.toInt())) {
            val w = componentWidth()
            val h = componentHeight()
            val t = continuousSeconds()
            val rocketSize = 50.rf
            // 1. Draw Starfield background (moving stars)
            paint {
                color(0x88FFFFFF.toInt())
                style(RcPaintStyle.Fill)
            }
            loop(0f.rf, 1f.rf, 10f.rf) { i ->
                val starX = noiseFrom(i * 13f) * w
                val starY = (noiseFrom(i * 37f) * h + t * 80f) % h
                drawCircle(starX, starY, 3f.rf)
            }

            // 2. Spaceship X controlled by drag/touch
            val goalX =
                addTouch(
                    defValue = w / 2f,
                    min = rocketSize,
                    max = w - rocketSize,
                    stopMode = RcTouchStopMode.Gently,
                    velocity = 0.rf,
                    notchHaptic = RcHaptic.NoHaptics,
                    touchSpec = null,
                    easingSpec = null,
                    exp = touchPosX(),
                )
            val shipX = (goalX + 0.0f).spring(1f, 1f, 0.01f, 0)
            val shipY = h - 120f.rf

            // 3. Falling Asteroids
            // Asteroid 1: Fast, sways left-right
            val ast1Y = (t * 220f) % h
            val ast1X = w * 0.3f + sin(t * 2.5f) * 80f

            // Asteroid 2: Slow, large
            val ast2Y = ((t + 2f) * 140f) % h
            val ast2X = w * 0.7f + cos(t * 1.2f) * 100f

            // Asteroid 3: Medium speed, straight down
            val ast3Y = ((t + 4f) * 180f) % h
            val ast3X = w * 0.5f

            // Draw Asteroids
            paint {
                color(0xFF8E8E93.toInt())
                style(RcPaintStyle.Fill)
            }
            drawCircle(ast1X, ast1Y, 32f.rf)
            drawCircle(ast2X, ast2Y, 48f.rf)
            drawCircle(ast3X, ast3Y, 24f.rf)

            // Draw Spaceship using translation for dynamic positioning
            save {
                translate(shipX, shipY)
                paint {
                    color(0xFF00D2FF.toInt()) // Neon Cyan
                    style(RcPaintStyle.Fill)
                }
                val shipPath = remotePath(0f, -30f)
                shipPath.lineTo(-25f, 25f)
                shipPath.lineTo(25f, 25f)
                shipPath.close()
                drawPath(shipPath.getPath())

                // Engine flame
                paint {
                    color(0xFFFF3B30.toInt()) // Fire Red
                    style(RcPaintStyle.Fill)
                }
                val flameHeight = 15f.rf + sin(t * 20f) * 10f.rf
                drawCircle(0f.rf, 30f.rf, flameHeight)
            }

            // 4. Collision Detection
            // If ship is close to Asteroid 1
            val dist1X = abs(shipX - ast1X)
            val dist1Y = abs(shipY - ast1Y)
            conditionalOperations(RcConditionOp.Lt, dist1X, 50f.rf) {
                conditionalOperations(RcConditionOp.Lt, dist1Y, 50f.rf) {
                    // Red screen flash
                    paint {
                        color(0xAAFF3B30.toInt())
                        style(RcPaintStyle.Fill)
                    }
                    drawRect(0f.rf, 0f.rf, w, h)
                    //  playSound(explosionSound)
                    performHaptic(RcHaptic.Reject)
                }
            }

            // If ship is close to Asteroid 2
            val dist2X = abs(shipX - ast2X)
            val dist2Y = abs(shipY - ast2Y)
            conditionalOperations(RcConditionOp.Lt, dist2X, 60f.rf) {
                conditionalOperations(RcConditionOp.Lt, dist2Y, 60f.rf) {
                    // Red screen flash
                    paint {
                        color(0xAAFF3B30.toInt())
                        style(RcPaintStyle.Fill)
                    }
                    drawRect(0f.rf, 0f.rf, w, h)
                    /// playSound(explosionSound)
                    performHaptic(RcHaptic.Reject)
                }
            }

            // If ship is close to Asteroid 3
            val dist3X = abs(shipX - ast3X)
            val dist3Y = abs(shipY - ast3Y)
            conditionalOperations(RcConditionOp.Lt, dist3X, 40f.rf) {
                conditionalOperations(RcConditionOp.Lt, dist3Y, 40f.rf) {
                    // Red screen flash
                    paint {
                        color(0xAAFF3B30.toInt())
                        style(RcPaintStyle.Fill)
                    }
                    drawRect(0f.rf, 0f.rf, w, h)
                    playSound(explosionSound)
                    performHaptic(RcHaptic.Reject)
                }
            }
        }
    }
}
