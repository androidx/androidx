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

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.RFloat
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.arrayLength
import androidx.compose.remote.creation.arraySum
import androidx.compose.remote.creation.cos
import androidx.compose.remote.creation.min
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.sin
import androidx.compose.remote.creation.times
import androidx.compose.remote.creation.toRad
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A demo that draws a Pie Chart using procedural creation APIs. */
@Suppress("RestrictedApiAndroidX")
fun demoPieChart(): RemoteComposeContextAndroid {
    val data = floatArrayOf(30f, 20f, 15f, 25f, 10f)
    val names = arrayOf("Android", "iOS", "Web", "Desktop", "Other")
    val colors =
        intArrayOf(
            0xFF4CAF50.toInt(), // Green
            0xFF2196F3.toInt(), // Blue
            0xFFFFC107.toInt(), // Amber
            0xFFE91E63.toInt(), // Pink
            0xFF9C27B0.toInt(), // Purple
        )

    val rc =
        RemoteComposeContextAndroid(
            platform = AndroidxRcPlatformServices(),
            apiLevel = 7,
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 500),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 500),
            RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Pie Chart Demo"),
        ) {
            root {
                box(
                    RecordingModifier().fillMaxSize().background(0xFFF0F0F0.toInt()),
                    BoxLayout.START,
                    BoxLayout.START,
                ) {
                    canvas(RecordingModifier().fillMaxSize()) {
                        val w = ComponentWidth()
                        val h = ComponentHeight()
                        val cx = w / 2f
                        val cy = h / 2f
                        val radius = min(w, h) * 0.4f

                        val total = data.sum()
                        var currentAngle = -90f // Start from top

                        for (i in data.indices) {
                            val sweepAngle = (data[i] / total) * 360f

                            // Draw slice
                            painter
                                .setColor(colors[i % colors.size])
                                .setStyle(Paint.Style.FILL)
                                .commit()

                            drawSector(
                                cx - radius,
                                cy - radius,
                                cx + radius,
                                cy + radius,
                                currentAngle,
                                sweepAngle,
                            )

                            // Draw border
                            painter
                                .setColor(Color.WHITE)
                                .setStyle(Paint.Style.STROKE)
                                .setStrokeWidth(2f)
                                .commit()

                            drawSector(
                                cx - radius,
                                cy - radius,
                                cx + radius,
                                cy + radius,
                                currentAngle,
                                sweepAngle,
                            )

                            // Draw Label
                            val labelAngle = (currentAngle + sweepAngle / 2f) * PI.toFloat() / 180f
                            val labelRadius = radius * 0.7f
                            val lx = cx + labelRadius * cos(labelAngle)
                            val ly = cy + labelRadius * sin(labelAngle)

                            painter
                                .setColor(Color.WHITE)
                                .setTextSize(24f)
                                .setStyle(Paint.Style.FILL)
                                .setTypeface(0, 700, false) // Bold
                                .commit()

                            val textId = addText(names[i])
                            drawTextAnchored(textId, lx, ly, 0.5f, 0.5f, 0)

                            currentAngle += sweepAngle
                        }

                        // Draw Legend
                        val legendX = 20f
                        var legendY = 20f
                        for (i in data.indices) {
                            painter
                                .setColor(colors[i % colors.size])
                                .setStyle(Paint.Style.FILL)
                                .commit()
                            drawRect(legendX, legendY, 20f, 20f)

                            painter.setColor(Color.BLACK).setTextSize(20f).commit()
                            val textId = addText(names[i] + " (" + data[i].toInt() + "%)")
                            drawTextAnchored(textId, legendX + 30f, legendY + 15f, 0f, 0.5f, 0)

                            legendY += 30f
                        }
                    }
                }
            }
        }
    return rc
}

