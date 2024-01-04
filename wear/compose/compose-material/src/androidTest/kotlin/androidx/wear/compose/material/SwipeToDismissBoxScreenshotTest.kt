/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.material

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalTestApi::class)
class SwipeToDismissBoxScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule
    val testName = TestName()

    @Test
    fun swiped_to_right_25_percent_ltr() {
        verifySwipedScreenshot(LayoutDirection.Ltr, 0.25f)
    }

    @Test
    fun swiped_to_right_25_percent_rtl() {
        verifySwipedScreenshot(LayoutDirection.Rtl, 0.25f)
    }

    @Test
    fun swiped_to_right_50_percent_ltr() {
        verifySwipedScreenshot(LayoutDirection.Ltr, 0.5f)
    }

    @Test
    fun swiped_to_right_50_percent_rtl() {
        verifySwipedScreenshot(LayoutDirection.Rtl, 0.5f)
    }

    @Test
    fun on_dismiss_overload_swiped_to_right_25_percent_ltr() {
        verifySwipedScreenshot(LayoutDirection.Ltr, 0.25f, true,
            "swiped_to_right_25_percent_ltr")
    }

    @Test
    fun on_dismiss_overload_swiped_to_right_25_percent_rtl() {
        verifySwipedScreenshot(LayoutDirection.Rtl, 0.25f, true,
            "swiped_to_right_25_percent_rtl")
    }

    @Test
    fun on_dismiss_overload_swiped_to_right_50_percent_ltr() {
        verifySwipedScreenshot(LayoutDirection.Ltr, 0.5f, true,
            "swiped_to_right_50_percent_ltr")
    }

    @Test
    fun on_dismiss_overload_swiped_to_right_50_percent_rtl() {
        verifySwipedScreenshot(LayoutDirection.Rtl, 0.5f, true,
            "swiped_to_right_50_percent_rtl")
    }

    private fun verifySwipedScreenshot(
        layoutDirection: LayoutDirection,
        swipedPercentage: Float,
        isOnDismissOverload: Boolean = false,
        goldenIdentifier: String = testName.methodName
    ) {
        val screenShotSizeDp = SCREENSHOT_SIZE.dp
        rule.setContentWithTheme {
            val originalConfiguration = LocalConfiguration.current
            val fixedScreenSizeConfiguration = remember(originalConfiguration) {
                Configuration(originalConfiguration).apply {
                    screenWidthDp = SCREENSHOT_SIZE
                    screenHeightDp = SCREENSHOT_SIZE
                }
            }

            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDirection,
                LocalConfiguration provides fixedScreenSizeConfiguration
            ) {
                val state = rememberSwipeToDismissBoxState()
                if (isOnDismissOverload) {
                    SwipeToDismissBox(
                        onDismissed = {},
                        modifier = Modifier
                            .testTag(TEST_TAG)
                            .size(screenShotSizeDp),
                        state = state
                    ) { isBackground ->
                        boxContent(isBackground = isBackground)
                    }
                } else {
                    SwipeToDismissBox(
                        modifier = Modifier
                            .testTag(TEST_TAG)
                            .size(screenShotSizeDp),
                        state = state
                    ) { isBackground ->
                        boxContent(isBackground = isBackground)
                    }
                }
            }
        }
        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            down(Offset(x = 0f, y = height / 2f))
            moveTo(Offset(x = width * swipedPercentage, y = height / 2f))
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }

    @Composable
    private fun BoxScope.boxContent(isBackground: Boolean) {
        if (isBackground) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                Text(color = Color.White, text = "Background")
            }
        } else {
            Box(modifier = Modifier.align(Alignment.Center)) {
                Text(color = Color.White, text = "Foreground")
            }
        }
    }

    private val SCREENSHOT_SIZE = 106
}
