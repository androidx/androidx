/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.lifecycle.viewmodel.compose.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.toMutableStateMap
import androidx.core.os.bundleOf
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.UUID

@Sampled
@Composable
fun CreationExtrasViewModel() {
    val owner = LocalViewModelStoreOwner.current
    val defaultExtras =
        (owner as? HasDefaultViewModelProviderFactory)?.defaultViewModelCreationExtras
            ?: CreationExtras.Empty
    // Custom extras should always be added on top of the default extras
    val extras = MutableCreationExtras(defaultExtras)
    extras[DEFAULT_ARGS_KEY] = bundleOf("test" to "my_value")
    // This factory is normally created separately and passed in
    val customFactory = remember {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val args = extras[DEFAULT_ARGS_KEY]?.getString("test")
                @Suppress("UNCHECKED_CAST")
                // TestViewModel is a basic ViewModel that sets a String variable
                return TestViewModel(args) as T
            }
        }
    }
    // Create a ViewModel using the custom factory passing in the custom extras
    val viewModel = customFactory.create(TestViewModel::class.java, extras)
    // The value from the extras is now available in the ViewModel
    viewModel.args
}

@Sampled
@Composable
fun CreationExtrasViewModelInitializer() {
    // Just like any call to viewModel(), the default owner is the LocalViewModelStoreOwner.current.
    // The lambda is only called the first time the ViewModel needs to be created.
    val viewModel = viewModel {
        // Within the lambda, you have direct access to the CreationExtras which allows you to call
        // extension methods on CreationExtras such as createSavedStateHandle()
        val handle = createSavedStateHandle()
        // You can send any custom parameter, repository, etc. to your ViewModel.
        SavedStateViewModel(handle, "custom_value")
    }
    // The handle and parameter are now available from the ViewModel
    viewModel.handle
    viewModel.value
}

class TestViewModel(val args: String?) : ViewModel()

class SavedStateViewModel(val handle: SavedStateHandle, val value: String) : ViewModel()

@Sampled
fun SnapshotStateViewModel() {

    /**
     * A simple item that is not inherently [Parcelable]
     */
    data class Item(
        val id: UUID,
        val value: String
    )

    @OptIn(SavedStateHandleSaveableApi::class)
    class SnapshotStateViewModel(handle: SavedStateHandle) : ViewModel() {

        /**
         * A snapshot-backed [MutableList] of a list of items, persisted by the [SavedStateHandle].
         * The size of this set must remain small in expectation, since the maximum size of saved
         * instance state space is limited.
         */
        private val items: MutableList<Item> = handle.saveable(
            key = "items",
            saver = listSaver(
                save = {
                    it.map { item ->
                        listOf(item.id.toString(), item.value)
                    }
                },
                restore = {
                    it.map { saved ->
                        Item(
                            id = UUID.fromString(saved[0]),
                            value = saved[1]
                        )
                    }.toMutableStateList()
                }
            )
        ) {
            mutableStateListOf()
        }

        /**
         * A snapshot-backed [MutableMap] representing a set of selected item ids, persisted by the
         * [SavedStateHandle]. A [MutableSet] is approximated by ignoring the keys.
         * The size of this set must remain small in expectation, since the maximum size of saved
         * instance state space is limited.
         */
        private val selectedItemIds: MutableMap<UUID, Unit> = handle.saveable(
            key = "selectedItemIds",
            saver = listSaver(
                save = { it.keys.map(UUID::toString) },
                restore = { it.map(UUID::fromString).map { id -> id to Unit }.toMutableStateMap() }
            )
        ) {
            mutableStateMapOf()
        }

        /**
         * A snapshot-backed flag representing where selections are enabled, persisted by the
         * [SavedStateHandle].
         */
        var areSelectionsEnabled by handle.saveable("areSelectionsEnabled") {
            mutableStateOf(true)
        }

        /**
         * A list of items paired with a selection state.
         */
        val selectedItems: List<Pair<Item, Boolean>> get() =
            items.map { it to (it.id in selectedItemIds) }

        /**
         * Updates the selection state for the item with [id] to [selected].
         */
        fun selectItem(id: UUID, selected: Boolean) {
            if (selected) {
                selectedItemIds[id] = Unit
            } else {
                selectedItemIds.remove(id)
            }
        }

        /**
         * Adds an item with the given [value].
         */
        fun addItem(value: String) {
            items.add(Item(UUID.randomUUID(), value))
        }
    }
}
