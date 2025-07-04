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

import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.SQLiteDriverTypeNames
import androidx.room.solver.CodeGenScope

/**
 * This Binder binds queries directly to Non-KMP compatible Paging3 PagingSource Binders. Used
 * solely by Non-KMP Paging3.
 */
class MultiTypedPagingSourceQueryResultBinder(
    private val listAdapter: ListQueryResultAdapter?,
    private val tableNames: Set<String>,
    className: XClassName,
) : QueryResultBinder(listAdapter) {

    private val itemTypeName: XTypeName =
        listAdapter?.rowAdapters?.firstOrNull()?.out?.asTypeName() ?: XTypeName.ANY_OBJECT
    private val pagingSourceTypeName: XTypeName = className.parametrizedBy(itemTypeName)

    override val usesCompatQueryWriter = true

    override fun convertAndReturn(
        sqlQueryVar: String,
        dbProperty: XPropertySpec,
        bindStatement: (CodeGenScope.(String) -> Unit)?,
        returnTypeName: XTypeName,
        inTransaction: Boolean,
        scope: CodeGenScope,
    ) {
        scope.builder.apply {
            val tableNamesList = tableNames.joinToString(", ") { "\"$it\"" }
            val pagingSourceSpec =
                XTypeSpec.anonymousClassBuilder(
                        argsFormat = "%L, %N, %L",
                        sqlQueryVar,
                        dbProperty,
                        tableNamesList,
                    )
                    .apply {
                        superclass(pagingSourceTypeName)
                        addFunction(
                            XFunSpec.builder(
                                    name = "convertRows",
                                    visibility = VisibilityModifier.PROTECTED,
                                    isOverride = true,
                                )
                                .apply {
                                    val rowsScope = scope.fork()
                                    val cursorParamName = "statement"
                                    val resultVar = scope.getTmpVar("_result")
                                    returns(CommonTypeNames.LIST.parametrizedBy(itemTypeName))
                                    addParameter(
                                        typeName = SQLiteDriverTypeNames.STATEMENT,
                                        name = cursorParamName,
                                    )
                                    listAdapter?.convert(resultVar, cursorParamName, rowsScope)
                                    addCode(rowsScope.generate())
                                    addStatement("return %L", resultVar)
                                }
                                .build()
                        )
                    }
                    .build()
            addStatement("return %L", pagingSourceSpec)
        }
    }
}
