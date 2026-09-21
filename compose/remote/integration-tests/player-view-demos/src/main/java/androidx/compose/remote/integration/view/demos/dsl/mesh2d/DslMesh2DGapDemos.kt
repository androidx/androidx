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
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.cos
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.minus
import androidx.compose.remote.creation.dsl.plus
import androidx.compose.remote.creation.dsl.sin
import androidx.compose.remote.creation.dsl.times
import androidx.compose.remote.creation.json.RemoteComposeJsonParser
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.RcPlatformProfiles

private const val DARK_SLATE = 0xFF1E293B.toInt()
private const val MUTED_SLATE = 0xFF475569.toInt()

/**
 * White-background verification demo for built-in default layout geometry (`x = null`, `y = null`),
 * single-axis override (`x = null` with `y` expression), omitted `alpha` (`alpha = null` defaulting
 * to `1.0f`), and the `MatrixFromMesh2D` degenerate-patch fallback at the `v = 0` apex of a `Fan`.
 *
 * Four quadrants on a white (`0xFFFFFFFF`) canvas:
 * 1. **Top-Left (`Polar`, `x = null, y = null`):** Uses the built-in unit disc `[-1, 1]` via canvas
 *    `translate` + `scale(62f, 62f)` with `alpha` omitted (`1.0f` default).
 * 2. **Top-Right (`Ring`, `x = null, y = null`):** Uses the built-in annulus (`r ∈ [0.5, 1.0]`) via
 *    canvas `translate` + `scale(62f, 62f)`.
 * 3. **Bottom-Left (`Fan`, `x = null, y = null` + Apex `matrixFromMesh2D`):** Samples
 *    `matrixFromMesh2D(fan, 0.25f.rf, 0f.rf, RcMeshMatrix.Full)` right at `v = 0` where all rim
 *    spokes collapse to `(0, 0)` (`|du| < 1e-6`). Because `MatrixFromMesh2D` falls back to unit
 *    basis `(1, 0)` / `(0, 1)`, the dark centre badge draws at full size instead of collapsing
 *    to 0.
 * 4. **Bottom-Right (`Grid`, `x = null`, custom `y`):** Leaves `x = null` (default `u` in `[0, 1]`)
 *    and overrides only `y = v + sin(...) * 0.14f`.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslMesh2DDefaultGeometry(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(
            Header.DOC_CONTENT_DESCRIPTION,
            "Mesh2D default geometry and apex fallback",
        ),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFFFFFFFF.toInt())) {
            val w = componentWidth()
            val h = componentHeight()
            val t = continuousSeconds()

            save {
                scale(w / 400f, h / 400f)

                // 1. Top-Left: Polar with x = null, y = null (built-in unit disc [-1, 1]) and alpha
                // = null
                val defaultPolar =
                    remoteMesh2D(RcMeshLayout.Polar, uCount = 48, vCount = 8) {
                        // x, y, and alpha are intentionally omitted to exercise their built-in
                        // defaults!
                        rgb(
                            red = 0.15f + u * 0.70f,
                            green = 0.35f + (1f.rf - v) * 0.50f,
                            blue = 0.85f - u * 0.45f,
                        )
                    }
                save {
                    translate(102f.rf, 122f.rf)
                    scale(78f.rf, 78f.rf)
                    drawMesh2D(defaultPolar)
                }

                // 2. Top-Right: Ring with x = null, y = null (built-in annulus r in [0.5, 1.0])
                val defaultRing =
                    remoteMesh2D(RcMeshLayout.Ring, uCount = 48, vCount = 6) {
                        rgb(
                            red = 0.90f - v * 0.55f,
                            green = 0.30f + u * 0.55f,
                            blue = 0.25f + v * 0.60f,
                        )
                    }
                save {
                    translate(298f.rf, 122f.rf)
                    scale(78f.rf, 78f.rf)
                    drawMesh2D(defaultRing)
                }

                // 3. Bottom-Left: Fan with x = null, y = null + matrixFromMesh2D at singular v = 0
                // apex
                val defaultFan =
                    remoteMesh2D(RcMeshLayout.Fan, uCount = 36, vCount = 2) {
                        rgb(
                            red = 0.12f + v * 0.15f,
                            green = 0.58f + cos(u * 6.2831855f + t) * 0.25f,
                            blue = 0.48f + v * 0.35f,
                        )
                    }
                save {
                    translate(102f.rf, 298f.rf)
                    scale(78f.rf, 78f.rf)
                    drawMesh2D(defaultFan)

                    // Query matrixFromMesh2D right at the degenerate apex (v = 0), where du = (0,
                    // 0).
                    // The fallback to unit basis (1, 0)/(0, 1) preserves scale so this 0.32-radius
                    // badge draws cleanly at the centre instead of collapsing to zero size.
                    save {
                        matrixFromMesh2D(defaultFan, 0.25f.rf, 0f.rf, RcMeshMatrix.Full)
                        paint {
                            color(DARK_SLATE)
                            style(RcPaintStyle.Fill)
                            antiAlias(true)
                        }
                        drawCircle(0f.rf, 0f.rf, 0.32f.rf)
                        paint { color(0xFFFACC15.toInt()) }
                        drawCircle(0f.rf, 0f.rf, 0.18f.rf)
                    }
                }

                // 4. Bottom-Right: Grid with x = null (defaults to u) and custom y expression
                val halfOverrideGrid =
                    remoteMesh2D(RcMeshLayout.Grid, uCount = 20, vCount = 10) {
                        // x is null -> defaults to u in [0, 1]; y overrides with a sine wave
                        y = v + sin(u * 6.2831855f + t * 2f) * 0.14f
                        rgb(
                            red = 0.38f + u * 0.45f,
                            green = 0.25f + v * 0.45f,
                            blue = 0.85f - v * 0.30f,
                        )
                    }
                save {
                    translate(222f.rf, 232f.rf)
                    scale(152f.rf, 132f.rf)
                    drawMesh2D(halfOverrideGrid)
                }

                // Quadrant captions for quick visual verification
                paint {
                    color(MUTED_SLATE)
                    style(RcPaintStyle.Fill)
                    textSize(13f)
                    antiAlias(true)
                }
                drawTextAnchored(remoteText("Polar (x/y=null)"), 102f.rf, 212f.rf, 0f.rf, 0f.rf, 0)
                drawTextAnchored(remoteText("Ring (x/y=null)"), 298f.rf, 212f.rf, 0f.rf, 0f.rf, 0)
                drawTextAnchored(
                    remoteText("Fan + v=0 Apex Full"),
                    102f.rf,
                    388f.rf,
                    0f.rf,
                    0f.rf,
                    0,
                )
                drawTextAnchored(
                    remoteText("Grid (x=null, y=expr)"),
                    298f.rf,
                    388f.rf,
                    0f.rf,
                    0f.rf,
                    0,
                )
            }

            drawMesh2DLabel("mesh2d Default x/y, Apex fallback", color = DARK_SLATE)
        }
    }
}

/**
 * White-background verification demo for `remoteMesh2DValues` with explicit `uv` coordinates, an
 * `image` texture, and `colors = null` (un-tinted texture sampling) side-by-side with vertex-tinted
 * modulation (`RcMeshBlend.Modulate`).
 */
