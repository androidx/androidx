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
import androidx.compose.remote.creation.compose.layout.RemoteArrangement.spacedBy
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI.Companion.DefaultContainerSize
import androidx.compose.remote.creation.compose.test.util.propertyName
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
class RemoteColumnTest {
    @get:Rule
    val composeTestRule: RemoteComposeScreenshotTestRule by lazy {
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)
    }

    private val gridScreenshotUI = GridScreenshotUI()

    private val alignments =
        listOf(RemoteAlignment.Start, RemoteAlignment.CenterHorizontally, RemoteAlignment.End)
    private val arrangements =
        listOf(RemoteArrangement.Top, RemoteArrangement.Center, RemoteArrangement.Bottom)

    @Test
    fun grid() =
        composeTestRule.runScreenshotTest {
            gridScreenshotUI.GridContent(getLayoutAlignmentUIs(alignments))
        }

    @Test
    fun rtl() =
        composeTestRule.runScreenshotTest(layoutDirection = LayoutDirection.Rtl) {
            val alignments =
                listOf(
                    RemoteAlignment.Start,
                    RemoteAlignment.CenterHorizontally,
                    RemoteAlignment.End,
                )
            gridScreenshotUI.GridContent(getLayoutAlignmentUIs(alignments))
        }

    @Test
    fun absoluteAlignment() =
        composeTestRule.runScreenshotTest {
            val alignments =
                listOf(
                    RemoteAbsoluteAlignment.Left,
                    RemoteAlignment.CenterHorizontally,
                    RemoteAbsoluteAlignment.Right,
                )
            gridScreenshotUI.GridContent(getLayoutAlignmentUIs(alignments))
        }

    @Test
    fun rtlAbsoluteAlignment() =
        composeTestRule.runScreenshotTest(layoutDirection = LayoutDirection.Rtl) {
            val alignments =
                listOf(
                    RemoteAbsoluteAlignment.Left,
                    RemoteAlignment.CenterHorizontally,
                    RemoteAbsoluteAlignment.Right,
                )
            gridScreenshotUI.GridContent(getLayoutAlignmentUIs(alignments))
        }

    @Test
    fun spacedBy() =
        composeTestRule.runScreenshotTest {
            gridScreenshotUI.GridContent(
                listOf(
                    "rdp Top" to { TestSpacedByRemoteDp(alignment = RemoteAlignment.Top) },
                    "rdp Center" to
                        {
                            TestSpacedByRemoteDp(alignment = RemoteAlignment.CenterVertically)
                        },
                    "rdp Bottom" to { TestSpacedByRemoteDp(alignment = RemoteAlignment.Bottom) },
                    "rf Top" to { TestSpacedByRemoteFloat(alignment = RemoteAlignment.Top) },
                    "rf Center" to
                        {
                            TestSpacedByRemoteFloat(alignment = RemoteAlignment.CenterVertically)
                        },
                    "rf Bottom" to { TestSpacedByRemoteFloat(alignment = RemoteAlignment.Bottom) },
                    "rdp" to { TestSpacedByRemoteDp() },
                    "rf" to { TestSpacedByRemoteFloat() },
                )
            )
        }

    private fun getLayoutAlignmentUIs(
        alignments: List<RemoteAlignment.Horizontal>
    ): List<Pair<String, @RemoteComposable @Composable () -> Unit>> =
        sequence {
                for (arrangement in arrangements) {
                    for (alignment in alignments) {
                        yield(
                            "${arrangement.propertyName()} ${alignment.propertyName()}" to
                                @RemoteComposable @Composable {
                                    // TODO(b/447100988): replace size by fillMaxSize in all
                                    // those RemoteColumns
                                    RemoteColumn(
                                        modifier = RemoteModifier.size(DefaultContainerSize),
                                        horizontalAlignment = alignment,
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

    @RemoteComposable
    @Composable
    private fun TestSpacedByRemoteDp() {
        RemoteColumn(
            // TODO(b/447100988): replace size by fillMaxSize
            modifier = RemoteModifier.size(DefaultContainerSize),
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
            verticalArrangement = spacedBy(5.rdp),
        ) {
            repeat(3) { index ->
                val color = if (index % 2 == 0) Color(0xFF6200EE) else Color(0xFF03DAC6)
                RemoteBox(modifier = RemoteModifier.height(20.rdp).fillMaxWidth().background(color))
            }
        }
    }

    @RemoteComposable
    @Composable
    private fun TestSpacedByRemoteFloat() {
        RemoteColumn(
            // TODO(b/447100988): replace size by fillMaxSize
            modifier = RemoteModifier.size(DefaultContainerSize),
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
            verticalArrangement = spacedBy(10f.rf),
        ) {
            repeat(3) { index ->
                val color = if (index % 2 == 0) Color(0xFF6200EE) else Color(0xFF03DAC6)
                RemoteBox(modifier = RemoteModifier.height(20.rdp).fillMaxWidth().background(color))
            }
        }
    }

    @RemoteComposable
    @Composable
    private fun TestSpacedByRemoteDp(alignment: RemoteAlignment.Vertical) {
        RemoteColumn(
            // TODO(b/447100988): replace size by fillMaxSize
            modifier = RemoteModifier.size(DefaultContainerSize),
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
            verticalArrangement = spacedBy(space = 5.rdp, alignment = alignment),
        ) {
            repeat(3) { index ->
                val color = if (index % 2 == 0) Color(0xFF6200EE) else Color(0xFF03DAC6)
                RemoteBox(modifier = RemoteModifier.height(20.rdp).fillMaxWidth().background(color))
            }
        }
    }

    @RemoteComposable
    @Composable
    private fun TestSpacedByRemoteFloat(alignment: RemoteAlignment.Vertical) {
        RemoteColumn(
            // TODO(b/447100988): replace size by fillMaxSize
            modifier = RemoteModifier.size(DefaultContainerSize),
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
            verticalArrangement = spacedBy(space = 10f.rf, alignment = alignment),
        ) {
            repeat(3) { index ->
                val color = if (index % 2 == 0) Color(0xFF6200EE) else Color(0xFF03DAC6)
                RemoteBox(modifier = RemoteModifier.height(20.rdp).fillMaxWidth().background(color))
            }
        }
    }
}
