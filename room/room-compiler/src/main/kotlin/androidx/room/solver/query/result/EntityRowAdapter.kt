/*
 * Copyright (C) 2016 The Android Open Source Project
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

import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.ext.AndroidTypeNames
import androidx.room.ext.ArrayLiteral
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.RoomMemberNames
import androidx.room.solver.CodeGenScope
import androidx.room.vo.ColumnIndexVar
import androidx.room.vo.Entity
import androidx.room.vo.columnNames
import androidx.room.writer.EntityCursorConverterWriter

class EntityRowAdapter(val entity: Entity) : QueryMappedRowAdapter(entity.type) {

    override val mapping = EntityMapping(entity)

    private lateinit var functionSpec: XFunSpec

    private var cursorDelegateVarName: String? = null

    private val indexAdapter = object : IndexAdapter {

        private var indexVars: List<ColumnIndexVar>? = null

        override fun onCursorReady(cursorVarName: String, scope: CodeGenScope) {
            indexVars = entity.columnNames.map { columnName ->
                ColumnIndexVar(
                    column = columnName,
                    indexVar = XCodeBlock.of(
                        scope.language,
                        "%M(%L, %S)",
                        RoomMemberNames.CURSOR_UTIL_GET_COLUMN_INDEX,
                        cursorVarName,
                        columnName
                    ).toString()
                )
            }
        }

        override fun getIndexVars() = indexVars ?: emptyList()
    }

    override fun onCursorReady(
        cursorVarName: String,
        scope: CodeGenScope,
        indices: List<ColumnIndexVar>
    ) {
        // Check if given indices are the default ones, i.e. onCursorReady() was called without
        // an indices argument and these are the default parameter ones, which means a wrapped
        // cursor is not needed since the generated entity cursor converter has access to the
        // original cursor.
        if (indices.isNotEmpty() && indices != indexAdapter.getIndexVars()) {
            // Due to entity converter code being shared and using Cursor.getColumnIndex() we can't
            // generate code that uses the mapping directly. Instead we create a wrapped Cursor that is
            // solely used in the shared converter method and whose getColumnIndex() is overridden
            // to return the resolved column index.
            cursorDelegateVarName = scope.getTmpVar("_wrappedCursor")
            val entityColumnNamesParam = ArrayLiteral(
                scope.language,
                CommonTypeNames.STRING,
                *entity.columnNames.toTypedArray()
            )
            val entityColumnIndicesParam = ArrayLiteral(
                scope.language,
                XTypeName.PRIMITIVE_INT,
                *indices.map { it.indexVar }.toTypedArray()
            )
            scope.builder.addLocalVariable(
                checkNotNull(cursorDelegateVarName),
                AndroidTypeNames.CURSOR,
                assignExpr = XCodeBlock.of(
                    scope.language,
                    "%M(%L, %L, %L)",
                    RoomMemberNames.CURSOR_UTIL_WRAP_MAPPED_COLUMNS,
                    cursorVarName,
                    entityColumnNamesParam,
                    entityColumnIndicesParam
                )
            )
        }
        functionSpec = scope.writer.getOrCreateFunction(EntityCursorConverterWriter(entity))
    }

    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        scope.builder.addStatement(
            "%L = %N(%L)",
            outVarName, functionSpec, cursorDelegateVarName ?: cursorVarName
        )
    }

    override fun getDefaultIndexAdapter() = indexAdapter

    data class EntityMapping(val entity: Entity) : Mapping() {
        override val usedColumns: List<String> = entity.columnNames
    }
}
