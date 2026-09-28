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

package androidx.compose.remote.integration.view.demos.blog

import androidx.compose.remote.creation.compose.action.Action.Companion.Empty
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.onTouchDown
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.dsl.RcCanvas
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.random
import androidx.compose.remote.creation.dsl.times
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Suppress("RestrictedApiAndroidX")
@RemoteComposable
@Composable
public fun CollisionDetect() {
    RemoteBox(
        modifier =
            RemoteModifier.fillMaxSize().background(RemoteColor(Color.White)).onTouchDown(Empty),
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
            RcCanvas(remoteComposeCreationState.document) {
                val width = componentWidth()
                val height = componentHeight()
                val floorY = (height * 0.9f).flush()

                // Horizontal line
                paint { color(0xFF000000.toInt()) }
                drawRoundRect(
                    20f.rf, // left
                    floorY - 8f, // top
                    width - 20f, // right
                    floorY + 8f, // bottom
                    8f.rf, // radiusX for rounded corners
                    8f.rf, // radiusY for rounded corners
                )

                // Tap triggered shower
                impulse(5f.rf, touchTime()) {
                    val stateRegisters = FloatArray(4)

                    val rainSystem =
                        createParticles(
                            stateRegisters,
                            arrayOf(
                                random(30f.rf, width - 30f), // Distributed across width
                                random((-160f).rf, 0f.rf), // Staggered starting Y
                                0f.rf, // Initial horizontal velocity
                                random(260f.rf, 420f.rf), // Falling speed px/s
                            ),
                            10, // particle count
                        )
                    val x = RcFloat(stateRegisters[0])
                    val y = RcFloat(stateRegisters[1])
                    val vx = RcFloat(stateRegisters[2])
                    val vy = RcFloat(stateRegisters[3])
                    val dt = deltaTime()

                    impulseProcess {
                        val nextVy = vy + (500f * dt)
                        val nextX = x + (vx * dt)
                        val nextY = y + (nextVy * dt)
                        val hitFloor = nextY - floorY
                        val bounceVy = -0.55f * vy
                        val radius = 20f
                        wakeIn(0.01f)
                        // particlesComparison is helpful when you need to:
                        // 1. Render effects on particles given a condition (like showing a splash
                        //    after colliding with an object).
                        // 2. Mutate state of particles given a condition (like bouncing up).

                        // Evaluates 'hitFloor' across all particles.
                        // If hitFloor > 0 the particle has hit the floor:
                        //   1. Replaces the particle's state registers with the provided array
                        //      values.
                        //   2. Executes the scoped draw lambda ONLY for the colliding particles.
                        particlesComparison(
                            id = rainSystem,
                            flags = 0,
                            // min/max of -1 means "no index range filter": test every particle.
                            min = (-1f).rf,
                            max = (-1f).rf,
                            condition = hitFloor, // Evaluated as true if > 0.
                            then =
                                arrayOf(
                                    nextX, // Register 0 (x): Maintain horizontal position
                                    // Register 1 (y): Snap position to the floor surface
                                    // (prevents tunneling)
                                    floorY,
                                    vx, // Register 2 (vx): Preserve horizontal velocity
                                    // Register 3 (vy): Invert & dampen vertical velocity
                                    // (-0.55 * vy) to bounce up
                                    bounceVy,
                                ),
                        ) {
                            save()
                            // Origin placed at the particle's floor contact point. Qualified
                            // because this block runs in a plain RcScope, which has no canvas
                            // transform operations.
                            this@RcCanvas.translate(x, floorY)
                            // Render an accent splash circle at the point of collision
                            paint { color(0xFF7FFF00.toInt()) }
                            drawCircle(0f, 0f, radius + (radius / 4))
                            restore() // Reset canvas transform for subsequent drawing commands
                        }

                        // Green falling particles
                        particlesLoop(rainSystem, null, arrayOf(nextX, nextY, vx, nextVy)) {
                            save()
                            this@RcCanvas.translate(x, y)
                            paint { color(0xFF34A853.toInt()) }
                            drawCircle(0f, 0f, radius)
                            restore()
                        }
                    }
                }
            }
        }
    }
}
