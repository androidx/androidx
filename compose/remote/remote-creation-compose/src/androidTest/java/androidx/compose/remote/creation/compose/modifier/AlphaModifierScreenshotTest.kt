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
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
class AlphaModifierScreenshotTest {
    @get:Rule
    val composeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    private val gridScreenshotUI = GridScreenshotUI()

    @Test
    fun grid() =
        composeTestRule.runScreenshotTest {
            val tests =
                listOf<Pair<String, @RemoteComposable @Composable () -> Unit>>(
                    "alpha(1.0f)" to
                        @RemoteComposable @Composable {
                            RemoteBox(
                                modifier = RemoteModifier.size(50.rdp).background(Color.Red)
                            ) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(20.rdp)
                                            .alpha(1.0f.rf)
                                            .background(Color.Blue)
                                )
                            }
                        },
                    "alpha(0.5f)" to
                        @RemoteComposable @Composable {
                            RemoteBox(
                                modifier = RemoteModifier.size(50.rdp).background(Color.Red)
                            ) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(20.rdp)
                                            .alpha(0.5f.rf)
                                            .background(Color.Blue)
                                )
                            }
                        },
                    "alpha(0.1f)" to
                        @RemoteComposable @Composable {
                            RemoteBox(
                                modifier = RemoteModifier.size(50.rdp).background(Color.Red)
                            ) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(20.rdp)
                                            .alpha(0.1f.rf)
                                            .background(Color.Blue)
                                )
                            }
                        },
                    "alpha(0f)" to
                        @RemoteComposable @Composable {
                            RemoteBox(
                                modifier = RemoteModifier.size(50.rdp).background(Color.Red)
                            ) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(20.rdp)
                                            .alpha(0f.rf)
                                            .background(Color.Blue)
                                )
                            }
                        },
                )

            gridScreenshotUI.GridContent(tests)
        }
}
