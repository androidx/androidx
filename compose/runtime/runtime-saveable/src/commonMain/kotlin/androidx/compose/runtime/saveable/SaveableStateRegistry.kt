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

package androidx.compose.runtime.saveable

import androidx.collection.MutableScatterMap
import androidx.collection.mutableScatterMapOf
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.saveable.SaveableStateRegistry.Entry
import androidx.compose.runtime.staticCompositionLocalOf

/** Allows components to save and restore their state using the saved instance state mechanism. */
public interface SaveableStateRegistry {
    /**
     * Returns the restored value for the given key. Once being restored the value is cleared, so
     * you can't restore the same key twice.
     *
     * @param key Key used to save the value
     */
    public fun consumeRestored(key: String): Any?

    /**
     * Registers the value provider.
     *
     * There are could be multiple providers registered for the same [key]. In this case the order
     * in which they were registered matters.
     *
     * Say we registered two providers for the key. One provides "1", second provides "2".
     * [performSave] in this case will have listOf("1", "2) as a value for the key in the map. And
     * later, when the registry will be recreated with the previously saved values, the first
     * execution of [consumeRestored] would consume "1" and the second one "2".
     *
     * @param key Key to use for storing the value
     * @param valueProvider Provides the current value, to be executed when [performSave] will be
     *   triggered to collect all the registered values
     * @return the registry entry which you can use to unregister the provider
     */
    public fun registerProvider(key: String, valueProvider: () -> Any?): Entry

    /**
     * Returns true if the value can be saved using this Registry. The default implementation will
     * return true if this value can be stored in Bundle.
     *
     * @param value The value which we want to save using this Registry
     */
    public fun canBeSaved(value: Any): Boolean

    /**
     * Executes all the registered value providers and combines these values into a map. We have a
     * list of values for each key as it is allowed to have multiple providers for the same key.
     */
    public fun performSave(): Map<String, List<Any?>>

    /** The registry entry which you get when you use [registerProvider]. */
    public interface Entry {
        /** Unregister previously registered entry. */
        public fun unregister()
    }
}

/**
 * Creates [SaveableStateRegistry].
 *
 * @param restoredValues The map of the restored values
 * @param canBeSaved Function which returns true if the given value can be saved by the registry
 */
public fun SaveableStateRegistry(
    restoredValues: Map<String, List<Any?>>?,
    canBeSaved: (Any) -> Boolean,
): SaveableStateRegistry = SaveableStateRegistryImpl(restoredValues, canBeSaved)

/** CompositionLocal with a current [SaveableStateRegistry] instance. */
public val LocalSaveableStateRegistry: ProvidableCompositionLocal<SaveableStateRegistry?> =
    staticCompositionLocalOf<SaveableStateRegistry?> { null }

// CharSequence.isBlank() allocates an iterator because it calls indices.all{}
private fun CharSequence.fastIsBlank(): Boolean {
    var blank = true
    for (i in 0 until length) {
        if (!this[i].isWhitespace()) {
            blank = false
            break
        }
    }
    return blank
}

private fun <K, V> Map<K, V>.toMutableScatterMap(): MutableScatterMap<K, V> {
    return MutableScatterMap<K, V>(size).also { it += this }
}

