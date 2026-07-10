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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.integration.view.demos.examples

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.arrayMax
import androidx.compose.remote.creation.arrayMin
import androidx.compose.remote.creation.arraySpline
import androidx.compose.remote.creation.arraySum
import androidx.compose.remote.creation.cos
import androidx.compose.remote.creation.floor
import androidx.compose.remote.creation.max
import androidx.compose.remote.creation.min
import androidx.compose.remote.creation.minus
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.plus
import androidx.compose.remote.creation.sin
import androidx.compose.remote.creation.times
import androidx.compose.remote.creation.toRad

// =====================================================================
// 1. Activity Rings
//    Three concentric circular progress rings for daily goals
// =====================================================================
fun demoActivityRings(): RemoteComposeWriter {
    val data = floatArrayOf(78f, 62f, 91f) // Move%, Exercise%, Stand%
    val ringColors = intArrayOf(0xFFFF2D55L.toInt(), 0xFF4CD964L.toInt(), 0xFF5AC8FAL.toInt())
    val trackColors = intArrayOf(0x44FF2D55L.toInt(), 0x444CD964L.toInt(), 0x445AC8FAL.toInt())
    val labels = arrayOf("Move", "Exercise", "Stand")

    val rc =
        RemoteComposeContextAndroid(
            platform = AndroidxRcPlatformServices(),
            apiLevel = 7,
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
            RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Activity Rings"),
        ) {
            root {
                canvas(RecordingModifier().fillMaxSize().background(0xFF1C1C1EL.toInt())) {
                    val w = ComponentWidth()
                    val h = ComponentHeight()
                    val cx = w / 2f
                    val cy = h * 0.43f
                    val density = rf(Rc.System.DENSITY)
                    val size = min(w, h)
                    val strokeW = size * 0.06f
                    val ringGap = strokeW * 1.6f

                    val values = rf(addFloatArray(data))

                    for (i in 0..2) {
                        val radius = size * 0.38f - rf(i.toFloat()) * ringGap
                        val progress = values.get(rf(i.toFloat()))
                        val sweepAngle = progress / 100f * 360f

                        // Background track
                        painter
                            .setColor(trackColors[i])
                            .setStyle(Paint.Style.STROKE)
                            .setStrokeWidth(strokeW.toFloat())
                            .setStrokeCap(Paint.Cap.ROUND)
                            .commit()
                        drawArc(cx - radius, cy - radius, cx + radius, cy + radius, 0, 360)

                        // Progress arc from 12 o'clock
                        painter.setColor(ringColors[i]).commit()
                        drawArc(cx - radius, cy - radius, cx + radius, cy + radius, -90, sweepAngle)
                    }

                    // Labels row below rings
                    painter.setStyle(Paint.Style.FILL).setStrokeWidth(0f).commit()
                    for (i in 0..2) {
                        val progress = values.get(rf(i.toFloat()))
                        val labelX = w * (0.2f + i.toFloat() * 0.3f)
                        val labelY = h * 0.85f
                        painter
                            .setColor(ringColors[i])
                            .setTextSize((density * 22f).toFloat())
                            .setTypeface(0, 700, false)
                            .commit()
                        val pctText =
                            createTextFromFloat(progress, 3, 0, Rc.TextFromFloat.PAD_PRE_NONE)
                        val pctLabel = textMerge(pctText, addText("%"))
                        drawTextAnchored(pctLabel, labelX, labelY, 0, 0, 0)

                        painter
                            .setTextSize((density * 11f).toFloat())
                            .setTypeface(0, 400, false)
                            .commit()
                        drawTextAnchored(labels[i], labelX, labelY, 0, 2.5f, 0)
                    }
                }
            }
        }
    return rc.writer
}

// =====================================================================
// 2. Heart Rate Timeline
//    24-hour HR line graph with color zones and pulsing BPM display
// =====================================================================
fun demoHeartRateTimeline(): RemoteComposeWriter {
    val hrData =
        floatArrayOf(
            62f,
            58f,
            55f,
            54f,
            56f,
            60f,
            72f,
            85f,
            95f,
            88f,
            78f,
            82f,
            90f,
            76f,
            70f,
            68f,
            105f,
            140f,
            130f,
            95f,
            80f,
            72f,
            65f,
            60f,
        )
    val rc =
        RemoteComposeContextAndroid(
            platform = AndroidxRcPlatformServices(),
            apiLevel = 7,
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 500),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
            RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Heart Rate Timeline"),
        ) {
            root {
                canvas(RecordingModifier().fillMaxSize().background(0xFF1A1A2EL.toInt())) {
                    val w = ComponentWidth()
                    val h = ComponentHeight()
                    val density = rf(Rc.System.DENSITY)
                    val values = rf(addFloatArray(hrData))

                    val gLeft = density * 20f
                    val gRight = w - density * 8f
                    val gTop = h * 0.28f
                    val gBottom = h * 0.82f
                    val gW = gRight - gLeft
                    val gH = gBottom - gTop
                    val minHR = 40f
                    val maxHR = 160f
                    val hrRange = maxHR - minHR

                    // Color zone backgrounds
                    val zones =
                        arrayOf(
                            Triple(40f, 60f, 0x30448AFFL.toInt()), // Resting blue
                            Triple(60f, 100f, 0x304CD964L.toInt()), // Normal green
                            Triple(100f, 130f, 0x30FFCC00L.toInt()), // Elevated yellow
                            Triple(130f, 160f, 0x30FF3B30L.toInt()), // Peak red
                        )
                    painter.setStyle(Paint.Style.FILL).setStrokeWidth(0f).commit()
                    for ((lo, hi, color) in zones) {
                        painter.setColor(color).commit()
                        val y1 = gBottom - (hi - minHR) / hrRange * gH
                        val y2 = gBottom - (lo - minHR) / hrRange * gH
                        drawRect(gLeft, y1, gW, y2 - y1)
                    }

                    // HR curve via arraySpline
                    val startY = gBottom - (arraySpline(values, rf(0f)) - minHR) / hrRange * gH
                    val linePath = pathCreate(gLeft.toFloat(), startY.toFloat())
                    val steps = 80f
                    loop(1, 1, steps) { step ->
                        val t = step / steps
                        val x = gLeft + gW * t
                        val hrVal = arraySpline(values, t)
                        val y = gBottom - (hrVal - minHR) / hrRange * gH
                        pathAppendLineTo(linePath, x.toFloat(), y.toFloat())
                    }
                    painter
                        .setColor(0xFFFF3B30L.toInt())
                        .setStyle(Paint.Style.STROKE)
                        .setStrokeWidth((density * 2.5f).toFloat())
                        .setStrokeCap(Paint.Cap.ROUND)
                        .commit()
                    drawPath(linePath)

                    // Hour labels
                    painter
                        .setColor(0x99FFFFFFL.toInt())
                        .setStyle(Paint.Style.FILL)
                        .setTextSize((density * 10f).toFloat())
                        .commit()
                    for (hr in intArrayOf(0, 6, 12, 18)) {
                        val x = gLeft + gW * rf(hr.toFloat()) / 23f
                        drawTextAnchored("${hr}h", x, gBottom + density * 12f, 0, 0, 0)
                    }

                    // Current BPM with pulsing effect
                    val pulse = sin(ContinuousSec() * 8f) * 0.15f + 1.0f
                    val bpmSize = density * 38f * pulse
                    painter
                        .setColor(0xFFFF3B30L.toInt())
                        .setTextSize(bpmSize.toFloat())
                        .setTypeface(0, 700, false)
                        .commit()
                    val bpmText =
                        createTextFromFloat(
                            values.get(rf(23f)),
                            3,
                            0,
                            Rc.TextFromFloat.PAD_PRE_NONE,
                        )
                    drawTextAnchored(bpmText, w * 0.75f, h * 0.12f, 0, 0, 0)
                    painter
                        .setTextSize((density * 14f).toFloat())
                        .setTypeface(0, 400, false)
                        .setColor(0xAAFFFFFFL.toInt())
                        .commit()
                    drawTextAnchored("BPM", w * 0.75f, h * 0.12f, 0, 3f, 0)

                    // Heart icon
                    painter
                        .setColor(0xFFFF3B30L.toInt())
                        .setTextSize((density * 20f * pulse).toFloat())
                        .commit()
                    drawTextAnchored("\u2665", w * 0.75f, h * 0.12f, 4f, 0, 0)
                }
            }
        }
    return rc.writer
}

