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
import androidx.appfunctions.compiler.core.AnnotatedAppFunctionSerializableProxy.ResolvedAnnotatedSerializableProxies
import androidx.appfunctions.compiler.core.AnnotatedAppFunctions
import androidx.appfunctions.compiler.core.AppFunctionComponentRegistryGenerator
import androidx.appfunctions.compiler.core.AppFunctionComponentRegistryGenerator.AppFunctionComponent
import androidx.appfunctions.compiler.core.AppFunctionInventoryCodeBuilder
import androidx.appfunctions.compiler.core.AppFunctionSymbolResolver
import androidx.appfunctions.compiler.core.IntrospectionHelper
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionComponentRegistryAnnotation
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock

/**
 * Generates implementations for the AppFunctionInventory interface.
 *
 * It resolves all functions in a class annotated with `@AppFunction`, and generates the
 * corresponding metadata for those functions.
 *
 * **Important:** [androidx.appfunctions.compiler.processors.AppFunctionInventoryProcessor] will
 * process exactly once for each compilation unit to generate a single registry for looking up all
 * generated inventories within the compilation unit.
 */
class AppFunctionInventoryProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {

    private var hasProcessed = false

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (hasProcessed) return emptyList()
        hasProcessed = true

        val appFunctionSymbolResolver = AppFunctionSymbolResolver(resolver)
        val appFunctionClasses = appFunctionSymbolResolver.resolveAnnotatedAppFunctions()
        val resolvedAnnotatedSerializableProxies =
            ResolvedAnnotatedSerializableProxies(
                appFunctionSymbolResolver.resolveAllAnnotatedSerializableProxiesFromModule()
            )
        val generatedInventoryComponents =
            buildList<AppFunctionComponent> {
                for (appFunctionClass in appFunctionClasses) {
                    val inventoryQualifiedName =
                        generateAppFunctionInventoryClass(
                            appFunctionClass,
                            resolvedAnnotatedSerializableProxies,
                        )
                    add(
                        AppFunctionComponent(
                            qualifiedName = inventoryQualifiedName,
                            sourceFiles = appFunctionClass.getSourceFiles(),
                        )
                    )
                }
            }

        AppFunctionComponentRegistryGenerator(codeGenerator)
            .generateRegistry(
                resolver.getModuleName().asString(),
                AppFunctionComponentRegistryAnnotation.Category.INVENTORY,
                generatedInventoryComponents,
            )
        return resolvedAnnotatedSerializableProxies.resolvedAnnotatedSerializableProxies.map {
            it.appFunctionSerializableProxyClass
        }
    }

    /**
     * Generates an implementation of AppFunctionInventory for [appFunctionClass].
     *
     * @return fully qualified name of the generated inventory implementation class.
     */
    private fun generateAppFunctionInventoryClass(
        appFunctionClass: AnnotatedAppFunctions,
        resolvedAnnotatedSerializableProxies: ResolvedAnnotatedSerializableProxies,
    ): String {
        val originalPackageName = appFunctionClass.classDeclaration.packageName.asString()
        val originalClassName = appFunctionClass.classDeclaration.simpleName.asString()

        val inventoryClassName = getAppFunctionInventoryClassName(originalClassName)
        val inventoryClassBuilder = TypeSpec.classBuilder(inventoryClassName)
        inventoryClassBuilder.addSuperinterface(IntrospectionHelper.APP_FUNCTION_INVENTORY_CLASS)
        inventoryClassBuilder.addAnnotation(AppFunctionCompiler.GENERATED_ANNOTATION)
        inventoryClassBuilder.addKdoc(buildSourceFilesKdoc(appFunctionClass))
        AppFunctionInventoryCodeBuilder(inventoryClassBuilder)
            .addFunctionMetadataProperties(
                appFunctionClass.createAppFunctionMetadataList(resolvedAnnotatedSerializableProxies)
            )

        val fileSpec =
            FileSpec.builder(originalPackageName, inventoryClassName)
                .addType(inventoryClassBuilder.build())
                .build()
        codeGenerator
            .createNewFile(
                Dependencies(aggregating = true, *appFunctionClass.getSourceFiles().toTypedArray()),
                originalPackageName,
                inventoryClassName,
            )
            .bufferedWriter()
            .use { fileSpec.writeTo(it) }

        return "${originalPackageName}.$inventoryClassName"
    }

    private fun buildSourceFilesKdoc(appFunctionClass: AnnotatedAppFunctions): CodeBlock {
        return buildCodeBlock {
            addStatement("Source Files:")
            for (file in appFunctionClass.getSourceFiles()) {
                addStatement(file.fileName)
            }
        }
    }

    private fun getAppFunctionInventoryClassName(functionClassName: String): String {
        return "$%s_AppFunctionInventory".format(functionClassName)
    }

    companion object {
        const val APP_FUNCTION_METADATA_PROPERTY_NAME = "APP_FUNCTION_METADATA"
        const val SCHEMA_METADATA_PROPERTY_NAME = "SCHEMA_METADATA"
        const val PARAMETER_METADATA_LIST_PROPERTY_NAME = "PARAMETER_METADATA_LIST"
        const val RESPONSE_METADATA_PROPERTY_NAME = "RESPONSE_METADATA"
        const val COMPONENT_METADATA_PROPERTY_NAME = "COMPONENTS_METADATA"
        const val FUNCTION_ID_TO_METADATA_MAP_PROPERTY_NAME = "functionIdToMetadataMap"
    }
}
