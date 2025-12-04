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
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.Rc.FloatExpression.*
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.cos
import androidx.compose.remote.creation.hypot
import androidx.compose.remote.creation.min
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.sin

@Suppress("RestrictedApiAndroidX")
fun demoUseOfGlobal(): RemoteComposeWriter {
    val rc =
        RemoteComposeContextAndroid(
            width = 500,
            height = 500,
            contentDescription = "Simple Timer",
            apiLevel = 6,
            profiles = 0,
            platform = AndroidxRcPlatformServices(),
        ) {
            root {
                column {
                    clock()
                    date()
                }
            }
        }
    return rc.writer
}

@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.date() {
    box(RecordingModifier().width(500).height(120), BoxLayout.START, BoxLayout.START) {
        beginGlobal()
        val space = addText(":")
        val tid1 =
            createTextFromFloat((Seconds() % 60f).toFloat(), 2, 0, Rc.TextFromFloat.PAD_PRE_ZERO)
        val tid2 =
            createTextFromFloat((Minutes() % 60f).toFloat(), 2, 0, Rc.TextFromFloat.PAD_PRE_ZERO)
        endGlobal()
        beginGlobal() // to prove we can have multiple sections
        val tid3 = createTextFromFloat((Hour() % 12f).toFloat(), 2, 0, 0)
        val clock = textMerge(tid3, space, tid2, space, tid1)
        endGlobal()
        text(
            clock,
            RecordingModifier().fillMaxSize().background(0xFF99FFFF.toInt()),
            fontSize = 100f,
        )
    }
}

@Suppress("RestrictedApiAndroidX")
fun RemoteComposeContextAndroid.clock() {
    box(
        RecordingModifier().width(500).height(500).clip(RoundedRectShape(30f, 30f, 30f, 30f)),
        BoxLayout.START,
        BoxLayout.START,
    ) {
        canvas(RecordingModifier().fillMaxSize()) {
            val w = ComponentWidth()
            val h = ComponentHeight()
            val cx = w / 2f
            val cy = h / 2f
            val rad = min(cx, cy)
            val rad2 = hypot(cx, cy)

            painter
                .setRadialGradient(
                    cx.toFloat(),
                    cy.toFloat(),
                    rad2.toFloat(),
                    intArrayOf(Color.LTGRAY, Color.DKGRAY),
                    floatArrayOf(0f, 2f),
                    Shader.TileMode.CLAMP,
                )
                .commit()

            drawRoundRect(0, 0, w, h, rad / 4f, rad / 4f)

            painter
                .setColor(0x99888888L.toInt())
                .setShader(0)
                .setStrokeWidth(32f)
                .setStrokeCap(Paint.Cap.ROUND)
                .commit()
            drawSector(
                rad * -1f,
                rad * -1f,
                w + rad,
                h + rad,
                -90f,
                ((ContinuousSec() * 360f) % 360f),
            )
            painter.setColor(Color.BLACK).setTextSize(512f).commit()
            val id =
                createTextFromFloat(((ContinuousSec() % 10f) * -1f + 9.999f).toFloat(), 1, 0, 0)
            save {
                scale(ContinuousSec() % 1f, ContinuousSec() % 1f, cx, cy)
                drawTextAnchored(id, cx, cy, 0f, 0f, 0)
            }

            painter.setColor(Color.WHITE).setStrokeWidth(4f).commit()
            drawLine(
                cx,
                cy,
                w / 2f + rad * sin(ContinuousSec() * (2 * Math.PI.toFloat())),
                h / 2f - rad * cos(ContinuousSec() * (2 * Math.PI.toFloat())),
            )
        }
    }
}
