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
import androidx.room.ext.S
import androidx.room.ext.T
import androidx.room.solver.CodeGenScope
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

/**
 * Used by Paging2 pipeline
 */
class PositionalDataSourceQueryResultBinder(
    val listAdapter: ListQueryResultAdapter?,
    val tableNames: Set<String>,
) : QueryResultBinder(listAdapter) {
    val itemTypeName: TypeName =
        listAdapter?.rowAdapters?.firstOrNull()?.out?.typeName ?: TypeName.OBJECT
    val typeName: ParameterizedTypeName = ParameterizedTypeName.get(
        RoomTypeNames.LIMIT_OFFSET_DATA_SOURCE, itemTypeName
    )

    override fun convertAndReturn(
        roomSQLiteQueryVar: String,
        sectionsVar: String?,
        tempTableVar: String,
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
            dbField, roomSQLiteQueryVar, inTransaction, true, tableNamesList
        ).apply {
            superclass(typeName)
            addMethod(createConvertRowsMethod(scope))
        }.build()
        val cursorVar = scope.getTmpVar("_cursor")
        scope.builder().apply {
            if (sectionsVar != null) {
                addStatement("$T $L = null", RoomTypeNames.ROOM_SQL_QUERY, roomSQLiteQueryVar)
            }
            addStatement("$T $L = null", AndroidTypeNames.CURSOR, cursorVar)

            if (sectionsVar != null) {
                val pairVar = scope.getTmpVar("_resultPair")
                addStatement(
                    "final $T $L = $T.prepareQuery($N, $L, $S, $L, true)",
                    ParameterizedTypeName.get(
                        ClassName.get(Pair::class.java),
                        RoomTypeNames.ROOM_SQL_QUERY,
                        TypeName.BOOLEAN.box()
                    ),
                    pairVar,
                    RoomTypeNames.QUERY_UTIL, dbField, inTransaction, tempTableVar, sectionsVar
                )
                addStatement("$L = $L.getFirst()", roomSQLiteQueryVar, pairVar)
            }

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
