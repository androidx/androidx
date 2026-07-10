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

package androidx.room3.solver.transaction.result

import androidx.room3.compiler.codegen.XClassName
import androidx.room3.compiler.codegen.XCodeBlock
import androidx.room3.solver.CodeGenScope
import androidx.room3.vo.TransactionFunction

/**
 * Class that knows how to generate the transaction function delegate code. Callers should take care
 * of using the invocation code in a statement or in another block (such as a lambda).
 */
class TransactionFunctionAdapter(
    private val functionName: String,
    private val jvmMethodName: String,
    private val typeParamNames: List<String>,
    private val callType: TransactionFunction.CallType,
) {
    fun createDelegateToSuperCode(
        parameterNames: List<String>,
        daoName: XClassName,
        daoImplName: XClassName,
        scope: CodeGenScope,
    ) {
        scope.builder.apply {
            val delegateInvokeArgs = mutableListOf<Any>()
            val delegateInvokeFormat = buildString {
                val invokeExpr = getKotlinInvokeExpr(daoImplName)
                append("%L")
                delegateInvokeArgs.add(invokeExpr)
                parameterNames.forEachIndexed { i, param ->
                    append("%L")
                    delegateInvokeArgs.add(param)
                    if (i < parameterNames.size - 1) {
                        append(", ")
                    }
                }
                append(")")
            }
            add(delegateInvokeFormat, *delegateInvokeArgs.toTypedArray())
        }
    }

    private fun getKotlinInvokeExpr(daoImplName: XClassName): XCodeBlock =
        if (typeParamNames.isEmpty()) {
            XCodeBlock.of("super@%T.%N(", daoImplName, functionName)
        } else {
            XCodeBlock.of(
                "super@%T.%N<%L>(",
                daoImplName,
                functionName,
                typeParamNames.joinToString(),
            )
        }
}
