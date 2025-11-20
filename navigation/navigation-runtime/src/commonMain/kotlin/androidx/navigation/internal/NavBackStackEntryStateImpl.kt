/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.navigation.internal

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavControllerViewModel
import androidx.navigation.NavDestination
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState

internal class NavBackStackEntryStateImpl {
    internal val id: String
    internal val destinationId: Int
    internal val args: SavedState?
    internal val savedState: SavedState

    internal constructor(entry: NavBackStackEntry, destId: Int) {
        id = entry.id
        destinationId = destId
        args = entry.arguments
        savedState = savedState()
        entry.saveState(savedState)
    }

    internal constructor(state: SavedState) {
        id = state.read { getString(KEY_ID) }
        destinationId = state.read { getInt(KEY_DESTINATION_ID) }
        args = state.read { getSavedState(KEY_ARGS) }
        savedState = state.read { getSavedState(KEY_SAVED_STATE) }
    }

    internal fun writeToState(): SavedState {
        return savedState {
            putString(KEY_ID, id)
            putInt(KEY_DESTINATION_ID, destinationId)
            putSavedState(KEY_ARGS, args ?: savedState())
            putSavedState(KEY_SAVED_STATE, savedState)
        }
    }

    fun instantiate(
        context: NavContext,
        destination: NavDestination,
        args: SavedState?,
        hostLifecycleState: Lifecycle.State,
        viewModel: NavControllerViewModel?,
    ): NavBackStackEntry {
        return NavBackStackEntry.create(
            context,
            destination,
            args,
            hostLifecycleState,
            viewModel,
            id,
            savedState,
        )
    }

    internal companion object {
        internal const val KEY_ID = "nav-entry-state:id"
        internal const val KEY_DESTINATION_ID = "nav-entry-state:destination-id"
        internal const val KEY_ARGS = "nav-entry-state:args"
        internal const val KEY_SAVED_STATE = "nav-entry-state:saved-state"
    }
}
