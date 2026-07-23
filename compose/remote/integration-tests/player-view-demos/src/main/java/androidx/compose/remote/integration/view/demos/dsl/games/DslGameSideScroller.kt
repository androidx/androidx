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
import androidx.compose.remote.creation.dsl.RcColumnVerticalPositioning
import androidx.compose.remote.creation.dsl.RcConditionOp
import androidx.compose.remote.creation.dsl.RcHaptic
import androidx.compose.remote.creation.dsl.RcHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcTouchStopMode
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.padding
import androidx.compose.remote.creation.dsl.rsp
import androidx.compose.remote.creation.dsl.touchPosY
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * A side-scrolling dodging game experiment. Drag your finger up and down to move the spaceship.
 * Dodge the incoming hazards flying from right to left.
 */
@Suppress("RestrictedApiAndroidX")
fun dslGameSideScroller(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = false) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFF003300L.toInt())) {
                val w = componentWidth()
                val h = componentHeight()
                val t = continuousSeconds()
                val rocketSize = 23.rf
                // 1. Player Y controlled by dragging up/down
                val playerX = 120f.rf
                // 2. Spaceship X controlled by drag/touch
                val playerY =
                    addTouch(
                        defValue = h / 2f,
                        min = rocketSize,
                        max = h - rocketSize,
                        stopMode = RcTouchStopMode.Gently,
                        velocity = 0.rf,
                        notchHaptic = RcHaptic.NoHaptics,
                        touchSpec = null,
                        easingSpec = null,
                        exp = touchPosY(),
                    )
                // 2. Incoming Obstacles (moving right to left)
                // Obstacle 1: Medium speed, sways up/down
                val obs1X = w - ((t * 180f) % (w + 100f))
                val obs1Y = h * 0.25f + sin(t * 3f) * 80f

                // Obstacle 2: Fast
                val obs2X = w - ((t + 1.5f) * 260f) % (w + 100f)
                val obs2Y = h * 0.75f + cos(t * 2f) * 100f

                // Obstacle 3: Slow, large
                val obs3X = w - ((t + 3f) * 140f) % (w + 100f)
                val obs3Y = h * 0.5f + sin(t * 1.2f) * 150f

                // Obstacle 4: Very fast
                val obs4X = w - ((t + 4.5f) * 320f) % (w + 100f)
                val obs4Y = h * 0.4f + cos(t * 4f) * 50f

                // Draw background grid lines (scrolling effect)
                paint {
                    color(0x11FFFFFFL.toInt())
                    style(RcPaintStyle.Stroke)
                    strokeWidth(2f)
                }
                val scrollOffset = (t * 150f) % 80f
                loop(0f.rf, 80f.rf, w) { gridX ->
                    val x = gridX - scrollOffset
                    drawLine(x, 0f.rf, x, h)
                }

                // Draw Obstacles (glowing red plasma balls)
                paint {
                    color(0xFFFF3B30.toInt()) // Hazard red
                    style(RcPaintStyle.Fill)
                }
                drawCircle(obs1X, obs1Y, 24f.rf)
                drawCircle(obs2X, obs2Y, 20f.rf)
                drawCircle(obs3X, obs3Y, 36f.rf)
                drawCircle(obs4X, obs4Y, 16f.rf)

                // Draw Player Spaceship (pointing right)
                save {
                    translate(playerX, playerY)
                    paint {
                        color(0xFF5AC8FA.toInt()) // Light blue
                        style(RcPaintStyle.Fill)
                    }
                    val shipPath = remotePath(30f, 0f)
                    shipPath.lineTo(-20f, -20f)
                    shipPath.lineTo(-10f, 0f)
                    shipPath.lineTo(-20f, 20f)
                    shipPath.close()
                    drawPath(shipPath.getPath())

                    // Thruster flame
                    paint {
                        color(0xFFFFCC00.toInt()) // Yellow/Orange
                        style(RcPaintStyle.Fill)
                    }
                    val flameWidth = 15f.rf + sin(t * 30f) * 8f.rf
                    drawCircle(-20f.rf, 0f.rf, flameWidth / 2f)
                }

                // 3. Collision Detection
                // Obstacle 1
                val d1X = abs(playerX - obs1X)
                val d1Y = abs(playerY - obs1Y)
                conditionalOperations(RcConditionOp.Lt, d1X, 44f.rf) {
                    conditionalOperations(RcConditionOp.Lt, d1Y, 44f.rf) {
                        paint {
                            color(0x66FF3B30L.toInt())
                            style(RcPaintStyle.Fill)
                        }
                        drawRect(0f.rf, 0f.rf, w, h)
                        performHaptic(RcHaptic.Reject)
                    }
                }

                // Obstacle 2
                val d2X = abs(playerX - obs2X)
                val d2Y = abs(playerY - obs2Y)
                conditionalOperations(RcConditionOp.Lt, d2X, 40f.rf) {
                    conditionalOperations(RcConditionOp.Lt, d2Y, 40f.rf) {
                        paint {
                            color(0x66FF3B30L.toInt())
                            style(RcPaintStyle.Fill)
                        }
                        drawRect(0f.rf, 0f.rf, w, h)
                        performHaptic(RcHaptic.Reject)
                    }
                }

                // Obstacle 3
                val d3X = abs(playerX - obs3X)
                val d3Y = abs(playerY - obs3Y)
                conditionalOperations(RcConditionOp.Lt, d3X, 56f.rf) {
                    conditionalOperations(RcConditionOp.Lt, d3Y, 56f.rf) {
                        paint {
                            color(0x66FF3B30L.toInt())
                            style(RcPaintStyle.Fill)
                        }
                        drawRect(0f.rf, 0f.rf, w, h)
                        performHaptic(RcHaptic.Reject)
                    }
                }

                // Obstacle 4
                val d4X = abs(playerX - obs4X)
                val d4Y = abs(playerY - obs4Y)
                conditionalOperations(RcConditionOp.Lt, d4X, 36f.rf) {
                    conditionalOperations(RcConditionOp.Lt, d4Y, 36f.rf) {
                        paint {
                            color(0x66FF3B30L.toInt())
                            style(RcPaintStyle.Fill)
                        }
                        drawRect(0f.rf, 0f.rf, w, h)
                        performHaptic(RcHaptic.Reject)
                    }
                }
            }

            // Overlay Instructions
            Column(
                modifier = Modifier.fillMaxSize().padding(0f, 24f, 0f, 0f),
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcColumnVerticalPositioning.Top,
            ) {
                Text(
                    text = remoteText("Drag UP & DOWN to move"),
                    color = 0xBBFFFFFF.toInt(),
                    fontSize = 18.rsp,
                )
            }
        }
    }
}
