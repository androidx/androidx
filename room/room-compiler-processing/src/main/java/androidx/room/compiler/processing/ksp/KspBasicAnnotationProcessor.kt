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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.XBasicAnnotationProcessor
import androidx.room.compiler.processing.XBasicAnnotationProcessor.Companion.getStepDeferredElementsAnnotatedWith
import androidx.room.compiler.processing.XBasicAnnotationProcessor.Companion.reportMissingElements
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XProcessingStep
import com.google.devtools.ksp.isLocal
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.validate

/**
 * KSP implementation of a [XBasicAnnotationProcessor] with built-in support for validating and
 * deferring symbols.
 */
abstract class KspBasicAnnotationProcessor(
    val symbolProcessorEnvironment: SymbolProcessorEnvironment
) : SymbolProcessor, XBasicAnnotationProcessor {

    private val logger = DelegateLogger(symbolProcessorEnvironment.logger)

    private val xEnv = KspProcessingEnv(
        symbolProcessorEnvironment.options,
        symbolProcessorEnvironment.codeGenerator,
        logger
    )

    final override val xProcessingEnv: XProcessingEnv get() = xEnv

    // Cache and lazily get steps during the initial process() so steps initialization is done once.
    private val steps by lazy { processingSteps() }

    private val elementsDeferredBySteps = mutableMapOf<XProcessingStep, Set<XElement>>()
        .withDefault { mutableSetOf() }

    final override fun process(resolver: Resolver): List<KSAnnotated> {
        xEnv.resolver = resolver // Set the resolver at the beginning of each round
        val xRoundEnv = KspRoundEnv(xEnv, false)
        processRound(xRoundEnv)
        postRound(xEnv, xRoundEnv)
        xEnv.clearCache() // Reset cache after every round to avoid leaking elements across rounds
        return elementsDeferredBySteps.values.flatten().map { (it as KspElement).declaration }
    }

    private fun processRound(xRoundEnv: KspRoundEnv) {
        val currentElementsDeferredBySteps = steps.associateWith { step ->
            val deferredElements = mutableSetOf<XElement>()
            val elementsByAnnotation = step.annotations().mapNotNull { annotation ->
                if (annotation == "*") {
                    return@mapNotNull null
                }
                val annotatedElements = xRoundEnv.getElementsAnnotatedWith(annotation) +
                    getStepDeferredElementsAnnotatedWith(elementsDeferredBySteps, step, annotation)
                val (validElements, invalidElements) = annotatedElements.partition {
                    (it as KspElement).declaration.validateExceptLocals()
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

    final override fun finish() {
        steps.forEach { it.processOver(xEnv) }
        postRound(xEnv, KspRoundEnv(xEnv, true))
        if (!logger.hasError) {
            // Report missing elements if no error was raised to avoid being noisy.
            reportMissingElements(elementsDeferredBySteps.values.flatten().toSet())
        }
    }

    // KSPLogger delegate to keep track if an error was raised or not.
    private class DelegateLogger(val delegate: KSPLogger) : KSPLogger by delegate {
        var hasError = false
        override fun error(message: String, symbol: KSNode?) {
            hasError = true
            delegate.error(message, symbol)
        }
        override fun exception(e: Throwable) {
            hasError = true
            delegate.exception(e)
        }
    }
}

/**
 * TODO remove this once we update to KSP beta03
 * https://github.com/google/ksp/pull/479
 */
private fun KSAnnotated.validateExceptLocals(): Boolean {
    return this.validate { parent, current ->
        // skip locals
        // https://github.com/google/ksp/issues/489
        val skip = (parent as? KSDeclaration)?.isLocal() == true ||
            (current as? KSDeclaration)?.isLocal() == true
        !skip
    }
}