// =====================================================================
// 3. Step Progress Arc
//    Semicircular arc showing step count progress toward daily goal
// =====================================================================
fun demoStepProgressArc(): RemoteComposeWriter {
    val data = floatArrayOf(7250f, 10000f) // currentSteps, goal

    val rc =
        RemoteComposeContextAndroid(
            platform = AndroidxRcPlatformServices(),
            apiLevel = 7,
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 350),
            RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Step Progress Arc"),
        ) {
            root {
                canvas(RecordingModifier().fillMaxSize().background(0xFF16213EL.toInt())) {
                    val w = ComponentWidth()
                    val h = ComponentHeight()
                    val cx = w / 2f
                    val cy = h * 0.55f
                    val density = rf(Rc.System.DENSITY)
                    val size = min(w, h)
                    val arcRadius = size * 0.4f
                    val strokeW = size * 0.07f

                    val values = rf(addFloatArray(data))
                    val current = values.get(rf(0f))
                    val goal = values.get(rf(1f))
                    val progress = min(current / goal, rf(1f))

                    // Background track (semicircle, 180° from left to right)
                    painter
                        .setColor(0x33FFFFFFL.toInt())
                        .setStyle(Paint.Style.STROKE)
                        .setStrokeWidth(strokeW.toFloat())
                        .setStrokeCap(Paint.Cap.ROUND)
                        .commit()
                    drawArc(
                        cx - arcRadius,
                        cy - arcRadius,
                        cx + arcRadius,
                        cy + arcRadius,
                        180,
                        180,
                    )

                    // Progress arc
                    painter.setColor(0xFF00D2FFL.toInt()).commit()
                    drawArc(
                        cx - arcRadius,
                        cy - arcRadius,
                        cx + arcRadius,
                        cy + arcRadius,
                        180,
                        progress * 180f,
                    )

                    // Milestone markers at 25%, 50%, 75%, 100%
                    painter
                        .setColor(0x88FFFFFFL.toInt())
                        .setStrokeWidth((density * 2f).toFloat())
                        .commit()
                    val tickLen = strokeW * 0.6f
                    for (pct in intArrayOf(25, 50, 75, 100)) {
                        val angle = 180f + pct.toFloat() / 100f * 180f
                        val rad = toRad(rf(angle))
                        val outerR = arcRadius + strokeW / 2f + density * 2f
                        val innerR = arcRadius + strokeW / 2f + tickLen
                        val tx1 = cx + outerR * cos(rad)
                        val ty1 = cy + outerR * sin(rad)
                        val tx2 = cx + innerR * cos(rad)
                        val ty2 = cy + innerR * sin(rad)
                        drawLine(tx1, ty1, tx2, ty2)

                        // Label
                        painter
                            .setStyle(Paint.Style.FILL)
                            .setTextSize((density * 10f).toFloat())
                            .commit()
                        val lR = arcRadius + strokeW / 2f + tickLen + density * 10f
                        drawTextAnchored("$pct%", cx + lR * cos(rad), cy + lR * sin(rad), 0, 0, 0)
                        painter.setStyle(Paint.Style.STROKE).commit()
                    }

                    // Step count text
                    painter
                        .setColor(Color.WHITE)
                        .setStyle(Paint.Style.FILL)
                        .setTextSize((density * 40f).toFloat())
                        .setTypeface(0, 700, false)
                        .commit()
                    val stepsText =
                        createTextFromFloat(current, 5, 0, Rc.TextFromFloat.PAD_PRE_NONE)
                    drawTextAnchored(stepsText, cx, cy - density * 15f, 0, 0, 0)

                    // Goal text
                    painter
                        .setColor(0xAAFFFFFFL.toInt())
                        .setTextSize((density * 16f).toFloat())
                        .setTypeface(0, 400, false)
                        .commit()
                    val goalLabel =
                        textMerge(
                            addText("/ "),
                            createTextFromFloat(goal, 5, 0, Rc.TextFromFloat.PAD_PRE_NONE),
                            addText(" steps"),
                        )
                    drawTextAnchored(goalLabel, cx, cy + density * 8f, 0, 0, 0)
                }
            }
        }
    return rc.writer
}

