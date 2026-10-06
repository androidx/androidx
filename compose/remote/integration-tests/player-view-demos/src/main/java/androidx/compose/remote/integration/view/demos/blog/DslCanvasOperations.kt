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

package androidx.compose.remote.integration.view.demos.blog

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.dsl.DrawOnBitmapMode
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcCanvasScope
import androidx.compose.remote.creation.dsl.RcColorValue
import androidx.compose.remote.creation.dsl.RcContentScale
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcFontType
import androidx.compose.remote.creation.dsl.RcImage
import androidx.compose.remote.creation.dsl.RcMeshLayout
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcPathType
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcRowScope
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.RcStrokeCap
import androidx.compose.remote.creation.dsl.RcText
import androidx.compose.remote.creation.dsl.RcWeight
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.bitmapFontGlyph
import androidx.compose.remote.creation.dsl.clip
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.horizontalWeight
import androidx.compose.remote.creation.dsl.hypot
import androidx.compose.remote.creation.dsl.mad
import androidx.compose.remote.creation.dsl.minus
import androidx.compose.remote.creation.dsl.padding
import androidx.compose.remote.creation.dsl.pingPong
import androidx.compose.remote.creation.dsl.random
import androidx.compose.remote.creation.dsl.sqrt
import androidx.compose.remote.creation.dsl.square
import androidx.compose.remote.creation.dsl.times
import androidx.compose.remote.creation.dsl.verticalWeight
import androidx.compose.remote.creation.modifiers.RectShape
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.integration.view.demos.dsl.RcPathData
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A tour of Remote Compose canvas operations, one panel per capability, in a 2x3 grid:
 *
 * |               |              |
 * |---------------|--------------|
 * | Draw commands | Paths        |
 * | Images        | Mesh2D       |
 * | Particles     | Bitmap fonts |
 *
 * Every panel is authored once and animated entirely by the player from expressions of time.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslCanvasOperations(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 800),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 1200),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Canvas Operations"),
        experimental = true,
    ) {
        Column(modifier = Modifier.fillMaxSize().background(PAGE_BG)) {
            Row(modifier = Modifier.fillMaxSize().verticalWeight(1f)) {
                panel { drawCommandsPanel() }
                panel { pathsPanel() }
            }
            Row(modifier = Modifier.fillMaxSize().verticalWeight(1f)) {
                panel { imagesPanel() }
                panel { mesh2DPanel() }
            }
            Row(modifier = Modifier.fillMaxSize().verticalWeight(1f)) {
                panel { particlesPanel() }
                panel { bitmapFontsPanel() }
            }
        }
    }
}

/** One equal-width cell of a row, with a gutter and its own canvas. */
@Suppress("RestrictedApiAndroidX")
private fun RcRowScope.panel(content: RcCanvasScope.() -> Unit) {
    Canvas(
        modifier =
            Modifier.fillMaxSize()
                .horizontalWeight(1f)
                .padding(6f)
                .clip(RectShape(0f, 0f, 0f, 0f))
                .background(PANEL_BG),
        content = content,
    )
}

@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.panelTitle(title: String) {
    paint {
        color(TITLE_COLOR)
        style(RcPaintStyle.Fill)
        textSize(22f)
        typeface(RcFontType.Default, RcWeight.Bold, italic = false)
    }
    // pan (-1, -1) anchors the text's top-left corner at (x, y).
    drawTextAnchored(remoteText(title), 10f, 8f, -1f, 2f)
}

/** Small caption text, anchored by its top-left corner. */
@Suppress("RestrictedApiAndroidX")
private fun RcScope.caption(text: String, x: RcFloat, y: RcFloat, color: Int = CAPTION_COLOR) {
    paint {
        color(color)
        style(RcPaintStyle.Fill)
        textSize(15f)
        typeface(RcFontType.Default, RcWeight.Normal, italic = false)
    }
    drawTextAnchored(remoteText(text), x, y, (-1f).rf, (-1f).rf)
}

// =====================================================================================
// Panel 1: Draw commands
// =====================================================================================

