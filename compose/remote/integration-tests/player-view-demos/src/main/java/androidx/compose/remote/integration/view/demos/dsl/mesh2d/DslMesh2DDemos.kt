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
import androidx.compose.remote.creation.dsl.RcMeshLayout
import androidx.compose.remote.creation.dsl.RcMeshMatrix
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcPathType
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.abs
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.clamp
import androidx.compose.remote.creation.dsl.cos
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.hypot
import androidx.compose.remote.creation.dsl.minus
import androidx.compose.remote.creation.dsl.plus
import androidx.compose.remote.creation.dsl.sin
import androidx.compose.remote.creation.dsl.times
import androidx.compose.remote.creation.profile.RcPlatformProfiles

private const val TAU = 6.2831855f

@Suppress("RestrictedApiAndroidX")
internal fun RcScope.drawMesh2DLabel(text: String, color: Int = 0xFFE2E8F0.toInt()) {
    save()
    scale(componentWidth() / 400f, componentHeight() / 400f)
    paint {
        style(RcPaintStyle.Fill)
        color(color)
        textSize(18f)
        antiAlias(true)
    }
    drawTextAnchored(remoteText(text), 200f.rf, 24f.rf, 0f.rf, 0f.rf, 0)
    restore()
}

/**
 * Five demos of the 2D mesh operations, each one chosen because the paint and path layers cannot
 * express it.
 *
 * The common thread: a mesh is a *field* of colour and position over a `(u, v)` domain, evaluated
 * per vertex. Gradients that curve, shading derived from geometry, ribbons that taper - all of
 * these are one expression each, and animating them costs no extra bytes because the expression is
 * already re-evaluated every frame.
 */

