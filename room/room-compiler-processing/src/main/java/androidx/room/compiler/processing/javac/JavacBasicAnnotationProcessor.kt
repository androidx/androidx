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

import androidx.room.compiler.processing.CommonProcessorDelegate
import androidx.room.compiler.processing.XBasicAnnotationProcessor
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XProcessingEnvConfig
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

/**
 * Javac implementation of a [XBasicAnnotationProcessor] with built-in support for validating and
 * deferring elements.
 */
abstract class JavacBasicAnnotationProcessor @JvmOverloads constructor(
    configureEnv: (Map<String, String>) -> XProcessingEnvConfig = { XProcessingEnvConfig.DEFAULT }
) : AbstractProcessor(), XBasicAnnotationProcessor {
    constructor(config: XProcessingEnvConfig) : this({ config })

    private val xEnv: JavacProcessingEnv by lazy {
        JavacProcessingEnv(processingEnv, configureEnv(processingEnv.options))
    }

    // Cache and lazily get steps during the initial process() so steps initialization is done once.
    private val steps by lazy { processingSteps().toList() }

    private val commonDelegate by lazy { CommonProcessorDelegate(this.javaClass, xEnv, steps) }

    final override val xProcessingEnv: XProcessingEnv
        get() = xEnv

    final override fun init(processingEnv: ProcessingEnvironment?) {
        super.init(processingEnv)
        initialize(xEnv)
    }

    final override fun getSupportedAnnotationTypes() = steps.flatMap { it.annotations() }.toSet()

    final override fun process(
        annotations: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment
    ): Boolean {
        val xRoundEnv = JavacRoundEnv(xEnv, roundEnv)
        if (roundEnv.processingOver()) {
            val missingElements = commonDelegate.processLastRound()
            postRound(xEnv, xRoundEnv)
            if (!xProcessingEnv.config.disableAnnotatedElementValidation &&
                !roundEnv.errorRaised()
            ) {
                // Report missing elements if no error was raised to avoid being noisy.
                commonDelegate.reportMissingElements(missingElements)
            }
        } else {
            commonDelegate.processRound(xRoundEnv)
            postRound(xEnv, xRoundEnv)
            xEnv.clearCache()
        }

        return false
    }
}