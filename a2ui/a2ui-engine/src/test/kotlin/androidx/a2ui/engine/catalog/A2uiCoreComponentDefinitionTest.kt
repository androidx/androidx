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

import androidx.a2ui.model.schema.A2uiAllOfSchema
import androidx.a2ui.model.schema.A2uiConstSchema
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiRefSchema
import androidx.a2ui.model.schema.A2uiSchema
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
                        KEY_COMPONENT to A2uiConstSchema(TEST_NAME),
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
                    A2uiAllOfSchema(
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
            A2uiAllOfSchema(
                description = TEST_DESCRIPTION,
                schemas =
                    listOf(
                        A2uiRefSchema(TEST_REF_PATH),
                        A2uiObjectSchema(
                            properties =
                                mapOf(
                                    KEY_COMPONENT to A2uiConstSchema(TEST_NAME),
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
                                KEY_COMPONENT to A2uiConstSchema(TEST_NAME),
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
                        KEY_COMPONENT to A2uiConstSchema(TEST_NAME),
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
                                KEY_COMPONENT to A2uiConstSchema(TEST_NAME),
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
                        KEY_COMPONENT to A2uiConstSchema(TEST_NAME),
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
                        properties = mapOf(KEY_COMPONENT to A2uiConstSchema(NAME_MISMATCHED)),
                        required = setOf(KEY_COMPONENT),
                    )
            )

        assertThrows(IllegalArgumentException::class.java) { testComponent.toSchema() }
    }

    @Test
    fun toSchema_withNestedAllOf_appendsDiscriminatorAtTopLevel() {
        val nestedAllOf = A2uiAllOfSchema(schemas = listOf(A2uiRefSchema(TEST_REF_PATH)))
        val testComponent =
            createComponentDefinition(
                propertySchema = A2uiAllOfSchema(schemas = listOf(nestedAllOf))
            )

        val expectedSchema =
            A2uiAllOfSchema(
                description = TEST_DESCRIPTION,
                schemas =
                    listOf(
                        nestedAllOf,
                        A2uiObjectSchema(
                            properties = mapOf(KEY_COMPONENT to A2uiConstSchema(TEST_NAME)),
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
            createComponentDefinition(
                propertySchema = A2uiAllOfSchema(schemas = listOf(obj1, obj2))
            )

        val expectedSchema =
            A2uiAllOfSchema(
                description = TEST_DESCRIPTION,
                schemas =
                    listOf(
                        obj1,
                        obj2,
                        A2uiObjectSchema(
                            properties = mapOf(KEY_COMPONENT to A2uiConstSchema(TEST_NAME)),
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
                        KEY_COMPONENT to A2uiConstSchema(TEST_NAME),
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
                        KEY_COMPONENT to A2uiConstSchema(TEST_NAME),
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
    }
}
