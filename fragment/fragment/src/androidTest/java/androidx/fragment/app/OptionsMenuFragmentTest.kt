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

import android.os.Build
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
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O) // Previous SDK levels have issues passing
// null menu that causes leak canary to crash to we don't want to run on those devices
@SmallTest
@RunWith(AndroidJUnit4::class)
class OptionsMenuFragmentTest {

    @Suppress("DEPRECATION")
    var activityRule = androidx.test.rule.ActivityTestRule(FragmentTestActivity::class.java)

    // Detect leaks BEFORE and AFTER activity is destroyed
    @get:Rule
    val ruleChain: RuleChain =
        RuleChain.outerRule(DetectLeaksAfterTestSuccess()).around(activityRule)

    @Test
    fun fragmentWithOptionsMenu() {
        val activity = activityRule.getActivity()
        /**
         * Internal call to [FragmentTestActivity.invalidateMenu] from
         * [FragmentTestActivity.addMenuProvider] upon activity creation
         */
        assertThat(activity.invalidateCount).isEqualTo(1)

        activityRule.setContentView(R.layout.simple_container)
        val fragment = MenuFragment()
        val fm = activity.supportFragmentManager
        fm.beginTransaction().add(R.id.fragmentContainer, fragment).commit()
        activityRule.executePendingTransactions()

        assertWithMessage("Fragment should have an options menu")
            .that(fragment.hasOptionsMenu())
            .isTrue()
        assertWithMessage("Adding fragment with options menu should invalidate menu")
            .that(activity.invalidateCount)
            .isEqualTo(2)
        assertWithMessage("Child fragments should not have an options menu")
            .that(fragment.mChildFragmentManager.checkForMenus())
            .isFalse()

        fm.beginTransaction().remove(fragment).commit()
        activityRule.executePendingTransactions()
        assertWithMessage("Removing fragment with options menu should invalidate menu")
            .that(activity.invalidateCount)
            .isEqualTo(3)
    }

    @LargeTest
    @Test
    fun setMenuVisibilityShowHide() {
        withUse(ActivityScenario.launch(SimpleContainerActivity::class.java)) {
            withActivity {
                /**
                 * Internal call to [SimpleContainerActivity.invalidateMenu] from
                 * [SimpleContainerActivity.addMenuProvider] upon activity creation
                 */
                assertThat(invalidateCount).isEqualTo(1)
            }

            val fm = withActivity { supportFragmentManager }
            val fragment = MenuFragment()

            withActivity { fm.beginTransaction().add(R.id.fragmentContainer, fragment).commitNow() }

            assertWithMessage("Fragment should have an options menu")
                .that(fragment.hasOptionsMenu())
                .isTrue()

            withActivity {
                assertWithMessage("Adding fragment with options menu should invalidate menu")
                    .that(invalidateCount)
                    .isEqualTo(2)
            }

            withActivity {
                fm.beginTransaction().hide(fragment).commitNow()
                assertWithMessage("Hiding fragment with options menu should invalidate menu")
                    .that(invalidateCount)
                    .isEqualTo(3)
            }

            fragment.onCreateOptionsMenuCountDownLatch = CountDownLatch(1)

            withActivity {
                fm.beginTransaction().show(fragment).commitNow()
                assertWithMessage("Showing fragment with options menu should invalidate menu")
                    .that(invalidateCount)
                    .isEqualTo(4)
            }

            assertWithMessage("onCreateOptionsMenu was not called")
                .that(fragment.onCreateOptionsMenuCountDownLatch.await(1000, TimeUnit.MILLISECONDS))
                .isTrue()

            withActivity {
                fm.beginTransaction().remove(fragment).commitNow()
                executePendingTransactions()
                assertWithMessage("Removing fragment with options menu should invalidate menu")
                    .that(invalidateCount)
                    .isEqualTo(5)
            }
        }
    }

