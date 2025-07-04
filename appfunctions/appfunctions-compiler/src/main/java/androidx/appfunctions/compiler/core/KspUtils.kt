/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions.compiler.core

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Variance
import com.google.devtools.ksp.symbol.Variance.CONTRAVARIANT
import com.google.devtools.ksp.symbol.Variance.COVARIANT
import com.google.devtools.ksp.symbol.Variance.INVARIANT
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * Resolves [KSTypeReference] based on the declaration.
 *
 * If the declaration is [KSClassDeclaration], returns the self type directly. If the declaration is
 * [KSTypeParameter], returns the upper bound type instead.
 */
fun KSTypeReference.resolveSelfOrUpperBoundType(): KSTypeReference {
    val declaration = this.resolve().declaration
    return when (declaration) {
        is KSClassDeclaration -> {
            this
        }
        is KSTypeParameter -> {
            declaration.bounds.singleOrNull()
                ?: throw ProcessingException(
                    "AppFunction compiler does not support multi-bounds type parameter",
                    declaration,
                )
        }
        else -> {
            throw ProcessingException("Unsupported declaration type", declaration)
        }
    }
}

/** Gets the [TypeVariableName] from [KSTypeParameter]. */
fun KSTypeParameter.toTypeVariableName(): TypeVariableName {
    return TypeVariableName(name.asString())
}

/**
 * Gets the qualified name from [KSDeclaration].
 *
 * @throws ProcessingException if unable to resolve qualified name.
 */
fun KSDeclaration.ensureQualifiedName(): String {
    return this.qualifiedName?.asString()
        ?: throw ProcessingException("Unable to resolve the qualified name", this)
}

/**
 * Gets the full [ClassName] from the [KSDeclaration].
 *
 * This ensures that the multi-layer declaration would return the right [ClassName] including all
 * the parent declarations. For example,
 * ```
 * package com.example
 *
 * class Something {
 *   class AnotherThing
 * }
 * ````
 *
 * Calling this function on AnotherThing's declaration would return
 * `com.example.Something.AnotherThing`.
 */
fun KSDeclaration.toClassName(): ClassName {
    val packageName = this.packageName.asString()
    val simpleNames =
        buildList {
                var currentDeclaration: KSDeclaration? = this@toClassName
                while (currentDeclaration != null) {
                    add(currentDeclaration.simpleName.asString())
                    val parent = currentDeclaration.parentDeclaration
                    if (parent == null || parent is KSFile) {
                        break
                    }
                    currentDeclaration = parent
                }
            }
            .reversed()
    return ClassName(packageName, simpleNames)
}

/**
 * Gets the JVM qualified name from [KSDeclaration].
 *
 * This ensures that the multi-layer declaration would return the right JVM qualified name. For
 * example,
 * ```
 * package com.example
 *
 * class Something {
 *   class AnotherThing
 * }
 * ````
 *
 * Calling this function on AnotherThing's declaration would return
 * `com.example.Something$AnotherThing`.
 */
fun KSDeclaration.getJvmQualifiedName(): String {
    return toClassName().reflectionName()
}

/**
 * Returns the JVM class name which takes into account multi-layer class declarations. For example,
 * ```
 * package com.example
 *
 * class Something {
 *   class AnotherThing
 * }
 * ````
 *
 * Calling this function on AnotherThing's declaration would return `Something$AnotherThing`.
 */
fun KSDeclaration.getJvmClassName(): String {
    return toClassName().reflectionName().substringAfterLast('.')
}

/**
 * Resolves the type reference to the parameterized type if it is a list.
 *
 * @return the resolved type reference
 * @throws ProcessingException If unable to resolve the type.
 */
fun KSTypeReference.resolveListParameterizedType(): KSTypeReference {
    if (!isOfType(LIST)) {
        throw ProcessingException(
            "Unable to resolve list parameterized type for non list type",
            this,
        )
    }
    return resolve().arguments.firstOrNull()?.type
        ?: throw ProcessingException("Unable to resolve the parameterized type for the list", this)
}

/**
 * Checks if the type reference is of the given type.
 *
 * @param type the type to check against
 * @return true if the type reference is of the given type
 * @throws ProcessingException If unable to resolve the type.
 */
fun KSTypeReference.isOfType(type: ClassName): Boolean {
    val typeName = ensureQualifiedTypeName()
    return typeName.asString() == type.canonicalName
}

/**
 * Finds and returns an annotation of [annotationClass] type.
 *
 * @param annotationClass the annotation class to find
 */
fun Sequence<KSAnnotation>.findAnnotation(annotationClass: ClassName): KSAnnotation? =
    this.singleOrNull {
        val shortName = it.shortName.getShortName()
        if (shortName != annotationClass.simpleName) {
            false
        } else {
            val typeName = it.annotationType.ensureQualifiedTypeName()
            typeName.asString() == annotationClass.canonicalName
        }
    }

/**
 * Resolves the type reference to its qualified name.
 *
 * @return the qualified name of the type reference
 */
fun KSTypeReference.ensureQualifiedTypeName(): KSName =
    resolve().declaration.qualifiedName
        ?: throw ProcessingException(
            "Unable to resolve the qualified type name for this reference",
            this,
        )

/** Returns the value of the annotation property if found. */
fun <T : Any> KSAnnotation.requirePropertyValueOfType(
    propertyName: String,
    expectedType: KClass<T>,
): T {
    val propertyValue =
        this.arguments.singleOrNull { it.name?.asString() == propertyName }?.value
            ?: throw ProcessingException("Unable to find property with name: $propertyName", this)
    return expectedType.cast(propertyValue)
}

// TODO: Import KotlinPoet KSP to replace these KSPUtils.
fun KSTypeReference.toTypeName(): TypeName {
    val args = resolve().arguments
    return resolve().toTypeName(args)
}

internal fun TypeName.ignoreNullable(): TypeName {
    return copy(nullable = false)
}

private fun KSType.toTypeName(arguments: List<KSTypeArgument> = emptyList()): TypeName {
    val type =
        when (declaration) {
            is KSClassDeclaration -> {
                val typeClassName = declaration.toClassName()
                typeClassName.withTypeArguments(arguments.map { it.toTypeName() })
            }
            else -> throw ProcessingException("Unable to resolve TypeName", null)
        }
    return type.copy(nullable = isMarkedNullable)
}

private fun KSTypeArgument.toTypeName(): TypeName {
    val type = this.type ?: return STAR
    return when (variance) {
        COVARIANT -> WildcardTypeName.producerOf(type.toTypeName())
        CONTRAVARIANT -> WildcardTypeName.consumerOf(type.toTypeName())
        Variance.STAR -> STAR
        INVARIANT -> type.toTypeName()
    }
}

private fun ClassName.withTypeArguments(arguments: List<TypeName>): TypeName {
    return if (arguments.isEmpty()) {
        this
    } else {
        this.parameterizedBy(arguments)
    }
}

fun KClass<*>.ensureQualifiedName(): String = checkNotNull(qualifiedName)
