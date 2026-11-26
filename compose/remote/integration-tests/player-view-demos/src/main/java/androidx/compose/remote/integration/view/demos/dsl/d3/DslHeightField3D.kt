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

import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression as Afe
import androidx.compose.remote.creation.ExperimentalRemoteCreationApi
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcMeshMode
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcSurface
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

/**
 * First MeshExpression3D demo: an animated ripple **height field**. The height is one hand-built
 * RPN expression `amp * sin(freq * hypot(x, z) - time)` evaluated over a 64×64 X/Z grid; the `time`
 * reference makes the op re-evaluate and re-upload the mesh every frame, so concentric waves travel
 * outward. Normals are auto-computed (finite differences), so the surface shades correctly as it
 * moves. Drawn through the requested [mode] like any other mesh.
 */
@Suppress("RestrictedApiAndroidX")
public fun heightField3D(mode: RcMeshMode): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true)) {
        val t = floatArrayOf(continuousSeconds().toFloat())
        val freq = 7f
        val amp = 0.22f
        // RPN: amp * sin(freq * hypot(VAR1=x, VAR2=z) - time)
        val height =
            floatArrayOf(Afe.VAR1, Afe.VAR2, Afe.HYPOT, freq, Afe.MUL) +
                t +
                floatArrayOf(Afe.SUB, Afe.SIN, amp, Afe.MUL)

        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFF0E141B.toInt())) {
                paint { color(0xFF38B0C8.toInt()) }
                // Drag horizontally to spin the height field about its vertical axis.
                val spinY = verticalSpinAngle()
                scene3D {
                    // params: xMin,xMax,uCount, zMin,zMax,vCount, yMin,yMax
                    val surface =
                        meshExpression(
                            id = 1,
                            type = RcSurface.HEIGHT_FIELD,
                            params = floatArrayOf(-1.5f, 1.5f, 64f, -1.5f, 1.5f, 64f, -1f, 1f),
                            pos = arrayOf(height),
                        )

                    perspectiveCameraDeg(
                        fovYDeg = 45f,
                        aspect = 1f,
                        near = 0.1f,
                        far = 100f,
                        eye = vec(RcFloat(0f), RcFloat(2.3f), RcFloat(3.0f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-1f), RcFloat(-1.2f), RcFloat(-0.4f)),
                        color = 0xFFFFE6C0.toInt(),
                        intensity = RcFloat(1.0f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(1f), RcFloat(-0.3f), RcFloat(0.6f)),
                        color = 0xFF4060FF.toInt(),
                        intensity = RcFloat(0.5f),
                    )

                    spunIdentity(spinY)
                    drawMesh(surface, mode = mode)
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun heightField3DSoftware(): ByteArray = heightField3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun heightField3DCanvas(): ByteArray = heightField3D(RcMeshMode.CanvasSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun HeightField3DPreview() {
    RemoteDocumentPreview(RemoteDocument(heightField3DSoftware()))
}
