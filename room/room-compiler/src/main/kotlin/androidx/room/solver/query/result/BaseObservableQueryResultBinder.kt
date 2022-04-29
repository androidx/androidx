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

import androidx.room.ext.AndroidTypeNames
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.S
import androidx.room.ext.T
import androidx.room.solver.CodeGenScope
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Modifier

/**
 * Base class for query result binders that observe the database. It includes common functionality
 * like creating a finalizer to release the query or creating the actual adapter call code.
 */
abstract class BaseObservableQueryResultBinder(adapter: QueryResultAdapter?) :
    QueryResultBinder(adapter) {

    protected fun createFinalizeMethod(roomSQLiteQueryVar: String): MethodSpec {
        return MethodSpec.methodBuilder("finalize").apply {
            addModifiers(Modifier.PROTECTED)
            addAnnotation(Override::class.java)
            beginControlFlow("if ($L != null)", roomSQLiteQueryVar).apply {
                addStatement("$L.release()", roomSQLiteQueryVar)
            }.endControlFlow()
        }.build()
    }

    protected fun createRunQueryAndReturnStatements(
        builder: MethodSpec.Builder,
        roomSQLiteQueryVar: String,
        sectionsVar: String?,
        tempTableVar: String,
        dbField: FieldSpec,
        inTransaction: Boolean,
        scope: CodeGenScope,
        cancellationSignalVar: String
    ) {
        val transactionWrapper = if (inTransaction) {
            builder.transactionWrapper(dbField)
        } else {
            null
        }
        val shouldCopyCursor = adapter?.shouldCopyCursor() == true
        val outVar = scope.getTmpVar("_result")
        val cursorVar = scope.getTmpVar("_cursor")
        val largeQueryVar = scope.getTmpVar("_isLargeQuery")

        transactionWrapper?.beginTransactionWithControlFlow()
        builder.apply {
            addStatement("$T $L = null", AndroidTypeNames.CURSOR, cursorVar)

            beginControlFlow("try").apply {
                addStatement("$T $L = false", TypeName.BOOLEAN, largeQueryVar)

                if (sectionsVar != null) {
                    val pairVar = scope.getTmpVar("_resultPair")
                    addStatement(
                        "final $T $L = $T.prepareQuery($N, $L, $S, $L, false)",
                        ParameterizedTypeName.get(
                            ClassName.get(Pair::class.java),
                            RoomTypeNames.ROOM_SQL_QUERY,
                            TypeName.BOOLEAN.box()
                        ),
                        pairVar,
                        RoomTypeNames.QUERY_UTIL, dbField, inTransaction, tempTableVar, sectionsVar
                    )
                    addStatement("$L = $L.getFirst()", roomSQLiteQueryVar, pairVar)
                    addStatement("$L = $L.getSecond()", largeQueryVar, pairVar)
                }

                addStatement(
                    "$L = $T.query($N, $L, $L, $L)",
                    cursorVar,
                    RoomTypeNames.DB_UTIL,
                    dbField,
                    roomSQLiteQueryVar,
                    if (shouldCopyCursor) "true" else "false",
                    cancellationSignalVar
                )

                val adapterScope = scope.fork()
                adapter?.convert(outVar, cursorVar, adapterScope)
                addCode(adapterScope.builder().build())

                beginControlFlow("if ($L)", largeQueryVar).apply {
                    addStatement(
                        "$N.getOpenHelper().getWritableDatabase().execSQL($S)",
                        dbField,
                        "DROP TABLE IF EXISTS $tempTableVar"
                    )
                    if (!inTransaction) {
                        addStatement("$N.setTransactionSuccessful()", dbField)
                        addStatement("$N.endTransaction()", dbField)
                    }
                }.endControlFlow()
                transactionWrapper?.commitTransaction()

                addStatement("return $L", outVar)
            }
            nextControlFlow("finally").apply {
                beginControlFlow("if ($L != null)", cursorVar).apply {
                    addStatement("$L.close()", cursorVar)
                }.endControlFlow()
            }
            endControlFlow()
        }
        transactionWrapper?.endTransactionWithControlFlow()
    }
}
