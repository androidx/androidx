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

package androidx.room3.solver.query.result

import androidx.room3.compiler.codegen.XCodeBlock
import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.compiler.processing.XArrayType
import androidx.room3.ext.getToArrayFunction
import androidx.room3.solver.CodeGenScope

class ArrayQueryResultAdapter(
    private val arrayType: XArrayType,
    private val listResultAdapter: ListQueryResultAdapter,
) : QueryResultAdapter(listResultAdapter.rowAdapters) {
    private val componentTypeName: XTypeName = arrayType.componentType.asTypeName()
    private val arrayTypeName = XTypeName.getArrayName(componentTypeName)

    override fun convert(outVarName: String, stmtVarName: String, scope: CodeGenScope) {
        val listVarName = scope.getTmpVar("_listResult")
        // Delegate to the ListQueryResultAdapter to convert query result to a List.
        listResultAdapter.convert(listVarName, stmtVarName, scope)
        val assignCode =
            XCodeBlock.of("%L", listVarName).let {
                if (componentTypeName.isPrimitive) {
                    // If we have a primitive array like LongArray or ShortArray,
                    // we use conversion functions like toLongArray() or toShortArray().
                    XCodeBlock.of("%L.%L", it, getToArrayFunction(componentTypeName))
                } else {
                    XCodeBlock.of("%L.%L", it, "toTypedArray()")
                }
            }
        scope.builder.addLocalVariable(
            name = outVarName,
            typeName = arrayTypeName,
            assignExpr = assignCode,
        )
    }
}
