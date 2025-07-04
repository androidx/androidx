/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.foundation.rotary

import android.os.Build
import android.view.ScrollFeedbackProvider
import android.view.ViewConfiguration
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performRotaryScrollInput
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollTest.Companion.TEST_TAG
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalTestApi::class)
class HapticsTest {
    @get:Rule val rule = createComposeRule()
    private val focusRequester = FocusRequester()

    @Test
    fun platformHaptics_scrollProgressCalled_once() {

        val mockedScrollFeedbackProvider = MockedScrollFeedbackProvider()
        rule.setContent { SLCRotaryFling(mockedScrollFeedbackProvider) }
        rule.runOnIdle { focusRequester.requestFocus() }

        rule.onNodeWithTag(TEST_TAG).performRotaryScrollInput { rotateToScrollVertically(10f) }

        Truth.assertThat(mockedScrollFeedbackProvider.onScrollProgressCounter).isEqualTo(1)
        Truth.assertThat(mockedScrollFeedbackProvider.onSnapToItemCounter).isEqualTo(0)
        Truth.assertThat(mockedScrollFeedbackProvider.onScrollLimitCounter).isEqualTo(0)
    }

    @Test
    fun platformHaptics_scrollProgressCalled_multipleTimes() {

        val mockedScrollFeedbackProvider = MockedScrollFeedbackProvider()
        rule.setContent { SLCRotaryFling(mockedScrollFeedbackProvider) }
        rule.runOnIdle { focusRequester.requestFocus() }

        rule.onNodeWithTag(TEST_TAG).performRotaryScrollInput {
            rotateToScrollVertically(10f)
            advanceEventTime(10)
            rotateToScrollVertically(10f)
            advanceEventTime(10)
            rotateToScrollVertically(10f)
        }

        Truth.assertThat(mockedScrollFeedbackProvider.onScrollProgressCounter).isEqualTo(3)
        Truth.assertThat(mockedScrollFeedbackProvider.onSnapToItemCounter).isEqualTo(0)
        Truth.assertThat(mockedScrollFeedbackProvider.onScrollLimitCounter).isEqualTo(0)
    }

    @Test
    fun platformHaptics_scrollLimitCalled() {

        val mockedScrollFeedbackProvider = MockedScrollFeedbackProvider()
        rule.setContent { SLCRotaryFling(mockedScrollFeedbackProvider) }
        rule.runOnIdle { focusRequester.requestFocus() }

        rule.onNodeWithTag(TEST_TAG).performRotaryScrollInput {
            // Scroll the rotary forwards and then backwards so that we'll reach the edge of the
            // list.
            rotateToScrollVertically(10f)
            advanceEventTime(10)
            rotateToScrollVertically(-11f)
        }

        Truth.assertThat(mockedScrollFeedbackProvider.onScrollLimitCounter).isEqualTo(1)
    }

    @Test
    fun platformHaptics_snapToItemCalled_once_highRes() {

        val mockedScrollFeedbackProvider = MockedScrollFeedbackProvider()
        rule.setContent { SLCHighResRotarySnap(mockedScrollFeedbackProvider) }
        rule.runOnIdle { focusRequester.requestFocus() }

        rule.onNodeWithTag(TEST_TAG).performRotaryScrollInput { rotateToScrollVertically(100f) }
        Truth.assertThat(mockedScrollFeedbackProvider.onSnapToItemCounter).isEqualTo(1)
    }

    @Test
    fun platformHaptics_snapToItemCalled_multipleTimes_highRes() {

        val mockedScrollFeedbackProvider = MockedScrollFeedbackProvider()
        rule.setContent { SLCHighResRotarySnap(mockedScrollFeedbackProvider) }
        rule.runOnIdle { focusRequester.requestFocus() }

        rule.onNodeWithTag(TEST_TAG).performRotaryScrollInput {
            rotateToScrollVertically(100f)
            advanceEventTime(10)
            rotateToScrollVertically(100f)
            advanceEventTime(10)
            rotateToScrollVertically(100f)
        }
        Truth.assertThat(mockedScrollFeedbackProvider.onSnapToItemCounter).isEqualTo(3)
    }

    @Test
    fun platformHaptics_snapToItemCalled_once_lowRes() {

        val mockedScrollFeedbackProvider = MockedScrollFeedbackProvider()
        rule.setContent { SLCLowResRotarySnap(mockedScrollFeedbackProvider) }
        rule.runOnIdle { focusRequester.requestFocus() }

        rule.onNodeWithTag(TEST_TAG).performRotaryScrollInput { rotateToScrollVertically(100f) }
        Truth.assertThat(mockedScrollFeedbackProvider.onSnapToItemCounter).isEqualTo(1)
    }

    @Test
    fun platformHaptics_snapToItemCalled_multipleTimes_lowRes() {

        val mockedScrollFeedbackProvider = MockedScrollFeedbackProvider()
        rule.setContent { SLCLowResRotarySnap(mockedScrollFeedbackProvider) }
        rule.runOnIdle { focusRequester.requestFocus() }

        rule.onNodeWithTag(TEST_TAG).performRotaryScrollInput {
            rotateToScrollVertically(100f)
            advanceEventTime(10)
            rotateToScrollVertically(100f)
            advanceEventTime(10)
            rotateToScrollVertically(100f)
        }
        Truth.assertThat(mockedScrollFeedbackProvider.onSnapToItemCounter).isEqualTo(3)
    }

