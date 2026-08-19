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

import androidx.appfunctions.compiler.AppFunctionCompiler
import androidx.appfunctions.compiler.AppFunctionCompilerOptions
import androidx.appfunctions.compiler.core.AnnotatedAppFunctionSerializableProxy.ResolvedAnnotatedSerializableProxies
import androidx.appfunctions.compiler.core.AnnotatedAppFunctionSignature
import androidx.appfunctions.compiler.core.AppFunctionSpecCodeBuilder
import androidx.appfunctions.compiler.core.AppFunctionSymbolResolver
import androidx.appfunctions.compiler.core.AppFunctionXmlGenerator
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionAdapterHelperClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.ExecuteAppFunctionRequestClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.ExecuteAppFunctionResponseClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.ExperimentalAppFunctionsApiAnnotation
import androidx.appfunctions.compiler.core.IntrospectionHelper.HandleAppFunctionRequestAdapterClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.HandleAppFunctionRequestClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.OptInAnnotation
import androidx.appfunctions.compiler.core.IntrospectionHelper.SuspendingAppFunctionClass
import androidx.appfunctions.compiler.core.ensureQualifiedName
import androidx.appfunctions.compiler.core.toClassName
import androidx.appfunctions.compiler.core.toTypeName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.buildCodeBlock

