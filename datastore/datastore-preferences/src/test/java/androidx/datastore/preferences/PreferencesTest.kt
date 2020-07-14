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

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(JUnit4::class)
class PreferencesTest {

    @Test
    fun testBoolean() {
        val booleanKey = "boolean_key"

        val prefs = Preferences
            .empty()
            .toBuilder()
            .setBoolean(booleanKey, true)
            .build()

        assertTrue { prefs.contains(booleanKey) }
        assertTrue { prefs.getBoolean(booleanKey, false) }
    }

    @Test
    fun testBooleanDefault() {
        assertFalse(Preferences.empty().getBoolean("nonexistent key", false))
    }

    @Test
    fun testFloat() {
        val floatKey = "float_key"

        val prefs = Preferences
            .empty()
            .toBuilder()
            .setFloat(floatKey, 1.1f)
            .build()

        assertTrue { prefs.contains(floatKey) }
        assertEquals(1.1f, prefs.getFloat(floatKey, 0.0f))
    }

    @Test
    fun testFloatDefault() {
        assertEquals(0.1f, Preferences.empty().getFloat("nonexistent key", 0.1f))
    }

    @Test
    fun testInt() {
        val intKey = "int_key"

        val prefs = Preferences
            .empty()
            .toBuilder()
            .setInt(intKey, 1)
            .build()

        assertTrue { prefs.contains(intKey) }
        assertEquals(1, prefs.getInt(intKey, -1))
    }

    @Test
    fun testIntDefault() {
        assertEquals(123, Preferences.empty().getInt("nonexistent key", 123))
    }

    @Test
    fun testLong() {
        val longKey = "long_key"

        val bigLong = 1L shr 50; // 2^50 > Int.MAX_VALUE

        val prefs = Preferences
            .empty()
            .toBuilder()
            .setLong(longKey, bigLong)
            .build()

        assertTrue { prefs.contains(longKey) }
        assertEquals(bigLong, prefs.getLong(longKey, -1))
    }

    @Test
    fun testLongDefault() {
        assertEquals(123, Preferences.empty().getLong("nonexistent key", 123))
    }

    @Test
    fun testString() {
        val stringKey = "string_key"

        val prefs = Preferences
            .empty()
            .toBuilder()
            .setString(stringKey, "string123")
            .build()

        assertTrue { prefs.contains(stringKey) }
        assertEquals("string123", prefs.getString(stringKey, "default string"))
    }

    @Test
    fun testStringDefault() {
        assertEquals("default val", Preferences.empty().getString("nonexistent key", "default val"))
    }

    @Test
    fun testStringSet() {
        val stringSetKey = "string_set_key"

        val prefs = Preferences
            .empty()
            .toBuilder()
            .setStringSet(stringSetKey, setOf("string1", "string2", "string3"))
            .build()

        assertTrue { prefs.contains(stringSetKey) }
        assertEquals(

            setOf(
                "string1",
                "string2",
                "string3"
            ), prefs.getStringSet(stringSetKey, setOf())
        )
    }

    @Test
    fun testStringSetDefault() {
        assertEquals(
            setOf("default set"), Preferences.empty().getStringSet(
                "nonexistent key", setOf("default set")
            )
        )
    }

    @Test
    fun testModifyingStringSetDoesntModifyInternalState() {
        val stringSetKey = "string_set_key"

        val prefs = Preferences
            .empty()
            .toBuilder()
            .setStringSet(stringSetKey, setOf("string1", "string2", "string3"))
            .build()

        val returnedSet: Set<String> = prefs.getStringSet(stringSetKey, setOf())
        val mutableReturnedSet: MutableSet<String> = returnedSet as MutableSet<String>
        mutableReturnedSet.clear()
        mutableReturnedSet.add("Original set does not contain this string")

        assertEquals(
            setOf(
                "string1",
                "string2",
                "string3"
            ),
            prefs.getStringSet(stringSetKey, setOf())
        )
    }

