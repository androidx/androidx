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

class GuavaImmutableMultimapQueryResultAdapter(
    private val keyTypeArg: XType,
    private val valueTypeArg: XType,
    private val keyRowAdapter: RowAdapter,
    private val valueRowAdapter: RowAdapter,
    private val immutableClassName: ClassName
) : QueryResultAdapter(listOf(keyRowAdapter, valueRowAdapter)) {
    private val mapType = ParameterizedTypeName.get(
        immutableClassName,
        keyTypeArg.typeName,
        valueTypeArg.typeName
    )

    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        val mapVarName = scope.getTmpVar("_mapBuilder")

        scope.builder().apply {
            keyRowAdapter.onCursorReady(cursorVarName, scope)
            valueRowAdapter.onCursorReady(cursorVarName, scope)
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
                addStatement("final $T $L", valueTypeArg.typeName, tmpValueVarName)
                valueRowAdapter.convert(tmpValueVarName, cursorVarName, scope)
                addStatement("$L.put($L, $L)", mapVarName, tmpKeyVarName, tmpValueVarName)
            }
            endControlFlow()
            addStatement("final $T $L = $L.build()", mapType, outVarName, mapVarName)
            keyRowAdapter.onCursorFinished()?.invoke(scope)
            valueRowAdapter.onCursorFinished()?.invoke(scope)
        }
    }
}