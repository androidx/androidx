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
import androidx.room.compiler.processing.XProcessingStep
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

    // This state is cached here so that it can be shared by all steps in a given processing round.
    // The state is initialized at beginning of each round using the InitializingStep, and
    // the state is cleared at the end of each round in BasicAnnotationProcessor#postRound()
    private var cachedXEnv: JavacProcessingEnv? = null

    final override fun steps(): Iterable<Step> {
        return processingSteps().map { DelegatingStep(it) }
    }

    /** A [Step] that delegates to an [XProcessingStep]. */
    private inner class DelegatingStep(val xStep: XProcessingStep) : Step {
        override fun annotations() = xStep.annotations()

        override fun process(
            elementsByAnnotation: ImmutableSetMultimap<String, Element>
        ): Set<Element> {
            // The first step in a round initializes the cachedXEnv. Note: the "first" step can
            // change each round depending on which annotations are present in the current round and
            // which elements were deferred in the previous round.
            val xEnv = cachedXEnv ?: JavacProcessingEnv(processingEnv).also { cachedXEnv = it }
            val xElementsByAnnotation = mutableMapOf<String, Set<XElement>>()
            xStep.annotations().forEach { annotation ->
                xElementsByAnnotation[annotation] =
                    elementsByAnnotation[annotation].mapNotNull { element ->
                        xEnv.wrapAnnotatedElement(element, annotation)
                    }.toSet()
            }
            return xStep.process(xEnv, xElementsByAnnotation).map {
                (it as JavacElement).element
            }.toSet()
        }
    }

    final override fun postRound(roundEnv: RoundEnvironment) {
        // The cachedXEnv can be null if none of the steps were processed in the round.
        // In this case, we just create a new one since there is no cached one to share.
        val xEnv = cachedXEnv ?: JavacProcessingEnv(processingEnv)
        val xRound = XRoundEnv.create(xEnv, roundEnv)
        postRound(xEnv, xRound)
        cachedXEnv = null // Reset after every round to allow GC
    }
}