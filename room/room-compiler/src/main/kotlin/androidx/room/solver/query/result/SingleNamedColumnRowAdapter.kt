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
import androidx.room.compiler.codegen.XMemberName.Companion.packageMember
import androidx.room.compiler.codegen.XTypeName
import androidx.room.ext.RoomMemberNames
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.capitalize
import androidx.room.ext.stripNonJava
import androidx.room.solver.CodeGenScope
import androidx.room.solver.types.CursorValueReader
import androidx.room.vo.ColumnIndexVar
import java.util.Locale

/** Wraps a row adapter for a single item from a known column result. */
class SingleNamedColumnRowAdapter(
    val reader: CursorValueReader,
    val columnName: String,
) : QueryMappedRowAdapter(reader.typeMirror()) {
    override val mapping = SingleNamedColumnRowMapping(columnName)

    override fun isMigratedToDriver(): Boolean = true

    private val indexAdapter =
        object : IndexAdapter {

            private val indexVarNamePrefix =
                "_columnIndexOf${columnName.stripNonJava().capitalize(Locale.US)}"

            private lateinit var indexVarName: String

            override fun onCursorReady(cursorVarName: String, scope: CodeGenScope) {
                indexVarName = scope.getTmpVar(indexVarNamePrefix)
                scope.builder.addLocalVariable(
                    name = indexVarName,
                    typeName = XTypeName.PRIMITIVE_INT,
                    assignExpr =
                        XCodeBlock.of(
                            scope.language,
                            "%M(%L, %S)",
                            if (scope.useDriverApi) {
                                RoomTypeNames.STATEMENT_UTIL.packageMember("getColumnIndexOrThrow")
                            } else {
                                RoomMemberNames.CURSOR_UTIL_GET_COLUMN_INDEX_OR_THROW
                            },
                            cursorVarName,
                            columnName
                        )
                )
            }

            override fun getIndexVars() = listOf(ColumnIndexVar(columnName, indexVarName))
        }

    private lateinit var columnIndexVar: ColumnIndexVar

    override fun onCursorReady(
        cursorVarName: String,
        scope: CodeGenScope,
        indices: List<ColumnIndexVar>
    ) {
        columnIndexVar =
            indices.singleOrNull()
                ?: error("Expected a single resolved index var but got ${indices.size}")
    }

    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        reader.readFromCursor(outVarName, cursorVarName, columnIndexVar.indexVar, scope)
    }

    override fun getDefaultIndexAdapter() = indexAdapter

    data class SingleNamedColumnRowMapping(val usedColumn: String) : Mapping() {
        override val usedColumns = listOf(usedColumn)
    }
}
