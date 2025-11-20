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

import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.internal.NavContext
import androidx.navigation.internal.NavControllerImpl
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState
import androidx.savedstate.write
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.InternalSerializationApi

public actual open class NavController {

    private val impl: NavControllerImpl = NavControllerImpl(this) {}

    internal actual val navContext = NavContext()

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

    init {
        impl._navigatorProvider.addNavigator(NavGraphNavigator(impl._navigatorProvider))
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

    @MainThread
    public open fun popBackStack(destinationId: Int, inclusive: Boolean): Boolean {
        return impl.popBackStack(destinationId, inclusive)
    }

    @MainThread
    public open fun popBackStack(
        destinationId: Int,
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
    ): Boolean = impl.popBackStack(route, inclusive, saveState)

    @MainThread
    @JvmOverloads
    public actual fun <T : Any> popBackStack(
        route: T,
        inclusive: Boolean,
        saveState: Boolean,
    ): Boolean = impl.popBackStack(route, inclusive, saveState)

    @MainThread
    private fun popBackStackInternal(
        destinationId: Int,
        inclusive: Boolean,
        saveState: Boolean = false,
    ): Boolean = impl.popBackStackInternal(destinationId, inclusive, saveState)

    @MainThread
    public actual fun clearBackStack(route: String): Boolean = impl.clearBackStack(route)

    @MainThread
    public actual inline fun <reified T : Any> clearBackStack(): Boolean = clearBackStack(T::class)

    @MainThread
    public actual fun <T : Any> clearBackStack(route: KClass<T>): Boolean =
        impl.clearBackStack(route)

    @MainThread
    public actual fun <T : Any> clearBackStack(route: T): Boolean = impl.clearBackStack(route)

    @MainThread
    public actual open fun navigateUp(): Boolean {
        if (destinationCountOnBackStack == 1) {
            // opposite to the android implementation, we don't start a new window for deep links,
            // so we mustn't reopen an initial screen here
            return false
        } else {
            return popBackStack()
        }
    }

    private val destinationCountOnBackStack: Int
        get() = impl.backQueue.count { entry -> entry.destination !is NavGraph }

    @MainThread
    @CallSuper
    public actual open fun setGraph(graph: NavGraph, startDestinationArgs: SavedState?) {
        impl.setGraph(graph, startDestinationArgs)
    }

    internal actual fun checkDeepLinkHandled(): Boolean {
        return false
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
            val deepLinkNodes = destination.buildDeepLinkDestinations()
            val globalArgs =
                destination.addInDefaultArgs(matchingDeepLink.matchingArgs) ?: savedState()
            val args = arrayOfNulls<SavedState>(deepLinkNodes.size)
            for (index in args.indices) {
                args[index] = savedState { putAll(globalArgs) }
            }

            // Start with a cleared task starting at our root when we're on our own task
            if (!impl.backQueue.isEmpty()) {
                popBackStackInternal(impl._graph!!.id, true)
            }

            for (i in deepLinkNodes.indices) {
                val node = deepLinkNodes[i]
                val arguments = args[i]
                navigate(
                    node,
                    arguments,
                    navOptions {
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
        } else {
            println(
                "Navigation destination that matches route $request cannot be found in the " +
                    "navigation graph ${impl._graph}"
            )
            return false
        }
    }

    public actual open val currentDestination: NavDestination?
        get() = impl.currentDestination

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun findDestination(destinationId: Int): NavDestination? {
        return impl.findDestination(destinationId, null)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun NavDestination.findDestinationComprehensive(
        destinationId: Int,
        searchChildren: Boolean,
    ): NavDestination? {
        return impl.findDestinationComprehensive(this, destinationId, searchChildren, null)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun findDestination(route: String): NavDestination? {
        return impl.findDestination(route)
    }

    @MainThread
    public actual open fun navigate(deepLink: NavUri) {
        impl.navigate(NavDeepLinkRequest(deepLink, null, null))
    }

    @MainThread
    public actual open fun navigate(deepLink: NavUri, navOptions: NavOptions?) {
        impl.navigate(NavDeepLinkRequest(deepLink, null, null), navOptions)
    }

    @MainThread
    public actual open fun navigate(
        deepLink: NavUri,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?,
    ) {
        impl.navigate(NavDeepLinkRequest(deepLink, null, null), navOptions)
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

    internal actual fun writeIntent(request: NavDeepLinkRequest, args: SavedState) {}

    @MainThread
    private fun navigate(
        node: NavDestination,
        args: SavedState?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?,
    ) {
        impl.navigate(node, args, navOptions, navigatorExtras)
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
        impl.restoreState(navState)
        navState?.read { deepLinkHandled = getBooleanOrNull(KEY_DEEP_LINK_HANDLED) ?: false }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual open fun setLifecycleOwner(owner: LifecycleOwner) {
        impl.setLifecycleOwner(owner)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual open fun setViewModelStore(viewModelStore: ViewModelStore) {
        impl.setViewModelStore(viewModelStore)
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
        public const val KEY_DEEP_LINK_HANDLED: String =
            "android-support-nav:controller:deepLinkHandled"
        private var deepLinkSaveState = true

        @JvmStatic
        @NavDeepLinkSaveStateControl
        public actual fun enableDeepLinkSaveState(saveState: Boolean) {
            implementedInJetBrainsFork()
        }
    }
}
