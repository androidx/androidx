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

import androidx.room.compiler.processing.XExecutableParameterElement
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XMethodType
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticContinuationParameterElement
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticReceiverParameterElement
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier

internal sealed class KspMethodElement(
    env: KspProcessingEnv,
    declaration: KSFunctionDeclaration
) : KspExecutableElement(env, declaration), XMethodElement {

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
        buildList {
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
            containing = this.enclosingElement.type
        )
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

    override fun isKotlinPropertyMethod() = false

    abstract override val returnType: KspType

    private class KspNormalMethodElement(
        env: KspProcessingEnv,
        declaration: KSFunctionDeclaration
    ) : KspMethodElement(env, declaration) {
        override val returnType: KspType by lazy {
            declaration.returnKspType(
                env = env,
                containing = enclosingElement.type
            ).copyWithScope(
                KSTypeVarianceResolverScope.MethodReturnType(
                    method = this,
                    asMemberOf = enclosingElement.type,
                )
            )
        }
        override fun isSuspendFunction() = false
    }

    private class KspSuspendMethodElement(
        env: KspProcessingEnv,
        declaration: KSFunctionDeclaration
    ) : KspMethodElement(env, declaration) {
        override fun isSuspendFunction() = true

        override val returnType: KspType by lazy {
            env.wrap(
                ksType = env.resolver.builtIns.anyType.makeNullable(),
                allowPrimitives = false
            ).copyWithScope(
                KSTypeVarianceResolverScope.MethodReturnType(
                    method = this,
                    asMemberOf = enclosingElement.type
                )
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
            declaration: KSFunctionDeclaration
        ): KspMethodElement {
            return if (declaration.modifiers.contains(Modifier.SUSPEND)) {
                KspSuspendMethodElement(env, declaration)
            } else {
                KspNormalMethodElement(env, declaration)
            }
        }
    }
}