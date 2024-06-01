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

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XCodeBlock.Builder.Companion.beginForEachControlFlow
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XArrayType
import androidx.room.ext.getToArrayFunction
import androidx.room.solver.CodeGenScope

class ArrayQueryResultAdapter(
    private val arrayType: XArrayType,
    private val listResultAdapter: ListQueryResultAdapter
) : QueryResultAdapter(listResultAdapter.rowAdapters) {
    private val componentTypeName: XTypeName = arrayType.componentType.asTypeName()
    private val arrayTypeName = XTypeName.getArrayName(componentTypeName)

    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        scope.builder.apply {
            val listVarName = scope.getTmpVar("_listResult")
            // Delegate to the ListQueryResultAdapter to convert query result to a List.
            listResultAdapter.convert(listVarName, cursorVarName, scope)

            // Initialize _result to be returned, using the list result we have.
            val tmpArrayResult = scope.getTmpVar("_tmpArrayResult")

            val assignCode =
                XCodeBlock.of(language = language, format = "%L", listVarName).let {
                    when (language) {
                        CodeLanguage.KOTLIN -> {
                            if (componentTypeName.isPrimitive) {
                                // If we have a primitive array like LongArray or ShortArray,
                                // we use conversion functions like toLongArray() or toShortArray().
                                XCodeBlock.of(
                                    language = language,
                                    format = "%L.%L",
                                    it,
                                    getToArrayFunction(componentTypeName)
                                )
                            } else {
                                XCodeBlock.of(
                                    language = language,
                                    format = "%L.%L",
                                    it,
                                    "toTypedArray()"
                                )
                            }
                        }
                        CodeLanguage.JAVA -> {
                            if (componentTypeName.isPrimitive) {
                                // In Java, initializing an Array using a List is not
                                // straightforward, and requires we create an empty array that will
                                // be
                                // initialized using the list contents.
                                addLocalVariable(
                                    name = tmpArrayResult,
                                    typeName = arrayTypeName,
                                    assignExpr =
                                        XCodeBlock.of(
                                            language = language,
                                            format = "new %T[%L.size()]",
                                            componentTypeName,
                                            listVarName
                                        )
                                )
                                // If the array is primitive, we have to loop over the list to copy
                                // contents, as we cannot use toArray() on primitive array types.
                                val indexVarName = scope.getTmpVar("_index")
                                addLocalVariable(
                                    name = indexVarName,
                                    typeName = componentTypeName,
                                    isMutable = true,
                                    assignExpr = XCodeBlock.of(language, "0")
                                )
                                val itrVar = scope.getTmpVar("_listItem")
                                beginForEachControlFlow(
                                        iteratorVarName = listVarName,
                                        typeName = componentTypeName,
                                        itemVarName = itrVar
                                    )
                                    .apply {
                                        addStatement(
                                            "%L[%L] = %L",
                                            tmpArrayResult,
                                            indexVarName,
                                            itrVar
                                        )
                                        addStatement("%L++", indexVarName)
                                    }
                                    .endControlFlow()
                                XCodeBlock.of(language = language, format = "%L", tmpArrayResult)
                            } else {
                                // If the array is not primitive, we use the List.toArray() utility.
                                XCodeBlock.of(
                                    language = language,
                                    format = "%L.toArray(new %T[0])",
                                    listVarName,
                                    componentTypeName
                                )
                            }
                        }
                    }
                }
            addLocalVariable(name = outVarName, typeName = arrayTypeName, assignExpr = assignCode)
        }
    }

    override fun isMigratedToDriver(): Boolean = true
}
