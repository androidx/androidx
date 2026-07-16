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

package androidx.a2ui.engine.schema

import androidx.a2ui.engine.catalog.A2uiCoreCatalog
import androidx.a2ui.engine.catalog.A2uiCoreComponentDefinition
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.catalog.A2uiFunctionDefinition
import androidx.a2ui.model.catalog.A2uiFunctionReturnType
import androidx.a2ui.model.protocol.A2uiException
import androidx.a2ui.model.protocol.A2uiExecutionContext
import androidx.a2ui.model.schema.A2uiAllOfSchema
import androidx.a2ui.model.schema.A2uiAnyOfSchema
import androidx.a2ui.model.schema.A2uiAnySchema
import androidx.a2ui.model.schema.A2uiArraySchema
import androidx.a2ui.model.schema.A2uiBooleanSchema
import androidx.a2ui.model.schema.A2uiCompositeSchema
import androidx.a2ui.model.schema.A2uiConstSchema
import androidx.a2ui.model.schema.A2uiEnumSchema
import androidx.a2ui.model.schema.A2uiNumberSchema
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiOneOfSchema
import androidx.a2ui.model.schema.A2uiRefSchema
import androidx.a2ui.model.schema.A2uiSchema
import androidx.a2ui.model.schema.A2uiStringSchema
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test

class A2UiCoreSchemaValidatorTest {

    private val validator = A2uiCoreSchemaValidator()

    @Test
    fun validateSchema_stringTypeWithValidString_returnsTrue() {
        val schema = A2uiStringSchema()
        assertThat(validator.validateSchema("hello", schema)).isTrue()
    }

