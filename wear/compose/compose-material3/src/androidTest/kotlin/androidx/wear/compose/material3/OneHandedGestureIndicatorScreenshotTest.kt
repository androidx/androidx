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

package androidx.wear.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.internal.Icons
import androidx.wear.compose.material3.internal.LocalWristOrientation
import androidx.wear.compose.material3.internal.WristOrientation
import androidx.wear.compose.material3.onehandedgesture.GestureIndicatorSize
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureHorizontalPageIndicator
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureIndicator
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureScrollIndicator
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureVerticalPageIndicator
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(TestParameterInjector::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class OneHandedGestureIndicatorScreenshotTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun oneHandedGesture_indicator(
        @TestParameter wrist: Wrist,
        @TestParameter layoutDirection: LayoutDirection,
    ) {
        verifyOneHandedGestureContentScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            wrist = wrist,
            layoutDirection = layoutDirection,
        ) {
            OneHandedGestureIndicator(
                gestureIndicatorVisible = true,
                onGestureIndicatorFinished = {},
                modifier = Modifier.testTag(TEST_TAG),
            ) {
                Icon(
                    imageVector = Icons.Check,
                    contentDescription = "",
                    modifier = Modifier.size(GestureIndicatorSize.Medium.size),
                )
            }
        }
    }

    @Test
    fun oneHandedGesture_scroll_indicator(
        @TestParameter wrist: Wrist,
        @TestParameter layoutDirection: LayoutDirection,
    ) {
        verifyOneHandedGestureContentScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            layoutDirection = layoutDirection,
            wrist = wrist,
        ) {
            Box(modifier = Modifier.testTag(TEST_TAG)) {
                OneHandedGestureScrollIndicator(
                    gestureIndicatorVisible = true,
                    onGestureIndicatorFinished = {},
                    state = rememberTransformingLazyColumnState(),
                )
            }
        }
    }

    @Test
    fun oneHandedGesture_slc_scroll_indicator(
        @TestParameter wrist: Wrist,
        @TestParameter layoutDirection: LayoutDirection,
    ) {
        verifyOneHandedGestureContentScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            layoutDirection = layoutDirection,
            wrist = wrist,
        ) {
            Box(modifier = Modifier.testTag(TEST_TAG)) {
                OneHandedGestureScrollIndicator(
                    gestureIndicatorVisible = true,
                    onGestureIndicatorFinished = {},
                    state = rememberScalingLazyListState(),
                )
            }
        }
    }

    @Test
    fun oneHandedGesture_horizontal_page_indicator(
        @TestParameter wrist: Wrist,
        @TestParameter layoutDirection: LayoutDirection,
    ) {
        verifyOneHandedGestureContentScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            layoutDirection = layoutDirection,
            wrist = wrist,
        ) {
            Box(modifier = Modifier.testTag(TEST_TAG)) {
                OneHandedGestureHorizontalPageIndicator(
                    gestureIndicatorVisible = true,
                    onGestureIndicatorFinished = {},
                    pagerState = rememberPagerState { 0 },
                )
            }
        }
    }

    @Test
    fun oneHandedGesture_vertical_page_indicator(
        @TestParameter wrist: Wrist,
        @TestParameter layoutDirection: LayoutDirection,
    ) {
        verifyOneHandedGestureContentScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            layoutDirection = layoutDirection,
            wrist = wrist,
        ) {
            Box(modifier = Modifier.testTag(TEST_TAG)) {
                OneHandedGestureVerticalPageIndicator(
                    gestureIndicatorVisible = true,
                    onGestureIndicatorFinished = {},
                    pagerState = rememberPagerState { 0 },
                )
            }
        }
    }

    private fun verifyOneHandedGestureContentScreenshot(
        testName: TestName,
        screenshotRule: AndroidXScreenshotTestRule,
        layoutDirection: LayoutDirection,
        @TestParameter wrist: Wrist,
        content: @Composable () -> Unit,
    ) {
        rule.setContentWithTheme {
            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDirection,
                LocalWristOrientation provides wrist.toWristOrientation(),
                content = content,
            )
        }

        rule.waitForIdle()

        rule.verifyScreenshot(
            testName,
            screenshotRule,
            testTagNode = rule.onAllNodes(hasTestTag(TEST_TAG), true).onFirst(),
        )
    }

    internal fun Wrist.toWristOrientation(): WristOrientation =
        if (this == Wrist.LEFT_WRIST) WristOrientation.LEFT_WRIST_ROTATION_0
        else WristOrientation.RIGHT_WRIST_ROTATION_0

    enum class Wrist {
        LEFT_WRIST,
        RIGHT_WRIST,
    }
}