/** The processor to validate and generate adapters for AppFunctionSignature. */
class AppFunctionSignatureProcessor(
    private val options: AppFunctionCompilerOptions,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    private var hasProcessed = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (hasProcessed) return emptyList()
        hasProcessed = true

        val appFunctionSymbolResolver = AppFunctionSymbolResolver(resolver)
        val appFunctionSignatures =
            appFunctionSymbolResolver.resolveAnnotatedAppFunctionSignatures()
        val resolvedAnnotatedSerializableProxies =
            ResolvedAnnotatedSerializableProxies(
                appFunctionSymbolResolver.resolveAllAnnotatedSerializableProxiesFromModule()
            )

        if (appFunctionSignatures.isNotEmpty()) {
            val xmlGenerator = AppFunctionXmlGenerator(codeGenerator, logger)
            val groupedSignatures =
                appFunctionSignatures.groupBy { signature -> signature.appFunctionXmlFileName }

            for ((appFunctionXmlFileName, signaturesInGroup) in groupedSignatures) {
                xmlGenerator.generateXml(
                    signaturesInGroup,
                    resolvedAnnotatedSerializableProxies,
                    appFunctionSymbolResolver.getAppFunctionSerializablesDescriptionMap(),
                    XML_PACKAGE_NAME,
                    appFunctionXmlFileName,
                    options.appFunctionsXmlLocation,
                )
            }
            // Generate adapter classes for each signature
            for (signature in appFunctionSignatures) {
                generateHandleAppFunctionRequestAdapterClass(
                    signature,
                    resolvedAnnotatedSerializableProxies,
                )
            }
        }

        return emptyList()
    }

    private fun generateHandleAppFunctionRequestAdapterClass(
        signature: AnnotatedAppFunctionSignature,
        resolvedAnnotatedSerializableProxies: ResolvedAnnotatedSerializableProxies,
    ) {
        val originalPackageName = signature.classDeclaration.packageName.asString()
        val originalClassName = signature.classDeclaration.simpleName.asString()

        val adapterClassName = AppFunctionSpecCodeBuilder.getGeneratedClassName(originalClassName)
        val enclosingClass = signature.classDeclaration.toClassName()

        val compileTimeMetadata =
            signature.createAppFunctionMetadata(resolvedAnnotatedSerializableProxies)

        // Generate metadata properties using the shared AppFunctionSpecCodeBuilder
        val specBuilder = AppFunctionSpecCodeBuilder()
        val companionObjectSpec =
            TypeSpec.companionObjectBuilder()
                .apply {
                    specBuilder.addPropertiesForParameterSpec(
                        this,
                        compileTimeMetadata.parameters,
                        propertyModifiers = listOf(KModifier.PRIVATE),
                    )
                    specBuilder.addPropertyForResponseSpec(this, compileTimeMetadata.response)
                }
                .build()

        val adapterClassSpec =
            TypeSpec.classBuilder(adapterClassName)
                .addSuperinterface(
                    HandleAppFunctionRequestAdapterClass.CLASS_NAME.parameterizedBy(enclosingClass)
                )
                .addAnnotation(AppFunctionCompiler.GENERATED_ANNOTATION)
                .addAnnotation(
                    AnnotationSpec.builder(OptInAnnotation.CLASS_NAME)
                        .addMember("%T::class", ExperimentalAppFunctionsApiAnnotation.CLASS_NAME)
                        .build()
                )
                .addProperty(buildFunctionIdentifier(signature))
                .addFunction(buildAdapt(signature))
                .addFunction(buildWithExtractedArgsFunction(signature))
                .addFunction(buildToExecuteAppFunctionResponseFunction(signature))
                .addType(companionObjectSpec)
                .build()

        val fileSpec =
            FileSpec.builder(originalPackageName, adapterClassName)
                .addType(adapterClassSpec)
                .build()
        codeGenerator
            .createNewFile(
                Dependencies(
                    aggregating = true,
                    sources = signature.getSourceFiles().toTypedArray(),
                ),
                originalPackageName,
                adapterClassName,
            )
            .bufferedWriter()
            .use { fileSpec.writeTo(it) }
    }

    private fun buildFunctionIdentifier(signature: AnnotatedAppFunctionSignature): PropertySpec {
        val methodName = signature.appFunctionDeclaration.simpleName.asString()
        val functionId = "${signature.classDeclaration.ensureQualifiedName()}#$methodName"

        return PropertySpec.builder(
                HandleAppFunctionRequestAdapterClass.FUNCTION_IDENTIFIER_PROPERTY_NAME,
                String::class,
            )
            .addModifiers(KModifier.OVERRIDE)
            .initializer("%S", functionId)
            .build()
    }

    private fun buildAdapt(signature: AnnotatedAppFunctionSignature): FunSpec {
        val instanceParam =
            ParameterSpec.builder(
                    HandleAppFunctionRequestAdapterClass.AdaptMethod.INSTANCE_PARAM_NAME,
                    signature.classDeclaration.toClassName(),
                )
                .build()

        return FunSpec.builder(HandleAppFunctionRequestAdapterClass.AdaptMethod.METHOD_NAME)
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(instanceParam)
            .returns(HandleAppFunctionRequestClass.CLASS_NAME)
            .addCode(
                buildCodeBlock {
                    add("return %T(\n", HandleAppFunctionRequestClass.CLASS_NAME)
                    indent()
                    add(
                        "%L = %N,\n",
                        HandleAppFunctionRequestClass.FUNCTION_IDENTIFIER_PROPERTY_NAME,
                        HandleAppFunctionRequestAdapterClass.FUNCTION_IDENTIFIER_PROPERTY_NAME,
                    )
                    add(
                        "%L = %T { %L ->\n",
                        HandleAppFunctionRequestClass.APP_FUNCTION_PROPERTY_NAME,
                        SuspendingAppFunctionClass.CLASS_NAME,
                        HandleAppFunctionRequestAdapterClass.WithExtractedArgumentsMethod
                            .REQUEST_PARAM_NAME,
                    )
                    indent()
                    val methodName = signature.appFunctionDeclaration.simpleName.asString()

                    val lambdaParamsBlock = buildCodeBlock {
                        val params = signature.appFunctionDeclaration.parameters
                        params.forEachIndexed { index, parameter ->
                            val paramName = checkNotNull(parameter.name).asString()
                            add("%L", paramName)
                            if (index < params.size - 1) {
                                add(", ")
                            }
                        }
                    }

                    if (signature.appFunctionDeclaration.parameters.isNotEmpty()) {
                        beginControlFlow(
                            "%N(%L) { %L ->",
                            HandleAppFunctionRequestAdapterClass.WithExtractedArgumentsMethod
                                .METHOD_NAME,
                            HandleAppFunctionRequestAdapterClass.WithExtractedArgumentsMethod
                                .REQUEST_PARAM_NAME,
                            lambdaParamsBlock,
                        )
                    } else {
                        beginControlFlow(
                            "%N(%L)",
                            HandleAppFunctionRequestAdapterClass.WithExtractedArgumentsMethod
                                .METHOD_NAME,
                            HandleAppFunctionRequestAdapterClass.WithExtractedArgumentsMethod
                                .REQUEST_PARAM_NAME,
                        )
                    }

                    val callArgsBlock = buildCodeBlock {
                        val params = signature.appFunctionDeclaration.parameters
                        params.forEachIndexed { index, parameter ->
                            val paramName = checkNotNull(parameter.name).asString()
                            add("%L", paramName)
                            if (index < params.size - 1) {
                                add(", ")
                            }
                        }
                    }

                    add(
                        "val %L = %L.%L(",
                        HandleAppFunctionRequestAdapterClass.ToExecuteAppFunctionResponseMethod
                            .RESULT_PARAM_NAME,
                        HandleAppFunctionRequestAdapterClass.AdaptMethod.INSTANCE_PARAM_NAME,
                        methodName,
                    )
                    add(callArgsBlock)
                    add(")\n")
                    addStatement(
                        "%N(%L)",
                        HandleAppFunctionRequestAdapterClass.ToExecuteAppFunctionResponseMethod
                            .METHOD_NAME,
                        HandleAppFunctionRequestAdapterClass.ToExecuteAppFunctionResponseMethod
                            .RESULT_PARAM_NAME,
                    )

                    endControlFlow() // closes withExtractedArgs block

                    unindent()
                    add("}\n")
                    unindent()
                    add(")\n")
                }
            )
            .build()
    }

    private fun buildWithExtractedArgsFunction(signature: AnnotatedAppFunctionSignature): FunSpec {
        val requestParam =
            ParameterSpec.builder(
                    HandleAppFunctionRequestAdapterClass.WithExtractedArgumentsMethod
                        .REQUEST_PARAM_NAME,
                    ExecuteAppFunctionRequestClass.CLASS_NAME,
                )
                .build()

        val rTypeVar = TypeVariableName("R")

        val lambdaParameters =
            signature.appFunctionDeclaration.parameters.map { parameter ->
                val paramName = checkNotNull(parameter.name).asString()
                val paramType = parameter.type.toTypeName()
                ParameterSpec.builder(paramName, paramType).build()
            }
        val lambdaType = LambdaTypeName.get(parameters = lambdaParameters, returnType = rTypeVar)

        val blockParam =
            ParameterSpec.builder(
                    HandleAppFunctionRequestAdapterClass.WithExtractedArgumentsMethod
                        .BLOCK_PARAM_NAME,
                    lambdaType,
                )
                .build()

        // TODO(b/524139557): Consider making this public for callback based implementations.
        return FunSpec.builder(
                HandleAppFunctionRequestAdapterClass.WithExtractedArgumentsMethod.METHOD_NAME
            )
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class).addMember("%S", "UNCHECKED_CAST").build()
            )
            .addModifiers(KModifier.PRIVATE, KModifier.INLINE)
            .addTypeVariable(rTypeVar)
            .addParameter(requestParam)
            .addParameter(blockParam)
            .returns(rTypeVar)
            .addCode(
                buildCodeBlock {
                    val params = signature.appFunctionDeclaration.parameters
                    params.forEach { parameter ->
                        val paramName = checkNotNull(parameter.name).asString()
                        val paramPropertyName = "${paramName.uppercase()}_PARAMETER_SPEC"
                        val paramType = parameter.type.toTypeName()
                        addStatement(
                            "val %L = %L.functionParameters.%M(%L) as %T",
                            paramName,
                            HandleAppFunctionRequestAdapterClass.WithExtractedArgumentsMethod
                                .REQUEST_PARAM_NAME,
                            AppFunctionAdapterHelperClass.UnsafeGetParameterValueMethod.METHOD_NAME,
                            paramPropertyName,
                            paramType,
                        )
                    }

                    val argsBlock = buildCodeBlock {
                        params.forEachIndexed { index, parameter ->
                            val paramName = checkNotNull(parameter.name).asString()
                            add("%L", paramName)
                            if (index < params.size - 1) {
                                add(", ")
                            }
                        }
                    }
                    add(
                        "return %L(",
                        HandleAppFunctionRequestAdapterClass.WithExtractedArgumentsMethod
                            .BLOCK_PARAM_NAME,
                    )
                    add(argsBlock)
                    add(")\n")
                }
            )
            .build()
    }

    private fun buildToExecuteAppFunctionResponseFunction(
        signature: AnnotatedAppFunctionSignature
    ): FunSpec {
        val returnType = signature.appFunctionDeclaration.returnType?.toTypeName() ?: UNIT

        val resultParam =
            ParameterSpec.builder(
                    HandleAppFunctionRequestAdapterClass.ToExecuteAppFunctionResponseMethod
                        .RESULT_PARAM_NAME,
                    returnType,
                )
                .build()

        // TODO(b/524139557): Consider making this public for callback based implementations.
        return FunSpec.builder(
                HandleAppFunctionRequestAdapterClass.ToExecuteAppFunctionResponseMethod.METHOD_NAME
            )
            .addModifiers(KModifier.PRIVATE)
            .addParameter(resultParam)
            .returns(ExecuteAppFunctionResponseClass.CLASS_NAME)
            .addCode(
                buildCodeBlock {
                    addStatement(
                        "val returnValue = RESPONSE_SPEC.%M(%L)",
                        AppFunctionAdapterHelperClass.UnsafeBuildReturnValueMethod.METHOD_NAME,
                        HandleAppFunctionRequestAdapterClass.ToExecuteAppFunctionResponseMethod
                            .RESULT_PARAM_NAME,
                    )
                    addStatement(
                        "return %T(returnValue)",
                        ExecuteAppFunctionResponseClass.SUCCESS_CLASS_NAME,
                    )
                }
            )
            .build()
    }

    private companion object {
        const val XML_PACKAGE_NAME = "assets"
    }
}
