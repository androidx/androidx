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
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
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
import com.squareup.kotlinpoet.WildcardTypeName
import kotlin.reflect.KClass
import kotlin.reflect.cast

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
            this
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
            this
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
    val args = element?.typeArguments ?: emptyList()
    return resolve().toTypeName(args)
}

private fun KSType.toTypeName(arguments: List<KSTypeArgument> = emptyList()): TypeName {
    val type =
        when (declaration) {
            is KSClassDeclaration -> {
                val typeClassName =
                    ClassName(declaration.packageName.asString(), declaration.simpleName.asString())
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
