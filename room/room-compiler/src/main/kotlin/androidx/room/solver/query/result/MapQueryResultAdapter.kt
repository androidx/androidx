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

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import androidx.room.ext.CommonTypeNames
import androidx.room.parser.ParsedQuery
import androidx.room.processor.Context
import androidx.room.solver.CodeGenScope
import androidx.room.solver.query.result.MultimapQueryResultAdapter.MapType.Companion.isSparseArray

class MapQueryResultAdapter(
    context: Context,
    private val parsedQuery: ParsedQuery,
    override val keyTypeArg: XType,
    override val valueTypeArg: XType,
    private val keyRowAdapter: QueryMappedRowAdapter,
    private val valueRowAdapter: QueryMappedRowAdapter,
    private val valueCollectionType: CollectionValueType?,
    private val mapType: MapType
) : MultimapQueryResultAdapter(context, parsedQuery, listOf(keyRowAdapter, valueRowAdapter)) {

    // The type name of the result map value
    // For Map<Foo, Bar> it is Bar
    // for Map<Foo, List<Bar> it is List<Bar>
    private val valueTypeName = if (valueCollectionType != null) {
        valueCollectionType.className.parametrizedBy(valueTypeArg.asTypeName())
    } else {
        valueTypeArg.asTypeName()
    }

    // The type name of the concrete result map value
    // For Map<Foo, Bar> it is Bar
    // For Map<Foo, List<Bar> it is ArrayList<Bar>
    private val implValueTypeName = when (valueCollectionType) {
        CollectionValueType.LIST ->
            CommonTypeNames.ARRAY_LIST.parametrizedBy(valueTypeArg.asTypeName())
        CollectionValueType.SET ->
            CommonTypeNames.HASH_SET.parametrizedBy(valueTypeArg.asTypeName())
        else ->
            valueTypeArg.asTypeName()
    }

    // The type name of the result map
    private val mapTypeName = when (mapType) {
        MapType.DEFAULT, MapType.ARRAY_MAP ->
            mapType.className.parametrizedBy(keyTypeArg.asTypeName(), valueTypeName)
        MapType.LONG_SPARSE, MapType.INT_SPARSE ->
            mapType.className.parametrizedBy(valueTypeName)
    }

    // The type name of the concrete result map
    private val implMapTypeName = when (mapType) {
        MapType.DEFAULT ->
            // LinkedHashMap is used as impl to preserve key ordering for ordered query results.
            CommonTypeNames.LINKED_HASH_MAP.parametrizedBy(
                keyTypeArg.asTypeName(), valueTypeName
            )
        MapType.ARRAY_MAP ->
            mapType.className.parametrizedBy(keyTypeArg.asTypeName(), valueTypeName)
        MapType.LONG_SPARSE, MapType.INT_SPARSE ->
            mapType.className.parametrizedBy(valueTypeName)
    }

    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
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

            addLocalVariable(
                name = outVarName,
                typeName = mapTypeName,
                assignExpr = XCodeBlock.ofNewInstance(language, implMapTypeName)
            )

            val tmpKeyVarName = scope.getTmpVar("_key")
            val tmpValueVarName = scope.getTmpVar("_value")
            beginControlFlow("while (%L.moveToNext())", cursorVarName).apply {
                addLocalVariable(tmpKeyVarName, keyTypeArg.asTypeName())
                keyRowAdapter.convert(tmpKeyVarName, cursorVarName, scope)

                val valueIndexVars =
                    dupeColumnsIndexAdapter?.getIndexVarsForMapping(valueRowAdapter.mapping)
                        ?: valueRowAdapter.getDefaultIndexAdapter().getIndexVars()
                val columnNullCheckCodeBlock = getColumnNullCheckCode(
                    language = language,
                    cursorVarName = cursorVarName,
                    indexVars = valueIndexVars
                )

                // If valueCollectionType is null, this means that we have a 1-to-1 mapping, as
                // opposed to a 1-to-many mapping.
                if (valueCollectionType != null) {
                    val tmpCollectionVarName = scope.getTmpVar("_values")
                    addLocalVariable(tmpCollectionVarName, valueTypeName)

                    if (mapType.isSparseArray()) {
                        beginControlFlow("if (%L.get(%L) != null)", outVarName, tmpKeyVarName)
                    } else {
                        beginControlFlow("if (%L.containsKey(%L))", outVarName, tmpKeyVarName)
                    }.apply {
                        val getFunction = when (language) {
                            CodeLanguage.JAVA -> "get"
                            CodeLanguage.KOTLIN ->
                                if (mapType.isSparseArray()) "get" else "getValue"
                        }
                        addStatement(
                            "%L = %L.%L(%L)",
                            tmpCollectionVarName,
                            outVarName,
                            getFunction,
                            tmpKeyVarName
                        )
                    }.nextControlFlow("else").apply {
                        addStatement(
                            "%L = %L",
                            tmpCollectionVarName,
                            XCodeBlock.ofNewInstance(language, implValueTypeName)
                        )
                        addStatement(
                            "%L.put(%L, %L)",
                            outVarName,
                            tmpKeyVarName,
                            tmpCollectionVarName
                        )
                    }.endControlFlow()

                    // Perform value columns null check, in a 1-to-many mapping we still add the key
                    // with an empty collection as the value entry.
                    beginControlFlow("if (%L)", columnNullCheckCodeBlock).apply {
                        addStatement("continue")
                    }.endControlFlow()

                    addLocalVariable(tmpValueVarName, valueTypeArg.asTypeName())
                    valueRowAdapter.convert(tmpValueVarName, cursorVarName, scope)
                    addStatement("%L.add(%L)", tmpCollectionVarName, tmpValueVarName)
                } else {
                    // Perform value columns null check, in a 1-to-1 mapping we still add the key
                    // with a null value entry if permitted.
                    beginControlFlow("if (%L)", columnNullCheckCodeBlock).apply {
                        if (
                            language == CodeLanguage.KOTLIN &&
                            valueTypeArg.nullability == XNullability.NONNULL
                        ) {
                            // TODO(b/249984504): Generate / output a better message.
                            addStatement("error(%S)", "Missing value for a key.")
                        } else {
                            addStatement("%L.put(%L, null)", outVarName, tmpKeyVarName)
                            addStatement("continue")
                        }
                    }.endControlFlow()

                    addLocalVariable(tmpValueVarName, valueTypeArg.asTypeName())
                    valueRowAdapter.convert(tmpValueVarName, cursorVarName, scope)

                    // For consistency purposes, in the one-to-one object mapping case, if
                    // multiple values are encountered for the same key, we will only consider
                    // the first ever encountered mapping.
                    if (mapType.isSparseArray()) {
                        beginControlFlow("if (%L.get(%L) == null)", outVarName, tmpKeyVarName)
                    } else {
                        beginControlFlow("if (!%L.containsKey(%L))", outVarName, tmpKeyVarName)
                    }.apply {
                        addStatement("%L.put(%L, %L)", outVarName, tmpKeyVarName, tmpValueVarName)
                    }.endControlFlow()
                }
            }
            endControlFlow()
        }
    }
}
