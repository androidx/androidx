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
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XCodeBlock.Builder.Companion.addLocalVal
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XType
import androidx.room.ext.ArrayLiteral
import androidx.room.ext.CallableTypeSpecBuilder
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.InvokeWithLambdaParameter
import androidx.room.ext.LambdaSpec
import androidx.room.ext.SQLiteDriverTypeNames
import androidx.room.solver.CodeGenScope

/** Converts the query into a LiveData and returns it. No query is run until necessary. */
class LiveDataQueryResultBinder(
    val typeArg: XType,
    val tableNames: Set<String>,
    adapter: QueryResultAdapter?
) : BaseObservableQueryResultBinder(adapter) {
    override fun convertAndReturn(
        roomSQLiteQueryVar: String,
        canReleaseQuery: Boolean,
        dbProperty: XPropertySpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val callableImpl =
            CallableTypeSpecBuilder(scope.language, typeArg.asTypeName()) {
                    addCode(
                        XCodeBlock.builder(language)
                            .apply {
                                createRunQueryAndReturnStatements(
                                    builder = this,
                                    roomSQLiteQueryVar = roomSQLiteQueryVar,
                                    inTransaction = inTransaction,
                                    dbProperty = dbProperty,
                                    scope = scope,
                                    cancellationSignalVar = "null" // LiveData can't be cancelled
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
            val arrayOfTableNamesLiteral =
                ArrayLiteral(scope.language, CommonTypeNames.STRING, *tableNames.toTypedArray())
            // Use property syntax in Kotlin and getter in Java
            val getInvalidationTracker =
                when (language) {
                    CodeLanguage.JAVA -> "getInvalidationTracker()"
                    CodeLanguage.KOTLIN -> "invalidationTracker"
                }

            addStatement(
                "return %N.%L.createLiveData(%L, %L, %L)",
                dbProperty,
                getInvalidationTracker,
                arrayOfTableNamesLiteral,
                if (inTransaction) "true" else "false",
                callableImpl
            )
        }
    }

    override fun isMigratedToDriver() = adapter?.isMigratedToDriver() == true

    override fun convertAndReturn(
        sqlQueryVar: String,
        dbProperty: XPropertySpec,
        bindStatement: CodeGenScope.(String) -> Unit,
        returnTypeName: XTypeName,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val arrayOfTableNamesLiteral =
            ArrayLiteral(scope.language, CommonTypeNames.STRING, *tableNames.toTypedArray())
        val connectionVar = scope.getTmpVar("_connection")
        val createBlock =
            InvokeWithLambdaParameter(
                scope = scope,
                functionCall =
                    XCodeBlock.of(
                        scope.language,
                        "%N.%L.createLiveData",
                        dbProperty,
                        when (scope.language) {
                            CodeLanguage.JAVA -> "getInvalidationTracker()"
                            CodeLanguage.KOTLIN -> "invalidationTracker"
                        }
                    ),
                argFormat = listOf("%L", "%L"),
                args = listOf(arrayOfTableNamesLiteral, inTransaction),
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
                            bindStatement(scope, statementVar)
                            val outVar = scope.getTmpVar("_result")
                            adapter?.convert(outVar, statementVar, scope)
                            addStatement("$returnPrefix%L", outVar)
                            nextControlFlow("finally")
                            addStatement("%L.close()", statementVar)
                            endControlFlow()
                        }
                    }
            )
        scope.builder.add("return %L", createBlock)
    }
}
