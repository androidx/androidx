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
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry.SavedStateProvider
import androidx.savedstate.SavedStateRegistryOwner
import kotlin.jvm.JvmStatic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A handle to saved state passed down to [androidx.lifecycle.ViewModel]. You should use
 * [SavedStateViewModelFactory] if you want to receive this object in `ViewModel`'s constructor.
 *
 * This is a key-value map that will let you write and retrieve objects to and from the saved state.
 * These values will persist after the process is killed by the system and remain available via the
 * same object.
 *
 * You can read a value from it via [get] or observe it via [androidx.lifecycle.LiveData] returned
 * by [getLiveData].
 *
 * You can write a value to it via [set] or setting a value to [androidx.lifecycle.MutableLiveData]
 * returned by [getLiveData].
 */
public expect class SavedStateHandle {

    /**
     * Creates a handle with the given initial arguments.
     *
     * **Important:** This constructor should only be used directly in tests. The created
     * [SavedStateHandle] is not bound to the current [SavedStateRegistryOwner], meaning its
     * internal state will not be restored in the event of a process death.
     *
     * In production, use [viewModelFactory] or implement [ViewModelProvider.Factory] directly,
     * using [CreationExtras.createSavedStateHandle] to create a [SavedStateHandle] that is bound to
     * the current [SavedStateRegistryOwner].
     *
     * @param initialState initial arguments for the SavedStateHandle
     */
    @VisibleForTesting public constructor(initialState: Map<String, Any?>)

    /**
     * Creates a handle with the empty state.
     *
     * **Important:** This constructor should only be used directly in tests. The created
     * [SavedStateHandle] is not bound to the current [SavedStateRegistryOwner], meaning its
     * internal state will not be restored in the event of a process death.
     *
     * In production, use [viewModelFactory] or implement [ViewModelProvider.Factory] directly,
     * using [CreationExtras.createSavedStateHandle] to create a [SavedStateHandle] that is bound
     * with the current [SavedStateRegistryOwner].
     */
    @VisibleForTesting public constructor()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public fun savedStateProvider(): SavedStateProvider

    /**
     * @param key The identifier for the value
     * @return true if there is value associated with the given key.
     */
    @MainThread public operator fun contains(key: String): Boolean

    /**
     * Returns a [StateFlow] that will emit the currently active value associated with the given
     * key.
     *
     * ```
     * val flow = savedStateHandle.getStateFlow(KEY, "defaultValue")
     * ```
     *
     * Since this is a [StateFlow] there will always be a value available which, is why an initial
     * value must be provided. The value of this flow is changed by making a call to [set], passing
     * in the key that references this flow.
     *
     * If there is already a value associated with the given key, the initial value will be ignored.
     *
     * Note: If [T] is an [Array] of [Parcelable] classes, note that you should always use
     * `Array<Parcelable>` and create a typed array from the result as going through process death
     * and recreation (or using the `Don't keep activities` developer option) will result in the
     * type information being lost, thus resulting in a `ClassCastException` if you directly try to
     * collect the result as an `Array<CustomParcelable>`.
     *
     * ```
     * val typedArrayFlow = savedStateHandle.getStateFlow<Array<Parcelable>>(
     *   "KEY"
     * ).map { array ->
     *   // Convert the Array<Parcelable> to an Array<CustomParcelable>
     *   array.map { it as CustomParcelable }.toTypedArray()
     * }
     * ```
     *
     * @param key The identifier for the flow
     * @param initialValue If no value exists with the given `key`, a new one is created with the
     *   given `initialValue`.
     */
    @MainThread public fun <T> getStateFlow(key: String, initialValue: T): StateFlow<T>

    /**
     * Returns a [MutableStateFlow] that will emit the currently active value associated with the
     * given key.
     *
     * ```
     * val flow = savedStateHandle.getMutableStateFlow(KEY, "defaultValue")
     * ```
     *
     * Since this is a [MutableStateFlow] there will always be a value available which, is why an
     * initial value must be provided. The value of this flow is changed by making a call to [set],
     * passing in the key that references this flow or by updating the value of the returned
     * [MutableStateFlow]
     *
     * If there is already a value associated with the given key, the initial value will be ignored.
     *
     * **Note 1:** If [T] is an [Array] of `Parcelable` classes, note that you should always use
     * `Array<Parcelable>` and create a typed array from the result as going through process death
     * and recreation (or using the `Don't keep activities` developer option) will result in the
     * type information being lost, thus resulting in a `ClassCastException` if you directly try to
     * collect the result as an `Array<CustomParcelable>`.
     *
     * ```
     * val typedArrayFlow = savedStateHandle.getMutableStateFlow<Array<Parcelable>>(
     *   "KEY"
     * ).map { array ->
     *   // Convert the Array<Parcelable> to an Array<CustomParcelable>
     *   array.map { it as CustomParcelable }.toTypedArray()
     * }
     * ```
     *
     * **Note 2:** On Android, this method is mutually exclusive with `getLiveData` for the same
     * key. You should use either `getMutableStateFlow` or `getLiveData` to access the stored value,
     * but not both. Using both methods with the same key will result in an `IllegalStateException`.
     *
     * @param key The identifier for the flow
     * @param initialValue If no value exists with the given `key`, a new one is created with the
     *   given `initialValue`.
     */
    @MainThread
    public fun <T> getMutableStateFlow(key: String, initialValue: T): MutableStateFlow<T>

    /**
     * Returns all keys contained in this [SavedStateHandle]
     *
     * Returned set contains all keys: keys used to get LiveData-s, to set SavedStateProviders and
     * keys used in regular [set].
     */
    @MainThread public fun keys(): Set<String>

    /**
     * Returns a value associated with the given key.
     *
     * Note: If [T] is an [Array] of [Parcelable] classes, note that you should always use
     * `Array<Parcelable>` and create a typed array from the result as going through process death
     * and recreation (or using the `Don't keep activities` developer option) will result in the
     * type information being lost, thus resulting in a `ClassCastException` if you directly try to
     * assign the result to an `Array<CustomParcelable>` value.
     *
     * ```
     * val typedArray = savedStateHandle.get<Array<Parcelable>>("KEY").map {
     *   it as CustomParcelable
     * }.toTypedArray()
     * ```
     *
     * @param key a key used to retrieve a value.
     */
    @MainThread public operator fun <T> get(key: String): T?

    /**
     * Associate the given value with the key. The value must have a type that could be stored in
     * [SavedState]
     *
     * This also sets values for any active [LiveData]s or [StateFlow]s.
     *
     * @param key a key used to associate with the given value.
     * @param value object of any type that can be accepted by Bundle.
     * @throws IllegalArgumentException value cannot be saved in saved state
     */
    @MainThread public operator fun <T> set(key: String, value: T?)

    /**
     * Removes a value associated with the given key. If there is a [LiveData] and/or [StateFlow]
     * associated with the given key, they will be removed as well.
     *
     * All changes to [androidx.lifecycle.LiveData]s or [StateFlow]s previously returned by
     * [SavedStateHandle.getLiveData] or [getStateFlow] won't be reflected in the saved state. Also
     * that `LiveData` or `StateFlow` won't receive any updates about new values associated by the
     * given key.
     *
     * @param key a key
     * @return a value that was previously associated with the given key.
     */
    @MainThread public fun <T> remove(key: String): T?

    /**
     * Set a [SavedStateProvider] that will have its state saved into this SavedStateHandle. This
     * provides a mechanism to lazily provide the [SavedState] of saved state for the given key.
     *
     * Calls to [get] with this same key will return the previously saved state as a [SavedState] if
     * it exists.
     *
     * ```
     * Bundle previousState = savedStateHandle.get("custom_object");
     * if (previousState != null) {
     *     // Convert the previousState into your custom object
     * }
     * savedStateHandle.setSavedStateProvider("custom_object", () -> {
     *     Bundle savedState = new Bundle();
     *     // Put your custom object into the Bundle, doing any conversion required
     *     return savedState;
     * });
     * ```
     *
     * Note: calling this method within [SavedStateProvider.saveState] is supported, but will only
     * affect future state saving operations.
     *
     * @param key a key which will populated with a [SavedState] produced by the provider
     * @param provider a SavedStateProvider which will receive a callback to
     *   [SavedStateProvider.saveState] when the state should be saved
     */
    @MainThread public fun setSavedStateProvider(key: String, provider: SavedStateProvider)

    /**
     * Clear any [SavedStateProvider] that was previously set via [setSavedStateProvider].
     *
     * Note: calling this method within [SavedStateProvider.saveState] is supported, but will only
     * affect future state saving operations.
     *
     * @param key a key previously used with [setSavedStateProvider]
     */
    @MainThread public fun clearSavedStateProvider(key: String)

    public companion object {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        @Suppress("DEPRECATION")
        public fun createHandle(
            restoredState: SavedState?,
            defaultState: SavedState?,
        ): SavedStateHandle

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public fun validateValue(value: Any?): Boolean
    }
}
