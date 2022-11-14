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

import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.toJavaPoet
import androidx.room.ext.AndroidTypeNames.CURSOR
import androidx.room.ext.CommonTypeNames.LIST
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.solver.CodeGenScope
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

/**
 * This Binder binds queries directly to native Paging3
 * PagingSource (i.e. [LimitOffsetPagingSource]) or its subclasses such as
 * [LimitOffsetListenableFuturePagingSource]. Used solely by Paging3.
 */
class MultiTypedPagingSourceQueryResultBinder(
    private val listAdapter: ListQueryResultAdapter?,
    private val tableNames: Set<String>,
    className: ClassName
) : QueryResultBinder(listAdapter) {

    private val itemTypeName: TypeName =
        listAdapter?.rowAdapters?.firstOrNull()?.out?.typeName ?: TypeName.OBJECT

    private val pagingSourceTypeName: ParameterizedTypeName = ParameterizedTypeName.get(
        className, itemTypeName
    )

    override fun convertAndReturn(
        roomSQLiteQueryVar: String,
        canReleaseQuery: Boolean,
        dbProperty: XPropertySpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val dbField = dbProperty.toJavaPoet()
        scope.builder().apply {
            val tableNamesList = tableNames.joinToString(", ") { "\"$it\"" }
            val pagingSourceSpec = TypeSpec.anonymousClassBuilder(
                "$L, $N, $L",
                roomSQLiteQueryVar,
                dbField,
                tableNamesList
            ).apply {
                addSuperinterface(pagingSourceTypeName)
                addMethod(createConvertRowsMethod(scope))
            }.build()
            addStatement("return $L", pagingSourceSpec)
        }
    }

    private fun createConvertRowsMethod(scope: CodeGenScope): MethodSpec {
        return MethodSpec.methodBuilder("convertRows").apply {
            addAnnotation(Override::class.java)
            addModifiers(Modifier.PROTECTED)
            returns(ParameterizedTypeName.get(LIST.toJavaPoet(), itemTypeName))
            val cursorParam = ParameterSpec.builder(CURSOR.toJavaPoet(), "cursor")
                .build()
            addParameter(cursorParam)
            val resultVar = scope.getTmpVar("_result")
            val rowsScope = scope.fork()
            listAdapter?.convert(resultVar, cursorParam.name, rowsScope)
            addCode(rowsScope.builder().build())
            addStatement("return $L", resultVar)
        }.build()
    }
}
