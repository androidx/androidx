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

package androidx.room.compiler.processing.javac

import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XMemberContainer
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeParameterElement
import androidx.room.compiler.processing.javac.kotlin.KmTypeParameterContainer
import com.squareup.javapoet.TypeVariableName
import javax.lang.model.element.TypeParameterElement

internal class JavacTypeParameterElement(
    env: JavacProcessingEnv,
    override val enclosingElement: XElement,
    override val element: TypeParameterElement,
    override val kotlinMetadata: KmTypeParameterContainer?,
) : JavacElement(env, element), XTypeParameterElement {

    override val name: String
        get() = element.simpleName.toString()

    override val typeVariableName: TypeVariableName by lazy {
        TypeVariableName.get(name, *bounds.map { it.typeName }.toTypedArray())
    }

    override val bounds: List<XType> by lazy {
        element.bounds.mapIndexed { i, bound ->
            env.wrap(bound, kotlinMetadata?.upperBounds?.getOrNull(i), XNullability.UNKNOWN)
        }
    }

    override val fallbackLocationText: String
        get() = element.simpleName.toString()

    override val closestMemberContainer: XMemberContainer
        get() = enclosingElement.closestMemberContainer
}
