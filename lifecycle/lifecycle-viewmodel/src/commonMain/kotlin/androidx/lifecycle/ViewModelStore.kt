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
 * Instances of `ViewModelStore` must be retained through configuration changes. If the owner of a
 * `ViewModelStore`, typically a [`ViewModelStoreOwner`], is destroyed and recreated due to a
 * configuration change, the new owner must have the old instance of the `ViewModelStore`.
 *
 * If the owner of a `ViewModelStore` is destroyed and is _not_ going to be recreated, it should
 * call [`clear`] on this `ViewModelStore` so that The `ViewModel`s stored by it are notified that
 * they are no longer needed.
 *
 * Use [`ViewModelStoreOwner.getViewModelStore`] to retrieve a `ViewModelStore` for activities and
 * fragments.
 */
public open class ViewModelStore {

    private val map = mutableMapOf<String, ViewModel>()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun put(key: String, viewModel: ViewModel) {
        val oldViewModel = map.put(key, viewModel)
        oldViewModel?.clear()
    }

    /** Returns the `ViewModel` mapped to the given `key` or null if none exists. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun get(key: String): ViewModel? {
        return map[key]
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun keys(): Set<String> {
        return HashSet(map.keys)
    }

    /** Clears internal storage and notifies `ViewModel`s that they are no longer used. */
    public fun clear() {
        for (vm in map.values) {
            vm.clear()
        }
        map.clear()
    }
}
