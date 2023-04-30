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
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference

/**
 * Root package comes as <root> instead of "" so we work around it here.
 */
internal fun KSDeclaration.getNormalizedPackageName(): String {
    return packageName.asString().let {
        if (it == "<root>") {
            ""
        } else {
            it
        }
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

internal fun KSType.withNullability(nullability: XNullability) = when (nullability) {
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
        val ksType = if (
            // Note: Technically, we could just make KSTypeWrapper internal and cast to get the
            // delegate, but since we need to use reflection anyway, just get it via reflection.
            this.javaClass.canonicalName == "androidx.room.compiler.processing.ksp.KSTypeWrapper") {
            this.javaClass.methods.find { it.name == "getDelegate" }?.invoke(this)
        } else {
            this
        }
        val kotlinType =
            ksType?.javaClass?.methods?.find { it.name == "getKotlinType" }?.invoke(ksType)
        val kotlinAnnotations =
            kotlinType?.javaClass
                ?.methods
                ?.find { it.name == "getAnnotations" }
                ?.invoke(kotlinType)
        return kotlinAnnotations?.javaClass
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