// =====================================================================
// 4. Weather Forecast Bars
//    Vertical bar chart showing hourly temperature + precipitation
// =====================================================================
fun demoWeatherForecastBars(): RemoteComposeWriter {
    val tempData = floatArrayOf(18f, 17f, 16f, 17f, 19f, 22f, 25f, 28f, 30f, 29f, 27f, 24f)
    val precipData = floatArrayOf(0f, 0f, 10f, 20f, 5f, 0f, 0f, 0f, 15f, 40f, 60f, 30f)
    val hours = arrayOf("6a", "7a", "8a", "9a", "10a", "11a", "12p", "1p", "2p", "3p", "4p", "5p")

    val rc =
        RemoteComposeContextAndroid(
            platform = AndroidxRcPlatformServices(),
            apiLevel = 7,
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 500),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
            RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Weather Forecast Bars"),
        ) {
            root {
                canvas(RecordingModifier().fillMaxSize().background(0xFF0A1628L.toInt())) {
                    val w = ComponentWidth()
                    val h = ComponentHeight()
                    val density = rf(Rc.System.DENSITY)

                    val temps = rf(addFloatArray(tempData))
                    val precips = rf(addFloatArray(precipData))

                    val marginL = density * 35f
                    val marginR = density * 10f
                    val marginTop = density * 50f
                    val marginBot = density * 40f
                    val graphW = w - (marginL - marginR).flush()
                    val graphH = h - marginTop - marginBot
                    val graphBottom = h - marginBot
                    val barCount = 12f
                    val barSpacing = graphW / barCount
                    val barWidth = barSpacing * 0.6f
                    val minTemp = 10f
                    val maxTemp = 35f
                    val tempRange = maxTemp - minTemp

                    // Title
                    painter
                        .setColor(Color.WHITE)
                        .setStyle(Paint.Style.FILL)
                        .setTextSize((density * 16f).toFloat())
                        .setTypeface(0, 700, false)
                        .commit()
                    drawTextAnchored("Hourly Forecast", w / 2f, density * 18f, 0, 0, 0)

                    // Temperature bars via player-side loop
                    loop(0, 1, barCount) { i ->
                        val temp = temps.get(i)
                        val normalizedTemp = ((temp - minTemp) / tempRange).flush()
                        val barH = max(density * 4f, normalizedTemp * graphH).flush()
                        val barX = (marginL + i * barSpacing + (barSpacing - barWidth) / 2f).flush()
                        val barY = graphBottom - barH

                        // Color by temperature: blue(cold) → red(hot) via HSV
                        val hue = (1f - normalizedTemp) * 0.66f
                        val colorId = addColorExpression(0xFF, hue.toFloat(), 0.8f, 0.9f)
                        painter.setColorId(colorId.toInt()).setStyle(Paint.Style.FILL).commit()
                        drawRoundRect(barX, barY, barWidth, barH, density * 3f, density * 3f)

                        // Temperature label on top of bar
                        painter
                            .setColor(Color.WHITE)
                            .setTextSize((density * 10f).toFloat())
                            .setTypeface(0, 600, false)
                            .commit()
                        val tempLabel =
                            createTextFromFloat(temp, 2, 0, Rc.TextFromFloat.PAD_PRE_NONE)
                        val tempStr = textMerge(tempLabel, addText("°"))
                        drawTextAnchored(
                            tempStr,
                            barX + barWidth / 2f,
                            barY - density * 6f,
                            0,
                            0,
                            0,
                        )

                        // Precipitation indicator (blue dot, size = probability)
                        val precip = precips.get(i)
                        val dotRadius = precip / 100f * density * 6f
                        painter.setColor(0xAA4488FFL.toInt()).commit()
                        drawCircle(
                            (barX + barWidth / 2f).toFloat(),
                            (graphBottom + density * 10f).toFloat(),
                            dotRadius.toFloat(),
                        )
                    }

                    // Hour labels (Kotlin loop for string access)
                    painter
                        .setColor(0x99FFFFFFL.toInt())
                        .setTextSize((density * 9f).toFloat())
                        .setTypeface(0, 400, false)
                        .commit()
                    for (i in hours.indices) {
                        val x = marginL + rf(i.toFloat()) * barSpacing + barSpacing / 2f
                        drawTextAnchored(hours[i], x, graphBottom + density * 22f, 0, 0, 0)
                    }

                    // Y-axis temperature labels
                    painter
                        .setColor(0x66FFFFFFL.toInt())
                        .setTextSize((density * 9f).toFloat())
                        .commit()
                    for (t in intArrayOf(15, 20, 25, 30)) {
                        val y = graphBottom - (t.toFloat() - minTemp) / tempRange * graphH
                        drawTextAnchored("$t°", marginL - density * 6f, y, 1f, 0, 0)
                        painter
                            .setColor(0x22FFFFFFL.toInt())
                            .setStrokeWidth(1f)
                            .setStyle(Paint.Style.STROKE)
                            .commit()
                        drawLine(marginL, y, w - marginR, y)
                        painter.setColor(0x66FFFFFFL.toInt()).setStyle(Paint.Style.FILL).commit()
                    }
                }
            }
        }
    return rc.writer
}

