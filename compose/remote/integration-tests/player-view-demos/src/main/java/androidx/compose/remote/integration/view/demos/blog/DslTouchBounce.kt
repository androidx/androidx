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

import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.clamp
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.minus
import androidx.compose.remote.creation.dsl.onTouchDown
import androidx.compose.remote.creation.dsl.times
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Tap anywhere and a dot jumps, then falls back to the floor under gravity.
 *
 * The interesting part is that the host is never involved. `onTouchDown {}` does no work itself; it
 * just makes the surface touchable so the player updates `touchTime()`. Everything after that is an
 * expression graph baked into the document:
 * - `thrustFactor` compares `touchTime()` against `animationTime()` and decays from 1 to 0 over the
 *   150 ms following a tap, which is the impulse window.
 * - A **particle system of one** carries the simulation state. `createParticles` allocates two
 *   registers (position, velocity) seeded to "resting on the floor"; `particlesLoop` writes the
 *   next frame's values back into those same registers. That write-back is what makes the state
 *   persistent across frames without any host round-trip.
 * - `deltaTime()` is the real frame duration, so multiplying velocity by `dt` keeps the motion at a
 *   fixed real-world speed regardless of the display's refresh rate.
 *
 * `impulse(5f, touchTime())` bounds the cost: the simulation only runs for five seconds after the
 * most recent tap, then the document goes quiet.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslTouchBounce(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Touch Bounce"),
        experimental = true,
    ) {
        Box(modifier = Modifier.fillMaxSize().background(0xFFFFFFFF.toInt())) {
            Canvas(modifier = Modifier.fillMaxSize().onTouchDown {}) {
                val w = componentWidth()
                val h = componentHeight()
                val centerX = (w * 0.5f).flush()
                val floorY = (h * 0.5f).flush()
                val minDimension = min(w, h)
                val radius = (minDimension * 0.2f).flush()

                // Calculate thrust factor based on time elapsed since touch.
                // Decays from 1.0 (max thrust) to 0.0 over 150 ms after the touch event.
                val thrustFactor =
                    clamp(
                        min = 0f.rf,
                        max = 1f.rf,
                        value = (touchTime() - animationTime() + 0.15f) / 0.15f,
                    )

                // Animates the dot for no more than 5 seconds after a touch event.
                impulse(5f.rf, touchTime()) {
                    // Defines the dot's initial position and velocity at rest.
                    val stateRegisters = FloatArray(2)
                    val particleSystem =
                        createParticles(
                            stateRegisters,
                            arrayOf(
                                floorY, // Initial vertical position
                                0f.rf, // Initial vertical velocity
                            ),
                            1, // Number of green dots
                        )
                    val verticalPosition = RcFloat(stateRegisters[0])
                    val verticalVelocity = RcFloat(stateRegisters[1])
                    // Measure the time of each frame. Multiply velocity by dt instead of raw
                    // pixels to move the dot at real-world speed independent of frame rate.
                    val dt = deltaTime()

                    impulseProcess {
                        // Euler method to calculate future vertical positions and velocities.
                        val newVerticalPosition =
                            min(floorY, verticalPosition + (verticalVelocity * dt))
                        val gravity = 1200f // Downward acceleration in px/s²
                        val jumpVelocity = -500f // Upward velocity in px/s
                        val fallingVelocity = verticalVelocity + (gravity * dt)
                        // When tapped (thrustFactor = 1.0): zero out falling momentum and apply
                        // jumpVelocity. While falling (thrustFactor = 0.0): gravity takes over.
                        val newVerticalVelocity =
                            (fallingVelocity * (1f - thrustFactor)) + (jumpVelocity * thrustFactor)

                        particlesLoop(
                            particleSystem,
                            null,
                            // Writes back to the registers attached to the particleSystem so that
                            // on the next frame newVerticalPosition and newVerticalVelocity are
                            // calculated using the position and velocity from this frame.
                            arrayOf(newVerticalPosition, newVerticalVelocity),
                        ) {
                            save()
                            // Qualified because the particle loop body runs in a plain RcScope,
                            // which does not expose the canvas transform operations.
                            this@Canvas.translate(centerX, verticalPosition)
                            paint { color(0xFF34A853.toInt()) }
                            drawCircle(0f.rf, 0f.rf, radius)
                            restore()
                        }
                    }
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun DslTouchBouncePreview() {
    RemoteDocumentPreview(RemoteDocument(dslTouchBounce()))
}