    @Test
    fun fragmentWithNoOptionsMenu() {
        val activity = activityRule.getActivity()
        /**
         * Internal call to [FragmentTestActivity.invalidateMenu] from
         * [FragmentTestActivity.addMenuProvider] upon activity creation
         */
        assertThat(activity.invalidateCount).isEqualTo(1)

        activityRule.setContentView(R.layout.simple_container)
        val fragment = StrictViewFragment()
        val fm = activity.supportFragmentManager
        fm.beginTransaction().add(R.id.fragmentContainer, fragment).commit()
        activityRule.executePendingTransactions()

        assertWithMessage("Fragment should not have an options menu")
            .that(fragment.hasOptionsMenu())
            .isFalse()
        assertWithMessage("Adding fragment without options menu should not invalidate menu")
            .that(activity.invalidateCount)
            .isEqualTo(1)
        assertWithMessage("Child fragments should not have an options menu")
            .that(fragment.mChildFragmentManager.checkForMenus())
            .isFalse()

        fm.beginTransaction().remove(fragment).commit()
        activityRule.executePendingTransactions()
        assertThat(activity.invalidateCount).isEqualTo(1)
        assertWithMessage("Removing fragment without options menu should not invalidate menu")
            .that(activity.invalidateCount)
            .isEqualTo(1)
    }

    @Test
    fun childFragmentWithOptionsMenu() {
        val activity = activityRule.getActivity()
        /**
         * Internal call to [FragmentTestActivity.invalidateMenu] from
         * [FragmentTestActivity.addMenuProvider] upon activity creation
         */
        assertThat(activity.invalidateCount).isEqualTo(1)

        activityRule.setContentView(R.layout.simple_container)
        val parent = ParentOptionsMenuFragment()
        val fm = activity.supportFragmentManager
        fm.beginTransaction().add(R.id.fragmentContainer, parent, "parent").commit()
        activityRule.executePendingTransactions()

        assertWithMessage("Fragment should not have an options menu")
            .that(parent.hasOptionsMenu())
            .isFalse()
        assertWithMessage("Child fragment should have an options menu")
            .that(parent.mChildFragmentManager.checkForMenus())
            .isTrue()
        assertWithMessage(
                "Adding child fragment with options menu and parent fragment " +
                    "without should invalidate menu only once"
            )
            .that(activity.invalidateCount)
            .isEqualTo(2)

        parent.mChildFragmentManager.beginTransaction().remove(parent.childFragment).commit()
        activityRule.executePendingTransactions()
        assertWithMessage("Removing child fragment with options menu should invalidate menu")
            .that(activity.invalidateCount)
            .isEqualTo(3)

        fm.beginTransaction().remove(parent).commit()
        activityRule.executePendingTransactions()
        assertWithMessage(
                "Removing parent fragment without options menu should not invalidate menu"
            )
            .that(activity.invalidateCount)
            .isEqualTo(3)
    }

    @Test
    fun childFragmentWithOptionsMenuParentMenuVisibilityFalse() {
        val activity = activityRule.getActivity()
        /**
         * Internal call to [FragmentTestActivity.invalidateMenu] from
         * [FragmentTestActivity.addMenuProvider] upon activity creation
         */
        assertThat(activity.invalidateCount).isEqualTo(1)

        activityRule.setContentView(R.layout.simple_container)
        val parent = ParentOptionsMenuFragment()
        val fm = activity.supportFragmentManager

        parent.setMenuVisibility(false)
        fm.beginTransaction().add(R.id.fragmentContainer, parent, "parent").commit()
        activityRule.executePendingTransactions()

        assertWithMessage("Fragment should not have an options menu")
            .that(parent.hasOptionsMenu())
            .isFalse()
        assertWithMessage("Child fragment should have an options menu")
            .that(parent.childFragmentManager.checkForMenus())
            .isTrue()
        assertWithMessage(
                "Adding child fragment with options menu and parent fragment " +
                    "without should invalidate menu only once"
            )
            .that(activity.invalidateCount)
            .isEqualTo(2)

        activityRule.runOnUiThread {
            assertWithMessage("child fragment onCreateOptions menu was not called")
                .that(parent.childFragment.onCreateOptionsMenuCountDownLatch.count)
                .isEqualTo(1)
        }

        parent.mChildFragmentManager.beginTransaction().remove(parent.childFragment).commit()
        activityRule.executePendingTransactions()
        assertWithMessage("Removing child fragment with options menu should invalidate menu")
            .that(activity.invalidateCount)
            .isEqualTo(3)

        fm.beginTransaction().remove(parent).commit()
        activityRule.executePendingTransactions()
        assertWithMessage(
                "Removing parent fragment without options menu should not invalidate menu"
            )
            .that(activity.invalidateCount)
            .isEqualTo(3)
    }

