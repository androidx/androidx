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
import androidx.compose.remote.creation.dsl.primitiveCylinder
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * DSL demo of a rotating cylinder, tilted so both a flat end cap and the curved side wall are
 * visible at once. It shows the mixed-surface primitive: the side wall is smoothly shaded (round
 * silhouette, continuous gradient) while the top/bottom caps stay flat with a **sharp rim crease**
 * — that crease survives even in `*_SMOOTH` because [primitiveCylinder] duplicates the rim vertices
 * (radial normal on the wall, axial normal on the cap).
 *
 * Lit by a warm key + cool fill via the light op, and parameterized by [mode] so the same scene can
 * run through any backend/shading combination.
 */
@Suppress("RestrictedApiAndroidX")
public fun cylinder3D(mode: RcMeshMode): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true)) {
        val time = continuousSeconds()
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFF101820.toInt())) {
                paint { color(0xFFB8B0A0.toInt()) }

                scene3D {
                    val cyl = primitiveCylinder(1, 0.7f, 0f, -0.8f, 0f, 0f, 0.8f, 0f)

                    perspectiveCameraDeg(
                        fovYDeg = 45f,
                        aspect = 1f,
                        near = 0.1f,
                        far = 100f,
                        eye = vec(RcFloat(0f), RcFloat(1.6f), RcFloat(3.6f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-1f), RcFloat(-1f), RcFloat(-0.6f)),
                        color = 0xFFFFE6C0.toInt(),
                        intensity = RcFloat(1.0f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(1f), RcFloat(-0.2f), RcFloat(0.4f)),
                        color = 0xFF4060FF.toInt(),
                        intensity = RcFloat(0.5f),
                    )

                    // Constant tilt so the top cap is visible, then spin around the axis.
                    rotate(angleRad = 0.5f, ax = 1f, ay = 0f, az = 0f)
                    rotate(angleRad = time, ax = 0f, ay = 1f, az = 0f)
                    drawMesh(cyl, mode = mode)
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun cylinder3DSoftware(): ByteArray = cylinder3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun cylinder3DCanvas(): ByteArray = cylinder3D(RcMeshMode.CanvasSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun Cylinder3DSoftwarePreview() {
    RemoteDocumentPreview(RemoteDocument(cylinder3DSoftware()))
}
