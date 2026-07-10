/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.room3.compiler.codegen.XMemberName
import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.compiler.processing.XNullability
import androidx.room3.compiler.processing.XProcessingEnv
import androidx.room3.compiler.processing.XType
import androidx.room3.ext.KotlinTypeNames
import androidx.room3.ext.KotlinUnsignedMemberNames
import androidx.room3.parser.SQLTypeAffinity
import androidx.room3.solver.CodeGenScope

/**
 * Adapter for unsigned integers types
 *
 * https://kotlinlang.org/docs/unsigned-integer-types.html
 */
class UnsignedIntegerColumnTypeAdapter(out: XType, val unsignedType: UnsignedType) :
    ColumnTypeAdapter(out, SQLTypeAffinity.INTEGER) {

    companion object {

        enum class UnsignedType(val typeName: XTypeName, val toMemberFunction: XMemberName) {
            UBYTE(KotlinTypeNames.U_BYTE, KotlinUnsignedMemberNames.TO_BYTE),
            USHORT(KotlinTypeNames.U_SHORT, KotlinUnsignedMemberNames.TO_SHORT),
            UINT(KotlinTypeNames.U_INT, KotlinUnsignedMemberNames.TO_INT),
            ULONG(KotlinTypeNames.U_LONG, KotlinUnsignedMemberNames.TO_LONG),
        }

        fun createUnsignedAdapters(env: XProcessingEnv): List<UnsignedIntegerColumnTypeAdapter> {
            return UnsignedType.entries.flatMap {
                val typeMirror = env.requireType(it.typeName)
                if (env.backend == XProcessingEnv.Backend.KSP) {
                    listOf(
                        UnsignedIntegerColumnTypeAdapter(typeMirror, it),
                        UnsignedIntegerColumnTypeAdapter(typeMirror.makeNullable(), it),
                    )
                } else {
                    listOf(UnsignedIntegerColumnTypeAdapter(typeMirror, it))
                }
            }
        }
    }

    override fun bindToStmt(
        stmtName: String,
        indexVarName: String,
        valueVarName: String,
        scope: CodeGenScope,
    ) {
        if (out.nullability == XNullability.NONNULL) {
            scope.builder.bindToStmt(stmtName, indexVarName, valueVarName)
        } else {
            scope.builder.apply {
                beginControlFlow("if (%L == null)", valueVarName)
                    .addStatement("%L.bindNull(%L)", stmtName, indexVarName)
                nextControlFlow("else").bindToStmt(stmtName, indexVarName, valueVarName)
                endControlFlow()
            }
        }
    }

    private fun XCodeBlock.Builder.bindToStmt(
        stmtName: String,
        indexVarName: String,
        valueVarName: String,
    ) {
        addStatement("%L.bindLong(%L, %L.toLong())", stmtName, indexVarName, valueVarName)
    }

    override fun readFromStatement(
        outVarName: String,
        stmtVarName: String,
        indexVarName: String,
        scope: CodeGenScope,
    ) {
        if (out.nullability == XNullability.NONNULL) {
            scope.builder.readFromStatement(outVarName, stmtVarName, indexVarName)
        } else {
            scope.builder.apply {
                beginControlFlow("if (%L.isNull(%L))", stmtVarName, indexVarName)
                    .addStatement("%L = null", outVarName)
                nextControlFlow("else").readFromStatement(outVarName, stmtVarName, indexVarName)
                endControlFlow()
            }
        }
    }

    private fun XCodeBlock.Builder.readFromStatement(
        outVarName: String,
        stmtVarName: String,
        indexVarName: String,
    ) {
        addStatement(
            "%L = %L.getLong(%L).%M()",
            outVarName,
            stmtVarName,
            indexVarName,
            unsignedType.toMemberFunction,
        )
    }
}