private class SaveableStateRegistryImpl(
    restored: Map<String, List<Any?>>?,
    private val canBeSaved: (Any) -> Boolean,
) : SaveableStateRegistry {

    private val restored: MutableScatterMap<String, List<Any?>>? =
        if (!restored.isNullOrEmpty()) restored.toMutableScatterMap() else null

    private var valueProviders: MutableScatterMap<String, MutableList<() -> Any?>>? = null

    override fun canBeSaved(value: Any): Boolean = canBeSaved.invoke(value)

    override fun consumeRestored(key: String): Any? {
        val list = restored?.remove(key)
        return if (!list.isNullOrEmpty()) {
            if (list.size > 1) {
                restored?.put(key, list.subList(1, list.size))
            }
            list[0]
        } else {
            null
        }
    }

    override fun registerProvider(key: String, valueProvider: () -> Any?): Entry {
        require(!key.fastIsBlank()) { "Registered key is empty or blank" }
        val valueProviders =
            valueProviders
                ?: mutableScatterMapOf<String, MutableList<() -> Any?>>().also {
                    valueProviders = it
                }
        valueProviders.getOrPut(key) { mutableListOf() }.add(valueProvider)
        return object : Entry {
            override fun unregister() {
                val list = valueProviders.remove(key)
                list?.remove(valueProvider)
                if (!list.isNullOrEmpty()) {
                    // if there are other providers for this key return list back to the map
                    valueProviders[key] = list
                }
            }
        }
    }

    override fun performSave(): Map<String, List<Any?>> {
        // Return early if no restored state and no providers exist.
        if (restored == null && valueProviders == null) {
            return emptyMap()
        }

        // Set initial capacity to prevent map resizing.
        val size = (restored?.size ?: 0) + (valueProviders?.size ?: 0)
        val map = MutableScatterMap<String, List<Any?>>(initialCapacity = size)

        // Keep restored state for composables not composed in this cycle.
        restored?.let { map.putAll(restored) }

        // Evaluate all registered providers to collect current state.
        valueProviders?.forEach { key, list ->
            // Optimize single-provider case (most common). Prevents list creation
            // if the single provider returns null.
            if (list.size == 1) {
                val value = list[0].invoke()
                if (value != null) {
                    check(canBeSaved(value)) { generateCannotBeSavedErrorMessage(value) }
                    // On JVM, listOf(value) calls Collections.singletonList to
                    // prevent backing array allocation.
                    map[key] = listOf(value)
                }
            } else {
                // Keep nulls for multiple providers to preserve order.
                // Example: First returns null, second returns "1". On restore,
                // first restores null and second restores "1".
                map[key] =
                    List(list.size) { index ->
                        val value = list[index].invoke()
                        if (value != null) {
                            check(canBeSaved(value)) { generateCannotBeSavedErrorMessage(value) }
                        }
                        value
                    }
            }
        }

        return ScatterMapWrapper(map)
    }
}

/**
 * Wraps [MutableScatterMap] as standard [Map].
 *
 * Matches `ScatterMap` from [SaveableStateRegistry.performSave] to represent underlying
 * [MutableScatterMap] state.
 *
 * **Note**: Implements [JvmSerializable] to satisfy `canBeSavedToBundle` checks. Android OS
 * `Parcel.writeValue` matches [Map] interface before `Serializable` or `Parcelable`. It serializes
 * it as standard JVM `HashMap` (via optimized `writeMapInternal`), bypassing custom `Parcelable`
 * implementations.
 */
@Suppress("AsCollectionCall")
internal class ScatterMapWrapper(
    private val base: MutableScatterMap<String, List<Any?>> = MutableScatterMap()
) : Map<String, List<Any?>> by base.asMap(), JvmSerializable

/**
 * Platform-agnostic representation of JVM `java.io.Serializable`.
 *
 * Resolves to `java.io.Serializable` on JVM and Android. Resolves to empty interface on other
 * platforms.
 *
 * **Rationale**: [SaveableStateRegistry] checks type safety using `canBeSavedToBundle` before
 * saving. Custom wrappers must implement `Serializable` or `Parcelable` to pass this check.
 *
 * Implementing `Serializable` or `Parcelable` on classes implementing `Map` is ignored during
 * parceling. Android OS `Parcel.writeValue` matches `Map` interface before `Serializable` or
 * `Parcelable`. Since Bundle marshalling uses `writeValue` for all entries, these classes are
 * always serialized as standard JVM `HashMap` (via optimized `writeMapInternal`), bypassing custom
 * `Parcelable` implementations.
 *
 * Implementing `JvmSerializable` on [ScatterMapWrapper] satisfies `canBeSaved` check while
 * acknowledging that custom parceling is bypassed.
 */
internal expect interface JvmSerializable
