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

import androidx.room.ext.AndroidTypeNames
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.RoomTypeNames.CURSOR_UTIL
import androidx.room.ext.S
import androidx.room.ext.T
import androidx.room.ext.W
import androidx.room.solver.CodeGenScope
import androidx.room.vo.ColumnIndexVar
import androidx.room.vo.Entity
import androidx.room.vo.columnNames
import androidx.room.writer.EntityCursorConverterWriter
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName

class EntityRowAdapter(val entity: Entity) : QueryMappedRowAdapter(entity.type) {

    override val mapping = EntityMapping(entity)

    private lateinit var methodSpec: MethodSpec

    private var cursorDelegateVarName: String? = null

    private val indexAdapter = object : IndexAdapter {

        private var indexVars: List<ColumnIndexVar>? = null

        override fun onCursorReady(cursorVarName: String, scope: CodeGenScope) {
            indexVars = entity.columnNames.map {
                ColumnIndexVar(
                    column = it,
                    indexVar = CodeBlock.of(
                        "$T.getColumnIndex($N, $S)",
                        CURSOR_UTIL, cursorVarName, it
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
            val entityColumnNamesParam = CodeBlock.of(
                "new $T[] { $L }",
                CommonTypeNames.STRING,
                CodeBlock.join(entity.columnNames.map { CodeBlock.of(S, it) }, ",$W")
            )
            val entityColumnIndicesParam = CodeBlock.of(
                "new $T[] { $L }",
                TypeName.INT,
                CodeBlock.join(indices.map { CodeBlock.of(L, it.indexVar) }, ",$W")
            )
            scope.builder().addStatement(
                "final $T $N = $T.wrapMappedColumns($N, $L, $L)",
                AndroidTypeNames.CURSOR,
                cursorDelegateVarName,
                RoomTypeNames.CURSOR_UTIL,
                cursorVarName,
                entityColumnNamesParam,
                entityColumnIndicesParam
            )
        }
        methodSpec = scope.writer.getOrCreateMethod(EntityCursorConverterWriter(entity))
    }

    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        scope.builder().addStatement(
            "$L = $N($L)",
            outVarName, methodSpec, cursorDelegateVarName ?: cursorVarName
        )
    }

    override fun getDefaultIndexAdapter() = indexAdapter

    data class EntityMapping(val entity: Entity) : Mapping() {
        override val usedColumns: List<String> = entity.columnNames
    }
}
