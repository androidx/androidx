/*
 * Copyright 2022 The Android Open Source Project
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

@file:JvmName("PreferencesFactory")

package androidx.datastore.preferences.core

import kotlin.jvm.JvmName

/**
 * Get a new empty Preferences.
 *
 * @return a new Preferences instance with no preferences set
 */
@JvmName("createEmpty")
public fun emptyPreferences(): Preferences = MutablePreferences(startFrozen = true)

/**
 * Construct a Preferences object with a list of Preferences.Pair<T>. Comparable to mapOf().
 *
 * Example usage:
 * ```
 * val counterKey = intPreferencesKey("counter")
 * val preferences = preferencesOf(counterKey to 100)
 * ```
 *
 * @param pairs the key value pairs with which to construct the preferences
 */
@JvmName("create")
public fun preferencesOf(vararg pairs: Preferences.Pair<*>): Preferences =
    mutablePreferencesOf(*pairs)

/**
 * Construct a MutablePreferences object with a list of Preferences.Pair<T>. Comparable to mapOf().
 *
 * Example usage:
 * ```
 * val counterKey = intPreferencesKey("counter")
 * val preferences = mutablePreferencesOf(counterKey to 100)
 * ```
 * @param pairs the key value pairs with which to construct the preferences
 */
@JvmName("createMutable")
public fun mutablePreferencesOf(vararg pairs: Preferences.Pair<*>): MutablePreferences =
    MutablePreferences(startFrozen = false).apply { putAll(*pairs) }
