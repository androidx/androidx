/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.room3.solver.query.parameter

import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.solver.CodeGenScope
import androidx.room3.solver.types.StatementValueBinder

/**
 * Adapter for binding composite keys (Pair, Triple, List) and its declared types for relationships
 * with composite keys.
 */
class CompositeKeyQueryParameterAdapter(val keyValueBinders: List<StatementValueBinder>) :
    QueryParameterAdapter(isMultiple = true) {

    init {
        check(keyValueBinders.size > 1) { "Expected 2 or more value binders." }
    }

    override fun bindToStmt(
        inputVarName: String,
        stmtVarName: String,
        startIndexVarName: String,
        scope: CodeGenScope,
    ) {
        val keyTmpVarName = scope.getTmpVar("_compositeKey")
        scope.builder.beginControlFlow("for (%L in %L)", keyTmpVarName, inputVarName)
        keyValueBinders.forEachIndexed { index, binder ->
            val valueVarName = scope.getTmpVar("_keyValue_$index")
            val valueVarTypeName = binder.typeMirror().asTypeName()
            val assignExpr =
                when (keyValueBinders.size) {
                    2 -> if (index == 0) "%L.first" else "%L.second"
                    3 ->
                        when (index) {
                            0 -> "%L.first"
                            1 -> "%L.second"
                            else -> "%L.third"
                        }
                    else -> "(%L[$index] as %T)"
                }
            val assignExprArgs =
                if (keyValueBinders.size > 3) {
                    arrayOf(keyTmpVarName, valueVarTypeName)
                } else {
                    arrayOf(keyTmpVarName)
                }
            scope.builder.addLocalVal(valueVarName, valueVarTypeName, assignExpr, *assignExprArgs)
            binder.bindToStmt(stmtVarName, startIndexVarName, valueVarName, scope)
            scope.builder.addStatement("%L++", startIndexVarName)
        }
        scope.builder.endControlFlow()
    }

    override fun getArgCount(inputVarName: String, outputVarName: String, scope: CodeGenScope) {
        scope.builder.apply {
            addLocalVal(
                outputVarName,
                XTypeName.PRIMITIVE_INT,
                "%L.size * %L",
                inputVarName,
                keyValueBinders.size,
            )
        }
    }
}
