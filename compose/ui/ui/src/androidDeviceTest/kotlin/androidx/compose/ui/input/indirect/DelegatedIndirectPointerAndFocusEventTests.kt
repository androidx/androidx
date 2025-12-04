/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.input.indirect

import android.os.SystemClock
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.ComposeUiFlags.isOptimizedFocusEventDispatchEnabled
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.indirect.util.focusableWithIndirectInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.NodeCoordinator
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.core.view.InputDeviceCompat.SOURCE_TOUCH_NAVIGATION
import androidx.test.core.view.MotionEventBuilder
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

/*
 * Verifies that indirect SOURCE_TOUCH_NAVIGATION [MotionEvent]s passing through the system
 * properly trigger Indirect Events (and their navigation movements/cancellations) in Compose when a
 * custom Modifier.Node delegates to BOTH [FocusTargetModifierNode] and
 * [IndirectPointerInputModifierNode] (see [FocusableAndIndirectPointerInputNode] as an example).
 *
 * For more detailed tests for general node types, see [IndirectPointerEventNavigationSystemTests].
 */
@RunWith(AndroidJUnit4::class)
class DelegatedIndirectPointerAndFocusEventTests {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    // Used to dispatch motion events
    private lateinit var rootView: AndroidComposeView
    private var receivedEvent: IndirectPointerEvent? = null

    private var indirectPointerCancelEventsThatShouldNotBeTriggered = false

    // Simple UI tests
    val testTagRootSimple = "testTagRootSimple"
    val testTagBox1 = "testTagBox1"
    val testTagBox2 = "testTagBox2"
    val testTagBox3 = "testTagBox3"

    // Complex UI tests (only needed ones)
    val testTagParent1Child1 = "testTagParent1Child1"

    val testTagParent2Child1 = "testTagParent2Child1"
    val testTagParent2Child2 = "testTagParent2Child2"

    val testTagParent3Child1 = "testTagParent3Child1"
    val testTagParent3Child2 = "testTagParent3Child2"

    // Other general setup and focus/fling enabling behavior variables
    val contentBoxSize = 100.dp
    val boxPadding = 10.dp

    lateinit var focusManager: FocusManager

    private val timeBetweenEvents = 20L
    private val flingTriggeringDistanceBetweenEvents = 50

    @Before
    fun setup() {
        indirectPointerCancelEventsThatShouldNotBeTriggered = false
        receivedEvent = null
    }

    // ----- Tests for indirect pointer cancellations -----
    @Test
    fun noNavigationMotionEvent_clearsFocus_triggersIndirectCancel() {
        var indirectPointerCancelForTopContainer = false
        var indirectPointerCancelForBox2 = false

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            Column(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.fillMaxSize()
                        .onIndirectPointerInput(
                            onEvent = {
                                indirectPointerEvent: IndirectPointerEvent,
                                pointerEventPass: PointerEventPass ->
                                if (pointerEventPass == PointerEventPass.Main) {
                                    receivedEvent = indirectPointerEvent
                                    // We don't consume the event, so it can pass on for system
                                    // navigation behavior.
                                }
                            },
                            onCancel = { indirectPointerCancelForTopContainer = true },
                        )
            ) {
                // Box 1
                Box(
                    modifier =
                        Modifier.size(contentBoxSize)
                            .background(Color.Red)
                            .padding(boxPadding)
                            .focusableWithIndirectInput(
                                onEvent = { _, _ -> },
                                onCancel = {
                                    indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                },
                                onFocusChange = { _, _ -> },
                            )
                )
                // Box 2
                Box(
                    modifier =
                        Modifier.testTag(testTagBox2)
                            .size(contentBoxSize)
                            .background(Color.Green)
                            .padding(boxPadding)
                            .focusableWithIndirectInput(
                                onEvent = { _, _ -> },
                                onCancel = { indirectPointerCancelForBox2 = true },
                                onFocusChange = { _, _ -> },
                            )
                )
                // Box 3
                Box(
                    modifier =
                        Modifier.size(contentBoxSize)
                            .background(Color.Blue)
                            .padding(boxPadding)
                            .focusableWithIndirectInput(
                                onEvent = { _, _ -> },
                                onCancel = {
                                    indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                },
                                onFocusChange = { _, _ -> },
                            )
                )
            }
        }

        // --- Test assertions and actions ---
        // Request initial focus for center box
        rule.onNodeWithTag(testTagBox2).requestFocus()

        // Manually clear focus
        rule.runOnIdle { focusManager.clearFocus(true) }

