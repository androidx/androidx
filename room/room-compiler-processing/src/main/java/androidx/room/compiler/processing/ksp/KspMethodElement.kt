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

import androidx.room.compiler.processing.XEnumTypeElement
import androidx.room.compiler.processing.XExecutableParameterElement
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XMethodType
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticContinuationParameterElement
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticReceiverParameterElement
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier

internal sealed class KspMethodElement(
    env: KspProcessingEnv,
    containing: KspMemberContainer,
    declaration: KSFunctionDeclaration
) : KspExecutableElement(
    env = env,
    containing = containing,
    declaration = declaration
),
    XMethodElement {

    override val name: String
        get() = declaration.simpleName.asString()

    @OptIn(KspExperimental::class)
    override val jvmName: String by lazy {
        val jvmName = runCatching {
            // see https://github.com/google/ksp/issues/716
            env.resolver.getJvmName(declaration)
        }
        jvmName.getOrNull() ?: declaration.simpleName.asString()
    }

    override val parameters: List<XExecutableParameterElement> by lazy {
        buildList<XExecutableParameterElement> {
            val extensionReceiver = declaration.extensionReceiver
            if (extensionReceiver != null) {
                // Synthesize the receiver parameter to be consistent with KAPT
                add(
                    KspSyntheticReceiverParameterElement(
                        env = env,
                        enclosingElement = this@KspMethodElement,
                        receiverType = extensionReceiver,
                    )
                )
            }
            addAll(
                declaration.parameters.mapIndexed { index, param ->
                    KspExecutableParameterElement(
                        env = env,
                        enclosingElement = this@KspMethodElement,
                        parameter = param,
                        parameterIndex = index
                    )
                }
            )
        }
    }

    override val executableType: XMethodType by lazy {
        KspMethodType.create(
            env = env,
            origin = this,
            containing = this.containing.type
        )
    }

    /**
     * The method type for the declaration if it is inherited from a super.
     * If this method is declared in the containing class (or in a file), it will be null.
     */
    val declarationMethodType: XMethodType? by lazy {
        val declaredIn = declaration.closestClassDeclaration()
        if (declaredIn == null || declaredIn == containing.declaration) {
            null
        } else {
            create(
                env = env,
                containing = env.wrapClassDeclaration(declaredIn),
                declaration = declaration
            ).executableType
        }
    }

    override val enclosingElement: KspMemberContainer
        // KSFunctionDeclarationJavaImpl.parent returns null for generated static enum functions
        // `values` and `valueOf` in Java source(https://github.com/google/ksp/issues/816).
        // To bypass this we use `containing` for these functions.
        get() = if (containing is XEnumTypeElement && (name == "values" || name == "valueOf")) {
            containing
        } else {
            super.enclosingElement
        }

    override fun isJavaDefault(): Boolean {
        return declaration.modifiers.contains(Modifier.JAVA_DEFAULT) ||
            declaration.hasJvmDefaultAnnotation()
    }

    override fun asMemberOf(other: XType): XMethodType {
        check(other is KspType)
        return KspMethodType.create(
            env = env,
            origin = this,
            containing = other
        )
    }

    override fun hasKotlinDefaultImpl(): Boolean {
        val parentDeclaration = declaration.parentDeclaration
        // if parent declaration is an interface and we are not marked as an abstract method nor
        // we are a private function, then we should have a default implementation
        return parentDeclaration is KSClassDeclaration &&
            parentDeclaration.classKind == ClassKind.INTERFACE &&
            !declaration.isAbstract &&
            !isPrivate()
    }

    override fun isExtensionFunction() = declaration.extensionReceiver != null

    override fun overrides(other: XMethodElement, owner: XTypeElement): Boolean {
        return env.resolver.overrides(this, other)
    }

    override fun copyTo(newContainer: XTypeElement): KspMethodElement {
        check(newContainer is KspTypeElement)
        return create(
            env = env,
            containing = newContainer,
            declaration = declaration
        )
    }

    private class KspNormalMethodElement(
        env: KspProcessingEnv,
        containing: KspMemberContainer,
        declaration: KSFunctionDeclaration
    ) : KspMethodElement(
        env, containing, declaration
    ) {
        override val returnType: XType by lazy {
            declaration.returnKspType(
                env = env,
                containing = containing.type
            )
        }
        override fun isSuspendFunction() = false
    }

    private class KspSuspendMethodElement(
        env: KspProcessingEnv,
        containing: KspMemberContainer,
        declaration: KSFunctionDeclaration
    ) : KspMethodElement(
        env, containing, declaration
    ) {
        override fun isSuspendFunction() = true

        override val returnType: XType by lazy {
            env.wrap(
                ksType = env.resolver.builtIns.anyType.makeNullable(),
                allowPrimitives = false
            )
        }

        override val parameters: List<XExecutableParameterElement>
            get() = super.parameters + KspSyntheticContinuationParameterElement(
                env = env,
                enclosingElement = this
            )
    }

    companion object {
        fun create(
            env: KspProcessingEnv,
            containing: KspMemberContainer,
            declaration: KSFunctionDeclaration
        ): KspMethodElement {
            return if (declaration.modifiers.contains(Modifier.SUSPEND)) {
                KspSuspendMethodElement(env, containing, declaration)
            } else {
                KspNormalMethodElement(env, containing, declaration)
            }
        }
    }
}