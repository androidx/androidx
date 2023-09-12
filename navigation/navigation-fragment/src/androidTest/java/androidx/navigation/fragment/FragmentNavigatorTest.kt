/*
 * Copyright 2018 The Android Open Source Project
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

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.StrictFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.test.EmptyFragment
import androidx.navigation.fragment.test.NavigationActivity
import androidx.navigation.fragment.test.R
import androidx.navigation.navOptions
import androidx.navigation.testing.TestNavigatorState
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.lang.IllegalArgumentException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class FragmentNavigatorTest {

    companion object {
        private const val INITIAL_FRAGMENT = 1
        private const val SECOND_FRAGMENT = 2
        private const val THIRD_FRAGMENT = 3
        private const val FOURTH_FRAGMENT = 4
        private const val TEST_LABEL = "test_label"
    }

    @Suppress("DEPRECATION")
    @get:Rule
    var activityRule = androidx.test.rule.ActivityTestRule(EmptyActivity::class.java)

    private lateinit var emptyActivity: EmptyActivity
    private lateinit var fragmentManager: FragmentManager
    private lateinit var navigatorState: TestNavigatorState
    private lateinit var fragmentNavigator: FragmentNavigator

    @Before
    fun setup() {
        emptyActivity = activityRule.activity
        fragmentManager = emptyActivity.supportFragmentManager
        navigatorState = TestNavigatorState()
        fragmentNavigator = FragmentNavigator(emptyActivity, fragmentManager, R.id.container)
        fragmentNavigator.onAttach(navigatorState)
    }

    @UiThreadTest
    @Test
    fun testNavigate() {
        val entry = createBackStackEntry()

        fragmentNavigator.navigate(listOf(entry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)
        fragmentManager.executePendingTransactions()
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment)
            .isNotNull()
        assertWithMessage("Fragment should be the correct type")
            .that(fragment)
            .isInstanceOf(EmptyFragment::class.java)
        assertWithMessage("Fragment should be the primary navigation Fragment")
            .that(fragmentManager.primaryNavigationFragment)
            .isSameInstanceAs(fragment)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithFragmentFactory() {
        fragmentManager.fragmentFactory = NonEmptyFragmentFactory()
        val entry = createBackStackEntry(clazz = NonEmptyConstructorFragment::class)

        fragmentNavigator.navigate(listOf(entry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)
        fragmentManager.executePendingTransactions()
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment)
            .isNotNull()
        assertWithMessage("Fragment should be the correct type")
            .that(fragment)
            .isInstanceOf(NonEmptyConstructorFragment::class.java)
        assertWithMessage("Fragment should be the primary navigation Fragment")
            .that(fragment)
            .isSameInstanceAs(fragmentManager.primaryNavigationFragment)
    }

    @UiThreadTest
    @Test
    fun testNavigateTwice() {
        val entry = createBackStackEntry()

        fragmentNavigator.navigate(listOf(entry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)
        fragmentManager.executePendingTransactions()
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment)
            .isNotNull()
        assertWithMessage("Fragment should be the correct type")
            .that(fragment)
            .isInstanceOf(EmptyFragment::class.java)
        assertWithMessage("Fragment should be the primary navigation Fragment")
            .that(fragmentManager.primaryNavigationFragment)
            .isSameInstanceAs(fragment)

        // Now push a second fragment
        val replacementEntry = createBackStackEntry(SECOND_FRAGMENT)
        fragmentNavigator.navigate(listOf(replacementEntry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry, replacementEntry).inOrder()
        fragmentManager.executePendingTransactions()
        val replacementFragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Replacement Fragment should be added")
            .that(replacementFragment)
            .isNotNull()
        assertWithMessage("Replacement Fragment should be the correct type")
            .that(replacementFragment)
            .isInstanceOf(EmptyFragment::class.java)
        assertWithMessage("Replacement Fragment should be the primary navigation Fragment")
            .that(fragmentManager.primaryNavigationFragment)
            .isSameInstanceAs(replacementFragment)
        assertThat(navigatorState.transitionsInProgress.value).isEmpty()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithPopUpToThenPop() {
        val entry = createBackStackEntry()

        // Push initial fragment
        fragmentNavigator.navigate(listOf(entry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)
        fragmentManager.executePendingTransactions()

        // Push a second fragment
        val secondEntry = createBackStackEntry(SECOND_FRAGMENT)
        fragmentNavigator.navigate(listOf(secondEntry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry, secondEntry).inOrder()
        fragmentManager.executePendingTransactions()

        // Pop and then push third fragment, simulating popUpTo to initial.
        fragmentNavigator.popBackStack(secondEntry, false)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)
        val thirdEntry = createBackStackEntry(THIRD_FRAGMENT)
        fragmentNavigator.navigate(listOf(thirdEntry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry, thirdEntry).inOrder()
        fragmentManager.executePendingTransactions()

        // Now pop the Fragment
        fragmentNavigator.popBackStack(thirdEntry, false)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)
    }

    @UiThreadTest
    @Test
    fun testSingleTopInitial() {
        val entry = createBackStackEntry()

        fragmentNavigator.navigate(listOf(entry), null, null)
        fragmentManager.executePendingTransactions()
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment)
            .isNotNull()
        val lifecycle = fragment!!.lifecycle

        fragmentNavigator.onLaunchSingleTop(entry)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)
        fragmentManager.executePendingTransactions()
        val replacementFragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Replacement Fragment should be added")
            .that(replacementFragment)
            .isNotNull()
        assertWithMessage("Replacement Fragment should be the correct type")
            .that(replacementFragment)
            .isInstanceOf(EmptyFragment::class.java)
        assertWithMessage("Replacement Fragment should be the primary navigation Fragment")
            .that(fragmentManager.primaryNavigationFragment)
            .isSameInstanceAs(replacementFragment)
        assertWithMessage("Replacement should be a new instance")
            .that(replacementFragment)
            .isNotSameInstanceAs(fragment)
        assertWithMessage("Old instance should be destroyed")
            .that(lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)
    }

    @UiThreadTest
    @Test
    fun testSingleTop() {
        val entry = createBackStackEntry()

        // First push an initial Fragment
        fragmentNavigator.navigate(listOf(entry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)
        fragmentManager.executePendingTransactions()
        val initialFragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Initial Fragment should be added")
            .that(initialFragment)
            .isNotNull()

        // Now push the Fragment that we want to replace with a singleTop operation
        val replacementEntry = createBackStackEntry(SECOND_FRAGMENT)
        fragmentNavigator.navigate(listOf(replacementEntry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry, replacementEntry).inOrder()
        fragmentManager.executePendingTransactions()
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment)
            .isNotNull()
        val lifecycle = fragment!!.lifecycle

        fragmentNavigator.onLaunchSingleTop(replacementEntry)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry, replacementEntry).inOrder()
        fragmentManager.executePendingTransactions()
        val replacementFragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Replacement Fragment should be added")
            .that(replacementFragment)
            .isNotNull()
        assertWithMessage("Replacement Fragment should be the correct type")
            .that(replacementFragment)
            .isInstanceOf(EmptyFragment::class.java)
        assertWithMessage("Replacement Fragment should be the primary navigation Fragment")
            .that(fragmentManager.primaryNavigationFragment)
            .isSameInstanceAs(replacementFragment)
        assertWithMessage("Replacement should be a new instance")
            .that(replacementFragment)
            .isNotSameInstanceAs(fragment)
        assertWithMessage("Old instance should be destroyed")
            .that(lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)

        fragmentNavigator.popBackStack(replacementEntry, false)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)

        fragmentManager.executePendingTransactions()
        assertWithMessage("Initial Fragment should be on top of back stack after pop")
            .that(fragmentManager.findFragmentById(R.id.container))
            .isSameInstanceAs(initialFragment)
        assertWithMessage("Initial Fragment should be the primary navigation Fragment")
            .that(fragmentManager.primaryNavigationFragment)
            .isSameInstanceAs(initialFragment)
    }

    @UiThreadTest
    @Test
    fun testPopInitial() {
        val entry = createBackStackEntry()

        // First push an initial Fragment
        fragmentNavigator.navigate(listOf(entry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)

        // Now pop the initial Fragment
        fragmentNavigator.popBackStack(entry, false)
        assertThat(navigatorState.backStack.value)
            .isEmpty()
    }

    @UiThreadTest
    @Test
    fun testPop() {
        val entry = createBackStackEntry()

        // First push an initial Fragment
        fragmentNavigator.navigate(listOf(entry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)
        fragmentManager.executePendingTransactions()
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment)
            .isNotNull()

        // Now push the Fragment that we want to pop
        val replacementEntry = createBackStackEntry(SECOND_FRAGMENT)
        fragmentNavigator.navigate(listOf(replacementEntry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry, replacementEntry).inOrder()
        fragmentManager.executePendingTransactions()
        val replacementFragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Replacement Fragment should be added")
            .that(replacementFragment)
            .isNotNull()
        assertWithMessage("Replacement Fragment should be the correct type")
            .that(replacementFragment)
            .isInstanceOf(EmptyFragment::class.java)
        assertWithMessage("Replacement Fragment should be the primary navigation Fragment")
            .that(fragmentManager.primaryNavigationFragment)
            .isSameInstanceAs(replacementFragment)

        // Now pop the Fragment
        fragmentNavigator.popBackStack(replacementEntry, false)
        fragmentManager.executePendingTransactions()
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)
        assertWithMessage("Fragment should be the primary navigation Fragment after pop")
            .that(fragmentManager.primaryNavigationFragment)
            .isSameInstanceAs(fragment)
        assertThat(navigatorState.transitionsInProgress.value).isEmpty()
    }

    @UiThreadTest
    @Test
    fun testPopWithSameDestinationTwice() {
        val entry = createBackStackEntry()

        // First push an initial Fragment
        fragmentNavigator.navigate(listOf(entry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)
        fragmentManager.executePendingTransactions()
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment)
            .isNotNull()

        // Now push a second Fragment
        val secondEntry = createBackStackEntry(SECOND_FRAGMENT)
        fragmentNavigator.navigate(listOf(secondEntry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry, secondEntry).inOrder()
        fragmentManager.executePendingTransactions()
        val replacementFragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Replacement Fragment should be added")
            .that(replacementFragment)
            .isNotNull()
        assertWithMessage("Replacement Fragment should be the primary navigation Fragment")
            .that(fragmentManager.primaryNavigationFragment)
            .isSameInstanceAs(replacementFragment)

        // Push the same Fragment a second time, creating a stack of two
        // identical Fragments
        val replacementEntry = createBackStackEntry(SECOND_FRAGMENT)
        fragmentNavigator.navigate(listOf(replacementEntry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry, secondEntry, replacementEntry).inOrder()
        fragmentManager.executePendingTransactions()
        val fragmentToPop = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment to pop should be added")
            .that(fragmentToPop)
            .isNotNull()
        assertWithMessage("Fragment to pop should be the primary navigation Fragment")
            .that(fragmentManager.primaryNavigationFragment)
            .isSameInstanceAs(fragmentToPop)

        // Now pop the Fragment
        fragmentNavigator.popBackStack(replacementEntry, false)
        fragmentManager.executePendingTransactions()
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry, secondEntry).inOrder()
        assertWithMessage(
            "Replacement Fragment should be the primary navigation Fragment " +
                "after pop"
        )
            .that(fragmentManager.primaryNavigationFragment)
            .isSameInstanceAs(replacementFragment)
    }

    @UiThreadTest
    @Test
    fun testPopWithChildFragmentBackStack() {
        val entry = createBackStackEntry()

        // First push an initial Fragment
        fragmentNavigator.navigate(listOf(entry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)
        fragmentManager.executePendingTransactions()
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment)
            .isNotNull()

        // Now push the Fragment that we want to pop
        val replacementEntry = createBackStackEntry(SECOND_FRAGMENT)
        fragmentNavigator.navigate(listOf(replacementEntry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry, replacementEntry).inOrder()
        fragmentManager.executePendingTransactions()
        val replacementFragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Replacement Fragment should be added")
            .that(replacementFragment)
            .isNotNull()
        assertWithMessage("Replacement Fragment should be the correct type")
            .that(replacementFragment)
            .isInstanceOf(EmptyFragment::class.java)
        assertWithMessage("Replacement Fragment should be the primary navigation Fragment")
            .that(fragmentManager.primaryNavigationFragment)
            .isSameInstanceAs(replacementFragment)

        // Add a Fragment to the replacementFragment's childFragmentManager back stack
        replacementFragment?.childFragmentManager?.run {
            beginTransaction()
                .add(EmptyFragment(), "child")
                .addToBackStack(null)
                .commit()
            executePendingTransactions()
        }

        // Now pop the Fragment
        fragmentNavigator.popBackStack(replacementEntry, false)
        fragmentManager.executePendingTransactions()
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)
        assertWithMessage("Fragment should be the primary navigation Fragment after pop")
            .that(fragmentManager.primaryNavigationFragment)
            .isSameInstanceAs(fragment)
    }

    @UiThreadTest
    @Test
    fun testDeepLinkPop() {
        val entry = createBackStackEntry()
        val secondEntry = createBackStackEntry(SECOND_FRAGMENT)

        // First push two Fragments as our 'deep link'
        fragmentNavigator.navigate(listOf(entry, secondEntry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry, secondEntry)

        // Now push the Fragment that we want to pop
        val thirdEntry = createBackStackEntry(THIRD_FRAGMENT)
        fragmentNavigator.navigate(listOf(thirdEntry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry, secondEntry, thirdEntry)
        fragmentManager.executePendingTransactions()
        val replacementFragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Replacement Fragment should be added")
            .that(replacementFragment)
            .isNotNull()
        assertWithMessage("Replacement Fragment should be the correct type")
            .that(replacementFragment)
            .isInstanceOf(EmptyFragment::class.java)
        assertWithMessage("Replacement Fragment should be the primary navigation Fragment")
            .that(fragmentManager.primaryNavigationFragment)
            .isSameInstanceAs(replacementFragment)

        // Now pop the Fragment
        fragmentNavigator.popBackStack(thirdEntry, false)
        fragmentManager.executePendingTransactions()
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be the primary navigation Fragment after pop")
            .that(fragmentManager.primaryNavigationFragment)
            .isSameInstanceAs(fragment)
    }

    @UiThreadTest
    @Test
    fun testMultipleNavigateFragmentTransactionsThenPop() {
        val entry = createBackStackEntry()
        val secondEntry = createBackStackEntry(SECOND_FRAGMENT, clazz = Fragment::class)
        val thirdEntry = createBackStackEntry(THIRD_FRAGMENT)

        // Push 3 fragments without executing pending transactions.
        fragmentNavigator.navigate(listOf(entry, secondEntry, thirdEntry), null, null)

        // Now pop the Fragment
        fragmentNavigator.popBackStack(thirdEntry, false)
        fragmentManager.executePendingTransactions()
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry, secondEntry)
        // We should ensure the fragment manager is on the proper fragment at the end
        assertWithMessage("FragmentManager back stack should have only SECOND_FRAGMENT")
            .that(fragmentManager.backStackEntryCount)
            .isEqualTo(1)
        assertWithMessage("PrimaryFragment should be the correct type")
            .that(fragmentManager.primaryNavigationFragment)
            .isNotInstanceOf(EmptyFragment::class.java)
        assertThat(navigatorState.transitionsInProgress.value).isEmpty()
    }

    @UiThreadTest
    @Test
    fun testMultipleNavigateFragmentTransactionsThenPopMultiple() {
        val entry = createBackStackEntry()
        val secondEntry = createBackStackEntry(SECOND_FRAGMENT, clazz = Fragment::class)
        val thirdEntry = createBackStackEntry(THIRD_FRAGMENT)

        // Push 3 fragments
        fragmentNavigator.navigate(listOf(entry, secondEntry, thirdEntry), null, null)
        fragmentManager.executePendingTransactions()

        // Now pop multiple fragments with savedState so that the secondEntry does not get
        // marked complete by clear viewModel
        fragmentNavigator.popBackStack(secondEntry, true)
        fragmentManager.executePendingTransactions()
        assertThat(navigatorState.backStack.value).containsExactly(entry)
        assertThat(navigatorState.transitionsInProgress.value).isEmpty()
    }

    @UiThreadTest
    @Test
    fun testMultiplePopFragmentTransactionsThenPop() {
        val entry = createBackStackEntry()
        val secondEntry = createBackStackEntry(SECOND_FRAGMENT)
        val thirdEntry = createBackStackEntry(THIRD_FRAGMENT)
        val fourthEntry = createBackStackEntry(FOURTH_FRAGMENT)

        // Push 4 fragments
        fragmentNavigator.navigate(
            listOf(entry, secondEntry, thirdEntry, fourthEntry),
            null, null
        )
        fragmentManager.executePendingTransactions()

        // Pop 2 fragments without executing pending transactions.
        fragmentNavigator.popBackStack(thirdEntry, false)

        fragmentNavigator.popBackStack(secondEntry, false)
        fragmentManager.executePendingTransactions()
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)
    }

    @UiThreadTest
    @Test
    fun testNavigationAndAddIndependentFragmentWithoutBackStack() {
        val entry = createBackStackEntry()

        fragmentNavigator.navigate(listOf(entry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)
        fragmentManager.executePendingTransactions()
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment)
            .isNotNull()
        assertWithMessage("Fragment should be the correct type")
            .that(fragment)
            .isInstanceOf(EmptyFragment::class.java)
        assertWithMessage("Fragment should be the primary navigation Fragment")
            .that(fragmentManager.primaryNavigationFragment)
            .isSameInstanceAs(fragment)

        val independentFragment = EmptyFragment()
        fragmentManager.beginTransaction()
            .replace(R.id.container, independentFragment)
            .commit()
        fragmentManager.executePendingTransactions()

        assertWithMessage("Independent fragment should be added")
            .that(fragmentManager.findFragmentById(R.id.container))
            .isEqualTo(independentFragment)
    }

    @UiThreadTest
    @Test
    fun testNavigateAndAddIndependentFragmentWithBackStack() {
        val entry = createBackStackEntry()

        fragmentNavigator.navigate(listOf(entry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)
        fragmentManager.executePendingTransactions()
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment)
            .isNotNull()
        assertWithMessage("Fragment should be the correct type")
            .that(fragment)
            .isInstanceOf(EmptyFragment::class.java)
        assertWithMessage("Fragment should be the primary navigation Fragment")
            .that(fragmentManager.primaryNavigationFragment)
            .isSameInstanceAs(fragment)

        val independentFragment = EmptyFragment()
        fragmentManager.beginTransaction()
            .replace(R.id.container, independentFragment)
            .addToBackStack(null)
            .commit()
        try {
            fragmentManager.executePendingTransactions()
        } catch (e: IllegalArgumentException) {
            assertWithMessage("adding a fragment to the back stack manually should fail")
                .that(e.message)
                .contains("The fragment " + independentFragment + " is unknown to the " +
                    "FragmentNavigator. Please use the navigate() function to add fragments to " +
                    "the FragmentNavigator managed FragmentManager.")
        }
    }

    @LargeTest
    @UiThreadTest
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    fun testEntryResumedWithAnimation() {
        val entry1 = createBackStackEntry()

        // use animation
        val options = navOptions {
            anim {
                enter = R.anim.fade_enter
                exit = R.anim.fade_exit
                popEnter = R.anim.fade_enter
                popExit = R.anim.fade_exit
            }
        }

        // navigate to first entry and verify it executed correctly
        fragmentNavigator.navigate(listOf(entry1), options, null)
        assertThat(navigatorState.backStack.value).containsExactly(entry1)
        fragmentManager.executePendingTransactions()

        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment)
            .isNotNull()

        assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        val entry2 = createBackStackEntry(SECOND_FRAGMENT)

        fragmentNavigator.navigate(listOf(entry2), options, null)
        fragmentManager.executePendingTransactions()

        assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        // assert entry received fragment lifecycle event to move it to resumed
        assertThat(entry2.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
    }

    @LargeTest
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    fun testEntryResumedWithAnimator() {
        val entry1 = createBackStackEntry()

        // use animator
        val options = navOptions {
            anim {
                enter = R.animator.fade_enter
                exit = R.animator.fade_exit
                popEnter = R.animator.fade_enter
                popExit = R.animator.fade_exit
            }
        }

        // navigate to first entry and verify it executed correctly
        activityRule.runOnUiThread {
            fragmentNavigator.navigate(listOf(entry1), options, null)
        }
        assertThat(navigatorState.backStack.value).containsExactly(entry1)
        activityRule.runOnUiThread {
            fragmentManager.executePendingTransactions()
        }
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment)
            .isNotNull()

        // assert states
        val countDownLatch = CountDownLatch(1)
        activityRule.runOnUiThread {
            fragment?.viewLifecycleOwner?.lifecycle?.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_RESUME) {
                        countDownLatch.countDown()
                    }
                }
            })
        }

        assertThat(countDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        // navigate to entry2
        val entry2 = createBackStackEntry(SECOND_FRAGMENT)
        activityRule.runOnUiThread {
            fragmentNavigator.navigate(listOf(entry2), options, null)
            fragmentManager.executePendingTransactions()
        }

        // assert states
        val fragment2 = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment2)
            .isNotNull()

        val countDownLatch2 = CountDownLatch(1)
        activityRule.runOnUiThread {
            fragment2?.viewLifecycleOwner?.lifecycle?.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_RESUME) {
                        countDownLatch2.countDown()
                    }
                }
            })
        }
        assertThat(countDownLatch2.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        // assert entry received fragment lifecycle event to move it to resumed
        assertThat(entry2.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
    }

    @UiThreadTest
    @Test
    fun testEntryResumedWhenRestoredState() {
        val entry = createBackStackEntry()

        // First push an initial Fragment
        fragmentNavigator.navigate(listOf(entry), null, null)
        assertThat(navigatorState.backStack.value).containsExactly(entry)
        fragmentManager.executePendingTransactions()
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment)
            .isNotNull()

        assertThat(entry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        // Now push the Fragment that we want to save
        val replacementEntry = createBackStackEntry(SECOND_FRAGMENT, SavedStateFragment::class)
        fragmentNavigator.navigate(listOf(replacementEntry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry, replacementEntry).inOrder()
        fragmentManager.executePendingTransactions()
        val replacementFragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Replacement Fragment should be added")
            .that(replacementFragment)
            .isNotNull()

        assertThat(entry.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(replacementEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        // Save some state into the replacement fragment
        (replacementFragment as SavedStateFragment).savedState = "test"

        // Now save the Fragment
        fragmentNavigator.popBackStack(replacementEntry, true)
        fragmentManager.executePendingTransactions()

        // Create a new FragmentNavigator, replacing the previous one
        val savedState = fragmentNavigator.onSaveState() as Bundle
        fragmentNavigator = FragmentNavigator(
            emptyActivity,
            fragmentManager, R.id.container
        )
        fragmentNavigator.onAttach(navigatorState)
        fragmentNavigator.onRestoreState(savedState)

        // And now restore the fragment
        val restoredEntry = navigatorState.restoreBackStackEntry(replacementEntry)
        fragmentNavigator.navigate(
            listOf(restoredEntry),
            NavOptions.Builder().setRestoreState(true).build(), null
        )
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry, restoredEntry).inOrder()
        fragmentManager.executePendingTransactions()

        assertThat(entry.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(restoredEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
    }

    @LargeTest
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    fun testEntryStatesWithAnimationAfterReconfiguration() {
        withUse(ActivityScenario.launch(NavigationActivity::class.java)) {
            val navController1 = withActivity { findNavController(R.id.nav_host) }
            val fragNavigator1 = navController1.navigatorProvider.getNavigator(
                FragmentNavigator::class.java
            )

            // navigated to startDestination -- assert states
            assertThat(fragNavigator1.backStack.value.size).isEqualTo(1)
            val entry1 = fragNavigator1.backStack.value[0]
            val fm1 = withActivity {
                supportFragmentManager.findFragmentById(R.id.nav_host)!!.childFragmentManager
                    .also { it.executePendingTransactions() }
            }

            val fragment1 = fm1.findFragmentByTag(entry1.id)
            assertWithMessage("Fragment should be added")
                .that(fragment1)
                .isNotNull()
            assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

            // use animation
            val options = navOptions {
                anim {
                    enter = R.anim.fade_enter
                    exit = R.anim.fade_exit
                    popEnter = R.anim.fade_enter
                    popExit = R.anim.fade_exit
                }
            }

            // navigate to second destination -- assert states
            onActivity {
                navController1.navigate(R.id.empty_fragment, null, options)
                fm1.executePendingTransactions()
            }
            assertThat(fragNavigator1.backStack.value.size).isEqualTo(2)
            val entry2 = fragNavigator1.backStack.value[1]
            val fragment2 = fm1.findFragmentByTag(entry2.id)
            assertWithMessage("Fragment should be added")
                .that(fragment2)
                .isNotNull()
            assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
            assertThat(entry2.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

            // recreate activity - imitate configuration change
            recreate()

            assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
            assertThat(entry2.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)

            // get restored components
            val fm2 = withActivity {
                supportFragmentManager.findFragmentById(R.id.nav_host)!!.childFragmentManager
                    .also { it.executePendingTransactions() }
            }
            val navController2 = withActivity { findNavController(R.id.nav_host) }
            val fragNavigator2 = navController2.navigatorProvider.getNavigator(
                FragmentNavigator::class.java
            )
            assertThat(fm2).isNotEqualTo(fm1)
            assertThat(navController2).isNotEqualTo(navController1)
            assertThat(fragNavigator2).isNotEqualTo(fragNavigator1)
            assertThat(fragNavigator2.backStack.value.size).isEqualTo(2)

            // check that entries are restored to correct states
            val entry1Restored = fragNavigator2.backStack.value[0]
            val entry2Restored = fragNavigator2.backStack.value[1]
            assertThat(entry1Restored.id).isEqualTo(entry1.id)
            assertThat(entry2Restored.id).isEqualTo(entry2.id)
            assertThat(entry1Restored.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
            assertThat(entry2Restored.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

            // check that fragments have been restored
            val fragment1Restored = fm2.findFragmentByTag(entry1.id)
            assertWithMessage("Fragment should be added")
                .that(fragment1Restored)
                .isNotNull()
            val fragment2Restored = fm2.findFragmentByTag(entry2.id)
            assertWithMessage("Fragment should be added")
                .that(fragment2Restored)
                .isNotNull()

            // attach ON_DESTROY listeners which should be triggered when we pop
            var entry2RestoredDestroyed = false
            val countDownLatch = CountDownLatch(1)
            onActivity {
                fragment2Restored?.viewLifecycleOwner?.lifecycle?.addObserver(
                    object : LifecycleEventObserver {
                        override fun onStateChanged(
                            source: LifecycleOwner,
                            event: Lifecycle.Event
                        ) {
                            if (event == Lifecycle.Event.ON_DESTROY) {
                                countDownLatch.countDown()
                            }
                        }
                    }
                )
                entry2Restored.lifecycle.addObserver(object : LifecycleEventObserver {
                    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                        if (event == Lifecycle.Event.ON_DESTROY) {
                            entry2RestoredDestroyed = true
                        }
                    }
                })
            }

            // pop backstack
            onActivity {
                navController2.popBackStack(entry2Restored.destination.id, true)
            }

            assertThat(countDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

            onActivity {
                fm2.executePendingTransactions()
            }

            // assert popped states
            assertThat(entry2RestoredDestroyed).isTrue()
            assertThat(entry1Restored.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
            assertThat(fragNavigator2.backStack.value).containsExactly(entry1Restored)
            // navController backstack is updated properly. Contains graph root entry & entry1
            assertThat(navController2.currentBackStack.value.size).isEqualTo(2)
            assertThat(navController2.currentBackStack.value.last()).isEqualTo(entry1Restored)
        }
    }

    @LargeTest
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    fun testEntryStatesWithAnimatorAfterReconfiguration() {
        withUse(ActivityScenario.launch(NavigationActivity::class.java)) {
            val navController1 = withActivity { findNavController(R.id.nav_host) }
            val fragNavigator1 = navController1.navigatorProvider.getNavigator(
                FragmentNavigator::class.java
            )

            // navigated to startDestination -- assert states
            assertThat(fragNavigator1.backStack.value.size).isEqualTo(1)
            val entry1 = fragNavigator1.backStack.value[0]
            val fm1 = withActivity {
                supportFragmentManager.findFragmentById(R.id.nav_host)!!.childFragmentManager
                    .also { it.executePendingTransactions() }
            }

            val fragment1 = fm1.findFragmentByTag(entry1.id)
            assertWithMessage("Fragment should be added")
                .that(fragment1)
                .isNotNull()
            assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

            // use animator
            val options = navOptions {
                anim {
                    enter = R.animator.fade_enter
                    exit = R.animator.fade_exit
                    popEnter = R.animator.fade_enter
                    popExit = R.animator.fade_exit
                }
            }

            // navigate to second destination -- assert states
            onActivity {
                navController1.navigate(R.id.animator_fragment, null, options)
                fm1.executePendingTransactions()
            }
            assertThat(fragNavigator1.backStack.value.size).isEqualTo(2)
            val entry2 = fragNavigator1.backStack.value[1]
            val fragment2 = fm1.findFragmentByTag(entry2.id) as AnimatorFragment

            assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
            if (fragment2.endLatch.count == 1L) {
                // Entry 2 should move back to STARTED if animating
                assertThat(entry2.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
            } else {
                // And to RESUMED if it finishes
                assertThat(entry2.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
            }

            // recreate activity - imitate configuration change
            recreate()

            assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
            assertThat(entry2.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)

            // get restored components
            val fm2 = withActivity {
                supportFragmentManager.findFragmentById(R.id.nav_host)!!.childFragmentManager
                    .also { it.executePendingTransactions() }
            }
            val navController2 = withActivity { findNavController(R.id.nav_host) }
            val fragNavigator2 = navController2.navigatorProvider.getNavigator(
                FragmentNavigator::class.java
            )
            assertThat(fm2).isNotEqualTo(fm1)
            assertThat(navController2).isNotEqualTo(navController1)
            assertThat(fragNavigator2).isNotEqualTo(fragNavigator1)
            assertThat(fragNavigator2.backStack.value.size).isEqualTo(2)

            // check that entries are restored to correct states
            val entry1Restored = fragNavigator2.backStack.value[0]
            val entry2Restored = fragNavigator2.backStack.value[1]
            assertThat(entry1Restored.id).isEqualTo(entry1.id)
            assertThat(entry2Restored.id).isEqualTo(entry2.id)
            assertThat(entry1Restored.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
            assertThat(entry2Restored.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

            // check that fragments have been restored
            val fragment1Restored = fm2.findFragmentByTag(entry1.id)
            assertWithMessage("Fragment should be added")
                .that(fragment1Restored)
                .isNotNull()
            val fragment2Restored = fm2.findFragmentByTag(entry2.id)
            assertWithMessage("Fragment should be added")
                .that(fragment2Restored)
                .isNotNull()

            // attach ON_DESTROY listeners which should be triggered when we pop
            var entry2RestoredDestroyed = false
            val countDownLatch = CountDownLatch(1)
            onActivity {
                fragment2Restored?.viewLifecycleOwner?.lifecycle?.addObserver(
                    object : LifecycleEventObserver {
                        override fun onStateChanged(
                            source: LifecycleOwner,
                            event: Lifecycle.Event
                        ) {
                            if (event == Lifecycle.Event.ON_DESTROY) {
                                countDownLatch.countDown()
                            }
                        }
                    }
                )
                entry2Restored.lifecycle.addObserver(object : LifecycleEventObserver {
                    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                        if (event == Lifecycle.Event.ON_DESTROY) {
                            entry2RestoredDestroyed = true
                        }
                    }
                })
            }

            // pop backstack
            onActivity {
                navController2.popBackStack(entry2Restored.destination.id, true)
            }

            assertThat(countDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

            onActivity {
                fm2.executePendingTransactions()
            }

            // assert popped states
            assertThat(entry2RestoredDestroyed).isTrue()
            assertThat(entry1Restored.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
            assertThat(fragNavigator2.backStack.value).containsExactly(entry1Restored)
            // navController backstack is updated properly. Contains graph root entry & entry1
            assertThat(navController2.currentBackStack.value.size).isEqualTo(2)
            assertThat(navController2.currentBackStack.value.last()).isEqualTo(entry1Restored)
        }
    }

    @UiThreadTest
    @Test
    fun testPopUpToDestroysIntermediateEntries() {
        val entry1 = createBackStackEntry()

        // navigate to first entry and verify it executed correctly
        fragmentNavigator.navigate(listOf(entry1), null, null)
        assertThat(navigatorState.backStack.value).containsExactly(entry1)
        fragmentManager.executePendingTransactions()

        assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        val entry2 = createBackStackEntry(SECOND_FRAGMENT)
        fragmentNavigator.navigate(listOf(entry2), null, null)
        fragmentManager.executePendingTransactions()

        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment)
            .isNotNull()

        assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        // assert entry received fragment lifecycle event to move it to resumed
        assertThat(entry2.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        val entry3 = createBackStackEntry(THIRD_FRAGMENT)
        fragmentNavigator.navigate(listOf(entry3), null, null)
        fragmentManager.executePendingTransactions()
        // assert states
        val fragment2 = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment2)
            .isNotNull()

        assertThat(entry2.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        // assert entry received fragment lifecycle event to move it to resumed
        assertThat(entry3.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        fragmentNavigator.popBackStack(entry2, false)
        fragmentManager.executePendingTransactions()

        assertThat(entry2.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(entry3.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
    }

    @Test
    fun testNavigatePopUpToGraphInterrupt() {
        withUse(ActivityScenario.launch(NavigationActivity::class.java)) {
            val navController1 = withActivity { findNavController(R.id.nav_host) }
            val fragNavigator1 = navController1.navigatorProvider.getNavigator(
                FragmentNavigator::class.java
            )

            // navigated to entry1
            assertThat(fragNavigator1.backStack.value.size).isEqualTo(1)
            val entry1 = fragNavigator1.backStack.value[0]
            val fm = withActivity {
                supportFragmentManager.findFragmentById(R.id.nav_host)!!.childFragmentManager
                    .also { it.executePendingTransactions() }
            }

            assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
            assertThat(fragNavigator1.backStack.value.size).isEqualTo(1)
            val fragment1 = fm.findFragmentById(R.id.nav_host)
            assertThat(fragment1).isNotNull()

            // setup pop options
            val popUpToOptions = NavOptions.Builder()
                .setPopUpTo((navController1.graph.id), false, false)
                .build()

            // navigate to entry2
            onActivity {
                navController1.navigate(R.id.empty_fragment, null, popUpToOptions)
            }

            assertThat(fragNavigator1.backStack.value.size).isEqualTo(1)
            val entry2 = fragNavigator1.backStack.value[0]
            assertThat(entry2.id).isNotEqualTo(entry1.id)

            // navigate to entry3 immediately
            onActivity {
                navController1.navigate(R.id.empty_fragment_2, null, popUpToOptions)
            }

            assertThat(fragNavigator1.backStack.value.size).isEqualTo(1)
            val entry3 = fragNavigator1.backStack.value[0]
            assertThat(entry3.id).isNotEqualTo(entry2.id)

            // execute operations
            onActivity { fm.executePendingTransactions() }

            val fragment2 = fm.findFragmentById(R.id.nav_host)
            assertThat(fragment2).isNotNull()
            assertThat(fragment2!!.tag).isEqualTo(entry3.id)

            assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
            assertThat(entry2.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
            assertThat(entry3.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

            assertThat(fragment1!!.lifecycle.currentState).isEqualTo(Lifecycle.State.INITIALIZED)
            assertThat(fragment2.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        }
    }

    @LargeTest
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    fun testNavigateNavigateInterrupt() {
        val entry1 = createBackStackEntry()
        var entry2Started = false
        var entry2Resumed = false
        val entry2 = createBackStackEntry(SECOND_FRAGMENT)
        // Add observer to entry to verify lifecycle events.
        activityRule.runOnUiThread {
            entry2.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_START) {
                        entry2Started = true
                    }
                    if (event == Lifecycle.Event.ON_RESUME) {
                        entry2Resumed = true
                    }
                }
            })
        }

        val entry3 = createBackStackEntry(THIRD_FRAGMENT)

        val options = navOptions {
            anim {
                enter = R.animator.fade_enter
                exit = R.animator.fade_exit
                popEnter = R.animator.fade_enter
                popExit = R.animator.fade_exit
            }
        }

        // navigate to first entry and verify it executed correctly
        activityRule.runOnUiThread {
            fragmentNavigator.navigate(listOf(entry1), options, null)
        }
        assertThat(navigatorState.backStack.value).containsExactly(entry1)
        activityRule.runOnUiThread {
            fragmentManager.executePendingTransactions()
        }
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment)
            .isNotNull()

        // navigate to both the second and third entry back to back.
        activityRule.runOnUiThread {
            fragmentNavigator.navigate(listOf(entry2), options, null)
            fragmentNavigator.navigate(listOf(entry3), options, null)
        }
        assertThat(navigatorState.backStack.value).containsExactly(entry1, entry2, entry3)
        activityRule.runOnUiThread {
            fragmentManager.executePendingTransactions()
        }
        val fragment3 = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment3)
            .isNotNull()

        // Verify that both entries on the back stack are in a CREATED state
        assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(entry2.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)

        // Add an observer to ensure that we don't attempt to verify the state until animations
        // are complete and the viewLifecycle has been RESUMED.
        val countDownLatch = CountDownLatch(1)
        activityRule.runOnUiThread {
            fragment3?.viewLifecycleOwner?.lifecycle?.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_RESUME) {
                        countDownLatch.countDown()
                    }
                }
            })
        }
        assertThat(countDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(entry3.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        // verify that the second entry made it to STARTED but not RESUMED since it was interrupted
        assertWithMessage("Entry2 should have been started").that(entry2Started).isTrue()
        assertWithMessage("Entry2 should never be resumed").that(entry2Resumed).isFalse()
    }

    @LargeTest
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    fun testNavigatePopInterrupt() {
        val entry1 = createBackStackEntry(clazz = AnimatorFragment::class)
        var entry1Stopped = false

        // Add observer to entry to verify lifecycle events.
        activityRule.runOnUiThread {
            entry1.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_STOP) {
                        entry1Stopped = true
                    }
                }
            })
        }

        val entry2 = createBackStackEntry(SECOND_FRAGMENT, clazz = AnimatorFragment::class)

        val options = navOptions {
            anim {
                enter = R.animator.fade_enter
                exit = R.animator.fade_exit
                popEnter = R.animator.fade_enter
                popExit = R.animator.fade_exit
            }
        }

        // navigate to first entry and verify it executed correctly
        activityRule.runOnUiThread {
            fragmentNavigator.navigate(listOf(entry1), options, null)
        }
        assertThat(navigatorState.backStack.value).containsExactly(entry1)
        activityRule.runOnUiThread {
            fragmentManager.executePendingTransactions()
        }
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment)
            .isNotNull()

        // navigate to the second entry and pop it back to back.
        activityRule.runOnUiThread {
            fragmentNavigator.navigate(listOf(entry2), options, null)
            fragmentManager.executePendingTransactions()
            fragmentNavigator.popBackStack(entry2, false)
        }
        assertThat(navigatorState.backStack.value).containsExactly(entry1)
        activityRule.runOnUiThread {
            fragmentManager.executePendingTransactions()
        }

        // Add an observer to ensure that we don't attempt to verify the state until animations
        // are complete and the viewLifecycle has been RESUMED.
        val viewCountDownLatch = CountDownLatch(1)
        val entryCountDownLatch = CountDownLatch(1)
        activityRule.runOnUiThread {
            fragment?.viewLifecycleOwner?.lifecycle?.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewCountDownLatch.countDown()
                    }
                }
            })
            entry1.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_RESUME) {
                        entryCountDownLatch.countDown()
                    }
                }
            })
        }
        assertThat(viewCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(entryCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        // Entry 1 should move back to RESUMED
        assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        // Entry 2 should be DESTROYED
        assertThat(entry2.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)

        // verify that the first entry made it down to CREATED
        assertWithMessage("Entry2 should have been stopped").that(entry1Stopped).isTrue()
    }

    @LargeTest
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    fun testNavigatePopInterruptSameFrame() {
        val entry1 = createBackStackEntry(clazz = AnimatorFragment::class)
        var entry1Stopped = false

        // Add observer to entry to verify lifecycle events.
        activityRule.runOnUiThread {
            entry1.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_STOP) {
                        entry1Stopped = true
                    }
                }
            })
        }

        val entry2 = createBackStackEntry(SECOND_FRAGMENT, clazz = AnimatorFragment::class)

        val options = navOptions {
            anim {
                enter = R.animator.fade_enter
                exit = R.animator.fade_exit
                popEnter = R.animator.fade_enter
                popExit = R.animator.fade_exit
            }
        }

        // navigate to first entry and verify it executed correctly
        activityRule.runOnUiThread {
            fragmentNavigator.navigate(listOf(entry1), options, null)
        }
        assertThat(navigatorState.backStack.value).containsExactly(entry1)
        activityRule.runOnUiThread {
            fragmentManager.executePendingTransactions()
        }
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment)
            .isNotNull()

        // navigate to the second entry and pop it back to back.
        activityRule.runOnUiThread {
            fragmentNavigator.navigate(listOf(entry2), options, null)
            fragmentNavigator.popBackStack(entry2, false)
        }
        assertThat(navigatorState.backStack.value).containsExactly(entry1)
        activityRule.runOnUiThread {
            fragmentManager.executePendingTransactions()
        }

        // Add an observer to ensure that we don't attempt to verify the state until animations
        // are complete and the viewLifecycle has been RESUMED.
        val viewCountDownLatch = CountDownLatch(1)
        val entryCountDownLatch = CountDownLatch(1)
        activityRule.runOnUiThread {
            fragment?.viewLifecycleOwner?.lifecycle?.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewCountDownLatch.countDown()
                    }
                }
            })
            entry1.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_RESUME) {
                        entryCountDownLatch.countDown()
                    }
                }
            })
        }

        assertThat(viewCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(entryCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        // Entry 1 should move back to RESUMED
        assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        // Entry 2 should be DESTROYED
        assertThat(entry2.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)

        // verify that the first entry made it down to CREATED
        assertWithMessage("Entry2 should have been stopped").that(entry1Stopped).isTrue()
    }

    @LargeTest
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    fun testPopPopInterrupt() {
        val entry1 = createBackStackEntry(clazz = AnimatorFragment::class)
        val entry2 = createBackStackEntry(SECOND_FRAGMENT, AnimatorFragment::class)
        val entry3 = createBackStackEntry(THIRD_FRAGMENT, AnimatorFragment::class)

        val options = navOptions {
            anim {
                enter = R.animator.fade_enter
                exit = R.animator.fade_exit
                popEnter = R.animator.fade_enter
                popExit = R.animator.fade_exit
            }
        }
        activityRule.runOnUiThread {
            // navigate to first entry and verify it executed correctly
            fragmentNavigator.navigate(listOf(entry1), options, null)
            assertThat(navigatorState.backStack.value).containsExactly(entry1)

            fragmentManager.executePendingTransactions()

            // navigate to both the second and third entry back to back.
            fragmentNavigator.navigate(listOf(entry2), options, null)
            fragmentNavigator.navigate(listOf(entry3), options, null)

            assertThat(navigatorState.backStack.value).containsExactly(entry1, entry2, entry3)

            fragmentManager.executePendingTransactions()
        }

        val fragment3 = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment3)
            .isNotNull()

        // Verify that both entries on the back stack are in a CREATED state
        assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(entry2.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)

        // Add an observer to ensure that we don't attempt to verify the state until animations
        // are complete and the viewLifecycle has been RESUMED.
        val countDownLatch1 = CountDownLatch(1)
        activityRule.runOnUiThread {
            fragment3?.viewLifecycleOwner?.lifecycle?.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_RESUME) {
                        countDownLatch1.countDown()
                    }
                }
            })
        }
        assertThat(countDownLatch1.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        // Entry 3 should be RESUMED
        assertThat(entry3.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        var entry2Started = false

        // Add observer to entry to verify lifecycle events.
        activityRule.runOnUiThread {
            entry2.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_START) {
                        entry2Started = true
                    }
                }
            })
        }

        fragmentNavigator.popBackStack(entry3, false)
        fragmentNavigator.popBackStack(entry2, false)
        assertThat(navigatorState.backStack.value).containsExactly(entry1)
        activityRule.runOnUiThread {
            fragmentManager.executePendingTransactions()
        }
        val fragment1 = fragmentManager.findFragmentById(R.id.container) as AnimatorFragment
        assertWithMessage("Fragment should be added")
            .that(fragment1)
            .isNotNull()

        // middle of transition

        if (fragment1.endLatch.count == 1L) {
            // Entry 1 should move back to STARTED while animating
            assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        } else {
            assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
            assertThat(entry2.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
            assertThat(entry3.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        }

        // Add an observer to ensure that we don't attempt to verify the state until animations
        // are complete and the viewLifecycle has been RESUMED.
        val countDownLatch2 = CountDownLatch(1)
        activityRule.runOnUiThread {
            fragment1.viewLifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_RESUME) {
                        countDownLatch2.countDown()
                    }
                }
            })
        }
        assertThat(countDownLatch2.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        // Entry 1 should move back to RESUMED
        assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        // Entry 2 and 3 should be DESTROYED
        assertThat(entry2.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(entry3.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)

        // verify that the second entry moved to started
        assertWithMessage("Entry2 should have been started").that(entry2Started).isTrue()
    }

    @LargeTest
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Suppress("DEPRECATION")
    fun testSystemBackPress() {
        val entry1 = createBackStackEntry()
        val entry2 = createBackStackEntry(SECOND_FRAGMENT)

        val options = navOptions {
            anim {
                enter = R.animator.fade_enter
                exit = R.animator.fade_exit
                popEnter = R.animator.fade_enter
                popExit = R.animator.fade_exit
            }
        }

        // navigate to first entry and verify it executed correctly
        activityRule.runOnUiThread {
            fragmentNavigator.navigate(listOf(entry1), options, null)
        }
        assertThat(fragmentNavigator.backStack.value).containsExactly(entry1)
        assertThat(navigatorState.backStack.value).containsExactly(entry1)
        activityRule.runOnUiThread {
            fragmentManager.executePendingTransactions()
        }

        // navigate to the second entry
        activityRule.runOnUiThread {
            fragmentNavigator.navigate(listOf(entry2), options, null)
            fragmentManager.executePendingTransactions()
        }
        assertThat(navigatorState.backStack.value).containsExactlyElementsIn(listOf(entry1, entry2))
        assertThat(fragmentNavigator.backStack.value).containsExactlyElementsIn(
            listOf(entry1, entry2)
        ).inOrder()
        val fragment2 = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment2)
            .isNotNull()

        val countDownLatch2 = CountDownLatch(1)
        activityRule.runOnUiThread {
            fragment2?.viewLifecycleOwner?.lifecycle?.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_RESUME) {
                        countDownLatch2.countDown()
                    }
                }
            })
        }
        assertThat(countDownLatch2.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(entry2.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        // system back press
        activityRule.runOnUiThread {
            emptyActivity.onBackPressed()
        }

        val countDownLatch3 = CountDownLatch(1)
        activityRule.runOnUiThread {
            entry1.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_RESUME) {
                        // wait for animator to finish
                        countDownLatch3.countDown()
                    }
                }
            })
        }
        assertThat(countDownLatch3.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        // assert exit from entry2 and enter entry1
        assertThat(fragmentNavigator.backStack.value).containsExactly(entry1)
        assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(entry2.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @LargeTest
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Suppress("DEPRECATION")
    fun testSystemBackPress_multiEntries() {
        val entry1 = createBackStackEntry()
        val entry2 = createBackStackEntry(SECOND_FRAGMENT)
        val entry3 = createBackStackEntry(THIRD_FRAGMENT)

        val options = navOptions {
            anim {
                enter = R.animator.fade_enter
                exit = R.animator.fade_exit
                popEnter = R.animator.fade_enter
                popExit = R.animator.fade_exit
            }
        }

        // navigate to first entry and verify it executed correctly
        activityRule.runOnUiThread {
            fragmentNavigator.navigate(listOf(entry1), options, null)
        }
        assertThat(fragmentNavigator.backStack.value).containsExactly(entry1)
        assertThat(navigatorState.backStack.value).containsExactly(entry1)
        activityRule.runOnUiThread {
            fragmentManager.executePendingTransactions()
        }

        // navigate to the second entry
        activityRule.runOnUiThread {
            fragmentNavigator.navigate(listOf(entry2), options, null)
            fragmentManager.executePendingTransactions()
        }
        assertThat(navigatorState.backStack.value).containsExactlyElementsIn(listOf(entry1, entry2))
        assertThat(fragmentNavigator.backStack.value).containsExactlyElementsIn(
            listOf(entry1, entry2)
        ).inOrder()
        var fragment2 = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment2)
            .isNotNull()

        var countDownLatch2 = CountDownLatch(1)
        activityRule.runOnUiThread {
            fragment2?.viewLifecycleOwner?.lifecycle?.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_RESUME) {
                        countDownLatch2.countDown()
                    }
                }
            })
        }
        assertThat(countDownLatch2.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(entry2.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        // navigate to third entry
        activityRule.runOnUiThread {
            fragmentNavigator.navigate(listOf(entry3), options, null)
            fragmentManager.executePendingTransactions()
        }
        assertThat(navigatorState.backStack.value).containsExactlyElementsIn(
            listOf(entry1, entry2, entry3)
        )
        assertThat(fragmentNavigator.backStack.value).containsExactlyElementsIn(
            listOf(entry1, entry2, entry3)
        ).inOrder()
        val fragment3 = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment3)
            .isNotNull()

        val countDownLatch3 = CountDownLatch(1)
        activityRule.runOnUiThread {
            fragment3?.viewLifecycleOwner?.lifecycle?.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_RESUME) {
                        countDownLatch3.countDown()
                    }
                }
            })
        }
        assertThat(countDownLatch3.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(entry2.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(entry3.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        // system back press
        activityRule.runOnUiThread {
            emptyActivity.onBackPressed()
        }

        fragment2 = fragmentManager.findFragmentById(R.id.container)

        countDownLatch2 = CountDownLatch(1)
        activityRule.runOnUiThread {
            fragment2?.viewLifecycleOwner?.lifecycle?.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_RESUME) {
                        countDownLatch2.countDown()
                    }
                }
            })
        }
        assertThat(countDownLatch2.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        // exit from entry3, enter entry2
        assertThat(navigatorState.backStack.value).containsExactlyElementsIn(
            listOf(entry1, entry2)
        )
        assertThat(fragmentNavigator.backStack.value).containsExactlyElementsIn(
            listOf(entry1, entry2)
        )
        assertThat(entry1.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(entry2.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(entry3.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @UiThreadTest
    @Test
    fun testSaveRestoreState() {
        val entry = createBackStackEntry()

        // First push an initial Fragment
        fragmentNavigator.navigate(listOf(entry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)
        fragmentManager.executePendingTransactions()
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment)
            .isNotNull()

        // Now push the Fragment that we want to save
        val replacementEntry = createBackStackEntry(SECOND_FRAGMENT, SavedStateFragment::class)
        fragmentNavigator.navigate(listOf(replacementEntry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry, replacementEntry).inOrder()
        fragmentManager.executePendingTransactions()
        val replacementFragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Replacement Fragment should be added")
            .that(replacementFragment)
            .isNotNull()
        assertWithMessage("Replacement Fragment should be the correct type")
            .that(replacementFragment)
            .isInstanceOf(SavedStateFragment::class.java)
        assertWithMessage("Replacement Fragment should be the primary navigation Fragment")
            .that(fragmentManager.primaryNavigationFragment)
            .isSameInstanceAs(replacementFragment)

        // Save some state into the replacement fragment
        (replacementFragment as SavedStateFragment).savedState = "test"

        // Now save the Fragment
        fragmentNavigator.popBackStack(replacementEntry, true)
        fragmentManager.executePendingTransactions()
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)
        assertWithMessage("Fragment should be the primary navigation Fragment after pop")
            .that(fragmentManager.primaryNavigationFragment)
            .isSameInstanceAs(fragment)

        // And now restore the fragment
        val restoredEntry = navigatorState.restoreBackStackEntry(replacementEntry)
        fragmentNavigator.navigate(
            listOf(restoredEntry),
            NavOptions.Builder().setRestoreState(true).build(), null
        )
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry, restoredEntry).inOrder()
        fragmentManager.executePendingTransactions()
        val restoredFragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Restored Fragment should be added")
            .that(restoredFragment)
            .isNotNull()
        assertWithMessage("Restored Fragment should be the correct type")
            .that(restoredFragment)
            .isInstanceOf(SavedStateFragment::class.java)
        assertWithMessage("Restored Fragment should be the primary navigation Fragment")
            .that(fragmentManager.primaryNavigationFragment)
            .isSameInstanceAs(restoredFragment)

        assertWithMessage("Restored Fragment should have its state restored")
            .that((restoredFragment as SavedStateFragment).savedState)
            .isEqualTo("test")
    }

    @UiThreadTest
    @Test
    fun testSaveRestoreStateAfterSaveState() {
        val entry = createBackStackEntry()

        // First push an initial Fragment
        fragmentNavigator.navigate(listOf(entry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)
        fragmentManager.executePendingTransactions()
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Fragment should be added")
            .that(fragment)
            .isNotNull()

        // Now push the Fragment that we want to save
        val replacementEntry = createBackStackEntry(SECOND_FRAGMENT, SavedStateFragment::class)
        fragmentNavigator.navigate(listOf(replacementEntry), null, null)
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry, replacementEntry).inOrder()
        fragmentManager.executePendingTransactions()
        val replacementFragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Replacement Fragment should be added")
            .that(replacementFragment)
            .isNotNull()
        assertWithMessage("Replacement Fragment should be the correct type")
            .that(replacementFragment)
            .isInstanceOf(SavedStateFragment::class.java)
        assertWithMessage("Replacement Fragment should be the primary navigation Fragment")
            .that(fragmentManager.primaryNavigationFragment)
            .isSameInstanceAs(replacementFragment)

        // Save some state into the replacement fragment
        (replacementFragment as SavedStateFragment).savedState = "test"

        // Now save the Fragment
        fragmentNavigator.popBackStack(replacementEntry, true)
        fragmentManager.executePendingTransactions()
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry)
        assertWithMessage("Fragment should be the primary navigation Fragment after pop")
            .that(fragmentManager.primaryNavigationFragment)
            .isSameInstanceAs(fragment)

        // Create a new FragmentNavigator, replacing the previous one
        val savedState = fragmentNavigator.onSaveState() as Bundle
        fragmentNavigator = FragmentNavigator(
            emptyActivity,
            fragmentManager, R.id.container
        )
        fragmentNavigator.onAttach(navigatorState)
        fragmentNavigator.onRestoreState(savedState)

        // And now restore the fragment
        val restoredEntry = navigatorState.restoreBackStackEntry(replacementEntry)
        fragmentNavigator.navigate(
            listOf(restoredEntry),
            NavOptions.Builder().setRestoreState(true).build(), null
        )
        assertThat(navigatorState.backStack.value)
            .containsExactly(entry, restoredEntry).inOrder()
        fragmentManager.executePendingTransactions()
        val restoredFragment = fragmentManager.findFragmentById(R.id.container)
        assertWithMessage("Restored Fragment should be added")
            .that(restoredFragment)
            .isNotNull()
        assertWithMessage("Restored Fragment should be the correct type")
            .that(restoredFragment)
            .isInstanceOf(SavedStateFragment::class.java)
        assertWithMessage("Restored Fragment should be the primary navigation Fragment")
            .that(fragmentManager.primaryNavigationFragment)
            .isSameInstanceAs(restoredFragment)

        assertWithMessage("Restored Fragment should have its state restored")
            .that((restoredFragment as SavedStateFragment).savedState)
            .isEqualTo("test")
    }

    @Test
    fun testToString() {
        val destination = fragmentNavigator.createDestination().apply {
            id = INITIAL_FRAGMENT
            setClassName(EmptyFragment::class.java.name)
            label = TEST_LABEL
        }
        val expected = "Destination(0x${INITIAL_FRAGMENT.toString(16)}) label=test_label " +
            "class=${EmptyFragment::class.java.name}"
        assertThat(destination.toString()).isEqualTo(expected)
    }

    @Test
    fun testToStringNoClassName() {
        val destination = fragmentNavigator.createDestination().apply {
            id = INITIAL_FRAGMENT
            label = TEST_LABEL
        }
        val expected = "Destination(0x${INITIAL_FRAGMENT.toString(16)}) label=test_label " +
            "class=null"
        assertThat(destination.toString()).isEqualTo(expected)
    }

    private fun createBackStackEntry(
        destId: Int = INITIAL_FRAGMENT,
        clazz: KClass<out Fragment> = EmptyFragment::class
    ): NavBackStackEntry {
        val destination = fragmentNavigator.createDestination().apply {
            id = destId
            setClassName(clazz.java.name)
        }
        return navigatorState.createBackStackEntry(destination, null)
    }
}

class EmptyActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.empty_activity)
    }
}

class SavedStateFragment : Fragment() {
    var savedState: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedState = savedInstanceState?.getString("savedState")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("savedState", savedState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return FrameLayout(requireContext())
    }
}

class AnimatorFragment(@LayoutRes contentLayoutId: Int = R.layout.strict_view_fragment) :
    StrictFragment(contentLayoutId) {
    lateinit var endLatch: CountDownLatch

    override fun onCreateAnimator(
        transit: Int,
        enter: Boolean,
        nextAnim: Int
    ): Animator? {
        if (nextAnim == 0) {
            return null
        }

        val animator: Animator = try {
            AnimatorInflater.loadAnimator(context, nextAnim)
        } catch (_: Resources.NotFoundException) {
            null
        } ?: ValueAnimator.ofFloat(0f, 1f).setDuration(1)

        return animator.apply {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    endLatch.countDown()
                }
            })
            endLatch = CountDownLatch(1)
        }
    }
}

class NonEmptyConstructorFragment(val test: String) : Fragment()

class NonEmptyFragmentFactory : FragmentFactory() {
    override fun instantiate(
        classLoader: ClassLoader,
        className: String
    ) = if (className == NonEmptyConstructorFragment::class.java.name) {
        NonEmptyConstructorFragment("test")
    } else {
        super.instantiate(classLoader, className)
    }
}
