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

/**
 * Class to store `ViewModel`s.
 *
 * An instance of `ViewModelStore` must be retained through configuration changes:
 * if an owner of this `ViewModelStore` is destroyed and recreated due to configuration
 * changes, new instance of an owner should still have the same old instance of
 * `ViewModelStore`.
 *
 * If an owner of this `ViewModelStore` is destroyed and is not going to be recreated,
 * then it should call [clear] on this `ViewModelStore`, so `ViewModel`s would
 * be notified that they are no longer used.
 *
 * Use [ViewModelStoreOwner.getViewModelStore] to retrieve a `ViewModelStore` for
 * activities and fragments.
 */
open class ViewModelStore {

    private val map = mutableMapOf<String, ViewModel>()

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun put(key: String, viewModel: ViewModel) {
        val oldViewModel = map.put(key, viewModel)
        oldViewModel?.onCleared()
    }

    /**
     * Returns the `ViewModel` mapped to the given `key` or null if none exists.
     */
    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    operator fun get(key: String): ViewModel? {
        return map[key]
    }

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun keys(): Set<String> {
        return HashSet(map.keys)
    }

    /**
     * Clears internal storage and notifies `ViewModel`s that they are no longer used.
     */
    fun clear() {
        for (vm in map.values) {
            vm.clear()
        }
        map.clear()
    }
}
