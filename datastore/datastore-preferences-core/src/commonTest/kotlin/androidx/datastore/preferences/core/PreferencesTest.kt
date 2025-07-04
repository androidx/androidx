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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PreferencesTest {

    @Test
    fun testBoolean() {
        val booleanKey = booleanPreferencesKey("boolean_key")

        val prefs = preferencesOf(booleanKey to true)

        assertTrue { booleanKey in prefs }
        assertTrue(prefs[booleanKey]!!)
    }

    @Test
    fun testBooleanNotSet() {
        val booleanKey = booleanPreferencesKey("boolean_key")

        assertNull(emptyPreferences()[booleanKey])
    }

    @Test
    fun testFloat() {
        val floatKey = floatPreferencesKey("float_key")

        val prefs = preferencesOf(floatKey to 1.1f)

        assertTrue { floatKey in prefs }
        assertEquals(1.1f, prefs[floatKey])
    }

    @Test
    fun testFloatNotSet() {
        val floatKey = floatPreferencesKey("float_key")
        assertNull(emptyPreferences()[floatKey])
    }

    @Test
    fun testDouble() {
        val doubleKey = doublePreferencesKey("double_key")

        val prefs = preferencesOf(doubleKey to Double.MAX_VALUE)

        assertTrue { doubleKey in prefs }
        assertEquals(Double.MAX_VALUE, prefs[doubleKey])
    }

    @Test
    fun testDoubleNotSet() {
        val doubleKey = doublePreferencesKey("double_key")
        assertNull(emptyPreferences()[doubleKey])
    }

    @Test
    fun testInt() {
        val intKey = intPreferencesKey("int_key")

        val prefs = preferencesOf(intKey to 1)

        assertTrue { prefs.contains(intKey) }
        assertEquals(1, prefs[intKey])
    }

    @Test
    fun testIntNotSet() {
        val intKey = intPreferencesKey("int_key")
        assertNull(emptyPreferences()[intKey])
    }

    @Test
    fun testLong() {
        val longKey = longPreferencesKey("long_key")

        val bigLong = 1L shr 50 // 2^50 > Int.MAX_VALUE

        val prefs = preferencesOf(longKey to bigLong)

        assertTrue { prefs.contains(longKey) }
        assertEquals(bigLong, prefs[longKey])
    }

    @Test
    fun testLongNotSet() {
        val longKey = longPreferencesKey("long_key")

        assertNull(emptyPreferences()[longKey])
    }

    @Test
    fun testString() {
        val stringKey = stringPreferencesKey("string_key")

        val prefs = preferencesOf(stringKey to "string123")

        assertTrue { prefs.contains(stringKey) }
        assertEquals("string123", prefs[stringKey])
    }

    @Test
    fun testStringNotSet() {
        val stringKey = stringPreferencesKey("string_key")

        assertNull(emptyPreferences()[stringKey])
    }

    @Test
    fun testStringSet() {
        val stringSetKey = stringSetPreferencesKey("string_set_key")

        val prefs = preferencesOf(stringSetKey to setOf("string1", "string2", "string3"))

        assertTrue { prefs.contains(stringSetKey) }
        assertEquals(setOf("string1", "string2", "string3"), prefs[stringSetKey])
    }

    @Test
    fun testStringSetNotSet() {
        val stringSetKey = stringSetPreferencesKey("string_set_key")

        assertNull(emptyPreferences()[stringSetKey])
    }

    @Test
    fun testByteArray() {
        val byteArrayKey = byteArrayPreferencesKey("byte_array_key")
        val byteArray = byteArrayOf(1, 2, 3, 4)

        val prefs = preferencesOf(byteArrayKey to byteArray)

        assertTrue { byteArrayKey in prefs }
        assertTrue(byteArray.contentEquals(prefs[byteArrayKey]))
    }

    @Test
    fun testByteArrayNotSet() {
        val byteArrayKey = byteArrayPreferencesKey("byte_array_key")
        assertNull(emptyPreferences()[byteArrayKey])
    }

    @Test
    fun testModifyingOriginalByteArrayDoesntModifyInternalState() {
        val byteArrayKey = byteArrayPreferencesKey("byte_array_key")
        val byteArray = byteArrayOf(1, 2, 3, 4)
        val prefs = preferencesOf(byteArrayKey to byteArray)

        byteArray[0] = 5 // modify the array passed into preferences

        assertTrue(byteArrayOf(5, 2, 3, 4).contentEquals(byteArray))
        assertTrue(byteArrayOf(1, 2, 3, 4).contentEquals(prefs[byteArrayKey]))
    }

    @Test
    fun testModifyingReturnedByteArrayDoesntModifyInternalState() {
        val byteArrayKey = byteArrayPreferencesKey("byte_array_key")
        val byteArray = byteArrayOf(1, 2, 3, 4)
        val prefs = preferencesOf(byteArrayKey to byteArray)

        val readPrefs = prefs[byteArrayKey]!!
        readPrefs[0] = 5

        assertTrue(byteArrayOf(5, 2, 3, 4).contentEquals(readPrefs))
        assertTrue(byteArrayOf(1, 2, 3, 4).contentEquals(prefs[byteArrayKey]))
    }

    @Test
    fun testGetAll() {
        val intKey = intPreferencesKey("int_key")
        val stringSetKey = stringSetPreferencesKey("string_set_key")

        val prefs = preferencesOf(intKey to 123, stringSetKey to setOf("1", "2", "3"))

        val allPreferences: Map<Preferences.Key<*>, Any> = prefs.asMap()
        assertEquals(2, allPreferences.size)

        assertEquals(123, allPreferences[intKey])
        assertEquals(setOf("1", "2", "3"), (allPreferences[stringSetKey]))
    }

    @Test
    fun testMutablePreferencesClear() {
        val intKey = intPreferencesKey("int_key")

        val prefsWithInt = preferencesOf(intKey to 123)

        val emptyPrefs = prefsWithInt.toMutablePreferences().apply { clear() }.toPreferences()

        assertEquals(emptyPreferences(), emptyPrefs)
    }

    @Test
    fun testMutablePreferencesRemove() {
        val intKey = intPreferencesKey("int_key")

        val prefsWithInt = preferencesOf(intKey to 123)

        val emptyPrefs =
            prefsWithInt.toMutablePreferences().apply { remove(intKey) }.toPreferences()

        assertEquals(emptyPreferences(), emptyPrefs)

        val emptyPrefs2 = prefsWithInt.toMutablePreferences()
        emptyPrefs2 -= intKey

        assertEquals(emptyPreferences(), emptyPrefs2)
    }

    @Test
    fun testBuilderPublicConstructor() {
        val emptyPrefs = mutablePreferencesOf().toPreferences()

        assertEquals(emptyPreferences(), emptyPrefs)
    }

    @Test
    fun testEqualsDifferentInstances() {
        val intKey1 = intPreferencesKey("int_key1")

        val prefs1 = preferencesOf(intKey1 to 123)
        val prefs2 = preferencesOf(intKey1 to 123)

        assertEquals(prefs1, prefs2)
    }

    @Test
    fun testNotEqualsDifferentKeys() {
        val intKey1 = intPreferencesKey("int_key1")
        val intKey2 = intPreferencesKey("int_key2")

        val prefs1 = preferencesOf(intKey1 to 123)
        val prefs2 = preferencesOf(intKey2 to 123)

        assertNotEquals(prefs1, prefs2)
    }

    @Test
    fun testNotEqualsDifferentValues() {
        val intKey1 = intPreferencesKey("int_key1")

        val prefs1 = preferencesOf(intKey1 to 123)
        val prefs2 = preferencesOf(intKey1 to 999)

        assertNotEquals(prefs1, prefs2)
    }

    @Test
    fun testNotEqualsDifferentStringSets() {
        val stringSetKey = stringSetPreferencesKey("string_set")

        val prefs1 = preferencesOf(stringSetKey to setOf("1"))
        val prefs2 = preferencesOf(stringSetKey to setOf())

        assertNotEquals(prefs1, prefs2)
    }

    @Test
    fun testEqualsByteArrayAndOther() {
        val byteArrayKey = byteArrayPreferencesKey("byte_array")
        val intKey = intPreferencesKey("int_key")

        val prefs1 = preferencesOf(byteArrayKey to byteArrayOf(1, 2, 3), intKey to 1)
        val prefs2 = preferencesOf(byteArrayKey to byteArrayOf(1, 2, 3), intKey to 1)

        assertEquals(prefs1, prefs2)
    }

    @Test
    fun testNotEqualsByteArrayAndOther() {
        val byteArrayKey = byteArrayPreferencesKey("byte_array")
        val intKey = intPreferencesKey("int_key")

        val prefs1 = preferencesOf(byteArrayKey to byteArrayOf(1, 2, 3), intKey to 1)
        val prefs2 = preferencesOf(byteArrayKey to byteArrayOf(1, 2, 4), intKey to 1)

        assertNotEquals(prefs1, prefs2)
    }

    @Test
    fun testEqualsSameByteArrays() {
        val byteArrayKey = byteArrayPreferencesKey("byte_array")

        val prefs1 = preferencesOf(byteArrayKey to byteArrayOf(1, 2, 3))
        val prefs2 = preferencesOf(byteArrayKey to byteArrayOf(1, 2, 3))

        assertEquals(prefs1, prefs2)
    }

    @Test
    fun testNotEqualsDifferentByteArrays() {
        val byteArrayKey = byteArrayPreferencesKey("byte_array")

        val prefs1 = preferencesOf(byteArrayKey to byteArrayOf(1, 2, 3))
        val prefs2 = preferencesOf(byteArrayKey to byteArrayOf(1, 2, 4))

        assertNotEquals(prefs1, prefs2)
    }

    @Test
    fun testByteArrayCreatesHashCodeBasedOnContents() {
        val byteArrayKey = byteArrayPreferencesKey("byte_array_key")

        val prefs = preferencesOf(byteArrayKey to byteArrayOf(1, 2, 3, 4))
        val prefs2 = preferencesOf(byteArrayKey to byteArrayOf(1, 2, 3, 4))
        val prefs3 = preferencesOf(byteArrayKey to byteArrayOf(1, 2, 3, 5))

        assertEquals(prefs.hashCode(), prefs2.hashCode())
        assertNotEquals(prefs.hashCode(), prefs3.hashCode())
    }

    @Test
    fun testToPreferences_retainsAllKeys() {
        val intKey1 = intPreferencesKey("int_key1")
        val intKey2 = intPreferencesKey("int_key2")
        val prefs = preferencesOf(intKey1 to 1, intKey2 to 2)
        val toPrefs = prefs.toPreferences()
        assertEquals(2, toPrefs.asMap().size)
        assertEquals(1, prefs[intKey1])
        assertEquals(2, prefs[intKey2])

        val mutablePreferences = preferencesOf(intKey1 to 1, intKey2 to 2)
        val mutableToPrefs = mutablePreferences.toPreferences()
        assertEquals(2, mutableToPrefs.asMap().size)
        assertEquals(1, prefs[intKey1])
        assertEquals(2, prefs[intKey2])
    }

    @Test
    fun testToMutablePreferences_retainsAllKeys() {
        val intKey1 = intPreferencesKey("int_key1")
        val intKey2 = intPreferencesKey("int_key2")
        val prefs = preferencesOf(intKey1 to 1, intKey2 to 2)
        val toPrefs = prefs.toMutablePreferences()
        assertEquals(2, toPrefs.asMap().size)
        assertEquals(1, prefs[intKey1])
        assertEquals(2, prefs[intKey2])

        val mutablePreferences = preferencesOf(intKey1 to 1, intKey2 to 2)
        val mutableToPrefs = mutablePreferences.toMutablePreferences()
        assertEquals(2, mutableToPrefs.asMap().size)
        assertEquals(1, prefs[intKey1])
        assertEquals(2, prefs[intKey2])
    }

    @Test
    fun testToMutablePreferences_doesntMutateOriginal() {
        val intKey1 = intPreferencesKey("int_key1")
        val intKey2 = intPreferencesKey("int_key2")
        val prefs = mutablePreferencesOf(intKey1 to 1, intKey2 to 2)
        val toPrefs = prefs.toMutablePreferences()
        toPrefs[intKey1] = 12903819
        assertEquals(1, prefs[intKey1])

        val mutablePreferences = preferencesOf(intKey1 to 1, intKey2 to 2)
        val mutableToPrefs = mutablePreferences.toMutablePreferences()
        mutableToPrefs[intKey1] = 12903819
        assertEquals(1, prefs[intKey1])
    }

    @Test
    fun testToString() {
        val intKey = intPreferencesKey("int_key")
        val booleanKey = booleanPreferencesKey("boolean_key")
        val floatKey = floatPreferencesKey("float_key")
        val stringKey = stringPreferencesKey("string_key")
        val stringSetKey = stringSetPreferencesKey("string_set_key")
        val longKey = longPreferencesKey("long_key")
        val byteArrayKey = byteArrayPreferencesKey("byte_array_key")

        val prefs =
            preferencesOf(
                intKey to 123,
                booleanKey to false,
                floatKey to 3.14f,
                stringKey to "abc",
                stringSetKey to setOf("1", "2", "3"),
                longKey to 10000000000L,
                byteArrayKey to byteArrayOf(1, 2, 3, 4),
            )

        assertEquals(
            """
            {
              int_key = 123,
              boolean_key = false,
              float_key = 3.14,
              string_key = abc,
              string_set_key = [1, 2, 3],
              long_key = 10000000000,
              byte_array_key = [1, 2, 3, 4]
            }
            """
                .trimIndent(),
            prefs.toString(),
        )
    }
}
