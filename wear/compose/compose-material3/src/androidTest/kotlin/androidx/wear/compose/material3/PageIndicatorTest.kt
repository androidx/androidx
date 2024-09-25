/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.material3

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.testutils.assertContainsColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.LayoutDirection
import androidx.compose.ui.test.RoundScreen
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.pager.PagerState
import org.junit.Rule
import org.junit.Test

@RequiresApi(Build.VERSION_CODES.O)
class PageIndicatorTest {
    @get:Rule val rule = createComposeRule()

    @Test
    public fun horizontalPageIndicator_supports_testtag_circular() {
        rule.setContentWithTheme {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.RoundScreen(isScreenRound = true)
            ) {
                HorizontalPageIndicator(
                    modifier = Modifier.testTag(TEST_TAG),
                    pagerState = pagerState_start,
                )
            }
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    public fun horizontalPageIndicator_supports_testtag_linear() {
        rule.setContentWithTheme {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.RoundScreen(isScreenRound = false)
            ) {
                HorizontalPageIndicator(
                    modifier = Modifier.testTag(TEST_TAG),
                    pagerState = pagerState_start
                )
            }
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    public fun verticalPageIndicator_supports_testtag_circular() {
        rule.setContentWithTheme {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.RoundScreen(isScreenRound = true)
            ) {
                VerticalPageIndicator(
                    modifier = Modifier.testTag(TEST_TAG),
                    pagerState = pagerState_start,
                )
            }
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    public fun verticalPageIndicator_supports_testtag_linear() {
        rule.setContentWithTheme {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.RoundScreen(isScreenRound = false)
            ) {
                VerticalPageIndicator(
                    modifier = Modifier.testTag(TEST_TAG),
                    pagerState = pagerState_start
                )
            }
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    public fun horizontalPageIndicator_position_is_selected_circular() {
        horizontalPageIndicator_position_is_selected_circular(LayoutDirection.Ltr)
    }

    @Test
    public fun horizontalPageIndicator_in_between_positions_circular() {
        rule.setContentWithTheme {
            DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(true)) {
                Box(modifier = Modifier.size(150.dp)) {
                    HorizontalPageIndicator(
                        modifier = Modifier.testTag(TEST_TAG),
                        pagerState = pagerState_middle,
                        selectedColor = selectedColor,
                        unselectedColor = unselectedColor,
                        backgroundColor = backgroundColor
                    )
                }
            }
        }
        rule.waitForIdle()

        // Selected color should occupy 2 dots with space in between, which
        // approximately equals to 12%
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(selectedColor, 11f..14f)
        // Unselected dots should also be visible on the screen, and should take around 9%
        // (4.4% per dot, 2 dots total)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(unselectedColor, 7f..10f)

        // Check that background color exists
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(backgroundColor)
    }

    @Test
    public fun verticalPageIndicator_position_is_selected_circular() {
        verticalPageIndicator_position_is_selected_circular(LayoutDirection.Ltr)
    }

    @Test
    public fun verticalPageIndicator_in_between_positions_circular() {
        rule.setContentWithTheme {
            DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(true)) {
                Box(modifier = Modifier.size(150.dp)) {
                    VerticalPageIndicator(
                        modifier = Modifier.testTag(TEST_TAG),
                        pagerState = pagerState_middle,
                        selectedColor = selectedColor,
                        unselectedColor = unselectedColor,
                        backgroundColor = backgroundColor
                    )
                }
            }
        }
        rule.waitForIdle()

        // Selected color should occupy 2 dots with space in between, which
        // approximately equals to 12%
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(selectedColor, 10f..14f)
        // Unselected dots ( which doesn't participate in color merge)
        // should also be visible on the screen, and should take around 8.8%
        // (4.4% per dot, 2 dots in total)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(unselectedColor, 7.5f..9f)

        // Check that background color exists
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(backgroundColor)
    }

    @Test
    fun horizontalPageIndicator_9_pages_sized_appropriately_circular() {
        val indicatorSize = PageIndicatorItemSize
        val spacing = PageIndicatorSpacing
        val padding = PaddingDefaults.edgePadding
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(true)) {
                Box(modifier = Modifier.size(150.dp)) {
                    HorizontalPageIndicator(
                        modifier = Modifier.testTag(TEST_TAG),
                        pagerState =
                            PagerState(
                                currentPage = 1,
                                currentPageOffsetFraction = 0.0f,
                                pageCount = { 9 }
                            ),
                    )
                }
            }
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assertWidthIsEqualTo((indicatorSize + spacing) * 6 + padding * 2)
        rule.onNodeWithTag(TEST_TAG).assertHeightIsEqualTo(indicatorSize * 2 + padding * 2)
    }

    @Test
    fun horizontalPageIndicator_3_pages_sized_appropriately_circular() {
        val indicatorSize = PageIndicatorItemSize
        val spacing = PageIndicatorSpacing
        val pagesCount = 3
        val padding = PaddingDefaults.edgePadding

        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(true)) {
                Box(modifier = Modifier.size(150.dp)) {
                    HorizontalPageIndicator(
                        modifier = Modifier.testTag(TEST_TAG),
                        pagerState =
                            PagerState(
                                currentPage = 1,
                                currentPageOffsetFraction = 0.0f,
                                pageCount = { pagesCount }
                            ),
                    )
                }
            }
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assertWidthIsEqualTo((indicatorSize + spacing) * pagesCount + padding * 2)
        rule.onNodeWithTag(TEST_TAG).assertHeightIsEqualTo(indicatorSize * 2 + padding * 2)
    }

