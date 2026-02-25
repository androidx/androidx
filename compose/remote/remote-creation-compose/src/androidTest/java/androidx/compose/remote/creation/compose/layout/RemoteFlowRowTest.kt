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

package androidx.compose.remote.creation.compose.layout

import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI
import androidx.compose.remote.creation.compose.test.util.propertyName
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
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
class RemoteFlowRowTest {
    @get:Rule
    val composeTestRule: RemoteComposeScreenshotTestRule by lazy {
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)
    }

    private val gridScreenshotUI = GridScreenshotUI()

    private val experimentalProfile =
        Profile(
            RcPlatformProfiles.ANDROIDX.apiLevel,
            RcPlatformProfiles.ANDROIDX.operationsProfiles or RcProfiles.PROFILE_EXPERIMENTAL,
            RcPlatformProfiles.ANDROIDX.platform,
            RcPlatformProfiles.ANDROIDX.profileFactory,
        )

    @Test
    fun grid() {
        composeTestRule.runScreenshotTest(profile = experimentalProfile) {
            val alignments =
                listOf(
                    RemoteArrangement.Start,
                    RemoteArrangement.CenterHorizontally,
                    RemoteArrangement.End,
                )
            val arrangements =
                listOf(RemoteArrangement.Top, RemoteArrangement.Center, RemoteArrangement.Bottom)

            gridScreenshotUI.GridContent(
                sequence {
                        for (arrangement in arrangements) {
                            for (alignment in alignments) {
                                yield(
                                    "${arrangement.propertyName()} ${alignment.propertyName()}" to
                                        @RemoteComposable @Composable {
                                            RemoteFlowRow(
                                                // TODO(b/487167164): change to fillMaxSize
                                                modifier = RemoteModifier.size(100.rdp),
                                                horizontalArrangement = alignment,
                                                verticalArrangement = arrangement,
                                            ) {
                                                RemoteBox(
                                                    modifier =
                                                        RemoteModifier.size(48.rdp)
                                                            .background(Color(0xFF6200EE))
                                                )
                                                RemoteBox(
                                                    modifier =
                                                        RemoteModifier.size(24.rdp)
                                                            .background(Color(0xFF03DAC6))
                                                )
                                            }
                                        }
                                )
                            }
                        }
                    }
                    .toList()
            )
        }
    }

    // TODO(b/487165969): merge wrap and wrapAlignedEnd into a grid with all combinations
    @Test
    fun wrap() {
        composeTestRule.runScreenshotTest(profile = experimentalProfile) {
            RemoteFlowRow(
                modifier = RemoteModifier.size(100.rdp).background(Color.LightGray),
                horizontalArrangement = RemoteArrangement.Start,
                verticalArrangement = RemoteArrangement.Top,
            ) {
                repeat(3) { index ->
                    val color = if (index % 2 == 0) Color(0xFF6200EE) else Color(0xFF03DAC6)
                    RemoteBox(modifier = RemoteModifier.size(40.rdp).background(color))
                }
            }
        }
    }

    // TODO(b/487165969): merge wrap and wrapAlignedEnd into a grid with all combinations
    @Test
    fun wrapAlignedEnd() {
        composeTestRule.runScreenshotTest(profile = experimentalProfile) {
            RemoteFlowRow(
                modifier = RemoteModifier.size(100.rdp).background(Color.LightGray),
                horizontalArrangement = RemoteArrangement.End,
                verticalArrangement = RemoteArrangement.Top,
            ) {
                repeat(3) { index ->
                    val color = if (index % 2 == 0) Color(0xFF6200EE) else Color(0xFF03DAC6)
                    RemoteBox(modifier = RemoteModifier.size(40.rdp).background(color))
                }
            }
        }
    }

    // b/487165969: should not include items that cross its boundaries
    @Test
    fun outOfBounds() {
        composeTestRule.runScreenshotTest(profile = experimentalProfile) {
            RemoteFlowRow(
                modifier = RemoteModifier.size(100.rdp).background(Color.LightGray),
                horizontalArrangement = RemoteArrangement.End,
                verticalArrangement = RemoteArrangement.Top,
            ) {
                repeat(5) { index ->
                    val color = if (index % 2 == 0) Color(0xFF6200EE) else Color(0xFF03DAC6)
                    RemoteBox(modifier = RemoteModifier.size(40.rdp).background(color))
                }
            }
        }
    }

    // b/487167164: not honouring vertical arrangement
    @Test
    fun fillMaxSize() {
        composeTestRule.runScreenshotTest(profile = experimentalProfile) {
            val alignments =
                listOf(
                    RemoteArrangement.Start,
                    RemoteArrangement.CenterHorizontally,
                    RemoteArrangement.End,
                )
            val arrangements =
                listOf(RemoteArrangement.Top, RemoteArrangement.Center, RemoteArrangement.Bottom)

            gridScreenshotUI.GridContent(
                sequence {
                        for (arrangement in arrangements) {
                            for (alignment in alignments) {
                                yield(
                                    "${arrangement.propertyName()} ${alignment.propertyName()}" to
                                        @RemoteComposable @Composable {
                                            RemoteFlowRow(
                                                modifier = RemoteModifier.fillMaxSize(),
                                                horizontalArrangement = alignment,
                                                verticalArrangement = arrangement,
                                            ) {
                                                RemoteBox(
                                                    modifier =
                                                        RemoteModifier.size(48.rdp)
                                                            .background(Color(0xFF6200EE))
                                                )
                                                RemoteBox(
                                                    modifier =
                                                        RemoteModifier.size(24.rdp)
                                                            .background(Color(0xFF03DAC6))
                                                )
                                            }
                                        }
                                )
                            }
                        }
                    }
                    .toList()
            )
        }
    }
}
