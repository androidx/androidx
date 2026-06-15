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
import androidx.compose.remote.creation.dsl.RcDynamicPath
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcStrokeCap
import androidx.compose.remote.creation.dsl.RcStrokeJoin
import androidx.compose.remote.creation.dsl.arraySpline
import androidx.compose.remote.creation.dsl.smoothStep
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.AreaLayout
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.Cartesian
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.LineInterp
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.Palette
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.SeriesKind
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.SeriesSpec
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.dashEffect

/**
 * The line/area family on a resolved [Cartesian] system. Geometry is fully reactive: y values map
 * through `value.mapR`, x positions come from the reactive [PointScale], so the chart reflows to
 * its canvas and re-resolves on the player. Reactive coordinates are encoded into the dynamic path
 * via `RcFloat.toFloat()`. Stacked/percent areas use host cumulative values mapped reactively.
 *
 * [arrays] carries each series' data already resolved into ONE document float array (created by the
 * caller and shared with the touch crosshair) — every `remoteFloatArray` call would otherwise
 * serialize a fresh copy of the data into the document. It may be empty for the stacked-area path,
 * which works from host cumulative values and never dereferences the arrays.
 */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderLines(
    cart: Cartesian,
    series: List<SeriesSpec>,
    arrays: List<RcFloat>,
    areaLayout: AreaLayout,
    animate: Boolean,
) {
    if (series.isEmpty()) return
    val grow =
        if (animate)
            smoothStep(continuousSeconds(), 0f.rf, cart.theme.animateDurationSec.rf).flush()
        else null

    val anyArea = series.any { it.kind == SeriesKind.Area }
    val stacked = anyArea && areaLayout != AreaLayout.Overlay
    if (stacked) {
        renderStackedAreas(cart, series, areaLayout == AreaLayout.Percent, grow)
        return
    }
    for (si in series.indices) {
        val spec = series[si]
        val arr = arrays[si]
        val color = spec.color ?: cart.theme.seriesColor(si)
        if (spec.kind == SeriesKind.Area) drawAreaFill(cart, spec, arr, color, grow)
        drawLineStroke(cart, spec, arr, color, grow)
        if (spec.markers) drawMarkers(cart, spec, arr, color, grow)
    }
}

/** Reactive y baseline for area fills: the value=0 line if visible, else the plot floor. */
@Suppress("RestrictedApiAndroidX")
private fun areaBaseY(cart: Cartesian): RcFloat {
    val lo = minOf(cart.valueAxis.min, cart.valueAxis.max)
    val hi = maxOf(cart.valueAxis.min, cart.valueAxis.max)
    return cart.value.map(0f.coerceIn(lo, hi))
}

/** Reactive y for a value, optionally animated rising from [base]. */
@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.pointY(
    cart: Cartesian,
    v: RcFloat,
    base: RcFloat,
    grow: RcFloat?,
): RcFloat {
    val y = cart.value.mapR(v)
    return if (grow == null) y else base + (y - base) * grow
}

