/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.room3.solver.query.result

import androidx.room3.compiler.codegen.XCodeBlock
import androidx.room3.compiler.codegen.XPropertySpec
import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.compiler.codegen.buildCodeBlock
import androidx.room3.compiler.processing.XType
import androidx.room3.ext.ArrayLiteral
import androidx.room3.ext.CommonTypeNames
import androidx.room3.ext.InvokeWithLambdaParameter
import androidx.room3.ext.KotlinCollectionMemberNames
import androidx.room3.ext.KotlinTypeNames
import androidx.room3.ext.LambdaSpec
import androidx.room3.ext.ListOfString
import androidx.room3.ext.RoomMemberNames.DB_UTIL_PERFORM_SUSPENDING
import androidx.room3.ext.RoomTypeNames.RAW_QUERY
import androidx.room3.ext.SQLiteDriverTypeNames
import androidx.room3.solver.CodeGenScope
import androidx.room3.solver.types.DaoReturnTypeConverter
import androidx.room3.solver.types.DaoReturnTypeConverter.OptionalParam
import androidx.room3.vo.QueryParameter

class DaoConverterQueryResultBinder(
    val typeArg: XType,
    val tableNames: Set<String>,
    adapter: QueryResultAdapter?,
    val converter: DaoReturnTypeConverter,
    val parameters: List<QueryParameter> = emptyList(),
) : QueryResultBinder(adapter) {

    override fun convertAndReturn(
        sqlQueryVar: String,
        dbProperty: XPropertySpec,
        bindStatement: (CodeGenScope.(String) -> Unit)?,
        returnTypeName: XTypeName,
        inTransaction: Boolean,
        scope: CodeGenScope,
    ) {
        val rawQueryVar = scope.getTmpVar("_rawQuery")
        val statementVar = scope.getTmpVar("_stmt")
        val argsVar = scope.getTmpVar("_args")
        if (converter.hasRawQueryParam()) {
            if (bindStatement != null) {
                val rawQueryAssignExprBlock =
                    InvokeWithLambdaParameter(
                        scope = scope,
                        functionCall = XCodeBlock.of("%T", RAW_QUERY),
                        argFormat = listOf("%N"),
                        args = listOf(sqlQueryVar),
                        lambdaSpec =
                            object :
                                LambdaSpec(
                                    parameterTypeName = SQLiteDriverTypeNames.STATEMENT,
                                    parameterName = statementVar,
                                    returnTypeName = XTypeName.UNIT_VOID,
                                    javaLambdaSyntaxAvailable = scope.javaLambdaSyntaxAvailable,
                                ) {
                                override fun XCodeBlock.Builder.body(scope: CodeGenScope) {
                                    bindStatement(scope, statementVar)
                                }
                            },
                    )
                scope.builder.addLocalVariable(
                    name = rawQueryVar,
                    typeName = RAW_QUERY,
                    isAssignExprStmt = false,
                    assignExpr = rawQueryAssignExprBlock,
                )
            } else {
                scope.builder.addLocalVal(rawQueryVar, RAW_QUERY, "%T(%L)", RAW_QUERY, sqlQueryVar)
            }
        }
        if (converter.requiredParameters.contains(OptionalParam.ARGS)) {
            val mapExpr = buildCodeBlock {
                if (parameters.isEmpty()) {
                    add("%M()", KotlinCollectionMemberNames.EMPTY_MAP)
                } else {
                    val placeHolders = parameters.joinToString(", ") { "%S to %L" }
                    val mapArgs = parameters.flatMap { listOf(it.name, it.name) }
                    add(
                        "%M($placeHolders)",
                        KotlinCollectionMemberNames.MAP_OF,
                        *mapArgs.toTypedArray(),
                    )
                }
            }
            scope.builder.addLocalVariable(
                name = argsVar,
                typeName =
                    CommonTypeNames.MAP.parametrizedBy(
                        CommonTypeNames.STRING,
                        KotlinTypeNames.ANY.copy(nullable = true),
                    ),
                assignExpr = mapExpr,
            )
        }

        val lambdaHasRawQuery = converter.executeAndReturnLambda.hasRawQueryParam
        val converterRawQuery = scope.getTmpVar("_converterQuery")
        val args =
            converter.requiredParameters.map {
                when (it) {
                    OptionalParam.ROOM_DB -> dbProperty.name
                    OptionalParam.TABLE_NAMES_ARRAY ->
                        ArrayLiteral(CommonTypeNames.STRING, *tableNames.toTypedArray())
                    OptionalParam.TABLE_NAMES_LIST -> ListOfString(*tableNames.toTypedArray())
                    OptionalParam.IN_TRANSACTION -> inTransaction
                    OptionalParam.RAW_QUERY -> rawQueryVar
                    OptionalParam.ARGS -> argsVar
                }
            }

        val convertBlock =
            InvokeWithLambdaParameter(
                scope = scope,
                functionCall = converter.buildStatement(typeArg.asTypeName(), scope),
                argFormat = List(args.size) { "%L" },
                args = args,
                lambdaSpec =
                    object :
                        LambdaSpec(
                            parameterTypeName = if (lambdaHasRawQuery) RAW_QUERY else null,
                            parameterName = if (lambdaHasRawQuery) converterRawQuery else null,
                            returnTypeName = converter.to.asTypeName(),
                            javaLambdaSyntaxAvailable = scope.javaLambdaSyntaxAvailable,
                        ) {
                        override fun XCodeBlock.Builder.body(scope: CodeGenScope) {
                            val converterQueryVar =
                                if (lambdaHasRawQuery) converterRawQuery else null
                            scope.builder.add(
                                "%L",
                                setUpPerformBlock(
                                    scope,
                                    dbProperty,
                                    typeArg,
                                    converterQueryVar,
                                    inTransaction,
                                    bindStatement,
                                    sqlQueryVar,
                                    statementVar,
                                ),
                            )
                        }
                    },
            )
        scope.builder.add("return %L", convertBlock)
    }

    private fun setUpPerformBlock(
        scope: CodeGenScope,
        dbProperty: XPropertySpec,
        typeArg: XType,
        converterQueryVar: String?,
        inTransaction: Boolean,
        bindStatement: (CodeGenScope.(String) -> Unit)?,
        sqlQueryVar: String,
        statementVar: String,
    ): XCodeBlock {
        val connectionVar = scope.getTmpVar("_connection")
        return InvokeWithLambdaParameter(
            scope = scope,
            functionName = DB_UTIL_PERFORM_SUSPENDING,
            functionTypeArg = typeArg.asTypeName(),
            argFormat = listOf("%N", "%L", "%L"),
            args = listOf(dbProperty, true, inTransaction),
            lambdaSpec =
                object :
                    LambdaSpec(
                        parameterTypeName = SQLiteDriverTypeNames.CONNECTION,
                        parameterName = connectionVar,
                        returnTypeName = typeArg.asTypeName(),
                        javaLambdaSyntaxAvailable = scope.javaLambdaSyntaxAvailable,
                    ) {
                    override fun XCodeBlock.Builder.body(scope: CodeGenScope) {
                        // Use the dynamic SQL if available (e.g. for
                        // Paging), else original SQL
                        val sqlSource =
                            if (converterQueryVar != null) "$converterQueryVar.sql" else sqlQueryVar

                        addLocalVal(
                            statementVar,
                            SQLiteDriverTypeNames.STATEMENT,
                            "%L.prepare(%L)",
                            connectionVar,
                            sqlSource,
                        )
                        beginControlFlow("try")
                        if (converterQueryVar != null) {
                            addStatement(
                                "%L.getBindingFunction().invoke(%L)",
                                converterQueryVar,
                                statementVar,
                            )
                        } else {
                            bindStatement?.invoke(scope, statementVar)
                        }

                        val outVar = scope.getTmpVar("_result")
                        adapter?.convert(outVar, statementVar, scope)
                        addStatement("%L", outVar)
                        nextControlFlow("finally")
                        addStatement("%L.close()", statementVar)
                        endControlFlow()
                    }
                },
        )
    }
}
