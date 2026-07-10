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

import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcCanvasScope
import androidx.compose.remote.creation.dsl.RcDynamicPath
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.RcStrokeCap
import androidx.compose.remote.creation.dsl.RcStrokeJoin
import androidx.compose.remote.creation.dsl.arraySpline
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderAnnotation
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderBand
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderBars
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderCandles
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderErrorBars
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderEventLine
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderLines
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderRefBand
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderRefLine
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderRegressionLine
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderScatter
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.renderXYLine
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.marks.touchCrosshair

/**
 * Wrap chart content into a complete RemoteCompose document. The charts are canvas *components*
 * ([RcScope] extensions), so a document is just `createRcBuffer { … }`; this is a convenience for
 * the common case of one chart filling a fixed-size doc.
 *
 * ```
 * graph2dDoc(800, 500, "Quarterly Revenue") {
 *     barChart(quarters, revenue, yTitle = "Revenue (\$M)")
 * }
 * ```
 */
@Suppress("RestrictedApiAndroidX")
public fun graph2dDoc(
    width: Int = 800,
    height: Int = 500,
    description: String = "Chart",
    content: RcScope.() -> Unit,
): ByteArray =
    createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, width),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, height),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, description),
        content = content,
    )

/**
 * Draw a 2D chart as canvas content. This is an [RcScope] extension that emits a `Canvas`, so you
 * use it like any other canvas inside a `createRcBuffer { … }` or a layout, and the [modifier]
 * controls its size / padding / position. The chart fills whatever space the Canvas receives — its
 * plot rect, scales and axes are computed reactively from `componentWidth()/componentHeight()`.
 *
 * The block declares **layers** that share one coordinate system and draw in declaration order:
 * category-indexed marks ([ChartScope.bars], [ChartScope.line], [ChartScope.area],
 * [ChartScope.errorBars], [ChartScope.band], [ChartScope.candles]) mix freely — a combo chart is
 * just `bars(...); line(...)` — while the numeric-x marks ([ChartScope.points],
 * [ChartScope.xyLine], [ChartScope.fitLine]) use a continuous x axis instead.
 *
 * Convention: the **x axis is always the category axis** and the **y axis is always the value
 * axis**; `orientation = Horizontal` simply rotates the drawing. Auto-scales with nice ticks unless
 * pinned.
 *
 * ```
 * createRcBuffer(*tags) {
 *     chart2d("Quarterly Revenue", modifier = Modifier.fillMaxSize().padding(8f)) {
 *         xAxis { categories = listOf("Q1", "Q2", "Q3", "Q4") }
 *         yAxis { title = "Revenue (\$M)" }
 *         bars(listOf(12, 19, 7, 23))
 *         line(listOf(15, 15, 15, 20), name = "Target", markers = true)   // combo overlay
 *     }
 * }
 * ```
 */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.chart2d(
    title: String? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
    block: ChartScope.() -> Unit,
) {
    val scope = ChartScope(theme).apply(block)
    val effectiveTitle = title ?: scope.title
    Canvas(modifier = modifier) { renderChart(scope, effectiveTitle) }
}

