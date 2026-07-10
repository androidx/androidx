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
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.Orientation
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.Palette
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.labelR
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.strokeLineDashedR

/**
 * Annotation marks shared by every cartesian chart: reference lines/bands on the value axis,
 * vertical event markers on the x axis, and point callouts. All positions map through the chart's
 * reactive scales, so annotations reflow with the canvas like the data marks do.
 */

/** Reference line at [value] on the primary value axis, with an optional end label. */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderRefLine(
    cart: Cartesian,
    value: Float,
    label: String?,
    color: Int,
    dash: FloatArray?,
) {
    val theme = cart.theme
    val p = cart.value.map(value)
    if (cart.orientation == Orientation.Vertical) {
        strokeLineDashedR(color, theme.axisStroke, dash, cart.plotLeft.rf, p, cart.plotRight, p)
        if (label != null) {
            labelR(
                label,
                cart.plotRight,
                p - theme.labelGap,
                1f,
                1f,
                color,
                theme.labelSize,
                bold = true,
            )
        }
    } else {
        strokeLineDashedR(color, theme.axisStroke, dash, p, cart.plotTop.rf, p, cart.plotBottom)
        if (label != null) {
            labelR(
                label,
                p + theme.labelGap,
                cart.plotTop.rf + theme.labelSize,
                -1f,
                0f,
                color,
                theme.labelSize,
                bold = true,
            )
        }
    }
}

/** Shaded band between [from] and [to] on the primary value axis. */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderRefBand(
    cart: Cartesian,
    from: Float,
    to: Float,
    label: String?,
    color: Int,
    alpha: Float,
) {
    val theme = cart.theme
    val p0 = cart.value.map(minOf(from, to))
    val p1 = cart.value.map(maxOf(from, to))
    paint {
        color(Palette.fadeAlpha(color, alpha))
        style(RcPaintStyle.Fill)
    }
    if (cart.orientation == Orientation.Vertical) {
        // p1 (larger value) maps to the smaller y — it is the rect top.
        drawRect(cart.plotLeft.rf, p1, cart.plotRight, p0)
        if (label != null) {
            labelR(
                label,
                cart.plotLeft.rf + theme.labelGap,
                p1 + (theme.labelGap + theme.labelSize * 0.5f),
                -1f,
                0f,
                color,
                theme.labelSize,
            )
        }
    } else {
        drawRect(p0, cart.plotTop.rf, p1, cart.plotBottom)
        if (label != null) {
            labelR(
                label,
                p0 + theme.labelGap,
                cart.plotTop.rf + theme.labelSize,
                -1f,
                0f,
                color,
                theme.labelSize,
            )
        }
    }
}

/**
 * Event marker crossing the plot at category-axis position [pos] (category center or numeric-x
 * pixel) — vertical in a vertical-orientation chart, horizontal otherwise.
 */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderEventLine(
    cart: Cartesian,
    pos: RcFloat,
    label: String?,
    color: Int,
    dash: FloatArray?,
) {
    val theme = cart.theme
    if (cart.orientation == Orientation.Vertical) {
        strokeLineDashedR(color, theme.axisStroke, dash, pos, cart.plotTop.rf, pos, cart.plotBottom)
        if (label != null) {
            labelR(
                label,
                pos + theme.labelGap,
                cart.plotTop.rf + theme.labelSize,
                -1f,
                0f,
                color,
                theme.labelSize,
                bold = true,
            )
        }
    } else {
        strokeLineDashedR(color, theme.axisStroke, dash, cart.plotLeft.rf, pos, cart.plotRight, pos)
        if (label != null) {
            labelR(
                label,
                cart.plotRight - theme.labelGap,
                pos - theme.labelGap,
                1f,
                1f,
                color,
                theme.labelSize,
                bold = true,
            )
        }
    }
}

/** Point callout: a dot at the reactive ([x], [y]) with [text] floating above it. */
@Suppress("RestrictedApiAndroidX")
internal fun RcCanvasScope.renderAnnotation(
    cart: Cartesian,
    x: RcFloat,
    y: RcFloat,
    text: String,
    color: Int,
) {
    val theme = cart.theme
    paint {
        color(color)
        style(RcPaintStyle.Fill)
    }
    drawCircle(x, y, theme.annotationDotRadius.rf)
    labelR(
        text,
        x,
        y - (theme.annotationDotRadius + theme.labelGap),
        0f,
        1f,
        color,
        theme.labelSize,
        bold = true,
    )
}