@Suppress("RestrictedApiAndroidX")
public fun dslMesh2DTexturedValues(): ByteArray {
    val grid = 5
    val leftVerts = FloatArray(grid * grid * 2)
    val rightVerts = FloatArray(grid * grid * 2)
    val uvs = FloatArray(grid * grid * 2)
    val tintColors = IntArray(grid * grid)
    val indices = IntArray((grid - 1) * (grid - 1) * 6)

    for (iy in 0 until grid) {
        val v = iy / (grid - 1f)
        for (ix in 0 until grid) {
            val u = ix / (grid - 1f)
            val idx = iy * grid + ix

            // Gentle pillow pinch so the UV warping on the literal mesh is immediately visible
            val pinch = 1f + 0.14f * kotlin.math.sin(v * Math.PI).toFloat()
            leftVerts[idx * 2] = 16f + (u * 168f) * (0.92f + 0.08f * pinch)
            leftVerts[idx * 2 + 1] = 62f + v * 268f + kotlin.math.sin(u * Math.PI).toFloat() * 18f

            rightVerts[idx * 2] = 208f + (u * 168f) * (0.92f + 0.08f * pinch)
            rightVerts[idx * 2 + 1] = 62f + v * 268f + kotlin.math.sin(u * Math.PI).toFloat() * 18f

            // Explicit literal UV coordinates in [0, 1]
            uvs[idx * 2] = u
            uvs[idx * 2 + 1] = v

            // Warm coral-gold gradient for the right-hand modulated mesh
            val r = (255 - (v * 35).toInt()).coerceIn(0, 255)
            val g = (135 + (u * 105).toInt()).coerceIn(0, 255)
            val b = (110 + ((1f - u) * 90).toInt()).coerceIn(0, 255)
            tintColors[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
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

    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(
            Header.DOC_CONTENT_DESCRIPTION,
            "Mesh2D textured literal values and null colors",
        ),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFFFFFFFF.toInt())) {
            val w = componentWidth()
            val h = componentHeight()

            // Offscreen 128x128 crisp 4x4 checkerboard + bullseye texture
            val checker = createBitmap(128, 128)
            drawOnBitmap(checker, DrawOnBitmapMode.CLEAR, RcColorValue(0xFFF8FAFC.toInt())) {
                paint {
                    style(RcPaintStyle.Fill)
                    color(0xFF1D4ED8.toInt())
                }
                for (ry in 0 until 4) {
                    for (rx in 0 until 4) {
                        if ((rx + ry) % 2 == 0) {
                            drawRect(rx * 32f, ry * 32f, (rx + 1) * 32f, (ry + 1) * 32f)
                        }
                    }
                }
                paint {
                    color(0xFFF59E0B.toInt())
                    antiAlias(true)
                }
                drawCircle(64f, 64f, 26f)
                paint { color(0xFFFFFFFF.toInt()) }
                drawCircle(64f, 64f, 12f)
            }

            // Left mesh: colors = null (uncolored literal mesh sampling texture via literal uv)
            val uncoloredValuesMesh =
                remoteMesh2DValues(
                    verts = leftVerts,
                    indices = indices,
                    uv = uvs,
                    colors = null,
                    halfFloat = false,
                    layout = RcMeshLayout.Grid,
                    uCount = grid,
                    vCount = grid,
                )

            // Right mesh: colors = tintColors (literal mesh multiplying texture by vertex colors)
            val tintedValuesMesh =
                remoteMesh2DValues(
                    verts = rightVerts,
                    indices = indices,
                    uv = uvs,
                    colors = tintColors,
                    halfFloat = false,
                    layout = RcMeshLayout.Grid,
                    uCount = grid,
                    vCount = grid,
                )

            save {
                scale(w / 400f, h / 400f)
                drawMesh2D(uncoloredValuesMesh, image = checker, blend = RcMeshBlend.Modulate)
                drawMesh2D(tintedValuesMesh, image = checker, blend = RcMeshBlend.Modulate)

                paint {
                    color(MUTED_SLATE)
                    style(RcPaintStyle.Fill)
                    textSize(14f)
                    antiAlias(true)
                }
                drawTextAnchored(
                    remoteText("colors = null (pure UV)"),
                    104f.rf,
                    368f.rf,
                    0f.rf,
                    0f.rf,
                    0,
                )
                drawTextAnchored(
                    remoteText("colors != null (Modulate)"),
                    296f.rf,
                    368f.rf,
                    0f.rf,
                    0f.rf,
                    0,
                )
            }

            drawMesh2DLabel("mesh2d Values UV texture, null colors", color = DARK_SLATE)
        }
    }
}

