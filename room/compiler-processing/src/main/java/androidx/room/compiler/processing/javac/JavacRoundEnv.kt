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

import androidx.annotation.VisibleForTesting
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XRoundEnv
import androidx.room.compiler.processing.XTypeElement
import com.google.auto.common.MoreElements
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import kotlin.reflect.KClass

@Suppress("UnstableApiUsage")
@VisibleForTesting
internal class JavacRoundEnv(
    private val env: JavacProcessingEnv,
    val delegate: RoundEnvironment
) : XRoundEnv {
    override val rootElements: Set<XElement> by lazy {
        delegate.rootElements.map {
            check(MoreElements.isType(it))
            env.wrapTypeElement(MoreElements.asType(it))
        }.toSet()
    }

    override fun getTypeElementsAnnotatedWith(annotationQualifiedName: String): Set<XTypeElement> {
        val element = env.elementUtils.getTypeElement(annotationQualifiedName)
            ?: error("Cannot find TypeElement: $annotationQualifiedName")
        val result = delegate.getElementsAnnotatedWith(element)
        return result.filter {
            MoreElements.isType(it)
        }.map {
            env.wrapTypeElement(MoreElements.asType(it))
        }.toSet()
    }

    override fun getTypeElementsAnnotatedWith(klass: KClass<out Annotation>): Set<XTypeElement> {
        return getTypeElementsAnnotatedWith(
            annotationQualifiedName = klass.java.name
        )
    }

    override fun getElementsAnnotatedWith(klass: KClass<out Annotation>): Set<XElement> {
        val elements = delegate.getElementsAnnotatedWith(klass.java)
        return wrapElements(elements, annotationName = { klass.java.name })
    }

    override fun getElementsAnnotatedWith(annotationQualifiedName: String): Set<XElement> {
        val element = env.elementUtils.getTypeElement(annotationQualifiedName)
            ?: error("Cannot find TypeElement: $annotationQualifiedName")

        val elements = delegate.getElementsAnnotatedWith(element)

        return wrapElements(elements, annotationName = { annotationQualifiedName })
    }

    private inline fun wrapElements(
        result: Set<Element>,
        annotationName: () -> String
    ): Set<XElement> {

        return result.map { element ->
            when (element) {
                is VariableElement -> {
                    env.wrapVariableElement(element)
                }
                is TypeElement -> {
                    env.wrapTypeElement(element)
                }
                is ExecutableElement -> {
                    env.wrapExecutableElement(element)
                }
                is PackageElement -> {
                    error(
                        "Cannot get elements with annotation ${annotationName()}. Package " +
                            "elements are not supported by KSP."
                    )
                }
                else -> error("Unsupported element $element with annotation ${annotationName()}")
            }
        }.toSet()
    }
}
