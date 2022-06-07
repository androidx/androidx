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
import com.google.devtools.ksp.symbol.Modifier
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
    /**
     * @param ksType The Kotlin type on which the variance will be applied
     * @param wildcardMode `wildcardMode` defines the default behavior of whether to inherit
     *        variance or not. This depends on the existence of `SuppressWildcard` annotations or
     *        the type's location (e.g. whether it is a method parameter or return type)
     * @param declarationType If a type is resolved via inheritance where it is not explicitly
     *        declared in its container, this value should have its original type from the
     *        declaration site. e.g. if you have `val BaseClass.x : T`, and the ksType is the
     *        type of `x` from `SubClass: BaseClass<String>`, `declarationType` would be `T` whereas
     *        the `ksType` is `String`. If the `ksType` is from the original declaration, this value
     *        should be `null`.
     */
    fun applyTypeVariance(
        ksType: KSType,
        wildcardMode: WildcardMode,
        declarationType: KSType?
    ): KSType = ksType.inheritVariance(declarationType, wildcardMode, ReferenceStack())

    /**
     * Update the variance of the arguments of this type based on the types declaration.
     *
     * For instance, in List<Foo>, it actually inherits the `out` variance from `List`.
     */
    private fun KSType.inheritVariance(
        declarationType: KSType?,
        wildcardMode: WildcardMode,
        referenceStack: ReferenceStack
    ): KSType {
        if (arguments.isEmpty()) return this
        return referenceStack.withReference(this) {
            // arrays don't inherit variance unless it is in an inherited method
            if (this.declaration.qualifiedName?.asString() == KOTLIN_ARRAY_Q_NAME &&
                declarationType == null
            ) {
                return@withReference this
            }

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
                typeArg.inheritVariance(declarationArg, argWildcardMode, param, referenceStack)
            }
            this.replace(newArguments)
        }
    }

    private fun KSTypeArgument.inheritVariance(
        declarationType: KSTypeArgument?,
        wildcardMode: WildcardMode,
        param: KSTypeParameter?,
        referenceStack: ReferenceStack
    ): KSTypeArgument {
        if (param == null) {
            return this
        }
        val myTypeRef = type ?: return this

        val myType = myTypeRef.resolve()

        if (referenceStack.contains(myType)) {
            // self referencing type
            return this
        }
        if (variance != Variance.INVARIANT) {
            return resolver.getTypeArgument(
                typeRef = myType.inheritVariance(
                    declarationType?.type?.resolve(),
                    wildcardMode,
                    referenceStack
                ).createTypeReference(),
                variance = variance
            )
        }

        // Now we need to guess from this type. If the type is final, it does not inherit unless
        // the parameter is CONTRAVARIANT (`in`).
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
                    // if there is a declaration type, that means we are being resolved for an
                    // inherited method/property; hence we should use the variance in the
                    // declaration
                    true
                } else {
                    param.variance == Variance.CONTRAVARIANT ||
                        when (val decl = myType.declaration) {
                            is KSClassDeclaration -> {
                                decl.isOpen() || decl.classKind == ClassKind.ENUM_CLASS ||
                                    decl.modifiers.contains(Modifier.SEALED)
                            }
                            else -> true
                        }
                }
            }
        }
        val newVariance = if (declarationType?.variance == Variance.STAR) {
            Variance.COVARIANT
        } else if (declarationType?.type?.resolve() is KSTypeParameter) {
            // fallback to the parameter variance if we are swapping a type parameter type
            param.variance
        } else {
            declarationType?.variance
        } ?: param.variance
        return if (shouldInherit) {
            resolver.getTypeArgument(
                typeRef = myType.inheritVariance(
                    declarationType?.type?.resolve(),
                    wildcardMode,
                    referenceStack
                ).createTypeReference(),
                variance = newVariance
            )
        } else {
            resolver.getTypeArgument(
                typeRef = myType.inheritVariance(null, wildcardMode, referenceStack)
                    .createTypeReference(),
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

/**
 * Inheriting variance for self referencing types (e.g. Foo<T : Foo>) could go into an infinite
 * loop. To avoid that issue, every time we visit a type, we keep it in the reference stack and
 * if a type argument resolves to it, it will stop recursion.
 */
private class ReferenceStack {
    @Suppress("PropertyName")
    val _queue = ArrayDeque<KSType>()

    fun contains(ksType: KSType) = _queue.contains(ksType)

    inline fun <T> withReference(
        ksType: KSType,
        crossinline block: () -> T
    ): T {
        return try {
            _queue.addLast(ksType)
            block()
        } finally {
            _queue.removeLast()
        }
    }
}