// =====================================================================
// 5. Sleep Quality Rings
//    Color-coded ring segments showing sleep stages
// =====================================================================
fun demoSleepQualityRings(): RemoteComposeWriter {
    // Minutes in each stage: deep, light, REM, awake
    val stageData = floatArrayOf(110f, 180f, 90f, 20f)
    val stageColors =
        intArrayOf(
            0xFF1A237EL.toInt(), // Deep - dark blue
            0xFF42A5F5L.toInt(), // Light - light blue
            0xFF7E57C2L.toInt(), // REM - purple
            0xFFFF7043L.toInt(), // Awake - orange
        )
    val stageLabels = arrayOf("Deep", "Light", "REM", "Awake")

    val rc =
        RemoteComposeContextAndroid(
            platform = AndroidxRcPlatformServices(),
            apiLevel = 7,
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
            RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Sleep Quality Rings"),
        ) {
            root {
                canvas(RecordingModifier().fillMaxSize().background(0xFF0D1B2AL.toInt())) {
                    val w = ComponentWidth()
                    val h = ComponentHeight()
                    val cx = w / 2f
                    val cy = h * 0.45f
                    val density = rf(Rc.System.DENSITY)
                    val size = min(w, h)
                    val ringRadius = size * 0.35f
                    val strokeW = size * 0.09f

                    val values = rf(addFloatArray(stageData))
                    val total = arraySum(values)

                    // Draw ring segments
                    var currentAngle = -90f // Start from 12 o'clock
                    for (i in stageData.indices) {
                        val stageMin = values.get(rf(i.toFloat()))
                        val sweepAngle = stageMin / total * 360f

                        painter
                            .setColor(stageColors[i])
                            .setStyle(Paint.Style.STROKE)
                            .setStrokeWidth(strokeW.toFloat())
                            .setStrokeCap(Paint.Cap.BUTT)
                            .commit()
                        drawArc(
                            cx - ringRadius,
                            cy - ringRadius,
                            cx + ringRadius,
                            cy + ringRadius,
                            currentAngle,
                            sweepAngle,
                        )

                        // Thin white separator
                        painter
                            .setColor(0xFF0D1B2AL.toInt())
                            .setStrokeWidth((density * 2f).toFloat())
                            .commit()
                        save {
                            rotate(currentAngle + sweepAngle, cx, cy)
                            drawLine(
                                cx,
                                (cy - ringRadius - strokeW / 2f),
                                cx,
                                (cy - ringRadius + strokeW / 2f),
                            )
                        }

                        currentAngle += sweepAngle.toFloat()
                    }

                    // Center text: total sleep time
                    val totalMinutes = total
                    val hrs = floor(totalMinutes / 60f)
                    val mins = totalMinutes - hrs * 60f

                    painter
                        .setColor(Color.WHITE)
                        .setStyle(Paint.Style.FILL)
                        .setTextSize((density * 32f).toFloat())
                        .setTypeface(0, 700, false)
                        .commit()
                    val hrsText = createTextFromFloat(hrs, 1, 0, Rc.TextFromFloat.PAD_PRE_NONE)
                    val minsText = createTextFromFloat(mins, 2, 0, Rc.TextFromFloat.PAD_PRE_NONE)
                    val timeLabel = textMerge(hrsText, addText("h "), minsText, addText("m"))
                    drawTextAnchored(timeLabel, cx, cy, 0, 0, 0)

                    painter
                        .setColor(0xAAFFFFFFL.toInt())
                        .setTextSize((density * 13f).toFloat())
                        .setTypeface(0, 400, false)
                        .commit()
                    drawTextAnchored("Total Sleep", cx, cy, 0, 3.5f, 0)

                    // Legend
                    for (i in stageData.indices) {
                        val lx = w * 0.15f + rf(i.toFloat()) * w * 0.2f
                        val ly = h * 0.88f
                        painter.setColor(stageColors[i]).setStyle(Paint.Style.FILL).commit()
                        drawCircle(lx.toFloat(), ly.toFloat(), (density * 4f).toFloat())

                        painter
                            .setColor(0xCCFFFFFFL.toInt())
                            .setTextSize((density * 10f).toFloat())
                            .commit()
                        drawTextAnchored(stageLabels[i], lx, ly + density * 12f, 0, 0, 0)
                    }
                }
            }
        }
    return rc.writer
}

// =====================================================================
// 6. Battery Radial Gauge
//    270-degree radial gauge with green→yellow→red gradient
// =====================================================================
fun demoBatteryRadialGauge(): RemoteComposeWriter {
    val data = floatArrayOf(67f, 8.5f) // percentage, estimatedHoursRemaining

    val rc =
        RemoteComposeContextAndroid(
            platform = AndroidxRcPlatformServices(),
            apiLevel = 7,
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
            RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Battery Radial Gauge"),
        ) {
            root {
                canvas(RecordingModifier().fillMaxSize().background(0xFF1A1A2EL.toInt())) {
                    val w = ComponentWidth()
                    val h = ComponentHeight()
                    val cx = w / 2f
                    val cy = h * 0.45f
                    val density = rf(Rc.System.DENSITY)
                    val size = min(w, h)
                    val gaugeRadius = size * 0.38f
                    val strokeW = size * 0.08f

                    val values = rf(addFloatArray(data))
                    val level = values.get(rf(0f))
                    val hoursLeft = values.get(rf(1f))

                    // Background track (270°, gap at bottom)
                    painter
                        .setColor(0x33FFFFFFL.toInt())
                        .setStyle(Paint.Style.STROKE)
                        .setStrokeWidth(strokeW.toFloat())
                        .setStrokeCap(Paint.Cap.ROUND)
                        .commit()
                    drawArc(
                        cx - gaugeRadius,
                        cy - gaugeRadius,
                        cx + gaugeRadius,
                        cy + gaugeRadius,
                        135,
                        270,
                    )

                    // Active gauge arc - color from green(100%) to red(0%) via HSV
                    val normalized = level / 100f
                    val hue = normalized * 0.33f // 0=red, 0.33=green
                    val gaugeColor = addColorExpression(0xFF, hue.toFloat(), 0.9f, 0.9f)
                    painter.setColorId(gaugeColor.toInt()).commit()
                    drawArc(
                        cx - gaugeRadius,
                        cy - gaugeRadius,
                        cx + gaugeRadius,
                        cy + gaugeRadius,
                        135,
                        normalized * 270f,
                    )

                    // Scale ticks
                    painter
                        .setColor(0x55FFFFFFL.toInt())
                        .setStrokeWidth((density * 1.5f).toFloat())
                        .commit()
                    val tickR1 = gaugeRadius + strokeW / 2f + density * 3f
                    val tickR2 = tickR1 + density * 6f
                    for (pct in intArrayOf(0, 25, 50, 75, 100)) {
                        val angle = 135f + pct.toFloat() / 100f * 270f
                        val rad = toRad(rf(angle))
                        drawLine(
                            cx + tickR1 * cos(rad),
                            cy + tickR1 * sin(rad),
                            cx + tickR2 * cos(rad),
                            cy + tickR2 * sin(rad),
                        )
                    }

                    // Percentage text
                    painter
                        .setColor(Color.WHITE)
                        .setStyle(Paint.Style.FILL)
                        .setTextSize((density * 48f).toFloat())
                        .setTypeface(0, 700, false)
                        .commit()
                    val pctText = createTextFromFloat(level, 3, 0, Rc.TextFromFloat.PAD_PRE_NONE)
                    val pctLabel = textMerge(pctText, addText("%"))
                    drawTextAnchored(pctLabel, cx, cy, 0, 0, 0)

                    // Hours remaining
                    painter
                        .setColor(0xAAFFFFFFL.toInt())
                        .setTextSize((density * 14f).toFloat())
                        .setTypeface(0, 400, false)
                        .commit()
                    val hrsText =
                        createTextFromFloat(hoursLeft, 2, 1, Rc.TextFromFloat.PAD_PRE_NONE)
                    val hrsLabel = textMerge(hrsText, addText("h remaining"))
                    drawTextAnchored(hrsLabel, cx, cy, 0, 3.5f, 0)

                    // Battery icon indicator
                    painter
                        .setColorId(gaugeColor.toInt())
                        .setTextSize((density * 20f).toFloat())
                        .commit()
                    drawTextAnchored("\u26A1", cx, cy, 0, -3.5f, 0)
                }
            }
        }
    return rc.writer
}

