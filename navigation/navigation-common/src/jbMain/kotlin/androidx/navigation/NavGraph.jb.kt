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

public actual open class NavGraph actual constructor(
    navGraphNavigator: Navigator<out NavGraph>
) : NavDestination(navGraphNavigator), Iterable<NavDestination> {

    public val nodes = mutableMapOf<String, NavDestination>()
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        get

    public actual fun addDestination(node: NavDestination) {
        val innerRoute = node.route
        require(innerRoute != null) {
            "Destinations must have a route"
        }
        if (route != null) {
            require(innerRoute != route) {
                "Destination $node cannot have the same route as graph $this"
            }
        }
        val existingDestination = nodes[innerRoute]
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
        node.parent = this
        nodes.put(innerRoute, node)
    }

    public actual fun addDestinations(nodes: Collection<NavDestination?>) {
        for (node in nodes) {
            if (node == null) {
                continue
            }
            addDestination(node)
        }
    }

    public actual fun addDestinations(vararg nodes: NavDestination) {
        for (node in nodes) {
            addDestination(node)
        }
    }

    public actual fun findNode(route: String?): NavDestination? {
        return if (!route.isNullOrBlank()) findNode(route, true) else null
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun findNode(route: String, searchParents: Boolean): NavDestination? {
        // first try matching with routePattern
        val destination = nodes[route]

        // Search the parent for the NavDestination if it is not a child of this navigation graph
        // and searchParents is true
        return destination
            ?: if (searchParents && parent != null) parent!!.findNode(route) else null
    }

    public actual final override fun iterator(): MutableIterator<NavDestination> {
        val iterator = nodes.values.iterator()
        return object : MutableIterator<NavDestination> {
            private var current: NavDestination? = null

            override fun hasNext(): Boolean = iterator.hasNext()

            override fun next(): NavDestination = iterator.next().also {
                current = it
            }

            override fun remove() {
                val current = this.current ?:
                    error("You must call next() before you can remove an element")
                current.parent = null
                iterator.remove()
                this.current = null
            }
        }
    }

    public actual fun addAll(other: NavGraph) {
        val iterator = other.iterator()
        while (iterator.hasNext()) {
            val destination = iterator.next()
            iterator.remove()
            addDestination(destination)
        }
    }

    public actual fun remove(node: NavDestination) {
        nodes.remove(node.route)?.also {
            it.parent = null
        }
    }

    public actual fun clear() {
        val iterator = iterator()
        while (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }

    override val displayName: String
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        get() = if (!route.isNullOrBlank()) super.displayName else "the root navigation"

    public actual fun setStartDestination(startDestRoute: String) {
        startDestinationRoute = startDestRoute
    }

    public actual var startDestinationRoute: String? = null

    public override fun toString(): String {
        val sb = StringBuilder()
        sb.append(super.toString())
        val startDestination = findNode(startDestinationRoute)
        sb.append(" startDestination=")
        if (startDestination == null) {
            sb.append(startDestinationRoute)
        } else {
            sb.append("{")
            sb.append(startDestination.toString())
            sb.append("}")
        }
        return sb.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is NavGraph) return false
        return super.equals(other) &&
            nodes == other.nodes
    }

    override fun hashCode(): Int {
        var result = 0
        for ((key, value) in nodes.entries) {
            result = 31 * result + key.hashCode()
            result = 31 * result + value.hashCode()
        }
        return result
    }

    public actual companion object {
        @JvmStatic
        public actual fun NavGraph.findStartDestination(): NavDestination =
            generateSequence(findNode(startDestinationRoute)) {
                if (it is NavGraph) {
                    it.findNode(it.startDestinationRoute)
                } else {
                    null
                }
            }.last()
    }
}

/**
 * Returns the destination with `route`.
 *
 * @throws IllegalArgumentException if no destination is found with that route.
 */
@Suppress("NOTHING_TO_INLINE")
public actual inline operator fun NavGraph.get(route: String): NavDestination =
    findNode(route)
        ?: throw IllegalArgumentException("No destination for $route was found in $this")

/** Returns `true` if a destination with `route` is found in this navigation graph. */
public actual operator fun NavGraph.contains(route: String): Boolean = findNode(route) != null

/**
 * Adds a destination to this NavGraph. The destination must have a route set.
 *
 * The destination must not have a [parent][NavDestination.parent] set. If
 * the destination is already part of a [NavGraph], call
 * [NavGraph.remove] before calling this method.</p>
 *
 * @param node destination to add
 */
@Suppress("NOTHING_TO_INLINE")
public actual inline operator fun NavGraph.plusAssign(node: NavDestination) {
    addDestination(node)
}

/**
 * Add all destinations from another collection to this one. As each destination has at most
 * one parent, the destinations will be removed from the given NavGraph.
 *
 * @param other collection of destinations to add. All destinations will be removed from the
 * parameter graph after being added to this graph.
 */
@Suppress("NOTHING_TO_INLINE")
public actual inline operator fun NavGraph.plusAssign(other: NavGraph) {
    addAll(other)
}

/** Removes `node` from this navigation graph. */
@Suppress("NOTHING_TO_INLINE")
public actual inline operator fun NavGraph.minusAssign(node: NavDestination) {
    remove(node)
}
