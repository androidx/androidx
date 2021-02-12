/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.hilt.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.HiltViewModelFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.getBackStackEntry

/**
 * Returns an existing
 * [HiltViewModel](https://dagger.dev/api/latest/dagger/hilt/android/lifecycle/HiltViewModel)
 * -annotated [ViewModel] or creates a new one scoped to the current navigation graph present on
 * the {@link NavController} back stack:
 * ```
 * @Composable
 * fun ExampleScreen() {
 *     val viewmodel = hiltNavGraphViewModel<ExampleViewModel>()
 * }
 * ```
 *
 * If no navigation graph is currently present then the current scope will be used, usually, a
 * fragment or an activity.
 */
@Composable
inline fun <reified VM : ViewModel> hiltNavGraphViewModel(): VM {
    val owner = LocalViewModelStoreOwner.current
    return if (owner is NavBackStackEntry) {
        hiltNavGraphViewModel(owner)
    } else {
        viewModel()
    }
}

/**
 * Returns an existing
 * [HiltViewModel](https://dagger.dev/api/latest/dagger/hilt/android/lifecycle/HiltViewModel)
 * -annotated [ViewModel] or creates a new one scoped to the current navigation graph present on
 * the [NavController] back stack:
 * ```
 * @Composable
 * fun MyApp() {
 *     // ...
 *     NavHost(navController, startDestination = startRoute) {
 *         composable("example") { backStackEntry ->
 *             val viewmodel = hiltNavGraphViewModel<ExampleViewModel>(backStackEntry)
 *         }
 *     }
 * }
 * ```
 *
 * @param backStackEntry The entry of a [NavController] back stack.
 */
@Composable
inline fun <reified VM : ViewModel> hiltNavGraphViewModel(backStackEntry: NavBackStackEntry): VM {
    val viewModelFactory = HiltViewModelFactory(
        context = LocalContext.current,
        navBackStackEntry = backStackEntry
    )
    return ViewModelProvider(backStackEntry, viewModelFactory).get(VM::class.java)
}

/**
 * Returns an existing
 * [HiltViewModel](https://dagger.dev/api/latest/dagger/hilt/android/lifecycle/HiltViewModel)
 * -annotated [ViewModel] or creates a new one scoped to the current navigation graph present on
 * the [NavController] back stack:
 * ```
 * @Composable
 * fun MyApp() {
 *     // ...
 *     NavHost(navController, startDestination = startRoute) {
 *         navigation(startDestination = innerStartRoute, route = "Parent") {
 *             // ...
 *             composable("example") { backStackEntry ->
 *                 val parentViewModel = hiltNavGraphViewModel<ParentViewModel>("Parent")
 *             }
 *         }
 *     }
 * }
 * ```
 * @param route route of a destination that exists on the [NavController] back stack.
 */
@Composable
inline fun <reified VM : ViewModel> NavController.hiltNavGraphViewModel(route: String) =
    hiltNavGraphViewModel<VM>(this.getBackStackEntry(route))
