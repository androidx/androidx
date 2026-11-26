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
import androidx.compose.remote.creation.dsl.primitiveSphere
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * DSL demo exercising **smooth (Gouraud) shading**: a rotating sphere whose per-vertex normals come
 * from [primitiveSphere]. Unlike the cube (no normals → flat per-face), each vertex is shaded by
 * its own normal and the colors are interpolated across each triangle, so the sphere reads as a
 * smooth curved surface rather than a faceted polyhedron.
 *
 * Parameterized by [mode] so the same scene can be rendered through either backend:
 * - [RcMeshMode.SoftwareSmooth] — the Gouraud software rasterizer (per-pixel color interp).
 * - [RcMeshMode.CanvasSmooth] — `Canvas.drawVertices`, which interpolates the per-vertex colors
 *   natively.
 */
@Suppress("RestrictedApiAndroidX")
public fun smoothSphere3D(mode: RcMeshMode): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true)) {
        val time = continuousSeconds()
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFF101820.toInt())) {
                paint { color(0xFFE0552B.toInt()) }

                scene3D {
                    val sphere = primitiveSphere(1, 1f, segments = 32f)

                    perspectiveCameraDeg(
                        fovYDeg = 50f,
                        aspect = 1f,
                        near = 0.1f,
                        far = 100f,
                        eye = vec(RcFloat(0f), RcFloat(0.6f), RcFloat(3.5f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    rotate(angleRad = time, ax = 0f, ay = 1f, az = 0f)
                    drawMesh(sphere, mode = mode)
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun smoothSphere3DSoftware(): ByteArray = smoothSphere3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun smoothSphere3DCanvas(): ByteArray = smoothSphere3D(RcMeshMode.CanvasSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun SmoothSphere3DSoftwarePreview() {
    RemoteDocumentPreview(RemoteDocument(smoothSphere3DSoftware()))
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun SmoothSphere3DCanvasPreview() {
    RemoteDocumentPreview(RemoteDocument(smoothSphere3DCanvas()))
}
