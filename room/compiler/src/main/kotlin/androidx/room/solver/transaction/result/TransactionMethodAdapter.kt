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

import androidx.room.ext.DEFAULT_IMPLS_CLASS_NAME
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.T
import androidx.room.compiler.processing.XType
import androidx.room.solver.CodeGenScope
import androidx.room.vo.TransactionMethod
import com.squareup.javapoet.ClassName

/**
 * Class that knows how to generate the transaction method delegate statement.
 */
class TransactionMethodAdapter(
    private val methodName: String,
    private val callType: TransactionMethod.CallType
) {
    fun createDelegateToSuperStatement(
        returnType: XType,
        parameterNames: List<String>,
        daoName: ClassName,
        daoImplName: ClassName,
        resultVar: String? = null, // name of result var to assign to, null if none
        returnStmt: Boolean = false, // true or false to prepend statement with 'return'
        scope: CodeGenScope
    ) {
        scope.builder().apply {
            val params: MutableList<Any> = mutableListOf()
            val format = buildString {
                if (resultVar != null && returnStmt) {
                    throw IllegalStateException(
                        "Can't assign to var and return in the same statement."
                    )
                } else if (resultVar != null) {
                    append("$T $L = ")
                    params.add(returnType.typeName)
                    params.add(resultVar)
                } else if (returnStmt) {
                    append("return ")
                }
                when (callType) {
                    TransactionMethod.CallType.CONCRETE -> {
                        append("$T.super.$N(")
                        params.add(daoImplName)
                        params.add(methodName)
                    }
                    TransactionMethod.CallType.DEFAULT_JAVA8 -> {
                        append("$T.super.$N(")
                        params.add(daoName)
                        params.add(methodName)
                    }
                    TransactionMethod.CallType.DEFAULT_KOTLIN -> {
                        append("$T.$N.$N($T.this")
                        params.add(daoName)
                        params.add(DEFAULT_IMPLS_CLASS_NAME)
                        params.add(methodName)
                        params.add(daoImplName)
                    }
                }
                var first = callType != TransactionMethod.CallType.DEFAULT_KOTLIN
                parameterNames.forEach {
                    if (first) {
                        first = false
                    } else {
                        append(", ")
                    }
                    append(L)
                    params.add(it)
                }
                append(")")
            }
            addStatement(format, *params.toTypedArray())
        }
    }
}