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
import androidx.appfunctions.compiler.core.fromCamelCaseToScreamingSnakeCase
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

/**
 * The processor to generate ID classes for AppFunction.
 *
 * For each AppFunction class, a corresponding ID class would be generated in the same package for
 * developer to access the function id easily. For example,
 * ```
 * class NoteFunction: CreateNote {
 *   @AppFunction
 *   override suspend fun createNote(): Note { ... }
 * }
 * ```
 *
 * A corresponding `NoteFunctionIds` class will be generated:
 * ```
 * object NoteFunctionIds {
 *   const val CREATE_NOTE_ID = "someId"
 * }
 * ```
 */
class AppFunctionIdProcessor(
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val appFunctionSymbolResolver = AppFunctionSymbolResolver(resolver)
        val appFunctionClasses = appFunctionSymbolResolver.resolveAnnotatedAppFunctions()
        for (appFunctionClass in appFunctionClasses) {
            generateAppFunctionIdClasses(appFunctionClass)
        }
        return emptyList()
    }

    private fun generateAppFunctionIdClasses(appFunctionClass: AnnotatedAppFunctions) {
        val originalPackageName = appFunctionClass.classDeclaration.packageName.asString()
        val originalClassName = appFunctionClass.classDeclaration.simpleName.asString()

        val idClassName = getAppFunctionIdClassName(originalClassName)
        val classBuilder =
            TypeSpec.objectBuilder(idClassName).apply {
                addAnnotation(AppFunctionCompiler.GENERATED_ANNOTATION)
                addAppFunctionIdProperties(appFunctionClass)
            }

        val fileSpec =
            FileSpec.builder(originalPackageName, idClassName).addType(classBuilder.build()).build()
        codeGenerator
            .createNewFile(
                Dependencies(
                    // Isolating, because the information to construct the ID class only comes
                    // from a single class containing AppFunction implementations and never from
                    // other or new files.
                    aggregating = false,
                    checkNotNull(appFunctionClass.classDeclaration.containingFile)
                ),
                originalPackageName,
                idClassName
            )
            .bufferedWriter()
            .use { fileSpec.writeTo(it) }
    }

    private fun getAppFunctionIdClassName(functionClassName: String): String {
        return "${functionClassName}Ids"
    }

    private fun TypeSpec.Builder.addAppFunctionIdProperties(
        appFunctionClass: AnnotatedAppFunctions
    ) {
        for (appFunctionDeclaration in appFunctionClass.appFunctionDeclarations) {
            // For example, transform method name from "createNote" to "CREATE_NOTE"
            val functionMethodName =
                appFunctionDeclaration.simpleName.asString().fromCamelCaseToScreamingSnakeCase()
            val propertySpec =
                PropertySpec.builder("${functionMethodName}_ID", String::class.asTypeName())
                    .addModifiers(KModifier.CONST)
                    .initializer(
                        "%S",
                        appFunctionClass.getAppFunctionIdentifier(appFunctionDeclaration)
                    )
                    .build()
            this.addProperty(propertySpec)
        }
    }
}
