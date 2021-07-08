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
import com.google.common.collect.ImmutableMap
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName

class ImmutableMapQueryResultAdapter(
    private val keyTypeArg: XType,
    private val valueTypeArg: XType,
    private val resultAdapter: QueryResultAdapter
) : QueryResultAdapter(resultAdapter.rowAdapters) {
    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        scope.builder().apply {
            val mapVarName = scope.getTmpVar("_mapResult")
            resultAdapter.convert(mapVarName, cursorVarName, scope)
            addStatement(
                "final $T $L = $T.copyOf($L)",
                ParameterizedTypeName.get(
                    ClassName.get(ImmutableMap::class.java),
                    keyTypeArg.typeName,
                    valueTypeArg.typeName
                ),
                outVarName,
                ClassName.get(ImmutableMap::class.java),
                mapVarName
            )
        }
    }
}