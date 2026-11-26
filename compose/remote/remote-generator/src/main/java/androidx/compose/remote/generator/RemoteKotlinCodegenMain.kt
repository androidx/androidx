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

package androidx.compose.remote.generator

import androidx.compose.remote.generator.codegen.KotlinGenerator
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Internal build-time codegen entry point invoked by Gradle to generate Kotlin protocol, AST, and
 * writer sources directly from bundled `/schema/operations.json`.
 */
internal object RemoteKotlinCodegenMain {
    @JvmStatic
    fun main(args: Array<String>) {
        var target: String? = null
        var outputDirPath: String? = null
        for (arg in args) {
            when {
                arg.startsWith("--target=") -> target = arg.removePrefix("--target=")
                arg.startsWith("--output-dir=") -> outputDirPath = arg.removePrefix("--output-dir=")
                else -> throw IllegalArgumentException("Unknown argument: $arg")
            }
        }
        val resolvedTarget =
            requireNotNull(target) { "Missing --target=protocol|document|writer argument" }
        val resolvedOutput =
            requireNotNull(outputDirPath) { "Missing --output-dir=<path> argument" }

        val schemaText =
            checkNotNull(
                    RemoteKotlinCodegenMain::class
                        .java
                        .getResourceAsStream("/schema/operations.json")
                ) {
                    "Missing bundled /schema/operations.json resource"
                }
                .bufferedReader()
                .use { it.readText() }

        val schema = JsonSchemaParser.parseString(schemaText)
        val kotlinGenerator = KotlinGenerator(schema)
        val pkgDir =
            Paths.get(resolvedOutput)
                .toAbsolutePath()
                .normalize()
                .resolve("androidx/compose/remote/document")
        Files.createDirectories(pkgDir)

        when (resolvedTarget) {
            "protocol" -> {
                Files.writeString(
                    pkgDir.resolve("RemoteProtocol.kt"),
                    kotlinGenerator.generateProtocolConstants(),
                )
            }
            "document" -> {
                Files.writeString(
                    pkgDir.resolve("RemoteProtocol.kt"),
                    kotlinGenerator.generateProtocolConstants(),
                )
                Files.writeString(
                    pkgDir.resolve("RemoteOperations.kt"),
                    kotlinGenerator.generateAstNodes(),
                )
            }
            "writer" -> {
                Files.writeString(
                    pkgDir.resolve("RemoteOperationsWriter.kt"),
                    kotlinGenerator.generateWriterNodes(),
                )
                Files.writeString(
                    pkgDir.resolve("RemoteOperationSink.kt"),
                    kotlinGenerator.generateOperationSink(),
                )
            }
            else ->
                throw IllegalArgumentException(
                    "Unsupported --target=$resolvedTarget (expected protocol, document, or writer)"
                )
        }
    }
}
