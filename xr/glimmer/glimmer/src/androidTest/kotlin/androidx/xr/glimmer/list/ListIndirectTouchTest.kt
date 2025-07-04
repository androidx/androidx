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

package androidx.xr.glimmer.list

import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.height
import androidx.compose.ui.ExperimentalIndirectTouchTypeApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectTouchEvent
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performIndirectTouchEvent
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.core.view.InputDeviceCompat.SOURCE_TOUCH_NAVIGATION
import androidx.test.filters.MediumTest
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.setGlimmerThemeContent
import org.junit.Test

@MediumTest
@OptIn(ExperimentalIndirectTouchTypeApi::class)
class ListIndirectTouchTest : BaseListTestWithOrientation(Orientation.Vertical) {

    @Test
    fun performIndirectScrollBackward() {
        rule.setGlimmerThemeContent {
            TestList(modifier = Modifier.height(200.dp), itemsCount = 3) { index ->
                Text(text = "Item-$index", modifier = Modifier.height(101.dp).focusable())
            }
        }

        rule.onNodeWithText("Item-0").requestFocus()

        rule.onNodeWithText("Item-0").isDisplayed()
        rule.onNodeWithText("Item-1").isDisplayed()
        rule.onNodeWithText("Item-2").isNotDisplayed()

        val swipeDistance = with(rule.density) { 105.dp.toPx() }
        rule.onNodeWithTag(LIST_TEST_TAG).performIndirectSwipe(-swipeDistance)

        rule.onNodeWithText("Item-0").isNotDisplayed()
        rule.onNodeWithText("Item-1").isDisplayed()
        rule.onNodeWithText("Item-2").isDisplayed()
    }

    @Test
    fun performIndirectScrollForward() {
        rule.setGlimmerThemeContent {
            TestList(modifier = Modifier.height(200.dp), itemsCount = 3) { index ->
                Text(text = "Item-$index", modifier = Modifier.height(101.dp).focusable())
            }
        }

        rule.onNodeWithTag(LIST_TEST_TAG).performScrollToIndex(2)
        rule.onNodeWithText("Item-2").requestFocus()

        rule.onNodeWithText("Item-0").isNotDisplayed()
        rule.onNodeWithText("Item-1").isDisplayed()
        rule.onNodeWithText("Item-2").isDisplayed()

        val swipeDistance = with(rule.density) { 105.dp.toPx() }
        rule.onNodeWithTag(LIST_TEST_TAG).performIndirectSwipe(swipeDistance)

        rule.onNodeWithText("Item-0").isDisplayed()
        rule.onNodeWithText("Item-1").isDisplayed()
        rule.onNodeWithText("Item-2").isNotDisplayed()
    }

    /** Synthetically range the x movements from 1000 to 0 */
    private fun SemanticsNodeInteraction.performIndirectSwipe(distance: Float) {
        val currentTime = SystemClock.uptimeMillis()

        val down =
            MotionEvent.obtain(
                currentTime, // downTime,
                currentTime, // eventTime,
                MotionEvent.ACTION_DOWN,
                0f,
                Offset.Zero.y,
                0,
            )
        down.source = SOURCE_TOUCH_NAVIGATION
        performIndirectTouchEvent(IndirectTouchEvent(down))

        val move =
            MotionEvent.obtain(
                currentTime + 200L,
                currentTime + 200L,
                MotionEvent.ACTION_MOVE,
                distance,
                Offset.Zero.y,
                0,
            )
        move.source = SOURCE_TOUCH_NAVIGATION
        performIndirectTouchEvent(IndirectTouchEvent(move))

        val up =
            MotionEvent.obtain(
                currentTime + 200L,
                currentTime + 200L,
                MotionEvent.ACTION_UP,
                distance,
                Offset.Zero.y,
                0,
            )
        up.source = SOURCE_TOUCH_NAVIGATION
        performIndirectTouchEvent(IndirectTouchEvent(up))
    }
}
