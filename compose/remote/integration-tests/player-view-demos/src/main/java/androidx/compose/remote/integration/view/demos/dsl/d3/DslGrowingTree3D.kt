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
import androidx.compose.remote.creation.dsl.primitiveCylinder
import androidx.compose.remote.creation.dsl.primitiveSphere
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.integration.view.demos.dsl.kit.clamp01
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val BG = 0xFF0E1410.toInt()
private const val WOOD = 0xFF8A5A33.toInt()
private const val WOOD_TIP = 0xFFB07A45.toInt()
private const val LEAF = 0xFF4FAE3C.toInt()

private const val MAX_DEPTH = 5
private const val GROW_DUR = 0.85f // seconds for one segment to extend
private const val CYCLE = 9.0f // grow, hold, then replay

private class Seg(
    val px: Float,
    val py: Float,
    val pz: Float,
    val angle: Float, // axis-angle that aims +Y at the segment direction
    val ax: Float,
    val ay: Float,
    val az: Float,
    val len: Float,
    val rad: Float,
    val birth: Float,
    val depth: Int,
)

private class Leaf(val x: Float, val y: Float, val z: Float, val birth: Float)

/** Build the static branch list once (author time); growth is applied per-segment at draw time. */
private fun buildTree(): Pair<List<Seg>, List<Leaf>> {
    val segs = ArrayList<Seg>()
    val leaves = ArrayList<Leaf>()

    fun emit(
        px: Float,
        py: Float,
        pz: Float,
        dx: Float,
        dy: Float,
        dz: Float,
        len: Float,
        rad: Float,
        birth: Float,
        depth: Int,
    ) {
        // Axis-angle to rotate the +Y unit cylinder onto the direction (dx,dy,dz) (unit).
        val dot = dy.coerceIn(-1f, 1f)
        val angle = acos(dot.toDouble()).toFloat()
        var ax = dz // cross((0,1,0),(dx,dy,dz)) = (dz, 0, -dx)
        var ay = 0f
        var az = -dx
        val al = sqrt(ax * ax + az * az)
        if (al < 1e-4f) {
            ax = 1f
            ay = 0f
            az = 0f // direction parallel to Y: any perpendicular axis
        } else {
            ax /= al
            az /= al
        }
        segs.add(Seg(px, py, pz, angle, ax, ay, az, len, rad, birth, depth))

        val tipX = px + dx * len
        val tipY = py + dy * len
        val tipZ = pz + dz * len
        if (depth >= MAX_DEPTH) {
            leaves.add(Leaf(tipX, tipY, tipZ, birth + GROW_DUR))
            return
        }
        // Build an orthonormal frame (dir, u, w) to spread children around the parent direction.
        var ux = -dy
        var uy = dx
        var uz = 0f
        if (ux * ux + uy * uy + uz * uz < 1e-5f) {
            ux = 1f
            uy = 0f
            uz = 0f
        }
        run {
            val l = sqrt(ux * ux + uy * uy + uz * uz)
            ux /= l
            uy /= l
            uz /= l
        }
        val wx = dy * uz - dz * uy
        val wy = dz * ux - dx * uz
        val wz = dx * uy - dy * ux

        val nChildren = if (depth < 2) 3 else 2
        val tilt = if (depth == 0) 0.42f else 0.62f
        val basePhi = depth * 1.3f // de-align successive levels
        for (c in 0 until nChildren) {
            val phi = basePhi + c * (2.0 * Math.PI / nChildren).toFloat()
            val st = sin(tilt)
            val ct = cos(tilt)
            val cp = cos(phi)
            val sp = sin(phi)
            var ndx = dx * ct + (ux * cp + wx * sp) * st
            var ndy = dy * ct + (uy * cp + wy * sp) * st
            var ndz = dz * ct + (uz * cp + wz * sp) * st
            val nl = sqrt(ndx * ndx + ndy * ndy + ndz * ndz)
            ndx /= nl
            ndy /= nl
            ndz /= nl
            emit(
                tipX,
                tipY,
                tipZ,
                ndx,
                ndy,
                ndz,
                len * 0.74f,
                rad * 0.66f,
                birth + GROW_DUR,
                depth + 1,
            )
        }
    }

    emit(0f, -2.4f, 0f, 0f, 1f, 0f, 2.0f, 0.16f, 0f, 0)
    return segs to leaves
}

