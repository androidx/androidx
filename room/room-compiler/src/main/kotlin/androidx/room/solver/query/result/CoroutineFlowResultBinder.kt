/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.processing.XType
import androidx.room.ext.ArrayLiteral
import androidx.room.ext.CallableTypeSpecBuilder
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.RoomCoroutinesTypeNames.COROUTINES_ROOM
import androidx.room.solver.CodeGenScope

/**
 * Binds the result of a of a Kotlin Coroutine Flow<T>
 */
class CoroutineFlowResultBinder(
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
        val callableImpl = CallableTypeSpecBuilder(scope.language, typeArg.asTypeName()) {
            addCode(
                XCodeBlock.builder(language).apply {
                    createRunQueryAndReturnStatements(
                        builder = this,
                        roomSQLiteQueryVar = roomSQLiteQueryVar,
                        dbProperty = dbProperty,
                        inTransaction = inTransaction,
                        scope = scope,
                        cancellationSignalVar = "null"
                    )
                }.build()
            )
        }.apply {
            if (canReleaseQuery) {
                createFinalizeMethod(roomSQLiteQueryVar)
            }
        }.build()

        scope.builder.apply {
            val arrayOfTableNamesLiteral = ArrayLiteral(
                scope.language,
                CommonTypeNames.STRING,
                *tableNames.toTypedArray()
            )
            addStatement(
                "return %T.createFlow(%N, %L, %L, %L)",
                COROUTINES_ROOM,
                dbProperty,
                if (inTransaction) "true" else "false",
                arrayOfTableNamesLiteral,
                callableImpl
            )
        }
    }
}
