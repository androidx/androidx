/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.integration.view.demos.examples.old

import android.graphics.Color
import android.graphics.Paint
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.Rc.FloatExpression.ADD
import androidx.compose.remote.creation.Rc.FloatExpression.DIV
import androidx.compose.remote.creation.Rc.FloatExpression.MIN
import androidx.compose.remote.creation.Rc.FloatExpression.MOD
import androidx.compose.remote.creation.Rc.FloatExpression.MUL
import androidx.compose.remote.creation.Rc.FloatExpression.SUB
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import java.text.DecimalFormat

@Suppress("RestrictedApiAndroidX")
private var sPlatform: RcPlatformServices = AndroidxRcPlatformServices()

@Suppress("RestrictedApiAndroidX")
fun kClock1(): RemoteComposeWriter {
    val rc = RemoteComposeWriterAndroid(300, 300, "Clock", sPlatform)

    rc.root {
        rc.startRow(RecordingModifier().fillMaxWidth().fillMaxHeight().padding(10), 1, 1)
        run {
            rc.startBox(
                RecordingModifier().horizontalWeight(1f).fillMaxHeight().padding(0, 0, 5, 0),
                BoxLayout.START,
                BoxLayout.TOP,
            )
            rc.startCanvas(RecordingModifier().fillMaxSize())
            val hr =
                rc.floatExpression(
                    rc.exp(Rc.Time.TIME_IN_HR, 1f, SUB, 12f, MOD, 1f, ADD),
                    rc.anim(0.4f, Rc.Animate.CUBIC_OVERSHOOT),
                )
            draw(rc, hr, true)

            rc.endCanvas()
            rc.endBox()
        }
        run {
            rc.startBox(
                RecordingModifier().horizontalWeight(1f).fillMaxHeight().padding(5, 0, 0, 0),
                BoxLayout.START,
                BoxLayout.TOP,
            )
            rc.startCanvas(RecordingModifier().fillMaxSize())
            val min =
                rc.floatExpression(
                    rc.exp(Rc.Time.TIME_IN_MIN, 60f, MOD),
                    rc.anim(0.4f, Rc.Animate.CUBIC_OVERSHOOT),
                )
            draw(rc, min, false)
            rc.endCanvas()
            rc.endBox()
        }
        rc.endRow()
    }
    return rc
}

@Suppress("RestrictedApiAndroidX")
private fun draw(rc: RemoteComposeWriterAndroid, `val`: Float, hours: Boolean) {
    val w = rc.addComponentWidthValue()
    val h = rc.addComponentHeightValue()
    val cx = rc.floatExpression(w, 0.5f, MUL)
    val cy = rc.floatExpression(h, 0.5f, MUL)
    val fontSize = rc.floatExpression(w, h, MIN, 2f, DIV)
    val mod = if ((hours)) 12 else 60
    rc.painter.setColor(Color.BLUE).setStyle(Paint.Style.FILL).commit()
    rc.drawRoundRect(0f, 0f, w, h, 32f, 32f)
    rc.save()
    rc.clipRect(0f, 0f, w, h)

    val anim = rc.floatExpression(`val`, -1f, fontSize, MUL, MUL)

    rc.translate(0f, anim)
    rc.painter.setTextSize(fontSize).setColor(Color.RED).setStyle(Paint.Style.FILL).commit()

    val df = DecimalFormat("00")
    rc.translate(0f, rc.floatExpression(fontSize, -4f, MUL))
    for (i in -3 until mod + 3) {
        rc.translate(0f, fontSize)
        val value = if ((hours)) ((12 + i - 1) % 12) + 1 else (60 + i) % 60
        rc.drawTextAnchored(df.format(value.toLong()), cx, cy, 0f, 0f, 0)
    }

    rc.restore()
}

@Suppress("RestrictedApiAndroidX")
fun kClock2(): RemoteComposeWriter {
    val rc = RemoteComposeWriterAndroid(300, 300, "Clock", sPlatform)
    val w = rc.floatExpression(Rc.System.WINDOW_WIDTH)
    val w2 = rc.floatExpression(Rc.System.WINDOW_WIDTH, 2f, DIV)
    val h = rc.floatExpression(Rc.System.WINDOW_HEIGHT)
    val cx1 = rc.floatExpression(w2, 0.5f, MUL)
    val cx2 = rc.floatExpression(w2, 1.5f, MUL)
    val cy = rc.floatExpression(h, 0.5f, MUL)
    rc.painter.setColor(Color.BLUE).setStyle(Paint.Style.FILL).commit()
    val right1 = rc.floatExpression(w2, 10f, SUB)
    val bottom1 = rc.floatExpression(h, 20f, SUB)
    rc.drawRoundRect(20f, 20f, right1, bottom1, 32f, 32f)
    val left2 = rc.floatExpression(w2, 10f, ADD)
    val right2 = rc.floatExpression(w, 20f, SUB)
    rc.drawRoundRect(left2, 20f, right2, bottom1, 32f, 32f)
    val fontSize = rc.floatExpression(w2, 2f, DIV)
    rc.painter.setTextSize(fontSize).setColor(Color.RED).setStyle(Paint.Style.FILL).commit()

    val hr =
        rc.floatExpression(
            rc.exp(Rc.Time.TIME_IN_HR, 1f, SUB, 12f, MOD, 1f, ADD),
            rc.anim(0.4f, Rc.Animate.CUBIC_OVERSHOOT),
        )
    val min =
        rc.floatExpression(
            rc.exp(RemoteContext.FLOAT_TIME_IN_MIN, 60f, MOD),
            rc.anim(0.4f, Rc.Animate.CUBIC_OVERSHOOT),
        )
    val df = DecimalFormat("00")

    // ============== hr (left) ================
    run {
        val hrMod = 12
        rc.save()
        rc.clipRect(20f, 20f, right1, bottom1)
        val hrAnim = rc.floatExpression(hr, -1f, fontSize, MUL, MUL)
        rc.translate(0f, hrAnim)
        rc.translate(0f, rc.floatExpression(fontSize, -4f, MUL))
        for (i in -3 until hrMod + 3) {
            rc.translate(0f, fontSize)
            val value = ((hrMod + i - 1) % hrMod) + 1
            rc.drawTextAnchored(df.format(value.toLong()), cx1, cy, 0f, 0f, 0)
        }
        rc.restore()
    }
    // ================ min (right) ================
    run {
        val minMod = 60
        rc.save()
        rc.clipRect(left2, 20f, right2, bottom1)
        val minAnim = rc.floatExpression(min, -1f, fontSize, MUL, MUL)
        rc.translate(0f, minAnim)
        rc.translate(0f, rc.floatExpression(fontSize, -4f, MUL))

        for (i in -3 until minMod + 3) {
            rc.translate(0f, fontSize)
            val value = (minMod + i) % minMod
            rc.drawTextAnchored(df.format(value.toLong()), cx2, cy, 0f, 0f, 0)
        }
        rc.restore()
    }
    return rc
}
