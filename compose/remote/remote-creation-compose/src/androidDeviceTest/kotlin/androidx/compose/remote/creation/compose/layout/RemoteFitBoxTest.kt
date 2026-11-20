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
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.runtime.Composable
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
class RemoteFitBoxTest {
    @get:Rule
    val composeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    private val gridScreenshotUI = GridScreenshotUI()

    @Test
    fun grid() = composeTestRule.runScreenshotTest {
        val alignments =
            listOf(
                "Top Start" to RemoteAlignment.TopStart,
                "Top CenterHorizontally" to RemoteAlignment.TopCenter,
                "Top End" to RemoteAlignment.TopEnd,
                "Center Start" to RemoteAlignment.CenterStart,
                "Center CenterHorizontally" to RemoteAlignment.Center,
                "Center End" to RemoteAlignment.CenterEnd,
                "Bottom Start" to RemoteAlignment.BottomStart,
                "Bottom CenterHorizontally" to RemoteAlignment.BottomCenter,
                "Bottom End" to RemoteAlignment.BottomEnd,
            )
        gridScreenshotUI.GridContent(getLayoutAlignmentUIs(alignments))
    }

    @Test
    fun rtl() = composeTestRule.runScreenshotTest {
        val alignments =
            listOf(
                "Top Start" to RemoteAlignment.TopStart,
                "Top CenterHorizontally" to RemoteAlignment.TopCenter,
                "Top End" to RemoteAlignment.TopEnd,
                "Center Start" to RemoteAlignment.CenterStart,
                "Center CenterHorizontally" to RemoteAlignment.Center,
                "Center End" to RemoteAlignment.CenterEnd,
                "Bottom Start" to RemoteAlignment.BottomStart,
                "Bottom CenterHorizontally" to RemoteAlignment.BottomCenter,
                "Bottom End" to RemoteAlignment.BottomEnd,
            )
        gridScreenshotUI.GridContent(
            getLayoutAlignmentUIs(alignments),
            layoutDirection = LayoutDirection.Rtl,
        )
    }

    @Test
    fun absoluteAlignment() = composeTestRule.runScreenshotTest {
        val alignments =
            listOf(
                "Top Left" to RemoteAbsoluteAlignment.TopLeft,
                "Top CenterHorizontally" to RemoteAlignment.TopCenter,
                "Top Right" to RemoteAbsoluteAlignment.TopRight,
                "Center Left" to RemoteAbsoluteAlignment.CenterLeft,
                "Center CenterHorizontally" to RemoteAlignment.Center,
                "Center Right" to RemoteAbsoluteAlignment.CenterRight,
                "Bottom Left" to RemoteAbsoluteAlignment.BottomLeft,
                "Bottom CenterHorizontally" to RemoteAlignment.BottomCenter,
                "Bottom Right" to RemoteAbsoluteAlignment.BottomRight,
            )
        gridScreenshotUI.GridContent(getLayoutAlignmentUIs(alignments))
    }

    @Test
    fun rtlAbsoluteAlignment() = composeTestRule.runScreenshotTest {
        val alignments =
            listOf(
                "Top Left" to RemoteAbsoluteAlignment.TopLeft,
                "Top CenterHorizontally" to RemoteAlignment.TopCenter,
                "Top Right" to RemoteAbsoluteAlignment.TopRight,
                "Center Left" to RemoteAbsoluteAlignment.CenterLeft,
                "Center CenterHorizontally" to RemoteAlignment.Center,
                "Center Right" to RemoteAbsoluteAlignment.CenterRight,
                "Bottom Left" to RemoteAbsoluteAlignment.BottomLeft,
                "Bottom CenterHorizontally" to RemoteAlignment.BottomCenter,
                "Bottom Right" to RemoteAbsoluteAlignment.BottomRight,
            )
        gridScreenshotUI.GridContent(
            getLayoutAlignmentUIs(alignments),
            layoutDirection = LayoutDirection.Rtl,
        )
    }

    private fun getLayoutAlignmentUIs(
        alignments: List<Pair<String, RemoteAlignment>>
    ): List<Pair<String, @RemoteComposable @Composable () -> Unit>> =
        alignments.map { (label, alignment) ->
            label to
                @RemoteComposable @Composable {
                    RemoteFitBox(
                        modifier = RemoteModifier.fillMaxSize(),
                        contentAlignment = alignment,
                    ) {
                        RemoteBox(modifier = RemoteModifier.size(48.rdp).background(0xFF6200EE.rc))
                        RemoteBox(modifier = RemoteModifier.size(24.rdp).background(0xFF03DAC6.rc))
                    }
                }
        }
}
