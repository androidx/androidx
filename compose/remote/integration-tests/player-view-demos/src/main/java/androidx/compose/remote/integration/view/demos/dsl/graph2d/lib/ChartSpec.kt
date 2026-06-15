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

/** Orientation of a cartesian chart: bars rise [Vertical]ly (columns) or extend [Horizontal]ly. */
public enum class Orientation {
    Vertical,
    Horizontal,
}

/** How multiple bar series share a category slot. */
public enum class BarLayout {
    /** Side-by-side bars within each category (clustered/grouped). */
    Grouped,
    /** Segments stacked into one bar per category; value axis shows absolute totals. */
    Stacked,
    /** Like [Stacked] but each category normalized to 100% (composition). */
    Percent,
}

/** Visual treatment of a bar mark. */
public enum class BarStyle {
    /** Solid rectangle. */
    Bar,
    /** Thin stem + endpoint dot (lollipop / Cleveland style). */
    Lollipop,
}

/** What kind of mark a series draws. */
public enum class SeriesKind {
    Bar,
    /** Connected line (optionally with markers / fill). */
    Line,
    /** Line with the region beneath it filled to the baseline. */
    Area,
}

/** How a line/area series interpolates between points. */
public enum class LineInterp {
    /** Straight segments. */
    Linear,
    /** Horizontal-then-vertical steps (e.g. discrete state changes). */
    Step,
    /** Smoothed Catmull-Rom-style curve (via `arraySpline` sampling). */
    Spline,
}

/** How multiple area series combine. */
public enum class AreaLayout {
    /** Drawn on top of each other (each filled to the baseline). */
    Overlay,
    /** Cumulatively stacked; value axis shows totals. */
    Stacked,
    /** Stacked then normalized so each x sums to 100%. */
    Percent,
}

/**
 * One plotted data series. [values] is plain host data (so domains/ticks are computed precisely);
 * the engine still maps it reactively at draw time. [color] `null` means "auto from the theme
 * palette by series index".
 */
public class SeriesSpec(
    public val values: FloatArray,
    public val name: String? = null,
    public val color: Int? = null,
    public val kind: SeriesKind = SeriesKind.Bar,
    public val interp: LineInterp = LineInterp.Linear,
    /** Markers (dots) at each data point, for line/area series. */
    public val markers: Boolean = false,
    /** Stroke width override (px); null ⇒ theme default. */
    public val strokeWidth: Float? = null,
    /** Dash intervals `[on, off, …]` in px; null ⇒ solid. Line/area stroke only. */
    public val dash: FloatArray? = null,
    /** Map this series on the right-hand y2 axis (line/area, vertical charts). */
    public val secondary: Boolean = false,
) {
    internal fun withColor(c: Int): SeriesSpec =
        SeriesSpec(values, name, c, kind, interp, markers, strokeWidth, dash, secondary)
}

/** Where the legend renders. */
public enum class LegendPosition {
    /** A centered row pinned to the bottom (the default). */
    Bottom,
    /** A column along the right edge of the chart. */
    Right,
}

// ---------------------------------------------------------------------------
// Layers — everything a cartesian chart draws, in declaration (z) order.
// ---------------------------------------------------------------------------

/**
 * One renderable layer of a cartesian chart. Layers share the chart's scales/axes and draw in the
 * order they were declared, so `bars(...); line(...)` puts the line on top of the bars. Created via
 * the [ChartScope] mark methods; the subclasses are internal.
 */
public sealed class ChartLayer

/** A bar/line/area series over the category axis. */
internal class SeriesLayer(val spec: SeriesSpec) : ChartLayer()

/** Scatter/bubble points on a numeric x axis. */
internal class PointsLayer(
    val xs: FloatArray,
    val ys: FloatArray,
    val sizes: FloatArray?,
    val color: Int?,
    val name: String?,
    val connect: Boolean,
) : ChartLayer()

/**
 * A polyline through numeric (x, y) points (fitted curve, reference diagonal, sampled function).
 */
internal class XYLineLayer(
    val xs: FloatArray,
    val ys: FloatArray,
    val color: Int?,
    val width: Float?,
) : ChartLayer()