/**
 * White-background verification demo for an invisible `remoteMesh2D` used solely as a 2D
 * coordinate-frame scaffold for `matrixFromMesh2D` (`drawMesh2D` is never called).
 *
 * A 16 × 16 wave mesh defines positions and local tangents across the canvas without allocating or
 * evaluating any color channels, and 20 crisp vector badges are placed and rotated onto the wave
 * via `matrixFromMesh2D(..., RcMeshMatrix.Rotation)`.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslMesh2DInvisibleScaffold(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(
            Header.DOC_CONTENT_DESCRIPTION,
            "Mesh2D invisible matrix scaffold",
        ),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFFFFFFFF.toInt())) {
            val w = componentWidth()
            val h = componentHeight()
            val t = continuousSeconds()

            // Purely geometric mesh: no rgb/alpha assigned, and drawMesh2D(scaffold) is never
            // called!
            val scaffold =
                remoteMesh2D(RcMeshLayout.Grid, uCount = 16, vCount = 16) {
                    val phase = u * 5.2f - t * 2.4f + v * 1.2f
                    x = w * (0.14f + u * 0.72f) + cos(phase) * 8f
                    y = h * (0.20f + v * 0.62f) + sin(phase) * 24f
                }

            // Place a 5 x 4 array of vector cards that ride and tilt with the invisible surface
            for (row in 0 until 4) {
                val vPos = row / 3f
                for (col in 0 until 5) {
                    val uPos = col / 4f
                    save {
                        matrixFromMesh2D(scaffold, uPos.rf, vPos.rf, RcMeshMatrix.Rotation)
                        paint {
                            color(
                                if ((row + col) % 2 == 0) 0xFF2563EB.toInt() else 0xFF0D9488.toInt()
                            )
                            style(RcPaintStyle.Fill)
                            antiAlias(true)
                        }
                        drawRect((-20f).rf, (-11f).rf, 20f.rf, 11f.rf)
                        paint {
                            color(0xFFFFFFFF.toInt())
                            style(RcPaintStyle.Stroke)
                            strokeWidth(2f)
                        }
                        // Tangent indicator line pointing along +du
                        drawLine((-12f).rf, 0f.rf, 12f.rf, 0f.rf)
                        paint {
                            color(0xFFFACC15.toInt())
                            style(RcPaintStyle.Fill)
                        }
                        drawCircle(10f.rf, 0f.rf, 3.5f.rf)
                    }
                }
            }

            paint {
                color(MUTED_SLATE)
                style(RcPaintStyle.Fill)
                textSize(13f)
                antiAlias(true)
            }
            drawTextAnchored(
                remoteText("No drawMesh2D() - 20 cards via matrixFromMesh2D"),
                w * 0.5f,
                h - 26f.rf,
                0f.rf,
                0f.rf,
                0,
            )

            drawMesh2DLabel("mesh2d Invisible mesh, matrix scaffold", color = DARK_SLATE)
        }
    }
}

/**
 * White-background JSON verification demo exercising:
 * 1. `"layout": "ring"` with `"x"` and `"y"` omitted in `RemoteComposeJsonParser` (default annulus)
 * 2. `"source": "f16Values"` (`TYPE_F16_VALUES`) literal vertex + index + color arrays in JSON
 * 3. `"matrixFromMesh2D"` with a dynamic expression (`"fract(time * 0.25)"`) for `"u"` and
 *    `"flags": "scale"`
 */
