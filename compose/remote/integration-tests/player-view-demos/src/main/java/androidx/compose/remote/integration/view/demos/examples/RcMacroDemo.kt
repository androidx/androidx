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
@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.integration.view.demos.examples

import android.graphics.Color
import android.graphics.Shader
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.ValueStringChange
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Suppress("RestrictedApiAndroidX")
fun RcMacroDemo(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        AndroidxRcPlatformServices(),
        7,
        RemoteComposeWriter.HTag(
            Header.DOC_PROFILES,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        ),
    ) {
        DefineGlimmerButton()
        //       DefineMyButton()
        root {
            box(Modifier.fillMaxSize()) {
                canvas(Modifier.fillMaxSize()) {
                    painter
                        .setLinearGradient(
                            0f,
                            0f,
                            400f,
                            400f,
                            intArrayOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt()),
                            null,
                            Shader.TileMode.CLAMP,
                        )
                        .commit()
                    drawRect(0f, 0f, windowWidth(), windowHeight())
                    painter.setColor(Color.BLACK).commit()
                }
                column(Modifier.fillMaxSize(), horizontal = ColumnLayout.CENTER) {
                    text("Macro Buttons Demo", fontSize = 60f, modifier = Modifier.padding(20))
                    val contentId = addText("Placeholder")
                    text(contentId)
                    flow {
                        for (i in 0..10) {
                            MyButton(contentId, "Action " + ('A' + i), "new $i", 30f)
                        }
                    }
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun RcNoMacroDemo(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        AndroidxRcPlatformServices(),
        7,
        RemoteComposeWriter.HTag(
            Header.DOC_PROFILES,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        ),
    ) {
        root {
            column(Modifier.fillMaxSize(), horizontal = ColumnLayout.CENTER) {
                text("Macro Buttons Demo", fontSize = 60f, modifier = Modifier.padding(20))
                val contentId = addText("Placeholder")
                text(contentId)

                for (i in 0..10) {
                    MyNonMacroButton(contentId, "Action " + ('A' + i), 101 + i, 30f)
                }
            }
        }
    }
}

fun RemoteComposeContextAndroid.MyNonMacroButton(
    id: Int,
    label: String,
    data: Int,
    fontSize: Float,
) {
    box(
        Modifier.padding(16)
            .clip(RoundedRectShape(16f, 16f, 16f, 16f))
            .background(0xFFE0E0E0.toInt())
            .padding(24)
            .onClick(ValueStringChange(id, "Test $data"))
    ) {
        text(label, fontSize = fontSize, color = 0xFF333333.toInt())
    }
}

var MyButtonId: Int = -1

private fun RemoteComposeContextAndroid.MyButton(
    contentId: Int,
    label: String,
    data: String,
    fontSize: Float,
) {
    val label1 = textId(label)
    val data1 = textId(data)
    val fs1 = addFloat(fontSize)
    MyButtonId = this.textId("GlimmerButton")
    inflatePattern(MyButtonId, label1, data1, fs1, contentId) {}
}

private fun RemoteComposeContextAndroid.DefineMyButton() {
    val labelParam = definePatternParameter("label")
    val clickDataParam = definePatternParameter("clickData")
    val fontSizeParam = definePatternParameter("fontSize")
    val textTargetId = definePatternParameter("targetId")

    MyButtonId =
        definePattern("MyButtonRed", labelParam, clickDataParam, fontSizeParam, textTargetId) {
            box(
                Modifier.padding(16)
                    .clip(RoundedRectShape(16f, 16f, 16f, 16f))
                    // .background(0xFFE0E0E0.toInt())
                    .background(Color.RED)
                    .padding(24)
                    .onClick(ValueStringChange(textTargetId, clickDataParam))
            ) {
                text(labelParam, fontSize = floatId(fontSizeParam), color = 0xFF333333.toInt())
            }
        }
}

private fun RemoteComposeContextAndroid.DefineGlimmerButton() {
    val labelParam = definePatternParameter("label")
    val clickDataParam = definePatternParameter("clickData")
    val fontSizeParam = definePatternParameter("fontSize")
    val textTargetId = definePatternParameter("targetId")

    MyButtonId =
        definePattern("GlimmerButton", labelParam, clickDataParam, fontSizeParam, textTargetId) {
            val corner = 64f
            box(
                Modifier.padding(16)
                    .clip(RoundedRectShape(corner, corner, corner, corner))
                    // .background(0xFFE0E0E0.toInt())
                    .background(Color.argb(100, 255, 255, 255))
                    .padding(4)
                    .clip(RoundedRectShape(corner, corner, corner, corner))
                    .background(Color.argb(100, 100, 100, 100))
                    .padding(24)
                    .onClick(ValueStringChange(textTargetId, clickDataParam))
            ) {
                text(labelParam, fontSize = floatId(fontSizeParam), color = Color.WHITE)
            }
        }
}

@Preview
@Composable
private fun RcMacroDemoPreview() {
    RemoteDocumentPreview(RcMacroDemo())
}