/** The eight primitive draw operations exercised by the draw-commands panel. */
private enum class Primitive(val color: Int) {
    Arc(0xFFEA4335.toInt()),
    BitmapScaled(0xFFFFFFFF.toInt()),
    Circle(0xFF34A853.toInt()),
    Line(0xFFFBBC04.toInt()),
    Oval(0xFF4285F4.toInt()),
    RoundRect(0xFFA142F4.toInt()),
    Sector(0xFFF439A0.toInt()),
    TextAnchored(0xFF24C1E0.toInt()),
}

/**
 * Creation-time constants describing one shape's motion. Baked into the document as literals;
 * nothing here is evaluated on the player except through the expressions built from them.
 */
private class Motion(
    val speedX: Float, // px/s along x
    val speedY: Float, // px/s along y
    val phaseX: Float, // px offset into the x bounce cycle
    val phaseY: Float, // px offset into the y bounce cycle
    val spin: Float, // deg/s
    val angle0: Float, // initial rotation, deg
)

/**
 * Every primitive, once filled and once stroked: 16 shapes drifting in straight lines and bouncing
 * off the panel edges while they spin.
 *
 * Straight-line motion with reflection is exactly a triangle wave per axis, which is what
 * `pingPong(range, x)` computes: it ramps 0 → range → 0 as `x` grows. Feeding it `t * speed +
 * phase` gives constant velocity between walls and a mirror bounce at each wall, with x and y
 * independent so each shape traces a billiard-ball path. Orientation is simply `t * spin + angle0`.
 * All of it is an expression of `continuousSeconds()`, so the document is written once and the
 * player animates it.
 */
@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.drawCommandsPanel() {
    // Procedural texture for drawScaledBitmap: a 64x64 four-color quadrant tile with a ring.
    val texture = createBitmap(64, 64)
    drawOnBitmap(texture, DrawOnBitmapMode.CLEAR, RcColorValue(0xFF4285F4.toInt())) {
        paint { color(0xFFEA4335.toInt()) }
        drawRect(32f, 0f, 64f, 32f)
        paint { color(0xFFFBBC04.toInt()) }
        drawRect(0f, 32f, 32f, 64f)
        paint { color(0xFF34A853.toInt()) }
        drawRect(32f, 32f, 64f, 64f)
        paint {
            color(0xFFFFFFFF.toInt())
            style(RcPaintStyle.Stroke)
            strokeWidth(6f)
        }
        drawCircle(32f, 32f, 20f)
    }

    val w = componentWidth()
    val h = componentHeight()
    val t = continuousSeconds()
    // Shape half-size scales with the panel so the demo reads at any size.
    val s = (min(w, h) * 0.075f).flush()
    // Keep rotated shapes fully inside the panel: the diagonal of a 2s square is ~1.42s.
    val margin = (s * 1.5f).flush()
    val rangeX = (w - margin * 2f).flush()
    val topInset = 36f // leave room for the title
    val rangeY = (h - margin * 2f - topInset).flush()
    val label = remoteText("Aa")

    // Fixed seed: the choreography is the same every time the document is generated.
    val rng = Random(20260928)
    fun rand(lo: Float, hi: Float) = lo + rng.nextFloat() * (hi - lo)

    for (primitive in Primitive.entries) {
        for (filled in listOf(true, false)) {
            val m =
                Motion(
                    speedX = rand(35f, 110f),
                    speedY = rand(35f, 110f),
                    phaseX = rand(0f, 2000f),
                    phaseY = rand(0f, 2000f),
                    spin = rand(30f, 120f) * (if (rng.nextBoolean()) 1f else -1f),
                    angle0 = rand(0f, 360f),
                )
            val x = margin + pingPong(rangeX, t * m.speedX + m.phaseX)
            val y = margin + topInset + pingPong(rangeY, t * m.speedY + m.phaseY)
            val angle = t * m.spin + m.angle0

            save()
            translate(x, y)
            rotate(angle)
            paint {
                color(primitive.color)
                style(if (filled) RcPaintStyle.Fill else RcPaintStyle.Stroke)
                strokeWidth(4f)
                strokeCap(RcStrokeCap.Round)
                textSize(s * 1.4f)
                typeface(RcFontType.Default, RcWeight.Bold, italic = false)
            }
            drawPrimitive(primitive, filled, s, texture, label)
            restore()
        }
    }

    panelTitle("Draw Commands")
}

