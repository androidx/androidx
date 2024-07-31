/*
 * Copyright 2018 The Android Open Source Project
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
import androidx.room.ext.AndroidTypeNames
import androidx.room.ext.CallableTypeSpecBuilder
import androidx.room.ext.InvokeWithLambdaParameter
import androidx.room.ext.LambdaSpec
import androidx.room.ext.RoomGuavaMemberNames.GUAVA_ROOM_CREATE_LISTENABLE_FUTURE
import androidx.room.ext.SQLiteDriverTypeNames
import androidx.room.solver.CodeGenScope

/**
 * A ResultBinder that emits a ListenableFuture<T> where T is the input {@code typeArg}.
 *
 * <p>The Future runs on the background thread Executor.
 */
class GuavaListenableFutureQueryResultBinder(val typeArg: XType, adapter: QueryResultAdapter?) :
    BaseObservableQueryResultBinder(adapter) {

    override fun convertAndReturn(
        roomSQLiteQueryVar: String,
        canReleaseQuery: Boolean,
        dbProperty: XPropertySpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val cancellationSignalVar = scope.getTmpVar("_cancellationSignal")
        scope.builder.apply {
            addLocalVariable(
                name = cancellationSignalVar,
                typeName = AndroidTypeNames.CANCELLATION_SIGNAL,
                assignExpr =
                    XCodeBlock.ofNewInstance(
                        language,
                        AndroidTypeNames.CANCELLATION_SIGNAL,
                    )
            )
        }

        // Callable<T> // Note that this callable does not release the query object.
        val callableImpl =
            CallableTypeSpecBuilder(scope.language, typeArg.asTypeName()) {
                    addCode(
                        XCodeBlock.builder(language)
                            .apply {
                                createRunQueryAndReturnStatements(
                                    builder = this,
                                    roomSQLiteQueryVar = roomSQLiteQueryVar,
                                    dbProperty = dbProperty,
                                    inTransaction = inTransaction,
                                    scope = scope,
                                    cancellationSignalVar = cancellationSignalVar
                                )
                            }
                            .build()
                    )
                }
                .build()

        scope.builder.apply {
            addStatement(
                "return %M(%N, %L, %L, %L, %L, %L)",
                GUAVA_ROOM_CREATE_LISTENABLE_FUTURE,
                dbProperty,
                if (inTransaction) "true" else "false",
                callableImpl,
                roomSQLiteQueryVar,
                canReleaseQuery,
                cancellationSignalVar
            )
        }
    }

    override fun convertAndReturn(
        sqlQueryVar: String,
        dbProperty: XPropertySpec,
        bindStatement: CodeGenScope.(String) -> Unit,
        returnTypeName: XTypeName,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val connectionVar = scope.getTmpVar("_connection")
        val performBlock =
            InvokeWithLambdaParameter(
                scope = scope,
                functionName = GUAVA_ROOM_CREATE_LISTENABLE_FUTURE,
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
        scope.builder.add("return %L", performBlock)
    }

    override fun isMigratedToDriver() = adapter?.isMigratedToDriver() == true
}
