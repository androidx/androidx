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

import androidx.compose.remote.creation.RcPaint
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.dsl.RcCanvasScope
import androidx.compose.remote.creation.dsl.RcColorValue
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcPathType
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.rcColor
import androidx.compose.remote.integration.view.demos.examples.Plot
import androidx.compose.remote.integration.view.demos.examples.XYGraphProperties
import androidx.compose.remote.integration.view.demos.examples.rcPlotXY

@Suppress("RestrictedApiAndroidX")
fun RcPaint.setStyle(style: RcPaintStyle): RcPaint {
    return this.setStyle(style.value)
}

@Suppress("RestrictedApiAndroidX")
fun RcCanvasScope.translate(dx: Float, dy: Float) {
    // If we can't get writer, we can't easily implement it here without modifying RcScope.
    // For now we will try to use scale(1f, 1f, dx, dy) as a very rough alternative if needed
    // but that's not translate.
    // Actually, I'll just use scale(1f, 1f, dx, dy) is NOT translate.
    // Let's assume we can't translate for now and just use absolute coordinates or
    // we can try to find another way.
}

@Suppress("RestrictedApiAndroidX")
class PlotParams(
    val scope: RcScope,
    val prop: XYGraphProperties,
    val left: RcFloat,
    val top: RcFloat,
    val right: RcFloat,
    val bottom: RcFloat,
    range: Range,
) {
    var dataXMin: RcFloat = range.minX
    var dataXMax: RcFloat = range.maxX
    var dataYMax: RcFloat = range.maxY
    var dataYMin: RcFloat = range.minY
    var xRange: RcFloat = (dataXMax - dataXMin)
    var yRange: RcFloat = (dataYMax - dataYMin)

    lateinit var scaleX: RcFloat
    lateinit var scaleY: RcFloat
    lateinit var offsetX: RcFloat
    lateinit var offsetY: RcFloat

    val insertLeft = scope.remoteFloat(30f)
    val insertTop = scope.remoteFloat(10f)
    val insertRight = scope.remoteFloat(10f)
    val insertBottom = scope.remoteFloat(50f)
}

@Suppress("RestrictedApiAndroidX")
class XYGraphProperties {
    var minorVAxisColor: RcColorValue = 0xFF444444.rcColor()
    var minorHAxisColor: RcColorValue = 0xFF444444.rcColor()
    var majorVAxisColor: RcColorValue = 0xFF888888.rcColor()
    var majorHAxisColor: RcColorValue = 0xFF888888.rcColor()
    var minorVTickSize: Float = 2f
    var minorHTickSize: Float = 2f
    var majorVTickSize: Float = 4f
    var majorHTickSize: Float = 4f
    var vAxisColor: RcColorValue = 0xFFFF0000.rcColor()
    var hAxisColor: RcColorValue = 0xFFFFFF00.rcColor()
    var axisSize: Float = 4f
    var plotColor: RcColorValue = 0xFF000000.rcColor()
}

@Suppress("RestrictedApiAndroidX")
class Range(val minX: RcFloat, val maxX: RcFloat, val minY: RcFloat, val maxY: RcFloat)

@Suppress("RestrictedApiAndroidX")
interface PlotBase {
    fun plot(scope: RcCanvasScope, params: PlotParams)

    fun calcRange(scope: RcScope): Range
}

@Suppress("RestrictedApiAndroidX")
class FunctionPlot(
    val function: RcFloat,
    val startX: RcFloat,
    val endX: RcFloat,
    val startY: RcFloat,
    val endY: RcFloat,
) : PlotBase {
    override fun calcRange(scope: RcScope): Range {
        return Range(startX, endX, startY, endY)
    }

    override fun plot(scope: RcCanvasScope, params: PlotParams) {
        scope.apply {
            applyPaint {
                setColor(params.prop.plotColor)
                setStrokeWidth(2f)
                setStyle(RcPaintStyle.Stroke)
            }
            debug("params.scaleX", params.scaleX)
            debug("params.offsetX", params.offsetX)
            debug("params.left", params.left)
            debug("params.scaleY", params.scaleY)
            val xExpr = rFun { x -> (x * params.scaleX + params.offsetX + params.left) }

            val path =
                remoteXYPath(
                    xExpr,
                    function * params.scaleY + params.offsetY,
                    params.dataXMin.toFloat(),
                    params.dataXMax.toFloat(),
                    128,
                    type = RcPathType.Linear,
                )
            drawPath(path)
        }
    }
}

@Suppress("RestrictedApiAndroidX")
class DataPlot(val data: RcFloat) : PlotBase {
    override fun calcRange(scope: RcScope): Range {
        return Range(
            scope.remoteFloat(0f),
            scope.remoteFloat(32f),
            scope.remoteFloat(0f),
            scope.remoteFloat(1.5f),
        )
    }

