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
import androidx.compose.remote.creation.dsl.RcHeaderTag
import androidx.compose.remote.creation.dsl.RcMesh
import androidx.compose.remote.creation.dsl.RcMeshMode
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcScene3DScope
import androidx.compose.remote.creation.dsl.RcSurface
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.primitiveCylinder
import androidx.compose.remote.creation.dsl.primitiveHelix
import androidx.compose.remote.creation.dsl.primitiveLathe
import androidx.compose.remote.creation.dsl.primitiveRoundedCube
import androidx.compose.remote.creation.dsl.primitiveSphere
import androidx.compose.remote.creation.dsl.primitiveTorus
import androidx.compose.remote.creation.dsl.primitiveTube
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/*
 * Wireframe edge-selection modes (a 5-up smoke test). The software hidden-line wireframe normally
 * draws all three edges of every triangle; the new modes draw a subset:
 *
 *   SOFTWARE_WIREFRAME1 — only the FIRST edge of each triangle (v0→v1)
 *   SOFTWARE_WIREFRAME2 — only the SECOND edge (v1→v2)
 *   SOFTWARE_WIREFRAME3 — the first AND second edges
 *   SOFTWARE_WIREFRAME  — all three edges (the classic full wireframe)
 *
 * Each demo lays the same mesh out as a quincunx: the smooth-shaded surface in the MIDDLE, with the
 * four wireframe renderings in the corners. When a generator emits its triangles in a consistent
 * order (e.g. a height-field grid always splits each quad the same way, a sphere's quads run
 * lat×long, a tube's run ring×section), each single-edge family lines up into a regular pattern —
 * so these demos double as a visual check that the triangulation order is consistent. Drag
 * horizontally to spin all five together, vertically to tilt.
 */

private const val BG = 0xFF0E141B.toInt()
private const val ORTHO_R = 3.3f // ortho half-extent (world units mapped to the square viewport)
private const val GRID = 1.62f // corner tile offset from center along X and Y

// Tile order: TL, TR, CENTER, BL, BR.
private val GRID_POS =
    arrayOf(
        floatArrayOf(-GRID, GRID),
        floatArrayOf(GRID, GRID),
        floatArrayOf(0f, 0f),
        floatArrayOf(-GRID, -GRID),
        floatArrayOf(GRID, -GRID),
    )
@Suppress("RestrictedApiAndroidX")
private val GRID_MODES =
    arrayOf(
        RcMeshMode.SoftwareWireframe1,
        RcMeshMode.SoftwareWireframe2,
        RcMeshMode.SoftwareSmooth,
        RcMeshMode.SoftwareWireframe3,
        RcMeshMode.SoftwareWireframe,
    )
// Per-tile line color (center is the smooth fill, set by the caller — its slot here is unused).
private val GRID_WIRE_COLORS =
    intArrayOf(
        0xFF5BE37A.toInt(), // edge 1 — green
        0xFFFFA64D.toInt(), // edge 2 — orange
        0,
        0xFF7FD7FF.toInt(), // edge 1+2 — cyan
        0xFFFFFFFF.toInt(), // all edges — white
    )
private val GRID_LABELS = arrayOf("edge 1", "edge 2", "smooth", "edge 1+2", "all edges")

/** Map a world (x,y) under the [ORTHO_R] camera to a screen pixel in the 1000×1000 doc. */
private fun screenX(wx: Float): Float = (wx + ORTHO_R) / (2f * ORTHO_R) * 1000f

private fun screenY(wy: Float): Float = (ORTHO_R - wy) / (2f * ORTHO_R) * 1000f

/**
 * Draw [mesh] five times in the quincunx layout, one per entry in [GRID_MODES]. The center tile is
 * smooth-shaded in [fill]; the corners are wireframe in their [GRID_WIRE_COLORS]. Each tile is
 * scaled by [scale] and posed by a static ([poseX],[poseY]) rotation plus the touch-driven
 * [spinY]/[tiltX] so all five turn together.
 */
@Suppress("RestrictedApiAndroidX")
private fun RcScene3DScope.drawWireframeGrid(
    mesh: RcMesh,
    scale: Float,
    poseX: Float,
    poseY: Float,
    fill: Int,
    spinY: RcFloat,
    tiltX: RcFloat,
) {
    for (k in 0 until 5) {
        val p = GRID_POS[k]
        color(if (k == 2) fill else GRID_WIRE_COLORS[k])
        identity()
        translate(p[0], p[1], 0f)
        rotate(tiltX, 1f, 0f, 0f) // touch tilt (outermost)
        rotate(spinY, 0f, 1f, 0f) // touch spin
        rotate(poseX, 1f, 0f, 0f) // static pose pitch
        rotate(poseY, 0f, 1f, 0f) // static pose yaw
        scale(scale)
        drawMesh(mesh, mode = GRID_MODES[k])
    }
}

/**
 * Full doc for one subject: the 5-up wireframe-mode grid plus a title and per-tile captions.
 * [buildMesh] is run inside the 3D scene to create the mesh (drawn five times).
 */
