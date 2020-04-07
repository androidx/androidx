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

import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.common.MoreElements
import com.google.common.collect.SetMultimap
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

/**
 * Processing step that generates code enabling assisted injection of Workers using Hilt.
 */
class WorkerInjectStep(
    private val processingEnv: ProcessingEnvironment
) : BasicAnnotationProcessor.ProcessingStep {

    override fun annotations() = setOf(WorkerInject::class.java)

    override fun process(
        elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>
    ): MutableSet<out Element> {
        val parsedElements = mutableSetOf<TypeElement>()
        elementsByAnnotation[WorkerInject::class.java].forEach { element ->
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
        return mutableSetOf()
    }

    private fun parse(
        typeElement: TypeElement,
        constructorElement: ExecutableElement
    ): WorkerInjectElements? {
        // TODO(danysantiago): Validate Worker
        return WorkerInjectElements(typeElement, constructorElement)
    }
}