/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.lifecycle

import androidx.annotation.RestrictTo
import androidx.collection.mutableScatterMapOf

/**
 * Stores [ViewModel] instances by key.
 *
 * A [ViewModelStore] instance must be retained across configuration changes. If an owner (typically
 * a [ViewModelStoreOwner]) is destroyed and recreated due to a configuration change, the new owner
 * must reuse the previous [ViewModelStore] instance.
 *
 * When the owner is being destroyed permanently (i.e., it will not be recreated), it should call
 * [clear] to notify all stored [ViewModel] instances that they are no longer needed (see
 * [ViewModel.onCleared]).
 *
 * Use [ViewModelStoreOwner.viewModelStore] to retrieve a [ViewModelStore] for activities and
 * fragments.
 *
 * ### Not stable for inheritance
 *
 * **This class is not intended for inheritance.** It is technically `open` for binary compatibility
 * with previous versions, but extending this class is unsupported.
 */
public open class ViewModelStore {

    private val map = mutableScatterMapOf<String, ViewModel>()

    /**
     * Stores [viewModel] under [key], replacing any existing entry.
     *
     * If a [ViewModel] is already stored for [key], it is removed and immediately cleared.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun put(key: String, viewModel: ViewModel) {
        val oldViewModel = map.put(key, viewModel)
        oldViewModel?.clear()
    }

    /** Returns the [ViewModel] stored under [key], or `null` if none exists. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun get(key: String): ViewModel? = map[key]

    /**
     * Returns a snapshot of currently stored keys.
     *
     * The returned set is not backed by this store and will not reflect subsequent changes.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun keys(): Set<String> {
        return buildSet(capacity = map.size) { map.forEachKey { key -> add(key) } }
    }

    /**
     * Clears this store and notifies all stored [ViewModel] instances that they are no longer used.
     *
     * After this call, the store is empty.
     *
     * @see ViewModel.onCleared
     */
    public fun clear() {
        map.forEachValue { viewModel -> viewModel.clear() }
        map.clear()
    }

    override fun toString(): String {
        // Ensure subclasses report their correct name.
        val className = this::class.simpleName ?: "ViewModelStore"
        // Discourage relying on the string output.
        val identity = hashCode().toString(radix = 16)
        return "$className#$identity(keys=${keys()})"
    }
}
