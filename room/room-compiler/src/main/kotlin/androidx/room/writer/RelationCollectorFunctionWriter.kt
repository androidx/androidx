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

package androidx.room.writer

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XCodeBlock.Builder.Companion.addLocalVal
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XFunSpec.Builder.Companion.addStatement
import androidx.room.compiler.codegen.XMemberName.Companion.packageMember
import androidx.room.compiler.codegen.XTypeName
import androidx.room.ext.AndroidTypeNames
import androidx.room.ext.CollectionTypeNames
import androidx.room.ext.CollectionsSizeExprCode
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.Function1TypeSpec
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.MapKeySetExprCode
import androidx.room.ext.RoomMemberNames
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.stripNonJava
import androidx.room.solver.CodeGenScope
import androidx.room.solver.query.result.PojoRowAdapter
import androidx.room.vo.RelationCollector

/**
 * Writes the function that fetches the relations of a POJO and assigns them into the given map.
 */
class RelationCollectorFunctionWriter(
    private val collector: RelationCollector
) : TypeWriter.SharedFunctionSpec(
    "fetchRelationship${collector.relation.entity.tableName.stripNonJava()}" +
        "As${collector.relation.pojoTypeName.toString(CodeLanguage.JAVA).stripNonJava()}"
) {
    companion object {
        const val PARAM_MAP_VARIABLE = "_map"
        const val KEY_SET_VARIABLE = "__mapKeySet"
    }

    private val usingLongSparseArray =
        collector.mapTypeName.rawTypeName == CollectionTypeNames.LONG_SPARSE_ARRAY
    private val usingArrayMap =
        collector.mapTypeName.rawTypeName == CollectionTypeNames.ARRAY_MAP

    override fun getUniqueKey(): String {
        val relation = collector.relation
        return "RelationCollectorMethodWriter" +
            "-${collector.mapTypeName}" +
            "-${relation.entity.typeName.toString(CodeLanguage.JAVA)}" +
            "-${relation.entityField.columnName}" +
            "-${relation.pojoTypeName}" +
            "-${relation.createLoadAllSql()}"
    }

    override fun prepare(methodName: String, writer: TypeWriter, builder: XFunSpec.Builder) {
        val scope = CodeGenScope(writer)
        scope.builder.apply {
            // Check the input map key set for emptiness, returning early as no fetching is needed.
            addIsInputEmptyCheck()

            // Check if the input map key set exceeds MAX_BIND_PARAMETER_CNT, if so do a recursive
            // fetch.
            beginControlFlow(
                "if (%L > %T.MAX_BIND_PARAMETER_CNT)",
                if (usingLongSparseArray) {
                    XCodeBlock.of(language, "%L.size()", PARAM_MAP_VARIABLE)
                } else {
                    CollectionsSizeExprCode(language, PARAM_MAP_VARIABLE)
                },
                RoomTypeNames.ROOM_DB
            ).apply {
                addRecursiveFetchCall(methodName)
                addStatement("return")
            }.endControlFlow()

            // Create SQL query, acquire statement and bind parameters.
            val stmtVar = scope.getTmpVar("_stmt")
            val sqlQueryVar = scope.getTmpVar("_sql")
            collector.queryWriter.prepareReadAndBind(sqlQueryVar, stmtVar, scope)

            // Perform query and get a Cursor
            val cursorVar = "_cursor"
            val shouldCopyCursor = collector.rowAdapter.let {
                it is PojoRowAdapter && it.relationCollectors.isNotEmpty()
            }
            addLocalVariable(
                name = cursorVar,
                typeName = AndroidTypeNames.CURSOR,
                assignExpr = XCodeBlock.of(
                    language,
                    "%M(%N, %L, %L, %L)",
                    RoomMemberNames.DB_UTIL_QUERY,
                    DaoWriter.DB_PROPERTY_NAME,
                    stmtVar,
                    if (shouldCopyCursor) "true" else "false",
                    "null"
                )
            )

            val relation = collector.relation
            beginControlFlow("try").apply {
                // Gets index of the column to be used as key
                val itemKeyIndexVar = "_itemKeyIndex"
                if (relation.junction != null) {
                    // When using a junction table the relationship map is keyed on the parent
                    // reference column of the junction table, the same column used in the WHERE IN
                    // clause, this column is the rightmost column in the generated SELECT
                    // clause.
                    val junctionParentColumnIndex = relation.projection.size
                    addStatement("// _junction.%L", relation.junction.parentField.columnName)
                    addLocalVal(
                        itemKeyIndexVar,
                        XTypeName.PRIMITIVE_INT,
                        "%L",
                        junctionParentColumnIndex
                    )
                } else {
                    addLocalVal(
                        itemKeyIndexVar,
                        XTypeName.PRIMITIVE_INT,
                        "%M(%L, %S)",
                        RoomMemberNames.CURSOR_UTIL_GET_COLUMN_INDEX,
                        cursorVar,
                        relation.entityField.columnName
                    )
                }
                // Check if index of column is not -1, indicating the column for the key is not in
                // the result, can happen if the user specified a bad projection in @Relation.
                beginControlFlow("if (%L == -1)", itemKeyIndexVar).apply {
                    addStatement("return")
                }
                endControlFlow()

                // Prepare item column indices
                collector.rowAdapter.onCursorReady(cursorVarName = cursorVar, scope = scope)

                val tmpVarName = scope.getTmpVar("_item")
                beginControlFlow("while (%L.moveToNext())", cursorVar).apply {
                    // Read key from the cursor, convert row to item and place it on map
                    collector.readKey(
                        cursorVarName = cursorVar,
                        indexVar = itemKeyIndexVar,
                        keyReader = collector.entityKeyColumnReader,
                        scope = scope
                    ) { keyVar ->
                        if (collector.relationTypeIsCollection) {
                            val relationVar = scope.getTmpVar("_tmpRelation")
                            addLocalVal(
                                relationVar,
                                collector.relationTypeName.copy(nullable = true),
                                "%L.get(%L)",
                                PARAM_MAP_VARIABLE, keyVar
                            )
                            beginControlFlow("if (%L != null)", relationVar)
                            addLocalVariable(tmpVarName, relation.pojoTypeName)
                            collector.rowAdapter.convert(tmpVarName, cursorVar, scope)
                            addStatement("%L.add(%L)", relationVar, tmpVarName)
                            endControlFlow()
                        } else {
                            beginControlFlow("if (%N.containsKey(%L))", PARAM_MAP_VARIABLE, keyVar)
                            addLocalVariable(tmpVarName, relation.pojoTypeName)
                            collector.rowAdapter.convert(tmpVarName, cursorVar, scope)
                            addStatement("%N.put(%L, %L)", PARAM_MAP_VARIABLE, keyVar, tmpVarName)
                            endControlFlow()
                        }
                    }
                }
                endControlFlow()
            }
            nextControlFlow("finally").apply {
                addStatement("%L.close()", cursorVar)
            }
            endControlFlow()
        }
        builder.apply {
            addParameter(collector.mapTypeName, PARAM_MAP_VARIABLE)
            addCode(scope.generate())
        }
    }

    private fun XCodeBlock.Builder.addIsInputEmptyCheck() {
        if (usingLongSparseArray) {
            beginControlFlow("if (%L.isEmpty())", PARAM_MAP_VARIABLE)
        } else {
            val keySetType = CommonTypeNames.SET.parametrizedBy(collector.keyTypeName)
            addLocalVariable(
                name = KEY_SET_VARIABLE,
                typeName = keySetType,
                assignExpr = MapKeySetExprCode(language, PARAM_MAP_VARIABLE)
            )
            beginControlFlow("if (%L.isEmpty())", KEY_SET_VARIABLE)
        }.apply {
            addStatement("return")
        }
        endControlFlow()
    }

    private fun XCodeBlock.Builder.addRecursiveFetchCall(methodName: String) {
        fun getRecursiveCall(itVarName: String) =
            XCodeBlock.of(
                language,
                "%L(%L)",
                methodName, itVarName
            )
        val utilFunction =
            RoomTypeNames.RELATION_UTIL.let {
                when {
                    usingLongSparseArray ->
                        it.packageMember("recursiveFetchLongSparseArray")
                    usingArrayMap ->
                        it.packageMember("recursiveFetchArrayMap")
                    else ->
                        it.packageMember("recursiveFetchHashMap")
                }
            }
        when (language) {
            CodeLanguage.JAVA -> {
                val paramName = "map"
                if (collector.javaLambdaSyntaxAvailable) {
                    add("%M(%L, %L, (%L) -> {\n",
                        utilFunction, PARAM_MAP_VARIABLE, collector.relationTypeIsCollection,
                        paramName
                    )
                    indent()
                    addStatement("%L", getRecursiveCall(paramName))
                    addStatement("return %T.INSTANCE", KotlinTypeNames.UNIT)
                    unindent()
                    addStatement("})")
                } else {
                    val functionImpl = Function1TypeSpec(
                        language = language,
                        parameterTypeName = collector.mapTypeName,
                        parameterName = paramName,
                        returnTypeName = KotlinTypeNames.UNIT,
                    ) {
                        addStatement("%L", getRecursiveCall(paramName))
                        addStatement("return %T.INSTANCE", KotlinTypeNames.UNIT)
                    }
                    addStatement(
                        "%M(%L, %L, %L)",
                        utilFunction, PARAM_MAP_VARIABLE, collector.relationTypeIsCollection,
                        functionImpl
                    )
                }
            }
            CodeLanguage.KOTLIN -> {
                beginControlFlow(
                    "%M(%L, %L)",
                    utilFunction, PARAM_MAP_VARIABLE, collector.relationTypeIsCollection
                )
                addStatement("%L", getRecursiveCall("it"))
                endControlFlow()
            }
        }
    }
}
