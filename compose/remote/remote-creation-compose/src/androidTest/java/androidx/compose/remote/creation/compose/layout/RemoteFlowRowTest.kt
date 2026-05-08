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
class RemoteFlowRowTest {
    @get:Rule
    val composeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
            matcher = MSSIMMatcher(threshold = 0.999),
        )

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
        composeTestRule.runScreenshotTest(profile = experimentalProfile) {
            gridScreenshotUI.GridContent(
                getLayoutAlignmentUIs(),
                layoutDirection = LayoutDirection.Rtl,
            )
        }

    @Test
    fun absoluteArrangement() =
        composeTestRule.runScreenshotTest(profile = experimentalProfile) {
            val horizontalArrangements = listOf(Absolute.Left, Absolute.Center, Absolute.Right)
            gridScreenshotUI.GridContent(getLayoutAlignmentUIs(horizontalArrangements))
        }

    @Test
    fun rtlAbsoluteArrangement() =
        composeTestRule.runScreenshotTest(profile = experimentalProfile) {
            val horizontalArrangements = listOf(Absolute.Left, Absolute.Center, Absolute.Right)
            gridScreenshotUI.GridContent(
                getLayoutAlignmentUIs(horizontalArrangements),
                layoutDirection = LayoutDirection.Rtl,
            )
        }

    @Test
    fun wrapGrid() {
        composeTestRule.runScreenshotTest(profile = experimentalProfile) {
            gridScreenshotUI.GridContent(getLayoutAlignmentWrapUIs())
        }
    }

    @Test
    fun wrapRtl() =
        composeTestRule.runScreenshotTest(profile = experimentalProfile) {
            gridScreenshotUI.GridContent(
                getLayoutAlignmentWrapUIs(),
                layoutDirection = LayoutDirection.Rtl,
            )
        }

    @Test
    fun wrapAbsoluteArrangement() =
        composeTestRule.runScreenshotTest(profile = experimentalProfile) {
            val horizontalArrangements = listOf(Absolute.Left, Absolute.Center, Absolute.Right)
            gridScreenshotUI.GridContent(getLayoutAlignmentWrapUIs(horizontalArrangements))
        }

    @Test
    fun wrapRtlAbsoluteArrangement() =
        composeTestRule.runScreenshotTest(profile = experimentalProfile) {
            val horizontalArrangements = listOf(Absolute.Left, Absolute.Center, Absolute.Right)
            gridScreenshotUI.GridContent(
                getLayoutAlignmentWrapUIs(horizontalArrangements),
                layoutDirection = LayoutDirection.Rtl,
            )
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

    @Test
    fun constraints() =
        composeTestRule.runScreenshotTest(profile = experimentalProfile) {
            gridScreenshotUI.GridContent(
                listOf(
                    "maxItemsInEachRow = 3" to { TestMaxItemsInEachRow() },
                    "maxLines = 2" to { TestMaxLines() },
                )
            )
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
                ),
                layoutDirection = LayoutDirection.Rtl,
            )
        }

    @Test
    fun spacedByAbsolute() =
        composeTestRule.runScreenshotTest(profile = experimentalProfile) {
            gridScreenshotUI.GridContent(
                listOf(
                    "rdp Start" to
                        {
                            TestSpacedByAbsoluteRemoteDp(alignment = RemoteAlignment.Start)
                        },
                    "rdp Center" to
                        {
                            TestSpacedByAbsoluteRemoteDp(
                                alignment = RemoteAlignment.CenterHorizontally
                            )
                        },
                    "rdp End" to { TestSpacedByAbsoluteRemoteDp(alignment = RemoteAlignment.End) },
                    "rdp Left" to
                        {
                            TestSpacedByAbsoluteRemoteDp(alignment = RemoteAbsoluteAlignment.Left)
                        },
                    "Blank" to { Blank() },
                    "rdp Right" to
                        {
                            TestSpacedByAbsoluteRemoteDp(alignment = RemoteAbsoluteAlignment.Right)
                        },
                    "rf Start" to
                        {
                            TestSpacedByAbsoluteRemoteFloat(alignment = RemoteAlignment.Start)
                        },
                    "rf Center" to
                        {
                            TestSpacedByAbsoluteRemoteFloat(
                                alignment = RemoteAlignment.CenterHorizontally
                            )
                        },
                    "rf End" to
                        {
                            TestSpacedByAbsoluteRemoteFloat(alignment = RemoteAlignment.End)
                        },
                    "rf Left" to
                        {
                            TestSpacedByAbsoluteRemoteFloat(
                                alignment = RemoteAbsoluteAlignment.Left
                            )
                        },
                    "Blank" to { Blank() },
                    "rf Right" to
                        {
                            TestSpacedByAbsoluteRemoteFloat(
                                alignment = RemoteAbsoluteAlignment.Right
                            )
                        },
                )
            )
        }

    @Test
    fun spacedByAbsoluteRtl() =
        composeTestRule.runScreenshotTest(profile = experimentalProfile) {
            gridScreenshotUI.GridContent(
                listOf(
                    "rdp Start" to
                        {
                            TestSpacedByAbsoluteRemoteDp(alignment = RemoteAlignment.Start)
                        },
                    "rdp Center" to
                        {
                            TestSpacedByAbsoluteRemoteDp(
                                alignment = RemoteAlignment.CenterHorizontally
                            )
                        },
                    "rdp End" to { TestSpacedByAbsoluteRemoteDp(alignment = RemoteAlignment.End) },
                    "rdp Left" to
                        {
                            TestSpacedByAbsoluteRemoteDp(alignment = RemoteAbsoluteAlignment.Left)
                        },
                    "Blank" to { Blank() },
                    "rdp Right" to
                        {
                            TestSpacedByAbsoluteRemoteDp(alignment = RemoteAbsoluteAlignment.Right)
                        },
                    "rf Start" to
                        {
                            TestSpacedByAbsoluteRemoteFloat(alignment = RemoteAlignment.Start)
                        },
                    "rf Center" to
                        {
                            TestSpacedByAbsoluteRemoteFloat(
                                alignment = RemoteAlignment.CenterHorizontally
                            )
                        },
                    "rf End" to
                        {
                            TestSpacedByAbsoluteRemoteFloat(alignment = RemoteAlignment.End)
                        },
                    "rf Left" to
                        {
                            TestSpacedByAbsoluteRemoteFloat(
                                alignment = RemoteAbsoluteAlignment.Left
                            )
                        },
                    "Blank" to { Blank() },
                    "rf Right" to
                        {
                            TestSpacedByAbsoluteRemoteFloat(
                                alignment = RemoteAbsoluteAlignment.Right
                            )
                        },
                ),
                layoutDirection = LayoutDirection.Rtl,
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

    private fun getLayoutAlignmentWrapUIs(
        horizontalArrangements: List<RemoteArrangement.Horizontal> = this.horizontalArrangements
    ): List<Pair<String, @RemoteComposable @Composable () -> Unit>> =
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
                                                RemoteModifier.size(40.rdp)
                                                    .background(Color(0xFF6200EE))
                                        )
                                        RemoteBox(
                                            modifier =
                                                RemoteModifier.size(40.rdp)
                                                    .background(Color(0xFF03DAC6))
                                        )
                                        RemoteBox(
                                            modifier =
                                                RemoteModifier.size(40.rdp)
                                                    .background(Color(0xFFBB86FC))
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
            modifier = RemoteModifier.fillMaxSize(),
            horizontalArrangement = spacedBy(5.rdp),
            verticalArrangement = RemoteArrangement.Top,
        ) {
            RemoteBox(
                modifier =
                    RemoteModifier.width(20.rdp).fillMaxHeight().background(Color(0xFF6200EE))
            )
            RemoteBox(
                modifier =
                    RemoteModifier.width(20.rdp).fillMaxHeight().background(Color(0xFF03DAC6))
            )
            RemoteBox(
                modifier =
                    RemoteModifier.width(20.rdp).fillMaxHeight().background(Color(0xFFBB86FC))
            )
        }
    }

    @RemoteComposable
    @Composable
    private fun TestSpacedByRemoteFloat() {
        RemoteFlowRow(
            modifier = RemoteModifier.fillMaxSize(),
            horizontalArrangement = spacedBy(10f.rf),
            verticalArrangement = RemoteArrangement.Top,
        ) {
            RemoteBox(
                modifier =
                    RemoteModifier.width(20.rdp).fillMaxHeight().background(Color(0xFF6200EE))
            )
            RemoteBox(
                modifier =
                    RemoteModifier.width(20.rdp).fillMaxHeight().background(Color(0xFF03DAC6))
            )
            RemoteBox(
                modifier =
                    RemoteModifier.width(20.rdp).fillMaxHeight().background(Color(0xFFBB86FC))
            )
        }
    }

    @RemoteComposable
    @Composable
    private fun TestSpacedByRemoteDp(alignment: RemoteAlignment.Horizontal) {
        RemoteFlowRow(
            modifier = RemoteModifier.fillMaxSize(),
            horizontalArrangement = spacedBy(space = 5.rdp, alignment = alignment),
            verticalArrangement = RemoteArrangement.Top,
        ) {
            RemoteBox(
                modifier =
                    RemoteModifier.width(20.rdp).fillMaxHeight().background(Color(0xFF6200EE))
            )
            RemoteBox(
                modifier =
                    RemoteModifier.width(20.rdp).fillMaxHeight().background(Color(0xFF03DAC6))
            )
            RemoteBox(
                modifier =
                    RemoteModifier.width(20.rdp).fillMaxHeight().background(Color(0xFFBB86FC))
            )
        }
    }

    @RemoteComposable
    @Composable
    private fun TestSpacedByRemoteFloat(alignment: RemoteAlignment.Horizontal) {
        RemoteFlowRow(
            modifier = RemoteModifier.fillMaxSize(),
            horizontalArrangement = spacedBy(space = 10f.rf, alignment = alignment),
            verticalArrangement = RemoteArrangement.Top,
        ) {
            RemoteBox(
                modifier =
                    RemoteModifier.width(20.rdp).fillMaxHeight().background(Color(0xFF6200EE))
            )
            RemoteBox(
                modifier =
                    RemoteModifier.width(20.rdp).fillMaxHeight().background(Color(0xFF03DAC6))
            )
            RemoteBox(
                modifier =
                    RemoteModifier.width(20.rdp).fillMaxHeight().background(Color(0xFFBB86FC))
            )
        }
    }

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
            modifier = RemoteModifier.fillMaxSize(),
            horizontalArrangement = RemoteArrangement.Start,
            verticalArrangement = verticalArrangement,
        ) {
            repeat(size) { index ->
                val color = if (index % 2 == 0) Color(0xFF6200EE) else Color(0xFF03DAC6)
                RemoteBox(modifier = RemoteModifier.size(30.rdp).background(color))
            }
        }
    }

    @RemoteComposable @Composable private fun Blank() {}

    @RemoteComposable
    @Composable
    private fun TestSpacedByAbsoluteRemoteDp(alignment: RemoteAlignment.Horizontal) {
        RemoteFlowRow(
            modifier = RemoteModifier.fillMaxSize(),
            horizontalArrangement = Absolute.spacedBy(space = 5.rdp, alignment = alignment),
            verticalArrangement = RemoteArrangement.Top,
        ) {
            RemoteBox(
                modifier =
                    RemoteModifier.width(20.rdp).fillMaxHeight().background(Color(0xFF6200EE))
            )
            RemoteBox(
                modifier =
                    RemoteModifier.width(20.rdp).fillMaxHeight().background(Color(0xFF03DAC6))
            )
            RemoteBox(
                modifier =
                    RemoteModifier.width(20.rdp).fillMaxHeight().background(Color(0xFFBB86FC))
            )
        }
    }

    @RemoteComposable
    @Composable
    private fun TestSpacedByAbsoluteRemoteFloat(alignment: RemoteAlignment.Horizontal) {
        RemoteFlowRow(
            modifier = RemoteModifier.fillMaxSize(),
            horizontalArrangement = Absolute.spacedBy(space = 10f.rf, alignment = alignment),
            verticalArrangement = RemoteArrangement.Top,
        ) {
            RemoteBox(
                modifier =
                    RemoteModifier.width(20.rdp).fillMaxHeight().background(Color(0xFF6200EE))
            )
            RemoteBox(
                modifier =
                    RemoteModifier.width(20.rdp).fillMaxHeight().background(Color(0xFF03DAC6))
            )
            RemoteBox(
                modifier =
                    RemoteModifier.width(20.rdp).fillMaxHeight().background(Color(0xFFBB86FC))
            )
        }
    }

    @Composable
    @RemoteComposable
    fun TestMaxItemsInEachRow() {
        RemoteFlowRow(
            modifier = RemoteModifier.fillMaxSize().background(Color.LightGray),
            maxItemsInEachRow = 3,
        ) {
            repeat(10) { index ->
                val color = if (index % 2 == 0) Color(0xFF6200EE) else Color(0xFF03DAC6)
                RemoteBox(modifier = RemoteModifier.size(20.rdp).background(color))
            }
        }
    }

    @Composable
    @RemoteComposable
    private fun TestMaxLines() {
        RemoteFlowRow(
            modifier = RemoteModifier.fillMaxSize().background(Color.LightGray),
            maxLines = 2,
        ) {
            repeat(15) { index ->
                val color = if (index % 2 == 0) Color(0xFF6200EE) else Color(0xFF03DAC6)
                RemoteBox(modifier = RemoteModifier.size(20.rdp).background(color))
            }
        }
    }
}
