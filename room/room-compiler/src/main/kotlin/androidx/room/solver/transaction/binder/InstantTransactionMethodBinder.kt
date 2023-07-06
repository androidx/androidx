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

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.isKotlinUnit
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.isNotKotlinUnit
import androidx.room.ext.isNotVoid
import androidx.room.solver.CodeGenScope
import androidx.room.solver.transaction.result.TransactionMethodAdapter

/**
 * Binder that knows how to write instant (blocking) transaction wrapper methods.
 */
class InstantTransactionMethodBinder(
    adapter: TransactionMethodAdapter
) : TransactionMethodBinder(adapter) {
    override fun executeAndReturn(
        returnType: XType,
        parameterNames: List<String>,
        daoName: XClassName,
        daoImplName: XClassName,
        dbProperty: XPropertySpec,
        scope: CodeGenScope
    ) {
        scope.builder.apply {
            addStatement("%N.beginTransaction()", dbProperty)
            beginControlFlow("try").apply {
                val returnsValue = returnType.isNotVoid() && returnType.isNotKotlinUnit()
                val resultVar = if (returnsValue) {
                    scope.getTmpVar("_result")
                } else {
                    null
                }
                if (resultVar != null) {
                    addLocalVariable(
                        name = resultVar,
                        typeName = returnType.asTypeName()
                    )
                }

                val adapterScope = scope.fork()
                adapter.createDelegateToSuperCode(
                    parameterNames = parameterNames,
                    daoName = daoName,
                    daoImplName = daoImplName,
                    resultVar = resultVar,
                    scope = adapterScope
                )
                addStatement("%L", adapterScope.generate())

                addStatement("%N.setTransactionSuccessful()", dbProperty)
                if (resultVar != null) {
                    addStatement("return %N", resultVar)
                } else if (returnType.isKotlinUnit() && language == CodeLanguage.JAVA) {
                    addStatement("return %T.INSTANCE", KotlinTypeNames.UNIT)
                }
            }
            nextControlFlow("finally").apply {
                addStatement("%N.endTransaction()", dbProperty)
            }
            endControlFlow()
        }
    }
}