/** Draws [primitive] centered on the current origin with half-size [s]. */
@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.drawPrimitive(
    primitive: Primitive,
    filled: Boolean,
    s: RcFloat,
    texture: RcImage,
    label: RcText,
) {
    val zero = 0f.rf
    when (primitive) {
        Primitive.Arc -> drawArc(-s, -s, s, s, zero, 270f.rf)
        Primitive.BitmapScaled -> {
            // Paint style does not apply to bitmaps, so the "stroke" variant is the same
            // image with a stroked frame around it to keep the pair distinguishable.
            drawScaledBitmap(
                texture,
                zero,
                zero,
                64f.rf,
                64f.rf,
                -s,
                -s,
                s,
                s,
                RcContentScale.FillBounds,
            )
            if (!filled) drawRect(-s, -s, s, s)
        }
        Primitive.Circle -> drawCircle(zero, zero, s)
        // Lines are always stroked, so the fill variant is visually identical by design: a
        // useful reminder that Paint.Style is ignored by drawLine.
        Primitive.Line -> drawLine(-s, zero, s, zero)
        Primitive.Oval -> drawOval(-s, s * -0.6f, s, s * 0.6f)
        Primitive.RoundRect -> drawRoundRect(-s, s * -0.7f, s, s * 0.7f, s * 0.3f, s * 0.3f)
        Primitive.Sector -> drawSector(-s, -s, s, s, 30f.rf, 300f.rf)
        // pan (0, 0) centers the text on the origin so it spins about its own middle.
        Primitive.TextAnchored -> drawTextAnchored(label, zero, zero, zero, zero)
    }
}

// =====================================================================================
// Panel 2: Paths
// =====================================================================================

/**
 * Paths computed on the player from time-varying math.
 *
 * Neither path exists as point data in the document. Each is a *path expression*: a function the
 * player samples `count` times over `[start, end]` every frame, and because the function reads
 * `continuousSeconds()` the geometry itself animates.
 * - **Filled polar blob**: `r(θ) = R·(0.7 + 0.18·sin(5θ + 1.3t) + 0.08·sin(3θ − 2.1t))`, closed
 *   with [RcPathType.Loop] so it can be filled, then stroked on top with the same path.
 * - **Stroked Lissajous**: `x(s) = sin(3s + 0.6t)`, `y(s) = sin(2s)`; the time term slides the
 *   phase so the knot continuously re-ties itself.
 */
@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.pathsPanel() {
    val w = componentWidth()
    val h = componentHeight()
    val t = continuousSeconds()
    val top = 36f
    val cx = (w * 0.5f).flush()
    val cy = ((h + top) * 0.5f).flush()
    val r = (min(w, h - top) * 0.42f).flush()

    val blob =
        remotePolarPath(
            rFun { th ->
                (sin(th * 5f + t * 1.3f) * 0.18f + sin(th * 3f - t * 2.1f) * 0.08f + 0.7f) * r
            },
            0f,
            TAU,
            120,
            cx,
            cy,
            RcPathType.Loop,
        )
    paint {
        color(0x5534A853)
        style(RcPaintStyle.Fill)
        antiAlias(true)
    }
    drawPath(blob)
    paint {
        color(0xFF34A853.toInt())
        style(RcPaintStyle.Stroke)
        strokeWidth(3f)
    }
    drawPath(blob)

    val knot =
        remoteXYPath(
            rFun { s -> sin(s * 3f + t * 0.6f) * r * 0.85f + cx },
            rFun { s -> sin(s * 2f) * r * 0.85f + cy },
            0f,
            TAU,
            180,
            RcPathType.Loop,
        )
    paint {
        color(0xFFFBBC04.toInt())
        style(RcPaintStyle.Stroke)
        strokeWidth(2.5f)
        strokeCap(RcStrokeCap.Round)
    }
    drawPath(knot)

    panelTitle("Paths")
    caption("polar r(θ, t) · filled", 10f.rf, h - 44f, 0xFF81C995.toInt())
    caption("xy(s, t) · stroked", 10f.rf, h - 24f, 0xFFFDD663.toInt())
}

// =====================================================================================
// Panel 3: Images
// =====================================================================================

