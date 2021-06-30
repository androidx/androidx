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
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XRoundEnv
import com.google.devtools.ksp.isLocal
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.validate

/**
 * KSP implementation of a [XBasicAnnotationProcessor] with built-in support for validating and
 * deferring symbols.
 */
abstract class KspBasicAnnotationProcessor(
    val symbolProcessorEnvironment: SymbolProcessorEnvironment
) : SymbolProcessor, XBasicAnnotationProcessor {

    final override fun process(resolver: Resolver): List<KSAnnotated> {
        val processingEnv = XProcessingEnv.create(
            symbolProcessorEnvironment.options,
            resolver,
            symbolProcessorEnvironment.codeGenerator,
            symbolProcessorEnvironment.logger
        )
        val round = XRoundEnv.create(processingEnv)
        val deferredElements = processingSteps().flatMap { step ->
            val invalidElements = mutableSetOf<XElement>()
            val elementsByAnnotation = step.annotations().associateWith { annotation ->
                val annotatedElements = round.getElementsAnnotatedWith(annotation)
                annotatedElements
                    .filter { (it as KspElement).declaration.validateExceptLocals() }
                    .also { invalidElements.addAll(annotatedElements - it) }
                    .toSet()
            }
            invalidElements + step.process(processingEnv, elementsByAnnotation)
        }
        postRound(processingEnv, round)
        return deferredElements.map { (it as KspElement).declaration }
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