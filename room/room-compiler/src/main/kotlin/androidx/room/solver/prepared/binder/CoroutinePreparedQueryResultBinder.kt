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

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XCodeBlock.Builder.Companion.addLocalVal
import androidx.room.compiler.codegen.XMemberName.Companion.packageMember
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.box
import androidx.room.ext.Function1TypeSpec
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.SQLiteDriverTypeNames
import androidx.room.solver.CodeGenScope
import androidx.room.solver.prepared.result.PreparedQueryResultAdapter

/**
 * Binder of prepared queries of a Kotlin coroutine suspend function.
 */
class CoroutinePreparedQueryResultBinder(
    adapter: PreparedQueryResultAdapter?,
    private val continuationParamName: String,
) : PreparedQueryResultBinder(adapter) {

    override fun executeAndReturn(
        prepareQueryStmtBlock: CodeGenScope.() -> String,
        preparedStmtProperty: XPropertySpec?,
        dbProperty: XPropertySpec,
        scope: CodeGenScope
    ) {
        error("Wrong executeAndReturn invoked")
    }

    override fun isMigratedToDriver(): Boolean = true

    override fun executeAndReturn(
        sqlQueryVar: String,
        dbProperty: XPropertySpec,
        bindStatement: CodeGenScope.(String) -> Unit,
        returnTypeName: XTypeName,
        scope: CodeGenScope
    ) {
        when (scope.language) {
            CodeLanguage.JAVA -> executeAndReturnJava(
                sqlQueryVar, dbProperty, bindStatement, returnTypeName, scope
            )
            CodeLanguage.KOTLIN -> executeAndReturnKotlin(
                sqlQueryVar, dbProperty, bindStatement, scope
            )
        }
    }

    private fun executeAndReturnJava(
        sqlQueryVar: String,
        dbProperty: XPropertySpec,
        bindStatement: CodeGenScope.(String) -> Unit,
        returnTypeName: XTypeName,
        scope: CodeGenScope
    ) {
        val connectionVar = scope.getTmpVar("_connection")
        val statementVar = scope.getTmpVar("_stmt")
        scope.builder.addStatement(
            "return %M(%N, %L, %L, %L, %L)",
            RoomTypeNames.DB_UTIL.packageMember("performSuspending"),
            dbProperty,
            false, // isReadOnly
            true, // inTransaction
            // TODO(b/322387497): Generate lambda syntax if possible
            Function1TypeSpec(
                language = scope.language,
                parameterTypeName = SQLiteDriverTypeNames.CONNECTION,
                parameterName = connectionVar,
                returnTypeName = returnTypeName.box()
            ) {
                val functionScope = scope.fork()
                val functionCode = functionScope.builder.apply {
                    addLocalVal(
                        statementVar,
                        SQLiteDriverTypeNames.STATEMENT,
                        "%L.prepare(%L)",
                        connectionVar,
                        sqlQueryVar
                    )
                    beginControlFlow("try")
                    bindStatement(functionScope, statementVar)
                    adapter?.executeAndReturn(connectionVar, statementVar, functionScope)
                    nextControlFlow("finally")
                    addStatement("%L.close()", statementVar)
                    endControlFlow()
                }.build()
                this.addCode(functionCode)
            },
            continuationParamName
        )
    }

    private fun executeAndReturnKotlin(
        sqlQueryVar: String,
        dbProperty: XPropertySpec,
        bindStatement: CodeGenScope.(String) -> Unit,
        scope: CodeGenScope
    ) {
        val connectionVar = scope.getTmpVar("_connection")
        val statementVar = scope.getTmpVar("_stmt")
        scope.builder.apply {
            beginControlFlow(
                "return %M(%N, %L, %L) { %L ->",
                RoomTypeNames.DB_UTIL.packageMember("performSuspending"),
                dbProperty,
                false, // isReadOnly
                true, // inTransaction
                connectionVar
            )
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
            endControlFlow()
        }
    }
}
