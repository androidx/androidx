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

package androidx.a2ui.engine.catalog

import androidx.a2ui.model.schema.A2uiAnySchema
import androidx.a2ui.model.schema.A2uiCompositeSchema
import androidx.a2ui.model.schema.A2uiNumberSchema
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiRefSchema
import androidx.a2ui.model.schema.A2uiSchema
import androidx.a2ui.model.schema.A2uiSchemaKeyword
import androidx.a2ui.model.schema.A2uiStringSchema
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class A2uiCoreComponentDefinitionTest {

    @Test
    fun toSchema_withObjectSchema_injectsComponentDiscriminatorAndDescription() {
        val testComponent =
            createComponentDefinition(
                propertySchema =
                    A2uiObjectSchema(
                        properties = mapOf(TEST_PROPERTY_1 to A2uiStringSchema()),
                        required = setOf(TEST_PROPERTY_1),
                    )
            )

        val expectedSchema =
            A2uiObjectSchema(
                description = TEST_DESCRIPTION,
                properties =
                    mapOf(
                        KEY_COMPONENT to constSchema(TEST_NAME),
                        TEST_PROPERTY_1 to A2uiStringSchema(),
                    ),
                required = setOf(KEY_COMPONENT, TEST_PROPERTY_1),
            )

        assertThat(testComponent.toSchema()).isEqualTo(expectedSchema)
    }

    @Test
    fun toSchema_withAllOfSchemaAndSingleSubObjectSchema_injectsComponentDiscriminatorIntoObjectSchema() {
        val testComponent =
            createComponentDefinition(
                propertySchema =
                    allOfSchema(
                        schemas =
                            listOf(
                                A2uiRefSchema(TEST_REF_PATH),
                                A2uiObjectSchema(
                                    properties = mapOf(TEST_PROPERTY_1 to A2uiStringSchema()),
                                    required = setOf(TEST_PROPERTY_1),
                                ),
                            )
                    )
            )

        val expectedSchema =
            allOfSchema(
                description = TEST_DESCRIPTION,
                schemas =
                    listOf(
                        A2uiRefSchema(TEST_REF_PATH),
                        A2uiObjectSchema(
                            properties =
                                mapOf(
                                    KEY_COMPONENT to constSchema(TEST_NAME),
                                    TEST_PROPERTY_1 to A2uiStringSchema(),
                                ),
                            required = setOf(KEY_COMPONENT, TEST_PROPERTY_1),
                        ),
                    ),
            )

        assertThat(testComponent.toSchema()).isEqualTo(expectedSchema)
    }

    @Test
    fun toSchema_withExistingDiscriminatorNotInRequired_injectsDiscriminatorIntoRequired() {
        val testComponent =
            createComponentDefinition(
                propertySchema =
                    A2uiObjectSchema(
                        properties =
                            mapOf(
                                KEY_COMPONENT to constSchema(TEST_NAME),
                                TEST_PROPERTY_1 to A2uiStringSchema(),
                            ),
                        required = setOf(TEST_PROPERTY_1),
                    )
            )

        val expectedSchema =
            A2uiObjectSchema(
                description = TEST_DESCRIPTION,
                properties =
                    mapOf(
                        KEY_COMPONENT to constSchema(TEST_NAME),
                        TEST_PROPERTY_1 to A2uiStringSchema(),
                    ),
                required = setOf(KEY_COMPONENT, TEST_PROPERTY_1),
            )

        assertThat(testComponent.toSchema()).isEqualTo(expectedSchema)
    }

    @Test
    fun toSchema_withExistingDiscriminatorAlreadyInRequired_succeeds() {
        val testComponent =
            createComponentDefinition(
                propertySchema =
                    A2uiObjectSchema(
                        properties =
                            mapOf(
                                KEY_COMPONENT to constSchema(TEST_NAME),
                                TEST_PROPERTY_1 to A2uiStringSchema(),
                            ),
                        required = setOf(KEY_COMPONENT, TEST_PROPERTY_1),
                    )
            )

        val expectedSchema =
            A2uiObjectSchema(
                description = TEST_DESCRIPTION,
                properties =
                    mapOf(
                        KEY_COMPONENT to constSchema(TEST_NAME),
                        TEST_PROPERTY_1 to A2uiStringSchema(),
                    ),
                required = setOf(KEY_COMPONENT, TEST_PROPERTY_1),
            )

        assertThat(testComponent.toSchema()).isEqualTo(expectedSchema)
    }

    @Test
    fun toSchema_withMismatchedDiscriminator_throwsException() {
        val testComponent =
            createComponentDefinition(
                propertySchema =
                    A2uiObjectSchema(
                        properties = mapOf(KEY_COMPONENT to constSchema(NAME_MISMATCHED)),
                        required = setOf(KEY_COMPONENT),
                    )
            )

        assertThrows(IllegalArgumentException::class.java) { testComponent.toSchema() }
    }

    @Test
    fun toSchema_withNestedAllOf_appendsDiscriminatorAtTopLevel() {
        val nestedAllOf = allOfSchema(schemas = listOf(A2uiRefSchema(TEST_REF_PATH)))
        val testComponent =
            createComponentDefinition(propertySchema = allOfSchema(schemas = listOf(nestedAllOf)))

        val expectedSchema =
            allOfSchema(
                description = TEST_DESCRIPTION,
                schemas =
                    listOf(
                        nestedAllOf,
                        A2uiObjectSchema(
                            properties = mapOf(KEY_COMPONENT to constSchema(TEST_NAME)),
                            required = setOf(KEY_COMPONENT),
                        ),
                    ),
            )

        assertThat(testComponent.toSchema()).isEqualTo(expectedSchema)
    }

    @Test
    fun toSchema_withMultipleObjectSchemas_appendsDiscriminatorSchema() {
        val obj1 = A2uiObjectSchema(properties = mapOf(TEST_PROPERTY_1 to A2uiStringSchema()))
        val obj2 = A2uiObjectSchema(properties = mapOf(TEST_PROPERTY_2 to A2uiStringSchema()))
        val testComponent =
            createComponentDefinition(propertySchema = allOfSchema(schemas = listOf(obj1, obj2)))

        val expectedSchema =
            allOfSchema(
                description = TEST_DESCRIPTION,
                schemas =
                    listOf(
                        obj1,
                        obj2,
                        A2uiObjectSchema(
                            properties = mapOf(KEY_COMPONENT to constSchema(TEST_NAME)),
                            required = setOf(KEY_COMPONENT),
                        ),
                    ),
            )

        assertThat(testComponent.toSchema()).isEqualTo(expectedSchema)
    }

    @Test
    fun toSchema_withPrimitivePropertySchema_throwsException() {
        val testComponent = createComponentDefinition(propertySchema = A2uiStringSchema())

        assertThrows(IllegalArgumentException::class.java) { testComponent.toSchema() }
    }

    @Test
    fun toSchema_withNonConstComponentProperty_throwsException() {
        val testComponent =
            createComponentDefinition(
                propertySchema =
                    A2uiObjectSchema(
                        properties = mapOf(KEY_COMPONENT to A2uiStringSchema()),
                        required = setOf(KEY_COMPONENT),
                    )
            )

        assertThrows(IllegalArgumentException::class.java) { testComponent.toSchema() }
    }

    @Test
    fun toSchema_withEmptyDescription_doesNotInjectDescription() {
        val testComponent =
            createComponentDefinition(
                propertySchema =
                    A2uiObjectSchema(
                        properties = mapOf(TEST_PROPERTY_1 to A2uiStringSchema()),
                        required = setOf(TEST_PROPERTY_1),
                    ),
                description = "",
            )

        val expectedSchema =
            A2uiObjectSchema(
                description = null,
                properties =
                    mapOf(
                        KEY_COMPONENT to constSchema(TEST_NAME),
                        TEST_PROPERTY_1 to A2uiStringSchema(),
                    ),
                required = setOf(KEY_COMPONENT, TEST_PROPERTY_1),
            )

        assertThat(testComponent.toSchema()).isEqualTo(expectedSchema)
    }

    @Test
    fun toSchema_withExistingMatchingDescription_succeeds() {
        val testComponent =
            createComponentDefinition(
                propertySchema =
                    A2uiObjectSchema(
                        description = TEST_DESCRIPTION,
                        properties = mapOf(TEST_PROPERTY_1 to A2uiStringSchema()),
                        required = setOf(TEST_PROPERTY_1),
                    )
            )

        val expectedSchema =
            A2uiObjectSchema(
                description = TEST_DESCRIPTION,
                properties =
                    mapOf(
                        KEY_COMPONENT to constSchema(TEST_NAME),
                        TEST_PROPERTY_1 to A2uiStringSchema(),
                    ),
                required = setOf(KEY_COMPONENT, TEST_PROPERTY_1),
            )

        assertThat(testComponent.toSchema()).isEqualTo(expectedSchema)
    }

    @Test
    fun toSchema_withExistingMismatchedDescription_throwsException() {
        val testComponent =
            createComponentDefinition(
                propertySchema =
                    A2uiObjectSchema(
                        description = "Mismatched Description",
                        properties = mapOf(TEST_PROPERTY_1 to A2uiStringSchema()),
                        required = setOf(TEST_PROPERTY_1),
                    )
            )

        assertThrows(IllegalArgumentException::class.java) { testComponent.toSchema() }
    }

    @Test
    fun toSchema_withCompositeSchemaResolvingToObject_wrapsInAllOfWithDiscriminator() {
        val compositeSchema =
            object : A2uiCompositeSchema() {
                override val description: String? = null
                override val definitionName: String = "TestComposite"

                override fun getDefinition(): A2uiSchema =
                    A2uiObjectSchema(
                        properties = mapOf(TEST_PROPERTY_1 to A2uiStringSchema()),
                        required = setOf(TEST_PROPERTY_1),
                    )
            }
        val testComponent = createComponentDefinition(propertySchema = compositeSchema)

        val expectedSchema =
            A2uiObjectSchema(
                description = TEST_DESCRIPTION,
                keywords =
                    listOf(
                        A2uiSchemaKeyword.AllOf(
                            listOf(
                                compositeSchema,
                                A2uiObjectSchema(
                                    properties = mapOf(KEY_COMPONENT to constSchema(TEST_NAME)),
                                    required = setOf(KEY_COMPONENT),
                                ),
                            )
                        )
                    ),
            )

        assertThat(testComponent.toSchema()).isEqualTo(expectedSchema)
    }

    @Test
    fun toSchema_withCompositeSchemaResolvingToPrimitive_throwsException() {
        val compositeSchema =
            object : A2uiCompositeSchema() {
                override val description: String? = null
                override val definitionName: String = "TestComposite"

                override fun getDefinition(): A2uiSchema = A2uiStringSchema()
            }
        val testComponent = createComponentDefinition(propertySchema = compositeSchema)

        assertThrows(IllegalArgumentException::class.java) { testComponent.toSchema() }
    }

    @Test
    fun toSchema_withAnySchemaHavingOneOf_wrapsInAllOfWithDiscriminator() {
        val obj1 = A2uiObjectSchema(properties = mapOf(TEST_PROPERTY_1 to A2uiStringSchema()))
        val obj2 = A2uiObjectSchema(properties = mapOf(TEST_PROPERTY_2 to A2uiStringSchema()))
        val oneOfSchema =
            A2uiAnySchema(keywords = listOf(A2uiSchemaKeyword.OneOf(listOf(obj1, obj2))))
        val testComponent = createComponentDefinition(propertySchema = oneOfSchema)

        val expectedSchema =
            A2uiObjectSchema(
                description = TEST_DESCRIPTION,
                keywords =
                    listOf(
                        A2uiSchemaKeyword.AllOf(
                            listOf(
                                oneOfSchema,
                                A2uiObjectSchema(
                                    properties = mapOf(KEY_COMPONENT to constSchema(TEST_NAME)),
                                    required = setOf(KEY_COMPONENT),
                                ),
                            )
                        )
                    ),
            )

        assertThat(testComponent.toSchema()).isEqualTo(expectedSchema)
    }

    @Test
    fun toSchema_withCompositeSchemaResolvingToOneOf_wrapsInAllOfWithDiscriminator() {
        val obj1 = A2uiObjectSchema(properties = mapOf(TEST_PROPERTY_1 to A2uiStringSchema()))
        val obj2 = A2uiObjectSchema(properties = mapOf(TEST_PROPERTY_2 to A2uiStringSchema()))
        val compositeSchema =
            object : A2uiCompositeSchema() {
                override val description: String? = null
                override val definitionName: String = "TestOneOfComposite"

                override fun getDefinition(): A2uiSchema =
                    A2uiAnySchema(keywords = listOf(A2uiSchemaKeyword.OneOf(listOf(obj1, obj2))))
            }
        val testComponent = createComponentDefinition(propertySchema = compositeSchema)

        val expectedSchema =
            A2uiObjectSchema(
                description = TEST_DESCRIPTION,
                keywords =
                    listOf(
                        A2uiSchemaKeyword.AllOf(
                            listOf(
                                compositeSchema,
                                A2uiObjectSchema(
                                    properties = mapOf(KEY_COMPONENT to constSchema(TEST_NAME)),
                                    required = setOf(KEY_COMPONENT),
                                ),
                            )
                        )
                    ),
            )

        assertThat(testComponent.toSchema()).isEqualTo(expectedSchema)
    }

    @Test
    fun toSchema_withOneOfSchemaContainingPrimitive_wrapsInAllOfWithDiscriminator() {
        val obj1 = A2uiObjectSchema(properties = mapOf(TEST_PROPERTY_1 to A2uiStringSchema()))
        val oneOfSchema =
            A2uiAnySchema(
                keywords = listOf(A2uiSchemaKeyword.OneOf(listOf(obj1, A2uiStringSchema())))
            )
        val testComponent = createComponentDefinition(propertySchema = oneOfSchema)

        val expectedSchema =
            A2uiObjectSchema(
                description = TEST_DESCRIPTION,
                keywords =
                    listOf(
                        A2uiSchemaKeyword.AllOf(
                            listOf(
                                oneOfSchema,
                                A2uiObjectSchema(
                                    properties = mapOf(KEY_COMPONENT to constSchema(TEST_NAME)),
                                    required = setOf(KEY_COMPONENT),
                                ),
                            )
                        )
                    ),
            )

        assertThat(testComponent.toSchema()).isEqualTo(expectedSchema)
    }

    @Test
    fun toSchema_withOneOfSchemaContainingOnlyPrimitives_throwsException() {
        val oneOfSchema =
            A2uiAnySchema(
                keywords =
                    listOf(A2uiSchemaKeyword.OneOf(listOf(A2uiStringSchema(), A2uiNumberSchema())))
            )
        val testComponent = createComponentDefinition(propertySchema = oneOfSchema)

        assertThrows(IllegalArgumentException::class.java) { testComponent.toSchema() }
    }

    @Test
    fun toSchema_withRefSchema_wrapsInAllOfWithDiscriminator() {
        val refSchema = A2uiRefSchema(TEST_REF_PATH)
        val testComponent = createComponentDefinition(propertySchema = refSchema)

        val expectedSchema =
            A2uiObjectSchema(
                description = TEST_DESCRIPTION,
                keywords =
                    listOf(
                        A2uiSchemaKeyword.AllOf(
                            listOf(
                                refSchema,
                                A2uiObjectSchema(
                                    properties = mapOf(KEY_COMPONENT to constSchema(TEST_NAME)),
                                    required = setOf(KEY_COMPONENT),
                                ),
                            )
                        )
                    ),
            )

        assertThat(testComponent.toSchema()).isEqualTo(expectedSchema)
    }

    private fun createComponentDefinition(
        propertySchema: A2uiSchema,
        name: String = TEST_NAME,
        description: String = TEST_DESCRIPTION,
    ): A2uiCoreComponentDefinition =
        object : A2uiCoreComponentDefinition {
            override val name: String = name
            override val description: String = description
            override val propertySchema: A2uiSchema = propertySchema
        }

    companion object {
        private const val KEY_COMPONENT = "component"

        private const val TEST_NAME = "TestComponent"
        private const val TEST_DESCRIPTION = "Test description"
        private const val NAME_MISMATCHED = "MismatchedComponent"
        private const val TEST_REF_PATH = "test/ref/path"

        private const val TEST_PROPERTY_1 = "prop1"
        private const val TEST_PROPERTY_2 = "prop2"

        private fun constSchema(value: String): A2uiSchema =
            A2uiStringSchema(keywords = listOf(A2uiSchemaKeyword.Const(value)))

        private fun allOfSchema(
            schemas: List<A2uiSchema>,
            description: String? = null,
        ): A2uiSchema =
            A2uiAnySchema(
                description = description,
                keywords = listOf(A2uiSchemaKeyword.AllOf(schemas)),
            )
    }
}
