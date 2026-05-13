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

package androidx.compose.remote.integration.view.demos.dsl

import android.graphics.Color
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcCanvasScope
import androidx.compose.remote.creation.dsl.RcColor
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcFlowScope
import androidx.compose.remote.creation.dsl.RcHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcPath
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.RcSp
import androidx.compose.remote.creation.dsl.RcVerticalPositioning
import androidx.compose.remote.creation.dsl.Spacer
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.clip
import androidx.compose.remote.creation.dsl.componentId
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.fillMaxWidth
import androidx.compose.remote.creation.dsl.height
import androidx.compose.remote.creation.dsl.padding
import androidx.compose.remote.creation.dsl.rdp
import androidx.compose.remote.creation.dsl.rsp
import androidx.compose.remote.creation.dsl.setStyle
import androidx.compose.remote.creation.dsl.size
import androidx.compose.remote.creation.dsl.verticalScroll
import androidx.compose.remote.creation.dsl.widthIn
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import java.util.Random
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

@Suppress("RestrictedApiAndroidX") private lateinit var colors: RcTickerColors

private lateinit var fontSizes: RcTickerFontSizes

@Composable
@Preview
@Suppress("RestrictedApiAndroidX")
fun RcDslTickerPreview() {
    RemoteDocumentPreview(RemoteDocument(dslTicker()))
}

