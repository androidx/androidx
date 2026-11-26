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
import androidx.compose.remote.creation.dsl.primitiveLathe
import androidx.compose.remote.creation.dsl.primitivePlane
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

private const val HALF_PI = (Math.PI / 2.0).toFloat()

// A turned-vase profile as (radius-from-axis, height) pairs, bottom to top: a flat base that closes
// to the axis, a bulged body, a narrow neck, and a flared lip.
private val VASE_PROFILE =
    floatArrayOf(
        0.00f,
        0.00f, // base center (closes the bottom)
        0.55f,
        0.00f, // base rim
        0.63f,
        0.15f,
        0.80f,
        0.55f, // bulge
        0.84f,
        0.95f, // widest
        0.70f,
        1.35f,
        0.46f,
        1.75f, // neck
        0.40f,
        1.98f,
        0.52f,
        2.18f, // flared lip
    )
private const val VASE_TOP = 2.18f
private const val VASE_COLOR = 0xFF2E86A8.toInt() // glazed teal ceramic

/**
 * Demo for the new **`LATHE` mesh primitive**: a 2D profile revolved around the Y axis. Here the
 * profile is a vase ([VASE_PROFILE]); [primitiveLathe] sweeps it [segments] times around the axis,
 * generating the surface with smooth per-vertex normals. Drag horizontally to spin it on a
 * turntable. The lathe generalizes sphere/cylinder/cone/torus from one command.
 */
@Suppress("RestrictedApiAndroidX")
public fun lathe3D(mode: RcMeshMode): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1080),
        RcHeaderTag.docHeight(1080),
        RcHeaderTag.contentDescription("3D Lathe (vase)"),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFF0E141B.toInt())) {
                paint { color(VASE_COLOR) }
                // Drag horizontally to spin the vase on its vertical axis.
                val spinY = verticalSpinAngle()
                scene3D {
                    val vase = primitiveLathe(1, VASE_PROFILE, segments = 56f)
                    val floor = primitivePlane(2, 8f, 8f)

                    perspectiveCameraDeg(
                        fovYDeg = 34f,
                        aspect = 1f,
                        near = 2.5f,
                        far = 16f,
                        eye = vec(RcFloat(2.4f), RcFloat(1.5f), RcFloat(5.4f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-0.5f), RcFloat(-0.7f), RcFloat(-0.5f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(0.9f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(0.6f), RcFloat(-0.1f), RcFloat(0.5f)),
                        color = 0xFF9DB6E0.toInt(),
                        intensity = RcFloat(0.4f),
                    )
                    // A glossy ceramic highlight.
                    material(specStrength = 0.6f, shininess = 48f)

                    // Floor, just below the vase base (the vase base sits at y = -VASE_TOP/2
                    // below).
                    color(0xFF1A2430.toInt())
                    identity()
                    translate(0f, -VASE_TOP * 0.5f, 0f)
                    rotate(angleRad = -HALF_PI, ax = 1f, ay = 0f, az = 0f)
                    drawMesh(floor, mode = mode)

                    // The vase, centered vertically and spun by horizontal drag.
                    color(VASE_COLOR)
                    spunIdentity(spinY)
                    translate(0f, -VASE_TOP * 0.5f, 0f)
                    drawMesh(vase, mode = mode)
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun lathe3DSoftware(): ByteArray = lathe3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun Lathe3DPreview() {
    RemoteDocumentPreview(RemoteDocument(lathe3DSoftware()))
}
