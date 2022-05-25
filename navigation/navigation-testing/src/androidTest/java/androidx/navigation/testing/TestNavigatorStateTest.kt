/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.navigation.testing

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.FloatingWindow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.navOptions
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class TestNavigatorStateTest {
    private lateinit var state: TestNavigatorState

    @Before
    fun setUp() {
        state = TestNavigatorState()
    }

    @Test
    fun testLifecycle() {
        val navigator = TestNavigator()
        navigator.onAttach(state)
        val firstEntry = state.createBackStackEntry(navigator.createDestination(), null)
        assertThat(firstEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.INITIALIZED)

        navigator.navigate(listOf(firstEntry), null, null)
        assertThat(firstEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        val secondEntry = state.createBackStackEntry(navigator.createDestination(), null)
        navigator.navigate(listOf(secondEntry), null, null)
        assertThat(firstEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        assertThat(secondEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        navigator.popBackStack(secondEntry, false)
        assertThat(firstEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertThat(secondEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun testFloatingWindowLifecycle() {
        val navigator = FloatingWindowTestNavigator()
        navigator.onAttach(state)
        val firstEntry = state.createBackStackEntry(navigator.createDestination(), null)
        assertThat(firstEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.INITIALIZED)

        navigator.navigate(listOf(firstEntry), null, null)
        assertThat(firstEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        val secondEntry = state.createBackStackEntry(navigator.createDestination(), null)
        navigator.navigate(listOf(secondEntry), null, null)
        assertThat(firstEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.STARTED)
        assertThat(secondEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        navigator.popBackStack(secondEntry, false)
        assertThat(firstEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertThat(secondEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun testWithTransitionLifecycle() {
        val navigator = TestTransitionNavigator()
        navigator.onAttach(state)
        val firstEntry = state.createBackStackEntry(navigator.createDestination(), null)
        assertThat(firstEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.INITIALIZED)

        navigator.navigate(listOf(firstEntry), null, null)
        assertThat(firstEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.STARTED)

        state.markTransitionComplete(firstEntry)
        assertThat(firstEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        val secondEntry = state.createBackStackEntry(navigator.createDestination(), null)
        navigator.navigate(listOf(secondEntry), null, null)
        assertThat(firstEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        assertThat(secondEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.STARTED)
        assertThat(state.transitionsInProgress.value.contains(firstEntry)).isTrue()

        state.markTransitionComplete(firstEntry)
        state.markTransitionComplete(secondEntry)
        assertThat(secondEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        navigator.popBackStack(secondEntry, true)
        assertThat(secondEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        assertThat(firstEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.STARTED)

        state.markTransitionComplete(firstEntry)
        assertThat(firstEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        state.markTransitionComplete(secondEntry)
        assertThat(secondEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)

        val restoredSecondEntry = state.restoreBackStackEntry(secondEntry)
        navigator.navigate(listOf(restoredSecondEntry), null, null)
        assertThat(firstEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        assertThat(restoredSecondEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.STARTED)
        assertThat(state.transitionsInProgress.value.contains(firstEntry)).isTrue()

        state.markTransitionComplete(firstEntry)
        state.markTransitionComplete(restoredSecondEntry)
        assertThat(restoredSecondEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
    }

    @Test
    fun testSameEntry() {
        val navigator = TestTransitionNavigator()
        navigator.onAttach(state)
        val firstEntry = state.createBackStackEntry(navigator.createDestination(), null)

        navigator.navigate(listOf(firstEntry), null, null)
        state.markTransitionComplete(firstEntry)

        val secondEntry = state.createBackStackEntry(navigator.createDestination(), null)
        navigator.navigate(listOf(secondEntry), null, null)
        assertThat(state.transitionsInProgress.value.contains(firstEntry)).isTrue()

        state.markTransitionComplete(firstEntry)
        state.markTransitionComplete(secondEntry)

        navigator.popBackStack(secondEntry, true)
        assertThat(state.transitionsInProgress.value.contains(firstEntry)).isTrue()
        assertThat(state.transitionsInProgress.value.contains(secondEntry)).isTrue()
        state.markTransitionComplete(firstEntry)
        state.markTransitionComplete(secondEntry)
        val restoredSecondEntry = state.restoreBackStackEntry(secondEntry)
        navigator.navigate(listOf(restoredSecondEntry), null, null)
        assertThat(
            state.transitionsInProgress.value.firstOrNull { it === restoredSecondEntry }
        ).isNotNull()

        state.markTransitionComplete(firstEntry)
        assertThat(state.transitionsInProgress.value.contains(firstEntry)).isFalse()

        state.markTransitionComplete(restoredSecondEntry)
        assertThat(state.transitionsInProgress.value.firstOrNull { it === secondEntry }).isNull()

        assertThat(firstEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        assertThat(restoredSecondEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        state.markTransitionComplete(secondEntry)
        assertThat(secondEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun testNewInstanceBeforeComplete() {
        val navigator = TestTransitionNavigator()
        navigator.onAttach(state)
        val firstEntry = state.createBackStackEntry(navigator.createDestination(), null)
        firstEntry.destination.route = "first"

        navigator.navigate(listOf(firstEntry), null, null)
        state.markTransitionComplete(firstEntry)

        val secondEntry = state.createBackStackEntry(navigator.createDestination(), null)
        secondEntry.destination.route = "second"
        navigator.navigate(listOf(secondEntry), navOptions {
            popUpTo("first") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }, null)

        val viewModel = ViewModelProvider(secondEntry).get(TestViewModel::class.java)

        navigator.popBackStack(secondEntry, true)
        val restoredSecondEntry = state.restoreBackStackEntry(secondEntry)
        navigator.navigate(listOf(restoredSecondEntry), navOptions {
            popUpTo("first") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }, null)

        state.transitionsInProgress.value.forEach {
            state.markTransitionComplete(it)
        }

        assertThat(viewModel.wasCleared).isFalse()
    }

    @Navigator.Name("test")
    internal class TestNavigator : Navigator<NavDestination>() {
        override fun createDestination(): NavDestination = NavDestination(this)
    }

    @Navigator.Name("test")
    internal class TestTransitionNavigator : Navigator<NavDestination>() {
        override fun createDestination(): NavDestination = NavDestination(this)

        override fun navigate(
            entries: List<NavBackStackEntry>,
            navOptions: NavOptions?,
            navigatorExtras: Extras?
        ) {
            entries.forEach { entry ->
                state.pushWithTransition(entry)
            }
        }

        override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
            state.popWithTransition(popUpTo, savedState)
        }
    }

    @Navigator.Name("test")
    internal class FloatingWindowTestNavigator : Navigator<FloatingTestDestination>() {
        override fun createDestination(): FloatingTestDestination = FloatingTestDestination(this)
    }

    internal class FloatingTestDestination(
        navigator: Navigator<out NavDestination>
    ) : NavDestination(navigator), FloatingWindow

    class TestViewModel : ViewModel() {
        var wasCleared = false

        override fun onCleared() {
            super.onCleared()
            wasCleared = true
        }
    }
}