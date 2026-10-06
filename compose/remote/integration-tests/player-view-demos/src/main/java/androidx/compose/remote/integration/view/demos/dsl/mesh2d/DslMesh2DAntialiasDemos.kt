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

package androidx.compose.remote.integration.view.demos.dsl.mesh2d

import android.annotation.SuppressLint
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.dsl.DrawOnBitmapMode
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcCanvasScope
import androidx.compose.remote.creation.dsl.RcColorValue
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcMesh
import androidx.compose.remote.creation.dsl.RcMeshLayout
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcPath
import androidx.compose.remote.creation.dsl.RcPathType
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.cos
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.div
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.minus
import androidx.compose.remote.creation.dsl.plus
import androidx.compose.remote.creation.dsl.sin
import androidx.compose.remote.creation.dsl.times
import androidx.compose.remote.creation.profile.RcPlatformProfiles

// Side by side demos of the antialiased meshes, remoteMesh2DAntialias and
// remoteMesh2DValuesAntialias.
//
// drawVertices does not antialias, so a mesh's outline is a staircase of fully covered and fully
// empty pixels. The antialiased meshes grow a skirt around the outline whose vertices repeat their
// neighbours' colour at alpha 0, so the edge fades out over about a pixel instead. Each demo here
// builds its shapes twice from the same expressions, plain on the left and antialiased on the
// right, so the skirt is the only difference between the columns.

/** A full turn, in radians. */
private const val TURN = 6.2831855f

/** The size of the offscreen bitmaps [dslMesh2DAntialiasZoom] magnifies, in pixels. */
private const val ZOOM_PIXELS = 40

private val BACKDROP = 0xFF0B1020.toInt()

/**
 * One vertex of a demo shape: its colour, its position or null for the layout's own, and for a
 * [RcMeshLayout.PathStrip] its width.
 */
@Suppress("RestrictedApiAndroidX")
private class ShapeVertex(
    val red: RcFloat,
    val green: RcFloat,
    val blue: RcFloat,
    val x: RcFloat? = null,
    val y: RcFloat? = null,
    val width: RcFloat? = null,
)

/**
 * Build the same parametric shape twice, plain and antialiased.
 *
 * [shape] is called once for each mesh, so each gets expressions of its own.
 */
@Suppress("RestrictedApiAndroidX")
private fun RcScope.plainAndAntialiased(
    layout: RcMeshLayout,
    uCount: Int,
    vCount: Int,
    antialiasWidth: RcFloat,
    path: RcPath? = null,
    shape: (u: RcFloat, v: RcFloat) -> ShapeVertex,
): Pair<RcMesh, RcMesh> {
    val plain =
        remoteMesh2D(layout, uCount, vCount, path) {
            val vertex = shape(u, v)
            x = vertex.x
            y = vertex.y
            width = vertex.width
            rgb(vertex.red, vertex.green, vertex.blue)
        }
    val antialiased =
        remoteMesh2DAntialias(layout, uCount, vCount, antialiasWidth, path) {
            val vertex = shape(u, v)
            x = vertex.x
            y = vertex.y
            width = vertex.width
            color(vertex.red, vertex.green, vertex.blue)
        }
    return plain to antialiased
}

/** "plain" over the left column, "antialias" over the right, and a hairline between them. */
@Suppress("RestrictedApiAndroidX")
private fun RcScope.columnHeaders(y: Float) {
    save()
    scale(componentWidth() / 400f, componentHeight() / 400f)
    paint {
        style(RcPaintStyle.Fill)
        color(0xFF94A3B8.toInt())
        textSize(15f)
        antiAlias(true)
    }
    drawTextAnchored(remoteText("plain"), 100f.rf, y.rf, 0f.rf, 0f.rf, 0)
    drawTextAnchored(remoteText("antialias"), 300f.rf, y.rf, 0f.rf, 0f.rf, 0)
    paint { color(0x33FFFFFF) }
    drawRect(199.5f, y + 12f, 200.5f, 396f)
    restore()
}

