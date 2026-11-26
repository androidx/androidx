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
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.primitiveCube
import androidx.compose.remote.creation.dsl.primitivePlane
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.integration.view.demos.dsl.kit.Noise3D
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

private const val BG = 0xFF0B1320.toInt()
private const val GRID = 18 // GRID×GRID columns
private const val CW = 0.6f // column footprint (world units)
private const val MAX_H = 3.6f // tallest column
private const val WATER_LEVEL = 0.34f // normalized height the sea fills to

private const val COL_SAND = 0xFFD8C284.toInt()
private const val COL_GRASS = 0xFF4F9E3A.toInt()
private const val COL_ROCK = 0xFF7C7E88.toInt()
private const val COL_SNOW = 0xFFEDF2F8.toInt()
private const val COL_WATER = 0xA03A78C8.toInt() // translucent blue (alpha ~0x A0)

private fun biomeColor(nh: Float): Int =
    when {
        nh < 0.30f -> COL_SAND
        nh < 0.52f -> COL_GRASS
        nh < 0.74f -> COL_ROCK
        else -> COL_SNOW
    }

/**
 * A **Minecraft-style voxel terrain** generated from value noise. A height field is sampled on an
 * 18×18 grid at author time; each cell becomes one box column scaled to its height and tinted by a
 * biome band (sand / grass / rock / snow), with a translucent sea plane at the water line. A slow
 * turntable spin (reactive [rotate]) shows the blocky silhouette in the round.
 *
 * API note: this one is **unrolled** (324 colored draws), not a [loop], precisely because each
 * column needs its own [color] and its own non-uniform height [scale] — both of which are author-
 * time-only. It's the mirror image of the cube-wave demo and shows where instancing can't reach:
 * per-instance color and per-axis size.
 */
@Suppress("RestrictedApiAndroidX")
public fun voxelTerrain3D(mode: RcMeshMode): ByteArray {
    // --- Author-time terrain: heights + colors ---
    val half = (GRID - 1) / 2f
    val nh = Array(GRID) { FloatArray(GRID) }
    for (gx in 0 until GRID) {
        for (gz in 0 until GRID) {
            nh[gx][gz] = Noise3D.fbm(gx * 0.28f, gz * 0.28f, octaves = 2)
        }
    }
    val waterY = WATER_LEVEL * MAX_H

    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription("Voxel Terrain"),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(BG)) {
                paint { color(0xFFFFFFFF.toInt()) }
                // Drag to orbit the terrain (horizontal = spin, vertical = tilt).
                val view = verticalSpinAngle()
                val tilt = verticalTiltAngle()

                scene3D {
                    val cube = primitiveCube(1, 1f, 1f, 1f)
                    val sea = primitivePlane(2, GRID * CW, GRID * CW)

                    perspectiveCameraDeg(
                        fovYDeg = 38f,
                        aspect = 1f,
                        near = 0.5f,
                        far = 90f,
                        eye = vec(RcFloat(0f), RcFloat(8.5f), RcFloat(13.5f)),
                        center = vec(RcFloat(0f), RcFloat(1.0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-0.45f), RcFloat(-0.78f), RcFloat(-0.45f)),
                        color = 0xFFFFF4E0.toInt(),
                        intensity = RcFloat(1.0f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(0.55f), RcFloat(0.35f), RcFloat(0.5f)),
                        color = 0xFF5C77AE.toInt(),
                        intensity = RcFloat(0.45f),
                    )

                    for (gx in 0 until GRID) {
                        for (gz in 0 until GRID) {
                            val h = (nh[gx][gz] * MAX_H).coerceAtLeast(0.45f)
                            val x = (gx - half) * CW
                            val z = (gz - half) * CW
                            color(biomeColor(nh[gx][gz]))
                            orbitIdentity(view, tilt)
                            translate(x, h * 0.5f, z)
                            scale(CW * 0.98f, h, CW * 0.98f)
                            drawMesh(cube, mode = mode)
                        }
                    }

                    // Translucent sea plane (drawn last so it composites over the land below it).
                    color(COL_WATER)
                    orbitIdentity(view, tilt)
                    translate(0f, waterY, 0f)
                    rotate((-Math.PI / 2.0).toFloat(), 1f, 0f, 0f)
                    drawMesh(sea, mode = mode)
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun voxelTerrain3DSoftware(): ByteArray = voxelTerrain3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun voxelTerrain3DCanvas(): ByteArray = voxelTerrain3D(RcMeshMode.CanvasSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun VoxelTerrain3DPreview() {
    RemoteDocumentPreview(RemoteDocument(voxelTerrain3DSoftware()))
}
