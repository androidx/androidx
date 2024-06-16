/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.customview.widget

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ViewDragHelperTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityRule = androidx.test.rule.ActivityTestRule(TestActivity::class.java)

    @Test
    fun testProcessTouchEventWithInconsistentStream() {
        val callback =
            object : ViewDragHelper.Callback() {
                override fun tryCaptureView(child: View, pointerId: Int): Boolean = false
            }
        activityRule.runOnUiThread {
            val activity = activityRule.activity
            val content = activity.findViewById<View>(android.R.id.content)!! as ViewGroup
            val helper = ViewDragHelper.create(content, callback)

            // Ensure the state is STATE_DRAGGING with the pointer DOWN.
            helper.processTouchEvent(obtainMotionEvent(MotionEvent.ACTION_DOWN, 2))

            // Set the active pointer ID to match.
            helper.captureChildView(content.getChildAt(0), 2)

            // Feed in touch event without the pointer ID.
            helper.processTouchEvent(obtainMotionEvent(MotionEvent.ACTION_MOVE, 0))
        }
    }
}

fun obtainMotionEvent(action: Int, pointerId: Int): MotionEvent {
    @Suppress("DEPRECATION")
    return MotionEvent.obtain(
        System.currentTimeMillis(),
        System.currentTimeMillis(),
        action,
        1,
        intArrayOf(pointerId),
        arrayOf(MotionEvent.PointerCoords()),
        0,
        0f,
        0f,
        0,
        0,
        0,
        0
    )
}
