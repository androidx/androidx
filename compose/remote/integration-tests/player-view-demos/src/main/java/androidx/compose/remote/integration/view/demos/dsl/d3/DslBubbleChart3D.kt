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

package androidx.compose.remote.integration.view.demos.dsl.d3

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import androidx.compose.remote.creation.ExperimentalRemoteCreationApi
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcMesh
import androidx.compose.remote.creation.dsl.RcMeshMode
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcScene3DScope
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.primitivePlane
import androidx.compose.remote.creation.dsl.primitiveSphere
import androidx.compose.remote.creation.dsl.remoteBitmap
import androidx.compose.remote.creation.dsl.vec
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import java.util.Random

private const val HALF_PI = (Math.PI / 2.0).toFloat()
private const val PI = Math.PI.toFloat()

/** A transparent grid bitmap: faint gridlines + a brighter border, for the data-cube walls. */
private fun gridTextureBitmap(): Bitmap {
    val s = 256
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888) // starts transparent
    val c = AndroidCanvas(bmp)
    val grid =
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = 0x99738098.toInt()
            strokeWidth = 2.5f
            style = AndroidPaint.Style.STROKE
        }
    val border =
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = 0xC0828EA0.toInt()
            strokeWidth = 4f
            style = AndroidPaint.Style.STROKE
        }
    val div = 4
    for (k in 1 until div) {
        val t = s * k / div.toFloat()
        c.drawLine(t, 0f, t, s.toFloat(), grid)
        c.drawLine(0f, t, s.toFloat(), t, grid)
    }
    c.drawRect(2f, 2f, s - 2f, s - 2f, border)
    return bmp
}

/**
 * Application-style demo: a **3D bubble chart** (a 4-dimensional scatter — x, y, z position plus
 * bubble *size* as the 4th variable, colored by category), inspired by Power BI's 3D scatter
 * visual. Colored, Lambert-shaded spheres scatter (clustered toward the origin) inside a data cube;
 * the whole scene spins slowly.
 *
 * The cube's gridlines are **large textured wall quads** (a transparent grid texture) — far cheaper
 * than the dozens of thin "bar" line meshes a naive wireframe needs (thin diagonal triangles are
 * pathological for the bounding-box software rasterizer). All six walls are drawn facing *inward*,
 * so backface culling shows the grid on exactly the three walls behind the data for the current
 * spin (and culls the three near ones). The depth-buffered software backend keeps the overlapping
 * bubbles correctly sorted; transparent texels are cutouts (no depth write), so the grid never
 * hides bubbles.
 */
@Suppress("RestrictedApiAndroidX")
public fun bubbleChart3D(mode: RcMeshMode): ByteArray =
    bubbleChart3D(mode, glossy = false, useLoop = false)

/**
 * The bubble chart with an optional **glossy** finish on the spheres. When [glossy] is set, the
 * bubbles get a Blinn-Phong specular highlight via [RcScene3DScope.material]; because the spheres
 * are smooth-shaded (per-vertex normals), the highlight interpolates into a round reflection spot —
 * the classic shiny-marble look. The grid walls stay matte (the material is set only after they are
 * drawn).
 *
 * [useLoop] selects how the 40 bubbles are emitted: `false` unrolls them at author time (40 baked
 * draw sequences — larger document, but constant transforms render fast); `true` uses a player-side
 * [RcScene3DScope.loop] per color group over uploaded data arrays (tiny document, but each bubble's
 * transform is a per-frame player-evaluated expression, so it renders slower). See both registered
 * demos to A/B.
 */
