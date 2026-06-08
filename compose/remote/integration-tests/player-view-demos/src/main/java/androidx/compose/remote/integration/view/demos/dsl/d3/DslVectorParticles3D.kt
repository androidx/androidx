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
@file:Suppress("RestrictedApiAndroidX")

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

private const val N = 120 // particles
private const val G = 6.0f // gravity
private const val LIFE = 3.0f // seconds
private const val R = 0.12f // sphere radius
private const val BG = 0xFF06080F.toInt()

/**
 * **A 3D particle fountain whose per-frame physics is written with vectors.** The position and
 * velocity updates are single [vec] expressions evaluated by `VectorRpn` and routed through
 * `particlesLoopVec`, instead of the usual three scalar equations each:
 * ```
 *   pos' = pos + vel * dt          // one vec3 equation, fills px,py,pz
 *   vel' = vel + vec(0,-G,0) * dt  // one vec3 equation, fills vx,vy,vz
 * ```
 *
 * Each `vec3` update is one logical equation (one op-header) that writes three consecutive particle
 * slots, instead of three scalar equations. The variables are still created as scalars (init runs
 * once and isn't the compactness-critical path); only the hot per-frame update is vectorized. Drag
 * to orbit.
 */
@Suppress("RestrictedApiAndroidX")
public fun vectorFountain3D(mode: RcMeshMode): ByteArray =
    createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription("Vector Particle Fountain"),
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize().background(BG)) {
                paint { color(0xFFFFFFFF.toInt()) }
                val view = verticalSpinAngle()
                val tilt = verticalTiltAngle()
                val dt = deltaTime()
                impulse(RcFloat(1_000_000f), RcFloat(0f)) {
                    val vars = FloatArray(8) // px,py,pz, vx,vy,vz, hue, age
                    val ps =
                        createParticles(
                            vars,
                            listOf(
                                rand() * 0.3f - 0.15f, // px
                                RcFloat(-1.6f), // py (nozzle)
                                rand() * 0.3f - 0.15f, // pz
                                rand() * 2.0f - 1.0f, // vx
                                rand() * 1.6f + 3.6f, // vy (up)
                                rand() * 2.0f - 1.0f, // vz
                                particleIndex() / N.toFloat(), // hue
                                rand() * LIFE, // age (staggered)
                            ),
                            N,
                        )
                    val px = RcFloat(vars[0])
                    val py = RcFloat(vars[1])
                    val pz = RcFloat(vars[2])
                    val vx = RcFloat(vars[3])
                    val vy = RcFloat(vars[4])
                    val vz = RcFloat(vars[5])
                    val hue = RcFloat(vars[6])
                    val age = RcFloat(vars[7])

                    val pos = vec(px, py, pz)
                    val vel = vec(vx, vy, vz)
                    val gravity = vec(RcFloat(0f), RcFloat(-G), RcFloat(0f))

                    process {
                        var ball: RcMesh? = null
                        // Unified RcVec 3D API: camera eye/center and light directions are vec3s.
                        val eye = vec(RcFloat(0f), RcFloat(0.5f), RcFloat(9.5f))
                        val center = vec(RcFloat(0f), RcFloat(0.3f), RcFloat(0f))
                        scene3D {
                            perspectiveCameraDeg(
                                42f,
                                1f,
                                0.5f,
                                60f,
                                eye,
                                center,
                                floatArrayOf(0f, 1f, 0f),
                            )
                            clearDepth()
                            clearLights()
                            directionalLight(
                                vec(RcFloat(-0.35f), RcFloat(-0.5f), RcFloat(-0.75f)),
                                0xFFFFFFFF.toInt(),
                                RcFloat(1.0f),
                            )
                            directionalLight(
                                vec(RcFloat(0.5f), RcFloat(0.4f), RcFloat(0.4f)),
                                0xFF7080B0.toInt(),
                                RcFloat(0.45f),
                            )
                            material(0.5f, 26f)
                            ball = primitiveSphere(1, R, segments = 14f)
                        }
                        val ballMesh = ball!!

                        particlesLoopVec(
                            ps,
                            restart = age * -1f, // recycle when age < 0
                            entries =
                                listOf(
                                    pos + vel * dt, // vec3 -> px,py,pz
                                    vel + gravity * dt, // vec3 -> vx,vy,vz
                                    hue, // scalar
                                    age - dt, // scalar
                                ),
                        ) {
                            val c = remoteColorExpression(255, hue, 0.85f, 1f)
                            scene3D {
                                color(c)
                                orbitIdentity(view, tilt)
                                translate(pos) // RcVec: same px,py,pz, one arg
                                drawMesh(ballMesh, mode = mode)
                            }
                        }
                    }
                }
            }
        }
    }

@Suppress("RestrictedApiAndroidX")
public fun vectorFountain3DSoftware(): ByteArray = vectorFountain3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun vectorFountain3DCanvas(): ByteArray = vectorFountain3D(RcMeshMode.CanvasSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun VectorFountain3DPreview() {
    RemoteDocumentPreview(RemoteDocument(vectorFountain3DSoftware()))
}
