/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions.compiler

import androidx.appfunctions.compiler.core.AnnotatedAppFunctions
import androidx.appfunctions.compiler.core.AppFunctionSymbolResolver
import androidx.appfunctions.compiler.core.ProcessingException
import androidx.appfunctions.compiler.core.SymbolNotReadyException
import androidx.appfunctions.compiler.core.logException
import androidx.appfunctions.compiler.processors.AppFunctionAggregateProcessor
import androidx.appfunctions.compiler.processors.AppFunctionComponentRegistryProcessor
import androidx.appfunctions.compiler.processors.AppFunctionIdProcessor
import androidx.appfunctions.compiler.processors.AppFunctionInventoryProcessor
import androidx.appfunctions.compiler.processors.AppFunctionInvokerProcessor
import androidx.appfunctions.compiler.processors.AppFunctionSchemaInventoryProcessor
import androidx.appfunctions.compiler.processors.AppFunctionSerializableProcessor
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.AnnotationSpec
import javax.annotation.processing.Generated

/** The compiler to process AppFunction implementations. */
class AppFunctionCompiler(
    private val processors: List<SymbolProcessor>,
    private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        return try {
            val deferred = shouldDeferAllProcessing(resolver)
            if (deferred.isNotEmpty()) {
                deferred
            } else {
                buildList {
                    for (processor in processors) {
                        addAll(processor.process(resolver))
                    }
                }
            }
        } catch (e: ProcessingException) {
            logger.logException(e)
            emptyList()
        }
    }

    /**
     * Returns a non-empty list of [KSAnnotated] nodes if the processor should defer all these
     * symbols.
     *
     * To ensure that all generated components are recorded in AppFunctionComponentRegistry in each
     * compilation unit, the processor should start the processing only when all the nodes are
     * ready.
     */
    private fun shouldDeferAllProcessing(resolver: Resolver): List<KSAnnotated> {
        val appFunctionSymbolResolver = AppFunctionSymbolResolver(resolver)
        val annotatedAppFunctions = appFunctionSymbolResolver.resolveAnnotatedAppFunctions()
        for (annotatedAppFunction in annotatedAppFunctions) {
            try {
                annotatedAppFunction.validate()
            } catch (e: SymbolNotReadyException) {
                logger.logging(e.message.toString(), e.node)
                return annotatedAppFunctions.flatMap(AnnotatedAppFunctions::getAllAnnotated)
            }
        }
        return emptyList()
    }

    class Provider : SymbolProcessorProvider {

        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
            val options = AppFunctionCompilerOptions.from(environment.options)

            val functionRegistryProcessor =
                AppFunctionComponentRegistryProcessor(environment.codeGenerator)
            val idProcessor = AppFunctionIdProcessor(environment.codeGenerator)
            val inventoryProcessor = AppFunctionInventoryProcessor(environment.codeGenerator)
            val invokerProcessor = AppFunctionInvokerProcessor(environment.codeGenerator)
            val entityProcessor =
                AppFunctionSerializableProcessor(environment.codeGenerator, environment.logger)
            val aggregateProcessor =
                AppFunctionAggregateProcessor(options, environment.codeGenerator)
            val schemaInventoryProcessor =
                AppFunctionSchemaInventoryProcessor(environment.codeGenerator, options)
            return AppFunctionCompiler(
                listOf(
                    functionRegistryProcessor,
                    idProcessor,
                    inventoryProcessor,
                    invokerProcessor,
                    entityProcessor,
                    aggregateProcessor,
                    schemaInventoryProcessor,
                ),
                environment.logger,
            )
        }
    }

    companion object {
        internal val GENERATED_ANNOTATION =
            AnnotationSpec.builder(Generated::class)
                .addMember("%S", AppFunctionCompiler::class.java.canonicalName)
                .build()
    }
}
