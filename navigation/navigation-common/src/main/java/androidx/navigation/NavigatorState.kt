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

package androidx.navigation

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.navigation.NavigatorState.OnTransitionCompleteListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * The NavigatorState encapsulates the state shared between the [Navigator] and the
 * [NavController].
 */
public abstract class NavigatorState {
    private val backStackLock = ReentrantLock(true)
    private val _backStack: MutableStateFlow<List<NavBackStackEntry>> = MutableStateFlow(listOf())
    private val _transitionsInProgress:
        MutableStateFlow<Map<NavBackStackEntry, OnTransitionCompleteListener>> =
            MutableStateFlow(mapOf())

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var isNavigating = false

    /**
     * While the [NavController] is responsible for the combined back stack across all
     * Navigators, this back stack is specifically the set of destinations associated
     * with this Navigator.
     *
     * Changing the back stack must be done via [add] and [pop].
     */
    public val backStack: StateFlow<List<NavBackStackEntry>> = _backStack.asStateFlow()

    /**
     * This is the map of currently running transitions to their individual
     * [OnTransitionCompleteListener]s. Use this map to retrieve the listener and execute the
     * callback once the transition is complete.
     */
    public val transitionsInProgress:
        StateFlow<Map<NavBackStackEntry, OnTransitionCompleteListener>> =
            _transitionsInProgress.asStateFlow()

    /**
     * Adds the given [backStackEntry] to the [backStack].
     */
    public open fun add(backStackEntry: NavBackStackEntry) {
        backStackLock.withLock {
            _backStack.value = _backStack.value + backStackEntry
        }
    }

    /**
     * Provides listener that once activated, adds the given [backStackEntry] to the [backStack].
     */
    public open fun addWithTransition(
        backStackEntry: NavBackStackEntry
    ): OnTransitionCompleteListener {
        add(backStackEntry)
        return OnTransitionCompleteListener {
            removeInProgressTransition(backStackEntry)
        }
    }

    /**
     * Create a new [NavBackStackEntry] from a given [destination] and [arguments].
     */
    public abstract fun createBackStackEntry(
        destination: NavDestination,
        arguments: Bundle?
    ): NavBackStackEntry

    /**
     * Pop all destinations up to and including [popUpTo]. This will remove those
     * destinations from the [backStack], saving their state if [saveState] is `true`.
     */
    public open fun pop(popUpTo: NavBackStackEntry, saveState: Boolean) {
        backStackLock.withLock {
            _backStack.value = _backStack.value.takeWhile { it != popUpTo }
        }
    }

    /**
     * Provides listener that once activated, Pops all destinations up to and including [popUpTo].
     *
     * This will remove those destinations from the [backStack], saving their state if
     * [saveState] is `true`.
     */
    public open fun popWithTransition(
        popUpTo: NavBackStackEntry,
        saveState: Boolean
    ): OnTransitionCompleteListener {
        val listener = OnTransitionCompleteListener {
            removeInProgressTransition(popUpTo)
        }
        pop(popUpTo, saveState)
        return listener
    }

    /**
     * Adds a transition listener to the group of in progress transitions.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun addInProgressTransition(
        entry: NavBackStackEntry,
        listener: OnTransitionCompleteListener
    ) {
        _transitionsInProgress.value = _transitionsInProgress.value + (entry to listener)
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun removeInProgressTransition(entry: NavBackStackEntry) {
        _transitionsInProgress.value = _transitionsInProgress.value - entry
    }

    /**
     * OnTransitionCompleteListener receives a callback when a destination transition is complete.
     */
    public fun interface OnTransitionCompleteListener {
        /**
         * Callback for when the transition has completed.
         */
        public fun onTransitionComplete()
    }
}
