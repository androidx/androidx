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

package androidx.savedstate.serialization.serializers

import androidx.savedstate.IgnoreAndroidHostTarget
import androidx.savedstate.read
import androidx.savedstate.savedState
import androidx.savedstate.write
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

@IgnoreAndroidHostTarget
class SavedStateSerializerTest {

    @Test
    fun testGenericSerialization_primitivesAndStrings() {
        val original =
            savedState().apply {
                write {
                    putBoolean("bool", true)
                    putInt("int", 42)
                    putLong("long", 123456789L)
                    putFloat("float", 3.14f)
                    putDouble("double", 2.71828)
                    putString("string", "Hello World")
                    putNull("nullKey")
                }
            }

        val encoded = Json.encodeToString(SavedStateSerializer, original)
        val decoded = Json.decodeFromString(SavedStateSerializer, encoded)

        decoded.read {
            assertEquals(true, getBoolean("bool"))
            assertEquals(42, getInt("int"))
            assertEquals(123456789L, getLong("long"))
            assertEquals(3.14f, getFloat("float"))
            assertEquals(2.71828, getDouble("double"))
            assertEquals("Hello World", getString("string"))
            assertTrue(contains("nullKey"))
        }
    }

    @Test
    fun testGenericSerialization_collectionsAndNestedState() {
        val nestedOriginal = savedState().apply { write { putString("nestedKey", "nestedValue") } }
        val original =
            savedState().apply {
                write {
                    putIntList("intList", listOf(1, 2, 3))
                    putStringArray("stringArray", arrayOf("A", "B", "C"))
                    putSavedState("nested", nestedOriginal)
                }
            }

        val encoded = Json.encodeToString(SavedStateSerializer, original)
        val decoded = Json.decodeFromString(SavedStateSerializer, encoded)

        decoded.read {
            assertEquals(listOf(1, 2, 3), getIntList("intList"))

            val decodedArray = getStringArray("stringArray")
            assertEquals(3, decodedArray.size)
            assertEquals("A", decodedArray[0])

            val decodedNested = getSavedState("nested")
            decodedNested.read { assertEquals("nestedValue", getString("nestedKey")) }
        }
    }

    @Test
    fun testGenericSerialization_emptyCollections() {
        // We branch on value.isEmpty() in wrapArray/wrapList to satisfy type safety
        // with empty string arrays, so we need to ensure this doesn't throw.
        val original =
            savedState().apply {
                write {
                    putStringArray("emptyArray", emptyArray())
                    putStringList("emptyList", emptyList())
                }
            }

        val encoded = Json.encodeToString(SavedStateSerializer, original)
        val decoded = Json.decodeFromString(SavedStateSerializer, encoded)

        decoded.read {
            assertTrue(getStringArray("emptyArray").isEmpty())
            assertTrue(getStringList("emptyList").isEmpty())
        }
    }
}
