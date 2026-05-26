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

package androidx.appfunctions.compiler.processors

import androidx.appfunctions.compiler.AppFunctionCompiler
import androidx.appfunctions.compiler.AppFunctionCompilerOptions
import androidx.appfunctions.compiler.core.AppFunctionSymbolResolver
import androidx.appfunctions.compiler.core.IntrospectionHelper.APP_FUNCTIONS_INTERNAL_PACKAGE_NAME
import androidx.appfunctions.compiler.core.IntrospectionHelper.APP_FUNCTIONS_SERVICE_INTERNAL_PACKAGE_NAME
import androidx.appfunctions.compiler.core.IntrospectionHelper.APP_FUNCTION_INVENTORY_CLASS
import androidx.appfunctions.compiler.core.IntrospectionHelper.AggregatedAppFunctionInventoryClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.AggregatedAppFunctionInvokerClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionInvokerClass
import androidx.appfunctions.compiler.core.toClassName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock

/**
 * The processor generates the implementation of AggregatedAppFunctionInventory and
 * AggregatedAppFunctionInvoker.
 *
 * The processor would only start aggregation process when
 * * [AppFunctionCompilerOptions.aggregateAppFunctions] is true.
 * * AND there is no remaining @AppFunction nodes to processed.
 */
