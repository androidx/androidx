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
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices

@Suppress("RestrictedApiAndroidX")
fun RcTextDemo(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        600,
        600,
        "Demo",
        7,
        RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        AndroidxRcPlatformServices(),
    ) {
        root {
            box(Modifier.fillMaxSize().background(Color.YELLOW)) {
                row(
                    Modifier.fillMaxSize(),
                    horizontal = RowLayout.SPACE_EVENLY,
                    vertical = RowLayout.TOP,
                ) {
                    text("Hello", modifier = Modifier.alignByBaseline())
                    text("World", modifier = Modifier.alignByBaseline(), fontSize = 100f)
                    text("the", modifier = Modifier.alignByBaseline(), fontSize = 12f)
                    text("quick", modifier = Modifier.alignByBaseline(), fontSize = 64f)
                    text("brown", modifier = Modifier.alignByBaseline(), fontSize = 72f)
                    text("fox", modifier = Modifier.alignByBaseline())
                }
                row(
                    Modifier.fillMaxSize(),
                    horizontal = RowLayout.SPACE_EVENLY,
                    vertical = RowLayout.CENTER,
                ) {
                    text("Hello", modifier = Modifier.alignByBaseline())
                    text("World", modifier = Modifier.alignByBaseline(), fontSize = 100f)
                    text("the", modifier = Modifier.alignByBaseline(), fontSize = 12f)
                    text("quick", modifier = Modifier.alignByBaseline(), fontSize = 64f)
                    text("brown", modifier = Modifier.alignByBaseline(), fontSize = 72f)
                    text("fox", modifier = Modifier.alignByBaseline())
                }
                row(
                    Modifier.fillMaxSize(),
                    horizontal = RowLayout.SPACE_EVENLY,
                    vertical = RowLayout.BOTTOM,
                ) {
                    text("Hello", modifier = Modifier.alignByBaseline())
                    text("World", modifier = Modifier.alignByBaseline(), fontSize = 100f)
                    text("the", modifier = Modifier.alignByBaseline(), fontSize = 12f)
                    text("quick", modifier = Modifier.alignByBaseline(), fontSize = 64f)
                    text("brown", modifier = Modifier.alignByBaseline(), fontSize = 72f)
                    text("fox", modifier = Modifier.alignByBaseline())
                }
            }
        }
    }
}
