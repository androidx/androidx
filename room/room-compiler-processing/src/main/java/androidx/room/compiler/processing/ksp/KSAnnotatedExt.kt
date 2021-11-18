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

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration

private fun KSAnnotated.hasAnnotationWithQName(qName: String) = annotations.any {
    it.annotationType.resolve().declaration.qualifiedName?.asString() == qName
}

internal fun KSAnnotated.hasJvmStaticAnnotation() = hasAnnotationWithQName("kotlin.jvm.JvmStatic")

internal fun KSAnnotated.hasJvmTransientAnnotation() =
    hasAnnotationWithQName("kotlin.jvm.Transient")

internal fun KSAnnotated.hasJvmFieldAnnotation() = hasAnnotationWithQName("kotlin.jvm.JvmField")

internal fun KSAnnotated.hasJvmDefaultAnnotation() = hasAnnotationWithQName("kotlin.jvm.JvmDefault")

/**
 * Return a reference to the containing file or class declaration via a wrapper that implements the
 * [javax.lang.model.element.Element] API so that we can report it to JavaPoet.
 */
internal fun KSAnnotated.wrapAsOriginatingElement(): OriginatingElementWrapper? {
    val ksDeclaration = this as? KSDeclaration ?: return null

    return ksDeclaration.containingFile?.let {
        KSFileAsOriginatingElement(it)
    } ?: (ksDeclaration as? KSClassDeclaration)?.let {
        KSClassDeclarationAsOriginatingElement(it)
    }
}
