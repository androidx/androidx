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

import androidx.room.ext.N
import androidx.room.compiler.processing.XType
import androidx.room.ext.isNotVoid
import androidx.room.solver.CodeGenScope
import androidx.room.solver.transaction.result.TransactionMethodAdapter
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec

/**
 * Binder that knows how to write instant (blocking) transaction wrapper methods.
 */
class InstantTransactionMethodBinder(
    adapter: TransactionMethodAdapter
) : TransactionMethodBinder(adapter) {
    override fun executeAndReturn(
        returnType: XType,
        parameterNames: List<String>,
        daoName: ClassName,
        daoImplName: ClassName,
        dbField: FieldSpec,
        scope: CodeGenScope
    ) {
        scope.builder().apply {
            addStatement("$N.beginTransaction()", dbField)
            beginControlFlow("try").apply {
                val returnsValue = returnType.isNotVoid()
                val resultVar = if (returnsValue) {
                    scope.getTmpVar("_result")
                } else {
                    null
                }

                val adapterScope = scope.fork()
                adapter.createDelegateToSuperStatement(
                    returnType = returnType,
                    parameterNames = parameterNames,
                    daoName = daoName,
                    daoImplName = daoImplName,
                    resultVar = resultVar,
                    scope = adapterScope
                )
                add(adapterScope.generate())

                addStatement("$N.setTransactionSuccessful()", dbField)
                if (returnsValue) {
                    addStatement("return $N", resultVar)
                }
            }
            nextControlFlow("finally").apply {
                addStatement("$N.endTransaction()", dbField)
            }
            endControlFlow()
        }
    }
}