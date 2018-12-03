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

import android.arch.lifecycle.Lifecycle
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.fragment.test.EmptyFragment
import androidx.navigation.fragment.test.R
import androidx.test.annotation.UiThreadTest
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

@SmallTest
@RunWith(AndroidJUnit4::class)
class FragmentNavigatorTest {

    companion object {
        private const val INITIAL_FRAGMENT = 1
        private const val SECOND_FRAGMENT = 2
        private const val THIRD_FRAGMENT = 3
        private const val FOURTH_FRAGMENT = 4
    }

    @get:Rule
    var activityRule = ActivityTestRule(EmptyActivity::class.java)

    private lateinit var emptyActivity: EmptyActivity
    private lateinit var fragmentManager: FragmentManager

    @Before
    fun setup() {
        emptyActivity = activityRule.activity
        fragmentManager = emptyActivity.supportFragmentManager
    }

    @UiThreadTest
    @Test
    fun testNavigate() {
        val fragmentNavigator = FragmentNavigator(emptyActivity, fragmentManager, R.id.container)
        val destination = fragmentNavigator.createDestination().apply {
            id = INITIAL_FRAGMENT
            className = EmptyFragment::class.java.name
        }

        assertThat(fragmentNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)
        fragmentManager.executePendingTransactions()
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertNotNull("Fragment should be added", fragment)
        assertEquals("Fragment should be the correct type",
                EmptyFragment::class.java, fragment!!::class.java)
        assertEquals("Fragment should be the primary navigation Fragment",
                fragment, fragmentManager.primaryNavigationFragment)
    }

