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
import androidx.compose.remote.creation.dsl.RcMeshMode
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.floor
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.primitiveCube
import androidx.compose.remote.creation.dsl.sqrt
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

private const val BG = 0xFF0A0E16.toInt()
private const val N = 14 // grid is N×N cubes
private const val SPACING = 0.92f
private const val AMP = 1.7f // wave height
private const val CUBE = 0.62f

/**
 * A grid of **N×N cubes rippling in a synchronized wave**. The wave is a single radial sine of
 * time: each cube's height is `sin(t·speed − distanceFromCenter·freq)`, so a ring of motion travels
 * out from the middle. The entire grid is emitted as **one player-side [loop]** of N² iterations —
 * the index is decoded into row/col and drives a reactive
 * [androidx.compose.remote.integration.view.demos.dsl.translate], so the document holds one cube
 * and one loop, not 196 unrolled draws, and the animation runs on-device.
 *
 * API note: every cube shares one [color] because `color(...)` is not a reactive/per-iteration
 * value — a per-height color gradient would have to be unrolled. The wave reads purely through
 * lighting.
 */
@Suppress("RestrictedApiAndroidX")
public fun cubeWave3D(mode: RcMeshMode): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription("Cube Wave"),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(BG)) {
                paint { color(0xFFFFFFFF.toInt()) }
                val t = continuousSeconds()
                // Drag to orbit the grid so the wave reads in 3D (horizontal = spin, vertical =
                // tilt).
                val view = verticalSpinAngle()
                val tilt = verticalTiltAngle()
                val half = (N - 1) / 2f

                // Reactive 3D base color (Tier-1 color(RcColor)): the hue drifts over time, in
                // [0,1).
                // One reactive color shared by every cube in the loop, re-resolved each frame on
                // the
                // player — no document re-upload. (Per-cube-distinct color would be Tier 2.)
                val hue = (t * 0.07f) % 1f
                val waveColor =
                    remoteColorExpression(alpha = 255, hue = hue, saturation = 0.6f, value = 0.95f)

                scene3D {
                    val cube = primitiveCube(1, CUBE, CUBE, CUBE)

                    perspectiveCameraDeg(
                        fovYDeg = 40f,
                        aspect = 1f,
                        near = 0.5f,
                        far = 80f,
                        eye = vec(RcFloat(0f), RcFloat(9.5f), RcFloat(13.5f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-0.4f), RcFloat(-0.8f), RcFloat(-0.45f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(1.0f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(0.5f), RcFloat(0.3f), RcFloat(0.5f)),
                        color = 0xFF4A6CA8.toInt(),
                        intensity = RcFloat(0.5f),
                    )

                    color(waveColor)
                    loop(0f, 1f, (N * N).toFloat()) { idx ->
                        // Decode the flat index into row/col.
                        val row = floor(idx / N.toFloat())
                        val col = idx % N.toFloat()
                        val x = (col - half) * SPACING
                        val z = (row - half) * SPACING
                        // True radial distance → concentric circular ripples. This single
                        // expression
                        // is ~35 RPN tokens (over the 32 cap); FloatExpressionOptimizer auto-spills
                        // and shares dx/dz so it compiles with no manual flush() — even inside a
                        // loop.
                        val dx = col - half
                        val dz = row - half
                        val dist = sqrt(dx * dx + dz * dz).flush()
                        val y = (dist * -1.1f + t * 2.4f).sin() * AMP
                        orbitIdentity(view, tilt)
                        translate(x, y, z)
                        drawMesh(cube, mode = mode)
                    }
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun cubeWave3DSoftware(): ByteArray = cubeWave3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun cubeWave3DCanvas(): ByteArray = cubeWave3D(RcMeshMode.CanvasSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun CubeWave3DPreview() {
    RemoteDocumentPreview(RemoteDocument(cubeWave3DSoftware()))
}
