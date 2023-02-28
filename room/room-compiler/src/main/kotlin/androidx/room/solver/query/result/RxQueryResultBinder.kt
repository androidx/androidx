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

import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.processing.XType
import androidx.room.ext.ArrayLiteral
import androidx.room.ext.CallableTypeSpecBuilder
import androidx.room.ext.CommonTypeNames
import androidx.room.solver.CodeGenScope
import androidx.room.solver.RxType

/**
 * Binds the result as an RxJava2 Flowable, Publisher and Observable.
 */
internal class RxQueryResultBinder(
    private val rxType: RxType,
    val typeArg: XType,
    val queryTableNames: Set<String>,
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
                        inTransaction = inTransaction,
                        dbProperty = dbProperty,
                        scope = scope,
                        cancellationSignalVar = "null"
                    )
                }.build()
            )
        }.apply {
            if (canReleaseQuery) {
                createFinalizeMethod(roomSQLiteQueryVar)
            }
        }
        scope.builder.apply {
            val arrayOfTableNamesLiteral = ArrayLiteral(
                scope.language,
                CommonTypeNames.STRING,
                *queryTableNames.toTypedArray()
            )
            addStatement(
                "return %T.%N(%N, %L, %L, %L)",
                rxType.version.rxRoomClassName,
                rxType.factoryMethodName!!,
                dbProperty,
                if (inTransaction) "true" else "false",
                arrayOfTableNamesLiteral,
                callableImpl.build()
            )
        }
    }
}
