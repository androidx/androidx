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

import com.google.common.truth.Truth.assertThat
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
        assertThat(TypeName(names = listOf("Foo")).packageName).isNull()
    }

    @Test
    fun testEmptyPackageName() {
        assertThat(TypeName("", "Foo").packageName).isNull()
    }

    @Test
    fun testCanonicalNameSingle() {
        assertThat(TypeName("com.example", "Foo").canonicalName)
            .isEqualTo("com.example.Foo")
    }

    @Test
    fun testCanonicalNameNested() {
        assertThat(TypeName("com.example", "Foo", "Bar").canonicalName)
            .isEqualTo("com.example.Foo.Bar")
    }

    @Test
    fun testCanonicalNameNullPackage() {
        assertThat(TypeName(null, "Foo").canonicalName).isEqualTo("Foo")
    }

    @Test
    fun testSimpleNameSingle() {
        assertThat(TypeName("com.example", "Foo").simpleName).isEqualTo("Foo")
    }

    @Test
    fun testSimpleNameNested() {
        assertThat(TypeName("com.example", "Foo", "Bar").simpleName).isEqualTo("Bar")
    }

    @Test
    fun testToString() {
        val name = TypeName("com.example", "Foo", "Bar")
        assertThat(name.toString()).isSameInstanceAs(name.canonicalName)
    }

    @Test
    fun testHashCode() {
        assertThat(TypeName("com.example", "Foo").hashCode())
            .isEqualTo(TypeName("com.example", "Foo").hashCode())

        assertThat(TypeName("com.example", "Bar").hashCode())
            .isNotEqualTo(TypeName("com.example", "Foo").hashCode())

        assertThat(TypeName("com.example.foo", "Quux").hashCode())
            .isNotEqualTo(TypeName("com.example.bar", "Quux").hashCode())
    }

    @Test
    fun testEquals() {
        assertThat(TypeName("com.example", "Foo", "Bar"))
            .isEqualTo(TypeName("com.example", "Foo", "Bar"))

        assertThat(TypeName("com.example", "Foo", "Bar"))
            .isNotEqualTo(TypeName("com.example", "Foo", "Quux"))

        assertThat(TypeName("com.example.foo", "Quux"))
            .isNotEqualTo(TypeName("com.example.bar", "Quux"))
    }

    @Test
    fun testCompareToByPackages() {
        assertThat(TypeName(null, "Foo"))
            .isEquivalentAccordingToCompareTo(TypeName(null, "Foo"))

        assertThat(TypeName("com.example", "Foo"))
            .isGreaterThan(TypeName(null, "Foo"))

        assertThat(TypeName("com.example", "Foo"))
            .isEquivalentAccordingToCompareTo(TypeName("com.example", "Foo"))

        assertThat(TypeName("com.example.foo", "Foo"))
            .isGreaterThan(TypeName("com.example.bar", "Foo"))

        assertThat(TypeName("com.example", "Foo"))
            .isLessThan(TypeName("com.example.foo", "Foo"))
    }

    @Test
    fun testCompareToByNames() {
        assertThat(TypeName(null, "Foo"))
            .isEquivalentAccordingToCompareTo(TypeName(null, "Foo"))

        assertThat(TypeName(null, "Foo"))
            .isGreaterThan(TypeName(null, "Bar"))

        assertThat(TypeName(null, "Foo", "Bar"))
            .isGreaterThan(TypeName(null, "Foo"))
    }
}
