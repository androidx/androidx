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

import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.dsl.DrawOnBitmapMode
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcColorValue
import androidx.compose.remote.creation.dsl.RcMeshBlend
import androidx.compose.remote.creation.dsl.RcMeshLayout
import androidx.compose.remote.creation.dsl.RcMeshMatrix
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.abs
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.cos
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.fraction
import androidx.compose.remote.creation.dsl.lerp
import androidx.compose.remote.creation.dsl.minus
import androidx.compose.remote.creation.dsl.plus
import androidx.compose.remote.creation.dsl.pow
import androidx.compose.remote.creation.dsl.sin
import androidx.compose.remote.creation.dsl.times
import androidx.compose.remote.creation.json.RemoteComposeJsonParser
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.RcPlatformProfiles

private const val TAU2 = 6.2831855f

/**
 * Demos for the corners of the mesh API the first five did not reach.
 *
 * The first batch drifted towards the layouts that are easy to motivate - grids and strips - and
 * left the radial ones, the texture path and the literal-mesh coordinate frame untouched. Each demo
 * here exists to exercise one of those, and the doc comments say which, because a demo whose
 * purpose is coverage should admit it.
 */

/**
 * A rose window: a scalloped stained-glass ring with a conic colour field and jewels set into it.
 *
 * **Exercises [RcMeshLayout.Ring], [RcMesh2DScope.rgb] and [RcMeshMatrix.Scale].**
 *
 * `Ring` is the layout the first batch lost. The ring gauge started on it and had to move to
 * `Strip`, because `Ring` wraps `u` - its last column joins back to its first - and a three-quarter
 * arc does not want to be closed. Here the wrap is the whole point: the twelve scallops and the hue
 * sweep both have to meet themselves at `u = 1` without a seam, and `Strip` could not do that at
 * any resolution. This is the case `Ring` was added for.
 *
 * The jewels are placed with [RcMeshMatrix.Scale] rather than `Rotation`, and that choice is only
 * observable because the band is scalloped: the local frame is wider at a lobe peak than in a
 * trough, so a jewel sized `1` in mesh space comes out larger on the peaks. With a constant-width
 * ring, `Scale` and `Rotation` would be indistinguishable.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslMesh2DRoseWindow(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Mesh2D rose window"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFF0A0710.toInt())) {
            val w = componentWidth()
            val cx = w * 0.5f
            val cy = componentHeight() * 0.52f
            val t = continuousSeconds()

            val window =
                remoteMesh2D(RcMeshLayout.Ring, uCount = 168, vCount = 6) {
                    val theta = u * TAU2

                    // Radius folded into one expression rather than a named inner and outer.
                    // The DSL has no common-subexpression elimination, so every `val` is
                    // re-expanded at each use; spelled as lerp(inner, outer, v) this lands at 31
                    // of the 32 permitted RPN tokens, and folding it gets back to 28.
                    // Inner radius is 0.16*w (64px) and outer scalloped peaks reach 0.43*w (172px),
                    // keeping all 12 lobes and jewels comfortably inside the 400x400 frame.
                    val r = w * (0.16f + v * (0.135f + abs(cos(theta * 6f)) * 0.135f))
                    x = cx + cos(theta) * r
                    y = cy + sin(theta) * r

                    // A conic gradient - hue as a function of angle - which is the one gradient
                    // the paint layer has no shader for. Three phase-shifted cosines stand in for
                    // a hue wheel, and because u wraps, so does the colour.
                    rgb(
                        red = 0.55f + cos(theta * 2f + t * 0.6f) * 0.45f,
                        green = 0.45f + cos(theta * 2f + t * 0.6f + 2.094f) * 0.40f,
                        blue = 0.60f + cos(theta * 2f + t * 0.6f + 4.188f) * 0.40f,
                    )
                    // Leading edges dark, so the glass reads as leaded rather than as a blur.
                    alpha = 0.35f + v * 0.65f
                }
            drawMesh2D(window)

            // 24 jewels alternating between the 12 wide lobe peaks (even k) and 12 narrow troughs
            // (odd k). Because they use RcMeshMatrix.Scale, the jewels on the lobe peaks are
            // automatically scaled larger than the ones in the troughs.
            paint {
                color(0xFFFFF4D6.toInt())
                antiAlias(true)
            }
            for (k in 0 until 24) {
                save {
                    matrixFromMesh2D(window, (k / 24f).rf, 0.68f.rf, RcMeshMatrix.Scale)
                    scale(0.14f.rf, 1f.rf)
                    drawCircle(0f.rf, 0f.rf, 0.065f.rf)
                }
            }

            drawMesh2DLabel("mesh2d Ring wrap, Scale matrix")
        }
    }
}

/**
 * A radar scope: a rotating beam with a phosphor trail that decays behind it.
 *
 * **Exercises [RcMeshLayout.Fan].**
 *
 * `Fan` is the only layout with irregular topology - `vertexCount` is `uCount + 1`, not `uCount *
 * vCount`, because vertex 0 is a single shared centre that every triangle uses. That makes it the
 * natural shape for anything radiating from a point, and it costs 145 vertices here where a polar
 * grid would need 288 for the same rim resolution.
 *
 * Two consequences of the shared centre are worth knowing before reaching for `Fan`:
 * - **`vCount` is ignored.** `v` only ever takes two values: 0 at the centre, 1 on the rim.
 * - **The centre is evaluated once, at `(u = 0, v = 0)`.** Any `u`-dependent expression collapses
 *   to whatever it happens to equal at `u = 0` there. The alpha below is written as `lerp(constant,
 *   beam, v)` precisely so the centre reads the constant and never the beam - otherwise the core of
 *   the scope would flicker each time the sweep crossed angle zero.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslMesh2DRadarSweep(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Mesh2D radar sweep"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFF02100A.toInt())) {
            val w = componentWidth()
            val cx = w * 0.5f
            val cy = componentHeight() * 0.52f
            val t = continuousSeconds()

            val scope =
                remoteMesh2D(RcMeshLayout.Fan, uCount = 144, vCount = 2) {
                    val theta = u * TAU2
                    // v is 0 at the centre and 1 on the rim, so it doubles as the radius.
                    val r = w * 0.41f * v
                    x = cx + cos(theta) * r
                    y = cy + sin(theta) * r

                    // How far behind the sweep this spoke is, as a fraction of a turn in [0, 1).
                    // Adding + 1f before taking `fraction` keeps the argument strictly positive
                    // even when `u < fraction(t * 0.22f)`.
                    val behind = fraction(u - fraction(t * 0.22f) + 1f)
                    val beam = pow(1f.rf - behind, 6f)

                    rgb(
                        red = beam * 0.35f,
                        green = 0.16f + beam * 0.84f,
                        blue = 0.08f + beam * 0.45f,
                    )
                    // See the note above: t = v, so the centre vertex takes the constant.
                    alpha = lerp(0.42f.rf, 0.15f + beam * 0.80f, v)
                }
            drawMesh2D(scope)

            // Range rings, drawn with ordinary paint over the mesh.
            paint {
                color(0x3355FFAA)
                style(RcPaintStyle.Stroke)
                strokeWidth(1.5f)
                antiAlias(true)
            }
            for (k in 1..3) {
                drawCircle(cx, cy, w * 0.41f * (k / 3f))
            }

            // A blip riding the rim, pinned to the mesh at the head of the sweep.
            save {
                matrixFromMesh2D(scope, fraction(t * 0.22f), 1f.rf, RcMeshMatrix.Origin)
                paint {
                    color(0xFFEAFFF2.toInt())
                    antiAlias(true)
                }
                drawCircle(0f.rf, 0f.rf, 4f.rf)
            }

            drawMesh2DLabel("mesh2d Fan apex, rgb() & fraction")
        }
    }
}

/**
 * A silk tapestry with an offscreen mosaic texture warped through animated `(texU, texV)` and lit
 * by wave slope, over a silhouette pass that ignores the texture.
 *
 * **Exercises `drawMesh2D(image = ...)`, [RcMeshBlend.Modulate], [RcMeshBlend.ColorsOnly], and
 * custom [RcMesh2DScope.texU] / [RcMesh2DScope.texV] expressions.**
 *
 * Every earlier demo left `texU` and `texV` null (the identity mapping) and drew without an
 * `RcImage`, which meant the `BitmapShader` path in the backends and both explicit [RcMeshBlend]
 * modes never ran. Here the document first renders a 128x128 geometric medallion into an offscreen
 * bitmap with `createBitmap` + `drawOnBitmap`, then samples it through a rippling cloth whose
 * `(texU, texV)` swirl with time while vertex colours carry the Lambertian wave shade:
 * - The **shadow pass** draws the same mesh with `image = medallion` and `blend =
 *   RcMeshBlend.ColorsOnly`, proving that `ColorsOnly` discards the bound texture and uses only the
 *   vertex colour channel.
 * - The **front pass** draws with `image = medallion` and `blend = RcMeshBlend.Modulate`,
 *   multiplying each texel by the slope-shaded vertex colour.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslMesh2DTexturedTapestry(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Mesh2D textured tapestry"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFF070A14.toInt())) {
            val w = componentWidth()
            val h = componentHeight()
            val t = continuousSeconds()

            // Build a 128x128 geometric tile texture inside the document itself, so the demo is
            // self-contained and works identically on device and in headless unit tests.
            val medallion = createBitmap(128, 128)
            drawOnBitmap(medallion, DrawOnBitmapMode.CLEAR, RcColorValue(0xFF162238.toInt())) {
                paint {
                    style(RcPaintStyle.Fill)
                    color(0xFF2B4C7E.toInt())
                }
                drawRect(8f, 8f, 120f, 120f)
                paint { color(0xFFD9A441.toInt()) }
                drawCircle(64f, 64f, 48f)
                paint { color(0xFF162238.toInt()) }
                drawCircle(64f, 64f, 34f)
                paint { color(0xFFE86F52.toInt()) }
                drawCircle(64f, 64f, 18f)
                paint {
                    color(0xFFF4E8C1.toInt())
                    style(RcPaintStyle.Stroke)
                    strokeWidth(4f)
                }
                drawLine(0f, 64f, 128f, 64f)
                drawLine(64f, 0f, 64f, 128f)
            }

            val tapestry =
                remoteMesh2D(RcMeshLayout.Grid, uCount = 24, vCount = 24) {
                    val phase = u * 5f + v * 3f + t * 2f
                    val fold = sin(phase) * (w * 0.035f)
                    x = w * (0.14f + u * 0.72f) + fold
                    y = h * (0.16f + v * 0.72f) + cos(phase) * (h * 0.035f)

                    // Non-identity UV: a gentle lens swirl that breathes over the medallion.
                    texU = u * 0.88f + 0.06f + sin(v * TAU2 + t * 1.4f) * 0.05f
                    texV = v * 0.88f + 0.06f + cos(u * TAU2 + t * 1.4f) * 0.05f

                    // Slope lighting from cos(phase), warmly tinted across the cloth.
                    val shade = 0.55f + cos(phase) * 0.45f
                    rgb(
                        red = shade * (0.85f + u * 0.15f),
                        green = shade * (0.80f + v * 0.20f),
                        blue = shade,
                    )
                    alpha = 0.92f.rf
                }

            // Pass 1: offset backdrop with ColorsOnly, even though `medallion` is passed. This
            // exercises the explicit ColorsOnly override on a textured draw call.
            save {
                translate(w * 0.03f, h * 0.035f)
                drawMesh2D(tapestry, image = medallion, blend = RcMeshBlend.ColorsOnly)
            }

            // Pass 2: main tapestry with Modulate (texel * vertex colour).
            drawMesh2D(tapestry, image = medallion, blend = RcMeshBlend.Modulate)

            drawMesh2DLabel("mesh2d Image texU/V, Modulate & ColorsOnly")
        }
    }
}

/**
 * A perspective-sheared magic carpet built as an f32 literal mesh (`halfFloat = false`), with a
 * central emblem deformed by [RcMeshMatrix.Full] and four corner tassels using `indices = null`.
 *
 * **Exercises `matrixFromMesh2D` on a literal mesh (`remoteMesh2DValues`), [RcMeshMatrix.Full],
 * `halfFloat = false` (`TYPE_VALUES`), `indices = null`, and `uv = null`.**
 *
 * Every earlier call to `matrixFromMesh2D` targeted a parametric mesh, and every earlier literal
 * mesh (`TerrainData`) used `halfFloat = true` with an explicit index array and never sampled a
 * coordinate frame. Here:
 * 1. The carpet is a 6x6 literal grid in full 32-bit floats (`halfFloat = false`, `uv = null`)
 *    whose rows both foreshorten and **shear** across the canvas, so the local frame at `(0.5,
 *    0.5)` has non-orthogonal axes (`du` and `dv` are not perpendicular).
 * 2. The central emblem is drawn with ordinary `drawCircle` / `drawRect` calls after
 *    `matrixFromMesh2D(carpet, 0.5f.rf, 0.5f.rf, RcMeshMatrix.Full)`, which applies the full 2x3
 *    affine (including shear) so a circle in local space becomes a perspective-tilted ellipse on
 *    the carpet.
 * 3. The four golden corner tassels are drawn with a second `remoteMesh2DValues` call where
 *    `indices = null` and `uv = null` - exercising the "draw the vertices in order as a triangle
 *    list" path.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslMesh2DPerspectiveCarpet(): ByteArray {
    val grid = 6
    val verts = FloatArray(grid * grid * 2)
    val colors = IntArray(grid * grid)
    val indices = IntArray((grid - 1) * (grid - 1) * 6)

    for (iy in 0 until grid) {
        val v = iy / (grid - 1f)
        // Perspective foreshortening + horizontal shear as v goes from top (far) to bottom (near).
        val spanX = 0.44f + v * 0.30f
        val shearX = (v - 0.5f) * 0.12f
        val y = 0.24f + v * 0.56f + kotlin.math.sin(v * Math.PI).toFloat() * 0.04f
        for (ix in 0 until grid) {
            val u = ix / (grid - 1f)
            val idx = iy * grid + ix
            verts[idx * 2] = 0.5f + (u - 0.5f) * spanX + shearX
            verts[idx * 2 + 1] = y

            val checker = if ((ix + iy) % 2 == 0) 1f else 0.72f
            val border = if (ix == 0 || ix == grid - 1 || iy == 0 || iy == grid - 1) 1f else 0f
            val r = ((0.68f * checker + 0.28f * border) * 255f).toInt().coerceIn(0, 255)
            val g = ((0.18f * checker + 0.58f * border) * 255f).toInt().coerceIn(0, 255)
            val b = ((0.32f * checker + 0.15f * border) * 255f).toInt().coerceIn(0, 255)
            colors[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    var k = 0
    for (iy in 0 until grid - 1) {
        for (ix in 0 until grid - 1) {
            val tl = iy * grid + ix
            val tr = tl + 1
            val bl = tl + grid
            val br = bl + 1
            indices[k++] = tl
            indices[k++] = bl
            indices[k++] = tr
            indices[k++] = tr
            indices[k++] = bl
            indices[k++] = br
        }
    }

    // Four triangles (12 vertices) for the corner tassels, in sequential vertex order so
    // `indices = null` draws them directly without an index array.
    val tasselVerts =
        floatArrayOf(
            // top-left tassel
            0.22f,
            0.24f,
            0.15f,
            0.18f,
            0.18f,
            0.27f,
            // top-right tassel
            0.66f,
            0.24f,
            0.73f,
            0.18f,
            0.70f,
            0.27f,
            // bottom-left tassel
            0.19f,
            0.80f,
            0.11f,
            0.87f,
            0.16f,
            0.76f,
            // bottom-right tassel
            0.93f,
            0.80f,
            0.99f,
            0.87f,
            0.88f,
            0.85f,
        )
    val tasselColors = IntArray(12) { if (it % 3 == 1) 0xFFFFE082.toInt() else 0xFFD99B26.toInt() }

    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Mesh2D perspective carpet"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFF090812.toInt())) {
            val w = componentWidth()
            val h = componentHeight()

            val tassels =
                remoteMesh2DValues(
                    verts = tasselVerts,
                    indices = null,
                    uv = null,
                    colors = tasselColors,
                    halfFloat = false,
                )

            val carpet =
                remoteMesh2DValues(
                    verts = verts,
                    indices = indices,
                    uv = null,
                    colors = colors,
                    halfFloat = false,
                    layout = RcMeshLayout.Grid,
                    uCount = grid,
                    vCount = grid,
                )

            save {
                scale(w, h)
                drawMesh2D(tassels)
                drawMesh2D(carpet)

                // Stamp a central medallion using the literal mesh's full 2x3 affine frame
                // (including its horizontal shear and perspective foreshortening).
                save {
                    matrixFromMesh2D(carpet, 0.5f.rf, 0.5f.rf, RcMeshMatrix.Full)
                    paint {
                        color(0xFFF6D365.toInt())
                        antiAlias(true)
                    }
                    drawCircle(0f.rf, 0f.rf, 0.22f.rf)
                    paint { color(0xFF1E152A.toInt()) }
                    drawRect((-0.14f).rf, (-0.14f).rf, 0.14f.rf, 0.14f.rf)
                }
            }

            drawMesh2DLabel("mesh2d f32 Values, Full matrix & null indices")
        }
    }
}

/**
 * JSON-authored Mesh2D waving banner with a surface-pinned label.
 *
 * **Exercises JSON authoring of `addMesh2D`, `drawMesh2D`, and `matrixFromMesh2D` (including the
 * bare `u` and `v` system variables in `ExpressionParser`).**
 */
