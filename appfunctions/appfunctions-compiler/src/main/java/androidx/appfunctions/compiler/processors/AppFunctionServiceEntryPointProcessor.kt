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
import androidx.appfunctions.compiler.AppFunctionCompilerOptions
import androidx.appfunctions.compiler.core.AnnotatedAppFunction
import androidx.appfunctions.compiler.core.AnnotatedAppFunctionSerializableProxy.ResolvedAnnotatedSerializableProxies
import androidx.appfunctions.compiler.core.AnnotatedAppFunctionServiceEntryPoint
import androidx.appfunctions.compiler.core.AppFunctionInventoryCodeBuilder
import androidx.appfunctions.compiler.core.AppFunctionLegacySchemaXmlGenerator
import androidx.appfunctions.compiler.core.AppFunctionSymbolResolver
import androidx.appfunctions.compiler.core.AppFunctionXmlGenerator
import androidx.appfunctions.compiler.core.IntrospectionHelper.APP_FUNCTION_FUNCTION_NOT_FOUND_EXCEPTION_CLASS
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionExecutionDispatcherClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionInventoryInterface
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionInventoryProviderInterface
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionServiceClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.CancellationSignalClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.ConsumerClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.CoroutineScopeClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.DispatchersClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.ExecuteAppFunctionRequestClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.ExecuteAppFunctionResponseClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.RequiresApiAnnotation
import androidx.appfunctions.compiler.core.ProcessingException
import androidx.appfunctions.compiler.core.findAnnotation
import androidx.appfunctions.compiler.core.fromCamelCaseToScreamingSnakeCase
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
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock

/**
 * The processor to generate the AppFunctionService subclass for an AppFunction entry point.
 *
 * For each class annotated with `@AppFunctionServiceEntryPoint`, a corresponding subclass extending
 * it will be generated under the same package. For example,
 * ```
 * @AppFunctionServiceEntryPoint(serviceName = "MyService")
 * abstract class MyBaseService : AppFunctionService() {
 *   @AppFunction
 *   suspend fun doSomething() { ... }
 * }
 * ```
 *
 * A corresponding `MyService` class will be generated:
 * ```
 * class MyService: MyBaseService() {
 *   override suspend fun onExecuteFunction(
 *     request: ExecuteAppFunctionRequest
 *   ): ExecuteAppFunctionResponse {
 *     // Routes the request to the corresponding method annotated with @AppFunction
 *     ...
 *   }
 * }
 * ```
 */