@Suppress("RestrictedApiAndroidX")
private fun wireframeModesDoc(
    title: String,
    scale: Float,
    poseX: Float,
    poseY: Float,
    fill: Int,
    buildMesh: RcScene3DScope.(RcFloat) -> RcMesh,
): ByteArray =
    createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription(title),
    ) {
        val timeSec = continuousSeconds()
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(BG)) {
                paint { color(0xFFFFFFFF.toInt()) }
                val spinY = verticalSpinAngle()
                val tiltX = verticalTiltAngle()
                scene3D {
                    val mesh = buildMesh(timeSec)
                    orthoCamera(
                        left = -ORTHO_R,
                        right = ORTHO_R,
                        bottom = -ORTHO_R,
                        top = ORTHO_R,
                        near = -10f,
                        far = 10f,
                        eye = vec(RcFloat(0f), RcFloat(0f), RcFloat(3f)),
                        center = vec(RcFloat(0f), RcFloat(0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )
                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-0.4f), RcFloat(-0.6f), RcFloat(-0.7f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(0.95f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(0.5f), RcFloat(-0.1f), RcFloat(0.4f)),
                        color = 0xFFB9C6E0.toInt(),
                        intensity = RcFloat(0.4f),
                    )
                    drawWireframeGrid(mesh, scale, poseX, poseY, fill, spinY, tiltX)
                }

                // Title.
                paint {
                    color(0xFFEDEFF3.toInt())
                    textSize(36f)
                }
                drawTextAnchored(remoteText(title), 500f, 52f, 0f, 0f)

                // Per-tile captions, just under each tile.
                paint {
                    color(0xFFA8B2C0.toInt())
                    textSize(24f)
                }
                for (k in 0 until 5) {
                    val p = GRID_POS[k]
                    drawTextAnchored(
                        remoteText(GRID_LABELS[k]),
                        screenX(p[0]),
                        screenY(p[1]) + 150f,
                        0f,
                        0f,
                    )
                }
            }
        }
    }

/**
 * 5-up wireframe modes over an animated ripple **height field** (a [RcSurface.HEIGHT_FIELD]
 * MeshExpression3D). The grid splits every quad the same way, so the single-edge families read as
 * clean diagonal / axis-aligned line sets.
 */
@Suppress("RestrictedApiAndroidX")
public fun wireframeModesHeightField3D(): ByteArray {
    val freq = 7f
    val amp = 0.26f
    return wireframeModesDoc(
        title = "Wireframe modes — HeightField",
        scale = 0.82f,
        poseX = 1.0f, // tip the XZ sheet so its top faces the camera (3/4 top view)
        poseY = 0.32f,
        fill = 0xFF38B0C8.toInt(),
    ) { timeSec ->
        val t = floatArrayOf(timeSec.toFloat())
        // RPN: amp * sin(freq * hypot(VAR1=x, VAR2=z) - time)
        val height =
            floatArrayOf(Afe.VAR1, Afe.VAR2, Afe.HYPOT, freq, Afe.MUL) +
                t +
                floatArrayOf(Afe.SUB, Afe.SIN, amp, Afe.MUL)
        meshExpression(
            id = 1,
            type = RcSurface.HEIGHT_FIELD,
            // xMin,xMax,uCount, zMin,zMax,vCount, yMin,yMax (modest grid so the lines are legible)
            params = floatArrayOf(-1.5f, 1.5f, 24f, -1.5f, 1.5f, 24f, -1f, 1f),
            pos = arrayOf(height),
        )
    }
}

/**
 * 5-up wireframe modes over a **sphere** ([primitiveSphere]). Quads run latitude × longitude, so
 * the edge families trace the meridians / parallels / one diagonal of the lat-long grid.
 */
@Suppress("RestrictedApiAndroidX")
public fun wireframeModesSphere3D(): ByteArray =
    wireframeModesDoc(
        title = "Wireframe modes — Sphere",
        scale = 0.62f,
        poseX = -0.35f,
        poseY = 0.5f,
        fill = 0xFFE0552B.toInt(),
    ) {
        primitiveSphere(1, 1f, segments = 18f)
    }

/**
 * 5-up wireframe modes over a curved **tube** ([primitiveTube]). Quads run ring × cross-section, so
 * the edge families trace the rings, the longitudinal seams, and one helical diagonal.
 */
@Suppress("RestrictedApiAndroidX")
public fun wireframeModesTube3D(): ByteArray =
    wireframeModesDoc(
        title = "Wireframe modes — Tube",
        scale = 0.78f,
        poseX = -0.3f,
        poseY = 0.45f,
        fill = 0xFF8E7BE8.toInt(),
    ) {
        primitiveTube(
            id = 1,
            radius = 0.32f,
            points =
                floatArrayOf(
                    -0.9f,
                    -0.95f,
                    0f,
                    -0.35f,
                    -0.1f,
                    0.35f,
                    0.45f,
                    0.45f,
                    -0.2f,
                    0.95f,
                    1.0f,
                    0f,
                ),
            capped = true,
            segments = 10f,
        )
    }

