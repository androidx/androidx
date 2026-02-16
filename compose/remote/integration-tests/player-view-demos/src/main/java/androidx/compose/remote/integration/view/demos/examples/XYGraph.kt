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

package androidx.compose.remote.integration.view.demos.examples

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.remote.creation.Painter
import androidx.compose.remote.creation.RFloat
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.arrayLength
import androidx.compose.remote.creation.arrayMax
import androidx.compose.remote.creation.arrayMin
import androidx.compose.remote.creation.arraySpline
import androidx.compose.remote.creation.floor
import androidx.compose.remote.creation.ifElse
import androidx.compose.remote.creation.log
import androidx.compose.remote.creation.max
import androidx.compose.remote.creation.minus
import androidx.compose.remote.creation.plus
import androidx.compose.remote.creation.pow
import androidx.compose.remote.creation.rf
import androidx.compose.remote.creation.times

// ================================ PlotParams ==============================
@Suppress("RestrictedApiAndroidX")
class PlotParams(
    val prop: XYGraphProperties,
    val left: RFloat,
    val top: RFloat,
    val right: RFloat,
    val bottom: RFloat,
    range: Range,
) {
    lateinit var yOffset: RFloat
    lateinit var yScale: RFloat
    var dataXMin: RFloat = range.minX
    var dataXMax: RFloat = range.maxX
    var dataYMax: RFloat = range.maxY
    var dataYMin: RFloat = range.minY
    var xRange: RFloat = (dataXMax - dataXMin).flush()
    var yRange: RFloat = (dataYMax - dataYMin).flush()
    val pad = 0.05f
    var graphMax: RFloat = (dataYMax + pad * yRange).flush()
    var graphMin: RFloat = (dataYMin - pad * yRange).flush()
    var density = RFloat(dataXMax.writer, Rc.System.DENSITY)
    var insertLeft = 30f * density
    var insertTop = 10f * density
    var insertRight = 10f * density
    var insertBottom = 50f * density
    lateinit var scaleX: RFloat
    lateinit var scaleY: RFloat
    lateinit var offsetX: RFloat
    lateinit var offsetY: RFloat
}

@Suppress("RestrictedApiAndroidX")
class XYGraphProperties {
    val minorVAxisColor: Int = Color.DKGRAY
    val minorVAxisColorIsId: Boolean = false
    val minorHAxisColor: Int = Color.DKGRAY
    val minorHAxisColorIsId: Boolean = false

    val majorVAxisColor: Int = Color.GRAY
    val majorVAxisColorIsId: Boolean = false

    val majorHAxisColor: Int = Color.GRAY
    val majorHAxisColorIsId: Boolean = false
    val minorVTickSize: Float = 2f
    val minorHTickSize: Float = 2f
    val majorVTickSize: Float = 4f
    val majorHTickSize: Float = 4f

    val vAxisColor: Int = Color.YELLOW
    val vAxisColorIsId: Boolean = false
    val hAxisColor: Int = Color.YELLOW
    val hAxisColorIsId: Boolean = false
    val axisSize: Float = 4f
    val plotColor: Int = Color.RED

    fun setVMinorAxis(paint: Painter): Painter {
        if (minorVAxisColorIsId) paint.setColorId(minorVAxisColor).setStrokeWidth(minorVTickSize)
        else paint.setColor(minorVAxisColor).setStrokeWidth(minorVTickSize)
        return paint
    }

    fun setMinorAxis(paint: Painter): Painter {
        if (minorHAxisColorIsId) paint.setColorId(minorHAxisColor).setStrokeWidth(minorHTickSize)
        else paint.setColor(minorHAxisColor).setStrokeWidth(minorHTickSize)
        return paint
    }

    fun setVMajorAxis(paint: Painter): Painter {
        if (majorVAxisColorIsId) paint.setColorId(majorVAxisColor).setStrokeWidth(majorVTickSize)
        else paint.setColor(majorVAxisColor).setStrokeWidth(majorVTickSize)
        return paint
    }

