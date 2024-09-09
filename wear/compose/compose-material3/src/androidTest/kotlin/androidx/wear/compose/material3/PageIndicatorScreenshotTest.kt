/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.LayoutDirection
import androidx.compose.ui.test.RoundScreen
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.material3.PageIndicatorTest.Companion.PAGE_COUNT
import androidx.wear.compose.material3.PageIndicatorTest.Companion.SELECTED_PAGE_INDEX
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(TestParameterInjector::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class PageIndicatorScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun horizontalPageIndicator_selected_page(
        @TestParameter screenSize: ScreenSize,
        @TestParameter shape: ScreenShape
    ) {
        verifyPageIndicator(shape.isRound, true, LayoutDirection.Ltr, screenSize)
    }

    @Test
    fun verticalPageIndicator_selected_page(
        @TestParameter screenSize: ScreenSize,
        @TestParameter shape: ScreenShape
    ) {
        verifyPageIndicator(shape.isRound, false, LayoutDirection.Ltr, screenSize)
    }

    @Test
    fun horizontalPageIndicator_selected_page_rtl(
        @TestParameter screenSize: ScreenSize,
        @TestParameter shape: ScreenShape
    ) {
        verifyPageIndicator(shape.isRound, true, LayoutDirection.Rtl, screenSize)
    }

    @Test
    fun verticalPageIndicator_selected_page_rtl(
        @TestParameter screenSize: ScreenSize,
        @TestParameter shape: ScreenShape
    ) {
        verifyPageIndicator(shape.isRound, false, LayoutDirection.Rtl, screenSize)
    }

    @Test
    fun horizontalPageIndicator_between_pages(
        @TestParameter screenSize: ScreenSize,
        @TestParameter shape: ScreenShape
    ) {
        verifyPageIndicator(
            shape.isRound,
            true,
            LayoutDirection.Ltr,
            screenSize,
            offsetFraction = 0.5f,
        )
    }

    @Test
    fun verticalPageIndicator_between_pages(
        @TestParameter screenSize: ScreenSize,
        @TestParameter shape: ScreenShape
    ) {
        verifyPageIndicator(
            shape.isRound,
            false,
            LayoutDirection.Ltr,
            screenSize,
            offsetFraction = 0.5f
        )
    }

    @Test
    fun horizontalPageIndicator_circular_9_pages(@TestParameter screenSize: ScreenSize) {
        verifyPageIndicator(
            true,
            true,
            LayoutDirection.Ltr,
            screenSize,
            pageCount = 9,
            selectedPageIndex = 6
        )
    }

    @Test
    fun verticalPageIndicator_circular_9_pages(@TestParameter screenSize: ScreenSize) {
        verifyPageIndicator(
            true,
            false,
            LayoutDirection.Ltr,
            screenSize,
            pageCount = 9,
            selectedPageIndex = 6
        )
    }

    // TODO(b/369535289) Add tests for linear page indicator with 9 pages

    private fun verifyPageIndicator(
        isRound: Boolean,
        isHorizontal: Boolean,
        layoutDirection: LayoutDirection,
        screenSize: ScreenSize = ScreenSize.SMALL,
        offsetFraction: Float = 0.0f,
        pageCount: Int = PAGE_COUNT,
        selectedPageIndex: Int = SELECTED_PAGE_INDEX
    ) {
        rule.setContentWithTheme {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(layoutDirection)
            ) {
                PageIndicator(
                    isRound,
                    isHorizontal,
                    offsetFraction,
                    screenSize,
                    pageCount,
                    selectedPageIndex
                )
            }
        }
        rule.waitForIdle()

        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.goldenIdentifier())
    }

    @Composable
    private fun PageIndicator(
        isRound: Boolean,
        isHorizontal: Boolean,
        offsetFraction: Float,
        screenSize: ScreenSize,
        pageCount: Int,
        selectedPageIndex: Int
    ) {
        DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(isRound)) {
            ScreenConfiguration(screenSize.size) {
                Box(modifier = Modifier.testTag(TEST_TAG).fillMaxSize().background(Color.White)) {
                    val pagerState =
                        PagerState(
                            currentPage = selectedPageIndex,
                            currentPageOffsetFraction = offsetFraction,
                            pageCount = { pageCount }
                        )
                    if (isHorizontal) {
                        HorizontalPageIndicator(pagerState = pagerState)
                    } else {
                        VerticalPageIndicator(pagerState = pagerState)
                    }
                }
            }
        }
    }
}
