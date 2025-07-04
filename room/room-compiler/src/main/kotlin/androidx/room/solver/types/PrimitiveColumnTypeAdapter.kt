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

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeName.Companion.PRIMITIVE_BYTE
import androidx.room.compiler.codegen.XTypeName.Companion.PRIMITIVE_CHAR
import androidx.room.compiler.codegen.XTypeName.Companion.PRIMITIVE_DOUBLE
import androidx.room.compiler.codegen.XTypeName.Companion.PRIMITIVE_FLOAT
import androidx.room.compiler.codegen.XTypeName.Companion.PRIMITIVE_INT
import androidx.room.compiler.codegen.XTypeName.Companion.PRIMITIVE_LONG
import androidx.room.compiler.codegen.XTypeName.Companion.PRIMITIVE_SHORT
import androidx.room.compiler.codegen.buildCodeBlock
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XType
import androidx.room.parser.SQLTypeAffinity
import androidx.room.solver.CodeGenScope

/** Adapters for all primitives that has direct cursor mappings. */
class PrimitiveColumnTypeAdapter(
    out: XType,
    typeAffinity: SQLTypeAffinity,
    val primitive: Primitive,
) : ColumnTypeAdapter(out, typeAffinity) {

    companion object {

        enum class Primitive(
            val typeName: XTypeName,
            val cursorGetter: String,
            val stmtGetter: String,
            val stmtSetter: String,
        ) {
            INT(PRIMITIVE_INT, "getInt", "getLong", "bindLong"),
            SHORT(PRIMITIVE_SHORT, "getShort", "getLong", "bindLong"),
            BYTE(PRIMITIVE_BYTE, "getShort", "getLong", "bindLong"),
            LONG(PRIMITIVE_LONG, "getLong", "getLong", "bindLong"),
            CHAR(PRIMITIVE_CHAR, "getInt", "getLong", "bindLong"),
            FLOAT(PRIMITIVE_FLOAT, "getFloat", "getDouble", "bindDouble"),
            DOUBLE(PRIMITIVE_DOUBLE, "getDouble", "getDouble", "bindDouble"),
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
            return Primitive.values().map {
                PrimitiveColumnTypeAdapter(
                    out = processingEnvironment.requireType(it.typeName),
                    typeAffinity = getAffinity(it),
                    primitive = it,
                )
            }
        }
    }

    private val cursorGetter = primitive.cursorGetter
    private val stmtGetter = primitive.stmtGetter
    private val stmtSetter = primitive.stmtSetter

    override fun bindToStmt(
        stmtName: String,
        indexVarName: String,
        valueVarName: String,
        scope: CodeGenScope,
    ) {
        // These primitives don't have an exact statement setter.
        val castFunction =
            when (primitive) {
                Primitive.INT,
                Primitive.SHORT,
                Primitive.BYTE,
                Primitive.CHAR -> "toLong"
                Primitive.FLOAT -> "toDouble"
                else -> null
            }
        val valueExpr = buildCodeBlock { language ->
            when (language) {
                // For Java, with the language's primitive type casting, value variable can be
                // used as bind argument directly.
                CodeLanguage.JAVA -> add("%L", valueVarName)
                // For Kotlin, a converter function is emitted when a cast is needed.
                CodeLanguage.KOTLIN -> {
                    if (castFunction != null) {
                        add("%L.%L()", valueVarName, castFunction)
                    } else {
                        add("%L", valueVarName)
                    }
                }
            }
        }
        scope.builder.addStatement("%L.%L(%L, %L)", stmtName, stmtSetter, indexVarName, valueExpr)
    }

    override fun readFromStatement(
        outVarName: String,
        stmtVarName: String,
        indexVarName: String,
        scope: CodeGenScope,
    ) {
        scope.builder.addStatement(
            "%L = %L",
            outVarName,
            XCodeBlock.of("%L.%L(%L)", stmtVarName, stmtGetter, indexVarName).let {
                // These primitives don't have an exact cursor / statement getter.
                val castFunction =
                    when (primitive) {
                        Primitive.INT -> "toInt"
                        Primitive.SHORT -> "toShort"
                        Primitive.BYTE -> "toByte"
                        Primitive.CHAR -> "toChar"
                        Primitive.FLOAT -> "toFloat"
                        else -> null
                    } ?: return@let it
                buildCodeBlock { language ->
                    when (language) {
                        // For Java a cast will suffice
                        CodeLanguage.JAVA -> add(XCodeBlock.ofCast(out.asTypeName(), it))
                        // For Kotlin a converter function is emitted
                        CodeLanguage.KOTLIN -> add(XCodeBlock.of("%L.%L()", it, castFunction))
                    }
                }
            },
        )
    }
}
