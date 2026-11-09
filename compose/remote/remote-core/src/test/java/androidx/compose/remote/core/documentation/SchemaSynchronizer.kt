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

package androidx.compose.remote.core.documentation

import androidx.compose.remote.generator.JsonSchemaParser
import androidx.compose.remote.generator.OperationSchema
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/**
 * Synchronizes the declarative JSON schema ([operations.json]) with Java operation definitions.
 *
 * Can run in validation mode (asserting synchronization) or update mode (regenerating/patching
 * [operations.json] on disk).
 */
class SchemaSynchronizer(private val schemaFile: File) {
    data class JavaOpInfo(
        val id: Int,
        val name: String,
        val category: String,
        val description: String,
        val fields: List<JavaFieldInfo>,
    )

    data class JavaFieldInfo(
        val name: String,
        val jsonType: String,
        val description: String,
        val enumValues: Map<String, Int> = emptyMap(),
    )

    /** Extracts all operation definitions registered in Java via [RemoteComposeDocumentation]. */
    fun collectJavaOperations(): List<JavaOpInfo> {
        val doc = RemoteComposeDocumentation("RemoteCompose")
        doc.listOperations()

        val collector =
            object : DocumentationBuilder {
                val operations = ArrayList<DocumentedOperation>()

                override fun add(value: String) {}

                override fun operation(
                    category: String,
                    id: Int,
                    name: String,
                ): DocumentedOperation {
                    val op = DocumentedOperation(category, id, name)
                    operations.add(op)
                    return op
                }

                override fun wipOperation(
                    category: String,
                    id: Int,
                    name: String,
                ): DocumentedOperation {
                    val op = DocumentedOperation(category, id, name, true)
                    operations.add(op)
                    return op
                }
            }

        for ((_, companion) in doc.operationsMap) {
            companion.documentation(collector)
        }

        return collector.operations.map { op ->
            val fields = ArrayList<JavaFieldInfo>()
            for (f in op.fields) {
                if (f is OperationField) {
                    val enumMap = LinkedHashMap<String, Int>()
                    for (pv in f.possibleValues) {
                        pv.value.toIntOrNull()?.let { enumMap[pv.name] = it }
                    }
                    fields.add(
                        JavaFieldInfo(
                            name = toCamelCase(f.name),
                            jsonType = mapDocTypeToJson(f.type),
                            description = f.description,
                            enumValues = enumMap,
                        )
                    )
                }
            }
            JavaOpInfo(
                id = op.id,
                name = op.name,
                category = op.category,
                description = op.description ?: "",
                fields = fields,
            )
        }
    }

    /**
     * Checks for discrepancies between Java operations and the JSON schema. Returns a list of
     * human-readable error descriptions.
     */
    fun findDiscrepancies(jsonSchema: OperationSchema): List<String> {
        val javaOps = collectJavaOperations()
        val discrepancies = ArrayList<String>()

        for (javaOp in javaOps) {
            val op = jsonSchema.operationsById[javaOp.id]
            if (op == null) {
                discrepancies.add("Missing in JSON: opcode ${javaOp.id} ('${javaOp.name}')")
            } else {
                val isKnownAlias =
                    (javaOp.id == 83 && op.name == "MultiClickModifier") ||
                        (javaOp.id == 151 && op.name == "TextLookup") ||
                        (javaOp.id == 166 && op.name == "FloatFunctionCall") ||
                        (javaOp.id == 168 && op.name == "FloatFunctionDefine")
                if (op.name != javaOp.name && !isKnownAlias) {
                    discrepancies.add(
                        "Name mismatch for opcode ${javaOp.id}: Java has '${javaOp.name}', JSON has '${op.name}'"
                    )
                }
                val javaHasVariablePayload =
                    javaOp.fields.any { it.jsonType.endsWith("[]") || it.jsonType == "bytes" }
                val jsonHasVariablePayload =
                    op.fields.any {
                        it.type.endsWith("[]") || it.type == "bytes" || it.type == "string"
                    } || !op.sparseParameters.isNullOrEmpty()
                if (javaHasVariablePayload && !jsonHasVariablePayload) {
                    discrepancies.add(
                        "Variable-length payload mismatch for opcode ${javaOp.id} ('${op.name}'): " +
                            "Java documentation declares array/buffer fields ${javaOp.fields.map { "${it.name}:${it.jsonType}" }} " +
                            "but JSON schema only has fixed scalar fields"
                    )
                }
            }
        }

        return discrepancies
    }

