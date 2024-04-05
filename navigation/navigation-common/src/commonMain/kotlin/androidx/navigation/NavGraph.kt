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

import androidx.annotation.RestrictTo
import kotlin.jvm.JvmStatic

/**
 * NavGraph is a collection of [NavDestination] nodes fetchable by ID.
 *
 * A NavGraph serves as a 'virtual' destination: while the NavGraph itself will not appear
 * on the back stack, navigating to the NavGraph will cause the
 * starting destination to be added to the back stack.
 *
 * Construct a new NavGraph. This NavGraph is not valid until you
 * [add a destination][addDestination] and [set the starting destination][setStartDestination].
 *
 * @param navGraphNavigator The [NavGraphNavigator] which this destination will be associated
 *                          with. Generally retrieved via a
 *                          [NavController]'s[NavigatorProvider.getNavigator] method.
 */
public expect open class NavGraph(
    navGraphNavigator: Navigator<out NavGraph>
) : NavDestination, Iterable<NavDestination> {

    /**
     * Adds a destination to this NavGraph. The destination must have a route set.
     *
     * The destination must not have a [parent][NavDestination.parent] set. If
     * the destination is already part of a [navigation graph][NavGraph], call
     * [remove] before calling this method.
     *
     * @param node destination to add
     * @throws IllegalArgumentException if destination does not have an id, the destination has
     * the same id as the graph, or the destination already has a parent.
     */
    public fun addDestination(node: NavDestination)

    /**
     * Adds multiple destinations to this NavGraph. Each destination must have a route set.
     *
     * Each destination must not have a [parent][NavDestination.parent] set. If any
     * destination is already part of a [navigation graph][NavGraph], call [remove] before
     * calling this method.
     *
     * @param nodes destinations to add
     */
    public fun addDestinations(nodes: Collection<NavDestination?>)

    /**
     * Adds multiple destinations to this NavGraph. Each destination must have a route set.
     *
     * Each destination must not have a [parent][NavDestination.parent] set. If any
     * destination is already part of a [navigation graph][NavGraph], call [remove] before
     * calling this method.
     *
     * @param nodes destinations to add
     */
    public fun addDestinations(vararg nodes: NavDestination)

    /**
     * Finds a destination in the collection by route. This will recursively check the
     * [parent][parent] of this navigation graph if node is not found in this navigation graph.
     *
     * @param route Route to locate
     * @return the node with route
     */
    public fun findNode(route: String?): NavDestination?

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun findNode(route: String, searchParents: Boolean): NavDestination?

    /**
     * @throws NoSuchElementException if there no more elements
     */
    public final override fun iterator(): MutableIterator<NavDestination>

    /**
     * Add all destinations from another collection to this one. As each destination has at most
     * one parent, the destinations will be removed from the given NavGraph.
     *
     * @param other collection of destinations to add. All destinations will be removed from this
     * graph after being added to this graph.
     */
    public fun addAll(other: NavGraph)

    /**
     * Remove a given destination from this NavGraph
     *
     * @param node the destination to remove.
     */
    public fun remove(node: NavDestination)

    /**
     * Clear all destinations from this navigation graph.
     */
    public fun clear()

    /**
     * Sets the starting destination for this NavGraph.
     *
     * @param startDestRoute The route of the destination to be shown when navigating to this
     *                    NavGraph.
     */
    public fun setStartDestination(startDestRoute: String)

    /**
     * The route for the starting destination for this NavGraph. When navigating to the
     * NavGraph, the destination represented by this route is the one the user will initially see.
     */
    public var startDestinationRoute: String?
        private set

    public companion object {
        /**
         * Finds the actual start destination of the graph, handling cases where the graph's starting
         * destination is itself a NavGraph.
         *
         * @return the actual startDestination of the given graph.
         */
        @JvmStatic
        public fun NavGraph.findStartDestination(): NavDestination
    }
}

/**
 * Returns the destination with `route`.
 *
 * @throws IllegalArgumentException if no destination is found with that route.
 */
public expect inline operator fun NavGraph.get(route: String): NavDestination

/** Returns `true` if a destination with `route` is found in this navigation graph. */
public expect operator fun NavGraph.contains(route: String): Boolean

/**
 * Adds a destination to this NavGraph. The destination must have a route set.
 *
 * The destination must not have a [parent][NavDestination.parent] set. If
 * the destination is already part of a [NavGraph], call
 * [NavGraph.remove] before calling this method.</p>
 *
 * @param node destination to add
 */
public expect inline operator fun NavGraph.plusAssign(node: NavDestination)

/**
 * Add all destinations from another collection to this one. As each destination has at most
 * one parent, the destinations will be removed from the given NavGraph.
 *
 * @param other collection of destinations to add. All destinations will be removed from the
 * parameter graph after being added to this graph.
 */
public expect inline operator fun NavGraph.plusAssign(other: NavGraph)

/** Removes `node` from this navigation graph. */
public expect inline operator fun NavGraph.minusAssign(node: NavDestination)
