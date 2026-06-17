/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.room3.compiler.processing.XFieldElement
import androidx.room3.compiler.processing.javac.kotlin.KmPropertyContainer
import androidx.room3.compiler.processing.javac.kotlin.KmTypeContainer
import androidx.room3.compiler.processing.javac.kotlin.descriptor
import javax.lang.model.element.VariableElement
import kotlin.reflect.KClass

internal class JavacFieldElement(
    env: JavacProcessingEnv,
    element: VariableElement,
    override val owner: JavacPropertyElement,
) : JavacVariableElement(env, element), XFieldElement {

    override val jvmDescriptor: String
        get() = element.descriptor(env.delegate)

    override fun <T : Annotation> getAnnotations(
        annotation: KClass<T>,
        containerAnnotation: KClass<out Annotation>?,
    ): List<XAnnotation> = buildList {
        if (env.config.includePropertyAnnotationsInFields) {
            addAll(owner.getAnnotations(annotation, containerAnnotation))
        }
        addAll(super<JavacVariableElement>.getAnnotations(annotation, containerAnnotation))
    }

    override fun getAllAnnotations(): List<XAnnotation> = buildList {
        if (env.config.includePropertyAnnotationsInFields) {
            addAll(owner.getAllAnnotations())
        }
        addAll(super.getAllAnnotations())
    }

    override fun hasAnnotation(
        annotation: KClass<out Annotation>,
        containerAnnotation: KClass<out Annotation>?,
    ): Boolean {
        val ownerHasAnnotation =
            if (env.config.includePropertyAnnotationsInFields) {
                owner.hasAnnotation(annotation, containerAnnotation)
            } else {
                false
            }
        return ownerHasAnnotation ||
            super<JavacVariableElement>.hasAnnotation(annotation, containerAnnotation)
    }

    override val kotlinType: KmTypeContainer?
        get() = owner.kotlinType

    override val kotlinMetadata: KmPropertyContainer?
        get() = owner.kotlinMetadata

    override val name: String
        get() = owner.name

    override val fallbackLocationText: String
        get() = owner.fallbackLocationText

    override val enclosingElement: JavacTypeElement
        get() = owner.enclosingElement

    override val closestMemberContainer: JavacTypeElement
        get() = owner.closestMemberContainer
}