/**
 * Draw a pair from [plainAndAntialiased] at height [centerY], the plain mesh centred in the left
 * column and the antialiased one in the right, each turned by [degrees] about its own origin.
 */
@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.drawSideBySide(
    pair: Pair<RcMesh, RcMesh>,
    centerY: RcFloat,
    degrees: RcFloat? = null,
) {
    val w = componentWidth()
    save {
        translate(w * 0.25f, centerY)
        degrees?.let { rotate(it) }
        drawMesh2D(pair.first)
    }
    save {
        translate(w * 0.75f, centerY)
        degrees?.let { rotate(it) }
        drawMesh2D(pair.second)
    }
}

/**
 * Three meshes, plain and antialiased, turning slowly so that the plain edges crawl.
 * - A sixteen point star, a [RcMeshLayout.Fan]: long straight edges at every angle.
 * - A thin ellipse, a [RcMeshLayout.Ring]. A ring has two outlines, so its skirt grows into the
 *   hole as well as out of the rim.
 * - A ribbon along a figure of eight, a [RcMeshLayout.PathStrip], whose width swells and thins
 *   below a pixel. Where it is thinnest the plain ribbon breaks into dashes and the antialiased one
 *   stays a faint, continuous line.
 *
 * Everything is authored in pixels, so a skirt one unit wide is one pixel wide.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslMesh2DAntialiasShapes(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Mesh2D antialias shapes"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(BACKDROP)) {
            val w = componentWidth()
            val h = componentHeight()
            val t = continuousSeconds()
            val onePixel = 1f.rf

            val star =
                plainAndAntialiased(RcMeshLayout.Fan, uCount = 32, vCount = 2, onePixel) { u, v ->
                    // The rim alternates tip, notch, tip: cos(16 * angle) is 1 on the even
                    // vertices and -1 on the odd ones.
                    val tip = 0.5f + cos(u * (TURN * 16f)) * 0.5f
                    val r = v * (w * 0.055f + tip * (w * 0.095f))
                    ShapeVertex(
                        red = 1f.rf,
                        green = 0.9f - v * 0.45f,
                        blue = 0.35f - v * 0.3f,
                        x = cos(u * TURN) * r,
                        y = sin(u * TURN) * r,
                    )
                }

            val ellipse =
                plainAndAntialiased(RcMeshLayout.Ring, uCount = 120, vCount = 2, onePixel) { u, v ->
                    // Three pixels thick, so the stepped edges are most of what there is.
                    ShapeVertex(
                        red = 0.3f.rf,
                        green = 0.75f + cos(u * TURN) * 0.25f,
                        blue = 1f.rf,
                        x = cos(u * TURN) * (w * 0.16f + v * 3f),
                        y = sin(u * TURN) * (w * 0.07f + v * 3f),
                    )
                }

            val orbit =
                remoteXYPath(
                    rFun { s -> sin(s * TURN) * (w * 0.18f) },
                    rFun { s -> sin(s * (TURN * 2f)) * (w * 0.075f) },
                    0f,
                    1f,
                    128,
                    RcPathType.Loop,
                )
            val ribbon =
                plainAndAntialiased(
                    RcMeshLayout.PathStrip,
                    uCount = 256,
                    vCount = 2,
                    onePixel,
                    path = orbit,
                ) { u, _ ->
                    // The path places the ribbon; the mesh only adds a width, 0.6 to 5.6 pixels
                    // in a wave that travels along it, and a hue that is periodic, so the colours
                    // meet where the loop closes.
                    ShapeVertex(
                        red = 0.6f + cos(u * TURN) * 0.4f,
                        green = 0.6f + cos(u * TURN + 2.09f) * 0.4f,
                        blue = 0.6f + cos(u * TURN + 4.19f) * 0.4f,
                        width = 0.6f + (0.5f + sin(u * (TURN * 3f) - t * 1.5f) * 0.5f) * 5f,
                    )
                }

            val spin = t * 4f
            drawSideBySide(star, h * 0.31f, spin)
            drawSideBySide(ellipse, h * 0.63f, -spin)
            drawSideBySide(ribbon, h * 0.89f)

            columnHeaders(y = 52f)
            drawMesh2DLabel("mesh2d antialias: plain vs skirt")
        }
    }
}

/** The arrays of a literal star: a fan, its centre first, then tips and notches alternating. */
private class LiteralStar(val verts: FloatArray, val colors: IntArray, val indices: IntArray)

