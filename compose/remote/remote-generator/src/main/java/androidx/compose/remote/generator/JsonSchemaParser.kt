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

package androidx.compose.remote.generator

import androidx.annotation.RestrictTo
import java.io.InputStream
import java.io.Reader
import org.json.JSONObject

/** Parser for the RemoteCompose operations JSON schema. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object JsonSchemaParser {

    public fun parse(reader: Reader): OperationSchema {
        return parseString(reader.readText())
    }

    public fun parse(inputStream: InputStream): OperationSchema {
        return parseString(inputStream.bufferedReader().readText())
    }

    public fun parseString(jsonText: String): OperationSchema {
        val root = JSONObject(jsonText)
        return parseJsonObject(root)
    }

    internal fun parseJsonObject(root: JSONObject): OperationSchema {
        val schemaVersion = root.optString("schemaVersion", "1.0.0")
        val targetApiLevel = root.optInt("targetApiLevel", 8)

        val types = mutableMapOf<String, SchemaTypeDefinition>()
        if (root.has("types")) {
            val typesObj = root.getJSONObject("types")
            for (key in typesObj.keys()) {
                val typeObj = typesObj.getJSONObject(key)
                types[key] =
                    SchemaTypeDefinition(
                        name = key,
                        wire = typeObj.optString("wire", ""),
                        base = if (typeObj.has("base")) typeObj.getString("base") else null,
                        description = typeObj.optString("description", ""),
                    )
            }
        }

        val categories = mutableListOf<SchemaCategory>()
        if (root.has("categories")) {
            val catArray = root.getJSONArray("categories")
            for (i in 0 until catArray.length()) {
                val catObj = catArray.getJSONObject(i)
                categories.add(
                    SchemaCategory(
                        name = catObj.optString("name", ""),
                        description = catObj.optString("description", ""),
                    )
                )
            }
        }

        val structs = mutableMapOf<String, SchemaStruct>()
        if (root.has("structs")) {
            val structsObj = root.getJSONObject("structs")
            for (key in structsObj.keys()) {
                val sObj = structsObj.getJSONObject(key)
                val sDesc = sObj.optString("description", "")
                val sFields = mutableListOf<SchemaField>()
                if (sObj.has("fields")) {
                    val fArray = sObj.getJSONArray("fields")
                    for (idx in 0 until fArray.length()) {
                        sFields.add(parseField(fArray.getJSONObject(idx)))
                    }
                }
                structs[key] = SchemaStruct(name = key, description = sDesc, fields = sFields)
            }
        }

        val paintCommands = mutableMapOf<Int, SchemaPaintCommand>()
        if (root.has("paintCommands")) {
            val pcObj = root.getJSONObject("paintCommands")
            for (key in pcObj.keys()) {
                val cmdObj = pcObj.getJSONObject(key)
                val id = cmdObj.getInt("id")
                val desc = cmdObj.optString("description", "")
                val fields = mutableListOf<SchemaField>()
                if (cmdObj.has("fields")) {
                    val fArray = cmdObj.getJSONArray("fields")
                    for (idx in 0 until fArray.length()) {
                        fields.add(parseField(fArray.getJSONObject(idx)))
                    }
                }
                paintCommands[id] =
                    SchemaPaintCommand(id = id, name = key, description = desc, fields = fields)
            }
        }

        val rpnOperators =
            if (root.has("rpnOperators")) {
                parseRpnOperatorTable(root.getJSONObject("rpnOperators"))
            } else null

        val intRpnOperators =
            if (root.has("intRpnOperators")) {
                parseRpnOperatorTable(root.getJSONObject("intRpnOperators"))
            } else null

        val systemVariables = mutableListOf<SchemaSystemVariable>()
        if (root.has("systemVariables")) {
            val svArray = root.getJSONArray("systemVariables")
            for (idx in 0 until svArray.length()) {
                val svObj = svArray.getJSONObject(idx)
                systemVariables.add(
                    SchemaSystemVariable(
                        id = svObj.getInt("id"),
                        name = svObj.getString("name"),
                        type = svObj.getString("type"),
                        description = svObj.optString("description", ""),
                    )
                )
            }
        }

        val pathCommands = mutableMapOf<Int, SchemaPathCommand>()
        if (root.has("pathCommands")) {
            val pcObj = root.getJSONObject("pathCommands")
            for (key in pcObj.keys()) {
                val cmdObj = pcObj.getJSONObject(key)
                val id = cmdObj.getInt("id")
                val desc = cmdObj.optString("description", "")
                val fields = mutableListOf<SchemaField>()
                if (cmdObj.has("fields")) {
                    val fArray = cmdObj.getJSONArray("fields")
                    for (idx in 0 until fArray.length()) {
                        fields.add(parseField(fArray.getJSONObject(idx)))
                    }
                }
                pathCommands[id] =
                    SchemaPathCommand(id = id, name = key, description = desc, fields = fields)
            }
        }

        val operations = mutableListOf<SchemaOperation>()
        if (root.has("operations")) {
            val opsArray = root.getJSONArray("operations")
            for (i in 0 until opsArray.length()) {
                val opObj = opsArray.getJSONObject(i)
                val id = opObj.getInt("id")
                val name = opObj.getString("name")
                val category = opObj.optString("category", "")
                val description = opObj.optString("description", "")
                val addedVersion = opObj.optInt("addedVersion", 6)
                val isContainer = opObj.optBoolean("isContainer", false)
                val isContainerEnd = opObj.optBoolean("isContainerEnd", false)
                val closedBy = if (opObj.has("closedBy")) opObj.getString("closedBy") else null
                val allowedChildren =
                    if (opObj.has("allowedChildren")) {
                        val acArray = opObj.getJSONArray("allowedChildren")
                        (0 until acArray.length()).map { acArray.getString(it) }
                    } else null

                val fields = mutableListOf<SchemaField>()
                if (opObj.has("fields")) {
                    val fieldsArray = opObj.getJSONArray("fields")
                    for (j in 0 until fieldsArray.length()) {
                        fields.add(parseField(fieldsArray.getJSONObject(j)))
                    }
                }

                val extensions =
                    if (opObj.has("extensions")) {
                        val extObj = opObj.getJSONObject("extensions")
                        val extType = extObj.optString("type", "")
                        val tagType = extObj.optString("tagType", "short")
                        val supportedTags = mutableListOf<SchemaTag>()
                        if (extObj.has("supportedTags")) {
                            val tagsArray = extObj.getJSONArray("supportedTags")
                            for (k in 0 until tagsArray.length()) {
                                val tagObj = tagsArray.getJSONObject(k)
                                supportedTags.add(
                                    SchemaTag(
                                        name = tagObj.getString("name"),
                                        tag = tagObj.getInt("tag"),
                                        type = tagObj.optString("type", "int"),
                                        description = tagObj.optString("description", ""),
                                    )
                                )
                            }
                        }
                        SchemaExtension(
                            type = extType,
                            tagType = tagType,
                            supportedTags = supportedTags,
                        )
                    } else null

                val profiles =
                    if (opObj.has("profiles")) {
                        val arr = opObj.getJSONArray("profiles")
                        (0 until arr.length()).map { arr.getString(it) }
                    } else {
                        listOf("baseline")
                    }

                val opCodeName =
                    if (opObj.has("opCodeName")) opObj.getString("opCodeName") else null

                val restrictToLibraryGroup = opObj.optBoolean("restrictToLibraryGroup", true)
                val encoding = if (opObj.has("encoding")) opObj.getString("encoding") else null
                val sparseCountType =
                    if (opObj.has("sparseCountType")) opObj.getString("sparseCountType") else null
                val sparseKeyType =
                    if (opObj.has("sparseKeyType")) opObj.getString("sparseKeyType") else null
                val sparseParameters =
                    if (opObj.has("sparseParameters")) {
                        val spArray = opObj.getJSONArray("sparseParameters")
                        (0 until spArray.length()).map { idx ->
                            val spObj = spArray.getJSONObject(idx)
                            SchemaSparseParam(
                                id = spObj.getInt("id"),
                                name = spObj.getString("name"),
                                type = spObj.getString("type"),
                                defaultValue =
                                    if (spObj.has("defaultValue")) spObj.getString("defaultValue")
                                    else null,
                                description = spObj.optString("description", ""),
                                lengthType =
                                    if (spObj.has("lengthType")) spObj.getString("lengthType")
                                    else null,
                                role = if (spObj.has("role")) spObj.getString("role") else null,
                                idKind =
                                    if (spObj.has("idKind")) spObj.getString("idKind") else null,
                                ignored = spObj.optBoolean("ignored", false),
                            )
                        }
                    } else null

                operations.add(
                    SchemaOperation(
                        id = id,
                        name = name,
                        category = category,
                        description = description,
                        addedVersion = addedVersion,
                        isContainer = isContainer,
                        isContainerEnd = isContainerEnd,
                        closedBy = closedBy,
                        fields = fields,
                        extensions = extensions,
                        profiles = profiles,
                        opCodeName = opCodeName,
                        restrictToLibraryGroup = restrictToLibraryGroup,
                        encoding = encoding,
                        sparseCountType = sparseCountType,
                        sparseKeyType = sparseKeyType,
                        sparseParameters = sparseParameters,
                        allowedChildren = allowedChildren,
                    )
                )
            }
        }

        return OperationSchema(
            schemaVersion = schemaVersion,
            targetApiLevel = targetApiLevel,
            types = types,
            categories = categories,
            operations = operations,
            structs = structs,
            paintCommands = paintCommands,
            rpnOperators = rpnOperators,
            intRpnOperators = intRpnOperators,
            pathCommands = pathCommands,
            systemVariables = systemVariables,
        )
    }

    private fun parseRpnOperatorTable(rpnObj: JSONObject): SchemaRpnOperatorTable {
        val offset = rpnObj.getInt("offset")
        val opsArray = rpnObj.getJSONArray("operators")
        val opsList = mutableListOf<SchemaRpnOperator>()
        for (idx in 0 until opsArray.length()) {
            val opObj = opsArray.getJSONObject(idx)
            opsList.add(
                SchemaRpnOperator(
                    id = opObj.getInt("id"),
                    name = opObj.getString("name"),
                    symbol = opObj.optString("symbol", ""),
                    arity = opObj.optInt("arity", 1),
                    addedVersion = opObj.optInt("addedVersion", 6),
                    description = opObj.optString("description", ""),
                )
            )
        }
        return SchemaRpnOperatorTable(offset = offset, operators = opsList)
    }

    /**
     * Validates the structural integrity and semantic consistency of a parsed operations schema.
     * Returns a list of validation error messages, or an empty list if validation passes.
     */
    internal fun validate(root: JSONObject): List<String> {
        val errors = mutableListOf<String>()

        // 1. Required top-level properties
        for (req in
            listOf("schemaVersion", "targetApiLevel", "types", "categories", "operations")) {
            if (!root.has(req)) {
                errors.add("Missing required top-level property: $req")
            }
        }
        if (errors.isNotEmpty()) return errors

        // 2. Types & structs catalog
        val typesObj = root.getJSONObject("types")
        val declaredTypes = typesObj.keys().asSequence().toMutableSet()
        if (root.has("structs")) {
            val structsObj = root.getJSONObject("structs")
            declaredTypes.addAll(structsObj.keys().asSequence())
            for (structName in structsObj.keys()) {
                val structObj = structsObj.getJSONObject(structName)
                if (structObj.has("fields")) {
                    val sfArray = structObj.getJSONArray("fields")
                    for (j in 0 until sfArray.length()) {
                        validateField(
                            owner = "Struct $structName",
                            f = sfArray.getJSONObject(j),
                            declaredTypes = declaredTypes,
                            errors = errors,
                        )
                    }
                }
            }
        }

        // 3. Operations validation
        val opsArray = root.getJSONArray("operations")
        val seenIds = mutableSetOf<Int>()
        val seenNames = mutableSetOf<String>()
        var prevId = -1

        for (i in 0 until opsArray.length()) {
            val op = opsArray.getJSONObject(i)
            val id = op.getInt("id")
            val name = op.getString("name")

            if (id !in 0..255) {
                errors.add("Operation $name has invalid ID $id (must be 0..255)")
            }
            if (id <= prevId) {
                errors.add("Operations not sorted: $name ($id) <= previous ID $prevId at index $i")
            }
            prevId = id

            if (!seenIds.add(id)) {
                errors.add("Duplicate operation ID: $id ($name)")
            }
            if (!seenNames.add(name)) {
                errors.add("Duplicate operation name: $name")
            }

            if (op.has("fields")) {
                val fArray = op.getJSONArray("fields")
                for (j in 0 until fArray.length()) {
                    validateField(
                        owner = "Operation $name",
                        f = fArray.getJSONObject(j),
                        declaredTypes = declaredTypes,
                        errors = errors,
                    )
                }
            }
            if (op.has("sparseCountType")) {
                val sct = op.getString("sparseCountType")
                if (sct !in VALID_LENGTH_TYPES) {
                    errors.add("Operation $name has invalid sparseCountType '$sct'")
                }
            }
            if (op.has("sparseKeyType")) {
                val skt = op.getString("sparseKeyType")
                if (skt !in VALID_LENGTH_TYPES) {
                    errors.add("Operation $name has invalid sparseKeyType '$skt'")
                }
            }
            if (op.has("sparseParameters")) {
                val spArray = op.getJSONArray("sparseParameters")
                for (j in 0 until spArray.length()) {
                    val sp = spArray.getJSONObject(j)
                    val spName = sp.getString("name")
                    val spType = sp.getString("type")
                    if (!isDeclaredSchemaType(spType, declaredTypes)) {
                        errors.add(
                            "Operation $name sparse parameter $spName has undeclared type '$spType'"
                        )
                    }
                    if (sp.has("lengthType")) {
                        val lt = sp.getString("lengthType")
                        if (lt !in VALID_LENGTH_TYPES) {
                            errors.add(
                                "Operation $name sparse parameter $spName has invalid lengthType '$lt'"
                            )
                        }
                    }
                }
            }
            val isSparseEncoding = op.optString("encoding", "") == "sparse"
            val hasSparseParameters = op.has("sparseParameters")
            if (hasSparseParameters && !isSparseEncoding) {
                errors.add("Operation $name declares sparseParameters without encoding == 'sparse'")
            }
            if (isSparseEncoding || hasSparseParameters) {
                if (!op.has("sparseCountType")) {
                    errors.add("Sparse operation $name must specify sparseCountType")
                }
                if (!op.has("sparseKeyType")) {
                    errors.add("Sparse operation $name must specify sparseKeyType")
                }
                if (hasSparseParameters) {
                    val spArr = op.getJSONArray("sparseParameters")
                    for (spIdx in 0 until spArr.length()) {
                        val spObj = spArr.getJSONObject(spIdx)
                        if (spObj.getString("type").endsWith("[]") && !spObj.has("lengthType")) {
                            errors.add(
                                "Sparse operation $name array parameter ${spObj.getString("name")} must specify lengthType"
                            )
                        }
                    }
                }
            }
        }

        // 4. Reference integrity for closedBy
        for (i in 0 until opsArray.length()) {
            val op = opsArray.getJSONObject(i)
            if (op.has("closedBy")) {
                val closedBy = op.getString("closedBy")
                if (closedBy !in seenNames) {
                    errors.add(
                        "Operation ${op.getString("name")} references non-existent closedBy '$closedBy'"
                    )
                }
            }
        }

        // 5. Paint commands validation
        if (root.has("paintCommands")) {
            val pcObj = root.getJSONObject("paintCommands")
            val pcIds = mutableSetOf<Int>()
            for (key in pcObj.keys()) {
                val cmdObj = pcObj.getJSONObject(key)
                val cmdId = cmdObj.getInt("id")
                if (!pcIds.add(cmdId)) {
                    errors.add("Duplicate paint command ID: $cmdId ($key)")
                }
            }
        }

        // 6. RPN operators validation
        for (rpnKey in listOf("rpnOperators", "intRpnOperators")) {
            if (root.has(rpnKey)) {
                val rpnObj = root.getJSONObject(rpnKey)
                val rpnOpsArray = rpnObj.getJSONArray("operators")
                val rpnIds = mutableSetOf<Int>()
                for (i in 0 until rpnOpsArray.length()) {
                    val rpnOp = rpnOpsArray.getJSONObject(i)
                    val opId = rpnOp.getInt("id")
                    if (!rpnIds.add(opId)) {
                        errors.add(
                            "Duplicate $rpnKey operator ID: $opId (${rpnOp.getString("name")})"
                        )
                    }
                }
            }
        }

        // 7. Path commands validation
        if (root.has("pathCommands")) {
            val pcObj = root.getJSONObject("pathCommands")
            val pathCmdIds = mutableSetOf<Int>()
            for (key in pcObj.keys()) {
                val cmdObj = pcObj.getJSONObject(key)
                val cmdId = cmdObj.getInt("id")
                if (!pathCmdIds.add(cmdId)) {
                    errors.add("Duplicate path command ID: $cmdId ($key)")
                }
            }
        }

        // 8. System variables validation
        if (root.has("systemVariables")) {
            val svArray = root.getJSONArray("systemVariables")
            val svIds = mutableSetOf<Int>()
            val svNames = mutableSetOf<String>()
            for (i in 0 until svArray.length()) {
                val svObj = svArray.getJSONObject(i)
                val svId = svObj.getInt("id")
                val svName = svObj.getString("name")
                if (!svIds.add(svId)) {
                    errors.add("Duplicate system variable ID: $svId ($svName)")
                }
                if (!svNames.add(svName)) {
                    errors.add("Duplicate system variable name: $svName")
                }
            }
        }

        return errors
    }

    private val VALID_LENGTH_TYPES = setOf("ubyte", "byte", "ushort", "short", "int")

    private fun isDeclaredSchemaType(type: String, declaredTypes: Set<String>): Boolean {
        val elemType = type.removeSuffix("[]")
        return type in declaredTypes ||
            elemType in declaredTypes ||
            type == "int[]" ||
            type == "bytes"
    }

    private fun isValidBitRange(range: String): Boolean {
        val low = range.substringBefore("..").toIntOrNull() ?: return false
        val high = range.substringAfter("..", range).toIntOrNull() ?: return false
        return low in 0..31 && high in low..31
    }

    private fun validateField(
        owner: String,
        f: JSONObject,
        declaredTypes: Set<String>,
        errors: MutableList<String>,
    ) {
        val fName = f.getString("name")
        val fType = f.getString("type")
        if (!isDeclaredSchemaType(fType, declaredTypes)) {
            errors.add("$owner field $fName has undeclared type '$fType'")
        }
        if (f.has("lengthType")) {
            val lt = f.getString("lengthType")
            if (lt !in VALID_LENGTH_TYPES) {
                errors.add("$owner field $fName has invalid lengthType '$lt'")
            }
        }
        if (f.optBoolean("strictEnum", false)) {
            val enumObj = f.optJSONObject("enum")
            if (enumObj == null || enumObj.length() == 0) {
                errors.add("$owner field $fName sets strictEnum without a non-empty enum")
            }
        }
        if (f.has("maxLength") && f.getInt("maxLength") < 0) {
            errors.add("$owner field $fName has negative maxLength")
        }
        if (f.has("payloadKind")) {
            val pk = f.getString("payloadKind")
            if (pk != "operations") {
                errors.add("$owner field $fName has invalid payloadKind '$pk'")
            }
            if (fType != "bytes") {
                errors.add("$owner field $fName sets payloadKind on non-bytes type '$fType'")
            }
        }
        if (f.has("conditionalTypes")) {
            val ctArr = f.getJSONArray("conditionalTypes")
            if (ctArr.length() == 0) {
                errors.add("$owner field $fName has empty conditionalTypes")
            }
            for (cIdx in 0 until ctArr.length()) {
                val cObj = ctArr.getJSONObject(cIdx)
                val whenExpr = cObj.optString("when", "")
                val cType = cObj.optString("type", "")
                if (whenExpr.isBlank()) {
                    errors.add("$owner field $fName conditionalTypes[$cIdx] has blank when")
                }
                if (!isDeclaredSchemaType(cType, declaredTypes)) {
                    errors.add(
                        "$owner field $fName conditionalTypes[$cIdx] has undeclared type '$cType'"
                    )
                }
            }
        }
        if (f.has("bits")) {
            val bitsArr = f.getJSONArray("bits")
            for (bIdx in 0 until bitsArr.length()) {
                val bObj = bitsArr.getJSONObject(bIdx)
                val bName = bObj.getString("name")
                val bRange = bObj.optString("range", "")
                if (!isValidBitRange(bRange)) {
                    errors.add("$owner field $fName bit $bName has invalid bit range '$bRange'")
                }
                if (bObj.optBoolean("strictEnum", false)) {
                    val bEnum = bObj.optJSONObject("enum")
                    if (bEnum == null || bEnum.length() == 0) {
                        errors.add(
                            "$owner field $fName bit $bName sets strictEnum without a non-empty enum"
                        )
                    }
                }
                if (bObj.has("whenBitSet")) {
                    val wbs = bObj.getJSONObject("whenBitSet")
                    val bitIdx = wbs.optInt("bit", -1)
                    val wbsRange = wbs.optString("range", "")
                    if (bitIdx !in 0..31) {
                        errors.add(
                            "$owner field $fName bit $bName has invalid whenBitSet.bit $bitIdx (must be 0..31)"
                        )
                    }
                    if (!isValidBitRange(wbsRange)) {
                        errors.add(
                            "$owner field $fName bit $bName has invalid whenBitSet.range '$wbsRange'"
                        )
                    }
                }
            }
        }
    }

    private fun parseField(fieldObj: JSONObject): SchemaField {
        val fieldName = fieldObj.getString("name")
        val fieldType = fieldObj.getString("type")
        val fieldDesc = fieldObj.optString("description", "")
        val enumValues =
            if (fieldObj.has("enum")) {
                val enumObj = fieldObj.getJSONObject("enum")
                val map = mutableMapOf<String, Int>()
                for (enumKey in enumObj.keys()) {
                    map[enumKey] = enumObj.getInt(enumKey)
                }
                map
            } else null

        val addedVersion =
            if (fieldObj.has("addedVersion")) fieldObj.getInt("addedVersion") else null
        val defaultValue =
            if (fieldObj.has("defaultValue")) fieldObj.getString("defaultValue") else null

        val bits =
            if (fieldObj.has("bits")) {
                val bitsArray = fieldObj.getJSONArray("bits")
                (0 until bitsArray.length()).map { idx ->
                    val bObj = bitsArray.getJSONObject(idx)
                    val bitEnums =
                        if (bObj.has("enum")) {
                            val bEnumObj = bObj.getJSONObject("enum")
                            val map = mutableMapOf<String, Int>()
                            for (eKey in bEnumObj.keys()) {
                                map[eKey] = bEnumObj.getInt(eKey)
                            }
                            map
                        } else null
                    val whenBitSet =
                        if (bObj.has("whenBitSet")) {
                            val wObj = bObj.getJSONObject("whenBitSet")
                            SchemaConditionalBitRange(
                                bit = wObj.getInt("bit"),
                                range = wObj.getString("range"),
                            )
                        } else null
                    SchemaBitField(
                        name = bObj.getString("name"),
                        range = bObj.getString("range"),
                        role = if (bObj.has("role")) bObj.getString("role") else null,
                        idKind = if (bObj.has("idKind")) bObj.getString("idKind") else null,
                        derivedFrom =
                            if (bObj.has("derivedFrom")) bObj.getString("derivedFrom") else null,
                        signed = bObj.optBoolean("signed", false),
                        description = bObj.optString("description", ""),
                        enumValues = bitEnums,
                        strictEnum = bObj.optBoolean("strictEnum", false),
                        whenBitSet = whenBitSet,
                    )
                }
            } else null

        val unpackBits = fieldObj.optBoolean("unpackBits", true)
        val lengthType = if (fieldObj.has("lengthType")) fieldObj.getString("lengthType") else null
        val countFrom = if (fieldObj.has("countFrom")) fieldObj.getString("countFrom") else null
        val derivedFrom =
            if (fieldObj.has("derivedFrom")) fieldObj.getString("derivedFrom") else null
        val presentIf = if (fieldObj.has("presentIf")) fieldObj.getString("presentIf") else null
        val omitIf = if (fieldObj.has("omitIf")) fieldObj.opt("omitIf")?.toString() else null
        val role = if (fieldObj.has("role")) fieldObj.getString("role") else null
        val idKind = if (fieldObj.has("idKind")) fieldObj.getString("idKind") else null
        val encoding = if (fieldObj.has("encoding")) fieldObj.getString("encoding") else null
        val offset = if (fieldObj.has("offset")) fieldObj.optString("offset", null) else null
        val lookupBit = if (fieldObj.has("lookupBit")) fieldObj.getInt("lookupBit") else null
        val strictEnum = fieldObj.optBoolean("strictEnum", false)
        val maxLength = if (fieldObj.has("maxLength")) fieldObj.getInt("maxLength") else null
        val javaLimit = if (fieldObj.has("javaLimit")) fieldObj.getString("javaLimit") else null
        val payloadKind =
            if (fieldObj.has("payloadKind")) fieldObj.getString("payloadKind") else null
        val conditionalTypes =
            if (fieldObj.has("conditionalTypes")) {
                val ctArray = fieldObj.getJSONArray("conditionalTypes")
                (0 until ctArray.length()).map { idx ->
                    val ctObj = ctArray.getJSONObject(idx)
                    SchemaConditionalType(
                        condition = ctObj.getString("when"),
                        type = ctObj.getString("type"),
                    )
                }
            } else emptyList()
        val conditionalRoles =
            if (fieldObj.has("conditionalRoles")) {
                val crArray = fieldObj.getJSONArray("conditionalRoles")
                (0 until crArray.length()).map { idx ->
                    val crObj = crArray.getJSONObject(idx)
                    SchemaConditionalRole(
                        condition = crObj.getString("when"),
                        role = crObj.getString("role"),
                        idKind = if (crObj.has("idKind")) crObj.getString("idKind") else null,
                    )
                }
            } else emptyList()

        return SchemaField(
            name = fieldName,
            type = fieldType,
            description = fieldDesc,
            enumValues = enumValues,
            addedVersion = addedVersion,
            defaultValue = defaultValue,
            bits = bits,
            unpackBits = unpackBits,
            lengthType = lengthType,
            countFrom = countFrom,
            derivedFrom = derivedFrom,
            presentIf = presentIf,
            omitIf = omitIf,
            role = role,
            idKind = idKind,
            encoding = encoding,
            offset = offset,
            lookupBit = lookupBit,
            strictEnum = strictEnum,
            maxLength = maxLength,
            javaLimit = javaLimit,
            payloadKind = payloadKind,
            conditionalTypes = conditionalTypes,
            conditionalRoles = conditionalRoles,
        )
    }
}
