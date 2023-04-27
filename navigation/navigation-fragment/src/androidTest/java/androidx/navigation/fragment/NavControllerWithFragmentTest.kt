/*
 * Copyright 2022 The Android Open Source Project
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

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.createGraph
import androidx.navigation.fragment.test.EmptyFragment
import androidx.navigation.fragment.test.NavigationActivity
import androidx.navigation.fragment.test.R
import androidx.navigation.navOptions
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavControllerWithFragmentTest {

    @Test
    fun fragmentNavigateWithSingleTop() = withNavigationActivity {
        navController.navigate(R.id.empty_fragment)

        val fm =
            supportFragmentManager.findFragmentById(R.id.nav_host)?.childFragmentManager
        fm?.executePendingTransactions()
        val fragment = fm?.findFragmentById(R.id.nav_host)

        val oldEntry = navController.currentBackStackEntry

        navController.navigate(
            R.id.empty_fragment,
            null,
            NavOptions.Builder().setLaunchSingleTop(true).build()
        )

        fm?.executePendingTransactions()

        val replacementFragment = fm?.findFragmentById(R.id.nav_host)

        assertWithMessage("Replacement should be a new instance")
            .that(replacementFragment)
            .isNotSameInstanceAs(fragment)

        assertWithMessage("Old Entry should have been DESTROYED")
            .that(oldEntry!!.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)
        assertWithMessage("New Entry should be RESUMED")
            .that(navController.currentBackStackEntry!!.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
    }
    @Ignore("b/276806142")
    @Test
    fun fragmentNavigateClearBackStack() = withNavigationActivity {
        navController.setGraph(R.navigation.nav_simple)

        val fm = supportFragmentManager.findFragmentById(R.id.nav_host)?.childFragmentManager
        fm?.executePendingTransactions()

        val navigator = navController.navigatorProvider.getNavigator(FragmentNavigator::class.java)
        assertThat(navigator.backStack.value.size).isEqualTo(1)

        navController.navigate(
            R.id.empty_fragment,
            null,
        )
        fm?.executePendingTransactions()

        assertThat(navigator.backStack.value.size).isEqualTo(2)
        val originalBackStackEntry = navController.currentBackStackEntry!!
        val originalEntryViewModel = ViewModelProvider(originalBackStackEntry)[
            TestClearViewModel::class.java
        ]
        val originalFragment = fm?.findFragmentById(R.id.nav_host) as Fragment
        val originalFragmentViewModel = ViewModelProvider(originalFragment)[
            TestClearViewModel::class.java
        ]

        navController.navigate(
            R.id.empty_fragment_2,
            null,
            navOptions {
                popUpTo(R.id.empty_fragment) {
                    inclusive = true
                    saveState = true
                }
            }
        )
        fm.executePendingTransactions()

        val currentTopFragment = fm.findFragmentById(R.id.nav_host)

        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.empty_fragment_2)
        assertThat(navigator.backStack.value.size).isEqualTo(2)

        navController.clearBackStack(R.id.empty_fragment)
        fm.executePendingTransactions()
        // clearing the back stack does not change the current fragment
        assertThat(fm.findFragmentById(R.id.nav_host)).isEqualTo(currentTopFragment)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.empty_fragment_2)
        assertThat(navigator.backStack.value.size).isEqualTo(2)
        assertThat(originalFragmentViewModel.cleared).isTrue()
        assertThat(originalEntryViewModel.cleared).isTrue()
    }

    @Test
    fun dialogFragmentNavigate_singleTop() = withNavigationActivity {
        val navigator =
            navController.navigatorProvider.getNavigator(DialogFragmentNavigator::class.java)
        navController.navigate(R.id.testDialog_fragment)

        val fm = supportFragmentManager.findFragmentById(R.id.nav_host)?.childFragmentManager
        fm?.executePendingTransactions()

        assertThat(navController.currentBackStackEntry?.destination?.id)
            .isEqualTo(R.id.testDialog_fragment)
        val originalFragment = fm?.findFragmentByTag(navController.currentBackStackEntry?.id)
            as? TestDialogFragment
        assertThat(originalFragment!!.dialogs.first().isShowing).isTrue()

        // backStacks should be in sync
        assertThat(navigator.backStack.value.size).isEqualTo(1)
        assertThat(fm.fragments.size).isEqualTo(2) // start + dialog fragment

        // singleTop navigation
        navController.navigate(
            R.id.testDialog_fragment,
            null,
            NavOptions.Builder().setLaunchSingleTop(true).build()
        )
        assertThat(navController.currentBackStackEntry?.destination?.id)
            .isEqualTo(R.id.testDialog_fragment)
        fm.executePendingTransactions()

        assertThat(navController.currentBackStackEntry?.destination?.id)
            .isEqualTo(R.id.testDialog_fragment)
        val replacementFragment = fm.findFragmentByTag(navController.currentBackStackEntry?.id)
            as? TestDialogFragment
        // the first dialog should be dismissed
        assertThat(originalFragment.dialogs.first().isShowing).isFalse()
        assertThat(replacementFragment!!.dialogs.first().isShowing).isTrue()

        assertWithMessage("Replacement should be a new instance")
            .that(originalFragment)
            .isNotSameInstanceAs(replacementFragment)

        // backStacks should be in sync
        assertThat(navigator.backStack.value.size).isEqualTo(1)
        assertThat(fm.fragments.size).isEqualTo(2) // start + dialog fragment
    }

    @Test
    fun dialogFragmentNavigate_immediateSingleTop() = withNavigationActivity {
        val navigator =
            navController.navigatorProvider.getNavigator(DialogFragmentNavigator::class.java)

        // first navigation
        navController.navigate(R.id.testDialog_fragment)

        // immediate second navigation with singleTop without executing first transaction
        navController.navigate(
            R.id.testDialog_fragment,
            null,
            NavOptions.Builder().setLaunchSingleTop(true).build()
        )
        assertThat(navController.currentBackStackEntry?.destination?.id)
            .isEqualTo(R.id.testDialog_fragment)

        val fm = supportFragmentManager.findFragmentById(R.id.nav_host)?.childFragmentManager
        fm?.executePendingTransactions()

        assertThat(navController.currentBackStackEntry?.destination?.id)
            .isEqualTo(R.id.testDialog_fragment)
        val replacementFragment = fm?.findFragmentByTag(navController.currentBackStackEntry?.id)
            as? TestDialogFragment

        assertThat(replacementFragment!!.dialogs.first().isShowing).isTrue()

        // ensure original Fragment is dismissed and backStacks are in sync
        assertThat(navigator.backStack.value.size).isEqualTo(1)
        assertThat(fm.fragments.size).isEqualTo(2) // start + dialog fragment
    }

    @Test
    fun dialogFragmentNavigate_multiImmediateSingleTop() = withNavigationActivity {
        val navigator =
            navController.navigatorProvider.getNavigator(DialogFragmentNavigator::class.java)

        // first navigation
        navController.navigate(R.id.testDialog_fragment)

        // immediate second navigation with singleTop without executing first transaction
        navController.navigate(
            R.id.testDialog_fragment,
            null,
            NavOptions.Builder().setLaunchSingleTop(true).build()
        )

        // immediate third navigation with singleTop without executing previous transactions
        navController.navigate(
            R.id.testDialog_fragment,
            null,
            NavOptions.Builder().setLaunchSingleTop(true).build()
        )

        assertThat(navController.currentBackStackEntry?.destination?.id)
            .isEqualTo(R.id.testDialog_fragment)

        val fm = supportFragmentManager.findFragmentById(R.id.nav_host)?.childFragmentManager
        fm?.executePendingTransactions()

        assertThat(navController.currentBackStackEntry?.destination?.id)
            .isEqualTo(R.id.testDialog_fragment)
        val replacementFragment = fm?.findFragmentByTag(navController.currentBackStackEntry?.id)
            as? TestDialogFragment

        assertThat(replacementFragment!!.dialogs.first().isShowing).isTrue()

        // ensure previous fragments are dismissed and backStacks are in sync
        assertThat(navigator.backStack.value.size).isEqualTo(1)
        assertThat(fm.fragments.size).isEqualTo(2) // start + dialog fragment
    }

    @Test
    fun dialogFragmentNavigate_partiallyImmediateSingleTop() = withNavigationActivity {
        val navigator =
            navController.navigatorProvider.getNavigator(DialogFragmentNavigator::class.java)

        // first navigation
        navController.navigate(R.id.testDialog_fragment)

        val fm = supportFragmentManager.findFragmentById(R.id.nav_host)?.childFragmentManager
        fm?.executePendingTransactions()

        // second navigation with singleTop
        navController.navigate(
            R.id.testDialog_fragment,
            null,
            NavOptions.Builder().setLaunchSingleTop(true).build()
        )

        // immediate third navigation with singleTop without executing previous singleTop
        navController.navigate(
            R.id.testDialog_fragment,
            null,
            NavOptions.Builder().setLaunchSingleTop(true).build()
        )

        fm?.executePendingTransactions()

        assertThat(navController.currentBackStackEntry?.destination?.id)
            .isEqualTo(R.id.testDialog_fragment)
        val replacementFragment = fm?.findFragmentByTag(navController.currentBackStackEntry?.id)
            as? TestDialogFragment

        assertThat(replacementFragment!!.dialogs.first().isShowing).isTrue()

        // ensure previous fragments are dismissed and backStacks are in sync
        assertThat(navigator.backStack.value.size).isEqualTo(1)
        assertThat(fm.fragments.size).isEqualTo(2) // start + last dialog fragment
    }

    @Test
    fun testPopEntryInFragmentResumed() = withNavigationActivity {
        navController.graph = navController.createGraph("first") {
            fragment<EmptyFragment>("first")
            fragment<PopInOnResumeFragment>("second")
        }
        navController.navigate("second")

        val fm = supportFragmentManager.findFragmentById(R.id.nav_host)?.childFragmentManager
        fm?.executePendingTransactions()

        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo("first")
    }

    private fun withNavigationActivity(
        block: NavigationActivity.() -> Unit
    ) {
        with(ActivityScenario.launch(NavigationActivity::class.java)) {
            withActivity {
                this.block()
            }
        }
    }
}

class PopInOnResumeFragment : Fragment(R.layout.strict_view_fragment) {
    override fun onResume() {
        super.onResume()
        findNavController().navigate("first") {
            popUpTo("first")
        }
    }
}

class TestDialogFragment : DialogFragment() {
    val dialogs = mutableListOf<Dialog>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialogs.add(dialog)
        return dialog
    }
}

class TestClearViewModel : ViewModel() {
    var cleared = false

    override fun onCleared() {
        cleared = true
    }
}
