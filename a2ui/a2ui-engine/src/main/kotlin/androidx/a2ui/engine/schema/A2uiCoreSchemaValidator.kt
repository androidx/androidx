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
import androidx.a2ui.model.protocol.A2uiException
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

/**
 * A stateless utility for validating raw JSON against A2UI Schema definitions. This class ensures
 * that incoming JSON payloads from the agent structurally conform to the expected UI component
 * schemas *before* they are allowed into the reactive data models.
 */
internal class A2uiCoreSchemaValidator(catalog: A2uiCoreCatalog? = null) {

    // TODO(nimrodp): remove this once functions expose their own schema
    private val anyFunctionSchema: A2uiSchema =
        A2uiOneOfSchema(
            schemas =
                (catalog?.functions ?: emptyList()).map { function ->
                    A2uiObjectSchema(
                        properties =
                            mapOf(
                                "call" to A2uiConstSchema(function.definition.name),
                                "args" to function.definition.argumentSchema,
                                "returnType" to
                                    A2uiConstSchema(function.definition.returnType.value),
                            ),
                        required = setOf("call"),
                        isAdditionalPropertiesAllowed = true,
                    )
                }
        )

    /**
     * Validates that the given payload conforms to the provided schema.
     *
     * @param payload The raw payload to validate (e.g., a component properties).
     * @param schema The schema defining the expected structure.
     * @param basePath The base path to use for error reporting (defaults to "$").
     * @throws A2uiException.A2uiValidationException if the payload violates the schema rules or
     *   JSON is malformed.
     */
    // TODO(b/535051800): Fix validation error path format
    internal fun validateSchema(payload: Any?, schema: A2uiSchema, basePath: String = "$") {
        validateSchemaInternal(payload, schema, basePath)
    }

    /**
     * Validates that the given payload conforms to the provided schema. Use [path] to track the
     * location of the subschema within the root schema for accurate errors.
     */
    private fun validateSchemaInternal(payload: Any?, schema: A2uiSchema, path: String = "$") {
        when (schema) {
            is A2uiObjectSchema -> validateObject(payload, schema, path)
            is A2uiStringSchema -> validateString(payload, path)
            is A2uiNumberSchema -> validateNumber(payload, path)
            is A2uiBooleanSchema -> validateBoolean(payload, path)
            is A2uiArraySchema -> validateArray(payload, schema, path)
            is A2uiEnumSchema -> validateEnum(payload, schema, path)
            is A2uiAnySchema -> validateAny()
            is A2uiOneOfSchema -> validateOneOf(payload, schema, path)
            is A2uiAllOfSchema -> validateAllOf(payload, schema, path)
            is A2uiAnyOfSchema -> validateAnyOf(payload, schema, path)
            is A2uiConstSchema -> validateConst(payload, schema, path)
            is A2uiRefSchema -> validateRef(payload, schema, path)
            is A2uiCompositeSchema -> validateSchemaInternal(payload, schema.getDefinition(), path)
        }
    }

    private fun validateObject(payload: Any?, schema: A2uiObjectSchema, path: String) {
        if (payload !is Map<*, *>) {
            throw A2uiException.A2uiValidationException(
                "Expected an object, but instead got: $payload",
                path,
            )
        }
        for (required in schema.required) {
            if (required !in payload) {
                throw A2uiException.A2uiValidationException(
                    "Missing required property '$required'",
                    path,
                )
            }
        }
        for ((key, value) in payload) {
            val propertySchema = schema.properties[key]
            if (propertySchema != null) {
                validateSchemaInternal(value, propertySchema, "$path.$key")
            } else if (!schema.isAdditionalPropertiesAllowed) {
                throw A2uiException.A2uiValidationException(
                    "Additional property '$key' not allowed",
                    path,
                )
            } else {
                val additionalPropertiesSchema = schema.additionalPropertiesSchema
                if (additionalPropertiesSchema != null) {
                    validateSchemaInternal(value, additionalPropertiesSchema, "$path.$key")
                }
            }
        }
    }

    private fun validateString(payload: Any?, path: String) {
        if (payload !is String) {
            throw A2uiException.A2uiValidationException(
                "Expected a string, but instead got: $payload",
                path,
            )
        }
    }

    private fun validateNumber(payload: Any?, path: String) {
        if (payload !is Number) {
            throw A2uiException.A2uiValidationException(
                "Expected a number, but instead got: $payload",
                path,
            )
        }
    }

    private fun validateBoolean(payload: Any?, path: String) {
        if (payload !is Boolean) {
            throw A2uiException.A2uiValidationException(
                "Expected a boolean, but instead got: $payload",
                path,
            )
        }
    }

    private fun validateArray(payload: Any?, schema: A2uiArraySchema, path: String) {
        if (payload !is Iterable<*>) {
            throw A2uiException.A2uiValidationException(
                "Expected an array, but instead got: $payload",
                path,
            )
        }
        val itemsSchema = schema.items
        if (itemsSchema != null) {
            payload.forEachIndexed { index, item ->
                validateSchemaInternal(item, itemsSchema, "$path[$index]")
            }
        }
    }

