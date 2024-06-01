/*
 * Copyright (C) 2016 The Android Open Source Project
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
import androidx.room.compiler.codegen.XMemberName.Companion.packageMember
import androidx.room.compiler.codegen.XTypeName
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.RoomMemberNames
import androidx.room.ext.RoomTypeNames
import androidx.room.parser.ParsedQuery
import androidx.room.parser.Section
import androidx.room.solver.CodeGenScope
import androidx.room.vo.QueryMethod
import androidx.room.vo.QueryParameter

/** Writes the SQL query and arguments for a QueryMethod. */
class QueryWriter(
    val parameters: List<QueryParameter>,
    val sectionToParamMapping: List<Pair<Section, QueryParameter?>>,
    val query: ParsedQuery
) {

    constructor(
        queryMethod: QueryMethod
    ) : this(queryMethod.parameters, queryMethod.sectionToParamMapping, queryMethod.query)

    fun prepareReadAndBind(
        outSqlQueryName: String,
        outRoomSQLiteQueryVar: String,
        scope: CodeGenScope
    ) {
        val listSizeVars = createSqlQueryAndArgs(outSqlQueryName, outRoomSQLiteQueryVar, scope)
        bindArgs(outRoomSQLiteQueryVar, listSizeVars, scope)
    }

    fun prepareQuery(
        outSqlQueryName: String,
        scope: CodeGenScope
    ): List<Pair<QueryParameter, String>> {
        return createSqlQueryAndArgs(outSqlQueryName, null, scope)
    }

    private fun createSqlQueryAndArgs(
        outSqlQueryName: String,
        outArgsName: String?,
        scope: CodeGenScope
    ): List<Pair<QueryParameter, String>> {
        val listSizeVars = arrayListOf<Pair<QueryParameter, String>>()
        val varargParams = parameters.filter { it.queryParamAdapter?.isMultiple ?: false }
        val sectionToParamMapping = sectionToParamMapping
        val knownQueryArgsCount =
            sectionToParamMapping
                .filterNot { it.second?.queryParamAdapter?.isMultiple ?: false }
                .size
        scope.builder.apply {
            if (varargParams.isNotEmpty()) {
                val stringBuilderVar = scope.getTmpVar("_stringBuilder")
                val stringBuilderTypeName =
                    when (language) {
                        CodeLanguage.JAVA -> CommonTypeNames.STRING_BUILDER
                        CodeLanguage.KOTLIN -> KotlinTypeNames.STRING_BUILDER
                    }
                addLocalVariable(
                    name = stringBuilderVar,
                    typeName = stringBuilderTypeName,
                    assignExpr = XCodeBlock.ofNewInstance(language, stringBuilderTypeName)
                )
                query.sections.forEach { section ->
                    when (section) {
                        is Section.Text ->
                            addStatement("%L.append(%S)", stringBuilderVar, section.text)
                        is Section.NewLine -> addStatement("%L.append(%S)", stringBuilderVar, "\n")
                        is Section.BindVar -> {
                            // If it is null, will be reported as error before. We just try out
                            // best to generate as much code as possible.
                            sectionToParamMapping
                                .firstOrNull { section == it.first }
                                ?.let { (_, param) ->
                                    if (param?.queryParamAdapter?.isMultiple == true) {
                                        val tmpCount = scope.getTmpVar("_inputSize")
                                        listSizeVars.add(param to tmpCount)
                                        param.queryParamAdapter.getArgCount(
                                            param.name,
                                            tmpCount,
                                            scope
                                        )
                                        addStatement(
                                            "%M(%L, %L)",
                                            RoomTypeNames.STRING_UTIL.packageMember(
                                                "appendPlaceholders"
                                            ),
                                            stringBuilderVar,
                                            tmpCount
                                        )
                                    } else {
                                        addStatement("%L.append(%S)", stringBuilderVar, "?")
                                    }
                                }
                        }
                    }
                }
                addLocalVal(
                    outSqlQueryName,
                    CommonTypeNames.STRING,
                    "%L.toString()",
                    stringBuilderVar
                )
                if (outArgsName != null) {
                    val argCount = scope.getTmpVar("_argCount")
                    addLocalVal(
                        argCount,
                        XTypeName.PRIMITIVE_INT,
                        "%L%L",
                        knownQueryArgsCount,
                        listSizeVars.joinToString("") { " + ${it.second}" }
                    )
                    addLocalVariable(
                        name = outArgsName,
                        typeName = RoomTypeNames.ROOM_SQL_QUERY,
                        assignExpr =
                            XCodeBlock.of(
                                language,
                                "%M(%L, %L)",
                                RoomMemberNames.ROOM_SQL_QUERY_ACQUIRE,
                                outSqlQueryName,
                                argCount
                            )
                    )
                }
            } else {
                addLocalVal(
                    outSqlQueryName,
                    CommonTypeNames.STRING,
                    "%S",
                    query.queryWithReplacedBindParams
                )
                if (outArgsName != null) {
                    addLocalVariable(
                        name = outArgsName,
                        typeName = RoomTypeNames.ROOM_SQL_QUERY,
                        assignExpr =
                            XCodeBlock.of(
                                language,
                                "%M(%L, %L)",
                                RoomMemberNames.ROOM_SQL_QUERY_ACQUIRE,
                                outSqlQueryName,
                                knownQueryArgsCount
                            )
                    )
                }
            }
        }
        return listSizeVars
    }

    fun bindArgs(
        outArgsName: String,
        listSizeVars: List<Pair<QueryParameter, String>>,
        scope: CodeGenScope
    ) {
        if (parameters.isEmpty()) {
            return
        }
        scope.builder.apply {
            val argIndex = scope.getTmpVar("_argIndex")
            addLocalVariable(
                name = argIndex,
                typeName = XTypeName.PRIMITIVE_INT,
                isMutable = true,
                assignExpr = XCodeBlock.of(language, "%L", 1)
            )
            // # of bindings with 1 placeholder
            var constInputs = 0
            // variable names for size of the bindings that have multiple  args
            val varInputs = arrayListOf<String>()
            sectionToParamMapping.forEach { (_, param) ->
                // reset the argIndex to the correct start index
                if (constInputs > 0 || varInputs.isNotEmpty()) {
                    addStatement(
                        "%L = %L%L",
                        argIndex,
                        if (constInputs > 0) {
                            1 + constInputs
                        } else {
                            "1"
                        },
                        varInputs.joinToString("") { " + $it" }
                    )
                }
                param?.let {
                    it.queryParamAdapter?.bindToStmt(it.name, outArgsName, argIndex, scope)
                }
                // add these to the list so that we can use them to calculate the next count.
                val sizeVar = listSizeVars.firstOrNull { it.first == param }
                if (sizeVar == null) {
                    constInputs++
                } else {
                    varInputs.add(sizeVar.second)
                }
            }
        }
    }
}
