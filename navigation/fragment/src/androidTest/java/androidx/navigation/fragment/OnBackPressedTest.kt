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

package androidx.navigation.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.test.EmptyFragment
import androidx.navigation.fragment.test.NavigationActivity
import androidx.navigation.fragment.test.R
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class OnBackPressedTest {

    @get:Rule
    var activityRule = ActivityTestRule(NavigationActivity::class.java)

    @UiThreadTest
    @Test
    fun testOnBackPressedOnRoot() {
        val activity = activityRule.activity
        val navController = activity.navController
        navController.setGraph(R.navigation.nav_simple)
        activity.onBackPressed()
        assertWithMessage("onBackPressed() should finish the activity on the root")
            .that(activity.isFinishing)
            .isTrue()
    }

    @UiThreadTest
    @Test
    fun testOnBackPressedAfterNavigate() {
        val activity = activityRule.activity
        val navController = activity.navController
        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.empty_fragment)

        activity.onBackPressed()
        assertWithMessage("onBackPressed() should trigger NavController.popBackStack()")
            .that(navController.currentDestination?.id)
            .isEqualTo(R.id.start_fragment)
    }

    @UiThreadTest
    @Test
    fun testOnBackPressedWithChildBackStack() {
        val activity = activityRule.activity
        val navHostFragment = activity.supportFragmentManager.primaryNavigationFragment
                as NavHostFragment
        val navHostFragmentManager = navHostFragment.childFragmentManager
        val navController = navHostFragment.navController
        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.child_back_stack_fragment)
        navHostFragmentManager.executePendingTransactions()

        val currentFragment = navHostFragmentManager.primaryNavigationFragment
                as ChildBackStackFragment
        assertWithMessage("Current Fragment should have a child Fragment by default")
            .that(currentFragment.childFragment)
            .isNotNull()

        activity.onBackPressed()
        assertWithMessage("onBackPressed() should not trigger NavController when there is a " +
                "child back stack")
            .that(navController.currentDestination?.id)
            .isEqualTo(R.id.child_back_stack_fragment)
        assertWithMessage("Child Fragment should be popped")
            .that(currentFragment.childFragment)
            .isNull()
    }
}

class ChildBackStackFragment : EmptyFragment() {
    val childFragment get() = childFragmentManager.findFragmentByTag("child")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        childFragmentManager.beginTransaction()
            .add(Fragment(), "child")
            .addToBackStack(null)
            .commit()
    }
}