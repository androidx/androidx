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
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlin.collections.ArrayList

/**
 * ===================================
 * A demo of a custom Scroller that works with RefreshBugKt::dynamicPaging To achieve paging like
 * behavior
 */
@Suppress("RestrictedApiAndroidX")
fun themeList(): RemoteComposeWriter {
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
                val backgroundId =
                    writer.addThemedColor(
                        Rc.AndroidColors.GROUP,
                        Rc.AndroidColors.SYSTEM_ACCENT2_50,
                        Rc.AndroidColors.SYSTEM_ACCENT2_800,
                        0xFFFFFFFF.toInt(),
                        0xFF000000.toInt(),
                    )
                val textColorId =
                    writer.addThemedColor(
                        Rc.AndroidColors.GROUP,
                        Rc.AndroidColors.SYSTEM_ON_SURFACE_LIGHT,
                        Rc.AndroidColors.SYSTEM_ON_SURFACE_DARK,
                        0xFF000000.toInt(),
                        0xFFFFFFFF.toInt(),
                    )
                val light = getColorAttribute(textColorId.toInt(), Rc.ColorAttribute.BRIGHTNESS)
                addDebugMessage(" text  ", light)

                box(
                    RecordingModifier().backgroundId(backgroundId).fillMaxSize(),
                    BoxLayout.START,
                    BoxLayout.START,
                ) {
                    column(RecordingModifier().fillMaxSize().verticalScroll()) {
                        makeThemeRows(backgroundId, textColorId)
                    }
                }
            }
        }
    return rc.writer
}

@Preview @Composable private fun ThemeListPreview() = RemoteDocPreview(themeList())

@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.makeThemeRows(bg: Short, fg: Short) {
    val names = ArrayList<String?>(150)
    val cids = makeThemes(names)
    box(RecordingModifier().background(0xFF000000.toInt()).fillMaxWidth().height(4)) {}
    var pad = 0
    for (i in 0 until cids.size) {
        val c = cids[i]
        val name = names[i]
        row(RecordingModifier().padding(pad, 0, 4, 0).backgroundId(bg).fillMaxWidth()) {
            val dim = 48f

            val colorId =
                writer.addThemedColor(
                    Rc.AndroidColors.GROUP,
                    c.toShort(),
                    c.toShort(),
                    0xFFFFFFFF.toInt(),
                    0xFFFFFFFF.toInt(),
                )
            box(
                RecordingModifier().padding(8, 0, 8, 0).backgroundId(colorId).width(dim).height(dim)
            ) {}
            text("" + name, RecordingModifier(), fontSize = dim, colorId = fg.toInt())

            // box(RecordingModifier().padding(8,0,8,0).backgroundId(c).horizontalWeight(1f).height(dim))

        }

        pad = 4
    }
}

@Suppress("RestrictedApiAndroidX")
private fun makeThemes(list: ArrayList<String?>): IntArray {
    val c: Class<*> = Rc.AndroidColors::class.java
    val f = c.declaredFields

    val value = IntArray(f.indices.last)
    var count = 0
    for (i in f.indices) {
        val field = f[i]
        if (field.type != Short::class.javaPrimitiveType) {
            continue
        }
        list.add(field.getName())
        value[count] = field.getInt(null)
        count++
    }
    return value
}