// =====================================================================
// 7. Calendar Heatmap Grid
//    7x5 grid showing activity intensity over 35 days
// =====================================================================
fun demoCalendarHeatmapGrid(): RemoteComposeWriter {
    val activityData =
        FloatArray(35) { i ->
            // Simulated activity data: 0 = none, 1 = max
            val patterns =
                floatArrayOf(
                    0.2f,
                    0.8f,
                    0.6f,
                    0.0f,
                    0.4f,
                    0.9f,
                    0.1f,
                    0.5f,
                    0.7f,
                    0.3f,
                    0.0f,
                    0.6f,
                    1.0f,
                    0.2f,
                    0.4f,
                    0.9f,
                    0.8f,
                    0.1f,
                    0.3f,
                    0.7f,
                    0.0f,
                    0.6f,
                    0.5f,
                    0.7f,
                    0.0f,
                    0.8f,
                    0.4f,
                    0.3f,
                    0.3f,
                    0.6f,
                    0.9f,
                    0.2f,
                    0.5f,
                    0.7f,
                    0.8f,
                )
            patterns[i]
        }
    val dayLabels = arrayOf("M", "T", "W", "T", "F", "S", "S")
    val currentDayIndex = 33 // Highlighted day (0-indexed)

    val rc =
        RemoteComposeContextAndroid(
            platform = AndroidxRcPlatformServices(),
            apiLevel = 7,
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
            RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Calendar Heatmap Grid"),
        ) {
            root {
                canvas(RecordingModifier().fillMaxSize().background(0xFF161B22L.toInt())) {
                    val w = ComponentWidth()
                    val h = ComponentHeight()
                    val density = rf(Rc.System.DENSITY)

                    val values = rf(addFloatArray(activityData))

                    val gridLeft = w * 0.15f
                    val gridTop = h * 0.22f
                    val gridW = w * 0.75f
                    val gridH = h * 0.6f
                    val cols = 7f
                    val rows = 5f
                    val cellW = gridW / cols
                    val cellH = gridH / rows
                    val gap = density * 3f

                    // Title
                    painter
                        .setColor(Color.WHITE)
                        .setStyle(Paint.Style.FILL)
                        .setTextSize((density * 16f).toFloat())
                        .setTypeface(0, 700, false)
                        .commit()
                    drawTextAnchored("Activity · Past 35 Days", w / 2f, density * 20f, 0, 0, 0)

                    // Day-of-week headers
                    painter
                        .setColor(0x99FFFFFFL.toInt())
                        .setTextSize((density * 11f).toFloat())
                        .setTypeface(0, 400, false)
                        .commit()
                    for (d in 0..6) {
                        val x = gridLeft + rf(d.toFloat()) * cellW + cellW / 2f
                        drawTextAnchored(dayLabels[d], x, gridTop - density * 8f, 0, 0, 0)
                    }

                    // Heatmap cells via player-side loop
                    loop(0, 1, 35) { idx ->
                        val col = idx % 7f
                        val row = floor(idx / 7f)
                        val cellX = gridLeft + col * cellW + gap / 2f
                        val cellY = gridTop + row * cellH + gap / 2f
                        val intensity = values.get(idx)

                        // Color: dark green (low) to bright green (high)
                        val colorId =
                            addColorExpression(
                                0xFF,
                                0.33f, // green hue
                                (intensity * 0.8f + 0.1f).toFloat(),
                                (intensity * 0.7f + 0.15f).toFloat(),
                            )
                        painter.setColorId(colorId.toInt()).setStyle(Paint.Style.FILL).commit()
                        drawRoundRect(
                            cellX,
                            cellY,
                            cellW - gap,
                            cellH - gap,
                            density * 3f,
                            density * 3f,
                        )
                    }

                    // Highlight current day with border
                    val curCol = currentDayIndex % 7
                    val curRow = currentDayIndex / 7
                    val hlX = gridLeft + rf(curCol.toFloat()) * cellW + gap / 2f
                    val hlY = gridTop + rf(curRow.toFloat()) * cellH + gap / 2f
                    painter
                        .setColor(Color.WHITE)
                        .setStyle(Paint.Style.STROKE)
                        .setStrokeWidth((density * 2f).toFloat())
                        .commit()
                    drawRoundRect(hlX, hlY, cellW - gap, cellH - gap, density * 3f, density * 3f)

                    // Legend
                    painter.setStyle(Paint.Style.FILL).commit()
                    val legendY = gridTop + gridH + density * 30f
                    painter
                        .setColor(0x99FFFFFFL.toInt())
                        .setTextSize((density * 10f).toFloat())
                        .commit()
                    drawTextAnchored("Less", w * 0.3f, legendY, 1.5f, 0, 0)
                    drawTextAnchored("More", w * 0.7f, legendY, -1.5f, 0, 0)
                    for (level in 0..4) {
                        val lx = w * 0.35f + rf(level.toFloat()) * density * 16f
                        val intensity = level.toFloat() / 4f
                        val colorId =
                            addColorExpression(
                                0xFF,
                                0.33f,
                                intensity * 0.8f + 0.1f,
                                intensity * 0.7f + 0.15f,
                            )
                        painter.setColorId(colorId.toInt()).commit()
                        drawRoundRect(
                            lx,
                            legendY - density * 5f,
                            density * 10f,
                            density * 10f,
                            density * 2f,
                            density * 2f,
                        )
                    }
                }
            }
        }
    return rc.writer
}

