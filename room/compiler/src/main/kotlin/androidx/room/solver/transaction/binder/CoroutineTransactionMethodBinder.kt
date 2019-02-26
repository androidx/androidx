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

package androidx.room.solver.transaction.binder

import androidx.room.ext.Function2TypeSpecBuilder
import androidx.room.ext.KotlinTypeNames.CONTINUATION
import androidx.room.ext.KotlinTypeNames.COROUTINE_SCOPE
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomTypeNames.ROOM_DB_KT
import androidx.room.ext.T
import androidx.room.ext.typeName
import androidx.room.solver.CodeGenScope
import androidx.room.solver.transaction.result.TransactionMethodAdapter
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.WildcardTypeName
import javax.lang.model.type.TypeMirror

/**
 * Binder that knows how to write suspending transaction wrapper methods.
 */
class CoroutineTransactionMethodBinder(
    adapter: TransactionMethodAdapter,
    private val continuationParamName: String
) : TransactionMethodBinder(adapter) {
    override fun executeAndReturn(
        returnType: TypeMirror,
        parameterNames: List<String>,
        daoName: ClassName,
        daoImplName: ClassName,
        dbField: FieldSpec,
        scope: CodeGenScope
    ) {
        val scopeParamName = "__scope"
        val innerContinuationParamName = "__cont"
        val functionImpl = Function2TypeSpecBuilder(
            parameter1 = COROUTINE_SCOPE to scopeParamName,
            parameter2 = ParameterizedTypeName.get(
                CONTINUATION,
                WildcardTypeName.supertypeOf(returnType.typeName())
            ) to innerContinuationParamName,
            returnTypeName = ClassName.OBJECT
        ) {
            val adapterScope = scope.fork()
            adapter.createDelegateToSuperStatement(
                returnType = returnType,
                parameterNames = parameterNames + innerContinuationParamName,
                daoName = daoName,
                daoImplName = daoImplName,
                returnStmt = true,
                scope = adapterScope
            )
            addCode(adapterScope.generate())
        }.build()

        scope.builder().apply {
            addStatement(
                "return $T.withTransaction($N, $L, $N)",
                ROOM_DB_KT, dbField, functionImpl, continuationParamName
            )
        }
    }
}