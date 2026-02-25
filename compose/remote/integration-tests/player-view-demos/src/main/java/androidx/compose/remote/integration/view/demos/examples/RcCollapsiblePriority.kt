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
import androidx.compose.remote.core.operations.layout.managers.CollapsiblePriority
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun RcCollapsiblePriorityPreview() {
    RemoteDocPreview(RcCollapsiblePriority())
}

@Suppress("RestrictedApiAndroidX")
fun RcCollapsiblePriority(): RemoteComposeContext {
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
                text("Collapsible Row with Priorities", fontSize = 40f)
                text("Highest priority number stays longest", fontSize = 30f)

                // A horizontal line to show the available space
                box(Modifier.fillMaxWidth().height(2).background(Color.LTGRAY))

                collapsibleRow(
                    Modifier.fillMaxWidth().background(Color.DKGRAY).padding(10),
                    horizontal = RowLayout.START,
                    vertical = RowLayout.CENTER,
                ) {
                    // P10: lower number, should disappear first
                    box(
                        Modifier.size(200, 100)
                            .background(Color.RED)
                            .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 10f)
                    ) {
                        text("P10", color = Color.WHITE)
                    }
                    // P100: highest number, should stay last
                    box(
                        Modifier.size(200, 100)
                            .background(Color.GREEN)
                            .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 100f)
                    ) {
                        text("P100", color = Color.WHITE)
                    }
                    // P50: middle number, should disappear second
                    box(
                        Modifier.size(200, 100)
                            .background(Color.BLUE)
                            .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 50f)
                    ) {
                        text("P50", color = Color.WHITE)
                    }
                }

                collapsibleRow(
                    Modifier.fillMaxWidth().background(Color.DKGRAY).padding(10),
                    horizontal = RowLayout.START,
                    vertical = RowLayout.CENTER,
                ) {
                    // P50: middle number, should disappear second
                    box(
                        Modifier.size(200, 100)
                            .background(Color.BLUE)
                            .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 50f)
                    ) {
                        text("P50", color = Color.WHITE)
                    }
                    // P100: highest number, should stay last
                    box(
                        Modifier.size(200, 100)
                            .background(Color.GREEN)
                            .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 100f)
                    ) {
                        text("P100", color = Color.WHITE)
                    }
                    // P10: lower number, should disappear first
                    box(
                        Modifier.size(200, 100)
                            .background(Color.RED)
                            .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 10f)
                    ) {
                        text("P10", color = Color.WHITE)
                    }
                }

                box(Modifier.height(40))
                text("Priority vs No Priority", fontSize = 30f)
                text("No priority set (MAX_VALUE) stays longest", fontSize = 24f)

                collapsibleRow(
                    Modifier.fillMaxWidth().background(Color.DKGRAY).padding(10),
                    horizontal = RowLayout.START,
                    vertical = RowLayout.CENTER,
                ) {
                    // No Priority: MAX_VALUE, should stay longest
                    box(Modifier.size(200, 100).background(Color.GRAY)) {
                        text("NoP", color = Color.WHITE)
                    }
                    // P10: lower number, should disappear first
                    box(
                        Modifier.size(200, 100)
                            .background(Color.GREEN)
                            .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 10f)
                    ) {
                        text("P10", color = Color.WHITE)
                    }
                }

                collapsibleRow(
                    Modifier.fillMaxWidth().background(Color.DKGRAY).padding(10),
                    horizontal = RowLayout.START,
                    vertical = RowLayout.CENTER,
                ) {
                    // P10: lower number, should disappear first
                    box(
                        Modifier.size(200, 100)
                            .background(Color.GREEN)
                            .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 10f)
                    ) {
                        text("P10", color = Color.WHITE)
                    }
                    // No Priority: MAX_VALUE, should stay longest
                    box(Modifier.size(200, 100).background(Color.GRAY)) {
                        text("NoP", color = Color.WHITE)
                    }
                }

                box(Modifier.height(40))
                text("Respecting Priority order", fontSize = 30f)
                text("Lower priority numbers disappear even if space available", fontSize = 24f)

                collapsibleRow(
                    Modifier.fillMaxWidth().background(Color.DKGRAY).padding(10),
                    horizontal = RowLayout.START,
                    vertical = RowLayout.CENTER,
                ) {
                    // P10: lower number, should disappear first
                    box(
                        Modifier.size(200, 100)
                            .background(Color.RED)
                            .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 10f)
                    ) {
                        text("P10", color = Color.WHITE)
                    }
                    // P100: highest number, should stay last
                    box(
                        Modifier.size(200, 100)
                            .background(Color.GREEN)
                            .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 100f)
                    ) {
                        text("P100", color = Color.WHITE)
                    }
                    // P50: middle number, should disappear second
                    box(
                        Modifier.size(100, 100)
                            .background(Color.BLUE)
                            .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 50f)
                    ) {
                        text("P50", color = Color.WHITE)
                    }
                }

                box(Modifier.height(40))
                text("Mixed priorities", fontSize = 30f)
                text("Lower priority numbers disappear first", fontSize = 24f)

                collapsibleRow(
                    Modifier.fillMaxWidth().background(Color.DKGRAY).padding(10),
                    horizontal = RowLayout.START,
                    vertical = RowLayout.CENTER,
                ) {
                    // P51: should stay long
                    box(
                        Modifier.size(80, 100)
                            .background(Color.CYAN)
                            .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 51f)
                    ) {
                        text("P51", color = Color.WHITE)
                    }
                    // P11: should disappear early
                    box(
                        Modifier.size(80, 100)
                            .background(Color.RED)
                            .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 11f)
                    ) {
                        text("P11", color = Color.WHITE)
                    }
                    // P52: should stay long
                    box(
                        Modifier.size(80, 100)
                            .background(Color.CYAN)
                            .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 52f)
                    ) {
                        text("P52", color = Color.WHITE)
                    }
                    // P12: should disappear early
                    box(
                        Modifier.size(80, 100)
                            .background(Color.RED)
                            .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 12f)
                    ) {
                        text("P12", color = Color.WHITE)
                    }
                    // P50: middle number, should stay long
                    box(
                        Modifier.size(80, 100)
                            .background(Color.CYAN)
                            .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 50f)
                    ) {
                        text("P50", color = Color.WHITE)
                    }
                    // P10: lowest number, should disappear first
                    box(
                        Modifier.size(80, 100)
                            .background(Color.RED)
                            .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 10f)
                    ) {
                        text("P10", color = Color.WHITE)
                    }
                }
            }
        }
    }
}
