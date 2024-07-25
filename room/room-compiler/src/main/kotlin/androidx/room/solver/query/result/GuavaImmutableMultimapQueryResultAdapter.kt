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

import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.processing.XType
import androidx.room.ext.GuavaTypeNames
import androidx.room.parser.ParsedQuery
import androidx.room.processor.Context
import androidx.room.solver.CodeGenScope

class GuavaImmutableMultimapQueryResultAdapter(
    context: Context,
    private val parsedQuery: ParsedQuery,
    private val keyTypeArg: XType,
    private val valueTypeArg: XType,
    private val keyRowAdapter: RowAdapter,
    private val valueRowAdapter: RowAdapter,
    private val immutableClassName: XClassName,
) : MultimapQueryResultAdapter(context, parsedQuery, listOf(keyRowAdapter, valueRowAdapter)) {
    private val mapType =
        immutableClassName.parametrizedBy(keyTypeArg.asTypeName(), valueTypeArg.asTypeName())

    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        val mapVarName = scope.getTmpVar("_mapBuilder")

        scope.builder.apply {
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

            val builderClassName =
                when (immutableClassName) {
                    GuavaTypeNames.IMMUTABLE_LIST_MULTIMAP ->
                        GuavaTypeNames.IMMUTABLE_LIST_MULTIMAP_BUILDER
                    GuavaTypeNames.IMMUTABLE_SET_MULTIMAP ->
                        GuavaTypeNames.IMMUTABLE_SET_MULTIMAP_BUILDER
                    else ->
                        // Return type is base class ImmutableMultimap, need the case handled here,
                        // but won't actually get here in the code if this is the case as we will
                        // do an early return in TypeAdapterStore.kt.
                        GuavaTypeNames.IMMUTABLE_MULTIMAP_BUILDER
                }

            addLocalVariable(
                name = mapVarName,
                typeName =
                    builderClassName.parametrizedBy(
                        keyTypeArg.asTypeName(),
                        valueTypeArg.asTypeName()
                    ),
                assignExpr =
                    XCodeBlock.of(language = language, format = "%T.builder()", immutableClassName)
            )

            val tmpKeyVarName = scope.getTmpVar("_key")
            val tmpValueVarName = scope.getTmpVar("_value")
            val stepName = if (scope.useDriverApi) "step" else "moveToNext"
            beginControlFlow("while (%L.$stepName())", cursorVarName).apply {
                addLocalVariable(name = tmpKeyVarName, typeName = keyTypeArg.asTypeName())
                keyRowAdapter.convert(tmpKeyVarName, cursorVarName, scope)

                // Iterate over all matched fields to check if all are null. If so, we continue in
                // the while loop to the next iteration.
                check(valueRowAdapter is QueryMappedRowAdapter)
                val valueIndexVars =
                    dupeColumnsIndexAdapter?.getIndexVarsForMapping(valueRowAdapter.mapping)
                        ?: valueRowAdapter.getDefaultIndexAdapter().getIndexVars()
                val columnNullCheckCodeBlock =
                    getColumnNullCheckCode(
                        language = language,
                        cursorVarName = cursorVarName,
                        indexVars = valueIndexVars
                    )
                // Perform column null check
                beginControlFlow("if (%L)", columnNullCheckCodeBlock)
                    .apply { addStatement("continue") }
                    .endControlFlow()

                addLocalVariable(name = tmpValueVarName, typeName = valueTypeArg.asTypeName())
                valueRowAdapter.convert(tmpValueVarName, cursorVarName, scope)
                addStatement("%L.put(%L, %L)", mapVarName, tmpKeyVarName, tmpValueVarName)
            }
            endControlFlow()
            addLocalVariable(
                name = outVarName,
                typeName = mapType,
                assignExpr = XCodeBlock.of(language = language, format = "%L.build()", mapVarName)
            )
        }
    }

    override fun isMigratedToDriver() = true
}
