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

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isOpen
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
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
internal class KSTypeVarianceResolver(private val resolver: Resolver) {
    /**
     * @param type The Kotlin type declared by the user on which the variance will be applied.
     * @param scope The [KSTypeVarianceResolverScope] associated with the given type.
     */
    @OptIn(KspExperimental::class)
    fun applyTypeVariance(type: KSType, scope: KSTypeVarianceResolverScope): KSType {
        if (type.isError ||
            type.arguments.isEmpty() ||
            resolver.isJavaRawType(type) ||
            !scope.needsWildcardResolution) {
            // There's nothing to resolve in this case, so just return the original type.
            return type
        }

        val resolvedType = if (hasTypeVariables(scope.declarationType())) {
            // If the associated declared type contains type variables that were resolved, e.g.
            // using "asMemberOf", then it has special rules about how to resolve the types.
            getJavaWildcardWithTypeVariables(
                type = type,
                declarationType = getJavaWildcard(scope.declarationType(), scope),
                scope = scope,
            )
        } else {
            getJavaWildcard(type, scope)
        }

        // As a final pass, we apply variance from any @JvmSuppressWildcards or @JvmWildcard
        // annotations on the resolved type.
        return applyJvmWildcardAnnotations(resolvedType)
    }

    private fun hasTypeVariables(
        type: KSType?,
        stack: ReferenceStack = ReferenceStack()
    ): Boolean {
        if (type == null || type.isError || stack.queue.contains(type)) {
            return false
        }
        return stack.withReference(type) {
            type.isTypeParameter() ||
                type.arguments.any { hasTypeVariables(it.type?.resolve(), stack) }
        }
    }

    private fun getJavaWildcard(
        type: KSType,
        scope: KSTypeVarianceResolverScope,
        typeStack: ReferenceStack = ReferenceStack(),
        typeArgStack: List<KSTypeArgument> = emptyList(),
        typeParamStack: List<KSTypeParameter> = emptyList(),
    ): KSType {
        if (type.isError || typeStack.queue.contains(type)) {
            return type
        }
        if (type.declaration is KSTypeAlias) {
            return getJavaWildcard(
                type = (type.declaration as KSTypeAlias).type.resolve(),
                scope = scope,
                typeStack = typeStack,
                typeArgStack = typeArgStack,
                typeParamStack = typeParamStack,
            )
        }
        return typeStack.withReference(type) {
            val resolvedTypeArgs =
                type.arguments.indices.map { i ->
                    getJavaWildcard(
                        typeArg = type.arguments[i],
                        typeParam = type.declaration.typeParameters[i],
                        scope = scope,
                        typeStack = typeStack,
                        typeArgStack = typeArgStack,
                        typeParamStack = typeParamStack,
                    )
                }
            type.replace(resolvedTypeArgs)
        }
    }