@Suppress("RestrictedApiAndroidX")
public fun rcJsonMesh2DRingAndF16(): ByteArray {
    val json =
        """
        {
          "header": {
            "width": 400,
            "height": 400,
            "contentDescription": "JSON Mesh2D Ring default and f16 literal values",
            "apiLevel": 7,
            "profiles": 513
          },
          "root": {
            "canvas": {
              "modifiers": [ "fillMaxSize", { "background": "#FFFFFFFF" } ],
              "commands": [
                { "variable": { "name": "w", "value": "width", "commit": true } },
                { "variable": { "name": "h", "value": "height", "commit": true } },
                {
                  "addMesh2D": {
                    "id": "jsonRing",
                    "source": "expression",
                    "layout": "ring",
                    "uCount": 48,
                    "vCount": 6,
                    "red": "0.15 + u * 0.70",
                    "green": "0.35 + v * 0.50",
                    "blue": "0.85 - u * 0.45"
                  }
                },
                {
                  "addMesh2D": {
                    "id": "f16Star",
                    "source": "f16Values",
                    "verts": [
                      298.0, 195.0,
                      298.0, 99.0,
                      392.0, 195.0,
                      298.0, 291.0,
                      204.0, 195.0
                    ],
                    "indices": [
                      0, 1, 2,
                      0, 2, 3,
                      0, 3, 4,
                      0, 4, 1
                    ],
                    "colors": [
                      "#FFFACC15",
                      "#FFEF4444",
                      "#FF3B82F6",
                      "#FF10B981",
                      "#FF8B5CF6"
                    ]
                  }
                },
                {
                  "save": {
                    "commands": [
                      { "scale": { "sx": "@w / 400.0", "sy": "@h / 400.0" } },
                      {
                        "save": {
                          "commands": [
                            { "translate": { "dx": 102.0, "dy": 195.0 } },
                            { "scale": { "sx": 92.0, "sy": 92.0 } },
                            {
                              "drawMesh2D": {
                                "mesh": "jsonRing",
                                "blend": "colorsOnly"
                              }
                            },
                            {
                              "save": {
                                "commands": [
                                  {
                                    "matrixFromMesh2D": {
                                      "mesh": "jsonRing",
                                      "u": "fract(time * 0.25)",
                                      "v": 0.5,
                                      "flags": "scale"
                                    }
                                  },
                                  { "scale": { "sx": 0.11, "sy": 1.0 } },
                                  { "paint": { "color": "#FF1E293B" } },
                                  { "drawCircle": { "cx": 0.0, "cy": 0.0, "radius": 0.28 } },
                                  { "paint": { "color": "#FFFACC15" } },
                                  { "drawCircle": { "cx": 0.0, "cy": 0.0, "radius": 0.14 } }
                                ]
                              }
                            }
                          ]
                        }
                      },
                      {
                        "drawMesh2D": {
                          "mesh": "f16Star",
                          "blend": "colorsOnly"
                        }
                      },
                      { "paint": { "color": "#FF475569", "textSize": 14.0 } },
                      { "drawTextAnchored": { "text": "JSON Ring (default x/y)", "x": 102.0, "y": 318.0, "panX": 0.0, "panY": 0.0, "flags": 0 } },
                      { "drawTextAnchored": { "text": "JSON source: f16Values", "x": 298.0, "y": 318.0, "panX": 0.0, "panY": 0.0, "flags": 0 } },
                      { "paint": { "color": "#FF1E293B", "textSize": 18.0 } },
                      { "drawTextAnchored": { "text": "mesh2d JSON Ring default, f16 Values", "x": 200.0, "y": 24.0, "panX": 0.0, "panY": 0.0, "flags": 0 } }
                    ]
                  }
                }
              ]
            }
          }
        }
        """
            .trimIndent()
    val buffer = RemoteComposeJsonParser.parse(json, AndroidxRcPlatformServices())
    return buffer.array()
}
