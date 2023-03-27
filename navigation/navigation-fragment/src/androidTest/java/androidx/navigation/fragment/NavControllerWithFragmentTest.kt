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
import androidx.fragment.app.DialogFragment
import android.os.Bundle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.test.NavigationActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import org.junit.Test
import org.junit.runner.RunWith
import androidx.navigation.fragment.test.R
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage

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

class TestDialogFragment : DialogFragment() {
    val dialogs = mutableListOf<Dialog>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialogs.add(dialog)
        return dialog
    }
}