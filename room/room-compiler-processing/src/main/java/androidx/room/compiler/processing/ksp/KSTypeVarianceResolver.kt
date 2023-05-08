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

import androidx.room.compiler.processing.rawTypeName
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
import com.squareup.kotlinpoet.javapoet.JClassName

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
    fun applyTypeVariance(type: KSType, scope: KSTypeVarianceResolverScope?): KSType {
        if (type.isError ||
            type.arguments.isEmpty() ||
            resolver.isJavaRawType(type) ||
            scope?.needsWildcardResolution == false) {
            // There's nothing to resolve in this case, so just return the original type.
            return type
        }

        // First wrap types/arguments in our own wrappers so that we can keep track of the original
        // type, which is needed to get annotations.
        return KSTypeWrapper(resolver, type)
            // Next, resolve wildcards based on the scope of the type
            .resolveWildcards(scope)
            // Next, apply any additional variance changes based on the @JvmSuppressWildcards or
            // @JvmWildcard annotations on the resolved type.
            .applyJvmWildcardAnnotations(scope)
            // Finally, unwrap any delegate types. (Note: as part of resolving wildcards, we wrap
            // types/type arguments in delegates to avoid loosing annotation information. However,
            // those delegates may cause issues later if KSP tries to cast the type/argument to a
            // particular implementation, so we unwrap them here.
            .unwrap()
    }

    private fun KSTypeWrapper.resolveWildcards(
        scope: KSTypeVarianceResolverScope?
    ) = if (scope == null) {
        this
    } else if (hasTypeVariables(scope.declarationType())) {
        // If the associated declared type contains type variables that were resolved, e.g.
        // using "asMemberOf", then it has special rules about how to resolve the types.
        getJavaWildcardWithTypeVariables(
            declarationType = KSTypeWrapper(resolver, scope.declarationType())
                .getJavaWildcard(scope),
            scope = scope,
        )
    } else {
        getJavaWildcard(scope)
    }

    private fun hasTypeVariables(type: KSType?, stack: List<KSType> = emptyList()): Boolean {
        if (type == null || type.isError || stack.contains(type)) {
            return false
        }
        return type.isTypeParameter() ||
            type.arguments.any { hasTypeVariables(it.type?.resolve(), stack + type) }
    }

    private fun KSTypeWrapper.getJavaWildcard(scope: KSTypeVarianceResolverScope) =
        replace(arguments.map { it.getJavaWildcard(scope) })

    private fun KSTypeArgumentWrapper.getJavaWildcard(
        scope: KSTypeVarianceResolverScope
    ): KSTypeArgumentWrapper {
        val type = type ?: return this
        val resolvedType = type.getJavaWildcard(scope)
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
                            // The declaration and use site variance is ignored when using
                            // @JvmWildcard explicitly on a type.
                            !typeArgStack[i].hasJvmWildcardAnnotation()
                    }) {
                    return false
                }
            } else {
                // For method parameters and var type scopes, we don't use the declaration-site
                // variance if all of the following conditions apply.
                if ( // If the last variance in the type argument stack is not contravariant
                    typeArgStack.isNotEmpty() &&
                    typeArgStack.last().variance != Variance.CONTRAVARIANT &&
                    // And the type parameter stack contains at least one invariant parameter.
                    typeParamStack.isNotEmpty() &&
                    typeParamStack.any { it.variance == Variance.INVARIANT } &&
                    // And the first invariant comes before the last contravariant (if any).
                    typeParamStack.indexOfFirst { it.variance == Variance.INVARIANT } >=
                    typeParamStack.indexOfLast { it.variance == Variance.CONTRAVARIANT }
                ) {
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
        } else if (typeParam.variance == variance) {
            // If we're not applying the declaration-site variance, and the use-site variance is the
            // same as the declaration-site variance then we don't include the use-site variance in
            // the jvm type either.
            Variance.INVARIANT
        } else {
            variance
        }
        return replace(resolvedType, resolvedVariance)
    }

    private fun KSTypeWrapper.getJavaWildcardWithTypeVariables(
        scope: KSTypeVarianceResolverScope,
        declarationType: KSTypeWrapper?,
    ) = if (declarationType?.isTypeParameter() == false) {
        replace(
            declarationType.arguments.indices.map { i ->
                arguments[i].getJavaWildcardWithTypeVariablesForOuterType(
                    declarationTypeArg = declarationType.arguments[i],
                    scope = scope,
                )
            }
        )
    } else {
        getJavaWildcardWithTypeVariablesForInnerType(scope)
    }

    private fun KSTypeWrapper.getJavaWildcardWithTypeVariablesForInnerType(
        scope: KSTypeVarianceResolverScope,
        typeParamStack: List<KSTypeParameter> = emptyList(),
    ) = replace(
        arguments.map { it.getJavaWildcardWithTypeVariablesForInnerType(scope, typeParamStack) }
    )

    private fun KSTypeArgumentWrapper.getJavaWildcardWithTypeVariablesForInnerType(
        scope: KSTypeVarianceResolverScope,
        typeParamStack: List<KSTypeParameter>,
    ): KSTypeArgumentWrapper {
        val type = type ?: return this
        val resolvedType = type.getJavaWildcardWithTypeVariablesForInnerType(
            scope = scope,
            typeParamStack = typeParamStack + typeParam
        )
        val resolvedVariance = if (
            typeParam.variance != Variance.INVARIANT &&
            // This is a weird rule, but empirically whether or not we inherit type variance in
            // this case depends on the scope of the type used when calling asMemberOf. For
            // example, if XMethodElement#asMemberOf(XType) is called with an XType that has no
            // scope or has a matching method scope then we inherit the parameter variance; however,
            // if asMemberOf was called with an XType that was from a different scope we only
            // inherit variance here if there is at least one contravariant in the param stack.
            (scope.asMemberOfScopeOrSelf() == scope ||
                typeParamStack.any { it.variance == Variance.CONTRAVARIANT })
        ) {
            typeParam.variance
        } else {
            variance
        }
        return replace(resolvedType, resolvedVariance)
    }

    private fun KSTypeArgumentWrapper.getJavaWildcardWithTypeVariablesForOuterType(
        declarationTypeArg: KSTypeArgumentWrapper,
        scope: KSTypeVarianceResolverScope,
    ): KSTypeArgumentWrapper {
        val type = type ?: return this
        val resolvedType = type.getJavaWildcardWithTypeVariables(
            declarationType = declarationTypeArg.type,
            scope = scope,
        )
        val resolvedVariance = if (declarationTypeArg.variance != Variance.INVARIANT) {
            declarationTypeArg.variance
        } else {
            variance
        }
        return replace(resolvedType, resolvedVariance)
    }

    private fun KSTypeWrapper.applyJvmWildcardAnnotations(
        scope: KSTypeVarianceResolverScope?
    ) =
        replace(arguments.map { it.applyJvmWildcardAnnotations(scope) })

    private fun KSTypeArgumentWrapper.applyJvmWildcardAnnotations(
        scope: KSTypeVarianceResolverScope?
    ): KSTypeArgumentWrapper {
        val type = type ?: return this
        val resolvedType = type.applyJvmWildcardAnnotations(scope)
        val resolvedVariance = when {
            typeParam.variance == Variance.INVARIANT && variance != Variance.INVARIANT -> variance
            hasJvmWildcardAnnotation() -> typeParam.variance
            scope?.hasSuppressWildcards == true ||
                // We only need to check the first type in the stack for @JvmSuppressWildcards.
                // Any other @JvmSuppressWildcards usages will be placed on the type arguments
                // rather than the types, so no need to check the rest of the types.
                typeStack.first().hasSuppressJvmWildcardAnnotation() ||
                this.hasSuppressWildcardsAnnotationInHierarchy() ||
                typeArgStack.any { it.hasSuppressJvmWildcardAnnotation() } ||
                typeParam.hasSuppressWildcardsAnnotationInHierarchy() -> Variance.INVARIANT
            else -> variance
        }
        return replace(resolvedType, resolvedVariance)
    }
}

