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

import android.content.ContextWrapper
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.onCommit
import androidx.compose.runtime.remember
import androidx.compose.runtime.savedinstancestate.ExperimentalRestorableStateHolder
import androidx.compose.runtime.savedinstancestate.RestorableStateHolder
import androidx.compose.runtime.savedinstancestate.rememberRestorableStateHolder
import androidx.compose.ui.platform.ContextAmbient
import androidx.compose.ui.platform.LifecycleOwnerAmbient
import androidx.compose.ui.platform.ViewModelStoreOwnerAmbient
import androidx.compose.ui.viewinterop.viewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
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
 * @sample androidx.navigation.compose.samples.BasicNav
 *
 * @param route the route for the graph
 * @param startDestination the route for the start destination
 * @param builder the builder used to construct the graph
 */
@Composable
public fun NavHost(
    navController: NavHostController,
    route: String? = null,
    startDestination: String,
    builder: NavGraphBuilder.() -> Unit
) {
    NavHost(
        navController,
        remember (route, startDestination, builder) {
            navController.createGraph(route, startDestination, builder)
        }
    )
}

/**
 * Provides in place in the Compose hierarchy for self contained navigation to occur.
 *
 * Once this is called, any Composable within the given [NavGraphBuilder] can be navigated to from
 * the provided [navController].
 *
 * @param navController the navController for this host
 * @param graph the graph for this host
 */
@OptIn(ExperimentalRestorableStateHolder::class)
@Composable
internal fun NavHost(navController: NavHostController, graph: NavGraph) {
    var context = ContextAmbient.current
    val lifecycleOwner = LifecycleOwnerAmbient.current
    val viewModelStore = ViewModelStoreOwnerAmbient.current.viewModelStore

    // on successful recompose we setup the navController with proper inputs
    // after the first time, this will only happen again if one of the inputs changes
    onCommit(navController, lifecycleOwner, viewModelStore) {
        navController.setLifecycleOwner(lifecycleOwner)
        navController.setViewModelStore(viewModelStore)

        // unwrap the context until we find an OnBackPressedDispatcherOwner
        while (context is ContextWrapper) {
            if (context is OnBackPressedDispatcherOwner) {
                navController.setOnBackPressedDispatcher(
                    (context as OnBackPressedDispatcherOwner).onBackPressedDispatcher
                )
                break
            }
            context = (context as ContextWrapper).baseContext
        }
    }

    onCommit(graph) {
        navController.graph = graph
    }

    val restorableStateHolder = rememberRestorableStateHolder<UUID>()

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
            Providers(
                ViewModelStoreOwnerAmbient provides currentNavBackStackEntry,
                LifecycleOwnerAmbient provides currentNavBackStackEntry
            ) {
                restorableStateHolder.withRestorableState {
                    destination.content(currentNavBackStackEntry)
                }
            }
        }
    }
}

@OptIn(ExperimentalRestorableStateHolder::class)
@Composable
private fun RestorableStateHolder<UUID>.withRestorableState(content: @Composable () -> Unit) {
    val viewModel = viewModel<BackStackEntryIdViewModel>()
    viewModel.restorableStateHolder = this
    withRestorableState(viewModel.id, content)
}

@OptIn(ExperimentalRestorableStateHolder::class)
internal class BackStackEntryIdViewModel(handle: SavedStateHandle) : ViewModel() {

    private val IdKey = "RestorableStateHolder_BackStackEntryKey"

    // we create our own id for each back stack entry to support multiple entries of the same
    // destination. this id will be restored by SavedStateHandle
    val id: UUID = handle.get<UUID>(IdKey) ?: UUID.randomUUID().also { handle.set(IdKey, it) }

    var restorableStateHolder: RestorableStateHolder<UUID>? = null

    // onCleared will be called on the entries removed from the back stack. here we notify
    // RestorableStateHolder that we shouldn't save the state for this id, so when we open this
    // destination again the state will not be restored.
    override fun onCleared() {
        super.onCleared()
        restorableStateHolder?.removeState(id)
    }
}
