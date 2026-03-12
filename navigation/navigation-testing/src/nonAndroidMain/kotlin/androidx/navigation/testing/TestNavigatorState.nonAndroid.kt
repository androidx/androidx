/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.lifecycle.ViewModelStore
import androidx.navigation.FloatingWindow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavViewModelStoreProvider
import androidx.navigation.NavigatorState
import androidx.navigation.SupportingPane
import androidx.navigation.internal.NavContext
import androidx.savedstate.SavedState
import androidx.savedstate.savedState
import kotlinx.coroutines.CoroutineDispatcher

public actual class TestNavigatorState
actual constructor(coroutineDispatcher: CoroutineDispatcher) : NavigatorState() {
    internal val navContext = NavContext()

    private val viewModelStoreProvider =
        object : NavViewModelStoreProvider {
            private val viewModelStores = mutableMapOf<String, ViewModelStore>()

            override fun getViewModelStore(backStackEntryId: String) =
                viewModelStores.getOrPut(backStackEntryId) { ViewModelStore() }
        }

    private val savedStates = mutableMapOf<String, SavedState>()
    private val entrySavedState = mutableMapOf<NavBackStackEntry, Boolean>()

    public actual override fun createBackStackEntry(
        destination: NavDestination,
        arguments: SavedState?,
    ): NavBackStackEntry =
        NavBackStackEntry.create(
            context = navContext,
            destination = destination,
            arguments = arguments,
            hostLifecycleState = Lifecycle.State.RESUMED,
            viewModelStoreProvider = viewModelStoreProvider,
        )

    /**
     * Restore a previously saved [NavBackStackEntry]. You must have previously called [pop] with
     * [previouslySavedEntry] and `true`.
     */
    public actual fun restoreBackStackEntry(
        previouslySavedEntry: NavBackStackEntry
    ): NavBackStackEntry {
        val savedState =
            checkNotNull(savedStates[previouslySavedEntry.id]) {
                "restoreBackStackEntry(previouslySavedEntry) must be passed a NavBackStackEntry " +
                    "that was previously popped with popBackStack(previouslySavedEntry, true)"
            }
        return NavBackStackEntry.create(
            context = navContext,
            destination = previouslySavedEntry.destination,
            arguments = previouslySavedEntry.arguments,
            hostLifecycleState = Lifecycle.State.RESUMED,
            viewModelStoreProvider = viewModelStoreProvider,
            id = previouslySavedEntry.id,
            savedState = savedState,
        )
    }

    public override fun push(backStackEntry: NavBackStackEntry) {
        super.push(backStackEntry)
        updateMaxLifecycle()
    }

    public override fun pop(popUpTo: NavBackStackEntry, saveState: Boolean) {
        val beforePopList = backStack.value
        val poppedList = beforePopList.subList(beforePopList.indexOf(popUpTo), beforePopList.size)
        super.pop(popUpTo, saveState)
        updateMaxLifecycle(poppedList, saveState)
    }

    public override fun popWithTransition(popUpTo: NavBackStackEntry, saveState: Boolean) {
        super.popWithTransition(popUpTo, saveState)
        entrySavedState[popUpTo] = saveState
    }

    public override fun markTransitionComplete(entry: NavBackStackEntry) {
        val savedState = entrySavedState[entry] == true
        super.markTransitionComplete(entry)
        entrySavedState.remove(entry)
        if (!backStack.value.contains(entry)) {
            updateMaxLifecycle(listOf(entry), savedState)
        } else {
            updateMaxLifecycle()
        }
    }

    public override fun prepareForTransition(entry: NavBackStackEntry) {
        super.prepareForTransition(entry)
        entry.maxLifecycle = Lifecycle.State.STARTED
    }

    private fun updateMaxLifecycle(
        poppedList: List<NavBackStackEntry> = emptyList(),
        saveState: Boolean = false,
    ) {
        // Mark all removed NavBackStackEntries as DESTROYED
        for (entry in poppedList.reversed()) {
            if (saveState && entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                // Move the NavBackStackEntry to the stopped state, then save its state
                entry.maxLifecycle = Lifecycle.State.CREATED
                val savedState = savedState()
                entry.saveState(savedState)
                savedStates[entry.id] = savedState
            }
            val transitioning = transitionsInProgress.value.contains(entry)
            if (!transitioning) {
                entry.maxLifecycle = Lifecycle.State.DESTROYED
                if (!saveState) {
                    savedStates.remove(entry.id)
                    viewModelStoreProvider.getViewModelStore(entry.id).clear()
                }
            } else {
                entry.maxLifecycle = Lifecycle.State.CREATED
            }
        }
        // Now go through the current list of destinations, updating their Lifecycle state
        val currentList = backStack.value
        var previousEntry: NavBackStackEntry? = null
        for (entry in currentList.reversed()) {
            val transitioning = transitionsInProgress.value.contains(entry)
            entry.maxLifecycle =
                when {
                    previousEntry == null ->
                        if (!transitioning) {
                            Lifecycle.State.RESUMED
                        } else {
                            Lifecycle.State.STARTED
                        }
                    previousEntry.destination is SupportingPane -> {
                        // Match the previous entry's destination, making sure
                        // a transitioning destination does not go to resumed
                        previousEntry.maxLifecycle.coerceAtMost(
                            if (!transitioning) {
                                Lifecycle.State.RESUMED
                            } else {
                                Lifecycle.State.STARTED
                            }
                        )
                    }
                    previousEntry.destination is FloatingWindow -> Lifecycle.State.STARTED
                    else -> Lifecycle.State.CREATED
                }
            previousEntry = entry
        }
    }
}
