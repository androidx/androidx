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

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.DrawTextAnchored
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.RemoteComposeWriterInterface
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

//    var platform = AndroidxRcPlatformServices()

@Suppress("RestrictedApiAndroidX")
fun demoTouch1(): RemoteComposeWriter {
    val rcDoc = RemoteComposeWriterAndroid(300, 300, "Clock", platform)
    rcDoc.root(
        RemoteComposeWriterInterface {
            rcDoc.startBox(
                RecordingModifier().fillMaxWidth().fillMaxHeight(),
                BoxLayout.START,
                BoxLayout.START,
            )
            rcDoc.startCanvas(RecordingModifier().fillMaxSize())
            // float tmp = rcDoc.floatExpression(FLOAT_CONTINUOUS_SEC);
            rcDoc.getPainter().setColor(Color.BLUE).commit()
            val w = rcDoc.addComponentWidthValue()
            val h = rcDoc.addComponentHeightValue()
            rcDoc.drawRect(0f, 0f, w, h)
            rcDoc.getPainter().setColor(Color.RED).commit()
            val cx = rcDoc.floatExpression(w, 0.5f, AnimatedFloatExpression.MUL)
            val cy = rcDoc.floatExpression(h, 0.5f, AnimatedFloatExpression.MUL)

            val top = rcDoc.floatExpression(cy, 10f, AnimatedFloatExpression.SUB)
            val bottom = rcDoc.floatExpression(cy, 10f, AnimatedFloatExpression.ADD)
            val left = 20f
            val right = rcDoc.floatExpression(w, 20f, AnimatedFloatExpression.SUB)
            val sliderSize = rcDoc.floatExpression(bottom, top, AnimatedFloatExpression.SUB)
            val pos =
                rcDoc.addTouch(
                    cx,
                    left,
                    right,
                    RemoteComposeWriter.STOP_INSTANTLY.toInt(),
                    0f,
                    0,
                    null,
                    null,
                    RemoteContext.FLOAT_TOUCH_POS_X,
                    1f,
                    AnimatedFloatExpression.MUL,
                )
            rcDoc.addDebugMessage(">>>>> [" + Utils.idFromNan(pos) + "]", pos)
            val leftSlider = rcDoc.floatExpression(pos, 20f, AnimatedFloatExpression.SUB)
            val rightSlider = rcDoc.floatExpression(pos, 20f, AnimatedFloatExpression.ADD)
            val topSlider = rcDoc.floatExpression(top, 20f, AnimatedFloatExpression.SUB)
            val bottomSlider = rcDoc.floatExpression(bottom, 20f, AnimatedFloatExpression.ADD)
            val idW = rcDoc.createTextFromFloat(w, 3, 2, 0)
            val idH = rcDoc.createTextFromFloat(h, 3, 2, 0)
            val space = rcDoc.textCreateId(" . ")
            val id = rcDoc.textMerge(idW, rcDoc.textMerge(space, idH))
            rcDoc.getPainter().setColor(Color.BLACK).setTextSize(64f).commit()

            rcDoc.getPainter().setColor(Color.GRAY).commit()
            rcDoc.drawRoundRect(left, top, right, bottom, 20f, 20f)
            rcDoc.getPainter().setColor(Color.RED).commit()
            rcDoc.drawRoundRect(leftSlider, topSlider, rightSlider, bottomSlider, 40f, 40f)
            val value =
                rcDoc.floatExpression(
                    pos,
                    20f,
                    AnimatedFloatExpression.SUB,
                    h,
                    40f,
                    AnimatedFloatExpression.SUB,
                    AnimatedFloatExpression.DIV,
                )
            val valueStr = rcDoc.createTextFromFloat(value, 1, 2, TextFromFloat.PAD_AFTER_ZERO)
            rcDoc.getPainter().setColor(Color.WHITE).commit()
            rcDoc.drawTextAnchored(
                valueStr,
                pos,
                cy,
                0f,
                -2f,
                DrawTextAnchored.ANCHOR_MONOSPACE_MEASURE,
            )

            rcDoc.endCanvas()
            rcDoc.endBox()
        }
    )
    return rcDoc
}

@Preview @Composable fun DemoTouch1Preview() = RemoteDocPreview(demoTouch1())

