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

import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.rawTypeName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.symbol.Variance
import com.squareup.kotlinpoet.javapoet.JClassName

internal fun KSType.replaceSuspendFunctionTypes(resolver: Resolver): KSType {
    return if (!isSuspendFunctionType) {
        this
    } else {
        // Find the JVM FunctionN type that will replace the suspend function and use that.
        val functionN =
            resolver.requireType(
                (declaration.asJTypeName(resolver).rawTypeName() as JClassName).canonicalName()
            )
        functionN.replace(
            buildList {
                addAll(arguments.dropLast(1))
                val continuationTypeRef =
                    resolver
                        .requireType("kotlin.coroutines.Continuation")
                        .replace(arguments.takeLast(1))
                        .createTypeReference()
                add(resolver.getTypeArgument(continuationTypeRef, Variance.INVARIANT))
                val objTypeRef = resolver.requireType("java.lang.Object").createTypeReference()
                add(resolver.getTypeArgument(objTypeRef, Variance.INVARIANT))
            }
        )
    }
}

internal fun KSType.replaceTypeAliases(resolver: Resolver): KSType {
    return if (declaration is KSTypeAlias) {
            // Note: KSP only gives us access to the typealias through the declaration. This means
            // that any type arguments on the typealias won't be resolved so we have to do this
            // manually by creating a map from type parameter to type argument and manually
            // substituting the type parameters as we find them.
            val typeParamNameToTypeArgs =
                declaration.typeParameters.indices.associate { i ->
                    declaration.typeParameters[i].name.asString() to arguments[i]
                }
            (declaration as KSTypeAlias)
                .type
                .resolve()
                .replaceTypeArgs(resolver, typeParamNameToTypeArgs)
        } else {
            this
        }
        .let { it.replace(it.arguments.map { typeArg -> typeArg.replaceTypeAliases(resolver) }) }
        .let {
            // if this type is nullable, carry it over
            if (nullability == Nullability.NULLABLE) {
                it.makeNullable()
            } else {
                it
            }
        }
}

private fun KSTypeArgument.replaceTypeAliases(resolver: Resolver): KSTypeArgument {
    val type = type?.resolve() ?: return this
    return resolver.getTypeArgument(
        type.replaceTypeAliases(resolver).createTypeReference(),
        variance
    )
}

private fun KSType.replaceTypeArgs(
    resolver: Resolver,
    typeArgsMap: Map<String, KSTypeArgument>
): KSType = replace(arguments.map { it.replaceTypeArgs(this, resolver, typeArgsMap) })

private fun KSTypeArgument.replaceTypeArgs(
    enclosingType: KSType,
    resolver: Resolver,
    typeArgsMap: Map<String, KSTypeArgument>
): KSTypeArgument {
    val type = type?.resolve() ?: return this
    if (type == enclosingType) return this
    if (type.isTypeParameter()) {
        val name = (type.declaration as KSTypeParameter).name.asString()
        if (typeArgsMap.containsKey(name)) {
            return typeArgsMap[name]!!
        }
    }
    return resolver.getTypeArgument(
        type.replaceTypeArgs(resolver, typeArgsMap).createTypeReference(),
        variance
    )
}

/** Root package comes as <root> instead of "" so we work around it here. */
internal fun KSDeclaration.getNormalizedPackageName(): String {
    return packageName.asString().getNormalizedPackageName()
}

internal fun String.getNormalizedPackageName(): String {
    return if (this == "<root>") {
        ""
    } else {
        this
    }
}

internal fun KSTypeArgument.requireType(): KSType {
    return checkNotNull(type?.resolve()) {
        "KSTypeArgument.type should not have been null, please file a bug. $this"
    }
}

internal fun KSTypeReference.isTypeParameterReference(): Boolean {
    return this.resolve().isTypeParameter()
}

internal fun KSType.isTypeParameter(): Boolean {
    return declaration is KSTypeParameter
}

internal fun KSType.withNullability(nullability: XNullability) =
    when (nullability) {
        XNullability.NULLABLE -> makeNullable()
        XNullability.NONNULL -> makeNotNullable()
        else -> throw IllegalArgumentException("Cannot set KSType nullability to platform")
    }

private fun KSAnnotated.hasAnnotation(qName: String) =
    annotations.any { it.hasQualifiedNameOrAlias(qName) }

private fun KSAnnotation.hasQualifiedNameOrAlias(qName: String): Boolean {
    return annotationType.resolve().hasQualifiedNameOrAlias(qName)
}

private fun KSType.hasQualifiedNameOrAlias(qName: String): Boolean {
    return declaration.qualifiedName?.asString() == qName ||
        (declaration as? KSTypeAlias)?.type?.resolve()?.hasQualifiedNameOrAlias(qName) ?: false
}

internal fun KSAnnotated.hasJvmWildcardAnnotation() =
    hasAnnotation(JvmWildcard::class.java.canonicalName!!)

internal fun KSAnnotated.hasSuppressJvmWildcardAnnotation() =
    hasAnnotation(JvmSuppressWildcards::class.java.canonicalName!!)

// TODO(bcorso): There's a bug in KSP where, after using KSType#asMemberOf() or KSType#replace(),
//  the annotations are removed from the resulting type. However, it turns out that the annotation
//  information is still available in the underlying KotlinType, so we use reflection to get them.
//  See https://github.com/google/ksp/issues/1376.
private fun KSType.hasAnnotation(qName: String): Boolean {
    fun String.toFqName(): Any {
        return Class.forName("org.jetbrains.kotlin.name.FqName")
            .getConstructor(String::class.java)
            .newInstance(this)
    }
    fun hasAnnotationViaReflection(qName: String): Boolean {
        val kotlinType = javaClass.methods.find { it.name == "getKotlinType" }?.invoke(this)
        val kotlinAnnotations =
            kotlinType?.javaClass?.methods?.find { it.name == "getAnnotations" }?.invoke(kotlinType)
        return kotlinAnnotations
            ?.javaClass
            ?.methods
            ?.find { it.name == "hasAnnotation" }
            ?.invoke(kotlinAnnotations, qName.toFqName()) == true
    }
    return if (annotations.toList().isEmpty()) {
        // If there are no annotations but KSType#toString() shows annotations, check the underlying
        // KotlinType for annotations using reflection.
        toString().startsWith("[") && hasAnnotationViaReflection(qName)
    } else {
        annotations.any { it.annotationType.resolve().hasQualifiedNameOrAlias(qName) }
    }
}

internal fun KSType.hasSuppressJvmWildcardAnnotation() =
    hasAnnotation(JvmSuppressWildcards::class.java.canonicalName!!)

internal fun KSNode.hasSuppressWildcardsAnnotationInHierarchy(): Boolean {
    (this as? KSAnnotated)?.let {
        if (hasSuppressJvmWildcardAnnotation()) {
            return true
        }
    }
    val parent = parent ?: return false
    return parent.hasSuppressWildcardsAnnotationInHierarchy()
}

/**
 * Returns the inner arguments for this type.
 *
 * Specifically it excludes outer type args when this type is an inner type.
 *
 * Needed due to https://github.com/google/ksp/issues/2065
 */
val KSType.innerArguments: List<KSTypeArgument>
    get() =
        if (arguments.isNotEmpty()) {
            arguments.subList(0, declaration.typeParameters.size)
        } else {
            emptyList()
        }