        rule.runOnIdle {
            assertThat(receivedEvent).isEqualTo(null)

            // Because a ui element with indirect pointer was focused, indirect pointer callbacks
            // WILL receive cancel events.
            @OptIn(ExperimentalComposeUiApi::class)
            if (isOptimizedFocusEventDispatchEnabled) {
                assertThat(indirectPointerCancelForTopContainer).isTrue()
            } else {
                assertThat(indirectPointerCancelForTopContainer).isFalse()
            }

            @OptIn(ExperimentalComposeUiApi::class)
            if (isOptimizedFocusEventDispatchEnabled) {
                assertThat(indirectPointerCancelForBox2).isTrue()
            } else {
                assertThat(indirectPointerCancelForBox2).isFalse()
            }
            assertThat(indirectPointerCancelEventsThatShouldNotBeTriggered).isFalse()
        }
    }

    @Test
    fun noNavigationMotionEvent_clearsFocusWithDeeperUiTree_triggersIndirectCancel() {
        var indirectPointerCancelForTopContainer = false
        var onIndirectPointerCancelForParent2 = false
        var onIndirectPointerCancelForParent2Child1 = false

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .onIndirectPointerInput(
                            onEvent = {
                                indirectPointerEvent: IndirectPointerEvent,
                                pointerEventPass: PointerEventPass ->
                                if (pointerEventPass == PointerEventPass.Main) {
                                    receivedEvent = indirectPointerEvent
                                    // We don't consume the event, so it can pass on for system
                                    // navigation behavior.
                                }
                            },
                            onCancel = { indirectPointerCancelForTopContainer = true },
                        )
            ) {
                // Parent UI Element 1
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .onIndirectPointerInput(
                                onEvent = { _, _ -> },
                                onCancel = {
                                    indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                },
                            )
                ) {
                    Box(
                        modifier =
                            @Suppress("DEPRECATION")
                            Modifier.size(contentBoxSize)
                                .background(Color.Red)
                                .padding(boxPadding)
                                .focusableWithIndirectInput(
                                    onEvent = { _, _ -> },
                                    onCancel = {
                                        indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                    },
                                    onFocusChange = { _, _ -> },
                                )
                    )
                    Box(
                        modifier =
                            @Suppress("DEPRECATION")
                            Modifier.size(contentBoxSize)
                                .background(Color.Magenta)
                                .padding(boxPadding)
                                .focusableWithIndirectInput(
                                    onEvent = { _, _ -> },
                                    onCancel = {
                                        indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                    },
                                    onFocusChange = { _, _ -> },
                                )
                    )
                }

                // Parent UI Element 2
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .onIndirectPointerInput(
                                onEvent = { _, _ -> },
                                onCancel = { onIndirectPointerCancelForParent2 = true },
                            )
                ) {
                    Box(
                        modifier =
                            @Suppress("DEPRECATION")
                            Modifier.testTag(testTagParent2Child1)
                                .size(contentBoxSize)
                                .background(Color.Green)
                                .padding(boxPadding)
                                .focusableWithIndirectInput(
                                    onEvent = { _, _ -> },
                                    onCancel = { onIndirectPointerCancelForParent2Child1 = true },
                                    onFocusChange = { _, _ -> },
                                )
                    )
                    Box(
                        modifier =
                            @Suppress("DEPRECATION")
                            Modifier.size(contentBoxSize)
                                .background(Color.Yellow)
                                .padding(boxPadding)
                                .focusableWithIndirectInput(
                                    onEvent = { _, _ -> },
                                    onCancel = {
                                        indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                    },
                                    onFocusChange = { _, _ -> },
                                )
                    )
                }

                // Parent UI Element 3
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .onIndirectPointerInput(
                                onEvent = { _, _ -> },
                                onCancel = {
                                    indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                },
                            )
                ) {
                    Box(
                        modifier =
                            @Suppress("DEPRECATION")
                            Modifier.size(contentBoxSize)
                                .background(Color.Blue)
                                .padding(boxPadding)
                                .focusableWithIndirectInput(
                                    onEvent = { _, _ -> },
                                    onCancel = {
                                        indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                    },
                                    onFocusChange = { _, _ -> },
                                )
                    )
                    Box(
                        modifier =
                            @Suppress("DEPRECATION")
                            Modifier.size(contentBoxSize)
                                .background(Color.Cyan)
                                .padding(boxPadding)
                                .focusableWithIndirectInput(
                                    onEvent = { _, _ -> },
                                    onCancel = {
                                        indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                    },
                                    onFocusChange = { _, _ -> },
                                )
                    )
                }
            }
        }

        // --- Test assertions and actions ---
        // Request initial focus for center box
        rule.onNodeWithTag(testTagParent2Child1).requestFocus()

        rule.runOnIdle {
            assertThat(receivedEvent).isEqualTo(null)
            // Since nothing was previously focused, the indirect pointer callbacks will NOT get
            // a cancel event.
            assertThat(indirectPointerCancelForTopContainer).isFalse()
            assertThat(onIndirectPointerCancelForParent2).isFalse()
            assertThat(onIndirectPointerCancelForParent2Child1).isFalse()
            assertThat(indirectPointerCancelEventsThatShouldNotBeTriggered).isFalse()
        }
        indirectPointerCancelForTopContainer = false
        onIndirectPointerCancelForParent2 = false
        onIndirectPointerCancelForParent2Child1 = false

        // Manually clear focus
        rule.runOnIdle { focusManager.clearFocus(true) }

        rule.runOnIdle {
            assertThat(receivedEvent).isEqualTo(null)
            // Any focus move (which triggers a focus change) will cause an indirect cancellation
            // event for any losing focus.
            @OptIn(ExperimentalComposeUiApi::class)
            if (isOptimizedFocusEventDispatchEnabled) {
                assertThat(indirectPointerCancelForTopContainer).isTrue()
                assertThat(onIndirectPointerCancelForParent2).isTrue()
                assertThat(onIndirectPointerCancelForParent2Child1).isTrue()
            } else {
                assertThat(indirectPointerCancelForTopContainer).isFalse()
                assertThat(onIndirectPointerCancelForParent2).isFalse()
                assertThat(onIndirectPointerCancelForParent2Child1).isFalse()
            }
            assertThat(indirectPointerCancelEventsThatShouldNotBeTriggered).isFalse()
        }
    }

    @Test
    fun noNavigationMotionEvent_moveFocusNextProgrammatically_triggersIndirectCancel() {
        var indirectPointerCancelForBox2 = false

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            Column(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.fillMaxSize()
                        .onIndirectPointerInput(
                            onEvent = {
                                indirectPointerEvent: IndirectPointerEvent,
                                pointerEventPass: PointerEventPass ->
                                if (pointerEventPass == PointerEventPass.Main) {
                                    receivedEvent = indirectPointerEvent
                                    // We don't consume the event, so it can pass on for system
                                    // navigation behavior.
                                }
                            },
                            onCancel = {
                                indirectPointerCancelEventsThatShouldNotBeTriggered = true
                            },
                        )
            ) {
                // Box 1
                Box(
                    modifier =
                        Modifier.size(contentBoxSize)
                            .background(Color.Red)
                            .padding(boxPadding)
                            .focusableWithIndirectInput(
                                onEvent = { _, _ -> },
                                onCancel = {
                                    indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                },
                                onFocusChange = { _, _ -> },
                            )
                )
                // Box 2
                Box(
                    modifier =
                        Modifier.testTag(testTagBox2)
                            .size(contentBoxSize)
                            .background(Color.Green)
                            .padding(boxPadding)
                            .focusableWithIndirectInput(
                                onEvent = { _, _ -> },
                                onCancel = { indirectPointerCancelForBox2 = true },
                                onFocusChange = { _, _ -> },
                            )
                )
                // Box 3
                Box(
                    modifier =
                        Modifier.size(contentBoxSize)
                            .background(Color.Blue)
                            .padding(boxPadding)
                            .focusableWithIndirectInput(
                                onEvent = { _, _ -> },
                                onCancel = {
                                    indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                },
                                onFocusChange = { _, _ -> },
                            )
                )
            }
        }

        // --- Test assertions and actions ---
        // Clear focus to start
        rule.runOnIdle { focusManager.clearFocus(true) }

        // Request initial focus for center box
        rule.onNodeWithTag(testTagBox2).requestFocus()

        // Manually move focus
        rule.runOnIdle { focusManager.moveFocus(FocusDirection.Next) }

        rule.runOnIdle {
            assertThat(receivedEvent).isEqualTo(null)
            // Any focus move (which triggers a focus change) will cause an indirect cancellation
            // event for any losing focus.
            @OptIn(ExperimentalComposeUiApi::class)
            if (isOptimizedFocusEventDispatchEnabled) {
                assertThat(indirectPointerCancelForBox2).isTrue()
            } else {
                assertThat(indirectPointerCancelForBox2).isFalse()
            }
            assertThat(indirectPointerCancelEventsThatShouldNotBeTriggered).isFalse()
        }
    }

    @Test
    fun noNavigationMotionEvent_moveFocusToFocusableParentProgrammatically_triggersIndirectCancelInChild() {
        var indirectPointerCancelForBox2 = false

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            Column(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.testTag(testTagRootSimple)
                        .fillMaxSize()
                        .onIndirectPointerInput(
                            onEvent = {
                                indirectPointerEvent: IndirectPointerEvent,
                                pointerEventPass: PointerEventPass ->
                                if (pointerEventPass == PointerEventPass.Main) {
                                    receivedEvent = indirectPointerEvent
                                    // We don't consume the event, so it can pass on for system
                                    // navigation behavior.
                                }
                            },
                            onCancel = {
                                indirectPointerCancelEventsThatShouldNotBeTriggered = true
                            },
                        )
                        .focusable()
            ) {
                // Box 1
                Box(
                    modifier =
                        Modifier.size(contentBoxSize)
                            .background(Color.Red)
                            .padding(boxPadding)
                            .focusableWithIndirectInput(
                                onEvent = { _, _ -> },
                                onCancel = {
                                    indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                },
                                onFocusChange = { _, _ -> },
                            )
                )
                // Box 2
                Box(
                    modifier =
                        Modifier.testTag(testTagBox2)
                            .size(contentBoxSize)
                            .background(Color.Green)
                            .padding(boxPadding)
                            .focusableWithIndirectInput(
                                onEvent = { _, _ -> },
                                onCancel = { indirectPointerCancelForBox2 = true },
                                onFocusChange = { _, _ -> },
                            )
                )
                // Box 3
                Box(
                    modifier =
                        Modifier.size(contentBoxSize)
                            .background(Color.Blue)
                            .padding(boxPadding)
                            .focusableWithIndirectInput(
                                onEvent = { _, _ -> },
                                onCancel = {
                                    indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                },
                                onFocusChange = { _, _ -> },
                            )
                )
            }
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager.clearFocus(true) }

        // Request initial focus for center box
        rule.onNodeWithTag(testTagBox2).requestFocus()
        rule.waitForIdle()

        // Manually move focus
        rule.onNodeWithTag(testTagRootSimple).requestFocus()

        rule.runOnIdle {
            assertThat(receivedEvent).isEqualTo(null)
            // Any focus move (which triggers a focus change) will cause an indirect cancellation
            // event for any losing focus.
            @OptIn(ExperimentalComposeUiApi::class)
            if (isOptimizedFocusEventDispatchEnabled) {
                assertThat(indirectPointerCancelForBox2).isTrue()
            } else {
                assertThat(indirectPointerCancelForBox2).isFalse()
            }
            assertThat(indirectPointerCancelEventsThatShouldNotBeTriggered).isFalse()
        }
    }

    @Test
    fun noNavigationMotionEvent_moveFocusProgrammaticallyWrapAround_triggersIndirectCancel() {
        var indirectPointerCancelForTopContainer = false
        var indirectPointerCancelForBox1 = false

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            Column(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.fillMaxSize()
                        .onIndirectPointerInput(
                            onEvent = {
                                indirectPointerEvent: IndirectPointerEvent,
                                pointerEventPass: PointerEventPass ->
                                if (pointerEventPass == PointerEventPass.Main) {
                                    receivedEvent = indirectPointerEvent
                                    // We don't consume the event, so it can pass on for system
                                    // navigation behavior.
                                }
                            },
                            onCancel = { indirectPointerCancelForTopContainer = true },
                        )
            ) {
                // Box 1
                Box(
                    modifier =
                        Modifier.testTag(testTagBox1)
                            .size(contentBoxSize)
                            .background(Color.Red)
                            .padding(boxPadding)
                            .focusableWithIndirectInput(
                                onEvent = { _, _ -> },
                                onCancel = { indirectPointerCancelForBox1 = true },
                                onFocusChange = { _, _ -> },
                            )
                )
                // Box 2
                Box(
                    modifier =
                        Modifier.size(contentBoxSize)
                            .background(Color.Green)
                            .padding(boxPadding)
                            .focusableWithIndirectInput(
                                onEvent = { _, _ -> },
                                onCancel = {
                                    indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                },
                                onFocusChange = { _, _ -> },
                            )
                )
                // Box 3
                Box(
                    modifier =
                        Modifier.size(contentBoxSize)
                            .background(Color.Blue)
                            .padding(boxPadding)
                            .focusableWithIndirectInput(
                                onEvent = { _, _ -> },
                                onCancel = {
                                    indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                },
                                onFocusChange = { _, _ -> },
                            )
                )
            }
        }

        // --- Test assertions and actions ---
        // Clear focus to start
        rule.runOnIdle { focusManager.clearFocus(true) }

        // Request initial focus for center box
        rule.onNodeWithTag(testTagBox1).requestFocus()

        // Manually move focus
        rule.runOnIdle { focusManager.moveFocus(FocusDirection.Previous) }

        rule.runOnIdle {
            assertThat(receivedEvent).isEqualTo(null)
            // Any focus move (which triggers a focus change) will cause an indirect cancellation
            // event for any losing focus.
            // For situations where the focus wraps around (start of the list going to end of the
            // list or vice versa), an indirect cancel will be sent to existing focused indirect
            // nodes.
            @OptIn(ExperimentalComposeUiApi::class)
            if (isOptimizedFocusEventDispatchEnabled) {
                assertThat(indirectPointerCancelForTopContainer).isTrue()
                assertThat(indirectPointerCancelForBox1).isTrue()
            } else {
                assertThat(indirectPointerCancelForTopContainer).isFalse()
                assertThat(indirectPointerCancelForBox1).isFalse()
            }
            assertThat(indirectPointerCancelEventsThatShouldNotBeTriggered).isFalse()
        }
    }

    @Test
    fun noNavigationMotionEvent_moveFocusProgrammaticallyWithDeeperUiTree_triggersIndirectCancel() {
        var onIndirectPointerCancelForParent2Child1 = false
        var onIndirectPointerCancelForParent2 = false
        var onIndirectPointerCancelForParent2Child2 = false
        var onIndirectPointerCancelForParent3 = false
        var onIndirectPointerCancelForParent3Child1 = false

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .onIndirectPointerInput(
                            onEvent = {
                                indirectPointerEvent: IndirectPointerEvent,
                                pointerEventPass: PointerEventPass ->
                                if (pointerEventPass == PointerEventPass.Main) {
                                    receivedEvent = indirectPointerEvent
                                    // We don't consume the event, so it can pass on for system
                                    // navigation behavior.
                                }
                            },
                            onCancel = {
                                indirectPointerCancelEventsThatShouldNotBeTriggered = true
                            },
                        )
            ) {
                // Parent UI Element 1
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .onIndirectPointerInput(
                                onEvent = { _, _ -> },
                                onCancel = {
                                    indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                },
                            )
                ) {
                    Box(
                        modifier =
                            @Suppress("DEPRECATION")
                            Modifier.size(contentBoxSize)
                                .background(Color.Red)
                                .padding(boxPadding)
                                .focusableWithIndirectInput(
                                    onEvent = { _, _ -> },
                                    onCancel = {
                                        indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                    },
                                    onFocusChange = { _, _ -> },
                                )
                    )
                    Box(
                        modifier =
                            @Suppress("DEPRECATION")
                            Modifier.size(contentBoxSize)
                                .background(Color.Magenta)
                                .padding(boxPadding)
                                .focusableWithIndirectInput(
                                    onEvent = { _, _ -> },
                                    onCancel = {
                                        indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                    },
                                    onFocusChange = { _, _ -> },
                                )
                    )
                }

                // Parent UI Element 2
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .onIndirectPointerInput(
                                onEvent = { _, _ -> },
                                onCancel = { onIndirectPointerCancelForParent2 = true },
                            )
                ) {
                    Box(
                        modifier =
                            @Suppress("DEPRECATION")
                            Modifier.testTag(testTagParent2Child1)
                                .size(contentBoxSize)
                                .background(Color.Green)
                                .padding(boxPadding)
                                .focusableWithIndirectInput(
                                    onEvent = { _, _ -> },
                                    onCancel = { onIndirectPointerCancelForParent2Child1 = true },
                                    onFocusChange = { _, _ -> },
                                )
                    )
                    Box(
                        modifier =
                            @Suppress("DEPRECATION")
                            Modifier.testTag(testTagParent2Child2)
                                .size(contentBoxSize)
                                .background(Color.Yellow)
                                .padding(boxPadding)
                                .focusableWithIndirectInput(
                                    onEvent = { _, _ -> },
                                    onCancel = { onIndirectPointerCancelForParent2Child2 = true },
                                    onFocusChange = { _, _ -> },
                                )
                    )
                }

                // Parent UI Element 3
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .onIndirectPointerInput(
                                onEvent = { _, _ -> },
                                onCancel = { onIndirectPointerCancelForParent3 = true },
                            )
                ) {
                    Box(
                        modifier =
                            @Suppress("DEPRECATION")
                            Modifier.testTag(testTagParent3Child1)
                                .size(contentBoxSize)
                                .background(Color.Blue)
                                .padding(boxPadding)
                                .focusableWithIndirectInput(
                                    onEvent = { _, _ -> },
                                    onCancel = { onIndirectPointerCancelForParent3Child1 = true },
                                    onFocusChange = { _, _ -> },
                                )
                    )
                    Box(
                        modifier =
                            @Suppress("DEPRECATION")
                            Modifier.size(contentBoxSize)
                                .background(Color.Cyan)
                                .padding(boxPadding)
                                .focusableWithIndirectInput(
                                    onEvent = { _, _ -> },
                                    onCancel = {
                                        indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                    },
                                    onFocusChange = { _, _ -> },
                                )
                    )
                }
            }
        }

        // --- Test assertions and actions ---
        // Clear focus to start
        rule.runOnIdle { focusManager.clearFocus(true) }

        // Request initial focus for center box
        rule.onNodeWithTag(testTagParent2Child1).requestFocus()

        // Manually move focus
        rule.runOnIdle { focusManager.moveFocus(FocusDirection.Next) }

        rule.runOnIdle {
            assertThat(receivedEvent).isEqualTo(null)
            // Any focus move (which triggers a focus change) will cause an indirect cancellation
            // event for any losing focus.
            assertThat(onIndirectPointerCancelForParent2).isFalse()
            @OptIn(ExperimentalComposeUiApi::class)
            if (isOptimizedFocusEventDispatchEnabled) {
                assertThat(onIndirectPointerCancelForParent2Child1).isTrue()
            } else {
                assertThat(onIndirectPointerCancelForParent2Child1).isFalse()
            }
            assertThat(onIndirectPointerCancelForParent2Child2).isFalse()
            assertThat(onIndirectPointerCancelForParent3).isFalse()
            assertThat(onIndirectPointerCancelForParent3Child1).isFalse()
            assertThat(indirectPointerCancelEventsThatShouldNotBeTriggered).isFalse()
        }
        onIndirectPointerCancelForParent2Child1 = false
        onIndirectPointerCancelForParent2 = false
        onIndirectPointerCancelForParent2Child2 = false
        onIndirectPointerCancelForParent3 = false
        onIndirectPointerCancelForParent3Child1 = false

        // Manually move focus
        rule.runOnIdle { focusManager.moveFocus(FocusDirection.Next) }

        rule.runOnIdle {
            assertThat(receivedEvent).isEqualTo(null)
            // Any focus move (which triggers a focus change) will cause an indirect cancellation
            // event for any losing focus.
            @OptIn(ExperimentalComposeUiApi::class)
            if (isOptimizedFocusEventDispatchEnabled) {
                assertThat(onIndirectPointerCancelForParent2).isTrue()
                assertThat(onIndirectPointerCancelForParent2Child2).isTrue()
            } else {
                assertThat(onIndirectPointerCancelForParent2).isFalse()
                assertThat(onIndirectPointerCancelForParent2Child2).isFalse()
            }
            assertThat(onIndirectPointerCancelForParent2Child1).isFalse()
            assertThat(onIndirectPointerCancelForParent3).isFalse()
            assertThat(onIndirectPointerCancelForParent3Child1).isFalse()
            assertThat(indirectPointerCancelEventsThatShouldNotBeTriggered).isFalse()
        }
        onIndirectPointerCancelForParent2Child1 = false
        onIndirectPointerCancelForParent2 = false
        onIndirectPointerCancelForParent2Child2 = false
        onIndirectPointerCancelForParent3 = false
        onIndirectPointerCancelForParent3Child1 = false

        // Manually move focus
        rule.runOnIdle { focusManager.moveFocus(FocusDirection.Previous) }

        rule.runOnIdle {
            assertThat(receivedEvent).isEqualTo(null)
            // Any focus move (which triggers a focus change) will cause an indirect cancellation
            // event for any losing focus.
            assertThat(onIndirectPointerCancelForParent2).isFalse()
            assertThat(onIndirectPointerCancelForParent2Child1).isFalse()
            assertThat(onIndirectPointerCancelForParent2Child2).isFalse()
            @OptIn(ExperimentalComposeUiApi::class)
            if (isOptimizedFocusEventDispatchEnabled) {
                assertThat(onIndirectPointerCancelForParent3).isTrue()
                assertThat(onIndirectPointerCancelForParent3Child1).isTrue()
            } else {
                assertThat(onIndirectPointerCancelForParent3).isFalse()
                assertThat(onIndirectPointerCancelForParent3Child1).isFalse()
            }
            assertThat(indirectPointerCancelEventsThatShouldNotBeTriggered).isFalse()
        }
    }

    @Test
    fun noNavigationMotionEvent_moveFocusProgrammaticallyWrapAroundWithDeeperUiTree_triggersIndirectCancel() {
        var indirectPointerCancelForTopContainer = false
        var onIndirectPointerCancelForParent1 = false
        var onIndirectPointerCancelForParent1Child1 = false
        var onIndirectPointerCancelForParent3 = false
        var onIndirectPointerCancelForParent3Child2 = false

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .onIndirectPointerInput(
                            onEvent = {
                                indirectPointerEvent: IndirectPointerEvent,
                                pointerEventPass: PointerEventPass ->
                                if (pointerEventPass == PointerEventPass.Main) {
                                    receivedEvent = indirectPointerEvent
                                    // We don't consume the event, so it can pass on for system
                                    // navigation behavior.
                                }
                            },
                            onCancel = { indirectPointerCancelForTopContainer = true },
                        )
            ) {
                // Parent UI Element 1
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .onIndirectPointerInput(
                                onEvent = { _, _ -> },
                                onCancel = { onIndirectPointerCancelForParent1 = true },
                            )
                ) {
                    Box(
                        modifier =
                            @Suppress("DEPRECATION")
                            Modifier.testTag(testTagParent1Child1)
                                .size(contentBoxSize)
                                .background(Color.Red)
                                .padding(boxPadding)
                                .focusableWithIndirectInput(
                                    onEvent = { _, _ -> },
                                    onCancel = { onIndirectPointerCancelForParent1Child1 = true },
                                    onFocusChange = { _, _ -> },
                                )
                    )
                    Box(
                        modifier =
                            @Suppress("DEPRECATION")
                            Modifier.size(contentBoxSize)
                                .background(Color.Magenta)
                                .padding(boxPadding)
                                .focusableWithIndirectInput(
                                    onEvent = { _, _ -> },
                                    onCancel = {
                                        indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                    },
                                    onFocusChange = { _, _ -> },
                                )
                    )
                }

                // Parent UI Element 2
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .onIndirectPointerInput(
                                onEvent = { _, _ -> },
                                onCancel = {
                                    indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                },
                            )
                ) {
                    Box(
                        modifier =
                            @Suppress("DEPRECATION")
                            Modifier.size(contentBoxSize)
                                .background(Color.Green)
                                .padding(boxPadding)
                                .focusableWithIndirectInput(
                                    onEvent = { _, _ -> },
                                    onCancel = {
                                        indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                    },
                                    onFocusChange = { _, _ -> },
                                )
                    )
                    Box(
                        modifier =
                            @Suppress("DEPRECATION")
                            Modifier.size(contentBoxSize)
                                .background(Color.Yellow)
                                .padding(boxPadding)
                                .focusableWithIndirectInput(
                                    onEvent = { _, _ -> },
                                    onCancel = {
                                        indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                    },
                                    onFocusChange = { _, _ -> },
                                )
                    )
                }

                // Parent UI Element 3
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .onIndirectPointerInput(
                                onEvent = { _, _ -> },
                                onCancel = { onIndirectPointerCancelForParent3 = true },
                            )
                ) {
                    Box(
                        modifier =
                            @Suppress("DEPRECATION")
                            Modifier.size(contentBoxSize)
                                .background(Color.Blue)
                                .padding(boxPadding)
                                .focusableWithIndirectInput(
                                    onEvent = { _, _ -> },
                                    onCancel = {
                                        indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                    },
                                    onFocusChange = { _, _ -> },
                                )
                    )
                    Box(
                        modifier =
                            @Suppress("DEPRECATION")
                            Modifier.testTag(testTagParent3Child2)
                                .size(contentBoxSize)
                                .background(Color.Cyan)
                                .padding(boxPadding)
                                .focusableWithIndirectInput(
                                    onEvent = { _, _ -> },
                                    onCancel = { onIndirectPointerCancelForParent3Child2 = true },
                                    onFocusChange = { _, _ -> },
                                )
                    )
                }
            }
        }

        // --- Test assertions and actions ---
        // Clear focus to start
        rule.runOnIdle { focusManager.clearFocus(true) }

        // Request initial focus for center box
        rule.onNodeWithTag(testTagParent1Child1).requestFocus()

        // Manually move focus
        rule.runOnIdle { focusManager.moveFocus(FocusDirection.Previous) }

        rule.runOnIdle {
            assertThat(receivedEvent).isEqualTo(null)
            // Any focus move (which triggers a focus change) will cause an indirect cancellation
            // event for any losing focus.
            // For situations where the focus wraps around (start of the list going to end of the
            // list or vice versa), an indirect cancel will be sent to existing focused indirect
            // nodes.
            @OptIn(ExperimentalComposeUiApi::class)
            if (isOptimizedFocusEventDispatchEnabled) {
                assertThat(indirectPointerCancelForTopContainer).isTrue()
                assertThat(onIndirectPointerCancelForParent1).isTrue()
                assertThat(onIndirectPointerCancelForParent1Child1).isTrue()
            } else {
                assertThat(indirectPointerCancelForTopContainer).isFalse()
                assertThat(onIndirectPointerCancelForParent1).isFalse()
                assertThat(onIndirectPointerCancelForParent1Child1).isFalse()
            }
            assertThat(onIndirectPointerCancelForParent3).isFalse()
            assertThat(onIndirectPointerCancelForParent3Child2).isFalse()
            assertThat(indirectPointerCancelEventsThatShouldNotBeTriggered).isFalse()
        }
        indirectPointerCancelForTopContainer = false
        onIndirectPointerCancelForParent1 = false
        onIndirectPointerCancelForParent1Child1 = false
        onIndirectPointerCancelForParent3 = false
        onIndirectPointerCancelForParent3Child2 = false

        // Manually move focus
        rule.runOnIdle { focusManager.moveFocus(FocusDirection.Next) }

        rule.runOnIdle {
            assertThat(receivedEvent).isEqualTo(null)
            // Any focus move (which triggers a focus change) will cause an indirect cancellation
            // event for any losing focus.
            @OptIn(ExperimentalComposeUiApi::class)
            if (isOptimizedFocusEventDispatchEnabled) {
                assertThat(indirectPointerCancelForTopContainer).isTrue()
                assertThat(onIndirectPointerCancelForParent3).isTrue()
                assertThat(onIndirectPointerCancelForParent3Child2).isTrue()
            } else {
                assertThat(indirectPointerCancelForTopContainer).isFalse()
                assertThat(onIndirectPointerCancelForParent3).isFalse()
                assertThat(onIndirectPointerCancelForParent3Child2).isFalse()
            }
            assertThat(onIndirectPointerCancelForParent1).isFalse()
            assertThat(onIndirectPointerCancelForParent1Child1).isFalse()
            assertThat(indirectPointerCancelEventsThatShouldNotBeTriggered).isFalse()
        }
    }

    @Test
    fun downViaNavigationMotionEvent_moveFocusProgrammatically_triggersIndirectCancel() {
        var indirectPointerCancelForBox2 = false

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            Column(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.fillMaxSize()
                        .onIndirectPointerInput(
                            onEvent = {
                                indirectPointerEvent: IndirectPointerEvent,
                                pointerEventPass: PointerEventPass ->
                                if (pointerEventPass == PointerEventPass.Main) {
                                    receivedEvent = indirectPointerEvent
                                    // We don't consume the event, so it can pass on for system
                                    // navigation behavior.
                                }
                            },
                            onCancel = {
                                indirectPointerCancelEventsThatShouldNotBeTriggered = true
                            },
                        )
            ) {
                // Box 1
                Box(
                    modifier =
                        Modifier.size(contentBoxSize)
                            .background(Color.Red)
                            .padding(boxPadding)
                            .focusableWithIndirectInput(
                                onEvent = { _, _ -> },
                                onCancel = {
                                    indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                },
                                onFocusChange = { _, _ -> },
                            )
                )
                // Box 2
                Box(
                    modifier =
                        Modifier.testTag(testTagBox2)
                            .size(contentBoxSize)
                            .background(Color.Green)
                            .padding(boxPadding)
                            .focusableWithIndirectInput(
                                onEvent = { _, _ -> },
                                onCancel = { indirectPointerCancelForBox2 = true },
                                onFocusChange = { _, _ -> },
                            )
                )
                // Box 3
                Box(
                    modifier =
                        Modifier.size(contentBoxSize)
                            .background(Color.Blue)
                            .padding(boxPadding)
                            .focusableWithIndirectInput(
                                onEvent = { _, _ -> },
                                onCancel = {
                                    indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                },
                                onFocusChange = { _, _ -> },
                            )
                )
            }
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager.clearFocus(true) }

        // Request initial focus for center box
        rule.onNodeWithTag(testTagBox2).requestFocus()

        val downTime = SystemClock.uptimeMillis()
        val indirectX = 100f
        val indirectY = 100f

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(downTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectPointerEventPrimaryDirectionalMotionAxis.X
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle {
            assertThat(receivedEvent?.type).isEqualTo(IndirectPointerEventType.Press)
            assertThat(indirectPointerCancelForBox2).isFalse()
            assertThat(indirectPointerCancelEventsThatShouldNotBeTriggered).isFalse()
        }

        receivedEvent = null

        // Manually move focus
        rule.runOnIdle { focusManager.moveFocus(FocusDirection.Next) }

        rule.runOnIdle {
            assertThat(receivedEvent).isEqualTo(null)
            @OptIn(ExperimentalComposeUiApi::class)
            if (isOptimizedFocusEventDispatchEnabled) {
                assertThat(indirectPointerCancelForBox2).isTrue()
            } else {
                assertThat(indirectPointerCancelForBox2).isFalse()
            }
            assertThat(indirectPointerCancelEventsThatShouldNotBeTriggered).isFalse()
        }
    }

    @Test
    fun downAndMovesViaNavigationMotionEvent_moveFocusProgrammatically_triggersIndirectCancel() {
        var indirectPointerCancelForBox2 = false

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            Column(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.fillMaxSize()
                        .onIndirectPointerInput(
                            onEvent = {
                                indirectPointerEvent: IndirectPointerEvent,
                                pointerEventPass: PointerEventPass ->
                                if (pointerEventPass == PointerEventPass.Main) {
                                    receivedEvent = indirectPointerEvent
                                    // We don't consume the event, so it can pass on for system
                                    // navigation behavior.
                                }
                            },
                            onCancel = {
                                indirectPointerCancelEventsThatShouldNotBeTriggered = true
                            },
                        )
            ) {
                // Box 1
                Box(
                    modifier =
                        Modifier.size(contentBoxSize)
                            .background(Color.Red)
                            .padding(boxPadding)
                            .focusableWithIndirectInput(
                                onEvent = { _, _ -> },
                                onCancel = {
                                    indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                },
                                onFocusChange = { _, _ -> },
                            )
                )
                // Box 2
                Box(
                    modifier =
                        Modifier.testTag(testTagBox2)
                            .size(contentBoxSize)
                            .background(Color.Green)
                            .padding(boxPadding)
                            .focusableWithIndirectInput(
                                onEvent = { _, _ -> },
                                onCancel = { indirectPointerCancelForBox2 = true },
                                onFocusChange = { _, _ -> },
                            )
                )
                // Box 3
                Box(
                    modifier =
                        Modifier.size(contentBoxSize)
                            .background(Color.Blue)
                            .padding(boxPadding)
                            .focusableWithIndirectInput(
                                onEvent = { _, _ -> },
                                onCancel = {
                                    indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                },
                                onFocusChange = { _, _ -> },
                            )
                )
            }
        }

        // --- Test assertions and actions ---
        // Clear focus to start
        rule.runOnIdle { focusManager.clearFocus(true) }

        // Request initial focus for center box
        rule.onNodeWithTag(testTagBox2).requestFocus()

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        val previousTime = eventTime
        var indirectX = 100f
        val previousIndirectX = indirectX
        val indirectY = 100f
        val previousIndirectY = indirectY

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectPointerEventPrimaryDirectionalMotionAxis.X
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle {
            assertThat(receivedEvent?.type).isEqualTo(IndirectPointerEventType.Press)
            val firstChange = checkNotNull(receivedEvent?.changes?.first())
            assertThat(firstChange.position.x).isEqualTo(indirectX)
            assertThat(firstChange.position.y).isEqualTo(indirectY)
            assertThat(firstChange.isConsumed).isEqualTo(false)
            assertThat(firstChange.pressed).isEqualTo(true)
            // For a first event in a stream, previous is going to equal current (since there is
            // no previous).
            assertThat(firstChange.previousPosition.x).isEqualTo(previousIndirectX)
            assertThat(firstChange.previousPosition.y).isEqualTo(previousIndirectY)
            assertThat(firstChange.previousUptimeMillis).isEqualTo(previousTime)
            assertThat(firstChange.previousPressed).isEqualTo(false)
            assertThat(indirectPointerCancelForBox2).isFalse()
            assertThat(indirectPointerCancelEventsThatShouldNotBeTriggered).isFalse()
        }

        eventTime += timeBetweenEvents
        indirectX += flingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle {
            assertThat(receivedEvent?.type).isEqualTo(IndirectPointerEventType.Move)
            val firstChange = checkNotNull(receivedEvent?.changes?.first())
            assertThat(firstChange.position.x).isEqualTo(indirectX)
            assertThat(firstChange.position.y).isEqualTo(indirectY)
            assertThat(firstChange.isConsumed).isEqualTo(false)
            assertThat(firstChange.pressed).isEqualTo(true)
            assertThat(firstChange.previousPosition.x).isEqualTo(previousIndirectX)
            assertThat(firstChange.previousPosition.y).isEqualTo(previousIndirectY)
            assertThat(firstChange.previousUptimeMillis).isEqualTo(previousTime)
            assertThat(firstChange.previousPressed).isEqualTo(true)
            assertThat(indirectPointerCancelForBox2).isFalse()
            assertThat(indirectPointerCancelEventsThatShouldNotBeTriggered).isFalse()
        }

        receivedEvent = null

        // Manually move focus
        rule.runOnIdle { focusManager.moveFocus(FocusDirection.Next) }

        rule.runOnIdle {
            assertThat(receivedEvent).isEqualTo(null)
            @OptIn(ExperimentalComposeUiApi::class)
            if (isOptimizedFocusEventDispatchEnabled) {
                assertThat(indirectPointerCancelForBox2).isTrue()
            } else {
                assertThat(indirectPointerCancelForBox2).isFalse()
            }
            assertThat(indirectPointerCancelEventsThatShouldNotBeTriggered).isFalse()
        }
    }

    // Indirect cancel will trigger after an up (considered the end of the event stream). We test
    // that after a full swipe (down, move, move, up).
    @Test
    fun swipeViaNavigationMotionEvent_moveFocusProgrammatically_triggersIndirectCancel() {
        var indirectPointerCancelForBox2 = false
        var indirectPointerCancelForBox3 = false

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            Column(
                modifier =
                    @Suppress("DEPRECATION")
                    Modifier.fillMaxSize()
                        .onIndirectPointerInput(
                            onEvent = {
                                indirectPointerEvent: IndirectPointerEvent,
                                pointerEventPass: PointerEventPass ->
                                if (pointerEventPass == PointerEventPass.Main) {
                                    receivedEvent = indirectPointerEvent
                                    // We don't consume the event, so it can pass on for system
                                    // navigation behavior.
                                }
                            },
                            onCancel = {
                                indirectPointerCancelEventsThatShouldNotBeTriggered = true
                            },
                        )
            ) {
                // Box 1
                Box(
                    modifier =
                        Modifier.size(contentBoxSize)
                            .background(Color.Red)
                            .padding(boxPadding)
                            .focusableWithIndirectInput(
                                onEvent = { _, _ -> },
                                onCancel = {
                                    indirectPointerCancelEventsThatShouldNotBeTriggered = true
                                },
                                onFocusChange = { _, _ -> },
                            )
                )
                // Box 2
                Box(
                    modifier =
                        Modifier.testTag(testTagBox2)
                            .size(contentBoxSize)
                            .background(Color.Green)
                            .padding(boxPadding)
                            .focusableWithIndirectInput(
                                onEvent = { _, _ -> },
                                onCancel = { indirectPointerCancelForBox2 = true },
                                onFocusChange = { _, _ -> },
                            )
                )
                // Box 3
                Box(
                    modifier =
                        Modifier.testTag(testTagBox3)
                            .size(contentBoxSize)
                            .background(Color.Blue)
                            .padding(boxPadding)
                            .focusableWithIndirectInput(
                                onEvent = { _, _ -> },
                                onCancel = { indirectPointerCancelForBox3 = true },
                                onFocusChange = { _, _ -> },
                            )
                )
            }
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager.clearFocus(true) }

        // Request initial focus for center box
        rule.onNodeWithTag(testTagBox2).requestFocus()

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        var indirectX = 100f
        val indirectY = 100f

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectPointerEventPrimaryDirectionalMotionAxis.X
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle {
            assertThat(receivedEvent?.type).isEqualTo(IndirectPointerEventType.Press)
            assertThat(indirectPointerCancelForBox2).isFalse()
            assertThat(indirectPointerCancelForBox3).isFalse()
            assertThat(indirectPointerCancelEventsThatShouldNotBeTriggered).isFalse()
        }
        eventTime += timeBetweenEvents
        indirectX += flingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle {
            assertThat(receivedEvent?.type).isEqualTo(IndirectPointerEventType.Move)
            assertThat(indirectPointerCancelForBox2).isFalse()
            assertThat(indirectPointerCancelForBox3).isFalse()
            assertThat(indirectPointerCancelEventsThatShouldNotBeTriggered).isFalse()
        }

        eventTime += timeBetweenEvents
        indirectX += flingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle {
            assertThat(receivedEvent?.type).isEqualTo(IndirectPointerEventType.Move)
            assertThat(indirectPointerCancelForBox2).isFalse()
            assertThat(indirectPointerCancelForBox3).isFalse()
            assertThat(indirectPointerCancelEventsThatShouldNotBeTriggered).isFalse()
        }

        eventTime += timeBetweenEvents
        indirectX += flingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle {
            assertThat(receivedEvent?.type).isEqualTo(IndirectPointerEventType.Release)
            // Any focus move (which triggers a focus change) will cause an indirect cancellation
            // event for any losing focus.
            @OptIn(ExperimentalComposeUiApi::class)
            if (isOptimizedFocusEventDispatchEnabled) {
                assertThat(indirectPointerCancelForBox2).isTrue()
            } else {
                assertThat(indirectPointerCancelForBox2).isFalse()
            }
            assertThat(indirectPointerCancelForBox3).isFalse()
            assertThat(indirectPointerCancelEventsThatShouldNotBeTriggered).isFalse()
        }

        receivedEvent = null
        indirectPointerCancelForBox2 = false
        indirectPointerCancelForBox3 = false

        // Manually move focus
        rule.runOnIdle { focusManager.moveFocus(FocusDirection.Previous) }

        rule.runOnIdle {
            assertThat(receivedEvent).isEqualTo(null)
            // Any focus move (which triggers a focus change) will cause an indirect cancellation
            // event for any losing focus.
            assertThat(indirectPointerCancelForBox2).isFalse()
            @OptIn(ExperimentalComposeUiApi::class)
            if (isOptimizedFocusEventDispatchEnabled) {
                assertThat(indirectPointerCancelForBox3).isTrue()
            } else {
                assertThat(indirectPointerCancelForBox3).isFalse()
            }
            assertThat(indirectPointerCancelEventsThatShouldNotBeTriggered).isFalse()
        }
    }

    /**
     * Retrieves all Modifier.Node instances associated with a given LayoutCoordinates.
     *
     * This is an internal-only function and relies on the internal Compose UI implementation
     * details. It should not be used in application code.
     *
     * @param coordinates The LayoutCoordinates to inspect.
     * @return A list of all Modifier.Node instances for the given coordinates, starting from the
     *   head of the modifier chain.
     */
    internal fun getAllModifierNodes(coordinates: LayoutCoordinates): List<Modifier.Node> {
        // LayoutCoordinates is an interface. The internal implementation is NodeCoordinator.
        val coordinator = coordinates as? NodeCoordinator ?: return emptyList()

        // The NodeCoordinator has a reference to its LayoutNode.
        val layoutNode = coordinator.layoutNode

        // The LayoutNode's `nodes` property is the NodeChain, which contains the head
        // of the Modifier.Node linked list.
        var currentNode: Modifier.Node? = layoutNode.nodes.head

        val nodes = mutableListOf<Modifier.Node>()
        while (currentNode != null) {
            nodes.add(currentNode)
            // Traverse to the next node in the chain.
            currentNode = currentNode.child
        }
        return nodes
    }
}
