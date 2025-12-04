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
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.RFloat
import androidx.compose.remote.creation.Rc.FloatExpression.*
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.cos
import androidx.compose.remote.creation.min
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.sin
import androidx.compose.remote.tooling.preview.RemoteDocPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Suppress("RestrictedApiAndroidX")
fun basicClock(): RemoteComposeWriter {
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
                box(RecordingModifier().fillMaxSize(), BoxLayout.START, BoxLayout.START) {
                    canvas(RecordingModifier().fillMaxSize()) {
                        val w = ComponentWidth() // component.width()
                        val h = ComponentHeight()
                        val cx = w / 2f
                        val cy = h / 2f
                        val rad = min(cx, cy)

                        painter.setColor(Color.BLUE).commit()

                        drawRoundRect(0, 0, w, h, rad / 4f, rad / 4f)

                        painter
                            .setColor(Color.GRAY)
                            .setStrokeWidth(32f)
                            .setStrokeCap(Paint.Cap.ROUND)
                            .commit()
                        save() {
                            rotate(Minutes() * 6f, cx, cy)
                            drawLine(cx, cy, cx, cy - rad * 0.8f)
                        }
                        painter
                            .setColor(Color.LTGRAY)
                            .setStrokeWidth(16f)
                            .setStrokeCap(Paint.Cap.ROUND)
                            .commit()
                        save() {
                            rotate(Hour() * 30f, cx, cy)
                            drawLine(cx, cy, cx, cy - rad / 2f)
                        }

                        painter.setColor(Color.WHITE).setStrokeWidth(4f).commit()
                        drawLine(
                            cx,
                            cy,
                            w / 2f + rad * sin(ContinuousSec() * (2 * Math.PI.toFloat() / 60f)),
                            h / 2f - rad * cos(ContinuousSec() * (2 * Math.PI.toFloat() / 60f)),
                        )
                    }
                }
            }
        }
    return rc.writer
}

@Suppress("RestrictedApiAndroidX")
fun multiClock(): RemoteComposeWriter {
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
                box(RecordingModifier().fillMaxSize(), BoxLayout.START, BoxLayout.START) {
                    canvas(RecordingModifier().fillMaxSize()) {
                        val w = ComponentWidth()
                        val h = ComponentHeight()
                        val cx = w / 2f
                        val cy = h / 2f
                        val rad = min(cx, cy)

                        painter.setColor(Color.BLUE).commit()
                        for (i in 1..12) {
                            clock(this as RemoteComposeContextAndroid, w, h, cx, cy, rad)
                        }
                    }
                }
            }
        }
    return rc.writer
}

@Suppress("RestrictedApiAndroidX")
fun clock(
    context: RemoteComposeContextAndroid,
    w: RFloat,
    h: RFloat,
    cx: RFloat,
    cy: RFloat,
    rad: RFloat,
) {
    with(context) {
        drawRoundRect(0, 0, w, h, rad / 4f, rad / 4f)

        painter.setColor(Color.GRAY).setStrokeWidth(32f).setStrokeCap(Paint.Cap.ROUND).commit()
        save() {
            rotate(Minutes() * 6f, cx, cy)
            drawLine(cx, cy, cx, cy - rad * 0.8f)
        }
        painter.setColor(Color.LTGRAY).setStrokeWidth(32f).setStrokeCap(Paint.Cap.ROUND).commit()
        save() {
            rotate(Hour() * 30f, cx, cy)
            drawLine(cx, cy, cx, cy - rad / 2f)
        }

        painter.setColor(Color.WHITE).setStrokeWidth(4f).commit()
        drawLine(
            cx,
            cy,
            w / 2f + rad * sin(ContinuousSec() * (2 * Math.PI.toFloat() / 60f)),
            h / 2f - rad * cos(ContinuousSec() * (2 * Math.PI.toFloat() / 60f)),
        )
    }
}

@Preview @Composable fun BasicClockPreview() = RemoteDocPreview(basicClock())