/**
 * A **tree that grows branch by branch over time**. The full L-system is built once at author time
 * (a recursive set of branch segments + leaves, each tagged with a `birth` time by depth). On the
 * player, every segment's reveal is `grow = clamp((phase − birth)/GROW_DUR, 0, 1)`, so the trunk
 * extends first, then its children, then theirs — sweeping outward — and the cycle replays.
 *
 * API note worth flagging: to make a branch **extend along its length** you'd want a reactive
 * non-uniform scale (grow Y only). The API only offers a reactive *uniform* [scale], so each
 * segment uses a static non-uniform `scale(rad,len,rad)` for its final shape **times** a reactive
 * uniform `scale(grow)` — the segment scales proportionally from its base joint. A reactive
 * per-axis scale would let the trunk stay full-thickness while telescoping upward.
 */
@Suppress("RestrictedApiAndroidX")
public fun growingTree3D(mode: RcMeshMode): ByteArray {
    val (segs, leaves) = buildTree()

    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription("Growing Tree"),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(BG)) {
                paint { color(0xFFFFFFFF.toInt()) }
                val phase = continuousSeconds() % CYCLE
                // Drag to orbit the tree (horizontal = spin, vertical = tilt).
                val view = verticalSpinAngle()
                val tilt = verticalTiltAngle()

                scene3D {
                    val unitCyl = primitiveCylinder(1, 1f, 0f, 0f, 0f, 0f, 1f, 0f, segments = 7f)
                    val leafBall = primitiveSphere(2, 0.26f, segments = 12f)

                    perspectiveCameraDeg(
                        fovYDeg = 42f,
                        aspect = 1f,
                        near = 0.5f,
                        far = 60f,
                        eye = vec(RcFloat(0f), RcFloat(1.2f), RcFloat(13.5f)),
                        center = vec(RcFloat(0f), RcFloat(1.0f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-0.4f), RcFloat(-0.6f), RcFloat(-0.7f)),
                        color = 0xFFFFF1D8.toInt(),
                        intensity = RcFloat(1.0f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(0.5f), RcFloat(0.4f), RcFloat(0.4f)),
                        color = 0xFF7FA0C0.toInt(),
                        intensity = RcFloat(0.45f),
                    )

                    for (s in segs) {
                        // grow = clamp((phase - birth)/GROW_DUR, 0, 1)
                        val grow: RcFloat = ((phase - s.birth) / GROW_DUR).clamp01()
                        color(if (s.depth >= MAX_DEPTH - 1) WOOD_TIP else WOOD)
                        orbitIdentity(view, tilt)
                        translate(s.px, s.py, s.pz)
                        rotate(s.angle, s.ax, s.ay, s.az)
                        scale(s.rad, s.len, s.rad) // static: final shape
                        scale(grow) // reactive uniform: grows from the joint
                        drawMesh(unitCyl, mode = mode)
                    }

                    color(LEAF)
                    for (lf in leaves) {
                        val grow: RcFloat = ((phase - lf.birth) / GROW_DUR).clamp01()
                        orbitIdentity(view, tilt)
                        translate(lf.x, lf.y, lf.z)
                        scale(grow)
                        drawMesh(leafBall, mode = mode)
                    }
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun growingTree3DSoftware(): ByteArray = growingTree3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
public fun growingTree3DCanvas(): ByteArray = growingTree3D(RcMeshMode.CanvasSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun GrowingTree3DPreview() {
    RemoteDocumentPreview(RemoteDocument(growingTree3DSoftware()))
}
