/*
 * Copyright 2020 The Android Open Source Project
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
import com.google.common.collect.ImmutableList
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName

class ImmutableListQueryResultAdapter(
    private val typeArg: XType,
    private val rowAdapter: RowAdapter
) : QueryResultAdapter(listOf(rowAdapter)) {
    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        scope.builder().apply {
            rowAdapter.onCursorReady(cursorVarName, scope)
            val collectionType = ParameterizedTypeName
                .get(ClassName.get(ImmutableList::class.java), typeArg.typeName)
            val immutableListBuilderType = ParameterizedTypeName
                .get(ClassName.get(ImmutableList.Builder::class.java), typeArg.typeName)
            val immutableListBuilderName = scope.getTmpVar("_immutableListBuilder")
            addStatement(
                "final $T $L = $T.<$T>builder()",
                immutableListBuilderType, immutableListBuilderName,
                ClassName.get(ImmutableList::class.java), typeArg.typeName
            )
            val tmpVarName = scope.getTmpVar("_item")
            beginControlFlow("while($L.moveToNext())", cursorVarName).apply {
                addStatement("final $T $L", typeArg.typeName, tmpVarName)
                rowAdapter.convert(tmpVarName, cursorVarName, scope)
                addStatement("$L.add($L)", immutableListBuilderName, tmpVarName)
            }
            endControlFlow()
            addStatement(
                "final $T $L = $L.build()",
                collectionType, outVarName, immutableListBuilderName
            )
            rowAdapter.onCursorFinished()?.invoke(scope)
        }
    }
}