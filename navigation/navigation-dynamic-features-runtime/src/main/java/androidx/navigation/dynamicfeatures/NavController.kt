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
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavType
import kotlin.reflect.KClass
import kotlin.reflect.KType

/** Construct a new [androidx.navigation.NavGraph] that supports dynamic navigation destinations */
@Suppress("Deprecation")
@Deprecated(
    "Use routes to create your dynamic NavGraph instead",
    ReplaceWith(
        "createGraph(startDestination = startDestination.toString(), route = id.toString()) " +
            "{ builder.invoke() }"
    )
)
public inline fun NavController.createGraph(
    @IdRes id: Int = 0,
    @IdRes startDestination: Int,
    builder: DynamicNavGraphBuilder.() -> Unit
): NavGraph = navigatorProvider.navigation(id, startDestination, builder)

/** Construct a new [androidx.navigation.NavGraph] that supports dynamic navigation destinations */
public inline fun NavController.createGraph(
    startDestination: String,
    route: String? = null,
    builder: DynamicNavGraphBuilder.() -> Unit
): NavGraph = navigatorProvider.navigation(startDestination, route, builder)

/**
 * Construct a new [androidx.navigation.NavGraph] that supports dynamic navigation destinations
 *
 * @param startDestination the starting destination's route from a [KClass] for this NavGraph. The
 *   respective NavDestination must be added as a [KClass] in order to match.
 * @param route the graph's unique route from a [KClass]
 * @param typeMap A mapping of KType to custom NavType<*> in the [route]. May be empty if [route]
 *   does not use custom NavTypes.
 * @param builder the builder used to construct the graph
 */
public inline fun NavController.createGraph(
    startDestination: KClass<*>,
    route: KClass<*>? = null,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    builder: DynamicNavGraphBuilder.() -> Unit,
): NavGraph = navigatorProvider.navigation(startDestination, route, typeMap, builder)

/**
 * Construct a new [androidx.navigation.NavGraph] that supports dynamic navigation destinations
 *
 * @param startDestination the starting destination's route from an Object for this NavGraph. The
 *   respective NavDestination must be added as a [KClass] in order to match.
 * @param route the graph's unique route from a [KClass]
 * @param typeMap A mapping of KType to custom NavType<*> in the [route]. May be empty if [route]
 *   does not use custom NavTypes.
 * @param builder the builder used to construct the graph
 */
public inline fun NavController.createGraph(
    startDestination: Any,
    route: KClass<*>? = null,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    builder: DynamicNavGraphBuilder.() -> Unit,
): NavGraph = navigatorProvider.navigation(startDestination, route, typeMap, builder)
