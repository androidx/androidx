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

import androidx.room.compiler.processing.XType
import androidx.room.ext.AndroidTypeNames
import androidx.room.ext.CallableTypeSpecBuilder
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.S
import androidx.room.ext.T
import androidx.room.solver.CodeGenScope
import androidx.room.solver.RxType
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Modifier

/**
 * Generic Result binder for Rx classes that accept a callable.
 */
internal class RxCallableQueryResultBinder(
    private val rxType: RxType,
    val typeArg: XType,
    adapter: QueryResultAdapter?
) : QueryResultBinder(adapter) {
    override fun convertAndReturn(
        roomSQLiteQueryVar: String,
        sectionsVar: String?,
        tempTableVar: String,
        canReleaseQuery: Boolean,
        dbField: FieldSpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val callable = CallableTypeSpecBuilder(typeArg.typeName) {
            fillInCallMethod(
                roomSQLiteQueryVar = roomSQLiteQueryVar,
                sectionsVar = sectionsVar,
                tempTableVar = tempTableVar,
                dbField = dbField,
                inTransaction = inTransaction,
                scope = scope
            )
        }.apply {
            if (sectionsVar != null) {
                addField(RoomTypeNames.ROOM_SQL_QUERY, roomSQLiteQueryVar, Modifier.PRIVATE)
            }
            if (canReleaseQuery) {
                addMethod(createFinalizeMethod(roomSQLiteQueryVar))
            }
        }.build()
        scope.builder().apply {
            if (rxType.isSingle()) {
                addStatement("return $T.createSingle($L)", rxType.version.rxRoomClassName, callable)
            } else {
                addStatement("return $T.fromCallable($L)", rxType.className, callable)
            }
        }
    }

    private fun MethodSpec.Builder.fillInCallMethod(
        roomSQLiteQueryVar: String,
        sectionsVar: String?,
        tempTableVar: String,
        dbField: FieldSpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val adapterScope = scope.fork()
        val transactionWrapper = if (inTransaction) {
            transactionWrapper(dbField)
        } else {
            null
        }
        transactionWrapper?.beginTransactionWithControlFlow()
        val shouldCopyCursor = adapter?.shouldCopyCursor() == true
        val outVar = scope.getTmpVar("_result")
        val cursorVar = scope.getTmpVar("_cursor")
        val largeQueryVar = scope.getTmpVar("_isLargeQuery")

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
                "null"
            )

            adapter?.convert(outVar, cursorVar, adapterScope)
            addCode(adapterScope.generate())
            if (!rxType.canBeNull) {
                beginControlFlow("if($L == null)", outVar).apply {
                    addStatement(
                        "throw new $T($S + $L.getSql())",
                        rxType.version.emptyResultExceptionClassName,
                        "Query returned empty result set: ",
                        roomSQLiteQueryVar
                    )
                }
                endControlFlow()
            }

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
        transactionWrapper?.endTransactionWithControlFlow()
    }

    private fun createFinalizeMethod(roomSQLiteQueryVar: String): MethodSpec {
        return MethodSpec.methodBuilder("finalize").apply {
            addModifiers(Modifier.PROTECTED)
            addAnnotation(Override::class.java)
            beginControlFlow("if ($L != null)", roomSQLiteQueryVar).apply {
                addStatement("$L.release()", roomSQLiteQueryVar)
            }.endControlFlow()
        }.build()
    }
}