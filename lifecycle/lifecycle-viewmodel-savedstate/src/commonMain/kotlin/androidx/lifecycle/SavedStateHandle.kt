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
import androidx.core.bundle.Bundle
import androidx.savedstate.SavedStateRegistry.SavedStateProvider
import kotlin.jvm.JvmStatic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

expect class SavedStateHandle {

    /**
     * Creates a handle with the given initial arguments.
     *
     * @param initialState initial arguments for the SavedStateHandle
     */
    constructor(initialState: Map<String, Any?>)

    /**
     * Creates a handle with the empty state.
     */
    constructor()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun savedStateProvider(): SavedStateProvider

    /**
     * @param key          The identifier for the value
     *
     * @return true if there is value associated with the given key.
     */
    @MainThread
    operator fun contains(key: String): Boolean

    /**
     * Returns a [StateFlow] that will emit the currently active value associated with the given
     * key.
     *
     * ```
     * val flow = savedStateHandle.getStateFlow(KEY, "defaultValue")
     * ```
     * Since this is a [StateFlow] there will always be a value available which, is why an initial
     * value must be provided. The value of this flow is changed by making a call to [set], passing
     * in the key that references this flow.
     *
     * If there is already a value associated with the given key, the initial value will be ignored.
     *
     * Note: If [T] is an [Array] classes, note that you should always use
     * `Array<Parcelable>` and create a typed array from the result as going through process
     * death and recreation (or using the `Don't keep activities` developer option) will result
     * in the type information being lost, thus resulting in a `ClassCastException` if you
     * directly try to collect the result as an `Array<CustomParcelable>`.
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
     * @param initialValue If no value exists with the given `key`, a new one is created
     * with the given `initialValue`.
     */
    @MainThread
    fun <T> getStateFlow(key: String, initialValue: T): StateFlow<T>

    /**
     * Returns all keys contained in this [SavedStateHandle]
     *
     * Returned set contains all keys: keys used to get LiveData-s, to set SavedStateProviders and
     * keys used in regular [set].
     */
    @MainThread
    fun keys(): Set<String>

    /**
     * Returns a value associated with the given key.
     *
     * Note: If [T] is an [Array] classes, note that you should always use
     * `Array<Parcelable>` and create a typed array from the result as going through process
     * death and recreation (or using the `Don't keep activities` developer option) will result
     * in the type information being lost, thus resulting in a `ClassCastException` if you
     * directly try to assign the result to an `Array<CustomParcelable>` value.
     *
     * ```
     * val typedArray = savedStateHandle.get<Array<Parcelable>>("KEY").map {
     *   it as CustomParcelable
     * }.toTypedArray()
     * ```
     *
     * @param key a key used to retrieve a value.
     */
    @MainThread
    operator fun <T> get(key: String): T?

    /**
     * Associate the given value with the key. The value must have a type that could be stored in
     * [Bundle]
     *
     * This also sets values for any active [Flow]s.
     *
     * @param key a key used to associate with the given value.
     * @param value object of any type that can be accepted by Bundle.
     *
     * @throws IllegalArgumentException value cannot be saved in saved state
     */
    @MainThread
    operator fun <T> set(key: String, value: T?)

    /**
     * Removes a value associated with the given key. If there is a [StateFlow]
     * associated with the given key, they will be removed as well.
     *
     * All changes to [androidx.lifecycle.LiveData]s or [StateFlow]s previously
     * returned by [getStateFlow] won't be reflected in
     * the saved state. Also that `LiveData` or `StateFlow` won't receive any updates about new
     * values associated by the given key.
     *
     * @param key a key
     * @return a value that was previously associated with the given key.
     */
    @MainThread
    fun <T> remove(key: String): T?

    /**
     * Set a [SavedStateProvider] that will have its state saved into this SavedStateHandle.
     * This provides a mechanism to lazily provide the [Bundle] of saved state for the given key.
     *
     * Calls to [get] with this same key will return the previously saved state as a [Bundle] if it
     * exists.
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
     * Note: calling this method within [SavedStateProvider.saveState] is supported, but
     * will only affect future state saving operations.
     *
     * @param key a key which will populated with a [Bundle] produced by the provider
     * @param provider a SavedStateProvider which will receive a callback to
     * [SavedStateProvider.saveState] when the state should be saved
     */
    @MainThread
    fun setSavedStateProvider(key: String, provider: SavedStateProvider)

    /**
     * Clear any [SavedStateProvider] that was previously set via
     * [setSavedStateProvider].
     *
     * Note: calling this method within [SavedStateProvider.saveState] is supported, but
     * will only affect future state saving operations.
     *
     * @param key a key previously used with [setSavedStateProvider]
     */
    @MainThread
    fun clearSavedStateProvider(key: String)

    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        fun createHandle(restoredState: Bundle?, defaultState: Bundle?): SavedStateHandle

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun validateValue(value: Any?): Boolean
    }
}
