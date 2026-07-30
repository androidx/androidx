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

import androidx.a2ui.model.catalog.toSchema
import androidx.a2ui.model.schema.A2uiAllOfSchema
import androidx.a2ui.model.schema.A2uiAnyOfSchema
import androidx.a2ui.model.schema.A2uiArraySchema
import androidx.a2ui.model.schema.A2uiCompositeSchema
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiOneOfSchema
import androidx.a2ui.model.schema.A2uiSchema
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Serializes an [A2uiCoreCatalog] into a JSON Schema string conforming to the A2UI v0.9.1 catalog
 * specification.
 */
internal fun serializeCatalogToJsonSchema(catalog: A2uiCoreCatalog): String =
    buildJsonObject {
            put("\$schema", "https://json-schema.org/draft/2020-12/schema")
            put("\$id", catalog.id)
            catalog.title?.let { put("title", it) }
            catalog.description?.let { put("description", it) }
            put("catalogId", catalog.id)

            put(
                "components",
                buildJsonObject {
                    for (component in catalog.componentDefinitions) {
                        put(component.name, component.toSchema().toJsonElement())
                    }
                },
            )

            put(
                "functions",
                buildJsonObject {
                    for (function in catalog.functions) {
                        put(
                            function.definition.name,
                            function.definition.toSchema().toJsonElement(),
                        )
                    }
                },
            )

            put(
                "\$defs",
                buildJsonObject {
                    for ((defName, defSchema) in collectLocalDefinitionsFromCatalog(catalog)) {
                        put(defName, defSchema.toJsonElement())
                    }

                    catalog.themeSchema?.let { put("theme", it.toJsonElement()) }

                    put(
                        "anyComponent",
                        buildJsonObject {
                            put(
                                "oneOf",
                                JsonArray(
                                    catalog.componentDefinitions.map { comp ->
                                        buildJsonObject {
                                            put("\$ref", "#/components/${comp.name}")
                                        }
                                    }
                                ),
                            )
                            put(
                                "discriminator",
                                buildJsonObject { put("propertyName", "component") },
                            )
                        },
                    )

                    put(
                        "anyFunction",
                        buildJsonObject {
                            put(
                                "oneOf",
                                JsonArray(
                                    catalog.functions.map { func ->
                                        buildJsonObject {
                                            put("\$ref", "#/functions/${func.definition.name}")
                                        }
                                    }
                                ),
                            )
                        },
                    )
                },
            )
        }
        .toString()

/**
 * Traverses the catalog to collect all local schema definitions.
 *
 * A local definition is a reusable subschema declared inside the document's top-level `$defs` map.
 * We extract the definitions of `A2uiCompositeSchema` that have no schemaId (and by that are
 * considered local), and collect them into this section of the schema.
 */
private fun collectLocalDefinitionsFromCatalog(catalog: A2uiCoreCatalog): Map<String, A2uiSchema> {
    val localDefs = mutableMapOf<String, A2uiSchema>()
    val visited = mutableSetOf<A2uiSchema>()
    for (component in catalog.componentDefinitions) {
        collectLocalDefinitionsFromSchema(component.propertySchema, localDefs, visited)
    }
    for (function in catalog.functions) {
        collectLocalDefinitionsFromSchema(function.definition.argumentSchema, localDefs, visited)
    }
    catalog.themeSchema?.let { collectLocalDefinitionsFromSchema(it, localDefs, visited) }
    return localDefs
}

private fun collectLocalDefinitionsFromSchema(
    schema: A2uiSchema,
    result: MutableMap<String, A2uiSchema>,
    visited: MutableSet<A2uiSchema>,
): Map<String, A2uiSchema> {
    if (!visited.add(schema)) return result

    if (schema is A2uiCompositeSchema) {
        if (schema.schemaId == null) {
            val defName = schema.definitionName
            if (defName != null) {
                result.putIfAbsent(defName, schema.getDefinition())
            }
            collectLocalDefinitionsFromSchema(schema.getDefinition(), result, visited)
        }
        return result
    }

    when (schema) {
        is A2uiObjectSchema -> {
            for (propSchema in schema.properties.values) {
                collectLocalDefinitionsFromSchema(propSchema, result, visited)
            }
            schema.additionalPropertiesSchema?.let {
                collectLocalDefinitionsFromSchema(it, result, visited)
            }
        }
        is A2uiArraySchema -> {
            schema.items?.let { collectLocalDefinitionsFromSchema(it, result, visited) }
        }
        is A2uiOneOfSchema -> {
            for (subSchema in schema.schemas) {
                collectLocalDefinitionsFromSchema(subSchema, result, visited)
            }
        }
        is A2uiAllOfSchema -> {
            for (subSchema in schema.schemas) {
                collectLocalDefinitionsFromSchema(subSchema, result, visited)
            }
        }
        is A2uiAnyOfSchema -> {
            for (subSchema in schema.schemas) {
                collectLocalDefinitionsFromSchema(subSchema, result, visited)
            }
        }
        else -> {}
    }

    return result
}
