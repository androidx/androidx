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

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavigatorState
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.reflect.KClass
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class DialogFragmentNavigatorTest {

    companion object {
        private const val INITIAL_FRAGMENT = 1
        private const val SECOND_FRAGMENT = 2
    }

    @Suppress("DEPRECATION")
    @get:Rule
    var activityRule = androidx.test.rule.ActivityTestRule(EmptyActivity::class.java)

    private lateinit var emptyActivity: EmptyActivity
    private lateinit var fragmentManager: FragmentManager
    private lateinit var navigatorState: TestNavigatorState
    private lateinit var dialogNavigator: DialogFragmentNavigator

    @Before
    fun setup() {
        emptyActivity = activityRule.activity
        fragmentManager = emptyActivity.supportFragmentManager
        navigatorState = TestNavigatorState()
        dialogNavigator = DialogFragmentNavigator(emptyActivity, fragmentManager)
        dialogNavigator.onAttach(navigatorState)
    }

    @UiThreadTest
    @Test
    fun testNavigate() {
        lateinit var dialogFragment: DialogFragment
        fragmentManager.fragmentFactory = object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                return super.instantiate(classLoader, className).also { fragment ->
                    if (fragment is DialogFragment) {
                        dialogFragment = fragment
                    }
                }
            }
        }
        val entry = createBackStackEntry()

        dialogNavigator.navigate(listOf(entry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)
        fragmentManager.executePendingTransactions()
        assertWithMessage("Dialog should be shown")
            .that(dialogFragment.requireDialog().isShowing)
            .isTrue()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithImmediatePop() {
        lateinit var dialogFragment: DialogFragment
        fragmentManager.fragmentFactory = object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                return super.instantiate(classLoader, className).also { fragment ->
                    if (fragment is DialogFragment) {
                        dialogFragment = fragment
                    }
                }
            }
        }
        val entry = createBackStackEntry()

        dialogNavigator.navigate(listOf(entry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)
        dialogNavigator.popBackStack(entry, false)
        fragmentManager.executePendingTransactions()
        assertThat(navigatorState.backStack.value)
            .isEmpty()

        assertWithMessage("Dialog should not be shown")
            .that(dialogFragment.dialog)
            .isNull()
    }

    @UiThreadTest
    @Test
    fun testFindNavController() {
        val dialogFragment = EmptyDialogFragment()
        // Fake using a NavHostFragment and instead just manually show the DialogFragment
        dialogFragment.showNow(fragmentManager, null)
        assertWithMessage("Dialog should be shown")
            .that(dialogFragment.requireDialog().isShowing)
            .isTrue()

        // And set a NavController on the root view
        val navController = NavController(emptyActivity)
        Navigation.setViewNavController(
            dialogFragment.requireDialog().window!!.decorView,
            navController
        )

        val returnedNavController = NavHostFragment.findNavController(dialogFragment)
        assertThat(returnedNavController)
            .isEqualTo(navController)
    }

    @UiThreadTest
    @Test
    fun testPop() {
        lateinit var dialogFragment: DialogFragment
        fragmentManager.fragmentFactory = object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                return super.instantiate(classLoader, className).also { fragment ->
                    if (fragment is DialogFragment) {
                        dialogFragment = fragment
                    }
                }
            }
        }
        val entry = createBackStackEntry()

        dialogNavigator.navigate(listOf(entry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)
        fragmentManager.executePendingTransactions()
        assertWithMessage("Dialog should be shown")
            .that(dialogFragment.requireDialog().isShowing)
            .isTrue()
        dialogNavigator.popBackStack(entry, false)
        fragmentManager.executePendingTransactions()
        assertWithMessage("DialogNavigator should pop dialog off the back stack")
            .that(navigatorState.backStack.value)
            .isEmpty()
        assertWithMessage("Dismiss should remove the dialog")
            .that(dialogFragment.dialog)
            .isNull()
        assertWithMessage("Dismissed DialogFragment should be removed from the FragmentManager")
            .that(fragmentManager.fragments)
            .doesNotContain(dialogFragment)
    }

    @UiThreadTest
    @Test
    fun testDismiss() {
        lateinit var dialogFragment: DialogFragment
        val entry = createBackStackEntry()
        var matchCount = 0

        fragmentManager.fragmentFactory = object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                return super.instantiate(classLoader, className).also { fragment ->
                    if (fragment is DialogFragment) {
                        dialogFragment = fragment
                        // observer before navigator to verify down states
                        dialogFragment.lifecycle.addObserver(object : LifecycleEventObserver {
                            override fun onStateChanged(
                                source: LifecycleOwner,
                                event: Lifecycle.Event
                            ) {
                                if (event == Lifecycle.Event.ON_STOP) {
                                    if (entry.lifecycle.currentState == Lifecycle.State.CREATED) {
                                        matchCount++
                                    }
                                }
                                if (event == Lifecycle.Event.ON_DESTROY) {
                                    if (entry.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                                        matchCount++
                                    }
                                    dialogFragment.lifecycle.removeObserver(this)
                                }
                            }
                        })
                    }
                }
            }
        }

        dialogNavigator.navigate(listOf(entry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)

        // observer after navigator to verify up states
        dialogFragment.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_START) {
                    if (entry.lifecycle.currentState == Lifecycle.State.STARTED) {
                        matchCount++
                    }
                }
                if (event == Lifecycle.Event.ON_RESUME) {
                    if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                        matchCount++
                    }
                }
                if (event == Lifecycle.Event.ON_DESTROY) {
                    dialogFragment.lifecycle.removeObserver(this)
                }
            }
        })

        fragmentManager.executePendingTransactions()
        assertWithMessage("Dialog should be shown")
            .that(dialogFragment.requireDialog().isShowing)
            .isTrue()

        dialogFragment.dismiss()
        fragmentManager.executePendingTransactions()
        assertThat(matchCount).isEqualTo(4)
        assertWithMessage("Dismiss should remove the dialog from the back stack")
            .that(navigatorState.backStack.value)
            .isEmpty()
        assertWithMessage("Dismiss should remove the dialog")
            .that(dialogFragment.dialog)
            .isNull()
        assertWithMessage("Dismissed DialogFragment should be removed from the FragmentManager")
            .that(fragmentManager.fragments)
            .doesNotContain(dialogFragment)
    }

    @UiThreadTest
    @Test
    fun testDismissAndNavigate() {
        val dialogFragments = mutableListOf<DialogFragment>()
        fragmentManager.fragmentFactory = object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                return super.instantiate(classLoader, className).also { fragment ->
                    if (fragment is DialogFragment) {
                        dialogFragments += fragment
                    }
                }
            }
        }
        val entry = createBackStackEntry()

        dialogNavigator.navigate(listOf(entry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)
        fragmentManager.executePendingTransactions()
        assertWithMessage("Dialog should be shown")
            .that(dialogFragments[0].requireDialog().isShowing)
            .isTrue()

        val secondEntry = createBackStackEntry(SECOND_FRAGMENT)

        // Call dismiss and, before executing pending transactions, call navigate()
        dialogFragments[0].dismiss()
        dialogNavigator.navigate(listOf(secondEntry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry, secondEntry).inOrder()
        fragmentManager.executePendingTransactions()
        assertWithMessage("Dismiss should remove the dialogs from the back stack")
            .that(navigatorState.backStack.value)
            .isEmpty()
        assertWithMessage("Dismiss should remove the dialog")
            .that(dialogFragments[0].dialog)
            .isNull()
        assertWithMessage("Dismissed DialogFragment should be removed from the FragmentManager")
            .that(fragmentManager.fragments)
            .doesNotContain(dialogFragments[0])
        assertWithMessage("Dismiss should remove the second dialog")
            .that(dialogFragments[1].dialog)
            .isNull()
        assertWithMessage("Second DialogFragment should be removed from the FragmentManager")
            .that(fragmentManager.fragments)
            .doesNotContain(dialogFragments[1])
    }

    @UiThreadTest
    @Test
    fun testPop_transitioningDialogStaysInTransition() {
        val dialogFragments = mutableListOf<DialogFragment>()
        fragmentManager.fragmentFactory = object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                return super.instantiate(classLoader, className).also { fragment ->
                    if (fragment is DialogFragment) {
                        dialogFragments += fragment
                    }
                }
            }
        }

        val firstEntry = createBackStackEntry()
        val secondEntry = createBackStackEntry(2)
        val thirdEntry = createBackStackEntry(3)

        dialogNavigator.navigate(listOf(firstEntry), null, null)
        dialogNavigator.navigate(listOf(secondEntry), null, null)
        dialogNavigator.navigate(listOf(thirdEntry), null, null)
        assertThat(navigatorState.backStack.value).containsExactly(
            firstEntry, secondEntry, thirdEntry
        ).inOrder()

        dialogNavigator.popBackStack(secondEntry, false)
        // should contain all entries as they have not moved to RESUMED state yet
        assertThat(navigatorState.transitionsInProgress.value).containsExactly(
            firstEntry, secondEntry, thirdEntry
        )
        fragmentManager.executePendingTransactions()
        assertThat(navigatorState.transitionsInProgress.value).isEmpty()
    }

    @UiThreadTest
    @Test
    fun testPop_nonTransitioningDialogMarkedComplete() {
        val dialogFragments = mutableListOf<DialogFragment>()
        fragmentManager.fragmentFactory = object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                return super.instantiate(classLoader, className).also { fragment ->
                    if (fragment is DialogFragment) {
                        dialogFragments += fragment
                    }
                }
            }
        }

        val firstEntry = createBackStackEntry()
        val secondEntry = createBackStackEntry(2)
        val thirdEntry = createBackStackEntry(3)

        dialogNavigator.navigate(listOf(firstEntry), null, null)
        dialogNavigator.navigate(listOf(secondEntry), null, null)
        dialogNavigator.navigate(listOf(thirdEntry), null, null)
        fragmentManager.executePendingTransactions()
        assertThat(navigatorState.transitionsInProgress.value).isEmpty()

        dialogNavigator.popBackStack(secondEntry, false)
        // firstEntry was moved to RESUMED so it is no longer transitioning. It should not
        // be in transition when dialog above it is getting popped.
        assertThat(navigatorState.transitionsInProgress.value).doesNotContain(firstEntry)
        fragmentManager.executePendingTransactions()
        assertThat(navigatorState.transitionsInProgress.value).isEmpty()
    }

    @UiThreadTest
    @Test
    fun testPush_transitioningDialogStaysInTransition() {
        val dialogFragments = mutableListOf<DialogFragment>()
        fragmentManager.fragmentFactory = object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                return super.instantiate(classLoader, className).also { fragment ->
                    if (fragment is DialogFragment) {
                        dialogFragments += fragment
                    }
                }
            }
        }

        val entry = createBackStackEntry()
        val secondEntry = createBackStackEntry(SECOND_FRAGMENT)

        dialogNavigator.navigate(listOf(entry), null, null)
        dialogNavigator.navigate(listOf(secondEntry), null, null)
        // Both entries have not reached RESUME and should be in transition
        assertThat(navigatorState.transitionsInProgress.value).containsExactly(
            entry, secondEntry
        ).inOrder()
        fragmentManager.executePendingTransactions()
        assertThat(navigatorState.transitionsInProgress.value).isEmpty()
    }

    @UiThreadTest
    @Test
    fun testPush_nonTransitioningDialogMarkedComplete() {
        val dialogFragments = mutableListOf<DialogFragment>()
        fragmentManager.fragmentFactory = object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                return super.instantiate(classLoader, className).also { fragment ->
                    if (fragment is DialogFragment) {
                        dialogFragments += fragment
                    }
                }
            }
        }

        val entry = createBackStackEntry()

        dialogNavigator.navigate(listOf(entry), null, null)
        fragmentManager.executePendingTransactions()
        assertThat(dialogFragments[0].requireDialog().isShowing).isTrue()
        assertThat(navigatorState.transitionsInProgress.value).isEmpty()

        val secondEntry = createBackStackEntry(SECOND_FRAGMENT)

        dialogNavigator.navigate(listOf(secondEntry), null, null)
        fragmentManager.executePendingTransactions()
        assertThat(dialogFragments[1].requireDialog().isShowing).isTrue()
        // ensure outgoing entry (first entry) is not transitioning
        assertThat(navigatorState.transitionsInProgress.value).isEmpty()
    }

    @UiThreadTest
    @Test
    fun testConsecutiveNavigateLifecycle() {
        val dialogFragments = mutableListOf<DialogFragment>()
        fragmentManager.fragmentFactory = object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                return super.instantiate(classLoader, className).also { fragment ->
                    if (fragment is DialogFragment) {
                        dialogFragments += fragment
                    }
                }
            }
        }

        val entry = createBackStackEntry()
        dialogNavigator.navigate(listOf(entry), null, null)
        fragmentManager.executePendingTransactions()

        assertThat(entry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        val secondEntry = createBackStackEntry(SECOND_FRAGMENT)
        dialogNavigator.navigate(listOf(secondEntry), null, null)
        fragmentManager.executePendingTransactions()

        assertThat(navigatorState.backStack.value).containsExactly(entry, secondEntry).inOrder()
        assertThat(entry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
    }

    @UiThreadTest
    @Test
    fun testConsecutiveNavigateThenPopLifecycle() {
        val dialogFragments = mutableListOf<DialogFragment>()
        fragmentManager.fragmentFactory = object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                return super.instantiate(classLoader, className).also { fragment ->
                    if (fragment is DialogFragment) {
                        dialogFragments += fragment
                    }
                }
            }
        }

        val entry = createBackStackEntry()
        val secondEntry = createBackStackEntry(SECOND_FRAGMENT)

        dialogNavigator.navigate(listOf(entry), null, null)
        dialogNavigator.navigate(listOf(secondEntry), null, null)
        fragmentManager.executePendingTransactions()

        assertThat(navigatorState.backStack.value).containsExactly(entry, secondEntry).inOrder()
        assertThat(entry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        // pop top dialog
        dialogNavigator.popBackStack(secondEntry, false)
        fragmentManager.executePendingTransactions()

        assertThat(navigatorState.backStack.value).containsExactly(entry)
        assertThat(entry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(dialogFragments[0].lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
    }

    private fun createBackStackEntry(
        destId: Int = INITIAL_FRAGMENT,
        clazz: KClass<out Fragment> = EmptyDialogFragment::class
    ): NavBackStackEntry {
        val destination = dialogNavigator.createDestination().apply {
            id = destId
            setClassName(clazz.java.name)
        }
        return navigatorState.createBackStackEntry(destination, null)
    }
}

class EmptyDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext()).create()
}