/** Resolve domain + layout from the live canvas size, then draw the layers in declaration order. */
@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.renderChart(scope: ChartScope, title: String?) {
    val theme = scope.theme
    val compW = componentWidth()
    val compH = componentHeight()

    val numeric = scope.hasNumericLayers()
    require(!(numeric && (scope.hasCategoricalLayers() || scope.xAxisSpec.categories != null))) {
        "graph2d: points()/xyLine()/fitLine() use a numeric x axis and cannot be mixed with " +
            "category-indexed layers (bars/line/area/errorBars/band/candles) or xAxis categories"
    }
    if (numeric) {
        renderNumericChart(scope, title, compW, compH)
        return
    }

    val layers = scope.layers.toList()
    val n = scope.xAxisSpec.categories?.size ?: layers.maxOfOrNull { categoryExtent(it) } ?: 0
    if (n == 0) {
        drawFrame(buildEmptyCartesian(theme, compW, compH), compW, compH, title, scope.subtitle)
        return
    }
    val categories = scope.xAxisSpec.categories ?: (1..n).map { it.toString() }
    for (layer in layers) {
        val ext = categoryExtent(layer)
        if (ext > 0) requireSameSize("categories", n, layerName(layer), ext)
    }

    // Resolve palette colors by declaration order so combined layers + the legend stay consistent.
    // A lone bar series keeps color=null → per-category palette coloring (the classic bar chart).
    val specs = scope.seriesSpecs()
    var colorIdx = 0
    val resolved =
        specs.map { s ->
            val idx = colorIdx++
            when {
                s.color != null -> s
                s.kind == SeriesKind.Bar && specs.size == 1 -> s
                else -> s.withColor(theme.seriesColor(idx))
            }
        }
    val barSpecs = resolved.filter { it.kind == SeriesKind.Bar }
    require(barSpecs.none { it.secondary }) { "graph2d: only line/area series can be secondary" }
    val lineSpecs = resolved.filter { it.kind != SeriesKind.Bar && !it.secondary }
    val y2Specs = resolved.filter { it.kind != SeriesKind.Bar && it.secondary }
    require(y2Specs.isEmpty() || scope.orientation == Orientation.Vertical) {
        "graph2d: a secondary y axis requires Orientation.Vertical"
    }

    val xKind =
        if (barSpecs.isNotEmpty() || layers.any { it is CandlesLayer || it is ErrorBarsLayer }) {
            XKind.Band
        } else {
            XKind.Point
        }
    val percent =
        (barSpecs.isNotEmpty() && scope.barLayout == BarLayout.Percent) ||
            (lineSpecs.any { it.kind == SeriesKind.Area } && scope.areaLayout == AreaLayout.Percent)
    val includeZero =
        scope.yAxisSpec.includeZero
            ?: (barSpecs.isNotEmpty() || lineSpecs.any { it.kind == SeriesKind.Area })

    val (dataMin, dataMax) = valueDomain(scope, layers, barSpecs, lineSpecs, n)
    val valueAxis =
        resolveValueAxis(scope.yAxisSpec, dataMin, dataMax, includeZero, percent, "y axis")

    val format =
        scope.yAxisSpec.format
            ?: when {
                percent -> NumberFormat.percent(0)
                scope.yAxisSpec.log -> NumberFormat { si(it) }
                else -> NumberFormat.auto(valueAxis.step)
            }
    val tickLabels = valueAxis.ticks.map { format.format(it) }

    // Secondary (right) axis from the secondary-flagged series.
    var y2Axis: NiceAxis? = null
    var y2Labels: List<String> = emptyList()
    var extraRight = 0f
    if (y2Specs.isNotEmpty()) {
        var lo = Float.MAX_VALUE
        var hi = -Float.MAX_VALUE
        for (s in y2Specs) for (v in s.values) {
            if (v < lo) lo = v
            if (v > hi) hi = v
        }
        val axis =
            resolveValueAxis(
                scope.y2AxisSpec,
                lo,
                hi,
                scope.y2AxisSpec.includeZero ?: false,
                percent = false,
                what = "y2 axis",
            )
        val y2Format = scope.y2AxisSpec.format ?: NumberFormat.auto(axis.step)
        y2Axis = axis
        y2Labels = axis.ticks.map { y2Format.format(it) }
        val maxW = y2Labels.maxOfOrNull { estimateTextWidth(it, theme.labelSize) } ?: 0f
        extraRight +=
            theme.tickLen +
                theme.labelGap +
                maxW +
                (if (scope.y2AxisSpec.title != null)
                    theme.labelGap + theme.axisTitleSize + theme.labelGap
                else 0f)
    }

    val legendEntries: List<LegendEntry> =
        if (scope.showLegend()) {
            resolved.mapIndexed { i, s ->
                LegendEntry(s.name ?: "Series ${i + 1}", s.color ?: theme.seriesColor(i))
            }
        } else {
            emptyList()
        }
    val legendRight = scope.legendPosition == LegendPosition.Right && legendEntries.isNotEmpty()
    val legendRows = if (legendRight) 0 else legendRowCount(legendEntries)
    if (legendRight) extraRight += legendColumnWidth(legendEntries, theme)

    val cart =
        computeCartesian(
            scope.orientation,
            theme,
            compW,
            compH,
            valueAxis,
            categories,
            tickLabels,
            hasTitle = title != null && !scope.bare,
            hasSubtitle = scope.subtitle != null && !scope.bare,
            xAxisTitle = scope.xAxisSpec.title,
            yAxisTitle = scope.yAxisSpec.title,
            legendRows = legendRows,
            xKind = xKind,
            bare = scope.bare,
            extraRightMargin = extraRight,
            valueLog = scope.yAxisSpec.log && !percent,
        )

    // The secondary scale shares the plot rect; a shallow Cartesian copy carries it to the marks.
    val cart2 =
        if (y2Axis != null) {
            val y2Scale =
                LinearScale.of(y2Axis, cart.plotBottom, RcFloat(cart.plotTop), scope.y2AxisSpec.log)
            Cartesian(
                cart.orientation,
                cart.plotLeft,
                cart.plotTop,
                cart.plotRight,
                cart.plotBottom,
                cart.plotWidth,
                cart.plotHeight,
                cart.band,
                cart.points,
                cart.xKind,
                y2Scale,
                y2Axis,
                cart.categories,
                y2Labels,
                theme,
                cart.bare,
            )
        } else {
            null
        }

    if (!scope.bare) {
        drawFrame(cart, compW, compH, title, scope.subtitle)
        drawGrid(cart, valueGrid = scope.yAxisSpec.grid)
    }

    // Resolve each line/area series' data into ONE document array, shared by the line marks and
    // the touch crosshair (each remoteFloatArray call serializes a fresh copy into the doc).
    val interactiveOn =
        scope.interactive && barSpecs.isEmpty() && xKind == XKind.Point && lineSpecs.isNotEmpty()
    val stackedAreas =
        lineSpecs.any { it.kind == SeriesKind.Area } && scope.areaLayout != AreaLayout.Overlay
    val lineArrays: List<RcFloat> =
        if (lineSpecs.isNotEmpty() && (!stackedAreas || interactiveOn)) {
            lineSpecs.map { remoteFloatArray(it.values) }
        } else {
            emptyList()
        }
    val y2Arrays: List<RcFloat> = y2Specs.map { remoteFloatArray(it.values) }

    var barsDone = false
    var linesDone = false
    var y2Done = false
    for (layer in layers) {
        when (layer) {
            is SeriesLayer ->
                if (layer.spec.kind == SeriesKind.Bar) {
                    if (!barsDone) {
                        barsDone = true
                        renderBars(
                            cart,
                            barSpecs,
                            scope.barLayout,
                            scope.barStyle,
                            scope.animate,
                            scope.valueLabels,
                            scope.negativeColor,
                        )
                    }
                } else if (layer.spec.secondary) {
                    if (!y2Done && cart2 != null) {
                        y2Done = true
                        renderLines(cart2, y2Specs, y2Arrays, AreaLayout.Overlay, scope.animate)
                    }
                } else {
                    if (!linesDone) {
                        linesDone = true
                        renderLines(cart, lineSpecs, lineArrays, scope.areaLayout, scope.animate)
                    }
                }
            is ErrorBarsLayer ->
                renderErrorBars(
                    cart,
                    layer.values,
                    layer.minus,
                    layer.plus,
                    layer.color ?: theme.seriesColor(0),
                )
            is BandLayer ->
                renderBand(
                    cart,
                    layer.lower,
                    layer.upper,
                    layer.color ?: theme.seriesColor(0),
                    layer.alpha ?: theme.bandFillAlpha,
                )
            is CandlesLayer ->
                renderCandles(
                    cart,
                    layer.opens,
                    layer.highs,
                    layer.lows,
                    layer.closes,
                    layer.upColor ?: theme.seriesColor(2),
                    layer.downColor ?: theme.seriesColor(3),
                    layer.ohlc,
                )
            is RefLineLayer ->
                renderRefLine(
                    cart,
                    layer.value,
                    layer.label,
                    layer.color ?: theme.valueLabelColor,
                    layer.dash,
                )
            is RefBandLayer ->
                renderRefBand(
                    cart,
                    layer.from,
                    layer.to,
                    layer.label,
                    layer.color ?: theme.seriesColor(0),
                    layer.alpha ?: theme.refBandAlpha,
                )
            is AnnotationLayer -> {
                val i = categoryIndexOf(layer.category, categories, "annotate")
                renderAnnotation(
                    cart,
                    cart.categoryX(i),
                    cart.value.map(layer.y),
                    layer.text,
                    layer.color ?: theme.valueLabelColor,
                )
            }
            is EventLineLayer -> {
                val i = categoryIndexOf(layer.category, categories, "eventLine")
                renderEventLine(
                    cart,
                    cart.categoryX(i),
                    layer.label,
                    layer.color ?: theme.valueLabelColor,
                    layer.dash,
                )
            }
            else -> {}
        }
    }

    if (!scope.bare) {
        drawAxes(
            cart,
            scope.xAxisSpec.title,
            scope.yAxisSpec.title,
            scope.xAxisSpec.labelEvery,
            scope.xAxisSpec.labelAngle,
        )
        if (cart2 != null && y2Axis != null) {
            drawSecondaryAxis(cart, cart2.value, y2Axis, y2Labels, scope.y2AxisSpec.title)
        }
        if (legendEntries.isNotEmpty()) {
            if (legendRight) drawLegendColumn(legendEntries, compW, cart.plotTop, theme)
            else drawLegend(legendEntries, compW, compH, theme)
        }
    }
    if (interactiveOn) {
        touchCrosshair(
            cart,
            lineSpecs.map { it.name },
            lineArrays,
            lineSpecs.mapIndexed { i, s -> s.color ?: theme.seriesColor(i) },
            categories,
        )
    }
}

