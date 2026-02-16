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
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.RFloat
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.arrayLength
import androidx.compose.remote.creation.arraySum
import androidx.compose.remote.creation.arraySumSqr
import androidx.compose.remote.creation.arraySumXY
import androidx.compose.remote.creation.minus
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.plus
import androidx.compose.remote.creation.rf
import androidx.compose.remote.creation.times
import kotlin.random.Random

@Suppress("RestrictedApiAndroidX")
fun demoLinearRegression(): RemoteComposeContextAndroid {
    val nPoints = 50
    val trueSlope = 0.5f
    val trueIntercept = 10f
    val noiseScale = 2f

    val xData = FloatArray(nPoints) { it.toFloat() + (Random.nextFloat() - 0.5f) * noiseScale }
    val yData =
        FloatArray(nPoints) {
            trueSlope * it + trueIntercept + (Random.nextFloat() - 0.5f) * noiseScale * 4f
        }

    val rc =
        RemoteComposeContextAndroid(
            platform = AndroidxRcPlatformServices(),
            apiLevel = 7,
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 500),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 500),
            RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Linear Regression Demo"),
            RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
        ) {
            val density = rf(Rc.System.DENSITY)
            root {
                box(RecordingModifier().fillMaxSize(), BoxLayout.START, BoxLayout.START) {
                    canvas(RecordingModifier().fillMaxSize().background(0xFFF8F8F8.toInt())) {
                        val w = ComponentWidth()
                        val h = ComponentHeight()

                        val rx = rf(addFloatArray(xData))
                        val ry = rf(addFloatArray(yData))

                        val n = arrayLength(rx)
                        val sumX = arraySum(rx).flush()
                        val sumY = arraySum(ry).flush()
                        val sumXX = arraySumSqr(rx)
                        val sumXY = arraySumXY(rx, ry)

                        // slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX)
                        val slope = ((n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX)).flush()
                        // intercept = (sumY - slope * sumX) / n
                        val intercept = ((sumY - slope * sumX) / n).flush()

                        val graphProp = XYGraphProperties()
                        //                        graphProp.hAxisColor = 0xFF000000.toInt();
                        //                        graphProp.vAxisColor = 0xFF000000.toInt();
                        // We need a custom PlotBase for scatter plot
                        val scatterPlot =
                            object : PlotBase {
                                override fun calcRange(rc: RemoteComposeContextAndroid): Range {
                                    return Range(rf(0f), rf(nPoints.toFloat()), rf(0f), rf(40f))
                                }

                                override fun plot(
                                    rc: RemoteComposeContextAndroid,
                                    params: PlotParams,
                                ) {
                                    with(rc) {
                                        painter
                                            .setColor(Color.BLACK)
                                            .setStyle(Paint.Style.FILL)
                                            .commit()
                                        loop(0, 1, n) { i ->
                                            val vx = rx.get(i)
                                            val vy = ry.get(i)
                                            val sx = vx * params.scaleX + params.offsetX
                                            val sy = vy * params.scaleY + params.offsetY
                                            drawCircle(
                                                sx.toFloat(),
                                                sy.toFloat(),
                                                (3f * density).toFloat(),
                                            )
                                        }
                                    }
                                }
                            }

                        val regressionLine =
                            FunctionPlot(
                                rFun { x -> x * slope + intercept },
                                0f,
                                nPoints.toFloat(),
                                0f,
                                40f,
                            )

                        // Overlay the regression line
                        // Let's use a composite plot to draw both scatter and regression line with
                        // one axis call
                        val combinedPlot =
                            object : PlotBase {
                                override fun calcRange(rc: RemoteComposeContextAndroid) =
                                    Range(rf(0f), rf(nPoints.toFloat()), rf(0f), rf(40f))

                                override fun plot(
                                    rc: RemoteComposeContextAndroid,
                                    params: PlotParams,
                                ) {
                                    scatterPlot.plot(rc, params)
                                    params.prop
                                        .setPlotPaint(
                                            rc.painter,
                                            (2f * rc.rf(Rc.System.DENSITY)).toFloat(),
                                        )
                                        .setColor(Color.BLUE)
                                        .commit()
                                    regressionLine.plot(rc, params)
                                }
                            }

                        rcPlotXY(
                            20f * density,
                            20f * density,
                            w - 20f * density,
                            h - 20f * density,
                            graphProp,
                            combinedPlot,
                        )

                        // Display the formula
                        painter.setColor(Color.BLACK).setTextSize((16f * density)).commit()
                        val slopeText = createTextFromFloat(slope, 5, 2, 0)
                        val interceptText = createTextFromFloat(intercept, 5, 2, 0)
                        val formulaId = textCreateId("y = ")
                        val plusId = textCreateId("x + ")
                        val fullFormula =
                            textMerge(
                                formulaId,
                                textMerge(slopeText, textMerge(plusId, interceptText)),
                            )

                        drawTextAnchored(fullFormula, 50f * density, 50f * density, 0f, 0f, 0)
                    }
                }
            }
        }
    return rc
}

@Suppress("RestrictedApiAndroidX")
private fun androidx.compose.remote.creation.Painter.setTextSize(
    size: RFloat
): androidx.compose.remote.creation.Painter {
    this.setTextSize(size.toFloat())
    return this
}
