/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.savedstate

import androidx.annotation.MainThread
import androidx.savedstate.internal.SavedStateRegistryImpl

actual class SavedStateRegistry
internal actual constructor(
    private val impl: SavedStateRegistryImpl,
) {

    @get:MainThread
    actual val isRestored: Boolean
        get() = impl.isRestored

    @MainThread
    actual fun consumeRestoredStateForKey(key: String): SavedState? =
        impl.consumeRestoredStateForKey(key)

    @MainThread
    actual fun registerSavedStateProvider(key: String, provider: SavedStateProvider) {
        impl.registerSavedStateProvider(key, provider)
    }

    actual fun getSavedStateProvider(key: String): SavedStateProvider? =
        impl.getSavedStateProvider(key)

    @MainThread
    actual fun unregisterSavedStateProvider(key: String) {
        impl.unregisterSavedStateProvider(key)
    }

    actual fun interface SavedStateProvider {
        actual fun saveState(): SavedState
    }
}
