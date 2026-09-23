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
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcPathType
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.plus
import androidx.compose.remote.creation.dsl.sin
import androidx.compose.remote.creation.dsl.times
import androidx.compose.remote.creation.profile.RcPlatformProfiles

private const val SPLINE_TAU = 6.2831855f
private const val INK = 0xFF1E293B.toInt()
private const val HINT = 0xFFB6C2D4.toInt()

/**
 * A gentle S laid across the canvas for a ribbon to follow.
 *
 * @param y where the wave is centred, in the 400x400 design space
 */
@Suppress("RestrictedApiAndroidX")
private fun RcScope.waveAt(y: Float) =
    remoteXYPath(
        rFun { s -> s * 320f + 40f },
        rFun { s -> sin(s * SPLINE_TAU) * 22f + y },
        0f,
        1f,
        64,
        RcPathType.Spline,
    )

/**
 * Three strokes along the same shape of path, differing only in their width profile.
 *
 * A stroked path has one width for its whole length, so a stroke that swells and tapers has to be
 * built by hand out of quads, or faked with a stack of overlapping strokes. Here the profile is
 * just the numbers it sounds like: `widths` are the control points and the monotonic spline fills
 * in between them, so a calligraphic stroke is three floats.
 *
 * Top to bottom:
 * 1. **Calligraphic** - `[0, 18, 3]` with no positions, so the widths spread evenly: nothing at the
 *    start, fattest in the middle, thin at the finish.
 * 2. **Lifting brush** - `[22, 1]`, a plain taper from one end to the other.
 * 3. **Placed control points** - the same idea but with explicit `positions`, which is the only way
 *    to put the fat part somewhere other than where even spacing would leave it.
 *
 * The faint line under each ribbon is the path itself, drawn with a hairline stroke, so the ribbon
 * can be seen to be centred on it.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslMesh2DSplineBrush(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Mesh2D spline width path strip"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFFFFFFFF.toInt())) {
            val w = componentWidth()
            val h = componentHeight()

            save {
                scale(w / 400f, h / 400f)

                val calligraphic = waveAt(110f)
                val lifting = waveAt(210f)
                val placed = waveAt(310f)

                // The paths themselves, so the ribbons can be seen to follow them.
                paint {
                    style(RcPaintStyle.Stroke)
                    strokeWidth(1f)
                    color(HINT)
                    antiAlias(true)
                }
                drawPath(calligraphic)
                drawPath(lifting)
                drawPath(placed)

                // A spline strip carries no vertex colours, so the paint supplies the colour.
                paint {
                    style(RcPaintStyle.Fill)
                    color(INK)
                    antiAlias(true)
                }

                // Evenly spaced: first width at the start of the path, last at the end.
                drawMesh2D(
                    remoteMesh2DPathStrip(
                        calligraphic,
                        segments = 64,
                        widths = floatArrayOf(0f, 18f, 3f),
                    )
                )

                // Two control points is a plain taper.
                drawMesh2D(
                    remoteMesh2DPathStrip(lifting, segments = 64, widths = floatArrayOf(22f, 1f))
                )

                // Explicit positions: the swell sits at 15% and again at 80%, which even spacing
                // could not place.
                drawMesh2D(
                    remoteMesh2DPathStrip(
                        placed,
                        segments = 64,
                        widths = floatArrayOf(2f, 20f, 2f, 14f, 1f),
                        positions = floatArrayOf(0f, 0.15f, 0.5f, 0.8f, 1f),
                    )
                )

                drawMesh2DLabel("mesh2d PathStrip, spline widths", color = INK)
            }
        }
    }
}

/**
 * The same ribbon with its width profile driven by time.
 *
 * The control points are remote floats rather than constants, so the spline is refitted every frame
 * the values change and the ribbon breathes. Nothing about the document grows: it is still one mesh
 * and a handful of numbers, and the animation happens entirely on the player.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslMesh2DSplineBreathingRibbon(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Mesh2D animated spline widths"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFFFFFFFF.toInt())) {
            val w = componentWidth()
            val h = componentHeight()
            val t = continuousSeconds()

            save {
                scale(w / 400f, h / 400f)

                val spine = waveAt(200f)

                paint {
                    style(RcPaintStyle.Stroke)
                    strokeWidth(1f)
                    color(HINT)
                    antiAlias(true)
                }
                drawPath(spine)

                paint {
                    style(RcPaintStyle.Fill)
                    color(INK)
                    antiAlias(true)
                }

                // Three control points, the middle one swinging between thin and fat. The two ends
                // swing in antiphase, so the ribbon does not simply scale - the shape of the
                // profile changes, which is what the spline buys over a single width.
                drawMesh2D(
                    remoteMesh2DPathStrip(
                        spine,
                        segments = 64,
                        widths =
                            arrayOf(
                                sin(t * 1.7f) * 5f + 6f,
                                sin(t * 1.1f) * 14f + 18f,
                                sin(t * 1.7f + 3.14f) * 5f + 6f,
                            ),
                    )
                )

                drawMesh2DLabel("mesh2d PathStrip, animated widths", color = INK)
            }
        }
    }
}

/** A caption above one of the ribbons, in the same 400x400 design space. */
@Suppress("RestrictedApiAndroidX")
private fun RcScope.rowLabel(text: String, y: Float) {
    paint {
        style(RcPaintStyle.Fill)
        color(HINT)
        textSize(14f)
        antiAlias(true)
    }
    drawTextAnchored(remoteText(text), 200f.rf, y.rf, 0f.rf, 0f.rf, 0)
}

