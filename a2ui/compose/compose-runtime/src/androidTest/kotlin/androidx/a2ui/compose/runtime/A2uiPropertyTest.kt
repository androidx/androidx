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

package androidx.a2ui.compose.runtime

import androidx.a2ui.model.schema.A2uiAnySchema
import androidx.a2ui.model.schema.A2uiArraySchema
import androidx.a2ui.model.schema.A2uiBooleanSchema
import androidx.a2ui.model.schema.A2uiEnumSchema
import androidx.a2ui.model.schema.A2uiNumberSchema
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiStringSchema
import androidx.a2ui.model.schema.commontypes.A2uiActionSchema
import androidx.a2ui.model.schema.commontypes.A2uiChildListSchema
import androidx.a2ui.model.schema.commontypes.A2uiComponentIdSchema
import androidx.a2ui.model.schema.commontypes.A2uiDynamicBooleanSchema
import androidx.a2ui.model.schema.commontypes.A2uiDynamicNumberSchema
import androidx.a2ui.model.schema.commontypes.A2uiDynamicStringListSchema
import androidx.a2ui.model.schema.commontypes.A2uiDynamicStringSchema
import androidx.a2ui.model.schema.commontypes.A2uiDynamicValueSchema
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiPropertyTest {

    // ========================================================================
    // NestedProperty tests
    // ========================================================================

    @Test
    fun nestedProperty_initialization_setsCorrectFieldsAndSchema() {
        val child1 = A2uiProperty.string("name", required = true)
        val child2 = A2uiProperty.number("age", required = false)

        val prop =
            A2uiProperty.nested(
                key = "user",
                properties = listOf(child1, child2),
                required = true,
                description = "A user object",
            )

        assertThat(prop.key).isEqualTo("user")
        assertThat(prop.isRequired).isTrue()
        assertThat(prop.schema)
            .isEqualTo(
                A2uiObjectSchema(
                    properties = mapOf("name" to A2uiStringSchema(), "age" to A2uiNumberSchema()),
                    required = setOf("name"),
                    description = "A user object",
                )
            )
    }

    @Test
    fun nestedProperty_safeCast_wrapsMapInA2uiComponentProperties() {
        val childProp = A2uiProperty.string("name")
        val prop = A2uiProperty.nested("user", listOf(childProp))
        val inputMap = mapOf("name" to "Alice", "extra" to 123)

        val result = prop.safeCast(inputMap)

        assertThat(result).isNotNull()
        assertThat(result?.get(childProp.key)).isEqualTo("Alice")
        assertThat(result?.raw).isEqualTo(inputMap)
    }

    @Test
    fun nestedProperty_safeCast_returnsNullForInvalidTypes() {
        val prop = A2uiProperty.nested("user", emptyList())

        assertThat(prop.safeCast("not a map")).isNull()
        assertThat(prop.safeCast(listOf("a", "b"))).isNull()
        assertThat(prop.safeCast(123)).isNull()
    }

    @Test
    fun nestedProperty_safeCast_acceptsEmptyMaps() {
        val prop = A2uiProperty.nested("obj", emptyList())

        val result = prop.safeCast(emptyMap<String, Any?>())

        assertThat(result).isNotNull()
        assertThat(result?.raw).isEmpty()
    }

    // ========================================================================
    // NestedListProperty tests
    // ========================================================================

    @Test
    fun nestedListProperty_initialization_setsCorrectFieldsAndSchema() {
        val child1 = A2uiProperty.string("title", required = true)
        val child2 = A2uiProperty.boolean("isActive", required = false)

        val prop =
            A2uiProperty.nestedList(
                key = "tabs",
                properties = listOf(child1, child2),
                required = true,
                description = "A list of tabs",
            )

        assertThat(prop.key).isEqualTo("tabs")
        assertThat(prop.isRequired).isTrue()
        assertThat(prop.schema)
            .isEqualTo(
                A2uiArraySchema(
                    items =
                        A2uiObjectSchema(
                            properties =
                                mapOf(
                                    "title" to A2uiStringSchema(),
                                    "isActive" to A2uiBooleanSchema(),
                                ),
                            required = setOf("title"),
                        ),
                    description = "A list of tabs",
                )
            )
    }

    @Test
    fun nestedListProperty_safeCast_wrapsListOfMaps() {
        val titleProp = A2uiProperty.string("title")
        val prop = A2uiProperty.nestedList("tabs", listOf(titleProp))

        val result = prop.safeCast(listOf(mapOf("title" to "Tab 1"), mapOf("title" to "Tab 2")))

        assertThat(result).isNotNull()
        assertThat(result).hasSize(2)
        assertThat(result?.get(0)?.get(titleProp.key)).isEqualTo("Tab 1")
        assertThat(result?.get(1)?.get(titleProp.key)).isEqualTo("Tab 2")
    }

    @Test
    fun nestedListProperty_safeCast_filtersOutInvalidItems() {
        val titleProp = A2uiProperty.string("title")
        val prop = A2uiProperty.nestedList("tabs", listOf(titleProp))
        val inputList =
            listOf(
                mapOf("title" to "Valid Tab"),
                "invalid_string", // Should be filtered out
                mapOf("title" to "Another Valid Tab"),
                123, // Should be filtered out
            )

        val result = prop.safeCast(inputList)

        assertThat(result).isNotNull()
        assertThat(result).hasSize(2)
        assertThat(result?.get(0)?.get(titleProp.key)).isEqualTo("Valid Tab")
        assertThat(result?.get(1)?.get(titleProp.key)).isEqualTo("Another Valid Tab")
    }

    @Test
    fun nestedListProperty_safeCast_returnsNullForNonList() {
        val prop = A2uiProperty.nestedList("tabs", emptyList())

        assertThat(prop.safeCast("not a list")).isNull()
        assertThat(prop.safeCast(mapOf("title" to "Tab 1"))).isNull()
        assertThat(prop.safeCast(123)).isNull()
    }

    @Test
    fun nestedListProperty_safeCast_acceptsEmptyLists() {
        val prop = A2uiProperty.nestedList("tabs", emptyList())

        val result = prop.safeCast(emptyList<Any>())

        assertThat(result).isNotNull()
        assertThat(result).isEmpty()
    }

    // ========================================================================
    // StringProperty tests
    // ========================================================================

    @Test
    fun stringProperty_initialization_setsCorrectFieldsAndSchema() {
        val prop = A2uiProperty.string("test", required = true, description = "Test string")

        assertThat(prop.key).isEqualTo("test")
        assertThat(prop.isRequired).isTrue()
        assertThat(prop.schema).isEqualTo(A2uiStringSchema("Test string"))
    }

    @Test
    fun stringProperty_safeCast_enforcesStrictTypes() {
        val prop = A2uiProperty.string("test")

        assertThat(prop.safeCast("valid")).isEqualTo("valid")
        assertThat(prop.safeCast(123)).isNull()
        assertThat(prop.safeCast(true)).isNull()
    }

    // ========================================================================
    // NumberProperty tests
    // ========================================================================

    @Test
    fun numberProperty_initialization_setsCorrectFieldsAndSchema() {
        val prop = A2uiProperty.number("test", required = true, description = "Test number")

        assertThat(prop.key).isEqualTo("test")
        assertThat(prop.isRequired).isTrue()
        assertThat(prop.schema).isEqualTo(A2uiNumberSchema("Test number"))
    }

    @Test
    fun numberProperty_safeCast_enforcesStrictTypes() {
        val prop = A2uiProperty.number("test")

        assertThat(prop.safeCast(123)).isEqualTo(123)
        assertThat(prop.safeCast(123.45)).isEqualTo(123.45)
        assertThat(prop.safeCast("123")).isNull()
    }

    // ========================================================================
    // BooleanProperty tests
    // ========================================================================

    @Test
    fun booleanProperty_initialization_setsCorrectFieldsAndSchema() {
        val prop = A2uiProperty.boolean("test", required = true, description = "Test boolean")

        assertThat(prop.key).isEqualTo("test")
        assertThat(prop.isRequired).isTrue()
        assertThat(prop.schema).isEqualTo(A2uiBooleanSchema("Test boolean"))
    }

    @Test
    fun booleanProperty_safeCast_enforcesStrictTypes() {
        val prop = A2uiProperty.boolean("test")

        assertThat(prop.safeCast(true)).isTrue()
        assertThat(prop.safeCast(false)).isFalse()
        assertThat(prop.safeCast("true")).isNull()
        assertThat(prop.safeCast(1)).isNull()
    }

    // ========================================================================
    // AnyProperty tests
    // ========================================================================

    @Test
    fun anyProperty_initialization_setsCorrectFieldsAndSchema() {
        val prop = A2uiProperty.any("test", required = true, description = "Test any")

        assertThat(prop.key).isEqualTo("test")
        assertThat(prop.isRequired).isTrue()
        assertThat(prop.schema).isEqualTo(A2uiAnySchema("Test any"))
    }

    @Test
    fun anyProperty_safeCast_returnsValueUnmodified() {
        val prop = A2uiProperty.any("test")

        assertThat(prop.safeCast("string")).isEqualTo("string")
        assertThat(prop.safeCast(123)).isEqualTo(123)
        assertThat(prop.safeCast(mapOf("a" to 1))).isEqualTo(mapOf("a" to 1))
    }

    // ========================================================================
    // StringListProperty tests
    // ========================================================================

    @Test
    fun stringListProperty_initialization_setsCorrectFieldsAndSchema() {
        val prop = A2uiProperty.stringList("test", required = true, description = "Test list")

        assertThat(prop.key).isEqualTo("test")
        assertThat(prop.isRequired).isTrue()
        assertThat(prop.schema).isEqualTo(A2uiArraySchema(A2uiStringSchema.INSTANCE, "Test list"))
    }

    @Test
    fun stringListProperty_safeCast_enforcesStrictTypes() {
        val prop = A2uiProperty.stringList("test")

        assertThat(prop.safeCast(listOf("a", "b"))).isEqualTo(listOf("a", "b"))
        assertThat(prop.safeCast(mapOf("0" to "a"))).isNull()
        assertThat(prop.safeCast("a, b")).isNull()
    }

    @Test
    fun stringListProperty_safeCast_acceptsEmptyLists() {
        val prop = A2uiProperty.stringList("test")

        assertThat(prop.safeCast(emptyList<Any>())).isEmpty()
    }

    // ========================================================================
    // NumberListProperty tests
    // ========================================================================

    @Test
    fun numberListProperty_initialization_setsCorrectFieldsAndSchema() {
        val prop =
            A2uiProperty.numberList("test", required = true, description = "Test number list")

        assertThat(prop.key).isEqualTo("test")
        assertThat(prop.isRequired).isTrue()
        assertThat(prop.schema)
            .isEqualTo(A2uiArraySchema(A2uiNumberSchema.INSTANCE, "Test number list"))
    }

    @Test
    fun numberListProperty_safeCast_enforcesStrictTypes() {
        val prop = A2uiProperty.numberList("test")

        assertThat(prop.safeCast(listOf(1, 2.5, 3L))).isEqualTo(listOf(1, 2.5, 3L))
        assertThat(prop.safeCast(mapOf("0" to 1))).isNull()
        assertThat(prop.safeCast("1, 2")).isNull()
    }

    @Test
    fun numberListProperty_safeCast_acceptsEmptyLists() {
        val prop = A2uiProperty.numberList("test")

        assertThat(prop.safeCast(emptyList<Any>())).isEmpty()
    }

    // ========================================================================
    // BooleanListProperty tests
    // ========================================================================

    @Test
    fun booleanListProperty_initialization_setsCorrectFieldsAndSchema() {
        val prop =
            A2uiProperty.booleanList("test", required = true, description = "Test boolean list")

        assertThat(prop.key).isEqualTo("test")
        assertThat(prop.isRequired).isTrue()
        assertThat(prop.schema)
            .isEqualTo(A2uiArraySchema(A2uiBooleanSchema.INSTANCE, "Test boolean list"))
    }

    @Test
    fun booleanListProperty_safeCast_enforcesStrictTypes() {
        val prop = A2uiProperty.booleanList("test")

        assertThat(prop.safeCast(listOf(true, false))).isEqualTo(listOf(true, false))
        assertThat(prop.safeCast(true)).isNull()
        assertThat(prop.safeCast("true, false")).isNull()
    }

    @Test
    fun booleanListProperty_safeCast_acceptsEmptyLists() {
        val prop = A2uiProperty.booleanList("test")

        assertThat(prop.safeCast(emptyList<Any>())).isEmpty()
    }

    // ========================================================================
    // AnyListProperty tests
    // ========================================================================

    @Test
    fun anyListProperty_initialization_setsCorrectFieldsAndSchema() {
        val prop = A2uiProperty.anyList("test", required = true, description = "Test any list")

        assertThat(prop.key).isEqualTo("test")
        assertThat(prop.isRequired).isTrue()
        assertThat(prop.schema).isEqualTo(A2uiArraySchema(A2uiAnySchema.INSTANCE, "Test any list"))
    }

    @Test
    fun anyListProperty_safeCast_enforcesStrictTypes() {
        val prop = A2uiProperty.anyList("test")
        val validList = listOf("a", 1, true, mapOf("k" to "v"))

        assertThat(prop.safeCast(validList)).isEqualTo(validList)
        assertThat(prop.safeCast("not a list")).isNull()
        assertThat(prop.safeCast(123)).isNull()
    }

    @Test
    fun anyListProperty_safeCast_acceptsEmptyLists() {
        val prop = A2uiProperty.anyList("test")

        assertThat(prop.safeCast(emptyList<Any>())).isEmpty()
    }

    // ========================================================================
    // StringEnumProperty tests
    // ========================================================================

    @Test
    fun stringEnumProperty_initialization_handlesEmptyEnumList() {
        val prop =
            A2uiProperty.stringEnum("test", enumValues = emptyList(), description = "Test enum")

        assertThat(prop.key).isEqualTo("test")
        assertThat(prop.schema).isEqualTo(A2uiEnumSchema(emptyList(), "Test enum"))
    }

    @Test
    fun stringEnumProperty_initialization_setsCorrectFieldsAndSchema() {
        val enumValues = listOf("A", "B")
        val prop =
            A2uiProperty.stringEnum(
                "test",
                enumValues = enumValues,
                required = true,
                description = "Test enum",
            )

        assertThat(prop.key).isEqualTo("test")
        assertThat(prop.isRequired).isTrue()
        assertThat(prop.schema).isEqualTo(A2uiEnumSchema(enumValues, "Test enum"))
    }

    @Test
    fun stringEnumProperty_safeCast_requiresString() {
        val prop = A2uiProperty.stringEnum("test", enumValues = listOf("A", "B"))

        assertThat(prop.safeCast("A")).isEqualTo("A")
        assertThat(prop.safeCast(1)).isNull()
    }

    // ========================================================================
    // NumberEnumProperty tests
    // ========================================================================

    @Test
    fun numberEnumProperty_initialization_handlesEmptyEnumList() {
        val prop =
            A2uiProperty.numberEnum("test", enumValues = emptyList(), description = "Test enum")

        assertThat(prop.key).isEqualTo("test")
        assertThat(prop.schema).isEqualTo(A2uiEnumSchema(emptyList(), "Test enum"))
    }

    @Test
    fun numberEnumProperty_initialization_setsCorrectFieldsAndSchema() {
        val enumValues = listOf(1, 2, 3)
        val prop =
            A2uiProperty.numberEnum(
                "test",
                enumValues = enumValues,
                required = true,
                description = "Test number enum",
            )

        assertThat(prop.key).isEqualTo("test")
        assertThat(prop.isRequired).isTrue()
        assertThat(prop.schema).isEqualTo(A2uiEnumSchema(enumValues, "Test number enum"))
    }

    @Test
    fun numberEnumProperty_safeCast_requiresNumber() {
        val prop = A2uiProperty.numberEnum("test", enumValues = listOf(1, 2))

        assertThat(prop.safeCast(1)).isEqualTo(1)
        assertThat(prop.safeCast("1")).isNull()
    }

    // ========================================================================
    // DynamicStringProperty tests
    // ========================================================================

    @Test
    fun dynamicStringProperty_initialization_setsCorrectFieldsAndSchema() {
        val prop =
            A2uiProperty.dynamicString("test", required = true, description = "Test dynamic string")

        assertThat(prop.key).isEqualTo("test")
        assertThat(prop.isRequired).isTrue()
        assertThat(prop.schema).isEqualTo(A2uiDynamicStringSchema("Test dynamic string"))
    }

    @Test
    fun dynamicStringProperty_safeCast_stringifiesEverything() {
        val prop = A2uiProperty.dynamicString("test")

        assertThat(prop.safeCast("text")).isEqualTo("text")
        assertThat(prop.safeCast(123)).isEqualTo("123")
        assertThat(prop.safeCast(12.5)).isEqualTo("12.5")
        assertThat(prop.safeCast(true)).isEqualTo("true")

        // Complex objects should be JSON stringified
        assertThat(prop.safeCast(mapOf("a" to 1))).isEqualTo("{\"a\":1}")
    }

    // ========================================================================
    // DynamicNumberProperty tests
    // ========================================================================

    @Test
    fun dynamicNumberProperty_initialization_setsCorrectFieldsAndSchema() {
        val prop =
            A2uiProperty.dynamicNumber("test", required = true, description = "Test dynamic number")

        assertThat(prop.key).isEqualTo("test")
        assertThat(prop.isRequired).isTrue()
        assertThat(prop.schema).isEqualTo(A2uiDynamicNumberSchema("Test dynamic number"))
    }

    @Test
    fun dynamicNumberProperty_safeCast_coercesValidStringsAndBooleans() {
        val prop = A2uiProperty.dynamicNumber("test")

        assertThat(prop.safeCast(42)).isEqualTo(42)
        assertThat(prop.safeCast(3.14)).isEqualTo(3.14)

        // Coercions
        assertThat(prop.safeCast("100")).isEqualTo(100.0)
        assertThat(prop.safeCast("-50.5")).isEqualTo(-50.5)
        assertThat(prop.safeCast(true)).isEqualTo(1)
        assertThat(prop.safeCast(false)).isEqualTo(0)

        // Invalid coercions
        assertThat(prop.safeCast("not a number")).isNull()
        assertThat(prop.safeCast(listOf(1, 2))).isNull()
    }

    @Test
    fun dynamicNumberProperty_safeCast_stringCoercionEdgeCases() {
        val prop = A2uiProperty.dynamicNumber("test")

        // Empty and blank strings should return null
        assertThat(prop.safeCast("")).isNull()
        assertThat(prop.safeCast("   ")).isNull()

        // Standard floating-point boundary strings
        assertThat(prop.safeCast("NaN")).isEqualTo(Double.NaN)
        assertThat(prop.safeCast("Infinity")).isEqualTo(Double.POSITIVE_INFINITY)
        assertThat(prop.safeCast("-Infinity")).isEqualTo(Double.NEGATIVE_INFINITY)
    }

    @Test
    fun dynamicNumberProperty_safeCast_parsesScientificNotation() {
        val prop = A2uiProperty.dynamicNumber("test")

        assertThat(prop.safeCast("1.5e3")).isEqualTo(1500.0)
        assertThat(prop.safeCast("-2.5E-2")).isEqualTo(-0.025)
    }

    @Test
    fun dynamicNumberProperty_safeCast_stringCoercionWithWhitespace() {
        val prop = A2uiProperty.dynamicNumber("test")

        assertThat(prop.safeCast("  42  ")).isEqualTo(42.0)
        assertThat(prop.safeCast("\n-3.14\t")).isEqualTo(-3.14)
    }

    @Test
    fun dynamicNumberProperty_safeCast_rejectsFormattedStringsWithCommas() {
        val prop = A2uiProperty.dynamicNumber("test")

        // Standard JSON and Kotlin's toDoubleOrNull do not support commas.
        assertThat(prop.safeCast("1,000")).isNull()
        assertThat(prop.safeCast("1,000.50")).isNull()
    }

    @Test
    fun dynamicNumberProperty_safeCast_handlesExtremeScientificNotation() {
        val prop = A2uiProperty.dynamicNumber("test")

        // Values too large for Double should gracefully coerce to Infinity.
        assertThat(prop.safeCast("1e400")).isEqualTo(Double.POSITIVE_INFINITY)
    }

    @Test
    fun dynamicNumberProperty_safeCast_handlesNonStringCharSequence() {
        val prop = A2uiProperty.dynamicNumber("test")

        // Simulating a dynamic numeric value bound from a text layout or rich-text builder
        val stringBuilder = java.lang.StringBuilder("42.5")

        assertThat(prop.safeCast(stringBuilder)).isEqualTo(42.5)
    }

    // ========================================================================
    // DynamicBooleanProperty tests
    // ========================================================================

    @Test
    fun dynamicBooleanProperty_initialization_setsCorrectFieldsAndSchema() {
        val prop =
            A2uiProperty.dynamicBoolean(
                "test",
                required = true,
                description = "Test dynamic boolean",
            )

        assertThat(prop.key).isEqualTo("test")
        assertThat(prop.isRequired).isTrue()
        assertThat(prop.schema).isEqualTo(A2uiDynamicBooleanSchema("Test dynamic boolean"))
    }

    @Test
    fun dynamicBooleanProperty_safeCast_coercesStringsAndNumbers() {
        val prop = A2uiProperty.dynamicBoolean("test")

        assertThat(prop.safeCast(true)).isTrue()
        assertThat(prop.safeCast(false)).isFalse()

        // String coercions
        assertThat(prop.safeCast("true")).isTrue()
        assertThat(prop.safeCast("True")).isTrue()
        assertThat(prop.safeCast("false")).isFalse()
        assertThat(prop.safeCast("invalid")).isFalse()

        // Number coercions (0 is false, everything else is true)
        assertThat(prop.safeCast(1)).isTrue()
        assertThat(prop.safeCast(-5)).isTrue()
        assertThat(prop.safeCast(0)).isFalse()

        // Complex objects cannot be coerced to boolean
        assertThat(prop.safeCast(mapOf("a" to 1))).isNull()
    }

    @Test
    fun dynamicBooleanProperty_safeCast_stringAndNumberEdgeCases() {
        val prop = A2uiProperty.dynamicBoolean("test")

        // Number 1 is true, but String "1" is false in Kotlin's toBoolean()
        assertThat(prop.safeCast(1)).isTrue()
        assertThat(prop.safeCast("1")).isFalse()

        // Empty strings and blank strings
        assertThat(prop.safeCast("")).isFalse()
        assertThat(prop.safeCast("   ")).isFalse()

        // Whitespace trimming
        assertThat(prop.safeCast(" true ")).isTrue()
    }

    @Test
    fun dynamicBooleanProperty_safeCast_coercesDecimalNumbersCorrectly() {
        val prop = A2uiProperty.dynamicBoolean("test")

        // 0.9 should be truthy
        assertThat(prop.safeCast(0.9)).isTrue()
        assertThat(prop.safeCast(-0.1)).isTrue()

        // 0.0 is strictly falsey
        assertThat(prop.safeCast(0.0)).isFalse()
    }

    @Test
    fun dynamicBooleanProperty_safeCast_caseInsensitiveStrings() {
        val prop = A2uiProperty.dynamicBoolean("test")

        assertThat(prop.safeCast("TrUe")).isTrue()
        assertThat(prop.safeCast("fAlSe")).isFalse()
    }

    @Test
    fun dynamicBooleanProperty_safeCast_coercesNanToFalse() {
        val prop = A2uiProperty.dynamicBoolean("test")

        assertThat(prop.safeCast(Double.NaN)).isFalse()
        assertThat(prop.safeCast(Float.NaN)).isFalse()
    }

    @Test
    fun dynamicBooleanProperty_safeCast_handlesNonStringCharSequence() {
        val prop = A2uiProperty.dynamicBoolean("test")

        // Simulating a dynamic value bound from a text layout or rich-text builder
        val stringBuilder = java.lang.StringBuilder("true")

        assertThat(prop.safeCast(stringBuilder)).isTrue()
    }

    // ========================================================================
    // DynamicValueProperty tests
    // ========================================================================

    @Test
    fun dynamicValueProperty_initialization_setsCorrectFieldsAndSchema() {
        val prop =
            A2uiProperty.dynamicValue("test", required = true, description = "Test dynamic value")

        assertThat(prop.key).isEqualTo("test")
        assertThat(prop.isRequired).isTrue()
        assertThat(prop.schema).isEqualTo(A2uiDynamicValueSchema("Test dynamic value"))
    }

    @Test
    fun dynamicValueProperty_safeCast_returnsValueUnmodified() {
        val prop = A2uiProperty.dynamicValue("test")

        assertThat(prop.safeCast("string")).isEqualTo("string")
        assertThat(prop.safeCast(123)).isEqualTo(123)
        assertThat(prop.safeCast(listOf(1, 2, 3))).isEqualTo(listOf(1, 2, 3))
    }

    // ========================================================================
    // DynamicStringListProperty tests
    // ========================================================================

    @Test
    fun dynamicStringListProperty_initialization_setsCorrectFieldsAndSchema() {
        val prop =
            A2uiProperty.dynamicStringList(
                "test",
                required = true,
                description = "Test dynamic string list",
            )

        assertThat(prop.key).isEqualTo("test")
        assertThat(prop.isRequired).isTrue()
        assertThat(prop.schema).isEqualTo(A2uiDynamicStringListSchema("Test dynamic string list"))
    }

    @Test
    fun dynamicStringListProperty_safeCast_enforcesStrictListBoundaryButCoercesChildren() {
        val prop = A2uiProperty.dynamicStringList("test")

        // Valid list with mixed primitives -> should stringify all children
        val mixedList = listOf("string", 42, true)
        assertThat(prop.safeCast(mixedList)).containsExactly("string", "42", "true").inOrder()

        // Agent hallucination: passed a scalar or map instead of a list -> must return null
        assertThat(prop.safeCast("just a string")).isNull()
        assertThat(prop.safeCast(42)).isNull()
        assertThat(prop.safeCast(mapOf("0" to "a"))).isNull()
    }

    @Test
    fun dynamicStringListProperty_safeCast_handlesNullElementsAndEmptyLists() {
        val prop = A2uiProperty.dynamicStringList("test")

        assertThat(prop.safeCast(emptyList<Any>())).isEmpty()
        assertThat(prop.safeCast(listOf("a", null, "c"))).containsExactly("a", "", "c").inOrder()
    }

    @Test
    fun dynamicStringListProperty_safeCast_stringifiesNestedComplexObjects() {
        val prop = A2uiProperty.dynamicStringList("test")
        // A list containing a map and another list
        val inputList = listOf("text", mapOf("key" to "value"), listOf(true, false))

        val result = prop.safeCast(inputList)

        assertThat(result).containsExactly("text", "{\"key\":\"value\"}", "[true,false]").inOrder()
    }

    @Test
    fun dynamicStringListProperty_safeCast_stringifiesNestedCollections() {
        val prop = A2uiProperty.dynamicStringList("test")
        // A List property where the agent hallucinates nested structures
        val nestedList = listOf(mapOf("nestedKey" to "nestedValue"), listOf(1, 2))

        val result = prop.safeCast(nestedList)

        // Each item must be validly stringified JSON
        assertThat(result).containsExactly("{\"nestedKey\":\"nestedValue\"}", "[1,2]").inOrder()
    }

    @Test
    fun dynamicStringProperty_safeCast_stringifiesLists() {
        val prop = A2uiProperty.dynamicString("test")

        assertThat(prop.safeCast(listOf("a", 2, false))).isEqualTo("[\"a\",2,false]")
    }

    @Test
    fun dynamicStringListProperty_safeCast_handlesDeepNullsInComplexObjects() {
        val prop = A2uiProperty.dynamicStringList("test")

        assertThat(prop.safeCast(listOf(mapOf("key" to null)))).containsExactly("{\"key\":null}")
    }

    // ========================================================================
    // ComponentIdProperty tests
    // ========================================================================

    @Test
    fun componentIdProperty_initialization_setsCorrectFieldsAndSchema() {
        val prop =
            A2uiProperty.componentId("test", required = true, description = "Test component ID")

        assertThat(prop.key).isEqualTo("test")
        assertThat(prop.isRequired).isTrue()
        assertThat(prop.schema).isEqualTo(A2uiComponentIdSchema("Test component ID"))
    }

    @Test
    fun componentIdProperty_safeCast_enforcesStrictString() {
        val prop = A2uiProperty.componentId("test")

        assertThat(prop.safeCast("header_id")).isEqualTo("header_id")
        assertThat(prop.safeCast(123)).isNull()
        assertThat(prop.safeCast(mapOf("id" to "header"))).isNull()
    }

    @Test
    fun componentIdProperty_safeCast_acceptsEmptyString() {
        val prop = A2uiProperty.componentId("test")

        assertThat(prop.safeCast("")).isEqualTo("")
    }

    // ========================================================================
    // ChildListProperty tests
    // ========================================================================

    @Test
    fun childListProperty_initialization_setsCorrectFieldsAndSchema() {
        val prop = A2uiProperty.childList("test", required = true, description = "Test child list")

        assertThat(prop.key).isEqualTo("test")
        assertThat(prop.isRequired).isTrue()
        assertThat(prop.schema).isEqualTo(A2uiChildListSchema("Test child list"))
    }

    // ========================================================================
    // ActionProperty tests
    // ========================================================================

    @Test
    fun actionProperty_initialization_setsCorrectFieldsAndSchema() {
        val prop = A2uiProperty.action("test", required = true, description = "Test action")

        assertThat(prop.key).isEqualTo("test")
        assertThat(prop.isRequired).isTrue()
        assertThat(prop.schema).isEqualTo(A2uiActionSchema("Test action"))
    }

    @Test
    fun actionProperty_safeCast_enforcesStrictMap() {
        val prop = A2uiProperty.action("test")

        val validAction = mapOf("event" to mapOf("name" to "submit"))
        assertThat(prop.safeCast(validAction)).isEqualTo(validAction)

        assertThat(prop.safeCast("submit")).isNull()
    }

    @Test
    fun actionProperty_safeCast_acceptsEmptyMaps() {
        val actionProp = A2uiProperty.action("act")

        assertThat(actionProp.safeCast(emptyMap<String, Any?>()))
            .isEqualTo(emptyMap<String, Any?>())
    }

    // ========================================================================
    // TypeConversion tests
    // ========================================================================

    @Test
    fun typeConversion_toString_handlesTopLevelNull() {
        assertThat(TypeConversion.toString(null)).isNull()
    }

    @Test
    fun typeConversion_toString_escapesSpecialCharactersInNestedStructures() {
        val rawString = "Line1\nLine2\t\"Quoted\"\\Slash"
        // Wrap the string in a list to trigger the deep JSON serialization logic
        val input = listOf(rawString)
        // The expected JSON array containing the properly escaped string
        val expected = "[\"Line1\\nLine2\\t\\\"Quoted\\\"\\\\Slash\"]"

        assertThat(TypeConversion.toString(input)).isEqualTo(expected)
    }

    @Test
    fun typeConversion_toString_escapesUnprintableControlCharacters() {
        // \u0001 is the ASCII 'Start of Heading' control character
        val rawString = "Data\u0001End"
        val input = listOf(rawString)

        // Verifies the custom padding logic results in exactly \\u0001
        assertThat(TypeConversion.toString(input)).isEqualTo("[\"Data\\u0001End\"]")
    }

    @Test
    fun typeConversion_toString_leavesTopLevelStringsUnescaped() {
        val input = "Line1\nLine2\t\"Quoted\"\\Slash"

        // At the top level (e.g. binding to a Text component), strings
        // must be returned exactly as-is without JSON quotes or escaping.
        assertThat(TypeConversion.toString(input)).isEqualTo(input)
    }

    @Test
    fun typeConversion_toString_handlesDeeplyNestedStructures() {
        val input =
            mapOf(
                "user" to "Alice",
                "age" to 30,
                "active" to true,
                "tags" to listOf("admin", 123),
                "metadata" to mapOf("key\n1" to null),
            )

        val result = TypeConversion.toString(input)

        val expectedJson =
            "{\"user\":\"Alice\",\"age\":30,\"active\":true,\"tags\":[\"admin\",123],\"metadata\":{\"key\\n1\":null}}"
        assertThat(result).isEqualTo(expectedJson)
    }

    @Test
    fun typeConversion_toString_escapesCustomObjectToString() {
        val customObject =
            object {
                override fun toString() = "Custom \"Object\" \n"
            }

        // Wrap in a list to force the deep node traversal
        val result = TypeConversion.toString(listOf(customObject))

        assertThat(result).isEqualTo("[\"Custom \\\"Object\\\" \\n\"]")
    }

    @Test
    fun typeConversion_toString_handlesEmptyCollections() {
        assertThat(TypeConversion.toString(emptyList<Any>())).isEqualTo("[]")
        assertThat(TypeConversion.toString(emptyMap<Any, Any>())).isEqualTo("{}")
    }

    @Test
    fun typeConversion_toString_handlesNullElementsInList() {
        val input = listOf("A", null, "B")

        assertThat(TypeConversion.toString(input)).isEqualTo("[\"A\",null,\"B\"]")
    }

    @Test
    fun typeConversion_toString_deeplyNestedNulls() {
        val input = mapOf("array" to listOf("a", null, "b"), "emptyKey" to null)

        val result = TypeConversion.toString(input)

        assertThat(result).isEqualTo("{\"array\":[\"a\",null,\"b\"],\"emptyKey\":null}")
    }

    @Test
    fun typeConversion_toString_mapWithNullKey() {
        val result = TypeConversion.toString(mapOf(null to "value"))

        assertThat(result).isEqualTo("{\"null\":\"value\"}")
    }

    @Test
    fun typeConversion_toString_invalidJsonNumbers() {
        val list = listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)

        val result = TypeConversion.toString(list)

        assertThat(result).isEqualTo("[null,null,null]")
    }

    @Test
    fun typeConversion_toString_invalidJsonFloatNumbers() {
        val list = listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)

        val result = TypeConversion.toString(list)

        assertThat(result).isEqualTo("[null,null,null]")
    }

    @Test
    fun typeConversion_toString_escapesCarriageReturnAndBackspace() {
        val rawString = "Hello\rWorld\b"
        val input = listOf(rawString)

        val result = TypeConversion.toString(input)

        assertThat(result).isEqualTo("[\"Hello\\rWorld\\b\"]")
    }

    @Test
    fun typeConversion_toString_escapesFormFeed() {
        val rawString = "Page1\u000CPage2"
        val input = listOf(rawString)

        val result = TypeConversion.toString(input)

        assertThat(result).isEqualTo("[\"Page1\\fPage2\"]")
    }

    @Test
    fun typeConversion_toString_handlesVariousArraysAndSets() {
        val intArray = intArrayOf(1, 2, 3)
        val stringSet = setOf("A", "B")
        val objectArray = arrayOf("a", "b")
        val booleanArray = booleanArrayOf(true, false)
        val charArray = charArrayOf('x', 'y')
        val doubleArray = doubleArrayOf(1.5, 2.5)

        // These should serialize to JSON arrays
        assertThat(TypeConversion.toString(intArray)).isEqualTo("[1,2,3]")
        assertThat(TypeConversion.toString(stringSet)).isEqualTo("[\"A\",\"B\"]")
        assertThat(TypeConversion.toString(objectArray)).isEqualTo("[\"a\",\"b\"]")
        assertThat(TypeConversion.toString(booleanArray)).isEqualTo("[true,false]")
        assertThat(TypeConversion.toString(charArray)).isEqualTo("[\"x\",\"y\"]")
        assertThat(TypeConversion.toString(doubleArray)).isEqualTo("[1.5,2.5]")
    }

    @Test
    fun typeConversion_toString_breaksCyclicReferencesOrEnforcesMaxDepth() {
        val cyclicList = mutableListOf<Any>()
        cyclicList.add(cyclicList) // Creates a cycle

        // Assert that the app didn't crash from a StackOverflowError
        assertThat(TypeConversion.toString(cyclicList)).isNotNull()
    }
}
