/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup.actions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PreferenceValuesTest {

    @Test
    fun parsesIntegerValues() {
        assertEquals(42, parsePreferenceValue("42", "INT"))
        assertEquals(-1, parsePreferenceValue("-1", "int"))
        assertEquals(0, parsePreferenceValue("0", "Int"))
    }

    @Test
    fun rejectsInvalidIntegerValues() {
        assertThrows(IllegalArgumentException::class.java) {
            parsePreferenceValue("not_an_int", "INT")
        }
        assertThrows(IllegalArgumentException::class.java) {
            parsePreferenceValue("1.5", "INT")
        }
    }

    @Test
    fun parsesLongValues() {
        assertEquals(42L, parsePreferenceValue("42", "LONG"))
        assertEquals(7L, parsePreferenceValue("007", "long"))
        assertEquals(-100L, parsePreferenceValue("-100", "Long"))
    }

    @Test
    fun rejectsInvalidLongValues() {
        assertThrows(IllegalArgumentException::class.java) {
            parsePreferenceValue("not_a_long", "LONG")
        }
    }

    @Test
    fun parsesFloatValues() {
        assertEquals(1.5f, parsePreferenceValue("1.5", "FLOAT"))
        assertEquals(1.5f, parsePreferenceValue("1.50", "float"))
        assertEquals(3.0f, parsePreferenceValue("3", "Float"))
    }

    @Test
    fun rejectsInvalidFloatValues() {
        assertThrows(IllegalArgumentException::class.java) {
            parsePreferenceValue("not_a_float", "FLOAT")
        }
    }

    @Test
    fun parsesBooleanValuesStrictly() {
        assertEquals(true, parsePreferenceValue("true", "BOOLEAN"))
        assertEquals(true, parsePreferenceValue("True", "boolean"))
        assertEquals(true, parsePreferenceValue("TRUE", "Boolean"))
        assertEquals(false, parsePreferenceValue("false", "BOOLEAN"))
        assertEquals(false, parsePreferenceValue("False", "boolean"))
        assertEquals(false, parsePreferenceValue("FALSE", "Boolean"))
    }

    @Test
    fun rejectsNonBooleanValuesAsBoolean() {
        assertThrows(IllegalArgumentException::class.java) {
            parsePreferenceValue("1", "BOOLEAN")
        }
        assertThrows(IllegalArgumentException::class.java) {
            parsePreferenceValue("0", "BOOLEAN")
        }
        assertThrows(IllegalArgumentException::class.java) {
            parsePreferenceValue("yes", "BOOLEAN")
        }
        assertThrows(IllegalArgumentException::class.java) {
            parsePreferenceValue("no", "BOOLEAN")
        }
        assertThrows(IllegalArgumentException::class.java) {
            parsePreferenceValue("", "BOOLEAN")
        }
        assertThrows(IllegalArgumentException::class.java) {
            parsePreferenceValue("foo", "BOOLEAN")
        }
    }

    @Test
    fun parsesStringValuesVerbatim() {
        assertEquals("hello", parsePreferenceValue("hello", "STRING"))
        assertEquals("", parsePreferenceValue("", "string"))
        assertEquals("123", parsePreferenceValue("123", "String"))
    }

    @Test
    fun rejectsUnsupportedValueTypes() {
        assertThrows(IllegalArgumentException::class.java) {
            parsePreferenceValue("1", "DOUBLE")
        }
        assertThrows(IllegalArgumentException::class.java) {
            parsePreferenceValue("1", "UNKNOWN")
        }
    }
}
