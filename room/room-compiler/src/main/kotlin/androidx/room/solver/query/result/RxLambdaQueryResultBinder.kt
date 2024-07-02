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

package androidx.room.solver.query.result

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XCodeBlock.Builder.Companion.addLocalVal
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XFunSpec.Builder.Companion.addStatement
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import androidx.room.ext.AndroidTypeNames.CURSOR
import androidx.room.ext.CallableTypeSpecBuilder
import androidx.room.ext.InvokeWithLambdaParameter
import androidx.room.ext.LambdaSpec
import androidx.room.ext.RoomMemberNames.DB_UTIL_QUERY
import androidx.room.ext.SQLiteDriverTypeNames
import androidx.room.solver.CodeGenScope
import androidx.room.solver.RxType

/** Generic result binder for Rx classes that are not reactive. */
internal class RxLambdaQueryResultBinder(
    private val rxType: RxType,
    val typeArg: XType,
    adapter: QueryResultAdapter?
) : QueryResultBinder(adapter) {
    override fun convertAndReturn(
        roomSQLiteQueryVar: String,
        canReleaseQuery: Boolean,
        dbProperty: XPropertySpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val callable =
            CallableTypeSpecBuilder(scope.language, typeArg.asTypeName()) {
                    addCode(
                        XCodeBlock.builder(language)
                            .apply {
                                fillInCallMethod(
                                    roomSQLiteQueryVar = roomSQLiteQueryVar,
                                    dbProperty = dbProperty,
                                    inTransaction = inTransaction,
                                    scope = scope,
                                    returnType = typeArg
                                )
                            }
                            .build()
                    )
                }
                .apply {
                    if (canReleaseQuery) {
                        createFinalizeMethod(roomSQLiteQueryVar)
                    }
                }
                .build()
        scope.builder.apply {
            if (rxType.isSingle()) {
                addStatement("return %M(%L)", rxType.factoryMethodName, callable)
            } else {
                addStatement("return %T.fromCallable(%L)", rxType.className, callable)
            }
        }
    }

    private fun XCodeBlock.Builder.fillInCallMethod(
        roomSQLiteQueryVar: String,
        dbProperty: XPropertySpec,
        inTransaction: Boolean,
        returnType: XType,
        scope: CodeGenScope
    ) {
        val adapterScope = scope.fork()
        val transactionWrapper =
            if (inTransaction) {
                transactionWrapper(dbProperty.name)
            } else {
                null
            }
        transactionWrapper?.beginTransactionWithControlFlow()
        val shouldCopyCursor = adapter?.shouldCopyCursor() == true
        val outVar = scope.getTmpVar("_result")
        val cursorVar = scope.getTmpVar("_cursor")
        addLocalVariable(
            name = cursorVar,
            typeName = CURSOR,
            assignExpr =
                XCodeBlock.of(
                    language = language,
                    format = "%M(%N, %L, %L, %L)",
                    DB_UTIL_QUERY,
                    dbProperty,
                    roomSQLiteQueryVar,
                    if (shouldCopyCursor) "true" else "false",
                    "null"
                )
        )
        beginControlFlow("try").apply {
            adapter?.convert(outVar, cursorVar, adapterScope)
            add(adapterScope.generate())
            if (
                !rxType.canBeNull &&
                    (language == CodeLanguage.JAVA ||
                        (language == CodeLanguage.KOTLIN &&
                            returnType.nullability == XNullability.NULLABLE))
            ) {
                beginControlFlow("if (%L == null)", outVar).apply {
                    addStatement(
                        "throw %L",
                        XCodeBlock.ofNewInstance(
                            language,
                            rxType.version.emptyResultExceptionClassName,
                            "%L",
                            XCodeBlock.of(
                                language,
                                when (language) {
                                    CodeLanguage.KOTLIN -> "%S + %L.sql"
                                    CodeLanguage.JAVA -> "%S + %L.getSql()"
                                },
                                "Query returned empty result set: ",
                                roomSQLiteQueryVar
                            )
                        )
                    )
                }
                endControlFlow()
            }
            transactionWrapper?.commitTransaction()
            addStatement("return %L", outVar)
        }
        nextControlFlow("finally").apply { addStatement("%L.close()", cursorVar) }
        endControlFlow()
        transactionWrapper?.endTransactionWithControlFlow()
    }

    private fun XTypeSpec.Builder.createFinalizeMethod(roomSQLiteQueryVar: String) {
        addFunction(
            XFunSpec.builder(
                    language = language,
                    name = "finalize",
                    visibility = VisibilityModifier.PROTECTED,
                    // To 'override' finalize in Kotlin one does not use the 'override' keyword, but
                    // in
                    // Java the @Override is needed
                    isOverride = language == CodeLanguage.JAVA
                )
                .apply { addStatement("%L.release()", roomSQLiteQueryVar) }
                .build()
        )
    }

    override fun isMigratedToDriver() = adapter?.isMigratedToDriver() == true

    override fun convertAndReturn(
        sqlQueryVar: String,
        dbProperty: XPropertySpec,
        bindStatement: (CodeGenScope.(String) -> Unit)?,
        returnTypeName: XTypeName,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val connectionVar = scope.getTmpVar("_connection")
        val performBlock =
            InvokeWithLambdaParameter(
                scope = scope,
                functionName = rxType.factoryMethodName,
                argFormat = listOf("%N", "%L", "%L"),
                args = listOf(dbProperty, /* isReadOnly= */ true, inTransaction),
                lambdaSpec =
                    object :
                        LambdaSpec(
                            parameterTypeName = SQLiteDriverTypeNames.CONNECTION,
                            parameterName = connectionVar,
                            returnTypeName = typeArg.asTypeName(),
                            javaLambdaSyntaxAvailable = scope.javaLambdaSyntaxAvailable
                        ) {
                        override fun XCodeBlock.Builder.body(scope: CodeGenScope) {
                            val returnPrefix =
                                when (language) {
                                    CodeLanguage.JAVA -> "return "
                                    CodeLanguage.KOTLIN -> ""
                                }
                            val statementVar = scope.getTmpVar("_stmt")
                            addLocalVal(
                                statementVar,
                                SQLiteDriverTypeNames.STATEMENT,
                                "%L.prepare(%L)",
                                connectionVar,
                                sqlQueryVar
                            )
                            beginControlFlow("try")
                            bindStatement?.invoke(scope, statementVar)
                            val outVar = scope.getTmpVar("_result")
                            adapter?.convert(outVar, statementVar, scope)
                            addStatement("$returnPrefix%L", outVar)
                            nextControlFlow("finally")
                            addStatement("%L.close()", statementVar)
                            endControlFlow()
                        }
                    }
            )
        scope.builder.add("return %L", performBlock)
    }
}