@Suppress("RestrictedApiAndroidX")
fun demoTouch2(): RemoteComposeWriter {
    val rcDoc = RemoteComposeWriterAndroid(300, 300, "Clock", platform)
    rcDoc.root(
        RemoteComposeWriterInterface {
            rcDoc.startBox(
                RecordingModifier().fillMaxWidth().fillMaxHeight(),
                BoxLayout.START,
                BoxLayout.START,
            )
            rcDoc.startCanvas(RecordingModifier().fillMaxSize())

            //            float anim = rcDoc.floatExpression(FLOAT_CONTINUOUS_SEC, 2, MOD,
            // 1, SUB);
            rcDoc.getPainter().setColor(Color.BLUE).commit()
            val w = rcDoc.addComponentWidthValue()
            val h = rcDoc.addComponentHeightValue()
            rcDoc.drawRect(0f, 0f, w, h)
            rcDoc.getPainter().setColor(Color.RED).commit()
            val cx = rcDoc.floatExpression(w, 0.5f, AnimatedFloatExpression.MUL)
            val cy = rcDoc.floatExpression(h, 0.5f, AnimatedFloatExpression.MUL)

            val left = rcDoc.floatExpression(cx, 10f, AnimatedFloatExpression.SUB)
            val right = rcDoc.floatExpression(cx, 10f, AnimatedFloatExpression.ADD)

            val top = 20f
            val bottom = rcDoc.floatExpression(h, 20f, AnimatedFloatExpression.SUB)
            val pos =
                rcDoc.addTouch(
                    cy,
                    top,
                    bottom,
                    RemoteComposeWriter.STOP_GENTLY.toInt(),
                    0f,
                    4,
                    null,
                    null,
                    RemoteContext.FLOAT_TOUCH_POS_Y,
                )
            val leftSlider = rcDoc.floatExpression(left, 20f, AnimatedFloatExpression.SUB)
            val rightSlider = rcDoc.floatExpression(right, 20f, AnimatedFloatExpression.ADD)
            val topSlider = rcDoc.floatExpression(pos, 20f, AnimatedFloatExpression.SUB)
            val bottomSlider = rcDoc.floatExpression(pos, 20f, AnimatedFloatExpression.ADD)

            val idW = rcDoc.createTextFromFloat(w, 3, 2, 0)
            val idH = rcDoc.createTextFromFloat(h, 3, 2, 0)
            val space = rcDoc.textCreateId(" . ")
            val id = rcDoc.textMerge(idW, rcDoc.textMerge(space, idH))
            rcDoc.getPainter().setColor(Color.BLACK).setTextSize(64f).commit()

            rcDoc.drawTextAnchored(id, cx, 0f, 0f, 1f, 0)
            rcDoc.getPainter().setColor(Color.GRAY).commit()
            rcDoc.drawRoundRect(left, top, right, bottom, 20f, 20f)
            rcDoc.getPainter().setColor(Color.RED).commit()
            rcDoc.drawRoundRect(leftSlider, topSlider, rightSlider, bottomSlider, 40f, 40f)
            val value =
                rcDoc.floatExpression(
                    pos,
                    20f,
                    AnimatedFloatExpression.SUB,
                    h,
                    40f,
                    AnimatedFloatExpression.SUB,
                    AnimatedFloatExpression.DIV,
                )
            val valueStr = rcDoc.createTextFromFloat(value, 1, 2, TextFromFloat.PAD_AFTER_ZERO)
            rcDoc.getPainter().setColor(Color.WHITE).commit()
            rcDoc.drawTextAnchored(
                valueStr,
                cx,
                pos,
                2f,
                0f,
                DrawTextAnchored.ANCHOR_MONOSPACE_MEASURE,
            )
            rcDoc.endCanvas()
            rcDoc.endBox()
        }
    )
    return rcDoc
}

@Preview @Composable fun DemoTouch2Preview() = RemoteDocPreview(demoTouch2())

