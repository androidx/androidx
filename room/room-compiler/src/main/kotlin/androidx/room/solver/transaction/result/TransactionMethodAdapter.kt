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

package androidx.room.solver.transaction.result

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.ext.DEFAULT_IMPLS_CLASS_NAME
import androidx.room.solver.CodeGenScope
import androidx.room.vo.TransactionMethod

/**
 * Class that knows how to generate the transaction method delegate code. Callers should take
 * care of using the invocation code in a statement or in another block (such as a lambda).
 */
class TransactionMethodAdapter(
    private val methodName: String,
    private val jvmMethodName: String,
    private val callType: TransactionMethod.CallType
) {
    fun createDelegateToSuperCode(
        parameterNames: List<String>,
        daoName: XClassName,
        daoImplName: XClassName,
        resultVar: String? = null, // name of result var to assign to, null if none
        returnStmt: Boolean = false, // true or false to prepend statement with 'return'
        scope: CodeGenScope
    ) {
        scope.builder.apply {
            val args = mutableListOf<Any>()
            val format = buildString {
                if (resultVar != null && returnStmt) {
                    error("Can't assign to var and return in the same statement.")
                } else if (resultVar != null) {
                    append("%L = ")
                    args.add(resultVar)
                } else if (returnStmt) {
                    append("return ")
                }

                val invokeExpr = when (scope.language) {
                    CodeLanguage.JAVA -> scope.getJavaInvokeExpr(daoName, daoImplName)
                    CodeLanguage.KOTLIN -> scope.getKotlinInvokeExpr(daoImplName)
                }
                append("%L")
                args.add(invokeExpr)

                if (scope.language == CodeLanguage.JAVA &&
                    callType == TransactionMethod.CallType.DEFAULT_KOTLIN &&
                    parameterNames.isNotEmpty()
                ) {
                    // An invoke to DefaultImpls has an extra 1st param so we need a comma if there
                    // are more params.
                    append(", ")
                }
                parameterNames.forEachIndexed { i, param ->
                    append("%L")
                    args.add(param)
                    if (i < parameterNames.size - 1) {
                        append(", ")
                    }
                }
                append(")")
            }
            add(format, *args.toTypedArray())
        }
    }

    private fun CodeGenScope.getJavaInvokeExpr(
        daoName: XClassName,
        daoImplName: XClassName,
    ): XCodeBlock = when (callType) {
        TransactionMethod.CallType.CONCRETE -> {
            XCodeBlock.of(
                language,
                "%T.super.%N(",
                daoImplName, jvmMethodName
            )
        }
        TransactionMethod.CallType.DEFAULT_JAVA8 -> {
            XCodeBlock.of(
                language,
                "%T.super.%N(",
                daoName, jvmMethodName
            )
        }
        TransactionMethod.CallType.DEFAULT_KOTLIN -> {
            XCodeBlock.of(
                language,
                "%T.%N.%N(%T.this",
                daoName, DEFAULT_IMPLS_CLASS_NAME, jvmMethodName, daoImplName
            )
        }
    }

    private fun CodeGenScope.getKotlinInvokeExpr(
        daoImplName: XClassName,
    ): XCodeBlock = XCodeBlock.of(
        language,
        "super@%T.%N(",
        daoImplName, methodName
    )
}
