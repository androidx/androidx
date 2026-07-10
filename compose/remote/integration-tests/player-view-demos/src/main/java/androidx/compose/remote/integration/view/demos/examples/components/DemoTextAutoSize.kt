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

package androidx.compose.remote.integration.view.demos.examples.components

import android.graphics.Color
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices

/**
 * Atomic demo for the Text component's auto-sizing feature. Demonstrates text scaling to fit a
 * fixed container.
 */
@Suppress("RestrictedApiAndroidX")
fun DemoTextAutoSize(): RemoteComposeWriter {
    return RemoteComposeContextAndroid(
            width = 500,
            height = 500,
            contentDescription = "DemoTextAutoSize",
            apiLevel = 7,
            profiles = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            platform = AndroidxRcPlatformServices(),
        ) {
            root {
                box(Modifier.size(200, 50).background(Color.LTGRAY)) {
                    text(
                        "This text scales to fit",
                        modifier = Modifier.fillMaxSize(),
                        autosize = true,
                        minFontSize = 10f,
                        maxFontSize = 100f,
                    )
                }
            }
        }
        .writer
}
