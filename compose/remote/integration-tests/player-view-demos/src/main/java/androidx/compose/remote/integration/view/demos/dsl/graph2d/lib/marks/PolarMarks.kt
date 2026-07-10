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
import androidx.compose.remote.creation.dsl.RcStrokeJoin
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.GraphTheme
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.Palette
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.fillRectR
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.labelR
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.strokeLineR
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Reactive center + radius shared by the polar marks, fitting the area between the reserves. */
@Suppress("RestrictedApiAndroidX")
private class Polar(val cx: RcFloat, val cy: RcFloat, val radius: RcFloat)

@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.polarOf(
    topReserve: Float,
    bottomReserve: Float,
    widthFrac: Float,
    heightFrac: Float,
): Polar {
    val compW = componentWidth()
    val compH = componentHeight()
    val cx = (compW * 0.5f).flush()
    val cy = ((compH - bottomReserve + topReserve) * 0.5f).flush()
    val avail = compH - (topReserve + bottomReserve)
    val radius = ((compW * widthFrac).min(avail * heightFrac)).flush()
    return Polar(cx, cy, radius)
}

/** Pie (or donut) slices with percentage labels. */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderPie(
    values: FloatArray,
    colors: IntArray,
    donut: Boolean,
    theme: GraphTheme,
    topReserve: Float,
    bottomReserve: Float,
) {
    val total = values.sum()
    if (total <= 0f) return
    val p = polarOf(topReserve, bottomReserve, 0.46f, 0.48f)
    val left = p.cx - p.radius
    val top = p.cy - p.radius
    val right = p.cx + p.radius
    val bottom = p.cy + p.radius
    val thickness = p.radius * theme.donutThicknessFrac
    val midR = p.radius * (1f - theme.donutThicknessFrac / 2f)

    var startAngle = -90f
    for (i in values.indices) {
        val sweep = values[i] / total * 360f
        val color = colors[i % colors.size]
        if (donut) {
            val ml = p.cx - midR
            val mt = p.cy - midR
            val mr = p.cx + midR
            val mb = p.cy + midR
            paint {
                color(color)
                style(RcPaintStyle.Stroke)
                strokeWidth(thickness)
            }
            drawArc(ml, mt, mr, mb, startAngle.rf, sweep.rf)
        } else {
            paint {
                color(color)
                style(RcPaintStyle.Fill)
            }
            drawSector(left, top, right, bottom, startAngle.rf, sweep.rf)
            paint {
                color(theme.background)
                style(RcPaintStyle.Stroke)
                strokeWidth(theme.pieBorderStroke)
            }
            drawSector(left, top, right, bottom, startAngle.rf, sweep.rf)
        }
        if (sweep >= 8f) {
            val mid = (startAngle + sweep / 2f) * (PI.toFloat() / 180f)
            val lr = if (donut) midR else p.radius * theme.pieLabelRadiusFrac
            val lx = p.cx + lr * cos(mid)
            val ly = p.cy + lr * sin(mid)
            val pct = (values[i] / total * 100f).roundToInt()
            val labelColor = if (donut) theme.labelColor else 0xFFFFFFFF.toInt()
            labelR("$pct%", lx, ly, 0f, 0f, labelColor, theme.labelSize, bold = true)
        }
        startAngle += sweep
    }
}

