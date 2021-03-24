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

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.annotation.NavigationRes
import androidx.annotation.RestrictTo
import androidx.core.app.TaskStackBuilder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * NavController manages app navigation within a [NavHost].
 *
 * Apps will generally obtain a controller directly from a host, or by using one of the utility
 * methods on the [Navigation] class rather than create a controller directly.
 *
 * Navigation flows and destinations are determined by the
 * [navigation graph][NavGraph] owned by the controller. These graphs are typically
 * [inflated][.getNavInflater] from an Android resource, but, like views, they can also
 * be constructed or combined programmatically or for the case of dynamic navigation structure.
 * (For example, if the navigation structure of the application is determined by live data obtained'
 * from a remote server.)
 */
public open class NavController(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val context: Context
) {
    private var activity: Activity? = generateSequence(context) {
        if (it is ContextWrapper) { it.baseContext } else null
    }.firstOrNull { it is Activity } as Activity?

    private var inflater: NavInflater? = null

    private var _graph: NavGraph? = null
    public open var graph: NavGraph
        /**
         * Gets the topmost navigation graph associated with this NavController.
         *
         * @see NavController.setGraph
         * @throws IllegalStateException if called before `setGraph()`.
         */
        get() {
            checkNotNull(_graph) { "You must call setGraph() before calling getGraph()" }
            return _graph as NavGraph
        }

        /**
         * Sets the [navigation graph][NavGraph] to the specified graph.
         * Any current navigation graph data (including back stack) will be replaced.
         *
         * The graph can be retrieved later via [.getGraph].
         *
         * @param graph graph to set
         * @see NavController.setGraph
         * @see NavController.getGraph
         */
        @CallSuper
        set(graph) {
            setGraph(graph, null)
        }

    private var navigatorStateToRestore: Bundle? = null
    private var backStackToRestore: Array<Parcelable>? = null
    private var deepLinkHandled = false

    /**
     * Retrieve the current back stack.
     *
     * @return The current back stack.
     * @hide
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open val backQueue: ArrayDeque<NavBackStackEntry> = ArrayDeque()
    private var lifecycleOwner: LifecycleOwner? = null
    private var viewModel: NavControllerViewModel? = null
    private val onDestinationChangedListeners =
        mutableListOf<OnDestinationChangedListener>()

    private val lifecycleObserver: LifecycleObserver = LifecycleEventObserver { _, event ->
        if (_graph != null) {
            for (entry in backQueue) {
                entry.handleLifecycleEvent(event)
            }
        }
    }

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                popBackStack()
            }
        }
    private var enableOnBackPressedCallback = true

    /**
     * OnDestinationChangedListener receives a callback when the
     * [.getCurrentDestination] or its arguments change.
     */
    public fun interface OnDestinationChangedListener {
        /**
         * Callback for when the [.getCurrentDestination] or its arguments change.
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

    private var _navigatorProvider = NavigatorProvider()
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open var navigatorProvider: NavigatorProvider
        /**
         * Retrieve the NavController's [NavigatorProvider]. All [Navigators][Navigator] used
         * to construct the [navigation graph][NavGraph] for this nav controller should be added
         * to this navigator provider before the graph is constructed.
         *
         * Generally, the Navigators are set for you by the [NavHost] hosting this NavController
         * and you do not need to manually interact with the navigator provider.
         *
         * @return The [NavigatorProvider] used by this NavController.
         */
        get() = _navigatorProvider
        /**
         * Sets the [navigator provider][NavigatorProvider] to the specified provider. This can
         * only be called before the graph is set via `setGraph()`.
         *
         * @param navigatorProvider [NavigatorProvider] to set
         * @throws IllegalStateException If this is called after `setGraph()`
         * @hide
         */
        set(navigatorProvider) {
            check(backQueue.isEmpty()) { "NavigatorProvider must be set before setGraph call" }
            _navigatorProvider = navigatorProvider
        }

    /**
     * Constructs a new controller for a given [Context]. Controllers should not be
     * used outside of their context and retain a hard reference to the context supplied.
     * If you need a global controller, pass [Context.getApplicationContext].
     *
     * Apps should generally not construct controllers, instead obtain a relevant controller
     * directly from a navigation host via [NavHost.getNavController] or by using one of
     * the utility methods on the [Navigation] class.
     *
     * Note that controllers that are not constructed with an [Activity] context
     * (or a wrapped activity context) will only be able to navigate to
     * [new tasks][android.content.Intent.FLAG_ACTIVITY_NEW_TASK] or
     * [new document tasks][android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT] when
     * navigating to new activities.
     *
     * @param context context for this controller
     */
    init {
        _navigatorProvider.addNavigator(NavGraphNavigator(_navigatorProvider))
        _navigatorProvider.addNavigator(ActivityNavigator(context))
    }

    /**
     * Adds an [OnDestinationChangedListener] to this controller to receive a callback
     * whenever the [.getCurrentDestination] or its arguments change.
     *
     * The current destination, if any, will be immediately sent to your listener.
     *
     * @param listener the listener to receive events
     */
    public open fun addOnDestinationChangedListener(listener: OnDestinationChangedListener) {
        // Inform the new listener of our current state, if any
        if (backQueue.isNotEmpty()) {
            val backStackEntry = backQueue.last()
            listener.onDestinationChanged(
                this,
                backStackEntry.destination,
                backStackEntry.arguments
            )
        }
        onDestinationChangedListeners.add(listener)
    }

    /**
     * Removes an [OnDestinationChangedListener] from this controller.
     * It will no longer receive callbacks.
     *
     * @param listener the listener to remove
     */
    public open fun removeOnDestinationChangedListener(listener: OnDestinationChangedListener) {
        onDestinationChangedListeners.remove(listener)
    }

    /**
     * Attempts to pop the controller's back stack. Analogous to when the user presses
     * the system [Back][android.view.KeyEvent.KEYCODE_BACK] button when the associated
     * navigation host has focus.
     *
     * @return true if the stack was popped at least once and the user has been navigated to
     * another destination, false otherwise
     */
    public open fun popBackStack(): Boolean {
        return if (backQueue.isEmpty()) {
            // Nothing to pop if the back stack is empty
            false
        } else {
            popBackStack(currentDestination!!.id, true)
        }
    }

    /**
     * Attempts to pop the controller's back stack back to a specific destination.
     *
     * @param destinationId The topmost destination to retain
     * @param inclusive Whether the given destination should also be popped.
     *
     * @return true if the stack was popped at least once and the user has been navigated to
     * another destination, false otherwise
     */
    public open fun popBackStack(@IdRes destinationId: Int, inclusive: Boolean): Boolean {
        val popped = popBackStackInternal(destinationId, inclusive)
        // Only return true if the pop succeeded and we've dispatched
        // the change to a new destination
        return popped && dispatchOnDestinationChanged()
    }

    /**
     * Attempts to pop the controller's back stack back to a specific destination. This does
     * **not** handle calling [.dispatchOnDestinationChanged]
     *
     * @param destinationId The topmost destination to retain
     * @param inclusive Whether the given destination should also be popped.
     *
     * @return true if the stack was popped at least once, false otherwise
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun popBackStackInternal(@IdRes destinationId: Int, inclusive: Boolean): Boolean {
        if (backQueue.isEmpty()) {
            // Nothing to pop if the back stack is empty
            return false
        }
        val popOperations = mutableListOf<Navigator<*>>()
        val iterator = backQueue.reversed().iterator()
        var foundDestination = false
        while (iterator.hasNext()) {
            val destination = iterator.next().destination
            val navigator = _navigatorProvider.getNavigator<Navigator<*>>(
                destination.navigatorName
            )
            if (inclusive || destination.id != destinationId) {
                popOperations.add(navigator)
            }
            if (destination.id == destinationId) {
                foundDestination = true
                break
            }
        }
        if (!foundDestination) {
            // We were passed a destinationId that doesn't exist on our back stack.
            // Better to ignore the popBackStack than accidentally popping the entire stack
            val destinationName = NavDestination.getDisplayName(
                context, destinationId
            )
            Log.i(
                TAG,
                "Ignoring popBackStack to destination $destinationName as it was not found " +
                    "on the current back stack"
            )
            return false
        }
        var popped = false
        for (navigator in popOperations) {
            if (navigator.popBackStack()) {
                val entry = backQueue.removeLast()
                if (entry.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                    entry.maxLifecycle = Lifecycle.State.DESTROYED
                }
                viewModel?.clear(entry.id)
                popped = true
            } else {
                // The pop did not complete successfully, so stop immediately
                break
            }
        }
        updateOnBackPressedCallbackEnabled()
        return popped
    }

    /**
     * Attempts to navigate up in the navigation hierarchy. Suitable for when the
     * user presses the "Up" button marked with a left (or start)-facing arrow in the upper left
     * (or starting) corner of the app UI.
     *
     * The intended behavior of Up differs from [Back][.popBackStack] when the user
     * did not reach the current destination from the application's own task. e.g. if the user
     * is viewing a document or link in the current app in an activity hosted on another app's
     * task where the user clicked the link. In this case the current activity (determined by the
     * context used to create this NavController) will be [finished][Activity.finish] and
     * the user will be taken to an appropriate destination in this app on its own task.
     *
     * @return true if navigation was successful, false otherwise
     */
    public open fun navigateUp(): Boolean {
        return if (destinationCountOnBackStack == 1) {
            // If there's only one entry, then we've deep linked into a specific destination
            // on another task so we need to find the parent and start our task from there
            val currentDestination = currentDestination
            var destId = currentDestination!!.id
            var parent = currentDestination.parent
            while (parent != null) {
                if (parent.startDestination != destId) {
                    val args = Bundle()
                    if (activity != null && activity!!.intent != null) {
                        val data = activity!!.intent.data

                        // We were started via a URI intent.
                        if (data != null) {
                            // Include the original deep link Intent so the Destinations can
                            // synthetically generate additional arguments as necessary.
                            args.putParcelable(
                                KEY_DEEP_LINK_INTENT,
                                activity!!.intent
                            )
                            val matchingDeepLink = _graph!!.matchDeepLink(
                                NavDeepLinkRequest(activity!!.intent)
                            )
                            if (matchingDeepLink != null) {
                                val destinationArgs = matchingDeepLink.destination.addInDefaultArgs(
                                    matchingDeepLink.matchingArgs
                                )
                                args.putAll(destinationArgs)
                            }
                        }
                    }
                    val parentIntents = NavDeepLinkBuilder(this)
                        .setDestination(parent.id)
                        .setArguments(args)
                        .createTaskStackBuilder()
                    parentIntents.startActivities()
                    activity?.finish()
                    return true
                }
                destId = parent.id
                parent = parent.parent
            }
            // We're already at the startDestination of the graph so there's no 'Up' to go to
            false
        } else {
            popBackStack()
        }
    }

    /**
     * Gets the number of non-NavGraph destinations on the back stack
     */
    private val destinationCountOnBackStack: Int
        get() = backQueue.count { entry ->
            entry.destination !is NavGraph
        }

    /**
     * Dispatch changes to all OnDestinationChangedListeners.
     *
     * If the back stack is empty, no events get dispatched.
     *
     * @return If changes were dispatched.
     */
    private fun dispatchOnDestinationChanged(): Boolean {
        // We never want to leave NavGraphs on the top of the stack
        while (!backQueue.isEmpty() &&
            backQueue.last().destination is NavGraph &&
            popBackStackInternal(backQueue.last().destination.id, true)
        ) {
            // Keep popping
        }
        if (!backQueue.isEmpty()) {
            // First determine what the current resumed destination is and, if and only if
            // the current resumed destination is a FloatingWindow, what destination is
            // underneath it that must remain started.
            var nextResumed: NavDestination? = backQueue.last().destination
            var nextStarted: NavDestination? = null
            if (nextResumed is FloatingWindow) {
                // Find the next destination in the back stack as that destination
                // should still be STARTED when the FloatingWindow destination is above it.
                val iterator = backQueue.reversed().iterator()
                while (iterator.hasNext()) {
                    val destination = iterator.next().destination
                    if (destination !is NavGraph && destination !is FloatingWindow) {
                        nextStarted = destination
                        break
                    }
                }
            }
            // First iterate downward through the stack, applying downward Lifecycle
            // transitions and capturing any upward Lifecycle transitions to apply afterwards.
            // This ensures proper nesting where parent navigation graphs are started before
            // their children and stopped only after their children are stopped.
            val upwardStateTransitions = HashMap<NavBackStackEntry, Lifecycle.State>()
            var iterator = backQueue.reversed().iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val currentMaxLifecycle = entry.maxLifecycle
                val destination = entry.destination
                if (nextResumed != null && destination.id == nextResumed.id) {
                    // Upward Lifecycle transitions need to be done afterwards so that
                    // the parent navigation graph is resumed before their children
                    if (currentMaxLifecycle != Lifecycle.State.RESUMED) {
                        upwardStateTransitions[entry] = Lifecycle.State.RESUMED
                    }
                    nextResumed = nextResumed.parent
                } else if (nextStarted != null && destination.id == nextStarted.id) {
                    if (currentMaxLifecycle == Lifecycle.State.RESUMED) {
                        // Downward transitions should be done immediately so children are
                        // paused before their parent navigation graphs
                        entry.maxLifecycle = Lifecycle.State.STARTED
                    } else if (currentMaxLifecycle != Lifecycle.State.STARTED) {
                        // Upward Lifecycle transitions need to be done afterwards so that
                        // the parent navigation graph is started before their children
                        upwardStateTransitions[entry] = Lifecycle.State.STARTED
                    }
                    nextStarted = nextStarted.parent
                } else {
                    entry.maxLifecycle = Lifecycle.State.CREATED
                }
            }
            // Apply all upward Lifecycle transitions by iterating through the stack again,
            // this time applying the new lifecycle to the parent navigation graphs first
            iterator = backQueue.iterator()
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

            // Now call all registered OnDestinationChangedListener instances
            val backStackEntry = backQueue.last()
            for (listener in onDestinationChangedListeners) {
                listener.onDestinationChanged(
                    this,
                    backStackEntry.destination,
                    backStackEntry.arguments
                )
            }
            return true
        }
        return false
    }

    /**
     * Returns the [inflater][NavInflater] for this controller.
     *
     * @return inflater for loading navigation resources
     */
    public open val navInflater: NavInflater by lazy {
        inflater ?: NavInflater(context, _navigatorProvider)
    }

    /**
     * Sets the [navigation graph][NavGraph] to the specified resource.
     * Any current navigation graph data (including back stack) will be replaced.
     *
     * The inflated graph can be retrieved via [.getGraph].
     *
     * @param graphResId resource id of the navigation graph to inflate
     *
     * @see NavController.getNavInflater
     * @see NavController.setGraph
     * @see NavController.getGraph
     */
    @CallSuper
    public open fun setGraph(@NavigationRes graphResId: Int) {
        setGraph(navInflater.inflate(graphResId), null)
    }

    /**
     * Sets the [navigation graph][NavGraph] to the specified resource.
     * Any current navigation graph data (including back stack) will be replaced.
     *
     * The inflated graph can be retrieved via [.getGraph].
     *
     * @param graphResId resource id of the navigation graph to inflate
     * @param startDestinationArgs arguments to send to the start destination of the graph
     *
     * @see NavController.getNavInflater
     * @see NavController.setGraph
     * @see NavController.getGraph
     */
    @CallSuper
    public open fun setGraph(@NavigationRes graphResId: Int, startDestinationArgs: Bundle?) {
        setGraph(navInflater.inflate(graphResId), startDestinationArgs)
    }

    /**
     * Sets the [navigation graph][NavGraph] to the specified graph.
     * Any current navigation graph data (including back stack) will be replaced.
     *
     * The graph can be retrieved later via [.getGraph].
     *
     * @param graph graph to set
     * @see NavController.setGraph
     * @see NavController.getGraph
     */
    @CallSuper
    public open fun setGraph(graph: NavGraph, startDestinationArgs: Bundle?) {
        _graph?.let { previousGraph ->
            // Pop everything from the old graph off the back stack
            popBackStackInternal(previousGraph.id, true)
        }
        _graph = graph
        onGraphCreated(startDestinationArgs)
    }

    private fun onGraphCreated(startDestinationArgs: Bundle?) {
        navigatorStateToRestore?.let { navigatorStateToRestore ->
            val navigatorNames = navigatorStateToRestore.getStringArrayList(
                KEY_NAVIGATOR_STATE_NAMES
            )
            if (navigatorNames != null) {
                for (name in navigatorNames) {
                    val navigator = _navigatorProvider.getNavigator<Navigator<*>>(name)
                    val bundle = navigatorStateToRestore.getBundle(name)
                    if (bundle != null) {
                        navigator.onRestoreState(bundle)
                    }
                }
            }
        }
        backStackToRestore?.let { backStackToRestore ->
            for (parcelable in backStackToRestore) {
                val state = parcelable as NavBackStackEntryState
                val node = findDestination(state.destinationId)
                if (node == null) {
                    val dest = NavDestination.getDisplayName(
                        context,
                        state.destinationId
                    )
                    throw IllegalStateException(
                        "Restoring the Navigation back stack failed: destination $dest cannot be " +
                            "found from the current destination $currentDestination"
                    )
                }
                val args = state.args?.apply {
                    classLoader = context.classLoader
                }
                val entry = NavBackStackEntry(
                    context, node, args,
                    lifecycleOwner, viewModel,
                    state.uuid, state.savedState
                )
                backQueue.add(entry)
            }
            updateOnBackPressedCallbackEnabled()
            this.backStackToRestore = null
        }
        if (_graph != null && backQueue.isEmpty()) {
            val deepLinked =
                !deepLinkHandled && activity != null && handleDeepLink(activity!!.intent)
            if (!deepLinked) {
                // Navigate to the first destination in the graph
                // if we haven't deep linked to a destination
                navigate(_graph!!, startDestinationArgs, null, null)
            }
        } else {
            dispatchOnDestinationChanged()
        }
    }

    /**
     * Checks the given Intent for a Navigation deep link and navigates to the deep link if present.
     * This is called automatically for you the first time you set the graph if you've passed in an
     * [Activity] as the context when constructing this NavController, but should be manually
     * called if your Activity receives new Intents in [Activity.onNewIntent].
     *
     * The types of Intents that are supported include:
     *
     * Intents created by [NavDeepLinkBuilder] or
     * [.createDeepLink]. This assumes that the current graph shares
     * the same hierarchy to get to the deep linked destination as when the deep link was
     * constructed.
     * Intents that include a [data Uri][Intent.getData]. This Uri will be checked
     * against the Uri patterns in the [NavDeepLinks][NavDeepLink] added via
     * [NavDestination.addDeepLink].
     *
     * The [navigation graph][.getGraph] should be set before calling this method.
     * @param intent The Intent that may contain a valid deep link
     * @return True if the navigation controller found a valid deep link and navigated to it.
     * @throws IllegalStateException if deep link cannot be accessed from the current destination
     * @see NavDestination.addDeepLink
     */
    public open fun handleDeepLink(intent: Intent?): Boolean {
        if (intent == null) {
            return false
        }
        val extras = intent.extras
        var deepLink = extras?.getIntArray(KEY_DEEP_LINK_IDS)
        var deepLinkArgs = extras?.getParcelableArrayList<Bundle>(KEY_DEEP_LINK_ARGS)
        val globalArgs = Bundle()
        val deepLinkExtras = extras?.getBundle(KEY_DEEP_LINK_EXTRAS)
        if (deepLinkExtras != null) {
            globalArgs.putAll(deepLinkExtras)
        }
        if ((deepLink == null || deepLink.isEmpty()) && intent.data != null) {
            val matchingDeepLink = _graph!!.matchDeepLink(NavDeepLinkRequest(intent))
            if (matchingDeepLink != null) {
                val destination = matchingDeepLink.destination
                deepLink = destination.buildDeepLinkIds()
                deepLinkArgs = null
                val destinationArgs = destination.addInDefaultArgs(matchingDeepLink.matchingArgs)
                globalArgs.putAll(destinationArgs)
            }
        }
        if (deepLink == null || deepLink.isEmpty()) {
            return false
        }
        val invalidDestinationDisplayName = findInvalidDestinationDisplayNameInDeepLink(deepLink)
        if (invalidDestinationDisplayName != null) {
            Log.i(
                TAG,
                "Could not find destination $invalidDestinationDisplayName in the " +
                    "navigation graph, ignoring the deep link from $intent"
            )
            return false
        }
        globalArgs.putParcelable(KEY_DEEP_LINK_INTENT, intent)
        val args = arrayOfNulls<Bundle>(deepLink.size)
        for (index in args.indices) {
            val arguments = Bundle()
            arguments.putAll(globalArgs)
            if (deepLinkArgs != null) {
                val deepLinkArguments = deepLinkArgs[index]
                if (deepLinkArguments != null) {
                    arguments.putAll(deepLinkArguments)
                }
            }
            args[index] = arguments
        }
        val flags = intent.flags
        if (flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0 &&
            flags and Intent.FLAG_ACTIVITY_CLEAR_TASK == 0
        ) {
            // Someone called us with NEW_TASK, but we don't know what state our whole
            // task stack is in, so we need to manually restart the whole stack to
            // ensure we're in a predictably good state.
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            val taskStackBuilder = TaskStackBuilder
                .create(context)
                .addNextIntentWithParentStack(intent)
            taskStackBuilder.startActivities()
            activity?.let { activity ->
                activity.finish()
                // Disable second animation in case where the Activity is created twice.
                activity.overridePendingTransition(0, 0)
            }
            return true
        }
        if (flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0) {
            // Start with a cleared task starting at our root when we're on our own task
            if (!backQueue.isEmpty()) {
                popBackStackInternal(_graph!!.id, true)
            }
            var index = 0
            while (index < deepLink.size) {
                val destinationId = deepLink[index]
                val arguments = args[index++]
                val node = findDestination(destinationId)
                if (node == null) {
                    val dest = NavDestination.getDisplayName(
                        context, destinationId
                    )
                    throw IllegalStateException(
                        "Deep Linking failed: destination $dest cannot be found from the current " +
                            "destination $currentDestination"
                    )
                }
                navigate(
                    node, arguments,
                    NavOptions.Builder().setEnterAnim(0).setExitAnim(0).build(), null
                )
            }
            return true
        }
        // Assume we're on another apps' task and only start the final destination
        var graph = _graph
        for (i in deepLink.indices) {
            val destinationId = deepLink[i]
            val arguments = args[i]
            val node = if (i == 0) _graph else graph!!.findNode(destinationId)
            if (node == null) {
                val dest = NavDestination.getDisplayName(context, destinationId)
                throw IllegalStateException(
                    "Deep Linking failed: destination $dest cannot be found in graph $graph"
                )
            }
            if (i != deepLink.size - 1) {
                // We're not at the final NavDestination yet, so keep going through the chain
                if (node is NavGraph) {
                    graph = node
                    // Automatically go down the navigation graph when
                    // the start destination is also a NavGraph
                    while (graph!!.findNode(graph.startDestination) is NavGraph) {
                        graph = graph.findNode(graph.startDestination) as NavGraph?
                    }
                }
            } else {
                // Navigate to the last NavDestination, clearing any existing destinations
                navigate(
                    node,
                    arguments,
                    NavOptions.Builder()
                        .setPopUpTo(_graph!!.id, true)
                        .setEnterAnim(0)
                        .setExitAnim(0)
                        .build(),
                    null
                )
            }
        }
        deepLinkHandled = true
        return true
    }

    /**
     * Looks through the deep link for invalid destinations, returning the display name of
     * any invalid destinations in the deep link array.
     *
     * @param deepLink array of deep link IDs that are expected to match the graph
     * @return The display name of the first destination not found in the graph or null if
     * all destinations were found in the graph.
     */
    private fun findInvalidDestinationDisplayNameInDeepLink(deepLink: IntArray): String? {
        var graph = _graph
        for (i in deepLink.indices) {
            val destinationId = deepLink[i]
            val node =
                (
                    if (i == 0)
                        if (_graph!!.id == destinationId) _graph
                        else null
                    else
                        graph!!.findNode(destinationId)
                    ) ?: return NavDestination.getDisplayName(context, destinationId)
            if (i != deepLink.size - 1) {
                // We're not at the final NavDestination yet, so keep going through the chain
                if (node is NavGraph) {
                    graph = node
                    // Automatically go down the navigation graph when
                    // the start destination is also a NavGraph
                    while (graph!!.findNode(graph.startDestination) is NavGraph) {
                        graph = graph.findNode(graph.startDestination) as NavGraph?
                    }
                }
            }
        }
        // We found every destination in the deepLink array, yay!
        return null
    }

    /**
     * Gets the current destination.
     */
    public open val currentDestination: NavDestination?
        get() {
            return currentBackStackEntry?.destination
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun findDestination(@IdRes destinationId: Int): NavDestination? {
        if (_graph == null) {
            return null
        }
        if (_graph!!.id == destinationId) {
            return _graph
        }
        val currentNode = if (backQueue.isEmpty()) _graph!! else backQueue.last().destination
        val currentGraph = if (currentNode is NavGraph) currentNode else currentNode.parent!!
        return currentGraph.findNode(destinationId)
    }

    /**
     * Navigate to a destination from the current navigation graph. This supports both navigating
     * via an [action][NavDestination.getAction] and directly navigating to a destination.
     *
     * @param resId an [action][NavDestination.getAction] id or a destination id to
     * navigate to
     *
     * @throws IllegalStateException if there is no current navigation node
     * @throws IllegalArgumentException if the desired destination cannot be found from the
     *                                  current destination
     */
    public open fun navigate(@IdRes resId: Int) {
        navigate(resId, null)
    }

    /**
     * Navigate to a destination from the current navigation graph. This supports both navigating
     * via an [action][NavDestination.getAction] and directly navigating to a destination.
     *
     * @param resId an [action][NavDestination.getAction] id or a destination id to
     * navigate to
     * @param args arguments to pass to the destination
     *
     * @throws IllegalStateException if there is no current navigation node
     * @throws IllegalArgumentException if the desired destination cannot be found from the
     *                                  current destination
     */
    public open fun navigate(@IdRes resId: Int, args: Bundle?) {
        navigate(resId, args, null)
    }

    /**
     * Navigate to a destination from the current navigation graph. This supports both navigating
     * via an [action][NavDestination.getAction] and directly navigating to a destination.
     *
     * @param resId an [action][NavDestination.getAction] id or a destination id to
     * navigate to
     * @param args arguments to pass to the destination
     * @param navOptions special options for this navigation operation
     *
     * @throws IllegalStateException if there is no current navigation node
     * @throws IllegalArgumentException if the desired destination cannot be found from the
     *                                  current destination
     */
    public open fun navigate(@IdRes resId: Int, args: Bundle?, navOptions: NavOptions?) {
        navigate(resId, args, navOptions, null)
    }

    /**
     * Navigate to a destination from the current navigation graph. This supports both navigating
     * via an [action][NavDestination.getAction] and directly navigating to a destination.
     *
     * @param resId an [action][NavDestination.getAction] id or a destination id to
     * navigate to
     * @param args arguments to pass to the destination
     * @param navOptions special options for this navigation operation
     * @param navigatorExtras extras to pass to the Navigator
     *
     * @throws IllegalStateException if there is no current navigation node
     * @throws IllegalArgumentException if the desired destination cannot be found from the
     *                                  current destination
     */
    public open fun navigate(
        @IdRes resId: Int,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        var finalNavOptions = navOptions
        val currentNode = (
            if (backQueue.isEmpty())
                _graph
            else
                backQueue.last().destination
            ) ?: throw IllegalStateException("no current navigation node")
        @IdRes
        var destId = resId
        val navAction = currentNode.getAction(resId)
        var combinedArgs: Bundle? = null
        if (navAction != null) {
            if (finalNavOptions == null) {
                finalNavOptions = navAction.navOptions
            }
            destId = navAction.destinationId
            val navActionArgs = navAction.defaultArguments
            if (navActionArgs != null) {
                combinedArgs = Bundle()
                combinedArgs.putAll(navActionArgs)
            }
        }
        if (args != null) {
            if (combinedArgs == null) {
                combinedArgs = Bundle()
            }
            combinedArgs.putAll(args)
        }
        if (destId == 0 && finalNavOptions != null && finalNavOptions.popUpTo != -1) {
            popBackStack(finalNavOptions.popUpTo, finalNavOptions.isPopUpToInclusive())
            return
        }
        require(destId != 0) {
            "Destination id == 0 can only be used in conjunction with a valid navOptions.popUpTo"
        }
        val node = findDestination(destId)
        if (node == null) {
            val dest = NavDestination.getDisplayName(context, destId)
            require(navAction == null) {
                "Navigation destination $dest referenced from action " +
                    "${NavDestination.getDisplayName(context, resId)} cannot be found from " +
                    "the current destination $currentNode"
            }
            throw IllegalArgumentException(
                "Navigation action/destination $dest cannot be found from the current " +
                    "destination $currentNode"
            )
        }
        navigate(node, combinedArgs, finalNavOptions, navigatorExtras)
    }

    /**
     * Navigate to a destination via the given deep link [Uri].
     * [NavDestination.hasDeepLink] should be called on
     * [the navigation graph][.getGraph] prior to calling this method to check if the deep
     * link is valid. If an invalid deep link is given, an [IllegalArgumentException] will be
     * thrown.
     *
     * @param deepLink deepLink to the destination reachable from the current NavGraph
     * @see NavController.navigate
     */
    public open fun navigate(deepLink: Uri) {
        navigate(NavDeepLinkRequest(deepLink, null, null))
    }

    /**
     * Navigate to a destination via the given deep link [Uri].
     * [NavDestination.hasDeepLink] should be called on
     * [the navigation graph][.getGraph] prior to calling this method to check if the deep
     * link is valid. If an invalid deep link is given, an [IllegalArgumentException] will be
     * thrown.
     *
     * @param deepLink deepLink to the destination reachable from the current NavGraph
     * @param navOptions special options for this navigation operation
     * @see NavController.navigate
     */
    public open fun navigate(deepLink: Uri, navOptions: NavOptions?) {
        navigate(NavDeepLinkRequest(deepLink, null, null), navOptions, null)
    }

    /**
     * Navigate to a destination via the given deep link [Uri].
     * [NavDestination.hasDeepLink] should be called on
     * [the navigation graph][.getGraph] prior to calling this method to check if the deep
     * link is valid. If an invalid deep link is given, an [IllegalArgumentException] will be
     * thrown.
     *
     * @param deepLink deepLink to the destination reachable from the current NavGraph
     * @param navOptions special options for this navigation operation
     * @param navigatorExtras extras to pass to the Navigator
     * @see NavController.navigate
     */
    public open fun navigate(
        deepLink: Uri,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        navigate(NavDeepLinkRequest(deepLink, null, null), navOptions, navigatorExtras)
    }

    /**
     * Navigate to a destination via the given [NavDeepLinkRequest].
     * [NavDestination.hasDeepLink] should be called on
     * [the navigation graph][.getGraph] prior to calling this method to check if the deep
     * link is valid. If an invalid deep link is given, an [IllegalArgumentException] will be
     * thrown.
     *
     * @param request deepLinkRequest to the destination reachable from the current NavGraph
     *
     * @throws IllegalArgumentException if the given deep link request is invalid
     */
    public open fun navigate(request: NavDeepLinkRequest) {
        navigate(request, null)
    }

    /**
     * Navigate to a destination via the given [NavDeepLinkRequest].
     * [NavDestination.hasDeepLink] should be called on
     * [the navigation graph][.getGraph] prior to calling this method to check if the deep
     * link is valid. If an invalid deep link is given, an [IllegalArgumentException] will be
     * thrown.
     *
     * @param request deepLinkRequest to the destination reachable from the current NavGraph
     * @param navOptions special options for this navigation operation
     *
     * @throws IllegalArgumentException if the given deep link request is invalid
     */
    public open fun navigate(request: NavDeepLinkRequest, navOptions: NavOptions?) {
        navigate(request, navOptions, null)
    }

    /**
     * Navigate to a destination via the given [NavDeepLinkRequest].
     * [NavDestination.hasDeepLink] should be called on
     * [the navigation graph][.getGraph] prior to calling this method to check if the deep
     * link is valid. If an invalid deep link is given, an [IllegalArgumentException] will be
     * thrown.
     *
     * @param request deepLinkRequest to the destination reachable from the current NavGraph
     * @param navOptions special options for this navigation operation
     * @param navigatorExtras extras to pass to the Navigator
     *
     * @throws IllegalArgumentException if the given deep link request is invalid
     */
    public open fun navigate(
        request: NavDeepLinkRequest,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        val deepLinkMatch = _graph!!.matchDeepLink(request)
        if (deepLinkMatch != null) {
            val destination = deepLinkMatch.destination
            val args = destination.addInDefaultArgs(deepLinkMatch.matchingArgs)
            val node = deepLinkMatch.destination
            navigate(node, args, navOptions, navigatorExtras)
        } else {
            throw IllegalArgumentException(
                "Navigation destination that matches request $request cannot be found in the " +
                    "navigation graph $_graph"
            )
        }
    }

    private fun navigate(
        node: NavDestination,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        var popped = false
        var launchSingleTop = false
        if (navOptions != null) {
            if (navOptions.popUpTo != -1) {
                popped = popBackStackInternal(
                    navOptions.popUpTo,
                    navOptions.isPopUpToInclusive()
                )
            }
        }
        val navigator = _navigatorProvider.getNavigator<Navigator<NavDestination>>(
            node.navigatorName
        )
        val finalArgs = node.addInDefaultArgs(args)
        val newDest = navigator.navigate(
            node, finalArgs,
            navOptions, navigatorExtras
        )
        if (newDest != null) {
            if (newDest !is FloatingWindow) {
                // We've successfully navigating to the new destination, which means
                // we should pop any FloatingWindow destination off the back stack
                // before updating the back stack with our new destination
                while (!backQueue.isEmpty() &&
                    backQueue.last().destination is FloatingWindow &&
                    popBackStackInternal(backQueue.last().destination.id, true)
                ) {
                    // Keep popping
                }
            }

            // When you navigate() to a NavGraph, we need to ensure that a new instance
            // is always created vs reusing an existing copy of that destination
            val hierarchy = ArrayDeque<NavBackStackEntry>()
            var destination = newDest
            if (node is NavGraph) {
                do {
                    val parent = destination!!.parent
                    if (parent != null) {
                        val entry = NavBackStackEntry(
                            context, parent,
                            finalArgs, lifecycleOwner, viewModel
                        )
                        hierarchy.addFirst(entry)
                        // Pop any orphaned copy of that navigation graph off the back stack
                        if (backQueue.isNotEmpty() && backQueue.last().destination === parent) {
                            popBackStackInternal(parent.id, true)
                        }
                    }
                    destination = parent
                } while (destination != null && destination !== node)
            }

            // Now collect the set of all intermediate NavGraphs that need to be put onto
            // the back stack
            destination = if (hierarchy.isEmpty()) newDest else hierarchy.first().destination
            while (destination != null && findDestination(destination.id) == null) {
                val parent = destination.parent
                if (parent != null) {
                    val entry = NavBackStackEntry(
                        context, parent, finalArgs, lifecycleOwner, viewModel
                    )
                    hierarchy.addFirst(entry)
                }
                destination = parent
            }
            val overlappingDestination: NavDestination =
                if (hierarchy.isEmpty())
                    newDest
                else
                    hierarchy.last().destination
            // Pop any orphaned navigation graphs that don't connect to the new destinations
            while (!backQueue.isEmpty() && backQueue.last().destination is NavGraph &&
                (backQueue.last().destination as NavGraph).findNode(
                        overlappingDestination.id, false
                    ) == null && popBackStackInternal(backQueue.last().destination.id, true)
            ) {
                // Keep popping
            }
            backQueue.addAll(hierarchy)
            // The _graph should always be on the back stack after you navigate()
            if (backQueue.isEmpty() || backQueue.first().destination !== _graph) {
                val entry = NavBackStackEntry(
                    context, _graph!!, finalArgs, lifecycleOwner, viewModel
                )
                backQueue.addFirst(entry)
            }
            // And finally, add the new destination with its default args
            val newBackStackEntry = NavBackStackEntry(
                context, newDest, newDest.addInDefaultArgs(finalArgs), lifecycleOwner, viewModel
            )
            backQueue.add(newBackStackEntry)
        } else if (navOptions != null && navOptions.shouldLaunchSingleTop()) {
            launchSingleTop = true
            val singleTopBackStackEntry = backQueue.last()
            singleTopBackStackEntry.replaceArguments(finalArgs)
        }
        updateOnBackPressedCallbackEnabled()
        if (popped || newDest != null || launchSingleTop) {
            dispatchOnDestinationChanged()
        }
    }

    /**
     * Navigate via the given [NavDirections]
     *
     * @param directions directions that describe this navigation operation
     */
    public open fun navigate(directions: NavDirections) {
        navigate(directions.actionId, directions.arguments, null)
    }

    /**
     * Navigate via the given [NavDirections]
     *
     * @param directions directions that describe this navigation operation
     * @param navOptions special options for this navigation operation
     */
    public open fun navigate(directions: NavDirections, navOptions: NavOptions?) {
        navigate(directions.actionId, directions.arguments, navOptions)
    }

    /**
     * Navigate via the given [NavDirections]
     *
     * @param directions directions that describe this navigation operation
     * @param navigatorExtras extras to pass to the [Navigator]
     */
    public open fun navigate(directions: NavDirections, navigatorExtras: Navigator.Extras) {
        navigate(directions.actionId, directions.arguments, null, navigatorExtras)
    }

    /**
     * Create a deep link to a destination within this NavController.
     *
     * @return a [NavDeepLinkBuilder] suitable for constructing a deep link
     */
    public open fun createDeepLink(): NavDeepLinkBuilder {
        return NavDeepLinkBuilder(this)
    }

    /**
     * Saves all navigation controller state to a Bundle.
     *
     * State may be restored from a bundle returned from this method by calling
     * [.restoreState]. Saving controller state is the responsibility
     * of a [NavHost].
     *
     * @return saved state for this controller
     */
    @CallSuper
    public open fun saveState(): Bundle? {
        var b: Bundle? = null
        val navigatorNames = ArrayList<String>()
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
            val backStack = arrayOfNulls<Parcelable>(backQueue.size)
            var index = 0
            for (backStackEntry in this.backQueue) {
                backStack[index++] = NavBackStackEntryState(backStackEntry)
            }
            b.putParcelableArray(KEY_BACK_STACK, backStack)
        }
        if (deepLinkHandled) {
            if (b == null) {
                b = Bundle()
            }
            b.putBoolean(KEY_DEEP_LINK_HANDLED, deepLinkHandled)
        }
        return b
    }

    /**
     * Restores all navigation controller state from a bundle. This should be called before any
     * call to [.setGraph].
     *
     * State may be saved to a bundle by calling [.saveState].
     * Restoring controller state is the responsibility of a [NavHost].
     *
     * @param navState state bundle to restore
     */
    @CallSuper
    public open fun restoreState(navState: Bundle?) {
        if (navState == null) {
            return
        }
        navState.classLoader = context.classLoader
        navigatorStateToRestore = navState.getBundle(KEY_NAVIGATOR_STATE)
        backStackToRestore = navState.getParcelableArray(KEY_BACK_STACK)
        deepLinkHandled = navState.getBoolean(KEY_DEEP_LINK_HANDLED)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open fun setLifecycleOwner(owner: LifecycleOwner) {
        if (owner == lifecycleOwner) {
            return
        }
        lifecycleOwner = owner
        lifecycleOwner!!.lifecycle.addObserver(lifecycleObserver)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open fun setOnBackPressedDispatcher(dispatcher: OnBackPressedDispatcher) {
        checkNotNull(lifecycleOwner) {
            "You must call setLifecycleOwner() before calling setOnBackPressedDispatcher()"
        }
        // Remove the callback from any previous dispatcher
        onBackPressedCallback.remove()
        // Then add it to the new dispatcher
        dispatcher.addCallback(lifecycleOwner!!, onBackPressedCallback)

        // Make sure that listener for updating the NavBackStackEntry lifecycles comes after
        // the dispatcher
        lifecycleOwner!!.lifecycle.apply {
            removeObserver(lifecycleObserver)
            addObserver(lifecycleObserver)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open fun enableOnBackPressed(enabled: Boolean) {
        enableOnBackPressedCallback = enabled
        updateOnBackPressedCallbackEnabled()
    }

    private fun updateOnBackPressedCallbackEnabled() {
        onBackPressedCallback.isEnabled = (
            enableOnBackPressedCallback && destinationCountOnBackStack > 1
            )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open fun setViewModelStore(viewModelStore: ViewModelStore) {
        if (viewModel == NavControllerViewModel.getInstance(viewModelStore)) {
            return
        }
        check(backQueue.isEmpty()) { "ViewModelStore should be set before setGraph call" }
        viewModel = NavControllerViewModel.getInstance(viewModelStore)
    }

    /**
     * Gets the [ViewModelStoreOwner] for a NavGraph. This can be passed to
     * [androidx.lifecycle.ViewModelProvider] to retrieve a ViewModel that is scoped
     * to the navigation graph - it will be cleared when the navigation graph is popped off
     * the back stack.
     *
     * @param navGraphId ID of a NavGraph that exists on the back stack
     * @throws IllegalStateException if called before the [NavHost] has called
     * [NavHostController.setViewModelStore].
     * @throws IllegalArgumentException if the NavGraph is not on the back stack
     */
    public open fun getViewModelStoreOwner(@IdRes navGraphId: Int): ViewModelStoreOwner {
        checkNotNull(viewModel) {
            "You must call setViewModelStore() before calling getViewModelStoreOwner()."
        }
        val lastFromBackStack = getBackStackEntry(navGraphId)
        require(lastFromBackStack.destination is NavGraph) {
            "No NavGraph with ID $navGraphId is on the NavController's back stack"
        }
        return lastFromBackStack
    }

    /**
     * Gets the topmost [NavBackStackEntry] for a destination id.
     *
     * This is always safe to use with [the current destination][.getCurrentDestination] or
     * [its parent][NavDestination.getParent] or grandparent navigation graphs as these
     * destinations are guaranteed to be on the back stack.
     *
     * @param destinationId ID of a destination that exists on the back stack
     * @throws IllegalArgumentException if the destination is not on the back stack
     */
    public open fun getBackStackEntry(@IdRes destinationId: Int): NavBackStackEntry {
        val lastFromBackStack: NavBackStackEntry? = backQueue.lastOrNull { entry ->
            entry.destination.id == destinationId
        }
        requireNotNull(lastFromBackStack) {
            "No destination with ID $destinationId is on the NavController's back stack. The " +
                "current destination is $currentDestination"
        }
        return lastFromBackStack
    }

    /**
     * Gets the topmost [NavBackStackEntry].
     *
     * @return the topmost entry on the back stack or null if the back stack is empty
     */
    public open val currentBackStackEntry: NavBackStackEntry?
        get() = backQueue.lastOrNull()

    /**
     * Gets the previous visible [NavBackStackEntry].
     *
     * This skips over any [NavBackStackEntry] that is associated with a [NavGraph].
     *
     * @return the previous visible entry on the back stack or null if the back stack has less
     * than two visible entries
     */
    public open val previousBackStackEntry: NavBackStackEntry?
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
        private const val TAG = "NavController"
        private const val KEY_NAVIGATOR_STATE = "android-support-nav:controller:navigatorState"
        private const val KEY_NAVIGATOR_STATE_NAMES =
            "android-support-nav:controller:navigatorState:names"
        private const val KEY_BACK_STACK = "android-support-nav:controller:backStack"
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val KEY_DEEP_LINK_IDS: String = "android-support-nav:controller:deepLinkIds"
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val KEY_DEEP_LINK_ARGS: String = "android-support-nav:controller:deepLinkArgs"
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Suppress("IntentName")
        public const val KEY_DEEP_LINK_EXTRAS: String =
            "android-support-nav:controller:deepLinkExtras"
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val KEY_DEEP_LINK_HANDLED: String =
            "android-support-nav:controller:deepLinkHandled"

        /**
         * The [Intent] that triggered a deep link to the current destination.
         */
        public const val KEY_DEEP_LINK_INTENT: String =
            "android-support-nav:controller:deepLinkIntent"
    }
}
