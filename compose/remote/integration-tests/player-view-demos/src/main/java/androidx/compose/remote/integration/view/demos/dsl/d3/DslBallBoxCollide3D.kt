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
import androidx.compose.remote.creation.dsl.RcParticleComparisonScope
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.concatenate
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.loadR1
import androidx.compose.remote.creation.dsl.loadR2
import androidx.compose.remote.creation.dsl.loadR3
import androidx.compose.remote.creation.dsl.min
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.primitiveCylinder
import androidx.compose.remote.creation.dsl.primitiveSphere
import androidx.compose.remote.creation.dsl.square
import androidx.compose.remote.creation.dsl.squareSum
import androidx.compose.remote.creation.dsl.storeR1
import androidx.compose.remote.creation.dsl.storeR2
import androidx.compose.remote.creation.dsl.storeR3
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

private const val N = 8 // balls
private const val B = 2.5f // box half-extent (walls at ±B)
private const val R = 0.46f // ball radius (big enough that ball-ball hits are frequent)
private const val SPEED = 2.0f // initial speed magnitude
private const val WALL = B - R // centre-coordinate where a ball touches a wall
private const val FOUR_R2 = (2f * R) * (2f * R) // (2R)² — squared centre distance at contact
private const val DDEPS = 1e-4f // keeps the impulse denominator finite if two balls coincide
private const val BG = 0xFF0B0E18.toInt()
private const val EDGE_COLOR = 0xFF7FD8EC.toInt()

/** The 12 edges of the cube `[-B,B]³` as `x1,y1,z1, x2,y2,z2` (a see-through wireframe box). */
private val EDGES: Array<FloatArray> = run {
    val c = floatArrayOf(-B, B)
    val list = ArrayList<FloatArray>()
    for (y in c) for (z in c) list.add(floatArrayOf(-B, y, z, B, y, z))
    for (x in c) for (z in c) list.add(floatArrayOf(x, -B, z, x, B, z))
    for (x in c) for (y in c) list.add(floatArrayOf(x, y, -B, x, y, B))
    list.toTypedArray()
}

/**
 * **Balls in a box with ball ↔ ball collisions.** The sequel to [ballBox3D]: in addition to the six
 * walls, every *pair* of balls now collides elastically. This uses the two-body
 * [particlesComparison] (the `then1`/`then2` overload), which walks all unordered particle pairs
 * and, for each, resolves `condition`/`then1`/`then2` where [first]`(v)` / [second]`(v)` read the
 * two particles' copies of variable `v`.
 *
 * **Why this pushes the API:** an elastic collision of two equal-mass spheres is `v1' = v1 −
 * ((v1−v2)·d̂)d̂`, `v2' = v2 + (…)d̂` with `d = p1 − p2`. Fully inlined, each velocity equation is
 * ~56 tokens — over the 46-token particle-equation cap. The fix is the evaluator's **scratch
 * registers**: the `condition` computes the inter-ball delta `d = (dx,dy,dz)` once and
 * [storeR1]/[storeR2]/[storeR3] it; the registers persist across the pair's condition→then1→then2
 * evaluations, so the `then` equations reuse it via [loadR1] etc. (one token each), bringing every
 * equation back under the cap. The approaching/receding gate is folded into the impulse as `min(0,
 * (v1−v2)·d)` so the `condition` only has to test overlap (`|d|² < (2R)²`) — that keeps the
 * condition small and stops overlapping-but-separating balls from re-colliding (no sticking).
 *
 * Per-particle variables are `[px, py, pz, vx, vy, vz, hue]`. Drag to orbit.
 */
@Suppress("RestrictedApiAndroidX")
public fun ballBoxCollide3D(mode: RcMeshMode): ByteArray =
    createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription("Colliding Balls in a Box"),
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
                                rand() * (2f * WALL) - WALL, // px
                                rand() * (2f * WALL) - WALL, // py
                                rand() * (2f * WALL) - WALL, // pz
                                rand() * (2f * SPEED) - SPEED, // vx
                                rand() * (2f * SPEED) - SPEED, // vy
                                rand() * (2f * SPEED) - SPEED, // vz
                                particleIndex() / N.toFloat(), // hue
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
                            ball = primitiveSphere(1, R, segments = 18f)
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

                        // --- ball ↔ ball: one two-body comparison over every pair ---
                        // d = p1 - p2 (stored in R1,R2,R3 by the condition so the then-equations
                        // can reuse it). proj = min(0, (v1-v2)·d) / (|d|² + eps): the min(0,·)
                        // makes
                        // it act only while the pair is approaching, so the condition only tests
                        // overlap. Equal-mass elastic: v1 -= proj·d, v2 += proj·d.
                        val dd =
                            squareSum(loadR1(), loadR2()) +
                                square(loadR3()) // |d|² (from registers)
                        // (v1-v2)·d, then proj = min(0, ·) / (|d|² + eps); needs the pair scope.
                        fun RcParticleComparisonScope.proj(): RcFloat {
                            val dot =
                                (first(vx) - second(vx)) * loadR1() +
                                    (first(vy) - second(vy)) * loadR2() +
                                    (first(vz) - second(vz)) * loadR3()
                            return min(0f, dot) / (dd + DDEPS)
                        }
                        particlesComparison(
                            ps,
                            RcFloat(-1f),
                            RcFloat(-1f),
                            condition = {
                                concatenate(
                                    storeR1(first(px) - second(px)),
                                    storeR2(first(py) - second(py)),
                                    storeR3(first(pz) - second(pz)),
                                    RcFloat(FOUR_R2) - dd,
                                )
                            },
                            // then1: particle1 keeps its position/hue, reflects its velocity
                            then1 = {
                                val proj = proj()
                                listOf(
                                    px,
                                    py,
                                    pz,
                                    first(vx) - proj * loadR1(),
                                    first(vy) - proj * loadR2(),
                                    first(vz) - proj * loadR3(),
                                    hue,
                                )
                            },
                            // then2: particle2 keeps its position/hue, reflects the other way
                            then2 = {
                                val proj = proj()
                                listOf(
                                    px,
                                    py,
                                    pz,
                                    second(vx) + proj * loadR1(),
                                    second(vy) + proj * loadR2(),
                                    second(vz) + proj * loadR3(),
                                    hue,
                                )
                            },
                        ) {}

                        // --- ball ↔ wall: one single-body comparison per face ---
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

                        // --- integrate (no gravity) and draw each ball ---
                        particlesLoop(
                            ps,
                            restart = null,
                            expressions =
                                listOf(px + vx * dt, py + vy * dt, pz + vz * dt, vx, vy, vz, hue),
                        ) {
                            val c = remoteColorExpression(255, hue, 0.8f, 1f)
                            scene3D {
                                color(c)
                                orbitIdentity(view, tilt)
                                translate(px, py, pz)
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

/**
 * One wall: when [condition] > 0 the matching velocity is reflected and the ball clamped ([then]).
 */
@Suppress("RestrictedApiAndroidX")
private fun RcScope.bounce(ps: RcFloat, condition: RcFloat, then: Array<RcFloat>) {
    particlesComparison(ps, RcFloat(-1f), RcFloat(-1f), condition, then.toList()) {}
}

@Suppress("RestrictedApiAndroidX")
public fun ballBoxCollide3DSoftware(): ByteArray = ballBoxCollide3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun ballBoxCollide3DCanvas(): ByteArray = ballBoxCollide3D(RcMeshMode.CanvasSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun BallBoxCollide3DPreview() {
    RemoteDocumentPreview(RemoteDocument(ballBoxCollide3DSoftware()))
}