/** Radar (spider) chart: concentric grid, spokes, and one filled polygon per series. */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderRadar(
    axisLabels: List<String>,
    series: List<FloatArray>,
    colors: IntArray,
    maxVal: Float,
    rings: Int,
    theme: GraphTheme,
    topReserve: Float,
    bottomReserve: Float,
) {
    val nAxes = axisLabels.size
    if (nAxes < 3) return
    val p = polarOf(topReserve, bottomReserve, 0.40f, 0.42f)
    val cosA = FloatArray(nAxes)
    val sinA = FloatArray(nAxes)
    for (k in 0 until nAxes) {
        val a = (-90f + k * 360f / nAxes) * (PI.toFloat() / 180f)
        cosA[k] = cos(a)
        sinA[k] = sin(a)
    }

    fun vertexX(frac: Float, k: Int): RcFloat = p.cx + p.radius * (frac * cosA[k])
    fun vertexY(frac: Float, k: Int): RcFloat = p.cy + p.radius * (frac * sinA[k])

    fun polygon(fracOf: (Int) -> Float, stroke: Int, fill: Int?, width: Float) {
        val path = remotePath(vertexX(fracOf(0), 0).toFloat(), vertexY(fracOf(0), 0).toFloat())
        for (k in 1 until nAxes) path.lineTo(
            vertexX(fracOf(k), k).toFloat(),
            vertexY(fracOf(k), k).toFloat(),
        )
        path.close()
        if (fill != null) {
            paint {
                color(fill)
                style(RcPaintStyle.Fill)
            }
            drawPath(path.getPath())
        }
        paint {
            color(stroke)
            style(RcPaintStyle.Stroke)
            strokeWidth(width)
            strokeJoin(RcStrokeJoin.Round)
        }
        drawPath(path.getPath())
    }

    // Grid rings.
    for (ring in 1..rings) {
        val frac = ring.toFloat() / rings
        polygon({ frac }, theme.gridColor, null, theme.gridStroke)
    }
    // Spokes.
    for (k in 0 until nAxes) {
        strokeLineR(theme.gridColor, theme.gridStroke, p.cx, p.cy, vertexX(1f, k), vertexY(1f, k))
    }
    // Axis labels.
    for (k in 0 until nAxes) {
        val lx = p.cx + p.radius * (1.12f * cosA[k])
        val ly = p.cy + p.radius * (1.12f * sinA[k])
        labelR(axisLabels[k], lx, ly, 0f, 0f, theme.labelColor, theme.labelSize)
    }
    // Series.
    for (s in series.indices) {
        val data = series[s]
        val color = colors[s % colors.size]
        polygon(
            { k -> (if (k < data.size) data[k] else 0f) / maxVal },
            color,
            Palette.fadeAlpha(color, theme.radarFillAlpha),
            2f,
        )
        paint {
            color(color)
            style(RcPaintStyle.Fill)
        }
        for (k in 0 until nAxes) {
            val frac = (if (k < data.size) data[k] else 0f) / maxVal
            drawCircle(vertexX(frac, k), vertexY(frac, k), 3f.rf)
        }
    }
}

/** Fill the background (full canvas) for a polar chart. */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.polarBackground(theme: GraphTheme) {
    if (theme.background ushr 24 != 0) {
        fillRectR(theme.background, 0f.rf, 0f.rf, componentWidth(), componentHeight())
    }
}

private const val DEG = PI.toFloat() / 180f

/** Radial gauge: a 270° track + value arc with center value text and min/max labels. */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderGauge(
    valueText: String,
    label: String?,
    frac: Float,
    color: Int,
    minText: String,
    maxText: String,
    theme: GraphTheme,
    topReserve: Float,
    bottomReserve: Float,
) {
    val p = polarOf(topReserve, bottomReserve, 0.46f, 0.48f)
    val trackR = p.radius * theme.gaugeTrackFrac
    val thickness = p.radius * theme.gaugeThicknessFrac
    val left = p.cx - trackR
    val top = p.cy - trackR
    val right = p.cx + trackR
    val bottom = p.cy + trackR
    val start = theme.gaugeStartDeg
    val full = theme.gaugeSweepDeg
    paint {
        color(theme.gridColor)
        style(RcPaintStyle.Stroke)
        strokeWidth(thickness)
    }
    drawArc(left, top, right, bottom, start.rf, full.rf)
    paint {
        color(color)
        style(RcPaintStyle.Stroke)
        strokeWidth(thickness)
    }
    drawArc(left, top, right, bottom, start.rf, (full * frac.coerceIn(0f, 1f)).rf)
    labelR(valueText, p.cx, p.cy, 0f, 0f, theme.titleColor, theme.titleSize * 1.7f, bold = true)
    if (label != null) {
        labelR(label, p.cx, p.cy + p.radius * 0.34f, 0f, 0f, theme.labelColor, theme.axisTitleSize)
    }
    labelR(
        minText,
        p.cx + p.radius * (0.78f * cos(start * DEG)),
        p.cy + p.radius * (0.78f * sin(start * DEG)),
        0f,
        0f,
        theme.labelColor,
        theme.labelSize,
    )
    labelR(
        maxText,
        p.cx + p.radius * (0.78f * cos((start + full) * DEG)),
        p.cy + p.radius * (0.78f * sin((start + full) * DEG)),
        0f,
        0f,
        theme.labelColor,
        theme.labelSize,
    )
}

