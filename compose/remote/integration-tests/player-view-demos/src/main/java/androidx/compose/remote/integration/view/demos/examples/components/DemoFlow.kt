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

/** Atomic demo for the Flow component. Demonstrates children wrapping to the next line. */
@Suppress("RestrictedApiAndroidX")
fun DemoFlow(): RemoteComposeWriter {
    return RemoteComposeContextAndroid(
            width = 500,
            height = 500,
            contentDescription = "DemoFlow",
            apiLevel = 7,
            profiles = RcProfiles.PROFILE_EXPERIMENTAL or RcProfiles.PROFILE_ANDROIDX,
            platform = AndroidxRcPlatformServices(),
        ) {
            root {
                flow(Modifier.fillMaxWidth().background(Color.LTGRAY)) {
                    for (i in 1..10) {
                        box(Modifier.size(60).padding(5).background(Color.RED))
                    }
                }
            }
        }
        .writer
}
