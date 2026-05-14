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

package androidx.room3.solver.query.parameter

import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.solver.CodeGenScope

/**
 * Adapter for binding a LongSparseArray keys into query arguments. This special adapter is only
 * used for binding the relationship query whose keys have INTEGER affinity.
 */
class LongSparseArrayKeyQueryParameterAdapter : QueryParameterAdapter(true) {
    override fun bindToStmt(
        inputVarName: String,
        stmtVarName: String,
        startIndexVarName: String,
        scope: CodeGenScope,
    ) {
        val itrIndexVar = "i"
        val itrItemVar = scope.getTmpVar("_item")
        scope.builder
            .beginControlFlow("for (%L in 0 until %L.size())", itrIndexVar, inputVarName)
            .addLocalVal(
                itrItemVar,
                XTypeName.PRIMITIVE_LONG,
                "%L.keyAt(%L)",
                inputVarName,
                itrIndexVar,
            )
            .addStatement("%L.bindLong(%L, %L)", stmtVarName, startIndexVarName, itrItemVar)
            .addStatement("%L++", startIndexVarName)
            .endControlFlow()
    }

    override fun getArgCount(inputVarName: String, outputVarName: String, scope: CodeGenScope) {
        scope.builder.addLocalVal(outputVarName, XTypeName.PRIMITIVE_INT, "%L.size()", inputVarName)
    }
}
