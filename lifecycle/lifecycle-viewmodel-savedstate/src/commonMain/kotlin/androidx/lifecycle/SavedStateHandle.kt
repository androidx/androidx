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
 * A handle to saved state passed to [ViewModel]. Typically, [SavedStateViewModelFactory] provides
 * this object to the [ViewModel] constructor.
 *
 * A key-value map allowing retrieval and storage of values to and from the saved state. These
 * values persist through system-initiated process death and remain available to the recreated
 * instance.
 *
 * Values can be read via [get] or observed as a flow via [getStateFlow]. Values can be written via
 * [set] or by updating the returned flow via [getMutableStateFlow].
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
     * @param key identifier of the value
     * @return `true` if a value is associated with [key]
     */
    @MainThread public operator fun contains(key: String): Boolean

    /**
     * Returns a [StateFlow] that will emit the currently active value associated with the given
     * key.
     *
     * val flow = savedStateHandle.getStateFlow(KEY, "defaultValue")
     *
     * Since this is a [StateFlow] there will always be a value available which, is why an initial
     * value must be provided. The value of this flow is changed by making a call to [set], passing
     * in the key that references this flow.
     *
     * If there is already a value associated with the given key, the initial value will be ignored.
     *
     * Note: If [T] is an [Array] of `android.os.Parcelable` classes, note that you should always
     * use `Array<Parcelable>` and create a typed array from the result as going through process
     * death and recreation (or using the `Don't keep activities` developer option) will result in
     * the type information being lost, thus resulting in a `ClassCastException` if you directly try
     * to collect the result as an `Array<CustomParcelable>`.
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
     * @param key identifier of the flow
     * @param initialValue value to use if no value is associated with [key]
     */
    @MainThread public fun <T> getStateFlow(key: String, initialValue: T): StateFlow<T>

    /**
     * Returns a [MutableStateFlow] that will emit the currently active value associated with the
     * given key.
     *
     * val flow = savedStateHandle.getMutableStateFlow(KEY, "defaultValue")
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
     * val typedArrayFlow = savedStateHandle.getMutableStateFlow<Array<Parcelable>>( "KEY" ).map {
     * array -> // Convert the Array<Parcelable> to an Array<CustomParcelable> array.map { it as
     * CustomParcelable }.toTypedArray() }
     *
     * **Note 2:** On Android, this method is mutually exclusive with `getLiveData` for the same
     * key. You should use either `getMutableStateFlow` or `getLiveData` to access the stored value,
     * but not both. Using both methods with the same key will result in an `IllegalStateException`.
     *
     * @param key identifier of the flow
     * @param initialValue value to use if no value is associated with [key]
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
     * Note: If [T] is an [Array] of `android.os.Parcelable` classes, note that you should always
     * use `Array<Parcelable>` and create a typed array from the result as going through process
     * death and recreation (or using the `Don't keep activities` developer option) will result in
     * the type information being lost, thus resulting in a `ClassCastException` if you directly try
     * to assign the result to an `Array<CustomParcelable>` value.
     *
     * val typedArray = savedStateHandle.get<Array<Parcelable>>("KEY").map { it as CustomParcelable
     * }.toTypedArray()
     *
     * @param key identifier of the value
     */
    @MainThread public operator fun <T> get(key: String): T?

    /**
     * Associate the given value with the key. The value must have a type that could be stored in
     * [SavedState]
     *
     * This also sets values for any active `androidx.lifecycle.LiveData` or [StateFlow].
     *
     * @param key identifier of the value
     * @param value value to associate with [key]
     * @throws IllegalArgumentException value cannot be saved in saved state
     */
    @MainThread public operator fun <T> set(key: String, value: T?)

    /**
     * Removes a value associated with the given key. If there is a `androidx.lifecycle.LiveData`
     * and/or [StateFlow] associated with the given key, they will be removed as well.
     *
     * All changes to `LiveData` or [StateFlow] previously returned by
     * `SavedStateHandle.getLiveData` or [getStateFlow] won't be reflected in the saved state. Also,
     * that `LiveData` or [StateFlow] won't receive any updates about new values associated by the
     * given key.
     *
     * @param key identifier of the value
     * @return value previously associated with [key], or `null` if none was present
     */
    @MainThread public fun <T> remove(key: String): T?

    /**
     * Sets a [SavedStateProvider] that will have its state saved into this [SavedStateHandle]. This
     * provides a mechanism to lazily supply the [SavedState] for the given key.
     *
     * Calls to [get] with the same key will return the previously saved state as a [SavedState] if
     * it exists.
     *
     * val previousState: SavedState? = savedStateHandle.get("custom_object") if (previousState !=
     * null) { // Convert the previousState into your custom object }
     * savedStateHandle.setSavedStateProvider("custom_object") { savedState { // Put your custom
     * object properties into the SavedState } }
     *
     * Note: Calling this method within [SavedStateProvider.saveState] is supported, but will only
     * affect future state saving operations.
     *
     * @param key identifier of the state
     * @param provider [SavedStateProvider] which will receive a callback to
     *   [SavedStateProvider.saveState] when the state should be saved
     */
    @MainThread public fun setSavedStateProvider(key: String, provider: SavedStateProvider)

    /**
     * Clear any [SavedStateProvider] that was previously set via [setSavedStateProvider].
     *
     * Note: calling this method within [SavedStateProvider.saveState] is supported, but will only
     * affect future state saving operations.
     *
     * @param key identifier previously used with [setSavedStateProvider]
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
