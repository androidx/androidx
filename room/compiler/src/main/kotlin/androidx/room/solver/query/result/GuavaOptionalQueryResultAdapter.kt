/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.room.ext.GuavaBaseTypeNames
import androidx.room.ext.L
import androidx.room.ext.T
import androidx.room.ext.typeName
import androidx.room.solver.CodeGenScope
import com.squareup.javapoet.ParameterizedTypeName

/**
 * Wraps a row adapter when there is only 1 item in the result, and the result's outer type is
 * {@link com.google.common.base.Optional}.
 */
class GuavaOptionalQueryResultAdapter(private val resultAdapter: SingleEntityQueryResultAdapter)
    : QueryResultAdapter(resultAdapter.rowAdapter) {
    val type = resultAdapter.rowAdapter?.out
    override fun convert(
            outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        scope.builder().apply {
            val valueVarName = scope.getTmpVar("_value")
            resultAdapter.convert(valueVarName, cursorVarName, scope)
            addStatement(
                    "final $T $L = $T.fromNullable($L)",
                    ParameterizedTypeName.get(GuavaBaseTypeNames.OPTIONAL, type?.typeName()),
                    outVarName,
                    GuavaBaseTypeNames.OPTIONAL,
                    valueVarName)
        }
    }
}

