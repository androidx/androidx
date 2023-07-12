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

package androidx.privacysandbox.tools.apicompiler.generator

import androidx.privacysandbox.tools.core.Metadata
import androidx.privacysandbox.tools.core.generator.AidlCompiler
import androidx.privacysandbox.tools.core.generator.AidlGenerator
import androidx.privacysandbox.tools.core.generator.ClientProxyTypeGenerator
import androidx.privacysandbox.tools.core.generator.CoreLibInfoAndBinderWrapperConverterGenerator
import androidx.privacysandbox.tools.core.generator.GenerationTarget
import androidx.privacysandbox.tools.core.generator.ServerBinderCodeConverter
import androidx.privacysandbox.tools.core.generator.ServiceFactoryFileGenerator
import androidx.privacysandbox.tools.core.generator.StubDelegatesGenerator
import androidx.privacysandbox.tools.core.generator.ThrowableParcelConverterFileGenerator
import androidx.privacysandbox.tools.core.generator.TransportCancellationGenerator
import androidx.privacysandbox.tools.core.generator.ValueConverterFileGenerator
import androidx.privacysandbox.tools.core.model.ParsedApi
import androidx.privacysandbox.tools.core.model.getOnlyService
import androidx.privacysandbox.tools.core.model.hasSuspendFunctions
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.FileSpec
import java.nio.file.Files.createTempDirectory
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

internal enum class SandboxApiVersion {
    // Android 13 - Tiramisu Privacy Sandbox
    API_33,

    // SDK runtime backwards compatibility library.
    SDK_RUNTIME_COMPAT_LIBRARY,
}

internal class SdkCodeGenerator(
    private val codeGenerator: CodeGenerator,
    private val api: ParsedApi,
    private val aidlCompilerPath: Path,
    private val frameworkAidlPath: Path?,
    private val sandboxApiVersion: SandboxApiVersion
) {
    private val binderCodeConverter = ServerBinderCodeConverter(api)
    private val target = GenerationTarget.SERVER

    fun generate() {
        if (api.services.isEmpty()) {
            return
        }
        generateAidlSources()
        generateAbstractSdkProvider()
        generateStubDelegates()
        generateValueConverters()
        generateCallbackProxies()
        generateToolMetadata()
        generateSuspendFunctionUtilities()
        generateServiceFactoryFile()
    }

    private fun generateAidlSources() {
        val workingDir = createTempDirectory("aidl")
        try {
            AidlGenerator.generate(
                AidlCompiler(aidlCompilerPath, frameworkAidlPath),
                api, workingDir
            )
                .forEach { source ->
                    // Sources created by the AIDL compiler have to be copied to files created
                    // through the KSP APIs, so that they are included in downstream compilation.
                    val kspGeneratedFile = codeGenerator.createNewFile(
                        Dependencies.ALL_FILES,
                        source.packageName,
                        source.interfaceName,
                        extensionName = "java"
                    )
                    source.file.inputStream().copyTo(kspGeneratedFile)
                }
        } finally {
            workingDir.toFile().deleteRecursively()
        }
    }

    private fun generateAbstractSdkProvider() {
        val generator = when (sandboxApiVersion) {
            SandboxApiVersion.API_33 -> Api33SdkProviderGenerator(api)
            SandboxApiVersion.SDK_RUNTIME_COMPAT_LIBRARY -> CompatSdkProviderGenerator(api)
        }
        generator.generate()?.also(::write)
    }

    private fun generateStubDelegates() {
        val stubDelegateGenerator = StubDelegatesGenerator(basePackageName(), binderCodeConverter)
        api.services.map { stubDelegateGenerator.generate(it, target) }
            .forEach(::write)
        api.interfaces.map { stubDelegateGenerator.generate(it, target) }
            .forEach(::write)
    }

    private fun generateValueConverters() {
        val valueConverterFileGenerator =
            ValueConverterFileGenerator(binderCodeConverter, target)
        api.values.map(valueConverterFileGenerator::generate).forEach(::write)
        api.interfaces.filter { it.inheritsSandboxedUiAdapter }.map {
            CoreLibInfoAndBinderWrapperConverterGenerator.generate(it).also(::write)
        }
    }

    private fun generateCallbackProxies() {
        val clientProxyGenerator = ClientProxyTypeGenerator(basePackageName(), binderCodeConverter)
        api.callbacks.map { clientProxyGenerator.generate(it, target) }
            .forEach(::write)
    }

    private fun generateToolMetadata() {
        codeGenerator.createNewFile(
            Dependencies.ALL_FILES,
            Metadata.filePath.parent.toString(),
            Metadata.filePath.nameWithoutExtension,
            Metadata.filePath.extension,
        ).use { Metadata.toolMetadata.writeTo(it) }
    }

    private fun generateServiceFactoryFile() {
        // The service factory stubs are generated so that the API Packager can include them in the
        // API descriptors, and the client can use those symbols without running the API Generator.
        // It's not intended to be used by the SDK code.
        val serviceFactoryFileGenerator = ServiceFactoryFileGenerator(generateStubs = true)
        api.services.forEach {
            serviceFactoryFileGenerator.generate(it).also(::write)
        }
    }

    private fun generateSuspendFunctionUtilities() {
        if (!api.hasSuspendFunctions()) return
        TransportCancellationGenerator(basePackageName()).generate().also(::write)
        ThrowableParcelConverterFileGenerator(basePackageName(), target).generate()
            .also(::write)
    }

    private fun write(spec: FileSpec) {
        codeGenerator.createNewFile(Dependencies.ALL_FILES, spec.packageName, spec.name)
            .bufferedWriter().use(spec::writeTo)
    }

    private fun basePackageName() = api.getOnlyService().type.packageName
}