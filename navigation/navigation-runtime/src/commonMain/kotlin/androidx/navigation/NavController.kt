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
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.core.bundle.Bundle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * NavController manages app navigation within a [NavHost].
 *
 * Apps will generally obtain a controller directly from a host, or by using one of the utility
 * methods on the [Navigation] class rather than create a controller directly.
 *
 * Navigation flows and destinations are determined by the
 * [navigation graph][NavGraph] owned by the controller.
 */
public expect open class NavController {
    /**
     * The topmost navigation graph associated with this NavController.
     *
     * When this is set any current navigation graph data (including back stack) will be replaced.
     *
     * @throws IllegalStateException if called before `setGraph()`.
     */
    public open var graph: NavGraph
        @MainThread
        get
        @MainThread
        @CallSuper
        set

    /**
     * Retrieve the current back stack.
     *
     * @return The current back stack.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val currentBackStack: StateFlow<List<NavBackStackEntry>>

    /**
     * A [StateFlow] that will emit the currently visible [NavBackStackEntries][NavBackStackEntry]
     * whenever they change. If there is no visible [NavBackStackEntry], this will be set to an
     * empty list.
     *
     * - `CREATED` entries are listed first and include all entries that are in the process of
     * completing their exit transition. Note that this can include entries that have been
     * popped off the Navigation back stack.
     * - `STARTED` entries on the back stack are next and include all entries that are running
     * their enter transition and entries whose destination is partially covered by a
     * `FloatingWindow` destination
     * - The last entry in the list is the topmost entry in the back stack and is in the `RESUMED`
     * state only if its enter transition has completed. Otherwise it too will be `STARTED`.
     *
     * Note that the `Lifecycle` of any entry cannot be higher than the containing
     * Activity/Fragment - if the Activity is not `RESUMED`, no entry will be `RESUMED`, no matter
     * what the transition state is.
     */
    public val visibleEntries: StateFlow<List<NavBackStackEntry>>

    /**
     * OnDestinationChangedListener receives a callback when the
     * [currentDestination] or its arguments change.
     */
    public fun interface OnDestinationChangedListener {
        /**
         * Callback for when the [currentDestination] or its arguments change.
         * This navigation may be to a destination that has not been seen before, or one that
         * was previously on the back stack. This method is called after navigation is complete,
         * but associated transitions may still be playing.
         *
         * @param controller the controller that navigated
         * @param destination the new destination
         * @param arguments the arguments passed to the destination
         */
        public fun onDestinationChanged(
            controller: NavController,
            destination: NavDestination,
            arguments: Bundle?
        )
    }

    /**
     * The NavController's [NavigatorProvider]. All [Navigators][Navigator] used
     * to construct the [navigation graph][NavGraph] for this nav controller should be added
     * to this navigator provider before the graph is constructed.
     *
     * This can only be set before the graph is set via `setGraph()`.
     *
     * Generally, the Navigators are set for you by the [NavHost] hosting this NavController
     * and you do not need to manually interact with the navigator provider.
     *
     * @throws IllegalStateException If this set called after `setGraph()`
     */
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open var navigatorProvider: NavigatorProvider

    /**
     * Adds an [OnDestinationChangedListener] to this controller to receive a callback
     * whenever the [currentDestination] or its arguments change.
     *
     * The current destination, if any, will be immediately sent to your listener.
     *
     * @param listener the listener to receive events
     */
    public open fun addOnDestinationChangedListener(listener: OnDestinationChangedListener)

    /**
     * Removes an [OnDestinationChangedListener] from this controller.
     * It will no longer receive callbacks.
     *
     * @param listener the listener to remove
     */
    public open fun removeOnDestinationChangedListener(listener: OnDestinationChangedListener)

    /**
     * Attempts to pop the controller's back stack. Analogous to when the user presses
     * the system back button when the associated navigation host has focus.
     *
     * @return true if the stack was popped at least once and the user has been navigated to
     * another destination, false otherwise
     */
    @MainThread
    public open fun popBackStack(): Boolean

    /**
     * Attempts to pop the controller's back stack back to a specific destination.
     *
     * @param route The topmost destination to retain. May contain filled in arguments as long as
     * it is exact match with route used to navigate.
     * @param inclusive Whether the given destination should also be popped.
     * @param saveState Whether the back stack and the state of all destinations between the
     * current destination and the [route] should be saved for later
     * restoration via [NavOptions.Builder.setRestoreState] or the `restoreState` attribute using
     * the same [route] (note: this matching ID is true whether
     * [inclusive] is true or false).
     *
     * @return true if the stack was popped at least once and the user has been navigated to
     * another destination, false otherwise
     */
    @MainThread
    @JvmOverloads
    public fun popBackStack(
        route: String,
        inclusive: Boolean,
        saveState: Boolean = false
    ): Boolean

    /**
     * Attempts to pop the controller's back stack back to a specific destination.
     *
     * @param T The topmost destination to retain with route from a [KClass]. The
     * target NavDestination must have been created with route from [KClass].
     * @param inclusive Whether the given destination should also be popped.
     * @param saveState Whether the back stack and the state of all destinations between the
     * current destination and [T] should be saved for later
     * restoration via [NavOptions.Builder.setRestoreState] or the `restoreState` attribute using
     * the same [T] (note: this matching ID is true whether
     * [inclusive] is true or false).
     *
     * @return true if the stack was popped at least once and the user has been navigated to
     * another destination, false otherwise
     */
    @MainThread
    @JvmOverloads
    public inline fun <reified T : Any> popBackStack(
        inclusive: Boolean,
        saveState: Boolean = false
    ): Boolean

    /**
     * Attempts to pop the controller's back stack back to a specific destination.
     *
     * @param route The topmost destination to retain with route from an Object. The
     * target NavDestination must have been created with route from [KClass].
     * @param inclusive Whether the given destination should also be popped.
     * @param saveState Whether the back stack and the state of all destinations between the
     * current destination and the [route] should be saved for later
     * restoration via [NavOptions.Builder.setRestoreState] or the `restoreState` attribute using
     * the same [route] (note: this matching ID is true whether
     * [inclusive] is true or false).
     *
     * @return true if the stack was popped at least once and the user has been navigated to
     * another destination, false otherwise
     */
    @MainThread
    @JvmOverloads
    public fun <T : Any> popBackStack(
        route: T,
        inclusive: Boolean,
        saveState: Boolean = false
    ): Boolean

    /**
     * Trigger a popBackStack() that originated from a Navigator specifically calling
     * [NavigatorState.pop] outside of a call to [popBackStack] (e.g., in response to some
     * user interaction that caused that destination to no longer be needed such as
     * dismissing a dialog destination).
     *
     * This method is responsible for popping all destinations above the given [popUpTo] entry and
     * popping the entry itself and removing it from the back stack before calling the
     * [onComplete] callback. Only after the processing here is done and the [onComplete]
     * callback completes does this method dispatch the destination change event.
     */
    internal fun popBackStackFromNavigator(popUpTo: NavBackStackEntry, onComplete: () -> Unit)

    /**
     * Clears any saved state associated with [route] that was previously saved
     * via [popBackStack] when using a `saveState` value of `true`.
     *
     * @param route The route of the destination previously used with [popBackStack] with a
     * `saveState` value of `true`. May contain filled in arguments as long as
     * it is exact match with route used with [popBackStack].
     *
     * @return true if the saved state of the stack associated with [route] was cleared.
     */
    @MainThread
    public fun clearBackStack(route: String): Boolean

    /**
     * Clears any saved state associated with KClass [T] that was previously saved
     * via [popBackStack] when using a `saveState` value of `true`.
     *
     * @param T The route from the [KClass] of the destination previously used with [popBackStack]
     * with a `saveState`value of `true`. The target NavDestination must have been created
     * with route from [KClass].
     *
     * @return true if the saved state of the stack associated with [T] was cleared.
     */
    @MainThread
    public inline fun <reified T : Any> clearBackStack(): Boolean

    /**
     * Clears any saved state associated with KClass [T] that was previously saved
     * via [popBackStack] when using a `saveState` value of `true`.
     *
     * @param route The route from an Object of the destination previously used with
     * [popBackStack] with a `saveState`value of `true`. The target NavDestination must
     * have been created with route from [KClass].
     *
     * @return true if the saved state of the stack associated with [T] was cleared.
     */
    @MainThread
    public fun <T : Any> clearBackStack(route: T): Boolean

    /**
     * Attempts to navigate up in the navigation hierarchy. Suitable for when the
     * user presses the "Up" button marked with a left (or start)-facing arrow in the upper left
     * (or starting) corner of the app UI.
     *
     * @return true if navigation was successful, false otherwise
     */
    @MainThread
    public open fun navigateUp(): Boolean

    /**
     * Sets the [navigation graph][NavGraph] to the specified graph.
     * Any current navigation graph data (including back stack) will be replaced.
     *
     * The graph can be retrieved later via [graph].
     *
     * @param graph graph to set
     * @param startDestinationArgs arguments to send to the start destination of the graph
     *
     * @see NavController.setGraph
     * @see NavController.graph
     */
    @MainThread
    @CallSuper
    public open fun setGraph(graph: NavGraph, startDestinationArgs: Bundle?)

    /**
     * The current destination.
     */
    public open val currentDestination: NavDestination?

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun findDestination(route: String): NavDestination?

    /**
     * Navigate to a route in the current NavGraph. If an invalid route is given, an
     * [IllegalArgumentException] will be thrown.
     *
     * If given [NavOptions] pass in [NavOptions.shouldRestoreState] `true`, any args passed here as part
     * of the route will be overridden by the restored args.
     *
     * @param route route for the destination
     * @param builder DSL for constructing a new [NavOptions]
     *
     * @throws IllegalArgumentException if the given route is invalid
     */
    @MainThread
    public fun navigate(route: String, builder: NavOptionsBuilder.() -> Unit)

    /**
     * Navigate to a route in the current NavGraph. If an invalid route is given, an
     * [IllegalArgumentException] will be thrown.
     *
     * If given [NavOptions] pass in [NavOptions.shouldRestoreState] `true`, any args passed here as part
     * of the route will be overridden by the restored args.
     *
     * @param route route for the destination
     * @param navOptions special options for this navigation operation
     * @param navigatorExtras extras to pass to the [Navigator]
     *
     * @throws IllegalArgumentException if the given route is invalid
     */
    @MainThread
    @JvmOverloads
    public fun navigate(
        route: String,
        navOptions: NavOptions? = null,
        navigatorExtras: Navigator.Extras? = null
    )

    /**
     * Navigate to a route from an Object in the current NavGraph. If an invalid route is given, an
     * [IllegalArgumentException] will be thrown.
     *
     * The target NavDestination must have been created with route from a [KClass]
     *
     * If given [NavOptions] pass in [NavOptions.restoreState] `true`, any args passed here as part
     * of the route will be overridden by the restored args.
     *
     * @param route route from an Object for the destination
     * @param builder DSL for constructing a new [NavOptions]
     *
     * @throws IllegalArgumentException if the given route is invalid
     */
    @MainThread
    public fun <T : Any> navigate(route: T, builder: NavOptionsBuilder.() -> Unit)

    /**
     * Navigate to a route from an Object in the current NavGraph. If an invalid route is given, an
     * [IllegalArgumentException] will be thrown.
     *
     * The target NavDestination must have been created with route from a [KClass]
     *
     * If given [NavOptions] pass in [NavOptions.restoreState] `true`, any args passed here as part
     * of the route will be overridden by the restored args.
     *
     * @param route route from an Object for the destination
     * @param navOptions special options for this navigation operation
     * @param navigatorExtras extras to pass to the [Navigator]
     *
     * @throws IllegalArgumentException if the given route is invalid
     */
    @MainThread
    @JvmOverloads
    public fun <T : Any> navigate(
        route: T,
        navOptions: NavOptions? = null,
        navigatorExtras: Navigator.Extras? = null
    )

    /**
     * Saves all navigation controller state to a Bundle.
     *
     * State may be restored from a bundle returned from this method by calling
     * [restoreState]. Saving controller state is the responsibility
     * of a [NavHost].
     *
     * @return saved state for this controller
     */
    @CallSuper
    public open fun saveState(): Bundle?

    /**
     * Restores all navigation controller state from a bundle. This should be called before
     * setting a graph.
     *
     * State may be saved to a bundle by calling [saveState].
     * Restoring controller state is the responsibility of a [NavHost].
     *
     * @param navState state bundle to restore
     */
    @CallSuper
    public open fun restoreState(navState: Bundle?)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open fun setLifecycleOwner(owner: LifecycleOwner)

    // TODO OnBackPressedDispatcher
    // @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    // public open fun setOnBackPressedDispatcher(dispatcher: OnBackPressedDispatcher)
    //
    // @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    // public open fun enableOnBackPressed(enabled: Boolean)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open fun setViewModelStore(viewModelStore: ViewModelStore)

    /**
     * Gets the topmost [NavBackStackEntry] for a route.
     *
     * This is always safe to use with [the current destination][currentDestination] or
     * [its parent][NavDestination.parent] or grandparent navigation graphs as these
     * destinations are guaranteed to be on the back stack.
     *
     * @param route route of a destination that exists on the back stack. May contain filled in
     * arguments as long as it is exact match with route used to navigate.
     * @throws IllegalArgumentException if the destination is not on the back stack
     */
    public fun getBackStackEntry(route: String): NavBackStackEntry

    /**
     * Gets the topmost [NavBackStackEntry] for a route from [KClass].
     *
     * This is always safe to use with [the current destination][currentDestination] or
     * [its parent][NavDestination.parent] or grandparent navigation graphs as these
     * destinations are guaranteed to be on the back stack.
     *
     * @param T route from the [KClass] of a destination that exists on the back stack. The
     * target NavBackStackEntry's [NavDestination] must have been created with route from [KClass].
     * @throws IllegalArgumentException if the destination is not on the back stack
     */
    public inline fun <reified T : Any> getBackStackEntry(): NavBackStackEntry

    /**
     * Gets the topmost [NavBackStackEntry] for a route from an Object.
     *
     * This is always safe to use with [the current destination][currentDestination] or
     * [its parent][NavDestination.parent] or grandparent navigation graphs as these
     * destinations are guaranteed to be on the back stack.
     *
     * @param route route from an Object of a destination that exists on the back stack. The
     * target NavBackStackEntry's [NavDestination] must have been created with route from [KClass].
     * @throws IllegalArgumentException if the destination is not on the back stack
     */
    public fun <T : Any> getBackStackEntry(route: T): NavBackStackEntry

    /**
     * The topmost [NavBackStackEntry].
     *
     * @return the topmost entry on the back stack or null if the back stack is empty
     */
    public open val currentBackStackEntry: NavBackStackEntry?

    /**
     * A [Flow] that will emit the currently active [NavBackStackEntry] whenever it changes. If
     * there is no active [NavBackStackEntry], no item will be emitted.
     */
    public val currentBackStackEntryFlow: Flow<NavBackStackEntry>

    /**
     * The previous visible [NavBackStackEntry].
     *
     * This skips over any [NavBackStackEntry] that is associated with a [NavGraph].
     *
     * @return the previous visible entry on the back stack or null if the back stack has less
     * than two visible entries
     */
    public open val previousBackStackEntry: NavBackStackEntry?
}

