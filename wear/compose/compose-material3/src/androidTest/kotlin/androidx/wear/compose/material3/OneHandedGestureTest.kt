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

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.material3.onehandedgesture.INDICATOR_ANIMATION_START_DELAY_MILLIS
import androidx.wear.compose.material3.onehandedgesture.LocalOneHandedGestureEnabled
import androidx.wear.compose.material3.onehandedgesture.LocalOneHandedGestureManager
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureAction
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureClickIndicator
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureClickIndicatorState
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureConfiguration
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureDefaults
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureHorizontalPageIndicator
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureIndicatorSize
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureManager
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureManagerImpl
import androidx.wear.compose.material3.onehandedgesture.OneHandedGesturePageIndicatorState
import androidx.wear.compose.material3.onehandedgesture.OneHandedGesturePriority
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureScrollIndicator
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureScrollIndicatorState
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureVerticalPageIndicator
import androidx.wear.compose.material3.onehandedgesture.SdkGestureInputManager
import androidx.wear.compose.material3.onehandedgesture.oneHandedGesture
import androidx.wear.compose.material3.onehandedgesture.rememberOneHandedGestureConfiguration
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.hamcrest.Matchers.startsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(TestParameterInjector::class)
class OneHandedGestureTest {
    @get:Rule val rule = createComposeRule()

    /** Verifies simple primary gesture */
    @Test
    fun simple_primary_gesture() {
        var gestured = false
        var indicatorShown = false
        var pressCoordinates: Offset = Offset.Zero
        var textSize: IntSize = IntSize.Zero
        val sdkGestureInputManager = SdkGestureInputManagerMock()
        val hapticResults = mutableMapOf<HapticFeedbackType, Int>()
        val gestureLabel = "click"

        rule.setContentWithTheme {
            val interactionSource = remember { MutableInteractionSource() }
            val gestureConfig =
                rememberOneHandedGestureConfiguration(action = OneHandedGestureAction.Primary)
            val indicatorState = remember { OneHandedGestureClickIndicatorState() }
            val coroutineScope = rememberCoroutineScope()

            MockSdkGestureInputManager(sdkGestureInputManager, hapticResults) {
                OneHandedGestureClickIndicator(
                    gestureConfiguration = gestureConfig,
                    state = indicatorState,
                ) {
                    Text(
                        "Gesturable",
                        modifier =
                            Modifier.onSizeChanged { textSize = it }
                                .oneHandedGesture(
                                    gestureConfiguration = gestureConfig,
                                    interactionSource = interactionSource,
                                    onGestureLabel = gestureLabel,
                                    onGestureAvailable = {
                                        coroutineScope.launch {
                                            indicatorState.showIndicator()
                                            indicatorShown = true
                                        }
                                    },
                                ) {
                                    gestured = true
                                },
                    )
                }
            }

            interactionSource.ListenForInteractions(onPressInteraction = { pressCoordinates = it })
        }

        // It takes at least a second for indicator to be shown. Fast-forward 3s to allow some delay
        rule.mainClock.advanceTimeBy(3000)

        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle {
            assertEquals(true, gestured)
            assertEquals(true, indicatorShown)
            assertEquals(textSize.center.toOffset(), pressCoordinates)

            assertThat(hapticResults).hasSize(1)
            assertEquals(hapticResults[HapticFeedbackType.LongPress], 1)
        }

        // Verify that correct content description is set for a11y
        val context = ApplicationProvider.getApplicationContext<Context>()
        val expectedText =
            context.getString(
                R.string.one_handed_gesture_primary_action_accessibility_text,
                gestureLabel,
            )
        onView(withContentDescription(expectedText)).checkExists()
    }

    /** Verifies that gesture isn't triggered when LocalOneHandedGestureEnabled is false */
    @Test
    fun local_composition_disable_gesture() {
        var gestured = false
        val sdkGestureInputManager = SdkGestureInputManagerMock()
        val hapticResults = mutableMapOf<HapticFeedbackType, Int>()
        val gestureLabel = "click"

        rule.setContentWithTheme {
            MockSdkGestureInputManager(sdkGestureInputManager, hapticResults) {
                // Disable gestures with LocalOneHandedGestureEnabled
                CompositionLocalProvider(LocalOneHandedGestureEnabled provides false) {
                    val gestureConfig =
                        rememberOneHandedGestureConfiguration(
                            action = OneHandedGestureAction.Primary
                        )
                    Text(
                        "Gesturable",
                        modifier =
                            Modifier.oneHandedGesture(
                                gestureConfiguration = gestureConfig,
                                onGestureLabel = gestureLabel,
                            ) {
                                gestured = true
                            },
                    )
                }
            }
        }

        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle {
            assertEquals(false, gestured)
            assertThat(hapticResults).hasSize(0)
        }

        // Verify that correct content description is not set for a11y if gestures are disabled
        val context = ApplicationProvider.getApplicationContext<Context>()
        val expectedText =
            context.getString(
                R.string.one_handed_gesture_primary_action_accessibility_text,
                gestureLabel,
            )
        onView(withContentDescription(expectedText)).checkDoesNotExist()
    }

    /** Verifies simple Dismiss gesture */
    @Test
    fun simple_dismiss_gesture() {
        var gestured = false
        var indicatorShown = false
        var pressCoordinates: Offset = Offset.Zero
        var textSize: IntSize = IntSize.Zero
        val sdkGestureInputManager = SdkGestureInputManagerMock()
        val hapticResults = mutableMapOf<HapticFeedbackType, Int>()
        val gestureLabel = "dismiss"

        rule.setContentWithTheme {
            val interactionSource = remember { MutableInteractionSource() }
            val gestureConfig =
                rememberOneHandedGestureConfiguration(action = OneHandedGestureAction.Dismiss)
            val indicatorState = remember { OneHandedGestureClickIndicatorState() }
            val coroutineScope = rememberCoroutineScope()

            MockSdkGestureInputManager(sdkGestureInputManager, hapticResults) {
                OneHandedGestureClickIndicator(
                    gestureConfiguration = gestureConfig,
                    state = indicatorState,
                ) {
                    Text(
                        "Gesturable",
                        modifier =
                            Modifier.onSizeChanged { textSize = it }
                                .oneHandedGesture(
                                    gestureConfiguration = gestureConfig,
                                    interactionSource = interactionSource,
                                    onGestureAvailable = {
                                        coroutineScope.launch {
                                            indicatorState.showIndicator()
                                            indicatorShown = true
                                        }
                                    },
                                    onGestureLabel = gestureLabel,
                                ) {
                                    gestured = true
                                },
                    )
                }
            }
            interactionSource.ListenForInteractions(onPressInteraction = { pressCoordinates = it })
        }

        // It takes at least a second for indicator to be shown. Fast-forward 3s to allow some delay
        rule.mainClock.advanceTimeBy(3000)

        sdkGestureInputManager.performGesture(sdkActionDismiss)

        rule.runOnIdle {
            assertEquals(true, gestured)
            assertEquals(true, indicatorShown)
            assertEquals(textSize.center.toOffset(), pressCoordinates)

            assertThat(hapticResults).hasSize(1)
            assertEquals(hapticResults[HapticFeedbackType.LongPress], 1)
        }

        // Verify that correct content description is set
        val context = ApplicationProvider.getApplicationContext<Context>()
        val expectedText =
            context.getString(
                R.string.one_handed_gesture_dismiss_action_accessibility_text,
                gestureLabel,
            )
        onView(withContentDescription(expectedText)).checkExists()
    }

    /** Verifies that Clickable priority is higher than Scrollable */
    @Test
    fun clickable_over_scrollable() {
        var scrollGestured = false
        var textGestured = false
        var scrollIndicatorShown = false
        var textIndicatorShown = false
        val sdkGestureInputManager = SdkGestureInputManagerMock()
        val buttonGestureLabel = "click"

        rule.setContentWithTheme {
            val scrollGestureConfig =
                rememberOneHandedGestureConfiguration(
                    action = OneHandedGestureAction.Primary,
                    priority = OneHandedGesturePriority.Scrollable,
                )
            val scrollIndicatorState =
                remember(scrollGestureConfig) { OneHandedGestureScrollIndicatorState() }

            val textGestureConfig =
                rememberOneHandedGestureConfiguration(
                    action = OneHandedGestureAction.Primary,
                    priority = OneHandedGesturePriority.Clickable,
                )
            val textIndicatorState = remember { OneHandedGestureClickIndicatorState() }
            val coroutineScope = rememberCoroutineScope()

            MockSdkGestureInputManager(sdkGestureInputManager) {
                val scrollState = rememberTransformingLazyColumnState()
                ScreenScaffold(
                    scrollIndicator = {
                        OneHandedGestureScrollIndicator(
                            gestureConfiguration = scrollGestureConfig,
                            indicatorState = scrollIndicatorState,
                            scrollState = scrollState,
                            modifier = Modifier.align(Alignment.CenterEnd),
                        )
                    }
                ) { paddings ->
                    TransformingLazyColumn(
                        state = scrollState,
                        modifier =
                            Modifier.oneHandedGesture(
                                gestureConfiguration = scrollGestureConfig,
                                onGestureLabel = "scroll",
                                onGestureAvailable = { scrollIndicatorShown = true },
                            ) {
                                scrollGestured = true
                            },
                        contentPadding = paddings,
                    ) {
                        item {
                            OneHandedGestureClickIndicator(
                                gestureConfiguration = textGestureConfig,
                                state = textIndicatorState,
                            ) {
                                Text(
                                    "Clickable",
                                    modifier =
                                        Modifier.oneHandedGesture(
                                            gestureConfiguration = textGestureConfig,
                                            onGestureLabel = buttonGestureLabel,
                                            onGestureAvailable = {
                                                coroutineScope.launch {
                                                    textIndicatorState.showIndicator()
                                                    textIndicatorShown = true
                                                }
                                            },
                                        ) {
                                            textGestured = true
                                        },
                                )
                            }
                        }
                    }
                }
            }
        }

        // It takes at least a second for indicator to be shown. Wait for 3s to allow some delay
        rule.mainClock.advanceTimeBy(3000)

        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle {
            assertEquals(false, scrollIndicatorShown)
            assertEquals(false, scrollGestured)
            assertEquals(true, textIndicatorShown)
            assertEquals(true, textGestured)
        }

        // Verify that correct content description is set
        val context = ApplicationProvider.getApplicationContext<Context>()
        val expectedText =
            context.getString(
                R.string.one_handed_gesture_primary_action_accessibility_text,
                buttonGestureLabel,
            )
        onView(withContentDescription(expectedText)).checkExists()
    }

