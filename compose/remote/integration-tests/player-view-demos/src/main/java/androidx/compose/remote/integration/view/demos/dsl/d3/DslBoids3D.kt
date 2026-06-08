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
import androidx.compose.remote.creation.dsl.RcScene3DScope
import androidx.compose.remote.creation.dsl.atan2
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

private const val BG = 0xFF0B1422.toInt()
private const val BIRD_A = 0xFFE7ECF5.toInt()
private const val BIRD_B = 0xFFE0876E.toInt()
private const val NB = 40
private const val SWARM = 2.4f // flock-center wander radius (smaller → stays more centered)
private const val SPD = 0.85f

// Flock-center wander frequencies (rad/s) and amplitudes — shared by all birds (cohesion).
private const val FX = 0.23f
private const val FZ = 0.19f
private const val FY = 0.37f

/** A small dart mesh pointing +Z (flat-shaded tetra: nose forward, swept wings, slight belly). */
private fun RcScene3DScope.birdMesh(id: Int): RcMesh {
    val verts =
        floatArrayOf(
            0f,
            0f,
            0.55f, // 0 nose
            -0.42f,
            0f,
            -0.30f, // 1 left wing
            0.42f,
            0f,
            -0.30f, // 2 right wing
            0f,
            -0.12f,
            -0.12f, // 3 belly
        )
    val indices =
        intArrayOf(
            0,
            2,
            1, // top
            0,
            1,
            3, // left
            0,
            3,
            2, // right
            1,
            2,
            3, // back
        )
    return defineMesh(id, indices, verts)
}

/**
 * **Flocking birds (Boids)** — a swirling flock that wheels and turns as one. A shared "flock
 * center" wanders on slow sines (cohesion) while each bird circles it on its own phase, and every
 * bird is yawed to face its **velocity** via `atan2(vx, vz)` of the analytic derivative of its
 * path. The result reads convincingly like a murmuration.
 *
 * **API note — the real weakness this surfaces:** these are not true boids. Reynolds boids
 * integrate each bird's velocity from its neighbors *every frame* (separation/alignment/cohesion),
 * which needs persistent cross-frame state and O(N²) neighbor queries. The reactive model is **pure
 * closed-form functions of time** — there's no way to feed last frame's positions back in, no
 * scratch buffers, no per-frame integration. So this is a *scripted* flock (each path baked as
 * sin/cos of t), not an emergent one. A general "particle/simulation" op with retained state is the
 * missing primitive.
 */
@Suppress("RestrictedApiAndroidX")
public fun boids3D(mode: RcMeshMode): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription("Flocking Birds"),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(BG)) {
                paint { color(0xFFFFFFFF.toInt()) }
                val t = continuousSeconds()
                // Drag to orbit the flock (horizontal = spin, vertical = tilt).
                val view = verticalSpinAngle()
                val tilt = verticalTiltAngle()

                scene3D {
                    val bird = birdMesh(1)

                    perspectiveCameraDeg(
                        fovYDeg = 46f,
                        aspect = 1f,
                        near = 0.5f,
                        far = 80f,
                        eye = vec(RcFloat(0f), RcFloat(3.6f), RcFloat(12.0f)),
                        center = vec(RcFloat(0f), RcFloat(0.6f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-0.4f), RcFloat(-0.6f), RcFloat(-0.7f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(1.0f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(0.5f), RcFloat(0.3f), RcFloat(0.5f)),
                        color = 0xFF6E86B8.toInt(),
                        intensity = RcFloat(0.5f),
                    )

                    val golden = 2.39996f // golden angle, for even phase spread
                    for (i in 0 until NB) {
                        val phi = i * golden
                        val ringR = 0.7f + (i % 6) * 0.34f
                        // ang is reused by px/pz/vx/vz; FloatExpressionOptimizer shares it (and
                        // keeps
                        // the yaw atan2(vx,vz) under the 32-token cap) without a manual flush().
                        val ang = t * SPD + phi // bird's own orbit angle (reactive)

                        // Position = wandering flock center + per-bird orbit.
                        val px = (t * FX).sin() * SWARM + ang.sin() * ringR
                        val pz = (t * FZ).cos() * SWARM + ang.cos() * ringR
                        val py =
                            (t * FY + 1.0f).sin() * 1.3f + (t * 0.9f + phi * 1.7f).sin() * 0.55f

                        // Velocity direction (analytic derivative) → heading yaw about Y.
                        val vx = (t * FX).cos() * (SWARM * FX) + ang.cos() * (ringR * SPD)
                        val vz = (t * FZ).sin() * (-SWARM * FZ) - ang.sin() * (ringR * SPD)
                        val yaw = atan2(vx, vz)

                        color(if (i % 3 == 0) BIRD_B else BIRD_A)
                        orbitIdentity(view, tilt)
                        translate(px, py, pz)
                        rotate(yaw, 0f, 1f, 0f)
                        drawMesh(bird, mode = mode)
                    }
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun boids3DSoftware(): ByteArray = boids3D(RcMeshMode.SoftwareFlat)

@Suppress("RestrictedApiAndroidX")
public fun boids3DCanvas(): ByteArray = boids3D(RcMeshMode.CanvasFlat)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun Boids3DPreview() {
    RemoteDocumentPreview(RemoteDocument(boids3DSoftware()))
}
