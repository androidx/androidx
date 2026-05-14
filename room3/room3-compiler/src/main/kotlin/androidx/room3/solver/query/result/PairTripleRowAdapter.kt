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

package androidx.room3.solver.query.result

import androidx.room3.compiler.processing.XType
import androidx.room3.solver.CodeGenScope
import androidx.room3.solver.types.StatementValueReader
import androidx.room3.vo.ColumnIndexVar

/** A row adapter that reads two or three columns and constructs a Kotlin [Pair] or [Triple]. */
class PairTripleRowAdapter(out: XType, val readers: List<StatementValueReader>) : RowAdapter(out) {

    init {
        require(readers.size in 2..3) { "PairTripleRowAdapter only supports 2 or 3 readers." }
    }

    override fun convert(outVarName: String, stmtVarName: String, scope: CodeGenScope) {
        val valueVars =
            readers.mapIndexed { index, reader ->
                val valueVar = scope.getTmpVar("_value$index")
                scope.builder.addLocalVariable(valueVar, reader.typeMirror().asTypeName())
                reader.readFromStatement(valueVar, stmtVarName, index.toString(), scope)
                valueVar
            }

        val ctrArgs = valueVars.joinToString(separator = ", ") { "%L" }
        scope.builder.addStatement(
            "%L = %T($ctrArgs)",
            outVarName,
            out.rawType.asTypeName(),
            *valueVars.toTypedArray(),
        )
    }

    override fun getDefaultIndexAdapter() =
        object : IndexAdapter {
            override fun onStatementReady(stmtVarName: String, scope: CodeGenScope) {}

            override fun getIndexVars() =
                List(readers.size) { index -> ColumnIndexVar(null, index.toString()) }
        }
}
