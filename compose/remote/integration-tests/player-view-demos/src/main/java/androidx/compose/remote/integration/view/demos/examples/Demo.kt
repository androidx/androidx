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

import android.graphics.Paint
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.TouchExpression
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.paint.PaintPathEffects
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.minus
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.plus
import androidx.compose.remote.creation.sign
import androidx.compose.remote.creation.sin
import androidx.compose.remote.creation.times
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Suppress("RestrictedApiAndroidX")
fun plot1(): RemoteComposeWriter {
    val rc =
        RemoteComposeContextAndroid(
            width = 500,
            height = 500,
            contentDescription = "Simple Timer",
            apiLevel = 7,
            profiles = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            platform = AndroidxRcPlatformServices(),
        ) {
            val backgroundId =
                writer.addThemedColor(
                    "color.system_accent2_10",
                    0xFFDDDDDD.toInt(),
                    "color.system_accent2_900",
                    0xFF222222.toInt(),
                )
            val titleId =
                writer.addThemedColor(
                    "color.system_neutral1_900",
                    0xFF111111.toInt(),
                    "color.system_neutral1_50",
                    0xFF111111.toInt(),
                )
            val axisId =
                writer.addThemedColor(
                    "color.system_neutral2_200",
                    0xFF999999.toInt(),
                    "color.system_neutral2_700",
                    0xFF999999.toInt(),
                )
            val curveId =
                writer.addThemedColor(
                    "color.system_accent1_100",
                    0xFF994422.toInt(),
                    "color.system_accent1_500",
                    0xFF994422.toInt(),
                )

            root {
                box(RecordingModifier().fillMaxSize(), BoxLayout.START, BoxLayout.START) {
                    canvas(RecordingModifier().fillMaxSize().background(0xFF99AAFF.toInt())) {
                        painter.setColorId(backgroundId.toInt()).setTextSize(64f).commit()
                        drawRect(
                            20f,
                            20f,
                            (ComponentWidth() - 20f).toFloat(),
                            (ComponentHeight() - 20f).toFloat(),
                        )

                        val minX = -10f
                        val maxX = 10f
                        val scale = (((Seconds() / 2f) % 2f) + 1f).anim(0.5f)
                        val scaleY = scale * (ComponentHeight() - 100f) / -10f
                        val offsetY = ComponentHeight() / 2f
                        val scaleX = (ComponentWidth() - 100f) / (maxX - minX)
                        val offsetX = 50f - minX * scaleX
                        val id = scale.genTextId(1, 1)
                        drawTextAnchored(id, ComponentWidth() / 2f, 100f, 0, 0, 0)
                        painter.setStrokeWidth(10f).setStyle(Paint.Style.STROKE).commit()
                        val equ = rFun { x -> sin(x + ContinuousSec() * 3f) }

                        painter.setColorId(curveId.toInt()).commit()

                        val pathId =
                            addPathExpression(
                                rFun { x -> x * scaleX + offsetX },
                                equ * scaleY + offsetY,
                                minX,
                                maxX,
                                64,
                                Rc.PathExpression.SPLINE_PATH,
                            )
                        drawPath(pathId)

                        val touchX =
                            touchExpression(
                                RemoteContext.FLOAT_TOUCH_POS_X,
                                scaleX.toFloat(),
                                Rc.FloatExpression.DIV,
                                defValue = (minX + maxX) / 2,
                                min = minX,
                                max = maxX,
                                touchMode = TouchExpression.STOP_INSTANTLY,
                            )
                        val sPos = touchX * scaleX + offsetX
                        painter
                            .setColorId(axisId.toInt())
                            .setStrokeWidth(3f)
                            .setPathEffect(PaintPathEffects.dash(0f, 10f, 10f))
                            .commit()

                        drawLine(sPos, 0, sPos, ComponentHeight())

                        val value = sin(touchX + ContinuousSec() * 3f).genTextId(0, 2)
                        val mx = (touchX * scaleX + offsetX).flush()
                        val my = sin(touchX + ContinuousSec() * 3f) * scaleY + offsetY
                        val deltaX = (100f * sign(mx - ComponentHeight() / 2f)).anim(0.5f)
                        val cx = mx - deltaX
                        val cy = (my + ComponentHeight()) / 2f
                        painter
                            .setColorId(axisId.toInt())
                            .setStyle(Paint.Style.FILL)
                            .setPathEffect(null)
                            .commit()

                        drawLine(mx, my, cx, cy)
                        val width = textAttribute(value, Rc.TextAttribute.MEASURE_WIDTH)
                        drawRoundRect(cx - 64 * 2, cy - 32f, cx + 64 * 2, cy + 32f, 10f, 10)
                        painter.setColorId(titleId.toInt()).setPathEffect(null).commit()

                        drawTextAnchored(value, cx, cy, 0, 0, 0)

                        // todo support this type of api
                        //                        box(RecordingModifier().fillMaxSize()) {
                        //
                        // box(RecordingModifier().background(0x8800FF00.toInt()).padding(8)
                        //                                .computePosition {
                        //                                    x = cx - (width as RFloat / 2f)
                        //                                    y = cy - (height as RFloat / 2f)
                        //                                }
                        //                            ) {
                        //                                text(value)
                        //                            }
                        //                        }
                    }
                }
            }
        }
    return rc.writer
}

