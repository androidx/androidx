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
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.Cartesian
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.GraphTheme
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.estimateTextWidth
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.fillRectR
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.labelR
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.strokeLineR

/**
 * Comparison / ranking marks: dot plot, dumbbell, candlestick (cartesian) + slope & population
 * pyramid (custom reactive layouts).
 */

/** Cleveland dot plot: a guide line + dot per category on a horizontal cartesian. */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderDotPlot(cart: Cartesian, values: FloatArray, color: Int) {
    for (i in values.indices) {
        val cy = cart.band.center(i)
        val x = cart.value.map(values[i])
        strokeLineR(cart.theme.gridColor, cart.theme.gridStroke + 1f, cart.plotLeft.rf, cy, x, cy)
        paint {
            color(color)
            style(RcPaintStyle.Fill)
        }
        drawCircle(x, cy, 6f.rf)
    }
}

/** Dumbbell chart: two values per category joined by a bar, with endpoint dots. */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderDumbbell(
    cart: Cartesian,
    a: FloatArray,
    b: FloatArray,
    colorA: Int,
    colorB: Int,
) {
    for (i in a.indices) {
        val cy = cart.band.center(i)
        val xa = cart.value.map(a[i])
        val xb = cart.value.map(b[i])
        strokeLineR(cart.theme.zeroLineColor, 3f, xa, cy, xb, cy)
        paint {
            color(colorA)
            style(RcPaintStyle.Fill)
        }
        drawCircle(xa, cy, 6.5f.rf)
        paint {
            color(colorB)
            style(RcPaintStyle.Fill)
        }
        drawCircle(xb, cy, 6.5f.rf)
    }
}

/**
 * Candlestick chart: open/high/low/close candles on a vertical cartesian. [ohlc] = bars not
 * candles.
 */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderCandles(
    cart: Cartesian,
    opens: FloatArray,
    highs: FloatArray,
    lows: FloatArray,
    closes: FloatArray,
    upColor: Int,
    downColor: Int,
    ohlc: Boolean,
) {
    for (i in opens.indices) {
        val cx = cart.band.center(i)
        val half = cart.band.bandWidth * cart.theme.candleBodyFrac
        val up = closes[i] >= opens[i]
        val color = if (up) upColor else downColor
        val yO = cart.value.map(opens[i])
        val yC = cart.value.map(closes[i])
        val yH = cart.value.map(highs[i])
        val yL = cart.value.map(lows[i])
        if (ohlc) {
            strokeLineR(color, 2f, cx, yH, cx, yL)
            strokeLineR(color, 2f, cx - half, yO, cx, yO) // open tick left
            strokeLineR(color, 2f, cx, yC, cx + half, yC) // close tick right
        } else {
            strokeLineR(color, cart.theme.candleWickStroke, cx, yH, cx, yL)
            if (up) fillRectR(color, cx - half, yC, cx + half, yO)
            else fillRectR(color, cx - half, yO, cx + half, yC)
        }
    }
}

/** Manual reactive value→pixel (host domain, reactive pixel extent). */
@Suppress("RestrictedApiAndroidX")
private class VScale(val lo: Float, val span: Float, val pixelLo: RcFloat, val scale: RcFloat) {
    fun map(v: Float): RcFloat = pixelLo - scale * (v - lo)
}

@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.vscale(
    yLo: Float,
    yHi: Float,
    plotTop: Float,
    plotBottom: RcFloat,
): VScale {
    val span = (yHi - yLo).let { if (it == 0f) 1f else it }
    val scale = ((plotBottom - plotTop) * (1f / span)).flush()
    return VScale(yLo, span, plotBottom, scale)
}

