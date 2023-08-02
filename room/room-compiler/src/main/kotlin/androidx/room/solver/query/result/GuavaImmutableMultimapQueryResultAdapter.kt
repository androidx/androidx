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

import androidx.room.compiler.processing.XType
import androidx.room.ext.L
import androidx.room.ext.T
import androidx.room.parser.ParsedQuery
import androidx.room.processor.Context
import androidx.room.solver.CodeGenScope
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName

class GuavaImmutableMultimapQueryResultAdapter(
    context: Context,
    private val parsedQuery: ParsedQuery,
    override val keyTypeArg: XType,
    override val valueTypeArg: XType,
    private val keyRowAdapter: QueryMappedRowAdapter,
    private val valueRowAdapter: QueryMappedRowAdapter,
    private val immutableClassName: ClassName,
) : MultimapQueryResultAdapter(context, parsedQuery, listOf(keyRowAdapter, valueRowAdapter)) {
    private val mapType = ParameterizedTypeName.get(
        immutableClassName,
        keyTypeArg.typeName,
        valueTypeArg.typeName
    )

    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        val mapVarName = scope.getTmpVar("_mapBuilder")

        scope.builder().apply {
            val dupeColumnsIndexAdapter: AmbiguousColumnIndexAdapter?
            if (duplicateColumns.isNotEmpty()) {
                // There are duplicate columns in the result objects, generate code that provides
                // us with the indices resolved and pass it to the adapters so it can retrieve
                // the index of each column used by it.
                dupeColumnsIndexAdapter = AmbiguousColumnIndexAdapter(mappings, parsedQuery)
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
                dupeColumnsIndexAdapter = null
                rowAdapters.forEach {
                    it.onCursorReady(cursorVarName = cursorVarName, scope = scope)
                }
            }
            addStatement(
                "final $T.Builder<$T, $T> $L = $T.builder()",
                immutableClassName,
                keyTypeArg.typeName,
                valueTypeArg.typeName,
                mapVarName,
                immutableClassName
            )
            val tmpKeyVarName = scope.getTmpVar("_key")
            val tmpValueVarName = scope.getTmpVar("_value")
            beginControlFlow("while ($L.moveToNext())", cursorVarName).apply {
                addStatement("final $T $L", keyTypeArg.typeName, tmpKeyVarName)
                keyRowAdapter.convert(tmpKeyVarName, cursorVarName, scope)

                // Iterate over all matched fields to check if all are null. If so, we continue in
                // the while loop to the next iteration.
                val valueIndexVars =
                    dupeColumnsIndexAdapter?.getIndexVarsForMapping(valueRowAdapter.mapping)
                        ?: valueRowAdapter.getDefaultIndexAdapter().getIndexVars()
                val columnNullCheckCodeBlock = getColumnNullCheckCode(
                    cursorVarName = cursorVarName,
                    indexVars = valueIndexVars
                )
                // Perform column null check
                beginControlFlow("if ($L)", columnNullCheckCodeBlock).apply {
                    addStatement("continue")
                }.endControlFlow()

                addStatement("final $T $L", valueTypeArg.typeName, tmpValueVarName)
                valueRowAdapter.convert(tmpValueVarName, cursorVarName, scope)
                addStatement("$L.put($L, $L)", mapVarName, tmpKeyVarName, tmpValueVarName)
            }
            endControlFlow()
            addStatement("final $T $L = $L.build()", mapType, outVarName, mapVarName)
        }
    }
}