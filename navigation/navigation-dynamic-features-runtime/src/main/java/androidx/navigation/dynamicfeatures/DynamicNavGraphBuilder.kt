/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.navigation.dynamicfeatures

import androidx.annotation.IdRes
import androidx.navigation.ExperimentalSafeArgsApi
import androidx.navigation.NavDestination
import androidx.navigation.NavDestinationDsl
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.NavigatorProvider
import androidx.navigation.dynamicfeatures.DynamicActivityNavigator.Destination
import androidx.navigation.get
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Construct a new [DynamicGraphNavigator.DynamicNavGraph]
 *
 * @param id NavGraph id.
 * @param startDestination Id start destination in the graph
 * @param builder Another builder for chaining.
 */
@Suppress("Deprecation")
@Deprecated(
    "Use routes to create your DynamicNavGraph instead",
    ReplaceWith(
        "navigation(startDestination = startDestination.toString(), route = id.toString()) " +
            "{ builder.invoke() }"
    )
)
public inline fun NavigatorProvider.navigation(
    @IdRes id: Int = 0,
    @IdRes startDestination: Int,
    builder: DynamicNavGraphBuilder.() -> Unit
): NavGraph = DynamicNavGraphBuilder(
    this,
    id,
    startDestination
).apply(builder).build()

/**
 * Construct a nested [DynamicGraphNavigator.DynamicNavGraph]
 *
 * @param id NavGraph id.
 * @param startDestination Id start destination in the graph
 * @param builder Another builder for chaining.
 */
@Suppress("Deprecation")
@Deprecated(
    "Use routes to create your DynamicNavGraph instead",
    ReplaceWith(
        "navigation(startDestination = startDestination.toString(), route = id.toString()) " +
            "{ builder.invoke() }"
    )
)
public inline fun DynamicNavGraphBuilder.navigation(
    @IdRes id: Int,
    @IdRes startDestination: Int,
    builder: DynamicNavGraphBuilder.() -> Unit
): Unit = destination(
    DynamicNavGraphBuilder(
        provider,
        id,
        startDestination
    ).apply(builder)
)

/**
 * Construct a new [DynamicGraphNavigator.DynamicNavGraph]
 *
 * @param route NavGraph route.
 * @param startDestination route start destination in the graph
 * @param builder Another builder for chaining.
 */
public inline fun NavigatorProvider.navigation(
    startDestination: String,
    route: String? = null,
    builder: DynamicNavGraphBuilder.() -> Unit
): NavGraph = DynamicNavGraphBuilder(
    this,
    startDestination,
    route
).apply(builder).build()

/**
 * Construct a nested [DynamicGraphNavigator.DynamicNavGraph]
 *
 * @param route NavGraph route.
 * @param startDestination route start destination in the graph
 * @param builder Another builder for chaining.
 */
public inline fun DynamicNavGraphBuilder.navigation(
    startDestination: String,
    route: String,
    builder: DynamicNavGraphBuilder.() -> Unit
): Unit = destination(
    DynamicNavGraphBuilder(
        provider,
        startDestination,
        route
    ).apply(builder)
)

/**
 * Construct a new [DynamicGraphNavigator.DynamicNavGraph]
 *
 * @param startDestination the starting destination's route from a [KClass] for this NavGraph. The
 * respective NavDestination must be added with route from a [KClass] in order to match.
 * @param route the graph's unique route as a [KClass]
 * @param typeMap A mapping of KType to custom NavType<*> in the [route]. May be empty if
 * [route] does not use custom NavTypes.
 * @param builder Another builder for chaining.
 */
@ExperimentalSafeArgsApi
public inline fun NavigatorProvider.navigation(
    startDestination: KClass<*>,
    route: KClass<*>? = null,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    builder: DynamicNavGraphBuilder.() -> Unit
): NavGraph = DynamicNavGraphBuilder(
    this,
    startDestination,
    route,
    typeMap
).apply(builder).build()

/**
 * Construct a new [DynamicGraphNavigator.DynamicNavGraph]
 *
 * @param startDestination the starting destination's route from an Object for this NavGraph. The
 * respective NavDestination must be added with route from a [KClass] in order to match.
 * @param route the graph's unique route as a [KClass]
 * @param typeMap A mapping of KType to custom NavType<*> in the [route]. May be empty if
 * [route] does not use custom NavTypes.
 * @param builder Another builder for chaining.
 */
@ExperimentalSafeArgsApi
public inline fun NavigatorProvider.navigation(
    startDestination: Any,
    route: KClass<*>? = null,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    builder: DynamicNavGraphBuilder.() -> Unit
): NavGraph = DynamicNavGraphBuilder(
    this,
    startDestination,
    route,
    typeMap
).apply(builder).build()

/**
 * Construct a nested [DynamicGraphNavigator.DynamicNavGraph]
 *
 * @param startDestination the starting destination's route from a [KClass] for this NavGraph. The
 * respective NavDestination must be added with route from a [KClass] in order to match.
 * @param T the graph's unique route as a [KClass]
 * @param typeMap A mapping of KType to custom NavType<*> in the [T]. May be empty if
 * [T] does not use custom NavTypes.
 * @param builder Another builder for chaining.
 */
@ExperimentalSafeArgsApi
public inline fun <reified T : Any> DynamicNavGraphBuilder.navigation(
    startDestination: KClass<*>,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    builder: DynamicNavGraphBuilder.() -> Unit
): Unit = destination(
    DynamicNavGraphBuilder(
        provider,
        startDestination,
        T::class,
        typeMap
    ).apply(builder)
)