    @Composable
    private fun SLCRotaryFling(scrollFeedbackProvider: ScrollFeedbackProvider) {
        val scrollableState = rememberScalingLazyListState()
        val viewConfiguration = ViewConfiguration.get(LocalContext.current)
        val flingBehavior = ScrollableDefaults.flingBehavior()

        ScalingLazyColumn(
            state = scrollableState,
            rotaryScrollableBehavior = null,
            modifier =
                Modifier.size(200.dp)
                    .testTag(TEST_TAG)
                    .rotaryScrollable(
                        behavior =
                            FlingRotaryScrollableBehavior(
                                isLowRes = false,
                                rotaryHaptics =
                                    PlatformRotaryHapticHandler(
                                        scrollableState,
                                        scrollFeedbackProvider,
                                    ),
                                rotaryFlingHandlerFactory = { inputDeviceId, initialTimestamp ->
                                    RotaryFlingHandler(
                                        scrollableState = scrollableState,
                                        flingBehavior = flingBehavior,
                                        viewConfiguration = viewConfiguration,
                                        flingTimeframe = 20,
                                        inputDeviceId = inputDeviceId,
                                        initialTimestamp = initialTimestamp,
                                    )
                                },
                                scrollHandlerFactory = { RotaryScrollHandler(scrollableState) },
                            ),
                        focusRequester = focusRequester,
                        reverseDirection = false,
                    ),
        ) {
            items(300) { BasicText(text = "Item #$it") }
        }
    }

    @Composable
    private fun SLCHighResRotarySnap(scrollFeedbackProvider: ScrollFeedbackProvider) {
        val scrollableState = rememberScalingLazyListState()

        val layoutInfoProvider =
            remember(scrollableState) {
                ScalingLazyColumnRotarySnapLayoutInfoProvider(scrollableState)
            }
        ScalingLazyColumn(
            state = scrollableState,
            // We need to switch off default rotary behavior
            rotaryScrollableBehavior = null,
            modifier =
                Modifier.size(200.dp)
                    .testTag(TEST_TAG)
                    .rotaryScrollable(
                        behavior =
                            HighResSnapRotaryScrollableBehavior(
                                rotaryHaptics =
                                    PlatformRotaryHapticHandler(
                                        scrollableState,
                                        scrollFeedbackProvider,
                                    ),
                                scrollDistanceDivider =
                                    RotarySnapSensitivity.DEFAULT.resistanceFactor,
                                thresholdHandlerFactory = {
                                    ThresholdHandler(
                                        RotarySnapSensitivity.DEFAULT.minThresholdDivider,
                                        RotarySnapSensitivity.DEFAULT.maxThresholdDivider,
                                    ) {
                                        50f
                                    }
                                },
                                snapHandlerFactory = {
                                    RotarySnapHandler(scrollableState, layoutInfoProvider, 0)
                                },
                                scrollHandlerFactory = { RotaryScrollHandler(scrollableState) },
                            ),
                        focusRequester = focusRequester,
                        reverseDirection = false,
                    ),
        ) {
            items(300) { BasicText(text = "Item #$it") }
        }
    }

    @Composable
    private fun SLCLowResRotarySnap(scrollFeedbackProvider: ScrollFeedbackProvider) {
        val scrollableState = rememberScalingLazyListState()
        val layoutInfoProvider =
            remember(scrollableState) {
                ScalingLazyColumnRotarySnapLayoutInfoProvider(scrollableState)
            }
        ScalingLazyColumn(
            state = scrollableState,
            // We need to switch off default rotary behavior
            rotaryScrollableBehavior = null,
            modifier =
                Modifier.size(200.dp)
                    .testTag(TEST_TAG)
                    .rotaryScrollable(
                        behavior =
                            LowResSnapRotaryScrollableBehavior(
                                rotaryHaptics =
                                    PlatformRotaryHapticHandler(
                                        scrollableState,
                                        scrollFeedbackProvider,
                                    ),
                                snapHandlerFactory = {
                                    RotarySnapHandler(scrollableState, layoutInfoProvider, 0)
                                },
                            ),
                        focusRequester = focusRequester,
                        reverseDirection = false,
                    ),
        ) {
            items(300) { BasicText(text = "Item #$it") }
        }
    }

    class MockedScrollFeedbackProvider() : ScrollFeedbackProvider {
        var onSnapToItemCounter = 0
        var onScrollLimitCounter = 0
        var onScrollProgressCounter = 0

        override fun onSnapToItem(inputDeviceId: Int, source: Int, axis: Int) {
            onSnapToItemCounter++
        }

        override fun onScrollLimit(inputDeviceId: Int, source: Int, axis: Int, isStart: Boolean) {
            onScrollLimitCounter++
        }

        override fun onScrollProgress(
            inputDeviceId: Int,
            source: Int,
            axis: Int,
            deltaInPixels: Int,
        ) {
            onScrollProgressCounter++
        }
    }
}
