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

package com.android.support.room.solver.query.result

import com.android.support.room.ext.L
import com.android.support.room.ext.T
import com.android.support.room.ext.typeName
import com.android.support.room.processor.Context
import com.android.support.room.solver.CodeGenScope
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Element

class ArrayQueryResultAdapter(val rowAdapter: RowAdapter) : QueryResultAdapter() {
    override fun reportErrors(context: Context, element: Element, suppressedWarnings: Set<String>) {
        rowAdapter.reportErrors(context, element, suppressedWarnings)
    }

    val type = rowAdapter.out
    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        scope.builder().apply {
            val converter = rowAdapter.init(cursorVarName, scope)
            val arrayType = ArrayTypeName.of(type.typeName())
            addStatement("final $T $L = new $T[$L.getCount()]",
                    arrayType, outVarName, type.typeName(), cursorVarName)
            val tmpVarName = scope.getTmpVar("_item")
            val indexVar = scope.getTmpVar("_index")
            addStatement("$T $L = 0", TypeName.INT, indexVar)
            beginControlFlow("while($L.moveToNext())", cursorVarName).apply {
                addStatement("final $T $L", type.typeName(), tmpVarName)
                converter.convert(tmpVarName, cursorVarName)
                addStatement("$L[$L] = $L", outVarName, indexVar, tmpVarName)
                addStatement("$L ++", indexVar)
            }
            endControlFlow()
        }
    }
}
