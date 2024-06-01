/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import androidx.room.ext.CommonTypeNames
import androidx.room.parser.SQLTypeAffinity
import androidx.room.solver.CodeGenScope

class ByteBufferColumnTypeAdapter constructor(out: XType) :
    ColumnTypeAdapter(out = out, typeAffinity = SQLTypeAffinity.BLOB) {
    override fun readFromCursor(
        outVarName: String,
        cursorVarName: String,
        indexVarName: String,
        scope: CodeGenScope
    ) {
        scope.builder.apply {
            fun XCodeBlock.Builder.addGetBlobStatement() {
                addStatement(
                    "%L = %T.wrap(%L.getBlob(%L))",
                    outVarName,
                    CommonTypeNames.BYTE_BUFFER,
                    cursorVarName,
                    indexVarName
                )
            }
            if (out.nullability == XNullability.NONNULL) {
                addGetBlobStatement()
            } else {
                beginControlFlow("if (%L.isNull(%L))", cursorVarName, indexVarName)
                    .addStatement("%L = null", outVarName)
                nextControlFlow("else").addGetBlobStatement()
                endControlFlow()
            }
        }
    }

    override fun bindToStmt(
        stmtName: String,
        indexVarName: String,
        valueVarName: String,
        scope: CodeGenScope
    ) {
        scope.builder.apply {
            fun XCodeBlock.Builder.addBindBlobStatement() {
                addStatement("%L.bindBlob(%L, %L.array())", stmtName, indexVarName, valueVarName)
            }
            if (out.nullability == XNullability.NONNULL) {
                addBindBlobStatement()
            } else {
                beginControlFlow("if (%L == null)", valueVarName)
                    .addStatement("%L.bindNull(%L)", stmtName, indexVarName)
                nextControlFlow("else").addBindBlobStatement()
                endControlFlow()
            }
        }
    }
}
