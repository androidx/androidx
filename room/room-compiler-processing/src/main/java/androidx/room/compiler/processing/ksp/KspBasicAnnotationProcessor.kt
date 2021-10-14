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

import androidx.room.compiler.processing.CommonProcessorDelegate
import androidx.room.compiler.processing.XBasicAnnotationProcessor
import androidx.room.compiler.processing.XProcessingEnv
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSNode

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

    // Cache and lazily get steps during the initial process() so steps initialization is done once.
    private val steps by lazy { processingSteps().toList() }

    private val commonDelegate by lazy { CommonProcessorDelegate(this.javaClass, xEnv, steps) }

    final override val xProcessingEnv: XProcessingEnv get() = xEnv

    final override fun process(resolver: Resolver): List<KSAnnotated> {
        xEnv.resolver = resolver // Set the resolver at the beginning of each round
        val xRoundEnv = KspRoundEnv(xEnv, false)
        commonDelegate.processRound(xRoundEnv)
        postRound(xEnv, xRoundEnv)
        xEnv.clearCache() // Reset cache after every round to avoid leaking elements across rounds
        // TODO(b/201307003): Use KSP deferring API.
        // For now don't defer symbols since this impl of basic annotation processor mimics
        // javac's impl where elements are deferred by remembering the name of the closest enclosing
        // type element and later in a subsequent round finding the type element using the
        // Resolver and then searching it for annotations requested by the steps.
        return emptyList()
    }

    final override fun finish() {
        val xRoundEnv = KspRoundEnv(xEnv, true)
        val missingElements = commonDelegate.processLastRound()
        postRound(xEnv, xRoundEnv)
        if (!logger.hasError) {
            // Report missing elements if no error was raised to avoid being noisy.
            commonDelegate.reportMissingElements(missingElements)
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