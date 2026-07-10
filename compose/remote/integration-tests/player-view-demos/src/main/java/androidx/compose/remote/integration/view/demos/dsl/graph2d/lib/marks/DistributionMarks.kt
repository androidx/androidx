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

package androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks

import androidx.compose.remote.creation.dsl.RcCanvasScope
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcStrokeCap
import androidx.compose.remote.creation.dsl.RcStrokeJoin
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.Cartesian
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.Palette
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.Stats
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.fillRectR
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.strokeLineR

/**
 * The distribution family. Statistical transforms ([Stats]) run host-side; these marks map the
 * resulting geometry onto a reactive [Cartesian]. Histogram / density / ECDF use a numeric x axis
 * (`cart.xScale`); box / violin / strip use the categorical band.
 */

/** Histogram: one bar per bin spanning its numeric edges, height = count. */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderHistogram(cart: Cartesian, hist: Stats.Histogram, color: Int) {
    val xs = cart.xScale ?: return
    val base = cart.value.map(0f)
    for (b in hist.counts.indices) {
        if (hist.counts[b] == 0) continue
        val x0 = xs.map(hist.edges[b]) + 0.5f
        val x1 = xs.map(hist.edges[b + 1]) - 0.5f
        val top = cart.value.map(hist.counts[b].toFloat())
        fillRectR(color, x0, top, x1, base)
    }
}

/** Density (KDE) curve over a numeric x axis, optionally filled to the baseline. */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderDensity(
    cart: Cartesian,
    curve: Stats.Curve,
    color: Int,
    fill: Boolean,
) {
    val xs = cart.xScale ?: return
    val n = curve.xs.size
    if (n < 2) return
    val base = cart.value.map(0f)
    if (fill) {
        val ap = remotePath(xs.map(curve.xs[0]).toFloat(), cart.value.map(curve.ys[0]).toFloat())
        for (i in 1 until n) ap.lineTo(
            xs.map(curve.xs[i]).toFloat(),
            cart.value.map(curve.ys[i]).toFloat(),
        )
        ap.lineTo(xs.map(curve.xs[n - 1]).toFloat(), base.toFloat())
        ap.lineTo(xs.map(curve.xs[0]).toFloat(), base.toFloat())
        ap.close()
        paint {
            color(Palette.fadeAlpha(color, cart.theme.densityFillAlpha))
            style(RcPaintStyle.Fill)
        }
        drawPath(ap.getPath())
    }
    val lp = remotePath(xs.map(curve.xs[0]).toFloat(), cart.value.map(curve.ys[0]).toFloat())
    for (i in 1 until n) lp.lineTo(
        xs.map(curve.xs[i]).toFloat(),
        cart.value.map(curve.ys[i]).toFloat(),
    )
    paint {
        color(color)
        style(RcPaintStyle.Stroke)
        strokeWidth(cart.theme.lineStroke)
        strokeJoin(RcStrokeJoin.Round)
    }
    drawPath(lp.getPath())
}

/** ECDF: a step function rising from 0 to 1 over a numeric x axis. */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderEcdf(cart: Cartesian, curve: Stats.Curve, color: Int) {
    val xs = cart.xScale ?: return
    val n = curve.xs.size
    if (n < 1) return
    val y0 = cart.value.map(0f)
    val path = remotePath(xs.map(curve.xs[0]).toFloat(), y0.toFloat())
    var prevP = 0f
    for (i in 0 until n) {
        val x = xs.map(curve.xs[i]).toFloat()
        path.lineTo(x, cart.value.map(prevP).toFloat())
        path.lineTo(x, cart.value.map(curve.ys[i]).toFloat())
        prevP = curve.ys[i]
    }
    paint {
        color(color)
        style(RcPaintStyle.Stroke)
        strokeWidth(cart.theme.lineStroke)
        strokeCap(RcStrokeCap.Square)
    }
    drawPath(path.getPath())
}

