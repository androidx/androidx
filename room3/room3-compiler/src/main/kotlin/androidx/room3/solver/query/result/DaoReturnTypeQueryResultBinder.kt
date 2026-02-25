/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.room3.solver.query.result

import androidx.room3.compiler.codegen.CodeLanguage
import androidx.room3.compiler.codegen.XCodeBlock
import androidx.room3.compiler.codegen.XCodeBlock.Builder.Companion.applyTo
import androidx.room3.compiler.codegen.XPropertySpec
import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.isArray
import androidx.room3.compiler.processing.isBoolean
import androidx.room3.ext.ArrayLiteral
import androidx.room3.ext.CommonTypeNames
import androidx.room3.ext.InvokeWithLambdaParameter
import androidx.room3.ext.LambdaSpec
import androidx.room3.ext.RoomMemberNames.DB_UTIL_PERFORM_SUSPENDING
import androidx.room3.ext.RoomTypeNames.ROOM_DB
import androidx.room3.ext.SQLiteDriverTypeNames
import androidx.room3.solver.CodeGenScope
import androidx.room3.solver.types.DaoReturnTypeConverter

/** Converts the query into a DAO return type and returns it. No query is run until necessary. */
class DaoReturnTypeQueryResultBinder(
    val typeArg: XType,
    val tableNames: Set<String>,
    adapter: QueryResultAdapter?,
    val converter: DaoReturnTypeConverter,
) : QueryResultBinder(adapter) {

    override fun convertAndReturn(
        sqlQueryVar: String,
        dbProperty: XPropertySpec,
        bindStatement: (CodeGenScope.(String) -> Unit)?,
        returnTypeName: XTypeName,
        inTransaction: Boolean,
        scope: CodeGenScope,
    ) {
        val arrayOfTableNamesLiteral =
            ArrayLiteral(CommonTypeNames.STRING, *tableNames.toTypedArray())
        val connectionVar = scope.getTmpVar("_connection")
        val args = buildList {
            // We always have a RoomDatabase param and the lambda parameter in DAO return type
            // converters. All other params are optional, but are limited to a Boolean representing
            // `inTransaction`, an Array<String> representing `tableNames` and a RoomRawQuery
            // representing `rawQuery`. We need to have a way to check if [1] any/which of these
            // parameters have been defined in the convert function we have, [2] the order in
            // which they have been supplied.
            converter.requiredFunctionParamTypes.forEach { paramType ->
                val typeName = paramType.asTypeName()
                when {
                    typeName == ROOM_DB -> add(dbProperty.name)
                    paramType.isArray() -> add(arrayOfTableNamesLiteral)
                    paramType.isBoolean() -> add(inTransaction)
                }
            }
        }

        val convertBlock =
            InvokeWithLambdaParameter(
                scope = scope,
                functionCall = converter.buildStatement(typeArg.asTypeName(), scope),
                argFormat = List(args.size) { "%L" },
                args = args,
                lambdaSpec =
                    object :
                        LambdaSpec(
                            parameterTypeName = null,
                            parameterName = null,
                            returnTypeName = converter.to.asTypeName(),
                            javaLambdaSyntaxAvailable = scope.javaLambdaSyntaxAvailable,
                        ) {
                        override fun XCodeBlock.Builder.body(scope: CodeGenScope) {
                            val performBlock =
                                InvokeWithLambdaParameter(
                                    scope = scope,
                                    functionName = DB_UTIL_PERFORM_SUSPENDING,
                                    argFormat = listOf("%N", "%L", "%L"),
                                    args =
                                        listOf(dbProperty, /* isReadOnly= */ true, inTransaction),
                                    lambdaSpec =
                                        object :
                                            LambdaSpec(
                                                parameterTypeName =
                                                    SQLiteDriverTypeNames.CONNECTION,
                                                parameterName = connectionVar,
                                                returnTypeName = typeArg.asTypeName(),
                                                javaLambdaSyntaxAvailable =
                                                    scope.javaLambdaSyntaxAvailable,
                                            ) {
                                            override fun XCodeBlock.Builder.body(
                                                scope: CodeGenScope
                                            ) {
                                                val statementVar = scope.getTmpVar("_stmt")
                                                addLocalVal(
                                                    statementVar,
                                                    SQLiteDriverTypeNames.STATEMENT,
                                                    "%L.prepare(%L)",
                                                    connectionVar,
                                                    sqlQueryVar,
                                                )
                                                beginControlFlow("try")
                                                bindStatement?.invoke(scope, statementVar)
                                                val outVar = scope.getTmpVar("_result")
                                                adapter?.convert(outVar, statementVar, scope)
                                                applyTo { language ->
                                                    when (language) {
                                                        CodeLanguage.JAVA ->
                                                            addStatement("return %L", outVar)
                                                        CodeLanguage.KOTLIN ->
                                                            addStatement("%L", outVar)
                                                    }
                                                }
                                                nextControlFlow("finally")
                                                addStatement("%L.close()", statementVar)
                                                endControlFlow()
                                            }
                                        },
                                )
                            when (scope.language) {
                                CodeLanguage.JAVA -> scope.builder.add("return %L", performBlock)
                                CodeLanguage.KOTLIN -> scope.builder.add("%L", performBlock)
                            }
                        }
                    },
            )
        scope.builder.add("return %L", convertBlock)
    }
}