/** A straight line `y = slope·x + intercept` spanning the resolved x domain. */
internal class FitLineLayer(val slope: Float, val intercept: Float, val color: Int?) : ChartLayer()

/** Per-category error bars (point + whiskers); asymmetric via distinct plus/minus. */
internal class ErrorBarsLayer(
    val values: FloatArray,
    val plus: FloatArray,
    val minus: FloatArray,
    val color: Int?,
) : ChartLayer()

/** A shaded band between two category-indexed series (confidence ribbon). */
internal class BandLayer(
    val lower: FloatArray,
    val upper: FloatArray,
    val color: Int?,
    val alpha: Float?,
) : ChartLayer()

/** OHLC candles (or open/close tick bars), one per category. */
internal class CandlesLayer(
    val opens: FloatArray,
    val highs: FloatArray,
    val lows: FloatArray,
    val closes: FloatArray,
    val upColor: Int?,
    val downColor: Int?,
    val ohlc: Boolean,
) : ChartLayer()

/** A horizontal reference line at a value on the primary value axis. */
internal class RefLineLayer(
    val value: Float,
    val label: String?,
    val color: Int?,
    val dash: FloatArray?,
) : ChartLayer()

/** A shaded horizontal region between two values on the primary value axis. */
internal class RefBandLayer(
    val from: Float,
    val to: Float,
    val label: String?,
    val color: Int?,
    val alpha: Float?,
) : ChartLayer()

/** A text callout (dot + label) at a data point. [category] non-null ⇒ categorical x. */
internal class AnnotationLayer(
    val category: String?,
    val x: Float,
    val y: Float,
    val text: String,
    val color: Int?,
) : ChartLayer()

/** A vertical marker line on the x axis. [category] non-null ⇒ categorical x. */
internal class EventLineLayer(
    val category: String?,
    val x: Float,
    val label: String?,
    val color: Int?,
    val dash: FloatArray?,
) : ChartLayer()

// ---------------------------------------------------------------------------
// Validation helpers — fail loudly at author time with actionable messages.
// ---------------------------------------------------------------------------

internal fun requireSameSize(aName: String, aSize: Int, bName: String, bSize: Int) {
    require(aSize == bSize) {
        "graph2d: $aName.size=$aSize but $bName.size=$bSize — they must match"
    }
}

internal fun requireFinite(values: FloatArray, what: String) {
    for (i in values.indices) {
        require(!values[i].isNaN() && !values[i].isInfinite()) {
            "graph2d: $what[$i] is ${values[i]} — all values must be finite"
        }
    }
}

internal fun List<Number>.toFloatArrayChecked(what: String): FloatArray =
    FloatArray(size) { this[it].toFloat() }.also { requireFinite(it, what) }

/**
 * Configuration for one axis. A categorical axis sets [categories]; a numeric (value) axis leaves
 * them null and is auto-scaled with nice ticks (override with [min]/[max]). All fields default to
 * sensible values so `xAxis { categories = … }` is usually all an agent needs.
 */
public class AxisSpec {
    /** Axis title (e.g. "Revenue ($M)"). */
    public var title: String? = null

    /** Category labels — set this to make the axis ordinal/categorical. */
    public var categories: List<String>? = null

    /** Explicit numeric domain; null ⇒ auto from data. */
    public var min: Float? = null
    public var max: Float? = null

    /** Draw gridlines for this axis. */
    public var grid: Boolean = true

    /** Target tick count for the value axis (1-2-5 nice algorithm). */
    public var tickCount: Int = 5

    /** Pull the value-axis baseline to zero; null ⇒ auto (true for bars/areas, false for lines). */
    public var includeZero: Boolean? = null

    /** Tick label formatter; null ⇒ auto from the tick step. */
    public var format: NumberFormat? = null

    /** Log10 scale (value axis, or numeric x). Data and domain must be positive. */
    public var log: Boolean = false

    /** Explicit tick values (sorted); overrides the nice-tick algorithm and pins the domain. */
    public var ticks: List<Number>? = null

