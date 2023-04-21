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
import androidx.privacysandbox.tools.core.Metadata
import androidx.privacysandbox.tools.core.generator.AidlCompiler
import androidx.privacysandbox.tools.core.generator.AidlGenerator
import androidx.privacysandbox.tools.core.generator.BinderCodeConverter
import androidx.privacysandbox.tools.core.generator.ClientBinderCodeConverter
import androidx.privacysandbox.tools.core.generator.ClientProxyTypeGenerator
import androidx.privacysandbox.tools.core.generator.GenerationTarget
import androidx.privacysandbox.tools.core.generator.PrivacySandboxExceptionFileGenerator
import androidx.privacysandbox.tools.core.generator.PrivacySandboxCancellationExceptionFileGenerator
import androidx.privacysandbox.tools.core.generator.ServiceFactoryFileGenerator
import androidx.privacysandbox.tools.core.generator.StubDelegatesGenerator
import androidx.privacysandbox.tools.core.generator.ThrowableParcelConverterFileGenerator
import androidx.privacysandbox.tools.core.generator.ValueConverterFileGenerator
import androidx.privacysandbox.tools.core.generator.ValueFileGenerator
import androidx.privacysandbox.tools.core.model.ParsedApi
import androidx.privacysandbox.tools.core.model.getOnlyService
import androidx.privacysandbox.tools.core.model.hasSuspendFunctions
import androidx.privacysandbox.tools.core.proto.PrivacySandboxToolsProtocol.ToolMetadata
import com.google.protobuf.InvalidProtocolBufferException
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.moveTo
import kotlin.io.path.outputStream
import kotlin.io.path.readBytes

/** Generate source files for communicating with an SDK running in the Privacy Sandbox. */
class PrivacySandboxApiGenerator {
    @Deprecated("Please supply the frameworkAidl path argument")
    fun generate(
        sdkInterfaceDescriptors: Path,
        aidlCompiler: Path,
        outputDirectory: Path,
    ) = generateImpl(sdkInterfaceDescriptors, aidlCompiler, null, outputDirectory)

    /**
     * Generate API sources for a given SDK.
     *
     * The SDK interface is defined by the [sdkInterfaceDescriptors], which is expected to be
     * a zip file with a set of compiled Kotlin interfaces using Privacy Sandbox tool annotations.
     * The SDK is expected to be compiled with compatible Privacy Sandbox tools.
     *
     * @param sdkInterfaceDescriptors Zip file with the SDK's annotated and compiled interfaces.
     * @param aidlCompiler AIDL compiler binary. It must target API 30 or above.
     * @param frameworkAidl Framework AIDL stubs to compile with
     * @param outputDirectory Output directory for the sources.
     */
    fun generate(
        sdkInterfaceDescriptors: Path,
        aidlCompiler: Path,
        frameworkAidl: Path,
        outputDirectory: Path,
    ) = generateImpl(sdkInterfaceDescriptors, aidlCompiler, frameworkAidl, outputDirectory)

    private fun generateImpl(
        sdkInterfaceDescriptors: Path,
        aidlCompiler: Path,
        frameworkAidl: Path?,
        outputDirectory: Path,
    ) {
        check(outputDirectory.exists() && outputDirectory.isDirectory()) {
            "$outputDirectory is not a valid output path."
        }

        val api = unzipDescriptorsFileAndParseStubs(sdkInterfaceDescriptors, outputDirectory)
        val output = outputDirectory.toFile()

        val basePackageName = api.getOnlyService().type.packageName
        val binderCodeConverter = ClientBinderCodeConverter(api)
        val interfaceFileGenerator = InterfaceFileGenerator()

        generateBinders(api, AidlCompiler(aidlCompiler, frameworkAidl), output)
        generateServiceFactory(api, output)
        generateStubDelegates(
            api,
            basePackageName,
            binderCodeConverter,
            interfaceFileGenerator,
            output
        )
        generateClientProxies(
            api,
            basePackageName,
            binderCodeConverter,
            interfaceFileGenerator,
            output
        )
        generateValueConverters(api, binderCodeConverter, output)
        generateSuspendFunctionUtilities(api, basePackageName, output)
    }