    @UiThreadTest
    @Test
    fun testNavigateTwice() {
        val fragmentNavigator = FragmentNavigator(emptyActivity, fragmentManager, R.id.container)
        val destination = fragmentNavigator.createDestination().apply {
            id = INITIAL_FRAGMENT
            className = EmptyFragment::class.java.name
        }

        assertThat(fragmentNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)
        fragmentManager.executePendingTransactions()
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertNotNull("Fragment should be added", fragment)
        assertEquals("Fragment should be the correct type",
                EmptyFragment::class.java, fragment!!::class.java)
        assertEquals("Fragment should be the primary navigation Fragment",
                fragment, fragmentManager.primaryNavigationFragment)

        // Now push a second fragment
        destination.id = SECOND_FRAGMENT
        assertThat(fragmentNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)
        fragmentManager.executePendingTransactions()
        val replacementFragment = fragmentManager.findFragmentById(R.id.container)
        assertNotNull("Replacement Fragment should be added", replacementFragment)
        assertEquals("Replacement Fragment should be the correct type",
                EmptyFragment::class.java, replacementFragment!!::class.java)
        assertEquals("Replacement Fragment should be the primary navigation Fragment",
                replacementFragment, fragmentManager.primaryNavigationFragment)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithPopUpToThenPop() {
        val fragmentNavigator = FragmentNavigator(emptyActivity, fragmentManager, R.id.container)
        val destination = fragmentNavigator.createDestination()
        destination.id = INITIAL_FRAGMENT
        destination.className = EmptyFragment::class.java.name

        // Push initial fragment
        assertThat(fragmentNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)
        fragmentManager.executePendingTransactions()

        // Push a second fragment
        destination.id = SECOND_FRAGMENT
        assertThat(fragmentNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)
        fragmentManager.executePendingTransactions()

        // Pop and then push third fragment, simulating popUpTo to initial.
        val success = fragmentNavigator.popBackStack()
        assertTrue("FragmentNavigator should return true when popping the third fragment", success)
        destination.id = THIRD_FRAGMENT
        assertThat(fragmentNavigator.navigate(destination, null,
                NavOptions.Builder().setPopUpTo(INITIAL_FRAGMENT, false).build(), null))
            .isEqualTo(destination)
        fragmentManager.executePendingTransactions()

        // Now pop the Fragment
        val popped = fragmentNavigator.popBackStack()
        assertTrue("FragmentNavigator should return true when popping the third fragment", popped)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithPopUpToThenPopWithFragmentManager() {
        val fragmentNavigator = FragmentNavigator(emptyActivity, fragmentManager, R.id.container)
        val backPressListener = mock(Navigator.OnNavigatorBackPressListener::class.java)
        fragmentNavigator.addOnNavigatorBackPressListener(backPressListener)
        val destination = fragmentNavigator.createDestination()
        destination.id = INITIAL_FRAGMENT
        destination.className = EmptyFragment::class.java.name

        // Push initial fragment
        assertThat(fragmentNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)
        fragmentManager.executePendingTransactions()

        // Push a second fragment
        destination.id = SECOND_FRAGMENT
        assertThat(fragmentNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)
        fragmentManager.executePendingTransactions()

        // Pop and then push third fragment, simulating popUpTo to initial.
        val success = fragmentNavigator.popBackStack()
        assertWithMessage("FragmentNavigator should popBackStack successfully")
                .that(success)
                .isTrue()
        destination.id = THIRD_FRAGMENT
        assertThat(fragmentNavigator.navigate(destination, null,
                NavOptions.Builder().setPopUpTo(INITIAL_FRAGMENT, false).build(), null))
            .isEqualTo(destination)
        fragmentManager.executePendingTransactions()

        // Now pop the Fragment
        val popped = fragmentManager.popBackStackImmediate()
        assertTrue("FragmentNavigator should return true when popping the third fragment", popped)
        verify(backPressListener).onPopBackStack(fragmentNavigator)
        verifyNoMoreInteractions(backPressListener)
    }

    @UiThreadTest
    @Test
    fun testSingleTopInitial() {
        val fragmentNavigator = FragmentNavigator(emptyActivity, fragmentManager, R.id.container)
        val destination = fragmentNavigator.createDestination()
        destination.className = EmptyFragment::class.java.name

        fragmentNavigator.navigate(destination, null, null, null)
        fragmentManager.executePendingTransactions()
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertNotNull("Fragment should be added", fragment)

        assertThat(fragmentNavigator.navigate(destination, null,
                NavOptions.Builder().setLaunchSingleTop(true).build(), null))
            .isNull()
        fragmentManager.executePendingTransactions()
        val replacementFragment = fragmentManager.findFragmentById(R.id.container)
        assertNotNull("Replacement Fragment should be added", replacementFragment)
        assertTrue("Replacement Fragment should be the correct type",
                replacementFragment is EmptyFragment)
        assertEquals("Replacement Fragment should be the primary navigation Fragment",
                replacementFragment, fragmentManager.primaryNavigationFragment)
        assertNotEquals("Replacement should be a new instance", fragment,
                replacementFragment)
        assertEquals("Old instance should be destroyed", Lifecycle.State.DESTROYED,
                fragment!!.lifecycle.currentState)
    }

    @UiThreadTest
    @Test
    fun testSingleTop() {
        val fragmentNavigator = FragmentNavigator(emptyActivity, fragmentManager, R.id.container)
        val destination = fragmentNavigator.createDestination()
        destination.className = EmptyFragment::class.java.name

        // First push an initial Fragment
        assertThat(fragmentNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)

        // Now push the Fragment that we want to replace with a singleTop operation
        assertThat(fragmentNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)
        fragmentManager.executePendingTransactions()
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertNotNull("Fragment should be added", fragment)

        assertThat(fragmentNavigator.navigate(destination, null,
                NavOptions.Builder().setLaunchSingleTop(true).build(), null))
            .isNull()
        fragmentManager.executePendingTransactions()
        val replacementFragment = fragmentManager.findFragmentById(R.id.container)
        assertNotNull("Replacement Fragment should be added", replacementFragment)
        assertTrue("Replacement Fragment should be the correct type",
                replacementFragment is EmptyFragment)
        assertEquals("Replacement Fragment should be the primary navigation Fragment",
                replacementFragment, fragmentManager.primaryNavigationFragment)
        assertNotEquals("Replacement should be a new instance", fragment,
                replacementFragment)
        assertEquals("Old instance should be destroyed", Lifecycle.State.DESTROYED,
                fragment!!.lifecycle.currentState)
    }

    @UiThreadTest
    @Test
    fun testPopInitial() {
        val fragmentNavigator = FragmentNavigator(emptyActivity,
                fragmentManager, R.id.container)
        val destination = fragmentNavigator.createDestination()
        destination.id = INITIAL_FRAGMENT
        destination.className = EmptyFragment::class.java.name

        // First push an initial Fragment
        assertThat(fragmentNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)

        // Now pop the initial Fragment
        val popped = fragmentNavigator.popBackStack()
        assertFalse("FragmentNavigator should return false when popping the initial Fragment",
                popped)
    }

    @UiThreadTest
    @Test
    fun testPop() {
        val fragmentNavigator = FragmentNavigator(emptyActivity, fragmentManager, R.id.container)
        val destination = fragmentNavigator.createDestination()
        destination.id = INITIAL_FRAGMENT
        destination.className = EmptyFragment::class.java.name

        // First push an initial Fragment
        assertThat(fragmentNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)
        fragmentManager.executePendingTransactions()
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertNotNull("Fragment should be added", fragment)

        // Now push the Fragment that we want to pop
        destination.id = SECOND_FRAGMENT
        assertThat(fragmentNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)
        fragmentManager.executePendingTransactions()
        val replacementFragment = fragmentManager.findFragmentById(R.id.container)
        assertNotNull("Replacement Fragment should be added", replacementFragment)
        assertTrue("Replacement Fragment should be the correct type",
                replacementFragment is EmptyFragment)
        assertEquals("Replacement Fragment should be the primary navigation Fragment",
                replacementFragment, fragmentManager.primaryNavigationFragment)

        // Now pop the Fragment
        val popped = fragmentNavigator.popBackStack()
        fragmentManager.executePendingTransactions()
        assertTrue("FragmentNavigator should return true when popping a Fragment", popped)
        assertEquals("Fragment should be the primary navigation Fragment after pop",
                fragment, fragmentManager.primaryNavigationFragment)
    }

    @UiThreadTest
    @Test
    fun testPopWithFragmentManager() {
        val fragmentNavigator = FragmentNavigator(emptyActivity, fragmentManager, R.id.container)
        val backPressListener = mock(Navigator.OnNavigatorBackPressListener::class.java)
        fragmentNavigator.addOnNavigatorBackPressListener(backPressListener)
        val destination = fragmentNavigator.createDestination()
        destination.id = INITIAL_FRAGMENT
        destination.className = EmptyFragment::class.java.name

        // First push an initial Fragment
        assertThat(fragmentNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)
        fragmentManager.executePendingTransactions()
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertNotNull("Fragment should be added", fragment)

        // Now push the Fragment that we want to pop
        destination.id = SECOND_FRAGMENT
        assertThat(fragmentNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)
        fragmentManager.executePendingTransactions()
        val replacementFragment = fragmentManager.findFragmentById(R.id.container)
        assertNotNull("Replacement Fragment should be added", replacementFragment)
        assertTrue("Replacement Fragment should be the correct type",
                replacementFragment is EmptyFragment)
        assertEquals("Replacement Fragment should be the primary navigation Fragment",
                replacementFragment, fragmentManager.primaryNavigationFragment)

        // Now pop the Fragment
        fragmentManager.popBackStackImmediate()
        verify(backPressListener).onPopBackStack(fragmentNavigator)
        assertEquals("Fragment should be the primary navigation Fragment after pop",
                fragment, fragmentManager.primaryNavigationFragment)
        verifyNoMoreInteractions(backPressListener)
    }

    @UiThreadTest
    @Test
    fun testDeepLinkPopWithFragmentManager() {
        val fragmentNavigator = FragmentNavigator(emptyActivity, fragmentManager, R.id.container)
        val backPressListener = mock(Navigator.OnNavigatorBackPressListener::class.java)
        fragmentNavigator.addOnNavigatorBackPressListener(backPressListener)
        val destination = fragmentNavigator.createDestination()
        destination.id = INITIAL_FRAGMENT
        destination.className = EmptyFragment::class.java.name

        // First push two Fragments as our 'deep link'
        assertThat(fragmentNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)
        destination.id = SECOND_FRAGMENT
        assertThat(fragmentNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)

        // Now push the Fragment that we want to pop
        destination.id = THIRD_FRAGMENT
        assertThat(fragmentNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)
        fragmentManager.executePendingTransactions()
        val replacementFragment = fragmentManager.findFragmentById(R.id.container)
        assertNotNull("Replacement Fragment should be added", replacementFragment)
        assertTrue("Replacement Fragment should be the correct type",
                replacementFragment is EmptyFragment)
        assertEquals("Replacement Fragment should be the primary navigation Fragment",
                replacementFragment, fragmentManager.primaryNavigationFragment)

        // Now pop the Fragment
        fragmentManager.popBackStackImmediate()
        verify(backPressListener).onPopBackStack(fragmentNavigator)
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertEquals("Fragment should be the primary navigation Fragment after pop",
                fragment, fragmentManager.primaryNavigationFragment)
        verifyNoMoreInteractions(backPressListener)
    }

    @UiThreadTest
    @Test
    fun testDeepLinkPopWithFragmentManagerWithSaveState() {
        var fragmentNavigator = FragmentNavigator(emptyActivity,
                fragmentManager, R.id.container)
        val backPressListener = mock(Navigator.OnNavigatorBackPressListener::class.java)
        fragmentNavigator.addOnNavigatorBackPressListener(backPressListener)
        val destination = fragmentNavigator.createDestination()
        destination.id = INITIAL_FRAGMENT
        destination.className = EmptyFragment::class.java.name

        // First push two Fragments as our 'deep link'
        assertThat(fragmentNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)
        destination.id = SECOND_FRAGMENT
        assertThat(fragmentNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)

        // Now push the Fragment that we want to pop
        destination.id = THIRD_FRAGMENT
        assertThat(fragmentNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)
        fragmentManager.executePendingTransactions()
        val replacementFragment = fragmentManager.findFragmentById(R.id.container)
        assertNotNull("Replacement Fragment should be added", replacementFragment)
        assertTrue("Replacement Fragment should be the correct type",
                replacementFragment is EmptyFragment)
        assertEquals("Replacement Fragment should be the primary navigation Fragment",
                replacementFragment, fragmentManager.primaryNavigationFragment)

        // Create a new FragmentNavigator, replacing the previous one
        val savedState = fragmentNavigator.onSaveState()
        fragmentNavigator.removeOnNavigatorBackPressListener(backPressListener)
        fragmentNavigator = FragmentNavigator(emptyActivity,
                fragmentManager, R.id.container)
        fragmentNavigator.onRestoreState(savedState)
        fragmentNavigator.addOnNavigatorBackPressListener(backPressListener)

        // Now pop the Fragment
        fragmentManager.popBackStackImmediate()
        verify(backPressListener).onPopBackStack(fragmentNavigator)
        val fragment = fragmentManager.findFragmentById(R.id.container)
        assertEquals("Fragment should be the primary navigation Fragment after pop",
                fragment, fragmentManager.primaryNavigationFragment)
        verifyNoMoreInteractions(backPressListener)
    }

    @UiThreadTest
    @Test
    fun testNavigateThenPopAfterSaveState() {
        var fragmentNavigator = FragmentNavigator(emptyActivity,
                fragmentManager, R.id.container)
        val backPressListener = mock(Navigator.OnNavigatorBackPressListener::class.java)
        fragmentNavigator.addOnNavigatorBackPressListener(backPressListener)
        val destination = fragmentNavigator.createDestination()
        destination.id = INITIAL_FRAGMENT
        destination.className = EmptyFragment::class.java.name

        assertThat(fragmentNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)
        fragmentManager.executePendingTransactions()
        var fragment = fragmentManager.findFragmentById(R.id.container)
        assertNotNull("Fragment should be added", fragment)
        assertEquals("Fragment should be the correct type",
                EmptyFragment::class.java, fragment!!::class.java)
        assertEquals("Fragment should be the primary navigation Fragment",
                fragment, fragmentManager.primaryNavigationFragment)

        // Now push a second fragment
        destination.id = SECOND_FRAGMENT
        assertThat(fragmentNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)
        fragmentManager.executePendingTransactions()
        var replacementFragment = fragmentManager.findFragmentById(R.id.container)
        assertNotNull("Replacement Fragment should be added", replacementFragment)
        assertEquals("Replacement Fragment should be the correct type",
                EmptyFragment::class.java, replacementFragment!!::class.java)
        assertEquals("Replacement Fragment should be the primary navigation Fragment",
                replacementFragment, fragmentManager.primaryNavigationFragment)

        // Create a new FragmentNavigator, replacing the previous one
        val savedState = fragmentNavigator.onSaveState()
        fragmentNavigator.removeOnNavigatorBackPressListener(backPressListener)
        fragmentNavigator = FragmentNavigator(emptyActivity,
                fragmentManager, R.id.container)
        fragmentNavigator.onRestoreState(savedState)
        fragmentNavigator.addOnNavigatorBackPressListener(backPressListener)

        // Now push a third fragment after the state save
        destination.id = THIRD_FRAGMENT
        assertThat(fragmentNavigator.navigate(destination, null, null, null))
            .isEqualTo(destination)
        fragmentManager.executePendingTransactions()
        replacementFragment = fragmentManager.findFragmentById(R.id.container)
        assertNotNull("Replacement Fragment should be added", replacementFragment)
        assertTrue("Replacement Fragment should be the correct type",
                replacementFragment is EmptyFragment)
        assertEquals("Replacement Fragment should be the primary navigation Fragment",
                replacementFragment, fragmentManager.primaryNavigationFragment)

        // Now pop the Fragment
        fragmentManager.popBackStackImmediate()
        verify(backPressListener).onPopBackStack(fragmentNavigator)
        fragment = fragmentManager.findFragmentById(R.id.container)
        assertEquals("Fragment should be the primary navigation Fragment after pop",
                fragment, fragmentManager.primaryNavigationFragment)

        verifyNoMoreInteractions(backPressListener)
    }

    @UiThreadTest
    @Test
    fun testMultipleNavigateFragmentTransactionsThenPopWithFragmentManager() {
        val fragmentNavigator = FragmentNavigator(emptyActivity,
                fragmentManager, R.id.container)
        val backPressListener = mock(Navigator.OnNavigatorBackPressListener::class.java)
        fragmentNavigator.addOnNavigatorBackPressListener(backPressListener)
        val destination = fragmentNavigator.createDestination()
        destination.className = EmptyFragment::class.java.name

        // Push 4 fragments without executing pending transactions.
        destination.id = INITIAL_FRAGMENT
        fragmentNavigator.navigate(destination, null, null, null)
        destination.id = SECOND_FRAGMENT
        fragmentNavigator.navigate(destination, null, null, null)
        destination.id = THIRD_FRAGMENT
        fragmentNavigator.navigate(destination, null, null, null)

        // Now pop the Fragment
        val popped = fragmentManager.popBackStackImmediate()
        assertTrue("FragmentNavigator should return true when popping the third fragment", popped)
        verify(backPressListener).onPopBackStack(fragmentNavigator)
        verifyNoMoreInteractions(backPressListener)
    }

    @UiThreadTest
    @Test
    fun testMultiplePopFragmentTransactionsThenPopWithFragmentManager() {
        val fragmentNavigator = FragmentNavigator(emptyActivity,
                fragmentManager, R.id.container)
        val backPressListener = mock(Navigator.OnNavigatorBackPressListener::class.java)
        fragmentNavigator.addOnNavigatorBackPressListener(backPressListener)
        val destination = fragmentNavigator.createDestination()
        destination.className = EmptyFragment::class.java.name

        // Push 4 fragments
        destination.id = INITIAL_FRAGMENT
        fragmentNavigator.navigate(destination, null, null, null)
        destination.id = SECOND_FRAGMENT
        fragmentNavigator.navigate(destination, null, null, null)
        destination.id = THIRD_FRAGMENT
        fragmentNavigator.navigate(destination, null, null, null)
        destination.id = FOURTH_FRAGMENT
        fragmentNavigator.navigate(destination, null, null, null)
        fragmentManager.executePendingTransactions()

        // Pop 2 fragments without executing pending transactions.
        fragmentNavigator.popBackStack()
        fragmentNavigator.popBackStack()

        val popped = fragmentManager.popBackStackImmediate()
        assertTrue("FragmentNavigator should return true when popping the third fragment", popped)
        verify(backPressListener).onPopBackStack(fragmentNavigator)
        verifyNoMoreInteractions(backPressListener)
    }
}

class EmptyActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.empty_activity)
    }
}
