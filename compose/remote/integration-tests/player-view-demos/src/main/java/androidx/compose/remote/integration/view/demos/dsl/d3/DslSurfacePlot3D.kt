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

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import androidx.compose.remote.creation.ExperimentalRemoteCreationApi
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcHeaderTag
import androidx.compose.remote.creation.dsl.RcMeshMode
import androidx.compose.remote.creation.dsl.RcPrimitive3D
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.primitivePlane
import androidx.compose.remote.creation.dsl.remoteBitmap
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

private const val HALF_PI = (Math.PI / 2.0).toFloat()
private const val PI = Math.PI.toFloat()

// ---- jet colormap (blue → cyan → green → yellow → red), t in [0,1] -------------------------
private fun clamp01(v: Float) = if (v < 0f) 0f else if (v > 1f) 1f else v

private fun jet(t: Float): Int {
    val r = clamp01(1.5f - Math.abs(4f * t - 3f))
    val g = clamp01(1.5f - Math.abs(4f * t - 2f))
    val b = clamp01(1.5f - Math.abs(4f * t - 1f))
    return (0xFF shl 24) or
        ((r * 255f).toInt() shl 16) or
        ((g * 255f).toInt() shl 8) or
        (b * 255f).toInt()
}

/** Classic "peaks" surface, x,y in [-3,3] — smooth hills and valleys. */
private fun peaks(x: Float, y: Float): Float {
    val e = { a: Float -> Math.exp(a.toDouble()).toFloat() }
    return 3f * (1 - x) * (1 - x) * e(-(x * x) - (y + 1) * (y + 1)) -
        10f * (x / 5f - x * x * x - y * y * y * y * y) * e(-(x * x) - (y * y)) -
        (1f / 3f) * e(-((x + 1) * (x + 1)) - (y * y))
}

// Domain helpers: grid coords (u,v in [0,1]) → peaks input (x,y in [-3,3]).
private fun zRaw(u: Float, v: Float): Float = peaks(-3f + 6f * u, -3f + 6f * v)

// Precompute the surface's min/max so heights normalize to [0,1].
private val zMinMax: Pair<Float, Float> by lazy {
    var lo = Float.MAX_VALUE
    var hi = -Float.MAX_VALUE
    val n = 80
    for (i in 0..n) for (j in 0..n) {
        val z = zRaw(i / n.toFloat(), j / n.toFloat())
        if (z < lo) lo = z
        if (z > hi) hi = z
    }
    lo to hi
}

/** Normalized height [0,1] at grid position (u,v). */
private fun zNorm(u: Float, v: Float): Float {
    val (lo, hi) = zMinMax
    return clamp01((zRaw(u, v) - lo) / (hi - lo))
}

/** A tall jet gradient strip used to color the surface by height (UV.v = normalized height). */
private fun jetGradientBitmap(): Bitmap {
    val w = 8
    val h = 256
    val px = IntArray(w * h)
    for (y in 0 until h) {
        val t = 1f - y / (h - 1f) // top row = high value (red), bottom = low (blue)
        val c = jet(t)
        for (x in 0 until w) px[y * w + x] = c
    }
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bmp.setPixels(px, 0, w, 0, 0, w, h)
    return bmp
}

/** Top-down filled-contour heatmap of the surface (for the floor). */
private fun contourBitmap(): Bitmap {
    val s = 256
    val bands = 14
    val px = IntArray(s * s)
    for (py in 0 until s) {
        for (qx in 0 until s) {
            // Floor texel (qx,py) maps directly to surface grid (u,v) — no flip on either axis
            // (verified with a 4-quadrant test: bitmap top-left → world (-x,-z), bottom-right →
            // (+x,+z)). So the contour sits directly under the matching surface feature.
            val u = qx / (s - 1f)
            val v = py / (s - 1f)
            val z = zNorm(u, v)
            val band = Math.min(bands - 1, (z * bands).toInt())
            // thin dark contour lines between bands
            val frac = z * bands - band
            val line = frac < 0.06f || frac > 0.94f
            val c = if (line) 0xFF3A3A3A.toInt() else jet((band + 0.5f) / bands)
            px[py * s + qx] = c
        }
    }
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    bmp.setPixels(px, 0, s, 0, 0, s, s)
    return bmp
}

