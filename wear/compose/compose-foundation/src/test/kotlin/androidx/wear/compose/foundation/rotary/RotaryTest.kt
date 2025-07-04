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

import android.content.Context
import android.os.Build
import android.view.ViewConfiguration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.RotaryInjectionScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performRotaryScrollInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowViewConfiguration

@RunWith(JUnit4::class)
class ThresholdHandlerTest {

    @Test
    fun testMinVelocityThreshold() {
        val itemHeight = 100f
        val minThresholdDivider = 1f
        val maxThresholdDivider = 2f
        val thresholdHandler =
            ThresholdHandler(
                minThresholdDivider = minThresholdDivider,
                maxThresholdDivider = maxThresholdDivider,
                averageItemSize = { itemHeight },
            )

        thresholdHandler.startThresholdTracking(0L)
        // Simulate very slow scroll
        thresholdHandler.updateTracking(100L, 1f)
        val result = thresholdHandler.calculateSnapThreshold()

        // Threshold should be equal to the height of an item divided by minThresholdDivider
        assertEquals(itemHeight / minThresholdDivider, result, 0.01f)
    }

    @Test
    fun testMaxVelocityThreshold() {
        val itemHeight = 100f
        val minThresholdDivider = 1f
        val maxThresholdDivider = 2f
        val thresholdHandler =
            ThresholdHandler(
                minThresholdDivider = minThresholdDivider,
                maxThresholdDivider = maxThresholdDivider,
                averageItemSize = { itemHeight },
            )

        thresholdHandler.startThresholdTracking(0L)
        // Simulate very fast scroll
        thresholdHandler.updateTracking(1L, 100f)
        val result = thresholdHandler.calculateSnapThreshold()

        // Threshold should be equal to the height of an item divided by maxThresholdDivider
        assertEquals(itemHeight / maxThresholdDivider, result, 0.01f)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@RunWith(RobolectricTestRunner::class)
class RotaryFlingHandlerTest {

    val context = ApplicationProvider.getApplicationContext<Context>()
    val viewConfiguration = ViewConfiguration.get(context)

    @Test
    @Config(sdk = [33])
    fun testFlingVelocityCalled_api33() {
        val mockViewConfiguration: ViewConfiguration = spy(viewConfiguration)

        val mockScrollState: ScrollableState = mock {}
        val mockFlingBehavior: FlingBehavior = mock {}
        val rotaryFlingHandler =
            RotaryFlingHandler(
                scrollableState = mockScrollState,
                flingBehavior = mockFlingBehavior,
                flingTimeframe = 100,
                viewConfiguration = mockViewConfiguration,
                inputDeviceId = 0,
                initialTimestamp = 0,
            )
        rotaryFlingHandler.observeEvent(0, 0f)

        // Verifying that the proper methods were called
        verify(mockViewConfiguration).scaledMaximumFlingVelocity
        verify(mockViewConfiguration).scaledMinimumFlingVelocity
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    @Config(sdk = [34])
    fun testFlingVelocityCalled_api34() {
        val mockViewConfiguration: ViewConfiguration = spy(viewConfiguration)

        val mockScrollState: ScrollableState = mock {}
        val mockFlingBehavior: FlingBehavior = mock {}
        val rotaryFlingHandler =
            RotaryFlingHandler(
                scrollableState = mockScrollState,
                flingBehavior = mockFlingBehavior,
                flingTimeframe = 100,
                viewConfiguration = mockViewConfiguration,
                inputDeviceId = 0,
                initialTimestamp = 0,
            )
        rotaryFlingHandler.observeEvent(0, 0f)

        // Verifying that the proper methods were called
        verify(mockViewConfiguration).getScaledMaximumFlingVelocity(any(), any(), any())
        verify(mockViewConfiguration).getScaledMinimumFlingVelocity(any(), any(), any())
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    @Config(sdk = [33])
    fun flingIsTriggered() = runTest {
        val mockViewConfiguration: ViewConfiguration =
            spy(viewConfiguration) {
                on { scaledMaximumFlingVelocity }.doReturn(1000)
                // Setting very low threshold for Fling to trigger
                on { scaledMinimumFlingVelocity }.doReturn(1)
            }

        var scrollIncrement = 0f
        val scrollState = ScrollableState {
            // Increment scrollIncrement by the scroll amount
            scrollIncrement += it
            it
        }

        val flingBehavior: FlingBehavior =
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    // Scroll by the fixed amount. It doesn't matter how much we scroll - we just
                    // want to test that the scrollState is changes.
                    scrollBy(10f)
                    return initialVelocity
                }
            }
        val rotaryFlingHandler =
            RotaryFlingHandler(
                scrollableState = scrollState,
                flingBehavior = flingBehavior,
                flingTimeframe = 10,
                viewConfiguration = mockViewConfiguration,
                inputDeviceId = 0,
                initialTimestamp = 0,
            )

        // Sending events to simulate rotary scroll
        rotaryFlingHandler.observeEvent(1, 10f)
        rotaryFlingHandler.observeEvent(2, 10f)
        rotaryFlingHandler.observeEvent(3, 10f)

        var beforeFlingCalled = false
        rotaryFlingHandler.performFlingIfRequired(
            this,
            { beforeFlingCalled = true },
            RotaryScrollLogic(null, null, false),
            {},
        )

        delay(1000L)

        assert(scrollIncrement > 0f)
        assert(beforeFlingCalled)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    @Config(sdk = [33])
    fun flingIsNotTriggered() = runTest {
        val mockViewConfiguration: ViewConfiguration =
            spy(viewConfiguration) {
                on { scaledMaximumFlingVelocity }.doReturn(10000)
                // Setting very high threshold for Fling to trigger
                on { scaledMinimumFlingVelocity }.doReturn(10000)
            }

        var scrollIncrement = 0f
        val scrollState = ScrollableState {
            // Increment scrollIncrement by the scroll amount
            scrollIncrement += it
            it
        }

        val flingBehavior: FlingBehavior =
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    // Scroll by the fixed amount.
                    if (initialVelocity != 0f) {
                        scrollBy(10f)
                    }
                    return initialVelocity
                }
            }
        val rotaryFlingHandler =
            RotaryFlingHandler(
                scrollableState = scrollState,
                flingBehavior = flingBehavior,
                flingTimeframe = 10,
                viewConfiguration = mockViewConfiguration,
                inputDeviceId = 0,
                initialTimestamp = 0,
            )

        // Sending events to simulate rotary scroll
        rotaryFlingHandler.observeEvent(1, 10f)
        rotaryFlingHandler.observeEvent(2, 10f)
        rotaryFlingHandler.observeEvent(3, 10f)

        var beforeFlingCalled = false
        rotaryFlingHandler.performFlingIfRequired(
            this,
            { beforeFlingCalled = true },
            RotaryScrollLogic(null, null, false),
            {},
        )

        delay(1000L)

        assertEquals(0f, scrollIncrement)
        assert(!beforeFlingCalled)
    }
}

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
class RotaryFlingTest {
    @get:Rule val rule = createComposeRule()
    private val focusRequester = FocusRequester()

