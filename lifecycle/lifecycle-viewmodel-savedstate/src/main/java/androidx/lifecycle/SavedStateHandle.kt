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

import android.os.Binder
import android.os.Bundle
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import androidx.savedstate.SavedStateRegistry.SavedStateProvider
import java.io.Serializable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
class SavedStateHandle {
    private val regular = mutableMapOf<String, Any?>()
    private val savedStateProviders = mutableMapOf<String, SavedStateProvider>()
    private val liveDatas = mutableMapOf<String, SavingStateLiveData<*>>()
    private val flows = mutableMapOf<String, MutableStateFlow<Any?>>()
    private val savedStateProvider = SavedStateProvider {
        // Get the saved state from each SavedStateProvider registered with this
        // SavedStateHandle, iterating through a copy to avoid re-entrance
        for ((key, provider) in savedStateProviders.toMap()) {
            set(key, provider.saveState())
        }

        // Convert the Map of current values into a Bundle
        bundleOf(
            KEYS to ArrayList(regular.keys),
            VALUES to ArrayList(regular.values),
        )
    }

    /**
     * Creates a handle with the given initial arguments.
     *
     * @param initialState initial arguments for the SavedStateHandle
     */
    constructor(initialState: Map<String, Any?>) {
        regular += initialState
    }

    /** Creates a handle with the empty state. */
    constructor()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun savedStateProvider(): SavedStateProvider = savedStateProvider

    /**
     * @param key The identifier for the value
     * @return true if there is value associated with the given key.
     */
    @MainThread operator fun contains(key: String): Boolean = regular.containsKey(key)

    /**
     * Returns a [androidx.lifecycle.LiveData] that access data associated with the given key.
     *
     * @param key The identifier for the value
     * @see getLiveData
     */
    @MainThread
    fun <T> getLiveData(key: String): MutableLiveData<T> {
        @Suppress("UNCHECKED_CAST")
        return getLiveDataInternal(key, false, null) as MutableLiveData<T>
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
    fun <T> getLiveData(key: String, initialValue: T): MutableLiveData<T> {
        return getLiveDataInternal(key, true, initialValue)
    }

    private fun <T> getLiveDataInternal(
        key: String,
        hasInitialValue: Boolean,
        initialValue: T
    ): MutableLiveData<T> {
        @Suppress("UNCHECKED_CAST") val previousLiveData = liveDatas[key] as? MutableLiveData<T>
        if (previousLiveData != null) {
            return previousLiveData
        }

        // double hashing but null is valid value
        val newLiveData: SavingStateLiveData<T> =
            when {
                regular.containsKey(key) -> {
                    @Suppress("UNCHECKED_CAST") SavingStateLiveData(this, key, regular[key] as T)
                }
                hasInitialValue -> {
                    regular[key] = initialValue
                    SavingStateLiveData(this, key, initialValue)
                }
                else -> {
                    SavingStateLiveData(this, key)
                }
            }
        liveDatas[key] = newLiveData
        return newLiveData
    }

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
    @MainThread
    fun <T> getStateFlow(key: String, initialValue: T): StateFlow<T> {
        @Suppress("UNCHECKED_CAST")
        // If a flow exists we should just return it, and since it is a StateFlow and a value must
        // always be set, we know a value must already be available
        return flows
            .getOrPut(key) {
                // If there is not a value associated with the key, add the initial value,
                // otherwise,
                // use the one we already have.
                if (!regular.containsKey(key)) {
                    regular[key] = initialValue
                }
                MutableStateFlow(regular[key]).apply { flows[key] = this }
            }
            .asStateFlow() as StateFlow<T>
    }

    /**
     * Returns all keys contained in this [SavedStateHandle]
     *
     * Returned set contains all keys: keys used to get LiveData-s, to set SavedStateProviders and
     * keys used in regular [set].
     */
    @MainThread fun keys(): Set<String> = regular.keys + savedStateProviders.keys + liveDatas.keys

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
    @MainThread
    operator fun <T> get(key: String): T? {
        return try {
            @Suppress("UNCHECKED_CAST")
            regular[key] as T?
        } catch (e: ClassCastException) {
            // Instead of failing on ClassCastException, we remove the value from the
            // SavedStateHandle and return null.
            remove<T>(key)
            null
        }
    }

    /**
     * Associate the given value with the key. The value must have a type that could be stored in
     * [android.os.Bundle]
     *
     * This also sets values for any active [LiveData]s or [StateFlow]s.
     *
     * @param key a key used to associate with the given value.
     * @param value object of any type that can be accepted by Bundle.
     * @throws IllegalArgumentException value cannot be saved in saved state
     */
    @MainThread
    operator fun <T> set(key: String, value: T?) {
        require(validateValue(value)) {
            "Can't put value with type ${value!!::class.java} into saved state"
        }
        @Suppress("UNCHECKED_CAST") val mutableLiveData = liveDatas[key] as? MutableLiveData<T?>?
        if (mutableLiveData != null) {
            // it will set value;
            mutableLiveData.setValue(value)
        } else {
            regular[key] = value
        }
        flows[key]?.value = value
    }

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
    @MainThread
    fun <T> remove(key: String): T? {
        @Suppress("UNCHECKED_CAST") val latestValue = regular.remove(key) as T?
        val liveData = liveDatas.remove(key)
        liveData?.detach()
        flows.remove(key)
        return latestValue
    }

    /**
     * Set a [SavedStateProvider] that will have its state saved into this SavedStateHandle. This
     * provides a mechanism to lazily provide the [Bundle] of saved state for the given key.
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
     * Note: calling this method within [SavedStateProvider.saveState] is supported, but will only
     * affect future state saving operations.
     *
     * @param key a key which will populated with a [Bundle] produced by the provider
     * @param provider a SavedStateProvider which will receive a callback to
     *   [SavedStateProvider.saveState] when the state should be saved
     */
    @MainThread
    fun setSavedStateProvider(key: String, provider: SavedStateProvider) {
        savedStateProviders[key] = provider
    }

    /**
     * Clear any [SavedStateProvider] that was previously set via [setSavedStateProvider].
     *
     * Note: calling this method within [SavedStateProvider.saveState] is supported, but will only
     * affect future state saving operations.
     *
     * @param key a key previously used with [setSavedStateProvider]
     */
    @MainThread
    fun clearSavedStateProvider(key: String) {
        savedStateProviders.remove(key)
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
            handle?.let {
                it.regular[key] = value
                it.flows[key]?.value = value
            }
            super.setValue(value)
        }

        fun detach() {
            handle = null
        }
    }

