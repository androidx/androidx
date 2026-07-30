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

@file:JvmName("A2uiCoreComponentDefinitionSerializerKt")

package androidx.a2ui.engine.catalog

import androidx.a2ui.model.schema.A2uiAllOfSchema
import androidx.a2ui.model.schema.A2uiConstSchema
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiSchema

/**
 * Converts a component definition into an [A2uiSchema].
 *
 * @param componentDefinition the component definition to serialize
 * @return the [A2uiSchema] representation of the component definition with discriminator
 */
internal fun serializeComponentDefinitionToSchema(
    componentDefinition: A2uiCoreComponentDefinition
): A2uiSchema {
    val rawSchema = componentDefinition.propertySchema
    val componentName = componentDefinition.name
    val description = componentDefinition.description

    return when (rawSchema) {
        is A2uiObjectSchema -> injectComponentMetadata(rawSchema, componentName, description)
        is A2uiAllOfSchema -> injectComponentMetadata(rawSchema, componentName, description)
        else ->
            throw IllegalArgumentException(
                "Unexpected schema type '${rawSchema::class.java.simpleName}'. " +
                    "Top-level component schema must be A2uiObjectSchema or A2uiAllOfSchema."
            )
    }
}

private fun injectComponentMetadata(
    schema: A2uiObjectSchema,
    componentName: String,
    componentDescription: String,
): A2uiObjectSchema {
    val targetDescription = resolveDescription(schema.description, componentDescription)
    return injectObjectDiscriminator(schema, componentName, targetDescription)
}

private fun injectObjectDiscriminator(
    schema: A2uiObjectSchema,
    componentName: String,
    description: String? = schema.description,
): A2uiObjectSchema {
    val existingComponentProperty = schema.properties["component"]
    if (existingComponentProperty != null) {
        require(
            existingComponentProperty is A2uiConstSchema &&
                existingComponentProperty.value == componentName
        ) {
            "Existing 'component' property const '${(existingComponentProperty as? A2uiConstSchema)?.value}' " +
                "does not match expected component name '$componentName'"
        }
        if (schema.description == description && schema.required.contains("component")) {
            return schema
        }
        return A2uiObjectSchema(
            description = description,
            properties = schema.properties,
            required =
                if (schema.required.contains("component")) schema.required
                else setOf("component") + schema.required,
            additionalPropertiesSchema = schema.additionalPropertiesSchema,
            isAdditionalPropertiesAllowed = schema.isAdditionalPropertiesAllowed,
        )
    }
    val updatedProperties = mapOf("component" to A2uiConstSchema(componentName)) + schema.properties
    val updatedRequired = setOf("component") + schema.required
    return A2uiObjectSchema(
        description = description,
        properties = updatedProperties,
        required = updatedRequired,
        additionalPropertiesSchema = schema.additionalPropertiesSchema,
        isAdditionalPropertiesAllowed = schema.isAdditionalPropertiesAllowed,
    )
}

private fun injectComponentMetadata(
    schema: A2uiAllOfSchema,
    componentName: String,
    componentDescription: String,
): A2uiAllOfSchema {
    val targetDescription = resolveDescription(schema.description, componentDescription)
    val objectSchemasCount = schema.schemas.count { it is A2uiObjectSchema }
    val updatedSubSchemas =
        if (objectSchemasCount == 1) {
            schema.schemas.map { subSchema ->
                if (subSchema is A2uiObjectSchema) {
                    injectObjectDiscriminator(subSchema, componentName)
                } else {
                    subSchema
                }
            }
        } else {
            val discriminatorObjectSchema =
                A2uiObjectSchema(
                    properties = mapOf("component" to A2uiConstSchema(componentName)),
                    required = setOf("component"),
                )
            schema.schemas + discriminatorObjectSchema
        }
    return A2uiAllOfSchema(description = targetDescription, schemas = updatedSubSchemas)
}

private fun resolveDescription(
    existingDescription: String?,
    componentDescription: String,
): String? {
    if (existingDescription != null) {
        require(existingDescription == componentDescription) {
            "Existing description '$existingDescription' does not match expected component description '$componentDescription'"
        }
        return existingDescription
    }
    return componentDescription.ifBlank { null }
}