/**
 * A waving flag with derived shading and a label that rides the cloth.
 *
 * Two things worth noticing:
 * - The **shading is computed from the wave itself**. `cos` of the same phase that displaces `y` is
 *   the slope of the cloth, so brightness tracks how the surface tilts. That is a lighting model in
 *   one expression, and nothing in the paint layer can do it.
 * - The label is drawn with `drawTextAnchored`, an ordinary text command that knows nothing about
 *   meshes. `matrixFromMesh2D` puts it on the surface at (0.5, 0.35) and rotates it to match, so it
 *   rides the wave. This is the operation that stops meshes being a closed world.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslMesh2DWavingFlag(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Mesh2D waving flag"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFF0B1020.toInt())) {
            val w = componentWidth()
            val h = componentHeight()
            val flagW = w * 0.74f
            val flagH = h * 0.42f
            val left = (w - flagW) / 2f
            val top = (h - flagH) / 2f
            val t = continuousSeconds()

            val flag =
                remoteMesh2D(RcMeshLayout.Grid, uCount = 32, vCount = 18) {
                    // The wave grows with u, so the edge by the flagpole stays put and the free
                    // edge travels furthest - the thing that makes cloth read as cloth.
                    val phase = u * 7f + t * 2.4f
                    val swing = u * flagH * 0.20f
                    x = left + u * flagW
                    y = top + v * flagH + sin(phase) * swing

                    // cos(phase) is the slope of that wave, so it doubles as a lighting term.
                    val shade = 0.62f + cos(phase) * 0.38f
                    alpha = 1f.rf
                    red = shade * (0.30f + u * 0.30f)
                    green = shade * (0.55f + v * 0.25f)
                    blue = shade * (0.95f - u * 0.25f)
                }
            drawMesh2D(flag)

            // Ordinary text, placed onto the deformed surface by the mesh's local frame.
            save {
                matrixFromMesh2D(flag, 0.5f.rf, 0.35f.rf, RcMeshMatrix.Rotation)
                paint {
                    color(0xF2FFFFFF.toInt())
                    textSize(34f)
                    antiAlias(true)
                }
                drawTextAnchored(remoteText("RemoteCompose"), 0f.rf, 0f.rf, 0f.rf, 0f.rf, 0)
            }

            // A flagpole, so the anchored edge has something to be anchored to.
            paint { color(0xFF64748B.toInt()) }
            drawRect(left - 5f, top - 18f, left, top + flagH + 70f)

            drawMesh2DLabel("mesh2d Grid, Rotation matrix")
        }
    }
}

/**
 * A rose curve drawn as a polar mesh, with colour as a function of angle and radius.
 *
 * A radial colour field is the one gradient the paint layer cannot express at all, and here it is
 * two dimensional: hue sweeps with the angle `u` while the petals fade toward the rim with `v`. The
 * petal geometry is the classic `r = cos(k * theta)` rose, which in this domain is a single
 * expression on the radius.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslMesh2DRoseField(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Mesh2D rose field"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFF08060F.toInt())) {
            val cx = componentWidth() / 2f
            val cy = componentHeight() / 2f
            val maxR = componentWidth() * 0.42f
            val t = continuousSeconds()

            val rose =
                remoteMesh2D(RcMeshLayout.Polar, uCount = 180, vCount = 12) {
                    val theta = u * TAU
                    // Six petals that breathe, never collapsing all the way to zero.
                    val petal = 0.25f + 0.75f * abs(cos(theta * 3f + t * 0.5f))
                    val r = v * maxR * petal
                    x = cx + cos(theta) * r
                    y = cy + sin(theta) * r

                    // Hue as three phase-shifted cosines of the angle: a colour wheel without
                    // needing an hsv() function.
                    alpha = clamp(0f.rf, 1f.rf, 1f.rf - v * 0.25f)
                    red = 0.5f + cos(theta * 3f + t) * 0.5f
                    green = 0.5f + cos(theta * 3f + t + 2.094f) * 0.5f
                    blue = 0.5f + cos(theta * 3f + t + 4.188f) * 0.5f
                }
            drawMesh2D(rose)

            drawMesh2DLabel("mesh2d Polar, radial color field")
        }
    }
}

/**
 * A comet whose tail tapers and fades along a curved path.
 *
 * This is what `layout = pathStrip` plus a `width` *expression* buys. The ribbon follows an
 * ordinary path, and because the width is an expression over `u` rather than a constant, it can
 * narrow to nothing at the tail - a variable-width stroke, which otherwise takes a stack of
 * single-coloured quads.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslMesh2DCometTrail(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Mesh2D comet trail"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFF05060D.toInt())) {
            val w = componentWidth()
            val cx = w / 2f
            val cy = componentHeight() / 2f
            val radius = w * 0.30f

            // A lissajous loop for the comet to run along.
            val orbit =
                remoteXYPath(
                    rFun { s -> cx + sin(s * TAU) * radius },
                    rFun { s -> cy + sin(s * TAU * 2f) * radius * 0.62f },
                    0f,
                    1f,
                    96,
                    RcPathType.Loop,
                )

            val trail =
                remoteMesh2D(RcMeshLayout.PathStrip, uCount = 96, vCount = 2, path = orbit) {
                    // Fat and bright at the head, vanishing at the tail. The width is a fraction
                    // of the component rather than a pixel count, so the ribbon keeps its
                    // proportions on any canvas - it is in the path's units, and the path is
                    // sized from componentWidth().
                    width = w * 0.005f + u * w * 0.065f
                    alpha = u * u
                    red = 0.55f + u * 0.45f
                    green = 0.25f + u * 0.62f
                    blue = 1f.rf
                }
            drawMesh2D(trail)

            // The comet head: an ordinary circle, placed by the ribbon's own frame at u = 1.
            save {
                matrixFromMesh2D(trail, 1f.rf, 0.5f.rf, RcMeshMatrix.Origin)
                paint { color(0xFFFFFFFF.toInt()) }
                drawCircle(0f.rf, 0f.rf, w * 0.0225f)
            }

            drawMesh2DLabel("mesh2d PathStrip, width expr")
        }
    }
}

/**
 * A gauge whose arc carries a colour field in two directions at once.
 *
 * A sweep gradient can vary colour *around* an arc and a radial gradient can vary it *across* the
 * band, but neither does both. A mesh is a two dimensional domain, so `u` (angle) and `v` (across
 * the band) each get their own term: the hue sweeps with the value while the inner edge stays dark,
 * giving the band depth. The tick is an ordinary rectangle placed by the band's own frame.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslMesh2DRingGauge(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Mesh2D ring gauge"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFF0A0F1C.toInt())) {
            val cx = componentWidth() / 2f
            val cy = componentHeight() / 2f
            val outer = componentWidth() * 0.38f
            val inner = outer * 0.62f
            // A value that sweeps back and forth, standing in for live data.
            val value = 0.5f.rf + sin(continuousSeconds() * 0.7f) * 0.45f

            val band =
                remoteMesh2D(RcMeshLayout.Strip, uCount = 160, vCount = 6) {
                    // Three quarters of a turn, starting at the lower left.
                    //
                    // Strip rather than Ring, even though this is visibly a ring. Ring wraps u -
                    // its last column joins back to its first, which is right for a closed donut
                    // and wrong here: it would span the quarter-turn gap with a chord. Strip does
                    // not wrap, and its u reaches exactly 1 rather than 1 - 1/uCount.
                    val theta = 2.356f + u * TAU * 0.75f
                    val r = inner + v * (outer - inner)
                    x = cx + cos(theta) * r
                    y = cy + sin(theta) * r

                    // Lit past the current value, dim before it - and darker on the inner edge,
                    // which is the second dimension a sweep gradient does not have.
                    val on = clamp(0f.rf, 1f.rf, (value - u) * 40f)
                    val depth = 0.45f + v * 0.55f
                    alpha = 1f.rf
                    red = depth * (0.12f + on * (0.10f + u * 0.85f))
                    green = depth * (0.14f + on * (0.80f - u * 0.45f))
                    blue = depth * (0.20f + on * (0.75f - u * 0.30f))
                }
            drawMesh2D(band)

            // A tick riding the outer edge at the current value.
            save {
                matrixFromMesh2D(band, value, 1f.rf, RcMeshMatrix.Rotation)
                paint { color(0xFFFFFFFF.toInt()) }
                drawRect((-2f).rf, (-13f).rf, 2f.rf, 13f.rf)
            }

            paint {
                color(0xFFE2E8F0.toInt())
                textSize(56f)
                antiAlias(true)
            }
            drawTextAnchored(
                createTextFromFloat(value * 100f, 2, 0, 0),
                cx,
                cy,
                0f.rf,
                0f.rf,
                0,
            )

            drawMesh2DLabel("mesh2d Strip arc, Rotation matrix")
        }
    }
}

/**
 * A grid that ripples outward from the centre, drawn as a wireframe of coloured cells.
 *
 * The point here is the cost: this is a 28x28 grid, 784 vertices, and the whole animation is two
 * expressions. Adding `continuousSeconds()` to the displacement does not change the byte count, so
 * a rippling surface costs exactly what a flat one costs - the claim that makes parametric meshes
 * worth having over literal vertex lists.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslMesh2DRippleField(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Mesh2D ripple field"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFF04070E.toInt())) {
            val w = componentWidth()
            val h = componentHeight()
            val t = continuousSeconds()

            val field =
                remoteMesh2D(RcMeshLayout.Grid, uCount = 28, vCount = 28) {
                    // Distance from the centre of the domain drives both shape and colour.
                    //
                    // `hypot` rather than `sqrt(dx * dx + dy * dy)` on purpose. Nothing here is
                    // shared: the DSL inlines each sub-expression wherever it is named, so `dist`
                    // is re-expanded at all five of its uses. Spelled with sqrt it is 16 RPN
                    // tokens a time and `x` lands past Limits.MAX_EXPRESSION_SIZE (32); hypot is
                    // 7, which is the difference between this compiling and not.
                    val dx = u - 0.5f
                    val dy = v - 0.5f
                    val dist = hypot(dx, dy)
                    val wave = sin(dist * 26f - t * 3f)

                    // Push each vertex away from the centre by the wave, so the grid puckers.
                    val push = wave * 0.05f
                    x = w * (0.5f + dx * (1.55f + push))
                    y = h * (0.5f + dy * (1.55f + push))

                    val lit = 0.5f + wave * 0.5f
                    alpha = clamp(0f.rf, 1f.rf, 1f.rf - dist * 1.5f)
                    red = lit * 0.25f
                    green = lit * (0.35f + dist)
                    blue = 0.35f + lit * 0.65f
                }
            drawMesh2D(field)

            drawMesh2DLabel("mesh2d Grid hypot, radial wave")
        }
    }
}