    fun setMajorHAxis(paint: Painter): Painter {
        if (majorHAxisColorIsId) paint.setColorId(majorHAxisColor).setStrokeWidth(majorHTickSize)
        else paint.setColor(majorHAxisColor).setStrokeWidth(majorHTickSize)
        return paint
    }

    fun setVAxis(paint: Painter): Painter {
        if (vAxisColorIsId) paint.setColorId(vAxisColor).setStrokeWidth(axisSize)
        else paint.setColor(vAxisColor).setStrokeWidth(axisSize)
        return paint
    }

    fun setHAxis(paint: Painter): Painter {
        if (hAxisColorIsId) paint.setColorId(hAxisColor).setStrokeWidth(axisSize)
        else paint.setColor(hAxisColor).setStrokeWidth(axisSize)
        return paint
    }

    fun setPlotPaint(paint: Painter, strokeWidth: Float = 2f): Painter {
        paint
            .setShader(0)
            .setColor(plotColor)
            .setStyle(Paint.Style.STROKE)
            .setStrokeWidth(strokeWidth)
        return paint
    }

    fun setPlotFill(
        paint: Painter,
        startX: RFloat,
        startY: RFloat,
        endX: RFloat,
        endY: RFloat,
    ): Painter {
        paint
            .setStyle(Paint.Style.FILL)
            .setLinearGradient(
                startX.toFloat(),
                startY.toFloat(),
                endX.toFloat(),
                endY.toFloat(),
                intArrayOf(plotColor, 0x00),
                0,
                null,
                Shader.TileMode.MIRROR,
            )
            .setPathEffect(null)
        return paint
    }
}

// ================================ End PlotParams ==============================
/** Simple xy Plotter */
@Suppress("RestrictedApiAndroidX")
fun RemoteComposeContextAndroid.rcPlotXY(
    left: Number,
    top: Number,
    right: Number,
    bottom: Number,
    prop: XYGraphProperties = XYGraphProperties(),
    plot: RFloat,
) {
    rcPlotXY(left, top, right, bottom, prop, Plot(plot))
}

@Suppress("RestrictedApiAndroidX")
class Range(val minX: RFloat, val maxX: RFloat, val minY: RFloat, val maxY: RFloat)

@Suppress("RestrictedApiAndroidX")
interface PlotBase {

    fun plot(rc: RemoteComposeContextAndroid, params: PlotParams)

    fun calcRange(rc: RemoteComposeContextAndroid): Range
}

@Suppress("RestrictedApiAndroidX")
class FunctionPlot(
    val function: RFloat,
    val startX: RFloat,
    val endX: RFloat,
    val startY: RFloat,
    val endY: RFloat,
) : PlotBase {

    constructor(
        function: RFloat,
        startX: Float,
        endX: Float,
        startY: Float,
        endY: Float,
    ) : this(
        function,
        RFloat(function.writer, startX),
        RFloat(function.writer, endX),
        RFloat(function.writer, startY),
        RFloat(function.writer, endY),
    )

    override fun calcRange(rc: RemoteComposeContextAndroid): Range {
        return Range(startX, endX, startY, endY)
    }

    override fun plot(rc: RemoteComposeContextAndroid, params: PlotParams) {
        params.prop.setPlotPaint(rc.painter, (2f * rc.rf(Rc.System.DENSITY)).toFloat()).commit()
        with(rc) {
            val pathId =
                addPathExpression(
                    rFun { x -> x * params.scaleX + params.offsetX },
                    function * params.scaleY +
                        params.offsetY, // rFun{ x -> params.scaleY*sin(x)+params.offsetY },//
                    params.dataXMin,
                    params.dataXMax,
                    128,
                    Rc.PathExpression.LINEAR_PATH,
                )
            drawPath(pathId)
        }
    }
}

val plotBasColor: Int = Color.RED
val plotBaseColorId = false

@Suppress("RestrictedApiAndroidX")
private fun configPlotPaint(paint: Painter): Painter {

    return paint
}

@Suppress("RestrictedApiAndroidX")
class Plot(val data: RFloat) : PlotBase {