    private fun getJavaWildcard(
        typeArg: KSTypeArgument,
        typeParam: KSTypeParameter,
        scope: KSTypeVarianceResolverScope,
        typeStack: ReferenceStack,
        typeArgStack: List<KSTypeArgument>,
        typeParamStack: List<KSTypeParameter>,
    ): KSTypeArgument {
        val type = typeArg.type?.resolve()
        if (
            type == null ||
            type.isError ||
            typeArg.variance == Variance.STAR ||
            typeStack.queue.contains(type)
        ) {
            return typeArg
        }
        val resolvedType = getJavaWildcard(
            type = type,
            scope = scope,
            typeStack = typeStack,
            typeArgStack = typeArgStack + typeArg,
            typeParamStack = typeParamStack + typeParam
        )
        fun inheritDeclarationSiteVariance(): Boolean {
            // Before we check the current variance, we need to check the previous variance in the
            // stack to see if they allow us to inherit the current variance, and that logic differs
            // depending on the scope.
            if (scope.isValOrReturnType()) {
                // For val and return type scopes, we don't use the declaration-site variance if
                // none of variances in the stack are contravariant.
                if (typeParamStack.indices.none { i ->
                        (typeParamStack[i].variance == Variance.CONTRAVARIANT ||
                            typeArgStack[i].variance == Variance.CONTRAVARIANT) &&
                        // The declaration and use site variance is ignored when using @JvmWildcard
                        // explicitly on a type.
                        !typeArgStack[i].hasJvmWildcardAnnotation()
                }) {
                    return false
                }
            } else {
                // For method parameters and var type scopes, we don't use the declaration-site
                // variance if the last variance in the declaration-site stack was invariant and
                // the last variance in the use-site stack was not contravariant.
                if (typeParamStack.lastOrNull()?.variance == Variance.INVARIANT &&
                    typeArgStack.lastOrNull()?.variance != Variance.CONTRAVARIANT) {
                    return false
                }
            }
            return when (typeParam.variance) {
                // If the current declaration-site variance is invariant then don't inherit it.
                Variance.INVARIANT -> false
                // If the current declaration-site variance is contravariant then inherit it.
                Variance.CONTRAVARIANT -> true
                // If the current declaration-site variance is covariant then inherit it unless
                // it's a final class (excluding enum/sealed classes).
                Variance.COVARIANT -> when (val declaration = type.declaration) {
                    is KSClassDeclaration -> declaration.isOpen() ||
                        declaration.classKind == ClassKind.ENUM_CLASS ||
                        declaration.modifiers.contains(Modifier.SEALED) ||
                        // For non-open/enum/sealed classes we may still decided to use the
                        // declaration-site variance based on if any of the type arguments in the
                        // resolved type has variance and the use-site variance is not equal to
                        // covariant/contravariant.
                        resolvedType.arguments.indices.any { i ->
                            resolvedType.arguments[i].variance != Variance.INVARIANT &&
                                type.arguments[i].variance != Variance.COVARIANT &&
                                type.arguments[i].variance != Variance.CONTRAVARIANT
                        }
                    else -> true
                }
                Variance.STAR -> error {
                    "Declaration site variance was not expected to contain STAR: $typeParam."
                }
            }
        }
        val resolvedVariance = if (inheritDeclarationSiteVariance()) {
            typeParam.variance
        } else if (typeParam.variance == typeArg.variance) {
            // If we're not applying the declaration-site variance, and the use-site variance is the
            // same as the declaration-site variance then we don't include the use-site variance in
            // the jvm type either.
            Variance.INVARIANT
        } else {
            typeArg.variance
        }
        return createTypeArgument(resolvedType, resolvedVariance)
    }

    private fun getJavaWildcardWithTypeVariables(
        type: KSType,
        declarationType: KSType? = null,
        scope: KSTypeVarianceResolverScope,
        typeStack: ReferenceStack = ReferenceStack(),
    ): KSType {
        if (type.isError || typeStack.queue.contains(type)) {
            return type
        }
        return typeStack.withReference(type) {
            val resolvedTypeArgs =
                if (declarationType != null && !declarationType.isTypeParameter()) {
                    declarationType.arguments.indices.map { i ->
                        getJavaWildcardWithTypeVariablesForOuterType(
                            typeArg = type.arguments[i],
                            declarationTypeArg = declarationType.arguments[i],
                            scope = scope,
                            typeStack = typeStack,
                        )
                    }
                } else {
                    type.arguments.indices.map { i ->
                        getJavaWildcardWithTypeVariablesForInnerType(
                            typeArg = type.arguments[i],
                            declarationTypeParameter = type.declaration.typeParameters[i],
                            scope = scope,
                            typeStack = typeStack,
                        )
                    }
                }
            type.replace(resolvedTypeArgs)
        }
    }

    private fun getJavaWildcardWithTypeVariablesForInnerType(
        typeArg: KSTypeArgument,
        declarationTypeParameter: KSTypeParameter,
        scope: KSTypeVarianceResolverScope,
        typeStack: ReferenceStack,
    ): KSTypeArgument {
        val type = typeArg.type?.resolve()
        if (
            type == null ||
            type.isError ||
            typeArg.variance == Variance.STAR ||
            typeStack.queue.contains(type)
        ) {
            return typeArg
        }
        val resolvedType = getJavaWildcardWithTypeVariables(
            type = type,
            scope = scope,
            typeStack = typeStack,
        )
        val resolvedVariance = if (declarationTypeParameter.variance != Variance.INVARIANT) {
            declarationTypeParameter.variance
        } else {
            typeArg.variance
        }
        return createTypeArgument(resolvedType, resolvedVariance)
    }

