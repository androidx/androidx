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

package androidx.navigation.internal

import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.FloatingWindow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavBackStackEntryState
import androidx.navigation.NavController
import androidx.navigation.NavController.NavControllerNavigatorState
import androidx.navigation.NavController.OnDestinationChangedListener
import androidx.navigation.NavControllerViewModel
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.createRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph
import androidx.navigation.NavGraph.Companion.childHierarchy
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavUri
import androidx.navigation.Navigator
import androidx.navigation.NavigatorProvider
import androidx.navigation.NavigatorState
import androidx.navigation.SupportingPane
import androidx.navigation.get
import androidx.navigation.navOptions
import androidx.navigation.serialization.generateHashCode
import androidx.navigation.serialization.generateRouteWithArgs
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState
import androidx.savedstate.write
import kotlin.collections.removeFirst as removeFirstKt
import kotlin.collections.removeLast as removeLastKt
import kotlin.reflect.KClass
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

internal class NavControllerImpl(
    val navController: NavController,
    var updateOnBackPressedCallbackEnabledCallback: () -> Unit,
) {
    val navContext: NavContext
        get() = navController.navContext

    internal var _graph: NavGraph? = null

    internal var graph: NavGraph
        @MainThread
        get() {
            checkNotNull(_graph) { "You must call setGraph() before calling getGraph()" }
            return _graph as NavGraph
        }
        @MainThread
        @CallSuper
        set(graph) {
            setGraph(graph, null)
        }

    internal var navigatorStateToRestore: SavedState? = null
    internal var backStackToRestore: Array<SavedState>? = null

    internal val backQueue: ArrayDeque<NavBackStackEntry> = ArrayDeque()

    internal val _currentBackStack: MutableStateFlow<List<NavBackStackEntry>> =
        MutableStateFlow(emptyList())

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    internal val currentBackStack: StateFlow<List<NavBackStackEntry>> =
        _currentBackStack.asStateFlow()

    internal val _visibleEntries: MutableStateFlow<List<NavBackStackEntry>> =
        MutableStateFlow(emptyList())

    internal val visibleEntries: StateFlow<List<NavBackStackEntry>> = _visibleEntries.asStateFlow()

    internal val childToParentEntries = mutableMapOf<NavBackStackEntry, NavBackStackEntry>()
    internal val parentToChildCount = mutableMapOf<NavBackStackEntry, AtomicInt>()

    internal fun linkChildToParent(child: NavBackStackEntry, parent: NavBackStackEntry) {
        childToParentEntries[child] = parent
        if (parentToChildCount[parent] == null) {
            parentToChildCount[parent] = AtomicInt(0)
        }
        parentToChildCount[parent]!!.incrementAndGet()
    }

    internal fun unlinkChildFromParent(child: NavBackStackEntry): NavBackStackEntry? {
        val parent = childToParentEntries.remove(child) ?: return null
        val count = parentToChildCount[parent]?.decrementAndGet()
        if (count == 0) {
            val navGraphNavigator: Navigator<out NavGraph> =
                _navigatorProvider[parent.destination.navigatorName]
            navigatorState[navGraphNavigator]?.markTransitionComplete(parent)
            parentToChildCount.remove(parent)
        }
        return parent
    }

    internal val backStackMap = mutableMapOf<Int, String?>()
    internal val backStackStates = mutableMapOf<String, ArrayDeque<NavBackStackEntryState>>()
    internal var lifecycleOwner: LifecycleOwner? = null
        private set

    internal var viewModel: NavControllerViewModel? = null
    internal val onDestinationChangedListeners = mutableListOf<OnDestinationChangedListener>()
    internal var hostLifecycleState: Lifecycle.State = Lifecycle.State.INITIALIZED
        get() {
            // A LifecycleOwner is not required by NavController.
            // In the cases where one is not provided, always keep the host lifecycle at CREATED
            return if (lifecycleOwner == null) {
                Lifecycle.State.CREATED
            } else {
                field
            }
        }

    internal val lifecycleObserver: LifecycleObserver = LifecycleEventObserver { _, event ->
        hostLifecycleState = event.targetState
        if (_graph != null) {
            // Operate on a copy of the queue to avoid issues with reentrant
            // calls if updating the Lifecycle calls navigate() or popBackStack()
            val backStack = backQueue.toMutableList()
            for (entry in backStack) {
                entry.handleLifecycleEvent(event)
            }
        }
    }

    internal var _navigatorProvider = NavigatorProvider()

    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    internal var navigatorProvider: NavigatorProvider
        get() = _navigatorProvider
        /**  */
        set(navigatorProvider) {
            check(backQueue.isEmpty()) { "NavigatorProvider must be set before setGraph call" }
            _navigatorProvider = navigatorProvider
        }

    internal val navigatorState =
        mutableMapOf<Navigator<out NavDestination>, NavControllerNavigatorState>()
    internal var addToBackStackHandler: ((backStackEntry: NavBackStackEntry) -> Unit)? = null
    internal var popFromBackStackHandler: ((popUpTo: NavBackStackEntry) -> Unit)? = null
    internal val entrySavedState = mutableMapOf<NavBackStackEntry, Boolean>()

    /**
     * Call [Navigator.navigate] while setting up a [handler] that receives callbacks when
     * [NavigatorState.push] is called.
     */
    internal fun navigateInternal(
        navigator: Navigator<out NavDestination>,
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?,
        handler: (backStackEntry: NavBackStackEntry) -> Unit = {},
    ) {
        addToBackStackHandler = handler
        navigator.navigate(entries, navOptions, navigatorExtras)
        addToBackStackHandler = null
    }

    /**
     * Call [Navigator.popBackStack] while setting up a [handler] that receives callbacks when
     * [NavigatorState.pop] is called.
     */
    internal fun popBackStackInternal(
        navigator: Navigator<out NavDestination>,
        popUpTo: NavBackStackEntry,
        saveState: Boolean,
        handler: (popUpTo: NavBackStackEntry) -> Unit = {},
    ) {
        popFromBackStackHandler = handler
        navigator.popBackStack(popUpTo, saveState)
        popFromBackStackHandler = null
    }

    internal fun push(state: NavControllerNavigatorState, backStackEntry: NavBackStackEntry) {
        val destinationNavigator: Navigator<out NavDestination> =
            _navigatorProvider[backStackEntry.destination.navigatorName]
        if (destinationNavigator == state.navigator) {
            val handler = addToBackStackHandler
            if (handler != null) {
                handler(backStackEntry)
                state.addInternal(backStackEntry)
            } else {
                // TODO handle the Navigator calling add() outside of a call to navigate()
                Log.i(
                    TAG,
                    "Ignoring add of destination ${backStackEntry.destination} " +
                        "outside of the call to navigate(). ",
                )
            }
        } else {
            val navigatorBackStack =
                checkNotNull(navigatorState[destinationNavigator]) {
                    "NavigatorBackStack for ${backStackEntry.destination.navigatorName} should " +
                        "already be created"
                }
            navigatorBackStack.push(backStackEntry)
        }
    }

    internal fun createBackStackEntry(destination: NavDestination, arguments: SavedState?) =
        NavBackStackEntry.create(navContext, destination, arguments, hostLifecycleState, viewModel)

    internal fun pop(
        state: NavControllerNavigatorState,
        popUpTo: NavBackStackEntry,
        saveState: Boolean,
        superCallback: () -> Unit,
    ) {
        val destinationNavigator: Navigator<out NavDestination> =
            _navigatorProvider[popUpTo.destination.navigatorName]
        entrySavedState[popUpTo] = saveState
        if (destinationNavigator == state.navigator) {
            val handler = popFromBackStackHandler
            if (handler != null) {
                handler(popUpTo)
                superCallback()
            } else {
                popBackStackFromNavigator(popUpTo) { superCallback() }
            }
        } else {
            navigatorState[destinationNavigator]!!.pop(popUpTo, saveState)
        }
    }

    internal fun markTransitionComplete(
        state: NavControllerNavigatorState,
        entry: NavBackStackEntry,
        superCallback: () -> Unit,
    ) {
        val savedState = entrySavedState[entry] == true
        superCallback()
        entrySavedState.remove(entry)
        if (!backQueue.contains(entry)) {
            unlinkChildFromParent(entry)
            // If the entry is no longer part of the backStack, we need to manually move
            // it to DESTROYED, and clear its view model
            if (entry.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                entry.maxLifecycle = Lifecycle.State.DESTROYED
            }
            if (backQueue.none { it.id == entry.id } && !savedState) {
                viewModel?.clear(entry.id)
            }
            updateBackStackLifecycle()
            // Nothing in backQueue changed, so unlike other places where
            // we change visibleEntries, we don't need to emit a new
            // currentBackStack
            _visibleEntries.tryEmit(populateVisibleEntries())
        } else if (!state.isNavigating) {
            updateBackStackLifecycle()
            _currentBackStack.tryEmit(backQueue.toMutableList())
            _visibleEntries.tryEmit(populateVisibleEntries())
        }
        // else, updateBackStackLifecycle() will be called after any ongoing navigate() call
        // completes
    }

    internal fun prepareForTransition(entry: NavBackStackEntry) {
        if (backQueue.contains(entry)) {
            entry.maxLifecycle = Lifecycle.State.STARTED
        } else {
            throw IllegalStateException("Cannot transition entry that is not in the back stack")
        }
    }

    internal fun addOnDestinationChangedListener(listener: OnDestinationChangedListener) {
        onDestinationChangedListeners.add(listener)

        // Inform the new listener of our current state, if any
        if (backQueue.isNotEmpty()) {
            val backStackEntry = backQueue.last()
            listener.onDestinationChanged(
                navController,
                backStackEntry.destination,
                backStackEntry.arguments,
            )
        }
    }

    internal fun removeOnDestinationChangedListener(listener: OnDestinationChangedListener) {
        onDestinationChangedListeners.remove(listener)
    }

    internal fun popBackStack(): Boolean {
        return if (backQueue.isEmpty()) {
            // Nothing to pop if the back stack is empty
            false
        } else {
            popBackStack(currentDestination!!.id, true)
        }
    }

    internal fun popBackStack(destinationId: Int, inclusive: Boolean): Boolean {
        return popBackStack(destinationId, inclusive, false)
    }

    internal fun popBackStack(destinationId: Int, inclusive: Boolean, saveState: Boolean): Boolean {
        val popped = popBackStackInternal(destinationId, inclusive, saveState)
        // Only return true if the pop succeeded and we've dispatched
        // the change to a new destination
        return popped && dispatchOnDestinationChanged()
    }

    internal fun popBackStack(route: String, inclusive: Boolean, saveState: Boolean): Boolean {
        val popped = popBackStackInternal(route, inclusive, saveState)
        // Only return true if the pop succeeded and we've dispatched
        // the change to a new destination
        return popped && dispatchOnDestinationChanged()
    }

    @OptIn(InternalSerializationApi::class)
    internal fun <T : Any> popBackStack(
        route: KClass<T>,
        inclusive: Boolean,
        saveState: Boolean,
    ): Boolean {
        val id = route.serializer().generateHashCode()
        requireNotNull(findDestinationComprehensive(graph, id, true)) {
            "Destination with route ${route.simpleName} cannot be found in navigation " +
                "graph $graph"
        }
        return popBackStack(id, inclusive, saveState)
    }

    internal fun <T : Any> popBackStack(route: T, inclusive: Boolean, saveState: Boolean): Boolean {
        val popped = popBackStackInternal(route, inclusive, saveState)
        // Only return true if the pop succeeded and we've dispatched
        // the change to a new destination
        return popped && dispatchOnDestinationChanged()
    }

    /**
     * Attempts to pop the controller's back stack back to a specific destination. This does **not**
     * handle calling [dispatchOnDestinationChanged]
     *
     * @param destinationId The topmost destination to retain
     * @param inclusive Whether the given destination should also be popped.
     * @param saveState Whether the back stack and the state of all destinations between the current
     *   destination and the [destinationId] should be saved for later restoration via
     *   [NavOptions.Builder.setRestoreState] or the `restoreState` attribute using the same
     *   [destinationId] (note: this matching ID is true whether [inclusive] is true or false).
     * @return true if the stack was popped at least once, false otherwise
     */
    internal fun popBackStackInternal(
        destinationId: Int,
        inclusive: Boolean,
        saveState: Boolean = false,
    ): Boolean {
        if (backQueue.isEmpty()) {
            // Nothing to pop if the back stack is empty
            return false
        }
        val popOperations = mutableListOf<Navigator<*>>()
        val iterator = backQueue.reversed().iterator()
        var foundDestination: NavDestination? = null
        while (iterator.hasNext()) {
            val destination = iterator.next().destination
            val navigator = _navigatorProvider.getNavigator<Navigator<*>>(destination.navigatorName)
            if (inclusive || destination.id != destinationId) {
                popOperations.add(navigator)
            }
            if (destination.id == destinationId) {
                foundDestination = destination
                break
            }
        }
        if (foundDestination == null) {
            // We were passed a destinationId that doesn't exist on our back stack.
            // Better to ignore the popBackStack than accidentally popping the entire stack
            val destinationName = NavDestination.getDisplayName(navContext, destinationId)
            Log.i(
                TAG,
                "Ignoring popBackStack to destination $destinationName as it was not found " +
                    "on the current back stack",
            )
            return false
        }
        return executePopOperations(popOperations, foundDestination, inclusive, saveState)
    }

    internal fun <T : Any> popBackStackInternal(
        route: T,
        inclusive: Boolean,
        saveState: Boolean = false,
    ): Boolean {
        // route contains arguments so we need to generate and pop with the populated route
        // rather than popping based on route pattern
        val finalRoute = generateRouteFilled(route)
        return popBackStackInternal(finalRoute, inclusive, saveState)
    }

    /**
     * Attempts to pop the controller's back stack back to a specific destination. This does **not**
     * handle calling [dispatchOnDestinationChanged]
     *
     * @param route The topmost destination with this route to retain
     * @param inclusive Whether the given destination should also be popped.
     * @param saveState Whether the back stack and the state of all destinations between the current
     *   destination and the destination with [route] should be saved for later to be restored via
     *   [NavOptions.Builder.setRestoreState] or the `restoreState` attribute using the
     *   [NavDestination.id] of the destination with this route (note: this matching ID is true
     *   whether [inclusive] is true or false).
     * @return true if the stack was popped at least once, false otherwise
     */
    internal fun popBackStackInternal(
        route: String,
        inclusive: Boolean,
        saveState: Boolean,
    ): Boolean {
        if (backQueue.isEmpty()) {
            // Nothing to pop if the back stack is empty
            return false
        }

        val popOperations = mutableListOf<Navigator<*>>()
        val foundDestination =
            backQueue
                .lastOrNull { entry ->
                    val hasRoute = entry.destination.hasRoute(route, entry.arguments)
                    if (inclusive || !hasRoute) {
                        val navigator =
                            _navigatorProvider.getNavigator<Navigator<*>>(
                                entry.destination.navigatorName
                            )
                        popOperations.add(navigator)
                    }
                    hasRoute
                }
                ?.destination

        if (foundDestination == null) {
            // We were passed a route that doesn't exist on our back stack.
            // Better to ignore the popBackStack than accidentally popping the entire stack
            Log.i(
                TAG,
                "Ignoring popBackStack to route $route as it was not found " +
                    "on the current back stack",
            )
            return false
        }
        return executePopOperations(popOperations, foundDestination, inclusive, saveState)
    }

    internal fun executePopOperations(
        popOperations: List<Navigator<*>>,
        foundDestination: NavDestination,
        inclusive: Boolean,
        saveState: Boolean,
    ): Boolean {
        var popped = false
        val savedState = ArrayDeque<NavBackStackEntryState>()
        for (navigator in popOperations) {
            var receivedPop = false
            popBackStackInternal(navigator, backQueue.last(), saveState) { entry ->
                receivedPop = true
                popped = true
                popEntryFromBackStack(entry, saveState, savedState)
            }
            if (!receivedPop) {
                // The pop did not complete successfully, so stop immediately
                break
            }
        }
        if (saveState) {
            if (!inclusive) {
                // If this isn't an inclusive pop, we need to explicitly map the
                // saved state to the destination you've actually passed to popUpTo
                // as well as its parents (if it is the start destination)
                generateSequence(foundDestination) { destination ->
                        if (destination.parent?.startDestinationId == destination.id) {
                            destination.parent
                        } else {
                            null
                        }
                    }
                    .takeWhile { destination ->
                        // Only add the state if it doesn't already exist
                        !backStackMap.containsKey(destination.id)
                    }
                    .forEach { destination ->
                        backStackMap[destination.id] = savedState.firstOrNull()?.id
                    }
            }
            if (savedState.isNotEmpty()) {
                val firstState = savedState.first()
                // Whether is is inclusive or not, we need to map the
                // saved state to the destination that was popped
                // as well as its parents (if it is the start destination)
                val firstStateDestination = findDestination(firstState.destinationId)
                generateSequence(firstStateDestination) { destination ->
                        if (destination.parent?.startDestinationId == destination.id) {
                            destination.parent
                        } else {
                            null
                        }
                    }
                    .takeWhile { destination ->
                        // Only add the state if it doesn't already exist
                        !backStackMap.containsKey(destination.id)
                    }
                    .forEach { destination -> backStackMap[destination.id] = firstState.id }

                if (backStackMap.values.contains(firstState.id)) {
                    // And finally, store the actual state itself if the entry was added
                    // to backStackMap
                    backStackStates[firstState.id] = savedState
                }
            }
        }
        updateOnBackPressedCallbackEnabledCallback()
        return popped
    }

    /**
     * Trigger a popBackStack() that originated from a Navigator specifically calling
     * [NavigatorState.pop] outside of a call to [popBackStack] (e.g., in response to some user
     * interaction that caused that destination to no longer be needed such as dismissing a dialog
     * destination).
     *
     * This method is responsible for popping all destinations above the given [popUpTo] entry and
     * popping the entry itself and removing it from the back stack before calling the [onComplete]
     * callback. Only after the processing here is done and the [onComplete] callback completes does
     * this method dispatch the destination change event.
     */
    internal fun popBackStackFromNavigator(popUpTo: NavBackStackEntry, onComplete: () -> Unit) {
        val popIndex = backQueue.indexOf(popUpTo)
        if (popIndex < 0) {
            Log.i(TAG, "Ignoring pop of $popUpTo as it was not found on the current back stack")
            return
        }
        if (popIndex + 1 != backQueue.size) {
            // There's other destinations stacked on top of this destination that
            // we need to pop first
            popBackStackInternal(
                backQueue[popIndex + 1].destination.id,
                inclusive = true,
                saveState = false,
            )
        }
        // Now record the pop of the actual entry - we don't use popBackStackInternal
        // here since we're being called from the Navigator already
        popEntryFromBackStack(popUpTo)
        onComplete()
        updateOnBackPressedCallbackEnabledCallback()
        dispatchOnDestinationChanged()
    }

    internal fun popEntryFromBackStack(
        popUpTo: NavBackStackEntry,
        saveState: Boolean = false,
        savedState: ArrayDeque<NavBackStackEntryState> = ArrayDeque(),
    ) {
        val entry = backQueue.last()
        check(entry == popUpTo) {
            "Attempted to pop ${popUpTo.destination}, which is not the top of the back stack " +
                "(${entry.destination})"
        }
        backQueue.removeLastKt()
        val navigator =
            navigatorProvider.getNavigator<Navigator<NavDestination>>(
                entry.destination.navigatorName
            )
        val state = navigatorState[navigator]
        // If we pop an entry with transitions, but not the graph, we will not make a call to
        // popBackStackInternal, so the graph entry will not be marked as transitioning so we
        // need to check if it still has children.
        val transitioning =
            state?.transitionsInProgress?.value?.contains(entry) == true ||
                parentToChildCount.containsKey(entry)
        if (entry.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            if (saveState) {
                // Move the state through STOPPED
                entry.maxLifecycle = Lifecycle.State.CREATED
                // Then save the state of the NavBackStackEntry
                savedState.addFirst(NavBackStackEntryState(entry))
            }
            if (!transitioning) {
                entry.maxLifecycle = Lifecycle.State.DESTROYED
                unlinkChildFromParent(entry)
            } else {
                entry.maxLifecycle = Lifecycle.State.CREATED
            }
        }
        if (!saveState && !transitioning) {
            viewModel?.clear(entry.id)
        }
    }

    internal fun clearBackStack(route: String): Boolean {
        val cleared = clearBackStackInternal(route)
        // Only return true if the clear succeeded and we've dispatched
        // the change to a new destination
        return cleared && dispatchOnDestinationChanged()
    }

    internal fun clearBackStack(destinationId: Int): Boolean {
        val cleared = clearBackStackInternal(destinationId)
        // Only return true if the clear succeeded and we've dispatched
        // the change to a new destination
        return cleared && dispatchOnDestinationChanged()
    }

    @OptIn(InternalSerializationApi::class)
    internal fun <T : Any> clearBackStack(route: KClass<T>): Boolean =
        clearBackStack(route.serializer().generateHashCode())

    @OptIn(InternalSerializationApi::class)
    internal fun <T : Any> clearBackStack(route: T): Boolean {
        // route contains arguments so we need to generate and clear with the populated route
        // rather than clearing based on route pattern
        val finalRoute = generateRouteFilled(route)
        val cleared = clearBackStackInternal(finalRoute)
        // Only return true if the clear succeeded and we've dispatched
        // the change to a new destination
        return cleared && dispatchOnDestinationChanged()
    }

    internal fun clearBackStackInternal(destinationId: Int): Boolean {
        navigatorState.values.forEach { state -> state.isNavigating = true }
        val restored =
            restoreStateInternal(destinationId, null, navOptions { restoreState = true }, null)
        navigatorState.values.forEach { state -> state.isNavigating = false }
        return restored && popBackStackInternal(destinationId, inclusive = true, saveState = false)
    }

    internal fun clearBackStackInternal(route: String): Boolean {
        navigatorState.values.forEach { state -> state.isNavigating = true }
        val restored = restoreStateInternal(route)
        navigatorState.values.forEach { state -> state.isNavigating = false }
        return restored && popBackStackInternal(route, inclusive = true, saveState = false)
    }

    private var dispatchReentrantCount = 0
    private val backStackEntriesToDispatch = mutableListOf<NavBackStackEntry>()

    /**
     * Dispatch changes to all OnDestinationChangedListeners.
     *
     * If the back stack is empty, no events get dispatched.
     *
     * @return If changes were dispatched.
     */
    internal fun dispatchOnDestinationChanged(): Boolean {
        // We never want to leave NavGraphs on the top of the stack
        while (!backQueue.isEmpty() && backQueue.last().destination is NavGraph) {
            popEntryFromBackStack(backQueue.last())
        }
        val lastBackStackEntry = backQueue.lastOrNull()
        if (lastBackStackEntry != null) {
            backStackEntriesToDispatch += lastBackStackEntry
        }
        // Track that we're updating the back stack lifecycle
        // just in case updateBackStackLifecycle() results in
        // additional calls to navigate() or popBackStack()
        dispatchReentrantCount++
        updateBackStackLifecycle()
        dispatchReentrantCount--

        if (dispatchReentrantCount == 0) {
            // Only the outermost dispatch should dispatch
            val dispatchList = backStackEntriesToDispatch.toMutableList()
            backStackEntriesToDispatch.clear()
            for (backStackEntry in dispatchList) {
                // Now call all registered OnDestinationChangedListener instances
                for (listener in onDestinationChangedListeners.toList()) {
                    listener.onDestinationChanged(
                        navController,
                        backStackEntry.destination,
                        backStackEntry.arguments,
                    )
                }
                _currentBackStackEntryFlow.tryEmit(backStackEntry)
            }
            _currentBackStack.tryEmit(backQueue.toMutableList())
            _visibleEntries.tryEmit(populateVisibleEntries())
        }
        return lastBackStackEntry != null
    }

    internal fun updateBackStackLifecycle() {
        // Operate on a copy of the queue to avoid issues with reentrant
        // calls if updating the Lifecycle calls navigate() or popBackStack()
        val backStack = backQueue.toMutableList()
        if (backStack.isEmpty()) {
            // Nothing to update
            return
        }
        // Lifecycle can be split into three layers:
        // 1. Resumed - these are the topmost destination(s) that the user can interact with
        // 2. Started - these destinations are visible, but are underneath resumed destinations
        // 3. Created - these destinations are not visible or on the process of being animated out

        // So first, we need to determine which destinations should be resumed and started
        // This is done by looking at the two special interfaces we have:
        // - FloatingWindow indicates a destination that is above all other destinations, leaving
        //   destinations below it visible, but not interactable. These are always only on the
        //   top of the back stack
        // - SupportingPane indicates a destination that sits alongside the previous destination
        //   and shares the same lifecycle (e.g., both will be resumed, started, or created)

        // This means no matter what, the topmost destination should be able to be resumed,
        // then we add in all of the destinations that also need to be resumed (if the
        // topmost screen is a SupportingPane)
        val topmostDestination = backStack.last().destination
        val nextResumed: MutableList<NavDestination> = mutableListOf(topmostDestination)
        if (topmostDestination is SupportingPane) {
            // A special note for destinations that are marked as both a FloatingWindow and a
            // SupportingPane: a supporting floating window destination can only support other
            // floating windows - if a supporting floating window destination is above
            // a regular destination, the regular destination will *not* be resumed, but instead
            // follow the normal rules between floating windows and regular destinations and only
            // be started.
            val onlyAllowFloatingWindows = topmostDestination is FloatingWindow
            val iterator = backStack.reversed().drop(1).iterator()
            while (iterator.hasNext()) {
                val destination = iterator.next().destination
                if (
                    onlyAllowFloatingWindows &&
                        destination !is FloatingWindow &&
                        destination !is NavGraph
                ) {
                    break
                }
                // Add all visible destinations (e.g., SupportingDestination destinations, their
                // NavGraphs, and the screen directly below all SupportingDestination destinations)
                // to nextResumed
                nextResumed.add(destination)
                // break if we find first visible screen
                if (destination !is SupportingPane && destination !is NavGraph) {
                    break
                }
            }
        }

        // Now that we've marked all of the resumed destinations, we continue to iterate
        // through the back stack to find any destinations that should be started - ones that are
        // below FloatingWindow destinations
        val nextStarted: MutableList<NavDestination> = mutableListOf()
        if (nextResumed.last() is FloatingWindow) {
            // Find all visible destinations in the back stack as they
            // should still be STARTED when the FloatingWindow destination is above it.
            val iterator = backStack.reversed().iterator()
            while (iterator.hasNext()) {
                val destination = iterator.next().destination
                // Add all visible destinations (e.g., FloatingWindow destinations, their
                // NavGraphs, and the screen directly below all FloatingWindow destinations)
                // to nextStarted
                nextStarted.add(destination)
                // break if we find first visible screen
                if (
                    destination !is FloatingWindow &&
                        destination !is SupportingPane &&
                        destination !is NavGraph
                ) {
                    break
                }
            }
        }

        // Now iterate downward through the stack, applying downward Lifecycle
        // transitions and capturing any upward Lifecycle transitions to apply afterwards.
        // This ensures proper nesting where parent navigation graphs are started before
        // their children and stopped only after their children are stopped.
        val upwardStateTransitions = HashMap<NavBackStackEntry, Lifecycle.State>()
        var iterator = backStack.reversed().iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val currentMaxLifecycle = entry.maxLifecycle
            val destination = entry.destination
            if (nextResumed.firstOrNull()?.id == destination.id) {
                // Upward Lifecycle transitions need to be done afterwards so that
                // the parent navigation graph is resumed before their children
                if (currentMaxLifecycle != Lifecycle.State.RESUMED) {
                    val navigator =
                        navigatorProvider.getNavigator<Navigator<*>>(
                            entry.destination.navigatorName
                        )
                    val state = navigatorState[navigator]
                    val transitioning = state?.transitionsInProgress?.value?.contains(entry)
                    if (transitioning != true && parentToChildCount[entry]?.get() != 0) {
                        upwardStateTransitions[entry] = Lifecycle.State.RESUMED
                    } else {
                        upwardStateTransitions[entry] = Lifecycle.State.STARTED
                    }
                }
                if (nextStarted.firstOrNull()?.id == destination.id) nextStarted.removeFirstKt()
                nextResumed.removeFirstKt()
                destination.parent?.let { nextResumed.add(it) }
            } else if (nextStarted.isNotEmpty() && destination.id == nextStarted.first().id) {
                val started = nextStarted.removeFirstKt()
                if (currentMaxLifecycle == Lifecycle.State.RESUMED) {
                    // Downward transitions should be done immediately so children are
                    // paused before their parent navigation graphs
                    entry.maxLifecycle = Lifecycle.State.STARTED
                } else if (currentMaxLifecycle != Lifecycle.State.STARTED) {
                    // Upward Lifecycle transitions need to be done afterwards so that
                    // the parent navigation graph is started before their children
                    upwardStateTransitions[entry] = Lifecycle.State.STARTED
                }
                started.parent?.let {
                    if (!nextStarted.contains(it)) {
                        nextStarted.add(it)
                    }
                }
            } else {
                entry.maxLifecycle = Lifecycle.State.CREATED
            }
        }
        // Apply all upward Lifecycle transitions by iterating through the stack again,
        // this time applying the new lifecycle to the parent navigation graphs first
        iterator = backStack.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val newState = upwardStateTransitions[entry]
            if (newState != null) {
                entry.maxLifecycle = newState
            } else {
                // Ensure the state is up to date
                entry.updateState()
            }
        }
    }

    internal fun populateVisibleEntries(): List<NavBackStackEntry> {
        val entries = mutableListOf<NavBackStackEntry>()
        // Add any transitioning entries that are not at least STARTED
        navigatorState.values.forEach { state ->
            entries +=
                state.transitionsInProgress.value.filter { entry ->
                    !entries.contains(entry) &&
                        !entry.maxLifecycle.isAtLeast(Lifecycle.State.STARTED)
                }
        }
        // Add any STARTED entries from the backQueue. This will include the topmost
        // non-FloatingWindow destination plus every FloatingWindow destination above it.
        entries +=
            backQueue.filter { entry ->
                !entries.contains(entry) && entry.maxLifecycle.isAtLeast(Lifecycle.State.STARTED)
            }
        return entries.filter { it.destination !is NavGraph }
    }

    internal fun setGraph(graph: NavGraph, startDestinationArgs: SavedState?) {
        check(backQueue.isEmpty() || hostLifecycleState != Lifecycle.State.DESTROYED) {
            "You cannot set a new graph on a NavController with entries on the back stack " +
                "after the NavController has been destroyed. Please ensure that your NavHost " +
                "has the same lifetime as your NavController."
        }
        if (_graph != graph) {
            _graph?.let { previousGraph ->
                // Clear all saved back stacks by iterating through a copy of the saved keys,
                // thus avoiding any concurrent modification exceptions
                val savedBackStackIds = ArrayList(backStackMap.keys)
                savedBackStackIds.forEach { id -> clearBackStackInternal(id) }
                // Pop everything from the old graph off the back stack
                popBackStackInternal(previousGraph.id, true)
            }
            _graph = graph
            onGraphCreated(startDestinationArgs)
        } else {
            // first we update _graph with new instances from graph
            for (i in 0 until graph.nodes.size()) {
                val newDestination = graph.nodes.valueAt(i)
                val key = _graph!!.nodes.keyAt(i)
                _graph!!.nodes.replace(key, newDestination)
            }
            // then we update backstack with the new instances
            backQueue.forEach { entry ->
                // we will trace this hierarchy in new graph to get new destination instance
                val hierarchy = entry.destination.hierarchy.toList().asReversed()
                val newDestination =
                    hierarchy.fold(_graph!!) { newDest: NavDestination, oldDest: NavDestination ->
                        if (oldDest == _graph && newDest == graph) {
                            // if root graph, it is already the node that matches with oldDest
                            newDest
                        } else if (newDest is NavGraph) {
                            // otherwise we walk down the hierarchy to the next child
                            newDest.findNode(oldDest.id)!!
                        } else {
                            // final leaf node found
                            newDest
                        }
                    }
                entry.destination = newDestination
            }
        }
    }

    internal fun onGraphCreated(startDestinationArgs: SavedState?) {
        navigatorStateToRestore?.read {
            if (contains(KEY_NAVIGATOR_STATE_NAMES)) {
                val navigatorNames = getStringList(KEY_NAVIGATOR_STATE_NAMES)
                for (name in navigatorNames) {
                    val navigator = _navigatorProvider.getNavigator<Navigator<*>>(name)
                    if (contains(name)) {
                        val savedState = getSavedState(name)
                        navigator.onRestoreState(savedState)
                    }
                }
            }
        }
        backStackToRestore?.let { backStackToRestore ->
            for (savedState in backStackToRestore) {
                val state = NavBackStackEntryState(savedState)
                val node = findDestination(state.destinationId)
                if (node == null) {
                    val dest = NavDestination.getDisplayName(navContext, state.destinationId)
                    throw IllegalStateException(
                        "Restoring the Navigation back stack failed: destination $dest cannot be " +
                            "found from the current destination $currentDestination"
                    )
                }
                val entry = state.instantiate(navContext, node, hostLifecycleState, viewModel)
                val navigator = _navigatorProvider.getNavigator<Navigator<*>>(node.navigatorName)
                val navigatorBackStack =
                    navigatorState.getOrPut(navigator) {
                        navController.createNavControllerNavigatorState(navigator)
                    }
                backQueue.add(entry)
                navigatorBackStack.addInternal(entry)
                val parent = entry.destination.parent
                if (parent != null) {
                    linkChildToParent(entry, getBackStackEntry(parent.id))
                }
            }
            updateOnBackPressedCallbackEnabledCallback()
            this.backStackToRestore = null
        }
        // Mark all Navigators as attached
        _navigatorProvider.navigators.values
            .filterNot { it.isAttached }
            .forEach { navigator ->
                val navigatorBackStack =
                    navigatorState.getOrPut(navigator) {
                        navController.createNavControllerNavigatorState(navigator)
                    }
                navigator.onAttach(navigatorBackStack)
            }
        if (_graph != null && backQueue.isEmpty()) {
            if (!navController.checkDeepLinkHandled()) {
                // Navigate to the first destination in the graph
                // if we haven't deep linked to a destination
                navigate(_graph!!, startDestinationArgs, null, null)
            }
        } else {
            dispatchOnDestinationChanged()
        }
    }

    internal fun findInvalidDestinationDisplayNameInDeepLink(deepLink: IntArray): String? {
        var graph = _graph
        for (i in deepLink.indices) {
            val destinationId = deepLink[i]
            val node =
                (if (i == 0) if (_graph!!.id == destinationId) _graph else null
                else graph!!.findNode(destinationId))
                    ?: return NavDestination.getDisplayName(navContext, destinationId)
            if (i != deepLink.size - 1) {
                // We're not at the final NavDestination yet, so keep going through the chain
                if (node is NavGraph) {
                    graph = node
                    // Automatically go down the navigation graph when
                    // the start destination is also a NavGraph
                    while (graph!!.findNode(graph.startDestinationId) is NavGraph) {
                        graph = graph.findNode(graph.startDestinationId) as NavGraph?
                    }
                }
            }
        }
        // We found every destination in the deepLink array, yay!
        return null
    }

    internal val currentDestination: NavDestination?
        get() {
            return currentBackStackEntry?.destination
        }

    internal fun findDestination(
        destinationId: Int,
        matchingDest: NavDestination? = null,
    ): NavDestination? {
        if (_graph == null) {
            return null
        }

        if (_graph!!.id == destinationId) {
            when {
                /**
                 * if the search expected a specific NavDestination (i.e. a duplicated destination
                 * within a specific graph), we need to make sure the result matches it to ensure
                 * this search returns the correct duplicate.
                 */
                matchingDest != null ->
                    if (_graph == matchingDest && matchingDest.parent == null) return _graph
                else -> return _graph
            }
        }

        val currentNode = backQueue.lastOrNull()?.destination ?: _graph!!
        return findDestinationComprehensive(currentNode, destinationId, false, matchingDest)
    }

    internal fun findDestinationComprehensive(
        destination: NavDestination,
        destinationId: Int,
        searchChildren: Boolean,
        matchingDest: NavDestination? = null,
    ): NavDestination? {
        if (destination.id == destinationId) {
            when {
                // check parent in case of duplicated destinations to ensure it finds the correct
                // nested destination
                matchingDest != null ->
                    if (destination == matchingDest && destination.parent == matchingDest.parent)
                        return destination
                else -> return destination
            }
        }
        val currentGraph = destination as? NavGraph ?: destination.parent!!
        return currentGraph.findNodeComprehensive(
            destinationId,
            currentGraph,
            searchChildren,
            matchingDest,
        )
    }

    internal fun findDestination(route: String): NavDestination? {
        if (_graph == null) {
            return null
        }
        // if not matched by routePattern, try matching with route args
        if (_graph!!.route == route || _graph!!.matchRoute(route) != null) {
            return _graph
        }
        return getTopGraph().findNode(route)
    }

    /**
     * Returns the last NavGraph on the backstack.
     *
     * If there are no NavGraphs on the stack, returns [_graph]
     */
    internal fun getTopGraph(): NavGraph {
        val currentNode = backQueue.lastOrNull()?.destination ?: _graph!!
        return currentNode as? NavGraph ?: currentNode.parent!!
    }

    @OptIn(InternalSerializationApi::class)
    internal fun <T : Any> generateRouteFilled(route: T): String {
        val id = route::class.serializer().generateHashCode()
        val destination = findDestinationComprehensive(graph, id, true)
        // throw immediately if destination is not found within the graph
        requireNotNull(destination) {
            "Destination with route ${route::class.simpleName} cannot be found " +
                "in navigation graph $_graph"
        }
        return generateRouteWithArgs(
            route,
            // get argument typeMap
            destination.arguments.mapValues { it.value.type },
        )
    }

    internal fun navigate(deepLink: NavUri) {
        navigate(NavDeepLinkRequest(deepLink, null, null))
    }

    internal fun navigate(deepLink: NavUri, navOptions: NavOptions?) {
        navigate(NavDeepLinkRequest(deepLink, null, null), navOptions, null)
    }

    internal fun navigate(
        deepLink: NavUri,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?,
    ) {
        navigate(NavDeepLinkRequest(deepLink, null, null), navOptions, navigatorExtras)
    }

    internal fun navigate(request: NavDeepLinkRequest) {
        navigate(request, null)
    }

    internal fun navigate(request: NavDeepLinkRequest, navOptions: NavOptions?) {
        navigate(request, navOptions, null)
    }

    internal fun navigate(
        request: NavDeepLinkRequest,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?,
    ) {
        requireNotNull(_graph) {
            "Cannot navigate to $request. Navigation graph has not been set for " +
                "NavController $navController."
        }
        val currGraph = getTopGraph()
        val deepLinkMatch =
            currGraph.matchDeepLinkComprehensive(
                navDeepLinkRequest = request,
                searchChildren = true,
                searchParent = true,
                lastVisited = currGraph,
            )
        if (deepLinkMatch != null) {
            val destination = deepLinkMatch.destination
            val args = destination.addInDefaultArgs(deepLinkMatch.matchingArgs) ?: savedState()
            val node = deepLinkMatch.destination
            navController.writeIntent(request, args)
            navigate(node, args, navOptions, navigatorExtras)
        } else {
            throw IllegalArgumentException(
                "Navigation destination that matches request $request cannot be found in the " +
                    "navigation graph $_graph"
            )
        }
    }

    @OptIn(InternalSerializationApi::class)
    internal fun navigate(
        node: NavDestination,
        args: SavedState?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?,
    ) {
        navigatorState.values.forEach { state -> state.isNavigating = true }
        var popped = false
        var launchSingleTop = false
        var navigated = false
        if (navOptions != null) {
            when {
                navOptions.popUpToRoute != null ->
                    popped =
                        popBackStackInternal(
                            navOptions.popUpToRoute!!,
                            navOptions.isPopUpToInclusive(),
                            navOptions.shouldPopUpToSaveState(),
                        )
                navOptions.popUpToRouteClass != null ->
                    popped =
                        popBackStackInternal(
                            navOptions.popUpToRouteClass!!.serializer().generateHashCode(),
                            navOptions.isPopUpToInclusive(),
                            navOptions.shouldPopUpToSaveState(),
                        )
                navOptions.popUpToRouteObject != null ->
                    popped =
                        popBackStackInternal(
                            navOptions.popUpToRouteObject!!,
                            navOptions.isPopUpToInclusive(),
                            navOptions.shouldPopUpToSaveState(),
                        )
                navOptions.popUpToId != -1 ->
                    popped =
                        popBackStackInternal(
                            navOptions.popUpToId,
                            navOptions.isPopUpToInclusive(),
                            navOptions.shouldPopUpToSaveState(),
                        )
            }
        }
        val finalArgs = node.addInDefaultArgs(args)
        // Now determine what new destinations we need to add to the back stack
        if (navOptions?.shouldRestoreState() == true && backStackMap.containsKey(node.id)) {
            navigated = restoreStateInternal(node.id, finalArgs, navOptions, navigatorExtras)
        } else {
            launchSingleTop =
                navOptions?.shouldLaunchSingleTop() == true && launchSingleTopInternal(node, args)

            if (!launchSingleTop) {
                // Not a single top operation, so we're looking to add the node to the back stack
                val backStackEntry =
                    NavBackStackEntry.create(
                        navContext,
                        node,
                        finalArgs,
                        hostLifecycleState,
                        viewModel,
                    )
                val navigator =
                    _navigatorProvider.getNavigator<Navigator<NavDestination>>(node.navigatorName)
                navigateInternal(navigator, listOf(backStackEntry), navOptions, navigatorExtras) {
                    navigated = true
                    addEntryToBackStack(node, finalArgs, it)
                }
            }
        }
        updateOnBackPressedCallbackEnabledCallback()
        navigatorState.values.forEach { state -> state.isNavigating = false }
        if (popped || navigated || launchSingleTop) {
            dispatchOnDestinationChanged()
        } else {
            updateBackStackLifecycle()
        }
    }

    private fun launchSingleTopInternal(node: NavDestination, args: SavedState?): Boolean {
        val currentBackStackEntry = currentBackStackEntry
        val nodeIndex = backQueue.indexOfLast { it.destination === node }
        // early return when node isn't even in backQueue
        if (nodeIndex == -1) return false
        if (node is NavGraph) {
            // get expected singleTop stack
            val childHierarchyId = node.childHierarchy().map { it.id }.toList()
            // if actual backQueue size does not match expected singleTop stack size, we know its
            // not a single top
            if (backQueue.size - nodeIndex != childHierarchyId.size) return false
            val backQueueId = backQueue.subList(nodeIndex, backQueue.size).map { it.destination.id }
            // then make sure the backstack and singleTop stack is exact match
            if (backQueueId != childHierarchyId) return false
        } else if (node.id != currentBackStackEntry?.destination?.id) {
            return false
        }

        val tempBackQueue: ArrayDeque<NavBackStackEntry> = ArrayDeque()
        // pop from startDestination back to original node and create a new entry for each
        while (backQueue.lastIndex >= nodeIndex) {
            val oldEntry = backQueue.removeLastKt()
            unlinkChildFromParent(oldEntry)
            val newEntry = NavBackStackEntry(oldEntry, oldEntry.destination.addInDefaultArgs(args))
            tempBackQueue.addFirst(newEntry)
        }

        // add each new entry to backQueue starting from original node to startDestination
        tempBackQueue.forEach { newEntry ->
            val parent = newEntry.destination.parent
            if (parent != null) {
                val newParent = getBackStackEntry(parent.id)
                linkChildToParent(newEntry, newParent)
            }
            backQueue.add(newEntry)
        }

        // we replace NavState entries here only after backQueue has been finalized
        tempBackQueue.forEach { newEntry ->
            val navigator =
                _navigatorProvider.getNavigator<Navigator<*>>(newEntry.destination.navigatorName)
            navigator.onLaunchSingleTop(newEntry)
        }

        return true
    }

    private fun restoreStateInternal(
        id: Int,
        args: SavedState?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?,
    ): Boolean {
        if (!backStackMap.containsKey(id)) {
            return false
        }
        val backStackId = backStackMap[id]
        // Clear out the state we're going to restore so that it isn't restored a second time
        backStackMap.values.removeAll { it == backStackId }
        val backStackState = backStackStates.remove(backStackId)
        // Now restore the back stack from its saved state
        val entries = instantiateBackStack(backStackState)
        return executeRestoreState(entries, args, navOptions, navigatorExtras)
    }

    private fun restoreStateInternal(route: String): Boolean {
        var id = createRoute(route).hashCode()
        // try to match based on routePattern
        return if (backStackMap.containsKey(id)) {
            restoreStateInternal(id, null, null, null)
        } else {
            // if it didn't match, it means the route contains filled in arguments and we need
            // to find the destination that matches this route's general pattern
            val matchingDestination = findDestination(route)
            check(matchingDestination != null) {
                "Restore State failed: route $route cannot be found from the current " +
                    "destination $currentDestination"
            }

            id = matchingDestination.id
            val backStackId = backStackMap[id]
            // Clear out the state we're going to restore so that it isn't restored a second time
            backStackMap.values.removeAll { it == backStackId }
            val backStackState = backStackStates.remove(backStackId)

            val matchingDeepLink = matchingDestination.matchRoute(route)
            // check if the topmost NavBackStackEntryState contains the arguments in this
            // matchingDeepLink. If not, we didn't find the correct stack.
            val isCorrectStack =
                matchingDeepLink!!.hasMatchingArgs(backStackState?.firstOrNull()?.args)
            if (!isCorrectStack) return false
            val entries = instantiateBackStack(backStackState)
            executeRestoreState(entries, null, null, null)
        }
    }

    private fun executeRestoreState(
        entries: List<NavBackStackEntry>,
        args: SavedState?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?,
    ): Boolean {
        // Split up the entries by Navigator so we can restore them as an atomic operation
        val entriesGroupedByNavigator = mutableListOf<MutableList<NavBackStackEntry>>()
        entries
            .filterNot { entry ->
                // Skip navigation graphs - they'll be added by addEntryToBackStack()
                entry.destination is NavGraph
            }
            .forEach { entry ->
                val previousEntryList = entriesGroupedByNavigator.lastOrNull()
                val previousNavigatorName = previousEntryList?.last()?.destination?.navigatorName
                if (previousNavigatorName == entry.destination.navigatorName) {
                    // Group back to back entries associated with the same Navigator together
                    previousEntryList += entry
                } else {
                    // Create a new group for the new Navigator
                    entriesGroupedByNavigator += mutableListOf(entry)
                }
            }
        var navigated = false
        // Now actually navigate to each set of entries
        for (entryList in entriesGroupedByNavigator) {
            val navigator =
                _navigatorProvider.getNavigator<Navigator<NavDestination>>(
                    entryList.first().destination.navigatorName
                )
            var lastNavigatedIndex = 0
            navigateInternal(navigator, entryList, navOptions, navigatorExtras) { entry ->
                navigated = true
                // If this destination is part of the restored back stack,
                // pass all destinations between the last navigated entry and this one
                // to ensure that any navigation graphs are properly restored as well
                val entryIndex = entries.indexOf(entry)
                val restoredEntries =
                    if (entryIndex != -1) {
                        entries.subList(lastNavigatedIndex, entryIndex + 1).also {
                            lastNavigatedIndex = entryIndex + 1
                        }
                    } else {
                        emptyList()
                    }
                addEntryToBackStack(entry.destination, args, entry, restoredEntries)
            }
        }
        return navigated
    }

    private fun instantiateBackStack(
        backStackState: ArrayDeque<NavBackStackEntryState>?
    ): List<NavBackStackEntry> {
        val backStack = mutableListOf<NavBackStackEntry>()
        var currentDestination = backQueue.lastOrNull()?.destination ?: graph
        backStackState?.forEach { state ->
            val node = findDestinationComprehensive(currentDestination, state.destinationId, true)
            checkNotNull(node) {
                val dest = NavDestination.getDisplayName(navContext, state.destinationId)
                "Restore State failed: destination $dest cannot be found from the current " +
                    "destination $currentDestination"
            }
            backStack += state.instantiate(navContext, node, hostLifecycleState, viewModel)
            currentDestination = node
        }
        return backStack
    }

    private fun addEntryToBackStack(
        node: NavDestination,
        finalArgs: SavedState?,
        backStackEntry: NavBackStackEntry,
        restoredEntries: List<NavBackStackEntry> = emptyList(),
    ) {
        val newDest = backStackEntry.destination
        if (newDest !is FloatingWindow) {
            // We've successfully navigating to the new destination, which means
            // we should pop any FloatingWindow destination off the back stack
            // before updating the back stack with our new destination
            while (
                !backQueue.isEmpty() &&
                    backQueue.last().destination is FloatingWindow &&
                    popBackStackInternal(backQueue.last().destination.id, true)
            ) {
                // Keep popping
            }
        }

        // When you navigate() to a NavGraph, we need to ensure that a new instance
        // is always created vs reusing an existing copy of that destination
        val hierarchy = ArrayDeque<NavBackStackEntry>()
        var destination: NavDestination? = newDest
        if (node is NavGraph) {
            do {
                val parent = destination!!.parent
                if (parent != null) {
                    val entry =
                        restoredEntries.lastOrNull { restoredEntry ->
                            restoredEntry.destination == parent
                        }
                            ?: NavBackStackEntry.create(
                                navContext,
                                parent,
                                finalArgs,
                                hostLifecycleState,
                                viewModel,
                            )
                    hierarchy.addFirst(entry)
                    // Pop any orphaned copy of that navigation graph off the back stack
                    if (backQueue.isNotEmpty() && backQueue.last().destination === parent) {
                        popEntryFromBackStack(backQueue.last())
                    }
                }
                destination = parent
            } while (destination != null && destination !== node)
        }

        // Now collect the set of all intermediate NavGraphs that need to be put onto
        // the back stack. Destinations can have multiple parents, so we check referential
        // equality to ensure that same destinations with a parent that is not this _graph
        // will also have their parents added to the hierarchy.
        destination = if (hierarchy.isEmpty()) newDest else hierarchy.first().destination
        while (
            destination != null && findDestination(destination.id, destination) !== destination
        ) {
            val parent = destination.parent
            if (parent != null) {
                val args = if (finalArgs?.read { isEmpty() } == true) null else finalArgs
                val entry =
                    restoredEntries.lastOrNull { restoredEntry ->
                        restoredEntry.destination == parent
                    }
                        ?: NavBackStackEntry.create(
                            navContext,
                            parent,
                            parent.addInDefaultArgs(args),
                            hostLifecycleState,
                            viewModel,
                        )
                hierarchy.addFirst(entry)
            }
            destination = parent
        }
        val overlappingDestination: NavDestination =
            if (hierarchy.isEmpty()) newDest else hierarchy.first().destination
        // Pop any orphaned navigation graphs that don't connect to the new destinations
        while (
            !backQueue.isEmpty() &&
                backQueue.last().destination is NavGraph &&
                (backQueue.last().destination as NavGraph).nodes[overlappingDestination.id] == null
        ) {
            popEntryFromBackStack(backQueue.last())
        }

        // The _graph should always be on the top of the back stack after you navigate()
        val firstEntry = backQueue.firstOrNull() ?: hierarchy.firstOrNull()
        if (firstEntry?.destination != _graph) {
            val entry =
                restoredEntries.lastOrNull { restoredEntry ->
                    restoredEntry.destination == _graph!!
                }
                    ?: NavBackStackEntry.create(
                        navContext,
                        _graph!!,
                        _graph!!.addInDefaultArgs(finalArgs),
                        hostLifecycleState,
                        viewModel,
                    )
            hierarchy.addFirst(entry)
        }

        // Now add the parent hierarchy to the NavigatorStates and back stack
        hierarchy.forEach { entry ->
            val navigator =
                _navigatorProvider.getNavigator<Navigator<*>>(entry.destination.navigatorName)
            val navigatorBackStack =
                checkNotNull(navigatorState[navigator]) {
                    "NavigatorBackStack for ${node.navigatorName} should already be created"
                }
            navigatorBackStack.addInternal(entry)
        }
        backQueue.addAll(hierarchy)

        // And finally, add the new destination
        backQueue.add(backStackEntry)

        // Link the newly added hierarchy and entry with the parent NavBackStackEntry
        // so that we can track how many destinations are associated with each NavGraph
        (hierarchy + backStackEntry).forEach {
            val parent = it.destination.parent
            if (parent != null) {
                linkChildToParent(it, getBackStackEntry(parent.id))
            }
        }
    }

    internal fun navigate(route: String, builder: NavOptionsBuilder.() -> Unit) {
        navigate(route, navOptions(builder))
    }

    internal fun navigate(
        route: String,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras? = null,
    ) {
        requireNotNull(_graph) {
            "Cannot navigate to $route. Navigation graph has not been set for " +
                "NavController $this."
        }
        val currGraph = getTopGraph()
        val deepLinkMatch =
            currGraph.matchRouteComprehensive(
                route,
                searchChildren = true,
                searchParent = true,
                lastVisited = currGraph,
            )
        if (deepLinkMatch != null) {
            val destination = deepLinkMatch.destination
            val args = destination.addInDefaultArgs(deepLinkMatch.matchingArgs) ?: savedState()
            val node = deepLinkMatch.destination
            val request =
                NavDeepLinkRequest.Builder.fromUri(NavUri(createRoute(destination.route))).build()
            navController.writeIntent(request, args)
            navigate(node, args, navOptions, navigatorExtras)
        } else {
            throw IllegalArgumentException(
                "Navigation destination that matches route $route cannot be found in the " +
                    "navigation graph $_graph"
            )
        }
    }

    internal fun <T : Any> navigate(route: T, builder: NavOptionsBuilder.() -> Unit) {
        navigate(route, navOptions(builder))
    }

    internal fun <T : Any> navigate(
        route: T,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras? = null,
    ) {
        navigate(generateRouteFilled(route), navOptions, navigatorExtras)
    }

    internal fun saveState(): SavedState? {
        var b: SavedState? = null
        val navigatorNames = ArrayList<String>()
        val navigatorState = savedState()
        for ((name, value) in _navigatorProvider.navigators) {
            val savedState = value.onSaveState()
            if (savedState != null) {
                navigatorNames.add(name)
                navigatorState.write { putSavedState(name, savedState) }
            }
        }
        if (navigatorNames.isNotEmpty()) {
            b = savedState {
                navigatorState.write { putStringList(KEY_NAVIGATOR_STATE_NAMES, navigatorNames) }
                putSavedState(KEY_NAVIGATOR_STATE, navigatorState)
            }
        }
        if (backQueue.isNotEmpty()) {
            if (b == null) {
                b = savedState()
            }
            val backStack = arrayListOf<SavedState>()
            for (backStackEntry in this.backQueue) {
                backStack.add(NavBackStackEntryState(backStackEntry).writeToState())
            }
            b.write { putSavedStateList(KEY_BACK_STACK, backStack) }
        }
        if (backStackMap.isNotEmpty()) {
            if (b == null) {
                b = savedState()
            }
            val backStackDestIds = IntArray(backStackMap.size)
            val backStackIds = ArrayList<String>()
            var index = 0
            for ((destId, id) in backStackMap) {
                backStackDestIds[index++] = destId
                backStackIds.add(id ?: "")
            }
            b.write {
                putIntArray(KEY_BACK_STACK_DEST_IDS, backStackDestIds)
                putStringList(KEY_BACK_STACK_IDS, backStackIds)
            }
        }
        if (backStackStates.isNotEmpty()) {
            if (b == null) {
                b = savedState()
            }
            val backStackStateIds = ArrayList<String>()
            for ((id, backStackStates) in backStackStates) {
                backStackStateIds += id
                val states = arrayListOf<SavedState>()
                backStackStates.forEach { backStackState ->
                    states.add(backStackState.writeToState())
                }
                b.write { putSavedStateList(KEY_BACK_STACK_STATES_PREFIX + id, states) }
            }
            b.write { putStringList(KEY_BACK_STACK_STATES_IDS, backStackStateIds) }
        }
        return b
    }

    internal fun restoreState(navState: SavedState?) {
        if (navState == null) {
            return
        }
        navState.read {
            navigatorStateToRestore =
                if (contains(KEY_NAVIGATOR_STATE)) {
                    getSavedState(KEY_NAVIGATOR_STATE)
                } else null
            backStackToRestore =
                if (contains(KEY_BACK_STACK)) {
                    getSavedStateList(KEY_BACK_STACK).toTypedArray()
                } else null
            backStackStates.clear()
            if (contains(KEY_BACK_STACK_DEST_IDS) && contains(KEY_BACK_STACK_IDS)) {
                val backStackDestIds = getIntArray(KEY_BACK_STACK_DEST_IDS)
                val backStackIds = getStringList(KEY_BACK_STACK_IDS)
                backStackDestIds.forEachIndexed { index, id ->
                    backStackMap[id] =
                        if (backStackIds[index] != "") {
                            backStackIds[index]
                        } else {
                            null
                        }
                }
            }
            if (contains(KEY_BACK_STACK_STATES_IDS)) {
                val backStackStateIds = getStringList(KEY_BACK_STACK_STATES_IDS)
                backStackStateIds.forEach { id ->
                    if (contains(KEY_BACK_STACK_STATES_PREFIX + id)) {
                        val backStackState = getSavedStateList(KEY_BACK_STACK_STATES_PREFIX + id)
                        backStackStates[id] =
                            ArrayDeque<NavBackStackEntryState>(backStackState.size).apply {
                                for (savedState in backStackState) {
                                    add(NavBackStackEntryState(savedState))
                                }
                            }
                    }
                }
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    internal fun setLifecycleOwner(owner: LifecycleOwner) {
        if (owner == lifecycleOwner) {
            return
        }
        lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
        lifecycleOwner = owner
        owner.lifecycle.addObserver(lifecycleObserver)
    }

    internal fun setViewModelStore(viewModelStore: ViewModelStore) {
        if (viewModel == NavControllerViewModel.getInstance(viewModelStore)) {
            return
        }
        check(backQueue.isEmpty()) { "ViewModelStore should be set before setGraph call" }
        viewModel = NavControllerViewModel.getInstance(viewModelStore)
    }

    internal fun getViewModelStoreOwner(navGraphId: Int): ViewModelStoreOwner {
        checkNotNull(viewModel) {
            "You must call setViewModelStore() before calling getViewModelStoreOwner()."
        }
        val lastFromBackStack = getBackStackEntry(navGraphId)
        require(lastFromBackStack.destination is NavGraph) {
            "No NavGraph with ID $navGraphId is on the NavController's back stack"
        }
        return lastFromBackStack
    }

    internal fun getBackStackEntry(destinationId: Int): NavBackStackEntry {
        val lastFromBackStack: NavBackStackEntry? =
            backQueue.lastOrNull { entry -> entry.destination.id == destinationId }
        requireNotNull(lastFromBackStack) {
            "No destination with ID $destinationId is on the NavController's back stack. The " +
                "current destination is $currentDestination"
        }
        return lastFromBackStack
    }

    internal fun getBackStackEntry(route: String): NavBackStackEntry {
        val lastFromBackStack: NavBackStackEntry? =
            backQueue.lastOrNull { entry -> entry.destination.hasRoute(route, entry.arguments) }
        requireNotNull(lastFromBackStack) {
            "No destination with route $route is on the NavController's back stack. The " +
                "current destination is $currentDestination"
        }
        return lastFromBackStack
    }

    @OptIn(InternalSerializationApi::class)
    internal fun <T : Any> getBackStackEntry(route: KClass<T>): NavBackStackEntry {
        val id = route.serializer().generateHashCode()
        requireNotNull(findDestinationComprehensive(graph, id, true)) {
            "Destination with route ${route.simpleName} cannot be found in navigation " +
                "graph $graph"
        }
        val lastFromBackStack =
            currentBackStack.value.lastOrNull { entry -> entry.destination.id == id }
        requireNotNull(lastFromBackStack) {
            "No destination with route ${route.simpleName} is on the NavController's " +
                "back stack. The current destination is $currentDestination"
        }
        return lastFromBackStack
    }

    internal fun <T : Any> getBackStackEntry(route: T): NavBackStackEntry {
        // route contains arguments so we need to generate the populated route
        // rather than getting entry based on route pattern
        val finalRoute = generateRouteFilled(route)
        return getBackStackEntry(finalRoute)
    }

    internal val currentBackStackEntry: NavBackStackEntry?
        get() = backQueue.lastOrNull()

    internal val _currentBackStackEntryFlow: MutableSharedFlow<NavBackStackEntry> =
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    internal val previousBackStackEntry: NavBackStackEntry?
        get() {
            val iterator = backQueue.reversed().iterator()
            // throw the topmost destination away.
            if (iterator.hasNext()) {
                iterator.next()
            }
            return iterator.asSequence().firstOrNull { entry -> entry.destination !is NavGraph }
        }

    internal companion object {
        internal const val TAG = "NavController"
        private const val KEY_NAVIGATOR_STATE = "android-support-nav:controller:navigatorState"
        private const val KEY_NAVIGATOR_STATE_NAMES =
            "android-support-nav:controller:navigatorState:names"
        private const val KEY_BACK_STACK = "android-support-nav:controller:backStack"
        private const val KEY_BACK_STACK_DEST_IDS =
            "android-support-nav:controller:backStackDestIds"
        private const val KEY_BACK_STACK_IDS = "android-support-nav:controller:backStackIds"
        private const val KEY_BACK_STACK_STATES_IDS =
            "android-support-nav:controller:backStackStates"
        private const val KEY_BACK_STACK_STATES_PREFIX =
            "android-support-nav:controller:backStackStates:"
    }
}
