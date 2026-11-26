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

@file:OptIn(ExperimentalRemoteCreationApi::class)

package androidx.compose.remote.integration.view.demos.dsl.d3

import androidx.compose.remote.creation.ExperimentalRemoteCreationApi
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcHeaderTag
import androidx.compose.remote.creation.dsl.RcMesh
import androidx.compose.remote.creation.dsl.RcMeshMode
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.primitiveSphere
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

// ============================================================================================
// Particle-system demos on the typed DSL. A particle system is built as:
//
//   impulse(duration, start) {                 // gate: init once, then process each frame
//       val vars = FloatArray(N)               // per-particle variables (filled with their ids)
//       val ps = createParticles(vars, initExpressions, count)   // runs ONCE
//       process {                              // runs every frame
//           particlesLoop(ps, restart, updateExpressions) {      // body runs per particle
//               // draw using the particle variables (loaded into context each iteration)
//           }
//       }
//   }
//
// `rand()` and `particleIndex()` seed the per-particle state; `deltaTime()` integrates motion.
// The body can draw 2D (drawCircle) OR open a `scene3D { }` and drawMesh — that 3D-inside-a-loop
// path is what these demos add to the DSL.
// ============================================================================================

@Suppress("RestrictedApiAndroidX")
private fun docTags(desc: String) =
    arrayOf(
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription(desc),
    )

/**
 * **2D particle fountain.** ~140 colored dots erupt from the bottom centre, arc under gravity, and
 * re-emit when they fall back off-screen. Each particle carries `x, y, vx, vy, life, hue`; the loop
 * integrates position/velocity with `deltaTime()`, and `paint { color(remoteColorExpression(hue…))
 * }` tints every particle by its own hue variable (resolved per-iteration).
 */
@Suppress("RestrictedApiAndroidX")
public fun particleFountain2D(): ByteArray =
    createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        *docTags("2D Particle Fountain"),
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize().background(0xFF05060B.toInt())) {
                val w = componentWidth()
                val h = componentHeight()
                val n = 140
                val dt = deltaTime()
                impulse(RcFloat(1_000_000f), RcFloat(0f)) {
                    val vars = FloatArray(6)
                    val ps =
                        createParticles(
                            vars,
                            listOf(
                                w * 0.5f + (rand() - 0.5f) * 30f, // x near centre
                                h - 12f, // y at the bottom
                                (rand() - 0.5f) * 540f, // vx
                                rand() * -1500f - 350f, // vy (upward; screen y grows downward)
                                rand() * 0.7f + 0.5f, // life
                                particleIndex() / n.toFloat(), // hue 0..1
                            ),
                            n,
                        )
                    val x = RcFloat(vars[0])
                    val y = RcFloat(vars[1])
                    val vx = RcFloat(vars[2])
                    val vy = RcFloat(vars[3])
                    val life = RcFloat(vars[4])
                    val hue = RcFloat(vars[5])
                    process {
                        particlesLoop(
                            ps,
                            restart = y - h, // > 0 once the particle falls below the bottom
                            expressions =
                                listOf(
                                    x + vx * dt, // x += vx·dt
                                    y + vy * dt, // y += vy·dt
                                    vx, // vx unchanged
                                    vy + dt * 2600f, // vy += gravity·dt
                                    life - dt, // age out
                                    hue, // hue unchanged
                                ),
                        ) {
                            val c = remoteColorExpression(255, hue, 0.85f, 1f)
                            paint { color(c) }
                            drawCircle(x, y, RcFloat(9f))
                        }
                    }
                }
            }
        }
    }

/**
 * **3D particle fountain.** ~90 glowing spheres jet up from a point, spread and fall under gravity
 * in true 3D (depth-tested, lit), re-emitting at the base. The particle loop body opens a `scene3D
 * { }` and issues one `drawMesh` per particle — the per-particle `x,y,z` drive a reactive
 * `translate`, and `color(remoteColorExpression(hue…))` tints each sphere. Drag to orbit the whole
 * fountain.
 *
 * Shows particles + 3D together: `scene3D` is now on `RcScope`, so it's legal inside a
 * `particlesLoop` body, and the 3D ops (`Matrix3DOp`, `DrawMesh3D`) read the particle variables via
 * VariableSupport.
 */
