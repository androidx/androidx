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

package androidx.room.solver.shortcut.result

import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.T
import androidx.room.ext.typeName
import androidx.room.solver.CodeGenScope
import androidx.room.vo.ShortcutQueryParameter
import androidx.room.writer.DaoWriter
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.type.TypeMirror

/**
 * Class that knows how to generate a delete or update method body.
 */
class DeleteOrUpdateMethodAdapter private constructor() {
    companion object {
        fun create(returnType: TypeMirror): DeleteOrUpdateMethodAdapter? {
            if (isDeleteOrUpdateValid(
                            returnType.typeName())) {
                return DeleteOrUpdateMethodAdapter()
            }
            return null
        }

        private fun isDeleteOrUpdateValid(returnTypeName: TypeName) =
                returnTypeName == TypeName.VOID || returnTypeName == TypeName.INT
    }

    fun createDeleteOrUpdateMethodBody(
        returnCount: Boolean,
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<FieldSpec, TypeSpec>>,
        scope: CodeGenScope
    ) {
        val resultVar = if (returnCount) {
            scope.getTmpVar("_total")
        } else {
            null
        }
        scope.builder().apply {
            if (resultVar != null) {
                addStatement("$T $L = 0", TypeName.INT, resultVar)
            }
            addStatement("$N.beginTransaction()", DaoWriter.dbField)
            beginControlFlow("try").apply {
                parameters.forEach { param ->
                    val adapter = adapters[param.name]?.first
                    addStatement("$L$N.$L($L)",
                            if (resultVar == null) "" else "$resultVar +=",
                            adapter, param.handleMethodName(), param.name)
                }
                addStatement("$N.setTransactionSuccessful()",
                        DaoWriter.dbField)
                if (resultVar != null) {
                    addStatement("return $L", resultVar)
                }
            }
            nextControlFlow("finally").apply {
                addStatement("$N.endTransaction()",
                        DaoWriter.dbField)
            }
            endControlFlow()
        }
    }
}