/** Box-and-whisker glyph per category (band), value on y. */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderBoxPlot(cart: Cartesian, groups: List<Stats.BoxStats>) {
    val theme = cart.theme
    for (i in groups.indices) {
        val g = groups[i]
        val color = theme.seriesColor(i)
        val cx = cart.band.center(i)
        val boxHalf = cart.band.bandWidth * cart.theme.boxWidthFrac
        val capHalf = cart.band.bandWidth * cart.theme.boxCapFrac
        val l = cx - boxHalf
        val r = cx + boxHalf
        val yQ3 = cart.value.map(g.q3)
        val yQ1 = cart.value.map(g.q1)
        val yMed = cart.value.map(g.median)
        val yWHi = cart.value.map(g.whiskerHi)
        val yWLo = cart.value.map(g.whiskerLo)
        // Whiskers + caps.
        strokeLineR(color, theme.axisStroke, cx, yWHi, cx, yQ3)
        strokeLineR(color, theme.axisStroke, cx, yQ1, cx, yWLo)
        strokeLineR(color, theme.axisStroke, cx - capHalf, yWHi, cx + capHalf, yWHi)
        strokeLineR(color, theme.axisStroke, cx - capHalf, yWLo, cx + capHalf, yWLo)
        // Box.
        paint {
            color(Palette.fadeAlpha(color, cart.theme.boxFillAlpha))
            style(RcPaintStyle.Fill)
        }
        drawRect(l, yQ3, r, yQ1)
        paint {
            color(color)
            style(RcPaintStyle.Stroke)
            strokeWidth(theme.axisStroke + 0.5f)
        }
        drawRect(l, yQ3, r, yQ1)
        strokeLineR(color, theme.axisStroke + 1.5f, l, yMed, r, yMed)
        // Outliers.
        paint {
            color(color)
            style(RcPaintStyle.Fill)
        }
        for (o in g.outliers) drawCircle(cx, cart.value.map(o), cart.theme.outlierRadius.rf)
    }
}

/** Violin per category: a symmetric KDE shape (density on x, value on y). */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderViolin(cart: Cartesian, groups: List<Stats.Curve>) {
    val theme = cart.theme
    for (i in groups.indices) {
        val curve = groups[i]
        val nG = curve.xs.size
        if (nG < 2) continue
        var maxD = 0f
        for (d in curve.ys) if (d > maxD) maxD = d
        if (maxD <= 0f) continue
        val color = theme.seriesColor(i)
        val cx = cart.band.center(i)
        val half = cart.band.bandWidth * cart.theme.violinWidthFrac
        fun w(d: Float) = half * (d / maxD)
        // Right edge upward, then left edge downward.
        val path =
            remotePath((cx + w(curve.ys[0])).toFloat(), cart.value.map(curve.xs[0]).toFloat())
        for (g in 1 until nG) {
            path.lineTo((cx + w(curve.ys[g])).toFloat(), cart.value.map(curve.xs[g]).toFloat())
        }
        for (g in nG - 1 downTo 0) {
            path.lineTo((cx - w(curve.ys[g])).toFloat(), cart.value.map(curve.xs[g]).toFloat())
        }
        path.close()
        paint {
            color(Palette.fadeAlpha(color, cart.theme.violinFillAlpha))
            style(RcPaintStyle.Fill)
        }
        drawPath(path.getPath())
        paint {
            color(color)
            style(RcPaintStyle.Stroke)
            strokeWidth(1.5f)
            strokeJoin(RcStrokeJoin.Round)
        }
        drawPath(path.getPath())
    }
}

/** Strip plot per category: every observation as a jittered dot. */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderStrip(cart: Cartesian, groups: List<FloatArray>) {
    val theme = cart.theme
    for (i in groups.indices) {
        val data = groups[i]
        val color = Palette.fadeAlpha(theme.seriesColor(i), cart.theme.stripAlpha)
        val cx = cart.band.center(i)
        val amp = cart.band.bandWidth * cart.theme.stripJitterFrac
        paint {
            color(color)
            style(RcPaintStyle.Fill)
        }
        for (k in data.indices) {
            val x = cx + amp * Stats.jitter(k, 1f)
            drawCircle(x, cart.value.map(data[k]), cart.theme.stripDotRadius.rf)
        }
    }
}
