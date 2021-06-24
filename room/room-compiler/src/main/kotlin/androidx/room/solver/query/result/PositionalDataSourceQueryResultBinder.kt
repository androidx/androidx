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
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

/**
 * Used by both Paging2 and Paging3 pipelines.
 *
 * Paging 3 might create data source on the main thread and does not have any strict
 * invalidation rules (e.g. query does not need to return as many records as it initially
 * counted).
 * Paging 2 always creates data source on a background thread and does not let data source
 * return less values then it counted. To achieve that, LimitOffsetDataSource always
 * synchronized invalidation trackers to the database, which is not necessary for Paging3.
 *
 * As a result, we change behavior based on whether we create the data source for paging 3 or 2.
 * In practice, [forPaging3] parameter controls whether LimitOffsetDataSource registers its observer
 * immediately (paging2) or not (paging3).
 */
class PositionalDataSourceQueryResultBinder(
    val listAdapter: ListQueryResultAdapter?,
    val tableNames: Set<String>,
    val forPaging3: Boolean,
) : QueryResultBinder(listAdapter) {
    val itemTypeName: TypeName =
        listAdapter?.rowAdapters?.firstOrNull()?.out?.typeName ?: TypeName.OBJECT
    val typeName: ParameterizedTypeName = ParameterizedTypeName.get(
        RoomTypeNames.LIMIT_OFFSET_DATA_SOURCE, itemTypeName
    )

    override fun convertAndReturn(
        roomSQLiteQueryVar: String,
        canReleaseQuery: Boolean,
        dbField: FieldSpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        // first comma for table names comes from the string since it might be empty in which case
        // we don't need a comma. If list is empty, this prevents generating bad code (it is still
        // an error to have empty list but that is already reported while item is processed)
        val tableNamesList = tableNames.joinToString("") { ", \"$it\"" }
        val spec = TypeSpec.anonymousClassBuilder(
            "$N, $L, $L, $L $L",
            dbField, roomSQLiteQueryVar, inTransaction, !forPaging3, tableNamesList
        ).apply {
            superclass(typeName)
            addMethod(createConvertRowsMethod(scope))
        }.build()
        scope.builder().apply {
            addStatement("return $L", spec)
        }
    }

    private fun createConvertRowsMethod(scope: CodeGenScope): MethodSpec =
        MethodSpec.methodBuilder("convertRows").apply {
            addAnnotation(Override::class.java)
            addModifiers(Modifier.PROTECTED)
            returns(ParameterizedTypeName.get(CommonTypeNames.LIST, itemTypeName))
            val cursorParam = ParameterSpec.builder(AndroidTypeNames.CURSOR, "cursor")
                .build()
            addParameter(cursorParam)
            val resultVar = scope.getTmpVar("_res")
            val rowsScope = scope.fork()
            listAdapter?.convert(resultVar, cursorParam.name, rowsScope)
            addCode(rowsScope.builder().build())
            addStatement("return $L", resultVar)
        }.build()
}
