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

package androidx.a2ui.compose.ui.catalog

import androidx.a2ui.compose.ui.toJsonSchemaString
import androidx.a2ui.model.catalog.basiccatalog.createBasicCatalogFunctions
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiBasicCatalogV1SchemaVerificationTest {

    // =========================================================================================
    // Component Schema Tests
    // =========================================================================================

    @Test
    fun componentNames_matchSpec() {
        assertThat(actualComponents.keys).isEqualTo(specificationComponents.keys)
    }

    @Test fun textComponent_matchesSpec() = assertComponentMatches("Text")

    @Test fun imageComponent_matchesSpec() = assertComponentMatches("Image")

    @Test fun iconComponent_matchesSpec() = assertComponentMatches("Icon")

    @Test fun videoComponent_matchesSpec() = assertComponentMatches("Video")

    @Test fun audioPlayerComponent_matchesSpec() = assertComponentMatches("AudioPlayer")

    @Test
    fun rowComponent_matchesSpec() {
        assertComponentMatches("Row") { properties ->
            // TODO(b/564449412): remove adjustment once catalog.json is updated to v1.0 - default
            //  align is "start" instead of "stretch" in A2uiBasicCatalogV1, which is expected to
            //  be corrected in the v1.0 specification.
            properties.updateObject("align") { it["default"] = JsonPrimitive("start") }
        }
    }

    @Test
    fun columnComponent_matchesSpec() {
        assertComponentMatches("Column") { properties ->
            // TODO(b/564449412): remove adjustment once catalog.json is updated to v1.0 - default
            //  align is "start" instead of "stretch" in A2uiBasicCatalogV1, which is expected to
            //  be corrected in the v1.0 specification.
            properties.updateObject("align") { it["default"] = JsonPrimitive("start") }
        }
    }

    @Test fun listComponent_matchesSpec() = assertComponentMatches("List")

    @Test fun cardComponent_matchesSpec() = assertComponentMatches("Card")

    @Test fun tabsComponent_matchesSpec() = assertComponentMatches("Tabs")

    @Test fun modalComponent_matchesSpec() = assertComponentMatches("Modal")

    @Test fun dividerComponent_matchesSpec() = assertComponentMatches("Divider")

    @Test fun buttonComponent_matchesSpec() = assertComponentMatches("Button")

    @Test fun textFieldComponent_matchesSpec() = assertComponentMatches("TextField")

    @Test fun checkBoxComponent_matchesSpec() = assertComponentMatches("CheckBox")

    @Test fun choicePickerComponent_matchesSpec() = assertComponentMatches("ChoicePicker")

    @Test fun sliderComponent_matchesSpec() = assertComponentMatches("Slider")

    @Test fun dateTimeInputComponent_matchesSpec() = assertComponentMatches("DateTimeInput")

    // =========================================================================================
    // Function Schema Tests
    // =========================================================================================

    @Test
    fun functionNames_matchSpec() {
        assertThat(actualFunctions.keys).isEqualTo(specificationFunctions.keys)
    }

    @Test fun requiredFunction_matchesSpec() = assertFunctionMatches("required")

    @Test fun regexFunction_matchesSpec() = assertFunctionMatches("regex")

    @Test
    fun lengthFunction_matchesSpec() {
        assertFunctionMatches("length") { functionSchema ->
            // TODO(b/564431958): fix discrepancy - spec defines anyOf requiring min or max, and
            //  types min/max as integer (minimum: 0) instead of number.
            functionSchema.updateObject("properties", "args") { arguments ->
                arguments.remove("anyOf")
                for (bound in listOf("min", "max")) {
                    arguments.updateObject("properties", bound) {
                        it["type"] = JsonPrimitive("number")
                        it.remove("minimum")
                    }
                }
            }
        }
    }

    @Test
    fun numericFunction_matchesSpec() {
        assertFunctionMatches("numeric") { functionSchema ->
            // TODO(b/564434278): fix discrepancy - spec defines anyOf requiring min or max on args.
            functionSchema.updateObject("properties", "args") { it.remove("anyOf") }
        }
    }

    @Test fun emailFunction_matchesSpec() = assertFunctionMatches("email")

    @Test
    fun formatStringFunction_matchesSpec() {
        assertFunctionMatches("formatString") { functionSchema ->
            // TODO(b/564433252): fix discrepancy - spec description wraps interpolation examples in
            //  backticks (`${expression}`), which are missing in A2uiFormatStringFunction.
            val description = functionSchema.getValue("description").jsonPrimitive.content
            functionSchema["description"] = JsonPrimitive(description.replace("`", ""))
        }
    }

    @Test fun formatNumberFunction_matchesSpec() = assertFunctionMatches("formatNumber")

    @Test fun formatCurrencyFunction_matchesSpec() = assertFunctionMatches("formatCurrency")

    @Test fun formatDateFunction_matchesSpec() = assertFunctionMatches("formatDate")

    @Test fun pluralizeFunction_matchesSpec() = assertFunctionMatches("pluralize")

    @Test
    fun openUrlFunction_matchesSpec() {
        assertFunctionMatches("openUrl") { functionSchema ->
            // TODO(b/564436335): fix discrepancy - spec defines "format": "uri" on the url argument
            functionSchema.updateObject("properties", "args", "properties", "url") {
                it.remove("format")
            }
        }
    }

    @Test
    fun andFunction_matchesSpec() {
        assertFunctionMatches("and") { functionSchema ->
            // TODO(b/564435019): fix discrepancy - spec defines "minItems": 2 on the values array
            //  argument.
            functionSchema.updateObject("properties", "args", "properties", "values") {
                it.remove("minItems")
            }
        }
    }

    @Test
    fun orFunction_matchesSpec() {
        assertFunctionMatches("or") { functionSchema ->
            // TODO(b/564436663): fix discrepancy - spec defines "minItems": 2 on the values array
            //  argument.
            functionSchema.updateObject("properties", "args", "properties", "values") {
                it.remove("minItems")
            }
        }
    }

    @Test fun notFunction_matchesSpec() = assertFunctionMatches("not")

    // =========================================================================================
    // Schema Comparison Helpers
    // =========================================================================================

    /**
     * Asserts that the serialized component schema for [name] matches the specification after
     * flattening `allOf` composition (`ComponentCommon`, `CatalogComponentCommon`, `Checkable`) and
     * applying [adjustProperties] for any component-specific discrepancies.
     */
    private fun assertComponentMatches(
        name: String,
        adjustProperties: (MutableMap<String, JsonElement>) -> Unit = {},
    ) {
        val allOf =
            specificationComponents.getValue(name).jsonObject.getValue("allOf").jsonArray.map {
                it.jsonObject
            }
        val resolvedItems = allOf.map { item ->
            item[$$"$ref"]?.let { definitionsByReference.getValue(it.jsonPrimitive.content) }
                ?: item
        }

        val properties = buildMap {
            for (item in resolvedItems) {
                item["properties"]?.jsonObject?.let { putAll(it) }
            }
            // TODO(b/564446628): fix discrepancy - spec requires the component envelope "id"
            //  property (from ComponentCommon) in serialized component schemas, which is currently
            //  omitted by the serializer.
            remove("id")
        }
            .toMutableMap()
            .apply(adjustProperties)

        val required =
            resolvedItems
                .flatMap { it["required"]?.jsonArray ?: emptyList() }
                // TODO(b/564446628): fix discrepancy - see above ("id" is omitted from properties)
                .filterNot { it.jsonPrimitive.content == "id" }

        val description = allOf.firstNotNullOfOrNull { item ->
            item.takeIf { $$"$ref" !in it }?.get("description")
        }

        val expectedSchema = buildJsonObject {
            put("type", "object")
            put("properties", JsonObject(properties))
            put("required", JsonArray(required))
            description?.let { put("description", it) }
        }

        assertSchemaEquals("Component '$name'", actualComponents.getValue(name), expectedSchema)
    }

    /**
     * Asserts that the serialized function schema for [name] matches the specification after
     * applying [adjustFunction] for any function-specific discrepancies.
     */
    private fun assertFunctionMatches(
        name: String,
        adjustFunction: (MutableMap<String, JsonElement>) -> Unit = {},
    ) {
        val expectedSchema =
            JsonObject(
                specificationFunctions
                    .getValue(name)
                    .jsonObject
                    .toMutableMap()
                    .apply(adjustFunction)
            )
        assertSchemaEquals("Function '$name'", actualFunctions.getValue(name), expectedSchema)
    }

    private fun assertSchemaEquals(message: String, actual: JsonElement, expected: JsonElement) {
        val prettyExpected =
            prettyJson.encodeToString(JsonElement.serializer(), normalize(expected, expected))
        val prettyActual =
            prettyJson.encodeToString(JsonElement.serializer(), normalize(actual, expected))
        assertWithMessage(message).that(prettyActual).isEqualTo(prettyExpected)
    }

    /**
     * Navigates a path of [String] object keys and/or [Int] array indices and mutates the target
     * [JsonObject] in-place.
     */
    private fun MutableMap<String, JsonElement>.updateObject(
        vararg path: Any,
        block: (MutableMap<String, JsonElement>) -> Unit,
    ) {
        fun updateElement(index: Int, element: JsonElement): JsonElement {
            if (index == path.size) {
                return JsonObject(element.jsonObject.toMutableMap().apply(block))
            }
            return when (val step = path[index]) {
                is String -> {
                    val map = element.jsonObject.toMutableMap()
                    map[step] = updateElement(index + 1, map.getValue(step))
                    JsonObject(map)
                }
                is Int -> {
                    val list = element.jsonArray.toMutableList()
                    list[step] = updateElement(index + 1, list[step])
                    JsonArray(list)
                }
                else -> error("Unsupported path step: $step")
            }
        }
        val rootKey = path[0] as String
        this[rootKey] = updateElement(1, getValue(rootKey))
    }

    /**
     * Normalizes a [JsonElement] for deterministic comparison by sorting object keys
     * alphabetically, sorting JSON Schema `required` and `enum` arrays, and omitting
     * `"description"` fields wherever [reference] does not specify a `"description"`.
     */
    private fun normalize(element: JsonElement, reference: JsonElement?): JsonElement =
        when (element) {
            is JsonObject -> {
                val referenceObject = reference as? JsonObject
                JsonObject(
                    element.entries
                        .filter { (key, _) ->
                            key != "description" ||
                                referenceObject == null ||
                                key in referenceObject
                        }
                        .sortedBy { it.key }
                        .associate { (key, value) ->
                            val normalizedValue =
                                if ((key == "required" || key == "enum") && value is JsonArray) {
                                    JsonArray(value.sortedBy { it.jsonPrimitive.content })
                                } else {
                                    normalize(value, referenceObject?.get(key))
                                }
                            key to normalizedValue
                        }
                )
            }
            is JsonArray -> {
                val referenceArray = reference as? JsonArray
                JsonArray(
                    element.mapIndexed { index, item ->
                        normalize(item, referenceArray?.getOrNull(index))
                    }
                )
            }
            else -> element
        }

    private companion object {
        val prettyJson = Json { prettyPrint = true }

        val actualCatalogJson = run {
            val functions =
                createBasicCatalogFunctions(
                    urlOpener = { _ -> },
                    messageFormatter = { template, _, _ -> template },
                )
            val catalog = createTestBasicCatalog(functions = functions).toA2uiCatalog()
            Json.parseToJsonElement(catalog.toJsonSchemaString()).jsonObject
        }
        val actualComponents = actualCatalogJson.getValue("components").jsonObject
        val actualFunctions = actualCatalogJson.getValue("functions").jsonObject

        val specificationCatalogJson =
            readJsonResource("catalog.json") {
                it
                    // TODO(b/564450642): fix discrepancy - SCHEMA_ID_COMMON_TYPES emits relative
                    //  "common_types.json" $ref URIs instead of the canonical specification URI.
                    .replace(
                        "https://a2ui.org/specification/v0_9/common_types.json",
                        "common_types.json",
                    )
                    // TODO(b/564448732): fix discrepancy - spec defines "unevaluatedProperties":
                    //  false (equivalent to "additionalProperties": false on a flattened object
                    //  schema) on all components
                    .replace("\"unevaluatedProperties\":", "\"additionalProperties\":")
                    // TODO(b/564449963): fix discrepancy - component and function serializers use
                    //  A2uiStringSchema for "const" discriminators ("component", "call",
                    //  "returnType"), which emits a redundant `"type": "string"` alongside
                    //  `"const"`.
                    .replace("\"const\":", "\"type\": \"string\", \"const\":")
            }
        val specificationComponents = specificationCatalogJson.getValue("components").jsonObject
        val specificationFunctions = specificationCatalogJson.getValue("functions").jsonObject

        val definitionsByReference: Map<String, JsonObject> = buildMap {
            specificationCatalogJson.getValue($$"$defs").jsonObject.forEach { (name, definition) ->
                put($$"#/$defs/$$name", definition.jsonObject)
            }
            readJsonResource("common_types.json") {
                    it.replace($$"\"#/$defs/", $$"\"common_types.json#/$defs/")
                }
                .getValue($$"$defs")
                .jsonObject
                .forEach { (name, definition) ->
                    put($$"common_types.json#/$defs/$$name", definition.jsonObject)
                }
        }

        fun readJsonResource(name: String, transformRaw: (String) -> String): JsonObject {
            val text =
                checkNotNull(
                        A2uiBasicCatalogV1SchemaVerificationTest::class.java.getResource(name)
                    ) {
                        "Missing $name test resource"
                    }
                    .readText()
            return Json.parseToJsonElement(transformRaw(text)).jsonObject
        }
    }
}
