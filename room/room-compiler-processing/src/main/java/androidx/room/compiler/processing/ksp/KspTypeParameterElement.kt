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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.XAnnotated
import androidx.room.compiler.processing.XMemberContainer
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeParameterElement
import androidx.room.compiler.processing.ksp.KspAnnotated.UseSiteFilter.Companion.NO_USE_SITE_OR_FIELD
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.squareup.javapoet.TypeVariableName

internal class KspTypeParameterElement(
    env: KspProcessingEnv,
    override val declaration: KSTypeParameter
) : KspElement(env, declaration),
    XTypeParameterElement,
    XAnnotated by KspAnnotated.create(env, declaration, NO_USE_SITE_OR_FIELD) {

    override val name: String
        get() = declaration.name.asString()

    override val typeVariableName: TypeVariableName by lazy {
        TypeVariableName.get(name, *bounds.map { it.typeName }.toTypedArray())
    }

    override val enclosingElement: KspMemberContainer by lazy {
        declaration.requireEnclosingMemberContainer(env)
    }

    override val bounds: List<XType> by lazy {
        declaration.bounds.map { env.wrap(it, it.resolve()) }.toList().ifEmpty {
            listOf(env.requireType(Any::class).makeNullable())
        }
    }

    override val fallbackLocationText: String
        get() = "${declaration.name} in ${enclosingElement.fallbackLocationText}"

    override val closestMemberContainer: XMemberContainer
        get() = enclosingElement.closestMemberContainer
}
