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

package androidx.compose.remote.integration.view.demos.dsl

import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcFontType
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcTileMode
import androidx.compose.remote.creation.dsl.RcWeight
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.cos
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.min
import androidx.compose.remote.creation.dsl.sin
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Showcase for the two-circle ("focal") form of `radialGradient`, which interpolates between a
 * start circle and an end circle instead of a single center + radius.
 *
 * Four quadrants, two of them animated:
 * 1. **Concentric** (static) - same center, non-zero start radius. Produces a flat core that only
 *    starts fading at the start radius, which the single-circle form cannot express.
 * 2. **Offset focal** (static) - zero-radius start circle pushed up and left, the classic lit
 *    sphere / specular highlight.
 * 3. **Orbiting focal** (dynamic) - the start circle center orbits with [continuousSeconds], so the
 *    highlight sweeps around the shape.
 * 4. **Pulsing focal radius + Repeat tiling** (dynamic) - the start radius breathes, and because
 *    the tile mode is [RcTileMode.Repeat] the bands ripple outward.
 *
 * Dynamic values are ordinary [RcFloat] expressions handed to the paint DSL via `toFloat()`, which
 * encodes them as NaN ids, so the gradient geometry is re-evaluated by the player every frame
 * without any recomposition or document rewrite.
 *
 * Requires API 31 on the player; below that the player falls back to the end circle alone.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslRadialGradientFocusDemo(): ByteArray {
    // Hot core -> mid tone -> deep edge, shared by every quadrant so the differences you see come
    // purely from the start/end circle geometry.
    val colors =
        intArrayOf(0xFFFFF3C4.toInt(), 0xFFFF9F1C.toInt(), 0xFFB5179E.toInt(), 0xFF10002B.toInt())
    val stops = floatArrayOf(0f, 0.35f, 0.7f, 1f)

    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Radial Gradient Focus"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFF0B0B10.toInt())) {
            val w = componentWidth()
            val h = componentHeight()
            val density = density()
            val time = continuousSeconds()

            // 2x2 grid of cells, each cell hosts one configuration.
            val cellW = w / 2f
            val cellH = h / 2f
            val radius = min(cellW, cellH) * 0.38f

            // Labels sit under each circle and must not inherit the gradient shader.
            val labelSize = (density * 11f).toFloat()

            for (index in 0..3) {
                val col = index % 2
                val row = index / 2
                val cx = cellW * (col + 0.5f)
                val cy = cellH * (row + 0.5f) - cellH * 0.08f

                when (index) {
                    // 1. Static, concentric circles with a non-zero start radius.
                    0 ->
                        paint {
                            radialGradient(
                                startX = cx.toFloat(),
                                startY = cy.toFloat(),
                                startRadius = (radius * 0.45f).toFloat(),
                                endX = cx.toFloat(),
                                endY = cy.toFloat(),
                                endRadius = radius.toFloat(),
                                colors = colors,
                                positions = stops,
                                tileMode = RcTileMode.Clamp,
                            )
                        }

                    // 2. Static, focal point offset towards the upper left: a lit sphere.
                    1 ->
                        paint {
                            radialGradient(
                                startX = (cx - radius * 0.45f).toFloat(),
                                startY = (cy - radius * 0.45f).toFloat(),
                                startRadius = 0f,
                                endX = cx.toFloat(),
                                endY = cy.toFloat(),
                                endRadius = radius.toFloat(),
                                colors = colors,
                                positions = stops,
                                tileMode = RcTileMode.Clamp,
                            )
                        }

                    // 3. Dynamic: the focal point orbits the center once every ~4 seconds.
                    2 -> {
                        val angle = time * 1.6f
                        val orbit = radius * 0.55f
                        // Hoisted: the paint DSL scope does not expose the RcFloat math helpers.
                        val focalX = (cx + cos(angle) * orbit).toFloat()
                        val focalY = (cy + sin(angle) * orbit).toFloat()
                        paint {
                            radialGradient(
                                startX = focalX,
                                startY = focalY,
                                startRadius = 0f,
                                endX = cx.toFloat(),
                                endY = cy.toFloat(),
                                endRadius = radius.toFloat(),
                                colors = colors,
                                positions = stops,
                                tileMode = RcTileMode.Clamp,
                            )
                        }
                    }

                    // 4. Dynamic: the start radius breathes, Repeat tiling turns it into ripples.
                    else -> {
                        // sin() in [-1,1] -> pulse in [0.05, 0.55] of the end radius.
                        val pulse = radius * ((sin(time * 2.2f) + 1f) * 0.25f + 0.05f)
                        paint {
                            radialGradient(
                                startX = cx.toFloat(),
                                startY = cy.toFloat(),
                                startRadius = pulse.toFloat(),
                                endX = cx.toFloat(),
                                endY = cy.toFloat(),
                                endRadius = (radius * 0.6f).toFloat(),
                                colors = colors,
                                positions = stops,
                                tileMode = RcTileMode.Repeat,
                            )
                        }
                    }
                }

                drawCircle(cx, cy, radius)

                // Drop the gradient before painting the caption, otherwise the text is shaded too.
                paint {
                    raw.setShader(0)
                    color(0xFFE8E8F0.toInt())
                    style(RcPaintStyle.Fill)
                    textSize(labelSize)
                    typeface(RcFontType.Default, RcWeight.Normal, italic = false)
                }
                drawTextAnchored(
                    remoteText(LABELS[index]),
                    cx,
                    cy + radius + (density * 18f),
                    0f.rf,
                    0f.rf,
                    0,
                )
            }
        }
    }
}

private val LABELS =
    arrayOf("concentric startR", "offset focal", "orbiting focal *", "pulsing startR * (repeat)")

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun DslRadialGradientFocusDemoPreview() {
    RemoteDocumentPreview(RemoteDocument(dslRadialGradientFocusDemo()))
}
