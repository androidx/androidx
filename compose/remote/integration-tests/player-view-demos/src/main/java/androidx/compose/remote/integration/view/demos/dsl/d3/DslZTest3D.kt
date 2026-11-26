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

/**
 * Minimal, unambiguous depth test: a **near red** sphere on the left overlapping a **far blue**
 * sphere on the right, head-on camera, static (no rotation). Red is translated toward the camera
 * (+Z), blue away (−Z); they overlap in the middle.
 *
 * Expected with a correct depth buffer ([RcMeshMode.SoftwareSmooth]): in the overlap region the red
 * sphere fully occludes the blue one (red is nearer). If blue bleeds through red, the depth test is
 * broken. The canvas path ([RcMeshMode.CanvasSmooth]) draws blue second with no shared depth
 * buffer, so blue paints over red regardless.
 */
@Suppress("RestrictedApiAndroidX")
public fun zTest3D(mode: RcMeshMode): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFF101820.toInt())) {
                var sphere: RcMesh? = null
                // Drag horizontally to spin the pair about the vertical axis — rotating swaps which
                // sphere is nearer, exercising the depth test interactively.
                val spinY = verticalSpinAngle()

                // Pass 1 — NEAR red sphere (translated toward the camera).
                paint { color(0xFFE2403A.toInt()) }
                scene3D {
                    sphere = primitiveSphere(1, 0.9f, segments = 32f)
                    perspectiveCameraDeg(
                        fovYDeg = 45f,
                        aspect = 1f,
                        near = 0.1f,
                        far = 100f,
                        eye = vec(RcFloat(0f), RcFloat(0f), RcFloat(4f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )
                    clearDepth()
                    spunIdentity(spinY)
                    translate(x = -0.5f, y = 0f, z = 0.8f)
                    drawMesh(sphere!!, mode = mode)
                }

                // Pass 2 — FAR blue sphere (translated away). Same camera, no clearDepth so the
                // software z-buffer carries over.
                paint { color(0xFF3A6CE2.toInt()) }
                scene3D {
                    perspectiveCameraDeg(
                        fovYDeg = 45f,
                        aspect = 1f,
                        near = 0.1f,
                        far = 100f,
                        eye = vec(RcFloat(0f), RcFloat(0f), RcFloat(4f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )
                    spunIdentity(spinY)
                    translate(x = 0.5f, y = 0f, z = -0.8f)
                    drawMesh(sphere!!, mode = mode)
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun ZTest3DSoftwarePreview() {
    RemoteDocumentPreview(RemoteDocument(zTest3D(RcMeshMode.SoftwareSmooth)))
}
