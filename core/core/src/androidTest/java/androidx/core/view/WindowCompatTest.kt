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

import android.app.Dialog
import android.graphics.Color
import android.os.Build
import android.support.v4.BaseInstrumentationTestCase
import android.view.View
import android.view.Window
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
import androidx.core.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class WindowCompatTest :
    BaseInstrumentationTestCase<WindowCompatActivity>(WindowCompatActivity::class.java) {
    @Test
    fun tests_setDecorFitsSystemWindows() {
        val window = mActivityTestRule.activity.window
        val view = mActivityTestRule.activity.findViewById<View>(R.id.view)!!
        mActivityTestRule.runOnUiThread { WindowCompat.setDecorFitsSystemWindows(window, false) }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertViewFillWindow(view, window)
    }

    @Suppress("DEPRECATION")
    @Test
    fun tests_enableEdgeToEdge_activity() {
        val window = mActivityTestRule.activity.window
        val view = mActivityTestRule.activity.findViewById<View>(R.id.view)!!
        mActivityTestRule.runOnUiThread {
            WindowCompat.enableEdgeToEdge(window)
            assertEquals(window.statusBarColor, Color.TRANSPARENT)
            assertEquals(window.navigationBarColor, Color.TRANSPARENT)
            if (Build.VERSION.SDK_INT >= 28) {
                assertEquals(
                    if (Build.VERSION.SDK_INT >= 30) LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    else LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES,
                    window.attributes.layoutInDisplayCutoutMode,
                )
            }
            if (Build.VERSION.SDK_INT >= 29) {
                assertEquals(false, window.isStatusBarContrastEnforced)
                assertEquals(false, window.isNavigationBarContrastEnforced)
            }
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertViewFillWindow(view, window)
    }

    @Suppress("DEPRECATION")
    @Test
    fun tests_enableEdgeToEdge_dialog() {
        mActivityTestRule.runOnUiThread {
            val dialog = Dialog(mActivityTestRule.activity)
            val window = dialog.window!!
            WindowCompat.enableEdgeToEdge(window)

            // Initialize the decor view after calling enableEdgeToEdge
            window.decorView

            assertEquals(window.statusBarColor, Color.TRANSPARENT)
            assertEquals(window.navigationBarColor, Color.TRANSPARENT)
            if (Build.VERSION.SDK_INT >= 28) {
                assertEquals(
                    if (Build.VERSION.SDK_INT >= 30) LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    else LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES,
                    window.attributes.layoutInDisplayCutoutMode,
                )
            }
            if (Build.VERSION.SDK_INT >= 29) {
                assertEquals(false, window.isStatusBarContrastEnforced)
                assertEquals(false, window.isNavigationBarContrastEnforced)
            }
        }
    }

    private fun assertViewFillWindow(view: View, window: Window) {
        mActivityTestRule.runOnUiThread {
            val decorView = window.decorView
            val locationInWindow = IntArray(2)
            view.getLocationInWindow(locationInWindow)

            assertEquals(0, locationInWindow[0])
            assertEquals(0, locationInWindow[1])

            assertEquals(decorView.width, view.width)
            assertEquals(decorView.height, view.height)
        }
    }
}