@Suppress("RestrictedApiAndroidX")
fun demoTouch3(mode: Int, spec: FloatArray?, title: String?): RemoteComposeWriter {
    val rcDoc = RemoteComposeWriterAndroid(300, 300, "Clock", platform)
    rcDoc.root(
        RemoteComposeWriterInterface {
            rcDoc.startBox(
                RecordingModifier().fillMaxWidth().fillMaxHeight(),
                BoxLayout.START,
                BoxLayout.START,
            )
            rcDoc.startCanvas(RecordingModifier().fillMaxSize())
            val anim =
                rcDoc.floatExpression(
                    RemoteContext.FLOAT_CONTINUOUS_SEC,
                    2f,
                    AnimatedFloatExpression.MOD,
                    1f,
                    AnimatedFloatExpression.SUB,
                )

            rcDoc.getPainter().setColor(Color.BLUE).commit()
            val w = rcDoc.addComponentWidthValue()
            val h = rcDoc.addComponentHeightValue()
            rcDoc.drawRoundRect(0f, 0f, w, h, 60f, 60f)

            rcDoc.getPainter().setColor(Color.RED).commit()
            val cx = rcDoc.floatExpression(w, 0.5f, AnimatedFloatExpression.MUL)
            val cy = rcDoc.floatExpression(h, 0.5f, AnimatedFloatExpression.MUL)

            rcDoc.save()
            rcDoc.scale(anim, 1f, cx, cy)
            // rcDoc.drawOval(0, 0, RemoteContext.FLOAT_WINDOW_WIDTH,
            // RemoteContext.FLOAT_WINDOW_HEIGHT);
            rcDoc.getPainter().setColor(Color.GRAY).setTextSize(64f).commit()
            rcDoc.restore()

            // track
            val rad =
                rcDoc.floatExpression(
                    w,
                    h,
                    AnimatedFloatExpression.MIN,
                    2f,
                    AnimatedFloatExpression.DIV,
                    30f,
                    AnimatedFloatExpression.SUB,
                )
            val left = rcDoc.floatExpression(cx, rad, AnimatedFloatExpression.SUB)
            val right = rcDoc.floatExpression(cx, rad, AnimatedFloatExpression.ADD)
            val top = rcDoc.floatExpression(cy, rad, AnimatedFloatExpression.SUB)
            val bottom = rcDoc.floatExpression(cy, rad, AnimatedFloatExpression.ADD)
            rcDoc
                .getPainter()
                .setStyle(Paint.Style.STROKE)
                .setStrokeWidth(20f)
                .setStrokeCap(Paint.Cap.ROUND)
                .commit()
            rcDoc.drawArc(left, top, right, bottom, 120f, 300f)
            rcDoc.getPainter().setStrokeWidth(5f).commit()
            val line = rcDoc.floatExpression(h, 40f, AnimatedFloatExpression.SUB)
            if (spec == null) {
                return@RemoteComposeWriterInterface
            }
            when (mode.toByte()) {
                RemoteComposeWriter.STOP_NOTCHES_EVEN -> {
                    var angle = 30f
                    while (angle <= 330) {
                        rcDoc.save()
                        rcDoc.rotate(angle, cx, cy)
                        rcDoc.drawLine(cx, h, cx, line)
                        rcDoc.restore()
                        angle += (330 - 30) / spec[0]
                    }
                }

                RemoteComposeWriter.STOP_NOTCHES_PERCENTS -> {
                    var i = 0

                    while (i < spec.size) {
                        val angle = spec[i] * (330 - 30) + 30
                        rcDoc.save()
                        rcDoc.rotate(angle, cx, cy)
                        rcDoc.drawLine(cx, h, cx, line)
                        rcDoc.restore()
                        i++
                    }
                }

                RemoteComposeWriter.STOP_NOTCHES_ABSOLUTE -> {
                    var i = 0
                    while (i < spec.size) {
                        val angle = spec[i]
                        rcDoc.save()
                        rcDoc.rotate(angle, cx, cy)
                        rcDoc.drawLine(cx, h, cx, line)
                        rcDoc.restore()
                        i++
                    }
                }

                RemoteComposeWriter.STOP_ENDS -> {
                    var i = 0
                    while (i < 2) {
                        val angle = (30 + (330 - 30) * i).toFloat()
                        rcDoc.save()
                        rcDoc.rotate(angle, cx, cy)
                        rcDoc.drawLine(cx, h, cx, line)
                        rcDoc.restore()
                        i++
                    }
                }
            }
            // NOTCH
            val tx = RemoteContext.FLOAT_TOUCH_POS_X
            val ty = RemoteContext.FLOAT_TOUCH_POS_Y

            val pos =
                rcDoc.addTouch(
                    180f,
                    30f,
                    330f,
                    mode,
                    0f,
                    4,
                    spec,
                    null,
                    tx,
                    cx,
                    AnimatedFloatExpression.SUB,
                    ty,
                    cy,
                    AnimatedFloatExpression.SUB,
                    AnimatedFloatExpression.ATAN2,
                    -180 / 3.141f,
                    AnimatedFloatExpression.MUL,
                    360f,
                    AnimatedFloatExpression.ADD,
                    360f,
                    AnimatedFloatExpression.MOD,
                )

            rcDoc
                .getPainter()
                .setColor(Color.RED)
                .setStyle(Paint.Style.FILL)
                .setStrokeWidth(0f)
                .commit()

            rcDoc.save()
            rcDoc.rotate(pos, cx, cy)
            rcDoc.drawCircle(cx, bottom, 20f)
            rcDoc.restore()
            // Label
            val value =
                rcDoc.floatExpression(
                    pos,
                    30f,
                    AnimatedFloatExpression.SUB,
                    300f,
                    AnimatedFloatExpression.DIV,
                )
            val valueStr = rcDoc.createTextFromFloat(value, 1, 2, TextFromFloat.PAD_AFTER_ZERO)
            rcDoc.getPainter().setColor(Color.WHITE).commit()
            rcDoc.drawTextAnchored(
                valueStr,
                cx,
                cy,
                0f,
                0f,
                DrawTextAnchored.ANCHOR_MONOSPACE_MEASURE,
            )
            var str = title
            if (str == null) {
                str = "NULL"
            }

            rcDoc.getPainter().setColor(Color.YELLOW).setTextSize(32f).commit()

            rcDoc.drawTextAnchored(str, cx, cy, 0f, 6f, DrawTextAnchored.ANCHOR_MONOSPACE_MEASURE)
            rcDoc.endCanvas()
            rcDoc.endBox()
        }
    )
    return rcDoc
}

