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

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavViewModelStoreProvider
import androidx.navigation.NavigatorState
import java.util.UUID

/**
 * An implementation of [NavigatorState] that allows testing a
 * [androidx.navigation.Navigator] in isolation (i.e., without requiring a
 * [androidx.navigation.NavController]).
 *
 * An optional [context] can be provided to allow for the usages of
 * [androidx.lifecycle.AndroidViewModel] within the created [NavBackStackEntry]
 * instances.
 */
public class TestNavigatorState @JvmOverloads constructor(
    private val context: Context? = null
) : NavigatorState() {

    private val lifecycleOwner: LifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

    private val viewModelStoreProvider = object : NavViewModelStoreProvider {
        private val viewModelStores = mutableMapOf<UUID, ViewModelStore>()
        override fun getViewModelStore(
            backStackEntryUUID: UUID
        ) = viewModelStores.getOrPut(backStackEntryUUID) {
            ViewModelStore()
        }
    }

    override fun createBackStackEntry(
        destination: NavDestination,
        arguments: Bundle?
    ): NavBackStackEntry = NavBackStackEntry.create(
        context, destination, arguments, lifecycleOwner, viewModelStoreProvider
    )
}