@JvmOverloads
@Suppress("RestrictedApiAndroidX")
public fun bubbleChart3D(mode: RcMeshMode, glossy: Boolean, useLoop: Boolean = false): ByteArray {
    val rng = Random(7)
    val palette =
        intArrayOf(0xFFE0A92E.toInt(), 0xFFD61F69.toInt(), 0xFFE8472E.toInt(), 0xFFB44CE0.toInt())
    val count = 40
    // Group the bubbles by palette color so each color is ONE player-side loop (the color is set
    // once per group, outside the loop). Same RNG sequence as before → identical bubbles.
    val gx = Array(palette.size) { mutableListOf<Float>() }
    val gy = Array(palette.size) { mutableListOf<Float>() }
    val gz = Array(palette.size) { mutableListOf<Float>() }
    val gr = Array(palette.size) { mutableListOf<Float>() }
    for (i in 0 until count) {
        val x = -0.95f + 1.9f * sq(rng.nextFloat())
        val y = -0.95f + 1.9f * sq(rng.nextFloat())
        val z = -0.95f + 1.9f * sq(rng.nextFloat())
        val r = 0.035f + 0.11f * sq(rng.nextFloat())
        val g = rng.nextInt(palette.size)
        gx[g].add(x)
        gy[g].add(y)
        gz[g].add(z)
        gr[g].add(r)
    }
    fun col(g: Int) = if (gx[g].isEmpty()) floatArrayOf(0f) else gx[g].toFloatArray()

    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true)) {
        val spin = continuousSeconds() * 0.25f
        val gridTex = remoteBitmap(gridTextureBitmap())
        // For the player-side loop, upload each color group's bubble data as float arrays (one set
        // of ops on the wire instead of 40 unrolled draws). Skipped entirely for the unrolled path.
        val arrX = if (useLoop) Array(palette.size) { g -> remoteFloatArray(col(g)) } else null
        val arrY =
            if (useLoop)
                Array(palette.size) { g ->
                    remoteFloatArray(
                        if (gy[g].isEmpty()) floatArrayOf(0f) else gy[g].toFloatArray()
                    )
                }
            else null
        val arrZ =
            if (useLoop)
                Array(palette.size) { g ->
                    remoteFloatArray(
                        if (gz[g].isEmpty()) floatArrayOf(0f) else gz[g].toFloatArray()
                    )
                }
            else null
        val arrR =
            if (useLoop)
                Array(palette.size) { g ->
                    remoteFloatArray(
                        if (gr[g].isEmpty()) floatArrayOf(0f) else gr[g].toFloatArray()
                    )
                }
            else null
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFF0B0D11.toInt())) {
                paint { color(0xFFFFFFFF.toInt()) }
                scene3D {
                    val sphere = primitiveSphere(1, 1f, segments = 14f)
                    val quad = primitivePlane(3, 1f, 1f)

                    perspectiveCameraDeg(
                        fovYDeg = 38f,
                        aspect = 1f,
                        near = 0.1f,
                        far = 100f,
                        eye = vec(RcFloat(2.9f), RcFloat(2.3f), RcFloat(3.6f)),
                        center = vec(RcFloat(0f), RcFloat(-0.1f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-0.5f), RcFloat(-0.8f), RcFloat(-0.6f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(0.95f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(0.7f), RcFloat(0.3f), RcFloat(0.5f)),
                        color = 0xFF6080C0.toInt(),
                        intensity = RcFloat(0.4f),
                    )

                    // ---- Grid: all six walls, each facing *inward* (normal toward the cube
                    // center). Backface culling then shows exactly the three walls behind the
                    // data for the current spin, and culls the three near ones — so the grid
                    // always lands on the back walls as the cube rotates.
                    color(0xFFFFFFFF.toInt()) // white base → texture shown untinted, lit
                    texture(gridTex)
                    wall(quad, mode, spin, 0f, 0f, -1f, 0f, 1f, 0f, 0f) // back  (+Z)
                    wall(quad, mode, spin, 0f, 0f, 1f, PI, 0f, 1f, 0f) // front (-Z)
                    wall(quad, mode, spin, -1f, 0f, 0f, HALF_PI, 0f, 1f, 0f) // left  (+X)
                    wall(quad, mode, spin, 1f, 0f, 0f, -HALF_PI, 0f, 1f, 0f) // right (-X)
                    wall(quad, mode, spin, 0f, -1f, 0f, -HALF_PI, 1f, 0f, 0f) // floor (+Y)
                    wall(quad, mode, spin, 0f, 1f, 0f, HALF_PI, 1f, 0f, 0f) // ceil  (-Y)
                    texture(null)

                    // ---- Bubbles: a colored, sized sphere per data point. Smooth spheres + a
                    // specular material give a round, shiny-marble highlight (walls stay matte).
                    // One player-side `loop` per color group: the iteration runs on the player and
                    // the index drives translate/scale via the uploaded data arrays.
                    if (glossy) {
                        material(specStrength = 0.6f, shininess = 28f)
                    }
                    for (g in palette.indices) {
                        if (gx[g].isEmpty()) continue
                        color(palette[g])
                        if (useLoop) {
                            loop(0f, 1f, gx[g].size.toFloat()) { idx ->
                                identity()
                                rotate(angleRad = spin, ax = 0f, ay = 1f, az = 0f)
                                translate(arrX!![g][idx], arrY!![g][idx], arrZ!![g][idx])
                                scale(arrR!![g][idx])
                                drawMesh(sphere, mode = mode)
                            }
                        } else {
                            for (j in gx[g].indices) {
                                identity()
                                rotate(angleRad = spin, ax = 0f, ay = 1f, az = 0f)
                                translate(gx[g][j], gy[g][j], gz[g][j])
                                scale(gr[g][j])
                                drawMesh(sphere, mode = mode)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draws a 2×2 grid wall quad, spun with the scene. The quad faces +Z by default; (ang, ax,ay,az)
 * rotates it onto the target face so its normal points *inward* (toward the cube center).
 * (tx,ty,tz) places it at that face. Only the inward-facing walls behind the data survive backface
 * culling.
 */
@Suppress("RestrictedApiAndroidX")
private fun RcScene3DScope.wall(
    quad: RcMesh,
    mode: RcMeshMode,
    spin: RcFloat,
    tx: Float,
    ty: Float,
    tz: Float,
    ang: Float,
    ax: Float,
    ay: Float,
    az: Float,
) {
    identity()
    rotate(angleRad = spin, ax = 0f, ay = 1f, az = 0f)
    translate(tx, ty, tz)
    rotate(angleRad = ang, ax = ax, ay = ay, az = az)
    scale(2f, 2f, 1f)
    drawMesh(quad, mode = mode)
}

private fun sq(v: Float): Float = v * v

@Suppress("RestrictedApiAndroidX")
public fun bubbleChart3DSoftware(): ByteArray = bubbleChart3D(RcMeshMode.SoftwareSmooth)

/** Loop variant: the 40 bubbles are drawn by a player-side loop (smaller doc, slower render). */
@Suppress("RestrictedApiAndroidX")
public fun bubbleChart3DLoopSoftware(): ByteArray =
    bubbleChart3D(RcMeshMode.SoftwareSmooth, glossy = false, useLoop = true)

@Suppress("RestrictedApiAndroidX")
public fun bubbleChart3DCanvas(): ByteArray = bubbleChart3D(RcMeshMode.CanvasSmooth)

@Suppress("RestrictedApiAndroidX")
public fun glossyBubbleChart3DSoftware(): ByteArray =
    bubbleChart3D(RcMeshMode.SoftwareSmooth, glossy = true)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun BubbleChart3DPreview() {
    RemoteDocumentPreview(RemoteDocument(bubbleChart3DSoftware()))
}
