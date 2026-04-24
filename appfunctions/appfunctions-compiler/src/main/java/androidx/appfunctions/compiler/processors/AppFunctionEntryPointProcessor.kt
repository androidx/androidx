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
import androidx.appfunctions.compiler.core.AnnotatedAppFunctionSerializableProxy.ResolvedAnnotatedSerializableProxies
import androidx.appfunctions.compiler.core.AppFunctionSymbolResolver
import androidx.appfunctions.compiler.core.AppFunctionXmlGenerator
import androidx.appfunctions.compiler.core.IntrospectionHelper.APP_FUNCTION_FUNCTION_NOT_FOUND_EXCEPTION_CLASS
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionExecutionDispatcherClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionServiceClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.ExecuteAppFunctionRequestClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.ExecuteAppFunctionResponseClass
import androidx.appfunctions.compiler.core.ProcessingException
import androidx.appfunctions.compiler.core.logException
import androidx.appfunctions.compiler.core.toTypeName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock

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
        val resolvedAnnotatedSerializableProxies =
            ResolvedAnnotatedSerializableProxies(
                appFunctionSymbolResolver.resolveAllAnnotatedSerializableProxiesFromModule()
            )
        val descriptionMap = appFunctionSymbolResolver.getAppFunctionSerializablesDescriptionMap()

        val entryPoints = appFunctionSymbolResolver.resolveAnnotatedAppFunctionEntryPoints()
        for (entryPoint in entryPoints) {
            try {
                generateAppFunctionService(entryPoint)
                generateXml(entryPoint, resolvedAnnotatedSerializableProxies, descriptionMap)
            } catch (e: ProcessingException) {
                logger.logException(e)
            }
        }
        return emptyList()
    }

    private fun generateXml(
        entryPoint: AnnotatedAppFunctionEntryPoint,
        resolvedAnnotatedSerializableProxies: ResolvedAnnotatedSerializableProxies,
        descriptionMap: Map<String, String>,
    ) {
        val generator = AppFunctionXmlGenerator(codeGenerator, logger)
        generator.generateXml(
            entryPoint = entryPoint,
            resolvedAnnotatedSerializableProxies = resolvedAnnotatedSerializableProxies,
            appFunctionSerializablesDescriptionMap = descriptionMap,
            packageName = XML_PACKAGE_NAME,
            fileName = entryPoint.appFunctionXmlFileName,
        )
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
                .addFunction(buildExecuteFunction(entryPoint))

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

    private fun buildExecuteFunction(entryPoint: AnnotatedAppFunctionEntryPoint): FunSpec {
        return FunSpec.builder(AppFunctionServiceClass.ExecuteFunctionMethod.METHOD_NAME)
            .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
            .addParameter(
                AppFunctionServiceClass.ExecuteFunctionMethod.REQUEST_PARAM_NAME,
                ExecuteAppFunctionRequestClass.CLASS_NAME,
            )
            .returns(ExecuteAppFunctionResponseClass.CLASS_NAME)
            .addCode(buildExecuteFunctionBody(entryPoint))
            .build()
    }

    private fun buildExecuteFunctionBody(entryPoint: AnnotatedAppFunctionEntryPoint): CodeBlock {
        return buildCodeBlock {
            beginControlFlow(
                "return %T.%L(request) { parameters ->",
                AppFunctionExecutionDispatcherClass.CLASS_NAME,
                AppFunctionExecutionDispatcherClass.ExecuteAppFunctionMethod.METHOD_NAME,
            )
            beginControlFlow("when (request.functionIdentifier)")
            for (appFunction in entryPoint.appFunctions) {
                val function = appFunction.appFunctionDeclaration
                val identifier = appFunction.getAppFunctionIdentifier(entryPoint.serviceDeclaration)
                beginControlFlow("%S ->", identifier)
                add("this.%N(\n", function.simpleName.asString())
                indent()
                for (param in function.parameters) {
                    val paramName = param.name!!.asString()
                    addStatement("parameters[%S] as %T,", paramName, param.type.toTypeName())
                }
                unindent()
                addStatement(")")
                endControlFlow()
            }
            beginControlFlow("else ->")
            addStatement(
                "throw %T(\n    \"\${request.functionIdentifier} is not available\"\n)",
                APP_FUNCTION_FUNCTION_NOT_FOUND_EXCEPTION_CLASS,
            )
            endControlFlow() // end else
            endControlFlow() // end when
            endControlFlow() // end executeAppFunction
        }
    }

    @VisibleForTesting
    class Provider : SymbolProcessorProvider {
        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
            return AppFunctionEntryPointProcessor(environment.codeGenerator, environment.logger)
        }
    }

    private companion object {
        const val XML_PACKAGE_NAME = "assets"
    }
}
