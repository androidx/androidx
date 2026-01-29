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
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI.Companion.DefaultContainerSize
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI.Companion.toInput
import androidx.compose.remote.creation.compose.test.util.propertyName
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
class RemoteCollapsibleRowTest {
    @get:Rule
    val composeTestRule: RemoteComposeScreenshotTestRule by lazy {
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)
    }

    private val arrangements =
        listOf(RemoteArrangement.Start, RemoteArrangement.CenterHorizontally, RemoteArrangement.End)
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

    private fun getLayoutAlignmentUIs():
        List<Pair<String, @RemoteComposable @Composable () -> Unit>> =
        sequence {
                for (alignment in alignments) {
                    for (arrangement in arrangements) {
                        yield(
                            "${alignment.propertyName()} ${arrangement.propertyName()}" to
                                @RemoteComposable @Composable {
                                    // TODO(b/447100988): replace size by fillMaxSize in all those
                                    // RemoteCollapsibleRow
                                    RemoteCollapsibleRow(
                                        modifier = RemoteModifier.size(DefaultContainerSize),
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
        // TODO(b/447100988): replace size by fillMaxSize in all those RemoteCollapsibleRow
        RemoteCollapsibleRow(modifier = modifier.size(DefaultContainerSize).background(Color.Red)) {
            content()
        }
    }

    private fun getPriorityModifier(priority: Float): RemoteModifier =
        with(RemoteCollapsibleRowScope()) { RemoteModifier.priority(priority) }
}