/** Resolve a value axis honoring explicit ticks > log > nice, with validation. */
private fun resolveValueAxis(
    spec: AxisSpec,
    dataMin: Float,
    dataMax: Float,
    includeZero: Boolean,
    percent: Boolean,
    what: String,
): NiceAxis {
    if (percent) return niceAxis(0f, 1f, spec.tickCount, includeZero = true)
    val explicit = spec.ticks
    if (explicit != null) return explicitAxis(explicit.map { it.toFloat() })
    if (spec.log) {
        val lo = spec.min ?: dataMin
        val hi = spec.max ?: dataMax
        require(lo > 0f) {
            "graph2d: $what is log-scaled but the domain reaches $lo — data must be positive"
        }
        return logAxis(lo, hi)
    }
    return niceAxis(
        spec.min ?: dataMin,
        spec.max ?: dataMax,
        spec.tickCount,
        includeZero = includeZero,
    )
}

/** Index of an annotation's category, with a helpful failure message. */
private fun categoryIndexOf(category: String?, categories: List<String>, what: String): Int {
    require(category != null) {
        "graph2d: $what(x = <number>) needs a numeric-x chart — use $what(category = \"…\") with categories"
    }
    val i = categories.indexOf(category)
    require(i >= 0) {
        "graph2d: $what category \"$category\" is not in xAxis categories $categories"
    }
    return i
}