/** The four content-scale modes, one per quadrant, clockwise from top-left. */
@Suppress("RestrictedApiAndroidX")
private val QUADRANT_SCALES =
    listOf(RcContentScale.Fit, RcContentScale.Crop, RcContentScale.None, RcContentScale.FillBounds)

/**
 * One 16:9 image drawn into four rectangles that keep changing shape.
 *
 * The split point orbits the panel center, so every quadrant cycles through tall, wide and square
 * aspect ratios, and each applies a different [RcContentScale]:
 * - **Fit** letterboxes to show the whole image.
 * - **Crop** fills the cell and trims the overflow.
 * - **None** draws at native size, centered, cropped by the cell.
 * - **FillBounds** stretches to the cell, so the circle in the image visibly squashes.
 *
 * The destination rectangle is an expression, so the player recomputes the scale every frame.
 */
@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.imagesPanel() {
    val imgW = 160f
    val imgH = 90f
    val photo = createBitmap(imgW.toInt(), imgH.toInt())
    drawOnBitmap(photo, DrawOnBitmapMode.CLEAR, RcColorValue(0xFF4FC3F7.toInt())) {
        paint { color(0xFF2E7D32.toInt()) }
        drawRect(0f, 60f, imgW, imgH)
        paint { color(0xFFFFEB3B.toInt()) }
        drawCircle(125f, 25f, 14f)
        paint { color(0xFFE53935.toInt()) }
        drawCircle(80f, 52f, 22f)
        paint {
            color(0xFFFFFFFF.toInt())
            style(RcPaintStyle.Stroke)
            strokeWidth(4f)
        }
        drawRect(2f, 2f, imgW - 2f, imgH - 2f)
    }

    val w = componentWidth()
    val h = componentHeight()
    val t = continuousSeconds()
    val top = 36f.rf
    val splitX = (w * 0.5f + cos(t * 0.8f) * w * 0.22f).flush()
    val splitY = ((h + top) * 0.5f + sin(t * 0.8f) * (h - top) * 0.22f).flush()

    val zero = 0f.rf
    val cells =
        listOf(
            arrayOf(zero, top, splitX, splitY),
            arrayOf(splitX, top, w, splitY),
            arrayOf(splitX, splitY, w, h),
            arrayOf(zero, splitY, splitX, h),
        )
    for ((i, cell) in cells.withIndex()) {
        val (l, tp, rt, b) = cell
        save()
        clipRect(l, tp, rt, b)
        drawScaledBitmap(
            photo,
            zero,
            zero,
            imgW.rf,
            imgH.rf,
            l,
            tp,
            rt,
            b,
            QUADRANT_SCALES[i],
        )
        caption(QUADRANT_SCALES[i].name, l + 6f, tp + 6f, 0xFFFFFFFF.toInt())
        restore()
    }

    // Split lines and the orbiting pivot.
    paint {
        color(0xFFFFFFFF.toInt())
        style(RcPaintStyle.Stroke)
        strokeWidth(2f)
    }
    drawLine(splitX, top, splitX, h)
    drawLine(zero, splitY, w, splitY)
    paint { style(RcPaintStyle.Fill) }
    drawCircle(splitX, splitY, 6f.rf)

    panelTitle("Images")
}

// =====================================================================================
// Panel 4: Mesh2D
// =====================================================================================

/**
 * A snake: an antialiased Mesh2D ribbon laid along a path, where the path, the width and the
 * colours all move.
 *
 * The spine is an XY path expression whose sine wave travels toward the head (`s·9.4 − 4t`) while
 * the whole body drifts, so the curve slithers. `remoteMesh2DAntialias` lays a
 * [RcMeshLayout.PathStrip] along it, and the mesh's own expressions, evaluated per vertex at the
 * arclength fraction `u` (tail 0 → head 1) and the cross fraction `v`, supply the rest:
 * - **Width**: the cubic through four control widths at `u` = 0, ⅓, ⅔ and 1: a pointed tail, a
 *   swelling body, a neck and a head. Body and neck pulse with time. See [WidthCubic].
 * - **Round caps**: each end closes in a semicircle whose radius is half the width there. Over the
 *   last `r / L` of the arclength the width is scaled by `√(1 − τ²)`, with τ running 0 → 1 toward
 *   the tip. That traces a circle only if `L` is the spine's true length, which [spineLength] works
 *   out once a frame.
 * - **Colour**: rainbow bands flowing toward the head, lit from one side so the body reads as
 *   round.
 *
 * A spline strip (`remoteMesh2DRoundStrip`) has no vertex colours, so it cannot take the antialias
 * flag. A parametric mesh can: a skirt a pixel wide fades every edge to transparent instead of
 * leaving a staircase. The player re-tessellates whenever something the mesh reads changes, here
 * the per-frame widths, length and colour phase, and each time it re-reads the path, so the mesh
 * stays glued to the moving spine.
 */
