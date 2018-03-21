/*
 * Copyright (C) 2016 The Android Open Source Project
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

import androidx.room.ext.L
import androidx.room.ext.T
import androidx.room.ext.typeName
import androidx.room.solver.CodeGenScope
import defaultValue

/**
 * Wraps a row adapter when there is only 1 item in the result
 */
class SingleEntityQueryResultAdapter(rowAdapter: RowAdapter) : QueryResultAdapter(rowAdapter) {
    val type = rowAdapter.out
    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        scope.builder().apply {
            rowAdapter?.onCursorReady(cursorVarName, scope)
            addStatement("final $T $L", type.typeName(), outVarName)
            beginControlFlow("if($L.moveToFirst())", cursorVarName)
                rowAdapter?.convert(outVarName, cursorVarName, scope)
            nextControlFlow("else").apply {
                addStatement("$L = $L", outVarName, rowAdapter?.out?.defaultValue())
            }
            endControlFlow()
            rowAdapter?.onCursorFinished()?.invoke(scope)
        }
    }
}
