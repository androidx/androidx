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
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.perspectiveCameraDeg
import androidx.compose.remote.creation.dsl.primitiveCube
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

// Heatmap ramp (legend 4.00 → 26.00 °C): blue → pale → cream → yellow → orange → red.
private val HEAT_T = floatArrayOf(4f, 7f, 10f, 13f, 15f, 18f, 21f, 24f, 26f)
private val HEAT_C =
    intArrayOf(
        0xFF1F35E0.toInt(), // blue
        0xFF4F7BF2.toInt(),
        0xFF9AB8F5.toInt(), // light blue
        0xFFCAD6F0.toInt(), // pale lavender
        0xFFF2F0DD.toInt(), // cream
        0xFFF5E84A.toInt(), // yellow
        0xFFF3A52E.toInt(), // orange
        0xFFE5482A.toInt(), // red-orange
        0xFFC81A14.toInt(), // deep red
    )

private fun lerpArgb(a: Int, b: Int, f: Float): Int {
    val ar = (a ushr 16) and 0xFF
    val ag = (a ushr 8) and 0xFF
    val ab = a and 0xFF
    val br = (b ushr 16) and 0xFF
    val bg = (b ushr 8) and 0xFF
    val bb = b and 0xFF
    val r = (ar + (br - ar) * f).toInt()
    val g = (ag + (bg - ag) * f).toInt()
    val bl = (ab + (bb - ab) * f).toInt()
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
}

/** Map a temperature to its heatmap color by interpolating the ramp stops. */
private fun heat(t: Float): Int {
    if (t <= HEAT_T[0]) return HEAT_C[0]
    if (t >= HEAT_T[HEAT_T.size - 1]) return HEAT_C[HEAT_C.size - 1]
    var i = 0
    while (i < HEAT_T.size - 1 && t > HEAT_T[i + 1]) i++
    val f = (t - HEAT_T[i]) / (HEAT_T[i + 1] - HEAT_T[i])
    return lerpArgb(HEAT_C[i], HEAT_C[i + 1], f)
}

/** An opaque [cols]×[rows] grid panel: solid [bg] with thin [line] gridlines (for floor/walls). */
private fun gridPanelBitmap(cols: Int, rows: Int, bg: Int, line: Int): Bitmap {
    val s = 256
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    c.drawColor(bg)
    val p =
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = line
            strokeWidth = 2f
            style = AndroidPaint.Style.STROKE
        }
    for (i in 0..cols) {
        val x = s * i / cols.toFloat()
        c.drawLine(x, 0f, x, s.toFloat(), p)
    }
    for (j in 0..rows) {
        val y = s * j / rows.toFloat()
        c.drawLine(0f, y, s.toFloat(), y, p)
    }
    return bmp
}

/** A vertical heatmap gradient strip for the legend (top = 26°, bottom = 4°). */
private fun legendBitmap(): Bitmap {
    val lw = 16
    val lh = 240
    val px = IntArray(lw * lh)
    for (y in 0 until lh) {
        val frac = (lh - 1 - y) / (lh - 1).toFloat() // 0 bottom .. 1 top
        val col = heat(4f + (26f - 4f) * frac)
        for (x in 0 until lw) px[y * lw + x] = col
    }
    val bmp = Bitmap.createBitmap(lw, lh, Bitmap.Config.ARGB_8888)
    bmp.setPixels(px, 0, lw, 0, 0, lw, lh)
    return bmp
}

/**
 * Application-style demo: a **3D matrix bar chart** — "Seasonal Mean Temperature". A grid of bars
 * (rows = time period along Z, columns = season along X) sits on a green gridded floor between two
 * light back-wall panels; each bar's height is its mean temperature and its color comes from a
 * blue→red heatmap (matching the legend on the right). Modeled on the classic statistical-package
 * 3D bar plot.
 *
 * The 3D scene is the bars + floor + walls (depth-buffered software backend, so the bars occlude
 * each other and the walls correctly). The title, color legend and tick labels are 2D overlays
 * drawn on top of the 3D blit.
 */
