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

package androidx.compose.remote.creation.compose.modifier

import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class PaddingModifierTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    private val gridScreenshotUI = GridScreenshotUI()

    @Test
    fun grid() =
        remoteComposeTestRule.runScreenshotTest {
            gridScreenshotUI.GridContent(
                listOf(
                    "padding start" to
                        @RemoteComposable @Composable {
                            PaddingItem(RemoteModifier.padding(start = 20.rdp))
                        },
                    "padding end" to
                        @RemoteComposable @Composable {
                            PaddingItem(RemoteModifier.padding(end = 20.rdp))
                        },
                    "padding all" to
                        @RemoteComposable @Composable {
                            PaddingItem(RemoteModifier.padding(20.rdp))
                        },
                    "padding top" to
                        @RemoteComposable @Composable {
                            PaddingItem(RemoteModifier.padding(top = 20.rdp))
                        },
                    "padding bottom" to
                        @RemoteComposable @Composable {
                            PaddingItem(RemoteModifier.padding(bottom = 20.rdp))
                        },
                    "Blank" to { Blank() },
                    "padding vertical" to
                        @RemoteComposable @Composable {
                            PaddingItem(RemoteModifier.padding(vertical = 20.rdp))
                        },
                    "padding horizontal" to
                        @RemoteComposable @Composable {
                            PaddingItem(RemoteModifier.padding(horizontal = 20.rdp))
                        },
                    "Blank" to { Blank() },
                    "padding start rf" to
                        @RemoteComposable @Composable {
                            PaddingItem(RemoteModifier.padding(start = 20f.rf))
                        },
                    "padding end rf" to
                        @RemoteComposable @Composable {
                            PaddingItem(RemoteModifier.padding(end = 20f.rf))
                        },
                    "padding all rf" to
                        @RemoteComposable @Composable {
                            PaddingItem(RemoteModifier.padding(20f.rf))
                        },
                    "padding top rf" to
                        @RemoteComposable @Composable {
                            PaddingItem(RemoteModifier.padding(top = 20f.rf))
                        },
                    "padding bottom rf" to
                        @RemoteComposable @Composable {
                            PaddingItem(RemoteModifier.padding(bottom = 20f.rf))
                        },
                    "Blank" to { Blank() },
                    "padding vertical rf" to
                        @RemoteComposable @Composable {
                            PaddingItem(RemoteModifier.padding(vertical = 20.rf))
                        },
                    "padding horizontal rf" to
                        @RemoteComposable @Composable {
                            PaddingItem(RemoteModifier.padding(horizontal = 20.rf))
                        },
                )
            )
        }

    @Test
    fun rtl() =
        // b(/500955051): should not pass layoutDirection runScreenshotTest only to GridContent
        remoteComposeTestRule.runScreenshotTest(
            creationComposableWrapper = ComposableWrappers.rtl
        ) {
            gridScreenshotUI.GridContent(
                listOf(
                    "padding start" to
                        @RemoteComposable @Composable {
                            PaddingItem(RemoteModifier.padding(start = 20.rdp))
                        },
                    "padding end" to
                        @RemoteComposable @Composable {
                            PaddingItem(RemoteModifier.padding(end = 20.rdp))
                        },
                    "Blank" to { Blank() },
                    "padding start rf" to
                        @RemoteComposable @Composable {
                            PaddingItem(RemoteModifier.padding(start = 20f.rf))
                        },
                    "padding end rf" to
                        @RemoteComposable @Composable {
                            PaddingItem(RemoteModifier.padding(end = 20f.rf))
                        },
                ),
                layoutDirection = LayoutDirection.Rtl,
            )
        }

    @RemoteComposable
    @Composable
    private fun PaddingItem(padding: RemoteModifier) {
        RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.Red)) {
            RemoteBox(modifier = RemoteModifier.fillMaxSize().then(padding).background(Color.Blue))
        }
    }

    @RemoteComposable @Composable private fun Blank() {}
}
