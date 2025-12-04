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

import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.minus
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.pingPong
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.times

@Suppress("RestrictedApiAndroidX")
fun colorButtons(): RemoteComposeWriter {
    val rc =
        RemoteComposeContextAndroid(
            width = 500,
            height = 500,
            contentDescription = "Simple Timer",
            apiLevel = 7,
            profiles = RcProfiles.PROFILE_ANDROIDX,
            platform = AndroidxRcPlatformServices(),
        ) {
            val bounce = pingPong(1, ContinuousSec()).toFloat()
            val col = addColorExpression(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), bounce)
            val col2 = addColorExpression(0xFF000000.toInt(), 0xFFFFFF00.toInt(), bounce)
            root {
                box(RecordingModifier().fillMaxSize(), BoxLayout.START, BoxLayout.START) {
                    column(RecordingModifier().background(0xFF666677L.toInt())) {
                        box(
                            RecordingModifier()
                                .background(0xFF007700L.toInt())
                                .fillMaxWidth()
                                .height(120)
                        )
                        box(RecordingModifier().backgroundId(col).fillMaxWidth().height(120))
                        box(
                            RecordingModifier()
                                .background(0xFF000077L.toInt())
                                .fillMaxWidth()
                                .height(120)
                        )
                        box(
                            RecordingModifier()
                                .backgroundId(col)
                                .dynamicBorder(10f, 30f, col2, 0)
                                .fillMaxWidth()
                                .height(120)
                        )
                        box(
                            RecordingModifier()
                                .background(0xFF007777L.toInt())
                                .fillMaxWidth()
                                .height(120)
                        )

                        canvas(
                            RecordingModifier()
                                .dynamicBorder(40f, 30f, col2, 0)
                                .fillMaxWidth()
                                .height(200)
                        ) {
                            val w = ComponentWidth() // component.width()
                            val h = ComponentHeight()

                            painter.setColorId(col.toInt()).commit()
                            drawRoundRect(0, 0, w, h, 2f, 2f)
                        }
                    }
                }
            }
        }
    return rc.writer
}
