/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.room.compiler.processing.XType
import androidx.room.ext.L
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.T
import androidx.room.parser.SQLTypeAffinity
import androidx.room.solver.CodeGenScope
import com.squareup.javapoet.TypeName

class UuidColumnTypeAdapter(
    out: XType,
) : ColumnTypeAdapter(
    out = out,
    typeAffinity = SQLTypeAffinity.BLOB
) {
    override fun bindToStmt(
        stmtName: String,
        indexVarName: String,
        valueVarName: String,
        scope: CodeGenScope
    ) {
        scope.builder().apply {
            val conversionMethodName = "convertUUIDToByte"
            beginControlFlow("if ($L == null)", valueVarName)
                .addStatement("$L.bindNull($L)", stmtName, indexVarName)
            nextControlFlow("else")
                .addStatement(
                    "$L.bindBlob($L, $T.$L($L))",
                    stmtName,
                    indexVarName,
                    RoomTypeNames.UUID_UTIL,
                    conversionMethodName,
                    valueVarName,
                )
            endControlFlow()
        }
    }

    override fun readFromCursor(
        outVarName: String,
        cursorVarName: String,
        indexVarName: String,
        scope: CodeGenScope
    ) {
        scope.builder().apply {
            val conversionMethodName = "convertByteToUUID"
            beginControlFlow("if ($L.isNull($L))", cursorVarName, indexVarName)
                .addStatement("$L = null", outVarName)
            nextControlFlow("else")
                .addStatement(
                    "$L = $T.$L($L.getBlob($L))",
                    outVarName,
                    RoomTypeNames.UUID_UTIL,
                    conversionMethodName,
                    cursorVarName,
                    indexVarName
                )
            endControlFlow()
        }
    }

    override fun convert(inputVarName: String, scope: CodeGenScope): String? {
        val conversionMethodName = "convertUUIDToByte"
        val outVarName = scope.getTmpVar()
        scope.builder().apply {
            addStatement(
                "final $T $L = $T.$L($L)",
                TypeName.get(ByteArray::class.java),
                outVarName,
                RoomTypeNames.UUID_UTIL,
                conversionMethodName,
                inputVarName
            )
        }
        return outVarName
    }

    override fun convertedType(): TypeName? {
        return TypeName.get(ByteArray::class.java)
    }
}