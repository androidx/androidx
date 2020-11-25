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

import androidx.room.compiler.processing.XAnnotated
import androidx.room.compiler.processing.XDeclaredType
import androidx.room.compiler.processing.XFieldElement
import androidx.room.compiler.processing.XHasModifiers
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.ksp.KspAnnotated.UseSiteFilter.Companion.FIELD
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

internal class KspFieldElement(
    env: KspProcessingEnv,
    override val declaration: KSPropertyDeclaration,
    val containing: KspTypeElement
) : KspElement(env, declaration),
    XFieldElement,
    XHasModifiers by KspHasModifiers.create(declaration),
    XAnnotated by KspAnnotated.create(env, declaration, FIELD) {

    override val equalityItems: Array<out Any?> by lazy {
        arrayOf(declaration, containing)
    }

    override val enclosingTypeElement: XTypeElement by lazy {
        declaration.requireEnclosingTypeElement(env)
    }

    override val name: String by lazy {
        declaration.simpleName.asString()
    }

    override val type: KspType by lazy {
        env.wrap(
            originatingReference = declaration.type,
            ksType = declaration.typeAsMemberOf(env.resolver, containing.type.ksType)
        )
    }

    override fun asMemberOf(other: XDeclaredType): XType {
        if (containing.type.isSameType(other)) {
            return type
        }
        check(other is KspType)
        val asMember = declaration.typeAsMemberOf(env.resolver, other.ksType)
        return env.wrap(
            originatingReference = declaration.type,
            ksType = asMember
        )
    }

    fun copyTo(newContaining: KspTypeElement) = KspFieldElement(
        env = env,
        declaration = declaration,
        containing = newContaining
    )
}
