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

package androidx.datastore.preferences.core

import androidx.kruth.assertThat
import kotlin.test.Test

class PreferencesTest {

    @Test
    fun booleanPreferenceCanBeRetrieved() {
        val booleanKey = booleanPreferencesKey("boolean_key")
        val preferences = preferencesOf(booleanKey to true)

        val booleanPreference = preferences[booleanKey]

        assertThat(booleanPreference).isTrue()
    }

    @Test
    fun unsetBooleanPreferenceReturnsNull() {
        val booleanKey = booleanPreferencesKey("boolean_key")
        val preferences = emptyPreferences()

        val booleanPreference = preferences[booleanKey]

        assertThat(booleanPreference).isNull()
    }

    @Test
    fun floatPreferenceCanBeRetrieved() {
        val floatKey = floatPreferencesKey("float_key")
        val preferences = preferencesOf(floatKey to 1.1f)

        val floatPreference = preferences[floatKey]

        assertThat(floatPreference).isEqualTo(1.1f)
    }

    @Test
    fun unsetFloatPreferenceReturnsNull() {
        val floatKey = floatPreferencesKey("float_key")
        val preferences = emptyPreferences()

        val floatPreference = preferences[floatKey]

        assertThat(floatPreference).isNull()
    }

    @Test
    fun doublePreferenceCanBeRetrieved() {
        val doubleKey = doublePreferencesKey("double_key")
        val preferences = preferencesOf(doubleKey to Double.MAX_VALUE)

        val doublePreference = preferences[doubleKey]

        assertThat(doublePreference).isEqualTo(Double.MAX_VALUE)
    }

    @Test
    fun unsetDoublePreferenceReturnsNull() {
        val doubleKey = floatPreferencesKey("double_key")
        val preferences = emptyPreferences()

        val doublePreference = preferences[doubleKey]

        assertThat(doublePreference).isNull()
    }

    @Test
    fun intPreferenceCanBeRetrieved() {
        val intKey = intPreferencesKey("int_key")
        val preferences = preferencesOf(intKey to 1)

        val intPreference = preferences[intKey]

        assertThat(intPreference).isEqualTo(1)
    }

    @Test
    fun unsetIntPreferenceReturnsNull() {
        val intKey = intPreferencesKey("int_key")
        val preferences = emptyPreferences()

        val intPreference = preferences[intKey]

        assertThat(intPreference).isNull()
    }

    @Test
    fun longPreferenceCanBeRetrieved() {
        val longKey = longPreferencesKey("long_key")
        val bigLong = 1L shr 50 // 2^50 > Int.MAX_VALUE
        val preferences = preferencesOf(longKey to bigLong)

        val longPreference = preferences[longKey]

        assertThat(longPreference).isEqualTo(bigLong)
    }

    @Test
    fun unsetLongPreferenceReturnsNull() {
        val longKey = longPreferencesKey("long_key")
        val preferences = emptyPreferences()

        val longPreference = preferences[longKey]

        assertThat(longPreference).isNull()
    }

    @Test
    fun stringPreferenceCanBeRetrieved() {
        val stringKey = stringPreferencesKey("string_key")
        val preferences = preferencesOf(stringKey to "string123")

        val stringPreference = preferences[stringKey]

        assertThat(stringPreference).isEqualTo("string123")
    }

    @Test
    fun unsetStringPreferenceReturnsNull() {
        val stringKey = stringPreferencesKey("string_key")
        val preferences = emptyPreferences()

        val stringPreference = preferences[stringKey]

        assertThat(stringPreference).isNull()
    }

    @Test
    fun stringSetPreferenceCanBeRetrieved() {
        val stringSetKey = stringSetPreferencesKey("string_set_key")
        val preferences = preferencesOf(stringSetKey to setOf("string1", "string2", "string3"))

        val stringSetPreference = preferences[stringSetKey]

        assertThat(stringSetPreference).isEqualTo(setOf("string1", "string2", "string3"))
    }

    @Test
    fun unsetStringSetPreferenceReturnsNull() {
        val stringSetKey = stringSetPreferencesKey("string_set_key")
        val preferences = emptyPreferences()

        val stringSetPreference = preferences[stringSetKey]

        assertThat(stringSetPreference).isNull()
    }

