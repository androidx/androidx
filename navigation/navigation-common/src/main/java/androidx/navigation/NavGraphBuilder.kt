/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.annotation.IdRes
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

/**
 * Construct a new [NavGraph]
 *
 * @param id the destination's unique id
 * @param startDestination the starting destination for this NavGraph
 * @param builder the builder used to construct the graph
 * @return the newly constructed NavGraph
 */
@Suppress("Deprecation")
@Deprecated(
    "Use routes to build your NavGraph instead",
    ReplaceWith(
        "navigation(startDestination = startDestination.toString(), route = id.toString()) " +
            "{ builder.invoke() }"
    )
)
public inline fun NavigatorProvider.navigation(
    @IdRes id: Int = 0,
    @IdRes startDestination: Int,
    builder: NavGraphBuilder.() -> Unit
): NavGraph = NavGraphBuilder(this, id, startDestination).apply(builder).build()

/**
 * Construct a new [NavGraph]
 *
 * @param startDestination the starting destination's route for this NavGraph
 * @param route the destination's unique route
 * @param builder the builder used to construct the graph
 * @return the newly constructed NavGraph
 */
public inline fun NavigatorProvider.navigation(
    startDestination: String,
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit
): NavGraph = NavGraphBuilder(this, startDestination, route).apply(builder).build()

/**
 * Construct a new [NavGraph]
 *
 * @param startDestination the starting destination's route from a [KClass] for this NavGraph. The
 *   respective NavDestination must be added with route from a [KClass] in order to match.
 * @param route the graph's unique route as a [KClass]
 * @param typeMap A mapping of KType to custom NavType<*> in the [route]. May be empty if [route]
 *   does not use custom NavTypes.
 * @param builder the builder used to construct the graph
 * @return the newly constructed NavGraph
 */
public inline fun NavigatorProvider.navigation(
    startDestination: KClass<*>,
    route: KClass<*>? = null,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    builder: NavGraphBuilder.() -> Unit
): NavGraph = NavGraphBuilder(this, startDestination, route, typeMap).apply(builder).build()

/**
 * Construct a new [NavGraph]
 *
 * @param startDestination the starting destination's route from an Object for this NavGraph. The
 *   respective NavDestination must be added with route from a [KClass] in order to match.
 * @param route the graph's unique route as a [KClass]
 * @param typeMap A mapping of KType to custom NavType<*> in the [route]. May be empty if [route]
 *   does not use custom NavTypes.
 * @param builder the builder used to construct the graph
 * @return the newly constructed NavGraph
 */
public inline fun NavigatorProvider.navigation(
    startDestination: Any,
    route: KClass<*>? = null,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    builder: NavGraphBuilder.() -> Unit
): NavGraph = NavGraphBuilder(this, startDestination, route, typeMap).apply(builder).build()

/**
 * Construct a nested [NavGraph]
 *
 * @param id the destination's unique id
 * @param startDestination the starting destination for this NavGraph
 * @param builder the builder used to construct the graph
 * @return the newly constructed nested NavGraph
 */
@Suppress("Deprecation")
@Deprecated(
    "Use routes to build your nested NavGraph instead",
    ReplaceWith(
        "navigation(startDestination = startDestination.toString(), route = id.toString()) " +
            "{ builder.invoke() }"
    )
)
public inline fun NavGraphBuilder.navigation(
    @IdRes id: Int,
    @IdRes startDestination: Int,
    builder: NavGraphBuilder.() -> Unit
): Unit = destination(NavGraphBuilder(provider, id, startDestination).apply(builder))

/**
 * Construct a nested [NavGraph]
 *
 * @param startDestination the starting destination's route for this NavGraph
 * @param route the destination's unique route
 * @param builder the builder used to construct the graph
 * @return the newly constructed nested NavGraph
 */
public inline fun NavGraphBuilder.navigation(
    startDestination: String,
    route: String,
    builder: NavGraphBuilder.() -> Unit
): Unit = destination(NavGraphBuilder(provider, startDestination, route).apply(builder))

/**
 * Construct a nested [NavGraph]
 *
 * @param T the graph's unique route from a KClass<T>
 * @param startDestination the starting destination's route from a [KClass] for this NavGraph. The
 *   respective NavDestination must be added with route from a [KClass] in order to match.
 * @param typeMap A mapping of KType to custom NavType<*> in the [T]. May be empty if [T] does not
 *   use custom NavTypes.
 * @param builder the builder used to construct the graph
 * @return the newly constructed nested NavGraph
 */
public inline fun <reified T : Any> NavGraphBuilder.navigation(
    startDestination: KClass<*>,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    builder: NavGraphBuilder.() -> Unit
): Unit = destination(NavGraphBuilder(provider, startDestination, T::class, typeMap).apply(builder))

/**
 * Construct a nested [NavGraph]
 *
 * @param T the graph's unique route from a KClass<T>
 * @param startDestination the starting destination's route from an Object for this NavGraph. The
 *   respective NavDestination must be added with route from a [KClass] in order to match.
 * @param typeMap A mapping of KType to custom NavType<*> in the [T]. May be empty if [T] does not
 *   use custom NavTypes.
 * @param builder the builder used to construct the graph
 * @return the newly constructed nested NavGraph
 */