@Suppress("RestrictedApiAndroidX")
public fun rcJsonMesh2DWavingFlag(): ByteArray {
    val json =
        """
        {
          "header": {
            "width": 400,
            "height": 400,
            "contentDescription": "JSON Mesh2D waving flag",
            "apiLevel": 7,
            "profiles": 513
          },
          "root": {
            "canvas": {
              "modifiers": [ "fillMaxSize", { "background": "#0B1020" } ],
              "commands": [
                { "variable": { "name": "w", "value": "width", "commit": true } },
                { "variable": { "name": "h", "value": "height", "commit": true } },
                {
                  "addMesh2D": {
                    "id": "flag",
                    "source": "expression",
                    "layout": "grid",
                    "uCount": 24,
                    "vCount": 14,
                    "x": "@w * (0.15 + u * 0.70)",
                    "y": "@h * (0.30 + v * 0.44) + sin(u * 6.283 + time * 2.0) * u * @h * 0.08",
                    "alpha": 1.0,
                    "red": "(0.62 + cos(u * 6.283 + time * 2.0) * 0.38) * (0.30 + u * 0.40)",
                    "green": "(0.62 + cos(u * 6.283 + time * 2.0) * 0.38) * (0.50 + v * 0.30)",
                    "blue": "(0.62 + cos(u * 6.283 + time * 2.0) * 0.38) * (0.95 - u * 0.25)"
                  }
                },
                {
                  "drawMesh2D": {
                    "mesh": "flag",
                    "blend": "colorsOnly"
                  }
                },
                {
                  "save": {
                    "commands": [
                      {
                        "matrixFromMesh2D": {
                          "mesh": "flag",
                          "u": 0.5,
                          "v": 0.5,
                          "flags": "rotation"
                        }
                      },
                      { "paint": { "color": "#F2FFFFFF", "textSize": 28.0 } },
                      { "drawTextAnchored": { "text": "JSON Mesh2D", "x": 0.0, "y": 0.0, "panX": 0.0, "panY": 0.0, "flags": 0 } }
                    ]
                  }
                },
                { "paint": { "color": "#FFE2E8F0", "textSize": 18.0 } },
                { "drawTextAnchored": { "text": "mesh2d JSON expression, Rotation matrix", "x": 200.0, "y": 24.0, "panX": 0.0, "panY": 0.0, "flags": 0 } }
              ]
            }
          }
        }
        """
            .trimIndent()
    val buffer = RemoteComposeJsonParser.parse(json, AndroidxRcPlatformServices())
    return buffer.array()
}
