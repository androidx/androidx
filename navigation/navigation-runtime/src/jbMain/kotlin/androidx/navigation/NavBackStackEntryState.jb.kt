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

internal actual class NavBackStackEntryState actual constructor(entry: NavBackStackEntry) {
    actual val id: String = entry.id
    val destinationRoute: String = entry.destination.route!!
    actual val args: Bundle? = entry.arguments
    actual val savedState: Bundle = Bundle()

    init {
        entry.saveState(savedState)
    }

    fun instantiate(
        destination: NavDestination,
        hostLifecycleState: Lifecycle.State,
        viewModel: NavControllerViewModel?
    ): NavBackStackEntry {
        return NavBackStackEntry.create(
            destination, args,
            hostLifecycleState, viewModel,
            id, savedState
        )
    }
}
