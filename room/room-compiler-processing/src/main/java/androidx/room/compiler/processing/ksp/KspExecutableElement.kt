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
import androidx.room.compiler.processing.XExecutableElement
import androidx.room.compiler.processing.XHasModifiers
import androidx.room.compiler.processing.XMemberContainer
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeParameterElement
import androidx.room.compiler.processing.ksp.KspAnnotated.UseSiteFilter.Companion.NO_USE_SITE
import androidx.room.compiler.processing.util.ISSUE_TRACKER_LINK
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier

internal abstract class KspExecutableElement(
    env: KspProcessingEnv,
    override val declaration: KSFunctionDeclaration
) : KspElement(env, declaration),
    XExecutableElement,
    XHasModifiers by KspHasModifiers.create(declaration),
    XAnnotated by KspAnnotated.create(
        env = env,
        delegate = declaration,
        filter = NO_USE_SITE
    ) {

    override val enclosingElement: KspMemberContainer by lazy {
        declaration.requireEnclosingMemberContainer(env)
    }

    override val closestMemberContainer: XMemberContainer
        get() = enclosingElement

    override val typeParameters: List<XTypeParameterElement> by lazy {
        declaration.typeParameters.map { KspTypeParameterElement(env, it) }
    }

    @OptIn(KspExperimental::class)
    override val thrownTypes: List<XType> by lazy {
        env.resolver.getJvmCheckedException(declaration).map {
            env.wrap(
                ksType = it,
                allowPrimitives = false
            )
        }.toList()
    }

    override fun isVarArgs(): Boolean {
        // in java, only the last argument can be a vararg so for suspend functions, it is never
        // a vararg function. this would change if room generated kotlin code
        return !declaration.modifiers.contains(Modifier.SUSPEND) &&
            declaration.parameters.any {
                it.isVararg
            }
    }

    companion object {
        fun create(
            env: KspProcessingEnv,
            declaration: KSFunctionDeclaration
        ): KspExecutableElement {
            val enclosingContainer = declaration.findEnclosingMemberContainer(env)

            checkNotNull(enclosingContainer) {
                """
                Couldn't find the container element for $declaration.
                Please file a bug at $ISSUE_TRACKER_LINK.
                """
            }

            return when {
                declaration.isConstructor() -> KspConstructorElement(env, declaration)
                else -> KspMethodElement.create(env, declaration)
            }
        }
    }
}