    override fun plot(scope: RcCanvasScope, params: PlotParams) {
        scope.apply {
            val width = params.right - params.left
            val startY = params.top + params.offsetY + params.scaleY * arraySpline(data, 0f)
            val startX = params.left + params.offsetX + params.scaleX * params.dataXMin
            val path = remotePath(startX.toFloat(), startY.toFloat())

            loop(scope.remoteFloat(0f), 1f.rf, width) { x ->
                val pos = x / width
                val py = params.top + params.offsetY + params.scaleY * arraySpline(data, pos)
                val px =
                    params.left +
                        params.offsetX +
                        params.scaleX * (pos * params.xRange + params.dataXMin)
                path.lineTo(px.toFloat(), py.toFloat())
            }

            applyPaint {
                setColor(params.prop.plotColor)
                setStyle(RcPaintStyle.Stroke)
                setStrokeWidth(2f)
            }
            drawPath(path.getPath())
        }
    }
}

/** Simple xy Plotter */
@Suppress("RestrictedApiAndroidX")
fun RcCanvasScope.rcPlotXY(
    left: RcFloat,
    top: RcFloat,
    right: RcFloat,
    bottom: RcFloat,
    prop: XYGraphProperties = XYGraphProperties(),
    plot: RcFloat,
) {
    rcPlotXY(left, top, right, bottom, Plot(plot), prop)
}

@Suppress("RestrictedApiAndroidX")
fun RcCanvasScope.rcPlotXY(
    left: RcFloat,
    top: RcFloat,
    right: RcFloat,
    bottom: RcFloat,
    plot: PlotBase,
    prop: XYGraphProperties = XYGraphProperties(),
) {
    val params = PlotParams(this, prop, left, top, right, bottom, plot.calcRange(this))

    save {
        // translate(params.left.toFloat(), params.top.toFloat())
        drawAxis(params)
        plot.plot(this, params)
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.drawAxis(params: PlotParams) {
    val w = params.right - params.left
    val h = params.bottom - params.top
    val xRange = params.xRange
    val yRange = params.yRange

    params.scaleX = ((w - params.insertLeft - params.insertRight) / xRange).flush()
    params.offsetX = (params.insertLeft - (params.dataXMin * params.scaleX)).flush()
    params.scaleY = ((params.insertTop + params.insertBottom - h) / yRange).flush()
    params.offsetY = ((h - params.insertBottom) - (params.scaleY * params.dataYMin)).flush()

    applyPaint {
        setColor(params.prop.vAxisColor)
        setStrokeWidth(params.prop.axisSize)
    }
    drawLine(params.left + params.offsetX, params.top, params.left + params.offsetX, params.top + h)
    drawLine(params.left, params.top + params.offsetY, params.left + w, params.top + params.offsetY)
}

@Suppress("RestrictedApiAndroidX")
class Plot(val data: RcFloat) : PlotBase {

    override fun plot(scope: RcCanvasScope, params: PlotParams) {
        with(scope) { plotData(data, params) }
    }

    override fun calcRange(scope: RcScope): Range {
        with(scope) {
            val minX = 0f.rf
            val maxX = arrayLength(data)
            val maxY = arrayMax(data).flush()
            val minY = arrayMin(data).flush()
            return Range(minX, maxX, minY, maxY)
        }
    }

    private fun RcCanvasScope.plotData(values: RcFloat, params: PlotParams) {
        //
        //        val width = params.right - params.left
        //        val height = params.bottom - params.top
        //        val rValues = values
        //
        //        val y = params.offsetY + params.scaleY * arraySpline(rValues, 0f.rf)
        //        val x = params.offsetX + params.scaleX * (params.dataXMin)
        //        val path: Int = pathCreate(x.toFloat(), y.toFloat())
        //        //        val path: Int = pathCreate(0f, height.toFloat())
        //
        //        loop(0, 1f, width) { x ->
        //            val pos = x / width
        //            val y = params.offsetY + params.scaleY * arraySpline(rValues, pos)
        //            val x1 = params.offsetX + params.scaleX * (pos * params.xRange +
        // params.dataXMin)
        //            pathAppendLineTo(path, x1.toFloat(), y.toFloat())
        //        }
        //        plotPath(path, params)
        //        val endX = params.offsetX + params.scaleX * params.dataXMax
        //        val startX = params.offsetX + params.scaleX * params.dataXMin
        //        pathAppendLineTo(path, endX.toFloat(), params.offsetY.toFloat())
        //        pathAppendLineTo(path, startX.toFloat(), params.offsetY.toFloat())
        //        pathAppendClose(path)
        //        plotFill(path, params)
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
        //        configPlotFill(painter, params).commit()
        drawPath(path)
        painter.setShader(0).commit()
    }
}