@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.mesh2DPanel() {
    val w = componentWidth()
    val h = componentHeight()
    val t = continuousSeconds()
    val top = 36f
    val size = (min(w, h - top)).flush()

    // Path expressions are capped at 32 tokens each (Limits.MAX_EXPRESSION_SIZE), so every term
    // that doesn't depend on s is computed once per frame as its own variable.
    val xSpan = (w * 0.72f).flush()
    val x0 = (w * 0.14f + sin(t * 0.7f) * w * 0.05f).flush()
    val phase = (t * 4f).flush()
    val amp = (size * 0.12f).flush()
    val y0 = ((h + top) * 0.5f + cos(t * 0.9f) * size * 0.08f).flush()

    // s runs 0 (tail) → 1 (head).
    val spine =
        remoteXYPath(
            rFun { s -> s * xSpan + x0 + 2f * amp * cos(s * 9.4f - phase) },
            rFun { s -> sin(s * 9.4f - phase) * (s * 0.5f + 0.5f) * amp + y0 },
            0f,
            1f,
            64,
            RcPathType.Spline,
        )

    val pulse = (sin(t * 6f) * 2f).flush()
    val profile =
        WidthCubic(
            tail = (size * 0.005f + 5f).flush(),
            body = (size * 0.07f + pulse * size * 0.012f + 5f).flush(),
            neck = (size * 0.05f - pulse * size * 0.008f + 5f).flush(),
            head = (size * 0.075f + 5f).flush(),
        )

    // A cap of radius r = width / 2 takes up r / L of the arclength. Over it, τ runs from 0 where
    // the cap starts to 1 at the tip: τ = (u − 1)·2L/head + 1 at the head, 1 − u·2L/tail at the
    // tail. Each is one mad, and max() picks whichever end the vertex is in (both are negative in
    // the body, where the clamp to 0 leaves the width alone).
    val length = spineLength(xSpan, amp, phase)
    val headScale = (length * 2f / profile.head).flush()
    val headOffset = (1f - headScale).flush()
    val tailScale = (length * -2f / profile.tail).flush()

    val flow = (t * 1.5f).flush()
    val snake =
        remoteMesh2DAntialias(
            RcMeshLayout.PathStrip,
            uCount = 400, // about 10 columns across the head's cap
            // One row of quads. The triangles are listed row by row, so with more rows the whole
            // lower half of the strip would draw before the upper half, and where the snake
            // crosses itself the halves would interleave instead of the head's end lying on top.
            vCount = 2,
            antialiasWidth = 1f.rf, // the spine is in pixels, so this is one pixel
            path = spine,
        ) {
            val tau = max(max(mad(u, headScale, headOffset), mad(u, tailScale, 1f.rf)), 0f)
            width = profile.at(u) * sqrt(1f - square(tau))

            // Lit from one side: full brightness along one edge, half along the other.
            val shade = v * 0.5f + 0.5f
            val band = u * 9f - flow
            color(
                red = shade * (cos(band) * 0.5f + 0.5f),
                green = shade * (cos(band - 2.09f) * 0.5f + 0.5f),
                blue = shade * (cos(band - 4.19f) * 0.5f + 0.5f),
            )
        }

    // The vertex colours are the whole colour; the paint only has to be opaque.
    paint {
        color(0xFFFFFFFF.toInt())
        style(RcPaintStyle.Fill)
        antiAlias(true)
    }
    drawMesh2D(snake)
    paint {
        color(0xFF_FFFFFF.toInt())
        style(RcPaintStyle.Stroke)
    }

    drawPath(spine)
    // The spine itself, as a hairline, so the driving path is visible.
    paint {
        color(0x88FFFFFF.toInt())
        style(RcPaintStyle.Stroke)
        strokeWidth(1f)
    }
    drawPath(spine)

    panelTitle("Mesh2D")
}

