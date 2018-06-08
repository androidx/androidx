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

import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomRxJava2TypeNames
import androidx.room.ext.T
import androidx.room.ext.arrayTypeName
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
 * Binds the result as an RxJava2 Flowable.
 */
class FlowableQueryResultBinder(val typeArg: TypeMirror, val queryTableNames: Set<String>,
                                adapter: QueryResultAdapter?)
    : BaseObservableQueryResultBinder(adapter) {
    override fun convertAndReturn(roomSQLiteQueryVar: String,
                                  canReleaseQuery: Boolean,
                                  dbField: FieldSpec,
                                  inTransaction: Boolean,
                                  scope: CodeGenScope) {
        val callableImpl = TypeSpec.anonymousClassBuilder("").apply {
            val typeName = typeArg.typeName()
            superclass(ParameterizedTypeName.get(java.util.concurrent.Callable::class.typeName(),
                    typeName))
            addMethod(MethodSpec.methodBuilder("call").apply {
                returns(typeName)
                addException(Exception::class.typeName())
                addModifiers(Modifier.PUBLIC)
                addAnnotation(Override::class.java)
                createRunQueryAndReturnStatements(builder = this,
                        roomSQLiteQueryVar = roomSQLiteQueryVar,
                        inTransaction = inTransaction,
                        dbField = dbField,
                        scope = scope)
            }.build())
            if (canReleaseQuery) {
                addMethod(createFinalizeMethod(roomSQLiteQueryVar))
            }
        }.build()
        scope.builder().apply {
            val tableNamesList = queryTableNames.joinToString(",") { "\"$it\"" }
            addStatement("return $T.createFlowable($N, new $T{$L}, $L)",
                    RoomRxJava2TypeNames.RX_ROOM, DaoWriter.dbField,
                    String::class.arrayTypeName(), tableNamesList, callableImpl)
        }
    }
}