    /** Draw every k-th tick/category label (declutter dense axes). Ticks still draw. */
    public var labelEvery: Int = 1

    /** Rotate tick/category labels by this many degrees (e.g. -45 for long category names). */
    public var labelAngle: Float = 0f

    /**
     * Treat numeric-x values as **epoch days** (days since 1970-01-01; see [daysFromCivil]) and use
     * calendar-aware ticks + date labels. Numeric-x charts only.
     */
    public var time: Boolean = false
}

/**
 * Collected, declarative description of a chart, populated by the `chart2d { … }` block and then
 * rendered as ordered layers sharing one coordinate system. Mirrors the visual structure (title,
 * axes, layers, legend) so agents can author charts that read like the picture.
 *
 * Layers may mix freely on the **category** axis (bars + line + errorBars + band + candles — e.g. a
 * combo chart is `bars(...); line(...)`). The **numeric-x** marks ([points], [xyLine], [fitLine])
 * use a continuous x axis instead and cannot be combined with category-indexed layers.
 */
public class ChartScope internal constructor(public var theme: GraphTheme) {
    /** Chart title; the `chart2d(title = …)` parameter takes precedence when both are set. */
    public var title: String? = null
    public var subtitle: String? = null

    /** Category-axis spec (backing field for the [xAxis] configuration block). */
    public val xAxisSpec: AxisSpec = AxisSpec()
    /** Value-axis spec (backing field for the [yAxis] configuration block). */
    public val yAxisSpec: AxisSpec = AxisSpec()
    /** Secondary (right) value-axis spec; active when any series sets `secondary = true`. */
    public val y2AxisSpec: AxisSpec = AxisSpec()

    /** Show a legend (auto when there is >1 named series). */
    public var legend: Boolean? = null

    /** Where the legend renders ([LegendPosition.Bottom] row or [LegendPosition.Right] column). */
    public var legendPosition: LegendPosition = LegendPosition.Bottom

    public var orientation: Orientation = Orientation.Vertical
    public var barLayout: BarLayout = BarLayout.Grouped
    public var barStyle: BarStyle = BarStyle.Bar

    /** How area series combine when there is more than one. */
    public var areaLayout: AreaLayout = AreaLayout.Overlay

    /**
     * "Bare" mode: drop all chrome (title, axes, grid, legend, margins) for inline/embedded use.
     */
    public var bare: Boolean = false

    /** Draw a value label above/beside each bar. */
    public var valueLabels: Boolean = false

    /** Animate a grow-in on first render (reactive, time-driven). */
    public var animate: Boolean = false

    /** Enable a draggable touch crosshair + readout (line/area charts). */
    public var interactive: Boolean = false

    /** For diverging bars: color for negative values (else palette/positive color is used). */
    public var negativeColor: Int? = null

    internal val layers: MutableList<ChartLayer> = mutableListOf()

    /** Configure the category/bottom axis. */
    public fun xAxis(block: AxisSpec.() -> Unit) {
        xAxisSpec.block()
    }

    /** Configure the value/left axis. */
    public fun yAxis(block: AxisSpec.() -> Unit) {
        yAxisSpec.block()
    }

    /**
     * Configure the secondary/right value axis (used by series declared with `secondary = true`).
     */
    public fun y2Axis(block: AxisSpec.() -> Unit) {
        y2AxisSpec.block()
    }

    /** Add a bar series from host floats. Call multiple times for grouped/stacked charts. */
    public fun bars(values: FloatArray, name: String? = null, color: Int? = null) {
        requireFinite(values, "bars values")
        layers.add(SeriesLayer(SeriesSpec(values, name, color)))
    }

    /** Add a bar series from any number list (convenience). */
    public fun bars(values: List<Number>, name: String? = null, color: Int? = null) {
        bars(FloatArray(values.size) { values[it].toFloat() }, name, color)
    }

