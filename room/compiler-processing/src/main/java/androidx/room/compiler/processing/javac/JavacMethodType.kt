/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.room.compiler.processing.javac

import androidx.room.compiler.processing.XMethodType
import androidx.room.compiler.processing.XSuspendMethodType
import androidx.room.compiler.processing.XType
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.TypeVariableName
import javax.lang.model.type.ExecutableType

internal sealed class JavacMethodType(
    val env: JavacProcessingEnv,
    val element: JavacMethodElement,
    val executableType: ExecutableType
) : XMethodType {
    override val returnType: JavacType by lazy {
        env.wrap<JavacType>(
            typeMirror = executableType.returnType,
            kotlinType = if (element.isSuspendFunction()) {
                // don't use kotlin metadata for suspend return type since it needs to look like
                // java perspective
                null
            } else {
                element.kotlinMetadata?.returnType
            },
            elementNullability = element.element.nullability
        )
    }

    override val typeVariableNames by lazy {
        executableType.typeVariables.map {
            TypeVariableName.get(it)
        }
    }

    override val parameterTypes: List<JavacType> by lazy {
        executableType.parameterTypes.mapIndexed { index, typeMirror ->
            env.wrap<JavacType>(
                typeMirror = typeMirror,
                kotlinType = element.parameters[index].kotlinType,
                elementNullability = element.parameters[index].element.nullability
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is JavacMethodType) return false
        return executableType == other.executableType
    }

    override fun hashCode(): Int {
        return executableType.hashCode()
    }

    override fun toString(): String {
        return executableType.toString()
    }

    private class NormalMethodType(
        env: JavacProcessingEnv,
        element: JavacMethodElement,
        executableType: ExecutableType
    ) : JavacMethodType(
        env = env,
        element = element,
        executableType = executableType
    )

    private class SuspendMethodType(
        env: JavacProcessingEnv,
        element: JavacMethodElement,
        executableType: ExecutableType
    ) : JavacMethodType(
        env = env,
        element = element,
        executableType = executableType
    ),
        XSuspendMethodType {
        override fun getSuspendFunctionReturnType(): XType {
            // the continuation parameter is always the last parameter of a suspend function and it
            // only has one type parameter, e.g Continuation<? super T>
            val typeParam =
                MoreTypes.asDeclared(executableType.parameterTypes.last()).typeArguments.first()
            // kotlin generates ? extends Foo and we want Foo so get the extends bounds
            val bounded = typeParam.extendsBound() ?: typeParam
            return env.wrap<JavacType>(
                typeMirror = bounded,
                // use kotlin metadata here to get the real type information
                kotlinType = element.kotlinMetadata?.returnType,
                elementNullability = element.element.nullability
            )
        }
    }

    companion object {
        fun create(
            env: JavacProcessingEnv,
            element: JavacMethodElement,
            executableType: ExecutableType
        ): JavacMethodType {
            return if (element.isSuspendFunction()) {
                SuspendMethodType(env, element, executableType)
            } else {
                NormalMethodType(env, element, executableType)
            }
        }
    }
}
