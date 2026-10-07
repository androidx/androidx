/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.collection.MutableScatterMap
import androidx.collection.mutableScatterMapOf
import androidx.savedstate.internal.SynchronizedObject
import androidx.savedstate.internal.synchronized

/**
 * A container that manages [SavedStateValue] instances to coordinate state saving and restoration
 * across hierarchical and key-value based components.
 *
 * [SavedStateContainer] allows registering, retrieving, removing, and nesting state-saving
 * components. When saving or restoring state, operations are delegated across all registered
 * [SavedStateValue] instances.
 */
public class SavedStateContainer {
    private val lock = SynchronizedObject()
    private val values: MutableScatterMap<String, SavedStateValue<*>> = mutableScatterMapOf()
    private val unconsumed: MutableScatterMap<String, SavedState> = mutableScatterMapOf()

    /**
     * Checks if a [SavedStateValue] is registered under the given [key].
     *
     * @param key unique identifier for the saved state value
     * @return `true` if a [SavedStateValue] is associated with [key], `false` otherwise
     */
    public operator fun contains(key: String): Boolean = synchronized(lock) { key in values }

    /**
     * Returns a [Set] containing all keys registered in this [SavedStateContainer].
     *
     * @return set of registered keys
     */
    public fun keys(): Set<String> = synchronized(lock) { values.asMap().keys.toSet() }

    /**
     * Retrieves an existing nested [SavedStateContainer] associated with [key], or creates and
     * registers a new child [SavedStateContainer] if none exists.
     *
     * @param key unique identifier for the child container
     * @return existing or newly created child [SavedStateContainer] instance
     */
    public fun getOrCreateContainer(key: String): SavedStateContainer =
        synchronized(lock) {
            val containerValue = values.getOrPut(key) { SavedStateContainerValue() }
            (containerValue as SavedStateContainerValue).value
        }

    /**
     * Registers a [savedStateValue] for state saving and restoration under the given [key].
     *
     * If a [SavedStateValue] was previously registered under [key], it will be overwritten.
     *
     * @param key unique identifier for the saved state value
     * @param savedStateValue component responsible for state saving and restoration
     */
    public fun <T, R : SavedStateValue<T>> putSavedStateValue(key: String, savedStateValue: R) {
        synchronized(lock) {
            values[key] = savedStateValue
            if (key in unconsumed) {
                savedStateValue.restoreState(unconsumed.remove(key))
            }
        }
    }

    internal fun <T, R : SavedStateValue<T>> registerSavedStateValue(
        key: String,
        savedStateValue: R,
    ): Boolean =
        synchronized(lock) {
            val existing = values[key]
            if (existing != null) {
                // Allow idempotent re-registration of the exact same instance, and
                // prevent key collisions between different instances.
                require(existing.value === savedStateValue.value) {
                    "SavedStateProvider with key '$key' already registered. " +
                        "Existing instance: '${existing.value}'. " +
                        "New instance: '${savedStateValue.value}'."
                }
                return@synchronized false
            }
            putSavedStateValue(key, savedStateValue)
            true
        }

    /**
     * Retrieves the registered [SavedStateValue] associated with [key].
     *
     * @param key unique identifier for the saved state value
     * @return registered [SavedStateValue] typed as [R], or `null` if no value is found
     */
    public fun <T, R : SavedStateValue<T>> getSavedStateValue(key: String): R? =
        synchronized(lock) {
            @Suppress("UNCHECKED_CAST")
            values[key] as? R
        }

    /**
     * Returns an existing [SavedStateValue] registered for [key], or evaluates [defaultValue],
     * registers the result in this container, and returns it.
     *
     * @param key unique identifier for the saved state value
     * @param defaultValue factory producing a new [SavedStateValue] if none is registered for [key]
     * @return existing or newly registered [SavedStateValue] instance
     */
    public fun <T, R : SavedStateValue<T>> getOrPutSavedStateValue(
        key: String,
        defaultValue: () -> R,
    ): R =
        synchronized(lock) {
            val existing = values[key]
            if (existing != null) {
                @Suppress("UNCHECKED_CAST")
                return@synchronized existing as R
            }
            val default = defaultValue()
            putSavedStateValue(key, default)
            default
        }

    /**
     * Removes and returns the registered [SavedStateValue] associated with [key].
     *
     * @param key unique identifier for the saved state value to remove
     * @return previously registered [SavedStateValue], or `null` if none was registered
     */
    public fun <T, R : SavedStateValue<T>> removeSavedStateValue(key: String): R? =
        synchronized(lock) {
            if (key in unconsumed) {
                unconsumed.remove(key)
            }
            @Suppress("UNCHECKED_CAST")
            values.remove(key) as? R
        }

    internal fun consumeForKey(key: String): SavedState? =
        synchronized(lock) { unconsumed.remove(key) }

    /**
     * Restores state for all registered [SavedStateValue] instances using the provided
     * [savedState].
     *
     * @param savedState previously saved state container, or `null` if no state was saved
     */
    public fun restoreState(savedState: SavedState?) {
        synchronized(lock) {
            savedState?.read {
                // we store all of values from the savedState in unconsumed so we only need to go
                // through them once.
                for (key in toMap().keys) {
                    val state = getSavedStateOrNull(key)
                    if (state != null) {
                        unconsumed[key] = state
                    }
                }
            }
            // we go through values separately to ensure that even if we have values
            // that are not in the savedState, that we restore null.
            values.forEach { key, savedStateValue ->
                savedStateValue.restoreState(unconsumed.remove(key))
            }
        }
    }

    /**
     * Invokes state saving on all registered [SavedStateValue] instances and bundles their states
     * into a single [SavedState].
     *
     * @return a [SavedState] containing the collected saved states of all registered providers
     */
    public fun saveState(): SavedState =
        synchronized(lock) {
            savedState(initialState = unconsumed.asMap()) {
                values.forEach { key, value ->
                    putSavedState(key, value.saveState())
                }
            }
        }
}