    @Test
    fun byteArrayPreferenceCanBeRetrieved() {
        val byteArrayKey = byteArrayPreferencesKey("byte_array_key")
        val preferences = preferencesOf(byteArrayKey to byteArrayOf(1, 2, 3, 4))

        val byteArrayPreference = preferences[byteArrayKey]

        assertThat(byteArrayPreference).isEqualTo(byteArrayOf(1, 2, 3, 4))
    }

    @Test
    fun unsetByteArrayPreferenceReturnsNull() {
        val byteArrayKey = byteArrayPreferencesKey("byte_array_key")
        val preferences = emptyPreferences()

        val byteArrayPreference = preferences[byteArrayKey]

        assertThat(byteArrayPreference).isNull()
    }

    @Test
    fun modifyingOriginalByteArrayDoesNotModifyInternalState() {
        val byteArrayKey = byteArrayPreferencesKey("byte_array_key")
        val byteArray = byteArrayOf(1, 2, 3, 4)
        val preferences = preferencesOf(byteArrayKey to byteArray)

        // modify the array passed into preferences.
        byteArray[0] = 5

        assertThat(byteArray).isEqualTo(byteArrayOf(5, 2, 3, 4))
        assertThat(preferences[byteArrayKey]).isEqualTo(byteArrayOf(1, 2, 3, 4))
    }

    @Test
    fun modifyingReturnedByteArrayDoesNotModifyInternalState() {
        val byteArrayKey = byteArrayPreferencesKey("byte_array_key")
        val byteArray = byteArrayOf(1, 2, 3, 4)
        val preferences = preferencesOf(byteArrayKey to byteArray)

        val readPrefs = preferences[byteArrayKey]!!
        readPrefs[0] = 5

        assertThat(readPrefs).isEqualTo(byteArrayOf(5, 2, 3, 4))
        assertThat(preferences[byteArrayKey]).isEqualTo(byteArrayOf(1, 2, 3, 4))
    }

    @Test
    fun asMapReturnsAllPreferences() {
        val intKey = intPreferencesKey("int_key")
        val stringSetKey = stringSetPreferencesKey("string_set_key")
        val prefs = preferencesOf(intKey to 123, stringSetKey to setOf("1", "2", "3"))

        val allPreferences: Map<Preferences.Key<*>, Any> = prefs.asMap()

        assertThat(allPreferences)
            .containsExactly(Pair(intKey, 123), Pair(stringSetKey, setOf("1", "2", "3")))
    }

    @Test
    fun clearRemovesAllPreferencesFromMutablePreferences() {
        val intKey = intPreferencesKey("int_key")
        val preferences = preferencesOf(intKey to 123)

        val clearedPreferences =
            preferences.toMutablePreferences().apply { clear() }.toPreferences()

        assertThat(clearedPreferences).isEqualTo(emptyPreferences())
    }

    @Test
    fun removeRemovesPreferenceKey() {
        val intKey = intPreferencesKey("int_key")
        val preferences = preferencesOf(intKey to 123)

        // Remove using function.
        val preferencesAfterRemove1 =
            preferences.toMutablePreferences().apply { remove(intKey) }.toPreferences()

        assertThat(preferencesAfterRemove1).isEqualTo(emptyPreferences())

        // Remove using overloaded operator.
        val preferencesAfterRemove2 =
            preferences.toMutablePreferences().also { it -= intKey }.toPreferences()

        assertThat(preferencesAfterRemove2).isEqualTo(emptyPreferences())
    }

    @Test
    fun mutablePreferencesPublicConstructor() {
        val preferences = mutablePreferencesOf().toPreferences()

        assertThat(preferences).isEqualTo(emptyPreferences())
    }

    @Test
    fun equalsReturnsTrueForDifferentInstancesWithSameValues() {
        val intKey1 = intPreferencesKey("int_key1")
        val prefs1 = preferencesOf(intKey1 to 123)
        val prefs2 = preferencesOf(intKey1 to 123)

        assertThat(prefs1).isEqualTo(prefs2)
    }

    @Test
    fun equalsReturnsFalseForDifferentKeys() {
        val intKey1 = intPreferencesKey("int_key1")
        val intKey2 = intPreferencesKey("int_key2")
        val prefs1 = preferencesOf(intKey1 to 123)
        val prefs2 = preferencesOf(intKey2 to 123)

        assertThat(prefs1).isNotEqualTo(prefs2)
    }

