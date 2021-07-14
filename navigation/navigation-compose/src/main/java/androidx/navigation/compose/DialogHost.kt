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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
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

    dialogBackStack.filter { backStackEntry ->
        backStackEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }.forEach { backStackEntry ->
        val destination = backStackEntry.destination as Destination
        Dialog(
            onDismissRequest = { dialogNavigator.dismiss(backStackEntry) },
            properties = destination.dialogProperties
        ) {
            // while in the scope of the composable, we provide the navBackStackEntry as the
            // ViewModelStoreOwner and LifecycleOwner
            backStackEntry.LocalOwnersProvider(saveableStateHolder) {
                destination.content(backStackEntry)
            }
        }
    }
}
