/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.parser.ParsedQuery
import androidx.room.processor.Context
import androidx.room.solver.CodeGenScope

class MapQueryResultAdapter(
    context: Context,
    parsedQuery: ParsedQuery,
    private val mapValueResultAdapter: MapValueResultAdapter.NestedMapValueResultAdapter,
) : MultimapQueryResultAdapter(context, parsedQuery, mapValueResultAdapter.rowAdapters) {

    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        scope.builder.apply {
            generateCursorIndexes(cursorVarName, scope)
            addLocalVariable(
                name = outVarName,
                typeName = mapValueResultAdapter.getDeclarationTypeName(),
                assignExpr = XCodeBlock.ofNewInstance(
                    language,
                    mapValueResultAdapter.getInstantiationTypeName()
                )
            )
            beginControlFlow("while (%L.moveToNext())", cursorVarName).apply {
                mapValueResultAdapter.convert(
                    scope,
                    outVarName,
                    cursorVarName,
                    dupeColumnsIndexAdapter,
                )
            }.endControlFlow()
        }
    }

    private fun generateCursorIndexes(cursorVarName: String, scope: CodeGenScope) {
        if (dupeColumnsIndexAdapter != null) {
            // There are duplicate columns in the result objects, generate code that provides
            // us with the indices resolved and pass it to the adapters so it can retrieve
            // the index of each column used by it.
            dupeColumnsIndexAdapter.onCursorReady(cursorVarName, scope)
            rowAdapters.forEach {
                check(it is QueryMappedRowAdapter)
                val indexVarNames = dupeColumnsIndexAdapter.getIndexVarsForMapping(it.mapping)
                it.onCursorReady(
                    indices = indexVarNames,
                    cursorVarName = cursorVarName,
                    scope = scope
                )
            }
        } else {
            rowAdapters.forEach {
                it.onCursorReady(cursorVarName = cursorVarName, scope = scope)
            }
        }
    }
}
