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

import androidx.room.compiler.processing.XExecutableElement
import androidx.room.compiler.processing.XHasModifiers
import androidx.room.compiler.processing.XMemberContainer
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeParameterElement
import androidx.room.compiler.processing.util.ISSUE_TRACKER_LINK
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

internal abstract class KspExecutableElement(
    env: KspProcessingEnv,
    override val declaration: KSFunctionDeclaration
) : KspElement(env, declaration),
    XExecutableElement,
    XHasModifiers by KspHasModifiers.create(declaration) {

    override val jvmDescriptor: String
        get() = this.jvmDescriptor()

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
        // TODO(b/254135327): Revisit with the introduction of a target language.
        if (this is KspMethodElement && this.isSuspendFunction()) {
            return false
        }
        return declaration.parameters.lastOrNull()?.isVararg ?: false
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
