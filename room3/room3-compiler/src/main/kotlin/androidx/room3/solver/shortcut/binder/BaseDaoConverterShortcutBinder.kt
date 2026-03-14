/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.room3.solver.shortcut.binder

import androidx.room3.compiler.codegen.XCodeBlock
import androidx.room3.compiler.codegen.XPropertySpec
import androidx.room3.compiler.processing.XType
import androidx.room3.ext.InvokeWithLambdaParameter
import androidx.room3.ext.LambdaSpec
import androidx.room3.ext.RoomMemberNames.DB_UTIL_PERFORM_SUSPENDING
import androidx.room3.ext.SQLiteDriverTypeNames
import androidx.room3.solver.CodeGenScope
import androidx.room3.solver.types.DaoReturnTypeConverter
import androidx.room3.solver.types.DaoReturnTypeConverter.OptionalParam

abstract class BaseDaoConverterShortcutBinder(val converter: DaoReturnTypeConverter) {
    /**
     * Used for Shortcut methods (@Insert, @Update, @Delete, @Upsert). This uses the same Converter
     * wrapping logic but delegates the execution body to the provided [generateBlock].
     */
    fun convertAndReturnShortcut(
        typeArg: XType,
        dbProperty: XPropertySpec,
        scope: CodeGenScope,
        generateBlock: (innerScope: CodeGenScope, connectionVar: String) -> Unit,
    ) {
        val lambdaSpec =
            object :
                LambdaSpec(
                    parameterTypeName = null,
                    parameterName = null,
                    returnTypeName = converter.to.asTypeName(),
                    javaLambdaSyntaxAvailable = scope.javaLambdaSyntaxAvailable,
                ) {
                override fun XCodeBlock.Builder.body(scope: CodeGenScope) {
                    scope.builder.add(
                        "%L",
                        setUpPerformBlock(scope, dbProperty, typeArg, generateBlock),
                    )
                }
            }
        val args =
            converter.requiredParameters.map {
                when (it) {
                    OptionalParam.ROOM_DB -> dbProperty.name
                    OptionalParam.IN_TRANSACTION -> true
                    else -> error("Unexpected optional param in converter: $it")
                }
            }
        val convertBlock =
            InvokeWithLambdaParameter(
                scope = scope,
                functionCall = converter.buildStatement(typeArg.asTypeName(), scope),
                argFormat = List(args.size) { "%L" },
                args = args,
                lambdaSpec = lambdaSpec,
            )
        scope.builder.add("return %L", convertBlock)
    }

    private fun setUpPerformBlock(
        scope: CodeGenScope,
        dbProperty: XPropertySpec,
        typeArg: XType,
        generateBlock: (innerScope: CodeGenScope, connectionVar: String) -> Unit,
    ): XCodeBlock {
        val connectionVar = scope.getTmpVar("_connection")
        return InvokeWithLambdaParameter(
            scope = scope,
            functionName = DB_UTIL_PERFORM_SUSPENDING,
            argFormat = listOf("%N", "%L", "%L"),
            args = listOf(dbProperty, /* isReadOnly= */ false, /* inTransaction= */ true),
            lambdaSpec =
                object :
                    LambdaSpec(
                        parameterTypeName = SQLiteDriverTypeNames.CONNECTION,
                        parameterName = connectionVar,
                        returnTypeName = typeArg.asTypeName(),
                        javaLambdaSyntaxAvailable = scope.javaLambdaSyntaxAvailable,
                    ) {
                    override fun XCodeBlock.Builder.body(scope: CodeGenScope) {
                        generateBlock(scope, connectionVar)
                    }
                },
        )
    }
}