/** Append the series' line to [path] (already positioned at the first point). */
@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.appendLine(
    path: RcDynamicPath,
    cart: Cartesian,
    spec: SeriesSpec,
    arr: RcFloat,
    base: RcFloat,
    grow: RcFloat?,
) {
    val n = spec.values.size
    when (spec.interp) {
        LineInterp.Spline -> {
            val x0 = cart.categoryX(0)
            val xN = cart.categoryX(n - 1)
            for (k in 1..cart.theme.splineSamples) {
                val t = k.toFloat() / cart.theme.splineSamples
                val x = x0 + (xN - x0) * t
                path.lineTo(x.toFloat(), pointY(cart, arraySpline(arr, t), base, grow).toFloat())
            }
        }
        LineInterp.Step -> {
            for (i in 1 until n) {
                val x = cart.categoryX(i).toFloat()
                path.lineTo(x, pointY(cart, arr[i - 1], base, grow).toFloat())
                path.lineTo(x, pointY(cart, arr[i], base, grow).toFloat())
            }
        }
        LineInterp.Linear -> {
            for (i in 1 until n) {
                path.lineTo(cart.categoryX(i).toFloat(), pointY(cart, arr[i], base, grow).toFloat())
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.startPath(
    cart: Cartesian,
    spec: SeriesSpec,
    arr: RcFloat,
    base: RcFloat,
    grow: RcFloat?,
): RcDynamicPath {
    val y0 =
        if (spec.interp == LineInterp.Spline) pointY(cart, arraySpline(arr, 0f), base, grow)
        else pointY(cart, arr[0], base, grow)
    return remotePath(cart.categoryX(0).toFloat(), y0.toFloat())
}

@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.drawLineStroke(
    cart: Cartesian,
    spec: SeriesSpec,
    arr: RcFloat,
    color: Int,
    grow: RcFloat?,
) {
    val base = areaBaseY(cart)
    val path = startPath(cart, spec, arr, base, grow)
    appendLine(path, cart, spec, arr, base, grow)
    paint {
        color(color)
        style(RcPaintStyle.Stroke)
        strokeWidth(spec.strokeWidth ?: cart.theme.lineStroke)
        strokeCap(RcStrokeCap.Round)
        strokeJoin(RcStrokeJoin.Round)
        if (spec.dash != null) pathEffect(dashEffect(*spec.dash))
    }
    drawPath(path.getPath())
    if (spec.dash != null) paint { pathEffect(null) }
}

@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.drawAreaFill(
    cart: Cartesian,
    spec: SeriesSpec,
    arr: RcFloat,
    color: Int,
    grow: RcFloat?,
) {
    val base = areaBaseY(cart)
    val n = spec.values.size
    val path = startPath(cart, spec, arr, base, grow)
    appendLine(path, cart, spec, arr, base, grow)
    path.lineTo(cart.categoryX(n - 1).toFloat(), base.toFloat())
    path.lineTo(cart.categoryX(0).toFloat(), base.toFloat())
    path.close()
    paint {
        color(Palette.fadeAlpha(color, cart.theme.areaFillAlpha))
        style(RcPaintStyle.Fill)
    }
    drawPath(path.getPath())
}

@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.drawMarkers(
    cart: Cartesian,
    spec: SeriesSpec,
    arr: RcFloat,
    color: Int,
    grow: RcFloat?,
) {
    val base = areaBaseY(cart)
    val r = (spec.strokeWidth ?: cart.theme.lineStroke) + 1.5f
    paint {
        color(color)
        style(RcPaintStyle.Fill)
    }
    for (i in spec.values.indices) {
        drawCircle(cart.categoryX(i), pointY(cart, arr[i], base, grow), r.rf)
    }
}

/** Stacked / 100%-stacked areas using host cumulative values mapped reactively. */
@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.renderStackedAreas(
    cart: Cartesian,
    series: List<SeriesSpec>,
    percent: Boolean,
    grow: RcFloat?,
) {
    val n = cart.categories.size
    val base = cart.value.map(0f)
    val lower = FloatArray(n)
    for (si in series.indices) {
        val spec = series[si]
        val color = spec.color ?: cart.theme.seriesColor(si)
        val upper = FloatArray(n)
        for (i in 0 until n) {
            val raw = if (i < spec.values.size) spec.values[i] else 0f
            val v =
                if (percent) {
                    var tot = 0f
                    for (s in series) tot += if (i < s.values.size) maxOf(s.values[i], 0f) else 0f
                    if (tot == 0f) 0f else maxOf(raw, 0f) / tot
                } else {
                    maxOf(raw, 0f)
                }
            upper[i] = lower[i] + v
        }
        val path =
            remotePath(
                cart.categoryX(0).toFloat(),
                growEdge(cart.value.map(upper[0]), base, grow).toFloat(),
            )
        for (i in 1 until n) {
            path.lineTo(
                cart.categoryX(i).toFloat(),
                growEdge(cart.value.map(upper[i]), base, grow).toFloat(),
            )
        }
        for (i in n - 1 downTo 0) {
            path.lineTo(
                cart.categoryX(i).toFloat(),
                growEdge(cart.value.map(lower[i]), base, grow).toFloat(),
            )
        }
        path.close()
        paint {
            color(color)
            style(RcPaintStyle.Fill)
        }
        drawPath(path.getPath())
        for (i in 0 until n) lower[i] = upper[i]
    }
}

/** Animate a mapped pixel toward [base] by [grow]. */
@Suppress("RestrictedApiAndroidX")
private fun growEdge(pixel: RcFloat, base: RcFloat, grow: RcFloat?): RcFloat =
    if (grow == null) pixel else base + (pixel - base) * grow