/** Reimplementation of RcTicker using the new type-safe DSL. */
@Suppress("RestrictedApiAndroidX")
fun dslTicker(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        colors = RcTickerColors(this)
        fontSizes = RcTickerFontSizes(this)

        Column(modifier = Modifier.fillMaxWidth().background(colors.background)) {
            Row(modifier = Modifier.padding(32f)) {
                Text(
                    "Watchlist",
                    modifier = Modifier.padding(24f),
                    fontSize = fontSizes.head1,
                    color = colors.textColor,
                )
                Spacer()
                RefreshIcon()
            }

            MyScroll {
                BigStock(name = "Dow Jones", price = 47739.32f, change = "-0.45%")
                Flow(modifier = Modifier.fillMaxWidth()) {
                    Stock(name = "S&P 500", price = 6846.51f, change = "-0.35%")
                    Stock(name = "Nasdaq", price = 23545.9f, change = "-0.14%")
                    Stock(name = "Russell", price = 2520.98f, change = "-0.020%")
                    Stock(name = "NYA", price = 21703.2f, change = "-0.49%")
                }
                FollowInvestments()
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RcScope.MyScroll(content: RcScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        val position = 0f.rf
        var sHeight = 0f.rf
        Column(
            modifier = Modifier.fillMaxSize().componentId(4343).verticalScroll(position),
            horizontal = RcHorizontalPositioning.Center,
        ) {
            Column(horizontal = RcHorizontalPositioning.Center) {
                content()
                sHeight = componentHeight().flush()
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) { Scrollbar(colors.stockName, position, sHeight) }
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RcFlowScope.Stock(name: String, price: Float, change: String) {
    val s = 48f
    Row(
        modifier =
            Modifier.padding(32f, 0f, 32f, 28f)
                .clip(RoundedRectShape(s, s, s, s))
                .background(colors.panels)
                .weight(1f)
                .widthIn(120f, Float.MAX_VALUE)
                .padding(24f)
    ) {
        Column {
            Row(vertical = RcVerticalPositioning.Bottom) {
                val numFlags =
                    Rc.TextFromFloat.PAD_PRE_NONE or
                        Rc.TextFromFloat.GROUPING_BY3 or
                        Rc.TextFromFloat.PAD_AFTER_ZERO
                Text(
                    price.format(8, 0, numFlags),
                    color = colors.stockPrice,
                    fontSize = fontSizes.priceDollars,
                )
                Text(
                    price.format(0, 2, numFlags),
                    color = colors.stockName,
                    fontSize = fontSizes.priceCents,
                )
            }
            Row {
                Column {
                    Text(name, color = colors.stockName, fontSize = fontSizes.default)
                    Text(change, color = colors.dotColor, fontSize = fontSizes.default)
                }
                Spacer()
                DirectionIcon()
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RcScope.BigStock(name: String, price: Float, change: String) {
    Column(modifier = Modifier.padding(32f, 40f, 48f, 1f)) {
        Row {
            Column {
                val numFlags =
                    Rc.TextFromFloat.PAD_PRE_NONE or
                        Rc.TextFromFloat.GROUPING_BY3 or
                        Rc.TextFromFloat.PAD_AFTER_ZERO
                Row(vertical = RcVerticalPositioning.Bottom) {
                    Text(
                        price.format(8, 0, numFlags),
                        color = colors.stockPrice,
                        fontSize = fontSizes.priceDollars,
                    )
                    Text(
                        price.format(0, 2, numFlags),
                        color = colors.stockName,
                        fontSize = fontSizes.priceCents,
                    )
                }
                Row(modifier = Modifier.padding(top = 16f)) {
                    Text(name, color = colors.stockName, fontSize = fontSizes.name)
                    Text(
                        change,
                        modifier = Modifier.padding(start = 8f),
                        color = colors.dotColor,
                        fontSize = fontSizes.name,
                    )
                }
            }
            Spacer()
            DirectionIcon()
        }
        StockGraph()
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RcScope.StockGraph() {
    Canvas(modifier = Modifier.height(260f).fillMaxWidth()) {
        val w = width
        val h = height
        val cx = (w / 2f).flush()
        val cy = (h / 2f).flush()
        val rad = min(cx, cy).flush()

        val data = generateStockDataArray(101, 100f, 8000f, 2000f, 0.01f)
        val stockValues = remoteFloatArray(data)
        val margin = (rad * 0.3f).flush()
        val lineBottom = (h - margin).flush()
        val path = remotePath(margin.toFloat(), lineBottom.toFloat())
        val maxValue = arrayMax(stockValues).flush()
        val minValue = (arrayMin(stockValues) - 100f).flush()
        val xEnd = (w - margin).flush()

        loop(margin, 1f, xEnd) { x ->
            val pos = ((x - margin) / (w - margin * 2f)).flush()
            val v = ((arraySpline(stockValues, pos) - minValue) / (maxValue - minValue)).flush()
            val y = (lineBottom - v * (lineBottom - margin)).flush()
            path.lineTo(x.toFloat(), y.toFloat())
        }
        path.lineTo(xEnd.toFloat(), lineBottom.toFloat())
        path.close()

        paint
            .setStyle(RcPaintStyle.Fill)
            .setLinearGradient(
                0f,
                0f,
                0f,
                lineBottom.toFloat(),
                intArrayOf(colors.dotColor.id, 0x00),
                1,
                null,
                0, /* CLAMP */
            )
            .setPathEffect(null)
            .setColor(Color.BLACK)
            .commit()

        save {
            val cut = 5f
            clipRect(margin + cut, margin + cut, xEnd - cut, lineBottom - cut)
            drawPath(path.getPath())

            paint
                .setShader(0)
                .setColorId(colors.dotColor.id)
                .setStyle(RcPaintStyle.Stroke)
                .setStrokeWidth(6f)
                .commit()
            drawPath(path.getPath())
        }
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RcScope.FollowInvestments() {
    val s = 48f
    Box(
        modifier =
            Modifier.padding(8f)
                .height(100f)
                .clip(RoundedRectShape(s, s, s, s))
                .background(colors.followText)
                .padding(4f)
                .clip(RoundedRectShape(s, s, s, s))
                .background(colors.background)
                .padding(16f),
        horizontal = RcHorizontalPositioning.Center,
        vertical = RcVerticalPositioning.Center,
    ) {
        Row(vertical = RcVerticalPositioning.Center) {
            Text("+ ", color = colors.followText, fontSize = 48.rsp)
            Text("Follow investments", color = colors.followText, fontSize = fontSizes.default)
        }
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RcScope.DirectionIcon() {
    val s = 60f
    Box(
        modifier =
            Modifier.size(120f)
                .padding(16f)
                .clip(RoundedRectShape(s, s, s, s))
                .background(colors.dotColor),
        horizontal = RcHorizontalPositioning.Center,
        vertical = RcVerticalPositioning.Center,
    ) {
        Text("↓", fontSize = 48.rsp, color = colors.arrowColor)
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RcScope.RefreshIcon() {
    Canvas(modifier = Modifier.size(64.rdp)) {
        paint.setColor(colors.textColor).commit()
        val path = refreshPath()
        val size = 64
        scale(size / 960f, size / 960f)
        drawPath(path)
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RcScope.refreshPath(): RcPath {
    val refreshStr =
        "M480,800Q346,800 253,707Q160,614 160,480Q160,346 253,253Q346," +
            "160 480,160Q549,160 612,188.5Q675,217 720,270L720,160L800,160L800,440L520," +
            "440L520,360L688,360Q656,304 600.5,272Q545,240 480,240Q380,240 310,310Q240," +
            "380 240,480Q240,580 310,650Q380,720 480,720Q557,720 619,676Q681,632 706," +
            "560L790,560Q762,666 676,733Q590,800 480,800Z"
    return remotePathData(refreshStr.toPathData())
}

@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.Scrollbar(
    color: RcColor,
    touchPosition: RcFloat,
    scrollPanelSize: RcFloat,
) {
    val w = width.flush()
    val h = height.flush()
    val alpha = sign(max(0f.rf, (touchTime() - animationTime() + 0.1f))).anim(1.2f).flush()

    with(paint) {
        setColorId(color.id)
        setAlpha(alpha)
        setStrokeWidth(10f)
        commit()
    }
    val safeSize = max(1f.rf, scrollPanelSize).flush()
    val len = (h * h / safeSize).flush()
    val off = (h * touchPosition / safeSize).flush()
    drawLine(w - 5f, off, w - 5f, off + len)
}

@Suppress("RestrictedApiAndroidX")
private class RcTickerColors(rc: RcScope) {
    val background: RcColor
    val textColor: RcColor
    val dotColor: RcColor
    val panels: RcColor
    val followText: RcColor
    val stockName: RcColor
    val stockPrice: RcColor
    val arrowColor: RcColor

    init {
        rc.beginGlobal()
        val accent2_800 = rc.remoteNamedColor("color.system_accent2_800", 0xFFFF0000.toInt())
        val accent2_50 = rc.remoteNamedColor("color.system_accent2_50", 0xFF113311.toInt())
        background = rc.remoteThemedColor(accent2_50, accent2_800)

        val surface_light = rc.remoteNamedColor("color.system_on_surface_light", 0xFF113311.toInt())
        val surface_dark = rc.remoteNamedColor("color.system_on_surface_dark", 0xFFFF9966.toInt())
        textColor = rc.remoteThemedColor(surface_light, surface_dark)

        val accent3_600 = rc.remoteNamedColor("color.system_accent3_600", 0xFF113311.toInt())
        val accent3_100 = rc.remoteNamedColor("color.system_accent3_100", 0xFFFF9966.toInt())
        dotColor = rc.remoteThemedColor(accent3_600, accent3_100)

        val accent2_10 = rc.remoteNamedColor("color.system_accent2_10", 0xFF113311.toInt())
        val accent2_900 = rc.remoteNamedColor("color.system_accent2_900", 0xFFFF9966.toInt())
        panels = rc.remoteThemedColor(accent2_10, accent2_900)

        val accent1_200 = rc.remoteNamedColor("color.system_accent1_200", 0xFF222222.toInt())
        followText = rc.remoteThemedColor(accent2_800, accent1_200)

        val neutral2_800 = rc.remoteNamedColor("color.system_neutral2_800", 0xFF113311.toInt())
        val neutral2_200 = rc.remoteNamedColor("color.system_neutral2_400", 0xFFFF9966.toInt())
        stockName = rc.remoteThemedColor(neutral2_800, neutral2_200)

        val accent1_900 = rc.remoteNamedColor("color.system_accent1_900", 0xFF113311.toInt())
        val accent1_50 = rc.remoteNamedColor("color.system_accent1_50", 0xFFFF9966.toInt())
        stockPrice = rc.remoteThemedColor(accent1_900, accent1_50)

        arrowColor = rc.remoteThemedColor(accent1_50, accent1_900)
        rc.endGlobal()
    }
}

@Suppress("RestrictedApiAndroidX")
private class RcTickerFontSizes(rc: RcScope) {
    val head1: RcSp
    val priceDollars: RcSp
    val priceCents: RcSp
    val name: RcSp
    val default: RcSp

    init {
        val fontScale = rc.remoteNamedFloat("system.font_size", 37f) / 37f
        with(rc) {
            head1 = (42f.rf * fontScale).toSp()
            priceDollars = (64f.rf * fontScale).toSp()
            priceCents = (48f.rf * fontScale).toSp()
            name = (32f.rf * fontScale).toSp()
            default = (32f.rf * fontScale).toSp()
        }
    }

    private fun RcFloat.toSp(): RcSp = RcSp(this.toFloat()) // Rough conversion for demo
}

@Suppress("RestrictedApiAndroidX")
private fun generateStockDataArray(
    numPoints: Int,
    startPrice: Float,
    annualDrift: Float,
    annualVolatility: Float,
    daysPerPoint: Float,
): FloatArray {
    val random = Random()
    val prices = FloatArray(numPoints)
    prices[0] = startPrice
    val dt = daysPerPoint / 252.0
    val drift = annualDrift / 100.0
    val volatility = annualVolatility / 100.0
    for (i in 1 until numPoints) {
        val u1 = random.nextFloat()
        val u2 = random.nextFloat()
        val randNormal =
            (sqrt(-2.0 * ln(u1.toDouble())) * cos(2.0 * Math.PI * u2.toDouble())).toFloat()
        val driftTerm = (drift - volatility * volatility / 2.0) * dt
        val randomTerm = volatility * sqrt(dt) * randNormal
        prices[i] = prices[i - 1] * exp(driftTerm + randomTerm).toFloat()
    }
    return prices
}
