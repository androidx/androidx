/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.drawerlayout.widget

import android.os.Build
import android.view.KeyEvent
import android.view.View
import androidx.drawerlayout.test.R
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.PollingCheck
import androidx.testutils.withActivity
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
public class DrawerBackHandlingTest {
    @get:Rule
    public val activityScenarioRule = ActivityScenarioRule(
        DrawerSingleStartActivity::class.java
    )

    @Test
    @SmallTest
    public fun testBackPress() {
        val listener = ObservableDrawerListener()
        val drawerLayout = activityScenarioRule.withActivity {
            val drawerLayout = findViewById<DrawerLayout>(R.id.drawer)
            drawerLayout.addDrawerListener(listener)
            drawerLayout.open()
            drawerLayout
        }

        // Wait until the animation ends. We disable animations on test
        // devices, but this is useful when running on a local device.
        PollingCheck.waitFor {
            listener.drawerOpenedCalled
        }
        listener.reset()

        // Ensure that back pressed dispatcher callback is registered on T+.
        if (Build.VERSION.SDK_INT >= 33) {
            Assert.assertTrue(drawerLayout.isBackInvokedCallbackRegistered)
        }

        Espresso.onView(ViewMatchers.isRoot()).perform(ViewActions.pressKey(KeyEvent.KEYCODE_BACK))

        PollingCheck.waitFor {
            listener.drawerClosedCalled
        }
        listener.reset()

        Assert.assertNull(drawerLayout.findOpenDrawer())

        // Ensure that back pressed dispatcher callback is unregistered on T+.
        if (Build.VERSION.SDK_INT >= 33) {
            Assert.assertFalse(drawerLayout.isBackInvokedCallbackRegistered)
        }
    }

    internal inner class ObservableDrawerListener : DrawerLayout.DrawerListener {
        var drawerOpenedCalled = false
        var drawerClosedCalled = false

        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

        override fun onDrawerOpened(drawerView: View) {
            drawerOpenedCalled = true
        }

        override fun onDrawerClosed(drawerView: View) {
            drawerClosedCalled = true
        }

        override fun onDrawerStateChanged(newState: Int) {}

        fun reset() {
            drawerOpenedCalled = false
            drawerClosedCalled = false
        }
    }
}
