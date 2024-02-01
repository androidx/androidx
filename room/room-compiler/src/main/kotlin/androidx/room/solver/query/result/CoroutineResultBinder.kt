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
import androidx.room.compiler.codegen.XFunSpec.Builder.Companion.addStatement
import androidx.room.compiler.codegen.XMemberName.Companion.companionMember
import androidx.room.compiler.codegen.XMemberName.Companion.packageMember
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.box
import androidx.room.compiler.processing.XType
import androidx.room.ext.AndroidTypeNames
import androidx.room.ext.CallableTypeSpecBuilder
import androidx.room.ext.Function1TypeSpec
import androidx.room.ext.RoomCoroutinesTypeNames
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.SQLiteDriverTypeNames
import androidx.room.solver.CodeGenScope

/**
 * Binds the result of a of a Kotlin coroutine suspend function.
 */
class CoroutineResultBinder(
    val typeArg: XType,
    private val continuationParamName: String,
    adapter: QueryResultAdapter?
) : QueryResultBinder(adapter) {

    override fun convertAndReturn(
        roomSQLiteQueryVar: String,
        canReleaseQuery: Boolean,
        dbProperty: XPropertySpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val cancellationSignalVar = scope.getTmpVar("_cancellationSignal")
        scope.builder.addLocalVariable(
            name = cancellationSignalVar,
            typeName = AndroidTypeNames.CANCELLATION_SIGNAL,
            assignExpr = XCodeBlock.ofNewInstance(
                scope.language,
                AndroidTypeNames.CANCELLATION_SIGNAL,
            ),
        )

        val callableImpl = CallableTypeSpecBuilder(scope.language, typeArg.asTypeName()) {
           addCode(
               XCodeBlock.builder(language).apply {
                   createRunQueryAndReturnStatements(
                       roomSQLiteQueryVar = roomSQLiteQueryVar,
                       canReleaseQuery = canReleaseQuery,
                       dbProperty = dbProperty,
                       inTransaction = inTransaction,
                       scope = scope,
                       cancellationSignalVar = "null"
                   )
               }.build()
           )
        }.build()

        // For Java there is an extra param to pass, the continuation.
        val formatExpr = when (scope.language) {
            CodeLanguage.JAVA -> "return %M(%N, %L, %L, %L, %L)"
            CodeLanguage.KOTLIN -> "return %M(%N, %L, %L, %L)"
        }
        val args = buildList {
            add(
                RoomCoroutinesTypeNames.COROUTINES_ROOM
                    .companionMember("execute", isJvmStatic = true)
            )
            add(dbProperty)
            add(if (inTransaction) "true" else "false")
            add(cancellationSignalVar)
            add(callableImpl)
            if (scope.language == CodeLanguage.JAVA) {
                add(continuationParamName)
            }
        }.toTypedArray()
        scope.builder.addStatement(formatExpr, *args)
    }

    private fun XCodeBlock.Builder.createRunQueryAndReturnStatements(
        roomSQLiteQueryVar: String,
        canReleaseQuery: Boolean,
        dbProperty: XPropertySpec,
        inTransaction: Boolean,
        scope: CodeGenScope,
        cancellationSignalVar: String
    ) {
        val transactionWrapper = if (inTransaction) {
            transactionWrapper(dbProperty.name)
        } else {
            null
        }
        val shouldCopyCursor = adapter?.shouldCopyCursor() == true
        val outVar = scope.getTmpVar("_result")
        val cursorVar = scope.getTmpVar("_cursor")
        transactionWrapper?.beginTransactionWithControlFlow()
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
                cancellationSignalVar
            )
        )
        beginControlFlow("try").apply {
            val adapterScope = scope.fork()
            adapter?.convert(outVar, cursorVar, adapterScope)
            add(adapterScope.generate())
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
        transactionWrapper?.endTransactionWithControlFlow()
    }

    override fun isMigratedToDriver(): Boolean = adapter?.isMigratedToDriver() == true

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
            "return %M(%N, %L, %L, %L)",
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
            },
            continuationParamName
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
            "performReadTransactionSuspending"
        } else {
            "performReadSuspending"
        }
}