    companion object {
        private const val VALUES = "values"
        private const val KEYS = "keys"

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        @Suppress("DEPRECATION")
        fun createHandle(restoredState: Bundle?, defaultState: Bundle?): SavedStateHandle {
            if (restoredState == null) {
                return if (defaultState == null) {
                    // No restored state and no default state -> empty SavedStateHandle
                    SavedStateHandle()
                } else {
                    val state: MutableMap<String, Any?> = HashMap()
                    for (key in defaultState.keySet()) {
                        state[key] = defaultState[key]
                    }
                    SavedStateHandle(state)
                }
            }

            // When restoring state, we use the restored state as the source of truth
            // and ignore any default state, thus ensuring we are exactly the same
            // state that was saved.
            restoredState.classLoader = SavedStateHandle::class.java.classLoader!!
            val keys: ArrayList<*>? = restoredState.getParcelableArrayList<Parcelable>(KEYS)
            val values: ArrayList<*>? = restoredState.getParcelableArrayList<Parcelable>(VALUES)
            check(!(keys == null || values == null || keys.size != values.size)) {
                "Invalid bundle passed as restored state"
            }
            val state = mutableMapOf<String, Any?>()
            for (i in keys.indices) {
                state[keys[i] as String] = values[i]
            }
            return SavedStateHandle(state)
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun validateValue(value: Any?): Boolean =
            value == null || ACCEPTABLE_CLASSES.any { classRef -> classRef.isInstance(value) }

        // doesn't have Integer, Long etc box types because they are "Serializable"
        private val ACCEPTABLE_CLASSES =
            arrayOf( // baseBundle
                    Boolean::class.javaPrimitiveType,
                    BooleanArray::class.java,
                    Double::class.javaPrimitiveType,
                    DoubleArray::class.java,
                    Int::class.javaPrimitiveType,
                    IntArray::class.java,
                    Long::class.javaPrimitiveType,
                    LongArray::class.java,
                    String::class.java,
                    Array<String>::class.java, // bundle
                    Binder::class.java,
                    Bundle::class.java,
                    Byte::class.javaPrimitiveType,
                    ByteArray::class.java,
                    Char::class.javaPrimitiveType,
                    CharArray::class.java,
                    CharSequence::class.java,
                    Array<CharSequence>::class.java,
                    // type erasure ¯\_(ツ)_/¯, we won't eagerly check elements contents
                    ArrayList::class.java,
                    Float::class.javaPrimitiveType,
                    FloatArray::class.java,
                    Parcelable::class.java,
                    Array<Parcelable>::class.java,
                    Serializable::class.java,
                    Short::class.javaPrimitiveType,
                    ShortArray::class.java,
                    SparseArray::class.java,
                    Size::class.java,
                    SizeF::class.java,
                )
                .filterNotNull()
    }
}
