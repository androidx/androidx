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
import androidx.navigation.FloatingWindow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
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

        state.transitionsInProgress.value[firstEntry]?.onTransitionComplete()
        assertThat(firstEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        val secondEntry = state.createBackStackEntry(navigator.createDestination(), null)
        navigator.navigate(listOf(secondEntry), null, null)
        assertThat(firstEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        assertThat(secondEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.STARTED)

        state.transitionsInProgress.value[secondEntry]?.onTransitionComplete()
        assertThat(secondEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        navigator.popBackStack(secondEntry, false)
        assertThat(firstEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        state.transitionsInProgress.value[secondEntry]?.onTransitionComplete()
        assertThat(secondEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)
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
}