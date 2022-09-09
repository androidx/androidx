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
import androidx.privacysandbox.tools.core.ParsedApi
import androidx.privacysandbox.tools.core.generator.AidlCompiler
import androidx.privacysandbox.tools.core.generator.AidlGenerator
import java.io.File
import java.nio.file.Files
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
        generateBinders(sdkApi, AidlCompiler(aidlCompiler), output)

        sdkApi.services.forEach {
            ServiceInterfaceFileGenerator(it).generate().writeTo(output)
            ServiceFactoryFileGenerator(it).generate().writeTo(output)
        }
    }

    private fun generateBinders(sdkApi: ParsedApi, aidlCompiler: AidlCompiler, output: File) {
        val aidlWorkingDir = output.resolve("tmp-aidl").also { it.mkdir() }
        try {
            val generatedFiles =
                AidlGenerator.generate(aidlCompiler, sdkApi, aidlWorkingDir.toPath())
            generatedFiles.forEach {
                val relativePath = aidlWorkingDir.toPath().relativize(it.file.toPath())
                val source = it.file.toPath()
                val dest = output.toPath().resolve(relativePath)
                dest.toFile().parentFile.mkdirs()
                Files.move(source, dest)
            }
        } finally {
            aidlWorkingDir.deleteRecursively()
        }
    }
}