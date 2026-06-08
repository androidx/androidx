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

package androidx.compose.remote.integration.view.demos.dsl

import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcHeaderTag
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.floor
import androidx.compose.remote.creation.dsl.squareSum
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

private const val BG = 0xFF05070E.toInt()
private const val CX = 540f // canvas centre (rendered space)
private const val CY = 540f
private const val D = 150f // half the pole separation
private const val PN = CY - D // N pole y (top)
private const val PS = CY + D // S pole y (bottom)
private const val MW = 42f // magnet half-width
private const val SEED_R = 26f // spawn-ring radius around the N pole
private const val SPEED = 175f // tracer speed along a field line (px/s)
private const val LIFESPAN = 4.2f // seconds before a tracer recycles to N
private const val LINES = 24 // distinct field lines (spawn angles)
private const val PER_LINE = 14 // tracers per line, evenly staggered along the line
private const val N = LINES * PER_LINE
private const val DOT_R = 5f
private const val SOFT = 36f // pole softening (≈6px)²: keeps r³ > 0 at the poles
private const val MAG_EPS = 1e-12f // field-magnitude floor: keeps the B̂ normaliser finite
private const val POLE_N = 0xFFE0402E.toInt() // N pole: warm red
private const val POLE_S = 0xFF2E6EE0.toInt() // S pole: cool blue

/**
 * **Magnetic dipole field lines, drawn by a particle system.** Each particle is a tracer that flows
 * along the field of a bar magnet — poles at `(CX, PN)` and `(CX, PS)`. Its per-step update moves
 * it a fixed distance along **B**, so the swarm paints the classic field-line pattern: lines
 * leaving the red N pole, looping out, and returning to the blue S pole. Tracers spawn on a small
 * ring around N ([LINES] spokes × [PER_LINE] tracers, staggered by random age) and recycle when
 * their age expires.
 *
 * This is the **compactness** argument for particles: the document doesn't carry the field-line
 * geometry (thousands of points), it carries the *dipole formula* — six short RPN update
 * expressions — and the player generates the motion. The whole animated scene is ~1.1 KB (45 ops).
 *
 * Per-particle variables `[bx, by, x, y, age, hue]`. Field components are scratch variables
 * computed first (so the position update sees the fresh field); `x, y` are absolute pixel
 * coordinates (drawn directly), and each field expression stays under the 32-token particle cap via
 * `square` + `pow(1.5)` for the `1/r³` falloff. Colour is a per-tracer hue stored at spawn (one
 * colour per field line).
 */
@Suppress("RestrictedApiAndroidX")
public fun magneticField2D(): ByteArray =
    createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription("Magnetic Field Lines"),
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize().background(BG)) {
                val dt = deltaTime()
                val twoPi = (2.0 * Math.PI).toFloat()

                impulse(RcFloat(1_000_000f), RcFloat(0f)) {
                    fun angle(): RcFloat =
                        particleIndex() % LINES.toFloat() / LINES.toFloat() * twoPi

                    val vars = FloatArray(6) // bx, by, x, y, age, hue
                    val ps =
                        createParticles(
                            vars,
                            listOf(
                                RcFloat(0f), // bx (computed on first step)
                                RcFloat(0f), // by
                                angle().cos() * SEED_R + CX, // x: ring around N (absolute px)
                                angle().sin() * SEED_R + PN, // y: ring around N
                                // age: even stagger — tracers on one spoke (same hue) sit at
                                // evenly spaced points along the line, so it reads as a smooth arc.
                                floor(particleIndex() / LINES.toFloat()) / PER_LINE.toFloat() *
                                    LIFESPAN,
                                particleIndex() % LINES.toFloat() / LINES.toFloat(), // hue per line
                            ),
                            N,
                        )
                    val bx = RcFloat(vars[0])
                    val by = RcFloat(vars[1])
                    val x = RcFloat(vars[2])
                    val y = RcFloat(vars[3])
                    val age = RcFloat(vars[4])
                    val hue = RcFloat(vars[5])

                    // 1/r³ to each pole; B = (+charge at N) − (charge at S).
                    // SOFT keeps r³ non-zero at the poles, MAG_EPS keeps the field-magnitude
                    // normaliser non-zero at field nulls — without these a tracer reaching a pole
                    // would compute x/0 → NaN, which the RPN evaluator cannot store as a particle
                    // value (a NaN data value is indistinguishable from a NaN operator token).
                    // squareSum(a,b) = a²+b² in ONE op, which keeps each equation under the
                    // 32-token
                    // cap.
                    fun rn3(): RcFloat = (squareSum(x - CX, y - PN) + SOFT).pow(1.5f)
                    fun rs3(): RcFloat = (squareSum(x - CX, y - PS) + SOFT).pow(1.5f)
                    fun mag(): RcFloat = (squareSum(bx, by) + MAG_EPS).sqrt()

                    process {
                        // bar magnet: red N (top half) + blue S (bottom half)
                        paint { color(POLE_N) }
                        drawRect(CX - MW, PN, CX + MW, CY)
                        paint { color(POLE_S) }
                        drawRect(CX - MW, CY, CX + MW, PS)

                        particlesLoop(
                            ps,
                            restart = age * -1f, // recycle to N when age < 0
                            expressions =
                                listOf(
                                    (x - CX) / rn3() - (x - CX) / rs3(), // bx = Bx
                                    (y - PN) / rn3() - (y - PS) / rs3(), // by = By
                                    x + bx / mag() * SPEED * dt, // x += B̂x·speed·dt
                                    y + by / mag() * SPEED * dt, // y += B̂y·speed·dt
                                    age - dt, // age down
                                    hue, // hue unchanged
                                ),
                        ) {
                            val c = remoteColorExpression(255, hue, 0.9f, 1f)
                            paint { color(c) }
                            drawCircle(x, y, RcFloat(DOT_R))
                        }
                    }
                }
            }
        }
    }

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun MagneticField2DPreview() {
    RemoteDocumentPreview(RemoteDocument(magneticField2D()))
}
