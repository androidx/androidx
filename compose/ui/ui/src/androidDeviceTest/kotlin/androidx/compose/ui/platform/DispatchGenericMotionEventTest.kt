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

package androidx.compose.ui.platform

import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import android.view.InputDevice
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for dispatching generic motion events (specifically, scroll) directly to the
 * AndroidComposeView to verify behavior.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalComposeUiApi::class)
class DispatchGenericMotionEventDirectlyToAndroidComposeViewTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun dispatchGenericMotionEvent_sourceUnknownScroll_whenFlagEnabled_movesFocus() {
        val originalFlagValue = ComposeUiFlags.isHardwareNavigationHandlingEnabled
        ComposeUiFlags.isHardwareNavigationHandlingEnabled = true
        val focusRequester1 = FocusRequester()
        var isFocused1 = false
        var isFocused2 = false
        try {
            val view =
                setTwoFocusableBoxesContent(
                    focusRequester1 = focusRequester1,
                    onFocus1Changed = { isFocused1 = it },
                    onFocus2Changed = { isFocused2 = it },
                )

            rule.runOnIdle {
                focusRequester1.requestFocus()
                // Focus starts on first item.
                assertThat(isFocused1).isTrue()
                assertThat(isFocused2).isFalse()

                val threshold = getThreshold(view)
                if (threshold <= 0f) {
                    return@runOnIdle
                }

                // 1. negative scroll amount maps to FocusDirection.Next
                val nextEvent =
                    createSourceUnknownScrollEvent(view, scrollAmount = -(threshold + 0.5f))
                val nextConsumed = view.dispatchGenericMotionEvent(nextEvent)
                nextEvent.recycle()

                // Verify that the event was consumed and focus moves to the second item.
                assertThat(nextConsumed).isTrue()
                assertThat(isFocused1).isFalse()
                assertThat(isFocused2).isTrue()

                // 2. positive scroll amount maps to FocusDirection.Previous
                val previousEvent =
                    createSourceUnknownScrollEvent(view, scrollAmount = (threshold + 0.5f))
                val previousConsumed = view.dispatchGenericMotionEvent(previousEvent)
                previousEvent.recycle()

                // Verify that the event was consumed and focus moves back to the first item.
                assertThat(previousConsumed).isTrue()
                assertThat(isFocused1).isTrue()
                assertThat(isFocused2).isFalse()
            }
        } finally {
            ComposeUiFlags.isHardwareNavigationHandlingEnabled = originalFlagValue
        }
    }

    @Test
    fun dispatchGenericMotionEvent_sourceUnknownScroll_whenFlagDisabled_doesNotMoveFocus() {
        val originalFlagValue = ComposeUiFlags.isHardwareNavigationHandlingEnabled
        ComposeUiFlags.isHardwareNavigationHandlingEnabled = false
        val focusRequester1 = FocusRequester()
        var isFocused1 = false
        var isFocused2 = false
        try {
            val view =
                setTwoFocusableBoxesContent(
                    focusRequester1 = focusRequester1,
                    onFocus1Changed = { isFocused1 = it },
                    onFocus2Changed = { isFocused2 = it },
                )

            rule.runOnIdle {
                focusRequester1.requestFocus()
                // Focus starts on first item.
                assertThat(isFocused1).isTrue()
                assertThat(isFocused2).isFalse()

                val threshold = getThreshold(view)
                if (threshold <= 0f) {
                    return@runOnIdle
                }

                val event = createSourceUnknownScrollEvent(view, scrollAmount = -(threshold + 0.5f))
                val consumed = view.dispatchGenericMotionEvent(event)
                event.recycle()

                // Verify that event is not consumed.
                assertThat(consumed).isFalse()
                // Verify that focus does not move (stays on first item).
                assertThat(isFocused1).isTrue()
                assertThat(isFocused2).isFalse()
            }
        } finally {
            ComposeUiFlags.isHardwareNavigationHandlingEnabled = originalFlagValue
        }
    }

    @Test
    fun dispatchGenericMotionEvent_sourceUnknownScroll_whenAtBoundary_doesNotConsumeEvent() {
        val originalFlagValue = ComposeUiFlags.isHardwareNavigationHandlingEnabled
        ComposeUiFlags.isHardwareNavigationHandlingEnabled = true
        val focusRequester2 = FocusRequester()
        var isFocused1 = false
        var isFocused2 = false
        try {
            val view =
                setTwoFocusableBoxesContent(
                    focusRequester2 = focusRequester2,
                    onFocus1Changed = { isFocused1 = it },
                    onFocus2Changed = { isFocused2 = it },
                )

            rule.runOnIdle {
                // Focus starts on the SECOND (last) item.
                focusRequester2.requestFocus()
                assertThat(isFocused1).isFalse()
                assertThat(isFocused2).isTrue()

                // negative scroll amount maps to FocusDirection.Next
                val threshold = getThreshold(view)
                if (threshold <= 0f) {
                    return@runOnIdle
                }
                val event = createSourceUnknownScrollEvent(view, scrollAmount = -(threshold + 0.5f))
                val consumed = view.dispatchGenericMotionEvent(event)
                event.recycle()

                // It is at boundary: focus remains on second item, and the event is not consumed.
                assertThat(isFocused1).isFalse()
                assertThat(isFocused2).isTrue()
                assertThat(consumed).isFalse()
            }
        } finally {
            ComposeUiFlags.isHardwareNavigationHandlingEnabled = originalFlagValue
        }
    }

    @Test
    fun dispatchGenericMotionEvent_sourceUnknownScroll_whenThresholdNotExceeded_doesNotMoveFocus() {
        val originalFlagValue = ComposeUiFlags.isHardwareNavigationHandlingEnabled
        ComposeUiFlags.isHardwareNavigationHandlingEnabled = true
        val focusRequester1 = FocusRequester()
        var isFocused1 = false
        var isFocused2 = false
        try {
            val view =
                setTwoFocusableBoxesContent(
                    focusRequester1 = focusRequester1,
                    onFocus1Changed = { isFocused1 = it },
                    onFocus2Changed = { isFocused2 = it },
                )

            rule.runOnIdle {
                focusRequester1.requestFocus()
                // Focus starts on first item.
                assertThat(isFocused1).isTrue()
                assertThat(isFocused2).isFalse()

                val threshold = getThreshold(view)
                if (threshold <= 0f) {
                    return@runOnIdle
                }

                // 1. Dispatch scroll amount below threshold: event is consumed for accumulation,
                // but focus should not move yet.
                val subThresholdEvent =
                    createSourceUnknownScrollEvent(view, scrollAmount = -(threshold / 2f))
                val subThresholdConsumed = view.dispatchGenericMotionEvent(subThresholdEvent)
                subThresholdEvent.recycle()

                assertThat(subThresholdConsumed).isTrue()
                assertThat(isFocused1).isTrue()
                assertThat(isFocused2).isFalse()

                // 2. Dispatch second scroll event so accumulated scroll exceeds threshold:
                // focus moves to the second item.
                val secondEvent =
                    createSourceUnknownScrollEvent(view, scrollAmount = -(threshold / 2f + 0.1f))
                val secondConsumed = view.dispatchGenericMotionEvent(secondEvent)
                secondEvent.recycle()

                assertThat(secondConsumed).isTrue()
                assertThat(isFocused1).isFalse()
                assertThat(isFocused2).isTrue()
            }
        } finally {
            ComposeUiFlags.isHardwareNavigationHandlingEnabled = originalFlagValue
        }
    }

    @Test
    fun dispatchGenericMotionEvent_dispatchScrollEventWhenContentIsScrollableAndIsScrolled_returnsTrue() {
        // 1. Arrange: Set content that can consume a scroll event.
        rule.setContent {
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                // Content larger than the screen to ensure it's scrollable
                Box(Modifier.size(10000.dp))
            }
        }

        var result = false

        // 2. Act: Dispatch a scroll event on the UI thread.
        rule.runOnIdle {
            val composeView = rule.activity.findComposeView()
            result = createScrollEvent(composeView, -10f)
        }

        // 3. Assert: The event should be consumed.
        assertThat(result).isTrue()
    }

    @Test
    fun dispatchGenericMotionEvent_dispatchScrollEventWhenContentIsScrollableAndIsNotScrolled_returnsFalse() {
        // 1. Arrange: Set content that can consume a scroll event.
        rule.setContent {
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                // Content larger than the screen to ensure it's scrollable
                Box(Modifier.size(10000.dp))
            }
        }

        var result = false

        // 2. Act: Dispatch a scroll event on the UI thread.
        rule.runOnIdle {
            val composeView = rule.activity.findComposeView()
            // Because we are already at the top, scrolling in that direction won't scroll
            result = createScrollEvent(composeView, 10f)
        }

        // 3. Assert: The event should be consumed.
        assertThat(result).isFalse()
    }

    @Test
    fun dispatchGenericMotionEvent_dispatchScrollEventWhenContentIsNotScrollable_returnsFalse() {
        // 1. Arrange: Set content that will NOT consume a scroll event.
        rule.setContent { Box(Modifier.fillMaxSize()) }

        var result = true

        // 2. Act: Dispatch a scroll event on the UI thread.
        rule.runOnIdle {
            val composeView = rule.activity.findComposeView()
            result = createScrollEvent(composeView, 10f)
        }

        // 3. Assert: The event should NOT be consumed.
        assertThat(result).isFalse()
    }

    /**
     * Helper function to create and dispatch a mouse scroll MotionEvent.
     *
     * This function simulates a complete scroll gesture by dispatching ACTION_HOVER_ENTER,
     * ACTION_SCROLL, and ACTION_HOVER_EXIT events with increasing event times and a downTime of 0.
     *
     * @return True if the scroll event was consumed, false otherwise.
     */
    private fun createScrollEvent(view: AndroidComposeView, scrollAmount: Float): Boolean {
        var eventTime = System.currentTimeMillis()
        val properties =
            arrayOf(
                MotionEvent.PointerProperties().apply {
                    id = 0
                    toolType = MotionEvent.TOOL_TYPE_MOUSE
                }
            )
        val coords =
            arrayOf(
                MotionEvent.PointerCoords().apply {
                    x = view.width / 2f
                    y = view.height / 2f
                }
            )

        // Dispatch hover enter
        MotionEvent.obtain(
                0,
                eventTime,
                MotionEvent.ACTION_HOVER_ENTER,
                1,
                properties,
                coords,
                0,
                0,
                1f,
                1f,
                19,
                0,
                InputDevice.SOURCE_MOUSE,
                0,
            )
            .also {
                view.dispatchGenericMotionEvent(it)
                it.recycle()
            }

        // Dispatch scroll
        eventTime += 100
        val scrollCoords =
            arrayOf(
                MotionEvent.PointerCoords().apply {
                    setAxisValue(MotionEvent.AXIS_VSCROLL, scrollAmount)
                    x = view.width / 2f
                    y = view.height / 2f
                }
            )
        val result =
            MotionEvent.obtain(
                    0,
                    eventTime,
                    MotionEvent.ACTION_SCROLL,
                    1,
                    properties,
                    scrollCoords,
                    0,
                    0,
                    0f,
                    0f,
                    0,
                    0,
                    InputDevice.SOURCE_MOUSE,
                    0,
                )
                .let {
                    val consumed = view.dispatchGenericMotionEvent(it)
                    it.recycle()
                    consumed
                }

        // Dispatch hover exit
        eventTime += 100
        MotionEvent.obtain(
                0,
                eventTime,
                MotionEvent.ACTION_HOVER_EXIT,
                1,
                properties,
                coords,
                0,
                0,
                1f,
                1f,
                19,
                0,
                InputDevice.SOURCE_MOUSE,
                0,
            )
            .also {
                view.dispatchGenericMotionEvent(it)
                it.recycle()
            }

        return result
    }

    /**
     * Helper to find the AndroidComposeView in the hierarchy by recursively searching the view
     * tree.
     */
    private fun ComponentActivity.findComposeView(): AndroidComposeView {
        val contentViewGroup = findViewById<ViewGroup>(android.R.id.content)
        return findComposeViewIn(contentViewGroup)
            ?: throw IllegalStateException("Could not find AndroidComposeView in hierarchy")
    }

    private fun findComposeViewIn(viewGroup: ViewGroup): AndroidComposeView? {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is AndroidComposeView) {
                return child
            } else if (child is ViewGroup) {
                val composeView = findComposeViewIn(child)
                if (composeView != null) {
                    return composeView
                }
            }
        }
        return null
    }

    private fun findRotaryDevice(): Int {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
        inputManager.inputDeviceIds.forEach { deviceId ->
            inputManager.getInputDevice(deviceId)?.apply {
                motionRanges
                    .find { it.source == InputDevice.SOURCE_ROTARY_ENCODER }
                    ?.let {
                        return deviceId
                    }
            }
        }
        return 0
    }

    private fun getThreshold(view: AndroidComposeView): Float {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN &&
                Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.CINNAMON_BUN_1
        ) {
            val config = ViewConfiguration.get(view.context)
            val deviceId = findRotaryDevice()
            return config
                .getFocusTraversalThreshold(
                    deviceId,
                    MotionEvent.AXIS_SCROLL,
                    InputDevice.SOURCE_ROTARY_ENCODER,
                )
                .toFloat()
        }
        return -1.0f
    }

    private fun createSourceUnknownScrollEvent(
        view: AndroidComposeView,
        scrollAmount: Float,
    ): MotionEvent {
        val properties =
            arrayOf(
                MotionEvent.PointerProperties().apply {
                    id = 0
                    toolType = MotionEvent.TOOL_TYPE_UNKNOWN
                }
            )
        val coords =
            arrayOf(
                MotionEvent.PointerCoords().apply {
                    setAxisValue(MotionEvent.AXIS_SCROLL, scrollAmount)
                    x = view.width / 2f
                    y = view.height / 2f
                }
            )

        val deviceId = findRotaryDevice()

        return MotionEvent.obtain(
            /* downTime = */ 0,
            /* eventTime = */ System.currentTimeMillis(),
            /* action = */ MotionEvent.ACTION_SCROLL,
            /* pointerCount = */ 1,
            /* pointerProperties = */ properties,
            /* pointerCoords = */ coords,
            /* metaState = */ 0,
            /* buttonState = */ 0,
            /* xPrecision = */ 0f,
            /* yPrecision = */ 0f,
            /* deviceId = */ deviceId,
            /* edgeFlags = */ 0,
            /* source = */ InputDevice.SOURCE_UNKNOWN,
            /* flags = */ 0,
        )
    }

    private fun setTwoFocusableBoxesContent(
        focusRequester1: FocusRequester = FocusRequester(),
        focusRequester2: FocusRequester = FocusRequester(),
        onFocus1Changed: (Boolean) -> Unit = {},
        onFocus2Changed: (Boolean) -> Unit = {},
    ): AndroidComposeView {
        var view: AndroidComposeView? = null
        rule.setContent {
            view = LocalView.current as AndroidComposeView
            Column(Modifier.fillMaxSize()) {
                Box(
                    Modifier.requiredSize(50.dp)
                        .focusRequester(focusRequester1)
                        .onFocusChanged { onFocus1Changed(it.isFocused) }
                        .focusable()
                )
                Box(
                    Modifier.requiredSize(50.dp)
                        .focusRequester(focusRequester2)
                        .onFocusChanged { onFocus2Changed(it.isFocused) }
                        .focusable()
                )
            }
        }
        rule.runOnIdle {
            view!!.measureAndLayout()
            // This means the non-touch mode.
            view!!.inputModeManager.requestInputMode(InputMode.Keyboard)
        }
        return view!!
    }
}
