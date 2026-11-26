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
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

private const val N = 10 // balls
private const val B = 2.4f // box half-extent (walls at ±B)
private const val R = 0.34f // ball radius
private const val SPEED = 2.3f // initial speed magnitude
private const val WALL = B - R // centre-coordinate where a ball touches a wall
private const val BG = 0xFF0B0E18.toInt()
private const val EDGE_COLOR =
    0xFF7FD8EC.toInt() // box wireframe edges (transparent box = edges only)

/**
 * The 12 edges of the cube `[-B,B]³`, each as `x1,y1,z1, x2,y2,z2`. A wireframe frame reads as a
 * transparent box; translucent faces can't be used because the software 3D path doesn't alpha-blend
 * between meshes (it would occlude the balls).
 */
private val EDGES: Array<FloatArray> = run {
    val c = floatArrayOf(-B, B)
    val list = ArrayList<FloatArray>()
    for (y in c) for (z in c) list.add(floatArrayOf(-B, y, z, B, y, z)) // 4 edges ‖ X
    for (x in c) for (z in c) list.add(floatArrayOf(x, -B, z, x, B, z)) // 4 edges ‖ Y
    for (x in c) for (y in c) list.add(floatArrayOf(x, y, -B, x, y, B)) // 4 edges ‖ Z
    list.toTypedArray()
}

/**
 * **10 balls bouncing in a transparent box**, where the *only* interaction is **ball ↔ wall** — the
 * balls pass through each other (no ball-ball collision). Each of the 6 walls is one
 * [particlesComparison] (the single-particle test form): its `condition` fires when a ball's centre
 * passes within [R] of that wall, and `then` reflects the matching velocity component and clamps
 * the ball back inside. A [particlesLoop] then integrates the (zero-gravity, perfectly elastic)
 * motion, so the balls ricochet around the box forever.
 *
 * Per-particle variables are `[px, py, pz, vx, vy, vz, hue]`. The box itself is drawn last as a
 * translucent cube so you see the balls through its glass walls. Drag to orbit.
 */
@Suppress("RestrictedApiAndroidX")
public fun ballBox3D(mode: RcMeshMode): ByteArray =
    createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription("Balls in a Box"),
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
                                rand() * (2f * WALL) - WALL, // px in (-WALL, WALL)
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

                        // --- ball ↔ wall collisions: one particlesComparison per wall face ---
                        // +X / -X: reflect vx, clamp px
                        bounce(ps, px - WALL, arrayOf(RcFloat(WALL), py, pz, vx * -1f, vy, vz, hue))
                        bounce(
                            ps,
                            px * -1f - WALL,
                            arrayOf(RcFloat(-WALL), py, pz, vx * -1f, vy, vz, hue),
                        )
                        // +Y / -Y: reflect vy, clamp py
                        bounce(ps, py - WALL, arrayOf(px, RcFloat(WALL), pz, vx, vy * -1f, vz, hue))
                        bounce(
                            ps,
                            py * -1f - WALL,
                            arrayOf(px, RcFloat(-WALL), pz, vx, vy * -1f, vz, hue),
                        )
                        // +Z / -Z: reflect vz, clamp pz
                        bounce(ps, pz - WALL, arrayOf(px, py, RcFloat(WALL), vx, vy, vz * -1f, hue))
                        bounce(
                            ps,
                            pz * -1f - WALL,
                            arrayOf(px, py, RcFloat(-WALL), vx, vy, vz * -1f, hue),
                        )

                        // --- integrate motion (no gravity) and draw each ball ---
                        particlesLoop(
                            ps,
                            restart = null,
                            expressions =
                                listOf(
                                    px + vx * dt, // px += vx·dt
                                    py + vy * dt, // py += vy·dt
                                    pz + vz * dt, // pz += vz·dt
                                    vx,
                                    vy,
                                    vz, // velocities unchanged (collisions handle bounces)
                                    hue,
                                ),
                        ) {
                            val c = remoteColorExpression(255, hue, 0.8f, 1f)
                            scene3D {
                                color(c)
                                orbitIdentity(view, tilt)
                                translate(px, py, pz)
                                drawMesh(ballMesh, mode = mode)
                            }
                        }

                        // --- the transparent box: a wireframe edge frame (see-through) ---
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
public fun ballBox3DSoftware(): ByteArray = ballBox3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun ballBox3DCanvas(): ByteArray = ballBox3D(RcMeshMode.CanvasSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun BallBox3DPreview() {
    RemoteDocumentPreview(RemoteDocument(ballBox3DSoftware()))
}