    /**
     * Add a line series. Call multiple times for a multi-line chart. [dash] draws the stroke dashed
     * (`[on, off]` px); [secondary] maps the series on the right-hand [y2Axis].
     */
    public fun line(
        values: List<Number>,
        name: String? = null,
        color: Int? = null,
        interp: LineInterp = LineInterp.Linear,
        markers: Boolean = false,
        width: Float? = null,
        dash: FloatArray? = null,
        secondary: Boolean = false,
    ) {
        val v = values.toFloatArrayChecked("line values")
        layers.add(
            SeriesLayer(
                SeriesSpec(v, name, color, SeriesKind.Line, interp, markers, width, dash, secondary)
            )
        )
    }

    /** Add a smoothed (spline) line series. */
    public fun spline(values: List<Number>, name: String? = null, color: Int? = null) {
        line(values, name, color, LineInterp.Spline)
    }

    /** Add a step line series. */
    public fun steps(values: List<Number>, name: String? = null, color: Int? = null) {
        line(values, name, color, LineInterp.Step)
    }

    /** Add an area series (line filled to the baseline). Multiple combine per [areaLayout]. */
    public fun area(
        values: List<Number>,
        name: String? = null,
        color: Int? = null,
        interp: LineInterp = LineInterp.Linear,
        secondary: Boolean = false,
    ) {
        val v = values.toFloatArrayChecked("area values")
        layers.add(
            SeriesLayer(SeriesSpec(v, name, color, SeriesKind.Area, interp, secondary = secondary))
        )
    }

    // ----- Annotations — reference lines/bands, callouts, event markers -----

    /**
     * A horizontal reference line at [value] on the primary value axis (target, threshold,
     * average). Dashed by default; pass `dash = null` for solid.
     */
    public fun refLine(
        value: Number,
        label: String? = null,
        color: Int? = null,
        dash: FloatArray? = floatArrayOf(8f, 6f),
    ) {
        val v = value.toFloat()
        require(!v.isNaN() && !v.isInfinite()) { "graph2d: refLine value must be finite (was $v)" }
        layers.add(RefLineLayer(v, label, color, dash))
    }

    /** A shaded horizontal region between [from] and [to] on the primary value axis. */
    public fun refBand(
        from: Number,
        to: Number,
        label: String? = null,
        color: Int? = null,
        alpha: Float? = null,
    ) {
        layers.add(RefBandLayer(from.toFloat(), to.toFloat(), label, color, alpha))
    }

    /**
     * A callout (dot + [text]) at a category/value point. [category] must match xAxis categories.
     */
    public fun annotate(category: String, value: Number, text: String, color: Int? = null) {
        layers.add(AnnotationLayer(category, 0f, value.toFloat(), text, color))
    }

    /** A callout (dot + [text]) at a numeric (x, y) point — numeric-x charts. */
    public fun annotate(x: Number, y: Number, text: String, color: Int? = null) {
        layers.add(AnnotationLayer(null, x.toFloat(), y.toFloat(), text, color))
    }

    /** A vertical event marker at a category (release date, incident, regime change). */
    public fun eventLine(
        category: String,
        label: String? = null,
        color: Int? = null,
        dash: FloatArray? = floatArrayOf(8f, 6f),
    ) {
        layers.add(EventLineLayer(category, 0f, label, color, dash))
    }

    /** A vertical event marker at a numeric x — numeric-x charts. */
    public fun eventLine(
        x: Number,
        label: String? = null,
        color: Int? = null,
        dash: FloatArray? = floatArrayOf(8f, 6f),
    ) {
        layers.add(EventLineLayer(null, x.toFloat(), label, color, dash))
    }

    /**
     * Add scatter points on a **numeric x axis**. [sizes] turns the points into area-scaled
     * bubbles; [connect] joins them in order (connected scatter / trajectory).
     */
    public fun points(
        points: List<Pair<Number, Number>>,
        name: String? = null,
        color: Int? = null,
        sizes: List<Number>? = null,
        connect: Boolean = false,
    ) {
        val xs =
            FloatArray(points.size) { points[it].first.toFloat() }
                .also { requireFinite(it, "points x") }
        val ys =
            FloatArray(points.size) { points[it].second.toFloat() }
                .also { requireFinite(it, "points y") }
        val sz = sizes?.toFloatArrayChecked("points sizes")
        if (sz != null) requireSameSize("points", points.size, "sizes", sz.size)
        layers.add(PointsLayer(xs, ys, sz, color, name, connect))
    }

