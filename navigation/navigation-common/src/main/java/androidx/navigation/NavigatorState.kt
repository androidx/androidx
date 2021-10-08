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
import androidx.annotation.CallSuper
import androidx.annotation.RestrictTo
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
    private val _transitionsInProgress: MutableStateFlow<List<NavBackStackEntry>> =
        MutableStateFlow(listOf())

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
     * Changing the back stack must be done via [push] and [pop].
     */
    public val backStack: StateFlow<List<NavBackStackEntry>> = _backStack.asStateFlow()

    /**
     * This is the set of currently running transitions. Use this set to retrieve the entry and call
     * [markTransitionComplete] once the transition is complete.
     */
    public val transitionsInProgress: StateFlow<List<NavBackStackEntry>> =
        _transitionsInProgress.asStateFlow()

    /**
     * Adds the given [backStackEntry] to the [backStack].
     */
    public open fun push(backStackEntry: NavBackStackEntry) {
        backStackLock.withLock {
            _backStack.value = _backStack.value + backStackEntry
        }
    }

    /**
     * Adds the given [backStackEntry] to the [backStack]. This also adds the given and
     * previous entry to the [set of in progress transitions][transitionsInProgress].
     * Added entries have their [Lifecycle] capped at [Lifecycle.State.STARTED] until an entry is
     * passed into the [markTransitionComplete] callback, when they are allowed to go to
     * [Lifecycle.State.RESUMED].
     *
     * @see transitionsInProgress
     * @see markTransitionComplete
     * @see popWithTransition
     */
    public open fun pushWithTransition(backStackEntry: NavBackStackEntry) {
        val previousEntry = backStack.value.lastOrNull()
        // When navigating, we need to mark the outgoing entry as transitioning until it
        // finishes its outgoing animation.
        if (previousEntry != null) {
            _transitionsInProgress.value = _transitionsInProgress.value + previousEntry
        }
        _transitionsInProgress.value = _transitionsInProgress.value + backStackEntry
        push(backStackEntry)
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
     * Pops all destinations up to and including [popUpTo]. This also adds the given and
     * incoming entry to the [set of in progress transitions][transitionsInProgress]. Added
     * entries have their [Lifecycle] held at [Lifecycle.State.CREATED] until an entry is
     * passed into the [markTransitionComplete] callback, when they are allowed to go to
     * [Lifecycle.State.DESTROYED] and have their state cleared.
     *
     * This will remove those destinations from the [backStack], saving their state if
     * [saveState] is `true`.
     *
     * @see transitionsInProgress
     * @see markTransitionComplete
     * @see pushWithTransition
     */
    public open fun popWithTransition(popUpTo: NavBackStackEntry, saveState: Boolean) {
        _transitionsInProgress.value = _transitionsInProgress.value + popUpTo
        val incomingEntry = backStack.value.lastOrNull { entry ->
            entry != popUpTo &&
                backStack.value.lastIndexOf(entry) < backStack.value.lastIndexOf(popUpTo)
        }
        // When popping, we need to mark the incoming entry as transitioning so we keep it
        // STARTED until the transition completes at which point we can move it to RESUMED
        if (incomingEntry != null) {
            _transitionsInProgress.value = _transitionsInProgress.value + incomingEntry
        }
        pop(popUpTo, saveState)
    }

    /**
     * Informational callback indicating that the given [backStackEntry] has been
     * affected by a [NavOptions.shouldLaunchSingleTop] operation.
     */
    @CallSuper
    public open fun onLaunchSingleTop(backStackEntry: NavBackStackEntry) {
        // We update the back stack here because we don't want to leave it to the navigator since
        // it might be using transitions.
        _backStack.value = _backStack.value - _backStack.value.last() + backStackEntry
    }

    /**
     * This removes the given [NavBackStackEntry] from the [set of the transitions in
     * progress][transitionsInProgress]. This should be called in conjunction with
     * [pushWithTransition] and [popWithTransition] as those call are responsible for adding
     * entries to [transitionsInProgress].
     *
     * Failing to call this method could result in entries being prevented from reaching their
     * final [Lifecycle.State]}.
     *
     * @see pushWithTransition
     * @see popWithTransition
     */
    public open fun markTransitionComplete(entry: NavBackStackEntry) {
        _transitionsInProgress.value = _transitionsInProgress.value - entry
    }
}
