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
import androidx.compose.remote.creation.dsl.RcMeshMode
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.primitiveCube
import androidx.compose.remote.creation.dsl.sin
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Application-style demo: a **3D bar chart** — a row of rising rectangular bars (boxes), each a
 * different color, viewed at a three-quarter angle, modeled on the classic glossy "rising bars"
 * stock illustration.
 *
 * Each bar is a single scaled [primitiveCube] cube, drawn with **flat** shading
 * ([RcMeshMode.SoftwareFlat]): the cube carries no stored normals, so the software backend lights
 * each face by its geometric normal — giving the crisp per-face Lambert look of a solid box (bright
 * top, mid front, dark side) rather than a rounded gradient. Bars are axis-aligned boxes with no
 * thin diagonals and no large translucent fills, so this renders far cheaper than the bubble chart.
 * The whole chart rocks gently (a `sin`-driven oscillation) so it always reads front-on.
 */
@Suppress("RestrictedApiAndroidX")
public fun barChart3D(mode: RcMeshMode): ByteArray = barChart3D(mode, glossy = false)

/**
 * The bar chart with an optional **glossy** finish. When [glossy] is set, each bar gets a
 * Blinn-Phong specular highlight via [RcScene3DScope.material] — a bright white reflection band
 * that gradates down the tall faces (the software backend lights the flat faces per-vertex when a
 * specular material is active), matching the shiny plastic look of the reference illustration.
 */
@Suppress("RestrictedApiAndroidX")
public fun barChart3D(mode: RcMeshMode, glossy: Boolean): ByteArray {
    // Rainbow palette, left → right, matching the reference illustration.
    val colors =
        intArrayOf(
            0xFFC0392B.toInt(), // brick red
            0xFFD9A521.toInt(), // gold
            0xFF8BC34A.toInt(), // light green
            0xFF5FB033.toInt(), // green
            0xFF7FCBC4.toInt(), // teal
            0xFF3D8FE0.toInt(), // blue
            0xFFE0392E.toInt(), // red (tallest)
        )
    val heights = floatArrayOf(0.9f, 1.4f, 2.1f, 2.6f, 3.3f, 4.1f, 5.0f)
    val n = colors.size
    val barW = 0.7f // footprint width/depth
    val pitch = 0.95f // center-to-center spacing (leaves a small gap)
    val x0 = -(n - 1) * 0.5f * pitch // center the row on the origin

    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true)) {
        // Gentle rock: ±0.45 rad oscillation so the chart stays front-on.
        val spin = sin(continuousSeconds() * 0.5f) * 0.45f
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFFFFFFFF.toInt())) {
                paint { color(0xFFFFFFFF.toInt()) }
                scene3D {
                    val cube = primitiveCube(1, 1f, 1f, 1f)

                    perspectiveCameraDeg(
                        fovYDeg = 31f,
                        aspect = 1f,
                        near = 0.1f,
                        far = 100f,
                        eye = vec(RcFloat(5.0f), RcFloat(4.7f), RcFloat(10.8f)),
                        center = vec(RcFloat(0f), RcFloat(2.4f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    // Key light: front-top-right (light travels down-left-toward-viewer), so the
                    // top faces are brightest, front faces mid, right/side faces darkest. Kept
                    // fairly frontal so the glossy sheen lands on the visible front faces.
                    directionalLight(
                        direction = vec(RcFloat(-0.32f), RcFloat(-0.62f), RcFloat(-0.62f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(0.9f),
                    )
                    // Cool fill from the opposite side so the shadowed faces aren't pure black.
                    directionalLight(
                        direction = vec(RcFloat(0.6f), RcFloat(-0.15f), RcFloat(0.5f)),
                        color = 0xFFAFC4E0.toInt(),
                        intensity = RcFloat(0.3f),
                    )

                    // Glossy plastic: a broad, bright specular sheen across the top and upper
                    // front faces. Matte (no specular) otherwise.
                    if (glossy) {
                        material(specStrength = 0.85f, shininess = 18f)
                    }

                    for (i in 0 until n) {
                        val h = heights[i]
                        color(colors[i])
                        identity()
                        rotate(angleRad = spin, ax = 0f, ay = 1f, az = 0f)
                        translate(x0 + i * pitch, h * 0.5f, 0f) // base sits on y = 0
                        scale(barW, h, barW)
                        drawMesh(cube, mode = mode)
                    }
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun barChart3DSoftware(): ByteArray = barChart3D(RcMeshMode.SoftwareFlat)

@Suppress("RestrictedApiAndroidX")
public fun barChart3DCanvas(): ByteArray = barChart3D(RcMeshMode.CanvasFlat)

@Suppress("RestrictedApiAndroidX")
public fun glossyBarChart3DSoftware(): ByteArray =
    barChart3D(RcMeshMode.SoftwareFlat, glossy = true)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun BarChart3DPreview() {
    RemoteDocumentPreview(RemoteDocument(barChart3DSoftware()))
}
