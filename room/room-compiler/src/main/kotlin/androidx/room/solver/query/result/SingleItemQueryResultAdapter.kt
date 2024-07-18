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

package androidx.room.solver.query.result

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.processing.XNullability
import androidx.room.solver.CodeGenScope

/**
 * Wraps a row adapter when there is only 1 item in the result
 */
class SingleItemQueryResultAdapter(
    private val rowAdapter: RowAdapter
) : QueryResultAdapter(listOf(rowAdapter)) {
    val type = rowAdapter.out
    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        scope.builder.apply {
            rowAdapter.onCursorReady(cursorVarName = cursorVarName, scope = scope)
            addLocalVariable(outVarName, type.asTypeName())
            beginControlFlow("if (%L.moveToFirst())", cursorVarName).apply {
                rowAdapter.convert(outVarName, cursorVarName, scope)
            }
            nextControlFlow("else").apply {
                val defaultValue = rowAdapter.out.defaultValue()
                if (
                    language == CodeLanguage.KOTLIN &&
                    type.nullability == XNullability.NONNULL &&
                    defaultValue == "null"
                ) {
                    addStatement(
                        "error(%S)", "The query result was empty, but expected a single row to " +
                            "return a NON-NULL object of " +
                            "type <${type.asTypeName().toString(language)}>."
                    )
                } else {
                    addStatement("%L = %L", outVarName, rowAdapter.out.defaultValue())
                }
            }
            endControlFlow()
        }
    }
}