    @Test
    fun equalsReturnsFalseForDifferentValues() {
        val intKey1 = intPreferencesKey("int_key1")
        val prefs1 = preferencesOf(intKey1 to 123)
        val prefs2 = preferencesOf(intKey1 to 999)

        assertThat(prefs1).isNotEqualTo(prefs2)
    }

    @Test
    fun equalsReturnsFalseForDifferentStringSets() {
        val stringSetKey = stringSetPreferencesKey("string_set")
        val prefs1 = preferencesOf(stringSetKey to setOf("1"))
        val prefs2 = preferencesOf(stringSetKey to setOf())

        assertThat(prefs1).isNotEqualTo(prefs2)
    }

    @Test
    fun equalsReturnsTrueForByteArrayAndOtherWithSameValues() {
        val byteArrayKey = byteArrayPreferencesKey("byte_array")
        val intKey = intPreferencesKey("int_key")
        val prefs1 = preferencesOf(byteArrayKey to byteArrayOf(1, 2, 3), intKey to 1)
        val prefs2 = preferencesOf(byteArrayKey to byteArrayOf(1, 2, 3), intKey to 1)

        assertThat(prefs1).isEqualTo(prefs2)
    }

    @Test
    fun equalsReturnsFalseForByteArrayAndOtherWithDifferentValues() {
        val byteArrayKey = byteArrayPreferencesKey("byte_array")
        val intKey = intPreferencesKey("int_key")
        val prefs1 = preferencesOf(byteArrayKey to byteArrayOf(1, 2, 3), intKey to 1)
        val prefs2 = preferencesOf(byteArrayKey to byteArrayOf(1, 2, 4), intKey to 1)

        assertThat(prefs1).isNotEqualTo(prefs2)
    }

    @Test
    fun equalsReturnsTrueForSameByteArrays() {
        val byteArrayKey = byteArrayPreferencesKey("byte_array")
        val prefs1 = preferencesOf(byteArrayKey to byteArrayOf(1, 2, 3))
        val prefs2 = preferencesOf(byteArrayKey to byteArrayOf(1, 2, 3))

        assertThat(prefs1).isEqualTo(prefs2)
    }

    @Test
    fun equalsReturnsFalseForDifferentByteArrays() {
        val byteArrayKey = byteArrayPreferencesKey("byte_array")
        val prefs1 = preferencesOf(byteArrayKey to byteArrayOf(1, 2, 3))
        val prefs2 = preferencesOf(byteArrayKey to byteArrayOf(1, 2, 4))

        assertThat(prefs1).isNotEqualTo(prefs2)
    }

    @Test
    fun hashCodeIsSameForSameContent() {
        val byteArrayKey = byteArrayPreferencesKey("byte_array_key")
        val prefs1 = preferencesOf(byteArrayKey to byteArrayOf(1, 2, 3, 4))
        val prefs2 = preferencesOf(byteArrayKey to byteArrayOf(1, 2, 3, 4))
        val prefs3 = preferencesOf(byteArrayKey to byteArrayOf(1, 2, 3, 5))

        val hashCode1 = prefs1.hashCode()
        val hashCode2 = prefs2.hashCode()
        val hashCode3 = prefs3.hashCode()

        assertThat(hashCode1).isEqualTo(hashCode2)
        assertThat(hashCode1).isNotEqualTo(hashCode3)
    }

    @Test
    fun toPreferencesRetainsAllKeys() {
        val intKey1 = intPreferencesKey("int_key1")
        val intKey2 = intPreferencesKey("int_key2")
        val preferences = preferencesOf(intKey1 to 1, intKey2 to 2)

        val toPrefs = preferences.toPreferences()

        assertThat(toPrefs.asMap()).containsExactly(Pair(intKey1, 1), Pair(intKey2, 2))

        val mutablePreferences = preferencesOf(intKey1 to 1, intKey2 to 2)

        val mutableToPrefs = mutablePreferences.toPreferences()

        assertThat(mutableToPrefs.asMap()).containsExactly(Pair(intKey1, 1), Pair(intKey2, 2))
    }

    @Test
    fun toMutablePreferencesRetainsAllKeys() {
        val intKey1 = intPreferencesKey("int_key1")
        val intKey2 = intPreferencesKey("int_key2")
        val prefs = preferencesOf(intKey1 to 1, intKey2 to 2)

        val toPrefs = prefs.toMutablePreferences()

        assertThat(toPrefs.asMap()).containsExactly(Pair(intKey1, 1), Pair(intKey2, 2))

        val mutablePreferences = preferencesOf(intKey1 to 1, intKey2 to 2)

        val mutableToPrefs = mutablePreferences.toMutablePreferences()

        assertThat(mutableToPrefs.asMap()).containsExactly(Pair(intKey1, 1), Pair(intKey2, 2))
    }

