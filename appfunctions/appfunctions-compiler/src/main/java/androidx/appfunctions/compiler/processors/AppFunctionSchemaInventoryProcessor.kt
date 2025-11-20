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
import androidx.appfunctions.compiler.core.AnnotatedAppFunctionSchemaDefinition
import androidx.appfunctions.compiler.core.AnnotatedAppFunctionSerializableProxy
import androidx.appfunctions.compiler.core.AppFunctionInventoryCodeBuilder
import androidx.appfunctions.compiler.core.AppFunctionSymbolResolver
import androidx.appfunctions.compiler.core.IntrospectionHelper
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec

// TODO(b/403525399): Aggregate shared components at the top level to reduce memory usage.
/**
 * Generates the static mapped inventory to look up AppFunctionMetadata with schema as the key.
 *
 * The processor would only start aggregation process when
 * * [AppFunctionCompilerOptions.generateMetadataFromSchema] is true.
 * * AND there is no remaining @AppFunctionSchemaDefinition nodes to be processed.
 */
class AppFunctionSchemaInventoryProcessor(
    private val codeGenerator: CodeGenerator,
    private val options: AppFunctionCompilerOptions,
) : SymbolProcessor {
    private var hasProcessed = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (!options.generateMetadataFromSchema) {
            return emptyList()
        }

        if (hasProcessed) {
            return emptyList()
        }

        if (!shouldProcess(resolver)) {
            return emptyList()
        }

        val symbolResolver = AppFunctionSymbolResolver(resolver)
        val schemaDefinitions = symbolResolver.getAppFunctionSchemaDefinitionFromAllModules()
        val resolvedAnnotatedSerializableProxies =
            AnnotatedAppFunctionSerializableProxy.ResolvedAnnotatedSerializableProxies(
                symbolResolver.resolveAllAnnotatedSerializableProxiesFromModule()
            )
        generateSchemaAppFunctionInventoryClass(
            schemaDefinitions,
            resolvedAnnotatedSerializableProxies,
        )

        hasProcessed = true
        return emptyList()
    }

    private fun generateSchemaAppFunctionInventoryClass(
        schemaDefinitions: List<AnnotatedAppFunctionSchemaDefinition>,
        resolvedAnnotatedSerializableProxies:
            AnnotatedAppFunctionSerializableProxy.ResolvedAnnotatedSerializableProxies,
    ) {
        val packageName = IntrospectionHelper.SCHEMA_APP_FUNCTION_INVENTORY_CLASS.packageName
        val inventoryClassName = SCHEMA_INVENTORY_CLASS_NAME
        val inventoryClassBuilder = TypeSpec.classBuilder(inventoryClassName)
        inventoryClassBuilder.superclass(IntrospectionHelper.SCHEMA_APP_FUNCTION_INVENTORY_CLASS)
        inventoryClassBuilder.addAnnotation(AppFunctionCompiler.GENERATED_ANNOTATION)
        AppFunctionInventoryCodeBuilder(inventoryClassBuilder)
            .addFunctionMetadataProperties(
                schemaDefinitions.map {
                    it.createAppFunctionMetadata(resolvedAnnotatedSerializableProxies)
                }
            )

        val fileSpec =
            FileSpec.builder(packageName, inventoryClassName)
                .addType(inventoryClassBuilder.build())
                .build()
        codeGenerator
            .createNewFile(
                Dependencies(
                    aggregating = true,
                    *schemaDefinitions
                        .flatMap(AnnotatedAppFunctionSchemaDefinition::getSourceFiles)
                        .toTypedArray(),
                ),
                packageName,
                inventoryClassName,
            )
            .bufferedWriter()
            .use { fileSpec.writeTo(it) }
    }

    private fun shouldProcess(resolver: Resolver): Boolean {
        val appFunctionSymbolResolver = AppFunctionSymbolResolver(resolver)
        val appFunctionClasses =
            appFunctionSymbolResolver.resolveAnnotatedAppFunctionSchemaDefinitions()
        return appFunctionClasses.isEmpty()
    }

    private companion object {
        const val SCHEMA_INVENTORY_CLASS_NAME = "\$SchemaAppFunctionInventory_Impl"
    }
}