// =====================================================================
// 8. Stock/Crypto Sparkline
//    Minimal line chart with gradient fill, price and change display
// =====================================================================
fun demoStockSparkline(): RemoteComposeWriter {
    val priceData =
        floatArrayOf(
            42150f,
            42380f,
            42200f,
            41900f,
            41750f,
            42000f,
            42400f,
            42800f,
            43100f,
            43050f,
            42900f,
            43200f,
            43500f,
            43400f,
            43800f,
            44100f,
            44000f,
            43700f,
            43900f,
            44200f,
            44500f,
            44300f,
            44600f,
            44850f,
        )

    val rc =
        RemoteComposeContextAndroid(
            platform = AndroidxRcPlatformServices(),
            apiLevel = 7,
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 500),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 350),
            RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Stock Sparkline"),
        ) {
            root {
                canvas(RecordingModifier().fillMaxSize().background(0xFF1C1C1EL.toInt())) {
                    val w = ComponentWidth()
                    val h = ComponentHeight()
                    val density = rf(Rc.System.DENSITY)

                    val prices = rf(addFloatArray(priceData))
                    val priceMin = arrayMin(prices)
                    val priceMax = arrayMax(prices)
                    val priceRange = max(priceMax - priceMin, rf(1f))
                    val firstPrice = prices.get(rf(0f))
                    val lastPrice = prices.get(rf(23f))
                    val changeAmt = lastPrice - firstPrice
                    val changePct = changeAmt / firstPrice * 100f

                    val gLeft = density * 15f
                    val gRight = w - density * 15f
                    val gTop = h * 0.38f
                    val gBottom = h * 0.82f
                    val gW = gRight - gLeft
                    val gH = gBottom - gTop

                    // Current price
                    painter
                        .setColor(Color.WHITE)
                        .setStyle(Paint.Style.FILL)
                        .setTextSize((density * 32f).toFloat())
                        .setTypeface(0, 700, false)
                        .commit()
                    val priceText =
                        createTextFromFloat(lastPrice, 5, 0, Rc.TextFromFloat.PAD_PRE_NONE)
                    drawTextAnchored(priceText, density * 20f, h * 0.12f, -1, 0, 0)

                    // Change percentage
                    painter
                        .setColor(0xFF4CD964L.toInt())
                        .setTextSize((density * 16f).toFloat())
                        .commit()
                    val changeText =
                        createTextFromFloat(
                            changePct,
                            2,
                            2,
                            Rc.TextFromFloat.PAD_AFTER_ZERO or Rc.TextFromFloat.PAD_PRE_NONE,
                        )
                    val changeLabel = textMerge(addText("\u25B2 "), changeText, addText("%"))
                    drawTextAnchored(changeLabel, density * 20f, h * 0.22f, -1, 0, 0)

                    painter
                        .setColor(0x66FFFFFFL.toInt())
                        .setTextSize((density * 11f).toFloat())
                        .commit()
                    drawTextAnchored("24h", density * 20f, h * 0.30f, -1, 0, 0)

                    // Build sparkline path
                    val startY =
                        gBottom - (arraySpline(prices, rf(0f)) - priceMin) / priceRange * gH
                    val linePath = pathCreate(gLeft.toFloat(), startY.toFloat())

                    val steps = 80f
                    loop(1, 1, steps) { step ->
                        val t = step / steps
                        val x = gLeft + gW * t
                        val price = arraySpline(prices, t)
                        val y = gBottom - (price - priceMin) / priceRange * gH
                        pathAppendLineTo(linePath, x.toFloat(), y.toFloat())
                    }

                    // Draw line
                    painter
                        .setColor(0xFF4CD964L.toInt())
                        .setStyle(Paint.Style.STROKE)
                        .setStrokeWidth((density * 2f).toFloat())
                        .setStrokeCap(Paint.Cap.ROUND)
                        .commit()
                    drawPath(linePath)

                    // Close path for gradient fill
                    pathAppendLineTo(linePath, gRight.toFloat(), gBottom.toFloat())
                    pathAppendLineTo(linePath, gLeft.toFloat(), gBottom.toFloat())
                    pathAppendClose(linePath)

                    painter
                        .setStyle(Paint.Style.FILL)
                        .setLinearGradient(
                            gLeft.toFloat(),
                            gTop.toFloat(),
                            gLeft.toFloat(),
                            gBottom.toFloat(),
                            intArrayOf(0x664CD964L.toInt(), 0x00000000),
                            null,
                            Shader.TileMode.CLAMP,
                        )
                        .commit()
                    drawPath(linePath)
                    painter.setShader(0).commit()

                    // Min/max indicators
                    painter
                        .setColor(0x66FFFFFFL.toInt())
                        .setTextSize((density * 9f).toFloat())
                        .setStyle(Paint.Style.FILL)
                        .commit()
                    val minText = createTextFromFloat(priceMin, 5, 0, Rc.TextFromFloat.PAD_PRE_NONE)
                    val maxText = createTextFromFloat(priceMax, 5, 0, Rc.TextFromFloat.PAD_PRE_NONE)
                    drawTextAnchored(minText, gRight, gBottom + density * 10f, 1, 0, 0)
                    drawTextAnchored(maxText, gRight, gTop - density * 4f, 1, 0, 0)
                }
            }
        }
    return rc.writer
}