    @Test
    fun toMutablePreferencesDoesNotMutateOriginal() {
        val intKey1 = intPreferencesKey("int_key1")
        val intKey2 = intPreferencesKey("int_key2")
        val prefs = mutablePreferencesOf(intKey1 to 1, intKey2 to 2)
        val mutablePreferences = preferencesOf(intKey1 to 1, intKey2 to 2)
        val toPrefs = prefs.toMutablePreferences()
        val mutableToPrefs = mutablePreferences.toMutablePreferences()

        toPrefs[intKey1] = 12903819
        mutableToPrefs[intKey1] = 12903819

        assertThat(prefs[intKey1]).isEqualTo(1)
        assertThat(mutablePreferences[intKey1]).isEqualTo(1)
    }

    @Test
    fun clearPreferencesWithinCopyBlock() {
        val key = intPreferencesKey("key")
        val prefsWithInt = preferencesOf(key to 123)

        val resultingPrefs = prefsWithInt.copy { it.clear() }

        assertThat(resultingPrefs.asMap()).isEmpty()
    }

    @Test
    fun removeItemWithinCopyBlock() {
        val key1 = intPreferencesKey("key1")
        val key2 = intPreferencesKey("key2")
        val key3 = intPreferencesKey("key3")
        val preferences = preferencesOf(key1 to 123, key2 to 456, key3 to 789)

        val copiedPreferences = preferences.copy { it.remove(key2) }

        assertThat(copiedPreferences.asMap()).containsExactly(Pair(key1, 123), Pair(key3, 789))
    }

    @Test
    fun removeOnlyItemWithinCopyBlock() {
        val intKey = intPreferencesKey("key")
        val prefsWithInt = preferencesOf(intKey to 123)

        val copiedPreferences = prefsWithInt.copy { it.remove(intKey) }

        assertThat(copiedPreferences.asMap()).isEmpty()
    }

    @Test
    fun copyRetainsAllKeys() {
        val key1 = intPreferencesKey("key1")
        val key2 = intPreferencesKey("key2")
        val key3 = intPreferencesKey("key3")
        val preferences = preferencesOf(key1 to 123, key2 to 456, key3 to 789)

        val copiedPreferences = preferences.copy {}

        assertThat(copiedPreferences.asMap())
            .containsExactly(Pair(key1, 123), Pair(key2, 456), Pair(key3, 789))
    }

    @Test
    fun copyDoesNotMutateOriginal() {
        val key1 = intPreferencesKey("key1")
        val key2 = intPreferencesKey("key2")
        val key3 = intPreferencesKey("key3")
        val preferences = preferencesOf(key1 to 123, key2 to 456, key3 to 789)

        val copiedPreferences = preferences.copy { pref -> pref[key2] = 100 }

        assertThat(preferences.asMap())
            .containsExactly(Pair(key1, 123), Pair(key2, 456), Pair(key3, 789))
        assertThat(copiedPreferences.asMap())
            .containsExactly(Pair(key1, 123), Pair(key2, 100), Pair(key3, 789))
    }

    @Test
    fun toStringReturnsCorrectStringRepresentation() {
        val preferences =
            preferencesOf(
                intPreferencesKey("int_key") to 123,
                booleanPreferencesKey("boolean_key") to false,
                floatPreferencesKey("float_key") to 3.14f,
                doublePreferencesKey("double_key") to 3.1415,
                stringPreferencesKey("string_key") to "abc",
                stringSetPreferencesKey("string_set_key") to setOf("1", "2", "3"),
                longPreferencesKey("long_key") to 10000000000L,
                byteArrayPreferencesKey("byte_array_key") to byteArrayOf(1, 2, 3, 4),
            )

        val stringValue = preferences.toString()

        assertThat(stringValue)
            .isEqualTo(
                """
                {
                  int_key = 123,
                  boolean_key = false,
                  float_key = 3.14,
                  double_key = 3.1415,
                  string_key = abc,
                  string_set_key = [1, 2, 3],
                  long_key = 10000000000,
                  byte_array_key = [1, 2, 3, 4]
                }
                """
                    .trimIndent()
            )
    }
}
