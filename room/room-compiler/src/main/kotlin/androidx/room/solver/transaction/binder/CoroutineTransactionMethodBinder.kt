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
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XType
import androidx.room.ext.InvokeWithLambdaParameter
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.LambdaSpec
import androidx.room.ext.RoomMemberNames.DB_UTIL_PERFORM_IN_TRANSACTION_SUSPENDING
import androidx.room.solver.CodeGenScope
import androidx.room.solver.transaction.result.TransactionMethodAdapter

/** Binder that knows how to write suspending transaction wrapper methods. */
class CoroutineTransactionMethodBinder(
    private val returnType: XType,
    adapter: TransactionMethodAdapter,
    private val continuationParamName: String
) : TransactionMethodBinder(adapter) {
    override fun executeAndReturn(
        parameterNames: List<String>,
        daoName: XClassName,
        daoImplName: XClassName,
        dbProperty: XPropertySpec,
        scope: CodeGenScope
    ) {
        val innerContinuationParamName = scope.getTmpVar("_cont")
        val performBlock =
            InvokeWithLambdaParameter(
                scope = scope,
                functionName = DB_UTIL_PERFORM_IN_TRANSACTION_SUSPENDING,
                argFormat = listOf("%N"),
                args = listOf(dbProperty),
                continuationParamName = continuationParamName,
                lambdaSpec =
                    object :
                        LambdaSpec(
                            parameterTypeName =
                                KotlinTypeNames.CONTINUATION.parametrizedBy(
                                    XTypeName.getConsumerSuperName(returnType.asTypeName())
                                ),
                            parameterName = innerContinuationParamName,
                            returnTypeName = KotlinTypeNames.ANY,
                            javaLambdaSyntaxAvailable = scope.javaLambdaSyntaxAvailable
                        ) {
                        override fun XCodeBlock.Builder.body(scope: CodeGenScope) {
                            val adapterScope = scope.fork()
                            adapter.createDelegateToSuperCode(
                                parameterNames =
                                    when (scope.language) {
                                        CodeLanguage.JAVA ->
                                            parameterNames + innerContinuationParamName
                                        CodeLanguage.KOTLIN -> parameterNames
                                    },
                                daoName = daoName,
                                daoImplName = daoImplName,
                                scope = adapterScope
                            )
                            val returnPrefix =
                                when (scope.language) {
                                    CodeLanguage.JAVA -> "return "
                                    CodeLanguage.KOTLIN -> ""
                                }
                            addStatement("$returnPrefix%L", adapterScope.generate())
                        }
                    }
            )
        scope.builder.add("return %L", performBlock)
    }
}
