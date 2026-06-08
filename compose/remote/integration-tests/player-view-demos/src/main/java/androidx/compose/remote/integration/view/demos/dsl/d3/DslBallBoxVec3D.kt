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
@file:SuppressLint("PrimitiveInCollection")

package androidx.compose.remote.integration.view.demos.dsl.d3

import android.annotation.SuppressLint
import androidx.compose.remote.creation.ExperimentalRemoteCreationApi
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcHeaderTag
import androidx.compose.remote.creation.dsl.RcMesh
import androidx.compose.remote.creation.dsl.RcMeshMode
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.primitiveCylinder
import androidx.compose.remote.creation.dsl.primitiveSphere
import androidx.compose.remote.creation.dsl.square
import androidx.compose.remote.creation.dsl.squareSum
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

private const val N = 8
private const val B = 2.5f
private const val R = 0.46f
private const val SPEED = 2.0f
private const val WALL = B - R
private const val FOUR_R2 = (2f * R) * (2f * R)
private const val SEP = 0.5f // on contact, push the pair apart by this fraction of their separation
private const val BG = 0xFF0B0E18.toInt()
private const val EDGE_COLOR = 0xFF7FD8EC.toInt()

private val EDGES: Array<FloatArray> = run {
    val c = floatArrayOf(-B, B)
    val list = ArrayList<FloatArray>()
    for (y in c) for (z in c) list.add(floatArrayOf(-B, y, z, B, y, z))
    for (x in c) for (z in c) list.add(floatArrayOf(x, -B, z, x, B, z))
    for (x in c) for (y in c) list.add(floatArrayOf(x, y, -B, x, y, B))
    list.toTypedArray()
}

/**
 * **Ball ↔ ball collisions written with vectors.** The vector version of [ballBoxCollide3D]: the
 * two-body collision response is expressed as whole-vec3 equations via `particlesComparisonVec`,
 * and the integration via `particlesLoopVec` — instead of per-component scalar equations.
 *
 * On contact (overlap condition, scalar) each ball takes the other's velocity (`vel ← otherVel`, a
 * one-op vec3 swap — correct elastic exchange for equal masses) and the pair is nudged apart along
 * their separation so they don't re-collide (`pos ← pos ± d·SEP`). Each of these is a single vec3
 * `then`-equation that fills three consecutive particle slots, evaluated by `VectorRpn`.
 *
 * Per-particle variables `[px, py, pz, vx, vy, vz, hue]`. Drag to orbit.
 */
@Suppress("RestrictedApiAndroidX")
public fun ballBoxVec3D(mode: RcMeshMode): ByteArray =
    createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription("Colliding Balls (vectors)"),
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize().background(BG)) {
                paint { color(0xFFFFFFFF.toInt()) }
                val view = verticalSpinAngle()
                val tilt = verticalTiltAngle()
                val dt = deltaTime()
                impulse(RcFloat(1_000_000f), RcFloat(0f)) {
                    val vars = FloatArray(7)
                    val ps =
                        createParticles(
                            vars,
                            listOf(
                                rand() * (2f * WALL) - WALL,
                                rand() * (2f * WALL) - WALL,
                                rand() * (2f * WALL) - WALL,
                                rand() * (2f * SPEED) - SPEED,
                                rand() * (2f * SPEED) - SPEED,
                                rand() * (2f * SPEED) - SPEED,
                                particleIndex() / N.toFloat(),
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

                    val pos = vec(px, py, pz)
                    val vel = vec(vx, vy, vz)

                    process {
                        var ball: RcMesh? = null
                        val edges = ArrayList<RcMesh>()
                        scene3D {
                            perspectiveCameraDeg(
                                40f,
                                1f,
                                0.5f,
                                60f,
                                vec(RcFloat(0f), RcFloat(0f), RcFloat(11.5f)),
                                vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
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
                            material(0.5f, 28f)
                            ball =
                                primitiveSphere(
                                    1,
                                    R,
                                    vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                                    segments = 18f,
                                )
                            EDGES.forEachIndexed { i, e ->
                                edges.add(
                                    primitiveCylinder(
                                        10 + i,
                                        0.035f,
                                        e[0],
                                        e[1],
                                        e[2],
                                        e[3],
                                        e[4],
                                        e[5],
                                        segments = 6f,
                                    )
                                )
                            }
                        }
                        val ballMesh = ball!!

                        // --- ball ↔ ball: one two-body comparison, vector then-equations ---
                        // condition: overlap (scalar). d = p1 - p2.
                        particlesComparisonVec(
                            ps,
                            RcFloat(-1f),
                            RcFloat(-1f),
                            condition = {
                                RcFloat(FOUR_R2) -
                                    (squareSum(first(px) - second(px), first(py) - second(py)) +
                                        square(first(pz) - second(pz)))
                            },
                            // then1: push particle1 along +d, take particle2's velocity
                            thenFirstParticle = {
                                val d = first(pos) - second(pos)
                                listOf(first(pos) + d * SEP, second(vel), hue)
                            },
                            // then2: push particle2 along -d, take particle1's velocity
                            thenSecondParticle = {
                                val d = first(pos) - second(pos)
                                listOf(second(pos) - d * SEP, first(vel), hue)
                            },
                        ) {}

                        // --- ball ↔ wall: one single-body comparison per face (scalar) ---
                        bounce(ps, px - WALL, arrayOf(RcFloat(WALL), py, pz, vx * -1f, vy, vz, hue))
                        bounce(
                            ps,
                            px * -1f - WALL,
                            arrayOf(RcFloat(-WALL), py, pz, vx * -1f, vy, vz, hue),
                        )
                        bounce(ps, py - WALL, arrayOf(px, RcFloat(WALL), pz, vx, vy * -1f, vz, hue))
                        bounce(
                            ps,
                            py * -1f - WALL,
                            arrayOf(px, RcFloat(-WALL), pz, vx, vy * -1f, vz, hue),
                        )
                        bounce(ps, pz - WALL, arrayOf(px, py, RcFloat(WALL), vx, vy, vz * -1f, hue))
                        bounce(
                            ps,
                            pz * -1f - WALL,
                            arrayOf(px, py, RcFloat(-WALL), vx, vy, vz * -1f, hue),
                        )

                        // --- integrate (zero-g) with a vector loop, and draw ---
                        particlesLoopVec(
                            ps,
                            restart = null,
                            entries = listOf(pos + vel * dt, vel, hue),
                        ) {
                            val c = remoteColorExpression(255, hue, 0.8f, 1f)
                            scene3D {
                                color(c)
                                orbitIdentity(view, tilt)
                                translate(pos)
                                drawMesh(ballMesh, mode = mode)
                            }
                        }

                        scene3D {
                            color(EDGE_COLOR)
                            orbitIdentity(view, tilt)
                            for (e in edges) drawMesh(e, mode = mode)
                        }
                    }
                }
            }
        }
    }

@Suppress("RestrictedApiAndroidX")
private fun RcScope.bounce(ps: RcFloat, condition: RcFloat, then: Array<RcFloat>) {
    particlesComparison(ps, RcFloat(-1f), RcFloat(-1f), condition, then.toList()) {}
}

@Suppress("RestrictedApiAndroidX")
public fun ballBoxVec3DSoftware(): ByteArray = ballBoxVec3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun ballBoxVec3DCanvas(): ByteArray = ballBoxVec3D(RcMeshMode.CanvasSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun BallBoxVec3DPreview() {
    RemoteDocumentPreview(RemoteDocument(ballBoxVec3DSoftware()))
}