    private fun validateEnum(payload: Any?, schema: A2uiEnumSchema, path: String) {
        if (!schema.enumValues.any { areEqual(payload, it) }) {
            throw A2uiException.A2uiValidationException(
                "Value '${payload}' is not a valid enum value",
                path,
            )
        }
    }

    private fun validateAny() {
        // Any valid JSON element is allowed.
    }

    private fun validateOneOf(payload: Any?, schema: A2uiOneOfSchema, path: String) {
        var matchCount = 0
        for (subSchema in schema.schemas) {
            try {
                validateSchemaInternal(payload, subSchema, path)
                matchCount++
            } catch (_: A2uiException.A2uiValidationException) {
                // Ignore validation failure for subschemas
            }
        }
        if (matchCount != 1) {
            throw A2uiException.A2uiValidationException(
                "Payload must match exactly one subschema, but instead matched $matchCount",
                path,
            )
        }
    }

    private fun validateAllOf(payload: Any?, schema: A2uiAllOfSchema, path: String) {
        for (subSchema in schema.schemas) {
            validateSchemaInternal(payload, subSchema, path)
        }
    }

    private fun validateAnyOf(payload: Any?, schema: A2uiAnyOfSchema, path: String) {
        var matched = false
        for (subSchema in schema.schemas) {
            try {
                validateSchemaInternal(payload, subSchema, path)
                matched = true
                break
            } catch (_: A2uiException.A2uiValidationException) {
                // Ignore validation failure for subschemas
            }
        }
        if (!matched) {
            throw A2uiException.A2uiValidationException(
                "Payload must match at least one subschema",
                path,
            )
        }
    }

    private fun validateConst(payload: Any?, schema: A2uiConstSchema, path: String) {
        if (!areEqual(payload, schema.value)) {
            throw A2uiException.A2uiValidationException(
                "Expected a const value '${schema.value}', but instead got: $payload",
                path,
            )
        }
    }

    private fun validateRef(payload: Any?, schema: A2uiRefSchema, path: String) {
        if (schema.ref == "catalog.json#/\$defs/anyFunction") {
            validateAnyFunctionRef(payload, path)
            return
        }

        throw A2uiException.A2uiValidationException(
            "Ref validation is not supported besides specific cases. " +
                "Use A2uiCompositeSchema to handle references gracefully.",
            path,
        )
    }

    private fun validateAnyFunctionRef(payload: Any?, path: String) {
        validateSchemaInternal(payload, anyFunctionSchema, path)
    }

    /**
     * Determines deep/structural equality between two values.
     *
     * Standard JVM equality (`==` or `.contains()`) falls short for JSON schema validation on
     * complex/nested payloads due to several issues:
     * 1. **Arrays**: JVM arrays (e.g., `Array`, `IntArray`, `DoubleArray`) check reference/identity
     *    equality instead of structural content equality.
     * 2. **Collection Types**: An `Array` and a `List` of the same elements are not considered
     *    equal by default `==` even if they represent the same JSON array structure.
     * 3. **Numeric Types**: Differing numeric types (e.g., `1` as an `Int` vs. `1L` as a `Long`)
     *    fail JVM equality even though they are mathematically equivalent numbers in a JSON
     *    context.
     *
     * This function recursively resolves these differences to perform an accurate deep structural
     * comparison for maps, arrays/collections, numbers, and primitives.
     *
     * @param a The first value.
     * @param b The second value.
     * @return True if [a] and [b] are structurally equal, false otherwise.
     */
    private fun areEqual(a: Any?, b: Any?): Boolean {
        if (a === b) return true
        if (a == null || b == null) return false

        if (a is Map<*, *> && b is Map<*, *>) {
            return areMapsEqual(a, b)
        }

        val aList = toList(a)
        val bList = toList(b)

        if (aList != null && bList != null) {
            return areCollectionsEqual(aList, bList)
        }

        if (a is Number && b is Number) {
            return areNumbersEqual(a, b)
        }

        return a == b
    }

    private fun areMapsEqual(a: Map<*, *>, b: Map<*, *>): Boolean {
        if (a.size != b.size) return false
        for ((k, v) in a) {
            if (!b.containsKey(k)) return false
            if (!areEqual(v, b[k])) return false
        }
        return true
    }

    private fun areCollectionsEqual(a: List<*>, b: List<*>): Boolean {
        if (a.size != b.size) return false
        for (i in a.indices) {
            if (!areEqual(a[i], b[i])) return false
        }
        return true
    }

    private fun areNumbersEqual(a: Number, b: Number): Boolean {
        if (a is Double || a is Float || b is Double || b is Float) {
            return a.toDouble() == b.toDouble()
        }
        return a.toLong() == b.toLong()
    }

    private fun toList(obj: Any?): List<*>? {
        return when (obj) {
            is Array<*> -> obj.toList()
            is BooleanArray -> obj.toList()
            is ByteArray -> obj.toList()
            is CharArray -> obj.toList()
            is DoubleArray -> obj.toList()
            is FloatArray -> obj.toList()
            is IntArray -> obj.toList()
            is LongArray -> obj.toList()
            is ShortArray -> obj.toList()
            is Iterable<*> -> obj.toList()
            else -> null
        }
    }
}
