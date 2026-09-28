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
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.ifElse
import androidx.compose.remote.creation.dsl.onTouchDown
import androidx.compose.remote.creation.dsl.random
import androidx.compose.remote.creation.dsl.times
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * The 8 state registers carried by each particle.
 *
 * `createParticles` hands back a flat [FloatArray] of register ids; this wrapper gives them names
 * so the simulation below reads like physics instead of array indexing. Each field is an [RcFloat]
 * *reference* to a register, not a value — reading it emits "read register n" into the expression
 * graph, and the write-back array passed to `particlesLoop` is what assigns to them.
 */
@Suppress("RestrictedApiAndroidX")
private data class ParticleRegisters2(
    val offsetX: RcFloat,
    val offsetY: RcFloat,
    val polarAngle: RcFloat,
    val polarRadius: RcFloat,
    val velocityX: RcFloat,
    val velocityY: RcFloat,
    val spinAngle: RcFloat,
    val spinSpeed: RcFloat,
) {
    constructor(
        array: FloatArray
    ) : this(
        RcFloat(array[0]),
        RcFloat(array[1]),
        RcFloat(array[2]),
        RcFloat(array[3]),
        RcFloat(array[4]),
        RcFloat(array[5]),
        RcFloat(array[6]),
        RcFloat(array[7]),
    )
}

/**
 * A green disc that shatters into 200 tumbling tiles when you tap it.
 *
 * This is the same machinery as the bouncing-dot demo scaled up: one particle system, but with 200
 * particles and 8 registers each. The document is still authored once. The player runs the Euler
 * integration for all 1600 registers per frame with no host involvement.
 *
 * Two details worth calling out:
 * - **The randomness is baked in, not streamed.** `random(min, max)` is evaluated once per particle
 *   when the system is created, so every tile gets its own launch direction, speed and spin without
 *   the creator enumerating 200 sets of constants.
 * - **`isExploding` gates the physics, not the drawing.** Before the first tap it is 0, which
 *   multiplies every delta to zero, so the tiles sit frozen in their initial polar positions
 *   underneath the solid disc. The tap flips it to 1 and simultaneously collapses the disc's radius
 *   to 0, revealing the tiles exactly as they start to move.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslParticlBreakup(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Particle Breakup"),
        experimental = true,
    ) {
        Box(modifier = Modifier.fillMaxSize().background(0xFFFFFFFF.toInt())) {
            Canvas(modifier = Modifier.fillMaxSize().onTouchDown {}) {
                val w = componentWidth()
                val h = componentHeight()
                val centerX = (w * 0.5f).flush()
                val centerY = (h * 0.5f).flush()
                val minDimension = min(w, h)
                val radius = (minDimension * 0.2f).flush()
                val tapTime = touchTime()

                // Ensures the square green tiles are roughly distributed in the shape of a
                // circle and are within the green circle area that draws underneath.
                val spread = (radius * 0.85f).flush()

                // Green circle
                paint { color(0xFF34A853.toInt()) }
                drawCircle(
                    centerX,
                    centerY,
                    // Hides the background circle on tap by collapsing its radius to 0f
                    ifElse(tapTime, 0f.rf, radius),
                )

                impulse(1000f.rf, 0f.rf) {
                    val rawRegisters = FloatArray(8)
                    val shatterSystem =
                        createParticles(
                            rawRegisters,
                            arrayOf(
                                0f.rf, // Initial explosion offset X
                                0f.rf, // Initial explosion offset Y
                                // Direction from center via the polar angle [0..2π]
                                random(0f.rf, 6.283185f.rf),
                                // Distance from center via the polar distance
                                random(0f.rf, spread),
                                random((-400f).rf, 400f.rf), // Horizontal velocity px/s
                                random((-500f).rf, 200f.rf), // Vertical velocity px/s
                                random(0f.rf, 360f.rf), // Initial rotation angle
                                random((-720f).rf, 720f.rf), // Spin velocity [deg/s]
                            ),
                            200, // Particle count
                        )
                    val particle = ParticleRegisters2(rawRegisters)
                    val dt = deltaTime()

                    impulseProcess {
                        val isExploding = ifElse(tapTime, 1f.rf, 0f.rf)
                        val gravity = 900f // px/s²

                        // Convert polar coordinates (r, θ) to initial Cartesian position (x, y)
                        val circleX = particle.polarRadius * cos(particle.polarAngle)
                        val circleY = particle.polarRadius * sin(particle.polarAngle)

                        // Euler method to calculate next frame's velocities, positions, and spin
                        // angles
                        val nextVelocityY = particle.velocityY + (gravity * dt * isExploding)
                        val nextOffsetX = particle.offsetX + (particle.velocityX * dt * isExploding)
                        val nextOffsetY = particle.offsetY + (nextVelocityY * dt * isExploding)
                        val nextSpin = particle.spinAngle + (particle.spinSpeed * dt * isExploding)

                        // Writes updated offsets, velocities, and angles back into particle
                        // registers for the next frame, then translates, rotates, and draws each
                        // square on the canvas
                        particlesLoop(
                            shatterSystem,
                            null,
                            arrayOf(
                                nextOffsetX,
                                nextOffsetY,
                                particle.polarAngle,
                                particle.polarRadius,
                                particle.velocityX,
                                nextVelocityY,
                                nextSpin,
                                particle.spinSpeed,
                            ),
                        ) {
                            save()
                            // Move origin to particle location so rotate() pivots around the
                            // particle's own center. Qualified because the particle loop body runs
                            // in a plain RcScope, which has no canvas transform operations.
                            this@Canvas.translate(
                                centerX + circleX + particle.offsetX,
                                centerY + circleY + particle.offsetY,
                            )
                            this@Canvas.rotate(particle.spinAngle)
                            paint { color(0xFF34A853.toInt()) }
                            // Draw a 40x40 px square centered at (0, 0) relative to the translated
                            // origin
                            drawRect(-20f, -20f, 20f, 20f)
                            restore() // Reset canvas matrix for the next particle
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
fun DslParticlBreakupPreview() {
    RemoteDocumentPreview(RemoteDocument(dslParticlBreakup()))
}
