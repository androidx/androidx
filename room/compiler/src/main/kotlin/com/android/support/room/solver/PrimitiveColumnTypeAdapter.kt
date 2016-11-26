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

package com.android.support.room.solver

import com.android.support.room.ext.L
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeKind

/**
 * Adapters for all primitives that has direct cursor mappings.
 */
open class PrimitiveColumnTypeAdapter(out: PrimitiveType,
                                      val cursorGetter: String,
                                      val stmtSetter: String,
                                      cast: Boolean = false) : ColumnTypeAdapter(out) {
    val cast = if (cast) "(${out.kind.name.toLowerCase()}) " else ""

    companion object {
        fun createPrimitiveAdapters(processingEnvironment: ProcessingEnvironment)
                : List<ColumnTypeAdapter> {
            return listOf(PrimitiveColumnTypeAdapter(
                    out = processingEnvironment.typeUtils.getPrimitiveType(TypeKind.INT),
                    cursorGetter = "getInt",
                    stmtSetter = "bindLong"),
                    PrimitiveColumnTypeAdapter(
                            out = processingEnvironment.typeUtils.getPrimitiveType(TypeKind.SHORT),
                            cursorGetter = "getShort",
                            stmtSetter = "bindLong"),
                    PrimitiveColumnTypeAdapter(
                            out = processingEnvironment.typeUtils.getPrimitiveType(TypeKind.BYTE),
                            cursorGetter = "getShort",
                            stmtSetter = "bindLong",
                            cast = true),
                    PrimitiveColumnTypeAdapter(
                            out = processingEnvironment.typeUtils.getPrimitiveType(TypeKind.LONG),
                            cursorGetter = "getLong",
                            stmtSetter = "bindLong"),
                    PrimitiveColumnTypeAdapter(
                            out = processingEnvironment.typeUtils.getPrimitiveType(TypeKind.CHAR),
                            cursorGetter = "getInt",
                            stmtSetter = "bindLong",
                            cast = true),
                    PrimitiveColumnTypeAdapter(
                            out = processingEnvironment.typeUtils.getPrimitiveType(TypeKind.FLOAT),
                            cursorGetter = "getFloat",
                            stmtSetter = "bindDouble"),
                    PrimitiveColumnTypeAdapter(
                            out = processingEnvironment.typeUtils.getPrimitiveType(TypeKind.DOUBLE),
                            cursorGetter = "getDouble",
                            stmtSetter = "bindDouble"))
        }
    }

    override fun bindToStmt(stmtName: String, index: Int, valueVarName: String,
                            scope: CodeGenScope) {
        scope.builder()
                .addStatement("$L.$L($L, $L)", stmtName, stmtSetter, index, valueVarName)
    }

    override fun readFromCursor(outVarName: String, cursorVarName: String, index: Int,
                                scope: CodeGenScope) {
        scope.builder()
                .addStatement("$L = $L$L.$L($L)", outVarName, cast, cursorVarName,
                        cursorGetter, index)
    }
}