/**
 * The cubic through four widths spaced evenly along a strip, at `u` = 0, ⅓, ⅔ and 1.
 *
 * Newton's forward differences over the four widths give the power-form coefficients, once a frame,
 * as variables. So per vertex [at] is three `mad`s, ten tokens, which leaves room in the width's 32
 * for the round caps.
 */
@Suppress("RestrictedApiAndroidX")
private class WidthCubic(val tail: RcFloat, body: RcFloat, neck: RcFloat, val head: RcFloat) {
    private val c1: RcFloat
    private val c2: RcFloat
    private val c3: RcFloat

    init {
        val d1 = body - tail
        val d2 = (neck - body * 2f + tail).flush()
        val d3 = (head - neck * 3f + body * 3f - tail).flush()
        // With x = 3u, p = tail + d1·x + d2·x(x − 1)/2 + d3·x(x − 1)(x − 2)/6.
        c1 = ((d1 - d2 * 0.5f + d3 * (1f / 3f)) * 3f).flush()
        c2 = ((d2 - d3) * 4.5f).flush()
        c3 = (d3 * 4.5f).flush()
    }

    /** The width at `u`, by Horner's rule. */
    fun at(u: RcFloat): RcFloat = mad(mad(mad(c3, u, c2), u, c1), u, tail)
}

/** 12-point Gauss–Legendre nodes, mapped onto [0, 1]. */
private val GAUSS_NODES =
    floatArrayOf(
        0.0092197f,
        0.0479414f,
        0.1150487f,
        0.2063410f,
        0.3160843f,
        0.4373833f,
        0.5626167f,
        0.6839157f,
        0.7936590f,
        0.8849513f,
        0.9520586f,
        0.9907803f,
    )

/** The weights of [GAUSS_NODES], which sum to 1. */
private val GAUSS_WEIGHTS =
    floatArrayOf(
        0.0235877f,
        0.0534697f,
        0.0800392f,
        0.1015837f,
        0.1167463f,
        0.1245735f,
        0.1245735f,
        0.1167463f,
        0.1015837f,
        0.0800392f,
        0.0534697f,
        0.0235877f,
    )

/**
 * The spine's arclength `L = ∫₀¹ |P′(s)| ds`, by 12-point Gauss–Legendre quadrature.
 *
 * With `a = 9.4s − phase`, the spine is `x = s·xSpan + x0 + 2·amp·cos a` and `y = amp·(0.5s +
 * 0.5)·sin a + y0`, so its derivative is
 * - `x′ = xSpan − 18.8·amp·sin a`
 * - `y′ = amp·(9.4·(0.5s + 0.5)·cos a + 0.5·sin a)`
 *
 * The code uses `b = phase − 9.4s = −a` (so `sin a = −sin b` and `cos a = cos b`), which keeps
 * `phase` a single variable token. Each node is one expression of about 26 tokens, and the sum is
 * one more. Where the path loops, the speed nearly stops and the integrand has a sharp dip, which
 * quadrature handles less well. Twelve nodes still come within 1% of the true length across panel
 * sizes and times.
 */
@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.spineLength(xSpan: RcFloat, amp: RcFloat, phase: RcFloat): RcFloat {
    val amp18 = (amp * 18.8f).flush()
    var length: RcFloat? = null
    for (k in GAUSS_NODES.indices) {
        val s = GAUSS_NODES[k]
        val b = phase - 9.4f * s
        val dx = xSpan + amp18 * sin(b)
        val dy = amp * (cos(b) * (9.4f * (0.5f * s + 0.5f)) - sin(b) * 0.5f)
        val term = (hypot(dx, dy) * GAUSS_WEIGHTS[k]).flush()
        length = length?.plus(term) ?: term
    }
    return length!!.flush()
}

// =====================================================================================
// Panel 5: Particles
// =====================================================================================

private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

/** Seconds a letter lives before respawning, plus up to [LIFETIME_JITTER] more. */
private const val LIFETIME = 4f
private const val LIFETIME_JITTER = 3f

