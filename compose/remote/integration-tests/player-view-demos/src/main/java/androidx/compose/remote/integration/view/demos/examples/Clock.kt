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

import androidx.compose.remote.core.RemoteContext.FLOAT_CONTINUOUS_SEC
import androidx.compose.remote.core.RemoteContext.FLOAT_OFFSET_TO_UTC
import androidx.compose.remote.core.RemoteContext.FLOAT_TIME_IN_HR
import androidx.compose.remote.core.RemoteContext.FLOAT_TIME_IN_MIN
import androidx.compose.remote.core.RemoteContext.FLOAT_TIME_IN_SEC
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxHeight
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.tooling.preview.RemotePreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview

const val androidShapeString =
    "M17.6,9.48" +
        "L19.44,6.3" +
        "C19.6,5.99,19.48,5.61,19.18,5.45" +
        "C18.89,5.30,18.53,5.395,18.35,5.67" +
        "L16.47,8.91" +
        "C13.61,7.7,10.39,7.7,7.53,8.91" +
        "L5.65,5.67" +
        "C5.46,5.38,5.07,5.29,4.78,5.47" +
        "C4.5,5.65,4.41,6.01,4.56,6.3" +
        "L6.4,9.48" +
        "C3.3,11.25,1.28,14.44,1,18" +
        "H23" +
        "C22.72,14.44,20.7,11.25,17.6,9.48Z" +
        "M7,15.25" +
        "C6.31,15.25,5.75,14.69,5.75,14" +
        "C5.75,13.31,6.31,12.75,7,12.75" +
        "S8.25,13.31,8.25,14" +
        "C8.25,14.69,7.69,15.25,7,15.25Z" +
        "M17,15.25" +
        "C16.31,15.25,15.75,14.69,15.75,14" +
        "C15.75,13.31,16.31,12.75,17,12.75" +
        "S18.25,13.31,18.25,14" +
        "C18.25,14.69,17.69,15.25,17,15.25Z"

@Suppress("RestrictedApiAndroidX") val androidPath = RemotePath(androidShapeString)

fun Color.paint(
    style: PaintingStyle? = null,
    strokeWidth: Float? = null,
    textSize: Float? = null,
    cap: StrokeCap? = null,
): RemotePaint = RemotePaint {
    color = this@paint.rc
    if (style != null) {
        this.style = style
    }
    if (strokeWidth != null) {
        this.strokeWidth = strokeWidth.rf
    }
    if (textSize != null) {
        this.textSize = textSize.rf
    }
    if (cap != null) {
        this.strokeCap = cap
    }
}

