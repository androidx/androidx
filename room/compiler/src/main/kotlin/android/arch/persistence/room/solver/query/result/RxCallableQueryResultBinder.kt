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

package android.arch.persistence.room.solver.query.result

import android.arch.persistence.room.ext.AndroidTypeNames
import android.arch.persistence.room.ext.L
import android.arch.persistence.room.ext.N
import android.arch.persistence.room.ext.RoomRxJava2TypeNames
import android.arch.persistence.room.ext.RxJava2TypeNames
import android.arch.persistence.room.ext.S
import android.arch.persistence.room.ext.T
import android.arch.persistence.room.ext.typeName
import android.arch.persistence.room.solver.CodeGenScope
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
class RxCallableQueryResultBinder(val rxType: RxType,
                                  val typeArg: TypeMirror,
                                  adapter: QueryResultAdapter?)
    : QueryResultBinder(adapter) {
    override fun convertAndReturn(roomSQLiteQueryVar: String,
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
        }.build()
        scope.builder().apply {
            addStatement("return $T.fromCallable($L)", rxType.className, callable)
        }
    }

    fun createCallMethod(roomSQLiteQueryVar: String,
                         dbField: FieldSpec,
                         inTransaction: Boolean,
                         scope: CodeGenScope): MethodSpec {
        val adapterScope = scope.fork()
        return MethodSpec.methodBuilder("call").apply {
            returns(typeArg.typeName())
            addException(Exception::class.typeName())
            addModifiers(Modifier.PUBLIC)
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
                addStatement("$L.release()", roomSQLiteQueryVar)
            }
            endControlFlow()
            transactionWrapper?.endTransactionWithControlFlow()
        }.build()
    }

    enum class RxType(val className: ClassName, val canBeNull: Boolean) {
        SINGLE(RxJava2TypeNames.SINGLE, canBeNull = false),
        MAYBE(RxJava2TypeNames.MAYBE, canBeNull = true);
    }
}