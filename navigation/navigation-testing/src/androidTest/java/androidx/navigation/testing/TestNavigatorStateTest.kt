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
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.navigation.FloatingWindow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.SupportingPane
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
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.INITIALIZED)

        navigator.navigate(listOf(firstEntry), null, null)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        val secondEntry = state.createBackStackEntry(navigator.createDestination(), null)
        navigator.navigate(listOf(secondEntry), null, null)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        navigator.popBackStack(secondEntry, false)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun testFloatingWindowLifecycle() {
        val navigator = FloatingWindowTestNavigator()
        navigator.onAttach(state)
        val firstEntry = state.createBackStackEntry(navigator.createDestination(), null)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.INITIALIZED)

        navigator.navigate(listOf(firstEntry), null, null)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        val secondEntry = state.createBackStackEntry(navigator.createDestination(), null)
        navigator.navigate(listOf(secondEntry), null, null)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        navigator.popBackStack(secondEntry, false)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun testSupportingPaneLifecycle() {
        val navigator = SupportingPaneTestNavigator()
        navigator.onAttach(state)
        val firstEntry = state.createBackStackEntry(navigator.createDestination(), null)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.INITIALIZED)

        navigator.navigate(listOf(firstEntry), null, null)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        val secondEntry = state.createBackStackEntry(navigator.createDestination(), null)
        navigator.navigate(listOf(secondEntry), null, null)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        navigator.popBackStack(secondEntry, false)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun testWithTransitionLifecycle() {
        val navigator = TestTransitionNavigator()
        navigator.onAttach(state)
        val firstEntry = state.createBackStackEntry(navigator.createDestination(), null)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.INITIALIZED)

        navigator.navigate(listOf(firstEntry), null, null)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)

        state.markTransitionComplete(firstEntry)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        val secondEntry = state.createBackStackEntry(navigator.createDestination(), null)
        navigator.navigate(listOf(secondEntry), null, null)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        assertThat(state.transitionsInProgress.value.contains(firstEntry)).isTrue()

        state.markTransitionComplete(firstEntry)
        state.markTransitionComplete(secondEntry)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        navigator.popBackStack(secondEntry, true)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)

        state.markTransitionComplete(firstEntry)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        state.markTransitionComplete(secondEntry)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)

        val restoredSecondEntry = state.restoreBackStackEntry(secondEntry)
        navigator.navigate(listOf(restoredSecondEntry), null, null)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(restoredSecondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        assertThat(state.transitionsInProgress.value.contains(firstEntry)).isTrue()

        state.markTransitionComplete(firstEntry)
        state.markTransitionComplete(restoredSecondEntry)
        assertThat(restoredSecondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
    }

    @Test
    fun testWithSupportingPaneTransitionLifecycle() {
        val navigator = SupportingPaneTestTransitionNavigator()
        navigator.onAttach(state)
        val firstEntry = state.createBackStackEntry(navigator.createDestination(), null)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.INITIALIZED)

        navigator.navigate(listOf(firstEntry), null, null)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)

        state.markTransitionComplete(firstEntry)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        val secondEntry = state.createBackStackEntry(navigator.createDestination(), null)
        navigator.navigate(listOf(secondEntry), null, null)
        // Both are started because they are SupportingPane destinations
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        assertThat(state.transitionsInProgress.value.contains(firstEntry)).isTrue()

        state.markTransitionComplete(secondEntry)
        // Even though the secondEntry has completed its transition, the firstEntry
        // hasn't completed its transition, so it shouldn't be resumed yet
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        state.markTransitionComplete(firstEntry)
        // Both are resumed because they are SupportingPane destinations that have finished
        // their transitions
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        navigator.popBackStack(secondEntry, true)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)

        state.markTransitionComplete(firstEntry)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        state.markTransitionComplete(secondEntry)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)

        val restoredSecondEntry = state.restoreBackStackEntry(secondEntry)
        navigator.navigate(listOf(restoredSecondEntry), null, null)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        assertThat(restoredSecondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        assertThat(state.transitionsInProgress.value.contains(firstEntry)).isTrue()

        state.markTransitionComplete(firstEntry)
        state.markTransitionComplete(restoredSecondEntry)
        assertThat(restoredSecondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
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
        assertThat(state.transitionsInProgress.value.firstOrNull { it === restoredSecondEntry })
            .isNotNull()

        state.markTransitionComplete(firstEntry)
        assertThat(state.transitionsInProgress.value.contains(firstEntry)).isFalse()

        state.markTransitionComplete(restoredSecondEntry)
        assertThat(state.transitionsInProgress.value.firstOrNull { it === secondEntry }).isNull()

        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(restoredSecondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        state.markTransitionComplete(secondEntry)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
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
        navigator.navigate(
            listOf(secondEntry),
            navOptions {
                popUpTo("first") { saveState = true }
                launchSingleTop = true
                restoreState = true
            },
            null
        )

        val viewModel = ViewModelProvider(secondEntry).get(TestViewModel::class.java)

        navigator.popBackStack(secondEntry, true)
        val restoredSecondEntry = state.restoreBackStackEntry(secondEntry)
        navigator.navigate(
            listOf(restoredSecondEntry),
            navOptions {
                popUpTo("first") { saveState = true }
                launchSingleTop = true
                restoreState = true
            },
            null
        )

        state.transitionsInProgress.value.forEach { state.markTransitionComplete(it) }

        assertThat(viewModel.wasCleared).isFalse()
    }

    @Test
    fun testTransitionInterruptPushPop() {
        val navigator = TestTransitionNavigator()
        navigator.onAttach(state)
        val firstEntry = state.createBackStackEntry(navigator.createDestination(), null)

        navigator.navigate(listOf(firstEntry), null, null)
        state.markTransitionComplete(firstEntry)

        val secondEntry = state.createBackStackEntry(navigator.createDestination(), null)
        navigator.navigate(listOf(secondEntry), null, null)
        assertThat(state.transitionsInProgress.value.contains(firstEntry)).isTrue()
        assertThat(state.transitionsInProgress.value.contains(secondEntry)).isTrue()
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)

        navigator.popBackStack(secondEntry, true)
        assertThat(state.transitionsInProgress.value.contains(firstEntry)).isTrue()
        assertThat(state.transitionsInProgress.value.contains(secondEntry)).isTrue()
        state.markTransitionComplete(firstEntry)
        state.markTransitionComplete(secondEntry)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun testTransitionInterruptPopPush() {
        val navigator = TestTransitionNavigator()
        navigator.onAttach(state)
        val firstEntry = state.createBackStackEntry(navigator.createDestination(), null)

        navigator.navigate(listOf(firstEntry), null, null)
        state.markTransitionComplete(firstEntry)

        val secondEntry = state.createBackStackEntry(navigator.createDestination(), null)
        navigator.navigate(listOf(secondEntry), null, null)
        assertThat(state.transitionsInProgress.value.contains(firstEntry)).isTrue()
        assertThat(state.transitionsInProgress.value.contains(secondEntry)).isTrue()
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        state.markTransitionComplete(firstEntry)
        state.markTransitionComplete(secondEntry)

        navigator.popBackStack(secondEntry, true)
        assertThat(state.transitionsInProgress.value.contains(firstEntry)).isTrue()
        assertThat(state.transitionsInProgress.value.contains(secondEntry)).isTrue()

        val secondEntryReplace = state.createBackStackEntry(navigator.createDestination(), null)
        navigator.navigate(listOf(secondEntryReplace), null, null)
        assertThat(state.transitionsInProgress.value.contains(firstEntry)).isTrue()
        assertThat(state.transitionsInProgress.value.contains(secondEntry)).isTrue()
        assertThat(state.transitionsInProgress.value.contains(secondEntryReplace)).isTrue()
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(secondEntryReplace.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        state.markTransitionComplete(firstEntry)
        state.markTransitionComplete(secondEntry)
        state.markTransitionComplete(secondEntryReplace)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(secondEntryReplace.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
    }

    @Test
    fun testPrepareForTransition() {
        val navigator = TestTransitionNavigator()
        navigator.onAttach(state)
        val firstEntry = state.createBackStackEntry(navigator.createDestination(), null)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.INITIALIZED)

        navigator.navigate(listOf(firstEntry), null, null)
        navigator.testLifecycle.addObserver(
            object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    when (event) {
                        Lifecycle.Event.ON_STOP -> {
                            // this is okay since the first entry will not be DESTROYED in this
                            // test.
                            state.markTransitionComplete(firstEntry)
                        }
                        Lifecycle.Event.ON_PAUSE -> state.prepareForTransition(firstEntry)
                        Lifecycle.Event.ON_START -> state.prepareForTransition(firstEntry)
                        Lifecycle.Event.ON_RESUME -> state.markTransitionComplete(firstEntry)
                        else -> {}
                    }
                }
            }
        )
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)

        navigator.testLifecycle.currentState = Lifecycle.State.RESUMED
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        navigator.testLifecycle.currentState = Lifecycle.State.STARTED
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)

        val secondEntry = state.createBackStackEntry(navigator.createDestination(), null)
        navigator.navigate(listOf(secondEntry), null, null)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        assertThat(state.transitionsInProgress.value.contains(firstEntry)).isTrue()

        // Moving down to reflect a destination being on a back stack and only CREATED
        navigator.testLifecycle.currentState = Lifecycle.State.CREATED

        state.markTransitionComplete(secondEntry)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)

        navigator.testLifecycle.currentState = Lifecycle.State.STARTED
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)

        navigator.popBackStack(secondEntry, true)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)

        navigator.testLifecycle.currentState = Lifecycle.State.RESUMED
        assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        state.markTransitionComplete(secondEntry)
        assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Navigator.Name("test")
    internal class TestNavigator : Navigator<NavDestination>() {
        override fun createDestination(): NavDestination = NavDestination(this)
    }

    @Navigator.Name("test")
    internal class TestTransitionNavigator : Navigator<NavDestination>() {
        private val testLifecycleOwner = TestLifecycleOwner()
        val testLifecycle = testLifecycleOwner.lifecycle

        override fun createDestination(): NavDestination = NavDestination(this)

        override fun navigate(
            entries: List<NavBackStackEntry>,
            navOptions: NavOptions?,
            navigatorExtras: Extras?
        ) {
            entries.forEach { entry -> state.pushWithTransition(entry) }
        }

        override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
            state.popWithTransition(popUpTo, savedState)
        }
    }

    @Navigator.Name("test")
    internal class FloatingWindowTestNavigator : Navigator<FloatingTestDestination>() {
        override fun createDestination(): FloatingTestDestination = FloatingTestDestination(this)
    }

    internal class FloatingTestDestination(navigator: Navigator<out NavDestination>) :
        NavDestination(navigator), FloatingWindow

    @Navigator.Name("test")
    internal class SupportingPaneTestNavigator : Navigator<SupportingPaneTestDestination>() {
        override fun createDestination(): SupportingPaneTestDestination =
            SupportingPaneTestDestination(this)
    }

    @Navigator.Name("test")
    internal class SupportingPaneTestTransitionNavigator :
        Navigator<SupportingPaneTestDestination>() {

        override fun createDestination(): SupportingPaneTestDestination =
            SupportingPaneTestDestination(this)

        override fun navigate(
            entries: List<NavBackStackEntry>,
            navOptions: NavOptions?,
            navigatorExtras: Extras?
        ) {
            entries.forEach { entry -> state.pushWithTransition(entry) }
        }

        override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
            state.popWithTransition(popUpTo, savedState)
        }
    }

    internal class SupportingPaneTestDestination(navigator: Navigator<out NavDestination>) :
        NavDestination(navigator), SupportingPane

    class TestViewModel : ViewModel() {
        var wasCleared = false

        override fun onCleared() {
            super.onCleared()
            wasCleared = true
        }
    }
}