// =====================================================================
// 9. Moon Phase Dial
//    Circular moon visualization with phase name and illumination
// =====================================================================
fun demoMoonPhaseDial(): RemoteComposeWriter {
    // illumination 0-1, phaseIndex 0-7
    val data = floatArrayOf(0.72f, 3f) // Waxing Gibbous
    val phaseNames =
        arrayOf(
            "New Moon",
            "Waxing Crescent",
            "First Quarter",
            "Waxing Gibbous",
            "Full Moon",
            "Waning Gibbous",
            "Last Quarter",
            "Waning Crescent",
        )

    val rc =
        RemoteComposeContextAndroid(
            platform = AndroidxRcPlatformServices(),
            apiLevel = 7,
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
            RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Moon Phase Dial"),
        ) {
            root {
                canvas(RecordingModifier().fillMaxSize().background(0xFF0B0D17L.toInt())) {
                    val w = ComponentWidth()
                    val h = ComponentHeight()
                    val cx = w / 2f
                    val cy = h * 0.42f
                    val density = rf(Rc.System.DENSITY)
                    val size = min(w, h)
                    val moonR = size * 0.3f

                    val values = rf(addFloatArray(data))
                    val illumination = values.get(rf(0f))
                    val phaseIdx = values.get(rf(1f))
                    val nameList = addStringList(*phaseNames)

                    // Outer glow ring
                    painter
                        .setColor(0x22FFFFFFL.toInt())
                        .setStyle(Paint.Style.STROKE)
                        .setStrokeWidth((density * 8f).toFloat())
                        .commit()
                    drawCircle(cx.toFloat(), cy.toFloat(), (moonR + density * 6f).toFloat())

                    // Full bright moon surface
                    painter.setColor(0xFFE8E8D0L.toInt()).setStyle(Paint.Style.FILL).commit()
                    drawCircle(cx.toFloat(), cy.toFloat(), moonR.toFloat())

                    // Subtle surface texture (dark spots)
                    painter.setColor(0x15000000L.toInt()).commit()
                    drawCircle(
                        (cx - moonR * 0.2f).toFloat(),
                        (cy - moonR * 0.15f).toFloat(),
                        (moonR * 0.15f).toFloat(),
                    )
                    drawCircle(
                        (cx + moonR * 0.25f).toFloat(),
                        (cy + moonR * 0.1f).toFloat(),
                        (moonR * 0.1f).toFloat(),
                    )
                    drawCircle(
                        (cx - moonR * 0.1f).toFloat(),
                        (cy + moonR * 0.3f).toFloat(),
                        (moonR * 0.12f).toFloat(),
                    )

                    // Shadow overlay: clip to moon bounds and draw dark region
                    // Shadow covers from the right side (for waxing) or left (waning)
                    // Terminator position based on illumination
                    save {
                        // Shadow fills the unilluminated portion
                        // For waxing (phaseIdx 0-3): shadow on left, light on right
                        // For waning (phaseIdx 4-7): shadow on right, light on left
                        // Simple approach: vertical terminator using clipRect
                        val shadowLeft = cx - moonR
                        val shadowRight = cx + moonR - illumination * moonR * 2f
                        clipRect(
                            shadowLeft.toFloat(),
                            (cy - moonR).toFloat(),
                            shadowRight.toFloat(),
                            (cy + moonR).toFloat(),
                        )
                        painter.setColor(0xDD0B0D17L.toInt()).commit()
                        drawCircle(cx.toFloat(), cy.toFloat(), moonR.toFloat())
                    }

                    // Moon outline
                    painter
                        .setColor(0x33FFFFFFL.toInt())
                        .setStyle(Paint.Style.STROKE)
                        .setStrokeWidth((density * 1f).toFloat())
                        .commit()
                    drawCircle(cx.toFloat(), cy.toFloat(), moonR.toFloat())

                    // Phase name
                    val phaseName = textLookup(nameList, phaseIdx.toFloat())
                    painter
                        .setColor(Color.WHITE)
                        .setStyle(Paint.Style.FILL)
                        .setTextSize((density * 18f).toFloat())
                        .setTypeface(0, 600, false)
                        .commit()
                    drawTextAnchored(phaseName, cx, h * 0.74f, 0, 0, 0)

                    // Illumination percentage
                    painter
                        .setColor(0xAAFFFFFFL.toInt())
                        .setTextSize((density * 14f).toFloat())
                        .setTypeface(0, 400, false)
                        .commit()
                    val illumText =
                        createTextFromFloat(
                            illumination * 100f,
                            3,
                            0,
                            Rc.TextFromFloat.PAD_PRE_NONE,
                        )
                    val illumLabel = textMerge(illumText, addText("% illuminated"))
                    drawTextAnchored(illumLabel, cx, h * 0.81f, 0, 0, 0)

                    // Decorative stars
                    painter
                        .setColor(0x66FFFFFFL.toInt())
                        .setTextSize((density * 8f).toFloat())
                        .commit()
                    val starPositions =
                        floatArrayOf(
                            0.1f,
                            0.15f,
                            0.85f,
                            0.2f,
                            0.15f,
                            0.7f,
                            0.9f,
                            0.65f,
                            0.75f,
                            0.1f,
                            0.3f,
                            0.9f,
                        )
                    for (s in 0 until starPositions.size / 2) {
                        drawTextAnchored(
                            "\u00B7",
                            w * starPositions[s * 2],
                            h * starPositions[s * 2 + 1],
                            0,
                            0,
                            0,
                        )
                    }
                }
            }
        }
    return rc.writer
}

