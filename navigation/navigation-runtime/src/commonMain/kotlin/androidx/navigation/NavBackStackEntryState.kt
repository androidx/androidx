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

package androidx.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.internal.NavContext
import androidx.savedstate.SavedState

internal expect class NavBackStackEntryState {
    val id: String
    val destinationId: Int
    val args: SavedState?
    val savedState: SavedState

    constructor(entry: NavBackStackEntry)

    constructor(state: SavedState)

    fun writeToState(): SavedState

    fun instantiate(
        context: NavContext,
        destination: NavDestination,
        hostLifecycleState: Lifecycle.State,
        viewModel: NavControllerViewModel?,
    ): NavBackStackEntry

    internal fun prepareArgs(args: SavedState, context: NavContext): SavedState?
}