    private fun configPlotFill(paint: Painter, params: PlotParams): Painter {

        val rLeft = params.insertLeft
        val rTop = params.insertTop
        val rRight = params.right - params.insertRight
        val rBottom = params.bottom - params.insertBottom

        return params.prop.setPlotFill(paint, rLeft, rTop, rLeft, params.offsetY)
    }

    override fun plot(rc: RemoteComposeContextAndroid, params: PlotParams) {
        with(rc) { plotData(data, params) }
    }

    override fun calcRange(rc: RemoteComposeContextAndroid): Range {
        val minX = RFloat(data.writer, 0.0f)
        val maxX = arrayLength(data).flush()
        val maxY = arrayMax(data).flush()
        val minY = arrayMin(data).flush()
        return Range(minX, maxX, minY, maxY)
    }

    private fun RemoteComposeContextAndroid.plotData(values: Number, params: PlotParams) {

        val width = params.right - params.left
        val height = params.bottom - params.top
        val rValues = toRf(values)

        val y = params.offsetY + params.scaleY * arraySpline(rValues, RFloat(rValues.writer, 0f))
        val x = params.offsetX + params.scaleX * (params.dataXMin)
        val path: Int = pathCreate(x.toFloat(), y.toFloat())
        //        val path: Int = pathCreate(0f, height.toFloat())

        loop(0, 1f, width) { x ->
            val pos = x / width
            val y = params.offsetY + params.scaleY * arraySpline(rValues, pos)
            val x1 = params.offsetX + params.scaleX * (pos * params.xRange + params.dataXMin)
            pathAppendLineTo(path, x1.toFloat(), y.toFloat())
        }
        plotPath(path, params)
        val endX = params.offsetX + params.scaleX * params.dataXMax
        val startX = params.offsetX + params.scaleX * params.dataXMin
        pathAppendLineTo(path, endX.toFloat(), params.offsetY.toFloat())
        pathAppendLineTo(path, startX.toFloat(), params.offsetY.toFloat())
        pathAppendClose(path)
        plotFill(path, params)
    }

    private fun RemoteComposeContextAndroid.plotPath(path: Int, params: PlotParams) {
        params.prop.setPlotPaint(painter).commit()
        drawPath(path)
    }

    private fun RemoteComposeContextAndroid.plotFill(path: Int, params: PlotParams) {

        val rLeft = params.insertLeft
        val rTop = params.insertTop
        val rRight = params.right - params.insertRight
        val rBottom = params.bottom - params.insertBottom
        configPlotFill(painter, params).commit()
        drawPath(path)
        painter.setShader(0).commit()
    }
}

@Suppress("RestrictedApiAndroidX")
fun RemoteComposeContextAndroid.rcPlotXY(
    left: Number,
    top: Number,
    right: Number,
    bottom: Number,
    prop: XYGraphProperties = XYGraphProperties(),
    plot: PlotBase,
) {

    val margin = 2f * rf(Rc.System.DENSITY)
    val params =
        PlotParams(
            prop,
            toRf(left) + margin,
            toRf(top) + margin,
            toRf(right) - margin,
            toRf(bottom) - margin,
            plot.calcRange(this),
        )

    save()
    translate(params.left.toFloat(), params.top.toFloat())
    axis(params)
    plot.plot(this, params)
    restore()
}

@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.toRf(values: Number): RFloat {
    return if (values is RFloat) values else rf(values)
}