    @Test
    fun validateSchema_stringTypeWithNumber_throwsValidationException() {
        val schema = A2uiStringSchema()
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(123, schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_stringTypeWithBoolean_throwsValidationException() {
        val schema = A2uiStringSchema()
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(true, schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_numberTypeWithValidInteger_returnsTrue() {
        val schema = A2uiNumberSchema()
        assertThat(validator.validateSchema(123, schema)).isTrue()
    }

    @Test
    fun validateSchema_numberTypeWithValidDouble_returnsTrue() {
        val schema = A2uiNumberSchema()
        assertThat(validator.validateSchema(123.45, schema)).isTrue()
    }

    @Test
    fun validateSchema_numberTypeWithStringifiedNumber_throwsValidationException() {
        val schema = A2uiNumberSchema()
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema("123", schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_numberTypeWithInvalidString_throwsValidationException() {
        val schema = A2uiNumberSchema()
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema("abc", schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_numberTypeWithBoolean_throwsValidationException() {
        val schema = A2uiNumberSchema()
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(true, schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_booleanTypeWithValidTrue_returnsTrue() {
        val schema = A2uiBooleanSchema()
        assertThat(validator.validateSchema(true, schema)).isTrue()
    }

    @Test
    fun validateSchema_booleanTypeWithValidFalse_returnsTrue() {
        val schema = A2uiBooleanSchema()
        assertThat(validator.validateSchema(false, schema)).isTrue()
    }

    @Test
    fun validateSchema_booleanTypeWithStringifiedBoolean_throwsValidationException() {
        val schema = A2uiBooleanSchema()
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema("true", schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_booleanTypeWithInvalidString_throwsValidationException() {
        val schema = A2uiBooleanSchema()
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema("abc", schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_booleanTypeWithNumber_throwsValidationException() {
        val schema = A2uiBooleanSchema()
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(123, schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_objectTypeWithAllProperties_returnsTrue() {
        assertThat(validator.validateSchema(mapOf("name" to "Alice", "age" to 30), OBJECT_SCHEMA))
            .isTrue()
    }

    @Test
    fun validateSchema_objectTypeWithOnlyRequiredProperties_returnsTrue() {
        assertThat(validator.validateSchema(mapOf("name" to "Alice"), OBJECT_SCHEMA)).isTrue()
    }

    @Test
    fun validateSchema_objectTypeMissingRequiredProperty_throwsValidationExceptionWithCorrectPath() {
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(mapOf("age" to 30), OBJECT_SCHEMA)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_objectTypeWithInvalidPropertyType_throwsValidationExceptionWithCorrectPath() {
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(mapOf("name" to 123), OBJECT_SCHEMA)
            }
        assertThat(ex.context["path"]).isEqualTo("$.name")
    }

    @Test
    fun validateSchema_objectTypeWithUnallowedAdditionalProperty_throwsValidationExceptionWithCorrectPath() {
        val strictObjectSchema =
            A2uiObjectSchema(
                properties = mapOf("name" to A2uiStringSchema()),
                isAdditionalPropertiesAllowed = false,
            )
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(
                    mapOf("name" to "Alice", "extra" to true),
                    strictObjectSchema,
                )
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_nestedObjectWithInvalidPropertyType_throwsValidationExceptionWithCorrectPath() {
        val nestedSchema = A2uiObjectSchema(properties = mapOf("user" to OBJECT_SCHEMA))
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(mapOf("user" to mapOf("name" to 123)), nestedSchema)
            }
        assertThat(ex.context["path"]).isEqualTo("$.user.name")
    }

    @Test
    fun validateSchema_arrayTypeWithValidItems_returnsTrue() {
        val schema = A2uiArraySchema(items = A2uiNumberSchema())
        assertThat(validator.validateSchema(listOf(1, 2, 3), schema)).isTrue()
    }

    @Test
    fun validateSchema_arrayTypeWithInvalidItems_throwsValidationExceptionWithCorrectPath() {
        val schema = A2uiArraySchema(items = A2uiNumberSchema())
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(listOf(1, "abc", 3), schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$[1]")
    }

    @Test
    fun validateSchema_nestedArrayWithInvalidItems_throwsValidationExceptionWithCorrectPath() {
        val schema =
            A2uiObjectSchema(
                properties = mapOf("list" to A2uiArraySchema(items = A2uiStringSchema()))
            )
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(mapOf("list" to listOf("a", 2, "c")), schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$.list[1]")
    }

    @Test
    fun validateSchema_arrayTypeWithNotArray_throwsValidationException() {
        val schema = A2uiArraySchema(items = A2uiNumberSchema())
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(mapOf("items" to listOf(1, 2, 3)), schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_enumTypeWithMatchingStringValue_returnsTrue() {
        val schema = A2uiEnumSchema(listOf("a", "b", "c"))
        assertThat(validator.validateSchema("a", schema)).isTrue()
        assertThat(validator.validateSchema("b", schema)).isTrue()
        assertThat(validator.validateSchema("c", schema)).isTrue()
    }

    @Test
    fun validateSchema_enumTypeWithNonMatchingStringValue_throwsValidationException() {
        val schema = A2uiEnumSchema(listOf("a", "b", "c"))
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema("d", schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_enumTypeWithMatchingNumberValue_returnsTrue() {
        val schema = A2uiEnumSchema(listOf(1, 2, 3))
        assertThat(validator.validateSchema(1, schema)).isTrue()
        assertThat(validator.validateSchema(2, schema)).isTrue()
        assertThat(validator.validateSchema(3, schema)).isTrue()
    }

    @Test
    fun validateSchema_enumTypeWithNonMatchingNumberValue_throwsValidationException() {
        val schema = A2uiEnumSchema(listOf(1, 2, 3))
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(4, schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_enumTypeWithMatchingBooleanValue_returnsTrue() {
        val schema = A2uiEnumSchema(listOf(true, false))
        assertThat(validator.validateSchema(true, schema)).isTrue()
        assertThat(validator.validateSchema(false, schema)).isTrue()
    }

    @Test
    fun validateSchema_enumTypeWithNonMatchingBooleanValue_throwsValidationException() {
        val schema = A2uiEnumSchema(listOf(true))
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(false, schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_enumTypeWithMatchingListValue_returnsTrue() {
        val schema = A2uiEnumSchema(listOf(listOf("a", "b"), listOf("c", "d")))
        assertThat(validator.validateSchema(listOf("a", "b"), schema)).isTrue()
        assertThat(validator.validateSchema(listOf("c", "d"), schema)).isTrue()
    }

    @Test
    fun validateSchema_enumTypeWithNonMatchingListValue_throwsValidationException() {
        val schema = A2uiEnumSchema(listOf(listOf("a", "b")))
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(listOf("a", "c"), schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_enumTypeWithMatchingMapValue_returnsTrue() {
        val schema = A2uiEnumSchema(listOf(mapOf("key1" to "value"), mapOf("key2" to 1)))
        assertThat(validator.validateSchema(mapOf("key1" to "value"), schema)).isTrue()
        assertThat(validator.validateSchema(mapOf("key2" to 1), schema)).isTrue()
    }

    @Test
    fun validateSchema_enumTypeWithNonMatchingMapValue_throwsValidationException() {
        val schema = A2uiEnumSchema(listOf(mapOf("key" to "value")))
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(mapOf("key" to "different"), schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_enumTypeWithMatchingNestedListValue_returnsTrue() {
        val schema = A2uiEnumSchema(listOf(listOf(listOf("a", "b")), listOf(listOf(1, 2))))
        assertThat(validator.validateSchema(listOf(listOf("a", "b")), schema)).isTrue()
        assertThat(validator.validateSchema(listOf(listOf(1, 2)), schema)).isTrue()
    }

    @Test
    fun validateSchema_enumTypeWithNonMatchingNestedListValue_throwsValidationException() {
        val schema = A2uiEnumSchema(listOf(listOf(listOf("a", "b"))))
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(listOf(listOf("a", "c")), schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_enumTypeWithMatchingNestedMapValue_returnsTrue() {
        val schema = A2uiEnumSchema(listOf(mapOf("outer" to mapOf("inner" to "value"))))
        assertThat(validator.validateSchema(mapOf("outer" to mapOf("inner" to "value")), schema))
            .isTrue()
    }

    @Test
    fun validateSchema_enumTypeWithNonMatchingNestedMapValue_throwsValidationException() {
        val schema = A2uiEnumSchema(listOf(mapOf("outer" to mapOf("inner" to "value"))))
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(mapOf("outer" to mapOf("inner" to "wrong")), schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_anyTypeWithObject_returnsTrue() {
        val schema = A2uiAnySchema()
        assertThat(validator.validateSchema(mapOf("some" to "data"), schema)).isTrue()
    }

    @Test
    fun validateSchema_anyTypeWithNumber_returnsTrue() {
        val schema = A2uiAnySchema()
        assertThat(validator.validateSchema(123, schema)).isTrue()
    }

    @Test
    fun validateSchema_oneOfTypeMatchingExactlyOne_returnsTrue() {
        val schema = A2uiOneOfSchema(listOf(A2uiStringSchema(), A2uiNumberSchema()))
        assertThat(validator.validateSchema("hello", schema)).isTrue()
        assertThat(validator.validateSchema(123, schema)).isTrue()
    }

    @Test
    fun validateSchema_oneOfTypeWithMixedTypesAndStringifiedValue_returnsTrue() {
        val schema = A2uiOneOfSchema(listOf(A2uiStringSchema(), A2uiNumberSchema()))
        assertThat(validator.validateSchema("123", schema)).isTrue()
    }

    @Test
    fun validateSchema_oneOfTypeMatchingNone_throwsValidationException() {
        val schema = A2uiOneOfSchema(listOf(A2uiStringSchema(), A2uiNumberSchema()))
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(true, schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_oneOfTypeMatchingMultiple_throwsValidationException() {
        val schema =
            A2uiOneOfSchema(
                listOf(
                    A2uiObjectSchema(
                        properties = mapOf("name" to A2uiStringSchema()),
                        required = setOf("name"),
                    ),
                    A2uiObjectSchema(
                        properties = mapOf("age" to A2uiNumberSchema()),
                        required = setOf("age"),
                    ),
                )
            )
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(mapOf("name" to "Alice", "age" to 30), schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_allOfTypeMatchingAll_returnsTrue() {
        val schema =
            A2uiAllOfSchema(
                listOf(
                    A2uiObjectSchema(
                        properties = mapOf("name" to A2uiStringSchema()),
                        required = setOf("name"),
                    ),
                    A2uiObjectSchema(
                        properties = mapOf("age" to A2uiNumberSchema()),
                        required = setOf("age"),
                    ),
                )
            )
        assertThat(validator.validateSchema(mapOf("name" to "Alice", "age" to 30), schema)).isTrue()
    }

    @Test
    fun validateSchema_allOfTypeMissingOne_throwsValidationException() {
        val schema =
            A2uiAllOfSchema(
                listOf(
                    A2uiObjectSchema(
                        properties = mapOf("name" to A2uiStringSchema()),
                        required = setOf("name"),
                    ),
                    A2uiObjectSchema(
                        properties = mapOf("age" to A2uiNumberSchema()),
                        required = setOf("age"),
                    ),
                )
            )
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(mapOf("name" to "Alice"), schema) // Missing age
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_anyOfTypeMatchingAtLeastOne_returnsTrue() {
        val schema = A2uiAnyOfSchema(listOf(A2uiStringSchema(), A2uiNumberSchema()))
        assertThat(validator.validateSchema("hello", schema)).isTrue() // Matches String
        assertThat(validator.validateSchema(123, schema)).isTrue() // Matches Number
    }

    @Test
    fun validateSchema_anyOfTypeMatchingNone_throwsValidationException() {
        val schema = A2uiAnyOfSchema(listOf(A2uiStringSchema(), A2uiNumberSchema()))
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(true, schema) // Boolean matches neither
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_constTypeWithMatchingStringValue_returnsTrue() {
        val schema = A2uiConstSchema("a")
        assertThat(validator.validateSchema("a", schema)).isTrue()
    }

    @Test
    fun validateSchema_constTypeWithNonMatchingStringValue_throwsValidationException() {
        val schema = A2uiConstSchema("a")
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema("b", schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_constTypeWithMatchingNumberValue_returnsTrue() {
        val schema = A2uiConstSchema(123)
        assertThat(validator.validateSchema(123, schema)).isTrue()
    }

    @Test
    fun validateSchema_constTypeWithNonMatchingNumberValue_throwsValidationException() {
        val schema = A2uiConstSchema(123)
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(456, schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_constTypeWithMatchingBooleanValue_returnsTrue() {
        val schema = A2uiConstSchema(true)
        assertThat(validator.validateSchema(true, schema)).isTrue()
    }

    @Test
    fun validateSchema_constTypeWithNonMatchingBooleanValue_throwsValidationException() {
        val schema = A2uiConstSchema(true)
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(false, schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_constTypeWithMatchingListValue_returnsTrue() {
        val schema = A2uiConstSchema(listOf("a", true, 3))
        assertThat(validator.validateSchema(listOf("a", true, 3), schema)).isTrue()
    }

    @Test
    fun validateSchema_constTypeWithNonMatchingListValue_throwsValidationException() {
        val schema = A2uiConstSchema(listOf("a", "b", 3))
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(listOf("a", "c", 3), schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_constTypeWithMatchingMapValue_returnsTrue() {
        val schema = A2uiConstSchema(mapOf("a" to "b", "c" to 1))
        assertThat(validator.validateSchema(mapOf("a" to "b", "c" to 1), schema)).isTrue()
    }

    @Test
    fun validateSchema_constTypeWithNonMatchingMapValue_throwsValidationException() {
        val schema = A2uiConstSchema(mapOf("a" to "b"))
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(mapOf("a" to "c"), schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_constTypeWithMatchingNestedListValue_returnsTrue() {
        val schema = A2uiConstSchema(listOf(listOf("a", "b"), listOf(1, 2)))
        assertThat(validator.validateSchema(listOf(listOf("a", "b"), listOf(1, 2)), schema))
            .isTrue()
    }

    @Test
    fun validateSchema_constTypeWithNonMatchingNestedListValue_throwsValidationException() {
        val schema = A2uiConstSchema(listOf(listOf("a", "b"), listOf(1, 2)))
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(listOf(listOf("a", "c"), listOf(1, 2)), schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_constTypeWithMatchingNestedMapValue_returnsTrue() {
        val schema = A2uiConstSchema(mapOf("outer" to mapOf("inner" to "value")))
        assertThat(validator.validateSchema(mapOf("outer" to mapOf("inner" to "value")), schema))
            .isTrue()
    }

    @Test
    fun validateSchema_constTypeWithNonMatchingNestedMapValue_throwsValidationException() {
        val schema = A2uiConstSchema(mapOf("outer" to mapOf("inner" to "value")))
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(mapOf("outer" to mapOf("inner" to "wrong")), schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_refTypeUnsupported_throwsValidationException() {
        val schema = A2uiRefSchema("some_ref")
        // Ref validation is not supported and always throws.
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema("anything", schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_refTypeAnyFunctionWithUnknownFunction_throwsValidationException() {
        val schema = A2uiRefSchema("catalog.json#/\$defs/anyFunction")
        val customValidator = A2uiCoreSchemaValidator(createTestCatalog())
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                customValidator.validateSchema(mapOf("call" to "unknownFunction"), schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_refTypeAnyFunctionMissingCall_throwsValidationException() {
        val schema = A2uiRefSchema("catalog.json#/\$defs/anyFunction")
        val customValidator = A2uiCoreSchemaValidator(createTestCatalog())

        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                customValidator.validateSchema(mapOf("other" to "value"), schema)
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_refTypeAnyFunctionWithValidPayload_returnsTrue() {
        val schema = A2uiRefSchema("catalog.json#/\$defs/anyFunction")
        val catalog =
            createTestCatalog(
                listOf(
                    object : A2uiFunctionDefinition {
                        override val name = "myFunction"
                        override val description = "test"
                        override val argumentSchema =
                            A2uiObjectSchema(
                                properties = mapOf("myArg" to A2uiStringSchema()),
                                required = setOf("myArg"),
                            )
                        override val returnType = A2uiFunctionReturnType.STRING
                    }
                )
            )
        val customValidator = A2uiCoreSchemaValidator(catalog)
        assertThat(
                customValidator.validateSchema(
                    mapOf(
                        "call" to "myFunction",
                        "args" to mapOf("myArg" to "value"),
                        "returnType" to "string",
                    ),
                    schema,
                )
            )
            .isTrue()
    }

    @Test
    fun validateSchema_refTypeAnyFunctionWithInvalidArgs_throwsValidationException() {
        val schema = A2uiRefSchema("catalog.json#/\$defs/anyFunction")
        val catalog =
            createTestCatalog(
                listOf(
                    object : A2uiFunctionDefinition {
                        override val name = "myFunction"
                        override val description = "test"
                        override val argumentSchema =
                            A2uiObjectSchema(
                                properties = mapOf("myArg" to A2uiStringSchema()),
                                required = setOf("myArg"),
                            )
                        override val returnType = A2uiFunctionReturnType.STRING
                    }
                )
            )
        val customValidator = A2uiCoreSchemaValidator(catalog)
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                customValidator.validateSchema(
                    mapOf("call" to "myFunction", "args" to mapOf("myArg" to 123)),
                    schema,
                )
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_refTypeAnyFunctionWithInvalidReturnType_throwsValidationException() {
        val schema = A2uiRefSchema("catalog.json#/\$defs/anyFunction")
        val catalog =
            createTestCatalog(
                listOf(
                    object : A2uiFunctionDefinition {
                        override val name = "myFunction"
                        override val description = "test"
                        override val argumentSchema = A2uiObjectSchema()
                        override val returnType = A2uiFunctionReturnType.NUMBER
                    }
                )
            )
        val customValidator = A2uiCoreSchemaValidator(catalog)
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                customValidator.validateSchema(
                    mapOf("call" to "myFunction", "returnType" to "string"),
                    schema,
                )
            }
        assertThat(ex.context["path"]).isEqualTo("$")
    }

    @Test
    fun validateSchema_CompositeSchemaWithMatchingValue_returnsTrue() {
        val composite =
            object : A2uiCompositeSchema() {
                override val description: String? = null

                override fun getDefinition(): A2uiSchema {
                    return A2uiObjectSchema(
                        properties = mapOf("id" to A2uiNumberSchema()),
                        required = setOf("id"),
                    )
                }
            }
        assertThat(validator.validateSchema(mapOf("id" to 123), composite)).isTrue()
    }

    @Test
    fun validateSchema_CompositeSchemaWithNonMatchingValue_throwsValidationException() {
        val composite =
            object : A2uiCompositeSchema() {
                override val description: String? = null

                override fun getDefinition(): A2uiSchema {
                    return A2uiObjectSchema(
                        properties = mapOf("id" to A2uiNumberSchema()),
                        required = setOf("id"),
                    )
                }
            }
        val ex =
            assertFailsWith<A2uiException.A2uiValidationException> {
                validator.validateSchema(mapOf("id" to "abc"), composite)
            }
        assertThat(ex.context["path"]).isEqualTo("$.id")
    }

    private fun createTestCatalog(
        functionsList: List<A2uiFunctionDefinition> = emptyList()
    ): A2uiCoreCatalog {
        val wrappedFunctions =
            functionsList.map { def ->
                object : A2uiFunction {
                    override val definition = def

                    override fun execute(
                        args: Map<String, Any>,
                        executionContext: A2uiExecutionContext,
                    ) = null
                }
            }
        return object : A2uiCoreCatalog {
            override val id = "test"
            override val components = emptyList<A2uiCoreComponentDefinition>()
            override val functions = wrappedFunctions
            override val themeSchema: A2uiSchema? = null

            override fun getComponent(name: String) = null

            override fun getFunction(name: String): A2uiFunction? {
                return functions.find { it.definition.name == name }
            }
        }
    }

    companion object {
        private val OBJECT_SCHEMA =
            A2uiObjectSchema(
                properties = mapOf("name" to A2uiStringSchema(), "age" to A2uiNumberSchema()),
                required = setOf("name"),
            )
    }
}
