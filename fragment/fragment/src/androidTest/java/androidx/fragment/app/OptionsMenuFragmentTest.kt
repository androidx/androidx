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

package androidx.fragment.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SmallTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class OptionsMenuFragmentTest {
    @Suppress("DEPRECATION")
    @get:Rule
    var activityRule = androidx.test.rule.ActivityTestRule(FragmentTestActivity::class.java)

    @Test
    fun fragmentWithOptionsMenu() {
        activityRule.setContentView(R.layout.simple_container)
        val fragment = MenuFragment()
        val fm = activityRule.activity.supportFragmentManager
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment)
            .commit()
        activityRule.executePendingTransactions()

        assertWithMessage("Fragment should have an options menu")
            .that(fragment.hasOptionsMenu()).isTrue()
        assertWithMessage("Child fragments should not have an options menu")
            .that(fragment.mChildFragmentManager.checkForMenus()).isFalse()
    }

    @LargeTest
    @Test
    fun setMenuVisibilityShowHide() {
        with(ActivityScenario.launch(SimpleContainerActivity::class.java)) {
            val fm = withActivity { supportFragmentManager }

            val fragment = MenuFragment()

            withActivity {
                fm.beginTransaction()
                    .add(R.id.fragmentContainer, fragment)
                    .commitNow()
            }

            assertWithMessage("Fragment should have an options menu")
                .that(fragment.hasOptionsMenu()).isTrue()

            withActivity {
                fm.beginTransaction()
                    .hide(fragment)
                    .commitNow()
            }

            fragment.onCreateOptionsMenuCountDownLatch = CountDownLatch(1)

            withActivity {
                fm.beginTransaction()
                    .show(fragment)
                    .commitNow()
            }

            assertWithMessage("onCreateOptionsMenu was not called")
                .that(fragment.onCreateOptionsMenuCountDownLatch.await(1000, TimeUnit.MILLISECONDS))
                .isTrue()
        }
    }

    @Test
    fun fragmentWithNoOptionsMenu() {
        activityRule.setContentView(R.layout.simple_container)
        val fragment = StrictViewFragment()
        val fm = activityRule.activity.supportFragmentManager
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment)
            .commit()
        activityRule.executePendingTransactions()

        assertWithMessage("Fragment should not have an options menu")
            .that(fragment.hasOptionsMenu()).isFalse()
        assertWithMessage("Child fragments should not have an options menu")
            .that(fragment.mChildFragmentManager.checkForMenus()).isFalse()
    }

    @Test
    fun childFragmentWithOptionsMenu() {
        activityRule.setContentView(R.layout.simple_container)
        val parent = ParentOptionsMenuFragment()
        val fm = activityRule.activity.supportFragmentManager
        fm.beginTransaction()
            .add(R.id.fragmentContainer, parent, "parent")
            .commit()
        activityRule.executePendingTransactions()

        assertWithMessage("Fragment should not have an options menu")
            .that(parent.hasOptionsMenu()).isFalse()
        assertWithMessage("Child fragment should have an options menu")
            .that(parent.mChildFragmentManager.checkForMenus()).isTrue()
    }

    @Test
    fun childFragmentWithOptionsMenuParentMenuVisibilityFalse() {
        activityRule.setContentView(R.layout.simple_container)
        val parent = ParentOptionsMenuFragment()
        val fm = activityRule.activity.supportFragmentManager

        parent.setMenuVisibility(false)
        fm.beginTransaction()
            .add(R.id.fragmentContainer, parent, "parent")
            .commit()
        activityRule.executePendingTransactions()

        assertWithMessage("Fragment should not have an options menu")
            .that(parent.hasOptionsMenu()).isFalse()
        assertWithMessage("Child fragment should have an options menu")
            .that(parent.childFragmentManager.checkForMenus()).isTrue()

        activityRule.runOnUiThread {
            assertWithMessage("child fragment onCreateOptions menu was not called")
                .that(parent.childFragment.onCreateOptionsMenuCountDownLatch.count)
                .isEqualTo(1)
        }
    }

    @Test
    fun childFragmentWithPrepareOptionsMenuParentMenuVisibilityFalse() {
        activityRule.setContentView(R.layout.simple_container)
        val parent = ParentOptionsMenuFragment()
        val fm = activityRule.activity.supportFragmentManager

        parent.setMenuVisibility(false)
        fm.beginTransaction()
            .add(R.id.fragmentContainer, parent, "parent")
            .commit()
        activityRule.executePendingTransactions()

        assertWithMessage("Fragment should not have an options menu")
            .that(parent.hasOptionsMenu()).isFalse()
        assertWithMessage("Child fragment should have an options menu")
            .that(parent.childFragmentManager.checkForMenus()).isTrue()

        activityRule.runOnUiThread {
            assertWithMessage("child fragment onCreateOptions menu was not called")
                .that(parent.childFragment.onPrepareOptionsMenuCountDownLatch.count)
                .isEqualTo(1)
        }
    }

    @Test
    fun parentAndChildFragmentWithOptionsMenu() {
        activityRule.setContentView(R.layout.simple_container)
        val parent = ParentOptionsMenuFragment(true)
        val fm = activityRule.activity.supportFragmentManager
        fm.beginTransaction()
            .add(R.id.fragmentContainer, parent, "parent")
            .commit()
        activityRule.executePendingTransactions()

        assertWithMessage("Fragment should have an options menu")
            .that(parent.hasOptionsMenu()).isTrue()
        assertWithMessage("Child fragment should have an options menu")
            .that(parent.mChildFragmentManager.checkForMenus()).isTrue()
    }

    @Test
    fun grandchildFragmentWithOptionsMenu() {
        activityRule.setContentView(R.layout.simple_container)
        val parent = StrictViewFragment(R.layout.double_container)
        val fm = activityRule.activity.supportFragmentManager
        fm.beginTransaction()
            .add(R.id.fragmentContainer, parent, "parent")
            .commit()
        activityRule.executePendingTransactions()

        parent.childFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer1, ParentOptionsMenuFragment())
            .commit()
        activityRule.executePendingTransactions()

        assertWithMessage("Fragment should not have an options menu")
            .that(parent.hasOptionsMenu()).isFalse()
        assertWithMessage("Grandchild fragment should have an options menu")
            .that(parent.mChildFragmentManager.checkForMenus()).isTrue()
    }

    // Ensure that we check further than just the first child fragment
    @Test
    fun secondChildFragmentWithOptionsMenu() {
        activityRule.setContentView(R.layout.simple_container)
        val parent = StrictViewFragment(R.layout.double_container)
        val fm = activityRule.activity.supportFragmentManager
        fm.beginTransaction()
            .add(R.id.fragmentContainer, parent, "parent")
            .commit()
        activityRule.executePendingTransactions()

        parent.childFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer1, Fragment())
            .commit()
        activityRule.executePendingTransactions()

        parent.childFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer2, MenuFragment())
            .commit()
        activityRule.executePendingTransactions()

        assertWithMessage("Fragment should not have an options menu")
            .that(parent.hasOptionsMenu()).isFalse()
        assertWithMessage("Second child fragment should have an options menu")
            .that(parent.mChildFragmentManager.checkForMenus()).isTrue()
    }

    @Test
    fun onPrepareOptionsMenu() {
        activityRule.setContentView(R.layout.simple_container)
        val fragment = MenuFragment()
        val fm = activityRule.activity.supportFragmentManager
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment)
            .commit()
        activityRule.executePendingTransactions()

        openActionBarOverflowOrOptionsMenu(activityRule.activity.applicationContext)
        onView(ViewMatchers.withText("Item1")).perform(ViewActions.click())
        assertWithMessage("onPrepareOptionsMenu was not called")
            .that(fragment.onPrepareOptionsMenuCountDownLatch.await(1000, TimeUnit.MILLISECONDS))
            .isTrue()
    }

    @Test
    fun inflatesMenu() {
        activityRule.setContentView(R.layout.simple_container)
        val fragment = MenuFragment()
        val fm = activityRule.activity.supportFragmentManager
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment)
            .commit()
        activityRule.executePendingTransactions()

        openActionBarOverflowOrOptionsMenu(activityRule.activity.applicationContext)
        onView(ViewMatchers.withText("Item1"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        onView(ViewMatchers.withText("Item2"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun menuItemSelected() {
        activityRule.setContentView(R.layout.simple_container)
        val fragment = MenuFragment()
        val fm = activityRule.activity.supportFragmentManager
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment)
            .commit()
        activityRule.executePendingTransactions()

        openActionBarOverflowOrOptionsMenu(activityRule.activity.applicationContext)
        onView(ViewMatchers.withText("Item1")).perform(ViewActions.click())

        openActionBarOverflowOrOptionsMenu(activityRule.activity.applicationContext)
        onView(ViewMatchers.withText("Item2")).perform(ViewActions.click())
    }

    @Test
    fun onOptionsMenuClosed() {
        activityRule.setContentView(R.layout.simple_container)
        val fragment = MenuFragment()
        val fm = activityRule.activity.supportFragmentManager
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment)
            .commit()
        activityRule.executePendingTransactions()

        openActionBarOverflowOrOptionsMenu(activityRule.activity.applicationContext)
        activityRule.runOnUiThread {
            activityRule.activity.closeOptionsMenu()
        }
        assertWithMessage("onOptionsMenuClosed was not called")
            .that(fragment.onOptionsMenuClosedCountDownLatch.await(1000, TimeUnit.MILLISECONDS))
            .isTrue()
    }

    class MenuFragment : StrictViewFragment(R.layout.fragment_a) {
        var onCreateOptionsMenuCountDownLatch = CountDownLatch(1)
        val onPrepareOptionsMenuCountDownLatch = CountDownLatch(1)
        val onOptionsMenuClosedCountDownLatch = CountDownLatch(1)

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setHasOptionsMenu(true)
        }

        override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
            super.onCreateOptionsMenu(menu, inflater)
            onCreateOptionsMenuCountDownLatch.countDown()
            inflater.inflate(R.menu.example_menu, menu)
        }

        override fun onPrepareOptionsMenu(menu: Menu) {
            super.onPrepareOptionsMenu(menu)
            onPrepareOptionsMenuCountDownLatch.countDown()
        }

        override fun onOptionsMenuClosed(menu: Menu) {
            super.onOptionsMenuClosed(menu)
            onOptionsMenuClosedCountDownLatch.countDown()
        }
    }

    class ParentOptionsMenuFragment(
        val createMenu: Boolean = false
    ) : StrictViewFragment(R.layout.double_container) {
        val childFragment = MenuFragment()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            if (createMenu) {
                setHasOptionsMenu(true)
            }
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            childFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer1, childFragment)
                .commit()
            childFragmentManager.executePendingTransactions()
            return super.onCreateView(inflater, container, savedInstanceState)
        }

        override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
            super.onCreateOptionsMenu(menu, inflater)
            inflater.inflate(R.menu.example_menu, menu)
        }
    }
}