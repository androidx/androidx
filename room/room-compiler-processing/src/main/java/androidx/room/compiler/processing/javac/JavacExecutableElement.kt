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

import androidx.room.compiler.processing.XExecutableElement
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.javac.kotlin.KmFunctionContainer
import androidx.room.compiler.processing.javac.kotlin.descriptor
import javax.lang.model.element.ExecutableElement

internal abstract class JavacExecutableElement(
    env: JavacProcessingEnv,
    override val element: ExecutableElement
) : JavacElement(env, element), XExecutableElement {
    abstract override val kotlinMetadata: KmFunctionContainer?

    override val jvmDescriptor by lazy {
        element.descriptor(env.delegate)
    }

    abstract override val parameters: List<JavacMethodParameter>

    override val enclosingElement: JavacTypeElement by lazy {
        element.requireEnclosingType(env)
    }

    override val closestMemberContainer: JavacTypeElement
        get() = enclosingElement

    override fun isVarArgs(): Boolean {
        return element.isVarArgs
    }

    override val thrownTypes by lazy {
        element.thrownTypes.map {
            env.wrap<JavacType>(
                typeMirror = it,
                kotlinType = null,
                elementNullability = XNullability.UNKNOWN
            )
        }
    }

    companion object {
        internal const val DEFAULT_IMPLS_CLASS_NAME = "DefaultImpls"
    }
}
