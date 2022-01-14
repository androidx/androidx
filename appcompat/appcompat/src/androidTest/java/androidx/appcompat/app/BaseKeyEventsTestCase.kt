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

import android.app.Instrumentation
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.action.ViewActions
import androidx.appcompat.test.R
import androidx.appcompat.testutils.BaseTestActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.MenuItemCompat
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.testutils.PollingCheck
import java.util.concurrent.atomic.AtomicBoolean
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
abstract class BaseKeyEventsTestCase<A : BaseTestActivity?> protected constructor(activityClass: Class<A>?) {
    @JvmField
    @Rule
    val mActivityTestRule: ActivityTestRule<A>
    private var mInstrumentation: Instrumentation? = null
    private var mActivity: A? = null

    @Before
    fun setup() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation()
        mActivity = mActivityTestRule.activity

        // Wait for activity to have window focus.
        PollingCheck.waitFor(2000) { mActivity!!.hasWindowFocus() }
    }

    @Test
    @MediumTest
    fun testBackDismissesActionMode() {
        val destroyed = AtomicBoolean()
        mActivity!!.runOnUiThread {
            mActivity!!.startSupportActionMode(object : ActionMode.Callback {
                override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                    mode.menuInflater.inflate(R.menu.sample_actions, menu)
                    return true
                }

                override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                    return false
                }

                override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                    return false
                }

                override fun onDestroyActionMode(mode: ActionMode) {
                    destroyed.set(true)
                }
            })
        }
        mInstrumentation!!.waitForIdleSync()
        mInstrumentation!!.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        mInstrumentation!!.waitForIdleSync()
        Assert.assertFalse("Activity was not finished", mActivity!!.isFinishing)
        Assert.assertTrue("ActionMode was destroyed", destroyed.get())
    }

    @Test
    @LargeTest
    @Throws(InterruptedException::class)
    fun testBackCollapsesActionView() {
        // Click on the Search menu item
        Espresso.onView(ViewMatchers.withId(R.id.action_search)).perform(ViewActions.click())
        // Check that the action view is displayed (expanded)
        Espresso.onView(
            ViewMatchers.withClassName(
                Matchers.`is`(
                    CustomCollapsibleView::class.java.name
                )
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        // Let things settle
        mInstrumentation!!.waitForIdleSync()
        // Now send a back event to collapse the custom action view
        mInstrumentation!!.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        mInstrumentation!!.waitForIdleSync()

        // Check that the Activity is still running
        Assert.assertFalse(mActivity!!.isFinishing)
        Assert.assertFalse(mActivity!!.isDestroyed)
        // ... and that our action view is not attached
        Espresso.onView(
            ViewMatchers.withClassName(
                Matchers.`is`(
                    CustomCollapsibleView::class.java.name
                )
            )
        )
            .check(ViewAssertions.doesNotExist())
    }

    @Test
    @MediumTest
    @Throws(InterruptedException::class)
    fun testMenuPressInvokesPanelCallbacks() {
        mInstrumentation!!.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU)
        mInstrumentation!!.waitForIdleSync()
        Assert.assertTrue("onMenuOpened called", mActivity!!.wasOnMenuOpenedCalled())
        mInstrumentation!!.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU)
        mInstrumentation!!.waitForIdleSync()
        Assert.assertTrue("onPanelClosed called", mActivity!!.wasOnPanelClosedCalled())
    }

    @Test
    @MediumTest
    @FlakyTest(bugId = 213627790)
    @Throws(
        InterruptedException::class
    )
    fun testBackPressWithMenuInvokesOnPanelClosed() {
        mInstrumentation!!.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU)
        mInstrumentation!!.waitForIdleSync()
        mInstrumentation!!.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        mInstrumentation!!.waitForIdleSync()
        Assert.assertTrue("onPanelClosed called", mActivity!!.wasOnPanelClosedCalled())
    }

    @Test
    @MediumTest
    @FlakyTest
    @Throws(
        InterruptedException::class
    )
    fun testBackPressWithEmptyMenuHandledByActivity() {
        repopulateWithEmptyMenu()
        mInstrumentation!!.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU)
        mInstrumentation!!.waitForIdleSync()
        mInstrumentation!!.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        mInstrumentation!!.waitForIdleSync()
        Assert.assertTrue("onBackPressed called", mActivity!!.wasOnBackPressedCalled())
    }

    @Test
    @MediumTest
    fun testDelKeyEventReachesActivity() {
        // First send the event
        mInstrumentation!!.sendKeyDownUpSync(KeyEvent.KEYCODE_DEL)
        mInstrumentation!!.waitForIdleSync()
        val downEvent = mActivity!!.invokedKeyDownEvent
        Assert.assertNotNull("onKeyDown called", downEvent)
        Assert.assertEquals(
            "onKeyDown event matches",
            KeyEvent.KEYCODE_DEL.toLong(),
            downEvent.keyCode.toLong()
        )
        val upEvent = mActivity!!.invokedKeyUpEvent
        Assert.assertNotNull("onKeyUp called", upEvent)
        Assert.assertEquals(
            "onKeyUp event matches",
            KeyEvent.KEYCODE_DEL.toLong(),
            upEvent.keyCode.toLong()
        )
    }

    @Test
    @MediumTest
    @Throws(InterruptedException::class)
    open fun testMenuKeyEventReachesActivity() {
        mInstrumentation!!.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU)
        mInstrumentation!!.waitForIdleSync()
        val downEvent = mActivity!!.invokedKeyDownEvent
        Assert.assertNotNull("onKeyDown called", downEvent)
        Assert.assertEquals(
            "onKeyDown event matches",
            KeyEvent.KEYCODE_MENU.toLong(),
            downEvent.keyCode.toLong()
        )
        val upEvent = mActivity!!.invokedKeyUpEvent
        Assert.assertNotNull("onKeyUp called", upEvent)
        Assert.assertEquals(
            "onKeyDown event matches",
            KeyEvent.KEYCODE_MENU.toLong(),
            upEvent.keyCode.toLong()
        )
    }

    @Test
    @MediumTest
    @Throws(Throwable::class)
    fun testActionMenuContent() {
        Espresso.onView(ViewMatchers.withId(R.id.action_search))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .check(ViewAssertions.matches(ViewMatchers.withContentDescription(R.string.search_menu_description)))
        Espresso.onView(ViewMatchers.withId(R.id.action_alpha_shortcut))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .check(ViewAssertions.matches(ViewMatchers.withContentDescription(null as String?)))
        val menu = mActivity!!.menu
        val alphaItem = menu.findItem(R.id.action_alpha_shortcut)
        Assert.assertNotNull(alphaItem)
        mActivityTestRule.runOnUiThread {
            MenuItemCompat.setContentDescription(
                alphaItem,
                mActivity!!.getString(R.string.alpha_menu_description)
            )
            MenuItemCompat.setTooltipText(
                alphaItem,
                mActivity!!.getString(R.string.alpha_menu_tooltip)
            )
        }
        Espresso.onView(ViewMatchers.withId(R.id.action_alpha_shortcut))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .check(ViewAssertions.matches(ViewMatchers.withContentDescription(R.string.alpha_menu_description)))
    }

    @Throws(InterruptedException::class)
    private fun repopulateWithEmptyMenu() {
        var count = 0
        mActivity!!.setShouldPopulateOptionsMenu(false)
        while (count++ < 10) {
            val menu = mActivity!!.menu
            if (menu == null || menu.size() != 0) {
                Thread.sleep(100)
            } else {
                return
            }
        }
    }

    init {
        mActivityTestRule = ActivityTestRule(activityClass)
    }
}