/**
 * Construct a nested [DynamicGraphNavigator.DynamicNavGraph]
 *
 * @param startDestination the starting destination's route from an Object for this NavGraph. The
 * respective NavDestination must be added with route from a [KClass] in order to match.
 * @param T the graph's unique route as a [KClass]
 * @param typeMap A mapping of KType to custom NavType<*> in the [T]. May be empty if
 * [T] does not use custom NavTypes.
 * @param builder Another builder for chaining.
 */
@ExperimentalSafeArgsApi
public inline fun <reified T : Any> DynamicNavGraphBuilder.navigation(
    startDestination: Any,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    builder: DynamicNavGraphBuilder.() -> Unit
): Unit = destination(
    DynamicNavGraphBuilder(
        provider,
        startDestination,
        T::class,
        typeMap
    ).apply(builder)
)

/**
 * DSL for constructing a new [DynamicGraphNavigator.DynamicNavGraph]
 */
@NavDestinationDsl
public class DynamicNavGraphBuilder : NavGraphBuilder {
    @IdRes private var startDestinationId: Int = 0
    private var startDestinationRoute: String? = null

    @Suppress("Deprecation")
    @Deprecated(
        "Use routes to create your DynamicNavGraphBuilder instead",
        ReplaceWith(
            "DynamicNavGraphBuilder(provider, startDestination = startDestination.toString(), " +
                "route = id.toString())"
        )
    )
    public constructor(
        provider: NavigatorProvider,
        @IdRes id: Int,
        @IdRes startDestination: Int
    ) : super(provider, id, startDestination) {
        this.startDestinationId = startDestination
    }

    public constructor(
        provider: NavigatorProvider,
        startDestination: String,
        route: String? = null
    ) : super(provider, startDestination, route) {
        this.startDestinationRoute = startDestination
    }

    /**
     * DSL for constructing a new [DynamicGraphNavigator.DynamicNavGraph]
     *
     * @param provider navigator used to create the destination
     * @param startDestination the starting destination's route as a [KClass] for this NavGraph. The
     * respective NavDestination must be added with route from a [KClass] in order to match.
     * @param route the graph's unique route as a [KClass]
     * @param typeMap A mapping of KType to custom NavType<*> in the [route]. May be empty if
     * [route] does not use custom NavTypes.
     *
     * @return the newly created NavGraph
     */
    @ExperimentalSafeArgsApi
    public constructor(
        provider: NavigatorProvider,
        startDestination: KClass<*>,
        route: KClass<*>?,
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>
    ) : super(provider, startDestination, route, typeMap)

    /**
     * DSL for constructing a new [DynamicGraphNavigator.DynamicNavGraph]
     *
     * @param provider navigator used to create the destination
     * @param startDestination the starting destination's route as an Object for this NavGraph. The
     * respective NavDestination must be added with route from a [KClass] in order to match.
     * @param route the graph's unique route as a [KClass]
     * @param typeMap A mapping of KType to custom NavType<*> in the [route].  May be empty if
     * [route] does not use custom NavTypes.
     *
     * @return the newly created NavGraph
     */
    @ExperimentalSafeArgsApi
    public constructor(
        provider: NavigatorProvider,
        startDestination: Any,
        route: KClass<*>?,
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>
    ) : super(provider, startDestination, route, typeMap)

    /**
     * The module name of this [Destination]'s dynamic feature module. This has to be the
     * same as defined in the dynamic feature module's AndroidManifest.xml file.
     */
    public var moduleName: String? = null

    private var _progressDestination: Int = 0
    /**
     * ID of the destination displayed during module installation. This generally does
     * not need to be set, but is instead filled in by the NavHost via
     * [DynamicGraphNavigator.installDefaultProgressDestination].
     *
     * Setting this clears any previously set [progressDestinationRoute].
     */
    public var progressDestination: Int
        get() = _progressDestination
        set(p) {
            if (progressDestinationRoute != null) {
                progressDestinationRoute = null
            }
            _progressDestination = p
        }

    /**
     * Route of the destination displayed during module installation. This generally does
     * not need to be set, but is instead filled in by the NavHost via
     * [DynamicGraphNavigator.installDefaultProgressDestination].
     *
     * Setting this overrides any previously set [progressDestination].
     */
    public var progressDestinationRoute: String? = null
        set(progDestRoute) {
            _progressDestination = if (progDestRoute == null) {
                0
            } else {
                require(progDestRoute.isNotBlank()) {
                    "Cannot have an empty progress destination route"
                }
                NavDestination.createRoute(progressDestinationRoute).hashCode()
            }
            field = progDestRoute
        }

    /**
     * @return The [DynamicGraphNavigator.DynamicNavGraph].
     */
    override fun build(): NavGraph =
        super.build().also { navGraph ->
            if (navGraph is DynamicGraphNavigator.DynamicNavGraph) {
                navGraph.moduleName = moduleName
                if (navGraph.route != null) {
                    navGraph.progressDestination =
                        NavDestination.createRoute(progressDestinationRoute).hashCode()
                } else {
                    navGraph.progressDestination = progressDestination
                }
                if (progressDestination == 0) {
                    val navGraphNavigator: DynamicGraphNavigator =
                        provider[DynamicGraphNavigator::class]
                    navGraphNavigator.destinationsWithoutDefaultProgressDestination
                        .add(navGraph)
                }
            }
        }
}
