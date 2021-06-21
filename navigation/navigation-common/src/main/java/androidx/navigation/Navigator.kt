/*
 * Copyright (C) 2017 The Android Open Source Project
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
import androidx.navigation.Navigator.Name

/**
 * Navigator defines a mechanism for navigating within an app.
 *
 * Each Navigator sets the policy for a specific type of navigation, e.g.
 * [ActivityNavigator] knows how to launch into [destinations][NavDestination]
 * backed by activities using [startActivity][Context.startActivity].
 *
 * Navigators should be able to manage their own back stack when navigating between two
 * destinations that belong to that navigator. The [NavController] manages a back stack of
 * navigators representing the current navigation stack across all navigators.
 *
 * Each Navigator should add the [Navigator.Name annotation][Name] to their class. Any
 * custom attributes used by the associated [destination][NavDestination] subclass should
 * have a name corresponding with the name of the Navigator, e.g., [ActivityNavigator] uses
 * `<declare-styleable name="ActivityNavigator">`
 *
 * @param D the subclass of [NavDestination] used with this Navigator which can be used
 * to hold any special data that will be needed to navigate to that destination.
 * Examples include information about an intent to navigate to other activities,
 * or a fragment class name to instantiate and swap to a new fragment.
 */
public abstract class Navigator<D : NavDestination> {
    /**
     * This annotation should be added to each Navigator subclass to denote the default name used
     * to register the Navigator with a [NavigatorProvider].
     *
     * @see NavigatorProvider.addNavigator
     * @see NavigatorProvider.getNavigator
     */
    @kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
    public annotation class Name(val value: String)

    private var _state: NavigatorState? = null

    /**
     * The state of the Navigator is the communication conduit between the Navigator
     * and the [NavController] that has called [onAttach].
     *
     * It is the responsibility of the Navigator to call [NavigatorState.push]
     * and [NavigatorState.pop] to in order to update the [NavigatorState.backStack] at
     * the appropriate times.
     *
     * @throws IllegalStateException if [isAttached] is `false`
     */
    protected val state: NavigatorState
        get() = checkNotNull(_state) {
            "You cannot access the Navigator's state until the Navigator is attached"
        }

    /**
     * Whether this Navigator is actively being used by a [NavController].
     *
     * This is set to `true` when [onAttach] is called.
     */
    public var isAttached: Boolean = false
        private set

    /**
     * Indicator that this Navigator is actively being used by a [NavController]. This
     * is called when the NavController's state is ready to be restored.
     */
    @CallSuper
    public open fun onAttach(state: NavigatorState) {
        _state = state
        isAttached = true
    }

    /**
     * Construct a new NavDestination associated with this Navigator.
     *
     * Any initialization of the destination should be done in the destination's constructor as
     * it is not guaranteed that every destination will be created through this method.
     * @return a new NavDestination
     */
    public abstract fun createDestination(): D

    /**
     * Navigate to a destination.
     *
     * Requests navigation to a given destination associated with this navigator in
     * the navigation graph. This method generally should not be called directly;
     * [NavController] will delegate to it when appropriate.
     *
     * @param entries destination(s) to navigate to
     * @param navOptions additional options for navigation
     * @param navigatorExtras extras unique to your Navigator.
     */
    @Suppress("UNCHECKED_CAST")
    public open fun navigate(
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
                    backStackEntry.replaceArguments(
                        navigatedToDestination.addInDefaultArgs(backStackEntry.arguments)
                    )
                    state.createBackStackEntry(navigatedToDestination, backStackEntry.arguments)
                }
            }
        }.filterNotNull().forEach { backStackEntry ->
            state.push(backStackEntry)
        }
    }

    /**
     * Informational callback indicating that the given [backStackEntry] has been
     * affected by a [NavOptions.shouldLaunchSingleTop] operation. The entry's state
     * and arguments have already been updated, but this callback can be used to synchronize
     * any external state with this operation.
     */
    @Suppress("UNCHECKED_CAST")
    public open fun onLaunchSingleTop(backStackEntry: NavBackStackEntry) {
        val destination = backStackEntry.destination as? D ?: return
        navigate(destination, null, navOptions { launchSingleTop = true }, null)
    }

    /**
     * Navigate to a destination.
     *
     * Requests navigation to a given destination associated with this navigator in
     * the navigation graph. This method generally should not be called directly;
     * [NavController] will delegate to it when appropriate.
     *
     * @param destination destination node to navigate to
     * @param args arguments to use for navigation
     * @param navOptions additional options for navigation
     * @param navigatorExtras extras unique to your Navigator.
     * @return The NavDestination that should be added to the back stack or null if
     * no change was made to the back stack (i.e., in cases of single top operations
     * where the destination is already on top of the back stack).
     */
    // TODO Deprecate this method once all call sites are removed
    @Suppress("UNUSED_PARAMETER", "RedundantNullableReturnType")
    public open fun navigate(
        destination: D,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ): NavDestination? = destination

    /**
     * Attempt to pop this navigator's back stack, performing the appropriate navigation.
     *
     * All destinations back to [popUpTo] should be popped off the back stack.
     *
     * @param popUpTo the entry that should be popped off the [NavigatorState.backStack]
     * along with all entries above this entry.
     * @param savedState whether any Navigator specific state associated with [popUpTo] should
     * be saved to later be restored by a call to [navigate] with [NavOptions.shouldRestoreState].
     */
    @Suppress("UNUSED_PARAMETER")
    public open fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
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

    /**
     * Attempt to pop this navigator's back stack, performing the appropriate navigation.
     *
     * Implementations should return `true` if navigation
     * was successful. Implementations should return `false` if navigation could not
     * be performed, for example if the navigator's back stack was empty.
     *
     * @return `true` if pop was successful
     */
    // TODO Deprecate this method once all call sites are removed
    public open fun popBackStack(): Boolean = true

    /**
     * Called to ask for a [Bundle] representing the Navigator's state. This will be
     * restored in [onRestoreState].
     */
    public open fun onSaveState(): Bundle? {
        return null
    }

    /**
     * Restore any state previously saved in [onSaveState]. This will be called before
     * any calls to [navigate] or
     * [popBackStack].
     *
     * Calls to [createDestination] should not be dependent on any state restored here as
     * [createDestination] can be called before the state is restored.
     *
     * @param savedState The state previously saved
     */
    public open fun onRestoreState(savedState: Bundle) {}

    /**
     * Interface indicating that this class should be passed to its respective
     * [Navigator] to enable Navigator specific behavior.
     */
    public interface Extras
}