/**
 * The 26 letters of the alphabet tumble down and bounce on the floor in an endless stream.
 *
 * One particle system of 26 particles, 6 registers each: `x, y, vy, angle, spin, age`.
 * - **Bounce**: a `particlesComparison` over all particles whose condition is `nextY - floorY`;
 *   only particles below the floor get their registers replaced (snapped to the floor, `vy`
 *   reflected and damped, spin damped), exactly as in the CollisionDetection demo.
 * - **Motion and respawn**: `particlesLoop` integrates every particle, and its restart expression
 *   `age - LIFETIME` kills any particle older than its lifetime and re-runs its initial
 *   expressions, so it reappears above the panel at a new random position. Ages start at a random
 *   negative offset, so particles expire one at a time and the letters keep coming.
 * - **Letters**: `particlesComparison` also takes an index range `[min, max)`. One comparison per
 *   letter, restricted to the single particle `k` with an always-true condition and pass-through
 *   registers, draws letter `k` for just that particle.
 */
@SuppressLint("PrimitiveInCollection")
@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.particlesPanel() {
    val w = componentWidth()
    val h = componentHeight()
    val size = (min(w, h) * 0.075f).flush()
    val floorY = (h - size * 0.8f).flush()
    val letters = ALPHABET.map { remoteText(it.toString()) }

    // Floor
    paint {
        color(0xFF64748B.toInt())
        style(RcPaintStyle.Fill)
    }
    drawRect(0f.rf, floorY + size * 0.55f, w, floorY + size * 0.55f + 3f)
    paint {
        style(RcPaintStyle.Fill)
        textSize(64f)
        typeface(RcFontType.Default, RcWeight.Bold, italic = false)
    }
    // A single impulse that never ends: set the system up once, then run it every frame.
    impulse(1_000_000f.rf, 0f.rf) {
        val regs = FloatArray(6)
        val letterSystem =
            createParticles(
                regs,
                arrayOf(
                    random(size, w - size), // x
                    random(h * -0.8f, -size), // y: above the panel, staggered
                    random(0f.rf, 150f.rf), // vy px/s
                    random(0f.rf, 360f.rf), // angle deg
                    random((-360f).rf, 360f.rf), // spin deg/s
                    random((-LIFETIME_JITTER).rf, 0f.rf), // age s: staggers the respawns
                ),
                ALPHABET.length,
            )
        val x = RcFloat(regs[0])
        val y = RcFloat(regs[1])
        val vy = RcFloat(regs[2])
        val angle = RcFloat(regs[3])
        val spin = RcFloat(regs[4])
        val age = RcFloat(regs[5])
        val dt = deltaTime()

        impulseProcess {
            val nextVy = vy + (900f * dt)
            val nextY = y + (nextVy * dt)
            val nextAngle = angle + (spin * dt)
            val nextAge = age + dt

            // Bounce: only particles that would end up below the floor.
            particlesComparison(
                id = letterSystem,
                flags = 0,
                min = (-1f).rf,
                max = (-1f).rf,
                condition = nextY - floorY,
                then = arrayOf(x, floorY, -0.6f * vy, angle, spin * 0.7f, age),
            ) {}

            // Integrate every particle; kill and respawn it once it outlives LIFETIME.
            particlesLoop(
                letterSystem,
                age - LIFETIME,
                arrayOf(x, nextY, nextVy, nextAngle, spin, nextAge),
            ) {}

            // Draw: particle k, and only particle k, gets letter k.
            val keep = arrayOf(x, y, vy, angle, spin, age)
            for ((k, text) in letters.withIndex()) {
                particlesComparison(
                    id = letterSystem,
                    flags = 0,
                    min = k.toFloat().rf,
                    max = (k + 1).toFloat().rf,
                    condition = 1f.rf, // always true
                    then = keep,
                ) {
                    save()
                    this@particlesPanel.translate(x, y)
                    this@particlesPanel.rotate(angle)
                    paint {
                        color(LETTER_COLORS[k % LETTER_COLORS.size])
                    }
                    drawTextAnchored(text, 0f, 0f, 0f, 0f)
                    restore()
                }
            }
        }
    }

    panelTitle("Particles")
}

