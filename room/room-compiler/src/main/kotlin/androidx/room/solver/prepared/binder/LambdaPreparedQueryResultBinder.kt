/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.room.solver.prepared.binder

import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XCodeBlock.Builder.Companion.addLocalVal
import androidx.room.compiler.codegen.XMemberName
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XType
import androidx.room.ext.InvokeWithLambdaParameter
import androidx.room.ext.LambdaSpec
import androidx.room.ext.SQLiteDriverTypeNames
import androidx.room.solver.CodeGenScope
import androidx.room.solver.prepared.result.PreparedQueryResultAdapter

/**
 * Binder for deferred queries.
 *
 * This binder generates code that invokes [functionName] with a lambda whose body will delegate to
 * the given [adapter].
 */
class LambdaPreparedQueryResultBinder(
    private val returnType: XType,
    private val functionName: XMemberName,
    adapter: PreparedQueryResultAdapter?
) : PreparedQueryResultBinder(adapter) {

    override fun executeAndReturn(
        prepareQueryStmtBlock: CodeGenScope.() -> String,
        preparedStmtProperty: XPropertySpec?,
        dbProperty: XPropertySpec,
        scope: CodeGenScope
    ) {
        error("Wrong executeAndReturn invoked")
    }

    override fun executeAndReturn(
        sqlQueryVar: String,
        dbProperty: XPropertySpec,
        bindStatement: CodeGenScope.(String) -> Unit,
        returnTypeName: XTypeName,
        scope: CodeGenScope
    ) {
        val connectionVar = scope.getTmpVar("_connection")
        val performBlock =
            InvokeWithLambdaParameter(
                scope = scope,
                functionName = functionName,
                argFormat = listOf("%N", "%L", "%L"),
                args = listOf(dbProperty, /* isReadOnly= */ false, /* inTransaction= */ true),
                lambdaSpec =
                    object :
                        LambdaSpec(
                            parameterTypeName = SQLiteDriverTypeNames.CONNECTION,
                            parameterName = connectionVar,
                            returnTypeName = returnType.asTypeName(),
                            javaLambdaSyntaxAvailable = scope.javaLambdaSyntaxAvailable
                        ) {
                        override fun XCodeBlock.Builder.body(scope: CodeGenScope) {
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
                            adapter?.executeAndReturn(connectionVar, statementVar, scope)
                            nextControlFlow("finally")
                            addStatement("%L.close()", statementVar)
                            endControlFlow()
                        }
                    }
            )
        scope.builder.add("return %L", performBlock)
    }

    override fun isMigratedToDriver() = true
}
