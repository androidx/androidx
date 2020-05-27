/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.datastore.preferences

/**
 * Preferences provides a schemaless key-value format for use with DataStore. It closely
 * resembles the SharedPreferences interface, with a few differences to allow compatibility with
 * DataStore. The differences include:
 * 1. There is no edit() method. Instead use the toBuilder() to create a new PreferencesBuilder
 * and pass the result to DataStore.updateData().
 * 2. There is no (un)registerOnSharedPreferenceChangeListener() methods. Instead, use DataStore
 * .data.
 */
class Preferences internal constructor(
    private val preferences: Map<String, Any> = mapOf()
) {

    /* Checks whether the Preferences contains a preference. */
    operator fun contains(key: String): Boolean {
        return preferences.containsKey(key)
    }

    /**
     * Retrieve a boolean value from Preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defaultValue Value to return if this preference does not exist.
     *
     * @return Returns the preference if it exists, otherwise returns defaultValue
     *
     * @throws ClassCastException if there is a preference for this key that is not a boolean.
     */
    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return getKeyOrDefault(key, defaultValue)
    }

    /**
     * Retrieve a float value from Preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defaultValue Value to return if this preference does not exist.
     *
     * @return Returns the preference if it exists, otherwise returns defaultValue
     *
     * @throws ClassCastException if there is a preference for this key that is not a float.
     */
    fun getFloat(key: String, defaultValue: Float): Float {
        return getKeyOrDefault(key, defaultValue)
    }

    /**
     * Retrieve a int value from Preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defaultValue Value to return if this preference does not exist.
     *
     * @return Returns the preference if it exists, otherwise returns defaultValue
     *
     * @throws ClassCastException if there is a preference for this key that is not a int.
     */
    fun getInt(key: String, defaultValue: Int): Int {
        return getKeyOrDefault(key, defaultValue)
    }

    /**
     * Retrieve a long value from Preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defaultValue Value to return if this preference does not exist.
     *
     * @return Returns the preference if it exists, otherwise returns defaultValue
     *
     * @throws ClassCastException if there is a preference for this key that is not a long.
     */
    fun getLong(key: String, defaultValue: Long): Long {
        return getKeyOrDefault(key, defaultValue)
    }

    /**
     * Retrieve a String value from Preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defaultValue Value to return if this preference does not exist.
     *
     * @return Returns the preference if it exists, otherwise returns defaultValue
     *
     * @throws ClassCastException if there is a preference for this key that is not a String.
     */
    fun getString(key: String, defaultValue: String): String {
        return getKeyOrDefault(key, defaultValue)
    }

    /**
     * Retrieve a set of Strings from Preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defaultValue Value to return if this preference does not exist.
     *
     * @return Returns the preference if it exists, otherwise returns defaultValue
     *
     * @throws ClassCastException if there is a preference for this key that is not a Set of
     * Strings.
     */
    fun getStringSet(key: String, defaultValue: Set<String>): Set<String> {
        return getKeyOrDefault(key, defaultValue).toSet()
    }

    /**
     * Retrieve a map of all values from the preferences.
     *
     * @return Returns a map containing representing all the preferences in Preferences.
     */
    fun getAll(): Map<String, Any> {
        return preferences.mapValues {
            val value = it.value
            if (value is Set<*>) {
                value.toSet()
            } else {
                value
            }
        }
    }

    // TODO(b/151635324): add getByteArray()... ByteArray, Byte[], List<Byte>?

    override fun equals(other: Any?): Boolean {
        if (other is Preferences) {
            return this.preferences == other.preferences
        }
        return false
    }

    override fun hashCode(): Int {
        return preferences.hashCode()
    }

    /**
     * Gets a builder which contains all the preferences in this Preferences. This can be used
     * to change preferences without building a new Preferences object from scratch.
     *
     * @return Returns a PreferencesBuilder with all the preferences from this Preferences.
     */
    fun toBuilder(): Builder {
        return Builder(
            preferences.toMutableMap()
        )
    }

    companion object {
        /**
         * Get a new empty Preferences.
         *
         * @return Returns a new Preferences instance with no preferences set.
         */
        fun empty(): Preferences {
            return Preferences()
        }
    }

    /**
     * The builder used for constructing Preferences. PreferencesBuilder resembles
     * SharedPreferences.Editor, with some key differences:
     * 1. It follows the builder pattern, so it cannot modify the state of any existing Preferences.
     * 2. There is no apply or commit method. Instead, pass the result of build() to
     *  DataStore.updateData()
     */
    class Builder internal constructor(
        private val preferencesMap: MutableMap<String, Any> = mutableMapOf()
    ) {
        constructor() : this(mutableMapOf()) {}

        /**
         * Set a boolean value in the PreferencesBuilder.
         *
         * @param key The name of the preference to set.
         * @param newValue The new value of the preference.
         *
         * @return Returns this instance of PreferencesBuilder.
         */
        fun setBoolean(key: String, newValue: Boolean) = apply {
            preferencesMap[key] = newValue
        }

        /**
         * Set a float value in the PreferencesBuilder.
         *
         * @param key The name of the preference to set.
         * @param newValue The new value of the preference.
         *
         * @return Returns this instance of PreferencesBuilder.
         */
        fun setFloat(key: String, newValue: Float) = apply {
            preferencesMap[key] = newValue
        }

        /**
         * Set a int value in the PreferencesBuilder.
         *
         * @param key The name of the preference to set.
         * @param newValue The new value of the preference.
         *
         * @return Returns this instance of PreferencesBuilder.
         */
        fun setInt(key: String, newValue: Int) = apply {
            preferencesMap[key] = newValue
        }

        /**
         * Set a long value in the PreferencesBuilder.
         *
         * @param key The name of the preference to set.
         * @param newValue The new value of the preference.
         *
         * @return Returns this instance of PreferencesBuilder.
         */
        fun setLong(key: String, newValue: Long) = apply {
            preferencesMap[key] = newValue
        }

        /**
         * Set a String value in the PreferencesBuilder.
         *
         * @param key The name of the preference to set.
         * @param newValue The new value of the preference.
         *
         * @return Returns this instance of PreferencesBuilder.
         */
        fun setString(key: String, newValue: String) = apply {
            preferencesMap[key] = newValue
        }

        /**
         * Set a String Set in the PreferencesBuilder.
         *
         * @param key The name of the preference to set.
         * @param newValue The new value of the preference.
         *
         * @return Returns this instance of PreferencesBuilder.
         */
        fun setStringSet(key: String, newValue: Set<String>) = apply {
            preferencesMap[key] = newValue.toSet()
        }

        /* Remove a preferences from the PreferencesBuilder. */
        fun remove(key: String) = apply {
            preferencesMap.remove(key)
        }

        /* Removes all preferences from the PreferencesBuilder. */
        fun clear() = apply {
            preferencesMap.clear()
        }

        // TODO(b/151635324): setByteArray(...)

        fun build(): Preferences {
            return Preferences(preferencesMap.toMap())
        }
    }

    private inline fun <reified T> getKeyOrDefault(key: String, defaultValue: T): T {
        return preferences.getOrElse(key, { defaultValue }) as T
    }
}