@Suppress("RestrictedApiAndroidX")
fun demoTouchWrap(): RemoteComposeWriter {
    val mode = RemoteComposeWriter.STOP_NOTCHES_EVEN.toInt()
    val spec = floatArrayOf(4f)
    val title = "wrap"
    val rcDoc = RemoteComposeWriterAndroid(300, 300, "Clock", platform)
    rcDoc.root(
        RemoteComposeWriterInterface {
            rcDoc.startBox(
                RecordingModifier().fillMaxWidth().fillMaxHeight(),
                BoxLayout.START,
                BoxLayout.START,
            )
            rcDoc.startCanvas(RecordingModifier().fillMaxSize())
            val anim =
                rcDoc.floatExpression(
                    RemoteContext.FLOAT_CONTINUOUS_SEC,
                    2f,
                    AnimatedFloatExpression.MOD,
                    1f,
                    AnimatedFloatExpression.SUB,
                )

            rcDoc.getPainter().setColor(Color.BLUE).commit()
            val w = rcDoc.addComponentWidthValue()
            val h = rcDoc.addComponentHeightValue()
            rcDoc.drawRoundRect(0f, 0f, w, h, 60f, 60f)

            rcDoc.getPainter().setColor(Color.RED).commit()
            val cx = rcDoc.floatExpression(w, 0.5f, AnimatedFloatExpression.MUL)
            val cy = rcDoc.floatExpression(h, 0.5f, AnimatedFloatExpression.MUL)

            rcDoc.save()
            rcDoc.scale(anim, 1f, cx, cy)
            // rcDoc.drawOval(0, 0, RemoteContext.FLOAT_WINDOW_WIDTH,
            // RemoteContext.FLOAT_WINDOW_HEIGHT);
            rcDoc.getPainter().setColor(Color.GRAY).setTextSize(64f).commit()
            rcDoc.restore()

            // track
            val rad =
                rcDoc.floatExpression(
                    w,
                    h,
                    AnimatedFloatExpression.MIN,
                    2f,
                    AnimatedFloatExpression.DIV,
                    30f,
                    AnimatedFloatExpression.SUB,
                )
            val left = rcDoc.floatExpression(cx, rad, AnimatedFloatExpression.SUB)
            val right = rcDoc.floatExpression(cx, rad, AnimatedFloatExpression.ADD)
            val top = rcDoc.floatExpression(cy, rad, AnimatedFloatExpression.SUB)
            val bottom = rcDoc.floatExpression(cy, rad, AnimatedFloatExpression.ADD)

            // NOTCH
            val tx = RemoteContext.FLOAT_TOUCH_POS_X
            val ty = RemoteContext.FLOAT_TOUCH_POS_Y

            val pos =
                rcDoc.addTouch(
                    180f,
                    Float.Companion.NaN,
                    360f,
                    mode,
                    0f,
                    4,
                    spec,
                    rcDoc.easing(5f, 0.2f, 1f),
                    *floatArrayOf(
                        tx,
                        cx,
                        AnimatedFloatExpression.SUB,
                        ty,
                        cy,
                        AnimatedFloatExpression.SUB,
                        AnimatedFloatExpression.ATAN2,
                        -180 / 3.141f,
                        AnimatedFloatExpression.MUL,
                        360f,
                        AnimatedFloatExpression.ADD,
                        360f,
                        AnimatedFloatExpression.MOD,
                    ),
                )

            rcDoc
                .getPainter()
                .setColor(Color.RED)
                .setStyle(Paint.Style.FILL)
                .setStrokeWidth(0f)
                .commit()

            rcDoc.save()
            rcDoc.rotate(pos, cx, cy)
            rcDoc.drawRoundRect(left, top, right, bottom, 100f, 100f)
            rcDoc
                .getPainter()
                .setColor(Color.CYAN)
                .setStyle(Paint.Style.STROKE)
                .setStrokeWidth(20f)
                .commit()
            rcDoc.drawLine(cx, cy, cx, bottom)
            rcDoc.restore()

            // Label
            val valueStr = rcDoc.createTextFromFloat(pos, 3, 1, TextFromFloat.PAD_AFTER_ZERO)
            rcDoc
                .getPainter()
                .setColor(Color.WHITE)
                .setStyle(Paint.Style.FILL)
                .setStrokeWidth(0f)
                .commit()
            rcDoc.drawTextAnchored(
                valueStr,
                cx,
                cy,
                0f,
                0f,
                DrawTextAnchored.ANCHOR_MONOSPACE_MEASURE,
            )
            var str = title

            rcDoc.getPainter().setColor(Color.YELLOW).setTextSize(32f).commit()

            rcDoc.drawTextAnchored(str, cx, cy, 0f, 6f, DrawTextAnchored.ANCHOR_MONOSPACE_MEASURE)

            rcDoc.endCanvas()
            rcDoc.endBox()
        }
    )
    return rcDoc
}