    private fun getJavaWildcardWithTypeVariablesForOuterType(
        typeArg: KSTypeArgument,
        declarationTypeArg: KSTypeArgument,
        scope: KSTypeVarianceResolverScope,
        typeStack: ReferenceStack,
    ): KSTypeArgument {
        val type = typeArg.type?.resolve()
        if (
            type == null ||
            type.isError ||
            typeArg.variance == Variance.STAR ||
            typeStack.queue.contains(type)
        ) {
            return typeArg
        }
        val resolvedType = getJavaWildcardWithTypeVariables(
            type = type,
            declarationType = declarationTypeArg.type?.resolve(),
            scope = scope,
            typeStack = typeStack
        )
        val resolvedVariance = if (declarationTypeArg.variance != Variance.INVARIANT &&
            (!scope.isValOrReturnType() || declarationTypeArg.variance != Variance.COVARIANT)) {
            declarationTypeArg.variance
        } else {
            typeArg.variance
        }
        return createTypeArgument(resolvedType, resolvedVariance)
    }

    private fun applyJvmWildcardAnnotations(
        type: KSType,
        typeStack: ReferenceStack = ReferenceStack(),
    ): KSType {
        if (type.isError || typeStack.queue.contains(type)) {
            return type
        }
        return typeStack.withReference(type) {
            val resolvedTypeArgs =
                type.arguments.indices.map { i ->
                    applyJvmWildcardAnnotations(
                        typeArg = type.arguments[i],
                        typeParameter = type.declaration.typeParameters[i],
                        typeStack = typeStack,
                    )
                }
            type.replace(resolvedTypeArgs)
        }
    }

    private fun applyJvmWildcardAnnotations(
        typeArg: KSTypeArgument,
        typeParameter: KSTypeParameter,
        typeStack: ReferenceStack,
    ): KSTypeArgument {
        val type = typeArg.type?.resolve()
        if (
            type == null ||
            type.isError ||
            typeArg.variance == Variance.STAR ||
            typeStack.queue.contains(type)
        ) {
            return typeArg
        }
        val resolvedType = applyJvmWildcardAnnotations(
            type = type,
            typeStack = typeStack,
        )
        val resolvedVariance = when {
            typeParameter.variance == Variance.INVARIANT &&
                typeArg.variance != Variance.INVARIANT -> typeArg.variance
            typeArg.hasJvmWildcardAnnotation() -> typeParameter.variance
                typeStack.queue.any { it.hasSuppressJvmWildcardAnnotation() } ||
                typeArg.hasSuppressWildcardsAnnotationInHierarchy() ||
                typeParameter.hasSuppressWildcardsAnnotationInHierarchy() -> Variance.INVARIANT
            else -> typeArg.variance
        }
        return createTypeArgument(resolvedType, resolvedVariance)
    }

    private fun KSType.isTypeParameter(): Boolean {
        return createTypeReference().isTypeParameterReference()
    }

    private fun createTypeArgument(type: KSType, variance: Variance): KSTypeArgument {
        return resolver.getTypeArgument(type.createTypeReference(), variance)
    }
}

/**
 * Inheriting variance for self referencing types (e.g. Foo<T : Foo>) could go into an infinite
 * loop. To avoid that issue, every time we visit a type, we keep it in the reference stack and
 * if a type argument resolves to it, it will stop recursion.
 */
private class ReferenceStack {
    val queue = ArrayDeque<KSType>()

    inline fun <T> withReference(
        ksType: KSType,
        crossinline block: () -> T
    ): T {
        return try {
            queue.addLast(ksType)
            block()
        } finally {
            queue.removeLast()
        }
    }
}