/** Slope chart: each item is a line connecting its left value to its right value (two periods). */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderSlope(
    labels: List<String>,
    left: FloatArray,
    right: FloatArray,
    leftHeader: String,
    rightHeader: String,
    theme: GraphTheme,
    topReserve: Float,
) {
    val pad = theme.outerPad
    val compW = componentWidth()
    val compH = componentHeight()
    val labelW =
        (labels.maxOfOrNull { estimateTextWidth(it, theme.labelSize) } ?: 0f) + theme.labelGap
    val xLeft = pad + labelW + 28f
    val plotTop = topReserve + theme.labelSize + theme.labelGap
    val plotBottom = (compH - pad).flush()
    val xRight = (compW - pad - labelW - 28f).flush()
    var yLo = Float.MAX_VALUE
    var yHi = -Float.MAX_VALUE
    for (v in left) {
        if (v < yLo) yLo = v
        if (v > yHi) yHi = v
    }
    for (v in right) {
        if (v < yLo) yLo = v
        if (v > yHi) yHi = v
    }
    val pad2 = (yHi - yLo) * 0.08f
    val vs = vscale(yLo - pad2, yHi + pad2, plotTop, plotBottom)
    // Headers.
    labelR(
        leftHeader,
        xLeft.rf,
        (topReserve + theme.labelSize).rf,
        0f,
        1f,
        theme.titleColor,
        theme.axisTitleSize,
        bold = true,
    )
    labelR(
        rightHeader,
        xRight,
        (topReserve + theme.labelSize).rf,
        0f,
        1f,
        theme.titleColor,
        theme.axisTitleSize,
        bold = true,
    )
    for (i in labels.indices) {
        val color = theme.seriesColor(i)
        val ya = vs.map(left[i])
        val yb = vs.map(right[i])
        strokeLineR(color, 2.5f, xLeft.rf, ya, xRight, yb)
        paint {
            color(color)
            style(RcPaintStyle.Fill)
        }
        drawCircle(xLeft.rf, ya, 5f.rf)
        drawCircle(xRight, yb, 5f.rf)
        labelR(
            labels[i],
            (xLeft - theme.labelGap).rf,
            ya,
            1f,
            0f,
            theme.labelColor,
            theme.labelSize,
        )
    }
}

/** Population pyramid: back-to-back horizontal bars (two groups) per category (age band). */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderPyramid(
    bands: List<String>,
    leftVals: FloatArray,
    rightVals: FloatArray,
    leftColor: Int,
    rightColor: Int,
    theme: GraphTheme,
    topReserve: Float,
    bottomReserve: Float,
) {
    val n = bands.size
    if (n == 0) return
    val pad = theme.outerPad
    val compW = componentWidth()
    val compH = componentHeight()
    val centerGap =
        (bands.maxOfOrNull { estimateTextWidth(it, theme.labelSize) } ?: 0f) + theme.labelGap * 2f
    var maxV = 0f
    for (v in leftVals) if (v > maxV) maxV = v
    for (v in rightVals) if (v > maxV) maxV = v
    if (maxV <= 0f) maxV = 1f
    val cx = (compW * 0.5f).flush()
    val plotTop = topReserve
    val areaH = (compH - (topReserve + bottomReserve)).flush()
    val rowH = (areaH * (1f / n)).flush()
    val barHalf = rowH * 0.36f
    val sideMax = (compW * 0.5f - pad - centerGap * 0.5f).flush()
    fun w(v: Float): RcFloat = sideMax * (v / maxV)
    val lStart = cx - centerGap * 0.5f
    val rStart = cx + centerGap * 0.5f
    for (i in 0 until n) {
        val yMid = rowH * (i + 0.5f) + plotTop
        val wl = w(leftVals[i])
        val wr = w(rightVals[i])
        fillRectR(leftColor, lStart - wl, yMid - barHalf, lStart, yMid + barHalf)
        fillRectR(rightColor, rStart, yMid - barHalf, rStart + wr, yMid + barHalf)
        labelR(bands[i], cx, yMid, 0f, 0f, theme.labelColor, theme.labelSize)
    }
}
