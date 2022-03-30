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

@file:JvmName("PreferencesKeys")

package androidx.datastore.preferences.core

/**
 * Get a key for an Int preference. You should not have multiple keys with the same name (for use
 * with the same Preferences). Using overlapping keys with different types can result in
 * ClassCastException.
 *
 * @param name the name of the preference
 * @return the Preferences.Key<Int> for [name]
 */
@JvmName("intKey")
public fun intPreferencesKey(name: String): Preferences.Key<Int> = Preferences.Key(name)

/**
 * Get a key for a Double preference. You should not have multiple keys with the same name (for use
 * with the same Preferences). Using overlapping keys with different types can result in
 * ClassCastException.
 *
 * @param name the name of the preference
 * @return the Preferences.Key<Double> for [name]
 */
@JvmName("doubleKey")
public fun doublePreferencesKey(name: String): Preferences.Key<Double> = Preferences.Key(name)

/**
 * Get a key for a String preference. You should not have multiple keys with the same name (for use
 * with the same Preferences). Using overlapping keys with different types can result in
 * ClassCastException.
 *
 * @param name the name of the preference
 * @return the Preferences.Key<String> for [name]
 */
@JvmName("stringKey")
public fun stringPreferencesKey(name: String): Preferences.Key<String> = Preferences.Key(name)

/**
 * Get a key for a Boolean preference. You should not have multiple keys with the same name (for use
 * with the same Preferences). Using overlapping keys with different types can result in
 * ClassCastException.
 *
 * @param name the name of the preference
 * @return the Preferences.Key<Boolean> for [name]
 */
@JvmName("booleanKey")
public fun booleanPreferencesKey(name: String): Preferences.Key<Boolean> = Preferences.Key(name)

/**
 * Get a key for a Float preference. You should not have multiple keys with the same name (for use
 * with the same Preferences). Using overlapping keys with different types can result in
 * ClassCastException.
 *
 * @param name the name of the preference
 * @return the Preferences.Key<Float> for [name]
 */
@JvmName("floatKey")
public fun floatPreferencesKey(name: String): Preferences.Key<Float> = Preferences.Key(name)

/**
 * Get a key for an Long preference. You should not have multiple keys with the same name (for use
 * with the same Preferences). Using overlapping keys with different types can result in
 * ClassCastException.
 *
 * @param name the name of the preference
 * @return the Preferences.Key<Long> for [name]
 */
@JvmName("longKey")
public fun longPreferencesKey(name: String): Preferences.Key<Long> = Preferences.Key(name)

/**
 * Get a key for a String Set preference. You should not have multiple keys with the same name (for
 * use with the same Preferences). Using overlapping keys with different types can result in
 * ClassCastException.
 *
 * Note: sets returned by DataStore are unmodifiable and will throw exceptions if mutated.
 *
 * @param name the name of the preference
 * @return the Preferences.Key<Set<String>> for [name]
 */
@JvmName("stringSetKey")
public fun stringSetPreferencesKey(name: String): Preferences.Key<Set<String>> =
    Preferences.Key(name)

/**
 * Get a key for an ByteArray preference. You should not have multiple keys with the same name (for
 * use with the same Preferences). Using overlapping keys with different types can result in
 * ClassCastException.
 *
 * Note: ByteArrays returned by DataStore are copies. Mutating their state will do nothing to the
 *       underlying data store.  They must be set explicitly.
 *
 * @param name the name of the preference
 * @return the Preferences.Key<ByteArray> for [name]
 */
@JvmName("byteArrayKey")
public fun byteArrayPreferencesKey(name: String): Preferences.Key<ByteArray> = Preferences.Key(name)