@Preview @Composable fun DemoTouchWrapPreview() = RemoteDocPreview(demoTouchWrap())

@Suppress("RestrictedApiAndroidX")
fun demoTouchThumbWheel1(): RemoteComposeWriter {
    val rcDoc = RemoteComposeWriterAndroid(300, 300, "Clock", platform)
    rcDoc.root(
        RemoteComposeWriterInterface {
            rcDoc.startBox(
                RecordingModifier().fillMaxWidth().fillMaxHeight(),
                BoxLayout.START,
                BoxLayout.START,
            )
            rcDoc.startCanvas(RecordingModifier().fillMaxSize())

            //                    rcDoc.getPainter().setShader(0).commit();
            val w = rcDoc.addComponentWidthValue()
            val h = rcDoc.addComponentHeightValue()

            // rcDoc.getPainter().setColor(Color.LTGRAY).commit();
            //                    rcDoc.getPainter()
            //                            .setLinearGradient(0, 0, 0, h,
            //                                    new int[]{0xFF5544, 0xFFFFAA88,
            // 0xFFFFAA88, 0xFF5544},
            //                                    new float[]{0,0.2f,0.8f,1},
            // Shader.TileMode.CLAMP).commit();
            // rcDoc.drawRect(0, 0, w, h);
            val cx = rcDoc.floatExpression(w, 0.5f, AnimatedFloatExpression.MUL)
            val cy = rcDoc.floatExpression(h, 0.5f, AnimatedFloatExpression.MUL)

            val touch =
                rcDoc.addTouch(
                    0f,
                    Float.Companion.NaN,
                    360f,
                    RemoteComposeWriter.STOP_NOTCHES_EVEN.toInt(),
                    0f,
                    4,
                    floatArrayOf(10f),
                    rcDoc.easing(10f, 2f, 60f),
                    RemoteContext.FLOAT_TOUCH_POS_Y,
                    0.2f,
                    AnimatedFloatExpression.MUL,
                )

            rcDoc.getPainter().setShader(0).setColor(Color.BLUE).setTextSize(128f).commit()
            val num =
                rcDoc.floatExpression(
                    375f,
                    touch,
                    AnimatedFloatExpression.SUB,
                    36f,
                    AnimatedFloatExpression.DIV,
                )
            val textId = rcDoc.createTextFromFloat(num, 1, 0, 0)
            rcDoc.drawTextAnchored(textId, cx, cy, -6f, 0f, 2)

            rcDoc
                .getPainter()
                .setTextSize(128f)
                .setLinearGradient(
                    0f,
                    0f,
                    0f,
                    h,
                    intArrayOf(Color.TRANSPARENT, -0xbbbbbc, Color.BLACK, Color.TRANSPARENT),
                    floatArrayOf(0f, 0.4f, 0.8f, 1f),
                    Shader.TileMode.CLAMP,
                )
                .commit()

            //                    for (int i = 0; i <= 9; i++) {
            //                        float angle = rcDoc.floatExpression(i*36f, touch, ADD,
            // RAD);
            //                        float scale = rcDoc.floatExpression(angle, COS, 0,
            // MAX);
            //                        float py = rcDoc.floatExpression(angle, SIN, cy, MUL,
            // cy, ADD);
            //                        rcDoc.save();
            //                        rcDoc.scale(1, scale, cx, py);
            //                        rcDoc.drawTextAnchored("" + i, cx, py, 0, 0, 0);
            //                        rcDoc.restore();
            //                    }
            val index = rcDoc.startLoop(10f)
            val angle =
                rcDoc.floatExpression(
                    index,
                    36f,
                    AnimatedFloatExpression.MUL,
                    touch,
                    AnimatedFloatExpression.ADD,
                    AnimatedFloatExpression.RAD,
                )
            val scale =
                rcDoc.floatExpression(
                    angle,
                    AnimatedFloatExpression.COS,
                    0f,
                    AnimatedFloatExpression.MAX,
                )
            val py =
                rcDoc.floatExpression(
                    angle,
                    AnimatedFloatExpression.SIN,
                    cy,
                    0.8f,
                    AnimatedFloatExpression.MUL,
                    AnimatedFloatExpression.MUL,
                    cy,
                    AnimatedFloatExpression.ADD,
                )
            rcDoc.save()
            rcDoc.scale(1f, scale, cx, py)
            val indexText = rcDoc.createTextFromFloat(index, 1, 0, 0)
            rcDoc.drawTextAnchored(indexText, cx, py, 0f, 0f, 0)
            rcDoc.restore()

            rcDoc.endLoop()
            val rrTop = rcDoc.floatExpression(cy, 64f, AnimatedFloatExpression.SUB)
            val rrBottom = rcDoc.floatExpression(cy, 64f, AnimatedFloatExpression.ADD)
            rcDoc.getPainter().setColor(Color.GRAY).setStyle(Paint.Style.STROKE).commit()
            rcDoc.drawRoundRect(0f, rrTop, w, rrBottom, 60f, 60f)
            rcDoc.endCanvas()
            rcDoc.endBox()
        }
    )

    return rcDoc
}

