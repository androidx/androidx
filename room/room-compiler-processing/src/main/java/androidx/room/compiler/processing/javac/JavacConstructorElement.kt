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
import androidx.room.compiler.processing.XTypeParameterElement
import androidx.room.compiler.processing.javac.kotlin.KmConstructorContainer
import com.google.auto.common.MoreTypes
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement

internal class JavacConstructorElement(
    env: JavacProcessingEnv,
    element: ExecutableElement
) : JavacExecutableElement(env, element),
    XConstructorElement {
    init {
        check(element.kind == ElementKind.CONSTRUCTOR) {
            "Constructor element is constructed with invalid type: $element"
        }
    }

    override fun isSyntheticConstructorForJvmOverloads() = false

    override val name: String
        get() = "<init>"

    override val typeParameters: List<XTypeParameterElement> by lazy {
        element.typeParameters.map {
            // Type parameters are not allowed in Kotlin sources, so if type parameters exist they
            // must have come from Java sources. Thus, there's no kotlin metadata so just use null.
            JavacTypeParameterElement(env, this, it, null)
        }
    }

    override val parameters: List<JavacMethodParameter> by lazy {
        element.parameters.mapIndexed { index, variable ->
            JavacMethodParameter(
                env = env,
                enclosingElement = this,
                element = variable,
                kotlinMetadataFactory = { kotlinMetadata?.parameters?.getOrNull(index) },
                argIndex = index
            )
        }
    }

    override val executableType: XConstructorType by lazy {
        JavacConstructorType(
            env = env,
            element = this,
            executableType = MoreTypes.asExecutable(element.asType())
        )
    }

    override fun asMemberOf(other: XType): XConstructorType {
        return if (other !is JavacDeclaredType || enclosingElement.type.isSameType(other)) {
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

    override val kotlinMetadata: KmConstructorContainer? by lazy {
        (enclosingElement as? JavacTypeElement)?.kotlinMetadata
            ?.getConstructorMetadata(element)
    }
}
