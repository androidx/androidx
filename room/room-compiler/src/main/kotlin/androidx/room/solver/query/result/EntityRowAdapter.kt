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

import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomTypeNames.CURSOR_UTIL
import androidx.room.ext.S
import androidx.room.ext.T
import androidx.room.solver.CodeGenScope
import androidx.room.vo.ColumnIndexVar
import androidx.room.vo.Entity
import androidx.room.vo.columnNames
import androidx.room.writer.EntityCursorConverterWriter
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec

class EntityRowAdapter(val entity: Entity) : QueryMappedRowAdapter(entity.type) {

    override val mapping = EntityMapping(entity)

    private lateinit var methodSpec: MethodSpec

    private val indexAdapter = object : IndexAdapter {

        private lateinit var indexVars: List<ColumnIndexVar>

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

        override fun getIndexVars() = indexVars
    }

    override fun onCursorReady(
        indices: List<ColumnIndexVar>,
        cursorVarName: String,
        scope: CodeGenScope
    ) {
        methodSpec = scope.writer.getOrCreateMethod(EntityCursorConverterWriter(entity))
    }

    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        scope.builder().addStatement("$L = $N($L)", outVarName, methodSpec, cursorVarName)
    }

    override fun getDefaultIndexAdapter() = indexAdapter

    data class EntityMapping(val entity: Entity) : Mapping() {
        override val usedColumns: List<String> = entity.columnNames
    }
}