@Preview @Composable fun DemoTouchThumbWheel1Preview() = RemoteDocPreview(demoTouchThumbWheel1())

@Suppress("RestrictedApiAndroidX")
fun demoTouchThumbWheel2(): RemoteComposeWriter {
    val rcDoc = RemoteComposeWriterAndroid(300, 300, "Clock", platform)
    rcDoc.root(
        RemoteComposeWriterInterface {
            rcDoc.startRow(
                RecordingModifier().fillMaxWidth().fillMaxHeight(),
                RowLayout.SPACE_BETWEEN,
                RowLayout.TOP,
            )
            val clickIdRef = dial1(rcDoc)
            dial2(rcDoc, clickIdRef)
            rcDoc.endRow()
        }
    )

    return rcDoc
}

@Preview @Composable fun DemoTouchThumbWheel2Preview() = RemoteDocPreview(demoTouchThumbWheel2())

@Suppress("RestrictedApiAndroidX")
private fun dial1(rcDoc: RemoteComposeWriterAndroid): Int {
    rcDoc.startCanvas(RecordingModifier().width(600).fillMaxHeight().background(-0x554434))
    val w = rcDoc.addComponentWidthValue()
    val h = rcDoc.addComponentHeightValue()
    val list =
        arrayOf<String?>(
            "NO HAPTICS",
            "LONG PRESS",
            "VIRTUAL KEY",
            "KEYBOARD TAP",
            "CLOCK TICK",
            "CONTEXT CLICK",
            "KEYBOARD PRESS",
            "KEYBOARD RELEASE",
            "VIRTUAL KEY RELEASE",
            "TEXT HANDLE MOVE",
            "GESTURE START",
            "GESTURE END",
            "CONFIRM",
            "REJECT",
            "TOGGLE ON",
            "TOGGLE OFF",
            "THRESHOLD ACTIVATE",
            "THRESHOLD DEACTIVATE",
            "DRAG START",
            "SEGMENT TICK",
            "FREQUENT TICK",
        )
    val strList = rcDoc.addStringList(*list)

    val cx = rcDoc.floatExpression(w, 0.5f, AnimatedFloatExpression.MUL)
    val cy = rcDoc.floatExpression(h, 0.5f, AnimatedFloatExpression.MUL)

    val touch =
        rcDoc.addTouch(
            0f,
            Float.Companion.NaN,
            360f,
            RemoteComposeWriter.STOP_NOTCHES_EVEN.toInt(),
            0f,
            4,
            floatArrayOf(list.size.toFloat()),
            rcDoc.easing(10f, 2f, 60f),
            RemoteContext.FLOAT_TOUCH_POS_Y,
            0.2f,
            AnimatedFloatExpression.MUL,
        )
    val num =
        rcDoc.floatExpression(
            360f,
            touch,
            AnimatedFloatExpression.SUB,
            (360f / list.size),
            AnimatedFloatExpression.DIV,
            AnimatedFloatExpression.ROUND,
        )
    rcDoc.getPainter().setShader(0).setColor(Color.BLUE).setTextSize(128f).commit()
    val numText = rcDoc.createTextFromFloat(num, 2, 0, 0)
    rcDoc.drawTextAnchored(numText, cx, cy, 0f, 0f, 2)

    rcDoc
        .getPainter()
        .setTextSize(48f)
        .setLinearGradient(
            0f,
            0f,
            0f,
            h,
            intArrayOf(
                Color.TRANSPARENT,
                -0xbbbbbc,
                Color.BLACK,
                Color.GREEN,
                Color.GREEN,
                Color.BLACK,
                Color.BLACK,
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.4f, 0.45f, 0.48f, 0.52f, 0.53f, 0.8f, 1f),
            Shader.TileMode.CLAMP,
        )
        .commit()

    val index = rcDoc.startLoop(list.size.toFloat())
    val angle =
        rcDoc.floatExpression(
            index,
            360f,
            list.size.toFloat(),
            AnimatedFloatExpression.DIV,
            AnimatedFloatExpression.MUL,
            touch,
            AnimatedFloatExpression.ADD,
            AnimatedFloatExpression.RAD,
        )
    val scale =
        rcDoc.floatExpression(angle, AnimatedFloatExpression.COS, 0f, AnimatedFloatExpression.MAX)
    val py =
        rcDoc.floatExpression(
            angle,
            AnimatedFloatExpression.SIN,
            cy,
            0.8f,
            AnimatedFloatExpression.MUL,
            AnimatedFloatExpression.MUL,
            cy,
            AnimatedFloatExpression.ADD,
        )
    val textId = rcDoc.textLookup(strList, index)
    rcDoc.save()
    rcDoc.scale(1f, scale, cx, py)

    rcDoc.drawTextAnchored(textId, cx, py, 0f, 0f, 0)
    rcDoc.restore()

    rcDoc.endLoop()
    rcDoc.getPainter().setShader(0).commit()

    rcDoc.endCanvas()
    return Utils.idFromNan(num)
}

