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
import androidx.room.compiler.codegen.box
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.isKotlinUnit
import androidx.room.compiler.processing.isVoid
import androidx.room.ext.InvokeWithLambdaParameter
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.LambdaSpec
import androidx.room.ext.RoomMemberNames.DB_UTIL_PERFORM_BLOCKING
import androidx.room.ext.SQLiteDriverTypeNames
import androidx.room.solver.CodeGenScope
import androidx.room.solver.transaction.result.TransactionMethodAdapter

/** Binder that knows how to write instant (blocking) transaction wrapper methods. */
class InstantTransactionMethodBinder(
    private val returnType: XType,
    adapter: TransactionMethodAdapter,
) : TransactionMethodBinder(adapter) {
    override fun executeAndReturn(
        parameterNames: List<String>,
        daoName: XClassName,
        daoImplName: XClassName,
        dbProperty: XPropertySpec,
        scope: CodeGenScope
    ) {
        val returnPrefix =
            when (scope.language) {
                CodeLanguage.JAVA ->
                    if (returnType.isVoid() || returnType.isKotlinUnit()) "" else "return "
                CodeLanguage.KOTLIN -> "return "
            }
        val performBlock =
            InvokeWithLambdaParameter(
                scope = scope,
                functionName = DB_UTIL_PERFORM_BLOCKING,
                argFormat = listOf("%N", "%L", "%L"),
                args = listOf(dbProperty, /* isReadOnly= */ false, /* inTransaction= */ true),
                lambdaSpec =
                    object :
                        LambdaSpec(
                            parameterTypeName = SQLiteDriverTypeNames.CONNECTION,
                            parameterName =
                                when (scope.language) {
                                    CodeLanguage.JAVA -> scope.getTmpVar("_connection")
                                    CodeLanguage.KOTLIN -> "_"
                                },
                            returnTypeName = returnType.asTypeName().box(),
                            javaLambdaSyntaxAvailable = scope.javaLambdaSyntaxAvailable
                        ) {
                        override fun XCodeBlock.Builder.body(scope: CodeGenScope) {
                            val adapterScope = scope.fork()
                            adapter.createDelegateToSuperCode(
                                parameterNames = parameterNames,
                                daoName = daoName,
                                daoImplName = daoImplName,
                                scope = adapterScope
                            )
                            when (scope.language) {
                                CodeLanguage.JAVA -> {
                                    addStatement("$returnPrefix%L", adapterScope.generate())
                                    if (returnPrefix.isEmpty()) {
                                        addStatement("return %T.INSTANCE", KotlinTypeNames.UNIT)
                                    }
                                }
                                CodeLanguage.KOTLIN -> {
                                    addStatement("%L", adapterScope.generate())
                                }
                            }
                        }
                    }
            )
        scope.builder.add("$returnPrefix%L", performBlock)
    }
}