private fun literalStar(tip: Float, notch: Float, points: Int = 5): LiteralStar {
    val rim = points * 2
    val verts = FloatArray((rim + 1) * 2)
    val colors = IntArray(rim + 1)
    colors[0] = 0xFFE0F2FE.toInt()
    for (i in 0 until rim) {
        val angle = TURN * i / rim
        val radius = if (i % 2 == 0) tip else notch
        verts[(i + 1) * 2] = kotlin.math.cos(angle) * radius
        verts[(i + 1) * 2 + 1] = kotlin.math.sin(angle) * radius
        colors[i + 1] = if (i % 2 == 0) 0xFF38BDF8.toInt() else 0xFF6366F1.toInt()
    }
    // The fan's own triangles: the centre and each rim edge. The antialiased mesh works these out
    // from its layout, but remoteMesh2DValues would otherwise take the vertices in order.
    val indices =
        IntArray(rim * 3) { k ->
            val i = k / 3
            when (k % 3) {
                0 -> 0
                1 -> 1 + i
                else -> 1 + (i + 1) % rim
            }
        }
    return LiteralStar(verts, colors, indices)
}

/**
 * Draw [mesh] into a [ZOOM_PIXELS] square offscreen bitmap, turned by [degrees] about the middle,
 * then blow the bitmap up into the square at [left], [top] without filtering, so that each of its
 * pixels shows as a block.
 */
@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.drawMagnified(
    mesh: RcMesh,
    degrees: RcFloat,
    left: RcFloat,
    top: RcFloat,
    size: RcFloat,
) {
    val image = createBitmap(ZOOM_PIXELS, ZOOM_PIXELS)
    drawOnBitmap(image, DrawOnBitmapMode.CLEAR, RcColorValue(BACKDROP)) {
        save {
            translate(ZOOM_PIXELS / 2f, ZOOM_PIXELS / 2f)
            rotate(degrees)
            drawMesh2D(mesh)
        }
    }
    paint { filterBitmap(false) }
    drawBitmap(image, left, top, left + size, top + size)
}

