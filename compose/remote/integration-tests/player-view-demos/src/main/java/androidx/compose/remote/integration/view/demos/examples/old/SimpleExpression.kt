/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.SUB
import androidx.compose.remote.creation.ComponentHeight
import androidx.compose.remote.creation.ComponentWidth
import androidx.compose.remote.creation.ContinuousSec
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.min
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** Creates a spinning test */
@Suppress("RestrictedApiAndroidX")
fun SimpleExprTest(): RemoteComposeContext {
    return RemoteComposeContextAndroid(800, 800, "Clock", platform = AndroidxRcPlatformServices()) {
        root {
            box(modifier = RecordingModifier().fillMaxSize()) {
                canvas(modifier = RecordingModifier().fillMaxSize()) {
                    val width = ComponentWidth()
                    val height = ComponentHeight()
                    val centerX = width / 2f
                    val centerY = height / 2f
                    val rad = min(centerX, centerY)
                    val textSize = addFloatConstant(200f)
                    painter.setShader(0).setTextSize(123f).setColor(Color.Gray.toArgb()).commit()
                    val sweep = (ContinuousSec() % 4F - 2f).flush()
                    painter
                        .setTextSize(textSize)
                        .setSweepGradient(
                            centerX.toFloat(),
                            centerY.toFloat(),
                            intArrayOf(
                                Color(0xFFAA8844).toArgb(),
                                Color(0xFF88AA44).toArgb(),
                                Color(0xFF88AA44).toArgb(),
                                Color(0xFF8844AA).toArgb(),
                                Color(0xFF8844AA).toArgb(),
                                Color(0xFF44AA88).toArgb(),
                                Color(0xFF44AA88).toArgb(),
                                Color(0xFFAA8833).toArgb(),
                                Color(0xFFAA8833).toArgb(),
                                Color(0xFFAA4333).toArgb(),
                                Color(0xFFAA4333).toArgb(),
                                Color(0xFFAA8844).toArgb(),
                            ),
                            null,
                        )
                        .setStyle(Paint.Style.FILL)
                        .commit()
                    save()
                    rotate((ContinuousSec() * 180f).flush(), centerX.flush(), centerY.flush())
                    drawCircle(centerX.toFloat(), centerY.toFloat(), rad.toFloat())
                    // drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), 300f, 300f)
                    restore()
                    painter
                        .setTextSize(textSize)
                        .setLinearGradient(
                            90f,
                            0f,
                            floatExpression(width.toFloat(), 90f, SUB),
                            0f,
                            intArrayOf(
                                Color.Transparent.toArgb(),
                                Color.White.toArgb(),
                                Color.White.toArgb(),
                                Color.Transparent.toArgb(),
                            ),
                            floatArrayOf(0f, 0.2f, 0.8f, 1.0f),
                            Shader.TileMode.CLAMP,
                        )
                        .setStyle(Paint.Style.FILL)
                        .commit()

                    var id = textCreateId(" This is a test for now.")

                    drawTextAnchored(id, centerX.flush(), centerY.flush(), sweep, 0f, 2)
                }
            }
        }
    }
}
