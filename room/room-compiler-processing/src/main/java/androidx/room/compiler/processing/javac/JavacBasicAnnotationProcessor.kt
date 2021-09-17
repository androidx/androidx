/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.room.compiler.processing.XBasicAnnotationProcessor
import androidx.room.compiler.processing.XBasicAnnotationProcessor.Companion.getStepDeferredElementsAnnotatedWith
import androidx.room.compiler.processing.XBasicAnnotationProcessor.Companion.reportMissingElements
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XProcessingStep
import com.google.auto.common.SuperficialValidation
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

/**
 * Javac implementation of a [XBasicAnnotationProcessor] with built-in support for validating and
 * deferring elements.
 */
abstract class JavacBasicAnnotationProcessor :
    AbstractProcessor(), XBasicAnnotationProcessor {

    private val xEnv: JavacProcessingEnv by lazy { JavacProcessingEnv(processingEnv) }

    // Cache and lazily get steps during the initial process() so steps initialization is done once.
    private val steps by lazy { processingSteps() }

    private val elementsDeferredBySteps = mutableMapOf<XProcessingStep, Set<XElement>>()
        .withDefault { mutableSetOf() }

    final override val xProcessingEnv: XProcessingEnv get() = xEnv

    final override fun init(processingEnv: ProcessingEnvironment?) {
        super.init(processingEnv)
    }

    final override fun getSupportedAnnotationTypes() = steps.flatMap { it.annotations() }.toSet()

    final override fun process(
        annotations: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment
    ): Boolean {
        val xRoundEnv = JavacRoundEnv(xEnv, roundEnv)
        if (roundEnv.processingOver()) {
            steps.forEach { it.processOver(xEnv) }
            postRound(xEnv, xRoundEnv)
            if (!roundEnv.errorRaised()) {
                // Report missing elements if no error was raised to avoid being noisy.
                reportMissingElements(elementsDeferredBySteps.values.flatten().toSet())
            }
        } else {
            processRound(xRoundEnv)
            postRound(xEnv, xRoundEnv)
            xEnv.clearCache()
        }

        return false
    }

    private fun processRound(xRoundEnv: JavacRoundEnv) {
        val currentElementsDeferredBySteps = steps.associateWith { step ->
            val deferredElements = mutableSetOf<XElement>()
            val elementsByAnnotation = step.annotations().mapNotNull { annotation ->
                if (annotation == "*") {
                    return@mapNotNull null
                }
                val annotatedElements = xRoundEnv.getElementsAnnotatedWith(annotation) +
                    getStepDeferredElementsAnnotatedWith(elementsDeferredBySteps, step, annotation)
                val (validElements, invalidElements) = annotatedElements.partition {
                    SuperficialValidation.validateElement((it as JavacElement).element)
                }
                deferredElements.addAll(invalidElements)
                if (validElements.isNotEmpty()) {
                    annotation to validElements.toSet()
                } else {
                    null
                }
            }.toMap()
            // Only process the step if there are annotated elements found for this step.
            if (elementsByAnnotation.isNotEmpty()) {
                deferredElements + step.process(xEnv, elementsByAnnotation)
            } else {
                deferredElements
            }
        }
        elementsDeferredBySteps.clear()
        elementsDeferredBySteps.putAll(currentElementsDeferredBySteps)
    }
}