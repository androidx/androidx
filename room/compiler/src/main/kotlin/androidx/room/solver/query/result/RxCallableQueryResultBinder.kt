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
import androidx.room.ext.RoomRxJava2TypeNames
import androidx.room.ext.RxJava2TypeNames
import androidx.room.ext.S
import androidx.room.ext.T
import androidx.room.ext.typeName
import androidx.room.solver.CodeGenScope
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeMirror

/**
 * Generic Result binder for Rx classes that accept a callable.
 */
class RxCallableQueryResultBinder(
        private val rxType: RxType,
        val typeArg: TypeMirror,
        adapter: QueryResultAdapter?) : QueryResultBinder(adapter) {
    override fun convertAndReturn(
            roomSQLiteQueryVar: String,
            canReleaseQuery: Boolean,
            dbField: FieldSpec,
            inTransaction: Boolean,
            scope: CodeGenScope) {
        val callable = TypeSpec.anonymousClassBuilder("").apply {
            val typeName = typeArg.typeName()
            superclass(ParameterizedTypeName.get(java.util.concurrent.Callable::class.typeName(),
                    typeName))
            addMethod(createCallMethod(
                    roomSQLiteQueryVar = roomSQLiteQueryVar,
                    dbField = dbField,
                    inTransaction = inTransaction,
                    scope = scope))
            if (canReleaseQuery) {
                addMethod(createFinalizeMethod(roomSQLiteQueryVar))
            }
        }.build()
        scope.builder().apply {
            addStatement("return $T.fromCallable($L)", rxType.className, callable)
        }
    }

    private fun createCallMethod(
            roomSQLiteQueryVar: String,
            dbField: FieldSpec,
            inTransaction: Boolean,
            scope: CodeGenScope): MethodSpec {
        val adapterScope = scope.fork()
        return MethodSpec.methodBuilder("call").apply {
            returns(typeArg.typeName())
            addException(Exception::class.typeName())
            addModifiers(Modifier.PUBLIC)
            addAnnotation(Override::class.java)
            val transactionWrapper = if (inTransaction) {
                transactionWrapper(dbField)
            } else {
                null
            }
            transactionWrapper?.beginTransactionWithControlFlow()
            val outVar = scope.getTmpVar("_result")
            val cursorVar = scope.getTmpVar("_cursor")
            addStatement("final $T $L = $N.query($L)", AndroidTypeNames.CURSOR, cursorVar,
                    dbField, roomSQLiteQueryVar)
            beginControlFlow("try").apply {
                adapter?.convert(outVar, cursorVar, adapterScope)
                addCode(adapterScope.generate())
                if (!rxType.canBeNull) {
                    beginControlFlow("if($L == null)", outVar).apply {
                        addStatement("throw new $T($S + $L.getSql())",
                                RoomRxJava2TypeNames.RX_EMPTY_RESULT_SET_EXCEPTION,
                                "Query returned empty result set: ",
                                roomSQLiteQueryVar)
                    }
                    endControlFlow()
                }
                transactionWrapper?.commitTransaction()
                addStatement("return $L", outVar)
            }
            nextControlFlow("finally").apply {
                addStatement("$L.close()", cursorVar)
            }
            endControlFlow()
            transactionWrapper?.endTransactionWithControlFlow()
        }.build()
    }

    private fun createFinalizeMethod(roomSQLiteQueryVar: String): MethodSpec {
        return MethodSpec.methodBuilder("finalize").apply {
            addModifiers(Modifier.PROTECTED)
            addAnnotation(Override::class.java)
            addStatement("$L.release()", roomSQLiteQueryVar)
        }.build()
    }

    enum class RxType(val className: ClassName, val canBeNull: Boolean) {
        SINGLE(RxJava2TypeNames.SINGLE, canBeNull = false),
        MAYBE(RxJava2TypeNames.MAYBE, canBeNull = true);
    }
}