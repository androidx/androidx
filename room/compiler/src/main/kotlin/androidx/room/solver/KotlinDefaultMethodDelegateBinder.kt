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

import androidx.room.ext.DEFAULT_IMPLS_CLASS_NAME
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.T
import com.squareup.javapoet.ClassName
import isVoid
import javax.lang.model.type.TypeMirror

/**
 * Method binder that delegates to concrete DAO function in a Kotlin interface.
 */
object KotlinDefaultMethodDelegateBinder {
    fun executeAndReturn(
        daoName: ClassName,
        daoImplName: ClassName,
        methodName: String,
        returnType: TypeMirror,
        parameterNames: List<String>,
        scope: CodeGenScope
    ) {
        scope.builder().apply {
            val params: MutableList<Any> = mutableListOf()
            val format = buildString {
                if (!returnType.isVoid()) {
                    append("return ")
                }
                append("$T.$N.$N($T.this")
                params.add(daoName)
                params.add(DEFAULT_IMPLS_CLASS_NAME)
                params.add(methodName)
                params.add(daoImplName)
                parameterNames.forEach {
                    append(", ")
                    append(L)
                    params.add(it)
                }
                append(")")
            }
            addStatement(format, *params.toTypedArray())
        }
    }
}