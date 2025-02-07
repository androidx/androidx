/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.solver.query.result

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XCodeBlock.Builder.Companion.applyTo
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.ext.Function1TypeSpec
import androidx.room.ext.InvokeWithLambdaParameter
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.LambdaSpec
import androidx.room.ext.RoomMemberNames.DB_UTIL_PERFORM_SUSPENDING
import androidx.room.ext.RoomTypeNames.RAW_QUERY
import androidx.room.ext.SQLiteDriverTypeNames
import androidx.room.solver.CodeGenScope
import androidx.room.solver.binderprovider.ConvertRowsOverrideInfo

/**
 * This Binder binds queries directly to KMP Compatible Paging3 PagingSource (i.e.
 * [androidx.room.paging.LimitOffsetPagingSource]). Used solely by KMP Paging3.
 */
class Paging3PagingSourceQueryResultBinder(
    private val listAdapter: ListQueryResultAdapter?,
    private val tableNames: Set<String>,
    private val convertRowsOverrideInfo: ConvertRowsOverrideInfo,
    className: XClassName
) : QueryResultBinder(listAdapter) {

    private val itemTypeName: XTypeName =
        listAdapter?.rowAdapters?.firstOrNull()?.out?.asTypeName() ?: XTypeName.ANY_OBJECT
    private val pagingSourceTypeName: XTypeName = className.parametrizedBy(itemTypeName)

    override fun convertAndReturn(
        sqlQueryVar: String,
        dbProperty: XPropertySpec,
        bindStatement: (CodeGenScope.(String) -> Unit)?,
        returnTypeName: XTypeName,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val rawQueryVarName = scope.getTmpVar("_rawQuery")
        val stmtVarName = scope.getTmpVar("_stmt")

        when (scope.language) {
            CodeLanguage.JAVA -> {
                val assignExpr =
                    if (bindStatement != null) {
                        XCodeBlock.ofNewInstance(
                            typeName = RAW_QUERY,
                            "%L, %L",
                            sqlQueryVar,
                            Function1TypeSpec(
                                parameterTypeName = SQLiteDriverTypeNames.STATEMENT,
                                parameterName = stmtVarName,
                                returnTypeName = KotlinTypeNames.UNIT
                            ) {
                                val functionScope = scope.fork()
                                functionScope.builder
                                    .apply { bindStatement.invoke(functionScope, stmtVarName) }
                                    .build()
                                addCode(functionScope.generate())
                                addStatement("return %T.INSTANCE", KotlinTypeNames.UNIT)
                            }
                        )
                    } else {
                        XCodeBlock.ofNewInstance(typeName = RAW_QUERY, "%L", sqlQueryVar)
                    }
                scope.builder.apply {
                    addLocalVariable(
                        name = rawQueryVarName,
                        typeName = RAW_QUERY,
                        assignExpr = assignExpr
                    )
                    addStatement(
                        "return %L",
                        createPagingSourceSpec(
                            scope = scope,
                            rawQueryVarName = rawQueryVarName,
                            stmtVarName = stmtVarName,
                            dbProperty = dbProperty,
                            inTransaction = inTransaction
                        )
                    )
                }
            }
            CodeLanguage.KOTLIN -> {
                scope.builder.apply {
                    if (bindStatement != null) {
                        beginControlFlow(
                            "val %L: %T = %T(%N) { %L ->",
                            rawQueryVarName,
                            RAW_QUERY,
                            RAW_QUERY,
                            sqlQueryVar,
                            stmtVarName
                        )
                        bindStatement.invoke(scope, stmtVarName)
                        endControlFlow()
                    } else {
                        addLocalVariable(
                            name = rawQueryVarName,
                            typeName = RAW_QUERY,
                            assignExpr =
                                XCodeBlock.ofNewInstance(
                                    typeName = RAW_QUERY,
                                    argsFormat = "%N",
                                    sqlQueryVar
                                )
                        )
                    }
                    addStatement(
                        "return %L",
                        createPagingSourceSpec(
                            scope = scope,
                            rawQueryVarName = rawQueryVarName,
                            stmtVarName = stmtVarName,
                            dbProperty = dbProperty,
                            inTransaction = false
                        )
                    )
                }
            }
        }
    }

    private fun createPagingSourceSpec(
        scope: CodeGenScope,
        rawQueryVarName: String,
        stmtVarName: String,
        dbProperty: XPropertySpec,
        inTransaction: Boolean
    ): XTypeSpec {
        val tableNamesList = tableNames.joinToString(", ") { "\"$it\"" }
        return XTypeSpec.anonymousClassBuilder(
                argsFormat = "%L, %N, %L",
                rawQueryVarName,
                dbProperty,
                tableNamesList
            )
            .apply {
                superclass(pagingSourceTypeName)
                addFunction(
                    createConvertRowsMethod(
                        scope = scope,
                        dbProperty = dbProperty,
                        stmtVarName = stmtVarName,
                        inTransaction = inTransaction
                    )
                )
            }
            .build()
    }

    private fun createConvertRowsMethod(
        scope: CodeGenScope,
        dbProperty: XPropertySpec,
        stmtVarName: String,
        inTransaction: Boolean
    ): XFunSpec {
        val resultVar = scope.getTmpVar("_result")
        return XFunSpec.overridingBuilder(
                element = convertRowsOverrideInfo.function,
                owner = convertRowsOverrideInfo.owner
            )
            .apply {
                val limitRawQueryParamName = "limitOffsetQuery"
                val rowsScope = scope.fork()
                val connectionVar = scope.getTmpVar("_connection")
                val performBlock =
                    InvokeWithLambdaParameter(
                        scope = scope,
                        functionName = DB_UTIL_PERFORM_SUSPENDING,
                        argFormat = listOf("%N", "%L", "%L"),
                        args = listOf(dbProperty, /* isReadOnly= */ true, inTransaction),
                        continuationParamName = convertRowsOverrideInfo.continuationParamName,
                        lambdaSpec =
                            object :
                                LambdaSpec(
                                    parameterTypeName = SQLiteDriverTypeNames.CONNECTION,
                                    parameterName = connectionVar,
                                    returnTypeName = convertRowsOverrideInfo.returnTypeName,
                                    javaLambdaSyntaxAvailable = scope.javaLambdaSyntaxAvailable
                                ) {
                                override fun XCodeBlock.Builder.body(scope: CodeGenScope) {
                                    applyTo { language ->
                                        val getSql =
                                            when (language) {
                                                CodeLanguage.JAVA -> "getSql()"
                                                CodeLanguage.KOTLIN -> "sql"
                                            }
                                        addLocalVal(
                                            stmtVarName,
                                            SQLiteDriverTypeNames.STATEMENT,
                                            "%L.prepare(%L.$getSql)",
                                            connectionVar,
                                            limitRawQueryParamName
                                        )
                                    }
                                    addStatement(
                                        "%L.getBindingFunction().invoke(%L)",
                                        limitRawQueryParamName,
                                        stmtVarName
                                    )
                                    beginControlFlow("try").apply {
                                        listAdapter?.convert(resultVar, stmtVarName, scope)
                                        applyTo { language ->
                                            val returnPrefix =
                                                when (language) {
                                                    CodeLanguage.JAVA -> "return "
                                                    CodeLanguage.KOTLIN -> ""
                                                }
                                            addStatement("$returnPrefix%L", resultVar)
                                        }
                                    }
                                    nextControlFlow("finally")
                                    addStatement("%L.close()", stmtVarName)
                                    endControlFlow()
                                }
                            }
                    )
                rowsScope.builder.add("return %L", performBlock)
                addCode(rowsScope.generate())
            }
            .build()
    }
}
