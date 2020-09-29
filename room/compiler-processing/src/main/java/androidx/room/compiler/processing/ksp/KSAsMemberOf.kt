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

import org.jetbrains.kotlin.ksp.closestClassDeclaration
import org.jetbrains.kotlin.ksp.getAllSuperTypes
import org.jetbrains.kotlin.ksp.processing.Resolver
import org.jetbrains.kotlin.ksp.symbol.KSClassDeclaration
import org.jetbrains.kotlin.ksp.symbol.KSDeclaration
import org.jetbrains.kotlin.ksp.symbol.KSName
import org.jetbrains.kotlin.ksp.symbol.KSPropertyDeclaration
import org.jetbrains.kotlin.ksp.symbol.KSType
import org.jetbrains.kotlin.ksp.symbol.KSTypeArgument
import org.jetbrains.kotlin.ksp.symbol.KSTypeParameter
import org.jetbrains.kotlin.ksp.symbol.KSTypeReference
import org.jetbrains.kotlin.ksp.symbol.Nullability

/**
 * Returns the type of a property as if it is member of the given [ksType].
 *
 * This is a temporary / inefficient implementation until KSP provides the API. It also does not
 * handle inner classes properly.
 * TODO: remove once https://github.com/android/kotlin/issues/26 is implemented
 */
internal fun KSPropertyDeclaration.typeAsMemberOf(resolver: Resolver, ksType: KSType): KSType {
    val myType: KSType = checkNotNull(type?.requireType()) {
        "Cannot find type of Kotlin property: $this"
    }
    return myType.asMemberOf(resolver, this, ksType)
}

/**
 * Returns `this` type as member of the [other] type.
 *
 * @param resolver The KSP resolver instance
 * @param declaration The KSDeclaration where the owner of this type is defined. Note that this can
 * be different from [KSType.declaration]. For instance, if you have a class `Foo<T>` with property
 * `x : List<T>`, `x`'s type declaration is `kotlin.List` whereas the declaration that
 * should be passed here is `x` (from which the implementation will find `Foo`). On the other hand,
 * `T` of `List<T>`'s declaration is already in `Foo`.
 * @param other The new owner for this type. For instance, if you want to resolve `x` in
 * `Bar<String>`, this would be the star projected type of `Bar`.
 */
internal fun KSType.asMemberOf(
    resolver: Resolver,
    declaration: KSDeclaration,
    other: KSType
): KSType {
    val parent = declaration.closestClassDeclaration() ?: return this
    val parentQName = parent.qualifiedName ?: return this
    val matchingParentType: KSType = (other.declaration as? KSClassDeclaration)
        ?.getAllSuperTypes()
        ?.firstOrNull {
            it.starProjection().declaration.qualifiedName == parentQName
        } ?: return this
    // create a map of replacements.
    val replacements = parent.typeParameters.mapIndexed { index, ksTypeParameter ->
        ksTypeParameter.name to matchingParentType.arguments.getOrNull(index)
    }.toMap()
    return replaceFromMap(resolver, replacements)
}

private fun KSTypeArgument.replaceFromMap(
    resolver: Resolver,
    arguments: Map<KSName, KSTypeArgument?>
): KSTypeArgument {
    val resolvedType = type?.resolve()
    val myTypeDeclaration = resolvedType?.declaration
    if (myTypeDeclaration is KSTypeParameter) {
        val match = arguments[myTypeDeclaration.name] ?: return this
        // workaround for https://github.com/google/ksp/issues/82
        val explicitNullable = resolvedType.makeNullable() == resolvedType
        return if (explicitNullable) {
            match.makeNullable(resolver)
        } else {
            match
        }
    }
    return this
}

private fun KSType.replaceFromMap(
    resolver: Resolver,
    arguments: Map<KSName, KSTypeArgument?>
): KSType {
    val myDeclaration = this.declaration
    if (myDeclaration is KSTypeParameter) {
        val match = arguments[myDeclaration.name]?.type?.resolve() ?: return this
        // workaround for https://github.com/google/ksp/issues/82
        val explicitNullable = this.makeNullable() == this
        return if (explicitNullable) {
            match.makeNullable()
        } else {
            match
        }
    }
    if (this.arguments.isEmpty()) {
        return this
    }
    return replace(this.arguments.map {
        it.replaceFromMap(resolver, arguments)
    })
}

private fun KSTypeArgument.makeNullable(resolver: Resolver): KSTypeArgument {
    val myType = type
    val resolved = myType?.resolve() ?: return this
    if (resolved.nullability == Nullability.NULLABLE) {
        return this
    }
    return resolver.getTypeArgument(myType.swapResolvedType(resolved.makeNullable()), variance)
}

private fun KSTypeReference.swapResolvedType(replacement: KSType): KSTypeReference {
    return DelegatingTypeReference(
        original = this,
        resolved = replacement
    )
}

private class DelegatingTypeReference(
    val original: KSTypeReference,
    val resolved: KSType
) : KSTypeReference by original {
    override fun resolve(): KSType? = resolved
}
