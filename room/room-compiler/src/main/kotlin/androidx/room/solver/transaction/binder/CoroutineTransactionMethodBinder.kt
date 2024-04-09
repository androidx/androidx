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
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XFunSpec.Builder.Companion.addStatement
import androidx.room.compiler.codegen.XMemberName.Companion.packageMember
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XType
import androidx.room.ext.Function1TypeSpec
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.RoomTypeNames
import androidx.room.solver.CodeGenScope
import androidx.room.solver.transaction.result.TransactionMethodAdapter

/**
 * Binder that knows how to write suspending transaction wrapper methods.
 */
class CoroutineTransactionMethodBinder(
    private val returnType: XType,
    adapter: TransactionMethodAdapter,
    private val continuationParamName: String,
    private val javaLambdaSyntaxAvailable: Boolean
) : TransactionMethodBinder(adapter) {
    override fun executeAndReturn(
        parameterNames: List<String>,
        daoName: XClassName,
        daoImplName: XClassName,
        dbProperty: XPropertySpec,
        scope: CodeGenScope
    ) {
        when (scope.language) {
            CodeLanguage.JAVA -> executeAndReturnJava(
                parameterNames, daoName, daoImplName, dbProperty, scope
            )
            CodeLanguage.KOTLIN -> executeAndReturnKotlin(
                parameterNames, daoName, daoImplName, dbProperty, scope
            )
        }
    }

    private fun executeAndReturnJava(
        parameterNames: List<String>,
        daoName: XClassName,
        daoImplName: XClassName,
        dbProperty: XPropertySpec,
        scope: CodeGenScope
    ) {
        val innerContinuationParamName = scope.getTmpVar("_cont")
        val adapterScope = scope.fork()
        adapter.createDelegateToSuperCode(
            parameterNames = parameterNames + innerContinuationParamName,
            daoName = daoName,
            daoImplName = daoImplName,
            scope = adapterScope
        )
        val functionImpl: Any = if (javaLambdaSyntaxAvailable) {
            XCodeBlock.of(
                scope.language,
                "(%L) -> %L",
                innerContinuationParamName, adapterScope.generate()
            )
        } else {
            Function1TypeSpec(
                language = scope.language,
                parameterTypeName = KotlinTypeNames.CONTINUATION.parametrizedBy(
                    XTypeName.getConsumerSuperName(returnType.asTypeName())
                ),
                parameterName = innerContinuationParamName,
                returnTypeName = KotlinTypeNames.ANY
            ) {
                addStatement("return %L", adapterScope.generate())
            }
        }

        scope.builder.addStatement(
            "return %M(%N, %L, %L)",
            RoomTypeNames.DB_UTIL.packageMember("performInTransactionSuspending"),
            dbProperty,
            functionImpl,
            continuationParamName
        )
    }

    private fun executeAndReturnKotlin(
        parameterNames: List<String>,
        daoName: XClassName,
        daoImplName: XClassName,
        dbProperty: XPropertySpec,
        scope: CodeGenScope
    ) {
        scope.builder.apply {
            beginControlFlow(
                "return %M(%N) {",
                RoomTypeNames.DB_UTIL.packageMember("performInTransactionSuspending"),
                dbProperty,
            )
            val adapterScope = scope.fork()
            adapter.createDelegateToSuperCode(
                parameterNames = parameterNames,
                daoName = daoName,
                daoImplName = daoImplName,
                scope = adapterScope
            )
            addStatement("%L", adapterScope.generate())
            endControlFlow()
        }
    }
}
