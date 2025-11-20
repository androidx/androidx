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

import androidx.annotation.RestrictTo
import androidx.collection.SparseArrayCompat
import androidx.collection.valueIterator
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.createRoute
import androidx.navigation.NavDestination.DeepLinkMatch
import androidx.navigation.NavGraph
import androidx.navigation.serialization.generateHashCode
import androidx.navigation.serialization.generateRouteWithArgs
import kotlin.reflect.KClass
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

internal class NavGraphImpl(val graph: NavGraph) {
    internal val nodes: SparseArrayCompat<NavDestination> = SparseArrayCompat()
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) get

    internal var startDestId = 0
    internal var startDestIdName: String? = null

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    internal fun matchRouteComprehensive(
        route: String,
        searchChildren: Boolean,
        searchParent: Boolean,
        lastVisited: NavDestination,
    ): DeepLinkMatch? {
        // First try to match with this graph's route
        val bestMatch = graph.matchRoute(route)
        // If searchChildren is true, search through all child destinations for a matching route
        val bestChildMatch =
            if (searchChildren) {
                graph
                    .mapNotNull { child ->
                        when (child) {
                            lastVisited -> null
                            is NavGraph ->
                                child.matchRouteComprehensive(
                                    route,
                                    searchChildren = true,
                                    searchParent = false,
                                    lastVisited = graph,
                                )
                            else -> child.matchRoute(route)
                        }
                    }
                    .maxOrNull()
            } else null

        // If searchParent is true, search through all parents (and their children) destinations
        // for a matching route
        val bestParentMatch =
            graph.parent?.let {
                if (searchParent && it != lastVisited)
                    it.matchRouteComprehensive(route, searchChildren, true, graph)
                else null
            }
        return listOfNotNull(bestMatch, bestChildMatch, bestParentMatch).maxOrNull()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    internal fun matchDeepLinkComprehensive(
        bestMatch: DeepLinkMatch?,
        navDeepLinkRequest: NavDeepLinkRequest,
        searchChildren: Boolean,
        searchParent: Boolean,
        lastVisited: NavDestination,
    ): DeepLinkMatch? {
        // If searchChildren is true, search through all child destinations for a matching deeplink
        val bestChildMatch =
            if (searchChildren) {
                graph
                    .mapNotNull { child ->
                        if (child != lastVisited) child.matchDeepLink(navDeepLinkRequest) else null
                    }
                    .maxOrNull()
            } else null

        // If searchParent is true, search through all parents (and their children) destinations
        // for a matching deeplink
        val bestParentMatch =
            graph.parent?.let {
                if (searchParent && it != lastVisited)
                    it.matchDeepLinkComprehensive(navDeepLinkRequest, searchChildren, true, graph)
                else null
            }
        return listOfNotNull(bestMatch, bestChildMatch, bestParentMatch).maxOrNull()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    internal fun matchDeepLink(
        superBestMatch: DeepLinkMatch?,
        navDeepLinkRequest: NavDeepLinkRequest,
    ): DeepLinkMatch? =
        matchDeepLinkComprehensive(
            superBestMatch,
            navDeepLinkRequest,
            searchChildren = true,
            searchParent = false,
            lastVisited = graph,
        )

    internal fun addDestination(node: NavDestination) {
        val id = node.id
        val innerRoute = node.route
        require(id != 0 || innerRoute != null) {
            "Destinations must have an id or route. Call setId(), setRoute(), or include an " +
                "android:id or app:route in your navigation XML."
        }
        if (graph.route != null) {
            require(innerRoute != graph.route) {
                "Destination $node cannot have the same route as graph $graph"
            }
        }
        require(id != graph.id) { "Destination $node cannot have the same id as graph $graph" }
        val existingDestination = nodes[id]
        if (existingDestination === node) {
            return
        }
        check(node.parent == null) {
            "Destination already has a parent set. Call NavGraph.remove() to remove the previous " +
                "parent."
        }
        if (existingDestination != null) {
            existingDestination.parent = null
        }
        node.parent = graph
        nodes.put(node.id, node)
    }

    internal fun addDestinations(nodes: Collection<NavDestination?>) {
        for (node in nodes) {
            if (node == null) {
                continue
            }
            addDestination(node)
        }
    }

    internal fun addDestinations(vararg nodes: NavDestination) {
        for (node in nodes) {
            addDestination(node)
        }
    }

    internal fun findNode(resId: Int): NavDestination? = findNodeComprehensive(resId, graph, false)

    internal fun findNodeComprehensive(
        resId: Int,
        lastVisited: NavDestination?,
        searchChildren: Boolean,
        matchingDest: NavDestination? = null,
    ): NavDestination? {
        // first search direct children
        var destination = nodes[resId]
        when {
            matchingDest != null ->
                // check parent in case of duplicated destinations to ensure it finds the correct
                // nested destination
                if (destination == matchingDest && destination.parent == matchingDest.parent)
                    return destination
                else destination = null
            else -> if (destination != null) return destination
        }

        if (searchChildren) {
            // then dfs through children. Avoid re-visiting children that were recursing up this
            // way.
            destination =
                nodes.valueIterator().asSequence().firstNotNullOfOrNull { child ->
                    if (child is NavGraph && child != lastVisited) {
                        child.findNodeComprehensive(resId, graph, true, matchingDest)
                    } else null
                }
        }

        // lastly search through parents. Avoid re-visiting parents that were recursing down
        // this way.
        return destination
            ?: if (graph.parent != null && graph.parent != lastVisited) {
                graph.parent!!.findNodeComprehensive(resId, graph, searchChildren, matchingDest)
            } else null
    }

    internal fun findNode(route: String?): NavDestination? {
        return if (!route.isNullOrBlank()) findNode(route, true) else null
    }

    @OptIn(InternalSerializationApi::class)
    internal fun findNode(route: KClass<*>): NavDestination? =
        findNode(route.serializer().generateHashCode())

    @OptIn(InternalSerializationApi::class)
    internal fun <T> findNode(route: T?): NavDestination? =
        route?.let { findNode(it::class.serializer().generateHashCode()) }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    internal fun findNode(route: String, searchParents: Boolean): NavDestination? {
        val destination =
            nodes.valueIterator().asSequence().firstOrNull {
                // first try matching with routePattern
                // if not found with routePattern, try matching with route args
                it.route.equals(route) || it.matchRoute(route) != null
            }

        // Search the parent for the NavDestination if it is not a child of this navigation graph
        // and searchParents is true
        return destination
            ?: if (searchParents && graph.parent != null) graph.parent!!.findNode(route) else null
    }

    internal fun iterator(): MutableIterator<NavDestination> {
        return object : MutableIterator<NavDestination> {
            private var index = -1
            private var wentToNext = false

            override fun hasNext(): Boolean {
                return index + 1 < nodes.size()
            }

            override fun next(): NavDestination {
                if (!hasNext()) {
                    throw NoSuchElementException()
                }
                wentToNext = true
                return nodes.valueAt(++index)
            }

            override fun remove() {
                check(wentToNext) { "You must call next() before you can remove an element" }
                with(nodes) {
                    valueAt(index).parent = null
                    removeAt(index)
                }
                index--
                wentToNext = false
            }
        }
    }

    internal fun addAll(other: NavGraph) {
        val iterator = other.iterator()
        while (iterator.hasNext()) {
            val destination = iterator.next()
            iterator.remove()
            addDestination(destination)
        }
    }

    internal fun remove(node: NavDestination) {
        val index = nodes.indexOfKey(node.id)
        if (index >= 0) {
            nodes.valueAt(index).parent = null
            nodes.removeAt(index)
        }
    }

    internal fun clear() {
        val iterator = iterator()
        while (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }

    internal fun getDisplayName(superName: String): String {
        return if (graph.id != 0) superName else "the root navigation"
    }

    internal var startDestinationId: Int
        get() = startDestId
        set(startDestId) {
            require(startDestId != graph.id) {
                "Start destination $startDestId cannot use the same id as the graph $graph"
            }
            if (startDestinationRoute != null) {
                startDestinationRoute = null
            }
            this.startDestId = startDestId
            startDestIdName = null
        }

    internal fun setStartDestination(startDestId: Int) {
        startDestinationId = startDestId
    }

    internal fun setStartDestination(startDestRoute: String) {
        startDestinationRoute = startDestRoute
    }

    @OptIn(InternalSerializationApi::class)
    internal fun <T : Any> setStartDestination(startDestRoute: KClass<T>) {
        setStartDestination(startDestRoute.serializer()) { startDestination ->
            startDestination.route!!
        }
    }

    @OptIn(InternalSerializationApi::class)
    internal fun <T : Any> setStartDestination(startDestRoute: T) {
        setStartDestination(startDestRoute::class.serializer()) { startDestination ->
            val args = startDestination.arguments.mapValues { it.value.type }
            generateRouteWithArgs(startDestRoute, args)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    internal fun <T> setStartDestination(
        serializer: KSerializer<T>,
        parseRoute: (NavDestination) -> String,
    ) {
        val id = serializer.generateHashCode()
        val startDest = findNode(id)
        checkNotNull(startDest) {
            "Cannot find startDestination ${serializer.descriptor.serialName} from NavGraph. " +
                "Ensure the starting NavDestination was added with route from KClass."
        }
        // when dest id is based on serializer, we expect the dest route to have been generated
        // and set
        startDestinationRoute = parseRoute(startDest)
        // bypass startDestinationId setter so we don't set route back to null
        this.startDestId = id
    }

    internal var startDestinationRoute: String? = null
        set(startDestRoute) {
            startDestId =
                if (startDestRoute == null) {
                    0
                } else {
                    require(startDestRoute != graph.route) {
                        "Start destination $startDestRoute cannot use the same route as the graph $graph"
                    }
                    require(startDestRoute.isNotBlank()) {
                        "Cannot have an empty start destination route"
                    }
                    val internalRoute = createRoute(startDestRoute)
                    internalRoute.hashCode()
                }
            field = startDestRoute
        }

    internal val startDestDisplayName: String
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        get() {
            if (startDestIdName == null) {
                startDestIdName = startDestinationRoute ?: startDestId.toString()
            }
            return startDestIdName!!
        }
}
