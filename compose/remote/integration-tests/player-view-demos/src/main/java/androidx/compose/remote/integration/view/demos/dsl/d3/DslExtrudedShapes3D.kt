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
@file:SuppressLint("PrimitiveInCollection")

package androidx.compose.remote.integration.view.demos.dsl.d3

import android.annotation.SuppressLint
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
import androidx.compose.remote.creation.dsl.primitiveExtrudedArc
import androidx.compose.remote.creation.dsl.primitiveExtrudedCircle
import androidx.compose.remote.creation.dsl.primitiveExtrudedRoundedRect
import androidx.compose.remote.creation.dsl.primitiveExtrudedSector
import androidx.compose.remote.creation.dsl.primitiveExtrudedSegment
import androidx.compose.remote.creation.dsl.primitiveExtrudedSquircle
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Gallery of the **extruded 2D-shape** primitives — a flat profile (circle, sector, segment, arc,
 * rounded rect, squircle) in the XY plane swept along Z by a `depth`, giving a prism with two cap
 * faces and a side wall. Each is one `MeshPrimitive` op (geometry in `Primitive3D.extrude*`),
 * tilted so the thickness shows, and auto-spun about Y. Smooth-shaded software backend.
 */
@Suppress("RestrictedApiAndroidX")
public fun extrudedShapes3D(mode: RcMeshMode): ByteArray {
    val colors =
        intArrayOf(
            0xFFE0492E.toInt(), // circle – red
            0xFFE8902E.toInt(), // sector – orange
            0xFFE8C72E.toInt(), // segment – yellow
            0xFF36A89A.toInt(), // arc – teal
            0xFF3D7FE0.toInt(), // rounded rect – blue
            0xFF8E4CE0.toInt(), // squircle – purple
        )
    val d = 0.3f // extrusion depth
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true)) {
        val spin = continuousSeconds() * 0.6f
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFF12151B.toInt())) {
                paint { color(0xFFFFFFFF.toInt()) }
                scene3D {
                    val meshes: Array<RcMesh> =
                        arrayOf(
                            primitiveExtrudedCircle(1, 0.46f, d, segments = 40f),
                            primitiveExtrudedSector(2, 0.52f, 0.35f, 1.7f, d, segments = 20f),
                            primitiveExtrudedSegment(3, 0.55f, 0.3f, 2.0f, d, segments = 20f),
                            primitiveExtrudedArc(4, 0.26f, 0.52f, 0.2f, 2.5f, d, segments = 24f),
                            primitiveExtrudedRoundedRect(5, 0.85f, 0.62f, 0.16f, d, segments = 8f),
                            primitiveExtrudedSquircle(
                                6,
                                0.48f,
                                exponent = 4f,
                                depth = d,
                                segments = 56f,
                            ),
                        )

                    perspectiveCameraDeg(
                        fovYDeg = 34f,
                        aspect = 1f,
                        near = 0.1f,
                        far = 100f,
                        eye = vec(RcFloat(0.8f), RcFloat(1.6f), RcFloat(6.6f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-0.35f), RcFloat(-0.6f), RcFloat(-0.7f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(0.85f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(0.6f), RcFloat(-0.1f), RcFloat(0.4f)),
                        color = 0xFF8090C0.toInt(),
                        intensity = RcFloat(0.4f),
                    )

                    val cols = 3
                    val rows = (meshes.size + cols - 1) / cols
                    val pitch = 1.5f
                    for (i in meshes.indices) {
                        val col = i % cols
                        val row = i / cols
                        val gx = (col - (cols - 1) * 0.5f) * pitch
                        val gy = ((rows - 1) * 0.5f - row) * pitch
                        color(colors[i])
                        identity()
                        translate(gx, gy, 0f)
                        rotate(
                            angleRad = 0.5f,
                            ax = 1f,
                            ay = 0f,
                            az = 0f,
                        ) // tilt to reveal thickness
                        rotate(angleRad = spin, ax = 0f, ay = 1f, az = 0f)
                        drawMesh(meshes[i], mode = mode)
                    }
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun extrudedShapes3DSoftware(): ByteArray = extrudedShapes3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun ExtrudedShapes3DPreview() {
    RemoteDocumentPreview(RemoteDocument(extrudedShapes3DSoftware()))
}
