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

package androidx.pdf.ink.view

import android.content.Context
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class ViewExtensionsTest {

    private lateinit var context: Context
    private lateinit var view: View

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        view = View(context)
        // Define the view's bounds for testing. Let's assume it's at (100, 100)
        // with a size of 200x100. So its rect is (100, 100, 300, 200).
        view.layout(100, 100, 300, 200)
    }

    @Test
    fun isTouchInView_whenTouchIsInside_returnsTrue() {
        // Create a touch event in the middle of the view.
        val touchEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 150f, 150f, 0)

        assertThat(view.isTouchInView(touchEvent)).isTrue()
    }

    @Test
    fun isTouchInView_whenTouchIsOutside_returnsFalse() {
        // Create a touch event far outside the view's bounds.
        val touchEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500f, 500f, 0)

        assertThat(view.isTouchInView(touchEvent)).isFalse()
    }

    @Test
    fun isTouchInView_whenTouchIsOnLeftEdge_returnsTrue() {
        // Create a touch event exactly on the left edge.
        val touchEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 100f, 150f, 0)

        assertThat(view.isTouchInView(touchEvent)).isTrue()
    }

    @Test
    fun isTouchInView_whenTouchIsJustOutsideRightEdge_returnsFalse() {
        // The rect's right is 300, so a touch at 300.0f is outside because
        // Rect.contains checks for left <= x < right.
        val touchEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 300f, 150f, 0)

        assertThat(view.isTouchInView(touchEvent)).isFalse()
    }

    @Test
    fun isTouchInView_whenTouchIsOnTopEdge_returnsTrue() {
        // Create a touch event exactly on the top edge.
        val touchEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 150f, 100f, 0)

        assertThat(view.isTouchInView(touchEvent)).isTrue()
    }

    @Test
    fun isTouchInView_whenTouchIsJustOutsideBottomEdge_returnsFalse() {
        // The rect's bottom is 200, so a touch at 200.0f is outside.
        val touchEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 150f, 200f, 0)

        assertThat(view.isTouchInView(touchEvent)).isFalse()
    }
}
