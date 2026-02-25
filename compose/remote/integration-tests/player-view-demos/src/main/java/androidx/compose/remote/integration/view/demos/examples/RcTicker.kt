/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.compose.remote.integration.view.demos.examples

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.creation.RFloat
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.arrayMax
import androidx.compose.remote.creation.arrayMin
import androidx.compose.remote.creation.arraySpline
import androidx.compose.remote.creation.max
import androidx.compose.remote.creation.min
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.sign
import androidx.compose.remote.creation.times
import androidx.compose.remote.integration.view.demos.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import java.util.Random
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

private lateinit var color: RcTickerColorPack
private lateinit var fontSize: RcFontSizes

@Preview
@Composable
private fun RcTickerPreview() {
    val context = LocalContext.current
    RemoteDocPreview(RcTicker(context))
}

@Suppress("RestrictedApiAndroidX")
fun RcTicker(context: Context): RemoteComposeContext {
    val res = context.resources
    val refresh = BitmapFactory.decodeResource(res, R.drawable.refresh)
    return RemoteComposeContextAndroid(
        AndroidxRcPlatformServices(),
        7,
        RemoteComposeWriter.HTag(
            Header.DOC_PROFILES,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        ),
        RemoteComposeWriter.HTag(Header.DEBUG, 0),
    ) {
        color = RcTickerColorPack(this)
        fontSize = RcFontSizes(this)
        root {
            column(Modifier.fillMaxWidth().backgroundId(color.backgroundId)) {
                row(Modifier.padding(32f)) {
                    val imageId = addBitmap(refresh)
                    text(
                        "Watchlist",
                        Modifier.padding(24),
                        fontSize = fontSize.head1,
                        colorId = color.textColorId,
                    )
                    space()
                    //  image(Modifier.size(80), imageId, ImageScaling.SCALE_INSIDE, 1f)
                    refreshIcon()
                }
                MyScroll() {
                    bigstock("Dow Jones", 47739.32f, "-0.45%")
                    flow(Modifier.fillMaxWidth()) {
                        stock("S&P 500", 6846.51f, "-0.35%")
                        stock("Nasdaq", 23545.9f, "-0.14%")
                        stock("Russell", 2520.98f, "-0.020%")
                        stock("NYA", 21703.2f, "-0.49%")
                    }
                    followInvestments()
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun RemoteComposeContextAndroid.MyScroll(content: RemoteComposeContextAndroid.() -> Unit) {
    box(Modifier.fillMaxWidth()) {
        val position = rf(0f)
        lateinit var sHeight: RFloat
        column(
            Modifier.fillMaxSize()
                .componentId(4343)
                //  .padding(32f)
                .verticalScroll(position.toFloat()),
            horizontal = ColumnLayout.CENTER,
        ) {
            column(horizontal = ColumnLayout.CENTER) {
                content()
                sHeight = ComponentHeight()
            }
        }

        scrollbar1(color.stockNameId, position, sHeight)
    }
}

@Suppress("RestrictedApiAndroidX")
fun RemoteComposeContextAndroid.followInvestments() {
    val s = 48f
    box(
        Modifier.padding(8)
            .height(100)
            .clip(RoundedRectShape(s, s, s, s))
            .backgroundId(color.followTextId)
            .padding(4)
            .clip(RoundedRectShape(s, s, s, s))
            .backgroundId(color.backgroundId)
            .padding(16),
        horizontal = BoxLayout.CENTER,
        vertical = BoxLayout.CENTER,
    ) {
        row(vertical = RowLayout.CENTER) {
            text("+ ", colorId = color.followTextId, fontSize = 48f)
            text("Follow investments", colorId = color.followTextId, fontSize = 36f)
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun RemoteComposeContextAndroid.space() {
    box(Modifier.horizontalWeight(1f))
}

@Suppress("RestrictedApiAndroidX")
fun RemoteComposeContextAndroid.stock(name: String, price: Float, change: String) {
    val s = 48f
    row(
        Modifier.padding(32, 0, 32, 28)
            .clip(RoundedRectShape(s, s, s, s))
            .backgroundId(color.panelsId)
            .horizontalWeight(1f)
            .widthIn(120f, Float.MAX_VALUE)
            .padding(24)
    ) {
        column {
            row(vertical = RowLayout.BOTTOM) {
                val numFlags =
                    Rc.TextFromFloat.PAD_PRE_NONE or
                        Rc.TextFromFloat.GROUPING_BY3 or
                        Rc.TextFromFloat.PAD_AFTER_ZERO

                val priceDollars = createTextFromFloat(price, 8, 0, numFlags)
                text(priceDollars, colorId = color.stockPriceId, fontSize = fontSize.priceDollars)
                val priceCents = createTextFromFloat(price, 0, 2, numFlags)
                text(priceCents, colorId = color.stockNameId, fontSize = fontSize.priceCents)
            }
            row {
                column {
                    text(name, colorId = color.stockNameId)
                    text(change, colorId = color.dotColorId)
                }
                space()
                direction()
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun RemoteComposeContextAndroid.bigstock(name: String, price: Float, change: String) {
    column(Modifier.padding(32, 40, 48, 1)) {
        row {
            column {
                val numFlags =
                    Rc.TextFromFloat.PAD_PRE_NONE or
                        Rc.TextFromFloat.GROUPING_BY3 or
                        Rc.TextFromFloat.PAD_AFTER_ZERO
                row(vertical = RowLayout.BOTTOM) {
                    val priceDollars = createTextFromFloat(price, 8, 0, numFlags)
                    text(
                        priceDollars,
                        colorId = color.stockPriceId,
                        fontSize = fontSize.bigPriceDollars,
                        fontFamily = "Roboto",
                    )
                    val priceCents = createTextFromFloat(price, 0, 2, numFlags)
                    text(priceCents, colorId = color.stockNameId, fontSize = fontSize.bigPriceCents)
                }

                row(Modifier.padding(0, 16, 0, 0)) {
                    text(name, colorId = color.stockNameId, fontSize = fontSize.name)
                    text(
                        change,
                        Modifier.padding(8, 0, 0, 0),
                        colorId = color.dotColorId,
                        fontSize = fontSize.name,
                    )
                }
            }
            space()
            direction()
        }
        graph()
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.graph() {
    canvas(RecordingModifier().height(260).fillMaxWidth()) {
        val w = ComponentWidth() // component.width()
        val h = ComponentHeight()
        val cx = (w / 2f).flush()
        val cy = (h / 2f).flush()
        val rad = min(cx, cy).flush()

        // val data = fillRandom(101, 10f, 1000f, 10f)
        val data = generateStockDataArray(101, 100f, 8000f, 2000f, 0.01f)
        //        for (i in 0 until data.size) {
        //            val s = i / data.size.toFloat()
        //            data[i] = (100 + Math.random() * 1000 * s + 1000 * s * s).toFloat()
        //        }
        val stockValues = RFloat(writer, addFloatArray(data))
        val margin = rad * 0.3f
        val lineBottom = h - margin
        val path: Int = pathCreate(margin.toFloat(), lineBottom.toFloat())
        val max = arrayMax(stockValues)
        val min = arrayMin(stockValues) - 100f
        val xEnd = w - margin
        loop(margin, 1f, xEnd) { x ->
            val pos = (x - margin) / (w - margin * 2f)
            val v = (arraySpline(stockValues, pos) - min) / (max - min)
            val y = lineBottom - v * (lineBottom - margin)
            pathAppendLineTo(path, x.toFloat(), y.toFloat())
        }

        pathAppendLineTo(path, xEnd.toFloat(), lineBottom.toFloat())
        pathAppendClose(path)

        //  ==========
        painter
            .setStyle(Paint.Style.FILL)
            .setLinearGradient(
                0f,
                0f,
                0f,
                lineBottom.toFloat(),
                intArrayOf(color.dotColorId, 0x00),
                1,
                null,
                Shader.TileMode.CLAMP,
            )
            .setPathEffect(null)
            .commit()
        save() {
            val cut = 5f
            clipRect(
                (margin + cut).toFloat(),
                (margin + cut).toFloat(),
                (xEnd - cut).toFloat(),
                (lineBottom - cut).toFloat(),
            )
            drawPath(path)
            painter
                .setShader(0)
                .setColorId(color.dotColorId)
                .setStyle(Paint.Style.STROKE)
                .setStrokeWidth(6f)
                .commit()
            drawPath(path)
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun RemoteComposeContextAndroid.direction() {
    val s = 60f
    box(
        Modifier.size(120)
            .padding(16)
            .clip(RoundedRectShape(s, s, s, s))
            .backgroundId(color.dotColorId),
        horizontal = BoxLayout.CENTER,
        vertical = BoxLayout.CENTER,
    ) {
        text(
            "↓",
            fontSize = 48f,
            textAlign = TextLayout.TEXT_ALIGN_CENTER,
            colorId = color.arrowColorId,
        )
    }
}

@Suppress("RestrictedApiAndroidX")
fun RemoteComposeContextAndroid.mColor(light: Int, dark: Int): Int {
    return writer.addThemedColor(light.toShort(), dark.toShort()).toInt()
}

@Suppress("RestrictedApiAndroidX")
private class RcFontSizes(val rc: RemoteComposeContextAndroid) {
    private val head1Base: Float = 42f / 37f
    private val normalBase: Float = 48f / 37f
    private val bigPriceDollarsBase: Float = 84f / 37f
    private val bigPriceCentsBase: Float = 66f / 37f
    private val priceDollarsBase: Float = 64f / 37f
    private val priceCentsBase: Float = 48f / 37f
    private val nameBase: Float = 32f / 37f

    var normal: Float
    var head1: Float
    var bigPriceDollars: Float
    var bigPriceCents: Float
    var priceDollars: Float
    var priceCents: Float
    var name: Float

    init {
        val fontScale = rc.rf(Rc.System.FONT_SIZE)

        head1 = (head1Base * fontScale).toFloat()
        normal = (normalBase * fontScale).toFloat()
        bigPriceDollars = (bigPriceDollarsBase * fontScale).toFloat()
        bigPriceCents = (bigPriceCentsBase * fontScale).toFloat()
        priceDollars = (priceDollarsBase * fontScale).toFloat()
        priceCents = (priceCentsBase * fontScale).toFloat()
        name = (nameBase * fontScale).toFloat()
        rc.addDebugMessage(" font scale ", fontScale)
    }
}

@Suppress("LocalVariableName", "RestrictedApiAndroidX")
private class RcTickerColorPack(val rc: RemoteComposeContextAndroid) {
    val backgroundId: Int
    val minutesColorId: Int
    val textColorId: Int
    val dotColorId: Int
    val hrId: Int
    val panelsId: Int
    val followTextId: Int
    val stockNameId: Int
    val stockPriceId: Int
    val arrowColorId: Int

    init {

        rc.beginGlobal()
        val system_accent2_800 = rc.addNamedColor("color.system_accent2_800", 0xFFFF0000.toInt())
        val system_accent2_50 = rc.addNamedColor("color.system_accent2_50", 0xFF113311.toInt())
        val system_accent1_500 = rc.addNamedColor("color.system_accent1_500", 0xFF113311.toInt())
        val system_accent1_100 = rc.addNamedColor("color.system_accent1_100", 0xFFFF9966.toInt())

        backgroundId = rc.mColor(system_accent2_50, system_accent2_800)
        minutesColorId = rc.mColor(system_accent1_500, system_accent1_100)
        val system_on_surface_light =
            rc.addNamedColor("color.system_on_surface_light", 0xFF113311.toInt())
        val system_on_surface_dark =
            rc.addNamedColor("color.system_on_surface_dark", 0xFFFF9966.toInt())

        textColorId = rc.mColor(system_on_surface_light, system_on_surface_dark)
        val system_accent3_600 = rc.addNamedColor("color.system_accent3_600", 0xFF113311.toInt())
        val system_accent3_100 = rc.addNamedColor("color.system_accent3_100", 0xFFFF9966.toInt())
        dotColorId = rc.mColor(system_accent3_600, system_accent3_100)

        val system_accent2_700 = rc.addNamedColor("color.system_accent2_700", 0xFF113311.toInt())
        val system_accent2_400 = rc.addNamedColor("color.system_accent2_400", 0xFFFF9966.toInt())
        hrId = rc.mColor(system_accent2_700, system_accent2_400)

        val system_accent2_10 = rc.addNamedColor("color.system_accent2_10", 0xFF113311.toInt())
        val system_accent2_900 = rc.addNamedColor("color.system_accent2_900", 0xFFFF9966.toInt())
        panelsId = rc.mColor(system_accent2_10, system_accent2_900)

        // val system_accent2_800 =  rc.addNamedColor("color.system_accent2_800",
        // 0xFF_999999.toInt())
        val system_accent1_200 = rc.addNamedColor("color.system_accent1_200", 0xFF_222222.toInt())
        followTextId = rc.mColor(system_accent2_800, system_accent1_200)

        val system_neutral2_800 = rc.addNamedColor("color.system_neutral2_800", 0xFF113311.toInt())
        val system_neutral2_200 = rc.addNamedColor("color.system_neutral2_400", 0xFFFF9966.toInt())
        stockNameId = rc.mColor(system_neutral2_800, system_neutral2_200)

        val system_accent1_900 = rc.addNamedColor("color.system_accent1_900", 0xFF113311.toInt())
        val system_accent1_50 = rc.addNamedColor("color.system_accent1_50", 0xFFFF9966.toInt())
        stockPriceId = rc.mColor(system_accent1_900, system_accent1_50)

        arrowColorId = rc.mColor(system_accent1_50, system_accent1_900) // Todo

        rc.endGlobal()
    }
}

@Suppress("RestrictedApiAndroidX")
private fun fillRandom(size: Int, startVal: Float, endVal: Float, roughness: Float): FloatArray {
    val arr = FloatArray(size)
    var random = Random()
    arr[0] = startVal
    arr[size - 1] = endVal
    divide(random, arr, 0, size - 1, roughness)
    for (i in 1 until size - 1) {
        val f = (1 + i) / (1 + size.toFloat())
        arr[i] =
            (arr[i - 1] + arr[i + 1]) / 2 +
                f * (random.nextFloat() - 0.5f) * roughness * (endVal - startVal) / 13
    }
    return arr
}

@Suppress("RestrictedApiAndroidX")
private fun divide(random: Random, arr: FloatArray, left: Int, right: Int, roughness: Float) {
    if (right - left < 2) {
        return
    }
    val mid = (left + right) / 2
    val average = (arr[left] + arr[right]) / 2
    val range = (right - left) * roughness
    val offset: Float = (random.nextFloat() * 2 - 1) * range
    arr[mid] = average + offset
    divide(random, arr, left, mid, roughness)
    divide(random, arr, mid, right, roughness)
}

@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.refreshIcon() {
    val size = 64
    val action = HostAction(567, textCreateId("refresh"))
    canvas(Modifier.size(size).onTouchDown(action)) {
        val path = refreshPath()
        scale(size / 960f, size / 960f)
        painter.setColorId(color.textColorId).commit()
        drawPath(path)
    }
}

@Suppress("RestrictedApiAndroidX")
fun RemoteComposeContextAndroid.refreshPath(): Int {
    val refreshStr =
        "M480,800Q346,800 253,707Q160,614 160,480Q160,346 253,253Q346," +
            "160 480,160Q549,160 612,188.5Q675,217 720,270L720,160L800,160L800,440L520," +
            "440L520,360L688,360Q656,304 600.5,272Q545,240 480,240Q380,240 310,310Q240," +
            "380 240,480Q240,580 310,650Q380,720 480,720Q557,720 619,676Q681,632 706," +
            "560L790,560Q762,666 676,733Q590,800 480,800Z"

    val pdata = RemotePath(refreshStr)

    return addPathData(pdata)
}

/**
 * Generate realistic stock price data using Geometric Brownian Motion
 *
 * The Geometric Brownian Motion formula is: S(t+1) = S(t) * exp((μ - σ²/2)dt + σ*√dt*Z)
 *
 * Where:
 * - μ (mu) is the drift (expected return rate)
 * - σ (sigma) is the volatility (standard deviation of returns)
 * - dt is the time step
 * - Z is a random variable from standard normal distribution
 *
 * @param numPoints Number of data points to generate
 * @param startPrice Starting price of the stock (e.g., 100.0)
 * @param annualDrift Annual drift/return as percentage (e.g., 8.0 for 8%)
 * @param annualVolatility Annual volatility as percentage (e.g., 25.0 for 25%)
 * @param daysPerPoint Trading days represented by each point (default: 1.0 for daily)
 * @return List of stock prices as doubles
 */
@Suppress("RestrictedApiAndroidX")
fun generateStockDataArray(
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

    for (i in 1..<numPoints) {
        val randNormal = generateRandomNormal(random)
        val driftTerm = (drift - volatility * volatility / 2.0) * dt
        val randomTerm = volatility * sqrt(dt) * randNormal

        prices[i] = prices[i - 1] * exp(driftTerm + randomTerm).toFloat()
    }

    return prices
}

@Suppress("RestrictedApiAndroidX")
private fun generateRandomNormal(random: Random): Float {
    val u1: Float = random.nextFloat()
    val u2: Float = random.nextFloat()
    return (sqrt(-2.0 * ln(u1)) * cos(2.0 * Math.PI * u2)).toFloat()
}

@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.scrollbar1(
    color: Int,
    touchPosition: RFloat,
    scrollPanelSize: RFloat,
) {

    startCanvas(RecordingModifier().fillMaxSize())
    val width = ComponentWidth()
    val height = ComponentHeight()
    val alpha = sign(max(0f, touchTime() - animationTime() + 0.1)).anim(1.2f)

    painter.setColorId(color).setAlpha(alpha.toFloat()).setStrokeWidth(10f).commit()
    val len = height * height / scrollPanelSize
    val off = height * touchPosition / scrollPanelSize

    drawLine((width - 5f), off, (width - 5f), off + len)
    endCanvas()
}

@Suppress("RestrictedApiAndroidX")
fun RemoteComposeContextAndroid.touchTime(): RFloat {
    return rf(Rc.Touch.TOUCH_EVENT_TIME)
}
