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
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.S
import androidx.room.ext.T
import androidx.room.solver.CodeGenScope
import androidx.room.solver.types.CursorValueReader
import com.squareup.javapoet.TypeName

/**
 * Wraps a row adapter for a single item from a known column result.
 */
class SingleNamedColumnRowAdapter(
    val reader: CursorValueReader,
    val columnName: String,
) : RowAdapter(reader.typeMirror()), QueryMappedRowAdapter {
    override val mapping = SingleNamedColumnRowMapping(columnName)

    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        val indexVarName = scope.getTmpVar(
            "_columnIndexOf$columnName"
        )
        val indexMethod = "getColumnIndexOrThrow"
        scope.builder().addStatement(
            "final $T $L = $T.$L($L, $S)",
            TypeName.INT,
            indexVarName,
            RoomTypeNames.CURSOR_UTIL,
            indexMethod,
            cursorVarName,
            columnName
        )
        reader.readFromCursor(outVarName, cursorVarName, indexVarName, scope)
    }

    data class SingleNamedColumnRowMapping(
        val usedColumn: String
    ) : QueryMappedRowAdapter.Mapping() {
        override val usedColumns = listOf(usedColumn)
    }
}