public inline fun <reified T : Any> NavGraphBuilder.navigation(
    startDestination: Any,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    builder: NavGraphBuilder.() -> Unit
): Unit = destination(NavGraphBuilder(provider, startDestination, T::class, typeMap).apply(builder))

/** DSL for constructing a new [NavGraph] */
@NavDestinationDsl
public open class NavGraphBuilder : NavDestinationBuilder<NavGraph> {
    /** The [NavGraphBuilder]'s [NavigatorProvider]. */
    public val provider: NavigatorProvider
    @IdRes private var startDestinationId: Int = 0
    private var startDestinationRoute: String? = null
    private var startDestinationClass: KClass<*>? = null
    private var startDestinationObject: Any? = null

    /**
     * DSL for constructing a new [NavGraph]
     *
     * @param provider navigator used to create the destination
     * @param id the graph's unique id
     * @param startDestination the starting destination for this NavGraph
     * @return the newly created NavGraph
     */
    @Suppress("Deprecation")
    @Deprecated(
        "Use routes to build your NavGraph instead",
        ReplaceWith(
            "NavGraphBuilder(provider, startDestination = startDestination.toString(), " +
                "route = id.toString())"
        )
    )
    public constructor(
        provider: NavigatorProvider,
        @IdRes id: Int,
        @IdRes startDestination: Int
    ) : super(provider[NavGraphNavigator::class], id) {
        this.provider = provider
        this.startDestinationId = startDestination
    }

    /**
     * DSL for constructing a new [NavGraph]
     *
     * @param provider navigator used to create the destination
     * @param startDestination the starting destination's route for this NavGraph
     * @param route the graph's unique route
     * @return the newly created NavGraph
     */
    public constructor(
        provider: NavigatorProvider,
        startDestination: String,
        route: String?
    ) : super(provider[NavGraphNavigator::class], route) {
        this.provider = provider
        this.startDestinationRoute = startDestination
    }

    /**
     * DSL for constructing a new [NavGraph]
     *
     * @param provider navigator used to create the destination
     * @param startDestination the starting destination's route as a [KClass] for this NavGraph. The
     *   respective NavDestination must be added with route from a [KClass] in order to match.
     * @param route the graph's unique route as a [KClass]
     * @param typeMap A mapping of KType to custom NavType<*> in the [route]. May be empty if
     *   [route] does not use custom NavTypes.
     * @return the newly created NavGraph
     */
    public constructor(
        provider: NavigatorProvider,
        startDestination: KClass<*>,
        route: KClass<*>?,
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>
    ) : super(provider[NavGraphNavigator::class], route, typeMap) {
        this.provider = provider
        this.startDestinationClass = startDestination
    }

    /**
     * DSL for constructing a new [NavGraph]
     *
     * @param provider navigator used to create the destination
     * @param startDestination the starting destination's route as an Object for this NavGraph. The
     *   respective NavDestination must be added with route from a [KClass] in order to match.
     * @param route the graph's unique route as a [KClass]
     * @param typeMap A mapping of KType to custom NavType<*> in the [route]. May be empty if
     *   [route] does not use custom NavTypes.
     * @return the newly created NavGraph
     */
    public constructor(
        provider: NavigatorProvider,
        startDestination: Any,
        route: KClass<*>?,
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>
    ) : super(provider[NavGraphNavigator::class], route, typeMap) {
        this.provider = provider
        this.startDestinationObject = startDestination
    }

    private val destinations = mutableListOf<NavDestination>()

    /** Build and add a new destination to the [NavGraphBuilder] */
    public fun <D : NavDestination> destination(navDestination: NavDestinationBuilder<D>) {
        destinations += navDestination.build()
    }

    /** Adds this destination to the [NavGraphBuilder] */
    public operator fun NavDestination.unaryPlus() {
        addDestination(this)
    }

    /** Add the destination to the [NavGraphBuilder] */
    public fun addDestination(destination: NavDestination) {
        destinations += destination
    }

    @OptIn(InternalSerializationApi::class)
    override fun build(): NavGraph =
        super.build().also { navGraph ->
            navGraph.addDestinations(destinations)
            if (
                startDestinationId == 0 &&
                    startDestinationRoute == null &&
                    startDestinationClass == null &&
                    startDestinationObject == null
            ) {
                if (route != null) {
                    throw IllegalStateException("You must set a start destination route")
                } else {
                    throw IllegalStateException("You must set a start destination id")
                }
            }
            if (startDestinationRoute != null) {
                navGraph.setStartDestination(startDestinationRoute!!)
            } else if (startDestinationClass != null) {
                navGraph.setStartDestination(startDestinationClass!!.serializer()) { it.route!! }
            } else if (startDestinationObject != null) {
                navGraph.setStartDestination(startDestinationObject!!)
            } else {
                navGraph.setStartDestination(startDestinationId)
            }
        }
}
