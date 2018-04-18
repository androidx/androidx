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

package androidx.room.solver.types

import androidx.room.ext.L
import androidx.room.ext.T
import androidx.room.solver.CodeGenScope
import javax.lang.model.type.TypeMirror

/**
 * A column adapter that uses a type converter to do the conversion. The type converter may be
 * a composite one.
 */
class CompositeAdapter(out: TypeMirror, val columnTypeAdapter: ColumnTypeAdapter,
                       val intoStatementConverter: TypeConverter?,
                       val fromCursorConverter: TypeConverter?)
    : ColumnTypeAdapter(out, columnTypeAdapter.typeAffinity) {
    override fun readFromCursor(outVarName: String, cursorVarName: String, indexVarName: String,
                                scope: CodeGenScope) {
        if (fromCursorConverter == null) {
            return
        }
        scope.builder().apply {
            val tmpCursorValue = scope.getTmpVar()
            addStatement("final $T $L", columnTypeAdapter.outTypeName, tmpCursorValue)
            columnTypeAdapter.readFromCursor(tmpCursorValue, cursorVarName, indexVarName, scope)
            fromCursorConverter.convert(tmpCursorValue, outVarName, scope)
        }
    }

    override fun bindToStmt(stmtName: String, indexVarName: String, valueVarName: String,
                            scope: CodeGenScope) {
        if (intoStatementConverter == null) {
            return
        }
        scope.builder().apply {
            val tmpVar = scope.getTmpVar()
            addStatement("final $T $L", columnTypeAdapter.out, tmpVar)
            intoStatementConverter.convert(valueVarName, tmpVar, scope)
            columnTypeAdapter.bindToStmt(stmtName, indexVarName, tmpVar, scope)
        }
    }
}