@Suppress("RestrictedApiAndroidX")
public fun seasonalBarChart3D(mode: RcMeshMode): ByteArray {
    val periods = arrayOf("1967-76", "1977-86", "1987-96") // rows, back → front
    // Mean temperatures [period][season] for Winter, Spring, Summer, Autumn.
    val temps =
        arrayOf(
            floatArrayOf(5.5f, 13.0f, 23.5f, 14.0f),
            floatArrayOf(6.0f, 15.5f, 24.5f, 12.5f),
            floatArrayOf(4.5f, 20.5f, 25.5f, 9.0f),
        )
    val rows = 3
    val cols = 4
    val pitch = 0.9f
    val barW = 0.5f
    val yScale = 0.108f // temperature → height
    val halfX = cols * pitch * 0.5f // 1.8
    val halfZ = rows * pitch * 0.5f // 1.35
    val wallH = 3.0f

    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.docWidth(1000),
        RcHeaderTag.docHeight(1000),
        RcHeaderTag.contentDescription("Seasonal Mean Temperature"),
    ) {
        val floorTex =
            remoteBitmap(gridPanelBitmap(cols, rows, 0xFFD6EFCF.toInt(), 0xFF8FBE8F.toInt()))
        val backTex = remoteBitmap(gridPanelBitmap(cols, 6, 0xFFF4F4F8.toInt(), 0xFFBFBFC8.toInt()))
        val leftTex = remoteBitmap(gridPanelBitmap(rows, 6, 0xFFF4F4F8.toInt(), 0xFFBFBFC8.toInt()))
        val legendImg = remoteBitmap(legendBitmap())
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFFFFFFFF.toInt())) {
                paint { color(0xFFFFFFFF.toInt()) }
                // Drag horizontally to spin the whole chart about its vertical axis; flick to
                // coast.
                val spinY = verticalSpinAngle()
                scene3D {
                    val cube = primitiveCube(1, 1f, 1f, 1f)
                    val quad = primitivePlane(2, 1f, 1f)

                    perspectiveCameraDeg(
                        fovYDeg = 32f,
                        aspect = 1f,
                        near = 0.1f,
                        far = 100f,
                        eye = vec(RcFloat(5.1f), RcFloat(4.4f), RcFloat(7.9f)),
                        center = vec(RcFloat(0f), RcFloat(0.9f), RcFloat(0f)),
                        up = floatArrayOf(0f, 1f, 0f),
                    )

                    clearDepth()
                    clearLights()
                    directionalLight(
                        direction = vec(RcFloat(-0.3f), RcFloat(-0.72f), RcFloat(-0.55f)),
                        color = 0xFFFFFFFF.toInt(),
                        intensity = RcFloat(0.72f),
                    )
                    directionalLight(
                        direction = vec(RcFloat(0.5f), RcFloat(-0.1f), RcFloat(0.55f)),
                        color = 0xFFDDE4EE.toInt(),
                        intensity = RcFloat(0.42f),
                    )

                    // Floor (green grid, faces +Y).
                    color(0xFFFFFFFF.toInt())
                    texture(floorTex)
                    spunIdentity(spinY)
                    rotate(angleRad = -HALF_PI, ax = 1f, ay = 0f, az = 0f)
                    scale(halfX * 2f, halfZ * 2f, 1f)
                    drawMesh(quad, mode = mode)
                    // All four side walls face inward; backface culling keeps the two behind the
                    // bars for the current spin, so the season/period grids stay at the back as the
                    // chart rotates. Opposite walls share a texture (season grid on ±Z, period grid
                    // on ±X) — they're plain grids, so the mirror on the far wall is invisible.
                    texture(backTex) // seasons × temperature
                    spunWall(
                        quad,
                        mode,
                        spinY,
                        0f,
                        wallH * 0.5f,
                        -halfZ,
                        0f,
                        0f,
                        1f,
                        0f,
                        halfX * 2f,
                        wallH,
                    ) // back +Z
                    spunWall(
                        quad,
                        mode,
                        spinY,
                        0f,
                        wallH * 0.5f,
                        halfZ,
                        PI,
                        0f,
                        1f,
                        0f,
                        halfX * 2f,
                        wallH,
                    ) // front -Z
                    texture(leftTex) // time-period × temperature
                    spunWall(
                        quad,
                        mode,
                        spinY,
                        -halfX,
                        wallH * 0.5f,
                        0f,
                        HALF_PI,
                        0f,
                        1f,
                        0f,
                        halfZ * 2f,
                        wallH,
                    ) // left +X
                    spunWall(
                        quad,
                        mode,
                        spinY,
                        halfX,
                        wallH * 0.5f,
                        0f,
                        -HALF_PI,
                        0f,
                        1f,
                        0f,
                        halfZ * 2f,
                        wallH,
                    ) // right -X

                    // Bars: one heatmap-colored box per (period, season) cell.
                    texture(null)
                    for (r in 0 until rows) {
                        for (cc in 0 until cols) {
                            val t = temps[r][cc]
                            val hgt = t * yScale
                            color(heat(t))
                            spunIdentity(spinY)
                            translate(
                                (cc - (cols - 1) * 0.5f) * pitch,
                                hgt * 0.5f,
                                (r - (rows - 1) * 0.5f) * pitch,
                            )
                            scale(barW, hgt, barW)
                            drawMesh(cube, mode = mode)
                        }
                    }
                }

                // ---- 2D overlays on top of the 3D scene (1000×1000 document space) ----
                // Title (anchor centered: panX = 0 → centered on x).
                paint {
                    color(0xFF2A2A2A.toInt())
                    textSize(40f)
                }
                drawTextAnchored(remoteText("Seasonal Mean Temperature"), 500f, 70f, 0f, 0f)

                // Color legend (gradient strip) on the right + tick labels.
                val lL = 890f
                val lR = 935f
                val lT = 300f
                val lB = 740f
                drawBitmap(legendImg, lL, lT, lR, lB)
                paint {
                    color(0xFF555555.toInt())
                    textSize(26f)
                }
                // panX = -1 → left-anchored (text starts at x, to the right of the strip).
                drawTextAnchored(remoteText("26.0"), lR + 8f, lT, -1f, 0f)
                drawTextAnchored(remoteText("15.0"), lR + 8f, (lT + lB) * 0.5f, -1f, 0f)
                drawTextAnchored(remoteText("4.0"), lR + 8f, lB, -1f, 0f)
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
public fun seasonalBarChart3DSoftware(): ByteArray = seasonalBarChart3D(RcMeshMode.SoftwareFlat)

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
public fun SeasonalBarChart3DPreview() {
    RemoteDocumentPreview(RemoteDocument(seasonalBarChart3DSoftware()))
}