/**
 * Construct a new [NavGraph]
 *
 * @param startDestination the route for the start destination
 * @param route the route for the graph
 * @param builder the builder used to construct the graph
 */
public inline fun NavController.createGraph(
    startDestination: String,
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit
): NavGraph = navigatorProvider.navigation(startDestination, route, builder)

/**
 * Construct a new [NavGraph]
 *
 * @param startDestination the starting destination's route from a [KClass] for this NavGraph. The
 *   respective NavDestination must be added as a [KClass] in order to match.
 * @param route the graph's unique route from a [KClass]
 * @param typeMap A mapping of KType to custom NavType<*> in the [route]. May be empty if [route]
 *   does not use custom NavTypes.
 * @param builder the builder used to construct the graph
 */
public inline fun NavController.createGraph(
    startDestination: KClass<*>,
    route: KClass<*>? = null,
    typeMap: Map<KType, NavType<*>> = emptyMap(),
    builder: NavGraphBuilder.() -> Unit
): NavGraph = navigatorProvider.navigation(startDestination, route, typeMap, builder)

/**
 * Construct a new [NavGraph]
 *
 * @param startDestination the starting destination's route from an Object for this NavGraph. The
 *   respective NavDestination must be added as a [KClass] in order to match.
 * @param route the graph's unique route from a [KClass]
 * @param typeMap A mapping of KType to custom NavType<*> in the [route]. May be empty if [route]
 *   does not use custom NavTypes.
 * @param builder the builder used to construct the graph
 */
public inline fun NavController.createGraph(
    startDestination: Any,
    route: KClass<*>? = null,
    typeMap: Map<KType, NavType<*>> = emptyMap(),
    builder: NavGraphBuilder.() -> Unit
): NavGraph = navigatorProvider.navigation(startDestination, route, typeMap, builder)
