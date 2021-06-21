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
import androidx.room.parser.SQLTypeAffinity
import androidx.room.parser.SQLTypeAffinity.REAL
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XType
import androidx.room.solver.CodeGenScope
import capitalize
import com.squareup.javapoet.TypeName.BYTE
import com.squareup.javapoet.TypeName.CHAR
import com.squareup.javapoet.TypeName.DOUBLE
import com.squareup.javapoet.TypeName.FLOAT
import com.squareup.javapoet.TypeName.INT
import com.squareup.javapoet.TypeName.LONG
import com.squareup.javapoet.TypeName.SHORT
import java.util.Locale

/**
 * Adapters for all primitives that has direct cursor mappings.
 */
open class PrimitiveColumnTypeAdapter(
    out: XType,
    val cursorGetter: String,
    val stmtSetter: String,
    typeAffinity: SQLTypeAffinity
) : ColumnTypeAdapter(out, typeAffinity) {
    val cast = if (cursorGetter == "get${out.typeName.toString().capitalize(Locale.US)}")
        ""
    else
        "(${out.typeName}) "

    companion object {
        fun createPrimitiveAdapters(
            processingEnvironment: XProcessingEnv
        ): List<PrimitiveColumnTypeAdapter> {
            return listOf(
                Triple(INT, "getInt", "bindLong"),
                Triple(SHORT, "getShort", "bindLong"),
                Triple(BYTE, "getShort", "bindLong"),
                Triple(LONG, "getLong", "bindLong"),
                Triple(CHAR, "getInt", "bindLong"),
                Triple(FLOAT, "getFloat", "bindDouble"),
                Triple(DOUBLE, "getDouble", "bindDouble")
            ).map {
                PrimitiveColumnTypeAdapter(
                    out = processingEnvironment.requireType(it.first),
                    cursorGetter = it.second,
                    stmtSetter = it.third,
                    typeAffinity = when (it.first) {
                        INT, SHORT, BYTE, LONG, CHAR -> SQLTypeAffinity.INTEGER
                        FLOAT, DOUBLE -> REAL
                        else -> throw IllegalArgumentException("invalid type")
                    }
                )
            }
        }
    }

    override fun bindToStmt(
        stmtName: String,
        indexVarName: String,
        valueVarName: String,
        scope: CodeGenScope
    ) {
        scope.builder()
            .addStatement("$L.$L($L, $L)", stmtName, stmtSetter, indexVarName, valueVarName)
    }

    override fun readFromCursor(
        outVarName: String,
        cursorVarName: String,
        indexVarName: String,
        scope: CodeGenScope
    ) {
        scope.builder()
            .addStatement(
                "$L = $L$L.$L($L)", outVarName, cast, cursorVarName,
                cursorGetter, indexVarName
            )
    }
}