    private lateinit var state: LazyListState

    private var itemSizePx: Float = 50f
    private var itemSizeDp: Dp = Dp.Infinity

    @Before
    fun before() {
        with(rule.density) { itemSizeDp = itemSizePx.toDp() }
    }

    @Test
    @Config(shadows = [MyShadowViewConfiguration::class])
    fun fast_scroll_with_fling_high_res() {
        fast_scroll_with_fling(lowRes = false)
    }

    @Test
    @Config(shadows = [MyShadowViewConfiguration::class])
    fun fast_scroll_with_fling_low_res() {
        fast_scroll_with_fling(lowRes = true)
    }

    private fun fast_scroll_with_fling(lowRes: Boolean) {
        var itemIndex = 0

        testScroll(
            beforeScroll = { itemIndex = state.firstVisibleItemIndex },
            rotaryAction = {
                // To produce fling we need to send 3 events,
                // which will be increasing the scroll velocity.
                // First event initializes velocityTracker
                rotateToScrollVertically(itemSizePx)
                advanceEventTime(10)
                // Next 2 events should increase the scroll velocity.
                rotateToScrollVertically(itemSizePx * 5)
                advanceEventTime(10)
                rotateToScrollVertically(itemSizePx * 6)
            },
            lowRes = lowRes,
        )

        rule.runOnIdle {
            // We check that we indeed scrolled the list further than
            // amount of pixels which we scrolled by.
            Truth.assertThat(state.firstVisibleItemIndex).isGreaterThan(itemIndex + 12)
        }
    }

    private fun testScroll(
        beforeScroll: () -> Unit,
        rotaryAction: RotaryInjectionScope.() -> Unit,
        lowRes: Boolean,
    ) {
        rule.setContent {
            state = rememberLazyListState()

            MockRotaryResolution(lowRes = lowRes) {
                LazyColumn(
                    modifier =
                        Modifier.size(200.dp)
                            .testTag(TEST_TAG)
                            .rotaryScrollable(
                                RotaryScrollableDefaults.behavior(state),
                                focusRequester,
                            ),
                    state = state,
                ) {
                    items(300) {
                        BasicText(modifier = Modifier.height(itemSizeDp), text = "Item #$it")
                    }
                }
            }
        }
        rule.runOnIdle { focusRequester.requestFocus() }
        beforeScroll()
        rule.onNodeWithTag(TEST_TAG).performRotaryScrollInput { rotaryAction() }
    }

    @Composable
    internal fun MockRotaryResolution(lowRes: Boolean = false, content: @Composable () -> Unit) {
        val context = LocalContext.current

        // Mocking low-res flag
        val mockContext = Mockito.spy(context)
        val mockPackageManager = Mockito.spy(context.packageManager)
        `when`(mockPackageManager.hasSystemFeature("android.hardware.rotaryencoder.lowres"))
            .thenReturn(lowRes)

        Mockito.doReturn(mockPackageManager).`when`(mockContext).packageManager

        CompositionLocalProvider(LocalContext provides mockContext) { content() }
    }

    val TEST_TAG = "test-tag"
}

/**
 * Mocking new getScaledMinimumFlingVelocity and getScaledMaximumFlingVelocity methods for
 * predictable results.
 */
@Implements(ViewConfiguration::class)
internal class MyShadowViewConfiguration : ShadowViewConfiguration() {
    @Implementation
    @Suppress("UNUSED_PARAMETER")
    fun getScaledMinimumFlingVelocity(inputDeviceId: Int, axis: Int, source: Int): Int {
        return 50
    }

    @Implementation
    @Suppress("UNUSED_PARAMETER")
    fun getScaledMaximumFlingVelocity(inputDeviceId: Int, axis: Int, source: Int): Int {
        return 8000
    }
}
