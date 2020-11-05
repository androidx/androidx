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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.XMethodType
import androidx.room.compiler.processing.XSuspendMethodType
import androidx.room.compiler.processing.XType
import com.squareup.javapoet.TypeVariableName

internal sealed class KspMethodType(
    val env: KspProcessingEnv,
    val origin: KspMethodElement,
    val containing: KspDeclaredType
) : XMethodType {
    override val parameterTypes: List<XType> by lazy {
        origin.parameters.map {
            it.asMemberOf(containing)
        }
    }

    override val typeVariableNames: List<TypeVariableName> by lazy {
        origin.declaration.typeParameters.map {
            TypeVariableName.get(
                it.name.asString(),
                *(
                    it.bounds.map {
                        it.typeName()
                    }.toTypedArray()
                    )
            )
        }
    }

    private class KspNormalMethodType(
        env: KspProcessingEnv,
        origin: KspMethodElement,
        containing: KspDeclaredType
    ) : KspMethodType(env, origin, containing) {
        override val returnType: XType by lazy {
            env.wrap(
                origin.declaration.returnTypeAsMemberOf(
                    resolver = env.resolver,
                    ksType = containing.ksType
                )
            )
        }
    }

    private class KspSuspendMethodType(
        env: KspProcessingEnv,
        origin: KspMethodElement,
        containing: KspDeclaredType
    ) : KspMethodType(env, origin, containing), XSuspendMethodType {
        override val returnType: XType
            // suspend functions always return Any?, no need to call asMemberOf
            get() = origin.returnType

        override fun getSuspendFunctionReturnType(): XType {
            return env.wrap(
                origin.declaration.returnTypeAsMemberOf(
                    resolver = env.resolver,
                    ksType = containing.ksType
                )
            )
        }
    }

    companion object {
        fun create(
            env: KspProcessingEnv,
            origin: KspMethodElement,
            containing: KspDeclaredType
        ) = if (origin.isSuspendFunction()) {
            KspSuspendMethodType(env, origin, containing)
        } else {
            KspNormalMethodType(env, origin, containing)
        }
    }
}
