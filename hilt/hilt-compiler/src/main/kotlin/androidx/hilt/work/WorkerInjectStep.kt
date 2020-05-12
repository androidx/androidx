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

package androidx.hilt.work

import androidx.hilt.AndroidXHiltProcessor
import androidx.hilt.ClassNames
import androidx.hilt.ext.hasAnnotation
import com.google.auto.common.MoreElements
import com.squareup.javapoet.TypeName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.NestingKind
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter
import javax.tools.Diagnostic

/**
 * Processing step that generates code enabling assisted injection of Workers using Hilt.
 */
class WorkerInjectStep(
    private val processingEnv: ProcessingEnvironment
) : AndroidXHiltProcessor.Step {

    private val elements = processingEnv.elementUtils
    private val types = processingEnv.typeUtils
    private val messager = processingEnv.messager

    override fun annotation() = ClassNames.WORKER_INJECT.canonicalName()

    override fun process(annotatedElements: Set<Element>) {
        val parsedElements = mutableSetOf<TypeElement>()
        annotatedElements.forEach { element ->
            val constructorElement =
                MoreElements.asExecutable(element)
            val typeElement =
                MoreElements.asType(constructorElement.enclosingElement)
            if (parsedElements.add(typeElement)) {
                parse(typeElement, constructorElement)?.let { worker ->
                    WorkerGenerator(
                        processingEnv,
                        worker
                    ).generate()
                }
            }
        }
    }

    private fun parse(
        typeElement: TypeElement,
        constructorElement: ExecutableElement
    ): WorkerInjectElements? {
        var valid = true

        if (elements.getTypeElement(ClassNames.WORKER_ASSISTED_FACTORY.toString()) == null) {
            error("To use @WorkerInject you must add the 'work' artifact. " +
                    "androidx.hilt:hilt-work:<version>")
            valid = false
        }

        if (!types.isSubtype(typeElement.asType(),
                elements.getTypeElement(ClassNames.WORKER.toString()).asType())) {
            error("@WorkerInject is only supported on types that subclass " +
                    "${ClassNames.WORKER}.")
            valid = false
        }

        ElementFilter.constructorsIn(typeElement.enclosedElements).filter {
            it.hasAnnotation(ClassNames.WORKER_INJECT.canonicalName())
        }.let { constructors ->
            if (constructors.size > 1) {
                error("Multiple @WorkerInject annotated constructors found.", typeElement)
                valid = false
            }
            constructors.filter { it.modifiers.contains(Modifier.PRIVATE) }.forEach {
                error("@WorkerInject annotated constructors must not be private.", it)
                valid = false
            }
        }

        if (typeElement.nestingKind == NestingKind.MEMBER &&
            !typeElement.modifiers.contains(Modifier.STATIC)) {
            error("@WorkerInject may only be used on inner classes if they are static.",
                typeElement)
            valid = false
        }

        constructorElement.parameters.filter {
            TypeName.get(it.asType()) == ClassNames.CONTEXT
        }.apply {
            if (size != 1) {
                error("Expected exactly one constructor argument of type " +
                        "${ClassNames.CONTEXT}, found $size", constructorElement)
                valid = false
            }
            firstOrNull()?.let {
                if (!it.hasAnnotation(ClassNames.ASSISTED.canonicalName())) {
                    error("Missing @Assisted annotation in param '${it.simpleName}'.", it)
                    valid = false
                }
            }
        }

        constructorElement.parameters.filter {
            TypeName.get(it.asType()) == ClassNames.WORKER_PARAMETERS
        }.apply {
            if (size != 1) {
                error("Expected exactly one constructor argument of type " +
                        "${ClassNames.WORKER_PARAMETERS}, found $size", constructorElement)
                valid = false
            }
            firstOrNull()?.let {
                if (!it.hasAnnotation(ClassNames.ASSISTED.canonicalName())) {
                    error("Missing @Assisted annotation in param '${it.simpleName}'.", it)
                    valid = false
                }
            }
        }

        if (!valid) return null

        return WorkerInjectElements(typeElement, constructorElement)
    }

    private fun error(message: String, element: Element? = null) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element)
    }
}