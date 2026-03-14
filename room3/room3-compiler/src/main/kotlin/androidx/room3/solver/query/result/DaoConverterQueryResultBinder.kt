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
import androidx.room3.compiler.processing.XType
import androidx.room3.ext.ArrayLiteral
import androidx.room3.ext.CommonTypeNames
import androidx.room3.ext.InvokeWithLambdaParameter
import androidx.room3.ext.LambdaSpec
import androidx.room3.ext.ListOfString
import androidx.room3.ext.RoomMemberNames.DB_UTIL_PERFORM_SUSPENDING
import androidx.room3.ext.RoomTypeNames.RAW_QUERY
import androidx.room3.ext.SQLiteDriverMemberNames
import androidx.room3.ext.SQLiteDriverTypeNames
import androidx.room3.solver.CodeGenScope
import androidx.room3.solver.types.DaoReturnTypeConverter
import androidx.room3.solver.types.DaoReturnTypeConverter.OptionalParam

class DaoConverterQueryResultBinder(
    val typeArg: XType,
    val tableNames: Set<String>,
    adapter: QueryResultAdapter?,
    val converter: DaoReturnTypeConverter,
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
        val executeAndReturnLambda = converter.executeAndReturnLambda
        if (executeAndReturnLambda.hasRawQueryParam) {
            scope.builder.apply {
                if (bindStatement != null) {
                    // TODO(b/487009207): Remove hard coded value
                    beginControlFlow(
                        "val %L: %T = %T(%N) { %L ->",
                        rawQueryVar,
                        RAW_QUERY,
                        RAW_QUERY,
                        sqlQueryVar,
                        statementVar,
                    )
                    bindStatement(scope, statementVar)
                    endControlFlow()
                } else {
                    addLocalVal(rawQueryVar, RAW_QUERY, "%T(%L)", RAW_QUERY, sqlQueryVar)
                }
            }
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
                            "%L.%M(%L)",
                            connectionVar,
                            SQLiteDriverMemberNames.CONNECTION_PREPARE,
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