private val LETTER_COLORS =
    intArrayOf(
        0xFFEA4335.toInt(),
        0xFFFBBC04.toInt(),
        0xFF34A853.toInt(),
        0xFF4285F4.toInt(),
        0xFFA142F4.toInt(),
        0xFF24C1E0.toInt(),
    )

// =====================================================================================
// Panel 6: Bitmap fonts
// =====================================================================================

/** The first 300 decimals of pi. */
private const val PI_DIGITS =
    "3.14159265358979323846264338327950288419716939937510" +
        "58209749445923078164062862089986280348253421170679"
//        "82148086513282306647093844609550582231725359408128" +
//        "48111745028410270193852110555964462294895493038196" +
//        "44288109756659334461284756482337867831652712019091" +
//        "45648566923460348610454326648213393607260249141273"

/** Radius of the spiral's outer end, in spiral units; the spiral is scaled to fit the panel. */
private const val SPIRAL_OUTER = 180f

/**
 * The digits of pi spiralling inward, rendered with a bitmap font.
 *
 * A bitmap font ships pre-rasterized glyph images inside the document, so the text looks identical
 * on every player regardless of installed fonts. Here the 11 glyphs `0-9` and `.` are rasterized at
 * creation time with an Android [Paint], registered with `remoteBitmap`, and assembled with
 * `createBitmapFont`. The text is then laid along an Archimedean spiral with the bitmap-font
 * variant of `drawTextOnPath`, and the whole spiral slowly turns.
 */
@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.bitmapFontsPanel() {
    val glyphPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 32f
            //  color = 0xFFFDD663.toInt()
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            shader =
                RadialGradientShader(
                    Offset(12f, 12f),
                    radius = 24f,
                    colors = listOf(Color.Red, Color.White, Color.Cyan, Color.White),
                )
        }
    val metrics = glyphPaint.fontMetrics
    val glyphH = ceil(metrics.descent - metrics.ascent).toInt()
    val glyphs =
        "0123456789."
            .map { ch ->
                val s = ch.toString()
                val glyphW = ceil(glyphPaint.measureText(s)).toInt()
                val bmp = Bitmap.createBitmap(glyphW, glyphH, Bitmap.Config.ARGB_8888)
                Canvas(bmp).drawText(s, 0f, -metrics.ascent, glyphPaint)
                bitmapFontGlyph(s, remoteBitmap(bmp), glyphW, glyphH)
            }
            .toTypedArray()
    val font = createBitmapFont(glyphs)
    val digits = remoteText(PI_DIGITS)
    val spiral = remotePathData(RcPathData(spiralSvg(SPIRAL_OUTER, 16f, 5.5f)))

    val w = componentWidth()
    val h = componentHeight()
    val t = continuousSeconds()
    val top = 36f
    val fit = (min(w, h - top) / (SPIRAL_OUTER * 2.1f * 0.8f)).flush()

    save()
    translate(w * 0.5f, (h + top) * 0.5f)
    scale(fit, fit)
    rotate(t * -12f)
    drawTextOnPath(digits, font, spiral, 0, -1, 0f, 0f)
    restore()

    panelTitle("Bitmap Fonts")
}

/**
 * SVG path data for an Archimedean spiral centered on the origin, running inward from [outer] to
 * [inner] over [turns] revolutions, so text laid along it starts on the outside.
 */
private fun spiralSvg(outer: Float, inner: Float, turns: Float): String {
    val steps = (turns * 72).toInt()
    val sb = StringBuilder()
    for (i in 0..steps) {
        val p = i.toFloat() / steps
        val a = p * turns * TAU
        val r = outer + (inner - outer) * p
        sb.append(if (i == 0) "M " else " L ")
        sb.append(r * cos(a)).append(',').append(r * sin(a))
    }
    return sb.toString()
}

private const val TAU = (2.0 * Math.PI).toFloat()
private const val PAGE_BG = 0xFF0F172A.toInt()
private const val PANEL_BG = 0xFF1E293B.toInt()
private const val TITLE_COLOR = 0xFFE2E8F0.toInt()
private const val CAPTION_COLOR = 0xFFCBD5E1.toInt()

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview(widthDp = 400, heightDp = 600)
fun DslCanvasOperationsPreview() {
    RemoteDocumentPreview(RemoteDocument(dslCanvasOperations()))
}
