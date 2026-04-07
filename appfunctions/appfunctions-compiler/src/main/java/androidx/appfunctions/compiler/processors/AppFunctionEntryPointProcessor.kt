/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.appfunctions.compiler.AppFunctionCompiler
import androidx.appfunctions.compiler.core.AnnotatedAppFunctionEntryPoint
import androidx.appfunctions.compiler.core.AppFunctionSymbolResolver
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionServiceClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.ExecuteAppFunctionRequestClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.ExecuteAppFunctionResponseClass
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
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec

/**
 * The processor to generate the AppFunctionService subclass for an AppFunction entry point.
 *
 * For each class annotated with `@AppFunctionEntryPoint`, a corresponding subclass extending it
 * will be generated under the same package. For example,
 * ```
 * @AppFunctionEntryPoint(serviceName = "MyService")
 * abstract class MyBaseService : AppFunctionService() {
 *   @AppFunction
 *   suspend fun doSomething() { ... }
 * }
 * ```
 *
 * A corresponding `MyService` class will be generated:
 * ```
 * class MyService: MyBaseService() {
 *   override suspend fun executeFunction(
 *     request: ExecuteAppFunctionRequest
 *   ): ExecuteAppFunctionResponse {
 *     // Routes the request to the corresponding method annotated with @AppFunction
 *     ...
 *   }
 * }
 * ```
 */
class AppFunctionEntryPointProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private var isProcessed = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (isProcessed) return emptyList()
        isProcessed = true

        val appFunctionSymbolResolver = AppFunctionSymbolResolver(resolver)
        val entryPoints = appFunctionSymbolResolver.resolveAnnotatedAppFunctionEntryPoints()
        for (entryPoint in entryPoints) {
            try {
                entryPoint.validate()
                generateAppFunctionService(entryPoint)
            } catch (e: ProcessingException) {
                logger.logException(e)
            }
        }
        return emptyList()
    }

    // TODO(b/463909015): Generate IDs for each AppFunction.
    private fun generateAppFunctionService(entryPoint: AnnotatedAppFunctionEntryPoint) {
        val serviceDeclaration = entryPoint.serviceDeclaration
        val serviceName = entryPoint.serviceName
        val packageName = serviceDeclaration.packageName.asString()
        val originalClassName = ClassName(packageName, serviceDeclaration.simpleName.asString())

        val serviceClassBuilder =
            TypeSpec.classBuilder(serviceName)
                .superclass(originalClassName)
                .addAnnotation(AppFunctionCompiler.GENERATED_ANNOTATION)
        val executeFunctionBuilder =
            FunSpec.builder(AppFunctionServiceClass.ExecuteFunctionMethod.METHOD_NAME)
                .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
                .addParameter(
                    AppFunctionServiceClass.ExecuteFunctionMethod.REQUEST_PARAM_NAME,
                    ExecuteAppFunctionRequestClass.CLASS_NAME,
                )
                .returns(ExecuteAppFunctionResponseClass.CLASS_NAME)
                // TODO(b/463909015): Implement the routing logic.
                .addStatement("TODO(%S)", "Not yet implemented")
                .build()

        serviceClassBuilder.addFunction(executeFunctionBuilder)

        val fileSpec =
            FileSpec.builder(packageName, serviceName).addType(serviceClassBuilder.build()).build()

        codeGenerator
            .createNewFile(
                Dependencies(
                    aggregating = false,
                    requireNotNull(serviceDeclaration.containingFile),
                ),
                packageName,
                serviceName,
            )
            .bufferedWriter()
            .use { fileSpec.writeTo(it) }
    }

    @VisibleForTesting
    class Provider : SymbolProcessorProvider {
        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
            return AppFunctionEntryPointProcessor(environment.codeGenerator, environment.logger)
        }
    }
}
