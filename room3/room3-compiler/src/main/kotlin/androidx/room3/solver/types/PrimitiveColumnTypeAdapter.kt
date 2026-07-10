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

package androidx.room3.solver.types

import androidx.room3.compiler.codegen.XCodeBlock
import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.compiler.codegen.XTypeName.Companion.PRIMITIVE_BYTE
import androidx.room3.compiler.codegen.XTypeName.Companion.PRIMITIVE_CHAR
import androidx.room3.compiler.codegen.XTypeName.Companion.PRIMITIVE_DOUBLE
import androidx.room3.compiler.codegen.XTypeName.Companion.PRIMITIVE_FLOAT
import androidx.room3.compiler.codegen.XTypeName.Companion.PRIMITIVE_INT
import androidx.room3.compiler.codegen.XTypeName.Companion.PRIMITIVE_LONG
import androidx.room3.compiler.codegen.XTypeName.Companion.PRIMITIVE_SHORT
import androidx.room3.compiler.processing.XProcessingEnv
import androidx.room3.compiler.processing.XType
import androidx.room3.parser.SQLTypeAffinity
import androidx.room3.solver.CodeGenScope

/** Adapters for all primitives that has direct cursor mappings. */
class PrimitiveColumnTypeAdapter(
    out: XType,
    typeAffinity: SQLTypeAffinity,
    val primitive: Primitive,
) : ColumnTypeAdapter(out, typeAffinity) {

    companion object {

        enum class Primitive(
            val typeName: XTypeName,
            val stmtGetter: String,
            val stmtSetter: String,
        ) {
            INT(PRIMITIVE_INT, "getLong", "bindLong"),
            SHORT(PRIMITIVE_SHORT, "getLong", "bindLong"),
            BYTE(PRIMITIVE_BYTE, "getLong", "bindLong"),
            LONG(PRIMITIVE_LONG, "getLong", "bindLong"),
            CHAR(PRIMITIVE_CHAR, "getLong", "bindLong"),
            FLOAT(PRIMITIVE_FLOAT, "getDouble", "bindDouble"),
            DOUBLE(PRIMITIVE_DOUBLE, "getDouble", "bindDouble"),
        }

        private fun getAffinity(primitive: Primitive) =
            when (primitive) {
                Primitive.INT,
                Primitive.SHORT,
                Primitive.BYTE,
                Primitive.LONG,
                Primitive.CHAR -> SQLTypeAffinity.INTEGER
                Primitive.FLOAT,
                Primitive.DOUBLE -> SQLTypeAffinity.REAL
            }

        fun createPrimitiveAdapters(
            processingEnvironment: XProcessingEnv
        ): List<PrimitiveColumnTypeAdapter> {
            return Primitive.entries.map {
                PrimitiveColumnTypeAdapter(
                    out = processingEnvironment.requireType(it.typeName),
                    typeAffinity = getAffinity(it),
                    primitive = it,
                )
            }
        }
    }

    private val stmtGetter = primitive.stmtGetter
    private val stmtSetter = primitive.stmtSetter

    override fun bindToStmt(
        stmtName: String,
        indexVarName: String,
        valueVarName: String,
        scope: CodeGenScope,
    ) {
        // These primitives don't have an exact statement setter.
        val castFunctionCall =
            when (primitive) {
                Primitive.INT,
                Primitive.SHORT,
                Primitive.BYTE,
                Primitive.CHAR -> ".toLong()"
                Primitive.FLOAT -> ".toDouble()"
                else -> null
            }
        val valueExpr =
            if (castFunctionCall != null) {
                XCodeBlock.of("%L%L", valueVarName, castFunctionCall)
            } else {
                XCodeBlock.of("%L", valueVarName)
            }
        scope.builder.addStatement("%L.%L(%L, %L)", stmtName, stmtSetter, indexVarName, valueExpr)
    }

    override fun readFromStatement(
        outVarName: String,
        stmtVarName: String,
        indexVarName: String,
        scope: CodeGenScope,
    ) {
        // These primitives don't have an exact cursor / statement getter.
        val castFunctionCall =
            when (primitive) {
                Primitive.INT -> ".toInt()"
                Primitive.SHORT -> ".toShort()"
                Primitive.BYTE -> ".toByte()"
                Primitive.CHAR -> ".toInt().toChar()"
                Primitive.FLOAT -> ".toFloat()"
                else -> null
            }
        val valueExpr =
            XCodeBlock.of("%L.%L(%L)", stmtVarName, stmtGetter, indexVarName).let {
                if (castFunctionCall != null) {
                    XCodeBlock.of("%L%L", it, castFunctionCall)
                } else {
                    it
                }
            }
        scope.builder.addStatement("%L = %L", outVarName, valueExpr)
    }
}
