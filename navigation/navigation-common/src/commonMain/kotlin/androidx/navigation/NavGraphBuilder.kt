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

/**
 * Construct a new [NavGraph]
 *
 * @param startDestination the starting destination's route for this NavGraph
 * @param route the destination's unique route
 * @param builder the builder used to construct the graph
 *
 * @return the newly constructed NavGraph
 */
public expect inline fun NavigatorProvider.navigation(
    startDestination: String,
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit
): NavGraph

/**
 * Construct a nested [NavGraph]
 *
 * @param startDestination the starting destination's route for this NavGraph
 * @param route the destination's unique route
 * @param builder the builder used to construct the graph
 *
 * @return the newly constructed nested NavGraph
 */
public expect inline fun NavGraphBuilder.navigation(
    startDestination: String,
    route: String,
    builder: NavGraphBuilder.() -> Unit
): Unit

/**
 * DSL for constructing a new [NavGraph]
 */
@NavDestinationDsl
public expect open class NavGraphBuilder
/**
 * DSL for constructing a new [NavGraph]
 *
 * @param provider navigator used to create the destination
 * @param startDestination the starting destination's route for this NavGraph
 * @param route the graph's unique route
 *
 * @return the newly created NavGraph
 */
public constructor(
    provider: NavigatorProvider,
    startDestination: String,
    route: String?
) : NavDestinationBuilder<NavGraph> {
    /**
     * The [NavGraphBuilder]'s [NavigatorProvider].
     */
    public val provider: NavigatorProvider

    /**
     * Build and add a new destination to the [NavGraphBuilder]
     */
    public fun <D : NavDestination> destination(navDestination: NavDestinationBuilder<D>)

    /**
     * Adds this destination to the [NavGraphBuilder]
     */
    public operator fun NavDestination.unaryPlus()

    /**
     * Add the destination to the [NavGraphBuilder]
     */
    public fun addDestination(destination: NavDestination)
}
