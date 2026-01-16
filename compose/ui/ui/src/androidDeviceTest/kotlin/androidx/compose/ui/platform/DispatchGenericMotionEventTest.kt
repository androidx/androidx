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

import android.view.InputDevice
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for dispatching generic motion events (specifically, scroll) directly to the
 * AndroidComposeView to verify behavior.
 */
@RunWith(AndroidJUnit4::class)
class DispatchGenericMotionEventDirectlyToAndroidComposeViewTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>(StandardTestDispatcher())

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
}
