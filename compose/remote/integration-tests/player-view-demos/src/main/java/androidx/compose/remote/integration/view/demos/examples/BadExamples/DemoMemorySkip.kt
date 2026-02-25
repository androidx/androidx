/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.integration.view.demos.examples.BadExamples

import android.graphics.Color
import android.graphics.Paint
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Skip
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.min
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices

@Suppress("RestrictedApiAndroidX")
fun skip1(): RemoteComposeWriter {
    val rc =
        RemoteComposeContextAndroid(
            width = 500,
            height = 500,
            contentDescription = "Simple Timer",
            apiLevel = 8,
            profiles = RcProfiles.PROFILE_ANDROIDX,
            platform = AndroidxRcPlatformServices(),
        ) {
            root {
                box(RecordingModifier().fillMaxSize(), BoxLayout.START, BoxLayout.START) {
                    canvas(RecordingModifier().fillMaxSize()) {
                        val w = ComponentWidth() // component.width()
                        val h = ComponentHeight()
                        val cx = w / 2f
                        val cy = h / 2f
                        val rad = min(cx, cy) * 0.9f
                        val scale = rad / 14f
                        val hr = scale * 4f
                        val min = scale * 2f
                        val sec = scale
                        painter.setColor(Color.WHITE).setStyle(Paint.Style.STROKE).commit()

                        drawCircle(cx.toFloat(), cy.toFloat(), rad.toFloat())
                        painter.setColor(0xff335577L.toInt()).setStyle(Paint.Style.FILL).commit()
                        save() {
                            rotate(Seconds() * 6f, cx, cy)
                            drawCircle(
                                cx.toFloat(),
                                (cy - scale).toFloat(),
                                (rad - scale).toFloat(),
                            )
                        }
                        painter.setColor(Color.WHITE).commit()
                        save() {
                            rotate(Hour() * 30f, cx, cy)
                            scale(0.3, 1, cx, 0)

                            drawCircle(cx.toFloat(), (cy - hr).toFloat(), hr.toFloat())
                        }

                        save() {
                            rotate(Minutes() * 6f, cx, cy)
                            scale(0.3, 1, cx, 0)

                            drawCircle(cx.toFloat(), (cy - scale * 10f).toFloat(), min.toFloat())
                        }
                        skip(Skip.SKIP_IF_API_EQUAL_TO, 7) {
                            painter.setColor(Color.LTGRAY).commit()
                            save() {
                                rotate(ContinuousSec() * 6f, cx, cy)
                                scale(0.3, 1, cx, 0)
                                drawCircle(
                                    cx.toFloat(),
                                    (cy - scale * 13f).toFloat(),
                                    sec.toFloat(),
                                )
                            }
                        }
                        skip(Skip.SKIP_IF_API_EQUAL_TO, 8) {
                            painter.setColor(Color.RED).commit()
                            save() {
                                rotate(ContinuousSec() * 6f, cx, cy)
                                scale(0.8, 1, cx, 0)
                                drawCircle(
                                    cx.toFloat(),
                                    (cy - scale * 13f).toFloat(),
                                    sec.toFloat(),
                                )
                            }
                        }
                    }
                }
            }
        }
    return rc.writer
}
