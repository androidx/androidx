/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.solver

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.unbox
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.isVoid
import androidx.room.vo.KotlinBoxedPrimitiveMethodDelegate

/**
 * Method binder that delegates to a sibling DAO function in a Kotlin interface or abstract class
 * and specifically to a sibling function with unboxed primitive parameters.
 *
 * @see [KotlinBoxedPrimitiveMethodDelegate]
 */
object KotlinBoxedPrimitiveMethodDelegateBinder {

    fun execute(
        methodName: String,
        returnType: XType,
        parameters: List<Pair<XTypeName, String>>,
        scope: CodeGenScope
    ) {
        check(scope.language == CodeLanguage.JAVA)
        scope.builder.apply {
            val params = mutableListOf<Any>()
            val format = buildString {
                if (!returnType.isVoid()) {
                    append("return ")
                }
                append("%L(")
                params.add(methodName)
                parameters.forEachIndexed { i, (typeName, name) ->
                    if (typeName.isPrimitive) {
                        append("(%T) %L")
                        params.add(typeName.unbox())
                        params.add(name)
                    } else {
                        append("%L")
                        params.add(name)
                    }
                    if (i < parameters.size - 1) {
                        append(", ")
                    }
                }
                append(")")
                emptyList<String>().joinToString()
            }
            addStatement(format, *params.toTypedArray())
        }
    }
}