/** A demo that draws a Pie Chart using procedural creation APIs. */
@Suppress("RestrictedApiAndroidX")
fun demoPieChart_good(): RemoteComposeContextAndroid {
    val data = floatArrayOf(30f, 20f, 15f, 25f, 10f)
    val names = arrayOf("Android", "iOS", "Web", "Desktop", "Other")
    val rc =
        RemoteComposeContextAndroid(
            platform = AndroidxRcPlatformServices(),
            apiLevel = 7,
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 500),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 500),
            RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Pie Chart Demo"),
        ) {
            root {
                box(
                    RecordingModifier().fillMaxSize().background(0xFFF0F0F0.toInt()),
                    BoxLayout.START,
                    BoxLayout.START,
                ) {
                    canvas(RecordingModifier().fillMaxSize()) {
                        val w = ComponentWidth()
                        val h = ComponentHeight()
                        val cx = w / 2f
                        val cy = h / 2f
                        val radius = min(w, h) * 0.4f

                        val values = rf(addFloatArray(data))
                        val names = addStringList(*names)
                        val len = arrayLength(values)
                        val total = arraySum(values)

                        save {
                            rotate(-90, cx, cy)
                            loop(0, 1, len) { i ->
                                val value = values.get(i)

                                val name = textLookup(names, i.toFloat())
                                val sweepAngle = (value / total) * 360f

                                val colorId =
                                    getDistributedColor(i, 0xff, 56.6 / 100f, 80 / 100f).toInt()

                                painter.setColorId(colorId).setStyle(Paint.Style.FILL).commit()

                                drawSector(
                                    cx - radius,
                                    cy - radius,
                                    cx + radius,
                                    cy + radius,
                                    0,
                                    sweepAngle,
                                )

                                // Draw border
                                painter
                                    .setColor(Color.WHITE)
                                    .setStyle(Paint.Style.STROKE)
                                    .setStrokeWidth(2f)
                                    .commit()

                                drawSector(
                                    cx - radius,
                                    cy - radius,
                                    cx + radius,
                                    cy + radius,
                                    0,
                                    sweepAngle,
                                )
                                rotate(sweepAngle, cx, cy)
                            }
                        }
                    }
                }
            }
        }
    return rc
}

/** A demo that draws a Pie Chart using procedural creation APIs. */
@Suppress("RestrictedApiAndroidX")
fun demoPieChart2(): RemoteComposeContextAndroid {
    val data = floatArrayOf(30f, 20f, 15f, 25f, 10f)
    val names = arrayOf("Android", "iOS", "Web", "Desktop", "Other")

    val rc =
        RemoteComposeContextAndroid(
            platform = AndroidxRcPlatformServices(),
            apiLevel = 7,
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 500),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 500),
            RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Pie Chart Demo"),
        ) {
            root {
                box(
                    RecordingModifier().fillMaxSize().background(0xFFF0F0F0.toInt()),
                    BoxLayout.START,
                    BoxLayout.START,
                ) {
                    canvas(RecordingModifier().fillMaxSize()) {
                        val w = ComponentWidth()
                        val h = ComponentHeight()
                        val density = rf(Rc.System.DENSITY)

                        val cx = w / 2f
                        val cy = h / 2f
                        val radius = min(w, h) * 0.4f

                        val values = rf(addFloatArray(data))
                        val names = addStringList(*names)
                        val len = arrayLength(values)
                        val total = arraySum(values)

                        // rotate(-90, cx, cy)
                        loop(0, 1, len) { i ->
                            val value = values.get(i)
                            val sum = arraySum(values, i - 1f).flush()
                            addDebugMessage("sum ", sum)
                            addDebugMessage("value ", value)
                            val name = textLookup(names, i.toFloat())
                            val sweepAngle = (value / total) * 360f
                            val sumAngle = (sum / total) * 360f

                            val colorId =
                                getDistributedColor(i, 0xff, 56.6 / 100f, 80 / 100f).toInt()

                            painter.setColorId(colorId).setStyle(Paint.Style.FILL).commit()

                            drawSector(
                                cx - radius,
                                cy - radius,
                                cx + radius,
                                cy + radius,
                                sumAngle,
                                sweepAngle,
                            )

                            // Draw border
                            painter
                                .setColor(Color.WHITE)
                                .setStyle(Paint.Style.STROKE)
                                .setStrokeWidth(2f)
                                .commit()

                            drawSector(
                                cx - radius,
                                cy - radius,
                                cx + radius,
                                cy + radius,
                                sumAngle,
                                sweepAngle,
                            )
                            painter
                                .setTextSize((16f * density).toFloat())
                                .setStyle(Paint.Style.FILL)
                                .commit()
                            val angleRad = toRad((sumAngle + sweepAngle * 0.5f))
                            val pieCenterX = cx + radius * cos(angleRad) * 0.66f
                            val pieCenterY = cy + radius * sin(angleRad) * 0.66f
                            drawTextAnchored(name, pieCenterX, pieCenterY, 0, 0, 0)
                        }
                    }
                }
            }
        }
    return rc
}

@SuppressLint("RestrictedApiAndroidX")
fun RemoteComposeContextAndroid.getDistributedColor(
    i: RFloat,
    alpha: Int,
    sat: Number,
    value: Number,
): Short {
    // The Golden Ratio Conjugate: (1 + sqrt(5)) / 2 - 1 ≈ 0.61803398875
    // This represents a ~137.5 degree turn on the color wheel.
    val golden_ratio_conjugate = 0.618033988749895f
    val hue = (i * golden_ratio_conjugate) % 1.0f
    return addColorExpression(alpha, hue.toFloat(), sat.toFloat(), value.toFloat())
}
