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
import androidx.compose.remote.creation.compose.layout.RemoteArrangement.Absolute
import androidx.compose.remote.creation.compose.layout.RemoteArrangement.spacedBy
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxHeight
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI
import androidx.compose.remote.creation.compose.test.util.propertyName
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
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

    private val horizontalArrangements =
        listOf(RemoteArrangement.Start, RemoteArrangement.Center, RemoteArrangement.End)
    private val verticalArrangements =
        listOf(RemoteArrangement.Top, RemoteArrangement.Center, RemoteArrangement.Bottom)

    @Test
    fun grid() {
        composeTestRule.runScreenshotTest(profile = experimentalProfile) {
            gridScreenshotUI.GridContent(getLayoutAlignmentUIs())
        }
    }

    @Test
    fun rtl() =
        composeTestRule.runScreenshotTest(
            layoutDirection = LayoutDirection.Rtl,
            profile = experimentalProfile,
        ) {
            gridScreenshotUI.GridContent(getLayoutAlignmentUIs())
        }

    @Test
    fun absoluteArrangement() =
        composeTestRule.runScreenshotTest(profile = experimentalProfile) {
            val horizontalArrangements = listOf(Absolute.Left, Absolute.Center, Absolute.Right)
            gridScreenshotUI.GridContent(getLayoutAlignmentUIs(horizontalArrangements))
        }

    @Test
    fun rtlAbsoluteArrangement() =
        composeTestRule.runScreenshotTest(
            layoutDirection = LayoutDirection.Rtl,
            profile = experimentalProfile,
        ) {
            val horizontalArrangements = listOf(Absolute.Left, Absolute.Center, Absolute.Right)
            gridScreenshotUI.GridContent(getLayoutAlignmentUIs(horizontalArrangements))
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
            gridScreenshotUI.GridContent(
                sequence {
                        for (verticalArrangement in verticalArrangements) {
                            for (horizontalArrangement in horizontalArrangements) {
                                yield(
                                    "${verticalArrangement.propertyName()} ${horizontalArrangement.propertyName()}" to
                                        @RemoteComposable @Composable {
                                            RemoteFlowRow(
                                                modifier = RemoteModifier.fillMaxSize(),
                                                horizontalArrangement = horizontalArrangement,
                                                verticalArrangement = verticalArrangement,
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

    @Test
    fun spacedBy() =
        composeTestRule.runScreenshotTest(profile = experimentalProfile) {
            gridScreenshotUI.GridContent(
                listOf(
                    "rdp Start" to { TestSpacedByRemoteDp(alignment = RemoteAlignment.Start) },
                    "rdp Center" to
                        {
                            TestSpacedByRemoteDp(alignment = RemoteAlignment.CenterHorizontally)
                        },
                    "rdp End" to { TestSpacedByRemoteDp(alignment = RemoteAlignment.End) },
                    "rdp Left" to
                        {
                            TestSpacedByRemoteDp(alignment = RemoteAbsoluteAlignment.Left)
                        },
                    "rdp" to { TestSpacedByRemoteDp() },
                    "rdp Right" to
                        {
                            TestSpacedByRemoteDp(alignment = RemoteAbsoluteAlignment.Right)
                        },
                    "rf Start" to { TestSpacedByRemoteFloat(alignment = RemoteAlignment.Start) },
                    "rf Center" to
                        {
                            TestSpacedByRemoteFloat(alignment = RemoteAlignment.CenterHorizontally)
                        },
                    "rf End" to { TestSpacedByRemoteFloat(alignment = RemoteAlignment.End) },
                    "rf Left" to
                        {
                            TestSpacedByRemoteFloat(alignment = RemoteAbsoluteAlignment.Left)
                        },
                    "rf" to { TestSpacedByRemoteFloat() },
                    "rf Right" to
                        {
                            TestSpacedByRemoteFloat(alignment = RemoteAbsoluteAlignment.Right)
                        },
                )
            )
        }

    @Test
    fun spacedByRtl() =
        composeTestRule.runScreenshotTest(
            layoutDirection = LayoutDirection.Rtl,
            profile = experimentalProfile,
        ) {
            gridScreenshotUI.GridContent(
                listOf(
                    "rdp Start" to { TestSpacedByRemoteDp(alignment = RemoteAlignment.Start) },
                    "rdp Center" to
                        {
                            TestSpacedByRemoteDp(alignment = RemoteAlignment.CenterHorizontally)
                        },
                    "rdp End" to { TestSpacedByRemoteDp(alignment = RemoteAlignment.End) },
                    "rdp Left" to
                        {
                            TestSpacedByRemoteDp(alignment = RemoteAbsoluteAlignment.Left)
                        },
                    "rdp" to { TestSpacedByRemoteDp() },
                    "rdp Right" to
                        {
                            TestSpacedByRemoteDp(alignment = RemoteAbsoluteAlignment.Right)
                        },
                    "rf Start" to { TestSpacedByRemoteFloat(alignment = RemoteAlignment.Start) },
                    "rf Center" to
                        {
                            TestSpacedByRemoteFloat(alignment = RemoteAlignment.CenterHorizontally)
                        },
                    "rf End" to { TestSpacedByRemoteFloat(alignment = RemoteAlignment.End) },
                    "rf Left" to
                        {
                            TestSpacedByRemoteFloat(alignment = RemoteAbsoluteAlignment.Left)
                        },
                    "rf" to { TestSpacedByRemoteFloat() },
                    "rf Right" to
                        {
                            TestSpacedByRemoteFloat(alignment = RemoteAbsoluteAlignment.Right)
                        },
                )
            )
        }

    private fun getLayoutAlignmentUIs(
        horizontalArrangements: List<RemoteArrangement.Horizontal> = this.horizontalArrangements
    ): List<Pair<String, @RemoteComposable @Composable () -> Unit>> =
        sequence {
                for (verticalArrangement in verticalArrangements) {
                    for (horizontalArrangement in horizontalArrangements) {
                        yield(
                            "${verticalArrangement.propertyName()} ${horizontalArrangement.propertyName()}" to
                                @RemoteComposable @Composable {
                                    RemoteFlowRow(
                                        // TODO(b/487167164): change to fillMaxSize
                                        modifier = RemoteModifier.size(100.rdp),
                                        horizontalArrangement = horizontalArrangement,
                                        verticalArrangement = verticalArrangement,
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

    @RemoteComposable
    @Composable
    private fun TestSpacedByRemoteDp() {
        RemoteFlowRow(
            // TODO(b/487167164): change to fillMaxSize
            modifier = RemoteModifier.size(100.rdp),
            horizontalArrangement = spacedBy(5.rdp),
            verticalArrangement = RemoteArrangement.Top,
        ) {
            repeat(3) { index ->
                val color = if (index % 2 == 0) Color(0xFF6200EE) else Color(0xFF03DAC6)
                RemoteBox(modifier = RemoteModifier.width(20.rdp).fillMaxHeight().background(color))
            }
        }
    }

    @RemoteComposable
    @Composable
    private fun TestSpacedByRemoteFloat() {
        RemoteFlowRow(
            // TODO(b/487167164): change to fillMaxSize
            modifier = RemoteModifier.size(100.rdp),
            horizontalArrangement = spacedBy(10f.rf),
            verticalArrangement = RemoteArrangement.Top,
        ) {
            repeat(3) { index ->
                val color = if (index % 2 == 0) Color(0xFF6200EE) else Color(0xFF03DAC6)
                RemoteBox(modifier = RemoteModifier.width(20.rdp).fillMaxHeight().background(color))
            }
        }
    }

    @RemoteComposable
    @Composable
    private fun TestSpacedByRemoteDp(alignment: RemoteAlignment.Horizontal) {
        RemoteFlowRow(
            // TODO(b/487167164): change to fillMaxSize
            modifier = RemoteModifier.size(100.rdp),
            horizontalArrangement = spacedBy(space = 5.rdp, alignment = alignment),
            verticalArrangement = RemoteArrangement.Top,
        ) {
            repeat(3) { index ->
                val color = if (index % 2 == 0) Color(0xFF6200EE) else Color(0xFF03DAC6)
                RemoteBox(modifier = RemoteModifier.width(20.rdp).fillMaxHeight().background(color))
            }
        }
    }

    @RemoteComposable
    @Composable
    private fun TestSpacedByRemoteFloat(alignment: RemoteAlignment.Horizontal) {
        RemoteFlowRow(
            // TODO(b/487167164): change to fillMaxSize
            modifier = RemoteModifier.size(100.rdp),
            horizontalArrangement = spacedBy(space = 10f.rf, alignment = alignment),
            verticalArrangement = RemoteArrangement.Top,
        ) {
            repeat(3) { index ->
                val color = if (index % 2 == 0) Color(0xFF6200EE) else Color(0xFF03DAC6)
                RemoteBox(modifier = RemoteModifier.width(20.rdp).fillMaxHeight().background(color))
            }
        }
    }

    // b/489510192: not drawing correctly when wrapping and vertical arrangement is Center or Bottom
    @Test
    fun wrapAndVerticalArrangement() =
        composeTestRule.runScreenshotTest(profile = experimentalProfile) {
            gridScreenshotUI.GridContent(
                listOf(
                    "Top" to { TestVerticalArrangementWrap(4, RemoteArrangement.Top) },
                    "Center" to { TestVerticalArrangementWrap(4, RemoteArrangement.Center) },
                    "Bottom" to { TestVerticalArrangementWrap(4, RemoteArrangement.Bottom) },
                    "Top no wrap" to { TestVerticalArrangementWrap(3, RemoteArrangement.Top) },
                    "Center no wrap" to
                        {
                            TestVerticalArrangementWrap(3, RemoteArrangement.Center)
                        },
                    "Bottom no wrap" to { TestVerticalArrangementWrap(3, RemoteArrangement.Bottom) },
                )
            )
        }

    @RemoteComposable
    @Composable
    private fun TestVerticalArrangementWrap(
        size: Int,
        verticalArrangement: RemoteArrangement.Vertical,
    ) {
        RemoteFlowRow(
            // TODO(b/487167164): change to fillMaxSize
            modifier = RemoteModifier.size(100.rdp),
            horizontalArrangement = RemoteArrangement.Start,
            verticalArrangement = verticalArrangement,
        ) {
            repeat(size) { index ->
                val color = if (index % 2 == 0) Color(0xFF6200EE) else Color(0xFF03DAC6)
                RemoteBox(modifier = RemoteModifier.size(30.rdp).background(color))
            }
        }
    }
}
