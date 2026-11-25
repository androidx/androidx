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
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class InteractiveScrollbarScreenshotTest(
    private val orientation: Orientation,
    private val layoutDirection: LayoutDirection,
) {
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val screenshotRule = AndroidXScreenshotTestRule("compose/material3/material3")

    private val Tag = "scrollbar"
    private val ScrollableContainerSize = 100.dp

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
    fun interactiveScrollbar() {
        composeTestRule.setContent {
            InteractiveScrollbarTestContainer(
                state = TestScrollState(0, 200, 100),
                orientation = orientation,
                layoutDirection = layoutDirection,
            )
        }
        composeTestRule
            .onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, getGoldenName("interactiveScrollbar"))
    }

    @Test
    fun interactiveScrollbar_hover() {
        composeTestRule.setContent {
            InteractiveScrollbarTestContainer(
                state = TestScrollState(0, 200, 100),
                orientation = orientation,
                layoutDirection = layoutDirection,
                thumbInteractionSource = AlwaysHoveredInteractionSource,
            )
        }
        composeTestRule
            .onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, getGoldenName("interactiveScrollbar_hover"))
    }

    @Test
    fun interactiveScrollbar_pressed() {
        composeTestRule.setContent {
            InteractiveScrollbarTestContainer(
                state = TestScrollState(0, 200, 100),
                orientation = orientation,
                layoutDirection = layoutDirection,
                thumbInteractionSource = AlwaysPressedInteractionSource,
            )
        }
        composeTestRule
            .onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, getGoldenName("interactiveScrollbar_pressed"))
    }

    @Composable
    private fun InteractiveScrollbarTestContainer(
        state: ScrollIndicatorState,
        orientation: Orientation,
        layoutDirection: LayoutDirection,
        modifier: Modifier = Modifier,
        thumbInteractionSource: MutableInteractionSource? = null,
    ) {
        MaterialTheme {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Box(
                    modifier
                        .size(ScrollableContainerSize)
                        .testTag(Tag)
                        .interactiveScrollbar(
                            state = state,
                            orientation = orientation,
                            onThumbDrag = {},
                            onTrackPress = {},
                            thumbInteractionSource = thumbInteractionSource,
                            isFadeEnabled = false,
                        )
                )
            }
        }
    }

    private class TestScrollState(
        override val scrollOffset: Int,
        override val contentSize: Int,
        override val viewportSize: Int,
    ) : ScrollIndicatorState
}

private val AlwaysHoveredInteractionSource: MutableInteractionSource =
    StaticMutableInteractionSource(HoverInteraction.Enter())

private val AlwaysPressedInteractionSource: MutableInteractionSource =
    StaticMutableInteractionSource(PressInteraction.Press(Offset.Zero))

private class StaticMutableInteractionSource(vararg interactionsToShow: Interaction) :
    MutableInteractionSource {
    override val interactions: Flow<Interaction> = interactionsToShow.asFlow()

    override suspend fun emit(interaction: Interaction) {}

    override fun tryEmit(interaction: Interaction): Boolean = true
}