    @Test
    fun childFragmentWithPrepareOptionsMenuParentMenuVisibilityFalse() {
        val activity = activityRule.getActivity()
        /**
         * Internal call to [FragmentTestActivity.invalidateMenu] from
         * [FragmentTestActivity.addMenuProvider] upon activity creation
         */
        assertThat(activity.invalidateCount).isEqualTo(1)

        activityRule.setContentView(R.layout.simple_container)
        val parent = ParentOptionsMenuFragment()
        val fm = activity.supportFragmentManager

        parent.setMenuVisibility(false)
        fm.beginTransaction().add(R.id.fragmentContainer, parent, "parent").commit()
        activityRule.executePendingTransactions()

        assertWithMessage("Fragment should not have an options menu")
            .that(parent.hasOptionsMenu())
            .isFalse()
        assertWithMessage("Child fragment should have an options menu")
            .that(parent.childFragmentManager.checkForMenus())
            .isTrue()
        assertWithMessage(
                "Adding child fragment with options menu and parent fragment " +
                    "without should invalidate menu only once"
            )
            .that(activity.invalidateCount)
            .isEqualTo(2)

        activityRule.runOnUiThread {
            assertWithMessage("child fragment onCreateOptions menu was not called")
                .that(parent.childFragment.onPrepareOptionsMenuCountDownLatch.count)
                .isEqualTo(1)
        }

        parent.mChildFragmentManager.beginTransaction().remove(parent.childFragment).commit()
        activityRule.executePendingTransactions()
        assertWithMessage("Removing child fragment with options menu should invalidate menu")
            .that(activity.invalidateCount)
            .isEqualTo(3)

        fm.beginTransaction().remove(parent).commit()
        activityRule.executePendingTransactions()
        assertWithMessage(
                "Removing parent fragment without options menu should not invalidate menu"
            )
            .that(activity.invalidateCount)
            .isEqualTo(3)
    }

    @Test
    fun parentAndChildFragmentWithOptionsMenu() {
        val activity = activityRule.getActivity()
        /**
         * Internal call to [FragmentTestActivity.invalidateMenu] from
         * [FragmentTestActivity.addMenuProvider] upon activity creation
         */
        assertThat(activity.invalidateCount).isEqualTo(1)

        activityRule.setContentView(R.layout.simple_container)
        val parent = ParentOptionsMenuFragment(true)
        val fm = activity.supportFragmentManager
        fm.beginTransaction().add(R.id.fragmentContainer, parent, "parent").commit()
        activityRule.executePendingTransactions()

        assertWithMessage("Fragment should have an options menu")
            .that(parent.hasOptionsMenu())
            .isTrue()
        assertWithMessage("Child fragment should have an options menu")
            .that(parent.mChildFragmentManager.checkForMenus())
            .isTrue()
        assertWithMessage(
                "Adding parent and child fragment both with options menu should invalidate menu twice"
            )
            .that(activity.invalidateCount)
            .isEqualTo(3)

        parent.mChildFragmentManager.beginTransaction().remove(parent.childFragment).commit()
        activityRule.executePendingTransactions()
        assertWithMessage("Removing child fragment with options menu should invalidate menu")
            .that(activity.invalidateCount)
            .isEqualTo(4)

        fm.beginTransaction().remove(parent).commit()
        activityRule.executePendingTransactions()
        assertWithMessage("Removing parent fragment with options menu should invalidate menu")
            .that(activity.invalidateCount)
            .isEqualTo(5)
    }