/**
 * A wrapper for creating a new [KSType] that allows arguments of type [KSTypeArgumentWrapper].
 *
 * Note: This wrapper acts similar to [KSType#replace(KSTypeArgument)]. However, we can't call
 * [KSType#replace(KSTypeArgument)] directly when using [KSTypeArgumentWrapper] or we'll get an
 * [IllegalStateException] since KSP tries to cast to its own implementation of [KSTypeArgument].
 */
private class KSTypeWrapper constructor(
    private val resolver: Resolver,
    private val originalType: KSType,
    private val newType: KSType =
        originalType.replaceTypeAliases().replaceSuspendFunctionTypes(resolver),
    newTypeArguments: List<KSTypeArgumentWrapper>? = null,
    private val typeStack: List<KSTypeWrapper> = emptyList(),
    private val typeArgStack: List<KSTypeArgumentWrapper> = emptyList(),
    private val typeParamStack: List<KSTypeParameter> = emptyList(),
) {
    val declaration = originalType.declaration

    val arguments: List<KSTypeArgumentWrapper> by lazy {
        newTypeArguments ?: newType.arguments.indices.map { i ->
            KSTypeArgumentWrapper(
                originalTypeArg = newType.arguments[i],
                typeParam = newType.declaration.typeParameters[i],
                resolver = resolver,
                typeStack = typeStack + this,
                typeArgStack = typeArgStack,
                typeParamStack = typeParamStack,
            )
        }
    }

    fun replace(newTypeArguments: List<KSTypeArgumentWrapper>) = KSTypeWrapper(
        originalType = originalType,
        newType = newType,
        newTypeArguments = newTypeArguments,
        resolver = resolver,
        typeStack = typeStack,
        typeArgStack = typeArgStack,
        typeParamStack = typeParamStack,
    )

    fun hasSuppressJvmWildcardAnnotation() = originalType.hasSuppressJvmWildcardAnnotation()

    fun isTypeParameter() = originalType.isTypeParameter()

    fun unwrap() = newType.replace(arguments.map { it.unwrap() })

    override fun toString() = buildString {
        if (originalType.annotations.toList().isNotEmpty()) {
            append("${originalType.annotations.toList()} ")
        }
        append(newType.declaration.simpleName.asString())
        if (arguments.isNotEmpty()) {
            append("$arguments")
        }
    }

    private companion object {
        fun KSType.replaceTypeAliases() = (declaration as? KSTypeAlias)?.type?.resolve() ?: this

        fun KSType.replaceSuspendFunctionTypes(resolver: Resolver) = if (!isSuspendFunctionType) {
            this
        } else {
            // Find the JVM FunctionN type that will replace the suspend function and use that.
            val functionN = resolver.requireType(
                (declaration.asJTypeName(resolver).rawTypeName() as JClassName).canonicalName()
            )
            functionN.replace(
                buildList {
                    addAll(arguments.dropLast(1))
                    val continuationArgs = arguments.takeLast(1)
                    val continuationTypeRef = resolver.requireType("kotlin.coroutines.Continuation")
                        .replace(continuationArgs)
                        .createTypeReference()
                    val objTypeRef = resolver.requireType("java.lang.Object").createTypeReference()
                    add(resolver.getTypeArgument(continuationTypeRef, Variance.INVARIANT))
                    add(resolver.getTypeArgument(objTypeRef, Variance.INVARIANT))
                }
            )
        }
    }
}

