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

import androidx.appfunctions.compiler.core.AppFunctionComponentRegistryGenerator
import androidx.appfunctions.compiler.core.AppFunctionComponentRegistryGenerator.AppFunctionComponent
import androidx.appfunctions.compiler.core.AppFunctionSymbolResolver
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionComponentRegistryAnnotation
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated

/**
 * Generates the registry of all symbols that are needed for later aggregation processing.
 *
 * This includes
 * * @AppFunctionSerializable - The serializable declarations that are needed for multi-module KDoc
 *   lookup when generating XML files.
 * * @AppFunctionSchemaDefinition - The schema definition that are needed to generating a statically
 *   mapped inventory to look up AppFunctionMetadata with schema key.
 *
 * **Important:** [androidx.appfunctions.compiler.processors.AppFunctionComponentRegistryProcessor]
 * will process exactly once for each compilation unit to generate a single registry for looking up
 * all symbols that are needed for later processing within the compilation unit.
 */
class AppFunctionComponentRegistryProcessor(private val codeGenerator: CodeGenerator) :
    SymbolProcessor {

    private var hasProcessed = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (hasProcessed) return emptyList()
        hasProcessed = true

        generateSerializableComponentRegistry(resolver)
        generateSchemaDefinitionComponentRegistry(resolver)

        return emptyList()
    }

    @OptIn(KspExperimental::class)
    private fun generateSerializableComponentRegistry(resolver: Resolver) {
        val annotatedSerializables =
            AppFunctionSymbolResolver(resolver).resolveAnnotatedAppFunctionSerializables()
        val serializableComponents = buildList {
            for (annotatedSerializable in annotatedSerializables) {
                add(
                    AppFunctionComponent(
                        qualifiedName = annotatedSerializable.jvmQualifiedName,
                        docString =
                            if (annotatedSerializable.isDescribedByKDoc) {
                                annotatedSerializable.getDescription()
                            } else {
                                ""
                            },
                    )
                )
                for (property in annotatedSerializable.getProperties()) {
                    add(
                        AppFunctionComponent(
                            qualifiedName = property.qualifiedName,
                            docString = property.description,
                        )
                    )
                }
            }
        }

        AppFunctionComponentRegistryGenerator(codeGenerator)
            .generateRegistry(
                resolver.getModuleName().asString(),
                AppFunctionComponentRegistryAnnotation.Category.SERIALIZABLE,
                serializableComponents,
            )
    }

    @OptIn(KspExperimental::class)
    private fun generateSchemaDefinitionComponentRegistry(resolver: Resolver) {
        val annotatedSchemaDefinitions =
            AppFunctionSymbolResolver(resolver).resolveAnnotatedAppFunctionSchemaDefinitions()
        val schemaDefinitionComponents =
            annotatedSchemaDefinitions.map { annotatedSchemaDefinition ->
                AppFunctionComponent(
                    qualifiedName = annotatedSchemaDefinition.qualifiedName,
                    sourceFiles = annotatedSchemaDefinition.getSourceFiles(),
                )
            }

        AppFunctionComponentRegistryGenerator(codeGenerator)
            .generateRegistry(
                resolver.getModuleName().asString(),
                AppFunctionComponentRegistryAnnotation.Category.SCHEMA_DEFINITION,
                schemaDefinitionComponents,
            )
    }
}