    @Test
    fun grandchildFragmentWithOptionsMenu() {
        val activity = activityRule.getActivity()
        /**
         * Internal call to [FragmentTestActivity.invalidateMenu] from
         * [FragmentTestActivity.addMenuProvider] upon activity creation
         */
        assertThat(activity.invalidateCount).isEqualTo(1)

        activityRule.setContentView(R.layout.simple_container)
        val grandParent = StrictViewFragment(R.layout.double_container)
        val fm = activity.supportFragmentManager
        fm.beginTransaction().add(R.id.fragmentContainer, grandParent, "grandParent").commit()
        activityRule.executePendingTransactions()
        assertWithMessage(
                "Adding grandparent fragment without options menu should not invalidate menu"
            )
            .that(activity.invalidateCount)
            .isEqualTo(1)

        val parent = ParentOptionsMenuFragment()
        grandParent.childFragmentManager
            .beginTransaction()
            .add(R.id.fragmentContainer1, parent)
            .commit()
        activityRule.executePendingTransactions()

        assertWithMessage("Parent fragment should not have an options menu")
            .that(grandParent.hasOptionsMenu())
            .isFalse()
        assertWithMessage("Grandchild fragment should have an options menu")
            .that(grandParent.mChildFragmentManager.checkForMenus())
            .isTrue()
        assertWithMessage(
                "Adding grandchild fragment with options menu and " +
                    "parent fragment without should invalidate menu only once"
            )
            .that(activity.invalidateCount)
            .isEqualTo(2)

        parent.mChildFragmentManager.beginTransaction().remove(parent.childFragment).commit()
        activityRule.executePendingTransactions()
        assertWithMessage("Removing grandchild fragment with options menu should invalidate menu")
            .that(activity.invalidateCount)
            .isEqualTo(3)

        grandParent.mChildFragmentManager.beginTransaction().remove(parent).commit()
        activityRule.executePendingTransactions()
        assertWithMessage(
                "Removing parent fragment without options menu should not invalidate menu"
            )
            .that(activity.invalidateCount)
            .isEqualTo(3)

        fm.beginTransaction().remove(grandParent).commit()
        activityRule.executePendingTransactions()
        assertWithMessage(
                "Removing grandparent fragment without options menu should not invalidate menu"
            )
            .that(activity.invalidateCount)
            .isEqualTo(3)
    }

    // Ensure that we check further than just the first child fragment
    @Test
    fun secondChildFragmentWithOptionsMenu() {
        val activity = activityRule.getActivity()
        /**
         * Internal call to [FragmentTestActivity.invalidateMenu] from
         * [FragmentTestActivity.addMenuProvider] upon activity creation
         */
        assertThat(activity.invalidateCount).isEqualTo(1)

        activityRule.setContentView(R.layout.simple_container)
        val parent = StrictViewFragment(R.layout.double_container)
        val fm = activity.supportFragmentManager
        fm.beginTransaction().add(R.id.fragmentContainer, parent, "parent").commit()
        activityRule.executePendingTransactions()

        val childWithoutMenu = Fragment()
        parent.childFragmentManager
            .beginTransaction()
            .add(R.id.fragmentContainer1, childWithoutMenu)
            .commit()
        activityRule.executePendingTransactions()

        val childWithMenu = MenuFragment()
        parent.childFragmentManager
            .beginTransaction()
            .add(R.id.fragmentContainer2, childWithMenu)
            .commit()
        activityRule.executePendingTransactions()

        assertWithMessage("Fragment should not have an options menu")
            .that(parent.hasOptionsMenu())
            .isFalse()
        assertWithMessage("Second child fragment should have an options menu")
            .that(parent.mChildFragmentManager.checkForMenus())
            .isTrue()
        assertWithMessage(
                "Adding second child fragment with options menu and the parent and " +
                    "first child fragments without should invalidate menu only once"
            )
            .that(activity.invalidateCount)
            .isEqualTo(2)

        parent.mChildFragmentManager.beginTransaction().remove(childWithoutMenu).commit()
        activityRule.executePendingTransactions()
        assertWithMessage(
                "Removing first child fragment without options menu should not invalidate menu"
            )
            .that(activity.invalidateCount)
            .isEqualTo(2)

        parent.mChildFragmentManager.beginTransaction().remove(childWithMenu).commit()
        activityRule.executePendingTransactions()
        assertWithMessage("Removing second child fragment with options menu should invalidate menu")
            .that(activity.invalidateCount)
            .isEqualTo(3)

        fm.beginTransaction().remove(parent).commit()
        activityRule.executePendingTransactions()
        assertWithMessage(
                "Removing parent fragment without options menu should not invalidate menu"
            )
            .that(activity.invalidateCount)
            .isEqualTo(3)
    }

