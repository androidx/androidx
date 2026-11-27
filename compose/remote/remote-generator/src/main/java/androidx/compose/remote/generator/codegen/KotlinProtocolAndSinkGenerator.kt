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

package androidx.compose.remote.generator.codegen

import androidx.compose.remote.generator.AstProperty
import androidx.compose.remote.generator.OperationSchema
import androidx.compose.remote.generator.SchemaBitField
import androidx.compose.remote.generator.SchemaOperation
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.INT_ARRAY
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT

internal class KotlinProtocolAndSinkGenerator(private val schema: OperationSchema) {

    fun generateProtocolConstants(): String {
        val fileBuilder =
            KotlinGenerator.newFileBuilder("RemoteProtocol")
                .addType(buildRcProfilesObject())
                .addType(buildFloatRpnObject())
                .addType(buildIntRpnObject())
                .addType(buildPaintCommandTagsObject())
                .addType(buildPaintConstantsObject())
                .addType(buildPathCommandTagsObject())
                .addType(buildRemoteContextIdsObject())
        KotlinWireCodecsGenerator(schema).appendProtocolConstantsTo(fileBuilder)
        return KotlinGenerator.renderFile(fileBuilder.build())
    }

    fun generateOperationSink(): String {
        val sinkOps = schema.operations.filter { it.id != 0 }
        val fileBuilder =
            KotlinGenerator.newFileBuilder("RemoteOperationSink")
                .addType(buildSinkInterface(sinkOps))
                .addFunction(buildEmitToExtension(sinkOps))
                .addFunction(buildEmitExtension())
                .addType(buildWireBufferOperationSink(sinkOps))
                .addType(buildNodeListOperationSink(sinkOps))
        return KotlinGenerator.renderFile(fileBuilder.build())
    }

    private fun buildRcProfilesObject(): TypeSpec {
        val builder =
            TypeSpec.objectBuilder("RcProfiles")
                .addKdoc(
                    "%L",
                    "Capability profile bitmasks and opcode validation tables for RemoteCompose documents.",
                )
                .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC)

        val profileConsts =
            listOf(
                "PROFILE_BASELINE" to "0",
                "PROFILE_EXPERIMENTAL" to "0x1",
                "PROFILE_DEPRECATED" to "0x2",
                "PROFILE_OEM" to "0x4",
                "PROFILE_LOW_POWER" to "0x8",
                "PROFILE_WIDGETS" to "0x100",
                "PROFILE_ANDROIDX" to "0x200",
                "PROFILE_ANDROID_NATIVE" to "0x400",
                "PROFILE_WEAR_WIDGETS" to "0x800",
            )
        for ((name, value) in profileConsts) {
            builder.addProperty(
                PropertySpec.builder(name, INT, KModifier.PUBLIC, KModifier.CONST)
                    .initializer("%L", value)
                    .build()
            )
        }

        val v6Ids = schema.operations.filter { it.addedVersion <= 6 }.map { it.id }
        val v7BaselineIds = schema.operations.filter { "baseline" in it.profiles }.map { it.id }
        builder.addProperty(buildIntSetProperty("V6_OPCODES", v6Ids))
        builder.addProperty(buildIntSetProperty("V7_BASELINE_OPCODES", v7BaselineIds))

        val isSupportedKdoc =
            """
            Returns true if [opcode] is supported at [apiLevel] under the given [profiles] bitmask.

            Matches `Operations.valid(opcode, apiLevel, profiles)` in `remote-core`: when multiple
            platform profiles ([PROFILE_ANDROIDX], [PROFILE_WIDGETS]) are set in [profiles], only the
            intersection of operations supported across all active profiles is accepted.
            """
                .trimIndent()

        val nonBaselineGroups =
            schema.operations
                .filter { "baseline" !in it.profiles && "oem" !in it.profiles }
                .groupBy { it.profiles }

        val isSupportedFun =
            FunSpec.builder("isOpcodeSupported")
                .addKdoc("%L", isSupportedKdoc)
                .addModifiers(KModifier.PUBLIC)
                .addParameter("opcode", INT)
                .addParameter("apiLevel", INT)
                .addParameter("profiles", INT)
                .returns(BOOLEAN)
                .apply {
                    beginControlFlow("if (apiLevel <= 6)")
                    addStatement("return opcode in V6_OPCODES")
                    endControlFlow()
                    beginControlFlow("if (opcode in V7_BASELINE_OPCODES)")
                    addStatement("return true")
                    endControlFlow()
                    addStatement("val hasAndroidx = (profiles and PROFILE_ANDROIDX) != 0")
                    addStatement("val hasWidgets = (profiles and PROFILE_WIDGETS) != 0")
                    beginControlFlow("if (!hasAndroidx && !hasWidgets)")
                    addStatement("return false")
                    endControlFlow()
                    addStatement("val hasExperimental = (profiles and PROFILE_EXPERIMENTAL) != 0")
                    addStatement("val hasDeprecated = (profiles and PROFILE_DEPRECATED) != 0")
                    beginControlFlow("return when (opcode)")
                    for ((profileList, ops) in nonBaselineGroups) {
                        val ids = ops.map { it.id }
                        val condParts = mutableListOf<String>()
                        if ("androidx" !in profileList) {
                            condParts.add("!hasAndroidx")
                        }
                        if ("widgets" !in profileList) {
                            condParts.add("!hasWidgets")
                        }
                        if ("experimental" in profileList) {
                            condParts.add("hasExperimental")
                        }
                        if ("deprecated" in profileList) {
                            condParts.add("hasDeprecated")
                        }
                        val condExpr =
                            if (condParts.isEmpty()) "true" else condParts.joinToString(" && ")
                        addStatement("%L -> %L", ids.joinToString(", "), condExpr)
                    }
                    addStatement("else -> false")
                    endControlFlow()
                }
                .build()