@Suppress("RestrictedApiAndroidX")
private fun dial2(rcDoc: RemoteComposeWriterAndroid, clickIdRef: Int) {
    rcDoc.startCanvas(RecordingModifier().width(200).fillMaxHeight().background(Color.LTGRAY))
    val w = rcDoc.addComponentWidthValue()
    val h = rcDoc.addComponentHeightValue()

    val cx = rcDoc.floatExpression(w, 0.5f, AnimatedFloatExpression.MUL)
    val cy = rcDoc.floatExpression(h, 0.5f, AnimatedFloatExpression.MUL)

    val touch =
        rcDoc.addTouch(
            0f,
            Float.Companion.NaN,
            360f,
            RemoteComposeWriter.STOP_NOTCHES_EVEN.toInt(),
            0f,
            clickIdRef or RemoteComposeWriter.ID_REFERENCE,
            floatArrayOf(10f),
            rcDoc.easing(10f, 2f, 60f),
            RemoteContext.FLOAT_TOUCH_POS_Y,
            0.2f,
            AnimatedFloatExpression.MUL,
        )

    rcDoc
        .getPainter()
        .setTextSize(128f)
        .setLinearGradient(
            0f,
            0f,
            0f,
            h,
            intArrayOf(Color.TRANSPARENT, -0xbbbbbc, Color.BLACK, Color.TRANSPARENT),
            floatArrayOf(0f, 0.4f, 0.8f, 1f),
            Shader.TileMode.CLAMP,
        )
        .commit()

    val index = rcDoc.startLoop(10f)
    val angle =
        rcDoc.floatExpression(
            index,
            36f,
            AnimatedFloatExpression.MUL,
            touch,
            AnimatedFloatExpression.ADD,
            AnimatedFloatExpression.RAD,
        )
    val scale =
        rcDoc.floatExpression(angle, AnimatedFloatExpression.COS, 0f, AnimatedFloatExpression.MAX)
    val py =
        rcDoc.floatExpression(
            angle,
            AnimatedFloatExpression.SIN,
            cy,
            0.8f,
            AnimatedFloatExpression.MUL,
            AnimatedFloatExpression.MUL,
            cy,
            AnimatedFloatExpression.ADD,
        )
    rcDoc.save()
    rcDoc.scale(1f, scale, cx, py)
    val indexText = rcDoc.createTextFromFloat(index, 1, 0, 0)
    rcDoc.drawTextAnchored(indexText, cx, py, 0f, 0f, 0)
    rcDoc.restore()

    rcDoc.endLoop()
    rcDoc.getPainter().setShader(0).commit()

    rcDoc.endCanvas()
}