    @Test
    fun onPrepareOptionsMenu() {
        val activity = activityRule.getActivity()
        /**
         * Internal call to [FragmentTestActivity.invalidateMenu] from
         * [FragmentTestActivity.addMenuProvider] upon activity creation
         */
        assertThat(activity.invalidateCount).isEqualTo(1)

        activityRule.setContentView(R.layout.simple_container)
        val fragment = MenuFragment()
        val fm = activity.supportFragmentManager
        fm.beginTransaction().add(R.id.fragmentContainer, fragment).commit()
        activityRule.executePendingTransactions()
        assertWithMessage("Adding fragment with options menu should invalidate menu")
            .that(activity.invalidateCount)
            .isEqualTo(2)

        openActionBarOverflowOrOptionsMenu(activity.applicationContext)
        onView(ViewMatchers.withText("Item1")).perform(ViewActions.click())
        assertWithMessage("onPrepareOptionsMenu was not called")
            .that(fragment.onPrepareOptionsMenuCountDownLatch.await(1000, TimeUnit.MILLISECONDS))
            .isTrue()

        fm.beginTransaction().remove(fragment).commit()
        activityRule.executePendingTransactions()
        assertWithMessage("Removing fragment with options menu should invalidate menu")
            .that(activity.invalidateCount)
            .isEqualTo(3)
    }

