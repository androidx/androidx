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
import androidx.appfunctions.compiler.core.AnnotatedAppFunction
import androidx.appfunctions.compiler.core.AnnotatedAppFunctionSerializableProxy.ResolvedAnnotatedSerializableProxies
import androidx.appfunctions.compiler.core.AppFunctionInventoryCodeBuilder
import androidx.appfunctions.compiler.core.AppFunctionSymbolResolver
import androidx.appfunctions.compiler.core.IntrospectionHelper
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock

/**
 * Generates implementations for the AppFunctionInventory interface.
 *
 * It resolves all functions in a class annotated with `@AppFunctionServiceEntryPoint`, and
 * generates the corresponding metadata for those functions.
 */
class AppFunctionInventoryProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {

    private var hasProcessed = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (hasProcessed) return emptyList()
        hasProcessed = true

        val appFunctionSymbolResolver = AppFunctionSymbolResolver(resolver)
        val appFunctionEntryPoints =
            appFunctionSymbolResolver.resolveAnnotatedAppFunctionServiceEntryPoints()
        val resolvedAnnotatedSerializableProxies =
            ResolvedAnnotatedSerializableProxies(
                appFunctionSymbolResolver.resolveAllAnnotatedSerializableProxiesFromModule()
            )
        for (appFunctionEntryPoint in appFunctionEntryPoints) {
            generateAppFunctionInventoryClass(
                enclosingClassDeclaration = appFunctionEntryPoint.serviceDeclaration,
                annotatedAppFunctions = appFunctionEntryPoint.appFunctions,
                resolvedAnnotatedSerializableProxies = resolvedAnnotatedSerializableProxies,
                sourceFiles = appFunctionEntryPoint.getSourceFiles(),
            )
        }
        return resolvedAnnotatedSerializableProxies.resolvedAnnotatedSerializableProxies.map {
            it.classDeclaration
        }
    }

    /** Generates an implementation of AppFunctionInventory. */
    private fun generateAppFunctionInventoryClass(
        enclosingClassDeclaration: KSClassDeclaration,
        annotatedAppFunctions: List<AnnotatedAppFunction>,
        resolvedAnnotatedSerializableProxies: ResolvedAnnotatedSerializableProxies,
        sourceFiles: Set<KSFile>,
    ) {
        val originalPackageName = enclosingClassDeclaration.packageName.asString()
        val originalClassName = enclosingClassDeclaration.simpleName.asString()

        val inventoryClassName =
            AppFunctionInventoryCodeBuilder.getAppFunctionInventoryClassName(originalClassName)
        val inventoryClassBuilder = TypeSpec.classBuilder(inventoryClassName)
        inventoryClassBuilder.addSuperinterface(IntrospectionHelper.APP_FUNCTION_INVENTORY_CLASS)
        inventoryClassBuilder.addAnnotation(AppFunctionCompiler.GENERATED_ANNOTATION)
        inventoryClassBuilder.addKdoc(buildSourceFilesKdoc(sourceFiles))
        AppFunctionInventoryCodeBuilder(inventoryClassBuilder)
            .addFunctionMetadataProperties(
                annotatedAppFunctions.map { annotatedAppFunction ->
                    annotatedAppFunction.createAppFunctionMetadata(
                        enclosingClassDeclaration,
                        resolvedAnnotatedSerializableProxies,
                    )
                }
            )

        val fileSpec =
            FileSpec.builder(originalPackageName, inventoryClassName)
                .addType(inventoryClassBuilder.build())
                .build()
        codeGenerator
            .createNewFile(
                Dependencies(aggregating = true, *sourceFiles.toTypedArray()),
                originalPackageName,
                inventoryClassName,
            )
            .bufferedWriter()
            .use { fileSpec.writeTo(it) }
    }

    private fun buildSourceFilesKdoc(sourceFiles: Set<KSFile>): CodeBlock {
        return buildCodeBlock {
            addStatement("Source Files:")
            for (file in sourceFiles) {
                addStatement(file.fileName)
            }
        }
    }

    companion object {
        const val APP_FUNCTION_METADATA_PROPERTY_NAME = "APP_FUNCTION_METADATA"
        const val SCHEMA_METADATA_PROPERTY_NAME = "SCHEMA_METADATA"
        const val DEPRECATION_METADATA_PROPERTY_NAME = "DEPRECATION_METADATA"
        const val PARAMETER_METADATA_LIST_PROPERTY_NAME = "PARAMETER_METADATA_LIST"
        const val RESPONSE_METADATA_PROPERTY_NAME = "RESPONSE_METADATA"
        const val COMPONENT_METADATA_PROPERTY_NAME = "COMPONENTS_METADATA"
        const val FUNCTION_ID_TO_METADATA_MAP_PROPERTY_NAME = "functionIdToMetadataMap"
        const val INVENTORY_COMPONENTS_METADATA_PROPERTY_NAME = "componentsMetadata"
    }
}