    @Test
    fun testWrongTypeThrowsClassCastException() {
        val stringKey = "string_key"

        val prefs = Preferences
            .empty()
            .toBuilder()
            .setString(stringKey, "string123")
            .build()

        assertTrue { prefs.contains(stringKey) }

        // Trying to get a long where there is a string value throws a ClassCastException.
        assertFailsWith<ClassCastException> { prefs.getLong(stringKey, 123) }
    }

    @Test
    fun testGetAll() {
        val intKey = "int_key"
        val stringSetKey = "string_set_key"

        val prefs = Preferences
            .empty()
            .toBuilder()
            .setInt(intKey, 123)
            .setStringSet(stringSetKey, setOf("1", "2", "3"))
            .build()

        val allPreferences = prefs.getAll()
        assertEquals(2, allPreferences.size)

        assertEquals(123, allPreferences[intKey])
        assertEquals(setOf("1", "2", "3"), (allPreferences[stringSetKey]))
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun testGetAllCantMutateInternalState() {
        val intKey = "int_key"
        val stringSetKey = "string_set_key"

        val prefs = Preferences
            .empty()
            .toBuilder()
            .setInt(intKey, 123)
            .setStringSet(stringSetKey, setOf("1", "2", "3"))
            .build()

        val mutableAllPreferences = prefs.getAll() as MutableMap
        mutableAllPreferences[intKey] = 99999
        (mutableAllPreferences[stringSetKey] as MutableSet<String>).clear()

        assertEquals(123, prefs.getInt(intKey, -1))
        assertEquals(setOf("1", "2", "3"), prefs.getStringSet(stringSetKey, setOf()))
    }

    @Test
    fun testBuilderClear() {
        val intKey = "int_key"

        val prefsWithInt = Preferences
            .empty()
            .toBuilder()
            .setInt(intKey, 123)
            .build()

        val emptyPrefs = prefsWithInt.toBuilder().clear().build()

        assertEquals(Preferences.empty(), emptyPrefs)
    }

    @Test
    fun testBuilderRemove() {
        val intKey = "int_key"

        val prefsWithInt = Preferences
            .empty()
            .toBuilder()
            .setInt(intKey, 123)
            .build()

        val emptyPrefs = prefsWithInt.toBuilder().remove(intKey).build()

        assertEquals(Preferences.empty(), emptyPrefs)
    }

    @Test
    fun testBuilderPublicConstructor() {
        val emptyPrefs = Preferences.Builder().build()

        assertEquals(Preferences.empty(), emptyPrefs)
    }

    @Test
    fun testEqualsDifferentInstances() {
        val intKey1 = "int_key1"

        val prefs1 = Preferences.empty().toBuilder().setInt(intKey1, 123).build()
        val prefs2 = Preferences.empty().toBuilder().setInt(intKey1, 123).build()

        assertEquals(prefs1, prefs2)
    }

    @Test
    fun testNotEqualsDifferentKeys() {
        val intKey1 = "int_key1"
        val intKey2 = "int_key2"

        val prefs1 = Preferences
            .empty()
            .toBuilder()
            .setInt(intKey1, 123)
            .build()

        val prefs2 = Preferences
            .empty()
            .toBuilder()
            .setInt(intKey2, 123)
            .build()

        assertNotEquals(prefs1, prefs2)
    }

    @Test
    fun testNotEqualsDifferentValues() {
        val intKey = "int_key"

        val prefs1 = Preferences
            .empty()
            .toBuilder()
            .setInt(intKey, 123)
            .build()

        val prefs2 = Preferences
            .empty()
            .toBuilder()
            .setInt(intKey, 999)
            .build()

        assertNotEquals(prefs1, prefs2)
    }

    @Test
    fun testNotEqualsDifferentStringSets() {
        val stringSetKey = "string_set_key"

        val prefs1 = Preferences
            .empty()
            .toBuilder()
            .setStringSet(stringSetKey, setOf("string1", "string2"))
            .build()

        val prefs2 = Preferences
            .empty()
            .toBuilder()
            .setStringSet(stringSetKey, setOf("different string1", "string2"))
            .build()

        assertNotEquals(prefs1, prefs2)
    }
}