/** Concentric "racetrack" rings, one per category, each filled to value/max of a 270° sweep. */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderRadialBars(
    labels: List<String>,
    values: FloatArray,
    colors: IntArray,
    maxVal: Float,
    theme: GraphTheme,
    topReserve: Float,
    bottomReserve: Float,
) {
    val n = values.size
    if (n == 0 || maxVal <= 0f) return
    val p = polarOf(topReserve, bottomReserve, 0.46f, 0.48f)
    val start = -90f
    val full = theme.radialSweepDeg
    val ringStep = p.radius * (0.82f / n)
    val thickness = ringStep * theme.radialRingThicknessFrac
    for (i in 0 until n) {
        val rI = p.radius - ringStep * (i + 0.5f)
        val l = p.cx - rI
        val t = p.cy - rI
        val r = p.cx + rI
        val b = p.cy + rI
        paint {
            color(Palette.fadeAlpha(colors[i % colors.size], theme.radialTrackAlpha))
            style(RcPaintStyle.Stroke)
            strokeWidth(thickness)
        }
        drawArc(l, t, r, b, start.rf, full.rf)
        val frac = (values[i] / maxVal).coerceIn(0f, 1f)
        paint {
            color(colors[i % colors.size])
            style(RcPaintStyle.Stroke)
            strokeWidth(thickness)
        }
        drawArc(l, t, r, b, start.rf, (full * frac).rf)
        // Category label just left of the ring's top start.
        labelR(
            labels[i],
            p.cx - theme.labelGap,
            p.cy - rI,
            1f,
            0f,
            theme.labelColor,
            theme.labelSize,
        )
    }
}

/**
 * Nightingale rose (coxcomb): equal-angle sectors whose radius is area-proportional to the value.
 */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderRose(
    labels: List<String>,
    values: FloatArray,
    colors: IntArray,
    maxVal: Float,
    rings: Int,
    theme: GraphTheme,
    topReserve: Float,
    bottomReserve: Float,
) {
    val n = values.size
    if (n == 0 || maxVal <= 0f) return
    val p = polarOf(topReserve, bottomReserve, 0.42f, 0.44f)
    // Grid circles.
    paint {
        color(theme.gridColor)
        style(RcPaintStyle.Stroke)
        strokeWidth(theme.gridStroke)
    }
    for (ring in 1..rings) drawCircle(p.cx, p.cy, p.radius * (ring.toFloat() / rings))
    val sweep = 360f / n
    val start = -90f
    for (i in 0 until n) {
        val frac = sqrtFrac(values[i] / maxVal)
        val rI = p.radius * frac
        val l = p.cx - rI
        val t = p.cy - rI
        val r = p.cx + rI
        val b = p.cy + rI
        val a = start + i * sweep
        paint {
            color(Palette.fadeAlpha(colors[i % colors.size], theme.roseFillAlpha))
            style(RcPaintStyle.Fill)
        }
        drawSector(l, t, r, b, a.rf, sweep.rf)
        paint {
            color(theme.background)
            style(RcPaintStyle.Stroke)
            strokeWidth(1.5f)
        }
        drawSector(l, t, r, b, a.rf, sweep.rf)
        val mid = (a + sweep / 2f) * DEG
        labelR(
            labels[i],
            p.cx + p.radius * (1.12f * cos(mid)),
            p.cy + p.radius * (1.12f * sin(mid)),
            0f,
            0f,
            theme.labelColor,
            theme.labelSize,
        )
    }
}

/**
 * Polar bar chart: radial bars (sectors with angular gaps) at radius ∝ value, with grid circles.
 */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderPolarBars(
    labels: List<String>,
    values: FloatArray,
    colors: IntArray,
    maxVal: Float,
    rings: Int,
    theme: GraphTheme,
    topReserve: Float,
    bottomReserve: Float,
) {
    val n = values.size
    if (n == 0 || maxVal <= 0f) return
    val p = polarOf(topReserve, bottomReserve, 0.42f, 0.44f)
    paint {
        color(theme.gridColor)
        style(RcPaintStyle.Stroke)
        strokeWidth(theme.gridStroke)
    }
    for (ring in 1..rings) drawCircle(p.cx, p.cy, p.radius * (ring.toFloat() / rings))
    val slot = 360f / n
    val barSweep = slot * theme.polarBarSweepFrac
    val gap = (slot - barSweep) / 2f
    val start = -90f
    for (i in 0 until n) {
        val frac = (values[i] / maxVal).coerceIn(0f, 1f)
        val rI = p.radius * frac
        val l = p.cx - rI
        val t = p.cy - rI
        val r = p.cx + rI
        val b = p.cy + rI
        val a = start + i * slot + gap
        paint {
            color(colors[i % colors.size])
            style(RcPaintStyle.Fill)
        }
        drawSector(l, t, r, b, a.rf, barSweep.rf)
        val mid = (a + barSweep / 2f) * DEG
        labelR(
            labels[i],
            p.cx + p.radius * (1.12f * cos(mid)),
            p.cy + p.radius * (1.12f * sin(mid)),
            0f,
            0f,
            theme.labelColor,
            theme.labelSize,
        )
    }
}

private fun sqrtFrac(x: Float): Float = kotlin.math.sqrt(x.coerceIn(0f, 1f))
