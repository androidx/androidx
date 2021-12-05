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

package androidx.room.compiler.processing.javac

import androidx.room.compiler.processing.XConstructorElement
import androidx.room.compiler.processing.XConstructorType
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.javac.kotlin.KmConstructor
import com.google.auto.common.MoreTypes
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement

internal class JavacConstructorElement(
    env: JavacProcessingEnv,
    containing: JavacTypeElement,
    element: ExecutableElement
) : JavacExecutableElement(
    env,
    containing,
    element
),
    XConstructorElement {
    init {
        check(element.kind == ElementKind.CONSTRUCTOR) {
            "Constructor element is constructed with invalid type: $element"
        }
    }

    override val enclosingElement: XTypeElement by lazy {
        element.requireEnclosingType(env)
    }

    override val parameters: List<JavacMethodParameter> by lazy {
        element.parameters.mapIndexed { index, variable ->
            JavacMethodParameter(
                env = env,
                enclosingMethodElement = this,
                containing = containing,
                element = variable,
                kotlinMetadataFactory = { kotlinMetadata?.parameters?.getOrNull(index) },
                argIndex = index
            )
        }
    }

    override val executableType: XConstructorType by lazy {
        val asMemberOf = env.typeUtils.asMemberOf(containing.type.typeMirror, element)
        JavacConstructorType(
            env = env,
            element = this,
            executableType = MoreTypes.asExecutable(asMemberOf)
        )
    }

    override fun asMemberOf(other: XType): XConstructorType {
        return if (other !is JavacDeclaredType || containing.type.isSameType(other)) {
            executableType
        } else {
            val asMemberOf = env.typeUtils.asMemberOf(other.typeMirror, element)
            JavacConstructorType(
                env = env,
                element = this,
                executableType = MoreTypes.asExecutable(asMemberOf)
            )
        }
    }

    override val kotlinMetadata: KmConstructor? by lazy {
        (enclosingElement as? JavacTypeElement)?.kotlinMetadata?.getConstructorMetadata(element)
    }
}
