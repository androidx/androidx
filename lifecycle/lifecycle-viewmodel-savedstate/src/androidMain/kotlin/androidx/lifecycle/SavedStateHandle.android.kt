/*
 * Copyright 2018 The Android Open Source Project
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

import android.os.Parcelable
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.internal.LiveDataSavedStateValue
import androidx.lifecycle.internal.SavedStateHandleImpl
import androidx.lifecycle.internal.SimpleSavedStateValue
import androidx.lifecycle.internal.StateFlowSavedStateValue
import androidx.lifecycle.internal.isAcceptableType
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateContainer
import androidx.savedstate.SavedStateRegistry.SavedStateProvider
import androidx.savedstate.SavedStateValue
import androidx.savedstate.read
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public actual class SavedStateHandle {

    private var impl: SavedStateHandleImpl

    internal actual constructor(container: SavedStateContainer) {
        impl = SavedStateHandleImpl(container)
    }

    @VisibleForTesting
    public actual constructor(initialState: Map<String, Any?>) {
        impl = SavedStateHandleImpl(initialState)
    }

    @VisibleForTesting
    public actual constructor() {
        impl = SavedStateHandleImpl()
    }

    @MainThread
    public actual operator fun contains(key: String): Boolean {
        if (key !in impl) return false
        // We need to check if the value is initialized to be consistent with previous behavior
        // where contains returned false. Since we are tracking all values as part of the
        // container, we would incorrectly return true otherwise.
        val savedStateValue = impl.container.getSavedStateValue<Any?, SavedStateValue<Any?>>(key)
        return savedStateValue !is LiveDataSavedStateValue<*> || savedStateValue.value.isInitialized
    }

    /**
     * Returns a [LiveData] that accesses data associated with the given [key].
     *
     * @param key identifier of the value
     * @see getLiveData
     */
    @MainThread
    public fun <T> getLiveData(key: String): MutableLiveData<T> {
        @Suppress("UNCHECKED_CAST")
        return getLiveDataInternal(key, hasInitialValue = false, initialValue = null)
            as MutableLiveData<T>
    }

    /**
     * Returns a [LiveData] that accesses data associated with the given [key].
     *
     * ```kotlin
     * val liveData = savedStateHandle.getLiveData(KEY, "defaultValue")
     * ```
     *
     * Note that [LiveData] can have `null` as a valid value. If the [initialValue] is `null` and
     * the data does not already exist in the [SavedStateHandle], the value of the returned
     * [LiveData] will be set to `null` and observers will be notified. You can call [getLiveData]
     * to avoid dispatching `null` to observers.
     *
     * ```kotlin
     * val defaultValue = ... // nullable
     * val liveData = if (defaultValue != null) {
     *     savedStateHandle.getLiveData(KEY, defaultValue)
     * } else {
     *     savedStateHandle.getLiveData(KEY)
     * }
     * ```
     *
     * If [T] is an [Array] of [Parcelable] classes, you should always use `Array<Parcelable>` and
     * create a typed array from the result. Going through process death and recreation (or using
     * the "Don't keep activities" developer option) will result in the type information being lost,
     * causing a `ClassCastException` if you directly try to observe the result as
     * `Array<CustomParcelable>`.
     *
     * ```kotlin
     * val typedArrayLiveData = savedStateHandle.getLiveData<Array<Parcelable>>(
     *     "KEY"
     * ).map { array ->
     *     // Convert the Array<Parcelable> to an Array<CustomParcelable>
     *     array.map { it as CustomParcelable }.toTypedArray()
     * }
     * ```
     *
     * @param key identifier of the value
     * @param initialValue value to use if no value is associated with [key]
     */
    @MainThread
    public fun <T> getLiveData(key: String, initialValue: T): MutableLiveData<T> {
        return getLiveDataInternal(key, hasInitialValue = true, initialValue)
    }

    private fun <T> getLiveDataInternal(
        key: String,
        hasInitialValue: Boolean,
        initialValue: T,
    ): MutableLiveData<T> {
        val existing = impl.container.getSavedStateValue<T, SavedStateValue<T>>(key)
        require(existing !is StateFlowSavedStateValue<*>) {
            createMutuallyExclusiveErrorMessage(key)
        }

        val liveDataSavedStateValue =
            when (existing) {
                is LiveDataSavedStateValue<*> -> existing
                is SimpleSavedStateValue<T> -> {
                    @Suppress("UNCHECKED_CAST") val existingValue = existing.value as T
                    val liveData = MutableLiveData(existingValue)
                    LiveDataSavedStateValue(liveData).also {
                        impl.container.putSavedStateValue(key, it)
                    }
                }
                null -> {
                    val liveData =
                        if (hasInitialValue) {
                            MutableLiveData(initialValue)
                        } else {
                            MutableLiveData()
                        }
                    LiveDataSavedStateValue(liveData).also {
                        impl.container.putSavedStateValue(key, it)
                    }
                }
                else -> throw IllegalArgumentException(createMutuallyExclusiveErrorMessage(key))
            }
        @Suppress("UNCHECKED_CAST")
        return (liveDataSavedStateValue as LiveDataSavedStateValue<T>).value
    }

    @MainThread
    public actual fun <T> getStateFlow(key: String, initialValue: T): StateFlow<T> {
        val existing = impl.container.getSavedStateValue<T, SavedStateValue<T>>(key)
        require(existing !is LiveDataSavedStateValue<*>) {
            createMutuallyExclusiveErrorMessage(key)
        }
        return impl.getStateFlow(key, initialValue)
    }

    @MainThread
    public actual fun <T> getMutableStateFlow(key: String, initialValue: T): MutableStateFlow<T> {
        val existing = impl.container.getSavedStateValue<T, SavedStateValue<T>>(key)
        require(existing !is LiveDataSavedStateValue<*>) {
            createMutuallyExclusiveErrorMessage(key)
        }
        return impl.getMutableStateFlow(key, initialValue)
    }

    @MainThread public actual fun keys(): Set<String> = impl.keys()

    @MainThread
    public actual operator fun <T> get(key: String): T? {
        val existing = impl.container.getSavedStateValue<T, SavedStateValue<T>>(key)
        if (existing is LiveDataSavedStateValue<*>) {
            @Suppress("UNCHECKED_CAST")
            return existing.value.value as T?
        }
        return impl[key]
    }

    @MainThread
    public actual operator fun <T> set(key: String, value: T?) {
        require(validateValue(value)) {
            "Can't put value with type ${value!!::class.java} into saved state"
        }
        val existing = impl.container.getSavedStateValue<T, SavedStateValue<T>>(key)
        if (existing is LiveDataSavedStateValue<*>) {
            @Suppress("UNCHECKED_CAST")
            (existing.value as MutableLiveData<T?>).value = value
        } else {
            impl[key] = value
        }
    }

    @MainThread
    public actual fun <T> remove(key: String): T? {
        val latestValue = get<T>(key)
        impl.remove<T>(key)
        return latestValue
    }

    @MainThread
    public actual fun setSavedStateProvider(key: String, provider: SavedStateProvider) {
        impl.setSavedStateProvider(key, provider)
    }

    @MainThread
    public actual fun clearSavedStateProvider(key: String) {
        impl.clearSavedStateProvider(key)
    }

    @MainThread public actual fun asContainer(): SavedStateContainer = impl.asContainer()

    @MainThread
    public actual fun getOrCreateContainer(key: String): SavedStateContainer =
        impl.getOrCreateContainer(key)

    @MainThread
    public actual fun getOrCreateSavedStateHandle(key: String): SavedStateHandle =
        SavedStateHandle(impl.getOrCreateContainer(key))

    public actual companion object {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        public actual fun createHandle(
            restoredState: SavedState?,
            defaultState: SavedState?,
        ): SavedStateHandle {
            val initialState = restoredState ?: defaultState

            // If there is no restored state or default state, an empty SavedStateHandle is created.
            if (initialState == null) return SavedStateHandle()

            // When restoring state, we prioritize the restored state as the single source of truth.
            // This ensures that the state is restored exactly as it was saved, preventing any
            // potential conflicts or inconsistencies with the default state.
            // This is particularly important when dealing with Parcelables.
            initialState.classLoader = SavedStateHandle::class.java.classLoader!!

            return SavedStateHandle(initialState = initialState.read { toMap() })
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public actual fun validateValue(value: Any?): Boolean = isAcceptableType(value)
    }
}

private fun createMutuallyExclusiveErrorMessage(key: String): String {
    return "StateFlow and LiveData are mutually exclusive for the same key. Please use either " +
        "'getMutableStateFlow' or 'getLiveData' for key '$key', but not both."
}
