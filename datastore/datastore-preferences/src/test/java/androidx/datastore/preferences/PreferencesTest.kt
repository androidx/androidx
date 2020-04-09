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

import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

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

        assertThat(prefs.contains(booleanKey)).isTrue()
        assertThat(prefs.getBoolean(booleanKey, false)).isTrue()
    }

    @Test
    fun testBooleanDefault() {
        assertThat(Preferences.empty().getBoolean("nonexistent key", false))
            .isFalse()
    }

    @Test
    fun testFloat() {
        val floatKey = "float_key"

        val prefs = Preferences
            .empty()
            .toBuilder()
            .setFloat(floatKey, 1.1f)
            .build()

        assertThat(prefs.contains(floatKey)).isTrue()
        assertThat(prefs.getFloat(floatKey, 0.0f)).isEqualTo(1.1f)
    }

    @Test
    fun testFloatDefault() {
        assertThat(Preferences.empty().getFloat("nonexistent key", 0.1f))
            .isEqualTo(0.1f)
    }

    @Test
    fun testInt() {
        val intKey = "int_key"

        val prefs = Preferences
            .empty()
            .toBuilder()
            .setInt(intKey, 1)
            .build()

        assertThat(prefs.contains(intKey)).isTrue()
        assertThat(prefs.getInt(intKey, -1)).isEqualTo(1)
    }

    @Test
    fun testIntDefault() {
        assertThat(Preferences.empty().getInt("nonexistent key", 123))
            .isEqualTo(123)
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

        assertThat(prefs.contains(longKey)).isTrue()
        assertThat(prefs.getLong(longKey, -1)).isEqualTo(bigLong)
    }

    @Test
    fun testLongDefault() {
        assertThat(Preferences.empty().getLong("nonexistent key", 123))
            .isEqualTo(123)
    }

    @Test
    fun testString() {
        val stringKey = "string_key"

        val prefs = Preferences
            .empty()
            .toBuilder()
            .setString(stringKey, "string123")
            .build()

        assertThat(prefs.contains(stringKey)).isTrue()
        assertThat(prefs.getString(stringKey, "default string"))
            .isEqualTo("string123")
    }

    @Test
    fun testStringDefault() {
        assertThat(Preferences.empty().getString("nonexistent key", "default val"))
            .isEqualTo("default val")
    }

    @Test
    fun testStringSet() {
        val stringSetKey = "string_set_key"

        val prefs = Preferences
            .empty()
            .toBuilder()
            .setStringSet(stringSetKey, setOf("string1", "string2", "string3"))
            .build()

        assertThat(prefs.contains(stringSetKey)).isTrue()
        assertThat(prefs.getStringSet(stringSetKey, setOf())).isEqualTo(
            setOf(
                "string1",
                "string2",
                "string3"
            )
        )
    }

    @Test
    fun testStringSetDefault() {
        assertThat(
            Preferences.empty().getStringSet(
                "nonexistent key", setOf("default set")
            )
        ).isEqualTo(setOf("default set"))
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

        assertThat(prefs.getStringSet(stringSetKey, setOf())).isEqualTo(
            setOf(
                "string1",
                "string2",
                "string3"
            )
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

        assertThat(prefs.contains(stringKey)).isTrue()

        // Trying to get a long where there is a string value throws a ClassCastException.
        assertThrows<ClassCastException> { prefs.getLong(stringKey, 123) }
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
        assertThat(allPreferences.size).isEqualTo(2)

        assertThat(allPreferences[intKey]).isEqualTo(123)
        assertThat(allPreferences[stringSetKey]).isEqualTo(setOf("1", "2", "3"))
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

        assertThat(prefs.getInt(intKey, -1)).isEqualTo(123)
        assertThat(prefs.getStringSet(stringSetKey, setOf())).isEqualTo(setOf("1", "2", "3"))
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

        assertThat(emptyPrefs).isEqualTo(Preferences.empty())
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

        assertThat(emptyPrefs).isEqualTo(Preferences.empty())
    }

    @Test
    fun testBuilderPublicConstructor() {
        val emptyPrefs = Preferences.Builder().build()

        assertThat(emptyPrefs).isEqualTo(Preferences.empty())
    }

    @Test
    fun testEqualsDifferentInstances() {
        val intKey1 = "int_key1"

        val prefs1 = Preferences.empty().toBuilder().setInt(intKey1, 123).build()
        val prefs2 = Preferences.empty().toBuilder().setInt(intKey1, 123).build()

        assertThat(prefs1).isEqualTo(prefs2)
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

        assertThat(prefs1).isNotEqualTo(prefs2)
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

        assertThat(prefs1).isNotEqualTo(prefs2)
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

        assertThat(prefs1).isNotEqualTo(prefs2)
    }
}