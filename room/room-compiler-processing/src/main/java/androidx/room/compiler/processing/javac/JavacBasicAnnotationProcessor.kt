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
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XRoundEnv
import com.google.auto.common.BasicAnnotationProcessor
import com.google.common.collect.ImmutableSetMultimap
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element

/**
 * Javac implementation of a [XBasicAnnotationProcessor] with built-in support for validating and
 * deferring elements via auto-common's [BasicAnnotationProcessor].
 */
abstract class JavacBasicAnnotationProcessor :
    BasicAnnotationProcessor(), XBasicAnnotationProcessor {

    final override fun steps(): Iterable<Step> {
        // Execute all processing steps in a single auto-common Step. This is done to share the
        // XProcessingEnv and its cached across steps in the same round.
        val steps = processingSteps()
        val parentStep = object : Step {
            override fun annotations() = steps.flatMap { it.annotations() }.toSet()

            override fun process(
                elementsByAnnotation: ImmutableSetMultimap<String, Element>
            ): Set<Element> {
                val xEnv = JavacProcessingEnv(processingEnv)
                val convertedElementsByAnnotation = mutableMapOf<String, Set<XElement>>()
                annotations().forEach { annotation ->
                    convertedElementsByAnnotation[annotation] =
                        elementsByAnnotation[annotation].mapNotNull { element ->
                            xEnv.wrapAnnotatedElement(element, annotation)
                        }.toSet()
                }
                val results = steps.flatMap { step ->
                    step.process(
                        env = xEnv,
                        elementsByAnnotation = step.annotations().associateWith {
                            convertedElementsByAnnotation[it] ?: emptySet()
                        }
                    )
                }
                return results.map { (it as JavacElement).element }.toSet()
            }
        }
        return listOf(parentStep)
    }

    final override fun postRound(roundEnv: RoundEnvironment) {
        // Due to BasicAnnotationProcessor taking over AbstractProcessor#process() we can't
        // share the same XProcessingEnv from the steps, but that might be ok...
        val xEnv = JavacProcessingEnv(processingEnv)
        val xRound = XRoundEnv.create(xEnv, roundEnv)
        postRound(xEnv, xRound)
    }
}