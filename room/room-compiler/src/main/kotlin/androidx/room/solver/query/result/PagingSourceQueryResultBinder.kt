/*
 * Copyright (C) 2017 The Android Open Source Project
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

import androidx.room.ext.AndroidTypeNames
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomTypeNames
import androidx.room.solver.CodeGenScope
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Modifier

/**
 * This Binder uses room/room-paging artifact and binds queries directly to native Paging3
 * PagingSource through `LimitOffsetPagingSource`. Used solely by Paging3.
 */
class PagingSourceQueryResultBinder(
    private val listAdapter: ListQueryResultAdapter?,
    private val tableNames: Set<String>,
) : QueryResultBinder(listAdapter) {
    private val itemTypeName: TypeName = listAdapter?.rowAdapter?.out?.typeName ?: TypeName.OBJECT
    private val limitOffsetPagingSourceTypeNam: ParameterizedTypeName = ParameterizedTypeName.get(
        RoomTypeNames.LIMIT_OFFSET_PAGING_SOURCE, itemTypeName
    )

    override fun convertAndReturn(
        roomSQLiteQueryVar: String,
        canReleaseQuery: Boolean,
        dbField: FieldSpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        scope.builder().apply {
            val tableNamesList = tableNames.joinToString(", ") { "\"$it\"" }
            val limitOffsetPagingSourceSpec = TypeSpec.anonymousClassBuilder(
                "$L, $N, $L",
                roomSQLiteQueryVar,
                dbField,
                tableNamesList
            ).apply {
                addSuperinterface(limitOffsetPagingSourceTypeNam)
                addMethod(createConvertRowsMethod(scope))
            }.build()
            addStatement("return $L", limitOffsetPagingSourceSpec)
        }
    }

    private fun createConvertRowsMethod(scope: CodeGenScope): MethodSpec {
        return MethodSpec.methodBuilder("convertRows").apply {
            addAnnotation(Override::class.java)
            addModifiers(Modifier.PROTECTED)
            returns(ParameterizedTypeName.get(CommonTypeNames.LIST, itemTypeName))
            val cursorParam = ParameterSpec.builder(AndroidTypeNames.CURSOR, "cursor")
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

/**
 * A compatibility Paging3 binder that uses `LimitOffsetDataSource` along with
 * `DataSource.Factory.asPagingSourceFactory()` to bind queries to Paging3 PagingSource.
 */
class CompatPagingSourceQueryResultBinder(
    positionalDataSourceQueryResultBinder: PositionalDataSourceQueryResultBinder
) : PagingQueryResultBinder(positionalDataSourceQueryResultBinder) {
    override fun returnStatementTemplate() = "return $L.asPagingSourceFactory().invoke()"
}
