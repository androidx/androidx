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

package androidx.compose.remote.integration.view.demos.dsl.graph2d.lib

import androidx.compose.remote.creation.dsl.RcCanvasScope
import kotlin.math.abs

/**
 * Draws the chart chrome — gridlines, tick marks, tick labels and axis titles — for a [Cartesian]
 * system, in either orientation. Tick *values* and labels are host-known, but their pixel positions
 * are reactive (the plot fills the live canvas), so this uses the reactive draw helpers.
 */

/** Light gridlines behind the data, one per value tick (plus numeric-x gridlines when present). */
@Suppress("RestrictedApiAndroidX")
public fun RcCanvasScope.drawGrid(cart: Cartesian, valueGrid: Boolean) {
    if (!valueGrid) return
    val t = cart.theme
    for (v in cart.valueAxis.ticks) {
        val p = cart.value.map(v)
        val isZero = abs(v) < cart.valueAxis.step * 1e-3f
        val color = if (isZero) t.zeroLineColor else t.gridColor
        val w = if (isZero) t.axisStroke else t.gridStroke
        val dash = if (isZero) null else t.gridDash
        if (cart.orientation == Orientation.Vertical) {
            strokeLineDashedR(color, w, dash, cart.plotLeft.rf, p, cart.plotRight, p)
        } else {
            strokeLineDashedR(color, w, dash, p, cart.plotTop.rf, p, cart.plotBottom)
        }
    }
    val xs = cart.xScale
    if (xs != null && cart.xAxisNice != null) {
        for (v in cart.xAxisNice.ticks) {
            val x = xs.map(v)
            strokeLineDashedR(
                t.gridColor,
                t.gridStroke,
                t.gridDash,
                x,
                cart.plotTop.rf,
                x,
                cart.plotBottom,
            )
        }
    }
}

/** Tick marks, numeric tick labels, category labels and axis titles. */
@Suppress("RestrictedApiAndroidX")
public fun RcCanvasScope.drawAxes(
    cart: Cartesian,
    xAxisTitle: String?,
    yAxisTitle: String?,
    xLabelEvery: Int = 1,
    xLabelAngle: Float = 0f,
) {
    val t = cart.theme
    if (cart.orientation == Orientation.Vertical) {
        drawValueAxisVertical(cart)
        if (cart.xScale != null) drawNumericXAxis(cart, xLabelEvery, xLabelAngle)
        else drawCategoryAxisVertical(cart, xLabelEvery, xLabelAngle)
        strokeLineR(
            t.axisColor,
            t.axisStroke,
            cart.plotLeft.rf,
            cart.plotTop.rf,
            cart.plotLeft.rf,
            cart.plotBottom,
        )
    } else {
        drawValueAxisHorizontal(cart)
        drawCategoryAxisHorizontal(cart)
        strokeLineR(
            t.axisColor,
            t.axisStroke,
            cart.plotLeft.rf,
            cart.plotBottom,
            cart.plotRight,
            cart.plotBottom,
        )
    }
    drawAxisTitles(cart, xAxisTitle, yAxisTitle)
}

