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
package androidx.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.internal.NavBackStackEntryStateImpl
import androidx.navigation.internal.NavContext
import androidx.savedstate.SavedState

internal actual class NavBackStackEntryState {
    actual val id: String
        get() = impl.id

    actual val destinationId: Int
        get() = impl.destinationId

    actual val args: SavedState?
        get() = impl.args

    actual val savedState: SavedState
        get() = impl.savedState

    private val impl: NavBackStackEntryStateImpl

    actual constructor(entry: NavBackStackEntry) {
        impl = NavBackStackEntryStateImpl(entry, entry.destination.id)
    }

    actual constructor(state: SavedState) {
        state.classLoader = javaClass.classLoader
        impl = NavBackStackEntryStateImpl(state)
    }

    actual fun writeToState(): SavedState {
        return impl.writeToState()
    }

    actual fun instantiate(
        context: NavContext,
        destination: NavDestination,
        hostLifecycleState: Lifecycle.State,
        viewModel: NavControllerViewModel?,
    ): NavBackStackEntry {
        val preparedArgs = args?.let { prepareArgs(it, context) }
        return impl.instantiate(context, destination, preparedArgs, hostLifecycleState, viewModel)
    }

    actual fun prepareArgs(args: SavedState, context: NavContext): SavedState? {
        return args.apply { classLoader = context.context?.classLoader }
    }
}
