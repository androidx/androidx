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

package androidx.room3.compiler.processing.javac

import androidx.room3.compiler.processing.XElement
import androidx.room3.compiler.processing.XRoundEnv
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import kotlin.reflect.KClass

internal class JavacRoundEnv(private val env: JavacProcessingEnv, val delegate: RoundEnvironment) :
    XRoundEnv {
    override val isProcessingOver: Boolean
        get() = delegate.processingOver()

    override fun getElementsAnnotatedWith(klass: KClass<out Annotation>): Set<XElement> {
        val elements = delegate.getElementsAnnotatedWith(klass.java)
        return getElementsAnnotatedWith(elements, klass.java.canonicalName)
    }

    override fun getElementsAnnotatedWith(annotationQualifiedName: String): Set<XElement> {
        if (annotationQualifiedName == "*") {
            return emptySet()
        }
        val annotationTypeElement =
            env.elementUtils.getTypeElement(annotationQualifiedName) ?: return emptySet()
        val elements = delegate.getElementsAnnotatedWith(annotationTypeElement)
        return getElementsAnnotatedWith(elements, annotationQualifiedName)
    }

    private fun getElementsAnnotatedWith(
        elements: Set<Element>,
        annotationName: String,
    ): Set<XElement> = buildSet {
        elements.forEach { element ->
            when (element) {
                is VariableElement ->
                    env.wrapVariableElement(element).let { variableElement ->
                        if (variableElement is JavacPropertyElement) {
                            if (variableElement.hasAnnotation(annotationName)) {
                                add((variableElement))
                            }
                            if (variableElement.backingField.hasAnnotation(annotationName)) {
                                add(variableElement.backingField)
                            }
                        } else {
                            add(variableElement)
                        }
                    }
                is TypeElement -> add(env.wrapTypeElement(element))
                is ExecutableElement -> add(env.wrapExecutableElement(element))
                is PackageElement -> add(JavacPackageElement(env, element))
                else -> error("Unsupported element $element with annotation $annotationName")
            }
        }
    }
}
