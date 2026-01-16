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

package androidx.navigation.testing

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavigatorState
import androidx.navigation.implementedInJetBrainsFork
import androidx.savedstate.SavedState
import kotlinx.coroutines.CoroutineDispatcher

public actual class TestNavigatorState
actual constructor(coroutineDispatcher: CoroutineDispatcher) : NavigatorState() {
    public actual fun restoreBackStackEntry(
        previouslySavedEntry: NavBackStackEntry
    ): NavBackStackEntry {
        implementedInJetBrainsFork()
    }

    public actual override fun createBackStackEntry(
        destination: NavDestination,
        arguments: SavedState?,
    ): NavBackStackEntry {
        implementedInJetBrainsFork()
    }
}
