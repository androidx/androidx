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
import javax.lang.model.element.Modifier
import javax.lang.model.element.NestingKind
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter
import javax.tools.Diagnostic

/**
 * Processing step that generates code enabling assisted injection of Workers using Hilt.
 */
class WorkerStep(
    private val processingEnv: ProcessingEnvironment
) : AndroidXHiltProcessor.Step {

    private val elements = processingEnv.elementUtils
    private val types = processingEnv.typeUtils
    private val messager = processingEnv.messager

    override fun annotation() = ClassNames.HILT_WORKER.canonicalName()

    override fun process(annotatedElements: Set<Element>) {
        val parsedElements = mutableSetOf<TypeElement>()
        annotatedElements.forEach { element ->
            val typeElement = MoreElements.asType(element)
            if (parsedElements.add(typeElement)) {
                parse(typeElement)?.let { worker ->
                    WorkerGenerator(
                        processingEnv,
                        worker
                    ).generate()
                }
            }
        }
    }

    private fun parse(typeElement: TypeElement): WorkerElements? {
        var valid = true

        if (elements.getTypeElement(ClassNames.WORKER_ASSISTED_FACTORY.toString()) == null) {
            error(
                "To use @HiltWorker you must add the 'work' artifact. " +
                    "androidx.hilt:hilt-work:<version>"
            )
            valid = false
        }

        if (!types.isSubtype(
                typeElement.asType(),
                elements.getTypeElement(ClassNames.LISTENABLE_WORKER.toString()).asType()
            )
        ) {
            error(
                "@HiltWorker is only supported on types that subclass " +
                    "${ClassNames.LISTENABLE_WORKER}."
            )
            valid = false
        }

        val constructors = ElementFilter.constructorsIn(typeElement.enclosedElements).filter {
            if (it.hasAnnotation(ClassNames.INJECT.canonicalName())) {
                error(
                    "Worker constructor should be annotated with @AssistedInject instead of " +
                        "@Inject."
                )
                valid = false
            }
            it.hasAnnotation(ClassNames.ASSISTED_INJECT.canonicalName())
        }
        if (constructors.size != 1) {
            error(
                "@HiltWorker annotated class should contain exactly one @AssistedInject " +
                    "annotated constructor.",
                typeElement
            )
            valid = false
        }
        constructors.filter { it.modifiers.contains(Modifier.PRIVATE) }.forEach {
            error("@AssistedInject annotated constructors must not be private.", it)
            valid = false
        }

        if (typeElement.nestingKind == NestingKind.MEMBER &&
            !typeElement.modifiers.contains(Modifier.STATIC)
        ) {
            error(
                "@HiltWorker may only be used on inner classes if they are static.",
                typeElement
            )
            valid = false
        }

        if (!valid) return null

        val injectConstructor = constructors.first()
        var contextIndex = -1
        var workerParametersIndex = -1
        injectConstructor.parameters.forEachIndexed { index, param ->
            if (TypeName.get(param.asType()) == ClassNames.CONTEXT) {
                if (!param.hasAnnotation(ClassNames.ASSISTED.canonicalName())) {
                    error("Missing @Assisted annotation in param '${param.simpleName}'.", param)
                    valid = false
                }
                contextIndex = index
            }
            if (TypeName.get(param.asType()) == ClassNames.WORKER_PARAMETERS) {
                if (!param.hasAnnotation(ClassNames.ASSISTED.canonicalName())) {
                    error("Missing @Assisted annotation in param '${param.simpleName}'.", param)
                    valid = false
                }
                workerParametersIndex = index
            }
        }
        if (contextIndex > workerParametersIndex) {
            error(
                "The 'Context' parameter must be declared before the 'WorkerParameters' in the " +
                    "@AssistedInject constructor of a @HiltWorker annotated class.",
                injectConstructor
            )
        }

        if (!valid) return null

        return WorkerElements(typeElement, injectConstructor)
    }

    private fun error(message: String, element: Element? = null) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element)
    }
}