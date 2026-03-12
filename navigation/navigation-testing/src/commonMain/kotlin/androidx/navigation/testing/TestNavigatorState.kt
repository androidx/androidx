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

package androidx.navigation.testing

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavigatorState
import androidx.savedstate.SavedState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * An implementation of [NavigatorState] that allows testing a [androidx.navigation.Navigator] in
 * isolation (i.e., without requiring a [androidx.navigation.NavController]).
 *
 * The [Lifecycle] of all [NavBackStackEntry] instances added to this TestNavigatorState will be
 * updated as they are added and removed from the state. This work is kicked off on the
 * [coroutineDispatcher].
 */
public expect class TestNavigatorState(
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) : NavigatorState {

    /**
     * Restore a previously saved [NavBackStackEntry]. You must have previously called [pop] with
     * [previouslySavedEntry] and `true`.
     */
    public fun restoreBackStackEntry(previouslySavedEntry: NavBackStackEntry): NavBackStackEntry

    public override fun createBackStackEntry(
        destination: NavDestination,
        arguments: SavedState?,
    ): NavBackStackEntry
}
