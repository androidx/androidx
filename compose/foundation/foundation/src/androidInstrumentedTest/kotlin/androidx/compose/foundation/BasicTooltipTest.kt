/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation

import android.os.Build
import android.view.InputDevice
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.CLASSIFICATION_DEEP_PRESS
import android.view.MotionEvent.CLASSIFICATION_NONE
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class BasicTooltipTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun tooltip_handleDefaultGestures_enabled() {
        lateinit var state: BasicTooltipState
        lateinit var scope: CoroutineScope
        rule.setContent {
            state = rememberBasicTooltipState(initialIsVisible = false)
            scope = rememberCoroutineScope()
            BasicTooltipBox(
                positionProvider = EmptyPositionProvider(),
                tooltip = {},
                state = state,
                modifier = Modifier.testTag(TOOLTIP_ANCHOR)
            ) {
                Box(modifier = Modifier.requiredSize(1.dp)) {}
            }
        }

        // Stop auto advance for test consistency
        rule.mainClock.autoAdvance = false

        // The tooltip should not be showing at first
        Truth.assertThat(state.isVisible).isFalse()

        // Long press the anchor
        rule.onNodeWithTag(TOOLTIP_ANCHOR, true).performTouchInput { longClick() }

        // Check that the tooltip is now showing
        rule.waitForIdle()
        Truth.assertThat(state.isVisible).isTrue()

        // Dismiss the tooltip and check that it dismissed
        scope.launch { state.dismiss() }
        rule.waitForIdle()
        Truth.assertThat(state.isVisible).isFalse()

        // Hover over the anchor with mouse input
        rule.onNodeWithTag(TOOLTIP_ANCHOR).performMouseInput { enter() }

        // Check that the tooltip is now showing
        rule.waitForIdle()
        Truth.assertThat(state.isVisible).isTrue()

        // Hover away from the anchor
        rule.onNodeWithTag(TOOLTIP_ANCHOR).performMouseInput { exit() }

        // Check that the tooltip is now dismissed
        rule.waitForIdle()
        Truth.assertThat(state.isVisible).isFalse()
    }

    @Test
    fun tooltip_handleDefaultGestures_disabled() {
        lateinit var state: BasicTooltipState
        rule.setContent {
            state = rememberBasicTooltipState(initialIsVisible = false)
            BasicTooltipBox(
                positionProvider = EmptyPositionProvider(),
                tooltip = {},
                enableUserInput = false,
                state = state,
                modifier = Modifier.testTag(TOOLTIP_ANCHOR)
            ) {
                Box(modifier = Modifier.requiredSize(1.dp)) {}
            }
        }

        // Stop auto advance for test consistency
        rule.mainClock.autoAdvance = false

        // The tooltip should not be showing at first
        Truth.assertThat(state.isVisible).isFalse()

        // Long press the anchor
        rule.onNodeWithTag(TOOLTIP_ANCHOR).performTouchInput { longClick() }

        // Check that the tooltip is still not showing
        rule.waitForIdle()
        Truth.assertThat(state.isVisible).isFalse()

        // Hover over the anchor with mouse input
        rule.onNodeWithTag(TOOLTIP_ANCHOR).performMouseInput { enter() }

        // Check that the tooltip is still not showing
        rule.waitForIdle()
        Truth.assertThat(state.isVisible).isFalse()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun tooltip_longPress_deepPress() {
        lateinit var view: View
        lateinit var state: BasicTooltipState
        rule.setContent {
            view = LocalView.current
            state = rememberBasicTooltipState(initialIsVisible = false)
            BasicTooltipBox(
                positionProvider = EmptyPositionProvider(),
                tooltip = {},
                state = state,
                modifier = Modifier.testTag(TOOLTIP_ANCHOR)
            ) {
                Box(modifier = Modifier.requiredSize(1.dp)) {}
            }
        }

        // Stop auto advance for test consistency
        rule.mainClock.autoAdvance = false

        val pointerProperties =
            arrayOf(
                MotionEvent.PointerProperties().also {
                    it.id = 0
                    it.toolType = MotionEvent.TOOL_TYPE_FINGER
                }
            )

        val downEvent =
            MotionEvent.obtain(
                /* downTime = */ 0,
                /* eventTime = */ 0,
                /* action = */ ACTION_DOWN,
                /* pointerCount = */ 1,
                /* pointerProperties = */ pointerProperties,
                /* pointerCoords = */ arrayOf(
                    MotionEvent.PointerCoords().apply {
                        x = 5f
                        y = 5f
                    }
                ),
                /* metaState = */ 0,
                /* buttonState = */ 0,
                /* xPrecision = */ 0f,
                /* yPrecision = */ 0f,
                /* deviceId = */ 0,
                /* edgeFlags = */ 0,
                /* source = */ InputDevice.SOURCE_TOUCHSCREEN,
                /* displayId = */ 0,
                /* flags = */ 0,
                /* classification = */ CLASSIFICATION_NONE
            )

        view.dispatchTouchEvent(downEvent)
        rule.mainClock.advanceTimeBy(50)

        rule.runOnIdle { Truth.assertThat(state.isVisible).isFalse() }

        val deepPressMoveEvent =
            MotionEvent.obtain(
                /* downTime = */ 0,
                /* eventTime = */ 50,
                /* action = */ ACTION_MOVE,
                /* pointerCount = */ 1,
                /* pointerProperties = */ pointerProperties,
                /* pointerCoords = */ arrayOf(
                    MotionEvent.PointerCoords().apply {
                        x = 10f
                        y = 10f
                    }
                ),
                /* metaState = */ 0,
                /* buttonState = */ 0,
                /* xPrecision = */ 0f,
                /* yPrecision = */ 0f,
                /* deviceId = */ 0,
                /* edgeFlags = */ 0,
                /* source = */ InputDevice.SOURCE_TOUCHSCREEN,
                /* displayId = */ 0,
                /* flags = */ 0,
                /* classification = */ CLASSIFICATION_DEEP_PRESS
            )

        view.dispatchTouchEvent(deepPressMoveEvent)
        rule.mainClock.advanceTimeBy(50)

        // Even though the timeout didn't pass, the deep press should immediately show the tooltip
        rule.runOnIdle { Truth.assertThat(state.isVisible).isTrue() }
    }
}

private class EmptyPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        return IntOffset(0, 0)
    }
}

private const val TOOLTIP_ANCHOR = "anchor"
