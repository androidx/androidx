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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavDestination.Companion.createRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.childHierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.serialization.generateHashCode
import androidx.navigation.serialization.generateRouteWithArgs
import kotlin.jvm.JvmOverloads
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

public actual open class NavController {
    private var _graph: NavGraph? = null
    public actual open var graph: NavGraph
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

    private var navigatorStateToRestore: Bundle? = null
    private var backStackToRestore: List<NavBackStackEntryState>? = null
    private var deepLinkHandled = false

    private val backQueue: ArrayDeque<NavBackStackEntry> = ArrayDeque()

    private val _currentBackStack: MutableStateFlow<List<NavBackStackEntry>> =
        MutableStateFlow(emptyList())

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual val currentBackStack: StateFlow<List<NavBackStackEntry>> =
        _currentBackStack.asStateFlow()

    private val _visibleEntries: MutableStateFlow<List<NavBackStackEntry>> =
        MutableStateFlow(emptyList())

    public actual val visibleEntries: StateFlow<List<NavBackStackEntry>> =
        _visibleEntries.asStateFlow()

    private val childToParentEntries = mutableMapOf<NavBackStackEntry, NavBackStackEntry>()
    private val parentToChildCount = mutableMapOf<NavBackStackEntry, AtomicInt>()

    private fun linkChildToParent(child: NavBackStackEntry, parent: NavBackStackEntry) {
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

    private val backStackMap = mutableMapOf<Int, String?>()
    private val backStackStates = mutableMapOf<String, ArrayDeque<NavBackStackEntryState>>()
    private var lifecycleOwner: LifecycleOwner? = null
    private var viewModel: NavControllerViewModel? = null
    private val onDestinationChangedListeners = mutableListOf<OnDestinationChangedListener>()
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

    private val lifecycleObserver: LifecycleObserver = LifecycleEventObserver { _, event ->
        hostLifecycleState = event.targetState
        if (_graph != null) {
            for (entry in backQueue) {
                entry.handleLifecycleEvent(event)
            }
        }
    }

    public actual fun interface OnDestinationChangedListener {
        public actual fun onDestinationChanged(
            controller: NavController,
            destination: NavDestination,
            arguments: Bundle?
        )
    }

    private var _navigatorProvider = NavigatorProvider()

    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual open var navigatorProvider: NavigatorProvider
        get() = _navigatorProvider
        /**  */
        set(navigatorProvider) {
            check(backQueue.isEmpty()) { "NavigatorProvider must be set before setGraph call" }
            _navigatorProvider = navigatorProvider
        }

    private val navigatorState =
        mutableMapOf<Navigator<out NavDestination>, NavControllerNavigatorState>()
    private var addToBackStackHandler: ((backStackEntry: NavBackStackEntry) -> Unit)? = null
    private var popFromBackStackHandler: ((popUpTo: NavBackStackEntry) -> Unit)? = null
    private val entrySavedState = mutableMapOf<NavBackStackEntry, Boolean>()

    /**
     * Call [Navigator.navigate] while setting up a [handler] that receives callbacks when
     * [NavigatorState.push] is called.
     */
    private fun Navigator<out NavDestination>.navigateInternal(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?,
        handler: (backStackEntry: NavBackStackEntry) -> Unit = {}
    ) {
        addToBackStackHandler = handler
        navigate(entries, navOptions, navigatorExtras)
        addToBackStackHandler = null
    }

    /**
     * Call [Navigator.popBackStack] while setting up a [handler] that receives callbacks when
     * [NavigatorState.pop] is called.
     */
    private fun Navigator<out NavDestination>.popBackStackInternal(
        popUpTo: NavBackStackEntry,
        saveState: Boolean,
        handler: (popUpTo: NavBackStackEntry) -> Unit = {}
    ) {
        popFromBackStackHandler = handler
        popBackStack(popUpTo, saveState)
        popFromBackStackHandler = null
    }

    private inner class NavControllerNavigatorState(val navigator: Navigator<out NavDestination>) :
        NavigatorState() {
        override fun push(backStackEntry: NavBackStackEntry) {
            val destinationNavigator: Navigator<out NavDestination> =
                _navigatorProvider[backStackEntry.destination.navigatorName]
            if (destinationNavigator == navigator) {
                val handler = addToBackStackHandler
                if (handler != null) {
                    handler(backStackEntry)
                    addInternal(backStackEntry)
                } else {
                    // TODO handle the Navigator calling add() outside of a call to navigate()
                    println(
                        "Ignoring add of destination ${backStackEntry.destination} " +
                            "outside of the call to navigate(). "
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

        fun addInternal(backStackEntry: NavBackStackEntry) {
            super.push(backStackEntry)
        }

        override fun createBackStackEntry(destination: NavDestination, arguments: Bundle?) =
            NavBackStackEntry.create(destination, arguments, hostLifecycleState, viewModel)

        override fun pop(popUpTo: NavBackStackEntry, saveState: Boolean) {
            val destinationNavigator: Navigator<out NavDestination> =
                _navigatorProvider[popUpTo.destination.navigatorName]
            entrySavedState[popUpTo] = saveState
            if (destinationNavigator == navigator) {
                val handler = popFromBackStackHandler
                if (handler != null) {
                    handler(popUpTo)
                    super.pop(popUpTo, saveState)
                } else {
                    popBackStackFromNavigator(popUpTo) { super.pop(popUpTo, saveState) }
                }
            } else {
                navigatorState[destinationNavigator]!!.pop(popUpTo, saveState)
            }
        }

        override fun popWithTransition(popUpTo: NavBackStackEntry, saveState: Boolean) {
            super.popWithTransition(popUpTo, saveState)
        }

        override fun markTransitionComplete(entry: NavBackStackEntry) {
            val savedState = entrySavedState[entry] == true
            super.markTransitionComplete(entry)
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
            } else if (!this@NavControllerNavigatorState.isNavigating) {
                updateBackStackLifecycle()
                _currentBackStack.tryEmit(backQueue.toMutableList())
                _visibleEntries.tryEmit(populateVisibleEntries())
            }
            // else, updateBackStackLifecycle() will be called after any ongoing navigate() call
            // completes
        }

        override fun prepareForTransition(entry: NavBackStackEntry) {
            super.prepareForTransition(entry)
            if (backQueue.contains(entry)) {
                entry.maxLifecycle = Lifecycle.State.STARTED
            } else {
                throw IllegalStateException("Cannot transition entry that is not in the back stack")
            }
        }
    }

    init {
        _navigatorProvider.addNavigator(NavGraphNavigator(_navigatorProvider))
    }

    public actual open fun addOnDestinationChangedListener(listener: OnDestinationChangedListener) {
        onDestinationChangedListeners.add(listener)

        // Inform the new listener of our current state, if any
        if (backQueue.isNotEmpty()) {
            val backStackEntry = backQueue.last()
            listener.onDestinationChanged(
                this,
                backStackEntry.destination,
                backStackEntry.arguments
            )
        }
    }

    public actual open fun removeOnDestinationChangedListener(listener: OnDestinationChangedListener) {
        onDestinationChangedListeners.remove(listener)
    }

    @MainThread
    public actual open fun popBackStack(): Boolean {
        return if (backQueue.isEmpty()) {
            // Nothing to pop if the back stack is empty
            false
        } else {
            popBackStack(currentDestination!!.id, true)
        }
    }

    @MainThread
    private fun popBackStack(destinationId: Int, inclusive: Boolean): Boolean {
        return popBackStack(destinationId, inclusive, false)
    }

    /**
     * Attempts to pop the controller's back stack back to a specific destination.
     *
     * @param destinationId The topmost destination to retain
     * @param inclusive Whether the given destination should also be popped.
     * @param saveState Whether the back stack and the state of all destinations between the current
     *   destination and the [destinationId] should be saved for later restoration via
     *   [NavOptions.Builder.setRestoreState] or the `restoreState` attribute using the same
     *   [destinationId] (note: this matching ID is true whether [inclusive] is true or false).
     * @return true if the stack was popped at least once and the user has been navigated to another
     *   destination, false otherwise
     */
    @PublishedApi
    @MainThread
    internal fun popBackStack(
        destinationId: Int,
        inclusive: Boolean,
        saveState: Boolean
    ): Boolean {
        val popped = popBackStackInternal(destinationId, inclusive, saveState)
        // Only return true if the pop succeeded and we've dispatched
        // the change to a new destination
        return popped && dispatchOnDestinationChanged()
    }

    @MainThread
    @JvmOverloads
    public actual fun popBackStack(
        route: String,
        inclusive: Boolean,
        saveState: Boolean
    ): Boolean {
        val popped = popBackStackInternal(route, inclusive, saveState)
        // Only return true if the pop succeeded and we've dispatched
        // the change to a new destination
        return popped && dispatchOnDestinationChanged()
    }

    @MainThread
    @JvmOverloads
    public actual inline fun <reified T : Any> popBackStack(
        inclusive: Boolean,
        saveState: Boolean
    ): Boolean {
        val id = serializer<T>().generateHashCode()
        requireNotNull(graph.findDestinationComprehensive(id, true)) {
            "Destination with route ${T::class.simpleName} cannot be found in navigation " +
                "graph $graph"
        }
        return popBackStack(id, inclusive, saveState)
    }

    @MainThread
    @JvmOverloads
    public actual fun <T : Any> popBackStack(
        route: T,
        inclusive: Boolean,
        saveState: Boolean
    ): Boolean {
        val popped = popBackStackInternal(route, inclusive, saveState)
        // Only return true if the pop succeeded and we've dispatched
        // the change to a new destination
        return popped && dispatchOnDestinationChanged()
    }

    @MainThread
    private fun popBackStackInternal(
        destinationId: Int,
        inclusive: Boolean,
        saveState: Boolean = false
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
            val destinationName = NavDestination.getDisplayName(destinationId)
            println(
                "Ignoring popBackStack to destination $destinationName as it was not found " +
                    "on the current back stack"
            )
            return false
        }
        return executePopOperations(popOperations, foundDestination, inclusive, saveState)
    }

    private fun <T : Any> popBackStackInternal(
        route: T,
        inclusive: Boolean,
        saveState: Boolean = false
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
    private fun popBackStackInternal(
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
            println(
                "Ignoring popBackStack to route $route as it was not found " +
                    "on the current back stack"
            )
            return false
        }
        return executePopOperations(popOperations, foundDestination, inclusive, saveState)
    }

    private fun executePopOperations(
        popOperations: List<Navigator<*>>,
        foundDestination: NavDestination,
        inclusive: Boolean,
        saveState: Boolean,
    ): Boolean {
        var popped = false
        val savedState = ArrayDeque<NavBackStackEntryState>()
        for (navigator in popOperations) {
            var receivedPop = false
            navigator.popBackStackInternal(backQueue.last(), saveState) { entry ->
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
        return popped
    }

    internal actual fun popBackStackFromNavigator(
        popUpTo: NavBackStackEntry,
        onComplete: () -> Unit
    ) {
        val popIndex = backQueue.indexOf(popUpTo)
        if (popIndex < 0) {
            println(
                "Ignoring pop of $popUpTo as it was not found on the current back stack"
            )
            return
        }
        if (popIndex + 1 != backQueue.size) {
            // There's other destinations stacked on top of this destination that
            // we need to pop first
            popBackStackInternal(
                backQueue[popIndex + 1].destination.id,
                inclusive = true,
                saveState = false
            )
        }
        // Now record the pop of the actual entry - we don't use popBackStackInternal
        // here since we're being called from the Navigator already
        popEntryFromBackStack(popUpTo)
        onComplete()
        dispatchOnDestinationChanged()
    }

    private fun popEntryFromBackStack(
        popUpTo: NavBackStackEntry,
        saveState: Boolean = false,
        savedState: ArrayDeque<NavBackStackEntryState> = ArrayDeque()
    ) {
        val entry = backQueue.last()
        check(entry == popUpTo) {
            "Attempted to pop ${popUpTo.destination}, which is not the top of the back stack " +
                "(${entry.destination})"
        }
        backQueue.removeLast()
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

    @MainThread
    public actual fun clearBackStack(route: String): Boolean {
        val cleared = clearBackStackInternal(route)
        // Only return true if the clear succeeded and we've dispatched
        // the change to a new destination
        return cleared && dispatchOnDestinationChanged()
    }

    /**
     * Clears any saved state associated with [destinationId] that was previously saved via
     * [popBackStack] when using a `saveState` value of `true`.
     *
     * @param destinationId The ID of the destination previously used with [popBackStack] with a
     *   `saveState`value of `true`
     * @return true if the saved state of the stack associated with [destinationId] was cleared.
     */
    @MainThread
    public fun clearBackStack(destinationId: Int): Boolean {
        val cleared = clearBackStackInternal(destinationId)
        // Only return true if the clear succeeded and we've dispatched
        // the change to a new destination
        return cleared && dispatchOnDestinationChanged()
    }

    @MainThread
    public actual inline fun <reified T : Any> clearBackStack(): Boolean =
        clearBackStack(serializer<T>().generateHashCode())

    @MainThread
    public actual fun <T : Any> clearBackStack(route: T): Boolean {
        // route contains arguments so we need to generate and clear with the populated route
        // rather than clearing based on route pattern
        val finalRoute = generateRouteFilled(route)
        val cleared = clearBackStackInternal(finalRoute)
        // Only return true if the clear succeeded and we've dispatched
        // the change to a new destination
        return cleared && dispatchOnDestinationChanged()
    }

    @MainThread
    private fun clearBackStackInternal(destinationId: Int): Boolean {
        navigatorState.values.forEach { state -> state.isNavigating = true }
        val restored =
            restoreStateInternal(destinationId, null, navOptions { restoreState = true }, null)
        navigatorState.values.forEach { state -> state.isNavigating = false }
        return restored && popBackStackInternal(destinationId, inclusive = true, saveState = false)
    }

    @MainThread
    private fun clearBackStackInternal(route: String): Boolean {
        navigatorState.values.forEach { state -> state.isNavigating = true }
        val restored = restoreStateInternal(route)
        navigatorState.values.forEach { state -> state.isNavigating = false }
        return restored && popBackStackInternal(route, inclusive = true, saveState = false)
    }

    @MainThread
    public actual open fun navigateUp(): Boolean {
        // TODO If there's only one entry, then we may have deep linked into a specific destination
        return popBackStack()
    }

    /** Gets the number of non-NavGraph destinations on the back stack */
    private val destinationCountOnBackStack: Int
        get() = backQueue.count { entry -> entry.destination !is NavGraph }

    private var dispatchReentrantCount = 0
    private val backStackEntriesToDispatch = mutableListOf<NavBackStackEntry>()

    /**
     * Dispatch changes to all OnDestinationChangedListeners.
     *
     * If the back stack is empty, no events get dispatched.
     *
     * @return If changes were dispatched.
     */
    private fun dispatchOnDestinationChanged(): Boolean {
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
                for (listener in onDestinationChangedListeners) {
                    listener.onDestinationChanged(
                        this,
                        backStackEntry.destination,
                        backStackEntry.arguments
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
        // First determine what the current resumed destination is and, if and only if
        // the current resumed destination is a FloatingWindow, what destinations are
        // underneath it that must remain started.
        var nextResumed: NavDestination? = backStack.last().destination
        val nextStarted: MutableList<NavDestination> = mutableListOf()
        if (nextResumed is FloatingWindow) {
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
                if (destination !is FloatingWindow && destination !is NavGraph) {
                    break
                }
            }
        }
        // First iterate downward through the stack, applying downward Lifecycle
        // transitions and capturing any upward Lifecycle transitions to apply afterwards.
        // This ensures proper nesting where parent navigation graphs are started before
        // their children and stopped only after their children are stopped.
        val upwardStateTransitions = HashMap<NavBackStackEntry, Lifecycle.State>()
        var iterator = backStack.reversed().iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val currentMaxLifecycle = entry.maxLifecycle
            val destination = entry.destination
            if (nextResumed != null && destination.id == nextResumed.id) {
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
                if (nextStarted.firstOrNull()?.id == destination.id) nextStarted.removeFirst()
                nextResumed = nextResumed.parent
            } else if (nextStarted.isNotEmpty() && destination.id == nextStarted.first().id) {
                val started = nextStarted.removeFirst()
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

    @MainThread
    @CallSuper
    public actual open fun setGraph(graph: NavGraph, startDestinationArgs: Bundle?) {
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

    @MainThread
    private fun onGraphCreated(startDestinationArgs: Bundle?) {
        navigatorStateToRestore?.let { navigatorStateToRestore ->
            val navigatorNames = navigatorStateToRestore.getStringArray(
                KEY_NAVIGATOR_STATE_NAMES
            )
            if (navigatorNames != null) {
                for (name in navigatorNames) {
                    val navigator = _navigatorProvider.getNavigator<Navigator<*>>(name!!)
                    val bundle = navigatorStateToRestore.getBundle(name)
                    if (bundle != null) {
                        navigator.onRestoreState(bundle)
                    }
                }
            }
        }
        backStackToRestore?.let { backStackToRestore ->
            for (state in backStackToRestore) {
                val node = findDestination(state.destinationId)
                if (node == null) {
                    val dest = NavDestination.getDisplayName(
                        state.destinationId
                    )
                    throw IllegalStateException(
                        "Restoring the Navigation back stack failed: destination $dest cannot be " +
                            "found from the current destination $currentDestination"
                    )
                }
                val entry = state.instantiate(node, hostLifecycleState, viewModel)
                val navigator = _navigatorProvider.getNavigator<Navigator<*>>(node.navigatorName)
                val navigatorBackStack = navigatorState.getOrPut(navigator) {
                    NavControllerNavigatorState(navigator)
                }
                backQueue.add(entry)
                navigatorBackStack.addInternal(entry)
                val parent = entry.destination.parent
                if (parent != null) {
                    linkChildToParent(entry, getBackStackEntry(parent.id))
                }
            }
            this.backStackToRestore = null
        }

        // Mark all Navigators as attached
        _navigatorProvider.navigators.values
            .filterNot { it.isAttached }
            .forEach { navigator ->
                val navigatorBackStack =
                    navigatorState.getOrPut(navigator) { NavControllerNavigatorState(navigator) }
                navigator.onAttach(navigatorBackStack)
            }
        if (_graph != null && backQueue.isEmpty()) {
            val deepLinked =
                !deepLinkHandled && handleDeepLink()
            if (!deepLinked) {
                // Navigate to the first destination in the graph
                // if we haven't deep linked to a destination
                navigate(_graph!!, startDestinationArgs, null, null)
            }
        } else {
            dispatchOnDestinationChanged()
        }
    }

    @MainThread
    private fun handleDeepLink(): Boolean {
        // TODO handle deep link
        return false
    }

    public actual open val currentDestination: NavDestination?
        get() {
            return currentBackStackEntry?.destination
        }

    /** Recursively searches through parents */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun findDestination(destinationId: Int): NavDestination? {
        if (_graph == null) {
            return null
        }
        if (_graph!!.id == destinationId) {
            return _graph
        }
        val currentNode = backQueue.lastOrNull()?.destination ?: _graph!!
        return currentNode.findDestinationComprehensive(destinationId, false)
    }

    /**
     * Recursively searches through parents. If [searchChildren] is true, also recursively searches
     * children.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun NavDestination.findDestinationComprehensive(
        destinationId: Int,
        searchChildren: Boolean
    ): NavDestination? {

        if (id == destinationId) {
            return this
        }
        val currentGraph = if (this is NavGraph) this else parent!!
        return currentGraph.findNodeComprehensive(destinationId, currentGraph, searchChildren)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun findDestination(route: String): NavDestination? {
        if (_graph == null) {
            return null
        }
        // if not matched by routePattern, try matching with route args
        if (_graph!!.route == route || _graph!!.matchDeepLink(route) != null) {
            return _graph
        }
        val currentNode = backQueue.lastOrNull()?.destination ?: _graph!!
        val currentGraph = if (currentNode is NavGraph) currentNode else currentNode.parent!!
        return currentGraph.findNode(route)
    }

    // Finds destination within _graph including its children and
    // generates a route filled with args based on the serializable object.
    // Throws if destination with `route` is not found
    @OptIn(InternalSerializationApi::class)
    private fun <T : Any> generateRouteFilled(route: T): String {
        val id = route::class.serializer().generateHashCode()
        val destination = graph.findDestinationComprehensive(id, true)
        // throw immediately if destination is not found within the graph
        requireNotNull(destination) {
            "Destination with route ${route::class.simpleName} cannot be found " +
                "in navigation graph $_graph"
        }
        return generateRouteWithArgs(
            route,
            // get argument typeMap
            destination.arguments.mapValues { it.value.type }
        )
    }

    @MainThread
    private fun navigateInternal(
        route: String,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        requireNotNull(_graph) {
            "Cannot navigate to $route. Navigation graph has not been set for " +
                "NavController $this."
        }
        val deepLinkMatch = _graph!!.matchDeepLink(route)
        if (deepLinkMatch != null) {
            val destination = deepLinkMatch.destination
            val args = destination.addInDefaultArgs(deepLinkMatch.matchingArgs) ?: Bundle()
            val node = deepLinkMatch.destination
            navigate(node, args, navOptions, navigatorExtras)
        } else {
            throw IllegalArgumentException(
                "Navigation destination that matches route $route cannot be found in the " +
                    "navigation graph $_graph"
            )
        }
    }

    @OptIn(InternalSerializationApi::class)
    @MainThread
    private fun navigate(
        node: NavDestination,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
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
                            navOptions.shouldPopUpToSaveState()
                        )
                navOptions.popUpToRouteClass != null ->
                    popped =
                        popBackStackInternal(
                            navOptions.popUpToRouteClass!!.serializer().generateHashCode(),
                            navOptions.isPopUpToInclusive(),
                            navOptions.shouldPopUpToSaveState()
                        )
                navOptions.popUpToRouteObject != null ->
                    popped =
                        popBackStackInternal(
                            navOptions.popUpToRouteObject!!,
                            navOptions.isPopUpToInclusive(),
                            navOptions.shouldPopUpToSaveState()
                        )
                navOptions.popUpToId != -1 ->
                    popped =
                        popBackStackInternal(
                            navOptions.popUpToId,
                            navOptions.isPopUpToInclusive(),
                            navOptions.shouldPopUpToSaveState()
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
                        node,
                        finalArgs,
                        hostLifecycleState,
                        viewModel
                    )
                val navigator =
                    _navigatorProvider.getNavigator<Navigator<NavDestination>>(node.navigatorName)
                navigator.navigateInternal(listOf(backStackEntry), navOptions, navigatorExtras) {
                    navigated = true
                    addEntryToBackStack(node, finalArgs, it)
                }
            }
        }
        navigatorState.values.forEach { state -> state.isNavigating = false }
        if (popped || navigated || launchSingleTop) {
            dispatchOnDestinationChanged()
        } else {
            updateBackStackLifecycle()
        }
    }

    private fun launchSingleTopInternal(node: NavDestination, args: Bundle?): Boolean {
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
            val oldEntry = backQueue.removeLast()
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
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
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

            val matchingDeepLink = matchingDestination.matchDeepLink(route)
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
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
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
            navigator.navigateInternal(entryList, navOptions, navigatorExtras) { entry ->
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
            val node = currentDestination.findDestinationComprehensive(state.destinationId, true)
            checkNotNull(node) {
                val dest = NavDestination.getDisplayName(state.destinationId)
                "Restore State failed: destination $dest cannot be found from the current " +
                    "destination $currentDestination"
            }
            backStack += state.instantiate(node, hostLifecycleState, viewModel)
            currentDestination = node
        }
        return backStack
    }

    private fun addEntryToBackStack(
        node: NavDestination,
        finalArgs: Bundle?,
        backStackEntry: NavBackStackEntry,
        restoredEntries: List<NavBackStackEntry> = emptyList()
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
                                parent,
                                finalArgs,
                                hostLifecycleState,
                                viewModel
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
        while (destination != null && findDestination(destination.id) !== destination) {
            val parent = destination.parent
            if (parent != null) {
                val args = if (finalArgs?.isEmpty() == true) null else finalArgs
                val entry =
                    restoredEntries.lastOrNull { restoredEntry ->
                        restoredEntry.destination == parent
                    }
                        ?: NavBackStackEntry.create(
                            parent,
                            parent.addInDefaultArgs(args),
                            hostLifecycleState,
                            viewModel
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
                        _graph!!,
                        _graph!!.addInDefaultArgs(finalArgs),
                        hostLifecycleState,
                        viewModel
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

    @MainThread
    public actual fun navigate(route: String, builder: NavOptionsBuilder.() -> Unit) {
        navigate(route, navOptions(builder))
    }

    @MainThread
    @JvmOverloads
    public actual fun navigate(
        route: String,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        navigateInternal(
            route = route,
            navOptions = navOptions,
            navigatorExtras = navigatorExtras
        )
    }

    @MainThread
    public actual fun <T : Any> navigate(route: T, builder: NavOptionsBuilder.() -> Unit) {
        navigate(route, navOptions(builder))
    }

    @MainThread
    @JvmOverloads
    public actual fun <T : Any> navigate(
        route: T,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        val finalRoute = generateRouteFilled(route)
        navigate(
            route = finalRoute,
            navOptions = navOptions,
            navigatorExtras = navigatorExtras
        )
    }

    @CallSuper
    public actual open fun saveState(): Bundle? {
        var b: Bundle? = null
        val navigatorNames = ArrayList<String?>()
        val navigatorState = Bundle()
        for ((name, value) in _navigatorProvider.navigators) {
            val savedState = value.onSaveState()
            if (savedState != null) {
                navigatorNames.add(name)
                navigatorState.putBundle(name, savedState)
            }
        }
        if (navigatorNames.isNotEmpty()) {
            b = Bundle()
            navigatorState.putStringArrayList(KEY_NAVIGATOR_STATE_NAMES, navigatorNames)
            b.putBundle(KEY_NAVIGATOR_STATE, navigatorState)
        }
        if (backQueue.isNotEmpty()) {
            if (b == null) {
                b = Bundle()
            }
            val backStack = arrayOfNulls<Bundle>(backQueue.size)
            var index = 0
            for (backStackEntry in this.backQueue) {
                backStack[index++] = NavBackStackEntryState(backStackEntry).toBundle()
            }
            b.putBundleArray(KEY_BACK_STACK, backStack)
        }
        if (backStackMap.isNotEmpty()) {
            if (b == null) {
                b = Bundle()
            }
            val backStackDestIds = IntArray(backStackMap.size)
            val backStackIds = ArrayList<String?>()
            var index = 0
            for ((destId, id) in backStackMap) {
                backStackDestIds[index++] = destId
                backStackIds += id
            }
            b.putIntArray(KEY_BACK_STACK_DEST_IDS, backStackDestIds)
            b.putStringArrayList(KEY_BACK_STACK_IDS, backStackIds)
        }
        if (backStackStates.isNotEmpty()) {
            if (b == null) {
                b = Bundle()
            }
            val backStackStateIds = ArrayList<String?>()
            for ((id, backStackStates) in backStackStates) {
                backStackStateIds += id
                val states = arrayOfNulls<Bundle>(backStackStates.size)
                backStackStates.forEachIndexed { stateIndex, backStackState ->
                    states[stateIndex] = backStackState.toBundle()
                }
                b.putBundleArray(KEY_BACK_STACK_STATES_PREFIX + id, states)
            }
            b.putStringArrayList(KEY_BACK_STACK_STATES_IDS, backStackStateIds)
        }
        if (deepLinkHandled) {
            if (b == null) {
                b = Bundle()
            }
            b.putBoolean(KEY_DEEP_LINK_HANDLED, deepLinkHandled)
        }
        return b
    }

    @CallSuper
    public actual open fun restoreState(navState: Bundle?) {
        if (navState == null) {
            return
        }
        navigatorStateToRestore = navState.getBundle(KEY_NAVIGATOR_STATE)
        backStackToRestore = navState.getBundleArray(KEY_BACK_STACK)
            ?.mapNotNull { NavBackStackEntryState.fromBundle(it) }
        backStackStates.clear()
        val backStackDestIds = navState.getIntArray(KEY_BACK_STACK_DEST_IDS)
        val backStackIds = navState.getStringArrayList(KEY_BACK_STACK_IDS)
        if (backStackDestIds != null && backStackIds != null) {
            backStackDestIds.forEachIndexed { index, id ->
                backStackMap[id] = backStackIds[index]
            }
        }
        val backStackStateIds = navState.getStringArrayList(KEY_BACK_STACK_STATES_IDS)
        backStackStateIds?.filterNotNull()?.forEach { id ->
            val backStackState = navState.getBundleArray(KEY_BACK_STACK_STATES_PREFIX + id)
                ?.mapNotNull { NavBackStackEntryState.fromBundle(it) }
            if (backStackState != null) {
                backStackStates[id] =
                    ArrayDeque<NavBackStackEntryState>(backStackState.size).apply {
                        for (i in backStackState) {
                            add(i)
                        }
                    }
            }
        }
        deepLinkHandled = navState.getBoolean(KEY_DEEP_LINK_HANDLED)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual open fun setLifecycleOwner(owner: LifecycleOwner) {
        if (owner == lifecycleOwner) {
            return
        }
        lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
        lifecycleOwner = owner
        owner.lifecycle.addObserver(lifecycleObserver)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual open fun setViewModelStore(viewModelStore: ViewModelStore) {
        if (viewModel == NavControllerViewModel.getInstance(viewModelStore)) {
            return
        }
        check(backQueue.isEmpty()) { "ViewModelStore should be set before setGraph call" }
        viewModel = NavControllerViewModel.getInstance(viewModelStore)
    }

    /**
     * Gets the topmost [NavBackStackEntry] for a destination id.
     *
     * This is always safe to use with [the current destination][currentDestination] or
     * [its parent][NavDestination.parent] or grandparent navigation graphs as these destinations
     * are guaranteed to be on the back stack.
     *
     * @param destinationId ID of a destination that exists on the back stack
     * @throws IllegalArgumentException if the destination is not on the back stack
     */
    private fun getBackStackEntry(destinationId: Int): NavBackStackEntry {
        val lastFromBackStack: NavBackStackEntry? =
            backQueue.lastOrNull { entry -> entry.destination.id == destinationId }
        requireNotNull(lastFromBackStack) {
            "No destination with ID $destinationId is on the NavController's back stack. The " +
                "current destination is $currentDestination"
        }
        return lastFromBackStack
    }

    public actual fun getBackStackEntry(route: String): NavBackStackEntry {
        val lastFromBackStack: NavBackStackEntry? =
            backQueue.lastOrNull { entry -> entry.destination.hasRoute(route, entry.arguments) }
        requireNotNull(lastFromBackStack) {
            "No destination with route $route is on the NavController's back stack. The " +
                "current destination is $currentDestination"
        }
        return lastFromBackStack
    }

    public actual inline fun <reified T : Any> getBackStackEntry(): NavBackStackEntry {
        val id = serializer<T>().generateHashCode()
        requireNotNull(graph.findDestinationComprehensive(id, true)) {
            "Destination with route ${T::class.simpleName} cannot be found in navigation " +
                "graph $graph"
        }
        val lastFromBackStack =
            currentBackStack.value.lastOrNull { entry -> entry.destination.id == id }
        requireNotNull(lastFromBackStack) {
            "No destination with route ${T::class.simpleName} is on the NavController's " +
                "back stack. The current destination is $currentDestination"
        }
        return lastFromBackStack
    }

    public actual fun <T : Any> getBackStackEntry(route: T): NavBackStackEntry {
        // route contains arguments so we need to generate the populated route
        // rather than getting entry based on route pattern
        val finalRoute = generateRouteFilled(route)
        return getBackStackEntry(finalRoute)
    }

    public actual open val currentBackStackEntry: NavBackStackEntry?
        get() = backQueue.lastOrNull()

    private val _currentBackStackEntryFlow: MutableSharedFlow<NavBackStackEntry> =
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    public actual val currentBackStackEntryFlow: Flow<NavBackStackEntry> =
        _currentBackStackEntryFlow.asSharedFlow()

    public actual open val previousBackStackEntry: NavBackStackEntry?
        get() {
            val iterator = backQueue.reversed().iterator()
            // throw the topmost destination away.
            if (iterator.hasNext()) {
                iterator.next()
            }
            return iterator.asSequence().firstOrNull { entry -> entry.destination !is NavGraph }
        }

    public companion object {
        private const val KEY_NAVIGATOR_STATE =
            "multiplatform-support-nav:controller:navigatorState"
        private const val KEY_NAVIGATOR_STATE_NAMES =
            "multiplatform-support-nav:controller:navigatorState:names"
        private const val KEY_BACK_STACK = "multiplatform-support-nav:controller:backStack"
        private const val KEY_BACK_STACK_DEST_IDS =
            "multiplatform-support-nav:controller:backStackDestIds"
        private const val KEY_BACK_STACK_IDS =
            "multiplatform-support-nav:controller:backStackIds"
        private const val KEY_BACK_STACK_STATES_IDS =
            "multiplatform-support-nav:controller:backStackStates"
        private const val KEY_BACK_STACK_STATES_PREFIX =
            "multiplatform-support-nav:controller:backStackStates:"
        private const val KEY_DEEP_LINK_HANDLED: String =
            "multiplatform-support-nav:controller:deepLinkHandled"
    }
}
