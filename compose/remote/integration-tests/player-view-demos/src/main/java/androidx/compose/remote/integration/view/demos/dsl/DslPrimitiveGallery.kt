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

package androidx.compose.remote.integration.view.demos.dsl

import androidx.compose.remote.creation.ExperimentalRemoteCreationApi
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcMeshMode
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.defineCapsuleMesh
import androidx.compose.remote.creation.dsl.defineConeMesh
import androidx.compose.remote.creation.dsl.defineEllipsoidMesh
import androidx.compose.remote.creation.dsl.definePlaneMesh
import androidx.compose.remote.creation.dsl.defineQuadMesh
import androidx.compose.remote.creation.dsl.defineRoundedBoxMesh
import androidx.compose.remote.creation.dsl.defineWedgeMesh
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * One generic demo for the rest of the `Primitive3D` family (cone, capsule, ellipsoid, rounded box,
 * wedge, plane, quad). Defines the [name]d primitive, lights it with a warm key + cool fill, tilts
 * and spins it, and draws it in the requested [mode]. Used both for the recycler entries (via the
 * per-primitive `*Software` wrappers) and for `DocCaptureActivity` (`--es shape <name> --ei mode
 * N`).
 */
@Suppress("RestrictedApiAndroidX")
public fun primitive3D(name: String, mode: RcMeshMode): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true)) {
        val time = continuousSeconds()
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFF101820.toInt())) {
                paint { color(0xFFC08A4A.toInt()) }
                scene3D {
                    val mesh =
                        when (name) {
                            "cone" -> defineConeMesh(id = 1)
                            "capsule" -> defineCapsuleMesh(id = 1)
                            "ellipsoid" -> defineEllipsoidMesh(id = 1)
                            "roundedbox" -> defineRoundedBoxMesh(id = 1)
                            "wedge" -> defineWedgeMesh(id = 1)
                            "plane" -> definePlaneMesh(id = 1, size = 2.6f, segments = 8)
                            "quad" -> defineQuadMesh(id = 1)
                            else -> defineConeMesh(id = 1)
                        }

                    perspectiveCameraDeg(
                        fovYDeg = 45f,
                        aspect = 1f,
                        near = 0.1f,
                        far = 100f,
                        eye = vec(RcFloat(0f), RcFloat(2.0f), RcFloat(3.8f)),
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

                    rotate(angleRad = 0.5f, ax = 1f, ay = 0f, az = 0f)
                    rotate(angleRad = time, ax = 0f, ay = 1f, az = 0f)
                    drawMesh(mesh, mode = mode)
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun cone3DSoftware(): ByteArray = primitive3D("cone", RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun capsule3DSoftware(): ByteArray = primitive3D("capsule", RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun ellipsoid3DSoftware(): ByteArray = primitive3D("ellipsoid", RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun roundedBox3DSoftware(): ByteArray = primitive3D("roundedbox", RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun wedge3DSoftware(): ByteArray = primitive3D("wedge", RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun plane3DSoftware(): ByteArray = primitive3D("plane", RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun quad3DSoftware(): ByteArray = primitive3D("quad", RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun PrimitiveGalleryPreview() {
    RemoteDocumentPreview(RemoteDocument(roundedBox3DSoftware()))
}
