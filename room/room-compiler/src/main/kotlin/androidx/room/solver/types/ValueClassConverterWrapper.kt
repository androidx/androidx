/*
 * Copyright (C) 2017 The Android Open Source Project
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
import androidx.room.parser.SQLTypeAffinity
import androidx.room.solver.CodeGenScope

/**
 * ColumnTypeAdapter for Kotlin value classes that simply wraps and forwards calls to a found
 * adapter for the underlying type.
 */
class ValueClassConverterWrapper(
    val valueTypeColumnAdapter: ColumnTypeAdapter,
    val affinity: SQLTypeAffinity,
    out: XType,
    val valuePropertyName: String
) : ColumnTypeAdapter(out, affinity) {
    override fun readFromCursor(
        outVarName: String,
        cursorVarName: String,
        indexVarName: String,
        scope: CodeGenScope
    ) {
        scope.builder.apply {
            fun XCodeBlock.Builder.addTypeToValueClassStatement() {
                val propertyValueVarName = scope.getTmpVar("_$valuePropertyName")
                addLocalVariable(propertyValueVarName, valueTypeColumnAdapter.outTypeName)
                valueTypeColumnAdapter.readFromCursor(
                    propertyValueVarName,
                    cursorVarName,
                    indexVarName,
                    scope
                )
                addStatement(
                    format = "%L = %L",
                    outVarName,
                    XCodeBlock.ofNewInstance(
                        language,
                        out.asTypeName(),
                        "%N",
                        propertyValueVarName
                    )
                )
            }
            if (out.nullability == XNullability.NONNULL) {
                addTypeToValueClassStatement()
            } else {
                beginControlFlow("if (%L.isNull(%L))", cursorVarName, indexVarName)
                    .addStatement("%L = null", outVarName)
                nextControlFlow("else")
                    .addTypeToValueClassStatement()
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
            val propertyName = scope.getTmpVar("_$valuePropertyName")
            val assignmentBlock = if (out.nullability == XNullability.NONNULL) {
                XCodeBlock.of(
                    scope.language,
                    "checkNotNull(%L.%L) { %S }",
                    valueVarName,
                    valuePropertyName,
                    "Cannot bind NULLABLE value '$valuePropertyName' of inline " +
                        "class '$out' to a NOT NULL column."
                )
            } else {
                XCodeBlock.of(
                    scope.language,
                    "%L?.%L",
                    valueVarName,
                    valuePropertyName
                )
            }
            addLocalVariable(
                name = propertyName,
                typeName = valueTypeColumnAdapter.outTypeName
                    .copy(nullable = out.nullability != XNullability.NONNULL),
                assignExpr = assignmentBlock
            )

            if (out.nullability == XNullability.NONNULL) {
                valueTypeColumnAdapter.bindToStmt(
                    stmtName,
                    indexVarName,
                    propertyName,
                    scope
                )
            } else {
                beginControlFlow(
                    "if (%L == null)",
                    propertyName
                ).addStatement("%L.bindNull(%L)", stmtName, indexVarName)
                nextControlFlow("else")
                valueTypeColumnAdapter.bindToStmt(
                    stmtName,
                    indexVarName,
                    propertyName,
                    scope
                )
                endControlFlow()
            }
        }
    }
}
