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
import java.lang.reflect.Modifier

/** In-memory representation of the RemoteCompose schema parsed from operations.json. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class OperationSchema(
    public val schemaVersion: String,
    public val targetApiLevel: Int,
    public val types: Map<String, SchemaTypeDefinition>,
    public val categories: List<SchemaCategory>,
    public val operations: List<SchemaOperation>,
    public val structs: Map<String, SchemaStruct> = emptyMap(),
    public val paintCommands: Map<Int, SchemaPaintCommand> = emptyMap(),
    public val rpnOperators: SchemaRpnOperatorTable? = null,
    public val intRpnOperators: SchemaRpnOperatorTable? = null,
    public val pathCommands: Map<Int, SchemaPathCommand> = emptyMap(),
    public val systemVariables: List<SchemaSystemVariable> = emptyList(),
    public val headerConstants: Map<String, Map<String, Int>> = emptyMap(),
    public val visibilityConstants: Map<String, Int> = emptyMap(),
    public val idEncoding: SchemaIdEncoding? = null,
    public val animationSpec: SchemaAnimationSpec? = null,
    public val pathEffects: SchemaPathEffects? = null,
    public val reservedOpcodes: List<SchemaReservedOpcode> = emptyList(),
) {
    public val operationsById: Map<Int, SchemaOperation> = operations.associateBy { it.id }
    public val operationsByName: Map<String, SchemaOperation> = operations.associateBy { it.name }
    public val systemVariablesById: Map<Int, SchemaSystemVariable> = systemVariables.associateBy {
        it.id
    }
    public val systemVariablesByName: Map<String, SchemaSystemVariable> =
        systemVariables.associateBy {
            it.name
        }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaSystemVariable(
    public val id: Int,
    public val name: String,
    public val type: String,
    public val description: String = "",
    public val isTime: Boolean = false,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaPaintCommand(
    public val id: Int,
    public val name: String,
    public val description: String = "",
    public val fields: List<SchemaField> = emptyList(),
    public val wireFormat: String? = null,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaPathCommand(
    public val id: Int,
    public val name: String,
    public val description: String = "",
    public val fields: List<SchemaField> = emptyList(),
    public val leadingPaddingFloats: Int = 0,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaReservedOpcode(
    public val id: Int,
    public val name: String,
    public val opCodeName: String,
    public val profiles: List<String> = emptyList(),
    public val description: String = "",
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaIdEncoding(
    public val nanPrefix: Int,
    public val payloadMask: Int,
    public val startId: Int,
    public val regionMask: Int,
    public val regionSubIdMask: Int,
    public val regionSystem: Int,
    public val regionVariable: Int,
    public val regionArray: Int,
    public val regionOperation: Int,
    public val longIdOffset: Long,
    public val ptrDereference: Int,
    public val valueMask: Int,
    public val macroLocalStartId: Int,
    public val macroLocalEndId: Int,
    public val nanExponent: Int = 0x7F800000,
    public val noId: Int = -1,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaAnimationEasing(
    public val id: Int,
    public val name: String,
    public val aliases: List<String> = emptyList(),
    public val cubicControlPoints: FloatArray? = null,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaAnimationSpec(
    public val easings: List<SchemaAnimationEasing>,
    public val flagsBits: List<SchemaBitField>,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaPathEffectType(
    public val id: Int,
    public val name: String,
    public val constName: String,
    public val fields: List<SchemaField>,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaPathEffects(
    public val maxDashIntervals: Int,
    public val maxNestingDepth: Int,
    public val types: List<SchemaPathEffectType>,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaRpnOperatorTable(
    public val offset: Int,
    public val operators: List<SchemaRpnOperator>,
) {
    public val operatorsById: Map<Int, SchemaRpnOperator> = operators.associateBy { it.id }
    public val operatorsByName: Map<String, SchemaRpnOperator> = operators.associateBy { it.name }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaRpnOperator(
    public val id: Int,
    public val name: String,
    public val symbol: String = "",
    public val arity: Int = 1,
    public val addedVersion: Int = 6,
    public val description: String = "",
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaStruct(
    public val name: String,
    public val description: String = "",
    public val fields: List<SchemaField> = emptyList(),
) {
    public val astProperties: List<AstProperty>
        get() = buildAstProperties(fields, null)

    public val computedBitProperties: List<Pair<String, SchemaBitField>>
        get() = buildComputedBitProperties(fields)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaTypeDefinition(
    public val name: String,
    public val wire: String,
    public val base: String?,
    public val description: String,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaCategory(
    public val name: String,
    public val description: String,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaOperation(
    public val id: Int,
    public val name: String,
    public val category: String,
    public val description: String,
    public val addedVersion: Int,
    public val isContainer: Boolean,
    public val isContainerEnd: Boolean,
    public val closedBy: String?,
    public val fields: List<SchemaField>,
    public val extensions: SchemaExtension?,
    public val profiles: List<String> = listOf("baseline"),
    public val opCodeName: String? = null,
    public val restrictToLibraryGroup: Boolean = true,
    public val encoding: String? = null,
    public val sparseCountType: String? = null,
    public val sparseKeyType: String? = null,
    public val sparseParameters: List<SchemaSparseParam>? = null,
    public val allowedChildren: List<String>? = null,
    public val conditionalSkip: SchemaConditionalSkip? = null,
    public val javaClassName: String? = null,
) {
    public val resolvedOpCodeName: String
        get() = opCodeName ?: ID_TO_OPCODE_NAME[id] ?: toScreamingSnakeCase(name)

    public val resolvedJavaClassName: String
        get() = javaClassName ?: name

    public val astProperties: List<AstProperty>
        get() = buildAstProperties(fields, sparseParameters)

    public val computedBitProperties: List<Pair<String, SchemaBitField>>
        get() = buildComputedBitProperties(fields)

    public companion object {
        public val ID_TO_OPCODE_NAME: Map<Int, String> by lazy {
            try {
                val clazz = Class.forName("androidx.compose.remote.core.Operations")
                clazz.fields
                    .filter {
                        it.type == Int::class.javaPrimitiveType && Modifier.isStatic(it.modifiers)
                    }
                    .associate { (it.getInt(null) as Int) to it.name }
            } catch (e: Throwable) {
                emptyMap()
            }
        }

        public fun toScreamingSnakeCase(name: String): String {
            val sb = StringBuilder()
            for (i in name.indices) {
                val c = name[i]
                if (c.isUpperCase() && i > 0 && !name[i - 1].isUpperCase()) {
                    sb.append('_')
                }
                sb.append(c.uppercaseChar())
            }
            return sb.toString()
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaField(
    public val name: String,
    public val type: String,
    public val description: String,
    public val enumValues: Map<String, Int>?,
    public val addedVersion: Int? = null,
    public val defaultValue: String? = null,
    public val bits: List<SchemaBitField>? = null,
    public val unpackBits: Boolean = true,
    public val lengthType: String? = null,
    public val countFrom: String? = null,
    public val derivedFrom: String? = null,
    public val presentIf: String? = null,
    public val omitIf: String? = null,
    public val role: String? = null,
    public val idKind: String? = null,
    public val encoding: String? = null,
    public val offset: String? = null,
    public val lookupBit: Int? = null,
    public val strictEnum: Boolean = false,
    public val minValue: Int? = null,
    public val minLength: Int? = null,
    public val maxLength: Int? = null,
    public val javaLimit: String? = null,
    public val payloadKind: String? = null,
    public val mustBeZeroBits: String? = null,
    public val conditionalTypes: List<SchemaConditionalType> = emptyList(),
    public val conditionalRoles: List<SchemaConditionalRole> = emptyList(),
    public val resolvedAfter: String? = null,
) {
    public val effectiveLengthType: String
        get() = lengthType ?: "short"
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaSkipConditionBranch(
    public val whenValue: Int,
    public val name: String,
    public val target: String,
    public val op: String,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaConditionalSkip(
    public val conditionField: String,
    public val conditionBits: String,
    public val conditionSigned: Boolean,
    public val valueField: String,
    public val lengthField: String,
    public val payloadField: String,
    public val unknownConditionSkips: Boolean,
    public val conditions: List<SchemaSkipConditionBranch>,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaConditionalBitRange(public val bit: Int, public val range: String) {
    public val lowBit: Int
        get() = range.substringBefore("..").toInt()

    public val highBit: Int
        get() = range.substringAfter("..", range).toInt()

    public val bitWidth: Int
        get() = highBit - lowBit + 1

    public val bitMask: Long
        get() = if (bitWidth >= 32) 0xFFFFFFFFL else (1L shl bitWidth) - 1L
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaConditionalType(public val condition: String, public val type: String)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaConditionalRole(
    public val condition: String,
    public val role: String,
    public val idKind: String? = null,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaBitField(
    public val name: String,
    public val range: String,
    public val role: String? = null,
    public val idKind: String? = null,
    public val derivedFrom: String? = null,
    public val signed: Boolean = false,
    public val description: String = "",
    public val enumValues: Map<String, Int>? = null,
    public val strictEnum: Boolean = false,
    public val whenBitSet: SchemaConditionalBitRange? = null,
) {
    public val lowBit: Int
        get() = range.substringBefore("..").toInt()

    public val highBit: Int
        get() = range.substringAfter("..", range).toInt()

    public val bitWidth: Int
        get() = highBit - lowBit + 1

    public val bitMask: Long
        get() = if (bitWidth >= 32) 0xFFFFFFFFL else (1L shl bitWidth) - 1L
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaSparseParam(
    public val id: Int,
    public val name: String,
    public val type: String,
    public val defaultValue: String? = null,
    public val description: String = "",
    public val lengthType: String? = null,
    public val role: String? = null,
    public val idKind: String? = null,
    public val ignored: Boolean = false,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaExtension(
    public val type: String,
    public val tagType: String,
    public val supportedTags: List<SchemaTag>,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaTag(
    public val name: String,
    public val tag: Int,
    public val type: String,
    public val description: String,
)

/** Projected AST property exposed by generated operation/struct classes across languages. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AstProperty(
    public val name: String,
    public val type: String,
    public val description: String,
    public val isNullable: Boolean = false,
    public val defaultValue: String? = null,
    public val isSparse: Boolean = false,
    public val sparseId: Int? = null,
)

internal fun buildAstProperties(
    fields: List<SchemaField>,
    sparseParameters: List<SchemaSparseParam>?,
): List<AstProperty> {
    val result = mutableListOf<AstProperty>()
    for (f in fields) {
        if (f.derivedFrom != null) continue
        val bits = f.bits
        if (bits != null && f.unpackBits) {
            for (b in bits) {
                if (b.derivedFrom != null) continue
                result.add(
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
            val isNullable = f.presentIf != null && !f.type.endsWith("[]") && f.defaultValue == null
            result.add(
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
    if (sparseParameters != null) {
        for (sp in sparseParameters) {
            result.add(
                AstProperty(
                    name = sp.name,
                    type = sp.type,
                    description = sp.description,
                    isNullable = true,
                    defaultValue = sp.defaultValue,
                    isSparse = true,
                    sparseId = sp.id,
                )
            )
        }
    }
    return result
}

internal fun buildComputedBitProperties(
    fields: List<SchemaField>
): List<Pair<String, SchemaBitField>> {
    val result = mutableListOf<Pair<String, SchemaBitField>>()
    for (f in fields) {
        val bits = f.bits
        if (bits != null && !f.unpackBits) {
            for (b in bits) {
                if (b.derivedFrom == null && b.name != f.name) {
                    result.add(f.name to b)
                }
            }
        }
    }
    return result
}

/** Parsed declarative `derivedFrom` expression (`len(x)`, `present(x)`, `nonEmpty(x)`). */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed class DerivedExpr(public val targetName: String) {
    public class LengthOf(targetName: String) : DerivedExpr(targetName)

    public class PresentOf(targetName: String) : DerivedExpr(targetName)

    public class NonEmptyOf(targetName: String) : DerivedExpr(targetName)

    public companion object {
        public fun parse(expr: String): DerivedExpr =
            when {
                expr.startsWith("len(") && expr.endsWith(")") ->
                    LengthOf(expr.substring(4, expr.length - 1))
                expr.startsWith("present(") && expr.endsWith(")") ->
                    PresentOf(expr.substring(8, expr.length - 1))
                expr.startsWith("nonEmpty(") && expr.endsWith(")") ->
                    NonEmptyOf(expr.substring(9, expr.length - 1))
                else -> throw IllegalArgumentException("Unsupported derivedFrom expression: $expr")
            }
    }
}
