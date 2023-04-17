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
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Modifier

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
    return this.resolve().declaration is KSTypeParameter
}

fun KSType.isInline() = declaration.modifiers.contains(Modifier.INLINE)

internal fun KSType.withNullability(nullability: XNullability) = when (nullability) {
    XNullability.NULLABLE -> makeNullable()
    XNullability.NONNULL -> makeNotNullable()
    else -> throw IllegalArgumentException("Cannot set KSType nullability to platform")
}

private fun KSAnnotated.hasAnnotation(qName: String) =
    annotations.any { it.hasQualifiedName(qName) }

private fun KSAnnotation.hasQualifiedName(qName: String): Boolean {
    return annotationType.resolve().hasQualifiedName(qName)
}

private fun KSType.hasQualifiedName(qName: String): Boolean {
    return declaration.qualifiedName?.asString() == qName
}

internal fun KSAnnotated.hasJvmWildcardAnnotation() =
    hasAnnotation(JvmWildcard::class.java.canonicalName!!)

internal fun KSAnnotated.hasSuppressJvmWildcardAnnotation() =
    hasAnnotation(JvmSuppressWildcards::class.java.canonicalName!!)

private fun KSType.hasAnnotation(qName: String) = annotations.any { it.hasQualifiedName(qName) }

internal fun KSType.hasJvmWildcardAnnotation() =
    hasAnnotation(JvmWildcard::class.java.canonicalName!!)

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