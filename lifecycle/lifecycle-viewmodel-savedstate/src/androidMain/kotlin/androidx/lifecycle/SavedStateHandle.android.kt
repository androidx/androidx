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
import androidx.lifecycle.internal.SavedStateHandleImpl
import androidx.lifecycle.internal.isAcceptableType
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry.SavedStateProvider
import androidx.savedstate.read
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

public actual class SavedStateHandle {

    private val liveDatas = mutableMapOf<String, SavingStateLiveData<*>>()
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
    public actual fun savedStateProvider(): SavedStateProvider = impl.savedStateProvider

    @MainThread public actual operator fun contains(key: String): Boolean = key in impl

    /**
     * Returns a [androidx.lifecycle.LiveData] that access data associated with the given key.
     *
     * @param key The identifier for the value
     * @see getLiveData
     */
    @MainThread
    public fun <T> getLiveData(key: String): MutableLiveData<T> {
        @Suppress("UNCHECKED_CAST")
        return getLiveDataInternal(key, hasInitialValue = false, initialValue = null)
            as MutableLiveData<T>
    }

    /**
     * Returns a [androidx.lifecycle.LiveData] that access data associated with the given key.
     *
     * ```
     * `LiveData<String> liveData = savedStateHandle.get(KEY, "defaultValue");`
     * ```
     *
     * Keep in mind that [LiveData] can have `null` as a valid value. If the `initialValue` is
     * `null` and the data does not already exist in the [SavedStateHandle], the value of the
     * returned [LiveData] will be set to `null` and observers will be notified. You can call
     * [getLiveData] if you want to avoid dispatching `null` to observers.
     *
     * ```
     * `String defaultValue = ...; // nullable
     * LiveData<String> liveData;
     * if (defaultValue != null) {
     *     liveData = savedStateHandle.getLiveData(KEY, defaultValue);
     * } else {
     *     liveData = savedStateHandle.getLiveData(KEY);
     * }`
     * ```
     *
     * Note: If [T] is an [Array] of [Parcelable] classes, note that you should always use
     * `Array<Parcelable>` and create a typed array from the result as going through process death
     * and recreation (or using the `Don't keep activities` developer option) will result in the
     * type information being lost, thus resulting in a `ClassCastException` if you directly try to
     * observe the result as an `Array<CustomParcelable>`.
     *
     * ```
     * val typedArrayLiveData = savedStateHandle.getLiveData<Array<Parcelable>>(
     *   "KEY"
     * ).map { array ->
     *   // Convert the Array<Parcelable> to an Array<CustomParcelable>
     *   array.map { it as CustomParcelable }.toTypedArray()
     * }
     * ```
     *
     * @param key The identifier for the value
     * @param initialValue If no value exists with the given `key`, a new one is created with the
     *   given `initialValue`. Note that passing `null` will create a [LiveData] with `null` value.
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
        require(key !in impl.mutableFlows) { createMutuallyExclusiveErrorMessage(key) }

        val liveData =
            liveDatas.getOrPut(key) {
                when {
                    key in impl.regular ->
                        SavingStateLiveData(handle = this, key, impl.regular[key])
                    hasInitialValue -> {
                        impl.regular[key] = initialValue
                        SavingStateLiveData(handle = this, key, initialValue)
                    }
                    else -> SavingStateLiveData(handle = this, key)
                }
            }
        @Suppress("UNCHECKED_CAST")
        return liveData as MutableLiveData<T>
    }

    @MainThread
    public actual fun <T> getStateFlow(key: String, initialValue: T): StateFlow<T> {
        return if (key in impl.mutableFlows) {
            // Return existing 'MutableStateFlow' as 'StateFlow' to keep values synchronized.
            impl.getMutableStateFlow(key, initialValue).asStateFlow()
        } else {
            impl.getStateFlow(key, initialValue)
        }
    }

    @MainThread
    public actual fun <T> getMutableStateFlow(key: String, initialValue: T): MutableStateFlow<T> {
        require(key !in liveDatas) { createMutuallyExclusiveErrorMessage(key) }
        return impl.getMutableStateFlow(key, initialValue)
    }

    @MainThread public actual fun keys(): Set<String> = impl.keys() + liveDatas.keys

    @MainThread public actual operator fun <T> get(key: String): T? = impl[key]

    @MainThread
    public actual operator fun <T> set(key: String, value: T?) {
        require(validateValue(value)) {
            "Can't put value with type ${value!!::class.java} into saved state"
        }
        @Suppress("UNCHECKED_CAST") val mutableLiveData = liveDatas[key] as? MutableLiveData<T?>?
        mutableLiveData?.setValue(value)
        impl[key] = value
    }

    @MainThread
    public actual fun <T> remove(key: String): T? =
        impl.remove<T?>(key).also { liveDatas.remove(key)?.detach() }

    @MainThread
    public actual fun setSavedStateProvider(key: String, provider: SavedStateProvider) {
        impl.setSavedStateProvider(key, provider)
    }

    @MainThread
    public actual fun clearSavedStateProvider(key: String) {
        impl.clearSavedStateProvider(key)
    }

    internal class SavingStateLiveData<T> : MutableLiveData<T> {
        private var key: String
        private var handle: SavedStateHandle?

        constructor(handle: SavedStateHandle?, key: String, value: T) : super(value) {
            this.key = key
            this.handle = handle
        }

        constructor(handle: SavedStateHandle?, key: String) : super() {
            this.key = key
            this.handle = handle
        }

        override fun setValue(value: T) {
            handle?.impl?.set(key, value)
            super.setValue(value)
        }

        fun detach() {
            handle = null
        }
    }

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
