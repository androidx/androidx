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

package androidx.core.view

import android.support.v4.BaseInstrumentationTestCase
import android.view.View
import androidx.core.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class WindowCompatTest :
    BaseInstrumentationTestCase<WindowCompatActivity>(WindowCompatActivity::class.java) {
    @Test
    fun tests_setDecorFitsSystemWindows() {
        val view = mActivityTestRule.activity.findViewById<View>(R.id.view)!!

        // Record the initial position
        val initialPosition = IntArray(2)
        mActivityTestRule.runOnUiThread { view.getLocationInWindow(initialPosition) }

        // Now call setDecorFitsSystemWindows()
        mActivityTestRule.runOnUiThread {
            WindowCompat.setDecorFitsSystemWindows(mActivityTestRule.activity.window, false)
        }

        // Await for a layout pass
        val latch = CountDownLatch(1)
        view.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> latch.countDown() }
        latch.await(2, TimeUnit.SECONDS)

        // Now check the new position
        val finalPosition = IntArray(2)
        mActivityTestRule.runOnUiThread { view.getLocationInWindow(finalPosition) }

        // Assert that the content view has moved to be laid at from 0,0 in the window
        assertNotEquals(initialPosition, finalPosition)
        assertEquals(0, finalPosition[0])
        assertEquals(0, finalPosition[1])
    }
}
