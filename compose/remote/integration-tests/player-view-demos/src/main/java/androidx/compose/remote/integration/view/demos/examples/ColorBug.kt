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

package androidx.compose.remote.integration.view.demos.examples

import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices

@Suppress("RestrictedApiAndroidX")
fun bugSim(): RemoteComposeWriter {
    val rc =
        RemoteComposeContextAndroid(
            width = 500,
            height = 500,
            contentDescription = "Simple Timer",
            apiLevel = 7,
            profiles = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            platform = AndroidxRcPlatformServices(),
        ) {
            val s = Seconds()

            val backgroundColor =
                writer.addThemedColor(
                    "color.system_accent2_50-",
                    0xffAAFFff.toInt(),
                    "color.system_accent2_800-",
                    0xff112233.toInt(),
                )
            val textcolor =
                writer.addThemedColor(
                    "color.system_neutral2_700-",
                    0xff005500.toInt(),
                    "color.system_accent2_80-0",
                    0xff00FF00.toInt(),
                )
            val number = createTextFromFloat(2f, 1, 0, Rc.TextFromFloat.OPTIONS_ROUNDING)
            root {
                column(Modifier.fillMaxSize().backgroundId(backgroundColor).padding(40)) {
                    text(
                        "\nBelow shoiuld be a 2 no decimal point",
                        Modifier.fillMaxWidth(),
                        textStyleId = 0,
                        textAlign = TextLayout.TEXT_ALIGN_CENTER,
                        colorId = textcolor.toInt(),
                        fontSize = 50f,
                        overflow = 3,
                    )
                    text(
                        number,
                        Modifier.fillMaxWidth(),
                        textStyleId = 0,
                        textAlign = TextLayout.TEXT_ALIGN_CENTER,
                        colorId = textcolor.toInt(),
                        fontSize = 100f,
                        overflow = 3,
                    )
                    text(
                        "Light Green in dark mode",
                        Modifier.fillMaxWidth(),
                        textStyleId = 0,
                        textAlign = TextLayout.TEXT_ALIGN_CENTER,
                        colorId = textcolor.toInt(),
                        fontSize = 50f,
                        overflow = 3,
                    )
                }
            }
        }
    return rc.writer
}

@Suppress("RestrictedApiAndroidX")
fun bugSim_og(): RemoteComposeWriter {
    val rc =
        RemoteComposeContextAndroid(
            width = 500,
            height = 500,
            contentDescription = "Simple Timer",
            apiLevel = 7,
            profiles = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            platform = AndroidxRcPlatformServices(),
        ) {
            val s = Seconds()

            val backgroundColor =
                writer.addThemedColor(
                    "color.system_accent2_50-",
                    0xffAAFFff.toInt(),
                    "color.system_accent2_800-",
                    0xff112233.toInt(),
                )
            val textcolor =
                writer.addThemedColor(
                    "color.system_neutral2_700-",
                    0xff005500.toInt(),
                    "color.system_accent2_80-0",
                    0xff00FF00.toInt(),
                )
            val number = createTextFromFloat(2f, 1, 0, Rc.TextFromFloat.PAD_PRE_NONE)
            root {
                column(Modifier.fillMaxSize().backgroundId(backgroundColor)) {
                    text(
                        "Bright   Green2",
                        Modifier.fillMaxWidth(),
                        textStyleId = 0,
                        textAlign = TextLayout.TEXT_ALIGN_CENTER,
                        colorId = textcolor.toInt(),
                        fontSize = 100f,
                        overflow = 3,
                    )
                }
            }
        }
    return rc.writer
}