/** Render a chart whose layers live on a continuous numeric x axis (scatter family). */
@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.renderNumericChart(
    scope: ChartScope,
    title: String?,
    compW: RcFloat,
    compH: RcFloat,
) {
    val theme = scope.theme
    val layers = scope.layers.toList()
    var xLo = Float.MAX_VALUE
    var xHi = -Float.MAX_VALUE
    var yLo = Float.MAX_VALUE
    var yHi = -Float.MAX_VALUE
    fun scan(xs: FloatArray, ys: FloatArray) {
        for (v in xs) {
            if (v < xLo) xLo = v
            if (v > xHi) xHi = v
        }
        for (v in ys) {
            if (v < yLo) yLo = v
            if (v > yHi) yHi = v
        }
    }
    for (l in layers) {
        when (l) {
            is PointsLayer -> scan(l.xs, l.ys)
            is XYLineLayer -> scan(l.xs, l.ys)
            else -> {}
        }
    }
    for (l in layers) {
        when (l) {
            is RefLineLayer -> {
                if (l.value < yLo) yLo = l.value
                if (l.value > yHi) yHi = l.value
            }
            is RefBandLayer -> {
                yLo = minOf(yLo, l.from, l.to)
                yHi = maxOf(yHi, l.from, l.to)
            }
            is AnnotationLayer -> {
                if (l.category == null) {
                    if (l.x < xLo) xLo = l.x
                    if (l.x > xHi) xHi = l.x
                    if (l.y < yLo) yLo = l.y
                    if (l.y > yHi) yHi = l.y
                }
            }
            is EventLineLayer -> {
                if (l.category == null) {
                    if (l.x < xLo) xLo = l.x
                    if (l.x > xHi) xHi = l.x
                }
            }
            else -> {}
        }
    }
    if (xLo > xHi) {
        xLo = 0f
        xHi = 1f
    }
    if (yLo > yHi) {
        yLo = 0f
        yHi = 1f
    }
    val xSpec = scope.xAxisSpec
    val ySpec = scope.yAxisSpec

    val xLabels: List<String>
    val xNice: NiceAxis
    if (xSpec.time) {
        require(!xSpec.log) { "graph2d: an axis cannot be both time and log" }
        val (axis, labels) = timeAxis(xSpec.min ?: xLo, xSpec.max ?: xHi, xSpec.tickCount)
        xNice = axis
        xLabels = labels
    } else {
        xNice =
            resolveValueAxis(
                xSpec,
                xLo,
                xHi,
                xSpec.includeZero ?: false,
                percent = false,
                what = "x axis",
            )
        val xFormat =
            xSpec.format
                ?: if (xSpec.log) NumberFormat { si(it) } else NumberFormat.auto(xNice.step)
        xLabels = xNice.ticks.map { xFormat.format(it) }
    }
    val yNice =
        resolveValueAxis(
            ySpec,
            yLo,
            yHi,
            ySpec.includeZero ?: false,
            percent = false,
            what = "y axis",
        )
    val yFormat =
        ySpec.format ?: if (ySpec.log) NumberFormat { si(it) } else NumberFormat.auto(yNice.step)
    val yLabels = yNice.ticks.map { yFormat.format(it) }

    val legendEntries: List<LegendEntry> =
        if (scope.showLegend()) {
            val named = layers.filterIsInstance<PointsLayer>().filter { it.name != null }
            named.mapIndexed { i, l -> LegendEntry(l.name!!, l.color ?: theme.seriesColor(i)) }
        } else {
            emptyList()
        }
    val legendRight = scope.legendPosition == LegendPosition.Right && legendEntries.isNotEmpty()
    val legendRows = if (legendRight) 0 else legendRowCount(legendEntries)
    val extraRight = if (legendRight) legendColumnWidth(legendEntries, theme) else 0f

    val cart =
        computeCartesian(
            Orientation.Vertical,
            theme,
            compW,
            compH,
            yNice,
            emptyList(),
            yLabels,
            hasTitle = title != null && !scope.bare,
            hasSubtitle = scope.subtitle != null && !scope.bare,
            xAxisTitle = xSpec.title,
            yAxisTitle = ySpec.title,
            legendRows = legendRows,
            xKind = XKind.Point,
            bare = scope.bare,
            xAxisNumeric = xNice,
            xTickLabels = xLabels,
            extraRightMargin = extraRight,
            valueLog = ySpec.log,
            xLog = xSpec.log,
        )
    if (!scope.bare) {
        drawFrame(cart, compW, compH, title, scope.subtitle)
        drawGrid(cart, valueGrid = ySpec.grid)
    }
    val xScale = cart.xScale!!
    var ci = 0
    for (l in layers) {
        when (l) {
            is PointsLayer -> {
                renderScatter(
                    cart,
                    l.xs,
                    l.ys,
                    l.sizes,
                    l.color ?: theme.seriesColor(ci),
                    l.connect,
                )
                ci++
            }
            is XYLineLayer -> {
                renderXYLine(
                    cart,
                    l.xs,
                    l.ys,
                    l.color ?: theme.seriesColor(ci),
                    l.width ?: theme.lineStroke,
                )
                ci++
            }
            is FitLineLayer ->
                renderRegressionLine(
                    cart,
                    l.slope,
                    l.intercept,
                    xNice.min,
                    xNice.max,
                    l.color ?: theme.seriesColor(3),
                )
            is RefLineLayer ->
                renderRefLine(cart, l.value, l.label, l.color ?: theme.valueLabelColor, l.dash)
            is RefBandLayer ->
                renderRefBand(
                    cart,
                    l.from,
                    l.to,
                    l.label,
                    l.color ?: theme.seriesColor(0),
                    l.alpha ?: theme.refBandAlpha,
                )
            is AnnotationLayer -> {
                require(l.category == null) {
                    "graph2d: annotate(category=\"${'$'}{l.category}\") needs xAxis categories — numeric-x charts use annotate(x, y, text)"
                }
                renderAnnotation(
                    cart,
                    xScale.map(l.x),
                    cart.value.map(l.y),
                    l.text,
                    l.color ?: theme.valueLabelColor,
                )
            }
            is EventLineLayer -> {
                require(l.category == null) {
                    "graph2d: eventLine(category=\"${'$'}{l.category}\") needs xAxis categories — numeric-x charts use eventLine(x)"
                }
                renderEventLine(
                    cart,
                    xScale.map(l.x),
                    l.label,
                    l.color ?: theme.valueLabelColor,
                    l.dash,
                )
            }
            else -> {}
        }
    }
    if (!scope.bare) {
        drawAxes(cart, xSpec.title, ySpec.title, xSpec.labelEvery, xSpec.labelAngle)
        if (legendEntries.isNotEmpty()) {
            if (legendRight) drawLegendColumn(legendEntries, compW, cart.plotTop, theme)
            else drawLegend(legendEntries, compW, compH, theme)
        }
    }
}

