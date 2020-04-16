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
import androidx.navigation.NavDestinationDsl
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavigatorProvider

/**
 * Construct a new [DynamicGraphNavigator.DynamicNavGraph]
 */
inline fun NavigatorProvider.navigation(
    @IdRes id: Int = 0,
    @IdRes startDestination: Int,
    moduleName: String? = null,
    progressDestination: Int = 0,
    builder: DynamicNavGraphBuilder.() -> Unit
) = DynamicNavGraphBuilder(
    this,
    id,
    startDestination,
    moduleName,
    progressDestination
).apply(builder).build()

/**
 * Construct a nested [DynamicGraphNavigator.DynamicNavGraph]
 */
inline fun DynamicNavGraphBuilder.navigation(
    @IdRes id: Int,
    @IdRes startDestination: Int,
    moduleName: String? = null,
    progressDestination: Int = 0,
    builder: DynamicNavGraphBuilder.() -> Unit
) = destination(
    DynamicNavGraphBuilder(
        provider,
        id,
        startDestination,
        moduleName,
        progressDestination
    ).apply(builder)
)

/**
 * DSL for constructing a new [DynamicGraphNavigator.DynamicNavGraph]
 */
@NavDestinationDsl
class DynamicNavGraphBuilder(
    provider: NavigatorProvider,
    @IdRes id: Int,
    @IdRes private var startDestination: Int,
    private val moduleName: String?,
    private val progressDestination: Int
) : NavGraphBuilder(provider, id, startDestination) {

    /**
     * @return The [DynamicGraphNavigator.DynamicNavGraph] if a [moduleName] is set.
     * Otherwise a [NavGraph].
     */
    override fun build(): NavGraph =
        super.build().also { navGraph ->
            if (navGraph is DynamicGraphNavigator.DynamicNavGraph) {
                navGraph.moduleName = moduleName
                navGraph.progressDestination = progressDestination
            }
        }
}