/** Opaque light-gray grid panel for the back walls. */
private fun wallGridBitmap(): Bitmap {
    val s = 256
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    c.drawColor(0xFFF3F4F7.toInt())
    val p =
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFC4C7CE.toInt()
            strokeWidth = 2f
            style = AndroidPaint.Style.STROKE
        }
    val div = 6
    for (k in 0..div) {
        val t = s * k / div.toFloat()
        c.drawLine(t, 0f, t, s.toFloat(), p)
        c.drawLine(0f, t, s.toFloat(), t, p)
    }
    return bmp
}

/**
 * Application-style demo: a **3D surface plot** — a height field `z = f(x,y)` colored by height
 * with a jet colormap, inside a gridded axes box, with the contour (top-down heatmap) projected on
 * the floor. Modeled on the classic scientific-plotting surface chart.
 *
 * The surface is a single smooth mesh; each vertex's `UV.v` is its normalized height, so texturing
 * it with a vertical jet gradient colors it by height *and* the smooth lighting shades it (textured
 * Gouraud). The floor/walls are textured [primitivePlane]s (contour heatmap / grid). 2D title and
 * z-axis ticks are overlays.
 */
@Suppress("RestrictedApiAndroidX")
public fun surfacePlot3D(mode: RcMeshMode): ByteArray {
    val nx = 56
    val ny = 56
    val boxH = 1.35f // world height of the box
    val half = 1.1f // world half-extent in x and z

    // Build the surface mesh: verts (x, height, z), finite-difference normals, uv (u, height).
    val nv = nx * ny
    val verts = FloatArray(nv * 3)
    val normals = FloatArray(nv * 3)
    val uv = FloatArray(nv * 2)
    val zg = Array(nx) { i -> FloatArray(ny) { j -> zNorm(i / (nx - 1f), j / (ny - 1f)) } }
    for (i in 0 until nx) {
        for (j in 0 until ny) {
            val idx = i * ny + j
            val wx = -half + 2f * half * i / (nx - 1f)
            val wz = -half + 2f * half * j / (ny - 1f)
            val hy = boxH * zg[i][j]
            verts[idx * 3] = wx
            verts[idx * 3 + 1] = hy
            verts[idx * 3 + 2] = wz
            uv[idx * 2] = i / (nx - 1f)
            uv[idx * 2 + 1] = zg[i][j] // v = normalized height → jet gradient
        }
    }
    // Finite-difference normals (point up: +Y).
    val cell = 2f * half / (nx - 1f)
    for (i in 0 until nx) {
        for (j in 0 until ny) {
            val zL = zg[if (i > 0) i - 1 else i][j] * boxH
            val zR = zg[if (i < nx - 1) i + 1 else i][j] * boxH
            val zD = zg[i][if (j > 0) j - 1 else j] * boxH
            val zU = zg[i][if (j < ny - 1) j + 1 else j] * boxH
            // tangent_i ≈ (cell, dzdi, 0), tangent_j ≈ (0, dzdj, cell); n = tangent_j × tangent_i
            val dzdi = (zR - zL)
            val dzdj = (zU - zD)
            var nxv = dzdi
            var nyv = 2f * cell
            var nzv = dzdj
            val len = Math.sqrt((nxv * nxv + nyv * nyv + nzv * nzv).toDouble()).toFloat()
            val idx = i * ny + j
            normals[idx * 3] = -nxv / len
            normals[idx * 3 + 1] = nyv / len
            normals[idx * 3 + 2] = -nzv / len
        }
    }
    // Indices: two +Y-facing triangles per cell.
    val indices = IntArray((nx - 1) * (ny - 1) * 6)
    var k = 0
    for (i in 0 until nx - 1) {
        for (j in 0 until ny - 1) {
            val a = i * ny + j
            val b = (i + 1) * ny + j
            val c = (i + 1) * ny + (j + 1)
            val d = i * ny + (j + 1)
            indices[k++] = a
            indices[k++] = c
            indices[k++] = b
            indices[k++] = a
            indices[k++] = d
            indices[k++] = c
        }
    }

    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription("3D Surface Plot"),
    ) {
        val jetTex = remoteBitmap(jetGradientBitmap())
        val contourTex = remoteBitmap(contourBitmap())
        val wallTex = remoteBitmap(wallGridBitmap())
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFFFFFFFF.toInt())) {
                paint { color(0xFFFFFFFF.toInt()) }
                // Drag horizontally to spin the plot about its vertical axis, vertically to tilt
                // (pitch) it; flick to coast. Tilt is clamped so it can't flip over.
                val spinY = verticalSpinAngle()
                val tiltX = verticalTiltAngle()
                scene3D {
                    val surface = defineMesh(1, indices, verts, normals, uv)
                    val plane = primitivePlane(2, 1f, 1f, uv = RcPrimitive3D.UV)

                    perspectiveCameraDeg(
                        fovYDeg = 33f,
                        aspect = 1f,
                        near = 0.1f,
                        far = 100f,
                        eye = vec(RcFloat(2.8f), RcFloat(2.2f), RcFloat(3.0f)),
                        center = vec(RcFloat(0f), RcFloat(0.45f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    // Soft, fairly frontal lighting so the colormap reads clearly.
                    directionalLight(
                        direction = vec(RcFloat(-0.3f), RcFloat(-0.55f), RcFloat(-0.75f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(0.75f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(0.5f), RcFloat(-0.2f), RcFloat(0.5f)),
                        color = 0xFFE0E6F0.toInt(),
                        intensity = RcFloat(0.5f),
                    )

                    color(0xFFFFFFFF.toInt())
                    // Floor (contour heatmap), faces +Y at y=0.
                    texture(contourTex)
                    orbitIdentity(spinY, tiltX)
                    rotate(angleRad = -HALF_PI, ax = 1f, ay = 0f, az = 0f)
                    scale(2f * half, 2f * half, 1f)
                    drawMesh(plane, mode = mode)
                    // All four side walls, each facing inward. Backface culling keeps exactly the
                    // two
                    // behind the surface for the current spin (and culls the near two), so the grid
                    // walls stay at the back as the plot rotates.
                    texture(wallTex)
                    val ww = 2f * half
                    orbitWall(
                        plane,
                        mode,
                        spinY,
                        tiltX,
                        0f,
                        boxH * 0.5f,
                        -half,
                        0f,
                        0f,
                        1f,
                        0f,
                        ww,
                        boxH,
                    ) // back +Z
                    orbitWall(
                        plane,
                        mode,
                        spinY,
                        tiltX,
                        0f,
                        boxH * 0.5f,
                        half,
                        PI,
                        0f,
                        1f,
                        0f,
                        ww,
                        boxH,
                    ) // front -Z
                    orbitWall(
                        plane,
                        mode,
                        spinY,
                        tiltX,
                        -half,
                        boxH * 0.5f,
                        0f,
                        HALF_PI,
                        0f,
                        1f,
                        0f,
                        ww,
                        boxH,
                    ) // left +X
                    orbitWall(
                        plane,
                        mode,
                        spinY,
                        tiltX,
                        half,
                        boxH * 0.5f,
                        0f,
                        -HALF_PI,
                        0f,
                        1f,
                        0f,
                        ww,
                        boxH,
                    ) // right -X

                    // The surface, colored by height via the jet gradient.
                    texture(jetTex)
                    orbitIdentity(spinY, tiltX)
                    drawMesh(surface, mode = mode)
                    texture(null)
                }

                // ---- 2D overlays (1000×1000 doc space) ----
                paint {
                    color(0xFF2A2A2A.toInt())
                    textSize(38f)
                }
                drawTextAnchored(remoteText("Surface Plot"), 500f, 70f, 0f, 0f)
                // z-axis ticks down the left edge (approx positions).
                paint {
                    color(0xFF555555.toInt())
                    textSize(24f)
                }
                drawTextAnchored(remoteText("2.0"), 70f, 150f, 1f, 0f)
                drawTextAnchored(remoteText("1.0"), 70f, 360f, 1f, 0f)
                drawTextAnchored(remoteText("0.0"), 70f, 560f, 1f, 0f)
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun surfacePlot3DSoftware(): ByteArray = surfacePlot3D(RcMeshMode.SoftwareSmooth)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun SurfacePlot3DPreview() {
    RemoteDocumentPreview(RemoteDocument(surfacePlot3DSoftware()))
}
