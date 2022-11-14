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

import androidx.room.compiler.codegen.toJavaPoet
import androidx.room.compiler.processing.XType
import androidx.room.ext.CollectionTypeNames.ARRAY_MAP
import androidx.room.ext.L
import androidx.room.ext.T
import androidx.room.parser.ParsedQuery
import androidx.room.processor.Context
import androidx.room.solver.CodeGenScope
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName

class MapQueryResultAdapter(
    context: Context,
    private val parsedQuery: ParsedQuery,
    override val keyTypeArg: XType,
    override val valueTypeArg: XType,
    private val keyRowAdapter: QueryMappedRowAdapter,
    private val valueRowAdapter: QueryMappedRowAdapter,
    private val valueCollectionType: XType?,
    isArrayMap: Boolean = false,
    private val isSparseArray: ClassName? = null,
) : MultimapQueryResultAdapter(context, parsedQuery, listOf(keyRowAdapter, valueRowAdapter)) {

    private val declaredValueType = if (valueCollectionType != null) {
        ParameterizedTypeName.get(
            valueCollectionType.typeElement?.className,
            valueTypeArg.typeName
        )
    } else {
        valueTypeArg.typeName
    }

    private val implValueType = if (valueCollectionType != null) {
        ParameterizedTypeName.get(
            declaredToImplCollection[valueCollectionType.typeElement?.className],
            valueTypeArg.typeName
        )
    } else {
        valueTypeArg.typeName
    }

    private val mapType = if (isSparseArray != null) {
        ParameterizedTypeName.get(
            isSparseArray,
            declaredValueType
        )
    } else {
        ParameterizedTypeName.get(
            if (isArrayMap) ARRAY_MAP.toJavaPoet() else ClassName.get(Map::class.java),
            keyTypeArg.typeName,
            declaredValueType
        )
    }

    private val implMapType = if (isSparseArray != null) {
        ParameterizedTypeName.get(
            isSparseArray,
            declaredValueType
        )
    } else {
        // LinkedHashMap is used as impl to preserve key ordering for ordered query results.
        ParameterizedTypeName.get(
            if (isArrayMap) ARRAY_MAP.toJavaPoet() else ClassName.get(LinkedHashMap::class.java),
            keyTypeArg.typeName,
            declaredValueType
        )
    }

    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
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

            addStatement("final $T $L = new $T()", mapType, outVarName, implMapType)

            val tmpKeyVarName = scope.getTmpVar("_key")
            val tmpValueVarName = scope.getTmpVar("_value")
            beginControlFlow("while ($L.moveToNext())", cursorVarName).apply {
                addStatement("final $T $L", keyTypeArg.typeName, tmpKeyVarName)
                keyRowAdapter.convert(tmpKeyVarName, cursorVarName, scope)

                val valueIndexVars =
                    dupeColumnsIndexAdapter?.getIndexVarsForMapping(valueRowAdapter.mapping)
                        ?: valueRowAdapter.getDefaultIndexAdapter().getIndexVars()
                val columnNullCheckCodeBlock = getColumnNullCheckCode(
                    cursorVarName = cursorVarName,
                    indexVars = valueIndexVars
                )

                // If valueCollectionType is null, this means that we have a 1-to-1 mapping, as
                // opposed to a 1-to-many mapping.
                if (valueCollectionType != null) {
                    val tmpCollectionVarName = scope.getTmpVar("_values")
                    addStatement("$T $L", declaredValueType, tmpCollectionVarName)

                    if (isSparseArray != null) {
                        beginControlFlow("if ($L.get($L) != null)", outVarName, tmpKeyVarName)
                    } else {
                        beginControlFlow("if ($L.containsKey($L))", outVarName, tmpKeyVarName)
                    }.apply {
                        addStatement(
                            "$L = $L.get($L)",
                            tmpCollectionVarName,
                            outVarName,
                            tmpKeyVarName
                        )
                    }.nextControlFlow("else").apply {
                        addStatement("$L = new $T()", tmpCollectionVarName, implValueType)
                        addStatement(
                            "$L.put($L, $L)",
                            outVarName,
                            tmpKeyVarName,
                            tmpCollectionVarName
                        )
                    }.endControlFlow()

                    // Perform value columns null check, in a 1-to-many mapping we still add the key
                    // with an empty collection as the value entry.
                    beginControlFlow("if ($L)", columnNullCheckCodeBlock).apply {
                        addStatement("continue")
                    }.endControlFlow()

                    addStatement("final $T $L", valueTypeArg.typeName, tmpValueVarName)
                    valueRowAdapter.convert(tmpValueVarName, cursorVarName, scope)
                    addStatement("$L.add($L)", tmpCollectionVarName, tmpValueVarName)
                } else {
                    // Perform value columns null check, in a 1-to-1 mapping we still add the key
                    // with a null value entry.
                    beginControlFlow("if ($L)", columnNullCheckCodeBlock).apply {
                        addStatement("$L.put($L, null)", outVarName, tmpKeyVarName)
                        addStatement("continue")
                    }.endControlFlow()

                    addStatement(
                        "final $T $L",
                        valueTypeArg.typeElement?.className,
                        tmpValueVarName
                    )
                    valueRowAdapter.convert(tmpValueVarName, cursorVarName, scope)

                    // For consistency purposes, in the one-to-one object mapping case, if
                    // multiple values are encountered for the same key, we will only consider
                    // the first ever encountered mapping.
                    if (isSparseArray != null) {
                        beginControlFlow("if ($L.get($L) == null)", outVarName, tmpKeyVarName)
                    } else {
                        beginControlFlow("if (!$L.containsKey($L))", outVarName, tmpKeyVarName)
                    }.apply {
                        addStatement("$L.put($L, $L)", outVarName, tmpKeyVarName, tmpValueVarName)
                    }.endControlFlow()
                }
            }
            endControlFlow()
        }
    }
}
