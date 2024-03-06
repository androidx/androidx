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

public actual inline fun NavigatorProvider.navigation(
    startDestination: String,
    route: String?,
    builder: NavGraphBuilder.() -> Unit
): NavGraph = NavGraphBuilder(this, startDestination, route).apply(builder)
    .build()

public actual inline fun NavGraphBuilder.navigation(
    startDestination: String,
    route: String,
    builder: NavGraphBuilder.() -> Unit
): Unit = destination(NavGraphBuilder(provider, startDestination, route).apply(builder))

@NavDestinationDsl
public actual open class NavGraphBuilder
public actual constructor(
    public actual val provider: NavigatorProvider,
    startDestination: String,
    route: String?
) : NavDestinationBuilder<NavGraph>(provider[NavGraphNavigator.name], route) {
    private var startDestinationRoute: String = startDestination

    private val destinations = mutableListOf<NavDestination>()

    public actual fun <D : NavDestination> destination(navDestination: NavDestinationBuilder<D>) {
        destinations += navDestination.build()
    }

    public actual operator fun NavDestination.unaryPlus() {
        addDestination(this)
    }

    public actual fun addDestination(destination: NavDestination) {
        destinations += destination
    }

    override fun build(): NavGraph = super.build().also { navGraph ->
        navGraph.addDestinations(destinations)
        navGraph.setStartDestination(startDestinationRoute)
    }
}
