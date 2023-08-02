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

package androidx.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.DialogNavigator.Destination

/**
 * Show each [Destination] on the [DialogNavigator]'s back stack as a [Dialog].
 *
 * Note that [NavHost] will call this for you; you do not need to call it manually.
 */
@Composable
public fun DialogHost(dialogNavigator: DialogNavigator) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val dialogBackStack by dialogNavigator.backStack.collectAsState()
    val visibleBackStack = rememberVisibleList(dialogBackStack)
    visibleBackStack.PopulateVisibleList(dialogBackStack)

    visibleBackStack.forEach { backStackEntry ->
        val destination = backStackEntry.destination as Destination
        Dialog(
            onDismissRequest = { dialogNavigator.dismiss(backStackEntry) },
            properties = destination.dialogProperties
        ) {
            DisposableEffect(backStackEntry) {
                onDispose {
                    dialogNavigator.onTransitionComplete(backStackEntry)
                }
            }

            // while in the scope of the composable, we provide the navBackStackEntry as the
            // ViewModelStoreOwner and LifecycleOwner
            backStackEntry.LocalOwnersProvider(saveableStateHolder) {
                destination.content(backStackEntry)
            }
        }
    }
}

@Composable
internal fun MutableList<NavBackStackEntry>.PopulateVisibleList(
    transitionsInProgress: Collection<NavBackStackEntry>
) {
    val isInspecting = LocalInspectionMode.current
    transitionsInProgress.forEach { entry ->
        DisposableEffect(entry.lifecycle) {
            val observer = LifecycleEventObserver { _, event ->
                // show dialog in preview
                if (isInspecting && !contains(entry)) {
                    add(entry)
                }
                // ON_START -> add to visibleBackStack, ON_STOP -> remove from visibleBackStack
                if (event == Lifecycle.Event.ON_START) {
                    // We want to treat the visible lists as Sets but we want to keep
                    // the functionality of mutableStateListOf() so that we recompose in response
                    // to adds and removes.
                    if (!contains(entry)) {
                        add(entry)
                    }
                }
                if (event == Lifecycle.Event.ON_STOP) {
                    remove(entry)
                }
            }
            entry.lifecycle.addObserver(observer)
            onDispose {
                entry.lifecycle.removeObserver(observer)
            }
        }
    }
}

@Composable
internal fun rememberVisibleList(
    transitionsInProgress: Collection<NavBackStackEntry>
): SnapshotStateList<NavBackStackEntry> {
    // show dialog in preview
    val isInspecting = LocalInspectionMode.current
    return remember(transitionsInProgress) {
        mutableStateListOf<NavBackStackEntry>().also {
            it.addAll(
                transitionsInProgress.filter { entry ->
                    if (isInspecting) {
                        true
                    } else {
                        entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                    }
                }
            )
        }
    }
}
