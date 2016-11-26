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

package com.android.support.room.solver

import com.android.support.room.ext.L
import com.android.support.room.ext.T
import com.squareup.javapoet.TypeName
import javax.lang.model.type.TypeMirror

/**
 * A column adapter that uses multiple type adapters to do the conversion.
 */
class CompositeAdapter(out: TypeMirror, val columnTypeAdapter: ColumnTypeAdapter,
                       val typeConverters: List<TypeConverter>) : ColumnTypeAdapter(out) {
    override fun readFromCursor(outVarName: String, cursorVarName: String, index: Int,
                                scope: CodeGenScope) {
        val reversed = typeConverters.reversed()

        scope.builder().apply {
            val tmpCursorValue = scope.getTmpVar()
            addStatement("final $T $L", columnTypeAdapter.outTypeName, tmpCursorValue)
            columnTypeAdapter.readFromCursor(tmpCursorValue, cursorVarName, index, scope)
            var tmpInVar = tmpCursorValue
            var tmpOutVar = scope.getTmpVar()
            reversed.take(reversed.size - 1).forEach {
                addStatement("final $T $L", it.fromTypeName, tmpOutVar)
                it.convertBackward(tmpInVar, tmpOutVar, scope)
                tmpInVar = tmpOutVar
                tmpOutVar = scope.getTmpVar()
            }
            reversed.last().convertBackward(tmpInVar, outVarName, scope)
        }
    }

    override fun bindToStmt(stmtName: String, index: Int, valueVarName: String,
                            scope: CodeGenScope) {
        var tmpInVar = valueVarName
        var tmpOutVar = scope.getTmpVar()
        scope.builder().apply {
            typeConverters.forEach {
                addStatement("final $T $L", it.toTypeName, tmpOutVar)
                it.convertForward(tmpInVar, tmpOutVar, scope)
                tmpInVar = tmpOutVar
                tmpOutVar = scope.getTmpVar()
            }
            columnTypeAdapter.bindToStmt(stmtName, index, tmpInVar, scope)
        }
    }
}
