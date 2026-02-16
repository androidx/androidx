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

import android.graphics.fonts.Font
import android.graphics.fonts.SystemFonts
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.RFloat
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Suppress("RestrictedApiAndroidX")
fun fontList(): RemoteComposeWriter {
    val rc =
        RemoteComposeContextAndroid(
            width = 500,
            height = 500,
            contentDescription = "Simple Timer",
            apiLevel = 7,
            profiles = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            platform = AndroidxRcPlatformServices(),
        ) {
            root {
                box(
                    RecordingModifier().background(0xFFAAAAAA.toInt()).fillMaxSize(),
                    BoxLayout.START,
                    BoxLayout.START,
                ) {
                    val fonts = getAllFonts()

                    canvas(RecordingModifier().fillMaxWidth().height(700f)) {
                        val x = ComponentWidth() / 2f

                        val gap = 100f
                        val strs =
                            arrayOf(
                                "DancingScript-Regular",
                                "DancingScript",
                                "DaxxxScript-Regular",
                                "RobotoFlex",
                                "Google-Sans",
                                "DroidSansMono",
                                "ComingSoon",
                                "ArroisGothicSC",
                                "NotoSerifCJK-Regular",
                            )
                        val subset = fonts.asList().shuffled().take(2).toTypedArray()
                        var y = ComponentHeight() / 10f + getScroll(gap * (strs.size + subset.size))

                        strs.forEach { str ->
                            write(str, x, y)
                            y += gap
                        }

                        subset.forEach { font ->
                            val str =
                                font.file?.name?.substring(0, font.file?.name?.indexOf('.') ?: 0)
                            print(str)
                            if (str != null) {
                                write(str, x, y)
                            }
                            y += gap
                        }
                    }
                }
            }
        }

    return rc.writer
}

@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContext.getScroll(max: Float): RFloat {
    val h = ComponentHeight() - max
    val touch =
        addTouch(
            0f,
            -1000f,
            0f,
            Rc.Touch.STOP_GENTLY,
            0f,
            0,
            null,
            writer.easing(0.5f, 10f, 0.1f),
            Rc.Touch.POSITION_Y,
        )
    addDebugMessage("scroll ", touch)
    return RFloat(touch, writer)
}

@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.write(str: String, x: Number, y: Number) {
    painter
        .setTypeface(addText(str), 400, false)
        .setFallbackTypeFace(1, 100, true)
        .setTextSize(70f)
        .commit()
    val id = addText(str)
    drawTextAnchored(id, x, y, 0, 0, 0)
}

private fun getAllFonts(): Array<Font> {
    val fList = SystemFonts.getAvailableFonts().toTypedArray()
    fList.forEach { font -> println(font.file?.name) }
    return fList
}

@Preview @Composable private fun FontListPreview() = RemoteDocPreview(fontList())
