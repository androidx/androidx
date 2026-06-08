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
import androidx.compose.remote.creation.dsl.primitiveInfinitePlane
import androidx.compose.remote.creation.dsl.primitivePlane
import androidx.compose.remote.creation.dsl.primitiveSphere
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/*
 * A tour of 3D features in one scene:
 *
 *   - an infinite floor (primitiveInfinitePlane) with a grid that fades out toward the horizon
 *   - pushMatrix/popMatrix: a planet carries a moon and a cube; each child is placed relative to
 *     the planet, and popping returns to the planet frame without rebuilding the matrix
 *   - RcMeshMode.Transparent: a translucent glass ball in front of the planet
 *   - RcMeshMode.DoubleSided: a spinning open card whose back face is shaded too
 *   - RcMeshMode.FrontCw: a tetrahedron wound clockwise, drawn the right way round as it is
 *   - ambientLight: a cool fill so the sides the sun does not reach are not black
 */

private const val BG = 0xFF0B1016.toInt()

private const val PLANE_ID = 1
private const val SPHERE_ID = 2
private const val CUBE_ID = 3
private const val CARD_ID = 4
private const val TETRA_ID = 5

/** A regular tetrahedron whose triangles are clockwise as seen from outside. */
private val TETRA_VERTS = floatArrayOf(1f, 1f, 1f, 1f, -1f, -1f, -1f, 1f, -1f, -1f, -1f, 1f)
private val TETRA_CW_INDICES = intArrayOf(0, 2, 1, 0, 1, 3, 0, 3, 2, 1, 2, 3)

/** 3D feature tour: a pushMatrix hierarchy over an infinite grid floor. */
public fun features3D2(): ByteArray =
    createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription("3D features"),
    ) {
        val time = continuousSeconds()
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(BG)) {
                scene3D {
                    val floor =
                        primitiveInfinitePlane(
                            PLANE_ID,
                            y = -1.2f,
                            spacing = 1f,
                            lineWidth = 1.5f,
                            lineColor = 0xA05A7A99.toInt(),
                        )
                    val sphere = primitiveSphere(SPHERE_ID, 1f, segments = 32f)
                    val cube = primitiveCube(CUBE_ID, 1f, 1f, 1f)
                    // A plane is a single face: only DOUBLE_SIDED shades its back.
                    val card = primitivePlane(CARD_ID, 1.6f, 1f)
                    val tetra = defineMesh(TETRA_ID, TETRA_CW_INDICES, TETRA_VERTS)

                    perspectiveCameraDeg(
                        45f,
                        1f,
                        0.5f,
                        100f,
                        vec(RcFloat(0f), RcFloat(2.5f), RcFloat(9f)),
                        vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        floatArrayOf(0f, 1f, 0f),
                    )
                    clearDepth()
                    clearLights()
                    ambientLight(0xFF6070A0.toInt(), 0.35f)
                    directionalLight(floatArrayOf(-0.5f, -0.8f, -0.4f), 0xFFFFF4E0.toInt(), 0.9f)

                    // The floor, drawn first so everything else stands on it.
                    identity()
                    color(0xFF18222C.toInt())
                    drawMesh(floor, RcMeshMode.SoftwareSmooth)

                    // The hierarchy: the whole system turns; the moon and cube ride the planet.
                    identity()
                    rotate(time * 0.3f, 0f, 1f, 0f)
                    pushMatrix {
                        translate(-1.8f, 0f, 0f)
                        color(0xFF3F8CFF.toInt())
                        drawMesh(sphere, RcMeshMode.SoftwareSmooth)
                        pushMatrix {
                            rotate(time * 1.5f, 0f, 1f, 0f)
                            translate(1.6f, 0.3f, 0f)
                            scale(0.3f)
                            color(0xFFE8E2D0.toInt())
                            drawMesh(sphere, RcMeshMode.SoftwareSmooth)
                        }
                        // Back in the planet frame: the cube sits on top of the planet.
                        translate(0f, 1.4f, 0f)
                        rotate(time, 0.7071f, 0.7071f, 0f)
                        scale(0.45f)
                        color(0xFFFFB347.toInt())
                        drawMesh(cube, RcMeshMode.SoftwareFlat)
                    }

                    // An open card, both sides lit.
                    pushMatrix {
                        translate(2f, 0.2f, 0f)
                        rotate(time * 0.8f, 0f, 1f, 0f)
                        color(0xFFE0506E.toInt())
                        drawMesh(card, RcMeshMode.SoftwareSmooth or RcMeshMode.DoubleSided)
                    }

                    // Clockwise triangles: FRONT_CW makes them the front faces, so the
                    // tetrahedron is culled and lit the right way round without new indices.
                    identity()
                    translate(-2.4f, 2f, 1f)
                    rotate(time * 0.9f, 0.6f, 0.8f, 0f)
                    scale(0.3f)
                    color(0xFF7CD992.toInt())
                    drawMesh(tetra, RcMeshMode.SoftwareFlat or RcMeshMode.FrontCw)

                    // The translucent glass ball, drawn last so what is behind it shows through.
                    identity()
                    translate(0f, -0.2f, 2f)
                    scale(0.8f)
                    color(0x7099E6FF)
                    drawMesh(sphere, RcMeshMode.SoftwareSmooth or RcMeshMode.Transparent)
                }

                paint {
                    color(0xFFEDEFF3.toInt())
                    textSize(36f)
                }
                drawTextAnchored(remoteText("3D features"), 500f, 52f, 0f, 0f)
                paint {
                    color(0xFFA8B2C0.toInt())
                    textSize(22f)
                }
                drawTextAnchored(
                    remoteText(
                        "infinite grid · push/pop · transparent · double-sided · front-cw · ambient"
                    ),
                    500f,
                    950f,
                    0f,
                    0f,
                )
            }
        }
    }

@Preview
@Composable
private fun Features3D2Preview() {
    RemoteDocumentPreview(RemoteDocument(features3D2()))
}
