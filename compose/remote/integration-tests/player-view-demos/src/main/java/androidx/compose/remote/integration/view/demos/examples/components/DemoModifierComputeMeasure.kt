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
import androidx.compose.remote.creation.RFloat
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.computeMeasure
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices

/**
 * Atomic demo for the computeMeasure modifier. Demonstrates custom measurement logic (e.g.,
 * maintaining aspect ratio).
 */
@Suppress("RestrictedApiAndroidX")
fun DemoModifierComputeMeasure(): RemoteComposeWriter {
    return RemoteComposeContextAndroid(
            width = 500,
            height = 500,
            contentDescription = "DemoModifierComputeMeasure",
            apiLevel = 7,
            profiles = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            platform = AndroidxRcPlatformServices(),
        ) {
            root {
                box(
                    Modifier.background(Color.RED)
                        .computeMeasure {
                            // Dynamically force height to be 1.5x width
                            height = (width as RFloat) * 1.5f
                        }
                        .width(100)
                )
            }
        }
        .writer
}
