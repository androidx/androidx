/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.privacysandbox.tools.apicompiler

import androidx.privacysandbox.tools.testing.AbstractDiffTest
import androidx.privacysandbox.tools.testing.CompilationTestHelper
import androidx.room.compiler.processing.util.Source
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.writeText

/** Base test class for API Compiler diff testing. */
abstract class AbstractApiCompilerDiffTest : AbstractDiffTest() {

    open val extraProcessorOptions: Map<String, String> = mapOf()

    override fun generateSources(
        inputSources: List<Source>,
        outputDirectory: Path
    ): List<Source> {
        val result = compileWithPrivacySandboxKspCompiler(inputSources, extraProcessorOptions)
        CompilationTestHelper.assertThat(result).succeeds()
        val sources = result.generatedSources

        // Writing generated sources to expected output directory.
        sources.forEach { source ->
            outputDirectory.resolve(source.relativePath).apply {
                parent?.createDirectories()
                createFile()
                writeText(source.contents)
            }
        }
        return sources
    }
}