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

import android.graphics.Typeface
import androidx.compose.remote.core.RemoteComposeBuffer.PAD_AFTER_ZERO
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.ADD
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.COS
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.DIV
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MOD
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.SIN
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Suppress("RestrictedApiAndroidX")
fun countDown(): RemoteComposeContext {
    var str = "HelloWorld"
    val colors =
        intArrayOf(
            Color.hsv(0f, 0.9f, 0.9f).toArgb(),
            Color.hsv(60f, 0.9f, 0.9f).toArgb(),
            Color.hsv(120f, 0.9f, 0.9f).toArgb(),
            Color.hsv(180f, 0.9f, 0.9f).toArgb(),
            Color.hsv(240f, 0.9f, 0.9f).toArgb(),
            Color.hsv(300f, 0.9f, 0.9f).toArgb(),
            Color.hsv(360f, 0.9f, 0.9f).toArgb(),
        )

    val doc =
        RemoteComposeContextAndroid(600, 600, "Demo") {
            painter.setTextSize(50f).commit()

            val pi2 = (PI * 2).toFloat()
            val x =
                floatExpression(
                    RemoteContext.FLOAT_CONTINUOUS_SEC,
                    3f,
                    MUL,
                    COS,
                    200f,
                    MUL,
                    300f,
                    ADD,
                )
            val y =
                floatExpression(
                    RemoteContext.FLOAT_CONTINUOUS_SEC,
                    3f,
                    MUL,
                    SIN,
                    200f,
                    MUL,
                    300f,
                    ADD,
                )
            val hue =
                floatExpression(RemoteContext.FLOAT_CONTINUOUS_SEC, 3f, MUL, pi2, DIV, 1f, MOD)
            painter.setSweepGradient(300f, 300f, colors, null).commit()
            drawCircle(300f, 300f, 200f)
            painter.setShader(0).commit()
            val id1: Short = addColorExpression(0x8F, hue, 0.9f, 0.9f)
            painter.setColorId(id1.toInt()).commit()
            drawCircle(x, y, 100f)
            painter
                .setColor(Color.Blue.toArgb())
                .setTextSize(100f)
                .setTypeface(Typeface.MONOSPACE)
                .commit()
            val textId = createTextFromFloat(hue, 1, 2, PAD_AFTER_ZERO)
            val merge = textMerge(textCreateId("Hue:"), textId)
            save()
            scale(hue, hue, 300f, 300f)
            drawTextAnchored(merge, 300f, 300f, 0f, 0f, 2)
            restore()
            for (i in 0..5) {
                val id2: Short = addColorExpression(0x8F, i / 6f, 0.9f, 0.9f)
                painter.setColorId(id2.toInt()).commit()
                val angle = i * Math.PI * 2f / 6
                val x = 300 + (cos(angle) * 200).toFloat()
                val y = 300 + (sin(angle) * 200).toFloat()
                drawCircle(x, y, 100f)
            }
        }
    return doc
}
