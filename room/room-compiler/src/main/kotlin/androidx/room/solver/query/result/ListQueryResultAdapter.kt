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

import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.processing.XType
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.CommonTypeNames.ARRAY_LIST
import androidx.room.solver.CodeGenScope

class ListQueryResultAdapter(
    private val typeArg: XType,
    private val rowAdapter: RowAdapter
) : QueryResultAdapter(listOf(rowAdapter)) {
    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        scope.builder.apply {
            rowAdapter.onCursorReady(cursorVarName = cursorVarName, scope = scope)
            val listTypeName = CommonTypeNames.MUTABLE_LIST.parametrizedBy(typeArg.asTypeName())
            addLocalVariable(
                name = outVarName,
                typeName = listTypeName,
                assignExpr = XCodeBlock.ofNewInstance(
                    language,
                    ARRAY_LIST.parametrizedBy(typeArg.asTypeName()),
                    "%L.getCount()",
                    cursorVarName
                )
            )
            val tmpVarName = scope.getTmpVar("_item")
            beginControlFlow("while (%L.moveToNext())", cursorVarName).apply {
                addLocalVariable(
                    name = tmpVarName,
                    typeName = typeArg.asTypeName()
                )
                rowAdapter.convert(tmpVarName, cursorVarName, scope)
                addStatement("%L.add(%L)", outVarName, tmpVarName)
            }
            endControlFlow()
        }
    }
}
