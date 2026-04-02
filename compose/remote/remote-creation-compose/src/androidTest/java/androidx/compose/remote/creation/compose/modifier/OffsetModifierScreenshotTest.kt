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
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class OffsetModifierScreenshotTest {
    @get:Rule
    val composeTestRule: RemoteComposeScreenshotTestRule by lazy {
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)
    }

    private val gridScreenshotUI = GridScreenshotUI()

    @Test
    fun grid() =
        composeTestRule.runScreenshotTest {
            val tests =
                listOf<Pair<String, @RemoteComposable @Composable () -> Unit>>(
                    "offset(0, 0)" to
                        @RemoteComposable @Composable {
                            RemoteBox(
                                modifier = RemoteModifier.size(50.rdp).background(Color.Red)
                            ) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(20.rdp)
                                            .offset(0.rdp, 0.rdp)
                                            .background(Color.Blue)
                                )
                            }
                        },
                    "offset(15, 0)" to
                        @RemoteComposable @Composable {
                            RemoteBox(
                                modifier = RemoteModifier.size(50.rdp).background(Color.Red)
                            ) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(20.rdp)
                                            .offset(15.rdp, 0.rdp)
                                            .background(Color.Blue)
                                )
                            }
                        },
                    "offset(0, 15)" to
                        @RemoteComposable @Composable {
                            RemoteBox(
                                modifier = RemoteModifier.size(50.rdp).background(Color.Red)
                            ) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(20.rdp)
                                            .offset(0.rdp, 15.rdp)
                                            .background(Color.Blue)
                                )
                            }
                        },
                    "offset(15, 15)" to
                        @RemoteComposable @Composable {
                            RemoteBox(
                                modifier = RemoteModifier.size(50.rdp).background(Color.Red)
                            ) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(20.rdp)
                                            .offset(15.rdp, 15.rdp)
                                            .background(Color.Blue)
                                )
                            }
                        },
                    "offset(-10, -10)" to
                        @RemoteComposable @Composable {
                            RemoteBox(
                                modifier =
                                    RemoteModifier.padding(20.rdp)
                                        .size(50.rdp)
                                        .background(Color.Red)
                            ) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(20.rdp)
                                            .offset((-10).rdp, (-10).rdp)
                                            .background(Color.Blue)
                                )
                            }
                        },
                )

            gridScreenshotUI.GridContent(tests)
        }
}
