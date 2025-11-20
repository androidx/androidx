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

@file:JvmName("NavControllerKt")
@file:JvmMultifileClass

package androidx.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.annotation.MainThread
import androidx.annotation.NavigationRes
import androidx.annotation.RestrictTo
import androidx.core.app.TaskStackBuilder
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.internal.Log
import androidx.navigation.internal.NavContext
import androidx.navigation.internal.NavControllerImpl
import androidx.navigation.serialization.generateHashCode
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState
import androidx.savedstate.write
import kotlin.collections.removeLast as removeLastKt
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

public actual open class NavController(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val context: Context
) {

    private val impl: NavControllerImpl =
        NavControllerImpl(this) { updateOnBackPressedCallbackEnabled() }

    internal actual val navContext = NavContext(context)

    private var activity: Activity? =
        generateSequence(context) {
                if (it is ContextWrapper) {
                    it.baseContext
                } else null
            }
            .firstOrNull { it is Activity } as Activity?

    private var inflater: NavInflater? = null

    public actual open var graph: NavGraph
        @MainThread
        get() {
            return impl.graph
        }
        @MainThread
        @CallSuper
        set(graph) {
            impl.graph = graph
        }

    internal var deepLinkHandled = false

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual val currentBackStack: StateFlow<List<NavBackStackEntry>>
        get() = impl.currentBackStack

    public actual val visibleEntries: StateFlow<List<NavBackStackEntry>>
        get() = impl.visibleEntries

    private var onBackPressedDispatcher: OnBackPressedDispatcher? = null

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                popBackStack()
            }
        }
    private var enableOnBackPressedCallback = true

    public actual fun interface OnDestinationChangedListener {
        public actual fun onDestinationChanged(
            controller: NavController,
            destination: NavDestination,
            arguments: SavedState?,
        )
    }

    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual open var navigatorProvider: NavigatorProvider
        get() = impl.navigatorProvider
        set(value) {
            impl.navigatorProvider = value
        }

    internal actual open inner class NavControllerNavigatorState
    actual constructor(actual val navigator: Navigator<out NavDestination>) : NavigatorState() {
        actual override fun push(backStackEntry: NavBackStackEntry) {
            impl.push(this, backStackEntry)
        }

        actual fun addInternal(backStackEntry: NavBackStackEntry) {
            super.push(backStackEntry)
        }

        actual override fun createBackStackEntry(
            destination: NavDestination,
            arguments: SavedState?,
        ) = impl.createBackStackEntry(destination, arguments)

        actual override fun pop(popUpTo: NavBackStackEntry, saveState: Boolean) {
            impl.pop(this, popUpTo, saveState) { super.pop(popUpTo, saveState) }
        }

        actual override fun popWithTransition(popUpTo: NavBackStackEntry, saveState: Boolean) {
            super.popWithTransition(popUpTo, saveState)
        }

        actual override fun markTransitionComplete(entry: NavBackStackEntry) {
            impl.markTransitionComplete(this, entry) { super.markTransitionComplete(entry) }
        }

        actual override fun prepareForTransition(entry: NavBackStackEntry) {
            super.prepareForTransition(entry)
            impl.prepareForTransition(entry)
        }
    }

    internal actual fun createNavControllerNavigatorState(
        navigator: Navigator<out NavDestination>
    ): NavControllerNavigatorState {
        return NavControllerNavigatorState(navigator)
    }

    /**
     * Constructs a new controller for a given [Context]. Controllers should not be used outside of
     * their context and retain a hard reference to the context supplied. If you need a global
     * controller, pass [Context.getApplicationContext].
     *
     * Apps should generally not construct controllers, instead obtain a relevant controller
     * directly from a navigation host via [NavHost.getNavController] or by using one of the utility
     * methods on the [Navigation] class.
     *
     * Note that controllers that are not constructed with an [Activity] context (or a wrapped
     * activity context) will only be able to navigate to
     * [new tasks][android.content.Intent.FLAG_ACTIVITY_NEW_TASK] or
     * [new document tasks][android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT] when navigating to
     * new activities.
     *
     * @param context context for this controller
     */
    init {
        impl._navigatorProvider.addNavigator(NavGraphNavigator(impl._navigatorProvider))
        impl._navigatorProvider.addNavigator(ActivityNavigator(context))
    }

    public actual open fun addOnDestinationChangedListener(listener: OnDestinationChangedListener) {
        impl.addOnDestinationChangedListener(listener)
    }

    public actual open fun removeOnDestinationChangedListener(
        listener: OnDestinationChangedListener
    ) {
        impl.removeOnDestinationChangedListener(listener)
    }

    @MainThread public actual open fun popBackStack(): Boolean = impl.popBackStack()

    /**
     * Attempts to pop the controller's back stack back to a specific destination.
     *
     * @param destinationId The topmost destination to retain
     * @param inclusive Whether the given destination should also be popped.
     * @return true if the stack was popped at least once and the user has been navigated to another
     *   destination, false otherwise
     */
    @MainThread
    public open fun popBackStack(@IdRes destinationId: Int, inclusive: Boolean): Boolean {
        return impl.popBackStack(destinationId, inclusive)
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
    @MainThread
    public open fun popBackStack(
        @IdRes destinationId: Int,
        inclusive: Boolean,
        saveState: Boolean,
    ): Boolean {
        return impl.popBackStack(destinationId, inclusive, saveState)
    }

    @MainThread
    @JvmOverloads
    public actual fun popBackStack(route: String, inclusive: Boolean, saveState: Boolean): Boolean {
        return impl.popBackStack(route, inclusive, saveState)
    }

    @MainThread
    @JvmOverloads
    public actual inline fun <reified T : Any> popBackStack(
        inclusive: Boolean,
        saveState: Boolean,
    ): Boolean = popBackStack(T::class, inclusive, saveState)

    @MainThread
    @JvmOverloads
    @OptIn(InternalSerializationApi::class)
    public actual fun <T : Any> popBackStack(
        route: KClass<T>,
        inclusive: Boolean,
        saveState: Boolean,
    ): Boolean {
        return impl.popBackStack(route, inclusive, saveState)
    }

    @MainThread
    @JvmOverloads
    public actual fun <T : Any> popBackStack(
        route: T,
        inclusive: Boolean,
        saveState: Boolean,
    ): Boolean {
        return impl.popBackStack(route, inclusive, saveState)
    }

    @MainThread
    private fun popBackStackInternal(
        @IdRes destinationId: Int,
        inclusive: Boolean,
        saveState: Boolean = false,
    ): Boolean {
        return impl.popBackStackInternal(destinationId, inclusive, saveState)
    }

    @MainThread
    public actual fun clearBackStack(route: String): Boolean {
        return impl.clearBackStack(route)
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
    public fun clearBackStack(@IdRes destinationId: Int): Boolean {
        return impl.clearBackStack(destinationId)
    }

    @MainThread
    public actual inline fun <reified T : Any> clearBackStack(): Boolean = clearBackStack(T::class)

    @OptIn(InternalSerializationApi::class)
    @MainThread
    public actual fun <T : Any> clearBackStack(route: KClass<T>): Boolean =
        impl.clearBackStack(route)

    @OptIn(InternalSerializationApi::class)
    @MainThread
    public actual fun <T : Any> clearBackStack(route: T): Boolean {
        return impl.clearBackStack(route)
    }

    @MainThread
    public actual open fun navigateUp(): Boolean {
        // If there's only one entry, then we may have deep linked into a specific destination
        // on another task.
        if (destinationCountOnBackStack == 1) {
            val extras = activity?.intent?.extras
            if (extras?.getIntArray(KEY_DEEP_LINK_IDS) != null) {
                return tryRelaunchUpToExplicitStack()
            } else {
                return tryRelaunchUpToGeneratedStack()
            }
        } else {
            return popBackStack()
        }
    }

    /**
     * Starts a new Activity directed to the next-upper Destination in the explicit deep link stack
     * used to start this Activity. Returns false if the current destination was already the root of
     * the deep link.
     */
    @Suppress("DEPRECATION")
    private fun tryRelaunchUpToExplicitStack(): Boolean {
        if (!deepLinkHandled) {
            return false
        }

        val intent = activity!!.intent
        val extras = intent.extras

        val deepLinkIds = extras!!.getIntArray(KEY_DEEP_LINK_IDS)!!.toMutableList()
        val deepLinkArgs = extras.getParcelableArrayList<SavedState>(KEY_DEEP_LINK_ARGS)

        // Probably deep linked to a single destination only.
        if (deepLinkIds.size < 2) {
            return false
        }

        // Remove the leaf destination to pop up to one level above it
        var leafDestinationId = deepLinkIds.removeLastKt()
        deepLinkArgs?.removeLastKt()

        // Find the destination if the leaf destination was a NavGraph
        with(graph.findDestinationComprehensive(leafDestinationId, false)) {
            if (this is NavGraph) {
                leafDestinationId = this.findStartDestination().id
            }
        }

        // The final element of the deep link couldn't have brought us to the current location.
        if (leafDestinationId != currentDestination?.id) {
            return false
        }

        val navDeepLinkBuilder = createDeepLink()

        // Attach the original global arguments, and also the original calling Intent.
        val arguments = savedState {
            putParcelable(KEY_DEEP_LINK_INTENT, intent)
            extras.getBundle(KEY_DEEP_LINK_EXTRAS)?.let { putAll(it) }
        }
        navDeepLinkBuilder.setArguments(arguments)

        deepLinkIds.forEachIndexed { index, deepLinkId ->
            navDeepLinkBuilder.addDestination(deepLinkId, deepLinkArgs?.get(index))
        }

        navDeepLinkBuilder.createTaskStackBuilder().startActivities()
        activity?.finish()
        return true
    }

    /**
     * Starts a new Activity directed to the parent of the current Destination. Returns false if the
     * current destination was already the root of the deep link.
     */
    private fun tryRelaunchUpToGeneratedStack(): Boolean {
        val currentDestination = currentDestination
        var destId = currentDestination!!.id
        var parent = currentDestination.parent
        while (parent != null) {
            if (parent.startDestinationId != destId) {
                val args = savedState {
                    if (activity != null && activity!!.intent != null) {
                        val data = activity!!.intent.data

                        // We were started via a URI intent.
                        if (data != null) {
                            // Include the original deep link Intent so the Destinations can
                            // synthetically generate additional arguments as necessary.
                            putParcelable(KEY_DEEP_LINK_INTENT, activity!!.intent)
                            val currGraph = impl.getTopGraph()
                            val matchingDeepLink =
                                currGraph.matchDeepLinkComprehensive(
                                    navDeepLinkRequest = NavDeepLinkRequest(activity!!.intent),
                                    searchChildren = true,
                                    searchParent = true,
                                    lastVisited = currGraph,
                                )
                            if (matchingDeepLink?.matchingArgs != null) {
                                val destinationArgs =
                                    matchingDeepLink.destination.addInDefaultArgs(
                                        matchingDeepLink.matchingArgs
                                    )
                                destinationArgs?.let { putAll(it) }
                            }
                        }
                    }
                }
                val parentIntents =
                    NavDeepLinkBuilder(this)
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
        return false
    }

    /** Gets the number of non-NavGraph destinations on the back stack */
    private val destinationCountOnBackStack: Int
        get() = impl.backQueue.count { entry -> entry.destination !is NavGraph }

    /**
     * The [inflater][NavInflater] for this controller.
     *
     * @return inflater for loading navigation resources
     */
    public open val navInflater: NavInflater by lazy {
        inflater ?: NavInflater(context, impl._navigatorProvider)
    }

    /**
     * Sets the [navigation graph][NavGraph] to the specified resource. Any current navigation graph
     * data (including back stack) will be replaced.
     *
     * The inflated graph can be retrieved via [graph].
     *
     * @param graphResId resource id of the navigation graph to inflate
     * @see NavController.navInflater
     * @see NavController.setGraph
     * @see NavController.graph
     */
    @MainThread
    @CallSuper
    public open fun setGraph(@NavigationRes graphResId: Int) {
        impl.setGraph(navInflater.inflate(graphResId), null)
    }

    /**
     * Sets the [navigation graph][NavGraph] to the specified resource. Any current navigation graph
     * data (including back stack) will be replaced.
     *
     * The inflated graph can be retrieved via [graph].
     *
     * @param graphResId resource id of the navigation graph to inflate
     * @param startDestinationArgs arguments to send to the start destination of the graph
     * @see NavController.navInflater
     * @see NavController.setGraph
     * @see NavController.graph
     */
    @MainThread
    @CallSuper
    public open fun setGraph(@NavigationRes graphResId: Int, startDestinationArgs: SavedState?) {
        impl.setGraph(navInflater.inflate(graphResId), startDestinationArgs)
    }

    @MainThread
    @CallSuper
    public actual open fun setGraph(graph: NavGraph, startDestinationArgs: SavedState?) {
        impl.setGraph(graph, startDestinationArgs)
    }

    internal actual fun checkDeepLinkHandled(): Boolean {
        return !deepLinkHandled && activity != null && handleDeepLink(activity!!.intent)
    }

    /**
     * Checks the given Intent for a Navigation deep link and navigates to the deep link if present.
     * This is called automatically for you the first time you set the graph if you've passed in an
     * [Activity] as the context when constructing this NavController, but should be manually called
     * if your Activity receives new Intents in [Activity.onNewIntent].
     *
     * The types of Intents that are supported include:
     *
     * Intents created by [NavDeepLinkBuilder] or [createDeepLink]. This assumes that the current
     * graph shares the same hierarchy to get to the deep linked destination as when the deep link
     * was constructed. Intents that include a [data Uri][Intent.getData]. This Uri will be checked
     * against the Uri patterns in the [NavDeepLinks][NavDeepLink] added via
     * [NavDestination.addDeepLink].
     *
     * The [navigation graph][graph] should be set before calling this method.
     *
     * @param intent The Intent that may contain a valid deep link
     * @return True if the navigation controller found a valid deep link and navigated to it.
     * @throws IllegalStateException if deep link cannot be accessed from the current destination
     * @see NavDestination.addDeepLink
     */
    @MainThread
    @Suppress("DEPRECATION")
    public open fun handleDeepLink(intent: Intent?): Boolean {
        if (intent == null) {
            return false
        }
        val extras = intent.extras
        var deepLink =
            try {
                extras?.getIntArray(KEY_DEEP_LINK_IDS)
            } catch (e: Exception) {
                android.util.Log.e(
                    NavControllerImpl.TAG,
                    "handleDeepLink() could not extract deepLink from $intent",
                    e,
                )
                null
            }
        var deepLinkArgs = extras?.getParcelableArrayList<SavedState>(KEY_DEEP_LINK_ARGS)
        val globalArgs = savedState()
        val deepLinkExtras = extras?.getBundle(KEY_DEEP_LINK_EXTRAS)
        if (deepLinkExtras != null) {
            globalArgs.write { putAll(deepLinkExtras) }
        }
        if (deepLink == null || deepLink.isEmpty()) {
            val currGraph = impl.getTopGraph()
            val matchingDeepLink =
                currGraph.matchDeepLinkComprehensive(
                    navDeepLinkRequest = NavDeepLinkRequest(intent),
                    searchChildren = true,
                    searchParent = true,
                    lastVisited = currGraph,
                )
            if (matchingDeepLink != null) {
                val destination = matchingDeepLink.destination
                deepLink = destination.buildDeepLinkIds()
                deepLinkArgs = null
                val destinationArgs = destination.addInDefaultArgs(matchingDeepLink.matchingArgs)
                if (destinationArgs != null) {
                    globalArgs.write { putAll(destinationArgs) }
                }
            }
        }
        if (deepLink == null || deepLink.isEmpty()) {
            return false
        }
        val invalidDestinationDisplayName = findInvalidDestinationDisplayNameInDeepLink(deepLink)
        if (invalidDestinationDisplayName != null) {
            Log.i(
                NavControllerImpl.TAG,
                "Could not find destination $invalidDestinationDisplayName in the " +
                    "navigation graph, ignoring the deep link from $intent",
            )
            return false
        }
        globalArgs.write { putParcelable(KEY_DEEP_LINK_INTENT, intent) }
        val args = arrayOfNulls<SavedState>(deepLink.size)
        for (index in args.indices) {
            val arguments = savedState {
                putAll(globalArgs)
                if (deepLinkArgs != null) {
                    val deepLinkArguments = deepLinkArgs[index]
                    if (deepLinkArguments != null) {
                        putAll(deepLinkArguments)
                    }
                }
            }
            args[index] = arguments
        }
        val flags = intent.flags
        if (
            flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0 &&
                flags and Intent.FLAG_ACTIVITY_CLEAR_TASK == 0
        ) {
            // Someone called us with NEW_TASK, but we don't know what state our whole
            // task stack is in, so we need to manually restart the whole stack to
            // ensure we're in a predictably good state.
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            val taskStackBuilder =
                TaskStackBuilder.create(context).addNextIntentWithParentStack(intent)
            taskStackBuilder.startActivities()
            activity?.let { activity ->
                activity.finish()
                // Disable second animation in case where the Activity is created twice.
                activity.overridePendingTransition(0, 0)
            }
            return true
        }
        return handleDeepLink(deepLink, args, flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @MainThread
    public actual fun handleDeepLink(request: NavDeepLinkRequest): Boolean {
        val currGraph = impl.getTopGraph()
        val matchingDeepLink =
            currGraph.matchDeepLinkComprehensive(
                navDeepLinkRequest = request,
                searchChildren = true,
                searchParent = true,
                lastVisited = currGraph,
            )
        if (matchingDeepLink != null) {
            val destination = matchingDeepLink.destination
            val deepLink = destination.buildDeepLinkIds()
            val globalArgs = savedState {
                val destinationArgs = destination.addInDefaultArgs(matchingDeepLink.matchingArgs)
                if (destinationArgs != null) {
                    putAll(destinationArgs)
                }
            }
            val args = arrayOfNulls<SavedState>(deepLink.size)
            for (index in args.indices) {
                val arguments = savedState { putAll(globalArgs) }
                args[index] = arguments
            }
            return handleDeepLink(deepLink, args, true)
        }
        return false
    }

    @MainThread
    private fun handleDeepLink(
        deepLink: IntArray,
        args: Array<SavedState?>,
        newTask: Boolean,
    ): Boolean {
        if (newTask) {
            // Start with a cleared task starting at our root when we're on our own task
            if (!impl.backQueue.isEmpty()) {
                popBackStackInternal(impl._graph!!.id, true)
            }
            var index = 0
            while (index < deepLink.size) {
                val destinationId = deepLink[index]
                val arguments = args[index++]
                val node = findDestination(destinationId)
                if (node == null) {
                    val dest = NavDestination.getDisplayName(navContext, destinationId)
                    throw IllegalStateException(
                        "Deep Linking failed: destination $dest cannot be found from the current " +
                            "destination $currentDestination"
                    )
                }
                navigate(
                    node,
                    arguments,
                    navOptions {
                        anim {
                            enter = 0
                            exit = 0
                        }
                        val changingGraphs =
                            node is NavGraph &&
                                node.hierarchy.none { it == currentDestination?.parent }
                        if (changingGraphs && deepLinkSaveState) {
                            // If we are navigating to a 'sibling' graph (one that isn't part
                            // of the current destination's hierarchy), then we need to saveState
                            // to ensure that each graph has its own saved state that users can
                            // return to
                            popUpTo(graph.findStartDestination().id) { saveState = true }
                            // Note we specifically don't call restoreState = true
                            // as our deep link should support multiple instances of the
                            // same graph in a row
                        }
                    },
                    null,
                )
            }
            deepLinkHandled = true
            return true
        }
        // Assume we're on another apps' task and only start the final destination
        var graph = impl._graph
        for (i in deepLink.indices) {
            val destinationId = deepLink[i]
            val arguments = args[i]
            val node = if (i == 0) impl._graph else graph!!.findNode(destinationId)
            if (node == null) {
                val dest = NavDestination.getDisplayName(navContext, destinationId)
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
                    while (graph!!.findNode(graph.startDestinationId) is NavGraph) {
                        graph = graph.findNode(graph.startDestinationId) as NavGraph?
                    }
                }
            } else {
                // Navigate to the last NavDestination, clearing any existing destinations
                navigate(
                    node,
                    arguments,
                    NavOptions.Builder()
                        .setPopUpTo(impl._graph!!.id, true)
                        .setEnterAnim(0)
                        .setExitAnim(0)
                        .build(),
                    null,
                )
            }
        }
        deepLinkHandled = true
        return true
    }

    /**
     * Looks through the deep link for invalid destinations, returning the display name of any
     * invalid destinations in the deep link array.
     *
     * @param deepLink array of deep link IDs that are expected to match the graph
     * @return The display name of the first destination not found in the graph or null if all
     *   destinations were found in the graph.
     */
    private fun findInvalidDestinationDisplayNameInDeepLink(deepLink: IntArray): String? {
        return impl.findInvalidDestinationDisplayNameInDeepLink(deepLink)
    }

    public actual open val currentDestination: NavDestination?
        get() = impl.currentDestination

    /**
     * Recursively searches through parents
     *
     * @param destinationId the [NavDestination.id]
     * @param matchingDest an optional NavDestination that the node should match with. This is
     *   because [destinationId] is only unique to a local graph. Nodes in sibling graphs can have
     *   the same id.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun findDestination(
        @IdRes destinationId: Int,
        matchingDest: NavDestination? = null,
    ): NavDestination? {
        return impl.findDestination(destinationId, matchingDest)
    }

    /**
     * Recursively searches through parents. If [searchChildren] is true, also recursively searches
     * children.
     *
     * @param destinationId the [NavDestination.id]
     * @param searchChildren recursively searches children when true
     * @param matchingDest an optional NavDestination that the node should match with. This is
     *   because [destinationId] is only unique to a local graph. Nodes in sibling graphs can have
     *   the same id.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun NavDestination.findDestinationComprehensive(
        @IdRes destinationId: Int,
        searchChildren: Boolean,
        matchingDest: NavDestination? = null,
    ): NavDestination? {
        return impl.findDestinationComprehensive(this, destinationId, searchChildren, matchingDest)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun findDestination(route: String): NavDestination? {
        return impl.findDestination(route)
    }

    /**
     * Navigate to a destination from the current navigation graph. This supports both navigating
     * via an [action][NavDestination.getAction] and directly navigating to a destination.
     *
     * @param resId an [action][NavDestination.getAction] id or a destination id to navigate to
     * @throws IllegalStateException if there is no current navigation node
     * @throws IllegalArgumentException if the desired destination cannot be found from the current
     *   destination
     */
    @MainThread
    public open fun navigate(@IdRes resId: Int) {
        navigate(resId, null)
    }

    /**
     * Navigate to a destination from the current navigation graph. This supports both navigating
     * via an [action][NavDestination.getAction] and directly navigating to a destination.
     *
     * @param resId an [action][NavDestination.getAction] id or a destination id to navigate to
     * @param args arguments to pass to the destination
     * @throws IllegalStateException if there is no current navigation node
     * @throws IllegalArgumentException if the desired destination cannot be found from the current
     *   destination
     */
    @MainThread
    public open fun navigate(@IdRes resId: Int, args: SavedState?) {
        navigate(resId, args, null)
    }

    /**
     * Navigate to a destination from the current navigation graph. This supports both navigating
     * via an [action][NavDestination.getAction] and directly navigating to a destination.
     *
     * If given [NavOptions] pass in [NavOptions.restoreState] `true`, any args passed here will be
     * overridden by the restored args.
     *
     * @param resId an [action][NavDestination.getAction] id or a destination id to navigate to
     * @param args arguments to pass to the destination
     * @param navOptions special options for this navigation operation
     * @throws IllegalStateException if there is no current navigation node
     * @throws IllegalArgumentException if the desired destination cannot be found from the current
     *   destination
     */
    @MainThread
    public open fun navigate(@IdRes resId: Int, args: SavedState?, navOptions: NavOptions?) {
        navigate(resId, args, navOptions, null)
    }

    /**
     * Navigate to a destination from the current navigation graph. This supports both navigating
     * via an [action][NavDestination.getAction] and directly navigating to a destination.
     *
     * If given [NavOptions] pass in [NavOptions.restoreState] `true`, any args passed here will be
     * overridden by the restored args.
     *
     * @param resId an [action][NavDestination.getAction] id or a destination id to navigate to
     * @param args arguments to pass to the destination
     * @param navOptions special options for this navigation operation
     * @param navigatorExtras extras to pass to the Navigator
     * @throws IllegalStateException if navigation graph has not been set for this NavController
     * @throws IllegalArgumentException if the desired destination cannot be found from the current
     *   destination
     */
    @OptIn(InternalSerializationApi::class)
    @MainThread
    public open fun navigate(
        @IdRes resId: Int,
        args: SavedState?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?,
    ) {
        var finalNavOptions = navOptions
        val currentNode =
            (if (impl.backQueue.isEmpty()) impl._graph else impl.backQueue.last().destination)
                ?: throw IllegalStateException(
                    "No current destination found. Ensure a navigation graph has been set for " +
                        "NavController $this."
                )

        @IdRes var destId = resId
        val navAction = currentNode.getAction(resId)
        var combinedArgs: SavedState? = null
        if (navAction != null) {
            if (finalNavOptions == null) {
                finalNavOptions = navAction.navOptions
            }
            destId = navAction.destinationId
            val navActionArgs = navAction.defaultArguments
            if (navActionArgs != null) {
                combinedArgs = savedState { putAll(navActionArgs) }
            }
        }
        if (args != null) {
            if (combinedArgs == null) {
                combinedArgs = savedState()
            }
            combinedArgs.write { putAll(args) }
        }
        // just pop and return if destId is invalid
        if (
            destId == 0 &&
                finalNavOptions != null &&
                (finalNavOptions.popUpToId != -1 ||
                    finalNavOptions.popUpToRoute != null ||
                    finalNavOptions.popUpToRouteClass != null)
        ) {
            when {
                finalNavOptions.popUpToRoute != null ->
                    popBackStack(
                        finalNavOptions.popUpToRoute!!,
                        finalNavOptions.isPopUpToInclusive(),
                    )
                finalNavOptions.popUpToRouteClass != null ->
                    popBackStack(
                        finalNavOptions.popUpToRouteClass!!.serializer().generateHashCode(),
                        finalNavOptions.isPopUpToInclusive(),
                    )
                finalNavOptions.popUpToId != -1 ->
                    popBackStack(finalNavOptions.popUpToId, finalNavOptions.isPopUpToInclusive())
            }
            return
        }
        require(destId != 0) {
            "Destination id == 0 can only be used in conjunction with a valid navOptions.popUpTo"
        }
        val node = findDestination(destId)
        if (node == null) {
            val dest = NavDestination.getDisplayName(navContext, destId)
            require(navAction == null) {
                "Navigation destination $dest referenced from action " +
                    "${NavDestination.getDisplayName(navContext, resId)} cannot be found from " +
                    "the current destination $currentNode"
            }
            throw IllegalArgumentException(
                "Navigation action/destination $dest cannot be found from the current " +
                    "destination $currentNode"
            )
        }
        navigate(node, combinedArgs, finalNavOptions, navigatorExtras)
    }

    @MainThread
    public actual open fun navigate(deepLink: Uri) {
        impl.navigate(NavDeepLinkRequest(deepLink, null, null))
    }

    @MainThread
    public actual open fun navigate(deepLink: Uri, navOptions: NavOptions?) {
        impl.navigate(NavDeepLinkRequest(deepLink, null, null), navOptions)
    }

    @MainThread
    public actual open fun navigate(
        deepLink: Uri,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?,
    ) {
        impl.navigate(NavDeepLinkRequest(deepLink, null, null), navOptions, navigatorExtras)
    }

    @MainThread
    public actual open fun navigate(request: NavDeepLinkRequest) {
        impl.navigate(request)
    }

    @MainThread
    public actual open fun navigate(request: NavDeepLinkRequest, navOptions: NavOptions?) {
        impl.navigate(request, navOptions)
    }

    @MainThread
    public actual open fun navigate(
        request: NavDeepLinkRequest,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?,
    ) {
        impl.navigate(request, navOptions, navigatorExtras)
    }

    internal actual fun writeIntent(request: NavDeepLinkRequest, args: SavedState) {
        val intent =
            Intent().apply {
                setDataAndType(request.uri, request.mimeType)
                action = request.action
            }
        args.write { putParcelable(KEY_DEEP_LINK_INTENT, intent) }
    }

    @MainThread
    private fun navigate(
        node: NavDestination,
        args: SavedState?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?,
    ) {
        impl.navigate(node, args, navOptions, navigatorExtras)
    }

    /**
     * Navigate via the given [NavDirections]
     *
     * @param directions directions that describe this navigation operation
     */
    @MainThread
    public open fun navigate(directions: NavDirections) {
        navigate(directions.actionId, directions.arguments, null)
    }

    /**
     * Navigate via the given [NavDirections]
     *
     * @param directions directions that describe this navigation operation
     * @param navOptions special options for this navigation operation
     */
    @MainThread
    public open fun navigate(directions: NavDirections, navOptions: NavOptions?) {
        navigate(directions.actionId, directions.arguments, navOptions)
    }

    /**
     * Navigate via the given [NavDirections]
     *
     * @param directions directions that describe this navigation operation
     * @param navigatorExtras extras to pass to the [Navigator]
     */
    @MainThread
    public open fun navigate(directions: NavDirections, navigatorExtras: Navigator.Extras) {
        navigate(directions.actionId, directions.arguments, null, navigatorExtras)
    }

    @MainThread
    public actual fun navigate(route: String, builder: NavOptionsBuilder.() -> Unit) {
        impl.navigate(route, builder)
    }

    @MainThread
    @JvmOverloads
    public actual fun navigate(
        route: String,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?,
    ) {
        impl.navigate(route, navOptions, navigatorExtras)
    }

    @MainThread
    public actual fun <T : Any> navigate(route: T, builder: NavOptionsBuilder.() -> Unit) {
        impl.navigate(route, builder)
    }

    @MainThread
    @JvmOverloads
    public actual fun <T : Any> navigate(
        route: T,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?,
    ) {
        impl.navigate(route, navOptions, navigatorExtras)
    }

    /**
     * Create a deep link to a destination within this NavController.
     *
     * @return a [NavDeepLinkBuilder] suitable for constructing a deep link
     */
    public open fun createDeepLink(): NavDeepLinkBuilder {
        return NavDeepLinkBuilder(this)
    }

    @CallSuper
    public actual open fun saveState(): SavedState? {
        var b = impl.saveState()
        if (deepLinkHandled) {
            if (b == null) {
                b = savedState()
            }
            b.write { putBoolean(KEY_DEEP_LINK_HANDLED, deepLinkHandled) }
        }
        return b
    }

    @CallSuper
    public actual open fun restoreState(navState: SavedState?) {
        navState?.classLoader = context.classLoader
        impl.restoreState(navState)
        navState?.read { deepLinkHandled = getBooleanOrNull(KEY_DEEP_LINK_HANDLED) ?: false }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual open fun setLifecycleOwner(owner: LifecycleOwner) {
        impl.setLifecycleOwner(owner)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open fun setOnBackPressedDispatcher(dispatcher: OnBackPressedDispatcher) {
        if (dispatcher == onBackPressedDispatcher) {
            return
        }
        val lifecycleOwner =
            checkNotNull(impl.lifecycleOwner) {
                "You must call setLifecycleOwner() before calling setOnBackPressedDispatcher()"
            }
        // Remove the callback from any previous dispatcher
        onBackPressedCallback.remove()
        // Then add it to the new dispatcher
        onBackPressedDispatcher = dispatcher
        dispatcher.addCallback(lifecycleOwner, onBackPressedCallback)

        // Make sure that listener for updating the NavBackStackEntry lifecycles comes after
        // the dispatcher
        lifecycleOwner.lifecycle.apply {
            removeObserver(impl.lifecycleObserver)
            addObserver(impl.lifecycleObserver)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open fun enableOnBackPressed(enabled: Boolean) {
        enableOnBackPressedCallback = enabled
        updateOnBackPressedCallbackEnabled()
    }

    private fun updateOnBackPressedCallbackEnabled() {
        onBackPressedCallback.isEnabled =
            (enableOnBackPressedCallback && destinationCountOnBackStack > 1)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual open fun setViewModelStore(viewModelStore: ViewModelStore) {
        impl.setViewModelStore(viewModelStore)
    }

    /**
     * Gets the [ViewModelStoreOwner] for a NavGraph. This can be passed to
     * [androidx.lifecycle.ViewModelProvider] to retrieve a ViewModel that is scoped to the
     * navigation graph - it will be cleared when the navigation graph is popped off the back stack.
     *
     * @param navGraphId ID of a NavGraph that exists on the back stack
     * @throws IllegalStateException if called before the [NavHost] has called
     *   [NavHostController.setViewModelStore].
     * @throws IllegalArgumentException if the NavGraph is not on the back stack
     */
    public open fun getViewModelStoreOwner(@IdRes navGraphId: Int): ViewModelStoreOwner {
        return impl.getViewModelStoreOwner(navGraphId)
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
    public open fun getBackStackEntry(@IdRes destinationId: Int): NavBackStackEntry {
        return impl.getBackStackEntry(destinationId)
    }

    public actual fun getBackStackEntry(route: String): NavBackStackEntry {
        return impl.getBackStackEntry(route)
    }

    public actual inline fun <reified T : Any> getBackStackEntry(): NavBackStackEntry =
        getBackStackEntry(T::class)

    @OptIn(InternalSerializationApi::class)
    public actual fun <T : Any> getBackStackEntry(route: KClass<T>): NavBackStackEntry {
        return impl.getBackStackEntry(route)
    }

    public actual fun <T : Any> getBackStackEntry(route: T): NavBackStackEntry {
        return impl.getBackStackEntry(route)
    }

    public actual open val currentBackStackEntry: NavBackStackEntry?
        get() = impl.currentBackStackEntry

    public actual val currentBackStackEntryFlow: Flow<NavBackStackEntry>
        get() = impl._currentBackStackEntryFlow.asSharedFlow()

    public actual open val previousBackStackEntry: NavBackStackEntry?
        get() = impl.previousBackStackEntry

    public actual companion object {
        @field:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val KEY_DEEP_LINK_IDS: String = "android-support-nav:controller:deepLinkIds"
        @field:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val KEY_DEEP_LINK_ARGS: String = "android-support-nav:controller:deepLinkArgs"
        @field:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Suppress("IntentName")
        public const val KEY_DEEP_LINK_EXTRAS: String =
            "android-support-nav:controller:deepLinkExtras"
        @field:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val KEY_DEEP_LINK_HANDLED: String =
            "android-support-nav:controller:deepLinkHandled"

        /** The [Intent] that triggered a deep link to the current destination. */
        public const val KEY_DEEP_LINK_INTENT: String =
            "android-support-nav:controller:deepLinkIntent"

        private var deepLinkSaveState = true

        @JvmStatic
        @NavDeepLinkSaveStateControl
        public actual fun enableDeepLinkSaveState(saveState: Boolean) {
            deepLinkSaveState = saveState
        }
    }
}

/**
 * Construct a new [NavGraph]
 *
 * @param id the graph's unique id
 * @param startDestination the route for the start destination
 * @param builder the builder used to construct the graph
 */
@Suppress("Deprecation")
@Deprecated(
    "Use routes to create your NavGraph instead",
    ReplaceWith(
        "createGraph(startDestination = startDestination.toString(), route = id.toString()) " +
            "{ builder.invoke() }"
    ),
)
public inline fun NavController.createGraph(
    @IdRes id: Int = 0,
    @IdRes startDestination: Int,
    builder: NavGraphBuilder.() -> Unit,
): NavGraph = navigatorProvider.navigation(id, startDestination, builder)

internal fun NavDeepLinkRequest(intent: Intent): NavDeepLinkRequest =
    NavDeepLinkRequest(intent.data, intent.action, intent.type)
