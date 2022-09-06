/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.tools.apigenerator

import androidx.privacysandbox.tools.apigenerator.parser.ApiStubParser
import androidx.privacysandbox.tools.core.AnnotatedInterface
import androidx.privacysandbox.tools.core.Method
import androidx.privacysandbox.tools.core.Parameter
import androidx.privacysandbox.tools.core.Type
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/** Generate source files for communicating with an SDK running in the Privacy Sandbox. */
class PrivacySandboxApiGenerator {
    /**
     * Generate API sources for a given SDK.
     *
     * The SDK interface is defined by the [sdkInterfaceDescriptors], which is expected to be
     * a zip file with a set of compiled Kotlin interfaces using Privacy Sandbox tool annotations.
     * The SDK is expected to be compiled with compatible Privacy Sandbox tools.
     *
     * @param sdkInterfaceDescriptors Zip file with the SDK's annotated and compiled interfaces.
     * @param aidlCompiler AIDL compiler binary. It must target API 30 or above.
     * @param outputDirectory Output directory for the sources.
     */
    // AIDL compiler parameter will be used once we start generating Binders.
    @Suppress("UNUSED_PARAMETER")
    fun generate(
        sdkInterfaceDescriptors: Path,
        aidlCompiler: Path,
        outputDirectory: Path,
    ) {
        check(outputDirectory.exists() && outputDirectory.isDirectory()) {
            "$outputDirectory is not a valid output path."
        }

        val output = outputDirectory.toFile()
        val sdkApi = ApiStubParser.parse(sdkInterfaceDescriptors)
        sdkApi.services.forEach {
            generateServiceInterface(it, output)
            generateServiceImplementation(it, output)
        }
    }

    private fun generateServiceInterface(service: AnnotatedInterface, outputDir: File) {
        val annotatedInterface =
            TypeSpec.interfaceBuilder(ClassName(service.packageName, service.name))
                .addFunctions(service.methods.map {
                    it.poetSpec()
                        .toBuilder().addModifiers(KModifier.ABSTRACT)
                        .build()
                })
                .build()
        FileSpec.get(service.packageName, annotatedInterface)
            .toBuilder()
            .addKotlinDefaultImports(includeJvm = false, includeJs = false)
            .build()
            .writeTo(outputDir)
    }

    private fun generateServiceImplementation(service: AnnotatedInterface, outputDir: File) {
        val implementation =
            TypeSpec.classBuilder(ClassName(service.packageName, "${service.name}Impl"))
                .addSuperinterface(ClassName(service.packageName, service.name))
                .addFunctions(service.methods.map {
                    it.poetSpec()
                        .toBuilder().addModifiers(KModifier.OVERRIDE)
                        .addCode(CodeBlock.of("TODO()"))
                        .build()
                })
                .build()
        FileSpec.get(service.packageName, implementation)
            .toBuilder()
            .addKotlinDefaultImports(includeJvm = false, includeJs = false)
            .build()
            .writeTo(outputDir)
    }

    private fun Method.poetSpec(): FunSpec {
        return FunSpec.builder(name)
            .addParameters(parameters.map { it.poetSpec() })
            .returns(returnType.poetSpec())
            .build()
    }

    private fun Parameter.poetSpec(): ParameterSpec {
        return ParameterSpec.builder(name, type.poetSpec()).build()
    }

    private fun Type.poetSpec(): TypeName {
        val splits = name.split('.')
        return ClassName(splits.dropLast(1).joinToString("."), splits.last())
    }
}