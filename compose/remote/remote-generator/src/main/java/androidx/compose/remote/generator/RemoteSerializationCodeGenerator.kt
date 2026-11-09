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

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.compose.remote.generator.codegen.CppGenerator
import androidx.compose.remote.generator.codegen.PythonGenerator
import androidx.compose.remote.generator.codegen.RustGenerator
import androidx.compose.remote.generator.codegen.SwiftGenerator
import androidx.compose.remote.generator.codegen.TypeScriptGenerator
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/** Generates RemoteCompose wire-protocol serialization bindings for target languages. */
public object RemoteSerializationCodeGenerator {
    /**
     * Generates C++17 bindings (`WireBuffer.h`, `WireBuffer.cpp`, `RemoteComposeOps.h`, and
     * `RemoteComposeOps.cpp`).
     */
    public const val LANGUAGE_CPP: Int = 1

    /** Generates Rust bindings (`remote_compose_operations.rs` and `op_codes.rs`). */
    public const val LANGUAGE_RUST: Int = 2

    /** Generates Python 3 bindings (`remote_compose.py`). */
    public const val LANGUAGE_PYTHON: Int = 3

    /** Generates Swift bindings (`RemoteComposeOperations.swift`). */
    public const val LANGUAGE_SWIFT: Int = 4

    /** Generates TypeScript bindings (`remoteComposeOperations.ts`). */
    public const val LANGUAGE_TYPESCRIPT: Int = 5

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        LANGUAGE_CPP,
        LANGUAGE_RUST,
        LANGUAGE_PYTHON,
        LANGUAGE_SWIFT,
        LANGUAGE_TYPESCRIPT,
    )
    public annotation class TargetLanguage

    /**
     * Generates RemoteCompose operation bindings for [language] into [outputDirectory].
     *
     * Creates [outputDirectory] if it does not exist and overwrites target files in place.
     *
     * @param language target language constant ([LANGUAGE_CPP], [LANGUAGE_RUST], [LANGUAGE_PYTHON],
     *   [LANGUAGE_SWIFT], or [LANGUAGE_TYPESCRIPT])
     * @param outputDirectory directory where generated source files are written
     * @throws IllegalArgumentException if [language] is not a supported `LANGUAGE_*` constant
     * @throws IOException if writing to [outputDirectory] fails
     */
    @JvmStatic
    @Throws(IOException::class)
    public fun generate(@TargetLanguage language: Int, outputDirectory: Path) {
        val schema = loadBundledSchema()
        Files.createDirectories(outputDirectory)
        when (language) {
            LANGUAGE_CPP -> {
                val gen = CppGenerator(schema)
                Files.writeString(
                    outputDirectory.resolve("WireBuffer.h"),
                    gen.generateWireBufferHeader(),
                )
                Files.writeString(
                    outputDirectory.resolve("WireBuffer.cpp"),
                    gen.generateWireBufferSource(),
                )
                Files.writeString(
                    outputDirectory.resolve("RemoteComposeOps.h"),
                    gen.generateHeader(),
                )
                Files.writeString(
                    outputDirectory.resolve("RemoteComposeOps.cpp"),
                    gen.generateSource(),
                )
            }
            LANGUAGE_RUST -> {
                val gen = RustGenerator(schema)
                Files.writeString(
                    outputDirectory.resolve("remote_compose_operations.rs"),
                    gen.generate(),
                )
                Files.writeString(
                    outputDirectory.resolve("op_codes.rs"),
                    gen.generateOpCodesModule(),
                )
            }
            LANGUAGE_PYTHON -> {
                val gen = PythonGenerator(schema)
                Files.writeString(outputDirectory.resolve("remote_compose.py"), gen.generate())
            }
            LANGUAGE_SWIFT -> {
                val gen = SwiftGenerator(schema)
                Files.writeString(
                    outputDirectory.resolve("RemoteComposeOperations.swift"),
                    gen.generate(),
                )
            }
            LANGUAGE_TYPESCRIPT -> {
                val gen = TypeScriptGenerator(schema)
                Files.writeString(
                    outputDirectory.resolve("remoteComposeOperations.ts"),
                    gen.generate(),
                )
            }
            else -> throw IllegalArgumentException("Unsupported target language: $language")
        }
    }

    internal fun loadBundledSchema(): OperationSchema {
        val stream =
            checkNotNull(
                RemoteSerializationCodeGenerator::class
                    .java
                    .getResourceAsStream("/schema/operations.json")
            ) {
                "Missing bundled /schema/operations.json resource"
            }
        return stream.use { JsonSchemaParser.parse(it) }
    }
}
