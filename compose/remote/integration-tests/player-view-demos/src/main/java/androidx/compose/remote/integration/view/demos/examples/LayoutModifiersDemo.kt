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
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.RFloat
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.computeMeasure
import androidx.compose.remote.creation.computePosition
import androidx.compose.remote.creation.cos
import androidx.compose.remote.creation.min
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.sin
import androidx.compose.remote.creation.toRad
import androidx.compose.remote.tooling.preview.RemoteDocPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Suppress("RestrictedApiAndroidX")
fun LayoutModifierDemo1(): RemoteComposeContext {
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
                box(
                    Modifier.background(Color.RED)
                        .padding(8)
                        .computeMeasure { height = width }
                        .computePosition {
                            x = (parentWidth - width) / 2f
                            y = (parentHeight - height) / 2f
                        },
                    horizontal = BoxLayout.CENTER,
                    vertical = BoxLayout.CENTER,
                ) {
                    text("Hello World", Modifier.background(Color.GREEN), fontSize = 100f)
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun LayoutModifierDemo2(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        600,
        600,
        "Demo",
        7,
        RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        AndroidxRcPlatformServices(),
    ) {
        root {
            box(
                Modifier.fillMaxSize()
                    .background(Color.YELLOW)
                    .widthIn(16f * 8, Float.MAX_VALUE)
                    .heightIn(16f * 8, Float.MAX_VALUE)
            ) {
                val size = 60f
                for (i in 0 until 12) {
                    box(
                        Modifier.background(Color.RED)
                            .widthIn(16f, Float.MAX_VALUE)
                            .heightIn(16f, Float.MAX_VALUE)
                            .computePosition {
                                val angle = toRad(RFloat((i * 360f / 12f)) + ContinuousSec() * 36f)
                                val r = min(parentWidth, parentHeight) * 0.3f
                                x = (parentWidth - size) / 2f + r * cos(angle)
                                y = (parentHeight - size) / 2f + r * sin(angle)
                            },
                        horizontal = BoxLayout.CENTER,
                        vertical = BoxLayout.CENTER,
                    ) {
                        text("${i + 1}", Modifier.background(Color.GREEN))
                    }
                }
            }
        }
    }
}

@Preview @Composable fun LayoutModifierDemo1Preview() = RemoteDocPreview(LayoutModifierDemo1())