/** How many category slots a layer occupies (0 = not category-indexed). */
private fun categoryExtent(layer: ChartLayer): Int =
    when (layer) {
        is SeriesLayer -> layer.spec.values.size
        is ErrorBarsLayer -> layer.values.size
        is BandLayer -> layer.lower.size
        is CandlesLayer -> layer.opens.size
        else -> 0
    }

private fun layerName(layer: ChartLayer): String =
    when (layer) {
        is SeriesLayer ->
            layer.spec.name?.let { "series '$it' values" }
                ?: "${layer.spec.kind.name.lowercase()} series values"
        is ErrorBarsLayer -> "errorBars values"
        is BandLayer -> "band values"
        is CandlesLayer -> "candles"
        else -> "layer"
    }

/** Min/max of the value axis across all layers, accounting for stacking. */
private fun valueDomain(
    scope: ChartScope,
    layers: List<ChartLayer>,
    barSpecs: List<SeriesSpec>,
    lineSpecs: List<SeriesSpec>,
    n: Int,
): Pair<Float, Float> {
    var lo = Float.MAX_VALUE
    var hi = -Float.MAX_VALUE
    fun acc(v: Float) {
        if (v < lo) lo = v
        if (v > hi) hi = v
    }
    fun accStacked(specs: List<SeriesSpec>) {
        for (i in 0 until n) {
            var pos = 0f
            var neg = 0f
            for (s in specs) {
                val v = if (i < s.values.size) s.values[i] else 0f
                if (v >= 0f) pos += v else neg += v
            }
            acc(pos)
            acc(neg)
        }
    }
    if (barSpecs.isNotEmpty()) {
        if (scope.barLayout != BarLayout.Grouped) {
            accStacked(barSpecs)
        } else {
            for (s in barSpecs) for (v in s.values) acc(v)
        }
    }
    if (lineSpecs.isNotEmpty()) {
        val stacked =
            lineSpecs.any { it.kind == SeriesKind.Area } && scope.areaLayout != AreaLayout.Overlay
        if (stacked) accStacked(lineSpecs) else for (s in lineSpecs) for (v in s.values) acc(v)
    }
    for (layer in layers) {
        when (layer) {
            is ErrorBarsLayer ->
                for (i in layer.values.indices) {
                    acc(layer.values[i] + layer.plus[i])
                    acc(layer.values[i] - layer.minus[i])
                }
            is BandLayer -> {
                for (v in layer.lower) acc(v)
                for (v in layer.upper) acc(v)
            }
            is CandlesLayer -> {
                for (v in layer.lows) acc(v)
                for (v in layer.highs) acc(v)
            }
            is RefLineLayer -> acc(layer.value)
            is RefBandLayer -> {
                acc(layer.from)
                acc(layer.to)
            }
            is AnnotationLayer -> acc(layer.y)
            else -> {}
        }
    }
    if (lo > hi) return 0f to 1f
    return lo to hi
}

