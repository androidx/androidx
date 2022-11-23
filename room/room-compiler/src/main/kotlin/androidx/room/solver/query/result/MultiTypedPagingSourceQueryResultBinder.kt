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
import androidx.room.compiler.codegen.XFunSpec.Builder.Companion.addStatement
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.ext.AndroidTypeNames.CURSOR
import androidx.room.ext.CommonTypeNames
import androidx.room.solver.CodeGenScope

/**
 * This Binder binds queries directly to native Paging3
 * PagingSource (i.e. [androidx.room.paging.LimitOffsetPagingSource]) or its subclasses such as
 * [androidx.room.paging.guava.LimitOffsetListenableFuturePagingSource]. Used solely by Paging3.
 */
class MultiTypedPagingSourceQueryResultBinder(
    private val listAdapter: ListQueryResultAdapter?,
    private val tableNames: Set<String>,
    className: XClassName
) : QueryResultBinder(listAdapter) {

    private val itemTypeName: XTypeName =
        listAdapter?.rowAdapters?.firstOrNull()?.out?.asTypeName() ?: XTypeName.ANY_OBJECT
    private val pagingSourceTypeName: XTypeName = className.parametrizedBy(itemTypeName)

    override fun convertAndReturn(
        roomSQLiteQueryVar: String,
        canReleaseQuery: Boolean,
        dbProperty: XPropertySpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        scope.builder.apply {
            val tableNamesList = tableNames.joinToString(", ") { "\"$it\"" }
            val pagingSourceSpec = XTypeSpec.anonymousClassBuilder(
                language = language,
                argsFormat = "%L, %N, %L",
                roomSQLiteQueryVar,
                dbProperty,
                tableNamesList
            ).apply {
                superclass(pagingSourceTypeName)
                addFunction(createConvertRowsMethod(scope))
            }.build()
            addStatement("return %L", pagingSourceSpec)
        }
    }

    private fun createConvertRowsMethod(scope: CodeGenScope): XFunSpec {
        return XFunSpec.builder(
            language = scope.language,
            name = "convertRows",
            visibility = VisibilityModifier.PROTECTED,
            isOverride = true
        ).apply {
            val cursorParamName = "cursor"
            returns(CommonTypeNames.LIST.parametrizedBy(itemTypeName))
            addParameter(
                typeName = CURSOR,
                name = cursorParamName
            )
            val resultVar = scope.getTmpVar("_result")
            val rowsScope = scope.fork()
            listAdapter?.convert(resultVar, cursorParamName, rowsScope)
            addCode(rowsScope.generate())
            addStatement("return %L", resultVar)
        }.build()
    }
}
