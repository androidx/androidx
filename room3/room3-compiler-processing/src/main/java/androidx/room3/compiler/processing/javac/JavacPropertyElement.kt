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

package androidx.room3.compiler.processing.javac

import androidx.room3.compiler.processing.XAnnotation
import androidx.room3.compiler.processing.XMethodElement
import androidx.room3.compiler.processing.XPropertyElement
import androidx.room3.compiler.processing.javac.kotlin.KmPropertyContainer
import androidx.room3.compiler.processing.javac.kotlin.KmTypeContainer
import javax.lang.model.element.VariableElement
import kotlin.reflect.KClass

internal class JavacPropertyElement(env: JavacProcessingEnv, element: VariableElement) :
    JavacVariableElement(env, element), XPropertyElement {

    override val name: String
        get() = kotlinMetadata?.name ?: element.simpleName.toString()

    override val backingField: JavacFieldElement by lazy { JavacFieldElement(env, element, this) }

    override fun <T : Annotation> getAnnotations(
        annotation: KClass<T>,
        containerAnnotation: KClass<out Annotation>?,
    ): List<XAnnotation> {
        return syntheticMethodForAnnotations?.getAnnotations(annotation, containerAnnotation)
            ?: emptyList()
    }

    override fun getAllAnnotations(): List<XAnnotation> {
        // For Kotlin sources, annotations placed on properties will appear on synthetic
        // "$annotations" methods in the KAPT stub rather than on the field so we append these
        // annotations to match KSP. Note that the synthetic "$annotations" method isn't
        // accessible on precompiled classes in KAPT due to
        // https://youtrack.jetbrains.com/issue/KT-34684, so they will still be missing in that
        // case, but there's nothing we can really do about that.
        return syntheticMethodForAnnotations
            ?.getAllAnnotations()
            ?.filter { it.qualifiedName != "java.lang.Deprecated" }
            ?.toList() ?: emptyList()
    }

    override fun hasAnnotation(
        annotation: KClass<out Annotation>,
        containerAnnotation: KClass<out Annotation>?,
    ): Boolean {
        return syntheticMethodForAnnotations?.hasAnnotation(annotation, containerAnnotation)
            ?: false
    }

    override fun hasAnnotationWithPackage(pkg: String): Boolean {
        return syntheticMethodForAnnotations?.hasAnnotationWithPackage(pkg) ?: false
    }

    override val kotlinMetadata: KmPropertyContainer? by lazy {
        enclosingElement.kotlinMetadata?.getPropertyMetadata(element)
            // If the metadata isn't in the enclosing class, check the companion object next.
            ?: enclosingElement.companionObject?.kotlinMetadata?.getPropertyMetadata(element)
    }

    private val syntheticMethodForAnnotations: JavacMethodElement? by lazy {
        val expectedName =
            kotlinMetadata?.syntheticMethodForAnnotations?.name
                ?: (element.simpleName.toString() + "\$annotations")
        enclosingElement.getSyntheticMethodsForAnnotations().singleOrNull {
            it.name == expectedName
        }
    }

    override val kotlinType: KmTypeContainer?
        get() = kotlinMetadata?.type

    override val enclosingElement: JavacTypeElement by lazy { element.requireEnclosingType(env) }

    override val closestMemberContainer: JavacTypeElement
        get() = enclosingElement

    override val getter: XMethodElement? by lazy {
        kotlinMetadata?.getter?.let { getterMetadata ->
            enclosingElement
                .getDeclaredMethods()
                .filter { it.isKotlinPropertyMethod() }
                .firstOrNull { method -> method.jvmName == getterMetadata.jvmName }
        }
    }

    override val setter: XMethodElement? by lazy {
        kotlinMetadata?.setter?.let { setterMetadata ->
            enclosingElement
                .getDeclaredMethods()
                .filter { it.isKotlinPropertyMethod() }
                .firstOrNull { method -> method.jvmName == setterMetadata.jvmName }
        }
    }

    override fun isPublic(): Boolean {
        return kotlinMetadata?.isPublic() ?: super.isPublic()
    }

    override fun isPrivate(): Boolean {
        return kotlinMetadata?.isPrivate() ?: super.isPrivate()
    }
}
