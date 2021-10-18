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

package androidx.glance.action

import java.util.Collections

/**
 * Action parameters, used to pass information to an [Action].
 *
 * Construct action parameters using [actionParametersOf] or [mutableActionParametersOf], with typed
 * key-value pairs. The [Key] class enforces typing of the values inserted.
 */
public abstract class ActionParameters internal constructor() {

    /**
     * Key for [ActionParameters] values. Type T is the type of the associated value. The [Key.name]
     * must be unique, keys with the same name are considered equal.
     */
    public class Key<T : Any> (public val name: String) {
        /**
         * Infix function to create a Parameters.Pair.
         *
         * @param value the value this key should point to
         */
        public infix fun to(value: T): Pair<T> = Pair(this, value)

        override fun equals(other: Any?): Boolean = other is Key<*> && name == other.name

        override fun hashCode(): Int = name.hashCode()

        override fun toString(): String = name
    }

    /**
     * Key Value pairs for Parameters. Type T is the type of the value. Pairs must have unique keys,
     * or they will be considered equal.
     *
     * Create this using the infix function [to].
     */
    public class Pair<T : Any> internal constructor(
        internal val key: Key<T>,
        internal val value: T
    ) {
        override fun equals(other: Any?): Boolean =
            other is Pair<*> && key == other.key && value == other.value

        override fun hashCode(): Int = key.hashCode() + value.hashCode()

        override fun toString(): String = "(${key.name}, $value)"
    }

    /**
     * Returns true if the Parameters set contains the given Key.
     *
     * @param key the key to check for
     */
    public abstract operator fun <T : Any> contains(key: Key<T>): Boolean

    /**
     * Get a parameter with a key. If the key is not set, returns null.
     *
     * @param T the type of the parameter
     * @param key the key for the parameter
     * @throws ClassCastException if there is something stored with the same name as [key] but it
     * cannot be cast to T
     */
    public abstract operator fun <T : Any> get(key: Key<T>): T?

    /**
     * Retrieves a map of all key value pairs. The map is unmodifiable, and attempts to mutate it
     * will throw runtime exceptions.
     *
     * @return a map of all parameters in this Parameters
     */
    public abstract fun asMap(): Map<Key<out Any>, Any>
}

/**
 * Mutable version of [ActionParameters]. Allows for editing the underlying data, and adding or
 * removing key-value pairs.
 */
public class MutableActionParameters internal constructor(
    internal val map: MutableMap<Key<out Any>, Any> = mutableMapOf()
) : ActionParameters() {

    override operator fun <T : Any> contains(key: Key<T>): Boolean = map.containsKey(key)

    @Suppress("UNCHECKED_CAST")
    override operator fun <T : Any> get(key: Key<T>): T? = map[key] as T?

    override fun asMap(): Map<Key<out Any>, Any> = Collections.unmodifiableMap(map)

    /**
     * Sets a key value pair in MutableParameters. If the value is null, the key is removed from the
     * parameters.
     *
     * @param key the parameter to set
     * @param value the value to assign to this parameter
     * @return the previous value associated with the key, or null if the key was not present
     */
    public operator fun <T : Any> set(key: Key<T>, value: T?): T? {
        val mapValue = get(key)
        when (value) {
            null -> remove(key)
            else -> map[key] = value
        }
        return mapValue
    }

    /**
     * Removes an item from this MutableParameters.
     *
     * @param key the parameter to remove
     * @return the original value of the parameter
     */
    @Suppress("UNCHECKED_CAST")
    public fun <T : Any> remove(key: Key<T>) = map.remove(key) as T?

    /**
     * Removes all parameters from this MutableParameters.
     */
    public fun clear() = map.clear()

    override fun equals(other: Any?): Boolean = other is MutableActionParameters && map == other.map

    override fun hashCode(): Int = map.hashCode()

    override fun toString(): String = map.toString()
}

/**
 * Returns a new read-only Parameters, from the specified contents. The key element in the given
 * pairs will point to the corresponding value element.
 *
 * If multiple pairs have the same key, the resulting map will contain only the value from the last
 * of those pairs.
 */
public fun actionParametersOf(vararg pairs: ActionParameters.Pair<out Any>): ActionParameters =
    mutableActionParametersOf(*pairs)

/**
 * Returns a new MutableParameters, from the specified contents. The key element in the given pairs
 * will point to the corresponding value element.
 *
 * If multiple pairs have the same key, the resulting Parameters will contain only the value from
 * the last of those pairs.
 */
public fun mutableActionParametersOf(
    vararg pairs: ActionParameters.Pair<out Any>
): MutableActionParameters = MutableActionParameters(
    mutableMapOf(*pairs.map { it.key to it.value }.toTypedArray())
)

/**
 * Gets a mutable copy of Parameters, containing all key value pairs. This can be used to edit
 * the parameter data without creating a new object.
 *
 * This is similar to [Map.toMutableMap].
 *
 * @return a MutableParameters with all the same parameters as this Parameters
 */
public fun ActionParameters.toMutableParameters(): MutableActionParameters =
    MutableActionParameters(asMap().toMutableMap())

/**
 * Gets a read-only copy of Parameters, containing all key value pairs.
 *
 * This is similar to [Map.toMap].
 *
 * @return a copy of this Parameters
 */
public fun ActionParameters.toParameters(): ActionParameters = toMutableParameters()