    /** Updates [schemaFile] on disk by appending any missing Java operations. */
    fun updateSchemaFile(): Int {
        if (!schemaFile.exists()) {
            throw IllegalStateException("Schema file does not exist: ${schemaFile.absolutePath}")
        }

        val schemaText = schemaFile.readText()
        val root = JSONObject(schemaText)
        val existingSchema = JsonSchemaParser.parseString(schemaText)
        val javaOps = collectJavaOperations()
        val missingOps = javaOps.filter { it.id !in existingSchema.operationsById }

        if (missingOps.isEmpty()) {
            return 0
        }

        val opsArray =
            if (root.has("operations")) {
                root.getJSONArray("operations")
            } else {
                JSONArray().also { root.put("operations", it) }
            }

        for (op in missingOps) {
            val opObj = JSONObject()
            opObj.put("id", op.id)
            opObj.put("name", op.name)
            opObj.put("category", op.category)
            if (op.description.isNotEmpty()) {
                opObj.put("description", op.description)
            }
            opObj.put("addedVersion", 6)
            opObj.put("profiles", JSONArray(listOf("baseline")))
            if (op.fields.isNotEmpty()) {
                val fieldsArray = JSONArray()
                for (f in op.fields) {
                    val fieldObj = JSONObject()
                    fieldObj.put("name", f.name)
                    fieldObj.put("type", f.jsonType)
                    if (f.jsonType.endsWith("[]")) {
                        fieldObj.put("lengthType", "int")
                    }
                    if (f.description.isNotEmpty()) {
                        fieldObj.put("description", f.description)
                    }
                    if (f.enumValues.isNotEmpty()) {
                        val enumObj = JSONObject()
                        for ((k, v) in f.enumValues) {
                            enumObj.put(k, v)
                        }
                        fieldObj.put("enum", enumObj)
                    }
                    fieldsArray.put(fieldObj)
                }
                opObj.put("fields", fieldsArray)
            }
            opsArray.put(opObj)
        }

        schemaFile.writeText(root.toString(2) + "\n")
        return missingOps.size
    }

    companion object {
        /** Wire doc type ID representing a float-or-id field. */
        const val DOC_TYPE_FLOAT_OR_ID: Int = 16

        fun mapDocTypeToJson(docType: Int): String =
            when (docType) {
                DocumentedOperation.INT -> "int"
                DocumentedOperation.FLOAT -> "float"
                DocumentedOperation.BOOLEAN -> "boolean"
                DocumentedOperation.BYTE -> "byte"
                DocumentedOperation.SHORT -> "short"
                DocumentedOperation.LONG -> "long"
                DocumentedOperation.UTF8 -> "string"
                DocumentedOperation.BUFFER,
                DocumentedOperation.BYTE_ARRAY,
                DocumentedOperation.REPEATED_BYTE -> "bytes"
                DocumentedOperation.FLOAT_ARRAY,
                DocumentedOperation.REPEATED_FLOAT -> "float_or_id[]"
                DocumentedOperation.INT_ARRAY,
                DocumentedOperation.REPEATED_INT -> "int[]"
                DocumentedOperation.VALUE -> "int"
                DOC_TYPE_FLOAT_OR_ID -> "float_or_id"
                else -> "int"
            }

        private fun toCamelCase(name: String): String {
            val cleaned = name.replace(Regex("\\[.*?]"), "").trim()
            if (!cleaned.contains('_')) return cleaned.replaceFirstChar { it.lowercaseChar() }
            val parts = cleaned.split('_').filter { it.isNotEmpty() }
            return parts.first().lowercase() +
                parts.drop(1).joinToString("") {
                    it.lowercase().replaceFirstChar { c -> c.uppercaseChar() }
                }
        }
    }
}
