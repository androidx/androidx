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

import androidx.room.ext.CallableTypeSpecBuilder
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomCoroutinesTypeNames
import androidx.room.ext.T
import androidx.room.ext.arrayTypeName
import androidx.room.compiler.processing.XType
import androidx.room.solver.CodeGenScope
import com.squareup.javapoet.FieldSpec

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
        dbField: FieldSpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val callableImpl = CallableTypeSpecBuilder(typeArg.typeName) {
            createRunQueryAndReturnStatements(
                builder = this,
                roomSQLiteQueryVar = roomSQLiteQueryVar,
                dbField = dbField,
                inTransaction = inTransaction,
                scope = scope,
                cancellationSignalVar = "null"
            )
        }.apply {
            if (canReleaseQuery) {
                addMethod(createFinalizeMethod(roomSQLiteQueryVar))
            }
        }.build()

        scope.builder().apply {
            val tableNamesList = tableNames.joinToString(",") { "\"$it\"" }
            addStatement(
                "return $T.createFlow($N, $L, new $T{$L}, $L)",
                RoomCoroutinesTypeNames.COROUTINES_ROOM,
                dbField,
                if (inTransaction) "true" else "false",
                String::class.arrayTypeName,
                tableNamesList,
                callableImpl
            )
        }
    }
}