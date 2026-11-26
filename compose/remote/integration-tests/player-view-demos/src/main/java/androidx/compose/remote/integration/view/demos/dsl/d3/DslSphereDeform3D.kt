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
import androidx.compose.remote.creation.dsl.expressionSphere
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * MeshExpression3D via the **typed** front-end: a pulsing, lumpy sphere built with
 * [expressionSphere]. The radial displacement is written as ordinary [RcFloat][
 * androidx.compose.remote.creation.dsl.RcFloat] math — `0.2 * sin(6u + t) * sin(5v)` — instead of
 * hand-assembled RPN. The captured `time` animates it; normals are auto-computed so the lumps shade
 * correctly as they move.
 */
@Suppress("RestrictedApiAndroidX")
public fun sphereDeform3D(mode: RcMeshMode): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true)) {
        val time = continuousSeconds()
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFF0E141B.toInt())) {
                paint { color(0xFFD9893F.toInt()) }
                scene3D {
                    val blob =
                        expressionSphere(id = 1, radius = 1.0f, uSeg = 96, vSeg = 64) { u, v ->
                            (((u * 6f) + time).sin()) * (v * 5f).sin() * 0.22f
                        }

                    perspectiveCameraDeg(
                        fovYDeg = 45f,
                        aspect = 1f,
                        near = 0.1f,
                        far = 100f,
                        eye = vec(RcFloat(0f), RcFloat(0.7f), RcFloat(4.0f)),
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

                    rotate(angleRad = time, ax = 0f, ay = 1f, az = 0f)
                    drawMesh(blob, mode = mode)
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun sphereDeform3DSoftware(): ByteArray = sphereDeform3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun sphereDeform3DCanvas(): ByteArray = sphereDeform3D(RcMeshMode.CanvasSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun SphereDeform3DPreview() {
    RemoteDocumentPreview(RemoteDocument(sphereDeform3DSoftware()))
}
