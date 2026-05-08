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

package androidx.xr.compose.spatial

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.testing.ShadowActivityEmbeddingController
import androidx.xr.compose.testing.SubspaceTestingActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests for [SpatialPopup]. */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowActivityEmbeddingController::class])
class OnClickOutsideTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun outsideClickNode_onGlobalInput_whenNotAttached_returnsEarly() {
        var clickOutsideCalled = false
        val node = OutsideClickNode(enabled = true, onClickOutside = { clickOutsideCalled = true })

        assertThat(node.isAttached).isFalse()

        node.onGlobalInput()

        assertThat(clickOutsideCalled).isFalse()
    }

    @Test
    fun outsideClickNode_onPointerEvent_whenNotAttached_returnsEarly() {
        val node = OutsideClickNode(enabled = true, onClickOutside = {})

        assertThat(node.isAttached).isFalse()

        node.onPointerEvent(
            pointerEvent = PointerEvent(listOf()),
            pass = PointerEventPass.Main,
            bounds = IntSize.Zero,
        )
    }

    @Test
    fun onClickOutside_enabled_triggersCallback() {
        var clickOutsideCalled = false
        composeTestRule.setContent {
            Box(
                modifier =
                    Modifier.size(100.dp)
                        .onClickOutside(
                            enabled = true,
                            onClickOutside = { clickOutsideCalled = true },
                        )
            )
        }

        assertThat(clickOutsideCalled).isFalse()

        triggerOutsideClick()

        // Wait for debounce delay (100ms)
        composeTestRule.mainClock.advanceTimeBy(150)
        composeTestRule.waitForIdle()

        assertThat(clickOutsideCalled).isTrue()
    }

    @Test
    fun onClickOutside_disabled_doesNotTriggerCallback() {
        var clickOutsideCalled = false
        composeTestRule.setContent {
            Box(
                modifier =
                    Modifier.size(100.dp)
                        .onClickOutside(
                            enabled = false,
                            onClickOutside = { clickOutsideCalled = true },
                        )
            )
        }

        triggerOutsideClick()

        composeTestRule.mainClock.advanceTimeBy(150)
        composeTestRule.waitForIdle()

        assertThat(clickOutsideCalled).isFalse()
    }

    @Test
    fun onClickOutside_insideClick_suppressesOutsideClick() {
        var clickOutsideCalled = false
        composeTestRule.setContent {
            Box(
                modifier =
                    Modifier.size(100.dp)
                        .testTag("box")
                        .onClickOutside(
                            enabled = true,
                            onClickOutside = { clickOutsideCalled = true },
                        )
            )
        }

        // Click inside the box
        composeTestRule.onNodeWithTag("box").performClick()

        // Immediately trigger outside click
        triggerOutsideClick()

        // Wait for debounce delay
        composeTestRule.mainClock.advanceTimeBy(150)
        composeTestRule.waitForIdle()

        // Should not be called because the inside click cancelled/blocked it
        assertThat(clickOutsideCalled).isFalse()
    }

    private fun triggerOutsideClick() {
        composeTestRule.runOnIdle {
            val windowManagerGlobalClass = Class.forName("android.view.WindowManagerGlobal")
            val getInstanceMethod = windowManagerGlobalClass.getMethod("getInstance")
            val windowManagerGlobal = getInstanceMethod.invoke(null)
            val mViewsField = windowManagerGlobalClass.getDeclaredField("mViews")
            mViewsField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val views = mViewsField.get(windowManagerGlobal) as List<View>

            // Find the InputCaptureView (it's private, so we check by its class simpleName)
            val inputCaptureView = views.find { it.javaClass.simpleName == "InputCaptureView" }
            checkNotNull(inputCaptureView) { "Could not find InputCaptureView in WindowManager" }

            // Dispatch a MotionEvent to simulate touch outside
            val now = SystemClock.uptimeMillis()
            val event = MotionEvent.obtain(now, now, MotionEvent.ACTION_OUTSIDE, 0f, 0f, 0)
            inputCaptureView.dispatchTouchEvent(event)
            event.recycle()
        }
    }
}
