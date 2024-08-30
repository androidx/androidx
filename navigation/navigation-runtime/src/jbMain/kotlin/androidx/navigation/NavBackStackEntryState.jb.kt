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

import androidx.core.bundle.Bundle
import androidx.lifecycle.Lifecycle

internal data class NavBackStackEntryState(
    val id: String,
    val destinationId: Int,
    val args: Bundle?,
    val savedState: Bundle
) {
    constructor(entry: NavBackStackEntry) : this(
        id = entry.id,
        destinationId = entry.destination.id,
        args = entry.arguments,
        savedState = Bundle()
    ) {
        entry.saveState(savedState)
    }

    fun instantiate(
        destination: NavDestination,
        hostLifecycleState: Lifecycle.State,
        viewModel: NavControllerViewModel?
    ): NavBackStackEntry {
        return NavBackStackEntry.create(
            destination,
            args,
            hostLifecycleState,
            viewModel,
            id,
            savedState
        )
    }

    fun toBundle(): Bundle = Bundle().apply {
        putString(KEY_ID, id)
        putInt(KEY_DESTINATION_ID, destinationId)
        putBundle(KEY_ARGS, args)
        putBundle(KEY_SAVED_STATE, savedState)
    }

    companion object {
        private const val KEY_ID = "NavBackStackEntryState.id"
        private const val KEY_DESTINATION_ID = "NavBackStackEntryState.destinationId"
        private const val KEY_ARGS = "NavBackStackEntryState.args"
        private const val KEY_SAVED_STATE = "NavBackStackEntryState.savedState"

        fun fromBundle(bundle: Bundle?): NavBackStackEntryState? {
            if (bundle == null) return null
            val id = bundle.getString(KEY_ID) ?: return null
            val destinationId = bundle.getInt(KEY_DESTINATION_ID)
            val args = bundle.getBundle(KEY_ARGS) ?: return null
            val savedState = bundle.getBundle(KEY_SAVED_STATE) ?: return null
            return NavBackStackEntryState(
                id = id,
                destinationId = destinationId,
                args = args,
                savedState = savedState
            )
        }
    }
}