    @Test
    fun inflatesMenu() {
        val activity = activityRule.getActivity()
        /**
         * Internal call to [FragmentTestActivity.invalidateMenu] from
         * [FragmentTestActivity.addMenuProvider] upon activity creation
         */
        assertThat(activity.invalidateCount).isEqualTo(1)

        activityRule.setContentView(R.layout.simple_container)
        val fragment = MenuFragment()
        val fm = activity.supportFragmentManager
        fm.beginTransaction().add(R.id.fragmentContainer, fragment).commit()
        activityRule.executePendingTransactions()
        assertWithMessage("Adding fragment with options menu should invalidate menu")
            .that(activity.invalidateCount)
            .isEqualTo(2)

        openActionBarOverflowOrOptionsMenu(activity.applicationContext)
        onView(ViewMatchers.withText("Item1"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        onView(ViewMatchers.withText("Item2"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        fm.beginTransaction().remove(fragment).commit()
        activityRule.executePendingTransactions()
        assertWithMessage("Removing fragment with options menu should invalidate menu")
            .that(activity.invalidateCount)
            .isEqualTo(3)
    }

    @Test
    fun menuItemSelected() {
        val activity = activityRule.getActivity()
        /**
         * Internal call to [FragmentTestActivity.invalidateMenu] from
         * [FragmentTestActivity.addMenuProvider] upon activity creation
         */
        assertThat(activity.invalidateCount).isEqualTo(1)

        activityRule.setContentView(R.layout.simple_container)
        val fragment = MenuFragment()
        val fm = activity.supportFragmentManager
        fm.beginTransaction().add(R.id.fragmentContainer, fragment).commit()
        activityRule.executePendingTransactions()
        assertWithMessage("Adding fragment with options menu should invalidate menu")
            .that(activity.invalidateCount)
            .isEqualTo(2)

        openActionBarOverflowOrOptionsMenu(activity.applicationContext)
        onView(ViewMatchers.withText("Item1")).perform(ViewActions.click())

        openActionBarOverflowOrOptionsMenu(activity.applicationContext)
        onView(ViewMatchers.withText("Item2")).perform(ViewActions.click())

        fm.beginTransaction().remove(fragment).commit()
        activityRule.executePendingTransactions()
        assertWithMessage("Removing fragment with options menu should invalidate menu")
            .that(activity.invalidateCount)
            .isEqualTo(3)
    }

    @Test
    fun onOptionsMenuClosed() {
        val activity = activityRule.getActivity()
        /**
         * Internal call to [FragmentTestActivity.invalidateMenu] from
         * [FragmentTestActivity.addMenuProvider] upon activity creation
         */
        assertThat(activity.invalidateCount).isEqualTo(1)

        activityRule.setContentView(R.layout.simple_container)
        val fragment = MenuFragment()
        val fm = activity.supportFragmentManager
        fm.beginTransaction().add(R.id.fragmentContainer, fragment).commit()
        activityRule.executePendingTransactions()
        assertWithMessage("Adding fragment with options menu should invalidate menu")
            .that(activity.invalidateCount)
            .isEqualTo(2)

        openActionBarOverflowOrOptionsMenu(activity.applicationContext)
        activityRule.runOnUiThread { activity.closeOptionsMenu() }
        assertWithMessage("onOptionsMenuClosed was not called")
            .that(fragment.onOptionsMenuClosedCountDownLatch.await(1000, TimeUnit.MILLISECONDS))
            .isTrue()

        fm.beginTransaction().remove(fragment).commit()
        activityRule.executePendingTransactions()
        assertWithMessage("Removing fragment with options menu should invalidate menu")
            .that(activity.invalidateCount)
            .isEqualTo(3)
    }

    @Suppress("DEPRECATION")
    class MenuFragment : StrictViewFragment(R.layout.fragment_a) {
        var onCreateOptionsMenuCountDownLatch = CountDownLatch(1)
        val onPrepareOptionsMenuCountDownLatch = CountDownLatch(1)
        val onOptionsMenuClosedCountDownLatch = CountDownLatch(1)

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setHasOptionsMenu(true)
        }

        @Deprecated("Deprecated in Fragment")
        override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
            super.onCreateOptionsMenu(menu, inflater)
            onCreateOptionsMenuCountDownLatch.countDown()
            inflater.inflate(R.menu.example_menu, menu)
        }

        @Deprecated("Deprecated in Fragment")
        override fun onPrepareOptionsMenu(menu: Menu) {
            super.onPrepareOptionsMenu(menu)
            onPrepareOptionsMenuCountDownLatch.countDown()
        }

        @Deprecated("Deprecated in Fragment")
        override fun onOptionsMenuClosed(menu: Menu) {
            super.onOptionsMenuClosed(menu)
            onOptionsMenuClosedCountDownLatch.countDown()
        }
    }

    @Suppress("DEPRECATION")
    class ParentOptionsMenuFragment(val createMenu: Boolean = false) :
        StrictViewFragment(R.layout.double_container) {
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
            savedInstanceState: Bundle?,
        ): View? {
            childFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer1, childFragment)
                .commit()
            childFragmentManager.executePendingTransactions()
            return super.onCreateView(inflater, container, savedInstanceState)
        }

        @Deprecated("Deprecated in Fragment")
        override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
            super.onCreateOptionsMenu(menu, inflater)
            inflater.inflate(R.menu.example_menu, menu)
        }
    }
}