class AppFunctionServiceEntryPointProcessor(
    private val options: AppFunctionCompilerOptions,
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

        val serviceEntryPoints =
            appFunctionSymbolResolver.resolveAnnotatedAppFunctionServiceEntryPoints()
        for (serviceEntryPoint in serviceEntryPoints) {
            try {
                generateAppFunctionService(serviceEntryPoint)
                generateXml(serviceEntryPoint, resolvedAnnotatedSerializableProxies, descriptionMap)
            } catch (e: ProcessingException) {
                logger.logException(e)
            }
        }
        return emptyList()
    }

    private fun generateXml(
        serviceEntryPoint: AnnotatedAppFunctionServiceEntryPoint,
        resolvedAnnotatedSerializableProxies: ResolvedAnnotatedSerializableProxies,
        descriptionMap: Map<String, String>,
    ) {
        val generator = AppFunctionXmlGenerator(codeGenerator, logger)
        generator.generateXml(
            serviceEntryPoint = serviceEntryPoint,
            resolvedAnnotatedSerializableProxies = resolvedAnnotatedSerializableProxies,
            appFunctionSerializablesDescriptionMap = descriptionMap,
            packageName = XML_PACKAGE_NAME,
            fileName = serviceEntryPoint.appFunctionXmlFileName,
        )
        if (options.generateV1Xml) {
            val legacyGenerator = AppFunctionLegacySchemaXmlGenerator(codeGenerator, logger)
            legacyGenerator.generateLegacyIndexXml(
                serviceEntryPoint = serviceEntryPoint,
                resolvedAnnotatedSerializableProxies = resolvedAnnotatedSerializableProxies,
            )
        }
    }

    private fun generateAppFunctionService(
        serviceEntryPoint: AnnotatedAppFunctionServiceEntryPoint
    ) {
        val serviceDeclaration = serviceEntryPoint.serviceDeclaration
        val serviceName = serviceEntryPoint.serviceName
        val packageName = serviceDeclaration.packageName.asString()
        val originalClassName = ClassName(packageName, serviceDeclaration.simpleName.asString())

        val serviceClassBuilder =
            TypeSpec.classBuilder(serviceName)
                .superclass(originalClassName)
                .addAnnotation(AppFunctionCompiler.GENERATED_ANNOTATION)
                .addProperty(
                    PropertySpec.builder(PROPERTY_SCOPE, CoroutineScopeClass.CLASS_NAME)
                        .mutable(true)
                        .addModifiers(KModifier.PRIVATE, KModifier.LATEINIT)
                        .build()
                )
                .addFunction(
                    FunSpec.builder(AppFunctionServiceClass.OnCreateMethod.METHOD_NAME)
                        .addModifiers(KModifier.OVERRIDE)
                        .addCode(
                            buildCodeBlock {
                                addStatement(
                                    "super.%L()",
                                    AppFunctionServiceClass.OnCreateMethod.METHOD_NAME,
                                )
                                addStatement(
                                    "%L = %T(%M() + %T.%L)",
                                    PROPERTY_SCOPE,
                                    CoroutineScopeClass.CLASS_NAME,
                                    CoroutineScopeClass.SUPERVISOR_JOB_METHOD_NAME,
                                    DispatchersClass.CLASS_NAME,
                                    DispatchersClass.PROPERTY_MAIN,
                                )
                            }
                        )
                        .build()
                )
                .addFunction(
                    FunSpec.builder(AppFunctionServiceClass.OnDestroyMethod.METHOD_NAME)
                        .addModifiers(KModifier.OVERRIDE)
                        .addCode(
                            buildCodeBlock {
                                addStatement(
                                    "%L.%M()",
                                    PROPERTY_SCOPE,
                                    CoroutineScopeClass.CANCEL_EXTENSION_METHOD_NAME,
                                )
                                addStatement(
                                    "super.%L()",
                                    AppFunctionServiceClass.OnDestroyMethod.METHOD_NAME,
                                )
                            }
                        )
                        .build()
                )
                .addFunction(buildExecuteFunction(serviceEntryPoint))
                .addFunction(buildResolveInventory(serviceEntryPoint))
                .addType(buildCompanionObject(serviceEntryPoint))

        val requiresApiAnnotation =
            serviceDeclaration.annotations.findAnnotation(RequiresApiAnnotation.CLASS_NAME)
        if (requiresApiAnnotation != null) {
            val annotationBuilder = AnnotationSpec.builder(RequiresApiAnnotation.CLASS_NAME)
            for (argument in requiresApiAnnotation.arguments) {
                val name = argument.name?.asString()
                val value = argument.value
                if (value != null && name != null) {
                    annotationBuilder.addMember("%L = %L", name, value)
                }
            }
            serviceClassBuilder.addAnnotation(annotationBuilder.build())
        }

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

    private fun buildExecuteFunction(
        serviceEntryPoint: AnnotatedAppFunctionServiceEntryPoint
    ): FunSpec {
        return FunSpec.builder(AppFunctionServiceClass.ExecuteFunctionMethod.METHOD_NAME)
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(
                AppFunctionServiceClass.ExecuteFunctionMethod.REQUEST_PARAM_NAME,
                ExecuteAppFunctionRequestClass.CLASS_NAME,
            )
            .addParameter(
                AppFunctionServiceClass.ExecuteFunctionMethod.CANCELLATION_SIGNAL_PARAM_NAME,
                CancellationSignalClass.CLASS_NAME,
            )
            .addParameter(
                AppFunctionServiceClass.ExecuteFunctionMethod.CALLBACK_PARAM_NAME,
                ConsumerClass.CLASS_NAME.parameterizedBy(ExecuteAppFunctionResponseClass.CLASS_NAME),
            )
            .addCode(buildExecuteFunctionBody(serviceEntryPoint))
            .build()
    }

    private fun buildResolveInventory(
        serviceEntryPoint: AnnotatedAppFunctionServiceEntryPoint
    ): FunSpec {
        val generatedInventoryTypeName =
            ClassName(
                serviceEntryPoint.serviceDeclaration.packageName.asString(),
                AppFunctionInventoryCodeBuilder.getAppFunctionInventoryClassName(
                    serviceEntryPoint.serviceDeclaration.simpleName.asString()
                ),
            )
        return FunSpec.builder(
                AppFunctionInventoryProviderInterface.ResolveInventoryMethod.METHOD_NAME
            )
            .addModifiers(KModifier.OVERRIDE)
            .returns(AppFunctionInventoryInterface.CLASS_NAME)
            .addCode(buildCodeBlock { addStatement("return %T()", generatedInventoryTypeName) })
            .build()
    }

    private fun buildExecuteFunctionBody(
        serviceEntryPoint: AnnotatedAppFunctionServiceEntryPoint
    ): CodeBlock {
        val innerParametersName = "parameters"
        return buildCodeBlock {
            beginControlFlow(
                """
                return %T.%L(
                  %L,
                  %L,
                  %N(),
                  %L,
                  %L,
                ) { %L ->
                """
                    .trimIndent(),
                AppFunctionExecutionDispatcherClass.CLASS_NAME,
                AppFunctionExecutionDispatcherClass.DispatchExecuteAppFunctionMethod.METHOD_NAME,
                PROPERTY_SCOPE,
                AppFunctionServiceClass.ExecuteFunctionMethod.REQUEST_PARAM_NAME,
                AppFunctionInventoryProviderInterface.ResolveInventoryMethod.METHOD_NAME,
                AppFunctionServiceClass.ExecuteFunctionMethod.CANCELLATION_SIGNAL_PARAM_NAME,
                AppFunctionServiceClass.ExecuteFunctionMethod.CALLBACK_PARAM_NAME,
                innerParametersName,
            )
            beginControlFlow(
                "when (%L.%L)",
                AppFunctionServiceClass.ExecuteFunctionMethod.REQUEST_PARAM_NAME,
                ExecuteAppFunctionRequestClass.PROPERTY_FUNCTION_IDENTIFIER,
            )
            for (appFunction in serviceEntryPoint.appFunctions) {
                val function = appFunction.appFunctionDeclaration
                beginControlFlow("%L ->", getFunctionIdConstantPropertyName(appFunction))
                add("this.%N(\n", function.simpleName.asString())
                indent()
                for (param in function.parameters) {
                    val paramName = param.name!!.asString()
                    addStatement(
                        "%L[%S] as %T,",
                        innerParametersName,
                        paramName,
                        param.type.toTypeName(),
                    )
                }
                unindent()
                addStatement(")")
                endControlFlow()
            }
            beginControlFlow("else ->")
            addStatement(
                "throw %T(\n    \"\${%L.%L} is not available\"\n)",
                APP_FUNCTION_FUNCTION_NOT_FOUND_EXCEPTION_CLASS,
                AppFunctionServiceClass.ExecuteFunctionMethod.REQUEST_PARAM_NAME,
                ExecuteAppFunctionRequestClass.PROPERTY_FUNCTION_IDENTIFIER,
            )
            endControlFlow() // end else
            endControlFlow() // end when
            endControlFlow() // end executeAppFunction
        }
    }

    private fun buildCompanionObject(
        serviceEntryPoint: AnnotatedAppFunctionServiceEntryPoint
    ): TypeSpec {
        val functionIdPropertySpecList = buildList {
            for (annotatedAppFunction in serviceEntryPoint.appFunctions) {
                val id =
                    annotatedAppFunction.getAppFunctionIdentifier(
                        serviceEntryPoint.serviceDeclaration
                    )
                val propertyName = getFunctionIdConstantPropertyName(annotatedAppFunction)
                add(
                    PropertySpec.builder(name = propertyName, type = String::class)
                        .addModifiers(KModifier.CONST)
                        .initializer("%S", id)
                        .build()
                )
            }
        }
        return TypeSpec.companionObjectBuilder()
            .apply {
                for (functionIdPropertySpec in functionIdPropertySpecList) {
                    addProperty(functionIdPropertySpec)
                }
            }
            .build()
    }

    private fun getFunctionIdConstantPropertyName(
        annotatedAppFunction: AnnotatedAppFunction
    ): String {
        val functionSimpleName = annotatedAppFunction.appFunctionDeclaration.simpleName.asString()
        return FUNCTION_ID_PREFIX + functionSimpleName.fromCamelCaseToScreamingSnakeCase()
    }

    @VisibleForTesting
    class Provider : SymbolProcessorProvider {
        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
            return AppFunctionServiceEntryPointProcessor(
                AppFunctionCompilerOptions.from(environment.options),
                environment.codeGenerator,
                environment.logger,
            )
        }
    }

    private companion object {
        const val XML_PACKAGE_NAME = "assets"
        const val FUNCTION_ID_PREFIX = "FUNCTION_ID_"
        const val PROPERTY_SCOPE = "scope"
    }
}
