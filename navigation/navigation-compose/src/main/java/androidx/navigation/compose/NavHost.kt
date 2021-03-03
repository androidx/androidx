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

package androidx.navigation.compose

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import java.util.UUID

/**
 * Provides in place in the Compose hierarchy for self contained navigation to occur.
 *
 * Once this is called, any Composable within the given [NavGraphBuilder] can be navigated to from
 * the provided [navController].
 *
 * The builder passed into this method is [remember]ed. This means that for this NavHost, the
 * contents of the builder cannot be changed.
 *
 * @sample androidx.navigation.compose.samples.BasicNav
 *
 * @param navController the navController for this host
 * @param startDestination the route for the start destination
 * @param route the route for the graph
 * @param builder the builder used to construct the graph
 */
@Composable
public fun NavHost(
    navController: NavHostController,
    startDestination: String,
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit
) {
    NavHost(
        navController,
        remember(route, startDestination, builder) {
            navController.createGraph(startDestination, route, builder)
        }
    )
}

/**
 * Provides in place in the Compose hierarchy for self contained navigation to occur.
 *
 * Once this is called, any Composable within the given [NavGraphBuilder] can be navigated to from
 * the provided [navController].
 *
 * The graph passed into this method is [remember]ed. This means that for this NavHost, the graph
 * cannot be changed.
 *
 * @param navController the navController for this host
 * @param graph the graph for this host
 */
@Composable
public fun NavHost(navController: NavHostController, graph: NavGraph) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "NavHost requires a ViewModelStoreOwner to be provided via LocalViewModelStoreOwner"
    }
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current.onBackPressedDispatcher
    val rememberedGraph = remember { graph }

    // on successful recompose we setup the navController with proper inputs
    // after the first time, this will only happen again if one of the inputs changes
    DisposableEffect(navController, lifecycleOwner, viewModelStoreOwner, onBackPressedDispatcher) {
        navController.setLifecycleOwner(lifecycleOwner)
        navController.setViewModelStore(viewModelStoreOwner.viewModelStore)
        navController.setOnBackPressedDispatcher(onBackPressedDispatcher)

        onDispose { }
    }

    DisposableEffect(rememberedGraph) {
        navController.graph = rememberedGraph
        onDispose { }
    }

    val saveableStateHolder = rememberSaveableStateHolder()

    // state from the navController back stack
    val currentNavBackStackEntry = navController.currentBackStackEntryAsState().value

    // If the currentNavBackStackEntry is null, we have popped all of the destinations
    // off of the navController back stack and have nothing to show.
    if (currentNavBackStackEntry != null) {
        val destination = currentNavBackStackEntry.destination
        // If the destination is not a compose destination, (e.i. activity, dialog, view, etc)
        // then we do nothing and rely on Navigation to show the proper destination
        if (destination is ComposeNavigator.Destination) {
            // while in the scope of the composable, we provide the navBackStackEntry as the
            // ViewModelStoreOwner and LifecycleOwner
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides currentNavBackStackEntry,
                LocalLifecycleOwner provides currentNavBackStackEntry,
                LocalSavedStateRegistryOwner provides currentNavBackStackEntry
            ) {
                saveableStateHolder.SaveableStateProvider {
                    destination.content(currentNavBackStackEntry)
                }
            }
        }
    }
}

@Composable
private fun SaveableStateHolder.SaveableStateProvider(content: @Composable () -> Unit) {
    val viewModel = viewModel<BackStackEntryIdViewModel>()
    viewModel.saveableStateHolder = this
    SaveableStateProvider(viewModel.id, content)
}

internal class BackStackEntryIdViewModel(handle: SavedStateHandle) : ViewModel() {

    private val IdKey = "SaveableStateHolder_BackStackEntryKey"

    // we create our own id for each back stack entry to support multiple entries of the same
    // destination. this id will be restored by SavedStateHandle
    val id: UUID = handle.get<UUID>(IdKey) ?: UUID.randomUUID().also { handle.set(IdKey, it) }

    var saveableStateHolder: SaveableStateHolder? = null

    // onCleared will be called on the entries removed from the back stack. here we notify
    // RestorableStateHolder that we shouldn't save the state for this id, so when we open this
    // destination again the state will not be restored.
    override fun onCleared() {
        super.onCleared()
        saveableStateHolder?.removeState(id)
    }
}