    /** Verifies that all gestures with the same priority are triggered */
    @Test
    fun two_gestures_same_priority() {
        var tlcGestured = false
        val textGestured = mutableListOf(false, false)
        val textIndicatorShown = mutableListOf(false, false)
        val sdkGestureInputManager = SdkGestureInputManagerMock()

        rule.setContentWithTheme {
            val coroutineScope = rememberCoroutineScope()

            MockSdkGestureInputManager(sdkGestureInputManager) {
                val scrollGestureConfig =
                    rememberOneHandedGestureConfiguration(
                        action = OneHandedGestureAction.Primary,
                        priority = OneHandedGesturePriority.Scrollable,
                    )
                val scrollIndicatorState = remember { OneHandedGestureClickIndicatorState() }
                TransformingLazyColumn(
                    modifier =
                        Modifier.oneHandedGesture(
                            gestureConfiguration = scrollGestureConfig,
                            onGestureLabel = "scroll",
                            onGestureAvailable = {
                                coroutineScope.launch {
                                    scrollIndicatorState.showIndicator()
                                }
                            },
                        ) {
                            tlcGestured = true
                        }
                ) {
                    items(2) { index ->
                        val textGestureConfig =
                            rememberOneHandedGestureConfiguration(
                                action = OneHandedGestureAction.Primary,
                                priority = OneHandedGesturePriority.Clickable,
                            )
                        val textIndicatorState = remember { OneHandedGestureClickIndicatorState() }
                        OneHandedGestureClickIndicator(
                            gestureConfiguration = textGestureConfig,
                            state = textIndicatorState,
                        ) {
                            Text(
                                "Clickable$index",
                                modifier =
                                    Modifier.oneHandedGesture(
                                        gestureConfiguration = textGestureConfig,
                                        onGestureLabel = "click text $index",
                                        onGestureAvailable = {
                                            coroutineScope.launch {
                                                textIndicatorState.showIndicator()
                                                textIndicatorShown[index] = true
                                            }
                                        },
                                    ) {
                                        textGestured[index] = true
                                    },
                            )
                        }
                    }
                }
            }
        }

        // It takes at least a second for indicator to be shown. Wait for 3s to allow some delay
        rule.mainClock.advanceTimeBy(3000)

        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle {
            assertEquals(false, tlcGestured)
            // Since all Texts have the same priority, verify that all of them have been gestured
            assertEquals(true, textGestured.all { it })
            assertEquals(true, textIndicatorShown.all { it })
        }
    }

    /** Verifies that onGestureAvailable is invoked after the default delay */
    @Test
    fun show_indicator_default_delay() {
        verifyShowIndicatorDelay(
            showIndicatorDelayMillis = null,
            expectedDelayMillis = OneHandedGestureDefaults.ShowIndicatorDelayMillis,
        )
    }

    /** Verifies that onGestureAvailable is invoked after a custom delay */
    @Test
    fun show_indicator_custom_delay() {
        verifyShowIndicatorDelay(showIndicatorDelayMillis = 2500, expectedDelayMillis = 2500)
    }

    /** Verifies that onGestureAvailable is invoked immediately when the delay is zero */
    @Test
    fun show_indicator_zero_delay() {
        verifyShowIndicatorDelay(showIndicatorDelayMillis = 0, expectedDelayMillis = 0)
    }

    /** Verifies that gestures with different delays are each made available after their delay */
    @Test
    fun show_indicator_with_different_delays() {
        var primaryAvailable = false
        var dismissAvailable = false
        val sdkGestureInputManager = SdkGestureInputManagerMock()

        rule.mainClock.autoAdvance = false
        rule.setContentWithTheme {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                val primaryConfig =
                    rememberOneHandedGestureConfiguration(action = OneHandedGestureAction.Primary)
                val dismissConfig =
                    rememberOneHandedGestureConfiguration(action = OneHandedGestureAction.Dismiss)
                Column {
                    Box(
                        Modifier.size(50.dp).oneHandedGesture(
                            gestureConfiguration = primaryConfig,
                            onGestureLabel = "primary",
                            showIndicatorDelayMillis = 500,
                            onGestureAvailable = { primaryAvailable = true },
                        ) {}
                    )
                    Box(
                        Modifier.size(50.dp).oneHandedGesture(
                            gestureConfiguration = dismissConfig,
                            onGestureLabel = "dismiss",
                            showIndicatorDelayMillis = 2000,
                            onGestureAvailable = { dismissAvailable = true },
                        ) {}
                    )
                }
            }
        }
        rule.waitForIdle()

        rule.mainClock.advanceTimeBy(1000)
        rule.runOnIdle {
            assertEquals(true, primaryAvailable)
            assertEquals(false, dismissAvailable)
        }

