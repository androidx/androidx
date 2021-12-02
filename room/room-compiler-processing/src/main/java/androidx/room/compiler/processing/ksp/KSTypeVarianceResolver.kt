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

import androidx.room.compiler.processing.ksp.KspArrayType.Companion.KOTLIN_ARRAY_Q_NAME
import com.google.devtools.ksp.isOpen
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Variance

/**
 * When kotlin generates java code, it has some interesting rules on how variance is handled.
 *
 * https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html#variant-generics
 *
 * This helper class applies that to [KspMethodType].
 *
 * Note that, this is only relevant when Room tries to generate overrides. For regular type
 * operations, we prefer the variance declared in Kotlin source.
 *
 * Note that this class will eventually be replaced when KSP provides the proper API:
 * https://github.com/google/ksp/issues/717
 *
 * Until then, the logic here is mostly reverse engineered from KAPT source code +
 * KspTypeNamesGoldenTest ¯\_(ツ)_/¯
 */
internal class KSTypeVarianceResolver(
    private val resolver: Resolver
) {
    fun applyTypeVariance(
        ksType: KSType,
        wildcardMode: WildcardMode,
        declarationType: KSType?
    ): KSType = ksType.inheritVariance(declarationType, wildcardMode)

    /**
     * Update the variance of the arguments of this type based on the types declaration.
     *
     * For instance, in List<Foo>, it actually inherits the `out` variance from `List`.
     */
    private fun KSType.inheritVariance(
        declarationType: KSType?,
        wildcardMode: WildcardMode,
    ): KSType {
        if (arguments.isEmpty()) return this
        // arrays don't inherit variance unless it is in an inherited method
        if (this.declaration.qualifiedName?.asString() == KOTLIN_ARRAY_Q_NAME &&
            declarationType == null
        ) return this

        // if we have type arguments but the declarationType doesn't, we should consider it like
        // star projection.
        // This happens when a given List<X> overrides T. In this case, we need to force X's
        // wildcards
        val starProject = declarationType != null && declarationType.arguments.isEmpty()

        // need to swap arguments with the variance from declaration
        val newArguments = arguments.mapIndexed { index, typeArg ->
            val param = declaration.typeParameters.getOrNull(index)
            val declarationArg = declarationType?.arguments?.getOrNull(index)
            val argWildcardMode = if (starProject) {
                WildcardMode.FORCED
            } else {
                wildcardMode
            }
            typeArg.inheritVariance(declarationArg, argWildcardMode, param)
        }
        return this.replace(newArguments)
    }

    private fun KSTypeReference.inheritVariance(
        declarationType: KSTypeReference?,
        wildcardMode: WildcardMode
    ): KSTypeReference {
        return resolve()
            .inheritVariance(declarationType = declarationType?.resolve(), wildcardMode)
            .createTypeReference()
    }

    private fun KSTypeArgument.inheritVariance(
        declarationType: KSTypeArgument?,
        wildcardMode: WildcardMode,
        param: KSTypeParameter?
    ): KSTypeArgument {
        if (param == null) {
            return this
        }
        val myTypeRef = type ?: return this

        if (variance != Variance.INVARIANT) {
            return resolver.getTypeArgument(
                typeRef = myTypeRef.inheritVariance(declarationType?.type, wildcardMode),
                variance = variance
            )
        }

        // Now we need to guess from this type. If the type is final, it does not inherit unless
        // the parameter is CONTRAVARIANT (`in`).
        val myType = myTypeRef.resolve()
        val shouldInherit = when {
            hasJvmWildcardAnnotation() -> {
                // we actually don't need to check for wildcard annotation here as the TypeName
                // conversion will do it for the general case. Nevertheless, we check for it for
                // consistency
                true
            }
            wildcardMode == WildcardMode.SUPPRESSED -> false
            wildcardMode == WildcardMode.FORCED -> true
            hasSuppressWildcardsAnnotationInHierarchy() -> false
            else -> {
                if (declarationType != null) {
                    true
                } else {
                    param.variance == Variance.CONTRAVARIANT ||
                        when (val decl = myType.declaration) {
                            is KSClassDeclaration -> {
                                decl.isOpen() || decl.classKind == ClassKind.ENUM_CLASS
                            }
                            else -> true
                        }
                }
            }
        }
        val newVariance = if (declarationType?.variance == Variance.STAR) {
            Variance.COVARIANT
        } else if (declarationType?.variance == Variance.INVARIANT) {
            param.variance
        } else {
            declarationType?.variance
        } ?: param.variance
        return if (shouldInherit) {
            resolver.getTypeArgument(
                typeRef = myTypeRef.inheritVariance(declarationType?.type, wildcardMode),
                variance = newVariance
            )
        } else {
            resolver.getTypeArgument(
                typeRef = myTypeRef.inheritVariance(null, wildcardMode),
                variance = variance
            )
        }
    }

    enum class WildcardMode {
        /**
         * Force wildcard inheritance that is commonly used when there is star projection involved
         */
        FORCED,

        /**
         * Apply wildcard inheritance when necessary.
         */
        PREFERRED,

        /**
         * Apply wildcard inheritance only if it is explicitly stated with JvmWildcards annotation.
         */
        SUPPRESSED
    }
}
