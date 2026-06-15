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
import androidx.compose.remote.creation.dsl.RcFloat

/** How the category axis positions items: padded [Band]s (bars) or endpoint [Point]s (lines). */
public enum class XKind {
    Band,
    Point,
}

/**
 * The resolved cartesian coordinate system for a chart. The plot's left/top edges are host pixels
 * (fixed margins), while the right/bottom edges and the scales are **reactive** [RcFloat]s derived
 * from the live `componentWidth()/componentHeight()`, so the chart fills whatever space its Canvas
 * gets. Built once by [computeCartesian]; consumed by the axis drawer and the marks.
 */
@Suppress("RestrictedApiAndroidX")
public class Cartesian(
    public val orientation: Orientation,
    public val plotLeft: Float,
    public val plotTop: Float,
    public val plotRight: RcFloat,
    public val plotBottom: RcFloat,
    public val plotWidth: RcFloat,
    public val plotHeight: RcFloat,
    public val band: BandScale,
    public val points: PointScale,
    public val xKind: XKind,
    public val value: LinearScale,
    public val valueAxis: NiceAxis,
    public val categories: List<String>,
    public val tickLabels: List<String>,
    public val theme: GraphTheme,
    public val bare: Boolean = false,
    /** When non-null the x axis is numeric (continuous) rather than categorical. */
    public val xScale: LinearScale? = null,
    public val xAxisNice: NiceAxis? = null,
    public val xTickLabels: List<String> = emptyList(),
) {
    /** Category-axis pixel of item [i] (band center or point position), reactive. */
    public fun categoryX(i: Int): RcFloat =
        if (xKind == XKind.Band) band.center(i) else points.position(i)
}

/**
 * Compute host-side margins from the (already formatted) labels + titles, then build the reactive
 * plot rect and scales from the live canvas size [compW] x [compH]. Margins are pure host
 * arithmetic (they depend only on label widths + theme); only the plot extent and scales are
 * reactive.
 */
@Suppress("RestrictedApiAndroidX")
public fun computeCartesian(
    orientation: Orientation,
    theme: GraphTheme,
    compW: RcFloat,
    compH: RcFloat,
    valueAxis: NiceAxis,
    categories: List<String>,
    tickLabels: List<String>,
    hasTitle: Boolean,
    hasSubtitle: Boolean,
    xAxisTitle: String?,
    yAxisTitle: String?,
    legendRows: Int,
    xKind: XKind = XKind.Band,
    bare: Boolean = false,
    xAxisNumeric: NiceAxis? = null,
    xTickLabels: List<String> = emptyList(),
    extraRightMargin: Float = 0f,
    valueLog: Boolean = false,
    xLog: Boolean = false,
): Cartesian {
    val n = categories.size.coerceAtLeast(1)
    val pad = theme.outerPad

    if (bare) {
        val inset = pad * 0.25f
        val pr = (compW - inset).flush()
        val pb = (compH - inset).flush()
        val pw = (compW - 2f * inset).flush()
        val ph = (compH - 2f * inset).flush()
        return Cartesian(
            orientation,
            inset,
            inset,
            pr,
            pb,
            pw,
            ph,
            BandScale(n, inset, pw),
            PointScale(n, inset, pw),
            xKind,
            LinearScale.of(valueAxis, pb, RcFloat(inset), valueLog),
            valueAxis,
            categories,
            tickLabels,
            theme,
            bare = true,
        )
    }

    val maxValueLabelW = tickLabels.maxOfOrNull { estimateTextWidth(it, theme.labelSize) } ?: 0f
    val maxCatLabelW = categories.maxOfOrNull { estimateTextWidth(it, theme.labelSize) } ?: 0f
    val labelLineH = theme.labelSize + theme.labelGap
    val axisTitleLineH = theme.axisTitleSize + theme.labelGap

    // Top margin: title + subtitle band + tick-label headroom.
    var top = pad
    if (hasTitle) top += theme.titleSize + theme.labelGap
    if (hasSubtitle) top += theme.subtitleSize + theme.labelGap
    if (hasTitle || hasSubtitle) top += theme.labelGap
    top += theme.labelSize * 0.7f

    val legendH = if (legendRows > 0) legendRows * (theme.legendSize + theme.labelGap) + pad else 0f

    val leftMargin: Float
    val bottomMargin: Float
    if (orientation == Orientation.Vertical) {
        leftMargin =
            pad +
                (if (yAxisTitle != null) axisTitleLineH else 0f) +
                maxValueLabelW +
                theme.tickLen +
                theme.labelGap
        bottomMargin = pad + legendH + labelLineH + (if (xAxisTitle != null) axisTitleLineH else 0f)
    } else {
        leftMargin =
            pad +
                (if (yAxisTitle != null) axisTitleLineH else 0f) +
                maxCatLabelW +
                theme.tickLen +
                theme.labelGap
        bottomMargin = pad + legendH + labelLineH + (if (xAxisTitle != null) axisTitleLineH else 0f)
    }
    val rightMargin = pad + extraRightMargin

    val plotLeft = leftMargin
    val plotTop = top
    val plotRight = (compW - rightMargin).flush()
    val plotBottom = (compH - bottomMargin).flush()
    val plotWidth = (compW - (rightMargin + plotLeft)).flush()
    val plotHeight = (compH - (bottomMargin + plotTop)).flush()

    val band: BandScale
    val points: PointScale
    val value: LinearScale
    if (orientation == Orientation.Vertical) {
        band = BandScale(n, plotLeft, plotWidth, theme.barPadInner, theme.barPadOuter)
        points = PointScale(n, plotLeft, plotWidth)
        value = LinearScale.of(valueAxis, plotBottom, RcFloat(plotTop), valueLog)
    } else {
        band = BandScale(n, plotTop, plotHeight, theme.barPadInner, theme.barPadOuter)
        points = PointScale(n, plotTop, plotHeight)
        value = LinearScale.of(valueAxis, RcFloat(plotLeft), plotRight, valueLog)
    }

    val xScale = xAxisNumeric?.let { LinearScale.of(it, RcFloat(plotLeft), plotRight, xLog) }

    return Cartesian(
        orientation,
        plotLeft,
        plotTop,
        plotRight,
        plotBottom,
        plotWidth,
        plotHeight,
        band,
        points,
        xKind,
        value,
        valueAxis,
        categories,
        tickLabels,
        theme,
        bare = false,
        xScale = xScale,
        xAxisNice = xAxisNumeric,
        xTickLabels = xTickLabels,
    )
}

/** Paint the chart background (full canvas), optional plot-area tint, title and subtitle. */
@Suppress("RestrictedApiAndroidX")
public fun RcCanvasScope.drawFrame(
    cart: Cartesian,
    compW: RcFloat,
    compH: RcFloat,
    title: String?,
    subtitle: String?,
) {
    val theme = cart.theme
    if (theme.background ushr 24 != 0) {
        fillRectR(theme.background, 0f.rf, 0f.rf, compW, compH)
    }
    if (theme.plotBackground ushr 24 != 0) {
        fillRectR(
            theme.plotBackground,
            cart.plotLeft.rf,
            cart.plotTop.rf,
            cart.plotRight,
            cart.plotBottom,
        )
    }

    var y = theme.outerPad + theme.titleSize
    if (title != null) {
        label(title, theme.outerPad, y, -1f, 1f, theme.titleColor, theme.titleSize, bold = true)
        y += theme.labelGap
    }
    if (subtitle != null) {
        y += theme.subtitleSize
        label(subtitle, theme.outerPad, y, -1f, 1f, theme.subtitleColor, theme.subtitleSize)
    }
}