    private fun generateBinders(api: ParsedApi, aidlCompiler: AidlCompiler, output: File) {
        val aidlWorkingDir = output.resolve("tmp-aidl").also { it.mkdir() }
        try {
            val generatedFiles =
                AidlGenerator.generate(aidlCompiler, api, aidlWorkingDir.toPath())
            generatedFiles.forEach {
                val relativePath = aidlWorkingDir.toPath().relativize(it.file.toPath())
                val source = it.file.toPath()
                val dest = output.toPath().resolve(relativePath)
                dest.parent.createDirectories()
                source.moveTo(dest)
            }
        } finally {
            aidlWorkingDir.deleteRecursively()
        }
    }

    private fun generateServiceFactory(api: ParsedApi, output: File) {
        val serviceFactoryFileGenerator = ServiceFactoryFileGenerator()
        api.services.forEach {
            serviceFactoryFileGenerator.generate(it).writeTo(output)
        }
    }

    private fun generateStubDelegates(
        api: ParsedApi,
        basePackageName: String,
        binderCodeConverter: BinderCodeConverter,
        interfaceFileGenerator: InterfaceFileGenerator,
        output: File
    ) {
        val stubDelegateGenerator = StubDelegatesGenerator(basePackageName, binderCodeConverter)
        api.callbacks.forEach {
            interfaceFileGenerator.generate(it).writeTo(output)
            stubDelegateGenerator.generate(it, GenerationTarget.CLIENT).writeTo(output)
        }
    }

    private fun generateClientProxies(
        api: ParsedApi,
        basePackageName: String,
        binderCodeConverter: BinderCodeConverter,
        interfaceFileGenerator: InterfaceFileGenerator,
        output: File
    ) {
        val clientProxyGenerator = ClientProxyTypeGenerator(basePackageName, binderCodeConverter)
        val annotatedInterfaces = api.services + api.interfaces
        annotatedInterfaces.forEach {
            interfaceFileGenerator.generate(it).writeTo(output)
            clientProxyGenerator.generate(it, GenerationTarget.CLIENT).writeTo(output)
        }
    }

    private fun generateValueConverters(
        api: ParsedApi,
        binderCodeConverter: BinderCodeConverter,
        output: File
    ) {
        val valueFileGenerator = ValueFileGenerator()
        val valueConverterFileGenerator =
            ValueConverterFileGenerator(binderCodeConverter, GenerationTarget.CLIENT)
        api.values.forEach {
            valueFileGenerator.generate(it).writeTo(output)
            valueConverterFileGenerator.generate(it).writeTo(output)
        }
    }

    private fun unzipDescriptorsFileAndParseStubs(
        sdkInterfaceDescriptors: Path,
        outputDirectory: Path,
    ): ParsedApi {
        val workingDirectory = outputDirectory.resolve("tmp-descriptors").createDirectories()
        try {
            ZipInputStream(sdkInterfaceDescriptors.inputStream()).use { input ->
                generateSequence { input.nextEntry }
                    .forEach { zipEntry ->
                        val destination = workingDirectory.resolve(zipEntry.name)
                        check(destination.startsWith(workingDirectory))
                        destination.parent?.createDirectories()
                        destination.createFile()
                        input.copyTo(destination.outputStream())
                    }
            }

            ensureValidMetadata(workingDirectory.resolve(Metadata.filePath))
            return ApiStubParser.parse(workingDirectory)
        } finally {
            workingDirectory.toFile().deleteRecursively()
        }
    }

    private fun ensureValidMetadata(metadataFile: Path) {
        require(metadataFile.exists()) {
            "Missing tool metadata in SDK API descriptor."
        }

        val metadata = try {
            ToolMetadata.parseFrom(metadataFile.readBytes())
        } catch (e: InvalidProtocolBufferException) {
            throw IllegalArgumentException("Invalid Privacy Sandbox tool metadata.", e)
        }

        val sdkCodeGenerationVersion = metadata.codeGenerationVersion
        val consumerVersion = Metadata.toolMetadata.codeGenerationVersion
        require(sdkCodeGenerationVersion <= consumerVersion) {
            "SDK uses incompatible Privacy Sandbox tooling " +
                "(version $sdkCodeGenerationVersion). Current version is $consumerVersion."
        }
    }

    private fun generateSuspendFunctionUtilities(
        api: ParsedApi,
        basePackageName: String,
        output: File
    ) {
        if (!api.hasSuspendFunctions()) return
        ThrowableParcelConverterFileGenerator(basePackageName, GenerationTarget.CLIENT)
            .generate().writeTo(output)
        PrivacySandboxExceptionFileGenerator(basePackageName).generate().writeTo(output)
        PrivacySandboxCancellationExceptionFileGenerator(basePackageName).generate().writeTo(output)
    }
}