@Composable
@RemoteComposable
@Suppress("RestrictedApiAndroidX")
fun RcSimpleClock1(
    timeHr: RemoteFloat = RemoteFloat(FLOAT_TIME_IN_HR),
    timeMin: RemoteFloat = RemoteFloat(FLOAT_TIME_IN_MIN),
    timeUtcOffset: RemoteFloat = RemoteFloat(FLOAT_OFFSET_TO_UTC),
    timeContinuousSec: RemoteFloat = RemoteFloat(FLOAT_CONTINUOUS_SEC),
    timeSeconds: RemoteFloat = RemoteFloat(FLOAT_TIME_IN_SEC),
) {
    var duration: Float = 1.0f
    var topBezel = "android.colorAccent"
    var bottomBezel = "android.colorControlNormal"
    var textColor = "android.textColor"
    var backgroundColor = "android.colorPrimary"
    var gmtHandColor = "android.colorError"
    var tickColor = "android.colorButtonNormal"
    // TODO implement Color theme
    var days = arrayOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

    RemoteRow(
        modifier = RemoteModifier.fillMaxSize(),
        verticalAlignment = RemoteAlignment.CenterVertically,
    ) {
        RemoteCanvas(modifier = RemoteModifier.fillMaxWidth().fillMaxHeight()) {
            val w = remote.component.width
            val h = remote.component.height
            val centerX = remote.component.centerX
            val centerY = remote.component.centerY
            val rad = centerX.min(centerY)
            val top = centerY - rad
            val bezel_thick: Float = 100f
            val hourHandLength = rad * 0.4f
            val minHandLength = rad * 0.7f
            val bezel1 = Color(0xFF1A1A5E)
            val bezel2 = Color(0xFF5E1A1A)

            val textColor = Color(0xFFAAAAAA)
            val backgroundColor = Color(0xFF323288)
            val tickColor = Color(0xFF7A7B7B)
            val bezelMarkColor = tickColor
            val hourHandColor = textColor
            val minHandColor = textColor
            val ticksColor = textColor

            val hr = timeHr
            val min = timeMin
            val utcOff = timeUtcOffset

            val secondAngle = (timeContinuousSec % 60f) * 6f
            val minAngle = min * 6f
            val hrAngle = hr * 30f
            val faceTop = centerY - rad
            val gmtAngle = ((hr - utcOff / 3600f) + ((min % 60f) / 60f)) * 15f
            val handWidth = 20f
            drawCircle(bezel1.paint(), RemoteOffset(centerX, centerY), rad)

            drawCircle(bezel1.paint(), RemoteOffset(centerX, centerY), rad)

            clipRect(0f.rf, centerY, w, h) {
                drawCircle(bezel2.paint(), RemoteOffset(centerX, centerY), rad)
            }
            drawCircle(Color.Black.paint(), RemoteOffset(centerX, centerY), rad)
            drawCircle(Color.DarkGray.paint(), RemoteOffset(centerX, centerY), rad - bezel_thick)

            for (i in 0 until 60) {
                rotate((i * 6f).rf, RemoteOffset(centerX, centerY)) {
                    drawLine(
                        ticksColor.paint(strokeWidth = 2f),
                        RemoteOffset(centerX, top + (bezel_thick + 20f)),
                        RemoteOffset(centerX, top + bezel_thick),
                    )
                }
            }

            // hour hand
            withTransform({
                translate((centerX - 64f), ((centerY + top) / 2f))
                scale(5f.rf, 5f.rf)
            }) {
                drawPath(androidPath, Color(0xFFA4C639).paint())
            }

            val shift = faceTop + bezel_thick + 20f
            // bezel circles
            for (i in 0 until 12) {
                rotate(15f.rf + 30f * i, RemoteOffset(centerX, centerY)) {
                    drawCircle(
                        minHandColor.paint(),
                        RemoteOffset(centerX, top + bezel_thick / 2),
                        8f.rf,
                    )
                }
            }
            // bezel text
            for (i in 0 until 12) {
                if (i != 0) {
                    rotate(30f.rf * i.rf, RemoteOffset(centerX, centerY)) {
                        //  drawText(textMeasurer.measure("" + (i*2)), color=Color.White,
                        // Offset(centerX, top +
                        // bezel_thick / 2),)
                        drawAnchoredText(
                            ("" + (i * 2)).rs,
                            centerX,
                            top + bezel_thick / 2,
                            panX = 0f.rf,
                            panY = 0f.rf,
                            paint = Color.White.paint(textSize = 50f),
                        )
                    }
                }
            }
            // ============ Draw Markers at various points =============
            for (i in 0 until 12) {
                if ((i + 1) % 3 != 0) {
                    rotate(30f.rf * i.rf + 30.rf, RemoteOffset(centerX, centerY)) {
                        drawCircle(
                            minHandColor.paint(),
                            RemoteOffset(centerX, top + (bezel_thick + 20f) + 20f),
                            20f.rf,
                        )
                    }
                } else {

                    if (i == 5) {
                        rotate(30f.rf * i.rf + 30.rf, RemoteOffset(centerX, centerY)) {
                            drawRect(
                                minHandColor.paint(),
                                topLeft = RemoteOffset(centerX - 10f, shift),
                                size = RemoteSize(20f.rf, 40f.rf),
                            )
                        }
                    } else if (i == 8) {
                        rotate(30f.rf * i.rf + 30.rf, RemoteOffset(centerX, centerY)) {
                            drawRoundRect(
                                minHandColor.paint(),
                                topLeft = RemoteOffset(centerX - 10f, shift),
                                size = RemoteSize(20f.rf, 40f.rf),
                                RemoteOffset(10f, 10f),
                            )
                        }
                        // drawRect(rect1, 130f, rect2, 180f)
                    } else if (i == 11) {
                        rotate(30f.rf * i.rf + 30.rf, RemoteOffset(centerX, centerY)) {
                            val path = RemotePath()

                            path.moveTo(40f, 0f)
                            path.lineTo(-40f, 0f)
                            path.lineTo(0f, 40f)
                            path.close()

                            translate((centerX), (faceTop + bezel_thick / 2f - 20f)) {
                                drawPath(path = path, minHandColor.paint())
                            }
                        }
                    }
                }
            }

            // =========== DATE Complication ===========
            val dateLeft = centerX + rad - bezel_thick - 115f
            val dateTop = centerY - 30f
            val dateRight = remote.component.width - 140f
            val dateBottom = centerY + 30f
            val cx = dateLeft + 40f
            drawRect(
                bezelMarkColor.paint(),
                RemoteOffset(dateLeft, dateTop),
                RemoteSize(80f.rf, 60f.rf),
            )
            drawAnchoredText(
                "32".rs,
                cx,
                centerY,
                0f.rf,
                0f.rf,
                paint = Color.Black.paint(textSize = 40f),
            )
            // =============== DAY Complication ===============
            val dayCenterX = centerX + rad - 280f
            val dayLeft = dayCenterX - 46f
            val dayRight = dayCenterX + 46f

            clipRect(dayLeft, dateTop, dayRight, dateBottom) {
                drawCircle(
                    Color.LightGray.paint(),
                    RemoteOffset(centerX, centerY),
                    dateLeft - centerX,
                )
                for (i in 0 until 7) {
                    val anim = remote.animateFloat((timeSeconds + i.toFloat()) * 360f / 7f, 0.2f)
                    rotate(anim, RemoteOffset(centerX, centerY)) {
                        drawAnchoredText(
                            days[6 - i].rs,
                            dayCenterX,
                            centerY,
                            0f.rf,
                            0f.rf,
                            paint = Color.Black.paint(textSize = 40f),
                        )
                    }
                }
            }
            // ===================== hour hand =========================
            rotate(hrAngle, RemoteOffset(centerX, centerY)) {
                drawLine(
                    hourHandColor.paint(strokeWidth = handWidth, cap = StrokeCap.Round),
                    RemoteOffset(centerX, centerY - hourHandLength),
                    RemoteOffset(centerX, centerY),
                )
            }

            rotate(minAngle, RemoteOffset(centerX, centerY)) {
                drawLine(
                    minHandColor.paint(strokeWidth = handWidth, cap = StrokeCap.Round),
                    RemoteOffset(centerX, centerY - minHandLength),
                    RemoteOffset(centerX, centerY),
                )
            }

            val edge = 12f
            val gmtColor = Color(0xFFFF0000)

            val gmtPath = RemotePath()
            gmtPath.moveTo(1f, 1f)
            gmtPath.moveTo((centerX - 20f).floatId, (top + (bezel_thick + 60f)).floatId)
            gmtPath.lineTo((centerX + 20f).floatId, (top + (bezel_thick + 60f)).floatId)
            gmtPath.lineTo(centerX.floatId, (top + (bezel_thick + 30f)).floatId)
            gmtPath.close()

            rotate(gmtAngle, RemoteOffset(centerX, centerY)) {
                drawLine(
                    gmtColor.paint(strokeWidth = 3f),
                    RemoteOffset(centerX, centerY),
                    RemoteOffset(centerX, top + (bezel_thick + 60f)),
                )
                drawPath(
                    gmtPath,
                    RemotePaint {
                        color = gmtColor.rc
                        style = PaintingStyle.Fill
                    },
                )
            }

            rotate(secondAngle, RemoteOffset(centerX, centerY)) {
                drawLine(
                    minHandColor.paint(strokeWidth = 4f, cap = StrokeCap.Round),
                    RemoteOffset(centerX, centerY - minHandLength),
                    RemoteOffset(centerX, centerY),
                )
                drawCircle(
                    minHandColor.paint(),
                    RemoteOffset(centerX, centerY - minHandLength * 0.7f),
                    handWidth.rf,
                )
            }

            drawCircle(minHandColor.paint(), RemoteOffset(centerX, centerY), handWidth.rf)
            drawCircle(Color.Black.paint(), RemoteOffset(centerX, centerY), 10f.rf)
        }
    }
}

@Preview @Composable private fun RcSimpleClock1Preview() = RemotePreview { RcSimpleClock1() }
