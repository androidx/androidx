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

import androidx.room3.compiler.codegen.CodeLanguage
import androidx.room3.compiler.codegen.XCodeBlock
import androidx.room3.compiler.codegen.XCodeBlock.Builder.Companion.applyTo
import androidx.room3.compiler.processing.XType
import androidx.room3.ext.CommonTypeNames
import androidx.room3.ext.CommonTypeNames.ARRAY_LIST
import androidx.room3.ext.KotlinCollectionMemberNames
import androidx.room3.solver.CodeGenScope

class ListQueryResultAdapter(private val typeArg: XType, private val rowAdapter: RowAdapter) :
    QueryResultAdapter(listOf(rowAdapter)) {
    override fun convert(outVarName: String, stmtVarName: String, scope: CodeGenScope) {
        rowAdapter.onStatementReady(stmtVarName = stmtVarName, scope = scope)
        scope.builder
            .applyTo { language ->
                val listTypeName = CommonTypeNames.MUTABLE_LIST.parametrizedBy(typeArg.asTypeName())
                when (language) {
                    CodeLanguage.JAVA ->
                        addLocalVariable(
                            name = outVarName,
                            typeName = listTypeName,
                            assignExpr =
                                XCodeBlock.ofNewInstance(
                                    ARRAY_LIST.parametrizedBy(typeArg.asTypeName())
                                ),
                        )
                    CodeLanguage.KOTLIN ->
                        addLocalVal(
                            outVarName,
                            listTypeName,
                            "%M()",
                            KotlinCollectionMemberNames.MUTABLE_LIST_OF,
                        )
                }
            }
            .apply {
                val tmpVarName = scope.getTmpVar("_item")
                beginControlFlow("while (%L.step())", stmtVarName).apply {
                    addLocalVariable(name = tmpVarName, typeName = typeArg.asTypeName())
                    rowAdapter.convert(tmpVarName, stmtVarName, scope)
                    addStatement("%L.add(%L)", outVarName, tmpVarName)
                }
                endControlFlow()
            }
    }
}
