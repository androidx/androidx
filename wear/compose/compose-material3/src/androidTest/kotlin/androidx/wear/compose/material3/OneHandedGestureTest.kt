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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertContainsColor
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.test.filters.MediumTest
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.material3.onehandedgesture.GestureAction
import androidx.wear.compose.material3.onehandedgesture.GestureIndicatorSize
import androidx.wear.compose.material3.onehandedgesture.GestureManagerImpl
import androidx.wear.compose.material3.onehandedgesture.GesturePriority
import androidx.wear.compose.material3.onehandedgesture.INDICATOR_ANIMATION_START_DELAY_MILLIS
import androidx.wear.compose.material3.onehandedgesture.LocalGestureManager
import androidx.wear.compose.material3.onehandedgesture.LocalOneHandedGestureEnabled
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureDefaults
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureHorizontalPageIndicator
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureIndicator
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureInteraction
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureScrollIndicator
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureVerticalPageIndicator
import androidx.wear.compose.material3.onehandedgesture.SdkGestureInputManager
import androidx.wear.compose.material3.onehandedgesture.oneHandedGesture
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(TestParameterInjector::class)
class OneHandedGestureTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    /** Verifies simple primary gesture */
    @Test
    fun simple_primary_gesture() {
        var gestured = false
        var indicatorAction: GestureAction? = null
        var pressCoordinates: Offset = Offset.Zero
        var textSize: IntSize = IntSize.Zero
        val sdkGestureInputManager = SdkGestureInputManagerMock()
        val hapticResults = mutableMapOf<HapticFeedbackType, Int>()

        rule.setContentWithTheme {
            val interactionSource = remember { MutableInteractionSource() }
            MockSdkGestureInputManager(sdkGestureInputManager, hapticResults) {
                Text(
                    "Clickable",
                    modifier =
                        Modifier.onSizeChanged { textSize = it }
                            .oneHandedGesture(
                                action = GestureAction.Primary,
                                interactionSource = interactionSource,
                            ) {
                                gestured = true
                            },
                )
            }

            interactionSource.ListenForInteractions(
                onPressInteraction = { pressCoordinates = it },
                onGestureInteraction = { interaction -> indicatorAction = interaction.action },
            )
        }

        // It takes at least a second for indicator to be shown. Fast-forward 3s to allow some delay
        rule.mainClock.advanceTimeBy(3000)

        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle {
            assertEquals(true, gestured)
            assertEquals(GestureAction.Primary, indicatorAction)
            assertEquals(textSize.center.toOffset(), pressCoordinates)

            assertThat(hapticResults).hasSize(1)
            assertEquals(hapticResults[HapticFeedbackType.LongPress], 1)
        }
    }

    /** Verifies that gesture isn't triggered when LocalOneHandedGestureEnabled is false */
    @Test
    fun local_composition_disable_gesture() {
        var gestured = false
        val sdkGestureInputManager = SdkGestureInputManagerMock()
        val hapticResults = mutableMapOf<HapticFeedbackType, Int>()

        rule.setContentWithTheme {
            MockSdkGestureInputManager(sdkGestureInputManager, hapticResults) {
                // Disable gestures with LocalOneHandedGestureEnabled
                CompositionLocalProvider(LocalOneHandedGestureEnabled provides false) {
                    Text(
                        "Clickable",
                        modifier =
                            Modifier.oneHandedGesture(action = GestureAction.Primary) {
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
    }

    /** Verifies simple Dismiss gesture */
    @Test
    fun simple_dismiss_gesture() {
        var gestured = false
        var indicatorAction: GestureAction? = null
        var pressCoordinates: Offset = Offset.Zero
        var textSize: IntSize = IntSize.Zero
        val sdkGestureInputManager = SdkGestureInputManagerMock()
        val hapticResults = mutableMapOf<HapticFeedbackType, Int>()

        rule.setContentWithTheme {
            val interactionSource = remember { MutableInteractionSource() }
            MockSdkGestureInputManager(sdkGestureInputManager, hapticResults) {
                Text(
                    "Clickable",
                    modifier =
                        Modifier.onSizeChanged { textSize = it }
                            .oneHandedGesture(
                                action = GestureAction.Dismiss,
                                interactionSource = interactionSource,
                            ) {
                                gestured = true
                            },
                )
            }
            interactionSource.ListenForInteractions(
                onPressInteraction = { pressCoordinates = it },
                onGestureInteraction = { interaction -> indicatorAction = interaction.action },
            )
        }

        // It takes at least a second for indicator to be shown. Fast-forward 3s to allow some delay
        rule.mainClock.advanceTimeBy(3000)

        sdkGestureInputManager.performGesture(sdkActionDismiss)

        rule.runOnIdle {
            assertEquals(true, gestured)
            assertEquals(GestureAction.Dismiss, indicatorAction)
            assertEquals(textSize.center.toOffset(), pressCoordinates)

            assertThat(hapticResults).hasSize(1)
            assertEquals(hapticResults[HapticFeedbackType.LongPress], 1)
        }
    }

    /** Verifies that Clickable priority is higher than Scrollable */
    @Test
    fun clickable_over_scrollable() {
        var tlcGestured = false
        var textGestured = false
        var tlcIndicatorAction: GestureAction? = null
        var textIndicatorAction: GestureAction? = null
        val sdkGestureInputManager = SdkGestureInputManagerMock()

        rule.setContentWithTheme {
            val tlcInteractionSource = remember { MutableInteractionSource() }
            val textInteractionSource = remember { MutableInteractionSource() }
            MockSdkGestureInputManager(sdkGestureInputManager) {
                TransformingLazyColumn(
                    modifier =
                        Modifier.oneHandedGesture(
                            action = GestureAction.Primary,
                            priority = GesturePriority.Scrollable,
                            interactionSource = tlcInteractionSource,
                        ) {
                            tlcGestured = true
                        }
                ) {
                    item {
                        Text(
                            "Clickable",
                            modifier =
                                Modifier.oneHandedGesture(
                                    action = GestureAction.Primary,
                                    priority = GesturePriority.Clickable,
                                    interactionSource = textInteractionSource,
                                ) {
                                    textGestured = true
                                },
                        )
                    }
                }
            }

            tlcInteractionSource.ListenForInteractions { interaction ->
                tlcIndicatorAction = interaction.action
            }

            textInteractionSource.ListenForInteractions { interaction ->
                textIndicatorAction = interaction.action
            }
        }

        // It takes at least a second for indicator to be shown. Wait for 3s to allow some delay
        rule.mainClock.advanceTimeBy(3000)

        sdkGestureInputManager.performGesture(sdkActionPrimary)
        rule.runOnIdle {
            assertEquals(null, tlcIndicatorAction)
            assertEquals(false, tlcGestured)
            assertEquals(GestureAction.Primary, textIndicatorAction)
            assertEquals(true, textGestured)
        }
    }

    /** Verifies that all gestures with the same priority are triggered */
    @Test
    fun two_gestures_same_priority() {
        var tlcGestured = false
        val textGestured = mutableListOf(false, false)
        val textIndicatorActions = mutableListOf<GestureAction?>(null, null)
        val sdkGestureInputManager = SdkGestureInputManagerMock()

        rule.setContentWithTheme {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                TransformingLazyColumn(
                    modifier =
                        Modifier.oneHandedGesture(
                            action = GestureAction.Primary,
                            priority = GesturePriority.Scrollable,
                        ) {
                            tlcGestured = true
                        }
                ) {
                    items(2) { index ->
                        val interactionSource = remember { MutableInteractionSource() }
                        Text(
                            "Clickable$index",
                            modifier =
                                Modifier.oneHandedGesture(
                                    action = GestureAction.Primary,
                                    priority = GesturePriority.Clickable,
                                    interactionSource = interactionSource,
                                ) {
                                    textGestured[index] = true
                                },
                        )
                        interactionSource.ListenForInteractions { indicator ->
                            textIndicatorActions[index] = indicator.action
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
            assertEquals(true, textIndicatorActions.all { it == GestureAction.Primary })
        }
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
                    Text(
                        "Clickable$it",
                        modifier = Modifier.oneHandedGesture(action = GestureAction.Primary) {},
                    )
                }
            }
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
                        Text(
                            "Clickable $page",
                            modifier =
                                Modifier.oneHandedGesture(action = GestureAction.Primary) {
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
                                action = GestureAction.Primary,
                                priority =
                                    if (invertPriorities) GesturePriority.Clickable
                                    else GesturePriority.Unspecified,
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
                                action = GestureAction.Primary,
                                priority =
                                    if (invertPriorities) GesturePriority.Unspecified
                                    else GesturePriority.Clickable,
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

    @Test
    fun alert_dialog_edge_button() {
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
        rule.runOnIdle { assert(edgeButtonClicked) }
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
    fun alert_dialog_content_groups_edge_button(@TestParameter enabled: Boolean) {
        val sdkGestureInputManager = SdkGestureInputManagerMock(false)
        var edgeButtonClicked = false
        rule.setContentWithTheme {
            CompositionLocalProvider(LocalOneHandedGestureEnabled provides enabled) {
                MockSdkGestureInputManager(sdkGestureInputManager) {
                    val transformationSpec = rememberTransformationSpec()
                    AlertDialog(
                        visible = true,
                        onDismissRequest = {},
                        title = { Text("Title") },
                        transformationSpec = transformationSpec,
                        edgeButton = {
                            AlertDialogDefaults.EdgeButton(onClick = { edgeButtonClicked = true }) {
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
                                    Text(modifier = Modifier.fillMaxWidth(), text = "Never share")
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
                                    Text(modifier = Modifier.fillMaxWidth(), text = "Share always")
                                },
                                transformation = SurfaceTransformation(transformationSpec),
                            )
                        }
                    }
                }
            }
        }

        // Scroll through alert dialog with one-handed gestures until edge button is gestured
        for (i in 0..10) {
            sdkGestureInputManager.performGesture(sdkActionPrimary)
            rule.waitForIdle()
            if (edgeButtonClicked) {
                break
            }
        }
        assertEquals(enabled, edgeButtonClicked)
    }

    @Test
    fun test_slc_scroll_down(
        @TestParameter anchor: TestParamScalingLazyListAnchorType,
        @TestParameter wrap: Boolean,
    ) {
        val sdkGestureInputManager = SdkGestureInputManagerMock(false)
        val state = ScalingLazyListState()

        rule.setContentWithTheme {
            ScreenConfiguration(SCREEN_SIZE_SMALL) {
                MockSdkGestureInputManager(sdkGestureInputManager) {
                    ScalingLazyColumn(
                        state = state,
                        modifier =
                            Modifier.background(Color.Black)
                                .fillMaxSize()
                                .oneHandedGesture(
                                    action = GestureAction.Primary,
                                    onGesture = { OneHandedGestureDefaults.scrollDown(state, wrap) },
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
                assertEquals(expectedIndex[iteration % expectedIndex.size], state.centerItemIndex)
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
        val state = ScalingLazyListState()
        val numberOfItems = 10

        rule.setContentWithTheme {
            ScreenConfiguration(SCREEN_SIZE_SMALL) {
                MockSdkGestureInputManager(sdkGestureInputManager) {
                    ScalingLazyColumn(
                        state = state,
                        modifier =
                            Modifier.background(Color.Black)
                                .fillMaxSize()
                                .oneHandedGesture(
                                    action = GestureAction.Primary,
                                    onGesture = {
                                        OneHandedGestureDefaults.scrollDownToNextItem(state, wrap)
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
                assertEquals(expectedIndex, state.centerItemIndex)
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
        val state = TransformingLazyColumnState()

        rule.setContentWithTheme {
            ScreenConfiguration(SCREEN_SIZE_SMALL) {
                MockSdkGestureInputManager(sdkGestureInputManager) {
                    TransformingLazyColumn(
                        state = state,
                        modifier =
                            Modifier.background(Color.Black)
                                .fillMaxSize()
                                .oneHandedGesture(
                                    action = GestureAction.Primary,
                                    onGesture = { OneHandedGestureDefaults.scrollDown(state, wrap) },
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
                assertEquals(expectedIndex[iteration % expectedIndex.size], state.anchorItemIndex)
                sdkGestureInputManager.performGesture(sdkActionPrimary)
            }
        }
    }

    @Test
    fun test_tlc_scroll_next_item(@TestParameter wrap: Boolean) {
        val sdkGestureInputManager = SdkGestureInputManagerMock(false)
        val state = TransformingLazyColumnState()
        val numberOfItems = 15

        rule.setContentWithTheme {
            ScreenConfiguration(SCREEN_SIZE_SMALL) {
                MockSdkGestureInputManager(sdkGestureInputManager) {
                    TransformingLazyColumn(
                        state = state,
                        modifier =
                            Modifier.background(Color.Black)
                                .fillMaxSize()
                                .oneHandedGesture(
                                    action = GestureAction.Primary,
                                    onGesture = {
                                        OneHandedGestureDefaults.scrollDownToNextItem(state, wrap)
                                    },
                                ),
                    ) {
                        items(numberOfItems) { Text("Item $it") }
                    }
                }
            }
        }
        // On screen load, TLC items are not automatically aligned.
        // Trigger a primary gesture to snap the center item into place.
        sdkGestureInputManager.performGesture(sdkActionPrimary)

        var expectedIndex = 4
        repeat(numberOfItems * 2) {
            rule.waitForIdle()
            assertEquals(expectedIndex, state.anchorItemIndex)
            sdkGestureInputManager.performGesture(sdkActionPrimary)
            if (expectedIndex == numberOfItems - 5 /* last 4 items can't be scrolled */) {
                if (wrap) {
                    expectedIndex = 4

                    // TLC has 4 items remaining below the viewport center that cannot be scrolled
                    // into focus

                    // Step 1: Scroll a few pixels to ensure the list hits its physical end and the
                    // last item is fully visible
                    sdkGestureInputManager.performGesture(sdkActionPrimary)
                    rule.waitForIdle()

                    // Step 2: Scroll back up to the first (0th) item.
                    sdkGestureInputManager.performGesture(sdkActionPrimary)
                    rule.waitForIdle()

                    // Step 3: Trigger snapping behavior on the center item.
                    sdkGestureInputManager.performGesture(sdkActionPrimary)
                    rule.waitForIdle()
                }
            } else {
                expectedIndex++
            }
        }
    }

    @Test
    fun key_uniqueness() {
        val sdkGestureInputManager = SdkGestureInputManagerMock()
        val primaryInteractionSource = MutableInteractionSource()
        val dismissInteractionSource = MutableInteractionSource()

        var primaryKey: String? = null
        var dismissKey: String? = null

        rule.setContentWithTheme {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                Button(
                    onClick = {},
                    modifier =
                        Modifier.oneHandedGesture(
                            action = GestureAction.Primary,
                            interactionSource = primaryInteractionSource,
                            onGesture = {},
                        ),
                ) {
                    Text("Primary")
                }
                Button(
                    onClick = {},
                    modifier =
                        Modifier.oneHandedGesture(
                            action = GestureAction.Dismiss,
                            interactionSource = dismissInteractionSource,
                            onGesture = {},
                        ),
                ) {
                    Text("Dismiss")
                }
            }
            primaryInteractionSource.ListenForInteractions { interaction ->
                primaryKey = interaction.key
            }
            dismissInteractionSource.ListenForInteractions { interaction ->
                dismissKey = interaction.key
            }
        }

        // It takes at least a second for indicator to be shown. Fast-forward 3s to allow some delay
        rule.mainClock.advanceTimeBy(3000)
        rule.runOnIdle {
            assertNotNull(primaryKey)
            assertNotNull(dismissKey)
            assertNotEquals(primaryKey, dismissKey)
        }
    }

    fun local_composition_disable_enable_gesture() {
        var gestured = false
        val sdkGestureInputManager = SdkGestureInputManagerMock()
        var enabled by mutableStateOf(false)

        rule.setContentWithTheme {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                CompositionLocalProvider(LocalOneHandedGestureEnabled provides enabled) {
                    Text(
                        "Clickable",
                        modifier =
                            Modifier.oneHandedGesture(action = GestureAction.Primary) {
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
                    Text(
                        "Clickable",
                        modifier =
                            Modifier.oneHandedGesture(action = GestureAction.Primary) {
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
        val sdkGestureInputManager = SdkGestureInputManagerMock()
        val interactionSource = MutableInteractionSource()
        rule.verifyColors(
            interactionSource = interactionSource,
            gestureAction = GestureAction.Primary,
            expectedContentColor = tintColor,
        ) {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                OneHandedGestureIndicator(
                    interactionSource = interactionSource,
                    gestureIndicatorTint = tintColor,
                    modifier = Modifier.testTag(TEST_TAG),
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "",
                        modifier = Modifier.size(GestureIndicatorSize.Medium.size),
                    )
                }
            }
        }
    }

    @Test
    fun gesture_scroll_indicator_colors() {
        val tintColor = Color.Yellow
        val containerColor = Color.Blue
        val interactionSource = MutableInteractionSource()
        val sdkGestureInputManager = SdkGestureInputManagerMock()
        rule.verifyColors(
            interactionSource = interactionSource,
            gestureAction = GestureAction.Primary,
            expectedContentColor = tintColor,
            expectedContainerColor = containerColor,
        ) {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                Box(modifier = Modifier.testTag(TEST_TAG)) {
                    OneHandedGestureScrollIndicator(
                        interactionSource = interactionSource,
                        gestureIndicatorTint = tintColor,
                        gestureIndicatorBackgroundColor = containerColor,
                        state = rememberTransformingLazyColumnState(),
                    )
                    val interactionSource = remember { MutableInteractionSource() }
                    Box(modifier = Modifier.testTag(TEST_TAG)) {
                        OneHandedGestureScrollIndicator(
                            interactionSource = interactionSource,
                            gestureIndicatorTint = tintColor,
                            gestureIndicatorBackgroundColor = containerColor,
                            state = rememberTransformingLazyColumnState(),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun gesture_horizontal_page_indicator_colors() {
        val tintColor = Color.Yellow
        val containerColor = Color.Blue
        val interactionSource = MutableInteractionSource()
        val sdkGestureInputManager = SdkGestureInputManagerMock()
        rule.verifyColors(
            interactionSource = interactionSource,
            gestureAction = GestureAction.Primary,
            expectedContentColor = tintColor,
            expectedContainerColor = containerColor,
        ) {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                Box(modifier = Modifier.testTag(TEST_TAG)) {
                    OneHandedGestureHorizontalPageIndicator(
                        interactionSource = interactionSource,
                        gestureIndicatorTint = tintColor,
                        gestureIndicatorBackgroundColor = containerColor,
                        pagerState = rememberPagerState { 0 },
                    )
                }
            }
        }
    }

    @Test
    fun gesture_vertical_page_indicator_colors() {
        val tintColor = Color.Yellow
        val containerColor = Color.Blue
        val interactionSource = MutableInteractionSource()
        val sdkGestureInputManager = SdkGestureInputManagerMock()
        rule.verifyColors(
            interactionSource = interactionSource,
            gestureAction = GestureAction.Primary,
            expectedContentColor = tintColor,
            expectedContainerColor = containerColor,
        ) {
            MockSdkGestureInputManager(sdkGestureInputManager) {
                Box(modifier = Modifier.testTag(TEST_TAG)) {
                    OneHandedGestureVerticalPageIndicator(
                        interactionSource = interactionSource,
                        gestureIndicatorTint = tintColor,
                        gestureIndicatorBackgroundColor = containerColor,
                        pagerState = rememberPagerState { 0 },
                    )
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    internal fun ComposeContentTestRule.verifyColors(
        interactionSource: MutableInteractionSource,
        gestureAction: GestureAction,
        expectedContentColor: Color,
        expectedContainerColor: Color? = null,
        content: @Composable BoxScope.() -> Unit,
    ) {
        val testBackgroundColor = Color.White
        rule.mainClock.autoAdvance = false
        setContentWithTheme {
            Box(Modifier.fillMaxSize().background(testBackgroundColor), content = content)
        }
        interactionSource.tryEmit(OneHandedGestureInteraction.Indicate(gestureAction, "test"))
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
        val gestureManager = remember(scope) { GestureManagerImpl(scope, sdkGestureInputManager) }

        CompositionLocalProvider(
            LocalGestureManager provides gestureManager,
            LocalHapticFeedback provides haptic,
        ) {
            content()
        }
    }

    @Composable
    private fun InteractionSource.ListenForInteractions(
        onPressInteraction: (Offset) -> Unit = {},
        onGestureInteraction: (OneHandedGestureInteraction.Indicate) -> Unit,
    ) {
        LaunchedEffect(this) {
            interactions.collect { interaction ->
                if (interaction is OneHandedGestureInteraction.Indicate) {
                    onGestureInteraction(interaction)
                } else if (interaction is PressInteraction.Press) {
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

        override fun notifyGestureConsumed(key: String, sdkGestureAction: Int) {}

        override fun shouldShowIndicator(
            key: String,
            sdkGestureAction: Int,
            isOverlay: Boolean,
        ): Boolean = showIndicator

        override fun notifyIndicatorShown(key: String, sdkGestureAction: Int) {}

        fun performGesture(sdkGestureAction: Int) {
            gestureConsumers[sdkGestureAction]?.invoke(sdkGestureAction)
        }

        private val gestureConsumers = mutableMapOf<Int, (Int) -> Unit>()
    }

    /* Copy from com.google.wear.input.GestureEvent class */
    private val sdkActionDismiss = 2
    private val sdkActionPrimary = 1

    enum class TestParamScalingLazyListAnchorType(val type: ScalingLazyListAnchorType) {
        ItemStart(ScalingLazyListAnchorType.ItemStart),
        ItemCenter(ScalingLazyListAnchorType.ItemCenter),
    }
}
