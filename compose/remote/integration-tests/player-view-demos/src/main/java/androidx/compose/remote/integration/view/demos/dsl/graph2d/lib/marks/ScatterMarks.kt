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
import kotlin.math.sqrt

/**
 * Scatter / bubble / connected-scatter on a both-numeric [Cartesian] (`xScale` on x, `value` on y).
 * Points map through `xScale.map(x)` and `value.map(y)` — reactive, so the cloud reflows to the
 * canvas. Bubble radii are area-proportional and computed host-side from the size channel.
 */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderScatter(
    cart: Cartesian,
    xs: FloatArray,
    ys: FloatArray,
    sizes: FloatArray?,
    color: Int,
    connect: Boolean,
) {
    val theme = cart.theme
    val pointRadius = theme.markerRadius
    val minR = theme.bubbleMinRadius
    val maxR = theme.bubbleMaxRadius
    val xScale = cart.xScale ?: return
    val n = minOf(xs.size, ys.size)
    if (n == 0) return

    if (connect) {
        val path = remotePath(xScale.map(xs[0]).toFloat(), cart.value.map(ys[0]).toFloat())
        for (i in 1 until n) {
            path.lineTo(xScale.map(xs[i]).toFloat(), cart.value.map(ys[i]).toFloat())
        }
        paint {
            color(color)
            style(RcPaintStyle.Stroke)
            strokeWidth(2f)
            strokeCap(RcStrokeCap.Round)
            strokeJoin(RcStrokeJoin.Round)
        }
        drawPath(path.getPath())
    }

    // Bubble radius scaling (area-proportional in the size channel).
    val radii =
        if (sizes != null) {
            var sLo = Float.MAX_VALUE
            var sHi = -Float.MAX_VALUE
            for (s in sizes) {
                if (s < sLo) sLo = s
                if (s > sHi) sHi = s
            }
            val rootLo = sqrt(maxOf(sLo, 0f))
            val rootSpan = (sqrt(maxOf(sHi, 0f)) - rootLo).let { if (it == 0f) 1f else it }
            FloatArray(n) { i ->
                minR + (sqrt(maxOf(sizes[i], 0f)) - rootLo) / rootSpan * (maxR - minR)
            }
        } else {
            FloatArray(n) { pointRadius }
        }

    val fill =
        if (sizes != null) Palette.fadeAlpha(color, theme.bubbleAlpha)
        else Palette.fadeAlpha(color, theme.pointAlpha)
    paint {
        color(fill)
        style(RcPaintStyle.Fill)
    }
    for (i in 0 until n) {
        drawCircle(xScale.map(xs[i]), cart.value.map(ys[i]), radii[i].rf)
    }
    if (sizes != null) {
        // Outline bubbles for definition.
        paint {
            color(color)
            style(RcPaintStyle.Stroke)
            strokeWidth(1.5f)
        }
        for (i in 0 until n) {
            drawCircle(xScale.map(xs[i]), cart.value.map(ys[i]), radii[i].rf)
        }
    }
}

/** A line through numeric (x, y) points on a both-numeric cartesian (e.g. ROC, fitted curve). */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderXYLine(
    cart: Cartesian,
    xs: FloatArray,
    ys: FloatArray,
    color: Int,
    width: Float,
) {
    val xScale = cart.xScale ?: return
    val n = minOf(xs.size, ys.size)
    if (n < 2) return
    val path = remotePath(xScale.map(xs[0]).toFloat(), cart.value.map(ys[0]).toFloat())
    for (i in 1 until n) path.lineTo(xScale.map(xs[i]).toFloat(), cart.value.map(ys[i]).toFloat())
    paint {
        color(color)
        style(RcPaintStyle.Stroke)
        strokeWidth(width)
        strokeCap(RcStrokeCap.Round)
        strokeJoin(RcStrokeJoin.Round)
    }
    drawPath(path.getPath())
}

/** A straight fitted line `y = slope·x + intercept` across `[xLo, xHi]`. */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderRegressionLine(
    cart: Cartesian,
    slope: Float,
    intercept: Float,
    xLo: Float,
    xHi: Float,
    color: Int,
) {
    val xScale = cart.xScale ?: return
    val path =
        remotePath(xScale.map(xLo).toFloat(), cart.value.map(slope * xLo + intercept).toFloat())
    path.lineTo(xScale.map(xHi).toFloat(), cart.value.map(slope * xHi + intercept).toFloat())
    paint {
        color(color)
        style(RcPaintStyle.Stroke)
        strokeWidth(2.5f)
    }
    drawPath(path.getPath())
}
