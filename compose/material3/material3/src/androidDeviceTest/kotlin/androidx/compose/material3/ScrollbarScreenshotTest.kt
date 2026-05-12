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

package androidx.compose.material3

import androidx.compose.foundation.ScrollIndicatorState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class ScrollbarScreenshotTest(
    private val orientation: Orientation,
    private val layoutDirection: LayoutDirection,
) {
    @get:Rule val composeTestRule = createComposeRule(StandardTestDispatcher())
    @get:Rule val screenshotRule = AndroidXScreenshotTestRule("compose/material3/material3")

    private val Tag = "scrollbar"

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "orientation={0}, layoutDirection={1}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(Orientation.Vertical, LayoutDirection.Ltr),
                arrayOf(Orientation.Vertical, LayoutDirection.Rtl),
                arrayOf(Orientation.Horizontal, LayoutDirection.Ltr),
                arrayOf(Orientation.Horizontal, LayoutDirection.Rtl),
            )
        }
    }

    private fun getGoldenName(testName: String): String {
        val orientationStr = if (orientation == Orientation.Vertical) "vertical" else "horizontal"
        val directionStr = if (layoutDirection == LayoutDirection.Ltr) "ltr" else "rtl"
        return "${testName}_${orientationStr}_${directionStr}"
    }

    @Test
    fun scrollbar_isDrawn() {
        composeTestRule.setContent {
            ScrollbarTestContainer(
                state = TestScrollState(0, 200, 100),
                orientation = orientation,
                layoutDirection = layoutDirection,
            )
        }
        composeTestRule
            .onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, getGoldenName("scrollbar_isDrawn"))
    }

    @Test
    fun scrollbar_customColorsAndThickness() {
        composeTestRule.setContent {
            ScrollbarTestContainer(
                state = TestScrollState(0, 200, 100),
                orientation = orientation,
                layoutDirection = layoutDirection,
                thumbColor = Color.Green,
                trackColor = Color.Yellow,
                thickness = 16.dp,
            )
        }
        composeTestRule
            .onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, getGoldenName("scrollbar_custom_colors_thickness"))
    }

    @Test
    fun scrollbar_customTrackInsets() {
        composeTestRule.setContent {
            ScrollbarTestContainer(
                state = TestScrollState(0, 200, 100),
                orientation = orientation,
                layoutDirection = layoutDirection,
                mainAxisTrackInset = 10.dp,
                crossAxisTrackInset = 10.dp,
            )
        }
        composeTestRule
            .onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, getGoldenName("scrollbar_custom_track_insets"))
    }

    @Composable
    private fun ScrollbarTestContainer(
        state: ScrollIndicatorState,
        orientation: Orientation,
        layoutDirection: LayoutDirection,
        mainAxisTrackInset: Dp = 0.dp,
        crossAxisTrackInset: Dp = 0.dp,
        thumbMinLength: Dp = 24.dp,
        thumbMaxLengthFraction: Float = 0.9f,
        thumbColor: Color = Color.Red,
        trackColor: Color = Color.Blue,
        thickness: Dp = 10.dp,
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Box(
                Modifier.size(100.dp)
                    .testTag(Tag)
                    .scrollbar(
                        state = state,
                        orientation = orientation,
                        thumbColor = thumbColor,
                        trackColor = trackColor,
                        thickness = thickness,
                        thumbMinLength = thumbMinLength,
                        thumbMaxLengthFraction = thumbMaxLengthFraction,
                        isFadeEnabled = false,
                        mainAxisTrackInset = mainAxisTrackInset,
                        crossAxisTrackInset = crossAxisTrackInset,
                    )
            )
        }
    }

    private class TestScrollState(
        override val scrollOffset: Int,
        override val contentSize: Int,
        override val viewportSize: Int,
    ) : ScrollIndicatorState
}