class AppFunctionAggregateProcessor(
    private val options: AppFunctionCompilerOptions,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    private var hasProcessed = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (!options.aggregateAppFunctions) {
            return emptyList()
        }

        if (hasProcessed) {
            return emptyList()
        }

        if (!shouldProcess(resolver)) {
            return emptyList()
        }

        generateAggregatedAppFunctionInventory(resolver)
        generateAggregatedAppFunctionInvoker(resolver)
        generateAggregatedIndexXml(resolver)

        hasProcessed = true
        return emptyList()
    }

    private fun generateAggregatedAppFunctionInventory(resolver: Resolver) {
        val generatedInventories =
            AppFunctionSymbolResolver(resolver).getGeneratedAppFunctionInventories()
        val aggregatedInventoryClassName =
            "${'$'}${AggregatedAppFunctionInventoryClass.CLASS_NAME.simpleName}_Impl"

        val aggregatedInventoryClassBuilder = TypeSpec.classBuilder(aggregatedInventoryClassName)
        aggregatedInventoryClassBuilder.superclass(AggregatedAppFunctionInventoryClass.CLASS_NAME)
        aggregatedInventoryClassBuilder.addAnnotation(AppFunctionCompiler.GENERATED_ANNOTATION)
        aggregatedInventoryClassBuilder.addProperty(buildInventoriesProperty(generatedInventories))

        val fileSpec =
            FileSpec.builder(APP_FUNCTIONS_INTERNAL_PACKAGE_NAME, aggregatedInventoryClassName)
                .addType(aggregatedInventoryClassBuilder.build())
                .build()

        codeGenerator
            .createNewFile(
                // TODO: Collect all AppFunction files as source files set
                Dependencies.ALL_FILES,
                APP_FUNCTIONS_INTERNAL_PACKAGE_NAME,
                aggregatedInventoryClassName,
            )
            .bufferedWriter()
            .use { fileSpec.writeTo(it) }
    }

    private fun buildInventoriesProperty(
        generatedInventories: List<KSClassDeclaration>
    ): PropertySpec {
        return PropertySpec.builder(
                AggregatedAppFunctionInventoryClass.PROPERTY_INVENTORIES_NAME,
                List::class.asClassName().parameterizedBy(APP_FUNCTION_INVENTORY_CLASS),
            )
            .addModifiers(KModifier.OVERRIDE)
            .initializer(
                buildCodeBlock {
                    addStatement("listOf(")
                    indent()
                    for (generatedInventory in generatedInventories) {
                        addStatement("%T(),", generatedInventory.toClassName())
                    }
                    unindent()
                    addStatement(")")
                }
            )
            .build()
    }

    private fun generateAggregatedAppFunctionInvoker(resolver: Resolver) {
        val generatedInvokers =
            AppFunctionSymbolResolver(resolver).getGeneratedAppFunctionInvokers()
        // TODO(b/463909015): Remove the condition logic once service module is removed
        val hasBaseInvoker =
            generatedInvokers.any { invokerClass ->
                invokerClass.superTypes.any { superType ->
                    superType.resolve().declaration.qualifiedName?.asString() ==
                        AppFunctionInvokerClass.CLASS_NAME_BASE.canonicalName
                }
            }
        val aggregatedInvokerClass =
            if (hasBaseInvoker) {
                AggregatedAppFunctionInvokerClass.CLASS_NAME_BASE
            } else {
                AggregatedAppFunctionInvokerClass.CLASS_NAME
            }
        val aggregatedInvokerPackage =
            if (hasBaseInvoker) {
                APP_FUNCTIONS_INTERNAL_PACKAGE_NAME
            } else {
                APP_FUNCTIONS_SERVICE_INTERNAL_PACKAGE_NAME
            }
        val aggregatedInvokerClassName = "${'$'}${aggregatedInvokerClass.simpleName}_Impl"

        val aggregatedInvokerClassBuilder = TypeSpec.classBuilder(aggregatedInvokerClassName)
        aggregatedInvokerClassBuilder.superclass(aggregatedInvokerClass)
        aggregatedInvokerClassBuilder.addAnnotation(AppFunctionCompiler.GENERATED_ANNOTATION)
        aggregatedInvokerClassBuilder.addProperty(
            buildInvokersProperty(generatedInvokers, hasBaseInvoker)
        )

        val fileSpec =
            FileSpec.builder(aggregatedInvokerPackage, aggregatedInvokerClassName)
                .addType(aggregatedInvokerClassBuilder.build())
                .build()

        codeGenerator
            .createNewFile(
                // TODO: Collect all AppFunction files as source files set
                Dependencies.ALL_FILES,
                aggregatedInvokerPackage,
                aggregatedInvokerClassName,
            )
            .bufferedWriter()
            .use { fileSpec.writeTo(it) }
    }

    private fun buildInvokersProperty(
        generatedInvokers: List<KSClassDeclaration>,
        hasBaseInvoker: Boolean,
    ): PropertySpec {
        // TODO(b/463909015): Remove the condition logic once service module is removed
        val invokerInterface =
            if (hasBaseInvoker) {
                AppFunctionInvokerClass.CLASS_NAME_BASE
            } else {
                AppFunctionInvokerClass.CLASS_NAME
            }
        return PropertySpec.builder(
                AggregatedAppFunctionInvokerClass.PROPERTY_INVOKERS_NAME,
                List::class.asClassName().parameterizedBy(invokerInterface),
            )
            .addModifiers(KModifier.OVERRIDE)
            .initializer(
                buildCodeBlock {
                    addStatement("listOf(")
                    indent()
                    for (generatedInvoker in generatedInvokers) {
                        addStatement("%T(),", generatedInvoker.toClassName())
                    }
                    unindent()
                    addStatement(")")
                }
            )
            .build()
    }

    private fun generateAggregatedIndexXml(resolver: Resolver) {
        // We generate both XML formats supported by old and new AppSearch indexer respectively
        // as it can't be guaranteed that the device will have the latest version of AppSearch.
        // TODO: Add compiler option to disable legacy xml generator.
        val legacyIndexProcessor =
            AppFunctionLegacyIndexXmlProcessor(codeGenerator, options, logger)
        legacyIndexProcessor.process(resolver)

        val indexProcessor = AppFunctionIndexXmlProcessor(codeGenerator, options, logger)
        indexProcessor.process(resolver)
    }

    private fun shouldProcess(resolver: Resolver): Boolean {
        val appFunctionSymbolResolver = AppFunctionSymbolResolver(resolver)
        val appFunctionClasses = appFunctionSymbolResolver.resolveAnnotatedAppFunctions()
        return appFunctionClasses.isEmpty()
    }
}
