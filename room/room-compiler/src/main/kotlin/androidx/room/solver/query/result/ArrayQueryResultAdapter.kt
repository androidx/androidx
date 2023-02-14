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
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.box
import androidx.room.compiler.processing.XArrayType
import androidx.room.compiler.processing.XNullability
import androidx.room.ext.KotlinCollectionMemberNames.ARRAY_OF_NULLS
import androidx.room.ext.getToArrayFunction
import androidx.room.solver.CodeGenScope

class ArrayQueryResultAdapter(
    private val arrayType: XArrayType,
    private val rowAdapter: RowAdapter
) : QueryResultAdapter(listOf(rowAdapter)) {
    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        scope.builder.apply {
            rowAdapter.onCursorReady(cursorVarName = cursorVarName, scope = scope)
            val componentTypeName: XTypeName = arrayType.componentType.asTypeName()
            val arrayTypeName = XTypeName.getArrayName(componentTypeName)

            // For Java, instantiate a new array of a size using the bracket syntax, for Kotlin
            // create the array using the std-lib function arrayOfNulls.
            val tmpResultName = scope.getTmpVar("_tmpResult")
            addLocalVariable(
                name = tmpResultName,
                typeName = XTypeName.getArrayName(componentTypeName.copy(nullable = true)),
                assignExpr = when (language) {
                    CodeLanguage.KOTLIN ->
                        XCodeBlock.of(
                            language = language,
                            format = "%M<%T>(%L.getCount())",
                            ARRAY_OF_NULLS,
                            componentTypeName,
                            cursorVarName
                        )
                    CodeLanguage.JAVA ->
                        XCodeBlock.of(
                            language = language,
                            format = "new %T[%L.getCount()]",
                            componentTypeName,
                            cursorVarName
                        )
                }
            )

            val tmpVarName = scope.getTmpVar("_item")
            val indexVar = scope.getTmpVar("_index")
            addLocalVariable(
                name = indexVar,
                typeName = XTypeName.PRIMITIVE_INT,
                assignExpr = XCodeBlock.of(language, "0"),
                isMutable = true
            )
            beginControlFlow("while (%L.moveToNext())", cursorVarName).apply {
                addLocalVariable(
                    name = tmpVarName,
                    typeName = componentTypeName
                )
                rowAdapter.convert(tmpVarName, cursorVarName, scope)
                addStatement("%L[%L] = %L", tmpResultName, indexVar, tmpVarName)
                addStatement("%L++", indexVar)
            }
            endControlFlow()

            // Finally initialize _result to be returned. Will avoid an unnecessary cast in
            // Kotlin if the Entity was already nullable.
            val assignCode = XCodeBlock.of(
                language = language,
                format = "%L",
                tmpResultName
            ).let {
                if (
                    language == CodeLanguage.KOTLIN &&
                    componentTypeName.nullability == XNullability.NONNULL
                ) {
                    XCodeBlock.ofCast(
                        language = language,
                        typeName = XTypeName.getArrayName(componentTypeName.box()),
                        expressionBlock = it
                    )
                } else {
                    it
                }
            }.let {
                // If the component is a primitive type and the language is Kotlin, we need to use
                // an additional built-in function to cast from the boxed to the primitive array
                // type, i.e. Array<Int> to IntArray.
                if (
                    language == CodeLanguage.KOTLIN &&
                    componentTypeName.isPrimitive
                ) {
                    XCodeBlock.of(
                        language = language,
                        format = "(%L).%L",
                        it,
                        getToArrayFunction(componentTypeName)
                    )
                } else {
                    it
                }
            }
            addLocalVariable(
                name = outVarName,
                typeName = arrayTypeName,
                assignExpr = assignCode
            )
        }
    }
}
