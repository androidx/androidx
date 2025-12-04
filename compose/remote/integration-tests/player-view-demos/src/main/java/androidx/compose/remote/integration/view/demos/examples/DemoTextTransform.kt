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
import androidx.compose.remote.core.operations.TextTransform
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.pingPong
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import kotlin.random.Random

@Suppress("RestrictedApiAndroidX")
fun demoTextTransform(): RemoteComposeWriter {
    val rc =
        RemoteComposeContextAndroid(
            width = 500,
            height = 500,
            contentDescription = "Simple Timer",
            apiLevel = 7,
            profiles = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            platform = AndroidxRcPlatformServices(),
        ) {
            val bounce = pingPong(1, ContinuousSec()).toFloat()
            val col = addColorExpression(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), bounce)
            root {
                box(RecordingModifier().fillMaxSize(), BoxLayout.START, BoxLayout.START) {
                    column(RecordingModifier().backgroundId(col)) {
                        text(
                            getTimeString(),
                            RecordingModifier()
                                .fillMaxWidth()
                                .height(240)
                                .background(0xFF99FFFF.toInt()),
                            fontSize = 100f,
                            color = 0xFF000000.toInt(),
                            textAlign = Rc.Text.ALIGN_CENTER,
                        )
                        beginGlobal()
                        val basic = addText(" hard work John ")
                        val upper =
                            textMerge(
                                addText("upper:"),
                                textTransform(basic, TextTransform.TEXT_TO_UPPERCASE),
                            )
                        val lower =
                            textMerge(
                                addText("lower:"),
                                textTransform(basic, TextTransform.TEXT_TO_LOWERCASE),
                            )
                        val trim =
                            textMerge(
                                addText("trim:"),
                                textTransform(basic, TextTransform.TEXT_TRIM),
                            )
                        val capitalize =
                            textMerge(
                                addText("capitalize:"),
                                textTransform(basic, TextTransform.TEXT_CAPITALIZE),
                            )
                        val sentence =
                            textMerge(
                                addText("Sentence:"),
                                textTransform(basic, TextTransform.TEXT_UPPERCASE_FIRST_CHAR),
                            )

                        endGlobal()
                        txt(basic)
                        txt(upper)
                        txt(lower)
                        txt(trim)
                        txt(capitalize)
                        txt(sentence)
                        canvas(RecordingModifier().fillMaxWidth().height(400)) {
                            val w = ComponentWidth()
                            val h = ComponentHeight()
                            painter.setColor(Random.nextInt()).commit()
                            drawRoundRect(0, 0, w, h, 2f, 2f)
                            painter.setColorId(col.toInt()).commit()
                            drawCircle((w / 2f).toFloat(), (h / 2f).toFloat(), 100f)
                            painter.setColor(0xFF000000.toInt()).setTextSize(100f).commit()
                            drawTextAnchored(getTimeString(), w / 2f, h / 2f, 0, 0, 0)
                        }
                    }
                }
            }
        }
    return rc.writer
}

@Suppress("RestrictedApiAndroidX")
fun RemoteComposeContextAndroid.txt(str: Int) {
    text(
        str,
        RecordingModifier().fillMaxWidth().height(100).background(Random.nextInt()),
        fontSize = 80f,
        color = 0xFF000000.toInt(),
        textAlign = Rc.Text.ALIGN_LEFT,
    )
}
