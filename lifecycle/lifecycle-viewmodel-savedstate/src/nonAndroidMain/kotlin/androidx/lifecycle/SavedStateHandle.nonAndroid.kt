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

package androidx.lifecycle

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.internal.SavedStateHandleImpl
import androidx.lifecycle.internal.isAcceptableType
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.read
import kotlin.jvm.JvmStatic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

public actual class SavedStateHandle {

    private var impl: SavedStateHandleImpl

    @VisibleForTesting
    public actual constructor(initialState: Map<String, Any?>) {
        impl = SavedStateHandleImpl(initialState)
    }

    @VisibleForTesting
    public actual constructor() {
        impl = SavedStateHandleImpl()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun savedStateProvider(): SavedStateRegistry.SavedStateProvider =
        impl.savedStateProvider()

    @MainThread public actual operator fun contains(key: String): Boolean = key in impl

    @MainThread
    public actual fun <T> getStateFlow(key: String, initialValue: T): StateFlow<T> {
        // On platforms other than Android, LiveData is not available.
        // Therefore, there's no need to check for mutual exclusivity with LiveData.
        // We can directly use getMutableStateFlow and convert it to a StateFlow.
        return impl.getMutableStateFlow(key, initialValue).asStateFlow()
    }

    @MainThread
    public actual fun <T> getMutableStateFlow(key: String, initialValue: T): MutableStateFlow<T> {
        return impl.getMutableStateFlow(key, initialValue)
    }

    @MainThread public actual fun keys(): Set<String> = impl.keys()

    @MainThread public actual operator fun <T> get(key: String): T? = impl.get(key)

    @MainThread
    public actual operator fun <T> set(key: String, value: T?): Unit = impl.set(key, value)

    @MainThread public actual fun <T> remove(key: String): T? = impl.remove(key)

    @MainThread
    public actual fun setSavedStateProvider(
        key: String,
        provider: SavedStateRegistry.SavedStateProvider,
    ): Unit = impl.setSavedStateProvider(key, provider)

    @MainThread
    public actual fun clearSavedStateProvider(key: String) {
        impl.clearSavedStateProvider(key)
    }

    public actual companion object {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        @Suppress("DEPRECATION")
        public actual fun createHandle(
            restoredState: SavedState?,
            defaultState: SavedState?,
        ): SavedStateHandle {
            val initialState = restoredState ?: defaultState

            // If there is no restored state or default state, an empty SavedStateHandle is created.
            if (initialState == null) return SavedStateHandle()

            return SavedStateHandle(initialState = initialState.read { toMap() })
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public actual fun validateValue(value: Any?): Boolean = isAcceptableType(value)
    }
}
