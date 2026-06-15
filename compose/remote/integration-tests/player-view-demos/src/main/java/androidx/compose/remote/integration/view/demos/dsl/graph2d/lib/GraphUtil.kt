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
import androidx.compose.remote.creation.dsl.RcFontType
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcWeight

/**
 * Small drawing helpers shared by the chart chrome and the marks, kept backward-compatible (only
 * the stable 2D canvas DSL: `paint{}`, `drawRect`, `drawLine`, `drawTextAnchored`, `save/rotate`).
 * Each helper sets its own paint before drawing because the writer holds a single mutable paint
 * state (there is no paint stack).
 *
 * Text anchoring uses pan factors: `panX` −1 left / 0 center / 1 right, `panY` −1 top / 0 middle /
 * 1 bottom.
 */

/**
 * Rough host-side text width (px) — used for layout margins. ~0.56·size per char is a good mean.
 */
public fun estimateTextWidth(text: String, size: Float): Float = text.length * size * 0.56f

@Suppress("RestrictedApiAndroidX")
public fun RcCanvasScope.fillRect(
    color: Int,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
) {
    paint {
        color(color)
        style(RcPaintStyle.Fill)
    }
    drawRect(left, top, right, bottom)
}

/** Fill a rect whose value-axis edge is a reactive coordinate (e.g. a bar top). */
@Suppress("RestrictedApiAndroidX")
public fun RcCanvasScope.fillRectR(
    color: Int,
    left: RcFloat,
    top: RcFloat,
    right: RcFloat,
    bottom: RcFloat,
) {
    paint {
        color(color)
        style(RcPaintStyle.Fill)
    }
    drawRect(left, top, right, bottom)
}

@Suppress("RestrictedApiAndroidX")
public fun RcCanvasScope.strokeLine(
    color: Int,
    width: Float,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
) {
    paint {
        color(color)
        style(RcPaintStyle.Stroke)
        strokeWidth(width)
    }
    drawLine(x1, y1, x2, y2)
}

/** Reactive line (any endpoint may be a live coordinate). */
@Suppress("RestrictedApiAndroidX")
public fun RcCanvasScope.strokeLineR(
    color: Int,
    width: Float,
    x1: RcFloat,
    y1: RcFloat,
    x2: RcFloat,
    y2: RcFloat,
) {
    paint {
        color(color)
        style(RcPaintStyle.Stroke)
        strokeWidth(width)
    }
    drawLine(x1, y1, x2, y2)
}

/**
 * Encode a Dash path-effect spec for `paint { pathEffect(...) }`: alternating on/off [intervals] in
 * px. Pair every dashed draw with a trailing `paint { pathEffect(null) }` — paint state is a single
 * committed value, so an effect left set leaks into later draws.
 */
public fun dashEffect(vararg intervals: Float, phase: Float = 0f): FloatArray {
    val out = FloatArray(3 + intervals.size)
    out[0] = Float.fromBits(1) // PaintPathEffects.DASH
    out[1] = phase
    out[2] = Float.fromBits(intervals.size)
    intervals.copyInto(out, 3)
    return out
}

/** Reactive line stroked with a dash pattern (clears the path effect after drawing). */
@Suppress("RestrictedApiAndroidX")
public fun RcCanvasScope.strokeLineDashedR(
    color: Int,
    width: Float,
    dash: FloatArray?,
    x1: RcFloat,
    y1: RcFloat,
    x2: RcFloat,
    y2: RcFloat,
) {
    if (dash == null) {
        strokeLineR(color, width, x1, y1, x2, y2)
        return
    }
    paint {
        color(color)
        style(RcPaintStyle.Stroke)
        strokeWidth(width)
        pathEffect(dashEffect(*dash))
    }
    drawLine(x1, y1, x2, y2)
    paint { pathEffect(null) }
}

/** Reactive anchored text label (position may be a live coordinate). */
@Suppress("RestrictedApiAndroidX")
public fun RcCanvasScope.labelR(
    text: String,
    x: RcFloat,
    y: RcFloat,
    panX: Float,
    panY: Float,
    color: Int,
    size: Float,
    bold: Boolean = false,
) {
    if (text.isEmpty()) return
    paint {
        color(color)
        style(RcPaintStyle.Fill)
        textSize(size)
        typeface(RcFontType.Default, if (bold) RcWeight.Bold else RcWeight.Normal, italic = false)
    }
    drawTextAnchored(remoteText(text), x, y, panX.rf, panY.rf, 0)
}

/** Draw a text label (host string) anchored at ([x],[y]) with the given pan and style. */
@Suppress("RestrictedApiAndroidX")
public fun RcCanvasScope.label(
    text: String,
    x: Float,
    y: Float,
    panX: Float,
    panY: Float,
    color: Int,
    size: Float,
    bold: Boolean = false,
) {
    if (text.isEmpty()) return
    paint {
        color(color)
        style(RcPaintStyle.Fill)
        textSize(size)
        typeface(RcFontType.Default, if (bold) RcWeight.Bold else RcWeight.Normal, italic = false)
    }
    drawTextAnchored(remoteText(text), x, y, panX, panY, 0)
}

/** Draw a label rotated 90° counter-clockwise (for vertical y-axis titles). */
@Suppress("RestrictedApiAndroidX")
public fun RcCanvasScope.labelVertical(text: String, x: Float, y: Float, color: Int, size: Float) {
    if (text.isEmpty()) return
    save {
        rotate(-90f, x, y)
        label(text, x, y, 0f, 0f, color, size, bold = false)
    }
}

/** Rotated y-axis title where the vertical center [y] is a reactive coordinate. */
@Suppress("RestrictedApiAndroidX")
public fun RcCanvasScope.labelVerticalR(
    text: String,
    x: Float,
    y: RcFloat,
    color: Int,
    size: Float,
) {
    if (text.isEmpty()) return
    save {
        rotate((-90f).rf, x.rf, y)
        labelR(text, x.rf, y, 0f, 0f, color, size)
    }
}