/**
 * 5-up wireframe modes over a **torus** ([primitiveTorus]). Quads run major-ring × tube, so edge 1
 * traces the big ring loops and edge 2 the tube loops.
 */
@Suppress("RestrictedApiAndroidX")
public fun wireframeModesTorus3D(): ByteArray =
    wireframeModesDoc(
        title = "Wireframe modes — Torus",
        scale = 0.92f,
        poseX = -0.62f,
        poseY = 0.4f,
        fill = 0xFFE0A53B.toInt(),
    ) {
        primitiveTorus(1, majorRadius = 0.72f, minorRadius = 0.3f, segments = 20f)
    }

/**
 * 5-up wireframe modes over a **cylinder** ([primitiveCylinder]). Edge 1 = the vertical seams along
 * the axis, edge 2 = the rings around it.
 */
@Suppress("RestrictedApiAndroidX")
public fun wireframeModesCylinder3D(): ByteArray =
    wireframeModesDoc(
        title = "Wireframe modes — Cylinder",
        scale = 0.85f,
        poseX = -0.32f,
        poseY = 0.4f,
        fill = 0xFF4FB0A6.toInt(),
    ) {
        primitiveCylinder(1, radius = 0.55f, 0f, -0.95f, 0f, 0f, 0.95f, 0f, segments = 18f)
    }

/**
 * 5-up wireframe modes over a **lathe**d vase ([primitiveLathe]). Edge 1 = the revolution rings,
 * edge 2 = the profile lines running up the surface.
 */
@Suppress("RestrictedApiAndroidX")
public fun wireframeModesLathe3D(): ByteArray =
    wireframeModesDoc(
        title = "Wireframe modes — Lathe",
        scale = 0.95f,
        poseX = -0.18f,
        poseY = 0.4f,
        fill = 0xFFCC6F8E.toInt(),
    ) {
        // [r,y] profile of a slim vase, revolved around Y.
        primitiveLathe(
            1,
            profile =
                floatArrayOf(
                    0.02f,
                    -0.95f,
                    0.30f,
                    -0.75f,
                    0.42f,
                    -0.35f,
                    0.22f,
                    0.10f,
                    0.30f,
                    0.55f,
                    0.40f,
                    0.95f,
                ),
            segments = 22f,
        )
    }

/**
 * 5-up wireframe modes over a **rounded cube** ([primitiveRoundedCube]). Each of the six rounded
 * faces is a grid; edge 1 and edge 2 are its two axes, edge 1+2 the per-face grid.
 */
@Suppress("RestrictedApiAndroidX")
public fun wireframeModesRoundedCube3D(): ByteArray =
    wireframeModesDoc(
        title = "Wireframe modes — Rounded cube",
        scale = 0.6f,
        poseX = -0.5f,
        poseY = 0.62f,
        fill = 0xFF6F8FE0.toInt(),
    ) {
        primitiveRoundedCube(
            1,
            dimX = 1.4f,
            dimY = 1.4f,
            dimZ = 1.4f,
            radius = 0.35f,
            segments = 6f,
        )
    }

/**
 * 5-up wireframe modes over a **helix** ([primitiveHelix], built on the sweep core). Edge 1 = the
 * longitudinal lines running along the coil, edge 2 = the section rings around the tube.
 */
@Suppress("RestrictedApiAndroidX")
public fun wireframeModesHelix3D(): ByteArray =
    wireframeModesDoc(
        title = "Wireframe modes — Helix",
        scale = 0.85f,
        poseX = -0.32f,
        poseY = 0.4f,
        fill = 0xFFB0B6BE.toInt(),
    ) {
        primitiveHelix(
            1,
            coilRadius = 0.5f,
            tubeRadius = 0.13f,
            pitch = 0.5f,
            turns = 3f,
            y = -0.7f,
            segments = 10f,
        )
    }

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun WireframeModesHeightField3DPreview() {
    RemoteDocumentPreview(RemoteDocument(wireframeModesHeightField3D()))
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun WireframeModesSphere3DPreview() {
    RemoteDocumentPreview(RemoteDocument(wireframeModesSphere3D()))
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun WireframeModesTube3DPreview() {
    RemoteDocumentPreview(RemoteDocument(wireframeModesTube3D()))
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun WireframeModesTorus3DPreview() {
    RemoteDocumentPreview(RemoteDocument(wireframeModesTorus3D()))
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun WireframeModesCylinder3DPreview() {
    RemoteDocumentPreview(RemoteDocument(wireframeModesCylinder3D()))
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun WireframeModesLathe3DPreview() {
    RemoteDocumentPreview(RemoteDocument(wireframeModesLathe3D()))
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun WireframeModesRoundedCube3DPreview() {
    RemoteDocumentPreview(RemoteDocument(wireframeModesRoundedCube3D()))
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun WireframeModesHelix3DPreview() {
    RemoteDocumentPreview(RemoteDocument(wireframeModesHelix3D()))
}