/**
 * The same width profile with square ends and with round ends.
 *
 * A spline strip stops dead where its path does, leaving a blunt edge cut across the ribbon. That
 * is right for something butted against other geometry and wrong for anything meant to look drawn,
 * which is what `RoundStrip` fixes: each end closes with a semicircle whose radius is half the
 * ribbon's width there.
 *
 * Top to bottom:
 * 1. **PathStrip, constant width** - the ends are flat, and they stop exactly on the hairline path.
 * 2. **RoundStrip, same width** - the ends dome out past the path by half the width. The hairline
 *    underneath is the same length in both, so the overhang is the cap and nothing else.
 * 3. **RoundStrip, tapering to nothing** - the cap radius follows the profile, so a width of zero
 *    at the ends leaves no dome to draw and the ribbon comes to a needle point instead.
 *
 * The caps cost extra columns rather than borrowing them, so all three follow their path at
 * identical resolution.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslMesh2DRoundStrip(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Mesh2D round capped path strip"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFFFFFFFF.toInt())) {
            val w = componentWidth()
            val h = componentHeight()

            save {
                scale(w / 400f, h / 400f)

                val squared = waveAt(130f)
                val domed = waveAt(240f)
                val pointed = waveAt(350f)

                rowLabel("PathStrip: flat ends", 78f)
                rowLabel("RoundStrip: domed ends", 188f)
                rowLabel("RoundStrip: tapered to a point", 298f)

                // The paths, so the caps can be seen to hang off the ends of them.
                paint {
                    style(RcPaintStyle.Stroke)
                    strokeWidth(1f)
                    color(HINT)
                    antiAlias(true)
                }
                drawPath(squared)
                drawPath(domed)
                drawPath(pointed)

                paint {
                    style(RcPaintStyle.Fill)
                    color(INK)
                    antiAlias(true)
                }

                // Same path, same single width: the only difference is where the ribbon stops.
                drawMesh2D(
                    remoteMesh2DPathStrip(squared, segments = 64, widths = floatArrayOf(18f))
                )
                drawMesh2D(remoteMesh2DRoundStrip(domed, segments = 64, widths = floatArrayOf(18f)))

                // A zero width end has a zero radius cap, so this ends in a point, not a stub.
                drawMesh2D(
                    remoteMesh2DRoundStrip(
                        pointed,
                        segments = 64,
                        widths = floatArrayOf(0f, 22f, 0f),
                    )
                )

                drawMesh2DLabel("mesh2d RoundStrip, round ends", color = INK)
            }
        }
    }
}

/**
 * Round caps under an animated profile, next to the flat ribbon they are built from.
 *
 * Both ribbons take the same breathing control points. The caps are not a fixed decoration on the
 * ends: their radius is half the width the profile happens to have there, so they swell and shrink
 * with it, and the rounded ribbon reads as a single drawn stroke throughout while the flat one
 * keeps showing its cut ends.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslMesh2DRoundStripBreathing(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Mesh2D animated round caps"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFFFFFFFF.toInt())) {
            val w = componentWidth()
            val h = componentHeight()
            val t = continuousSeconds()

            save {
                scale(w / 400f, h / 400f)

                val flat = waveAt(150f)
                val round = waveAt(290f)

                rowLabel("PathStrip", 98f)
                rowLabel("RoundStrip", 238f)

                paint {
                    style(RcPaintStyle.Stroke)
                    strokeWidth(1f)
                    color(HINT)
                    antiAlias(true)
                }
                drawPath(flat)
                drawPath(round)

                paint {
                    style(RcPaintStyle.Fill)
                    color(INK)
                    antiAlias(true)
                }

                // The ends swing widest so the caps have something to do; the middle swings in
                // antiphase so the profile changes shape rather than simply scaling.
                drawMesh2D(
                    remoteMesh2DPathStrip(
                        flat,
                        segments = 64,
                        widths =
                            arrayOf(
                                sin(t * 1.3f) * 9f + 11f,
                                sin(t * 0.9f + 3.14f) * 8f + 14f,
                                sin(t * 1.3f) * 9f + 11f,
                            ),
                    )
                )
                drawMesh2D(
                    remoteMesh2DRoundStrip(
                        round,
                        segments = 64,
                        widths =
                            arrayOf(
                                sin(t * 1.3f) * 9f + 11f,
                                sin(t * 0.9f + 3.14f) * 8f + 14f,
                                sin(t * 1.3f) * 9f + 11f,
                            ),
                    )
                )

                drawMesh2DLabel("mesh2d RoundStrip, animated caps", color = INK)
            }
        }
    }
}
