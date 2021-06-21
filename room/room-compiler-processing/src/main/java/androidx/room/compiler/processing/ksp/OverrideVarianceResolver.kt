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

import androidx.room.compiler.processing.XMethodType
import androidx.room.compiler.processing.XType
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.isOpen
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.symbol.Variance
import com.squareup.javapoet.TypeVariableName

/**
 * When kotlin generates java code, it has some interesting rules on how variance is handled.
 *
 * https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html#variant-generics
 *
 * This helper class applies that to [KspMethodType].
 *
 * Note that, this is only relevant when Room tries to generate overrides. For regular type
 * operations, we prefer the variance declared in Kotlin source.
 */
internal class OverrideVarianceResolver(
    private val env: KspProcessingEnv,
    private val methodType: KspMethodType
) {
    fun resolve(): XMethodType {
        // Look at the true origin to decide whether we need variance resolution or not.
        val parentOrigin = (methodType.origin.enclosingElement as? KspTypeElement)
            ?.declaration?.origin
        if (parentOrigin == Origin.JAVA || parentOrigin == Origin.JAVA_LIB) {
            return methodType
        }
        val overideeElm = methodType.origin.findOverridee()
        return ResolvedMethodType(
            // kotlin does not touch return type
            returnType = methodType.returnType,
            parameterTypes = methodType.parameterTypes.mapIndexed { index, xType ->
                xType.maybeInheritVariance(overideeElm?.parameterTypes?.getOrNull(index))
            },
            typeVariableNames = methodType.typeVariableNames
        )
    }

    private fun XType.maybeInheritVariance(
        overridee: XType?
    ): XType {
        return if (this is KspType) {
            this.inheritVariance(overridee as? KspType)
        } else {
            this
        }
    }

    private fun KspType.inheritVariance(overridee: KspType?): KspType {
        return env.wrap(
            ksType = ksType.inheritVariance(overridee?.ksType),
            allowPrimitives = this is KspPrimitiveType || (this is KspVoidType && !this.boxed)
        )
    }

    /**
     * Finds the method type for the method element that was overridden by this method element.
     */
    private fun KspMethodElement.findOverridee(): KspMethodType? {
        // now find out if this is overriding a method
        val funDeclaration = declaration
        val declaredIn = funDeclaration.closestClassDeclaration() ?: return null
        if (declaredIn == containing.declaration) {
            // if declared in the same class, skip
            return null
        }

        // it is declared in a super type, get that
        val overridee = funDeclaration.findOverridee() as? KSFunctionDeclaration
        // in kotlin, a method cannot override a property and we get to this code only for kotlin,
        // hence we only check for overridee if it is a KSFunction. Override is KSDeclaration by
        // default to handle cases when a Java method overrides a kotlin property
        val overrideeElm = KspMethodElement.create(
            env = env,
            containing = env.wrapClassDeclaration(declaredIn),
            declaration = overridee ?: funDeclaration
        )
        val containing = overrideeElm.enclosingElement.type ?: return null
        return KspMethodType.create(
            env = env,
            origin = overrideeElm,
            containing = containing
        )
    }

    /**
     * Update the variance of the arguments of this type based on the types declaration.
     *
     * For instance, in List<Foo>, it actually inherits the `out` variance from `List`.
     */
    private fun KSType.inheritVariance(
        overridee: KSType?
    ): KSType {
        if (arguments.isEmpty()) return this
        // need to swap arguments with the variance from declaration
        val newArguments = arguments.mapIndexed { index, typeArg ->
            val param = declaration.typeParameters.getOrNull(index)
            val overrideeArg = overridee?.arguments?.getOrNull(index)
            typeArg.inheritVariance(overrideeArg, param)
        }
        return this.replace(newArguments)
    }

    private fun KSTypeReference.inheritVariance(
        overridee: KSTypeReference?
    ): KSTypeReference {
        return resolve()
            .inheritVariance(overridee = overridee?.resolve())
            .createTypeReference()
    }

    private fun KSTypeArgument.inheritVariance(
        overridee: KSTypeArgument?,
        param: KSTypeParameter?
    ): KSTypeArgument {
        if (param == null) {
            return this
        }
        val myTypeRef = type ?: return this

        if (variance != Variance.INVARIANT) {
            return env.resolver.getTypeArgument(
                typeRef = myTypeRef.inheritVariance(overridee?.type),
                variance = variance
            )
        }
        if (overridee != null) {
            // get it from overridee
            return env.resolver.getTypeArgument(
                typeRef = myTypeRef.inheritVariance(overridee.type),
                variance = if (overridee.variance == Variance.STAR) {
                    Variance.COVARIANT
                } else {
                    overridee.variance
                }
            )
        }
        // Now we need to guess from this type. If the type is final, it does not inherit unless
        // the parameter is CONTRAVARIANT (`in`).
        val myType = myTypeRef.resolve()
        val shouldInherit = param.variance == Variance.CONTRAVARIANT ||
            when (val decl = myType.declaration) {
                is KSClassDeclaration -> {
                    decl.isOpen() ||
                        decl.classKind == ClassKind.ENUM_CLASS ||
                        decl.classKind == ClassKind.OBJECT
                }
                else -> true
            }
        return if (shouldInherit) {
            env.resolver.getTypeArgument(
                typeRef = myTypeRef.inheritVariance(overridee = null),
                variance = param.variance
            )
        } else {
            env.resolver.getTypeArgument(
                typeRef = myTypeRef.inheritVariance(overridee = null),
                variance = variance
            )
        }
    }

    /**
     * [XMethodType] implementation where variance of types are resolved.
     */
    private class ResolvedMethodType(
        override val returnType: XType,
        override val parameterTypes: List<XType>,
        override val typeVariableNames: List<TypeVariableName>
    ) : XMethodType
}
