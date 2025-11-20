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

@file:JvmName("NavGraphKt")
@file:JvmMultifileClass

package androidx.navigation

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.IdRes
import androidx.annotation.RestrictTo
import androidx.collection.SparseArrayCompat
import androidx.collection.forEach
import androidx.collection.size
import androidx.collection.valueIterator
import androidx.core.content.res.use
import androidx.navigation.common.R
import androidx.navigation.internal.NavContext
import androidx.navigation.internal.NavGraphImpl
import kotlin.reflect.KClass
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer

public actual open class NavGraph actual constructor(navGraphNavigator: Navigator<out NavGraph>) :
    NavDestination(navGraphNavigator), Iterable<NavDestination> {

    private val impl: NavGraphImpl = NavGraphImpl(this)

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual val nodes: SparseArrayCompat<NavDestination> by impl::nodes

    override fun onInflate(context: Context, attrs: AttributeSet) {
        super.onInflate(context, attrs)
        context.resources.obtainAttributes(attrs, R.styleable.NavGraphNavigator).use {
            startDestinationId = it.getResourceId(R.styleable.NavGraphNavigator_startDestination, 0)
            impl.startDestIdName = getDisplayName(NavContext(context), impl.startDestId)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun matchRouteComprehensive(
        route: String,
        searchChildren: Boolean,
        searchParent: Boolean,
        lastVisited: NavDestination,
    ): DeepLinkMatch? {
        return impl.matchRouteComprehensive(route, searchChildren, searchParent, lastVisited)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun matchDeepLinkComprehensive(
        navDeepLinkRequest: NavDeepLinkRequest,
        searchChildren: Boolean,
        searchParent: Boolean,
        lastVisited: NavDestination,
    ): DeepLinkMatch? {
        // First search through any deep links directly added to this NavGraph
        val bestMatch = super.matchDeepLink(navDeepLinkRequest)
        return impl.matchDeepLinkComprehensive(
            bestMatch,
            navDeepLinkRequest,
            searchChildren,
            searchParent,
            lastVisited,
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual override fun matchDeepLink(
        navDeepLinkRequest: NavDeepLinkRequest
    ): DeepLinkMatch? =
        impl.matchDeepLink(super.matchDeepLink(navDeepLinkRequest), navDeepLinkRequest)

    public actual fun addDestination(node: NavDestination) {
        impl.addDestination(node)
    }

    public actual fun addDestinations(nodes: Collection<NavDestination?>) {
        impl.addDestinations(nodes)
    }

    public actual fun addDestinations(vararg nodes: NavDestination) {
        impl.addDestinations(*nodes)
    }

    /**
     * Finds a destination in the collection by ID. This will recursively check the [parent][parent]
     * of this navigation graph if node is not found in this navigation graph.
     *
     * @param resId ID to locate
     * @return the node with ID resId
     */
    public fun findNode(@IdRes resId: Int): NavDestination? = impl.findNode(resId)

    /**
     * Searches all children and parents recursively.
     *
     * Does not revisit graphs (whether it's a child or parent) if it has already been visited.
     *
     * @param resId the [NavDestination.id]
     * @param lastVisited the previously visited node
     * @param searchChildren searches the graph's children for the node when true
     * @param matchingDest an optional NavDestination that the node should match with. This is
     *   because [resId] is only unique to a local graph. Nodes in sibling graphs can have the same
     *   id.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun findNodeComprehensive(
        @IdRes resId: Int,
        lastVisited: NavDestination?,
        searchChildren: Boolean,
        matchingDest: NavDestination?,
    ): NavDestination? {
        return impl.findNodeComprehensive(resId, lastVisited, searchChildren, matchingDest)
    }

    public actual fun findNode(route: String?): NavDestination? {
        return impl.findNode(route)
    }

    public actual inline fun <reified T> findNode(): NavDestination? = findNode(T::class)

    @OptIn(InternalSerializationApi::class)
    public actual fun findNode(route: KClass<*>): NavDestination? = impl.findNode(route)

    @OptIn(InternalSerializationApi::class)
    public actual fun <T> findNode(route: T?): NavDestination? = impl.findNode(route)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun findNode(route: String, searchParents: Boolean): NavDestination? {
        return impl.findNode(route, searchParents)
    }

    public actual final override fun iterator(): MutableIterator<NavDestination> {
        return impl.iterator()
    }

    public actual fun addAll(other: NavGraph) {
        impl.addAll(other)
    }

    public actual fun remove(node: NavDestination) {
        impl.remove(node)
    }

    public actual fun clear() {
        impl.clear()
    }

    actual override val displayName: String
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) get() = impl.getDisplayName(super.displayName)

    /**
     * Gets the starting destination for this NavGraph. When navigating to the NavGraph, this
     * destination is the one the user will initially see.
     *
     * @return the start destination
     */
    @IdRes
    @Deprecated("Use getStartDestinationId instead.", ReplaceWith("startDestinationId"))
    public fun getStartDestination(): Int = impl.startDestinationId

    /**
     * The starting destination id for this NavGraph. When navigating to the NavGraph, the
     * destination represented by this id is the one the user will initially see.
     */
    @get:IdRes
    public actual var startDestinationId: Int by impl::startDestinationId
        private set

    /**
     * Sets the starting destination for this NavGraph.
     *
     * This will clear any previously set [startDestinationRoute].
     *
     * @param startDestId The id of the destination to be shown when navigating to this NavGraph.
     */
    public fun setStartDestination(startDestId: Int) {
        impl.setStartDestination(startDestId)
    }

    public actual fun setStartDestination(startDestRoute: String) {
        impl.setStartDestination(startDestRoute)
    }

    public actual inline fun <reified T : Any> setStartDestination() {
        setStartDestination(T::class)
    }

    /**
     * Sets the starting destination for this NavGraph.
     *
     * This will override any previously set [startDestinationId]
     *
     * @param startDestRoute The [KClass] of route [T] to be shown when navigating to this NavGraph.
     */
    @JvmSynthetic
    @OptIn(InternalSerializationApi::class)
    public actual fun <T : Any> setStartDestination(startDestRoute: KClass<T>) {
        impl.setStartDestination(startDestRoute)
    }

    @JvmSynthetic
    @OptIn(InternalSerializationApi::class)
    public actual fun <T : Any> setStartDestination(startDestRoute: T) {
        impl.setStartDestination(startDestRoute)
    }

    // unfortunately needs to be public so reified setStartDestination can access this
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @OptIn(ExperimentalSerializationApi::class)
    public actual fun <T> setStartDestination(
        serializer: KSerializer<T>,
        parseRoute: (NavDestination) -> String,
    ) {
        impl.setStartDestination(serializer, parseRoute)
    }

    public actual var startDestinationRoute: String? by impl::startDestinationRoute
        private set

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual val startDestDisplayName: String by impl::startDestDisplayName

    public override fun toString(): String {
        return buildString {
            append(super.toString())
            val startDestination = findNode(startDestinationRoute) ?: findNode(startDestinationId)
            append(" startDestination=")
            if (startDestination == null) {
                when {
                    startDestinationRoute != null -> append(startDestinationRoute)
                    impl.startDestIdName != null -> append(impl.startDestIdName)
                    else -> append("0x${Integer.toHexString(impl.startDestId)}")
                }
            } else {
                append("{")
                append(startDestination.toString())
                append("}")
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is NavGraph) return false
        return super.equals(other) &&
            nodes.size == other.nodes.size &&
            startDestinationId == other.startDestinationId &&
            nodes.valueIterator().asSequence().all { it == other.nodes.get(it.id) }
    }

    override fun hashCode(): Int {
        var result = startDestinationId
        nodes.forEach { key, value ->
            result = 31 * result + key
            result = 31 * result + value.hashCode()
        }
        return result
    }

    public actual companion object {
        @JvmStatic
        public actual fun NavGraph.findStartDestination(): NavDestination = childHierarchy().last()

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public actual fun NavGraph.childHierarchy(): Sequence<NavDestination> =
            generateSequence(this as NavDestination) {
                if (it is NavGraph) {
                    it.findNode(it.startDestinationId)
                } else {
                    null
                }
            }
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

/** Returns `true` if a destination with `id` is found in this navigation graph. */
public operator fun NavGraph.contains(@IdRes id: Int): Boolean = findNode(id) != null
