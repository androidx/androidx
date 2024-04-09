/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.room.compiler.processing.javac

import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XRoundEnv
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import kotlin.reflect.KClass

internal class JavacRoundEnv(
    private val env: JavacProcessingEnv,
    val delegate: RoundEnvironment
) : XRoundEnv {
    override val isProcessingOver: Boolean
        get() = delegate.processingOver()

    override fun getElementsAnnotatedWith(klass: KClass<out Annotation>): Set<XElement> {
        val elements = delegate.getElementsAnnotatedWith(klass.java)
        return wrapAnnotatedElements(elements, klass.java.canonicalName)
    }

    override fun getElementsAnnotatedWith(annotationQualifiedName: String): Set<XElement> {
        if (annotationQualifiedName == "*") {
            return emptySet()
        }
        val annotationTypeElement =
            env.elementUtils.getTypeElement(annotationQualifiedName) ?: return emptySet()
        val elements = delegate.getElementsAnnotatedWith(annotationTypeElement)
        return wrapAnnotatedElements(elements, annotationQualifiedName)
    }

    private fun wrapAnnotatedElements(
        elements: Set<Element>,
        annotationName: String
    ): Set<XElement> {
        return elements.map { env.wrapAnnotatedElement(it, annotationName) }.toSet()
    }
}
