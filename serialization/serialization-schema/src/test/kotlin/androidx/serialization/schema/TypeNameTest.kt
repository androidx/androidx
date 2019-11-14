/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.serialization.schema

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [TypeName].
 */
class TypeNameTest {
    @Test(expected = IllegalArgumentException::class)
    fun testAtLeastOneNameRequirement() {
        TypeName("com.example", emptyList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun testNonEmptyNameRequirement() {
        TypeName("com.example", "Foo", "", "Bar")
    }

    @Test
    fun testDefaultNullPackage() {
        assertNull(TypeName(names = listOf("Foo")).packageName)
    }

    @Test
    fun testEmptyPackageName() {
        assertNull(TypeName("", "Foo").packageName)
    }

    @Test
    fun testCanonicalNameSingle() {
        assertEquals("com.example.Foo", TypeName("com.example", "Foo").canonicalName)
    }

    @Test
    fun testCanonicalNameNested() {
        assertEquals("com.example.Foo.Bar", TypeName("com.example", "Foo", "Bar").canonicalName)
    }

    @Test
    fun testCanonicalNameNullPackage() {
        assertEquals("Foo", TypeName(null, "Foo").canonicalName)
    }

    @Test
    fun testSimpleNameSingle() {
        assertEquals("Foo", TypeName("com.example", "Foo").simpleName)
    }

    @Test
    fun testSimpleNameNested() {
        assertEquals("Bar", TypeName("com.example", "Foo", "Bar").simpleName)
    }

    @Test
    fun testToString() {
        val name = TypeName("com.example", "Foo", "Bar")
        assertEquals(name.canonicalName, name.toString())
    }

    @Test
    fun testHashCode() {
        val a = TypeName("com.example", "Foo")
        val b = TypeName("com.example", "Foo")
        val c = TypeName("com.example", "Bar")

        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a.hashCode(), c.hashCode())
    }

    @Test
    fun testEquals() {
        val a = TypeName("com.example", "Foo")
        val b = TypeName("com.example", "Foo")
        val c = TypeName("com.example", "Bar")

        assertTrue(a == b)
        assertTrue(b == a)
        assertFalse(a == c)
        assertFalse(c == a)
    }
}