    /** Add a polyline through numeric (x, y) points (fitted curve, reference line, function). */
    public fun xyLine(
        points: List<Pair<Number, Number>>,
        color: Int? = null,
        width: Float? = null,
    ) {
        val xs =
            FloatArray(points.size) { points[it].first.toFloat() }
                .also { requireFinite(it, "xyLine x") }
        val ys =
            FloatArray(points.size) { points[it].second.toFloat() }
                .also { requireFinite(it, "xyLine y") }
        layers.add(XYLineLayer(xs, ys, color, width))
    }

    /** Add a straight `y = slope·x + intercept` line spanning the full (nice) x domain. */
    public fun fitLine(slope: Number, intercept: Number, color: Int? = null) {
        layers.add(FitLineLayer(slope.toFloat(), intercept.toFloat(), color))
    }

    /** Add symmetric ± error bars, one per category. Overlays bars/lines or stands alone. */
    public fun errorBars(values: List<Number>, errors: List<Number>, color: Int? = null) {
        errorBars(values, errors, errors, color)
    }

    /** Add asymmetric error bars: `value + plus[i]` up, `value - minus[i]` down. */
    public fun errorBars(
        values: List<Number>,
        plus: List<Number>,
        minus: List<Number>,
        color: Int? = null,
    ) {
        val v = values.toFloatArrayChecked("errorBars values")
        val p = plus.toFloatArrayChecked("errorBars plus")
        val m = minus.toFloatArrayChecked("errorBars minus")
        requireSameSize("errorBars values", v.size, "plus", p.size)
        requireSameSize("errorBars values", v.size, "minus", m.size)
        layers.add(ErrorBarsLayer(v, p, m, color))
    }

    /** Add a shaded band between [lower] and [upper] (confidence ribbon), category-indexed. */
    public fun band(
        lower: List<Number>,
        upper: List<Number>,
        color: Int? = null,
        alpha: Float? = null,
    ) {
        val lo = lower.toFloatArrayChecked("band lower")
        val hi = upper.toFloatArrayChecked("band upper")
        requireSameSize("band lower", lo.size, "upper", hi.size)
        layers.add(BandLayer(lo, hi, color, alpha))
    }

    /** Add OHLC candles, one per category. [ohlc] draws open/close ticks instead of bodies. */
    public fun candles(
        candles: List<Candle>,
        upColor: Int? = null,
        downColor: Int? = null,
        ohlc: Boolean = false,
    ) {
        val o = FloatArray(candles.size) { candles[it].open.toFloat() }
        val h = FloatArray(candles.size) { candles[it].high.toFloat() }
        val l = FloatArray(candles.size) { candles[it].low.toFloat() }
        val c = FloatArray(candles.size) { candles[it].close.toFloat() }
        requireFinite(o, "candles open")
        requireFinite(h, "candles high")
        requireFinite(l, "candles low")
        requireFinite(c, "candles close")
        layers.add(CandlesLayer(o, h, l, c, upColor, downColor, ohlc))
    }

    // ----- internal views over the layers -----

    internal fun seriesSpecs(): List<SeriesSpec> =
        layers.filterIsInstance<SeriesLayer>().map { it.spec }

    /** True if any layer positions itself on the category axis. */
    internal fun hasCategoricalLayers(): Boolean =
        layers.any {
            it is SeriesLayer || it is ErrorBarsLayer || it is BandLayer || it is CandlesLayer
        }

    /** True if any layer needs a continuous numeric x axis. */
    internal fun hasNumericLayers(): Boolean =
        layers.any { it is PointsLayer || it is XYLineLayer || it is FitLineLayer }

    /** Whether a legend should actually render given the data and the [legend] override. */
    internal fun showLegend(): Boolean {
        val named =
            layers.count {
                (it is SeriesLayer && it.spec.name != null) ||
                    (it is PointsLayer && it.name != null)
            }
        return !bare && (legend ?: (named > 1))
    }
}
