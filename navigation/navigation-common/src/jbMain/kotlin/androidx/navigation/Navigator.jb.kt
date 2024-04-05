/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.annotation.CallSuper
import androidx.core.bundle.Bundle

public actual abstract class Navigator<D : NavDestination>(
    public val name: String
) {
    private var _state: NavigatorState? = null

    protected actual val state: NavigatorState
        get() = checkNotNull(_state) {
            "You cannot access the Navigator's state until the Navigator is attached"
        }

    public actual var isAttached: Boolean = false
        private set

    @CallSuper
    public actual open fun onAttach(state: NavigatorState) {
        _state = state
        isAttached = true
    }

    public actual abstract fun createDestination(): D

    @Suppress("UNCHECKED_CAST")
    public actual open fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ) {
        entries.asSequence().map { backStackEntry ->
            val destination = backStackEntry.destination as? D ?: return@map null
            val navigatedToDestination = navigate(
                destination, backStackEntry.arguments, navOptions, navigatorExtras
            )
            when (navigatedToDestination) {
                null -> null
                destination -> backStackEntry
                else -> {
                    state.createBackStackEntry(
                        navigatedToDestination,
                        navigatedToDestination.addInDefaultArgs(backStackEntry.arguments)
                    )
                }
            }
        }.filterNotNull().forEach { backStackEntry ->
            state.push(backStackEntry)
        }
    }

    @Suppress("UNCHECKED_CAST")
    public actual open fun onLaunchSingleTop(backStackEntry: NavBackStackEntry) {
        val destination = backStackEntry.destination as? D ?: return
        navigate(destination, null, navOptions { launchSingleTop = true }, null)
        state.onLaunchSingleTop(backStackEntry)
    }

    public actual open fun navigate(
        destination: D,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ): NavDestination? = destination

    public actual open fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        val backStack = state.backStack.value
        check(backStack.contains(popUpTo)) {
            "popBackStack was called with $popUpTo which does not exist in back stack $backStack"
        }
        val iterator = backStack.listIterator(backStack.size)
        var lastPoppedEntry: NavBackStackEntry? = null
        do {
            if (!popBackStack()) {
                // Quit early if popBackStack() returned false
                break
            }
            lastPoppedEntry = iterator.previous()
        } while (lastPoppedEntry != popUpTo)
        if (lastPoppedEntry != null) {
            state.pop(lastPoppedEntry, savedState)
        }
    }

    public actual open fun popBackStack(): Boolean = true

    public actual open fun onSaveState(): Bundle? {
        return null
    }

    public actual open fun onRestoreState(savedState: Bundle) {}

    public actual interface Extras
}
