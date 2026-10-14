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
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.saveable.SaveableStateRegistry.Entry
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.savedstate.SavedStateContainer

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

/**
 * Creates [SaveableStateRegistry] backed by the given [SavedStateContainer].
 *
 * @param container The [SavedStateContainer] to back this [SaveableStateRegistry]
 * @param canBeSaved Function which returns true if the given value can be saved by the registry
 */
public fun SaveableStateRegistry(
    container: SavedStateContainer,
    canBeSaved: (Any) -> Boolean,
): SaveableStateRegistry = SaveableStateRegistryImpl(container = container, canBeSaved = canBeSaved)

/** CompositionLocal with a current [SaveableStateRegistry] instance. */
public val LocalSaveableStateRegistry: ProvidableCompositionLocal<SaveableStateRegistry?> =
    staticCompositionLocalOf<SaveableStateRegistry?> { null }

private class SaveableStateRegistryImpl(
    private val container: SavedStateContainer,
    restored: Map<String, List<Any?>>? = null,
    private val canBeSaved: (Any) -> Boolean,
) : SaveableStateRegistry {

    constructor(
        restored: Map<String, List<Any?>>?,
        canBeSaved: (Any) -> Boolean,
    ) : this(SavedStateContainer(), restored, canBeSaved)

    private val saveableStateValue: SaveableStateValue =
        container.getOrPutSavedStateValue(SAVEABLE_STATE_VALUE_KEY) {
            SaveableStateValue(restored = restored, canBeSaved = canBeSaved)
        }

    override fun canBeSaved(value: Any): Boolean = canBeSaved.invoke(value)

    override fun consumeRestored(key: String): Any? = saveableStateValue.consumeRestored(key)

    override fun registerProvider(key: String, valueProvider: () -> Any?): Entry =
        saveableStateValue.registerProvider(key, valueProvider)

    override fun performSave(): Map<String, List<Any?>> = saveableStateValue.value
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