@Suppress("RestrictedApiAndroidX")
fun plot2(): RemoteComposeWriter {
    val rc =
        RemoteComposeContextAndroid(
            width = 500,
            height = 500,
            contentDescription = "Simple Timer",
            apiLevel = 7,
            profiles = RcProfiles.PROFILE_ANDROIDX,
            platform = AndroidxRcPlatformServices(),
        ) {
            root {
                box(RecordingModifier().fillMaxSize(), BoxLayout.START, BoxLayout.START) {
                    canvas(RecordingModifier().fillMaxSize().background(0xFF127799.toInt())) {
                        painter.setColor(0xFFFF9966.toInt()).setTextSize(64f).commit()
                        val minX = -10f
                        val maxX = 10f

                        val scaleY = (ComponentHeight() - 100f) / -10f
                        val offsetY = ComponentHeight() / 2f
                        val scaleX = (ComponentWidth() - 100f) / (maxX - minX)
                        val offsetX = 50f - minX * scaleX

                        painter.setStrokeWidth(10f).setStyle(Paint.Style.STROKE).commit()
                        val equ = rFun { x -> sin(x) }
                        val pathId =
                            addPathExpression(
                                rFun { x -> x * scaleX + offsetX },
                                equ * scaleY + offsetY,
                                minX,
                                maxX,
                                64,
                                Rc.PathExpression.SPLINE_PATH,
                            )
                        drawPath(pathId)
                        val touchX =
                            addTouch(
                                (minX + maxX) / 2,
                                min = minX,
                                max = maxX,
                                TouchExpression.STOP_INSTANTLY,
                                0f,
                                0,
                                null,
                                null,
                                RemoteContext.FLOAT_TOUCH_POS_X,
                                scaleX.toFloat(),
                                Rc.FloatExpression.DIV,
                            )
                        val sPos = touchX * scaleX + offsetX
                        drawLine(sPos, 0, sPos, ComponentHeight())
                    }
                }
            }
        }
    return rc.writer
}

@Suppress("RestrictedApiAndroidX")
fun plot3(): RemoteComposeWriter {
    val rc =
        RemoteComposeContextAndroid(
            width = 500,
            height = 500,
            contentDescription = "Simple Timer",
            apiLevel = 7,
            profiles = RcProfiles.PROFILE_ANDROIDX,
            platform = AndroidxRcPlatformServices(),
        ) {
            root {
                box(RecordingModifier().fillMaxSize(), BoxLayout.START, BoxLayout.START) {
                    canvas(RecordingModifier().fillMaxSize().background(0xFF127799.toInt())) {
                        painter.setColor(0xFFFF9966.toInt()).setTextSize(64f).commit()
                        val minX = -10f
                        val maxX = 10f
                        val scale = (((Seconds() / 2f) % 2f) + 1f).anim(0.5f)
                        val scaleY = scale * (ComponentHeight() - 100f) / -10f
                        val offsetY = ComponentHeight() / 2f
                        val scaleX = (ComponentWidth() - 100f) / (maxX - minX)
                        val offsetX = 50f - minX * scaleX
                        val id = scale.genTextId(1, 1)
                        drawTextAnchored(id, ComponentWidth() / 2f, 100f, 0, 0, 0)
                        painter.setStrokeWidth(10f).setStyle(Paint.Style.STROKE).commit()
                        val equ = rFun { x -> sin(x + ContinuousSec() * 3f) }
                        val pathId =
                            addPathExpression(
                                rFun { x -> x * scaleX + offsetX },
                                equ * scaleY + offsetY,
                                minX,
                                maxX,
                                64,
                                Rc.PathExpression.SPLINE_PATH,
                            )
                        drawPath(pathId)
                    }
                }
            }
        }
    return rc.writer
}

@Suppress("RestrictedApiAndroidX")
fun plot4(): RemoteComposeWriter {
    val rc =
        RemoteComposeContextAndroid(
            width = 500,
            height = 500,
            contentDescription = "Simple Timer",
            apiLevel = 7,
            profiles = RcProfiles.PROFILE_ANDROIDX,
            platform = AndroidxRcPlatformServices(),
        ) {
            root {
                box(RecordingModifier().fillMaxSize(), BoxLayout.START, BoxLayout.START) {
                    canvas(RecordingModifier().fillMaxSize().background(0xFF127799.toInt())) {
                        painter.setColor(0xFFFF9966.toInt()).setTextSize(64f).commit()
                        val minX = 0
                        val maxX = Math.PI.toFloat() * 2f
                        val scale =
                            (((Seconds() / 2f) % 2f) + 1f).anim(0.5f, Rc.Animate.CUBIC_DECELERATE)
                        val id = scale.genTextId(1, 1)
                        drawTextAnchored(id, ComponentWidth() / 2f, 100f, 0, 0, 0)

                        val equ = rFun { x -> 100f + 10f * sin(x * 10f + ContinuousSec() * 3f) }
                        val pathId =
                            addPolarPathExpression(
                                equ * scale,
                                minX,
                                maxX,
                                64,
                                centerX = ComponentWidth() / 2f,
                                centerY = ComponentHeight() / 2f,
                                Rc.PathExpression.SPLINE_PATH,
                            )
                        drawPath(pathId)
                    }
                }
            }
        }
    return rc.writer
}

// Failing to display due to b/450104887
@Preview @Composable fun Plot1Preview() = RemoteDocPreview(plot1())

@Preview @Composable fun Plot2Preview() = RemoteDocPreview(plot2())

@Preview @Composable fun Plot3Preview() = RemoteDocPreview(plot3())

@Preview @Composable fun Plot4Preview() = RemoteDocPreview(plot4())
