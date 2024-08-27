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
    fun applyTypeVariance(type: KSType, scope: KSTypeVarianceResolverScope?): KSType {
        if (
            type.isError || resolver.isJavaRawType(type) || scope?.needsWildcardResolution == false
        ) {
            // There's nothing to resolve in this case, so just return the original type.
            return type
        }

        // First wrap types/arguments in our own wrappers so that we can keep track of the original
        // type, which is needed to get annotations.
        return KSTypeWrapper(resolver, type)
            // Next, replace all type aliases with their resolved types
            .replaceTypeAliases()
            // Next, replace all suspend functions with their JVM types.
            .replaceSuspendFunctionTypes()
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

    private fun KSTypeWrapper.replaceTypeAliases(): KSTypeWrapper {
        return if (declaration is KSTypeAlias) {
                // Note: KSP only gives us access to the type alias through the declaration. This
                // means
                // that any type arguments on the type alias won't be resolved.
                // For example, if we have a type alias,  MyAlias<T> = Foo<Bar<T>>, and a property,
                // MyAlias<Baz>, then calling KSTypeAlias#type on the property will give Foo<Bar<T>>
                // rather than Foo<Bar<Baz>>.
                val typeParamNameToTypeArgs =
                    declaration.typeParameters.indices.associate { i ->
                        declaration.typeParameters[i].name.asString() to arguments[i]
                    }
                replaceType(declaration.type.resolve()).replaceTypeArgs(typeParamNameToTypeArgs)
            } else {
                this
            }
            .let { it.replace(it.arguments.map { typeArg -> typeArg.replaceTypeAliases() }) }
    }

    private fun KSTypeArgumentWrapper.replaceTypeAliases(): KSTypeArgumentWrapper {
        val type = type ?: return this
        return replace(type.replaceTypeAliases(), variance)
    }

    private fun KSTypeWrapper.replaceTypeArgs(
        typeArgsMap: Map<String, KSTypeArgumentWrapper>
    ): KSTypeWrapper = replace(arguments.map { it.replaceTypeArgs(typeArgsMap) })

    private fun KSTypeArgumentWrapper.replaceTypeArgs(
        typeArgsMap: Map<String, KSTypeArgumentWrapper>
    ): KSTypeArgumentWrapper {
        val type = type ?: return this
        if (type.isTypeParameter()) {
            val name = (type.declaration as KSTypeParameter).name.asString()
            if (typeArgsMap.containsKey(name)) {
                return replace(typeArgsMap[name]?.type!!, variance)
            }
        }
        return replace(type.replaceTypeArgs(typeArgsMap), variance)
    }

    private fun KSTypeWrapper.replaceSuspendFunctionTypes(): KSTypeWrapper {
        val newArguments = arguments.map { it.replaceSuspendFunctionTypes() }
        return if (!newType.isSuspendFunctionType) {
            replace(newArguments)
        } else {
            val newKSType = newType.replaceSuspendFunctionTypes(resolver)
            val newType = KSTypeWrapper(resolver, newKSType)
            replaceType(newKSType)
                .replace(
                    buildList {
                        addAll(newArguments.dropLast(1))
                        val originalArg = newArguments.last()
                        val continuationArg = newType.arguments[newType.arguments.lastIndex - 1]
                        add(
                            continuationArg.replace(
                                continuationArg.type!!.replace(
                                    continuationArg.type!!.arguments.map {
                                        it.replace(originalArg.type!!, originalArg.variance)
                                    }
                                ),
                                continuationArg.variance
                            )
                        )
                        add(newType.arguments.last())
                    }
                )
        }
    }

    private fun KSTypeArgumentWrapper.replaceSuspendFunctionTypes(): KSTypeArgumentWrapper {
        val type = type ?: return this
        return replace(type.replaceSuspendFunctionTypes(), variance)
    }

    private fun KSTypeWrapper.resolveWildcards(scope: KSTypeVarianceResolverScope?) =
        if (scope == null) {
            this
        } else if (hasTypeVariables(scope.declarationType())) {
            // If the associated declared type contains type variables that were resolved, e.g.
            // using "asMemberOf", then it has special rules about how to resolve the types.
            getJavaWildcardWithTypeVariables(
                declarationType =
                    KSTypeWrapper(resolver, scope.declarationType())
                        .replaceTypeAliases()
                        .replaceSuspendFunctionTypes()
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
            // The variance of previous type arguments on the stack. If the type argument has no
            // explicit (aka use-site) variance, the type param (aka declaration site) variance is
            // used.
            val varianceStack =
                typeArgStack.indices.map { i ->
                    if (typeArgStack[i].variance != Variance.INVARIANT) {
                        typeArgStack[i].variance
                    } else {
                        typeParamStack[i].variance
                    }
                }
            // Before we check the current variance, we need to check the previous variance in the
            // stack to see if they allow us to inherit the current variance, and that logic differs
            // depending on the scope.
            if (scope.isValOrReturnType()) {
                // For val and return type scopes, we don't use the declaration-site variance if
                // none of variances in the stack are contravariant.
                if (
                    varianceStack.indices.none { i ->
                        (varianceStack[i] == Variance.CONTRAVARIANT) &&
                            // The declaration and use site variance is ignored when using
                            // @JvmWildcard explicitly on a type.
                            !typeArgStack[i].hasJvmWildcardAnnotation()
                    }
                ) {
                    return false
                }
            } else {
                // For method parameters and var type scopes, we don't use the declaration-site
                // variance if all of the following conditions apply.
                if ( // If the last variance in the stack is not contravariant
                    varianceStack.isNotEmpty() &&
                        varianceStack.last() != Variance.CONTRAVARIANT &&
                        // And the stack contains at least one invariant parameter.
                        varianceStack.any { it == Variance.INVARIANT } &&
                        // And the first invariant comes before the last contravariant (if any).
                        varianceStack.indexOfFirst { it == Variance.INVARIANT } >=
                            varianceStack.indexOfLast { it == Variance.CONTRAVARIANT }
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
                Variance.COVARIANT ->
                    when (val declaration = type.declaration) {
                        is KSClassDeclaration ->
                            declaration.isOpen() ||
                                declaration.classKind == ClassKind.ENUM_CLASS ||
                                declaration.modifiers.contains(Modifier.SEALED) ||
                                // For non-open/enum/sealed classes we may still decided to use the
                                // declaration-site variance based on if any of the type arguments
                                // in the
                                // resolved type has variance and the use-site variance is not equal
                                // to
                                // covariant/contravariant.
                                resolvedType.arguments.indices.any { i ->
                                    resolvedType.arguments[i].variance != Variance.INVARIANT &&
                                        type.arguments[i].variance != Variance.COVARIANT &&
                                        type.arguments[i].variance != Variance.CONTRAVARIANT
                                }
                        else -> true
                    }
                Variance.STAR ->
                    error {
                        "Declaration site variance was not expected to contain STAR: $typeParam."
                    }
            }
        }
        val resolvedVariance =
            if (inheritDeclarationSiteVariance()) {
                typeParam.variance
            } else if (typeParam.variance == variance) {
                // If we're not applying the declaration-site variance, and the use-site variance is
                // the
                // same as the declaration-site variance then we don't include the use-site variance
                // in
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
    ) =
        if (declarationType?.isTypeParameter() == false) {
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
    ) =
        replace(
            arguments.map { it.getJavaWildcardWithTypeVariablesForInnerType(scope, typeParamStack) }
        )

    private fun KSTypeArgumentWrapper.getJavaWildcardWithTypeVariablesForInnerType(
        scope: KSTypeVarianceResolverScope,
        typeParamStack: List<KSTypeParameter>,
    ): KSTypeArgumentWrapper {
        val type = type ?: return this
        val resolvedType =
            type.getJavaWildcardWithTypeVariablesForInnerType(
                scope = scope,
                typeParamStack = typeParamStack + typeParam
            )
        val resolvedVariance =
            if (
                typeParam.variance != Variance.INVARIANT &&
                    // This is a weird rule, but empirically whether or not we inherit type variance
                    // in
                    // this case depends on the scope of the type used when calling asMemberOf. For
                    // example, if XMethodElement#asMemberOf(XType) is called with an XType that has
                    // no
                    // scope or has a matching method scope then we inherit the parameter variance;
                    // however,
                    // if asMemberOf was called with an XType that was from a different scope we
                    // only
                    // inherit variance here if there is at least one contravariant in the param
                    // stack.
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
        val resolvedType =
            type.getJavaWildcardWithTypeVariables(
                declarationType = declarationTypeArg.type,
                scope = scope,
            )
        val resolvedVariance =
            if (declarationTypeArg.variance != Variance.INVARIANT) {
                declarationTypeArg.variance
            } else {
                variance
            }
        return replace(resolvedType, resolvedVariance)
    }

    private fun KSTypeWrapper.applyJvmWildcardAnnotations(scope: KSTypeVarianceResolverScope?) =
        replace(arguments.map { it.applyJvmWildcardAnnotations(scope) })

    private fun KSTypeArgumentWrapper.applyJvmWildcardAnnotations(
        scope: KSTypeVarianceResolverScope?
    ): KSTypeArgumentWrapper {
        val type = type ?: return this
        val resolvedType = type.applyJvmWildcardAnnotations(scope)
        val resolvedVariance =
            when {
                typeParam.variance == Variance.INVARIANT && variance != Variance.INVARIANT ->
                    variance
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
private class KSTypeWrapper
constructor(
    val resolver: Resolver,
    val originalType: KSType,
    val newType: KSType = originalType,
    val newTypeArguments: List<KSTypeArgumentWrapper>? = null,
    val typeStack: List<KSTypeWrapper> = emptyList(),
    val typeArgStack: List<KSTypeArgumentWrapper> = emptyList(),
    val typeParamStack: List<KSTypeParameter> = emptyList(),
) {
    val declaration = newType.declaration

    @OptIn(KspExperimental::class)
    val arguments: List<KSTypeArgumentWrapper> by lazy {
        if (resolver.isJavaRawType(newType)) {
            return@lazy emptyList()
        }
        val arguments =
            newTypeArguments
                ?: newType.innerArguments.indices.map { i ->
                    KSTypeArgumentWrapper(
                        originalTypeArg = newType.innerArguments[i],
                        typeParam = newType.declaration.typeParameters[i],
                        resolver = resolver,
                    )
                }
        arguments.map { newTypeArg ->
            newTypeArg.copy(
                typeStack = typeStack + this,
                typeArgStack = typeArgStack,
                typeParamStack = typeParamStack,
            )
        }
    }

    fun replaceType(newType: KSType): KSTypeWrapper = copy(newType = newType)

    fun replace(newTypeArguments: List<KSTypeArgumentWrapper>) =
        copy(newTypeArguments = newTypeArguments)

    fun copy(
        originalType: KSType = this.originalType,
        newType: KSType = this.newType,
        newTypeArguments: List<KSTypeArgumentWrapper>? = this.newTypeArguments,
        typeStack: List<KSTypeWrapper> = this.typeStack,
        typeArgStack: List<KSTypeArgumentWrapper> = this.typeArgStack,
        typeParamStack: List<KSTypeParameter> = this.typeParamStack,
    ) =
        KSTypeWrapper(
            resolver = resolver,
            originalType = originalType,
            newType = newType,
            newTypeArguments = newTypeArguments,
            typeStack = typeStack,
            typeArgStack = typeArgStack,
            typeParamStack = typeParamStack,
        )

    fun hasSuppressJvmWildcardAnnotation() = originalType.hasSuppressJvmWildcardAnnotation()

    fun isTypeParameter() = originalType.isTypeParameter()

    fun unwrap(): KSType {
        val newArgs = arguments.map { it.unwrap() }
        return newType.replace(
            newType.arguments.mapIndexed { index, oldArg ->
                if (index < newArgs.size) {
                    newArgs[index]
                } else {
                    oldArg
                }
            }
        )
    }

    override fun toString() = buildString {
        if (originalType.annotations.toList().isNotEmpty()) {
            append("${originalType.annotations.toList()} ")
        }
        append(newType.declaration.simpleName.asString())
        if (arguments.isNotEmpty()) {
            append("<${arguments.joinToString(", ")}>")
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
private class KSTypeArgumentWrapper
constructor(
    val originalTypeArg: KSTypeArgument,
    val newType: KSTypeWrapper? = null,
    val resolver: Resolver,
    val typeParam: KSTypeParameter,
    val variance: Variance = originalTypeArg.variance,
    val typeStack: List<KSTypeWrapper> = emptyList(),
    val typeArgStack: List<KSTypeArgumentWrapper> = emptyList(),
    val typeParamStack: List<KSTypeParameter> = emptyList(),
) {
    val type: KSTypeWrapper? by lazy {
        if (variance == Variance.STAR || originalTypeArg.type == null) {
            // Return null for star projections, otherwise we'll end up in an infinite loop.
            return@lazy null
        }
        val type = newType ?: KSTypeWrapper(resolver, originalTypeArg.type!!.resolve())
        type.copy(
            typeStack = typeStack,
            typeArgStack = typeArgStack + this,
            typeParamStack = typeParamStack + typeParam,
        )
    }

    fun replace(newType: KSTypeWrapper, newVariance: Variance) =
        copy(
            newType = newType,
            variance = newVariance,
        )

    fun copy(
        originalTypeArg: KSTypeArgument = this.originalTypeArg,
        newType: KSTypeWrapper? = this.newType,
        typeParam: KSTypeParameter = this.typeParam,
        variance: Variance = this.variance,
        typeStack: List<KSTypeWrapper> = this.typeStack,
        typeArgStack: List<KSTypeArgumentWrapper> = this.typeArgStack,
        typeParamStack: List<KSTypeParameter> = this.typeParamStack,
    ) =
        KSTypeArgumentWrapper(
            resolver = resolver,
            originalTypeArg = originalTypeArg,
            newType = newType,
            variance = variance,
            typeParam = typeParam,
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
