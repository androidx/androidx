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
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.creation.RFloat
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.min
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.integration.view.demos.R
import java.util.Random

private lateinit var color: RcTickerColorPack

@Suppress("RestrictedApiAndroidX")
fun RcTicker(context: Context): RemoteComposeContext {
    val res = context.resources
    val refresh = BitmapFactory.decodeResource(res, R.drawable.refresh)
    return RemoteComposeContextAndroid(
        0,
        0,
        "Demo",
        7,
        RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        AndroidxRcPlatformServices(),
    ) {
        color = RcTickerColorPack(this)
        root {
            column(Modifier.fillMaxSize().backgroundId(color.backgroundId)) {
                row(Modifier.padding(32f)) {
                    val imageId = addBitmap(refresh)
                    text(
                        "Watchlist",
                        Modifier.padding(24),
                        fontSize = 48f,
                        colorId = color.textColorId,
                    )
                    space()
                    //  image(Modifier.size(80), imageId, ImageScaling.SCALE_INSIDE, 1f)
                    refreshIcon()
                }
                column(
                    Modifier.fillMaxWidth().verticalWeight(1f).padding(32f).verticalScroll(),
                    horizontal = ColumnLayout.CENTER,
                ) {
                    bigstock("Dow Jones", 47739.32f, "-0.45%")
                    stock("S&P 500", 6846.51f, "-0.35%")
                    stock("Nasdaq", 23545.9f, "-0.14%")
                    stock("Russell", 2520.98f, "-0.020%")
                    stock("NYA", 21703.2f, "-0.49%")
                    followInvestments()
                }
            }
        }
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
            .padding(24)
    ) {
        column {
            row(vertical = RowLayout.BOTTOM) {
                val numFlags =
                    Rc.TextFromFloat.PAD_PRE_NONE or
                        Rc.TextFromFloat.GROUPING_BY3 or
                        Rc.TextFromFloat.PAD_AFTER_ZERO

                val priceDollars = createTextFromFloat(price, 8, 0, numFlags)
                text(priceDollars, colorId = color.stockPriceId, fontSize = 72f)
                val priceCents = createTextFromFloat(price, 0, 2, numFlags)
                text(priceCents, colorId = color.stockNameId, fontSize = 66f)
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
    column(Modifier.padding(32, 0, 32, 1)) {
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
                        fontSize = 84f,
                        fontFamily = "Roboto",
                    )
                    val priceCents = createTextFromFloat(price, 0, 2, numFlags)
                    text(priceCents, colorId = color.stockNameId, fontSize = 66f)
                }

                row(Modifier.padding(0, 10, 0, 0)) {
                    text(name, colorId = color.stockNameId, fontSize = 32f)
                    text(
                        change,
                        Modifier.padding(8, 0, 0, 0),
                        colorId = color.dotColorId,
                        fontSize = 32f,
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
    canvas(RecordingModifier().height(200).fillMaxWidth()) {
        val w = ComponentWidth() // component.width()
        val h = ComponentHeight()
        val cx = (w / 2f).flush()
        val cy = (h / 2f).flush()
        val rad = min(cx, cy).flush()

        val data = fillRandom(101, 10f, 1000f, 10f)
        //        for (i in 0 until data.size) {
        //            val s = i / data.size.toFloat()
        //            data[i] = (100 + Math.random() * 1000 * s + 1000 * s * s).toFloat()
        //        }
        val stockValues = RFloat(writer, addFloatArray(data))
        val margin = rad * 0.3f
        val lineBottom = h - margin
        val path: Int = pathCreate(margin.toFloat(), lineBottom.toFloat())
        val max = aMax(stockValues)
        val min = aMin(stockValues) - 100f
        val xEnd = w - margin
        loop(margin, 1f, xEnd) { x ->
            val pos = (x - margin) / (w - margin * 2f)
            val v = (aSpline(stockValues, pos) - min) / (max - min)
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
        val system_neutral2_200 = rc.addNamedColor("color.system_neutral2_200", 0xFFFF9966.toInt())
        stockNameId = rc.mColor(system_neutral2_800, system_neutral2_200)

        val system_accent1_900 = rc.addNamedColor("color.system_accent1_900", 0xFF113311.toInt())
        val system_accent1_50 = rc.addNamedColor("color.system_accent1_50", 0xFFFF9966.toInt())
        stockPriceId = rc.mColor(system_accent1_900, system_accent1_50)

        arrowColorId = rc.mColor(system_accent1_50, system_accent1_900) // Todo

        rc.endGlobal()
    }
}

// TODO move to RFloat
@Suppress("RestrictedApiAndroidX")
public fun aMax(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.A_MAX))
}

@Suppress("RestrictedApiAndroidX")
public fun aMin(a: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, Rc.FloatExpression.A_MIN))
}

@Suppress("RestrictedApiAndroidX")
public fun aSpline(a: RFloat, pos: RFloat): RFloat {
    return RFloat(a.writer, floatArrayOf(*a.array, *pos.array, Rc.FloatExpression.A_SPLINE))
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
    canvas(Modifier.size(size)) {
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
