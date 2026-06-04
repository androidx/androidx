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

import androidx.annotation.RestrictTo
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.ViewModelStoreProvider

/**
 * Interface that allows you to retrieve a [ViewModelStore] associated with a particular
 * [NavBackStackEntry.id].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface NavViewModelStoreProvider {
    public fun get(key: String): ViewModelStore

    public fun clear(key: String)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun NavViewModelStoreProvider(
    viewModelStore: ViewModelStore? = null
): NavViewModelStoreProvider =
    NavViewModelStoreProviderImpl(
        provider =
            ViewModelStoreProvider(
                parentStore = viewModelStore,
                parentKey = "androidx.navigation.NavControllerViewModel",
            )
    )

private class NavViewModelStoreProviderImpl(private val provider: ViewModelStoreProvider) :
    NavViewModelStoreProvider {

    override fun get(key: String): ViewModelStore {
        return provider.getOrCreate(key)
    }

    override fun clear(key: String) {
        provider.clearKey(key)
    }

    override fun toString(): String = "NavControllerViewModel(provider=$provider)"
}
