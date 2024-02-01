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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.material3.HorizontalPageIndicatorTest.Companion.pageIndicatorState
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class HorizontalPageIndicatorScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule
    val testName = TestName()

    @Test
    fun horizontalPageIndicator_circular_selected_page() {
        selected_page(true, LayoutDirection.Ltr)
    }

    @Test
    fun horizontalPageIndicator_linear_selected_page() {
        selected_page(false, LayoutDirection.Ltr)
    }

    @Test
    fun horizontalPageIndicator_circular_selected_page_rtl() {
        selected_page(true, LayoutDirection.Rtl)
    }

    @Test
    fun horizontalPageIndicator_linear_selected_page_rtl() {
        selected_page(false, LayoutDirection.Rtl)
    }

    @Test
    fun horizontalPageIndicator_circular_between_pages() {
        between_pages(true)
    }

    @Test
    fun horizontalPageIndicator_linear_between_pages() {
        between_pages(false)
    }

    private fun selected_page(
        isRound: Boolean,
        layoutDirection: LayoutDirection
    ) {
        rule.setContentWithTheme {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(layoutDirection)
            ) {
                defaultHorizontalPageIndicator(isRound)
            }
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }

    private fun between_pages(isRound: Boolean) {
        rule.setContentWithTheme {
            DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(isRound)) {
                HorizontalPageIndicator(
                    modifier = Modifier
                        .testTag(TEST_TAG)
                        .size(200.dp),
                    pageIndicatorState = pageIndicatorState(0.5f),
                    selectedColor = Color.Yellow,
                    unselectedColor = Color.Red,
                    indicatorSize = 15.dp
                )
            }
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }

    @Composable
    private fun defaultHorizontalPageIndicator(isRound: Boolean) {
        DeviceConfigurationOverride(DeviceConfigurationOverride.RoundScreen(isRound)) {
            HorizontalPageIndicator(
                modifier = Modifier
                    .testTag(TEST_TAG)
                    .size(200.dp),
                pageIndicatorState = pageIndicatorState(),
                selectedColor = Color.Yellow,
                unselectedColor = Color.Red,
                indicatorSize = 15.dp
            )
        }
    }
}
