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
import androidx.navigation.FloatingWindow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavViewModelStoreProvider
import androidx.navigation.NavigatorState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * An implementation of [NavigatorState] that allows testing a
 * [androidx.navigation.Navigator] in isolation (i.e., without requiring a
 * [androidx.navigation.NavController]).
 *
 * An optional [context] can be provided to allow for the usages of
 * [androidx.lifecycle.AndroidViewModel] within the created [NavBackStackEntry]
 * instances.
 *
 * The [Lifecycle] of all [NavBackStackEntry] instances added to this TestNavigatorState
 * will be updated as they are added and removed from the state. This work is kicked off
 * on the [coroutineDispatcher].
 */
public class TestNavigatorState @JvmOverloads constructor(
    private val context: Context? = null,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) : NavigatorState() {

    private val lifecycleOwner: LifecycleOwner = TestLifecycleOwner(
        Lifecycle.State.RESUMED,
        coroutineDispatcher
    )

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

    override fun add(backStackEntry: NavBackStackEntry) {
        super.add(backStackEntry)
        updateMaxLifecycle()
    }

    override fun pop(popUpTo: NavBackStackEntry, saveState: Boolean) {
        val beforePopList = backStack.value
        val poppedList = beforePopList.subList(beforePopList.indexOf(popUpTo), beforePopList.size)
        super.pop(popUpTo, saveState)
        updateMaxLifecycle(poppedList)
    }

    private fun updateMaxLifecycle(poppedList: List<NavBackStackEntry> = emptyList()) {
        runBlocking(coroutineDispatcher) {
            // NavBackStackEntry Lifecycles must be updated on the main thread
            // as per the contract within Lifecycle, so we explicitly swap to the main thread
            // no matter what CoroutineDispatcher was passed to us.
            withContext(Dispatchers.Main.immediate) {
                // Mark all removed NavBackStackEntries as DESTROYED
                for (entry in poppedList.reversed()) {
                    entry.maxLifecycle = Lifecycle.State.DESTROYED
                }
                // Now go through the current list of destinations, updating their Lifecycle state
                val currentList = backStack.value
                var previousEntry: NavBackStackEntry? = null
                for (entry in currentList.reversed()) {
                    entry.maxLifecycle = when {
                        previousEntry == null -> Lifecycle.State.RESUMED
                        previousEntry.destination is FloatingWindow -> Lifecycle.State.STARTED
                        else -> Lifecycle.State.CREATED
                    }
                    previousEntry = entry
                }
            }
        }
    }
}
