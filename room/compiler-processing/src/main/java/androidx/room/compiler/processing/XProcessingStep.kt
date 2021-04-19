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

package androidx.room.compiler.processing

import androidx.room.compiler.processing.javac.JavacElement
import androidx.room.compiler.processing.javac.JavacProcessingEnv
import androidx.room.compiler.processing.ksp.KspElement
import androidx.room.compiler.processing.ksp.KspProcessingEnv
import com.google.auto.common.BasicAnnotationProcessor
import com.google.common.collect.ImmutableSetMultimap
import com.google.devtools.ksp.symbol.KSAnnotated
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element

/**
 * Processing step to simplify processing a set of annotations.
 */
interface XProcessingStep {
    /**
     * The implementation of processing logic for the step. It is guaranteed that the keys in
     * [elementsByAnnotation] will be a subset of the set returned by [annotations].
     *
     * @return the elements (a subset of the values of [elementsByAnnotation]) that this step
     *     is unable to process, possibly until a later processing round. These elements will be
     *     passed back to this step at the next round of processing.
     */
    fun process(
        env: XProcessingEnv,
        elementsByAnnotation: Map<String, Set<XElement>>
    ): Set<XElement>

    /**
     * The set of annotation qualified names processed by this step.
     */
    fun annotations(): Set<String>

    companion object {

        /**
         * Wraps current [XProcessingStep] into an Auto Common
         * [BasicAnnotationProcessor.ProcessingStep].
         */
        @JvmStatic
        fun XProcessingStep.asAutoCommonProcessor(
            env: ProcessingEnvironment,
            targetLanguage: XProcessingEnv.Language = XProcessingEnv.Language.JAVA
        ): BasicAnnotationProcessor.Step {
            return JavacProcessingStepDelegate(
                env = env,
                delegate = this,
                targetLanguage = targetLanguage
            )
        }

        @JvmStatic
        fun XProcessingStep.executeInKsp(env: XProcessingEnv): List<KSAnnotated> {
            check(env is KspProcessingEnv)
            val round = XRoundEnv.create(env)
            val args = annotations().associateWith { annotation ->
                round.getElementsAnnotatedWith(annotation)
            }
            return process(env, args)
                .map { (it as KspElement).declaration }
        }
    }
}

internal class JavacProcessingStepDelegate(
    val env: ProcessingEnvironment,
    val delegate: XProcessingStep,
    val targetLanguage: XProcessingEnv.Language
) : BasicAnnotationProcessor.Step {
    override fun annotations(): Set<String> = delegate.annotations()

    @Suppress("UnstableApiUsage")
    override fun process(
        elementsByAnnotation: ImmutableSetMultimap<String, Element>
    ): Set<Element> {
        val converted = mutableMapOf<String, Set<XElement>>()
        // create a new x processing environment for each step to ensure it can freely cache
        // whatever it wants and we don't keep elements references across rounds.
        val xEnv = JavacProcessingEnv(env, targetLanguage)
        annotations().forEach { annotation ->
            val elements = elementsByAnnotation[annotation].mapNotNull { element ->
                xEnv.wrapAnnotatedElement(element, annotation)
            }.toSet()
            converted[annotation] = elements
        }
        val result = delegate.process(xEnv, converted)
        return result.map {
            (it as JavacElement).element
        }.toSet()
    }
}
