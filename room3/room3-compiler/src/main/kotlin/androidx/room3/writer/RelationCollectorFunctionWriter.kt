/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room3.writer

import androidx.room3.compiler.codegen.CodeLanguage
import androidx.room3.compiler.codegen.XCodeBlock
import androidx.room3.compiler.codegen.XFunSpec
import androidx.room3.compiler.codegen.XMemberName.Companion.packageMember
import androidx.room3.compiler.codegen.XName
import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.ext.CollectionTypeNames
import androidx.room3.ext.CollectionsSizeExprCode
import androidx.room3.ext.CommonTypeNames
import androidx.room3.ext.InvokeWithLambdaParameter
import androidx.room3.ext.KotlinTypeNames
import androidx.room3.ext.LambdaSpec
import androidx.room3.ext.MapKeySetExprCode
import androidx.room3.ext.RoomTypeNames
import androidx.room3.ext.RoomTypeNames.RELATION_UTIL
import androidx.room3.ext.SQLiteDriverTypeNames
import androidx.room3.ext.stripNonJava
import androidx.room3.solver.CodeGenScope
import androidx.room3.vo.RelationCollector

/** Writes the function that fetches the relations of a POJO and assigns them into the given map. */
class RelationCollectorFunctionWriter(private val collector: RelationCollector) :
    TypeWriter.SharedFunctionSpec(
        baseName =
            "fetchRelationship${collector.relation.entity.tableName.stripNonJava()}" +
                "As${collector.relation.dataClassTypeName.toString(CodeLanguage.JAVA).stripNonJava()}" +
                (collector.relation.junction?.let { "With${it.entity.tableName.stripNonJava()}" }
                    ?: "")
    ) {
    companion object {
        const val PARAM_MAP_VARIABLE = "_map"
        const val PARAM_CONNECTION_VARIABLE = "_connection"
        const val KEY_SET_VARIABLE = "__mapKeySet"
    }

    private val usingLongSparseArray =
        collector.mapTypeName.rawTypeName == CollectionTypeNames.LONG_SPARSE_ARRAY
    private val usingArrayMap = collector.mapTypeName.rawTypeName == CollectionTypeNames.ARRAY_MAP

    override fun getUniqueKey(): String {
        val relation = collector.relation
        return "RelationCollectorFunctionWriter" +
            "-${collector.mapTypeName}" +
            "-${relation.entity.typeName.toString(CodeLanguage.JAVA)}" +
            "-${relation.dataClassTypeName}" +
            "-${collector.relationQueryWriter.createLoadAllSql()}"
    }

    override fun isSuspendFun() = true

    override fun prepare(functionName: String, writer: TypeWriter, builder: XFunSpec.Builder) {
        val scope = CodeGenScope(writer = writer)
        scope.builder.apply {
            // Check the input map key set for emptiness, returning early as no fetching is needed.
            addIsInputEmptyCheck()

            // Check if the input map key set exceeds MAX_BIND_PARAMETER_CNT, if so do a recursive
            // fetch.
            beginControlFlow(
                    "if (%L > %L)",
                    if (usingLongSparseArray) {
                        XCodeBlock.of("%L.size()", PARAM_MAP_VARIABLE)
                    } else {
                        CollectionsSizeExprCode(PARAM_MAP_VARIABLE)
                    },
                    "999",
                )
                .apply {
                    addRecursiveFetchCall(scope, functionName)
                    addStatement("return")
                }
                .endControlFlow()

            createStmtAndReturn(scope)
        }
        builder.apply {
            addParameter(PARAM_CONNECTION_VARIABLE, SQLiteDriverTypeNames.CONNECTION)
            addParameter(PARAM_MAP_VARIABLE, collector.mapTypeName)
            addCode(scope.generate())
        }
    }

    private fun XCodeBlock.Builder.createStmtAndReturn(scope: CodeGenScope) {
        // Create SQL query, acquire statement and bind parameters.
        val stmtVar = scope.getTmpVar("_stmt")
        val sqlQueryVar = scope.getTmpVar("_sql")
        val connectionVar = scope.getTmpVar(PARAM_CONNECTION_VARIABLE)
        collector.relationQueryWriter.prepareStatementAndBindArgs(
            connectionVarName = connectionVar,
            outSqlQueryName = sqlQueryVar,
            outStmtName = stmtVar,
            scope = scope,
        )
        addRelationCollectorCode(scope, stmtVar)
    }

    private fun XCodeBlock.Builder.addRelationCollectorCode(scope: CodeGenScope, stmtVar: String) {
        val relation = collector.relation
        beginControlFlow("try").apply {
            // Gets indices of the columns to be used as key
            val itemKeyIndexVars =
                if (relation.junction != null) {
                    // When using a junction table the relationship map is keyed on the parent
                    // reference columns of the junction table, the same columns used in the WHERE
                    // IN clause, these columns are the rightmost columns in the generated SELECT
                    // clause.
                    List(relation.junction.parentProperties.size) { index ->
                        val idx = relation.projection.size + index
                        val varName = scope.getTmpVar("_itemKeyIndex_$index")
                        addLocalVal(varName, XTypeName.PRIMITIVE_INT, "%L", idx)
                        varName
                    }
                } else {
                    relation.entityProperties.mapIndexed { index, prop ->
                        val varName = scope.getTmpVar("_itemKeyIndex_$index")
                        addLocalVal(
                            name = varName,
                            typeName = XTypeName.PRIMITIVE_INT,
                            assignExprFormat = "%M(%L, %S)",
                            RoomTypeNames.STATEMENT_UTIL.packageMember("getColumnIndex"),
                            stmtVar,
                            prop.columnName,
                        )
                        varName
                    }
                }

            // Check if index of column is not -1, indicating that one of the columns for the key is
            // not in the result, can happen if the user specified a bad projection in @Relation.
            beginControlFlow(
                "if (${itemKeyIndexVars.joinToString(" || ") { "%L == -1" }})",
                *itemKeyIndexVars.toTypedArray(),
            )
            addStatement("return")
            endControlFlow()

            // Prepare item column indices
            collector.rowAdapter.onStatementReady(stmtVarName = stmtVar, scope = scope)
            val tmpVarName = scope.getTmpVar("_item")
            beginControlFlow("while (%L.step())", stmtVar).apply {
                // Read key from the statement, convert row to item and place it on map
                collector.readKey(
                    stmtVarName = stmtVar,
                    indexVars = itemKeyIndexVars,
                    keyReaders =
                        if (relation.junction != null) {
                            collector.parentKeyColumnReaders
                        } else {
                            collector.entityKeyColumnReaders
                        },
                    scope = scope,
                ) { keyVar ->
                    if (collector.relationTypeIsCollection) {
                        val relationVar = scope.getTmpVar("_tmpRelation")
                        addLocalVal(
                            relationVar,
                            collector.relationTypeName.copy(nullable = true),
                            "%L.get(%L)",
                            PARAM_MAP_VARIABLE,
                            keyVar,
                        )
                        beginControlFlow("if (%L != null)", relationVar)
                        addLocalVariable(tmpVarName, relation.dataClassTypeName)
                        collector.rowAdapter.convert(tmpVarName, stmtVar, scope)
                        addStatement("%L.add(%L)", relationVar, tmpVarName)
                        endControlFlow()
                    } else {
                        beginControlFlow("if (%N.containsKey(%L))", PARAM_MAP_VARIABLE, keyVar)
                        addLocalVariable(tmpVarName, relation.dataClassTypeName)
                        collector.rowAdapter.convert(tmpVarName, stmtVar, scope)
                        addStatement("%N.put(%L, %L)", PARAM_MAP_VARIABLE, keyVar, tmpVarName)
                        endControlFlow()
                    }
                }
            }
            endControlFlow()
        }
        nextControlFlow("finally").apply { addStatement("%L.close()", stmtVar) }
        endControlFlow()
    }

    private fun XCodeBlock.Builder.addIsInputEmptyCheck() {
        if (usingLongSparseArray) {
                beginControlFlow("if (%L.isEmpty())", PARAM_MAP_VARIABLE)
            } else {
                val keySetType = CommonTypeNames.SET.parametrizedBy(collector.keyTypeName)
                addLocalVariable(
                    name = KEY_SET_VARIABLE,
                    typeName = keySetType,
                    assignExpr = MapKeySetExprCode(PARAM_MAP_VARIABLE),
                )
                beginControlFlow("if (%L.isEmpty())", KEY_SET_VARIABLE)
            }
            .apply { addStatement("return") }
        endControlFlow()
    }

    private fun XCodeBlock.Builder.addRecursiveFetchCall(
        scope: CodeGenScope,
        functionName: String,
    ) {
        val utilFunction =
            RELATION_UTIL.let {
                when {
                    usingLongSparseArray -> it.packageMember("recursiveFetchLongSparseArray")
                    usingArrayMap -> it.packageMember("recursiveFetchArrayMap")
                    else ->
                        it.packageMember(
                            XName.of(java = "recursiveFetchHashMap", kotlin = "recursiveFetchMap")
                        )
                }
            }
        val paramName = scope.getTmpVar("_tmpMap")
        val recursiveFetchBlock =
            InvokeWithLambdaParameter(
                scope = scope,
                functionName = utilFunction,
                argFormat = listOf("%L", "%L"),
                args = listOf(PARAM_MAP_VARIABLE, collector.relationTypeIsCollection),
                lambdaSpec =
                    object :
                        LambdaSpec(
                            parameterTypeName = collector.mapTypeName,
                            parameterName = paramName,
                            returnTypeName = KotlinTypeNames.UNIT,
                            javaLambdaSyntaxAvailable = scope.javaLambdaSyntaxAvailable,
                        ) {
                        override fun XCodeBlock.Builder.body(scope: CodeGenScope) {
                            val recursiveCall =
                                XCodeBlock.of(
                                    "%L(%L, %L)",
                                    functionName,
                                    PARAM_CONNECTION_VARIABLE,
                                    paramName,
                                )
                            addStatement("%L", recursiveCall)
                        }
                    },
            )
        add("%L", recursiveFetchBlock)
    }
}
