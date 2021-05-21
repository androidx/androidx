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

/**
 * A host is a single context or container for navigation via a [NavController].
 *
 * It is strongly recommended to construct the nav controller by instantiating a
 * [NavHostController], which offers additional APIs specifically for a NavHost.
 * The NavHostController should still only be externally accessible as a [NavController],
 * rather than directly exposing it as a [NavHostController].
 *
 * Navigation hosts must:
 *
 *  * Handle [saving][NavController.saveState] and
 * [restoring][NavController.restoreState] their controller's state
 *  * Call [Navigation.setViewNavController] on their root view
 *  * Route system Back button events to the NavController either by manually calling
 * [NavController.popBackStack] or by calling
 * [NavHostController.setOnBackPressedDispatcher]
 * when constructing the NavController.
 *
 * Optionally, a navigation host should consider calling:
 *
 *  * Call [NavHostController.setLifecycleOwner] to associate the
 * NavController with a specific Lifecycle.
 *  * Call [NavHostController.setViewModelStore] to enable usage of
 * [NavController.getViewModelStoreOwner] and navigation graph scoped ViewModels.
 */
public interface NavHost {
    /**
     * The [navigation controller][NavController] for this navigation host.
     */
    public val navController: NavController
}

/**
 * Construct a new [NavGraph]
 */
public inline fun NavHost.createGraph(
    @IdRes id: Int = 0,
    @IdRes startDestination: Int,
    builder: NavGraphBuilder.() -> Unit
): NavGraph = navController.createGraph(id, startDestination, builder)

/**
 * Construct a new [NavGraph]
 */
public inline fun NavHost.createGraph(
    startDestination: String,
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit
): NavGraph = navController.createGraph(startDestination, route, builder)