        val supportedOpcodesFun =
            FunSpec.builder("supportedOpcodes")
                .addKdoc(
                    "%L",
                    "Returns the set of opcodes supported at [apiLevel] under [profiles].",
                )
                .addModifiers(KModifier.PUBLIC)
                .addParameter("apiLevel", INT)
                .addParameter("profiles", INT)
                .returns(SET.parameterizedBy(INT))
                .addStatement("val result = LinkedHashSet<Int>()")
                .beginControlFlow("for (opcode in ALL_KNOWN_OPCODES)")
                .beginControlFlow("if (isOpcodeSupported(opcode, apiLevel, profiles))")
                .addStatement("result.add(opcode)")
                .endControlFlow()
                .endControlFlow()
                .addStatement("return result")
                .build()

        builder.addFunction(isSupportedFun)
        builder.addFunction(supportedOpcodesFun)
        builder.addProperty(
            buildIntSetProperty("ALL_KNOWN_OPCODES", schema.operations.map { it.id })
        )
        return builder.build()
    }

    private fun buildIntSetProperty(name: String, ids: List<Int>): PropertySpec =
        PropertySpec.builder(name, SET.parameterizedBy(INT), KModifier.PRIVATE)
            .initializer("setOf(%L)", ids.joinToString(", "))
            .build()

    private fun buildFloatRpnObject(): TypeSpec {
        val rpn = checkNotNull(schema.rpnOperators) { "Missing rpnOperators in schema" }
        val maxV6 = rpn.operators.filter { it.addedVersion <= 6 }.maxOfOrNull { it.id } ?: 50
        val maxId = rpn.operators.maxOfOrNull { it.id } ?: 79

        val kdoc =
            """
            Floating-point RPN expression operator NaN tokens matching `AnimatedFloatExpression`.

            Each operator is encoded as a negative quiet NaN `Float.fromBits((OFFSET + id) or -0x800000)`.
            Always compare RPN float tokens using [Float.toRawBits] rather than `==`.
            """
                .trimIndent()

        val builder =
            TypeSpec.objectBuilder("FloatRpn")
                .addKdoc("%L", kdoc)
                .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC)
                .addProperty(
                    PropertySpec.builder("OFFSET", INT, KModifier.PUBLIC, KModifier.CONST)
                        .initializer("0x%L", rpn.offset.toString(16).uppercase())
                        .build()
                )
                .addProperty(
                    PropertySpec.builder(
                            "API_LEVEL_6_MAX_ID",
                            INT,
                            KModifier.PUBLIC,
                            KModifier.CONST,
                        )
                        .initializer("%L", maxV6)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("MAX_ID", INT, KModifier.PUBLIC, KModifier.CONST)
                        .initializer("%L", maxId)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("NAN_PREFIX", INT, KModifier.PRIVATE, KModifier.CONST)
                        .initializer("%L", "-0x800000")
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("NAN_EXPONENT", INT, KModifier.PRIVATE, KModifier.CONST)
                        .initializer(
                            "%L",
                            "0x${(schema.idEncoding?.nanExponent ?: 0x7F800000).toUInt().toString(16).uppercase()}",
                        )
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("NO_ID", INT, KModifier.PRIVATE, KModifier.CONST)
                        .initializer("%L", schema.idEncoding?.noId ?: -1)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("PAYLOAD_MASK", INT, KModifier.PRIVATE, KModifier.CONST)
                        .initializer("%L", "0x3FFFFF")
                        .build()
                )

        for (op in rpn.operators) {
            builder.addProperty(
                PropertySpec.builder("ID_${op.name}", INT, KModifier.PUBLIC, KModifier.CONST)
                    .initializer("%L", op.id)
                    .build()
            )
        }
        if ("DEG_TO_RAD" in rpn.operatorsByName && "DEG" !in rpn.operatorsByName) {
            builder.addProperty(
                PropertySpec.builder("ID_DEG", INT, KModifier.PUBLIC, KModifier.CONST)
                    .initializer("ID_DEG_TO_RAD")
                    .build()
            )
        }
        if ("RAD_TO_DEG" in rpn.operatorsByName && "RAD" !in rpn.operatorsByName) {
            builder.addProperty(
                PropertySpec.builder("ID_RAD", INT, KModifier.PUBLIC, KModifier.CONST)
                    .initializer("ID_RAD_TO_DEG")
                    .build()
            )
        }

        for (op in rpn.operators) {
            builder.addProperty(
                PropertySpec.builder(op.name, FLOAT, KModifier.PUBLIC)
                    .initializer("Float.fromBits((OFFSET + %L) or NAN_PREFIX)", op.id)
                    .build()
            )
        }
        if ("DEG_TO_RAD" in rpn.operatorsByName && "DEG" !in rpn.operatorsByName) {
            builder.addProperty(
                PropertySpec.builder("DEG", FLOAT, KModifier.PUBLIC)
                    .initializer("DEG_TO_RAD")
                    .build()
            )
        }
        if ("RAD_TO_DEG" in rpn.operatorsByName && "RAD" !in rpn.operatorsByName) {
            builder.addProperty(
                PropertySpec.builder("RAD", FLOAT, KModifier.PUBLIC)
                    .initializer("RAD_TO_DEG")
                    .build()
            )
        }

        builder.addFunction(
            FunSpec.builder("tokenFromId")
                .addKdoc(
                    "%L",
                    "Encodes a float RPN operator [id] (`1..MAX_ID`) as a NaN [Float] token.",
                )
                .addModifiers(KModifier.PUBLIC)
                .addParameter("id", INT)
                .returns(FLOAT)
                .beginControlFlow("require(id in 1..MAX_ID && id != 68 && id != 69)")
                .addStatement("%L", "\"Invalid float RPN operator id: \$id\"")
                .endControlFlow()
                .addStatement("return Float.fromBits((OFFSET + id) or NAN_PREFIX)")
                .build()
        )

        builder.addFunction(
            FunSpec.builder("isOperator")
                .addKdoc(
                    "%L",
                    "Returns true if [token] is a NaN-encoded float RPN operator.",
                )
                .addModifiers(KModifier.PUBLIC)
                .addParameter("token", FLOAT)
                .returns(BOOLEAN)
                .addStatement("val bits = token.toRawBits()")
                .addStatement("if ((bits and NAN_EXPONENT) != NAN_EXPONENT) return false")
                .addStatement("val id = (bits and PAYLOAD_MASK) - OFFSET")
                .addStatement("return id in 1..MAX_ID && id != 68 && id != 69")
                .build()
        )

        builder.addFunction(
            FunSpec.builder("operatorId")
                .addKdoc(
                    "%L",
                    "Extracts the operator ID (`1..MAX_ID`) from [token], or `-1` if not an operator.",
                )
                .addModifiers(KModifier.PUBLIC)
                .addParameter("token", FLOAT)
                .returns(INT)
                .addStatement("if (!isOperator(token)) return NO_ID")
                .addStatement("return (token.toRawBits() and PAYLOAD_MASK) - OFFSET")
                .build()
        )

        builder.addFunction(
            FunSpec.builder("isSupportedAtApiLevel")
                .addKdoc(
                    "%L",
                    "Returns true if [token] (if an RPN operator) is supported at [apiLevel].",
                )
                .addModifiers(KModifier.PUBLIC)
                .addParameter("token", FLOAT)
                .addParameter("apiLevel", INT)
                .returns(BOOLEAN)
                .addStatement("val id = operatorId(token)")
                .addStatement("if (id == NO_ID) return true")
                .addStatement(
                    "return if (apiLevel <= 6) id <= API_LEVEL_6_MAX_ID else id <= MAX_ID"
                )
                .build()
        )

        return builder.build()
    }

    private fun buildIntRpnObject(): TypeSpec {
        val intRpn = checkNotNull(schema.intRpnOperators) { "Missing intRpnOperators in schema" }
        val maxId = intRpn.operators.maxOfOrNull { it.id } ?: 25
        val kdoc =
            """
            Integer RPN expression operator tokens matching `IntegerExpressionEvaluator`.

            In `IntegerExpression`, bit `i` of `mask` (`1 shl i`) is `1` when `srcValues[i]` is an operator
            or variable ID, and `0` when `srcValues[i]` is a literal 32-bit integer. Note that integer RPN
            expressions have a maximum length of [MAX_TOKENS] (32) elements.
            """
                .trimIndent()

        val builder =
            TypeSpec.objectBuilder("IntRpn")
                .addKdoc("%L", kdoc)
                .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC)
                .addProperty(
                    PropertySpec.builder("OFFSET", INT, KModifier.PUBLIC, KModifier.CONST)
                        .initializer("0x%L", intRpn.offset.toString(16).uppercase())
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("MAX_ID", INT, KModifier.PUBLIC, KModifier.CONST)
                        .initializer("%L", maxId)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("MAX_TOKENS", INT, KModifier.PUBLIC, KModifier.CONST)
                        .initializer("%L", 32)
                        .build()
                )

        for (op in intRpn.operators) {
            builder.addProperty(
                PropertySpec.builder(op.name, INT, KModifier.PUBLIC, KModifier.CONST)
                    .initializer("OFFSET + %L", op.id)
                    .build()
            )
        }

        builder.addFunction(
            FunSpec.builder("isOperatorCode")
                .addKdoc(
                    "%L",
                    "Returns true if [value] is an integer RPN operator code (`OFFSET + 1..OFFSET + MAX_ID`).",
                )
                .addModifiers(KModifier.PUBLIC)
                .addParameter("value", INT)
                .returns(BOOLEAN)
                .addStatement("return value in (OFFSET + 1)..(OFFSET + MAX_ID)")
                .build()
        )

        return builder.build()
    }

    private fun buildPaintCommandTagsObject(): TypeSpec {
        val builder =
            TypeSpec.objectBuilder("PaintCommandTags")
                .addKdoc(
                    "%L",
                    "Command tag IDs inside `PaintDataNode.words` matching `PaintBundle`.",
                )
                .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC)

        for ((id, cmd) in schema.paintCommands.entries.sortedBy { it.key }) {
            val constName = SchemaOperation.toScreamingSnakeCase(cmd.name)
            builder.addProperty(
                PropertySpec.builder(constName, INT, KModifier.PUBLIC, KModifier.CONST)
                    .initializer("%L", id)
                    .build()
            )
        }
        return builder.build()
    }

    private fun buildPaintConstantsObject(): TypeSpec {
        val builder =
            TypeSpec.objectBuilder("PaintConstants")
                .addKdoc(
                    "%L",
                    "Wire enum constants and bitmasks for `PaintDataNode` (`PaintBundle`) generated from `operations.json`.",
                )
                .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC)

        val commandsByName = schema.paintCommands.values.associateBy { it.name }

        fun addEnumConsts(commandName: String, fieldName: String, prefix: String) {
            val enumMap =
                commandsByName[commandName]
                    ?.fields
                    ?.firstOrNull { it.name == fieldName }
                    ?.enumValues ?: return
            for ((enumKey, enumVal) in enumMap.entries.sortedBy { it.value }) {
                val constName =
                    if (prefix == "BLEND_MODE_" && enumKey == "PORTER_MODE_ADD") {
                        "PORTER_MODE_ADD"
                    } else {
                        "$prefix$enumKey"
                    }
                builder.addProperty(
                    PropertySpec.builder(constName, INT, KModifier.PUBLIC, KModifier.CONST)
                        .initializer("%L", enumVal)
                        .build()
                )
            }
        }

        addEnumConsts("Style", "style", "STYLE_")
        addEnumConsts("StrokeCap", "cap", "CAP_")
        addEnumConsts("StrokeJoin", "join", "JOIN_")
        addEnumConsts("Gradient", "tileMode", "TILE_")
        addEnumConsts("Typeface", "fontId", "FONT_TYPE_")
        addEnumConsts("BlendMode", "mode", "BLEND_MODE_")
        addEnumConsts("Gradient", "type", "GRADIENT_")

        builder.addProperty(
            PropertySpec.builder("TYPEFACE_WEIGHT_MASK", INT, KModifier.PUBLIC, KModifier.CONST)
                .addKdoc(
                    "%L",
                    "Bitmask for the 10-bit font weight encoded in `TYPEFACE` and `FALLBACK_TYPEFACE`'s upper 16 bits.",
                )
                .initializer("0x3FF")
                .build()
        )
        builder.addProperty(
            PropertySpec.builder("TYPEFACE_FONT_DATA_BIT", INT, KModifier.PUBLIC, KModifier.CONST)
                .addKdoc(
                    "%L",
                    "Flag bit in `TYPEFACE`'s upper 16 bits indicating an embedded `FontData` ID.",
                )
                .initializer("1 shl 10")
                .build()
        )
        builder.addProperty(
            PropertySpec.builder("TYPEFACE_ITALIC_BIT", INT, KModifier.PUBLIC, KModifier.CONST)
                .addKdoc(
                    "%L",
                    "Flag bit in `TYPEFACE` and `FALLBACK_TYPEFACE`'s upper 16 bits indicating italic style.",
                )
                .initializer("1 shl 11")
                .build()
        )

        val blendEnum =
            commandsByName["BlendMode"]
                ?.fields
                ?.firstOrNull { it.name == "mode" }
                ?.enumValues
                .orEmpty()
        if (blendEnum.isNotEmpty()) {
            builder.addFunction(
                FunSpec.builder("isSrcOverCompatibleBlendMode")
                    .addAnnotation(JvmStatic::class)
                    .addModifiers(KModifier.PUBLIC)
                    .addParameter("code", INT)
                    .returns(BOOLEAN)
                    .addKdoc(
                        "%L",
                        "Returns true if [code] behaves like [BLEND_MODE_SRC_OVER] (either [BLEND_MODE_SRC_OVER] or [BLEND_MODE_NULL]).",
                    )
                    .addStatement("return code == BLEND_MODE_SRC_OVER || code == BLEND_MODE_NULL")
                    .build()
            )
            val blendNameFun =
                FunSpec.builder("blendModeName")
                    .addAnnotation(JvmStatic::class)
                    .addModifiers(KModifier.PUBLIC)
                    .addParameter("code", INT)
                    .returns(STRING)
                    .addKdoc(
                        "%L",
                        "Formats a wire blend mode [code] as its name matching `PaintBundle.blendModeString`.",
                    )
                    .beginControlFlow("return when (code)")
            for ((enumKey, _) in blendEnum.entries.sortedBy { it.value }) {
                val constName =
                    if (enumKey == "PORTER_MODE_ADD") "PORTER_MODE_ADD" else "BLEND_MODE_$enumKey"
                val label =
                    when (enumKey) {
                        "NULL" -> "null"
                        "PORTER_MODE_ADD" -> "ADD"
                        else -> enumKey
                    }
                blendNameFun.addStatement("%L -> %S", constName, label)
            }
            blendNameFun.addStatement("else -> %S", "null")
            blendNameFun.endControlFlow()
            builder.addFunction(blendNameFun.build())
        }

        val tileEnum =
            commandsByName["Gradient"]
                ?.fields
                ?.firstOrNull { it.name == "tileMode" }
                ?.enumValues
                .orEmpty()
        if (tileEnum.isNotEmpty()) {
            val tileNameFun =
                FunSpec.builder("tileModeName")
                    .addAnnotation(JvmStatic::class)
                    .addModifiers(KModifier.PUBLIC)
                    .addParameter("code", INT)
                    .returns(STRING)
                    .addKdoc("%L", "Formats a wire tile mode [code] as its human-readable name.")
                    .beginControlFlow("return when (code)")
            for ((enumKey, _) in tileEnum.entries.sortedBy { it.value }) {
                tileNameFun.addStatement("TILE_%L -> %S", enumKey, enumKey)
            }
            tileNameFun.addStatement("else -> %S", "UNKNOWN")
            tileNameFun.endControlFlow()
            builder.addFunction(tileNameFun.build())
        }

        return builder.build()
    }

    private fun buildPathCommandTagsObject(): TypeSpec {
        val builder =
            TypeSpec.objectBuilder("PathCommandTags")
                .addKdoc(
                    "%L",
                    "Path segment command IDs and NaN tags inside `PathDataNode` and " +
                        "`PathAppendNode` (where `RESET` is specific to `PathAppend`).",
                )
                .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC)
                .addProperty(
                    PropertySpec.builder("NAN_PREFIX", INT, KModifier.PRIVATE, KModifier.CONST)
                        .initializer("%L", "-0x800000")
                        .build()
                )

        val sortedCommands = schema.pathCommands.entries.sortedBy { it.key }
        for ((id, cmd) in sortedCommands) {
            val constName = SchemaOperation.toScreamingSnakeCase(cmd.name)
            builder.addProperty(
                PropertySpec.builder(constName, INT, KModifier.PUBLIC, KModifier.CONST)
                    .initializer("%L", id)
                    .build()
            )
        }
        for ((id, cmd) in sortedCommands) {
            val constName = SchemaOperation.toScreamingSnakeCase(cmd.name)
            builder.addProperty(
                PropertySpec.builder("${constName}_NAN", FLOAT, KModifier.PUBLIC)
                    .initializer("Float.fromBits(%L or NAN_PREFIX)", id)
                    .build()
            )
        }
        return builder.build()
    }

    private fun buildRemoteContextIdsObject(): TypeSpec {
        val builder =
            TypeSpec.objectBuilder("RemoteContextIds")
                .addKdoc(
                    "%L",
                    "Built-in system context variable IDs and NaN handles matching `RemoteContext`.",
                )
                .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC)

        for (sv in schema.systemVariables) {
            builder.addProperty(
                PropertySpec.builder("ID_${sv.name}", INT, KModifier.PUBLIC, KModifier.CONST)
                    .initializer("%L", sv.id)
                    .build()
            )
        }
        for (sv in schema.systemVariables) {
            if (sv.type == "float") {
                builder.addProperty(
                    PropertySpec.builder(
                            "FLOAT_${sv.name}",
                            KotlinGenerator.MAYBE_VARIABLE_FLOAT_CLASS,
                            KModifier.PUBLIC,
                        )
                        .initializer(
                            "%T.variable(ID_%L)",
                            KotlinGenerator.MAYBE_VARIABLE_FLOAT_CLASS,
                            sv.name,
                        )
                        .build()
                )
            } else if (sv.type == "long") {
                builder.addProperty(
                    PropertySpec.builder("INT_${sv.name}", LONG, KModifier.PUBLIC, KModifier.CONST)
                        .initializer("ID_%L.toLong() + 0x100000000L", sv.name)
                        .build()
                )
            }
        }
        val timeVars = schema.systemVariables.filter { it.isTime }.sortedBy { it.id }
        val isTimeBody =
            if (timeVars.isEmpty()) {
                "return false"
            } else {
                val minVar = timeVars.first()
                val maxVar = timeVars.last()
                val isContiguous =
                    timeVars.size == (maxVar.id - minVar.id + 1) &&
                        timeVars.indices.all { timeVars[it].id == minVar.id + it }
                if (isContiguous) {
                    "return id in ID_${minVar.name}..ID_${maxVar.name}"
                } else {
                    "return id in setOf(${timeVars.joinToString(", ") { "ID_${it.name}" }})"
                }
            }
        builder.addFunction(
            FunSpec.builder("isTime")
                .addKdoc(
                    "%L",
                    "Returns true if [id] is marked `isTime` in `operations.json` (matching legacy `RemoteContext.isTime`).",
                )
                .addModifiers(KModifier.PUBLIC)
                .addParameter("id", INT)
                .returns(BOOLEAN)
                .addStatement("%L", isTimeBody)
                .build()
        )
        builder.addFunction(
            FunSpec.builder("isTime")
                .addKdoc(
                    "%L",
                    "Returns true if [floatValue] is a variable reference whose ID satisfies " +
                        "[isTime] (`1..12`). Returns `false` for literal floats and non-ID NaNs.",
                )
                .addModifiers(KModifier.PUBLIC)
                .addParameter("floatValue", KotlinGenerator.MAYBE_VARIABLE_FLOAT_CLASS)
                .returns(BOOLEAN)
                .addStatement("return floatValue.isVariable && isTime(floatValue.variableId)")
                .build()
        )
        return builder.build()
    }

    private fun buildSinkInterface(sinkOps: List<SchemaOperation>): TypeSpec {
        val kdoc =
            """
            Typed low-level operation sink for emitting RemoteCompose wire operations into either a streaming
            [WireBuffer] ([WireBufferOperationSink]) or an AST list ([NodeListOperationSink]).
            """
                .trimIndent()

        val builder =
            TypeSpec.interfaceBuilder("RemoteOperationSink")
                .addKdoc("%L", kdoc)
                .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC)

        for (op in sinkOps) {
            val cleanMethodName = cleanMethodNameForOp(op)
            builder.addFunction(
                buildSinkFunBuilder(
                        cleanMethodName = cleanMethodName,
                        props = op.astProperties,
                        includeDefaults = true,
                        modifiers = listOf(KModifier.PUBLIC, KModifier.ABSTRACT),
                    )
                    .build()
            )

            if (op.fields.any { it.bits != null && !it.unpackBits }) {
                buildUnpackedDefaultOverloadIfDistinct(op, cleanMethodName)?.let {
                    builder.addFunction(it)
                }
            }
        }
        return builder.build()
    }

    private fun buildUnpackedDefaultOverloadIfDistinct(
        op: SchemaOperation,
        cleanMethodName: String,
    ): FunSpec? {
        val unpackedParams = mutableListOf<AstProperty>()
        for (f in op.fields) {
            if (f.derivedFrom != null) continue
            val bits = f.bits
            if (bits != null) {
                for (b in bits) {
                    if (b.derivedFrom != null) continue
                    unpackedParams.add(
                        AstProperty(
                            name = b.name,
                            type = "int",
                            description = b.description.ifEmpty { f.description },
                            isNullable = false,
                            defaultValue = "0",
                        )
                    )
                }
            } else {
                val isNullable =
                    f.presentIf != null && !f.type.endsWith("[]") && f.defaultValue == null
                unpackedParams.add(
                    AstProperty(
                        name = f.name,
                        type = f.type,
                        description = f.description,
                        isNullable = isNullable,
                        defaultValue = f.defaultValue,
                    )
                )
            }
        }
        if (op.sparseParameters != null) {
            for (sp in op.sparseParameters) {
                unpackedParams.add(
                    AstProperty(
                        name = sp.name,
                        type = sp.type,
                        description = sp.description,
                        isNullable = true,
                        defaultValue = null,
                    )
                )
            }
        }

        val primaryTypes =
            op.astProperties.map { KotlinGenerator.mapToKotlinType(it.type, it.isNullable) }
        val unpackedTypes = unpackedParams.map {
            KotlinGenerator.mapToKotlinType(it.type, it.isNullable)
        }
        if (primaryTypes == unpackedTypes) return null

        val fnBuilder =
            buildSinkFunBuilder(
                cleanMethodName = cleanMethodName,
                props = unpackedParams,
                includeDefaults = true,
                modifiers = listOf(KModifier.PUBLIC),
            )

        val callArgs = mutableListOf<String>()
        for (f in op.fields) {
            if (f.derivedFrom != null) continue
            val bits = f.bits
            if (bits != null && !f.unpackBits) {
                val terms = mutableListOf<Pair<String, SchemaBitField>>()
                for (b in bits) {
                    if (b.derivedFrom == null) {
                        terms.add(KotlinGenerator.sanitizeIdentifier(b.name) to b)
                    }
                }
                val packedExpr = CodegenExprUtils.packBitsExpr(terms, ExprLang.KOTLIN)
                val fName = KotlinGenerator.sanitizeIdentifier(f.name)
                val packedVar = "packed_${KotlinGenerator.cleanIdentifier(f.name)}"
                fnBuilder.addStatement("val %L = %L", packedVar, packedExpr)
                val argValue =
                    when (f.type) {
                        "short" -> "$packedVar.toShort()"
                        "byte" -> "$packedVar.toByte()"
                        else -> packedVar
                    }
                callArgs.add("$fName = $argValue")
            } else if (bits != null && f.unpackBits) {
                for (b in bits) {
                    if (b.derivedFrom == null) {
                        val bName = KotlinGenerator.sanitizeIdentifier(b.name)
                        callArgs.add("$bName = $bName")
                    }
                }
            } else {
                val fName = KotlinGenerator.sanitizeIdentifier(f.name)
                callArgs.add("$fName = $fName")
            }
        }
        if (op.sparseParameters != null) {
            for (sp in op.sparseParameters) {
                val spName = KotlinGenerator.sanitizeIdentifier(sp.name)
                callArgs.add("$spName = $spName")
            }
        }

        fnBuilder.addStatement("%N(%L)", cleanMethodName, callArgs.joinToString(", "))
        return fnBuilder.build()
    }

    private fun buildEmitToExtension(sinkOps: List<SchemaOperation>): FunSpec =
        FunSpec.builder("emitTo")
            .addKdoc(
                "%L",
                "Replays this [OperationNode] into [sink] without manual `when` dispatch in callers.",
            )
            .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
            .addModifiers(KModifier.PUBLIC)
            .receiver(KotlinGenerator.OPERATION_NODE_CLASS)
            .addParameter("sink", REMOTE_OPERATION_SINK_CLASS)
            .apply {
                beginControlFlow("when (this)")
                addStatement(
                    "is %T -> throw %T(%S)",
                    ClassName(KotlinGenerator.DOCUMENT_PACKAGE, "HeaderNode"),
                    IllegalArgumentException::class,
                    "HeaderNode must be written via DocumentHeader, not RemoteOperationSink",
                )
                for (op in sinkOps) {
                    val methodName = methodNameForOp(op)
                    val args =
                        op.astProperties.joinToString(", ") {
                            KotlinGenerator.sanitizeIdentifier(it.name)
                        }
                    addStatement(
                        "is %T -> sink.%L(%L)",
                        ClassName(KotlinGenerator.DOCUMENT_PACKAGE, "${op.name}Node"),
                        methodName,
                        args,
                    )
                }
                addStatement(
                    "else -> error(%L)",
                    "\"Unsupported OperationNode: \${this::class.java.name}\"",
                )
                endControlFlow()
            }
            .build()

    private fun buildEmitExtension(): FunSpec =
        FunSpec.builder("emit")
            .addKdoc("%L", "Emits [node] into this [RemoteOperationSink].")
            .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
            .addModifiers(KModifier.PUBLIC)
            .receiver(REMOTE_OPERATION_SINK_CLASS)
            .addParameter("node", KotlinGenerator.OPERATION_NODE_CLASS)
            .addStatement("node.emitTo(this)")
            .build()

    private fun buildWireBufferOperationSink(sinkOps: List<SchemaOperation>): TypeSpec {
        val kdoc =
            """
            [RemoteOperationSink] implementation that writes wire bytes directly into [buffer] without
            allocating intermediate [OperationNode] instances.

            @param buffer destination [WireBuffer]
            @param targetApiLevel target wire protocol API level (defaults to 8)
            @param supportedOpcodes optional set of allowed opcodes for profile validation
            @param onOpcode optional callback invoked before each opcode is written
            """
                .trimIndent()

        val supportedOpcodesType = SET.parameterizedBy(INT).copy(nullable = true)
        val onOpcodeType =
            LambdaTypeName.get(parameters = arrayOf(INT), returnType = UNIT).copy(nullable = true)

        val builder =
            TypeSpec.classBuilder("WireBufferOperationSink")
                .addKdoc("%L", kdoc)
                .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC)
                .addSuperinterface(REMOTE_OPERATION_SINK_CLASS)
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter(
                            ParameterSpec.builder("buffer", KotlinGenerator.WIRE_BUFFER_CLASS)
                                .defaultValue("%T()", KotlinGenerator.WIRE_BUFFER_CLASS)
                                .build()
                        )
                        .addParameter(
                            ParameterSpec.builder("targetApiLevel", INT)
                                .defaultValue("%L", "DEFAULT_DOCUMENT_API_LEVEL")
                                .build()
                        )
                        .addParameter(
                            ParameterSpec.builder("supportedOpcodes", supportedOpcodesType)
                                .defaultValue("null")
                                .build()
                        )
                        .addParameter(
                            ParameterSpec.builder("onOpcode", onOpcodeType)
                                .defaultValue("null")
                                .build()
                        )
                        .build()
                )
                .addProperty(
                    PropertySpec.builder(
                            "buffer",
                            KotlinGenerator.WIRE_BUFFER_CLASS,
                            KModifier.PUBLIC,
                        )
                        .initializer("buffer")
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("targetApiLevel", INT, KModifier.PUBLIC)
                        .initializer("targetApiLevel")
                        .build()
                )
                .addProperty(
                    PropertySpec.builder(
                            "supportedOpcodes",
                            supportedOpcodesType,
                            KModifier.PUBLIC,
                        )
                        .initializer("supportedOpcodes")
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("onOpcode", onOpcodeType, KModifier.PRIVATE)
                        .initializer("onOpcode")
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("bytesWritten", INT, KModifier.PUBLIC)
                        .addKdoc("%L", "Total bytes written to [buffer] so far.")
                        .getter(
                            FunSpec.getterBuilder().addStatement("return buffer.size()").build()
                        )
                        .build()
                )
                .addFunction(buildValidateOpcodeFunSpec())
                .addFunction(
                    FunSpec.builder("beginSkip")
                        .addKdoc(
                            "%L",
                            "Starts a `Skip` (`236`) block with a placeholder byte-length field and returns the buffer\noffset to pass to [endSkip].",
                        )
                        .addModifiers(KModifier.PUBLIC)
                        .addParameter("conditionType", INT)
                        .addParameter("value", INT)
                        .returns(INT)
                        .addStatement(
                            "validateOpcode(%T.OPCODE, %S)",
                            ClassName(KotlinGenerator.DOCUMENT_PACKAGE, "SkipNode"),
                            "Skip",
                        )
                        .addStatement(
                            "buffer.start(%T.OPCODE)",
                            ClassName(KotlinGenerator.DOCUMENT_PACKAGE, "SkipNode"),
                        )
                        .addStatement("buffer.writeInt(conditionType)")
                        .addStatement("buffer.writeInt(value)")
                        .addStatement("val skipOffset = buffer.index")
                        .addStatement("buffer.writeInt(0)")
                        .addStatement("return skipOffset")
                        .build()
                )
                .addFunction(
                    FunSpec.builder("endSkip")
                        .addKdoc(
                            "%L",
                            "Backpatches the skipped byte length at [skipOffset] opened by [beginSkip].",
                        )
                        .addModifiers(KModifier.PUBLIC)
                        .addParameter("skipOffset", INT)
                        .addStatement(
                            "buffer.overwriteInt(skipOffset, buffer.size() - (skipOffset + 4))"
                        )
                        .build()
                )
                .addFunction(
                    FunSpec.builder("beginPatternDefine")
                        .addKdoc(
                            "%L",
                            "Starts a `PatternDefine` (`246`) container with a placeholder body byte-length field and\nreturns the buffer offset to pass to [endPatternDefine].",
                        )
                        .addModifiers(KModifier.PUBLIC)
                        .addParameter("id", INT)
                        .addParameter("paramIds", INT_ARRAY)
                        .returns(INT)
                        .addStatement(
                            "validateOpcode(%T.OPCODE, %S)",
                            ClassName(KotlinGenerator.DOCUMENT_PACKAGE, "PatternDefineNode"),
                            "PatternDefine",
                        )
                        .addStatement(
                            "buffer.start(%T.OPCODE)",
                            ClassName(KotlinGenerator.DOCUMENT_PACKAGE, "PatternDefineNode"),
                        )
                        .addStatement("buffer.writeInt(id)")
                        .addStatement("buffer.writeInt(paramIds.size)")
                        .beginControlFlow("for (item in paramIds)")
                        .addStatement("buffer.writeInt(item)")
                        .endControlFlow()
                        .addStatement("val skipOffset = buffer.index")
                        .addStatement("buffer.writeInt(0)")
                        .addStatement("return skipOffset")
                        .build()
                )
                .addFunction(
                    FunSpec.builder("endPatternDefine")
                        .addKdoc(
                            "%L",
                            "Backpatches the pattern body byte length at [skipOffset] opened by [beginPatternDefine] and\nemits the closing `ContainerEnd` (`214`) operation.",
                        )
                        .addModifiers(KModifier.PUBLIC)
                        .addParameter("skipOffset", INT)
                        .addStatement(
                            "buffer.overwriteInt(skipOffset, buffer.size() - (skipOffset + 4))"
                        )
                        .addStatement("containerEnd()")
                        .build()
                )

        for (op in sinkOps) {
            val cleanMethodName = cleanMethodNameForOp(op)
            val props = op.astProperties
            val fnBuilder =
                buildSinkFunBuilder(
                    cleanMethodName = cleanMethodName,
                    props = props,
                    includeDefaults = false,
                    modifiers = listOf(KModifier.OVERRIDE),
                )
            fnBuilder.addStatement(
                "validateOpcode(%T.OPCODE, %S)",
                ClassName(KotlinGenerator.DOCUMENT_PACKAGE, "${op.name}Node"),
                op.name,
            )
            val callArgs = buildList {
                add("buffer")
                add("targetApiLevel")
                for (p in props) {
                    add(KotlinGenerator.sanitizeIdentifier(p.name))
                }
            }
            fnBuilder.addStatement("write%LOperation(%L)", op.name, callArgs.joinToString(", "))
            builder.addFunction(fnBuilder.build())
        }

        return builder.build()
    }

    private fun buildNodeListOperationSink(sinkOps: List<SchemaOperation>): TypeSpec {
        val kdoc =
            """
            [RemoteOperationSink] implementation that instantiates and appends [OperationNode] AST instances
            directly to a mutable list without serializing bytes.

            @param mutableNodes backing list receiving emitted [DocumentNode] elements
            @param supportedOpcodes optional set of allowed opcodes for profile validation
            @param onOpcode optional callback invoked before each node is appended
            """
                .trimIndent()

        val mutableNodesType = MUTABLE_LIST.parameterizedBy(KotlinGenerator.DOCUMENT_NODE_CLASS)
        val supportedOpcodesType = SET.parameterizedBy(INT).copy(nullable = true)
        val onOpcodeType =
            LambdaTypeName.get(parameters = arrayOf(INT), returnType = UNIT).copy(nullable = true)

        val builder =
            TypeSpec.classBuilder("NodeListOperationSink")
                .addKdoc("%L", kdoc)
                .addAnnotation(KotlinGenerator.RESTRICT_TO_ANNOTATION)
                .addModifiers(KModifier.PUBLIC)
                .addSuperinterface(REMOTE_OPERATION_SINK_CLASS)
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter(
                            ParameterSpec.builder("mutableNodes", mutableNodesType)
                                .defaultValue("ArrayList()")
                                .build()
                        )
                        .addParameter(
                            ParameterSpec.builder("supportedOpcodes", supportedOpcodesType)
                                .defaultValue("null")
                                .build()
                        )
                        .addParameter(
                            ParameterSpec.builder("onOpcode", onOpcodeType)
                                .defaultValue("null")
                                .build()
                        )
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("mutableNodes", mutableNodesType, KModifier.PRIVATE)
                        .initializer("mutableNodes")
                        .build()
                )
                .addProperty(
                    PropertySpec.builder(
                            "supportedOpcodes",
                            supportedOpcodesType,
                            KModifier.PUBLIC,
                        )
                        .initializer("supportedOpcodes")
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("onOpcode", onOpcodeType, KModifier.PRIVATE)
                        .initializer("onOpcode")
                        .build()
                )
                .addProperty(
                    PropertySpec.builder(
                            "nodes",
                            LIST.parameterizedBy(KotlinGenerator.DOCUMENT_NODE_CLASS),
                            KModifier.PUBLIC,
                        )
                        .addKdoc("%L", "Emitted [DocumentNode] list in stream order.")
                        .getter(FunSpec.getterBuilder().addStatement("return mutableNodes").build())
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("size", INT, KModifier.PUBLIC)
                        .addKdoc("%L", "Number of [DocumentNode] elements emitted so far.")
                        .getter(
                            FunSpec.getterBuilder().addStatement("return mutableNodes.size").build()
                        )
                        .build()
                )
                .addFunction(buildValidateOpcodeFunSpec())
                .addFunction(
                    FunSpec.builder("appendNode")
                        .addKdoc(
                            "%L",
                            "Validates and appends an existing [node] directly to [nodes].",
                        )
                        .addModifiers(KModifier.PUBLIC)
                        .addParameter("node", KotlinGenerator.DOCUMENT_NODE_CLASS)
                        .beginControlFlow("if (node is %T)", KotlinGenerator.OPERATION_NODE_CLASS)
                        .addStatement("validateOpcode(node.opcode, node.operationName)")
                        .endControlFlow()
                        .addStatement("mutableNodes.add(node)")
                        .build()
                )

        for (op in sinkOps) {
            val cleanMethodName = cleanMethodNameForOp(op)
            val props = op.astProperties
            val nodeClass = ClassName(KotlinGenerator.DOCUMENT_PACKAGE, "${op.name}Node")
            val fnBuilder =
                buildSinkFunBuilder(
                    cleanMethodName = cleanMethodName,
                    props = props,
                    includeDefaults = false,
                    modifiers = listOf(KModifier.OVERRIDE),
                )
            fnBuilder.addStatement("validateOpcode(%T.OPCODE, %S)", nodeClass, op.name)

            val omitZeroFields =
                op.fields
                    .filter { it.omitIf != null && it.type == "float_or_id" }
                    .map { it.name }
                    .toSet()

            for (fNameRaw in omitZeroFields) {
                val pName = KotlinGenerator.sanitizeIdentifier(fNameRaw)
                val effName = "effective_${KotlinGenerator.cleanIdentifier(fNameRaw)}"
                fnBuilder.beginControlFlow(
                    "val %L = if (%L != null && !%L.isVariable && %L.floatValue == 0f)",
                    effName,
                    pName,
                    pName,
                    pName,
                )
                fnBuilder.addStatement("null")
                fnBuilder.nextControlFlow("else")
                fnBuilder.addStatement("%L", pName)
                fnBuilder.endControlFlow()
            }

            val ctorArgs =
                props.joinToString(", ") { p ->
                    val pName = KotlinGenerator.sanitizeIdentifier(p.name)
                    if (p.name in omitZeroFields) {
                        "$pName = effective_${KotlinGenerator.cleanIdentifier(p.name)}"
                    } else {
                        "$pName = $pName"
                    }
                }
            fnBuilder.addStatement("mutableNodes.add(%T(%L))", nodeClass, ctorArgs)
            builder.addFunction(fnBuilder.build())
        }

        return builder.build()
    }

    private fun buildValidateOpcodeFunSpec(): FunSpec =
        FunSpec.builder("validateOpcode")
            .addModifiers(KModifier.PRIVATE)
            .addParameter("opcode", INT)
            .addParameter("operationName", STRING)
            .addStatement("val allowed = supportedOpcodes")
            .beginControlFlow("if (allowed != null && opcode !in allowed)")
            .addStatement(
                "throw %T(%L)",
                IllegalArgumentException::class,
                "\"Operation \$operationName (opcode=\$opcode) is not supported by active profile\"",
            )
            .endControlFlow()
            .addStatement("onOpcode?.invoke(opcode)")
            .build()

    private fun buildSinkFunBuilder(
        cleanMethodName: String,
        props: List<AstProperty>,
        includeDefaults: Boolean,
        modifiers: List<KModifier>,
    ): FunSpec.Builder {
        val builder = FunSpec.builder(cleanMethodName).addModifiers(modifiers)
        if (props.any { it.type == "byte" || it.type == "short" }) {
            builder.addAnnotation(KotlinGenerator.NO_BYTE_OR_SHORT_ANNOTATION)
        }
        for (p in props) {
            val cleanName = KotlinGenerator.cleanIdentifier(p.name)
            val paramBuilder =
                ParameterSpec.builder(
                    cleanName,
                    KotlinGenerator.mapToKotlinTypeName(p.type, p.isNullable),
                )
            if (includeDefaults) {
                val defVal =
                    KotlinGenerator.mapToKotlinDefault(p.type, p.isNullable, p.defaultValue)
                paramBuilder.defaultValue("%L", defVal)
            }
            builder.addParameter(paramBuilder.build())
        }
        return builder
    }

    companion object {
        private val REMOTE_OPERATION_SINK_CLASS =
            ClassName(KotlinGenerator.DOCUMENT_PACKAGE, "RemoteOperationSink")

        private fun cleanMethodNameForOp(op: SchemaOperation): String =
            KotlinGenerator.cleanIdentifier(op.name.replaceFirstChar { it.lowercaseChar() })

        fun methodNameForOp(op: SchemaOperation): String =
            KotlinGenerator.sanitizeIdentifier(op.name.replaceFirstChar { it.lowercaseChar() })
    }
}
