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

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.IdRes
import androidx.annotation.RestrictTo
import androidx.collection.SparseArrayCompat
import androidx.core.content.res.use
import androidx.navigation.common.R
import java.lang.StringBuilder

/**
 * NavGraph is a collection of [NavDestination] nodes fetchable by ID.
 *
 * A NavGraph serves as a 'virtual' destination: while the NavGraph itself will not appear
 * on the back stack, navigating to the NavGraph will cause the
 * [starting destination][getStartDestination] to be added to the back stack.
 *
 * Construct a new NavGraph. This NavGraph is not valid until you
 * [add a destination][addDestination] and [set the starting destination][setStartDestination].
 *
 * @param navGraphNavigator The [NavGraphNavigator] which this destination will be associated
 *                          with. Generally retrieved via a
 *                          [NavController]'s[NavigatorProvider.getNavigator] method.
 */
public open class NavGraph(navGraphNavigator: Navigator<out NavGraph>) :
    NavDestination(navGraphNavigator), Iterable<NavDestination> {

    public val nodes: SparseArrayCompat<NavDestination> = SparseArrayCompat<NavDestination>()
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        get
    private var startDestId = 0
    private var startDestIdName: String? = null

    override fun onInflate(context: Context, attrs: AttributeSet) {
        super.onInflate(context, attrs)
        context.resources.obtainAttributes(
            attrs,
            R.styleable.NavGraphNavigator
        ).use {
            startDestinationId = it.getResourceId(R.styleable.NavGraphNavigator_startDestination, 0)
            startDestIdName = getDisplayName(context, startDestId)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override fun matchDeepLink(navDeepLinkRequest: NavDeepLinkRequest): DeepLinkMatch? {
        // First search through any deep links directly added to this NavGraph
        val bestMatch = super.matchDeepLink(navDeepLinkRequest)
        // Then search through all child destinations for a matching deep link
        val bestChildMatch = mapNotNull { child ->
            child.matchDeepLink(navDeepLinkRequest)
        }.maxOrNull()

        return listOfNotNull(bestMatch, bestChildMatch).maxOrNull()
    }

    /**
     * Adds a destination to this NavGraph. The destination must have an
     * [NavDestination.id] id} set.
     *
     * The destination must not have a [parent][NavDestination.parent] set. If
     * the destination is already part of a [navigation graph][NavGraph], call
     * [remove] before calling this method.
     *
     * @param node destination to add
     * @throws IllegalArgumentException if destination does not have an id, the destination has
     * the same id as the graph, or the destination already has a parent.
     */
    public fun addDestination(node: NavDestination) {
        val id = node.id
        val innerRoute = node.route
        require(id != 0 || innerRoute != null) {
            "Destinations must have an id or route. Call setId(), setRoute(), or include an " +
                "android:id or app:route in your navigation XML."
        }
        if (route != null) {
            require(innerRoute != route) {
                "Destination $node cannot have the same route as graph $this"
            }
        }
        require(id != this.id) { "Destination $node cannot have the same id as graph $this" }
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
        node.parent = this
        nodes.put(node.id, node)
    }

    /**
     * Adds multiple destinations to this NavGraph. Each destination must have an
     * [NavDestination.id] id} set.
     *
     * Each destination must not have a [parent][NavDestination.parent] set. If any
     * destination is already part of a [navigation graph][NavGraph], call [remove] before
     * calling this method.
     *
     * @param nodes destinations to add
     */
    public fun addDestinations(nodes: Collection<NavDestination?>) {
        for (node in nodes) {
            if (node == null) {
                continue
            }
            addDestination(node)
        }
    }

    /**
     * Adds multiple destinations to this NavGraph. Each destination must have an
     * [NavDestination.id] id} set.
     *
     * Each destination must not have a [parent][NavDestination.parent] set. If any
     * destination is already part of a [navigation graph][NavGraph], call [remove] before
     * calling this method.
     *
     * @param nodes destinations to add
     */
    public fun addDestinations(vararg nodes: NavDestination) {
        for (node in nodes) {
            addDestination(node)
        }
    }

    /**
     * Finds a destination in the collection by ID. This will recursively check the
     * [parent][parent] of this navigation graph if node is not found in this navigation graph.
     *
     * @param resid ID to locate
     * @return the node with ID resid
     */
    public fun findNode(@IdRes resid: Int): NavDestination? {
        return findNode(resid, true)
    }

    /**
     * Finds a destination in the collection by route. This will recursively check the
     * [parent][parent] of this navigation graph if node is not found in this navigation graph.
     *
     * @param route Route to locate
     * @return the node with route
     */
    public fun findNode(route: String?): NavDestination? {
        return if (!route.isNullOrBlank()) findNode(route, true) else null
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun findNode(@IdRes resid: Int, searchParents: Boolean): NavDestination? {
        val destination = nodes[resid]
        // Search the parent for the NavDestination if it is not a child of this navigation graph
        // and searchParents is true
        return destination
            ?: if (searchParents && parent != null) parent!!.findNode(resid) else null
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun findNode(route: String, searchParents: Boolean): NavDestination? {
        val id = createRoute(route).hashCode()
        val destination = nodes[id]
        // Search the parent for the NavDestination if it is not a child of this navigation graph
        // and searchParents is true
        return destination
            ?: if (searchParents && parent != null) parent!!.findNode(route) else null
    }

    /**
     * @throws NoSuchElementException if there no more elements
     */
    public final override fun iterator(): MutableIterator<NavDestination> {
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

    /**
     * Add all destinations from another collection to this one. As each destination has at most
     * one parent, the destinations will be removed from the given NavGraph.
     *
     * @param other collection of destinations to add. All destinations will be removed from this
     * graph after being added to this graph.
     */
    public fun addAll(other: NavGraph) {
        val iterator = other.iterator()
        while (iterator.hasNext()) {
            val destination = iterator.next()
            iterator.remove()
            addDestination(destination)
        }
    }

    /**
     * Remove a given destination from this NavGraph
     *
     * @param node the destination to remove.
     */
    public fun remove(node: NavDestination) {
        val index = nodes.indexOfKey(node.id)
        if (index >= 0) {
            nodes.valueAt(index).parent = null
            nodes.removeAt(index)
        }
    }

    /**
     * Clear all destinations from this navigation graph.
     */
    public fun clear() {
        val iterator = iterator()
        while (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val displayName: String
        /**
         * @hide
         */
        get() = if (id != 0) super.displayName else "the root navigation"

    /**
     * Gets the starting destination for this NavGraph. When navigating to the NavGraph, this
     * destination is the one the user will initially see.
     *
     * @return the start destination
     */
    @IdRes
    @Deprecated("Use getStartDestinationId instead.", ReplaceWith("startDestinationId"))
    public fun getStartDestination(): Int = startDestinationId

    /**
     * The starting destination id for this NavGraph. When navigating to the NavGraph, the
     * destination represented by this id is the one the user will initially see.
     */
    @get:IdRes
    public var startDestinationId: Int
        get() = startDestId
        private set(startDestId) {
            require(startDestId != id) {
                "Start destination $startDestId cannot use the same id as the graph $this"
            }
            if (startDestinationRoute != null) {
                startDestinationRoute = null
            }
            this.startDestId = startDestId
            startDestIdName = null
        }

    /**
     * Sets the starting destination for this NavGraph.
     *
     * This will clear any previously set [startDestinationRoute].
     *
     * @param startDestId The id of the destination to be shown when navigating to this
     *                    NavGraph.
     */
    public fun setStartDestination(startDestId: Int) {
        startDestinationId = startDestId
    }

    /**
     * Sets the starting destination for this NavGraph.
     *
     * This will override any previously set [startDestinationId]
     *
     * @param startDestRoute The route of the destination to be shown when navigating to this
     *                    NavGraph.
     */
    public fun setStartDestination(startDestRoute: String) {
        startDestinationRoute = startDestRoute
    }

    /**
     * The route for the starting destination for this NavGraph. When navigating to the
     * NavGraph, the destination represented by this route is the one the user will initially see.
     */
    public var startDestinationRoute: String? = null
        private set(startDestRoute) {
            startDestId = if (startDestRoute == null) {
                0
            } else {
                require(startDestRoute != route) {
                    "Start destination $startDestRoute cannot use the same route as the graph $this"
                }
                require(startDestRoute.isNotBlank()) {
                    "Cannot have an empty start destination route"
                }
                val internalRoute = createRoute(startDestRoute)
                internalRoute.hashCode()
            }
            field = startDestRoute
        }

    public val startDestDisplayName: String
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        get() {
            if (startDestIdName == null) {
                startDestIdName = startDestId.toString()
            }
            return startDestIdName!!
        }

    public override fun toString(): String {
        val sb = StringBuilder()
        sb.append(super.toString())
        val startDestination = findNode(startDestinationRoute) ?: findNode(startDestinationId)
        sb.append(" startDestination=")
        if (startDestination == null) {
            when {
                startDestinationRoute != null -> sb.append(startDestinationRoute)
                startDestIdName != null -> sb.append(startDestIdName)
                else -> sb.append("0x${Integer.toHexString(startDestId)}")
            }
        } else {
            sb.append("{")
            sb.append(startDestination.toString())
            sb.append("}")
        }
        return sb.toString()
    }
}

/**
 * Returns the destination with `id`.
 *
 * @throws IllegalArgumentException if no destination is found with that id.
 */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun NavGraph.get(@IdRes id: Int): NavDestination =
    findNode(id) ?: throw IllegalArgumentException("No destination for $id was found in $this")

/**
 * Returns the destination with `route`.
 *
 * @throws IllegalArgumentException if no destination is found with that route.
 */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun NavGraph.get(route: String): NavDestination =
    findNode(route)
        ?: throw IllegalArgumentException("No destination for $route was found in $this")

/** Returns `true` if a destination with `id` is found in this navigation graph. */
public operator fun NavGraph.contains(@IdRes id: Int): Boolean = findNode(id) != null

/** Returns `true` if a destination with `route` is found in this navigation graph. */
public operator fun NavGraph.contains(route: String): Boolean = findNode(route) != null

/**
 * Adds a destination to this NavGraph. The destination must have an
 * [id][NavDestination.id] set.
 *
 * The destination must not have a [parent][NavDestination.parent] set. If
 * the destination is already part of a [NavGraph], call
 * [NavGraph.remove] before calling this method.</p>
 *
 * @param node destination to add
 */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun NavGraph.plusAssign(node: NavDestination) {
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
public inline operator fun NavGraph.plusAssign(other: NavGraph) {
    addAll(other)
}

/** Removes `node` from this navigation graph. */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun NavGraph.minusAssign(node: NavDestination) {
    remove(node)
}
