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
import androidx.room.compiler.codegen.XFunSpec.Builder.Companion.addStatement
import androidx.room.compiler.codegen.XMemberName.Companion.packageMember
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.box
import androidx.room.ext.AndroidTypeNames
import androidx.room.ext.Function1TypeSpec
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.SQLiteDriverTypeNames
import androidx.room.solver.CodeGenScope

/**
 * Instantly runs and returns the query.
 */
class InstantQueryResultBinder(adapter: QueryResultAdapter?) : QueryResultBinder(adapter) {
    override fun convertAndReturn(
        roomSQLiteQueryVar: String,
        canReleaseQuery: Boolean,
        dbProperty: XPropertySpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        scope.builder.apply {
            addStatement("%N.assertNotSuspendingTransaction()", dbProperty)
        }
        val transactionWrapper = if (inTransaction) {
            scope.builder.transactionWrapper(dbProperty.name)
        } else {
            null
        }
        transactionWrapper?.beginTransactionWithControlFlow()
        scope.builder.apply {
            val shouldCopyCursor = adapter?.shouldCopyCursor() == true
            val outVar = scope.getTmpVar("_result")
            val cursorVar = scope.getTmpVar("_cursor")
            addLocalVariable(
                name = cursorVar,
                typeName = AndroidTypeNames.CURSOR,
                assignExpr = XCodeBlock.of(
                    language,
                    "%M(%N, %L, %L, %L)",
                    RoomTypeNames.DB_UTIL.packageMember("query"),
                    dbProperty,
                    roomSQLiteQueryVar,
                    if (shouldCopyCursor) "true" else "false",
                    "null"
                )
            )
            beginControlFlow("try").apply {
                adapter?.convert(outVar, cursorVar, scope)
                transactionWrapper?.commitTransaction()
                addStatement("return %L", outVar)
            }
            nextControlFlow("finally").apply {
                addStatement("%L.close()", cursorVar)
                if (canReleaseQuery) {
                    addStatement("%L.release()", roomSQLiteQueryVar)
                }
            }
            endControlFlow()
        }
        transactionWrapper?.endTransactionWithControlFlow()
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
        when (scope.language) {
            CodeLanguage.JAVA -> convertAndReturnJava(
                sqlQueryVar, dbProperty, bindStatement, returnTypeName, inTransaction, scope
            )
            CodeLanguage.KOTLIN -> convertAndReturnKotlin(
                sqlQueryVar, dbProperty, bindStatement, inTransaction, scope
            )
        }
    }

    private fun convertAndReturnJava(
        sqlQueryVar: String,
        dbProperty: XPropertySpec,
        bindStatement: CodeGenScope.(String) -> Unit,
        returnTypeName: XTypeName,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val performFunctionName = getPerformFunctionName(inTransaction)
        val statementVar = scope.getTmpVar("_stmt")
        scope.builder.addStatement(
            "return %M(%N, %L, %L)",
            RoomTypeNames.DB_UTIL.packageMember(performFunctionName),
            dbProperty,
            sqlQueryVar,
            // TODO(b/322387497): Generate lambda syntax if possible
            Function1TypeSpec(
                language = scope.language,
                parameterTypeName = SQLiteDriverTypeNames.STATEMENT,
                parameterName = statementVar,
                returnTypeName = returnTypeName.box()
            ) {
                val functionScope = scope.fork()
                bindStatement(functionScope, statementVar)
                val outVar = functionScope.getTmpVar("_result")
                adapter?.convert(outVar, statementVar, functionScope)
                this.addCode(functionScope.generate())
                this.addStatement("return %L", outVar)
            }
        )
    }

    private fun convertAndReturnKotlin(
        sqlQueryVar: String,
        dbProperty: XPropertySpec,
        bindStatement: CodeGenScope.(String) -> Unit,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val statementVar = scope.getTmpVar("_stmt")
        val performFunctionName = getPerformFunctionName(inTransaction)
        scope.builder.apply {
            beginControlFlow(
                "return %M(%N, %L) { %L ->",
                RoomTypeNames.DB_UTIL.packageMember(performFunctionName),
                dbProperty,
                sqlQueryVar,
                statementVar
            )
            bindStatement(scope, statementVar)
            val outVar = scope.getTmpVar("_result")
            adapter?.convert(outVar, statementVar, scope)
            addStatement("%L", outVar)
            endControlFlow()
        }
    }

    private fun getPerformFunctionName(inTransaction: Boolean) =
        if (inTransaction) {
            "performReadTransactionBlocking"
        } else {
            "performReadBlocking"
        }
}