@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.axis(params: PlotParams) {
    val w = params.right - params.left
    val h = params.bottom - params.top

    val xRange = params.xRange
    val yRange = params.yRange

    val majorStepX = plotIncrement(xRange, 3)
    val insertX = params.insertLeft
    val insertY = params.insertBottom

    params.scaleX = ((w - params.insertLeft - params.insertRight) / (xRange)).flush()
    params.offsetX = (insertX - (params.dataXMin * params.scaleX)).flush()
    params.scaleY = ((params.insertTop + params.insertBottom - h) / (yRange)).flush()
    params.offsetY = ((h - insertY) - (params.scaleY * params.dataYMin)).flush()
    val density = rf(Rc.System.DENSITY)

    painter.setColor(Color.BLUE).setTextSize((density * 16f).toFloat()).commit()
    params.prop.setVMinorAxis(painter).commit()
    // ================ minor grid  ================
    val minorStepX = plotIncrement(xRange, 20)

    val y1 = (params.dataYMin * params.scaleY + params.offsetY).flush()
    val y2 = (params.dataYMax * params.scaleY + params.offsetY).flush()
    loop(params.dataXMin, minorStepX, params.dataXMax) { x ->
        val sx = x * params.scaleX + params.offsetX
        drawLine(sx, y1, sx, y2)
    }

    params.prop.setMinorAxis(painter).commit()
    val minorStepY = niceIncrement(yRange, 20)

    val x1 = (params.dataXMin * params.scaleX + params.offsetX).flush()
    val x2 = (params.dataXMax * params.scaleX + params.offsetX).flush()
    loop(params.dataYMin, minorStepY, params.dataYMax + 0.01f) { y ->
        val sy = y * params.scaleY + params.offsetY
        drawLine(x1, sy, x2, sy)
    }
    // ================ major grid  ================
    params.prop.setVMajorAxis(painter).commit()
    val bottom = y1
    val right = x2
    loop(params.dataXMin, majorStepX, params.dataXMax + 0.01f) { x ->
        val posX = x * params.scaleX + params.offsetX
        val id =
            createTextFromFloat(
                x.toFloat(),
                5,
                1,
                Rc.TextFromFloat.PAD_AFTER_ZERO or Rc.TextFromFloat.PAD_PRE_NONE,
            )
        drawTextAnchored(id, posX, bottom, 0f, 1.5f, 0)
        drawLine(posX, y1, posX, y2)
    }

    params.prop.setMajorHAxis(painter).commit()
    val majorStepY = niceIncrement(yRange, 5)
    val end = (params.dataXMin * params.scaleX + params.offsetX).flush()
    loop(params.dataYMin, majorStepY, params.dataYMax + 0.01f) { y ->
        val yPos = y * params.scaleY + params.offsetY
        val id =
            createTextFromFloat(
                y.toFloat(),
                5,
                1,
                Rc.TextFromFloat.PAD_AFTER_ZERO or Rc.TextFromFloat.PAD_PRE_NONE,
            )
        drawTextAnchored(id, insertX, yPos, 1.5f, 0f, 0)
        drawLine(x1, yPos, x2, yPos)
    }

    params.prop.setVAxis(painter).commit()
    drawLine(params.offsetX, y1, params.offsetX, y2)

    params.prop.setHAxis(painter).commit()
    drawLine(x1, params.offsetY, right, params.offsetY)
    // ========================================================

}

@Suppress("RestrictedApiAndroidX")
private fun plotIncrement(range: RFloat, minSteps: Int): RFloat {
    val maxIncrement = range / minSteps.toFloat()
    val n = floor(log(maxIncrement))
    val powerOf10 = pow(10f, n)
    powerOf10.flush()

    val normalizedIncrement = maxIncrement / powerOf10
    val ret =
        ifElse(
            normalizedIncrement - 5f,
            powerOf10 * 5.0f,
            ifElse(normalizedIncrement - 2f, powerOf10 * 2.0f, powerOf10),
        )
    return max(2f, ret)
}

@Suppress("RestrictedApiAndroidX")
private fun niceIncrement(range: RFloat, minSteps: Int): RFloat {
    val maxIncrement = range / minSteps.toFloat()
    val n = floor(log(maxIncrement))
    val powerOf10 = pow(10f, n)
    powerOf10.flush()

    val normalizedIncrement = maxIncrement / powerOf10

    val ret =
        ifElse(
            normalizedIncrement - 5f,
            powerOf10 * 5.0f,
            ifElse(normalizedIncrement - 2f, powerOf10 * 2.0f, powerOf10),
        )
    return ret
}
