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
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices

@Suppress("RestrictedApiAndroidX")
fun theme1(): RemoteComposeWriter {
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

            val color1 =
                writer.addThemedColor(
                    "color.system_neutral1_10",
                    0xFF00FF00.toInt(),
                    "color.system_neutral1_800",
                    0xFFFF0000.toInt(),
                )
            val backgroundColor =
                writer.addThemedColor(
                    "color.system_neutral1_900",
                    0xFFFFCCAAL.toInt(),
                    "color.system_neutral1_0",
                    0xFF553322L.toInt(),
                )
            root {
                column(Modifier.backgroundId(backgroundColor).fillMaxSize().spacedBy(12f)) {
                    box(Modifier.backgroundId(color1).width(200).height(200))
                    text("hello", colorId = color1.toInt(), fontSize = 100f)
                }
            }
        }
    return rc.writer
}

@Suppress("RestrictedApiAndroidX")
fun theme2(): RemoteComposeWriter {
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

            val color1 =
                writer.addThemedColor(
                    "color.system_neutral1_10",
                    0xFF00FF00.toInt(),
                    "color.system_neutral1_800",
                    0xFFFF0000.toInt(),
                )
            val backgroundColor =
                writer.addThemedColor(
                    "color.system_neutral1_900",
                    0xFFFFCCAAL.toInt(),
                    "color.system_neutral1_0",
                    0xFF553322L.toInt(),
                )
            root {
                column(Modifier.backgroundId(backgroundColor).fillMaxSize().spacedBy(12f)) {
                    box(Modifier.backgroundId(color1).width(200).height(200))
                    text("hello", colorId = color1.toInt(), fontSize = 100f)
                }
            }
        }
    return rc.writer
}
