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
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.fillRectR
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.strokeLineR

/**
 * Interval / forecast marks: shaded bands ([renderBand]), error bars ([renderErrorBars]) and forest
 * plots ([renderForest]). Bands sit on a point-scale (vertical) cartesian; error bars on a
 * categorical (vertical) one; forest on a horizontal (value-on-x, category-on-y) cartesian.
 */

/** A shaded band between [lowers] and [uppers] across the point-scale x axis. */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderBand(
    cart: Cartesian,
    lowers: FloatArray,
    uppers: FloatArray,
    color: Int,
    fillAlpha: Float,
) {
    val n = minOf(lowers.size, uppers.size)
    if (n < 2) return
    val path = remotePath(cart.categoryX(0).toFloat(), cart.value.map(uppers[0]).toFloat())
    for (i in 1 until n) path.lineTo(
        cart.categoryX(i).toFloat(),
        cart.value.map(uppers[i]).toFloat(),
    )
    for (i in n - 1 downTo 0) path.lineTo(
        cart.categoryX(i).toFloat(),
        cart.value.map(lowers[i]).toFloat(),
    )
    path.close()
    paint {
        color(Palette.fadeAlpha(color, fillAlpha))
        style(RcPaintStyle.Fill)
    }
    drawPath(path.getPath())
}

/** Stroke a polyline through [values] on the point-scale x axis (e.g. a band's center line). */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.strokePolyline(
    cart: Cartesian,
    values: FloatArray,
    color: Int,
    width: Float,
    markers: Boolean = false,
) {
    if (values.size < 2) return
    val path = remotePath(cart.categoryX(0).toFloat(), cart.value.map(values[0]).toFloat())
    for (i in 1 until values.size) path.lineTo(
        cart.categoryX(i).toFloat(),
        cart.value.map(values[i]).toFloat(),
    )
    paint {
        color(color)
        style(RcPaintStyle.Stroke)
        strokeWidth(width)
        strokeCap(RcStrokeCap.Round)
        strokeJoin(RcStrokeJoin.Round)
    }
    drawPath(path.getPath())
    if (markers) {
        paint {
            color(color)
            style(RcPaintStyle.Fill)
        }
        for (i in values.indices) drawCircle(
            cart.categoryX(i),
            cart.value.map(values[i]),
            (width + 1.5f).rf,
        )
    }
}

/**
 * Vertical error bars (± interval) with caps and a point, on a categorical (vertical) cartesian.
 */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderErrorBars(
    cart: Cartesian,
    values: FloatArray,
    errLo: FloatArray,
    errHi: FloatArray,
    color: Int,
) {
    val w = cart.theme.axisStroke + 0.5f
    for (i in values.indices) {
        val cx = cart.band.center(i)
        val cap = cart.band.bandWidth * cart.theme.errorBarCapFrac
        val yHi = cart.value.map(values[i] + errHi[i])
        val yLo = cart.value.map(values[i] - errLo[i])
        strokeLineR(color, w, cx, yHi, cx, yLo)
        strokeLineR(color, w, cx - cap, yHi, cx + cap, yHi)
        strokeLineR(color, w, cx - cap, yLo, cx + cap, yLo)
        paint {
            color(color)
            style(RcPaintStyle.Fill)
        }
        drawCircle(cx, cart.value.map(values[i]), cart.theme.errorBarPointRadius.rf)
    }
}

/** Forest plot: per-category CI line + square marker (size ∝ weight) and a reference line. */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderForest(
    cart: Cartesian,
    estimates: FloatArray,
    los: FloatArray,
    his: FloatArray,
    weights: FloatArray?,
    refValue: Float,
    color: Int,
) {
    val theme = cart.theme
    // Reference line.
    val xRef = cart.value.map(refValue)
    strokeLineR(theme.zeroLineColor, theme.axisStroke, xRef, cart.plotTop.rf, xRef, cart.plotBottom)
    var wMax = 1f
    if (weights != null) for (w in weights) if (w > wMax) wMax = w
    for (i in estimates.indices) {
        val cy = cart.band.center(i)
        val cap = cart.band.bandWidth * 0.16f
        val xLo = cart.value.map(los[i])
        val xHi = cart.value.map(his[i])
        val xEst = cart.value.map(estimates[i])
        strokeLineR(color, theme.axisStroke + 0.5f, xLo, cy, xHi, cy)
        strokeLineR(color, theme.axisStroke + 0.5f, xLo, cy - cap, xLo, cy + cap)
        strokeLineR(color, theme.axisStroke + 0.5f, xHi, cy - cap, xHi, cy + cap)
        val r =
            if (weights != null)
                theme.forestMarkerMin +
                    (theme.forestMarkerMax - theme.forestMarkerMin) * (weights[i] / wMax)
            else theme.markerRadius + 2f
        paint {
            color(color)
            style(RcPaintStyle.Fill)
        }
        fillRectR(color, xEst - r, cy - r, xEst + r, cy + r)
    }
}