// =====================================================================
// 10. Hydration Progress Wave
//     Animated wave/liquid fill showing water intake progress
// =====================================================================
fun demoHydrationWave(): RemoteComposeWriter {
    val data = floatArrayOf(5f, 8f) // cupsConsumed, goalCups

    val rc =
        RemoteComposeContextAndroid(
            platform = AndroidxRcPlatformServices(),
            apiLevel = 7,
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 450),
            RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Hydration Progress Wave"),
        ) {
            root {
                canvas(RecordingModifier().fillMaxSize().background(0xFF0F172AL.toInt())) {
                    val w = ComponentWidth()
                    val h = ComponentHeight()
                    val cx = w / 2f
                    val density = rf(Rc.System.DENSITY)

                    val values = rf(addFloatArray(data))
                    val cups = values.get(rf(0f))
                    val goal = values.get(rf(1f))
                    val progress = min(cups / goal, rf(1f))

                    // Container dimensions (glass shape)
                    val glassW = w * 0.45f
                    val glassH = h * 0.55f
                    val glassLeft = cx - glassW / 2f
                    val glassRight = cx + glassW / 2f
                    val glassTop = h * 0.18f
                    val glassBottom = glassTop + glassH
                    val cornerR = density * 12f

                    // Glass outline
                    painter
                        .setColor(0x44FFFFFFL.toInt())
                        .setStyle(Paint.Style.STROKE)
                        .setStrokeWidth((density * 2f).toFloat())
                        .commit()
                    drawRoundRect(glassLeft, glassTop, glassW, glassH, cornerR, cornerR)

                    // Wave fill - animated sinusoidal surface
                    val fillH = progress * glassH
                    val waveY = glassBottom - fillH
                    val waveAmp = density * 6f
                    val waveFreq = 3.14159f * 4f // 2 full waves across glass

                    // Build wave path: bottom-left → wave surface → bottom-right → close
                    val wavePath = pathCreate(glassLeft.toFloat(), glassBottom.toFloat())

                    // Wave surface from left to right
                    val waveSteps = 40f
                    loop(0, 1, waveSteps + 1f) { step ->
                        val t = (step / waveSteps).flush()
                        val x = (glassLeft + t * glassW).flush()
                        val y = waveY + sin(t * waveFreq + ContinuousSec() * 3f) * waveAmp
                        pathAppendLineTo(wavePath, x.toFloat(), y.toFloat())
                    }

                    // Close: right side down, across bottom, back up
                    pathAppendLineTo(wavePath, glassRight.toFloat(), glassBottom.toFloat())
                    pathAppendClose(wavePath)

                    // Fill with water gradient
                    painter
                        .setStyle(Paint.Style.FILL)
                        .setLinearGradient(
                            glassLeft.toFloat(),
                            waveY.toFloat(),
                            glassLeft.toFloat(),
                            glassBottom.toFloat(),
                            intArrayOf(0xFF38BDF8L.toInt(), 0xFF0284C7L.toInt()),
                            null,
                            Shader.TileMode.CLAMP,
                        )
                        .commit()

                    // Clip to glass interior
                    save {
                        clipRect(
                            (glassLeft + density * 2f).toFloat(),
                            (glassTop + density * 2f).toFloat(),
                            (glassRight - density * 2f).toFloat(),
                            (glassBottom - density * 2f).toFloat(),
                        )
                        drawPath(wavePath)
                    }
                    painter.setShader(0).commit()

                    // Cup count display
                    painter
                        .setColor(Color.WHITE)
                        .setStyle(Paint.Style.FILL)
                        .setTextSize((density * 42f).toFloat())
                        .setTypeface(0, 700, false)
                        .commit()
                    val cupsText = createTextFromFloat(cups, 1, 0, Rc.TextFromFloat.PAD_PRE_NONE)
                    drawTextAnchored(cupsText, cx, h * 0.83f, 0, 0, 0)

                    painter
                        .setColor(0xAAFFFFFFL.toInt())
                        .setTextSize((density * 16f).toFloat())
                        .setTypeface(0, 400, false)
                        .commit()
                    val goalText = createTextFromFloat(goal, 1, 0, Rc.TextFromFloat.PAD_PRE_NONE)
                    val label = textMerge(addText("of "), goalText, addText(" cups"))
                    drawTextAnchored(label, cx, h * 0.90f, 0, 0, 0)

                    // Progress percentage inside glass
                    painter
                        .setColor(0xCCFFFFFFL.toInt())
                        .setTextSize((density * 20f).toFloat())
                        .setTypeface(0, 700, false)
                        .commit()
                    val pctVal = progress * 100f
                    val pctText = createTextFromFloat(pctVal, 3, 0, Rc.TextFromFloat.PAD_PRE_NONE)
                    val pctLabel = textMerge(pctText, addText("%"))
                    drawTextAnchored(pctLabel, cx, glassTop + glassH * 0.4f, 0, 0, 0)

                    // Water drop icon
                    painter
                        .setColor(0xFF38BDF8L.toInt())
                        .setTextSize((density * 24f).toFloat())
                        .commit()
                    drawTextAnchored("\u2B29", cx, h * 0.10f, 0, 0, 0)
                    painter.setColor(Color.WHITE).setTextSize((density * 14f).toFloat()).commit()
                    drawTextAnchored("Hydration", cx, h * 0.10f, 0, 3f, 0)
                }
            }
        }
    return rc.writer
}

// =====================================================================
// LOW-LEVEL APIS THAT WOULD HAVE BEEN HELPFUL
// =====================================================================
//
// 1. drawRing(cx, cy, innerRadius, outerRadius, startAngle, sweepAngle)
//    Draws a filled annular sector (ring segment). Currently must use
//    thick-stroke drawArc which cannot have per-segment gradient colors
//    along the arc. Needed for: Activity Rings, Sleep Quality Rings.
//
// 2. drawGradientArc(bounds, startAngle, sweepAngle, startColor, endColor)
//    Arc with gradient color along its stroke path. Would enable smooth
//    color transitions in gauges (Battery Gauge: green→yellow→red along
//    the arc) without needing multiple arc segments.
//
// 3. colorLerp(color1: Int, color2: Int, fraction: RFloat): Short
//    Dynamic color interpolation returning a color ID. Currently
//    addColorExpression only supports HSV-based creation or static tween.
//    A runtime lerp between two RGB colors would simplify heatmaps and
//    temperature-based coloring.
//
// 4. addClipCircle(cx, cy, radius) or addClipOval(bounds)
//    Circular clip region. clipRect exists but clipCircle does not.
//    Moon Phase rendering requires clipping to a circular boundary;
//    clipRect produces a straight terminator instead of a curved one.
//    Workaround: addPolarPathExpression + addClipPath, but verbose.
//
// 5. drawCircle(cx: Number, cy: Number, radius: Number)
//    Number-typed overload for drawCircle. Currently only accepts Float,
//    requiring .toFloat() on every RFloat argument. drawArc, drawLine,
//    drawRect all accept Number but drawCircle does not. Same issue
//    with clipRect and translate.
//
// 6. setAlpha(value: RFloat) on Painter
//    Set paint alpha from a dynamic expression. Would enable pulsing
//    transparency effects and data-driven opacity without needing to
//    rebuild the full color via addColorExpression.
//
// 7. pathFromArc(cx, cy, radius, startAngle, sweepAngle): Int
//    Create a path from an arc segment. Would allow arc-shaped regions
//    to be used as clip paths or filled with gradients. Currently must
//    approximate arcs with many line segments via addPathExpression.
//
// 8. arrayGetColor(colorArrayId, index: RFloat): Short
//    Look up a color from a remote color array by dynamic index.
//    Would enable fully data-driven coloring in player-side loops
//    without needing addColorExpression per loop iteration.
//
// 9. sqrt(value: RFloat): RFloat
//    Kotlin wrapper for the SQRT expression opcode. The opcode exists
//    in the expression engine but there is no creation-API wrapper,
//    requiring manual expression construction or pow(x, 0.5f).
//
// 10. drawTextAnchored with rotation angle
//     Would allow rotated text labels along arcs, gauge faces, and
//     circular layouts without needing save/rotate/restore blocks.
//
