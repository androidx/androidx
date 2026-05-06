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
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI.Companion.DefaultContainerSize
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI.Companion.toInput
import androidx.compose.remote.creation.compose.test.util.propertyName
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
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
class RemoteCollapsibleRowTest {
    @get:Rule
    val composeTestRule: RemoteComposeScreenshotTestRule by lazy {
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            matcher = MSSIMMatcher(threshold = 0.999),
        )
    }

    private val arrangements =
        listOf(RemoteArrangement.Start, RemoteArrangement.Center, RemoteArrangement.End)
    private val alignments =
        listOf(RemoteAlignment.Top, RemoteAlignment.CenterVertically, RemoteAlignment.Bottom)

    private val gridScreenshotUI = GridScreenshotUI()
    private val collapsibleUI =
        CollapsibleUI(
            contentUnderTest = ::CollapsibleContentUnderTest,
            priorityModifier = ::getPriorityModifier,
        )

    @Test
    fun grid() =
        composeTestRule.runScreenshotTest {
            gridScreenshotUI.GridContent(getLayoutAlignmentUIs() + collapsibleUI.getUIs().toInput())
        }

    @Test
    fun rtl() =
        composeTestRule.runScreenshotTest {
            gridScreenshotUI.GridContent(
                getLayoutAlignmentUIs(),
                layoutDirection = LayoutDirection.Rtl,
            )
        }

    @Test
    fun absoluteArrangement() =
        composeTestRule.runScreenshotTest {
            val arrangements = listOf(Absolute.Left, Absolute.Center, Absolute.Right)
            gridScreenshotUI.GridContent(getLayoutAlignmentUIs(arrangements))
        }

    @Test
    fun rtlAbsoluteArrangement() =
        composeTestRule.runScreenshotTest {
            val arrangements = listOf(Absolute.Left, Absolute.Center, Absolute.Right)
            gridScreenshotUI.GridContent(
                getLayoutAlignmentUIs(arrangements),
                layoutDirection = LayoutDirection.Rtl,
            )
        }

    @Test
    fun spacedBy() =
        composeTestRule.runScreenshotTest {
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
        composeTestRule.runScreenshotTest {
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
        composeTestRule.runScreenshotTest {
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
        composeTestRule.runScreenshotTest {
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
        arrangements: List<RemoteArrangement.Horizontal> = this.arrangements
    ): List<Pair<String, @RemoteComposable @Composable () -> Unit>> =
        sequence {
                for (alignment in alignments) {
                    for (arrangement in arrangements) {
                        yield(
                            "${alignment.propertyName()} ${arrangement.propertyName()}" to
                                @RemoteComposable @Composable {
                                    RemoteCollapsibleRow(
                                        modifier = RemoteModifier.fillMaxSize(),
                                        horizontalArrangement = arrangement,
                                        verticalAlignment = alignment,
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

    @Composable
    @RemoteComposable
    private fun CollapsibleContentUnderTest(
        modifier: RemoteModifier,
        content: @Composable @RemoteComposable () -> Unit,
    ) {
        RemoteCollapsibleRow(modifier = modifier.fillMaxSize().background(Color.Red)) { content() }
    }

    private fun getPriorityModifier(priority: Float): RemoteModifier =
        with(RemoteCollapsibleRowScope()) { RemoteModifier.priority(priority) }

    @RemoteComposable
    @Composable
    private fun TestSpacedByRemoteDp() {
        RemoteCollapsibleRow(
            modifier = RemoteModifier.fillMaxSize(),
            horizontalArrangement = spacedBy(5.rdp),
            verticalAlignment = RemoteAlignment.CenterVertically,
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
        RemoteCollapsibleRow(
            modifier = RemoteModifier.fillMaxSize(),
            horizontalArrangement = spacedBy(10f.rf),
            verticalAlignment = RemoteAlignment.CenterVertically,
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
        RemoteCollapsibleRow(
            modifier = RemoteModifier.fillMaxSize(),
            horizontalArrangement = spacedBy(space = 5.rdp, alignment = alignment),
            verticalAlignment = RemoteAlignment.CenterVertically,
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
        RemoteCollapsibleRow(
            modifier = RemoteModifier.fillMaxSize(),
            horizontalArrangement = spacedBy(space = 10f.rf, alignment = alignment),
            verticalAlignment = RemoteAlignment.CenterVertically,
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

    @RemoteComposable @Composable private fun Blank() {}

    @RemoteComposable
    @Composable
    private fun TestSpacedByAbsoluteRemoteDp(alignment: RemoteAlignment.Horizontal) {
        RemoteCollapsibleRow(
            modifier = RemoteModifier.size(DefaultContainerSize),
            horizontalArrangement = Absolute.spacedBy(space = 5.rdp, alignment = alignment),
            verticalAlignment = RemoteAlignment.CenterVertically,
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
        RemoteCollapsibleRow(
            modifier = RemoteModifier.size(DefaultContainerSize),
            horizontalArrangement = Absolute.spacedBy(space = 10f.rf, alignment = alignment),
            verticalAlignment = RemoteAlignment.CenterVertically,
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
}