@Suppress("RestrictedApiAndroidX")
private fun buildEmptyCartesian(theme: GraphTheme, compW: RcFloat, compH: RcFloat): Cartesian =
    computeCartesian(
        Orientation.Vertical,
        theme,
        compW,
        compH,
        niceAxis(0f, 1f),
        emptyList(),
        emptyList(),
        hasTitle = true,
        hasSubtitle = false,
        xAxisTitle = null,
        yAxisTitle = null,
        legendRows = 0,
    )

// ---------------------------------------------------------------------------
// Bar family shortcuts — canvas components. The 90% case in one call.
// ---------------------------------------------------------------------------

/** Vertical bar/column chart for a single series. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.barChart(
    categories: List<String>,
    values: List<Number>,
    title: String? = null,
    yTitle: String? = null,
    theme: GraphTheme = GraphTheme.Light,
    horizontal: Boolean = false,
    valueLabels: Boolean = false,
    animate: Boolean = false,
    xTitle: String? = null,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    chart2d(title, theme, modifier) {
        orientation = if (horizontal) Orientation.Horizontal else Orientation.Vertical
        this.valueLabels = valueLabels
        this.animate = animate
        xAxis {
            this.categories = categories
            this.title = xTitle
        }
        yAxis { this.title = yTitle }
        bars(values)
    }

/** Horizontal bar chart (good for long category labels / rankings). */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.horizontalBarChart(
    categories: List<String>,
    values: List<Number>,
    title: String? = null,
    yTitle: String? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit = barChart(categories, values, title, yTitle, theme, horizontal = true, modifier = modifier)

/** Grouped (clustered) bars: several named series side-by-side per category. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.groupedBarChart(
    categories: List<String>,
    series: List<Pair<String, List<Number>>>,
    title: String? = null,
    yTitle: String? = null,
    theme: GraphTheme = GraphTheme.Light,
    xTitle: String? = null,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    chart2d(title, theme, modifier) {
        xAxis {
            this.categories = categories
            this.title = xTitle
        }
        yAxis { this.title = yTitle }
        barLayout = BarLayout.Grouped
        series.forEach { (name, v) -> bars(v, name) }
    }

/** Stacked (or 100%-stacked) bars from several named series. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.stackedBarChart(
    categories: List<String>,
    series: List<Pair<String, List<Number>>>,
    title: String? = null,
    yTitle: String? = null,
    percent: Boolean = false,
    theme: GraphTheme = GraphTheme.Light,
    xTitle: String? = null,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    chart2d(title, theme, modifier) {
        xAxis {
            this.categories = categories
            this.title = xTitle
        }
        yAxis { this.title = yTitle }
        barLayout = if (percent) BarLayout.Percent else BarLayout.Stacked
        series.forEach { (name, v) -> bars(v, name) }
    }

/** Lollipop chart: a minimalist single-series bar (stem + dot). */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.lollipopChart(
    categories: List<String>,
    values: List<Number>,
    title: String? = null,
    yTitle: String? = null,
    theme: GraphTheme = GraphTheme.Light,
    horizontal: Boolean = false,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    chart2d(title, theme, modifier) {
        orientation = if (horizontal) Orientation.Horizontal else Orientation.Vertical
        barStyle = BarStyle.Lollipop
        xAxis { this.categories = categories }
        yAxis { this.title = yTitle }
        bars(values)
    }

/** Diverging bars: positive/negative values colored differently from a zero baseline. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.divergingBarChart(
    categories: List<String>,
    values: List<Number>,
    title: String? = null,
    yTitle: String? = null,
    positiveColor: Int = 0xFF4393C3.toInt(),
    negativeColor: Int = 0xFFD6604D.toInt(),
    theme: GraphTheme = GraphTheme.Light,
    horizontal: Boolean = true,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    chart2d(title, theme, modifier) {
        orientation = if (horizontal) Orientation.Horizontal else Orientation.Vertical
        this.negativeColor = negativeColor
        xAxis { this.categories = categories }
        yAxis {
            this.title = yTitle
            includeZero = true
        }
        bars(values, color = positiveColor)
    }

/**
 * Combo chart: bars with one or more lines overlaid. With [secondaryLines] the lines map on their
 * own right-hand axis (independent units, e.g. revenue bars + conversion-% line). For full control
 * declare the layers yourself: `chart2d { bars(...); line(..., secondary = true); y2Axis { … } }`.
 */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.comboChart(
    categories: List<String>,
    bars: Pair<String, List<Number>>,
    lines: List<Pair<String, List<Number>>>,
    title: String? = null,
    yTitle: String? = null,
    secondaryLines: Boolean = false,
    y2Title: String? = null,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    chart2d(title, theme, modifier) {
        xAxis { this.categories = categories }
        yAxis { this.title = yTitle }
        bars(bars.second, bars.first)
        lines.forEach { (name, v) -> line(v, name, markers = true, secondary = secondaryLines) }
        if (secondaryLines) y2Axis { this.title = y2Title }
        legend = true
    }

// ---------------------------------------------------------------------------
// Line / area family shortcuts.
// ---------------------------------------------------------------------------

/** Single line chart over ordered categories. [interp] = Linear / Step / Spline. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.lineChart(
    categories: List<String>,
    values: List<Number>,
    title: String? = null,
    yTitle: String? = null,
    interp: LineInterp = LineInterp.Linear,
    markers: Boolean = true,
    interactive: Boolean = false,
    theme: GraphTheme = GraphTheme.Light,
    animate: Boolean = false,
    xTitle: String? = null,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    chart2d(title, theme, modifier) {
        this.animate = animate
        this.interactive = interactive
        xAxis {
            this.categories = categories
            this.title = xTitle
        }
        yAxis { this.title = yTitle }
        line(values, interp = interp, markers = markers)
    }

/** Multi-line chart: several named series sharing axes, with a legend. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.multiLineChart(
    categories: List<String>,
    series: List<Pair<String, List<Number>>>,
    title: String? = null,
    yTitle: String? = null,
    interp: LineInterp = LineInterp.Linear,
    markers: Boolean = false,
    interactive: Boolean = false,
    theme: GraphTheme = GraphTheme.Light,
    xTitle: String? = null,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    chart2d(title, theme, modifier) {
        this.interactive = interactive
        xAxis {
            this.categories = categories
            this.title = xTitle
        }
        yAxis { this.title = yTitle }
        series.forEach { (name, v) -> line(v, name, interp = interp, markers = markers) }
    }

/** Smoothed (spline) line chart. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.splineChart(
    categories: List<String>,
    values: List<Number>,
    title: String? = null,
    yTitle: String? = null,
    interactive: Boolean = false,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    lineChart(
        categories,
        values,
        title,
        yTitle,
        LineInterp.Spline,
        markers = true,
        interactive = interactive,
        theme = theme,
        modifier = modifier,
    )

/** Step line chart (values change at discrete intervals). */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.stepChart(
    categories: List<String>,
    values: List<Number>,
    title: String? = null,
    yTitle: String? = null,
    interactive: Boolean = false,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    lineChart(
        categories,
        values,
        title,
        yTitle,
        LineInterp.Step,
        markers = false,
        interactive = interactive,
        theme = theme,
        modifier = modifier,
    )

/** Area chart: a filled line. [interp] = Linear / Step / Spline. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.areaChart(
    categories: List<String>,
    values: List<Number>,
    title: String? = null,
    yTitle: String? = null,
    interp: LineInterp = LineInterp.Linear,
    interactive: Boolean = false,
    theme: GraphTheme = GraphTheme.Light,
    animate: Boolean = false,
    xTitle: String? = null,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    chart2d(title, theme, modifier) {
        this.animate = animate
        this.interactive = interactive
        xAxis {
            this.categories = categories
            this.title = xTitle
        }
        yAxis { this.title = yTitle }
        area(values, interp = interp)
    }

/** Stacked (or 100%-stacked) area chart from several named series. */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.stackedAreaChart(
    categories: List<String>,
    series: List<Pair<String, List<Number>>>,
    title: String? = null,
    yTitle: String? = null,
    percent: Boolean = false,
    theme: GraphTheme = GraphTheme.Light,
    modifier: Modifier = Modifier.fillMaxSize(),
): Unit =
    chart2d(title, theme, modifier) {
        areaLayout = if (percent) AreaLayout.Percent else AreaLayout.Stacked
        xAxis { this.categories = categories }
        yAxis { this.title = yTitle }
        series.forEach { (name, v) -> area(v, name) }
    }

// ---------------------------------------------------------------------------
// Sparkline — the minimal chrome-less canvas component.
// ---------------------------------------------------------------------------

/**
 * A chrome-less sparkline (no axes/title/legend) drawn as ordinary canvas content. Like every chart
 * here it is an [RcScope] extension, so you use it inside `createRcBuffer { … }` or a layout and
 * the [modifier] controls size / padding / background. The chart fills the canvas reactively.
 *
 * ```
 * createRcBuffer(*tags) {
 *     sparkline(prices, fill = true, modifier = Modifier.fillMaxSize().padding(12f))
 * }
 * ```
 */
@Suppress("RestrictedApiAndroidX")
public fun RcScope.sparkline(
    values: List<Number>,
    color: Int = 0xFF4C78A8.toInt(),
    fill: Boolean = false,
    interp: LineInterp = LineInterp.Spline,
    strokeWidth: Float = 2.5f,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    Canvas(modifier = modifier) { renderSparkline(values, color, fill, interp, strokeWidth) }
}

/** Samples used to draw a smoothed sparkline. */
private const val SPARK_SPLINE_STEPS = 56

/**
 * Reactive sparkline renderer: x spans the live canvas width, y maps the (host-known) data range.
 */
@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.renderSparkline(
    values: List<Number>,
    color: Int,
    fill: Boolean,
    interp: LineInterp,
    strokeWidth: Float,
) {
    val n = values.size
    if (n < 2) return
    val raw = FloatArray(n) { values[it].toFloat() }
    var dmin = raw[0]
    var dmax = raw[0]
    for (v in raw) {
        if (v < dmin) dmin = v
        if (v > dmax) dmax = v
    }
    if (dmin == dmax) {
        dmin -= 1f
        dmax += 1f
    }
    val span = dmax - dmin
    val data = remoteFloatArray(raw)

    val inset = strokeWidth + 2f
    val left = inset
    val right = (componentWidth() - inset).flush()
    val bottom = (componentHeight() - inset).flush()
    val plotW = (right - left).flush()
    val plotH = (bottom - inset).flush()
    val pixelsPerUnit = (plotH * (1f / span)).flush()

    fun yAt(v: RcFloat): RcFloat = bottom - (v - dmin) * pixelsPerUnit
    fun xAt(frac: Float): RcFloat = plotW * frac + left

    fun appendPoints(path: RcDynamicPath) {
        when (interp) {
            LineInterp.Spline ->
                for (k in 1..SPARK_SPLINE_STEPS) {
                    val t = k.toFloat() / SPARK_SPLINE_STEPS
                    path.lineTo(xAt(t).toFloat(), yAt(arraySpline(data, t)).toFloat())
                }
            LineInterp.Step ->
                for (i in 1 until n) {
                    val x = xAt(i.toFloat() / (n - 1)).toFloat()
                    path.lineTo(x, yAt(data[i - 1]).toFloat())
                    path.lineTo(x, yAt(data[i]).toFloat())
                }
            LineInterp.Linear ->
                for (i in 1 until n) {
                    path.lineTo(xAt(i.toFloat() / (n - 1)).toFloat(), yAt(data[i]).toFloat())
                }
        }
    }

    val firstY = if (interp == LineInterp.Spline) yAt(arraySpline(data, 0f)) else yAt(data[0])

    if (fill) {
        val ap = remotePath(left, firstY.toFloat())
        appendPoints(ap)
        ap.lineTo(right.toFloat(), bottom.toFloat())
        ap.lineTo(left, bottom.toFloat())
        ap.close()
        paint {
            color(Palette.fadeAlpha(color, 0.28f))
            style(RcPaintStyle.Fill)
        }
        drawPath(ap.getPath())
    }

    val lp = remotePath(left, firstY.toFloat())
    appendPoints(lp)
    paint {
        color(color)
        style(RcPaintStyle.Stroke)
        strokeWidth(strokeWidth)
        strokeCap(RcStrokeCap.Round)
        strokeJoin(RcStrokeJoin.Round)
    }
    drawPath(lp.getPath())
}