@Suppress("RestrictedApiAndroidX")
fun touchStopGently(): RemoteComposeWriter {
    return demoTouch3(RemoteComposeWriter.STOP_GENTLY.toInt(), null, "STOP_GENTLY")
}

@Preview @Composable fun TouchStopGentlyPreview() = RemoteDocPreview(touchStopGently())

@Suppress("RestrictedApiAndroidX")
fun touchStopEnds(): RemoteComposeWriter {
    return demoTouch3(RemoteComposeWriter.STOP_ENDS.toInt(), null, "STOP_ENDS")
}

@Preview @Composable fun TouchStopEndsPreview() = RemoteDocPreview(touchStopEnds())

@Suppress("RestrictedApiAndroidX")
fun touchStopInstantly(): RemoteComposeWriter {
    return demoTouch3(RemoteComposeWriter.STOP_INSTANTLY.toInt(), null, "STOP_INSTANTLY")
}

@Preview @Composable fun TouchStopInstantlyPreview() = RemoteDocPreview(touchStopInstantly())

@Suppress("RestrictedApiAndroidX")
fun touchStopNotchesEven(): RemoteComposeWriter {
    return demoTouch3(
        RemoteComposeWriter.STOP_NOTCHES_EVEN.toInt(),
        floatArrayOf(10f),
        "STOP_NOTCHES_EVEN",
    )
}

@Preview @Composable fun TouchStopNotchesEvenPreview() = RemoteDocPreview(touchStopNotchesEven())

@Suppress("RestrictedApiAndroidX")
fun touchStopNotchesPercents(): RemoteComposeWriter {
    return demoTouch3(
        RemoteComposeWriter.STOP_NOTCHES_PERCENTS.toInt(),
        floatArrayOf(0f, 0.25f, 0.33333f, 0.5f, 0.66666f, 0.75f, 1f),
        "STOP_NOTCHES_PERCENTS",
    )
}

@Preview
@Composable
fun TouchStopNotchesPercentsPreview() = RemoteDocPreview(touchStopNotchesPercents())

@Suppress("RestrictedApiAndroidX")
fun touchStopNotchesAbsolute(): RemoteComposeWriter {
    return demoTouch3(
        RemoteComposeWriter.STOP_NOTCHES_ABSOLUTE.toInt(),
        floatArrayOf(30f, 60f, 180f, 330f),
        "STOP_NOTCHES_ABSOLUTE",
    )
}

@Preview
@Composable
fun TouchStopNotchesAbsolutePreview() = RemoteDocPreview(touchStopNotchesAbsolute())

@Suppress("RestrictedApiAndroidX")
fun touchStopAbsolutePos(): RemoteComposeWriter {
    return demoTouch3(RemoteComposeWriter.STOP_ABSOLUTE_POS.toInt(), null, "STOP_ABSOLUTE_POS")
}

@Preview @Composable fun TouchStopAbsolutePosPreview() = RemoteDocPreview(touchStopAbsolutePos())

@Suppress("RestrictedApiAndroidX")
fun simpleJavaAnim(): RemoteComposeWriter {
    val rcDoc = RemoteComposeWriterAndroid(300, 300, "Clock", platform)

    rcDoc.root(
        RemoteComposeWriterInterface {
            rcDoc.startBox(
                RecordingModifier().fillMaxWidth().fillMaxHeight(),
                BoxLayout.START,
                BoxLayout.START,
            )
            rcDoc.startCanvas(RecordingModifier().fillMaxSize())
            val anim =
                rcDoc.floatExpression(
                    RemoteContext.FLOAT_CONTINUOUS_SEC,
                    2f,
                    AnimatedFloatExpression.MOD,
                    1f,
                    AnimatedFloatExpression.SUB,
                )
            rcDoc.getPainter().setColor(Color.RED).commit()
            val cx =
                rcDoc.floatExpression(
                    RemoteContext.FLOAT_WINDOW_WIDTH,
                    0.5f,
                    AnimatedFloatExpression.MUL,
                )
            val cy =
                rcDoc.floatExpression(
                    RemoteContext.FLOAT_WINDOW_HEIGHT,
                    0.5f,
                    AnimatedFloatExpression.MUL,
                )
            rcDoc.save()
            rcDoc.scale(anim, 1f, cx, cy)
            rcDoc.drawOval(
                0f,
                0f,
                RemoteContext.FLOAT_WINDOW_WIDTH,
                RemoteContext.FLOAT_WINDOW_HEIGHT,
            )
            rcDoc.restore()
            rcDoc.endCanvas()
            rcDoc.endBox()
        }
    )
    return rcDoc
}

@Preview @Composable fun SimpleJavaAnimPreview() = RemoteDocPreview(simpleJavaAnim())
