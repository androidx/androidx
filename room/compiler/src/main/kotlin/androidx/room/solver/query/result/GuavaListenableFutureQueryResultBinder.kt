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

import androidx.room.ext.AndroidTypeNames
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomGuavaTypeNames
import androidx.room.ext.T
import androidx.room.ext.typeName
import androidx.room.solver.CodeGenScope
import androidx.room.writer.DaoWriter
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeMirror

/**
 * A ResultBinder that emits a ListenableFuture<T> where T is the input {@code typeArg}.
 *
 * <p>The Future runs on the background thread Executor.
 */
class GuavaListenableFutureQueryResultBinder(
        val typeArg: TypeMirror,
        adapter: QueryResultAdapter?)
    : BaseObservableQueryResultBinder(adapter) {

    override fun convertAndReturn(
            roomSQLiteQueryVar: String,
            canReleaseQuery: Boolean,
            dbField: FieldSpec,
            inTransaction: Boolean,
            scope: CodeGenScope) {
        // Callable<T>
        val callableImpl = createCallableOfT(
                roomSQLiteQueryVar,
                dbField,
                inTransaction,
                scope)

        scope.builder().apply {
            addStatement(
                    "return $T.createListenableFuture($L, $L, $L)",
                    RoomGuavaTypeNames.GUAVA_ROOM,
                    callableImpl,
                    roomSQLiteQueryVar,
                    canReleaseQuery)
        }
    }

    /**
     * Returns an anonymous subclass of Callable<T> that executes the database transaction and
     * constitutes the result T.
     *
     * <p>Note that this method does not release the query object.
     */
    private fun createCallableOfT(
            roomSQLiteQueryVar: String,
            dbField: FieldSpec,
            inTransaction: Boolean,
            scope: CodeGenScope): TypeSpec {
        return TypeSpec.anonymousClassBuilder("").apply {
            superclass(
                    ParameterizedTypeName.get(java.util.concurrent.Callable::class.typeName(),
                            typeArg.typeName()))
            addMethod(
                    MethodSpec.methodBuilder("call").apply {
                        // public T call() throws Exception {}
                        returns(typeArg.typeName())
                        addAnnotation(Override::class.typeName())
                        addModifiers(Modifier.PUBLIC)
                        addException(Exception::class.typeName())

                        // Body.
                        val transactionWrapper = if (inTransaction) {
                            transactionWrapper(dbField)
                        } else {
                            null
                        }
                        transactionWrapper?.beginTransactionWithControlFlow()
                        apply {
                            val outVar = scope.getTmpVar("_result")
                            val cursorVar = scope.getTmpVar("_cursor")
                            addStatement("final $T $L = $N.query($L)", AndroidTypeNames.CURSOR,
                                    cursorVar,
                                    DaoWriter.dbField, roomSQLiteQueryVar)
                            beginControlFlow("try").apply {
                                val adapterScope = scope.fork()
                                adapter?.convert(outVar, cursorVar, adapterScope)
                                addCode(adapterScope.builder().build())
                                transactionWrapper?.commitTransaction()
                                addStatement("return $L", outVar)
                            }
                            nextControlFlow("finally").apply {
                                addStatement("$L.close()", cursorVar)
                            }
                            endControlFlow()
                        }
                        transactionWrapper?.endTransactionWithControlFlow()
                    }.build())
        }.build()
    }
}