/**
 * A wrapper for creating a new [KSTypeArgument] that delegates to the original argument for
 * annotations.
 *
 * Note: This wrapper acts similar to [Resolver#getTypeArgument(KSTypeReference, Variance)].
 * However, we can't call [Resolver#getTypeArgument(KSTypeReference, Variance)] directly because
 * we'll lose information about annotations (e.g. `@JvmSuppressWildcards`) that were on the original
 * type argument.
 */
private class KSTypeArgumentWrapper constructor(
    private val originalTypeArg: KSTypeArgument,
    private val newType: KSTypeWrapper? = null,
    private val resolver: Resolver,
    val typeParam: KSTypeParameter,
    val variance: Variance = originalTypeArg.variance,
    val typeStack: List<KSTypeWrapper>,
    val typeArgStack: List<KSTypeArgumentWrapper>,
    val typeParamStack: List<KSTypeParameter>,
) {
    val type: KSTypeWrapper? by lazy {
        if (variance == Variance.STAR || originalTypeArg.type == null) {
            // Return null for star projections, otherwise we'll end up in an infinite loop.
            null
        } else {
            newType ?: KSTypeWrapper(
                originalType = originalTypeArg.type!!.resolve(),
                resolver = resolver,
                typeStack = typeStack,
                typeArgStack = typeArgStack + this,
                typeParamStack = typeParamStack + typeParam,
            )
        }
    }

    fun replace(newType: KSTypeWrapper, newVariance: Variance) = KSTypeArgumentWrapper(
        originalTypeArg = originalTypeArg,
        typeParam = typeParam,
        newType = newType,
        variance = newVariance,
        resolver = resolver,
        typeStack = typeStack,
        typeArgStack = typeArgStack,
        typeParamStack = typeParamStack,
    )

    fun hasJvmWildcardAnnotation() = originalTypeArg.hasJvmWildcardAnnotation()

    fun hasSuppressJvmWildcardAnnotation() = originalTypeArg.hasSuppressJvmWildcardAnnotation()

    fun hasSuppressWildcardsAnnotationInHierarchy() =
        originalTypeArg.hasSuppressWildcardsAnnotationInHierarchy()

    fun unwrap(): KSTypeArgument {
        val unwrappedType = type?.unwrap()
        return if (unwrappedType == null || unwrappedType.isError) {
            originalTypeArg
        } else {
            resolver.getTypeArgument(unwrappedType.createTypeReference(), variance)
        }
    }

    override fun toString() = buildString {
        if (originalTypeArg.annotations.toList().isNotEmpty()) {
            append("${originalTypeArg.annotations.toList()} ")
        }
        append(
            when (variance) {
                Variance.INVARIANT -> "$type"
                Variance.CONTRAVARIANT -> "in $type"
                Variance.COVARIANT -> "out $type"
                Variance.STAR -> "*"
            }
        )
    }
}
