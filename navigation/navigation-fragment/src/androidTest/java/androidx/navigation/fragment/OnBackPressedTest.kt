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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.test.EmptyFragment
import androidx.navigation.fragment.test.NavigationActivity
import androidx.navigation.fragment.test.NavigationActivityWithFragmentTag
import androidx.navigation.fragment.test.NavigationBaseActivity
import androidx.navigation.fragment.test.R
import androidx.navigation.navOptions
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.MediumTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Suppress("DEPRECATION")
@MediumTest
@RunWith(Parameterized::class)
class OnBackPressedTest(private val activityClass: Class<NavigationBaseActivity>) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Array<Class<out NavigationBaseActivity>> {
            return arrayOf(
                NavigationActivity::class.java,
                NavigationActivityWithFragmentTag::class.java
            )
        }
    }

    @Test
    fun testOnBackPressedOnRoot() {
        with(ActivityScenario.launch(activityClass)) {
            val countDownLatch = withActivity {
                navController.setGraph(R.navigation.nav_simple)
                onBackPressed()
                finishCountDownLatch
            }
            assertWithMessage("onBackPressed() should finish the activity on the root")
                .that(countDownLatch.await(1, TimeUnit.SECONDS))
                .isTrue()
        }
    }

    @Test
    fun testOnBackPressedAfterNavigate() {
        with(ActivityScenario.launch(activityClass)) {
            withActivity {
                navController.setGraph(R.navigation.nav_simple)
                navController.navigate(R.id.empty_fragment)
                onBackPressed()
                assertWithMessage("onBackPressed() should trigger NavController.popBackStack()")
                    .that(navController.currentDestination?.id)
                    .isEqualTo(R.id.start_fragment)
            }
        }
    }

    @Test
    fun testOnBackPressedAfterNavigateWithAnimators() {
        with(ActivityScenario.launch(activityClass)) {
            withActivity {
                navController.setGraph(R.navigation.nav_simple)
                navController.navigate(
                    R.id.empty_fragment,
                    null,
                    navOptions {
                        anim {
                            enter = R.animator.fade_enter
                            exit = R.animator.fade_exit
                            popEnter = R.animator.fade_enter
                            popExit = R.animator.fade_exit
                        }
                    }
                )
                onBackPressed()
                assertWithMessage("onBackPressed() should trigger NavController.popBackStack()")
                    .that(navController.currentDestination?.id)
                    .isEqualTo(R.id.start_fragment)
            }
        }
    }

    @Test
    fun testOnBackPressedAfterNavigatePopUpTo() {
        with(ActivityScenario.launch(activityClass)) {
            withActivity {
                navController.setGraph(R.navigation.nav_simple)

                val navigator =
                    navController.navigatorProvider.getNavigator(FragmentNavigator::class.java)
                val fragment = supportFragmentManager.findFragmentById(R.id.nav_host)
                navController.navigate(R.id.empty_fragment)
                fragment?.childFragmentManager?.executePendingTransactions()

                navController.navigate(
                    R.id.empty_fragment_2,
                    null,
                    navOptions { popUpTo(R.id.empty_fragment) { inclusive = true } }
                )
                fragment?.childFragmentManager?.executePendingTransactions()

                onBackPressed()
                fragment?.childFragmentManager?.executePendingTransactions()

                assertWithMessage("onBackPressed() should trigger NavController.popBackStack()")
                    .that(navController.currentDestination?.id)
                    .isEqualTo(R.id.start_fragment)
                assertWithMessage("navigator back stack should contain 1 entry")
                    .that(navigator.backStack.value.size)
                    .isEqualTo(1)
            }
        }
    }

    @Test
    fun testOnBackPressedAfterNavigate_notDefaultNavHost() {
        with(ActivityScenario.launch(activityClass)) {
            val countDownLatch = withActivity {
                navController.setGraph(R.navigation.nav_simple)
                navController.navigate(R.id.empty_fragment)
                supportFragmentManager
                    .beginTransaction()
                    .setPrimaryNavigationFragment(null)
                    .commitNow()

                onBackPressed()
                finishCountDownLatch
            }
            assertWithMessage(
                    "onBackPressed() should finish the activity when not the " + "primary nav"
                )
                .that(countDownLatch.await(1, TimeUnit.SECONDS))
                .isTrue()
        }
    }

    @Test
    fun testOnBackPressedWithChildBackStack() {
        with(ActivityScenario.launch(activityClass)) {
            withActivity {
                val navHostFragment =
                    supportFragmentManager.primaryNavigationFragment as NavHostFragment
                val navHostFragmentManager = navHostFragment.childFragmentManager
                val navController = navHostFragment.navController
                navController.setGraph(R.navigation.nav_simple)
                navController.navigate(R.id.child_back_stack_fragment)
                navHostFragmentManager.executePendingTransactions()

                val currentFragment =
                    navHostFragmentManager.primaryNavigationFragment as ChildBackStackFragment
                assertWithMessage("Current Fragment should have a child Fragment by default")
                    .that(currentFragment.childFragment)
                    .isNotNull()

                onBackPressed()
                assertWithMessage(
                        "onBackPressed() should not trigger NavController when there " +
                            "is a child back stack"
                    )
                    .that(navController.currentDestination?.id)
                    .isEqualTo(R.id.child_back_stack_fragment)
                assertWithMessage("Child Fragment should be popped")
                    .that(currentFragment.childFragment)
                    .isNull()
            }
        }
    }

    @Test
    fun testOnBackPressedToNullViewFragment() {
        with(ActivityScenario.launch(activityClass)) {
            withActivity {
                navController.setGraph(R.navigation.nav_simple)

                val navigator =
                    navController.navigatorProvider.getNavigator(FragmentNavigator::class.java)
                val fragment = supportFragmentManager.findFragmentById(R.id.nav_host)

                navController.navigate(R.id.null_view_fragment, null, null)
                fragment?.childFragmentManager?.executePendingTransactions()

                navController.navigate(R.id.empty_fragment, null, null)
                fragment?.childFragmentManager?.executePendingTransactions()

                onBackPressed()
                fragment?.childFragmentManager?.executePendingTransactions()

                assertWithMessage("onBackPressed() should trigger NavController.popBackStack()")
                    .that(navController.currentDestination?.id)
                    .isEqualTo(R.id.null_view_fragment)
                assertWithMessage("navigator back stack should contain 1 entry")
                    .that(navigator.backStack.value.size)
                    .isEqualTo(2)
            }
        }
    }
}

class ChildBackStackFragment : EmptyFragment() {
    val childFragment
        get() = childFragmentManager.findFragmentByTag("child")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        childFragmentManager
            .beginTransaction()
            .add(Fragment(), "child")
            .addToBackStack(null)
            .commit()
    }
}

class NullViewFragment : EmptyFragment() {
    var viewAlreadyCreated = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (viewAlreadyCreated) {
            return null
        }
        viewAlreadyCreated = true
        return FrameLayout(requireContext())
    }
}