/**
 * The same comparison under a magnifier.
 *
 * Each panel is a star drawn into a 40 pixel bitmap, which is then drawn ten or so times larger
 * without filtering, so every pixel shows. The plain star's edge is a staircase of pixels that are
 * either the star's colour or the background. The antialiased one has a band a pixel wide of
 * partial alpha between them, and that band is the whole of the skirt.
 *
 * The top row is a parametric mesh, [RcScope.remoteMesh2DAntialias]; the bottom row is the literal
 * form, [RcScope.remoteMesh2DValuesAntialias], with a colour per vertex and a fan's layout.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslMesh2DAntialiasZoom(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Mesh2D antialias zoom"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(BACKDROP)) {
            val w = componentWidth()
            val h = componentHeight()
            val spin = continuousSeconds() * 6f

            // A five point star from expressions, 32 pixels across, which leaves room in the bitmap
            // for the skirt's mitred points to reach about 3 pixels past the star's.
            val star =
                plainAndAntialiased(RcMeshLayout.Fan, uCount = 10, vCount = 2, 1f.rf) { u, v ->
                    val tip = 0.5f + cos(u * (TURN * 5f)) * 0.5f
                    val r = v * (6.5f + tip * 9.5f)
                    ShapeVertex(
                        red = 1f.rf,
                        green = 0.85f - v * 0.4f,
                        blue = 0.3f - v * 0.25f,
                        x = cos(u * TURN) * r,
                        y = sin(u * TURN) * r,
                    )
                }

            // The same star as literal vertices.
            val gem = literalStar(tip = 16f, notch = 6.5f)
            val plainGem =
                remoteMesh2DValues(
                    verts = gem.verts,
                    indices = gem.indices,
                    colors = gem.colors,
                    layout = RcMeshLayout.Fan,
                    uCount = 10,
                    vCount = 2,
                )
            val antialiasedGem =
                remoteMesh2DValuesAntialias(
                    verts = gem.verts,
                    colors = gem.colors,
                    layout = RcMeshLayout.Fan,
                    uCount = 10,
                    vCount = 2,
                    antialiasWidth = 1f.rf,
                )

            val size = w * 0.4f
            val left = w * 0.06f
            val right = w * 0.54f
            val top = h * 0.15f
            val bottom = h * 0.58f
            drawMagnified(star.first, spin, left, top, size)
            drawMagnified(star.second, spin, right, top, size)
            drawMagnified(plainGem, -spin, left, bottom, size)
            drawMagnified(antialiasedGem, -spin, right, bottom, size)

            columnHeaders(y = 46f)
            drawMesh2DLabel("mesh2d antialias, magnified")
        }
    }
}

/**
 * A textured flag waving, plain and antialiased, authored in unit space.
 *
 * The flag's vertices run from 0 to 1 and the canvas scales them up, so a skirt one pixel wide is
 * `1 / scale` in the mesh's own units. That width is an expression over the component's size, which
 * the mesh resolves as a variable. The skirt repeats its neighbours' texture coordinates as well as
 * their colours, so a textured edge fades the same way; the colours still have to be there to fade,
 * and here they carry the lighting, multiplying the texture.
 */
@SuppressLint("SteppedForLoop")
@Suppress("RestrictedApiAndroidX")
public fun dslMesh2DAntialiasTextured(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Mesh2D antialias textured"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(BACKDROP)) {
            val w = componentWidth()
            val h = componentHeight()
            val t = continuousSeconds()

            // A checkerboard, drawn by the document into an offscreen bitmap.
            val checks = createBitmap(64, 64)
            drawOnBitmap(checks, DrawOnBitmapMode.CLEAR, RcColorValue(0xFF1D4ED8.toInt())) {
                paint {
                    style(RcPaintStyle.Fill)
                    color(0xFFFBBF24.toInt())
                }
                for (row in 0 until 8) {
                    for (column in row % 2 until 8 step 2) {
                        drawRect(column * 8f, row * 8f, column * 8f + 8f, row * 8f + 8f)
                    }
                }
            }

            val flagSize = w * 0.4f
            val flags =
                plainAndAntialiased(
                    RcMeshLayout.Grid,
                    uCount = 24,
                    vCount = 12,
                    antialiasWidth = 1f / flagSize,
                ) { u, v ->
                    val phase = u * 6f + t * 2.2f
                    // Lit by the slope of the wave, as the waving flag demo is.
                    val shade = 0.7f + cos(phase) * 0.3f
                    ShapeVertex(
                        red = shade,
                        green = shade,
                        blue = shade,
                        x = u,
                        y = v * 0.7f + sin(phase) * (u * 0.08f),
                    )
                }

            val top = h * 0.5f - flagSize * 0.35f
            save {
                translate(w * 0.05f, top)
                scale(flagSize, flagSize)
                drawMesh2D(flags.first, image = checks)
            }
            save {
                translate(w * 0.55f, top)
                scale(flagSize, flagSize)
                drawMesh2D(flags.second, image = checks)
            }

            columnHeaders(y = 52f)
            drawMesh2DLabel("mesh2d antialias, textured")
        }
    }
}
