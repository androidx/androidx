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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@SmallTest
@RunWith(AndroidJUnit4::class)
class OptionsMenuFragmentTest {
    @get:Rule
    val activityRule = ActivityTestRule(FragmentTestActivity::class.java)

    @Test
    fun fragmentWithOptionsMenu() {
        activityRule.setContentView(R.layout.simple_container)
        val fragment = OptionsMenuFragment()
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
            .add(R.id.fragmentContainer2, OptionsMenuFragment())
            .commit()
        activityRule.executePendingTransactions()

        assertWithMessage("Fragment should not have an options menu")
            .that(parent.hasOptionsMenu()).isFalse()
        assertWithMessage("Second child fragment should have an options menu")
            .that(parent.mChildFragmentManager.checkForMenus()).isTrue()
    }

    class OptionsMenuFragment : StrictViewFragment(R.layout.fragment_a) {
        val onCreateOptionsMenuCountDownLatch = CountDownLatch(1)

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setHasOptionsMenu(true)
        }

        override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
            super.onCreateOptionsMenu(menu, inflater)
            onCreateOptionsMenuCountDownLatch.countDown()
            inflater.inflate(R.menu.example_menu, menu)
        }
    }

    class ParentOptionsMenuFragment(
        val createMenu: Boolean = false
    ) : StrictViewFragment(R.layout.double_container) {
        val childFragment = OptionsMenuFragment()

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