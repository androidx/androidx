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

import android.annotation.SuppressLint
import androidx.compose.remote.creation.dsl.RcCanvasScope
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcFontType
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcTextFromFloatSpec
import androidx.compose.remote.creation.dsl.RcTouchStopMode
import androidx.compose.remote.creation.dsl.RcWeight
import androidx.compose.remote.creation.dsl.clamp
import androidx.compose.remote.creation.dsl.round
import androidx.compose.remote.creation.dsl.touchPosX
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.Cartesian
import androidx.compose.remote.integration.view.demos.dsl.graph2d.lib.strokeLineR

/**
 * Tier-1 touch scrubber: a draggable vertical crosshair that snaps to the nearest data index, marks
 * each series' point at that index, and shows a reactive readout (category + value(s)). Everything
 * is reactive — the finger's X position (`RemoteContext.FLOAT_TOUCH_POS_X`, clamped to the plot)
 * drives a reactive index, and `textLookup` / `createTextFromFloat` resolve the labels on the
 * player.
 *
 * Wired into the point-scale (line / area) charts via `chart2d { interactive = true }`.
 */
@Suppress("RestrictedApiAndroidX")
@SuppressLint("PrimitiveInCollection", "deprecation", "UnknownNullness")
internal fun RcCanvasScope.touchCrosshair(
    cart: Cartesian,
    names: List<String?>,
    arrays: List<RcFloat>,
    colors: List<Int>,
    categories: List<String>,
) {
    val n = categories.size
    if (n < 2 || arrays.isEmpty()) return
    val theme = cart.theme
    val catList = remoteArrayOf(*categories.toTypedArray())

    // Finger X scaled from window pixels into the chart's drawing space (identity when the doc
    // renders 1:1, i.e. componentWidth == windowWidth), clamped reactively to the plot rect. Stop
    // *instantly* on release so the crosshair stays where you lift, then → nearest index → snapped
    // X.
    val touchExpr = touchPosX() * componentWidth() / windowWidth()
    val touchX =
        addTouch(
                cart.plotLeft + 1f,
                cart.plotLeft.rf,
                cart.plotRight,
                RcTouchStopMode.Instantly,
                touchExpr,
            )
            .flush()
    val t = (touchX - cart.plotLeft) / cart.plotWidth
    val idx = clamp(0f.rf, (n - 1).rf, round(t * (n - 1f))).flush()
    val snapX = (cart.plotWidth * (idx * (1f / (n - 1))) + cart.plotLeft).flush()

    // Crosshair.
    strokeLineR(
        theme.zeroLineColor,
        theme.crosshairStroke,
        snapX,
        cart.plotTop.rf,
        snapX,
        cart.plotBottom,
    )

    // Point markers at the selected index (ring + inner dot in the bg color).
    for (si in arrays.indices) {
        val py = cart.value.mapR(arrays[si][idx])
        paint {
            color(colors[si])
            style(RcPaintStyle.Fill)
        }
        drawCircle(snapX, py, theme.crosshairMarkerRadius.rf)
        paint {
            color(theme.background)
            style(RcPaintStyle.Fill)
        }
        drawCircle(snapX, py, 3f.rf)
    }

    // Readout panel — host position (top-left of plot), reactive content.
    val spec = RcTextFromFloatSpec.of(padPre = RcTextFromFloatSpec.PadPre.None)
    val px = cart.plotLeft + 8f
    var py = cart.plotTop + theme.labelSize + 6f
    paint {
        color(theme.titleColor)
        style(RcPaintStyle.Fill)
        textSize(theme.labelSize + 1f)
        typeface(RcFontType.Default, RcWeight.Bold, italic = false)
    }
    drawTextAnchored(textLookup(catList, idx), px, py, -1f, 0f, 0)
    val sw = theme.labelSize * 0.7f
    for (si in arrays.indices) {
        py += theme.labelSize + theme.labelGap
        paint {
            color(colors[si])
            style(RcPaintStyle.Fill)
        }
        drawRect(px, py - theme.labelSize * 0.5f, px + sw, py + theme.labelSize * 0.2f)
        val vText = createTextFromFloat(arrays[si][idx], 6, 1, spec)
        val nm = names[si]
        val text = if (nm != null) textMerge(remoteText("$nm  "), vText) else vText
        paint {
            color(theme.labelColor)
            style(RcPaintStyle.Fill)
            textSize(theme.labelSize)
            typeface(RcFontType.Default, RcWeight.Normal, italic = false)
        }
        drawTextAnchored(text, px + sw + theme.labelGap, py, -1f, 0f, 0)
    }
}
