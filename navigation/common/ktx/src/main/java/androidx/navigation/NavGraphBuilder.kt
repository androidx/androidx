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

import android.support.annotation.IdRes

/**
 * Construct a new [NavGraph]
 */
inline fun NavigatorProvider.navigation(
        @IdRes id: Int = 0,
        @IdRes startDestination: Int,
        block: NavGraphBuilder.() -> Unit
) = NavGraphBuilder(this, id, startDestination).apply(block).build()

/**
 * Construct a nested [NavGraph]
 */
inline fun NavGraphBuilder.navigation(
        @IdRes id: Int,
        @IdRes startDestination: Int,
        block: NavGraphBuilder.() -> Unit
) = destination(NavGraphBuilder(provider, id, startDestination).apply(block))

/**
 * DSL for constructing a new [NavGraph]
 */
@NavDestinationDsl
class NavGraphBuilder(
        val provider: NavigatorProvider,
        @IdRes id: Int,
        @IdRes private var startDestination: Int
) : NavDestinationBuilder<NavGraph>(provider[NavGraphNavigator::class], id) {
    private val destinations = mutableListOf<NavDestination>()

    /**
     * Build and add a new destination to the [NavGraphBuilder]
     */
    fun <D : NavDestination> destination(navDestination: NavDestinationBuilder<D>) {
        destinations += navDestination.build()
    }

    /**
     * Adds this destination to the [NavGraphBuilder]
     */
    operator fun NavDestination.unaryPlus() {
        addDestination(this)
    }

    /**
     * Add the destination to the [NavGraphBuilder]
     */
    fun addDestination(destination: NavDestination) {
        destinations += destination
    }

    override fun build(): NavGraph = super.build().also { navGraph ->
        navGraph.addDestinations(destinations)
        if (startDestination == 0) {
            throw IllegalStateException("You must set a startDestination")
        }
        navGraph.startDestination = startDestination
    }
}
