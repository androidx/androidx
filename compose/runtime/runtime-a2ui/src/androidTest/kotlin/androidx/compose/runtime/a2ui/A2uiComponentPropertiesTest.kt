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

package androidx.compose.runtime.a2ui

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiComponentPropertiesTest {

    @Test
    fun get_returnsValue() {
        val rawMap = mapOf("foo" to "bar", "count" to 42)
        val properties = A2uiComponentProperties(rawMap)

        assertThat(properties["foo"]).isEqualTo("bar")
        assertThat(properties["count"]).isEqualTo(42)
        assertThat(properties["missing"]).isNull()
    }

    @Test
    fun get_staticProperty_validValue_returnsCastedValue() {
        val rawMap = mapOf("stringValue" to "hello", "intValue" to 42)
        val properties = A2uiComponentProperties(rawMap)

        val stringProp = A2uiProperty.string("stringValue")
        val numberProp = A2uiProperty.number("intValue")

        assertThat(properties[stringProp]).isEqualTo("hello")
        assertThat(properties[numberProp]).isEqualTo(42)
    }

    @Test
    fun get_staticProperty_nullValue_returnsNull() {
        val properties = A2uiComponentProperties(mapOf("nullKey" to null))
        val stringProp = A2uiProperty.string("nullKey")

        assertThat(properties[stringProp]).isNull()
    }

    @Test
    fun get_staticProperty_variousPropertyTypes_areCorrectlyExtracted() {
        val rawMap =
            mapOf(
                "booleanProp" to true,
                "anyProp" to mapOf("a" to 1),
                "enumProp" to "optionA",
                "listProp" to listOf("a", "b"),
                "nestedProp" to mapOf("child" to "value"),
            )
        val properties = A2uiComponentProperties(rawMap)
        val booleanProp = A2uiProperty.boolean("booleanProp")
        val anyProp = A2uiProperty.any("anyProp")
        val enumProp = A2uiProperty.stringEnum("enumProp", listOf("optionA", "optionB"))
        val listProp = A2uiProperty.stringList("listProp")
        val nestedProp = A2uiProperty.nested("nestedProp", emptyList())

        assertThat(properties[booleanProp]).isTrue()
        assertThat(properties[anyProp]).isEqualTo(mapOf("a" to 1))
        assertThat(properties[enumProp]).isEqualTo("optionA")
        assertThat(properties[listProp]).isEqualTo(listOf("a", "b"))
        assertThat(properties[nestedProp]?.raw).isEqualTo(mapOf("child" to "value"))
    }

    @Test
    fun get_staticProperty_requiredMissingKey_throwsException() {
        val properties = A2uiComponentProperties(emptyMap())
        val stringProp = A2uiProperty.string("missingKey", required = true)

        val exception = assertThrows(IllegalStateException::class.java) { properties[stringProp] }

        assertThat(exception).hasMessageThat().contains("missingKey")
        assertThat(exception).hasMessageThat().contains("is missing")
    }

    @Test
    fun get_staticProperty_optionalNullValue_returnsNull() {
        val properties = A2uiComponentProperties(mapOf("nullKey" to null))
        val stringProp = A2uiProperty.string("nullKey", required = false)

        assertThat(properties[stringProp]).isNull()
    }

    @Test
    fun get_staticProperty_requiredNullValue_returnsNull() {
        val properties = A2uiComponentProperties(mapOf("nullKey" to null))
        val stringProp = A2uiProperty.string("nullKey", required = true)

        assertThat(properties[stringProp]).isNull()
    }

    @Test
    fun get_staticProperty_invalidType_throwsException() {
        val properties = A2uiComponentProperties(mapOf("stringValue" to 123))
        val stringProp = A2uiProperty.string("stringValue")

        val exception = assertThrows(IllegalStateException::class.java) { properties[stringProp] }

        assertThat(exception).hasMessageThat().contains("stringValue") // Property key
        assertThat(exception).hasMessageThat().contains("StringProperty") // Property type
        assertThat(exception).hasMessageThat().contains("Integer") // Type of the actual value
        assertThat(exception).hasMessageThat().contains("Type mismatch")
    }

    @Test
    fun equalsAndHashCode_enforcesStrictIdentityEquality() {
        val mapA = mapOf("key" to "value")
        val mapAIdentical = mapOf("key" to "value") // Deeply equal, but different instance
        val mapB = mapOf("key" to "different")

        val propsA1 = A2uiComponentProperties(mapA)
        val propsA2 = A2uiComponentProperties(mapA)
        val propsAIdentical = A2uiComponentProperties(mapAIdentical)
        val propsB = A2uiComponentProperties(mapB)

        // Reflexivity
        assertThat(propsA1).isEqualTo(propsA1)

        // Equality: wrapping the exact same map instance
        assertThat(propsA1).isEqualTo(propsA2)
        assertThat(propsA2).isEqualTo(propsA1)
        assertThat(propsA1.hashCode()).isEqualTo(propsA2.hashCode())

        // Inequality: wrapping structurally identical maps but different instances
        assertThat(propsA1).isNotEqualTo(propsAIdentical)
        assertThat(propsAIdentical).isNotEqualTo(propsA1)
        assertThat(propsA2).isNotEqualTo(propsAIdentical)

        // Inequality: wrapping a structurally different map instance
        assertThat(propsA1).isNotEqualTo(propsB)
        assertThat(propsB).isNotEqualTo(propsA1)
        assertThat(propsAIdentical).isNotEqualTo(propsB)

        // Edge cases: null and different types
        assertThat(propsA1).isNotEqualTo(null)
        assertThat(propsA1).isNotEqualTo(Any())
    }
}
