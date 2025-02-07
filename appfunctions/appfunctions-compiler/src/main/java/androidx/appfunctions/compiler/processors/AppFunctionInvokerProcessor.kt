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
import androidx.appfunctions.compiler.core.AnnotatedAppFunctions
import androidx.appfunctions.compiler.core.AppFunctionSymbolResolver
import androidx.appfunctions.compiler.core.IntrospectionHelper.APP_FUNCTION_FUNCTION_NOT_FOUND_EXCEPTION_CLASS
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionContextClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionInvokerClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.ConfigurableAppFunctionFactoryClass
import androidx.appfunctions.compiler.core.toTypeName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.buildCodeBlock

/**
 * The processor to generate AppFunctionInvoker classes for AppFunction.
 *
 * For each AppFunction class, a corresponding AppFunctionInvoker implementation would be generated
 * under the same package. For example,
 * ```
 * class NoteFunction: CreateNote {
 *   @AppFunction
 *   override suspend fun createNote(): Note { ... }
 * }
 * ```
 *
 * A corresponding `$NoteFunction_AppFunctionInvoker` class will be generated:
 * ```
 * class $$NoteFunction_AppFunctionInvoker: AppFunctionInvoker {
 *   override val supportedFunctionIds: Set<String> = setOf(
 *     "com.example.NoteFunction#createNote",
 *   )
 *
 *   suspend fun unsafeInvoke(
 *     appFunctionContext: AppFunctionContext,
 *     functionIdentifier: String,
 *     parameters: Map<String, Any?>,
 *   ): Any? {
 *     return when(functionIdentifier) {
 *       "com.example.NoteFunction#createNote" -> {
 *         ConfigurableAppFunctionFactory<NoteFunction>(
 *           appFunctionContext.context
 *         )
 *         .createEnclosingClass(NoteFunction::class.java)
 *         .createNote(
 *           appFunctionContext,
 *           parameters["createNoteParams"] as MyCreateNoteParams
 *         )
 *       }
 *       else -> {
 *         throw AppFunctionFunctionNotFoundException(...)
 *       }
 *     }
 *   }
 * }
 * ```
 */
class AppFunctionInvokerProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val appFunctionSymbolResolver = AppFunctionSymbolResolver(resolver)
        val appFunctionClasses = appFunctionSymbolResolver.resolveAnnotatedAppFunctions()
        for (appFunctionClass in appFunctionClasses) {
            generateAppFunctionInvokerClass(appFunctionClass)
        }
        return emptyList()
    }

    private fun generateAppFunctionInvokerClass(appFunctionClass: AnnotatedAppFunctions) {
        val originalPackageName = appFunctionClass.classDeclaration.packageName.asString()
        val originalClassName = appFunctionClass.classDeclaration.simpleName.asString()

        val invokerClassName = getAppFunctionInvokerClassName(originalClassName)
        val invokerClassBuilder = TypeSpec.classBuilder(invokerClassName)
        invokerClassBuilder.addSuperinterface(AppFunctionInvokerClass.CLASS_NAME)
        invokerClassBuilder.addAnnotation(AppFunctionCompiler.GENERATED_ANNOTATION)
        invokerClassBuilder.addProperty(buildSupportedFunctionIdsProperty(appFunctionClass))
        invokerClassBuilder.addFunction(buildUnsafeInvokeFunction(appFunctionClass))

        val fileSpec =
            FileSpec.builder(originalPackageName, invokerClassName)
                .addType(invokerClassBuilder.build())
                .build()
        codeGenerator
            .createNewFile(
                Dependencies(
                    aggregating = false,
                    checkNotNull(appFunctionClass.classDeclaration.containingFile)
                ),
                originalPackageName,
                invokerClassName
            )
            .bufferedWriter()
            .use { fileSpec.writeTo(it) }
    }

    private fun buildSupportedFunctionIdsProperty(
        annotatedAppFunctions: AnnotatedAppFunctions
    ): PropertySpec {
        val functionIds =
            annotatedAppFunctions.appFunctionDeclarations.map { function ->
                annotatedAppFunctions.getAppFunctionIdentifier(function)
            }
        return PropertySpec.builder(
                AppFunctionInvokerClass.SUPPORTED_FUNCTION_IDS_PROPERTY_NAME,
                Set::class.asClassName().parameterizedBy(String::class.asTypeName()),
            )
            .addModifiers(KModifier.OVERRIDE)
            .initializer(
                buildCodeBlock {
                    addStatement("setOf(")
                    indent()
                    for (functionId in functionIds) {
                        addStatement("%S,", functionId)
                    }
                    unindent()
                    add(")")
                }
            )
            .build()
    }

    private fun buildUnsafeInvokeFunction(annotatedAppFunctions: AnnotatedAppFunctions): FunSpec {
        val contextSpec =
            ParameterSpec.builder(
                    AppFunctionInvokerClass.UnsafeInvokeMethod.APPLICATION_CONTEXT_PARAM_NAME,
                    AppFunctionContextClass.CLASS_NAME
                )
                .build()
        val functionIdentifierSpec =
            ParameterSpec.builder(
                    AppFunctionInvokerClass.UnsafeInvokeMethod.FUNCTION_ID_PARAM_NAME,
                    String::class
                )
                .build()
        val functionParametersSpec =
            ParameterSpec.builder(
                    AppFunctionInvokerClass.UnsafeInvokeMethod.PARAMETERS_PARAM_NAME,
                    Map::class.asClassName()
                        .parameterizedBy(
                            String::class.asTypeName(),
                            Any::class.asTypeName().copy(nullable = true),
                        ),
                )
                .build()
        return FunSpec.builder(AppFunctionInvokerClass.UnsafeInvokeMethod.METHOD_NAME)
            .addModifiers(KModifier.SUSPEND)
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(contextSpec)
            .addParameter(functionIdentifierSpec)
            .addParameter(functionParametersSpec)
            .returns(Any::class.asTypeName().copy(nullable = true))
            .addCode(
                buildCodeBlock {
                    addStatement("val result = when (${functionIdentifierSpec.name}) {")
                    indent()
                    for (appFunction in annotatedAppFunctions.appFunctionDeclarations) {
                        appendInvocationBranchStatement(
                            annotatedAppFunctions,
                            appFunction,
                            contextSpec,
                            functionParametersSpec,
                        )
                    }
                    unindent()
                    add(
                        """
                else -> {
                  throw %T("Unable to find ${'$'}${functionIdentifierSpec.name}")
                }
              }
              return result
              """
                            .trimIndent(),
                        APP_FUNCTION_FUNCTION_NOT_FOUND_EXCEPTION_CLASS,
                    )
                }
            )
            .build()
    }

    /**
     * Appends a branch statement for [appFunction] within [annotatedAppFunctions].
     *
     * This append the code block like
     *
     * ```
     * "com.example.TestFunction#test" -> {
     *   ConfigurableAppFunctionFactory<TestFunction>(applicationContext.context)
     *     .createEnclosingClass(TestFunction::class.java)
     *     .test(appFunctionContext, parameters["param1"] as Int)
     * }
     * ```
     */
    private fun CodeBlock.Builder.appendInvocationBranchStatement(
        annotatedAppFunctions: AnnotatedAppFunctions,
        appFunction: KSFunctionDeclaration,
        contextSpec: ParameterSpec,
        functionParametersSpec: ParameterSpec,
    ) {
        val functionParameterStatement =
            appFunction.getAppFunctionParametersStatement(contextSpec, functionParametersSpec)
        val formatStringMap =
            mapOf<String, Any>(
                "function_id" to annotatedAppFunctions.getAppFunctionIdentifier(appFunction),
                "factory_class" to ConfigurableAppFunctionFactoryClass.CLASS_NAME,
                "enclosing_class" to annotatedAppFunctions.getEnclosingClassName(),
                "context_param" to contextSpec.name,
                "context_property" to AppFunctionContextClass.CONTEXT_PROPERTY_NAME,
                "create_method" to
                    ConfigurableAppFunctionFactoryClass.CreateEnclosingClassMethod.METHOD_NAME,
                "function_name" to appFunction.simpleName.asString(),
                "parameters" to functionParameterStatement
            )
        addNamed("\"%function_id:L\" -> {\n", formatStringMap)
        indent()
        addNamed("%factory_class:T<%enclosing_class:T>(\n", formatStringMap)
        indent()
        addNamed("%context_param:L.%context_property:L\n", formatStringMap)
        unindent()
        add(")\n")
        addNamed(".%create_method:L(%enclosing_class:T::class.java)\n", formatStringMap)
        addNamed(".%function_name:L(%parameters:L)\n", formatStringMap)
        unindent()
        add("}\n")
    }

    private fun KSFunctionDeclaration.getAppFunctionParametersStatement(
        contextSpec: ParameterSpec,
        functionParametersSpec: ParameterSpec,
    ): String {
        val args =
            buildList<String> {
                for ((index, value) in parameters.withIndex()) {
                    if (index == 0) {
                        // The first parameter is always AppFunctionContext.
                        add(contextSpec.name)
                    } else {
                        val parameterName = checkNotNull(value.name).asString()
                        val parameterType = value.type.toTypeName()
                        add(
                            "${functionParametersSpec.name}[\"${parameterName}\"] as $parameterType"
                        )
                    }
                }
            }
        return args.joinToString(separator = ", ")
    }

    private fun getAppFunctionInvokerClassName(functionClassName: String): String {
        return "$%s_AppFunctionInvoker".format(functionClassName)
    }
}
