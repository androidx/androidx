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

import androidx.annotation.VisibleForTesting
import androidx.appfunctions.compiler.core.AnnotatedAppFunctionSerializableProxy.ResolvedAnnotatedSerializableProxies
import androidx.appfunctions.compiler.core.AppFunctionSerializableType
import androidx.appfunctions.compiler.core.AppFunctionSymbolResolver
import androidx.appfunctions.compiler.core.ProcessingException
import androidx.appfunctions.compiler.core.logException
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.FileSpec

/**
 * Generates a factory class with methods to convert classes annotated with
 * androidx.appfunctions.AppFunctionSerializable or
 * androidx.appfunctions.AppFunctionSerializableProxy to androidx.appfunctions.AppFunctionData, and
 * vice-versa.
 *
 * **Example:**
 *
 * ```
 * @AppFunctionSerializable
 * class Location(val latitude: Double, val longitude: Double)
 * ```
 *
 * A corresponding `LocationFactory` class will be generated:
 * ```
 * @Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
 * public class LocationFactory : AppFunctionSerializableFactory<Location> {
 *   override fun fromAppFunctionData(appFunctionData: AppFunctionData): Location {
 *     val latitude = appFunctionData.getDouble("latitude")
 *     val longitude = appFunctionData.getDouble("longitude")
 *
 *     return Location(latitude, longitude)
 *   }
 *
 *   override fun toAppFunctionData(appFunctionSerializable: Location): AppFunctionData {
 *     val builder = getAppFunctionDataBuilder("")
 *
 *     builder.setDouble("latitude", location.latitude)
 *     builder.setDouble("longitude", location.longitude)
 *
 *     return builder.build()
 *   }
 * }
 * ```
 */
class AppFunctionSerializableProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private var hasProcessed = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (hasProcessed) return emptyList()
        hasProcessed = true

        try {
            val entitySymbolResolver = AppFunctionSymbolResolver(resolver)
            val entityClasses = entitySymbolResolver.resolveAnnotatedAppFunctionSerializables()
            val globalResolvedAnnotatedSerializableProxies =
                ResolvedAnnotatedSerializableProxies(
                    entitySymbolResolver.resolveAllAnnotatedSerializableProxiesFromModule()
                )
            val localResolvedAnnotatedSerializableProxies =
                ResolvedAnnotatedSerializableProxies(
                    entitySymbolResolver.resolveLocalAnnotatedAppFunctionSerializableProxy()
                )
            for (entity in entityClasses) {
                val fileSpec =
                    entity
                        .getFactoryCodeBuilder(globalResolvedAnnotatedSerializableProxies)
                        .buildAppFunctionSerializableFactoryClass()
                writeFile(serializable = entity, fileSpec = fileSpec)
            }
            for (entityProxy in
                localResolvedAnnotatedSerializableProxies.resolvedAnnotatedSerializableProxies) {
                // Check if the factory class has already been generated.
                if (
                    codeGenerator.generatedFile.any {
                        it.path.contains(entityProxy.factoryClassName.simpleName)
                    }
                ) {
                    continue
                }
                // Only generate factory for local proxy classes to ensure that the factory is
                // only generated once in the same compilation unit as the prexy definition.
                val fileSpec =
                    entityProxy
                        .getFactoryCodeBuilder(globalResolvedAnnotatedSerializableProxies)
                        .buildAppFunctionSerializableFactoryClass()
                writeFile(serializable = entityProxy, fileSpec = fileSpec)
            }
            return globalResolvedAnnotatedSerializableProxies.resolvedAnnotatedSerializableProxies
                .map { it.classDeclaration }
        } catch (e: ProcessingException) {
            logger.logException(e)
        }

        return emptyList()
    }

    private fun writeFile(serializable: AppFunctionSerializableType, fileSpec: FileSpec) {
        codeGenerator
            .createNewFile(
                Dependencies(
                    aggregating = true,
                    *serializable.getSerializableSourceFiles().toTypedArray(),
                ),
                fileSpec.packageName,
                fileSpec.name,
            )
            .bufferedWriter()
            .use { fileSpec.writeTo(it) }
    }

    @VisibleForTesting
    class Provider : SymbolProcessorProvider {
        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
            return AppFunctionSerializableProcessor(environment.codeGenerator, environment.logger)
        }
    }
}
