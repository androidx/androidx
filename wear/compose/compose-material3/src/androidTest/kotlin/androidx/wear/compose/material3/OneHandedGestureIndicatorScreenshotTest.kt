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

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.wear.compose.material3.onehandedgesture.INDICATOR_ANIMATION_START_DELAY_MILLIS
import androidx.wear.compose.material3.onehandedgesture.LocalOneHandedGestureManager
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureAction
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureClickIndicator
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureClickIndicatorState
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureConfiguration
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureHorizontalPageIndicator
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureIndicatorSize
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureManagerImpl
import androidx.wear.compose.material3.onehandedgesture.OneHandedGesturePageIndicatorState
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureScrollIndicator
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureScrollIndicatorState
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureVerticalPageIndicator
import androidx.wear.compose.material3.onehandedgesture.SdkGestureInputManager
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(TestParameterInjector::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class OneHandedGestureIndicatorScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun oneHandedGesture_indicator(
        @TestParameter wrist: Wrist,
        @TestParameter layoutDirection: LayoutDirection,
        @TestParameter gestureAction: GestureActions,
    ) {
        val gestureConfig =
            OneHandedGestureConfiguration(action = gestureAction.action, gestureId = "key")
        val indicatorState = OneHandedGestureClickIndicatorState()

        verifyOneHandedGestureContentScreenshot(
            activate = { launch { indicatorState.showIndicator() } },
            testName = testName,
            screenshotRule = screenshotRule,
            wrist = wrist,
            layoutDirection = layoutDirection,
        ) {
            CompositionLocalProvider(LocalContentColor provides Color.Black) {
                OneHandedGestureClickIndicator(
                    gestureConfiguration = gestureConfig,
                    state = indicatorState,
                    modifier = Modifier.testTag(TEST_TAG),
                ) {
                    Icon(
                        imageVector = Icons.Check,
                        contentDescription = "",
                        modifier = Modifier.size(OneHandedGestureIndicatorSize.Medium.size),
                    )
                }
            }
        }
    }

    @Test
    fun oneHandedGesture_scroll_indicator(
        @TestParameter wrist: Wrist,
        @TestParameter layoutDirection: LayoutDirection,
        @TestParameter gestureAction: GestureActions,
    ) {
        val gestureConfig =
            OneHandedGestureConfiguration(action = gestureAction.action, gestureId = "key")
        lateinit var indicatorState: OneHandedGestureScrollIndicatorState

        verifyOneHandedGestureContentScreenshot(
            activate = { launch { indicatorState.showIndicator() } },
            testName = testName,
            screenshotRule = screenshotRule,
            layoutDirection = layoutDirection,
            wrist = wrist,
            {
                indicatorState = remember(gestureConfig) { OneHandedGestureScrollIndicatorState() }

                Box(modifier = Modifier.testTag(TEST_TAG)) {
                    OneHandedGestureScrollIndicator(
                        gestureConfiguration = gestureConfig,
                        indicatorState = indicatorState,
                        scrollState = rememberTransformingLazyColumnState(),
                    )
                }
            },
        )
    }

    @Test
    fun oneHandedGesture_slc_scroll_indicator(
        @TestParameter wrist: Wrist,
        @TestParameter layoutDirection: LayoutDirection,
        @TestParameter gestureAction: GestureActions,
    ) {
        val gestureConfig =
            OneHandedGestureConfiguration(action = gestureAction.action, gestureId = "key")
        lateinit var indicatorState: OneHandedGestureScrollIndicatorState

        verifyOneHandedGestureContentScreenshot(
            activate = { launch { indicatorState.showIndicator() } },
            testName = testName,
            screenshotRule = screenshotRule,
            layoutDirection = layoutDirection,
            wrist = wrist,
            {
                indicatorState = remember(gestureConfig) { OneHandedGestureScrollIndicatorState() }

                Box(modifier = Modifier.testTag(TEST_TAG)) {
                    OneHandedGestureScrollIndicator(
                        gestureConfiguration = gestureConfig,
                        indicatorState = indicatorState,
                        scrollState = rememberScalingLazyListState(),
                    )
                }
            },
        )
    }

    @Test
    fun oneHandedGesture_horizontal_page_indicator(
        @TestParameter wrist: Wrist,
        @TestParameter layoutDirection: LayoutDirection,
        @TestParameter gestureAction: GestureActions,
    ) {
        val gestureConfig =
            OneHandedGestureConfiguration(action = gestureAction.action, gestureId = "key")
        val indicatorState = OneHandedGesturePageIndicatorState()
        verifyOneHandedGestureContentScreenshot(
            activate = { launch { indicatorState.showIndicator() } },
            testName = testName,
            screenshotRule = screenshotRule,
            layoutDirection = layoutDirection,
            wrist = wrist,
            {
                Box(modifier = Modifier.testTag(TEST_TAG)) {
                    OneHandedGestureHorizontalPageIndicator(
                        gestureConfiguration = gestureConfig,
                        indicatorState = indicatorState,
                        pagerState = rememberPagerState { 0 },
                    )
                }
            },
        )
    }

    @Test
    fun oneHandedGesture_vertical_page_indicator(
        @TestParameter wrist: Wrist,
        @TestParameter layoutDirection: LayoutDirection,
        @TestParameter gestureAction: GestureActions,
    ) {
        val gestureConfig =
            OneHandedGestureConfiguration(action = gestureAction.action, gestureId = "key")
        val indicatorState = OneHandedGesturePageIndicatorState()
        verifyOneHandedGestureContentScreenshot(
            activate = { launch { indicatorState.showIndicator() } },
            testName = testName,
            screenshotRule = screenshotRule,
            layoutDirection = layoutDirection,
            wrist = wrist,
            {
                Box(modifier = Modifier.testTag(TEST_TAG)) {
                    OneHandedGestureVerticalPageIndicator(
                        gestureConfiguration = gestureConfig,
                        indicatorState = indicatorState,
                        pagerState = rememberPagerState { 0 },
                    )
                }
            },
        )
    }

    private fun verifyOneHandedGestureContentScreenshot(
        activate: CoroutineScope.() -> Unit,
        testName: TestName,
        screenshotRule: AndroidXScreenshotTestRule,
        layoutDirection: LayoutDirection,
        @TestParameter wrist: Wrist,
        content: @Composable () -> Unit,
    ) {
        lateinit var scope: CoroutineScope
        rule.mainClock.autoAdvance = false

        rule.setContentWithTheme {
            scope = rememberCoroutineScope()
            val gestureManager =
                remember(scope) { OneHandedGestureManagerImpl(scope, SdkGestureInputManagerMock()) }

            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDirection,
                LocalWristOrientation provides wrist.toWristOrientation(),
                LocalOneHandedGestureManager provides gestureManager,
                content = { Box(modifier = Modifier.fillMaxSize()) { content() } },
            )
        }

        rule.waitForIdle()
        scope.activate()
        // Advance alpha animation of gesture indicator. After this, gesture should be fully visible
        rule.mainClock.advanceTimeBy(INDICATOR_ANIMATION_START_DELAY_MILLIS)

        rule.verifyScreenshot(
            testName,
            screenshotRule,
            testTagNode = rule.onAllNodes(hasTestTag(TEST_TAG), true).onFirst(),
        )
    }

    private class SdkGestureInputManagerMock : SdkGestureInputManager {
        override fun isAvailable(context: Context): Boolean = true

        override fun subscribeToSdkGestureAction(
            view: View,
            sdkGestureAction: Int,
            enabledInAmbient: Boolean,
            onGesture: (Int) -> Unit,
        ) {}

        override fun unsubscribeFromSdkGestureAction(view: View, sdkGestureAction: Int) {}

        override fun notifyGestureConsumed(gestureId: String, sdkGestureAction: Int) {}

        override fun shouldShowIndicator(
            gestureId: String,
            sdkGestureAction: Int,
            isOverlay: Boolean,
        ): Boolean = true

        override fun notifyIndicatorShown(gestureId: String, sdkGestureAction: Int) {}
    }

    internal fun Wrist.toWristOrientation(): WristOrientation =
        if (this == Wrist.LEFT_WRIST) WristOrientation.LEFT_WRIST_ROTATION_0
        else WristOrientation.RIGHT_WRIST_ROTATION_0

    enum class Wrist {
        LEFT_WRIST,
        RIGHT_WRIST,
    }

    enum class GestureActions(val action: OneHandedGestureAction) {
        Primary(OneHandedGestureAction.Primary),
        Dismiss(OneHandedGestureAction.Dismiss),
    }
}