@Suppress("RestrictedApiAndroidX")
public fun particleFountain3D(mode: RcMeshMode): ByteArray =
    createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        *docTags("3D Particle Fountain"),
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize().background(0xFF080614.toInt())) {
                paint { color(0xFFFFFFFF.toInt()) }
                val view = verticalSpinAngle()
                val tilt = verticalTiltAngle()
                val n = 90
                val dt = deltaTime()
                impulse(RcFloat(1_000_000f), RcFloat(0f)) {
                    val vars = FloatArray(7)
                    val ps =
                        createParticles(
                            vars,
                            listOf(
                                (rand() - 0.5f) * 0.5f, // x
                                RcFloat(-3.2f), // y (base)
                                (rand() - 0.5f) * 0.5f, // z
                                (rand() - 0.5f) * 3.4f, // vx
                                rand() * 4.0f + 6.2f, // vy (up)
                                (rand() - 0.5f) * 3.4f, // vz
                                particleIndex() / n.toFloat(), // hue
                            ),
                            n,
                        )
                    val x = RcFloat(vars[0])
                    val y = RcFloat(vars[1])
                    val z = RcFloat(vars[2])
                    val vx = RcFloat(vars[3])
                    val vy = RcFloat(vars[4])
                    val vz = RcFloat(vars[5])
                    val hue = RcFloat(vars[6])
                    process {
                        // Set up the 3D scene once per frame (camera/lights/mesh persist for the
                        // particle draws that follow).
                        var sphere: RcMesh? = null
                        scene3D {
                            perspectiveCameraDeg(
                                42f,
                                1f,
                                0.5f,
                                60f,
                                vec(RcFloat(0f), RcFloat(1.5f), RcFloat(11.5f)),
                                vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                                floatArrayOf(0f, 1f, 0f),
                            )
                            clearDepth()
                            clearLights()
                            directionalLight(
                                vec(RcFloat(-0.3f), RcFloat(-0.6f), RcFloat(-0.7f)),
                                0xFFFFFFFF.toInt(),
                                RcFloat(1.0f),
                            )
                            directionalLight(
                                vec(RcFloat(0.5f), RcFloat(0.4f), RcFloat(0.4f)),
                                0xFF6070A0.toInt(),
                                RcFloat(0.4f),
                            )
                            material(0.5f, 24f)
                            sphere = primitiveSphere(1, 1f, segments = 16f)
                        }
                        val mesh = sphere!!
                        particlesLoop(
                            ps,
                            restart = y * -1f - 3.4f, // > 0 once the particle falls below the base
                            expressions =
                                listOf(
                                    x + vx * dt, // x
                                    y + vy * dt, // y
                                    z + vz * dt, // z
                                    vx, // vx
                                    vy + dt * -13f, // vy += gravity·dt
                                    vz, // vz
                                    hue, // hue
                                ),
                        ) {
                            val c = remoteColorExpression(255, hue, 0.85f, 1f)
                            scene3D {
                                color(c)
                                orbitIdentity(view, tilt)
                                translate(x, y, z)
                                scale(0.2f)
                                drawMesh(mesh, mode = mode)
                            }
                        }
                    }
                }
            }
        }
    }

@Suppress("RestrictedApiAndroidX")
public fun particleFountain3DSoftware(): ByteArray = particleFountain3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun particleFountain3DCanvas(): ByteArray = particleFountain3D(RcMeshMode.CanvasSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun ParticleFountain2DPreview() {
    RemoteDocumentPreview(RemoteDocument(particleFountain2D()))
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun ParticleFountain3DPreview() {
    RemoteDocumentPreview(RemoteDocument(particleFountain3DSoftware()))
}
