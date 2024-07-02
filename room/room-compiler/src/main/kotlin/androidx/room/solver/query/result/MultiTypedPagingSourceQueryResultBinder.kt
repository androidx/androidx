/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.solver.query.result

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XFunSpec.Builder.Companion.addStatement
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.ext.AndroidTypeNames
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.Function1TypeSpec
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.RoomTypeNames.RAW_QUERY
import androidx.room.ext.SQLiteDriverTypeNames
import androidx.room.solver.CodeGenScope

/**
 * This Binder binds queries directly to native Paging3 PagingSource (i.e.
 * [androidx.room.paging.LimitOffsetPagingSource]) or its subclasses such as
 * [androidx.room.paging.guava.LimitOffsetListenableFuturePagingSource]. Used solely by Paging3.
 */
class MultiTypedPagingSourceQueryResultBinder(
    private val listAdapter: ListQueryResultAdapter?,
    private val tableNames: Set<String>,
    className: XClassName,
    val isBasePagingSource: Boolean
) : QueryResultBinder(listAdapter) {

    private val itemTypeName: XTypeName =
        listAdapter?.rowAdapters?.firstOrNull()?.out?.asTypeName() ?: XTypeName.ANY_OBJECT
    private val pagingSourceTypeName: XTypeName = className.parametrizedBy(itemTypeName)

    override fun isMigratedToDriver(): Boolean = isBasePagingSource

    override fun convertAndReturn(
        roomSQLiteQueryVar: String,
        canReleaseQuery: Boolean,
        dbProperty: XPropertySpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        scope.builder.apply {
            val tableNamesList = tableNames.joinToString(", ") { "\"$it\"" }
            val pagingSourceSpec =
                XTypeSpec.anonymousClassBuilder(
                        language = language,
                        argsFormat = "%L, %N, %L",
                        roomSQLiteQueryVar,
                        dbProperty,
                        tableNamesList
                    )
                    .apply {
                        superclass(pagingSourceTypeName)
                        addFunction(
                            createConvertRowsMethod(
                                scope = scope,
                                stmtParamName = "cursor",
                                stmtParamTypeName = AndroidTypeNames.CURSOR,
                                rawQueryParamName = null
                            )
                        )
                    }
                    .build()
            addStatement("return %L", pagingSourceSpec)
        }
    }

    override fun convertAndReturn(
        sqlQueryVar: String,
        dbProperty: XPropertySpec,
        bindStatement: (CodeGenScope.(String) -> Unit)?,
        returnTypeName: XTypeName,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        check(isBasePagingSource) {
            "This version of `convertAndReturn` should only be called when the binder is for the " +
                "base PagingSource. "
        }
        val rawQueryVarName = scope.getTmpVar("_rawQuery")
        val stmtVarName = scope.getTmpVar("_stmt")

        when (scope.language) {
            CodeLanguage.JAVA -> {
                val assignExpr =
                    if (bindStatement != null) {
                        XCodeBlock.ofNewInstance(
                            language = scope.language,
                            typeName = RAW_QUERY,
                            "%L, %L",
                            sqlQueryVar,
                            Function1TypeSpec(
                                language = scope.language,
                                parameterTypeName = SQLiteDriverTypeNames.STATEMENT,
                                parameterName = stmtVarName,
                                returnTypeName = KotlinTypeNames.UNIT
                            ) {
                                val functionScope = scope.fork()
                                functionScope.builder
                                    .apply { bindStatement.invoke(functionScope, stmtVarName) }
                                    .build()
                                addCode(functionScope.generate())
                                addStatement("return %T.INSTANCE", KotlinTypeNames.UNIT)
                            }
                        )
                    } else {
                        XCodeBlock.ofNewInstance(
                            language = scope.language,
                            typeName = RAW_QUERY,
                            "%L",
                            sqlQueryVar
                        )
                    }
                scope.builder.addLocalVariable(
                    name = rawQueryVarName,
                    typeName = RAW_QUERY,
                    assignExpr = assignExpr
                )
            }
            CodeLanguage.KOTLIN ->
                scope.builder.apply {
                    if (bindStatement != null) {
                        beginControlFlow(
                            "val %L: %T = %T(%N) { %L ->",
                            rawQueryVarName,
                            RAW_QUERY,
                            RAW_QUERY,
                            sqlQueryVar,
                            stmtVarName
                        )
                        bindStatement.invoke(scope, stmtVarName)
                        endControlFlow()
                    } else {
                        addLocalVariable(
                            name = rawQueryVarName,
                            typeName = RAW_QUERY,
                            assignExpr =
                                XCodeBlock.ofNewInstance(
                                    language = scope.language,
                                    typeName = RAW_QUERY,
                                    argsFormat = "%N",
                                    sqlQueryVar
                                )
                        )
                    }
                }
        }

        scope.builder.apply {
            val tableNamesList = tableNames.joinToString(", ") { "\"$it\"" }
            val statementParamName = "statement"
            val pagingSourceSpec =
                XTypeSpec.anonymousClassBuilder(
                        language = language,
                        argsFormat = "%L, %N, %L",
                        rawQueryVarName,
                        dbProperty,
                        tableNamesList
                    )
                    .apply {
                        superclass(pagingSourceTypeName)
                        addFunction(
                            createConvertRowsMethod(
                                scope = scope,
                                stmtParamName = statementParamName,
                                stmtParamTypeName = SQLiteDriverTypeNames.STATEMENT,
                                rawQueryParamName = rawQueryVarName
                            )
                        )
                    }
                    .build()
            addStatement("return %L", pagingSourceSpec)
        }
    }

    private fun createConvertRowsMethod(
        scope: CodeGenScope,
        stmtParamName: String,
        stmtParamTypeName: XTypeName,
        rawQueryParamName: String?
    ): XFunSpec {
        return XFunSpec.builder(
                language = scope.language,
                name = "convertRows",
                visibility = VisibilityModifier.PROTECTED,
                isOverride = true
            )
            .apply {
                returns(CommonTypeNames.LIST.parametrizedBy(itemTypeName))
                addParameter(typeName = stmtParamTypeName, name = stmtParamName)
                if (stmtParamTypeName == SQLiteDriverTypeNames.STATEMENT) {
                    // The SQLiteStatement version requires a second parameter for backwards
                    // compatibility for delegating to CursorSQLiteStatement.
                    addParameter(typeName = XTypeName.PRIMITIVE_INT, name = "itemCount")
                }
                val resultVar = scope.getTmpVar("_result")
                val rowsScope = scope.fork()
                if (stmtParamTypeName == SQLiteDriverTypeNames.STATEMENT) {
                    checkNotNull(rawQueryParamName)
                    addStatement(
                        "%L.getBindingFunction().invoke(%L)",
                        rawQueryParamName,
                        stmtParamName,
                    )
                }
                listAdapter?.convert(resultVar, stmtParamName, rowsScope)
                addCode(rowsScope.generate())
                addStatement("return %L", resultVar)
            }
            .build()
    }
}
