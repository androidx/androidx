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
import androidx.room.ext.typeName
import androidx.room.parser.SQLTypeAffinity
import androidx.room.parser.SQLTypeAffinity.REAL
import androidx.room.solver.CodeGenScope
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeKind.BYTE
import javax.lang.model.type.TypeKind.CHAR
import javax.lang.model.type.TypeKind.DOUBLE
import javax.lang.model.type.TypeKind.FLOAT
import javax.lang.model.type.TypeKind.INT
import javax.lang.model.type.TypeKind.LONG
import javax.lang.model.type.TypeKind.SHORT

/**
 * Adapters for all primitives that has direct cursor mappings.
 */
open class PrimitiveColumnTypeAdapter(out: PrimitiveType,
                                      val cursorGetter: String,
                                      val stmtSetter: String,
                                      typeAffinity: SQLTypeAffinity)
        : ColumnTypeAdapter(out, typeAffinity) {
    val cast = if (cursorGetter == "get${out.typeName().toString().capitalize()}")
                    ""
                else
                    "(${out.typeName()}) "

    companion object {
        fun createPrimitiveAdapters(
                processingEnvironment: ProcessingEnvironment
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
                        out = processingEnvironment.typeUtils.getPrimitiveType(it.first),
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

    override fun bindToStmt(stmtName: String, indexVarName: String, valueVarName: String,
                            scope: CodeGenScope) {
        scope.builder()
                .addStatement("$L.$L($L, $L)", stmtName, stmtSetter, indexVarName, valueVarName)
    }

    override fun readFromCursor(outVarName: String, cursorVarName: String, indexVarName: String,
                                scope: CodeGenScope) {
        scope.builder()
                .addStatement("$L = $L$L.$L($L)", outVarName, cast, cursorVarName,
                        cursorGetter, indexVarName)
    }
}