@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.drawValueAxisVertical(cart: Cartesian) {
    val t = cart.theme
    val labelX = cart.plotLeft - t.tickLen - t.labelGap
    val tickX0 = cart.plotLeft - t.tickLen
    for (i in cart.valueAxis.ticks.indices) {
        val y = cart.value.map(cart.valueAxis.ticks[i])
        strokeLineR(t.axisColor, t.axisStroke, tickX0.rf, y, cart.plotLeft.rf, y)
        labelR(cart.tickLabels[i], labelX.rf, y, 1f, 0f, t.labelColor, t.labelSize)
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.drawCategoryAxisVertical(
    cart: Cartesian,
    every: Int = 1,
    angle: Float = 0f,
) {
    val t = cart.theme
    val labelY = cart.plotBottom + (t.tickLen + t.labelGap)
    val tickY1 = cart.plotBottom + t.tickLen
    for (i in cart.categories.indices) {
        val cx = cart.categoryX(i)
        strokeLineR(t.axisColor, t.axisStroke, cx, cart.plotBottom, cx, tickY1)
        if (i % every != 0) continue
        if (angle == 0f) {
            labelR(cart.categories[i], cx, labelY, 0f, -1f, t.labelColor, t.labelSize)
        } else {
            save {
                rotate(angle.rf, cx, labelY)
                labelR(
                    cart.categories[i],
                    cx,
                    labelY,
                    if (angle < 0f) 1f else -1f,
                    0f,
                    t.labelColor,
                    t.labelSize,
                )
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.drawNumericXAxis(cart: Cartesian, every: Int = 1, angle: Float = 0f) {
    val t = cart.theme
    val xs = cart.xScale ?: return
    val nice = cart.xAxisNice ?: return
    val labelY = cart.plotBottom + (t.tickLen + t.labelGap)
    val tickY1 = cart.plotBottom + t.tickLen
    for (i in nice.ticks.indices) {
        val x = xs.map(nice.ticks[i])
        strokeLineR(t.axisColor, t.axisStroke, x, cart.plotBottom, x, tickY1)
        if (i % every != 0) continue
        val text = cart.xTickLabels.getOrNull(i) ?: ""
        if (angle == 0f) {
            labelR(text, x, labelY, 0f, -1f, t.labelColor, t.labelSize)
        } else {
            save {
                rotate(angle.rf, x, labelY)
                labelR(text, x, labelY, if (angle < 0f) 1f else -1f, 0f, t.labelColor, t.labelSize)
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.drawValueAxisHorizontal(cart: Cartesian) {
    val t = cart.theme
    val labelY = cart.plotBottom + (t.tickLen + t.labelGap)
    val tickY1 = cart.plotBottom + t.tickLen
    for (i in cart.valueAxis.ticks.indices) {
        val x = cart.value.map(cart.valueAxis.ticks[i])
        strokeLineR(t.axisColor, t.axisStroke, x, cart.plotBottom, x, tickY1)
        labelR(cart.tickLabels[i], x, labelY, 0f, -1f, t.labelColor, t.labelSize)
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.drawCategoryAxisHorizontal(cart: Cartesian) {
    val t = cart.theme
    val labelX = cart.plotLeft - t.tickLen - t.labelGap
    val tickX0 = cart.plotLeft - t.tickLen
    for (i in cart.categories.indices) {
        val cy = cart.categoryX(i)
        strokeLineR(t.axisColor, t.axisStroke, tickX0.rf, cy, cart.plotLeft.rf, cy)
        labelR(cart.categories[i], labelX.rf, cy, 1f, 0f, t.labelColor, t.labelSize)
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.drawAxisTitles(
    cart: Cartesian,
    xAxisTitle: String?,
    yAxisTitle: String?,
) {
    val t = cart.theme
    val cxPlot = (cart.plotRight + cart.plotLeft) * 0.5f
    val cyPlot = (cart.plotBottom + cart.plotTop) * 0.5f
    if (xAxisTitle != null) {
        val y =
            cart.plotBottom + (t.tickLen + t.labelGap + t.labelSize + t.labelGap + t.axisTitleSize)
        labelR(xAxisTitle, cxPlot, y, 0f, 1f, t.axisTitleColor, t.axisTitleSize, bold = true)
    }
    if (yAxisTitle != null) {
        val maxLabelW =
            if (cart.orientation == Orientation.Vertical) {
                cart.tickLabels.maxOfOrNull { estimateTextWidth(it, t.labelSize) } ?: 0f
            } else {
                cart.categories.maxOfOrNull { estimateTextWidth(it, t.labelSize) } ?: 0f
            }
        val x = cart.plotLeft - t.tickLen - t.labelGap - maxLabelW - t.labelGap
        labelVerticalR(yAxisTitle, x, cyPlot, t.axisTitleColor, t.axisTitleSize)
    }
}

/**
 * The secondary (right) value axis: axis line at the plot's right edge, outward ticks,
 * left-anchored labels and an optional rotated title. [scale] maps the secondary domain onto the
 * shared plot rect. Vertical-orientation charts only.
 */
@Suppress("RestrictedApiAndroidX")
public fun RcCanvasScope.drawSecondaryAxis(
    cart: Cartesian,
    scale: LinearScale,
    axis: NiceAxis,
    labels: List<String>,
    title: String?,
) {
    val t = cart.theme
    strokeLineR(
        t.axisColor,
        t.axisStroke,
        cart.plotRight,
        cart.plotTop.rf,
        cart.plotRight,
        cart.plotBottom,
    )
    val labelX = cart.plotRight + (t.tickLen + t.labelGap)
    for (i in axis.ticks.indices) {
        val y = scale.map(axis.ticks[i])
        strokeLineR(t.axisColor, t.axisStroke, cart.plotRight, y, cart.plotRight + t.tickLen, y)
        labelR(labels.getOrNull(i) ?: "", labelX, y, -1f, 0f, t.labelColor, t.labelSize)
    }
    if (title != null) {
        val maxLabelW = labels.maxOfOrNull { estimateTextWidth(it, t.labelSize) } ?: 0f
        val x = cart.plotRight + (t.tickLen + t.labelGap + maxLabelW + t.labelGap + t.axisTitleSize)
        val cy = (cart.plotBottom + cart.plotTop) * 0.5f
        save {
            rotate(90f.rf, x, cy)
            labelR(title, x, cy, 0f, 0f, t.axisTitleColor, t.axisTitleSize)
        }
    }
}