        rule.mainClock.advanceTimeBy(1500)
        rule.runOnIdle {
            assertEquals(true, primaryAvailable)
            assertEquals(true, dismissAvailable)
        }
    }

    @Test
    fun show_indicator_same_priority_different_delays() {
        val availableOrder = mutableListOf<String>()
        val sdkGestureInputManager = SdkGestureInputManagerMock()

        rule.mainClock.autoAdvance = false
        rule.setContentWithTheme {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                Column {
                    listOf("slow" to 2000L, "fast" to 1000L).forEach { (name, delayMillis) ->
                        key(name) {
                            val gestureConfig =
                                rememberOneHandedGestureConfiguration(
                                    action = OneHandedGestureAction.Primary,
                                    priority = OneHandedGesturePriority.Clickable,
                                )
                            Box(
                                Modifier.size(50.dp).oneHandedGesture(
                                    gestureConfiguration = gestureConfig,
                                    onGestureLabel = name,
                                    showIndicatorDelayMillis = delayMillis,
                                    onGestureAvailable = { availableOrder.add(name) },
                                ) {}
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()

        rule.mainClock.advanceTimeBy(900)
        rule.runOnIdle { assertThat(availableOrder).isEmpty() }

        rule.mainClock.advanceTimeBy(200)
        rule.runOnIdle { assertThat(availableOrder).containsExactly("fast") }

        rule.mainClock.advanceTimeBy(1000)
        rule.runOnIdle { assertThat(availableOrder).containsExactly("fast", "slow").inOrder() }
    }

    /**
     * Verifies that registering multiple oneHandedGestures with the same experienceId doesn't throw
     * an exception
     */
    @Test
    fun register_same_experience_id() {
        val sdkGestureInputManager = SdkGestureInputManagerMock()

        rule.setContentWithTheme {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                repeat(2) {
                    val gestureConfig =
                        rememberOneHandedGestureConfiguration(
                            action = OneHandedGestureAction.Primary
                        )
                    Text(
                        "Clickable$it",
                        modifier =
                            Modifier.oneHandedGesture(
                                gestureConfiguration = gestureConfig,
                                onGestureLabel = "click text $it",
                            ) {},
                    )
                }
            }
        }
        rule.waitForIdle()
    }

    @Test(expected = IllegalArgumentException::class)
    fun register_indicators_with_different_floating_value_throws_error() {
        val gestureConfig =
            OneHandedGestureConfiguration(
                action = OneHandedGestureAction.Primary,
                gestureId = "key",
            )

        rule.setContentWithTheme {
            // Register initial gesture indicator
            LocalOneHandedGestureManager.current.registerGestureIndicator(
                gestureConfig,
                isFloating = true,
                duration = 500.milliseconds,
            )

            // Should throw
            LocalOneHandedGestureManager.current.registerGestureIndicator(
                gestureConfig,
                isFloating = false,
                duration = 500.milliseconds,
            )
        }
        rule.waitForIdle()
    }

    @Test(expected = IllegalArgumentException::class)
    fun register_indicators_with_different_duration_throws_error() {
        val gestureConfig =
            OneHandedGestureConfiguration(
                action = OneHandedGestureAction.Primary,
                gestureId = "key",
            )

        rule.setContentWithTheme {
            // Register initial gesture indicator
            LocalOneHandedGestureManager.current.registerGestureIndicator(
                gestureConfig,
                isFloating = true,
                duration = 500.milliseconds,
            )

            // Should throw
            LocalOneHandedGestureManager.current.registerGestureIndicator(
                gestureConfig,
                isFloating = true,
                duration = 250.milliseconds,
            )
        }
        rule.waitForIdle()
    }

    /** Verifies behavior of gesturable Composables in Pager */
    @Test
    fun pager_gesture_aware_content_per_page() {
        val numberOfPages = 10
        val textGestured = MutableList(numberOfPages) { false }
        val sdkGestureInputManager = SdkGestureInputManagerMock()

        rule.setContentWithTheme {
            val state = rememberPagerState { numberOfPages }

            MockSdkGestureInputManager(sdkGestureInputManager) {
                HorizontalPager(
                    state = state,
                    modifier = Modifier.fillMaxSize().testTag("Pager"),
                ) { page ->
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val gestureConfig =
                            rememberOneHandedGestureConfiguration(
                                action = OneHandedGestureAction.Primary
                            )
                        Text(
                            "Clickable $page",
                            modifier =
                                Modifier.oneHandedGesture(
                                    gestureConfiguration = gestureConfig,
                                    onGestureLabel = "click text",
                                ) {
                                    textGestured[page] = true
                                },
                        )
                    }
                }
            }
        }

        repeat(numberOfPages) {
            sdkGestureInputManager.performGesture(sdkActionPrimary)
            rule.onNodeWithTag("Pager").performTouchInput { swipeLeft() }
        }

        rule.runOnIdle {
            // Check that Texts on all Pager pages have been gestured
            assertEquals(true, textGestured.all { it })
        }
    }

    /** Verifies that updating Modifier.oneHandedGesture is correctly handled by the system */
    @Test
    fun updating_one_handed_gesture_modifier() {
        val buttonGestured = MutableList(2) { 0 }
        val sdkGestureInputManager = SdkGestureInputManagerMock()

        rule.setContentWithTheme {
            var invertPriorities by remember { mutableStateOf(false) }
            MockSdkGestureInputManager(sdkGestureInputManager) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val button1Spec =
                        rememberOneHandedGestureConfiguration(
                            action = OneHandedGestureAction.Primary,
                            priority =
                                if (invertPriorities) OneHandedGesturePriority.Clickable
                                else OneHandedGesturePriority.Unspecified,
                        )

                    val button2Spec =
                        rememberOneHandedGestureConfiguration(
                            action = OneHandedGestureAction.Primary,
                            priority =
                                if (invertPriorities) OneHandedGesturePriority.Unspecified
                                else OneHandedGesturePriority.Clickable,
                        )

                    Button(
                        onClick = { invertPriorities = !invertPriorities },
                        modifier = Modifier.testTag("InvertPriorityButton"),
                    ) {
                        Text("Invert priority")
                    }

                    Button(
                        onClick = {},
                        modifier =
                            Modifier.oneHandedGesture(
                                gestureConfiguration = button1Spec,
                                onGestureLabel = "click first button",
                            ) {
                                buttonGestured[0]++
                            },
                    ) {
                        Text("Gesturable 1")
                    }
                    Button(
                        onClick = {},
                        modifier =
                            Modifier.oneHandedGesture(
                                gestureConfiguration = button2Spec,
                                onGestureLabel = "click second button",
                            ) {
                                buttonGestured[1]++
                            },
                    ) {
                        Text("Gesturable 2")
                    }
                }
            }
        }

        sdkGestureInputManager.performGesture(sdkActionPrimary)

        rule.runOnIdle {
            // By default, 2nd button has higher priority and should be gestured
            assertEquals(buttonGestured[0], 0)
            assertEquals(buttonGestured[1], 1)
        }
        rule.onNodeWithTag("InvertPriorityButton").performClick()
        rule.waitForIdle()

        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle {
            // After inverting priority, first button should be gestured. Number of gestures
            // performed on the second button should not change
            assertEquals(buttonGestured[0], 1)
            assertEquals(buttonGestured[1], 1)
        }
    }

    @SuppressLint("UnusedBoxWithConstraintsScope")
    @Test
    fun moving_gesture_modifier_with_movable_content_does_not_crash() {
        var gestured = false
        var moveContent by mutableStateOf(false)
        val sdkGestureInputManager = SdkGestureInputManagerMock()

        rule.setContentWithTheme {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                val gesturableContent = remember {
                    movableContentOf {
                        // Subcomposition inside the moved content, so that the gesture node is
                        // updated before it is attached again in its new location.
                        BoxWithConstraints {
                            val gestureConfig =
                                rememberOneHandedGestureConfiguration(
                                    action = OneHandedGestureAction.Primary
                                )
                            Text(
                                "Gesturable",
                                modifier =
                                    Modifier.oneHandedGesture(
                                        gestureConfiguration = gestureConfig,
                                        onGestureLabel = moveContent.toString(),
                                    ) {
                                        gestured = true
                                    },
                            )
                        }
                    }
                }

                // Changing the key moves the content to a new location in the composition.
                key(moveContent) { gesturableContent() }
            }
        }

        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle {
            assertEquals(true, gestured)
            gestured = false
        }

        // Move the content - the gesture node is detached, updated and attached again.
        rule.runOnIdle { moveContent = true }
        rule.waitForIdle()

        // Verify that the gesture is still registered after the move.
        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle { assertEquals(true, gestured) }
    }

    @Test
    fun alert_dialog_confirm_and_dismiss() {
        val sdkGestureInputManager = SdkGestureInputManagerMock(false)
        var confirmButtonClicked = false
        rule.setContentWithTheme {
            val transformationSpec = rememberTransformationSpec()
            MockSdkGestureInputManager(sdkGestureInputManager) {
                AlertDialog(
                    visible = true,
                    onDismissRequest = {},
                    icon = {
                        Icon(
                            Icons.Rounded.AccountCircle,
                            modifier = Modifier.size(32.dp),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    title = { Text("Title") },
                    transformationSpec = transformationSpec,
                    confirmButton = {
                        AlertDialogDefaults.ConfirmButton(onClick = { confirmButtonClicked = true })
                    },
                ) {
                    item { Text(text = "This is a text which has to be scrolled through") }
                    item { Button(onClick = {}) { Text(text = "Random button") } }
                }
            }
        }

        // Scroll through alert dialog with one-handed gestures until confirm button is gestured
        for (i in 0..10) {
            sdkGestureInputManager.performGesture(sdkActionPrimary)
            rule.waitForIdle()
            if (confirmButtonClicked) {
                break
            }
        }
        assert(confirmButtonClicked)
    }

    @Test
    fun alert_dialog_confirm_button_gesture_disabled() {
        val sdkGestureInputManager = SdkGestureInputManagerMock(false)
        var confirmButtonClicked = false
        rule.setContentWithTheme {
            val transformationSpec = rememberTransformationSpec()
            MockSdkGestureInputManager(sdkGestureInputManager) {
                AlertDialog(
                    visible = true,
                    onDismissRequest = {},
                    icon = {
                        Icon(
                            Icons.Rounded.AccountCircle,
                            modifier = Modifier.size(32.dp),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    title = { Text("Title") },
                    transformationSpec = transformationSpec,
                    confirmButton = {
                        AlertDialogDefaults.ConfirmButton(
                            onClick = { confirmButtonClicked = true },
                            gestureVisibilityThreshold = AlertDialogDefaults.GestureDisabled,
                        )
                    },
                ) {
                    item { Text(text = "This is a text which has to be scrolled through") }
                    item { Button(onClick = {}) { Text(text = "Random button") } }
                }
            }
        }

        // Scroll through alert dialog with one-handed gestures; with gestureVisibilityThreshold =
        // 0f, the confirm button gesture is disabled and should never trigger.
        for (i in 0..10) {
            sdkGestureInputManager.performGesture(sdkActionPrimary)
            rule.waitForIdle()
        }
        rule.runOnIdle { assertEquals(false, confirmButtonClicked) }
    }

    @Test
    fun alert_dialog_confirm_button_gesture_visibility_threshold() {
        val sdkGestureInputManager = SdkGestureInputManagerMock(false)
        var confirmButtonClicked = false
        var offsetX by mutableStateOf(0.dp)
        var gestureVisibilityThreshold by mutableFloatStateOf(0f)

        rule.setContentWithTheme {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                Box(
                    modifier = Modifier.fillMaxSize().offset { IntOffset(offsetX.roundToPx(), 0) },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    AlertDialogDefaults.ConfirmButton(
                        onClick = { confirmButtonClicked = true },
                        gestureVisibilityThreshold = gestureVisibilityThreshold,
                    )
                }
            }
        }

        // When fully visible (offsetX = 0.dp), gestureVisibilityThreshold = 0f disables the
        // gesture.
        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle { assertEquals(false, confirmButtonClicked) }

        // Shift the 63.dp-wide ConfirmButton left by 21.dp across the left window edge (x = 0) so
        // two-thirds (~67%) of the button remains inside the window. With threshold = 0.9f, the
        // gesture is not active.
        offsetX = (-21).dp
        gestureVisibilityThreshold = 0.9f
        rule.waitForIdle()
        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle { assertEquals(false, confirmButtonClicked) }

        // Lowering gestureVisibilityThreshold to 0.5f at the same -21.dp offset activates the
        // gesture.
        gestureVisibilityThreshold = 0.5f
        rule.waitForIdle()
        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle {
            assertEquals(true, confirmButtonClicked)
            confirmButtonClicked = false
        }

        // Shifting the button further left to -42.dp (~33% visible) deactivates the gesture for a
        // 0.5f threshold.
        offsetX = (-42).dp
        rule.waitForIdle()
        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle { assertEquals(false, confirmButtonClicked) }

        // Returning the button to full visibility (offsetX = 0.dp) with threshold = 0.9f activates
        // the gesture.
        offsetX = 0.dp
        gestureVisibilityThreshold = 0.9f
        rule.waitForIdle()
        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle { assertEquals(true, confirmButtonClicked) }
    }

    @Test
    fun alert_dialog_edge_button_without_gesture_does_not_click() {
        val sdkGestureInputManager = SdkGestureInputManagerMock(false)
        var edgeButtonClicked = false

        rule.setContentWithTheme {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                AlertDialog(
                    visible = true,
                    onDismissRequest = {},
                    title = {},
                    edgeButton = {
                        AlertDialogDefaults.EdgeButton(onClick = { edgeButtonClicked = true })
                    },
                )
            }
        }

        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle { assertEquals(false, edgeButtonClicked) }
    }

    @Test
    fun alert_dialog_edge_button_with_gesture(@TestParameter useTransformingLazyColumn: Boolean) {
        val sdkGestureInputManager = SdkGestureInputManagerMock(false)
        var edgeButtonClicked = false

        rule.setContentWithTheme {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                if (useTransformingLazyColumn) {
                    AlertDialog(
                        visible = true,
                        onDismissRequest = {},
                        title = {},
                        transformationSpec = rememberTransformationSpec(),
                        edgeButton = {
                            AlertDialogDefaults.EdgeButton(
                                onClick = { edgeButtonClicked = true },
                                gestureVisibilityThreshold = 1f,
                            )
                        },
                    )
                } else {
                    AlertDialog(
                        visible = true,
                        onDismissRequest = {},
                        title = {},
                        edgeButton = {
                            AlertDialogDefaults.EdgeButton(
                                onClick = { edgeButtonClicked = true },
                                gestureVisibilityThreshold = 1f,
                            )
                        },
                    )
                }
            }
        }

        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle { assertEquals(true, edgeButtonClicked) }
    }

    @Test
    fun alert_dialog_content_groups_edge_button(
        @TestParameter enabled: Boolean,
        @TestParameter useTransformingLazyColumn: Boolean,
    ) {
        val sdkGestureInputManager = SdkGestureInputManagerMock(false)
        var edgeButtonClicked = false
        rule.setContentWithTheme {
            ScreenConfiguration(SCREEN_SIZE_SMALL) {
                CompositionLocalProvider(LocalOneHandedGestureEnabled provides enabled) {
                    MockSdkGestureInputManager(sdkGestureInputManager) {
                        if (useTransformingLazyColumn) {
                            val transformationSpec = rememberTransformationSpec()
                            AlertDialog(
                                visible = true,
                                onDismissRequest = {},
                                modifier = Modifier.size(SCREEN_SIZE_SMALL.dp),
                                title = { Text("Title") },
                                transformationSpec = transformationSpec,
                                edgeButton = {
                                    AlertDialogDefaults.EdgeButton(
                                        onClick = { edgeButtonClicked = true },
                                        gestureVisibilityThreshold = 0.9f,
                                        onGestureLabel = "share once",
                                    ) {
                                        Text("Share once")
                                    }
                                },
                            ) {
                                item {
                                    SwitchButton(
                                        modifier =
                                            Modifier.fillMaxWidth()
                                                .transformedHeight(this, transformationSpec),
                                        checked = true,
                                        onCheckedChange = {},
                                        label = { Text("Weather") },
                                        transformation = SurfaceTransformation(transformationSpec),
                                    )
                                }
                                item {
                                    SwitchButton(
                                        modifier =
                                            Modifier.fillMaxWidth()
                                                .transformedHeight(this, transformationSpec),
                                        checked = true,
                                        onCheckedChange = {},
                                        label = { Text("Calendar") },
                                        transformation = SurfaceTransformation(transformationSpec),
                                    )
                                }
                                item { AlertDialogDefaults.GroupSeparator() }
                                item {
                                    FilledTonalButton(
                                        modifier =
                                            Modifier.fillMaxWidth()
                                                .transformedHeight(this, transformationSpec),
                                        onClick = {},
                                        label = {
                                            Text(
                                                modifier = Modifier.fillMaxWidth(),
                                                text = "Never share",
                                            )
                                        },
                                        transformation = SurfaceTransformation(transformationSpec),
                                    )
                                }
                                item {
                                    FilledTonalButton(
                                        modifier =
                                            Modifier.fillMaxWidth()
                                                .transformedHeight(this, transformationSpec),
                                        onClick = {},
                                        label = {
                                            Text(
                                                modifier = Modifier.fillMaxWidth(),
                                                text = "Share always",
                                            )
                                        },
                                        transformation = SurfaceTransformation(transformationSpec),
                                    )
                                }
                            }
                        } else {
                            AlertDialog(
                                visible = true,
                                onDismissRequest = {},
                                modifier = Modifier.size(SCREEN_SIZE_SMALL.dp),
                                title = { Text("Title") },
                                edgeButton = {
                                    AlertDialogDefaults.EdgeButton(
                                        onClick = { edgeButtonClicked = true },
                                        gestureVisibilityThreshold = 0.9f,
                                        onGestureLabel = "share once",
                                    ) {
                                        Text("Share once")
                                    }
                                },
                            ) {
                                item {
                                    SwitchButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        checked = true,
                                        onCheckedChange = {},
                                        label = { Text("Weather") },
                                    )
                                }
                                item {
                                    SwitchButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        checked = true,
                                        onCheckedChange = {},
                                        label = { Text("Calendar") },
                                    )
                                }
                                item { AlertDialogDefaults.GroupSeparator() }
                                item {
                                    FilledTonalButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {},
                                        label = {
                                            Text(
                                                modifier = Modifier.fillMaxWidth(),
                                                text = "Never share",
                                            )
                                        },
                                    )
                                }
                                item {
                                    FilledTonalButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {},
                                        label = {
                                            Text(
                                                modifier = Modifier.fillMaxWidth(),
                                                text = "Share always",
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // First gesture should scroll down while EdgeButton is hidden, not click EdgeButton.
        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle { assertEquals(false, edgeButtonClicked) }

        // Continue scrolling through alert dialog with one-handed gestures until edge button is
        // gestured.
        for (i in 1..10) {
            sdkGestureInputManager.performGesture(sdkActionPrimary)
            rule.waitForIdle()
            if (edgeButtonClicked) {
                break
            }
        }
        rule.runOnIdle { assertEquals(enabled, edgeButtonClicked) }
    }

    @Test
    fun alert_dialog_edge_button_gesture_visibility_threshold() {
        val sdkGestureInputManager = SdkGestureInputManagerMock(false)
        var edgeButtonClicked = false
        var buttonHeight by mutableStateOf(16.dp)
        var gestureVisibilityThreshold by mutableFloatStateOf(0f)

        rule.setContentWithTheme {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                Box(modifier = Modifier.height(buttonHeight)) {
                    AlertDialogDefaults.EdgeButton(
                        onClick = { edgeButtonClicked = true },
                        gestureVisibilityThreshold = gestureVisibilityThreshold,
                    )
                }
            }
        }

        // gestureVisibilityThreshold = 0f disables the gesture even at non-zero height.
        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle { assertEquals(false, edgeButtonClicked) }

        // At 16.dp total height (10.dp inner button height after 6.dp vertical padding, ~14.3% of
        // EdgeButtonSize.Medium's 70.dp maximumHeight), gestureVisibilityThreshold = 0.1f
        // activates the gesture.
        gestureVisibilityThreshold = 0.1f
        rule.waitForIdle()
        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle {
            assertEquals(true, edgeButtonClicked)
            edgeButtonClicked = false
        }

        // At 41.dp total height (35.dp inner button height = 50% of EdgeButtonSize.Medium's 70.dp
        // maximumHeight), gestureVisibilityThreshold = 0.5f does not activate the gesture (must
        // exceed threshold).
        buttonHeight = 35.dp + EdgeButtonVerticalPadding * 2
        gestureVisibilityThreshold = 0.5f
        rule.waitForIdle()
        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle { assertEquals(false, edgeButtonClicked) }

        // Lowering gestureVisibilityThreshold to 0.4f at the same 50% inner height activates the
        // gesture (exceeds threshold).
        gestureVisibilityThreshold = 0.4f
        rule.waitForIdle()
        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle {
            assertEquals(true, edgeButtonClicked)
            edgeButtonClicked = false
        }

        // Raising gestureVisibilityThreshold to 1.0f at 50% inner height deactivates the gesture.
        gestureVisibilityThreshold = 1.0f
        rule.waitForIdle()
        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle { assertEquals(false, edgeButtonClicked) }

        // At full height, gestureVisibilityThreshold = 1.0f activates the gesture.
        buttonHeight = EdgeButtonSize.Medium.maximumHeightPlusPadding()
        rule.waitForIdle()
        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle {
            assertEquals(true, edgeButtonClicked)
            edgeButtonClicked = false
        }

        // Even at full height, setting gestureVisibilityThreshold = GestureDisabled (0f) disables
        // the gesture.
        gestureVisibilityThreshold = AlertDialogDefaults.GestureDisabled
        rule.waitForIdle()
        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle { assertEquals(false, edgeButtonClicked) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun alert_dialog_edge_button_invalid_gesture_visibility_threshold_throws() {
        rule.setContentWithTheme {
            AlertDialogDefaults.EdgeButton(onClick = {}, gestureVisibilityThreshold = 1.5f)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun alert_dialog_confirm_button_invalid_gesture_visibility_threshold_throws() {
        rule.setContentWithTheme {
            AlertDialogDefaults.ConfirmButton(onClick = {}, gestureVisibilityThreshold = -0.1f)
        }
    }

    @Test
    fun alert_dialog_edge_button_accessibility_label() {
        val sdkGestureInputManager = SdkGestureInputManagerMock()
        var useDefaultLabel by mutableStateOf(true)
        var customLabel by mutableStateOf<String?>("share once")
        var gestureVisibilityThreshold by mutableFloatStateOf(1f)

        rule.setContentWithTheme {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                AlertDialog(
                    visible = true,
                    onDismissRequest = {},
                    title = {},
                    edgeButton = {
                        if (useDefaultLabel) {
                            AlertDialogDefaults.EdgeButton(
                                onClick = {},
                                gestureVisibilityThreshold = gestureVisibilityThreshold,
                            )
                        } else {
                            AlertDialogDefaults.EdgeButton(
                                onClick = {},
                                gestureVisibilityThreshold = gestureVisibilityThreshold,
                                onGestureLabel = customLabel,
                            )
                        }
                    },
                )
            }
        }

        rule.mainClock.advanceTimeBy(GESTURE_AVAILABLE_DELAY)
        rule.waitForIdle()

        val context = ApplicationProvider.getApplicationContext<Context>()
        val defaultConfirmLabel =
            context.getString(
                R.string.one_handed_gesture_alert_dialog_primary_action_confirm_accessibility_text
            )
        val defaultExpectedText =
            context.getString(
                R.string.one_handed_gesture_primary_action_accessibility_text,
                defaultConfirmLabel,
            )
        onView(withContentDescription(defaultExpectedText)).checkExists()

        // Update to custom onGestureLabel and verify updated accessibility announcement.
        useDefaultLabel = false
        rule.mainClock.advanceTimeBy(GESTURE_AVAILABLE_DELAY)
        rule.waitForIdle()

        val customExpectedText =
            context.getString(
                R.string.one_handed_gesture_primary_action_accessibility_text,
                "share once",
            )
        onView(withContentDescription(customExpectedText)).checkExists()

        // Passing onGestureLabel = null suppresses the accessibility indicator view.
        customLabel = null
        rule.mainClock.advanceTimeBy(GESTURE_AVAILABLE_DELAY)
        rule.waitForIdle()
        onView(withContentDescription(customExpectedText)).checkDoesNotExist()
        onView(withContentDescription(defaultExpectedText)).checkDoesNotExist()

        // Disabling the gesture with gestureVisibilityThreshold = 0f also suppresses the
        // accessibility indicator view even when using the default label.
        useDefaultLabel = true
        gestureVisibilityThreshold = 0f
        rule.mainClock.advanceTimeBy(GESTURE_AVAILABLE_DELAY)
        rule.waitForIdle()
        onView(withContentDescription(defaultExpectedText)).checkDoesNotExist()
    }

    @Test
    fun alert_dialog_confirm_button_accessibility_label() {
        val sdkGestureInputManager = SdkGestureInputManagerMock()
        var useDefaultLabel by mutableStateOf(true)
        var customLabel by mutableStateOf<String?>("approve")
        var gestureVisibilityThreshold by mutableFloatStateOf(0.9f)

        rule.setContentWithTheme {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                AlertDialog(
                    visible = true,
                    onDismissRequest = {},
                    title = {},
                    confirmButton = {
                        if (useDefaultLabel) {
                            AlertDialogDefaults.ConfirmButton(
                                onClick = {},
                                gestureVisibilityThreshold = gestureVisibilityThreshold,
                            )
                        } else {
                            AlertDialogDefaults.ConfirmButton(
                                onClick = {},
                                gestureVisibilityThreshold = gestureVisibilityThreshold,
                                onGestureLabel = customLabel,
                            )
                        }
                    },
                )
            }
        }

        rule.mainClock.advanceTimeBy(GESTURE_AVAILABLE_DELAY)
        rule.waitForIdle()

        val context = ApplicationProvider.getApplicationContext<Context>()
        val defaultConfirmLabel =
            context.getString(
                R.string.one_handed_gesture_alert_dialog_primary_action_confirm_accessibility_text
            )
        val defaultExpectedText =
            context.getString(
                R.string.one_handed_gesture_primary_action_accessibility_text,
                defaultConfirmLabel,
            )
        onView(withContentDescription(defaultExpectedText)).checkExists()

        // Update to custom onGestureLabel and verify updated accessibility announcement.
        useDefaultLabel = false
        rule.mainClock.advanceTimeBy(GESTURE_AVAILABLE_DELAY)
        rule.waitForIdle()

        val customExpectedText =
            context.getString(
                R.string.one_handed_gesture_primary_action_accessibility_text,
                "approve",
            )
        onView(withContentDescription(customExpectedText)).checkExists()

        // Passing onGestureLabel = null suppresses the accessibility indicator view.
        customLabel = null
        rule.mainClock.advanceTimeBy(GESTURE_AVAILABLE_DELAY)
        rule.waitForIdle()
        onView(withContentDescription(customExpectedText)).checkDoesNotExist()
        onView(withContentDescription(defaultExpectedText)).checkDoesNotExist()

        // Disabling the gesture with gestureVisibilityThreshold = 0f also suppresses the
        // accessibility indicator view.
        useDefaultLabel = true
        gestureVisibilityThreshold = 0f
        rule.mainClock.advanceTimeBy(GESTURE_AVAILABLE_DELAY)
        rule.waitForIdle()
        onView(withContentDescription(defaultExpectedText)).checkDoesNotExist()
    }

    @Test
    fun alert_dialog_edge_button_click_indicator_shown() {
        val sdkGestureInputManager = SdkGestureInputManagerMock(showIndicator = true)

        rule.setContentWithTheme {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                AlertDialog(
                    visible = true,
                    onDismissRequest = {},
                    title = {},
                    edgeButton = {
                        AlertDialogDefaults.EdgeButton(
                            onClick = {},
                            gestureVisibilityThreshold = 1f,
                        )
                    },
                )
            }
        }

        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(GESTURE_AVAILABLE_DELAY)
        rule.waitForIdle()

        rule.runOnIdle {
            assertEquals(sdkActionPrimary, sdkGestureInputManager.lastNotifiedSdkGestureAction)
            assertNotNull(sdkGestureInputManager.lastNotifiedGestureId)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun alert_dialog_edge_button_click_indicator_uses_content_color() {
        val sdkGestureInputManager = SdkGestureInputManagerMock(showIndicator = true)
        val expectedContentColor = Color.Yellow

        rule.mainClock.autoAdvance = false
        rule.setContentWithTheme {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    AlertDialogDefaults.EdgeButton(
                        onClick = {},
                        gestureVisibilityThreshold = 1f,
                        modifier = Modifier.testTag(TEST_TAG),
                        colors = ButtonDefaults.buttonColors(contentColor = expectedContentColor),
                        content = {
                            Spacer(Modifier.size(OneHandedGestureIndicatorSize.Medium.size))
                        },
                    )
                }
            }
        }

        rule.waitForIdle()
        // Before ShowIndicatorDelayMillis elapses, expectedContentColor is not yet rendered
        // since the button content is an empty Spacer.
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertDoesNotContainColor(expectedContentColor)

        // Advance past ShowIndicatorDelayMillis to trigger showIndicator(), then advance past the
        // fade-in delay so the gesture indicator is actively rendered using LocalContentColor.
        rule.mainClock.advanceTimeBy(OneHandedGestureDefaults.ShowIndicatorDelayMillis)
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(INDICATOR_ANIMATION_START_DELAY_MILLIS)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedContentColor)
    }

    @Test
    fun test_slc_scroll_down(
        @TestParameter anchor: TestParamScalingLazyListAnchorType,
        @TestParameter wrap: Boolean,
    ) {
        val sdkGestureInputManager = SdkGestureInputManagerMock(false)
        val listState = ScalingLazyListState()
        val gestureConfig =
            OneHandedGestureConfiguration(
                action = OneHandedGestureAction.Primary,
                gestureId = "GestureId",
            )

        rule.setContentWithTheme {
            ScreenConfiguration(SCREEN_SIZE_SMALL) {
                MockSdkGestureInputManager(sdkGestureInputManager) {
                    ScalingLazyColumn(
                        state = listState,
                        modifier =
                            Modifier.background(Color.Black)
                                .fillMaxSize()
                                .oneHandedGesture(
                                    gestureConfiguration = gestureConfig,
                                    onGestureLabel = "scroll",
                                    onGesture = {
                                        OneHandedGestureDefaults.scrollDown(listState, wrap)
                                    },
                                ),
                        anchorType = anchor.type,
                    ) {
                        items(10) { Text("Item $it") }
                    }
                }
            }
        }

        // scrollDown() scrolls 50% of the screen, making centerItemIndex to move 1 -> 5 -> 9
        val expectedWrapIndex = listOf(1, 5, 9)
        val expectedNoWrapIndex = listOf(1, 5, 9, 9, 9, 9, 9, 9, 9, 9)
        val expectedIndex = if (wrap) expectedWrapIndex else expectedNoWrapIndex
        repeat(10) { iteration ->
            rule.runOnIdle {
                assertEquals(
                    expectedIndex[iteration % expectedIndex.size],
                    listState.centerItemIndex,
                )
                sdkGestureInputManager.performGesture(sdkActionPrimary)
            }
        }
    }

    @Test
    fun test_slc_scroll_next_item(
        @TestParameter anchor: TestParamScalingLazyListAnchorType,
        @TestParameter wrap: Boolean,
    ) {
        val sdkGestureInputManager = SdkGestureInputManagerMock(false)
        val listState = ScalingLazyListState()
        val numberOfItems = 10
        val gestureConfig =
            OneHandedGestureConfiguration(
                action = OneHandedGestureAction.Primary,
                gestureId = "GestureId",
            )

        rule.setContentWithTheme {
            ScreenConfiguration(SCREEN_SIZE_SMALL) {
                MockSdkGestureInputManager(sdkGestureInputManager) {
                    ScalingLazyColumn(
                        state = listState,
                        modifier =
                            Modifier.background(Color.Black)
                                .fillMaxSize()
                                .oneHandedGesture(
                                    gestureConfiguration = gestureConfig,
                                    onGestureLabel = "scroll",
                                    onGesture = {
                                        OneHandedGestureDefaults.scrollDownToNextItem(
                                            listState,
                                            wrap,
                                        )
                                    },
                                ),
                        anchorType = anchor.type,
                    ) {
                        items(numberOfItems) { Text("Item $it") }
                    }
                }
            }
        }

        var expectedIndex = 1
        repeat(numberOfItems * 2) {
            rule.runOnIdle {
                assertEquals(expectedIndex, listState.centerItemIndex)
                sdkGestureInputManager.performGesture(sdkActionPrimary)
                if (expectedIndex == numberOfItems - 1) {
                    if (wrap) expectedIndex = 1
                } else {
                    expectedIndex++
                }
            }
        }
    }

    @Test
    fun test_tlc_scroll_down(@TestParameter wrap: Boolean) {
        val sdkGestureInputManager = SdkGestureInputManagerMock(false)
        val listState = TransformingLazyColumnState()
        val gestureConfig =
            OneHandedGestureConfiguration(
                action = OneHandedGestureAction.Primary,
                gestureId = "GestureId",
            )

        rule.setContentWithTheme {
            ScreenConfiguration(SCREEN_SIZE_SMALL) {
                MockSdkGestureInputManager(sdkGestureInputManager) {
                    TransformingLazyColumn(
                        state = listState,
                        modifier =
                            Modifier.background(Color.Black)
                                .fillMaxSize()
                                .oneHandedGesture(
                                    gestureConfiguration = gestureConfig,
                                    onGestureLabel = "scroll",
                                    onGesture = {
                                        OneHandedGestureDefaults.scrollDown(listState, wrap)
                                    },
                                ),
                    ) {
                        items(20) { Text("Item $it") }
                    }
                }
            }
        }

        // scrollDown() scrolls 50% of the screen, making centerItemIndex to move 4 -> 8 -> 12 -> 15
        val expectedWrapIndex = listOf(4, 8, 12, 15)
        val expectedNoWrapIndex = listOf(4, 8, 12, 15, 15, 15, 15, 15, 15, 15)
        val expectedIndex = if (wrap) expectedWrapIndex else expectedNoWrapIndex
        repeat(10) { iteration ->
            rule.runOnIdle {
                assertEquals(
                    expectedIndex[iteration % expectedIndex.size],
                    listState.anchorItemIndex,
                )
                sdkGestureInputManager.performGesture(sdkActionPrimary)
            }
        }
    }

    @Test
    fun test_tlc_scroll_next_item(@TestParameter wrap: Boolean) {
        val sdkGestureInputManager = SdkGestureInputManagerMock(false)
        val listState = TransformingLazyColumnState()
        val numberOfItems = 15
        val gestureConfig =
            OneHandedGestureConfiguration(
                action = OneHandedGestureAction.Primary,
                gestureId = "GestureId",
            )

        rule.setContentWithTheme {
            ScreenConfiguration(SCREEN_SIZE_SMALL) {
                MockSdkGestureInputManager(sdkGestureInputManager) {
                    TransformingLazyColumn(
                        state = listState,
                        modifier =
                            Modifier.background(Color.Black)
                                .fillMaxSize()
                                .oneHandedGesture(
                                    gestureConfiguration = gestureConfig,
                                    onGestureLabel = "scroll",
                                    onGesture = {
                                        OneHandedGestureDefaults.scrollDownToNextItem(
                                            listState,
                                            wrap,
                                        )
                                    },
                                ),
                    ) {
                        items(numberOfItems) { Text("Item $it") }
                    }
                }
            }
        }
        // On screen load, item 4 is the anchor item. At the top of the list, the gesture always
        // moves to the next item, even if the anchor item is not aligned with the viewport center.
        val initialAnchorIndex = 4

        var expectedIndex = initialAnchorIndex
        repeat(numberOfItems * 2) {
            rule.waitForIdle()
            assertEquals(expectedIndex, listState.anchorItemIndex)
            sdkGestureInputManager.performGesture(sdkActionPrimary)
            if (expectedIndex == numberOfItems - 5 /* last 4 items can't be scrolled */) {
                if (wrap) {
                    expectedIndex = initialAnchorIndex

                    // TLC has 4 items remaining below the viewport center that cannot be scrolled
                    // into focus

                    // Step 1: Scroll a few pixels to ensure the list hits its physical end and the
                    // last item is fully visible
                    sdkGestureInputManager.performGesture(sdkActionPrimary)
                    rule.waitForIdle()

                    // Step 2: Scroll back up to the first (0th) item.
                    sdkGestureInputManager.performGesture(sdkActionPrimary)
                    rule.waitForIdle()
                }
            } else {
                expectedIndex++
            }
        }
    }

    @Test
    fun test_tlc_scroll_next_item_at_top_with_anchor_below_center_scrolls_to_next_item() {
        val sdkGestureInputManager = SdkGestureInputManagerMock(false)
        val listState = TransformingLazyColumnState()
        val gestureConfig =
            OneHandedGestureConfiguration(
                action = OneHandedGestureAction.Primary,
                gestureId = "GestureId",
            )

        rule.setContentWithTheme {
            ScreenConfiguration(SCREEN_SIZE_SMALL) {
                MockSdkGestureInputManager(sdkGestureInputManager) {
                    TransformingLazyColumn(
                        state = listState,
                        // Large top padding places the first item below the viewport center.
                        contentPadding = PaddingValues(top = 120.dp),
                        modifier =
                            Modifier.background(Color.Black)
                                .fillMaxSize()
                                .oneHandedGesture(
                                    gestureConfiguration = gestureConfig,
                                    onGestureLabel = "scroll",
                                    onGesture = {
                                        OneHandedGestureDefaults.scrollDownToNextItem(listState)
                                    },
                                ),
                    ) {
                        items(10) {
                            Box(Modifier.fillMaxWidth().height(40.dp)) { Text("Item $it") }
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(listState.canScrollBackward).isFalse()
            assertEquals(0, listState.anchorItemIndex)
            assertThat(listState.anchorItemScrollOffset).isLessThan(0)
        }

        sdkGestureInputManager.performGesture(sdkActionPrimary)

        rule.runOnIdle { assertEquals(1, listState.anchorItemIndex) }
    }

    @Test
    fun test_pager_scroll_next_page(@TestParameter wrap: Boolean) {
        val sdkGestureInputManager = SdkGestureInputManagerMock(false)
        val numberOfPages = 5
        val pagerState = PagerState { numberOfPages }
        val gestureConfig =
            OneHandedGestureConfiguration(
                action = OneHandedGestureAction.Primary,
                gestureId = "GestureId",
            )

        rule.setContentWithTheme {
            ScreenConfiguration(SCREEN_SIZE_SMALL) {
                MockSdkGestureInputManager(sdkGestureInputManager) {
                    HorizontalPager(
                        state = pagerState,
                        modifier =
                            Modifier.background(Color.Black)
                                .fillMaxSize()
                                .oneHandedGesture(
                                    gestureConfiguration = gestureConfig,
                                    onGestureLabel = "scroll to next page",
                                    onGesture = {
                                        OneHandedGestureDefaults.scrollToNextPage(pagerState, wrap)
                                    },
                                ),
                    ) {
                        Text("Page $it")
                    }
                }
            }
        }

        var expectedIndex = 0
        repeat(numberOfPages * 2) {
            rule.runOnIdle {
                assertEquals(expectedIndex, pagerState.currentPage)
                sdkGestureInputManager.performGesture(sdkActionPrimary)
                if (expectedIndex == numberOfPages - 1) {
                    if (wrap) expectedIndex = 0
                } else {
                    expectedIndex++
                }
            }
        }
    }

    @Test
    fun gesture_id_uniqueness() {
        val sdkGestureInputManager = SdkGestureInputManagerMock()

        var clickableGestureConfig: OneHandedGestureConfiguration? = null
        var scrollableGestureConfig: OneHandedGestureConfiguration? = null

        rule.setContentWithTheme {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                clickableGestureConfig =
                    rememberOneHandedGestureConfiguration(
                        action = OneHandedGestureAction.Primary,
                        priority = OneHandedGesturePriority.Clickable,
                    )
                scrollableGestureConfig =
                    rememberOneHandedGestureConfiguration(
                        action = OneHandedGestureAction.Primary,
                        priority = OneHandedGesturePriority.Scrollable,
                    )
            }
        }

        assertNotNull(clickableGestureConfig)
        assertNotNull(scrollableGestureConfig)
        assertNotEquals(clickableGestureConfig.gestureId, scrollableGestureConfig.gestureId)
    }

    fun local_composition_disable_enable_gesture() {
        var gestured = false
        val sdkGestureInputManager = SdkGestureInputManagerMock()
        var enabled by mutableStateOf(false)

        rule.setContentWithTheme {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                CompositionLocalProvider(LocalOneHandedGestureEnabled provides enabled) {
                    val gestureConfig =
                        rememberOneHandedGestureConfiguration(
                            action = OneHandedGestureAction.Primary
                        )
                    Text(
                        "Clickable",
                        modifier =
                            Modifier.oneHandedGesture(
                                gestureConfiguration = gestureConfig,
                                onGestureLabel = "gesture",
                            ) {
                                gestured = true
                            },
                    )
                }
            }
        }

        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle { assertEquals(false, gestured) }

        enabled = true
        rule.waitForIdle()
        sdkGestureInputManager.performGesture(sdkActionPrimary)

        rule.runOnIdle { assertEquals(true, gestured) }
    }

    @Test
    fun local_composition_enable_disable_gesture() {
        var gestured = false
        val sdkGestureInputManager = SdkGestureInputManagerMock()
        var enabled by mutableStateOf(true)

        rule.setContentWithTheme {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                CompositionLocalProvider(LocalOneHandedGestureEnabled provides enabled) {
                    val gestureConfig =
                        rememberOneHandedGestureConfiguration(
                            action = OneHandedGestureAction.Primary
                        )
                    Text(
                        "Clickable",
                        modifier =
                            Modifier.oneHandedGesture(
                                gestureConfiguration = gestureConfig,
                                onGestureLabel = "gesture",
                            ) {
                                gestured = true
                            },
                    )
                }
            }
        }

        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle { assertEquals(true, gestured) }

        gestured = false
        enabled = false
        rule.waitForIdle()
        sdkGestureInputManager.performGesture(sdkActionPrimary)

        rule.runOnIdle { assertEquals(false, gestured) }
    }

    @Test
    fun gesture_indicator_colors() {
        val tintColor = Color.Yellow
        val gestureConfig =
            OneHandedGestureConfiguration(
                action = OneHandedGestureAction.Primary,
                gestureId = "gestureId",
            )
        val indicatorState = OneHandedGestureClickIndicatorState()

        rule.verifyColors(
            activate = { launch { indicatorState.showIndicator() } },
            expectedContentColor = tintColor,
        ) {
            OneHandedGestureClickIndicator(
                gestureConfiguration = gestureConfig,
                state = indicatorState,
                gestureIndicatorTint = tintColor,
                modifier = Modifier.testTag(TEST_TAG),
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "",
                    modifier = Modifier.size(OneHandedGestureIndicatorSize.Medium.size),
                )
            }
        }
    }

    @Test
    fun gesture_scroll_indicator_colors() {
        val tintColor = Color.Yellow
        val containerColor = Color.Blue
        val gestureConfig =
            OneHandedGestureConfiguration(
                action = OneHandedGestureAction.Primary,
                gestureId = "test",
            )
        lateinit var indicatorState: OneHandedGestureScrollIndicatorState

        rule.verifyColors(
            activate = { launch { indicatorState.showIndicator() } },
            expectedContentColor = tintColor,
            expectedContainerColor = containerColor,
        ) {
            indicatorState = remember(gestureConfig) { OneHandedGestureScrollIndicatorState() }

            Box(modifier = Modifier.testTag(TEST_TAG)) {
                OneHandedGestureScrollIndicator(
                    gestureConfiguration = gestureConfig,
                    indicatorState = indicatorState,
                    gestureIndicatorTint = tintColor,
                    gestureIndicatorBackgroundColor = containerColor,
                    scrollState = rememberTransformingLazyColumnState(),
                )
            }
        }
    }

    @Test
    fun gesture_horizontal_page_indicator_colors() {
        val tintColor = Color.Yellow
        val containerColor = Color.Blue
        lateinit var indicatorState: OneHandedGesturePageIndicatorState

        rule.verifyColors(
            activate = { launch { indicatorState.showIndicator() } },
            expectedContentColor = tintColor,
            expectedContainerColor = containerColor,
        ) {
            val gestureConfig =
                rememberOneHandedGestureConfiguration(action = OneHandedGestureAction.Primary)
            indicatorState = remember { OneHandedGesturePageIndicatorState() }
            Box(modifier = Modifier.testTag(TEST_TAG)) {
                OneHandedGestureHorizontalPageIndicator(
                    gestureConfiguration = gestureConfig,
                    indicatorState = indicatorState,
                    gestureIndicatorTint = tintColor,
                    gestureIndicatorBackgroundColor = containerColor,
                    pagerState = rememberPagerState { 0 },
                )
            }
        }
    }

    @Test
    fun gesture_vertical_page_indicator_colors() {
        val tintColor = Color.Yellow
        val containerColor = Color.Blue
        lateinit var indicatorState: OneHandedGesturePageIndicatorState

        rule.verifyColors(
            activate = { launch { indicatorState.showIndicator() } },
            expectedContentColor = tintColor,
            expectedContainerColor = containerColor,
        ) {
            val gestureConfig =
                rememberOneHandedGestureConfiguration(action = OneHandedGestureAction.Primary)
            indicatorState = remember { OneHandedGesturePageIndicatorState() }
            Box(modifier = Modifier.testTag(TEST_TAG)) {
                OneHandedGestureVerticalPageIndicator(
                    gestureConfiguration = gestureConfig,
                    indicatorState = indicatorState,
                    gestureIndicatorTint = tintColor,
                    gestureIndicatorBackgroundColor = containerColor,
                    pagerState = rememberPagerState { 0 },
                )
            }
        }
    }

    @Test
    fun test_accessibility_primary_dismiss() {
        val sdkGestureInputManager = SdkGestureInputManagerMock()
        val primaryLabel = "primary"
        val dismissLabel = "dismiss"

        rule.setContentWithTheme {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                Text(
                    "Primary",
                    modifier =
                        Modifier.oneHandedGesture(
                            gestureConfiguration =
                                rememberOneHandedGestureConfiguration(
                                    OneHandedGestureAction.Primary
                                ),
                            onGestureLabel = primaryLabel,
                        ) {},
                )

                Text(
                    "Dismiss",
                    modifier =
                        Modifier.oneHandedGesture(
                            gestureConfiguration =
                                rememberOneHandedGestureConfiguration(
                                    OneHandedGestureAction.Dismiss
                                ),
                            onGestureLabel = dismissLabel,
                        ) {},
                )
            }
        }

        // It takes at least a second for indicator to be shown. Fast-forward 3s to allow some delay
        rule.mainClock.advanceTimeBy(3000)
        rule.waitForIdle()

        // Verify that correct content description is set for a11y
        val context = ApplicationProvider.getApplicationContext<Context>()
        val primaryExpectedText =
            context.getString(
                R.string.one_handed_gesture_primary_action_accessibility_text,
                primaryLabel,
            )
        val dismissExpectedText =
            context.getString(
                R.string.one_handed_gesture_dismiss_action_accessibility_text,
                dismissLabel,
            )
        onView(withContentDescription(primaryExpectedText)).checkExists()
        onView(withContentDescription(dismissExpectedText)).checkExists()
    }

    @Test
    fun test_accessibility_change_priority() {
        val sdkGestureInputManager = SdkGestureInputManagerMock()
        val primaryLabelScrollable = "primary scroll"
        val primaryLabelClickable = "primary button"
        var showClickable by mutableStateOf(true)

        rule.setContentWithTheme {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                Text(
                    "Primary",
                    modifier =
                        Modifier.oneHandedGesture(
                            gestureConfiguration =
                                rememberOneHandedGestureConfiguration(
                                    action = OneHandedGestureAction.Primary,
                                    priority = OneHandedGesturePriority.Scrollable,
                                ),
                            onGestureLabel = primaryLabelScrollable,
                        ) {},
                )

                if (showClickable) {
                    Text(
                        "Dismiss",
                        modifier =
                            Modifier.oneHandedGesture(
                                gestureConfiguration =
                                    rememberOneHandedGestureConfiguration(
                                        action = OneHandedGestureAction.Primary,
                                        priority = OneHandedGesturePriority.Clickable,
                                    ),
                                onGestureLabel = primaryLabelClickable,
                            ) {},
                    )
                }
            }
        }

        // It takes at least a second for accessibility to trigger. Fast-forward 3s to allow some
        // delay
        rule.mainClock.advanceTimeBy(3000)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val primaryExpectedClickableText =
            context.getString(
                R.string.one_handed_gesture_primary_action_accessibility_text,
                primaryLabelClickable,
            )
        val primaryExpectedScrollableText =
            context.getString(
                R.string.one_handed_gesture_primary_action_accessibility_text,
                primaryLabelScrollable,
            )
        onView(withContentDescription(primaryExpectedClickableText)).checkExists()
        onView(withContentDescription(primaryExpectedScrollableText)).checkDoesNotExist()

        // Hide Button with Clickable priority and test that a11y text was updated to Scrollable
        showClickable = false
        rule.mainClock.advanceTimeBy(3000)
        onView(withContentDescription(primaryExpectedClickableText)).checkDoesNotExist()
        onView(withContentDescription(primaryExpectedScrollableText)).checkExists()

        // Show Button with Clickable priority and test that a11y text was updated back to Clickable
        showClickable = true
        rule.mainClock.advanceTimeBy(3000)
        onView(withContentDescription(primaryExpectedClickableText)).checkExists()
        onView(withContentDescription(primaryExpectedScrollableText)).checkDoesNotExist()
    }

    @Test
    fun test_accessibility_null() {
        val sdkGestureInputManager = SdkGestureInputManagerMock()

        rule.setContentWithTheme {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                Text(
                    "Gesturable text",
                    modifier =
                        Modifier.oneHandedGesture(
                            gestureConfiguration =
                                rememberOneHandedGestureConfiguration(
                                    action = OneHandedGestureAction.Primary
                                ),
                            onGestureLabel = null,
                        ) {},
                )
            }
        }

        // It takes at least a second for accessibility to trigger. Fast-forward 3s to allow some
        // delay
        rule.mainClock.advanceTimeBy(3000)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val unexpectedContentDescription =
            context.getString(R.string.one_handed_gesture_primary_action_accessibility_text, "")
        onView(withContentDescription(startsWith(unexpectedContentDescription))).checkDoesNotExist()
    }

    @Test
    fun test_gesture_indicator_registered_and_shown() =
        verifyIndicatorRegistration(expectShown = true) { gestureConfig, indicatorState, _, _ ->
            val coroutineScope = rememberCoroutineScope()

            Button(
                onClick = {},
                modifier =
                    Modifier.oneHandedGesture(
                        gestureConfiguration = gestureConfig,
                        onGestureLabel = "scroll",
                        onGestureAvailable = {
                            coroutineScope.launch { indicatorState.showIndicator() }
                        },
                        onGesture = {},
                    ),
            ) {
                OneHandedGestureClickIndicator(
                    gestureConfiguration = gestureConfig,
                    indicatorState,
                ) {
                    Text("Click")
                }
            }
        }

    @Test
    fun test_gesture_indicator_not_shown() =
        verifyIndicatorRegistration(expectShown = false) { gestureConfig, indicatorState, _, _ ->
            Button(
                onClick = {},
                modifier =
                    Modifier.oneHandedGesture(
                        gestureConfiguration = gestureConfig,
                        onGestureLabel = "scroll",
                        onGestureAvailable = {
                            // Indicator not shown
                        },
                        onGesture = {},
                    ),
            ) {
                OneHandedGestureClickIndicator(
                    gestureConfiguration = gestureConfig,
                    indicatorState,
                ) {
                    Text("Click")
                }
            }
        }

    @Test
    fun test_scroll_gesture_indicator_registered_and_shown() =
        verifyIndicatorRegistration(expectShown = true) { gestureConfig, _, scrollIndicatorState, _
            ->
            val coroutineScope = rememberCoroutineScope()
            val scrollState = rememberTransformingLazyColumnState()

            ScreenScaffold(
                scrollState = scrollState,
                scrollIndicator = {
                    OneHandedGestureScrollIndicator(
                        gestureConfiguration = gestureConfig,
                        scrollIndicatorState,
                        scrollState,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                },
            ) { contentPadding ->
                TransformingLazyColumn(
                    state = scrollState,
                    contentPadding = contentPadding,
                    modifier =
                        Modifier.oneHandedGesture(
                            gestureConfig,
                            onGestureLabel = "scroll",
                            onGestureAvailable = {
                                coroutineScope.launch { scrollIndicatorState.showIndicator() }
                            },
                            onGesture = {},
                        ),
                ) {
                    item { Button(onClick = {}) { Text("Click") } }
                }
            }
        }

    @Test
    fun test_scroll_gesture_indicator_not_shown() =
        verifyIndicatorRegistration(expectShown = false) { gestureConfig, _, scrollIndicatorState, _
            ->
            val scrollState = rememberTransformingLazyColumnState()
            ScreenScaffold(
                scrollState = scrollState,
                scrollIndicator = {
                    OneHandedGestureScrollIndicator(
                        gestureConfiguration = gestureConfig,
                        scrollIndicatorState,
                        scrollState,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                },
            ) { contentPadding ->
                TransformingLazyColumn(
                    state = scrollState,
                    contentPadding = contentPadding,
                    modifier =
                        Modifier.oneHandedGesture(
                            gestureConfig,
                            onGestureLabel = "scroll",
                            onGestureAvailable = { /* Indicator not shown */ },
                            onGesture = {},
                        ),
                ) {
                    item { Button(onClick = {}) { Text("Click") } }
                }
            }
        }

    @Test
    fun test_horizontal_page_gesture_indicator_registered_and_shown() =
        verifyIndicatorRegistration(expectShown = true) { gestureConfig, _, _, pageIndicatorState ->
            val coroutineScope = rememberCoroutineScope()
            val pagerState = rememberPagerState(pageCount = { 3 })

            HorizontalPagerScaffold(
                pagerState = pagerState,
                pageIndicator = {
                    OneHandedGestureHorizontalPageIndicator(
                        gestureConfiguration = gestureConfig,
                        indicatorState = pageIndicatorState,
                        pagerState = pagerState,
                    )
                },
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier =
                        Modifier.oneHandedGesture(
                            gestureConfiguration = gestureConfig,
                            onGestureLabel = "Page",
                            onGestureAvailable = {
                                coroutineScope.launch { pageIndicatorState.showIndicator() }
                            },
                            onGesture = {},
                        ),
                ) { page ->
                    Text(text = "Page $page")
                }
            }
        }

    @Test
    fun test_horizontal_page_gesture_indicator_not_shown() =
        verifyIndicatorRegistration(expectShown = false) { gestureConfig, _, _, pageIndicatorState
            ->
            val pagerState = rememberPagerState(pageCount = { 3 })

            HorizontalPagerScaffold(
                pagerState = pagerState,
                pageIndicator = {
                    OneHandedGestureHorizontalPageIndicator(
                        gestureConfiguration = gestureConfig,
                        indicatorState = pageIndicatorState,
                        pagerState = pagerState,
                    )
                },
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier =
                        Modifier.oneHandedGesture(
                            gestureConfiguration = gestureConfig,
                            onGestureLabel = "Page",
                            onGestureAvailable = {
                                // Indicator not shown
                            },
                            onGesture = {},
                        ),
                ) { page ->
                    Text(text = "Page $page")
                }
            }
        }

    @Test
    fun test_vertical_page_gesture_indicator_registered_and_shown() =
        verifyIndicatorRegistration(expectShown = true) { gestureConfig, _, _, pageIndicatorState ->
            val coroutineScope = rememberCoroutineScope()
            val pagerState = rememberPagerState(pageCount = { 3 })

            VerticalPagerScaffold(
                pagerState = pagerState,
                pageIndicator = {
                    OneHandedGestureVerticalPageIndicator(
                        gestureConfiguration = gestureConfig,
                        indicatorState = pageIndicatorState,
                        pagerState = pagerState,
                    )
                },
            ) {
                VerticalPager(
                    state = pagerState,
                    modifier =
                        Modifier.oneHandedGesture(
                            gestureConfiguration = gestureConfig,
                            onGestureLabel = "Page",
                            onGestureAvailable = {
                                coroutineScope.launch { pageIndicatorState.showIndicator() }
                            },
                            onGesture = {},
                        ),
                ) { page ->
                    Text(text = "Page $page")
                }
            }
        }

    @Test
    fun test_vertical_page_gesture_indicator_not_shown() =
        verifyIndicatorRegistration(expectShown = false) { gestureConfig, _, _, pageIndicatorState
            ->
            val pagerState = rememberPagerState(pageCount = { 3 })

            VerticalPagerScaffold(
                pagerState = pagerState,
                pageIndicator = {
                    OneHandedGestureVerticalPageIndicator(
                        gestureConfiguration = gestureConfig,
                        indicatorState = pageIndicatorState,
                        pagerState = pagerState,
                    )
                },
            ) {
                VerticalPager(
                    state = pagerState,
                    modifier =
                        Modifier.oneHandedGesture(
                            gestureConfiguration = gestureConfig,
                            onGestureLabel = "Page",
                            onGestureAvailable = {
                                // Indicator not shown
                            },
                            onGesture = {},
                        ),
                ) { page ->
                    Text(text = "Page $page")
                }
            }
        }

    @Test
    fun gesture_scroll_indicator_scroll_offset_not_changed() {
        lateinit var coroutineScope: CoroutineScope
        val scrollState = TransformingLazyColumnState()
        val indicatorState = OneHandedGestureScrollIndicatorState()

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val gestureConfig =
                rememberOneHandedGestureConfiguration(
                    action = OneHandedGestureAction.Primary,
                    priority = OneHandedGesturePriority.Scrollable,
                )

            ScreenScaffold(
                scrollState = scrollState,
                scrollIndicator = {
                    OneHandedGestureScrollIndicator(
                        gestureConfiguration = gestureConfig,
                        indicatorState = indicatorState,
                        scrollState = scrollState,
                    )
                },
            ) {
                TransformingLazyColumn(state = scrollState, modifier = Modifier.fillMaxSize()) {
                    items(20) { index -> Text("Item $index") }
                }
            }
        }
        rule.waitForIdle()
        val initialIndex = scrollState.anchorItemIndex
        val initialOffset = scrollState.anchorItemScrollOffset

        coroutineScope.launch { indicatorState.showIndicator() }
        // Advance mid-animation and verify offset is unchanged
        rule.mainClock.advanceTimeBy(1000)
        assertThat(scrollState.anchorItemScrollOffset).isEqualTo(initialOffset)
        assertThat(scrollState.anchorItemIndex).isEqualTo(initialIndex)
        // Advance to completion and verify offset is still unchanged
        rule.mainClock.advanceTimeBy(3000)
        rule.waitForIdle()
        assertThat(scrollState.anchorItemScrollOffset).isEqualTo(initialOffset)
        assertThat(scrollState.anchorItemIndex).isEqualTo(initialIndex)
    }

    @Test
    fun gesture_scroll_indicator_keeps_visible_during_animation() {
        lateinit var coroutineScope: CoroutineScope
        val scrollState = TransformingLazyColumnState()
        val indicatorState = OneHandedGestureScrollIndicatorState()

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val gestureConfig =
                rememberOneHandedGestureConfiguration(
                    action = OneHandedGestureAction.Primary,
                    priority = OneHandedGesturePriority.Scrollable,
                )

            ScreenScaffold(
                scrollState = scrollState,
                scrollIndicator = {
                    OneHandedGestureScrollIndicator(
                        gestureConfiguration = gestureConfig,
                        indicatorState = indicatorState,
                        scrollState = scrollState,
                    )
                },
            ) {
                TransformingLazyColumn(state = scrollState, modifier = Modifier.fillMaxSize()) {
                    items(20) { index -> Text("Item $index") }
                }
            }
        }
        rule.waitForIdle()
        assertThat(indicatorState.keepIndicatorVisible?.value).isFalse()
        coroutineScope.launch { indicatorState.showIndicator() }

        // Advance to mid-animation and verify keepIndicatorVisible is true
        rule.mainClock.advanceTimeBy(1000)
        assertThat(indicatorState.keepIndicatorVisible?.value).isTrue()
        // Advance to completion and verify keepIndicatorVisible is false
        rule.mainClock.advanceTimeBy(3000)
        rule.waitForIdle()
        assertThat(indicatorState.keepIndicatorVisible?.value).isFalse()
    }

    @Test
    fun gesture_scroll_indicator_cancelled_while_animating_resets_keep_visible() {
        lateinit var coroutineScope: CoroutineScope
        val scrollState = TransformingLazyColumnState()
        val indicatorState = OneHandedGestureScrollIndicatorState()

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val gestureConfig =
                rememberOneHandedGestureConfiguration(
                    action = OneHandedGestureAction.Primary,
                    priority = OneHandedGesturePriority.Scrollable,
                )
            ScreenScaffold(
                scrollState = scrollState,
                scrollIndicator = {
                    OneHandedGestureScrollIndicator(
                        gestureConfiguration = gestureConfig,
                        indicatorState = indicatorState,
                        scrollState = scrollState,
                    )
                },
            ) {
                TransformingLazyColumn(state = scrollState, modifier = Modifier.fillMaxSize()) {
                    items(20) { index -> Text("Item $index") }
                }
            }
        }
        rule.waitForIdle()
        assertThat(indicatorState.keepIndicatorVisible?.value).isFalse()
        val hintJob = coroutineScope.launch { indicatorState.showIndicator() }

        // Advance to mid-animation and verify keepIndicatorVisible is true
        rule.mainClock.advanceTimeBy(1000)
        assertThat(indicatorState.keepIndicatorVisible?.value).isTrue()

        // Cancel hint animation and verify keepIndicatorVisible is false
        hintJob.cancel()
        rule.mainClock.advanceTimeBy(100)
        rule.waitForIdle()
        assertThat(indicatorState.keepIndicatorVisible?.value).isFalse()
    }

    @Test
    fun gesture_scroll_indicator_disposed_clears_keep_indicator_visible() {
        lateinit var coroutineScope: CoroutineScope
        val scrollState = TransformingLazyColumnState()
        val indicatorState = OneHandedGestureScrollIndicatorState()
        var showIndicatorComposable by mutableStateOf(true)
        lateinit var scaffoldState: ScaffoldState

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            AppScaffold {
                scaffoldState = LocalScaffoldState.current!!
                val gestureConfig =
                    rememberOneHandedGestureConfiguration(
                        action = OneHandedGestureAction.Primary,
                        priority = OneHandedGesturePriority.Scrollable,
                    )
                ScreenScaffold(
                    scrollState = scrollState,
                    scrollIndicator = {
                        if (showIndicatorComposable) {
                            OneHandedGestureScrollIndicator(
                                gestureConfiguration = gestureConfig,
                                indicatorState = indicatorState,
                                scrollState = scrollState,
                            )
                        }
                    },
                ) {
                    TransformingLazyColumn(state = scrollState, modifier = Modifier.fillMaxSize()) {
                        items(20) { index -> Text("Item $index") }
                    }
                }
            }
        }
        rule.waitForIdle()
        assertThat(indicatorState.keepIndicatorVisible?.value).isFalse()
        assertThat(scaffoldState.keepIndicatorVisible.value).isFalse()

        // Advance to mid-animation and verify keepIndicatorVisible is true
        coroutineScope.launch { indicatorState.showIndicator() }
        rule.mainClock.advanceTimeBy(1000)
        assertThat(indicatorState.keepIndicatorVisible?.value).isTrue()
        assertThat(scaffoldState.keepIndicatorVisible.value).isTrue()

        // Dispose the indicator composable while animating
        rule.runOnIdle { showIndicatorComposable = false }
        rule.waitForIdle()

        // Verify keepIndicatorVisible reference is cleared and scaffold state is reset to false
        assertThat(indicatorState.keepIndicatorVisible).isNull()
        assertThat(scaffoldState.keepIndicatorVisible.value).isFalse()
    }

    /**
     * VerifyIndicatorRegistration expects only one of OneHandedGestureIndicatorState,
     * OneHandedScrollGestureIndicatorState, OneHandedPageIndicatorState is used per test.
     */
    private fun verifyIndicatorRegistration(
        expectShown: Boolean,
        indicatorContent:
            @Composable
            CoroutineScope.(
                gestureConfig: OneHandedGestureConfiguration,
                indicatorState: OneHandedGestureClickIndicatorState,
                scrollIndicatorState: OneHandedGestureScrollIndicatorState,
                pageIndicatorState: OneHandedGesturePageIndicatorState,
            ) -> Unit,
    ) {
        val sdkGestureInputManager = SdkGestureInputManagerMock(showIndicator = true)
        var localGestureManager: OneHandedGestureManager? = null
        lateinit var gestureConfig: OneHandedGestureConfiguration

        rule.setContentWithTheme {
            val coroutineScope = rememberCoroutineScope()

            gestureConfig =
                rememberOneHandedGestureConfiguration(
                    action = OneHandedGestureAction.Primary,
                    priority = OneHandedGesturePriority.Clickable,
                )

            // We expect only one of these is used in the test (hence only one config definition).
            val indicatorState = remember { OneHandedGestureClickIndicatorState() }
            val scrollIndicatorState =
                remember(gestureConfig) { OneHandedGestureScrollIndicatorState() }
            val pageIndicatorState = remember { OneHandedGesturePageIndicatorState() }

            MockSdkGestureInputManager(sdkGestureInputManager) {
                localGestureManager = LocalOneHandedGestureManager.current

                coroutineScope.indicatorContent(
                    gestureConfig,
                    indicatorState,
                    scrollIndicatorState,
                    pageIndicatorState,
                )
            }
        }

        rule.waitForIdle()

        // Verify the gesture indicator registered itself
        val registeredIndicator = localGestureManager?.getRegisteredGestureIndicator(gestureConfig)

        assertNotNull(registeredIndicator)

        // Fast-forward so that onGestureAvailable -> showIndicator
        rule.mainClock.advanceTimeBy(GESTURE_AVAILABLE_DELAY)
        rule.waitForIdle()

        rule.runOnIdle {
            if (expectShown) {
                assertEquals(gestureConfig.gestureId, sdkGestureInputManager.lastNotifiedGestureId)
                assertEquals(sdkActionPrimary, sdkGestureInputManager.lastNotifiedSdkGestureAction)
            } else {
                assertEquals(null, sdkGestureInputManager.lastNotifiedGestureId)
                assertEquals(null, sdkGestureInputManager.lastNotifiedSdkGestureAction)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    internal fun ComposeContentTestRule.verifyColors(
        activate: CoroutineScope.() -> Unit,
        expectedContentColor: Color,
        expectedContainerColor: Color? = null,
        content: @Composable BoxScope.() -> Unit,
    ) {
        val sdkGestureInputManager = SdkGestureInputManagerMock()
        val testBackgroundColor = Color.White
        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false
        setContentWithTheme {
            scope = rememberCoroutineScope()
            MockSdkGestureInputManager(sdkGestureInputManager) {
                Box(Modifier.fillMaxSize().background(testBackgroundColor), content = content)
            }
        }

        scope.activate()
        rule.waitForIdle()

        // Advance alpha animation of gesture indicator. After this, gesture should be fully visible
        rule.mainClock.advanceTimeBy(INDICATOR_ANIMATION_START_DELAY_MILLIS)

        val image = onNodeWithTag(TEST_TAG).captureToImage()

        expectedContainerColor?.let { image.assertContainsColor(it) }

        image.assertContainsColor(expectedContentColor)
    }

    @Composable
    private fun MockSdkGestureInputManager(
        sdkGestureInputManager: SdkGestureInputManager,
        results: MutableMap<HapticFeedbackType, Int> = mutableMapOf(),
        content: @Composable () -> Unit,
    ) {
        val scope: CoroutineScope = rememberCoroutineScope()
        val haptic = hapticFeedback(collectResultsFromHapticFeedback(results))
        val gestureManager =
            remember(scope) { OneHandedGestureManagerImpl(scope, sdkGestureInputManager) }

        CompositionLocalProvider(
            LocalOneHandedGestureManager provides gestureManager,
            LocalHapticFeedback provides haptic,
        ) {
            content()
        }
    }

    @Composable
    private fun InteractionSource.ListenForInteractions(onPressInteraction: (Offset) -> Unit = {}) {
        LaunchedEffect(this) {
            interactions.collect { interaction ->
                if (interaction is PressInteraction.Press) {
                    onPressInteraction(interaction.pressPosition)
                }
            }
        }
    }

    private class SdkGestureInputManagerMock(private val showIndicator: Boolean = true) :
        SdkGestureInputManager {
        override fun isAvailable(context: Context): Boolean = true

        override fun subscribeToSdkGestureAction(
            view: View,
            sdkGestureAction: Int,
            enabledInAmbient: Boolean,
            onGesture: (Int) -> Unit,
        ) {
            gestureConsumers[sdkGestureAction] = onGesture
        }

        override fun unsubscribeFromSdkGestureAction(view: View, sdkGestureAction: Int) {
            gestureConsumers.remove(sdkGestureAction)
        }

        override fun notifyGestureConsumed(gestureId: String, sdkGestureAction: Int) {}

        override fun shouldShowIndicator(
            gestureId: String,
            sdkGestureAction: Int,
            isOverlay: Boolean,
        ): Boolean = showIndicator

        var lastNotifiedGestureId: String? = null
        var lastNotifiedSdkGestureAction: Int? = null

        override fun notifyIndicatorShown(gestureId: String, sdkGestureAction: Int) {
            lastNotifiedGestureId = gestureId
            lastNotifiedSdkGestureAction = sdkGestureAction
        }

        fun performGesture(sdkGestureAction: Int) {
            gestureConsumers[sdkGestureAction]?.invoke(sdkGestureAction)
        }

        private val gestureConsumers = mutableMapOf<Int, (Int) -> Unit>()
    }

    private fun verifyShowIndicatorDelay(
        showIndicatorDelayMillis: Long?,
        expectedDelayMillis: Long,
    ) {
        var gestureAvailable = false
        val sdkGestureInputManager = SdkGestureInputManagerMock()

        rule.mainClock.autoAdvance = false
        rule.setContentWithTheme {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                val gestureConfig =
                    rememberOneHandedGestureConfiguration(action = OneHandedGestureAction.Primary)
                val onGestureAvailable = { gestureAvailable = true }
                Box(
                    Modifier.size(50.dp)
                        .then(
                            if (showIndicatorDelayMillis == null) {
                                Modifier.oneHandedGesture(
                                    gestureConfiguration = gestureConfig,
                                    onGestureLabel = "click",
                                    onGestureAvailable = onGestureAvailable,
                                ) {}
                            } else {
                                Modifier.oneHandedGesture(
                                    gestureConfiguration = gestureConfig,
                                    onGestureLabel = "click",
                                    showIndicatorDelayMillis = showIndicatorDelayMillis,
                                    onGestureAvailable = onGestureAvailable,
                                ) {}
                            }
                        )
                )
            }
        }
        rule.waitForIdle()

        if (expectedDelayMillis > 0) {
            rule.mainClock.advanceTimeBy(expectedDelayMillis - 100)
            rule.runOnIdle { assertEquals(false, gestureAvailable) }
        }

        rule.mainClock.advanceTimeBy(200)
        rule.runOnIdle { assertEquals(true, gestureAvailable) }
    }

    private fun ViewInteraction.checkExists(): ViewInteraction =
        this.check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

    private fun ViewInteraction.checkDoesNotExist(): ViewInteraction = this.check(doesNotExist())

    /* Copy from com.google.wear.input.GestureEvent class */
    private val sdkActionDismiss = 2
    private val sdkActionPrimary = 1

    enum class TestParamScalingLazyListAnchorType(val type: ScalingLazyListAnchorType) {
        ItemStart(ScalingLazyListAnchorType.ItemStart),
        ItemCenter(ScalingLazyListAnchorType.ItemCenter),
    }

    private val GESTURE_AVAILABLE_DELAY = 3000L
}
