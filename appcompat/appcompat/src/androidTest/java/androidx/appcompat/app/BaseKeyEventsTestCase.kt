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
@file:Suppress("UNCHECKED_CAST")

package androidx.appcompat.app

import android.app.Instrumentation
import android.os.Build
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.test.R
import androidx.appcompat.testutils.BaseTestActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.MenuItemCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.PollingCheck
import androidx.testutils.withActivity
import java.util.concurrent.atomic.AtomicBoolean
import org.hamcrest.Matchers.`is`
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
abstract class BaseKeyEventsTestCase<A : BaseTestActivity>(private val activityClass: Class<A>) {
    private var mInstrumentation: Instrumentation? = null

    @Before
    fun setup() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation()
    }

    @Test
    @MediumTest
    fun testBackDismissesActionMode() {
        with(ActivityScenario.launch(activityClass)) {
            val destroyed = AtomicBoolean()
            val scenario = (this as? ActivityScenario<BaseTestActivity>)!!

            scenario.withActivity {
                startSupportActionMode(object : ActionMode.Callback {
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
                })!!
            }

            pressBack()
            PollingCheck.waitFor { destroyed.get() }
        }
    }

    @Test
    @LargeTest
    @Throws(InterruptedException::class)
    fun testBackCollapsesActionView() {
        with(ActivityScenario.launch(activityClass)) {
            // Click on the Search menu item
            onView(withId(R.id.action_search)).perform(click())

            // Check that the action view is displayed (expanded)
            onView(withClassName(`is`(CustomCollapsibleView::class.java.name)))
                .check(matches(isDisplayed()))

            // Press the back button
            pressBack()
            mInstrumentation!!.waitForIdleSync()

            // Check that the Activity is still running
            val scenario = (this as? ActivityScenario<BaseTestActivity>)!!
            assertFalse(scenario.withActivity { isFinishing })
            assertFalse(scenario.withActivity { isDestroyed })

            // ...and that our action view is not attached
            onView(withClassName(`is`(CustomCollapsibleView::class.java.name)))
                .check(ViewAssertions.doesNotExist())
        }
    }

    @Test
    @MediumTest
    @Throws(InterruptedException::class)
    fun testMenuPressInvokesPanelCallbacks() {
        with(ActivityScenario.launch(activityClass)) {
            // Pressing the menu key opens the menu.
            val scenario = (this as? ActivityScenario<BaseTestActivity>)!!
            scenario.pressMenuKeyAndWaitFor { wasOnMenuOpenedCalled() }

            if (Build.VERSION.SDK_INT < 28) {
                // Prior to SDK 28, pressing the menu key a second time closes the overflow menu.
                scenario.pressMenuKeyAndWaitFor { wasOnPanelClosedCalled() }
            }
        }
    }

    @Test
    @MediumTest
    @Throws(
        InterruptedException::class
    )
    fun testBackPressWithMenuInvokesOnPanelClosed() {
        with(ActivityScenario.launch(activityClass)) {
            // Pressing the menu key opens the menu.
            val scenario = (this as? ActivityScenario<BaseTestActivity>)!!
            scenario.pressMenuKeyAndWaitFor { wasOnMenuOpenedCalled() }

            // Press back and wait the menu panel to close.
            pressBack()
            PollingCheck.waitFor { scenario.withActivity { wasOnPanelClosedCalled() } }
        }
    }

    @Test
    @MediumTest
    fun testBackPressWithEmptyMenuHandledByActivity() {
        with(ActivityScenario.launch(activityClass)) {
            // Pressing the menu key with an empty menu does nothing.
            val scenario = (this as? ActivityScenario<BaseTestActivity>)!!
            scenario.repopulateWithEmptyMenu()
            scenario.pressMenuKeyAndWaitFor { true /* sync with activity thread */ }

            // Press back and wait for the activity to be destroyed via back press.
            val activity = scenario.withActivity { this }
            pressBackUnconditionally()
            PollingCheck.waitFor { activity.isDestroyed }
            Assert.assertTrue("onBackPressed called", activity.wasOnBackPressedCalled())
        }
    }

    @Test
    @MediumTest
    fun testDelKeyEventReachesActivity() {
        with(ActivityScenario.launch(activityClass)) {
            val scenario = (this as? ActivityScenario<BaseTestActivity>)!!

            // First send the event
            onView(isRoot()).perform(pressKey(KeyEvent.KEYCODE_DEL))
            val downEvent = scenario.withActivity { invokedKeyDownEvent }
            assertNotNull("onKeyDown called", downEvent)
            assertEquals(
                "onKeyDown event matches",
                KeyEvent.KEYCODE_DEL.toLong(),
                downEvent.keyCode.toLong()
            )
            val upEvent = scenario.withActivity { invokedKeyUpEvent }
            assertNotNull("onKeyUp called", upEvent)
            assertEquals(
                "onKeyUp event matches",
                KeyEvent.KEYCODE_DEL.toLong(),
                upEvent.keyCode.toLong()
            )
        }
    }

    @Test
    @MediumTest
    @Throws(InterruptedException::class)
    open fun testMenuKeyEventReachesActivity() {
        with(ActivityScenario.launch(activityClass)) {
            val scenario = (this as? ActivityScenario<BaseTestActivity>)!!
            scenario.pressMenuKeyAndWaitFor { wasOnMenuOpenedCalled() }

            val downEvent = scenario.withActivity { invokedKeyDownEvent }
            assertNotNull("onKeyDown called", downEvent)
            assertEquals(
                "onKeyDown event matches",
                KeyEvent.KEYCODE_MENU.toLong(),
                downEvent.keyCode.toLong()
            )

            val upEvent = scenario.withActivity { invokedKeyUpEvent }
            assertNotNull("onKeyUp called", upEvent)
            assertEquals(
                "onKeyDown event matches",
                KeyEvent.KEYCODE_MENU.toLong(),
                upEvent.keyCode.toLong()
            )
        }
    }

    @Test
    @MediumTest
    @Throws(Throwable::class)
    fun testActionMenuContent() {
        with(ActivityScenario.launch(activityClass)) {
            val scenario = (this as? ActivityScenario<BaseTestActivity>)!!
            onView(withId(R.id.action_search))
                .check(matches(isDisplayed()))
                .check(matches(withContentDescription(R.string.search_menu_description)))
            onView(withId(R.id.action_alpha_shortcut))
                .check(matches(isDisplayed()))
                .check(matches(withContentDescription(null as String?)))

            val menu = scenario.withActivity { menu }
            val alphaItem = menu.findItem(R.id.action_alpha_shortcut)
            assertNotNull(alphaItem)
            scenario.withActivity {
                MenuItemCompat.setContentDescription(
                    alphaItem,
                    getString(R.string.alpha_menu_description)
                )
                MenuItemCompat.setTooltipText(
                    alphaItem,
                    getString(R.string.alpha_menu_tooltip)
                )
            }
            onView(withId(R.id.action_alpha_shortcut))
                .check(matches(isDisplayed()))
                .check(matches(withContentDescription(R.string.alpha_menu_description)))
        }
    }

    private fun ActivityScenario<BaseTestActivity>.repopulateWithEmptyMenu() {
        withActivity { setShouldPopulateOptionsMenu(false) }
        PollingCheck.waitFor { withActivity { menu.size() == 0 } }
    }

    private inline fun ActivityScenario<BaseTestActivity>.pressMenuKeyAndWaitFor(
        crossinline block: BaseTestActivity.() -> Boolean
    ) {
        onView(isRoot()).perform(pressKey(KeyEvent.KEYCODE_MENU))
        PollingCheck.waitFor { withActivity(block) }
    }
}
