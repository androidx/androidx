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

import android.graphics.Color
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.managers.FitBoxLayout
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun RcFitBoxPreview() {
    RemoteDocPreview(RcFitBox())
}

@Suppress("RestrictedApiAndroidX")
fun RcFitBox(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        AndroidxRcPlatformServices(),
        7,
        RemoteComposeWriter.HTag(
            Header.DOC_PROFILES,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        ),
        RemoteComposeWriter.HTag(Header.FEATURE_MEASURE_VERSION, 2),
    ) {
        root {
            column(Modifier.fillMaxSize().background(Color.WHITE).padding(20)) {
                text("FitBox Demo: Resizes to fit children")
                // A FitBox that changes content based on its own size
                // We'll put it in a column and give it a width that we can imagine changing
                fitBox(
                    Modifier.fillMaxWidth().height(300).background(Color.LTGRAY),
                    FitBoxLayout.CENTER,
                    FitBoxLayout.CENTER,
                ) {
                    // Try to show large content first
                    column(Modifier.width(500).height(200).background(Color.RED).padding(10)) {
                        text("Large Content (500px wide)", color = Color.WHITE)
                        text("Only visible if space > 500px", color = Color.WHITE)
                    }

                    // Then medium content
                    column(Modifier.width(300).height(150).background(Color.GREEN).padding(10)) {
                        text("Medium Content (300px wide)")
                        text("Visible if space is between 300 and 500px")
                    }

                    // Finally small content
                    column(Modifier.width(150).height(100).background(Color.BLUE).padding(10)) {
                        text("Small Content", color = Color.WHITE)
                        text("Space < 300px", color = Color.WHITE)
                    }
                }

                box(Modifier.height(20))

                text("Another FitBox with vertical constraints")

                fitBox(
                    Modifier.width(400).fillMaxHeight(0.5f).background(Color.YELLOW),
                    FitBoxLayout.CENTER,
                    FitBoxLayout.CENTER,
                ) {
                    // Tall child
                    box(Modifier.size(200, 400).background(Color.MAGENTA)) {
                        text("Tall (400px)", color = Color.WHITE)
                    }
                    // Short child
                    box(Modifier.size(200, 100).background(Color.CYAN)) { text("Short (100px)") }
                }

                box(Modifier.height(20))

                text("FitBox using widthIn constraints")

                column(Modifier.fillMaxWidth()) {
                    fitBox(
                        Modifier.fillMaxWidth().height(150).background(Color.LTGRAY),
                        FitBoxLayout.CENTER,
                        FitBoxLayout.CENTER,
                    ) {
                        // This child defines its "preferred" size via constraints.
                        // It will only be picked if there's at least 250px.
                        box(
                            Modifier.horizontalWeight(1f)
                                .widthIn(200f, 200f)
                                .height(100)
                                .background(Color.RED)
                        ) {
                            text("Preferred 400-600px", color = Color.WHITE)
                        }

                        // Backup child
                        box(Modifier.width(200).height(80).background(Color.BLUE)) {
                            text("Fallback 200px", color = Color.WHITE)
                        }
                    }
                }
            }
        }
    }
}
