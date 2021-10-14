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
import androidx.room.ext.CollectionTypeNames.ARRAY_MAP
import androidx.room.ext.L
import androidx.room.ext.T
import androidx.room.solver.CodeGenScope
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName

class MapQueryResultAdapter(
    override val keyTypeArg: XType,
    override val valueTypeArg: XType,
    private val keyRowAdapter: RowAdapter,
    private val valueRowAdapter: RowAdapter,
    private val valueCollectionType: XType?,
    isArrayMap: Boolean = false,
    private val isSparseArray: ClassName? = null
) : QueryResultAdapter(listOf(keyRowAdapter, valueRowAdapter)), MultimapQueryResultAdapter {
    private val declaredToConcreteCollection = mapOf<ClassName, ClassName>(
        ClassName.get(List::class.java) to ClassName.get(ArrayList::class.java),
        ClassName.get(Set::class.java) to ClassName.get(HashSet::class.java)
    )

    private val declaredValueType = if (valueCollectionType != null) {
        ParameterizedTypeName.get(
            valueCollectionType.typeElement?.className,
            valueTypeArg.typeName
        )
    } else {
        valueTypeArg.typeName
    }

    private val concreteValueType = if (valueCollectionType != null) {
        ParameterizedTypeName.get(
            declaredToConcreteCollection[valueCollectionType.typeElement?.className],
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
            if (isArrayMap) ARRAY_MAP else ClassName.get(Map::class.java),
            keyTypeArg.typeName,
            declaredValueType
        )
    }

    // LinkedHashMap is used as impl to preserve key ordering for ordered query results.
    private val mapImplType =
        if (isSparseArray != null) {
            ParameterizedTypeName.get(
                isSparseArray,
                declaredValueType
            )
        } else {
            ParameterizedTypeName.get(
                if (isArrayMap) ARRAY_MAP else ClassName.get(LinkedHashMap::class.java),
                keyTypeArg.typeName,
                declaredValueType
            )
        }

    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        scope.builder().apply {
            keyRowAdapter.onCursorReady(cursorVarName, scope)
            valueRowAdapter.onCursorReady(cursorVarName, scope)

            addStatement("final $T $L = new $T()", mapType, outVarName, mapImplType)

            val tmpKeyVarName = scope.getTmpVar("_key")
            val tmpValueVarName = scope.getTmpVar("_value")
            beginControlFlow("while ($L.moveToNext())", cursorVarName).apply {
                addStatement("final $T $L", keyTypeArg.typeName, tmpKeyVarName)
                keyRowAdapter.convert(tmpKeyVarName, cursorVarName, scope)

                // If valueCollectionType is null, this means that we have a 1-to-1 mapping, as
                // opposed to a 1-to-many mapping.
                if (valueCollectionType != null) {
                    addStatement("final $T $L", valueTypeArg.typeName, tmpValueVarName)
                    valueRowAdapter.convert(tmpValueVarName, cursorVarName, scope)
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
                    }
                    nextControlFlow("else").apply {
                        addStatement("$L = new $T()", tmpCollectionVarName, concreteValueType)
                        addStatement(
                            "$L.put($L, $L)",
                            outVarName,
                            tmpKeyVarName,
                            tmpCollectionVarName
                        )
                    }
                    endControlFlow()
                    addStatement("$L.add($L)", tmpCollectionVarName, tmpValueVarName)
                } else {
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
                    }
                    endControlFlow()
                }
            }
            endControlFlow()
            keyRowAdapter.onCursorFinished()?.invoke(scope)
            valueRowAdapter.onCursorFinished()?.invoke(scope)
        }
    }
}
