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
import androidx.room.solver.CodeGenScope
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName

class MapQueryResultAdapter(
    private val keyTypeArg: XType,
    private val valueTypeArg: XType,
    private val keyRowAdapter: RowAdapter,
    private val valueRowAdapter: RowAdapter,
) : QueryResultAdapter(listOf(keyRowAdapter, valueRowAdapter)) {
    private val listType = ParameterizedTypeName.get(
        ClassName.get(List::class.java),
        valueTypeArg.typeName
    )

    private val arrayListType = ParameterizedTypeName
        .get(ClassName.get(ArrayList::class.java), valueTypeArg.typeName)

    private val mapType = ParameterizedTypeName.get(
        ClassName.get(Map::class.java),
        keyTypeArg.typeName,
        listType
    )

    private val hashMapType = ParameterizedTypeName.get(
        ClassName.get(HashMap::class.java),
        keyTypeArg.typeName,
        listType
    )

    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        scope.builder().apply {
            keyRowAdapter.onCursorReady(cursorVarName, scope)
            valueRowAdapter.onCursorReady(cursorVarName, scope)
            addStatement(
                "final $T $L = new $T()",
                mapType, outVarName, hashMapType
            )
            val tmpKeyVarName = scope.getTmpVar("_key")
            val tmpValueVarName = scope.getTmpVar("_value")
            beginControlFlow("while ($L.moveToNext())", cursorVarName).apply {
                addStatement("final $T $L", keyTypeArg.typeName, tmpKeyVarName)
                keyRowAdapter.convert(tmpKeyVarName, cursorVarName, scope)

                addStatement("final $T $L", valueTypeArg.typeName, tmpValueVarName)
                valueRowAdapter.convert(tmpValueVarName, cursorVarName, scope)

                val tmpListVarName = scope.getTmpVar("_values")
                addStatement("$T $L", listType, tmpListVarName)
                beginControlFlow("if ($L.containsKey($L))", outVarName, tmpKeyVarName).apply {
                    addStatement("$L = $L.get($L)", tmpListVarName, outVarName, tmpKeyVarName)
                }
                nextControlFlow("else").apply {
                    addStatement("$L = new $T()", tmpListVarName, arrayListType)
                    addStatement("$L.put($L, $L)", outVarName, tmpKeyVarName, tmpListVarName)
                }
                endControlFlow()
                addStatement("$L.add($L)", tmpListVarName, tmpValueVarName)
            }
            endControlFlow()
            keyRowAdapter.onCursorFinished()?.invoke(scope)
            valueRowAdapter.onCursorFinished()?.invoke(scope)
        }
    }
}