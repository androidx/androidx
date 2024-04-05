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
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

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
        /**
         */
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
     * Call [Navigator.navigate] while setting up a [handler] that receives callbacks
     * when [NavigatorState.push] is called.
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
     * Call [Navigator.popBackStack] while setting up a [handler] that receives callbacks
     * when [NavigatorState.pop] is called.
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

    private inner class NavControllerNavigatorState(
        val navigator: Navigator<out NavDestination>
    ) : NavigatorState() {
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
                val navigatorBackStack = checkNotNull(navigatorState[destinationNavigator]) {
                    "NavigatorBackStack for ${backStackEntry.destination.navigatorName} should " +
                        "already be created"
                }
                navigatorBackStack.push(backStackEntry)
            }
        }

        fun addInternal(backStackEntry: NavBackStackEntry) {
            super.push(backStackEntry)
        }

        override fun createBackStackEntry(
            destination: NavDestination,
            arguments: Bundle?
        ) = NavBackStackEntry.create(
            destination, arguments,
            hostLifecycleState, viewModel
        )

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
                    popBackStackFromNavigator(popUpTo) {
                        super.pop(popUpTo, saveState)
                    }
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
            popBackStack(currentDestination!!.route!!, true)
        }
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

    private fun popBackStackInternal(
        route: String,
        inclusive: Boolean,
        saveState: Boolean = false
    ): Boolean {
        if (backQueue.isEmpty()) {
            // Nothing to pop if the back stack is empty
            return false
        }

        val popOperations = mutableListOf<Navigator<*>>()
        val foundDestination = backQueue.lastOrNull { entry ->
            val hasRoute = entry.destination.hasRoute(route, entry.arguments)
            if (inclusive || !hasRoute) {
                val navigator = _navigatorProvider.getNavigator<Navigator<*>>(
                    entry.destination.navigatorName
                )
                popOperations.add(navigator)
            }
            hasRoute
        }?.destination

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
            if (savedState.isNotEmpty()) {
                val firstState = savedState.first()
                // Whether is is inclusive or not, we need to map the
                // saved state to the destination that was popped
                // as well as its parents (if it is the start destination)
                val firstStateDestination = findDestination(firstState.destinationRoute)
                generateSequence(firstStateDestination) { destination ->
                    if (destination.parent?.startDestinationRoute == destination.route) {
                        destination.parent
                    } else {
                        null
                    }
                }.takeWhile { destination ->
                    // Only add the state if it doesn't already exist
                    destination.route != null && !backStackStates.containsKey(destination.route)
                }.forEach { destination ->
                    // TODO: re-check this changed logic
                    backStackStates[destination.route!!] = savedState
                }
            }
        }
        return popped
    }

    internal actual fun popBackStackFromNavigator(popUpTo: NavBackStackEntry, onComplete: () -> Unit) {
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
                backQueue[popIndex + 1].destination.route!!,
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
        val navigator = navigatorProvider
            .getNavigator<Navigator<NavDestination>>(entry.destination.navigatorName)
        val state = navigatorState[navigator]
        // If we pop an entry with transitions, but not the graph, we will not make a call to
        // popBackStackInternal, so the graph entry will not be marked as transitioning so we
        // need to check if it still has children.
        val transitioning = state?.transitionsInProgress?.value?.contains(entry) == true ||
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

    @MainThread
    private fun clearBackStackInternal(route: String): Boolean {
        navigatorState.values.forEach { state ->
            state.isNavigating = true
        }
        val restored = restoreStateInternal(route)
        navigatorState.values.forEach { state ->
            state.isNavigating = false
        }
        return restored && popBackStackInternal(route, inclusive = true, saveState = false)
    }

    @MainThread
    public actual open fun navigateUp(): Boolean {
        // TODO If there's only one entry, then we may have deep linked into a specific destination
        return popBackStack()
    }

    /**
     * Gets the number of non-NavGraph destinations on the back stack
     */
    private val destinationCountOnBackStack: Int
        get() = backQueue.count { entry ->
            entry.destination !is NavGraph
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
            if (nextResumed != null && destination.route == nextResumed.route) {
                // Upward Lifecycle transitions need to be done afterwards so that
                // the parent navigation graph is resumed before their children
                if (currentMaxLifecycle != Lifecycle.State.RESUMED) {
                    val navigator = navigatorProvider
                        .getNavigator<Navigator<*>>(entry.destination.navigatorName)
                    val state = navigatorState[navigator]
                    val transitioning = state?.transitionsInProgress?.value?.contains(entry)
                    if (transitioning != true && parentToChildCount[entry]?.get() != 0) {
                        upwardStateTransitions[entry] = Lifecycle.State.RESUMED
                    } else {
                        upwardStateTransitions[entry] = Lifecycle.State.STARTED
                    }
                }
                if (nextStarted.isNotEmpty() &&
                    nextStarted.firstOrNull()?.route == destination.route) {
                    nextStarted.removeFirst()
                }
                nextResumed = nextResumed.parent
            } else if (nextStarted.isNotEmpty() && destination.route == nextStarted.first().route) {
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
                    if (!nextStarted.contains(it)) { nextStarted.add(it) }
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
            entries += state.transitionsInProgress.value.filter { entry ->
                !entries.contains(entry) &&
                    !entry.maxLifecycle.isAtLeast(Lifecycle.State.STARTED)
            }
        }
        // Add any STARTED entries from the backQueue. This will include the topmost
        // non-FloatingWindow destination plus every FloatingWindow destination above it.
        entries += backQueue.filter { entry ->
            !entries.contains(entry) &&
                entry.maxLifecycle.isAtLeast(Lifecycle.State.STARTED)
        }
        return entries.filter {
            it.destination !is NavGraph
        }
    }

    @MainThread
    @CallSuper
    public actual open fun setGraph(graph: NavGraph, startDestinationArgs: Bundle?) {
        if (_graph != graph) {
            _graph?.let { previousGraph ->
                // Clear all saved back stacks by iterating through a copy of the saved keys,
                // thus avoiding any concurrent modification exceptions
                val savedBackStackRoutes = ArrayList(backStackStates.keys)
                savedBackStackRoutes.forEach { route ->
                    clearBackStackInternal(route)
                }
                // Pop everything from the old graph off the back stack
                popBackStackInternal(previousGraph.route!!, true)
            }
            _graph = graph
            onGraphCreated(startDestinationArgs)
        } else {
            // first we update _graph with new instances from graph
            _graph?.nodes?.putAll(graph.nodes)
            // then we update backstack with the new instances
            backQueue.forEach { entry ->
                // we will trace this hierarchy in new graph to get new destination instance
                val hierarchy = entry.destination.hierarchy.toList().asReversed()
                val newDestination = hierarchy.fold(_graph!!) {
                        newDest: NavDestination, oldDest: NavDestination ->
                    if (oldDest == _graph && newDest == graph) {
                        // if root graph, it is already the node that matches with oldDest
                        newDest
                    } else if (newDest is NavGraph) {
                        // otherwise we walk down the hierarchy to the next child
                        newDest.findNode(oldDest.route)!!
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
        // TODO
//        navigatorStateToRestore?.let { navigatorStateToRestore ->
//            val navigatorNames = navigatorStateToRestore.getStringArrayList(
//                KEY_NAVIGATOR_STATE_NAMES
//            )
//            if (navigatorNames != null) {
//                for (name in navigatorNames) {
//                    val navigator = _navigatorProvider.getNavigator<Navigator<*>>(name)
//                    val bundle = navigatorStateToRestore.getBundle(name)
//                    if (bundle != null) {
//                        navigator.onRestoreState(bundle)
//                    }
//                }
//            }
//        }
//        backStackToRestore?.let { backStackToRestore ->
//            for (parcelable in backStackToRestore) {
//                val state = parcelable as NavBackStackEntryState
//                val node = findDestination(state.destinationId)
//                if (node == null) {
//                    val dest = NavDestination.getDisplayName(
//                        context,
//                        state.destinationId
//                    )
//                    throw IllegalStateException(
//                        "Restoring the Navigation back stack failed: destination $dest cannot be " +
//                            "found from the current destination $currentDestination"
//                    )
//                }
//                val entry = state.instantiate(node, hostLifecycleState, viewModel)
//                val navigator = _navigatorProvider.getNavigator<Navigator<*>>(node.navigatorName)
//                val navigatorBackStack = navigatorState.getOrPut(navigator) {
//                    NavControllerNavigatorState(navigator)
//                }
//                backQueue.add(entry)
//                navigatorBackStack.addInternal(entry)
//                val parent = entry.destination.parent
//                if (parent != null) {
//                    linkChildToParent(entry, getBackStackEntry(parent.route))
//                }
//            }
//        }
        // Mark all Navigators as attached
        _navigatorProvider.navigators.values.filterNot { it.isAttached }.forEach { navigator ->
            val navigatorBackStack = navigatorState.getOrPut(navigator) {
                NavControllerNavigatorState(navigator)
            }
            navigator.onAttach(navigatorBackStack)
        }
        if (_graph != null && backQueue.isEmpty()) {
            navigate(_graph!!, startDestinationArgs, null, null)
        } else {
            dispatchOnDestinationChanged()
        }
    }

    public actual open val currentDestination: NavDestination?
        get() {
            return currentBackStackEntry?.destination
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun findDestination(route: String): NavDestination? {
        if (_graph == null) {
            return null
        }
        // if not matched by routePattern, try matching with route args
        if (_graph!!.route == route) {
            return _graph
        }
        val currentNode = backQueue.lastOrNull()?.destination ?: _graph!!
        val currentGraph = if (currentNode is NavGraph) currentNode else currentNode.parent!!
        return currentGraph.findNode(route)
    }

    private fun NavDestination.findDestination(destinationRoute: String): NavDestination? {
        if (route == destinationRoute) {
            return this
        }
        val currentGraph = if (this is NavGraph) this else parent!!
        return currentGraph.findNode(destinationRoute)
    }

    @MainThread
    private fun navigate(
        node: NavDestination,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        navigatorState.values.forEach { state ->
            state.isNavigating = true
        }
        var popped = false
        var launchSingleTop = false
        var navigated = false
        if (navOptions != null) {
            when {
                navOptions.popUpToRoute != null ->
                    popped = popBackStackInternal(
                        navOptions.popUpToRoute!!,
                        navOptions.isPopUpToInclusive(),
                        navOptions.shouldPopUpToSaveState()
                    )
            }
        }
        val finalArgs = node.addInDefaultArgs(args)
        // Now determine what new destinations we need to add to the back stack
        if (navOptions?.shouldRestoreState() == true && backStackStates.containsKey(node.route)) {
            navigated = restoreStateInternal(node.route!!, finalArgs, navOptions, navigatorExtras)
        } else {
            launchSingleTop = navOptions?.shouldLaunchSingleTop() == true &&
                launchSingleTopInternal(node, args)

            if (!launchSingleTop) {
                // Not a single top operation, so we're looking to add the node to the back stack
                val backStackEntry = NavBackStackEntry.create(
                    node, finalArgs, hostLifecycleState, viewModel
                )
                val navigator = _navigatorProvider.getNavigator<Navigator<NavDestination>>(
                    node.navigatorName
                )
                navigator.navigateInternal(listOf(backStackEntry), navOptions, navigatorExtras) {
                    navigated = true
                    addEntryToBackStack(node, finalArgs, it)
                }
            }
        }
        navigatorState.values.forEach { state ->
            state.isNavigating = false
        }
        if (popped || navigated || launchSingleTop) {
            dispatchOnDestinationChanged()
        } else {
            updateBackStackLifecycle()
        }
    }

    private fun launchSingleTopInternal(
        node: NavDestination,
        args: Bundle?
    ): Boolean {
        val currentBackStackEntry = currentBackStackEntry
        val nodeRoute = if (node is NavGraph) node.findStartDestination().route else node.route
        if (nodeRoute != currentBackStackEntry?.destination?.route) return false

        val tempBackQueue: ArrayDeque<NavBackStackEntry> = ArrayDeque()
        // pop from startDestination back to original node and create a new entry for each
        backQueue.indexOfLast { it.destination === node }.let { nodeIndex ->
            while (backQueue.lastIndex >= nodeIndex) {
                val oldEntry = backQueue.removeLast()
                unlinkChildFromParent(oldEntry)
                val newEntry = NavBackStackEntry(
                    oldEntry,
                    oldEntry.destination.addInDefaultArgs(args)
                )
                tempBackQueue.addFirst(newEntry)
            }
        }

        // add each new entry to backQueue starting from original node to startDestination
        tempBackQueue.forEach { newEntry ->
            val parent = newEntry.destination.parent
            if (parent != null) {
                val newParent = getBackStackEntry(parent.route!!)
                linkChildToParent(newEntry, newParent)
            }
            backQueue.add(newEntry)
        }

        // we replace NavState entries here only after backQueue has been finalized
        tempBackQueue.forEach { newEntry ->
            val navigator = _navigatorProvider.getNavigator<Navigator<*>>(
                newEntry.destination.navigatorName
            )
            navigator.onLaunchSingleTop(newEntry)
        }

        return true
    }

    private fun restoreStateInternal(
        route: String,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ): Boolean {
        val backStackState = backStackStates.remove(route)
        // Now restore the back stack from its saved state
        val entries = instantiateBackStack(backStackState)
        return executeRestoreState(entries, args, navOptions, navigatorExtras)
    }

    private fun restoreStateInternal(route: String): Boolean {
        // try to match based on routePattern
        return if (backStackStates.containsKey(route)) {
            restoreStateInternal(route, null, null, null)
        } else {
            // if it didn't match, it means the route contains filled in arguments and we need
            // to find the destination that matches this route's general pattern
            val matchingDestination = findDestination(route)
            check(matchingDestination != null) {
                "Restore State failed: route $route cannot be found from the current " +
                    "destination $currentDestination"
            }
            val backStackState = backStackStates.remove(matchingDestination.route)

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
        entries.filterNot { entry ->
            // Skip navigation graphs - they'll be added by addEntryToBackStack()
            entry.destination is NavGraph
        }.forEach { entry ->
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
            val navigator = _navigatorProvider.getNavigator<Navigator<NavDestination>>(
                entryList.first().destination.navigatorName
            )
            var lastNavigatedIndex = 0
            navigator.navigateInternal(entryList, navOptions, navigatorExtras) { entry ->
                navigated = true
                // If this destination is part of the restored back stack,
                // pass all destinations between the last navigated entry and this one
                // to ensure that any navigation graphs are properly restored as well
                val entryIndex = entries.indexOf(entry)
                val restoredEntries = if (entryIndex != -1) {
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
            val node = currentDestination.findDestination(state.destinationRoute)
            checkNotNull(node) {
                "Restore State failed: destination ${state.id} cannot be found from the current " +
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
            while (!backQueue.isEmpty() &&
                backQueue.last().destination is FloatingWindow &&
                popBackStackInternal(backQueue.last().destination.route!!, true)
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
                    val entry = restoredEntries.lastOrNull { restoredEntry ->
                        restoredEntry.destination == parent
                    } ?: NavBackStackEntry.create(
                        parent, finalArgs, hostLifecycleState, viewModel
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
        while (destination?.route != null && findDestination(destination.route!!) !== destination) {
            val parent = destination.parent
            if (parent != null) {
                val args = if (finalArgs?.isEmpty() == true) null else finalArgs
                val entry = restoredEntries.lastOrNull { restoredEntry ->
                    restoredEntry.destination == parent
                } ?: NavBackStackEntry.create(
                    parent, parent.addInDefaultArgs(args), hostLifecycleState, viewModel
                )
                hierarchy.addFirst(entry)
            }
            destination = parent
        }
        val overlappingDestination: NavDestination =
            if (hierarchy.isEmpty())
                newDest
            else
                hierarchy.first().destination
        // Pop any orphaned navigation graphs that don't connect to the new destinations
        while (!backQueue.isEmpty() && backQueue.last().destination is NavGraph &&
            (backQueue.last().destination as NavGraph).findNode(
                overlappingDestination.route!!, false
            ) == null
        ) {
            popEntryFromBackStack(backQueue.last())
        }

        // The _graph should always be on the top of the back stack after you navigate()
        val firstEntry = backQueue.firstOrNull() ?: hierarchy.firstOrNull()
        if (firstEntry?.destination != _graph) {
            val entry = restoredEntries.lastOrNull { restoredEntry ->
                restoredEntry.destination == _graph!!
            } ?: NavBackStackEntry.create(
                _graph!!, _graph!!.addInDefaultArgs(finalArgs), hostLifecycleState, viewModel
            )
            hierarchy.addFirst(entry)
        }

        // Now add the parent hierarchy to the NavigatorStates and back stack
        hierarchy.forEach { entry ->
            val navigator = _navigatorProvider.getNavigator<Navigator<*>>(
                entry.destination.navigatorName
            )
            val navigatorBackStack = checkNotNull(navigatorState[navigator]) {
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
            val parentRoute = it.destination.parent?.route
            if (parentRoute != null) {
                linkChildToParent(it, getBackStackEntry(parentRoute))
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
        requireNotNull(_graph) {
            "Cannot navigate to $route. Navigation graph has not been set for " +
                "NavController $this."
        }
        val destination = _graph!!.findDestination(route)
        if (destination != null) {
            navigate(destination, null, navOptions, navigatorExtras)
        } else {
            throw IllegalArgumentException(
                "Navigation destination that matches route $route cannot be found in the " +
                    "navigation graph $_graph"
            )
        }
    }

    @CallSuper
    public actual open fun saveState(): Bundle? {
        var b: Bundle? = null
        // TODO
//        val navigatorNames = ArrayList<String>()
//        val navigatorState = Bundle()
//        for ((name, value) in _navigatorProvider.navigators) {
//            val savedState = value.onSaveState()
//            if (savedState != null) {
//                navigatorNames.add(name)
//                navigatorState.putBundle(name, savedState)
//            }
//        }
//        if (navigatorNames.isNotEmpty()) {
//            b = Bundle()
//            navigatorState.putStringArrayList(KEY_NAVIGATOR_STATE_NAMES, navigatorNames)
//            b.putBundle(KEY_NAVIGATOR_STATE, navigatorState)
//        }
//        if (backQueue.isNotEmpty()) {
//            if (b == null) {
//                b = Bundle()
//            }
//            val backStack = arrayOfNulls<Parcelable>(backQueue.size)
//            var index = 0
//            for (backStackEntry in this.backQueue) {
//                backStack[index++] = NavBackStackEntryState(backStackEntry)
//            }
//            b.putParcelableArray(KEY_BACK_STACK, backStack)
//        }
//        if (backStackMap.isNotEmpty()) {
//            if (b == null) {
//                b = Bundle()
//            }
//            val backStackDestIds = IntArray(backStackMap.size)
//            val backStackIds = ArrayList<String?>()
//            var index = 0
//            for ((destId, id) in backStackMap) {
//                backStackDestIds[index++] = destId
//                backStackIds += id
//            }
//            b.putIntArray(KEY_BACK_STACK_DEST_IDS, backStackDestIds)
//            b.putStringArrayList(KEY_BACK_STACK_IDS, backStackIds)
//        }
//        if (backStackStates.isNotEmpty()) {
//            if (b == null) {
//                b = Bundle()
//            }
//            val backStackStateIds = ArrayList<String>()
//            for ((id, backStackStates) in backStackStates) {
//                backStackStateIds += id
//                val states = arrayOfNulls<Parcelable>(backStackStates.size)
//                backStackStates.forEachIndexed { stateIndex, backStackState ->
//                    states[stateIndex] = backStackState
//                }
//                b.putParcelableArray(KEY_BACK_STACK_STATES_PREFIX + id, states)
//            }
//            b.putStringArrayList(KEY_BACK_STACK_STATES_IDS, backStackStateIds)
//        }
        return b
    }

    /**
     * Restores all navigation controller state from a bundle. This should be called before any
     * call to [setGraph].
     *
     * State may be saved to a bundle by calling [saveState].
     * Restoring controller state is the responsibility of a [NavHost].
     *
     * @param navState state bundle to restore
     */
    @CallSuper
    public actual open fun restoreState(navState: Bundle?) {
        // TODO
//        if (navState == null) {
//            return
//        }
//        navigatorStateToRestore = navState.getBundle(KEY_NAVIGATOR_STATE)
//        backStackToRestore = navState.getParcelableArray(KEY_BACK_STACK)
//        backStackStates.clear()
//        val backStackDestIds = navState.getIntArray(KEY_BACK_STACK_DEST_IDS)
//        val backStackIds = navState.getStringArrayList(KEY_BACK_STACK_IDS)
//        if (backStackDestIds != null && backStackIds != null) {
//            backStackDestIds.forEachIndexed { index, id ->
//                backStackMap[id] = backStackIds[index]
//            }
//        }
//        val backStackStateIds = navState.getStringArrayList(KEY_BACK_STACK_STATES_IDS)
//        backStackStateIds?.forEach { id ->
//            val backStackState = navState.getParcelableArray(KEY_BACK_STACK_STATES_PREFIX + id)
//            if (backStackState != null) {
//                backStackStates[id] = ArrayDeque<NavBackStackEntryState>(
//                    backStackState.size
//                ).apply {
//                    for (parcelable in backStackState) {
//                        add(parcelable as NavBackStackEntryState)
//                    }
//                }
//            }
//        }
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

    public actual fun getBackStackEntry(route: String): NavBackStackEntry {
        val lastFromBackStack: NavBackStackEntry? = backQueue.lastOrNull { entry ->
            entry.destination.hasRoute(route, entry.arguments)
        }
        requireNotNull(lastFromBackStack) {
            "No destination with route $route is on the NavController's back stack. The " +
                "current destination is $currentDestination"
        }
        return lastFromBackStack
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
            return iterator.asSequence().firstOrNull { entry ->
                entry.destination !is NavGraph
            }
        }

    public companion object {
//        private const val KEY_NAVIGATOR_STATE = "android-support-nav:controller:navigatorState"
//        private const val KEY_NAVIGATOR_STATE_NAMES =
//            "android-support-nav:controller:navigatorState:names"
//        private const val KEY_BACK_STACK = "android-support-nav:controller:backStack"
//        private const val KEY_BACK_STACK_DEST_IDS =
//            "android-support-nav:controller:backStackDestIds"
//        private const val KEY_BACK_STACK_IDS =
//            "android-support-nav:controller:backStackIds"
//        private const val KEY_BACK_STACK_STATES_IDS =
//            "android-support-nav:controller:backStackStates"
//        private const val KEY_BACK_STACK_STATES_PREFIX =
//            "android-support-nav:controller:backStackStates:"
    }
}

public actual inline fun NavController.createGraph(
    startDestination: String,
    route: String?,
    builder: NavGraphBuilder.() -> Unit
): NavGraph = navigatorProvider.navigation(startDestination, route, builder)
