/*
 * Copyright (C) 2015 The Android Open Source Project
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
package androidx.appcompat.app

import android.os.SystemClock
import android.view.MotionEvent
import android.view.Window
import androidx.appcompat.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.MediumTest
import org.junit.Assert.assertTrue
import org.junit.Test

@MediumTest
public class SupportActionBarTestCase {

    /**
     * Regression test for b/186791590 where custom window callbacks were overwritten.
     */
    @Test
    public fun testSetSupportActionBarPreservesWindowCallback() {
        ActivityScenario.launch(ToolbarAppCompatActivity::class.java).onActivity { activity ->
            val previousCallback = activity.window.callback
            var dispatchedToCallback = false

            activity.window.callback = object : Window.Callback by previousCallback {
                override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
                    dispatchedToCallback = true
                    return previousCallback.dispatchTouchEvent(event)
                }
            }

            activity.setSupportActionBar(activity.findViewById(R.id.toolbar))
            activity.window.callback.dispatchTouchEvent(obtainEvent(MotionEvent.ACTION_DOWN))

            assertTrue(
                "Event was not dispatched to custom window callback",
                dispatchedToCallback
            )
        }
    }

    @Suppress("SameParameterValue")
    private fun obtainEvent(action: Int): MotionEvent {
        val now: Long = SystemClock.uptimeMillis()
        return MotionEvent.obtain(now, now, action, 0f, 0f, 0)
    }
}