/*
 * Copyright 2020 The Android Open Source Project
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
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.isVoid
import androidx.room.ext.DEFAULT_IMPLS_CLASS_NAME
import androidx.room.vo.KotlinDefaultMethodDelegate

/**
 * Method binder that delegates to concrete DAO function in a Kotlin interface, specifically to
 * a function where the implementation is in the DefaultImpl Kotlin generated class.
 *
 * @see [KotlinDefaultMethodDelegate]
 */
object KotlinDefaultMethodDelegateBinder {
    fun executeAndReturn(
        daoName: XClassName,
        daoImplName: XClassName,
        methodName: String,
        returnType: XType,
        parameterNames: List<String>,
        scope: CodeGenScope
    ) {
        check(scope.language == CodeLanguage.JAVA)
        scope.builder.apply {
            val params = mutableListOf<Any>()
            val format = buildString {
                if (!returnType.isVoid()) {
                    append("return ")
                }
                append("%T.%L.%L(%T.this")
                params.add(daoName)
                params.add(DEFAULT_IMPLS_CLASS_NAME)
                params.add(methodName)
                params.add(daoImplName)
                parameterNames.forEach {
                    append(", %L")
                    params.add(it)
                }
                append(")")
            }
            addStatement(format, *params.toTypedArray())
        }
    }
}