    @Test
    fun horizontalPageIndicator_position_is_selected_circular_rtl() {
        horizontalPageIndicator_position_is_selected_circular(LayoutDirection.Rtl)
    }

    @Test
    fun verticalPageIndicator_position_is_selected_circular_rtl() {
        verticalPageIndicator_position_is_selected_circular(LayoutDirection.Rtl)
    }

    private fun horizontalPageIndicator_position_is_selected_circular(
        layoutDirection: LayoutDirection
    ) {
        rule.setContentWithTheme {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.RoundScreen(true)
                    .then(DeviceConfigurationOverride.LayoutDirection(layoutDirection))
            ) {
                Box(modifier = Modifier.size(150.dp)) {
                    HorizontalPageIndicator(
                        modifier = Modifier.testTag(TEST_TAG),
                        pagerState = pagerState_start,
                        selectedColor = selectedColor,
                        unselectedColor = unselectedColor,
                        backgroundColor = backgroundColor
                    )
                }
            }
        }
        rule.waitForIdle()

        // A selected dot with specified color should be visible on the screen, which is apprx 4.4%
        // (4.4% per dot, 1 dot in total)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(selectedColor, 4f..5f)
        // Unselected dots should also be visible on the screen, and should take around 13.2%
        // (4.4% per dot, 3 dots total)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(unselectedColor, 11f..16f)

        // Check that background color exists
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(backgroundColor)
    }

    private fun verticalPageIndicator_position_is_selected_circular(
        layoutDirection: LayoutDirection
    ) {
        rule.setContentWithTheme {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.RoundScreen(true)
                    .then(DeviceConfigurationOverride.LayoutDirection(layoutDirection))
            ) {
                Box(modifier = Modifier.size(150.dp)) {
                    VerticalPageIndicator(
                        modifier = Modifier.testTag(TEST_TAG),
                        pagerState = pagerState_start,
                        selectedColor = selectedColor,
                        unselectedColor = unselectedColor,
                        backgroundColor = backgroundColor
                    )
                }
            }
        }
        rule.waitForIdle()

        // A selected dot with specified color should be visible on the screen, which is apprx 4.4%
        // (4.% per dot, 1 dot in total)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(selectedColor, 3.5f..5.5f)
        // Unselected dots should also be visible on the screen, and should take around 13%
        // (4.4% per dot, 3 dots total)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(unselectedColor, 11f..14f)

        // Check that background color exists
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(backgroundColor)
    }

    private val pagerState_start =
        PagerState(
            currentPage = SELECTED_PAGE_INDEX,
            currentPageOffsetFraction = 0.0f,
            pageCount = { PAGE_COUNT }
        )

    private val pagerState_middle =
        PagerState(
            currentPage = SELECTED_PAGE_INDEX,
            currentPageOffsetFraction = 0.5f,
            pageCount = { PAGE_COUNT }
        )

    companion object {
        val selectedColor = Color.Yellow
        val unselectedColor = Color.Red
        val backgroundColor = Color.Green

        const val PAGE_COUNT = 4
        const val SELECTED_PAGE_INDEX = 1
    }
}
